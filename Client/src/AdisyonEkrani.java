
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AdisyonEkrani extends JDialog {
    private PersonelPaneli anaPanel;
    private SiparisModulu siparisModulu;
    private String aktifPersonel;
    private String siparisTuru;
    private String baslikIsmi;

    private DefaultTableModel sepetTableModel;
    private double toplamTutar = 0.0;
    private double gecerliOncekiTutar = 0.0;
    private JLabel lblToplamTutar;

    public AdisyonEkrani(PersonelPaneli anaPanel, SiparisModulu siparisModulu, String aktifPersonel, String siparisTuru, String baslikIsmi) {
        super(anaPanel, "Sipariş: " + baslikIsmi, true);
        this.anaPanel = anaPanel;
        this.siparisModulu = siparisModulu;
        this.aktifPersonel = aktifPersonel;
        this.siparisTuru = siparisTuru;
        this.baslikIsmi = baslikIsmi;

        setSize(1200, 750);
        setLayout(new BorderLayout());
        setLocationRelativeTo(anaPanel);

        // ==========================================
        // 1. ÖNCEKİ SİPARİŞLERİ (EK SİPARİŞSE) YÜKLEME
        // ==========================================
        gecerliOncekiTutar = 0.0; 
        String oncekiMusteriIsmi = ""; 
        StringBuilder oncekiSiparisHTML = new StringBuilder();
        
        boolean ekSiparisMi = siparisTuru.equals("MASA") && siparisModulu != null && !siparisModulu.getMasaDurumlari().getOrDefault(baslikIsmi, "BOS").equals("BOS");

        if (ekSiparisMi) {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
                String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
                for (String s : siparisler) {
                    if (s.trim().isEmpty()) continue;
                    String[] d = s.split("~_~"); 
                    if (d.length >= 5 && d[1].equals(baslikIsmi)) {
                        oncekiMusteriIsmi = d[2]; 
                        String html = d[4];
                        oncekiSiparisHTML.append("<div style='border-bottom: 1px dashed #ccc; padding-bottom: 5px; margin-bottom: 5px;'>")
                                         .append(html).append("</div>");
                        
                        // Fiyatı eski fişten çekip önceki tutara ekliyoruz
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<!\\-\\-PRICE([0-9.,]+)").matcher(html);
                        if (m.find()) {
                            gecerliOncekiTutar += guvenliDoubleCevir(m.group(1));
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. ÜST BÖLÜM: MÜŞTERİ BİLGİ FORMU
        // ==========================================
        JPanel pnlMusteriBilgi = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10)); 
        pnlMusteriBilgi.setBackground(new Color(44, 62, 80)); 
        pnlMusteriBilgi.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(39, 174, 96)));
        
        JTextField txtMusteriAd = new JTextField(15); 
        if (!oncekiMusteriIsmi.isEmpty()) txtMusteriAd.setText(oncekiMusteriIsmi); 
        
        JTextField txtMusteriTel = new JTextField(12); 
        JTextField txtMusteriAdres = new JTextField(25); 
        JTextField txtEkAciklama = new JTextField(20);
        
        JLabel lblMusteriAd = new JLabel(siparisTuru.equals("MASA") ? "Müşteri Adı (*):" : "Müşteri Adı (*):"); 
        lblMusteriAd.setForeground(Color.WHITE); lblMusteriAd.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblTelefon = new JLabel("Telefon:"); 
        lblTelefon.setForeground(Color.WHITE); lblTelefon.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblAdres = new JLabel("Adres:"); 
        lblAdres.setForeground(Color.WHITE); lblAdres.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblEkAciklama = new JLabel("Ek Açıklama:"); 
        lblEkAciklama.setForeground(Color.WHITE); lblEkAciklama.setFont(new Font("Arial", Font.BOLD, 14));

        pnlMusteriBilgi.add(lblMusteriAd); pnlMusteriBilgi.add(txtMusteriAd);
        
        if (siparisTuru.equals("EVE_SERVIS")) { 
            pnlMusteriBilgi.add(lblTelefon); pnlMusteriBilgi.add(txtMusteriTel); 
            pnlMusteriBilgi.add(lblAdres); pnlMusteriBilgi.add(txtMusteriAdres); 
            pnlMusteriBilgi.add(lblEkAciklama); pnlMusteriBilgi.add(txtEkAciklama); 
        }
        add(pnlMusteriBilgi, BorderLayout.NORTH);

        // ==========================================
        // 3. ORTA BÖLÜM: ÜRÜNLER VE SEPET (SPLIT PANE)
        // ==========================================
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        split.setDividerLocation(700);

        // SOL: KATEGORİLER VE ÜRÜNLER
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
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KAT_LISTESI_GETIR");
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
                    if(parcalar.length > 1) {
                        yukleUrunleriButonOlarak(parcalar[1].split(";")[0], pnlUrunListesi); 
                    }
                });
            }
        }).start();

        pnlUrunler.add(scrollKategori, BorderLayout.WEST); 
        pnlUrunler.add(new JScrollPane(pnlUrunListesi), BorderLayout.CENTER);

        // SAĞ: SEPET VE GEÇMİŞ SİPARİŞLER
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

        // ==========================================
        // 4. ALT BÖLÜM: TOPLAM VE GÖNDER BUTONU
        // ==========================================
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
            if(musteriAdi.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Lütfen Müşteri ismini giriniz!", "Hata", JOptionPane.ERROR_MESSAGE); 
                return; 
            }
            if(sepetTableModel.getRowCount() == 0) { 
                JOptionPane.showMessageDialog(this, "Sepet boş!"); 
                return; 
            }
            
            StringBuilder fis = new StringBuilder("<html>"); 
            String zaman = new SimpleDateFormat("HH:mm").format(new Date()); 
            String alanKisiHTML = "Siparişi Alan: <b>" + aktifPersonel + "</b><br>";
            
            if(siparisTuru.equals("EVE_SERVIS")) {
                fis.append("<b style='font-size:13px; color:red;'>[").append(zaman).append("] 🛵 EVE SERVİS</b><br>")
                   .append(alanKisiHTML).append("Müşteri: <b>").append(musteriAdi).append("</b> (").append(txtMusteriTel.getText()).append(")<br>")
                   .append("Adres: ").append(txtMusteriAdres.getText()).append("<br>");
                
                if(!txtEkAciklama.getText().trim().isEmpty()) {
                    fis.append("Ek Açıklama: <i>").append(txtEkAciklama.getText().trim()).append("</i><br>");
                }
                fis.append("<hr>");
            } else if (siparisTuru.equals("PAKET")) {
                fis.append("<b style='font-size:13px; color:blue;'>[").append(zaman).append("] 📦 GEL-AL PAKET</b><br>")
                   .append(alanKisiHTML).append("Müşteri: <b>").append(musteriAdi).append("</b><br><hr>");
            } else { 
                String tag = ekSiparisMi ? "➕ EK SİPARİŞ" : "🍽 YENİ MASA"; 
                fis.append("<b style='font-size:13px; color:green;'>[").append(zaman).append("] ").append(tag).append(" (").append(baslikIsmi).append(")</b><br>")
                   .append(alanKisiHTML).append("Müşteri: <b>").append(musteriAdi).append("</b><br><hr>"); 
            }

            StringBuilder stokDusecekListe = new StringBuilder(); 

            for (int i = 0; i < sepetTableModel.getRowCount(); i++) {
                String urun = sepetTableModel.getValueAt(i, 0).toString(); 
                String adet = sepetTableModel.getValueAt(i, 1).toString(); 
                String notlar = sepetTableModel.getValueAt(i, 3).toString(); 
                
                stokDusecekListe.append(urun).append(":").append(adet).append(",");
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
            
            // ==========================================
            // YENİ EKLENEN KISIM: FİYATI FİŞE YAZDIRMAK
            // ==========================================
            double genelToplam = gecerliOncekiTutar + toplamTutar;
            String formatliToplam = String.format(java.util.Locale.US, "%.2f", genelToplam);
            
            // 1. Kasiyer ve Müşteri için Görsel Tutar (Fişin en altına eklenir)
            fis.append("<hr><div style='text-align: right; font-size: 15px;'>");
            fis.append("<b>Genel Toplam: <span style='color:red;'>").append(formatliToplam).append(" TL</span></b>");
            fis.append("</div>");
            
            // 2. Kasa modülünün arkada okuyabilmesi için Gizli Tutar Etiketi
            fis.append("");
            
            fis.append("</html>");
            // ==========================================
            
            String gonderilecekStokDatasi = stokDusecekListe.length() > 0 ? stokDusecekListe.substring(0, stokDusecekListe.length() - 1) : "null";
            anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_OLUSTUR|" + baslikIsmi + "|" + musteriAdi + "|" + fis.toString() + "|" + gonderilecekStokDatasi);
            
            JOptionPane.showMessageDialog(this, "Sipariş mutfağa başarıyla iletildi!"); 
            dispose();
            
            if (siparisModulu != null) {
                siparisModulu.baslat(); 
            }
        });

        pnlAlt.add(lblToplamTutar, BorderLayout.WEST); 
        pnlAlt.add(btnMutfagaGonder, BorderLayout.EAST); 
        pnlSepet.add(pnlAlt, BorderLayout.SOUTH);
        
        split.setLeftComponent(pnlUrunler); 
        split.setRightComponent(pnlSepet); 
        add(split, BorderLayout.CENTER);
    }

    private double guvenliDoubleCevir(String veri) {
        try { 
            return (veri == null || veri.trim().isEmpty()) ? 0.0 : Double.parseDouble(veri.replace(",", ".").trim()); 
        } catch (Exception e) { 
            return 0.0; 
        }
    }

    private void yukleUrunleriButonOlarak(String kategoriAdi, JPanel pnlListe) {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + kategoriAdi);
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
        d.setSize(450, 600); 
        d.setLayout(new BorderLayout()); 
        d.setLocationRelativeTo(this);
        
        JPanel pnlAna = new JPanel(); 
        pnlAna.setLayout(new BoxLayout(pnlAna, BoxLayout.Y_AXIS)); 
        pnlAna.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JPanel pnlAdet = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        pnlAdet.setBackground(Color.WHITE); 
        pnlAdet.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        pnlAdet.add(new JLabel("<html><font size='4'><b>Kaç Adet: </b></font></html>"));
        
        JSpinner spnAdet = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)); 
        spnAdet.setFont(new Font("Arial", Font.BOLD, 18));
        pnlAdet.add(spnAdet); 
        pnlAna.add(pnlAdet); 
        pnlAna.add(Box.createVerticalStrut(10));

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
        
        if (pnlCikarilabilir.getComponentCount() > 0) pnlAna.add(pnlCikarilabilir); 
        pnlAna.add(Box.createVerticalStrut(10));
        
        if (pnlEkstralar.getComponentCount() > 0) pnlAna.add(pnlEkstralar);
        pnlAna.add(Box.createVerticalStrut(15)); 
        pnlAna.add(new JLabel("<html><b>Aşçıya Özel Not:</b></html>")); 
        
        JTextField txtNot = new JTextField(); 
        txtNot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30)); 
        pnlAna.add(txtNot);
        
        JButton btnSepeteEkle = new JButton("Sepete Ekle"); 
        btnSepeteEkle.setBackground(new Color(39, 174, 96)); 
        btnSepeteEkle.setForeground(Color.WHITE); 
        btnSepeteEkle.setFont(new Font("Arial", Font.BOLD, 15));
        
        btnSepeteEkle.addActionListener(e -> {
            int secilenAdet = (int) spnAdet.getValue();
            double birimFiyat = tabanFiyat; 
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
                        birimFiyat += guvenliDoubleCevir(mDetay[2]); 
                    } 
                } 
            }
            if(!txtNot.getText().trim().isEmpty()) {
                notListesi.add("Not: " + txtNot.getText().trim());
            }
            
            String sonNot = String.join(" | ", notListesi); 
            if(sonNot.isEmpty()) sonNot = "Standart";
            
            double sonFiyat = birimFiyat * secilenAdet;
            sepetTableModel.addRow(new Object[]{urunAd, secilenAdet, sonFiyat, sonNot}); 
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
}