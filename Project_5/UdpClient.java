import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

class IPv4Packet {
  public static int ip_version = 4;
  public static int header_length = 5;
  public static int flag = 64; // No fragmentation
  public static int time_to_live = 50;
  public static int protocol = 17; // UDP
  public static String source_address = "127.0.0.1";
  public static String destination_address = "127.0.0.1";
  public static byte[] data;

  public static byte[] generatePacket () {
    byte packet[] = new byte[20 + data.length];

    packet[0] = (byte) ((ip_version << 4) | header_length);
    packet[2] = (byte) ((20 + data.length) >> 8); // Total Length (Upper)
    packet[3] = (byte) (20 + data.length); // Total Length (Lower)
    packet[6] = (byte) flag;
    packet[8] = (byte) time_to_live;
    packet[9] = (byte) protocol;

    // [12..15] Source Address
    String[] s = source_address.split("\\.");
    for (int j = 0; j < s.length; j++) {
      int value = Integer.valueOf(s[j]);
      packet[j + 12] = (byte) value;
    }

    // [16..19] Destination Address
    String[] d = destination_address.split("\\.");
    for (int j = 0; j < d.length; j++) {
      int value = Integer.valueOf(d[j]);
      packet[j + 16] = (byte) value;
    }

    short checksum_value = checksum(packet);
    packet[10] = (byte) (checksum_value >> 8); // upper
    packet[11] = (byte) checksum_value; // lower

    // [20..n] Data (Payload)
    for (int i = 0; i < data.length; i++) {
      packet[20 + i] = data[i];
    }

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

class UDPPacket {
  public static int source_port;
  public static int destination_port;
  public static byte[] data;

  public static String destination_address; // For pseudo header
  public static String source_address = "127.0.0.1"; // For pseudo header

  public static byte[] generatePacket () {
    byte packet[] = new byte[8 + data.length];

    packet[0] = (byte) (source_port >> 8);
    packet[1] = (byte) source_port;
    packet[2] = (byte) (destination_port >> 8);
    packet[3] = (byte) destination_port;
    packet[4] = (byte) (data.length >> 8);
    packet[5] = (byte) (data.length);

    short checksum_value = checksum(generatePseudoHeader(packet));
    packet[6] = (byte) (checksum_value >> 8); // upper
    packet[7] = (byte) checksum_value; // lower

    return packet;
  }

  private static byte[] generatePseudoHeader (byte[] packet){
    byte header[] = new byte[12 + packet.length];
    
    // [0..3] Source Address
    String[] s = source_address.split("\\.");
    for (int i = 0; i < s.length; i++) {
      int value = Integer.valueOf(s[i]);
      header[i] = (byte) value;
    }

    // [4..7] Destination Address
    String[] d = destination_address.split("\\.");
    for (int i = 0; i < d.length; i++) {
      int value = Integer.valueOf(d[i]);
      header[i + 4] = (byte) value;
    }

    header[9] = (byte) 17; // Protocol
    header[10] = packet[4]; // Length
    header[11] = packet[5]; // Length

    // [12..n] Datagram
    for (int i = 0; i < packet.length; i++) {
      header[12 + i] = packet[i];
    }

    return header;
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

    int destination_port = handshake(socket, output_stream, input_stream);
    udp_stream(socket, output_stream, input_stream, destination_port);

    socket.close();
  }

  public static int handshake (Socket socket, OutputStream output_stream, InputStream input_stream) throws IOException {
    byte data[] = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};

    // Packet configuration
    IPv4Packet ipv4_packet = new IPv4Packet();
    ipv4_packet.destination_address = socket.getInetAddress().getHostAddress();
    ipv4_packet.data = data;
    
    // Send handshake to server
    output_stream.write(ipv4_packet.generatePacket());

    // Get handshake response from server
    System.out.printf("Handshake response: 0x");
    for (byte j = 0; j < 4; j++) {
      System.out.printf("%02X", input_stream.read());
    }

    // Return destination port from server
    int destination_port = (input_stream.read() << 8) | input_stream.read();
    System.out.printf("\nPort number received: %d\n", destination_port);
    return destination_port;
  }

  public static void udp_stream (Socket socket, OutputStream output_stream, InputStream input_stream, int destination_port) throws IOException {
    // Generate random source port
    int source_port = (int) (Math.random() * 65534);



    // Packet configuration
    UDPPacket udp_packet = new UDPPacket();
    udp_packet.destination_port = destination_port;
    udp_packet.source_port = source_port;
    udp_packet.destination_address = socket.getInetAddress().getHostAddress();

    IPv4Packet ipv4_packet = new IPv4Packet();
    ipv4_packet.destination_address = socket.getInetAddress().getHostAddress(); 


    float round_time_trip_total = 0;

    // Begin sending data to destination port
    for (int i = 2; i <= 4096; i*=2) {
      System.out.printf("\nSending packet with %d bytes of data\n", i);

      udp_packet.data = new byte[i];
      ipv4_packet.data = udp_packet.generatePacket();

      // Send packet to server
      output_stream.write(ipv4_packet.generatePacket());

      long round_time_trip = -System.currentTimeMillis();

      // Get response from Server
      System.out.printf("Response: 0x");
      for (byte j = 0; j < 4; j++) {
        System.out.printf("%02X", input_stream.read());
      }

      round_time_trip += System.currentTimeMillis();
      System.out.printf("\nRTT: %dms\n", round_time_trip);
      round_time_trip_total += round_time_trip;

    }

    System.out.printf("\nAverage RTT: %.2fms\n", round_time_trip_total / 12);

  }

}
