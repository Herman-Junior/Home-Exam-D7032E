package kittens.api;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/** Talking to the players: asking for decisions and reporting what happens. */
public interface Messenger {

    /**
     * Asks {@code player} to decide. The engine checks the answer, so the result is always a valid index into
     * {@link Decision#options()}.
     */
    int ask(PlayerId player, Decision decision);

    /**
     * Asks all the given players at the same time and returns the answers that arrive within {@code limit}, in the
     * order they arrive. It returns early once every player has answered. A player who does not answer in time is
     * simply missing from the result. This is how timed moments such as the Nope window are built; all the waiting
     * and the threads stay inside the engine.
     */
    List<Answer> askAll(Collection<PlayerId> players, Decision decision, Duration limit);

    /** Tells everyone about an event. */
    void announce(GameEvent event);

    /** Tells one player about an event that the others must not see, such as the top cards of the deck. */
    void tell(PlayerId player, GameEvent event);
}
