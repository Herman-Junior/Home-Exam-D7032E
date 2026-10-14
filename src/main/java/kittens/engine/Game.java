package kittens.engine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import kittens.api.CardDefinition;
import kittens.api.CardType;
import kittens.api.Combo;
import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.DrawEffect;
import kittens.api.Expansion;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayedAction;
import kittens.api.PlayerId;
import kittens.api.RandomSource;
import kittens.model.GameState;
import kittens.model.PlayerState;

/**
 * A game of Exploding Kittens. It sets the game up from the expansions and then plays turn after turn until one
 * player is left. It knows no card: what the cards do comes from the expansions, whom to ask comes from the
 * participants, and chance comes from the {@link RandomSource}.
 */
public final class Game {

    private final GameState state;
    private final EngineContext context;
    private final ExpansionSet content;
    private final PlayerChannel channel;
    private boolean winnerAnnounced;

    private Game(GameState state, EngineContext context, ExpansionSet content, PlayerChannel channel) {
        this.state = state;
        this.context = context;
        this.content = content;
        this.channel = channel;
    }

    /**
     * Sets up a game. The expansions say how many players are allowed, which cards make up the deck and which cards
     * the players start with. Then the deck is shuffled, the hands are dealt, the last cards are shuffled in, and a
     * random player is chosen to go first.
     *
     * @throws IllegalArgumentException if the number of participants is not allowed or the expansions clash
     */
    public static Game create(List<Expansion> expansions, List<Participant> participants, RandomSource random) {
        ExpansionSet content = new ExpansionSet(expansions);
        SetupPlan plan = new SetupPlan(participants.size());
        for (Expansion expansion : expansions) {
            expansion.setUp(plan);
        }
        plan.validate();

        GameState state = new GameState(participants.size());
        plan.deck().forEach((type, count) -> repeat(count, () -> state.deck().addToBottom(type)));
        state.deck().shuffle(random);
        for (PlayerState player : state.players()) {
            for (int i = 0; i < plan.dealPerPlayer(); i++) {
                if (state.deck().isEmpty()) {
                    throw new IllegalStateException("the deck ran out while dealing the hands");
                }
                player.hand().add(state.deck().drawTop());
            }
            plan.extras().forEach((type, count) -> repeat(count, () -> player.hand().add(type)));
        }
        plan.afterDealing().forEach((type, count) -> repeat(count, () -> state.deck().addToBottom(type)));
        state.deck().shuffle(random);
        state.setCurrent(new PlayerId(random.nextInt(participants.size())));

        PlayerChannel channel = new PlayerChannel(participants);
        EngineContext context = new EngineContext(state, channel, content, random);
        channel.announce(GameEvent.of("game-started", Integer.toString(participants.size()),
                state.current().toString()));
        return new Game(state, context, content, channel);
    }

