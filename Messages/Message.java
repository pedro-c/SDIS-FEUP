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
    private String clientAddress;
    private int clientPort;
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
        this.clientPort = -1;
        this.clientAddress = null;
    }


    public Message(String messageType, BigInteger senderId, Object obj) {
        this.messageType = messageType;
        this.clientPort = clientPort;

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
     * Get client InetAddress
     * @return
     */
    public String getClientAddress() {
        return clientAddress;
    }

    /**
     * Get client port
     * @return
     */
    public int getClientPort() {
        return clientPort;
    }

    /**
     * Set client address
     * @param clientAddress
     */
    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    /**
     * Set client port
     * @param clientPort
     */
    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }
    /**
     * Prints message content
     */
    public void printMessage(Message message) {

        System.out.println("Message Type: " + message.getMessageType() + "\n");
        System.out.println("Body: " + message.getBody() + "\n");
    }


}