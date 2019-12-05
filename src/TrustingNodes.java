import java.util.Map;

public interface TrustingNodes extends java.rmi.Remote {
    int evaluateDataEntry(String dataEntryToBeProcessed, int trustNodeSelector) throws java.rmi.RemoteException;
    Map<Integer, String> requestTypesOfTrustSupportedByNode() throws java.rmi.RemoteException;
}
