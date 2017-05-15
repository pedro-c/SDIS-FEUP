package Utilities;


public class Constants {

    public static final byte CR = 0xD;
    public static final byte LF = 0xA;
    public static final String  CRLF = "\r\n";

    //Messages
    public static final String FINGERTABLE = "FINGERTABLE";
    public static final String NEWNODE = "NEWNODE"; //Format: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
    public static final String NEWNODE_ANSWER = "NEWNODE ANSWER";
    public static final String PREDECESSOR = "PREDECESSOR";

    //SIGNIN SenderId email password
    public static final String SUCCESSOR = "SUCCESSOR";
    public static final String SIGNIN = "SIGNIN";

    //SIGNUP SenderId email passwordHash
    public static final String SIGNUP = "SIGNUP";

    //CLIENT_SUCCESS SenderId
    public static final String CLIENT_SUCCESS = "CLIENT_SUCCESS";

    //CLIENT_ERROR SenderId code
    public static final String CLIENT_ERROR = "CLIENT_ERROR";

    //CREATECHAT SenderId Chat
    public static final String CREATE_CHAT = "CREATECHAT";

    //Directories
    public static final String USER_DIRECTORY = "users";
    public static final String CHAT_DIRECTORY = "chats";
    public static final String DATA_DIRECTORY = "data";

    //Files
    public static final String SERVERS_INFO = "servers.info";

    //Numbers
    //TODO: CHANGE max finger table size to 32 on final version
    public static final int MAX_FINGER_TABLE_SIZE = 5;
    public static final int MAX_NUMBER_OF_REQUESTS = 10;
    public static final int AFTER = 10;
    public static final int BEFORE = 20;
    public static final long MAX_NUMBER_OF_NODES = (long)Math.pow(2,MAX_FINGER_TABLE_SIZE);

    //Code meaning
    public static final String EMAIL_ALREADY_USED = "-1";
    public static final String EMAIL_NOT_FOUND = "-2";
    public static final String WRONG_PASSWORD = "-3";

}
