package src; // VS Code paket hatası verirse bu satırı silin

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:uygulama_veritabani.db";

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

            // Urunler Tablosu (Artık Ingredients sütunu yok, Malzemeler ayrı tabloda)
            stmt.execute("CREATE TABLE IF NOT EXISTS Urunler (" +
                    "ProductID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CategoryID INTEGER, ProductName TEXT NOT NULL, " +
                    "Description TEXT, Price REAL, Stock INTEGER DEFAULT 0, " +
                    "KdvRate REAL DEFAULT 18, ImagePath TEXT, " +
                    "FOREIGN KEY(CategoryID) REFERENCES Kategoriler(CategoryID));");

            // YENİ: Urun_Malzemeleri Tablosu (Ekle/Çıkar Modifikatörleri İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Urun_Malzemeleri (" +
                    "MalzemeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ProductID INTEGER, " +
                    "MalzemeAdi TEXT NOT NULL, " +
                    "VarsayilanVarMi INTEGER DEFAULT 1, " + // 1: Standart (İçinde var, çıkarılabilir), 0: Ekstra (Eklenebilir)
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
                    "Modifiers TEXT, " + // Siparişteki özel istekler (Örn: "-Soğan, +Ekstra Sos")
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
            // YENİ: Vestiyer Tablosu (Sadece Masalar İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Vestiyer_Kayitlari (" +
                    "IslemID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaNo TEXT NOT NULL, " +
                    "AskiNumarasi TEXT NOT NULL, " +
                    "Durum TEXT DEFAULT 'TESLIM_ALINDI', " + // TESLIM_ALINDI veya IADE_EDILDI
                    "KayitZamani DATETIME DEFAULT CURRENT_TIMESTAMP);");
            // YENİ: Masalar Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Masalar (" +
                    "MasaID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaAdi TEXT NOT NULL UNIQUE, " +
                    "Durum TEXT DEFAULT 'BOS');"); // BOS, DOLU, REZERVE
            // YENİ: Gerçek Zamanlı Mutfak Siparişleri Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparisler_Mutfak (" +
                    "OrderID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "MasaIsmi TEXT, " +
                    "MusteriIsmi TEXT, " +
                    "Durum TEXT DEFAULT 'BEKLEMEDE', " +
                    "FisHTML TEXT, " +
                    "SiparisZamani DATETIME DEFAULT CURRENT_TIMESTAMP);");
            // REZERVASYON TABLOSU// REZERVASYON TABLOSUNU OLUŞTUR
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
                    "MasaDetay TEXT);");
            // GÜNLÜK SATIŞLAR VE STOK TABLOLARI
            stmt.execute("CREATE TABLE IF NOT EXISTS GunlukSatislar (SatisID INTEGER PRIMARY KEY AUTOINCREMENT, UrunAdi TEXT, Adet INTEGER);");
            // Kurye Takibi İçin Yama (Eski tabloyu silmeden yeni kolon ekler)
            try { stmt.execute("ALTER TABLE Siparisler_Mutfak ADD COLUMN Kurye TEXT DEFAULT 'Atanmadi';"); } catch (Exception ignored) {}
            // Eğer Urunler tablosunda Stok kolonu yoksa otomatik ekle (Yama)
            try { stmt.execute("ALTER TABLE Urunler ADD COLUMN Stok INTEGER DEFAULT 100;"); } catch (Exception ignored) {}
            // Eğer GunSonuRaporlari tablosunda StokDetay yoksa ekle (Yama)
            try { stmt.execute("ALTER TABLE GunSonuRaporlari ADD COLUMN StokDetay TEXT;"); } catch (Exception ignored) {}
            // ========================================================
            try {
                stmt.execute("ALTER TABLE Rezervasyonlar ADD COLUMN Telefon TEXT;");
            } catch (Exception ignored) { } // Eğer kolon zaten varsa hata verir, görmezden gel!

            try {
                stmt.execute("ALTER TABLE Rezervasyonlar ADD COLUMN Notlar TEXT;");
            } catch (Exception ignored) { } // Eğer kolon zaten varsa hata verir, görmezden gel!
            ilkAdminKoy(conn);
            System.out.println("Veritabanı Sistemi: Yeni Modifikatör (Malzeme) altyapısı aktif.");
            
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

