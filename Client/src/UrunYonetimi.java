

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UrunYonetimi extends JPanel {
    private AdminPaneli anaPanel;
    private DefaultTableModel katTableModel, urunTableModel;
    private JTable katTablo, urunTablo;
    private JComboBox<String> cbKatSecim;

    public UrunYonetimi(AdminPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 15));

        // ==========================================
        // 1. SEKMESİ: KATEGORİ YÖNETİMİ
        // ==========================================
        JPanel pnlKategori = new JPanel(new BorderLayout(10, 10));
        
        katTableModel = new DefaultTableModel(new String[]{"Kategori Adı", "Görsel Dosyası"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        katTablo = new JTable(katTableModel); 
        katTablo.setRowHeight(30); 
        katTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        pnlKategori.add(new JScrollPane(katTablo), BorderLayout.CENTER);

        JPanel pnlKatForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JTextField txtKatAd = new JTextField(15); 
        JTextField txtKatDesc = new JTextField(20);
        
        JButton btnKatEkle = new JButton("➕ Kategori Ekle"); 
        btnKatEkle.setBackground(new Color(39, 174, 96)); 
        btnKatEkle.setForeground(Color.WHITE);
        
        JButton btnKatSil = new JButton("❌ Kategori Sil"); 
        btnKatSil.setBackground(new Color(192, 57, 43)); 
        btnKatSil.setForeground(Color.WHITE);
        
        pnlKatForm.add(new JLabel("<html><b>Kategori Adı:</b></html>")); 
        pnlKatForm.add(txtKatAd); 
        pnlKatForm.add(new JLabel("<html><b>Açıklama:</b></html>")); 
        pnlKatForm.add(txtKatDesc); 
        pnlKatForm.add(btnKatEkle); 
        pnlKatForm.add(btnKatSil);
        
        pnlKategori.add(pnlKatForm, BorderLayout.SOUTH);
        tabbedPane.addTab("📂 Kategori Yönetimi", pnlKategori);

        // ==========================================
        // 2. SEKMESİ: ÜRÜN YÖNETİMİ VE SİHİRBAZ
        // ==========================================
        JPanel pnlUrun = new JPanel(new BorderLayout(10, 10));
        
        urunTableModel = new DefaultTableModel(new String[]{"İsim", "Kategori", "Açıklama", "Fiyat", "İçindekiler Listesi (Ekstra İçerikler)"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        urunTablo = new JTable(urunTableModel); 
        urunTablo.setRowHeight(35); 
        urunTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        urunTablo.getColumnModel().getColumn(4).setPreferredWidth(450); 
        
        JPanel pnlUrunUst = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cbKatSecim = new JComboBox<>(); 
        cbKatSecim.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton btnUrunGetir = new JButton("🔄 Seçili Kategorinin Ürünlerini Getir"); 
        btnUrunGetir.setBackground(new Color(52, 152, 219)); 
        btnUrunGetir.setForeground(Color.WHITE);
        
        pnlUrunUst.add(new JLabel("<html><b>Kategori Seç:</b></html>")); 
        pnlUrunUst.add(cbKatSecim); 
        pnlUrunUst.add(btnUrunGetir);
        
        pnlUrun.add(pnlUrunUst, BorderLayout.NORTH);
        pnlUrun.add(new JScrollPane(urunTablo), BorderLayout.CENTER);

        // --- YENİ ÜRÜN EKLEME FORMU (Görseldeki Tasarım) ---
        JPanel pnlUrunForm = new JPanel(new GridBagLayout()); 
        pnlUrunForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96), 2), "Yeni Ürün Ekle", 0, 0, new Font("Arial", Font.BOLD, 14)));
        GridBagConstraints gbc = new GridBagConstraints(); 
        gbc.insets = new Insets(10, 10, 10, 10); 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        
        JTextField txtUrunAd = new JTextField(15); 
        JTextField txtFiyat = new JTextField(8); 
        JTextField txtAciklama = new JTextField(35); 
        JTextField txtMalzemeler = new JTextField(30);
        
        // SİHİRBAZ BUTONU
        JButton btnSihirbaz = new JButton("📝 İçerik Sihirbazı");
        btnSihirbaz.setBackground(new Color(52, 73, 94));
        btnSihirbaz.setForeground(Color.WHITE);
        btnSihirbaz.addActionListener(e -> icerikOlusturucuModal(txtMalzemeler));

        JPanel pnlMalzemeAlan = new JPanel(new BorderLayout(5, 0));
        pnlMalzemeAlan.add(txtMalzemeler, BorderLayout.CENTER);
        pnlMalzemeAlan.add(btnSihirbaz, BorderLayout.EAST);

        // SATIR 1
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; pnlUrunForm.add(new JLabel("<html><b>İsim:</b></html>"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5; pnlUrunForm.add(txtUrunAd, gbc);
        gbc.gridx = 2; gbc.weightx = 0; pnlUrunForm.add(new JLabel("<html><b>Fiyat (TL):</b></html>"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5; pnlUrunForm.add(txtFiyat, gbc);
        
        // SATIR 2
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; pnlUrunForm.add(new JLabel("<html><b>Açıklama:</b></html>"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1; pnlUrunForm.add(txtAciklama, gbc);
        
        // SATIR 3
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0; pnlUrunForm.add(new JLabel("<html><b>İçindekiler Listesi:</b></html>"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1; pnlUrunForm.add(pnlMalzemeAlan, gbc);
        
        // SATIR 4
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3;
        JLabel lblBilgi = new JLabel("<html><i>Elle yazmak isterseniz Format: MalzemeAdı:1:0.0 -> Örn: <font color='red'><b>Domates:1:0.0, Ekstra Sos:0:5.5</b></font></i></html>"); 
        lblBilgi.setForeground(Color.GRAY);
        pnlUrunForm.add(lblBilgi, gbc);

        // SATIR 5 (BUTONLAR)
        JPanel pnlU3 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnUrunSil = new JButton("❌ Seçili Ürünü Sil"); 
        btnUrunSil.setBackground(new Color(192, 57, 43)); 
        btnUrunSil.setForeground(Color.WHITE); 
        btnUrunSil.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnUrunEkle = new JButton("➕ Ürünü Ekle"); 
        btnUrunEkle.setBackground(new Color(39, 174, 96)); 
        btnUrunEkle.setForeground(Color.WHITE); 
        btnUrunEkle.setFont(new Font("Arial", Font.BOLD, 14));
        
        pnlU3.add(btnUrunSil); 
        pnlU3.add(btnUrunEkle);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; 
        pnlUrunForm.add(pnlU3, gbc);
        
        pnlUrun.add(pnlUrunForm, BorderLayout.SOUTH);
        tabbedPane.addTab("🍔 Ürün Yönetimi", pnlUrun);

        add(tabbedPane, BorderLayout.CENTER);

        // ==========================================
        // DİNLEYİCİLER VE SUNUCU AKSİYONLARI
        // ==========================================
        
        btnKatEkle.addActionListener(e -> {
            String katAd = txtKatAd.getText().trim();
            String katDesc = txtKatDesc.getText().trim();
            if(katAd.isEmpty()) return;
            if(katDesc.isEmpty()) katDesc = "Açıklama Yok";
            
            String komut = "KAT_EKLE|" + katAd + "|" + katDesc + "|gorsel_yok.png";
            JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl(komut));
            
            txtKatAd.setText(""); txtKatDesc.setText(""); 
            verileriYenile(); 
        });

        btnKatSil.addActionListener(e -> {
            int row = katTablo.getSelectedRow();
            if(row != -1) {
                String seciliKat = katTableModel.getValueAt(row, 0).toString();
                JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("KAT_SIL|" + seciliKat));
                verileriYenile();
            } else {
                JOptionPane.showMessageDialog(this, "Önce tablodan silinecek kategoriyi seçin!");
            }
        });

        btnUrunGetir.addActionListener(e -> {
            if(cbKatSecim.getSelectedItem() != null) urunleriListele(cbKatSecim.getSelectedItem().toString());
        });

        cbKatSecim.addActionListener(e -> {
            if(cbKatSecim.getSelectedItem() != null) urunleriListele(cbKatSecim.getSelectedItem().toString());
        });

        btnUrunEkle.addActionListener(e -> {
            if(txtUrunAd.getText().trim().isEmpty() || txtFiyat.getText().trim().isEmpty() || cbKatSecim.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "İsim, Fiyat ve Kategori boş bırakılamaz!", "Hata", JOptionPane.ERROR_MESSAGE); 
                return;
            }
            String kat = cbKatSecim.getSelectedItem().toString();
            String urunAd = txtUrunAd.getText().trim();
            String fiyat = txtFiyat.getText().trim().replace(",","."); 
            String aciklama = txtAciklama.getText().trim().isEmpty() ? "Açıklama Yok" : txtAciklama.getText().trim();
            String mlz = txtMalzemeler.getText().trim().isEmpty() ? "null" : txtMalzemeler.getText().trim();
            
            String komut = "URUN_EKLE_DETAYLI|" + kat + "|" + urunAd + "|" + fiyat + "|" + aciklama + "|gorsel_yok.png|" + mlz;
            
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut);
            JOptionPane.showMessageDialog(this, cvp);
            
            if (cvp.startsWith("BAŞARILI")) {
                txtUrunAd.setText(""); txtFiyat.setText(""); txtAciklama.setText(""); txtMalzemeler.setText(""); 
                urunleriListele(kat);
            }
        });

        btnUrunSil.addActionListener(e -> {
            int row = urunTablo.getSelectedRow();
            if(row != -1) {
                String seciliUrunAdi = urunTableModel.getValueAt(row, 0).toString();
                JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("URUN_SIL|" + seciliUrunAdi));
                if(cbKatSecim.getSelectedItem() != null) urunleriListele(cbKatSecim.getSelectedItem().toString());
            } else {
                JOptionPane.showMessageDialog(this, "Önce tablodan silinecek ürünü seçin!");
            }
        });
    }

    // ==========================================
    // İÇERİK OLUŞTURUCU MODAL (SİHİRBAZ)
    // ==========================================
    private void icerikOlusturucuModal(JTextField txtMalzemeler) {
        JDialog d = new JDialog(anaPanel, "İçerik / Ekstra Ekleme Sihirbazı", true);
        d.setSize(450, 300);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(this);

        JPanel pnl = new JPanel(new GridLayout(4, 2, 10, 15));
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField txtAd = new JTextField();
        JComboBox<String> cbTur = new JComboBox<>(new String[]{"Standart İçerik (Ücretsiz, Çıkarılabilir)", "Ekstra Malzeme (Ücretli, Eklenebilir)"});
        JTextField txtFiyat = new JTextField("0.0");
        txtFiyat.setEnabled(false); // Standart seçiliyken fiyat kapalıdır.

        cbTur.addActionListener(e -> {
            if (cbTur.getSelectedIndex() == 1) { // Ekstra ise fiyatı aç
                txtFiyat.setEnabled(true);
                txtFiyat.setText("");
            } else {
                txtFiyat.setEnabled(false);
                txtFiyat.setText("0.0");
            }
        });

        pnl.add(new JLabel("<html><b>Malzeme / Ekstra Adı:</b></html>")); pnl.add(txtAd);
        pnl.add(new JLabel("<html><b>İçerik Türü:</b></html>")); pnl.add(cbTur);
        pnl.add(new JLabel("<html><b>Ekstra Ücret (TL):</b></html>")); pnl.add(txtFiyat);

        JButton btnEkle = new JButton("Listeye Ekle");
        btnEkle.setBackground(new Color(39, 174, 96));
        btnEkle.setForeground(Color.WHITE);
        btnEkle.setFont(new Font("Arial", Font.BOLD, 15));
        btnEkle.setPreferredSize(new Dimension(0, 40));

        btnEkle.addActionListener(e -> {
            String ad = txtAd.getText().trim();
            if (ad.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Lütfen malzeme adını girin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Veritabanı Kodlaması (1 = Standart, 0 = Ekstra)
            String turKodu = cbTur.getSelectedIndex() == 0 ? "1" : "0";
            String fiyat = txtFiyat.getText().trim().replace(",", ".");
            if (fiyat.isEmpty()) fiyat = "0.0";

            // Formatı birleştir
            String formatliVeri = ad + ":" + turKodu + ":" + fiyat;

            // Metin kutusuna yazdır (Virgül ile ayırarak)
            String mevcut = txtMalzemeler.getText().trim();
            if (mevcut.isEmpty() || mevcut.equals("null")) {
                txtMalzemeler.setText(formatliVeri);
            } else {
                txtMalzemeler.setText(mevcut + ", " + formatliVeri);
            }
            d.dispose();
        });

        d.add(pnl, BorderLayout.CENTER);
        d.add(btnEkle, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KAT_LISTESI_GETIR");
            SwingUtilities.invokeLater(() -> {
                katTableModel.setRowCount(0); 
                String mevcutSecim = cbKatSecim.getSelectedItem() != null ? cbKatSecim.getSelectedItem().toString() : null;
                cbKatSecim.removeAllItems();
                
                if (cevap != null && cevap.startsWith("KAT_LISTESI|")) {
                    String[] parcalar = cevap.split("\\|");
                    for (int i = 1; i < parcalar.length; i++) {
                        String[] d = parcalar[i].split(";");
                        katTableModel.addRow(new Object[]{d[0], d[1]}); 
                        cbKatSecim.addItem(d[0]);
                    }
                }
                if (mevcutSecim != null) cbKatSecim.setSelectedItem(mevcutSecim);
            });
        }).start();
    }

    private void urunleriListele(String katAdi) {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + katAdi);
            SwingUtilities.invokeLater(() -> {
                urunTableModel.setRowCount(0);
                if (cevap != null && cevap.startsWith("URUN_LISTESI_DETAYLI|")) {
                    String[] parcalar = cevap.split("\\|");
                    
                    for (int i = 1; i < parcalar.length; i++) {
                        String[] d = parcalar[i].split(";", -1); 
                        
                        if (d.length >= 6) {
                            String ad = d[0];
                            String fiyat = d[1] + " TL";
                            String aciklama = d[2];
                            String rawMlz = d[5];
                            
                            StringBuilder mlzFormatli = new StringBuilder();
                            if (rawMlz != null && !rawMlz.isEmpty() && !rawMlz.equals("null")) {
                                String[] mArr = rawMlz.split(",");
                                for (String m : mArr) {
                                    String[] mDetay = m.split(":");
                                    if(mDetay.length == 3) {
                                        String mAd = mDetay[0];
                                        boolean isStandart = mDetay[1].equals("1");
                                        String mFiyat = mDetay[2];
                                        
                                        if (isStandart) mlzFormatli.append(mAd).append(" (Standart), ");
                                        else mlzFormatli.append(mAd).append(" (+").append(mFiyat).append(" TL), ");
                                    }
                                }
                                if (mlzFormatli.length() > 2) mlzFormatli.setLength(mlzFormatli.length() - 2);
                            } else {
                                mlzFormatli.append("İçerik Girilmemiş");
                            }
                            
                            urunTableModel.addRow(new Object[]{ad, katAdi, aciklama, fiyat, mlzFormatli.toString()});
                        }
                    }
                }
            });
        }).start();
    }
}