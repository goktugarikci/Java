package src; 

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:uygulama_veritabani.db";

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // ==========================================
    // 1. VERİTABANI VE TABLO KURULUMU
    // ==========================================
    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // Yabancı anahtar (Foreign Key) desteğini aktif et
            stmt.execute("PRAGMA foreign_keys = ON;");
                
            // Kullanicilar
            stmt.execute("CREATE TABLE IF NOT EXISTS Kullanicilar (" +
                    "UserID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserName TEXT NOT NULL UNIQUE, Password TEXT NOT NULL, " +
                    "FirstName TEXT, LastName TEXT, Role TEXT, " +
                    "Email TEXT, Phone TEXT, Address TEXT);");

            // Kategoriler
            stmt.execute("CREATE TABLE IF NOT EXISTS Kategoriler (" +
                    "CategoryID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CategoryName TEXT NOT NULL UNIQUE, Description TEXT);");

            // Urunler Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Urunler (" +
                    "ProductID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CategoryID INTEGER, ProductName TEXT NOT NULL, " +
                    "Description TEXT, Price REAL, Stock INTEGER DEFAULT 100, " +
                    "KdvRate REAL DEFAULT 18, ImagePath TEXT, " +
                    "FOREIGN KEY(CategoryID) REFERENCES Kategoriler(CategoryID));");

            // Urun_Malzemeleri Tablosu (Ekle/Çıkar Modifikatörleri İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Urun_Malzemeleri (" +
                    "MalzemeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ProductID INTEGER, " +
                    "MalzemeAdi TEXT NOT NULL, " +
                    "VarsayilanVarMi INTEGER DEFAULT 1, " + 
                    "EkstraUcret REAL DEFAULT 0, " +
                    "FOREIGN KEY(ProductID) REFERENCES Urunler(ProductID) ON DELETE CASCADE);");

            // Siparisler
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparisler (" +
                    "OrderID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserID INTEGER, OrderType TEXT, TableNumber TEXT, " +
                    "Status TEXT DEFAULT 'BEKLEMEDE', OrderDate DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "TotalPrice REAL DEFAULT 0, PaymentType TEXT, " +
                    "FOREIGN KEY(UserID) REFERENCES Kullanicilar(UserID));");

            // Siparis Detaylari
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparis_Detaylari (" +
                    "DetailID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "OrderID INTEGER, ProductID INTEGER, " +
                    "Quantity INTEGER, UnitPrice REAL, " +
                    "Modifiers TEXT, " + 
                    "FOREIGN KEY(OrderID) REFERENCES Siparisler(OrderID), " +
                    "FOREIGN KEY(ProductID) REFERENCES Urunler(ProductID));");

            // Zaman Logları ve Günlük Raporlar
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparis_Zaman_Loglari (" +
                    "LogID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "OrderID INTEGER, Status TEXT, IslemZamani DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(OrderID) REFERENCES Siparisler(OrderID));");

            stmt.execute("CREATE TABLE IF NOT EXISTS Gunluk_Raporlar (" +
                    "RaporID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "RaporTarihi DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "ToplamCiro REAL, StokHatalari TEXT, KapatanAdmin TEXT);");
                    
            // Vestiyer Tablosu (Sadece Masalar İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Vestiyer_Kayitlari (" +
                    "IslemID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaNo TEXT NOT NULL, " +
                    "AskiNumarasi TEXT NOT NULL, " +
                    "Durum TEXT DEFAULT 'TESLIM_ALINDI', " + 
                    "KayitZamani DATETIME DEFAULT CURRENT_TIMESTAMP);");
                    
            // Masalar Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Masalar (" +
                    "MasaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaAdi TEXT NOT NULL UNIQUE, " +
                    "Durum TEXT DEFAULT 'BOS');"); 
                    
            // Gerçek Zamanlı Mutfak Siparişleri Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparisler_Mutfak (" +
                    "OrderID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaIsmi TEXT, " +
                    "MusteriIsmi TEXT, " +
                    "Durum TEXT DEFAULT 'BEKLEMEDE', " +
                    "FisHTML TEXT, " +
                    "Kurye TEXT DEFAULT 'Atanmadi', " + // KURYE YAMASI
                    "SiparisZamani DATETIME DEFAULT CURRENT_TIMESTAMP);");
                    
            // REZERVASYON TABLOSU
            stmt.execute("CREATE TABLE IF NOT EXISTS Rezervasyonlar (" +
                    "RezID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaIsmi TEXT NOT NULL, " +
                    "MusteriAdi TEXT NOT NULL, " +
                    "Telefon TEXT, " +
                    "Tarih TEXT NOT NULL, " + 
                    "Saat TEXT NOT NULL, " +  
                    "Notlar TEXT, " +
                    "Durum TEXT DEFAULT 'AKTIF');");

            // GÜN SONU (Z RAPORU) TABLOSU
            stmt.execute("CREATE TABLE IF NOT EXISTS GunSonuRaporlari (" +
                    "RaporID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Tarih TEXT NOT NULL, " +
                    "Ciro REAL, " +
                    "ToplamSiparis INTEGER, " +
                    "PaketSayisi INTEGER, " +
                    "EveServisSayisi INTEGER, " +
                    "MasaSayisi INTEGER, " +
                    "MasaDetay TEXT, " +
                    "StokDetay TEXT);");
            stmt.execute("CREATE TABLE IF NOT EXISTS PersonelVardiya ("
                  + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                  + "personel_adi TEXT,"
                  + "tarih TEXT,"
                  + "giris_saati TEXT,"
                  + "cikis_saati TEXT,"
                  + "toplam_saat REAL DEFAULT 0,"
                  + "mesai_saati REAL DEFAULT 0,"
                  + "hakedis_tl REAL DEFAULT 0,"
                  + "durum TEXT" // 'AKTIF' veya 'TAMAMLANDI'
                  + ");");
            // GÜNLÜK SATIŞLAR VE STOK TABLOLARI
            stmt.execute("CREATE TABLE IF NOT EXISTS GunlukSatislar (SatisID INTEGER PRIMARY KEY AUTOINCREMENT, UrunAdi TEXT, Adet INTEGER);");

            // ========================================================
            // EKSİK KOLON YAMALARI (ESKİ VERİTABANINI GÜNCELLEMEK İÇİN)
            // ========================================================
            try { stmt.execute("ALTER TABLE Urunler ADD COLUMN Ingredients TEXT;"); } catch (Exception ignored) {} 
            try { stmt.execute("ALTER TABLE Urunler ADD COLUMN ImagePath TEXT;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Urunler ADD COLUMN Stock INTEGER DEFAULT 100;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Kategoriler ADD COLUMN ImagePath TEXT;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Rezervasyonlar ADD COLUMN Telefon TEXT;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Rezervasyonlar ADD COLUMN Notlar TEXT;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Siparisler_Mutfak ADD COLUMN Kurye TEXT DEFAULT 'Atanmadi';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE GunSonuRaporlari ADD COLUMN StokDetay TEXT;"); } catch (Exception ignored) {}

            ilkAdminKoy(conn);
            System.out.println("Veritabanı Sistemi: Tüm modüller (Kurye, Stok, Z-Raporu vb.) aktif.");
            
        } catch (Exception e) {
            System.err.println("Veritabanı Başlatma Hatası: " + e.getMessage());
        }
    }

    private static void ilkAdminKoy(Connection conn) {
        String sql = "INSERT OR IGNORE INTO Kullanicilar (UserName, Password, Role, FirstName, LastName) " +
                     "VALUES ('admin', 'admin123', 'Admin', 'Sistem', 'Yöneticisi')";
        try (Statement stmt = conn.createStatement()) { stmt.execute(sql); } catch (Exception ignored) {}
    }

    // ==========================================
    // 2. KULLANICI VE GİRİŞ İŞLEMLERİ
    // ==========================================
    public static String girisYap(String user, String pass) {
        String sql = "SELECT Role, FirstName, LastName FROM Kullanicilar WHERE UserName = ? AND Password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user); pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return "BAŞARILI|" + rs.getString("Role") + "|" + rs.getString("FirstName") + " " + rs.getString("LastName");
            return "HATA|Kullanıcı adı veya şifre yanlış.";
        } catch (Exception e) { return "HATA|Veritabanı hatası."; }
    }

    public static String kullaniciEkle(String user, String pass, String ad, String soyad, String rol, String email, String tel, String adres) {
        String sql = "INSERT INTO Kullanicilar (UserName, Password, FirstName, LastName, Role, Email, Phone, Address) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user); pstmt.setString(2, pass); pstmt.setString(3, ad); pstmt.setString(4, soyad);
            pstmt.setString(5, rol); pstmt.setString(6, email); pstmt.setString(7, tel); pstmt.setString(8, adres);
            pstmt.executeUpdate();
            return "BAŞARILI|Yeni kullanıcı sisteme tanımlandı.";
        } catch (Exception e) { return "HATA|Kullanıcı eklenemedi: " + e.getMessage(); }
    }

    public static String kullanicilariGetir() {
        StringBuilder sb = new StringBuilder("KULLANICI_LISTESI");
        String sql = "SELECT UserID, UserName, FirstName, Role FROM Kullanicilar";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append("|").append(rs.getInt("UserID")).append(";")
                  .append(rs.getString("UserName")).append(";")
                  .append(rs.getString("FirstName")).append(";")
                  .append(rs.getString("Role"));
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|" + e.getMessage(); }
    }

    public static String kullaniciSil(String userName) {
        if (userName.equalsIgnoreCase("admin")) {
            return "HATA|Ana yönetici hesabı (admin) silinemez!";
        }
        String sql = "DELETE FROM Kullanicilar WHERE UserName = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            int affected = pstmt.executeUpdate();
            if (affected > 0) return "BAŞARILI|Kullanıcı başarıyla silindi: " + userName;
            return "HATA|Silinecek kullanıcı bulunamadı.";
        } catch (Exception e) { return "HATA|Kullanıcı silinemedi: " + e.getMessage(); }
    }

    public static String kullaniciGuncelle(String eskiKullaniciAdi, String userName, String pass, String ad, String soyad, String rol, String email, String tel, String adres) {
        if (eskiKullaniciAdi.equalsIgnoreCase("admin") && !userName.equalsIgnoreCase("admin")) {
            return "HATA|'admin' hesabının kullanıcı adı değiştirilemez!";
        }
        String sql = "UPDATE Kullanicilar SET UserName=?, Password=?, FirstName=?, LastName=?, Role=?, Email=?, Phone=?, Address=? WHERE UserName=?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName); pstmt.setString(2, pass);
            pstmt.setString(3, ad); pstmt.setString(4, soyad);
            pstmt.setString(5, rol); pstmt.setString(6, email);
            pstmt.setString(7, tel); pstmt.setString(8, adres);
            pstmt.setString(9, eskiKullaniciAdi);
            pstmt.executeUpdate();
            return "BAŞARILI|Kullanıcı bilgileri güncellendi!";
        } catch (Exception e) { return "HATA|Güncelleme hatası: " + e.getMessage(); }
    }
    // ==========================================
    // VARDİYA (GİRİŞ/ÇIKIŞ) VE MESAİ HESAPLAMA METODU
    // ==========================================
    public static synchronized String vardiyaIslem(String islemTipi, String personelAdi) {
        String tarih = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String saat = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        try (Connection conn = connect()) {
            if (islemTipi.equals("GIRIS")) {
                // 1. Zaten aktif bir vardiyası var mı kontrol et
                String checkSql = "SELECT id FROM PersonelVardiya WHERE personel_adi = ? AND durum = 'AKTIF'";
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setString(1, personelAdi);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) return "HATA|Zaten aktif bir vardiyanız bulunuyor!";
                }
                
                // 2. Yeni Vardiya Kaydı Aç
                String sql = "INSERT INTO PersonelVardiya (personel_adi, tarih, giris_saati, durum) VALUES (?, ?, ?, 'AKTIF')";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, personelAdi);
                    pstmt.setString(2, tarih);
                    pstmt.setString(3, saat);
                    pstmt.executeUpdate();
                    return "BASARILI|Vardiyanız " + saat + " itibariyle başlatıldı. İyi çalışmalar!";
                }
            } 
            else if (islemTipi.equals("CIKIS")) {
                // 1. Kapatılacak vardiyayı bul
                String getSql = "SELECT id, giris_saati FROM PersonelVardiya WHERE personel_adi = ? AND durum = 'AKTIF'";
                int vId = -1;
                String girisSaati = "";
                
                try (PreparedStatement pstmt = conn.prepareStatement(getSql)) {
                    pstmt.setString(1, personelAdi);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        vId = rs.getInt("id");
                        girisSaati = rs.getString("giris_saati");
                    } else {
                        return "HATA|Kapatılacak aktif bir vardiya bulunamadı!";
                    }
                }

                // 2. Süre ve Maaş Hesaplamaları (Saat farkı bulunur)
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
                java.util.Date d1 = sdf.parse(girisSaati);
                java.util.Date d2 = sdf.parse(saat);
                long farkMs = d2.getTime() - d1.getTime();
                
                double toplamSaat = (double) farkMs / (1000 * 60 * 60);
                if(toplamSaat < 0) toplamSaat += 24; // Gece yarısını geçerse (Örn: 23:00 - 02:00)

                // 8 saat altı normal, üstü fazla mesai kabul edilir
                double standartSaat = Math.min(toplamSaat, 8.0);
                double mesaiSaat = Math.max(0, toplamSaat - 8.0);
                
                double SAATLIK_UCRET = 100.0; // Restoranınızın saatlik ücretini buraya yazın
                double hakedis = (standartSaat * SAATLIK_UCRET) + (mesaiSaat * (SAATLIK_UCRET * 1.5));

                // 3. Veritabanını Güncelle
                String updateSql = "UPDATE PersonelVardiya SET cikis_saati = ?, toplam_saat = ?, mesai_saati = ?, hakedis_tl = ?, durum = 'TAMAMLANDI' WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setString(1, saat);
                    pstmt.setDouble(2, Math.round(toplamSaat * 100.0) / 100.0);
                    pstmt.setDouble(3, Math.round(mesaiSaat * 100.0) / 100.0);
                    pstmt.setDouble(4, Math.round(hakedis * 100.0) / 100.0);
                    pstmt.setInt(5, vId);
                    pstmt.executeUpdate();
                    
                    return "BASARILI|Vardiya Bitirildi!\nToplam Çalışma: " + String.format("%.1f", toplamSaat) + " Saat\nHakediş: " + Math.round(hakedis) + " TL";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "HATA|Veritabanı hatası: " + e.getMessage();
        }
        return "HATA|Geçersiz işlem tipi.";
    }

    // ==========================================
    // GÜN SONU / Z RAPORU METODU (NET KÂR HESABI)
    // ==========================================
    public static synchronized String gunSonuKapat() {
        double toplamNakit = 0, toplamKart = 0, toplamGider = 0;
        String bugunTarih = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        try (Connection conn = connect()) {
            
            // 1. KASAYA GİREN PARAYI HESAPLA (Müşteri Satışları)
            // Sadece başarılı ve o gün ödenmiş siparişler baz alınır
            String sqlSatis = "SELECT toplam_tutar, odeme_turu FROM Siparisler WHERE durum IN ('ODENDI', 'TESLIM_EDILDI')";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSatis)) {
                while (rs.next()) {
                    double tutar = rs.getDouble("toplam_tutar");
                    String tur = rs.getString("odeme_turu");
                    
                    if (tur != null) {
                        if (tur.toLowerCase().contains("nakit")) toplamNakit += tutar;
                        else if (tur.toLowerCase().contains("kart")) toplamKart += tutar;
                    }
                }
            }

            // 2. KASADAN ÇIKAN PARAYI HESAPLA (Personel Maaşları)
            // O gün vardiyasını kapatmış tüm personelin toplam hakedişi
            String sqlGider = "SELECT SUM(hakedis_tl) as totalGider FROM PersonelVardiya WHERE durum = 'TAMAMLANDI' AND tarih = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGider)) {
                pstmt.setString(1, bugunTarih);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) toplamGider = rs.getDouble("totalGider");
            }

            // 3. MATEMATİKSEL İŞLEMLER
            double brutCiro = toplamNakit + toplamKart;
            double netKar = brutCiro - toplamGider;

            // 4. VERİTABANI YENİ GÜNE SIFIRLAMA (Arşivleme)
            // O günün siparişlerinin durumunu 'GUN_SONU' yaparak ertesi gün kasanın sıfırdan başlamasını sağlar
            String sqlSifirla = "UPDATE Siparisler SET durum = 'GUN_SONU' WHERE durum IN ('ODENDI', 'TESLIM_EDILDI', 'IPTAL')";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlSifirla);
            }

            // İstenilen Format: RAPOR | Brut | Nakit | Kart | Maliyet | Net Kar
            return "RAPOR|" + brutCiro + "|" + toplamNakit + "|" + toplamKart + "|" + toplamGider + "|" + netKar;

        } catch (Exception e) {
            e.printStackTrace();
            return "HATA|Z Raporu oluşturulamadı: " + e.getMessage();
        }
    }
    // ==========================================
    // 3. KATEGORİ İŞLEMLERİ
    // ==========================================
    public static String kategoriEkle(String categoryName, String description, String imagePath) {
        String sql = "INSERT INTO Kategoriler (CategoryName, Description, ImagePath) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName); 
            pstmt.setString(2, description);
            pstmt.setString(3, imagePath);
            pstmt.executeUpdate(); 
            return "BAŞARILI|Kategori eklendi: " + categoryName;
        } catch (Exception e) { return "HATA|Kategori eklenemedi: " + e.getMessage(); }
    }

    public static String kategorileriGetir() {
        StringBuilder sb = new StringBuilder("KAT_LISTESI");
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            boolean gorselSutunuVarMi = true;
            try { stmt.executeQuery("SELECT ImagePath FROM Kategoriler LIMIT 1"); } 
            catch (Exception e) { gorselSutunuVarMi = false; } 

            String sql = gorselSutunuVarMi ? "SELECT CategoryName, ImagePath FROM Kategoriler" : "SELECT CategoryName FROM Kategoriler";
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) { 
                String ad = rs.getString("CategoryName");
                String img = gorselSutunuVarMi ? rs.getString("ImagePath") : "gorsel_yok.png";
                sb.append("|").append(ad).append(";").append(img == null ? "gorsel_yok.png" : img); 
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Kategoriler çekilemedi: " + e.getMessage(); }
    }

    public static String kategoriGuncelle(String eskiAd, String yeniAd, String imagePath) {
        String sql = "UPDATE Kategoriler SET CategoryName = ?, ImagePath = ? WHERE CategoryName = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, yeniAd);
            pstmt.setString(2, imagePath);
            pstmt.setString(3, eskiAd);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) return "BAŞARILI|Kategori güncellendi.";
            return "HATA|Kategori bulunamadı.";
        } catch (Exception e) { return "HATA|Kategori güncellenemedi: " + e.getMessage(); }
    }

    public static String kategoriSil(String categoryName) {
        String checkSql = "SELECT COUNT(*) FROM Urunler u JOIN Kategoriler k ON u.CategoryID = k.CategoryID WHERE k.CategoryName = ?";
        String delSql = "DELETE FROM Kategoriler WHERE CategoryName = ?";
        
        try (Connection conn = DriverManager.getConnection(URL)) {
            try (PreparedStatement pCheck = conn.prepareStatement(checkSql)) {
                pCheck.setString(1, categoryName);
                ResultSet rs = pCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return "HATA|Silmek istediğiniz kategoride " + rs.getInt(1) + " adet ürün bulunuyor! Önce ürünleri silin.";
                }
            }
            try (PreparedStatement pDel = conn.prepareStatement(delSql)) {
                pDel.setString(1, categoryName);
                pDel.executeUpdate();
                return "BAŞARILI|Kategori başarıyla silindi.";
            }
        } catch (Exception e) { return "HATA|Kategori silinemedi: " + e.getMessage(); }
    }

    // ==========================================
    // 4. GELİŞMİŞ ÜRÜN VE MODİFİKATÖR İŞLEMLERİ
    // ==========================================
    public static String urunEkleDetayli(String katAdi, String urunAdi, double fiyat, String aciklama, String gorsel, String malzemelerStr) {
        String idSorgu = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String urunSql = "INSERT INTO Urunler (CategoryID, ProductName, Price, Description, ImagePath, Stock) VALUES (?, ?, ?, ?, ?, 100)";
        String malzemeSql = "INSERT INTO Urun_Malzemeleri (ProductID, MalzemeAdi, VarsayilanVarMi, EkstraUcret) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(URL)) {
            int katId = -1;
            try (PreparedStatement pstmtId = conn.prepareStatement(idSorgu)) {
                pstmtId.setString(1, katAdi);
                ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) katId = rs.getInt("CategoryID");
            }
            if (katId == -1) return "HATA|Kategori bulunamadı!";

            int productId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(urunSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, katId); pstmt.setString(2, urunAdi);
                pstmt.setDouble(3, fiyat); pstmt.setString(4, aciklama);
                pstmt.setString(5, gorsel);
                pstmt.executeUpdate();
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        productId = generatedKeys.getInt(1);
                    }
                }
            }

            if (productId != -1 && malzemelerStr != null && !malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
                try (PreparedStatement pMat = conn.prepareStatement(malzemeSql)) {
                    String[] malzemeler = malzemelerStr.split(",");
                    for (String m : malzemeler) {
                        String[] mDetay = m.split(":");
                        if (mDetay.length == 3) {
                            pMat.setInt(1, productId);
                            pMat.setString(2, mDetay[0].trim()); 
                            pMat.setInt(3, Integer.parseInt(mDetay[1].trim())); 
                            pMat.setDouble(4, Double.parseDouble(mDetay[2].trim())); 
                            pMat.addBatch();
                        }
                    }
                    pMat.executeBatch(); 
                }
            }

            return "BAŞARILI|Ürün ve interaktif malzemeler başarıyla kaydedildi!";
        } catch (Exception e) { return "HATA|Kayıt hatası: " + e.getMessage(); }
    }

    public static String urunleriDetayliGetir(String katAdi) {
        StringBuilder sb = new StringBuilder("URUN_LISTESI_DETAYLI");
        String sql = "SELECT u.ProductID, u.ProductName, u.Price, u.Description, u.Stock, u.ImagePath, " +
                     "(SELECT GROUP_CONCAT(MalzemeAdi || ':' || VarsayilanVarMi || ':' || EkstraUcret, ',') " +
                     " FROM Urun_Malzemeleri WHERE ProductID = u.ProductID) AS Malzemeler " +
                     "FROM Urunler u JOIN Kategoriler k ON u.CategoryID = k.CategoryID " +
                     "WHERE k.CategoryName = ?";
        
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, katAdi);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String desc = rs.getString("Description");
                String gorsel = rs.getString("ImagePath");
                String malzemeler = rs.getString("Malzemeler"); 
                
                sb.append("|").append(rs.getString("ProductName"))
                  .append(";").append(rs.getDouble("Price"))
                  .append(";").append((desc == null || desc.isEmpty()) ? "Açıklama Yok" : desc)
                  .append(";").append(rs.getInt("Stock"))
                  .append(";").append(gorsel == null ? "gorsel_yok.png" : gorsel)
                  .append(";").append(malzemeler == null ? "" : malzemeler);
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Ürünler yüklenemedi: " + e.getMessage(); }
    }

    public static String urunEkleIsimle(String katAdi, String urunAdi, String aciklama, double fiyat, int stok, double kdv) {
        String idSorgu = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String urunSql = "INSERT INTO Urunler (CategoryID, ProductName, Description, Price, Stock, KdvRate) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL)) {
            int katId = -1;
            try (PreparedStatement pstmtId = conn.prepareStatement(idSorgu)) {
                pstmtId.setString(1, katAdi);
                ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) katId = rs.getInt("CategoryID");
            }
            if (katId == -1) return "HATA|Kategori bulunamadı!";

            try (PreparedStatement pstmtUrun = conn.prepareStatement(urunSql)) {
                pstmtUrun.setInt(1, katId); pstmtUrun.setString(2, urunAdi); pstmtUrun.setString(3, aciklama);
                pstmtUrun.setDouble(4, fiyat); pstmtUrun.setInt(5, stok); pstmtUrun.setDouble(6, kdv);
                pstmtUrun.executeUpdate();
                return "BAŞARILI|Ürün eklendi: " + urunAdi;
            }
        } catch (Exception e) { return "HATA|Veritabanı hatası: " + e.getMessage(); }
    }

    public static String urunSil(String urunAdi) {
        String sqlGetId = "SELECT ProductID FROM Urunler WHERE ProductName = ?";
        String sqlDeleteUrun = "DELETE FROM Urunler WHERE ProductID = ?";
        try (Connection conn = DriverManager.getConnection(URL)) {
            int productId = -1;
            try (PreparedStatement pstmtId = conn.prepareStatement(sqlGetId)) {
                pstmtId.setString(1, urunAdi); ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) productId = rs.getInt("ProductID");
            }
            if (productId == -1) return "HATA|Silinecek ürün bulunamadı!";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteUrun)) {
                pstmt.setInt(1, productId); pstmt.executeUpdate();
                return "BAŞARILI|Ürün başarıyla silindi: " + urunAdi;
            }
        } catch (Exception e) { return "HATA|Ürün silinirken hata oluştu: " + e.getMessage(); }
    }

    public static String urunGuncelle(String eskiAd, String katAdi, String yeniAd, double fiyat, String aciklama, String gorsel, String malzemelerStr) {
        String sqlGetId = "SELECT ProductID FROM Urunler WHERE ProductName = ?";
        String sqlGetKatId = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String sqlUpdateUrun = "UPDATE Urunler SET CategoryID = ?, ProductName = ?, Price = ?, Description = ?, ImagePath = ? WHERE ProductID = ?";
        String sqlDeleteMalzemeler = "DELETE FROM Urun_Malzemeleri WHERE ProductID = ?";
        String sqlInsertMalzeme = "INSERT INTO Urun_Malzemeleri (ProductID, MalzemeAdi, VarsayilanVarMi, EkstraUcret) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            int productId = -1; int katId = -1;
            try (PreparedStatement pId = conn.prepareStatement(sqlGetId)) {
                pId.setString(1, eskiAd); ResultSet rs = pId.executeQuery();
                if(rs.next()) productId = rs.getInt("ProductID");
            }
            if(productId == -1) return "HATA|Güncellenecek ürün bulunamadı!";

            try (PreparedStatement pKat = conn.prepareStatement(sqlGetKatId)) {
                pKat.setString(1, katAdi); ResultSet rs = pKat.executeQuery();
                if(rs.next()) katId = rs.getInt("CategoryID");
            }

            try (PreparedStatement pUpd = conn.prepareStatement(sqlUpdateUrun)) {
                pUpd.setInt(1, katId); pUpd.setString(2, yeniAd); pUpd.setDouble(3, fiyat);
                pUpd.setString(4, aciklama); pUpd.setString(5, gorsel); pUpd.setInt(6, productId); pUpd.executeUpdate();
            }

            try (PreparedStatement pDelMat = conn.prepareStatement(sqlDeleteMalzemeler)) {
                pDelMat.setInt(1, productId); pDelMat.executeUpdate();
            }

            if (malzemelerStr != null && !malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
                try (PreparedStatement pMat = conn.prepareStatement(sqlInsertMalzeme)) {
                    String[] malzemeler = malzemelerStr.split(",");
                    for (String m : malzemeler) {
                        String[] mDetay = m.split(":");
                        if (mDetay.length == 3) {
                            pMat.setInt(1, productId); pMat.setString(2, mDetay[0].trim());
                            pMat.setInt(3, Integer.parseInt(mDetay[1].trim())); pMat.setDouble(4, Double.parseDouble(mDetay[2].trim()));
                            pMat.addBatch();
                        }
                    }
                    pMat.executeBatch();
                }
            }
            return "BAŞARILI|Ürün ve içerikler başarıyla güncellendi!";
        } catch (Exception e) { return "HATA|Güncelleme hatası: " + e.getMessage(); }
    }

    // ==========================================
    // 5. MASA VE VESTİYER İŞLEMLERİ
    // ==========================================
    public static String masaEkle(String masaAdi) {
        String sql = "INSERT INTO Masalar (MasaAdi) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaAdi); pstmt.executeUpdate(); return "BAŞARILI|Masa eklendi: " + masaAdi;
        } catch (Exception e) { return "HATA|Masa eklenemedi (İsim kullanılıyor olabilir): " + e.getMessage(); }
    }

    public static String masalariGetir() {
        StringBuilder sb = new StringBuilder("MASA_LISTESI");
        String sql = "SELECT MasaAdi, Durum FROM Masalar";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { sb.append("|").append(rs.getString("MasaAdi")).append(";").append(rs.getString("Durum")); }
            return sb.toString();
        } catch (Exception e) { return "HATA|Masalar çekilemedi: " + e.getMessage(); }
    }

    public static String masaGuncelle(String eskiAd, String yeniAd) {
        String sql = "UPDATE Masalar SET MasaAdi = ? WHERE MasaAdi = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, yeniAd); pstmt.setString(2, eskiAd);
            int affected = pstmt.executeUpdate();
            if (affected > 0) return "BAŞARILI|Masa adı güncellendi.";
            return "HATA|Masa bulunamadı.";
        } catch (Exception e) { return "HATA|Masa güncellenemedi: " + e.getMessage(); }
    }

    public static String masaSil(String masaAdi) {
        String sql = "DELETE FROM Masalar WHERE MasaAdi = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaAdi); pstmt.executeUpdate(); return "BAŞARILI|Masa silindi: " + masaAdi;
        } catch (Exception e) { return "HATA|Masa silinemedi: " + e.getMessage(); }
    }

    public static String vestiyerEkle(String masaNo, String askiNo) {
        String sql = "INSERT INTO Vestiyer_Kayitlari (MasaNo, AskiNumarasi) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaNo); pstmt.setString(2, askiNo); pstmt.executeUpdate();
            return "BAŞARILI|Eşya vestiyere eklendi. Askı No: " + askiNo;
        } catch (Exception e) { return "HATA|Vestiyer kaydı yapılamadı: " + e.getMessage(); }
    }

    public static String vestiyerTeslimEt(int islemId) {
        String sql = "UPDATE Vestiyer_Kayitlari SET Durum = 'IADE_EDILDI' WHERE IslemID = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, islemId); pstmt.executeUpdate();
            return "BAŞARILI|Eşya müşteriye teslim edildi.";
        } catch (Exception e) { return "HATA|İşlem başarısız: " + e.getMessage(); }
    }

    public static String aktifMasalariGetir() {
        StringBuilder sb = new StringBuilder("AKTIF_MASALAR|");
        // Paket ve Eve Servis hariç tüm masalar
        String sql = "SELECT MasaIsmi, Durum, SiparisZamani FROM Siparisler_Mutfak WHERE Durum NOT IN ('ODENDI', 'IPTAL') AND MasaIsmi NOT IN ('PAKET', 'EVE_SERVIS') GROUP BY MasaIsmi";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString("MasaIsmi")).append("~_~").append(rs.getString("Durum")).append("~_~").append(rs.getString("SiparisZamani")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Aktif masalar çekilemedi: " + e.getMessage(); }
    }

    // ==========================================
    // 6. SİPARİŞ VE KASA (YENİ ALTYAPI)
    // ==========================================
    public static String siparisOlustur(String masa, String musteri, String html, String urunlerData) {
        String sql = "INSERT INTO Siparisler_Mutfak (MasaIsmi, MusteriIsmi, FisHTML, SiparisZamani, Durum) VALUES (?, ?, ?, datetime('now','localtime'), 'BEKLEMEDE')";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masa); pstmt.setString(2, musteri); pstmt.setString(3, html);
            pstmt.executeUpdate();
            
            // Stok Düşme
            if (urunlerData != null && !urunlerData.isEmpty() && !urunlerData.equals("null")) {
                String[] urunler = urunlerData.split(",");
                for (String u : urunler) {
                    String[] detay = u.split(":");
                    if (detay.length == 2) {
                        String ad = detay[0]; int adet = Integer.parseInt(detay[1]);
                        try(PreparedStatement p1 = conn.prepareStatement("UPDATE Urunler SET Stock = Stock - ? WHERE ProductName = ?")) {
                            p1.setInt(1, adet); p1.setString(2, ad); p1.executeUpdate();
                        }
                        try(PreparedStatement p2 = conn.prepareStatement("INSERT INTO GunlukSatislar (UrunAdi, Adet) VALUES (?, ?)")) {
                            p2.setString(1, ad); p2.setInt(2, adet); p2.executeUpdate();
                        }
                    }
                }
            }
            return "BAŞARILI|Sipariş oluşturuldu.";
        } catch (Exception e) { return "HATA|" + e.getMessage(); }
    }

