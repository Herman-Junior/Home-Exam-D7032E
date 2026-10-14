package kittens.expansion.base;

import java.util.List;

import kittens.api.CardType;
import kittens.api.Decision;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

/** Three of a kind: name a card, and take it from another player if that player has it. */
final class NameAndTakeCard implements ComboEffect {

    @Override
    public void apply(GameContext context, PlayerId player, List<CardType> cards) {
        PlayerId target = Targeting.chooseTarget(context, player);
        List<CardType> named = context.cardTypes();
        List<Option> options = named.stream().map(card -> Option.of("name", card.id())).toList();
        CardType wanted = named.get(context.ask(player, new Decision(BaseDecisions.NAME_CARD, options)));
        if (context.hand(target).contains(wanted)) {
            context.moveCard(target, player, wanted);
            context.tell(player, GameEvent.of("card-received", wanted.id(), target.toString()));
            context.tell(target, GameEvent.of("card-taken", wanted.id(), player.toString()));
        } else {
            context.tell(player, GameEvent.of("card-not-found", wanted.id(), target.toString()));
        }
    }
}
