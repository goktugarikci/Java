
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class KuryeModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private String kuryeAdi;
    private DefaultTableModel model;
    private JTable tablo;
    private JEditorPane txtDetay;
    private String seciliOrderId = "";

    public KuryeModulu(PersonelPaneli anaPanel, String kuryeAdi) {
        this.anaPanel = anaPanel;
        this.kuryeAdi = kuryeAdi;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(450);

        // SOL: SİPARİŞ LİSTESİ
        model = new DefaultTableModel(new String[]{"Sipariş No", "Müşteri Adı", "Durum"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablo = new JTable(model);
        tablo.setRowHeight(35);
        
        JPanel pnlSol = new JPanel(new BorderLayout(5,5));
        pnlSol.add(new JLabel("🏍️ Bana Atanan Teslimatlar"), BorderLayout.NORTH);
        pnlSol.add(new JScrollPane(tablo), BorderLayout.CENTER);
        
        JButton btnYenile = new JButton("🔄 Listeyi Yenile");
        btnYenile.addActionListener(e -> verileriYenile());
        pnlSol.add(btnYenile, BorderLayout.SOUTH);
        split.setLeftComponent(pnlSol);

        // SAĞ: DETAY VE BUTONLAR
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10));
        txtDetay = new JEditorPane();
        txtDetay.setContentType("text/html");
        txtDetay.setEditable(false);
        pnlSag.add(new JScrollPane(txtDetay), BorderLayout.CENTER);

        JPanel pnlAlt = new JPanel(new GridLayout(1, 3, 5, 5));
        JButton btnNakit = new JButton("✅ Nakit Tahsil Edildi");
        btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE);
        
        JButton btnKart = new JButton("💳 K.Kartı Tahsil Edildi");
        btnKart.setBackground(new Color(41, 128, 185)); btnKart.setForeground(Color.WHITE);
        
        JButton btnHata = new JButton("❌ Teslim Edilemedi");
        btnHata.setBackground(new Color(192, 57, 43)); btnHata.setForeground(Color.WHITE);

        pnlAlt.add(btnNakit); pnlAlt.add(btnKart); pnlAlt.add(btnHata);
        pnlSag.add(pnlAlt, BorderLayout.SOUTH);
        split.setRightComponent(pnlSag);

        add(split, BorderLayout.CENTER);

        // --- OLAYLAR ---
        tablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablo.getSelectedRow() != -1) {
                seciliOrderId = model.getValueAt(tablo.getSelectedRow(), 0).toString();
                String html = (String) tablo.getClientProperty("html_" + seciliOrderId);
                txtDetay.setText(html);
            }
        });

        // Teslimat Onay Butonları
        btnNakit.addActionListener(e -> teslimatKapat("Nakit"));
        btnKart.addActionListener(e -> teslimatKapat("Kredi Kartı"));
        btnHata.addActionListener(e -> teslimatKapat("HATA_İPTAL"));
    }

    private void teslimatKapat(String tur) {
        if (seciliOrderId.isEmpty()) return;
        
        String onayMesaji = tur.equals("HATA_İPTAL") ? "Teslim edilemedi olarak işaretlensin mi?" : "Ödeme alındı ve teslim edildi olarak işaretlensin mi?";
        if (JOptionPane.showConfirmDialog(this, onayMesaji, "Onay", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            
            String komut;
            if (tur.equals("HATA_İPTAL")) {
                komut = "SIPARIS_DURUM_GUNCELLE|" + seciliOrderId + "|IPTAL";
            } else {
                // Sunucuda bu komut siparişi ödenmiş ve teslim edilmiş yapar
                komut = "SIPARIS_ODEME_AL|" + seciliOrderId + "|" + tur;
            }
            
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl(komut);
            JOptionPane.showMessageDialog(this, cvp);
            
            seciliOrderId = "";
            txtDetay.setText("");
            verileriYenile();
        }
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("KURYE_SIPARISLERI_GETIR|" + kuryeAdi);
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                if (cvp != null && cvp.startsWith("KURYE_VERI|") && cvp.length() > 11) {
                    String[] siparisler = cvp.substring(11).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        String[] d = s.split("~_~");
                        if (d.length >= 4) {
                            model.addRow(new Object[]{d[0], d[1], d[2]});
                            tablo.putClientProperty("html_" + d[0], d[3]);
                        }
                    }
                }
            });
        }).start();
    }
}