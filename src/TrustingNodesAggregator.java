import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrustingNodesAggregator {
    private static ExecutorService trustNodesExecutor = Executors.newFixedThreadPool(4);
    private int trustValue1, trustValue2, trustValue3;

    private static TrustingNodes differentialTrustingNode;

    private TrustingNodesAggregator() {
        super();
        this.trustValue1 = 0;
        this.trustValue2 = 0;
        this.trustValue3 = 0;
    }

    private void setTrustValue1(int trustValue1) {
        this.trustValue1 = trustValue1;
    }

    private void setTrustValue2(int trustValue2) {
        this.trustValue2 = trustValue2;
    }

    private void setTrustValue3(int trustVAlue3) {
        this.trustValue3 = trustVAlue3;
    }

    private int getTrustValue1() {
        return trustValue1;
    }

    private int getTrustValue2() {
        return trustValue2;
    }

    private int getTrustValue3() {
        return trustValue3;
    }

    private void processData(String fileName) {
        Queue<String> inputDataStructure = readFromDataSource(fileName);
        Queue<String> outputDataStructure = new ArrayDeque<>();
        //The first line read is the table header that needs to be skipped;
        String inputData = inputDataStructure.poll();
        inputData = inputDataStructure.poll();
        //The while loop goes through the entire ArrayDeque, and sends each value to be processed
        while (inputData != null) {

            boolean sendProcessingRequest = true;

            long endTime;
            long processingTime;
            boolean waitingForATrustValue = true;

            //System.out.println("inputData = \n"+inputData);
            final String message = encryptOutGoingMessage(inputData);

            long startTime = System.nanoTime();
            //The do-while loop keeps running until all trust nodes sent their values or until waiting time expires, and
            //you have at least one trust value
            do {
                //The threads that runs the Trust Nodes are started on the first loop of do-while
                if (sendProcessingRequest) {

                    trustNodesExecutor.submit(() -> {
                        try {
                            final int value1 = differentialTrustingNode.evaluateDataEntry(message);
                            setTrustValue1(value1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });

                    //differentialTrustingNode can be replaced with the other trust nodes that will be created
                    trustNodesExecutor.submit(() -> {
                        try {
                            final int value2 = differentialTrustingNode.evaluateDataEntry(message);
                            setTrustValue2(value2);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });

                    //differentialTrustingNode can be replaced with the other trust nodes that will be created
                    trustNodesExecutor.submit(() -> {
                        try {
                            final int value3 = differentialTrustingNode.evaluateDataEntry(message);
                            setTrustValue3(value3);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });

                    sendProcessingRequest = false;
                }
                endTime = System.nanoTime();
                processingTime = endTime-startTime;
                processingTime = TimeUnit.NANOSECONDS.toMillis(processingTime);
                waitingForATrustValue = processingTime < 100;

                if (waitingForATrustValue) {
                    if ((getTrustValue1() != 0) && (getTrustValue2() != 0) && (getTrustValue3() != 0)) {
                        waitingForATrustValue = false;
                    }
                }

                if (!waitingForATrustValue) {
                    if ((getTrustValue1() == 0) && (getTrustValue2() == 0) && (getTrustValue3() == 0)) {
                        waitingForATrustValue = true;
                    }
                }
                //System.out.println("Waiting for a trust value");

            } while (waitingForATrustValue);

            double trustValue = 0.33 * getTrustValue1() + 0.33 * getTrustValue2() + 0.33 * getTrustValue3();
/*
            System.out.println("TrustValue = "+trustValue+" trustValue1 = "+getTrustValue1()+" trustValue2 = "+getTrustValue2()+" trustValue3 = "+getTrustValue3());
            System.out.println("timepassed = "+processingTime);
*/


            if (trustValue >= 0) {
                outputDataStructure.offer(inputData);
            }
            inputData = inputDataStructure.poll();
            setTrustValue1(0);
            setTrustValue2(0);
            setTrustValue3(0);
            //System.out.println("Entry line has been processed");
        }
        System.out.println("File processed");
        trustNodesExecutor.shutdown();

        saveToOutputFormat(outputDataStructure, fileName);

    }

    private void saveToOutputFormat(Queue<String> outputDataStructure, String fileName) {
        System.out.println("Writing to file...");
        String name = "processed_"+fileName;

        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), StandardCharsets.UTF_8))) {
            String line;
            while ((line=outputDataStructure.poll())!=null) {
                line=line+"\n";
                out.write(line);
                //System.out.println("line: "+line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Processed file saved!");
    }

    private Queue<String> readFromDataSource(String fileName) {
        Queue<String> dataStructure = new ArrayDeque<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line=in.readLine()) != null){
                dataStructure.offer(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataStructure;
    }

    //This method encrypt the message sent to the client for transfer a file or a subset
    private String encryptOutGoingMessage(String message) {
        String secret = "qwertyuiopasdfgh";
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        SecretKey secretKey;

        try {
            Cipher myCipher = Cipher.getInstance("AES");
            secretKey = new SecretKeySpec(Arrays.copyOf(decodedKey, 16), "AES");
            myCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] bytesToBeEncrypted = message.getBytes("UTF-8");
            byte[] encryptedBytes = myCipher.doFinal(bytesToBeEncrypted);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static void main(String[] args) {
        System.out.println("Set security policy file");
        System.setProperty("java.security.policy", "file:./server.policy");
        System.out.println("Set Security Manager");
        System.setSecurityManager(new SecurityManager());
        try {
            System.out.println("Looking for the Server");
            String name = "//DStoian-LEN/DifferentialTrustingNode";
            //String name = "//Doru-PC/DifferentialTrustingNode";
            //String name = "//LAPTOP-GDTMA4IQ/DifferentialTrustingNode";
            differentialTrustingNode = (TrustingNodes) Naming.lookup(name);
            System.out.println("Binding to the server");
            //For now the file name to be processed is hard coded
            String fileName = "Hamilton_County_Sheriff.csv";
            TrustingNodesAggregator trustingNodesAggregator = new TrustingNodesAggregator();
            trustingNodesAggregator.processData(fileName);
        } catch (RemoteException | NotBoundException | MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

}
