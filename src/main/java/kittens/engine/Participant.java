package kittens.engine;

import kittens.api.GameEvent;
import kittens.api.GameListener;
import kittens.api.PlayerPort;

/**
 * Someone taking part in a game: the {@link PlayerPort} that makes the decisions and the {@link GameListener} that
 * hears what happens. The engine numbers the participants from 0 in the order it gets them. A participant that only
 * decides, such as a bot, can leave the listener out.
 */
public record Participant(PlayerPort port, GameListener listener) {

    public Participant {
        if (port == null || listener == null) {
            throw new IllegalArgumentException("a participant needs a port and a listener");
        }
    }

    /** A participant that decides but does not listen. */
    public static Participant of(PlayerPort port) {
        return new Participant(port, (GameEvent event) -> { });
    }
}
