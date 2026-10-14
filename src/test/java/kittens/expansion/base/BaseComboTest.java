package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.ATTACK;
import static kittens.expansion.base.BaseCards.CATTERMELON;
import static kittens.expansion.base.BaseCards.SKIP;
import static kittens.expansion.base.BaseCards.TACO_CAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/** Two and three of a kind (R11f, R11g), and a wildcard card that works with them without changing them. */
class BaseComboTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);

    /** The Feral Cat of Imploding Kittens, as a plain card definition. It is not part of this project's base game. */
    private static final CardType FERAL = new CardType("feral-cat");

    private final Combo pair = new BaseGame().combos().get(0);
    private final Combo triple = new BaseGame().combos().get(1);

    private final FakeGameContext context = new FakeGameContext(3)
            .withDefinitions(new BaseGame().cards().toArray(CardDefinition[]::new))
            .withDefinitions(CardDefinition.passive(FERAL).asWildcardFor(BaseCards.CAT));

    private static boolean sameCards(List<CardType> a, List<CardType> b) {
        List<String> left = new ArrayList<>(a.stream().map(CardType::id).toList());
        List<String> right = new ArrayList<>(b.stream().map(CardType::id).toList());
        left.sort(null);
        right.sort(null);
        return left.equals(right);
    }

    private static boolean offers(List<List<CardType>> plays, CardType... cards) {
        return plays.stream().anyMatch(play -> sameCards(play, List.of(cards)));
    }

    // ---- which plays are offered ----

    @Test
    void twoIdenticalCardsFormAPair() {
        assertEquals(List.of(List.of(TACO_CAT, TACO_CAT)), pair.plays(List.of(TACO_CAT, TACO_CAT, SKIP), context));
    }

    @Test
    void twoCardsOfAnyKindFormAPairNotJustCats() { // R11f says cards "of any kind"
        assertEquals(List.of(List.of(ATTACK, ATTACK)), pair.plays(List.of(ATTACK, ATTACK), context));
    }

    @Test
    void differentCardsAreNoPair() {
        assertTrue(pair.plays(List.of(TACO_CAT, CATTERMELON, SKIP), context).isEmpty());
    }

    @Test
    void threeOfAKindNeedsThreeIdenticalCards() {
        assertEquals(List.of(List.of(TACO_CAT, TACO_CAT, TACO_CAT)),
                triple.plays(List.of(TACO_CAT, TACO_CAT, TACO_CAT, SKIP), context));
        assertTrue(triple.plays(List.of(TACO_CAT, TACO_CAT, SKIP), context).isEmpty());
    }

    @Test
    void aWildcardPairsWithAnyCatWithoutAnyChangeToTheCombo() {
        List<List<CardType>> plays = pair.plays(List.of(TACO_CAT, CATTERMELON, FERAL), context);
        assertEquals(2, plays.size());
        assertTrue(offers(plays, TACO_CAT, FERAL));
        assertTrue(offers(plays, CATTERMELON, FERAL));
    }

    @Test
    void aWildcardDoesNotPairWithACardThatIsNotACat() {
        assertTrue(pair.plays(List.of(SKIP, FERAL), context).isEmpty());
    }

    @Test
    void wildcardsCanCompleteAThreeOfAKind() {
        List<List<CardType>> plays = triple.plays(List.of(TACO_CAT, FERAL, FERAL, CATTERMELON), context);
        assertEquals(2, plays.size()); // each of the two cats can be completed by the two wildcards
        assertTrue(offers(plays, TACO_CAT, FERAL, FERAL));
        assertTrue(offers(plays, CATTERMELON, FERAL, FERAL));
        assertTrue(triple.plays(List.of(TACO_CAT, CATTERMELON, FERAL), context).isEmpty());
    }

    // ---- what they do ----

    @Test
    void twoOfAKindTakesARandomCardFromTheChosenPlayer() { // R11f
        context.withHand(1, SKIP).withPlayer(0, ScriptedPlayer.options("target 1"));

        pair.play(context, P0, List.of(TACO_CAT, TACO_CAT));

        assertEquals(List.of(SKIP), context.hand(P0));
        assertTrue(context.hand(P1).isEmpty());
        assertEquals(List.of(GameEvent.of("card-received", "skip", "1")), context.toldTo(0));
        assertEquals(List.of(GameEvent.of("card-taken", "skip", "0")), context.toldTo(1));
    }

    @Test
    void twoOfAKindFromAPlayerWithNoCardsTakesNothing() {
        context.withPlayer(0, ScriptedPlayer.options("target 1"));
        pair.play(context, P0, List.of(TACO_CAT, TACO_CAT));
        assertTrue(context.hand(P0).isEmpty());
        assertTrue(context.toldTo(0).isEmpty());
    }

    @Test
    void threeOfAKindTakesTheNamedCardIfThePlayerHasIt() { // R11g
        context.withHand(1, SKIP, ATTACK).withPlayer(0, ScriptedPlayer.options("target 1", "name attack"));

        triple.play(context, P0, List.of(TACO_CAT, TACO_CAT, TACO_CAT));

        assertEquals(List.of(ATTACK), context.hand(P0));
        assertEquals(List.of(SKIP), context.hand(P1));
        assertEquals(List.of(GameEvent.of("card-received", "attack", "1")), context.toldTo(0));
    }

    @Test
    void threeOfAKindTakesNothingIfThePlayerDoesNotHaveTheNamedCard() { // R11g "if available"
        context.withHand(1, SKIP).withPlayer(0, ScriptedPlayer.options("target 1", "name attack"));

        triple.play(context, P0, List.of(TACO_CAT, TACO_CAT, TACO_CAT));

        assertTrue(context.hand(P0).isEmpty());
        assertEquals(List.of(SKIP), context.hand(P1));
        assertEquals(List.of(GameEvent.of("card-not-found", "attack", "1")), context.toldTo(0));
    }

    @Test
    void anyCardOfTheGameCanBeNamed() {
        ScriptedPlayer player = ScriptedPlayer.options("target 1", "name attack");
        context.withPlayer(0, player);
        triple.play(context, P0, List.of(TACO_CAT, TACO_CAT, TACO_CAT));
        List<String> names = ScriptedPlayer.describe(player.asked().get(1));
        assertTrue(names.contains("name skip"));
        assertTrue(names.contains("name feral-cat"));
        assertEquals(context.cardTypes().size(), names.size());
    }
}
