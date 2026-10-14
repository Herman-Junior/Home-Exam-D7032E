package kittens.net;

import java.io.IOException;

/**
 * A line between two programs that carries {@link Message}s in both directions. What is behind it, a socket or, in a
 * test, two queues in memory, does not matter to whoever uses it. {@link #receive()} waits for the next message and
 * fails with an {@link IOException} once the connection is closed or broken.
 */
public interface Connection extends AutoCloseable {

    void send(Message message) throws IOException;

    Message receive() throws IOException;

    /** Closes the connection. A {@link #receive()} that is waiting fails. Closing twice does no harm. */
    @Override
    void close();
}