// 1. DAHA GÜÇLÜ KOLON KONTROLÜ (Eğer veritabanı kilitliyse bile dener)
    private static void kolonlariKontrolEt() {
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE Urunler ADD COLUMN Ingredients TEXT;");
        } catch (Exception ignored) {} 
        
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE Urunler ADD COLUMN ImagePath TEXT;");
        } catch (Exception ignored) {}
        
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE Kategoriler ADD COLUMN ImagePath TEXT;");
        } catch (Exception ignored) {}
    }

    // 2. Kategori Ekleme Metodu (Güncellendi)
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

// 2. KURŞUN GEÇİRMEZ KATEGORİ GETİRME METODU
    public static String kategorileriGetir() {
        kolonlariKontrolEt(); // Her ihtimale karşı çağırıp sütunun eklendiğinden emin oluyoruz
        
        StringBuilder sb = new StringBuilder("KAT_LISTESI");
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // Eğer sütun kilitlenmeden dolayı eklenemediyse sistemin çökmemesi için akıllı kontrol yapıyoruz
            boolean gorselSutunuVarMi = true;
            try { stmt.executeQuery("SELECT ImagePath FROM Kategoriler LIMIT 1"); } 
            catch (Exception e) { gorselSutunuVarMi = false; } // Sütun yoksa false dön

            // Sütun varsa görselle çek, yoksa sadece isimle çek
            String sql = gorselSutunuVarMi ? "SELECT CategoryName, ImagePath FROM Kategoriler" : "SELECT CategoryName FROM Kategoriler";
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) { 
                String ad = rs.getString("CategoryName");
                String img = gorselSutunuVarMi ? rs.getString("ImagePath") : "gorsel_yok.png";
                // Format: KategoriAdı;GörselYolu
                sb.append("|").append(ad).append(";").append(img == null ? "gorsel_yok.png" : img); 
            }
            return sb.toString();
        } catch (Exception e) { 
            return "HATA|Kategoriler çekilemedi: " + e.getMessage(); 
        }
    }

    // 4. Kategori Güncelleme Metodu (Güncellendi)
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
    // ==========================================
    // 4. GELİŞMİŞ ÜRÜN VE MODİFİKATÖR İŞLEMLERİ
    // ==========================================
    
    /**
     * malzemelerStr Formatı: "MalzemeAdi:VarsayilanMi:Fiyat, MalzemeAdi2:VarsayilanMi:Fiyat"
     * Örnek: "Domates:1:0.0, Ekstra Kaşar:0:15.5" (1=Standart İçerik, 0=Ekstra Ücretli)
     */
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

            // Ürünü Ekle ve Oluşturulan Ürün ID'sini Al
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

            // Eğer malzeme dizesi boş değilse parçala ve malzeme tablosuna ekle
            if (productId != -1 && malzemelerStr != null && !malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
                try (PreparedStatement pMat = conn.prepareStatement(malzemeSql)) {
                    String[] malzemeler = malzemelerStr.split(",");
                    for (String m : malzemeler) {
                        String[] mDetay = m.split(":");
                        if (mDetay.length == 3) {
                            pMat.setInt(1, productId);
                            pMat.setString(2, mDetay[0].trim()); // Malzeme Adı
                            pMat.setInt(3, Integer.parseInt(mDetay[1].trim())); // 1 veya 0
                            pMat.setDouble(4, Double.parseDouble(mDetay[2].trim())); // Ekstra Ücret
                            pMat.addBatch();
                        }
                    }
                    pMat.executeBatch(); // Tüm malzemeleri tek seferde kaydet
                }
            }

            return "BAŞARILI|Ürün ve interaktif malzemeler başarıyla kaydedildi!";
        } catch (Exception e) { return "HATA|Kayıt hatası: " + e.getMessage(); }
    }

    public static String urunleriDetayliGetir(String katAdi) {
        StringBuilder sb = new StringBuilder("URUN_LISTESI_DETAYLI");
        // GROUP_CONCAT ile malzemeleri tek bir sütunda çekiyoruz (Ad:Standart:Fiyat, Ad:Standart:Fiyat...)
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
                String malzemeler = rs.getString("Malzemeler"); // Örn: Domates:1:0.0,Ekstra Kaşar:0:15.0
                
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

    // ==========================================
    // 5. KASA VE GÜN SONU İŞLEMLERİ
    // ==========================================
    public static String kasaKapat(String adminIsmi) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            String aktifSorgu = "SELECT COUNT(*) FROM Siparisler WHERE Status NOT IN ('TESLIM_EDILDI', 'IPTAL', 'KASA_KAPATILDI')";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(aktifSorgu)) {
                if (rs.next() && rs.getInt(1) > 0) return "HATA|İçeride aktif sipariş var. Kasa kapatılamaz!";
            }

            StringBuilder stokHatalari = new StringBuilder();
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT ProductName, Stock FROM Urunler WHERE Stock < 0")) {
                while (rs.next()) stokHatalari.append(rs.getString("ProductName")).append("(").append(rs.getInt("Stock")).append("), ");
            }

            double toplamCiro = 0;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT SUM(TotalPrice) FROM Siparisler WHERE Status = 'TESLIM_EDILDI'")) {
                if (rs.next()) toplamCiro = rs.getDouble(1);
            }

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO Gunluk_Raporlar (ToplamCiro, StokHatalari, KapatanAdmin) VALUES (?, ?, ?)")) {
                pstmt.setDouble(1, toplamCiro);
                pstmt.setString(2, stokHatalari.length() == 0 ? "Hata Yok" : stokHatalari.toString());
                pstmt.setString(3, adminIsmi);
                pstmt.executeUpdate();
            }
            conn.createStatement().execute("UPDATE Siparisler SET Status = 'KASA_KAPATILDI' WHERE Status = 'TESLIM_EDILDI'");

            return "BAŞARILI|Gün sonu alındı. Ciro: " + toplamCiro + " TL.";
        } catch (Exception e) { return "HATA|Kasa kapatılamadı: " + e.getMessage(); }
    }

    // --- ESKİ SİSTEM: İSİMLE BASİT ÜRÜN EKLEME (Geriye Dönük Uyumluluk İçin) ---
    public static String urunEkleIsimle(String katAdi, String urunAdi, String aciklama, double fiyat, int stok, double kdv) {
        String idSorgu = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String urunSql = "INSERT INTO Urunler (CategoryID, ProductName, Description, Price, Stock, KdvRate) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(URL)) {
            int katId = -1;
            
            // 1. Kategorinin ID'sini bul
            try (PreparedStatement pstmtId = conn.prepareStatement(idSorgu)) {
                pstmtId.setString(1, katAdi);
                ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) {
                    katId = rs.getInt("CategoryID");
                }
            }
            
            if (katId == -1) return "HATA|Kategori bulunamadı!";

            // 2. Ürünü Ekle
            try (PreparedStatement pstmtUrun = conn.prepareStatement(urunSql)) {
                pstmtUrun.setInt(1, katId);
                pstmtUrun.setString(2, urunAdi);
                pstmtUrun.setString(3, aciklama);
                pstmtUrun.setDouble(4, fiyat);
                pstmtUrun.setInt(5, stok);
                pstmtUrun.setDouble(6, kdv);
                pstmtUrun.executeUpdate();
                return "BAŞARILI|Ürün eklendi: " + urunAdi;
            }
        } catch (Exception e) { 
            return "HATA|Veritabanı hatası: " + e.getMessage(); 
        }
    }
    // --- ÜRÜN SİLME METODU ---
    public static String urunSil(String urunAdi) {
        String sqlGetId = "SELECT ProductID FROM Urunler WHERE ProductName = ?";
        String sqlDeleteUrun = "DELETE FROM Urunler WHERE ProductID = ?";
        // Not: Urun_Malzemeleri tablosunda ON DELETE CASCADE olduğu için, 
        // ana ürün silinince ona ait malzemeler otomatik silinecektir.

        try (Connection conn = DriverManager.getConnection(URL)) {
            int productId = -1;
            try (PreparedStatement pstmtId = conn.prepareStatement(sqlGetId)) {
                pstmtId.setString(1, urunAdi);
                ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) productId = rs.getInt("ProductID");
            }
            if (productId == -1) return "HATA|Silinecek ürün bulunamadı!";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteUrun)) {
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
                return "BAŞARILI|Ürün başarıyla silindi: " + urunAdi;
            }
        } catch (Exception e) { return "HATA|Ürün silinirken hata oluştu: " + e.getMessage(); }
    }

    // --- ÜRÜN VE İÇERİK GÜNCELLEME METODU ---
    public static String urunGuncelle(String eskiAd, String katAdi, String yeniAd, double fiyat, String aciklama, String gorsel, String malzemelerStr) {
        String sqlGetId = "SELECT ProductID FROM Urunler WHERE ProductName = ?";
        String sqlGetKatId = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String sqlUpdateUrun = "UPDATE Urunler SET CategoryID = ?, ProductName = ?, Price = ?, Description = ?, ImagePath = ? WHERE ProductID = ?";
        String sqlDeleteMalzemeler = "DELETE FROM Urun_Malzemeleri WHERE ProductID = ?";
        String sqlInsertMalzeme = "INSERT INTO Urun_Malzemeleri (ProductID, MalzemeAdi, VarsayilanVarMi, EkstraUcret) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            int productId = -1;
            int katId = -1;

            // 1. Ürün ID ve Kategori ID'yi Bul
            try (PreparedStatement pId = conn.prepareStatement(sqlGetId)) {
                pId.setString(1, eskiAd);
                ResultSet rs = pId.executeQuery();
                if(rs.next()) productId = rs.getInt("ProductID");
            }
            if(productId == -1) return "HATA|Güncellenecek ürün bulunamadı!";

            try (PreparedStatement pKat = conn.prepareStatement(sqlGetKatId)) {
                pKat.setString(1, katAdi);
                ResultSet rs = pKat.executeQuery();
                if(rs.next()) katId = rs.getInt("CategoryID");
            }

            // 2. Ürünün Ana Bilgilerini Güncelle
            try (PreparedStatement pUpd = conn.prepareStatement(sqlUpdateUrun)) {
                pUpd.setInt(1, katId); pUpd.setString(2, yeniAd); pUpd.setDouble(3, fiyat);
                pUpd.setString(4, aciklama); pUpd.setString(5, gorsel); pUpd.setInt(6, productId);
                pUpd.executeUpdate();
            }

            // 3. Ürünün Eski Malzemelerini Silip, Tablodaki Yeni Halini Ekle
            try (PreparedStatement pDelMat = conn.prepareStatement(sqlDeleteMalzemeler)) {
                pDelMat.setInt(1, productId);
                pDelMat.executeUpdate();
            }

            if (malzemelerStr != null && !malzemelerStr.isEmpty() && !malzemelerStr.equals("null")) {
                try (PreparedStatement pMat = conn.prepareStatement(sqlInsertMalzeme)) {
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
            return "BAŞARILI|Ürün ve içerikler başarıyla güncellendi!";
        } catch (Exception e) { return "HATA|Güncelleme hatası: " + e.getMessage(); }
    }
    // --- KATEGORİ SİLME METODU (Güvenlik Kontrollü) ---
    public static String kategoriSil(String categoryName) {
        // Kural: İçinde ürün olan kategori silinemez!
        String checkSql = "SELECT COUNT(*) FROM Urunler u JOIN Kategoriler k ON u.CategoryID = k.CategoryID WHERE k.CategoryName = ?";
        String delSql = "DELETE FROM Kategoriler WHERE CategoryName = ?";
        
        try (Connection conn = DriverManager.getConnection(URL)) {
            // Önce ürün var mı diye kontrol et
            try (PreparedStatement pCheck = conn.prepareStatement(checkSql)) {
                pCheck.setString(1, categoryName);
                ResultSet rs = pCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return "HATA|Silmek istediğiniz kategoride " + rs.getInt(1) + " adet ürün bulunuyor! Önce ürünleri silin.";
                }
            }
            // Ürün yoksa güvenle sil
            try (PreparedStatement pDel = conn.prepareStatement(delSql)) {
                pDel.setString(1, categoryName);
                pDel.executeUpdate();
                return "BAŞARILI|Kategori başarıyla silindi.";
            }
        } catch (Exception e) { return "HATA|Kategori silinemedi: " + e.getMessage(); }
    }
    // --- KULLANICI SİLME METODU ---
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

    // --- KULLANICI GÜNCELLEME METODU ---
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
    // MUTFAK, KASA VE VESTİYER İŞLEMLERİ
    // ==========================================

