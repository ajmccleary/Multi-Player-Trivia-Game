package game;

import java.io.IOException;
import java.io.PrintWriter;
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
        Socket socket = new Socket();

        PrintWriter writer;
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("fr");

            // Establishes a connection
           socket.connect(new InetSocketAddress("127.0.0.1", 8001));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main (String args[]) {
        GameClient gc = new GameClient();
    }
}