// ==========================================
    // SİPARİŞ DURUMUNU GÜNCELLEME METODU (Mutfak, Kasa vb. için)
    // ==========================================
    public static String siparisDurumuGuncelle(int orderId, String yeniDurum) {
        String sql = "UPDATE Siparisler_Mutfak SET Durum = ? WHERE OrderID = ?";
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, yeniDurum);
            pstmt.setInt(2, orderId);
            int etkilenen = pstmt.executeUpdate();
            
            if (etkilenen > 0) {
                return "BAŞARILI|Durum güncellendi.";
            } else {
                return "HATA|Sipariş bulunamadı veya güncellenemedi.";
            }
        } catch (Exception e) { 
            return "HATA|Durum güncelleme hatası: " + e.getMessage(); 
        }
    }


// ==========================================
    // KASADAN ÖDEME ALINDIĞINDA KURYE VE ÖDEME TÜRÜNÜ GEÇMİŞE YAZAR
    // ==========================================
    public static String siparisOdemeAl(int orderId, String odemeTuru, String tutar) {
        String sqlUpdate = "UPDATE Siparisler_Mutfak SET Durum = 'ODENDI' WHERE OrderID = ?";
        
        // Z Raporu ve Kurye hesapları için Kurye ve OdemeTuru bilgilerini de Ciro Arşivine yolluyoruz
        String sqlInsertGecmis = "INSERT INTO SiparisGecmisi (OrderID, MasaAdi, FisHTML, ToplamTutar, Tarih, Kurye, OdemeTuru) " +
                                 "SELECT OrderID, MasaIsmi, FisHTML, ?, datetime('now', 'localtime'), Kurye, ? " +
                                 "FROM Siparisler_Mutfak WHERE OrderID = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL)) {
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }
            try (java.sql.PreparedStatement pstmt2 = conn.prepareStatement(sqlInsertGecmis)) {
                double miktar = 0.0;
                try { miktar = Double.parseDouble(tutar.replace(",", ".")); } catch (Exception ignored) {}
                
                pstmt2.setDouble(1, miktar);
                pstmt2.setString(2, odemeTuru); // Nakit veya Kart
                pstmt2.setInt(3, orderId);
                pstmt2.executeUpdate();
            }
            return "BAŞARILI|Sipariş kapatıldı ve " + tutar + " TL tutarındaki ödeme (" + odemeTuru + ") Z Raporu cirosuna eklendi!";
            
        } catch (Exception e) { 
            return "HATA|Ödeme alınamadı: " + e.getMessage(); 
        }
    }
    // ==========================================
    // YENİ: KURYE GÜN SONU / VARDİYA HESABI ALMA
    // ==========================================
    public static String kuryeGunlukRaporGetir(String kuryeAdi) {
        StringBuilder sb = new StringBuilder("KURYE_HESAP|");
        double toplamNakit = 0, toplamKart = 0;
        int siparisSayisi = 0;
        StringBuilder liste = new StringBuilder();

        // Z Raporu alınsa dahi, kuryenin "Bugün" (date('now')) teslim ettiği tüm arşivi çeker
        String sql = "SELECT ToplamTutar, OdemeTuru, FisHTML, Tarih FROM SiparisGecmisi " +
                     "WHERE Kurye = ? AND date(Tarih) = date('now', 'localtime') ORDER BY ID DESC";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, kuryeAdi);
            java.sql.ResultSet rs = pstmt.executeQuery();
            
            while(rs.next()) {
                siparisSayisi++;
                double tutar = rs.getDouble("ToplamTutar");
                String tur = rs.getString("OdemeTuru");
                String html = rs.getString("FisHTML");
                String tarih = rs.getString("Tarih");
                
                if(tur != null && tur.toLowerCase().contains("kart")) toplamKart += tutar;
                else toplamNakit += tutar;

                // Fişin içinden müşteri ismini söküp alır
                String musteri = "Bilinmiyor";
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("Müşteri:\\s*<b>([^<]+)</b>").matcher(html);
                if(m.find()) musteri = m.group(1);

                liste.append(tarih.substring(11, 16)).append(" ➔ ").append(musteri)
                     .append(" (").append(tutar).append(" TL - ").append(tur).append(")<br>");
            }
            
            sb.append(siparisSayisi).append("|").append(toplamNakit).append("|").append(toplamKart).append("|");
            if(siparisSayisi == 0) sb.append("Bugün teslim edilen sipariş bulunmamaktadır.");
            else sb.append(liste.toString());
            
            return sb.toString();
        } catch (Exception e) { 
            return "HATA|" + e.getMessage(); 
        }
    }
    // ==========================================
    // 1. GÜNLÜK ÖZET (DASHBOARD) - GECE SIFIRLANMAYAN VARDİYA SİSTEMİ
    // ==========================================
    public static String gunlukOzetGetir() {
        finansalTablolariHazirla();

        double toplamCiro = 0.0;
        int tamamlananSiparis = 0;
        int aktifMasa = 0;
        int bugunkuRezervasyon = 0;

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.Statement stmt = conn.createStatement()) {

            // 1. EN SON ALINAN "GÜN SONU (Z RAPORU)" SAATİNİ BUL
            String sonZRaporuZamani = "1970-01-01 00:00:00"; // Eğer hiç rapor alınmamışsa en baştan başlar
            try (java.sql.ResultSet rsZ = stmt.executeQuery("SELECT MAX(Tarih) AS SonZ FROM ZRaporlari")) {
                if (rsZ.next() && rsZ.getString("SonZ") != null) {
                    sonZRaporuZamani = rsZ.getString("SonZ");
                }
            }

            // 2. O SAATTEN BU YANA (ŞU ANKİ VARDİYADA) KESİLEN SİPARİŞLERİ TOPLA
            // Dikkat: date('now') kaldırıldı! Sadece son rapor saatinden büyük olanları toplar.
            String sqlCiro = "SELECT SUM(ToplamTutar) AS GunlukCiro, COUNT(*) AS SiparisSayisi " +
                             "FROM SiparisGecmisi WHERE Tarih > '" + sonZRaporuZamani + "'";
            
            try (java.sql.ResultSet rs = stmt.executeQuery(sqlCiro)) {
                if (rs.next()) {
                    toplamCiro = rs.getDouble("GunlukCiro");
                    tamamlananSiparis = rs.getInt("SiparisSayisi");
                }
            }

            // 3. AKTİF MASALARI SAYMA (Hazır olmayanlar zaten masada oturanlardır)
            String sqlMasa = "SELECT COUNT(DISTINCT MasaAdi) AS MasaSayisi FROM Siparisler_Mutfak WHERE Durum != 'HAZIR'";
            try (java.sql.ResultSet rs = stmt.executeQuery(sqlMasa)) {
                if (rs.next()) aktifMasa = rs.getInt("MasaSayisi");
            }

            // 4. REZERVASYONLAR (Bu normal takvim gününe göre çalışır)
            String sqlRez = "SELECT COUNT(*) AS RezSayisi FROM Rezervasyonlar WHERE date(Tarih) = date('now', 'localtime')";
            try (java.sql.ResultSet rs = stmt.executeQuery(sqlRez)) {
                if (rs.next()) bugunkuRezervasyon = rs.getInt("RezSayisi");
            }

            // Sonuçları paketle (0.00 TL formatında)
            String formatliCiro = String.format(java.util.Locale.US, "%.2f", toplamCiro);
            return "GUNLUK_OZET|" + formatliCiro + "|" + tamamlananSiparis + "|" + aktifMasa + "|" + bugunkuRezervasyon;

        } catch (Exception e) {
            return "HATA|Özet verileri alınamadı: " + e.getMessage();
        }
    }

    // ==========================================
    // FİNANSAL TABLOLARI VE KOLONLARI OTOMATİK KURAN MOTOR
    // ==========================================
    public static void finansalTablolariHazirla() {
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS SiparisGecmisi (" +
                         "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "OrderID INTEGER, " +
                         "MasaAdi TEXT, " +
                         "FisHTML TEXT, " +
                         "ToplamTutar REAL DEFAULT 0.0, " + 
                         "Kurye TEXT DEFAULT 'Atanmadi', " + // KURYE HESAPLARI İÇİN YENİ
                         "OdemeTuru TEXT DEFAULT 'Nakit', " + // KURYE NAKİT/KART AYRIMI İÇİN YENİ
                         "Tarih DATETIME DEFAULT (datetime('now', 'localtime')))");

            try { stmt.execute("ALTER TABLE SiparisGecmisi ADD COLUMN ToplamTutar REAL DEFAULT 0.0"); } catch (Exception ignored) {} 
            try { stmt.execute("ALTER TABLE SiparisGecmisi ADD COLUMN Tarih DATETIME DEFAULT (datetime('now', 'localtime'))"); } catch (Exception ignored) {} 
            try { stmt.execute("ALTER TABLE SiparisGecmisi ADD COLUMN Kurye TEXT DEFAULT 'Atanmadi'"); } catch (Exception ignored) {} 
            try { stmt.execute("ALTER TABLE SiparisGecmisi ADD COLUMN OdemeTuru TEXT DEFAULT 'Nakit'"); } catch (Exception ignored) {} 

            stmt.execute("CREATE TABLE IF NOT EXISTS ZRaporlari (" +
                         "RaporID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "IsGunu TEXT, " + 
                         "Tarih DATETIME DEFAULT (datetime('now', 'localtime')), " + 
                         "ToplamCiro REAL DEFAULT 0.0, " +
                         "SiparisSayisi INTEGER DEFAULT 0)");
            
        } catch (Exception e) {
            System.out.println("Tablo kurulum hatası: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. GÜN SONU ALMA (Z RAPORU) VE RESETLEME İŞLEMİ
    // ==========================================
    public static String gunSonuRaporuAl() {
        finansalTablolariHazirla();
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.Statement stmt = conn.createStatement()) {
            
            // 1. En Son Rapor Ne Zaman Alınmış?
            String sonZRaporuZamani = "1970-01-01 00:00:00";
            try (java.sql.ResultSet rsZ = stmt.executeQuery("SELECT MAX(Tarih) AS SonZ FROM ZRaporlari")) {
                if (rsZ.next() && rsZ.getString("SonZ") != null) sonZRaporuZamani = rsZ.getString("SonZ");
            }

            double ciro = 0.0;
            int siparis = 0;
            String isGunuTarihi = new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date()); 

            // 2. O saatten bu yana olan tüm ciro, sipariş ve VARDİYANIN BAŞLADIĞI İLK TARİHİ bul!
            String sqlOzet = "SELECT SUM(ToplamTutar) AS Ciro, COUNT(*) AS Adet, MIN(Tarih) AS VardiyaBaslangici " +
                             "FROM SiparisGecmisi WHERE Tarih > '" + sonZRaporuZamani + "'";
            
            try (java.sql.ResultSet rs = stmt.executeQuery(sqlOzet)) {
                if (rs.next()) {
                    ciro = rs.getDouble("Ciro");
                    siparis = rs.getInt("Adet");
                    String baslangic = rs.getString("VardiyaBaslangici");
                    
                    // İş gününü ilk siparişin tarihinden al (Böylece 01.01 öğlen başlayıp 02.01 gece biten iş, 01.01 sayılır)
                    if (baslangic != null && baslangic.length() >= 10) {
                        String yil = baslangic.substring(0, 4);
                        String ay = baslangic.substring(5, 7);
                        String gun = baslangic.substring(8, 10);
                        isGunuTarihi = gun + "." + ay + "." + yil; 
                    }
                }
            }

            if (siparis == 0) {
                return "HATA|Z Raporu alınacak yeni bir işlem/ciro bulunamadı!";
            }

            // 3. Z Raporunu (Gün Sonunu) Veritabanına Arşivle
            String sqlZKaydet = "INSERT INTO ZRaporlari (IsGunu, Tarih, ToplamCiro, SiparisSayisi) VALUES (?, datetime('now', 'localtime'), ?, ?)";
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlZKaydet)) {
                pstmt.setString(1, isGunuTarihi); // Sisteme 01.01 olarak işlenir
                pstmt.setDouble(2, ciro);
                pstmt.setInt(3, siparis);
                pstmt.executeUpdate();
            }

            return "BASARILI|Gün Sonu Başarıyla Alındı!\\nİş Günü: " + isGunuTarihi + "\\nToplam Ciro: " + String.format(java.util.Locale.US, "%.2f", ciro) + " TL";

        } catch (Exception e) {
            return "HATA|Gün Sonu alınırken hata oluştu: " + e.getMessage();
        }
    }
    // ==========================================
    // MUTFAK İŞLEMLERİ (ÖDENDİ, İPTAL VE YOLDA OLANLARI DA GETİRİR)
    // ==========================================
    public static String mutfakSiparisleriGetirFull() {
        StringBuilder sb = new StringBuilder("MUTFAK_FULL_VERI|");
        // DİKKAT: Artık sadece YENİ veya HAZIR olanları değil, Arşivlenmemiş (Gün Sonu Alınmamış) TÜM siparişleri mutfağa yollar
        String sql = "SELECT * FROM Siparisler_Mutfak WHERE Durum NOT LIKE '%_ARSIV' ORDER BY OrderID DESC LIMIT 100";
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL);
             java.sql.Statement stmt = conn.createStatement(); 
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            
            boolean kayitVar = false;
            while (rs.next()) {
                kayitVar = true;
                
                int orderId = rs.getInt("OrderID");
                String durum = rs.getString("Durum");
                
                String masa = "Tür/Masa Bilinmiyor";
                try { masa = rs.getString("MasaIsmi"); } catch (Exception e1) {}
                
                String musteri = "Müşteri Bilinmiyor";
                try { musteri = rs.getString("MusteriIsmi"); } catch (Exception e) {}
                if (musteri == null || musteri.trim().isEmpty()) musteri = "Müşteri Bilinmiyor";

                String tarih = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                try { 
                    String t = rs.getString("SiparisZamani");
                    if (t != null && !t.trim().isEmpty()) tarih = t;
                } catch (Exception e) {}

                String fisHtml = "İçerik Bulunamadı";
                try { fisHtml = rs.getString("FisHTML"); } catch (Exception e) {}
                if (fisHtml != null) fisHtml = fisHtml.replace("\n", " ").replace("\r", " ").replace("|", " ");

                sb.append(orderId).append("~_~")
                  .append(masa).append("~_~")
                  .append(musteri).append("~_~")
                  .append(fisHtml).append("~_~") 
                  .append(durum).append("~_~")
                  .append(tarih).append("|||");
            }
            if (!kayitVar) return "MUTFAK_FULL_VERI|BOS";
            return sb.toString();
            
        } catch (Exception e) { 
            return "HATA|SQL Hatası: " + e.getMessage(); 
        }
    }

    public static String mutfakSiparisleriGetir() {
        StringBuilder sb = new StringBuilder("MUTFAK_VERI|");
        String sql = "SELECT OrderID, MasaAdi, Durum FROM Siparisler_Mutfak WHERE Durum IN ('YENI', 'HAZIRLANIYOR', 'HAZIR') ORDER BY OrderID ASC";
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL); 
             java.sql.Statement stmt = conn.createStatement(); 
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            
            boolean kayitVar = false;
            while (rs.next()) {
                kayitVar = true;
                sb.append(rs.getInt("OrderID")).append("~_~")
                  .append(rs.getString("MasaAdi")).append("~_~")
                  .append(rs.getString("Durum")).append("|||");
            }
            if (!kayitVar) return "MUTFAK_VERI|BOS";
            return sb.toString();
        } catch (Exception e) { 
            return "HATA|Mutfak özet verileri çekilemedi: " + e.getMessage(); 
        }
    }

    public static String kasaSiparisleriGetir() {
        StringBuilder sb = new StringBuilder("KASA_VERI|");
        String sql = "SELECT OrderID, MasaIsmi, MusteriIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Durum NOT IN ('ODENDI', 'IPTAL') ORDER BY OrderID DESC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~").append(rs.getString("MasaIsmi")).append("~_~").append(rs.getString("MusteriIsmi")).append("~_~").append(rs.getString("Durum")).append("~_~").append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Kasa verisi çekilemedi: " + e.getMessage(); }
    }

    public static String kasaGecmisSiparisleriGetir() {
        StringBuilder sb = new StringBuilder("KASA_GECMIS_VERI|");
        String sql = "SELECT OrderID, MasaIsmi, MusteriIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Durum IN ('ODENDI', 'IPTAL') ORDER BY OrderID DESC LIMIT 50";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~").append(rs.getString("MasaIsmi")).append("~_~").append(rs.getString("MusteriIsmi")).append("~_~").append(rs.getString("Durum")).append("~_~").append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Geçmiş veriler çekilemedi: " + e.getMessage(); }
    }

    public static String kasaKapat(String adminIsmi) {
        // Eski Sistem Uyumluluğu İçin Bırakılmıştır
        try (Connection conn = DriverManager.getConnection(URL)) {
            String aktifSorgu = "SELECT COUNT(*) FROM Siparisler WHERE Status NOT IN ('TESLIM_EDILDI', 'IPTAL', 'KASA_KAPATILDI')";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(aktifSorgu)) {
                if (rs.next() && rs.getInt(1) > 0) return "HATA|İçeride aktif eski sipariş var. Kasa kapatılamaz!";
            }
            double toplamCiro = 0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(TotalPrice) FROM Siparisler WHERE Status = 'TESLIM_EDILDI'")) {
                if (rs.next()) toplamCiro = rs.getDouble(1);
            }
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Gunluk_Raporlar (ToplamCiro, StokHatalari, KapatanAdmin) VALUES (?, ?, ?)")) {
                pstmt.setDouble(1, toplamCiro); pstmt.setString(2, "Hata Yok"); pstmt.setString(3, adminIsmi); pstmt.executeUpdate();
            }
            conn.createStatement().execute("UPDATE Siparisler SET Status = 'KASA_KAPATILDI' WHERE Status = 'TESLIM_EDILDI'");
            return "BAŞARILI|Gün sonu alındı. Ciro: " + toplamCiro + " TL.";
        } catch (Exception e) { return "HATA|Kasa kapatılamadı: " + e.getMessage(); }
    }

    // ==========================================
    // 7. Z RAPORU VE GÜN SONU
    // ==========================================
    public static String gunSonuAl(String tarih) {
        double ciro = 0.0; int toplam = 0, paket = 0, eve = 0, masa = 0;
        Map<String, Integer> masaDetay = new HashMap<>();

        String selectSql = "SELECT MasaIsmi, FisHTML FROM Siparisler_Mutfak WHERE Durum = 'ODENDI'";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(selectSql)) {
            while (rs.next()) {
                toplam++; String mName = rs.getString("MasaIsmi"); String html = rs.getString("FisHTML");
                if (mName.contains("PAKET")) paket++;
                else if (mName.contains("EVE_SERVIS")) eve++;
                else { masa++; masaDetay.put(mName, masaDetay.getOrDefault(mName, 0) + 1); }

                int fBas = html.indexOf("<b>");
                int fEnd = html.indexOf("</b>", fBas);
                if (fBas != -1 && fEnd != -1) {
                    try { ciro += Double.parseDouble(html.substring(fBas + 3, fEnd).replace(" TL", "").replace(",", ".").trim()); } catch(Exception ignored){}
                }
            }
        } catch (Exception e) { return "HATA|Hesaplama hatası: " + e.getMessage(); }

        if (toplam == 0) return "HATA|Gün sonu alınacak tamamlanmış sipariş (ciro) bulunamadı!";

        StringBuilder detayStr = new StringBuilder();
        for (Map.Entry<String, Integer> entry : masaDetay.entrySet()) detayStr.append(entry.getKey()).append(":").append(entry.getValue()).append(",");

        StringBuilder stokDetayStr = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(URL)) {
            String satisSql = "SELECT UrunAdi, SUM(Adet) as ToplamAdet FROM GunlukSatislar GROUP BY UrunAdi";
            try (Statement st = conn.createStatement(); ResultSet rsSatis = st.executeQuery(satisSql)) {
                while(rsSatis.next()) {
                    String uAd = rsSatis.getString("UrunAdi"); int tAdet = rsSatis.getInt("ToplamAdet");
                    int kalanStok = 0;
                    try(PreparedStatement psStok = conn.prepareStatement("SELECT Stock FROM Urunler WHERE ProductName = ?")) {
                        psStok.setString(1, uAd); ResultSet rsStok = psStok.executeQuery();
                        if(rsStok.next()) kalanStok = rsStok.getInt("Stock");
                    }
                    stokDetayStr.append(uAd).append(" : ").append(tAdet).append(" adet satıldı (Stokta Kalan: ").append(kalanStok).append(")|");
                }
            }
            try(Statement st2 = conn.createStatement()) { st2.executeUpdate("DELETE FROM GunlukSatislar"); } 
        } catch(Exception ignored) {}

        String finalStok = stokDetayStr.length() > 0 ? stokDetayStr.toString() : "Satış Yok";

        String insertSql = "INSERT INTO GunSonuRaporlari (Tarih, Ciro, ToplamSiparis, PaketSayisi, EveServisSayisi, MasaSayisi, MasaDetay, StokDetay) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, tarih); pstmt.setDouble(2, ciro); pstmt.setInt(3, toplam); pstmt.setInt(4, paket); pstmt.setInt(5, eve); pstmt.setInt(6, masa); pstmt.setString(7, detayStr.toString()); pstmt.setString(8, finalStok);
            pstmt.executeUpdate();
        } catch (Exception e) { return "HATA|Rapor kaydedilemedi: " + e.getMessage(); }

        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE Siparisler_Mutfak SET Durum = Durum || '_ARSIV' WHERE Durum IN ('ODENDI', 'IPTAL')");
        } catch (Exception ignored) {}

        return "BAŞARILI|Gün sonu Z Raporu başarıyla alındı.";
    }

    public static String eskiRaporlariGetir() {
        StringBuilder sb = new StringBuilder("GUNSONU_RAPORLARI|");
        String sql = "SELECT * FROM GunSonuRaporlari ORDER BY RaporID DESC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString("Tarih")).append("~_~").append(rs.getDouble("Ciro")).append("~_~")
                  .append(rs.getInt("ToplamSiparis")).append("~_~").append(rs.getInt("PaketSayisi")).append("~_~")
                  .append(rs.getInt("EveServisSayisi")).append("~_~").append(rs.getInt("MasaSayisi")).append("~_~")
                  .append(rs.getString("MasaDetay")).append("~_~").append(rs.getString("StokDetay")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Raporlar çekilemedi: " + e.getMessage(); }
    }

    // ==========================================
    // 8. KURYE / MOTORCU İŞLEMLERİ
    // ==========================================
// ==========================================
    // 8. KURYE / MOTORCU İŞLEMLERİ
    // ==========================================
    public static String kuryeListesiGetir() {
        StringBuilder sb = new StringBuilder("KURYE_LISTESI");
        
        // DÜZELTME: Artık veritabanında 'Motokurye' rolüne sahip olanları da bulacak!
        String sql = "SELECT FirstName FROM Kullanicilar WHERE Role IN ('Motokurye', 'Motorcu', 'Kurye')";
        
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            boolean kuryeVarMi = false;
            while (rs.next()) {
                kuryeVarMi = true;
                sb.append("|").append(rs.getString("FirstName")); // Ad Soyad FirstName sütununda tutuluyor
            }
            if (!kuryeVarMi) return "KURYE_LISTESI|Sistemde Kayıtlı Motorcu Yok";
            return sb.toString();
        } catch (Exception e) { return "HATA|Kuryeler çekilemedi: " + e.getMessage(); }
    }

    // ==========================================
    // SADELEŞTİRİLMİŞ KURYE ATAMA MOTORU (FİŞE YAZMA İPTAL EDİLDİ)
    // ==========================================
    public static String kuryeAta(int orderId, String kuryeAdi) {
        // Sadece veritabanındaki Kurye ve Durum sütunlarını güncelliyoruz.
        String sqlUpdate = "UPDATE Siparisler_Mutfak SET Kurye = ?, Durum = 'YOLA_CIKTI' WHERE OrderID = ?";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL)) {
            
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setString(1, kuryeAdi);
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            } catch (java.sql.SQLException ex) {
                // Eğer Kurye sütunu eski veritabanında henüz yoksa, anında oluşturup tekrar deneriz.
                try (java.sql.Statement st = conn.createStatement()) {
                    st.execute("ALTER TABLE Siparisler_Mutfak ADD COLUMN Kurye TEXT DEFAULT 'Atanmadi';");
                } catch (Exception ignored) {}

                try (java.sql.PreparedStatement pstmt2 = conn.prepareStatement(sqlUpdate)) {
                    pstmt2.setString(1, kuryeAdi);
                    pstmt2.setInt(2, orderId);
                    pstmt2.executeUpdate();
                }
            }
            return "BAŞARILI|Kurye (" + kuryeAdi + ") atandı ve sipariş yola çıktı.";
            
        } catch (Exception e) {
            return "HATA|Kurye atama işlemi başarısız: " + e.getMessage();
        }
    }
    // ==========================================
    // YÖNETİCİ İÇİN KURYE TAKİP VERİLERİNİ GETİRİR
    // ==========================================
    public static String kuryeTakipSiparisleriGetir(String kuryeAdi) {
        StringBuilder sb = new StringBuilder("KURYE_TAKIP_VERI|");
        String sql = "SELECT OrderID, Musteri, Durum, FisHTML FROM Siparisler_Mutfak WHERE Kurye = ? ORDER BY OrderID DESC";
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(URL); 
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // İsimdeki olası boşlukları temizleyerek arıyoruz
            pstmt.setString(1, kuryeAdi.trim());
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                boolean veriVar = false;
                while (rs.next()) {
                    veriVar = true;
                    String html = rs.getString("FisHTML");
                    if (html == null) html = "";
                    
                    // KRİTİK NOKTA: HTML içindeki enter/satır atlamalarını siliyoruz ki Soket bağlantısı kopmasın!
                    html = html.replace("\n", " ").replace("\r", " ");
                    
                    sb.append(rs.getInt("OrderID")).append("~_~")
                      .append(rs.getString("Musteri")).append("~_~")
                      .append(rs.getString("Durum")).append("~_~")
                      .append(html).append("|||");
                }
                if (!veriVar) return "KURYE_TAKIP_VERI|BOS"; // Eğer sipariş yoksa boş döner
            }
            return sb.toString();
        } catch (Exception e) { 
            return "HATA|Kurye siparişleri çekilemedi: " + e.getMessage(); 
        }
    }

    public static String kuryeSiparisleriGetir(String kuryeAdi) {
        StringBuilder sb = new StringBuilder("KURYE_SIPARISLERI|");
        String sql = "SELECT OrderID, MusteriIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Kurye = ? AND Durum IN ('YOLA_CIKTI', 'ODENDI', 'IPTAL') ORDER BY OrderID DESC LIMIT 50";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kuryeAdi);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~")
                  .append(rs.getString("MusteriIsmi")).append("~_~")
                  .append(rs.getString("Durum")).append("~_~")
                  .append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Kurye siparişleri çekilemedi: " + e.getMessage(); }
    }

    // ==========================================
    // 9. REZERVASYON İŞLEMLERİ
    // ==========================================
    public static String rezervasyonEkle(String masa, String musteri, String telefon, String tarih, String saat, String notlar) {
        String checkSql = "SELECT COUNT(*) FROM Rezervasyonlar WHERE MasaIsmi = ? AND Tarih = ? AND Durum = 'AKTIF'";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, masa); checkStmt.setString(2, tarih); ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return "HATA|Bu masa " + tarih + " tarihinde zaten rezerve edilmiştir!";
        } catch (Exception e) { return "HATA|Kontrol hatası: " + e.getMessage(); }

        String sql = "INSERT INTO Rezervasyonlar (MasaIsmi, MusteriAdi, Telefon, Tarih, Saat, Notlar) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masa); pstmt.setString(2, musteri); pstmt.setString(3, telefon); 
            pstmt.setString(4, tarih); pstmt.setString(5, saat); pstmt.setString(6, notlar);
            pstmt.executeUpdate();
            return "BAŞARILI|Rezervasyon eklendi.";
        } catch (Exception e) { return "HATA|Kayıt başarısız: " + e.getMessage(); }
    }

    public static String rezervasyonlariGetir() {
        StringBuilder sb = new StringBuilder("REZ_LISTESI|");
        String sql = "SELECT RezID, MasaIsmi, MusteriAdi, Telefon, Tarih, Saat, Notlar FROM Rezervasyonlar WHERE Durum = 'AKTIF' ORDER BY Tarih ASC, Saat ASC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("RezID")).append("~_~").append(rs.getString("MasaIsmi")).append("~_~")
                  .append(rs.getString("MusteriAdi")).append("~_~").append(rs.getString("Telefon")).append("~_~")
                  .append(rs.getString("Tarih")).append("~_~").append(rs.getString("Saat")).append("~_~")
                  .append(rs.getString("Notlar")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Rezervasyonlar çekilemedi: " + e.getMessage(); }
    }

    public static String bugunkuRezervasyonlariGetir(String bugunTarih) {
        StringBuilder sb = new StringBuilder("BUGUN_REZ|");
        String sql = "SELECT MasaIsmi, MusteriAdi, Saat FROM Rezervasyonlar WHERE Durum = 'AKTIF' AND Tarih = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bugunTarih);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("MasaIsmi")).append("~_~").append(rs.getString("Saat")).append("~_~").append(rs.getString("MusteriAdi")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Bugünün rezervasyonları çekilemedi: " + e.getMessage(); }
    }

    public static String rezervasyonDurumGuncelle(int id, String durum) {
        String sql = "UPDATE Rezervasyonlar SET Durum = ? WHERE RezID = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, durum); pstmt.setInt(2, id); pstmt.executeUpdate();
            return "BAŞARILI|Rezervasyon güncellendi.";
        } catch (Exception e) { return "HATA|Güncelleme başarısız: " + e.getMessage(); }
    }
    // ==========================================
    // PERSONEL YÖNETİMİ (YENİ ARAYÜZ İÇİN)
    // ==========================================
    public static String personelleriGetir() {
        StringBuilder sb = new StringBuilder("PERSONEL_LISTESI|");
        String sql = "SELECT UserName, Password, FirstName, Role, Phone, Email, Address FROM Kullanicilar";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString("UserName")).append("~_~")
                  .append(rs.getString("Password")).append("~_~")
                  .append(rs.getString("FirstName")).append("~_~")
                  .append(rs.getString("Role")).append("~_~")
                  .append(rs.getString("Phone") == null ? "" : rs.getString("Phone")).append("~_~")
                  .append(rs.getString("Email") == null ? "" : rs.getString("Email")).append("~_~")
                  .append(rs.getString("Address") == null ? "" : rs.getString("Address")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Personeller çekilemedi: " + e.getMessage(); }
    }

    public static String personelEkle(String kAdi, String sifre, String adSoyad, String rol, String tel, String email, String adres) {
        String sql = "INSERT INTO Kullanicilar (UserName, Password, FirstName, Role, Phone, Email, Address) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kAdi); pstmt.setString(2, sifre); pstmt.setString(3, adSoyad); pstmt.setString(4, rol);
            pstmt.setString(5, tel);  pstmt.setString(6, email); pstmt.setString(7, adres);
            pstmt.executeUpdate();
            return "BAŞARILI|Yeni personel başarıyla eklendi.";
        } catch (Exception e) { return "HATA|Bu kullanıcı adı zaten mevcut olabilir!"; }
    }

    public static String personelGuncelle(String eskiKAdi, String yeniKAdi, String sifre, String adSoyad, String rol, String tel, String email, String adres) {
        if (eskiKAdi.equalsIgnoreCase("admin") && !yeniKAdi.equalsIgnoreCase("admin")) return "HATA|Admin adı değiştirilemez!";
        String sql = "UPDATE Kullanicilar SET UserName = ?, Password = ?, FirstName = ?, Role = ?, Phone = ?, Email = ?, Address = ? WHERE UserName = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, yeniKAdi); pstmt.setString(2, sifre); pstmt.setString(3, adSoyad); pstmt.setString(4, rol);
            pstmt.setString(5, tel);      pstmt.setString(6, email); pstmt.setString(7, adres); pstmt.setString(8, eskiKAdi);
            pstmt.executeUpdate();
            return "BAŞARILI|Personel bilgileri güncellendi.";
        } catch (Exception e) { return "HATA|Güncelleme başarısız: " + e.getMessage(); }
    }

    public static String personelSil(String kAdi) {
        if(kAdi.equalsIgnoreCase("admin")) return "HATA|Ana yönetici hesabı silinemez!"; 
        String sql = "DELETE FROM Kullanicilar WHERE UserName = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kAdi); pstmt.executeUpdate();
            return "BAŞARILI|Personel sistemden silindi.";
        } catch (Exception e) { return "HATA|Silme başarısız: " + e.getMessage(); }
    }
}