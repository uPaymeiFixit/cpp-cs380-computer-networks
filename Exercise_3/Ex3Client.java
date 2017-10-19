import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public final class Ex3Client {
  public static void main (String[] args) throws IOException, UnknownHostException {
    Socket socket = new Socket("18.221.102.182", 38103);
    System.out.println("Connected to server.");

    InputStream input_stream = socket.getInputStream();
    OutputStream output_stream = socket.getOutputStream();

    // Get the number of bytes being sent from the server
    int number_bytes = input_stream.read();
    byte[] bytes = readBytes(input_stream, number_bytes);
    short check = checksum(bytes);
    checkBytes(check, input_stream, output_stream);
  }

  // Read 32 bytes from server encoded as 5B/4B with NRZI
  public static byte[] readBytes (
    InputStream input_stream, int number_bytes) throws IOException {

    System.out.printf("Reading %d bytes.\nData received:\n", number_bytes);
    byte[] bytes = new byte[number_bytes];
    for (int i = 0; i < number_bytes; i++) {
      if (i % 10 == 0) {
        System.out.print("  ");
      }
      bytes[i] = (byte) input_stream.read();
      System.out.printf("%02X", bytes[i]);
      if (i % 10 == 9) {
        System.out.println();
      }
    }
    System.out.println();
    return bytes;
  }

  // The C code for the algorithm is provided below:
  // u_short cksum(u_short *buf, int count)
  // {
  //   register u_long sum = 0;
  //   while (count--)
  //   {
  //     sum += *buf++;
  //     if (sum & 0xFFFF0000)
  //     {
  //       /* carry occurred. so wrap around */
  //       sum &= 0xFFFF;
  //       sum++;
  //     }
  //   }
  //   return ~(sum & 0xFFFF);
  // }

  public static short checksum(byte[] b) {
    int sum = 0;

    for (byte i : b) {
      sum += i;
      if ((sum & 0xFFFF0000) != 0) {
        /* carry occurred. so wrap around */
        sum &= 0xFFFF;
        sum++;
      }
    }
    return (short) ~(sum & 0xFFFF);
  }

  // Confirm with the server that we have decoded the message properly
  public static void checkBytes (
    short check, InputStream input_stream,
    OutputStream output_stream) throws IOException {

    // Split checksum up into 2 bytes
    byte[] code = new byte[2];
    code[0] = (byte) ((check >> 8) & 0xFF);
    code[1] = (byte) (check & 0xFF);

    System.out.printf("Checksum calculated: 0x%02X%02X.\n", code[0], code[1]);

    output_stream.write(code);
    if (input_stream.read() == 1) {
      System.out.println("Response good.");
    } else {
      System.out.println("Response bad.");
    }
  }

}
