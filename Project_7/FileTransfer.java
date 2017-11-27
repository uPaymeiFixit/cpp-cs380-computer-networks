import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class FileTransfer {

  public static void main (String[] args) throws Exception {
    switch (args[0]) {
      case "makekeys": makekeys(); break;
      case "server": new FileTransfer_server(args[1], Integer.parseInt(args[2])); break;
      case "client": new FileTransfer_client(args[1], args[2], Integer.parseInt(args[3])); break;
    }
  }

  // Generate public and private keys (code given in instructions)
  public static void makekeys () {
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(4096);
      KeyPair keyPair = gen.genKeyPair();
      PrivateKey privateKey = keyPair.getPrivate();
      PublicKey publicKey = keyPair.getPublic();
      System.out.print("Generating ./public.bin...  ");
      try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("public.bin"))
      )) {
        oos.writeObject(publicKey);
        System.out.println("Success!");
      }
      System.out.print("Generating ./private.bin... ");      
      try (ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(new File("private.bin"))
      )) {
        oos.writeObject(privateKey);
        System.out.println("Success!");
      }
      
    } catch (NoSuchAlgorithmException | IOException e) {
      e.printStackTrace(System.err);
    }
  }
}
