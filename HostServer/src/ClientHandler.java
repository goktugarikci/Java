package src; // Eğer VS Code hata verirse bu satırı silmeyi unutma!

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // 1. İstemciye karşılama mesajı gönderiyoruz
            out.println("BAĞLANTI_BASARILI|Sunucuya hoş geldiniz.");

            while (true) { // Sürekli dinleme döngüsü (Bağlantı kopana kadar)
                String komut = in.readLine();
                if (komut == null || komut.equalsIgnoreCase("exit")) {
                    break;
                }

                // --- KONSOLA LOG (KAYIT) YAZDIRMA: GELEN KOMUT ---
                String zaman = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("[" + zaman + "] [İSTEMCİ: " + ip + "] GELEN KOMUT: " + komut);

                // İşlemi yap ve cevabı al
                String cevap = islemYap(komut);

                // --- KONSOLA LOG (KAYIT) YAZDIRMA: GİDEN CEVAP ---
                // Fiş HTML'i gibi çok uzun metinler konsolu kirletmesin diye kırparak yazdırıyoruz
                String kisaCevap = cevap.length() > 150 ? cevap.substring(0, 150) + "... [DEVAMI GİZLENDİ]" : cevap;
                System.out.println("[" + zaman + "] [SUNUCU CEVABI] -> " + kisaCevap + "\n");

                // Sonucu istemciye geri gönder
                out.println(cevap); 
            }

        } catch (Exception e) {
            String zaman = new SimpleDateFormat("HH:mm:ss").format(new Date());
            System.err.println("[" + zaman + "] [BİLGİ] Bir istemci bağlantıyı kopardı: " + e.getMessage());
        }
    }

    private String islemYap(String komut) {
        // split limitini -1 yapıyoruz ki boş ("") gönderilen parametreler yok sayılmasın
        String[] parcalar = komut.split("\\|", -1);
        String islem = parcalar[0];

        try {
            switch (islem) {
                // ==========================================
                // 1. GİRİŞ VE KASA İŞLEMLERİ
                // ==========================================
                case "GIRIS":
                    if (parcalar.length >= 3) return DatabaseManager.girisYap(parcalar[1], parcalar[2]);
                    return "HATA|Eksik parametre!";

                case "KASA_KAPAT":
                    if (parcalar.length >= 2) return DatabaseManager.kasaKapat(parcalar[1]);
                    return "HATA|Eksik parametre!";

                // ==========================================
                // 2. KULLANICI İŞLEMLERİ
                // ==========================================
                case "KULLANICI_EKLE":
                    if (parcalar.length >= 5) { 
                        String soyad = parcalar.length > 4 ? parcalar[4] : "";
                        String rol = parcalar.length > 5 ? parcalar[5] : "Personel";
                        String email = parcalar.length > 6 ? parcalar[6] : "";
                        String tel = parcalar.length > 7 ? parcalar[7] : "";
                        String adres = parcalar.length > 8 ? parcalar[8] : "";
                        return DatabaseManager.kullaniciEkle(parcalar[1], parcalar[2], parcalar[3], soyad, rol, email, tel, adres);
                    }
                    return "HATA|Eksik parametre!";

                case "KULLANICI_GUNCELLE":
                    if (parcalar.length >= 10) return DatabaseManager.kullaniciGuncelle(parcalar[1], parcalar[2], parcalar[3], parcalar[4], parcalar[5], parcalar[6], parcalar[7], parcalar[8], parcalar[9]);
                    return "HATA|Eksik parametre!";

                case "KULLANICI_SIL":
                    if (parcalar.length >= 2) return DatabaseManager.kullaniciSil(parcalar[1]);
                    return "HATA|Eksik parametre!";

                case "KULLANICI_LISTESI_GETIR":
                    return DatabaseManager.kullanicilariGetir();

                // ==========================================
                // 3. KATEGORİ İŞLEMLERİ
                // ==========================================
                case "KATEGORI_EKLE":
                    if (parcalar.length >= 2) {
                        String aciklama = parcalar.length > 2 ? parcalar[2] : "Açıklama Yok";
                        String gorsel = parcalar.length > 3 ? parcalar[3] : "gorsel_yok.png";
                        return DatabaseManager.kategoriEkle(parcalar[1], aciklama, gorsel);
                    }
                    return "HATA|Eksik parametre!";

                case "KATEGORI_GUNCELLE":
                    if (parcalar.length >= 4) return DatabaseManager.kategoriGuncelle(parcalar[1], parcalar[2], parcalar[3]);
                    return "HATA|Kategori güncelleme parametreleri eksik!";

                case "KATEGORI_SIL":
                    if (parcalar.length >= 2) return DatabaseManager.kategoriSil(parcalar[1]);
                    return "HATA|Kategori silme parametreleri eksik!";

                case "KAT_LISTESI_GETIR":
                    return DatabaseManager.kategorileriGetir();

                // ==========================================
                // 4. ÜRÜN İŞLEMLERİ
                // ==========================================
                case "URUN_EKLE_DETAYLI":
                    if (parcalar.length >= 7) {
                        double fiyat = Double.parseDouble(parcalar[3]);
                        return DatabaseManager.urunEkleDetayli(parcalar[1], parcalar[2], fiyat, parcalar[4], parcalar[5], parcalar[6]);
                    }
                    return "HATA|URUN_EKLE_DETAYLI için eksik parametre! Gönderilen: " + parcalar.length;

                case "URUN_GUNCELLE_DETAYLI":
                    if (parcalar.length >= 8) {
                        double f = Double.parseDouble(parcalar[4]);
                        return DatabaseManager.urunGuncelle(parcalar[1], parcalar[2], parcalar[3], f, parcalar[5], parcalar[6], parcalar[7]);
                    }
                    return "HATA|Güncelleme için eksik parametre!";

                case "URUN_SIL":
                    if (parcalar.length >= 2) return DatabaseManager.urunSil(parcalar[1]);
                    return "HATA|Eksik parametre!";

                case "URUNLERI_GETIR_DETAYLI":
                    if (parcalar.length >= 2) return DatabaseManager.urunleriDetayliGetir(parcalar[1]);
                    return "HATA|Eksik parametre!";

                // Eski Ürün Komutları (Geriye Dönük Uyumluluk İçin)
                case "URUN_EKLE_ISIMLE":
                    if (parcalar.length >= 7) return DatabaseManager.urunEkleIsimle(parcalar[1], parcalar[2], parcalar[3], Double.parseDouble(parcalar[4]), Integer.parseInt(parcalar[5]), Double.parseDouble(parcalar[6]));
                    return "HATA|Eksik parametre!";
                case "URUNLERI_GETIR":
                    if (parcalar.length >= 2) return DatabaseManager.urunleriDetayliGetir(parcalar[1]); 
                    return "HATA|Eksik parametre!";

                // ==========================================
                // 5. MASA YÖNETİM İŞLEMLERİ
                // ==========================================
                case "MASALARI_GETIR":
                    return DatabaseManager.masalariGetir();
                case "MASA_EKLE":
                    if (parcalar.length >= 2) return DatabaseManager.masaEkle(parcalar[1]);
                    return "HATA|Eksik parametre!";
                case "MASA_GUNCELLE":
                    if (parcalar.length >= 3) return DatabaseManager.masaGuncelle(parcalar[1], parcalar[2]);
                    return "HATA|Eksik parametre!";
                case "MASA_SIL":
                    if (parcalar.length >= 2) return DatabaseManager.masaSil(parcalar[1]);
                    return "HATA|Eksik parametre!";
                case "AKTIF_MASALARI_GETIR": // Masa Hafızası Komutu
                    return DatabaseManager.aktifMasalariGetir();

                // ==========================================
                // 6. SİPARİŞ, KASA VE MUTFAK İŞLEMLERİ
                // ==========================================
                case "SIPARIS_OLUSTUR":
                    if (parcalar.length >= 4) {
                        // FisHTML içinde "|" karakteri olabileceği için substring ile en sondaki HTML bloğunu alıyoruz.
                        String fisHtml = komut.substring(komut.indexOf(parcalar[3]));
                        return DatabaseManager.siparisOlustur(parcalar[1], parcalar[2], fisHtml); 
                    }
                    return "HATA|Eksik parametre!";
                
                case "SIPARIS_DURUM_GUNCELLE":
                    if (parcalar.length >= 3) return DatabaseManager.siparisDurumuGuncelle(Integer.parseInt(parcalar[1]), parcalar[2]);
                    return "HATA|Eksik parametre!";
                
                case "SIPARIS_ODEME_AL":
                    if (parcalar.length >= 3) return DatabaseManager.siparisOdemeAl(Integer.parseInt(parcalar[1]), parcalar[2]);
                    return "HATA|Eksik parametre!";

                case "MUTFAK_SIPARIS_GETIR":
                    return DatabaseManager.mutfakSiparisleriGetir();
                case "MUTFAK_SIPARIS_GETIR_FULL":
                    return DatabaseManager.mutfakSiparisleriGetirFull();
                case "KASA_SIPARIS_GETIR":
                    return DatabaseManager.kasaSiparisleriGetir();
                case "REZERVASYON_EKLE":
                    if (parcalar.length >= 5) return DatabaseManager.rezervasyonEkle(parcalar[1], parcalar[2], parcalar[3], parcalar[4]);
                    return "HATA|Eksik parametre!";
                case "REZ_LISTESI_GETIR":
                    return DatabaseManager.rezervasyonlariGetir();
                case "BUGUN_REZ_GETIR":
                    if (parcalar.length >= 2) return DatabaseManager.bugunkuRezervasyonlariGetir(parcalar[1]);
                    return "HATA|Eksik parametre!";
                case "REZ_DURUM_GUNCELLE":
                    if (parcalar.length >= 3) return DatabaseManager.rezervasyonDurumGuncelle(Integer.parseInt(parcalar[1]), parcalar[2]);
                    return "HATA|Eksik parametre!";
                // ==========================================
                // 7. VESTİYER İŞLEMLERİ
                // ==========================================
                case "VESTIYER_EKLE":
                    if (parcalar.length >= 3) return DatabaseManager.vestiyerEkle(parcalar[1], parcalar[2]);
                    return "HATA|Eksik parametre!";
                case "VESTIYER_TESLIM_ET":
                    if (parcalar.length >= 2) return DatabaseManager.vestiyerTeslimEt(Integer.parseInt(parcalar[1]));
                    return "HATA|Eksik parametre!";

                // ==========================================
                // BİLİNMEYEN KOMUT
                // ==========================================
                default:
                    return "HATA|Sunucu bu komutu tanımıyor (" + islem + ")";
            }

        } catch (NumberFormatException ex) {
            return "HATA|Sayısal bir değer girilmesi gereken yere metin girildi! (" + ex.getMessage() + ")";
        } catch (Exception ex) {
            return "HATA|Sunucu işlem hatası: " + ex.getMessage();
        }
    }
}