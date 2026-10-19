# Exploding Kittens: D7032E Home Exam 2026

Re-engineering of DevCat's single-file `ExplodingKittens.java`.

## Requirements

- JDK 17 or newer (check with `java -version`). The code is compiled for Java 17 and also runs on newer JDKs.
- Maven is **not** needed to be installed: the Maven wrapper (`mvnw`) downloads it on first use, so the first run needs an
  internet connection. Later runs work offline.
- Use `./mvnw` on Linux and macOS, and `mvnw.cmd` on Windows.

## Compile, test, run

All commands are given from the repository root.

| Task | Command |
|------|---------|
| Compile only | `./mvnw compile` |
| Run all unit tests (JUnit 5, prints the result per test class) | `./mvnw test` |
| Build the runnable jar (compiles and runs the tests) | `./mvnw package` |
| Build the jar without the tests | `./mvnw package -DskipTests` |
| Start from a clean state | `./mvnw clean package` |
| Play a local game against computer players | `java -jar target/exploding-kittens.jar 1 2` (1 human, 2 bots) |
| Host a game for other people | `java -jar target/exploding-kittens.jar 3 1` (3 humans, 1 bot: you and two who join) |
| Join a hosted game | `java -jar target/exploding-kittens.jar 192.168.1.20` (the host's address; add `:port` for another port) |

`./mvnw package` creates `target/exploding-kittens.jar`. A successful run of `./mvnw test` ends with
`Tests run: 244, Failures: 0, Errors: 0` in total and `BUILD SUCCESS`. The report of each test class is written to
`target/surefire-reports/`.

### Tests of the original code (Question 1)

```
./mvnw -f legacy/pom.xml test
```

These tests are run against the original, unchanged `ExplodingKittens.java`. 7 of the 9 tests are **expected to fail**: each
failure shows a requirement that the original code does not fulfil (see the comments in
`legacy/src/test/java/ExplodingKittensLegacyTest.java`). The build still ends green so that the summary can be read; add
`-Dmaven.test.failure.ignore=false` to make a failing test stop the build. These tests are not part of the root
`./mvnw test`.

### Troubleshooting

- `release version 17 not supported` or `invalid target release`: the JDK is older than 17. Install JDK 17 or newer and make
  sure `JAVA_HOME` points to it.
- `mvnw: Permission denied` (Linux and macOS): run `chmod +x mvnw`.
- The first build fails to download Maven or the plug-ins: check the internet connection or the proxy settings.
- `Address already in use` when hosting: another program uses port 2048 (the host always listens on this port). Close that program and start again.
- A client cannot connect: check the host's IP address and that the host's firewall allows TCP port 2048.

### Playing

Run `./mvnw package` first, which builds `target/exploding-kittens.jar`. The game is played in the console. Each question is
a numbered list: type the number and press Enter. When another player plays a card and you hold a Nope, you get a few seconds
to answer the Nope question.

The command line is the one of the original game. The first number is the number of **humans**, including the one at the
host's own console, and the second is the number of bots. A game needs 2 to 5 players in all.

- **Local**: with 1 human, no network is used.
- **Hosting**: with more than 1 human, the host waits on TCP port 2048 until the other humans have joined, and then the game
  starts. Players are numbered in this order: the host (0), the bots, and then the clients in the order they connected.
  The host's firewall must allow port 2048.
- **Joining**: start the jar with the host's IP address. If a player disconnects, the game goes on and that player passes
  from then on.

The base game (requirements 1 to 13) is playable. The expansions and the menu are not built yet.

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
  player/                computer players (RandomBot) and the players at other computers (RemotePlayer, RemoteClient)
  net/                   network transport: typed messages, connection, server, client (knows nothing of the game)
  ui/                    the human player at the console (ConsolePlayer); the menu will be added
src/test/java/kittens/   JUnit tests
```

The package structure follows the architecture design for Question 2.

## Hand-in

Canvas wants one archive containing a folder named after your LTU username, with the report PDF and the code.
Leave out `target/`.
