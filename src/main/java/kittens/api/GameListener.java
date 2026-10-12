package kittens.api;

/**
 * Receives what happens in the game. Kept apart from {@link PlayerPort} so that a participant that only has to
 * make decisions, such as a bot, does not have to implement it, and so that observers other than players can listen.
 */
public interface GameListener {

    void onEvent(GameEvent event);
}
