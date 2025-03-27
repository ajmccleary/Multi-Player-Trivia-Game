package game;

import java.net.*;

public class GameClient {
    //static variables
    private static DatagramSocket socket; //client socket
    private static int portNum;

    //instance variables
    private ClientWindow window;

    //constructor method
    public GameClient () {
        //connect to server
    }

    public static void main (String args[]) {
        GameClient gc = new GameClient();
    }
}
