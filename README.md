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
| Start a server (players, bots) | `java -jar target/exploding-kittens.jar 2 0` |
| Start a client (server IP) | `java -jar target/exploding-kittens.jar 127.0.0.1` |

### Tests of the original code (Question 1)

```
./mvnw -f legacy/pom.xml test
```

Several of these tests are **expected to fail**: each failure shows a requirement the original code does not fulfil
(see the comments in `legacy/src/test/java/ExplodingKittensLegacyTest.java`). The build still ends green so the summary is readable;
add `-Dmaven.test.failure.ignore=false` to make failures fatal. They are not run by the root `./mvnw test`.

Start the server first. It waits for the online clients before the game starts.
Test reports are written to `target/surefire-reports/`.

> The game logic is not implemented yet: `Main` currently only prints the usage text.

## Layout

```
pom.xml                  Maven build (Java 17, JUnit 5)
legacy/                  the original code (unchanged) and its tests, a separate Maven module for Question 1
src/main/java/kittens/
  app/                   Main and the game builder (composition root; creates and injects everything)
  api/                   interfaces and shared types; engine and plug-ins depend on these, not on each other
  engine/                the rules: setup, turns, Nope window, rule hooks (no I/O, no static state)
  model/                 plain game data: Card, Deck, Hand, ... (no I/O, no threads)
  expansion/             base game (always on) and the Imploding, Streaking and Barking expansions
  player/                bots and the remote player
  net/                   network transport, independent of the game
  ui/                    menu, console output and the human player at the keyboard
src/test/java/kittens/   JUnit tests
```

The package structure follows the architecture design for Question 2.

## Hand-in

Canvas wants one archive containing a folder named after your LTU username, with the report PDF and the code.
Leave out `target/`.
