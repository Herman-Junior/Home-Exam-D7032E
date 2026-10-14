package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/**
 * Checks that a combo can be written against this package alone, and that a wildcard card such as the Feral Cat works
 * with it without the combo being changed. The combo here is a sketch of the pair of the base expansion.
 */
class ComboUsageTest {

    private static final CardType TACO = new CardType("taco-cat");
    private static final CardType MELON = new CardType("cattermelon");
    private static final CardType FERAL = new CardType("feral-cat");
    private static final CardType SKIP = new CardType("skip");

    private static final CardDefinition TACO_DEF = CardDefinition.passive(TACO).withTag("cat");
    private static final CardDefinition MELON_DEF = CardDefinition.passive(MELON).withTag("cat");
    private static final CardDefinition FERAL_DEF = CardDefinition.passive(FERAL).asWildcardFor("cat");
    private static final CardDefinition SKIP_DEF = CardDefinition.passive(SKIP);

    /** Two identical cards, or a card with a tag together with a wildcard for that tag. Steals a random card. */
    private static final Combo PAIR = new Combo() {
        @Override
        public List<List<CardType>> plays(List<CardType> hand, CardCatalog catalog) {
            Set<CardType> kinds = new LinkedHashSet<>(hand);
            List<List<CardType>> plays = new ArrayList<>();
            for (CardType kind : kinds) {
                if (Collections.frequency(hand, kind) >= 2) {
                    plays.add(List.of(kind, kind));
                }
                for (CardType other : kinds) {
                    if (!other.equals(kind) && covers(catalog, other, kind)) {
                        plays.add(List.of(kind, other));
                    }
                }
            }
            return plays;
        }

        /** Whether {@code wildcard} is a wildcard for a tag that {@code card} carries. */
        private boolean covers(CardCatalog catalog, CardType wildcard, CardType card) {
            Optional<String> tag = catalog.definition(wildcard).flatMap(CardDefinition::wildcardFor);
            return tag.isPresent() && catalog.definition(card).map(d -> d.tags().contains(tag.get())).orElse(false);
        }

        @Override
        public void play(GameContext context, PlayerId player, List<CardType> cards) {
            List<Option> targets = context.otherPlayers().stream()
                    .map(target -> Option.of("target", target.toString())).toList();
            int choice = context.ask(player, new Decision(new DecisionKind("choose-target"), targets));
            PlayerId target = context.otherPlayers().get(choice);
            context.stealRandomCard(target, player);
        }
    };

    private final FakeGameContext context = new FakeGameContext(3)
            .withDefinitions(TACO_DEF, MELON_DEF, FERAL_DEF, SKIP_DEF);

    @Test
    void identicalCardsFormAPair() {
        assertEquals(List.of(List.of(TACO, TACO)), PAIR.plays(List.of(TACO, TACO, SKIP), context));
    }

    @Test
    void differentCardsDoNot() {
        assertTrue(PAIR.plays(List.of(TACO, MELON, SKIP), context).isEmpty());
    }

    @Test
    void aWildcardPairsWithAnyCardOfItsTag() {
        List<List<CardType>> plays = PAIR.plays(List.of(TACO, MELON, FERAL), context);
        assertEquals(2, plays.size());
        assertTrue(plays.contains(List.of(TACO, FERAL)));
        assertTrue(plays.contains(List.of(MELON, FERAL)));
    }

    @Test
    void aWildcardDoesNotPairWithACardOutsideItsTag() {
        assertTrue(PAIR.plays(List.of(SKIP, FERAL), context).isEmpty());
    }

    @Test
    void twoWildcardsFormAPair() {
        assertEquals(List.of(List.of(FERAL, FERAL)), PAIR.plays(List.of(FERAL, FERAL), context));
    }

    @Test
    void playingThePairStealsFromTheChosenPlayer() {
        context.withHand(2, SKIP).withPlayer(0, ScriptedPlayer.indexes(1)); // otherPlayers() is [1, 2], index 1 = player 2
        PAIR.play(context, new PlayerId(0), List.of(TACO, TACO));
        assertEquals(List.of(SKIP), context.hand(new PlayerId(0)));
        assertTrue(context.hand(new PlayerId(2)).isEmpty());
    }
}
