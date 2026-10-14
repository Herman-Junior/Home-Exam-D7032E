package kittens.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

class ConsoleTextTest {

    private static final PlayerId ME = new PlayerId(0);

    private static String say(String code, String... args) {
        return ConsoleText.event(GameEvent.of(code, args), ME).orElseThrow();
    }

    @Test
    void youAreYouAndOthersArePlayers() {
        assertEquals("You played: skip", say("card-played", "0", "skip"));
        assertEquals("Player 2 played: taco cat + taco cat", say("card-played", "2", "taco-cat", "taco-cat"));
        assertEquals("Player 1 won the game!", say("game-won", "1"));
        assertEquals("You won the game!", say("game-won", "0"));
    }

    @Test
    void cardNamesAreReadableWords() {
        assertEquals("Your hand: taco cat, defuse", say("hand", "taco-cat", "defuse"));
        assertEquals("Your hand: (none)", say("hand"));
        assertEquals("The top cards of the deck: see the future", say("top-cards", "see-the-future"));
    }

    @Test
    void turnsAreCountedInTheRightWords() {
        assertTrue(say("turn-started", "1", "1").contains("Turn of player 1 (1 turn to take)"));
        assertTrue(say("turn-started", "0", "4").contains("Turn of you (4 turns to take)"));
    }

    @Test
    void aNopeOnANopeIsAYup() {
        assertEquals("Player 2 played a Nope!", say("nope-played", "2", "nope"));
        assertTrue(say("nope-played", "2", "yup").contains("Yup"));
    }

    @Test
    void drawingIsShownForOthersButNotAgainForYourself() {
        assertEquals("Player 2 drew a card.", say("player-drew", "2"));
        assertEquals(Optional.empty(), ConsoleText.event(GameEvent.of("player-drew", "0"), ME));
        assertEquals("You drew: skip", say("card-drawn", "skip"));
        assertEquals("Player 3 drew an Exploding Kitten!", say("kitten-drawn", "3"));
    }

    @Test
    void anEventWithNoWordsIsShownGenerically() {
        assertEquals("flood: 3 meters", say("flood", "3", "meters"));
        assertEquals("flood", say("flood"));
    }

    @Test
    void theTurnDecisionIsListedInWords() {
        assertEquals("What do you want to do?", ConsoleText.prompt(
                new Decision(DecisionKind.TURN_ACTION, List.of(Option.of("pass")))));
        assertEquals("Pass (draw a card and end your turn)", ConsoleText.option(DecisionKind.TURN_ACTION, Option.of("pass")));
        assertEquals("Play skip", ConsoleText.option(DecisionKind.TURN_ACTION, Option.of("play-card", "skip")));
        assertEquals("Play together: taco cat + taco cat",
                ConsoleText.option(DecisionKind.TURN_ACTION, Option.of("play-combo", "taco-cat", "taco-cat")));
    }

    @Test
    void thePassOptionOfTheNopeQuestionMeansLettingItHappen() {
        assertEquals("Let it happen", ConsoleText.option(DecisionKind.NOPE, Option.of("pass")));
        assertEquals("Play a Nope", ConsoleText.option(DecisionKind.NOPE, Option.of("nope")));
    }

    @Test
    void aKindThatTheConsoleKnowsNothingAboutIsStillAskedAndListed() {
        DecisionKind unknown = new DecisionKind("alter-the-future");
        Decision decision = new Decision(unknown, List.of(Option.of("order", "a", "b")));
        assertEquals("Choose (alter-the-future):", ConsoleText.prompt(decision));
        assertEquals("order a b", ConsoleText.option(unknown, decision.options().get(0)));
    }

    @Test
    void onlyThePositionInTheDeckIsAnsweredByNumber() {
        Decision position = new Decision(DecisionKind.DEFUSE_POSITION, List.of(Option.of("position", "0"), Option.of("position", "1")));
        assertTrue(ConsoleText.answeredByNumber(position));
        assertTrue(ConsoleText.prompt(position).contains("from 0 (the top) to 1 (the bottom)"));
        assertFalse(ConsoleText.answeredByNumber(new Decision(DecisionKind.NOPE, List.of(Option.of("nope")))));
    }
}
