package Messages;

import java.io.ByteArrayInputStream;

import static Utilities.Constants.CRLF;

/**
 * Message builder class.
 */
public class MessageUtils {

    public static String createMessage(String... headerFields){
        return (String.join(" ", headerFields) + " " + CRLF + CRLF);
    }

    public void parseHeader(ByteArrayInputStream byteArrayInputStream){

    }
}