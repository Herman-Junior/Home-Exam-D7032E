package kittens.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** The connecting side: finds the server, greets it, and learns which player it is. */
public final class Client {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int WELCOME_TIMEOUT_MILLIS = 30_000;

    /** A connection that the server has welcomed, and the player number it gave. */
    public record Joined(Connection connection, int playerNumber) {
    }

    private Client() {
    }

    /**
     * Connects to the server and greets it.
     *
     * @throws IOException if the server cannot be reached or does not let the client in. A refusal is a
     *         {@link ProtocolException} that carries the server's reason.
     */
    public static Joined connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        SocketConnection connection = null;
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            connection = new SocketConnection(socket);
            connection.send(new Message.Hello(Protocol.VERSION));
            connection.setReadTimeout(WELCOME_TIMEOUT_MILLIS);
            Message reply = connection.receive();
            connection.setReadTimeout(0);
            if (reply instanceof Message.Welcome welcome) {
                return new Joined(connection, welcome.playerNumber());
            }
            if (reply instanceof Message.Rejected rejected) {
                throw new ProtocolException("the server refused: " + rejected.reason());
            }
            throw new ProtocolException("the server did not welcome the client");
        } catch (IOException e) {
            if (connection != null) {
                connection.close();
            } else {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // nothing more to do
                }
            }
            throw e;
        }
    }
}
