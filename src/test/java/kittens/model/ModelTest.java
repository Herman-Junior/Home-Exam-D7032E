package kittens.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.api.CardType;
import kittens.api.PlayerId;
import kittens.testing.FixedRandom;

class ModelTest {

    private static final CardType A = new CardType("a");
    private static final CardType B = new CardType("b");
    private static final CardType C = new CardType("c");

    @Test
    void theTopOfTheDeckIsPositionZero() {
        Deck deck = new Deck();
        deck.addToBottom(A);
        deck.addToBottom(B);
        deck.insert(C, 0);
        assertEquals(List.of(C, A, B), deck.cards());
        assertEquals(List.of(C, A), deck.peekTop(2));
        assertEquals(List.of(C, A, B), deck.peekTop(9));
        assertEquals(C, deck.drawTop());
        assertEquals(2, deck.size());
    }

    @Test
    void aCardCanGoToTheVeryBottomButNotBeyond() {
        Deck deck = new Deck();
        deck.addToBottom(A);
        deck.insert(B, 1);
        assertEquals(List.of(A, B), deck.cards());
        assertThrows(IllegalArgumentException.class, () -> deck.insert(C, 3));
        assertThrows(IllegalArgumentException.class, () -> deck.insert(C, -1));
    }

    @Test
    void drawingFromAnEmptyDeckIsAnError() {
        assertThrows(IllegalStateException.class, () -> new Deck().drawTop());
    }

    @Test
    void theDeckIsShuffledBytheRandomSourceItIsGiven() {
        FixedRandom random = new FixedRandom();
        new Deck().shuffle(random);
        assertEquals(1, random.shuffles());
    }

    @Test
    void aHandRemovesOneCardAtATime() {
        Hand hand = new Hand();
        hand.add(A);
        hand.add(A);
        assertTrue(hand.remove(A));
        assertEquals(List.of(A), hand.cards());
        assertTrue(hand.remove(A));
        assertFalse(hand.remove(A));
        assertTrue(hand.isEmpty());
    }

    @Test
    void turnOrderWrapsAroundAndSkipsEliminatedPlayers() {
        GameState state = new GameState(4);
        state.player(new PlayerId(1)).eliminate();
        assertEquals(new PlayerId(2), state.nextAlive(new PlayerId(0)));
        assertEquals(new PlayerId(0), state.nextAlive(new PlayerId(3)));
        assertEquals(List.of(new PlayerId(2), new PlayerId(3)), state.alivePlayersAfter(new PlayerId(0)));
        assertEquals(List.of(new PlayerId(0), new PlayerId(2)), state.alivePlayersAfter(new PlayerId(3)));
    }

    @Test
    void theGameIsOverWhenOnePlayerIsLeft() {
        GameState state = new GameState(3);
        assertFalse(state.isOver());
        assertEquals(Optional.empty(), state.winner());
        state.player(new PlayerId(0)).eliminate();
        state.player(new PlayerId(2)).eliminate();
        assertTrue(state.isOver());
        assertEquals(Optional.of(new PlayerId(1)), state.winner());
        assertEquals(new PlayerId(1), state.nextAlive(new PlayerId(1)));
    }

    @Test
    void anUnknownPlayerIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new GameState(2).player(new PlayerId(2)));
    }
}
