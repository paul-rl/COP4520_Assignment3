import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class Problem2 {
    static final int NUM_THREADS = 8;
    static final int SECS_IN_MIN = 60;
    static final int MIN_IN_HOUR = 60;
    static final int NUM_HOURS = 24;
    static final int HIGH = 70;
    static final int LOW = -100;
     
    static int[][] prevReadings = new int[NUM_THREADS][SECS_IN_MIN];
    static int[][] currReadings = new int[NUM_THREADS][SECS_IN_MIN];
    static int[][][] localExtremeVals = new int[NUM_HOURS][NUM_THREADS][5 + 5 + 2];

    static ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);

    private static class ReadingGenTask implements Callable<Integer>{
        int sensorID, minuteNo;

        public ReadingGenTask(int sensorID, int minuteNo){
            this.sensorID = sensorID;
            this.minuteNo = minuteNo;
        }

        public Integer call(){
            int reading = ThreadLocalRandom.current().nextInt(HIGH - LOW) + LOW;
            currReadings[sensorID][minuteNo] = reading;
            return 0;
        }
    }
    private static class CopyArrTask implements Callable<Integer> {
        int sensorID;

        public CopyArrTask(int sensorID){
            this.sensorID = sensorID;
        }

        public Integer call(){
            prevReadings = currReadings.clone();
            return 0;
        }
    }
    private static class TakeReadingsTask implements Callable<Integer> {
        int minuteNo;

        public TakeReadingsTask(int minuteNo){
            this.minuteNo = minuteNo;
        }

        public Integer call(){
            // System.out.println("Taking readings");
            for (int i = 0; i < NUM_THREADS; i++){
                ReadingGenTask rgt = new ReadingGenTask(i, minuteNo);
                exec.submit(rgt);
            }
            return 0;
        }
    }
    private static class FindExtremaTask implements Callable<Integer> {
        int hourNo;
        public FindExtremaTask(int hourNo){
            this.hourNo = hourNo;
        }
        public Integer call(){
            List<Future<Integer>> wait = new ArrayList<Future<Integer>>();
            for (int i = 0; i < NUM_THREADS; i++){
                wait.add(exec.submit(new FindLocalExtremaTask(i, hourNo)));
            }
            return 0;
        }
    }
    private static class FindLocalExtremaTask implements Callable<Integer>{
        int sensorID;
        int hourNo;

        public FindLocalExtremaTask(int sensorID, int hourNo){
            this.sensorID = sensorID;
            this.hourNo = hourNo;
        }

        public Integer call(){
            // Find 5 highest temperatures for this sensor
            Arrays.parallelSort(prevReadings[sensorID]);
            for (int i = 0; i < 5; i++){
                localExtremeVals[hourNo][sensorID][10 - 1 - i] = prevReadings[sensorID][prevReadings[sensorID].length - 1 - i];
                localExtremeVals[hourNo][sensorID][i] = prevReadings[sensorID][i];
            }
            return 0;
        }
    }
    
    public static Future<Integer> simulateMinute(int minuteNo){
        // System.out.println("Simulating Minute");
        int count = 0;
        while (count < SECS_IN_MIN){
            count++;
        }
        TakeReadingsTask trt = new TakeReadingsTask(minuteNo);
        Future<Integer> wait = exec.submit(trt);
        return wait;
    }
    public static void simulateHour(int hourNo){
        // System.out.println("Simulating hour");
        int count = 0;
        ArrayList<Future<Integer>> waitList = new ArrayList<Future<Integer>>(60);
        while (count < MIN_IN_HOUR){
            waitList.add(simulateMinute(count));
            count++;
        }
        // Wait for all readings to be taken
        for (Future<Integer> future : waitList) {
            while(!future.isDone());
        }
    }
    public static void takeReadings(int minuteNo){
        // Generate a reading for this minue in each of the 8 rows in 2D array
        for (int i = 0; i < NUM_THREADS; i++){
            ReadingGenTask rgt = new ReadingGenTask(i, minuteNo);
            exec.submit(rgt);
        }
    }
    public static void compileReport(int hourNo){
        // Move data over to previous array so report can be compiled without new readings
        // overriding previous ones
        List<Future<Integer>> wait = new ArrayList<Future<Integer>>();
        for (int i = 0; i < NUM_THREADS; i++){
            CopyArrTask cat = new CopyArrTask(i);
            wait.add(exec.submit(cat));
        }
        // Wait until all arrays are copied over
        for (int i = 0; i < NUM_THREADS; i++){
            Future<Integer> currWait = wait.get(i);
            while (!currWait.isDone());
        }

        for (int i = 0; i < NUM_THREADS; i++){
            for (int a : prevReadings[i]) {
                System.out.print(a + " ");
            }
            System.out.println();
        }

        FindExtremaTask fmt = new FindExtremaTask(hourNo);
        Future<Integer> currWait = exec.submit(fmt);
        // wait for local sensor maximums to be found
        while (!currWait.isDone());
        System.out.println("========Report for Hour #========" + hourNo);
        System.out.println("Top 5 Highest Temperatures: ");
        
        // Find 5 highest
        int[] maxIdx = {10 - 1, 10 - 1, 10 - 1, 10 - 1, 10 - 1, 10 - 1, 10 - 1, 10 -1};
        for (int i = 0; i < 5; i++){
            int[] maximums = new int[NUM_THREADS];
            System.out.println("MAXIMUMS");
            for (int j = 0; j < maximums.length; j++){
                maximums[j] = localExtremeVals[hourNo][j][maxIdx[j]];
                System.out.println(maximums[j]);
            }

            int globalMax = Integer.MIN_VALUE;
            int globalMaxIdx = -1;
            for (int j = 0; j < maximums.length; j++){
                if (maximums[j] > globalMax){
                    globalMax = maximums[j];
                    globalMaxIdx = j;
                }
            }
            maxIdx[globalMaxIdx]--;
            System.out.println(globalMax);
        }

        // Find 5 lowest
        int[] minIdx = {0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 5; i++){
            int[] minimums = new int[NUM_THREADS];
            for (int j = 0; j < minimums.length; j++){
                minimums[j] = localExtremeVals[hourNo][j][minIdx[j]];
            }

            int globalMin = Integer.MAX_VALUE;
            int globalMinIdx = -1;
            for (int j = 0; j < minimums.length; j++){
                if (minimums[j] < globalMin){
                    globalMin = minimums[j];
                    globalMinIdx = j;
                }
            }
            maxIdx[globalMinIdx]++;
            System.out.println(globalMin);
        }
    }

    public static void main(String[] args){
        // System.out.print("YEAH YEAH YEAH");
        for (int i = 0; i < 1; i++){
            simulateHour(i);
            compileReport(i); // This should be parallelized
            // System.out.println("Readings this hour");
            for (int j = 0; j < 1; j++){
                for (int k = 0; k < MIN_IN_HOUR; k++){
                    // System.out.print(prevReadings[j][k] + " ");
                }
            }
        }
    }
}
