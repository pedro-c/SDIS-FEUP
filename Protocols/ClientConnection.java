package Protocols;

import Client.Client;
import Messages.Message;
import Chat.Chat;

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
        System.out.println("\nSending message - Header: " + message.getMessageType() + " Body " + message.getBody());
        super.sendMessage(message);
    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage(){
        Message message = super.receiveMessage();

        System.out.println("\nReceiving message - Header: " + message.getMessageType() + " Body " + message.getBody());

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
            case CLIENT_ERROR:
                client.verifyState(message);
                break;
            case NEW_CHAT_INVITATION:
                System.out.println("Received new chat invitation..");
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {

        while(true){

            System.out.println("Listening...");

            Message message = receiveMessage();

            Runnable task = () -> {
                handleMessage(message);
            };

            service.execute(task);
        }

    }
}
