/**
 * Network transport: typed {@link kittens.net.Message}s, a {@link kittens.net.Connection} that carries them, a
 * {@link kittens.net.Server} that lets clients in and a {@link kittens.net.Client} that joins. It uses only strings and
 * numbers and knows nothing about the game, so the transport and the rules can change independently. The adapters that
 * connect it to the game are in {@code player}.
 */
package kittens.net;
