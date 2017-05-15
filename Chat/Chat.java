package Chat;

import Server.User;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class Chat implements Serializable {
    //criador
    private BigInteger idChat;
    private String chatName;
    private Set<User> participants;
    private ArrayList<ChatMessage>  chatMessages;

    public Chat(BigInteger idChat) {
        this.idChat = idChat;
    }

    public void addParticipant(User user){
        participants.add(user);
    }

    public void removeParticipant(User user){
        participants.remove(user);
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getChatName() {
        return chatName;
    }

}
