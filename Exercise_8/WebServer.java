import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class WebServer {
  public static void main (String[] args) throws Exception {
    ServerSocket server_socket = new ServerSocket(8080);
    while (true) {
      Socket socket = server_socket.accept();
      InputStream in = socket.getInputStream();
      InputStreamReader input_stream = new InputStreamReader(in, "UTF-8");
      BufferedReader buffered_reader = new BufferedReader(input_stream);

      String message = buffered_reader.readLine();
      String request = "";
      while (!message.equals("")) {
        request += message + "\n";
        message = buffered_reader.readLine();
      }

      OutputStream output_stream = socket.getOutputStream();
      PrintStream out = new PrintStream(output_stream, true, "UTF-8");

      System.out.println(request);
      out.printf("%s", generateResponse(request));
    }
  }

  static String generateResponse(String request) throws Exception {
    String file_path = getFilePath(request);
    File file = new File(file_path);
    return file.exists() ? create200(file_path) : create404();
  }

  static String create200 (String file_path) throws Exception {
    String file = fileToString(file_path);
    String response = "HTTP/1.1 200 OK\nContent-type: text/html\n"
                    + "Content-length: " + file.length() + "\n\n";
    return response + file;
  }

  static String create404 () throws Exception {
    String file = fileToString("www/404.html");
    String response = "HTTP/1.1 404 Not Found\nContent-type: text/html\n"
                    + "Content-length: " + file.length() + "\n\n";
    return response + file;
  }

  static String fileToString (String file_path) throws Exception {
    Scanner file_scanner = new Scanner(new FileInputStream(file_path));
    String file = "";
    while (file_scanner.hasNext()) {
      file += file_scanner.nextLine();
    }
    file_scanner.close();
    return file;
  }

  static String getFilePath(String request) throws Exception {
    if (!request.startsWith("GET ") || request.charAt(4) != '/') {
      throw new Exception("Unexpected request header:\n" + request);
    }
    return "www" + request.substring(4, request.indexOf(" HTTP/1.1"));
  }
}
