import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client extends Thread{

    private String status;
    private String path = "C:\\Users\\siddh\\Desktop";
    private Socket ClientSocket;
    private String ipAddress;
    
    public Client(String status) throws IOException{
        if(status.toUpperCase() == "SERVER"){
            throw new IOException("File path missing: Enter file path");
        } else if(status.toUpperCase() == "CLIENT"){
            this.status = status.toUpperCase();
        } else {
            throw new IOException("Invalid Status| allowed: SERVER or CLIENT");
        }
    }
    
    public Client(String status, String path, Socket ClientSocket, String ipAddress) throws IOException{
        if(status.toUpperCase() == "SERVER"){
            this.status = status.toUpperCase();
            this.path = path;
            this.ClientSocket = ClientSocket;
            this.ipAddress = ipAddress;
        } else {
            throw new IOException("Invalid Status| allowed: SERVER or CLIENT");
        }
    }
    
    @Override
    public void run() {
        if (status == "SERVER") {
            try {
                String ipAddress = ClientSocket.getInetAddress().getHostAddress();
                System.out.println();
                System.out.println("Request to Peer from "+ ipAddress);
                System.out.println();
                String request = new DataInputStream(ClientSocket.getInputStream()).readUTF();
                System.out.println(request);
                String[] input = request.split("\\s");
                DataOutputStream output = new DataOutputStream(ClientSocket.getOutputStream());
                P2PServer(input, path, output);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (status == "CLIENT") {
            try {
                CIClient();
            } catch (NumberFormatException | IOException e) {
                System.out.println("Start the server before starting the client");
                e.printStackTrace();
            }
        }
    }
    
    private static void P2PServer(String[] input, String path, DataOutputStream output) throws IOException {
        
        String response;        
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date dateobj = new Date();
        String date = df.format(dateobj).toString();
        Path FullFileName = Paths.get(path + "\\" + input[2] + ".txt");
        File file = FullFileName.toFile();
        
        synchronized(Client.class){
            if(file.exists()) {
                BasicFileAttributes FileAttributes = Files.readAttributes(FullFileName, BasicFileAttributes.class);
                response = "P2P-CI/1.0 200 OK\n";
                response = response + date + " " + "\n";
                response = response + "OS: " + System.getProperty("os.name") + "\n";
                response = response + "Last-Modified: " + FileAttributes.lastModifiedTime() + "\n";
                response = response + "Content-Length: " +  + FileAttributes.size() + "\n";
                response = response + "Content-Type: text/text\n";
            } else {
                response = "P2P-CI/1.0 404 Not Found\n";
                response = response + "Date: " + date + " " + "\n";
                response = response + "OS: " + System.getProperty("os.name") + "\n";
                synchronized(Client.class){
                    output.writeUTF(response);
                }
                return;
            }
        }
        
        BufferedReader br = new BufferedReader(new FileReader(file));
        String CurrentLine;
        while ((CurrentLine = br.readLine()) != null) {
            response += CurrentLine;
        }
        synchronized(Client.class){
            output.writeUTF(response);
        }
        br.close();
        return;
    }
    
    
    private void CIClient() throws NumberFormatException, IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            
            String request = "";
            String response = "Didn't work";
            int choice = 0;
            int rfc;
            String title;
            int port = 7735;
            String PeerIPAddress;
            DataOutputStream MessageToServer = null;
            DataInputStream MessageFromServer = null;
            DataOutputStream MessageToPeer = null;
            DataInputStream MessageFromPeer = null;
            Socket client = null;
            Socket peer = null;
            try {
                client = new Socket(InetAddress.getByName(this.ipAddress), 7734);
                MessageToServer = new DataOutputStream(client.getOutputStream());
                MessageFromServer = new DataInputStream(client.getInputStream());
                System.out.println("\n1: ADD 2: LOOKUP 3: LIST 4: DOWNLOAD 5: EXIT ");
                choice = Integer.parseInt(br.readLine());
            } catch (IOException e) {
                    System.out.println(e);
            }
            switch(choice) {
                case 1:
                    System.out.println("Enter number of RFC to be Added");
                    rfc = Integer.parseInt(br.readLine());
                    System.out.println("Enter title of RFC to be Added, no space allowed, use '_'");
                    title = br.readLine().trim();
                    request = "ADD RFC " + rfc + " P2P-CI/1.0";
                    request = request + "\n" + "Host: " + InetAddress.getLocalHost().getHostAddress();
                    request = request + "\n" + "Port: " + port;
                    request = request + "\n" + "Title: " + title; //+                   
                    MessageToServer.writeUTF(request);                    
                    response = MessageFromServer.readUTF();
                    System.out.println("Message received from Central Server: " + response);
                    break;
                case 2:
                    System.out.println("Enter RFC number to LOOKUP");
                    rfc = Integer.parseInt(br.readLine());
                    request = "LOOKUP RFC " + rfc + " P2P-CI/1.0";
                    request = request + "\n" + "Host: " + InetAddress.getLocalHost().getHostAddress();
                    request = request + "\n" + "Port: " + port;
                    MessageToServer.writeUTF(request);
                    response = MessageFromServer.readUTF();
                    System.out.println("STATUS HOST_IP HOST_PORT RFC_NUMBER RFC_TITLE");
                    System.out.println(response);
                    break;
                case 3:
                    request = "LIST ALL P2P-CI/1.0";
                    request = request + "\n" + "Host: " + InetAddress.getLocalHost().getHostAddress();
                    request = request + "\n" + "Port: " + port;                    
                    MessageToServer.writeUTF(request);           
                    response = MessageFromServer.readUTF();
                    //System.out.println("STATUS HOST_IP HOST_PORT RFC_NUMBER RFC_TITLE");
                    System.out.println(response);
                    break;                    
                case 4:
                    System.out.println("Enter the RFC Number to Download");
                    rfc = Integer.parseInt(br.readLine());
                    System.out.println("Enter the IP Address of the Peer, default P2P port: 7735");
                    PeerIPAddress = br.readLine().trim();
                    request = "GET RFC " + rfc + " P2P-CI/1.0";
                    request = request + "\nHost: " + InetAddress.getLocalHost().getHostAddress();
                    request = request + "\nOS: " + System.getProperty("os.name");
                    try {
                        peer = new Socket(InetAddress.getByName(PeerIPAddress), port);
                        MessageToPeer = new DataOutputStream(peer.getOutputStream());
                        MessageFromPeer = new DataInputStream(peer.getInputStream());
                        MessageToPeer.writeUTF(request);           
                        response = MessageFromPeer.readUTF();
                        //System.out.println(response);
                        for(int i = 0; i < 6; i++){
                            response = response.substring(response.indexOf("\n")+1);
                        }
                        //System.out.println(response);
                        // change the path to path variable
                        String FileName = path + "\\" + rfc +".txt";
                        WriteTofile(response, FileName);
                        System.out.println("Imported file successfully.\nFile: " + FileName);
                    } catch (IOException e) {
                        System.out.println("Peer not online");
                    }
                    break;   
                case 5:
                    
                    break;
                default:
                    System.out.println("Invalid Choice. Try again");
            }
        }
    }
    
    private static synchronized void WriteTofile(String response, String FileName) throws FileNotFoundException {
        try {
            File file = new File(FileName);
            if (file.createNewFile()){
                PrintWriter out = new PrintWriter(FileName);
                out.println(response);
                out.close();
            }
        } catch (IOException e){
            System.out.println(e);
        }
    }
    
    
    public static void main(String[] args) throws IOException {
        Client client = new Client("CLIENT");
        int clientServerPort = 7735;
        ServerSocket ClientSocket = new ServerSocket(clientServerPort);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the ipaddress of the server");       
        String ipAddress = br.readLine();
        client.start();
        while(true) {
            Client c1 = new Client("SERVER", "C:\\Users\\siddh\\Downloads", ClientSocket.accept(), ipAddress);
            c1.start();
        }
    }
}
