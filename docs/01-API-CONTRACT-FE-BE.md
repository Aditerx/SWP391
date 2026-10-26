# 01-API-CONTRACT-FE-BE.md — Hợp Đồng Kết Nối API Giữa Frontend & Backend

> **Mục tiêu:** Đây là tài liệu duy nhất và chính thức quy định toàn bộ Endpoints, Headers, Request Body, Response Schema, Bảng giá chuẩn 9 bộ môn, và Từ điển Mã lỗi (Error Codes Dictionary) để đội Frontend (`FE_Project-main/`) tích hợp thông suốt với Backend SCMS (Sports Center Management System).
> **Nguyên tắc cốt lõi:** Bám sát tuyệt đối [AGENTS.md](file:///d:/Projects/SWP391/AGENTS.md) và tài liệu nghiệp vụ `Member.pdf`. Không tin tưởng dữ liệu Client, price freezing trên hóa đơn, pessimistic locking chống race condition, và toàn hệ thống đồng nhất múi giờ `Asia/Ho_Chi_Minh` (UTC+7).


> **Bản sửa 23/10/2026:** Cần migration 06 và 07 (PostgreSQL) trước khi chạy backend; xem mục 13 về F-06/F-07. Mục 12 bên dưới quy định hành vi an toàn mới; bảng giá và các chính sách còn xung đột vẫn chờ quyết định.

---

## 1. QUY CHUẨN GIAO TIẾP CHUNG & XÁC THỰC

### 1.1. Cấu Hình Môi Trường & Kết Nối
* **Base URL:**
  * Local Development: `http://localhost:8080`
  * Production (Render): `https://<ten-app>.onrender.com`
* **Múi giờ nghiệp vụ:** `Asia/Ho_Chi_Minh` (UTC+7) qua Spring bean `Clock`. Mọi định dạng ngày giờ gửi lên / nhận về đều theo chuẩn ISO 8601:
  * Ngày: `YYYY-MM-DD` (VD: `2026-10-20`)
  * Giờ: `HH:mm:ss` hoặc `HH:mm` (VD: `06:00:00` hoặc `07:30`)
* **Headers bắt buộc:**
  * `Content-Type: application/json`
  * `Cookie: JSESSIONID=...` (Hệ thống dùng Session Cookie bảo mật; Axios/Fetch trên Frontend bắt buộc bật `withCredentials: true`).

### 1.2. Nhóm API Xác Thực (Authentication Flow)
* **Đăng nhập:** `POST /api/auth/login`
  * Body: `{ "email": "member@example.com", "password": "password123" }`
  * Response (200 OK): Trả về thông tin User, Role và gán Cookie `JSESSIONID`.
* **Đăng ký hội viên:** `POST /api/auth/register`
  * Body: `{ "fullName": "Nguyễn Văn A", "email": "a@example.com", "password": "password123", "phone": "0912345678", "gender": "Male", "dateOfBirth": "2000-01-01" }`
  * Response (200 OK): Gửi mã OTP xác thực qua email.
* **Xác thực OTP đăng ký:** `POST /api/auth/verify-registration`
  * Body: `{ "email": "a@example.com", "otp": "123456" }`
* **Gửi lại mã OTP:** `POST /api/auth/resend-otp`
  * Body: `{ "email": "a@example.com" }`
* **Quên mật khẩu:** `POST /api/auth/forgot-password`
  * Body: `{ "email": "a@example.com" }`
* **Đặt lại mật khẩu:** `POST /api/auth/reset-password`
  * Body: `{ "email": "a@example.com", "otp": "123456", "newPassword": "NewPassword123" }`
* **Đăng xuất:** `POST /api/auth/logout`

### 1.3. Danh Sách Tài Khoản Demo Hệ Thống (Mật Khẩu Đồng Bộ: `Scms@2026`)
Toàn bộ tài khoản mẫu đã được khởi tạo trong cơ sở dữ liệu (`schema_postgres.sql` / `schema_user_2310.sql`) với mật khẩu chuẩn hóa: **`Scms@2026`** (BCrypt: `$2a$10$QBctKpWoGbyJxKd43TNSPemf2xsDeVUugnAhEEfkZH6pLU8ncMqMi`).

#### A. Nhóm 5 Vai Trò Đại Diện Cốt Lõi (1 Account / 1 Role)
| STT | Vai Trò (Role) | Email Đăng Nhập | Mật Khẩu | Họ Và Tên | Mục Đích Kiểm Thử / Demo |
|:---:|---|---|---|---|---|
| 1 | **CenterManager** | `manager@scms.com` | `Scms@2026` | Nguyễn Văn An | Quản lý vận hành lớp học, phân công HLV, duyệt dời lịch, xem báo cáo |
| 2 | **Receptionist** | `receptionist@scms.com` | `Scms@2026` | Phạm Thị Thu | Đăng ký hội viên tại quầy, thu học phí tiền mặt, check-in QR, hỗ trợ |
| 3 | **Coach (Đại diện)** | `coach@scms.com` | `Scms@2026` | Nguyễn Văn Hùng | Điểm danh ca học, ghi nhận kết quả tập luyện, xem lịch dạy |
| 4 | **Member (Đại diện)**| `member@scms.com` | `Scms@2026` | Nguyễn Văn Thành | Sẵn **12 Credit / 45 ngày** $\rightarrow$ Demo tra cứu ca rảnh, đặt lịch 1:1, hủy hoàn credit |
| 5 | **Admin** | `admin@scms.com` | `Scms@2026` | Admin Hệ Thống | Quản trị người dùng, ma trận phân quyền RBAC |
| 6 | **Receptionist 2** | `thu.reception@scms.com`| `Scms@2026` | Phạm Thị Thu (Ca Chiều)| Đồng bộ với tài khoản lễ tân phụ trên Frontend |

#### B. Đội Ngũ Huấn Luyện Viên 9 Bộ Môn Chuẩn (2 HLV / Môn)
| STT | Bộ Môn | Sân Bãi | HLV 1 (Khớp Frontend) | HLV 2 (Phụ / 1:1 Nâng Cao) |
|:---:|---|:---:|---|---|
| 1 | **Bóng đá (Football)** | *Outdoor* | `coach.football@scms.com`<br>*(Lê Trần Phi - AFC Pro)* | `coach.football2@scms.com`<br>*(Nguyễn Tuấn Vũ)* |
| 2 | **Quần vợt (Tennis)** | *Outdoor* | `coach.tennis@scms.com`<br>*(Huỳnh Diễm My - ITF L2)* | `coach.tennis2@scms.com`<br>*(Trần Quốc Bảo)* |
| 3 | **Bơi lội (Swimming)** | *Outdoor* | `coach.swimming@scms.com`<br>*(Lê Phương Anh - FINA L2)* | `coach.swimming2@scms.com`<br>*(Đỗ Hải Đăng)* |
| 4 | **Bắn cung (Archery)** | *Outdoor* | `coach.archery@scms.com`<br>*(Lê Nguyễn Thiên Phúc - Kiện tướng)* | `coach.archery2@scms.com`<br>*(Hoàng Minh Tâm)* |
| 5 | **Cầu lông (Badminton)** | *Indoor* | `coach.badminton@scms.com`<br>*(Trần Thị Hương - BWF L2)* | `coach.badminton2@scms.com`<br>*(Lê Minh Khang)* |
| 6 | **Bóng bàn (Table Tennis)**| *Indoor* | `coach.tabletennis@scms.com`<br>*(Lê Minh Khoa - ITTF L2)* | `coach.tabletennis2@scms.com`<br>*(Nguyễn Đức Trí)* |
| 7 | **Bóng rổ (Basketball)** | *Indoor* | `coach.basketball@scms.com`<br>*(Trần Anh Dũng - FIBA L1)* | `coach.basketball2@scms.com`<br>*(Phan Hoàng Nam)* |
| 8 | **Bóng chuyền (Volleyball)**| *Indoor* | `coach.volleyball@scms.com`<br>*(Nguyễn Văn Hoàng - FIVB L1)* | `coach.volleyball2@scms.com`<br>*(Đặng Tuấn Anh)* |
| 9 | **Pickleball** | *Indoor* | `coach.pickleball@scms.com`<br>*(Phan Thị Lan - IPTPA L2)* | `coach.pickleball2@scms.com`<br>*(Vũ Thị Thảo)* |

#### C. Hội Viên Theo Kịch Bản Nghiệp Vụ
| STT | Email Đăng Nhập | Mật Khẩu | Họ Và Tên | Kịch Bản Nghiệp Vụ Sẵn Có |
|:---:|---|---|---|---|
| 1 | `member@scms.com` | `Scms@2026` | Nguyễn Văn Thành | Đã kích hoạt Gói 1:1 Advanced, còn **12 Credit**, hạn 45 ngày |
| 2 | `member.normal@scms.com` | `Scms@2026` | Hoàng Thị Oanh | Đã ghi danh Lớp Cầu lông 12 buổi cố định $\rightarrow$ Test chặn nút hủy |
| 3 | `member.new@scms.com` | `Scms@2026` | Bùi Văn Phúc | Hội viên mới, có hóa đơn giữ chỗ 5 phút $\rightarrow$ Test thanh toán |
| 4 | `member.expired@scms.com`| `Scms@2026` | Đặng Văn Sơn | Gói 1:1 đã quá hạn 45 ngày $\rightarrow$ Test thông báo & gia hạn gói |
| 5 | `member.active2@scms.com`| `Scms@2026` | Ngô Thị Quyên | Đã ghi danh lớp Pickleball $\rightarrow$ Test sĩ số và lịch tập |

---

## 2. BẢNG GIÁ CHUẨN 9 BỘ MÔN THỂ THAO & HAI DÒNG SẢN PHẨM (ĐỐI CHIẾU MEMBER.PDF)

### 2.1. Ma Trận Giá 9 Môn Thể Thao
**Phạm vi danh mục đã chốt:** chỉ 9 môn trong Member.pdf; giữ ID 19–27 theo snapshot DB đã kiểm. Owner chọn xóa hẳn 8 bản trùng, không giữ Inactive; môn ngoài danh mục như Bắn súng cũng phải loại khỏi danh mục. Đây là kế hoạch chưa thực thi trên DB; không dùng ID snapshot cho DB khác nếu chưa kiểm mapping.

**Đã chốt ngày 25/10/2026:** Mỗi môn có một giá Normal và một giá Advanced riêng; Owner chọn đủ 18 mức giá PDF/migration 04 dưới đây. Đây là giá niêm yết mục tiêu cho giao dịch mới, **chưa phải xác nhận DB/deploy đã cập nhật**. Bảng demo cũ và kiểm kê ID/liên kết nằm tại [04-DEBUG-NOTE.md, mục 9–10](04-DEBUG-NOTE.md). Không chạy seed ghi đè DB hoặc thay amount hóa đơn cũ.

| STT | Môn Thể Thao | Loại Sân / Phòng | Ca Học Cho Phép | Học Phí Khóa Normal (12 buổi nhóm cố định) | Giá Gói Khóa Advanced (1:1 linh hoạt 12 Credit) |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **1** | **Bóng đá (Soccer)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **1.000.000 VNĐ** | **3.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **2** | **Quần vợt (Tennis)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **2.500.000 VNĐ** | **8.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **3** | **Bơi lội (Swimming)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **1.500.000 VNĐ** | **4.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **4** | **Bắn cung (Archery)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **2.000.000 VNĐ** | **6.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **5** | **Cầu lông (Badminton)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **900.000 VNĐ** | **3.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **6** | **Bóng bàn (Table Tennis)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **800.000 VNĐ** | **2.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **7** | **Bóng rổ (Basketball)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **1.200.000 VNĐ** | **3.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **8** | **Bóng chuyền (Volleyball)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **1.000.000 VNĐ** | **3.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **9** | **Pickleball** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **1.800.000 VNĐ** | **5.000.000 VNĐ** *(12 Credit / 45 ngày)* |


**Lưu ý mẫu response:** Các số tiền/ID gói và hóa đơn minh họa ở các mục API khác chưa được rà lại theo cấu hình gói theo môn; không dùng chúng thay bảng niêm yết này. Hóa đơn/lớp lịch sử có thể giữ snapshot khác hợp lệ. Cách áp giá cho lớp đang mở và gói còn chờ D02/D03.

### 2.2. So Sánh Đặc Tính Kỹ Thuật Hai Dòng Sản Phẩm
> Bảng này mô tả luồng cũ/chưa đồng bộ thay đổi 25/10. Thiết kế mục tiêu đã chốt: Coach chính tạo ca, hạn 1080 giờ từ Paid hợp lệ, hủy Advanced >=12 giờ; xem mục 14. Không coi các rule mục tiêu là code đã deploy, không lấy ngưỡng 24 giờ dưới đây để ghi đè quyết định mới.

| Đặc Tính | Khóa Normal (Học Nhóm Cố Định) | Khóa Advanced (Học 1:1 Kèm Riêng) |
| :--- | :--- | :--- |
| **Sĩ số lớp** | Tối đa 15 học viên (`maxCapacity <= 15`) | Đúng 1 học viên - 1 HLV (`maxCapacity = 1`) |
| **Cơ chế lịch học** | Sinh cố định 12 buổi vào DB theo thứ trong tuần (VD: T2-4-6) | Tự do book từng ca học theo giờ rảnh của HLV |
| **Đơn vị thanh toán** | Thu học phí trọn gói theo môn (1 hóa đơn) | Nạp 12 Credit vào tài khoản gói 1:1 |
| **Thời hạn sử dụng** | Theo chu kỳ ngày bắt đầu $\rightarrow$ ngày kết thúc của lớp | **45 ngày đếm ngược** từ thời điểm thanh toán thành công |
| **Chính sách Hủy lịch** | **CẤM HỦY / BÁO VẮNG** (`NORMAL_CLASS_NON_CANCELLABLE`). Nút hủy bị vô hiệu hóa trên FE. | - Hủy trước $\ge 24h$: Hoàn trả 1 Credit vào gói.<br>- Hủy dưới 24h: Bị từ chối (`CANCEL_TOO_LATE`). |
| **Xử lý Vắng mặt (No-show)**| Điểm danh vắng $\rightarrow$ Mất buổi học đó, không hoàn tiền, không bảo lưu, không học bù. | HLV điểm danh vắng $\rightarrow$ Tiêu thụ 1 Credit (bảo vệ thù lao HLV), không hoàn lại credit. |
| **Quản lý Hết hạn** | Kết thúc khi buổi học thứ 12 diễn ra xong | Cronjob 00:00 hàng đêm quét: Gói quá 45 ngày chuyển `Expired`, vô hiệu hóa credit thừa. |

---

## 3. NHÓM API KHUNG GIỜ CA HỌC CHUẨN (7 SLOTS & CHỐNG SỐC NHIỆT)

### 3.1. Lấy danh sách 7 Ca học chuẩn
* **Endpoint:** `GET /api/sessions/time-slots`
* **Quyền:** Tất cả người dùng đã đăng nhập (Member, Coach, Receptionist, Manager, Admin).
* **Query Params:**
  * `isOutdoor` (boolean, optional): Nếu truyền `true`, chỉ trả về 5 ca hợp lệ (tự động loại trừ Ca 3 và Ca 4).
* **Response (200 OK):**
```json
[
  {
    "slotNumber": 1,
    "name": "Ca 1",
    "startTime": "06:00:00",
    "endTime": "07:30:00",
    "outdoorAllowed": true,
    "displayName": "Ca 1 (06:00 - 07:30)"
  },
  {
    "slotNumber": 2,
    "name": "Ca 2",
    "startTime": "07:45:00",
    "endTime": "09:15:00",
    "outdoorAllowed": true,
    "displayName": "Ca 2 (07:45 - 09:15)"
  },
  {
    "slotNumber": 3,
    "name": "Ca 3",
    "startTime": "09:30:00",
    "endTime": "11:00:00",
    "outdoorAllowed": false,
    "displayName": "Ca 3 (09:30 - 11:00)"
  },
  {
    "slotNumber": 4,
    "name": "Ca 4",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "outdoorAllowed": false,
    "displayName": "Ca 4 (13:00 - 14:30)"
  },
  {
    "slotNumber": 5,
    "name": "Ca 5",
    "startTime": "14:45:00",
    "endTime": "16:15:00",
    "outdoorAllowed": true,
    "displayName": "Ca 5 (14:45 - 16:15)"
  },
  {
    "slotNumber": 6,
    "name": "Ca 6",
    "startTime": "16:30:00",
    "endTime": "18:00:00",
    "outdoorAllowed": true,
    "displayName": "Ca 6 (16:30 - 18:00)"
  },
  {
    "slotNumber": 7,
    "name": "Ca 7",
    "startTime": "18:15:00",
    "endTime": "19:45:00",
    "outdoorAllowed": true,
    "displayName": "Ca 7 (18:15 - 19:45)"
  }
]
```

---

## 4. NHÓM API DANH MỤC & TRA CỨU CHUNG (CATALOG & LOOKUP APIS)

### 4.1. Danh Sách Môn Thể Thao (Subjects)
* **Endpoint:** `GET /api/subjects`
* **Quyền:** Authenticated (SecurityConfig yêu cầu đăng nhập cho `/api/**`; anonymous nhận 401). Không yêu cầu riêng MANAGE_USERS để xem danh sách.
* **Mô tả:** Trả về danh sách 9 bộ môn kèm giá chuẩn Normal, giá gói Advanced, slug và cờ `outdoor`.
* **Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Bóng đá",
    "slug": "bong-da",
    "description": "Bóng đá sân cỏ nhân tạo tiêu chuẩn ngoài trời",
    "normalPrice": 900000.00,
    "advancedPrice": 8000000.00,
    "outdoor": true,
    "status": "Active"
  },
  {
    "id": 8,
    "name": "Cầu lông",
    "slug": "cau-long",
    "description": "Cầu lông sàn thảm tiêu chuẩn thi đấu trong nhà",
    "normalPrice": 900000.00,
    "advancedPrice": 7500000.00,
    "outdoor": false,
    "status": "Active"
  }
]
```
* **Chi tiết một môn:** `GET /api/subjects/{id}`

### 4.2. Danh Sách Gói Hội Viên & Gói 1:1 (Membership Packages)
* **Endpoint:** `GET /api/packages`
* **Quyền:** Public / Authenticated
* **Mô tả:** Trả về toàn bộ gói hội viên thường (`Normal`) và gói huấn luyện viên 1:1 (`Advanced`), kèm thời lượng ngày, giá và số lượng credit.
* **Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Gói Hội Viên Tháng Tiêu Chuẩn",
    "durationDays": 30,
    "price": 500000.00,
    "benefits": "Sử dụng tự do các trang thiết bị phòng tập gym và hồ bơi",
    "status": "ACTIVE",
    "packageType": "Normal",
    "totalCredit": 12
  },
  {
    "id": 10,
    "name": "Gói 1:1 Tennis Nâng Cao",
    "durationDays": 45,
    "price": 10000000.00,
    "benefits": "12 buổi học kèm 1:1 cùng HLV Tennis chuyên nghiệp, thời hạn 45 ngày",
    "status": "ACTIVE",
    "packageType": "Advanced",
    "totalCredit": 12
  }
]
```

