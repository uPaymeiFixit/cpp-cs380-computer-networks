import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

public final class PhysLayerClient {
  public static void main (String[] args) throws IOException, UnknownHostException {
    Socket socket = new Socket("18.221.102.182", 38002);
    System.out.println("Connected to server.");

    InputStream input_stream = socket.getInputStream();
    OutputStream output_stream = socket.getOutputStream();

    double baseline = calculateBaseline(input_stream, 64);
    char[] bits = readBits(input_stream, 320, baseline);
    decodeNRZI(bits);
    byte[] bytes = decode5B4B(new String(bits));
    checkBytes(bytes, input_stream, output_stream);
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
  public static char[] readBits (InputStream input_stream, int number_bits, double baseline) throws IOException {
    char[] bits = new char[number_bits];
    for (int i = 0; i < number_bits; i++) {
      bits[i] = input_stream.read() > baseline ? '1' : '0';
    }
    return bits;
  }

  // Decode an array of bits encoded as NRZI
  public static void decodeNRZI (char[] bits) {
    char last = '0';
    for (int i = 0; i < bits.length; i++) {
      char temp = bits[i];
      bits[i] = last == bits[i] ? '0' : '1';
      last = temp;
    }
  }

  // Decode an array of bits encoded as 5B/4B
  public static byte[] decode5B4B (String bits) {
    byte[] bytes = new byte[bits.length() / 10];
    Hashtable<String, String> table = new Hashtable<String, String>();
		table.put("11110", "0000");
		table.put("01001", "0001");
		table.put("10100", "0010");
		table.put("10101", "0011");
		table.put("01010", "0100");
		table.put("01011", "0101");
		table.put("01110", "0110");
		table.put("01111", "0111");
		table.put("10010", "1000");
		table.put("10011", "1001");
		table.put("10110", "1010");
		table.put("10111", "1011");
		table.put("11010", "1100");
		table.put("11011", "1101");
		table.put("11100", "1110");
		table.put("11101", "1111");

    System.out.print("Received 32 bytes: ");

    // Decrypt with 5B/4B table
    for (int i = 0; i < bits.length(); i += 10) {
      String first_half = table.get(bits.substring(i, i + 5));
      String second_half = table.get(bits.substring(i + 5, i + 10));
      bytes[i / 10] = (byte) Integer.parseInt(first_half + second_half, 2);
      System.out.printf("%X", bytes[i / 10]);
    }
    System.out.println();
    return bytes;
  }

  // Confirm with the server that we have decoded the message properly
  public static void checkBytes (byte[] bytes, InputStream input_stream, OutputStream output_stream) throws IOException {
    output_stream.write(bytes);
    if (input_stream.read() == 1) {
      System.out.println("Response good.");
    } else {
      System.out.println("Response bad.");
    }
  }

}
