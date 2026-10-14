package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.ATTACK;
import static kittens.expansion.base.BaseCards.DEFUSE;
import static kittens.expansion.base.BaseCards.EXPLODING_KITTEN;
import static kittens.expansion.base.BaseCards.NOPE;
import static kittens.expansion.base.BaseCards.SKIP;
import static kittens.expansion.base.BaseCards.TACO_CAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import kittens.api.CardType;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.api.PlayerPort;
import kittens.engine.Game;
import kittens.engine.JavaRandomSource;
import kittens.engine.Participant;
import kittens.model.PlayerState;
import kittens.testing.FixedRandom;
import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/** The base game played by the real engine. */
class BaseGameIntegrationTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);

    /** A player who always passes, whatever the question allows. */
    private static final PlayerPort PASSIVE = decision -> {
        for (int i = 0; i < decision.options().size(); i++) {
            if (decision.options().get(i).code().equals("pass")) {
                return i;
            }
        }
        return 0;
    };

    private static List<Participant> passive(int players) {
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            participants.add(Participant.of(PASSIVE));
        }
        return participants;
    }

    @ParameterizedTest(name = "{0} players")
    @org.junit.jupiter.params.provider.ValueSource(ints = {2, 3, 4, 5})
    void aGameIsDealtAsTheRulesSay(int players) { // R1 to R8, through the real setup
        Game game = Game.create(List.of(new BaseGame()), passive(players), new JavaRandomSource(new Random(1)));

        int cards = game.state().deck().size();
        int defuse = Collections.frequency(game.state().deck().cards(), DEFUSE);
        for (PlayerState player : game.state().players()) {
            assertEquals(8, player.hand().size()); // 7 dealt and 1 Defuse
            assertTrue(player.hand().contains(DEFUSE));
            defuse += Collections.frequency(player.hand().cards(), DEFUSE);
            cards += player.hand().size();
        }
        // a player can be dealt another Defuse from the deck, so the Defuse cards are counted over the whole game
        assertEquals((players == 5 ? 1 : 2) + players, defuse);
        assertEquals(players - 1, Collections.frequency(game.state().deck().cards(), EXPLODING_KITTEN));
        // 46 cards without Defuse and kittens, the Defuse cards, and the kittens
        assertEquals(46 + defuse + (players - 1), cards);
    }

    @Test
    void aGameOfOneOrSixPlayersIsRefused() { // R1
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Game.create(List.of(new BaseGame()), passive(1), new JavaRandomSource()));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Game.create(List.of(new BaseGame()), passive(6), new JavaRandomSource()));
    }

    @ParameterizedTest(name = "{0} players, seed {1}")
    @CsvSource({"2,1", "2,2", "3,3", "3,4", "4,5", "4,6", "5,7", "5,8", "5,9"})
    void aGameWherePlayersOnlyPassEndsWithOneWinnerWhoIsToldToEveryone(int players, long seed) { // R13
        List<RecordingListener> listeners = new ArrayList<>();
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            RecordingListener listener = new RecordingListener();
            listeners.add(listener);
            participants.add(new Participant(PASSIVE, listener));
        }
        Game game = Game.create(List.of(new BaseGame()), participants, new JavaRandomSource(new Random(seed)));

        PlayerId winner = game.play();

        assertTrue(game.isOver());
        assertEquals(1, game.state().alivePlayers().size());
        assertEquals(winner, game.state().alivePlayers().get(0));
        for (RecordingListener listener : listeners) {
            assertTrue(listener.events().contains(GameEvent.of("game-won", winner.toString())));
        }
    }

    // ---- scenes played with real cards ----

    private Game scene(Duration nope, List<PlayerPort> ports, List<RecordingListener> listeners) {
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < ports.size(); i++) {
            participants.add(new Participant(ports.get(i), listeners.get(i)));
        }
        Game game = Game.create(List.of(new BaseGame(nope)), participants, new FixedRandom());
        for (PlayerState player : game.state().players()) {
            player.hand().clear();
        }
        game.state().setCurrent(P0);
        return game;
    }

    private static void give(Game game, PlayerId player, CardType... cards) {
        for (CardType card : cards) {
            game.state().player(player).hand().add(card);
        }
    }

    private static void clearDeck(Game game) {
        while (!game.state().deck().isEmpty()) {
            game.state().deck().drawTop();
        }
    }

    @Test
    void aNopeFromAnotherPlayerStopsASkip() { // R12, with real threads and the real Nope window
        List<RecordingListener> listeners = List.of(new RecordingListener(), new RecordingListener());
        Game game = scene(Duration.ofSeconds(2),
                List.of(ScriptedPlayer.options("play-card skip", "pass"), ScriptedPlayer.codes("nope")), listeners);
        clearDeck(game);
        game.state().deck().addToBottom(TACO_CAT);
        give(game, P0, SKIP);
        give(game, P1, NOPE);

        game.playTurn();

        assertEquals(List.of(TACO_CAT), game.state().player(P0).hand().cards()); // the Skip was stopped: P0 had to pass
        assertEquals(List.of(SKIP, NOPE), game.state().discardPile().cards());
        assertTrue(listeners.get(1).events().contains(GameEvent.of("action-cancelled", "0", "skip")));
    }

    @Test
    void aNopeOnANopeLetsTheSkipThrough() { // R12a
        List<RecordingListener> listeners = List.of(new RecordingListener(), new RecordingListener(),
                new RecordingListener());
        Game game = scene(Duration.ofSeconds(2),
                List.of(ScriptedPlayer.options("play-card skip"), ScriptedPlayer.codes("nope"),
                        ScriptedPlayer.codes("pass", "nope")), listeners);
        clearDeck(game);
        give(game, P0, SKIP);
        give(game, P1, NOPE);
        give(game, P2, NOPE);

        game.playTurn();

        assertEquals(P1, game.state().current()); // the Skip happened: the turn ended without drawing
        assertTrue(game.state().discardPile().cards().containsAll(List.of(SKIP, NOPE, NOPE)));
    }

    @Test
    void anAttackGivesTheNextPlayerTwoTurnsAndTheyBothHappen() { // R11a
        List<RecordingListener> listeners = List.of(new RecordingListener(), new RecordingListener());
        Game game = scene(Duration.ofMillis(200),
                List.of(ScriptedPlayer.options("play-card attack"), ScriptedPlayer.options("pass", "pass")), listeners);
        clearDeck(game);
        game.state().deck().addToBottom(TACO_CAT);
        game.state().deck().addToBottom(TACO_CAT);
        give(game, P0, ATTACK);

        game.playTurn();
        game.playTurn();
        game.playTurn();

        assertEquals(P0, game.state().current());
        assertEquals(List.of(TACO_CAT, TACO_CAT), game.state().player(P1).hand().cards());
    }

    @Test
    void twoOfAKindFromAPlayerWithOneCardWorksEndToEnd() { // R11f, which crashed the original
        List<RecordingListener> listeners = List.of(new RecordingListener(), new RecordingListener());
        Game game = scene(Duration.ofMillis(200),
                List.of(ScriptedPlayer.options("play-combo taco-cat taco-cat", "target 1", "pass"), ScriptedPlayer.indexes()),
                listeners);
        clearDeck(game);
        game.state().deck().addToBottom(SKIP);
        give(game, P0, TACO_CAT, TACO_CAT);
        give(game, P1, ATTACK);

        game.playTurn();

        assertEquals(List.of(ATTACK, SKIP), game.state().player(P0).hand().cards());
        assertTrue(game.state().player(P1).hand().isEmpty());
        assertEquals(P1, game.state().current());
    }

    @Test
    void aPlayerWhoDrawsTheLastKittenWithoutADefuseLosesAndTheOtherWins() { // R10b and R13
        List<RecordingListener> listeners = List.of(new RecordingListener(), new RecordingListener());
        Game game = scene(Duration.ofMillis(200), List.of(ScriptedPlayer.options("pass"), ScriptedPlayer.indexes()),
                listeners);
        clearDeck(game);
        game.state().deck().addToBottom(EXPLODING_KITTEN);

        game.playTurn();

        assertTrue(game.isOver());
        assertEquals(java.util.Optional.of(P1), game.winner());
    }
}
