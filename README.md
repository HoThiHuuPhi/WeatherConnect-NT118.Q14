# WeatherConnect ☁️🌪️
## 💡 Đồ án: WeatherConnect
> Đồ án môn **Phát triển ứng dụng trên thiết bị di động – NT118.Q14** – Trường Đại học Công nghệ Thông tin (UIT).  
> Ứng dụng theo dõi thời tiết theo vị trí, kết nối cộng đồng theo khu vực và hỗ trợ **SOS/cứu hộ** trong tình huống thiên tai.

<img width="1200" alt="Cover" src="https://github.com/user-attachments/assets/d400b17e-a577-4d07-87d7-97d389eb29f8" />

---

## 🌟 Tính năng nổi bật

| Tính năng | Mô tả |
|---|---|
| Xem thời tiết theo vị trí | Lấy vị trí hiện tại, hiển thị thời tiết hiện tại + dự báo (hourly/daily) từ **Open‑Meteo** |
| Hình nền thời tiết động | Dynamic Weather Background thay đổi theo trạng thái thời tiết (nắng/mưa/đêm, …) |
| Tìm kiếm địa điểm | Tìm theo tên địa điểm → chuyển đổi sang tọa độ và xem dự báo |
| Bản đồ thời tiết | Nhúng bản đồ thời tiết qua **WebView** (VentuSky) |
| SOS 1 chạm | Gửi SOS kèm tọa độ + nội dung lên Firebase |
| Offline SOS Queue | Mất mạng vẫn “gửi” được: SOS lưu cục bộ, có mạng sẽ tự đồng bộ |
| Danh sách SOS (Monitor) | Xem danh sách SOS theo thời gian; mở chi tiết trên bản đồ |
| Bản đồ cứu hộ (Overview/Detail) | Overview: nhiều marker + bottom sheet; Detail: 1 SOS + vị trí tôi + chỉ đường |
| Chat cộng đồng theo khu vực | Chat realtime (Firebase) + tag mức độ **Thông tin / Cảnh báo / Khẩn cấp**, hỗ trợ ảnh |
| Cài đặt & hồ sơ | Quản lý một số tuỳ chọn (DataStore) + đăng xuất |

---

## 🧱 Công nghệ sử dụng

- **Ngôn ngữ:** Kotlin  
- **UI:** Jetpack Compose + Material 3  
- **Kiến trúc:** MVVM + Navigation Compose (Single-Activity)  
- **Networking:** Retrofit + Gson (Open‑Meteo)  
- **Backend:** Firebase Authentication, Firestore, Storage, FCM  
- **Bản đồ & định vị:** Fused Location Provider + VietMap SDK, mở chỉ đường bằng Intent (Google Maps)  
- **Offline-first:** DataStore + NetworkMonitor + SOS Queue  
- **Khác:** Coroutines/Flow, Coil (hiển thị ảnh)

---

## 🧭 Điều hướng (Userflow / Navigation)

### Các màn hình (Routes)
| Route | Màn hình | Ghi chú |
|---|---|---|
| `login` | Đăng nhập | Điều hướng sang Register/Forgot |
| `register` | Đăng ký | Tạo tài khoản Firebase |
| `forgot_password` | Quên mật khẩu | Gửi email reset |
| `main` | Trang chính | Thời tiết + mở SOS + mở AI Advisor |
| `search` | Tìm địa điểm | Tìm và xem dự báo |
| `weather_map` | Bản đồ thời tiết | WebView VentuSky |
| `chat` | Chat cộng đồng | Realtime + tag mức độ |
| `settings` | Cài đặt | Logout + tuỳ chọn |
| `rescue_map_overview` | Bản đồ SOS tổng quan | Nhiều marker + mở danh sách |
| `rescue_list` | Danh sách SOS | Mở SOS detail map |
| `rescue_map/{lat}/{lon}/{name}` | Bản đồ cứu hộ (chi tiết) | 1 SOS + vị trí tôi + chỉ đường |

### Mermaid (sơ đồ luồng đơn giản, đủ ý)
> Có thể dán đoạn dưới vào: Mermaid Live Editor / diagrams.net (Mermaid plugin) / Markdown preview hỗ trợ Mermaid.

