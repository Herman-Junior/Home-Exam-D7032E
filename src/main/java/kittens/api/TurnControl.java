package kittens.api;

import java.util.List;

/** What a card effect may do with the order of play. */
public interface TurnControl {

    PlayerId currentPlayer();

    /** The player who is next in turn order and still in the game. */
    PlayerId nextPlayer();

    /** All other players still in the game, starting with the next player. */
    List<PlayerId> otherPlayers();

    /** Ends the current turn without drawing a card. When the player has several turns to take, only one ends. */
    void endTurn();

    /**
     * Ends the current player's turns without drawing and makes {@code victim} take {@code turns} turns in a row.
     * If the current player was attacked and still owes turns, the victim takes those as well, so a chain of
     * attacks gives 2, 4, 6 turns.
     */
    void attack(PlayerId victim, int turns);

    /** Takes a player out of the game: the hand is discarded and the player takes no more turns. */
    void eliminate(PlayerId player);
}
