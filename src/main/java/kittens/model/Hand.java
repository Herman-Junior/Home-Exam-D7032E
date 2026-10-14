package kittens.model;

import java.util.ArrayList;
import java.util.List;

import kittens.api.CardType;

/** The cards a player holds. */
public final class Hand {

    private final List<CardType> cards = new ArrayList<>();

    /** A copy of the cards, in the order they were added. */
    public List<CardType> cards() {
        return List.copyOf(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public boolean contains(CardType card) {
        return cards.contains(card);
    }

    public void add(CardType card) {
        cards.add(card);
    }

    /** Removes one card of this type. Returns false if the hand has none. */
    public boolean remove(CardType card) {
        return cards.remove(card);
    }

    /** Removes and returns the card at {@code index}. */
    public CardType removeAt(int index) {
        return cards.remove(index);
    }

    public void clear() {
        cards.clear();
    }
}
