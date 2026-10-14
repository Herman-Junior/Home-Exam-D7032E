package kittens.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** The handshake and the transport, over real sockets on this computer. */
@Timeout(30)
class ServerClientTest {

    private static CompletableFuture<Connection> accept(Server server, int playerNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return server.acceptPlayer(playerNumber);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void aClientIsWelcomedAndToldWhichPlayerItIs() throws Exception {
        try (Server server = new Server(0)) {
            CompletableFuture<Connection> accepted = accept(server, 3);

            Client.Joined joined = Client.connect("127.0.0.1", server.port());
            Connection atServer = accepted.get(5, TimeUnit.SECONDS);

            assertEquals(3, joined.playerNumber());
            atServer.send(new Message.Notice("hello", List.of("world")));
            assertEquals(new Message.Notice("hello", List.of("world")), joined.connection().receive());
            joined.connection().send(new Message.Reply(1, 2));
            assertEquals(new Message.Reply(1, 2), atServer.receive());
            atServer.close();
            joined.connection().close();
        }
    }

    @Test
    void aClientThatSpeaksAnotherVersionIsTurnedAwayAndTheServerKeepsWaitingForTheRightOne() throws Exception {
        try (Server server = new Server(0)) {
            CompletableFuture<Connection> accepted = accept(server, 1);

            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                SocketConnection oldClient = new SocketConnection(socket);
                oldClient.send(new Message.Hello(Protocol.VERSION + 98));
                Message reply = oldClient.receive();
                assertTrue(reply instanceof Message.Rejected rejected && rejected.reason().contains("version"), "" + reply);
            }
            assertTrue(!accepted.isDone(), "the refused client took the place of a player");

            Client.Joined joined = Client.connect("127.0.0.1", server.port());
            assertEquals(1, joined.playerNumber());
            accepted.get(5, TimeUnit.SECONDS).close();
            joined.connection().close();
        }
    }

    @Test
    void aProgramThatSendsRubbishIsDroppedAndDoesNotBlockTheOthers() throws Exception {
        try (Server server = new Server(0)) {
            CompletableFuture<Connection> accepted = accept(server, 1);

            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: x\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                assertEquals(-1, socket.getInputStream().read()); // the server closed it
            }
            Client.Joined joined = Client.connect("127.0.0.1", server.port());
            accepted.get(5, TimeUnit.SECONDS).close();
            joined.connection().close();
        }
    }

    @Test
    void aProgramThatConnectsAndSaysNothingIsDroppedAfterAShortWhile() throws Exception {
        try (Server server = new Server(0, 300)) {
            CompletableFuture<Connection> accepted = accept(server, 1);

            try (Socket silent = new Socket("127.0.0.1", server.port())) {
                Client.Joined joined = Client.connect("127.0.0.1", server.port());
                accepted.get(5, TimeUnit.SECONDS).close();
                joined.connection().close();
            }
        }
    }

    @Test
    void aClientIsToldWhyItWasRefused() throws Exception {
        try (ServerSocket fake = new ServerSocket(0)) {
            Thread thread = new Thread(() -> {
                try (Socket socket = fake.accept()) {
                    SocketConnection connection = new SocketConnection(socket);
                    connection.receive();
                    connection.send(new Message.Rejected("the game is full"));
                } catch (IOException e) {
                    // the test will fail on its own
                }
            });
            thread.setDaemon(true);
            thread.start();

            ProtocolException refused = assertThrows(ProtocolException.class,
                    () -> Client.connect("127.0.0.1", fake.getLocalPort()));
            assertTrue(refused.getMessage().contains("the game is full"), refused.getMessage());
        }
    }

    @Test
    void aClientThatCannotReachTheServerGetsAnError() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        assertThrows(IOException.class, () -> Client.connect("127.0.0.1", closedPort));
    }

    @Test
    void closingTheServerEndsAWaitingAccept() throws Exception {
        Server server = new Server(0);
        CompletableFuture<Connection> accepted = accept(server, 1);
        Thread.sleep(200);
        server.close();
        assertThrows(Exception.class, () -> accepted.get(5, TimeUnit.SECONDS));
    }

    @Test
    void aClosedConnectionEndsTheOthersReceive() throws Exception {
        try (Server server = new Server(0)) {
            CompletableFuture<Connection> accepted = accept(server, 1);
            Client.Joined joined = Client.connect("127.0.0.1", server.port());
            Connection atServer = accepted.get(5, TimeUnit.SECONDS);

            joined.connection().close();

            assertThrows(IOException.class, atServer::receive);
            atServer.close();
        }
    }
}
