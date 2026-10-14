package kittens.engine;

import java.util.Random;

import kittens.api.RandomSource;

/** The normal source of randomness, backed by {@link java.util.Random}. */
public final class JavaRandomSource implements RandomSource {

    private final Random random;

    public JavaRandomSource() {
        this(new Random());
    }

    /** For a repeatable game, pass a {@code Random} with a seed. */
    public JavaRandomSource(Random random) {
        this.random = random;
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
