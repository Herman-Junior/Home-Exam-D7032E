package kittens.api;

import java.util.List;

/**
 * A question the engine puts to a player: what kind of decision it is, and the complete list of legal answers.
 * The player answers with the index of one option, and the engine refuses any index that {@link #accepts}
 * does not allow, so a bad answer can never reach the game rules.
 */
public record Decision(DecisionKind kind, List<Option> options) {

    public Decision {
        if (kind == null) {
            throw new IllegalArgumentException("a decision needs a kind");
        }
        options = List.copyOf(options);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("a decision needs at least one option");
        }
    }

    /** Whether {@code choice} is a valid answer to this decision. */
    public boolean accepts(int choice) {
        return choice >= 0 && choice < options.size();
    }
}
