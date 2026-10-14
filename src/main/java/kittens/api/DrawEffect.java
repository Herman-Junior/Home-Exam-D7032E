package kittens.api;

/**
 * What happens when a card is drawn. Only some cards have one, such as the kittens. The effect decides where the
 * card ends up: a card without a draw effect goes into the hand of the player who drew it, but a card with one is
 * left to the effect, which can for example put it back in the deck or into the hand and then eliminate the player.
 */
@FunctionalInterface
public interface DrawEffect {

    void onDrawn(GameContext context, PlayerId player, CardType card);
}
