package kittens.api;

import java.util.List;

/**
 * One legal answer to a {@link Decision}. It is plain data: a code that says what the option does and arguments
 * that say with what, for example {@code ("play-pair", "TacoCat", "2")}. How an option is shown to a human is
 * decided by the user interface, not by the engine, and the same data can be sent over the network unchanged.
 */
public record Option(String code, List<String> args) {

    public Option {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("an option needs a code");
        }
        args = List.copyOf(args);
    }

    public static Option of(String code, String... args) {
        return new Option(code, List.of(args));
    }
}
