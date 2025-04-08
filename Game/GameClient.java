package Game;

public class GameClient {
    //instance variables
    private ClientWindow window;

    //constructor method
    public GameClient () {
        //initialize game window
        this.window = new ClientWindow("127.0.0.1", 8001);
    }

    public static void main (String args[]) {
        //initialize game client
        GameClient gc = new GameClient();
    }
}