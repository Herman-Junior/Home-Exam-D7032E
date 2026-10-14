package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/**
 * Checks that the Nope rule can be written as a {@link Reaction} with nothing but types from this package. The engine
 * will not know what a Nope card is. The reaction here is a sketch of the one that the base expansion will contain.
 */
class NopeReactionTest {

    private static final CardType NOPE = new CardType("nope");
    private static final CardType SKIP = new CardType("skip");
    private static final DecisionKind NOPE_KIND = DecisionKind.NOPE;
    private static final PlayerId P0 = new PlayerId(0);

    /** Everyone holding a Nope may play it within the window. A Nope can itself be Noped, so each one flips the result. */
    private static final Reaction NOPE_RULE = (context, action) -> {
        if (!action.nopeable()) {
            return false;
        }
        boolean cancelled = false;
        while (true) {
            List<PlayerId> everyone = new ArrayList<>(context.otherPlayers());
            everyone.add(context.currentPlayer());
            List<PlayerId> holders = everyone.stream().filter(p -> context.hand(p).contains(NOPE)).toList();
            if (holders.isEmpty()) {
                return cancelled;
            }
            Decision decision = new Decision(NOPE_KIND, List.of(Option.of("nope"), Option.of("pass")));
            Optional<Answer> nope = context.askAll(holders, decision, Duration.ofSeconds(5)).stream()
                    .filter(answer -> decision.options().get(answer.choice()).code().equals("nope"))
                    .findFirst();
            if (nope.isEmpty()) {
                return cancelled;
            }
            context.discard(nope.get().player(), NOPE);
            context.announce(GameEvent.of("nope-played", nope.get().player().toString()));
            cancelled = !cancelled;
        }
    };

    private static final PlayedAction SKIP_PLAYED = new PlayedAction(P0, List.of(SKIP), true);

    @Test
    void anActionNobodyNopesIsNotCancelled() {
        FakeGameContext context = new FakeGameContext(3);
        assertFalse(NOPE_RULE.cancels(context, SKIP_PLAYED));
    }

    @Test
    void aNopeCancelsTheActionAndIsDiscarded() {
        FakeGameContext context = new FakeGameContext(3)
                .withHand(1, NOPE)
                .withPlayer(1, ScriptedPlayer.codes("nope"));
        assertTrue(NOPE_RULE.cancels(context, SKIP_PLAYED));
        assertEquals(List.of(NOPE), context.discardPile());
        assertEquals(1, context.announced().size());
    }

    @Test
    void aNopeOnANopeLetsTheActionThrough() {
        FakeGameContext context = new FakeGameContext(3)
                .withHand(1, NOPE).withHand(2, NOPE)
                .withPlayer(1, ScriptedPlayer.codes("nope"))
                .withPlayer(2, ScriptedPlayer.codes("pass", "nope"));
        // Round 1: players 1 and 2 are both asked and player 1 Nopes. Round 2: only player 2 still holds a Nope
        // and Nopes the Nope, so the action goes through.
        assertFalse(NOPE_RULE.cancels(context, SKIP_PLAYED));
        assertEquals(List.of(NOPE, NOPE), context.discardPile());
    }

    @Test
    void aPlayerWhoDoesNotAnswerInTimeDoesNotNope() {
        // Player 2 holds a Nope but has no registered answer, which the fake treats as no answer within the window.
        FakeGameContext context = new FakeGameContext(3).withHand(2, NOPE);
        assertFalse(NOPE_RULE.cancels(context, SKIP_PLAYED));
        assertEquals(List.of(NOPE), context.hand(new PlayerId(2)));
    }

    @Test
    void anActionThatCannotBeNopedIsNeverOffered() {
        FakeGameContext context = new FakeGameContext(3)
                .withHand(1, NOPE)
                .withPlayer(1, ScriptedPlayer.codes());
        assertFalse(NOPE_RULE.cancels(context, new PlayedAction(P0, List.of(SKIP), false)));
        assertEquals(List.of(NOPE), context.hand(new PlayerId(1)));
    }
}
