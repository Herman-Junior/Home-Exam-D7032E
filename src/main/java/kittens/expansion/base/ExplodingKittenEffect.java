package kittens.expansion.base;

import java.util.ArrayList;
import java.util.List;

import kittens.api.CardType;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.DrawEffect;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

/**
 * Drawing an Exploding Kitten. A player with a Defuse uses it and puts the kitten back in the deck wherever the player
 * likes, including the very bottom. A player without one discards the hand together with the kitten and is out.
 */
final class ExplodingKittenEffect implements DrawEffect {

    @Override
    public void onDrawn(GameContext context, PlayerId player, CardType kitten) {
        if (context.hand(player).contains(BaseCards.DEFUSE)) {
            context.discard(player, BaseCards.DEFUSE);
            List<Option> positions = new ArrayList<>();
            for (int position = 0; position <= context.deckSize(); position++) {
                positions.add(Option.of("position", Integer.toString(position)));
            }
            int position = context.ask(player, new Decision(DecisionKind.DEFUSE_POSITION, positions));
            context.insertIntoDeck(kitten, position);
            context.announce(GameEvent.of("defused", player.toString()));
        } else {
            context.addToHand(player, kitten);
            context.eliminate(player);
        }
    }
}
