import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class AdminPaneli extends JFrame {
    private JPanel icerikPaneli;
    private CardLayout cardLayout;
    private String aktifYonetici;
    
    // UI Bileşenleri
    private JPanel interaktifIcerikPaneli;
    private DefaultListModel<String> katListeModel = new DefaultListModel<>();
    private JComboBox<String> comboKategoriler = new JComboBox<>();
    private DefaultTableModel kullaniciTableModel;
    private String secilenGorselAdi = "gorsel_yok.png";

    public AdminPaneli(String adSoyad) {
        this.aktifYonetici = adSoyad;

        // Pencere Ayarları
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- 1. ÜST BAR ---
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(41, 128, 185)); 
        ustBar.setPreferredSize(new Dimension(0, 50));
        
        JLabel lblBaslik = new JLabel("  SİSTEM YÖNETİM MERKEZİ | Yetkili: " + aktifYonetici);
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

        // --- 2. SOL MENÜ ---
        JPanel solMenu = new JPanel(); 
        solMenu.setLayout(new BoxLayout(solMenu, BoxLayout.Y_AXIS));
        solMenu.setBackground(new Color(44, 62, 80)); 
        solMenu.setPreferredSize(new Dimension(250, 0));
        solMenu.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        String[] menuler = {"Kullanıcı Yönetimi", "Ürün Yönetimi", "Geriye Dönük Raporlar", "Kasa / Gün Sonu"};
        for (String m : menuler) {
            JButton btn = menuButonuOlustur(m);
            btn.addActionListener(e -> cardLayout.show(icerikPaneli, m));
            solMenu.add(btn); 
            solMenu.add(Box.createVerticalStrut(10));
        }
        add(solMenu, BorderLayout.WEST);

        // --- 3. SAĞ İÇERİK (CARDLAYOUT) ---
        cardLayout = new CardLayout();
        icerikPaneli = new JPanel(cardLayout);

        icerikPaneli.add(kullaniciYonetimSayfasi(), "Kullanıcı Yönetimi");
        icerikPaneli.add(urunYonetimSayfasi(), "Ürün Yönetimi");
        icerikPaneli.add(sayfaOlustur("Raporlama Ekranı Yapım Aşamasında..."), "Geriye Dönük Raporlar");
        icerikPaneli.add(kasaKapatmaSayfasi(), "Kasa / Gün Sonu");

        add(icerikPaneli, BorderLayout.CENTER);

        // İlk açılışta verileri sunucudan çek
        yukleKategorileri();
        yukleKullanicilari();
    }

    // ==========================================
    // SAYFA OLUŞTURUCU METOTLAR
    // ==========================================

    private JPanel kullaniciYonetimSayfasi() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        
        // Form Alanı
        JPanel form = new JPanel(new GridLayout(3, 4, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Yeni Personel Kaydı"));
        
        JTextField tUser = new JTextField();
        JTextField tPass = new JTextField();
        JTextField tAd = new JTextField();
        JComboBox<String> cRol = new JComboBox<>(new String[]{"Admin", "Staff", "Personel"});
        JButton bEkle = new JButton("Kaydet");

        form.add(new JLabel(" Kullanıcı Adı:")); form.add(tUser);
        form.add(new JLabel(" Şifre:")); form.add(tPass);
        form.add(new JLabel(" Ad Soyad:")); form.add(tAd);
        form.add(new JLabel(" Yetki:")); form.add(cRol);
        form.add(new JLabel("")); form.add(bEkle);

        // Tablo Alanı
        String[] columns = {"ID", "Kullanıcı Adı", "Ad Soyad", "Rol"};
        kullaniciTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(kullaniciTableModel);
        
        bEkle.addActionListener(e -> {
            String cmd = "KULLANICI_EKLE|" + tUser.getText() + "|" + tPass.getText() + "|" + tAd.getText() + "|Soyad|" + cRol.getSelectedItem() + "|email|tel|adres";
            String cevap = sunucuyaKomutGonderVeCevapAl(cmd);
            JOptionPane.showMessageDialog(this, cevap);
            yukleKullanicilari(); // Tabloyu yenile
        });

        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel urunYonetimSayfasi() {
        JPanel anaPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        anaPanel.setBackground(new Color(245, 245, 245));
        anaPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // SOL PANEL: LİSTE VE İÇERİK
        JPanel sol = new JPanel(new BorderLayout(10, 10));
        JList<String> katList = new JList<>(katListeModel);
        katList.setFont(new Font("Arial", Font.BOLD, 14));
        katList.setBorder(BorderFactory.createTitledBorder("Mevcut Kategoriler (Çift Tıkla)"));

        interaktifIcerikPaneli = new JPanel();
        interaktifIcerikPaneli.setLayout(new BoxLayout(interaktifIcerikPaneli, BoxLayout.Y_AXIS));
        interaktifIcerikPaneli.setBackground(new Color(255, 255, 224));

        katList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String secilen = katList.getSelectedValue();
                    if (secilen != null) yukleUrunleriDetayli(secilen);
                }
            }
        });

        sol.add(new JScrollPane(katList), BorderLayout.NORTH);
        sol.add(new JScrollPane(interaktifIcerikPaneli), BorderLayout.CENTER);

        // SAĞ PANEL: FORMLAR
        JPanel sag = new JPanel(); 
        sag.setLayout(new BoxLayout(sag, BoxLayout.Y_AXIS));

        // Kategori Ekleme
        JPanel pnlKat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlKat.setBorder(BorderFactory.createTitledBorder("Hızlı Kategori Tanımla"));
        JTextField tNewKat = new JTextField(15); 
        JButton bKatAdd = new JButton("Kategori Kaydet");
        pnlKat.add(new JLabel("Adı:")); pnlKat.add(tNewKat); pnlKat.add(bKatAdd);

        // Ürün Ekleme (GridBagLayout)
        JPanel pnlUrun = new JPanel(new GridBagLayout());
        pnlUrun.setBorder(BorderFactory.createTitledBorder("Yeni Ürün Tanımlama"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(5,5,5,5);

        JTextField tAd = new JTextField(15); 
        JTextField tFiyat = new JTextField(10);
        JTextArea tAciklama = new JTextArea(2, 15);
        JTextArea tIcerik = new JTextArea(3, 15); // Virgüllü malzemeler
        JButton bGorsel = new JButton("🖼 Görsel Seç...");
        JButton bKaydet = new JButton("Ürünü Kaydet");
        bKaydet.setBackground(new Color(39, 174, 96)); bKaydet.setForeground(Color.WHITE);

        gbc.gridx=0; gbc.gridy=0; pnlUrun.add(new JLabel("Kategori:"), gbc); gbc.gridx=1; pnlUrun.add(comboKategoriler, gbc);
        gbc.gridx=0; gbc.gridy=1; pnlUrun.add(new JLabel("Ürün İsmi:"), gbc); gbc.gridx=1; pnlUrun.add(tAd, gbc);
        gbc.gridx=0; gbc.gridy=2; pnlUrun.add(new JLabel("Fiyat (TL):"), gbc); gbc.gridx=1; pnlUrun.add(tFiyat, gbc);
        gbc.gridx=0; gbc.gridy=3; pnlUrun.add(new JLabel("Açıklama:"), gbc); gbc.gridx=1; pnlUrun.add(new JScrollPane(tAciklama), gbc);
        gbc.gridx=0; gbc.gridy=4; pnlUrun.add(new JLabel("Malzemeler (Virgülle):"), gbc); gbc.gridx=1; pnlUrun.add(new JScrollPane(tIcerik), gbc);
        gbc.gridx=0; gbc.gridy=5; pnlUrun.add(new JLabel("Görsel:"), gbc); gbc.gridx=1; pnlUrun.add(bGorsel, gbc);
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2; pnlUrun.add(bKaydet, gbc);

        // Olaylar
        bKatAdd.addActionListener(e -> { 
            sunucuyaKomutGonderVeCevapAl("KATEGORI_EKLE|" + tNewKat.getText() + "|Açıklama"); 
            yukleKategorileri(); 
            tNewKat.setText(""); 
        });
        
        bGorsel.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                secilenGorselAdi = fc.getSelectedFile().getName();
                bGorsel.setText("Seçildi: " + secilenGorselAdi);
            }
        });

        bKaydet.addActionListener(e -> {
            String cmd = "URUN_EKLE_DETAYLI|" + comboKategoriler.getSelectedItem() + "|" + tAd.getText() + "|" + tFiyat.getText() + "|" + tAciklama.getText() + "|" + tIcerik.getText() + "|" + secilenGorselAdi;
            String cevap = sunucuyaKomutGonderVeCevapAl(cmd);
            JOptionPane.showMessageDialog(this, cevap);
        });

        sag.add(pnlKat); sag.add(Box.createVerticalStrut(15)); sag.add(pnlUrun);
        anaPanel.add(sol); anaPanel.add(sag);
        return anaPanel;
    }

    private JPanel kasaKapatmaSayfasi() {
        JPanel p = new JPanel(new GridBagLayout());
        JButton btn = new JButton("GÜN SONU (Z RAPORU) VE KASA KAPAT");
        btn.setPreferredSize(new Dimension(400, 100));
        btn.setBackground(new Color(192, 57, 43)); btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.addActionListener(e -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("KASA_KAPAT|" + aktifYonetici);
            JOptionPane.showMessageDialog(this, cevap);
        });
        p.add(btn);
        return p;
    }

    // ==========================================
    // NETWORK VE VERİ ÇEKME METOTLARI
    // ==========================================

    private String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); // Karşılama
            out.println(komut);
            return in.readLine();
        } catch (Exception e) {
            return "HATA|Bağlantı Hatası: " + e.getMessage();
        }
    }

    private void yukleKategorileri() {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("KAT_LISTESI_GETIR");
            if (cevap != null && cevap.startsWith("KAT_LISTESI")) {
                String[] parcalar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    katListeModel.clear();
                    comboKategoriler.removeAllItems();
                    for (int i = 1; i < parcalar.length; i++) {
                        katListeModel.addElement(parcalar[i]);
                        comboKategoriler.addItem(parcalar[i]);
                    }
                });
            }
        }).start();
    }

    private void yukleKullanicilari() {
        new Thread(() -> {
            // Sunucu tarafında "KULLANICI_LISTESI_GETIR" komutunu desteklediğinden emin ol
            String cevap = sunucuyaKomutGonderVeCevapAl("KULLANICI_LISTESI_GETIR");
            if (cevap != null && cevap.startsWith("KULLANICI_LISTESI")) {
                String[] satirlar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    if(kullaniciTableModel != null) {
                        kullaniciTableModel.setRowCount(0);
                        for (int i = 1; i < satirlar.length; i++) {
                            kullaniciTableModel.addRow(satirlar[i].split(";"));
                        }
                    }
                });
            }
        }).start();
    }

    private void yukleUrunleriDetayli(String kat) {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR|" + kat);
            if(cevap != null && cevap.startsWith("URUN_LISTESI")) {
                String[] urunler = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    interaktifIcerikPaneli.removeAll();
                    interaktifIcerikPaneli.add(new JLabel("<html><h2>--- " + kat + " ---</h2></html>"));
                    
                    for(int i = 1; i < urunler.length; i++) {
                        // Format: Ad;Fiyat;Açıklama;Stok
                        String[] d = urunler[i].split(";");
                        JPanel pnlUrun = new JPanel(); 
                        pnlUrun.setLayout(new BoxLayout(pnlUrun, BoxLayout.Y_AXIS));
                        pnlUrun.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
                        pnlUrun.setOpaque(false);

                        JLabel lblBaslik = new JLabel("<html><b>📦 " + d[0] + "</b> - " + d[1] + " TL</html>");
                        JLabel lblAciklama = new JLabel("<html><i>Açıklama: " + (d.length > 2 ? d[2] : "") + "</i></html>");
                        pnlUrun.add(lblBaslik); pnlUrun.add(lblAciklama);

                        // Eğer sunucudan içerik/malzeme virgüllü gelirse CheckBox ekle
                        if(d.length > 4 && !d[4].isEmpty() && !d[4].equals("null")) {
                            JPanel pnlMalzemeler = new JPanel(new FlowLayout(FlowLayout.LEFT));
                            pnlMalzemeler.setOpaque(false);
                            String[] malzemeler = d[4].split(",");
                            for(String m : malzemeler) {
                                pnlMalzemeler.add(new JCheckBox(m.trim(), true));
                            }
                            pnlUrun.add(pnlMalzemeler);
                        }
                        
                        interaktifIcerikPaneli.add(pnlUrun);
                    }
                    interaktifIcerikPaneli.revalidate(); 
                    interaktifIcerikPaneli.repaint();
                });
            }
        }).start();
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================
    
    private JButton menuButonuOlustur(String text) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(240, 50));
        b.setBackground(new Color(52, 73, 94));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        return b;
    }

    private JPanel sayfaOlustur(String t) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(t, SwingConstants.CENTER));
        return p;
    }
}