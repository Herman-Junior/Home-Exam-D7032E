package kittens.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.ToIntFunction;

import kittens.api.Decision;
import kittens.api.Option;
import kittens.api.PlayerPort;

/**
 * A player for tests that answers from a fixed script and remembers every decision it was asked. It makes it possible
 * to run any phase of a game without a keyboard, a socket or a bot. Asking it more than it was scripted for fails
 * the test, so a player that must not act is one with an empty script.
 * <p>
 * An answer can be given as an option index, as the code of an option, or as a description such as
 * {@code "play-card skip"} (the code followed by its arguments). The styles can be mixed:
 * {@code ScriptedPlayer.options("pass").thenIndex(2)}.
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
            player.thenIndex(choice);
        }
        return player;
    }

    /** Answers each decision with the first option that has the next code in the list. */
    public static ScriptedPlayer codes(String... codes) {
        ScriptedPlayer player = new ScriptedPlayer();
        for (String code : codes) {
            player.thenCode(code);
        }
        return player;
    }

    /** Answers each decision with the option that has the next description, such as {@code "play-card skip"}. */
    public static ScriptedPlayer options(String... descriptions) {
        ScriptedPlayer player = new ScriptedPlayer();
        for (String description : descriptions) {
            player.thenOption(description);
        }
        return player;
    }

    public ScriptedPlayer thenIndex(int choice) {
        script.add(decision -> choice);
        return this;
    }

    public ScriptedPlayer thenCode(String code) {
        script.add(decision -> {
            for (int i = 0; i < decision.options().size(); i++) {
                if (decision.options().get(i).code().equals(code)) {
                    return i;
                }
            }
            throw new AssertionError("no option with code '" + code + "' in " + describe(decision));
        });
        return this;
    }

    public ScriptedPlayer thenOption(String description) {
        script.add(decision -> {
            for (int i = 0; i < decision.options().size(); i++) {
                if (describe(decision.options().get(i)).equals(description)) {
                    return i;
                }
            }
            throw new AssertionError("no option '" + description + "' in " + describe(decision));
        });
        return this;
    }

    /** An option as text: its code followed by its arguments. */
    public static String describe(Option option) {
        return option.args().isEmpty() ? option.code() : option.code() + " " + String.join(" ", option.args());
    }

    /** The options of a decision as text. */
    public static List<String> describe(Decision decision) {
        return decision.options().stream().map(ScriptedPlayer::describe).toList();
    }

    @Override
    public synchronized int choose(Decision decision) {
        asked.add(decision);
        ToIntFunction<Decision> next = script.poll();
        if (next == null) {
            throw new AssertionError("the scripted player was asked an unscripted decision: " + describe(decision));
        }
        return next.applyAsInt(decision);
    }

    /** The decisions asked so far, oldest first. */
    public synchronized List<Decision> asked() {
        return Collections.unmodifiableList(new ArrayList<>(asked));
    }
}
