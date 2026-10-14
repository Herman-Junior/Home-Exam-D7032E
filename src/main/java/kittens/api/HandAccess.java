package kittens.api;

import java.util.List;
import java.util.Optional;

/** What a card effect may do with the cards in the players' hands and the discard pile. */
public interface HandAccess {

    /** The cards in a player's hand. */
    List<CardType> hand(PlayerId player);

    /** Puts a card that is in nobody's hand, for example one that was just drawn, into a player's hand. */
    void addToHand(PlayerId player, CardType card);

    /** Moves a card from one player's hand to another's. */
    void moveCard(PlayerId from, PlayerId to, CardType card);

    /** Moves a randomly chosen card from one hand to another, if the hand has any card. Returns the card moved. */
    Optional<CardType> stealRandomCard(PlayerId from, PlayerId to);

    /** Moves a card from a player's hand to the discard pile. */
    void discard(PlayerId player, CardType card);
}
