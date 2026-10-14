package kittens.api;

/** Identifies a participant in a game. Players are numbered from 0. */
public record PlayerId(int value) {

    public PlayerId {
        if (value < 0) {
            throw new IllegalArgumentException("a player id cannot be negative");
        }
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
