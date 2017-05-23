package Chat;

import java.math.BigInteger;
import java.util.Date;


public class ChatMessage {
    private Date creationDate;
    private BigInteger userId;
    private byte[] content;

    public ChatMessage(Date creationDate, BigInteger userId, byte[] content) {
        this.creationDate = creationDate;
        this.userId = userId;
        this.content = content;
    }

}
