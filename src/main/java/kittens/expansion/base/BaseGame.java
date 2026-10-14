package kittens.expansion.base;

import static kittens.expansion.base.BaseCards.ATTACK;
import static kittens.expansion.base.BaseCards.CATTERMELON;
import static kittens.expansion.base.BaseCards.DEFUSE;
import static kittens.expansion.base.BaseCards.EXPLODING_KITTEN;
import static kittens.expansion.base.BaseCards.FAVOR;
import static kittens.expansion.base.BaseCards.HAIRY_POTATO_CAT;
import static kittens.expansion.base.BaseCards.NOPE;
import static kittens.expansion.base.BaseCards.OVERWEIGHT_BIKINI_CAT;
import static kittens.expansion.base.BaseCards.RAINBOW_RALPHING_CAT;
import static kittens.expansion.base.BaseCards.SEE_THE_FUTURE;
import static kittens.expansion.base.BaseCards.SHUFFLE;
import static kittens.expansion.base.BaseCards.SKIP;
import static kittens.expansion.base.BaseCards.TACO_CAT;

import java.time.Duration;
import java.util.List;

import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.Expansion;
import kittens.api.GameSetup;
import kittens.api.Reaction;

/**
 * The base game, as an expansion like the others: its cards with their effects, how many of each go in the deck, how
 * the players start, the combos of two and three of a kind, and the Nope rule. It implements requirements 1 to 13.
 */
public final class BaseGame implements Expansion {

    public static final String ID = "base-game";

    /** The time that players have to play a Nope after a card is played (R12). */
    public static final Duration NOPE_WINDOW = Duration.ofSeconds(5);

    private static final List<CardType> CATS =
            List.of(HAIRY_POTATO_CAT, CATTERMELON, RAINBOW_RALPHING_CAT, TACO_CAT, OVERWEIGHT_BIKINI_CAT);

    private final Duration nopeWindow;

    public BaseGame() {
        this(NOPE_WINDOW);
    }

    /** A base game with another Nope window, for a test that must not wait five seconds. */
    public BaseGame(Duration nopeWindow) {
        this.nopeWindow = nopeWindow;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<CardDefinition> cards() {
        List<CardDefinition> cards = new java.util.ArrayList<>(List.of(
                CardDefinition.drawn(EXPLODING_KITTEN, new ExplodingKittenEffect()).notNopeable(),
                CardDefinition.passive(DEFUSE),
                CardDefinition.playable(ATTACK, (context, player) -> context.attack(context.nextPlayer(), 2)),
                CardDefinition.playable(FAVOR, new FavorEffect()),
                CardDefinition.passive(NOPE),
                CardDefinition.playable(SHUFFLE, (context, player) -> context.shuffleDeck()),
                CardDefinition.playable(SKIP, (context, player) -> context.endTurn()),
                CardDefinition.playable(SEE_THE_FUTURE, new SeeTheFutureEffect())));
        for (CardType cat : CATS) {
            cards.add(CardDefinition.passive(cat).withTag(BaseCards.CAT));
        }
        return cards;
    }

    @Override
    public void setUp(GameSetup setup) {
        int players = setup.playerCount();
        setup.allowPlayerCount(2, 5);                                  // R1
        setup.addToDeck(DEFUSE, players == 5 ? 1 : 2);                 // R3a
        setup.addToDeck(ATTACK, 4);                                    // R3b
        setup.addToDeck(FAVOR, 4);                                     // R3c
        setup.addToDeck(NOPE, 5);                                      // R3d
        setup.addToDeck(SHUFFLE, 4);                                   // R3e
        setup.addToDeck(SKIP, 4);                                      // R3f
        setup.addToDeck(SEE_THE_FUTURE, 5);                            // R3g
        CATS.forEach(cat -> setup.addToDeck(cat, 4));                  // R3h to R3l
        setup.giveEachPlayer(DEFUSE, 1);                               // R2
        setup.dealPerPlayer(7);                                        // R5
        setup.addAfterDealing(EXPLODING_KITTEN, players - 1);          // R6
    }

    @Override
    public List<Combo> combos() {
        return List.of(
                new SameKindCombo(2, new StealRandomCard()),           // R11f
                new SameKindCombo(3, new NameAndTakeCard()));          // R11g
    }

    @Override
    public List<Reaction> reactions() {
        return List.of(new NopeRule(nopeWindow));                      // R12
    }
}
