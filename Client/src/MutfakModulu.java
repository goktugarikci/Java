import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MutfakModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private JPanel pnlSiparisler;
    private Timer zamanlayici;
    private Timer otoYenileme;
    private Map<JLabel, String> zamanEtiketleri;
    private Set<String> bilinenSiparisler;
    private boolean uyariEkraniAcik = false; 
    private String aktifSekme = "AKTIF"; 
    
    private JButton btnAktif;
    private JButton btnGecmis;

    public MutfakModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout()); 
        setBackground(new Color(236, 240, 241)); 

        zamanEtiketleri = new HashMap<>();
        bilinenSiparisler = new HashSet<>();

        // ==========================================
        // 1. ÜST NAVİGASYON BARI
        // ==========================================
        JPanel pnlUstBar = new JPanel(new BorderLayout());
        pnlUstBar.setBackground(new Color(44, 62, 80)); 
        pnlUstBar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel pnlSekmeler = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlSekmeler.setOpaque(false);

        btnAktif = new JButton("🍽️ Aktif Siparişler");
        btnAktif.setFont(new Font("Arial", Font.BOLD, 15));
        btnAktif.setForeground(Color.WHITE);
        btnAktif.setFocusPainted(false);
        btnAktif.setPreferredSize(new Dimension(180, 40));

        btnGecmis = new JButton("🗄️ Geçmiş Siparişler");
        btnGecmis.setFont(new Font("Arial", Font.BOLD, 15));
        btnGecmis.setForeground(Color.WHITE);
        btnGecmis.setFocusPainted(false);
        btnGecmis.setPreferredSize(new Dimension(200, 40));

        sekmeGorselGuncelle();

        btnAktif.addActionListener(e -> { 
            aktifSekme = "AKTIF"; 
            sekmeGorselGuncelle();
            verileriYenile(); 
        });
        
        btnGecmis.addActionListener(e -> { 
            aktifSekme = "GECMIS"; 
            sekmeGorselGuncelle();
            verileriYenile(); 
        });

        pnlSekmeler.add(btnAktif);
        pnlSekmeler.add(btnGecmis);

        JButton btnManuelYenile = new JButton("🔄 Sayfayı Yenile");
        btnManuelYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnManuelYenile.setBackground(new Color(52, 152, 219));
        btnManuelYenile.setForeground(Color.WHITE);
        btnManuelYenile.setFocusPainted(false);
        btnManuelYenile.addActionListener(e -> verileriYenile());

        pnlUstBar.add(pnlSekmeler, BorderLayout.WEST);
        pnlUstBar.add(btnManuelYenile, BorderLayout.EAST);

        add(pnlUstBar, BorderLayout.NORTH);

        // ==========================================
        // 2. İÇERİK ALANI (KAYDIRILABİLİR LİSTE)
        // ==========================================
        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(new Color(236, 240, 241)); 
        pnlSiparisler.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel siparisWrapper = new JPanel(new BorderLayout());
        siparisWrapper.setBackground(new Color(236, 240, 241));
        siparisWrapper.add(pnlSiparisler, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(siparisWrapper);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);

        zamanlayici = new Timer(1000, e -> zamanlariGuncelle());
        zamanlayici.start();
        
        otoYenileme = new Timer(5000, e -> {
            if (!uyariEkraniAcik) verileriYenile();
        });
        otoYenileme.start();
    }

    private void sekmeGorselGuncelle() {
        if(aktifSekme.equals("AKTIF")) {
            btnAktif.setBackground(new Color(46, 204, 113)); 
            btnGecmis.setBackground(new Color(52, 73, 94)); 
        } else {
            btnAktif.setBackground(new Color(52, 73, 94)); 
            btnGecmis.setBackground(new Color(255, 152, 0)); 
        }
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("MUTFAK_SIPARIS_GETIR_FULL");
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();
                zamanEtiketleri.clear();

                if (cvp != null && cvp.startsWith("MUTFAK_FULL_VERI|")) {
                    String data = cvp.substring(17);
                    if (!data.isEmpty() && !data.equals("BOS")) {
                        String[] siparisler = data.split("\\|\\|\\|");
                        Set<String> guncelIdler = new HashSet<>();
                        boolean yeniSiparisVar = false;

                        for (String s : siparisler) {
                            if (s.trim().isEmpty()) continue;
                            String[] d = s.split("~_~", -1);
                            if (d.length >= 6) {
                                String id = d[0];
                                String durum = d[4];
                                guncelIdler.add(id);
                                
                                if (!bilinenSiparisler.contains(id) && (durum.equals("YENI") || durum.equals("BEKLEMEDE"))) {
                                    yeniSiparisVar = true;
                                }

                                boolean isGecmis = durum.equals("HAZIR") || durum.equals("ODENDI") || durum.equals("IPTAL") || durum.equals("YOLA_CIKTI");
                                
                                if (aktifSekme.equals("AKTIF") && !isGecmis) {
                                    pnlSiparisler.add(siparisKartiOlustur(id, d[1], d[2], d[3], durum, d[5]));
                                    pnlSiparisler.add(Box.createVerticalStrut(15)); 
                                } else if (aktifSekme.equals("GECMIS") && isGecmis) {
                                    pnlSiparisler.add(siparisKartiOlustur(id, d[1], d[2], d[3], durum, d[5]));
                                    pnlSiparisler.add(Box.createVerticalStrut(15)); 
                                }
                            }
                        }

                        if (yeniSiparisVar && !bilinenSiparisler.isEmpty() && !uyariEkraniAcik) {
                            yeniSiparisBildirimi();
                        }
                        bilinenSiparisler = guncelIdler;

                    } else {
                        bilinenSiparisler.clear();
                        gosterBosMesaj("Bu sekmede gösterilecek sipariş bulunamadı.");
                    }
                } else {
                    gosterBosMesaj("<html><font color='red'>Sunucu Hatası veya Bağlantı Yok!</font></html>");
                }
                
                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private void gosterBosMesaj(String msj) {
        JLabel lblBos = new JLabel(msj);
        lblBos.setFont(new Font("Arial", Font.BOLD, 18));
        lblBos.setForeground(Color.DARK_GRAY);
        lblBos.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlSiparisler.add(lblBos);
    }

    private void yeniSiparisBildirimi() {
        uyariEkraniAcik = true;
        Toolkit.getDefaultToolkit().beep(); 
        new Thread(() -> {
            JOptionPane.showMessageDialog(this, "Mutfak panosuna yeni bir sipariş düştü!", "🔔 YENİ SİPARİŞ", JOptionPane.WARNING_MESSAGE);
            uyariEkraniAcik = false; 
            verileriYenile(); 
        }).start();
    }

    // ==========================================
    // 3. TAMAMEN AÇIK SABİT SİPARİŞ KARTI (HIZLI ONAY SÜRÜMÜ)
    // ==========================================
    private JPanel siparisKartiOlustur(String id, String masa, String musteri, String html, String durum, String tarih) {
        JPanel kartAna = new JPanel(new BorderLayout(0, 5));
        kartAna.setAlignmentX(Component.LEFT_ALIGNMENT);
        kartAna.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); 
        
        Color bgRenk = new Color(250, 250, 250); 
        if (durum.equals("YENI") || durum.equals("BEKLEMEDE")) bgRenk = new Color(255, 249, 196);
        else if (durum.equals("HAZIRLANIYOR")) bgRenk = new Color(200, 230, 201);
        else if (durum.equals("IPTAL")) bgRenk = new Color(255, 205, 210);

        kartAna.setBackground(bgRenk);
        kartAna.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15) 
        ));

        // --- VERİ TEMİZLEME ---
        String siparisiAlan = "Bilinmiyor";
        String temizUrunler = html;
        try {
            String safMetin = html.replace("<br>", "\n").replaceAll("<[^>]+>", " ");
            Matcher mMus = Pattern.compile("Müşteri:\\s*([^\\n]+)").matcher(safMetin);
            if (mMus.find()) musteri = mMus.group(1).trim();
            Matcher mAla = Pattern.compile("Siparişi Alan:\\s*([^\\n]+)").matcher(safMetin);
            if (mAla.find()) siparisiAlan = mAla.group(1).trim();
            temizUrunler = html.replaceAll("(?i)<html.*?>|</html>|<body.*?>|</body>", "")
                               .split("(?i)<!\\-\\-PRICE")[0].replace("<hr>", "").trim();
        } catch (Exception e) { temizUrunler = "İçerik Hatası"; }

        // --- 1. ÜST PANEL ---
        JPanel pnlUst = new JPanel(new BorderLayout(10, 10));
        pnlUst.setOpaque(false);
        pnlUst.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY));

        JPanel pnlUstSol = new JPanel(new GridLayout(2, 1));
        pnlUstSol.setOpaque(false);
        pnlUstSol.add(new JLabel("<html><font size='5'><b>SİPARİŞ #" + id + " | " + masa + "</b></font></html>"));
        pnlUstSol.add(new JLabel("<html><font size='4' color='#34495e'>Müşteri: " + musteri + " | Garson: " + siparisiAlan + "</font></html>"));
        pnlUst.add(pnlUstSol, BorderLayout.WEST);

        // DURUM ETİKET BUTONU
        JPanel pnlUstSag = new JPanel(new GridLayout(2, 1));
        pnlUstSag.setOpaque(false);
        
        JButton btnDurum = new JButton("<html><div style='text-align:right;'><b>" + durum + "</b></div></html>");
        btnDurum.setContentAreaFilled(false);
        btnDurum.setBorderPainted(false);
        btnDurum.setFocusPainted(false);
        btnDurum.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDurum.setFont(new Font("Arial", Font.BOLD, 16));
        
        if (durum.equals("BEKLEMEDE")) btnDurum.setForeground(new Color(211, 84, 0));
        else if (durum.equals("HAZIRLANIYOR")) btnDurum.setForeground(new Color(39, 174, 96));
        else btnDurum.setForeground(Color.BLACK);

        pnlUstSag.add(btnDurum);
        
        JLabel lblZaman = new JLabel("<html><div style='text-align:right;'><font size='4'><b>Süre:</b> 00:00</font></div></html>");
        if(durum.equals("YENI") || durum.equals("BEKLEMEDE") || durum.equals("HAZIRLANIYOR")) {
            zamanEtiketleri.put(lblZaman, tarih); 
        } else {
            lblZaman.setText("<html><div style='text-align:right;'><font size='4'><b>Bitti</b></font></div></html>");
        }
        pnlUstSag.add(lblZaman);
        pnlUst.add(pnlUstSag, BorderLayout.EAST);

        // --- 2. İÇERİK ---
        JEditorPane txtUrunler = new JEditorPane("text/html", icerikRenklendir(temizUrunler));
        txtUrunler.setEditable(false);
        txtUrunler.setOpaque(false);

        // ==========================================
        // DÜZELTME: SORU SORMADAN DİREKT ONAYLA
        // ==========================================
        Runnable direkOnayla = () -> {
            if (!durum.equals("HAZIR") && !durum.equals("ODENDI")) {
                durumDegistir(id, "HAZIR"); // Soru penceresi kaldırıldı!
            }
        };

        // Durum yazısına tıklandığında
        btnDurum.addActionListener(e -> direkOnayla.run());

        // Kartın herhangi bir yerine çift tıklandığında
        MouseAdapter ciftTik = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) direkOnayla.run();
            }
        };
        
        kartAna.addMouseListener(ciftTik);
        txtUrunler.addMouseListener(ciftTik);
        pnlUst.addMouseListener(ciftTik);

        kartAna.add(pnlUst, BorderLayout.NORTH);
        kartAna.add(txtUrunler, BorderLayout.CENTER);

        return kartAna;
    }

    private String icerikRenklendir(String yazi) {
        yazi = yazi.replaceAll("(?i)(olmasın|çıkart|hariç|yok|soğansız|istemiyor)", "<span style='color:red; font-weight:bold;'>$1</span>");
        yazi = yazi.replaceAll("(?i)(ekstra|bol|fazla|ekle|ilave)", "<span style='color:green; font-weight:bold;'>$1</span>");
        return "<html><div style='font-family: Arial; font-size: 18px; line-height: 1.5;'>" + yazi + "</div></html>";
    }

    private void durumDegistir(String id, String yeni) {
        new Thread(() -> {
            anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + id + "|" + yeni);
            SwingUtilities.invokeLater(this::verileriYenile);
        }).start();
    }

    private void zamanlariGuncelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date suan = new Date();
        for (Map.Entry<JLabel, String> e : zamanEtiketleri.entrySet()) {
            try {
                Date d = sdf.parse(e.getValue());
                long diff = Math.max(0, suan.getTime() - d.getTime());
                long s = (diff / 1000) % 60; long m = (diff / (1000 * 60)) % 60;
                e.getKey().setText(String.format("<html><div style='text-align:right;'><font size='4'><b>Süre:</b> %02d:%02d</font></div></html>", m, s));
            } catch (Exception ex) { e.getKey().setText("..."); }
        }
    }
}