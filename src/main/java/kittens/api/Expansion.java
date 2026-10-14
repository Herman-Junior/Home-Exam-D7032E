package kittens.api;

import java.util.List;

/**
 * A plug-in with game content: the base game, Imploding Kittens, Streaking Kittens, Barking Kittens. An expansion
 * supplies its cards, with their effects, and takes part in the setup. Adding an expansion means adding a class that
 * implements this interface; the engine does not change. Hooks for changing the core rules will be added here later.
 */
public interface Expansion {

    /** A stable name, such as {@code "imploding-kittens"}, used by the menu and in the configuration. */
    String id();

    /** The cards this expansion adds to the game. */
    List<CardDefinition> cards();

    /** Adds this expansion's cards and counts to the setup of a game. */
    void setUp(GameSetup setup);
}
