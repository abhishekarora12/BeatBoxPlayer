package beatboxserver;

import java.io.*;
import java.util.*;
import java.net.*;

/**
 * @author Abhishek Arora
 */
public class BeatBoxServer {
    ArrayList<ObjectOutputStream> clientOutputStreams;
    
    public static void main(String[] args) {
        new BeatBoxServer().go();
    }
    
    public void go() {
        clientOutputStreams = new ArrayList<ObjectOutputStream>();
        
        try {
            ServerSocket serverSock = new ServerSocket(4242);
            
            while(true) {
                Socket clientSocket = serverSock.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);
                
                Thread t =  new Thread(new ClientHandler(clientSocket));
                t.start();
                
                System.out.println("Got a connection");
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public class ClientHandler implements Runnable {
        ObjectInputStream in;
        Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            try {
                clientSocket = socket;
                in = new ObjectInputStream(clientSocket.getInputStream());
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        
        public void run() {
            Object o1 = null;
            Object o2 = null;
            
            try {
                while ( (o1 = in.readObject()) != null ) {
                    o2 = in.readObject();
                    
                    System.out.println("Read two objects");
                    tellEveryone(o1, o2);
                }
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        
        public void tellEveryone(Object one, Object two) {
            Iterator it = clientOutputStreams.iterator();
            while(it.hasNext()) {
                try {
                    ObjectOutputStream out = (ObjectOutputStream) it.next();
                    out.writeObject(one);
                    out.writeObject(two);
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
}
