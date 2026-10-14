package kittens.model;

import java.util.ArrayList;
import java.util.List;

import kittens.api.CardType;

/** The cards that have been played or lost, in the order they were discarded. */
public final class DiscardPile {

    private final List<CardType> cards = new ArrayList<>();

    public void add(CardType card) {
        cards.add(card);
    }

    public List<CardType> cards() {
        return List.copyOf(cards);
    }

    public int size() {
        return cards.size();
    }
}
