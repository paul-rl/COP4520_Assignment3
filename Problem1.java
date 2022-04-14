import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class Problem1{
    static int TESTER = 32;
    private static class Node {
        int key;
        AtomicMarkableReference<Node> next;
        public Node(int key){
            this.key = key;
            next = new AtomicMarkableReference<Problem1.Node>(null, false);
        }
    }

    private static class Window {
        public Node pred, curr;
        public Window(Node myPred, Node myCurr){
            pred = myPred;
            curr = myCurr;
        }
    }

    private static class ParallelLinkedList {
        Node head;
        int size = 0;
        public ParallelLinkedList() {
            head = new Node(Integer.MIN_VALUE);
            Node tail = new Node(Integer.MAX_VALUE);
            head.next = new AtomicMarkableReference<Node>(tail, false);
        }

        public boolean add(int guestID){
            while (true){
                Window window = find(head, guestID);
                Node pred = window.pred, curr = window.curr;
                if (curr.key == guestID) { 
                    return false;
                } else {
                    Node node = new Node(guestID);
                    node.next = new AtomicMarkableReference<Node>(curr, false);
                    if (pred.next.compareAndSet(curr, node, false, false)) {
                        return true;
                    }
                }
            }
        }

        public boolean remove(int guestID){
            boolean snip;
            while (true) {
                Window window = find(head, guestID);
                Node pred = window.pred, curr = window.curr;
                if (curr.key != guestID) {
                    return false;
                } else {
                    Node succ = curr.next.getReference();
                    snip = curr.next.compareAndSet(succ, succ, false, true);
                    if (!snip)continue;
                    pred.next.compareAndSet(curr, succ, false, false);
                    return true;
                }
            }
        }

        public boolean contains(int guestID) {
            Node curr = head;
            while (curr.key < guestID) {
                curr = curr.next.getReference();
            } 
            return (curr.key == guestID && !curr.next.isMarked());
        }

        public Window find(Node head, int key){
            Node pred = null, curr = null, succ = null;
            boolean[] marked = {false};
            boolean snip;
            outer: while(true){
                pred = head;
                curr = pred.next.getReference();
                while (true){
                    succ = curr.next.get(marked);
                    while(marked[0]){
                        snip = pred.next.compareAndSet(curr, succ, false, false);
                        if (!snip) continue outer;
                        curr = pred.next.getReference();
                        succ = curr.next.get(marked);
                    }
                    if (curr.key >= key){ return new Window(pred, curr);}
                    pred = curr;
                    curr = succ;
                }
            }
        }
    }

    private static class AddTask implements Callable<Boolean>{
        int toAdd;
        ParallelLinkedList list;
        public AddTask(int guestID, ParallelLinkedList list){
            toAdd = guestID;
            this.list = list;        
        }

        @Override
        public Boolean call(){
            // System.out.println("Adding " + toAdd);
            boolean added = list.add(toAdd);
            if (added)
                list.size++;
            return added;
        }
    }

    private static class RemoveTask implements Callable<Boolean>{
        int toRemove;
        ParallelLinkedList list;
        public RemoveTask(int guestID, ParallelLinkedList list){
            toRemove = guestID;
            this.list = list;
        }

        @Override
        public Boolean call(){
            // System.out.println("Removing " + toRemove);
            boolean removed = list.remove(toRemove);
            if (removed)
                list.size--;
            return removed;
        }
    }

    private static class ContainsTask implements Callable<Boolean>{
        int toSearch;
        ParallelLinkedList list;
        public ContainsTask(int guestID, ParallelLinkedList list){
            toSearch = guestID;
            this.list = list;
        }

        @Override
        public Boolean call(){
            boolean found = list.contains(toSearch);
            return found;
        }
    }
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final int NUM_PRESENTS = 500_000;
        final int NUM_THREADS = 4;
        
        // Make randomly sorted bag of presents
        ArrayList<Integer> arr = new ArrayList<Integer>();
        Random rand = new Random();
        for (int id = 0; id < NUM_PRESENTS; id++) {
            arr.add(id);
        }
        Collections.shuffle(arr);
        
        ParallelLinkedList list = new ParallelLinkedList();
        verifyList(list);

        int countAdded = 0, countRemoved = 0;
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        ArrayList<Callable<Boolean>> todo = new ArrayList<Callable<Boolean>>();
        while (countAdded < NUM_PRESENTS && countRemoved < NUM_PRESENTS){
            float randFloat = rand.nextFloat();
            // Minotaur wants to check if the gift is in the chain
            if (randFloat < 0.05f) {
                int idx = rand.nextInt(NUM_PRESENTS);
                ContainsTask task = new ContainsTask(idx, list);
                todo.add(task);
            }

            // Two servants will add the next element found in "randomly sorted bag"
            for (int i = 0; i < NUM_THREADS / 2; i++){
                if (countAdded < NUM_PRESENTS){
                    AddTask addTask = new AddTask(arr.get(countAdded++), list);
                    todo.add(addTask);
                }
            }
            
            
            // Two servants will remove the first present in line
            for (int i = 0; i < NUM_THREADS / 2; i++){
                if (countRemoved < NUM_PRESENTS){
                    RemoveTask removeTask = new RemoveTask(arr.get(countRemoved++), list);
                    todo.add(removeTask);
                }
            }
        }
        exec.invokeAll(todo);

        // Get rid of stragglers
        Node head = list.head;
        int len = list.size;
        while(len > 0 && head.next != null && head.next.getReference() != null){
            exec.submit(new RemoveTask(head.next.getReference().key, list));
            head = head.next.getReference();
            len--;
        }
        exec.shutdown();
        try{
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (Exception e){System.out.println("Error awaiting termination of first executor.");}
        //verifyList(list);
        System.out.println(" All thank you notes have been written! :)");
    }
    
    public static void verifyList(ParallelLinkedList list){
        //System.out.println("Verifying list!");
        Node n = list.head;
        while (n.next != null && n.next.getReference() != null){
            //System.out.println("u"+n.key);
            if (n.key > n.next.getReference().key){
                //System.out.println(n.key);
                //System.out.println("NOT SORTED, ABORT");
                return;
            }                
            n = n.next.getReference();
        }
        //System.out.println("LIST HAS " + list.size + " ELEMENTS");
    }
}