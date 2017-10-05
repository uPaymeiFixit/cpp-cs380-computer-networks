import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public final class ChatClient {

    private static volatile Socket socket;

    public static void main(String[] args) throws Exception {

        // Let up listener thread
        Runnable listen = () -> {

            // Set up input stream
            try {
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                
                // Listen for incoming messages
                while (true) {
                    String message = br.readLine();
                    if (message != null) {
                        System.out.println(message);
                    }
                } 
            } catch (IOException e) {
                System.err.print(e);
            }
        };

        // Get username
        Scanner userInput = new Scanner(System.in);
        System.out.print("Enter your username: ");
        
        try (Socket s = new Socket("18.221.102.182", 38001)) {
            socket = s;

            // Set up output stream
            OutputStream os = socket.getOutputStream();
            PrintStream out = new PrintStream(os, true, "UTF-8");

            // Send the server our username
            out.println(userInput.nextLine());

            // Start the listener thread
            new Thread(listen).start();

            // Get input from user
            while (userInput.hasNextLine()) {
                out.println(userInput.nextLine());
            }
        }
    }
}
