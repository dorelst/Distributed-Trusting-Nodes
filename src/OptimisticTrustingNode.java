import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class OptimisticTrustingNode extends UnicastRemoteObject implements TrustingNodes, Serializable {

    private static final long serialVersionUID = 1001L;

    public OptimisticTrustingNode() throws RemoteException {
    }

    public int evaluateDataEntry(String dataEntryToBeProcessed) throws RemoteException {
        Random rand = new Random();
        System.out.println("Second Trust Node: Optimistic Trust");

        // Choose 85% of the inputs to be trustworthy (High percentage)
        return (rand.nextInt(100)+1)<85 ? 1 : -1;
    }

    //This method decrypt a string
    private String decryptReceivedMessage (String message) {
        String secret = "qwertyuiopasdfgh";
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        SecretKey secretKey;

        try {
            Cipher myCipher = Cipher.getInstance("AES");
            secretKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
            myCipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] bytesToBeDecrypted = Base64.getDecoder().decode(message);
            byte[] decryptedBytes = myCipher.doFinal(bytesToBeDecrypted);
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static void main(String[] args) {

        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        OptimisticTrustingNode secondTrustingNode=null;

        try {
            System.out.println("Creating Second Trusting Node Server!");
            String name = "OptimisticTrustingNode";
            secondTrustingNode = new OptimisticTrustingNode();
            System.out.println("Second Trusting Node: binding it to name: " + name);
            Naming.rebind(name, secondTrustingNode);
            System.out.println("Second Trusting Node Server Ready!");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
