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
  Main.java              entry point
  core/                  game engine: setup, turns, rules (no I/O)
  cards/                 card types and effects
  expansion/             base game and expansion plug-ins
  player/                human, remote and bot players
  net/                   network transport, independent of the rules
  ui/                    menu and console presentation
src/test/java/kittens/   JUnit tests
```

The package split is a starting point for the design in Question 2 and may change.

## Hand-in

Canvas wants one archive containing a folder named after your LTU username, with the report PDF and the code.
Leave out `target/`.
