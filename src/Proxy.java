import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Proxy extends Thread{
    private ServerSocket serverSocket;
    static boolean finish;

    public Proxy(int port)throws IOException {
        serverSocket = new ServerSocket(port);
        finish = true;
    }

    public void proxyOn()throws InterruptedException {
        System.out.println("Proxy On");
        while (true){
            Thread thread = new Thread(()->{
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println(clientSocket + " is connected on port : " + clientSocket.getPort());
                    Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                    clientHandler.start();
                }catch (IOException io){
                    System.out.println(io);
                }
            });
            thread.start();
            Thread.sleep(60);
        }
    }

    public static void main(String[]args) throws IOException, InterruptedException {
        new Proxy(80).proxyOn();
    }
}
