import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class EchoServer {
    
    private static volatile Socket socket;
    public static void main(String[] args) throws Exception {

        Runnable client = () -> {
            try {
                String address = socket.getInetAddress().getHostAddress();
                System.out.printf("Client connected: %s%n", address);
                OutputStream os = socket.getOutputStream();
                PrintStream out = new PrintStream(os, true, "UTF-8");
                out.printf("Hi %s, thanks for connecting!%n", address);
                out.flush();

                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

                while (true) {
                    String message = br.readLine();
                    if (message == null || message.trim().toLowerCase().equals("exit")) {
                        socket.close();
                        System.out.printf("Client disconnected: %s%n", address);
                        break;
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                System.err.print(e);
            }
        };

        try (ServerSocket serverSocket = new ServerSocket(22222)) {
            while (true) {
                socket = serverSocket.accept();
                new Thread(client).start();
            }
        }
    }
}
