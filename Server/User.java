package Server;

import Chat.Chat;

import java.math.BigInteger;
import java.util.Hashtable;

import static Utilities.Utilities.createHash;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class User {

    private String email;
    private BigInteger password;
    private Hashtable<BigInteger, ServerChat> chats;
    private String clientAddress;
    private int clientPort;


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

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public BigInteger getUserId(){
        return createHash(email);
    }

}
