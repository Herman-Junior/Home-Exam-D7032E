package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.NOPE;
import static kittens.expansion.base.BaseCards.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.api.GameEvent;
import kittens.api.PlayedAction;
import kittens.api.PlayerId;
import kittens.api.Reaction;
import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/** The Nope rule (R12). */
class NopeRuleTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);
    private static final PlayedAction SKIP_PLAYED = new PlayedAction(P0, List.of(SKIP), true);

    private final Reaction rule = new BaseGame().reactions().get(0);
    private final FakeGameContext context = new FakeGameContext(3);

    @Test
    void theWindowIsFiveSeconds() {
        context.withHand(1, NOPE).withPlayer(1, ScriptedPlayer.codes("pass"));
        rule.cancels(context, SKIP_PLAYED);
        assertEquals(List.of(Duration.ofSeconds(5)), context.askAllLimits());
    }

    @Test
    void anActionNobodyNopesGoesThrough() {
        assertFalse(rule.cancels(context, SKIP_PLAYED));
        assertTrue(context.askAllLimits().isEmpty()); // nobody holds a Nope, so nobody is asked
    }

    @Test
    void aNopeStopsTheActionAndTheNopeCardIsSpent() {
        context.withHand(1, NOPE).withPlayer(1, ScriptedPlayer.codes("nope"));

        assertTrue(rule.cancels(context, SKIP_PLAYED));

        assertEquals(List.of(NOPE), context.discardPile());
        assertEquals(List.of(GameEvent.of("nope-played", "1", "nope")), context.announced());
    }

    @Test
    void aNopeOnANopeIsAYupAndTheActionGoesThrough() { // R12a
        context.withHand(1, NOPE).withHand(2, NOPE)
                .withPlayer(1, ScriptedPlayer.codes("nope"))
                .withPlayer(2, ScriptedPlayer.codes("pass", "nope"));

        assertFalse(rule.cancels(context, SKIP_PLAYED));

        assertEquals(List.of(GameEvent.of("nope-played", "1", "nope"), GameEvent.of("nope-played", "2", "yup")),
                context.announced());
    }

    @Test
    void threeNopesInARowStopTheAction() {
        context.withHand(0, NOPE).withHand(1, NOPE).withHand(2, NOPE)
                .withPlayer(0, ScriptedPlayer.codes("pass", "pass", "nope"))
                .withPlayer(1, ScriptedPlayer.codes("nope"))
                .withPlayer(2, ScriptedPlayer.codes("pass", "nope"));
        // Round 1: player 0 passes, player 1 Nopes. Round 2: players 0 and 2 are asked, player 2 Nopes.
        // Round 3: only player 0 is left with a Nope and plays it.
        assertTrue(rule.cancels(context, SKIP_PLAYED));
        assertEquals(3, context.discardPile().size());
    }

    @Test
    void thePlayerWhoPlayedTheCardMayNopeToo() {
        context.withHand(0, NOPE).withPlayer(0, ScriptedPlayer.codes("nope"));
        assertTrue(rule.cancels(context, SKIP_PLAYED));
    }

    @Test
    void aPlayerWhoDoesNotAnswerInTimeDoesNotNope() {
        context.withHand(2, NOPE); // no answer registered for player 2: the fake treats it as a timeout
        assertFalse(rule.cancels(context, SKIP_PLAYED));
        assertEquals(List.of(NOPE), context.hand(P2));
    }

    @Test
    void anActionThatCannotBeNopedIsNeverOffered() {
        context.withHand(1, NOPE).withPlayer(1, ScriptedPlayer.codes());
        assertFalse(rule.cancels(context, new PlayedAction(P0, List.of(SKIP), false)));
        assertEquals(List.of(NOPE), context.hand(P1));
    }

    @Test
    void playersWhoAreOutAreNotAsked() {
        context.withHand(1, NOPE).withPlayer(1, ScriptedPlayer.codes());
        context.eliminate(P1);
        assertFalse(rule.cancels(context, SKIP_PLAYED));
        assertTrue(context.askAllLimits().isEmpty());
    }
}
