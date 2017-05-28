package GUI;

import Chat.Chat;
import Chat.ChatMessage;
import Controller.Controller;
import Messages.Message;
import Protocols.Connection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import static Utilities.Constants.NEW_MESSAGE;
import static Utilities.Constants.RESPONSIBLE;
import static Utilities.Constants.TEXT_MESSAGE;

public class ChatRoom extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea messenger;
    private JTextArea newMessage;

    private Controller controller;
    private BigInteger chatId;
    private Connection connection;

    private ConcurrentHashMap<BigInteger, Chat> chats;

    public ChatRoom(Controller controller, BigInteger chatId) {
        super(controller);
        this.controller = controller;

        this.chatId = chatId;

        System.out.println("estou na chat room");

        messenger =  new JTextArea();

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);


        for (ChatMessage message : chats.get(chatId).getChatMessages()){
            System.out.println("debug " + chats.get(chatId).getChatMessages().size());
            if(message.getType().equals(TEXT_MESSAGE))
                messenger.append(new String(message.getContent()));
            else
                messenger.append("Received new file with name : " + message.getFilename());

        }

        messenger.append("--------- New Messages ---------");

        Iterator<ChatMessage> iter = chats.get(chatId).getChatPendingMessages().iterator();
        while (iter.hasNext()) {
            ChatMessage message = iter.next();
            if (message.getChatId().compareTo(chatId) == 0) {
                controller.getUser().getChat(chatId).addChatMessage(message);
                if(message.getType().equals(TEXT_MESSAGE))
                    messenger.append(new String(message.getContent()));
                else messenger.append("Received new file with name : " + message.getFilename());
                iter.remove();
            }
        }

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }


    public void addText(String message){
        messenger.append(message);
    }

    public void sendMessage(){
        String newString = newMessage.getText();
        newMessage.setText("");
        messenger.append(newString);
        //controller.getUser().sendChatMessage(chatId);
    }
}
