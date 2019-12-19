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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.lang.Thread;

public class TrustingNodesAggregator {

    private List<TrustingNodes> remotes;
    private List<Queue<String>> inputs;
    private List<Queue<Integer>> outputs;

    private TrustingNodesAggregator(List<TrustingNodes> remotes) {
        this.remotes = remotes;
        this.inputs = new ArrayList<Queue<String>>(remotes.size() + 1);
        this.outputs = new ArrayList<Queue<Integer>>(remotes.size());
        for (int i = 0; i < remotes.size(); i++){
            this.inputs.add(new ConcurrentLinkedQueue<String>());
            this.outputs.add(new ConcurrentLinkedQueue<Integer>());
        }
        // we need one additional input for the combiner to write to the output
        this.inputs.add(new ConcurrentLinkedQueue<String>());
    }

    private void processData(String fileName) {
        long endTime;
        long processingTime;
        long startTime = System.nanoTime();

        List<Thread> threads = new ArrayList<Thread>(remotes.size() + 2);
        String outputFileName = "Processed_" + fileName;

        /*
         * This class is a runnable that will read all the lines of the
         * input file, and popuate each processor threads input queues
         * with the file data
         */
        class Injester implements Runnable{
            private String filename;
            private List<Queue<String>> inputs;
            
            public Injester(String filename, List<Queue<String>> inputs){
                this.filename = filename;
                this.inputs = inputs;
            }

            public void run(){
                try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
                    String line;
                    // The first line read is the table header that needs to be skipped
                    in.readLine();
                    while ((line=in.readLine()) != null){
                        for (Queue<String> q : inputs){
                            q.offer(line);
                        }
                    }
                    for (Queue<String> q : inputs){
                        q.offer("DONE");
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /*
         * This class is a runnable that will injest file data from a queue,
         * send that data to a remote trust node to be evaluated, then put
         * that trust result into a queue for combining
         */
        class Processor implements Runnable{
            private Queue<String> input;
            private Queue<Integer> output;
            private TrustingNodes remote;

            public Processor(Queue<String> input, Queue<Integer> output, TrustingNodes remote){
                this.input = input;
                this.output = output;
                this.remote = remote;
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

            public void run(){
                String inputData;
                while(true){
                    inputData = input.poll();
                    if(inputData == null){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if (inputData.equals("DONE")){
                        break;
                    }
                    try{
                        int result = remote.evaluateDataEntry(encryptOutGoingMessage(inputData));
                        output.offer(result);
                    } catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        /*
         * This class is a runnable that will combine all the trust results
         * generated by the remotes, and use that data to decide weither or
         * not to trust a piece of input data. If so, it will write that data
         * to the output file, if not, it will discard it
         */
        class Combiner implements Runnable{
            private Queue<String> input;
            private List<Queue<Integer>> outputs;
            private String outputFileName;

            public Combiner(Queue<String> input, List<Queue<Integer>> outputs, String outputFileName){
                this.input = input;
                this.outputs = outputs;
                this.outputFileName = outputFileName;
            }

            private boolean allQueuesHaveData(){
                for(Queue<Integer> q : outputs){
                    if (q.peek() == null){
                        return false;
                    }
                }
                return true;
            }

            public void run(){
                String inputData;
                try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), StandardCharsets.UTF_8))){
                    while(true){
                        inputData = input.poll();
                        if (inputData == null){
			    try{
				Thread.sleep(10);
			    } catch (Exception e){
				e.printStackTrace();
			    }
                            continue;
                        }
                        if (inputData.equals("DONE")){
                            break;
                        }
                        // if any output queues are empty, wait 10 ms to allow catchup
                        while(!allQueuesHaveData()){
                            try{
                                Thread.sleep(10);
                            } catch (Exception e){
                                // swallow exceptions
                            }
                        }
                        double p = 0.0;
                        for(Queue<Integer> q : outputs){
                            p = p + q.poll();
                        }
                        p = p / outputs.size();
                        if (p < 0){
                            out.write(inputData + "\n");
                        }
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        
        Thread t;
        t = new Thread(new Injester(fileName, inputs));
        t.start();
        threads.add(t);

        for(int i = 0; i < remotes.size(); i++){
            t = new Thread(new Processor(inputs.get(i), outputs.get(i), remotes.get(i)));
            t.start();
            threads.add(t);
        }

        t = new Thread(new Combiner(inputs.get(remotes.size()), outputs, outputFileName));
        t.start();
        threads.add(t);

        // now that all the threads are going, we just need to wait for them all to finish
        for(Thread td : threads){
            try{
                td.join();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        endTime = System.nanoTime();
        processingTime = endTime-startTime;
        processingTime = TimeUnit.NANOSECONDS.toMillis(processingTime);
        System.out.println("Processing time = "+processingTime+" miliseconds");
    }


    public static void main(String[] args) {
        System.out.println("Set security policy file");
        System.setProperty("java.security.policy", "file:./server.policy");
        System.out.println("Set Security Manager");
        System.setSecurityManager(new SecurityManager());
        try {
            System.out.println("Looking for the Server");
            String name1 = "//in-csci-rrpc02.cs.iupui.edu/DifferentialTrustingNode";
            String name2 = "//in-csci-rrpc03.cs.iupui.edu/EMATrustingNode";
            String name3 = "//in-csci-rrpc04.cs.iupui.edu/PessimisticTrustingNode";
            List<TrustingNodes> remotes = new ArrayList<TrustingNodes>();
            remotes.add((TrustingNodes) Naming.lookup(name1));
            System.out.println("Binding to the differentialTrustingNode server");
            remotes.add((TrustingNodes) Naming.lookup(name2));
            System.out.println("Binding to the secondTrustingNode server");
            remotes.add((TrustingNodes) Naming.lookup(name3));
            System.out.println("Binding to the thirdTrustingNode server");
            //For now the file name to be processed is hard coded
            String fileName = "SampleData.csv";
            TrustingNodesAggregator trustingNodesAggregator = new TrustingNodesAggregator(remotes);
            trustingNodesAggregator.processData(fileName);
        } catch (RemoteException | NotBoundException | MalformedURLException ex) {
	    System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

}
