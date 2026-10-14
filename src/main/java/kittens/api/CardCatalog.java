package kittens.api;

import java.util.Optional;

/** Looks up what is known about a card type, from the {@link CardDefinition}s of all the expansions in the game. */
public interface CardCatalog {

    Optional<CardDefinition> definition(CardType type);
}
