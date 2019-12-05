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


public class ThirdTrustingNode extends UnicastRemoteObject implements TrustingNodes, Serializable {

    private static final long serialVersionUID = 1001L;

    private List<String> sureEvents;
    private List<String> highProbabilityEvents;
    private Map<Integer, String> typesOfTrustSupportedByNode;

    private ThirdTrustingNode() throws RemoteException {
        super();
        this.sureEvents = new ArrayList<>(Arrays.asList("TRAFFIC STOP", "VIN CHECK", "DIRECTED PATROL", "COURT ORDERED PROPETY HOLD", "DEATH INVESTIGATION", "ESCORT"));
        this.highProbabilityEvents = new ArrayList<>(Arrays.asList("ACC SINKING VEH", "ACCELERATOR STUCK", "ACCIDENT- HIT & RUN PERSONAL INJURY", "ACCIDENT- HIT & RUN PROPERTY DAMAGE",
                "ACCIDENT- PERSONAL INJURY", "ACCIDENT- PROPERY DAMAGE", "ANIMAL BITE/ATTACK", "ASSIST FIRE", "ASSIST OTHER DEPT", "ASSIST PUBLIC", "BURGLARY", "CASE FOLLOW UP",
                "CRIMINAL MISCHIEF", "DRUG LAB", "HOSTAGE SITUATION", "INTOXICATED PERSON", "INVESTIGATION", "KIDNAPPING", "ROBBERY", "SHOOTING", "STABBING", "SUICIDE",
                "THEFT", "THEFT FROM VEHICLE", "TOW RELEASE", "WEAPONS COMPLAINT"));
        this.typesOfTrustSupportedByNode = new LinkedHashMap<>();
        this.typesOfTrustSupportedByNode.put(1, "Differential Trust - C");
        this.typesOfTrustSupportedByNode.put(2, "Optimistic Trust - C");
        this.typesOfTrustSupportedByNode.put(3, "Pessimistic Trust - C");
    }

    private List<String> getSureEvents() {
        return sureEvents;
    }

    private List<String> getHighProbabilityEvents() {
        return highProbabilityEvents;
    }

    private Map<Integer, String> getTypesOfTrustSupportedByNode() {
        return typesOfTrustSupportedByNode;
    }

    @Override
    public int evaluateDataEntry(String dataEntryToBeProcessed, int trustTypeSelector) throws RemoteException {
        //System.out.println("Request received!");

        switch (trustTypeSelector) {
            case 1: return differentialTrust(dataEntryToBeProcessed);
            case 2: return optimisticTrust();
            case 3: return pessimisticTrust();
            default: return 0;
        }
    }

    @Override
    public Map<Integer, String> requestTypesOfTrustSupportedByNode() throws RemoteException {
        return getTypesOfTrustSupportedByNode();
    }

    private int differentialTrust(String dataEntryToBeProcessed) {
        System.out.print("Third Trust Node: Differential Trust: ");
        String decryptedMessage = decryptReceivedMessage(dataEntryToBeProcessed);
        String[] splitRecord = decryptedMessage.split(",", 7);
        String typeOfEvent = splitRecord[5];
        //System.out.println("decryptedMessage: \n"+decryptedMessage);
        //System.out.println("typeOfEvent: "+typeOfEvent);
        Random rand = new Random();
        if (getSureEvents().contains(typeOfEvent)) {
            System.out.print("sure event\n");
            return 1;
        }

        if (getHighProbabilityEvents().contains(typeOfEvent)) {
            if ((rand.nextInt(100)+1)<85){ // Choose 85% of the inputs to be trustworthy (High percentage)
                System.out.print("high probability event\n");
                return 1;
            }
        }

        if ((rand.nextInt(100)+1)<51) { // Choose 50% of the inputs to be trustworthy (medium percentage)
            System.out.print("Medium probability event\n");
            return 1;
        }

        return -1;

    }

    private int optimisticTrust() {
        Random rand = new Random();
        System.out.println("Third Trust Node: Optimistic Trust");

        // Choose 85% of the inputs to be trustworthy (High percentage)
        return (rand.nextInt(100)+1)<85 ? 1 : -1;
    }

    private int pessimisticTrust() {
        Random rand = new Random();
        System.out.println("Third Trust Node: Pessimistic Trust");

        // Choose 85% of the inputs to be trustworthy (High percentage)
        return (rand.nextInt(100)+1)<25 ? 1 : -1;
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
        ThirdTrustingNode thirdTrustingNode=null;

        try {
            System.out.println("Creating Third Trusting Node Server!");
            String name = "ThirdTrustingNode";
            thirdTrustingNode = new ThirdTrustingNode();
            System.out.println("Third Trusting Node: binding it to name: " + name);
            Naming.rebind(name, thirdTrustingNode);
            System.out.println("Third Trusting Node Server Ready!");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
