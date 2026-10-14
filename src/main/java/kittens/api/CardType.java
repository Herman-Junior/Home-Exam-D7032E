package kittens.api;

/**
 * The kind of a card, such as Attack or Reverse. Like {@link DecisionKind} it is an open type identified by an id, so
 * an expansion introduces its own cards without any change to shared code. Two card types are equal when their ids
 * are equal. What a card does is not stored here but in the {@link CardDefinition} that an {@link Expansion} supplies.
 */
public record CardType(String id) {

    public CardType {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a card type needs an id");
        }
    }
}
