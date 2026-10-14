package kittens.api;

/**
 * Everything a card effect is allowed to do to the running game. The engine implements it, and tests use an in-memory
 * fake, so an effect can be tested without the engine. It is split into four small roles so that code which only needs
 * one of them, for example the deck, depends on just that role.
 */
public interface GameContext extends Messenger, DeckAccess, HandAccess, TurnControl {
}