### 4.3. Danh Sách Lớp Học (Classes)
* **Endpoint:** `GET /api/classes`
* **Quyền:** Public / Authenticated
* **Mô tả:** Trả về danh sách lớp học có sẵn `subjectName`, `coachName`, `enrolledCount`, `maxCapacity`, `tuitionFee`, `classType`.
* **Response (200 OK):**
```json
[
  {
    "id": 105,
    "name": "Cầu lông Cơ bản K01",
    "subjectId": 8,
    "subjectName": "Cầu lông",
    "coachId": 3,
    "coachName": "Trần Thị Hương",
    "maxCapacity": 15,
    "tuitionFee": 900000.00,
    "startDate": "2026-11-02",
    "endDate": "2026-12-10",
    "status": "Open",
    "enrolledCount": 8,
    "classType": "Normal",
    "totalSessions": 12
  }
]
```
* **Chi tiết lớp học:** `GET /api/classes/{id}`

### 4.4. Danh Sách Phòng & Sân Tập (Rooms / Courts)
* **Endpoint:** `GET /api/rooms`
* **Quyền:** Public / Authenticated
* **Mô tả:** Trả về danh sách phòng/sân tập kèm phân loại `facilityType` (`Indoor` / `Outdoor`) và cờ `outdoor`.
* **Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Sân Tennis 1",
    "location": "Khu thể thao ngoài trời",
    "capacity": 4,
    "status": "Available",
    "facilityType": "Outdoor",
    "outdoor": true
  },
  {
    "id": 5,
    "name": "Nhà thi đấu Cầu lông (Sàn thảm)",
    "location": "Nhà đa năng Tầng 2",
    "capacity": 20,
    "status": "Available",
    "facilityType": "Indoor",
    "outdoor": false
  }
]
```

### 4.5. Danh Sách Huấn Luyện Viên (Coaches Directory)
* **Endpoint:** `GET /api/coaches`
* **Quyền:** Public / Authenticated
* **Query Params:**
  * `specializationId` (Integer, optional): Lọc theo chuyên môn môn học.
  * `keyword` (String, optional): Tìm kiếm theo tên HLV hoặc tên bộ môn.
* **Response (200 OK):**
```json
[
  {
    "id": 3,
    "name": "Trần Thị Hương",
    "avatarUrl": null,
    "status": "Active",
    "specializations": ["Cầu lông", "Pickleball"],
    "bio": "HLV cấp quốc gia với hơn 8 năm kinh nghiệm huấn luyện chuyên sâu",
    "certification": "Bằng HLV Quốc Tế BWF Cấp 2",
    "phone": null,
    "email": null
  }
]
```
*(Lưu ý: Trường `phone` và `email` chỉ được trả về khi người đăng nhập có quyền `MANAGE_USERS` nhằm bảo mật thông tin cá nhân).*

Theo source hiện tại: danh sách giới hạn theo center của tài khoản; Admin không gán center có phạm vi toàn hệ thống, tài khoản thường thiếu center bị từ chối. `keyword` được trim, tìm không phân biệt hoa thường theo tên Coach hoặc chuyên môn; `specializationId` là ID Specialization, không phải Subject. Coach không chuyên môn có `specializations: []`; bộ lọc không khớp trả `[]`. Route chưa tự lọc Coach/chuyên môn chỉ Active. Ví dụ trên minh họa DTO, không phải response probe mới. D09 đang chuẩn bị sửa lazy loading, chưa bảo đảm route live hết 500; xem 14.3.

---

## 5. NHÓM API QUẢN LÝ LỚP HỌC NHÓM (KHÓA NORMAL - 12 BUỔI TĨNH)

### 5.1. Tạo Lớp học Normal (Center Manager)
* **Endpoint:** `POST /api/classes`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')` (Center Manager / Admin)
* **Đặc tả nghiệp vụ:** Backend tự động lấy `normal_price` từ `Subject` gán vào học phí `tuitionFee`. Sĩ số `maxCapacity` bắt buộc $\le 15$.
* **Request Body:**
```json
{
  "name": "Cầu lông Cơ bản K01",
  "subjectId": 8,
  "coachId": 3,
  "maxCapacity": 15,
  "startDate": "2026-11-02",
  "endDate": "2026-12-10",
  "status": "Open",
  "classType": "Normal",
  "totalSessions": 12
}
```
* **Response (201 Created):**
```json
{
  "id": 105,
  "name": "Cầu lông Cơ bản K01",
  "subjectId": 8,
  "subjectName": "Cầu lông",
  "coachId": 3,
  "coachName": "Trần Thị Hương",
  "tuitionFee": 900000.00,
  "classType": "Normal",
  "totalSessions": 12,
  "maxCapacity": 15,
  "enrolledCount": 0,
  "status": "Open"
}
```

