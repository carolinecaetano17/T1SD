/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author carol_000
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

/**
 *
 * @author carol_000
 */
public class SDThread extends Thread{   
    
    private List<String> threadListOrdered;
    private int acks; 
    private Socket socket = null;
    private ArrayList<SDThread> threadTrack = null;

    //Constructor method
    public SDThread(ArrayList<SDThread> t, Socket s) {
        this.threadListOrdered = new ArrayList<String>();
        acks = 0; 
        this.threadTrack = t;     
        this.socket = s; 
    }
  
    public void run() {
        
        /*Send message in broadcast*/
        
        String inputLine, outputLine;
        PrintWriter client_out;
        BufferedReader client_in;
        
        try {
            //set up IO to client
            client_out = new PrintWriter(socket.getOutputStream(), true);
            client_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            inputLine = client_in.readLine();

            if (threadTrack.size() == 0) {

                outputLine = "No resources available!";

            } else {
                //send inputline in broadcast       
                outputLine = "Resources was available!";
            }
            client_out.println(outputLine);

        } catch (IOException e) {
            e.printStackTrace();
        }

        while ((this.acks < 4) && (this.threadListOrdered.size() < 5) ) {
                
                /*If message (inputline) is not an ACK*/
                    /*Send Ack*/
                    /*Add the message to the threadList and order it by time desc*/
                
                /*Else*/ 
                    /*Message is an Ack update acks*/  
                    this.acks++;
            

            /*Print final list to compare with the list of the others threads,
             they should match*/
            
        }
    }
    
}   
