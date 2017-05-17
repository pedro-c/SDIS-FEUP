package Server;

import java.math.BigInteger;
import Chat.*;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class User {

    private String email;
    private BigInteger password;
    private Hashtable<BigInteger, Chat> chats;


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

    public void addChat(Chat chat){
        chats.put(chat.getIdChat(),chat);
    }

    public Chat getChat(Chat chat){
        return chats.get(chat.getIdChat());
    }
}
