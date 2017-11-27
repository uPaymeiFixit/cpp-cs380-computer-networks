import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.PrivateKey;
import java.util.zip.CRC32;
import javax.crypto.Cipher;

public class FileTransfer_server {

  private Key key;
  private long size;
  private String file;

  public FileTransfer_server (String private_key_file, int port) throws Exception {
    // Set up server
    ServerSocket server_socket = new ServerSocket(port);
    Socket socket = server_socket.accept();
    
    // Read message from client
    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

    while (true) {
      Message response = (Message) in.readObject();

      // Handle client message
      switch (response.getType()) {
        case DISCONNECT:
          socket.close();
          server_socket.close();
          break;
        case STOP:
          out.writeObject(new AckMessage(-1));
          break;
        case START:
          start((StartMessage) response, private_key_file, out);
          break;
        case CHUNK:
          chunk((Chunk) response, out);
          break;
      }
    }

  }

  void start (StartMessage response, String private_key_file, ObjectOutputStream out) throws Exception {
    this.file = response.getFile();
    this.size = response.getSize();
    byte[] retrieved_file = new byte[(int) size];
    long chunk_size = response.getChunkSize();
    long total_size = this.size;
    this.size = total_size / chunk_size;

    if (total_size == 0 || (total_size - this.size) > 0) {
      this.size++;
    }

    byte[] encrypted_key = response.getEncryptedKey();
    PrivateKey private_key = getPrivateKey(private_key_file);

    Cipher c = Cipher.getInstance("RSA");
    c.init(Cipher.UNWRAP_MODE, private_key);
    this.key = c.unwrap(encrypted_key, "AES", Cipher.SECRET_KEY);
    out.writeObject(new AckMessage(0));
  }

  PrivateKey getPrivateKey (String private_key_file) throws Exception {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(private_key_file));
    PrivateKey key = (PrivateKey) in.readObject();
    in.close();
    return key;
  }

  void chunk (Chunk response, ObjectOutputStream out) throws Exception {
    int expected_sequence_number = 0;
    if (response.getSeq() != expected_sequence_number) {
      out.writeObject(new AckMessage(expected_sequence_number));
      throw new Exception("Unexpected sequence number");
    }

    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, this.key);
    byte[] decrypted = cipher.doFinal(response.getData());

    CRC32 crc = new CRC32();
    crc.reset();
    crc.update(decrypted);

    if (response.getCrc() != (int) crc.getValue()) {
      throw new Exception("Unexpected CRC32 value");
    }

    System.out.printf("Chunk received [%d/%d]\n", ++expected_sequence_number, this.size);
    out.writeObject(new AckMessage(expected_sequence_number));

    if (this.size != expected_sequence_number) {
      throw new Exception("Unexpected chunk size");
    }

    outputFile(decrypted);

    System.out.println("Output Path: " + this.file);
    System.out.println("File transferred, shutting down...");

  }

  private void outputFile (byte[] decrypted) throws Exception {
    String output_file_name = file.replace(".", "2.");
    File f = new File(output_file_name);
    FileOutputStream fos = new FileOutputStream(new File(output_file_name));
    fos.write(decrypted);
    fos.close();

  }

}
