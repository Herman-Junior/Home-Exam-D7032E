package kittens.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import kittens.api.Answer;
import kittens.api.Decision;
import kittens.api.GameEvent;
import kittens.api.GameListener;
import kittens.api.Messenger;
import kittens.api.PlayerId;

/**
 * The engine's line to the players. It asks them for decisions and checks every answer, so a bad answer, or a port
 * that fails, never reaches the rules. All waiting and all threads for timed questions are here.
 */
final class PlayerChannel implements Messenger {

    /** How often a player is asked again after a bad answer before the first option is used. */
    static final int MAX_ATTEMPTS = 3;

    private final List<Participant> participants;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "player-answer");
        thread.setDaemon(true);
        return thread;
    });

    PlayerChannel(List<Participant> participants) {
        this.participants = List.copyOf(participants);
    }

    /**
     * Asks a player, who gets {@link #MAX_ATTEMPTS} tries. A bad answer, such as an index outside the options, or a
     * port that throws, is reported to the player and counts as a try. If all tries fail, the first option is used,
     * which for the turn decision is to pass.
     */
    @Override
    public int ask(PlayerId player, Decision decision) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            OptionalInt answer = askOnce(player, decision);
            if (answer.isPresent()) {
                return answer.getAsInt();
            }
            tell(player, GameEvent.of("invalid-answer", Integer.toString(attempt), Integer.toString(MAX_ATTEMPTS)));
        }
        announce(GameEvent.of("answer-defaulted", player.toString()));
        return 0;
    }

    @Override
    public List<Answer> askAll(Collection<PlayerId> players, Decision decision, Duration limit) {
        CompletionService<Answer> completion = new ExecutorCompletionService<>(executor);
        List<Future<Answer>> pending = new ArrayList<>();
        for (PlayerId player : players) {
            pending.add(completion.submit(() -> {
                OptionalInt answer = askOnce(player, decision);
                if (answer.isEmpty()) {
                    throw new IllegalStateException("no valid answer from " + player);
                }
                return new Answer(player, answer.getAsInt());
            }));
        }
        List<Answer> answers = new ArrayList<>();
        long deadline = System.nanoTime() + limit.toNanos();
        try {
            for (int received = 0; received < pending.size(); received++) {
                long remaining = deadline - System.nanoTime();
                Future<Answer> done = remaining <= 0 ? null : completion.poll(remaining, TimeUnit.NANOSECONDS);
                if (done == null) {
                    break;
                }
                try {
                    answers.add(done.get());
                } catch (ExecutionException e) {
                    // a player who gave no valid answer is the same as one who did not answer in time
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pending.forEach(future -> future.cancel(true));
        }
        return answers;
    }

    @Override
    public void announce(GameEvent event) {
        for (Participant participant : participants) {
            deliver(participant.listener(), event);
        }
    }

    @Override
    public void tell(PlayerId player, GameEvent event) {
        deliver(participant(player).listener(), event);
    }

    private OptionalInt askOnce(PlayerId player, Decision decision) {
        try {
            int choice = participant(player).port().choose(decision);
            return decision.accepts(choice) ? OptionalInt.of(choice) : OptionalInt.empty();
        } catch (RuntimeException e) {
            return OptionalInt.empty();
        }
    }

    private void deliver(GameListener listener, GameEvent event) {
        try {
            listener.onEvent(event);
        } catch (RuntimeException e) {
            // one listener that fails must not stop the game or keep the others from hearing about it
        }
    }

    private Participant participant(PlayerId player) {
        if (player.value() >= participants.size()) {
            throw new IllegalArgumentException("there is no player " + player);
        }
        return participants.get(player.value());
    }
}
