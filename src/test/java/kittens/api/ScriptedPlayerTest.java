package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/** Checks the test helpers themselves, because later tests rely on them. */
class ScriptedPlayerTest {

    private final Decision turn = new Decision(DecisionKind.TURN_ACTION,
            List.of(Option.of("pass"), Option.of("play-card", "Skip")));

    @Test
    void answersWithTheScriptedIndexesInOrder() {
        ScriptedPlayer player = ScriptedPlayer.indexes(1, 0);
        assertEquals(1, player.choose(turn));
        assertEquals(0, player.choose(turn));
    }

    @Test
    void canAnswerByOptionCode() {
        ScriptedPlayer player = ScriptedPlayer.codes("play-card", "pass");
        assertEquals(1, player.choose(turn));
        assertEquals(0, player.choose(turn));
    }

    @Test
    void remembersWhatItWasAsked() {
        ScriptedPlayer player = ScriptedPlayer.indexes(0);
        player.choose(turn);
        assertEquals(List.of(turn), player.asked());
    }

    @Test
    void failsWhenAskedMoreThanScripted() {
        ScriptedPlayer player = ScriptedPlayer.indexes(0);
        player.choose(turn);
        assertThrows(AssertionError.class, () -> player.choose(turn));
    }

    @Test
    void failsWhenTheScriptedCodeIsNotAnOption() {
        assertThrows(AssertionError.class, () -> ScriptedPlayer.codes("play-pair").choose(turn));
    }

    @Test
    void theListenerKeepsEventsInOrder() {
        RecordingListener listener = new RecordingListener();
        listener.onEvent(GameEvent.of("turn-started", "1"));
        listener.onEvent(GameEvent.of("card-played", "1", "Skip"));
        assertEquals(List.of(GameEvent.of("turn-started", "1"), GameEvent.of("card-played", "1", "Skip")),
                listener.events());
    }

    @Test
    void canAnswerByDescriptionAndMixStyles() {
        ScriptedPlayer player = ScriptedPlayer.options("play-card Skip").thenIndex(0);
        assertEquals(1, player.choose(turn));
        assertEquals(0, player.choose(turn));
    }

    @Test
    void failsWhenTheDescriptionMatchesNoOption() {
        assertThrows(AssertionError.class, () -> ScriptedPlayer.options("play-card Attack").choose(turn));
    }
}
