package kittens.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kittens.api.GameEvent;
import kittens.api.GameListener;

/** A listener for tests that keeps every event it receives. */
public final class RecordingListener implements GameListener {

    private final List<GameEvent> events = new ArrayList<>();

    @Override
    public synchronized void onEvent(GameEvent event) {
        events.add(event);
    }

    public synchronized List<GameEvent> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
