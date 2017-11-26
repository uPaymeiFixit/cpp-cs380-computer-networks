import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

public class FileTransfer {

  public static void main (String[] args) {
    switch (args[0]) {
      case "makekeys": makekeys(); break;
      case "server": server(args[1], Integer.parseInt(args[2])); break;
      case "client": client(args[1], args[2], Integer.parseInt(args[3])); break;
    }
  
  }

  public static void makekeys () {
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(4096); // you can use 2048 for faster key generatoion
      KeyPair keyPair = gen.genKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("public.bin"))
      )) {
        oos.writeObject(publicKey);
      }
      try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("private.bin"))
      )) {
        oos.writeObject(privateKey);
      }
      
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace(System.err);
    }
  }

  public static void server (String private_key_file, int port) {

  }

  public static void client (String public_key_file, String host, int port) {

  }

}
