package kittens.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.Option;
import kittens.engine.JavaRandomSource;
import kittens.testing.FixedRandom;

class RandomBotTest {

    private static final Decision TURN = new Decision(DecisionKind.TURN_ACTION,
            List.of(Option.of("pass"), Option.of("play-card", "skip"), Option.of("play-card", "attack")));
    private static final Decision NOPE = new Decision(DecisionKind.NOPE, List.of(Option.of("nope"), Option.of("pass")));

    @Test
    void mostOfTheTimeItPassesOnItsTurn() {
        assertEquals(0, new RandomBot(new FixedRandom(RandomBot.PASS_CHANCE - 1)).choose(TURN));
    }

    @Test
    void otherwiseItPlaysARandomCard() {
        assertEquals(2, new RandomBot(new FixedRandom(RandomBot.PASS_CHANCE, 1)).choose(TURN));
        assertEquals(1, new RandomBot(new FixedRandom(99, 0)).choose(TURN));
    }

    @Test
    void withNothingToPlayItPasses() {
        Decision onlyPass = new Decision(DecisionKind.TURN_ACTION, List.of(Option.of("pass")));
        assertEquals(0, new RandomBot(new FixedRandom(99)).choose(onlyPass));
    }

    @Test
    void itPlaysANopeHalfOfTheTime() {
        assertEquals(0, new RandomBot(new FixedRandom(0)).choose(NOPE));
        assertEquals(1, new RandomBot(new FixedRandom(1)).choose(NOPE));
    }

    @Test
    void anyOtherQuestionIsAnsweredAtRandom() {
        Decision other = new Decision(DecisionKind.GIVE_CARD,
                List.of(Option.of("give", "a"), Option.of("give", "b"), Option.of("give", "c")));
        assertEquals(2, new RandomBot(new FixedRandom(2)).choose(other));
    }

    @Test
    void itAlwaysAnswersWithAValidOption() {
        RandomBot bot = new RandomBot(new JavaRandomSource(new Random(5)));
        List<Decision> decisions = List.of(TURN, NOPE,
                new Decision(DecisionKind.DEFUSE_POSITION, List.of(Option.of("position", "0"), Option.of("position", "1"))),
                new Decision(new DecisionKind("something-new"), List.of(Option.of("x"))));
        for (int i = 0; i < 2000; i++) {
            Decision decision = decisions.get(i % decisions.size());
            assertTrue(decision.accepts(bot.choose(decision)));
        }
    }
}
