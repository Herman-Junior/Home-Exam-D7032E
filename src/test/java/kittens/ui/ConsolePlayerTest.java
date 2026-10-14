package kittens.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

class ConsolePlayerTest {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);

    private static final Decision TURN = new Decision(DecisionKind.TURN_ACTION,
            List.of(Option.of("pass"), Option.of("play-card", "skip")));

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private ConsolePlayer playerTyping(String input) {
        return new ConsolePlayer(new PlayerId(0), new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out);
    }

    @Test
    void theOptionsAreListedFromOneAndTheAnswerIsTheNumberTyped() {
        int choice = playerTyping("2\n").choose(TURN);

        assertEquals(1, choice);
        assertTrue(output().contains("What do you want to do?"));
        assertTrue(output().contains("1) Pass (draw a card and end your turn)"));
        assertTrue(output().contains("2) Play skip"));
    }

    @Test
    void successiveQuestionsReadSuccessiveLines() {
        ConsolePlayer player = playerTyping("2\n1\n");
        assertEquals(1, player.choose(TURN));
        assertEquals(0, player.choose(TURN));
    }

    @Test
    void somethingThatIsNotANumberIsAnInvalidAnswerForTheEngineToRefuse() {
        assertEquals(-1, playerTyping("abc\n").choose(TURN));
        assertFalse(new Decision(DecisionKind.NOPE, List.of(Option.of("nope"))).accepts(-1));
    }

    @Test
    void aNumberOutsideTheListIsPassedOnAsTyped() {
        assertEquals(8, playerTyping("9\n").choose(TURN));
        assertEquals(-1, playerTyping("0\n").choose(TURN));
    }

    @Test
    void aPositionInTheDeckIsTypedAsTheNumberItselfAndTheOptionsAreNotListed() {
        Decision position = new Decision(DecisionKind.DEFUSE_POSITION,
                List.of(Option.of("position", "0"), Option.of("position", "1"), Option.of("position", "2")));

        int choice = playerTyping("2\n").choose(position);

        assertEquals(2, choice);
        assertTrue(output().contains("from 0 (the top) to 2 (the bottom)"));
        assertFalse(output().contains("1) "));
    }

    @Test
    void eventsArePrintedAsText() {
        ConsolePlayer player = playerTyping("");
        player.onEvent(GameEvent.of("card-drawn", "taco-cat"));
        assertTrue(output().contains("You drew: taco cat"));
    }

    @Test
    void whenTheInputEndsTheQuestionFailsInsteadOfWaitingForever() {
        ConsolePlayer player = playerTyping("");
        assertThrows(IllegalStateException.class, () -> player.choose(TURN));
        assertThrows(IllegalStateException.class, () -> player.choose(TURN));
    }

    @Test
    void aQuestionThatIsInterruptedStopsAtOnceAndLeavesNoReaderBehindToSwallowTheNextLine() throws Exception {
        PipedOutputStream typing = new PipedOutputStream();
        ConsolePlayer player = new ConsolePlayer(new PlayerId(0), new PipedInputStream(typing), out);

        // the timed question: nobody types, and the engine interrupts it when the time is up
        AtomicReference<Throwable> first = new AtomicReference<>();
        Thread timed = new Thread(() -> {
            try {
                player.choose(TURN);
            } catch (RuntimeException e) {
                first.set(e);
            }
        });
        timed.start();
        Thread.sleep(200);
        timed.interrupt();
        timed.join(3000);
        assertFalse(timed.isAlive(), "the interrupted question is still waiting");
        assertTrue(first.get() instanceof IllegalStateException);

        // a line typed too late to count must not answer the next question
        write(typing, "9\n");
        Thread.sleep(300);
        AtomicReference<Integer> second = new AtomicReference<>();
        Thread next = new Thread(() -> second.set(player.choose(TURN)));
        next.start();
        Thread.sleep(300);
        write(typing, "2\n");
        next.join(3000);

        assertEquals(1, second.get());
    }

    private static void write(PipedOutputStream stream, String text) throws IOException {
        stream.write(text.getBytes(StandardCharsets.UTF_8));
        stream.flush();
    }
}
