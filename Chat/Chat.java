package Chat;

import Utilities.Utilities;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class Chat implements Serializable {

    private BigInteger idChat;
    private String chatName;
    private String creatorEmail;
    private Set<String> participants;
    private ArrayList<ChatMessage> chatMessages;
    private ArrayList<ChatMessage> pendingChatMessages;

    public Chat(String creatorEmail, String name) {
        this.idChat = Utilities.generateChatId(creatorEmail);
        this.creatorEmail = creatorEmail;
        this.chatMessages = new ArrayList<ChatMessage>();
        this.pendingChatMessages = new ArrayList<ChatMessage>();
        this.participants = new HashSet<>();
        if(name==null)
            chatName = "chat default";
        else chatName = name;
    }

    public Chat(BigInteger idChat, String chatName) {
        this.idChat = idChat;
        this.chatName = chatName;
        this.creatorEmail = creatorEmail;
        this.chatMessages = new ArrayList<ChatMessage>();
        this.pendingChatMessages = new ArrayList<ChatMessage>();
        this.participants = new HashSet<>();
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public BigInteger getIdChat() {
        return idChat;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void addParticipant(String email) {
        participants.add(email);
    }

    public ArrayList<ChatMessage> getChatMessages() {return chatMessages;}

    public ArrayList<ChatMessage> getChatPendingMessages() {return pendingChatMessages;}

    public void addChatMessage(ChatMessage chatMessage){chatMessages.add(chatMessage);}

    public void addPendingChatMessage(ChatMessage chatMessage){pendingChatMessages.add(chatMessage);}

}
