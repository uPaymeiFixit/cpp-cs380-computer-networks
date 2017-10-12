import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.zip.CRC32;

public final class Ex2Client {
  public static void main (String[] args) throws IOException, UnknownHostException {
    Socket socket = new Socket("18.221.102.182", 38102);
    System.out.println("Connected to server.");

    InputStream input_stream = socket.getInputStream();
    OutputStream output_stream = socket.getOutputStream();

    byte[] bytes = readBytes(input_stream, 100);
    checkBytes(bytes, input_stream, output_stream);

    socket.close();
    System.out.println("Disconnected from server.");
  }

  // Read 32 bytes from server encoded as 5B/4B with NRZI
  public static byte[] readBytes (
    InputStream input_stream, int number_bytes) throws IOException {
    byte[] bytes = new byte[number_bytes];
    for (int i = 0; i < number_bytes; i++) {
      if (i % 10 == 0) {
        System.out.print("  ");
      }
      bytes[i] = (byte) (input_stream.read() << 4 | input_stream.read());
      System.out.printf("%02X", bytes[i]);
      if (i % 10 == 9) {
        System.out.println();
      }
    }
    return bytes;
  }

  // Confirm with the server that we have decoded the message properly
  public static void checkBytes (
    byte[] bytes, InputStream input_stream,
    OutputStream output_stream) throws IOException {

    CRC32 crc32 = new CRC32();
    crc32.update(bytes);

    long n = crc32.getValue();
    byte[] code = new byte[4];

    code[0] = (byte) ((n >> 24) & 0xFF);
    code[1] = (byte) ((n >> 16) & 0xFF);
    code[2] = (byte) ((n >> 8) & 0xFF);
    code[3] = (byte) (n & 0xFF);

    System.out.printf("Generated CRC32: %02X%02X%02X%02X.\n", 
      code[0], code[1], code[2], code[3]);

    output_stream.write(code);
    if (input_stream.read() == 1) {
      System.out.println("Response good.");
    } else {
      System.out.println("Response bad.");
    }
  }

}
