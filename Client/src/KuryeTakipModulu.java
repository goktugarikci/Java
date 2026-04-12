import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KuryeTakipModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultListModel<String> kuryeListModel;
    private JList<String> kuryeListesi;
    private JPanel pnlSiparisler;
    
    private JButton btnKuryeAksiyon; // Dinamik Giriş/Çıkış Butonu
    private String mevcutKuryeDurumu = "BEKLIYOR";

    public KuryeTakipModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);

        // ==========================================
        // SOL SÜTUN: KURYE LİSTESİ
        // ==========================================
        JPanel pnlSol = new JPanel(new BorderLayout(5, 5));
        pnlSol.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Aktif Kuryeler", 0, 0, new Font("Arial", Font.BOLD, 14)));

        kuryeListModel = new DefaultListModel<>();
        kuryeListesi = new JList<>(kuryeListModel);
        kuryeListesi.setFont(new Font("Arial", Font.BOLD, 16));
        kuryeListesi.setFixedCellHeight(45);
        
        pnlSol.add(new JScrollPane(kuryeListesi), BorderLayout.CENTER);

        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.addActionListener(e -> kuryeleriGetir());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        // ==========================================
        // SAĞ SÜTUN: DETAYLI SİPARİŞ LİSTESİ VE GİRİŞ/ÇIKIŞ
        // ==========================================
        JPanel pnlSagAna = new JPanel(new BorderLayout(5, 5));

        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(Color.WHITE);

        JScrollPane scrollSiparisler = new JScrollPane(pnlSiparisler);
        scrollSiparisler.setBorder(BorderFactory.createTitledBorder("Kurye Hareketleri ve Sipariş Detayları"));
        
        pnlSagAna.add(scrollSiparisler, BorderLayout.CENTER);

        // --- GİRİŞ / ÇIKIŞ BUTON PANELİ ---
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
                siparisleriGetir(kuryeListesi.getSelectedValue());
            }
        });

        kuryeleriGetir();
    }

    public void kuryeleriGetir() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            SwingUtilities.invokeLater(() -> {
                kuryeListModel.clear();
                if (cvp != null && cvp.startsWith("KURYE_LISTESI|")) {
                    String[] kuryeler = cvp.split("\\|");
                    for (int i = 1; i < kuryeler.length; i++) {
                        if (!kuryeler[i].trim().isEmpty()) kuryeListModel.addElement(kuryeler[i]);
                    }
                }
            });
        }).start();
    }

    private void siparisleriGetir(String kuryeAdi) {
        new Thread(() -> {
            // Sunucudan kuryenin aktif ve geçmiş tüm detaylarını istiyoruz
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + kuryeAdi);
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();
                boolean yoldaMi = false;
                boolean bekleyenVarMi = false;

                if (cvp != null && cvp.startsWith("KURYE_TAKIP_VERI|") && cvp.length() > 17) {
                    String[] siparisler = cvp.substring(17).split("\\|\\|\\|");
                    
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~", -1); // id, musteri, durum, html
                        if (d.length >= 4) {
                            String durum = d[2];
                            if (durum.equals("YOLA_CIKTI")) yoldaMi = true;
                            if (durum.equals("HAZIR") || durum.equals("BEKLIYOR")) bekleyenVarMi = true;
                            
                            pnlSiparisler.add(detayliSiparisKarti(d[0], d[1], durum, d[3]));
                            pnlSiparisler.add(Box.createVerticalStrut(10));
                        }
                    }
                }

                // --- GİRİŞ / ÇIKIŞ MANTIĞI ---
                if (yoldaMi) {
                    mevcutKuryeDurumu = "YOLDA";
                    btnKuryeAksiyon.setText("🏠 GİRİŞ YAP (Merkeze Döndü)");
                    btnKuryeAksiyon.setBackground(new Color(230, 126, 34)); // Turuncu
                    btnKuryeAksiyon.setForeground(Color.WHITE);
                    btnKuryeAksiyon.setEnabled(true);
                } else if (bekleyenVarMi) {
                    mevcutKuryeDurumu = "MUSAIT";
                    btnKuryeAksiyon.setText("🛵 ÇIKIŞ YAP (Yola Çıkart)");
                    btnKuryeAksiyon.setBackground(new Color(46, 204, 113)); // Yeşil
                    btnKuryeAksiyon.setForeground(Color.WHITE);
                    btnKuryeAksiyon.setEnabled(true);
                } else {
                    btnKuryeAksiyon.setText("Sipariş Bekleniyor");
                    btnKuryeAksiyon.setBackground(Color.LIGHT_GRAY);
                    btnKuryeAksiyon.setEnabled(false);
                }

                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private void kuryeAksiyonuGerceklestir() {
        String kurye = kuryeListesi.getSelectedValue();
        if (kurye == null) return;

        String komut = mevcutKuryeDurumu.equals("YOLDA") ? "KURYE_MERKEZE_DONDU|" : "KURYE_TOPLU_YOLA_CIKAR|";
        String mesaj = mevcutKuryeDurumu.equals("YOLDA") ? "Kurye merkeze giriş yapsın mı?" : "Kurye yola çıksın mı?";

        if (JOptionPane.showConfirmDialog(this, mesaj, "Kurye Hareket Onayı", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut + kurye);
            JOptionPane.showMessageDialog(this, cvp);
            siparisleriGetir(kurye);
        }
    }

    private JPanel detayliSiparisKarti(String id, String musteri, String durum, String html) {
        JPanel kart = new JPanel(new BorderLayout(10, 5));
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        kart.setBackground(durum.equals("YOLA_CIKTI") ? new Color(253, 235, 208) : Color.WHITE);
        kart.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));

        // Ödeme Tipi ve Çıkış Saatini HTML içinden çekiyoruz
        String odemeTipi = "Belirtilmedi";
        String saat = "00:00";
        
        Matcher mOdeme = Pattern.compile("Ödeme:\\s*<b>(.*?)</b>").matcher(html);
        if (mOdeme.find()) odemeTipi = mOdeme.group(1);
        
        Matcher mSaat = Pattern.compile("SAAT:\\s*<b>(.*?)</b>").matcher(html);
        if (mSaat.find()) saat = mSaat.group(1);

        JLabel lblSol = new JLabel("<html><font size='4'><b>#" + id + " " + musteri + "</b></font><br>" +
                                   "<font color='blue'>Ödeme: " + odemeTipi + "</font></html>");
        
        JLabel lblSag = new JLabel("<html><div style='text-align: right;'><font size='4' color='red'><b>" + durum + "</b></font><br>" +
                                   "Saat: " + saat + "</div></html>");

        kart.add(lblSol, BorderLayout.WEST);
        kart.add(lblSag, BorderLayout.EAST);
        
        return kart;
    }
}