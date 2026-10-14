package kittens.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import kittens.api.PlayerId;

/**
 * Everything that describes a game at one moment: the players with their hands, the deck, the discard pile and whose
 * turn it is. It is plain data with a few lookups and holds no rules, so it can be built in any state for a test.
 * Players are numbered from 0 and take turns in that order.
 */
public final class GameState {

    private final List<PlayerState> players = new ArrayList<>();
    private final Deck deck = new Deck();
    private final DiscardPile discardPile = new DiscardPile();
    private PlayerId current;
    private int turnsRemaining = 1;
    private boolean underAttack;

    public GameState(int playerCount) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("a game needs players");
        }
        for (int i = 0; i < playerCount; i++) {
            players.add(new PlayerState(new PlayerId(i)));
        }
        current = players.get(0).id();
    }

    public List<PlayerState> players() {
        return Collections.unmodifiableList(players);
    }

    public PlayerState player(PlayerId id) {
        if (id.value() >= players.size()) {
            throw new IllegalArgumentException("there is no player " + id);
        }
        return players.get(id.value());
    }

    public Deck deck() {
        return deck;
    }

    public DiscardPile discardPile() {
        return discardPile;
    }

    /** The players still in the game, in turn order. */
    public List<PlayerId> alivePlayers() {
        return players.stream().filter(p -> !p.eliminated()).map(PlayerState::id).toList();
    }

    /** The players still in the game, in turn order, starting with the one after {@code from} and not including it. */
    public List<PlayerId> alivePlayersAfter(PlayerId from) {
        List<PlayerId> result = new ArrayList<>();
        for (int i = 1; i < players.size(); i++) {
            PlayerState candidate = players.get((from.value() + i) % players.size());
            if (!candidate.eliminated()) {
                result.add(candidate.id());
            }
        }
        return result;
    }

    /** The next player in turn order who is still in the game, or {@code from} itself if nobody else is. */
    public PlayerId nextAlive(PlayerId from) {
        List<PlayerId> after = alivePlayersAfter(from);
        return after.isEmpty() ? from : after.get(0);
    }

    public boolean isOver() {
        return alivePlayers().size() <= 1;
    }

    public Optional<PlayerId> winner() {
        List<PlayerId> alive = alivePlayers();
        return alive.size() == 1 ? Optional.of(alive.get(0)) : Optional.empty();
    }

    public PlayerId current() {
        return current;
    }

    public void setCurrent(PlayerId current) {
        this.current = current;
    }

    /** How many turns the current player still has to take, counting the one in progress. */
    public int turnsRemaining() {
        return turnsRemaining;
    }

    public void setTurnsRemaining(int turnsRemaining) {
        this.turnsRemaining = turnsRemaining;
    }

    /** Whether the current player's turns were forced by an attack. */
    public boolean underAttack() {
        return underAttack;
    }

    public void setUnderAttack(boolean underAttack) {
        this.underAttack = underAttack;
    }
}
