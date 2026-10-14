package kittens.expansion.base;

import java.util.LinkedHashSet;
import java.util.List;

import kittens.api.CardEffect;
import kittens.api.CardType;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

/** Favor: another player gives you one card of their choice. If they have no cards, nothing happens. */
final class FavorEffect implements CardEffect {

    @Override
    public void play(GameContext context, PlayerId player) {
        PlayerId target = Targeting.chooseTarget(context, player);
        List<CardType> hand = context.hand(target);
        if (hand.isEmpty()) {
            context.announce(GameEvent.of("favor-failed", player.toString(), target.toString()));
            return;
        }
        List<CardType> choices = List.copyOf(new LinkedHashSet<>(hand));
        List<Option> options = choices.stream().map(card -> Option.of("give", card.id())).toList();
        CardType given = choices.get(context.ask(target, new Decision(DecisionKind.GIVE_CARD, options)));
        context.moveCard(target, player, given);
        context.tell(player, GameEvent.of("card-received", given.id(), target.toString()));
        context.tell(target, GameEvent.of("card-taken", given.id(), player.toString()));
    }
}
