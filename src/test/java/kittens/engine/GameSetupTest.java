package kittens.engine;

import static kittens.engine.TestContent.CAT;
import static kittens.engine.TestContent.DEFUSE;
import static kittens.engine.TestContent.KITTEN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.api.CardDefinition;
import kittens.api.Expansion;
import kittens.api.PlayerId;
import kittens.testing.FixedRandom;
import kittens.testing.ScriptedPlayer;

/** The setup of a game: the number of players, the deck, the hands and the first player (R1 to R8). */
class GameSetupTest {

    private static List<Participant> participants(int count) {
        List<Participant> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(Participant.of(ScriptedPlayer.indexes()));
        }
        return list;
    }

    private static Game create(int players, FixedRandom random, Expansion... expansions) {
        return Game.create(List.of(expansions), participants(players), random);
    }

    @Test
    void aGameNeedsBetweenTwoAndFivePlayers() {
        assertThrows(IllegalArgumentException.class, () -> create(1, new FixedRandom(), TestContent.expansion()));
        assertThrows(IllegalArgumentException.class, () -> create(6, new FixedRandom(), TestContent.expansion()));
        assertDoesNotThrow(() -> create(2, new FixedRandom(), TestContent.expansion()));
        assertDoesNotThrow(() -> create(5, new FixedRandom(), TestContent.expansion()));
    }

    @Test
    void anExpansionCanWidenTheNumberOfPlayers() {
        Expansion sixPlayers = TestContent.expansion(setup -> setup.allowPlayerCount(2, 6))
                .withId("six").withCards(List.of());
        assertDoesNotThrow(() -> create(6, new FixedRandom(), TestContent.expansion(), sixPlayers));
    }

    @Test
    void someExpansionMustSayHowManyPlayersCanPlay() {
        Expansion silent = TestContent.expansion(setup -> { });
        assertThrows(IllegalArgumentException.class, () -> create(2, new FixedRandom(), silent));
    }

    @Test
    void theDeckIsShuffledBeforeTheDealAndAgainAtTheEndAndAPlayerIsChosenToStart() {
        FixedRandom random = new FixedRandom(2);
        Expansion expansion = TestContent.expansion(setup -> {
            setup.allowPlayerCount(2, 5);
            setup.addToDeck(CAT, 10);
            setup.dealPerPlayer(3);
            setup.giveEachPlayer(DEFUSE, 1);
            setup.addAfterDealing(KITTEN, setup.playerCount() - 1);
        });

        Game game = create(3, random, expansion);

        assertEquals(2, random.shuffles());
        assertEquals(new PlayerId(2), game.state().current());
        game.state().players().forEach(player -> {
            assertEquals(List.of(CAT, CAT, CAT, DEFUSE), sorted(player.hand().cards()));
        });
        assertEquals(List.of(CAT, KITTEN, KITTEN), game.state().deck().cards());
    }

    @Test
    void cardsAreAddedUpAcrossExpansions() {
        Expansion first = TestContent.expansion(setup -> {
            setup.allowPlayerCount(2, 5);
            setup.addToDeck(CAT, 5);
        });
        Expansion second = TestContent.expansion(setup -> {
            setup.addToDeck(CAT, 3);
            setup.addToDeck(DEFUSE, 1);
        }).withId("second").withCards(List.of());

        Game game = create(2, new FixedRandom(), first, second);

        assertEquals(9, game.state().deck().size());
    }

    @Test
    void anExpansionCanRemoveWhatAnEarlierOneAdded() {
        Expansion first = TestContent.expansion(setup -> {
            setup.allowPlayerCount(2, 5);
            setup.addAfterDealing(KITTEN, 3);
        });
        Expansion second = TestContent.expansion(setup -> setup.addAfterDealing(KITTEN, -1))
                .withId("second").withCards(List.of());

        Game game = create(2, new FixedRandom(), first, second);

        assertEquals(List.of(KITTEN, KITTEN), game.state().deck().cards());
    }

    @Test
    void removingMoreThanWasAddedIsAnError() {
        Expansion expansion = TestContent.expansion(setup -> {
            setup.allowPlayerCount(2, 5);
            setup.addToDeck(CAT, -1);
        });
        assertThrows(IllegalStateException.class, () -> create(2, new FixedRandom(), expansion));
    }

    @Test
    void theDeckMustHaveEnoughCardsToDeal() {
        Expansion expansion = TestContent.expansion(setup -> {
            setup.allowPlayerCount(2, 5);
            setup.addToDeck(CAT, 3);
            setup.dealPerPlayer(2);
        });
        assertThrows(IllegalStateException.class, () -> create(2, new FixedRandom(), expansion));
    }

    @Test
    void aCardDefinedByTwoExpansionsIsRefused() {
        Expansion clash = TestContent.expansion().withId("clash");
        assertThrows(IllegalArgumentException.class, () -> create(2, new FixedRandom(), TestContent.expansion(), clash));
    }

    @Test
    void theSameExpansionTwiceIsRefused() {
        Expansion again = TestContent.expansion().withCards(List.<CardDefinition>of());
        assertThrows(IllegalArgumentException.class, () -> create(2, new FixedRandom(), TestContent.expansion(), again));
    }

    private static List<kittens.api.CardType> sorted(List<kittens.api.CardType> cards) {
        List<kittens.api.CardType> copy = new ArrayList<>(cards);
        Collections.sort(copy, java.util.Comparator.comparing(kittens.api.CardType::id));
        return copy;
    }
}
