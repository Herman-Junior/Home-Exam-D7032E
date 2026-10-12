package kittens.api;

/**
 * How the engine asks a participant for a decision. Implemented by the human at the keyboard, bots, remote players
 * and by scripted players in tests. There is deliberately one method: a new kind of decision from an expansion is a
 * new {@link DecisionKind}, not a new method, so no implementation has to change.
 */
public interface PlayerPort {

    /**
     * Picks one of the options of {@code decision}.
     *
     * @return the index of the chosen option in {@link Decision#options()}
     */
    int choose(Decision decision);
}
