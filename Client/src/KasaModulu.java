import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KasaModulu extends JPanel {
    private JFrame anaPanel;
    private DefaultTableModel aktifTableModel, gecmisTableModel;
    private JTable aktifTablo, gecmisTablo;
    private JEditorPane txtFisDetay; 
    private JPanel pnlIslemButonlari;
    private JButton btnFisYazdir;
    
    private JLabel lblOdenecekTutar;
    private String guncelSiparisTutari = "0.00";
    
    private JButton btnYolaCikti, btnIptal, btnNakit, btnKrediKarti;
    private String seciliSiparisId = "";
    private String seciliTur = "";

    private String ayarMagazaAdi = "YÜKLENİYOR...";
    private String ayarOnBilgi = "";
    private String ayarAltBilgi = "";
    private String ayarVKN = "";

    public KasaModulu(JFrame anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10)); 
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        ayarlariGetir();

        // --- ÜST PANEL ---
        JPanel pnlKasaUst = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("💳 Kasa, Tahsilat ve İşlem Yönetimi");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        
        JPanel pnlUstSag = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnKuryeHesap = new JButton("🛵 Kurye Hesap Kesimi");
        btnKuryeHesap.setBackground(new Color(142, 68, 173)); 
        btnKuryeHesap.setForeground(Color.WHITE);
        btnKuryeHesap.setPreferredSize(new Dimension(200, 45));
        btnKuryeHesap.addActionListener(e -> kuryeHesapEkraniAc());

        JButton btnGunSonu = new JButton("📊 GÜN SONU (Z RAPORU)");
        btnGunSonu.setBackground(new Color(52, 73, 94)); 
        btnGunSonu.setForeground(Color.WHITE);
        btnGunSonu.setPreferredSize(new Dimension(220, 45));
        btnGunSonu.addActionListener(e -> gunSonuIslemi());
        
        pnlUstSag.add(btnKuryeHesap);
        pnlUstSag.add(btnGunSonu);
        pnlKasaUst.add(lblBaslik, BorderLayout.WEST);
        pnlKasaUst.add(pnlUstSag, BorderLayout.EAST);
        add(pnlKasaUst, BorderLayout.NORTH);

        // --- TABLOLAR ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        splitPane.setDividerLocation(650);

        JTabbedPane tabbedPane = new JTabbedPane(); 
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        aktifTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Mevcut Durum", "Süre", "HTML_GIZLI", "RAWZAMAN_GIZLI"}, 0);
        aktifTablo = new JTable(aktifTableModel); 
        aktifTablo.setRowHeight(40); 
        
        aktifTablo.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String durum = table.getValueAt(row, 3).toString();
                if (!isSelected) {
                    if (durum.equals("HAZIR")) c.setBackground(new Color(255, 243, 176));
                    else if (durum.contains("YOLA_CIKTI")) c.setBackground(new Color(255, 204, 204));
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        aktifTablo.getColumnModel().getColumn(5).setMinWidth(0); aktifTablo.getColumnModel().getColumn(5).setMaxWidth(0);
        aktifTablo.getColumnModel().getColumn(6).setMinWidth(0); aktifTablo.getColumnModel().getColumn(6).setMaxWidth(0);
        
        tabbedPane.addTab("🟢 Aktif / Bekleyen İşlemler", new JScrollPane(aktifTablo));

        gecmisTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Son Durum", "Zaman", "HTML_GIZLI"}, 0);
        gecmisTablo = new JTable(gecmisTableModel); 
        gecmisTablo.setRowHeight(35); 
        gecmisTablo.getColumnModel().getColumn(5).setMinWidth(0); gecmisTablo.getColumnModel().getColumn(5).setMaxWidth(0);
        
        tabbedPane.addTab("📜 Geçmiş İşlemler", new JScrollPane(gecmisTablo));
        splitPane.setLeftComponent(tabbedPane);

        // --- SAĞ PANEL (FİŞ VE KONTROL) ---
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10)); 
        txtFisDetay = new JEditorPane("text/html", ""); 
        txtFisDetay.setEditable(false); 
        txtFisDetay.setBackground(new Color(253, 254, 254));
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        JPanel pnlAltGrup = new JPanel(new BorderLayout(5, 5));
        lblOdenecekTutar = new JLabel("Bir Sipariş Seçin", SwingConstants.CENTER);
        lblOdenecekTutar.setFont(new Font("Arial", Font.BOLD, 22));
        pnlAltGrup.add(lblOdenecekTutar, BorderLayout.NORTH);

        pnlIslemButonlari = new JPanel(new GridLayout(1, 3, 10, 10)); 
        btnYolaCikti = new JButton("🛵 Kurye Seç & Yola Çıkar"); 
        btnYolaCikti.setBackground(new Color(52, 152, 219)); btnYolaCikti.setForeground(Color.WHITE);
        btnIptal = new JButton("❌ İptal Et"); btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE);
        btnNakit = new JButton("💵 Nakit Al"); btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE);
        btnKrediKarti = new JButton("💳 K.Kartı Al"); btnKrediKarti.setBackground(new Color(41, 128, 185)); btnKrediKarti.setForeground(Color.WHITE);
        
        pnlAltGrup.add(pnlIslemButonlari, BorderLayout.CENTER);

        btnFisYazdir = new JButton("🖨️ Fişi Yazdır");
        btnFisYazdir.setFont(new Font("Arial", Font.BOLD, 14));
        btnFisYazdir.setEnabled(false);
        btnFisYazdir.addActionListener(e -> { try { txtFisDetay.print(); } catch (Exception ex) {} });
        pnlAltGrup.add(btnFisYazdir, BorderLayout.SOUTH);

        pnlSag.add(pnlAltGrup, BorderLayout.SOUTH); 
        splitPane.setRightComponent(pnlSag); 
        add(splitPane, BorderLayout.CENTER);

        // --- AKSIYONLAR ---
        aktifTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && aktifTablo.getSelectedRow() != -1) {
                gecmisTablo.clearSelection();
                int row = aktifTablo.getSelectedRow();
                seciliSiparisId = aktifTableModel.getValueAt(row, 0).toString();
                seciliTur = aktifTableModel.getValueAt(row, 1).toString();
                String durum = aktifTableModel.getValueAt(row, 3).toString();
                String html = aktifTableModel.getValueAt(row, 5).toString();
                fisGoruntule(html);
                guncelSiparisTutari = fiyatiSok(html);
                lblOdenecekTutar.setText("Ödenecek: " + guncelSiparisTutari + " TL");
                butonlariDuzenle(seciliTur, true, durum);
            }
        });

        gecmisTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && gecmisTablo.getSelectedRow() != -1) {
                aktifTablo.clearSelection();
                int row = gecmisTablo.getSelectedRow();
                String html = gecmisTableModel.getValueAt(row, 5).toString();
                fisGoruntule(html);
                lblOdenecekTutar.setText("Tahsil Edildi: " + fiyatiSok(html) + " TL");
                butonlariDuzenle("", false, "");
            }
        });

        btnYolaCikti.addActionListener(e -> {
            if (seciliSiparisId.isEmpty()) return;

            // Sunucudan aktif kuryeleri iste
            String kuryelerCvp = sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            
            if (kuryelerCvp != null && kuryelerCvp.startsWith("KURYE_LISTESI|")) {
                String[] parcalar = kuryelerCvp.split("\\|");
                if (parcalar.length <= 1) {
                    JOptionPane.showMessageDialog(this, "Sistemde aktif kurye bulunamadı!");
                    return;
                }

                // Listeyi diziye çevir
                String[] kuryeListesi = new String[parcalar.length - 1];
                System.arraycopy(parcalar, 1, kuryeListesi, 0, parcalar.length - 1);

                // Kurye Seçim Diyaloğu
                String secilen = (String) JOptionPane.showInputDialog(this, 
                        "Siparişi Teslim Edecek Kuryeyi Seçin:", "Kurye Ata",
                        JOptionPane.QUESTION_MESSAGE, null, kuryeListesi, kuryeListesi[0]);

                if (secilen != null) {
                    // Sunucuya atama komutunu gönder
                    sunucuyaKomutGonderVeCevapAl("KURYE_ATA|" + seciliSiparisId + "|" + secilen);
                    verileriYenile(); // Tabloyu güncelle (Sarıdan Kırmızıya dönecek)
                }
            }
        });

        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKrediKarti.addActionListener(e -> odemeAl("Kredi Kartı"));
        btnIptal.addActionListener(e -> {
             if(JOptionPane.showConfirmDialog(this, "İptal edilsin mi?", "Onay", 0)==0) {
                 sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + seciliSiparisId + "|IPTAL");
                 verileriYenile();
             }
        });

        Timer timer = new Timer(1000, e -> zamanlariGuncelle());
        timer.start();
        
        new Timer(10000, e -> verileriYenile()).start();
        verileriYenile();
    }

    private void zamanlariGuncelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date suan = new Date();
        for (int i = 0; i < aktifTableModel.getRowCount(); i++) {
            try {
                Date d = sdf.parse(aktifTableModel.getValueAt(i, 6).toString());
                long diff = Math.max(0, suan.getTime() - d.getTime());
                long s = (diff / 1000) % 60; long m = (diff / (1000 * 60)) % 60;
                aktifTableModel.setValueAt(String.format("%02d:%02d", m, s), i, 4);
            } catch (Exception e) {}
        }
    }

    public void verileriYenile() {
        // AKTİF ÇEK
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                aktifTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("KASA_VERI|")) {
                    String[] items = cvp.substring(10).split("\\|\\|\\|");
                    for (String s : items) {
                        String[] d = s.split("~_~"); 
                        if (d.length >= 6) aktifTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], "00:00", d[4], d[5]});
                    }
                }
            });
        }).start();

        // GEÇMİŞ ÇEK (DÜZELTİLDİ)
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("KASA_GECMIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                gecmisTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("KASA_GECMIS_VERI|")) {
                    String[] items = cvp.substring(17).split("\\|\\|\\|");
                    for (String s : items) {
                        String[] d = s.split("~_~"); 
                        if (d.length >= 5) gecmisTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], "Tamamlandı", d[4]});
                    }
                }
            });
        }).start();
    }

    private void fisGoruntule(String htmlFis) {
        String temiz = htmlFis.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "");
        String adisyon = "<html><body style='padding:10px; font-family:monospace;'>" +
                         "<div style='width:300px; border:1px solid #ccc; background:white; padding:10px;'>" +
                         "<center><b>" + ayarMagazaAdi + "</b><br>" + ayarOnBilgi + "<hr></center>" +
                         temiz + "<hr><center>" + ayarAltBilgi + "<br>" + ayarVKN + "</center></div></body></html>";
        txtFisDetay.setText(adisyon);
        btnFisYazdir.setEnabled(true);
    }

    private void odemeAl(String tur) {
        if (JOptionPane.showConfirmDialog(this, guncelSiparisTutari + " TL tahsil edilsin mi?", "Ödeme", 0) == 0) {
            sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + tur + "|" + guncelSiparisTutari);
            verileriYenile();
        }
    }

    private void butonlariDuzenle(String tur, boolean aktifMi, String mevcutDurum) {
        pnlIslemButonlari.removeAll();
        
        if (aktifMi) {
            btnFisYazdir.setEnabled(true);
            
            // DURUM: Kurye Bekleyen Eve Servis (Sarı Satır)
            // Hem eski "Telefon Siparişi" hem yeni "Eve Servis" isimlendirmesini destekler
            if (mevcutDurum.equals("HAZIR") && 
               (tur.contains("Eve") || tur.contains("EVE") || tur.contains("Telefon"))) {
                
                btnYolaCikti.setText("🛵 Kurye Seç & Yola Çıkar");
                pnlIslemButonlari.add(btnYolaCikti);
                pnlIslemButonlari.add(btnIptal);
            } 
            // DURUM: Yolda Olan Paket (Kırmızı Satır)
            else if (mevcutDurum.contains("YOLA_CIKTI")) {
                btnNakit.setText("💵 Nakit (Teslimat)");
                btnKrediKarti.setText("💳 K.Kartı (Teslimat)");
                pnlIslemButonlari.add(btnNakit);
                pnlIslemButonlari.add(btnKrediKarti);
            }
            // DURUM: Diğer (Masa veya Gel-Al)
            else {
                pnlIslemButonlari.add(btnNakit);
                pnlIslemButonlari.add(btnKrediKarti);
                pnlIslemButonlari.add(btnIptal);
            }
        }
        pnlIslemButonlari.revalidate();
        pnlIslemButonlari.repaint();
    }

    private void ayarlariGetir() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
            if (cvp != null && cvp.startsWith("AYARLAR|")) {
                String[] items = cvp.substring(8).split("\\|\\|\\|");
                for (String a : items) {
                    String[] kv = a.split("~_~");
                    if (kv.length == 2) {
                        if(kv[0].equals("MagazaAdi")) ayarMagazaAdi = kv[1];
                        if(kv[0].equals("MagazaOnBilgi")) ayarOnBilgi = kv[1];
                        if(kv[0].equals("MagazaAltBilgi")) ayarAltBilgi = kv[1];
                        if(kv[0].equals("MagazaVKN")) ayarVKN = kv[1];
                    }
                }
            }
        }).start();
    }

    private String fiyatiSok(String html) {
        try {
            Matcher m = Pattern.compile("Genel Toplam:.*?([0-9]+[.,][0-9]{2})").matcher(html);
            return m.find() ? m.group(1).replace(",", ".") : "0.00";
        } catch (Exception e) { return "0.00"; }
    }

    private String sunucuyaKomutGonderVeCevapAl(String k) {
        if (anaPanel instanceof PersonelPaneli) return ((PersonelPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(k);
        return null;
    }
    // ==========================================
    // GÜN SONU / Z RAPORU İŞLEMİ
    // ==========================================
    private void gunSonuIslemi() {
        int onay = JOptionPane.showConfirmDialog(this, 
            "Gün sonu raporu kesilecek ve sistem yeni güne sıfırlanacaktır.\nBu işlem geri alınamaz. Onaylıyor musunuz?", 
            "Z Raporu Onayı", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (onay == JOptionPane.YES_OPTION) {
            String rapor = sunucuyaKomutGonderVeCevapAl("GUN_SONU_KAPAT");
            if (rapor != null && rapor.startsWith("RAPOR|")) {
                String[] r = rapor.split("\\|"); 
                String mesaj = "<html><body style='width:300px; font-family:Arial; padding:10px;'>" +
                               "<h2 style='color:#2c3e50; border-bottom:1px solid #ccc;'>🏁 GÜNLÜK Z RAPORU</h2>" +
                               "<b>Toplam Brüt Satış:</b> " + r[1] + " TL<br><br>" +
                               "<b>Tahsil Edilen Nakit:</b> <font color='green'>" + r[2] + " TL</font><br>" +
                               "<b>Tahsil Edilen Kart (POS):</b> <font color='blue'>" + r[3] + " TL</font><br><hr>" +
                               "<b>Mutfak/Gider Yükü:</b> <font color='red'>-" + r[4] + " TL</font><br>" +
                               "<b>Net Kâr:</b> <font color='#27ae60'><b>" + r[5] + " TL</b></font><br><br>" +
                               "<i>Sistem yeni iş gününe hazır.</i></body></html>";
                               
                JOptionPane.showMessageDialog(this, mesaj, "Gün Sonu Özeti", JOptionPane.INFORMATION_MESSAGE);
                verileriYenile();
            } else {
                JOptionPane.showMessageDialog(this, "Gün sonu alınırken hata oluştu!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==========================================
    // KURYE HESAP KESİM EKRANI
    // ==========================================
    private void kuryeHesapEkraniAc() {
        String kuryelerCvp = sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
        if(kuryelerCvp != null && kuryelerCvp.startsWith("KURYE_LISTESI|")) {
            String[] split = kuryelerCvp.split("\\|");
            if (split.length <= 1) { 
                JOptionPane.showMessageDialog(this, "Sistemde aktif kurye bulunamadı."); 
                return; 
            }
            
            String[] kuryeListesi = new String[split.length - 1];
            System.arraycopy(split, 1, kuryeListesi, 0, split.length - 1);
            
            String secilenKurye = (String) JOptionPane.showInputDialog(this, 
                                  "Günlük hesabı görüntülenecek kuryeyi seçin:", 
                                  "Kurye Tahsilat Takibi", JOptionPane.QUESTION_MESSAGE, null, kuryeListesi, kuryeListesi[0]);
                                  
            if (secilenKurye != null) {
                String hesapCvp = sunucuyaKomutGonderVeCevapAl("KURYE_HESAP_GETIR|" + secilenKurye);
                if (hesapCvp != null && hesapCvp.startsWith("KURYE_HESAP|")) {
                    String[] h = hesapCvp.split("\\|");
                    // h[1]: Sipariş Adedi, h[2]: Nakit Toplam, h[3]: Kart Toplam, h[4]: Detaylı Liste
                    String msg = "<html><div style='width:320px; font-family:Arial;'>" +
                                 "<h2 style='color:#8e44ad;'>🛵 " + secilenKurye + " Raporu</h2>" +
                                 "<b>Toplam Teslimat:</b> " + h[1] + " Adet<br><hr>" +
                                 "<font size='5'><b>Nakit:</b> <font color='green'>" + h[2] + " TL</font></font><br>" +
                                 "<font size='5'><b>K.Kartı:</b> <font color='blue'>" + h[3] + " TL</font></font><hr>" +
                                 "<h3 style='color:#333;'>Teslimat Geçmişi:</h3>" +
                                 "<div style='font-size:11px; color:#555;'>" + h[4].replace("|||", "<br>") + "</div>" +
                                 "</div></html>";
                    JOptionPane.showMessageDialog(this, msg, "Kurye Hesabı", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
}