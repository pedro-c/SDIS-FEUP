package Chat;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class Chat implements Serializable {

    private BigInteger idChat;
    private String chatName;
    private String creatorEmail;
    private String participant_email;
    private Set<String> participants;
    private ArrayList<ChatMessage> chatMessages;

    public Chat(BigInteger idChat, String creatorEmail) {
        this.idChat = idChat;
        this.creatorEmail = creatorEmail;
        this.chatMessages = new ArrayList<ChatMessage>();
        this.participants = new HashSet<>();
    }

    public String getCreatorEmail() {return creatorEmail;}

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
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

    public void addParticipant(String email) {
        participants.add(email);
    }

}
