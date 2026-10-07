package kittens;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Placeholder that proves the JUnit 5 setup runs. Replace with real tests. */
class MainTest {

    @Test
    void usageDescribesServerAndClientSyntax() {
        String usage = Main.usage();
        assertTrue(usage.contains("Server syntax"));
        assertTrue(usage.contains("Client syntax"));
    }
}