### 5.2. Preview Lịch 12 Buổi Học (Holiday Skipper)
* **Endpoint:** `POST /api/sessions/preview-recurring`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')`
* **Chức năng:** Manager chọn thứ trong tuần (VD: T2-4-6: `[1, 3, 5]`) và ngày khai giảng $\rightarrow$ Backend tự động tính toán, nhảy qua các ngày nghỉ lễ có `is_active = true`, trả về danh sách đúng 12 buổi học để FE hiển thị bảng kiểm tra trước khi lưu.
* **Request Body:**
```json
{
  "classId": 105,
  "roomId": 5,
  "startDate": "2026-11-02",
  "daysOfWeek": [1, 3, 5],
  "startTime": "06:00:00",
  "endTime": "07:30:00"
}
```
* **Response (200 OK):**
```json
{
  "totalSessions": 12,
  "classId": 105,
  "className": "Cầu lông Cơ bản K01",
  "roomId": 5,
  "roomName": "Nhà thi đấu Cầu lông (Sàn thảm)",
  "hasConflict": false,
  "sessions": [
    {
      "sessionNumber": 1,
      "date": "2026-11-02",
      "dayOfWeek": "MONDAY",
      "startTime": "06:00:00",
      "endTime": "07:30:00",
      "hasConflict": false,
      "conflictDetails": []
    },
    {
      "sessionNumber": 2,
      "date": "2026-11-04",
      "dayOfWeek": "WEDNESDAY",
      "startTime": "06:00:00",
      "endTime": "07:30:00",
      "hasConflict": false,
      "conflictDetails": []
    }
  ],
  "skippedHolidays": [
    {
      "date": "2026-11-20",
      "holidayName": "Ngày Nhà giáo Việt Nam",
      "tag": "CENTER_EVENT"
    }
  ]
}
```

### 5.3. Xác Nhận Lưu 12 Buổi Học Vào Database
* **Endpoint:** `POST /api/sessions/generate`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')`
* **Chức năng:** Lưu 12 buổi học tĩnh vào bảng `sessions` và tự động cập nhật ngày kết thúc `SportsClass.endDate` theo ngày của buổi thứ 12.
* **Request Body:**
```json
{
  "classId": 105,
  "roomId": 5,
  "startDate": "2026-11-02",
  "endDate": "2026-12-10",
  "daysOfWeek": [1, 3, 5],
  "startTime": "06:00:00",
  "endTime": "07:30:00"
}
```

### 5.4. Đăng Ký Khóa Normal (Member / Receptionist)
* **Endpoint:** `POST /api/classes/{classId}/enroll`
* **Quyền:** `hasRole('MEMBER')` hoặc `hasAuthority('MANAGE_CLASSES')`
* **Request Body:**
```json
{
  "paymentMethod": "VNPay"
}
```
* **Response (201 Created):**
```json
{
  "id": 210,
  "status": "Pending",
  "invoiceId": 501,
  "invoiceCode": "INV-2026-000501",
  "amount": 900000.00,
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expiresAt": "2026-10-20T16:05:00"
}
```
*(Cửa sổ giữ chỗ thanh toán chuẩn hóa là **5 phút**. Khi hóa đơn được thanh toán thành công, học viên chuyển sang `Registered` và được gán vào 12 buổi học).*

### 5.5. Chặn Hủy Khóa Normal (`NORMAL_CLASS_NON_CANCELLABLE`)
* **Endpoint:** `PATCH /api/enrollments/{enrollmentId}/cancel`
* **Hành vi:** Với lớp Normal, Backend **từ chối 400 Bad Request**:
```json
{
  "code": "NORMAL_CLASS_NON_CANCELLABLE",
  "message": "Lớp học nhóm cố định 12 buổi không hỗ trợ hủy hoặc hoàn tiền theo quy chế trung tâm."
}
```

---

## 6. NHÓM API KHÓA ADVANCED (HỌC 1:1 LINH HOẠT BẰNG 12 CREDIT)

### 6.1. Mua Gói 1:1 & Kiểm Tra Số Dư Credit
> Payload/công thức ngày bên dưới là implementation cũ. D03 đã chốt đăng ký theo Subject/Primary Coach và deadline timestamp 1080 giờ, chưa triển khai. Bổ sung DTO/quyền/luồng duyệt thật tại mục này khi BE code xong; xem mục 14.
* **Mua Gói:** `POST /api/members/me/subscriptions` *(hoặc alias `POST /api/member-packages/subscribe-me`)*
* **Quyền:** `hasRole('MEMBER')`
* **Request Body:**
```json
{
  "packageId": 10,
  "paymentMethod": "VNPay"
}
```
* **Response (201 Created):**
```json
{
  "subscriptionId": 101,
  "invoiceId": 55,
  "invoiceCode": "INV-2026-000055",
  "amount": 10000000.00,
  "paymentMethod": "VNPay",
  "status": "Pending",
  "expiresAt": "2026-10-20T16:05:00",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
}
```
* **Kích hoạt sau thanh toán:** Khi hóa đơn được xác nhận `Paid` (VNPay callback hoặc Cash tại quầy) $\rightarrow$ Backend tự động nạp `remainingCredit = 12`, `validityDays = 45`, `startDate = today`, `endDate = today + 45 ngày`.
* **Xem thông tin Gói & Số dư Credit:** `GET /api/member-packages/me` (hoặc `GET /api/member-packages?memberId=me`)
* **Response (200 OK):**
```json
[
  {
    "subscriptionId": 101,
    "memberId": 20,
    "memberName": "Nguyễn Văn B",
    "packageId": 10,
    "packageName": "Gói 1:1 Tennis Nâng Cao",
    "price": 10000000.00,
    "startDate": "2026-10-20",
    "endDate": "2026-12-04",
    "status": "Active",
    "packageType": "Advanced",
    "remainingCredit": 12,
    "validityDays": 45
  }
]
```

### 6.2. Tra Cứu Ca Rảnh Của HLV (Coach Available Slots)
* **Endpoint:** `GET /api/coaches/{coachId}/available-slots`
  *(Hoặc `GET /api/sessions/coaches/{coachId}/available-slots`)*
* **Quyền:** `hasRole('MEMBER')`, `hasAuthority('MANAGE_CLASSES')`
* **Query Params:**
  * `date`: `YYYY-MM-DD` (Bắt buộc, ví dụ: `2026-10-25`)
  * `subjectId`: `Integer` (Tùy chọn; nếu là môn ngoài trời như Tennis, Bơi, Bắn cung, Bóng đá $\rightarrow$ tự động khóa Ca 3 và Ca 4)
  * `roomId`: `Integer` (Tùy chọn; kiểm tra trùng phòng/sân tập cụ thể)
* **Response (200 OK):**
```json
[
  {
    "slotNumber": 1,
    "slotName": "Ca 1",
    "startTime": "06:00:00",
    "endTime": "07:30:00",
    "isAvailable": true,
    "reason": null
  },
  {
    "slotNumber": 2,
    "slotName": "Ca 2",
    "startTime": "07:45:00",
    "endTime": "09:15:00",
    "isAvailable": false,
    "reason": "HLV đã có lịch dạy vào khung giờ này"
  },
  {
    "slotNumber": 3,
    "slotName": "Ca 3",
    "startTime": "09:30:00",
    "endTime": "11:00:00",
    "isAvailable": false,
    "reason": "Ca học bị khóa cho môn thể thao ngoài trời (chính sách chống sốc nhiệt 09:30:00 - 11:00:00)"
  },
  {
    "slotNumber": 4,
    "slotName": "Ca 4",
    "startTime": "13:00:00",
    "endTime": "14:30:00",
    "isAvailable": false,
    "reason": "Ca học bị khóa cho môn thể thao ngoài trời (chính sách chống sốc nhiệt 13:00:00 - 14:30:00)"
  },
  {
    "slotNumber": 5,
    "slotName": "Ca 5",
    "startTime": "14:45:00",
    "endTime": "16:15:00",
    "isAvailable": true,
    "reason": null
  }
]
```

### 6.3. Member Đặt Lịch Ca Học 1:1 (Flexible Booking)
> Mô tả implementation hiện tại. D03-c đã chốt 1080 giờ từ Paid hợp lệ; D04 đã chốt Coach chính tạo/Manager hỗ trợ, Member không tự tạo trong thiết kế mới. Chưa triển khai thay implementation bên dưới; theo dõi mục 14, không coi quyền Coach tạo hoặc timestamp mới đã khả dụng.

* **Endpoint:** `POST /api/sessions/book-private`
* **Quyền:** `hasRole('MEMBER') or hasAuthority('REGISTER_MEMBER') or hasAuthority('MANAGE_CLASSES')`
* **Ràng buộc kiểm tra nghiêm ngặt (Backend Validation):**
  1. Gói học 1:1 phải ở trạng thái `Active`, còn hạn 45 ngày (`today <= endDate`) và còn ít nhất 1 Credit (`remainingCredit >= 1`).
  2. Bắt buộc đặt trước tối thiểu **12 tiếng** (`sessionStart >= now + 12h`).
  3. Khung giờ phải thuộc 7 ca học chuẩn; không được rơi vào Ca 3 và Ca 4 nếu môn ngoài trời.
  4. Không được đặt lịch vào ngày nghỉ lễ đang có hiệu lực của trung tâm.
  5. Pessimistic lock `findByIdForUpdate` trên `member_packages` để trừ 1 Credit và chống tranh chấp đồng thời.
* **Request Body:**
```json
{
  "subscriptionId": 101,
  "coachId": 3,
  "roomId": 1,
  "sessionDate": "2026-10-25",
  "startTime": "07:45:00",
  "endTime": "09:15:00",
  "classId": 200,
  "subjectId": 1
}
```
* **Response (201 Created):**
```json
{
  "id": 999,
  "classId": 200,
  "className": "Quần vợt (Tennis) (1:1 Nâng Cao)",
  "subjectId": 1,
  "subjectName": "Quần vợt (Tennis)",
  "coachId": 3,
  "coachName": "Trần Thị Hương",
  "roomId": 1,
  "roomName": "Sân Tennis ngoài trời 1",
  "roomLocation": "Khu thể thao ngoài trời",
  "sessionDate": "2026-10-25",
  "startTime": "07:45:00",
  "endTime": "09:15:00",
  "status": "Scheduled",
  "enrolledCount": 1,
  "maxCapacity": 1,
  "bookedMemberId": 20,
  "bookedMemberName": "Nguyễn Văn B",
  "isPrivate": true
}
```

### 6.4. Hủy Lịch Học 1:1
> Implementation hiện tại còn ngưỡng 24 giờ. Owner đã chốt D05-a đổi hủy thông thường Advanced sang >=12 giờ, chưa triển khai; theo dõi mục 14 và 04-DEBUG-NOTE.md 13.3. Không dùng quyết định này để coi API đang chạy đã hỗ trợ 12 giờ.

* **Endpoint:** `POST /api/sessions/{sessionId}/cancel-private`
* **Quyền:** `hasRole('MEMBER') or hasAuthority('REGISTER_MEMBER') or hasAuthority('MANAGE_CLASSES')` (Member chỉ được hủy buổi của chính mình).
* **Quy tắc thời gian (24h Cutoff):**
  * **Hủy trước $\ge 24h$:** Hợp lệ $\rightarrow$ Buổi học chuyển sang `Cancelled`, hoàn trả 1 Credit vào gói học 1:1 của Member. Trả về `200 OK`.
  * **Hủy dưới 24h:** Backend từ chối `400 Bad Request` kèm mã lỗi:
    `CANCEL_TOO_LATE: Không thể hủy lịch khi còn dưới 24 tiếng. Buổi học sẽ bị tiêu thụ nếu không tham gia.`
    *(Credit không được hoàn, bảo vệ thù lao cho HLV).*
* **Response khi hủy thành công (200 OK):**
```json
{
  "id": 999,
  "classId": 200,
  "className": "Quần vợt (Tennis) (1:1 Nâng Cao)",
  "status": "Cancelled",
  "isPrivate": true,
  "bookedMemberId": 20
}
```

