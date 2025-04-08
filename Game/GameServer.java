package Game;

import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    // static variables
    private static GameServer gs;
    private static int portNum = 8001;
    private static DatagramSocket buzzerSocket; // UDP buzzer socket
    private static ServerSocket gameSocket; // TCP game socket
    private static String[] questions; // 20 questions
    private static List<String> options, answers; // options is four per questions, answers is one per question

    // instance variables
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<String> buzzerQueue = new ConcurrentLinkedQueue<String>();
    private ConcurrentHashMap<String, Integer> clients = new ConcurrentHashMap<String, Integer>(); // clientID, ("[ip]:[port]"), and current score
    private ConcurrentHashMap<String, OutputStream> clientOutputStreams = new ConcurrentHashMap<>();
    private volatile int questionNumber = 0;
    private volatile boolean shutdownFlag, timerEndedFlag, nextQuestionFlag, questionSentFlag;
    private boolean negativeAckSent = false;
    private ConcurrentHashMap<String, Integer> killSwitch = new ConcurrentHashMap<String, Integer>(); // clientID ("[ip]:[port]") and number of times client has not polled in a row
    private static ConcurrentHashMap<String , String> clientNames = new ConcurrentHashMap<>();  // Map clientID to player name 
    private static int clientCounter = 0;   //Counter for assignig client

    //static helper class to hold buzzer data
    private static class BuzzerEntry implements Comparable<BuzzerEntry> {
        final String clientId;
        final Instant timestamp;
        final static PriorityQueue<BuzzerEntry> buzzerPriorityQueue = new PriorityQueue<>(); // priority queue to be ordered by time sent before adding to buzzer queue
        
        public BuzzerEntry(String clientId, Instant timestamp) {
            this.clientId = clientId;
            this.timestamp = timestamp;
        }
        
        public static synchronized void addToBuzzerQueue(BuzzerEntry entry) {
            buzzerPriorityQueue.add(entry);
            
            // Process the queue if we have all expected responses or after timeout
            if (buzzerPriorityQueue.size() >= gs.clients.size()) {
                processBuzzerQueue();
            }
        }
        
        private static synchronized void processBuzzerQueue() {
            while (!buzzerPriorityQueue.isEmpty()) {
                BuzzerEntry entry = buzzerPriorityQueue.poll();
                gs.buzzerQueue.add(entry.clientId);
            }
        }

        @Override
        public int compareTo(BuzzerEntry other) {
            return this.timestamp.compareTo(other.timestamp);
        }
    }

    // constructor method
    public GameServer() {
        this.shutdownFlag = false;
        this.timerEndedFlag = false;
        this.nextQuestionFlag = true;
        this.questionSentFlag = false;
        // connect to specified socket for UDP and TCP connections
        try {
            // port num 8000 for buzzer
            GameServer.buzzerSocket = new DatagramSocket(portNum - 1);

            // port num 8001 for game
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

        // initialize array to hold questions
        questions = new String[size];

        // initialize options to hold all options
        options = new ArrayList<>();

        // initialize answers to hold all answers
        answers = new ArrayList<>();

        int index = 0;
        File questionFile = new File("questions.txt");
        try (Scanner fileScan = new Scanner(questionFile)) {
            while (fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" \\| ");

                questions[index] = parts[0];
                for (int i = 1; i <= 5; i++) {
                    if (i <= 4)
                        options.add(parts[i]);
                    else
                        answers.add(parts[i]);
                }
                index++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }
    }

    // detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        while (!gs.shutdownFlag) {
            try {
                // receive prcoess and store data from client attempting to connect
                Socket clientSocket = gameSocket.accept();

                // create a new thread with the client's information to send messages to client
                gs.executorService.submit(() -> gs.clientThread(clientSocket));

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    // send question thread method (uses TCP)
    public void clientThread(Socket clientSocket) {
        String playerName = " Player " + (++clientCounter);
        // store clientID as "[ip]:[port]"
        String clientID = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();

        //Map the clientID to the player name 
        clientNames.put(clientID, playerName);

        // initialize score to 0 (value) and tie to clientID (key)
        clients.put(clientID, 0);

        //initialize kill switch for client
        killSwitch.put(clientID, 0);

        System.out.println("Client thread started: " + playerName  + " (" +  clientID + ")");

        // stall until next question is being sent to clients OR handle joining mid question
        while (true) {
            synchronized (gs) {
                if (!gs.questionSentFlag) break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        System.out.println("Client thread READY: " + clientID);

        // local variable to track sent question flag
        boolean localQuestionSentFlag = false;

        // initialize in and out streams for client socket
        try (OutputStream out = clientSocket.getOutputStream();
        InputStream in = clientSocket.getInputStream();){
            // add clients output stream to map of client output streams
            clientOutputStreams.put(clientID, out);

            //Send the assigned player name to the client
            out.write(("Your ID: " + playerName + "\n").getBytes());
            out.flush();

            while (!gs.shutdownFlag) {
                // update local next question flag to false to allow for next question to be sent
                if (!gs.questionSentFlag) {
                    // reset the localQuestionSentFlag to false
                    localQuestionSentFlag = false;

                    // wait for other threads to finish processing
                    Thread.sleep(1000);

                    //only one thread should run this
                    if (!gs.questionSentFlag)
                        // set global question sent flag to true to indicate local variable is reset
                        gs.questionSentFlag = true;
                }

                // run on one thread at a time
                synchronized (gs) {                   
                    //if local question sent flag is set to false
                    if(!localQuestionSentFlag) {
                        //send "next" message to client to indicate next question is coming
                        out.write("next\n".getBytes());

                        negativeAckSent = false;

                        int pollDuration = 15;
                        
                        int questionDuration = 20;

                        StringBuilder questionData = new StringBuilder(questions[gs.questionNumber]);
                        for (int j = 0; j < 4; j++) {

                            questionData.append(" ; ").append(options.get(j + (gs.questionNumber * 4)));
                        }
                        questionData.append(" ; ").append(pollDuration);
                        questionData.append("\n");
                        questionData.append(" ; ").append(questionDuration);
                        questionData.append("\n");
                        // Sends the questions and options to the client
                        out.write(questionData.toString().getBytes());
                        out.flush();

                        //kill switch logic
                        killSwitch.put(clientID, killSwitch.get(clientID) + 1);
                        if (killSwitch.get(clientID) > 3) {
                            // send kill switch message to client
                            out.write("remove\n".getBytes());
                            out.flush();
                            // remove client from clients map
                            clients.remove(clientID);
                            // remove client from kill switch map
                            killSwitch.remove(clientID);
                        }

                        //handle ending of game if there are no clients left
                        if (gs.clients.isEmpty()) {
                            //shut down if all clients disconnect
                            gs.shutdownFlag = true;
                            System.out.println("No clients connected. Ending game.");
                            System.exit(1);
                        }

                        // print message indicated questions have been sent
                        System.out.println("Sending questions to " + clientID);

                        // wait to allow other threads to register nextQuestionFlag before setting it back to false
                        Thread.sleep(1500);

                        //mark questions as having been sent
                        localQuestionSentFlag = true;

                        //reset next question flag
                        gs.nextQuestionFlag = false;
                    }
                }

                // if question timer ends
                if (gs.timerEndedFlag && !gs.nextQuestionFlag) {
                    // run on one thread at a time
                    synchronized (gs.buzzerQueue) {
                        // if queue is empty
                        if (gs.buzzerQueue.isEmpty()) {
                            System.out.println("No buzzer signal received within timeout period.");

                            // prevent infinite loop
                            gs.timerEndedFlag = false;

                            // set nextQuestionFlag to true to allow for next question to be sent
                            gs.nextQuestionFlag = true;

                            // move on to next iteration of loop
                            continue;

                        // check top of buzzer queue against current clientID of current clientThread
                        } else if (gs.buzzerQueue.peek().equals(clientID) && !gs.nextQuestionFlag) {
                            System.out.println(clientID + " was first in queue!");

                            // send ack message to winner
                            out.write("ack\n".getBytes()); // client knows it can now answer question
                            out.flush();

                            // remove winner from queue
                            gs.buzzerQueue.poll();

                            // initialize holder for winner response
                            byte[] response = new byte[1024];

                            // client stops waiting after ten seconds
                            clientSocket.setSoTimeout(10000);

                            // get value of current score
                            int currentScore = clients.get(clientID);

                            try {
                                // store response
                                int bytesRead = in.read(response); // blocks until response received or timeout

                                // if no response received
                                if (bytesRead <= 0) { // will pretty much never happen, but who knows
                                    // decrement currentScore
                                    currentScore -= 20;

                                    // send "no response" message to client
                                    out.write("no-response\n".getBytes());
                                    out.flush();

                                } else {
                                    // trim and store response string
                                    String responseString = new String(response, 0, bytesRead).trim();

                                    // if client answer received is equal to the correct answer stored for the
                                    // current question
                                    if (responseString.equals(answers.get(questionNumber))) {
                                        // send "correct" message to client
                                        out.write("correct\n".getBytes()); // client now knows its score increased
                                        out.flush();

                                        // increment currentScore
                                        currentScore += 10;

                                    } else {
                                        // send "wrong" message to client
                                        out.write("wrong\n".getBytes()); // client now knows its score decreased
                                        out.flush();

                                        // decrement currentScore
                                        currentScore -= 10;
                                    }
                                }
                            } catch (SocketTimeoutException e) {
                                // decrement currentScore
                                currentScore -= 20;

                                // send "no response" message to client
                                out.write("no-response\n".getBytes());
                                out.flush();
                            }

                            // print score updates
                            System.out.println(clientID + " score updated: " + currentScore);

                            // update current score for a given clientID
                            clients.put(clientID, currentScore);

                            synchronized (gs) {
                                // set next question flag to true
                                gs.nextQuestionFlag = true;
                            }

                        } else { // client not on top of queue
                            if (!negativeAckSent) {
                                // send negative ack message
                                out.write("negative-ack\n".getBytes()); // client now knows it was not first in queue
                                out.flush();
                                negativeAckSent = true;
                            }
                        }
                    }
                }
                Thread.sleep(50);
            }
        } catch (IOException e) {
            // print error
            System.err.println("Communication error with client " + clientID + ": " + e.getMessage());
            gs.clients.remove(clientID);
            gs.clientOutputStreams.remove(clientID);
            gs.buzzerQueue.remove(clientID);

            // remove client from queue upon disconnect
            gs.buzzerQueue.remove(clientID);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Closing client socket: " + clientID);
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    // monitor buzzer from all clients (UDP)
    public void UDPThread() {
        // set timeout for buzzer socket to 1 second
        try {
            buzzerSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (!gs.shutdownFlag) {
            // initialize reply packet
            byte[] incomingData = new byte[1024]; // buffer for incoming data
            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

            try {
                // attempt to receive packet from buzzer
                buzzerSocket.receive(incomingPacket);

                // deserialize received packet
                BuzzerProtocol receivedPacket = null;
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingPacket.getData());
                        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                    receivedPacket = (BuzzerProtocol) objectInputStream.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                //ensure packet is not null
                if (receivedPacket != null) {     
                    // create buzzID to store received socket information
                    String buzzerID = incomingPacket.getAddress().getHostAddress() + ":" + receivedPacket.getPort();
                    
                    // Create a wrapper object to store both packet and timestamp
                    BuzzerEntry entry = new BuzzerEntry(buzzerID, receivedPacket.getTimeSent());
                    
                    // Add to a temporary priority queue for ordering
                    BuzzerEntry.addToBuzzerQueue(entry);

                    //update kill switch for client
                    killSwitch.put(buzzerID, 0);
                }

            } catch (SocketTimeoutException e) {
                // timeout occurred, continue waiting for packets
                continue;

            } catch (IOException e) {
                System.err.println("Error receiving packet: " + e.getMessage());
            }

            // queue is then handled in seperate thread clientThreads
        }
    }

    // main
    public static void main(String args[]) { // hi zak (and not fahim) - jjguy
        // print start message
        System.out.println("Game Server Starting...");

        // initialize game server object
        GameServer.gs = new GameServer();

        // initialize thread pool
        gs.executorService = Executors.newFixedThreadPool(10); // enough threads for 8 players

        // run threads
        gs.executorService.submit(() -> gs.UDPThread());

        gs.executorService.submit(() -> gs.listenThread());

        // gameplay loop
        while (gs.questionNumber < questions.length) {
            // if there is more than one client, begin game
            if (gs.clients.size() > 0) {
                try {
                    System.out.println("\nStarting question " + (gs.questionNumber + 1) + " out of 20");

                    synchronized (gs) {
                        //reset flags at start of each question
                        gs.nextQuestionFlag = true;
                        gs.timerEndedFlag = false;
                        gs.questionSentFlag = false;

                        //clear buzzer queue and priority queue at start of each question
                        gs.buzzerQueue.clear();
                        BuzzerEntry.buzzerPriorityQueue.clear();
                    }

                    // timer for 20 seconds
                    Thread.sleep(15000);

                    // End question period
                    synchronized (gs) {
                        gs.timerEndedFlag = true;
                    }

                    // Wait for processing to complete
                    while (true) {
                        synchronized (gs) {
                            if (gs.nextQuestionFlag)
                                break;
                        }
                        Thread.sleep(1000);
                    }

                    synchronized (gs) {
                        //increment question number
                        gs.questionNumber++;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // winner logic
        if (!gs.shutdownFlag) {
            StringBuilder winnerMessage = new StringBuilder("Winner: ");
            int highestScore = Integer.MIN_VALUE;
            
            List<String> winners = new ArrayList<>();
            
            for (Map.Entry<String, Integer> entry : gs.clients.entrySet()) {
                int score = entry.getValue();
                
                if (score > highestScore) {
                    highestScore = score;
                    winners.clear();
                    winners.add(entry.getKey());
                } else if (score == highestScore) {
                    winners.add(entry.getKey());
                    
                }
            }
            // Append player names to the winner message
            for (String winner : winners) {
                String playerName = clientNames.get(winner);
                winnerMessage.append(playerName).append(" ");
            }
            winnerMessage.append("with a score of ").append(highestScore).append("!");

            // Broadcast the winner to all clients
            for (Map.Entry<String, OutputStream> entry : gs.clientOutputStreams.entrySet()) {
                try {
                    
                    OutputStream out = entry.getValue();
                    out.write((winnerMessage.toString() + "\n").getBytes());
                    out.flush();
                    
                } catch (IOException e) {
                    System.err.println("Error sending winner message to client " + entry.getKey() + ":" + e.getMessage());
                }
            }
            System.out.println(winnerMessage.toString());
        }

        // if game complete, kill all threads
        gs.shutdownFlag = true;
    }
}
