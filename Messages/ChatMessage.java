package Messages;


public class ChatMessage {

    private Integer senderId;
    private Integer chatId;
    private String dateMessage;
    private String senderPublicKey;
    private String body;
    private String bodyChecksum;

    public ChatMessage(Integer senderId, Integer chatId, String senderPublicKey, String  body){
        this.senderId = senderId;
        this.chatId =  chatId;
        this.senderPublicKey = senderPublicKey;
        this.body = body;
    }

    public String createMessage(){
        String message = null;
        //Create Message
        return message;
    }
}
