package kittens.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerPort;
import kittens.net.Message;
import kittens.net.WireOption;
import kittens.testing.MemoryConnection;
import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/** The player's own side: messages from the server become calls on a local player, and the answers go back. */
@Timeout(30)
class RemoteClientTest {

    private final MemoryConnection[] line = MemoryConnection.pair();
    private final MemoryConnection server = line[0];
    private final RecordingListener listener = new RecordingListener();
    private Thread running;

    @BeforeEach
    void nothingYet() {
        running = null;
    }

    @AfterEach
    void stop() throws InterruptedException {
        server.close();
        if (running != null) {
            running.join(5000);
        }
    }

    private void start(PlayerPort port) {
        RemoteClient client = new RemoteClient(line[1], port, listener);
        running = new Thread(client::run, "test-client");
        running.setDaemon(true);
        running.start();
    }

    private static Message.Ask ask(int id, String kind, WireOption... options) {
        return new Message.Ask(id, kind, List.of(options));
    }

    @Test
    void aNoticeBecomesAnEventForTheListener() throws Exception {
        start(ScriptedPlayer.indexes());
        server.send(new Message.Notice("card-played", List.of("1", "skip")));
        server.send(new Message.Goodbye("done"));
        running.join(5000);
        assertEquals(List.of(GameEvent.of("card-played", "1", "skip")), listener.events());
    }

    @Test
    void aQuestionIsPutToThePortAndTheAnswerIsSentBackWithTheQuestionsNumber() throws Exception {
        ScriptedPlayer port = ScriptedPlayer.indexes(1);
        start(port);

        server.send(ask(7, "turn-action", new WireOption("pass", List.of()), new WireOption("play-card", List.of("skip"))));

        assertEquals(new Message.Reply(7, 1), server.receiveWithin(5000));
        assertEquals(List.of(new Decision(DecisionKind.TURN_ACTION,
                List.of(Option.of("pass"), Option.of("play-card", "skip")))), port.asked());
    }

    @Test
    void aKindOfQuestionThatNobodyKnowsIsStillPutToThePort() throws Exception {
        ScriptedPlayer port = ScriptedPlayer.indexes(0);
        start(port);
        server.send(ask(1, "alter-the-future", new WireOption("order", List.of("a", "b"))));
        assertEquals(new Message.Reply(1, 0), server.receiveWithin(5000));
        assertEquals(new DecisionKind("alter-the-future"), port.asked().get(0).kind());
    }

    @Test
    void aQuestionThatCannotBeUnderstoodGetsAnAnswerThatIsRefusedInsteadOfSilence() throws Exception {
        start(ScriptedPlayer.indexes());
        server.send(ask(3, "turn-action")); // no options
        assertEquals(new Message.Reply(3, -1), server.receiveWithin(5000));
    }

    /** A port that waits until it is interrupted, and notes that it was. */
    private static final class WaitingPort implements PlayerPort {
        final CountDownLatch asked = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);

        @Override
        public int choose(Decision decision) {
            asked.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
            return 0;
        }
    }

    @Test
    void aCancelInterruptsTheQuestionAtOnceAndSendsNoAnswer() throws Exception {
        WaitingPort port = new WaitingPort();
        start(port);
        server.send(ask(1, "nope", new WireOption("nope", List.of()), new WireOption("pass", List.of())));
        assertTrue(port.asked.await(5, TimeUnit.SECONDS));

        server.send(new Message.Cancel(1));

        assertTrue(port.interrupted.await(5, TimeUnit.SECONDS), "the question was not interrupted");
        assertNull(server.receiveWithin(300), "a cancelled question must not be answered");
    }

    @Test
    void aCancelForAnotherQuestionIsIgnored() throws Exception {
        WaitingPort port = new WaitingPort();
        start(port);
        server.send(ask(2, "nope", new WireOption("nope", List.of())));
        assertTrue(port.asked.await(5, TimeUnit.SECONDS));

        server.send(new Message.Cancel(99));

        assertFalse(port.interrupted.await(300, TimeUnit.MILLISECONDS));
    }

    @Test
    void aNewQuestionTakesTheOldOneAwayAndOnlyTheNewOneIsAnswered() throws Exception {
        AtomicBoolean first = new AtomicBoolean(true);
        CountDownLatch firstInterrupted = new CountDownLatch(1);
        CountDownLatch firstAsked = new CountDownLatch(1);
        start(decision -> {
            if (first.getAndSet(false)) {
                firstAsked.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    firstInterrupted.countDown();
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted", e);
                }
            }
            return 1;
        });
        server.send(ask(1, "nope", new WireOption("nope", List.of()), new WireOption("pass", List.of())));
        assertTrue(firstAsked.await(5, TimeUnit.SECONDS));

        server.send(ask(2, "nope", new WireOption("nope", List.of()), new WireOption("pass", List.of())));

        assertTrue(firstInterrupted.await(5, TimeUnit.SECONDS));
        assertEquals(new Message.Reply(2, 1), server.receiveWithin(5000));
        assertNull(server.receiveWithin(300));
    }

    @Test
    void aGoodbyeEndsTheClientAndClosesTheConnection() throws Exception {
        start(ScriptedPlayer.indexes());
        server.send(new Message.Goodbye("the game is over"));
        running.join(5000);
        assertFalse(running.isAlive());
        assertThrowsIo();
    }

    @Test
    void aLostConnectionEndsTheClient() throws Exception {
        start(ScriptedPlayer.indexes());
        server.close();
        running.join(5000);
        assertFalse(running.isAlive());
    }

    @Test
    void aListenerThatFailsDoesNotEndTheGameForThePlayer() throws Exception {
        RemoteClient client = new RemoteClient(line[1], ScriptedPlayer.indexes(0), event -> {
            throw new IllegalStateException("cannot show this");
        });
        running = new Thread(client::run);
        running.setDaemon(true);
        running.start();
        server.send(new Message.Notice("anything", List.of()));
        server.send(ask(1, "turn-action", new WireOption("pass", List.of())));
        assertEquals(new Message.Reply(1, 0), server.receiveWithin(5000));
    }

    private void assertThrowsIo() {
        try {
            server.send(new Message.Cancel(1));
            org.junit.jupiter.api.Assertions.fail("the connection should be closed");
        } catch (IOException expected) {
            // closed, as intended
        }
    }
}
