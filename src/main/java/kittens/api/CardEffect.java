package kittens.api;

/**
 * What happens when a card is played. Each card type has its own effect, so a new card is a new effect and the engine
 * is not edited. The engine has already removed the card from the hand and let the other players try to Nope it.
 */
@FunctionalInterface
public interface CardEffect {

    void play(GameContext context, PlayerId player);
}
