package kittens.api;

import java.util.List;

/** What a card effect may do with the draw pile. Position 0 is the top of the deck. */
public interface DeckAccess {

    int deckSize();

    /** The top {@code count} cards, or fewer if the deck is smaller. The deck is not changed. */
    List<CardType> peekTop(int count);

    /** Puts a card into the deck. {@code position} 0 is the top and {@link #deckSize()} is the very bottom. */
    void insertIntoDeck(CardType card, int position);

    void shuffleDeck();
}
