package GUI;

import Controller.Controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NotificationWrongLogin extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;

    private Controller controller;

    public NotificationWrongLogin(Controller controller, String signType) {
        super(controller);
        this.controller=controller;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK(signType);
            }
        });
    }

    private void onOK(String type) {
        // add your code here
        dispose();
        if(type.equals("signIn"))
            controller.getView().showSignInView();
        else
            controller.getView().showSignUpView();
    }
}
