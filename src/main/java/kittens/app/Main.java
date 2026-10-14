package kittens.app;

import java.io.InputStream;
import java.io.PrintStream;

import kittens.api.RandomSource;
import kittens.engine.Game;
import kittens.engine.JavaRandomSource;

/**
 * Entry point. Keeps the command line of the original game:
 * <pre>
 *   server: java -jar exploding-kittens.jar [numPlayers] [numBots]
 *   client: java -jar exploding-kittens.jar [server IP]
 * </pre>
 * Only a local game is available so far: one human ({@code numPlayers} is 1) against {@code numBots} computer players.
 * Playing over the network will use the same arguments once it is built.
 */
public final class Main {

    private Main() {
    }

    static String usage() {
        return "Server syntax: java -jar exploding-kittens.jar numPlayers numBots\n"
             + "Client syntax: java -jar exploding-kittens.jar serverIP\n"
             + "A local game against computer players: java -jar exploding-kittens.jar 1 numBots (1 to 4 bots)";
    }

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, new JavaRandomSource()));
    }

    /**
     * Runs the program. The input, the output and the source of chance are passed in so that a test can run a whole game.
     *
     * @return the exit code: 0 after a game, 1 if the game could not be started, 2 for a command line that is not understood
     */
    static int run(String[] args, InputStream in, PrintStream out, RandomSource random) {
        if (args.length == 1) {
            out.println("Playing over the network is not available yet.");
            out.println(usage());
            return 2;
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
        if (players != 1) {
            out.println("Playing over the network is not available yet: use 1 as the number of players for a local game.");
            return 2;
        }
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
}
