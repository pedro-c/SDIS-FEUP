package Controller;

import Client.Client;
import GUI.InterfaceView;

/**
 * Created by ines on 23/05/17.
 */

public class Controller {

    private Client client;
    private InterfaceView view;

    public Controller(InterfaceView view, Client client) {

        this.view = view;
        this.client = client;
    }
}
