package kittens.net;

import java.util.List;

/** One option of a question as it travels over the network: a code and its arguments, nothing more. */
public record WireOption(String code, List<String> args) {

    public WireOption {
        args = List.copyOf(args);
    }
}
