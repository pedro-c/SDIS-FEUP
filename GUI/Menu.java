package GUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Menu extends InterfaceView {
    private JPanel contentPane;
    private JButton signUpButton;
    private JButton exitButton;
    private JButton signInButton;
    //private Menu dialog;


    public Menu() {
        setContentPane(contentPane);
        //setModal(true);
        //getRootPane().setDefaultButton(buttonOK);

        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onExit();
            }
        });

//        buttonOK.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                onOK();
//            }
//        });
//
//        buttonCancel.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                onCancel();
//            }
//        });
//
//        // call onCancel() when cross is clicked
//        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                onCancel();
//            }
//        });
//
//        // call onCancel() on ESCAPE
//        contentPane.registerKeyboardAction(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                onCancel();
//            }
//        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //dialog.setVisible(false);
                new SignInWindow().setVisible(true);
            }
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onExit() {
        // add your code here if necessary
        dispose();
        System.exit(0);
    }

    /*public static void main(String[] args) {
        Menu dialog = new Menu();
        dialog.pack();
        dialog.setVisible(true);

    }*/
}
