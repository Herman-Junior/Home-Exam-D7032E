package kittens.api;

import java.util.List;
import java.util.Optional;

/** Looks up what is known about a card type, from the {@link CardDefinition}s of all the expansions in the game. */
public interface CardCatalog {

    Optional<CardDefinition> definition(CardType type);

    /** Every card type in the game, in a stable order: the expansions in turn, each with its cards in order. */
    List<CardType> cardTypes();
}
