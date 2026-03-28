
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GirisSecimEkrani extends JFrame {

    private JLabel lblSaat, lblSunucuDurum;
    private Timer saatTimer;

    public GirisSecimEkrani() {
        // 1. Pencere Başlığını Kaldır
        setUndecorated(true);
        
        // 2. Önemli: Resizable true olmalı ki Windows simge durumundan geri getirebilsin
        setResizable(true);
        
        // 3. Tam Ekran Yap
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new BorderLayout());

        // --- SIMGE DURUMU SORUNU İÇİN ÖZEL DİNLEYİCİ ---
        this.addWindowStateListener(e -> {
            if ((e.getOldState() & Frame.ICONIFIED) != 0 && (e.getNewState() & Frame.ICONIFIED) == 0) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        // --- ESC TUŞU İLE ÇIKIŞ KISAYOLU ---
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "kapat");
        this.getRootPane().getActionMap().put("kapat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        // --- ÜST KONTROL BARI ---
        JPanel ustKontrolBar = new JPanel(new BorderLayout());
        ustKontrolBar.setBackground(new Color(45, 45, 45));
        ustKontrolBar.setPreferredSize(new Dimension(0, 40));

        JLabel lblBaslik = new JLabel("  SİSTEM GİRİŞ PANELİ (ESC ile Kapatabilirsin)");
        lblBaslik.setForeground(Color.LIGHT_GRAY);
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 12));
        ustKontrolBar.add(lblBaslik, BorderLayout.WEST);

        JPanel sagButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sagButonlar.setOpaque(false);

        JButton btnSimge = butonOlustur("_", new Color(60, 60, 60));
        JButton btnKapat = butonOlustur("X", new Color(180, 40, 40));

        btnSimge.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));
        btnKapat.addActionListener(e -> System.exit(0));

        sagButonlar.add(btnSimge);
        sagButonlar.add(btnKapat);
        ustKontrolBar.add(sagButonlar, BorderLayout.EAST);

        add(ustKontrolBar, BorderLayout.NORTH);

        // --- ORTA PANEL (DEV BUTONLAR) ---
        JPanel ortaPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        ortaPanel.setBackground(new Color(30, 30, 30));

        JButton btnYetkili = anaButonOlustur("YETKİLİ GİRİŞİ", new Color(70, 130, 180));
        JButton btnPersonel = anaButonOlustur("PERSONEL GİRİŞİ", new Color(46, 139, 87));

        // --- KRİTİK DÜZELTME BURASI: this.dispose() EKLENDİ ---
        btnYetkili.addActionListener(e -> {
            new LoginEkrani("Admin").setVisible(true); // LoginEkrani 'Admin' veya 'Personel' bekliyor
            this.dispose(); // Seçim ekranını kapat
        });

        btnPersonel.addActionListener(e -> {
            new LoginEkrani("Personel").setVisible(true);
            this.dispose(); // Seçim ekranını kapat
        });

        ortaPanel.add(btnYetkili);
        ortaPanel.add(btnPersonel);
        add(ortaPanel, BorderLayout.CENTER);

        // --- ALT BİLGİ ÇUBUĞU ---
        JPanel altBar = new JPanel(new BorderLayout());
        altBar.setBackground(new Color(45, 45, 45));
        altBar.setPreferredSize(new Dimension(0, 35));
        altBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));

        lblSaat = new JLabel();
        lblSaat.setForeground(Color.WHITE);
        lblSaat.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblSaat.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        lblSunucuDurum = new JLabel("SUNUCU: BAĞLANILIYOR... ");
        lblSunucuDurum.setForeground(Color.YELLOW);
        lblSunucuDurum.setFont(new Font("Arial", Font.BOLD, 12));
        lblSunucuDurum.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        altBar.add(lblSaat, BorderLayout.WEST);
        altBar.add(lblSunucuDurum, BorderLayout.EAST);
        add(altBar, BorderLayout.SOUTH);

        saatBaslat();
        sunucuKontrolBaslat();
    }

    private void saatBaslat() {
        saatTimer = new Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss");
            lblSaat.setText(sdf.format(new Date()));
        });
        saatTimer.start();
    }

    private void sunucuKontrolBaslat() {
        // İkinci Düzeltme: Thread'i Daemon (Arka plan) yaptık. 
        // Böylece pencere kapanınca bu döngü RAM'de sonsuza dek çalışmaya devam etmez.
        Thread t = new Thread(() -> {
            while (true) {
                try (Socket s = new Socket("localhost", 8080)) {
                    lblSunucuDurum.setText("● SUNUCU AKTİF ");
                    lblSunucuDurum.setForeground(new Color(50, 205, 50));
                } catch (Exception e) {
                    lblSunucuDurum.setText("○ SUNUCU BAĞLANTISI KOPUK ");
                    lblSunucuDurum.setForeground(Color.RED);
                }
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        });
        t.setDaemon(true); 
        t.start();
    }

    private JButton anaButonOlustur(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 45));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JButton butonOlustur(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 40));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GirisSecimEkrani().setVisible(true));
    }
}