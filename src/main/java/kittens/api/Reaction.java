package kittens.api;

/**
 * A chance for the players to react to a played action before it takes effect. The Nope rule is a reaction. The engine
 * offers every played card and combination to the reactions of all expansions and skips the effect if one of them
 * cancels it. The engine does not know what a Nope card is, so another way of interrupting an action can be added
 * as a new reaction without touching the engine.
 */
@FunctionalInterface
public interface Reaction {

    /** Returns true if the action must not take effect. The reaction may ask players, using the context. */
    boolean cancels(GameContext context, PlayedAction action);
}
