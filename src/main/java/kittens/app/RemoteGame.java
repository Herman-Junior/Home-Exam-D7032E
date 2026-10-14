package kittens.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import kittens.api.PlayerId;
import kittens.net.Client;
import kittens.player.RemoteClient;
import kittens.ui.ConsolePlayer;

/** Joining a game that is hosted on another computer, and playing it from this console. */
final class RemoteGame {

    private RemoteGame() {
    }

    /** @return 0 after the game, 1 if the game could not be joined */
    static int join(String host, int port, InputStream in, PrintStream out) {
        Client.Joined joined;
        try {
            joined = Client.connect(host, port);
        } catch (IOException e) {
            out.println("Cannot join the game: " + e.getMessage());
            return 1;
        }
        ConsolePlayer console = new ConsolePlayer(new PlayerId(joined.playerNumber()), in, out);
        out.println("Joined as player " + joined.playerNumber() + ". Waiting for the game to start...");
        new RemoteClient(joined.connection(), console, console).run();
        return 0;
    }
}
