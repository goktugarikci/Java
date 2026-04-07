
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
    
    // Üst Sekme Butonları
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
        // 2. İÇERİK ALANI
        // ==========================================
        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(new Color(236, 240, 241)); 
        pnlSiparisler.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(pnlSiparisler);
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
                    if (!cvp.equals("MUTFAK_FULL_VERI|BOS")) {
                        String[] siparisler = cvp.substring(17).split("\\|\\|\\|");
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

                                boolean isGecmis = durum.equals("HAZIR");
                                
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
                    gosterBosMesaj("<html><font color='red'>Sunucuya Bağlanılamadı!</font></html>");
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
        pnlSiparisler.add(lblBos);
    }

    private void yeniSiparisBildirimi() {
        uyariEkraniAcik = true;
        Toolkit.getDefaultToolkit().beep(); 
        
        new Thread(() -> {
            JOptionPane.showOptionDialog(this, "Mutfak panosuna yeni bir sipariş düştü!", "🔔 YENİ SİPARİŞ",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, 
                new Object[]{"Siparişi Gördüm"}, "Siparişi Gördüm");
            
            uyariEkraniAcik = false; 
            verileriYenile(); 
        }).start();
    }

    // ==========================================
    // 3. TASARIMDAKİ YATAY KART DİZİLİMİ
    // ==========================================
    private JPanel siparisKartiOlustur(String id, String masa, String musteri, String html, String durum, String tarih) {
        JPanel kart = new JPanel(new BorderLayout(15, 10));
        kart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); 
        kart.setMinimumSize(new Dimension(600, 150));
        
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15) 
        ));

        if (durum.equals("YENI") || durum.equals("BEKLEMEDE")) {
            kart.setBackground(new Color(255, 235, 153)); 
        } else if (durum.equals("HAZIRLANIYOR")) {
            kart.setBackground(new Color(173, 235, 173)); 
        } else {
            kart.setBackground(new Color(220, 220, 220)); 
        }

        // ==========================================
        // İSİM BULAMAMA HATASINI ÇÖZEN YENİ MOTOR
        // ==========================================
        String siparisiAlan = "Bilinmiyor";
        String temizUrunler = html;
        
        try {
            // HTML etiketlerini geçici olarak silip "saf metin" elde ediyoruz ki kelime ararken takılmasın
            String safMetin = html.replace("<br>", "\n").replaceAll("<[^>]+>", " ");

            Matcher mMus = Pattern.compile("Müşteri:\\s*([^\\n]+)").matcher(safMetin);
            if (mMus.find() && !mMus.group(1).trim().isEmpty()) musteri = mMus.group(1).trim();
            
            Matcher mAla = Pattern.compile("Siparişi Alan:\\s*([^\\n]+)").matcher(safMetin);
            if (mAla.find() && !mAla.group(1).trim().isEmpty()) siparisiAlan = mAla.group(1).trim();
            
            Matcher mMas = Pattern.compile("(?i)(YENİ MASA|MASA|EVE SERVİS|PAKET|GEL-AL)[\\s\\(]*([^\\n\\)]*)").matcher(safMetin);
            if (mMas.find()) {
                masa = mMas.group(1).trim();
                String detay = mMas.group(2).trim();
                if (!detay.isEmpty() && !masa.contains(detay)) masa += " (" + detay + ")";
            }

            // Sipariş ürünlerinin olduğu kısmı temizleme
            temizUrunler = temizUrunler.replaceAll("(?i)(<[^>]+>)?\\[\\d{2}:\\d{2}\\].*?(?:<br>|\\n)", "");
            temizUrunler = temizUrunler.replaceAll("(?i)(<[^>]+>)?(Siparişi Alan:|Müşteri:|Tarih:|Adres:|Not:).*?(?:<br>|\\n)", "");
            
            String fiyatEtiketi = "<!" + "--PRICE"; 
            int fiyatIdx = temizUrunler.indexOf(fiyatEtiketi);
            if(fiyatIdx != -1) {
                temizUrunler = temizUrunler.substring(0, fiyatIdx);
            }
            
            temizUrunler = temizUrunler.replace("<hr>", "").replaceAll("(-{3,})", ""); 
            temizUrunler = temizUrunler.replace("<br><br>", "<br>").trim();
            if (temizUrunler.startsWith("<br>")) temizUrunler = temizUrunler.substring(4).trim();
        } catch (Exception e) {
            temizUrunler = "İçerik Yüklenemedi.";
        }

        // KARTIN SOL KISMI
        JPanel pnlSolBilgi = new JPanel(new BorderLayout(0, 5));
        pnlSolBilgi.setOpaque(false);
        
        JPanel pnlMusteriDetay = new JPanel(new GridLayout(2, 1));
        pnlMusteriDetay.setOpaque(false);
        pnlMusteriDetay.add(new JLabel("<html><font size='4'><b>Müşteri:</b> " + musteri + "</font></html>"));
        pnlMusteriDetay.add(new JLabel("<html><font size='4'><b>Siparişi Alan:</b> " + siparisiAlan + "</font></html>"));
        pnlSolBilgi.add(pnlMusteriDetay, BorderLayout.NORTH);
        
        JPanel pnlIcerikWrapper = new JPanel(new BorderLayout());
        pnlIcerikWrapper.setOpaque(false);
        pnlIcerikWrapper.add(new JLabel("<html><font size='4'><b>Sipariş İçeriği:</b></font></html>"), BorderLayout.NORTH);
        
        JEditorPane txtUrunler = new JEditorPane("text/html", icerikRenklendir(temizUrunler));
        txtUrunler.setEditable(false);
        txtUrunler.setOpaque(false);
        pnlIcerikWrapper.add(new JScrollPane(txtUrunler), BorderLayout.CENTER);
        
        pnlSolBilgi.add(pnlIcerikWrapper, BorderLayout.CENTER);

        // KARTIN SAĞ KISMI
        JPanel pnlSagBilgi = new JPanel(new GridLayout(3, 1));
        pnlSagBilgi.setOpaque(false);
        pnlSagBilgi.setPreferredSize(new Dimension(250, 0));
        
        String gorselDurum = durum.equals("YENI") || durum.equals("BEKLEMEDE") ? "Sıraya Alındı / Bekliyor" : 
                             durum.equals("HAZIRLANIYOR") ? "Hazırlanıyor" : "Tamamlandı";
                             
        pnlSagBilgi.add(new JLabel("<html><font size='4'><b>Durum:</b> " + gorselDurum + "</font></html>"));
        pnlSagBilgi.add(new JLabel("<html><font size='4'><b>Tip:</b> " + masa + "</font></html>"));
        
        JLabel lblZaman = new JLabel("<html><font size='4'><b>Süre:</b> 00:00:00</font></html>");
        
        if(!durum.equals("HAZIR")) {
            zamanEtiketleri.put(lblZaman, tarih); 
        } else {
            lblZaman.setText("<html><font size='4'><b>Süre:</b> Tamamlandı</font></html>");
        }
        
        pnlSagBilgi.add(lblZaman);

        kart.add(pnlSolBilgi, BorderLayout.CENTER);
        kart.add(pnlSagBilgi, BorderLayout.EAST);

        MouseAdapter ciftTiklama = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !durum.equals("HAZIR")) {
                    durumDegistir(id, "HAZIR");
                }
            }
        };
        kart.addMouseListener(ciftTiklama);
        txtUrunler.addMouseListener(ciftTiklama);

        return kart;
    }

    private String icerikRenklendir(String yazi) {
        yazi = yazi.replaceAll("(?i)(olmasın|çıkart|hariç|yok|soğansız|domatessiz|istemiyor|koyma)", "<span style='color:#c0392b; font-weight:bold;'>$1</span>");
        yazi = yazi.replaceAll("(?i)(ekstra|bol|fazla|çift|ekle|büyük|ilave)", "<span style='color:#27ae60; font-weight:bold;'>$1</span>");
        yazi = yazi.replaceAll("(?i)(kola|ayran|su\\b|şalgam|çay|fanta|meyve suyu|sprite|soda)", "<span style='color:#2980b9; font-weight:bold;'>🥤 $1</span>");
        return "<html><div style='font-family: Arial; font-size: 14px; line-height: 1.2;'>" + yazi + "</div></html>";
    }

    private void durumDegistir(String id, String yeni) {
        new Thread(() -> {
            anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + id + "|" + yeni);
            SwingUtilities.invokeLater(() -> verileriYenile());
        }).start();
    }

    private void zamanlariGuncelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date suan = new Date();
        for (Map.Entry<JLabel, String> e : zamanEtiketleri.entrySet()) {
            try {
                Date d = sdf.parse(e.getValue());
                long diff = suan.getTime() - d.getTime();
                if(diff < 0) diff = 0;
                long s = (diff / 1000) % 60; long m = (diff / (1000 * 60)) % 60; long h = (diff / (1000 * 60 * 60)) % 24;
                e.getKey().setText(String.format("<html><font size='4'><b>Süre:</b> %02d:%02d:%02d</font></html>", h, m, s));
            } catch (Exception ex) { 
                e.getKey().setText("<html><font size='4'><b>Süre:</b> Bekliyor..</font></html>"); 
            }
        }
    }
}