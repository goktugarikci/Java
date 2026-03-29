
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
        
        // Kategori Tablosu
        katTableModel = new DefaultTableModel(new String[]{"Kategori Adı", "Görsel Dosyası"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        katTablo = new JTable(katTableModel); 
        katTablo.setRowHeight(30); 
        katTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        pnlKategori.add(new JScrollPane(katTablo), BorderLayout.CENTER);

        // Kategori Formu
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
        // 2. SEKMESİ: ÜRÜN YÖNETİMİ
        // ==========================================
        JPanel pnlUrun = new JPanel(new BorderLayout(10, 10));
        
        // SENİN İSTEDİĞİN SÜTUNLAR: İsim, Kategori, Açıklama, Fiyat, İçindekiler Listesi
        urunTableModel = new DefaultTableModel(new String[]{"İsim", "Kategori", "Açıklama", "Fiyat", "İçindekiler Listesi (Ekstra İçerik Dahil Fiyat Girilecek)"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        urunTablo = new JTable(urunTableModel); 
        urunTablo.setRowHeight(35); 
        urunTablo.setFont(new Font("Arial", Font.PLAIN, 14));
        urunTablo.getColumnModel().getColumn(4).setPreferredWidth(400); // İçindekiler kısmı geniş olsun
        
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

        // ÜRÜN EKLEME FORMU
        JPanel pnlUrunForm = new JPanel(new GridBagLayout()); 
        pnlUrunForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(39, 174, 96), 2), "Yeni Ürün Ekle", 0, 0, new Font("Arial", Font.BOLD, 14)));
        GridBagConstraints gbc = new GridBagConstraints(); 
        gbc.insets = new Insets(10, 10, 10, 10); 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.anchor = GridBagConstraints.WEST;

        JTextField txtUrunAd = new JTextField(15); 
        JTextField txtFiyat = new JTextField(8); 
        JTextField txtAciklama = new JTextField(25); 
        JTextField txtMalzemeler = new JTextField(40);
        
        gbc.gridx = 0; gbc.gridy = 0; pnlUrunForm.add(new JLabel("<html><b>Ürün İsmi:</b></html>"), gbc);
        gbc.gridx = 1; pnlUrunForm.add(txtUrunAd, gbc);
        gbc.gridx = 2; pnlUrunForm.add(new JLabel("<html><b>Fiyat (TL):</b></html>"), gbc);
        gbc.gridx = 3; pnlUrunForm.add(txtFiyat, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; pnlUrunForm.add(new JLabel("<html><b>Açıklama:</b></html>"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; pnlUrunForm.add(txtAciklama, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; pnlUrunForm.add(new JLabel("<html><b>İçindekiler Listesi:</b></html>"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; pnlUrunForm.add(txtMalzemeler, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3;
        JLabel lblBilgi = new JLabel("<html><i>Format: MalzemeAdı:1:0.0 (Standart için 1, Ekstra için 0 ve Fiyat) -> Örn: <font color='red'><b>Domates:1:0.0, Ekstra Sos:0:5.5</b></font></i></html>"); 
        lblBilgi.setForeground(Color.GRAY);
        pnlUrunForm.add(lblBilgi, gbc);

        JPanel pnlU3 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnUrunEkle = new JButton("➕ Ürünü Ekle"); 
        btnUrunEkle.setBackground(new Color(39, 174, 96)); 
        btnUrunEkle.setForeground(Color.WHITE); 
        btnUrunEkle.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton btnUrunSil = new JButton("❌ Seçili Ürünü Sil"); 
        btnUrunSil.setBackground(new Color(192, 57, 43)); 
        btnUrunSil.setForeground(Color.WHITE); 
        btnUrunSil.setFont(new Font("Arial", Font.BOLD, 14));
        
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
        
        // 1. KATEGORİ EKLE
        btnKatEkle.addActionListener(e -> {
            String katAd = txtKatAd.getText().trim();
            String katDesc = txtKatDesc.getText().trim();
            if(katAd.isEmpty()) return;
            if(katDesc.isEmpty()) katDesc = "Açıklama Yok";
            
            // ClientHandler'daki KATEGORI_EKLE komutu ile eşleşir
            String komut = "KATEGORI_EKLE|" + katAd + "|" + katDesc + "|gorsel_yok.png";
            JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl(komut));
            
            txtKatAd.setText(""); txtKatDesc.setText(""); 
            verileriYenile(); // Listeyi güncelle
        });

        // 2. KATEGORİ SİL
        btnKatSil.addActionListener(e -> {
            int row = katTablo.getSelectedRow();
            if(row != -1) {
                String seciliKat = katTableModel.getValueAt(row, 0).toString();
                // ClientHandler'daki KATEGORI_SIL komutu ile eşleşir
                JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("KATEGORI_SIL|" + seciliKat));
                verileriYenile();
            } else {
                JOptionPane.showMessageDialog(this, "Önce tablodan silinecek kategoriyi seçin!");
            }
        });

        // 3. ÜRÜNLERİ GETİR
        btnUrunGetir.addActionListener(e -> {
            if(cbKatSecim.getSelectedItem() != null) {
                urunleriListele(cbKatSecim.getSelectedItem().toString());
            }
        });

        cbKatSecim.addActionListener(e -> {
            if(cbKatSecim.getSelectedItem() != null) {
                urunleriListele(cbKatSecim.getSelectedItem().toString());
            }
        });

        // 4. ÜRÜN EKLE
        btnUrunEkle.addActionListener(e -> {
            if(txtUrunAd.getText().trim().isEmpty() || txtFiyat.getText().trim().isEmpty() || cbKatSecim.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "İsim, Fiyat ve Kategori boş bırakılamaz!", "Hata", JOptionPane.ERROR_MESSAGE); 
                return;
            }
            String kat = cbKatSecim.getSelectedItem().toString();
            String urunAd = txtUrunAd.getText().trim();
            String fiyat = txtFiyat.getText().trim().replace(",","."); // Virgülü noktaya çevir
            String aciklama = txtAciklama.getText().trim().isEmpty() ? "Açıklama Yok" : txtAciklama.getText().trim();
            String mlz = txtMalzemeler.getText().trim().isEmpty() ? "null" : txtMalzemeler.getText().trim();
            
            // ClientHandler'daki URUN_EKLE_DETAYLI komutu ile eşleşir
            // Format: URUN_EKLE_DETAYLI | Kategori | UrunAdi | Fiyat | Aciklama | Gorsel | Malzemeler
            String komut = "URUN_EKLE_DETAYLI|" + kat + "|" + urunAd + "|" + fiyat + "|" + aciklama + "|gorsel_yok.png|" + mlz;
            
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut);
            JOptionPane.showMessageDialog(this, cvp);
            
            if (cvp.startsWith("BAŞARILI")) {
                txtUrunAd.setText(""); txtFiyat.setText(""); txtAciklama.setText(""); txtMalzemeler.setText(""); 
                urunleriListele(kat);
            }
        });

        // 5. ÜRÜN SİL
        btnUrunSil.addActionListener(e -> {
            int row = urunTablo.getSelectedRow();
            if(row != -1) {
                String seciliUrunAdi = urunTableModel.getValueAt(row, 0).toString();
                // ClientHandler'daki URUN_SIL komutu ile eşleşir
                JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("URUN_SIL|" + seciliUrunAdi));
                
                if(cbKatSecim.getSelectedItem() != null) {
                    urunleriListele(cbKatSecim.getSelectedItem().toString());
                }
            } else {
                JOptionPane.showMessageDialog(this, "Önce tablodan silinecek ürünü seçin!");
            }
        });
    }

    // Arayüz yüklendiğinde Kategorileri Çeker
    public void verileriYenile() {
        new Thread(() -> {
            // ClientHandler'daki KAT_LISTESI_GETIR komutu ile eşleşir
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
                
                if (mevcutSecim != null) {
                    cbKatSecim.setSelectedItem(mevcutSecim);
                }
            });
        }).start();
    }

    // Ürünleri Çekme ve Tabloya Basma (Özel Biçimlendirme)
    private void urunleriListele(String katAdi) {
        new Thread(() -> {
            // ClientHandler'daki URUNLERI_GETIR_DETAYLI komutu ile eşleşir
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("URUNLERI_GETIR_DETAYLI|" + katAdi);
            SwingUtilities.invokeLater(() -> {
                urunTableModel.setRowCount(0);
                if (cevap != null && cevap.startsWith("URUN_LISTESI_DETAYLI|")) {
                    String[] parcalar = cevap.split("\\|");
                    
                    for (int i = 1; i < parcalar.length; i++) {
                        // -1 parametresi boşlukların silinmesini engeller
                        String[] d = parcalar[i].split(";", -1); 
                        
                        if (d.length >= 6) {
                            String ad = d[0];
                            String fiyat = d[1] + " TL";
                            String aciklama = d[2];
                            String rawMlz = d[5]; // Malzemeler datası
                            
                            StringBuilder mlzFormatli = new StringBuilder();
                            if (rawMlz != null && !rawMlz.isEmpty() && !rawMlz.equals("null")) {
                                String[] mArr = rawMlz.split(",");
                                for (String m : mArr) {
                                    String[] mDetay = m.split(":");
                                    if(mDetay.length == 3) {
                                        String mAd = mDetay[0];
                                        boolean isStandart = mDetay[1].equals("1");
                                        String mFiyat = mDetay[2];
                                        
                                        if (isStandart) {
                                            mlzFormatli.append(mAd).append(" (Standart), ");
                                        } else {
                                            mlzFormatli.append(mAd).append(" (+").append(mFiyat).append(" TL), ");
                                        }
                                    }
                                }
                                // Sondaki virgülü sil
                                if (mlzFormatli.length() > 2) mlzFormatli.setLength(mlzFormatli.length() - 2);
                            } else {
                                mlzFormatli.append("İçerik/Ekstra Girilmemiş");
                            }
                            
                            // Tablo formatı: "İsim", "Kategori", "Açıklama", "Fiyat", "İçindekiler Listesi"
                            urunTableModel.addRow(new Object[]{ad, katAdi, aciklama, fiyat, mlzFormatli.toString()});
                        }
                    }
                }
            });
        }).start();
    }
}