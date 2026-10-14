package kittens.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import kittens.api.CardCatalog;
import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.Expansion;
import kittens.api.Reaction;

/** The content of all the expansions in a game, gathered in one place. */
final class ExpansionSet implements CardCatalog {

    private final Map<CardType, CardDefinition> definitions = new LinkedHashMap<>();
    private final List<Combo> combos = new ArrayList<>();
    private final List<Reaction> reactions = new ArrayList<>();

    ExpansionSet(List<Expansion> expansions) {
        Set<String> ids = new HashSet<>();
        Map<CardType, String> owners = new HashMap<>();
        for (Expansion expansion : expansions) {
            if (!ids.add(expansion.id())) {
                throw new IllegalArgumentException("the expansion " + expansion.id() + " is in the game twice");
            }
            for (CardDefinition definition : expansion.cards()) {
                String earlier = owners.put(definition.type(), expansion.id());
                if (earlier != null) {
                    throw new IllegalArgumentException("the card " + definition.type().id() + " is defined by both "
                            + earlier + " and " + expansion.id());
                }
                definitions.put(definition.type(), definition);
            }
            combos.addAll(expansion.combos());
            reactions.addAll(expansion.reactions());
        }
    }

    @Override
    public Optional<CardDefinition> definition(CardType type) {
        return Optional.ofNullable(definitions.get(type));
    }

    @Override
    public List<CardType> cardTypes() {
        return List.copyOf(definitions.keySet());
    }

    List<Combo> combos() {
        return combos;
    }

    List<Reaction> reactions() {
        return reactions;
    }
}
