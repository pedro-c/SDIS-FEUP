package Server;

import Chat.ChatMessage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mariajoaomirapaulo on 16/05/17.
 */
public class ServerChat {

    private Set<User> participants;
    private BigInteger idChat;
    //private User creator;
    private String creatorEmail;
    private ArrayList<ChatMessage> chatMessages;

    public ServerChat(BigInteger idChat, String creatorEmail) {
        this.idChat = idChat;
        this.chatMessages = new ArrayList<>();
        this.participants = new HashSet<User>();
        this.creatorEmail = creatorEmail;
    }

    public void addParticipant(User user) {
        participants.add(user);
    }

    public BigInteger getIdChat() {
        return idChat;
    }


}
