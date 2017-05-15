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
    private String creatorEmail;
    //TODO: Faz sentido ter no client??
    private Set<User> participants;
    private String participant_email;
    private ArrayList<ChatMessage>  chatMessages;


    public Chat(BigInteger idChat, String creatorEmail) {
        this.idChat = idChat;
        this.creatorEmail = creatorEmail;
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

    public BigInteger getIdChat() {
        return idChat;
    }

    public String getParticipant_email() {
        return participant_email;
    }

    public void setParticipant_email(String participant_email) {
        this.participant_email = participant_email;
    }

}
