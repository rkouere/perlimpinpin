// Visidia imports
import visidia.simulation.process.algorithm.Algorithm;
import visidia.simulation.process.messages.Door;
import visidia.simulation.process.messages.Message;

// Java imports
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import Message.*;

public class RicartAgrawala extends Algorithm {
    // All nodes data
    private int procId;
    private int t;
    private int t_prime;
    private int nbProcs;
    private List<Integer> a;

    // Higher speed means lower simulation speed
    private int speed = 1;

    // To display the state
    private boolean want = false;
    private boolean in = false;

    // Critical section thread
    private ReceptionRules rr = null;
    // State display frame
    private DisplayFrame df;
    
    private String demande = "demande";
    private String critique = "critique";
    private String neutre = "neutre";


    public String getDescription() {
        return ("Ricart-Agrawala Algorithm for Mutual Exclusion");
    }

    @Override
    public Object clone() {
        return new RicartAgrawala();
    }

    //
    // Nodes' code
    //
    @Override
    public void init() {
        procId = getId();
        Random rand = new Random(procId);
        putProperty("label", neutre);

        t = 0;
        t_prime = 0;
        a = new LinkedList<Integer>();

        rr = new ReceptionRules(this);
        rr.start();

        // Display initial state + give time to place frames
        df = new DisplayFrame(procId);
        displayState();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
        }

        while (true) {
            // Wait for some time
            int time = (3 + rand.nextInt(10)) * speed * 1000;
            System.out.println("Process " + procId + " wait for " + time);
            try {
                Thread.sleep(time);
            } catch (InterruptedException ie) {
            }

            // Try to access critical section
            askForCritical();
            putProperty("label", critique);

            // Access critical
            want = false;
            in = true;

            displayState();

            // Simulate critical resource use
            time = (1 + rand.nextInt(3)) * 1000;
            System.out.println("Process " + procId + " enter SC " + time);
            try {
                Thread.sleep(time);
            } catch (InterruptedException ie) {
            }
            System.out.println("Process " + procId + " exit SC ");

            // Release critical use
            endCriticalUse();
        }
    }

    //--------------------
    // Rules
    //-------------------

    // Rule 1 : ask for critical section
    synchronized void askForCritical() {
        want = true;
        t_prime = t + 1;
        nbProcs = getArity();
        putProperty("label", demande);
        REQMessage msg = new REQMessage(t_prime);
        sendAll(msg);
        System.out.println(procId + " - Ask for critical to all");
        while (nbProcs != 0) {
            displayState();
            try {
                this.wait();
            } catch( InterruptedException ie) {}
        }
    }

    // Rule 2 : receive REQ from d
    void receiveREQ(int d, int hd) {
        t = Math.max(t, hd);
        if (want && ((t_prime < hd) || ((t_prime == hd) && procId < d))) {
            a.add(d);
            System.out.println(procId + " - Receive REQ from " + d + ", add to waiting list " + d);
        }
        else {
            RELMessage msg = new RELMessage();
            sendTo(d, msg);
            System.out.println(procId + " - Receive REQ from " + d + ", send REL to " + d);
        }
    }

    // Rule 3 : receive REL from d
    synchronized void receiveREL(int d) {
        nbProcs--;
        System.out.println(procId + " - ReceiveREL from " + d + ", nbProcs = " + nbProcs);
        if (nbProcs == 0) {
            notify();
        }
    }

    // Rule 4
    void endCriticalUse() {
        in = false;
        RELMessage msg = new RELMessage();
        for (int node : a) {
            sendTo(node, msg);
        }
        a.clear();
        displayState();
        putProperty("label", neutre);

    }

    // Access to receive function
    public Message recoit(Door d) {
        return receive(d);
    }

    // Display state
    void displayState() {

        String state = new String("\n");
        state = state + "--------------------------------------\n";
        if (in)
            state = state + "of proc " + procId + "** ACCESS CRITICAL **";
        else if (want)
            state = state + "of proc " + procId + "* WAIT FOR *";
        else
            state = state + "of proc " + procId + "-- SLEEPING --";

        df.display(state);
    }
}
