package Server;

import Messages.Message;
import Messages.MessageHandler;

import javax.net.ssl.SSLSocket;
import java.io.*;

import static Utilities.Constants.*;

/**
 * Handles new SSL Connections to the server
 */
public class ConnectionHandler implements Runnable {

    private SSLSocket sslSocket;
    private BufferedReader in;
    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Server server;

    /**
     * Handles new SSL Connections to the server
     * @param socket
     * @param server
     */
    public ConnectionHandler(SSLSocket socket, Server server) {
        this.sslSocket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            serverOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            serverInputStream = new ObjectInputStream(sslSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("Error creating buffered reader...");
            e.printStackTrace();
        }
    }

    /**
     * Sends a message through a ssl socket
     */
    public void sendMessage(Message message) {
        try {
            serverOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Analyses Responses
     * @param response
     */
    public void analyseResponse(Message response) {
        String[] body = response.getBody().split(" ");

        System.out.println("Message received: " + response.getMessageType());

        switch (response.getMessageType()) {
            case SIGNIN:
                server.loginUser(body[0], body[1]);
                break;
            case SIGNUP:
                server.addUser(body[0],body[1]);
                break;
            case NEWNODE:
                int newNodeKey = Integer.parseInt(body[0]);
                Node n = server.predecessorLookUp(newNodeKey);
                int position = server.getNewNodePosition(newNodeKey);

                Node newNode = new Node(body[1],body[2],Integer.parseInt(body[0]));

                //In case of being a successor
                if(n.getNodeId() == server.getNodeId() && position == BEFORE){
                    try {
                        serverOutputStream.writeObject(new Message(SUCCESSOR.getBytes(),
                                Integer.toString(server.getNodeId()),Integer.toString(server.getNodeId()),server.getNodeIp(),server.getNodePort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.setPredecessor(newNode);
                }
                //In case of being the predecessor
                else if(n.getNodeId() == server.getNodeId() && position == AFTER){
                    try {
                        serverOutputStream.writeObject(new Message(PREDECESSOR.getBytes(),
                                Integer.toString(server.getNodeId()),Integer.toString(server.getNodeId()),server.getNodeIp(),server.getNodePort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        serverOutputStream.writeObject(new Message(NEWNODE_ANSWER.getBytes(),
                                Integer.toString(server.getNodeId()),Integer.toString(n.getNodeId()),n.getNodeIp(),n.getNodePort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                server.updateFingerTable(newNode);

                try {
                    sslSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            default:
                break;
        }
    }


    /**
     * Reads Messages
     */
    public void run() {
        Message message = null;
        try {
            message = (Message) serverInputStream.readObject();
            analyseResponse(message);
        } catch (IOException e) {
            System.out.println("Error reading message...");
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading message...");
        }

    }
}