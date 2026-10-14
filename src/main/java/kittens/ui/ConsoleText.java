package kittens.ui;

import java.util.List;
import java.util.Optional;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.api.PlayerId;

/**
 * The words of the console game: what an event says, and how a decision and its options are put to the player. The
 * engine sends codes and arguments, and this is the only place that turns them into text. It does not know the
 * expansions' classes. A kind, code or event that it has no words for is shown generically, so the game still works
 * with content that was added later.
 */
final class ConsoleText {

    /** Kinds of decision whose answer is a number in the range itself, without a list of options. */
    private static final String POSITION_KIND = DecisionKind.DEFUSE_POSITION.id();

    private ConsoleText() {
    }

    /** A card id as words: {@code "taco-cat"} becomes {@code "taco cat"}. */
    static String name(String id) {
        return id.replace('-', ' ');
    }

    private static String names(List<String> ids) {
        return ids.isEmpty() ? "(none)" : String.join(", ", ids.stream().map(ConsoleText::name).toList());
    }

    /** Who a player id refers to, as the start of a sentence: "You" or "Player 2". */
    private static String who(String id, PlayerId self) {
        return id.equals(self.toString()) ? "You" : "Player " + id;
    }

    private static String whoInSentence(String id, PlayerId self) {
        return id.equals(self.toString()) ? "you" : "player " + id;
    }

    /** What an event says, from the point of view of {@code self}. Empty if there is nothing to say to this player. */
    static Optional<String> event(GameEvent event, PlayerId self) {
        List<String> args = event.args();
        return Optional.ofNullable(switch (event.code()) {
            case "game-started" -> "Game started with " + args.get(0) + " players. First to play: "
                    + whoInSentence(args.get(1), self) + ".";
            case "turn-started" -> "\n=== Turn of " + whoInSentence(args.get(0), self) + " (" + args.get(1)
                    + (args.get(1).equals("1") ? " turn" : " turns") + " to take) ===";
            case "hand" -> "Your hand: " + names(args);
            case "card-drawn" -> "You drew: " + name(args.get(0));
            case "player-drew" -> args.get(0).equals(self.toString()) ? null : "Player " + args.get(0) + " drew a card.";
            case "kitten-drawn" -> who(args.get(0), self) + " drew an Exploding Kitten!";
            case "card-played" -> who(args.get(0), self) + " played: "
                    + String.join(" + ", args.subList(1, args.size()).stream().map(ConsoleText::name).toList());
            case "action-cancelled" -> "That was stopped!";
            case "nope-played" -> who(args.get(0), self) + " played a Nope"
                    + (args.get(1).equals("yup") ? " on the Nope: it goes through again (Yup)!" : "!");
            case "player-eliminated" -> who(args.get(0), self) + " exploded! Out of the game.";
            case "defused" -> who(args.get(0), self) + " defused an Exploding Kitten.";
            case "top-cards" -> "The top cards of the deck: " + names(args);
            case "card-received" -> "You received " + name(args.get(0)) + " from player " + args.get(1) + ".";
            case "card-taken" -> "Player " + args.get(1) + " took " + name(args.get(0)) + " from you.";
            case "card-not-found" -> "Player " + args.get(1) + " has no " + name(args.get(0)) + ".";
            case "favor-failed" -> "Player " + args.get(1) + " had no card to give.";
            case "invalid-answer" -> "That is not a valid choice, try again (" + args.get(0) + " of " + args.get(1) + ").";
            case "answer-defaulted" -> who(args.get(0), self) + " gave no valid answer, so the first option was used.";
            case "game-won" -> who(args.get(0), self) + " won the game!";
            default -> event.code() + (args.isEmpty() ? "" : ": " + String.join(" ", args));
        });
    }

    /** The question to put to the player. */
    static String prompt(Decision decision) {
        return switch (decision.kind().id()) {
            case "turn-action" -> "What do you want to do?";
            case "nope" -> "Play a Nope on that? You only have a few seconds.";
            case "give-card" -> "Which card do you give?";
            case "defuse-position" -> "Where do you put the Exploding Kitten back in the deck? Enter a number from 0 (the top) to "
                    + (decision.options().size() - 1) + " (the bottom).";
            case "choose-target" -> "Choose a player:";
            case "name-card" -> "Name the card you want:";
            default -> "Choose (" + decision.kind().id() + "):";
        };
    }

    /** Whether the player answers with the number itself, from 0, and the options are not listed. */
    static boolean answeredByNumber(Decision decision) {
        return decision.kind().id().equals(POSITION_KIND);
    }

    /** One option as the player reads it. */
    static String option(DecisionKind kind, Option option) {
        List<String> args = option.args();
        return switch (kind.id() + "/" + option.code()) {
            case "turn-action/pass" -> "Pass (draw a card and end your turn)";
            case "turn-action/play-card" -> "Play " + names(args);
            case "turn-action/play-combo" -> "Play together: " + String.join(" + ", args.stream().map(ConsoleText::name).toList());
            case "nope/nope" -> "Play a Nope";
            case "nope/pass" -> "Let it happen";
            case "choose-target/target" -> "Player " + args.get(0);
            case "give-card/give", "name-card/name" -> names(args);
            default -> option.code() + (args.isEmpty() ? "" : " " + String.join(" ", args));
        };
    }
}
