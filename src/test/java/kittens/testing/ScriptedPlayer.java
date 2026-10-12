package kittens.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.ToIntFunction;

import kittens.api.Decision;
import kittens.api.PlayerPort;

/**
 * A player for tests that answers from a fixed script and remembers every decision it was asked. It makes it possible
 * to run any phase of a game without a keyboard, a socket or a bot. Asking it more than it was scripted for fails
 * the test.
 */
public final class ScriptedPlayer implements PlayerPort {

    private final Deque<ToIntFunction<Decision>> script = new ArrayDeque<>();
    private final List<Decision> asked = new ArrayList<>();

    private ScriptedPlayer() {
    }

    /** Answers with these option indexes, one per decision, in order. */
    public static ScriptedPlayer indexes(int... choices) {
        ScriptedPlayer player = new ScriptedPlayer();
        for (int choice : choices) {
            player.script.add(decision -> choice);
        }
        return player;
    }

    /** Answers each decision with the first option that has the next code in the list. */
    public static ScriptedPlayer codes(String... codes) {
        ScriptedPlayer player = new ScriptedPlayer();
        for (String code : codes) {
            player.script.add(decision -> {
                for (int i = 0; i < decision.options().size(); i++) {
                    if (decision.options().get(i).code().equals(code)) {
                        return i;
                    }
                }
                throw new AssertionError("no option with code '" + code + "' in " + decision);
            });
        }
        return player;
    }

    @Override
    public synchronized int choose(Decision decision) {
        asked.add(decision);
        ToIntFunction<Decision> next = script.poll();
        if (next == null) {
            throw new AssertionError("the scripted player was asked an unscripted decision: " + decision);
        }
        return next.applyAsInt(decision);
    }

    /** The decisions asked so far, oldest first. */
    public synchronized List<Decision> asked() {
        return Collections.unmodifiableList(new ArrayList<>(asked));
    }
}
