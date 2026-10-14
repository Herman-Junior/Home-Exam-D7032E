package kittens.api;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Describes one card type: what it does when played, what it does when drawn, whether it can be Noped, and which tags
 * it carries. An {@link Expansion} supplies the definitions of its cards. A card with neither effect, such as a cat
 * card, only counts in combinations.
 * <p>
 * Tags group cards, for example {@code "cat"}. A card can be a wildcard for a tag, so that it counts as any card with
 * that tag. This is how the Feral Cat of Imploding Kittens works with the combos of the base game without changing
 * them: the combos look at tags and wildcards, and the Feral Cat is just a card that is a wildcard for {@code "cat"}.
 */
public final class CardDefinition {

    private final CardType type;
    private final CardEffect playEffect;
    private final DrawEffect drawEffect;
    private final boolean nopeable;
    private final Set<String> tags;
    private final String wildcardTag;

    private CardDefinition(CardType type, CardEffect playEffect, DrawEffect drawEffect, boolean nopeable,
            Set<String> tags, String wildcardTag) {
        if (type == null) {
            throw new IllegalArgumentException("a card definition needs a card type");
        }
        this.type = type;
        this.playEffect = playEffect;
        this.drawEffect = drawEffect;
        this.nopeable = nopeable;
        this.tags = Set.copyOf(tags);
        this.wildcardTag = wildcardTag;
    }

    /** A card with no effect of its own. */
    public static CardDefinition passive(CardType type) {
        return new CardDefinition(type, null, null, true, Set.of(), null);
    }

    /** A card that can be played for an effect. It can be Noped. */
    public static CardDefinition playable(CardType type, CardEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("a playable card needs an effect");
        }
        return new CardDefinition(type, effect, null, true, Set.of(), null);
    }

    /** A card with an effect when it is drawn. */
    public static CardDefinition drawn(CardType type, DrawEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("a card with a draw effect needs an effect");
        }
        return new CardDefinition(type, null, effect, true, Set.of(), null);
    }

    /** The same card, but it cannot be Noped. */
    public CardDefinition notNopeable() {
        return new CardDefinition(type, playEffect, drawEffect, false, tags, wildcardTag);
    }

    /** The same card with one more tag. */
    public CardDefinition withTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("a tag cannot be blank");
        }
        Set<String> more = new HashSet<>(tags);
        more.add(tag);
        return new CardDefinition(type, playEffect, drawEffect, nopeable, more, wildcardTag);
    }

    /** The same card, but it counts as any card that has the given tag. */
    public CardDefinition asWildcardFor(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("a tag cannot be blank");
        }
        return new CardDefinition(type, playEffect, drawEffect, nopeable, tags, tag);
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

    public Set<String> tags() {
        return tags;
    }

    /** The tag this card is a wildcard for, if it is one. */
    public Optional<String> wildcardFor() {
        return Optional.ofNullable(wildcardTag);
    }
}
