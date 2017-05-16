package Messages;

import java.io.Serializable;
import java.math.BigInteger;

import static Utilities.Constants.CRLF;

/**
 * Message class.
 */
public class Message implements Serializable {

    private String messageType;
    private BigInteger senderId;
    private String body;

    private Object object;

    /**
     * Message Constructor
     *
     * @param messageType
     * @param senderId
     * @param body
     */
    public Message(String messageType, BigInteger senderId, String... body) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.body = String.join(" ", body);
    }


    public Message(String messageType, BigInteger senderId, Object obj) {
        this.messageType = messageType;
        this.senderId = senderId;

        if (obj instanceof String)
            this.body = String.join(" ", (String) obj);
        else
            this.object = obj;
    }

    /**
     * Creates Message with format[MessageType][SenderId][CRLF][Body][CRLF][CRLF]
     *
     * @param messageType
     * @param senderId
     * @param body
     * @return
     */
    public byte[] createMessage(String messageType, String senderId, String body) {

        return (String.join(" ", messageType) + " "
                + String.join(" ", senderId) + " "
                + CRLF
                + String.join(" ", body) + " "
                + CRLF + CRLF).getBytes();
    }

    /**
     * Returns message type
     *
     * @return messageType
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns message senderId
     *
     * @return senderId
     */
    public BigInteger getSenderId() {
        return senderId;
    }

    /**
     * Returns message body
     *
     * @return body
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns Chat
     *
     * @return
     */
    public Object getObject() {
        return object;
    }

    /**
     * Prints message content
     */
    public void printMessage(Message message) {

        System.out.println("Message Type: " + message.getMessageType() + "\n");
        System.out.println("Body: " + message.getBody() + "\n");
    }


}