package kittens.player;

import java.util.ArrayList;
import java.util.List;

import kittens.api.Decision;
import kittens.api.DecisionKind;
import kittens.api.PlayerPort;
import kittens.api.RandomSource;

/**
 * A simple computer player. On its turn it mostly passes, and now and then plays a random card. It plays a Nope half of
 * the time it can, and every other decision it answers at random. It is only as clever as it needs to be to make a
 * local game playable; a better bot is another {@link PlayerPort} and changes nothing else. All its chance comes from
 * the {@link RandomSource}, so a test can fix what it does.
 */
public final class RandomBot implements PlayerPort {

    /** The chance, in percent, that the bot passes on its turn without playing a card first. */
    static final int PASS_CHANCE = 70;

    private final RandomSource random;

    public RandomBot(RandomSource random) {
        this.random = random;
    }

    @Override
    public int choose(Decision decision) {
        if (decision.kind().equals(DecisionKind.TURN_ACTION)) {
            return chooseOnTurn(decision);
        }
        if (decision.kind().equals(DecisionKind.NOPE)) {
            int wanted = random.nextInt(2) == 0 ? indexOf(decision, "nope") : indexOf(decision, "pass");
            return wanted >= 0 ? wanted : 0;
        }
        return random.nextInt(decision.options().size());
    }

    private int chooseOnTurn(Decision decision) {
        int pass = Math.max(indexOf(decision, "pass"), 0);
        List<Integer> plays = new ArrayList<>();
        for (int i = 0; i < decision.options().size(); i++) {
            if (i != pass) {
                plays.add(i);
            }
        }
        boolean passNow = random.nextInt(100) < PASS_CHANCE || plays.isEmpty();
        return passNow ? pass : plays.get(random.nextInt(plays.size()));
    }

    private static int indexOf(Decision decision, String code) {
        for (int i = 0; i < decision.options().size(); i++) {
            if (decision.options().get(i).code().equals(code)) {
                return i;
            }
        }
        return -1;
    }
}
