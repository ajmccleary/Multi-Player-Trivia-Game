package game;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Scanner;
import java.util.Timer;
import javax.swing.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;

public class ClientWindow implements ActionListener {
	private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private JLabel timer;
	private JLabel score;
	private TimerTask clock;

	private JFrame window;

	private java.net.Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	private static SecureRandom random = new SecureRandom();

	// write setters and getters as you need

	public ClientWindow(String serverAddress, int port) {
		// try{

			// Connect to the server 
			// socket = new Socket(InetAddress.getByName(serverAddress), port);
			// in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// out = new PrintWriter(socket.getOutputStream(), true);

			JOptionPane.showMessageDialog(window, "This is a trivia game");
	
			window = new JFrame("Trivia");
			question = new JLabel("Waiting for the first question"); // represents the question
			window.add(question);
			question.setBounds(10, 5, 350, 100);
	
			options = new JRadioButton[4];
			optionGroup = new ButtonGroup();
			for (int index = 0; index < options.length; index++) {
				options[index] = new JRadioButton("Option " + (index + 1)); // represents an option
				// if a radio button is clicked, the event would be thrown to this class to
				// handle
				options[index].addActionListener(this);
				options[index].setBounds(10, 110 + (index * 20), 350, 20);
				window.add(options[index]);
				optionGroup.add(options[index]);
			}
			timer = new JLabel("TIMER"); // represents the countdown shown on the window
			timer.setBounds(250, 250, 100, 20);
			clock = new TimerCode(30); // represents clocked task that should run after X seconds
			Timer t = new Timer(); // event generator
			t.schedule(clock, 0, 1000); // clock is called every second
			window.add(timer);
	
			score = new JLabel("SCORE"); // represents the score
			score.setBounds(50, 250, 100, 20);
			window.add(score);
	
			poll = new JButton("Poll"); // button that use clicks/ like a buzzer
			poll.setBounds(10, 300, 100, 20);
			poll.addActionListener(this); // calls actionPerformed of this class
			window.add(poll);
	
			submit = new JButton("Submit"); // button to submit their answer
			submit.setBounds(200, 300, 100, 20);
			submit.addActionListener(this); // calls actionPerformed of this class
			window.add(submit);
	
			window.setSize(400, 400);
			window.setBounds(50, 50, 400, 400);
			window.setLayout(null);
			window.setVisible(true);
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setResizable(false);

			//Start listening for questions from the server
			// new Thread(this :: listenForQuestions).start();


		// } catch(IOException e ){
		// 	JOptionPane.showMessageDialog(null, "Error connecting to Server: "  + e.getMessage());
		// }
	}

	private void listenForQuestions(){
		try{
			String line;
			while((line = in.readLine()) != null){
				//Parse the question and options
				String[] parts = line.split(";");
				
				// Ensure the format is correct
				if(parts.length ==5){
					SwingUtilities.invokeLater(() -> {
						question.setText(parts[0]);	// Set the questions
						for(int i = 0; i < options.length; i++){
							options[i].setText(parts[i + 1]);	// Set the options
							options[i].setEnabled(true);	// Enable the options
						}
					});

				}
			}
		} catch (IOException e){
			JOptionPane.showMessageDialog(null, "Error receiving data from the server:" + e.getMessage());
		}
	}

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("You clicked " + e.getActionCommand());

		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();
		File file = new File("ipConfig.txt");
		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String[] parts = fileScanner.nextLine().split(" "); // read the first line and split by whitespace
		String serverIP = parts[0]; 
		int serverPort = Integer.parseInt(parts[1]) - 1; 
		switch (input) {
			case "Option 1":

				break;

			case "Option 2": 

				break;

			case "Option 3": 

				break;

			case "Option 4": 

				break;

			case "Poll":
				try {
					String message = "buzz";
					byte[] data = message.getBytes();
					DatagramSocket socket = new DatagramSocket(1001);
					DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIP), serverPort);
					socket.send(packet);
					System.out.println( "Buzz pressed, sending message to server...");
					socket.close();
				} catch (SocketException socketException) {
					System.out.println("Error in creating DatagramSocket: " + socketException.getMessage());
					return;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				break;

			case "Submit": 

				break;

			default:
				System.out.println("Incorrect Option");
		}

		// test code below to demo enable/disable components
		// DELETE THE CODE BELOW FROM HERE***
		if (poll.isEnabled()) {
			poll.setEnabled(false);
			submit.setEnabled(true);
		} else {
			poll.setEnabled(true);
			submit.setEnabled(false);
		}

		question.setText("Q2. This is another test problem " + random.nextInt());

		// you can also enable disable radio buttons
		// options[random.nextInt(4)].setEnabled(false);
		// options[random.nextInt(4)].setEnabled(true);
		// TILL HERE ***

	}

	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask {
		private int duration; // write setters and getters as you need

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {
			if (duration < 0) {
				timer.setText("Timer expired");
				window.repaint();
				this.cancel(); // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}

			if (duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);

			timer.setText(duration + "");
			duration--;
			window.repaint();
		}
	}

}
