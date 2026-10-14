package kittens.expansion.base;

import java.util.List;

import kittens.api.CardType;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.PlayerId;

/** Two of a kind: take a random card from another player. */
final class StealRandomCard implements ComboEffect {

    @Override
    public void apply(GameContext context, PlayerId player, List<CardType> cards) {
        PlayerId target = Targeting.chooseTarget(context, player);
        context.stealRandomCard(target, player).ifPresent(card -> {
            context.tell(player, GameEvent.of("card-received", card.id(), target.toString()));
            context.tell(target, GameEvent.of("card-taken", card.id(), player.toString()));
        });
    }
}
