# Exploding Kittens: D7032E Home Exam 2026

Re-engineering of DevCat's single-file `ExplodingKittens.java`.

## Requirements

- JDK 17 or newer (`java -version`). Maven is **not** needed: the Maven wrapper downloads it on first use.

## Build, test, run

From the repository root (use `mvnw.cmd` instead of `./mvnw` on Windows):

| Task | Command |
|------|---------|
| Run all unit tests (JUnit 5, prints pass/fail per test) | `./mvnw test` |
| Build the runnable jar (also runs the tests) | `./mvnw package` |
| Play a local game against 1 to 4 computer players | `java -jar target/exploding-kittens.jar 1 2` (the first number is the number of humans, always 1 for now; the second is the number of bots) |

### Tests of the original code (Question 1)

```
./mvnw -f legacy/pom.xml test
```

Several of these tests are **expected to fail**: each failure shows a requirement the original code does not fulfil
(see the comments in `legacy/src/test/java/ExplodingKittensLegacyTest.java`). The build still ends green so the summary is readable;
add `-Dmaven.test.failure.ignore=false` to make failures fatal. They are not run by the root `./mvnw test`.

Test reports are written to `target/surefire-reports/`.

### Playing

Run `./mvnw package` first, which builds `target/exploding-kittens.jar`. The game is played in the console. Each question is
a numbered list: type the number and press Enter. When another player plays a card and you hold a Nope, you get a few seconds
to answer the Nope question. The base game (requirements 1 to 13) is playable against computer players. The expansions, playing
over the network and the menu are not built yet, so a game with more than one human (the original `server` and `client`
arguments) is refused with a message.

## Layout

```
pom.xml                  Maven build (Java 17, JUnit 5)
legacy/                  the original code (unchanged) and its tests, a separate Maven module for Question 1
src/main/java/kittens/
  app/                   Main and the game builder (composition root; creates and injects everything)
  api/                   interfaces and shared types; engine and plug-ins depend on these, not on each other
  engine/                the rules: setup, turns, Nope window, rule hooks (no I/O, no static state)
  model/                 plain game data: Card, Deck, Hand, ... (no I/O, no threads)
  expansion/base/        the base game as an expansion (always on); Imploding, Streaking and Barking are not written yet
  player/                computer players (RandomBot); the remote player will be added
  net/                   network transport, independent of the game
  ui/                    the human player at the console (ConsolePlayer); the menu will be added
src/test/java/kittens/   JUnit tests
```

The package structure follows the architecture design for Question 2.

## Hand-in

Canvas wants one archive containing a folder named after your LTU username, with the report PDF and the code.
Leave out `target/`.
