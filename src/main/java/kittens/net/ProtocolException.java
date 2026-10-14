package kittens.net;

import java.io.IOException;

/** The other side sent something that is not part of the protocol, or the greeting was refused. */
public class ProtocolException extends IOException {

    public ProtocolException(String message) {
        super(message);
    }
}
