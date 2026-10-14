package kittens.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kittens.engine.JavaRandomSource;
import kittens.net.Client;
import kittens.net.Message;
import kittens.net.Protocol;
import kittens.net.SocketConnection;

/** Whole games between a host and clients, over real sockets on this computer, started through {@code Main}. */
@Timeout(120)
class NetworkGameTest {

    /** A console: what is typed, and what is printed. */
    private static final class Console {
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);

        /** The player types 1 whenever asked: pass on the turn, the first option everywhere else. */
        InputStream alwaysOne() {
            return new InputStream() {
                private int position;

                @Override
                public int read() {
                    return "1\n".charAt(position++ % 2);
                }

                @Override
                public int available() {
                    return 2;
                }
            };
        }

        String text() {
            return captured.toString(StandardCharsets.UTF_8);
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static CompletableFuture<Integer> runInBackground(Console console, int port, long seed, String... args) {
        return CompletableFuture.supplyAsync(
                () -> Main.run(args, console.alwaysOne(), console.out, new JavaRandomSource(new Random(seed)), port));
    }

    /** Waits until the host says that it is listening for a player. */
    private static void waitUntilListening(Console host) throws Exception {
        for (int i = 0; i < 500 && !host.text().contains("Waiting for player"); i++) {
            Thread.sleep(20);
        }
        assertTrue(host.text().contains("Waiting for player"), host.text());
    }

    @Test
    void aHostAndOneClientPlayAWholeGameWithABot() throws Exception {
        int port = freePort();
        Console host = new Console();
        Console guest = new Console();

        CompletableFuture<Integer> hosting = runInBackground(host, port, 11, "2", "1");
        waitUntilListening(host);
        CompletableFuture<Integer> joining = runInBackground(guest, port, 12, "127.0.0.1:" + port);

        assertEquals(0, hosting.get(60, TimeUnit.SECONDS), host.text());
        assertEquals(0, joining.get(60, TimeUnit.SECONDS), guest.text());
        // players: 0 is the host, 1 the bot, 2 the client
        assertTrue(host.text().contains("Player 2 has joined."), host.text());
        assertTrue(host.text().contains("won the game!"), host.text());
        assertTrue(guest.text().contains("Joined as player 2"), guest.text());
        assertTrue(guest.text().contains("Game started with 3 players"), guest.text());
        assertTrue(guest.text().contains("Your hand:"), guest.text());
        assertTrue(guest.text().contains("won the game!"), guest.text());
    }

    @Test
    void aHostAndTwoClientsPlayWithTheWinnerToldToEveryone() throws Exception {
        int port = freePort();
        Console host = new Console();
        Console first = new Console();
        Console second = new Console();

        CompletableFuture<Integer> hosting = runInBackground(host, port, 21, "3", "1");
        waitUntilListening(host);
        CompletableFuture<Integer> joiningFirst = runInBackground(first, port, 22, "127.0.0.1:" + port);
        for (int i = 0; i < 500 && !host.text().contains("Player 2 has joined."); i++) {
            Thread.sleep(20);
        }
        CompletableFuture<Integer> joiningSecond = runInBackground(second, port, 23, "127.0.0.1:" + port);

        assertEquals(0, hosting.get(60, TimeUnit.SECONDS), host.text());
        assertEquals(0, joiningFirst.get(60, TimeUnit.SECONDS), first.text());
        assertEquals(0, joiningSecond.get(60, TimeUnit.SECONDS), second.text());
        assertTrue(first.text().contains("Joined as player 2"), first.text());
        assertTrue(second.text().contains("Joined as player 3"), second.text());
        for (Console console : List.of(host, first, second)) {
            assertTrue(console.text().contains("won the game!"), console.text());
            assertTrue(console.text().contains("Game started with 4 players"), console.text());
        }
    }

    @Test
    void aClientThatDisconnectsRightAwayDoesNotStopTheGame() throws Exception {
        int port = freePort();
        Console host = new Console();

        CompletableFuture<Integer> hosting = runInBackground(host, port, 31, "2", "1");
        waitUntilListening(host);
        Client.Joined joined = Client.connect("127.0.0.1", port);
        joined.connection().close(); // gone before the game starts

        assertEquals(0, hosting.get(60, TimeUnit.SECONDS), host.text());
        assertTrue(host.text().contains("won the game!"), host.text());
    }

    @Test
    void aClientThatLeavesInTheMiddleOfTheGameDoesNotStopIt() throws Exception {
        int port = freePort();
        Console host = new Console();

        CompletableFuture<Integer> hosting = runInBackground(host, port, 41, "2", "2");
        waitUntilListening(host);
        Client.Joined joined = Client.connect("127.0.0.1", port);
        // listens until the game is under way and then simply leaves
        while (!(joined.connection().receive() instanceof Message.Ask)) {
            // skip the notices
        }
        joined.connection().close();

        assertEquals(0, hosting.get(60, TimeUnit.SECONDS), host.text());
        assertTrue(host.text().contains("won the game!"), host.text());
    }

    @Test
    void aClientWithAnotherProtocolVersionIsRefusedAndTheRightOneIsStillLetIn() throws Exception {
        int port = freePort();
        Console host = new Console();
        Console guest = new Console();

        CompletableFuture<Integer> hosting = runInBackground(host, port, 51, "2", "1");
        waitUntilListening(host);
        try (Socket socket = new Socket("127.0.0.1", port)) {
            SocketConnection old = new SocketConnection(socket);
            old.send(new Message.Hello(Protocol.VERSION + 1));
            assertTrue(old.receive() instanceof Message.Rejected);
        }
        CompletableFuture<Integer> joining = runInBackground(guest, port, 52, "127.0.0.1:" + port);

        assertEquals(0, hosting.get(60, TimeUnit.SECONDS), host.text());
        assertEquals(0, joining.get(60, TimeUnit.SECONDS), guest.text());
        assertTrue(guest.text().contains("won the game!"), guest.text());
    }

    @Test
    void aGameWithTooManyPlayersIsRefusedBeforeAnybodyIsWaitedFor() throws Exception {
        Console host = new Console();
        int exit = Main.run(new String[] {"5", "3"}, new ByteArrayInputStream(new byte[0]), host.out,
                new JavaRandomSource(), freePort());
        assertEquals(1, exit);
        assertTrue(host.text().contains("Cannot start the game: a game needs between 2 and 5 players"), host.text());
        assertTrue(!host.text().contains("Waiting for player"), host.text());
    }

    @Test
    void joiningAGameThatIsNotThereIsReportedAndEnds() throws Exception {
        Console guest = new Console();
        int exit = Main.run(new String[] {"127.0.0.1:" + freePort()}, guest.alwaysOne(), guest.out,
                new JavaRandomSource(), 2048);
        assertEquals(1, exit);
        assertTrue(guest.text().contains("Cannot join the game"), guest.text());
    }

    @Test
    void aBrokenPortInTheAddressPrintsTheUsage() {
        Console guest = new Console();
        assertEquals(2, Main.run(new String[] {"127.0.0.1:many"}, guest.alwaysOne(), guest.out,
                new JavaRandomSource(), 2048));
        assertTrue(guest.text().contains("Client syntax"), guest.text());
    }

    @Test
    void aPortThatIsAlreadyInUseIsReported() throws Exception {
        try (ServerSocket taken = new ServerSocket(0)) {
            Console host = new Console();
            int exit = Main.run(new String[] {"2", "1"}, host.alwaysOne(), host.out, new JavaRandomSource(),
                    taken.getLocalPort());
            assertEquals(1, exit);
            assertTrue(host.text().contains("Cannot host the game"), host.text());
        }
    }

    @Test
    void zeroOrNegativeNumbersPrintTheUsage() {
        Console console = new Console();
        List<Integer> codes = new ArrayList<>();
        codes.add(Main.run(new String[] {"0", "2"}, console.alwaysOne(), console.out, new JavaRandomSource()));
        codes.add(Main.run(new String[] {"1", "-1"}, console.alwaysOne(), console.out, new JavaRandomSource()));
        assertEquals(List.of(2, 2), codes);
    }
}
