import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the ORIGINAL, unmodified ExplodingKittens.java (Question 1).
 *
 * Each test checks one requirement from the exam sheet. A test that FAILS shows that the original
 * code does not fulfil the requirement. The tests are in the default package because the original
 * class is, and a class in a named package cannot import it.
 *
 * How the original is driven without changing it:
 *  - Players, hands, the static deck and numberOfTurnsToTake are public, so a game state is built by hand.
 *  - The game reads System.in, so a scripted input is installed. When the script runs out it blocks,
 *    which tells the test that the game state is stable and can be inspected.
 *  - game() and initGame() run on a daemon thread, because they never return while a game is running.
 *  - secondsToInterruptWithNope is set to 0 so each played card waits 0.5 s instead of 5.5 s.
 */
class ExplodingKittensLegacyTest {

    private static final ExplodingKittens.Card DEFUSE = ExplodingKittens.Card.Defuse;
    private static final ExplodingKittens.Card KITTEN = ExplodingKittens.Card.ExplodingKitten;
    private static final ExplodingKittens.Card SKIP = ExplodingKittens.Card.Skip;
    private static final ExplodingKittens.Card TACO = ExplodingKittens.Card.TacoCat;

    private InputStream originalIn;
    private PrintStream originalOut;

    @BeforeEach
    void resetStaticStateAndSilenceOutput() {
        originalIn = System.in;
        originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        // The original keeps its game state in static fields, so every test must start from scratch.
        ExplodingKittens.deck.clear();
        ExplodingKittens.discard.clear();
        ExplodingKittens.numberOfTurnsToTake = 1;
    }

    @AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // ------------------------------------------------------------------------------------------
    // Requirement 1: there can be between 2 and 5 players
    // ------------------------------------------------------------------------------------------

