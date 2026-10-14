package kittens.model;

import java.util.ArrayList;
import java.util.List;

import kittens.api.CardType;
import kittens.api.RandomSource;

/** The draw pile. Position 0 is the top. */
public final class Deck {

    private final List<CardType> cards = new ArrayList<>();

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** A copy of the cards, top card first. */
    public List<CardType> cards() {
        return List.copyOf(cards);
    }

    /** The top {@code count} cards, or fewer if the deck is smaller. The deck is not changed. */
    public List<CardType> peekTop(int count) {
        return List.copyOf(cards.subList(0, Math.min(Math.max(count, 0), cards.size())));
    }

    /** Removes and returns the top card. */
    public CardType drawTop() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("the deck is empty");
        }
        return cards.remove(0);
    }

    /** Puts a card at {@code position}: 0 is the top and {@link #size()} is the very bottom. */
    public void insert(CardType card, int position) {
        if (position < 0 || position > cards.size()) {
            throw new IllegalArgumentException("no position " + position + " in a deck of " + cards.size() + " cards");
        }
        cards.add(position, card);
    }

    public void addToBottom(CardType card) {
        cards.add(card);
    }

    public void shuffle(RandomSource random) {
        random.shuffle(cards);
    }
}
