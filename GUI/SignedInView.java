package GUI;

import Controller.Controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SignedInView extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton createChatButton;
    private JButton seeMyChatsButton;
    private JButton signOutButton;
    private JLabel welcome;

    private  Controller controller;

    public SignedInView(Controller controller) {
        super(controller);
        this.controller = controller;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);


        welcome.setText("Welcome " + controller.getUser().getEmail());


        createChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCreateChat();
            }
        });


        seeMyChatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoadChats();
            }
        });
        signOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSignOut();
            }
        });
    }


    public void onCreateChat(){
        controller.getView().showCreateMenu();
    }

    public void onLoadChats(){
        controller.getView().showLoadChats();
    }

    public void onSignOut(){
        this.dispose();
        controller.getUser().signOut();
    }
}
