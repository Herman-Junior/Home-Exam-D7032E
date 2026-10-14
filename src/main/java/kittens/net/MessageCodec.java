package kittens.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns messages into bytes and back. A message is sent as a count and then that many strings, the first of which says
 * which message it is. Nothing is executed or looked up by name from what arrives, so a stranger cannot make the program
 * create objects of its choice, which is the weakness of sending Java objects over a socket. The size of what is
 * accepted is limited, and anything that does not fit is a {@link ProtocolException}.
 */
public final class MessageCodec {

    /** The most strings that one message may consist of. */
    static final int MAX_FIELDS = 1000;

    private MessageCodec() {
    }

    public static void write(DataOutputStream out, Message message) throws IOException {
        List<String> fields = toFields(message);
        out.writeInt(fields.size());
        for (String field : fields) {
            out.writeUTF(field);
        }
        out.flush();
    }

    public static Message read(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 1 || count > MAX_FIELDS) {
            throw new ProtocolException("a message cannot have " + count + " parts");
        }
        List<String> fields = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fields.add(in.readUTF());
        }
        return fromFields(fields);
    }

    // ---- message to strings ----

    static List<String> toFields(Message message) {
        List<String> fields = new ArrayList<>();
        if (message instanceof Message.Hello hello) {
            fields.add("hello");
            fields.add(Integer.toString(hello.version()));
        } else if (message instanceof Message.Welcome welcome) {
            fields.add("welcome");
            fields.add(Integer.toString(welcome.version()));
            fields.add(Integer.toString(welcome.playerNumber()));
        } else if (message instanceof Message.Rejected rejected) {
            fields.add("rejected");
            fields.add(rejected.reason());
        } else if (message instanceof Message.Ask ask) {
            fields.add("ask");
            fields.add(Integer.toString(ask.id()));
            fields.add(ask.kind());
            fields.add(Integer.toString(ask.options().size()));
            for (WireOption option : ask.options()) {
                fields.add(option.code());
                fields.add(Integer.toString(option.args().size()));
                fields.addAll(option.args());
            }
        } else if (message instanceof Message.Reply reply) {
            fields.add("reply");
            fields.add(Integer.toString(reply.id()));
            fields.add(Integer.toString(reply.choice()));
        } else if (message instanceof Message.Cancel cancel) {
            fields.add("cancel");
            fields.add(Integer.toString(cancel.id()));
        } else if (message instanceof Message.Notice notice) {
            fields.add("notice");
            fields.add(notice.code());
            fields.addAll(notice.args());
        } else if (message instanceof Message.Goodbye goodbye) {
            fields.add("goodbye");
            fields.add(goodbye.reason());
        } else {
            throw new IllegalArgumentException("unknown message " + message);
        }
        return fields;
    }

    // ---- strings to message ----

    static Message fromFields(List<String> fields) throws ProtocolException {
        Cursor cursor = new Cursor(fields);
        String type = cursor.text();
        Message message = switch (type) {
            case "hello" -> new Message.Hello(cursor.number());
            case "welcome" -> new Message.Welcome(cursor.number(), cursor.number());
            case "rejected" -> new Message.Rejected(cursor.text());
            case "ask" -> readAsk(cursor);
            case "reply" -> new Message.Reply(cursor.number(), cursor.number());
            case "cancel" -> new Message.Cancel(cursor.number());
            case "notice" -> new Message.Notice(cursor.text(), cursor.rest());
            case "goodbye" -> new Message.Goodbye(cursor.text());
            default -> throw new ProtocolException("unknown message type '" + type + "'");
        };
        cursor.requireEnd();
        return message;
    }

    private static Message.Ask readAsk(Cursor cursor) throws ProtocolException {
        int id = cursor.number();
        String kind = cursor.text();
        int optionCount = cursor.number();
        List<WireOption> options = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            String code = cursor.text();
            int argCount = cursor.number();
            List<String> args = new ArrayList<>();
            for (int j = 0; j < argCount; j++) {
                args.add(cursor.text());
            }
            options.add(new WireOption(code, args));
        }
        return new Message.Ask(id, kind, options);
    }

    /** Reads the strings of one message in order and complains if they run out or do not make sense. */
    private static final class Cursor {

        private final List<String> fields;
        private int position;

        Cursor(List<String> fields) {
            this.fields = fields;
        }

        String text() throws ProtocolException {
            if (position >= fields.size()) {
                throw new ProtocolException("the message ends too soon");
            }
            return fields.get(position++);
        }

        int number() throws ProtocolException {
            String text = text();
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new ProtocolException("'" + text + "' is not a number");
            }
        }

        List<String> rest() {
            List<String> rest = new ArrayList<>(fields.subList(position, fields.size()));
            position = fields.size();
            return rest;
        }

        void requireEnd() throws ProtocolException {
            if (position != fields.size()) {
                throw new ProtocolException("the message has parts left over");
            }
        }
    }
}
