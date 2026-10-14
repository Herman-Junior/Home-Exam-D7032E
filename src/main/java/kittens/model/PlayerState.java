package kittens.model;

import kittens.api.PlayerId;

/** What the game keeps about one player: the hand and whether the player is out. */
public final class PlayerState {

    private final PlayerId id;
    private final Hand hand = new Hand();
    private boolean eliminated;

    public PlayerState(PlayerId id) {
        this.id = id;
    }

    public PlayerId id() {
        return id;
    }

    public Hand hand() {
        return hand;
    }

    public boolean eliminated() {
        return eliminated;
    }

    public void eliminate() {
        eliminated = true;
    }
}
