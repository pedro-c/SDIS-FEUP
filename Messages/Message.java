package Messages;

import java.io.*;

import static Utilities.Constants.CRLF;

/**
 * Message class.
 */
public class Message implements Serializable {

    private String messageType;
    private String senderId;
    private String body;


    public Message(byte[] messageType, byte[] senderId, String ... body){
        this.messageType = new String(messageType);
        this.senderId = new String(senderId);
        this.body = String.join(" ", body);

        createMessage(this.messageType,this.senderId,this.body);
    }

    //MessageType SenderId CRLF Body CRLF CRLF
    public byte[] createMessage(String messageType, String senderId, String body){
        return (String.join(" ", messageType) + " "
                + String.join(" ", senderId) + " "
                + CRLF
                + String.join(" ", body) + " "
                + CRLF + CRLF).getBytes();
    }
    /**
     * Returns message type
     * @return messageType
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns message senderId
     * @return senderId
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Returns message body
     * @return body
     */
    public String getBody() {
        return body;
    }


    //TODO: Fix this. CRLF after header??? IT IS DIFFERENT TO READ! read line doesn't work
   /* public static String createMessage(String... headerFields){
        return (String.join(" ", headerFields) + " " + CRLF + CRLF);
    }

    public static String[] parseMessage(String message){
        return message.split(" ");
    }*/
}