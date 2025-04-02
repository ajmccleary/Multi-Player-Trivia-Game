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

        PrintWriter writer;
        try {
            //connect to server
            Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), 8001);
            ClientWindow gameWindow = new ClientWindow("127.0.0.1", 8000);


            System.out.println("guh");

            writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("fr");

            //make in
            //receive packet
            //process it
            //break it down
            //print the questions as one line and each option line by line 
            
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main (String args[]) {
        GameClient gc = new GameClient();
    }
}
