# 02-DEPLOYMENT-GUIDE.md — Hướng Dẫn Triển Khai Hệ Thống Lên Render & Supabase

> **Mục tiêu:** Cung cấp tài liệu chi tiết, từng bước cấu hình môi trường để triển khai Backend Spring Boot lên dịch vụ **Render**, kết nối cơ sở dữ liệu **Supabase (PostgreSQL)** và kịch bản kiểm thử trực tiếp cổng thanh toán **VNPay Sandbox**.

---

## 1. KIẾN TRÚC MÔI TRƯỜNG TRIỂN KHAI

* **Backend Service:** Render Web Service (Java 17, Spring Boot 3.3.3, Dockerfile hoặc Native Java Maven).
* **Database:** Supabase PostgreSQL (Managed Cloud Database).
* **Frontend Service:** Vercel / Netlify / Render Static Site (React + Vite).
* **Múi giờ:** Ép cứng `Asia/Ho_Chi_Minh` (UTC+7) qua Spring bean `Clock`.
* **Cổng thanh toán:** VNPay Sandbox API (VNPAY QR & Thẻ ATM nội địa demo).

---

## 2. DANH SÁCH BIẾN MÔI TRƯỜNG (ENVIRONMENT VARIABLES) TRÊN RENDER

Khi tạo mới một **Web Service** trên Render Dashboard, cấu hình các biến môi trường sau:

