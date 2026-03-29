
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RezervasyonModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultTableModel tabloModeli;
    private JTable tablo;
    
    private JTextField txtMasaNo, txtMusteri, txtTarih, txtSaat;

    public RezervasyonModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- ÜST: KAYIT FORMU ---
        JPanel pnlUst = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlUst.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(142, 68, 173)), "Yeni Rezervasyon Ekle", 0, 0, new Font("Arial", Font.BOLD, 14)));
        pnlUst.setBackground(new Color(244, 240, 246));

        txtMasaNo = new JTextField(8);
        txtMusteri = new JTextField(12);
        txtTarih = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 10); // Otomatik bugünü yazar
        txtSaat = new JTextField("20:00", 5);
        
        JButton btnEkle = new JButton("➕ Rezerve Et");
        btnEkle.setBackground(new Color(142, 68, 173)); // Mor Tema
        btnEkle.setForeground(Color.WHITE); btnEkle.setFont(new Font("Arial", Font.BOLD, 14));

        pnlUst.add(new JLabel("Masa (Örn: Masa 5):")); pnlUst.add(txtMasaNo);
        pnlUst.add(new JLabel("Müşteri Adı:")); pnlUst.add(txtMusteri);
        pnlUst.add(new JLabel("Tarih (Y-A-G):")); pnlUst.add(txtTarih);
        pnlUst.add(new JLabel("Saat:")); pnlUst.add(txtSaat);
        pnlUst.add(btnEkle);

        add(pnlUst, BorderLayout.NORTH);

        // --- ORTA: LİSTE ---
        tabloModeli = new DefaultTableModel(new String[]{"ID", "Masa", "Müşteri Adı", "Tarih", "Saat"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablo = new JTable(tabloModeli); tablo.setRowHeight(30); tablo.setFont(new Font("Arial", Font.PLAIN, 14));
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(tablo);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Aktif Tüm Rezervasyonlar"));
        add(scrollPane, BorderLayout.CENTER);

        // --- ALT: İŞLEMLER ---
        JPanel pnlAlt = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnIptal = new JButton("❌ İptal Et"); btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE);
        JButton btnGeldi = new JButton("✔ Müşteri Geldi (Masaya Al)"); btnGeldi.setBackground(new Color(39, 174, 96)); btnGeldi.setForeground(Color.WHITE);
        
        pnlAlt.add(btnIptal); pnlAlt.add(btnGeldi);
        add(pnlAlt, BorderLayout.SOUTH);

        // --- AKSİYONLAR ---
        btnEkle.addActionListener(e -> {
            if(txtMasaNo.getText().isEmpty() || txtMusteri.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masa ve Müşteri alanları zorunludur!"); return;
            }
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("REZERVASYON_EKLE|" + txtMasaNo.getText() + "|" + txtMusteri.getText() + "|" + txtTarih.getText() + "|" + txtSaat.getText());
            JOptionPane.showMessageDialog(this, cvp);
            verileriYenile();
        });

        btnIptal.addActionListener(e -> durumDegistir("IPTAL"));
        btnGeldi.addActionListener(e -> durumDegistir("GELDILER"));
    }

    private void durumDegistir(String durum) {
        int row = tablo.getSelectedRow();
        if(row == -1) { JOptionPane.showMessageDialog(this, "Tablodan bir rezervasyon seçin!"); return; }
        String id = tabloModeli.getValueAt(row, 0).toString();
        anaPanel.sunucuyaKomutGonderVeCevapAl("REZ_DURUM_GUNCELLE|" + id + "|" + durum);
        verileriYenile();
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("REZ_LISTESI_GETIR");
            SwingUtilities.invokeLater(() -> {
                tabloModeli.setRowCount(0);
                if (cevap != null && cevap.startsWith("REZ_LISTESI|") && cevap.length() > 12) {
                    String[] kayitlar = cevap.substring(12).split("\\|\\|\\|");
                    for (String k : kayitlar) {
                        if (k.trim().isEmpty()) continue;
                        String[] d = k.split("~_~"); 
                        if (d.length == 5) tabloModeli.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                    }
                }
            });
        }).start();
    }
}