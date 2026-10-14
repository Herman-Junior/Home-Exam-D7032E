package kittens.api;

/** One player's answer to a {@link Decision}: the index of the option the player chose. */
public record Answer(PlayerId player, int choice) {
}
