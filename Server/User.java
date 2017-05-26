package Server;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import Chat.Chat;

import static Utilities.Utilities.createHash;

public class User implements Serializable{

    protected String email;
    protected BigInteger password;
    protected ConcurrentHashMap<BigInteger, Chat> chats;
    protected ConcurrentHashMap<BigInteger, Chat> pendingRequests;

    public User(String email, BigInteger password) {
        this.email = email;
        this.password = password;
        chats = new ConcurrentHashMap<BigInteger, Chat>();
        pendingRequests = new ConcurrentHashMap<BigInteger, Chat>();

    }

    public boolean confirmSignIn(String newEmail, BigInteger newPassword){
        if(email.equals(newEmail)){
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

    public Chat getChat(BigInteger chatId){ return chats.get(chatId); }

    public void addPendingChat(Chat chat){ pendingRequests.put(chat.getIdChat(),chat);}

    public BigInteger getUserId() {
        return createHash(email);
    }

    public ConcurrentHashMap<BigInteger, Chat> getChats() {
        return chats;
    }

    public ConcurrentHashMap<BigInteger, Chat> getPendingRequests() {
        return pendingRequests;
    }

    public void instantiateChats() {
        chats = new ConcurrentHashMap<BigInteger, Chat>();
        pendingRequests = new ConcurrentHashMap<BigInteger, Chat>();
    }
}
