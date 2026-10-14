package kittens.api;

/** Talking to the players: asking for decisions and reporting what happens. */
public interface Messenger {

    /**
     * Asks {@code player} to decide. The engine checks the answer, so the result is always a valid index into
     * {@link Decision#options()}.
     */
    int ask(PlayerId player, Decision decision);

    /** Tells everyone about an event. */
    void announce(GameEvent event);

    /** Tells one player about an event that the others must not see, such as the top cards of the deck. */
    void tell(PlayerId player, GameEvent event);
}