    private static void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }

    /** Plays until one player is left, and returns that player. */
    public PlayerId play() {
        while (!isOver()) {
            playTurn();
        }
        return winner().orElseThrow();
    }

    public boolean isOver() {
        return state.isOver();
    }

    public Optional<PlayerId> winner() {
        return state.winner();
    }

    /**
     * The state of the game, for tests that arrange a situation or inspect the result, and for saving a game. Changing
     * it while a game is being played bypasses the rules.
     */
    public GameState state() {
        return state;
    }

    EngineContext context() {
        return context;
    }

    /**
     * Plays one turn of the current player: the player plays cards for as long as the player likes and then passes
     * and draws, or a card ends the turn. Then the game moves on to whoever is next.
     */
    public void playTurn() {
        if (isOver()) {
            throw new IllegalStateException("the game is over");
        }
        PlayerId player = state.current();
        context.beginTurn();
        channel.announce(GameEvent.of("turn-started", player.toString(), Integer.toString(state.turnsRemaining())));
        while (!context.turnOver() && !state.isOver()) {
            resolve(player, askForAction(player));
        }
        passOn();
    }

    // ---- choosing ----

    private TurnChoice askForAction(PlayerId player) {
        List<CardType> hand = state.player(player).hand().cards();
        List<TurnChoice> choices = new ArrayList<>();
        List<Option> options = new ArrayList<>();
        choices.add(new TurnChoice.Pass());
        options.add(Option.of("pass"));
        for (CardType type : new LinkedHashSet<>(hand)) {
            if (content.definition(type).flatMap(CardDefinition::playEffect).isPresent()) {
                choices.add(new TurnChoice.PlayCard(type));
                options.add(Option.of("play-card", type.id()));
            }
        }
        for (Combo combo : content.combos()) {
            for (List<CardType> cards : combo.plays(hand, content)) {
                requireHeld(hand, cards);
                choices.add(new TurnChoice.PlayCombo(combo, cards));
                options.add(Option.of("play-combo", cards.stream().map(CardType::id).toArray(String[]::new)));
            }
        }
        channel.tell(player, GameEvent.of("hand", hand.stream().map(CardType::id).toArray(String[]::new)));
        int index = channel.ask(player, new Decision(DecisionKind.TURN_ACTION, options));
        return choices.get(index);
    }

    private static void requireHeld(List<CardType> hand, List<CardType> cards) {
        List<CardType> remaining = new ArrayList<>(hand);
        for (CardType card : cards) {
            if (!remaining.remove(card)) {
                throw new IllegalStateException("a combo offered cards that the player does not hold: " + cards);
            }
        }
    }

    // ---- doing ----

    private void resolve(PlayerId player, TurnChoice choice) {
        if (choice instanceof TurnChoice.Pass) {
            draw(player);
        } else if (choice instanceof TurnChoice.PlayCard single) {
            CardDefinition definition = content.definition(single.type()).orElseThrow();
            play(player, List.of(single.type()), definition.nopeable(),
                    () -> definition.playEffect().orElseThrow().play(context, player));
        } else if (choice instanceof TurnChoice.PlayCombo combo) {
            play(player, combo.cards(), true, () -> combo.combo().play(context, player, combo.cards()));
        }
    }

    /** The cards leave the hand and are discarded, the reactions get their chance, and then the effect happens. */
    private void play(PlayerId player, List<CardType> cards, boolean nopeable, Runnable effect) {
        PlayerState played = state.player(player);
        for (CardType card : cards) {
            played.hand().remove(card);
            state.discardPile().add(card);
        }
        String[] ids = idsOf(player, cards);
        channel.announce(GameEvent.of("card-played", ids));
        PlayedAction action = new PlayedAction(player, cards, nopeable);
        if (content.reactions().stream().anyMatch(reaction -> reaction.cancels(context, action))) {
            channel.announce(GameEvent.of("action-cancelled", ids));
        } else {
            effect.run();
        }
    }

    private static String[] idsOf(PlayerId player, List<CardType> cards) {
        List<String> ids = new ArrayList<>();
        ids.add(player.toString());
        cards.forEach(card -> ids.add(card.id()));
        return ids.toArray(String[]::new);
    }

    /** Draws the top card. A card with a draw effect is left to that effect, any other card goes into the hand. */
    private void draw(PlayerId player) {
        CardType card = state.deck().drawTop();
        Optional<DrawEffect> effect = content.definition(card).flatMap(CardDefinition::drawEffect);
        if (effect.isPresent()) {
            effect.get().onDrawn(context, player, card);
        } else {
            state.player(player).hand().add(card);
            channel.tell(player, GameEvent.of("card-drawn", card.id()));
            channel.announce(GameEvent.of("player-drew", player.toString()));
        }
        if (!context.turnOver()) {
            context.endTurn();
        }
    }

    // ---- moving on ----

    /**
     * Decides who plays next. After an attack it is the victim, with the turns the attack gives. A player who still has
     * turns left continues. Otherwise it is the next player still in the game, with one turn.
     */
    private void passOn() {
        if (state.isOver()) {
            announceWinner();
        } else if (context.hasPendingAttack()) {
            state.setCurrent(context.pendingVictim());
            state.setTurnsRemaining(context.pendingTurns());
            state.setUnderAttack(true);
        } else if (state.turnsRemaining() <= 0 || state.player(state.current()).eliminated()) {
            state.setCurrent(state.nextAlive(state.current()));
            state.setTurnsRemaining(1);
            state.setUnderAttack(false);
        }
    }

    private void announceWinner() {
        if (!winnerAnnounced) {
            winnerAnnounced = true;
            state.winner().ifPresent(winner -> channel.announce(GameEvent.of("game-won", winner.toString())));
        }
    }
}
