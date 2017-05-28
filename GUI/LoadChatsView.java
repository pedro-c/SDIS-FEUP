package GUI;

import Chat.Chat;
import Controller.Controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class LoadChatsView extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel chatPanel;

    private Controller controller;
    private ConcurrentHashMap<BigInteger, Chat> chats;


    public LoadChatsView(Controller controller) {
        super(controller);
        this.controller = controller;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        //controller.getUser().loadChats();
        chatPanel = new JPanel();

        chats = controller.getUser().getClientChats();

        System.out.println("LOAD CHATS" + chats.size());

        int i=1;
        for(BigInteger chatId : chats.keySet()){

            System.out.println("n" + i + " " + chatId);
            JButton button = new JButton(chats.get(chatId).getChatName());
            button.setVisible(true);

            System.out.println("print 1");
            chatPanel.add(button);
            button.revalidate();
            validate();
            button.setVisible(true);
            System.out.println("print2");
            button.revalidate();
            button.repaint();
            System.out.println("print3");

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onClick(chatId);
                }
            });

        }
    }

    public void onClick(BigInteger chatId){
        controller.getUser().openChat(chatId);
    }
}
