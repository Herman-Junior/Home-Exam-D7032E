package kittens.engine;

import static kittens.engine.TestContent.ATTACK;
import static kittens.engine.TestContent.CAT;
import static kittens.engine.TestContent.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.testing.FixedRandom;
import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/** What a card effect can do through the context, on the real game state. */
class EngineContextTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);

    private final RecordingListener listener = new RecordingListener();

    private Game game(FixedRandom random) {
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant(ScriptedPlayer.indexes(), listener));
        participants.add(Participant.of(ScriptedPlayer.indexes()));
        participants.add(Participant.of(ScriptedPlayer.indexes()));
        Game game = Game.create(List.of(TestContent.expansion()), participants, random);
        game.state().setCurrent(P0);
        game.context().beginTurn();
        return game;
    }

    @Test
    void stealingFromAHandOfOneCardTakesThatCard() { // R11f, broken in the original
        Game game = game(new FixedRandom());
        game.state().player(P1).hand().add(SKIP);

        Optional<kittens.api.CardType> stolen = game.context().stealRandomCard(P1, P0);

        assertEquals(Optional.of(SKIP), stolen);
        assertTrue(game.state().player(P1).hand().isEmpty());
        assertEquals(List.of(SKIP), game.state().player(P0).hand().cards());
    }

    @Test
    void anyCardOfTheHandCanBeStolenIncludingTheLastOne() { // the original could never pick the last card
        Game game = game(new FixedRandom(0, 1)); // the first number picks the starting player, the second the card
        game.state().player(P1).hand().add(SKIP);
        game.state().player(P1).hand().add(ATTACK);

        assertEquals(Optional.of(ATTACK), game.context().stealRandomCard(P1, P0));
    }

    @Test
    void stealingFromAnEmptyHandGivesNothing() {
        assertEquals(Optional.empty(), game(new FixedRandom()).context().stealRandomCard(P1, P0));
    }

    @Test
    void movingACardThatIsNotInTheHandIsAnError() {
        Game game = game(new FixedRandom());
        assertThrows(IllegalArgumentException.class, () -> game.context().moveCard(P1, P0, SKIP));
        assertThrows(IllegalArgumentException.class, () -> game.context().discard(P1, SKIP));
    }

    @Test
    void discardingMovesTheCardFromTheHandToTheDiscardPile() {
        Game game = game(new FixedRandom());
        game.state().player(P0).hand().add(CAT);

        game.context().discard(P0, CAT);

        assertTrue(game.state().player(P0).hand().isEmpty());
        assertEquals(List.of(CAT), game.state().discardPile().cards());
    }

    @Test
    void eliminatingAPlayerDiscardsTheHandAnnouncesItAndIsOnlyDoneOnce() {
        Game game = game(new FixedRandom());
        game.state().player(P1).hand().add(CAT);

        game.context().eliminate(P1);
        game.context().eliminate(P1);

        assertTrue(game.state().player(P1).eliminated());
        assertEquals(List.of(CAT), game.state().discardPile().cards());
        assertEquals(1, listener.events().stream().filter(e -> e.equals(GameEvent.of("player-eliminated", "1"))).count());
    }

    @Test
    void theTurnCanOnlyEndOnce() {
        Game game = game(new FixedRandom());
        game.context().endTurn();
        assertThrows(IllegalStateException.class, () -> game.context().endTurn());
        assertThrows(IllegalStateException.class, () -> game.context().attack(P1, 2));
    }

    @Test
    void aPlayerWhoIsOutCannotBeAttacked() {
        Game game = game(new FixedRandom());
        game.context().eliminate(P1);
        assertThrows(IllegalArgumentException.class, () -> game.context().attack(P1, 2));
    }

    @Test
    void theNextPlayerAndTheOthersFollowTheTurnOrderAndLeaveOutPlayersWhoAreOut() {
        Game game = game(new FixedRandom());
        assertEquals(P1, game.context().nextPlayer());
        assertEquals(List.of(P1, P2), game.context().otherPlayers());
        game.context().eliminate(P1);
        assertEquals(P2, game.context().nextPlayer());
        assertEquals(List.of(P2), game.context().otherPlayers());
    }

    @Test
    void theDeckOperationsWorkOnTheRealDeck() {
        Game game = game(new FixedRandom());
        game.state().deck().addToBottom(CAT);
        game.state().deck().addToBottom(SKIP);

        game.context().insertIntoDeck(ATTACK, 1);

        assertEquals(3, game.context().deckSize());
        assertEquals(List.of(CAT, ATTACK), game.context().peekTop(2));
        assertThrows(IllegalArgumentException.class, () -> game.context().insertIntoDeck(ATTACK, 4));
    }
}
