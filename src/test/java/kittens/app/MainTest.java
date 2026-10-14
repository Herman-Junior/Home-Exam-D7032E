package kittens.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import kittens.engine.JavaRandomSource;

class MainTest {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private int run(InputStream in, String... args) {
        return Main.run(args, in, out, new JavaRandomSource(new Random(7)));
    }

    /** A human who types 1 whenever asked: pass on the turn, the first option everywhere else. */
    private static InputStream alwaysOne() {
        return new InputStream() {
            private int position;

            @Override
            public int read() {
                return "1\n".charAt(position++ % 2);
            }

            @Override
            public int available() {
                return 2;
            }
        };
    }

    @Test
    void usageDescribesServerClientAndLocalSyntax() {
        String usage = Main.usage();
        assertTrue(usage.contains("Server syntax"));
        assertTrue(usage.contains("Client syntax"));
        assertTrue(usage.contains("local game"));
    }

    @Test
    void aCommandLineThatIsNotUnderstoodPrintsTheUsage() {
        assertEquals(2, run(new ByteArrayInputStream(new byte[0])));
        assertTrue(output().contains("Server syntax"));
    }

    @Test
    void wordsInsteadOfNumbersPrintTheUsage() {
        assertEquals(2, run(new ByteArrayInputStream(new byte[0]), "many", "few"));
        assertTrue(output().contains("Server syntax"));
    }

    @Test
    void networkPlayIsNotAvailableYet() {
        assertEquals(2, run(new ByteArrayInputStream(new byte[0]), "2", "1"));
        assertTrue(output().contains("not available yet"));
        assertEquals(2, run(new ByteArrayInputStream(new byte[0]), "127.0.0.1"));
    }

    @ParameterizedTest(name = "{0} bots")
    @ValueSource(strings = {"0", "5", "9"})
    void aNumberOfPlayersThatIsNotAllowedIsReportedAndNoGameStarts(String bots) { // R1
        assertEquals(1, run(new ByteArrayInputStream(new byte[0]), "1", bots));
        assertTrue(output().contains("Cannot start the game: a game needs between 2 and 5 players"), output());
    }

    @ParameterizedTest(name = "{0} bots")
    @ValueSource(strings = {"1", "2", "3", "4"})
    void aLocalGameIsPlayedToTheEnd(String bots) {
        assertEquals(0, run(alwaysOne(), "1", bots));
        assertTrue(output().contains("won the game!"), output());
        assertTrue(output().contains("Game started with " + (Integer.parseInt(bots) + 1) + " players"));
    }
}
