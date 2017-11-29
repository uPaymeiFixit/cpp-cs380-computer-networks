import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class FileTransfer_client {

  private ObjectOutputStream out;
  private ObjectInputStream in;

  public FileTransfer_client (String public_key_file, String host, int port) throws Exception {
    Socket socket = new Socket(host, port);
    out = new ObjectOutputStream(socket.getOutputStream());
    in = new ObjectInputStream(socket.getInputStream());
    
    do {
      // 1. Generate an AES session key
      SecretKey session_key = generateSessionKey();
      // 2. Encrypt the session key using the server's public key
      byte[] encrypted_session_key = encryptSessionKey(session_key, public_key_file);
      // 3. Prompt the user to enter the path for a file to transfer
      String file_path = askFilePath();
      // 4. If the path is valid, ask the user to enter the desired chunk size in bytes (default of 1024 bytes).    
      int chunk_size = askChunkSize();
      // 5. [...] send the server a StartMessage that contains the file name, length of the file in bytes, chunk size, and encrypted session key. [...]
      sendStartMessage(file_path, chunk_size, encrypted_session_key);
      // 6. [...]  send each chunk of the file in order. After each chunk, wait for the server to respond with the appropriate AckMessage [...]
      sendFile(new File(file_path), chunk_size, session_key);
      // 7. [...] the client can either begin a new file transfer or disconnect.
    } while (askRepeat());

    socket.close();
    out.close();
    in.close();

  }

  // 1. Generate an AES session key.
  SecretKey generateSessionKey () throws Exception {
    KeyGenerator key_gen = KeyGenerator.getInstance("AES");
    SecureRandom random = new SecureRandom();
    key_gen.init(random);
    return key_gen.generateKey();
  }

  // 2. Encrypt the session key using the server's public key
  byte[] encryptSessionKey (SecretKey session_key, String public_key_file) throws Exception {
    PublicKey public_key = getPublicKey(public_key_file);
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.WRAP_MODE, public_key);
    return cipher.wrap(session_key);
  }

  PublicKey getPublicKey (String public_key_file) throws Exception {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(public_key_file));
    PublicKey key = (PublicKey) in.readObject();
    in.close();
    return key;
  }

  // 3. Prompt the user to enter the path for a file to transfer
  String askFilePath () throws Exception {
    Scanner in = new Scanner(System.in);
    while (true) {
      System.out.print("Enter filepath: ");
      String file_path = in.nextLine();
      File file = new File(file_path);
      if (file.exists()) {
        return file_path;
      }
      System.out.println("%s not found. Try again.");
    }
  }

  // 4. If the path is valid, ask the user to enter the desired chunk size in bytes 
  //    (default of 1024 bytes).
  int askChunkSize () throws Exception {
    Scanner in = new Scanner(System.in);
    System.out.print("Enter chunk size [1024]: ");
    String input = in.nextLine();
    return input.length() == 0 ? 1024 : Integer.parseInt(input);
  }

  // 5. After accepting the path and chunk size, send the server a StartMessage that 
  //    contains the file name, length of the file in bytes, chunk size, and encrypted 
  //    session key.
  //
  //    The server should respond with an AckMessage with sequence number 0 if the
  //    transfer can proceed, otherwise the sequence number will be -1.
  void sendStartMessage (String file_path, int chunk_size, byte[] encrypted_session_key) throws Exception {
    File file = new File(file_path);
    System.out.printf("Sending: %s  File Size: %d\n", file_path, file.length());
    this.out.writeObject(new StartMessage(file_path, encrypted_session_key, chunk_size));

    Message response = (Message) this.in.readObject();
    if (response.getType() != MessageType.ACK) {
      throw new Exception("Expected ACK following StartMessage transmission.");
    }
    if (((AckMessage) response).getSeq() != 0) {
      throw new Exception("seq != 0. Cannot proceed.");
    }
  }

  // 6. The client should then send each chunk of the file in order. After each chunk, 
  //    wait for the server to respond with the appropriate AckMessage. The sequence 
  //    number in the ACK should be the number for the next expected chunk.
  //    
  //    For each chunk, the client must first read the data from the file and store in 
  //    an array based on the chunk size. It should then calculate the CRC32 value for 
  //    the chunk. Finally, encrypt the chunk data using the session key. Note that the
  //    CRC32 value is for the plaintext of the chunk, not the ciphertext.
  void sendFile (File file, int chunk_size, SecretKey session_key) throws Exception {
    FileInputStream file_input_stream = new FileInputStream(file);
    int sequence_number = 0;
    int total_chunks = (int) Math.ceil(((double) file.length()) / chunk_size);
    System.out.printf("Sending %d chunks.\n", total_chunks);
    while (total_chunks > sequence_number) {
      // [...] read the data from the file and store in an array based on the chunk size [...]
      byte[] chunk = readChunks(file_input_stream, chunk_size, file.length());
      // [...] calculate the CRC32 value for the chunk [...]
      long checksum_value = calculateChecksum(chunk);
      // [...] encrypt the chunk data using the session key [...]
      chunk = encryptChunks(session_key, chunk);
      // [...] send each chunk of the file in order [...]
      sequence_number = sendChunk(sequence_number, chunk, checksum_value);
      System.out.printf("Chunks completed [%d/%d].\n", sequence_number, total_chunks);
      
    }
    file_input_stream.close();
  }

  // 6. [...] read the data from the file and store in an array based on the chunk size [...]
  byte[] readChunks (FileInputStream file_input_stream, int chunk_size, long file_size) throws Exception {
    byte[] chunk = new byte[file_size > chunk_size ? chunk_size : (int) file_size];
    file_input_stream.read(chunk);
    return chunk;
  }

  // 6. [...] calculate the CRC32 value for the chunk [...]
  long calculateChecksum (byte[] chunk) {
    CRC32 checksum = new CRC32();
    checksum.update(chunk);
    return checksum.getValue();
  }

  // 6. [...] encrypt the chunk data using the session key [...]
  byte[] encryptChunks (SecretKey session_key, byte[] chunk) throws Exception {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, session_key);
    return cipher.doFinal(chunk);
  }

  // 6. [...] send each chunk of the file in order [...]
  int sendChunk (int sequence_number, byte[] chunk, long checksum_value) throws Exception {
    Chunk message = new Chunk(sequence_number, chunk, (int) checksum_value);
    // 6. [...] wait for the server to respond with the appropriate AckMessage [...]
    while (true) {
      this.out.writeObject(message);
      Message response = (Message) this.in.readObject();
      if (response.getType() == MessageType.ACK) {
        // 6. [...] The sequence number in the ACK should be the number for the next expected chunk [...]
        if (((AckMessage) response).getSeq() == sequence_number + 1) {
          return sequence_number + 1;
        }
      }
    }
  }

  // 7. After sending all chunks and receiving the final ACK, the transfer has completed 
  //    and the client can either begin a new file transfer or disconnect.
  boolean askRepeat () {
    System.out.println("Transfer complete.");
    Scanner in = new Scanner(System.in);
    System.out.print("Would you like to transfer another file? [y/N]: ");
    String response = in.nextLine();
    if (response.length() == 0) {
      return false;
    }
    return response.charAt(0) == 'y' || response.charAt(0) == 'Y';
  }


}
