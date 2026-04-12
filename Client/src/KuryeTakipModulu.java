import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KuryeTakipModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultListModel<String> kuryeListModel;
    private JList<String> kuryeListesi;
    private JPanel pnlSiparisler;
    
    private JButton btnKuryeAksiyon; 
    private String mevcutKuryeDurumu = "BEKLIYOR";
    private String seciliGercekKuryeAdi = ""; 
    private String ayarMagazaAdi = "YÜKLENİYOR...";
    private String ayarOnBilgi = "";
    private String ayarAltBilgi = "";
    private String ayarVKN = "";
    public KuryeTakipModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(320);

        JPanel pnlSol = new JPanel(new BorderLayout(5, 5));
        pnlSol.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Aktif Kuryeler", 0, 0, new Font("Arial", Font.BOLD, 14)));

        kuryeListModel = new DefaultListModel<>();
        kuryeListesi = new JList<>(kuryeListModel);
        kuryeListesi.setFont(new Font("Arial", Font.BOLD, 16));
        kuryeListesi.setFixedCellHeight(50);
        kuryeListesi.setSelectionBackground(new Color(52, 152, 219));
        kuryeListesi.setSelectionForeground(Color.WHITE);
        
        pnlSol.add(new JScrollPane(kuryeListesi), BorderLayout.CENTER);

        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.addActionListener(e -> kuryeleriGetir());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        JPanel pnlSagAna = new JPanel(new BorderLayout(5, 5));

        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(new Color(245, 246, 250));

        JPanel siparisWrapper = new JPanel(new BorderLayout());
        siparisWrapper.setBackground(new Color(245, 246, 250));
        siparisWrapper.add(pnlSiparisler, BorderLayout.NORTH);

        JScrollPane scrollSiparisler = new JScrollPane(siparisWrapper);
        scrollSiparisler.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96), 2), "Kurye Üzerindeki ve Tamamlanan Siparişler", 0, 0, new Font("Arial", Font.BOLD, 14)));
        scrollSiparisler.getVerticalScrollBar().setUnitIncrement(16);
        
        pnlSagAna.add(scrollSiparisler, BorderLayout.CENTER);

        JPanel pnlAltKontrol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnKuryeAksiyon = new JButton("İşlem Seçin");
        btnKuryeAksiyon.setFont(new Font("Arial", Font.BOLD, 16));
        btnKuryeAksiyon.setPreferredSize(new Dimension(300, 50));
        btnKuryeAksiyon.setFocusPainted(false);
        btnKuryeAksiyon.setEnabled(false);
        
        btnKuryeAksiyon.addActionListener(e -> kuryeAksiyonuGerceklestir());
        pnlAltKontrol.add(btnKuryeAksiyon);
        
        pnlSagAna.add(pnlAltKontrol, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlSagAna);
        add(splitPane, BorderLayout.CENTER);

        kuryeListesi.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && kuryeListesi.getSelectedValue() != null) {
                seciliGercekKuryeAdi = kuryeListesi.getSelectedValue()
                        .replace("🛵 ", "").replace("🟢 ", "")
                        .replace(" (Yolda)", "").replace(" (Müsait)", "").trim();
                
                siparisleriGetir(seciliGercekKuryeAdi);
            }
        });

        kuryeleriGetir();
    }

    public void kuryeleriGetir() {
        new Thread(() -> {
        String cvpAyar = anaPanel.sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
        if (cvpAyar != null && cvpAyar.startsWith("AYARLAR|")) {
            String[] ayarlar = cvpAyar.substring(8).split("\\|\\|\\|");
            for (String a : ayarlar) {
                if (a.isEmpty()) continue;
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
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            SwingUtilities.invokeLater(() -> {
                kuryeListModel.clear();
                if (cvp != null && cvp.startsWith("KURYE_LISTESI|")) {
                    String[] kuryeler = cvp.split("\\|");
                    for (int i = 1; i < kuryeler.length; i++) {
                        String kAdi = kuryeler[i].trim();
                        if (!kAdi.isEmpty() && !kAdi.equals("Sistemde Kayıtlı Motorcu Yok")) {
                            new Thread(() -> {
                                String durumCvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + kAdi);
                                SwingUtilities.invokeLater(() -> {
                                    if (durumCvp != null && durumCvp.contains("YOLA_CIKTI")) {
                                        kuryeListModel.addElement("🛵 " + kAdi + " (Yolda)");
                                    } else {
                                        kuryeListModel.addElement("🟢 " + kAdi + " (Müsait)");
                                    }
                                });
                            }).start();
                        }
                    }
                }
            });
        }).start();
    }

    private void siparisleriGetir(String kuryeAdi) {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + kuryeAdi);
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();
                boolean yoldaMi = false;
                boolean bekleyenVarMi = false;

                if (cvp != null && cvp.startsWith("KURYE_TAKIP_VERI|")) {
                    String[] anaBolumler = cvp.substring(17).split("===GECMIS===");

                    if (anaBolumler.length > 0 && !anaBolumler[0].trim().isEmpty()) {
                        pnlSiparisler.add(createSectionLabel("🚚 AKTİF TESLİMATLAR"));
                        String[] siparisler = anaBolumler[0].split("\\|\\|\\|");
                        for (String s : siparisler) {
                            if (s.trim().isEmpty()) continue;
                            String[] d = s.split("~_~", -1);
                            if (d.length >= 7) { 
                                String durum = d[2];
                                if (durum.equals("YOLA_CIKTI")) yoldaMi = true;
                                if (durum.equals("HAZIR") || durum.equals("BEKLEMEDE") || durum.equals("HAZIRLANIYOR")) bekleyenVarMi = true;
                                
                                pnlSiparisler.add(detayliSiparisKarti(d[0], d[1], durum, d[3], d[4], d[5], d[6]));
                                pnlSiparisler.add(Box.createVerticalStrut(10)); 
                            }
                        }
                    }

                    if (anaBolumler.length > 1 && !anaBolumler[1].trim().isEmpty()) {
                        pnlSiparisler.add(Box.createVerticalStrut(15));
                        pnlSiparisler.add(createSectionLabel("📜 BUGÜN TAMAMLANANLAR"));
                        String[] gecmisler = anaBolumler[1].split("\\|\\|\\|");
                        for (String s : gecmisler) {
                            if (s.trim().isEmpty()) continue;
                            String[] d = s.split("~_~", -1);
                            if (d.length >= 7) {
                                pnlSiparisler.add(detayliSiparisKarti(d[0], d[1], d[2], d[3], d[4], d[5], d[6]));
                                pnlSiparisler.add(Box.createVerticalStrut(10));
                            }
                        }
                    }
                }

                if (pnlSiparisler.getComponentCount() == 0) {
                    JLabel lblBos = new JLabel("<html><br>&nbsp;&nbsp;Bu kurye üzerinde herhangi bir işlem bulunmuyor.</html>");
                    lblBos.setFont(new Font("Arial", Font.ITALIC, 16));
                    lblBos.setForeground(Color.GRAY);
                    pnlSiparisler.add(lblBos);
                }

                if (yoldaMi) {
                    mevcutKuryeDurumu = "YOLDA";
                    btnKuryeAksiyon.setText("🏠 Merkeze Dönüş Yaptı");
                    btnKuryeAksiyon.setBackground(new Color(230, 126, 34)); 
                    btnKuryeAksiyon.setForeground(Color.WHITE);
                    btnKuryeAksiyon.setEnabled(true);
                } else if (bekleyenVarMi) {
                    mevcutKuryeDurumu = "MUSAIT";
                    btnKuryeAksiyon.setText("🛵 Seçili Kuryeyi Yola Çıkart");
                    btnKuryeAksiyon.setBackground(new Color(46, 204, 113)); 
                    btnKuryeAksiyon.setForeground(Color.WHITE);
                    btnKuryeAksiyon.setEnabled(true);
                } else {
                    btnKuryeAksiyon.setText("İşlem Bekleniyor");
                    btnKuryeAksiyon.setBackground(Color.LIGHT_GRAY);
                    btnKuryeAksiyon.setForeground(Color.DARK_GRAY);
                    btnKuryeAksiyon.setEnabled(false);
                }

                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private void kuryeAksiyonuGerceklestir() {
        if (seciliGercekKuryeAdi.isEmpty()) return;
        String komut = mevcutKuryeDurumu.equals("YOLDA") ? "KURYE_MERKEZE_DONDU|" : "KURYE_TOPLU_YOLA_CIKAR|";
        String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut + seciliGercekKuryeAdi);
        JOptionPane.showMessageDialog(this, cvp);
        kuryeleriGetir();
        siparisleriGetir(seciliGercekKuryeAdi);
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel("<html><div style='padding-top:5px; padding-bottom:5px;'><font size='5' color='#2c3e50'><b>" + text + "</b></font><hr></div></html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private String formatZaman(String dt) {
        if (dt == null || dt.contains("Bilinmiyor") || dt.contains("Henüz")) return dt;
        try {
            if (dt.length() >= 16) return dt.substring(11, 16); 
        } catch(Exception e) {}
        return dt;
    }

    // ==========================================
    // AKORDİYON & ADİSYON (FİŞ) ÇIKTISI TASARIMI
    // ==========================================
    private JPanel detayliSiparisKarti(String id, String musteri, String durum, String html, String siparisZamani, String yolaZamani, String teslimZamani) {
        JPanel kartAna = new JPanel(new BorderLayout());
        kartAna.setAlignmentX(Component.LEFT_ALIGNMENT); 
        kartAna.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); 
        
        kartAna.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // --- ÜST PANEL (ÖZET KARTI) ---
        JPanel pnlOzet = new JPanel(new BorderLayout(10, 5));
        boolean isGecmis = durum.equals("TESLIM_EDILDI") || durum.equals("ODENDI");
        pnlOzet.setBackground(durum.equals("YOLA_CIKTI") ? new Color(208, 236, 231) : (isGecmis ? new Color(236, 240, 241) : Color.WHITE));
        pnlOzet.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        pnlOzet.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String adresOzet = "Belirtilmedi";
        try {
            Matcher mAdres = Pattern.compile("Adres:\\s*(.*?)<").matcher(html);
            if (mAdres.find()) {
                adresOzet = mAdres.group(1).trim();
                if (adresOzet.length() > 30) adresOzet = adresOzet.substring(0, 30) + "...";
            }
        } catch (Exception e) {}

        JLabel lblSol = new JLabel("<html><font size='5'><b>#" + id + " - " + musteri + "</b></font><br>" +
                                   "<font size='4' color='#34495e'>Mahalle / Adres: " + adresOzet + "</font></html>");
        
        JLabel lblSag = new JLabel("<html><div style='text-align: right;'><font size='4' color='" + (isGecmis ? "#27ae60" : "#c0392b") + "'><b>" + durum.replace("_", " ") + "</b></font><br>" +
                                   "<font color='blue'>▼ Fişi ve Detayları Gör ▼</font></div></html>");

        pnlOzet.add(lblSol, BorderLayout.WEST);
        pnlOzet.add(lblSag, BorderLayout.EAST);

        // --- ALT PANEL (ADİSYON GÖRÜNÜMÜ VE BUTONLAR) ---
        JPanel pnlDetay = new JPanel(new BorderLayout(5, 5));
        pnlDetay.setVisible(false);
        pnlDetay.setBackground(new Color(236, 240, 241)); // Fiş arkası masa rengi
        pnlDetay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), 
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Dinamik Fiş HTML Tasarımı (Değişkenler Sunucudan Geliyor)
       // --- ADİSYON (TERMAL FİŞ) HTML TASARIMI ---
        // DÜZELTME: İç içe HTML hatasını çözen temizleme kodu
        String temizHtml = html.replaceAll("(?i)<html.*?>", "")
                               .replaceAll("(?i)</html>", "")
                               .replaceAll("(?i)<body.*?>", "")
                               .replaceAll("(?i)</body>", "");

        // HTML ile gerçek termal fiş simülasyonu (3 Bölümlü)
        String adisyonHtml = "<html><body style='background-color:#ffffff; margin: 0; padding: 15px; font-family: monospace, Courier New;'>" +
                             "<div style='width: 320px; margin: 0 auto; border: 1px solid #ddd; padding: 10px; background-color: #fff; box-shadow: 2px 2px 5px rgba(0,0,0,0.1);'>" +
                                
                                // BÖLÜM 1: ÜST METİN (BAŞLIK)
                                "<div style='text-align: center; border-bottom: 2px dashed #000; padding-bottom: 10px; margin-bottom: 10px;'>" +
                                    "<h2 style='margin: 0; font-size: 18px;'>*** " + ayarMagazaAdi + " ***</h2>" +
                                    "<p style='margin: 5px 0 0 0; font-size: 12px; color: #555;'>" + ayarOnBilgi + "</p>" +
                                "</div>" +
                                
                                // BÖLÜM 2: GERÇEK SİPARİŞ İÇERİĞİ
                                "<div style='font-size: 13px; line-height: 1.4; color: #000;'>" + temizHtml + "</div>" + // Temizlenmiş HTML eklendi
                                
                                // BÖLÜM 3: ALT METİN VE ZAMAN DAMGASI
                                "<div style='text-align: center; border-top: 2px dashed #000; padding-top: 10px; margin-top: 10px;'>" +
                                    "<p style='margin: 0; font-size: 12px; font-weight: bold;'>" + ayarAltBilgi + "</p>" +
                                    "<p style='margin: 5px 0 0 0; font-size: 11px; color: #777;'>" + ayarVKN + "</p>" +
                                    "<br><span style='font-size: 10px; color: #777;'>Sipariş Alınma: " + formatZaman(siparisZamani) + "</span>" +
                                "</div>" +
                                
                             "</div></body></html>";

        JEditorPane txtHtml = new JEditorPane("text/html", adisyonHtml);
        txtHtml.setEditable(false);
        // ... (Kodun geri kalanı aynı)

        txtHtml.setBackground(new Color(236, 240, 241));
        pnlDetay.add(txtHtml, BorderLayout.CENTER);

        // --- YAZDIRMA BUTONU EKLENTİSİ ---
        JPanel pnlFisKontrol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlFisKontrol.setBackground(new Color(236, 240, 241));
        
        JButton btnYazdir = new JButton("🖨️ Fişi Yazdır");
        btnYazdir.setFont(new Font("Arial", Font.BOLD, 14));
        btnYazdir.setBackground(new Color(52, 73, 94));
        btnYazdir.setForeground(Color.WHITE);
        btnYazdir.setFocusPainted(false);
        btnYazdir.addActionListener(e -> {
            try {
                // Java'nın yerleşik yazdırma diyalogunu çağırır
                txtHtml.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Yazdırma işlemi başarısız oldu:\n" + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        pnlFisKontrol.add(btnYazdir);
        pnlDetay.add(pnlFisKontrol, BorderLayout.SOUTH);

        // --- AKORDİYON TIKLAMA OLAYI ---
        pnlOzet.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                boolean suAnkiDurum = pnlDetay.isVisible();
                pnlDetay.setVisible(!suAnkiDurum);
                
                lblSag.setText("<html><div style='text-align: right;'><font size='4' color='" + (isGecmis ? "#27ae60" : "#c0392b") + "'><b>" + durum.replace("_", " ") + "</b></font><br>" +
                               "<font color='blue'>" + (!suAnkiDurum ? "▲ Fişi ve Detayları Gizle ▲" : "▼ Fişi ve Detayları Gör ▼") + "</font></div></html>");
                
                kartAna.revalidate();
                pnlSiparisler.revalidate(); 
                pnlSiparisler.repaint();
            }
        });

        kartAna.add(pnlOzet, BorderLayout.NORTH);
        kartAna.add(pnlDetay, BorderLayout.CENTER);

        return kartAna;
    }
}