package kittens.app;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import kittens.api.PlayerId;
import kittens.api.RandomSource;
import kittens.engine.Game;
import kittens.engine.Participant;
import kittens.expansion.base.BaseGame;
import kittens.player.RandomBot;
import kittens.ui.ConsolePlayer;

/**
 * The composition root for a game: the one place that creates the concrete players and expansions and hands them to the
 * engine. Everything else works through the interfaces in {@code api}. The base game is always part of a game.
 */
final class GameBuilder {

    private GameBuilder() {
    }

    /**
     * A game for one human at the console against computer players. The human is player 0.
     *
     * @throws IllegalArgumentException if the number of players is not allowed
     */
    static Game local(int bots, InputStream in, PrintStream out, RandomSource random) {
        ConsolePlayer human = new ConsolePlayer(new PlayerId(0), in, out);
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant(human, human));
        for (int i = 0; i < bots; i++) {
            participants.add(Participant.of(new RandomBot(random)));
        }
        return Game.create(List.of(new BaseGame()), participants, random);
    }
}
