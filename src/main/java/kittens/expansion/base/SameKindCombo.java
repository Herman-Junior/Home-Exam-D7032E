package kittens.expansion.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import kittens.api.CardCatalog;
import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.GameContext;
import kittens.api.PlayerId;

/**
 * Several cards "of a kind" played together, for example two or three of a kind. The cards are of a kind when they are
 * all the same card, or when some of them are wildcards for a tag that the others carry. A Feral Cat, which counts as
 * any cat card, therefore works with this combo without any change to it: it is a card that is a wildcard for the
 * {@link BaseCards#CAT} tag.
 */
final class SameKindCombo implements Combo {

    private final int size;
    private final ComboEffect effect;

    SameKindCombo(int size, ComboEffect effect) {
        this.size = size;
        this.effect = effect;
    }

    @Override
    public List<List<CardType>> plays(List<CardType> hand, CardCatalog catalog) {
        Map<CardType, Integer> available = new HashMap<>();
        for (CardType card : hand) {
            available.merge(card, 1, Integer::sum);
        }
        List<List<CardType>> plays = new ArrayList<>();
        collect(hand.stream().distinct().toList(), 0, available, new ArrayList<>(), plays, catalog);
        return plays;
    }

    /** Builds every selection of {@code size} cards from the hand once, without regard to order. */
    private void collect(List<CardType> kinds, int from, Map<CardType, Integer> available, List<CardType> chosen,
            List<List<CardType>> plays, CardCatalog catalog) {
        if (chosen.size() == size) {
            if (ofAKind(chosen, catalog)) {
                plays.add(List.copyOf(chosen));
            }
            return;
        }
        for (int i = from; i < kinds.size(); i++) {
            CardType kind = kinds.get(i);
            if (available.get(kind) == 0) {
                continue;
            }
            available.merge(kind, -1, Integer::sum);
            chosen.add(kind);
            collect(kinds, i, available, chosen, plays, catalog);
            chosen.remove(chosen.size() - 1);
            available.merge(kind, 1, Integer::sum);
        }
    }

    private static boolean ofAKind(List<CardType> cards, CardCatalog catalog) {
        List<CardType> real = cards.stream().filter(card -> wildcardTag(card, catalog).isEmpty()).toList();
        if (real.isEmpty()) {
            return true;
        }
        CardType kind = real.get(0);
        if (!real.stream().allMatch(kind::equals)) {
            return false;
        }
        Set<String> tags = catalog.definition(kind).map(CardDefinition::tags).orElse(Set.of());
        return cards.stream().allMatch(card -> card.equals(kind)
                || wildcardTag(card, catalog).map(tags::contains).orElse(false));
    }

    private static Optional<String> wildcardTag(CardType card, CardCatalog catalog) {
        return catalog.definition(card).flatMap(CardDefinition::wildcardFor);
    }

    @Override
    public void play(GameContext context, PlayerId player, List<CardType> cards) {
        effect.apply(context, player, cards);
    }
}