    /** server() is what initGame() uses to create the players. With 1 human + 6 bots it must refuse. */
    @Test
    void r1_moreThanFivePlayersIsRejected() throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]); // only prints the usage text
        ek.server(1, 6);
        assertTrue(ek.players.size() <= 5, "a game with " + ek.players.size() + " players was accepted");
    }

    @Test
    void r1_fewerThanTwoPlayersIsRejected() throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]);
        ek.server(1, 0);
        assertTrue(ek.players.size() >= 2, "a game with " + ek.players.size() + " player was accepted");
    }

    // ------------------------------------------------------------------------------------------
    // Requirement 3a: 2 Defuse cards in the deck for 2-4 players, 1 for 5 players
    // ------------------------------------------------------------------------------------------

    /**
     * The deck is built inside initGame(), which goes on to play the game, so the deck cannot be
     * inspected before the hands are dealt. Instead the total number of Defuse cards in the deck and in
     * all hands is counted once the game waits for its first input. By requirement 2 every player holds
     * exactly 1 extra Defuse, so the total must be (cards in deck) + (number of players).
     */
    @ParameterizedTest(name = "{0} players")
    @ValueSource(ints = {2, 3, 4, 5})
    void r3a_defuseCardsInDeck(int players) throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]);
        ek.secondsToInterruptWithNope = 0;

        runUntilSettled(() -> ek.initGame(1, players - 1)); // 1 human + (players-1) bots, no sockets

        int totalDefuse = Collections.frequency(ExplodingKittens.deck, DEFUSE);
        for (ExplodingKittens.Player p : ek.players) {
            totalDefuse += Collections.frequency(p.hand, DEFUSE);
        }
        int expectedInDeck = (players == 5) ? 1 : 2;
        assertEquals(expectedInDeck + players, totalDefuse,
                "total Defuse cards (deck + hands) with " + players + " players");
    }

    // ------------------------------------------------------------------------------------------
    // Requirement 10b: an exploded player may not take additional turns
    // ------------------------------------------------------------------------------------------

    /**
     * Player 1 has to take 2 turns (as after an Attack). The first card drawn is an ExplodingKitten and
     * player 1 has no Defuse, so player 1 explodes. Player 1 must then be out of the game. The second
     * "Pass" in the script belongs to the next player.
     */
    @Test
    void r10b_explodedPlayerTakesNoAdditionalTurns() throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]);
        ek.secondsToInterruptWithNope = 0;
        addPlayer(ek, TACO);
        ExplodingKittens.Player exploding = addPlayer(ek, TACO);
        addPlayer(ek, TACO);
        ExplodingKittens.deck.addAll(Arrays.asList(KITTEN, TACO, TACO, TACO, TACO));
        ExplodingKittens.numberOfTurnsToTake = 2;

        runUntilSettled(() -> ek.game(1), "Pass", "Pass");

        assertTrue(exploding.exploded, "player 1 should have exploded");
        assertTrue(exploding.hand.isEmpty(),
                "player 1 exploded but took another turn and drew " + exploding.hand);
    }

    // ------------------------------------------------------------------------------------------
    // Requirement 11d: Skip ends the turn without drawing (only 1 turn if taking multiple turns)
    // ------------------------------------------------------------------------------------------

    /**
     * Player 0 must take 2 turns: Pass on the first and Skip on the last. When the turn passes to the
     * next player the counter of turns to take must be back to 1.
     */
    @Test
    void r11d_skipOnLastOfSeveralTurnsDoesNotCarryOver() throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]);
        ek.secondsToInterruptWithNope = 0;
        addPlayer(ek, SKIP, TACO);
        addPlayer(ek, TACO);
        ExplodingKittens.deck.addAll(Arrays.asList(TACO, TACO, TACO, TACO));
        ExplodingKittens.numberOfTurnsToTake = 2;

        runUntilSettled(() -> ek.game(0), "Pass", "Skip");

        assertEquals(1, ExplodingKittens.numberOfTurnsToTake,
                "turns the next player has to take after Skip");
    }

    // ------------------------------------------------------------------------------------------
    // Requirement 11f: two of a kind steals a random card from another player
    // ------------------------------------------------------------------------------------------

    /** The target holds a single card, so that card is the only possible one to steal. */
    @Test
    void r11f_twoOfAKindCanStealFromPlayerWithOneCard() throws Exception {
        ExplodingKittens ek = new ExplodingKittens(new String[0]);
        ek.secondsToInterruptWithNope = 0;
        ExplodingKittens.Player thief = addPlayer(ek, TACO, TACO);
        ExplodingKittens.Player victim = addPlayer(ek, SKIP);
        ExplodingKittens.deck.addAll(Arrays.asList(TACO, TACO, TACO, TACO));

        Throwable crash = runUntilSettled(() -> ek.game(0), "Two TacoCat 1");

        assertNull(crash, "the game crashed while stealing a card: " + crash);
        assertTrue(victim.hand.isEmpty(), "the victim's only card should have been stolen");
        assertEquals(Arrays.asList(SKIP), thief.hand);
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private ExplodingKittens.Player addPlayer(ExplodingKittens ek, ExplodingKittens.Card... hand) {
        ExplodingKittens.Player player = ek.new Player(ek.players.size(), false, null, null, null);
        player.hand.addAll(Arrays.asList(hand));
        ek.players.add(player);
        return player;
    }

    private interface Action {
        void run() throws Exception;
    }

    /**
     * Runs the action on a daemon thread with the given lines as System.in, and returns when the action
     * has ended or is waiting for input the script does not have. Returns the exception the action
     * ended with, or null.
     */
    private Throwable runUntilSettled(Action action, String... inputLines) throws InterruptedException {
        CountDownLatch settled = new CountDownLatch(1);
        System.setIn(new ScriptedInput(settled, inputLines));
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread game = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                settled.countDown();
            }
        });
        game.setDaemon(true);
        game.start();
        assertTrue(settled.await(20, TimeUnit.SECONDS), "the game neither ended nor asked for more input");
        return error.get();
    }

    /**
     * System.in that hands out ONE scripted line per read, like a terminal. This matters because the
     * original creates a new Scanner for every read; a Scanner buffers ahead and would swallow the
     * lines meant for the next read. When the script is used up, read() signals {@code settled} and
     * blocks forever (the game thread is a daemon and is left behind).
     */
    private static final class ScriptedInput extends InputStream {
        private final Deque<byte[]> lines = new ArrayDeque<>();
        private final CountDownLatch settled;
        private byte[] current = new byte[0];
        private int position = 0;

        ScriptedInput(CountDownLatch settled, String... script) {
            this.settled = settled;
            for (String line : script) {
                lines.add((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            if (position == current.length) {
                if (lines.isEmpty()) {
                    settled.countDown();
                    try {
                        new CountDownLatch(1).await(); // never returns
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
                current = lines.poll();
                position = 0;
            }
            int n = Math.min(length, current.length - position);
            System.arraycopy(current, position, buffer, offset, n);
            position += n;
            return n;
        }
    }
}
