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

        // ==========================================
        // ÜST BÖLÜM
        // ==========================================
        JPanel pnlKasaUst = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("💳 Kasa, Tahsilat ve İşlem Yönetimi");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        
        JPanel pnlUstSag = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnKuryeHesap = new JButton("🛵 Kurye Hesap Kesimi");
        btnKuryeHesap.setBackground(new Color(142, 68, 173)); 
        btnKuryeHesap.setForeground(Color.WHITE);
        btnKuryeHesap.setFont(new Font("Arial", Font.BOLD, 14));
        btnKuryeHesap.setPreferredSize(new Dimension(200, 45));
        btnKuryeHesap.addActionListener(e -> kuryeHesapEkraniAc());

        JButton btnGunSonu = new JButton("📊 GÜN SONU (Z RAPORU)");
        btnGunSonu.setBackground(new Color(52, 73, 94)); 
        btnGunSonu.setForeground(Color.WHITE);
        btnGunSonu.setFont(new Font("Arial", Font.BOLD, 14));
        btnGunSonu.setPreferredSize(new Dimension(220, 45));
        btnGunSonu.addActionListener(e -> gunSonuIslemi());
        
        pnlUstSag.add(btnKuryeHesap);
        pnlUstSag.add(btnGunSonu);
        pnlKasaUst.add(lblBaslik, BorderLayout.WEST);
        pnlKasaUst.add(pnlUstSag, BorderLayout.EAST);
        add(pnlKasaUst, BorderLayout.NORTH);

        // ==========================================
        // TABLOLAR VE RENKLENDİRME
        // ==========================================
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        splitPane.setDividerLocation(650);

        JTabbedPane tabbedPane = new JTabbedPane(); 
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        aktifTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Mevcut Durum", "Süre", "HTML_GIZLI", "RAWZAMAN_GIZLI"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        aktifTablo = new JTable(aktifTableModel); 
        aktifTablo.setRowHeight(40); 
        aktifTablo.setFont(new Font("Arial", Font.PLAIN, 14)); 
        aktifTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // RENKLENDİRME RENDERER
        aktifTablo.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String durum = table.getValueAt(row, 3).toString();
                if (!isSelected) {
                    if (durum.equals("HAZIR")) c.setBackground(new Color(255, 243, 176)); // SARI
                    else if (durum.contains("YOLA_CIKTI")) c.setBackground(new Color(255, 204, 204)); // KIRMIZI
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        aktifTablo.getColumnModel().getColumn(5).setMinWidth(0); aktifTablo.getColumnModel().getColumn(5).setMaxWidth(0);
        aktifTablo.getColumnModel().getColumn(6).setMinWidth(0); aktifTablo.getColumnModel().getColumn(6).setMaxWidth(0);
        
        tabbedPane.addTab("🟢 Aktif / Bekleyen İşlemler", new JScrollPane(aktifTablo));

        gecmisTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Son Durum", "Süre", "HTML_GIZLI"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        gecmisTablo = new JTable(gecmisTableModel); 
        gecmisTablo.setRowHeight(35); 
        gecmisTablo.getColumnModel().getColumn(5).setMinWidth(0); gecmisTablo.getColumnModel().getColumn(5).setMaxWidth(0);
        
        tabbedPane.addTab("📜 Geçmiş İşlemler", new JScrollPane(gecmisTablo));
        splitPane.setLeftComponent(tabbedPane);

        // ==========================================
        // SAĞ TARAF: FİŞ VE BUTONLAR
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10)); 
        txtFisDetay = new JEditorPane("text/html", ""); 
        txtFisDetay.setEditable(false); 
        txtFisDetay.setBackground(new Color(236, 240, 241)); 
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        JPanel pnlAltGrup = new JPanel(new BorderLayout(5, 5));
        lblOdenecekTutar = new JLabel("Bir Sipariş Seçin", SwingConstants.CENTER);
        lblOdenecekTutar.setFont(new Font("Arial", Font.BOLD, 22));
        pnlAltGrup.add(lblOdenecekTutar, BorderLayout.NORTH);

        pnlIslemButonlari = new JPanel(new GridLayout(1, 3, 10, 10)); 
        btnYolaCikti = new JButton("🛵 Kuryeye Ver"); btnYolaCikti.setBackground(new Color(52, 152, 219)); btnYolaCikti.setForeground(Color.WHITE);
        btnIptal = new JButton("❌ İptal"); btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE);
        btnNakit = new JButton("💵 Nakit"); btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE);
        btnKrediKarti = new JButton("💳 K.Kartı"); btnKrediKarti.setBackground(new Color(41, 128, 185)); btnKrediKarti.setForeground(Color.WHITE);
        pnlAltGrup.add(pnlIslemButonlari, BorderLayout.CENTER);

        btnFisYazdir = new JButton("🖨️ Fişi Yazdır");
        btnFisYazdir.setBackground(new Color(44, 62, 80)); btnFisYazdir.setForeground(Color.WHITE);
        btnFisYazdir.setEnabled(false);
        btnFisYazdir.addActionListener(e -> { try { txtFisDetay.print(); } catch (PrinterException ex) {} });
        pnlAltGrup.add(btnFisYazdir, BorderLayout.SOUTH);

        pnlSag.add(pnlAltGrup, BorderLayout.SOUTH); 
        splitPane.setRightComponent(pnlSag); 
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // DİNLEYİCİLER VE TIMER
        // ==========================================
        aktifTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && aktifTablo.getSelectedRow() != -1) {
                int row = aktifTablo.getSelectedRow();
                seciliSiparisId = aktifTableModel.getValueAt(row, 0).toString();
                seciliTur = aktifTableModel.getValueAt(row, 1).toString();
                fisGoruntule(aktifTableModel.getValueAt(row, 5).toString());
                guncelSiparisTutari = fiyatiSok(aktifTableModel.getValueAt(row, 5).toString());
                lblOdenecekTutar.setText("Ödenecek: " + guncelSiparisTutari + " TL");
                butonlariDuzenle(seciliTur, true, aktifTableModel.getValueAt(row, 3).toString());
            }
        });

        Timer timer = new Timer(1000, e -> zamanlariGuncelle());
        timer.start();
        
        Timer otoYenile = new Timer(10000, e -> verileriYenile());
        otoYenile.start();

        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKrediKarti.addActionListener(e -> odemeAl("Kredi Kartı"));
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

    private void fisGoruntule(String htmlFis) {
        String temizHtml = htmlFis.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "");
        String adisyonHtml = "<html><body style='padding:10px; font-family:monospace;'>" +
                             "<div style='width:300px; border:1px solid #ccc; background:white; padding:10px;'>" +
                             "<center><b>" + ayarMagazaAdi + "</b><br>" + ayarOnBilgi + "<hr></center>" +
                             temizHtml + "<hr><center>" + ayarAltBilgi + "<br>" + ayarVKN + "</center></div></body></html>";
        txtFisDetay.setText(adisyonHtml);
        btnFisYazdir.setEnabled(true);
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                aktifTableModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("KASA_VERI|")) {
                    String[] siparisler = cvp.substring(10).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        String[] d = s.split("~_~"); 
                        if (d.length >= 6) aktifTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], "00:00", d[4], d[5]});
                    }
                }
            });
        }).start();
    }

    private void odemeAl(String tur) {
        if (JOptionPane.showConfirmDialog(this, guncelSiparisTutari + " TL tahsil edilsin mi?", "Ödeme", 0) == 0) {
            sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + tur + "|" + guncelSiparisTutari);
            verileriYenile();
        }
    }

    private void ayarlariGetir() {
        new Thread(() -> {
            String cvp = sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
            if (cvp != null && cvp.startsWith("AYARLAR|")) {
                String[] ayarlar = cvp.substring(8).split("\\|\\|\\|");
                for (String a : ayarlar) {
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

    private void butonlariDuzenle(String tur, boolean aktifMi, String mevcutDurum) {
        pnlIslemButonlari.removeAll();
        
        if (aktifMi || !seciliSiparisId.isEmpty()) {
            btnFisYazdir.setEnabled(true); 
        } else {
            btnFisYazdir.setEnabled(false);
        }

        if (aktifMi) {
            // DURUM 1: KURYEDE / YOLDA (Kırmızı Satır) -> Tahsilat Butonları Çıkar
            if (mevcutDurum.contains("YOLA_CIKTI")) {
                btnNakit.setText("💵 Nakit (Teslim Edildi)");
                btnKrediKarti.setText("💳 K.Kartı (Teslim Edildi)");
                pnlIslemButonlari.add(btnNakit);
                pnlIslemButonlari.add(btnKrediKarti);
                pnlIslemButonlari.add(btnIptal);
            } 
            // DURUM 2: MUTFAKTAN ÇIKTI, ATAMA BEKLİYOR (Sarı Satır) -> Kurye Seçme Butonu
            else if (tur.contains("Telefon") || tur.contains("Eve") || tur.contains("EVE")) {
                btnYolaCikti.setText("🛵 Kurye Seç & Yola Çıkar");
                pnlIslemButonlari.add(btnYolaCikti);
                pnlIslemButonlari.add(btnIptal);
            } 
            // DURUM 3: PAKET / GEL-AL SİPARİŞ
            else if (tur.contains("Paket") || tur.contains("PAKET") || tur.contains("Müşteri")) {
                btnNakit.setText("📦 Teslim Et & Nakit Al");
                btnKrediKarti.setText("📦 Teslim Et & Kart Al");
                pnlIslemButonlari.add(btnNakit);
                pnlIslemButonlari.add(btnKrediKarti);
                pnlIslemButonlari.add(btnIptal);
            } 
            // DURUM 4: NORMAL MASA
            else {
                btnNakit.setText("💵 Nakit Al & Kapat");
                btnKrediKarti.setText("💳 K.Kartı & Kapat");
                pnlIslemButonlari.add(btnNakit);
                pnlIslemButonlari.add(btnKrediKarti);
                pnlIslemButonlari.add(btnIptal);
            }
        }
        pnlIslemButonlari.revalidate(); 
        pnlIslemButonlari.repaint();
    }

    private String sunucuyaKomutGonderVeCevapAl(String k) {
        if (anaPanel instanceof PersonelPaneli) return ((PersonelPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(k);
        return null;
    }

    // ==========================================
    // GÜN SONU / Z RAPORU MANTIĞI
    // ==========================================
    private void gunSonuIslemi() {
        int onay = JOptionPane.showConfirmDialog(this, 
            "Gün sonu raporu kesilecek, vardiyalar kapatılacak ve sistem yeni güne sıfırlanacaktır.\nOnaylıyor musunuz?", 
            "Z Raporu Onayı", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (onay == JOptionPane.YES_OPTION) {
            String rapor = sunucuyaKomutGonderVeCevapAl("GUN_SONU_KAPAT");
            if (rapor != null && rapor.startsWith("RAPOR|")) {
                String[] r = rapor.split("\\|"); 
                String mesaj = "<html><body style='width:350px; font-family:Arial; padding:10px;'>" +
                               "<h2 style='color:#2c3e50; border-bottom:1px solid #ccc;'>🏁 GÜNLÜK KAPANIŞ ÖZETİ (Z)</h2>" +
                               "<font size='4'><b>Brüt Satış:</b> " + fiyatiSok(r[1]) + " TL</font><br><br>" +
                               "<b>Tahsil Edilen Nakit:</b> <font color='green'>" + fiyatiSok(r[2]) + " TL</font><br>" +
                               "<b>Tahsil Edilen Kart (POS):</b> <font color='blue'>" + fiyatiSok(r[3]) + " TL</font><br><hr>" +
                               "<b>Personel / Vardiya Yükü:</b> <font color='red'>-" + fiyatiSok(r[4]) + " TL</font><br>" +
                               "<b>Tahmini Net Kâr:</b> <font color='#27ae60'><b>" + fiyatiSok(r[5]) + " TL</b></font><br><br>" +
                               "<i style='color:#7f8c8d;'>Yeni iş gününe geçiş yapıldı. Muhasebe kayıtları oluşturuldu.</i></body></html>";
                               
                JOptionPane.showMessageDialog(this, mesaj, "Gün Sonu Raporu", JOptionPane.INFORMATION_MESSAGE);
                verileriYenile();
            } else {
                JOptionPane.showMessageDialog(this, "Gün sonu alınırken sistem yanıt vermedi veya hata oluştu.\nDetay: " + rapor, "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void kuryeHesapEkraniAc() {
        String kuryelerCvp = sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
        if(kuryelerCvp != null && kuryelerCvp.startsWith("KURYE_LISTESI|")) {
            String[] split = kuryelerCvp.split("\\|");
            if (split.length <= 1) { JOptionPane.showMessageDialog(this, "Sistemde kurye yok."); return; }
            
            String[] kuryeListesi = new String[split.length - 1];
            System.arraycopy(split, 1, kuryeListesi, 0, split.length - 1);
            
            String secilenKurye = (String) JOptionPane.showInputDialog(this, "Günlük hesabı görüntülenecek kuryeyi seçin:", 
                                  "Kurye Hesap Kesimi", JOptionPane.QUESTION_MESSAGE, null, kuryeListesi, kuryeListesi[0]);
                                  
            if (secilenKurye != null) {
                String hesapCvp = sunucuyaKomutGonderVeCevapAl("KURYE_HESAP_GETIR|" + secilenKurye);
                if (hesapCvp != null && hesapCvp.startsWith("KURYE_HESAP|")) {
                    String[] h = hesapCvp.split("\\|");
                    String msg = "<html><div style='width:350px; font-family:Arial;'>" +
                                 "<h2 style='color:#8e44ad; border-bottom: 2px solid #ccc;'>🛵 " + secilenKurye + " - Bugünün Özeti</h2>" +
                                 "<font size='4'><b>Teslim Edilen Sipariş:</b> " + h[1] + " Adet</font><br><br>" +
                                 "<font size='5'><b>Toplanan Nakit:</b> <font color='green'>" + h[2] + " TL</font></font><br>" +
                                 "<font size='5'><b>Kredi Kartı (Pos):</b> <font color='blue'>" + h[3] + " TL</font></font><hr>" +
                                 "<h3 style='color:#333;'>Son Teslimat Geçmişi:</h3>" +
                                 "<div style='font-size:12px; color:#555;'>" + h[4] + "</div>" +
                                 "</div></html>";
                    JOptionPane.showMessageDialog(this, msg, "Kurye Hesabı", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Kurye listesi alınamadı!");
        }
    }
}