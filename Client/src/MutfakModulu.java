package src;

import javax.swing.*;
import java.awt.*;

public class MutfakModulu extends JPanel {
    private DefaultListModel<String> modelGecmis = new DefaultListModel<>();
    private DefaultListModel<String> modelBekleyen = new DefaultListModel<>();
    private DefaultListModel<String> modelHazirlanan = new DefaultListModel<>();
    
    private JList<String> listGecmis, listBekleyen, listHazirlanan;
    private PersonelPaneli anaPanel;

    public MutfakModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 3'lü Sütun Yapısı (Geçmiş | Bekleyen | Hazırlanan)
        JPanel pnlListeler = new JPanel(new GridLayout(1, 3, 15, 0));

        // 1. Geçmiş Listesi (Gri Tema)
        listGecmis = new JList<>(modelGecmis);
        pnlListeler.add(listeOlustur("📜 Geçmiş Siparişler (Bugün)", listGecmis, new Color(245, 245, 245)));

        // 2. Bekleyen Listesi (Kırmızı/Pembe Tema)
        listBekleyen = new JList<>(modelBekleyen);
        pnlListeler.add(listeOlustur("🔔 Bekleyen Siparişler", listBekleyen, new Color(255, 235, 235)));

        // 3. Hazırlanan Listesi (Sarı Tema)
        listHazirlanan = new JList<>(modelHazirlanan);
        pnlListeler.add(listeOlustur("🍳 Şu An Hazırlananlar", listHazirlanan, new Color(255, 248, 220)));

        add(pnlListeler, BorderLayout.CENTER);

        // Alt Butonlar ve Kontroller
        JPanel pnlButonlar = new JPanel(new GridLayout(1, 3, 15, 10));
        
        JLabel lblBos = new JLabel("Geçmiş siparişler salt okunurdur.", SwingConstants.CENTER);
        lblBos.setForeground(Color.GRAY);
        
        JButton btnHazirla = new JButton("Seçiliyi Hazırlamaya Başla ->");
        btnHazirla.setBackground(new Color(230, 126, 34));
        btnHazirla.setForeground(Color.WHITE);
        btnHazirla.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnTamamla = new JButton("✔ SİPARİŞ HAZIR! (Onayla)");
        btnTamamla.setBackground(new Color(39, 174, 96));
        btnTamamla.setForeground(Color.WHITE);
        btnTamamla.setFont(new Font("Arial", Font.BOLD, 14));

        pnlButonlar.add(lblBos);
        pnlButonlar.add(btnHazirla);
        pnlButonlar.add(btnTamamla);
        add(pnlButonlar, BorderLayout.SOUTH);

        // Aksiyonlar (Veritabanı Durum Güncellemesi)
        btnHazirla.addActionListener(e -> durumGuncelle(listBekleyen, "HAZIRLANIYOR"));
        btnTamamla.addActionListener(e -> durumGuncelle(listHazirlanan, "HAZIR"));
    }

    private JPanel listeOlustur(String baslik, JList<String> list, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), baslik, 
                // Başlık fontu kalınlaştırıldı
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));
        list.setBackground(bg);
        list.setCellRenderer(new MutfakListeRenderer(bg));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    // Ana PersonelPaneli tarafından tetiklenir (Timer ile 3 saniyede bir)
    public void verileriYenile() {
        String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("MUTFAK_SIPARIS_GETIR_FULL");
        
        if (cevap != null && cevap.startsWith("MUTFAK_FULL_VERI|")) {
            SwingUtilities.invokeLater(() -> {
                // Listeleri temizle ve veritabanından gelen en taze verilerle doldur
                modelGecmis.clear(); modelBekleyen.clear(); modelHazirlanan.clear();
                
                if(cevap.length() > 17) {
                    String[] siparisler = cevap.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        
                        String[] detay = s.split("~_~"); // Format: OrderID ~_~ MasaIsmi ~_~ Durum ~_~ FisHTML
                        if (detay.length == 4) {
                            String item = "" + detay[3];
                            
                            switch (detay[2]) {
                                case "BEKLEMEDE": 
                                    modelBekleyen.addElement(item); 
                                    break;
                                case "HAZIRLANIYOR": 
                                    modelHazirlanan.addElement(item); 
                                    break;
                                case "HAZIR": 
                                    modelGecmis.addElement(item); 
                                    break;
                            }
                        }
                    }
                }
            });
        }
    }

    private void durumGuncelle(JList<String> liste, String yeniDurum) {
        String secili = liste.getSelectedValue();
        if (secili != null) {
            String id = idGetir(secili);
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + id + "|" + yeniDurum);
            
            if (cvp != null && cvp.startsWith("BAŞARILI")) {
                verileriYenile(); // Başarılıysa ekranı anında yenile
                
                if (yeniDurum.equals("HAZIR")) {
                    JOptionPane.showMessageDialog(this, "Sipariş Hazırlandı!\nKasa ve Garson ekranlarına bilgi gönderildi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Güncelleme başarısız: " + cvp, "Hata", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Lütfen listeden işlem yapmak istediğiniz siparişi seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String idGetir(String htmlData) {
        int bas = htmlData.indexOf("");
        return htmlData.substring(bas, son);
    }

    // Fişlerin tasarımlı ve aralarına çizgi çekilmiş görünmesi için Render Motoru
    class MutfakListeRenderer extends DefaultListCellRenderer {
        private Color bg;
        public MutfakListeRenderer(Color bg) { this.bg = bg; }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!isSelected) {
                c.setBackground(bg);
            }
            setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.GRAY)); // Her fiş arasına kalın ayırıcı çizgi
            return c;
        }
    }
}