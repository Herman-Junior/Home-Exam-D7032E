package kittens;

/**
 * Entry point. Keeps the command line of the original game:
 * <pre>
 *   server: java -jar exploding-kittens.jar [numPlayers] [numBots]
 *   client: java -jar exploding-kittens.jar [server IP]
 * </pre>
 * The game itself is not wired in yet.
 */
public final class Main {

    private Main() {
    }

    static String usage() {
        return "Server syntax: java -jar exploding-kittens.jar numPlayers numBots\n"
             + "Client syntax: java -jar exploding-kittens.jar serverIP";
    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            System.out.println(usage());
            return;
        }
        System.out.println("Not implemented yet.");
    }
}
