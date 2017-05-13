package Utilities;


public class Constants {

    public static final byte CR = 0xD;
    public static final byte LF = 0xA;
    public static final String  CRLF = "\r\n";


    public static final String FINGERTABLE = "FINGERTABLE";
    public static final String NEWNODE = "NEWNODE"; //Format: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
    public static final String NEWNODE_ANSWER = "NEWNODE ANSWER";
    public static final String PREDECESSOR = "PREDECESSOR";
    public static final String SUCCESSOR = "SUCCESSOR";
    public static final String SIGNIN = "SIGNIN";
    public static final String SIGNUP = "SIGNUP";

    public static final int MAX_FINGER_TABLE_SIZE = 32;
    public static final int MAX_NUMBER_OF_REQUESTS = 10;
    public static final int AFTER = 10;
    public static final int BEFORE = 20;
    public static final long MAX_NUMBER_OF_NODES = (long)Math.pow(2,32);

}
