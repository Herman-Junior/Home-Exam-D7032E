package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.ATTACK;
import static kittens.expansion.base.BaseCards.CATTERMELON;
import static kittens.expansion.base.BaseCards.DEFUSE;
import static kittens.expansion.base.BaseCards.EXPLODING_KITTEN;
import static kittens.expansion.base.BaseCards.FAVOR;
import static kittens.expansion.base.BaseCards.HAIRY_POTATO_CAT;
import static kittens.expansion.base.BaseCards.NOPE;
import static kittens.expansion.base.BaseCards.OVERWEIGHT_BIKINI_CAT;
import static kittens.expansion.base.BaseCards.RAINBOW_RALPHING_CAT;
import static kittens.expansion.base.BaseCards.SEE_THE_FUTURE;
import static kittens.expansion.base.BaseCards.SHUFFLE;
import static kittens.expansion.base.BaseCards.SKIP;
import static kittens.expansion.base.BaseCards.TACO_CAT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import kittens.api.CardType;
import kittens.testing.RecordingSetup;

/** Requirements 1 to 7 of the exam, checked directly on what the base game asks of the setup. */
class BaseSetupTest {

    private static RecordingSetup setupFor(int players) {
        RecordingSetup setup = new RecordingSetup(players);
        new BaseGame().setUp(setup);
        return setup;
    }

    @Test
    void r1_aGameHasTwoToFivePlayers() {
        RecordingSetup setup = setupFor(3);
        assertEquals(2, setup.minPlayers);
        assertEquals(5, setup.maxPlayers);
    }

    @Test
    void r2_everyPlayerGetsOneDefuse() {
        assertEquals(Map.of(DEFUSE, 1), setupFor(4).extras);
    }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = {2, 3, 4, 5})
    void r3a_theDeckHasTwoDefuseForTwoToFourPlayersAndOneForFive(int players) {
        int expected = players == 5 ? 1 : 2;
        assertEquals(expected, setupFor(players).deck.get(DEFUSE));
    }

    @Test
    void r3_theRestOfTheDeck() {
        Map<CardType, Integer> deck = setupFor(3).deck;
        assertEquals(4, deck.get(ATTACK));
        assertEquals(4, deck.get(FAVOR));
        assertEquals(5, deck.get(NOPE));
        assertEquals(4, deck.get(SHUFFLE));
        assertEquals(4, deck.get(SKIP));
        assertEquals(5, deck.get(SEE_THE_FUTURE));
        assertEquals(4, deck.get(HAIRY_POTATO_CAT));
        assertEquals(4, deck.get(CATTERMELON));
        assertEquals(4, deck.get(RAINBOW_RALPHING_CAT));
        assertEquals(4, deck.get(TACO_CAT));
        assertEquals(4, deck.get(OVERWEIGHT_BIKINI_CAT));
        assertEquals(12, deck.size());
    }

    @Test
    void r5_sevenCardsAreDealtToEachPlayer() {
        assertEquals(7, setupFor(3).dealPerPlayer);
    }

    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = {2, 3, 4, 5})
    void r6_oneFewerExplodingKittenThanPlayersIsShuffledInAfterTheDeal(int players) {
        assertEquals(Map.of(EXPLODING_KITTEN, players - 1), setupFor(players).afterDealing);
    }
}
