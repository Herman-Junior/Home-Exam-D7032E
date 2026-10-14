package kittens.api;

import java.util.List;

/**
 * The only source of randomness in a game. The engine and the model never create a {@code Random} themselves, so a
 * test can pass a source with a fixed outcome and check exactly what happens, for instance who goes first or which
 * card is stolen.
 */
public interface RandomSource {

    /** A number from 0 up to, but not including, {@code bound}. */
    int nextInt(int bound);

    /** Shuffles the list. A source may override this, for example a test source that keeps the order. */
    default void shuffle(List<?> list) {
        swapShuffle(list);
    }

    private <T> void swapShuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T swapped = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swapped);
        }
    }
}
