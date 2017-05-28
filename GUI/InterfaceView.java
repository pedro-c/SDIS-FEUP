package GUI;


import javax.swing.*;

import Controller.Controller;


public class InterfaceView extends JDialog {

    protected JDialog window;
    private Controller controller;

    public InterfaceView(Controller controller) {
        this.controller = controller;
    }

    public void showMenuWindow(){
        window = new Menu(controller);
        //window.pack();
        //window.setVisible(true);
    }

    public void showSignInView(){
        window = new SignInView(controller);
        window.pack();
        window.setVisible(true);
    }

    public void showSignUpView(){
        window = new SignUpView(controller);
        window.pack();
        window.setVisible(true);
    }

    public void showNotificationLogin(String signType) {
        window = new NotificationWrongLogin(controller, signType);
        window.pack();
        window.setVisible(true);
    }

    public void showSignedInView(){
        window = new SignedInView(controller);
        window.pack();
        window.setVisible(true);
    }

    public void showCreateMenu(){
        window = new CreateMenuView(controller);
        window.pack();
        window.setVisible(true);

    }

    public void showLoadChats(){
        window = new LoadChatsView(controller);
        window.pack();
        window.setVisible(true);
    }

}