```mermaid
flowchart TD
  A([Mở ứng dụng]) --> B{Đã đăng nhập?}
  B -- Không --> L[Login] 
  L -->|Đăng ký| R[Register] --> L
  L -->|Quên mật khẩu| F[Forgot Password] --> L
  L -->|Đăng nhập thành công| M[Main]

  B -- Có --> M[Main]

  %% Main modules
  M --> S[Search]
  M --> WM[Weather Map (WebView)]
  M --> C[Community Chat]
  M --> ST[Settings]
  M --> SO[SOS Dialog]
  M --> AI[AI Advisor]

  %% SOS flow
  SO -->|Gửi SOS| OK[Thông báo thành công]
  OK -->|Mở danh sách| RL[Rescue List (SOS Monitor)]
  OK -->|Đóng| M

  %% Rescue map
  M --> OV[Rescue Map Overview]
  OV -->|Mở danh sách| RL
  OV -->|Chọn 1 SOS| RM[Rescue Map Detail]
  RL -->|Chọn 1 SOS| RM
  RM -->|Chỉ đường| GM[Google Maps]
  RM -->|Quay lại| OV

  %% Back actions
  S --> M
  WM --> M
  C --> M
  ST -->|Logout| L
  ST --> M
```

---

## 🗂️ Cấu trúc thư mục (tham khảo)

```
app/src/main/java/com/example/doanck/
├─ navigation/         # AppNav (Navigation Compose routes)
├─ ui/
│  ├─ auth/           # Forgot password
│  ├─ login/          # Login screen
│  ├─ register/       # Register screen
│  ├─ main/           # Main + Search + Map + SOS screens
│  ├─ chat/           # Community chat UI
│  └─ settings/       # Settings UI
├─ data/
│  ├─ api/            # RetrofitClient + WeatherService
│  └─ datastore/      # AppDataStore (settings, session, SOS queue)
├─ utils/             # NetworkMonitor, helpers
└─ ...                # Service/Notification (SOSService, FCM)
```

---

## ▶️ Cách chạy dự án

### 1) Yêu cầu môi trường
- Android Studio (khuyến nghị bản mới)
- JDK 17 (theo yêu cầu Android Gradle Plugin)
- Thiết bị/AVD có Google Play services (để dùng Location)

### 2) Mở project
1. Clone / tải project
2. Mở bằng Android Studio
3. Sync Gradle

### 3) Firebase
Project đã dùng:
- Firebase Authentication
- Firestore
- Storage
- Cloud Messaging (FCM)

> Nếu bạn dùng Firebase project của riêng bạn: tạo app Android trên Firebase Console → tải **google-services.json** và đặt vào `app/`.

### 4) API Keys (khuyến nghị)
- **VietMap**: không nên hard-code trong source.  
  → nên đưa vào `local.properties` và đọc bằng `BuildConfig` (hoặc thay trực tiếp trong code khi demo).

Ví dụ `local.properties` (không commit):
```properties
VIETMAP_API_KEY=YOUR_KEY_HERE
ANTHROPIC_API_KEY=YOUR_KEY_HERE
```

> Lưu ý: phần AI Advisor gọi Claude/Anthropic cần API key hợp lệ. Nếu không có key, app có thể fallback sang gợi ý mặc định (tuỳ code).

### 5) Chạy
- Chọn thiết bị → Run `app`

---

## 🔐 Quyền truy cập (Permissions)
- `INTERNET`, `ACCESS_NETWORK_STATE` (gọi API + kiểm tra mạng)
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (lấy vị trí, SOS)
- `POST_NOTIFICATIONS` (Android 13+) (thông báo)

---

## 🎬 Demo flow gợi ý (1–2 phút)
1. Login → Main (thời tiết)
2. Mở **AI Advisor**
3. Mở **SOS** → gửi thử → mở **Danh sách SOS**
4. Mở **Bản đồ SOS tổng quan** → chọn SOS → **Chỉ đường**
5. Vào **Chat** gửi tin nhắn (tag Cảnh báo)

---

## 🎓 Thành viên thực hiện

| MSSV | Họ và Tên | Email | Github |
|---|---|---|---|
| 23520168 | Đoàn Ngọc Minh Châu | 23520168@gm.uit.edu.vn | [Minh Châu](https://github.com/23520168) |
| 23521153 | Hồ Thị Hữu Phi | 23521153@gm.uit.edu.vn | [Hữu Phi](https://github.com/HoThiHuuPhi) |

---

## 📌 Ghi chú
Đây là đồ án môn học, **không dùng cho mục đích thương mại**.
