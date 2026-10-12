package kittens.api;

import java.util.List;

/**
 * Something that happened in the game, for example {@code ("card-played", "2", "Skip")}. Like {@link Option} it is
 * plain data: a code and arguments, so expansions can add new events and the network can carry them unchanged.
 */
public record GameEvent(String code, List<String> args) {

    public GameEvent {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("an event needs a code");
        }
        args = List.copyOf(args);
    }

    public static GameEvent of(String code, String... args) {
        return new GameEvent(code, List.of(args));
    }
}
