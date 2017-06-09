package Server;

import Chat.Chat;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import static Utilities.Utilities.createHash;

public class User implements Serializable {

    protected String email;
    protected BigInteger password;
    protected ConcurrentHashMap<BigInteger, Chat> chats;
    protected ConcurrentHashMap<BigInteger, Chat> pendingRequests;
    protected byte[] privateKey;
    protected PublicKey publicKey;

    public User(String email, BigInteger password) {
        this.email = email;
        this.password = password;
        chats = new ConcurrentHashMap<BigInteger, Chat>();
        pendingRequests = new ConcurrentHashMap<BigInteger, Chat>();

    }


    public User(String email, BigInteger password, byte[] privateKey, PublicKey publicKey) {
        this.email = email;
        this.password = password;
        chats = new ConcurrentHashMap<BigInteger, Chat>();
        pendingRequests = new ConcurrentHashMap<BigInteger, Chat>();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public boolean confirmSignIn(String newEmail, BigInteger newPassword) {
        if (email.equals(newEmail)) {
            if (password.equals(newPassword))
                return true;
        }
        return false;
    }

    public String getEmail() {
        return email;
    }

    public BigInteger getPassword() {
        return password;
    }

    public void addChat(Chat chat) {
        chats.put(chat.getIdChat(), chat);
    }

    public Chat getChat(BigInteger chatId) {
        return chats.get(chatId);
    }

    public void addPendingChat(Chat chat) {
        pendingRequests.put(chat.getIdChat(), chat);
    }

    public BigInteger getUserId() {
        return createHash(email);
    }

    public ConcurrentHashMap<BigInteger, Chat> getChats() {
        return chats;
    }

    public ConcurrentHashMap<BigInteger, Chat> getPendingRequests() {
        return pendingRequests;
    }

    public void deletePendingRequest(BigInteger chatId) {
        pendingRequests.remove(chatId);
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
}
