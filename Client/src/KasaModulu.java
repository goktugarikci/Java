

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class KasaModulu extends JPanel {
    private PersonelPaneli anaPanel;
    
    private DefaultTableModel aktifTableModel;
    private JTable aktifTablo;
    
    private DefaultTableModel gecmisTableModel;
    private JTable gecmisTablo;
    
    private JEditorPane txtFisDetay; 
    private JPanel pnlButonlar;
    
    // Aksiyon Butonları
    private JButton btnYolaCikti, btnIptal, btnNakit, btnKrediKarti;
    
    private String seciliSiparisId = "";
    private String seciliTur = "";

    public KasaModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);

        // ==========================================
        // SOL TARAFTAKİ SEKMELER (AKTİF VE GEÇMİŞ)
        // ==========================================
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        // 1. AKTİF SİPARİŞLER SEKMESİ
        aktifTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Mevcut Durum"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        aktifTablo = new JTable(aktifTableModel);
        aktifTablo.setRowHeight(35);
        aktifTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        aktifTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel pnlAktif = new JPanel(new BorderLayout());
        pnlAktif.add(new JScrollPane(aktifTablo), BorderLayout.CENTER);
        tabbedPane.addTab("🟢 Aktif İşlemler", pnlAktif);

        // 2. GEÇMİŞ SİPARİŞLER SEKMESİ (ARŞİV)
        gecmisTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Son Durum"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        gecmisTablo = new JTable(gecmisTableModel);
        gecmisTablo.setRowHeight(35);
        gecmisTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        gecmisTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel pnlGecmis = new JPanel(new BorderLayout());
        pnlGecmis.add(new JScrollPane(gecmisTablo), BorderLayout.CENTER);
        tabbedPane.addTab("📜 Geçmiş (Arşiv)", pnlGecmis);

        splitPane.setLeftComponent(tabbedPane);

        // ==========================================
        // SAĞ TARAF: FİŞ VE AKILLI BUTONLAR
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10));
        pnlSag.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Seçili Sipariş Detayı ve İşlemler", 0, 0, new Font("Arial", Font.BOLD, 14)));

        txtFisDetay = new JEditorPane();
        txtFisDetay.setContentType("text/html");
        txtFisDetay.setEditable(false);
        txtFisDetay.setBackground(new Color(255, 255, 240)); 
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        // DİNAMİK BUTON PANELİ (Siparişe göre butonlar değişecek)
        pnlButonlar = new JPanel(new GridLayout(1, 3, 10, 10)); 
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        btnYolaCikti = new JButton("🛵 Gönder (Yola Çıktı)");
        btnYolaCikti.setBackground(new Color(52, 152, 219)); btnYolaCikti.setForeground(Color.WHITE); btnYolaCikti.setFont(new Font("Arial", Font.BOLD, 14));
        
        btnIptal = new JButton("❌ İptal Et");
        btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE); btnIptal.setFont(new Font("Arial", Font.BOLD, 14));

        btnNakit = new JButton("💵 Nakit Al & Kapat");
        btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE); btnNakit.setFont(new Font("Arial", Font.BOLD, 14));

        btnKrediKarti = new JButton("💳 K.Kartı & Kapat");
        btnKrediKarti.setBackground(new Color(41, 128, 185)); btnKrediKarti.setForeground(Color.WHITE); btnKrediKarti.setFont(new Font("Arial", Font.BOLD, 14));

        pnlSag.add(pnlButonlar, BorderLayout.SOUTH);
        splitPane.setRightComponent(pnlSag);
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // TABLO TIKLAMA DİNLEYİCİLERİ
        // ==========================================
        aktifTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && aktifTablo.getSelectedRow() != -1) {
                gecmisTablo.clearSelection(); // Diğer tablodaki seçimi iptal et
                
                seciliSiparisId = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 0).toString();
                seciliTur = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 1).toString();
                String fisHtml = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 4).toString();
                
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px;'>" + fisHtml + "</div>");
                butonlariDuzenle(seciliTur, true); // Aktif siparişler için butonları diz
            }
        });

        gecmisTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && gecmisTablo.getSelectedRow() != -1) {
                aktifTablo.clearSelection(); 
                
                seciliSiparisId = gecmisTableModel.getValueAt(gecmisTablo.getSelectedRow(), 0).toString();
                seciliTur = gecmisTableModel.getValueAt(gecmisTablo.getSelectedRow(), 1).toString();
                String fisHtml = gecmisTableModel.getValueAt(gecmisTablo.getSelectedRow(), 4).toString();
                
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px; opacity: 0.6;'>" + fisHtml + "</div>");
                butonlariDuzenle(seciliTur, false); // Geçmişte butonlar GİZLENİR (Salt Okunur)
            }
        });

        tabbedPane.addChangeListener(e -> {
            aktifTablo.clearSelection();
            gecmisTablo.clearSelection();
            txtFisDetay.setText("");
            butonlariDuzenle("", false);
        });

        // --- BUTON AKSİYONLARI ---
        btnYolaCikti.addActionListener(e -> durumGuncelle("YOLA_CIKTI"));
        btnIptal.addActionListener(e -> durumGuncelle("IPTAL"));
        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKrediKarti.addActionListener(e -> odemeAl("Kredi Kartı"));
    }

    // Seçilen fişin türüne göre (Masa mı, Eve Servis mi) butonları yerleştirir
    private void butonlariDuzenle(String tur, boolean aktifMi) {
        pnlButonlar.removeAll();
        
        if (aktifMi) {
            // Eğer EVE SERVİS veya PAKET ise sadece Gönder ve İptal çıkar
            if (tur.contains("Eve") || tur.contains("Paket") || tur.contains("EVE")) {
                pnlButonlar.add(btnYolaCikti);
                pnlButonlar.add(btnIptal);
            } 
            // Eğer MASA ise Ödeme Alma ve İptal çıkar
            else {
                pnlButonlar.add(btnNakit);
                pnlButonlar.add(btnKrediKarti);
                pnlButonlar.add(btnIptal);
            }
        }
        
        pnlButonlar.revalidate();
        pnlButonlar.repaint();
    }

    public void verileriYenile() {
        // 1. AKTİF SİPARİŞLERİ YÜKLE
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                aktifTableModel.setColumnCount(4); 
                aktifTableModel.setRowCount(0);
                aktifTableModel.addColumn("HTML_GIZLI"); 

                if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
                    String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~"); 
                        if (d.length >= 5) aktifTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                    }
                }
                aktifTablo.getColumnModel().getColumn(4).setMinWidth(0);
                aktifTablo.getColumnModel().getColumn(4).setMaxWidth(0);
                aktifTablo.getColumnModel().getColumn(4).setWidth(0);
            });
        }).start();

        // 2. GEÇMİŞ SİPARİŞLERİ YÜKLE
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KASA_GECMIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                gecmisTableModel.setColumnCount(4); 
                gecmisTableModel.setRowCount(0);
                gecmisTableModel.addColumn("HTML_GIZLI"); 

                if (cevap != null && cevap.startsWith("KASA_GECMIS_VERI|") && cevap.length() > 17) {
                    String[] siparisler = cevap.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~"); 
                        if (d.length >= 5) gecmisTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                    }
                }
                gecmisTablo.getColumnModel().getColumn(4).setMinWidth(0);
                gecmisTablo.getColumnModel().getColumn(4).setMaxWidth(0);
                gecmisTablo.getColumnModel().getColumn(4).setWidth(0);
            });
        }).start();
        
        txtFisDetay.setText("");
        butonlariDuzenle("", false);
    }

    private void durumGuncelle(String yeniDurum) {
        if(seciliSiparisId.isEmpty()) { JOptionPane.showMessageDialog(this, "Önce bir sipariş seçin!"); return; }
        
        int onay = JOptionPane.showConfirmDialog(this, "Bu siparişi iptal etmek veya göndermek istediğinize emin misiniz?", "İşlem Onayı", JOptionPane.YES_NO_OPTION);
        if(onay == JOptionPane.YES_OPTION) {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + seciliSiparisId + "|" + yeniDurum);
            JOptionPane.showMessageDialog(this, cvp);
            
            // Eğer işlem yapılan şey bir "Masa" ise masayı da tamamen griye dönüştür ve sistemden kapat
            if ((yeniDurum.equals("IPTAL") || yeniDurum.equals("YOLA_CIKTI")) && !seciliTur.contains("Müşteri")) {
                anaPanel.masayiSifirla(seciliTur);
            }
            verileriYenile();
        }
    }

    private void odemeAl(String odemeTuru) {
        if(seciliSiparisId.isEmpty()) { JOptionPane.showMessageDialog(this, "Önce ödenecek siparişi seçin!"); return; }
        
        int onay = JOptionPane.showConfirmDialog(this, "Sipariş " + odemeTuru + " olarak kapatılacak. Onaylıyor musunuz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION);
        if (onay == JOptionPane.YES_OPTION) {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + odemeTuru);
            JOptionPane.showMessageDialog(this, cvp);
            
            // Masayı temizle
            if (!seciliTur.contains("Müşteri")) {
                anaPanel.masayiSifirla(seciliTur); 
            }
            verileriYenile();
        }
    }
}