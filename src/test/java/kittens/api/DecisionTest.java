package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DecisionTest {

    @Test
    void aDecisionNeedsAtLeastOneOption() {
        assertThrows(IllegalArgumentException.class, () -> new Decision(DecisionKind.NOPE, List.of()));
    }

    @Test
    void onlyIndexesOfExistingOptionsAreAccepted() {
        Decision decision = new Decision(DecisionKind.NOPE, List.of(Option.of("yes"), Option.of("no")));
        assertFalse(decision.accepts(-1));
        assertTrue(decision.accepts(0));
        assertTrue(decision.accepts(1));
        assertFalse(decision.accepts(2));
    }

    @Test
    void theOptionListCannotBeChangedAfterwards() {
        List<Option> options = new ArrayList<>(List.of(Option.of("pass")));
        Decision decision = new Decision(DecisionKind.TURN_ACTION, options);
        options.add(Option.of("cheat"));
        assertEquals(1, decision.options().size());
        assertThrows(UnsupportedOperationException.class, () -> decision.options().add(Option.of("cheat")));
    }

    @Test
    void anExpansionCanDefineItsOwnKindOfDecision() {
        DecisionKind custom = new DecisionKind("alter-the-future");
        assertEquals(custom, new DecisionKind("alter-the-future"));
        assertFalse(custom.equals(DecisionKind.TURN_ACTION));
    }

    @Test
    void kindsOptionsAndEventsNeedACode() {
        assertThrows(IllegalArgumentException.class, () -> new DecisionKind(" "));
        assertThrows(IllegalArgumentException.class, () -> Option.of(""));
        assertThrows(IllegalArgumentException.class, () -> GameEvent.of(""));
    }
}
