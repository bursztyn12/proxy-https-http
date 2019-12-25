import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    private Socket socketClientProxy;
    private Socket socketProxyServer;
    private PrintWriter proxyToServer;
    private PrintWriter connectionEstablished;
    private InputStream serverToProxyHttp_Https;
    private OutputStream proxyToClientHttp_Https;
    private InputStream clientToProxyHttps;
    private OutputStream proxyToServerHttps;
    private BufferedReader readFromClient;

    public ClientHandler(Socket socket) {
        this.socketClientProxy = socket;
    }

    @Override
    public void run() {
        System.out.println("------------------START OF THE RUN----------------------");
        requestFromClient();
        System.out.println("-------------------END OF THE RUN----------------");
    }

    public void requestFromClient() {
        System.out.println("----------------REQUEST FROM CLIENT-----------------");
        try {
            readFromClient = new BufferedReader(new InputStreamReader(socketClientProxy.getInputStream()));
            String line1 = "";
            line1 = readFromClient.readLine();
            System.out.println(line1);
            if (line1.contains("CONNECT")) {
                System.out.println(line1);
                List<String>list = processConnect(line1);
                socketProxyServer = new Socket(list.get(0),Integer.parseInt(list.get(1)));
                connectionEstablished = new PrintWriter(socketClientProxy.getOutputStream());
                clientToProxyHttps = socketClientProxy.getInputStream();
                proxyToServerHttps = socketProxyServer.getOutputStream();
                serverToProxyHttp_Https = socketProxyServer.getInputStream();
                proxyToClientHttp_Https = socketClientProxy.getOutputStream();

                System.out.println("***********CONNECT**********");
                System.out.println(line1);
                while (!(line1=readFromClient.readLine()).isEmpty()){
                    System.out.println(line1);
                }
                System.out.println("*************END OF CONNECT**********");
                System.out.println("HTTP/1.1 200 Connection established");
                connectionEstablished.println("HTTP/1.1 200 Connection established");
                connectionEstablished.println("\r\n");
                connectionEstablished.flush();
                new Thread(()->{
                    System.out.println("*********CLIENT THREAD**********");
                    try {
                        readHttpsFromClient();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("************END OF CLIENT THREAD*********");
                }).start();
                Thread.sleep(25);
                new Thread(()->{
                    System.out.println("************SERVER THREAD************");
                    try {
                        readHttpsFromServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("*****************END OF SERVER THREAD************");
                }).start();
            }else if (line1.contains("GET")){
                String address;
                List<String> data;
                if (line1.contains("HTTP/1.0")) {
                    System.out.println("REQUEST FROM CLIENT /1.0");
                    String line = "";
                    address = processGet(line1);
                    socketProxyServer = new Socket(address, 80);
                    data = new ArrayList<>();
                    data.add(line1);
                    line = readFromClient.readLine();
                    while (!line.isEmpty()) {
                        data.add(line);
                        line = readFromClient.readLine();
                    }
                    System.out.println(data);
                    sendHttpToServer(data);
                    readHttpFromServer();
                    closeSocketsAndStreams();
                    System.out.println("CLOSING SOCKET, " + line1 + " FINISHED");
                }else if (line1.contains("HTTP/1.1")) {
                    System.out.println("REQUEST FROM CLIENT /1.1");
                    String line = "";
                    address = processGet(line1);
                    System.out.println(address);
                    socketProxyServer = new Socket(address, 80);
                    data = new ArrayList<>();
                    data.add(line1);
                    while ((line = readFromClient.readLine()) != null) {
                        data.add(line);
                        if (line.isEmpty()) {
                            System.out.println(data);
                            sendHttpToServer(data);
                        }
                    }
                    System.out.println("CLOSING SOCKET, " + line1 + " FINISHED");
                    closeSocketsAndStreams();
                }
            }else{
                System.out.println("PROXY NOT HANDLING THIS METHOD");
            }
            System.out.println("------------END OF REQUEST FROM CLIENT-------------");
        } catch (IOException | NullPointerException | InterruptedException io) {
            System.out.println(io);
        }
    }

    public List<String> processConnect(String line){
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("(.*)(\\s)(.*)(:)(.*)(\\s)(.*)");
        Matcher matcher = pattern.matcher(line);
        String address = "";
        String port = "";
        while (matcher.find()){
            address = matcher.group(3);
            port = matcher.group(5);
        }
        list.add(address);
        list.add(port);
        System.out.println(list);
        return list;
    }
    
    public String processGet(String header) {
        String address = "";
        Pattern pattern = Pattern.compile("(.*)(://)(.*)");
        Matcher matcher = pattern.matcher(header);
        while (matcher.find()) {
            if (matcher.group(2).equals("://")) {
                address = matcher.group(3);
                StringBuilder sb = new StringBuilder();
                if (address.contains("/")) {
                    for (int i = 0; i < address.length(); i++) {
                        if (address.charAt(i) == '/') {
                            break;
                        } else {
                            sb.append(address.charAt(i));
                        }
                    }
                    address = sb.toString();
                }
            }
        }
        return address;
    }

    public void sendHttpToServer(List<String> data) {
        System.out.println("-----------------SENDING REQUEST TO SERVER-----------------");
        try {
            proxyToServer = new PrintWriter(socketProxyServer.getOutputStream(), true);
            for (String line : data) {
                proxyToServer.println(line);
            }
            proxyToServer.println("");
            System.out.println("------------------REQUEST SEND------------------------");
            readHttpFromServer();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void readHttpFromServer(){
        System.out.println("method responsefromserver");
        try {
            System.out.println("---------------------RESPONSE FROM SERVER , SENDING TO PORT : " + socketClientProxy.getPort()+"--------------");
            serverToProxyHttp_Https = socketProxyServer.getInputStream();
            proxyToClientHttp_Https = socketClientProxy.getOutputStream();
            byte[] reply = new byte[1460];
            int bytes_read;
            while ((bytes_read = serverToProxyHttp_Https.read(reply)) != -1) {
                System.out.println("Bytes read : " + bytes_read);
                System.out.println("START");
                System.out.println(new String(reply, StandardCharsets.UTF_8));
                System.out.println("END");
                proxyToClientHttp_Https.write(reply, 0, bytes_read);
                proxyToClientHttp_Https.flush();
            }
            System.out.println("--------------------END OF RESPONSE FROM SERVER-------------------------");
        }catch (IOException io){
            System.out.println("FROM ANSWER " + io);
        }
    }

    public void readHttpsFromClient() throws IOException {
        byte[]buffer = new byte[1460];
        int bytes_read;
        try {
            while ((bytes_read=clientToProxyHttps.read(buffer))>=0){
                System.out.println("Bytes read : " + bytes_read);
                System.out.println("***********START FROM CLIENT****************");
                System.out.println(new String(buffer, StandardCharsets.UTF_8));
                System.out.println("******************END*******************");
                proxyToServerHttps.write(buffer,0,bytes_read);
                proxyToServerHttps.flush();
            }
        }catch (SocketException ex){
            System.out.println(ex);
        }
        closeStreamsHttpsFromClient();
    }

    public void closeStreamsHttpsFromClient(){
        System.out.println("-------------CLOSING STREAMS FROM CLIENT------------");
        try {
            socketClientProxy.close();
            clientToProxyHttps.close();
            proxyToServerHttps.close();
        }catch (IOException io){
            System.out.println(io);
        }
        System.out.println("___________________________________________________");
    }

    public void readHttpsFromServer() throws IOException {
        byte[]buffer = new byte[1460];
        int bytes_read;
        try {
            while ((bytes_read=serverToProxyHttp_Https.read(buffer))>=0){
                System.out.println("Bytes read : " + bytes_read);
                System.out.println("START FROM SERVER");
                System.out.println(new String(buffer, StandardCharsets.UTF_8));
                System.out.println("SOCKET STATE ");
                System.out.println("END");
                proxyToClientHttp_Https.write(buffer,0,bytes_read);
                proxyToClientHttp_Https.flush();
            }
        }catch (SocketException ex){
            System.out.println(ex);
        }
        closeStreamsHttpsFromServer();
    }

    public void closeStreamsHttpsFromServer(){
        System.out.println("---------------CLOSING STREAMS FROM SERVER---------------");
        try {
            socketProxyServer.close();
            serverToProxyHttp_Https.close();
            proxyToClientHttp_Https.close();
        }catch (IOException io){
            System.out.println(io);
        }
        System.out.println("_____________________________________________________________");
    }

    public void closeSocketsAndStreams() throws IOException {
        socketClientProxy.close();
        socketClientProxy.close();
        readFromClient.close();
        proxyToServer.close();
        serverToProxyHttp_Https.close();
        proxyToClientHttp_Https.close();
    }
}
