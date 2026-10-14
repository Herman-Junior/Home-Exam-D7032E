package kittens.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import kittens.api.Decision;
import kittens.api.GameEvent;
import kittens.api.GameListener;
import kittens.api.Option;
import kittens.api.PlayerId;
import kittens.api.PlayerPort;

/**
 * The human at the keyboard: puts every decision on the screen as a numbered list and reads the answer, and prints
 * what happens in the game. The input and output are passed in, so the same class can be tested with a script.
 * <p>
 * The input is read by a thread of its own, and a question waits for a line with an interruptible wait. That matters for
 * the timed question, the Nope window: when the time is up the engine interrupts the wait and the question ends, and no
 * thread is left behind that would swallow the next line the player types. A line typed too late to count is thrown
 * away before the next question.
 */
public final class ConsolePlayer implements PlayerPort, GameListener {

    /** Marks the end of the input in the queue. Compared by identity, so no real line can be mistaken for it. */
    private static final String END_OF_INPUT = new String("end of input");

    private final PlayerId self;
    private final PrintStream out;
    private final BlockingQueue<String> lines = new ArrayBlockingQueue<>(64);
    private volatile boolean discardPending;
    private boolean inputEnded;

    public ConsolePlayer(PlayerId self, InputStream in, PrintStream out) {
        this.self = self;
        this.out = out;
        Thread reader = new Thread(() -> readLines(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))),
                "console-input");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLines(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lines.put(line);
            }
        } catch (IOException e) {
            // the input is gone: treat it like the end of the input
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            lines.put(END_OF_INPUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        ConsoleText.event(event, self).ifPresent(out::println);
    }

    /**
     * Puts the decision to the player and returns the answer. Options are numbered from 1, except a position in the deck,
     * which is entered as the number itself. Anything that is not a number is returned as an invalid answer, and the
     * engine reports it and asks again.
     */
    @Override
    public int choose(Decision decision) {
        if (discardPending) {
            lines.clear();
            discardPending = false;
        }
        out.println();
        out.println(ConsoleText.prompt(decision));
        boolean byNumber = ConsoleText.answeredByNumber(decision);
        if (!byNumber) {
            for (int i = 0; i < decision.options().size(); i++) {
                Option option = decision.options().get(i);
                out.println("  " + (i + 1) + ") " + ConsoleText.option(decision.kind(), option));
            }
        }
        out.print("> ");
        out.flush();
        String line = nextLine();
        try {
            int number = Integer.parseInt(line.trim());
            return byNumber ? number : number - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String nextLine() {
        if (inputEnded) {
            throw new IllegalStateException("the input has ended");
        }
        try {
            String line = lines.take();
            if (line == END_OF_INPUT) {
                inputEnded = true;
                throw new IllegalStateException("the input has ended");
            }
            return line;
        } catch (InterruptedException e) {
            discardPending = true;
            Thread.currentThread().interrupt();
            throw new IllegalStateException("the question was interrupted", e);
        }
    }
}
