package kittens.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.GameEvent;
import kittens.api.Option;
import kittens.testing.MemoryConnection;
import kittens.testing.RecordingListener;
import kittens.testing.ScriptedPlayer;

/** A RemotePlayer and a RemoteClient joined by a connection: the game's own types arrive unchanged on the other side. */
@Timeout(30)
class RemotePairTest {

    @Test
    void decisionsAndEventsCrossTheNetworkWithoutChange() throws Exception {
        MemoryConnection[] line = MemoryConnection.pair();
        RemotePlayer atServer = new RemotePlayer(line[0]);
        ScriptedPlayer atClient = ScriptedPlayer.indexes(2, 0);
        RecordingListener heard = new RecordingListener();
        Thread client = new Thread(new RemoteClient(line[1], atClient, heard)::run);
        client.setDaemon(true);
        client.start();

        Decision turn = new Decision(DecisionKind.TURN_ACTION,
                List.of(Option.of("pass"), Option.of("play-card", "skip"), Option.of("play-combo", "taco-cat", "taco-cat")));
        Decision unknown = new Decision(new DecisionKind("alter-the-future"),
                List.of(Option.of("order", "attack", "skip", "shuffle")));

        atServer.onEvent(GameEvent.of("turn-started", "0", "1"));
        assertEquals(2, CompletableFuture.supplyAsync(() -> atServer.choose(turn)).get(5, TimeUnit.SECONDS));
        assertEquals(0, CompletableFuture.supplyAsync(() -> atServer.choose(unknown)).get(5, TimeUnit.SECONDS));
        atServer.onEvent(GameEvent.of("game-won", "0"));
        atServer.close();
        client.join(5000);

        assertEquals(List.of(turn, unknown), atClient.asked());
        assertEquals(List.of(GameEvent.of("turn-started", "0", "1"), GameEvent.of("game-won", "0")), heard.events());
    }
}
