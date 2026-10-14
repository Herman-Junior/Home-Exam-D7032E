package kittens.net;

import java.util.List;

/**
 * Everything that can be said between a server and a client. Every message is a type of its own with named fields, so
 * a program reads {@code message instanceof Ask} and never searches a text for a word. The messages are plain strings
 * and numbers: this package knows nothing about the game, so the game and the network can change independently.
 */
public sealed interface Message {

    /** Client to server, first: "I speak this version." */
    record Hello(int version) implements Message {
    }

    /** Server to client: "Welcome. You are player number {@code playerNumber}." */
    record Welcome(int version, int playerNumber) implements Message {
    }

    /** Server to client instead of a welcome: the client may not join, and why. */
    record Rejected(String reason) implements Message {
    }

    /** Server to client: a question. The {@code id} ties the reply to it. */
    record Ask(int id, String kind, List<WireOption> options) implements Message {

        public Ask {
            options = List.copyOf(options);
        }
    }

    /** Client to server: the answer to the question with this id, as the number of the chosen option. */
    record Reply(int id, int choice) implements Message {
    }

    /** Server to client: the question with this id is no longer wanted, for example because the time ran out. */
    record Cancel(int id) implements Message {
    }

    /** Server to client: something happened that the player should hear about. */
    record Notice(String code, List<String> args) implements Message {

        public Notice {
            args = List.copyOf(args);
        }
    }

    /** Either way: the conversation is over. */
    record Goodbye(String reason) implements Message {
    }
}
