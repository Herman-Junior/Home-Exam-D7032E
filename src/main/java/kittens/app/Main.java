package kittens.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import kittens.api.RandomSource;
import kittens.engine.Game;
import kittens.engine.JavaRandomSource;
import kittens.net.Server;

/**
 * Entry point. Keeps the command line of the original game:
 * <pre>
 *   server: java -jar exploding-kittens.jar [numPlayers] [numBots]
 *   client: java -jar exploding-kittens.jar [server IP]
 * </pre>
 * The server hosts one human at its own console, {@code numPlayers - 1} humans who join from other computers, and
 * {@code numBots} computer players. With {@code numPlayers} 1 no network is used.
 */
public final class Main {

    private Main() {
    }

    static String usage() {
        return "Server syntax: java -jar exploding-kittens.jar numPlayers numBots\n"
             + "Client syntax: java -jar exploding-kittens.jar serverIP[:port]\n"
             + "A local game against computer players: java -jar exploding-kittens.jar 1 numBots\n"
             + "(numPlayers counts the humans, including the one at the server; the game needs 2 to 5 players in all)";
    }

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, new JavaRandomSource()));
    }

    static int run(String[] args, InputStream in, PrintStream out, RandomSource random) {
        return run(args, in, out, random, Server.DEFAULT_PORT);
    }

    /**
     * Runs the program. The input, the output and the source of chance are passed in, and so is the port, so that a test
     * can run a whole game, also over the network.
     *
     * @return the exit code: 0 after a game, 1 if the game could not be started or joined, 2 for a command line that is
     *         not understood
     */
    static int run(String[] args, InputStream in, PrintStream out, RandomSource random, int port) {
        if (args.length == 1) {
            return join(args[0], port, in, out);
        }
        if (args.length != 2) {
            out.println(usage());
            return 2;
        }
        int players;
        int bots;
        try {
            players = Integer.parseInt(args[0]);
            bots = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            out.println(usage());
            return 2;
        }
        if (players < 1 || bots < 0) {
            out.println(usage());
            return 2;
        }
        return players == 1 ? playLocal(bots, in, out, random) : host(players - 1, bots, port, in, out, random);
    }

    private static int join(String address, int defaultPort, InputStream in, PrintStream out) {
        String host = address;
        int port = defaultPort;
        int colon = address.lastIndexOf(':');
        if (colon > 0) {
            host = address.substring(0, colon);
            try {
                port = Integer.parseInt(address.substring(colon + 1));
            } catch (NumberFormatException e) {
                out.println(usage());
                return 2;
            }
        }
        return RemoteGame.join(host, port, in, out);
    }

    private static int playLocal(int bots, InputStream in, PrintStream out, RandomSource random) {
        Game game;
        try {
            game = GameBuilder.local(bots, in, out, random);
        } catch (IllegalArgumentException e) {
            out.println("Cannot start the game: " + e.getMessage() + ".");
            return 1;
        }
        game.play();
        return 0;
    }

    private static int host(int clients, int bots, int port, InputStream in, PrintStream out, RandomSource random) {
        try (Server server = new Server(port)) {
            try (GameBuilder.Built built = GameBuilder.hosted(clients, bots, server, in, out, random)) {
                built.game().play();
            }
            return 0;
        } catch (IllegalArgumentException e) {
            out.println("Cannot start the game: " + e.getMessage() + ".");
            return 1;
        } catch (IOException e) {
            out.println("Cannot host the game: " + e.getMessage());
            return 1;
        }
    }
}
