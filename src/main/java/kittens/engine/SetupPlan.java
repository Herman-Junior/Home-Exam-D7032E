package kittens.engine;

import java.util.LinkedHashMap;
import java.util.Map;

import kittens.api.CardType;
import kittens.api.GameSetup;

/**
 * Collects what the expansions say about the setup of a game. Nothing is built here: the {@link Game} reads the plan and
 * builds the deck and the hands from it.
 */
final class SetupPlan implements GameSetup {

    private final int playerCount;
    private final Map<CardType, Integer> deck = new LinkedHashMap<>();
    private final Map<CardType, Integer> extras = new LinkedHashMap<>();
    private final Map<CardType, Integer> afterDealing = new LinkedHashMap<>();
    private int dealPerPlayer;
    private Integer minPlayers;
    private Integer maxPlayers;

    SetupPlan(int playerCount) {
        this.playerCount = playerCount;
    }

    @Override
    public int playerCount() {
        return playerCount;
    }

    @Override
    public void allowPlayerCount(int min, int max) {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("not a range of players: " + min + " to " + max);
        }
        minPlayers = minPlayers == null ? min : Math.min(minPlayers, min);
        maxPlayers = maxPlayers == null ? max : Math.max(maxPlayers, max);
    }

    @Override
    public void addToDeck(CardType type, int count) {
        deck.merge(type, count, Integer::sum);
    }

    @Override
    public void giveEachPlayer(CardType type, int count) {
        extras.merge(type, count, Integer::sum);
    }

    @Override
    public void addAfterDealing(CardType type, int count) {
        afterDealing.merge(type, count, Integer::sum);
    }

    @Override
    public void dealPerPlayer(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("cannot deal " + count + " cards");
        }
        dealPerPlayer = count;
    }

    /** Checks the plan: the number of players is allowed, and no card ends up with a negative count. */
    void validate() {
        if (minPlayers == null) {
            throw new IllegalArgumentException("no expansion says how many players can play");
        }
        if (playerCount < minPlayers || playerCount > maxPlayers) {
            throw new IllegalArgumentException(
                    "a game needs between " + minPlayers + " and " + maxPlayers + " players, not " + playerCount);
        }
        for (Map<CardType, Integer> counts : java.util.List.of(deck, extras, afterDealing)) {
            counts.forEach((type, count) -> {
                if (count < 0) {
                    throw new IllegalStateException("the expansions remove more " + type.id() + " than they add");
                }
            });
        }
    }

    Map<CardType, Integer> deck() {
        return deck;
    }

    Map<CardType, Integer> extras() {
        return extras;
    }

    Map<CardType, Integer> afterDealing() {
        return afterDealing;
    }

    int dealPerPlayer() {
        return dealPerPlayer;
    }
}
