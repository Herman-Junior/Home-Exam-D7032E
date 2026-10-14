package kittens.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/** A {@link Connection} over a TCP socket. Several threads may send; each message is written whole. */
public final class SocketConnection implements Connection {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    @Override
    public synchronized void send(Message message) throws IOException {
        MessageCodec.write(out, message);
    }

    @Override
    public Message receive() throws IOException {
        return MessageCodec.read(in);
    }

    /** How long {@link #receive()} waits for a message before it gives up; 0 means for as long as it takes. */
    void setReadTimeout(int millis) throws IOException {
        socket.setSoTimeout(millis);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // already closed or broken: nothing more to do
        }
    }
}
