package kittens.api;

import java.util.List;

/**
 * Something a player has played that is about to take effect: one card or a combination. The cards have already left
 * the player's hand.
 *
 * @param nopeable whether the other players may still try to stop it
 */
public record PlayedAction(PlayerId player, List<CardType> cards, boolean nopeable) {

    public PlayedAction {
        cards = List.copyOf(cards);
    }
}
