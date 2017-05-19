package Protocols;

import Client.Client;
import Messages.Message;
import Server.ServerChat;
import Chat.Chat;

import java.math.BigInteger;

import static Client.Client.Task.*;
import static Utilities.Constants.*;

public class ClientConnection extends Connection implements Runnable {

    private Client client;

    public ClientConnection(String ip, int port, Client client) {
        super(ip, port);

        this.client = client;
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
        System.out.println("\nSending message - Header: " + message.getMessageType() + " Body " + message.getBody() + "\n");
        super.sendMessage(message);
    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage(){
        Message message = super.receiveMessage();

        System.out.println("\nReceiving message - Header: " + message.getMessageType() + " Body " + message.getBody() + "\n");

        return message;
    }

    /**
     * Close the connection
     */
    public void closeConnection(){
        super.closeConnection();
    }

    /**
     * Handles clients message
     * @param message to be processed
     */
    public void handleMessage(Message message) {


        switch (message.getMessageType()) {

            case CLIENT_SUCCESS:
               /* if (connectionHandler != null) {
                    System.out.println("Sending message back to initiator server");
                    connectionHandler.sendMessage(response);
                }*/
               /* if (client.getAtualState() == WAITING_SIGNUP || client.getAtualState() == WAITING_SIGNIN) {
                    client.setServerIp(message.getInitialServerAddress());
                    client.setServerPort(message.getInitialServerPort());
                    client.setAtualState(SIGNED_IN);
                    System.out.println("Logged in with success..");
                } else if (client.getAtualState() == WAITING_CREATE_CHAT) {
                    client.setAtualState(CREATING_CHAT);
                    System.out.println("Creating chat");
                    client.setPendingChat(new BigInteger(message.getBody()));
                }
                break;*/
            case CLIENT_ERROR:
                /*if (client.getAtualState() == WAITING_SIGNUP || client.getAtualState() == WAITING_SIGNIN) {
                    client.setAtualState(HOLDING);
                } else if (client.getAtualState() == WAITING_CREATE_CHAT) {
                    client.setAtualState(HOLDING);
                }*/
                client.verifyState(message);
                break;
            case NEW_CHAT_INVITATION:
                System.out.println("Received new chat invitation..");
                if (message.getMessageType().equals(NEW_CHAT_INVITATION)) {
                    ServerChat sv = (ServerChat) message.getObject();
                    Chat chat = new Chat(sv.getIdChat(), sv.getCreatorEmail());
                    client.addPendingChat(chat);
                } else {
                    System.out.println("Error");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {

        while(true){
            Message message = receiveMessage();

            Runnable task = () -> {
                handleMessage(message);
            };

            service.execute(task);
        }

    }
}
