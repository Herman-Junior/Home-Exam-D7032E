package kittens.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import kittens.api.PlayerId;
import kittens.api.RandomSource;
import kittens.engine.Game;
import kittens.engine.Participant;
import kittens.expansion.base.BaseGame;
import kittens.net.Connection;
import kittens.net.Server;
import kittens.player.RandomBot;
import kittens.player.RemotePlayer;
import kittens.ui.ConsolePlayer;

/**
 * The composition root for a game: the one place that creates the concrete players and expansions and hands them to the
 * engine. Everything else works through the interfaces in {@code api}. The base game is always part of a game.
 */
final class GameBuilder {

    private GameBuilder() {
    }

    /** A game that is ready to play, and the connections to close when it is over. */
    record Built(Game game, List<RemotePlayer> remotePlayers) implements AutoCloseable {

        @Override
        public void close() {
            remotePlayers.forEach(RemotePlayer::close);
        }
    }

    /**
     * A game for one human at the console against computer players. The human is player 0.
     *
     * @throws IllegalArgumentException if the number of players is not allowed
     */
    static Game local(int bots, InputStream in, PrintStream out, RandomSource random) {
        return participantsFor(bots, in, out, List.of(), random);
    }

    /**
     * A game that is hosted from this console: the human here is player 0, then come the computer players, and then one
     * player for each client that connects to {@code server}. As in the original game, the players are numbered in that
     * order and take their turns in it.
     *
     * @throws IllegalArgumentException if the number of players is not allowed. It is checked before anybody is waited for.
     * @throws IOException if the server fails while waiting for the clients
     */
    static Built hosted(int clients, int bots, Server server, InputStream in, PrintStream out, RandomSource random)
            throws IOException {
        checkPlayerCount(1 + bots + clients, random);
        List<RemotePlayer> remotePlayers = new ArrayList<>();
        try {
            for (int i = 0; i < clients; i++) {
                int number = 1 + bots + i;
                out.println("Waiting for player " + number + " (" + (i + 1) + " of " + clients + ") to connect on port "
                        + server.port() + "...");
                Connection connection = server.acceptPlayer(number);
                remotePlayers.add(new RemotePlayer(connection));
                out.println("Player " + number + " has joined.");
            }
            return new Built(participantsFor(bots, in, out, remotePlayers, random), remotePlayers);
        } catch (IOException | RuntimeException e) {
            remotePlayers.forEach(RemotePlayer::close);
            throw e;
        }
    }

    private static Game participantsFor(int bots, InputStream in, PrintStream out, List<RemotePlayer> remotePlayers,
            RandomSource random) {
        ConsolePlayer human = new ConsolePlayer(new PlayerId(0), in, out);
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant(human, human));
        for (int i = 0; i < bots; i++) {
            participants.add(Participant.of(new RandomBot(random)));
        }
        for (RemotePlayer remote : remotePlayers) {
            participants.add(new Participant(remote, remote));
        }
        return Game.create(List.of(new BaseGame()), participants, random);
    }

    /** Asks the engine whether a game with this many players is allowed, by trying to set one up. */
    private static void checkPlayerCount(int players, RandomSource random) {
        List<Participant> placeholders = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            placeholders.add(Participant.of(decision -> 0));
        }
        Game.create(List.of(new BaseGame()), placeholders, random);
    }
}
