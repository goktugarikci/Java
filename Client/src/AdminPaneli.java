
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminPaneli extends JFrame {
    private String aktifPersonel;
    private JPanel icerikPaneli;
    private CardLayout cardLayout;

    // Rapor Değişkenleri
    private JLabel lblCiro, lblSiparisSayisi, lblAktifMasa, lblRezervasyon;
    private DefaultTableModel zRaporTableModel;
    private JTable zRaporTablo;
    
    // Alt Modüller
    private JPanel personelYonetimEkrani;
    private UrunYonetimi urunYonetimEkrani; // YENİ

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
        personelYonetimEkrani = personelYonetimiOlustur();
        urunYonetimEkrani = new UrunYonetimi(this); // YENİ

        // Sayfaları (Kartları) İçerik Paneline Ekle
        icerikPaneli.add(dashboardSayfasiOlustur(), "Dashboard");
        icerikPaneli.add(zRaporlariSayfasiOlustur(), "ZRaporlari");
        icerikPaneli.add(personelYonetimEkrani, "PersonelYonetimi");
        icerikPaneli.add(urunYonetimEkrani, "UrunYonetimi"); // YENİ EKLENDİ

        add(icerikPaneli, BorderLayout.CENTER);
        verileriGuncelle();
    }

    private JPanel personelYonetimiOlustur() {
        // PersonelYonetimi sınıfı henüz tanımlanmadığı veya import edilemediği için 
        // geçici bir placeholder panel döndürüyoruz.
        // Eğer PersonelYonetimi.java dosyanız varsa, isminin ve paketinin doğruluğunu kontrol edin.
        return bosSayfaOlustur("Personel Yönetimi Modülü Yüklenemedi");
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(41, 128, 185)); 
        ustBar.setPreferredSize(new Dimension(0, 50));
        
        JLabel lblBaslik = new JLabel("  YÖNETİCİ (ADMİN) PANELİ | Hoş Geldiniz, " + aktifPersonel);
        lblBaslik.setForeground(Color.WHITE); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 16));
        
        JPanel pnlSagButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5)); pnlSagButonlar.setOpaque(false);
        JButton btnRestoranGecis = new JButton("Geri Dön: Restoran Ekranı ➔"); btnRestoranGecis.setBackground(new Color(39, 174, 96)); btnRestoranGecis.setForeground(Color.WHITE); btnRestoranGecis.setFocusPainted(false);
        btnRestoranGecis.addActionListener(e -> { dispose(); new PersonelPaneli(aktifPersonel, "Admin").setVisible(true); });

        JButton btnCikis = new JButton("Oturumu Kapat X "); btnCikis.setBackground(new Color(192, 57, 43)); btnCikis.setForeground(Color.WHITE); btnCikis.setFocusPainted(false);
        btnCikis.addActionListener(e -> { dispose(); new GirisSecimEkrani().setVisible(true); });
        
        pnlSagButonlar.add(btnRestoranGecis); pnlSagButonlar.add(btnCikis);
        ustBar.add(lblBaslik, BorderLayout.WEST); ustBar.add(pnlSagButonlar, BorderLayout.EAST);
        add(ustBar, BorderLayout.NORTH);
    }

    private void solMenuAyarla() {
        JPanel solMenu = new JPanel(); solMenu.setLayout(new BoxLayout(solMenu, BoxLayout.Y_AXIS));
        solMenu.setBackground(new Color(44, 62, 80)); solMenu.setPreferredSize(new Dimension(250, 0));
        solMenu.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        solMenu.add(new JLabel("<html><font color='white' size='4'><b>YÖNETİM MENÜSÜ</b></font></html>"));
        solMenu.add(Box.createVerticalStrut(20));

        solMenu.add(menuButonuOlustur("📊 Günlük Özet (Dashboard)", "Dashboard")); solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("📁 Geçmiş Z Raporları", "ZRaporlari")); solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("👥 Personel Yönetimi", "PersonelYonetimi")); solMenu.add(Box.createVerticalStrut(10));
        solMenu.add(menuButonuOlustur("🍔 Ürün & Menü Yönetimi", "UrunYonetimi")); 
        
        add(solMenu, BorderLayout.WEST);
    }

    private JButton menuButonuOlustur(String text, String cardName) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setMaximumSize(new Dimension(230, 55)); btn.setBackground(new Color(52, 73, 94)); btn.setForeground(Color.WHITE); btn.setFocusPainted(false); btn.setFont(new Font("Arial", Font.BOLD, 14));
        
        btn.addActionListener(e -> {
            cardLayout.show(icerikPaneli, cardName);
            if(cardName.equals("Dashboard")) verileriGuncelle();
            if(cardName.equals("ZRaporlari")) zRaporlariniYenile();
            if(cardName.equals("UrunYonetimi")) urunYonetimEkrani.verileriYenile(); // YENİ EKLENDİ
        });
        return btn;
    }

    private JPanel dashboardSayfasiOlustur() {
        JPanel pnlMain = new JPanel(new BorderLayout(20, 20)); pnlMain.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JPanel pnlBaslik = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("Bugünün İşletme Özeti"); lblBaslik.setFont(new Font("Arial", Font.BOLD, 28));
        
        JPanel pnlSagBaslik = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnYenile = new JButton("🔄 Yenile"); btnYenile.setFont(new Font("Arial", Font.BOLD, 14)); btnYenile.setBackground(new Color(52, 152, 219)); btnYenile.setForeground(Color.WHITE);
        btnYenile.addActionListener(e -> verileriGuncelle());
        
        JButton btnGunSonu = new JButton("🔒 GÜN SONU AL (Z Raporu)");
        btnGunSonu.setFont(new Font("Arial", Font.BOLD, 14)); btnGunSonu.setBackground(new Color(192, 57, 43)); btnGunSonu.setForeground(Color.WHITE);
        btnGunSonu.addActionListener(e -> {
            if(JOptionPane.showConfirmDialog(this, "GÜN SONU DİKKAT!\n\nBu işlem bugünün tüm ödenmiş ve iptal edilmiş siparişlerini Z Raporu olarak arşivleyecektir.\nGeçmiş Kasa ekranındaki veriler sıfırlanacaktır.\n\nOnaylıyor musunuz?", "Z Raporu Onayı", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                String tarih = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
                JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("GUN_SONU_AL|" + tarih));
                verileriGuncelle();
            }
        });

        pnlSagBaslik.add(btnYenile); pnlSagBaslik.add(btnGunSonu);
        pnlBaslik.add(lblBaslik, BorderLayout.WEST); pnlBaslik.add(pnlSagBaslik, BorderLayout.EAST);
        pnlMain.add(pnlBaslik, BorderLayout.NORTH);

        JPanel pnlKartlar = new JPanel(new GridLayout(2, 2, 25, 25));
        lblCiro = new JLabel("Hesaplanıyor...", SwingConstants.CENTER); pnlKartlar.add(bilgiKartiOlustur("💰 Bugünkü Toplam Ciro", lblCiro, new Color(39, 174, 96)));
        lblSiparisSayisi = new JLabel("Hesaplanıyor...", SwingConstants.CENTER); pnlKartlar.add(bilgiKartiOlustur("🛍️ Tamamlanan Sipariş", lblSiparisSayisi, new Color(41, 128, 185)));
        lblAktifMasa = new JLabel("Hesaplanıyor...", SwingConstants.CENTER); pnlKartlar.add(bilgiKartiOlustur("🍽️ Aktif Açık Masalar", lblAktifMasa, new Color(230, 126, 34)));
        lblRezervasyon = new JLabel("Hesaplanıyor...", SwingConstants.CENTER); pnlKartlar.add(bilgiKartiOlustur("📅 Bugünkü Rezervasyonlar", lblRezervasyon, new Color(142, 68, 173)));
        pnlMain.add(pnlKartlar, BorderLayout.CENTER);
        return pnlMain;
    }

    private JPanel zRaporlariSayfasiOlustur() {
        JPanel pnlMain = new JPanel(new BorderLayout(15, 15)); pnlMain.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        zRaporTableModel = new DefaultTableModel(new String[]{"Rapor Tarihi", "Toplam Ciro", "Toplam Fiş", "Masalar", "Paket Servis", "Eve Servis", "GIZLI_MASA", "GIZLI_STOK"}, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
        zRaporTablo = new JTable(zRaporTableModel); zRaporTablo.setRowHeight(35); zRaporTablo.setFont(new Font("Arial", Font.PLAIN, 15));
        
        zRaporTablo.getColumnModel().getColumn(6).setMinWidth(0); zRaporTablo.getColumnModel().getColumn(6).setMaxWidth(0); zRaporTablo.getColumnModel().getColumn(6).setWidth(0);
        zRaporTablo.getColumnModel().getColumn(7).setMinWidth(0); zRaporTablo.getColumnModel().getColumn(7).setMaxWidth(0); zRaporTablo.getColumnModel().getColumn(7).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(zRaporTablo); scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Geçmiş Kasa (Z) Raporları Arşivi", 0, 0, new Font("Arial", Font.BOLD, 15))); pnlMain.add(scrollPane, BorderLayout.CENTER);

        JButton btnDetay = new JButton("🔍 Seçili Raporun DETAYLI (Stok & Masa) Analizini Gör"); btnDetay.setBackground(new Color(44, 62, 80)); btnDetay.setForeground(Color.WHITE); btnDetay.setFont(new Font("Arial", Font.BOLD, 16)); btnDetay.setPreferredSize(new Dimension(0, 50));
        btnDetay.addActionListener(e -> {
            int row = zRaporTablo.getSelectedRow();
            if (row != -1) {
                String tarih = zRaporTableModel.getValueAt(row, 0).toString(); String ciro = zRaporTableModel.getValueAt(row, 1).toString(); String masaDetay = zRaporTableModel.getValueAt(row, 6).toString(); String stokDetay = zRaporTableModel.getValueAt(row, 7).toString(); 
                StringBuilder mesaj = new StringBuilder("<html><div style='font-family:Arial; font-size:14px; width:450px;'><h2>" + tarih + " - Z Raporu Analizi</h2><b>Kapanış Cirosu: </b><font color='green' size='5'>" + ciro + "</font><br><hr><h3>🍽️ Masa Kullanım Analizi</h3>");
                for(String m : masaDetay.split(",")) { if(!m.trim().isEmpty()) { String[] md = m.split(":"); if(md.length == 2) mesaj.append("► <b>").append(md[0]).append("</b> : ").append(md[1]).append(" defa sipariş aldı.<br>"); } }
                mesaj.append("<hr><h3>📦 Günlük Satış ve Stok Analizi</h3>");
                if (stokDetay.equals("Satış Yok") || stokDetay.isEmpty() || stokDetay.equals("null")) mesaj.append("<i>Stok düşülecek ürün satışı bulunamadı.</i>");
                else for(String s : stokDetay.split("\\|")) { if(!s.trim().isEmpty()) mesaj.append("► ").append(s).append("<br>"); }
                mesaj.append("</div></html>");
                JOptionPane.showMessageDialog(this, mesaj.toString(), "Z Raporu Detayı", JOptionPane.INFORMATION_MESSAGE);
            } else JOptionPane.showMessageDialog(this, "Lütfen detayını görmek istediğiniz raporu tablodan seçin!");
        });
        pnlMain.add(btnDetay, BorderLayout.SOUTH); return pnlMain;
    }

    private void zRaporlariniYenile() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("ESKI_RAPORLARI_GETIR");
            SwingUtilities.invokeLater(() -> {
                zRaporTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("GUNSONU_RAPORLARI|") && cvp.length() > 18) {
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

    private JPanel bilgiKartiOlustur(String baslik, JLabel degerLabel, Color renk) {
        JPanel kart = new JPanel(new BorderLayout()); kart.setBackground(Color.WHITE); kart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(renk, 3, true), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        JLabel lblBaslik = new JLabel(baslik, SwingConstants.CENTER); lblBaslik.setFont(new Font("Arial", Font.BOLD, 20)); lblBaslik.setForeground(Color.DARK_GRAY);
        degerLabel.setFont(new Font("Arial", Font.BOLD, 45)); degerLabel.setForeground(renk);
        kart.add(lblBaslik, BorderLayout.NORTH); kart.add(degerLabel, BorderLayout.CENTER); return kart;
    }

    private void verileriGuncelle() {
        new Thread(() -> {
            String bugun = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            double gunlukCiro = 0.0;
            int gunlukSiparis = 0;
            
            // DÜZELTME: Aktif siparişlere değil, Kasadaki "GEÇMİŞ (ARŞİV) / ÖDENMİŞ" tablosuna bakar.
            String ciroCvp = sunucuyaKomutGonderVeCevapAl("KASA_GECMIS_GETIR");
            if (ciroCvp != null && ciroCvp.startsWith("KASA_GECMIS_VERI|") && ciroCvp.length() > 17) {
                String[] siparisler = ciroCvp.substring(17).split("\\|\\|\\|");
                for (String s : siparisler) {
                    if (s.trim().isEmpty()) continue;
                    String[] d = s.split("~_~"); 
                    if (d.length >= 5 && d[3].equals("ODENDI")) {
                        gunlukSiparis++;
                        String html = d[4];
                        int fBas = html.indexOf("<b>");
                        if (fBas != -1) {
                            int fEnd = html.indexOf("</b>", fBas);
                            if (fEnd != -1) {
                                try {
                                    gunlukCiro += Double.parseDouble(html.substring(fBas + 3, fEnd).replace(" TL", "").replace(",", ".").trim());
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            
            final double fCiro = gunlukCiro;
            final int fSiparis = gunlukSiparis;
            SwingUtilities.invokeLater(() -> { lblCiro.setText(fCiro + " TL"); lblSiparisSayisi.setText(fSiparis + " Adet"); });

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

    public String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080); PrintWriter out = new PrintWriter(s.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); out.println(komut); return in.readLine();
        } catch (Exception e) { return null; }
    }

    private JPanel bosSayfaOlustur(String mesaj) {
        JPanel p = new JPanel(new BorderLayout()); JLabel l = new JLabel(mesaj, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 24)); l.setForeground(Color.GRAY); p.add(l, BorderLayout.CENTER); return p;
    }
}