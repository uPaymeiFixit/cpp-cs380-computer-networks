import java.security.PublicKey;
import java.net.Socket;
import java.util.Scanner;
import javax.crypto.KeyGenerator;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import java.security.Key;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileTransfer_client {
  public FileTransfer_client (String public_key_file, String host, int port) throws Exception {
    Socket socket = new Socket(host, port);

    Scanner keyboard = new Scanner(System.in);
    System.out.print("Enter filepath: ");
    String file_path = keyboard.nextLine();
    System.out.print("Enter chunk size: ");
    int chunk_size = Integer.parseInt(keyboard.nextLine());

    KeyGenerator key_gen = KeyGenerator.getInstance("AES");
    key_gen.init(128);

    Key session_key = key_gen.generateKey();
    Cipher c = Cipher.getInstance("RSA");
    c.init(Cipher.WRAP_MODE, getPublicKey(public_key_file));
    byte[] to_send = c.wrap(session_key);

    File file = new File(file_path);
    System.out.printf("Sending: %s\tFile Size: %d\n", file_path, file.length());
    byte[] file_store = new byte[(int) file.length()];
    FileInputStream file_input_stream = new FileInputStream(file);
    file_input_stream.read();

    int tip = (int) (file_store.length / chunk_size);
    byte[][] packaged_file = new byte[tip + 1][];
    int remainder = file_store.length - tip * chunk_size;

    for (int i = 0; i * chunk_size < file_store.length; i++) {
      if (tip == i) {
        packaged_file[i] = new byte[remainder];
        packaged_file[i] = Arrays.copyOfRange(file_store, i * chunk_size, i * chunk_size + remainder - 1);
      } else if (i == 0) {
        packaged_file[i] = new byte[chunk_size];
        packaged_file[i] = Arrays.copyOfRange(file_store, i * chunk_size, 1);
      } else {
        packaged_file[i] = new byte[chunk_size];
        packaged_file[i] = Arrays.copyOfRange(file_store, i * chunk_size, i * 2 * chunk_size - 1);
      }
    }


    StartMessage begin = new StartMessage(file_path, to_send, chunk_size);
    
    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
    out.writeObject(begin);

    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    for (int i = 0; i < tip + 2; i++) {
      Message received = (Message) in.readObject();

      if (received.getType() != MessageType.ACK) {
        throw new Exception("Unexpected response. Expected ACK");
      }

      AckMessage ack = (AckMessage) received;

      if (ack.getSeq() == packaged_file.length) {
        System.out.println("All chunks sent!");
      } else {
        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(packaged_file[ack.getSeq()]);

        c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, session_key);
        byte[] encrypted_data = c.doFinal(packaged_file[ack.getSeq()]);

        Chunk send_this = new Chunk(ack.getSeq(), encrypted_data, (int) crc.getValue());

        System.out.printf("Chunk transferred [%d/%d]\n", send_this.getSeq() + 1, packaged_file.length);
        out.writeObject(send_this);
      }
    }

  }

  PublicKey getPublicKey (String public_key_file) throws Exception {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(public_key_file));
    PublicKey key = (PublicKey) in.readObject();
    in.close();
    return key;
  }
}
