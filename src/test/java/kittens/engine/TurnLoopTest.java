package kittens.engine;

import static kittens.engine.TestContent.ATTACK;
import static kittens.engine.TestContent.CAT;
import static kittens.engine.TestContent.DEFUSE;
import static kittens.engine.TestContent.KITTEN;
import static kittens.engine.TestContent.SHUFFLE;
import static kittens.engine.TestContent.SKIP;
import static kittens.engine.TestContent.UNSTOPPABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import kittens.api.Combo;
import kittens.api.CardCatalog;
import kittens.api.CardType;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.api.PlayerPort;
import kittens.testing.FixedRandom;
import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/**
 * The turn loop, played with the engine's own test cards. Nobody is dealt anything: each test arranges the hands and
 * the deck through {@code game.state()}, scripts what the players do, and checks what happens.
 */
class TurnLoopTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);

    private final RecordingListener[] listeners = {new RecordingListener(), new RecordingListener(),
        new RecordingListener()};
    private final FixedRandom random = new FixedRandom();

    private Game start(TestContent.TestExpansion expansion, PlayerPort... ports) {
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < ports.length; i++) {
            participants.add(new Participant(ports[i], listeners[i]));
        }
        return Game.create(List.of(expansion), participants, random);
    }

    private static ScriptedPlayer idle() {
        return ScriptedPlayer.indexes();
    }

    private static void deck(Game game, CardType... top) {
        for (CardType card : top) {
            game.state().deck().addToBottom(card);
        }
    }

    private static void hand(Game game, PlayerId player, CardType... cards) {
        for (CardType card : cards) {
            game.state().player(player).hand().add(card);
        }
    }

    private static List<CardType> handOf(Game game, PlayerId player) {
        return game.state().player(player).hand().cards();
    }

    // ---- R9: pass or play cards ----

    @Test
    void passingDrawsTheTopCardAndMovesToTheNextPlayer() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("pass"), idle(), idle());
        deck(game, CAT, SKIP);

        game.playTurn();

        assertEquals(List.of(CAT), handOf(game, P0));
        assertEquals(List.of(SKIP), game.state().deck().cards());
        assertEquals(P1, game.state().current());
        assertEquals(1, game.state().turnsRemaining());
    }

    @Test
    void aPlayerMayPlayAsManyCardsAsDesiredBeforePassing() {
        ScriptedPlayer first = ScriptedPlayer.options("play-card shuffle", "play-card shuffle", "pass");
        Game game = start(TestContent.expansion(), first, idle(), idle());
        hand(game, P0, SHUFFLE, SHUFFLE);
        deck(game, CAT);
        int shufflesBefore = random.shuffles();

        game.playTurn();

        assertEquals(2, random.shuffles() - shufflesBefore);
        assertEquals(List.of(SHUFFLE, SHUFFLE), game.state().discardPile().cards());
        assertEquals(List.of(CAT), handOf(game, P0));
        assertEquals(P1, game.state().current());
    }

    @Test
    void theTurnDecisionListsPassAndEveryPlayableCardButNotCardsWithoutAnEffect() {
        ScriptedPlayer first = ScriptedPlayer.options("pass");
        Game game = start(TestContent.expansion(), first, idle(), idle());
        hand(game, P0, CAT, SKIP, SKIP, ATTACK);
        deck(game, CAT);

        game.playTurn();

        assertEquals(List.of("pass", "play-card skip", "play-card attack"), ScriptedPlayer.describe(first.asked().get(0)));
    }

    // ---- R11: the cards ----

    @Test
    void skipEndsTheTurnWithoutDrawing() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("play-card skip"), idle(), idle());
        hand(game, P0, SKIP);
        deck(game, CAT);

        game.playTurn();

        assertEquals(1, game.state().deck().size());
        assertTrue(handOf(game, P0).isEmpty());
        assertEquals(P1, game.state().current());
    }

    @Test
    void skipOnTheLastOfSeveralTurnsDoesNotPassOnTheExtraTurns() { // R11d, broken in the original
        ScriptedPlayer first = ScriptedPlayer.options("play-card attack");
        ScriptedPlayer second = ScriptedPlayer.options("play-card skip", "play-card skip");
        Game game = start(TestContent.expansion(), first, second, idle());
        hand(game, P0, ATTACK);
        hand(game, P1, SKIP, SKIP);
        deck(game, CAT, CAT, CAT);

        game.playTurn();
        assertEquals(P1, game.state().current());
        assertEquals(2, game.state().turnsRemaining());

        game.playTurn();
        assertEquals(P1, game.state().current());
        assertEquals(1, game.state().turnsRemaining());

        game.playTurn();
        assertEquals(P2, game.state().current());
        assertEquals(1, game.state().turnsRemaining());
        assertFalse(game.state().underAttack());
        assertEquals(3, game.state().deck().size());
    }

    @Test
    void attackedPlayersPassOnTheirTurnsPlusTwoEachTime() { // R11a: 2 turns, then 4, then 6
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("play-card attack"),
                ScriptedPlayer.options("play-card attack"), ScriptedPlayer.options("play-card attack"));
        hand(game, P0, ATTACK);
        hand(game, P1, ATTACK);
        hand(game, P2, ATTACK);

        game.playTurn();
        assertEquals(P1, game.state().current());
        assertEquals(2, game.state().turnsRemaining());

        game.playTurn();
        assertEquals(P2, game.state().current());
        assertEquals(4, game.state().turnsRemaining());

        game.playTurn();
        assertEquals(P0, game.state().current());
        assertEquals(6, game.state().turnsRemaining());
    }

    @Test
    void anAttackFromAPlayerWhoWasNotAttackedGivesExactlyTwoTurns() { // the original added the old attack on top
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("play-card attack"),
                ScriptedPlayer.options("pass", "pass"), ScriptedPlayer.options("play-card attack"));
        hand(game, P0, ATTACK);
        hand(game, P2, ATTACK);
        deck(game, CAT, CAT, CAT, CAT);

        game.playTurn(); // player 0 attacks player 1
        game.playTurn(); // player 1, first of two turns
        game.playTurn(); // player 1, second turn
        game.playTurn(); // player 2 attacks player 0

        assertEquals(P0, game.state().current());
        assertEquals(2, game.state().turnsRemaining());
    }

    // ---- R10: kittens ----

    @Test
    void aPlayerWithADefuseChoosesWhereTheKittenGoesIncludingTheBottom() { // R10a
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("pass").thenIndex(2), idle(), idle());
        hand(game, P0, DEFUSE);
        deck(game, KITTEN, CAT, CAT);

        game.playTurn();

        assertEquals(List.of(CAT, CAT, KITTEN), game.state().deck().cards());
        assertEquals(List.of(DEFUSE), game.state().discardPile().cards());
        assertFalse(game.state().player(P0).eliminated());
        assertEquals(P1, game.state().current());
    }

    @Test
    void anExplodedPlayerDiscardsTheHandAndTakesNoMoreTurns() { // R10b, broken in the original
        ScriptedPlayer attacked = ScriptedPlayer.options("pass"); // asked once only: a second turn would fail the test
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("play-card attack"), attacked, idle());
        hand(game, P0, ATTACK);
        hand(game, P1, CAT, SKIP);
        deck(game, KITTEN, CAT, CAT);

        game.playTurn();
        game.playTurn();

        assertTrue(game.state().player(P1).eliminated());
        assertTrue(handOf(game, P1).isEmpty());
        assertTrue(game.state().discardPile().cards().containsAll(List.of(ATTACK, CAT, SKIP, KITTEN)));
        assertEquals(1, attacked.asked().size());
        assertEquals(P2, game.state().current());
        assertEquals(1, game.state().turnsRemaining());
    }

    @Test
    void eliminatedPlayersAreSkipped() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("play-card skip"), idle(),
                ScriptedPlayer.options("play-card skip"));
        hand(game, P0, SKIP);
        hand(game, P2, SKIP);
        game.state().player(P1).eliminate();

        game.playTurn();
        assertEquals(P2, game.state().current());
        game.playTurn();
        assertEquals(P0, game.state().current());
    }

    // ---- R13: the winner ----

    @Test
    void theLastPlayerLeftWinsAndEveryoneIsToldThatTheGameIsWon() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("pass"), idle());
        deck(game, KITTEN);

        game.playTurn();

        assertTrue(game.isOver());
        assertEquals(java.util.Optional.of(P1), game.winner());
        for (int i = 0; i < 2; i++) {
            assertTrue(listeners[i].events().contains(GameEvent.of("game-won", "1")));
        }
        assertThrows(IllegalStateException.class, game::playTurn);
    }

    @Test
    void playRunsTheGameToTheEndAndReturnsTheWinner() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("pass"), idle());
        deck(game, KITTEN);

        assertEquals(P1, game.play());
    }

    // ---- R12: the reactions ----

    @Test
    void aReactionThatCancelsStopsTheEffectButTheCardIsStillSpent() {
        Game game = start(TestContent.expansion().withReactions((context, action) -> action.cards().contains(SKIP)),
                ScriptedPlayer.options("play-card skip", "pass"), idle(), idle());
        hand(game, P0, SKIP);
        deck(game, CAT);

        game.playTurn();

        assertEquals(List.of(CAT), handOf(game, P0)); // the turn went on, so the player passed and drew
        assertEquals(List.of(SKIP), game.state().discardPile().cards());
        assertTrue(listeners[0].events().contains(GameEvent.of("action-cancelled", "0", "skip")));
    }

    @Test
    void theReactionIsToldWhetherTheActionCanBeNoped() {
        List<Boolean> nopeable = new ArrayList<>();
        Game game = start(TestContent.expansion().withReactions((context, action) -> {
            nopeable.add(action.nopeable());
            return false;
        }), ScriptedPlayer.options("play-card unstoppable", "play-card skip"), idle(), idle());
        hand(game, P0, UNSTOPPABLE, SKIP);

        game.playTurn();

        assertEquals(List.of(false, true), nopeable);
    }

    @Test
    void aReactionThatDoesNotCancelLetsTheEffectHappen() {
        Game game = start(TestContent.expansion().withReactions((context, action) -> false),
                ScriptedPlayer.options("play-card skip"), idle(), idle());
        hand(game, P0, SKIP);

        game.playTurn();

        assertEquals(P1, game.state().current());
    }

    // ---- combos ----

    /** Two identical cards. */
    private static final Combo PAIR = new Combo() {
        @Override
        public List<List<CardType>> plays(List<CardType> hand, CardCatalog catalog) {
            return hand.stream().distinct().filter(card -> java.util.Collections.frequency(hand, card) >= 2)
                    .map(card -> List.of(card, card)).toList();
        }

        @Override
        public void play(kittens.api.GameContext context, PlayerId player, List<CardType> cards) {
            context.announce(GameEvent.of("combo-played", player.toString()));
        }
    };

    @Test
    void aComboIsOfferedAsAnOptionAndPlayedWithAllItsCards() {
        ScriptedPlayer first = ScriptedPlayer.options("play-combo cat cat", "pass");
        Game game = start(TestContent.expansion().withCombos(PAIR), first, idle(), idle());
        hand(game, P0, CAT, CAT, SKIP);
        deck(game, CAT);

        game.playTurn();

        assertEquals(List.of("pass", "play-card skip", "play-combo cat cat"), ScriptedPlayer.describe(first.asked().get(0)));
        assertEquals(List.of(CAT, CAT), game.state().discardPile().cards());
        assertEquals(List.of(SKIP, CAT), handOf(game, P0));
        assertTrue(listeners[0].events().contains(GameEvent.of("combo-played", "0")));
    }

    @Test
    void aComboThatOffersCardsThePlayerDoesNotHoldIsAnError() {
        Combo cheating = new Combo() {
            @Override
            public List<List<CardType>> plays(List<CardType> hand, CardCatalog catalog) {
                return List.of(List.of(SKIP, SKIP));
            }

            @Override
            public void play(kittens.api.GameContext context, PlayerId player, List<CardType> cards) {
            }
        };
        Game game = start(TestContent.expansion().withCombos(cheating), idle(), idle());
        assertThrows(IllegalStateException.class, game::playTurn);
    }

    // ---- bad answers ----

    @Test
    void aPlayerWhoKeepsGivingBadAnswersEndsUpPassing() {
        ScriptedPlayer confused = ScriptedPlayer.indexes(99, -1, 99);
        Game game = start(TestContent.expansion(), confused, idle());
        deck(game, CAT);

        game.playTurn();

        assertEquals(3, confused.asked().size());
        assertEquals(List.of(CAT), handOf(game, P0));
        assertEquals(3, listeners[0].events().stream().filter(e -> e.code().equals("invalid-answer")).count());
        assertTrue(listeners[0].events().contains(GameEvent.of("answer-defaulted", "0")));
    }

    @Test
    void aPortThatThrowsIsTreatedAsABadAnswer() {
        AtomicInteger calls = new AtomicInteger();
        PlayerPort flaky = decision -> {
            if (calls.incrementAndGet() <= 2) {
                throw new IllegalStateException("connection lost");
            }
            return 0;
        };
        Game game = start(TestContent.expansion(), flaky, idle());
        deck(game, CAT);

        game.playTurn();

        assertEquals(3, calls.get());
        assertEquals(List.of(CAT), handOf(game, P0));
    }

    @Test
    void aListenerThatThrowsDoesNotStopTheGame() {
        List<Participant> participants = List.of(
                new Participant(ScriptedPlayer.options("pass"), event -> {
                    throw new IllegalStateException("broken listener");
                }),
                Participant.of(idle()));
        Game game = Game.create(List.of(TestContent.expansion()), participants, random);
        deck(game, CAT);

        game.playTurn();

        assertEquals(P1, game.state().current());
    }

    @Test
    void drawingFromAnEmptyDeckIsAnErrorAndNotASilentCrashOfTheRules() {
        Game game = start(TestContent.expansion(), ScriptedPlayer.options("pass"), idle());
        assertThrows(IllegalStateException.class, game::playTurn);
    }
}
