🏠 Smart Home Control

Smart Home Control, kullanıcıların akıllı ev cihazlarını uzaktan yönetebilmesine imkân sağlayan, modern arayüzlü ve Firebase tabanlı bir mobil uygulamadır.
Sistem; cihaz kontrolü, gerçek zamanlı sensör verisi, tema değiştirme, profil yönetimi ve güvenli kullanıcı doğrulama gibi özellikler sunar.

📱 Özellikler
🔐 Kullanıcı Girişi & Kayıt

Firebase Authentication ile güvenli e-posta/şifre girişi

Yeni kullanıcı oluşturma

Şifre güncelleme

Güvenli çıkış işlemi

⚡ Gerçek Zamanlı Kontrol

LED, fan, su seviyesi ve PIR sensör durumlarını anlık görme

Cihazları tek dokunuşla açma/kapatma

Değerler Firebase Realtime Database üzerinden canlı güncellenir

🌙 Tema Değiştirici (Dark / Light Mode)

Modern ışık ve karanlık tema desteği

Seçilen tema DataStore ile kalıcı olarak saklanır

UI, temaya göre dinamik olarak güncellenir

👤 Profil Sayfası

Profil fotoğrafı yükleme (Base64 olarak Firebase'e kaydedilir)

Şifre değiştirme

Hesap oluşturulma tarihi ve kayıtlı e-posta görüntüleme

🎨 Modern UI (Jetpack Compose)

Gradient arka planlar

Material 3 tasarım bileşenleri

Yuvarlatılmış kartlar, yumuşak geçişler

Responsive ve temiz Compose mimarisi

🤖 Kullanılan Teknolojiler
Teknoloji	Amaç
Kotlin (Jetpack Compose)	Modern UI geliştirme
Firebase Authentication	Kullanıcı doğrulama
Firebase Realtime Database	Gerçek zamanlı veri depolama
Material 3	Arayüz bileşenleri ve tema desteği
DataStore Preferences	Kalıcı tema ve dil ayarları
🗂️ Proje Dosya Yapısı
SmartHomeControl/
├── StartActivity.kt            # Splash / Oturum kontrolü
│                                - Login veya Main yönlendirmesi
│                                - Kullanıcı ilk giriş kaydı
│                                - Tema / dil uyumu
│
├── MainActivity.kt             # Ana ekran (Dashboard / Profil / Ayarlar)
│                                - Realtime kontrol
│                                - Navigasyon bar
│
├── LoginActivity.kt            # Kullanıcı girişi
│                                - Firebase auth
│                                - Modern tasarım
│
├── RegisterActivity.kt         # Yeni kullanıcı kaydı
│
├── ProfileActivity.kt          # Profil yönetimi
│                                - Fotoğraf yükleme
│                                - Şifre güncelleme
│
├── data/
│   ├── ThemePreferences.kt     # Tema & dil ayarları (DataStore)
│
├── ui/theme/
│   ├── SmartHomeTheme.kt       # Material 3 uyumlu tema ayarları
│
└── res/
    ├── drawable/
    │   ├── logo_light.png
    │   └── logo_dark.png
    └── values/
        ├── strings.xml
        └── colors.xml

⚙️ Kurulum
1️⃣ Projeyi klonla:
git clone https://github.com/<kullaniciadi>/SmartHomeControl.git

2️⃣ Android Studio’da projeyi aç.
3️⃣ Firebase’i yapılandır:

Authentication → Email/Password etkinleştir

Realtime Database → Start in test mode seç

Firebase’den aldığın google-services.json dosyasını:

app/
└── google-services.json


içine ekle

4️⃣ Çalıştır 🚀
🌟 Tema Önizlemesi

Aşağıya koyacağın ekran görüntüleri için yer hazır:

Karanlık Tema	/   Aydınlık Tema
<img width="431" height="844" alt="SmartHomeControlDark" src="https://github.com/user-attachments/assets/5bb28be1-ee0b-4de3-a078-527b7c1fa468" />
<img width="394" height="836" alt="SmartHomeControlLight" src="https://github.com/user-attachments/assets/9702ef06-d2af-4cc3-85ca-27c116c99cd4" />