// ==========================================
    // MUTFAK VE KASA SİPARİŞ DURUM GÜNCELLEMELERİ
    // ==========================================
    
    public static String siparisDurumuGuncelle(int orderId, String yeniDurum) {
        // DİKKAT: Tablo adı 'Siparisler_Mutfak' olmalıdır.
        String sql = "UPDATE Siparisler_Mutfak SET Durum = ? WHERE OrderID = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, yeniDurum);
            pstmt.setInt(2, orderId);
            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                return "BAŞARILI|Sipariş durumu '" + yeniDurum + "' olarak güncellendi.";
            }
            return "HATA|Sipariş bulunamadı (Eski bir tabloyu veya silinmiş bir ID'yi arıyor olabilirsiniz).";
        } catch (Exception e) { 
            return "HATA|Durum güncellenemedi: " + e.getMessage(); 
        }
    }

    public static String siparisOdemeAl(int orderId, String odemeTuru) {
        // DİKKAT: Tablo adı 'Siparisler_Mutfak' olmalıdır.
        String sql = "UPDATE Siparisler_Mutfak SET Durum = 'ODENDI' WHERE OrderID = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, orderId);
            int affected = pstmt.executeUpdate();
            
            if (affected > 0) {
                return "BAŞARILI|Sipariş başarıyla kapatıldı (" + odemeTuru + ").";
            }
            return "HATA|Ödenecek sipariş bulunamadı.";
        } catch (Exception e) { 
            return "HATA|Ödeme alınamadı: " + e.getMessage(); 
        }
    }

    // Vestiyere Eşya Ekleme
    public static String vestiyerEkle(String masaNo, String askiNo) {
        String sql = "INSERT INTO Vestiyer_Kayitlari (MasaNo, AskiNumarasi) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaNo);
            pstmt.setString(2, askiNo);
            pstmt.executeUpdate();
            return "BAŞARILI|Eşya vestiyere eklendi. Askı No: " + askiNo;
        } catch (Exception e) { return "HATA|Vestiyer kaydı yapılamadı: " + e.getMessage(); }
    }

    // Vestiyerdeki Eşyayı Teslim Etme
    public static String vestiyerTeslimEt(int islemId) {
        String sql = "UPDATE Vestiyer_Kayitlari SET Durum = 'IADE_EDILDI' WHERE IslemID = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, islemId);
            pstmt.executeUpdate();
            return "BAŞARILI|Eşya müşteriye teslim edildi.";
        } catch (Exception e) { return "HATA|İşlem başarısız: " + e.getMessage(); }
    }
    // ==========================================
    // MASA YÖNETİM İŞLEMLERİ
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
        // Not: Masada aktif sipariş varsa silinmesini engelleyen bir kontrol eklenebilir.
        String sql = "DELETE FROM Masalar WHERE MasaAdi = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaAdi); pstmt.executeUpdate(); return "BAŞARILI|Masa silindi: " + masaAdi;
        } catch (Exception e) { return "HATA|Masa silinemedi: " + e.getMessage(); }
    }
    // ==========================================
    // MUTFAK VE SİPARİŞ HABERLEŞME METOTLARI
    // ==========================================
    public static String siparisOlustur(String masaIsmi, String musteriIsmi, String fisHTML) {
        String sql = "INSERT INTO Siparisler_Mutfak (MasaIsmi, MusteriIsmi, FisHTML) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masaIsmi);
            pstmt.setString(2, musteriIsmi);
            pstmt.setString(3, fisHTML);
            pstmt.executeUpdate();
            return "BAŞARILI|Sipariş Mutfağa İletildi!";
        } catch (Exception e) { return "HATA|Sipariş oluşturulamadı: " + e.getMessage(); }
    }

    public static String mutfakSiparisleriGetir() {
        // Mutfak sadece BEKLEMEDE ve HAZIRLANIYOR olanları görür
        StringBuilder sb = new StringBuilder("MUTFAK_VERI|");
        String sql = "SELECT OrderID, MasaIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Durum IN ('BEKLEMEDE', 'HAZIRLANIYOR') ORDER BY OrderID ASC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Verileri ~_~ ile, Siparişleri ise ||| ile ayırıyoruz (HTML kodları karışmasın diye)
                sb.append(rs.getInt("OrderID")).append("~_~")
                  .append(rs.getString("MasaIsmi")).append("~_~")
                  .append(rs.getString("Durum")).append("~_~")
                  .append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Mutfak verisi çekilemedi: " + e.getMessage(); }
    }
    public static String mutfakSiparisleriGetirFull() {
        StringBuilder sb = new StringBuilder("MUTFAK_FULL_VERI|");
        // Sadece bugünün BEKLEMEDE, HAZIRLANIYOR ve yeni tamamlanmış (HAZIR) siparişleri
        String sql = "SELECT OrderID, MasaIsmi, Durum, FisHTML FROM Siparisler_Mutfak " +
                    "WHERE Durum IN ('BEKLEMEDE', 'HAZIRLANIYOR', 'HAZIR') " +
                    "AND date(SiparisZamani) = date('now') ORDER BY OrderID DESC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~")
                .append(rs.getString("MasaIsmi")).append("~_~")
                .append(rs.getString("Durum")).append("~_~")
                .append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|" + e.getMessage(); }
    }
