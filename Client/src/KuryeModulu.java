
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class KuryeModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private String kuryeAdi;
    
    private DefaultTableModel tabloModel;
    private JTable tablo;
    private JEditorPane txtFisDetay;
    private String seciliSiparisId = "";

    public KuryeModulu(PersonelPaneli anaPanel, String kuryeAdi) {
        this.anaPanel = anaPanel;
        this.kuryeAdi = kuryeAdi;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);

        // SOL TARAF: TESLİMATLAR
        JPanel pnlSol = new JPanel(new BorderLayout());
        pnlSol.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "🛵 Bana Atanan Teslimatlar"));

        tabloModel = new DefaultTableModel(new String[]{"Sipariş No", "Müşteri Adı", "Durum"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablo = new JTable(tabloModel); tablo.setRowHeight(35); tablo.setFont(new Font("Arial", Font.PLAIN, 14));
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pnlSol.add(new JScrollPane(tablo), BorderLayout.CENTER);

        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.setBackground(new Color(52, 152, 219)); btnYenile.setForeground(Color.WHITE);
        btnYenile.addActionListener(e -> verileriYenile());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);

        // SAĞ TARAF: FİŞ VE ÖDEME ALMA
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10));
        pnlSag.setBorder(BorderFactory.createTitledBorder("Müşteri Adresi ve Fiş Detayı"));

        txtFisDetay = new JEditorPane(); txtFisDetay.setContentType("text/html"); txtFisDetay.setEditable(false);
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        JPanel pnlButonlar = new JPanel(new GridLayout(1, 3, 10, 10));
        JButton btnNakit = new JButton("💵 Nakit Tahsil Edildi"); btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE);
        JButton btnKart = new JButton("💳 K.Kartı Tahsil Edildi"); btnKart.setBackground(new Color(41, 128, 185)); btnKart.setForeground(Color.WHITE);
        JButton btnIptal = new JButton("❌ Teslim Edilemedi"); btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE);

        pnlButonlar.add(btnNakit); pnlButonlar.add(btnKart); pnlButonlar.add(btnIptal);
        pnlSag.add(pnlButonlar, BorderLayout.SOUTH);

        splitPane.setRightComponent(pnlSag);
        add(splitPane, BorderLayout.CENTER);

        // Tıklama Olayı
        tablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablo.getSelectedRow() != -1) {
                seciliSiparisId = tabloModel.getValueAt(tablo.getSelectedRow(), 0).toString();
                // 3. kolonda (gizli) HTML var
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px;'>" + tabloModel.getValueAt(tablo.getSelectedRow(), 3).toString() + "</div>");
            }
        });

        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKart.addActionListener(e -> odemeAl("Kredi Kartı"));
        btnIptal.addActionListener(e -> iptalEt());
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_SIPARISLERI_GETIR|" + kuryeAdi);
            SwingUtilities.invokeLater(() -> {
                tabloModel.setColumnCount(3); tabloModel.setRowCount(0); tabloModel.addColumn("HTML_GIZLI"); 
                if (cevap != null && cevap.startsWith("KURYE_SIPARISLERI|") && cevap.length() > 18) {
                    String[] siparisler = cevap.substring(18).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue; String[] d = s.split("~_~"); 
                        if (d.length >= 4) tabloModel.addRow(new Object[]{d[0], d[1], d[2], d[3]});
                    }
                }
                tablo.getColumnModel().getColumn(3).setMinWidth(0); tablo.getColumnModel().getColumn(3).setMaxWidth(0); tablo.getColumnModel().getColumn(3).setWidth(0);
                txtFisDetay.setText(""); seciliSiparisId = "";
            });
        }).start();
    }

    private void odemeAl(String tur) {
        if(seciliSiparisId.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Paketi teslim edip " + tur + " ödeme aldığınızı onaylıyor musunuz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + tur));
            verileriYenile();
        }
    }

    private void iptalEt() {
        if(seciliSiparisId.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Siparişi teslim edilemedi/iptal olarak işaretliyorsunuz. Emin misiniz?", "İptal Onayı", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + seciliSiparisId + "|IPTAL"));
            verileriYenile();
        }
    }
}