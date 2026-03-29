

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginEkrani extends JFrame {
    private String secilenRol; 
    private JTextField txtKullaniciAdi;
    private JPasswordField txtSifre;
    private JLabel lblMesaj;

    public LoginEkrani(String rol) {
        this.secilenRol = rol;

        setTitle(rol + " Girişi");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel baslik = new JLabel(rol + " GİRİŞ PANELİ", SwingConstants.CENTER);
        baslik.setFont(new Font("Arial", Font.BOLD, 18));
        baslik.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(baslik, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        formPanel.add(new JLabel("Kullanıcı Adı:"));
        txtKullaniciAdi = new JTextField();
        formPanel.add(txtKullaniciAdi);

        formPanel.add(new JLabel("Şifre:"));
        txtSifre = new JPasswordField();
        formPanel.add(txtSifre);

        lblMesaj = new JLabel("", SwingConstants.CENTER);
        lblMesaj.setForeground(Color.RED);
        formPanel.add(new JLabel("")); 
        formPanel.add(lblMesaj);

        add(formPanel, BorderLayout.CENTER);

        JPanel butonPanel = new JPanel();
        JButton btnGirisYap = new JButton("Giriş Yap");
        btnGirisYap.setBackground(new Color(70, 130, 180));
        btnGirisYap.setForeground(Color.WHITE);
        btnGirisYap.setFont(new Font("Arial", Font.BOLD, 14));
        btnGirisYap.setFocusPainted(false);
        
        btnGirisYap.addActionListener(e -> sunucuyaGirisIstegiGonder());

        butonPanel.add(btnGirisYap);
        add(butonPanel, BorderLayout.SOUTH);
    }

    private void sunucuyaGirisIstegiGonder() {
        String kullaniciAdi = txtKullaniciAdi.getText().trim();
        String sifre = new String(txtSifre.getPassword()).trim();

        if (kullaniciAdi.isEmpty() || sifre.isEmpty()) {
            lblMesaj.setText("Boş alan bırakmayınız!");
            return;
        }

        lblMesaj.setText("Sunucuya bağlanılıyor...");
        lblMesaj.setForeground(Color.BLUE);

        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            in.readLine();

            // Giriş komutunu gönder
            String komut = "GIRIS|" + kullaniciAdi + "|" + sifre;
            out.println(komut);

            String sunucuCevabi = in.readLine();

            if (sunucuCevabi != null && sunucuCevabi.startsWith("BAŞARILI")) {
                String[] parcalar = sunucuCevabi.split("\\|");
                String yetki = parcalar[1];
                String adSoyad = parcalar[2];

                // YETKİ KONTROLÜ
                if (yetki.equalsIgnoreCase("Admin")) {
                    AdminPaneli adminPaneli = new AdminPaneli(adSoyad); 
                    adminPaneli.setVisible(true);
                } else {
                    // Garson, Kasiyer veya Mutfak için Personel Panelini Aç
                    PersonelPaneli personelPaneli = new PersonelPaneli(adSoyad, yetki);
                    personelPaneli.setVisible(true);
                }
                
                this.dispose(); 
                
            } else {
                lblMesaj.setText("Hatalı kullanıcı adı veya şifre!");
                lblMesaj.setForeground(Color.RED);
            }

            out.println("exit");

        } catch (Exception ex) {
            lblMesaj.setText("Sunucuya bağlanılamadı!");
            lblMesaj.setForeground(Color.RED);
        }
    }
}