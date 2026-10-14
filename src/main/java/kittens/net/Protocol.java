package kittens.net;

/** The agreement between a server and a client about how they talk. */
public final class Protocol {

    /** Changes whenever the messages change in a way that an older program cannot understand. */
    public static final int VERSION = 1;

    private Protocol() {
    }
}
