package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.ATTACK;
import static kittens.expansion.base.BaseCards.DEFUSE;
import static kittens.expansion.base.BaseCards.EXPLODING_KITTEN;
import static kittens.expansion.base.BaseCards.FAVOR;
import static kittens.expansion.base.BaseCards.SEE_THE_FUTURE;
import static kittens.expansion.base.BaseCards.SHUFFLE;
import static kittens.expansion.base.BaseCards.SKIP;
import static kittens.expansion.base.BaseCards.TACO_CAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.testing.FakeGameContext;
import kittens.testing.ScriptedPlayer;

/** What each card of the base game does (R10 and R11), run against the fake context. */
class BaseCardsTest {

    private static final PlayerId P0 = new PlayerId(0);
    private static final PlayerId P1 = new PlayerId(1);
    private static final PlayerId P2 = new PlayerId(2);

    private final FakeGameContext context = new FakeGameContext(3)
            .withDefinitions(new BaseGame().cards().toArray(CardDefinition[]::new));

    private static CardDefinition definition(CardType type) {
        return new BaseGame().cards().stream().filter(card -> card.type().equals(type)).findFirst().orElseThrow();
    }

    private void play(CardType card) {
        definition(card).playEffect().orElseThrow().play(context, P0);
    }

    private void draw(CardType card) {
        definition(card).drawEffect().orElseThrow().onDrawn(context, P0, card);
    }

    @Test
    void attackEndsTheTurnAndGivesTheNextPlayerTwoTurns() { // R11a
        play(ATTACK);
        assertEquals(List.of(new FakeGameContext.Attack(P1, 2)), context.attacks());
    }

    @Test
    void favorMakesTheChosenPlayerGiveACardOfTheirOwnChoice() { // R11b
        context.withHand(1, SKIP, TACO_CAT).withHand(2, ATTACK)
                .withPlayer(0, ScriptedPlayer.indexes(0))            // target: the first other player, player 1
                .withPlayer(1, ScriptedPlayer.options("give taco-cat"));

        play(FAVOR);

        assertEquals(List.of(TACO_CAT), context.hand(P0));
        assertEquals(List.of(SKIP), context.hand(P1));
        assertEquals(List.of(new GameEvent("card-received", List.of("taco-cat", "1"))), context.toldTo(0));
    }

    @Test
    void favorFromAPlayerWithNoCardsDoesNothing() {
        context.withPlayer(0, ScriptedPlayer.indexes(0));
        play(FAVOR);
        assertTrue(context.hand(P0).isEmpty());
        assertEquals(List.of(GameEvent.of("favor-failed", "0", "1")), context.announced());
    }

    @Test
    void favorOffersEachKindOfCardOnlyOnce() {
        ScriptedPlayer giver = ScriptedPlayer.indexes(0);
        context.withHand(1, SKIP, SKIP, ATTACK).withPlayer(0, ScriptedPlayer.indexes(0)).withPlayer(1, giver);

        play(FAVOR);

        assertEquals(List.of("give skip", "give attack"), ScriptedPlayer.describe(giver.asked().get(0)));
    }

    @Test
    void shuffleShufflesTheDeck() { // R11c
        play(SHUFFLE);
        assertEquals(1, context.shuffles());
    }

    @Test
    void skipEndsTheTurnWithoutDrawing() { // R11d
        play(SKIP);
        assertEquals(1, context.turnsEnded());
    }

    @Test
    void seeTheFutureShowsTheTopThreeCardsPrivately() { // R11e
        context.withDeck(SKIP, ATTACK, SHUFFLE, TACO_CAT);
        play(SEE_THE_FUTURE);
        assertEquals(List.of(GameEvent.of("top-cards", "skip", "attack", "shuffle")), context.toldTo(0));
        assertEquals(4, context.deckSize());
        assertTrue(context.announced().isEmpty());
    }

    @Test
    void seeTheFutureWorksWithFewerThanThreeCardsLeft() { // the original crashed here
        context.withDeck(SKIP);
        play(SEE_THE_FUTURE);
        assertEquals(List.of(GameEvent.of("top-cards", "skip")), context.toldTo(0));
    }

    @Test
    void aDefusedKittenGoesBackWhereThePlayerChoosesEvenTheBottom() { // R10a
        context.withHand(0, DEFUSE, TACO_CAT).withDeck(SKIP, SKIP).withPlayer(0, ScriptedPlayer.indexes(2));

        draw(EXPLODING_KITTEN);

        assertEquals(List.of(SKIP, SKIP, EXPLODING_KITTEN), context.deck());
        assertEquals(List.of(TACO_CAT), context.hand(P0));
        assertEquals(List.of(DEFUSE), context.discardPile());
        assertTrue(context.eliminated().isEmpty());
        assertEquals(List.of(GameEvent.of("defused", "0")), context.announced());
    }

    @Test
    void withoutADefuseThePlayerDiscardsTheHandAndTheKittenAndIsOut() { // R10b
        context.withHand(0, TACO_CAT, SKIP);

        draw(EXPLODING_KITTEN);

        assertTrue(context.eliminated().contains(P0));
        assertTrue(context.hand(P0).isEmpty());
        assertEquals(List.of(TACO_CAT, SKIP, EXPLODING_KITTEN), context.discardPile());
    }

    @Test
    void theKittenCannotBeNopedAndEveryOtherPlayableCardCan() {
        assertFalse(definition(EXPLODING_KITTEN).nopeable());
        assertTrue(definition(ATTACK).nopeable());
        assertTrue(definition(SKIP).nopeable());
    }

    @Test
    void onlyTheRightCardsCanBePlayedForAnEffect() {
        assertTrue(definition(ATTACK).playEffect().isPresent());
        assertTrue(definition(TACO_CAT).playEffect().isEmpty());
        assertTrue(definition(DEFUSE).playEffect().isEmpty());
        assertEquals(Optional.empty(), definition(BaseCards.NOPE).playEffect());
        assertTrue(definition(EXPLODING_KITTEN).playEffect().isEmpty());
    }
}
