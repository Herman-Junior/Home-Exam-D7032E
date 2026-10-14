package kittens.expansion.base;

import java.util.List;

import kittens.api.CardType;
import kittens.api.GameContext;
import kittens.api.PlayerId;

/** What a combination of cards does once it is played. */
@FunctionalInterface
interface ComboEffect {

    void apply(GameContext context, PlayerId player, List<CardType> cards);
}
