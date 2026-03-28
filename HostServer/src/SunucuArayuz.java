package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SunucuArayuz extends JFrame {
    private JTextArea logEkrani;
    private JLabel durumEtiketi;
    private static final int PORT = 8080;
    
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    private TrayIcon trayIcon;

    public SunucuArayuz() {
        setTitle("Ana Makine (Sunucu) Kontrol Paneli");
        setSize(700, 500); // Butonlar eklendiği için boyutu biraz büyüttük
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                if (trayIcon != null) {
                    trayIcon.displayMessage(
                        "Sunucu Arka Planda", 
                        "Ana makine çalışmaya devam ediyor. Açmak için sağ alttaki ikona tıklayın.", 
                        TrayIcon.MessageType.INFO
                    );
                }
            }
        });

        // Üst Panel
        JPanel ustPanel = new JPanel();
        ustPanel.setBackground(new Color(240, 240, 240)); 
        durumEtiketi = new JLabel("Durum: SUNUCU KAPALI");
        durumEtiketi.setForeground(Color.RED);
        durumEtiketi.setFont(new Font("Arial", Font.BOLD, 18));
        ustPanel.add(durumEtiketi);
        add(ustPanel, BorderLayout.NORTH);

        // Orta Kısım: Log Ekranı (Beyaz Arka Plan, Siyah Yazı)
        logEkrani = new JTextArea();
        logEkrani.setEditable(false);
        logEkrani.setBackground(Color.WHITE); 
        logEkrani.setForeground(Color.BLACK); 
        logEkrani.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logEkrani.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); 
        JScrollPane scrollPane = new JScrollPane(logEkrani);
        add(scrollPane, BorderLayout.CENTER);

        // Alt Panel: Kontrol Butonları
        JPanel altPanel = new JPanel();
        altPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        altPanel.setBackground(new Color(230, 230, 230));

        JButton btnBaslat = new JButton("Başlat");
        btnBaslat.setBackground(new Color(34, 139, 34)); // Yeşil
        btnBaslat.setForeground(Color.WHITE);
        btnBaslat.setFont(new Font("Arial", Font.BOLD, 14));
        btnBaslat.setFocusPainted(false);

        JButton btnDurdur = new JButton("Durdur");
        btnDurdur.setBackground(new Color(255, 140, 0)); // Turuncu
        btnDurdur.setForeground(Color.WHITE);
        btnDurdur.setFont(new Font("Arial", Font.BOLD, 14));
        btnDurdur.setFocusPainted(false);

        JButton btnYenidenBaslat = new JButton("Yeniden Başlat");
        btnYenidenBaslat.setBackground(new Color(70, 130, 180)); // Mavi
        btnYenidenBaslat.setForeground(Color.WHITE);
        btnYenidenBaslat.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenidenBaslat.setFocusPainted(false);

        JButton btnZorlaKapat = new JButton("Zorla Kapat");
        btnZorlaKapat.setBackground(new Color(220, 20, 60)); // Kırmızı
        btnZorlaKapat.setForeground(Color.WHITE);
        btnZorlaKapat.setFont(new Font("Arial", Font.BOLD, 14));
        btnZorlaKapat.setFocusPainted(false);

        // Buton Olayları (Eventleri)
        btnBaslat.addActionListener(e -> sunucuyuBaslat());
        btnDurdur.addActionListener(e -> durdurmaIstegi());
        btnYenidenBaslat.addActionListener(e -> yenidenBaslatmaIstegi());
        btnZorlaKapat.addActionListener(e -> kapatmaIstegi());

        altPanel.add(btnBaslat);
        altPanel.add(btnDurdur);
        altPanel.add(btnYenidenBaslat);
        altPanel.add(btnZorlaKapat);
        
        add(altPanel, BorderLayout.SOUTH);

        sistemCekmecesiniAyarla();
    }

    public void logYaz(String mesaj) {
        String zaman = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        String logMetni = "[" + zaman + "] " + mesaj;

        SwingUtilities.invokeLater(() -> {
            logEkrani.append(logMetni + "\n");
            logEkrani.setCaretPosition(logEkrani.getDocument().getLength());
        });

        try (PrintWriter pw = new PrintWriter(new FileWriter("sunucu_loglari.txt", true))) {
            pw.println(logMetni);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sunucuyuBaslat() {
        if (isRunning) {
            JOptionPane.showMessageDialog(this, "Sunucu zaten çalışıyor!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            return; 
        }
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                
                SwingUtilities.invokeLater(() -> {
                    durumEtiketi.setText("Durum: SUNUCU AKTİF (Port: " + PORT + ")");
                    durumEtiketi.setForeground(new Color(34, 139, 34)); 
                });
                
                logYaz("SİSTEM: Sunucu " + PORT + " portunda dinlemeye başladı.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    logYaz("BAĞLANTI: Yeni istemci (" + clientSocket.getInetAddress().getHostAddress() + ")");
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                if (isRunning) logYaz("HATA: Sunucu dinleme hatası - " + e.getMessage());
            }
        }).start();
    }

    public void sunucuyuDurdur() {
        if (!isRunning) return;
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            SwingUtilities.invokeLater(() -> {
                durumEtiketi.setText("Durum: SUNUCU DURDURULDU");
                durumEtiketi.setForeground(Color.RED);
            });
            logYaz("SİSTEM: Sunucu manuel olarak DURDURULDU.");
        } catch (IOException e) {
            logYaz("HATA: Sunucu durdurulurken sorun oluştu - " + e.getMessage());
        }
    }

    public void sunucuyuYenidenBaslat() {
        logYaz("SİSTEM: Sunucu yeniden başlatılıyor...");
        sunucuyuDurdur();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        sunucuyuBaslat();
    }

    // --- ONAY (KONTROL) METOTLARI ---
    private void durdurmaIstegi() {
        if (!isRunning) {
            JOptionPane.showMessageDialog(this, "Sunucu zaten kapalı durumda.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int secim = JOptionPane.showConfirmDialog(this, 
            "Sunucuyu durdurmak istediğinize emin misiniz?\nYeni bağlantılar kabul edilmeyecek.", 
            "Sunucuyu Durdur", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (secim == JOptionPane.YES_OPTION) sunucuyuDurdur();
    }

    private void yenidenBaslatmaIstegi() {
        int secim = JOptionPane.showConfirmDialog(this, 
            "Sunucuyu yeniden başlatmak istediğinize emin misiniz?\nMevcut bağlantılar anlık olarak kopabilir.", 
            "Yeniden Başlat", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (secim == JOptionPane.YES_OPTION) sunucuyuYenidenBaslat();
    }

    private void kapatmaIstegi() {
        int secim = JOptionPane.showConfirmDialog(this, 
            "Sistemi tamamen kapatmak istediğinize emin misiniz?\nTüm işlemler durdurulacak ve program sonlandırılacak.", 
            "Sistemi Kapat", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (secim == JOptionPane.YES_OPTION) {
            logYaz("SİSTEM: Ana makine tamamen kapatılıyor...");
            sunucuyuDurdur();
            System.exit(0);
        }
    }

    private void sistemCekmecesiniAyarla() {
        if (!SystemTray.isSupported()) {
            logYaz("UYARI: İşletim sisteminiz System Tray özelliğini desteklemiyor.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(1, 1, 14, 14); 
        g2d.setColor(Color.BLACK);
        g2d.drawLine(2, 5, 13, 5); 
        g2d.drawLine(2, 10, 13, 10); 
        g2d.setColor(Color.GREEN);
        g2d.fillRect(3, 2, 2, 2); 
        g2d.fillRect(3, 7, 2, 2); 
        g2d.fillRect(3, 12, 2, 2); 
        g2d.dispose();

        PopupMenu popup = new PopupMenu();

        MenuItem gosterItem = new MenuItem("Arayuzu Goster / Gizle");
        gosterItem.addActionListener(e -> setVisible(!isVisible()));

        // Menüdeki butonlar da artık ortak onay metotlarını çağırıyor
        MenuItem durdurItem = new MenuItem("Sunucuyu Durdur");
        durdurItem.addActionListener(e -> durdurmaIstegi());

        MenuItem restartItem = new MenuItem("Yeniden Baslat");
        restartItem.addActionListener(e -> yenidenBaslatmaIstegi());

        MenuItem kapatItem = new MenuItem("Sistemi Zorla Kapat");
        kapatItem.addActionListener(e -> kapatmaIstegi());

        popup.add(gosterItem);
        popup.addSeparator();
        popup.add(durdurItem);
        popup.add(restartItem);
        popup.addSeparator();
        popup.add(kapatItem);

        trayIcon = new TrayIcon(image, "Sunucu Kontrol Paneli", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> setVisible(true)); 

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            logYaz("HATA: Tray ikonu eklenemedi.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SunucuArayuz anaEkran = new SunucuArayuz();
            anaEkran.setVisible(true); 
            anaEkran.logYaz("SİSTEM: Ana makine başlatıldı. Veritabanı kontrol ediliyor...");
            DatabaseManager.initialize();
            anaEkran.sunucuyuBaslat();
        });
    }
}