### 6.5. Điểm Danh Vắng Mặt (No-show) & Tự Động Hết Hạn 45 Ngày
* **Điểm danh Vắng mặt (No-show):** Nếu học viên không đến lớp và không hủy trước $\ge 24h$, HLV/Hệ thống điểm danh `Absent` $\rightarrow$ Tiêu thụ 1 Credit (credit đã trừ khi book không được hoàn), không sinh lịch bù.
* **Cronjob tự động 00:00 hàng đêm (`PackageExpirationJob`):** Quét các gói `Active` có `endDate < today` $\rightarrow$ Tự động chuyển `status = 'Expired'` và hủy toàn bộ số credit thừa còn lại (`remainingCredit = 0`).

---

## 7. NHÓM API XỬ LÝ SỰ CỐ TRUNG TÂM (MƯA BÃO, HLV NGHỈ, GIA HẠN GÓI)
> Các route bên dưới đã có trong source nhưng chưa đầy đủ D06–D08 đã chốt: xác nhận Member, quyền Manager, phân loại nguyên nhân/notice, bù gián đoạn và phục hồi credit. Không dùng tự kéo endDate trong source làm công thức bù mới; xem mục 14 và docs 04 mục 15–19.

### 7.1. Dời Lịch Buổi Học Do Sự Cố (Reschedule Session)
* **Endpoint:** `POST /api/sessions/{sessionId}/reschedule`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')` (Center Manager / Admin)
* **Mục đích:** Xử lý các tình huống bất khả kháng (sân ngập mưa bão, hỏng thiết bị, bảo trì đột xuất, v.v.).
* **Hành vi nghiệp vụ:**
  * Kiểm tra ca học mới phải thuộc 7 ca chuẩn và tuân thủ chính sách chống sốc nhiệt ngoài trời.
  * Kiểm tra không trùng phòng và không trùng HLV trong ca mới.
  * Nếu là buổi học của lớp Normal: tự động cập nhật ngày kết thúc `SportsClass.endDate` nếu dời vượt qua ngày cũ.
  * Nếu là buổi học 1:1:
    * Kiểm tra không trùng lịch của học viên.
    * **Tự động gia hạn hạn 45 ngày:** Nếu ngày dời mới vượt quá ngày hết hạn của gói hội viên, hệ thống tự động kéo dài `endDate` và tăng `validityDays` tương ứng, chuyển lại `Active` nếu gói bị `Expired` do quá ngày.
    * Có thể kèm thêm `extendValidityDays` để Manager chủ động đền bù thêm ngày sử dụng cho hội viên.
* **Request Body:**
```json
{
  "newDate": "2026-11-05",
  "newStartTime": "07:45:00",
  "newEndTime": "09:15:00",
  "newRoomId": 2,
  "reason": "Mưa bão lớn ngập sân ngoài trời",
  "extendValidityDays": 7
}
```
* **Response (200 OK):** Đối tượng `SessionResponse` với thông tin ngày, giờ, phòng mới và trạng thái `Scheduled`.

### 7.2. Chỉ Định HLV Thay Thế Do Sự Cố (Substitute Coach)
* **Endpoint:** `POST /api/sessions/{sessionId}/substitute-coach`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')` (Center Manager / Admin)
* **Mục đích:** Xử lý sự cố HLV phụ trách đột xuất bị ốm, tai nạn, việc gia đình khẩn cấp.
* **Hành vi nghiệp vụ:**
  * Buổi học chưa diễn ra (`Scheduled`).
  * Người thay thế bắt buộc phải có vai trò `Coach`.
  * HLV thay thế không được có lịch dạy khác trùng vào khung giờ của buổi học.
  * Hệ thống ghi nhận nhật ký kiểm toán (Audit Log) chi tiết HLV cũ $\rightarrow$ HLV mới và lý do thay thế.
* **Request Body:**
```json
{
  "newCoachId": 11,
  "reason": "HLV Nguyễn Văn A nghỉ ốm đột xuất, bàn giao HLV Trần Văn B đứng lớp"
}
```
* **Response (200 OK):** Đối tượng `SessionResponse` cập nhật HLV mới.

### 7.3. Gia Hạn Thời Hạn Gói Học Viên (Extend Package Validity)
* **Endpoint:** `POST /api/member-packages/{subscriptionId}/extend`
  *(Hoặc `POST /api/subscriptions/{subscriptionId}/extend`)*
* **Quyền:** `hasAuthority('MANAGE_SUBSCRIPTIONS')` (Center Manager / Admin)
* **Mục đích:** Chủ động bù thêm ngày sử dụng cho học viên bị ảnh hưởng bởi sự cố trung tâm hoặc bảo trì dài ngày.
* **Hành vi nghiệp vụ:**
  * Cộng thêm `extendDays` vào `endDate` và `validityDays`.
  * Nếu gói đang `Expired` mà ngày hết hạn mới lớn hơn ngày hiện tại, tự động phục hồi về `Active`.
* **Request Body:**
```json
{
  "extendDays": 14,
  "reason": "Bù thời gian hội viên do trung tâm đóng cửa bảo dưỡng mặt sàn thảm"
}
```
* **Response (200 OK):**
```json
{
  "subscriptionId": 101,
  "memberId": 20,
  "memberName": "Nguyễn Văn B",
  "packageId": 10,
  "packageName": "Gói 1:1 Tennis Nâng Cao",
  "price": 10000000.00,
  "startDate": "2026-10-20",
  "endDate": "2026-12-18",
  "status": "Active",
  "packageType": "Advanced",
  "remainingCredit": 8,
  "validityDays": 59
}
```

---

## 8. NHÓM API QUẢN LÝ NGÀY NGHỈ LỄ (HOLIDAYS)

### 8.1. Xem Danh Sách Ngày Lễ
* **Endpoint:** `GET /api/holidays`
* **Response (200 OK):**
```json
[
  {
    "id": 1,
    "holidayDate": "2026-04-30",
    "holidayName": "Giải phóng miền Nam",
    "tag": "NATIONAL_FIXED",
    "active": true,
    "description": "Kỷ niệm ngày thống nhất đất nước"
  },
  {
    "id": 2,
    "holidayDate": "2026-05-01",
    "holidayName": "Quốc tế Lao động",
    "tag": "NATIONAL_FIXED",
    "active": true,
    "description": "Ngày Quốc tế Lao động"
  }
]
```

### 8.2. Bật / Tắt Trạng Thái Ngày Nghỉ
* **Endpoint:** `PATCH /api/holidays/{id}/toggle`
* **Quyền:** `hasAuthority('MANAGE_CLASSES')` (Center Manager)
* **Request Body:** `{ "active": false }`
* **Response (200 OK):** Trả về đối tượng Holiday sau khi cập nhật.

---

## 9. NHÓM API THANH TOÁN (VNPAY SANDBOX & TIỀN MẶT TẠI QUẦY)

### 9.1. Xác nhận Thu Tiền Mặt Tại Quầy (Receptionist)
* **Endpoint:** `POST /api/invoices/{invoiceId}/confirm-cash`
* **Quyền:** `hasAuthority('MANAGE_INVOICES')`
* **Xử lý:** Xác nhận thanh toán hóa đơn `Paid` $\rightarrow$ Kích hoạt ngay suất học hoặc gói 1:1.

