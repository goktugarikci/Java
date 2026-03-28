
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PersonelPaneli extends JFrame {
    private JPanel icerikPaneli;
    private CardLayout cardLayout;
    private String aktifPersonel;
    private String aktifRol;

    // Sipariş (Adisyon) Sepeti
    private DefaultTableModel sepetTableModel;
    private double toplamTutar = 0.0;
    private JLabel lblToplamTutar;

    // Canlı Masa Takibi
    private JPanel masalarPaneli;
    private Map<String, Long> siparisZamanlari = new HashMap<>(); 
    private Map<String, String> masaDurumlari = new HashMap<>(); 

    // Harici Modüller
    private MutfakModulu mutfakEkrani;
    private KasaModulu kasaEkrani;

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

        // Modülleri Başlatıyoruz
        mutfakEkrani = new MutfakModulu(this);
        kasaEkrani = new KasaModulu(this);

        icerikPaneli.add(kasaVeSiparisSayfasi(), "Masalar ve Sipariş");
        icerikPaneli.add(kasaEkrani, "Kasa Takip");
        icerikPaneli.add(mutfakEkrani, "Mutfak Panosu");
        icerikPaneli.add(sayfaOlustur("Vestiyer Modülü Yakında..."), "Vestiyer Modülü");

        add(icerikPaneli, BorderLayout.CENTER);

        // Rol tabanlı başlangıç ekranı yönlendirmesi
        if (aktifRol.equalsIgnoreCase("Mutfak")) {
            cardLayout.show(icerikPaneli, "Mutfak Panosu");
            mutfakEkrani.verileriYenile(); // Mutfak anında verileri çeker
        } else {
            cardLayout.show(icerikPaneli, "Masalar ve Sipariş");
            masaRenkGuncelleyiciyiBaslat(); // Masa süre ve renk sayacı başlar
        }
    }

    private void ustBarAyarla() {
        JPanel ustBar = new JPanel(new BorderLayout());
        ustBar.setBackground(new Color(39, 174, 96)); ustBar.setPreferredSize(new Dimension(0, 50));
        JLabel lblBaslik = new JLabel("  RESTORAN POS SİSTEMİ | Personel: " + aktifPersonel + " (" + aktifRol + ")");
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
        solMenu.add(new JLabel("<html><font color='white'><b>MODÜLLER</b></font></html>"));
        solMenu.add(Box.createVerticalStrut(15));

        // ROL BAZLI MENÜ FİLTRELEME
        if (aktifRol.equalsIgnoreCase("Kasiyer") || aktifRol.equalsIgnoreCase("Admin")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş"));
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Kasa / Ödemeler", "Kasa Takip"));
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Vestiyer Modülü", "Vestiyer Modülü"));
            solMenu.add(Box.createVerticalStrut(10));
        } else if (aktifRol.equalsIgnoreCase("Garson")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş"));
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Vestiyer Modülü", "Vestiyer Modülü"));
            solMenu.add(Box.createVerticalStrut(10));
        }

        if (aktifRol.equalsIgnoreCase("Mutfak") || aktifRol.equalsIgnoreCase("Admin")) {
            solMenu.add(menuButonuOlustur("Mutfak Panosu", "Mutfak Panosu"));
        }
        add(solMenu, BorderLayout.WEST);
    }

    private JButton menuButonuOlustur(String text, String cardName) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setMaximumSize(new Dimension(200, 55)); btn.setBackground(new Color(52, 73, 94)); btn.setForeground(Color.WHITE); 
        btn.setFocusPainted(false); btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.addActionListener(e -> {
            cardLayout.show(icerikPaneli, cardName);
            if(cardName.equals("Kasa Takip")) {
                kasaEkrani.verileriYenile(); // Kasa sekmesine geçince verileri tazele
            } else if (cardName.equals("Mutfak Panosu")) {
                mutfakEkrani.verileriYenile(); // Mutfak sekmesine geçince verileri tazele
            }
        });
        return btn;
    }

    private JPanel kasaVeSiparisSayfasi() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel baslikPanel = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("Aktif Masalar ve Sipariş Yönetimi"); lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        baslikPanel.add(lblBaslik, BorderLayout.WEST);
        
        if(!aktifRol.equalsIgnoreCase("Garson")) {
            JPanel pnlHizliSiparis = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPaket = new JButton("📦 Gel-Al Paket Sipariş");
            JButton btnEveServis = new JButton("🛵 Eve Servis (Telefon)");
            btnPaket.setBackground(new Color(230, 126, 34)); btnPaket.setForeground(Color.WHITE); btnPaket.setFont(new Font("Arial", Font.BOLD, 14));
            btnEveServis.setBackground(new Color(41, 128, 185)); btnEveServis.setForeground(Color.WHITE); btnEveServis.setFont(new Font("Arial", Font.BOLD, 14));
            
            btnPaket.addActionListener(e -> adisyonEkraniAc("PAKET", "Paket Müşterisi"));
            btnEveServis.addActionListener(e -> adisyonEkraniAc("EVE_SERVIS", "Eve Servis Müşterisi"));
            
            pnlHizliSiparis.add(btnPaket); pnlHizliSiparis.add(btnEveServis);
            baslikPanel.add(pnlHizliSiparis, BorderLayout.EAST);
        }

        masalarPaneli = new JPanel(new GridLayout(5, 4, 15, 15));
        masalarPaneli.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // 20 Masa Butonu (Grid)
        for (int i = 1; i <= 20; i++) {
            String masaIsmi = "Masa " + i;
            JButton masaButon = new JButton(masaIsmi);
            masaButon.setName(masaIsmi); 
            masaButon.setFont(new Font("Arial", Font.BOLD, 18)); masaButon.setBackground(new Color(236, 240, 241)); masaButon.setFocusPainted(false);
            masaButon.addActionListener(e -> adisyonEkraniAc("MASA", masaIsmi));
            masalarPaneli.add(masaButon);
        }
        panel.add(baslikPanel, BorderLayout.NORTH); panel.add(masalarPaneli, BorderLayout.CENTER);
        return panel;
    }

    private void adisyonEkraniAc(String siparisTuru, String baslikIsmi) {
        JDialog adisyonDialog = new JDialog(this, "Sipariş: " + baslikIsmi, true);
        adisyonDialog.setSize(1200, 750); adisyonDialog.setLayout(new BorderLayout()); adisyonDialog.setLocationRelativeTo(this);

        JPanel pnlMusteriBilgi = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlMusteriBilgi.setBackground(new Color(44, 62, 80)); 
        pnlMusteriBilgi.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(39, 174, 96)));
        
        JTextField txtMusteriAd = new JTextField(15);
        JTextField txtMusteriTel = new JTextField(12);
        JTextField txtMusteriAdres = new JTextField(25);
        
        JLabel lblMusteriAd = new JLabel(siparisTuru.equals("MASA") ? "Müşteri Adı (*):" : "Müşteri Adı (*):"); 
        lblMusteriAd.setForeground(Color.WHITE); lblMusteriAd.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel lblTelefon = new JLabel("Telefon:"); lblTelefon.setForeground(Color.WHITE); lblTelefon.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel lblAdres = new JLabel("Adres:"); lblAdres.setForeground(Color.WHITE); lblAdres.setFont(new Font("Arial", Font.BOLD, 14));

        pnlMusteriBilgi.add(lblMusteriAd); pnlMusteriBilgi.add(txtMusteriAd);
        
        if (siparisTuru.equals("EVE_SERVIS")) {
            pnlMusteriBilgi.add(lblTelefon); pnlMusteriBilgi.add(txtMusteriTel);
            pnlMusteriBilgi.add(lblAdres); pnlMusteriBilgi.add(txtMusteriAdres);
        }
        adisyonDialog.add(pnlMusteriBilgi, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); split.setDividerLocation(700);

        JPanel pnlUrunler = new JPanel(new BorderLayout());
        JPanel pnlKategoriler = new JPanel(new GridLayout(0, 1, 5, 5)); 
        pnlKategoriler.setBackground(new Color(52, 73, 94)); pnlKategoriler.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollKategori = new JScrollPane(pnlKategoriler); scrollKategori.setPreferredSize(new Dimension(160, 0));
        scrollKategori.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));

        JPanel pnlUrunListesi = new JPanel(new GridLayout(0, 3, 10, 10)); 
        pnlUrunListesi.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("KAT_LISTESI_GETIR");
            if (cevap != null && cevap.startsWith("KAT_LISTESI")) {
                String[] parcalar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    for (int i = 1; i < parcalar.length; i++) {
                        String katAdi = parcalar[i].split(";")[0];
                        JButton b = new JButton("<html><center>" + katAdi + "</center></html>"); 
                        b.setPreferredSize(new Dimension(130, 50)); b.setBackground(Color.DARK_GRAY); b.setForeground(Color.WHITE); b.setFont(new Font("Arial", Font.BOLD, 13)); b.setFocusPainted(false);
                        b.addActionListener(e -> yukleUrunleriButonOlarak(katAdi, pnlUrunListesi));
                        pnlKategoriler.add(b);
                    }
                    if(parcalar.length > 1) yukleUrunleriButonOlarak(parcalar[1].split(";")[0], pnlUrunListesi); 
                });
            }
        }).start();

        pnlUrunler.add(scrollKategori, BorderLayout.WEST); pnlUrunler.add(new JScrollPane(pnlUrunListesi), BorderLayout.CENTER);

        JPanel pnlSepet = new JPanel(new BorderLayout()); pnlSepet.setBorder(BorderFactory.createTitledBorder("Adisyon (Sepet)"));
        sepetTableModel = new DefaultTableModel(new String[]{"Ürün", "Adet", "Fiyat", "Not/Ekstra"}, 0);
        JTable sepetTablosu = new JTable(sepetTableModel); sepetTablosu.getColumnModel().getColumn(3).setPreferredWidth(200); 
        pnlSepet.add(new JScrollPane(sepetTablosu), BorderLayout.CENTER);

        JPanel pnlAlt = new JPanel(new BorderLayout());
        lblToplamTutar = new JLabel("Toplam: 0.0 TL"); lblToplamTutar.setFont(new Font("Arial", Font.BOLD, 22)); lblToplamTutar.setForeground(new Color(192, 57, 43));
        
        JButton btnMutfagaGonder = new JButton("Siparişi Mutfağa Gönder");
        btnMutfagaGonder.setBackground(new Color(39, 174, 96)); btnMutfagaGonder.setForeground(Color.WHITE); btnMutfagaGonder.setFont(new Font("Arial", Font.BOLD, 16));
        
        btnMutfagaGonder.addActionListener(e -> {
            String musteriAdi = txtMusteriAd.getText().trim();
            if(musteriAdi.isEmpty()) {
                JOptionPane.showMessageDialog(adisyonDialog, "Lütfen Müşteri ismini giriniz!", "Hata", JOptionPane.ERROR_MESSAGE); return;
            }
            if(sepetTableModel.getRowCount() == 0) { JOptionPane.showMessageDialog(adisyonDialog, "Sepet boş!"); return; }
            
            StringBuilder fis = new StringBuilder("<html>");
            String zaman = new SimpleDateFormat("HH:mm").format(new Date());
            String alanKisiHTML = "Siparişi Alan: <b>" + aktifPersonel + "</b><br>";
            
            if(siparisTuru.equals("EVE_SERVIS")) {
                fis.append("<b style='font-size:13px; color:red;'>[").append(zaman).append("] 🛵 EVE SERVİS</b><br>")
                   .append(alanKisiHTML).append("Müşteri: <b>").append(musteriAdi).append("</b> (").append(txtMusteriTel.getText()).append(")<br>")
                   .append("Adres: ").append(txtMusteriAdres.getText()).append("<br><hr>");
            } else if (siparisTuru.equals("PAKET")) {
                fis.append("<b style='font-size:13px; color:blue;'>[").append(zaman).append("] 📦 GEL-AL PAKET</b><br>")
                   .append(alanKisiHTML).append("Müşteri: <b>").append(musteriAdi).append("</b><br><hr>");
            } else {
                fis.append("<b style='font-size:13px; color:green;'>[").append(zaman).append("] 🍽 ").append(baslikIsmi).append("</b><br>")
                   .append(alanKisiHTML).append("Masa Müşterisi: <b>").append(musteriAdi).append("</b><br><hr>");
                siparisZamanlari.put(baslikIsmi, System.currentTimeMillis());
                masaDurumlari.put(baslikIsmi, "HAZIRLANIYOR");
            }

            for (int i = 0; i < sepetTableModel.getRowCount(); i++) {
                String urun = sepetTableModel.getValueAt(i, 0).toString();
                String adet = sepetTableModel.getValueAt(i, 1).toString();
                String notlar = sepetTableModel.getValueAt(i, 3).toString(); 
                
                fis.append("<b style='font-size:14px;'>").append(adet).append("x ").append(urun).append("</b><br>");
                
                if(!notlar.equals("Standart") && !notlar.isEmpty()) {
                    String[] notArray = notlar.split(" \\| ");
                    for(String n : notArray) {
                        if(n.startsWith("-")) fis.append("&nbsp;&nbsp;<b style='color:red;'>").append(n).append("</b><br>"); 
                        else if(n.startsWith("+")) fis.append("&nbsp;&nbsp;<b style='color:green;'>").append(n).append("</b><br>"); 
                        else if(n.startsWith("Not:")) fis.append("&nbsp;&nbsp;<i style='color:#444444;'>").append(n).append("</i><br>"); 
                    }
                }
            }
            fis.append("</html>");
            
            // Veritabanına (Sunucuya) gönder
            String gonderilecekKomut = "SIPARIS_OLUSTUR|" + baslikIsmi + "|" + musteriAdi + "|" + fis.toString();
            sunucuyaKomutGonderVeCevapAl(gonderilecekKomut);
            
            JOptionPane.showMessageDialog(adisyonDialog, "Sipariş mutfağa başarıyla iletildi!");
            adisyonDialog.dispose();
        });

        pnlAlt.add(lblToplamTutar, BorderLayout.WEST); pnlAlt.add(btnMutfagaGonder, BorderLayout.EAST);
        pnlSepet.add(pnlAlt, BorderLayout.SOUTH);

        split.setLeftComponent(pnlUrunler); split.setRightComponent(pnlSepet); adisyonDialog.add(split, BorderLayout.CENTER);
        adisyonDialog.setVisible(true);
    }

    private void yukleUrunleriButonOlarak(String kategoriAdi, JPanel pnlListe) {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + kategoriAdi);
            if(cevap != null && cevap.startsWith("URUN_LISTESI_DETAYLI")) {
                String[] urunler = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    pnlListe.removeAll();
                    for(int i = 1; i < urunler.length; i++) {
                        String[] d = urunler[i].split(";");
                        String ad = d[0]; double fiyat = Double.parseDouble(d[1]);
                        String malzemeler = (d.length > 5) ? d[5] : "";

                        JButton btnUrun = new JButton("<html><center>" + ad + "<br><b>" + fiyat + " TL</b></center></html>");
                        btnUrun.setBackground(new Color(52, 152, 219)); btnUrun.setForeground(Color.WHITE);
                        btnUrun.addActionListener(e -> urunOzellestirmePenceresi(ad, fiyat, malzemeler));
                        pnlListe.add(btnUrun);
                    }
                    pnlListe.revalidate(); pnlListe.repaint();
                });
            }
        }).start();
    }

    private void urunOzellestirmePenceresi(String urunAd, double tabanFiyat, String malzemelerStr) {
        JDialog d = new JDialog(this, urunAd + " - Özelleştir", true);
        d.setSize(450, 550); d.setLayout(new BorderLayout()); d.setLocationRelativeTo(this);

        JPanel pnlAna = new JPanel(); pnlAna.setLayout(new BoxLayout(pnlAna, BoxLayout.Y_AXIS)); pnlAna.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JPanel pnlCikarilabilir = new JPanel(new GridLayout(0, 1, 5, 5)); pnlCikarilabilir.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "İçindekiler (Çıkarılacakların tikini kaldırın)"));
        JPanel pnlEkstralar = new JPanel(new GridLayout(0, 1, 5, 5)); pnlEkstralar.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96)), "Ekstralar (Ücretli Eklentiler)"));

        if(!malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
            String[] malzemeler = malzemelerStr.split(",");
            for(String m : malzemeler) {
                String[] detay = m.split(":"); 
                if(detay.length == 3) {
                    boolean standartMi = detay[1].equals("1");
                    double ekUcret = Double.parseDouble(detay[2]);
                    if (standartMi) {
                        JCheckBox cb = new JCheckBox(detay[0], true); cb.setName(m); pnlCikarilabilir.add(cb);
                    } else {
                        JCheckBox cb = new JCheckBox(detay[0] + " (+ " + ekUcret + " TL)", false); cb.setName(m); pnlEkstralar.add(cb);
                    }
                }
            }
        }
        if (pnlCikarilabilir.getComponentCount() > 0) pnlAna.add(pnlCikarilabilir); pnlAna.add(Box.createVerticalStrut(10));
        if (pnlEkstralar.getComponentCount() > 0) pnlAna.add(pnlEkstralar);
        
        pnlAna.add(Box.createVerticalStrut(15)); pnlAna.add(new JLabel("<html><b>Aşçıya Özel Not:</b></html>"));
        JTextField txtNot = new JTextField(); txtNot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); pnlAna.add(txtNot);

        JButton btnSepeteEkle = new JButton("Sepete Ekle (" + tabanFiyat + " TL)"); btnSepeteEkle.setBackground(new Color(39, 174, 96)); btnSepeteEkle.setForeground(Color.WHITE); btnSepeteEkle.setFont(new Font("Arial", Font.BOLD, 15));
        
        btnSepeteEkle.addActionListener(e -> {
            double sonFiyat = tabanFiyat; 
            ArrayList<String> notListesi = new ArrayList<>(); 
            
            for (Component comp : pnlCikarilabilir.getComponents()) {
                if (comp instanceof JCheckBox) { 
                    JCheckBox cb = (JCheckBox) comp; 
                    if(!cb.isSelected()) notListesi.add("- " + cb.getName().split(":")[0]); 
                }
            }
            for (Component comp : pnlEkstralar.getComponents()) {
                if (comp instanceof JCheckBox) { 
                    JCheckBox cb = (JCheckBox) comp; 
                    if(cb.isSelected()) { 
                        String[] mDetay = cb.getName().split(":"); 
                        notListesi.add("+ " + mDetay[0]); 
                        sonFiyat += Double.parseDouble(mDetay[2]); 
                    } 
                }
            }
            if(!txtNot.getText().trim().isEmpty()) notListesi.add("Not: " + txtNot.getText().trim());
            
            String sonNot = String.join(" | ", notListesi);
            if(sonNot.isEmpty()) sonNot = "Standart";

            sepetTableModel.addRow(new Object[]{urunAd, 1, sonFiyat, sonNot});
            hesaplaToplam(); d.dispose();
        });
        d.add(new JScrollPane(pnlAna), BorderLayout.CENTER); d.add(btnSepeteEkle, BorderLayout.SOUTH); d.setVisible(true);
    }

    private void hesaplaToplam() {
        toplamTutar = 0; for (int i = 0; i < sepetTableModel.getRowCount(); i++) toplamTutar += Double.parseDouble(sepetTableModel.getValueAt(i, 2).toString());
        lblToplamTutar.setText("Toplam: " + toplamTutar + " TL");
    }

    // Kasadan masa ödemesi alındığında KasaModulu tarafından çağrılan Sıfırlayıcı Metot
    public void masayiSifirla(String masaAdi) {
        siparisZamanlari.remove(masaAdi);
        masaDurumlari.put(masaAdi, "BOS");
    }

    // Canlı Masa Takip Saati
    private void masaRenkGuncelleyiciyiBaslat() {
        Timer masaTimer = new Timer(1000, e -> { 
            long suan = System.currentTimeMillis();
            if(masalarPaneli != null) {
                for (Component c : masalarPaneli.getComponents()) {
                    if (c instanceof JButton) {
                        JButton btn = (JButton) c;
                        String mName = btn.getName();
                        String durum = masaDurumlari.getOrDefault(mName, "BOS");
                        Long acilisZamani = siparisZamanlari.get(mName);

                        if (durum.equals("BOS")) {
                            btn.setText(mName);
                            btn.setBackground(new Color(236, 240, 241));
                        } else {
                            long farkMs = suan - acilisZamani;
                            long dk = farkMs / (60 * 1000);
                            String saatStr = new SimpleDateFormat("HH:mm").format(new Date(acilisZamani));
                            
                            btn.setText("<html><center>" + mName + "<br><font size='3'>Giriş: " + saatStr + "<br><b>" + dk + " dk</b></font></center></html>");

                            if (durum.equals("HAZIRLANIYOR")) {
                                if (dk >= 10) btn.setBackground(new Color(231, 76, 60)); // 10 Dk üstü Kırmızı
                                else btn.setBackground(new Color(46, 204, 113)); // Yeşil
                            } else if (durum.equals("TESLIM_EDILDI")) {
                                btn.setBackground(new Color(241, 196, 15)); // Mutfak Onayladı (Sarı)
                            }
                        }
                    }
                }
            }
        });
        masaTimer.start();
    }

    // Tüm modüllerin erişebilmesi için PUBLIC yapıldı
    public String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080); PrintWriter out = new PrintWriter(s.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            in.readLine(); out.println(komut); return in.readLine();
        } catch (Exception e) { return null; }
    }

    private JPanel sayfaOlustur(String t) { JPanel p = new JPanel(new BorderLayout()); p.add(new JLabel(t, SwingConstants.CENTER)); return p; }
}