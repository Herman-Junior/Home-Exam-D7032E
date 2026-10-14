package kittens.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

class MessageCodecTest {

    private static Message roundTrip(Message message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessageCodec.write(new DataOutputStream(bytes), message);
        return MessageCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    private static Message readFields(String... fields) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(fields.length);
        for (String field : fields) {
            out.writeUTF(field);
        }
        return MessageCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }

    @Test
    void everyKindOfMessageSurvivesTheTripThroughBytes() throws IOException {
        List<Message> messages = List.of(
                new Message.Hello(1),
                new Message.Welcome(1, 3),
                new Message.Rejected("the game is full"),
                new Message.Ask(42, "turn-action", List.of(new WireOption("pass", List.of()),
                        new WireOption("play-combo", List.of("taco-cat", "taco-cat")))),
                new Message.Ask(7, "empty", List.of()),
                new Message.Reply(42, 1),
                new Message.Reply(5, -1),
                new Message.Cancel(42),
                new Message.Notice("card-played", List.of("0", "skip")),
                new Message.Notice("game-won", List.of()),
                new Message.Goodbye("the game is over"));
        for (Message message : messages) {
            assertEquals(message, roundTrip(message));
        }
    }

    @Test
    void textWithOtherCharactersAndSpacesIsKeptAsItIs() throws IOException {
        Message message = new Message.Notice("sagt", List.of("Älg på väg", "", "a b\tc\nd"));
        assertEquals(message, roundTrip(message));
    }

    @Test
    void aMessageTypeThatDoesNotExistIsRefused() {
        assertThrows(ProtocolException.class, () -> readFields("launch-missiles", "1"));
    }

    @Test
    void aNumberThatIsNotANumberIsRefused() {
        assertThrows(ProtocolException.class, () -> readFields("reply", "one", "2"));
    }

    @Test
    void aMessageThatEndsTooSoonOrHasPartsLeftOverIsRefused() {
        assertThrows(ProtocolException.class, () -> readFields("reply", "1"));
        assertThrows(ProtocolException.class, () -> readFields("cancel", "1", "extra"));
        assertThrows(ProtocolException.class, () -> readFields("ask", "1", "kind", "3", "pass", "0"));
    }

    @Test
    void anAbsurdNumberOfOptionsOrPartsIsRefusedWithoutAllocatingMemoryForThem() {
        assertThrows(ProtocolException.class, () -> readFields("ask", "1", "kind", "2000000000"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertThrows(ProtocolException.class, () -> {
            new DataOutputStream(bytes).writeInt(Integer.MAX_VALUE);
            MessageCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        });
    }

    @Test
    void aMessageWithNoPartsOrANegativeCountIsRefused() {
        for (int count : new int[] {0, -1}) {
            assertThrows(ProtocolException.class, () -> {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                new DataOutputStream(bytes).writeInt(count);
                MessageCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            });
        }
    }

    @Test
    void aStreamThatEndsInTheMiddleOfAMessageIsAnEndOfStreamNotAHang() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessageCodec.write(new DataOutputStream(bytes), new Message.Notice("a", List.of("b", "c")));
        byte[] whole = bytes.toByteArray();
        byte[] cut = java.util.Arrays.copyOf(whole, whole.length - 2);
        assertThrows(EOFException.class, () -> MessageCodec.read(new DataInputStream(new ByteArrayInputStream(cut))));
    }

    @Test
    void textThatLooksLikeAnotherProtocolIsRefused() {
        byte[] http = "GET / HTTP/1.1\r\nHost: x\r\n\r\n".getBytes();
        assertThrows(ProtocolException.class, () -> MessageCodec.read(new DataInputStream(new ByteArrayInputStream(http))));
    }

    @Test
    void severalMessagesInARowAreReadOneByOne() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        MessageCodec.write(out, new Message.Cancel(1));
        MessageCodec.write(out, new Message.Reply(2, 3));
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(new Message.Cancel(1), MessageCodec.read(in));
        assertEquals(new Message.Reply(2, 3), MessageCodec.read(in));
    }
}
