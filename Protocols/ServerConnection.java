package Protocols;

import Chat.ChatMessage;
import Messages.Message;
import Server.*;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

import static Utilities.Constants.*;
import static Utilities.Utilities.*;

public class ServerConnection extends Connection implements Runnable {

    private Server server;

    public ServerConnection(String ip, int port, Server server) {
        super(ip, port);

        this.server = server;
    }

    public ServerConnection(SSLSocket socket, Server server){
        super(socket);

        this.server = server;
    }

    /**
     * Connects to a certain ip and port
     */
    public void connect() throws IOException {
        super.connect();
    }

    /**
     * Sends a message
     * @param message message to be sent
     */
    public void sendMessage(Message message) {
        System.out.println("\nSending message - Header: " + message.getMessageType() + " Body " + message.getBody());
        super.sendMessage(message);
    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        Message message = super.receiveMessage();

        System.out.println("\nReceiving message - Header: " + message.getMessageType() + " Sender: " + Integer.remainderUnsigned(message.getSenderId().intValue(),128) + " Body " + message.getBody());

        return message;
    }

    /**
     * Close the connection
     */
    public void closeConnection(){
        System.out.println("Closing server connection");
        super.closeConnection();
    }

    /**
     * Handles server message
     * @param message to be processed
     */
    public void handleMessage(Message message){
        String[] body;
        
        switch (message.getMessageType()) {
            case SIGNIN:
            case SIGNUP:
                server.isResponsible(this,message);
                break;
            case SIGNOUT:
            case CREATE_CHAT:
            case CREATE_CHAT_BY_INVITATION:
                server.isResponsible(this,message);
                break;
            case GET_CHAT:
                server.isResponsible(this,message);
                break;
            case GET_ALL_CHATS:
                server.isResponsible(this,message);
                System.out.print("Sending chats");
                break;
            case GET_ALL_PENDING_CHATS:
                System.out.println("Received Get All Pending Chats");
                server.isResponsible(this, message);
                break;
            case INVITE_USER:
                if (message.getResponsible().equals(RESPONSIBLE)) {
                    System.out.println("I'm the RESPONSIBLE server");
                } else {
                    server.redirect(this,message);
                }
                break;
            case NEW_MESSAGE_TO_PARTICIPANT:
            case NEW_MESSAGE:
                server.isResponsible(this, message);
                break;
            case NEWNODE:
                body = message.getBody().split(" ");
                server.newNode(body);
                server.getDht().printFingerTable();
                closeConnection();
                break;
            case PREDECESSOR:
                Node temp = (Node) message.getObject();
                server.getDht().setPredecessor(temp);
                server.sendFingerTableToSuccessor();
                server.getDht().printFingerTable();
                closeConnection();
                break;
            case SUCCESSOR_FT:
                ArrayList<Node> ft = (ArrayList<Node>) message.getObject();
                server.getDht().updateFingerTableFromSuccessor(ft);
                server.getDht().setPredecessor(ft.get(0));
                server.getDht().printFingerTable();
                closeConnection();
                break;
            case BACKUP_USER:
                sendMessage(server.backupInfo(message));
                closeConnection();
                break;
            case ADD_USER:
                sendMessage(server.addUser((User) message.getObject()));
                break;
            case USER_UPDATED_CONNECTION:
                System.out.println("\nEntrei\n");
                server.saveConnection(this,message.getSenderId());
                break;
            case SERVER_SUCCESS:
                body = message.getBody().split(" ");
                server.printReturnCodes(body[0],message.getSenderId());
                break;
            case SERVER_DOWN:
                body = message.getBody().split(" ");
                System.out.println("Server " + body[0] + " is down.");
                server.handleNodeFailure(Integer.parseInt(body[0]), message);
                break;
            case FILE_TRANSACTION:
                server.isResponsible(this,message);
                break;
            case STORE_FILE_ON_PARTICIPANT:
                server.isResponsible(this,message);
                break;
            case STORE_FILE_MESSAGE:
                server.isResponsible(this,message);
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {

        while (true){

            try {
                Message message = receiveMessage();

                Runnable task = () -> {
                    handleMessage(message);
                };

                service.execute(task);
            } catch (IOException e) {
                System.out.println("Server closed Connection");
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("Server closed Connection");
                return;
            }

        }

    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
