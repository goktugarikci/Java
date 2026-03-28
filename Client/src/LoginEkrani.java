

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginEkrani extends JFrame {
    private String secilenRol; // "YETKİLİ" veya "PERSONEL"
    private JTextField txtKullaniciAdi;
    private JPasswordField txtSifre;
    private JLabel lblMesaj;

    public LoginEkrani(String rol) {
        this.secilenRol = rol;

        setTitle(rol + " Girişi");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Sadece bu pencereyi kapat
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Üst Başlık
        JLabel baslik = new JLabel(rol + " GİRİŞ PANELİ", SwingConstants.CENTER);
        baslik.setFont(new Font("Arial", Font.BOLD, 18));
        baslik.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(baslik, BorderLayout.NORTH);

        // Orta Form Alanı (Kullanıcı Adı ve Şifre)
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        formPanel.add(new JLabel("Kullanıcı Adı:"));
        txtKullaniciAdi = new JTextField();
        formPanel.add(txtKullaniciAdi);

        formPanel.add(new JLabel("Şifre:"));
        txtSifre = new JPasswordField();
        formPanel.add(txtSifre);

        // Hata veya başarı mesajlarını göstereceğimiz etiket
        lblMesaj = new JLabel("", SwingConstants.CENTER);
        lblMesaj.setForeground(Color.RED);
        formPanel.add(new JLabel("")); // Boşluk tutucu
        formPanel.add(lblMesaj);

        add(formPanel, BorderLayout.CENTER);

        // Alt Buton Alanı
        JPanel butonPanel = new JPanel();
        JButton btnGirisYap = new JButton("Giriş Yap");
        btnGirisYap.setBackground(new Color(70, 130, 180));
        btnGirisYap.setForeground(Color.WHITE);
        btnGirisYap.setFont(new Font("Arial", Font.BOLD, 14));
        btnGirisYap.setFocusPainted(false);
        
        // Giriş Yap butonuna tıklanınca çalışacak olay
        btnGirisYap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sunucuyaGirisIstegiGonder();
            }
        });

        butonPanel.add(btnGirisYap);
        add(butonPanel, BorderLayout.SOUTH);
    }

    // Sunucuya bağlanıp LOGIN komutunu gönderen metot
    private void sunucuyaGirisIstegiGonder() {
        String kullaniciAdi = txtKullaniciAdi.getText().trim();
        String sifre = new String(txtSifre.getPassword()).trim();

        if (kullaniciAdi.isEmpty() || sifre.isEmpty()) {
            lblMesaj.setText("Boş alan bırakmayınız!");
            return;
        }

        lblMesaj.setText("Sunucuya bağlanılıyor...");
        lblMesaj.setForeground(Color.BLUE);

        // Sunucuya Soket ile Bağlan (Yerel bilgisayar testleri için "localhost" veya "127.0.0.1")
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Sunucudan gelen ilk karşılama mesajını oku ve geç
            in.readLine();

            // Komutu sunucuya gönder: LOGIN|admin|admin123
            String komut = "LOGIN|" + kullaniciAdi + "|" + sifre;
            out.println(komut);

            // Sunucunun cevabını bekle
            String sunucuCevabi = in.readLine();

            // Eğer sunucu "BAŞARILI" dediyse
            if (sunucuCevabi != null && sunucuCevabi.startsWith("BAŞARILI")) {
                // Cevap formatı: BAŞARILI|Admin|Sistem Yöneticisi
                String[] parcalar = sunucuCevabi.split("\\|");
                String yetki = parcalar[1];
                String adSoyad = parcalar[2];

                // Yetkiye göre doğru paneli aç
                if (yetki.equalsIgnoreCase("Admin")) {
                    AdminPaneli adminPaneli = new AdminPaneli(adSoyad);
                    adminPaneli.setVisible(true);
                } else {
                    // Personel paneli daha kodlanmadı, şimdilik uyarı veriyoruz
                    JOptionPane.showMessageDialog(this, "Hoşgeldin " + adSoyad + "!\nPersonel paneli yapım aşamasında.", "Giriş Başarılı", JOptionPane.INFORMATION_MESSAGE);
                }
                
                this.dispose(); // Giriş ekranını tamamen kapat
                
            } else {
                // Sunucu HATA döndüyse
                lblMesaj.setText("Hatalı kullanıcı adı veya şifre!");
                lblMesaj.setForeground(Color.RED);
            }

            // İşimiz bitti, soket bağlantısını kapatması için çıkış komutu gönder
            out.println("exit");

        } catch (Exception ex) {
            lblMesaj.setText("Sunucuya bağlanılamadı!");
            lblMesaj.setForeground(Color.RED);
            System.err.println("Bağlantı Hatası: " + ex.getMessage());
        }
    }
}