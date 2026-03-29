
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RezervasyonModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultTableModel tabloModeli;
    private JTable tablo;
    
    // Form Elemanları
    private JComboBox<String> cmbMasa;
    private JTextField txtMusteri;
    private JTextField txtTelefon;
    private JTextField txtTarih;
    private JTextField txtSaat;
    private JTextArea txtNot;

    public RezervasyonModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(380);
        splitPane.setEnabled(false); // Formun genişliğini sabitle

        // ==========================================
        // SOL TARAF: REZERVASYON FORMU
        // ==========================================
        JPanel pnlFormSarici = new JPanel(new BorderLayout());
        pnlFormSarici.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(142, 68, 173), 2), "Yeni Rezervasyon Oluştur", 0, 0, new Font("Arial", Font.BOLD, 15)));
        
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBackground(new Color(244, 240, 246)); // Hafif mor-gri arka plan
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Form Veri Alanları Tanımlaması
        cmbMasa = new JComboBox<>();
        cmbMasa.setPreferredSize(new Dimension(200, 30));
        
        txtMusteri = new JTextField(); 
        txtTelefon = new JTextField();
        txtTarih = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        txtSaat = new JTextField("20:00");
        
        txtNot = new JTextArea(3, 20);
        txtNot.setLineWrap(true);
        txtNot.setWrapStyleWord(true);
        JScrollPane scrollNot = new JScrollPane(txtNot);

        // Satır 1: Masa Seçimi
        gbc.gridx = 0; gbc.gridy = 0; pnlForm.add(new JLabel("Masa Seçimi (*):"), gbc);
        gbc.gridx = 1; pnlForm.add(cmbMasa, gbc);

        // Satır 2: Müşteri Adı
        gbc.gridx = 0; gbc.gridy = 1; pnlForm.add(new JLabel("Müşteri Adı (*):"), gbc);
        gbc.gridx = 1; pnlForm.add(txtMusteri, gbc);

        // Satır 3: Telefon
        gbc.gridx = 0; gbc.gridy = 2; pnlForm.add(new JLabel("Telefon Numarası:"), gbc);
        gbc.gridx = 1; pnlForm.add(txtTelefon, gbc);

        // Satır 4: Tarih
        gbc.gridx = 0; gbc.gridy = 3; pnlForm.add(new JLabel("Tarih (Y-A-G) (*):"), gbc);
        gbc.gridx = 1; pnlForm.add(txtTarih, gbc);

        // Satır 5: Saat
        gbc.gridx = 0; gbc.gridy = 4; pnlForm.add(new JLabel("Saat (*):"), gbc);
        gbc.gridx = 1; pnlForm.add(txtSaat, gbc);

        // Satır 6: Notlar
        gbc.gridx = 0; gbc.gridy = 5; pnlForm.add(new JLabel("Ekstra Notlar:"), gbc);
        gbc.gridx = 1; pnlForm.add(scrollNot, gbc);

        // Satır 7: Kaydet Butonu
        JButton btnEkle = new JButton("➕ Rezerve Et");
        btnEkle.setBackground(new Color(142, 68, 173));
        btnEkle.setForeground(Color.WHITE);
        btnEkle.setFont(new Font("Arial", Font.BOLD, 15));
        btnEkle.setPreferredSize(new Dimension(0, 45));
        
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; 
        gbc.insets = new Insets(20, 10, 10, 10);
        pnlForm.add(btnEkle, gbc);

        pnlFormSarici.add(pnlForm, BorderLayout.NORTH);
        splitPane.setLeftComponent(pnlFormSarici);

        // ==========================================
        // SAĞ TARAF: REZERVASYON LİSTESİ
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout(5, 5));
        
        tabloModeli = new DefaultTableModel(new String[]{"ID", "Masa", "Müşteri Adı", "Telefon", "Tarih", "Saat", "Aşçı/Servis Notu"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablo = new JTable(tabloModeli); 
        tablo.setRowHeight(30); 
        tablo.setFont(new Font("Arial", Font.PLAIN, 14));
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablo.getColumnModel().getColumn(0).setPreferredWidth(40); // ID kolonu dar olsun
        
        JScrollPane scrollTablo = new JScrollPane(tablo);
        scrollTablo.setBorder(BorderFactory.createTitledBorder("Aktif Tüm Rezervasyonlar"));
        pnlSag.add(scrollTablo, BorderLayout.CENTER);

        JPanel pnlAltIslemler = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnIptal = new JButton("❌ İptal Et"); 
        btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE); btnIptal.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton btnGeldi = new JButton("✔ Müşteri Geldi (Masaya Al)"); 
        btnGeldi.setBackground(new Color(39, 174, 96)); btnGeldi.setForeground(Color.WHITE); btnGeldi.setFont(new Font("Arial", Font.BOLD, 14));
        
        pnlAltIslemler.add(btnIptal); 
        pnlAltIslemler.add(btnGeldi);
        pnlSag.add(pnlAltIslemler, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlSag);
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // BUTON AKSİYONLARI VE İŞLEMLER
        // ==========================================
        btnEkle.addActionListener(e -> {
            if(cmbMasa.getSelectedItem() == null || txtMusteri.getText().trim().isEmpty() || txtTarih.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masa, Müşteri, Tarih ve Saat alanları zorunludur!", "Eksik Veri", JOptionPane.WARNING_MESSAGE); return;
            }

            String masa = cmbMasa.getSelectedItem().toString();
            String musteri = txtMusteri.getText().trim().replace("|", " "); // Pipe karakteri protokolü bozmasın diye engelleniyor
            String telefon = txtTelefon.getText().trim().isEmpty() ? "Belirtilmedi" : txtTelefon.getText().trim().replace("|", " ");
            String tarih = txtTarih.getText().trim();
            String saat = txtSaat.getText().trim();
            String notlar = txtNot.getText().trim().isEmpty() ? "Yok" : txtNot.getText().trim().replace("|", " ");

            String komut = "REZERVASYON_EKLE|" + masa + "|" + musteri + "|" + telefon + "|" + tarih + "|" + saat + "|" + notlar;
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut);
            
            if (cvp != null && cvp.startsWith("BAŞARILI")) {
                JOptionPane.showMessageDialog(this, "Rezervasyon başarıyla oluşturuldu.");
                // Formu temizle (Tarih ve Saat kalsın, serilik sağlar)
                txtMusteri.setText(""); txtTelefon.setText(""); txtNot.setText("");
                verileriYenile();
            } else {
                // Çifte rezervasyon veya başka bir hata varsa göster
                JOptionPane.showMessageDialog(this, cvp, "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnIptal.addActionListener(e -> durumDegistir("IPTAL"));
        btnGeldi.addActionListener(e -> durumDegistir("GELDILER"));
    }

    private void durumDegistir(String durum) {
        int row = tablo.getSelectedRow();
        if(row == -1) { JOptionPane.showMessageDialog(this, "Lütfen işlem yapmak için tablodan bir rezervasyon seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE); return; }
        
        String id = tabloModeli.getValueAt(row, 0).toString();
        String masa = tabloModeli.getValueAt(row, 1).toString();
        
        if (durum.equals("IPTAL")) {
            int onay = JOptionPane.showConfirmDialog(this, masa + " rezervasyonunu İPTAL etmek istediğinize emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION);
            if (onay != JOptionPane.YES_OPTION) return;
        }

        anaPanel.sunucuyaKomutGonderVeCevapAl("REZ_DURUM_GUNCELLE|" + id + "|" + durum);
        verileriYenile();
    }

    // Masaları combobox içine doldurur ve tabloyu yeniler
    public void verileriYenile() {
        // 1. Önce masaları sunucudan çekip ComboBox'a doldur
        new Thread(() -> {
            String masaCevap = anaPanel.sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            if (masaCevap != null && masaCevap.startsWith("MASA_LISTESI")) {
                String[] parcalar = masaCevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    String seciliMasa = cmbMasa.getSelectedItem() != null ? cmbMasa.getSelectedItem().toString() : null;
                    cmbMasa.removeAllItems();
                    for(int i = 1; i < parcalar.length; i++) {
                        String masaAdi = parcalar[i].split(";")[0];
                        cmbMasa.addItem(masaAdi);
                    }
                    if (seciliMasa != null) cmbMasa.setSelectedItem(seciliMasa);
                });
            }
        }).start();

        // 2. Tabloyu güncel rezervasyonlarla doldur
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("REZ_LISTESI_GETIR");
            SwingUtilities.invokeLater(() -> {
                tabloModeli.setRowCount(0);
                if (cevap != null && cevap.startsWith("REZ_LISTESI|") && cevap.length() > 12) {
                    String[] kayitlar = cevap.substring(12).split("\\|\\|\\|");
                    for (String k : kayitlar) {
                        if (k.trim().isEmpty()) continue;
                        String[] d = k.split("~_~"); 
                        // d = [ID, Masa, Musteri, Telefon, Tarih, Saat, Notlar]
                        if (d.length == 7) {
                            tabloModeli.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4], d[5], d[6]});
                        }
                    }
                }
            });
        }).start();
    }
}