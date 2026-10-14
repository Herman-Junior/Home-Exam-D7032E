package kittens.api;

import java.util.List;

/**
 * A plug-in with game content: the base game, Imploding Kittens, Streaking Kittens, Barking Kittens. An expansion
 * supplies its cards, with their effects, and takes part in the setup. Adding an expansion means adding a class that
 * implements this interface; the engine does not change.
 * <p>
 * What an expansion can contribute grows by adding methods with a default implementation, such as {@link #combos()}
 * and {@link #reactions()}. A default does nothing, so every expansion that already exists keeps compiling and
 * behaving the same when a new extension point is added.
 */
public interface Expansion {

    /** A stable name, such as {@code "imploding-kittens"}, used by the menu and in the configuration. */
    String id();

    /** The cards this expansion adds to the game. */
    List<CardDefinition> cards();

    /** Adds this expansion's cards and counts to the setup of a game. */
    void setUp(GameSetup setup);

    /** The ways of playing several cards together that this expansion adds. */
    default List<Combo> combos() {
        return List.of();
    }

    /** The ways of reacting to a played action that this expansion adds, such as the Nope rule. */
    default List<Reaction> reactions() {
        return List.of();
    }
}
