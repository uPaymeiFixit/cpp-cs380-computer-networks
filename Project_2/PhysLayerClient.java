import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public final class PhysLayerClient {
  public static void main (String[] args) throws Exception {
    Socket socket = connect("18.221.102.182", 38002);
    InputStream input_stream = socket.getInputStream();
    double baseline = calculateBaseline(input_stream, 64);
    boolean[] bits = readBits(input_stream, 320, baseline);
    decodeNRZI(bits);
    byte[] bytes = decode5B4B(bits);
    checkBytes(bytes, socket, input_stream);
  }

  // Connect to server and return input stream
  public static Socket connect (String ip_address, int port) throws IOException, UnknownHostException {
    Socket socket = new Socket(ip_address, port);
    System.out.println("Connected to server.");
    return socket;
  }

  // Establish baseline by reading 64 values and averaging them
  public static double calculateBaseline (InputStream input_stream, int preamble_length) throws IOException {
    double baseline = 0.0;
    for (int i = 0; i < preamble_length; i++) {
      baseline += input_stream.read();
    }
    baseline /= preamble_length;
    System.out.printf("Baseline established from preamble: %.2f\n", baseline);
    return baseline;
  }

  // Read 32 bytes from server encoded as 5B/4B with NRZI
  public static boolean[] readBits (InputStream input_stream, int number_bits, double baseline) throws IOException {
    boolean[] bits = new boolean[320];
    for (int i = 0; i < bits.length; i++) {
      bits[i] = input_stream.read() > baseline;
    }
    return bits;
  }

  // Decode an array of bits encoded as NRZI
  public static void decodeNRZI (boolean[] bits) {
    boolean last = false;
    for (int i = 0; i < bits.length; i++) {
      boolean temp = bits[i];
      bits[i] = last != bits[i];
      last = temp;
    }
  }

  // Decode an array of bits encoded as 5B/4B
  public static byte[] decode5B4B (boolean[] bits) {
    byte[] bytes = new byte[bits.length / 10];
    byte[] table = {
      0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F,
      0x7F, 0x01, 0x04, 0x05, 0x7F, 0x7F, 0x06, 0x07,
      0x7F, 0x7F, 0x08, 0x09, 0x02, 0x03, 0x0A, 0x0B,
      0x7F, 0x7F, 0x0C, 0x0D, 0x0E, 0x0F, 0x00, 0x7F
    };

    // Convert bits array to String
    String bits_string = "";
    for (boolean bit : bits) {
      bits_string += bit ? '1' : '0';
    }

    System.out.print("Received 32 bytes: ");

    // Decrypt with 5B/4B table
    for (int i = 0; i < bits.length; i += 10) {
      byte first_half = table[Integer.parseInt(bits_string.substring(i, i + 4), 2)];
      byte second_half = table[Integer.parseInt(bits_string.substring(i + 4, i + 9), 2)];

      bytes[i / 10] = (byte) (first_half << 4 | second_half);
      System.out.printf("%X", bytes[i / 10]);
    }
    System.out.println();
    return bytes;
  }

  // Confirm with the server that we have decoded the message properly
  public static void checkBytes (byte[] bytes, Socket socket, InputStream input_stream) throws IOException {
    OutputStream output_stream = socket.getOutputStream();
    output_stream.write(bytes);
    if (input_stream.read() == 1) {
      System.out.println("Response good.");
    } else {
      System.out.println("Response bad.");
    }
  }

}
