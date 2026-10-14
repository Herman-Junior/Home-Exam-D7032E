package kittens.engine;

import java.util.List;

import kittens.api.CardType;
import kittens.api.Combo;

/** What a player can do on a turn. The engine keeps one of these for every option it offers. */
sealed interface TurnChoice {

    /** End the turn by drawing a card. */
    record Pass() implements TurnChoice {
    }

    /** Play one card for its effect. */
    record PlayCard(CardType type) implements TurnChoice {
    }

    /** Play several cards together as a combo. */
    record PlayCombo(Combo combo, List<CardType> cards) implements TurnChoice {
    }
}
