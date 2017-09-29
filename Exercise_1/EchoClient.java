
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public final class EchoClient {

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("localhost", 22222)) {
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            System.out.println(br.readLine());

            java.io.OutputStream os = socket.getOutputStream();
            java.io.PrintStream out = new java.io.PrintStream(os, true, "UTF-8");
            java.util.Scanner userInput = new java.util.Scanner(System.in);
            System.out.print("Client> ");
            while (userInput.hasNextLine()) {
                out.println(userInput.nextLine());
                String response = br.readLine();
                if (response == null) {
                    break;
                }
                System.out.println("Server> " + response);
                System.out.print("Client> ");
            }
        }
    }
}
