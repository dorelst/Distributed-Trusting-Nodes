import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrustingNodesAgreggator {
    ExecutorService trustNodesExecutor = Executors.newFixedThreadPool(4);
    int trustValue1, trustValue2, trustVAlue3;

    private static TrustingNodes differentialTrustingNode;

    public void setTrustValue1(int trustValue1) {
        this.trustValue1 = trustValue1;
    }

    public void setTrustValue2(int trustValue2) {
        this.trustValue2 = trustValue2;
    }

    public void setTrustValue3(int trustVAlue3) {
        this.trustVAlue3 = trustVAlue3;
    }

    public int getTrustValue1() {
        return trustValue1;
    }

    public int getTrustValue2() {
        return trustValue2;
    }

    public int getTrustValue3() {
        return trustVAlue3;
    }

    private void processData(String fileName) {
        Queue<String> inputDataStructure = readFromDataSource(fileName);
        Queue<String> outputDataStructure = new ArrayDeque<>();
        String inputData = inputDataStructure.poll();

        while (inputData != null) {
            double trustValue = 0;
            int i = 0;
            long startTime = System.nanoTime();
            long endTime;
            long processingTime;
            boolean waiting = true;
            do {
                if (i == 0) {
                    final String message = inputData;
                    Runnable getTrustValue1 = () -> {
                        try {
                            final int value1 = differentialTrustingNode.recordScore(message);
                            setTrustValue1(value1);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    };

                    Runnable getTrustValue2 = () -> {
                        try {
                            final int value2 = differentialTrustingNode.recordScore(message);
                            setTrustValue2(value2);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    };

                    Runnable getTrustValue3 = () -> {
                        try {
                            final int value3 = differentialTrustingNode.recordScore(message);
                            setTrustValue3(value3);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    };
                    trustNodesExecutor.submit(getTrustValue1);
                    trustNodesExecutor.submit(getTrustValue2);
                    trustNodesExecutor.submit(getTrustValue3);
                    i++;
                }
                endTime = System.nanoTime();
                processingTime = endTime-startTime;
                processingTime = TimeUnit.NANOSECONDS.toMillis(processingTime);
                if (processingTime < 100) {
                    waiting = true;
                } else {
                    waiting = false;
                }

                if (waiting) {
                    if ((getTrustValue1() != 0) && (getTrustValue2() != 0) && (getTrustValue3() != 0)) {
                        waiting = false;
                    }
                } else {
                    if ((getTrustValue1() != 0) && (getTrustValue2() != 0) && (getTrustValue3() != 0)) {
                        waiting = true;
                    }
                }

            } while (waiting);


            trustValue = 0.33 * getTrustValue1() + 0.33 * getTrustValue2() + 0.33 * getTrustValue3();
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
        }
        saveToOutputFormat(outputDataStructure, fileName);
    }




    private void saveToOutputFormat(Queue<String> outputDataStructure, String fileName) {
        String name = "processed_"+fileName;
        try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), StandardCharsets.UTF_8))) {
            String line;
            while ((line=outputDataStructure.poll())!=null) {
                line=line+"\n";
                out.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Queue readFromDataSource(String fileName) {
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
            String fileName = "Hamilton_County_Sheriff.csv";
            TrustingNodesAgreggator trustingNodesAgreggator = new TrustingNodesAgreggator();
            trustingNodesAgreggator.processData(fileName);
        } catch (RemoteException | NotBoundException | MalformedURLException ex) {
            ex.printStackTrace();
        }

    }

}
