import javax.swing.*;
import java.awt.*;
import java.awt.print.PrinterException;

public class KuryeTakipModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultListModel<String> kuryeListModel;
    private JList<String> kuryeListesi;
    private JPanel pnlSiparisler;
    
    private JButton btnKuryeAksiyon; 
    private String mevcutKuryeDurumu = "BEKLIYOR";
    private String seciliGercekKuryeAdi = ""; 
    
    private String ayarMagazaAdi = "Yükleniyor...";
    private String ayarOnBilgi = "";
    private String ayarAltBilgi = "";
    private String ayarVKN = "";

    public KuryeTakipModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        ayarlariYukle();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(320);

        // --- SOL PANEL (Aktif Kuryeler) ---
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

        // --- SAĞ PANEL (Kurye Üzerindeki Siparişler) ---
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
        btnKuryeAksiyon.setEnabled(false);
        
        btnKuryeAksiyon.addActionListener(e -> kuryeAksiyonuGerceklestir());
        pnlAltKontrol.add(btnKuryeAksiyon);
        
        pnlSagAna.add(pnlAltKontrol, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlSagAna);
        add(splitPane, BorderLayout.CENTER);

        // KURYE SEÇİMİ (KESİN TEMİZLEME MANTIĞI)
        kuryeListesi.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && kuryeListesi.getSelectedValue() != null) {
                String hamMetin = kuryeListesi.getSelectedValue();
                // Emojileri ve parantezli durum yazılarını temizler, sadece kurye adını bırakır
                seciliGercekKuryeAdi = hamMetin.replaceAll("[🛵🟢🔴]", "").split("\\(")[0].trim();
                siparisleriGetir(seciliGercekKuryeAdi);
            }
        });

        kuryeleriGetir();
    }

    private void ayarlariYukle() {
        new Thread(() -> {
            String cvpAyar = anaPanel.sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
            if (cvpAyar != null && cvpAyar.startsWith("AYARLAR|")) {
                String[] ayarlar = cvpAyar.substring(8).split("\\|\\|\\|");
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

    public void kuryeleriGetir() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            SwingUtilities.invokeLater(() -> {
                kuryeListModel.clear();
                if (cvp != null && cvp.startsWith("KURYE_LISTESI|")) {
                    String[] parcalar = cvp.split("\\|");
                    for (int i = 1; i < parcalar.length; i++) {
                        String kAdi = parcalar[i].trim();
                        if (!kAdi.isEmpty()) {
                            String durumCvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + kAdi);
                            if (durumCvp != null && durumCvp.contains("YOLA_CIKTI")) {
                                kuryeListModel.addElement("🛵 " + kAdi + " (Yolda)");
                            } else {
                                kuryeListModel.addElement("🟢 " + kAdi + " (Müsait)");
                            }
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
                    String data = cvp.substring(17);
                    if(!data.equals("BOS") && !data.isEmpty()) {
                        String[] anaBolumler = data.split("===GECMIS===");

                        if (anaBolumler.length > 0 && !anaBolumler[0].trim().isEmpty()) {
                            pnlSiparisler.add(new JLabel("<html><font size='5' color='#2c3e50'><b>🚚 AKTİF TESLİMATLAR</b></font><hr></html>"));
                            String[] siparisler = anaBolumler[0].split("\\|\\|\\|");
                            for (String s : siparisler) {
                                if (s.trim().isEmpty()) continue;
                                String[] d = s.split("~_~", -1);
                                if (d.length >= 4) { 
                                    if (d[2].equals("YOLA_CIKTI")) yoldaMi = true;
                                    if (d[2].equals("HAZIR") || d[2].equals("BEKLEMEDE")) bekleyenVarMi = true;
                                    String sZaman = (d.length > 4) ? d[4] : "Bilinmiyor";
                                    pnlSiparisler.add(detayliSiparisKarti(d[0], d[1], d[2], d[3], sZaman));
                                    pnlSiparisler.add(Box.createVerticalStrut(10));
                                }
                            }
                        }

                        if (anaBolumler.length > 1 && !anaBolumler[1].trim().isEmpty()) {
                            pnlSiparisler.add(Box.createVerticalStrut(15));
                            pnlSiparisler.add(new JLabel("<html><font size='5' color='#27ae60'><b>📜 BUGÜN TAMAMLANANLAR</b></font><hr></html>"));
                            String[] gecmisler = anaBolumler[1].split("\\|\\|\\|");
                            for (String s : gecmisler) {
                                if (s.trim().isEmpty()) continue;
                                String[] d = s.split("~_~", -1);
                                if (d.length >= 4) {
                                    String sZaman = (d.length > 4) ? d[4] : "Bilinmiyor";
                                    pnlSiparisler.add(detayliSiparisKarti(d[0], d[1], "TESLİM EDİLDİ", d[3], sZaman));
                                    pnlSiparisler.add(Box.createVerticalStrut(10));
                                }
                            }
                        }
                    }
                }

                if (pnlSiparisler.getComponentCount() == 0) {
                    JLabel lblBos = new JLabel("<html><br>&nbsp;&nbsp;Bu kurye üzerinde işlem bulunmuyor.</html>");
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
                    btnKuryeAksiyon.setEnabled(false);
                    btnKuryeAksiyon.setText("İşlem Bekleniyor");
                    btnKuryeAksiyon.setBackground(Color.LIGHT_GRAY);
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

    private JPanel detayliSiparisKarti(String id, String musteri, String durum, String html, String zaman) {
        JPanel kart = new JPanel(new BorderLayout(15, 0));
        kart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        kart.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        kart.setBackground(Color.WHITE);

        JLabel lblInfo = new JLabel("<html><b>Sipariş #" + id + " - " + musteri + "</b><br>Durum: <font color='" + (durum.equals("TESLİM EDİLDİ") ? "green" : "red") + "'>" + durum + "</font></html>");
        kart.add(lblInfo, BorderLayout.CENTER);

        JButton btnFis = new JButton("Fişi Gör");
        btnFis.addActionListener(e -> {
            JDialog d = new JDialog();
            d.setTitle("Fiş Detayı #" + id);
            d.setSize(350, 550);
            d.setLocationRelativeTo(this);
            
            String temiz = html.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "");
            String full = "<html><body style='font-family:monospace; padding:10px; background:white;'>" +
                          "<div style='border:1px solid #000; padding:10px;'>" +
                          "<center><b>" + ayarMagazaAdi + "</b><br>" + ayarOnBilgi + "<hr></center>" +
                          temiz + "<hr><center>" + ayarAltBilgi + "<br>" + ayarVKN + "<br>Saat: " + zaman + "</center></div></body></html>";
            
            JEditorPane ep = new JEditorPane("text/html", full);
            ep.setEditable(false);
            d.add(new JScrollPane(ep), BorderLayout.CENTER);
            
            JButton p = new JButton("🖨️ Yazdır");
            p.addActionListener(ex -> { try { ep.print(); } catch (Exception pex) {} });
            d.add(p, BorderLayout.SOUTH);
            d.setVisible(true);
        });
        kart.add(btnFis, BorderLayout.EAST);

        return kart;
    }
}