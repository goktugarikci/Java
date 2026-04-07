
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiparisModulu extends JPanel {
    private JFrame anaPanel;
    private String aktifPersonel;
    private String aktifRol;
    
    private JPanel pnlMasalar;
    private Timer zamanlayici;
    private Map<String, String> masaDurumlari;
    private Map<String, String> masaZamanlari;
    private Map<JButton, String> masaButonlari;

    // ==========================================
    // 1. PERSONEL PANELİ İÇİN CONSTRUCTOR
    // ==========================================
    public SiparisModulu(PersonelPaneli anaPanel, String aktifPersonel) {
        this.anaPanel = anaPanel;
        this.aktifPersonel = aktifPersonel;
        this.aktifRol = "Personel";
        arayuzuKur();
    }

    // ==========================================
    // 2. ADMİN PANELİ İÇİN CONSTRUCTOR (Yedek)
    // ==========================================
    public SiparisModulu(JFrame anaPanel, String aktifPersonel, String aktifRol) {
        this.anaPanel = anaPanel;
        this.aktifPersonel = aktifPersonel;
        this.aktifRol = aktifRol;
        arayuzuKur();
    }

    private void arayuzuKur() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        masaDurumlari = new HashMap<>();
        masaZamanlari = new HashMap<>();
        masaButonlari = new HashMap<>();

        // ==========================================
        // ÜST BÖLÜM: BAŞLIK VE LOGOLU YENİ BUTONLAR
        // ==========================================
        JPanel pnlUst = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("🍽️ Restoran Masa ve Sipariş Yönetimi");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        
        JPanel pnlUstButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlUstButonlar.setOpaque(false);

        // LOGOLU TELEFON SİPARİŞİ BUTONU
        JButton btnTelefon = new JButton("<html><center><font size='6'>📞</font><br>Telefon Siparişi</center></html>");
        btnTelefon.setPreferredSize(new Dimension(140, 60));
        btnTelefon.setBackground(new Color(155, 89, 182)); // Mor Tema
        btnTelefon.setForeground(Color.WHITE);
        btnTelefon.setFont(new Font("Arial", Font.BOLD, 13));
        btnTelefon.setFocusPainted(false);
        btnTelefon.addActionListener(e -> adisyonAc("EVE_SERVIS", "Telefon Siparişi"));

        // LOGOLU PAKET SİPARİŞ BUTONU
        JButton btnPaket = new JButton("<html><center><font size='6'>📦</font><br>Paket Siparişi</center></html>");
        btnPaket.setPreferredSize(new Dimension(140, 60));
        btnPaket.setBackground(new Color(230, 126, 34)); // Turuncu Tema
        btnPaket.setForeground(Color.WHITE);
        btnPaket.setFont(new Font("Arial", Font.BOLD, 13));
        btnPaket.setFocusPainted(false);
        btnPaket.addActionListener(e -> adisyonAc("PAKET", "Gel-Al Paket Sipariş"));

        // LOGOLU YENİLE BUTONU
        JButton btnYenile = new JButton("<html><center><font size='6'>🔄</font><br>Ekranı Yenile</center></html>");
        btnYenile.setPreferredSize(new Dimension(140, 60));
        btnYenile.setBackground(new Color(52, 152, 219)); // Mavi Tema
        btnYenile.setForeground(Color.WHITE);
        btnYenile.setFont(new Font("Arial", Font.BOLD, 13));
        btnYenile.setFocusPainted(false);
        btnYenile.addActionListener(e -> baslat());

        pnlUstButonlar.add(btnTelefon);
        pnlUstButonlar.add(btnPaket);
        pnlUstButonlar.add(btnYenile);

        pnlUst.add(lblBaslik, BorderLayout.WEST);
        pnlUst.add(pnlUstButonlar, BorderLayout.EAST);
        add(pnlUst, BorderLayout.NORTH);

        // ==========================================
        // MASALAR BÖLÜMÜ (KARE TASARIM İÇİN WRAPPER)
        // ==========================================
        pnlMasalar = new JPanel(new GridLayout(0, 6, 15, 15)); 
        
        JPanel pnlWrapper = new JPanel(new BorderLayout());
        pnlWrapper.add(pnlMasalar, BorderLayout.NORTH);
        
        JScrollPane scrollPane = new JScrollPane(pnlWrapper);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        zamanlayici = new Timer(1000, e -> sureleriGuncelle());
        zamanlayici.start();
        
        baslat();
    }

    public void baslat() {
        new Thread(() -> {
            masaDurumlari.clear();
            masaZamanlari.clear();
            
            String aktifCvp = sunucuyaKomutGonderVeCevapAl("AKTIF_MASALARI_GETIR");
            if (aktifCvp != null && aktifCvp.startsWith("AKTIF_MASALAR|") && aktifCvp.length() > 14) {
                String[] masalar = aktifCvp.substring(14).split("\\|\\|\\|");
                for (String m : masalar) {
                    if (m.trim().isEmpty()) continue;
                    String[] detay = m.split("~_~");
                    if (detay.length >= 3) {
                        masaDurumlari.put(detay[0], detay[1]);
                        masaZamanlari.put(detay[0], detay[2]);
                    }
                }
            }

            String masaCvp = sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            if (masaCvp == null || !masaCvp.startsWith("MASA_LISTESI")) {
                masaCvp = sunucuyaKomutGonderVeCevapAl("MASA_LISTESI"); 
            }

            final String finalMasaCvp = masaCvp;

            SwingUtilities.invokeLater(() -> {
                pnlMasalar.removeAll();
                masaButonlari.clear();
                
                if (finalMasaCvp != null && finalMasaCvp.startsWith("MASA_LISTESI|") && finalMasaCvp.length() > 13) {
                    String[] tumMasalar = finalMasaCvp.substring(13).split("\\|");
                    for (String m : tumMasalar) {
                        if (m.trim().isEmpty()) continue;
                        String masaAdi = m.split(";")[0];
                        
                        JButton btn = new JButton();
                        btn.setPreferredSize(new Dimension(150, 150)); 
                        btn.setFont(new Font("Arial", Font.BOLD, 18)); 
                        btn.setFocusPainted(false);
                        
                        String durum = masaDurumlari.getOrDefault(masaAdi, "BOS");
                        if (durum.equals("BOS")) {
                            btn.setBackground(new Color(46, 204, 113)); 
                            btn.setForeground(Color.WHITE);
                            btn.setText("<html><center>" + masaAdi + "<br><br><font size='4'>BOS</font></center></html>");
                        } else {
                            btn.setBackground(new Color(231, 76, 60)); 
                            btn.setForeground(Color.WHITE);
                            masaButonlari.put(btn, masaAdi); 
                            btn.setText("<html><center>" + masaAdi + "<br><font size='4'>" + durum + "</font><br><br><font size='4' color='yellow'>⏱️ 00:00</font></center></html>");
                        }
                        
                        btn.addActionListener(e -> {
                            if (durum.equals("BOS")) {
                                adisyonAc("MASA", masaAdi);
                            } else {
                                masaSecenekleriGoster(masaAdi);
                            }
                        });
                        
                        pnlMasalar.add(btn);
                    }
                }
                sureleriGuncelle();
                pnlMasalar.revalidate();
                pnlMasalar.repaint();
            });
        }).start();
    }

    private void sureleriGuncelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date suan = new Date();
        
        for (Map.Entry<JButton, String> entry : masaButonlari.entrySet()) {
            JButton btn = entry.getKey();
            String masaAdi = entry.getValue();
            String baslangicZamani = masaZamanlari.get(masaAdi);
            String durum = masaDurumlari.getOrDefault(masaAdi, "DOLU");
            
            if (baslangicZamani != null) {
                try {
                    Date d = sdf.parse(baslangicZamani);
                    long fark = suan.getTime() - d.getTime();
                    if (fark > 0) {
                        long s = (fark / 1000) % 60;
                        long m = (fark / (1000 * 60)) % 60;
                        long h = (fark / (1000 * 60 * 60)) % 24;
                        String sureStr = String.format("%02d:%02d:%02d", h, m, s);
                        btn.setText("<html><center>" + masaAdi + "<br><font size='4'>" + durum + "</font><br><br><font size='4' color='yellow'>⏱️ " + sureStr + "</font></center></html>");
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void masaSecenekleriGoster(String masaAdi) {
        JDialog dialog = new JDialog(anaPanel, "İşlem Seç - " + masaAdi, true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(anaPanel);
        
        JButton btnEkSiparis = new JButton("➕ Ek Sipariş Gir");
        btnEkSiparis.setBackground(new Color(52, 152, 219));
        btnEkSiparis.setForeground(Color.WHITE);
        btnEkSiparis.setFont(new Font("Arial", Font.BOLD, 18));
        
        JButton btnOdemeAl = new JButton("💳 Ödeme Al");
        btnOdemeAl.setBackground(new Color(39, 174, 96));
        btnOdemeAl.setForeground(Color.WHITE);
        btnOdemeAl.setFont(new Font("Arial", Font.BOLD, 18));
        
        JButton btnIptal = new JButton("❌ Siparişi İptal Et");
        btnIptal.setBackground(new Color(192, 57, 43));
        btnIptal.setForeground(Color.WHITE);
        btnIptal.setFont(new Font("Arial", Font.BOLD, 18));
        
        JButton btnVazgec = new JButton("↩️ Vazgeç / Kapat");
        btnVazgec.setFont(new Font("Arial", Font.BOLD, 18));
        
        btnEkSiparis.addActionListener(e -> {
            dialog.dispose();
            adisyonAc("MASA", masaAdi);
        });
        
        btnOdemeAl.addActionListener(e -> {
            dialog.dispose();
            odemeEkraniAc(masaAdi);
        });
        
        btnIptal.addActionListener(e -> {
            int onay = JOptionPane.showConfirmDialog(dialog, masaAdi + " masasına ait sipariş tamamen iptal edilecek. Emin misiniz?", "İptal Onayı", JOptionPane.YES_NO_OPTION);
            if (onay == JOptionPane.YES_OPTION) {
                String[] detay = aktifSiparisDetayiGetir(masaAdi);
                if (detay != null) {
                    sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + detay[0] + "|IPTAL");
                    masayiSifirla(masaAdi);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Hata: Masaya ait aktif sipariş bulunamadı.");
                }
                dialog.dispose();
            }
        });
        
        btnVazgec.addActionListener(e -> dialog.dispose());
        
        JPanel pnlButonlar = new JPanel(new GridLayout(4, 1, 10, 15));
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pnlButonlar.add(btnEkSiparis); 
        pnlButonlar.add(btnOdemeAl); 
        pnlButonlar.add(btnIptal); 
        pnlButonlar.add(btnVazgec);
        
        dialog.setContentPane(pnlButonlar);
        dialog.setVisible(true);
    }

    private void odemeEkraniAc(String masaAdi) {
        String[] detay = aktifSiparisDetayiGetir(masaAdi);
        if (detay == null) {
            JOptionPane.showMessageDialog(anaPanel, "Bu masaya ait aktif sipariş bulunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String orderId = detay[0]; 
        String musteriAdi = detay[2]; 
        String htmlFis = detay[4];
        
        String toplamTutar = fiyatiSok(htmlFis);
        
        String siparisZamaniStr = masaZamanlari.get(masaAdi);
        String gecenSure = "Bilinmiyor";
        if (siparisZamaniStr != null) {
            try {
                Date baslangic = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(siparisZamaniStr);
                long farkMs = new Date().getTime() - baslangic.getTime();
                if (farkMs > 0) {
                    long s = (farkMs / 1000) % 60;
                    long m = (farkMs / (1000 * 60)) % 60;
                    long h = (farkMs / (1000 * 60 * 60)) % 24;
                    gecenSure = String.format("%02d:%02d:%02d", h, m, s);
                }
            } catch (Exception ignored) {}
        }
        
        JDialog d = new JDialog(anaPanel, "💳 Hızlı Ödeme Al - " + masaAdi, true);
        d.setSize(600, 750);
        d.setLayout(new BorderLayout(10, 10));
        d.setLocationRelativeTo(anaPanel);
        
        JPanel pnlUst = new JPanel(new GridLayout(3, 1, 5, 5));
        pnlUst.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        pnlUst.setBackground(new Color(44, 62, 80)); 
        
        JLabel lblMusteri = new JLabel("👤 Müşteri: " + musteriAdi); 
        lblMusteri.setForeground(Color.WHITE); 
        lblMusteri.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel lblSure = new JLabel("⏱️ Masada Geçen Süre: " + gecenSure); 
        lblSure.setForeground(new Color(241, 196, 15)); 
        lblSure.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel lblTutar = new JLabel("💰 Toplam Tutar: " + toplamTutar + " TL"); 
        lblTutar.setForeground(new Color(231, 76, 60)); 
        lblTutar.setFont(new Font("Arial", Font.BOLD, 26));
        
        pnlUst.add(lblMusteri); 
        pnlUst.add(lblSure); 
        pnlUst.add(lblTutar);
        d.add(pnlUst, BorderLayout.NORTH);
        
        JEditorPane txtIcerik = new JEditorPane("text/html", "<div style='font-family: Arial; padding: 15px; font-size: 15px;'>" + htmlFis + "</div>");
        txtIcerik.setEditable(false);
        d.add(new JScrollPane(txtIcerik), BorderLayout.CENTER);
        
        JPanel pnlAlt = new JPanel(new GridLayout(1, 2, 15, 10));
        pnlAlt.setBorder(BorderFactory.createEmptyBorder(15, 15, 20, 15));
        
        JButton btnNakit = new JButton("💵 Nakit Kapat"); 
        btnNakit.setBackground(new Color(39, 174, 96)); 
        btnNakit.setForeground(Color.WHITE); 
        btnNakit.setFont(new Font("Arial", Font.BOLD, 20));
        
        JButton btnKredi = new JButton("💳 Kartla Kapat"); 
        btnKredi.setBackground(new Color(41, 128, 185)); 
        btnKredi.setForeground(Color.WHITE); 
        btnKredi.setFont(new Font("Arial", Font.BOLD, 20));
        
        final String finalTutar = toplamTutar;
        
        btnNakit.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(d, "Toplam " + finalTutar + " TL Nakit olarak tahsil edilecek.\nOnaylıyor musunuz?", "Onay", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + orderId + "|Nakit|" + finalTutar);
                masayiSifirla(masaAdi);
                d.dispose();
            }
        });
        
        btnKredi.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(d, "Toplam " + finalTutar + " TL Kart ile tahsil edilecek.\nOnaylıyor musunuz?", "Onay", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + orderId + "|Kredi Kartı|" + finalTutar);
                masayiSifirla(masaAdi);
                d.dispose();
            }
        });
        
        pnlAlt.add(btnNakit); 
        pnlAlt.add(btnKredi);
        d.add(pnlAlt, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ==========================================
    // AKILLI FİYAT AYIKLAMA MOTORU
    // ==========================================
    public static String fiyatiSok(String htmlFis) {
        if (htmlFis == null || htmlFis.isEmpty()) {
            return "0.00";
        }
        
        try {
            Matcher m1 = Pattern.compile("<!\\-\\-PRICE([0-9.,]+)").matcher(htmlFis);
            if (m1.find()) {
                return m1.group(1).replace(",", ".").trim();
            }

            Matcher m2 = Pattern.compile("Genel Toplam:.*?([0-9]+[.,][0-9]{1,2})").matcher(htmlFis);
            if (m2.find()) {
                return m2.group(1).replace(",", ".").trim();
            }

            String temizMetin = htmlFis.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ");
            Matcher m3 = Pattern.compile("([0-9]+[.,][0-9]{1,2})\\s*(?:TL|₺)", Pattern.CASE_INSENSITIVE).matcher(temizMetin);
            
            String bulunanFiyat = "0.00";
            while (m3.find()) {
                bulunanFiyat = m3.group(1);
            }
            
            return bulunanFiyat.replace(",", ".").trim();
            
        } catch (Exception e) {
            return "0.00";
        }
    }

    // ==========================================
    // VERİ ÇEKME YARDIMCILARI
    // ==========================================
    private String[] aktifSiparisDetayiGetir(String masaAdi) {
        String cvp = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR"); 
        if (cvp != null && cvp.startsWith("KASA_VERI|") && cvp.length() > 10) {
            String[] siparisler = cvp.substring(10).split("\\|\\|\\|");
            for (String s : siparisler) {
                if (s.trim().isEmpty()) continue;
                String[] d = s.split("~_~"); 
                if (d.length >= 5 && d[1].equals(masaAdi)) return d;
            }
        }
        return null;
    }

    public Map<String, String> getMasaDurumlari() { 
        return masaDurumlari; 
    }

    // ==========================================
    // KÖPRÜ FONKSİYONLARI 
    // ==========================================
    private String sunucuyaKomutGonderVeCevapAl(String komut) {
        if (anaPanel instanceof PersonelPaneli) {
            return ((PersonelPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(komut);
        } else if (anaPanel instanceof AdminPaneli) {
            return ((AdminPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(komut);
        }
        return null;
    }

    private void adisyonAc(String tur, String baslik) {
        if (anaPanel instanceof PersonelPaneli) {
            ((PersonelPaneli) anaPanel).adisyonEkraniAc(tur, baslik);
        } else {
            try {
                if(anaPanel instanceof PersonelPaneli == false) {
                     new AdisyonEkrani((PersonelPaneli) anaPanel, this, aktifPersonel, tur, baslik).setVisible(true);
                }
            } catch (Exception ignored) {}
        }
    }

    private void masayiSifirla(String masaAdi) {
        if (anaPanel instanceof PersonelPaneli) {
            ((PersonelPaneli) anaPanel).masayiSifirla(masaAdi);
        } else {
            baslat();
        }
    }
}