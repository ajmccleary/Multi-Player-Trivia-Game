package Game;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Scanner;

public class GameClient {
    //instance variables
    private ClientWindow window;

    //constructor method
    public GameClient () {
        File file = new File("ipConfig.txt");
        Scanner scan = null;
        try {
            scan = new Scanner(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String ipPort = scan.nextLine();
        String ip = ipPort.split(" ")[0];
        System.out.println(ip);
        //initialize game window
        this.window = new ClientWindow(ip, 8001);
    }

    public static void main (String args[]) {
        //initialize game client
        GameClient gc = new GameClient();
    }
}