package Utilities;


public class Constants {

    public static final byte CR = 0xD;
    public static final byte LF = 0xA;
    public static final String CRLF = "\r\n";

    //Messages
    public static final String FINGERTABLE = "FINGERTABLE";
    public static final String NEWNODE = "NEWNODE"; //Format: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
    public static final String NEWNODE_ANSWER = "NEWNODE ANSWER";
    public static final String PREDECESSOR = "PREDECESSOR";
    public static final String SUCCESSOR_FT = "SUCCESSOR_FT";
    public static final String USER_UPDATED_CONNECTION = "USER_UPDATED_CONNECTION";

    //INVITE_USER SenderId Chat password
    public static final String INVITE_USER = "INVITE_USER";

    //SIGNIN SenderId email password
    public static final String SUCCESSOR = "SUCCESSOR";

    //SIGNIN SenderId email passwordHash
    public static final String SIGNIN = "SIGNIN";

    //SIGNUP SenderId email passwordHash
    public static final String SIGNUP = "SIGNUP";

    //CLIENT_SUCCESS SenderId Code
    public static final String CLIENT_SUCCESS = "CLIENT_SUCCESS";

    //CLIENT_ERROR SenderId code
    public static final String CLIENT_ERROR = "CLIENT_ERROR";

    //CREATECHAT SenderId Chat
    //CLIENT_SUCCESS SenderId chatId
    public static final String CREATE_CHAT = "CREATECHAT";

    public static final String GET_CHAT = "GET_CHAT";

    //NEW_CHAT_INVITATION CHAT_ID CHAT_NAME
    public static final String NEW_CHAT_INVITATION = "NEW_CHAT_INVITATION";


    //SIGNOUT SenderId userId
    public static final String NEW_MESSAGE = "NEW_MESSAGE";

    //SIGNOUT SenderId userId
    public static final String SIGNOUT = "SIGNOUT";

    //BACKUP_USER SenderId email passwordHash
    public static final String BACKUP_USER = "BACKUP_USER";

    //SERVER_SUCCESS SenderId code
    public static final String SERVER_SUCCESS = "SERVER_SUCCESS";

    //SERVER_ERROR SenderId code
    public static final String SERVER_ERROR = "SERVER_ERROR";

    //ADD_USER SenderId User
    public static final String ADD_USER = "ADD_USER";

    //Directories
    public static final String USER_DIRECTORY = "users";
    public static final String CHAT_DIRECTORY = "chats";
    public static final String DATA_DIRECTORY = "data";

    //Numbers
    public static final int MAX_NUMBER_OF_THREADS = 5;

    //TODO: CHANGE max finger table size to 32 on final version
    public static final int MAX_FINGER_TABLE_SIZE = 7;
    public static final int MAX_NUMBER_OF_REQUESTS = 10;
    public static final int AFTER = 10;
    public static final int BEFORE = 20;
    public static final long MAX_NUMBER_OF_NODES = (long) Math.pow(2, MAX_FINGER_TABLE_SIZE);

    //Code meaning
    public static final String EMAIL_ALREADY_USED = "-1";
    public static final String EMAIL_NOT_FOUND = "-2";
    public static final String WRONG_PASSWORD = "-3";
    public static final String ERROR_CREATING_CHAT = "-4";
    public static final String INVALID_USER_EMAIL = "-5";
    public static final String USER_ADDED = "1";
    public static final String SENT_INVITATIONS = "2";
    public static final String CREATED_CHAT_WITH_SUCCESS = "3";
    public static final String SENT_MESSAGE = "4";



    public static final String IMAGE_MESSAGE = "image";
    public static final String TEXT_MESSAGE = "text";


}
