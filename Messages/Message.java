package Messages;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;

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
    private String responsible;
    private String email;
    private String password;
    private byte[] privateKey;
    private PublicKey publicKey;
    private Object object;
    private String chatId;

    /**
     * Message Constructor
     *
     * @param messageType
     * @param senderId
     * @param body
     */
    public Message(String messageType, BigInteger senderId, String responsible, String... body) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.responsible = responsible;
        this.body = String.join(" ", body);
        this.initialServerPort = -1;
        this.initialServerAddress = null;
    }

    public Message(String messageType, BigInteger senderId, String responsible, byte[] privateKey, PublicKey publicKey) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.responsible = responsible;
        this.body = String.join(" ", body);
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Message(String messageType, BigInteger senderId, String responsible, String email, String password, byte[] privateKey, PublicKey publicKey) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.responsible = responsible;
        this.body = String.join(" ", body);
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.email = email;
        this.password = password;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Message(String messageType, BigInteger senderId, String responsible, String chatId, PublicKey publicKey, BigInteger receiver) {
        this.messageType = messageType;
        this.senderId = senderId;
        this.responsible = responsible;
        this.body = String.join(" ", body);
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.chatId = chatId;
        this.publicKey = publicKey;
        this.receiver = receiver;
    }



    public Message(String messageType, BigInteger senderId, String responsible, Object obj) {
        this.messageType = messageType;
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.senderId = senderId;
        this.responsible = responsible;

        if (obj instanceof String)
            this.body = String.join(" ", (String) obj);
        else
            this.object = obj;
    }

    public Message(String messageType, BigInteger senderId, String responsible, Object obj, BigInteger clientId) {
        this.messageType = messageType;
        this.initialServerPort = -1;
        this.initialServerAddress = null;
        this.senderId = senderId;
        this.receiver = clientId;
        this.responsible = responsible;

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


    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setReceiver(BigInteger receiver) {
        this.receiver = receiver;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}