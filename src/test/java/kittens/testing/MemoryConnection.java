package kittens.testing;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import kittens.net.Connection;
import kittens.net.Message;

/**
 * One end of a {@link Connection} that lives in memory, for testing the code on either side of a network without a
 * network. {@link #pair()} gives the two ends: what is sent on one is received on the other. Closing either end makes
 * the {@code receive()} of both end with an {@link EOFException}, as with a socket.
 */
public final class MemoryConnection implements Connection {

    private static final Object CLOSED = new Object();

    private final BlockingQueue<Object> incoming = new LinkedBlockingQueue<>();
    private MemoryConnection peer;
    private volatile boolean closed;

    private MemoryConnection() {
    }

    public static MemoryConnection[] pair() {
        MemoryConnection a = new MemoryConnection();
        MemoryConnection b = new MemoryConnection();
        a.peer = b;
        b.peer = a;
        return new MemoryConnection[] {a, b};
    }

    @Override
    public void send(Message message) throws IOException {
        if (closed || peer.closed) {
            throw new IOException("the connection is closed");
        }
        peer.incoming.add(message);
    }

    @Override
    public Message receive() throws IOException {
        try {
            Object next = incoming.take();
            if (next == CLOSED) {
                incoming.add(CLOSED); // every later receive ends too
                throw new EOFException("the connection is closed");
            }
            return (Message) next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for a message", e);
        }
    }

    /** Waits for the next message for at most {@code millis}, and returns null if none comes. For tests. */
    public Message receiveWithin(long millis) throws IOException {
        try {
            Object next = incoming.poll(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (next == null) {
                return null;
            }
            if (next == CLOSED) {
                incoming.add(CLOSED);
                throw new EOFException("the connection is closed");
            }
            return (Message) next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for a message", e);
        }
    }

    @Override
    public void close() {
        closed = true;
        incoming.add(CLOSED);
        peer.incoming.add(CLOSED);
    }
}
