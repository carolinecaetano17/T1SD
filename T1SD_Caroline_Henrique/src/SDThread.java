/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author carol_000
 */

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * @author carol_000
 */
public class SDThread extends Thread {
    //Keeps track of every message received
    private List<Pair<String, Integer>> messageList;
    //Keeps track of each message's ack number
    private List<Pair<String, Integer>> ackList;

    //Thread maintenance variables
    private int sequenceNumber;
    private List<DatagramSocket> clientList;
    private int ackNumber;
    private int messagesReceived;
    private final int pID;
    private final int totalMessagesToSend;
    private Random r;

    //To pass Pair objects directly, we use these classes
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    //Starts the thread and its server Socket, listening on given port
    public SDThread(int pID, List<DatagramSocket> clientList) {
        this.messageList = new ArrayList<Pair<String, Integer>>();
        this.ackList = new ArrayList<Pair<String, Integer>>();

        this.clientList = clientList;
        this.ackNumber = 0;
        this.sequenceNumber = 0;
        this.pID = pID;
        this.messagesReceived = 0;
        this.totalMessagesToSend = 1;

        this.r = new Random();
    }

    //Sends a multicast message
    public void send(String message) {
        //New event, increment sequence number
        sequenceNumber = sequenceNumber + 1;

        //This is done so we can achieve total ordering between processes,
        //appending their unique ID to the sequence number
        int mID = (sequenceNumber * 10) + pID;

        Pair<String, Integer> outMessage = new Pair<String, Integer>(message, mID);
        for (int i = 0; i < clientList.size(); i++) {
            if (i != pID) {
                DatagramSocket s = clientList.get(i);
                //Send the message to every client connected to this process
                try {
                    s.send(new DatagramPacket(serialize(outMessage), serialize(outMessage).length,
                            s.getLocalAddress(), s.getLocalPort()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //Send message to itself
        receive(outMessage);
    }

    //Sends a multicast acknowledgement of the message and adds it to the message list
    private void receive(Pair message) {
        //Receive event, adjust clock (we divide the message ID by 10 to remove the process ID from it)
        sequenceNumber = 1 + Math.max(sequenceNumber, ((int) message.getId() / 10));

        //Prepare the ack message to be sent
        Pair<String, Integer> ackMessage = new Pair<String, Integer>("ACK", (Integer) message.getId());

        for (DatagramSocket s : clientList) {
            //Send ack message to every client connected to this process
            try {
                s.send(new DatagramPacket(serialize(ackMessage), serialize(ackMessage).length,
                        s.getLocalAddress(), s.getLocalPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Insert the message on the list and reorder it
        messageList.add(message);
        Collections.sort(messageList, mIDComparator);

        //Keep track of how many messages are left
        messagesReceived++;

        return;
    }

    //Processes ack messages, eventually releasing messages from the message list
    private void ack(Pair message) {
        //New event, increment sequence number
        sequenceNumber = sequenceNumber + 1;

        String mID = message.getId().toString();

        //Search for the mID in the ackList. If found,
        //increment its counter, otherwise add it to the list
        for (int i = 0; i < ackList.size(); i++) {
            if (ackList.get(i).getMessage().equals(mID)) {
                Pair<String, Integer> ack = ackList.remove(i);
                ack.setId(ack.getId() + 1);
                ackList.add(i, ack);
                //Since the ack has been found,
                //check the list to see if any messages can be released or not
                checkAck();
                return;
            }
        }
        //mID not found, add it to the list and reorder
        ackList.add(new Pair<String, Integer>(mID, 1));
        Collections.sort(ackList, ackComparator);
        return;
    }

    public void checkAck() {
        boolean stop = false;
        //Since both lists are ordered, we can just check to see
        //if the first message can be released. If so, repeat
        //the process, releasing messages with enough ACKs
        //until we have no messages, or find one needing more
        //ACKs
        while (!stop && ackList.size() != 0) {
            Pair<String, Integer> message = ackList.get(0);
            if (message.getId() == clientList.size()) {
                System.out.println(messageList.remove(0).getMessage() + " Freed by thread " + pID);
                ackList.remove(0);
            } else {
                stop = true;
            }
        }
        return;
    }

    public void run() {
        int messagesSent = 0;
        byte[] data = new byte[1000000];
        System.out.println("Ahi");

        DatagramPacket packet = new DatagramPacket(data, 1000000);

        while (true) {
            //If we have already sent and received every message
            if (messagesReceived == (int) Math.pow(totalMessagesToSend * clientList.size(), 2)) {
                for(DatagramSocket ds : clientList)
                    ds.close();
                return;
            }

            //Send some messages
            if (messagesSent != totalMessagesToSend) {
                send("Message " + messagesSent + " sent by thread " + pID);
                messagesSent++;
                System.out.println("sending");
            }

            //Listen to own socket for data
            try {
                clientList.get(pID).receive(packet);
                if (packet.getLength() != 0) {
                    try {
                        Pair<String, Integer> m = (Pair<String, Integer>) deserialize(packet.getData());
                        if (m.getMessage().equals("ACK"))
                            ack(m);
                        else {
                            receive(m);
                        }
                    } catch (Exception e) {
                        System.out.println("Log: stream header.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Log: tempo de recebimento esgotado thread " + pID);
            }
        }
    }


    //Comparator classes for sorting
    public static Comparator<Pair<String, Integer>> mIDComparator = new Comparator<Pair<String, Integer>>() {
        public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
            Integer mID1 = p1.getId();
            Integer mID2 = p2.getId();

            //ascending order
            return mID1 - mID2;

        }
    };

    public static Comparator<Pair<String, Integer>> ackComparator = new Comparator<Pair<String, Integer>>() {
        public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
            String m1 = p1.getMessage().toUpperCase();
            String m2 = p2.getMessage().toUpperCase();

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

    private X message;
    private Y id;

    public Pair() {
    }

    public Pair(X message, Y id) {
        this.message = message;
        this.id = id;
    }

    public X getMessage() {
        return message;
    }

    public Y getId() {
        return id;
    }

    public void setId(Y id) {
        this.id = id;
    }

}