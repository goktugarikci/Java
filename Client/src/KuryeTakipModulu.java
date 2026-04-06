

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KuryeTakipModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultListModel<String> kuryeListModel;
    private JList<String> kuryeListesi;
    private JPanel pnlSiparisler;

    public KuryeTakipModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);

        // ==========================================
        // ORTA SÜTUN: KURYE LİSTESİ
        // ==========================================
        JPanel pnlSol = new JPanel(new BorderLayout(5, 5));
        pnlSol.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Kişi Listesi (Aktif Kuryeler)", 0, 0, new Font("Arial", Font.BOLD, 14)));

        kuryeListModel = new DefaultListModel<>();
        kuryeListesi = new JList<>(kuryeListModel);
        kuryeListesi.setFont(new Font("Arial", Font.BOLD, 16));
        kuryeListesi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kuryeListesi.setFixedCellHeight(45);
        kuryeListesi.setBackground(new Color(236, 240, 241));
        
        pnlSol.add(new JScrollPane(kuryeListesi), BorderLayout.CENTER);

        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.setBackground(new Color(52, 152, 219));
        btnYenile.setForeground(Color.WHITE);
        btnYenile.setFont(new Font("Arial", Font.BOLD, 14));
        btnYenile.addActionListener(e -> kuryeleriGetir());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        // ==========================================
        // SAĞ SÜTUN: MÜŞTERİ VE SİPARİŞ DETAYLARI
        // ==========================================
        pnlSiparisler = new JPanel();
        pnlSiparisler.setLayout(new BoxLayout(pnlSiparisler, BoxLayout.Y_AXIS));
        pnlSiparisler.setBackground(Color.WHITE);

        JScrollPane scrollSiparisler = new JScrollPane(pnlSiparisler);
        scrollSiparisler.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96), 2), "Müşteri Adresi ve Fiş Detayı", 0, 0, new Font("Arial", Font.BOLD, 14)));
        scrollSiparisler.getVerticalScrollBar().setUnitIncrement(16);
        
        splitPane.setRightComponent(scrollSiparisler);
        add(splitPane, BorderLayout.CENTER);

        // Kurye seçildiğinde siparişlerini çek
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
                pnlSiparisler.removeAll();
                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
                
                if (cvp != null && cvp.startsWith("KURYE_LISTESI|")) {
                    String[] kuryeler = cvp.split("\\|");
                    for (int i = 1; i < kuryeler.length; i++) {
                        if (!kuryeler[i].trim().isEmpty() && !kuryeler[i].equals("Sistemde Kayıtlı Motorcu Yok")) {
                            kuryeListModel.addElement(kuryeler[i]);
                        }
                    }
                }
                if(kuryeListModel.isEmpty()) {
                    kuryeListModel.addElement("Aktif Kurye Bulunamadı");
                    kuryeListesi.setEnabled(false);
                } else {
                    kuryeListesi.setEnabled(true);
                }
            });
        }).start();
    }

    private void siparisleriGetir(String kuryeAdi) {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_TAKIP_SIPARIS_GETIR|" + kuryeAdi);
            SwingUtilities.invokeLater(() -> {
                pnlSiparisler.removeAll();
                if (cvp != null && cvp.startsWith("KURYE_TAKIP_VERI|") && cvp.length() > 17) {
                    String[] siparisler = cvp.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~", -1);
                        if (d.length >= 4) {
                            pnlSiparisler.add(siparisKartiOlustur(d[0], d[1], d[2], d[3]));
                            pnlSiparisler.add(Box.createVerticalStrut(10));
                        }
                    }
                } else {
                    JLabel lblBos = new JLabel("  Bu kuryeye atanmış aktif bir sipariş bulunamadı.");
                    lblBos.setFont(new Font("Arial", Font.ITALIC, 16));
                    lblBos.setForeground(Color.GRAY);
                    pnlSiparisler.add(lblBos);
                }
                pnlSiparisler.revalidate();
                pnlSiparisler.repaint();
            });
        }).start();
    }

    private JPanel siparisKartiOlustur(String orderId, String musteri, String durum, String html) {
        JPanel kart = new JPanel(new GridLayout(4, 1, 5, 5));
        kart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        kart.setMaximumSize(new Dimension(2000, 160)); // Genişliği esnek tutuyoruz

        // Duruma göre arkaplan rengi (Senin tasarımındaki gibi)
        if (durum.equals("YOLA_CIKTI")) kart.setBackground(new Color(208, 236, 231)); // Açık turkuaz
        else if (durum.equals("TESLIM_EDILDI") || durum.equals("ODENDI")) kart.setBackground(new Color(235, 245, 251)); // Teslim edilenler açık gri/mavi
        else kart.setBackground(new Color(253, 235, 208)); // Bekleyenler

        // Fişin içinden (HTML) Regex ile Adres ve Telefon çekme
        String telefon = "Belirtilmedi", adres = "Belirtilmedi", cikisSaati = "Henüz Çıkmadı";
        try {
            Matcher mTel = Pattern.compile("Müşteri:.*?\\((.*?)\\)").matcher(html);
            if (mTel.find()) telefon = mTel.group(1).trim();

            Matcher mAdres = Pattern.compile("Adres:\\s*(.*?)<").matcher(html);
            if (mAdres.find()) adres = mAdres.group(1).trim();

            Matcher mSaat = Pattern.compile("ÇIKIŞ SAATİ:\\s*(.*?)<").matcher(html);
            if (mSaat.find()) cikisSaati = mSaat.group(1).trim();
        } catch (Exception e) {}

        JLabel lblAdDurum = new JLabel("<html><font size='5'><b>Müşteri Adı:</b> " + musteri + "</font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>Durum:</b> <font color='#c0392b'>" + durum + "</font></html>");
        JLabel lblAdres = new JLabel("<html><font size='4'><b>Müşteri Adresi:</b> " + adres + "</font></html>");
        JLabel lblTel = new JLabel("<html><font size='4'><b>Müşteri Telefonu:</b> " + telefon + "</font></html>");
        JLabel lblZaman = new JLabel("<html><font size='4'><b>Sipariş No:</b> #" + orderId + " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>Çıkış Saati:</b> " + cikisSaati + "</font></html>");

        kart.add(lblAdDurum);
        kart.add(lblAdres);
        kart.add(lblTel);
        kart.add(lblZaman);

        return kart;
    }
}