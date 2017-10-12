import java.io.InputStream;
import java.net.Socket;

public final class PhysLayerClient {
  public static void main(String[] args) throws Exception {

    // Connet to server
    try (Socket socket = new Socket("18.221.102.182", 38002)) {
      System.out.println("Connected to server.");
      
      // Set up input stream
      InputStream input_stream = socket.getInputStream();

      // Establish baseline by reading 64 values and averaging them
      double baseline = 0.0;
      for (int i = 0; i < 64; i++) {
        baseline += input_stream.read();
      }
      baseline /= 64;
      System.out.printf("Baseline established from preamble: %.2f\n", baseline);

      boolean last_signal = false;
      for (int i = 0; i < 64; i++) {

        String josh = "0 ";
        String five_bits = "";
        for (int j = 0; j < 5; j++) {
          boolean signal = input_stream.read() > baseline;
          josh += signal ? "1 " : "0 ";
          five_bits += last_signal == signal ? "0 " : "1 ";
          last_signal = signal;
        }
        System.out.println(josh + "\n " + five_bits + "\n");
      }


// 0110 1010
// 6    10

// 01101010
// 106



System.out.println(Byte.parseByte("11111", 2));

      // while (input_stream.available() < 64) {}

      // byte[] preamble = new byte[64];
      // input_stream.read(preamble, 0, 64);

      // for (int i = 0; i < preamble.length; i++) {
      //   System.out.println(preamble[i]);
      // }

    }
  }
}
