package kittens.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CardDefinitionTest {

    private static final CardType SKIP = new CardType("skip");

    @Test
    void aPassiveCardHasNoEffects() {
        CardDefinition cat = CardDefinition.passive(new CardType("taco-cat"));
        assertTrue(cat.playEffect().isEmpty());
        assertTrue(cat.drawEffect().isEmpty());
    }

    @Test
    void aPlayableCardHasAPlayEffectAndCanBeNoped() {
        CardDefinition skip = CardDefinition.playable(SKIP, (context, player) -> context.endTurn());
        assertTrue(skip.playEffect().isPresent());
        assertTrue(skip.drawEffect().isEmpty());
        assertTrue(skip.nopeable());
    }

    @Test
    void aCardWithADrawEffectHasNoPlayEffect() {
        CardDefinition kitten = CardDefinition.drawn(new CardType("exploding-kitten"), (context, player, card) -> { });
        assertTrue(kitten.drawEffect().isPresent());
        assertTrue(kitten.playEffect().isEmpty());
    }

    @Test
    void notNopeableGivesACopyAndLeavesTheOriginalAlone() {
        CardDefinition skip = CardDefinition.playable(SKIP, (context, player) -> { });
        CardDefinition unstoppable = skip.notNopeable();
        assertFalse(unstoppable.nopeable());
        assertTrue(skip.nopeable());
        assertEquals(SKIP, unstoppable.type());
        assertTrue(unstoppable.playEffect().isPresent());
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> CardDefinition.playable(SKIP, null));
        assertThrows(IllegalArgumentException.class, () -> CardDefinition.drawn(SKIP, null));
        assertThrows(IllegalArgumentException.class, () -> CardDefinition.passive(null));
        assertThrows(IllegalArgumentException.class, () -> new CardType(" "));
        assertThrows(IllegalArgumentException.class, () -> new PlayerId(-1));
    }
}
