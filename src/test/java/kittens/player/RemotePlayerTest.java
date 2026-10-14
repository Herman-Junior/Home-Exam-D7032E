package kittens.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.net.Message;
import kittens.net.WireOption;
import kittens.testing.MemoryConnection;

/** The server's side of a remote player: the engine's calls become messages and the replies become answers. */
@Timeout(30)
class RemotePlayerTest {

    private static final Decision TURN = new Decision(DecisionKind.TURN_ACTION,
            List.of(Option.of("pass"), Option.of("play-card", "skip")));

    private final MemoryConnection[] line = MemoryConnection.pair();
    private final MemoryConnection client = line[1];
    private RemotePlayer player = new RemotePlayer(line[0]);

    @AfterEach
    void closeEverything() {
        player.close();
    }

    private CompletableFuture<Integer> chooseInBackground(RemotePlayer remote, Decision decision) {
        return CompletableFuture.supplyAsync(() -> remote.choose(decision));
    }

    private Message.Ask expectAsk() throws IOException {
        Message message = client.receiveWithin(5000);
        assertNotNull(message, "no question was sent");
        assertTrue(message instanceof Message.Ask, "expected a question but got " + message);
        return (Message.Ask) message;
    }

    @Test
    void aQuestionIsSentAsATypedMessageAndTheReplyIsTheAnswer() throws Exception {
        CompletableFuture<Integer> answer = chooseInBackground(player, TURN);

        Message.Ask ask = expectAsk();
        assertEquals("turn-action", ask.kind());
        assertEquals(List.of(new WireOption("pass", List.of()), new WireOption("play-card", List.of("skip"))),
                ask.options());
        client.send(new Message.Reply(ask.id(), 1));

        assertEquals(1, answer.get(5, TimeUnit.SECONDS));
    }

    @Test
    void aReplyToAQuestionThatIsNotOpenIsIgnored() throws Exception {
        CompletableFuture<Integer> answer = chooseInBackground(player, TURN);
        Message.Ask ask = expectAsk();

        client.send(new Message.Reply(ask.id() + 50, 0));
        client.send(new Message.Reply(ask.id(), 1));

        assertEquals(1, answer.get(5, TimeUnit.SECONDS));
    }

    @Test
    void aQuestionThatIsInterruptedIsCancelledAtTheClientAndItsLateReplyCannotAnswerTheNextQuestion() throws Exception {
        Thread asker = new Thread(() -> {
            try {
                player.choose(TURN);
            } catch (IllegalStateException e) {
                // cancelled, as intended
            }
        });
        asker.start();
        Message.Ask first = expectAsk();
        asker.interrupt();
        asker.join(5000);
        assertFalse(asker.isAlive());
        assertEquals(new Message.Cancel(first.id()), client.receiveWithin(5000));

        CompletableFuture<Integer> second = chooseInBackground(player, TURN);
        Message.Ask ask = expectAsk();
        assertTrue(ask.id() != first.id());
        client.send(new Message.Reply(first.id(), 0)); // too late: the first question is long gone
        client.send(new Message.Reply(ask.id(), 1));

        assertEquals(1, second.get(5, TimeUnit.SECONDS));
    }

    @Test
    void eventsAreSentAsNotices() throws Exception {
        player.onEvent(GameEvent.of("card-played", "1", "skip"));
        assertEquals(new Message.Notice("card-played", List.of("1", "skip")), client.receiveWithin(5000));
    }

    @Test
    void whenTheConnectionIsLostAWaitingQuestionFailsAndEveryLaterOneFailsAtOnce() throws Exception {
        CompletableFuture<Integer> waiting = chooseInBackground(player, TURN);
        expectAsk();

        client.close();

        ExecutionException failed = assertThrows(ExecutionException.class, () -> waiting.get(5, TimeUnit.SECONDS));
        assertTrue(failed.getCause() instanceof IllegalStateException);
        assertThrows(IllegalStateException.class, () -> player.choose(TURN));
        assertFalse(player.isConnected());
    }

    @Test
    void aPlayerWhoDoesNotAnswerInTimeIsDroppedSoTheGameIsNotHeldUp() throws Exception {
        player.close();
        MemoryConnection[] another = MemoryConnection.pair();
        player = new RemotePlayer(another[0], Duration.ofMillis(300));

        assertThrows(IllegalStateException.class, () -> player.choose(TURN));
        assertFalse(player.isConnected());
        long start = System.nanoTime();
        assertThrows(IllegalStateException.class, () -> player.choose(TURN));
        assertTrue((System.nanoTime() - start) / 1_000_000 < 200, "the second question waited again");
    }

    @Test
    void aGoodbyeFromTheClientEndsTheConnection() throws Exception {
        client.send(new Message.Goodbye("I am leaving"));
        for (int i = 0; i < 100 && player.isConnected(); i++) {
            Thread.sleep(20);
        }
        assertFalse(player.isConnected());
    }

    @Test
    void closingSaysGoodbyeToTheClient() throws Exception {
        player.close();
        assertEquals(new Message.Goodbye("the game is over"), client.receiveWithin(5000));
    }

    @Test
    void eventsToAPlayerWhoHasGoneAreIgnoredAndDoNotFail() {
        client.close();
        player.onEvent(GameEvent.of("anything"));
        player.onEvent(GameEvent.of("anything"));
    }
}
