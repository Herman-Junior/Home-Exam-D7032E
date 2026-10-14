package kittens.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The listening side. It waits for clients and lets in only those that greet properly with the same protocol version.
 * A program that connects and sends nothing, sends rubbish, or speaks another version is turned away without holding
 * up the players who are waiting behind it.
 */
public final class Server implements AutoCloseable {

    /** The port of the original game. */
    public static final int DEFAULT_PORT = 2048;

    private static final int DEFAULT_GREETING_TIMEOUT_MILLIS = 5000;

    private final ServerSocket socket;
    private final int greetingTimeoutMillis;

    /** Listens on {@code port}; 0 lets the system choose a free one, which {@link #port()} then tells. */
    public Server(int port) throws IOException {
        this(port, DEFAULT_GREETING_TIMEOUT_MILLIS);
    }

    /** {@code greetingTimeoutMillis} is how long a new client may take to send its greeting. */
    public Server(int port, int greetingTimeoutMillis) throws IOException {
        this.socket = new ServerSocket(port);
        this.greetingTimeoutMillis = greetingTimeoutMillis;
    }

    public int port() {
        return socket.getLocalPort();
    }

    /**
     * Waits until a client has connected and greeted properly, welcomes it as player number {@code playerNumber} and
     * returns the connection. Clients that do not manage to greet properly are dropped and the wait goes on.
     *
     * @throws IOException if the server is closed while waiting
     */
    public Connection acceptPlayer(int playerNumber) throws IOException {
        while (true) {
            Socket client = socket.accept();
            SocketConnection connection = new SocketConnection(client);
            try {
                connection.setReadTimeout(greetingTimeoutMillis);
                Message first = connection.receive();
                if (!(first instanceof Message.Hello hello)) {
                    refuse(connection, "the first message must be a greeting");
                } else if (hello.version() != Protocol.VERSION) {
                    refuse(connection, "this server speaks protocol version " + Protocol.VERSION
                            + ", the client speaks " + hello.version());
                } else {
                    connection.send(new Message.Welcome(Protocol.VERSION, playerNumber));
                    connection.setReadTimeout(0);
                    return connection;
                }
            } catch (IOException e) {
                connection.close(); // silent, broken or too slow: the next client may come
            }
        }
    }

    private static void refuse(SocketConnection connection, String reason) {
        try {
            connection.send(new Message.Rejected(reason));
        } catch (IOException e) {
            // the client is gone already
        }
        connection.close();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // nothing more to do
        }
    }
}
