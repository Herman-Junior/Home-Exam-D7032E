package kittens.expansion.base;

import kittens.api.CardEffect;
import kittens.api.CardType;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.PlayerId;

/** See the Future: the player privately sees the top three cards of the deck, or fewer if the deck is smaller. */
final class SeeTheFutureEffect implements CardEffect {

    @Override
    public void play(GameContext context, PlayerId player) {
        context.tell(player, GameEvent.of("top-cards",
                context.peekTop(3).stream().map(CardType::id).toArray(String[]::new)));
    }
}
