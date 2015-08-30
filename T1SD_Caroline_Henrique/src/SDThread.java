/*
 * Caroline Pessoa Caetano - 408247
 * Henrique Squinello      - 408352
 * Trabalho 1 - Sistemas Distribuídos
 */

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class SDThread extends Thread {

    //Thread maintenance variables
    private int sequenceNumber;
    private List<DatagramSocket> clientList;
    private final int pID;
    //Only one number generator for all threads
    private static Random r = new Random();

    //Initialize the thread with the given information
    public SDThread(int pID, List<DatagramSocket> clientList) {
        this.clientList = clientList;
        //Start the sequence number at different places for different threads
        this.sequenceNumber = r.nextInt(4);
        this.pID = pID;
    }

    //Send a multicast message from the current socket
    public void send(String message) {

        //New event, increment sequence number
        sequenceNumber = sequenceNumber + 1;

        //This is done so we can achieve total ordering between processes,
        //appending their unique ID to the sequence number
        int mID = (sequenceNumber * 10) + pID;

        //Create the pair to be send in broadcast
        Pair<String, Integer> outMessage = new Pair<String, Integer>(message, mID);

        //Send the message to all threads
        for (int i = 0; i < clientList.size(); i++) {
            DatagramSocket s = clientList.get(i);
            try {
                s.send(new DatagramPacket(serialize(outMessage), serialize(outMessage).length,
                        s.getLocalAddress(), s.getLocalPort()));
            } catch (IOException e) {
                System.out.println("Log: Error sending message, from " + pID + " to " + i + "; Retrying");
                e.printStackTrace();
            }
        }
    }

    //Send an acknowledgement of the message and update the message list
    private List<Pair<String, Integer>> receive(Pair message, List<Pair<String, Integer>> messageList) {

        //Message received, adjust clock the following way:
        //1. Append the process ID to the sequence number for a fair comparison (see send function)
        //2. Store the highest sequence number WITHOUT the process specific ID
        //This is because the tie has already been broken, no need for the pID
        sequenceNumber = 1 + Math.max(sequenceNumber * 10 + pID, ((int) message.getQnt()));
        sequenceNumber = sequenceNumber / 10;

        //Retrieve the ID of the thread who sent the message
        int tID = (Integer) message.getQnt() % 10;
        
        //Prepare the ack message to be sent
        Pair<String, Integer> ackMessage = new Pair<String, Integer>("ACK", (Integer) message.getQnt());

        DatagramSocket s = clientList.get(tID);

        //If the message wasn't sent by itself, send an ACK back to itself
        if (tID != pID) {
            try {
                s.send(new DatagramPacket(serialize(ackMessage), serialize(ackMessage).length,
                        s.getLocalAddress(), s.getLocalPort()));
            } catch (IOException e) {
                System.out.println("Log: Error sending ACK, from thread " + pID + " to " + tID);
            }
        }

        //Insert the message in the list and reorder it
        messageList.add(message);
        Collections.sort(messageList, mIDComparator);
        return messageList;
    }

    //Processes ack messages
    private List<Pair<String, Integer>> ack(Pair message, List<Pair<String, Integer>> ackList) {

        //Message received, adjust clock the same way as receive()
        sequenceNumber = 1 + Math.max(sequenceNumber * 10 + pID, ((int) message.getQnt()));
        sequenceNumber = sequenceNumber / 10;

        //Message IDs are stored as strings in the ackList
        String mID = message.getmID().toString();

        //Search for the mID in the ackList. If found,
        //increment its counter, otherwise add it to the list
        for (int i = 0; i < ackList.size(); i++) {
            if (ackList.get(i).getmID().equals(mID)) {
                Pair<String, Integer> ack = ackList.remove(i);
                ack.setQnt(ack.getQnt() + 1);
                ackList.add(ack);
                Collections.sort(ackList, ackComparator);
                return ackList;
            }
        }

        //mID not found, add it to the list and reorder
        ackList.add(new Pair<String, Integer>(mID, 1));
        Collections.sort(ackList, ackComparator);
        return ackList;
    }

    public void run() {

        //Keep track of every message received
        List<Pair<String, Integer>> messageList = new ArrayList<Pair<String, Integer>>();

        //Keep track of each message's ack quantity
        List<Pair<String, Integer>> ackList = new ArrayList<Pair<String, Integer>>();


        //Variables to keep track of the thread progress
        int messagesReceived = 0;
        int totalMessagesToSend = 3;
        int messagesSent = 0;
        byte[] data = new byte[10000000];

        //Create the packet to be sent
        DatagramPacket packet = new DatagramPacket(data, 10000000);

        while (true) {

            //If we have already sent and received every message we are done
            //Rationale: clientList - 1 ACKs per message +
            //           clientList messages received from each thread (including itself)
            if (messagesReceived == (totalMessagesToSend * (clientList.size() - 1)) +
                    totalMessagesToSend * clientList.size()) {
                /*
                  System.out.println(" List from Thread " + pID + " : ");
                
                  for(int i = 0; i < messageList.size(); i++){
                      System.out.println(messageList.get(i).getmID());
                  }
                  return;
                */

                //Write the list to a thread specific file for easy diffing
                try {
                    PrintWriter writer = new PrintWriter("thread" + pID + ".txt", "UTF-8");
                    for (int i = 0; i < messageList.size(); i++) {
                        writer.println(messageList.get(i).getmID());
                    }
                    writer.close();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                clientList.get(pID).close();
            }

            //Send a message and update the quantity of messages sent by this thread
            if (messagesSent < totalMessagesToSend) {
                send("Message " + messagesSent + " sent by thread " + pID + " with sequence number " + sequenceNumber);
                messagesSent++;
            }

            //Listen to own socket for data
            try {
                clientList.get(pID).receive(packet);
                if (packet.getLength() != 0) {
                    try {
                        Pair<String, Integer> m = (Pair<String, Integer>) deserialize(packet.getData());

                        //If it is an ACK
                        if (m.getmID().equals("ACK")) {
                            ackList = ack(m, ackList);

                            //If it is a message with content
                        } else {
                            messageList = receive(m, messageList);
                        }

                        //Keep track of how many messages are left
                        messagesReceived++;

                    } catch (Exception e) {
                        System.out.println("Log: stream header corrupted");
                    }
                }
            } catch (IOException e) {
                System.out.println("Log: timeout on thread " + pID);
            }
        }
    }


    //Compare Messages for sorting
    public static Comparator<Pair<String, Integer>> mIDComparator = new Comparator<Pair<String, Integer>>() {
        public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
            Integer mID1 = p1.getQnt();
            Integer mID2 = p2.getQnt();

            //ascending order
            return mID1 - mID2;

        }
    };

    public static Comparator<Pair<String, Integer>> ackComparator = new Comparator<Pair<String, Integer>>() {
        public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
            String m1 = p1.getmID().toUpperCase();
            String m2 = p2.getmID().toUpperCase();

            //ascending order
            return m1.compareTo(m2);

        }
    };

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }
}

//Utility class that stores a pair of string and integer that is serializable
class Pair<X extends Serializable, Y extends Serializable> implements Serializable {

    private X mID;

    //Quantity of acks received to the message with the mId = this.mID
    private Y qnt;

    public Pair() {
    }

    public Pair(X mID, Y qnt) {
        this.mID = mID;
        this.qnt = qnt;
    }

    public X getmID() {
        return mID;
    }

    public Y getQnt() {
        return qnt;
    }

    public void setQnt(Y qnt) {
        this.qnt = qnt;
    }

}