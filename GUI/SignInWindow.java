package GUI;

import javax.swing.*;
import Client.Client;
import javafx.util.converter.BigIntegerStringConverter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodListener;
import java.math.BigInteger;

public class SignInWindow extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField emailField;
    private JTextField passField;
    private JLabel emaiLabel;
    private JLabel passLabel;
    private JButton signInButton;
    private Client user;

    public SignInWindow() {
        setContentPane(contentPane);

       //getRootPane().setDefaultButton(buttonOK)


        this.pack();
        setModal(true);

        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSignIn();
            }
        });
    }


    public void onSignIn(){

        String email =  emailField.getText();
        BigInteger password = new BigInteger(emailField.getText());

        //user.signInUser();


    }


}
