package GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Controller.Controller;
import Client.Client;

public class SignUpView extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonReturn;
    private JTextField emailField;
    private JTextField passField;
    private JButton signUpButton;

    private Controller controller;
    private String email=null;
    private String password=null;

    public SignUpView(Controller controller) {
        super(controller);
        this.controller=controller;

        setContentPane(contentPane);
        this.pack();
        setModal(true);
        //getRootPane().setDefaultButton(buttonReturn);

        this.controller = controller;

        signUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onSignUp();
            }
        });

        buttonReturn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onReturn();
            }
        });
    }

    public void onSignUp(){
        email = emailField.getText();
        password = passField.getText();
        this.dispose();
        controller.getUser().signUpUser();
    }

    public void onReturn(){
        this.dispose();
        controller.getView().showMenuWindow();
    }
}
