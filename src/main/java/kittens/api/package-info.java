/**
 * Interfaces and shared types only; nothing here runs. The engine and every plug-in (ui, player, expansion)
 * depend on this package and never on each other, and this package depends on nothing else in the project.
 * <p>
 * Decisions: {@link kittens.api.PlayerPort} asks, {@link kittens.api.Decision} holds the question and the legal
 * {@link kittens.api.Option}s, and {@link kittens.api.DecisionKind} says what it is about.
 * Events: {@link kittens.api.GameListener} receives {@link kittens.api.GameEvent}s.
 * Content: an {@link kittens.api.Expansion} supplies {@link kittens.api.CardDefinition}s whose
 * {@link kittens.api.CardEffect}s and {@link kittens.api.DrawEffect}s act on the game through a
 * {@link kittens.api.GameContext}, and it takes part in setup through {@link kittens.api.GameSetup}.
 * Still to come: rule hooks, RandomSource, Timer and GameConfig.
 */
package kittens.api;
