package kittens.player;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import kittens.api.Decision;
import kittens.api.GameEvent;
import kittens.api.GameListener;
import kittens.api.PlayerPort;
import kittens.net.Connection;
import kittens.net.Message;
import kittens.net.WireOption;

/**
 * A player at another computer. To the engine it is an ordinary {@link PlayerPort} and {@link GameListener}; this class
 * turns the calls into typed messages over a {@link Connection} and the replies back into answers. The engine knows
 * nothing of sockets, and the network package knows nothing of the game.
 * <p>
 * Every question gets a number and a reply counts only for its own number, so a reply that arrives after the question was
 * cancelled, for example when the Nope window closed, is ignored and can never answer a later question. If the connection
 * is lost, or a player does not answer within the time allowed, the player is treated as gone: every question after that
 * fails at once and the engine falls back to its default, which is to pass.
 */
public final class RemotePlayer implements PlayerPort, GameListener, AutoCloseable {

    /** How long a player may take to answer a question that has no time limit of its own. */
    public static final Duration DEFAULT_ANSWER_TIMEOUT = Duration.ofMinutes(5);

    private final Connection connection;
    private final Duration answerTimeout;
    private final AtomicInteger lastId = new AtomicInteger();
    private final Map<Integer, CompletableFuture<Integer>> pending = new ConcurrentHashMap<>();
    private volatile boolean open = true;

    public RemotePlayer(Connection connection) {
        this(connection, DEFAULT_ANSWER_TIMEOUT);
    }

    public RemotePlayer(Connection connection, Duration answerTimeout) {
        this.connection = connection;
        this.answerTimeout = answerTimeout;
        Thread reader = new Thread(this::readReplies, "remote-player-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readReplies() {
        try {
            while (true) {
                Message message = connection.receive();
                if (message instanceof Message.Reply reply) {
                    CompletableFuture<Integer> question = pending.get(reply.id());
                    if (question != null) {
                        question.complete(reply.choice());
                    } // a reply to a question that is no longer open is ignored
                } else if (message instanceof Message.Goodbye) {
                    break;
                }
            }
        } catch (IOException e) {
            // the connection ended or broke
        } finally {
            markGone();
        }
    }

    private void markGone() {
        open = false;
        pending.values().forEach(question -> question.completeExceptionally(new IOException("connection lost")));
    }

    /** Whether the player is still connected. */
    public boolean isConnected() {
        return open;
    }

    @Override
    public int choose(Decision decision) {
        requireOpen();
        int id = lastId.incrementAndGet();
        CompletableFuture<Integer> question = new CompletableFuture<>();
        pending.put(id, question);
        try {
            requireOpen(); // the connection may have been lost just before the question was registered
            connection.send(new Message.Ask(id, decision.kind().id(), decision.options().stream()
                    .map(option -> new WireOption(option.code(), option.args())).toList()));
            return question.get(answerTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            withdraw(id);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("the question was cancelled", e);
        } catch (TimeoutException e) {
            close(); // a player who does not answer would hold up the whole game
            throw new IllegalStateException("the player did not answer in time", e);
        } catch (ExecutionException | IOException e) {
            throw new IllegalStateException("the player is no longer connected", e);
        } finally {
            pending.remove(id);
        }
    }

    private void requireOpen() {
        if (!open) {
            throw new IllegalStateException("the player is no longer connected");
        }
    }

    /** Tells the client that a question is no longer wanted, so that it can take the prompt away. */
    private void withdraw(int id) {
        try {
            connection.send(new Message.Cancel(id));
        } catch (IOException e) {
            // the client is gone: nothing to cancel
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        try {
            connection.send(new Message.Notice(event.code(), event.args()));
        } catch (IOException e) {
            markGone();
        }
    }

    @Override
    public void close() {
        if (open) {
            try {
                connection.send(new Message.Goodbye("the game is over"));
            } catch (IOException e) {
                // already gone
            }
        }
        markGone();
        connection.close();
    }
}
