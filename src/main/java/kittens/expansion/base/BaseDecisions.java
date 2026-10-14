package kittens.expansion.base;

import kittens.api.DecisionKind;

/**
 * The kinds of decision that only the base game asks for. They are defined here and not in {@code api}, which shows how
 * an expansion adds a kind of decision without any change to shared code.
 */
public final class BaseDecisions {

    /** Choose another player as the target of a card or a combo. */
    public static final DecisionKind CHOOSE_TARGET = new DecisionKind("choose-target");

    /** Name a card that you want to take from another player (three of a kind). */
    public static final DecisionKind NAME_CARD = new DecisionKind("name-card");

    private BaseDecisions() {
    }
}
