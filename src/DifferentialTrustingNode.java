import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DifferentialTrustingNode extends UnicastRemoteObject implements TrustingNodes, Serializable {
    private static final long serialVersionUID = 1001L;

    List<String> sureEvents;
    List<String> highProbabilityEvents;

    public DifferentialTrustingNode() throws RemoteException {
        super();
        this.sureEvents = new ArrayList<>(Arrays.asList("TRAFFIC STOP", "VIN CHECK", "DIRECTED PATROL", "COURT ORDERED PROPETY HOLD", "DEATH INVESTIGATION", "ESCORT"));
        this.highProbabilityEvents = new ArrayList<>(Arrays.asList("ACC SINKING VEH", "ACCELERATOR STUCK", "ACCIDENT- HIT & RUN PERSONAL INJURY", "ACCIDENT- HIT & RUN PROPERTY DAMAGE",
                "ACCIDENT- PERSONAL INJURY", "ACCIDENT- PROPERY DAMAGE", "ANIMAL BITE/ATTACK", "ASSIST FIRE", "ASSIST OTHER DEPT", "ASSIST PUBLIC", "BURGLARY", "CASE FOLLOW UP",
                "CRIMINAL MISCHIEF", "DRUG LAB", "HOSTAGE SITUATION", "INTOXICATED PERSON", "INVESTIGATION", "KIDNAPPING", "ROBBERY", "SHOOTING", "STABBING", "SUICIDE",
                "THEFT", "THEFT FROM VEHICLE", "TOW RELEASE", "WEAPONS COMPLAINT"));
    }

    public List<String> getSureEvents() {
        return sureEvents;
    }

    public List<String> getHighProbabilityEvents() {
        return highProbabilityEvents;
    }


    @Override
    public int recordScore(String recordEntry) throws RemoteException {
        String[] splitRecord = recordEntry.split(",", 7);
        String typeOfEvent = splitRecord[5];
        //System.out.println("recordEntry: \n"+recordEntry);
        //System.out.println("typeOfEvent: "+typeOfEvent);
        Random rand = new Random();
        if (getSureEvents().contains(typeOfEvent)) {
            System.out.println("sure event");
            return 1;
        }

        if (getHighProbabilityEvents().contains(typeOfEvent)) {
            if ((rand.nextInt(100)+1)<85){ // Choose 85% of the inputs to be trustworthy (High percentage)
                System.out.println("high probability event");
                return 1;
            }
        }

        if ((rand.nextInt(100)+1)<51) { // Choose 50% of the inputs to be trustworthy (medium percentage)
            System.out.println("Medium probability event");
            return 1;
        }

        return -1;
    }

    public static void main(String[] args) {

        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        DifferentialTrustingNode differentialTrustingNodes=null;

        try {
            System.out.println("Creating Detection Nodes Server!");
            String name = "DifferentialTrustingNode";
            differentialTrustingNodes = new DifferentialTrustingNode();
            System.out.println("Differential Trusting Node: binding it to name: " + name);
            Naming.rebind(name, differentialTrustingNodes);
            System.out.println("Differential Trusting Node Server Ready!");
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
