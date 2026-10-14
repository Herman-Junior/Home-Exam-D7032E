package kittens.expansion.base;

import java.util.List;

import kittens.api.Decision;
import kittens.api.GameContext;
import kittens.api.Option;
import kittens.api.PlayerId;

/** Choosing the other player that a card or combo is aimed at. */
final class Targeting {

    /** Asks the player to choose among the other players who are still in the game. */
    static PlayerId chooseTarget(GameContext context, PlayerId chooser) {
        List<PlayerId> candidates = context.otherPlayers();
        List<Option> options = candidates.stream().map(player -> Option.of("target", player.toString())).toList();
        return candidates.get(context.ask(chooser, new Decision(BaseDecisions.CHOOSE_TARGET, options)));
    }

    private Targeting() {
    }
}
