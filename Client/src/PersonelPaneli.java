
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PersonelPaneli extends JFrame {
    private JPanel icerikPaneli;
    private CardLayout cardLayout;
    private String aktifPersonel;
    private String aktifRol;

    // Vestiyer Bileşenleri
    private DefaultTableModel vestiyerTableModel;
    private JTable vestiyerTablosu;

    public PersonelPaneli(String adSoyad, String rol) {
        this.aktifPersonel = adSoyad;
        this.aktifRol = rol;

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ustBarAyarla();
        solMenuAyarla();

        cardLayout = new CardLayout();
        icerikPaneli = new JPanel(cardLayout);

        // Ekranları Oluştur ve Ekle
        icerikPaneli.add(kasaVeSiparisSayfasi(), "Kasa ve Sipariş");
        icerikPaneli.add(mutfakSayfasi(), "Mutfak Panosu");
        icerikPaneli.add(vestiyerSayfasi(), "Vestiyer Modülü");

        add(icerikPaneli, BorderLayout.CENTER);

        // Rol tabanlı başlangıç ekranı yönlendirmesi
        if (aktifRol.equalsIgnoreCase("Mutfak")) {
            cardLayout.show(icerikPaneli, "Mutfak Panosu");
        } else {
            cardLayout.show(icerikPaneli, "Kasa ve Sipariş");
        }
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(39, 174, 96)); // Personel ekranı yeşil tonlarında
        ustBar.setPreferredSize(new Dimension(0, 50));
        
        JLabel lblBaslik = new JLabel("  RESTORAN OTOMASYONU | Personel: " + aktifPersonel + " (" + aktifRol + ")");
        lblBaslik.setForeground(Color.WHITE); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        
        JButton btnCikis = new JButton("Oturumu Kapat X ");
        btnCikis.setBackground(new Color(192, 57, 43)); btnCikis.setForeground(Color.WHITE);
        btnCikis.setFocusPainted(false);
        btnCikis.addActionListener(e -> { dispose(); new GirisSecimEkrani().setVisible(true); });
        
        ustBar.add(lblBaslik, BorderLayout.WEST); 
        ustBar.add(btnCikis, BorderLayout.EAST);
        add(ustBar, BorderLayout.NORTH);
    }

    private void solMenuAyarla() {
        JPanel solMenu = new JPanel(); 
        solMenu.setLayout(new BoxLayout(solMenu, BoxLayout.Y_AXIS));
        solMenu.setBackground(new Color(44, 62, 80)); 
        solMenu.setPreferredSize(new Dimension(220, 0));
        solMenu.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        // YETKİYE GÖRE MENÜ FİLTRELEME
        solMenu.add(new JLabel("<html><font color='white'><b>MODÜLLER</b></font></html>"));
        solMenu.add(Box.createVerticalStrut(15));

        if (aktifRol.equalsIgnoreCase("Kasiyer") || aktifRol.equalsIgnoreCase("Garson") || aktifRol.equalsIgnoreCase("Admin")) {
            solMenu.add(menuButonuOlustur("Kasa ve Sipariş", "Kasa ve Sipariş"));
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Vestiyer Modülü", "Vestiyer Modülü"));
            solMenu.add(Box.createVerticalStrut(10));
        }
        
        if (aktifRol.equalsIgnoreCase("Mutfak") || aktifRol.equalsIgnoreCase("Admin")) {
            solMenu.add(menuButonuOlustur("Mutfak Panosu", "Mutfak Panosu"));
            solMenu.add(Box.createVerticalStrut(10));
        }

        add(solMenu, BorderLayout.WEST);
    }

    private JButton menuButonuOlustur(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 45)); 
        btn.setBackground(new Color(52, 73, 94));
        btn.setForeground(Color.WHITE); 
        btn.setFocusPainted(false); 
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.addActionListener(e -> cardLayout.show(icerikPaneli, cardName));
        return btn;
    }

    // ==========================================
    // 1. KASA VE SİPARİŞ (MASALAR) EKRANI
    // ==========================================
    private JPanel kasaVeSiparisSayfasi() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel baslikPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblBaslik = new JLabel("Aktif Masalar ve Sipariş Yönetimi");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        baslikPanel.add(lblBaslik);
        
        // Masa Izgarası (Örneğin 20 Masa)
        JPanel masalarPaneli = new JPanel(new GridLayout(4, 5, 15, 15));
        
        for (int i = 1; i <= 20; i++) {
            JButton masaButon = new JButton("Masa " + i);
            masaButon.setFont(new Font("Arial", Font.BOLD, 18));
            masaButon.setBackground(new Color(236, 240, 241)); // Boş masa rengi (Gri/Beyaz)
            masaButon.setFocusPainted(false);
            
            final int masaNo = i;
            masaButon.addActionListener(e -> {
                // Tıklanan masanın sipariş ekranı (Adisyon) açılacak (Sonraki adım)
                JOptionPane.showMessageDialog(this, "Masa " + masaNo + " adisyon paneli açılıyor...\n(Bu bölüm detaylı ürün listesiyle kodlanacak)");
            });
            masalarPaneli.add(masaButon);
        }

        panel.add(baslikPanel, BorderLayout.NORTH);
        panel.add(masalarPaneli, BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // 2. MUTFAK KANBAN PANOSU
    // ==========================================
    private JPanel mutfakSayfasi() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0)); // Ekranı ikiye böl: Bekleyenler / Hazırlananlar
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Bekleyen Siparişler Paneli
        JPanel pnlBekleyen = new JPanel(new BorderLayout());
        pnlBekleyen.setBorder(BorderFactory.createTitledBorder("🔔 Yeni Gelen Siparişler (Beklemede)"));
        pnlBekleyen.setBackground(new Color(255, 235, 235)); // Açık kırmızı
        
        // Şimdilik temsili bir liste
        DefaultListModel<String> bekleyenModel = new DefaultListModel<>();
        bekleyenModel.addElement("Sipariş #102 - Masa 5 (1x Burger, 2x Kola)");
        bekleyenModel.addElement("Sipariş #103 - Masa 12 (1x Tiramisu - Ekstra Çikolatalı)");
        JList<String> listBekleyen = new JList<>(bekleyenModel);
        listBekleyen.setFont(new Font("Arial", Font.BOLD, 14));
        pnlBekleyen.add(new JScrollPane(listBekleyen), BorderLayout.CENTER);
        
        JButton btnHazirla = new JButton("Seçili Siparişi Hazırlamaya Başla ->");
        btnHazirla.setBackground(new Color(230, 126, 34)); btnHazirla.setForeground(Color.WHITE);
        pnlBekleyen.add(btnHazirla, BorderLayout.SOUTH);

        // Hazırlanan Siparişler Paneli
        JPanel pnlHazirlanan = new JPanel(new BorderLayout());
        pnlHazirlanan.setBorder(BorderFactory.createTitledBorder("🍳 Şu An Hazırlananlar"));
        pnlHazirlanan.setBackground(new Color(255, 248, 220)); // Açık sarı
        
        DefaultListModel<String> hazirlananModel = new DefaultListModel<>();
        JList<String> listHazirlanan = new JList<>(hazirlananModel);
        listHazirlanan.setFont(new Font("Arial", Font.BOLD, 14));
        pnlHazirlanan.add(new JScrollPane(listHazirlanan), BorderLayout.CENTER);
        
        JButton btnTamamla = new JButton("✔ Sipariş Hazır! (Garsona Bildir)");
        btnTamamla.setBackground(new Color(39, 174, 96)); btnTamamla.setForeground(Color.WHITE);
        pnlHazirlanan.add(btnTamamla, BorderLayout.SOUTH);

        // Aksiyonlar (Temsili Taşıma İşlemi)
        btnHazirla.addActionListener(e -> {
            String secili = listBekleyen.getSelectedValue();
            if(secili != null) {
                bekleyenModel.removeElement(secili);
                hazirlananModel.addElement(secili);
            }
        });
        
        btnTamamla.addActionListener(e -> {
            String secili = listHazirlanan.getSelectedValue();
            if(secili != null) {
                hazirlananModel.removeElement(secili);
                JOptionPane.showMessageDialog(this, "Garsona bildirim gönderildi!");
            }
        });

        panel.add(pnlBekleyen);
        panel.add(pnlHazirlanan);
        return panel;
    }

    // ==========================================
    // 3. VESTİYER MODÜLÜ
    // ==========================================
    private JPanel vestiyerSayfasi() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Üst Kayıt Formu
        JPanel pnlKayit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlKayit.setBorder(BorderFactory.createTitledBorder("Yeni Eşya Teslim Al"));
        
        JTextField txtMasaNo = new JTextField(10);
        JTextField txtAskiNo = new JTextField(10);
        JButton btnEkle = new JButton("Vestiyere Ekle");
        btnEkle.setBackground(new Color(41, 128, 185)); btnEkle.setForeground(Color.WHITE);
        
        pnlKayit.add(new JLabel("Masa No:")); pnlKayit.add(txtMasaNo);
        pnlKayit.add(new JLabel("Askı Numarası:")); pnlKayit.add(txtAskiNo);
        pnlKayit.add(btnEkle);

        // Alt Tablo
        vestiyerTableModel = new DefaultTableModel(new String[]{"İşlem ID", "Masa No", "Askı No", "Durum", "Kayıt Zamanı"}, 0);
        vestiyerTablosu = new JTable(vestiyerTableModel);
        vestiyerTablosu.setRowHeight(30);
        
        JPanel pnlTeslim = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnTeslimEt = new JButton("Seçili Eşyayı Müşteriye İade Et");
        btnTeslimEt.setBackground(new Color(39, 174, 96)); btnTeslimEt.setForeground(Color.WHITE);
        pnlTeslim.add(btnTeslimEt);

        // İşlemler
        btnEkle.addActionListener(e -> {
            if(!txtMasaNo.getText().isEmpty() && !txtAskiNo.getText().isEmpty()) {
                String cmd = "VESTIYER_EKLE|" + txtMasaNo.getText() + "|" + txtAskiNo.getText();
                JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl(cmd));
                txtMasaNo.setText(""); txtAskiNo.setText("");
                // Not: Tabloyu güncelleyen metot eklenecek
            }
        });

        panel.add(pnlKayit, BorderLayout.NORTH);
        panel.add(new JScrollPane(vestiyerTablosu), BorderLayout.CENTER);
        panel.add(pnlTeslim, BorderLayout.SOUTH);

        return panel;
    }

    // --- NETWORK ---
    private String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); out.println(komut); return in.readLine();
        } catch (Exception e) { return "HATA|Bağlantı Hatası: " + e.getMessage(); }
    }
}