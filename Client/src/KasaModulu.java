

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KasaModulu extends JPanel {
    private JFrame anaPanel;
    private DefaultTableModel aktifTableModel, gecmisTableModel;
    private JTable aktifTablo, gecmisTablo;
    private JEditorPane txtFisDetay; 
    private JPanel pnlButonlar;
    
    private JLabel lblOdenecekTutar;
    private String guncelSiparisTutari = "0.00";
    
    private JButton btnYolaCikti, btnIptal, btnNakit, btnKrediKarti;
    private String seciliSiparisId = "";
    private String seciliTur = "";

    public KasaModulu(JFrame anaPanel) {
        this.anaPanel = anaPanel;
        setLayout(new BorderLayout(10, 10)); 
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // ==========================================
        // ÜST BÖLÜM: BAŞLIK VE KURYE HESAP BUTONU
        // ==========================================
        JPanel pnlKasaUst = new JPanel(new BorderLayout());
        JLabel lblBaslik = new JLabel("💳 Kasa, Tahsilat ve Kurye Yönetimi");
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));
        
        JButton btnKuryeHesap = new JButton("🛵 Kurye Gün Sonu & Hesap Kesim");
        btnKuryeHesap.setBackground(new Color(142, 68, 173)); // Şık bir Mor Tema
        btnKuryeHesap.setForeground(Color.WHITE);
        btnKuryeHesap.setFont(new Font("Arial", Font.BOLD, 15));
        btnKuryeHesap.setFocusPainted(false);
        btnKuryeHesap.setPreferredSize(new Dimension(280, 45));
        btnKuryeHesap.addActionListener(e -> kuryeHesapEkraniAc());
        
        pnlKasaUst.add(lblBaslik, BorderLayout.WEST);
        pnlKasaUst.add(btnKuryeHesap, BorderLayout.EAST);
        add(pnlKasaUst, BorderLayout.NORTH);

        // ==========================================
        // İÇERİK BÖLÜMÜ (SPLIT PANE)
        // ==========================================
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        splitPane.setDividerLocation(650);

        JTabbedPane tabbedPane = new JTabbedPane(); 
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        aktifTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Mevcut Durum"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        aktifTablo = new JTable(aktifTableModel); 
        aktifTablo.setRowHeight(35); 
        aktifTablo.setFont(new Font("Arial", Font.PLAIN, 14)); 
        aktifTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel pnlAktif = new JPanel(new BorderLayout()); 
        pnlAktif.add(new JScrollPane(aktifTablo), BorderLayout.CENTER); 
        tabbedPane.addTab("🟢 Aktif / Bekleyen İşlemler", pnlAktif);

        gecmisTableModel = new DefaultTableModel(new String[]{"Sipariş No", "Tür / Masa", "Müşteri", "Son Durum"}, 0) { 
            @Override public boolean isCellEditable(int row, int column) { return false; } 
        };
        gecmisTablo = new JTable(gecmisTableModel); 
        gecmisTablo.setRowHeight(35); 
        gecmisTablo.setFont(new Font("Arial", Font.PLAIN, 14)); 
        gecmisTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel pnlGecmis = new JPanel(new BorderLayout()); 
        pnlGecmis.add(new JScrollPane(gecmisTablo), BorderLayout.CENTER); 
        tabbedPane.addTab("📜 Geçmiş İşlemler", pnlGecmis);
        
        splitPane.setLeftComponent(tabbedPane);

        // ==========================================
        // SAĞ TARAF: FİŞ DETAYLARI VE BUTONLAR
        // ==========================================
        JPanel pnlSag = new JPanel(new BorderLayout(10, 10)); 
        pnlSag.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Seçili Sipariş Detayı", 0, 0, new Font("Arial", Font.BOLD, 14)));
        
        txtFisDetay = new JEditorPane(); 
        txtFisDetay.setContentType("text/html"); 
        txtFisDetay.setEditable(false); 
        txtFisDetay.setBackground(new Color(255, 255, 240)); 
        pnlSag.add(new JScrollPane(txtFisDetay), BorderLayout.CENTER);

        lblOdenecekTutar = new JLabel("Lütfen Bir Sipariş Seçin", SwingConstants.CENTER);
        lblOdenecekTutar.setFont(new Font("Arial", Font.BOLD, 22));
        lblOdenecekTutar.setForeground(Color.GRAY);
        lblOdenecekTutar.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        pnlButonlar = new JPanel(new GridLayout(1, 3, 10, 10)); 
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        btnYolaCikti = new JButton("🛵 Kurye Seç & Yola Çıkar"); btnYolaCikti.setBackground(new Color(52, 152, 219)); btnYolaCikti.setForeground(Color.WHITE); btnYolaCikti.setFont(new Font("Arial", Font.BOLD, 14));
        btnIptal = new JButton("❌ İptal Et"); btnIptal.setBackground(new Color(192, 57, 43)); btnIptal.setForeground(Color.WHITE); btnIptal.setFont(new Font("Arial", Font.BOLD, 14));
        btnNakit = new JButton("💵 Nakit Al & Kapat"); btnNakit.setBackground(new Color(39, 174, 96)); btnNakit.setForeground(Color.WHITE); btnNakit.setFont(new Font("Arial", Font.BOLD, 14));
        btnKrediKarti = new JButton("💳 K.Kartı & Kapat"); btnKrediKarti.setBackground(new Color(41, 128, 185)); btnKrediKarti.setForeground(Color.WHITE); btnKrediKarti.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel pnlAltGrup = new JPanel(new BorderLayout());
        pnlAltGrup.add(lblOdenecekTutar, BorderLayout.NORTH);
        pnlAltGrup.add(pnlButonlar, BorderLayout.CENTER);

        pnlSag.add(pnlAltGrup, BorderLayout.SOUTH); 
        splitPane.setRightComponent(pnlSag); 
        add(splitPane, BorderLayout.CENTER);

        // ==========================================
        // TABLO TIKLAMA OLAYLARI VE FİYAT HESAPLAMA
        // ==========================================
        aktifTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && aktifTablo.getSelectedRow() != -1) {
                gecmisTablo.clearSelection(); 
                seciliSiparisId = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 0).toString();
                seciliTur = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 1).toString();
                String html = aktifTableModel.getValueAt(aktifTablo.getSelectedRow(), 4).toString();
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px;'>" + html + "</div>");
                
                guncelSiparisTutari = fiyatiSok(html);
                lblOdenecekTutar.setText("Toplam Ödenecek Tutar: " + guncelSiparisTutari + " TL");
                lblOdenecekTutar.setForeground(new Color(192, 57, 43)); 
                
                butonlariDuzenle(seciliTur, true); 
            }
        });

        gecmisTablo.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && gecmisTablo.getSelectedRow() != -1) {
                aktifTablo.clearSelection(); 
                seciliSiparisId = gecmisTableModel.getValueAt(gecmisTablo.getSelectedRow(), 0).toString();
                String html = gecmisTableModel.getValueAt(gecmisTablo.getSelectedRow(), 4).toString();
                txtFisDetay.setText("<div style='font-family: Arial; padding: 10px; opacity: 0.6;'>" + html + "</div>");
                
                guncelSiparisTutari = fiyatiSok(html);
                lblOdenecekTutar.setText("Tahsil Edilen Tutar: " + guncelSiparisTutari + " TL");
                lblOdenecekTutar.setForeground(new Color(39, 174, 96)); 
                
                butonlariDuzenle("", false); 
            }
        });

        tabbedPane.addChangeListener(e -> { 
            aktifTablo.clearSelection(); gecmisTablo.clearSelection(); txtFisDetay.setText(""); 
            lblOdenecekTutar.setText("Lütfen Bir Sipariş Seçin"); lblOdenecekTutar.setForeground(Color.GRAY);
            guncelSiparisTutari = "0.00"; butonlariDuzenle("", false); 
        });

        // ==========================================
        // YENİ VE ŞIK KURYE ATAMA EKRANI (Üstte İsim, Altta Fiş)
        // ==========================================
        btnYolaCikti.addActionListener(e -> {
            if(seciliSiparisId.isEmpty()) return;
            int row = aktifTablo.getSelectedRow();
            if (!aktifTableModel.getValueAt(row, 3).toString().equals("HAZIR")) {
                JOptionPane.showMessageDialog(this, "Sipariş mutfakta 'HAZIR' olmadan kuryeye atanamaz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String kuryelerCvp = sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
            if(kuryelerCvp != null && kuryelerCvp.startsWith("KURYE_LISTESI|")) {
                String[] split = kuryelerCvp.split("\\|");
                if (split.length <= 1) { JOptionPane.showMessageDialog(this, "Sistemde Kayıtlı Kurye Bulunamadı!"); return; }
                
                String[] kuryeListesi = new String[split.length - 1];
                System.arraycopy(split, 1, kuryeListesi, 0, split.length - 1);
                
                JDialog d = new JDialog(anaPanel, "🛵 Kurye Atama ve Yönlendirme", true);
                d.setSize(450, 600);
                d.setLayout(new BorderLayout(10, 10));
                d.setLocationRelativeTo(this);

                JPanel pnlUst = new JPanel(new BorderLayout(5, 5));
                pnlUst.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                pnlUst.setBackground(new Color(52, 152, 219));
                
                JLabel lblBilgi = new JLabel("<html><center><font color='white' size='5'><b>Teslim Edecek Personeli Seçin</b></font></center></html>", SwingConstants.CENTER);
                JComboBox<String> cbKuryeler = new JComboBox<>(kuryeListesi);
                cbKuryeler.setFont(new Font("Arial", Font.BOLD, 18));
                
                pnlUst.add(lblBilgi, BorderLayout.NORTH);
                pnlUst.add(cbKuryeler, BorderLayout.CENTER);

                // Altta Sipariş (Fiş) Detayı
                String htmlFis = aktifTableModel.getValueAt(row, 4).toString();
                JEditorPane txtDetay = new JEditorPane("text/html", "<div style='font-family:Arial; font-size:15px; padding:10px;'>" + htmlFis + "</div>");
                txtDetay.setEditable(false);
                JScrollPane scrollFis = new JScrollPane(txtDetay);
                scrollFis.setBorder(BorderFactory.createTitledBorder("📦 Gönderilecek Sipariş ve Adres İçeriği"));

                JButton btnAta = new JButton("Seçili Kuryeyi Ata ve Yola Çıkar");
                btnAta.setBackground(new Color(39, 174, 96));
                btnAta.setForeground(Color.WHITE);
                btnAta.setFont(new Font("Arial", Font.BOLD, 18));
                btnAta.setPreferredSize(new Dimension(0, 55));
                btnAta.setFocusPainted(false);
                btnAta.addActionListener(e2 -> {
                    String secilen = (String) cbKuryeler.getSelectedItem();
                    JOptionPane.showMessageDialog(d, sunucuyaKomutGonderVeCevapAl("KURYE_ATA|" + seciliSiparisId + "|" + secilen));
                    verileriYenile();
                    d.dispose();
                });

                d.add(pnlUst, BorderLayout.NORTH);
                d.add(scrollFis, BorderLayout.CENTER);
                d.add(btnAta, BorderLayout.SOUTH);
                d.setVisible(true);
            }
        });

        btnIptal.addActionListener(e -> durumGuncelle("IPTAL"));
        btnNakit.addActionListener(e -> odemeAl("Nakit"));
        btnKrediKarti.addActionListener(e -> odemeAl("Kredi Kartı"));
    }

    // ==========================================
    // YENİ: KURYE VARDİYA / HESAP KESİM PANELİ
    // ==========================================
    private void kuryeHesapEkraniAc() {
        String kuryelerCvp = sunucuyaKomutGonderVeCevapAl("KURYELERI_GETIR");
        if(kuryelerCvp != null && kuryelerCvp.startsWith("KURYE_LISTESI|")) {
            String[] split = kuryelerCvp.split("\\|");
            if (split.length <= 1) { JOptionPane.showMessageDialog(this, "Sistemde kurye yok."); return; }
            
            String[] kuryeListesi = new String[split.length - 1];
            System.arraycopy(split, 1, kuryeListesi, 0, split.length - 1);
            
            String secilenKurye = (String) JOptionPane.showInputDialog(this, "Günlük hesabı (Vardiya Özeti) görüntülenecek kuryeyi seçin:", 
                                  "Kurye Hesap Kesimi", JOptionPane.QUESTION_MESSAGE, null, kuryeListesi, kuryeListesi[0]);
                                  
            if (secilenKurye != null) {
                String hesapCvp = sunucuyaKomutGonderVeCevapAl("KURYE_HESAP_GETIR|" + secilenKurye);
                if (hesapCvp != null && hesapCvp.startsWith("KURYE_HESAP|")) {
                    String[] h = hesapCvp.split("\\|");
                    
                    String msg = "<html><div style='width:350px; font-family:Arial;'>" +
                                 "<h2 style='color:#8e44ad; border-bottom: 2px solid #ccc;'>🛵 " + secilenKurye + " - Bugünün Özeti</h2>" +
                                 "<font size='4'><b>Teslim Edilen Sipariş:</b> " + h[1] + " Adet</font><br><br>" +
                                 "<font size='5'><b>Toplanan Nakit:</b> <font color='green'>" + h[2] + " TL</font></font><br>" +
                                 "<font size='5'><b>Kredi Kartı (Pos):</b> <font color='blue'>" + h[3] + " TL</font></font><hr>" +
                                 "<h3 style='color:#333;'>Son Teslimat Geçmişi:</h3>" +
                                 "<div style='font-size:12px; color:#555;'>" + h[4] + "</div>" +
                                 "</div></html>";
                                 
                    JOptionPane.showMessageDialog(this, msg, "Kurye Vardiya Hesabı", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Kurye listesi alınamadı!");
        }
    }

    // ==========================================
    // GÜÇLENDİRİLMİŞ FİYAT OKUMA MOTORU (Hata Düzeltmeleri Eklendi)
    // ==========================================
    public static String fiyatiSok(String htmlFis) {
        if (htmlFis == null || htmlFis.isEmpty()) return "0.00";
        try {
            Matcher m1 = Pattern.compile("<!\\-\\-PRICE([0-9.,]+)").matcher(htmlFis);
            if (m1.find()) return m1.group(1).replace(",", ".").trim();

            Matcher m2 = Pattern.compile("Genel Toplam:.*?([0-9]+[.,][0-9]{1,2})").matcher(htmlFis);
            if (m2.find()) return m2.group(1).replace(",", ".").trim();

            String temizMetin = htmlFis.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ");
            // (?<!\d) eklentisi sayesinde, fiyat görünümlü adres veya telefon numaralarını pas geçer, sadece TL ile biten gerçek sayıları bulur.
            Matcher m3 = Pattern.compile("(?<!\\d)([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:TL|₺)", Pattern.CASE_INSENSITIVE).matcher(temizMetin);
            String sonFiyat = "0.00";
            while (m3.find()) sonFiyat = m3.group(1);
            
            return sonFiyat.replace(",", ".").trim();
        } catch (Exception e) { return "0.00"; }
    }

    private void butonlariDuzenle(String tur, boolean aktifMi) {
        pnlButonlar.removeAll();
        if (aktifMi) {
            if (tur.contains("Eve") || tur.contains("EVE")) {
                btnYolaCikti.setText("🛵 Kurye Seç & Yola Çıkar"); pnlButonlar.add(btnYolaCikti); pnlButonlar.add(btnIptal);
            } else if (tur.contains("Paket") || tur.contains("PAKET")) {
                btnNakit.setText("📦 Teslim Et & Nakit Al"); btnKrediKarti.setText("📦 Teslim Et & Kart Al");
                pnlButonlar.add(btnNakit); pnlButonlar.add(btnKrediKarti); pnlButonlar.add(btnIptal);
            } else {
                btnNakit.setText("💵 Nakit Al & Kapat"); btnKrediKarti.setText("💳 K.Kartı & Kapat");
                pnlButonlar.add(btnNakit); pnlButonlar.add(btnKrediKarti); pnlButonlar.add(btnIptal);
            }
        }
        pnlButonlar.revalidate(); pnlButonlar.repaint();
    }

    public void verileriYenile() {
        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                aktifTableModel.setColumnCount(4); aktifTableModel.setRowCount(0); aktifTableModel.addColumn("HTML_GIZLI"); 
                if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
                    String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue; String[] d = s.split("~_~"); 
                        if (d.length >= 5) aktifTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                    }
                }
                aktifTablo.getColumnModel().getColumn(4).setMinWidth(0); aktifTablo.getColumnModel().getColumn(4).setMaxWidth(0); aktifTablo.getColumnModel().getColumn(4).setWidth(0);
            });
        }).start();

        new Thread(() -> {
            String cevap = sunucuyaKomutGonderVeCevapAl("KASA_GECMIS_GETIR");
            SwingUtilities.invokeLater(() -> {
                gecmisTableModel.setColumnCount(4); gecmisTableModel.setRowCount(0); gecmisTableModel.addColumn("HTML_GIZLI"); 
                if (cevap != null && cevap.startsWith("KASA_GECMIS_VERI|") && cevap.length() > 17) {
                    String[] siparisler = cevap.substring(17).split("\\|\\|\\|");
                    for (String s : siparisler) {
                        if (s.trim().isEmpty()) continue; String[] d = s.split("~_~"); 
                        if (d.length >= 5) gecmisTableModel.addRow(new Object[]{d[0], d[1], d[2], d[3], d[4]});
                    }
                }
                gecmisTablo.getColumnModel().getColumn(4).setMinWidth(0); gecmisTablo.getColumnModel().getColumn(4).setMaxWidth(0); gecmisTablo.getColumnModel().getColumn(4).setWidth(0);
            });
        }).start();
        
        txtFisDetay.setText(""); lblOdenecekTutar.setText("Lütfen Bir Sipariş Seçin"); lblOdenecekTutar.setForeground(Color.GRAY); butonlariDuzenle("", false);
    }

    private void durumGuncelle(String yeniDurum) {
        if(seciliSiparisId.isEmpty()) return;
        if(JOptionPane.showConfirmDialog(this, "Bu siparişi iptal etmek istediğinize emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("SIPARIS_DURUM_GUNCELLE|" + seciliSiparisId + "|" + yeniDurum));
            if ((yeniDurum.equals("IPTAL") || yeniDurum.equals("YOLA_CIKTI")) && !seciliTur.contains("Müşteri")) masayiSifirla(seciliTur);
            verileriYenile();
        }
    }

    private void odemeAl(String odemeTuru) {
        if(seciliSiparisId.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Toplam " + guncelSiparisTutari + " TL " + odemeTuru + " olarak tahsil edilecek.\nOnaylıyor musunuz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + seciliSiparisId + "|" + odemeTuru + "|" + guncelSiparisTutari));
            if (!seciliTur.contains("Müşteri")) masayiSifirla(seciliTur); 
            verileriYenile();
        }
    }

    private String sunucuyaKomutGonderVeCevapAl(String komut) {
        if (anaPanel instanceof PersonelPaneli) return ((PersonelPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(komut);
        else if (anaPanel instanceof AdminPaneli) return ((AdminPaneli) anaPanel).sunucuyaKomutGonderVeCevapAl(komut);
        return null;
    }

    private void masayiSifirla(String masaAdi) {
        if (anaPanel instanceof PersonelPaneli) ((PersonelPaneli) anaPanel).masayiSifirla(masaAdi);
    }
}