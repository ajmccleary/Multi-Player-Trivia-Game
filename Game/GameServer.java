package game;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    //static variables
    private static GameServer gs;
    private static int portNum = 8001;
    private static DatagramSocket buzzerSocket; //UDP buzzer socket
    private static ServerSocket gameSocket; //TCP game socket
    private static String[] questions;
    
    //instance variables
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<String> buzzerQueue = new ConcurrentLinkedQueue<String>();
    private HashMap<String, Integer> clients = new HashMap<String, Integer>(); //ASK - is this fine for ClientID requirment?
    private int questionNumber = 0;
    private boolean shutdownFlag, timerEndedFlag = false;
    
    //constructor method
    public GameServer() {
        //connect to specified socket for UDP and TCP connections
        try {
            //port num 8000 for buzzer
            GameServer.buzzerSocket = new DatagramSocket(portNum - 1);

            //port num 8001 for game
            GameServer.gameSocket = new ServerSocket(portNum);

        } catch (SocketException e) {
            System.out.println("Failed to create UDP socket. Reason " + e.getMessage());
            System.exit(1);

        } catch (IOException e) {
            System.out.println("Failed to create TCP socket. Reason " + e.getMessage());
            System.exit(1);
        }

        // Getting size of config file
        File configFile = new File("questions.txt");
        int size = 0;
        try (Scanner fileScan = new Scanner(configFile)) {
            while (fileScan.hasNextLine()) {
                size++;
                fileScan.nextLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }

        // Create an array to hold the questions
        questions = new String[size]; 

        List<String> options = new ArrayList<>();

        int index = 0;
        File questionFile = new File("questions.txt");
        try (Scanner fileScan = new Scanner(questionFile)) {
            while (fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" \\| ");

                questions[index] = parts[0];
                for (int i = 1; i <= 4; i++) {
                    // options[i - 1] = parts[i];

                    options.add(parts[i]);
                }
                index++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }
    }

    //send question thread method (uses TCP)
    public void clientThread (Socket clientSocket) {
        //store clientID
        String clientID = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        //periodically blast question and question data to client
        try (OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream()) {
            while (!gs.shutdownFlag) {
                out.write(1);

                //if question timer ends
                if (gs.timerEndedFlag) {
                    synchronized (gs.buzzerQueue) {
                        //check if top of queue equals client ID
                        if (gs.buzzerQueue.peek().equals(clientID)) {
                            //send ack message to winner
                            out.write("ack".getBytes());
                            
                            //remove winner from queue - DEV actually do the logic for letting them answer here/receiving the answer here
                            gs.buzzerQueue.poll(); 

                            //initialize holder for winner response
                            byte[] response = new byte[1024];

                            //timer runs ten seconds - ASK is this fine
                            Thread.sleep(10000);

                            //get value of current score
                            int currentScore = clients.get(clientID);

                            //if no response received
                            if (in.read(response) == -1)
                                //decrement currentScore
                                currentScore -= 20;

                            else if (response.toString().equals("correctAnswer")) { //DEV implement this
                                //send "correct" message to client
                                out.write("correct".getBytes());

                                //increment currentScore
                                currentScore += 10;
                            } else {
                                //send "wrong" message to client
                                out.write("wrong".getBytes());

                                //decrement currentScore
                                currentScore -= 10;
                            }

                            //update current score
                            clients.put(clientID, currentScore);

                        } else { //client not on top of queue
                            //send negative ack message
                            out.write("negative-ack".getBytes());
                        }
                    }
                }
            }
        } catch (IOException e) {
            //print error
            System.err.println("Communication error with client " + clientID + ": " + e.getMessage());

            //remove client from queue upon disconnect
            gs.buzzerQueue.remove(clientID);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    //detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        while (!gs.shutdownFlag) {
            try {
                //receive prcoess and store data from client attempting to connect
                Socket clientSocket = gameSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();

                //initialize score to 0 (value) and tie to clientID (key)
                clients.put(clientIP + ":" + clientPort, 0);

                //create a new thread with the client's information to send questions to client
                gs.executorService.submit(() -> gs.clientThread(clientSocket));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //monitor buzzer from all clients (UDP)
    public void buzzerHandler() {
        while (!shutdownFlag) {
            //initialize reply packet
            byte[] replyData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(replyData, replyData.length);

            //use UDP socket
            try {
                buzzerSocket.receive(replyPacket);
            } catch (IOException e) {
                System.out.println("Failed to connect socket. Reason " + e.getMessage());
            }

            //add replies to queue in order received
            gs.buzzerQueue.add(replyPacket.getAddress().getHostAddress() + ":" + replyPacket.getPort());

            //queue is then handled in seperate thread clientThread
        }
    }
    
    //main
    public static void main (String args[]) { //hi zak (and not fahim) - jjguy
        //print start message
        System.out.println("Game Server Starting...");

        //initialize game server object
        GameServer.gs = new GameServer();

        //initialize thread pool
        gs.executorService = Executors.newFixedThreadPool(5);

        //run threads
        gs.executorService.submit(() -> gs.buzzerHandler());

        gs.executorService.submit(() -> gs.listenThread());

        //gameplay loop
        while (gs.questionNumber <= 20) {
            //game logic

            //if question has progressed, increment
            //base on timer
            //gs.questionNumber++;
        }

        //if questions complete
        gs.shutdownFlag = true;
    }
}
