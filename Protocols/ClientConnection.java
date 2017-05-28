package Protocols;

import Client.Client;
import Messages.Message;
import Chat.Chat;
import Chat.ChatMessage;

import java.math.BigInteger;

import java.io.IOException;

import static Utilities.Constants.*;

public class ClientConnection extends Connection implements Runnable {

    private Client client;
    private Boolean listen;

    public ClientConnection(String ip, int port, Client client) {
        super(ip, port);
        this.listen = true;
        this.client = client;
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
        try {
            super.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("\nError sending message...");
            client.recoverConnection();
        }
    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {

        Message message = super.receiveMessage();

        System.out.println("\nReceiving message - Header: " + message.getMessageType() +  " Sender: " + Integer.remainderUnsigned(message.getSenderId().intValue(),128) + " Body " + message.getBody());

        return message;
    }

    /**
     * Close the connection
     */
    public void closeConnection(){
        System.out.println("Closing client connection");
        super.closeConnection();
    }

    /**
     * Handles clients message
     * @param message to be processed
     */
    public void handleMessage(Message message) {
        String[] body;

        switch (message.getMessageType()) {
            case CLIENT_SUCCESS:
            case CLIENT_ERROR:
                client.verifyState(message);
                break;
            case NEW_CHAT_INVITATION:
                System.out.println(" Received new chat invitation... ");
                Chat chat = (Chat) message.getObject();
                client.addChat(chat);
                //TODO: Preciso??
                //client.askForChat(chat.getIdChat());
                break;
            case NEW_MESSAGE:
                System.out.println("Received a new message\n" );
                ChatMessage chatMessage = (ChatMessage) message.getObject();
                if(client.getCurrentChat()==NO_CHAT_OPPEN || client.getCurrentChat()!=chatMessage.getChatId().intValue()){
                    client.getChat(chatMessage.getChatId()).addPendingChatMessage(chatMessage);
                    System.out.println("Saved on pending chat messages");
                }
                else {
                    client.getChat(chatMessage.getChatId()).addChatMessage(chatMessage);
                    System.out.println(new String(chatMessage.getContent()));
                }
                break;
            case SERVER_UPDATE_CONNECTION:
                body = message.getBody().split(" ");
                client.updateConnection(body[0], Integer.parseInt(body[1]));
                break;
            case SERVER_SUCCESS:
            case SERVER_ERROR:
                body = message.getBody().split(" ");
                client.printError(body[0]);
                break;
            default:
                break;
        }
    }

    public void endThread(){
        this.listen = false;
    }

    @Override
    public void run() {

        while(true){

            System.out.println("Listening...");

            try {
                Message message = receiveMessage();

                System.out.println("Received message: " + message.getMessageType());

                Runnable task = () -> {
                    handleMessage(message);
                };

                service.execute(task);
            } catch (IOException e) {
                System.out.println("Closed Connection");
                stopTasks();
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("Closed Connection");
                stopTasks();
                return;
            }

        }

    }
}
