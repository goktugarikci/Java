
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
    private String aktifRol; 
    
    private JComboBox<String> comboKategorilerListesi = new JComboBox<>();
    private DefaultTableModel urunTableModel;
    private JTable urunTablosu;
    
    private JComboBox<String> formComboKategori = new JComboBox<>();
    private JTextField txtUrunAd = new JTextField(15);
    private JTextField txtFiyat = new JTextField(15);
    private JTextArea txtAciklama = new JTextArea(3, 15);
    private String secilenGorselAdi = "gorsel_yok.png";
    private JButton btnGorsel = new JButton("🖼 Görsel Seç...");
    
    private DefaultTableModel malzemeTableModel;
    private JTable malzemeTablosu;
    private String secilenEskiUrunAdi = "";
    private String secilenKatGorselAdi = "gorsel_yok.png";

    // Kullanıcı Yönetimi
    private DefaultTableModel kullaniciTableModel;
    private JTable kullaniciTablosu;
    private JTextField txtKulAdi = new JTextField(15);
    private JTextField txtKulSifre = new JTextField(15);
    private JTextField txtKulAd = new JTextField(15);
    private JTextField txtKulSoyad = new JTextField(15);
    private JComboBox<String> comboKulRol = new JComboBox<>(new String[]{"Admin", "Staff", "Kasiyer", "Garson", "Mutfak"});
    private JTextField txtKulEmail = new JTextField(15);
    private JTextField txtKulTel = new JTextField(15);
    private JTextArea txtKulAdres = new JTextArea(3, 15);
    private String secilenEskiKulAdi = "";

    // Masa Yönetimi
    private DefaultTableModel masaTableModel;
    private JTable masaTablosu;

    public AdminPaneli(String adSoyad, String rol) {
        this.aktifYonetici = adSoyad;
        this.aktifRol = rol;

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ustBarAyarla();
        solMenuAyarla();

        cardLayout = new CardLayout();
        icerikPaneli = new JPanel(cardLayout);

        icerikPaneli.add(kullaniciYonetimSayfasi(), "Kullanıcı Yönetimi");
        icerikPaneli.add(urunYonetimSayfasi(), "Ürün Yönetimi");
        icerikPaneli.add(masaYonetimSayfasi(), "Masa Yönetimi"); // YENİ EKLENDİ
        icerikPaneli.add(sayfaOlustur("Raporlama Ekranı"), "Geriye Dönük Raporlar");
        icerikPaneli.add(sayfaOlustur("Kasa Kapatma Ekranı"), "Kasa / Gün Sonu");

        add(icerikPaneli, BorderLayout.CENTER);

        if (aktifRol.equalsIgnoreCase("Admin")) {
            cardLayout.show(icerikPaneli, "Masa Yönetimi"); // Test için ilk bura açılsın
        } else {
            cardLayout.show(icerikPaneli, "Geriye Dönük Raporlar");
        }

        yukleKategorileri();
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(41, 128, 185)); ustBar.setPreferredSize(new Dimension(0, 50));
        JLabel lblBaslik = new JLabel("  SİSTEM YÖNETİM MERKEZİ | Yetkili: " + aktifYonetici + " (" + aktifRol + ")");
        lblBaslik.setForeground(Color.WHITE); lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        JButton btnCikis = new JButton("Oturumu Kapat X ");
        btnCikis.setBackground(new Color(192, 57, 43)); btnCikis.setForeground(Color.WHITE);
        btnCikis.setFocusPainted(false);
        btnCikis.addActionListener(e -> { dispose(); new GirisSecimEkrani().setVisible(true); });
        ustBar.add(lblBaslik, BorderLayout.WEST); ustBar.add(btnCikis, BorderLayout.EAST);
        add(ustBar, BorderLayout.NORTH);
    }

    private void solMenuAyarla() {
        JPanel solMenu = new JPanel(); solMenu.setLayout(new BoxLayout(solMenu, BoxLayout.Y_AXIS));
        solMenu.setBackground(new Color(44, 62, 80)); solMenu.setPreferredSize(new Dimension(220, 0));
        solMenu.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        String[] menuler;
        if (aktifRol.equalsIgnoreCase("Admin")) {
            menuler = new String[]{"Masa Yönetimi", "Kullanıcı Yönetimi", "Ürün Yönetimi", "Geriye Dönük Raporlar", "Kasa / Gün Sonu"};
        } else {
            menuler = new String[]{"Geriye Dönük Raporlar", "Kasa / Gün Sonu"};
        }

        for (String m : menuler) {
            JButton btn = new JButton(m);
            btn.setMaximumSize(new Dimension(200, 45)); btn.setBackground(new Color(52, 73, 94));
            btn.setForeground(Color.WHITE); btn.setFocusPainted(false); btn.setFont(new Font("Arial", Font.BOLD, 13));
            btn.addActionListener(e -> cardLayout.show(icerikPaneli, m));
            solMenu.add(btn); solMenu.add(Box.createVerticalStrut(10));
        }
        add(solMenu, BorderLayout.WEST);
    }

    // ==========================================
    // MASA YÖNETİM SAYFASI (YENİ)
    // ==========================================
    private JPanel masaYonetimSayfasi() {
        JPanel panel = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // SOL PANEL - Mevcut Masalar
        JPanel solPanel = new JPanel(new BorderLayout(5, 5));
        solPanel.setBorder(BorderFactory.createTitledBorder("Mevcut Masalar"));
        
        masaTableModel = new DefaultTableModel(new String[]{"Masa Adı", "Mevcut Durum"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        masaTablosu = new JTable(masaTableModel);
        masaTablosu.setRowHeight(30);
        masaTablosu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JButton btnSil = new JButton("Seçili Masayı Sil");
        btnSil.setBackground(new Color(192, 57, 43)); btnSil.setForeground(Color.WHITE);
        
        btnSil.addActionListener(e -> {
            int row = masaTablosu.getSelectedRow();
            if(row != -1) {
                String secilenMasa = masaTableModel.getValueAt(row, 0).toString();
                if(JOptionPane.showConfirmDialog(this, secilenMasa + " silinecek, emin misiniz?", "Sil", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("MASA_SIL|" + secilenMasa));
                    yukleMasalari();
                }
            } else { JOptionPane.showMessageDialog(this, "Önce listeden bir masa seçin."); }
        });

        solPanel.add(new JScrollPane(masaTablosu), BorderLayout.CENTER);
        solPanel.add(btnSil, BorderLayout.SOUTH);

        // SAĞ PANEL - Yeni Ekle & Düzenle
        JPanel sagPanel = new JPanel();
        sagPanel.setLayout(new BoxLayout(sagPanel, BoxLayout.Y_AXIS));
        sagPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tekli Ekleme / Güncelleme
        JPanel pnlTekli = new JPanel(new GridBagLayout());
        pnlTekli.setBorder(BorderFactory.createTitledBorder("Masa Ekle / Adını Değiştir"));
        GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(10,10,10,10);
        
        JTextField txtMasaAdi = new JTextField(15);
        JButton btnEkle = new JButton("Yeni Masa Olarak Ekle");
        JButton btnGuncelle = new JButton("Seçilinin Adını Güncelle");
        btnEkle.setBackground(new Color(39, 174, 96)); btnEkle.setForeground(Color.WHITE);
        btnGuncelle.setBackground(new Color(41, 128, 185)); btnGuncelle.setForeground(Color.WHITE);

        g.gridx=0; g.gridy=0; pnlTekli.add(new JLabel("Masa İsmi (Örn: Teras 1):"), g); g.gridx=1; pnlTekli.add(txtMasaAdi, g);
        g.gridx=0; g.gridy=1; pnlTekli.add(btnEkle, g); g.gridx=1; pnlTekli.add(btnGuncelle, g);

        // Toplu Ekleme
        JPanel pnlToplu = new JPanel(new GridBagLayout());
        pnlToplu.setBorder(BorderFactory.createTitledBorder("Toplu Masa Üretimi"));
        JTextField txtOnEk = new JTextField("Masa ", 10);
        JTextField txtSayi = new JTextField("20", 5);
        JButton btnTopluUret = new JButton("Otomatik Üret");
        btnTopluUret.setBackground(new Color(230, 126, 34)); btnTopluUret.setForeground(Color.WHITE);

        g.gridx=0; g.gridy=0; pnlToplu.add(new JLabel("Ön Ek:"), g); g.gridx=1; pnlToplu.add(txtOnEk, g);
        g.gridx=0; g.gridy=1; pnlToplu.add(new JLabel("Kaç Adet:"), g); g.gridx=1; pnlToplu.add(txtSayi, g);
        g.gridx=0; g.gridy=2; g.gridwidth=2; pnlToplu.add(btnTopluUret, g);

        masaTablosu.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting() && masaTablosu.getSelectedRow() != -1) {
                txtMasaAdi.setText(masaTableModel.getValueAt(masaTablosu.getSelectedRow(), 0).toString());
            }
        });

        btnEkle.addActionListener(e -> {
            if(!txtMasaAdi.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("MASA_EKLE|" + txtMasaAdi.getText().trim()));
                yukleMasalari(); txtMasaAdi.setText("");
            }
        });

        btnGuncelle.addActionListener(e -> {
            int row = masaTablosu.getSelectedRow();
            if(row != -1 && !txtMasaAdi.getText().trim().isEmpty()) {
                String eskiAd = masaTableModel.getValueAt(row, 0).toString();
                JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("MASA_GUNCELLE|" + eskiAd + "|" + txtMasaAdi.getText().trim()));
                yukleMasalari(); txtMasaAdi.setText(""); masaTablosu.clearSelection();
            } else { JOptionPane.showMessageDialog(this, "Önce tablodan bir masa seçin."); }
        });

        btnTopluUret.addActionListener(e -> {
            try {
                int sayi = Integer.parseInt(txtSayi.getText().trim());
                String prefix = txtOnEk.getText(); // Örn: "Masa "
                for(int i=1; i<=sayi; i++) {
                    sunucuyaKomutGonderVeCevapAl("MASA_EKLE|" + prefix + i);
                }
                JOptionPane.showMessageDialog(this, sayi + " adet masa başarıyla üretildi!");
                yukleMasalari();
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Geçerli bir sayı girin."); }
        });

        sagPanel.add(pnlTekli);
        sagPanel.add(Box.createVerticalStrut(20));
        sagPanel.add(pnlToplu);

        splitPane.setLeftComponent(solPanel);
        splitPane.setRightComponent(sagPanel);
        panel.add(splitPane, BorderLayout.CENTER);
        
        yukleMasalari(); // Sayfa açılınca masaları çek
        return panel;
    }

    private void yukleMasalari() {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            if (cevap != null && cevap.startsWith("MASA_LISTESI")) {
                String[] satirlar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    masaTableModel.setRowCount(0);
                    for (int i = 1; i < satirlar.length; i++) {
                        String[] d = satirlar[i].split(";");
                        masaTableModel.addRow(new Object[]{d[0], d[1]});
                    }
                });
            }
        }).start();
    }

    // ==========================================
    // KULLANICI VE ÜRÜN YÖNETİM SAYFALARI (Gizlenmiş Standart Kodlar)
    // ==========================================
    private JPanel kullaniciYonetimSayfasi() {
        JPanel panel = new JPanel(new BorderLayout()); JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); splitPane.setDividerLocation(500);
        JPanel solPanel = new JPanel(new BorderLayout(5, 5)); solPanel.setBorder(BorderFactory.createTitledBorder("Mevcut Kullanıcılar/Personeller"));
        String[] columns = {"ID", "Kullanıcı Adı", "Ad", "Soyad", "Rol", "Telefon", "E-Mail", "Adres"};
        kullaniciTableModel = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        kullaniciTablosu = new JTable(kullaniciTableModel); kullaniciTablosu.setRowHeight(30);
        kullaniciTablosu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && kullaniciTablosu.getSelectedRow() != -1) {
                int row = kullaniciTablosu.getSelectedRow(); secilenEskiKulAdi = kullaniciTableModel.getValueAt(row, 1).toString();
                txtKulAdi.setText(kullaniciTableModel.getValueAt(row, 1) != null ? kullaniciTableModel.getValueAt(row, 1).toString() : ""); txtKulSifre.setText("********"); txtKulAd.setText(kullaniciTableModel.getValueAt(row, 2) != null ? kullaniciTableModel.getValueAt(row, 2).toString() : ""); txtKulSoyad.setText(kullaniciTableModel.getValueAt(row, 3) != null ? kullaniciTableModel.getValueAt(row, 3).toString() : ""); comboKulRol.setSelectedItem(kullaniciTableModel.getValueAt(row, 4) != null ? kullaniciTableModel.getValueAt(row, 4).toString() : "Personel"); txtKulTel.setText(kullaniciTableModel.getValueAt(row, 5) != null ? kullaniciTableModel.getValueAt(row, 5).toString() : ""); txtKulEmail.setText(kullaniciTableModel.getValueAt(row, 6) != null ? kullaniciTableModel.getValueAt(row, 6).toString() : ""); txtKulAdres.setText(kullaniciTableModel.getValueAt(row, 7) != null ? kullaniciTableModel.getValueAt(row, 7).toString() : "");
            }
        });
        solPanel.add(new JScrollPane(kullaniciTablosu), BorderLayout.CENTER);
        JPanel sagPanel = new JPanel(new GridBagLayout()); sagPanel.setBorder(BorderFactory.createTitledBorder("Personel Kayıt ve Düzenleme Formu")); GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(10, 10, 10, 10);
        g.gridx=0; g.gridy=0; sagPanel.add(new JLabel("Kullanıcı Adı:"), g); g.gridx=1; sagPanel.add(txtKulAdi, g); g.gridx=0; g.gridy=1; sagPanel.add(new JLabel("Şifre:"), g); g.gridx=1; sagPanel.add(txtKulSifre, g); g.gridx=0; g.gridy=2; sagPanel.add(new JLabel("Adı:"), g); g.gridx=1; sagPanel.add(txtKulAd, g); g.gridx=0; g.gridy=3; sagPanel.add(new JLabel("Soyadı:"), g); g.gridx=1; sagPanel.add(txtKulSoyad, g); g.gridx=0; g.gridy=4; sagPanel.add(new JLabel("Sistem Rolü:"), g); g.gridx=1; sagPanel.add(comboKulRol, g); g.gridx=0; g.gridy=5; sagPanel.add(new JLabel("Telefon:"), g); g.gridx=1; sagPanel.add(txtKulTel, g); g.gridx=0; g.gridy=6; sagPanel.add(new JLabel("E-Mail:"), g); g.gridx=1; sagPanel.add(txtKulEmail, g); g.gridx=0; g.gridy=7; sagPanel.add(new JLabel("Adres:"), g); g.gridx=1; sagPanel.add(new JScrollPane(txtKulAdres), g);
        JPanel pnlAksiyon = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); JButton btnTemizle = new JButton("Formu Temizle"); JButton btnSil = new JButton("Kullanıcıyı Sil"); JButton btnGuncelle = new JButton("Bilgileri Güncelle"); JButton btnYeniKaydet = new JButton("Yeni Kullanıcı Kaydet");
        btnYeniKaydet.setBackground(new Color(39, 174, 96)); btnYeniKaydet.setForeground(Color.WHITE); btnGuncelle.setBackground(new Color(41, 128, 185)); btnGuncelle.setForeground(Color.WHITE); btnSil.setBackground(new Color(192, 57, 43)); btnSil.setForeground(Color.WHITE);
        btnTemizle.addActionListener(e -> kulFormuTemizle());
        btnYeniKaydet.addActionListener(e -> { if(txtKulAdi.getText().isEmpty() || txtKulSifre.getText().isEmpty()) { JOptionPane.showMessageDialog(this, "Kullanıcı adı ve şifre zorunludur!"); return; } String cmd = "KULLANICI_EKLE|" + txtKulAdi.getText() + "|" + txtKulSifre.getText() + "|" + txtKulAd.getText() + "|" + txtKulSoyad.getText() + "|" + comboKulRol.getSelectedItem() + "|" + txtKulEmail.getText() + "|" + txtKulTel.getText() + "|" + txtKulAdres.getText().replaceAll("\n", " "); JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl(cmd)); yukleKullanicilariDetayli(); kulFormuTemizle(); });
        btnGuncelle.addActionListener(e -> { if(secilenEskiKulAdi.isEmpty()) { JOptionPane.showMessageDialog(this, "Önce listeden bir kullanıcı seçin!"); return; } String cmd = "KULLANICI_GUNCELLE|" + secilenEskiKulAdi + "|" + txtKulAdi.getText() + "|" + txtKulSifre.getText() + "|" + txtKulAd.getText() + "|" + txtKulSoyad.getText() + "|" + comboKulRol.getSelectedItem() + "|" + txtKulEmail.getText() + "|" + txtKulTel.getText() + "|" + txtKulAdres.getText().replaceAll("\n", " "); JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl(cmd)); yukleKullanicilariDetayli(); kulFormuTemizle(); });
        btnSil.addActionListener(e -> { if(!secilenEskiKulAdi.isEmpty()) { if(JOptionPane.showConfirmDialog(this, secilenEskiKulAdi + " silinecek, emin misiniz?", "Sil", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("KULLANICI_SIL|" + secilenEskiKulAdi)); yukleKullanicilariDetayli(); kulFormuTemizle(); } } });
        pnlAksiyon.add(btnTemizle); pnlAksiyon.add(btnSil); pnlAksiyon.add(btnGuncelle); pnlAksiyon.add(btnYeniKaydet); g.gridx=0; g.gridy=8; g.gridwidth=2; sagPanel.add(pnlAksiyon, g); splitPane.setLeftComponent(solPanel); splitPane.setRightComponent(new JScrollPane(sagPanel)); panel.add(splitPane, BorderLayout.CENTER); yukleKullanicilariDetayli(); return panel;
    }
    private void kulFormuTemizle() { txtKulAdi.setText(""); txtKulSifre.setText(""); txtKulAd.setText(""); txtKulSoyad.setText(""); txtKulEmail.setText(""); txtKulTel.setText(""); txtKulAdres.setText(""); kullaniciTablosu.clearSelection(); secilenEskiKulAdi = ""; }
    private void yukleKullanicilariDetayli() { new Thread(() -> { String cevap = sunucuyaKomutGonderVeCevapAl("KULLANICI_LISTESI_GETIR"); if (cevap != null && cevap.startsWith("KULLANICI_LISTESI")) { String[] satirlar = cevap.split("\\|"); SwingUtilities.invokeLater(() -> { if(kullaniciTableModel != null) { kullaniciTableModel.setRowCount(0); for (int i = 1; i < satirlar.length; i++) { String[] detay = satirlar[i].split(";", -1); Object[] rowData = new Object[8]; for(int j=0; j<8; j++) rowData[j] = j < detay.length ? detay[j] : ""; kullaniciTableModel.addRow(rowData); } } }); } }).start(); }

    private JPanel urunYonetimSayfasi() {
        JPanel panel = new JPanel(new BorderLayout()); JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); splitPane.setDividerLocation(500); splitPane.setResizeWeight(0.4);
        JPanel solPanel = new JPanel(new BorderLayout(5, 5)); solPanel.setBorder(BorderFactory.createTitledBorder("Mevcut Kayıtlar"));
        JPanel pnlFiltre = new JPanel(new FlowLayout(FlowLayout.LEFT)); pnlFiltre.add(new JLabel("Kategori Filtresi: ")); pnlFiltre.add(comboKategorilerListesi); comboKategorilerListesi.addActionListener(e -> { if(comboKategorilerListesi.getSelectedItem() != null) { yukleUrunleriTabloya(comboKategorilerListesi.getSelectedItem().toString()); formuTemizle(); } });
        String[] columns = {"Ürün Adı", "Fiyat (TL)", "Açıklama", "Malzemeler_Gizli"}; urunTableModel = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } }; urunTablosu = new JTable(urunTableModel); urunTablosu.setRowHeight(30); urunTablosu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urunTablosu.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting() && urunTablosu.getSelectedRow() != -1) { int row = urunTablosu.getSelectedRow(); secilenEskiUrunAdi = urunTableModel.getValueAt(row, 0).toString(); txtUrunAd.setText(secilenEskiUrunAdi); txtFiyat.setText(urunTableModel.getValueAt(row, 1).toString()); txtAciklama.setText(urunTableModel.getValueAt(row, 2).toString()); malzemeTableModel.setRowCount(0); String malzemelerStr = urunTableModel.getValueAt(row, 3).toString(); if(!malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) { String[] malzemeler = malzemelerStr.split(","); for(String m : malzemeler) { String[] mDetay = m.split(":"); if(mDetay.length == 3) { String tur = mDetay[1].equals("1") ? "Standart (Çıkarılabilir)" : "Ekstra (Ücretli)"; malzemeTableModel.addRow(new Object[]{mDetay[0], tur, mDetay[2]}); } } } } });
        solPanel.add(pnlFiltre, BorderLayout.NORTH); solPanel.add(new JScrollPane(urunTablosu), BorderLayout.CENTER);
        JPanel sagPanel = new JPanel(); sagPanel.setLayout(new BoxLayout(sagPanel, BoxLayout.Y_AXIS)); sagPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JPanel pnlKatYonet = new JPanel(new FlowLayout(FlowLayout.LEFT)); pnlKatYonet.setBorder(BorderFactory.createTitledBorder("1. Kategori İşlemleri")); JTextField txtHizliKatEkle = new JTextField(10); JButton btnKatGorsel = new JButton("🖼 Görsel"); JButton btnKatEkle = new JButton("Yeni Ekle"); JButton btnKatDuzenleSil = new JButton("⚙ Seçiliyi Düzenle/Sil"); btnKatDuzenleSil.setBackground(new Color(230, 126, 34)); btnKatDuzenleSil.setForeground(Color.WHITE); pnlKatYonet.add(new JLabel("Kategori:")); pnlKatYonet.add(txtHizliKatEkle); pnlKatYonet.add(btnKatGorsel); pnlKatYonet.add(btnKatEkle); pnlKatYonet.add(btnKatDuzenleSil);
        btnKatGorsel.addActionListener(e -> { JFileChooser fc = new JFileChooser(); if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { secilenKatGorselAdi = fc.getSelectedFile().getName(); btnKatGorsel.setText("Seçildi!"); } });
        btnKatEkle.addActionListener(e -> { if(!txtHizliKatEkle.getText().trim().isEmpty()){ String cvp = sunucuyaKomutGonderVeCevapAl("KATEGORI_EKLE|" + txtHizliKatEkle.getText() + "|Açıklama|" + secilenKatGorselAdi); JOptionPane.showMessageDialog(this, cvp); yukleKategorileri(); txtHizliKatEkle.setText(""); btnKatGorsel.setText("🖼 Görsel"); secilenKatGorselAdi = "gorsel_yok.png"; } });
        btnKatDuzenleSil.addActionListener(e -> { String secilenKat = (String) formComboKategori.getSelectedItem(); if(secilenKat != null) kategoriYonetimPenceresiAc(secilenKat); else JOptionPane.showMessageDialog(this, "Aşağıdaki formdan bir kategori seçin."); });
        JPanel pnlAnaBilgi = new JPanel(new GridBagLayout()); pnlAnaBilgi.setBorder(BorderFactory.createTitledBorder("2. Ürün Tanımlama / Düzenleme")); GridBagConstraints g = new GridBagConstraints(); g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(10,10,10,10); g.gridx=0; g.gridy=0; pnlAnaBilgi.add(new JLabel("Kategori:"), g); g.gridx=1; pnlAnaBilgi.add(formComboKategori, g); g.gridx=0; g.gridy=1; pnlAnaBilgi.add(new JLabel("Ürün İsmi:"), g); g.gridx=1; pnlAnaBilgi.add(txtUrunAd, g); g.gridx=0; g.gridy=2; pnlAnaBilgi.add(new JLabel("Fiyat (TL):"), g); g.gridx=1; pnlAnaBilgi.add(txtFiyat, g); g.gridx=0; g.gridy=3; pnlAnaBilgi.add(new JLabel("Açıklama:"), g); g.gridx=1; pnlAnaBilgi.add(new JScrollPane(txtAciklama), g); g.gridx=0; g.gridy=4; pnlAnaBilgi.add(new JLabel("Görsel:"), g); g.gridx=1; pnlAnaBilgi.add(btnGorsel, g); JButton btnIcerikYonet = new JButton("➕ İçindekiler Özellikleri Ekle / Yönet"); btnIcerikYonet.setBackground(new Color(52, 152, 219)); btnIcerikYonet.setForeground(Color.WHITE); btnIcerikYonet.addActionListener(e -> icerikOzellikPenceresiAc()); g.gridx=0; g.gridy=5; pnlAnaBilgi.add(new JLabel("Özellikler:"), g); g.gridx=1; pnlAnaBilgi.add(btnIcerikYonet, g);
        btnGorsel.addActionListener(e -> { JFileChooser fc = new JFileChooser(); if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { secilenGorselAdi = fc.getSelectedFile().getName(); btnGorsel.setText("Seçildi: " + secilenGorselAdi); } });
        JPanel pnlMalzeme = new JPanel(new BorderLayout(5, 5)); pnlMalzeme.setBorder(BorderFactory.createTitledBorder("İçindekiler / Ekstra Malzemeler Listesi")); malzemeTableModel = new DefaultTableModel(new String[]{"Malzeme Adı", "Tür", "Ekstra Fiyat (TL)"}, 0); malzemeTablosu = new JTable(malzemeTableModel); malzemeTablosu.setRowHeight(25); JPanel pnlMalzemeSil = new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton btnMalzemeSil = new JButton("➖ Seçili Olanı Çıkar"); btnMalzemeSil.addActionListener(e -> { int row = malzemeTablosu.getSelectedRow(); if(row != -1) malzemeTableModel.removeRow(row); }); pnlMalzemeSil.add(btnMalzemeSil); pnlMalzeme.add(new JScrollPane(malzemeTablosu), BorderLayout.CENTER); pnlMalzeme.add(pnlMalzemeSil, BorderLayout.SOUTH);
        JPanel pnlAksiyon = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10)); JButton btnTemizle = new JButton("Yeni Kayıt İçin Temizle"); JButton btnSil = new JButton("Seçili Ürünü Sil"); JButton btnGuncelle = new JButton("Seçili Ürünü Güncelle"); JButton btnYeniKaydet = new JButton("Yeni Olarak Kaydet"); btnYeniKaydet.setBackground(new Color(39, 174, 96)); btnYeniKaydet.setForeground(Color.WHITE); btnGuncelle.setBackground(new Color(41, 128, 185)); btnGuncelle.setForeground(Color.WHITE); btnSil.setBackground(new Color(192, 57, 43)); btnSil.setForeground(Color.WHITE);
        btnTemizle.addActionListener(e -> formuTemizle());
        btnYeniKaydet.addActionListener(e -> { String mStr = malzemeleriBirlestir(); String cmd = "URUN_EKLE_DETAYLI|" + formComboKategori.getSelectedItem() + "|" + txtUrunAd.getText() + "|" + txtFiyat.getText() + "|" + txtAciklama.getText() + "|" + secilenGorselAdi + "|" + mStr; JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl(cmd)); yukleUrunleriTabloya(formComboKategori.getSelectedItem().toString()); formuTemizle(); });
        btnGuncelle.addActionListener(e -> { if(secilenEskiUrunAdi.isEmpty()) return; String mStr = malzemeleriBirlestir(); String cmd = "URUN_GUNCELLE_DETAYLI|" + secilenEskiUrunAdi + "|" + formComboKategori.getSelectedItem() + "|" + txtUrunAd.getText() + "|" + txtFiyat.getText() + "|" + txtAciklama.getText() + "|" + secilenGorselAdi + "|" + mStr; JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl(cmd)); yukleUrunleriTabloya(formComboKategori.getSelectedItem().toString()); formuTemizle(); });
        btnSil.addActionListener(e -> { if(!secilenEskiUrunAdi.isEmpty()) { if(JOptionPane.showConfirmDialog(this, "Bu ürünü silmek istediğinize emin misiniz?", "Sil", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("URUN_SIL|" + secilenEskiUrunAdi)); yukleUrunleriTabloya(formComboKategori.getSelectedItem().toString()); formuTemizle(); } } });
        pnlAksiyon.add(btnTemizle); pnlAksiyon.add(btnSil); pnlAksiyon.add(btnGuncelle); pnlAksiyon.add(btnYeniKaydet);
        sagPanel.add(pnlKatYonet); sagPanel.add(pnlAnaBilgi); sagPanel.add(Box.createVerticalStrut(10)); sagPanel.add(pnlMalzeme); sagPanel.add(Box.createVerticalStrut(10)); sagPanel.add(pnlAksiyon);
        splitPane.setLeftComponent(solPanel); splitPane.setRightComponent(new JScrollPane(sagPanel)); panel.add(splitPane, BorderLayout.CENTER); return panel;
    }

    private void kategoriYonetimPenceresiAc(String eskiKatAdi) {
        JDialog dialog = new JDialog(this, "Kategori Düzenle / Sil", true); dialog.setSize(450, 220); dialog.setLayout(new GridBagLayout()); dialog.setLocationRelativeTo(this); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;
        JTextField txtYeniAd = new JTextField(eskiKatAdi, 15); JButton btnKatGorselGuncelle = new JButton("🖼 Yeni Görsel Seç..."); JButton btnGuncelle = new JButton("Güncelle"); JButton btnSil = new JButton("Tamamen Sil"); btnGuncelle.setBackground(new Color(41, 128, 185)); btnGuncelle.setForeground(Color.WHITE); btnSil.setBackground(new Color(192, 57, 43)); btnSil.setForeground(Color.WHITE); final String[] yeniGorsel = {"gorsel_yok.png"};
        btnKatGorselGuncelle.addActionListener(e -> { JFileChooser fc = new JFileChooser(); if(fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) { yeniGorsel[0] = fc.getSelectedFile().getName(); btnKatGorselGuncelle.setText("Seçildi: " + yeniGorsel[0]); } });
        g.gridx=0; g.gridy=0; dialog.add(new JLabel("Mevcut Kategori: " + eskiKatAdi), g); g.gridy=1; dialog.add(new JLabel("Yeni Adı:"), g); g.gridx=1; dialog.add(txtYeniAd, g); g.gridx=0; g.gridy=2; dialog.add(new JLabel("Yeni Görsel:"), g); g.gridx=1; dialog.add(btnKatGorselGuncelle, g);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); btnPanel.add(btnSil); btnPanel.add(btnGuncelle); g.gridx=0; g.gridy=3; g.gridwidth=2; dialog.add(btnPanel, g);
        btnGuncelle.addActionListener(e -> { String yeniAd = txtYeniAd.getText().trim(); if(!yeniAd.isEmpty()) { String cvp = sunucuyaKomutGonderVeCevapAl("KATEGORI_GUNCELLE|" + eskiKatAdi + "|" + yeniAd + "|" + yeniGorsel[0]); JOptionPane.showMessageDialog(dialog, cvp); if(cvp.startsWith("BAŞARILI")) { yukleKategorileri(); dialog.dispose(); } } });
        btnSil.addActionListener(e -> { if(JOptionPane.showConfirmDialog(dialog, eskiKatAdi + " kategorisini silmek istediğinize emin misiniz?", "Sil", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { String cvp = sunucuyaKomutGonderVeCevapAl("KATEGORI_SIL|" + eskiKatAdi); JOptionPane.showMessageDialog(dialog, cvp); if(cvp.startsWith("BAŞARILI")) { yukleKategorileri(); dialog.dispose(); } } });
        dialog.setVisible(true);
    }
    private void icerikOzellikPenceresiAc() {
        JDialog dialog = new JDialog(this, "İçerik ve Ekstra Özellik Ekle", true); dialog.setSize(400, 250); dialog.setLayout(new GridBagLayout()); dialog.setLocationRelativeTo(this); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(10, 10, 10, 10); g.fill = GridBagConstraints.HORIZONTAL;
        JTextField tAd = new JTextField(15); JTextField tFiyat = new JTextField("0", 10); JCheckBox cStandart = new JCheckBox("Standart İçerik (Ücretsiz, Çıkarılabilir)", true); JButton bEkle = new JButton("Listeye Ekle"); bEkle.setBackground(new Color(39, 174, 96)); bEkle.setForeground(Color.WHITE);
        g.gridx=0; g.gridy=0; dialog.add(new JLabel("Özellik Adı:"), g); g.gridx=1; dialog.add(tAd, g); g.gridx=0; g.gridy=1; dialog.add(new JLabel("Ekstra Ücret:"), g); g.gridx=1; dialog.add(tFiyat, g); g.gridx=0; g.gridy=2; g.gridwidth=2; dialog.add(cStandart, g); g.gridy=3; dialog.add(bEkle, g);
        bEkle.addActionListener(e -> { String ad = tAd.getText().trim(); String fiyat = tFiyat.getText().trim(); if(!ad.isEmpty()) { String tur = cStandart.isSelected() ? "Standart (Çıkarılabilir)" : "Ekstra (Ücretli)"; malzemeTableModel.addRow(new Object[]{ad, tur, fiyat}); tAd.setText(""); tFiyat.setText("0"); cStandart.setSelected(true); JOptionPane.showMessageDialog(dialog, ad + " eklendi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE); } }); dialog.setVisible(true);
    }
    private String malzemeleriBirlestir() { if(malzemeTableModel.getRowCount() == 0) return "null"; StringBuilder sb = new StringBuilder(); for(int i = 0; i < malzemeTableModel.getRowCount(); i++) { String ad = malzemeTableModel.getValueAt(i, 0).toString(); String var = malzemeTableModel.getValueAt(i, 1).toString().contains("Standart") ? "1" : "0"; String f = malzemeTableModel.getValueAt(i, 2).toString(); sb.append(ad).append(":").append(var).append(":").append(f).append(","); } return sb.toString().substring(0, sb.length() - 1); }
    private void formuTemizle() { txtUrunAd.setText(""); txtFiyat.setText(""); txtAciklama.setText(""); btnGorsel.setText("🖼 Görsel Seç..."); secilenGorselAdi = "gorsel_yok.png"; malzemeTableModel.setRowCount(0); urunTablosu.clearSelection(); secilenEskiUrunAdi = ""; }
    private String sunucuyaKomutGonderVeCevapAl(String komut) { try (Socket s = new Socket("localhost", 8080); PrintWriter out = new PrintWriter(s.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) { in.readLine(); out.println(komut); return in.readLine(); } catch (Exception e) { return "HATA|Bağlantı Hatası: " + e.getMessage(); } }
    private void yukleKategorileri() { new Thread(() -> { String cevap = sunucuyaKomutGonderVeCevapAl("KAT_LISTESI_GETIR"); if (cevap != null && cevap.startsWith("KAT_LISTESI")) { String[] parcalar = cevap.split("\\|"); SwingUtilities.invokeLater(() -> { comboKategorilerListesi.removeAllItems(); formComboKategori.removeAllItems(); for (int i = 1; i < parcalar.length; i++) { String[] detay = parcalar[i].split(";"); comboKategorilerListesi.addItem(detay[0]); formComboKategori.addItem(detay[0]); } }); } }).start(); }
    private void yukleUrunleriTabloya(String kat) { new Thread(() -> { String cevap = sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + kat); if(cevap != null && cevap.startsWith("URUN_LISTESI_DETAYLI")) { String[] urunler = cevap.split("\\|"); SwingUtilities.invokeLater(() -> { urunTableModel.setRowCount(0); for(int i = 1; i < urunler.length; i++) { String[] d = urunler[i].split(";"); String malzemeler = (d.length > 5) ? d[5] : ""; urunTableModel.addRow(new Object[]{d[0], d[1], d[2], malzemeler}); } urunTablosu.getColumnModel().getColumn(3).setMinWidth(0); urunTablosu.getColumnModel().getColumn(3).setMaxWidth(0); urunTablosu.getColumnModel().getColumn(3).setWidth(0); }); } }).start(); }
    private JPanel sayfaOlustur(String t) { JPanel p = new JPanel(new BorderLayout()); p.add(new JLabel(t, SwingConstants.CENTER)); return p; }
}