// ==========================================
    // KASA VE ÖDEME İŞLEMLERİ
    // ==========================================
    public static String kasaSiparisleriGetir() {
        StringBuilder sb = new StringBuilder("KASA_VERI|");
        // Kasada sadece henüz ödenmemiş veya iptal edilmemiş aktif siparişler görünür.
        String sql = "SELECT OrderID, MasaIsmi, MusteriIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Durum NOT IN ('ODENDI', 'IPTAL') ORDER BY OrderID DESC";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~")
                  .append(rs.getString("MasaIsmi")).append("~_~")
                  .append(rs.getString("MusteriIsmi")).append("~_~")
                  .append(rs.getString("Durum")).append("~_~")
                  .append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Kasa verisi çekilemedi: " + e.getMessage(); }
    }

// ==========================================
    // MASALARIN HAFIZASI (SİSTEM YENİDEN BAŞLADIĞINDA)
    // ==========================================
    public static String aktifMasalariGetir() {
        StringBuilder sb = new StringBuilder("AKTIF_MASALAR|");
        // DİKKAT: "MasaIsmi LIKE 'Masa %'" kısıtlamasını sildik! 
        // Paket ve Eve Servis hariç tüm masalar listeye dahil edilir.
        String sql = "SELECT MasaIsmi, Durum, SiparisZamani FROM Siparisler_Mutfak WHERE Durum NOT IN ('ODENDI', 'IPTAL') AND MasaIsmi NOT IN ('PAKET', 'EVE_SERVIS') GROUP BY MasaIsmi";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString("MasaIsmi")).append("~_~")
                  .append(rs.getString("Durum")).append("~_~")
                  .append(rs.getString("SiparisZamani")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Aktif masalar çekilemedi: " + e.getMessage(); }
    }
    // Kasa Geçmiş Arşivi
    public static String kasaGecmisSiparisleriGetir() {
        StringBuilder sb = new StringBuilder("KASA_GECMIS_VERI|");
        // Sadece ödenmiş ve iptal edilmiş fişleri son 50 taneye kadar getir (Kasmaması için limit konuldu)
        String sql = "SELECT OrderID, MasaIsmi, MusteriIsmi, Durum, FisHTML FROM Siparisler_Mutfak WHERE Durum IN ('ODENDI', 'IPTAL') ORDER BY OrderID DESC LIMIT 50";
        try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt("OrderID")).append("~_~")
                  .append(rs.getString("MasaIsmi")).append("~_~")
                  .append(rs.getString("MusteriIsmi")).append("~_~")
                  .append(rs.getString("Durum")).append("~_~")
                  .append(rs.getString("FisHTML")).append("|||");
            }
            return sb.toString();
        } catch (Exception e) { return "HATA|Geçmiş veriler çekilemedi: " + e.getMessage(); }
    }
