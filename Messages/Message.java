package Messages;

import java.io.Serializable;

import static Utilities.Constants.CRLF;

/**
 * Message class.
 */
public class Message implements Serializable {

    //TODO: Fix this. CRLF after header??? IT IS DIFFERENT TO READ! read line doesn't work
    public static String createMessage(String... headerFields){
        return (String.join(" ", headerFields) + " " + CRLF + CRLF);
    }

    public static String[] parseMessage(String message){
        return message.split(" ");
    }
}