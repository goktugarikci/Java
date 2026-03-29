
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SiparisModulu extends JPanel {
    private PersonelPaneli anaPanel;
    private String aktifPersonel;
    private String aktifRol;

    private JPanel masalarPaneli;
    private Map<String, Long> siparisZamanlari = new HashMap<>(); 
    private Map<String, String> masaDurumlari = new HashMap<>(); 
    private Map<String, String> bugunkuRezervasyonlar = new HashMap<>();

    public SiparisModulu(PersonelPaneli anaPanel, String aktifPersonel, String aktifRol) {
        this.anaPanel = anaPanel;
        this.aktifPersonel = aktifPersonel;
        this.aktifRol = aktifRol;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel baslikPanel = new JPanel(new BorderLayout());
        JPanel pnlSolBaslik = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblBaslik = new JLabel("Aktif Masalar ve Sipariş Yönetimi"); 
        lblBaslik.setFont(new Font("Arial", Font.BOLD, 22));

        JButton btnYenile = new JButton("🔄 Masaları Yenile");
        btnYenile.setBackground(new Color(52, 152, 219)); 
        btnYenile.setForeground(Color.WHITE); 
        btnYenile.setFocusPainted(false);
        btnYenile.addActionListener(e -> baslat());

        pnlSolBaslik.add(lblBaslik); 
        pnlSolBaslik.add(btnYenile); 
        baslikPanel.add(pnlSolBaslik, BorderLayout.WEST);

        if(!aktifRol.equalsIgnoreCase("Garson")) {
            JPanel pnlHizliSiparis = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPaket = new JButton("📦 Gel-Al Paket Sipariş"); 
            JButton btnEveServis = new JButton("🛵 Eve Servis (Telefon)");

            btnPaket.setBackground(new Color(230, 126, 34)); 
            btnPaket.setForeground(Color.WHITE); 
            btnPaket.setFont(new Font("Arial", Font.BOLD, 14));
            
            btnEveServis.setBackground(new Color(41, 128, 185)); 
            btnEveServis.setForeground(Color.WHITE); 
            btnEveServis.setFont(new Font("Arial", Font.BOLD, 14));

            // NOT: Adisyon Ekrani metodu geçici olarak PersonelPaneli'nden çağrılır.
            btnPaket.addActionListener(e -> new AdisyonEkrani(anaPanel, SiparisModulu.this, aktifPersonel, "PAKET", "Paket Müşterisi").setVisible(true)); 
            btnEveServis.addActionListener(e -> new AdisyonEkrani(anaPanel, SiparisModulu.this, aktifPersonel, "EVE_SERVIS", "Eve Servis Müşterisi").setVisible(true));

            pnlHizliSiparis.add(btnPaket); 
            pnlHizliSiparis.add(btnEveServis); 
            baslikPanel.add(pnlHizliSiparis, BorderLayout.EAST);
        }

        masalarPaneli = new JPanel(new GridLayout(0, 4, 15, 15)); 
        masalarPaneli.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel masaSarici = new JPanel(new BorderLayout()); 
        masaSarici.add(masalarPaneli, BorderLayout.NORTH); 

        add(baslikPanel, BorderLayout.NORTH); 
        add(new JScrollPane(masaSarici), BorderLayout.CENTER);
        
        masaRenkGuncelleyiciyiBaslat();
    }

    public void baslat() {
        gercekMasalariSunucudanCek(); 
        masalariVeritabanindanGeriYukle(); 
        bugunkuRezervasyonlariGuncelle();
    }

    private void gercekMasalariSunucudanCek() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("MASALARI_GETIR");
            if (cevap != null && cevap.startsWith("MASA_LISTESI")) {
                String[] parcalar = cevap.split("\\|");
                SwingUtilities.invokeLater(() -> {
                    masalarPaneli.removeAll(); 
                    for (int i = 1; i < parcalar.length; i++) {
                        String masaIsmi = parcalar[i].split(";")[0]; 
                        JButton masaButon = new JButton(masaIsmi); 
                        masaButon.setName(masaIsmi); 
                        masaButon.setFont(new Font("Arial", Font.BOLD, 18)); 
                        masaButon.setBackground(new Color(236, 240, 241)); 
                        masaButon.setFocusPainted(false); 
                        masaButon.setPreferredSize(new Dimension(200, 150)); 
                        
                        masaButon.addActionListener(e -> {
                            String durum = masaDurumlari.getOrDefault(masaIsmi, "BOS");
                            
                            if (durum.equals("BOS") && bugunkuRezervasyonlar.containsKey(masaIsmi)) {
                                int onay = JOptionPane.showConfirmDialog(this, masaIsmi + " rezerve edilmiş.\nMüşteri geldi mi? (Siparişe Başla)", "Rezerve Masa", JOptionPane.YES_NO_OPTION);
                                if(onay == JOptionPane.YES_OPTION) new AdisyonEkrani(anaPanel, SiparisModulu.this, aktifPersonel, "MASA", masaIsmi).setVisible(true);;
                                return;
                            }

                            if (!durum.equals("BOS") && !aktifRol.equalsIgnoreCase("Garson")) {
                                Object[] options = {"📝 Ek Sipariş Gir", "💵 Hesabı Kapat (Nakit)", "💳 Hesabı Kapat (Kredi Kartı)", "❌ Vazgeç"};
                                int secim = JOptionPane.showOptionDialog(this, masaIsmi + " şu an aktif.\nNe yapmak istersiniz?", "Masa İşlemleri", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                                if (secim == 0) {
                                    new AdisyonEkrani(anaPanel, SiparisModulu.this, aktifPersonel, "MASA", masaIsmi).setVisible(true);
                                } else if (secim == 1 || secim == 2) {
                                    String odemeTuru = (secim == 1) ? "Nakit" : "Kredi Kartı";
                                    ArrayList<String> orderIds = getMasaAdisyonIDleri(masaIsmi);
                                    if (!orderIds.isEmpty()) {
                                        int onay = JOptionPane.showConfirmDialog(this, masaIsmi + " hesabı " + odemeTuru + " olarak kapatılacak. Emin misiniz?", "Ödeme Onayı", JOptionPane.YES_NO_OPTION);
                                        if (onay == JOptionPane.YES_OPTION) {
                                            for(String id : orderIds) {
                                                anaPanel.sunucuyaKomutGonderVeCevapAl("SIPARIS_ODEME_AL|" + id + "|" + odemeTuru);
                                            }
                                            JOptionPane.showMessageDialog(this, "Hesap kapatıldı!");
                                            masayiSifirla(masaIsmi); 
                                            anaPanel.kasaGuncelle();
                                        }
                                    } else {
                                        JOptionPane.showMessageDialog(this, "Aktif bir sipariş bulunamadı!");
                                    }
                                }
                            } else {
                                new AdisyonEkrani(anaPanel, SiparisModulu.this, aktifPersonel, "MASA", masaIsmi).setVisible(true);
                            }
                        });
                        masalarPaneli.add(masaButon);
                    }
                    masalarPaneli.revalidate(); 
                    masalarPaneli.repaint();
                    arayuzuGuncelle(); 
                });
            }
        }).start();
    }

    private ArrayList<String> getMasaAdisyonIDleri(String masaIsmi) {
        ArrayList<String> ids = new ArrayList<>();
        String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("KASA_SIPARIS_GETIR");
        if (cevap != null && cevap.startsWith("KASA_VERI|") && cevap.length() > 10) {
            String[] siparisler = cevap.substring(10).split("\\|\\|\\|");
            for (String s : siparisler) {
                if (s.trim().isEmpty()) continue;
                String[] d = s.split("~_~"); 
                if (d.length >= 5 && d[1].equals(masaIsmi)) ids.add(d[0]); 
            }
        } 
        return ids;
    }

    private void masalariVeritabanindanGeriYukle() {
        new Thread(() -> {
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("AKTIF_MASALARI_GETIR");
            if (cevap != null && cevap.startsWith("AKTIF_MASALAR|")) {
                Map<String, String> yeniDurumlar = new HashMap<>();
                Map<String, Long> yeniZamanlar = new HashMap<>();
                
                if(cevap.length() > 14) {
                    String[] masalar = cevap.substring(14).split("\\|\\|\\|");
                    for (String m : masalar) {
                        if (m.trim().isEmpty()) continue;
                        String[] detay = m.split("~_~"); 
                        if (detay.length == 3) {
                            String masaAdi = detay[0]; 
                            String durum = detay[1]; 
                            String zamanStr = detay[2]; 
                            
                            long msZaman = System.currentTimeMillis();
                            try { 
                                msZaman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(zamanStr).getTime(); 
                            } catch(Exception ignored) {}
                            
                            if(durum.equals("BEKLEMEDE") || durum.equals("HAZIRLANIYOR")) {
                                yeniDurumlar.put(masaAdi, "HAZIRLANIYOR");
                            } else if (durum.equals("HAZIR") || durum.equals("TESLIM_EDILDI")) {
                                yeniDurumlar.put(masaAdi, "TESLIM_EDILDI");
                            }
                            yeniZamanlar.put(masaAdi, msZaman);
                        }
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    masaDurumlari.clear();
                    masaDurumlari.putAll(yeniDurumlar);
                    
                    siparisZamanlari.clear();
                    siparisZamanlari.putAll(yeniZamanlar);
                    arayuzuGuncelle();
                });
            }
        }).start();
    }

    private void bugunkuRezervasyonlariGuncelle() {
        new Thread(() -> {
            String bugun = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String cevap = anaPanel.sunucuyaKomutGonderVeCevapAl("BUGUN_REZ_GETIR|" + bugun);
            
            if (cevap != null && cevap.startsWith("BUGUN_REZ|")) {
                Map<String, String> yeniRezler = new HashMap<>();
                if(cevap.length() > 10) {
                    String[] rezler = cevap.substring(10).split("\\|\\|\\|");
                    for (String r : rezler) {
                        if (r.trim().isEmpty()) continue;
                        String[] d = r.split("~_~"); 
                        if(d.length == 3) yeniRezler.put(d[0], d[1] + " - " + d[2]); 
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    bugunkuRezervasyonlar.clear();
                    bugunkuRezervasyonlar.putAll(yeniRezler);
                    arayuzuGuncelle();
                });
            }
        }).start();
    }

    public void arayuzuGuncelle() {
        long suan = System.currentTimeMillis();
        if (masalarPaneli != null) {
            for (Component c : masalarPaneli.getComponents()) {
                if (c instanceof JButton) {
                    guncelleMasaButonu((JButton) c, suan);
                }
            }
        }
    }

    private void masaRenkGuncelleyiciyiBaslat() {
        Timer masaTimer = new Timer(5000, e -> { 
            bugunkuRezervasyonlariGuncelle(); 
            masalariVeritabanindanGeriYukle(); 
        });
        masaTimer.start();
    }

    private void guncelleMasaButonu(JButton btn, long suan) {
        String mName = btn.getName();
        if (mName == null) return;
        
        String durum = masaDurumlari.getOrDefault(mName, "BOS");
        Long acilisZamani = siparisZamanlari.get(mName);
        long acilis = (acilisZamani != null) ? acilisZamani : suan; 

        if (durum.equals("BOS")) {
            String rezBilgisi = bugunkuRezervasyonlar.get(mName);
            if (rezBilgisi != null) {
                btn.setText("<html><center>" + mName + "<br><font size='3' color='white'><b>REZERVE</b><br>" + rezBilgisi + "</font></center></html>");
                btn.setBackground(new Color(142, 68, 173)); 
            } else {
                btn.setText(mName); 
                btn.setBackground(new Color(236, 240, 241)); 
            }
        } else {
            long farkMs = suan - acilis;
            long dk = Math.max(0, farkMs / (60 * 1000));
            String saatStr = new SimpleDateFormat("HH:mm").format(new Date(acilis));
            
            btn.setText("<html><center>" + mName + "<br><font size='3'>Giriş: " + saatStr + "<br><b>" + dk + " dk</b></font></center></html>");

            if (durum.equals("HAZIRLANIYOR")) {
                if (dk >= 10) btn.setBackground(new Color(231, 76, 60)); 
                else btn.setBackground(new Color(46, 204, 113)); 
            } else if (durum.equals("TESLIM_EDILDI")) {
                btn.setBackground(new Color(243, 156, 18)); 
            }
        }
    }

    public void masayiSifirla(String masaAdi) {
        siparisZamanlari.remove(masaAdi); 
        masaDurumlari.put(masaAdi, "BOS");
        arayuzuGuncelle();
    }
    
    public Map<String, String> getMasaDurumlari() {
        return masaDurumlari;
    }
}