package kittens.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.api.Answer;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.Option;
import kittens.api.PlayerId;
import kittens.api.PlayerPort;
import kittens.testing.ScriptedPlayer;

/** The timed question: all players are asked at once and only the answers that come in time count. */
class PlayerChannelTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);
    private static final Decision NOPE = new Decision(DecisionKind.NOPE, List.of(Option.of("nope"), Option.of("pass")));

    private static PlayerPort after(long millis, int choice) {
        return decision -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return choice;
        };
    }

    private static PlayerChannel channel(PlayerPort... ports) {
        return new PlayerChannel(java.util.Arrays.stream(ports).map(Participant::of).toList());
    }

    @Test
    void answersComeBackInTheOrderTheyArrive() {
        PlayerChannel channel = channel(after(300, 0), after(0, 1), after(150, 0));

        List<Answer> answers = channel.askAll(List.of(P0, P1, P2), NOPE, Duration.ofSeconds(5));

        assertEquals(List.of(new Answer(P1, 1), new Answer(P2, 0), new Answer(P0, 0)), answers);
    }

    @Test
    void aPlayerWhoIsTooSlowIsLeftOut() {
        PlayerChannel channel = channel(after(0, 0), after(3000, 0));

        long start = System.nanoTime();
        List<Answer> answers = channel.askAll(List.of(P0, P1), NOPE, Duration.ofMillis(300));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(List.of(new Answer(P0, 0)), answers);
        assertTrue(elapsedMillis < 2000, "waited " + elapsedMillis + " ms for a player who was too slow");
    }

    @Test
    void itDoesNotWaitForTheWholeLimitWhenEveryoneHasAnswered() {
        PlayerChannel channel = channel(after(0, 0), after(0, 1));

        long start = System.nanoTime();
        List<Answer> answers = channel.askAll(List.of(P0, P1), NOPE, Duration.ofSeconds(10));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(2, answers.size());
        assertTrue(elapsedMillis < 2000, "waited " + elapsedMillis + " ms although everyone had answered");
    }

    @Test
    void aBadAnswerOrAFailingPortCountsAsNoAnswer() {
        PlayerPort broken = decision -> {
            throw new IllegalStateException("connection lost");
        };
        PlayerChannel channel = channel(after(0, 99), broken, after(0, 1));

        List<Answer> answers = channel.askAll(List.of(P0, P1, P2), NOPE, Duration.ofSeconds(5));

        assertEquals(List.of(new Answer(P2, 1)), answers);
    }

    @Test
    void askingNobodyReturnsNothing() {
        assertTrue(channel(ScriptedPlayer.indexes()).askAll(List.of(), NOPE, Duration.ofMillis(50)).isEmpty());
    }

    @Test
    void aSingleQuestionIsAskedAgainAfterABadAnswer() {
        ScriptedPlayer player = ScriptedPlayer.indexes(7, 1);
        assertEquals(1, channel(player).ask(P0, NOPE));
        assertEquals(2, player.asked().size());
    }
}
