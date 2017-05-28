package Controller;

import Client.Client;
import GUI.InterfaceView;
import java.math.BigInteger;


/**
 * Created by ines on 23/05/17.
 */

public class Controller {

    private Client user;
    private InterfaceView view;

    public Controller(String serverIp, int serverPort){

        user = new Client(serverPort, serverIp, this);
        view = new InterfaceView(this);

        view.showMenuWindow();

        while(true){
        }


    }

    public static void main(String[] args){
        if (args.length != 2) {
            throw new IllegalArgumentException("\nUsage : java Controller.Controller <serverIp> <serverPort>");
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        Controller controller = new Controller(serverIp, serverPort);
    }


    public void updateScreen(Client.Task task){
        switch (task){
            //case WAITING_SIGNOUT:
              //  view.showNotificationLogin();
               // break;
        }
    }

    public Client getUser() {
        return user;
    }

    public InterfaceView getView() {
        return view;
    }

    public void setNewState(Client.Task state) {
        user.setActualState(state);
    }

    public String getClientEmail(){
        return user.getEmail();
    }

    public BigInteger getClientPass(){
        return user.getPassword();
    }
}



