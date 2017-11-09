import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class IPPacket {
  public static int ip_version;
  public static int header_length;
  public static int type_of_service;
  public static int data_length;
  public static int identification;
  public static int flag;
  public static int fragment_offset;
  public static int time_to_live;
  public static int protocol;
  public static int checksum;
  public static String source_address;
  public static String destination_address;

  public static byte[] generatePacket () {
    byte packet[] = new byte[20];

    packet[0] = (byte) ((ip_version << 4 & 0xF0) | (header_length & 0xF));
    packet[1] = (byte) type_of_service;
    packet[2] = (byte) (((20 + data_length) >> 8) & 0xFF); // Total Length (Upper)
    packet[3] = (byte) ((20 + data_length) & 0xFF); // Total Length (Lower)
    packet[4] = (byte) identification;
    packet[5] = (byte) identification;
    packet[6] = (byte) flag;
    packet[7] = (byte) fragment_offset;
    packet[8] = (byte) time_to_live;
    packet[9] = (byte) protocol;
    packet[10] = (byte) checksum;
    packet[11] = (byte) checksum;

    String[] s = source_address.split("\\.");
    for (int j = 0; j < s.length; j++) {
      int value = Integer.valueOf(s[j]);
      packet[j + 12] = (byte) value;
    }

    String[] d = destination_address.split("\\.");
    for (int j = 0; j < d.length; j++) {
      int value = Integer.valueOf(d[j]);
      packet[j + 16] = (byte) value;
    }

    short checksum_value = checksum(packet);
    packet[10] = (byte) (checksum_value >> 8 & 0xFF); // upper
    packet[11] = (byte) (checksum_value & 0xFF); // lower

    return packet;
  }

  public static short checksum(byte[] b) {
    int sum = 0;

    for (int i = 0; i < b.length; i += 2) {

      // Convert to 16 bit
      sum += ((b[i] << 8 & 0xFF00) | (b[i + 1] & 0xFF));

      if ((sum & 0xFFFF0000) != 0) {
        /* carry occurred. so wrap around */
        sum &= 0xFFFF;
        sum++;
      }
    }
    return (short) ~(sum & 0xFFFF);
  }

}

public class UdpClient {

  public static void main (String[] args) throws IOException, UnknownHostException {
    Socket socket = new Socket("18.221.102.182", 38005);

    OutputStream output_stream = socket.getOutputStream();
    InputStream input_stream = socket.getInputStream();
    InputStreamReader isr = new InputStreamReader(input_stream, "UTF-8");
    BufferedReader buffered_reader = new BufferedReader(isr);

    IPPacket p = new IPPacket();
    p.ip_version = 4;
    p.header_length = 5;
    p.type_of_service = 0;
    p.identification = 0;
    p.flag = 64;
    p.fragment_offset = 0;
    p.time_to_live = 50;
    p.protocol = 6;
    p.checksum = 0;
    p.source_address = "127.0.0.1";
    p.destination_address = socket.getInetAddress().getHostAddress();

    for (int i = 1; i <= 12; i++) {
      p.data_length = (int) Math.pow(2, i);
      System.out.printf("data length: %d\n", p.data_length);

      byte[] packet = p.generatePacket();

      // Write to Server
      for (byte b : packet) {
        output_stream.write(b);
      }

      // Write empty data
      for (int j = 0; j < p.data_length; j++) {
        output_stream.write(0);
      }

      System.out.printf("%s\n\n", buffered_reader.readLine());

    }

    socket.close();
  }

}
