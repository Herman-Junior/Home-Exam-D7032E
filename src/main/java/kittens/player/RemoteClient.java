package kittens.player;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.GameListener;
import kittens.api.Option;
import kittens.api.PlayerPort;
import kittens.net.Connection;
import kittens.net.Message;

/**
 * The other end of a {@link RemotePlayer}: it runs on the player's own computer and plays a local {@link PlayerPort}, the
 * console, in a game that runs on the server. Notices become events for the listener, and a question is put to the port
 * and the answer sent back with the number of the question.
 * <p>
 * A question runs on a thread of its own, so the client keeps listening while the player thinks. If the server cancels
 * the question, or asks a new one, the old question is interrupted at once, so that a Nope prompt does not linger after
 * the time is up.
 */
public final class RemoteClient {

    private final Connection connection;
    private final PlayerPort port;
    private final GameListener listener;

    public RemoteClient(Connection connection, PlayerPort port, GameListener listener) {
        this.connection = connection;
        this.port = port;
        this.listener = listener;
    }

    /** Listens until the game is over or the connection ends, and then closes the connection. */
    public void run() {
        ExecutorService questions = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "remote-question");
            thread.setDaemon(true);
            return thread;
        });
        int currentId = -1;
        Future<?> current = null;
        try {
            while (true) {
                Message message = connection.receive();
                if (message instanceof Message.Notice notice) {
                    tell(notice);
                } else if (message instanceof Message.Ask ask) {
                    if (current != null) {
                        current.cancel(true);
                    }
                    currentId = ask.id();
                    current = questions.submit(() -> answer(ask));
                } else if (message instanceof Message.Cancel cancel) {
                    if (current != null && cancel.id() == currentId) {
                        current.cancel(true);
                    }
                } else if (message instanceof Message.Goodbye) {
                    return;
                }
            }
        } catch (IOException e) {
            // the connection ended or broke: the game is over for this player
        } finally {
            if (current != null) {
                current.cancel(true);
            }
            questions.shutdownNow();
            connection.close();
        }
    }

    private void tell(Message.Notice notice) {
        try {
            listener.onEvent(new GameEvent(notice.code(), notice.args()));
        } catch (RuntimeException e) {
            // a notice that cannot be shown must not end the game for this player
        }
    }

    /** Puts one question to the port and sends the reply. A question that is cancelled sends no reply. */
    private void answer(Message.Ask ask) {
        int choice;
        try {
            List<Option> options = ask.options().stream()
                    .map(option -> new Option(option.code(), option.args())).toList();
            choice = port.choose(new Decision(new DecisionKind(ask.kind()), options));
        } catch (IllegalArgumentException e) {
            choice = -1; // a question that cannot be understood gets an answer that is refused, not silence
        } catch (RuntimeException e) {
            return; // cancelled, or the port failed: no reply
        }
        try {
            connection.send(new Message.Reply(ask.id(), choice));
        } catch (IOException e) {
            // the server is gone
        }
    }
}
