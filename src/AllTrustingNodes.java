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

public class AllTrustingNodes{
    public static void main(String[] args) {

        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        try {
            String name = "PessimisticTrustingNode";
            PessimisticTrustingNode pnode = new PessimisticTrustingNode();
            Naming.rebind(name, pnode);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            String name = "OptimisticTrustingNode";
            OptimisticTrustingNode onode = new OptimisticTrustingNode();
            Naming.rebind(name, onode);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            String name = "DifferentialTrustingNode";
            DifferentialTrustingNode dnode = new DifferentialTrustingNode();
            Naming.rebind(name, dnode);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            String name = "EMATrustingNode";
            EMATrustingNode enode = new EMATrustingNode();
            Naming.rebind(name, enode);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
