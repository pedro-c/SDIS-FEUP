package Messages;

/**
 * Created by mariajoaomirapaulo on 28/04/17.
 */
public class ChatMessage {

    private Integer senderId;
    private Integer chatId;
    private String dateMessage;
    private String senderPublicKey;
    private byte[] body;
    private String bodyChecksum;

    public ChatMessage(Integer senderId, Integer chatId, String senderPublicKey, byte[] body){
        this.senderId = senderId;
        this.chatId =  chatId;
        this.senderPublicKey = senderPublicKey;
        this.body = body;
    }

    public byte[] createMessage(){
        byte[] message = null;
        //Create Message
        return message;
    }
}
