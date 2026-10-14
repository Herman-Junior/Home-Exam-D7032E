package kittens.api;

import java.util.List;

/**
 * A way of playing several cards together, such as two or three of a kind. An {@link Expansion} supplies its combos,
 * so a new combination is a new class and the engine does not change.
 */
public interface Combo {

    /**
     * The ways this combo can be played from the given hand. Each element is one play: the cards that would be used.
     * The catalog tells which cards carry which tags and which act as a wildcard.
     */
    List<List<CardType>> plays(List<CardType> hand, CardCatalog catalog);

    /** What happens when the combo is played. The engine has already taken {@code cards} out of the player's hand. */
    void play(GameContext context, PlayerId player, List<CardType> cards);
}
