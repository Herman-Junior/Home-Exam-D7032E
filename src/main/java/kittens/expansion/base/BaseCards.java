package kittens.expansion.base;

import kittens.api.CardType;

/** The cards of the base game. */
public final class BaseCards {

    public static final CardType EXPLODING_KITTEN = new CardType("exploding-kitten");
    public static final CardType DEFUSE = new CardType("defuse");
    public static final CardType ATTACK = new CardType("attack");
    public static final CardType FAVOR = new CardType("favor");
    public static final CardType NOPE = new CardType("nope");
    public static final CardType SHUFFLE = new CardType("shuffle");
    public static final CardType SKIP = new CardType("skip");
    public static final CardType SEE_THE_FUTURE = new CardType("see-the-future");
    public static final CardType HAIRY_POTATO_CAT = new CardType("hairy-potato-cat");
    public static final CardType CATTERMELON = new CardType("cattermelon");
    public static final CardType RAINBOW_RALPHING_CAT = new CardType("rainbow-ralphing-cat");
    public static final CardType TACO_CAT = new CardType("taco-cat");
    public static final CardType OVERWEIGHT_BIKINI_CAT = new CardType("overweight-bikini-cat");

    /** The tag that the cat cards carry. Expansions can add cards with this tag, or wildcards for it. */
    public static final String CAT = "cat";

    private BaseCards() {
    }
}
