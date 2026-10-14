package kittens.api;

/**
 * How an {@link Expansion} takes part in setting up a game. The engine applies the expansions in order, then checks
 * the number of players, shuffles the deck, deals the hands, gives the extra cards, inserts the cards for
 * {@link #addAfterDealing}, and shuffles again. Counts add up across expansions, and a negative count removes cards
 * that an earlier expansion added.
 */
public interface GameSetup {

    int playerCount();

    /**
     * Says that a game with a number of players from {@code min} to {@code max} is allowed. The allowed numbers are
     * those from the lowest {@code min} to the highest {@code max} that any expansion names, so an expansion can
     * widen the range. At least one expansion has to name a range.
     */
    void allowPlayerCount(int min, int max);

    /** Cards that are in the deck when the hands are dealt. */
    void addToDeck(CardType type, int count);

    /** Extra cards that every player starts with, on top of the cards dealt from the deck. */
    void giveEachPlayer(CardType type, int count);

    /** Cards that are shuffled into the deck after the hands have been dealt, such as the exploding kittens. */
    void addAfterDealing(CardType type, int count);

    /** How many cards each player is dealt from the deck. If several expansions say, the last one wins. */
    void dealPerPlayer(int count);
}
