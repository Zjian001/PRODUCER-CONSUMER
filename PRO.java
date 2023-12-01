import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;

class SharedResource {
    BlockingQueue<String> buffer = new LinkedBlockingQueue<>();
    volatile boolean producerDone = false;
}

class Producer implements Runnable {
    private SharedResource sharedResource;
    private int itemsToProduce;
    private long sleepTime;

    public Producer(SharedResource sharedResource, int itemsToProduce, long sleepTime) {
        this.sharedResource = sharedResource;
        this.itemsToProduce = itemsToProduce;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        for (int i = 0; i < itemsToProduce; i++) {
            try {
                Thread.sleep(sleepTime);
                String item = "Item " + i;
                sharedResource.buffer.put(item);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sharedResource.producerDone = true;
    }
}

class Consumer implements Runnable {
    private SharedResource sharedResource;
    private long sleepTime;

    public Consumer(SharedResource sharedResource, long sleepTime) {
        this.sharedResource = sharedResource;
        this.sleepTime = sleepTime;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String item = sharedResource.buffer.poll(1, TimeUnit.SECONDS);
                if (item == null && sharedResource.producerDone) {
                    break; // Stop consuming when the producer is done and the buffer is empty
                }
                if (item != null) {
                    Thread.sleep(sleepTime);
                    System.out.println("Consumed: " + item);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


public class PRO {
    private static final String OUTPUT_FILE = "sample_output.txt";

    
    public static void runTest(int producers, int consumers, long sleepTime, int testNumber) {
        SharedResource sharedResource = new SharedResource();
        ExecutorService executorService = Executors.newFixedThreadPool(consumers + producers);
    
        // Record the start time
        long startTime = System.currentTimeMillis();
    
        executorService.execute(new Producer(sharedResource, 16, sleepTime));
        for (int i = 0; i < consumers; i++) {
            executorService.execute(new Consumer(sharedResource, sleepTime));
        }
    
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        // Record the end time
        long endTime = System.currentTimeMillis();
        long turnaroundTime = endTime - startTime;
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true))) {
            writer.write(String.format("Test case %d: Turnaround Time with Sleep Time %d: %.2f ms%n",
                testNumber, sleepTime, (double) turnaroundTime));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    public static void main(String[] args) {
        long sleepTime = Long.parseLong(args[0]); // Get sleep time from command line
    
        int[][] testCases = {
                {1, 1}, {4, 1}, {16, 1},
                {1, 2}, {4, 2}, {16, 2},
                {1, 4}, {4, 4}, {16, 4},
                {1, 16}, {4, 16}, {16, 16},
        };
    
        for (int i = 0; i < testCases.length; i++) {
            int producers = testCases[i][0];
            int consumers = testCases[i][1];
            
            int testNumber = i + 1;
            PRO.runTest(producers, consumers, sleepTime,testNumber);
        }
    }
}
