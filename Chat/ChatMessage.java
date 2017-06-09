package Chat;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;


public class ChatMessage implements Serializable {
    private Date creationDate;
    private BigInteger userId;
    private byte[] content;
    private String type;
    private BigInteger chatId;
    private String filename;

    public ChatMessage(BigInteger chatId, Date creationDate, BigInteger userId, byte[] content, String type) {
        this.chatId = chatId;
        this.creationDate = creationDate;
        this.userId = userId;
        this.content = content;
        this.type = type;
    }

    public ChatMessage(BigInteger chatId, Date creationDate, BigInteger userId, byte[] content, String type, String filename) {
        this.chatId = chatId;
        this.creationDate = creationDate;
        this.userId = userId;
        this.content = content;
        this.type = type;
        this.filename = filename;
    }

    public BigInteger getUserId() {
        return userId;
    }

    public BigInteger getChatId() {
        return chatId;
    }

    public byte[] getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }

}
