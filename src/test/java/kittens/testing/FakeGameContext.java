package kittens.testing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import kittens.api.Answer;
import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Decision;
import kittens.api.GameContext;
import kittens.api.GameEvent;
import kittens.api.PlayerId;
import kittens.api.PlayerPort;

/**
 * An in-memory {@link GameContext} for testing card effects without the engine. It holds a small game state (hands,
 * deck, discard pile, who is in the game) and records what the effect did, so a test can set up a situation, run an
 * effect and check the result.
 * <p>
 * It is deterministic: {@link #shuffleDeck()} only counts the shuffles, and {@link #stealRandomCard} always takes
 * the first card of the hand. Answers to {@link #ask} come from the {@link PlayerPort} registered for the player,
 * usually a {@link ScriptedPlayer}. {@link #askAll} asks the registered players in the order given, and a player
 * without a registered port counts as one who did not answer in time.
 */
public final class FakeGameContext implements GameContext {

    /** An attack that an effect asked for. */
    public record Attack(PlayerId victim, int turns) {
    }

    private final List<PlayerId> order = new ArrayList<>();
    private final Map<PlayerId, List<CardType>> hands = new HashMap<>();
    private final Map<PlayerId, PlayerPort> ports = new HashMap<>();
    private final Map<PlayerId, List<GameEvent>> told = new HashMap<>();
    private final List<CardType> deck = new ArrayList<>();
    private final List<CardType> discardPile = new ArrayList<>();
    private final Set<PlayerId> eliminated = new LinkedHashSet<>();
    private final List<GameEvent> announced = new ArrayList<>();
    private final List<Attack> attacks = new ArrayList<>();
    private final Map<CardType, CardDefinition> definitions = new LinkedHashMap<>();
    private final List<Duration> askAllLimits = new ArrayList<>();
    private PlayerId current;
    private int turnsEnded;
    private int shuffles;

    public FakeGameContext(int playerCount) {
        for (int i = 0; i < playerCount; i++) {
            PlayerId id = new PlayerId(i);
            order.add(id);
            hands.put(id, new ArrayList<>());
            told.put(id, new ArrayList<>());
        }
        current = order.get(0);
    }

    // ---- setting up a situation ----

    public FakeGameContext withHand(int player, CardType... cards) {
        hands.get(new PlayerId(player)).addAll(List.of(cards));
        return this;
    }

    /** The cards of the deck, top card first. */
    public FakeGameContext withDeck(CardType... cards) {
        deck.addAll(List.of(cards));
        return this;
    }

    public FakeGameContext withPlayer(int player, PlayerPort port) {
        ports.put(new PlayerId(player), port);
        return this;
    }

    public FakeGameContext withDefinitions(CardDefinition... cards) {
        for (CardDefinition card : cards) {
            definitions.put(card.type(), card);
        }
        return this;
    }

    public FakeGameContext withCurrentPlayer(int player) {
        current = new PlayerId(player);
        return this;
    }

    // ---- what the effect did ----

    public List<CardType> deck() {
        return Collections.unmodifiableList(new ArrayList<>(deck));
    }

    public List<CardType> discardPile() {
        return Collections.unmodifiableList(new ArrayList<>(discardPile));
    }

    public Set<PlayerId> eliminated() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(eliminated));
    }

    public List<GameEvent> announced() {
        return Collections.unmodifiableList(new ArrayList<>(announced));
    }

    public List<GameEvent> toldTo(int player) {
        return Collections.unmodifiableList(new ArrayList<>(told.get(new PlayerId(player))));
    }

    public List<Attack> attacks() {
        return Collections.unmodifiableList(new ArrayList<>(attacks));
    }

    /** The time limits of the timed questions that were asked, in order. */
    public List<Duration> askAllLimits() {
        return Collections.unmodifiableList(new ArrayList<>(askAllLimits));
    }

    public int turnsEnded() {
        return turnsEnded;
    }

    public int shuffles() {
        return shuffles;
    }

    // ---- Messenger ----

    @Override
    public int ask(PlayerId player, Decision decision) {
        PlayerPort port = ports.get(player);
        if (port == null) {
            throw new AssertionError("no player registered for " + player + ", asked " + decision);
        }
        int choice = port.choose(decision);
        if (!decision.accepts(choice)) {
            throw new AssertionError("player " + player + " answered " + choice + " to " + decision);
        }
        return choice;
    }

    @Override
    public List<Answer> askAll(Collection<PlayerId> players, Decision decision, Duration limit) {
        askAllLimits.add(limit);
        List<Answer> answers = new ArrayList<>();
        for (PlayerId player : players) {
            if (ports.containsKey(player)) {
                answers.add(new Answer(player, ask(player, decision)));
            }
        }
        return answers;
    }

    @Override
    public Optional<CardDefinition> definition(CardType type) {
        return Optional.ofNullable(definitions.get(type));
    }

    @Override
    public List<CardType> cardTypes() {
        return List.copyOf(definitions.keySet());
    }

    @Override
    public void announce(GameEvent event) {
        announced.add(event);
    }

    @Override
    public void tell(PlayerId player, GameEvent event) {
        told.get(player).add(event);
    }

    // ---- DeckAccess ----

    @Override
    public int deckSize() {
        return deck.size();
    }

    @Override
    public List<CardType> peekTop(int count) {
        return List.copyOf(deck.subList(0, Math.min(count, deck.size())));
    }

    @Override
    public void insertIntoDeck(CardType card, int position) {
        if (position < 0 || position > deck.size()) {
            throw new AssertionError("no such deck position " + position + ", the deck has " + deck.size() + " cards");
        }
        deck.add(position, card);
    }

    @Override
    public void shuffleDeck() {
        shuffles++;
    }

    // ---- HandAccess ----

    @Override
    public List<CardType> hand(PlayerId player) {
        return List.copyOf(hands.get(player));
    }

    @Override
    public void addToHand(PlayerId player, CardType card) {
        hands.get(player).add(card);
    }

    @Override
    public void moveCard(PlayerId from, PlayerId to, CardType card) {
        remove(from, card);
        hands.get(to).add(card);
    }

    @Override
    public Optional<CardType> stealRandomCard(PlayerId from, PlayerId to) {
        List<CardType> source = hands.get(from);
        if (source.isEmpty()) {
            return Optional.empty();
        }
        CardType card = source.remove(0);
        hands.get(to).add(card);
        return Optional.of(card);
    }

    @Override
    public void discard(PlayerId player, CardType card) {
        remove(player, card);
        discardPile.add(card);
    }

    private void remove(PlayerId player, CardType card) {
        if (!hands.get(player).remove(card)) {
            throw new AssertionError("player " + player + " does not hold " + card);
        }
    }

    // ---- TurnControl ----

    @Override
    public PlayerId currentPlayer() {
        return current;
    }

    @Override
    public PlayerId nextPlayer() {
        return otherPlayers().isEmpty() ? current : otherPlayers().get(0);
    }

    @Override
    public List<PlayerId> otherPlayers() {
        List<PlayerId> others = new ArrayList<>();
        int start = order.indexOf(current);
        for (int i = 1; i < order.size(); i++) {
            PlayerId candidate = order.get((start + i) % order.size());
            if (!eliminated.contains(candidate)) {
                others.add(candidate);
            }
        }
        return others;
    }

    @Override
    public void endTurn() {
        turnsEnded++;
    }

    @Override
    public void attack(PlayerId victim, int turns) {
        attacks.add(new Attack(victim, turns));
    }

    @Override
    public void eliminate(PlayerId player) {
        eliminated.add(player);
        discardPile.addAll(hands.get(player));
        hands.get(player).clear();
    }
}
