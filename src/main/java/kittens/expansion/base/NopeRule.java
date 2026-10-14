package kittens.expansion.base;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import kittens.api.Answer;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayedAction;
import kittens.api.PlayerId;
import kittens.api.Reaction;

/**
 * The Nope rule. After a card or combo is played, every player holding a Nope card has a time window to play it, and
 * the first to answer does. A Nope can itself be Noped, so each Nope turns the result around between "Nope" and
 * "Yup", and the window opens again until nobody plays another. The engine knows nothing of this: it only offers each
 * action to its reactions.
 */
final class NopeRule implements Reaction {

    private final Duration window;

    NopeRule(Duration window) {
        this.window = window;
    }

    @Override
    public boolean cancels(GameContext context, PlayedAction action) {
        if (!action.nopeable()) {
            return false;
        }
        boolean cancelled = false;
        while (true) {
            List<PlayerId> holders = holders(context);
            if (holders.isEmpty()) {
                return cancelled;
            }
            Decision decision = new Decision(DecisionKind.NOPE, List.of(Option.of("nope"), Option.of("pass")));
            Optional<Answer> nope = context.askAll(holders, decision, window).stream()
                    .filter(answer -> decision.options().get(answer.choice()).code().equals("nope"))
                    .findFirst();
            if (nope.isEmpty()) {
                return cancelled;
            }
            context.discard(nope.get().player(), BaseCards.NOPE);
            cancelled = !cancelled;
            context.announce(GameEvent.of("nope-played", nope.get().player().toString(), cancelled ? "nope" : "yup"));
        }
    }

    /** Every player still in the game who holds a Nope card, the player who played the action included. */
    private static List<PlayerId> holders(GameContext context) {
        List<PlayerId> everyone = new ArrayList<>();
        everyone.add(context.currentPlayer());
        everyone.addAll(context.otherPlayers());
        return everyone.stream().filter(player -> context.hand(player).contains(BaseCards.NOPE)).toList();
    }
}
