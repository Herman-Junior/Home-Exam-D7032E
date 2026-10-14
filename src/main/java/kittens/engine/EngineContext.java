package kittens.engine;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import kittens.api.Answer;
import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Decision;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.api.RandomSource;
import kittens.model.GameState;
import kittens.model.PlayerState;

/**
 * The engine's implementation of {@link GameContext}: what a card effect is allowed to do, carried out on the real
 * {@link GameState}. It also keeps track of how the current turn ends, so that the {@link Game} can decide who is next.
 */
final class EngineContext implements GameContext {

    private final GameState state;
    private final PlayerChannel channel;
    private final ExpansionSet content;
    private final RandomSource random;

    private boolean turnOver;
    private PlayerId pendingVictim;
    private int pendingTurns;

    EngineContext(GameState state, PlayerChannel channel, ExpansionSet content, RandomSource random) {
        this.state = state;
        this.channel = channel;
        this.content = content;
        this.random = random;
    }

    // ---- bookkeeping for the turn loop ----

    void beginTurn() {
        turnOver = false;
        pendingVictim = null;
        pendingTurns = 0;
    }

    boolean turnOver() {
        return turnOver;
    }

    boolean hasPendingAttack() {
        return pendingVictim != null;
    }

    PlayerId pendingVictim() {
        return pendingVictim;
    }

    int pendingTurns() {
        return pendingTurns;
    }

    // ---- Messenger ----

    @Override
    public int ask(PlayerId player, Decision decision) {
        return channel.ask(player, decision);
    }

    @Override
    public List<Answer> askAll(Collection<PlayerId> players, Decision decision, Duration limit) {
        return channel.askAll(players, decision, limit);
    }

    @Override
    public void announce(GameEvent event) {
        channel.announce(event);
    }

    @Override
    public void tell(PlayerId player, GameEvent event) {
        channel.tell(player, event);
    }

    // ---- DeckAccess ----

    @Override
    public int deckSize() {
        return state.deck().size();
    }

    @Override
    public List<CardType> peekTop(int count) {
        return state.deck().peekTop(count);
    }

    @Override
    public void insertIntoDeck(CardType card, int position) {
        state.deck().insert(card, position);
    }

    @Override
    public void shuffleDeck() {
        state.deck().shuffle(random);
    }

    // ---- HandAccess ----

    @Override
    public List<CardType> hand(PlayerId player) {
        return state.player(player).hand().cards();
    }

    @Override
    public void addToHand(PlayerId player, CardType card) {
        state.player(player).hand().add(card);
    }

    @Override
    public void moveCard(PlayerId from, PlayerId to, CardType card) {
        takeFromHand(from, card);
        state.player(to).hand().add(card);
    }

    @Override
    public Optional<CardType> stealRandomCard(PlayerId from, PlayerId to) {
        PlayerState victim = state.player(from);
        if (victim.hand().isEmpty()) {
            return Optional.empty();
        }
        CardType card = victim.hand().removeAt(random.nextInt(victim.hand().size()));
        state.player(to).hand().add(card);
        return Optional.of(card);
    }

    @Override
    public void discard(PlayerId player, CardType card) {
        takeFromHand(player, card);
        state.discardPile().add(card);
    }

    private void takeFromHand(PlayerId player, CardType card) {
        if (!state.player(player).hand().remove(card)) {
            throw new IllegalArgumentException("player " + player + " does not hold " + card.id());
        }
    }

    // ---- TurnControl ----

    @Override
    public PlayerId currentPlayer() {
        return state.current();
    }

    @Override
    public PlayerId nextPlayer() {
        return state.nextAlive(state.current());
    }

    @Override
    public List<PlayerId> otherPlayers() {
        return state.alivePlayersAfter(state.current());
    }

    @Override
    public void endTurn() {
        requireTurnInProgress();
        state.setTurnsRemaining(state.turnsRemaining() - 1);
        turnOver = true;
    }

    @Override
    public void attack(PlayerId victim, int turns) {
        requireTurnInProgress();
        if (state.player(victim).eliminated()) {
            throw new IllegalArgumentException("cannot attack player " + victim + ", who is out of the game");
        }
        int owed = state.underAttack() ? state.turnsRemaining() : 0;
        pendingVictim = victim;
        pendingTurns = turns + owed;
        state.setTurnsRemaining(0);
        turnOver = true;
    }

    @Override
    public void eliminate(PlayerId player) {
        PlayerState out = state.player(player);
        if (out.eliminated()) {
            return;
        }
        for (CardType card : out.hand().cards()) {
            state.discardPile().add(card);
        }
        out.hand().clear();
        out.eliminate();
        channel.announce(GameEvent.of("player-eliminated", player.toString()));
        if (player.equals(state.current())) {
            state.setTurnsRemaining(0);
            turnOver = true;
        }
    }

    private void requireTurnInProgress() {
        if (turnOver) {
            throw new IllegalStateException("the turn is already over");
        }
    }

    // ---- CardCatalog ----

    @Override
    public Optional<CardDefinition> definition(CardType type) {
        return content.definition(type);
    }

    @Override
    public List<CardType> cardTypes() {
        return content.cardTypes();
    }
}