| Tên biến (Key) | Giá trị mẫu / Mô tả | Bắt buộc |
|---|---|:---:|
| `PORT` | `8080` (hoặc để Render tự cấp phát) | Có |
| `SPRING_PROFILES_ACTIVE` | `prod` (hoặc `default`) | Có |
| `DB_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` | Có |
| `DB_USERNAME` | `postgres` (hoặc user Supabase của bạn) | Có |
| `DB_PASSWORD` | `<mat-khau-database-supabase>` | Có |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | Có |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` | Có |
| `SCMS_PAYMENT_EXPIRE_MINUTES` | `5` (Chuẩn hóa giữ chỗ thanh toán 5 phút) | Có |
| `SCMS_BOOKING_BLOCK_ENABLED` | `false` (Tạm tắt chế tài khóa 7 ngày theo AGENTS.md 2.7) | Có |
| `VNPAY_TMN_CODE` | Mã Merchant VNPay Sandbox (VD: `SCMS0001` hoặc mã từ VNPay cấp) | Có |
| `VNPAY_HASH_SECRET` | Khóa bí mật ký SHA512 VNPay Sandbox | Có |
| `VNPAY_PAY_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | Có |
| `VNPAY_RETURN_URL` | `https://<ten-backend-domain>/api/payments/vnpay/return` | Có |
| `FRONTEND_PAYMENT_RESULT_URL` | `https://<ten-fe-domain>/payment-result` | Có |
| `FRONTEND_URL` | `https://<ten-fe-domain>` (Dùng để cấp phép CORS) | Có |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` *(Cấu hình sau deploy khi làm thông báo)* | Tùy chọn |
| `SPRING_MAIL_PORT` | `587` | Tùy chọn |
| `SPRING_MAIL_USERNAME` | `<gmail-cua-trung-tam>` | Tùy chọn |
| `SPRING_MAIL_PASSWORD` | `<gmail-app-password>` | Tùy chọn |

---

## 3. CẤU HÌNH BUILD & START TRÊN RENDER

### Phương án: Dùng Maven Wrapper (Native Java)
* **Environment:** `Java`
* **Java Version:** `17`
* **Build Command:**
  ```bash
  chmod +x mvnw && ./mvnw clean package -DskipTests
  ```
* **Start Command:**
  ```bash
  java -Duser.timezone=Asia/Ho_Chi_Minh -jar target/sports-center-management-0.0.1-SNAPSHOT.jar
  ```
* **Health Check Path:**
  ```
  /api/health
  ```
  *(Endpoint này đã được cấu hình `permitAll` trong SecurityConfig, trả về mã HTTP 200 giúp Render giám sát service).*

---

## 4. KỊCH BẢN KIỂM THỬ TRỰC TIẾP VNPAY SANDBOX (LIVE TEST SCENARIO)

### 4.1. Thông Tin Thẻ Demo VNPay Sandbox Chuẩn
Khi thực hiện thanh toán trên cổng VNPay Sandbox, nhập thông tin thẻ demo sau:

* **Ngân hàng:** NCB (Ngân hàng Quốc Dân)
* **Số thẻ (Card Number):** `9704198526191432198`
* **Tên chủ thẻ (Cardholder Name):** `NGUYEN VAN A`
* **Ngày phát hành (Issue Date):** `07/15`
* **Mã OTP xác thực:** `123456`

### 4.2. Luồng Kiểm Thử Hóa Đơn Khóa Normal (Lớp Nhóm 12 Buổi)
1. Đăng nhập tài khoản Member trên Frontend (hoặc gọi qua Postman).
2. Gọi `POST /api/classes/{classId}/enroll` với `{"paymentMethod": "VNPay"}`.
3. Nhận về URL thanh toán VNPay (`paymentUrl`). Hạn giữ chỗ là **5 phút**.
4. Mở `paymentUrl` trên trình duyệt: Chọn ngân hàng NCB $\rightarrow$ nhập thông tin thẻ demo trên $\rightarrow$ nhập OTP `123456`.
5. Sau khi thanh toán thành công: VNPay chuyển hướng về `VNPAY_RETURN_URL` $\rightarrow$ Backend kiểm tra chữ ký SHA512 $\rightarrow$ Cập nhật hóa đơn `Paid` $\rightarrow$ Kích hoạt trạng thái `Registered` của Enrollment và gán học viên vào 12 buổi học tĩnh.
6. Màn hình trả về kết quả `success` tại `FRONTEND_PAYMENT_RESULT_URL`.

### 4.3. Luồng Kiểm Thử Mua Gói 1:1 Advanced (12 Credit / 45 Ngày)
1. Gọi `POST /api/member-packages/subscribe-me` với gói `Advanced` (`packageId = ...`, `paymentMethod = "VNPay"`).
2. Nhận về `paymentUrl` $\rightarrow$ Thực hiện thanh toán demo bằng thẻ NCB.
3. Callback VNPay hoàn tất:
   * Hóa đơn chuyển trạng thái `Paid`.
   * Gói hội viên chuyển `Active`.
   * Số dư nạp đúng **12 Credit**.
   * Thời hạn tính đúng **45 ngày** kể từ ngày thanh toán.
4. Kiểm tra qua `GET /api/member-packages/me`: Xác nhận `remainingCredit = 12`, `validityDays = 45`.
5. Đặt thử 1 buổi học 1:1 qua `POST /api/sessions/book-private` $\rightarrow$ Số credit giảm còn `11`.
6. Hủy buổi học đó trước $\ge 24h$ qua `POST /api/sessions/{id}/cancel-private` $\rightarrow$ Hoàn trả lại `12` credit.

### 4.4. Kiểm Thử Sự Cố Hết Hạn Giữ Chỗ (Holding Window Timeout)
1. Tạo yêu cầu đặt chỗ/mua gói nhưng không nhập thanh toán, để quá **5 phút**.
2. Cronjob `PaymentExpirationJob` quét tự động:
   * Hóa đơn `Pending` chuyển sang `Expired`.
   * Chỗ đăng ký hoặc gói `Pending` được hủy và giải phóng tài nguyên.

---

## 5. LƯU Ý KẾT NỐI SUPABASE POSTGRESQL

* **Chế độ kết nối:**
  * Khuyến nghị dùng chế độ Session Pooler (port `5432`) hoặc Transaction Pooler (port `6543`).
  * Tham số bắt buộc trong JDBC URL: `?sslmode=require`.
* **Kích thước Connection Pool (HikariCP):**
  * Trên Render gói Free/Starter, thiết lập `maximum-pool-size=5` (đã được cấu hình trong `application.properties`) để tránh cạn kiệt connection pool của Supabase.
* **Migration Database:**
  * File migration `database/migrations/04_migration_member_flow_v2.sql` phải được thực thi trong SQL Editor của Supabase trước khi start server.

---

## 6. CHECKLIST KIỂM THỬ TRƯỚC VÀ SAU DEPLOY (PRE & POST-DEPLOY CHECKLIST)

### 6.1. Trước khi Deploy (Pre-deploy)
- [x] Chạy lệnh kiểm thử toàn diện trên máy nội bộ:
  ```powershell
  .\mvnw.cmd clean test
  ```
  *Đảm bảo 100% test pass (178 tests run, 163 passed, 15 skipped, 0 fail).*
- [ ] Kiểm tra file migration `04_migration_member_flow_v2.sql` đã được chạy trên Database Supabase.
- [ ] Xác nhận không còn lệnh debug mang thông tin nhạy cảm.

### 6.2. Sau khi Deploy (Post-deploy Smoke Test)
- [ ] Chạy script kiểm thử khói tự động toàn diện:
  ```powershell
  .\scripts\smoke-test-api.ps1 -BaseUrl "https://<ten-app>.onrender.com"
  ```
  *Script sẽ tự động kiểm tra: `/api/health`, 7 ca học chuẩn & khóa ca 3, 4 ngoài trời, danh mục 9 môn và giá chuẩn, danh sách gói, lớp học, phòng tập, ngày lễ, và tạo mã TOTP 30s.*
- [ ] Chạy script kiểm thử riêng mã QR thanh toán và TOTP check-in:
  ```powershell
  .\scripts\test-payment-and-totp.ps1 -BaseUrl "https://<ten-app>.onrender.com"
  ```
- [ ] Kiểm tra log trên Render Console: Đảm bảo Spring Boot khởi động thành công, HikariCP kết nối ổn định, các cronjob (`PackageExpirationJob`, `PaymentExpirationJob`, `AttendanceFinalizationJob`) đăng ký thành công.

### 6.3. Quy Trình Double-Check Giữa Người Dùng & AI
1. Người dùng push code lên GitHub/GitLab liên kết với Render:
   ```powershell
   git status
   git add .
   git commit -m "feat: complete phase 5 backend jobs, incident handling and DTO enrichment"
   git push origin <ten-branch>
   ```
2. Render tự động build và deploy web service.
3. Khi Render báo `Live`, người dùng copy đường link Web Service (VD: `https://scms-backend.onrender.com`) gửi vào đoạn chat.
4. AI sẽ hỗ trợ phân tích log, hướng dẫn chạy smoke test hoặc xác thực toàn bộ các endpoint quan trọng để đảm bảo Backend sẵn sàng 100% cho Frontend kết nối.
