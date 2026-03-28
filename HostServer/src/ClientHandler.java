package src; // Eğer VS Code hata verirse bu satırı silmeyi unutma!
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
            // 1. İstemciye karşılama mesajı gönderiyoruz (İstemci tarafındaki ilk in.readLine() bunu okur)
            out.println("BAĞLANTI_BASARILI|Sunucuya hoş geldiniz.");

            // 2. İstemciden gelen asıl komutu okuyoruz
            String komut = in.readLine();
            if (komut != null) {
                System.out.println("İstemciden Gelen Komut: " + komut);
                String cevap = islemYap(komut);
                out.println(cevap); // Sonucu istemciye geri gönder
            }

        } catch (Exception e) {
            System.err.println("İstemci bağlantı hatası: " + e.getMessage());
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
                    if (parcalar.length >= 3) {
                        return DatabaseManager.girisYap(parcalar[1], parcalar[2]);
                    }
                    return "HATA|Eksik parametre!";

                case "KASA_KAPAT":
                    if (parcalar.length >= 2) {
                        return DatabaseManager.kasaKapat(parcalar[1]);
                    }
                    return "HATA|Eksik parametre!";

                // ==========================================
                // 2. KULLANICI İŞLEMLERİ
                // ==========================================
                case "KULLANICI_EKLE":
                    // Format: KULLANICI_EKLE|User|Pass|Ad|Soyad|Rol|Email|Tel|Adres
                    if (parcalar.length >= 5) { 
                        String soyad = parcalar.length > 4 ? parcalar[4] : "";
                        String rol = parcalar.length > 5 ? parcalar[5] : "Personel";
                        String email = parcalar.length > 6 ? parcalar[6] : "";
                        String tel = parcalar.length > 7 ? parcalar[7] : "";
                        String adres = parcalar.length > 8 ? parcalar[8] : "";
                        return DatabaseManager.kullaniciEkle(parcalar[1], parcalar[2], parcalar[3], soyad, rol, email, tel, adres);
                    }
                    return "HATA|Eksik parametre!";

                case "KULLANICI_LISTESI_GETIR":
                    // DatabaseManager içinde bu metodu yazdığımızı varsayıyoruz
                    return DatabaseManager.kullanicilariGetir();

                // ==========================================
                // 3. KATEGORİ İŞLEMLERİ
                // ==========================================
                case "KATEGORI_EKLE":
                    // Format: KATEGORI_EKLE|KategoriAdı|Aciklama|Gorsel
                    if (parcalar.length >= 2) {
                        String aciklama = parcalar.length > 2 ? parcalar[2] : "Açıklama Yok";
                        String gorsel = parcalar.length > 3 ? parcalar[3] : "gorsel_yok.png";
                        return DatabaseManager.kategoriEkle(parcalar[1], aciklama, gorsel);
                    }
                    return "HATA|Eksik parametre!";

                case "KATEGORI_GUNCELLE":
                    // Format: KATEGORI_GUNCELLE|EskiAd|YeniAd|Gorsel
                    if (parcalar.length >= 4) {
                        return DatabaseManager.kategoriGuncelle(parcalar[1], parcalar[2], parcalar[3]);
                    }
                    return "HATA|Kategori güncelleme parametreleri eksik!";

                case "KAT_LISTESI_GETIR":
                    return DatabaseManager.kategorileriGetir();

                // ==========================================
                // 4. STANDART ÜRÜN İŞLEMLERİ (Eski Komutlar)
                // ==========================================
                case "URUN_EKLE_ISIMLE":
                    if (parcalar.length >= 7) {
                        return DatabaseManager.urunEkleIsimle(parcalar[1], parcalar[2], parcalar[3], 
                                Double.parseDouble(parcalar[4]), Integer.parseInt(parcalar[5]), Double.parseDouble(parcalar[6]));
                    }
                    return "HATA|Eksik parametre!";

                case "URUNLERI_GETIR":
                    if (parcalar.length >= 2) {
                        // Yeni sisteme uyum sağlaması için detaylı getiri çağırıyoruz
                        return DatabaseManager.urunleriDetayliGetir(parcalar[1]); 
                    }
                    return "HATA|Eksik parametre!";

                case "KATEGORI_SIL":
                    if (parcalar.length >= 2) {
                        return DatabaseManager.kategoriSil(parcalar[1]);
                    }
                    return "HATA|Kategori silme parametreleri eksik!";
                case "URUN_SIL":
                    if (parcalar.length >= 2) {
                        return DatabaseManager.urunSil(parcalar[1]);
                    }
                    return "HATA|Eksik parametre!";
                    
                case "URUN_GUNCELLE_DETAYLI":
                    // Format: URUN_GUNCELLE_DETAYLI|EskiUrunAdi|KatAdi|YeniUrunAdi|Fiyat|Aciklama|Gorsel|Malzemeler
                    if (parcalar.length >= 8) {
                        double f = Double.parseDouble(parcalar[4]);
                        return DatabaseManager.urunGuncelle(parcalar[1], parcalar[2], parcalar[3], f, parcalar[5], parcalar[6], parcalar[7]);
                    }
                    return "HATA|Güncelleme için eksik parametre!";
                // ==========================================
                // 5. YENİ GELİŞMİŞ ÜRÜN İŞLEMLERİ (İçerik ve Görsel)
                // ==========================================
                case "URUN_EKLE_DETAYLI":
                    // Format: URUN_EKLE_DETAYLI|KatAdi|UrunAdi|Fiyat|Aciklama|Icerikler|Gorsel
                    if (parcalar.length >= 7) {
                        double fiyat = Double.parseDouble(parcalar[3]);
                        return DatabaseManager.urunEkleDetayli(parcalar[1], parcalar[2], fiyat, parcalar[4], parcalar[5], parcalar[6]);
                    }
                    return "HATA|URUN_EKLE_DETAYLI için eksik parametre! Gönderilen: " + parcalar.length;

                case "URUNLERI_GETIR_DETAYLI":
                    if (parcalar.length >= 2) {
                        return DatabaseManager.urunleriDetayliGetir(parcalar[1]);
                    }
                    return "HATA|Eksik parametre!";
                case "KULLANICI_SIL":
                    if (parcalar.length >= 2) return DatabaseManager.kullaniciSil(parcalar[1]);
                    return "HATA|Eksik parametre!";
                case "SIPARIS_DURUM_GUNCELLE":
                    // Format: SIPARIS_DURUM_GUNCELLE|OrderID|YENI_DURUM (Örn: HAZIRLANIYOR, IPTAL, ODENDI)
                    if (parcalar.length >= 3) {
                        return DatabaseManager.siparisDurumuGuncelle(Integer.parseInt(parcalar[1]), parcalar[2]);
                    }
                    return "HATA|Eksik parametre!";

                case "VESTIYER_EKLE":
                    // Format: VESTIYER_EKLE|MasaNo|AskiNo
                    if (parcalar.length >= 3) {
                        return DatabaseManager.vestiyerEkle(parcalar[1], parcalar[2]);
                    }
                    return "HATA|Eksik parametre!";

                case "VESTIYER_TESLIM_ET":
                    // Format: VESTIYER_TESLIM_ET|IslemID
                    if (parcalar.length >= 2) {
                        return DatabaseManager.vestiyerTeslimEt(Integer.parseInt(parcalar[1]));
                    }
                    return "HATA|Eksik parametre!";
                case "KULLANICI_GUNCELLE":
                    // Format: KULLANICI_GUNCELLE|EskiKAdi|YeniKAdi|Sifre|Ad|Soyad|Rol|Email|Tel|Adres
                    if (parcalar.length >= 10) {
                        return DatabaseManager.kullaniciGuncelle(parcalar[1], parcalar[2], parcalar[3], parcalar[4], parcalar[5], parcalar[6], parcalar[7], parcalar[8], parcalar[9]);
                    }
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