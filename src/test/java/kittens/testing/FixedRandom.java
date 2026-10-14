package kittens.testing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import kittens.api.RandomSource;

/**
 * A {@link RandomSource} with a fixed outcome for tests. {@link #nextInt} returns the scripted numbers in order and then
 * 0, and {@link #shuffle} counts the shuffles but keeps the order, so the test can predict exactly where every card is.
 */
public final class FixedRandom implements RandomSource {

    private final Deque<Integer> numbers = new ArrayDeque<>();
    private int shuffles;

    public FixedRandom(int... nextInts) {
        for (int number : nextInts) {
            numbers.add(number);
        }
    }

    @Override
    public int nextInt(int bound) {
        int number = numbers.isEmpty() ? 0 : numbers.poll();
        if (number >= bound) {
            throw new AssertionError("the scripted number " + number + " is not below " + bound);
        }
        return number;
    }

    @Override
    public void shuffle(List<?> list) {
        shuffles++;
    }

    public int shuffles() {
        return shuffles;
    }
}
