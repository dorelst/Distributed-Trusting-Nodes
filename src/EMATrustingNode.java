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

/*
 * This is a trusting node that keeps an exponential moving average of the 
 * probablilites of different incident types. This is react strongly to
 * many reports of the same incedent type in a short timeframe, while
 * scattered reports will cause the probablity of trusting them to fall off
 */
public class EMATrustingNode extends UnicastRemoteObject implements TrustingNodes {
    private Map<String, Double> emaProbs = new HashMap<String, Double>();
    private Map<String, Integer> emaCounts = new HashMap<String, Integer>();
    private double alpha = 0.5;
    private Random r = new Random();

    public EMATrustingNode() throws RemoteException {}

    private double getProbAndUpdate(String typeOfEvent){
        synchronized(this){
            Set<String> keys = emaProbs.keySet();
            if(keys.contains(typeOfEvent)){
                for(String key : keys){
                    double p = emaProbs.get(key);
                    int n = emaCounts.get(key);
                    if(key.equals(typeOfEvent)){
                        p = (1 + ((1 - alpha) * p)) / (1 + ((1 - alpha) * n));
                    } else {
                        p = ((1 - alpha) * p) / (1 + (( 1 - alpha) * n));
                    }
                    emaProbs.put(key, p);
                    emaCounts.put(key, n + 1);
                }
            } else {
                for(String key : keys){
                    double p = emaProbs.get(key);
                    int n = emaCounts.get(key);
                    p = ((1 - alpha) * p) / (1 + (( 1 - alpha) * n));
                    emaProbs.put(key, p);
                    emaCounts.put(key, n + 1);
                }
                emaProbs.put(typeOfEvent, 0.5);
                emaCounts.put(typeOfEvent, 1);
            }
            return emaProbs.get(typeOfEvent);
        }
    }

    public int evaluateDataEntry(String dataEntryToBeProcessed, int trustNodeSelector) throws RemoteException{
        String decryptedMessage = decryptReceivedMessage(dataEntryToBeProcessed);
        String[] splitRecord = decryptedMessage.split(",", 7);
        String typeOfEvent = splitRecord[5];
        double p = getProbAndUpdate(typeOfEvent);
        if (r.nextDouble() < p){
            return 1;
        } else {
            return -1;
        }
    }

    public Map<Integer, String> requestTypesOfTrustSupportedByNode() throws RemoteException{
        return new HashMap<Integer, String>() {{ put(1, "EMA"); }};
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
        EMATrustingNode emaTrustingNode=null;

        try {
            System.out.println("Creating Detection Nodes Server!");
            String name = "EMATrustingNode";
            emaTrustingNode = new EMATrustingNode();
            System.out.println("Differential Trusting Node: binding it to name: " + name);
            Naming.rebind(name, emaTrustingNode);
            System.out.println("Differential Trusting Node Server Ready!");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
