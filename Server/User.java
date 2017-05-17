package Server;

import Chat.Chat;

import java.math.BigInteger;
import java.util.Hashtable;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class User {

    private String email;
    private BigInteger password;
    private Hashtable<BigInteger, ServerChat> chats;


    public User(String email, BigInteger password) {
        this.email = email;
        this.password = password;
        chats = new Hashtable<BigInteger, ServerChat>();
    }

    public String getEmail() {
        return email;
    }

    public BigInteger getPassword() {
        return password;
    }

    public void addChat(ServerChat chat) {
        chats.put(chat.getIdChat(), chat);
    }
}
