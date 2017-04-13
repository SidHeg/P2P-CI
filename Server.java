import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;


class RFC {
    int number;
    String fileName;
    String hostId;
    int hostPort;
}

class PeerRecord {
    String ipAddress;
    int portNumber;
    String name;
    
}

public class Server extends Thread{
    private static LinkedList<RFC> RFCList = new LinkedList<>();
    private static LinkedList<PeerRecord> peerList = new LinkedList<>();
    private Socket clientSocket;
    
    public Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        try {          
            HandleRequest(clientSocket);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }
    }
    
    private void HandleRequest(Socket clientSocket) throws IOException {
        String ipAddress = clientSocket.getInetAddress().getHostAddress();
        System.out.println();
        System.out.println("Request to Central index from "+ ipAddress);
        System.out.println();
        String request = new DataInputStream(clientSocket.getInputStream()).readUTF();
        System.out.println(request);
        String[] input = request.split("\\s");
/*        for(int i = 0; i < input.length; i++){
            System.out.println(input[i]);
        }*/
        String status = input[0].trim();
        status = status.toUpperCase();
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        if(status.equals("ADD")) {
            output.writeUTF(Add(input));
        } else if(status.equals("LOOKUP")) {
            output.writeUTF(LookUp(input));
        } else if(status.equals("LIST")) {
            output.writeUTF(List());
        }
    }

    private static String Add(String[] input) {
        RFC rfc = new RFC();
        PeerRecord peer = new PeerRecord();
        rfc.hostId = peer.ipAddress = input[5];
        peer.portNumber = rfc.hostPort = Integer.parseInt(input[7].trim());
        rfc.number = Integer.parseInt(input[2].trim());
        rfc.fileName = input[9].trim();
        synchronized(Server.class) {
            RFCList.add(rfc);
        }
        System.out.println("Added " + rfc.fileName + " to the index.");
        return "SUCCESS";
    }
    
    private static synchronized String LookUp(String[] input) {
        for(RFC rfc : RFCList){
            if (rfc.number == Integer.parseInt(input[2])){
                String output = "SUCCESS: " + rfc.hostId + " " + rfc.hostPort + " " + rfc.number + " " + rfc.fileName;
                return output;
            }
        }
        String output = "FAIL: Not Available";
        return output;
    }
    
    private static synchronized String List() {
        String output = "";
        for(RFC rfc : RFCList){
            output = output + "\n" + rfc.hostId + " " + rfc.hostPort + " " + rfc.number + " " + rfc.fileName;
        }
        if (!output.equals("")){
            output = "SUCCESS: " + output;
        } else {
            output = "FAIL: Not Available";
        }
        return output;
    }

    public static void main(String[] args) {
        int port = 7734;
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Server started on " + InetAddress.getLocalHost());
            while(true) {
                Server t = new Server(server.accept());
                t.start();
            }
        } catch(IOException e) {
            System.out.println(e);
        }
    }
}