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

  private ObjectOutputStream out;

  private int next_sequence_number = -1;
  private Key session_key;
  private String file_path;
  private int total_chunks;
  private FileOutputStream file_output_stream;


  public FileTransfer_server (String private_key_file, int port) throws Exception {
    ServerSocket server_socket = new ServerSocket(port);
    while (true) {
      Socket socket = server_socket.accept();
      out = new ObjectOutputStream(socket.getOutputStream());
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

      while (true) {
        // Message message;
        try {
          Message message = (Message) in.readObject();
          handleMessage(message, private_key_file);
        } catch (Exception e) {
          break;
        }
      }
    }
  }

  void handleMessage(Message message, String private_key_file) throws Exception {
    switch (message.getType()) {
      case DISCONNECT:
        handleMessage((DisconnectMessage) message);
        break;
      case START:
        handleMessage((StartMessage) message, private_key_file);
        break;
      case STOP:
        handleMessage((StopMessage) message);
        break;
      case CHUNK:
        handleMessage((Chunk) message);
        break; 
    }
  }

  // 1. If the client sends a DisconnectMessage, the server should close the connection 
  //    and wait for a new one.
  void handleMessage (DisconnectMessage message) {
    next_sequence_number = -1;
  }

  // 2. If the client sends a StartMessage, the server should prepare for a file 
  //    transfer based on the information in the message. It should then respond to 
  //    the client with an AckMessage with sequence number 0. If the server is unable 
  //    to begin the file transfer it should respond with an AckMessage with sequence 
  //    number -1. 
  //
  //    The preparation for file transfer includes decrypting the session 
  //    key passed by the client. To do this, the session key was sent in the 
  //    StartMessage encrypted with the server’s public key. The server should decrypt 
  //    this with its private key to an instance of Key. Use Cipher.UNWRAP MOD
  void handleMessage (StartMessage message, String private_key_file) throws Exception {
    this.session_key = decryptSessionKey(private_key_file, message);
    this.file_path = message.getFile();
    this.total_chunks = (int) Math.ceil(((double) message.getSize()) / message.getChunkSize());
    this.next_sequence_number = 0;
    int status = setupFile();
    // [...] respond to the client with an AckMessage with sequence number 0. [...]
    this.out.writeObject(new AckMessage(status));
  }

  // 2. [...] decrypting the session key passed by the client [...]
  Key decryptSessionKey (String private_key_file, StartMessage message) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.UNWRAP_MODE, getPrivateKey(private_key_file));
    return cipher.unwrap(message.getEncryptedKey(), "AES", Cipher.SECRET_KEY);
  }

  PrivateKey getPrivateKey (String private_key_file) throws Exception {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(private_key_file));
    PrivateKey key = (PrivateKey) in.readObject();
    in.close();
    return key;
  }

  int setupFile () throws Exception {
    File file = new File(this.file_path);
    String new_file_path = this.file_path;
    int i = 2;
    while (file.exists()) {
      new_file_path = this.file_path.replace(".", i++ + ".");
      file = new File(new_file_path);
    }
    this.file_path = new_file_path;
    this.file_output_stream = new FileOutputStream(file);

    return 0;
  }

  // 3. If the client sends a StopMessage, the server should discard the associated
  //    file transfer and respond with an AckMessage with sequence number -1.
  void handleMessage(StopMessage message) throws Exception {
    // [...] respond with an AckMessage with sequence number -1 [...]
    this.out.writeObject(new AckMessage(-1));
  }

  // 4. If the client sends a Chunk and the server has initiated the file transfer,
  //    it must handle the Chunk with the following steps:
  void handleMessage(Chunk message) throws Exception {
    // (a) The Chunk’s sequence number must be the next expected sequence number by 
    //     the server
    if (verifySequenceNumber(message)) {
      // (b) If so, the server should decrypt the data stored in the Chunk using the
      //     session key from the transfer initialization step.
      byte[] chunk = decryptChunk(message.getData(), this.session_key);
      // (c) Next, the server should calculate the CRC32 value for the decrypted data
      //     and compare it with the CRC32 value included in the chunk.
      if (validateChecksum(chunk, message)) {
        // (d) If these values match and the sequence number of the chunk is the next 
        //     expected sequence number, the server should accept the chunk by storing
        //     the data and incrementing the next expected sequence number.
        acceptChunk(chunk);
        // (e) The server should then respond with an AckMessage with sequence number
        //     of the next expected chunk.
        this.out.writeObject(new AckMessage(this.next_sequence_number));
      }
    }
  }

  // 4. (a) The Chunk’s sequence number must be the next expected sequence number by 
  //        the server
  boolean verifySequenceNumber (Chunk message) throws Exception {
    if (message.getSeq() != this.next_sequence_number) {
      this.out.writeObject(new AckMessage(this.next_sequence_number));
      return false;
    }
    return true;
  }

  // 4. (b) If so, the server should decrypt the data stored in the Chunk using the
  //        session key from the transfer initialization step.
  byte[] decryptChunk (byte[] chunk, Key session_key) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, session_key);
    return cipher.doFinal(chunk);
  }

  // 4. (c) Next, the server should calculate the CRC32 value for the decrypted data
  //        and compare it with the CRC32 value included in the chunk.
  boolean validateChecksum (byte[] chunk, Chunk message) throws Exception {
    CRC32 checksum = new CRC32();
    checksum.update(chunk);

    if (message.getCrc() != (int) checksum.getValue()) {
      this.out.writeObject(new AckMessage(this.next_sequence_number));
      return false;
    }
    return true;
  }

  // 4. (d) If these values match and the sequence number of the chunk is the next 
  //     expected sequence number, the server should accept the chunk by storing
  //     the data and incrementing the next expected sequence number.
  void acceptChunk (byte[] chunk) throws Exception {
    this.next_sequence_number++;
    this.file_output_stream.write(chunk);
    System.out.printf("Chunk received [%d/%d].\n", this.next_sequence_number, this.total_chunks);
    if (this.next_sequence_number == this.total_chunks) {
      System.out.println("Transfer complete.");
      System.out.printf("Output path: %s\n\n", this.file_path);
    }
  }

}
