import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PersonelYonetimi extends JPanel {
    private AdminPaneli anaPanel;
    private DefaultTableModel tabloModel;
    private JTable tablo;

    // Form Elemanları
    private JTextField txtKullaniciAdi, txtSifre, txtAdSoyad;
    private JComboBox<String> cbRoller;
    private String seciliEskiKullaniciAdi = ""; // Güncelleme işlemi için

    public PersonelYonetimi(AdminPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);

        // ==========================================
        // SOL TARAF: PERSONEL TABLOSU
        // ==========================================
        JPanel pnlSol = new JPanel(new BorderLayout(5, 5));
        
        tabloModel = new DefaultTableModel(new String[]{"Kullanıcı Adı", "Şifre", "Ad Soyad", "Rol / Yetki"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablo = new JTable(tabloModel); 
        tablo.setRowHeight(35); 
        tablo.setFont(new Font("Arial", Font.PLAIN, 14));
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(tablo);
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Sistemde Kayıtlı Personeller", 0, 0, new Font("Arial", Font.BOLD, 14)));
        pnlSol.add(scrollPane, BorderLayout.CENTER);

        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.setBackground(new Color(52, 152, 219)); 
        btnYenile.setForeground(Color.WHITE);
        btnYenile.addActionListener(e -> verileriYenile());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        // ==========================================
        // SAĞ TARAF: BİLGİ GİRİŞ FORMU VE İŞLEMLER
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout());
        pnlSag.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2), "Personel Ekle / Güncelle", 0, 0, new Font("Arial", Font.BOLD, 15)));

        JPanel pnlForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15); 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.anchor = GridBagConstraints.WEST;

        txtAdSoyad = new JTextField(15); txtAdSoyad.setFont(new Font("Arial", Font.PLAIN, 15));
        txtKullaniciAdi = new JTextField(15); txtKullaniciAdi.setFont(new Font("Arial", Font.PLAIN, 15));
        txtSifre = new JTextField(15); txtSifre.setFont(new Font("Arial", Font.PLAIN, 15));
        
        // SENİN İSTEDİĞİN ROLLER BURAYA EKLENDİ
        String[] roller = {"Yetkili(admin)", "Kasiyer", "Garson", "Staff", "Motokurye"};
        cbRoller = new JComboBox<>(roller); 
        cbRoller.setFont(new Font("Arial", Font.PLAIN, 15));

        gbc.gridx = 0; gbc.gridy = 0; pnlForm.add(new JLabel("<html><b>Ad Soyad:</b></html>"), gbc);
        gbc.gridx = 1; pnlForm.add(txtAdSoyad, gbc);

        gbc.gridx = 0; gbc.gridy = 1; pnlForm.add(new JLabel("<html><b>Kullanıcı Adı:</b></html>"), gbc);
        gbc.gridx = 1; pnlForm.add(txtKullaniciAdi, gbc);

        gbc.gridx = 0; gbc.gridy = 2; pnlForm.add(new JLabel("<html><b>Şifre:</b></html>"), gbc);
        gbc.gridx = 1; pnlForm.add(txtSifre, gbc);

        gbc.gridx = 0; gbc.gridy = 3; pnlForm.add(new JLabel("<html><b>Rol / Yetki:</b></html>"), gbc);
        gbc.gridx = 1; pnlForm.add(cbRoller, gbc);

        pnlSag.add(pnlForm, BorderLayout.NORTH);

        // ALT BUTONLAR
        JPanel pnlButonlar = new JPanel(new GridLayout(1, 3, 10, 10)); 
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton btnEkle = new JButton("➕ Yeni Ekle"); 
        btnEkle.setBackground(new Color(39, 174, 96)); 
        btnEkle.setForeground(Color.WHITE); 
        btnEkle.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton btnGuncelle = new JButton("💾 Güncelle"); 
        btnGuncelle.setBackground(new Color(243, 156, 18)); 
        btnGuncelle.setForeground(Color.WHITE); 
        btnGuncelle.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton btnSil = new JButton("❌ Sil"); 
        btnSil.setBackground(new Color(192, 57, 43)); 
        btnSil.setForeground(Color.WHITE); 
        btnSil.setFont(new Font("Arial", Font.BOLD, 14));

        pnlButonlar.add(btnEkle); 
        pnlButonlar.add(btnGuncelle); 
        pnlButonlar.add(btnSil);
        
        pnlSag.add(pnlButonlar, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlSag);
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // DİNLEYİCİLER VE AKSİYONLAR
        // ==========================================
        tablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablo.getSelectedRow() != -1) {
                int row = tablo.getSelectedRow();
                seciliEskiKullaniciAdi = tabloModel.getValueAt(row, 0).toString();
                txtKullaniciAdi.setText(seciliEskiKullaniciAdi);
                txtSifre.setText(tabloModel.getValueAt(row, 1).toString());
                txtAdSoyad.setText(tabloModel.getValueAt(row, 2).toString());
                cbRoller.setSelectedItem(tabloModel.getValueAt(row, 3).toString());
            }
        });

        btnEkle.addActionListener(e -> islemYap("EKLE"));
        btnGuncelle.addActionListener(e -> islemYap("GUNCELLE"));
        btnSil.addActionListener(e -> islemYap("SIL"));
    }

    private void islemYap(String tur) {
        String kAdi = txtKullaniciAdi.getText().trim().replace("|", "");
        String sifre = txtSifre.getText().trim().replace("|", "");
        String adSoyad = txtAdSoyad.getText().trim().replace("|", "");
        String rol = cbRoller.getSelectedItem().toString();

        if (tur.equals("SIL")) {
            if (seciliEskiKullaniciAdi.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Silinecek personeli tablodan seçiniz!"); 
                return; 
            }
            if (JOptionPane.showConfirmDialog(this, adSoyad + " adlı personeli sistemden kalıcı olarak silmek istediğinize emin misiniz?", "Silme Onayı", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("PERSONEL_SIL|" + seciliEskiKullaniciAdi);
                JOptionPane.showMessageDialog(this, cvp);
                formuTemizle(); 
                verileriYenile();
            }
            return;
        }

        // Ekleme ve Güncelleme için boş alan kontrolü
        if (kAdi.isEmpty() || sifre.isEmpty() || adSoyad.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "Tüm alanları doldurmak zorunludur!", "Uyarı", JOptionPane.WARNING_MESSAGE); 
            return; 
        }

        if (tur.equals("EKLE")) {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("PERSONEL_EKLE|" + kAdi + "|" + sifre + "|" + adSoyad + "|" + rol);
            JOptionPane.showMessageDialog(this, cvp);
            if(cvp.startsWith("BAŞARILI")) { formuTemizle(); verileriYenile(); }
        } 
        else if (tur.equals("GUNCELLE")) {
            if (seciliEskiKullaniciAdi.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Güncellenecek personeli tablodan seçiniz!"); 
                return; 
            }
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("PERSONEL_GUNCELLE|" + seciliEskiKullaniciAdi + "|" + kAdi + "|" + sifre + "|" + adSoyad + "|" + rol);
            JOptionPane.showMessageDialog(this, cvp);
            if(cvp.startsWith("BAŞARILI")) { formuTemizle(); verileriYenile(); }
        }
    }

    private void formuTemizle() {
        txtAdSoyad.setText(""); 
        txtKullaniciAdi.setText(""); 
        txtSifre.setText(""); 
        cbRoller.setSelectedIndex(0);
        seciliEskiKullaniciAdi = ""; 
        tablo.clearSelection();
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("PERSONELLERI_GETIR");
            SwingUtilities.invokeLater(() -> {
                tabloModel.setRowCount(0);
                if (cevap != null && cevap.startsWith("PERSONEL_LISTESI|") && cevap.length() > 17) {
                    String[] kisiler = cevap.substring(17).split("\\|\\|\\|");
                    for (String k : kisiler) {
                        if (k.trim().isEmpty()) continue;
                        String[] d = k.split("~_~"); 
                        // Şifreyi Admin görsün ki unutan personele hatırlatabilsin
                        if (d.length == 4) tabloModel.addRow(new Object[]{d[0], d[1], d[2], d[3]});
                    }
                }
            });
        }).start();
    }
}