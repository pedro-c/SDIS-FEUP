package Messages;

import static Utilities.Constants.CRLF;

/**
 * Message builder class.
 */
public class MessageBuilder {

    public static String createMessage(String... headerFields){
        return (String.join(" ", headerFields) + " " + CRLF + CRLF);
    }
}