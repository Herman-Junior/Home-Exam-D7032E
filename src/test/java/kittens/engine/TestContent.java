package kittens.engine;

import java.util.List;
import java.util.function.Consumer;

import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.DrawEffect;
import kittens.api.Expansion;
import kittens.api.GameEvent;
import kittens.api.GameSetup;
import kittens.api.Option;
import kittens.api.Reaction;

import java.util.ArrayList;

/**
 * A few invented cards for testing the engine. The engine knows no real card, so its tests use content of their own
 * instead of the base game: the rules of the engine are checked on their own.
 */
final class TestContent {

    static final CardType DEFUSE = new CardType("defuse");
    static final CardType KITTEN = new CardType("kitten");
    static final CardType SKIP = new CardType("skip");
    static final CardType ATTACK = new CardType("attack");
    static final CardType SHUFFLE = new CardType("shuffle");
    static final CardType UNSTOPPABLE = new CardType("unstoppable");
    static final CardType CAT = new CardType("cat");

    /** A drawn kitten: defuse it and put it back wherever the player wants, or be eliminated. */
    static final DrawEffect KITTEN_EFFECT = (context, player, card) -> {
        if (context.hand(player).contains(DEFUSE)) {
            context.discard(player, DEFUSE);
            List<Option> positions = new ArrayList<>();
            for (int position = 0; position <= context.deckSize(); position++) {
                positions.add(Option.of("position", Integer.toString(position)));
            }
            context.insertIntoDeck(card, context.ask(player, new Decision(DecisionKind.DEFUSE_POSITION, positions)));
        } else {
            context.addToHand(player, card);
            context.eliminate(player);
        }
    };

    static List<CardDefinition> allCards() {
        return List.of(
                CardDefinition.playable(SKIP, (context, player) -> context.endTurn()),
                CardDefinition.playable(ATTACK, (context, player) -> context.attack(context.nextPlayer(), 2)),
                CardDefinition.playable(SHUFFLE, (context, player) -> context.shuffleDeck()),
                CardDefinition.playable(UNSTOPPABLE,
                        (context, player) -> context.announce(GameEvent.of("unstoppable-played"))).notNopeable(),
                CardDefinition.drawn(KITTEN, KITTEN_EFFECT),
                CardDefinition.passive(DEFUSE),
                CardDefinition.passive(CAT).withTag("cat"));
    }

    /** Two to five players, nobody is dealt anything. A test arranges the cards it needs. */
    static Consumer<GameSetup> plainSetup() {
        return setup -> {
            setup.allowPlayerCount(2, 5);
            setup.dealPerPlayer(0);
        };
    }

    static TestExpansion expansion() {
        return expansion(plainSetup());
    }

    static TestExpansion expansion(Consumer<GameSetup> setup) {
        return new TestExpansion("test", allCards(), setup, List.of(), List.of());
    }

    /** An expansion of the engine's tests. */
    static final class TestExpansion implements Expansion {

        private final String id;
        private final List<CardDefinition> cards;
        private final Consumer<GameSetup> setup;
        private final List<Combo> combos;
        private final List<Reaction> reactions;

        TestExpansion(String id, List<CardDefinition> cards, Consumer<GameSetup> setup, List<Combo> combos,
                List<Reaction> reactions) {
            this.id = id;
            this.cards = cards;
            this.setup = setup;
            this.combos = combos;
            this.reactions = reactions;
        }

        TestExpansion withId(String newId) {
            return new TestExpansion(newId, cards, setup, combos, reactions);
        }

        TestExpansion withCards(List<CardDefinition> newCards) {
            return new TestExpansion(id, newCards, setup, combos, reactions);
        }

        TestExpansion withCombos(Combo... more) {
            return new TestExpansion(id, cards, setup, List.of(more), reactions);
        }

        TestExpansion withReactions(Reaction... more) {
            return new TestExpansion(id, cards, setup, combos, List.of(more));
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public List<CardDefinition> cards() {
            return cards;
        }

        @Override
        public void setUp(GameSetup plan) {
            setup.accept(plan);
        }

        @Override
        public List<Combo> combos() {
            return combos;
        }

        @Override
        public List<Reaction> reactions() {
            return reactions;
        }
    }

    private TestContent() {
    }
}
