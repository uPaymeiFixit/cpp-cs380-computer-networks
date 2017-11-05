import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class IPPacket {
  public static byte ip_version;
  public static byte protocol;
  public static byte hop_limit;
  public static String source_address;
  public static String destination_address;

  public static byte[] generatePacket (short data_length) {
    byte packet[] = new byte[40 + data_length];

    // IP version
    packet[0] = (byte) (ip_version << 4);

    // Payload length
    packet[4] = (byte) (data_length >>> 8);
    packet[5] = (byte) data_length;

    // Next header
    packet[6] = protocol;

    // Hop limit
    packet[7] = hop_limit;


    // Source address
    String[] source_array = source_address.split("\\.");
    packet[18] = (byte) 255;
    packet[19] = (byte) 255;
    packet[20] = (byte) Integer.parseInt(source_array[0]);
    packet[21] = (byte) Integer.parseInt(source_array[1]);
    packet[22] = (byte) Integer.parseInt(source_array[2]);
    packet[23] = (byte) Integer.parseInt(source_array[3]);

    // Destination address
    String[] destination_array = destination_address.split("\\.");
    packet[34] = (byte) 255;
    packet[35] = (byte) 255;
    packet[36] = (byte) Integer.parseInt(destination_array[0]);
    packet[37] = (byte) Integer.parseInt(destination_array[1]);
    packet[38] = (byte) Integer.parseInt(destination_array[2]);
    packet[39] = (byte) Integer.parseInt(destination_array[3]);

    return packet;
  }

}

public class Ipv6Client {

  public static void main (String[] args) throws IOException, UnknownHostException {
    Socket socket = new Socket("18.221.102.182", 38004);
    OutputStream output_stream = socket.getOutputStream();
    InputStream input_stream = socket.getInputStream();

    // Configure packet
    IPPacket p = new IPPacket();
    p.ip_version = 6;
    p.protocol = 17; // UDP
    p.hop_limit = 20;
    p.source_address = "127.0.0.1";
    p.destination_address = socket.getInetAddress().getHostAddress();

    for (byte i = 1; i <= 12; i++) {
      short data_length = (short) Math.pow(2, i);
      System.out.printf("data length: %d\n", data_length);

      byte[] packet = p.generatePacket(data_length);

      // Write to Server
      for (byte b : packet) {
        output_stream.write(b);
      }

      // Get response from Server
      System.out.printf("Response: 0x");
      for (byte j = 0; j < 4; j++) {
        System.out.printf("%X", input_stream.read());
      }
      System.out.println("\n");

    }

    socket.close();
  }

}
