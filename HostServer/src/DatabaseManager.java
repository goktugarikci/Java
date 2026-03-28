package src;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:uygulama_veritabani.db";

    /**
     * Uygulama başladığında tabloları ve ilişkileri kurar.
     */
    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            // SQLite'ta Foreign Key desteğini açıyoruz
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1. Kullanicilar Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Kullanicilar (" +
                    "UserID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserName TEXT NOT NULL UNIQUE, " +
                    "Password TEXT NOT NULL, " +
                    "FirstName TEXT, " +
                    "LastName TEXT, " +
                    "Role TEXT, " + // Admin, Staff, Personel
                    "Email TEXT, " +
                    "Phone TEXT, " +
                    "Address TEXT);");

            // 2. Kategoriler Tablosu
            stmt.execute("CREATE TABLE IF NOT EXISTS Kategoriler (" +
                    "CategoryID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CategoryName TEXT NOT NULL UNIQUE, " +
                    "Description TEXT);");

            // 3. Urunler Tablosu (Stok Takibi Dahil)
            stmt.execute("CREATE TABLE IF NOT EXISTS Urunler (" +
                    "ProductID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "CategoryID INTEGER, " +
                    "ProductName TEXT NOT NULL, " +
                    "Description TEXT, " +
                    "Price REAL, " +
                    "Stock INTEGER DEFAULT 0, " +
                    "KdvRate REAL, " +
                    "FOREIGN KEY(CategoryID) REFERENCES Kategoriler(CategoryID));");

            // 4. Siparisler Tablosu (Gelişmiş Tip ve Durum Takibi)
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparisler (" +
                    "OrderID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "UserID INTEGER, " +
                    "OrderType TEXT, " + // MASA, PAKET_SERVIS, GEL_AL
                    "TableNumber TEXT, " + // Masa Numarası (Opsiyonel)
                    "Status TEXT DEFAULT 'BEKLEMEDE', " + // BEKLEMEDE, HAZIRLANIYOR, HAZIR, TESLIM_EDILDI, IPTAL, KASA_KAPATILDI
                    "OrderDate DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "TotalPrice REAL DEFAULT 0, " +
                    "PaymentType TEXT, " + // Nakit, Kredi Kartı
                    "FOREIGN KEY(UserID) REFERENCES Kullanicilar(UserID));");

            // 5. Siparis_Detaylari
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparis_Detaylari (" +
                    "DetailID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "OrderID INTEGER, " +
                    "ProductID INTEGER, " +
                    "Quantity INTEGER, " +
                    "UnitPrice REAL, " +
                    "FOREIGN KEY(OrderID) REFERENCES Siparisler(OrderID), " +
                    "FOREIGN KEY(ProductID) REFERENCES Urunler(ProductID));");

            // 6. Siparis_Zaman_Loglari (Performans Analizi İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Siparis_Zaman_Loglari (" +
                    "LogID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "OrderID INTEGER, " +
                    "Status TEXT, " +
                    "IslemZamani DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(OrderID) REFERENCES Siparisler(OrderID));");

            // 7. Gunluk_Raporlar (Z Raporu ve Hata Takibi İçin)
            stmt.execute("CREATE TABLE IF NOT EXISTS Gunluk_Raporlar (" +
                    "RaporID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "RaporTarihi DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "ToplamCiro REAL, " +
                    "StokHatalari TEXT, " + // Eksiye düşen stoklar burada raporlanır
                    "KapatanAdmin TEXT);");
            // Ürünlerin standart içeriği ve eklenebilir ekstraları
            stmt.execute("CREATE TABLE IF NOT EXISTS Urun_Malzemeleri (" +
                    "MalzemeID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ProductID INTEGER, " +
                    "MalzemeAdi TEXT, " +
                    "VarsayilanVarMi BOOLEAN DEFAULT 1, " + // Üründe standart olarak var mı?
                    "EkstraUcret REAL DEFAULT 0, " +        // Ekstra eklenirse maliyeti
                    "FOREIGN KEY(ProductID) REFERENCES Urunler(ProductID));");

            // Varsayılan yöneticiyi ekle
            ilkAdminKoy(conn);
            
            System.out.println("Veritabanı Sistemi: Tüm modüller güncel ve aktif.");
            
        } catch (Exception e) {
            System.err.println("Veritabanı Başlatma Hatası: " + e.getMessage());
        }
    }

    private static void ilkAdminKoy(Connection conn) {
        String sql = "INSERT OR IGNORE INTO Kullanicilar (UserName, Password, Role, FirstName, LastName) " +
                     "VALUES ('admin', 'admin123', 'Admin', 'Sistem', 'Yöneticisi')";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ignored) {}
    }

    // --- KASA KAPATMA MANTIĞI (SİSTEM SIFIRLAMA) ---
    public static String kasaKapat(String adminIsmi) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            // 1. Kural: Tamamlanmamış (Aktif) sipariş var mı kontrol et
            String aktifSiparisSorgu = "SELECT COUNT(*) FROM Siparisler WHERE Status NOT IN ('TESLIM_EDILDI', 'IPTAL', 'KASA_KAPATILDI')";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(aktifSiparisSorgu)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "HATA|İçeride hala " + rs.getInt(1) + " adet aktif sipariş var. Kasa kapatılamaz!";
                }
            }

            // 2. Analiz: Stokta eksiye düşen ürünleri yakala (Geçmişe dönük rapor için)
            StringBuilder stokHatalari = new StringBuilder();
            String stokSorgu = "SELECT ProductName, Stock FROM Urunler WHERE Stock < 0";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(stokSorgu)) {
                while (rs.next()) {
                    stokHatalari.append(rs.getString("ProductName")).append(" (").append(rs.getInt("Stock")).append("), ");
                }
            }

            // 3. Ciro Hesapla
            double toplamCiro = 0;
            String ciroSorgu = "SELECT SUM(TotalPrice) FROM Siparisler WHERE Status = 'TESLIM_EDILDI'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(ciroSorgu)) {
                if (rs.next()) toplamCiro = rs.getDouble(1);
            }

            // 4. Günlük Raporu Kaydet
            String raporSql = "INSERT INTO Gunluk_Raporlar (ToplamCiro, StokHatalari, KapatanAdmin) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(raporSql)) {
                pstmt.setDouble(1, toplamCiro);
                pstmt.setString(2, stokHatalari.toString().isEmpty() ? "Hata Yok" : stokHatalari.toString());
                pstmt.setString(3, adminIsmi);
                pstmt.executeUpdate();
            }

            // 5. Siparişleri 'KASA_KAPATILDI' olarak işaretleyerek yeni günü temizle
            conn.createStatement().execute("UPDATE Siparisler SET Status = 'KASA_KAPATILDI' WHERE Status = 'TESLIM_EDILDI'");

            return "BAŞARILI|Gün sonu alındı. Toplam Ciro: " + toplamCiro + " TL. Arşivleme tamam.";

        } catch (Exception e) {
            return "HATA|Kasa kapatma işlemi başarısız: " + e.getMessage();
        }
    }

    // --- KULLANICI EKLEME (GENİŞLETİLMİŞ) ---
    public static String kullaniciEkle(String user, String pass, String ad, String soyad, String rol, String email, String tel, String adres) {
        String sql = "INSERT INTO Kullanicilar (UserName, Password, FirstName, LastName, Role, Email, Phone, Address) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user); pstmt.setString(2, pass);
            pstmt.setString(3, ad); pstmt.setString(4, soyad);
            pstmt.setString(5, rol); pstmt.setString(6, email);
            pstmt.setString(7, tel); pstmt.setString(8, adres);
            pstmt.executeUpdate();
            return "BAŞARILI|Yeni kullanıcı sisteme tanımlandı.";
        } catch (Exception e) {
            return "HATA|Kullanıcı eklenemedi: " + e.getMessage();
        }
    }

    // --- GİRİŞ KONTROLÜ ---
    public static String girisYap(String user, String pass) {
        String sql = "SELECT Role, FirstName, LastName FROM Kullanicilar WHERE UserName = ? AND Password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "BAŞARILI|" + rs.getString("Role") + "|" + rs.getString("FirstName") + " " + rs.getString("LastName");
            }
            return "HATA|Kullanıcı adı veya şifre yanlış.";
        } catch (Exception e) {
            return "HATA|Veritabanı hatası.";
        }
    }
    // --- KATEGORİ EKLEME METODU ---
    public static String kategoriEkle(String categoryName, String description) {
        String sql = "INSERT INTO Kategoriler (CategoryName, Description) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, categoryName);
            pstmt.setString(2, description);
            
            pstmt.executeUpdate();
            return "BAŞARILI|Kategori eklendi: " + categoryName;
            
        } catch (Exception e) {
            return "HATA|Kategori eklenemedi: " + e.getMessage();
        }
    }

    // --- ÜRÜN EKLEME METODU ---
    public static String urunEkle(int categoryId, String productName, String description, double price, int stock, double kdvRate) {
        String sql = "INSERT INTO Urunler (CategoryID, ProductName, Description, Price, Stock, KdvRate) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, categoryId);
            pstmt.setString(2, productName);
            pstmt.setString(3, description);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, stock);
            pstmt.setDouble(6, kdvRate);
            
            pstmt.executeUpdate();
            return "BAŞARILI|Ürün eklendi: " + productName;
            
        } catch (Exception e) {
            return "HATA|Ürün eklenemedi: " + e.getMessage();
        }
    }
    public static String urunEkleIsimle(String katAdi, String urunAdi, String aciklama, double fiyat, int stok, double kdv) {
        String idSorgu = "SELECT CategoryID FROM Kategoriler WHERE CategoryName = ?";
        String urunSql = "INSERT INTO Urunler (CategoryID, ProductName, Description, Price, Stock, KdvRate) VALUES (?, ?, ?, ?, ?, ?)";
        
        // Her iki işlemi de TEK bir connection ve TRY bloğu içinde yapıyoruz
        try (Connection conn = DriverManager.getConnection(URL)) {
            int katId = -1;
            
            // 1. Önce ID'yi bul
            try (PreparedStatement pstmtId = conn.prepareStatement(idSorgu)) {
                pstmtId.setString(1, katAdi);
                ResultSet rs = pstmtId.executeQuery();
                if (rs.next()) {
                    katId = rs.getInt("CategoryID");
                }
            }

            if (katId == -1) return "HATA|Kategori bulunamadı!";

            // 2. Şimdi Ürünü Ekle (Aynı bağlantı üzerinden)
            try (PreparedStatement pstmtUrun = conn.prepareStatement(urunSql)) {
                pstmtUrun.setInt(1, katId);
                pstmtUrun.setString(2, urunAdi);
                pstmtUrun.setString(3, aciklama);
                pstmtUrun.setDouble(4, fiyat);
                pstmtUrun.setInt(5, stok);
                pstmtUrun.setDouble(6, kdv);
                pstmtUrun.executeUpdate();
                return "BAŞARILI|Ürün başarıyla eklendi: " + urunAdi;
            }
            
        } catch (Exception e) {
            return "HATA|Veritabanı kilitlendi veya hata oluştu: " + e.getMessage();
        }
    }

    public static String kategorileriGetir() {
        StringBuilder sb = new StringBuilder("KAT_LISTESI");
        String sql = "SELECT CategoryName FROM Kategoriler";
        try (Connection conn = DriverManager.getConnection(URL);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sb.append("|").append(rs.getString("CategoryName"));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HATA|Kategoriler çekilemedi: " + e.getMessage();
        }
    }
    public static String urunleriGetir(String katAdi) {
        StringBuilder sb = new StringBuilder("URUN_LISTESI");
        String sql = "SELECT ProductName, Price, Stock FROM Urunler " +
                    "JOIN Kategoriler ON Urunler.CategoryID = Kategoriler.CategoryID " +
                    "WHERE Kategoriler.CategoryName = ?";
        
        try (Connection conn = DriverManager.getConnection(URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, katAdi);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                sb.append("|").append(rs.getString("ProductName"))
                .append(";").append(rs.getDouble("Price"))
                .append(";").append(rs.getInt("Stock"));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HATA|Ürünler çekilemedi: " + e.getMessage();
        }

    }
    public static String urunleriDetayliGetir(String katAdi) {
        StringBuilder sb = new StringBuilder("URUN_LISTESI");
        // Görseldeki sütun isimlerine (ProductName, Price, Stock, Description) göre sorgu:
        String sql = "SELECT u.ProductName, u.Price, u.Stock, u.Description " +
                    "FROM Urunler u " +
                    "JOIN Kategoriler k ON u.CategoryID = k.CategoryID " +
                    "WHERE k.CategoryName = ?";
        
        try (Connection conn = DriverManager.getConnection(URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, katAdi);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                sb.append("|").append(rs.getString("ProductName"))
                .append(";").append(rs.getDouble("Price"))
                .append(";").append(rs.getString("Description")) // Görseldeki Description'ı çekiyoruz
                .append(";").append(rs.getInt("Stock"));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HATA|Ürünler yüklenemedi: " + e.getMessage();
        }
    }
}