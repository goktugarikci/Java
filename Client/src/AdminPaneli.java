

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminPaneli extends JFrame {
    private String aktifPersonel;
    private JPanel icerikPaneli;
    private CardLayout cardLayout;

    // Rapor Değişkenleri
    private JLabel lblCiro, lblSiparisSayisi, lblAktifMasa, lblRezervasyon;
    private DefaultTableModel zRaporTableModel;
    private JTable zRaporTablo;
    
    // Masa Yönetimi Değişkenleri
    private DefaultTableModel masaTableModel;
    private JTable masaTablo;

    // Alt Modüller
    private PersonelYonetimi personelYonetimEkrani;
    private UrunYonetimi urunYonetimEkrani;

    public AdminPaneli(String adSoyad) {
        this.aktifPersonel = adSoyad;

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ustBarAyarla();
        solMenuAyarla();

        cardLayout = new CardLayout();
        icerikPaneli = new JPanel(cardLayout);

        // Modülleri Başlat
        personelYonetimEkrani = new PersonelYonetimi(this);
        urunYonetimEkrani = new UrunYonetimi(this);

        // Sayfaları CardLayout'a Ekle
        icerikPaneli.add(dashboardSayfasiOlustur(), "Dashboard");
        icerikPaneli.add(zRaporlariSayfasiOlustur(), "ZRaporlari");
        icerikPaneli.add(masaYonetimSayfasiOlustur(), "MasaYonetimi"); // YENİ EKLENEN
        icerikPaneli.add(personelYonetimEkrani, "PersonelYonetimi");
        icerikPaneli.add(urunYonetimEkrani, "UrunYonetimi");

        add(icerikPaneli, BorderLayout.CENTER);
        verileriGuncelle();
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(41, 128, 185)); 
        ustBar.setPreferredSize(new Dimension(0, 50));
        
        JLabel lblBaslik = new JLabel("  YÖNETİCİ (ADMİN) PANELİ | Hoş Geldiniz, " + aktifPersonel);
        lblBaslik.setForeground(Color.WHITE); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        
        JPanel pnlSagButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        pnlSagButonlar.setOpaque(false);
        
        JButton btnRestoranGecis = new JButton("Geri Dön: Restoran Ekranı ➔");
        btnRestoranGecis.setBackground(new Color(39, 174, 96)); 
        btnRestoranGecis.setForeground(Color.WHITE);
        btnRestoranGecis.setFocusPainted(false);
        btnRestoranGecis.addActionListener(e -> { 
            dispose(); 
            new PersonelPaneli(aktifPersonel, "Yetkili(admin)").setVisible(true); 
        });

        JButton btnCikis = new JButton("Oturumu Kapat X ");
        btnCikis.setBackground(new Color(192, 57, 43)); 
        btnCikis.setForeground(Color.WHITE);
        btnCikis.setFocusPainted(false);
        btnCikis.addActionListener(e -> { 
            dispose(); 
            new GirisSecimEkrani().setVisible(true); 
        });
        
        pnlSagButonlar.add(btnRestoranGecis);
        pnlSagButonlar.add(btnCikis);

        ustBar.add(lblBaslik, BorderLayout.WEST); 
        ustBar.add(pnlSagButonlar, BorderLayout.EAST);
        add(ustBar, BorderLayout.NORTH);
    }

    private void solMenuAyarla() {
        JPanel solMenu = new JPanel(); 
        solMenu.setLayout(new BoxLayout(solMenu, BoxLayout.Y_AXIS));
        solMenu.setBackground(new Color(44, 62, 80)); 
        solMenu.setPreferredSize(new Dimension(250, 0));
        solMenu.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        solMenu.add(new JLabel("<html><font color='white' size='4'><b>YÖNETİM MENÜSÜ</b></font></html>"));
        solMenu.add(Box.createVerticalStrut(20));

        solMenu.add(menuButonuOlustur("📊 Günlük Özet (Dashboard)", "Dashboard"));
        solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("📁 Geçmiş Z Raporları", "ZRaporlari"));
        solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("🪑 Masa Yönetimi", "MasaYonetimi")); // YENİ EKLENEN BUTON
        solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("👥 Personel Yönetimi", "PersonelYonetimi"));
        solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("🍔 Ürün & Menü Yönetimi", "UrunYonetimi"));
        
        add(solMenu, BorderLayout.WEST);
    }

    private JButton menuButonuOlustur(String text, String cardName) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setMaximumSize(new Dimension(230, 55)); 
        btn.setBackground(new Color(52, 73, 94)); 
        btn.setForeground(Color.WHITE); 
        btn.setFocusPainted(false); 
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        
        btn.addActionListener(e -> {
            cardLayout.show(icerikPaneli, cardName);
            if(cardName.equals("Dashboard")) verileriGuncelle();
            if(cardName.equals("ZRaporlari")) zRaporlariniYenile();
            if(cardName.equals("MasaYonetimi")) masalariYenile(); // TIKLANDIĞINDA MASALARI YÜKLER
            if(cardName.equals("PersonelYonetimi")) personelYonetimEkrani.verileriYenile();
            if(cardName.equals("UrunYonetimi")) urunYonetimEkrani.verileriYenile();
        });
        return btn;
    }

    // ==========================================
    // YENİ: MASA YÖNETİM SAYFASI
    // ==========================================
    private JPanel masaYonetimSayfasiOlustur() {
        JPanel pnlMain = new JPanel(new BorderLayout(15, 15));
        pnlMain.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Üst Kısım: Ekleme/Silme Formu
        JPanel pnlUst = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlUst.setBorder(BorderFactory.createTitledBorder("Yeni Masa Ekle / Sil"));

        JTextField txtMasaAdi = new JTextField(15);
        txtMasaAdi.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnEkle = new JButton("➕ Masa Ekle");
        btnEkle.setBackground(new Color(39, 174, 96));
        btnEkle.setForeground(Color.WHITE);
        btnEkle.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnSil = new JButton("❌ Seçili Masayı Sil");
        btnSil.setBackground(new Color(192, 57, 43));
        btnSil.setForeground(Color.WHITE);
        btnSil.setFont(new Font("Arial", Font.BOLD, 14));

        pnlUst.add(new JLabel("Masa Adı (Örn: Bahçe-1):"));
        pnlUst.add(txtMasaAdi);
        pnlUst.add(btnEkle);
        pnlUst.add(btnSil);

        // Orta Kısım: Masa Tablosu
        masaTableModel = new DefaultTableModel(new String[]{"Masa Adı", "Anlık Durumu"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        masaTablo = new JTable(masaTableModel);
        masaTablo.setRowHeight(35);
        masaTablo.setFont(new Font("Arial", Font.PLAIN, 15));
        
        // Tablo Sütun Boyut Ayarları
        masaTablo.getColumnModel().getColumn(0).setPreferredWidth(300);
        masaTablo.getColumnModel().getColumn(1).setPreferredWidth(150);

        pnlMain.add(pnlUst, BorderLayout.NORTH);
        pnlMain.add(new JScrollPane(masaTablo), BorderLayout.CENTER);

        // Aksiyonlar
        btnEkle.addActionListener(e -> {
            String mAdi = txtMasaAdi.getText().trim();
            if (mAdi.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lütfen bir masa adı girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String cvp = sunucuyaKomutGonderVeCevapAl("MASA_EKLE|" + mAdi);
            JOptionPane.showMessageDialog(this, cvp);
            txtMasaAdi.setText("");
            masalariYenile(); // Tabloyu anında güncelle
        });

        btnSil.addActionListener(e -> {
            int row = masaTablo.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Lütfen silinecek masayı aşağıdaki listeden seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String mAdi = masaTableModel.getValueAt(row, 0).toString();
            String durum = masaTableModel.getValueAt(row, 1).toString();

            if (!durum.equals("BOS")) {
                JOptionPane.showMessageDialog(this, "Sadece 'BOS' durumundaki masalar silinebilir. İçinde oturan müşteri varsa lütfen önce hesabı kapatın.", "İşlem Reddedildi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int onay = JOptionPane.showConfirmDialog(this, "'" + mAdi + "' isimli masa sistemden tamamen silinecek. Emin misiniz?", "Silme Onayı", JOptionPane.YES_NO_OPTION);
            if (onay == JOptionPane.YES_OPTION) {
                String cvp = sunucuyaKomutGonderVeCevapAl("MASA_SIL|" + mAdi);
                JOptionPane.showMessageDialog(this, cvp);
                masalariYenile(); // Tabloyu anında güncelle
            }
        });

        return pnlMain;
    }

    private void masalariYenile() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            SwingUtilities.invokeLater(() -> {
                masaTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("MASA_LISTESI|") && cvp.length() > 13) {
                    String[] masalar = cvp.substring(13).split("\\|");
                    for (String m : masalar) {
                        if (m.trim().isEmpty()) continue;
                        String[] d = m.split(";");
                        if (d.length >= 2) {
                            masaTableModel.addRow(new Object[]{d[0], d[1]});
                        }
                    }
                }
            });
        }).start();
    }
    // ==========================================

    private JPanel dashboardSayfasiOlustur() {
        JPanel pnlMain = new JPanel(new BorderLayout(20, 20));
        pnlMain.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel pnlBaslik = new JPanel(new BorderLayout());
        JLabel lblDashboardBaslik = new JLabel("Bugünün İşletme Özeti");
        lblDashboardBaslik.setFont(new Font("Arial", Font.BOLD, 28));
        
        JPanel pnlButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnYenile = new JButton("🔄 Yenile");
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.setBackground(new Color(52, 152, 219));
        btnYenile.setForeground(Color.WHITE);
        btnYenile.addActionListener(e -> verileriGuncelle());

        JButton btnGunSonu = new JButton("🔒 GÜN SONU AL (Z Raporu)");
        btnGunSonu.setFont(new Font("Arial", Font.BOLD, 14));
        btnGunSonu.setBackground(new Color(192, 57, 43));
        btnGunSonu.setForeground(Color.WHITE);
        btnGunSonu.addActionListener(e -> {
            int onay = JOptionPane.showConfirmDialog(this, "Bu işlem bugünün tüm ödenmiş siparişlerini arşivleyecektir. Emin misiniz?", "Z Raporu Onayı", JOptionPane.YES_NO_OPTION);
            if(onay == JOptionPane.YES_OPTION) {
                String tarih = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
                JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("GUN_SONU_AL|" + tarih));
                verileriGuncelle();
            }
        });

        pnlButonlar.add(btnYenile);
        pnlButonlar.add(btnGunSonu);
        pnlBaslik.add(lblDashboardBaslik, BorderLayout.WEST);
        pnlBaslik.add(pnlButonlar, BorderLayout.EAST);
        pnlMain.add(pnlBaslik, BorderLayout.NORTH);

        JPanel pnlKartlar = new JPanel(new GridLayout(2, 2, 25, 25));
        lblCiro = new JLabel("Hesaplanıyor...", SwingConstants.CENTER);
        pnlKartlar.add(bilgiKartiOlustur("💰 Bugünkü Toplam Ciro", lblCiro, new Color(39, 174, 96)));

        lblSiparisSayisi = new JLabel("Hesaplanıyor...", SwingConstants.CENTER);
        pnlKartlar.add(bilgiKartiOlustur("🛍️ Tamamlanan Sipariş Sayısı", lblSiparisSayisi, new Color(41, 128, 185)));

        lblAktifMasa = new JLabel("Hesaplanıyor...", SwingConstants.CENTER);
        pnlKartlar.add(bilgiKartiOlustur("🍽️ Şu Anki Aktif Masalar", lblAktifMasa, new Color(230, 126, 34)));

        lblRezervasyon = new JLabel("Hesaplanıyor...", SwingConstants.CENTER);
        pnlKartlar.add(bilgiKartiOlustur("📅 Bugünkü Rezervasyonlar", lblRezervasyon, new Color(142, 68, 173)));

        pnlMain.add(pnlKartlar, BorderLayout.CENTER);
        return pnlMain;
    }

    private JPanel bilgiKartiOlustur(String baslik, JLabel degerLabel, Color renk) {
        JPanel kart = new JPanel(new BorderLayout());
        kart.setBackground(Color.WHITE);
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(renk, 3, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblB = new JLabel(baslik, SwingConstants.CENTER);
        lblB.setFont(new Font("Arial", Font.BOLD, 20));
        lblB.setForeground(Color.DARK_GRAY);

        degerLabel.setFont(new Font("Arial", Font.BOLD, 45));
        degerLabel.setForeground(renk);

        kart.add(lblB, BorderLayout.NORTH);
        kart.add(degerLabel, BorderLayout.CENTER);
        return kart;
    }

    private JPanel zRaporlariSayfasiOlustur() {
        JPanel pnlMain = new JPanel(new BorderLayout(15, 15));
        pnlMain.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        zRaporTableModel = new DefaultTableModel(new String[]{"Rapor Tarihi", "Toplam Ciro", "Fiş Sayısı", "Masa", "Paket", "Eve Servis", "GIZLI_MASA", "GIZLI_STOK"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        zRaporTablo = new JTable(zRaporTableModel);
        zRaporTablo.setRowHeight(35);
        
        zRaporTablo.getColumnModel().getColumn(6).setMinWidth(0); zRaporTablo.getColumnModel().getColumn(6).setMaxWidth(0);
        zRaporTablo.getColumnModel().getColumn(7).setMinWidth(0); zRaporTablo.getColumnModel().getColumn(7).setMaxWidth(0);

        JScrollPane scroll = new JScrollPane(zRaporTablo);
        scroll.setBorder(BorderFactory.createTitledBorder("Z Raporları Arşivi"));
        pnlMain.add(scroll, BorderLayout.CENTER);

        JButton btnDetay = new JButton("🔍 Seçili Raporun Detaylı Analizini Gör");
        btnDetay.setFont(new Font("Arial", Font.BOLD, 16));
        btnDetay.setBackground(new Color(44, 62, 80));
        btnDetay.setForeground(Color.WHITE);
        btnDetay.setPreferredSize(new Dimension(0, 50));
        btnDetay.addActionListener(e -> {
            int row = zRaporTablo.getSelectedRow();
            if (row != -1) {
                String tarih = zRaporTableModel.getValueAt(row, 0).toString();
                String ciro = zRaporTableModel.getValueAt(row, 1).toString();
                String masaDetay = zRaporTableModel.getValueAt(row, 6).toString();
                String stokDetay = zRaporTableModel.getValueAt(row, 7).toString();
                
                StringBuilder msg = new StringBuilder("<html><div style='width:400px; padding:10px;'><h2>" + tarih + " Analizi</h2>");
                msg.append("<b>Toplam Ciro: </b><font color='green'>" + ciro + "</font><hr>");
                msg.append("<h3>🍽️ Masa Kullanımı</h3>" + masaDetay.replace(",", "<br>"));
                msg.append("<hr><h3>📦 Stok & Satış</h3>" + stokDetay.replace("|", "<br>"));
                msg.append("</div></html>");
                JOptionPane.showMessageDialog(this, msg.toString(), "Rapor Detayı", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        pnlMain.add(btnDetay, BorderLayout.SOUTH);
        return pnlMain;
    }

    private double ciroAyikla(String html) {
        int pStart = html.indexOf("<!--PRICE");
        if (pStart != -1) {
            int pEnd = html.indexOf("-->", pStart);
            if (pEnd != -1) {
                try {
                    String val = html.substring(pStart + 9, pEnd).trim().replace(",", ".");
                    return Double.parseDouble(val);
                } catch (Exception e) {}
            }
        }
        
        try {
            Pattern pattern = Pattern.compile("([0-9]+[.,]?[0-9]*)\\s*TL");
            Matcher matcher = pattern.matcher(html);
            String sonBulunanTutar = "0.0";
            while (matcher.find()) {
                sonBulunanTutar = matcher.group(1); 
            }
            return Double.parseDouble(sonBulunanTutar.replace(",", "."));
        } catch (Exception e) {}

        return 0.0;
    }

    private void verileriGuncelle() {
        new Thread(() -> {
            String bugun = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            double gunlukCiro = 0.0;
            int gunlukSiparis = 0;
            
            String ciroCvp = sunucuyaKomutGonderVeCevapAl("KASA_GECMIS_GETIR");
            if (ciroCvp != null && ciroCvp.startsWith("KASA_GECMIS_VERI|")) {
                String[] siparisler = ciroCvp.substring(17).split("\\|\\|\\|");
                for (String s : siparisler) {
                    if (s.trim().isEmpty()) continue;
                    String[] d = s.split("~_~"); 
                    if (d.length >= 5 && d[3].equals("ODENDI")) {
                        gunlukSiparis++;
                        gunlukCiro += ciroAyikla(d[4]); 
                    }
                }
            }
            
            final double fCiro = gunlukCiro;
            final int fSiparis = gunlukSiparis;
            SwingUtilities.invokeLater(() -> { 
                lblCiro.setText(fCiro + " TL"); 
                lblSiparisSayisi.setText(fSiparis + " Adet"); 
            });

            String aktifMasaCvp = sunucuyaKomutGonderVeCevapAl("AKTIF_MASALARI_GETIR");
            if (aktifMasaCvp != null && aktifMasaCvp.startsWith("AKTIF_MASALAR|")) {
                int sayi = aktifMasaCvp.length() > 14 ? aktifMasaCvp.substring(14).split("\\|\\|\\|").length : 0;
                SwingUtilities.invokeLater(() -> lblAktifMasa.setText(sayi + " Masa"));
            }

            String rezCvp = sunucuyaKomutGonderVeCevapAl("BUGUN_REZ_GETIR|" + bugun);
            if (rezCvp != null && rezCvp.startsWith("BUGUN_REZ|")) {
                int sayi = rezCvp.length() > 10 ? rezCvp.substring(10).split("\\|\\|\\|").length : 0;
                SwingUtilities.invokeLater(() -> lblRezervasyon.setText(sayi + " Kayıt"));
            }
        }).start();
    }

    private void zRaporlariniYenile() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("ESKI_RAPORLARI_GETIR");
            SwingUtilities.invokeLater(() -> {
                zRaporTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("GUNSONU_RAPORLARI|")) {
                    String[] raporlar = cvp.substring(18).split("\\|\\|\\|");
                    for (String r : raporlar) {
                        if (r.trim().isEmpty()) continue;
                        String[] d = r.split("~_~");
                        if (d.length >= 8) zRaporTableModel.addRow(new Object[]{ d[0], d[1] + " TL", d[2], d[5], d[3], d[4], d[6], d[7] });
                    }
                }
            });
        }).start();
    }

    public String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080); 
             PrintWriter out = new PrintWriter(s.getOutputStream(), true); 
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); out.println(komut); return in.readLine();
        } catch (Exception e) { return null; }
    }
}