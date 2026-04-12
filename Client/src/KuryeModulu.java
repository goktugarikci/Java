import javax.swing.*;
import java.awt.*;

public class KuryeModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private String aktifKurye;
    private JPanel pnlSiparisler;
    private Timer otoYenileme;
    
    private String ayarMagazaAdi = "Yükleniyor...";
    private String ayarOnBilgi = "";
    private String ayarAltBilgi = "";
    private String ayarVKN = "";

    public KuryeModulu(PersonelPaneli anaPanel, String aktifKurye) {
        this.anaPanel = anaPanel;
        this.aktifKurye = aktifKurye;
        
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        ayarlariYukle();

        // --- ÜST PANEL ---
        JPanel pnlUst = new JPanel(new BorderLayout());
        pnlUst.setOpaque(false);
        JLabel lblBaslik = new JLabel("🛵 Hoş Geldin, " + aktifKurye + " | Aktif Teslimatların");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        lblBaslik.setForeground(new Color(44, 62, 80));
        pnlUst.add(lblBaslik, BorderLayout.WEST);

        JButton btnYenile = new JButton("🔄 Sayfayı Yenile");
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.addActionListener(e -> verileriYenile());
        pnlUst.add(btnYenile, BorderLayout.EAST);
        
        add(pnlUst, BorderLayout.NORTH);

        // --- İÇERİK PANELİ ---
        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(Color.WHITE);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(pnlSiparisler, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 2));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Her 10 saniyede bir yeni sipariş var mı kontrol et
        otoYenileme = new Timer(10000, e -> verileriYenile());
        otoYenileme.start();

        // İlk açılışta verileri yükle
        verileriYenile();
    }

    private void ayarlariYukle() {
        new Thread(() -> {
            String cvpAyar = anaPanel.sunucuyaKomutGonderVeCevapAl("TUM_AYARLARI_GETIR");
            if (cvpAyar != null && cvpAyar.startsWith("AYARLAR|")) {
                String[] ayarlar = cvpAyar.substring(8).split("\\|\\|\\|");
                for (String a : ayarlar) {
                    if(a.trim().isEmpty()) continue;
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

    // DÜZELTME: Metot adı verileriYenile olarak değiştirildi ve public yapıldı
    public void verileriYenile() {
        new Thread(() -> {
            // Sadece bu kuryeye ait aktif (YOLA_CIKTI) siparişleri getirir
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_USER_SIPARIS_GETIR|" + aktifKurye);
            
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();

                if (cvp != null && cvp.startsWith("KURYE_SIPARISLERI|")) {
                    String data = cvp.substring(18);
                    if (!data.equals("BOS") && !data.isEmpty()) {
                        String[] siparisler = data.split("\\|\\|\\|");
                        for (String s : siparisler) {
                            if (s.trim().isEmpty()) continue;
                            String[] d = s.split("~_~", -1);
                            
                            // Kurye kendi ekranında sadece YOLA ÇIKMIŞ paketleri görür
                            // d dizisi: 0=ID, 1=Tur, 2=Musteri, 3=Durum, 4=HTML
                            if (d.length >= 5 && d[3].contains("YOLA_CIKTI")) {
                                pnlSiparisler.add(kuryeKartiOlustur(d[0], d[1], d[2], d[4]));
                                pnlSiparisler.add(Box.createVerticalStrut(10));
                            }
                        }
                    }
                }

                if (pnlSiparisler.getComponentCount() == 0) {
                    JLabel lblBos = new JLabel("<html><br>&nbsp;&nbsp;Şu anda üzerinde teslim edilecek paket bulunmuyor.</html>");
                    lblBos.setFont(new Font("Arial", Font.ITALIC, 16));
                    lblBos.setForeground(Color.GRAY);
                    pnlSiparisler.add(lblBos);
                }

                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private JPanel kuryeKartiOlustur(String id, String tur, String musteri, String html) {
        JPanel kart = new JPanel(new BorderLayout(10, 10));
        kart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        kart.setBackground(new Color(235, 245, 251));

        JLabel lblInfo = new JLabel("<html><font size='5'><b>#" + id + " - " + musteri + "</b></font><br><font color='#7f8c8d'>Tür: " + tur + "</font></html>");
        kart.add(lblInfo, BorderLayout.CENTER);

        JPanel pnlButonlar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlButonlar.setOpaque(false);

        JButton btnFis = new JButton("Fişi Gör");
        btnFis.setPreferredSize(new Dimension(120, 50));
        btnFis.addActionListener(e -> fisGoster(id, html));
        
        JButton btnTeslimEt = new JButton("✅ Müşteriye Teslim Ettim");
        btnTeslimEt.setFont(new Font("Arial", Font.BOLD, 14));
        btnTeslimEt.setBackground(new Color(39, 174, 96));
        btnTeslimEt.setForeground(Color.WHITE);
        btnTeslimEt.setPreferredSize(new Dimension(220, 50));
        
        btnTeslimEt.addActionListener(e -> {
            int onay = JOptionPane.showConfirmDialog(this, "Paketi müşteriye teslim ettiniz mi?", "Teslimat Onayı", JOptionPane.YES_NO_OPTION);
            if (onay == JOptionPane.YES_OPTION) {
                // Kurye teslim ettiğinde sipariş durumu TESLIM_EDILDI olur
                anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TESLIM_ET|" + id + "|" + aktifKurye);
                verileriYenile(); // İşlem sonrası listeyi hemen güncelle
            }
        });

        pnlButonlar.add(btnFis);
        pnlButonlar.add(btnTeslimEt);
        kart.add(pnlButonlar, BorderLayout.EAST);

        return kart;
    }

    private void fisGoster(String id, String html) {
        JDialog d = new JDialog();
        d.setTitle("Adres ve Fiş Detayı #" + id);
        d.setSize(350, 500);
        d.setLocationRelativeTo(this);
        
        String temiz = html.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "");
        String full = "<html><body style='font-family:monospace; padding:10px; background:white;'>" +
                      "<div style='border:1px solid #000; padding:10px;'>" +
                      "<center><b>" + ayarMagazaAdi + "</b><br>" + ayarOnBilgi + "<hr></center>" +
                      temiz + "<hr><center>" + ayarAltBilgi + "<br>" + ayarVKN + "</center></div></body></html>";
        
        JEditorPane ep = new JEditorPane("text/html", full);
        ep.setEditable(false);
        d.add(new JScrollPane(ep), BorderLayout.CENTER);
        d.setVisible(true);
    }
}