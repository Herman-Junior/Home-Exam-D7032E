package kittens.api;

/**
 * What a {@link Decision} is about. This is an open type on purpose: an expansion that needs a new kind of
 * decision creates its own {@code new DecisionKind("alter-the-future")} without editing this class or
 * {@link PlayerPort}. Two kinds are equal when their ids are equal.
 */
public record DecisionKind(String id) {

    /** Choose what to do on your turn: pass, or play one or several cards. */
    public static final DecisionKind TURN_ACTION = new DecisionKind("turn-action");
    /** Play a Nope on the card that was just played, or let it through. */
    public static final DecisionKind NOPE = new DecisionKind("nope");
    /** Choose which card to hand over (Favor). */
    public static final DecisionKind GIVE_CARD = new DecisionKind("give-card");
    /** Choose where in the deck to put a defused Exploding Kitten. */
    public static final DecisionKind DEFUSE_POSITION = new DecisionKind("defuse-position");

    public DecisionKind {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a decision kind needs an id");
        }
    }
}
