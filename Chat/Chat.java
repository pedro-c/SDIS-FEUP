package Chat;

import Server.User;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by mariajoaomirapaulo on 13/05/17.
 */
public class Chat {
    private BigInteger idChat;
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

}
