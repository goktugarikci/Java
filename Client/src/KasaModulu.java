

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class KasaModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private DefaultTableModel siparisTableModel;
    private JTable siparisTablosu;
    private JEditorPane txtFisDetay; 
    private String seciliSiparisId = "";

    public KasaModulu(PersonelPaneli anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);

        JPanel pnlSol = new JPanel(new BorderLayout());
        pnlSol.setBorder(BorderFactory.createTitledBorder("Aktif Siparişler (Paket, Eve Servis ve Masalar)"));
        
        siparisTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Mevcut Durum"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        siparisTablosu = new JTable(siparisTableModel);
        siparisTablosu.setRowHeight(35);
        siparisTablosu.setFont(new Font("Arial", Font.PLAIN, 14));
        siparisTablosu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pnlSol.add(new JScrollPane(siparisTablosu), BorderLayout.CENTER);

        JPanel pnlSag = new JPanel(new BorderLayout(10, 10));
        pnlSag.setBorder(BorderFactory.createTitledBorder("Seçili Sipariş Detayı ve İşlemler"));

        txtFisDetay = new JEditorPane();
        txtFisDetay.setContentType("text/html");
        txtFisDetay.setEditable(false);
        txtFisDetay.setBackground(new Color(255, 255, 240)); 
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        JPanel pnlButonlar = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton btnYolaCikti = new JButton("🛵 Kuryeye Ver (Yola Çıktı)");
        btnYolaCikti.setBackground(new Color(52, 152, 219)); btnYolaCikti.setForeground(Color.WHITE);
        
        JButton btnIptal = new JButton("❌ Siparişi İptal Et");
        btnIptal.setBackground(new Color(149, 165, 166)); btnIptal.setForeground(Color.WHITE);

        JButton btnNakit = new JButton("💵 Nakit Ödeme Al & Kapat");
        btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE); btnNakit.setFont(new Font("Arial", Font.BOLD, 14));

        JButton btnKrediKarti = new JButton("💳 Kredi Kartı Al & Kapat");
        btnKrediKarti.setBackground(new Color(41, 128, 185)); btnKrediKarti.setForeground(Color.WHITE); btnKrediKarti.setFont(new Font("Arial", Font.BOLD, 14));

        pnlButonlar.add(btnYolaCikti); pnlButonlar.add(btnNakit);
        pnlButonlar.add(btnIptal); pnlButonlar.add(btnKrediKarti);
        pnlSag.add(pnlButonlar, BorderLayout.SOUTH);

        splitPane.setLeftComponent(pnlSol);
        splitPane.setRightComponent(pnlSag);
        add(splitPane, BorderLayout.CENTER);

        siparisTablosu.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && siparisTablosu.getSelectedRow() != -1) {
                seciliSiparisId = siparisTableModel.getValueAt(siparisTablosu.getSelectedRow(), 0).toString();
                String fisHtml = siparisTableModel.getValueAt(siparisTablosu.getSelectedRow(), 4).toString();
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px;'>" + fisHtml + "</div>");
            }
        });

        btnYolaCikti.addActionListener(e -> durumGuncelle("YOLA_CIKTI"));
        btnIptal.addActionListener(e -> durumGuncelle("IPTAL"));
        
        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKrediKarti.addActionListener(e -> odemeAl("Kredi Kartı"));
    }

    public void verileriYenile() {
        String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
        if (cevap != null && cevap.startsWith("KASA_VERI|")) {
            SwingUtilities.invokeLater(() -> {
                siparisTableModel.setColumnCount(4); 
                siparisTableModel.setRowCount(0);
                siparisTableModel.addColumn("HTML_GIZLI"); 

                if (cevap.length() > 10) {
                    String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue;
                        String[] d = s.split("~_~"); 
                        if (d.length == 5) {
                            siparisTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                        }
                    }
                }
                siparisTablosu.getColumnModel().getColumn(4).setMinWidth(0);
                siparisTablosu.getColumnModel().getColumn(4).setMaxWidth(0);
                siparisTablosu.getColumnModel().getColumn(4).setWidth(0);
                
                txtFisDetay.setText(""); seciliSiparisId = "";
            });
        }
    }

    private void durumGuncelle(String yeniDurum) {
        if(seciliSiparisId.isEmpty()) { JOptionPane.showMessageDialog(this, "Önce bir sipariş seçin!"); return; }
        String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + seciliSiparisId + "|" + yeniDurum);
        JOptionPane.showMessageDialog(this, cvp);
        
        // İptal edilirse masayı da sıfırlıyoruz.
        if (yeniDurum.equals("IPTAL")) {
            String masaAdi = siparisTableModel.getValueAt(siparisTablosu.getSelectedRow(), 1).toString();
            anaPanel.masayiSifirla(masaAdi);
        }
        verileriYenile();
    }

    private void odemeAl(String odemeTuru) {
        if(seciliSiparisId.isEmpty()) { JOptionPane.showMessageDialog(this, "Önce ödenecek siparişi seçin!"); return; }
        
        int onay = JOptionPane.showConfirmDialog(this, "Sipariş " + odemeTuru + " olarak kapatılacak. Onaylıyor musunuz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION);
        if (onay == JOptionPane.YES_OPTION) {
            String cvp = anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + odemeTuru);
            JOptionPane.showMessageDialog(this, cvp);
            
            // KRİTİK DEĞİŞİKLİK: Masa isminde "Masa" kelimesi aramıyoruz, adı "test" bile olsa masayı kapatıyoruz.
            String masaAdi = siparisTableModel.getValueAt(siparisTablosu.getSelectedRow(), 1).toString();
            anaPanel.masayiSifirla(masaAdi); 
            
            verileriYenile();
        }
    }
}