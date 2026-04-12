import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.LocalTime;

public class VardiyaModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultTableModel vardiyaModel;
    private JLabel lblDurum;

    public VardiyaModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ÜST PANEL: Giriş-Çıkış Butonları
        JPanel pnlKontrol = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton btnGiris = new JButton("Vardiya Başlat");
        btnGiris.setBackground(new Color(46, 204, 113)); btnGiris.setForeground(Color.WHITE);
        
        JButton btnCikis = new JButton("Vardiya Bitir");
        btnCikis.setBackground(new Color(231, 76, 60)); btnCikis.setForeground(Color.WHITE);
        
        lblDurum = new JLabel("Mevcut Durum: Vardiya Dışı");
        lblDurum.setFont(new Font("Arial", Font.BOLD, 14));

        pnlKontrol.add(btnGiris);
        pnlKontrol.add(btnCikis);
        pnlKontrol.add(new JSeparator(SwingConstants.VERTICAL));
        pnlKontrol.add(lblDurum);
        
        add(pnlKontrol, BorderLayout.NORTH);

        // ORTA PANEL: Günlük Vardiya Listesi
        vardiyaModel = new DefaultTableModel(new String[]{"Personel", "Giriş", "Çıkış", "Süre", "Mesai", "Hakediş"}, 0);
        JTable tblVardiya = new JTable(vardiyaModel);
        add(new JScrollPane(tblVardiya), BorderLayout.CENTER);

        // ETKİLEŞİMLER
        btnGiris.addActionListener(e -> vardiyaGuncelle("GIRIS"));
        btnCikis.addActionListener(e -> vardiyaGuncelle("CIKIS"));
    }

    private void vardiyaGuncelle(String tip) {
        String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("VARDIYA_ISLEM|" + tip);
        JOptionPane.showMessageDialog(this, cevap);
        verileriYenile();
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("VARDIYA_LISTESI_GETIR");
            SwingUtilities.invokeLater(() -> {
                vardiyaModel.setRowCount(0);
                if (cvp != null && cvp.startsWith("VARDIYA_VERI|")) {
                    String[] satirlar = cvp.substring(13).split("\\|\\|\\|");
                    for (String s : satirlar) {
                        vardiyaModel.addRow(s.split("~_~"));
                    }
                }
            });
        }).start();
    }
}