// ==========================================
    // REZERVASYON İŞLEMLERİ (GELİŞMİŞ)
    // ==========================================
    public static String rezervasyonEkle(String masa, String musteri, String telefon, String tarih, String saat, String notlar) {
        // ÇİFTE REZERVASYON KONTROLÜ: Aynı masa, aynı tarihte zaten rezerve edilmiş mi?
        String checkSql = "SELECT COUNT(*) FROM Rezervasyonlar WHERE MasaIsmi = ? AND Tarih = ? AND Durum = 'AKTIF'";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, masa);
            checkStmt.setString(2, tarih);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "HATA|Bu masa " + tarih + " tarihinde zaten rezerve edilmiştir!";
            }
        } catch (Exception e) { return "HATA|Kontrol hatası: " + e.getMessage(); }

        // Müsaitse kaydı oluştur
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
    // Masaları boyamak için sadece BUGÜNÜN rezervasyonları
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
    
    public static String siparisOlustur(String masa, String musteri, String html, String urunlerData) {
        // datetime('now','localtime') sayesinde Türkiye saatine tam uyumlu kayıt yapar.
        String sql = "INSERT INTO Siparisler_Mutfak (MasaIsmi, MusteriIsmi, FisHTML, SiparisZamani, Durum) VALUES (?, ?, ?, datetime('now','localtime'), 'BEKLEMEDE')";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, masa); pstmt.setString(2, musteri); pstmt.setString(3, html);
            pstmt.executeUpdate();
            
            // ARKA PLAN: STOK DÜŞME VE GÜNLÜK SATIŞA YAZMA
            if (urunlerData != null && !urunlerData.isEmpty() && !urunlerData.equals("null")) {
                String[] urunler = urunlerData.split(",");
                for (String u : urunler) {
                    String[] detay = u.split(":");
                    if (detay.length == 2) {
                        String ad = detay[0]; int adet = Integer.parseInt(detay[1]);
                        // Stok Düş
                        try(PreparedStatement p1 = conn.prepareStatement("UPDATE Urunler SET Stok = Stok - ? WHERE UrunAdi = ?")) {
                            p1.setInt(1, adet); p1.setString(2, ad); p1.executeUpdate();
                        }
                        // Günlük Satış Listesine Ekle
                        try(PreparedStatement p2 = conn.prepareStatement("INSERT INTO GunlukSatislar (UrunAdi, Adet) VALUES (?, ?)")) {
                            p2.setString(1, ad); p2.setInt(2, adet); p2.executeUpdate();
                        }
                    }
                }
            }
            return "BAŞARILI|Sipariş oluşturuldu.";
        } catch (Exception e) { return "HATA|" + e.getMessage(); }
    }

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
                if (fBas != -1) {
                    int fEnd = html.indexOf("</b>", fBas);
                    if (fEnd != -1) {
                        try { ciro += Double.parseDouble(html.substring(fBas + 3, fEnd).replace(" TL", "").replace(",", ".").trim()); } catch(Exception ignored){}
                    }
                }
            }
        } catch (Exception e) { return "HATA|Hesaplama hatası: " + e.getMessage(); }

        if (toplam == 0) return "HATA|Gün sonu alınacak tamamlanmış sipariş (ciro) bulunamadı!";

        StringBuilder detayStr = new StringBuilder();
        for (Map.Entry<String, Integer> entry : masaDetay.entrySet()) detayStr.append(entry.getKey()).append(":").append(entry.getValue()).append(",");

        // === STOK (SATIŞ) ANALİZİNİ ÇIKAR VE SIFIRLA ===
        StringBuilder stokDetayStr = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(URL)) {
            String satisSql = "SELECT UrunAdi, SUM(Adet) as ToplamAdet FROM GunlukSatislar GROUP BY UrunAdi";
            try (Statement st = conn.createStatement(); ResultSet rsSatis = st.executeQuery(satisSql)) {
                while(rsSatis.next()) {
                    String uAd = rsSatis.getString("UrunAdi"); int tAdet = rsSatis.getInt("ToplamAdet");
                    int kalanStok = 0;
                    try(PreparedStatement psStok = conn.prepareStatement("SELECT Stok FROM Urunler WHERE UrunAdi = ?")) {
                        psStok.setString(1, uAd); ResultSet rsStok = psStok.executeQuery();
                        if(rsStok.next()) kalanStok = rsStok.getInt("Stok");
                    }
                    stokDetayStr.append(uAd).append(" : ").append(tAdet).append(" adet satıldı (Stokta Kalan: ").append(kalanStok).append(")|");
                }
            }
            try(Statement st2 = conn.createStatement()) { st2.executeUpdate("DELETE FROM GunlukSatislar"); } // Günü Sıfırla
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
    // KURYE / MOTORCU İŞLEMLERİ
    // ==========================================
    public static String kuryeListesiGetir() {
        // Sistemde dinamik personel tablosu yoksa test edebilmen için sahte kuryeler oluşturur
        return "KURYE_LISTESI|Ahmet Motorcu|Mehmet Kurye|Hızlı Ali";
    }

    public static String kuryeAta(int orderId, String kuryeAdi) {
        String sql = "UPDATE Siparisler_Mutfak SET Kurye = ?, Durum = 'YOLA_CIKTI' WHERE OrderID = ?";
        try (Connection conn = DriverManager.getConnection(URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kuryeAdi); pstmt.setInt(2, orderId); pstmt.executeUpdate();
            return "BAŞARILI|Kurye (" + kuryeAdi + ") atandı ve sipariş yola çıktı.";
        } catch (Exception e) { return "HATA|Kurye atanamadı: " + e.getMessage(); }
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
    
}