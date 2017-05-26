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
    private BigInteger receiver;
    private String initialServerAddress;
    private int initialServerPort;
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
        this.initialServerPort = -1;
        this.initialServerAddress = null;
    }


    public Message(String messageType, BigInteger senderId, Object obj) {
        this.messageType = messageType;
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.senderId = senderId;

        if (obj instanceof String)
            this.body = String.join(" ", (String) obj);
        else
            this.object = obj;
    }

    public Message(String messageType, BigInteger senderId, Object obj, BigInteger clientId) {
        this.messageType = messageType;
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.senderId = senderId;
        this.receiver = clientId;

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

    /**
     * Get initial server address
     *
     * @return
     */
    public String getInitialServerAddress() {
        return initialServerAddress;
    }

    /**
     * Set initial server address
     *
     * @param initialServerAddress
     */
    public void setInitialServerAddress(String initialServerAddress) {
        this.initialServerAddress = initialServerAddress;
    }

    /**
     * Get initial server port
     *
     * @return
     */
    public int getInitialServerPort() {
        return initialServerPort;
    }

    /**
     * Set initial server port
     *
     * @param initialServerPort
     */
    public void setInitialServerPort(int initialServerPort) {
        this.initialServerPort = initialServerPort;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setSenderId(BigInteger senderId) {
        this.senderId = senderId;
    }

    public BigInteger getReceiver() {
        return receiver;
    }

}