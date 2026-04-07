

import javax.swing.*;
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

    // ==========================================
    // SİSTEM MODÜLLERİ 
    // ==========================================
    private SiparisModulu siparisEkrani;
    private MutfakModulu mutfakEkrani;
    private KasaModulu kasaEkrani;
    private RezervasyonModulu rezervasyonEkrani;
    private KuryeModulu kuryeEkrani;
    private KuryeTakipModulu kuryeTakipEkrani;

    public PersonelPaneli(String adSoyad, String rol) {
        this.aktifPersonel = adSoyad;
        this.aktifRol = rol; // Rol Seçenekleri: Yetkili(admin), Kasiyer, Garson, Staff, Motokurye

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ustBarAyarla(); 
        solMenuAyarla();

        cardLayout = new CardLayout(); 
        icerikPaneli = new JPanel(cardLayout);

        // ==========================================
        // MODÜLLERİ BAŞLAT VE EKRANA EKLE (YENİ MİMARİ)
        // ==========================================
        // DİKKAT: SiparisModulu artık yeni mimariye uygun olarak 2 parametre ile çağrılıyor
        siparisEkrani = new SiparisModulu(this, aktifPersonel);
        mutfakEkrani = new MutfakModulu(this);
        kasaEkrani = new KasaModulu(this);
        rezervasyonEkrani = new RezervasyonModulu(this);
        kuryeEkrani = new KuryeModulu(this, aktifPersonel);
        kuryeTakipEkrani = new KuryeTakipModulu(this); 

        icerikPaneli.add(siparisEkrani, "Masalar ve Sipariş");
        icerikPaneli.add(kasaEkrani, "Kasa Takip");
        icerikPaneli.add(mutfakEkrani, "Mutfak Panosu");
        icerikPaneli.add(rezervasyonEkrani, "Rezervasyonlar");
        icerikPaneli.add(kuryeEkrani, "Kurye Paneli");
        icerikPaneli.add(kuryeTakipEkrani, "Kurye Takip Paneli"); 

        add(icerikPaneli, BorderLayout.CENTER);

        // ==========================================
        // ROL TABANLI BAŞLANGIÇ EKRANI YÖNLENDİRMESİ
        // ==========================================
        if (aktifRol.equalsIgnoreCase("Staff") || aktifRol.equalsIgnoreCase("Mutfak")) {
            cardLayout.show(icerikPaneli, "Mutfak Panosu"); 
            mutfakEkrani.verileriYenile(); 
        } else if (aktifRol.equalsIgnoreCase("Motokurye")) {
            cardLayout.show(icerikPaneli, "Kurye Paneli"); 
            kuryeEkrani.verileriYenile();
        } else {
            cardLayout.show(icerikPaneli, "Masalar ve Sipariş");
            siparisEkrani.baslat(); 
        }
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout()); 
        ustBar.setBackground(new Color(39, 174, 96)); 
        ustBar.setPreferredSize(new Dimension(0, 50));
        
        JLabel lblBaslik = new JLabel("  RESTORAN POS SİSTEMİ | Personel: " + aktifPersonel + " (" + aktifRol + ")"); 
        lblBaslik.setForeground(Color.WHITE); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        
        JButton btnCikis = new JButton("Oturumu Kapat X "); 
        btnCikis.setBackground(new Color(192, 57, 43)); 
        btnCikis.setForeground(Color.WHITE); 
        btnCikis.setFocusPainted(false);
        
        btnCikis.addActionListener(e -> { 
            dispose(); 
            new GirisSecimEkrani().setVisible(true); 
        });
        
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
        
        solMenu.add(new JLabel("<html><font color='white'><b>MODÜLLER</b></font></html>")); 
        solMenu.add(Box.createVerticalStrut(15));
        
        // ==========================================
        // YETKİLERE GÖRE SOL MENÜ BUTONLARI
        // ==========================================
        if (aktifRol.equalsIgnoreCase("Kasiyer") || aktifRol.equalsIgnoreCase("Yetkili(admin)")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Kasa / Ödemeler", "Kasa Takip")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Rezervasyonlar", "Rezervasyonlar")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("🛵 Kurye Takip Paneli", "Kurye Takip Paneli"));
            solMenu.add(Box.createVerticalStrut(10));
        } else if (aktifRol.equalsIgnoreCase("Garson")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Rezervasyonlar", "Rezervasyonlar")); 
            solMenu.add(Box.createVerticalStrut(10));
        } else if (aktifRol.equalsIgnoreCase("Motokurye")) {
            solMenu.add(menuButonuOlustur("🛵 Teslimatlarım", "Kurye Paneli"));
            solMenu.add(Box.createVerticalStrut(10));
        } 
        
        // Mutfak / Staff Sadece Mutfak Panosunu Görür!
        if (aktifRol.equalsIgnoreCase("Staff") || aktifRol.equalsIgnoreCase("Mutfak") || aktifRol.equalsIgnoreCase("Yetkili(admin)")) {
            solMenu.add(menuButonuOlustur("Mutfak Panosu", "Mutfak Panosu"));
        }
        
        add(solMenu, BorderLayout.WEST);
    }

    private JButton menuButonuOlustur(String text, String cardName) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>"); 
        btn.setMaximumSize(new Dimension(200, 55)); 
        btn.setBackground(new Color(52, 73, 94)); 
        btn.setForeground(Color.WHITE); 
        btn.setFocusPainted(false); 
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        
        btn.addActionListener(e -> {
            cardLayout.show(icerikPaneli, cardName);
            // Sekmeye tıklandığında ilgili modülün verilerini yenile
            if(cardName.equals("Kasa Takip")) kasaEkrani.verileriYenile(); 
            else if (cardName.equals("Mutfak Panosu")) mutfakEkrani.verileriYenile(); 
            else if (cardName.equals("Rezervasyonlar")) rezervasyonEkrani.verileriYenile(); 
            else if (cardName.equals("Kurye Paneli")) kuryeEkrani.verileriYenile(); 
            else if (cardName.equals("Masalar ve Sipariş")) siparisEkrani.baslat(); 
        }); 
        return btn;
    }

    // ==========================================
    // KÖPRÜ METOTLARI (Modüller Arası İletişim)
    // ==========================================
    
    public void adisyonEkraniAc(String siparisTuru, String baslikIsmi) {
        if (siparisEkrani != null) {
            new AdisyonEkrani(this, siparisEkrani, aktifPersonel, siparisTuru, baslikIsmi).setVisible(true);
        }
    }

    public void kasaGuncelle() {
        if(kasaEkrani != null) kasaEkrani.verileriYenile();
    }

    // YENİ MİMARİ: Kasa Modülü ödeme aldığında masayı veritabanından güncel duruma (BOS/Yeşil) çeker
    public void masayiSifirla(String masaAdi) {
        if (siparisEkrani != null) {
            siparisEkrani.baslat(); // Sipariş Modülünü canlı olarak yeniden çizer
        }
    }

    // Tüm modüllerin ortak kullandığı Sunucu İstek Metodu
    public String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080); 
             PrintWriter out = new PrintWriter(s.getOutputStream(), true); 
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); // Sunucudan gelen ilk karşılama mesajını atla
            out.println(komut); 
            return in.readLine();
        } catch (Exception e) { 
            return null; 
        }
    }
}