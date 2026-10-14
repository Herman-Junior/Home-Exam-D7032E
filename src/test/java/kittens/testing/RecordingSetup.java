package kittens.testing;

import java.util.LinkedHashMap;
import java.util.Map;

import kittens.api.CardType;
import kittens.api.GameSetup;

/**
 * A {@link GameSetup} for tests that only records what an expansion asks for, so a test can check the setup rules
 * directly, for example how many Defuse cards go into the deck, without playing any game. The fields are public so
 * that tests can read them.
 */
public final class RecordingSetup implements GameSetup {

    public final Map<CardType, Integer> deck = new LinkedHashMap<>();
    public final Map<CardType, Integer> extras = new LinkedHashMap<>();
    public final Map<CardType, Integer> afterDealing = new LinkedHashMap<>();
    public int dealPerPlayer = -1;
    public int minPlayers = -1;
    public int maxPlayers = -1;
    private final int playerCount;

    public RecordingSetup(int playerCount) {
        this.playerCount = playerCount;
    }

    @Override
    public int playerCount() {
        return playerCount;
    }

    @Override
    public void allowPlayerCount(int min, int max) {
        minPlayers = min;
        maxPlayers = max;
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
        dealPerPlayer = count;
    }
}
