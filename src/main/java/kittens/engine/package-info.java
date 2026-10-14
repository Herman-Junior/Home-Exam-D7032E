/**
 * The rules: setting up a game, the turn loop (choosing, playing cards, drawing, who goes next, attacks, elimination,
 * the winner) and the connection to the players, including the timed questions. No I/O, no sockets and no static state.
 * It knows no card and no expansion: it works through the interfaces in {@code api}, with game data from {@code model}.
 */
package kittens.engine;
