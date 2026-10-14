package kittens.api;

/**
 * How an {@link Expansion} takes part in setting up a game. The engine applies the expansions in order, then shuffles
 * the deck, deals the hands, inserts the cards for {@link #addAfterDealing}, and shuffles again. Counts add up
 * across expansions, and a negative count removes cards that an earlier expansion added.
 */
public interface GameSetup {

    int playerCount();

    /** Cards that are in the deck when the hands are dealt. */
    void addToDeck(CardType type, int count);

    /** Extra cards that every player starts with, on top of the cards dealt from the deck. */
    void giveEachPlayer(CardType type, int count);

    /** Cards that are shuffled into the deck after the hands have been dealt, such as the exploding kittens. */
    void addAfterDealing(CardType type, int count);

    /** How many cards each player is dealt from the deck. */
    void dealPerPlayer(int count);
}
