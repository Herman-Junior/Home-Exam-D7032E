/**
 * Interfaces and shared types only; nothing here runs. The engine and every plug-in (ui, player, expansion)
 * depend on this package and never on each other.
 * <p>
 * Decisions: {@link kittens.api.PlayerPort} asks, {@link kittens.api.Decision} holds the question and the legal
 * {@link kittens.api.Option}s, and {@link kittens.api.DecisionKind} says what it is about.
 * Events: {@link kittens.api.GameListener} receives {@link kittens.api.GameEvent}s.
 * Still to come: Expansion, CardEffect, RandomSource, Timer and GameConfig.
 */
package kittens.api;
