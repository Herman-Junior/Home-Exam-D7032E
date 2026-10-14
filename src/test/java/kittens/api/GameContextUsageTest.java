package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/**
 * Checks that the interfaces are enough to express real card effects, using only types from this package. The effects
 * here are small sketches of base game cards; the real ones will live in the expansion package.
 */
class GameContextUsageTest {

    private static final CardType DEFUSE = new CardType("defuse");
    private static final CardType KITTEN = new CardType("exploding-kitten");
    private static final CardType TACO = new CardType("taco-cat");
    private static final CardType SKIP = new CardType("skip");
    private static final PlayerId P0 = new PlayerId(0);

    private static final DecisionKind DEFUSE_POSITION = DecisionKind.DEFUSE_POSITION;

    /** Drawing an Exploding Kitten: defuse it and choose where it goes, or be eliminated. */
    private static final DrawEffect EXPLODING_KITTEN = (context, player, card) -> {
        if (context.hand(player).contains(DEFUSE)) {
            context.discard(player, DEFUSE);
            List<Option> positions = new ArrayList<>();
            for (int position = 0; position <= context.deckSize(); position++) {
                positions.add(Option.of("position", Integer.toString(position)));
            }
            int position = context.ask(player, new Decision(DEFUSE_POSITION, positions));
            context.insertIntoDeck(card, position);
        } else {
            context.addToHand(player, card);
            context.eliminate(player);
        }
    };

    @Test
    void aDefusedKittenGoesBackWhereThePlayerChooses() {
        FakeGameContext context = new FakeGameContext(2)
                .withHand(0, DEFUSE, TACO)
                .withDeck(TACO, TACO)
                .withPlayer(0, ScriptedPlayer.indexes(1));

        EXPLODING_KITTEN.onDrawn(context, P0, KITTEN);

        assertEquals(List.of(TACO, KITTEN, TACO), context.deck());
        assertEquals(List.of(TACO), context.hand(P0));
        assertEquals(List.of(DEFUSE), context.discardPile());
        assertTrue(context.eliminated().isEmpty());
    }

    @Test
    void theKittenCanBePutAtTheVeryBottomOfTheDeck() {
        FakeGameContext context = new FakeGameContext(2)
                .withHand(0, DEFUSE)
                .withDeck(TACO, TACO)
                .withPlayer(0, ScriptedPlayer.indexes(2));

        EXPLODING_KITTEN.onDrawn(context, P0, KITTEN);

        assertEquals(List.of(TACO, TACO, KITTEN), context.deck());
    }

    @Test
    void withoutADefuseThePlayerExplodesAndDiscardsTheHandAndTheKitten() {
        FakeGameContext context = new FakeGameContext(2).withHand(0, TACO, SKIP);

        EXPLODING_KITTEN.onDrawn(context, P0, KITTEN);

        assertTrue(context.eliminated().contains(P0));
        assertTrue(context.hand(P0).isEmpty());
        assertEquals(List.of(TACO, SKIP, KITTEN), context.discardPile());
    }

    @Test
    void playingASkipEndsTheTurn() {
        CardEffect skip = (context, player) -> context.endTurn();
        FakeGameContext context = new FakeGameContext(2);
        skip.play(context, P0);
        assertEquals(1, context.turnsEnded());
    }

    @Test
    void anAttackEndsTheTurnAndPassesTwoTurnsOn() {
        CardEffect attack = (context, player) -> context.attack(context.nextPlayer(), 2);
        FakeGameContext context = new FakeGameContext(3);
        attack.play(context, P0);
        assertEquals(List.of(new FakeGameContext.Attack(new PlayerId(1), 2)), context.attacks());
    }
}
