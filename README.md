# Đồ án: WeatherConnect 🌦️🆘
Đồ án môn **Phát triển ứng dụng trên thiết bị di động** (NT118.Q14) – UIT.  
**WeatherConnect** là ứng dụng theo dõi thời tiết theo vị trí, kết nối cộng đồng và hỗ trợ gửi tín hiệu **SOS** trong tình huống khẩn cấp.

<img width="1000" alt="Cover" src="https://github.com/user-attachments/assets/d400b17e-a577-4d07-87d7-97d389eb29f8" />

---

## ✨ Tính năng nổi bật

| Tính năng | Mô tả |
|---|---|
| Xem thời tiết theo vị trí | Lấy dữ liệu thời tiết từ Open-Meteo, hiển thị tổng quan + dự báo |
| Nền thời tiết động | Dynamic Weather Background thay đổi theo trạng thái thời tiết |
| AI Advisor (gợi ý) | Mở từ MainScreen để nhận gợi ý/nhắc nhở theo thời tiết (tuỳ cấu hình API) |
| SOS một chạm | Gửi SOS kèm tọa độ + nội dung lên Firebase |
| SOS Offline Queue | Mất mạng vẫn ghi nhận, có mạng sẽ tự đồng bộ gửi lại |
| Danh sách SOS | Xem danh sách các SOS và mở bản đồ chi tiết |
| Bản đồ SOS tổng quan | Hiển thị nhiều marker SOS (overview) |
| Bản đồ cứu hộ chi tiết | Hiển thị 1 SOS + vị trí của tôi + hỗ trợ mở chỉ đường |
| Chat cộng đồng | Chat realtime theo khu vực, hỗ trợ tag mức độ (Thông tin/Cảnh báo/Khẩn cấp) |
| Cài đặt + Đăng xuất | Logout Firebase + xoá session DataStore |

---

## 🧩 Các màn hình / Route (Navigation)
Start destination: **Nếu đã đăng nhập → `main`, chưa đăng nhập → `login`**.

| Route | Màn hình | Ghi chú |
|---|---|---|
| `login` | Đăng nhập | Thành công → `main` |
| `register` | Đăng ký | Xong → quay lại `login` |
| `forgot_password` | Quên mật khẩu | Gửi mail reset → quay lại |
| `main` | Trang chính | Thời tiết + nút AI + nút SOS + mở các module |
| `chat` | Chat cộng đồng | Realtime |
| `search` | Tìm kiếm | Tìm thời tiết địa điểm khác |
| `weather_map` | Bản đồ thời tiết | WebView (Ventusky) |
| `settings` | Cài đặt | Logout + tuỳ chọn |
| `rescue_map_overview` | Bản đồ SOS tổng quan | Nhiều marker + mở danh sách |
| `rescue_list` | Danh sách SOS | Chọn SOS → map chi tiết |
| `rescue_map/{lat}/{lon}/{name}` | Bản đồ cứu hộ (chi tiết) | 1 SOS + vị trí tôi + chỉ đường |

---

## 🧭 Sơ đồ luồng ứng dụng (Userflow)

<img width="1000" alt="USERFLOW" src="https://github.com/user-attachments/assets/cf02769d-3a7e-4d45-8d65-4dc92d8fc1c9" />

---

## 🛠️ Công nghệ sử dụng
- **Kotlin**, **Jetpack Compose**, **Material 3**
- **Retrofit + Gson** (Open-Meteo API)
- **Firebase**: Authentication, Firestore/Realtime, Storage, Cloud Messaging (FCM)
- **Vị trí & bản đồ**: Fused Location Provider, VietMap SDK, mở chỉ đường qua Intent
- **Lưu trữ cục bộ**: DataStore (session + settings), cơ chế Offline-first cho SOS

---

## ▶️ Cách chạy dự án

### 1) Yêu cầu môi trường
- Android Studio (khuyến nghị bản mới)
- JDK 17
- Thiết bị thật hoặc AVD (khuyến nghị AVD có Google Play để dùng Location)

### 2) Mở project
1. Clone / tải project
2. Mở bằng Android Studio
3. Sync Gradle
4. Run `app`

### 3) Firebase
Project đang dùng Firebase. Nếu bạn **fork và dùng Firebase riêng**:
- Tạo Firebase project → Add Android app
- Tải `google-services.json` và đặt vào thư mục `app/`
- Bật các dịch vụ cần thiết: Auth / Firestore / Storage / (FCM nếu dùng)

### 4) API Key bản đồ / AI (nếu có)
- VietMap: nếu project đang để key trong code, bạn có thể thay bằng key của bạn.
- AI Advisor: tuỳ theo code module AI, nếu thiếu API key thì phần gọi AI có thể không hoạt động (app vẫn chạy các chức năng khác).

---

## 🔐 Quyền truy cập (Permissions)
- `INTERNET`, `ACCESS_NETWORK_STATE`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)

---

## 🎬 Demo flow gợi ý (1–2 phút)
1. Login → Main (xem thời tiết)
2. Mở **AI Advisor**
3. Nhấn **SOS** → gửi → chọn **Mở danh sách SOS** (hoặc đóng để về Main)
4. Mở **Bản đồ SOS tổng quan** → chọn SOS → vào **map chi tiết**
5. Vào **Chat** gửi tin nhắn (tag Cảnh báo)

---

## 🎓 Thành viên thực hiện
| MSSV | Họ và Tên | Email | Github |
|---|---|---|---|
| 23520168 | Đoàn Ngọc Minh Châu | 23520168@gm.uit.edu.vn | https://github.com/23520168 |
| 23521153 | Hồ Thị Hữu Phi | 23521153@gm.uit.edu.vn | https://github.com/HoThiHuuPhi |

---

## 📌 Ghi chú
Đây là đồ án môn học, **không dùng cho mục đích thương mại**.

---

