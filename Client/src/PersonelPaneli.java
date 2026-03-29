
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

    private DefaultTableModel sepetTableModel;
    private double toplamTutar = 0.0;
    private double gecerliOncekiTutar = 0.0;
    private JLabel lblToplamTutar;

    private JPanel masalarPaneli;
    private Map<String, Long> siparisZamanlari = new HashMap<>(); 
    private Map<String, String> masaDurumlari = new HashMap<>(); 
    private Map<String, String> bugunkuRezervasyonlar = new HashMap<>();

    private MutfakModulu mutfakEkrani;
    private KasaModulu kasaEkrani;
    private RezervasyonModulu rezervasyonEkrani;

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

        mutfakEkrani = new MutfakModulu(this);
        kasaEkrani = new KasaModulu(this);
        rezervasyonEkrani = new RezervasyonModulu(this);

        icerikPaneli.add(kasaVeSiparisSayfasi(), "Masalar ve Sipariş");
        icerikPaneli.add(kasaEkrani, "Kasa Takip");
        icerikPaneli.add(mutfakEkrani, "Mutfak Panosu");
        icerikPaneli.add(rezervasyonEkrani, "Rezervasyonlar");

        add(icerikPaneli, BorderLayout.CENTER);

        if (aktifRol.equalsIgnoreCase("Mutfak")) {
            cardLayout.show(icerikPaneli, "Mutfak Panosu");
            mutfakEkrani.verileriYenile(); 
        } else {
            cardLayout.show(icerikPaneli, "Masalar ve Sipariş");
            gercekMasalariSunucudanCek(); 
            masalariVeritabanindanGeriYukle(); 
            masaRenkGuncelleyiciyiBaslat(); 
        }
    }

    // --- GÜVENLİ SAYI DÖNÜŞTÜRÜCÜ ---
    private double guvenliDoubleCevir(String veri) {
        try {
            if (veri == null || veri.trim().isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(veri.replace(",", ".").trim());
        } catch (Exception e) {
            return 0.0;
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

        if (aktifRol.equalsIgnoreCase("Kasiyer") || aktifRol.equalsIgnoreCase("Admin")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Kasa / Ödemeler", "Kasa Takip")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Rezervasyonlar", "Rezervasyonlar")); 
            solMenu.add(Box.createVerticalStrut(10));
        } else if (aktifRol.equalsIgnoreCase("Garson")) {
            solMenu.add(menuButonuOlustur("Masalar ve Sipariş", "Masalar ve Sipariş")); 
            solMenu.add(Box.createVerticalStrut(10));
            solMenu.add(menuButonuOlustur("Rezervasyonlar", "Rezervasyonlar")); 
            solMenu.add(Box.createVerticalStrut(10));
        }
        
        if (aktifRol.equalsIgnoreCase("Mutfak") || aktifRol.equalsIgnoreCase("Admin")) {
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
            if (cardName.equals("Kasa Takip")) {
                kasaEkrani.verileriYenile(); 
            } else if (cardName.equals("Mutfak Panosu")) {
                mutfakEkrani.verileriYenile(); 
            } else if (cardName.equals("Rezervasyonlar")) {
                rezervasyonEkrani.verileriYenile(); 
            }
        });
        
        return btn;
    }

    private JPanel kasaVeSiparisSayfasi() {
        JPanel panel = new JPanel(new BorderLayout()); 
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel baslikPanel = new JPanel(new BorderLayout());
        JPanel pnlSolBaslik = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblBaslik = new JLabel("Aktif Masalar ve Sipariş Yönetimi"); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        
        JButton btnYenile = new JButton("🔄 Masaları Yenile");
        btnYenile.setBackground(new Color(52, 152, 219)); 
        btnYenile.setForeground(Color.WHITE); 
        btnYenile.setFocusPainted(false);
        
        btnYenile.addActionListener(e -> { 
            gercekMasalariSunucudanCek(); 
            masalariVeritabanindanGeriYukle(); 
        });
        
        pnlSolBaslik.add(lblBaslik); 
        pnlSolBaslik.add(btnYenile);
        baslikPanel.add(pnlSolBaslik, BorderLayout.WEST);
        
        if (!aktifRol.equalsIgnoreCase("Garson")) {
            JPanel pnlHizliSiparis = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPaket = new JButton("📦 Gel-Al Paket Sipariş"); 
            JButton btnEveServis = new JButton("🛵 Eve Servis (Telefon)");
            
            btnPaket.setBackground(new Color(230, 126, 34)); 
            btnPaket.setForeground(Color.WHITE); 
            btnPaket.setFont(new Font("Arial", Font.BOLD, 14));
            
            btnEveServis.setBackground(new Color(41, 128, 185)); 
            btnEveServis.setForeground(Color.WHITE); 
            btnEveServis.setFont(new Font("Arial", Font.BOLD, 14));
            
            btnPaket.addActionListener(e -> adisyonEkraniAc("PAKET", "Paket Müşterisi")); 
            btnEveServis.addActionListener(e -> adisyonEkraniAc("EVE_SERVIS", "Eve Servis Müşterisi"));
            
            pnlHizliSiparis.add(btnPaket); 
            pnlHizliSiparis.add(btnEveServis); 
            baslikPanel.add(pnlHizliSiparis, BorderLayout.EAST);
        }

        masalarPaneli = new JPanel(new GridLayout(0, 4, 15, 15)); 
        masalarPaneli.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel masaSarici = new JPanel(new BorderLayout()); 
        masaSarici.add(masalarPaneli, BorderLayout.NORTH); 
        
        panel.add(baslikPanel, BorderLayout.NORTH); 
        panel.add(new JScrollPane(masaSarici), BorderLayout.CENTER); 
        
        return panel;
    }

    private void gercekMasalariSunucudanCek() {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            if (cevap != null && cevap.startsWith("MASA_LISTESI")) {
                String[] parcalar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    masalarPaneli.removeAll(); 
                    
                    for (int i = 1; i < parcalar.length; i++) {
                        String[] detay = parcalar[i].split(";"); 
                        String masaIsmi = detay[0]; 

                        JButton masaButon = new JButton(masaIsmi); 
                        masaButon.setName(masaIsmi); 
                        masaButon.setFont(new Font("Arial", Font.BOLD, 18)); 
                        masaButon.setBackground(new Color(236, 240, 241)); 
                        masaButon.setFocusPainted(false); 
                        masaButon.setPreferredSize(new Dimension(200, 150)); 
                        
                        masaButon.addActionListener(e -> {
                            String durum = masaDurumlari.getOrDefault(masaIsmi, "BOS");
                            
                            // Rezerve Kontrolü
                            if (durum.equals("BOS") && bugunkuRezervasyonlar.containsKey(masaIsmi)) {
                                int onay = JOptionPane.showConfirmDialog(this, masaIsmi + " rezerve edilmiş.\nMüşteri geldi mi? (Siparişe Başla)", "Rezerve Masa", JOptionPane.YES_NO_OPTION);
                                if (onay == JOptionPane.YES_OPTION) {
                                    adisyonEkraniAc("MASA", masaIsmi);
                                }
                                return;
                            }

                            // Dolu Masa Kontrolü
                            if (!durum.equals("BOS") && !aktifRol.equalsIgnoreCase("Garson")) {
                                Object[] options = {"📝 Ek Sipariş Gir", "💵 Hesabı Kapat (Nakit)", "💳 Hesabı Kapat (Kredi Kartı)", "❌ Vazgeç"};
                                int secim = JOptionPane.showOptionDialog(this, masaIsmi + " şu an aktif.\nNe yapmak istersiniz?", "Masa İşlemleri", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                                if (secim == 0) {
                                    adisyonEkraniAc("MASA", masaIsmi);
                                } else if (secim == 1 || secim == 2) {
                                    String odemeTuru = (secim == 1) ? "Nakit" : "Kredi Kartı";
                                    ArrayList<String> orderIds = getMasaAdisyonIDleri(masaIsmi);
                                    
                                    if (!orderIds.isEmpty()) {
                                        int onay = JOptionPane.showConfirmDialog(this, masaIsmi + " hesabı " + odemeTuru + " olarak kapatılacak. Emin misiniz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION);
                                        if (onay == JOptionPane.YES_OPTION) {
                                            for (String id : orderIds) {
                                                sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + id + "|" + odemeTuru);
                                            }
                                            JOptionPane.showMessageDialog(this, "Hesap başarıyla alındı ve masa kapatıldı!");
                                            masayiSifirla(masaIsmi); 
                                            kasaEkrani.verileriYenile(); 
                                        }
                                    } else {
                                        JOptionPane.showMessageDialog(this, "Aktif bir sipariş bulunamadı!");
                                    }
                                }
                            } else {
                                adisyonEkraniAc("MASA", masaIsmi);
                            }
                        });
                        
                        masalarPaneli.add(masaButon);
                    }
                    masalarPaneli.revalidate(); 
                    masalarPaneli.repaint();
                });
            }
        }).start();
    }

    private ArrayList<String> getMasaAdisyonIDleri(String masaIsmi) {
        ArrayList<String> ids = new ArrayList<>();
        String cevap = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
        
        if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
            String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
            for (String s : siparisler) {
                if (s.trim().isEmpty()) {
                    continue;
                }
                String[] d = s.split("~_~"); 
                if (d.length >= 5 && d[1].equals(masaIsmi)) {
                    ids.add(d[0]); 
                }
            }
        } 
        return ids;
    }

    private void masalariVeritabanindanGeriYukle() {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("AKTIF_MASALARI_GETIR");
            if (cevap != null && cevap.startsWith("AKTIF_MASALAR|")) {
                
                Map<String, String> yeniDurumlar = new HashMap<>();
                Map<String, Long> yeniZamanlar = new HashMap<>();
                
                if (cevap.length() > 14) {
                    String[] masalar = cevap.substring(14).split("\\|\\|\\|");
                    for (String m : masalar) {
                        if (m.trim().isEmpty()) continue;
                        
                        String[] detay = m.split("~_~"); 
                        if (detay.length == 3) {
                            String masaAdi = detay[0]; 
                            String durum = detay[1]; 
                            String zamanStr = detay[2]; 
                            
                            long msZaman = System.currentTimeMillis();
                            try { 
                                msZaman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(zamanStr).getTime(); 
                            } catch(Exception ignored) {}
                            
                            if (durum.equals("BEKLEMEDE") || durum.equals("HAZIRLANIYOR")) {
                                yeniDurumlar.put(masaAdi, "HAZIRLANIYOR");
                            } else if (durum.equals("HAZIR") || durum.equals("TESLIM_EDILDI")) {
                                yeniDurumlar.put(masaAdi, "TESLIM_EDILDI");
                            }
                            
                            yeniZamanlar.put(masaAdi, msZaman);
                        }
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    masaDurumlari.clear();
                    masaDurumlari.putAll(yeniDurumlar);
                    
                    siparisZamanlari.clear();
                    siparisZamanlari.putAll(yeniZamanlar);
                });
            }
        }).start();
    }

    private void bugunkuRezervasyonlariGuncelle() {
        new Thread(() -> {
            String bugun = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String cevap = sunucuyaKomutGonderVeCevapAl("BUGUN_REZ_GETIR|" + bugun);
            
            if (cevap != null && cevap.startsWith("BUGUN_REZ|")) {
                Map<String, String> yeniRezler = new HashMap<>();
                if (cevap.length() > 10) {
                    String[] rezler = cevap.substring(10).split("\\|\\|\\|");
                    for (String r : rezler) {
                        if (r.trim().isEmpty()) continue;
                        String[] d = r.split("~_~"); 
                        if (d.length == 3) {
                            yeniRezler.put(d[0], d[1] + " - " + d[2]); 
                        }
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    bugunkuRezervasyonlar.clear();
                    bugunkuRezervasyonlar.putAll(yeniRezler);
                });
            }
        }).start();
    }

    private void adisyonEkraniAc(String siparisTuru, String baslikIsmi) {
        JDialog adisyonDialog = new JDialog(this, "Sipariş: " + baslikIsmi, true);
        adisyonDialog.setSize(1200, 750); 
        adisyonDialog.setLayout(new BorderLayout()); 
        adisyonDialog.setLocationRelativeTo(this);

        gecerliOncekiTutar = 0.0; 
        String oncekiMusteriIsmi = ""; 
        StringBuilder oncekiSiparisHTML = new StringBuilder();
        boolean ekSiparisMi = siparisTuru.equals("MASA") && !masaDurumlari.getOrDefault(baslikIsmi, "BOS").equals("BOS");

        if (ekSiparisMi) {
            String cevap = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
                String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
                for (String s : siparisler) {
                    if (s.trim().isEmpty()) {
                        continue;
                    }
                    String[] d = s.split("~_~"); 
                    if (d.length >= 5 && d[1].equals(baslikIsmi)) {
                        oncekiMusteriIsmi = d[2]; 
                        String html = d[4];
                        oncekiSiparisHTML.append("<div style='border-bottom: 1px dashed #ccc; padding-bottom: 5px; margin-bottom: 5px;'>")
                                         .append(html).append("</div>");
                        
                        // ONARILMIŞ GÜVENLİ FİYAT OKUMA MEKANİZMASI
                        int fBas = html.indexOf("<");
                        if (fBas != -1) {
                            int fEnd = html.indexOf("</b>", fBas);
                            if (fEnd != -1) {
                                String fiyatStr = html.substring(fBas + 10, fEnd);
                                gecerliOncekiTutar += guvenliDoubleCevir(fiyatStr);
                            }
                        }
                    }
                }
            }
        }

        JPanel pnlMusteriBilgi = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)); 
        pnlMusteriBilgi.setBackground(new Color(44, 62, 80)); 
        pnlMusteriBilgi.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(39, 174, 96)));
        
        JTextField txtMusteriAd = new JTextField(15); 
        if (!oncekiMusteriIsmi.isEmpty()) {
            txtMusteriAd.setText(oncekiMusteriIsmi); 
        }
        
        JTextField txtMusteriTel = new JTextField(12); 
        JTextField txtMusteriAdres = new JTextField(25);
        
        JLabel lblMusteriAd = new JLabel(siparisTuru.equals("MASA") ? "Müşteri Adı (*):" : "Müşteri Adı (*):"); 
        lblMusteriAd.setForeground(Color.WHITE); 
        lblMusteriAd.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblTelefon = new JLabel("Telefon:"); 
        lblTelefon.setForeground(Color.WHITE); 
        lblTelefon.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblAdres = new JLabel("Adres:"); 
        lblAdres.setForeground(Color.WHITE); 
        lblAdres.setFont(new Font("Arial", Font.BOLD, 14));

        pnlMusteriBilgi.add(lblMusteriAd); 
        pnlMusteriBilgi.add(txtMusteriAd);
        
        if (siparisTuru.equals("EVE_SERVIS")) { 
            pnlMusteriBilgi.add(lblTelefon); 
            pnlMusteriBilgi.add(txtMusteriTel); 
            pnlMusteriBilgi.add(lblAdres); 
            pnlMusteriBilgi.add(txtMusteriAdres); 
        }
        
        adisyonDialog.add(pnlMusteriBilgi, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        split.setDividerLocation(700);

        JPanel pnlUrunler = new JPanel(new BorderLayout());
        JPanel pnlKategoriler = new JPanel(new GridLayout(0, 1, 5, 5)); 
        pnlKategoriler.setBackground(new Color(52, 73, 94)); 
        pnlKategoriler.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollKategori = new JScrollPane(pnlKategoriler); 
        scrollKategori.setPreferredSize(new Dimension(160, 0)); 
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
                        b.setPreferredSize(new Dimension(130, 50)); 
                        b.setBackground(Color.DARK_GRAY); 
                        b.setForeground(Color.WHITE); 
                        b.setFont(new Font("Arial", Font.BOLD, 13)); 
                        b.setFocusPainted(false);
                        
                        b.addActionListener(e -> yukleUrunleriButonOlarak(katAdi, pnlUrunListesi)); 
                        pnlKategoriler.add(b);
                    }
                    if (parcalar.length > 1) {
                        yukleUrunleriButonOlarak(parcalar[1].split(";")[0], pnlUrunListesi); 
                    }
                });
            }
        }).start();

        pnlUrunler.add(scrollKategori, BorderLayout.WEST); 
        pnlUrunler.add(new JScrollPane(pnlUrunListesi), BorderLayout.CENTER);

        JPanel pnlSepet = new JPanel(new BorderLayout(5, 5)); 
        pnlSepet.setBorder(BorderFactory.createTitledBorder("Adisyon (Sepet)"));
        
        if (ekSiparisMi && oncekiSiparisHTML.length() > 0) {
            JEditorPane txtEski = new JEditorPane(); 
            txtEski.setContentType("text/html"); 
            txtEski.setText("<html><div style='font-family: Arial; font-size: 11px;'>" + oncekiSiparisHTML.toString() + "</div></html>"); 
            txtEski.setEditable(false); 
            txtEski.setBackground(new Color(250, 250, 250));
            
            JScrollPane scrollEski = new JScrollPane(txtEski); 
            scrollEski.setPreferredSize(new Dimension(0, 200)); 
            scrollEski.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Daha Önce Söylenenler (Mevcut Borç: " + gecerliOncekiTutar + " TL)"));
            pnlSepet.add(scrollEski, BorderLayout.NORTH);
        }

        sepetTableModel = new DefaultTableModel(new String[]{"Ürün", "Adet", "Fiyat", "Not/Ekstra"}, 0);
        JTable sepetTablosu = new JTable(sepetTableModel); 
        sepetTablosu.getColumnModel().getColumn(3).setPreferredWidth(200); 
        pnlSepet.add(new JScrollPane(sepetTablosu), BorderLayout.CENTER);

        JPanel pnlAlt = new JPanel(new BorderLayout()); 
        lblToplamTutar = new JLabel("Toplam: 0.0 TL"); 
        lblToplamTutar.setFont(new Font("Arial", Font.BOLD, 14)); 
        
        hesaplaToplam(); 

        JButton btnMutfagaGonder = new JButton("Yeni Siparişi Mutfağa Gönder"); 
        btnMutfagaGonder.setBackground(new Color(39, 174, 96)); 
        btnMutfagaGonder.setForeground(Color.WHITE); 
        btnMutfagaGonder.setFont(new Font("Arial", Font.BOLD, 15));
        
        btnMutfagaGonder.addActionListener(e -> {
            String musteriAdi = txtMusteriAd.getText().trim();
            
            if (musteriAdi.isEmpty()) { 
                JOptionPane.showMessageDialog(adisyonDialog, "Lütfen Müşteri ismini giriniz!", "Hata", JOptionPane.ERROR_MESSAGE); 
                return; 
            }
            if (sepetTableModel.getRowCount() == 0) { 
                JOptionPane.showMessageDialog(adisyonDialog, "Sepet boş! Ek sipariş vermeyecekseniz pencereyi kapatın."); 
                return; 
            }
            
            StringBuilder fis = new StringBuilder("<html>"); 
            String zaman = new SimpleDateFormat("HH:mm").format(new Date()); 
            String alanKisiHTML = "Siparişi Alan: <b>" + aktifPersonel + "</b><br>";
            
            if (siparisTuru.equals("EVE_SERVIS")) {
                fis.append("<b style='font-size:13px; color:red;'>[").append(zaman).append("] 🛵 EVE SERVİS</b><br>")
                   .append(alanKisiHTML)
                   .append("Müşteri: <b>").append(musteriAdi).append("</b> (").append(txtMusteriTel.getText()).append(")<br>")
                   .append("Adres: ").append(txtMusteriAdres.getText()).append("<br><hr>");
            } else if (siparisTuru.equals("PAKET")) {
                fis.append("<b style='font-size:13px; color:blue;'>[").append(zaman).append("] 📦 GEL-AL PAKET</b><br>")
                   .append(alanKisiHTML)
                   .append("Müşteri: <b>").append(musteriAdi).append("</b><br><hr>");
            } else { 
                String tag = ekSiparisMi ? "➕ EK SİPARİŞ" : "🍽 YENİ MASA"; 
                fis.append("<b style='font-size:13px; color:green;'>[").append(zaman).append("] ").append(tag).append(" (").append(baslikIsmi).append(")</b><br>")
                   .append(alanKisiHTML)
                   .append("Müşteri: <b>").append(musteriAdi).append("</b><br><hr>"); 
                   
                siparisZamanlari.putIfAbsent(baslikIsmi, System.currentTimeMillis()); 
                masaDurumlari.put(baslikIsmi, "HAZIRLANIYOR"); 
            }

            for (int i = 0; i < sepetTableModel.getRowCount(); i++) {
                String urun = sepetTableModel.getValueAt(i, 0).toString(); 
                String adet = sepetTableModel.getValueAt(i, 1).toString(); 
                String notlar = sepetTableModel.getValueAt(i, 3).toString(); 
                
                fis.append("<b style='font-size:14px;'>").append(adet).append("x ").append(urun).append("</b><br>");
                
                if (!notlar.equals("Standart") && !notlar.isEmpty()) { 
                    String[] notArray = notlar.split(" \\| "); 
                    for (String n : notArray) { 
                        if (n.startsWith("-")) {
                            fis.append("&nbsp;&nbsp;<b style='color:red;'>").append(n).append("</b><br>"); 
                        } else if (n.startsWith("+")) {
                            fis.append("&nbsp;&nbsp;<b style='color:green;'>").append(n).append("</b><br>"); 
                        } else if (n.startsWith("Not:")) {
                            fis.append("&nbsp;&nbsp;<i style='color:#444444;'>").append(n).append("</i><br>"); 
                        }
                    } 
                }
            }
            
            fis.append("</html>");
            
            sunucuyaKomutGonderVeCevapAl("SIPARIS_OLUSTUR|" + baslikIsmi + "|" + musteriAdi + "|" + fis.toString());
            
            if (bugunkuRezervasyonlar.containsKey(baslikIsmi)) {
                bugunkuRezervasyonlar.remove(baslikIsmi);
            }

            JOptionPane.showMessageDialog(adisyonDialog, "Sipariş mutfağa başarıyla iletildi!"); 
            adisyonDialog.dispose();
        });

        pnlAlt.add(lblToplamTutar, BorderLayout.WEST); 
        pnlAlt.add(btnMutfagaGonder, BorderLayout.EAST); 
        pnlSepet.add(pnlAlt, BorderLayout.SOUTH);
        
        split.setLeftComponent(pnlUrunler); 
        split.setRightComponent(pnlSepet); 
        
        adisyonDialog.add(split, BorderLayout.CENTER); 
        adisyonDialog.setVisible(true);
    }

    private void yukleUrunleriButonOlarak(String kategoriAdi, JPanel pnlListe) {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + kategoriAdi);
            if (cevap != null && cevap.startsWith("URUN_LISTESI_DETAYLI")) {
                String[] urunler = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    pnlListe.removeAll();
                    for (int i = 1; i < urunler.length; i++) {
                        String[] d = urunler[i].split(";"); 
                        String ad = d[0]; 
                        double fiyat = guvenliDoubleCevir(d[1]); 
                        String malzemeler = (d.length > 5) ? d[5] : "";
                        
                        JButton btnUrun = new JButton("<html><center>" + ad + "<br><b>" + fiyat + " TL</b></center></html>"); 
                        btnUrun.setBackground(new Color(52, 152, 219)); 
                        btnUrun.setForeground(Color.WHITE);
                        
                        btnUrun.addActionListener(e -> urunOzellestirmePenceresi(ad, fiyat, malzemeler)); 
                        pnlListe.add(btnUrun);
                    }
                    pnlListe.revalidate(); 
                    pnlListe.repaint();
                });
            }
        }).start();
    }

    private void urunOzellestirmePenceresi(String urunAd, double tabanFiyat, String malzemelerStr) {
        JDialog d = new JDialog(this, urunAd + " - Özelleştir", true); 
        d.setSize(450, 550); 
        d.setLayout(new BorderLayout()); 
        d.setLocationRelativeTo(this);
        
        JPanel pnlAna = new JPanel(); 
        pnlAna.setLayout(new BoxLayout(pnlAna, BoxLayout.Y_AXIS)); 
        pnlAna.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JPanel pnlCikarilabilir = new JPanel(new GridLayout(0, 1, 5, 5)); 
        pnlCikarilabilir.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "İçindekiler (Çıkarılacakların tikini kaldırın)"));
        
        JPanel pnlEkstralar = new JPanel(new GridLayout(0, 1, 5, 5)); 
        pnlEkstralar.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96)), "Ekstralar (Ücretli Eklentiler)"));

        if (!malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
            String[] malzemeler = malzemelerStr.split(",");
            for (String m : malzemeler) {
                String[] detay = m.split(":"); 
                if (detay.length == 3) {
                    boolean standartMi = detay[1].equals("1"); 
                    double ekUcret = guvenliDoubleCevir(detay[2]);
                    
                    if (standartMi) { 
                        JCheckBox cb = new JCheckBox(detay[0], true); 
                        cb.setName(m); 
                        pnlCikarilabilir.add(cb); 
                    } else { 
                        JCheckBox cb = new JCheckBox(detay[0] + " (+ " + ekUcret + " TL)", false); 
                        cb.setName(m); 
                        pnlEkstralar.add(cb); 
                    }
                }
            }
        }
        
        if (pnlCikarilabilir.getComponentCount() > 0) {
            pnlAna.add(pnlCikarilabilir); 
        }
        pnlAna.add(Box.createVerticalStrut(10));
        
        if (pnlEkstralar.getComponentCount() > 0) {
            pnlAna.add(pnlEkstralar);
        }
        pnlAna.add(Box.createVerticalStrut(15)); 
        pnlAna.add(new JLabel("<html><b>Aşçıya Özel Not:</b></html>")); 
        
        JTextField txtNot = new JTextField(); 
        txtNot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 
        pnlAna.add(txtNot);
        
        JButton btnSepeteEkle = new JButton("Sepete Ekle (" + tabanFiyat + " TL)"); 
        btnSepeteEkle.setBackground(new Color(39, 174, 96)); 
        btnSepeteEkle.setForeground(Color.WHITE); 
        btnSepeteEkle.setFont(new Font("Arial", Font.BOLD, 15));
        
        btnSepeteEkle.addActionListener(e -> {
            double sonFiyat = tabanFiyat; 
            ArrayList<String> notListesi = new ArrayList<>(); 
            
            for (Component comp : pnlCikarilabilir.getComponents()) { 
                if (comp instanceof JCheckBox) { 
                    JCheckBox cb = (JCheckBox) comp; 
                    if (!cb.isSelected()) {
                        notListesi.add("- " + cb.getName().split(":")[0]); 
                    }
                } 
            }
            
            for (Component comp : pnlEkstralar.getComponents()) { 
                if (comp instanceof JCheckBox) { 
                    JCheckBox cb = (JCheckBox) comp; 
                    if (cb.isSelected()) { 
                        String[] mDetay = cb.getName().split(":"); 
                        notListesi.add("+ " + mDetay[0]); 
                        sonFiyat += guvenliDoubleCevir(mDetay[2]); 
                    } 
                } 
            }
            
            if (!txtNot.getText().trim().isEmpty()) {
                notListesi.add("Not: " + txtNot.getText().trim());
            }
            
            String sonNot = String.join(" | ", notListesi); 
            if (sonNot.isEmpty()) {
                sonNot = "Standart";
            }
            
            sepetTableModel.addRow(new Object[]{urunAd, 1, sonFiyat, sonNot}); 
            hesaplaToplam(); 
            d.dispose();
        });
        
        d.add(new JScrollPane(pnlAna), BorderLayout.CENTER); 
        d.add(btnSepeteEkle, BorderLayout.SOUTH); 
        d.setVisible(true);
    }

    private void hesaplaToplam() {
        toplamTutar = 0; 
        for (int i = 0; i < sepetTableModel.getRowCount(); i++) {
            toplamTutar += guvenliDoubleCevir(sepetTableModel.getValueAt(i, 2).toString());
        }
        
        if (gecerliOncekiTutar > 0) {
            lblToplamTutar.setText("<html>Ara Toplam: <b>" + gecerliOncekiTutar + " TL</b><br>Yeni Sipariş: <b>" + toplamTutar + " TL</b><br><font size='5' color='red'>GENEL TOPLAM: " + (gecerliOncekiTutar + toplamTutar) + " TL</font></html>");
        } else {
            lblToplamTutar.setText("<html><font size='5' color='red'>Toplam: " + toplamTutar + " TL</font></html>");
        }
    }

    public void masayiSifirla(String masaAdi) {
        siparisZamanlari.remove(masaAdi); 
        masaDurumlari.put(masaAdi, "BOS");
        
        if (masalarPaneli != null) {
            for (Component c : masalarPaneli.getComponents()) {
                if (c instanceof JButton && c.getName() != null && c.getName().equals(masaAdi)) { 
                    ((JButton) c).setText(masaAdi); 
                    c.setBackground(new Color(236, 240, 241)); 
                }
            }
        }
    }

    private void masaRenkGuncelleyiciyiBaslat() {
        Timer masaTimer = new Timer(5000, e -> { 
            bugunkuRezervasyonlariGuncelle(); 
            masalariVeritabanindanGeriYukle(); 
            
            long suan = System.currentTimeMillis();
            if (masalarPaneli != null) {
                for (Component c : masalarPaneli.getComponents()) {
                    if (c instanceof JButton) {
                        guncelleMasaButonu((JButton) c, suan);
                    }
                }
            }
        });
        masaTimer.start();
    }

    private void guncelleMasaButonu(JButton btn, long suan) {
        String mName = btn.getName();
        if (mName == null) return;
        
        String durum = masaDurumlari.getOrDefault(mName, "BOS");
        Long acilisZamani = siparisZamanlari.get(mName);
        long acilis = (acilisZamani != null) ? acilisZamani : suan; 

        if (durum.equals("BOS")) {
            String rezBilgisi = bugunkuRezervasyonlar.get(mName);
            if (rezBilgisi != null) {
                btn.setText("<html><center>" + mName + "<br><font size='3' color='white'><b>REZERVE</b><br>" + rezBilgisi + "</font></center></html>");
                btn.setBackground(new Color(142, 68, 173)); 
            } else {
                btn.setText(mName); 
                btn.setBackground(new Color(236, 240, 241)); 
            }
        } else {
            long farkMs = suan - acilis;
            long dk = farkMs / (60 * 1000);
            String saatStr = new SimpleDateFormat("HH:mm").format(new Date(acilis));
            
            btn.setText("<html><center>" + mName + "<br><font size='3'>Giriş: " + saatStr + "<br><b>" + dk + " dk</b></font></center></html>");

            if (durum.equals("HAZIRLANIYOR")) {
                if (dk >= 10) {
                    btn.setBackground(new Color(231, 76, 60)); 
                } else {
                    btn.setBackground(new Color(46, 204, 113)); 
                }
            } else if (durum.equals("TESLIM_EDILDI")) {
                btn.setBackground(new Color(243, 156, 18)); 
            }
        }
    }

    String sunucuyaKomutGonderVeCevapAl(String komut) {
        try (Socket s = new Socket("localhost", 8080); 
             PrintWriter out = new PrintWriter(s.getOutputStream(), true); 
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            
            in.readLine(); 
            out.println(komut); 
            return in.readLine();
            
        } catch (Exception e) { 
            return null; 
        }
    }

    private JPanel sayfaOlustur(String t) { 
        JPanel p = new JPanel(new BorderLayout()); 
        p.add(new JLabel(t, SwingConstants.CENTER)); 
        return p; 
    }
}