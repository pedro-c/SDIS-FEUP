package Server;

import java.math.BigInteger;
import java.util.Hashtable;

import Chat.Chat;

import static Utilities.Utilities.createHash;

public class User {

    protected String email;
    protected BigInteger password;
    protected Hashtable<BigInteger, Chat> chats;

    public User(String email, BigInteger password) {
        this.email = email;
        this.password = password;
        chats = new Hashtable<BigInteger, Chat>();
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

    public BigInteger getUserId() {
        return createHash(email);
    }

}
