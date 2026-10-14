package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/** Checks the fake itself, because later tests of card effects rely on it. */
class FakeGameContextTest {

    private static final CardType A = new CardType("a");
    private static final CardType B = new CardType("b");
    private static final CardType C = new CardType("c");
    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);

    @Test
    void turnOrderSkipsEliminatedPlayersAndWrapsAround() {
        FakeGameContext context = new FakeGameContext(3).withCurrentPlayer(1);
        assertEquals(P2, context.nextPlayer());
        assertEquals(List.of(P2, P0), context.otherPlayers());
        context.eliminate(P2);
        assertEquals(P0, context.nextPlayer());
    }

    @Test
    void eliminatingAPlayerDiscardsTheHand() {
        FakeGameContext context = new FakeGameContext(2).withHand(1, A, B);
        context.eliminate(P1);
        assertTrue(context.hand(P1).isEmpty());
        assertEquals(List.of(A, B), context.discardPile());
        assertTrue(context.eliminated().contains(P1));
    }

    @Test
    void cardsMoveBetweenHandsDeckAndDiscardPile() {
        FakeGameContext context = new FakeGameContext(2).withHand(0, A, B).withDeck(C);
        context.moveCard(P0, P1, A);
        context.discard(P0, B);
        context.insertIntoDeck(A, 1);
        assertEquals(List.of(A), context.hand(P1));
        assertEquals(List.of(B), context.discardPile());
        assertEquals(List.of(C, A), context.deck());
        assertEquals(List.of(C), context.peekTop(1));
        assertEquals(List.of(C, A), context.peekTop(5));
    }

    @Test
    void stealingTakesTheFirstCardOrNothing() {
        FakeGameContext context = new FakeGameContext(2).withHand(1, A, B);
        assertEquals(Optional.of(A), context.stealRandomCard(P1, P0));
        assertEquals(Optional.of(B), context.stealRandomCard(P1, P0));
        assertEquals(Optional.empty(), context.stealRandomCard(P1, P0));
        assertEquals(List.of(A, B), context.hand(P0));
    }

    @Test
    void askingUsesTheRegisteredPlayerAndChecksTheAnswer() {
        Decision decision = new Decision(DecisionKind.NOPE, List.of(Option.of("yes"), Option.of("no")));
        FakeGameContext context = new FakeGameContext(2)
                .withPlayer(0, ScriptedPlayer.indexes(1))
                .withPlayer(1, ScriptedPlayer.indexes(7));
        assertEquals(1, context.ask(P0, decision));
        assertThrows(AssertionError.class, () -> context.ask(P1, decision));
        assertThrows(AssertionError.class, () -> context.ask(P2, decision));
    }

    @Test
    void insertingOutsideTheDeckFails() {
        FakeGameContext context = new FakeGameContext(2).withDeck(A);
        context.insertIntoDeck(B, 1);
        assertThrows(AssertionError.class, () -> context.insertIntoDeck(C, 3));
    }

    @Test
    void recordsWhatTheEffectDid() {
        FakeGameContext context = new FakeGameContext(2);
        context.endTurn();
        context.attack(P1, 2);
        context.shuffleDeck();
        context.announce(GameEvent.of("x"));
        context.tell(P1, GameEvent.of("secret"));
        assertEquals(1, context.turnsEnded());
        assertEquals(List.of(new FakeGameContext.Attack(P1, 2)), context.attacks());
        assertEquals(1, context.shuffles());
        assertEquals(List.of(GameEvent.of("x")), context.announced());
        assertEquals(List.of(GameEvent.of("secret")), context.toldTo(1));
        assertTrue(context.toldTo(0).isEmpty());
    }
}
