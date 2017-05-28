package GUI;

import Controller.Controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

public class CreateMenuView extends InterfaceView {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField chatnameField;
    private JTextField emailField;
    private JButton addButton;

    private Vector<String> emails;
    private Controller controller;

    public CreateMenuView(Controller controller) {
        super(controller);
        this.controller = controller;

        setContentPane(contentPane);
        setModal(true);

        emails = new Vector<>();

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAdd();
            }
        });


        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
    }

    public void onAdd(){
        emails.add(emailField.getText());
        emailField.setText("");
    }

    public void onOK(){
        this.dispose();
        controller.getUser().createChatView(emails, chatnameField.getText());
    }

    public void onCancel(){
        this.dispose();
    }
}
