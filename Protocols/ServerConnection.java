package Protocols;

import Chat.Chat;
import Messages.Message;
import Server.*;

import javax.net.ssl.SSLSocket;
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
    public void connect(){
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
    public Message receiveMessage(){
        Message message = super.receiveMessage();

        System.out.println("\nReceiving message - Header: " + message.getMessageType() + " Sender: " + message.getSenderId() + " Body " + message.getBody());

        return message;
    }

    /**
     * Close the connection
     */
    public void closeConnection(){
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
                if(server.isResponsibleFor(message.getSenderId())){
                    server.saveConnection(this, message.getSenderId());
                    server.printLoggedInUsers();
                }
            case SIGNOUT:
            case CREATE_CHAT:
                server.isResponsible(this,message);
                break;
            case GET_CHAT:
                server.isResponsible(this,message);
                break;
            case INVITE_USER:
                if (server.isResponsibleFor(message.getSenderId())) {
                    System.out.println("I'm the RESPONSIBLE server");
                } else {
                    server.redirect(this,message);
                }
                break;
            case NEW_MESSAGE:
                server.isResponsible(this, message);
                break;
            case NEWNODE:
                body = message.getBody().split(" ");
                server.newNode(body);
                server.getDht().printFingerTable();
                break;
            case PREDECESSOR:
                Node temp = (Node) message.getObject();
                server.getDht().setPredecessor(temp);
                server.sendFingerTableToSuccessor();
                server.getDht().printFingerTable();
                break;
            case SUCCESSOR_FT:
                ArrayList<Node> ft = (ArrayList<Node>) message.getObject();
                server.getDht().updateFingerTableFromSuccessor(ft);
                server.getDht().setPredecessor(ft.get(0));
                server.getDht().printFingerTable();
                break;
            case BACKUP_USER:
                if (message.getObject() != null) {
                    User user = (User) message.getObject();
                    server.getBackups().put(createHash(user.getEmail()), user);
                    sendMessage(new Message(SERVER_SUCCESS, BigInteger.valueOf(this.server.getNodeId()), USER_ADDED));
                } else
                    sendMessage(server.backupInfo(message));
                break;
            case ADD_USER:
                sendMessage(server.addUser((User) message.getObject()));
                break;
            case USER_UPDATED_CONNECTION:
                server.saveConnection(this,message.getSenderId());
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {

        while (true){
            Message message = receiveMessage();

            Runnable task = () -> {
                handleMessage(message);
            };

            service.execute(task);
        }
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
