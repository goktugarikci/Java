package src;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
public class MainServer {
    private static final int PORT = 8080; // Dinlenecek port adresi

    public static void main(String[] args) {
        // Önce veritabanını hazırla
        DatabaseManager.initialize();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Sunucu " + PORT + " portunda başlatıldı. İstemciler bekleniyor...");

            while (true) {
                // İstemci bağlanana kadar burada bekler
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bir istemci bağlandı: " + clientSocket.getInetAddress().getHostAddress());

                // Her bağlanan istemci için yeni bir thread başlat (Aynı anda birden fazla kişiye hizmet verebilmek için)
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Sunucu başlatılamadı: " + e.getMessage());
        }
    }
}