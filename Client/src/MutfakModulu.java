
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MutfakModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private JPanel pnlSiparisler;
    private Timer zamanlayici;
    private Map<JLabel, String> zamanEtiketleri; // Sipariş zamanlarını takip eder

    public MutfakModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        zamanEtiketleri = new HashMap<>();

        // --- ÜST BAŞLIK ---
        JPanel pnlUst = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("👨‍🍳 Mutfak Sipariş Panosu");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 24));
        
        JButton btnYenile = new JButton("🔄 Yenile");
        btnYenile.setBackground(new Color(52, 152, 219));
        btnYenile.setForeground(Color.WHITE);
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.addActionListener(e -> verileriYenile());
        
        pnlUst.add(lblBaslik, BorderLayout.WEST);
        pnlUst.add(btnYenile, BorderLayout.EAST);
        add(pnlUst, BorderLayout.NORTH);

        // --- SİPARİŞLER (KART YAPISI) ---
        pnlSiparisler = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        pnlSiparisler.setBackground(new Color(236, 240, 241));
        
        JScrollPane scrollPane = new JScrollPane(pnlSiparisler);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 1 Saniyelik Canlı Kronometre Başlatıcı
        zamanlayici = new Timer(1000, e -> zamanlariGuncelle());
        zamanlayici.start();
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("MUTFAK_SIPARIS_GETIR_FULL");
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();
                zamanEtiketleri.clear();

                if (cvp != null && cvp.startsWith("MUTFAK_FULL_VERI|") && cvp.length() > 17) {
                    String[] siparisler = cvp.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~", -1);
                        
                        // Format: OrderID, MasaAdi, Musteri, Urunler, Durum, Tarih
                        if (d.length >= 6) {
                            pnlSiparisler.add(siparisKartiOlustur(d[0], d[1], d[2], d[3], d[4], d[5]));
                        }
                    }
                } else {
                    JLabel lblBos = new JLabel("Mutfakta bekleyen aktif sipariş yok.");
                    lblBos.setFont(new Font("Arial", Font.ITALIC, 18));
                    pnlSiparisler.add(lblBos);
                }
                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private JPanel siparisKartiOlustur(String id, String masa, String musteri, String urunler, String durum, String tarih) {
        JPanel kart = new JPanel(new BorderLayout(5, 5));
        kart.setPreferredSize(new Dimension(320, 280));
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Duruma göre arkaplan rengi
        if (durum.equals("YENI")) kart.setBackground(new Color(253, 235, 208)); // Açık turuncu
        else if (durum.equals("HAZIRLANIYOR")) kart.setBackground(new Color(212, 230, 241)); // Açık mavi
        else kart.setBackground(new Color(208, 236, 231)); // Açık yeşil

        // --- ÜST: Sipariş No ve Canlı Zaman ---
        JPanel pnlUst = new JPanel(new BorderLayout());
        pnlUst.setOpaque(false);
        
        JLabel lblId = new JLabel("Sipariş No: #" + id);
        lblId.setFont(new Font("Arial", Font.BOLD, 14));
        
        JLabel lblZaman = new JLabel("00:00:00");
        lblZaman.setFont(new Font("Arial", Font.BOLD, 15));
        lblZaman.setForeground(new Color(192, 57, 43)); // Kırmızı Kronometre
        zamanEtiketleri.put(lblZaman, tarih); // Tıklayan zamanlayıcıya kaydet
        
        pnlUst.add(lblId, BorderLayout.WEST);
        pnlUst.add(lblZaman, BorderLayout.EAST);
        kart.add(pnlUst, BorderLayout.NORTH);

        // --- ORTA: Müşteri Adı ve İçerik Listesi ---
        JPanel pnlOrta = new JPanel(new BorderLayout(0, 5));
        pnlOrta.setOpaque(false);
        
        JLabel lblMusteri = new JLabel("👤 " + musteri + " (" + masa + ")");
        lblMusteri.setFont(new Font("Arial", Font.BOLD, 15));
        lblMusteri.setForeground(Color.BLUE);
        pnlOrta.add(lblMusteri, BorderLayout.NORTH);

        // İçerikleri alt alta yazdır (Virgülleri alt satıra çevir)
        String temizUrunler = urunler.replace(",", "\n• ");
        if(!temizUrunler.startsWith("•")) temizUrunler = "• " + temizUrunler;
        
        JTextArea txtUrunler = new JTextArea(temizUrunler);
        txtUrunler.setEditable(false);
        txtUrunler.setOpaque(false);
        txtUrunler.setFont(new Font("Arial", Font.PLAIN, 15));
        pnlOrta.add(new JScrollPane(txtUrunler), BorderLayout.CENTER);
        
        kart.add(pnlOrta, BorderLayout.CENTER);

        // --- ALT: İşlem Butonları ---
        JPanel pnlAlt = new JPanel(new GridLayout(1, 2, 5, 0));
        pnlAlt.setOpaque(false);
        
        JButton btnHazirlaniyor = new JButton("Hazırlanıyor");
        btnHazirlaniyor.setBackground(new Color(41, 128, 185)); btnHazirlaniyor.setForeground(Color.WHITE);
        btnHazirlaniyor.setEnabled(!durum.equals("HAZIRLANIYOR") && !durum.equals("HAZIR"));
        btnHazirlaniyor.addActionListener(e -> durumDegistir(id, "HAZIRLANIYOR"));

        JButton btnHazir = new JButton("Hazır");
        btnHazir.setBackground(new Color(39, 174, 96)); btnHazir.setForeground(Color.WHITE);
        btnHazir.setEnabled(!durum.equals("HAZIR"));
        btnHazir.addActionListener(e -> durumDegistir(id, "HAZIR"));

        pnlAlt.add(btnHazirlaniyor);
        pnlAlt.add(btnHazir);
        kart.add(pnlAlt, BorderLayout.SOUTH);

        return kart;
    }

    private void durumDegistir(String id, String yeniDurum) {
        new Thread(() -> {
            anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + id + "|" + yeniDurum);
            SwingUtilities.invokeLater(() -> verileriYenile());
        }).start();
    }

    // Kronometreyi her 1 saniyede bir tetikleyen arka plan metodu
    private void zamanlariGuncelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date suan = new Date();
        
        for (Map.Entry<JLabel, String> entry : zamanEtiketleri.entrySet()) {
            try {
                Date siparisZamani = sdf.parse(entry.getValue());
                long farkMs = suan.getTime() - siparisZamani.getTime();
                
                if(farkMs < 0) farkMs = 0;

                long saniye = (farkMs / 1000) % 60;
                long dakika = (farkMs / (1000 * 60)) % 60;
                long saat = (farkMs / (1000 * 60 * 60)) % 24;

                String formatliZaman = String.format("%02d:%02d:%02d", saat, dakika, saniye);
                entry.getKey().setText("⌛ " + formatliZaman);
            } catch (Exception e) {
                entry.getKey().setText("⌛ Bekliyor..");
            }
        }
    }
}