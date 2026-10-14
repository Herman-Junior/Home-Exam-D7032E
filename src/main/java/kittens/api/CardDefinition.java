package kittens.api;

import java.util.Optional;

/**
 * Describes one card type: what it does when played, what it does when drawn, and whether it can be Noped. An
 * {@link Expansion} supplies the definitions of its cards. A card with neither effect, such as a cat card, only
 * counts in combinations.
 */
public final class CardDefinition {

    private final CardType type;
    private final CardEffect playEffect;
    private final DrawEffect drawEffect;
    private final boolean nopeable;

    private CardDefinition(CardType type, CardEffect playEffect, DrawEffect drawEffect, boolean nopeable) {
        if (type == null) {
            throw new IllegalArgumentException("a card definition needs a card type");
        }
        this.type = type;
        this.playEffect = playEffect;
        this.drawEffect = drawEffect;
        this.nopeable = nopeable;
    }

    /** A card with no effect of its own. */
    public static CardDefinition passive(CardType type) {
        return new CardDefinition(type, null, null, true);
    }

    /** A card that can be played for an effect. It can be Noped. */
    public static CardDefinition playable(CardType type, CardEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("a playable card needs an effect");
        }
        return new CardDefinition(type, effect, null, true);
    }

    /** A card with an effect when it is drawn. */
    public static CardDefinition drawn(CardType type, DrawEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("a card with a draw effect needs an effect");
        }
        return new CardDefinition(type, null, effect, true);
    }

    /** The same card, but it cannot be Noped. */
    public CardDefinition notNopeable() {
        return new CardDefinition(type, playEffect, drawEffect, false);
    }

    public CardType type() {
        return type;
    }

    public Optional<CardEffect> playEffect() {
        return Optional.ofNullable(playEffect);
    }

    public Optional<DrawEffect> drawEffect() {
        return Optional.ofNullable(drawEffect);
    }

    public boolean nopeable() {
        return nopeable;
    }
}
