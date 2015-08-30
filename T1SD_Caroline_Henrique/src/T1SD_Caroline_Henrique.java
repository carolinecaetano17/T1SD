/*
 * Caroline Pessoa Caetano - 408247
 * Henrique Squinello      - 408352
 * Trabalho 1 - Sistemas Distribuídos
 */

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class T1SD_Caroline_Henrique {

    //Define the number of threads
    private final static int THREAD_NUMBER = 5;

    public static void main(String[] args) {

        ArrayList<SDThread> threadPool = new ArrayList<SDThread>();
        ArrayList<DatagramSocket> socketList = new ArrayList<DatagramSocket>();

        //Create one socket for each thread
        for(int i = 0; i < THREAD_NUMBER; i++) {
            try {
                //Bind the socket with the local address, set port and timeout
                InetAddress addr = InetAddress.getByName("127.0.0.1");
                DatagramSocket s = new DatagramSocket(20000 + i, addr);
                s.setSoTimeout(0);
                socketList.add(i, s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Create threads
        for (int i = 0; i < THREAD_NUMBER; i++) {
            SDThread t = new SDThread(i, socketList);
            threadPool.add(t);
        }

        //Start all of them
        for (SDThread t : threadPool)
            t.start();
    }

}
