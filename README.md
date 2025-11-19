# 🏠 Smart Home Control

Smart Home Control, kullanıcıların akıllı ev cihazlarını (LED, fan, sensör, su seviyesi vb.) uzaktan yönetebilmesini sağlayan Firebase tabanlı bir mobil uygulamadır. Uygulama; kullanıcı girişi, tema değiştirme, profil yönetimi ve gerçek zamanlı veri takibi gibi modern fonksiyonlara sahiptir.

---

## 📱 Özellikler

### ✅ Kullanıcı Girişi & Kayıt
- Firebase Authentication ile e-posta/şifre tabanlı oturum açma  
- Kayıt olma ve güvenli giriş  
- Şifre güncelleme  
- Çıkış yapma  

### ✅ Gerçek Zamanlı Kontrol
- LED, fan, su seviyesi ve hareket sensörü verileri gerçek zamanlı Firebase’den okunur  
- Cihazlar anlık olarak açılıp kapatılabilir  

### ✅ Tema Değiştirici (Dark / Light Mode)
- Aydınlık & karanlık tema desteği  
- Kullanıcı tercihi DataStore ile kalıcı olarak saklanır  

### ✅ Profil Sayfası
- Profil fotoğrafı yükleme (Base64 formatında Firebase’e kaydedilir)  
- Şifre güncelleme  
- Hesap oluşturulma tarihini görüntüleme  

### ✅ Modern UI (Jetpack Compose)
- Gradient arka planlar  
- Material 3 bileşenleri  
- Yuvarlatılmış kart tasarımları  
- Minimal ve modern arayüz  

---

## 🤖 Teknolojiler

| Teknoloji | Açıklama |
|----------|----------|
| **Kotlin (Jetpack Compose)** | Modern UI framework |
| **Firebase Authentication** | Kullanıcı doğrulama |
| **Firebase Realtime Database** | Gerçek zamanlı veri depolama |
| **Material 3** | Modern UI bileşenleri |
| **DataStore Preferences** | Tema & dil tercihlerinin saklanması |

---

## 🖥️ Dosya Yapısı

SmartHomeControl/
├── StartActivity.kt
│ └── Splash ekranı, oturum kontrolü, tema/dil yükleme
│
├── MainActivity.kt
│ └── Dashboard, Profil, Ayarlar sekmeleri
│
├── LoginActivity.kt
│ └── Firebase ile kullanıcı girişi
│
├── RegisterActivity.kt
│ └── Yeni kullanıcı oluşturma
│
├── ProfileActivity.kt
│ └── Profil fotoğrafı, şifre güncelleme
│
├── data/
│ └── ThemePreferences.kt # Tema ve dil ayarları (DataStore)
│
├── ui/theme/
│ └── SmartHomeTheme.kt # Tema renkleri, Material 3 ayarları
│
└── res/
├── drawable/ # Logo, ikonlar
├── values/ # strings.xml, colors.xml
└── mipmap/ # Uygulama ikonları

---

## ⚙️ Kurulum Adımları

### 1️⃣ Projeyi klonla
```bash
git clone https://github.com/<kullaniciadi>/SmartHomeControl.git
2️⃣ Android Studio ile aç
3️⃣ Firebase yapılandırması
✔ Authentication
“Email/Password” yöntemini aktif edin

✔ Realtime Database
“Start in Test Mode” ile başlatın

✔ google-services.json ekleyin
Firebase’den indirin → app/ klasörüne ekleyin

4️⃣ Uygulamayı çalıştır 🚀

🌟 Tema Önizlemesi
Karanlık Mod<img width="431" height="844" alt="SmartHomeControlDark" src="https://github.com/user-attachments/assets/82e13cf6-8cb4-4496-885c-967195064ac1" />
	Aydınlık Mod<img width="394" height="836" alt="SmartHomeControlLight" src="https://github.com/user-attachments/assets/d54c0fa2-e84a-401f-a565-6f5d9db12ea1" />