### 9.2. Lấy URL Thanh Toán Trực Tuyến VNPay Sandbox
* **Endpoint:** `POST /api/invoices/{invoiceId}/pay-online`
* **Response (200 OK):**
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."
}
```
* **Cửa sổ giữ chỗ:** **5 phút** (`SCMS_PAYMENT_EXPIRE_MINUTES = 5`). Hết 5 phút mà chưa thanh toán, hệ thống tự động hủy giữ chỗ.

---

## 10. ĐIỂM DANH & QUÉT MÃ QR TOTP

### 10.1. Lấy Mã QR TOTP Check-in (Member)
* **Endpoint:** `GET /api/members/me/checkin-code` *(hoặc alias `GET /api/attendance/checkin-code`)*
* **Quyền:** `hasRole('MEMBER')`
* **Response (200 OK):**
```json
{
  "payload": "974219",
  "expiresInSeconds": 30
}
```
*(Trường `payload` là chuỗi mã 6 chữ số đổi liên tục mỗi 30 giây để chống gian lận điểm danh; trường `expiresInSeconds` là thời gian đếm ngược còn lại của mã).*

### 10.2. Quét Mã Check-in (Staff / Coach)
* **Endpoint:** `POST /api/attendance/scan`
* **Quyền:** `hasRole('COACH')` hoặc `hasRole('RECEPTIONIST')`
* **Phạm vi giờ hợp lệ:** Trong vòng 30 phút trước hoặc trong giờ buổi học.

---

## 11. BẢNG TRA CỨU MÃ LỖI CHUẨN TOÀN HỆ THỐNG (ERROR CODES DICTIONARY)

BusinessException có tiền tố mã chuẩn trả cấu trúc sau; FE cần fallback theo message/HTTP status cho lỗi legacy, validation hoặc 401/403 chưa có code:
```json
{
  "code": "MÃ_LỖI_CHUẨN",
  "message": "Mô tả chi tiết bằng Tiếng Việt hiển thị trực tiếp cho người dùng"
}
```

Bảng đối chiếu mã lỗi để Frontend hiển thị thông báo (Toast / Alert) phù hợp:

| Mã Lỗi (`code`) | HTTP Status | Nguyên Nhân Nghiệp Vụ | Hướng Dẫn Hiển Thị Cho Frontend |
| :--- | :---: | :--- | :--- |
| `NORMAL_CLASS_NON_CANCELLABLE` | 400 | Học viên cố bấm hủy lớp học nhóm Normal 12 buổi. | Vô hiệu hóa nút Hủy, hiển thị tooltip hoặc toast: *"Lớp học nhóm cố định 12 buổi không hỗ trợ hủy hoặc hoàn tiền theo quy chế trung tâm."* |
| `CANCEL_TOO_LATE` | 400 | Học viên bấm hủy buổi học 1:1 khi còn dưới 24 tiếng. | Toast cảnh báo đỏ: *"Không thể hủy lịch khi còn dưới 24 tiếng. Buổi học sẽ bị tiêu thụ nếu không tham gia."* |
| `ADVANCED_PACKAGE_EXPIRED` | 400 | Gói 1:1 đã quá hạn 45 ngày kể từ ngày thanh toán. | Toast cảnh báo: *"Gói học 1:1 của bạn đã hết hạn sử dụng (45 ngày). Vui lòng mua gói mới hoặc liên hệ trung tâm để gia hạn."* |
| `OUT_OF_CREDIT` | 400 | Gói 1:1 đã hết số dư Credit (`remainingCredit < 1`). | Modal gợi ý nạp thêm: *"Số dư buổi học của bạn đã hết (0 Credit). Vui lòng đăng ký thêm gói mới để tiếp tục đặt lịch."* |
| `ADVANCED_BOOKING_TOO_LATE` | 400 | Học viên đặt ca 1:1 khi ca học bắt đầu dưới 12 tiếng. | Toast lỗi: *"Bạn bắt buộc phải đặt lịch trước giờ học tối thiểu 12 tiếng để HLV sắp xếp giáo án."* |
| `OUTDOOR_HEAT_BLOCK` | 400 | Đặt môn ngoài trời (Tennis, Bơi, Bóng đá, Bắn cung) vào Ca 3 hoặc Ca 4. | Ẩn hoặc disable nút chọn Ca 3 và Ca 4, báo: *"Ca 3 (09:30 - 11:00) và Ca 4 (13:00 - 14:30) bị khóa đối với môn thể thao ngoài trời nhằm đảm bảo an toàn sức khỏe (chống sốc nhiệt)."* |
| `ROOM_CONFLICT` | 409 | Phòng / Sân tập đã có lớp hoặc ca học khác chiếm chỗ. | Toast lỗi: *"Sân tập/Phòng học này đã có lịch đặt trong khung giờ đã chọn. Vui lòng chọn sân hoặc khung giờ khác."* |
| `COACH_CONFLICT` | 409 | HLV đã có lịch dạy ca học khác trong khung giờ đó. | Toast lỗi: *"Huấn luyện viên đã có lịch dạy trong khung giờ này. Vui lòng kiểm tra lại bảng ca rảnh của HLV."* |
| `CLASS_CAPACITY_EXCEEDED` | 400 | Sĩ số lớp nhóm đã đạt tối đa (`Registered` + `Pending` $\ge$ `maxCapacity`). | Gắn nhãn *"Hết chỗ (Full)"* trên card lớp, khóa nút Đăng ký: *"Lớp học đã đủ sĩ số tối đa (15 học viên)."* |
| `PAYMENT_NOT_PAYABLE` (dùng fallback message; chưa có code riêng) | 409 | Hóa đơn không còn payable. | Hiển thị lỗi và tải lại hóa đơn. |
| `HOLIDAY_BLOCKED` | 400 | Đặt lịch vào ngày nghỉ lễ/Tết đang được trung tâm kích hoạt. | Tô đỏ ô lịch ngày lễ: *"Trung tâm tạm đóng cửa nghỉ lễ trong ngày này. Vui lòng chọn ngày học khác."* |
| `SUBSTITUTE_COACH_CONFLICT` | 409 | HLV thay thế được chỉ định bị trùng lịch dạy khác. | Toast lỗi: *"HLV thay thế đã có lịch dạy khác trong khung giờ này. Vui lòng chọn HLV khác."* |
| `ROOM_HAS_FUTURE_SESSIONS` | 400 | Đổi trạng thái phòng sang Bảo trì/Đóng cửa khi còn ca học tương lai. | Toast lỗi: *"Phòng/Sân tập vẫn còn các buổi học đã xếp lịch trong tương lai. Vui lòng dời lịch các buổi học trước khi đóng sân."* |
| `INVALID_TIME_SLOT` | 400 | Giờ bắt đầu / kết thúc gửi lên không khớp 7 ca học chuẩn. | Toast lỗi: *"Khung giờ học không hợp lệ. Vui lòng chọn đúng 1 trong 7 ca học chuẩn của trung tâm."* |

## 12. CONTRACT AN TOÀN BỔ SUNG NGÀY 23/10/2026

### 12.1. Tạo Normal và 12 Session nguyên khối
`POST /api/classes/normal-with-sessions`, authority `MANAGE_CLASSES`, trả 201 `SportsClassResponse`.

```json
{
  "requestId": "5c337454-c07a-45a9-8aa7-e0c26e3ff467",
  "sportsClass": {
    "name": "Tennis Normal K01", "subjectId": 1, "coachId": 9,
    "maxCapacity": 15, "startDate": "2026-11-02", "classType": "Normal", "totalSessions": 12
  },
  "roomId": 1, "daysOfWeek": [1, 3, 5], "startTime": "06:00", "endTime": "07:30"
}
```
ID chỉ minh họa: lấy Subject/Coach/Room thực tế cùng center; Coach có specialization Active liên kết đúng Subject. Cùng requestId/body trả cùng Class; body khác trả 409 `IDEMPOTENCY_CONFLICT`. Class, 12 buổi và mã idempotency commit cùng transaction; xung đột rollback toàn bộ.

Không thêm Draft: POST Normal kiểu cũ trả Closed để chuẩn bị lịch, generate đủ 12 rồi PUT Class sang Open. Generate đã có lịch trả `SCHEDULE_ALREADY_EXISTS`, không append thêm 12 buổi. Publish/enroll kiểm tra đủ 12 Session tương lai thực tế, không chỉ tin ngày Class từ client. Backend giữ sĩ số Normal tối đa 15 và 12 buổi cố định.

### 12.2. Lịch cá nhân và lịch trước mua
- GET `/api/sessions`: ép memberId/coachId theo người đăng nhập; staff có MANAGE_CLASSES/REGISTER_MEMBER xem trong center. Không dùng để đọc lịch người khác.
- GET `/api/sessions/{id}` kiểm tra owner/phân công/center.
- GET `/api/sessions/catalog/{classId}`: người đã đăng nhập xem lịch Normal Open/Ongoing trước mua. Trả `{id,sessionDate,startTime,endTime,coachName,roomName}`; không có Member/subscription, không cung cấp Advanced private.
- EnrollmentResponse dùng `id`, có `amount`, `expiresAt` cho đơn Pending. Ngày giờ LocalDateTime không chứa offset, được diễn giải theo Asia/Ho_Chi_Minh (UTC+7); FE phải dùng múi giờ này khi đếm ngược.
- Xem gói cá nhân bằng GET `/api/members/{memberId}/subscriptions` hoặc `/packages`; không có GET `/api/member-packages/me`.

### 12.3. Danh mục và validation
- SubjectRequest có `outdoor: true/false`, bắt buộc khi tạo môn; SubjectResponse.outdoor có thể null với dữ liệu cũ chưa phân loại. Không suy loại môn từ tên/slug. Môn chưa phân loại không được xếp lịch.
- Có lịch Scheduled tương lai thì chưa đổi loại môn hoặc capacity/facilityType của phòng; xử lý lịch trước, không tự dời hàng loạt.
- DELETE Subject còn Class/Specialization trả 409 `SUBJECT_IN_USE`; FK bảo vệ cả liên kết phát sinh đồng thời.
- Package nhận Normal/Advanced và totalCredit dương. Chưa chọn giá 0đ hoặc credit dùng chung nhiều môn.
- Coach API dùng `id`, `name`, `avatarUrl`, `specializations`, `email`, `phone`, thêm `bio`, `certification`, `status`. Contact chỉ hiện theo MANAGE_USERS. Book/generate/substitute kiểm Coach Active, cùng center, đúng môn qua specialization.subjectId.

### 12.4. Tiền, credit, điểm danh và lịch sử
> D10-a đã chốt 5 phút ngày 25/10, xem docs 04 mục 21.3; chưa xác nhận giá trị cấu hình đang chạy trên Render.
- Cash: POST `/api/invoices/{id}/confirm-cash`, cần role RECEPTIONIST và MANAGE_INVOICES; lưu người/giờ thu. Chưa mở thêm quyền thu Cash cho Manager.
- Holding chuẩn đã chốt 5 phút; deadline được kiểm khi xác nhận, job giải phóng suất chạy mỗi 60 giây. Độ trễ job không kéo dài quyền thanh toán. Chưa đổi env hoặc deadline hóa đơn cũ.
- VNPay hợp lệ về sau khi đơn không còn payable vẫn được ghi Paid, `reconciliationRequired=true`; không tự kích hoạt suất/gói đã giải phóng. Callback lặp không cấp quyền lần hai. Hóa đơn cần đối soát không dùng để tự re-activate gói qua API đổi status. Quy trình Manager xếp lại lịch/hoàn tiền còn chờ chốt.
- InvoiceResponse thêm `reconciliationRequired`. Đổi status/gia hạn không nạp lại credit đã dùng. Cancel hoàn về subscription gốc còn Active/hạn, không chọn gói khác; dữ liệu mất liên kết/vượt trần bị từ chối.
- Normal không hỗ trợ báo vắng/hủy enrollment, kể cả staff. Hủy Invoice Pending có route riêng trước khi thu tiền.
- HLV 1:1/thay thế được kiểm theo Session.coach và bookedMember. Staff chỉ hỗ trợ điểm danh, không ghi nhận xét chuyên môn.
- Finalization chuyển Session Completed; Normal đủ 12 buổi chốt thì Enrollment Completed. Advanced chỉ Completed khi credit bằng 0 và không còn ca Scheduled chưa chốt; 0 credit đang giữ cho lịch tương lai chưa phải học xong.
- `scms.booking-block.enabled=false` tắt khóa 7 ngày, vẫn ghi vắng/tiêu thụ buổi; giữ code và test chế tài.
- Toggle ngăn tạo khóa tự động mới; không tự xóa bookingBlockedUntil đã có. Manager vẫn dùng route gỡ khóa hiện có khi cần.
- Không xóa/dời ca đã kết thúc/finalized; không hủy thẳng ca có quyền học đã trả tiền qua update-status để tránh mất quyền. Dùng reschedule; chính sách hủy sự cố khác còn chờ quyết định.

Mã mới (400): CLASS_NOT_READY, NORMAL_SESSION_COUNT, SCHEDULE_ALREADY_EXISTS, COACH_INACTIVE, COACH_SUBJECT_MISMATCH, SUBJECT_FACILITY_REQUIRED, SUBSCRIPTION_LINK_REQUIRED, CREDIT_LIMIT_EXCEEDED, SESSION_HISTORY_IMMUTABLE, INCIDENT_POLICY_REQUIRED. Các xung đột (409): IDEMPOTENCY_CONFLICT, SUBJECT_IN_USE, ROOM_CONFLICT, COACH_CONFLICT, SUBSTITUTE_COACH_CONFLICT. FE vẫn cần fallback message/status với lỗi chưa có code.

Tạo Member tại quầy có packageId: backend tạo subscription và Invoice Cash Pending, không cấp quyền học ngay. Staff tra Invoice theo memberId rồi confirm-cash sau khi nhận đủ tiền. MemberResponse.membershipStatus là pending và totalSpent không cộng đơn chưa thu. Tổng đã thu lấy từ Invoice Paid.amount đã chốt, không lấy giá package hiện tại.


## 13. F-06/F-07 — đồng bộ schema Session ngày 23/10/2026

Không đổi endpoint, payload hoặc quyền. Session.memberPackage cần cột nullable public.sessions.subscription_id tham chiếu member_packages(subscription_id). DB hiện hữu dùng migration `07_session_subscription_postgres.sql`, sau 04/06; hai schema dựng mới đã đồng bộ, FK tạo sau bảng đích. Không chạy schema seed lên DB đang dùng.

Log Render bổ sung xác nhận truy vấn thiếu subscription_id lúc 08:28:28 UTC+7 ngày 23/10. Probe thật bắt đầu 19:23:02 UTC+7 tại https://scms-app-p1gy.onrender.com: Session list bốn vai trò và dashboard Manager đều 200; không tái hiện 500. Chưa có SHA deploy hay lịch sử sửa DB để nối các mốc. DB trong .env hiện có cột/FK/index, chưa xác nhận trùng DB Render. Xem bằng chứng trong `be-request-f06-f07.md` và HISTORY.

FE chỉ đóng F-06/F-07 sau khi đối chiếu SHA deploy và probe mới: Session list theo bốn vai trò, dashboard Manager, dữ liệu và phạm vi owner/assignment/center đúng. Test local chưa chứng minh lịch, đặt lớp và dashboard đã nghiệm thu Render.

Lưu ý quyền live: Coach mẫu có MANAGE_CLASSES và đọc dashboard được (200), khác seed workspace. Backend dashboard hiện chấp nhận VIEW_REPORTS/MANAGE_USERS/MANAGE_CLASSES. Chờ đối chiếu cấu hình quyền mong muốn; không tự thay role/permission để đạt status kỳ vọng.

## 14. Theo dõi thay đổi Advanced cho bàn giao FE — DỰ KIẾN, CHƯA TRIỂN KHAI

> Quyết định mới nhất về hạn tại docs 04 mục 30.3 thay các mô tả 1080 giờ liên tục bên dưới: Advanced 45 ngày sử dụng từ Paid, TT nghỉ/đóng thì skip; sửa ngày nghỉ cập nhật hạn gói đang dùng, không cộng bù trùng D08. Chưa code/deploy. FE dùng deadline chính xác BE trả, không tự suy Paid+45 ngày. Biên sửa ngày nghỉ quá khứ/lịch đã đặt đang chờ tại 30.4.

Ngày 25/10/2026: đây là danh sách thay đổi mục tiêu theo 04-DEBUG-NOTE.md; nhiều quyết định đã chốt nhưng **chưa triển khai, không phải API mới khả dụng**. Chúng ta là BE phụ trách code/kiểm thử/tài liệu; FE tích hợp theo phiên bản BE được bàn giao. Trước bàn giao phải thay nội dung dự kiến bằng endpoint/method, request/response, quyền, trạng thái, lỗi, ví dụ và kết quả kiểm thử thực tế của từng thay đổi.

| Feature/nghiệp vụ | Tình trạng quyết định | Phần phải ghi rõ khi triển khai/bàn giao |
|---|---|---|
| Hồ sơ Member tự phục vụ | D01 đã chốt, chưa triển khai | GET/PATCH /api/members/me; name/phone/address/dateOfBirth/gender/goal/healthNote, DTO riêng, owner từ Auth. Chốt kiểu/null/validation JSON lúc code; không mở status/role/credit, email/password/avatar dùng luồng riêng |
| Danh mục giá và fixture | D02 đã chốt phạm vi, chưa migration | Chín môn/18 giá ở 2.1; giữ 19–27 theo snapshot đúng DB, xóa bản dư thay Inactive. FE tải ID/giá thực từ BE, không hardcode ID snapshot hoặc giữ môn ngoài PDF. Đối soát fixture được chỉ rõ trong docs 04, không reset DB hoặc sửa Paid.amount |
| Lỗi GET /api/coaches | D09 có log lazy loading, chưa sửa | Chúng ta sửa BE query/mapping, giữ response/contact/center/bộ lọc; FE tiếp tục dùng API 4.5, không đổi sang mock để che lỗi. Khi bàn giao cung cấp SHA và probe Manager/Member, màn chọn Coach thật; không hứa đã fix chỉ từ log |
| Mua Advanced theo môn và Coach chính trên từng đăng ký | Đã chốt D03-a, chưa triển khai | Payload chọn Subject/Primary Coach; response gói theo môn; nguồn giá Subject.advancedPrice, snapshot Invoice.amount, không nhận giá tùy ý; ca phải khớp môn/Coach hợp lệ |
| Lịch làm việc Coach, ngoại lệ và ngừng nhận học viên mới | Owner đồng ý hướng D03-b, chưa triển khai | API quản lý/xem lịch, quyền theo center/Coach, ca chuẩn và dữ liệu lịch trống; chưa có endpoint mới được chốt |
| Coach xét khả năng nhận trước tạo Invoice; Manager xác nhận thay có audit | Owner đồng ý hướng D03-b, chưa triển khai | Luồng yêu cầu/duyệt/từ chối, trạng thái và thời hạn xét duyệt, actor/lý do, thông báo và kiểm tải; chờ duyệt chưa thu tiền/chưa chạy hạn gói; Invoice chỉ bắt đầu cửa sổ 5 phút khi phát hành |
| Cam kết năng lực khi phát hành Invoice | Hướng D03-b, chưa triển khai | Tính Pending còn hạn và credit chưa xếp; lock/idempotency; hết hạn giải phóng; không đồng nghĩa giữ ca/sân; UI không hứa đã có 12 lịch |
| Member nhập thời gian rảnh lúc mua | Đã chốt bỏ khỏi MVP | Không thêm field/form/API khai lịch Member; giữ trao đổi ca cụ thể sau Paid. Lịch làm việc Coach cũng chưa phải tính năng sẵn có |
| Thời hạn Advanced 45×24 giờ | Đã chốt D03-c, chưa triển khai | Đủ 1080 giờ từ Paid hợp lệ; bỏ Ngày 0/cutoff. BE lưu/trả thời điểm kích hoạt/hết hạn chính xác; FE hiển thị giờ Việt Nam. Trước Paid chỉ có hạn dự kiến, sau Paid dùng hạn chính thức BE trả |
| Minh bạch hạn khi mua và tạo/dời ca | Ghi nhận cùng quyết định D03-c, chưa triển khai | FE công bố quy tắc trước mua, hiển thị giờ hết hạn/thời gian còn lại; đánh dấu ca vượt hạn hoặc thiếu notice và lý do. BE kiểm hiện tại còn quyền, notice >=12 giờ, ca kết thúc không vượt hạn; lịch cho subscription cụ thể cần xét hạn gói. Không dùng hạn gói -12 giờ làm mốc đặt cuối chung. Không tự thêm lịch gửi thông báo chủ động khi chưa chốt ngưỡng/kênh |
| Quyền tạo ca, xác nhận trao đổi và thông báo tạo ca | Đã chốt A ở D04-a, chưa triển khai | Thiết kế mục tiêu: Coach chính tạo sau thống nhất; Manager cùng center hỗ trợ có lý do/audit, Member xem/nhận thông báo. Bắt buộc xác nhận trao đổi, lưu actor/time thật, chống lặp và chỉ thông báo ca thành công; không có vòng Member duyệt từng ca. Member hiện còn tự book theo implementation 6.3; đồng bộ quyền/màn FE cùng BE khi triển khai. Handoff 04 mục 12.4 |
| Cutoff hủy Advanced | Đã chốt D05-a 12 giờ, chưa triển khai | >=12 giờ hủy/giải phóng ca/hoàn đúng một credit vào gói gốc; <12 giờ không hủy hoàn thông thường, không trừ thêm. Không gia hạn/hoàn tiền hoặc áp sang Normal; dời/báo vắng xét riêng. Implementation 6.4 hiện còn 24 giờ; handoff BE/FE và biên kiểm thử ở 04 mục 13.3 |
| Báo vắng Advanced và kết quả điểm danh | Đã chốt D05-b A, chưa triển khai | Chủ ca báo trong khoảng 0 < thời gian còn lại <12 giờ, lưu dấu báo trước riêng (actor/time/lý do), giữ credit/tài nguyên; Coach/hệ thống chốt Present/Absent theo thực tế. Thay thiết kế ghi Excused/notice hai giờ hiện tại; không chốt Absent từ thao tác báo, không mở cho Normal. Handoff BE/FE và kiểm thử ở 04 mục 14.3 |
| Sự cố trung tâm và Member đồng ý phương án | Đủ ba phương án đã chốt; D06-a chốt A, chưa triển khai | Manager cùng center ghi nhận đã trao đổi/Member đồng ý đúng phương án dời/thay, lý do/actor/time và ca/Coach cũ-mới; hủy ghi kết quả không thống nhất tương ứng. Audit và thông báo sau commit; không có vòng Member trực tiếp duyệt. Handoff BE/FE ở 04 mục 15.4; quyền bù sau hạn xử lý D08 |
| Notice ca mới khi dời do trung tâm | D06-b đã chốt A, chưa triển khai | Manager dời do lỗi trung tâm được chọn ca mới dưới 12 giờ khi ca cũ/mới chưa bắt đầu, Member đồng ý và tài nguyên/quyền học hợp lệ. Giữ credit, không tự gia hạn; đặt mới vẫn >=12 giờ, không tự áp ngoại lệ cho Member/ca đã diễn ra. Handoff và kiểm thử 04 mục 15.6 |
| Dời sớm theo yêu cầu Member | D07-a đã chốt A, chưa triển khai | Coach chính thao tác/Manager cùng center hỗ trợ sau thỏa thuận; ca cũ/mới cách hiện tại >=12 giờ, ca mới kết thúc trong hạn, không đổi credit/hạn. Không mở Member tự dời/workflow mới; handoff 04 mục 16.3 |
| UI/UX chung tạo và dời ca | Owner yêu cầu, chưa triển khai | Dùng chung bộ chọn ngày/ca/phòng, mở dời từ Session và điền sẵn Member/gói/môn/Coach; so sánh cũ-mới/tác động credit trước xác nhận. Preview không giải phóng lịch cũ, dời là transaction riêng không phải tạo rồi hủy. Phân quyền/nguyên nhân và lỗi xung đột theo BE; handoff 04 mục 16.4 |
| Dời sát giờ theo yêu cầu Member | D07-b đã chốt A, chưa triển khai | Chỉ Manager cùng center duyệt bất khả kháng khi 0 < thời gian tới ca cũ <12 giờ; ca mới vẫn >=12 giờ và trong hạn, giữ credit/hạn, lý do/xác nhận/audit. Không tự áp ngoại lệ lỗi trung tâm; dùng chung UI dời có chế độ ngoại lệ riêng. Handoff 04 mục 17.3 |
| Báo vắng cũ khi dời ca | D07-c đã chốt, chưa triển khai | Chỉ thao tác “Dời ca” thành công có đổi ngày/giờ mới bỏ dấu báo vắng đang hiệu lực; lịch sử giữ phía hệ thống, lịch mới Scheduled không mang dấu. Chỉ đổi sân/Coach cùng giờ, không đổi lịch, preview/thất bại không bỏ dấu. Không đổi credit/hạn; chống retry/race xóa nhầm dấu mới, handoff 04 mục 18.4 |
| Gia hạn do lỗi trung tâm | D08-a B và D08-b đã chốt, chưa triển khai | Gia hạn toàn gói bằng thời gian gián đoạn thực tế không trùng; Manager duyệt thêm tối thiểu đủ ca bù khi cần, có lý do. Một luồng trong màn xử lý sự cố: khoảng ảnh hưởng → thời lượng bù/hạn đề xuất → duyệt. Không cấp lại 1080 giờ/12 credit hoặc quyền bù riêng A; BE tính/chống cộng trùng, Member xem hạn mới. Handoff 04 mục 19.5 |
| Phục hồi credit bị expiry thu hồi | D08-c đã chốt, chưa triển khai | Manager phục hồi đúng số dư hợp lệ có căn cứ khi sự cố phát sinh lúc gói còn hạn và hạn bù mới còn hiệu lực; job lưu số dư trước thu hồi để đối soát. Không mặc định 12/không mở mọi gói cũ, không hoàn trùng ca; tích hợp cùng màn duyệt hạn. Handoff BE/FE/kiểm thử 04 mục 19.7 |
| Sự cố chưa biết lúc khôi phục | D08-d đã chốt, chưa triển khai | Lưu “Chờ khắc phục”, bổ sung giờ kết thúc thực tế rồi tính/duyệt bù; không gia hạn tạm/vô hạn. Nếu gói hết hạn trong lúc chờ thì chưa đặt tiếp, sau duyệt hạn hợp lệ phục hồi theo D08-c. Một màn lưu chờ/hoàn tất, không workflow Member duyệt mới; handoff 04 mục 19.9, chưa có API/status mới khả dụng |

Khi bàn giao, bổ sung hướng chuyển đổi dữ liệu/phiên bản tương thích và ca kiểm thử FE–BE; phân biệt các deadline của yêu cầu xét duyệt, Invoice giữ chỗ và gói học. Paid vẫn là chốt kích hoạt; callback cần đối soát không tự cấp quyền. Chưa đổi endpoint/error code đang dùng chỉ bằng việc ghi mục này.

Hướng dẫn handoff BE chi tiết: mục 11.16 trong [04-DEBUG-NOTE.md](04-DEBUG-NOTE.md), gồm danh sách thay đổi, trình tự migration/policy/kích hoạt/validation/DTO, bộ ca nghiệm thu và giới hạn thông báo chủ động. Khi có implementation, thay ngữ nghĩa dự kiến bằng tên field/endpoint/error thật và bàn giao FE cùng phiên bản BE tương ứng; không dùng Invoice.expiresAt làm hạn học.

### 14.1. Cách đọc trạng thái và ranh giới BE–FE

- **Đã chốt/chưa triển khai:** Owner đồng ý business rule, chưa có API mới để FE gọi. **Đã code/test local:** chưa chứng minh Render có bản đó. **Đã deploy/probe:** phải kèm SHA và môi trường; FE nghiệm thu luồng thật rồi mới đóng tích hợp.
- BE chịu trách nhiệm Auth/role/owner/center, giá snapshot, quyền học/thời gian, locks/transaction/idempotency, credit, audit/notification sau commit và validation/lỗi cuối cùng. FE hiển thị/điều hướng/disable theo dữ liệu BE, gửi yêu cầu, giải thích lỗi và tải lại trạng thái; không tự quyết giá/deadline/quyền hoặc báo thành công khi request thất bại.
- Với timestamp gói mới phải có giờ và offset rõ, FE hiển thị Asia/Ho_Chi_Minh; deadline giữ thanh toán 5 phút khác deadline gói 1080 giờ. Trường ngày cũ chỉ giữ nếu có kế hoạch tương thích, không để hai nguồn hạn cùng quyết định một quyền.
- Mẫu endpoint trong mục 1–13 chưa đồng bộ phải được sửa ở chính vị trí sau khi code; mục 14 là danh sách theo dõi, không thay cho contract JSON cuối cùng. Không đổi public API cho D09 nếu sửa được bên trong query/mapping.

### 14.2. Gói bàn giao FE bắt buộc theo từng phần BE hoàn thành

| Phần bàn giao | Nội dung cụ thể |
|---|---|
| Phiên bản/phạm vi | Mã Dxx, source SHA, deploy SHA/URL, migrations/config đã áp; trạng thái local/deployed/FE verified và phần còn mở |
| API thật | Method/path/quyền/owner/center, request/response JSON thực, field bắt buộc/nullable, timestamp offset, status/error code/message và các thay đổi tương thích |
| UX nghiệp vụ | Người có nút tạo/dời/hủy/báo vắng/xử lý sự cố; form tạo–dời chung, dữ liệu điền sẵn, cũ-mới, credit/hạn trước-sau, xác nhận trao đổi/lý do, notification và lỗi xung đột sau preview |
| Ví dụ thành công/thất bại | Đúng/sát biên 12 giờ, ca kết thúc vượt hạn, Pending/Paid/expiry, thiếu quyền/sai owner/center, retry/race, lỗi API không dùng fallback mock |
| Dữ liệu test | Fixture/ID tra từ đúng môi trường và kịch bản có thể lặp, quyền mutation/reset được phép; không dùng Paid cũ để kích hoạt quyền mới hoặc gọi Sandbox là DB cô lập |
| Kết quả kiểm | clean test thật sau code, tích hợp DB phù hợp, probe trên đúng SHA, FE browser E2E với API thật; không dùng kết quả lịch sử làm chứng nhận feature mới |

Nếu một phần chưa xong thì ghi rõ chưa khả dụng và phần FE cần chờ; không gửi bảng đề xuất dưới tên “API đã bàn giao”. Sau mỗi phần cập nhật docs 00 tiến độ, 02 rollout/bằng chứng, 04 quyết định; không đợi tới cuối dự án mới sửa contract.

### 14.3. D09 — chuẩn bị bản sửa và ca tích hợp FE, chưa bàn giao bản đã fix

- Giữ `GET /api/coaches`, query/DTO ở 4.5; bản sửa dự kiến chỉ đổi query/transaction/mapping BE. Không thêm payload/field mới hoặc yêu cầu FE đổi cách chọn Coach. Đã đối chiếu quyền Authenticated với source, chưa probe mới.
- Khi BE sửa/test xong, bàn giao source SHA và kết quả clean test; khi deploy xong, bổ sung deploy SHA/URL/config OSIV=false và response thật Manager/Member. Hiện chưa có các bằng chứng này cho bản sửa D09.
- FE kiểm màn danh sách/chọn Coach bằng đăng nhập thật: tải danh sách có chuyên môn, tìm tên Coach/tên chuyên môn, lọc specializationId, kết hợp tìm + lọc, kết quả rỗng và Coach không chuyên môn. Đối chiếu contact theo MANAGE_USERS và phạm vi center; ghi request/status/body đã ẩn thông tin cá nhân cần thiết, không dùng mock thay API lỗi.
- Kế hoạch test tái hiện không có transaction test bao quanh request, batch fetch và chống N+1: docs 04 mục 20.6. Chưa có thay đổi về năng lực nhận học viên/Primary Coach của D03 trong D09.

### 14.4. D10-a — đã chốt holding 5 phút

Owner đã đồng ý 5 phút tại docs 04 mục 21.3. Source dùng `payment.expire-minutes` với hai env override, khác tên key trong AGENTS; chưa xác nhận cấu hình live. FE dùng expiresAt BE làm deadline, không khởi động lại 5 phút khi mở modal/nhấn Pay. Không tính lại hạn hóa đơn cũ từ quyết định này. Invoice 3/null method/null deadline đang phân tích riêng tại docs 04 mục 22, chưa có bản sửa khả dụng.

### 14.5. D10-b — đã chốt A, chưa triển khai

Source hiện tại: pay-online cần ROLE_MEMBER, đúng owner, method VNPay, status Pending và chưa expired; trả `{paymentUrl}` khi hợp lệ, 404 khi không tồn tại/khác chủ, 409 khi không payable. Guard expired hiện không coi deadline null là hết hạn; chưa có guard riêng từ chối null deadline. Snapshot Invoice 3 thiếu method đủ phù hợp nhánh 409, chưa xác nhận live cùng SHA. Owner đã chọn A tại docs 04 mục 22.3: chặn online khi thiếu method/deadline, giữ chứng từ/amount, đối soát trước xử lý đơn cũ/tạo đơn mới đúng luồng; không bổ sung phương thức/hạn để mở lại hóa đơn cũ. Chưa sửa BE/field/status/error code; không coi guard mới đã khả dụng.

D10-c: Owner xác nhận FE_Project-main là bản tham khảo rất cũ, không dùng mapper/test ở đó làm bằng chứng FE hiện hành. Handoff cho FE tự đối chiếu bản mới theo docs 04 mục 23.3: giữ trạng thái thiếu dữ liệu thay vì suy Cash/VNPay, Pay theo dữ liệu thật/BE quyết định cuối, countdown theo expiresAt, 409 tải lại hóa đơn. Chưa FE verified. Source gateway còn gọi expiresAt.atZone nên null deadline vượt guard sẽ lỗi tạo URL; chưa phải trường hợp nhận URL thành công.

### 14.6. D10-d1 — đã chốt A, chưa triển khai

Source hiện tại cả GET /api/payments/vnpay/return và /ipn gọi processVnpayResult có thể ghi DB. Owner đã chọn A tại docs 04 mục 24.3: chỉ IPN cập nhật thanh toán trong hai đường này, Return điều hướng/hiển thị, FE đọc Invoice qua API xác thực. Không đổi luồng Cash riêng. Chưa thay hành vi hai endpoint. Không dùng query status trên redirect làm bằng chứng Paid: hiện là mã xử lý, không phải paymentStatus.

D10-d2 đã chốt A tại docs 04 mục 25.3: tự refresh hữu hạn khi Pending, sau đó cho Kiểm tra lại/xem hóa đơn; không tự báo thất bại/thu lại. API đọc hiện có là GET /api/members/me/invoices/{id} (ROLE_MEMBER, đúng owner; khác chủ/không có trả 404). Dùng paymentStatus và reconciliationRequired làm căn cứ; Pending không tự là thất bại, Paid cần đối soát không hứa đã kích hoạt quyền học. Hết phiên thì đăng nhập lại, không thêm API public để bỏ qua owner. Chưa chốt query/field mới của Return hoặc thông số polling; chưa FE verified.

Owner yêu cầu thuận tiện bổ sung email Member sau này: BE chuẩn bị nguồn kết quả/sự kiện sau commit, tách giao email khỏi callback/Return/polling và chống trùng; FE không gửi mail thay BE. Chưa bật email thanh toán, chưa có field/API email mới. Chi tiết độ bền/retry và test tại docs 04 mục 25.3.

### 14.7. D10-e1 và D10-e2a — đã chốt A, chưa triển khai

Owner chốt A tại docs 04 mục 26.4: IPN thành công hợp lệ được xử lý lúc đã tới/quá deadline hoặc đơn không còn Pending thì ghi Paid/reconciliationRequired=true, không tự kích hoạt; Paid đã ghi xử lý idempotent. Không đổi chuẩn 5 phút, không mở lại quyền đã giải phóng. FE hiển thị đã ghi nhận tiền/đăng ký cần xử lý, không báo thất bại hay mời trả lại; thời điểm gateway là chứng cứ, không tự thay hạn giữ.

D10-e2a Owner chọn A tại docs 04 mục 27.3: role CenterManager duyệt, Receptionist hỗ trợ; chưa chọn phương án cấp quyền/hoàn tiền hoặc công bố API xử lý mới. “Center” trong ghi chú trước là phạm vi users.center_id có sẵn trong source, không phải role mới hoặc quyết định thêm nhiều chi nhánh. Không dùng confirm-cash/update-status gói để đóng đối soát. Chưa BE/FE triển khai hoặc nghiệm thu.

D10-e2b chờ chọn tại docs 04 mục 28.2: A cho CenterManager ghi nhận thỏa thuận Member, cấp đúng quyền đã mua sau kiểm lại đầy đủ hoặc hoàn đủ tiền nếu không thể/không muốn tiếp tục; B chỉ hoàn đủ tiền, đăng ký lại riêng sau đó. Chưa có API/DTO/trạng thái xử lý mới khả dụng. Không trộn với D10-b đơn thiếu dữ liệu chưa thu tiền, không thu lại hoặc reset hạn 1080 giờ từ lúc duyệt. Cơ chế thực hiện hoàn tiền tách bước sau.

Sau yêu cầu ưu tiên đơn giản/gộp quyết định của Owner, dùng ba phương án tổng thể tại docs 04 mục 28.4 để lựa chọn tiếp: (1) refund-only ngoài hệ thống có chứng cứ — đề xuất MVP; (2) refund-only qua API gateway; (3) cấp quyền có kiểm soát hoặc hoàn ngoài hệ thống. Chưa chọn phương án nào; câu tách kênh hoàn sang bước riêng phía trên là kế hoạch trước điều chỉnh. Chưa có endpoint/field mới để FE tích hợp; mọi hướng đều cần trạng thái xử lý rõ, không coi duyệt hoàn là tiền đã hoàn hoặc Paid gốc là doanh thu ròng sau hoàn.

**Quyết định cuối D10-e2:** Owner chọn hướng 1 tại docs 04 mục 28.5, thay các ghi chú chờ chọn ở trên. MVP hoàn đủ ngoài SCMS do CenterManager duyệt, lưu chứng cứ/kết quả hoàn riêng; không cấp lại quyền từ khoản này. Refund API và cấp quyền lại để nâng cấp sau khi hệ thống ổn định. FE cần thể hiện khoản đã nhận/đang xử lý/đã hoàn theo dữ liệu BE sau triển khai, không dựa Paid để hứa quyền học. Chưa có DTO/endpoint hoàn mới khả dụng.

### 14.8. D11 — chờ chọn môi trường tích hợp

Ba hướng local PostgreSQL riêng rồi smoke deploy / staging BE+DB riêng / deploy hiện có với fixture giới hạn đang chờ chọn tại docs 04 mục 29.2. Cần source FE hiện hành và SHA BE/deploy tương ứng trước FE E2E; không dùng FE_Project-main cũ hoặc VNPay Sandbox làm bằng chứng dữ liệu đã cô lập. Chưa cấp phạm vi mutation cloud hoặc đổi cấu hình FE.

**D11 đã chốt hướng 1 tại 04 mục 29.3:** PostgreSQL local riêng/fixture riêng để kiểm nghiệp vụ, FE hiện hành nối BE test khi kiểm UI, rồi smoke deploy có kiểm soát. Chưa tạo môi trường hoặc mở mutation cloud toàn bộ; ghi chú chờ chọn trên là lịch sử.

### 14.9. D12 — chờ xác nhận bộ quy tắc ca/ngày nghỉ

Đề xuất tại docs 04 mục 30.2: giữ 7 ca hiện hành, Outdoor khóa ca 3 (09:30–11:00)/4 (13:00–14:30), mọi Holiday active trong ba tag chặn lịch mới, Normal bỏ ngày nghỉ để đủ 12 buổi; Advanced bị từ chối ngày nghỉ không tạo ca/trừ credit và không tự kéo hạn 1080 giờ. Chờ Owner xác nhận giờ ví dụ PDF sai/nghĩa “không trừ”; không có API/time-slot mới khả dụng. Lịch đã tồn tại chịu ảnh hưởng khi đổi Holiday tách D14, không hướng dẫn FE tự dời/hủy.

**Cập nhật quyết định hạn D12:** Owner đã chọn skip TT nghỉ/đóng và cập nhật hạn gói đang dùng khi thêm/tắt ngày nghỉ, không bù trùng (04 mục 30.3), thay nội dung “không tự kéo hạn” ở đề xuất cũ. Chưa có field/API mới; khi code sẽ đồng bộ mọi mô tả endpoint/ví dụ hạn thay vì chỉ giữ chú thích này. Đề xuất giữ phần nghỉ thực tế quá khứ, tính lại phần tương lai và kiểm ảnh hưởng ca đã đặt khi hạn rút ngắn chờ Owner tại 30.4; chưa hướng dẫn FE tự hủy ca hay tự rút hạn.

**Biên D12 đã được Owner đồng ý tại 04 mục 30.5:** giữ phần skip thực tế quá khứ, tính lại phần tương lai; ca đã đặt vượt hạn mới phải được CenterManager xử lý trước khi xác nhận thay đổi, không tự hủy. Gắn cùng preview/confirm tác động Holiday ở D14; chưa có API workflow mới khả dụng.

### 14.10. D13 — đã chốt cả hai, UI ưu tiên theo người

**Quyết định cuối tại 04 mục 31.3:** Owner chọn cả hai, UI/UX ưu tiên theo người. Chỉ số chính phân biệt chưa từng học/còn quyền/từng học hiện hết quyền; số đăng ký hết hạn là bổ sung có đơn vị rõ. Giữ định nghĩa quyền học ở 31.2, không đổi expiredMembers thành đếm gói. Đoạn phân tích chờ chọn bên dưới là lịch sử; chưa code/DTO mới/FE verified.

Source /api/reports/members hiện có expiredMembers=totalMembers-activeMembers, chưa đủ nghĩa quyền học Normal/Advanced. Ba hướng chỉ người/chỉ đăng ký/cả hai chờ chọn ở 04 mục 31.2; đề xuất cả hai, không đổi đơn vị field cũ ngầm. Quyền học mục tiêu cần xét Normal còn buổi và Advanced còn hạn skip + credit hoặc ca hợp lệ đã đặt; Member chưa từng có quyền/khóa tài khoản là nhóm khác. Chưa công bố field mới hoặc chứng nhận report live đúng định nghĩa.

### 14.11. D14 — chờ workflow thay đổi Holiday

**Quyết định cuối tại 04 mục 32.3:** Owner chọn 1, xem ảnh hưởng/xử lý lịch riêng rồi kiểm lại và xác nhận Holiday; còn xung đột thì chặn lưu thay đổi. Giữ lịch sử/cập nhật hạn nhất quán/không tự dời hoặc khôi phục lịch. Đoạn chờ chọn dưới là phân tích trước quyết định; chưa API mới/deploy.

Source có read/create/toggle, chưa có preview ảnh hưởng/update/delete/lịch sử. Ba hướng tại 04 mục 32.2: báo tác động và xử lý lịch riêng rồi confirm (đề xuất MVP), gợi ý/chọn lịch áp dụng chung, hoặc tự dời hàng loạt. Mọi hướng giữ audit, kiểm lại lúc commit, hạn skip/ảnh hưởng ca đã đặt D12, không tự khôi phục buổi khi tắt ngày nghỉ. Chưa có endpoint/DTO workflow mới để FE gọi; chờ Owner chọn trước thiết kế implementation.

### 14.12. D15 — chờ phạm vi check-in/attendance và quyền sửa

**Quyết định mới nhất 04 mục 33.5:** Owner chọn attendance mới Present/Absent trong giờ buổi, không Late/scan sớm 30 phút; CenterManager sửa sau buổi có audit, giữ Late lịch sử. MVP ưu tiên attendance, check-in cửa ưu tiên nâng cấp sau. Các đoạn chờ cấu hình bên dưới là lịch sử đề xuất; source/API hiện chưa thay.

**Quyết định tại 04 mục 33.3:** Owner chọn attendance MVP/CenterManager sửa sau buổi, ưu tiên nâng cấp UI check-in cửa sau; giữ code/data hiện có. Late và mốc 30/10 chưa được chốt. Đánh giá ba cấu hình tại 33.4, đề xuất chỉ Present/Absent trong giờ buổi, không tự Late/scan sớm; chờ chọn trước khi thay API/UI. Giữ Late lịch sử tương thích, chưa xóa dữ liệu hoặc đổi contract hiện hành.

Source có check-in cửa session=null và scan theo Session, không đồng nhất hai dữ liệu; scan đang mở trước 30 phút đến hết buổi, sau 10 phút Late. Sửa attendance hiện chưa giới hạn sau finalized trong service. Ba hướng ở 04 mục 33.2: ưu tiên attendance/cửa để sau/CenterManager sửa sau buổi (đề xuất), cả hai trong MVP, hoặc cho Coach sửa sau buổi. Chưa thay role/endpoint/window/field; báo vắng riêng đã chốt D05 giữ nguyên, không tự sửa attendance để hoàn credit hay giải quyết sự cố.

Sự cố thực sự không tổ chức được buổi nhưng báo sau finalized còn chờ chọn tại 04 mục 34.2: xử lý bù có kiểm soát bằng hồ sơ liên kết buổi gốc, hoặc chỉ ghi hồ sơ/chưa hỗ trợ bù trong MVP. Không dùng /correct hay reschedule quá khứ để cấp bù, chưa có API/mã trạng thái mới. Không suy vắng do Member là lỗi trung tâm.

**D15 sự cố báo muộn đã chốt 1 tại 04 mục 34.3:** CenterManager xác minh, hồ sơ gắn buổi gốc; Advanced hoàn một credit chưa bù, Normal bù để đủ 12; giữ lịch sử/không no-show của Member, không bù trùng D08/D12. Chưa có API/DTO xử lý mới khả dụng; đoạn chờ chọn trên là lịch sử.

### 14.13. D16 — chờ phạm vi Cash/biên nhận/sửa sai

Source confirm-cash đã có actor/time/amount snapshot/audit, cần RECEPTIONIST + MANAGE_INVOICES. Ba gói ở 04 mục 35.2: core thu/note/biên nhận xem-in/hồ sơ sai sót không tự đảo (đề xuất MVP), thêm đảo có kiểm soát quyền chưa dùng, hoặc điều chỉnh đầy đủ. Chưa có note/receipt/correction API mới; FE không cho sửa amount/actor/time client hoặc dùng note để giả đã xử lý hậu quả. Phân biệt ghi Paid nhầm chưa thu với hoàn khoản đã thu thật.

Đánh giá hướng 2 ở 04 mục 35.3: chưa chọn; đề xuất nếu làm thì giới hạn xác nhận Cash nhầm/chưa thực thu, Advanced liên kết rõ và chưa sử dụng/booking/phụ thuộc. Không suy số dư 12 là chưa dùng. Thu hồi Enrollment Normal đụng rule không hủy Normal nên cần quyết định ngoại lệ riêng; chưa tự mở quyền/API đảo cho hai loại. Chưa DTO/migration khả dụng.

**D16 đã chốt 2 hẹp tại 04 mục 35.4:** CenterManager xử lý xác nhận Cash nhầm/chưa nhận tiền, Advanced chưa dùng/booking/phụ thuộc; không Normal hoặc tự hủy lịch để đảo. Giữ chứng từ/amount và ghi điều chỉnh riêng/audit/report/idempotency. Ghi chú chờ chọn trên là lịch sử, chưa API correction/note/receipt mới khả dụng.

### 14.14. D17 — QR contract source, FE chưa verified

GET /api/members/me/checkin-code (ROLE_MEMBER) trả `{payload, expiresInSeconds}`, TTL còn lại bước TOTP 30 giây (1–30); không QR giả/field expiryInSeconds/luôn đếm lại 30. Loading/error/retry, đổi account và 401 phải đúng; D15 scan trong giờ buổi. GET có thể ghi secret lần đầu. Chưa kiểm FE hiện hành/probe mới, handoff chi tiết 04 mục 36.

### 14.15. D18 — đã chốt 1, chưa triển khai

Source dashboard cho VIEW_REPORTS/MANAGE_USERS/MANAGE_CLASSES, có tổng tài chính; Coach live có MANAGE_CLASSES giải thích 200 lịch sử. Ba hướng tại 04 mục 37.2: quản trị CenterManager/Admin + Coach màn cá nhân (đề xuất), giữ quyền rộng hiện tại, hoặc dashboard lọc theo role. Chưa đổi quyền/query/DTO; FE ẩn card không thay BE kiểm dữ liệu.

**Quyết định cuối tại 04 mục 37.3:** Owner chọn 1; CenterManager/Admin có quyền report xem quản trị, Coach nghiệp vụ được giao; MANAGE_CLASSES đơn lẻ không đủ đọc tổng tài chính. Receptionist giữ chức năng thu/Invoice, không tự mở report quản trị. Phân tích chờ chọn trên là lịch sử, API/annotation hiện chưa thay.

### 14.16. D19 và ba chỉ số tiền đã chốt

Kế hoạch kiểm/fixture/version/bằng chứng ở 04 mục 38, dùng source FE hiện hành và BE SHA tương ứng; không dùng build/test FE cũ hoặc kết quả lịch sử làm nghiệm thu mới. Sau sửa BE chạy clean test, PG local theo D11 rồi smoke deploy khi được yêu cầu.

Owner chốt tại 04 mục 39.3: BE trả đủ **tiền đã thu / tiền đã hoàn / thu ròng**; FE quyết định chỉ số chính, hiển thị cả ba hay bố trí khác. BE không ép ưu tiên thu ròng. Số đã hoàn cần chứng cứ hoàn tất, Cash ghi nhầm đã điều chỉnh không tự là tiền đã nhận; thu ròng=đã thu−đã hoàn. Kỳ lọc/đơn vị/tên field thật/compatibility totalRevenue phải ghi khi triển khai, chưa có DTO hoặc API ba số mới khả dụng. Quyền report D18 giữ nguyên quyết định.

### 14.17. Phân công và mốc bàn giao theo hai phase — kế hoạch

Theo docs 04 mục 40: đồng đội nhận D01 profile tự phục vụ và D09 directory read-fix, sở hữu file giới hạn/commit riêng, BE chính giữ các flow/schema/docs trung tâm. Handoff T phải có JSON/error/test/evidence thật, BE chính cập nhật contract ở endpoint tương ứng sau xác minh/merge, không để hai nhánh sửa bốn docs cùng lúc.

Phase 1 bàn giao catalog/đăng ký theo môn-Coach, time policy, mua/thanh toán/quyền và gói T. Phase 2 bàn giao booking/dời/hủy/báo vắng, sự cố/Holiday/attendance/đối soát/sửa Cash/report. Chưa hoàn thành phase phụ thuộc thì ghi pending, không giới thiệu tất cả API mục 14 là khả dụng. BE chính cung cấp source/deploy SHA/manifest/fixture đúng môi trường; FE dùng source hiện hành, không bản FE cũ. Ba chỉ số tiền đủ dữ liệu từ BE, layout do FE quyết.

Demo qua API/PG local thực và FE hiện hành khi có; callback local có chữ ký vẫn ghi LOCAL INTEGRATION, không gọi VNPay live. Chuẩn nghiệm thu/bằng chứng tại 04 mục 40.6 và docs 02 mục 9.9, chưa có kết quả mới trong lượt plan.

Rà bàn giao 27/10: 04 mục 40.8–40.9 có checklist D01/D09 chi tiết cho đồng đội, phạm vi file/điều cấm/test/acceptance; BE chính cập nhật contract thật sau merge. Baseline hiện có đã clean test/PG riêng đạt theo docs 02 mục 9.10 và artifact review, chưa phải D01/D09 đã code hoặc API mục tiêu khả dụng. .env DB test mới không chia sẻ qua Git; kiểm kết nối không tương đương nghiệm thu schema/flow.

Baseline đã push tai `9097197`; lần kiểm Supabase .env sau push chưa kết nối được (pooler tenant/user not found, direct JDBC UnknownHostException). Chưa có schema/API hoặc FE integration verified trên DB mới; kết quả/điều kiện thử lại ở docs 02 mục 9.11. Không dùng baseline local xanh để hứa môi trường mới đã chạy đúng.
