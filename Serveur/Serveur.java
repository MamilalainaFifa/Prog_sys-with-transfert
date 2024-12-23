import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Serveur {
    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("../Configuration/config.properties")) {
            config.load(input);
        } catch (IOException ex) {
            System.err.println("Erreur lors de la lecture du fichier config.properties : " + ex.getMessage());
            return;
        }

        int port = Integer.parseInt(config.getProperty("server.image.port", "5000"));
        int controlPort = Integer.parseInt(config.getProperty("server.control.port", "5001"));
        int maxClients = Integer.parseInt(config.getProperty("max.clients", "10"));

        ExecutorService clientPool = Executors.newFixedThreadPool(maxClients);

        try (ServerSocket serverSocket = new ServerSocket(port);
             ServerSocket controlSocket = new ServerSocket(controlPort)) {

            System.out.println("Serveur en attente de connexions...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Socket controlClientSocket = controlSocket.accept();
                System.out.println("Connexion établie avec un client.");

                clientPool.execute(new ClientHandler(clientSocket, controlClientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientPool.shutdown();
        }
    }
}
