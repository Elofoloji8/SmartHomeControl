# 🏠 Smart Home Control

**Smart Home Control**, kullanıcıların akıllı ev cihazlarını (LED, fan, sensör, su seviyesi vb.) uzaktan yönetebilmesini sağlayan, Firebase tabanlı bir **mobil uygulamadır**. Uygulama; kullanıcı girişi, tema değiştirme, profil yönetimi ve gerçek zamanlı veri takibi gibi modern fonksiyonlara sahiptir.

---

## 📱 Özellikler

✅ **Kullanıcı Girişi & Kayıt**

* Firebase Authentication ile e-posta/şifre tabanlı oturum açma.
* Kayıt olma, şifre yenileme, çıkış yapma.

✅ **Gerçek Zamanlı Kontrol**

* Işık, fan, su seviyesi ve hareket sensörü durumu anlık olarak Firebase'den okunur.
* Kullanıcılar cihazlarını anında açabilir/kapatabilir.

✅ **Tema Değiştirici (Dark/Light Mode)**

* Aydınlık ve karanlık tema desteği.
* Kullanıcı tercihi DataStore ile kalıcı olarak saklanır.

✅ **Profil Sayfası**

* Profil fotoğrafı yükleme (Base64 formatında Firebase'e kaydedilir).
* Şifre güncelleme, e-posta görüntüleme.

✅ **Modern UI (Jetpack Compose)**

* Gradient arka planlar, Material 3 tasarımı.
* Yuvarlatılmış kartlar ve yumuşak animasyonlar.

---

## 🤖 Teknolojiler

| Teknoloji                      | Amaç                                  |
| ------------------------------ | ------------------------------------- |
| **Kotlin (Jetpack Compose)**   | Modern UI framework.                  |
| **Firebase Authentication**    | Kullanıcı doğrulama.                  |
| **Firebase Realtime Database** | Gerçek zamanlı veri depolama.         |
| **Material 3**                 | Tasarım bileşenleri ve renk temaları. |
| **DataStore Preferences**      | Tema tercihi kaydı.                   |

---

## 🖥️ Dosya Yapısı

```
SmartHomeControl/
├── StartActivity.kt            → Uygulama açılış ekranı (Splash)
│                                 - Kullanıcı oturum kontrolü
│                                 - Login veya Main'e yönlendirme
│                                 - Firebase'e ilk giriş bilgisi kaydı
│                                 - Otomatik tema uyumu
│
├── MainActivity.kt             → Ana ekran (Dashboard, Profil, Ayarlar sekmeleri)
│                                 - Gerçek zamanlı cihaz kontrolü
│                                 - Navigasyon bar
│
├── LoginActivity.kt            → Giriş ekranı
│                                 - Firebase Authentication girişi
│                                 - Logo görünümü ve modern tasarım
│
├── RegisterActivity.kt         → Kayıt ekranı
│                                 - Yeni kullanıcı oluşturma
│                                 - Firebase'e veri kaydı
│
├── ProfileActivity.kt          → Profil sayfası
│                                 - Profil fotoğrafı, şifre güncelleme
│                                 - Tema uyumlu arayüz
│
├── data/
│   ├── ThemePreferences.kt     → Tema değişikliği için DataStore
│
├── ui/theme/
│   ├── SmartHomeTheme.kt       → Tema renkleri, fontlar, Material 3 ayarları
│
└── res/
    ├── drawable/
    │   ├── logo_light.png      → Aydınlık tema logosu
    │   └── logo_dark.png       → Karanlık tema logosu
    └── layout/                 → Compose layout bileşenleri
```

---

## ⚙️ Kurulum Adımları

1. Bu projeyi klonla:

   ```bash
   git clone https://github.com/<kullaniciadi>/SmartHomeControl.git
   ```
2. Android Studio’da projeyi aç.
3. Firebase ayarlarını yap:

   * Authentication → E-posta/Şifre aktif et.
   * Realtime Database → **Start in test mode** seç.
   * `google-services.json` dosyasını `app/` klasörüne ekle.
4. Uygulamayı çalıştır 🚀

---

## 🌟 Tema Önizlemesi

| Aydınlık Mod 
                                                                                                                                                                                                | Karanlık Mod                                                                             |
| ------------------------------------------------------------------------------------------                                                        | ---------------------------------------------------------------------------------------- |
| <img width="431" height="844" alt="SmartHomeControlDark" src="https://github.com/user-attachments/assets/9986ca1d-f7c9-489a-bf56-43662a672e89" /> | <img width="394" height="836" alt="SmartHomeControlLight" src="https://github.com/user-attachments/assets/845c4595-d4d0-4e9b-83ed-c10e5e490aca" /> |

---

