

import javax.swing.*;
import java.awt.*;

// Fişleri ve ID'lerini güvenle tutacağımız Özel Sınıf Kapsülü
class SiparisItem {
    int orderId;
    String htmlData;

    public SiparisItem(int orderId, String htmlData) {
        this.orderId = orderId;
        this.htmlData = htmlData;
    }

    @Override
    public String toString() {
        return htmlData; // Ekranda HTML fişin içeriğinin görünmesini sağlar
    }
}

public class MutfakModulu extends JPanel {
    private DefaultListModel<SiparisItem> modelGecmis = new DefaultListModel<>();
    private DefaultListModel<SiparisItem> modelBekleyen = new DefaultListModel<>();
    private DefaultListModel<SiparisItem> modelHazirlanan = new DefaultListModel<>();
    
    private JList<SiparisItem> listGecmis, listBekleyen, listHazirlanan;
    private PersonelPaneli anaPanel;

    public MutfakModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 3'lü Sütun Yapısı (Geçmiş | Bekleyen | Hazırlanan)
        JPanel pnlListeler = new JPanel(new GridLayout(1, 3, 15, 0));

        listGecmis = new JList<>(modelGecmis);
        pnlListeler.add(listeOlustur("📜 Geçmiş Siparişler (Bugün)", listGecmis, new Color(245, 245, 245)));

        listBekleyen = new JList<>(modelBekleyen);
        pnlListeler.add(listeOlustur("🔔 Bekleyen Siparişler", listBekleyen, new Color(255, 235, 235)));

        listHazirlanan = new JList<>(modelHazirlanan);
        pnlListeler.add(listeOlustur("🍳 Şu An Hazırlananlar", listHazirlanan, new Color(255, 248, 220)));

        add(pnlListeler, BorderLayout.CENTER);

        // Alt Butonlar ve Kontroller
        JPanel pnlButonlar = new JPanel(new GridLayout(1, 3, 15, 10));
        
        JLabel lblBos = new JLabel("<html><center>Geçmiş siparişler salt okunurdur.<br><i>Sadece görüntüleme amaçlıdır.</i></center></html>", SwingConstants.CENTER);
        lblBos.setForeground(Color.GRAY);
        
        JButton btnHazirla = new JButton("Seçiliyi Hazırlamaya Başla ➔");
        btnHazirla.setBackground(new Color(230, 126, 34));
        btnHazirla.setForeground(Color.WHITE);
        btnHazirla.setFont(new Font("Arial", Font.BOLD, 15));
        btnHazirla.setFocusPainted(false);
        btnHazirla.setPreferredSize(new Dimension(0, 50));

        JButton btnTamamla = new JButton("✔ SİPARİŞ HAZIR! (Onayla)");
        btnTamamla.setBackground(new Color(39, 174, 96));
        btnTamamla.setForeground(Color.WHITE);
        btnTamamla.setFont(new Font("Arial", Font.BOLD, 15));
        btnTamamla.setFocusPainted(false);

        pnlButonlar.add(lblBos);
        pnlButonlar.add(btnHazirla);
        pnlButonlar.add(btnTamamla);
        add(pnlButonlar, BorderLayout.SOUTH);

        // --- ONAYLI AKSİYONLAR ---
        btnHazirla.addActionListener(e -> {
            durumGuncelle(
                listBekleyen, 
                "HAZIRLANIYOR", 
                "Bu siparişi 'Hazırlanıyor' aşamasına almak istiyor musunuz?", 
                "Hazırlanıyor Onayı"
            );
        });

        btnTamamla.addActionListener(e -> {
            durumGuncelle(
                listHazirlanan, 
                "HAZIR", 
                "Sipariş hazırlandı olarak işaretlenecek ve Garson/Kasa ekranında masa rengi 'SARI' (Teslim Edildi) olacak.\nOnaylıyor musunuz?", 
                "Sipariş Hazır Onayı"
            );
        });
    }

    private JPanel listeOlustur(String baslik, JList<SiparisItem> list, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true), 
            baslik, 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            new Font("Arial", Font.BOLD, 15),
            Color.DARK_GRAY
        ));
        
        list.setBackground(bg);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new MutfakListeRenderer(bg));
        
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder()); 
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    public void verileriYenile() {
        String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("MUTFAK_SIPARIS_GETIR_FULL");
        
        if (cevap != null && cevap.startsWith("MUTFAK_FULL_VERI|")) {
            SwingUtilities.invokeLater(() -> {
                // Seçimleri hafızada tut
                int seciliBekleyen = listBekleyen.getSelectedIndex();
                int seciliHazirlanan = listHazirlanan.getSelectedIndex();

                modelGecmis.clear(); modelBekleyen.clear(); modelHazirlanan.clear();
                
                if(cevap.length() > 17) {
                    String[] siparisler = cevap.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        
                        String[] detay = s.split("~_~"); 
                        if (detay.length == 4) {
                            try {
                                int id = Integer.parseInt(detay[0]);
                                SiparisItem item = new SiparisItem(id, detay[3]);
                                
                                switch (detay[2]) {
                                    case "BEKLEMEDE": modelBekleyen.addElement(item); break;
                                    case "HAZIRLANIYOR": modelHazirlanan.addElement(item); break;
                                    case "HAZIR": modelGecmis.addElement(item); break;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // Eski seçimleri geri yükle
                if(seciliBekleyen < modelBekleyen.getSize()) listBekleyen.setSelectedIndex(seciliBekleyen);
                if(seciliHazirlanan < modelHazirlanan.getSize()) listHazirlanan.setSelectedIndex(seciliHazirlanan);
            });
        }
    }

    // --- ONAY PENCERELİ GÜNCELLEME METODU ---
    private void durumGuncelle(JList<SiparisItem> liste, String yeniDurum, String mesaj, String baslik) {
        SiparisItem seciliItem = liste.getSelectedValue();
        
        if (seciliItem != null) {
            // Mutfak personeline emin misin diye soruyoruz
            int secim = JOptionPane.showConfirmDialog(this, mesaj, baslik, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            
            if (secim == JOptionPane.YES_OPTION) {
                int id = seciliItem.orderId; 
                String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + id + "|" + yeniDurum);
                
                if (cvp != null && cvp.startsWith("BAŞARILI")) {
                    verileriYenile(); // Başarılıysa ekranı anında yenile ve fişi diğer sütuna kaydır
                } else {
                    JOptionPane.showMessageDialog(this, "Güncelleme başarısız: " + cvp, "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Lütfen işlem yapmak istediğiniz siparişi tablodan seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
    }

    // TASARIM MOTORU
    class MutfakListeRenderer extends DefaultListCellRenderer {
        private Color normalBg;
        private Color selectedBg = new Color(173, 216, 230); // Soft Açık Mavi

        public MutfakListeRenderer(Color normalBg) {
            this.normalBg = normalBg;
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            SiparisItem item = (SiparisItem) value;
            String htmlText = item.htmlData;
            
            if(htmlText != null && htmlText.startsWith("<html>")) {
                htmlText = "<html><div style='padding: 10px; font-family: sans-serif; font-size: 13px;'>" + htmlText.substring(6) + "</div></html>";
            }

            JLabel label = (JLabel) super.getListCellRendererComponent(list, htmlText, index, isSelected, cellHasFocus);
            
            if (isSelected) {
                label.setBackground(selectedBg);
                label.setForeground(Color.BLACK); 
            } else {
                label.setBackground(normalBg);
                label.setForeground(Color.BLACK);
            }

            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, Color.GRAY), 
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));

            return label;
        }
    }
}