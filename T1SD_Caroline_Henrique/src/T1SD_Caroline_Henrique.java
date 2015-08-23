/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;

/**
 * @author carol_000
 */
public class T1SD_Caroline_Henrique {
    private final static int THREAD_NUMBER = 3;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArrayList<SDThread> threadPool = new ArrayList<SDThread>();
        ArrayList<DatagramSocket> socketList = new ArrayList<DatagramSocket>();

        //We create one socket for each thread
        for(int i = 0; i < THREAD_NUMBER; i++) {
            try {
                DatagramSocket s = new DatagramSocket(20000+i);
                s.setSoTimeout(2000);
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
