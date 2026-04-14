

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.print.PrinterException;

public class KuryeTakipModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultListModel<String> kuryeListModel;
    private JList<String> lstKuryeler;
    private JPanel pnlKuryeSiparisler;
    private JLabel lblDurumBaslik;
    private JButton btnGenelIslem;
    private String seciliKurye = "";

    private String ayarMagazaAdi = "Yükleniyor...";
    private String ayarOnBilgi = "";
    private String ayarAltBilgi = "";
    private String ayarVKN = "";

    public KuryeTakipModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ayarlariYukle();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(320);

        // ==========================================
        // SOL PANEL: MESAİDEKİ KURYELER
        // ==========================================
        JPanel pnlSol = new JPanel(new BorderLayout(5, 5));
        pnlSol.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Mesaideki Kuryeler", 0, 0, new Font("Arial", Font.BOLD, 14)));

        kuryeListModel = new DefaultListModel<>();
        lstKuryeler = new JList<>(kuryeListModel);
        lstKuryeler.setFont(new Font("Arial", Font.BOLD, 15));
        lstKuryeler.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstKuryeler.addListSelectionListener(this::kuryeSecildi);

        JScrollPane scKurye = new JScrollPane(lstKuryeler);
        pnlSol.add(scKurye, BorderLayout.CENTER);

        JPanel pnlSolButonlar = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JButton btnVardiya = new JButton("⏱️ Mesai (Vardiya) Başlat/Bitir");
        btnVardiya.setFont(new Font("Arial", Font.BOLD, 14));
        btnVardiya.setBackground(new Color(52, 73, 94));
        btnVardiya.setForeground(Color.WHITE);
        btnVardiya.setFocusPainted(false);
        btnVardiya.addActionListener(e -> vardiyaIslemiYap());
        
        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.setFocusPainted(false);
        btnYenile.addActionListener(e -> verileriYenile());
        
        pnlSolButonlar.add(btnVardiya);
        pnlSolButonlar.add(btnYenile);
        pnlSol.add(pnlSolButonlar, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        // ==========================================
        // SAĞ PANEL: SİPARİŞ DETAYLARI
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10));
        lblDurumBaslik = new JLabel("📍 Lütfen Listeden Bir Kurye Seçiniz", SwingConstants.LEFT);
        lblDurumBaslik.setFont(new Font("Arial", Font.BOLD, 18));
        pnlSag.add(lblDurumBaslik, BorderLayout.NORTH);

        pnlKuryeSiparisler = new JPanel();
        pnlKuryeSiparisler.setLayout(new BoxLayout(pnlKuryeSiparisler, BoxLayout.Y_AXIS));
        pnlKuryeSiparisler.setBackground(Color.WHITE);
        
        JPanel pnlSiparisWrapper = new JPanel(new BorderLayout());
        pnlSiparisWrapper.setBackground(Color.WHITE);
        pnlSiparisWrapper.add(pnlKuryeSiparisler, BorderLayout.NORTH);

        JScrollPane scSiparis = new JScrollPane(pnlSiparisWrapper);
        scSiparis.setBorder(BorderFactory.createLineBorder(new Color(46, 204, 113), 2));
        scSiparis.getVerticalScrollBar().setUnitIncrement(16);
        pnlSag.add(scSiparis, BorderLayout.CENTER);

        btnGenelIslem = new JButton("İşlem Bekleniyor");
        btnGenelIslem.setFont(new Font("Arial", Font.BOLD, 16));
        btnGenelIslem.setEnabled(false);
        btnGenelIslem.setPreferredSize(new Dimension(0, 50));
        btnGenelIslem.addActionListener(e -> kuryeIslemYap());
        pnlSag.add(btnGenelIslem, BorderLayout.SOUTH);

        add(pnlSol, BorderLayout.WEST);
        add(pnlSag, BorderLayout.CENTER);

        kuryeListesiYukle();
    }

    // ==========================================
    // VARDİYA İŞLEMİ (AÇILIR LİSTE - DROPDOWN)
    // ==========================================
    private void vardiyaIslemiYap() {
        String[] secenekler = {"🟢 Mesai Başlat (GİRİŞ)", "🔴 Mesai Bitir (ÇIKIŞ)"};
        int secim = JOptionPane.showOptionDialog(this, 
                "Kurye için hangi vardiya işlemini yapmak istiyorsunuz?", 
                "Vardiya İşlemi", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, 
                null, secenekler, secenekler[0]);

        if (secim >= 0) {
            String islem = secim == 0 ? "GIRIS" : "CIKIS";
            
            // Veritabanındaki tüm kuryeleri açılır liste için çek
            String cvpKuryeler = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            
            if (cvpKuryeler != null && cvpKuryeler.startsWith("KURYE_LISTESI|")) {
                String[] parcalar = cvpKuryeler.split("\\|");
                if (parcalar.length > 1) {
                    String[] kuryeDizisi = new String[parcalar.length - 1];
                    System.arraycopy(parcalar, 1, kuryeDizisi, 0, parcalar.length - 1);
                    
                    String mesaj = secim == 0 ? "Mesaiye başlayacak kuryeyi seçiniz:" : "Mesaisi bitecek kuryeyi seçiniz:";
                    
                    // KULLANICIYA AÇILIR LİSTE (COMBO BOX) GÖSTERİLİR
                    String secilenKurye = (String) JOptionPane.showInputDialog(this, 
                            mesaj, "Kurye Seçimi", JOptionPane.QUESTION_MESSAGE, 
                            null, kuryeDizisi, kuryeDizisi[0]);
                            
                    if (secilenKurye != null && !secilenKurye.trim().isEmpty()) {
                        String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("VARDIYA_ISLEM|" + islem + "|" + secilenKurye.trim());
                        JOptionPane.showMessageDialog(this, cvp);
                        verileriYenile();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Sistemde kayıtlı Kurye/Motokurye bulunamadı!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Sunucudan kurye listesi alınamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void kuryeListesiYukle() {
        new Thread(() -> {
            // SADECE MESAİDE OLANLARI ÇEKER
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("MESAIDEKI_KURYELERI_GETIR");
            SwingUtilities.invokeLater(() -> {
                kuryeListModel.clear();
                if (cvp != null && cvp.startsWith("MESAIDEKI_KURYELER|")) {
                    String[] parcalar = cvp.split("\\|");
                    for (int i = 1; i < parcalar.length; i++) {
                        String kAdi = parcalar[i].trim();
                        if (!kAdi.isEmpty()) {
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

    private void kuryeSecildi(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            String val = lstKuryeler.getSelectedValue();
            if (val != null) {
                seciliKurye = val.replaceAll("[🛵🟢🔴]", "").split("\\(")[0].trim();
                lblDurumBaslik.setText("📍 " + seciliKurye + " Üzerindeki Siparişler");
                kuryeSiparisleriniGetir();
            }
        }
    }

    public void verileriYenile() {
        kuryeListesiYukle();
        if (seciliKurye != null && !seciliKurye.isEmpty()) {
            kuryeSiparisleriniGetir();
        }
    }

    private void kuryeSiparisleriniGetir() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + seciliKurye);
            
            SwingUtilities.invokeLater(() -> {
                pnlKuryeSiparisler.removeAll();
                boolean yoldaMi = false;
                boolean bekleyenVarMi = false;

                if (cvp != null && cvp.contains("|")) {
                    String data = cvp.substring(cvp.indexOf('|') + 1);
                    if (!data.equals("BOS") && !data.trim().isEmpty()) {
                        String[] bolumler = data.split("===GECMIS===");

                        if (bolumler.length > 0 && !bolumler[0].trim().isEmpty()) {
                            JLabel lblBaslik1 = new JLabel("<html><div style='padding-top:10px;'><font size='5' color='#e67e22'><b>🚚 AKTİF SİPARİŞLER</b></font><hr></div></html>");
                            lblBaslik1.setAlignmentX(Component.LEFT_ALIGNMENT);
                            pnlKuryeSiparisler.add(lblBaslik1);
                            
                            String[] siparisler = bolumler[0].split("\\|\\|\\|");
                            for (String s : siparisler) {
                                if (s.trim().isEmpty()) continue;
                                String[] d = s.split("~_~", -1);
                                
                                String id = d.length > 0 ? d[0] : "?"; 
                                String musteri = d.length > 1 ? d[1] : "Bilinmiyor"; 
                                String durum = d.length > 2 ? d[2] : ""; 
                                String html = d.length > 3 ? d[3] : "";
                                String zaman = d.length > 4 ? d[4] : "Bilinmiyor";

                                for (String col : d) {
                                    if(col.contains("YOLA") || col.contains("HAZIR") || col.contains("BEKLE")) durum = col;
                                }

                                if (durum.contains("YOLA_CIKTI")) yoldaMi = true;
                                if (durum.contains("HAZIR") || durum.contains("BEKLE")) bekleyenVarMi = true;

                                pnlKuryeSiparisler.add(siparisKartiOlustur(id, musteri, durum, html, zaman));
                                pnlKuryeSiparisler.add(Box.createVerticalStrut(10));
                            }
                        }

                        if (bolumler.length > 1 && !bolumler[1].trim().isEmpty()) {
                            pnlKuryeSiparisler.add(Box.createVerticalStrut(15));
                            JLabel lblBaslik2 = new JLabel("<html><div style='padding-top:10px;'><font size='5' color='#27ae60'><b>📜 GEÇMİŞ / TAMAMLANANLAR</b></font><hr></div></html>");
                            lblBaslik2.setAlignmentX(Component.LEFT_ALIGNMENT);
                            pnlKuryeSiparisler.add(lblBaslik2);
                            
                            String[] gecmisler = bolumler[1].split("\\|\\|\\|");
                            for (String s : gecmisler) {
                                if (s.trim().isEmpty()) continue;
                                String[] d = s.split("~_~", -1);
                                
                                String id = d.length > 0 ? d[0] : "?";
                                String musteri = d.length > 1 ? d[1] : "Bilinmiyor";
                                String html = d.length > 3 ? d[3] : "";
                                String zaman = d.length > 4 ? d[4] : "Bilinmiyor";
                                
                                pnlKuryeSiparisler.add(siparisKartiOlustur(id, musteri, "TESLİM EDİLDİ", html, zaman));
                                pnlKuryeSiparisler.add(Box.createVerticalStrut(10));
                            }
                        }
                    }
                }

                if (pnlKuryeSiparisler.getComponentCount() == 0) {
                    JLabel lblBos = new JLabel("<html><br>&nbsp;&nbsp;Bu kurye üzerinde herhangi bir işlem bulunmuyor.</html>");
                    lblBos.setFont(new Font("Arial", Font.ITALIC, 16));
                    lblBos.setForeground(Color.GRAY);
                    lblBos.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pnlKuryeSiparisler.add(lblBos);
                }

                if (yoldaMi) {
                    btnGenelIslem.setText("🏠 " + seciliKurye + " Merkeze Dönüş Yaptı (Teslimatları Kapat)");
                    btnGenelIslem.setBackground(new Color(230, 126, 34));
                    btnGenelIslem.setForeground(Color.WHITE);
                    btnGenelIslem.setEnabled(true);
                } else if (bekleyenVarMi) {
                    btnGenelIslem.setText("🛵 " + seciliKurye + " Siparişleri Aldı, Yola Çıkar");
                    btnGenelIslem.setBackground(new Color(46, 204, 113));
                    btnGenelIslem.setForeground(Color.WHITE);
                    btnGenelIslem.setEnabled(true);
                } else {
                    btnGenelIslem.setText("İşlem Bekleniyor");
                    btnGenelIslem.setBackground(Color.LIGHT_GRAY);
                    btnGenelIslem.setForeground(Color.DARK_GRAY);
                    btnGenelIslem.setEnabled(false);
                }

                pnlKuryeSiparisler.revalidate();
                pnlKuryeSiparisler.repaint();
            });
        }).start();
    }

    private void kuryeIslemYap() {
        if (seciliKurye.isEmpty()) return;
        String durum = btnGenelIslem.getText();
        String komut = durum.contains("Merkeze") ? "KURYE_MERKEZE_DONDU|" : "KURYE_TOPLU_YOLA_CIKAR|";
        
        String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut + seciliKurye);
        JOptionPane.showMessageDialog(this, cvp);
        verileriYenile();
    }

    private JPanel siparisKartiOlustur(String id, String musteri, String durum, String html, String zaman) {
        JPanel kart = new JPanel(new BorderLayout(10, 5));
        kart.setAlignmentX(Component.LEFT_ALIGNMENT); 
        kart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        kart.setBackground(new Color(248, 249, 249));
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String renk = durum.contains("TESLİM") ? "green" : (durum.contains("YOLA") ? "red" : "orange");
        String info = "<html><b>#" + id + " - Müşteri: " + musteri + "</b><br>Durum: <font color='" + renk + "'>" + durum + "</font> | Zaman: " + formatZaman(zaman) + "</html>";
        
        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(new Font("Arial", Font.PLAIN, 15));
        kart.add(lblInfo, BorderLayout.CENTER);

        JButton btnSec = new JButton("Fişi Gör / Yazdır");
        btnSec.setFocusPainted(false);
        btnSec.addActionListener(e -> fisGoster(id, html, zaman));
        kart.add(btnSec, BorderLayout.EAST);

        return kart;
    }

    private void fisGoster(String id, String html, String zaman) {
        JDialog d = new JDialog();
        d.setTitle("Fiş Detayı #" + id);
        d.setSize(350, 500);
        d.setLocationRelativeTo(this);
        
        String temiz = html.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "");
        String full = "<html><body style='font-family:monospace; padding:10px; background:white;'>" +
                      "<div style='border:1px solid #000; padding:10px;'>" +
                      "<center><b>" + ayarMagazaAdi + "</b><br>" + ayarOnBilgi + "<hr></center>" +
                      temiz + "<hr><center>" + ayarAltBilgi + "<br>" + ayarVKN + "<br>Kayıt: " + formatZaman(zaman) + "</center></div></body></html>";
        
        JEditorPane ep = new JEditorPane("text/html", full);
        ep.setEditable(false);
        d.add(new JScrollPane(ep), BorderLayout.CENTER);
        
        JButton p = new JButton("🖨️ Yazdır");
        p.setFont(new Font("Arial", Font.BOLD, 14));
        p.setBackground(new Color(41, 128, 185));
        p.setForeground(Color.WHITE);
        p.setFocusPainted(false);
        p.addActionListener(ex -> { try { ep.print(); } catch (PrinterException pex) {} });
        d.add(p, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private String formatZaman(String dt) {
        if (dt == null || dt.length() < 16) return dt;
        return dt.substring(11, 16);
    }

    private void ayarlariYukle() {
        new Thread(() -> {
            String cvpAyar = anaPanel.sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
            if (cvpAyar != null && cvpAyar.startsWith("AYARLAR|")) {
                String[] ayarlar = cvpAyar.substring(8).split("\\|\\|\\|");
                for (String a : ayarlar) {
                    if (a.trim().isEmpty()) continue;
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
}