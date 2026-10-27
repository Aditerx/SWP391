# 01-API-CONTRACT-FE-BE.md — Hợp Đồng Kết Nối API Giữa Frontend & Backend

> **Mục tiêu:** Đây là tài liệu duy nhất và chính thức quy định toàn bộ Endpoints, Headers, Request Body, Response Schema, Bảng giá chuẩn 9 bộ môn, và Từ điển Mã lỗi (Error Codes Dictionary) để đội Frontend (`FE_Project-main/`) tích hợp thông suốt với Backend SCMS (Sports Center Management System).
> **Nguyên tắc cốt lõi:** Bám sát tuyệt đối [AGENTS.md](file:///d:/Projects/SWP391/AGENTS.md) và tài liệu nghiệp vụ `Member.pdf`. Không tin tưởng dữ liệu Client, price freezing trên hóa đơn, pessimistic locking chống race condition, và toàn hệ thống đồng nhất múi giờ `Asia/Ho_Chi_Minh` (UTC+7).


> **Bản BE hai phase 27/10/2026:** cần migrations 06/07/08–12 trước BE. Mục 6–10 và 14 mô tả source mới; bằng chứng/hướng dẫn ở docs 02 mục 10. P1.5/gói T D01/D09/FE được Owner để lại. Demo account mục 1.3 chỉ dành seed local lịch sử; production bootstrap không tạo các tài khoản này.

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

#### Quy tắc đăng ký OTP

- Đăng ký mới trả `200` với `{ "message": "..." }`. Tài khoản Pending đăng ký lại chỉ phát OTP mới, giữ nguyên thông tin hiện có và trả `{ "message": "Tài khoản đang chờ xác thực; mã OTP mới đã được gửi.", "code": "REGISTRATION_PENDING" }`.
- Email Active bị từ chối với `400 { "code": "EMAIL_ALREADY_REGISTERED", "message": "..." }`.
- Đăng nhập Pending: mật khẩu đúng trả `400` với `REGISTRATION_PENDING`; mật khẩu sai vẫn trả `401 Invalid email or password`.
- OTP có hiệu lực 10 phút, tối đa 5 lần nhập sai, dùng một lần. Hết lượt phải yêu cầu OTP mới. Demo OTP chỉ bật bằng `REGISTRATION_DEMO_OTP`; chỉ áp dụng đăng ký.
- Gửi lại OTP tối đa 3 OTP trong một giờ và cách lần phát trước tối thiểu 60 giây. Lỗi `OTP_RESEND_LIMIT` hoặc `OTP_RESEND_TOO_SOON`; lỗi chờ gồm số giây còn lại trong `message` (ví dụ `OTP_RESEND_TOO_SOON: 42`). FE nên đếm ngược 60 giây trước khi bật nút gửi lại.
- Các lỗi xác thực OTP: `OTP_INVALID`, `OTP_EXPIRED`, `OTP_ATTEMPTS_EXCEEDED`. `forgot-password` luôn trả phản hồi chung dù email có tồn tại hay không.

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

### 2.2. Hai dòng sản phẩm — rule đang triển khai

| Thuộc tính | Normal | Advanced |
|---|---|---|
| Quy mô | Tối đa 15, 12 buổi hợp lệ | 1:1, 12 credit |
| Mua | Lớp có lịch cố định, giá Subject.normalPrice | Đăng ký môn/Primary Coach, nhận trước khi xuất Invoice |
| Lịch | Sinh nguyên khối 12 buổi, skip Holiday | Coach chính tạo, Manager/Admin hỗ trợ có lý do và consent |
| Hạn | Lịch khóa học | Từ Paid + 45 ngày sử dụng, skip thời gian TT đóng theo hợp các khoảng |
| Thành viên hủy | Cấm hủy/báo vắng | Hủy >=12h hoàn đúng 1 credit; báo vắng chỉ 0<notice<12h, không hoàn |
| Sự cố TT | Có chứng cứ buổi thực sự không tổ chức: liên kết 1 buổi bù | Hồ sơ sự cố, pause/khôi phục credit gốc; không cấp lại 12 |
| Attendance mới | Present/Absent trong [start,end) | Present/Absent trong [start,end), credit đã trừ khi book |

Paid là chốt cấp quyền. Pending 5 phút giữ cam kết năng lực, không giữ một ca cụ thể. Giá Invoice được lưu cứng; thay bảng giá không đổi hóa đơn cũ.

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

### 6.1. Đăng ký Advanced theo môn và Primary Coach — source mới 27/10

Luồng mới thay việc mua mẫu gói Advanced chung. Cần migrations 08/09 sau 06/07; chưa deploy, P1.5/gói T/FE được Owner để lại. Các endpoint dưới thuộc BE local; kết quả kiểm cuối xem docs 02.

1. Manager có `ROLE_CENTERMANAGER` hoặc `ROLE_ADMIN` và `MANAGE_CLASSES` cấu hình `PUT /api/coaches/{id}/work-schedule`:
   ```json
   {"weeklySlots":["MONDAY:1","WEDNESDAY:2","FRIDAY:7"],"daysOff":[],"acceptingNew":true}
   ```
   `GET` cùng đường dẫn trả `coachId/acceptingNew/weeklySlots/daysOff`; `GET /api/coaches/{id}/admission-capacity?subjectId=26` kiểm năng lực dự kiến và trả `availableSlots/outstandingCredits/requestedCredits`. Kiểm cả môn khác của Coach, credit chưa xếp, đơn Pending chưa hết hạn, sân và lịch đã giữ. Chọn Coach không giữ ca/sân.
2. Member gọi `POST /api/advanced-registrations` với `{"subjectId":26,"primaryCoachId":15,"requestKey":"purchase-unique-1"}`. Danh tính lấy từ phiên. `requestKey` 1–80 ký tự `[A-Za-z0-9_.:-]`; lặp cùng nội dung trả bản cũ, đổi nội dung trả 409 `IDEMPOTENCY_CONFLICT`.
3. Coach chính gọi `POST /api/advanced-registrations/{id}/decision` với `{"accepted":true}`. Manager hỗ trợ cùng center phải thêm `reason`. Chưa nhận thì chưa có Invoice, credit hoặc thời hạn đang chạy.
4. Member chủ yêu cầu gọi `POST /api/advanced-registrations/{id}/order` với `{"paymentMethod":"Cash"}` hoặc `VNPay`. BE kiểm lại năng lực dưới center lock rồi snapshot `Subject.advancedPrice`. Response là `subscriptionId/invoiceId/invoiceCode/amount/paymentMethod/status/expiresAt/paymentUrl`; `expiresAt` ở response đơn là hạn thanh toán 5 phút. Retry trả cùng hóa đơn; không tự mở lại đơn hết hạn.
5. Cash do Receptionist có `MANAGE_INVOICES` xác nhận; VNPay chỉ IPN hợp lệ cập nhật. Paid đúng hạn mới Active/12 credit. Return chỉ kiểm chữ ký/chuyển hướng; Member đọc Invoice qua API có xác thực để biết kết quả chính thức. IPN muộn ghi nhận Paid/cần đối soát và không cấp quyền.

`GET /api/advanced-registrations` trả các bản trong phạm vi Member chủ/Coach chính/Manager hoặc Admin cùng center. Field: `id/memberId/memberName/subjectId/subjectName/primaryCoachId/primaryCoachName/status/createdAt/acceptedBy/acceptedAt/reason/subscriptionId/subscriptionStatus/remainingCredit/activatedAt/expiresAt`. Trạng thái: Requested → Accepted → Invoiced, hoặc Requested → Rejected; Invoice là nguồn trạng thái tiền sau đó.

`GET /api/members/{memberId}/subscriptions` giữ kiểm chủ tài khoản. DTO thêm `subjectId/primaryCoachId/activatedAt/expiresAt`. `activatedAt/expiresAt` null trước Paid; sau Paid, deadline chính xác dùng 45 ngày sử dụng, hợp nhất thời gian TT đóng/nghỉ, không cộng bù trùng. Session phải kết thúc không vượt deadline. `startDate/endDate` chỉ là phần ngày tương thích, không thay timestamp. Field `price` lấy purchaseAmount đã đóng băng khi mua; legacy chỉ fallback metadata mẫu gói nếu có, có thể null. Chứng từ tiền chính thức là Invoice.amount.

Mua Advanced qua `POST /api/member-packages/subscribe-me` hoặc endpoint gói chung trả 400 `USE_ADVANCED_REGISTRATION`; không tự suy môn từ tên/giá của dữ liệu cũ. Các bản legacy thiếu môn/Coach/timestamp cần đối soát, không backfill từ amount. D01/D09 vẫn backlog riêng.

Lỗi chính: 401 chưa đăng nhập; 403 sai chủ/role/center; 404 không thấy bản; 400 `COACH_ACCEPTANCE_REQUIRED`, `COACH_NOT_ACCEPTING`, `COACH_SCHEDULE_REQUIRED`, `COACH_CAPACITY_INSUFFICIENT`, `COACH_SUBJECT_MISMATCH`, `MEMBER_NOT_ACTIVE`, `BOOKING_BLOCKED`, `INVALID_PRICE`; 409 xung đột idempotency/trạng thái. JSON request không nhận amount/actor/thời điểm xác nhận từ client.

### 6.2. Tra cứu ca và đặt lịch

`GET /api/coaches/{coachId}/available-slots?date=YYYY-MM-DD&subjectId=26&roomId=1` (alias `/api/sessions/coaches/...`) trả 7 ca với `slotNumber/slotName/startTime/endTime/isAvailable/reason`. Đây là tra cứu lịch tài nguyên; cấu hình làm việc lấy từ `/work-schedule`, quyền gói/deadline được kiểm lại lúc ghi. Chọn ca không giữ chỗ.

`POST /api/sessions/book-private` — Coach chính, hoặc Manager/Admin có MANAGE_CLASSES hỗ trợ cùng center. Member/Receptionist không tự tạo:

```json
{"subscriptionId":101,"coachId":15,"roomId":1,"sessionDate":"2026-11-02","startTime":"07:45:00","endTime":"09:15:00","subjectId":26,"agreementConfirmed":true,"requestKey":"booking-101-1","supportReason":null}
```

`classId` tùy chọn tương thích. BE kiểm Paid/Active, chủ Member active/không bị block, môn/Primary Coach, lịch làm việc, credit, >=12h, end<=expiresAt chính xác, 7 ca, Outdoor khóa 3/4, Holiday, phòng/Coach/Member không trùng. Center lock + subscription lock + transaction bảo đảm trừ đúng một credit. Retry cùng requestKey/nội dung trả ca cũ; đổi nội dung 409 IDEMPOTENCY_CONFLICT. Manager hỗ trợ bắt buộc supportReason. Consent phải true.

Response Session giữ field cũ; thêm `centerInvalidated/makeupForSessionId/creditReturned/createdBy/agreementConfirmedAt`. Không gửi amount/memberId/actor để thay danh tính hay giá. Ca Advanced không tạo/chuyển status/sửa thời gian qua CRUD chung; dùng workflow có audit.

### 6.3. Hủy, báo vắng và hết hạn

- `POST /api/sessions/{id}/cancel-private`: chỉ Member chủ, ca Scheduled, còn >=12h (bằng 12h hợp lệ). Cancelled và trả đúng 1 credit về gói gốc. Retry trả lỗi SESSION_ALREADY_CANCELLED, không hoàn lần hai. <12h: CANCEL_TOO_LATE. Normal luôn cấm.
- `POST /api/sessions/{id}/absence` body `{"reason":"..."}`: chỉ Member chủ Advanced trong 0<notice<12h. Trả notice riêng, không tạo Excused/Attendance, không hoàn credit. `GET /api/sessions/{id}/absence-notices` cho chủ/Coach được giao/Manager cùng center.
- Job expiry dùng expiresAt chính xác và Clock UTC+7; chụp expiryRevokedCredit rồi thu credit dư, không tự cấp lại khi reactivation. Legacy thiếu thời điểm gốc cần đối soát. No-show tiêu thụ buổi đã book, không trừ thêm credit; chế tài khóa 7 ngày giữ code nhưng default tắt.

## 7. LỊCH THAY ĐỔI VÀ SỰ CỐ TRUNG TÂM

### 7.1. Dời lịch có audit

`POST /api/sessions/{id}/reschedule` — Primary Coach hoặc Manager/Admin hỗ trợ; body:

```json
{"newDate":"2026-11-03","newStartTime":"07:45:00","newEndTime":"09:15:00","newRoomId":1,"reason":"Member đề nghị","cause":"MEMBER_EARLY","memberAgreed":true,"requestKey":"move-1"}
```

`MEMBER_EARLY`: ca cũ và mới đều >=12h. `MEMBER_EMERGENCY`: Manager/Admin, ca cũ còn 0<12h, ca mới >=12h, không hoàn credit. `CENTER_INCIDENT`: Manager/Admin, incidentId đúng hồ sơ, ca tương lai có thể dưới 12h. Tất cả kiểm ca chuẩn/tài nguyên/deadline/consent và audit trước-sau. `extendValidityDays` không cho cộng tùy ý; nếu ca bù vượt hạn cần `incidentId/extraValidityReason`, chỉ duyệt phần thời gian mở cửa tối thiểu tới cuối ca. Dời thực sự supersede notice cũ nhưng giữ lịch sử; no-op/đổi sân/Coach cùng giờ giữ notice.

### 7.2. Thay Coach

`POST /api/sessions/{id}/substitute-coach`: Manager/Admin MANAGE_CLASSES cùng center, body `{"newCoachId":16,"reason":"...","incidentId":1,"memberAgreed":true,"requestKey":"sub-1"}`. Kiểm chuyên môn/lịch làm việc/trùng lịch, consent riêng cho thay Coach. Không đổi Primary Coach trên gói.

### 7.3. Hồ sơ và phục hồi

Các API `/api/center-incidents` cần Manager/Admin + MANAGE_CLASSES, active và cùng center:

| Method/path | JSON / hành vi |
|---|---|
| GET | Danh sách hồ sơ theo center |
| POST | `sessionId,startedAt,reason,evidence,requestKey`; WaitingForRecovery; snapshot Member ảnh hưởng, chưa bù tự động |
| POST /{id}/resolve | `endedAt`; hợp nhất pause, tính lại hạn không bù trùng |
| POST /{id}/compensate | `reason,verifiedNotHeld:true`; xác minh buổi thực sự không tổ chức vì TT, đánh dấu centerInvalidated và bù đúng một lần |
| POST /{id}/normal-makeup | `date,startTime,endTime,roomId,coachId,reason`; buổi thay có makeupForSessionId, một bản cho buổi gốc |

Advanced hoàn vào gói gốc; nếu hết hạn giữ credit chờ, phục hồi từ revoked/deferred thực tế có chứng cứ, không reset 12. Hồ sơ báo sau buổi vẫn phải xác minh wholly unheld. Buổi gốc giữ lịch sử nhưng không tính vào đủ 12 Normal. Member nghỉ cá nhân không được dùng luồng này. Gia hạn Advanced qua API gói chung bị chặn USE_INCIDENT_RECOVERY.

## 8. HOLIDAY — PREVIEW RỒI CONFIRM

`GET /api/holidays` và `/active` giữ nguyên. Thay đổi cần Manager/Admin MANAGE_CLASSES:

1. `POST /api/holidays/preview` body `{"holidayDate":"2026-12-01","holidayName":"Bảo trì","tag":"CENTER_EVENT","active":true}` hoặc `{"holidayId":1,"active":false}`.
2. Response `previewToken/blocked/affectedSessionIds/reconciliationSubscriptionIds/deadlineChanges` (subscriptionId/oldDeadline/newDeadline). Nếu blocked, xử lý từng ca qua workflow sự cố/dời trước.
3. `POST /api/holidays` body tạo như trên, hoặc `PATCH /api/holidays/{id}/toggle` body `{"active":false}`; gửi header `X-Preview-Token` đúng preview gần nhất.

Thiếu preview: HOLIDAY_PREVIEW_REQUIRED; dữ liệu đổi:409 HOLIDAY_PREVIEW_STALE; tác động chưa xử lý:409 HOLIDAY_IMPACT_UNRESOLVED. Không tự hủy/dời hàng loạt. Tắt nghỉ tương lai tính rút hạn và chặn ca vượt hạn; phần TT đã thực sự đóng trong quá khứ được giữ. Không tạo nghỉ quá khứ để tự bù, dùng incident có chứng cứ. Holidays đang global; ảnh hưởng center khác bị chặn HOLIDAY_SCOPE_REVIEW_REQUIRED.

## 9. THANH TOÁN, BIÊN NHẬN VÀ ĐỐI SOÁT

- `POST /api/invoices/{id}/confirm-cash`: Receptionist + MANAGE_INVOICES, body tùy chọn `{"note":"..."}`. Chỉ Cash Pending còn đúng hạn/đủ điều kiện mới Paid và cấp quyền, retry không cấp lại.
- `POST /api/invoices/{id}/pay-online`: Member chủ, response `paymentUrl`. Invoice Pending có expiresAt 5 phút; không nhận giá từ client.
- `GET /api/payments/vnpay/return`: kiểm chữ ký, redirect FE, **chỉ đọc**. FE đọc `GET /api/members/me/invoices/{id}` để biết trạng thái tiền; status trên redirect không phải bằng chứng Paid.
- `GET /api/payments/vnpay/ipn`: chỉ callback chữ ký hợp lệ mới ghi Paid. Kiểm amount/reference/idempotency. Tiền đến sau hạn hoặc không còn đủ điều kiện: Paid + reconciliationRequired, không cấp quyền.
- `GET /api/invoices/{id}/receipt`: Member chủ hoặc staff có MANAGE_INVOICES cùng center; chỉ Paid. Response Invoice giữ amount gốc, thêm cashNote/resolutionStatus/cashConfirmationReversed. Sửa Cash không xóa chứng từ Paid ban đầu.

Các case `/api/payment-resolutions`: GET/POST cho Receptionist/Manager/Admin có MANAGE_INVOICES; approve/complete chỉ Manager/Admin cùng center:

| Method/path | JSON |
|---|---|
| POST | `{"invoiceId":1,"type":"Refund","reason":"..."}` hoặc CashCorrection; retry một case/type/invoice |
| POST /{id}/approve | Không body; Open→Approved hoặc Blocked nếu không đủ điều kiện; chưa ghi tiền hoàn |
| POST /{id}/complete | Refund: `{"evidence":"chứng từ bên ngoài","performedAt":"2026-11-01T10:00:00","moneyNotReceived":false}`; CashCorrection: `{"evidence":"...","moneyNotReceived":true}` |

Refund MVP chỉ toàn bộ tiền Paid cần đối soát/chưa cấp quyền; hoàn được thực hiện bên ngoài SCMS, thời gian receipt<=performedAt<=now. Không tự gọi gateway refund. CashCorrection chỉ Advanced Cash chưa nhận tiền thực, đủ 12 chưa dùng, không lịch kể cả đã hủy, không phụ thuộc/gia hạn/expiry/sự cố/hóa đơn khác; không áp dụng Normal. Complete kiểm lại điều kiện dưới lock, original amount/Paid giữ nguyên, quyền gói bị thu hồi; không tạo tiền hoàn giả. Response có id/invoiceId/type/status/amount/reason/evidence/outcomeNote/createdAt/approvedAt/performedAt/completedAt.

## 10. ATTENDANCE VÀ QR

- `GET /api/attendances/me/checkin-code` (alias `/checkin-code`, prefix `/api/attendance` cũng hỗ trợ): chỉ Member chủ. Response **đúng hai field** `{"payload":"123456","expiresInSeconds":17}`. TTL còn lại của bước hiện tại 1–30 giây. FE bỏ mã hết hạn/refetch khi foreground, không log token. D17 FE chưa browser verified.
- `POST /api/attendances/scan` body `{"memberId":20,"code":"123456","sessionId":999}`: staff/Coach có quyền, active/cùng center/Coach được giao, bắt buộc session. Member có quyền ca Paid. Chỉ nhận bước QR hiện tại, chống replay/rate; chỉ [start,end), không scan sớm hoặc check-in cửa.
- TrainingResult/điểm danh thủ công chỉ Coach được giao và trong giờ ca, record mới chỉ Present hoặc Absent. Lịch sử Late/Excused giữ nguyên. Finalizer sau end ghi Absent còn thiếu/Completed, không trừ credit lần hai và không tính buổi centerInvalidated.
- `POST /api/attendances/correct` body `{"attendanceId":1,"newState":"Present","reason":"..."}`: Manager/Admin MANAGE_CLASSES sau end, có audit, đồng bộ result, không đổi credit.
- Check-in cửa `/check-in` và `/check-out` giữ code nâng cấp, default `scms.lobby-checkin.enabled=false`; không dùng để thay attendance ca.

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
| `CANCEL_TOO_LATE` | 400 | Học viên bấm hủy buổi học 1:1 khi còn dưới 12 tiếng. | Toast cảnh báo đỏ: *"Không thể hủy lịch khi còn dưới 12 tiếng. Buổi học sẽ bị tiêu thụ nếu không tham gia."* |
| `ADVANCED_PACKAGE_EXPIRED` | 400 | Gói 1:1 đã quá expiresAt sau 45 ngày sử dụng. | Toast cảnh báo: *"Gói học 1:1 của bạn đã hết hạn sử dụng (45 ngày). Vui lòng mua gói mới hoặc liên hệ trung tâm để gia hạn."* |
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
| `NOTIFICATION_NOT_FOUND` | 404 | Thông báo không tồn tại hoặc không thuộc người đang đăng nhập. | Hiển thị thông báo đã bị xóa hoặc không còn khả dụng. |

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

Các quy tắc hiện hành và JSON nằm tại mục 6–10. Invoice.amount/purchaseAmount đóng băng, Paid mới cấp quyền, center/row locks bảo vệ ghi cạnh tranh. Không chuyển Advanced Active bằng status CRUD; không sửa/hủy ca đã mua qua CRUD chung. Generic Class CRUD không nhận giá thay Subject hoặc xóa lịch đã có history. Giữ code/tests booking-block nhưng default tắt. Mọi thời điểm nghiệp vụ dùng Clock Asia/Ho_Chi_Minh.

---

## 13. F-06/F-07 — đồng bộ schema Session ngày 23/10/2026

Không đổi endpoint, payload hoặc quyền. Session.memberPackage cần cột nullable public.sessions.subscription_id tham chiếu member_packages(subscription_id). DB hiện hữu dùng migration `07_session_subscription_postgres.sql`, sau 04/06; hai schema dựng mới đã đồng bộ, FK tạo sau bảng đích. Không chạy schema seed lên DB đang dùng.

Log Render bổ sung xác nhận truy vấn thiếu subscription_id lúc 08:28:28 UTC+7 ngày 23/10. Probe thật bắt đầu 19:23:02 UTC+7 tại https://scms-app-p1gy.onrender.com: Session list bốn vai trò và dashboard Manager đều 200; không tái hiện 500. Chưa có SHA deploy hay lịch sử sửa DB để nối các mốc. DB trong .env hiện có cột/FK/index, chưa xác nhận trùng DB Render. Xem bằng chứng trong `be-request-f06-f07.md` và HISTORY.

FE chỉ đóng F-06/F-07 sau khi đối chiếu SHA deploy và probe mới: Session list theo bốn vai trò, dashboard Manager, dữ liệu và phạm vi owner/assignment/center đúng. Test local chưa chứng minh lịch, đặt lớp và dashboard đã nghiệm thu Render.

Lưu ý quyền live: Coach mẫu có MANAGE_CLASSES và đọc dashboard được (200), khác seed workspace. Backend dashboard hiện chấp nhận VIEW_REPORTS/MANAGE_USERS/MANAGE_CLASSES. Chờ đối chiếu cấu hình quyền mong muốn; không tự thay role/permission để đạt status kỳ vọng.

## 14. Báo cáo và trạng thái bàn giao 27/10/2026

P1.0–P1.4 và BE P2 đã có source + kiểm local (chi tiết bằng chứng/command tại docs 02 mục 10). P1.5/gói T D01/D09/FE vẫn backlog theo Owner, không lấy notification/chat merge làm nghiệm thu FE. Các phần kế hoạch/quyết định lịch sử giữ tại docs 04; hợp đồng hiện hành là mục 6–10 ở trên. Migration 06/07/08–12 bắt buộc trước BE; source local không chứng minh Render đã deploy.

### 14.1. Báo cáo theo center và quyền

`GET /api/reports/dashboard`, `/members`, `/revenue?startDate=2026-10-01&endDate=2026-10-31&groupBy=month` chỉ Manager/Admin **có VIEW_REPORTS**, active cùng center. Coach không đọc tổng tài chính, kể cả có authority phụ. EndDate bao gồm cả ngày bằng khoảng [start,ngày kế tiếp), không mất phần giây lẻ cuối ngày.

Dashboard giữ field cũ và thêm moneyReceived/moneyRefunded/netReceived. Revenue giữ totalRevenue/totalTransactions/averageTransactionValue/revenueByPackage/revenueByMethod/timeline; thêm:

| Field | Ý nghĩa |
|---|---|
| moneyReceived | Tiền thực thu theo paymentDate, có tiền muộn cần đối soát; loại Cash được xác minh chưa nhận |
| moneyRefunded | Case Refund Completed theo performedAt thực tế, có chứng cứ; Approved chưa tính |
| netReceived | moneyReceived − moneyRefunded; có thể âm ở kỳ chỉ hoàn |
| cashCorrectionsRecorded | Giá trị CashCorrection ghi nhận trong kỳ completedAt, không phải tiền hoàn |
| totalRevenue | Gross Paid lịch sử để tương thích; không dùng thay ba số trên |

Revenue breakdown/timeline cũ vẫn theo gross Paid. FE tự chọn cách trình bày ba số, BE không ép layout.

Member report giữ totalMembers/activeMembers/expiredMembers/packageDistribution/monthlyGrowth và thêm neverEntitledMembers/previouslyEntitledMembersWithoutRights/expiredRegistrations/completedRegistrations/reconciliationRegistrations/reconciliationMembers. activeMembers đếm người có quyền hiện tại (Normal Paid còn nghĩa vụ hoặc Advanced đúng hạn có credit/ca hợp lệ); 0 credit còn ca vẫn có quyền. expiredMembers là alias nhóm đã từng có nhưng hiện hết quyền, không gom người chưa từng mua. Registrations đếm từng gói/lượt, Completed tách Expired; legacy thiếu dữ kiện tách reconciliation. Người có nhiều đăng ký chỉ đếm một lần trong chỉ số người.

### 14.2. Giới hạn kiểm chứng

Demo dùng đăng nhập HTTP thật vào BE local và DB assertions sau commit, có cả PostgreSQL. Callback VNPay được ký bằng khóa test cục bộ, không phải giao dịch gateway live. Không SMTP delivery/live gateway/SQL Server runtime hoặc trình duyệt FE từ kết quả này. Đọc hướng dẫn tái lập và sổ evidence trong docs 02 mục 10.

## 15. THÔNG BÁO TRONG ỨNG DỤNG

Tất cả endpoint yêu cầu đăng nhập và chỉ truy cập thông báo của user trong session. Member, Coach, Receptionist và Manager đều dùng được. Không có email, push hay WebSocket.

### 15.1. Danh sách thông báo

`GET /api/notifications/me?unreadOnly=false&type=SESSION_REMINDER&page=0&size=20`

Query params:

| Tên | Kiểu | Mặc định | Mô tả |
|---|---|---:|---|
| `unreadOnly` | boolean | `false` | Chỉ lấy thông báo chưa đọc khi `true`. |
| `type` | enum | — | Lọc theo một `NotificationType`. |
| `page` | integer | `0` | Trang, bắt đầu từ 0. |
| `size` | integer | `20` | Số mục mỗi trang, tối đa 100. |

Response là Spring `Page`:

```json
{
  "content": [
    {
      "id": 101,
      "type": "SESSION_RESCHEDULED",
      "title": "Buổi học được dời lịch",
      "message": "Buổi học Cầu lông đã được dời sang 28/10/2026, 07:45–09:15, Sân A2.",
      "relatedType": "SESSION",
      "relatedId": 5531,
      "isRead": false,
      "createdAt": "2026-10-22T09:15:00+07:00"
    }
  ],
  "number": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 15.2. Đếm và cập nhật trạng thái đọc

| Method | Endpoint | Thành công |
|---|---|---|
| `GET` | `/api/notifications/me/unread-count` | `200 { "unreadCount": 3 }` |
| `PATCH` | `/api/notifications/{id}/read` | `204 No Content` |
| `PATCH` | `/api/notifications/me/read-all` | `204 No Content` |

Thông báo không thuộc user hiện tại và ID không tồn tại đều trả `404` với mã `NOTIFICATION_NOT_FOUND`.

### 15.3. Các loại thông báo

| `NotificationType` | Ý nghĩa |
|---|---|
| `ENROLLMENT_CONFIRMED` | Thanh toán thành công và ghi danh lớp Normal. |
| `PRIVATE_BOOKING_CONFIRMED` | Đặt lịch học 1:1 thành công. |
| `PRIVATE_BOOKING_CANCELLED` | Hủy lịch 1:1; nội dung nêu trạng thái hoàn credit. |
| `SESSION_RESCHEDULED` | Lịch buổi học được dời. |
| `COACH_SUBSTITUTED` | HLV dạy buổi học được thay đổi. |
| `PAYMENT_SUCCESS` | Hóa đơn đã thanh toán. |
| `PAYMENT_EXPIRED` | Hóa đơn hết hạn giữ chỗ. |
| `PACKAGE_EXTENDED` | Gói 1:1 được gia hạn. |
| `PACKAGE_EXPIRING_SOON` | Gói 1:1 còn không quá 3 ngày. |
| `PACKAGE_EXPIRED` | Gói 1:1 hết hạn. |
| `CREDIT_LOW` | Số dư credit sau khi đặt buổi học còn không quá 2. |
| `NO_SHOW_RECORDED` | Buổi vắng/no-show đã được chốt. |
| `SESSION_REMINDER` | Nhắc lịch khoảng 24 giờ trước buổi học. |

FE có thể poll `GET /api/notifications/me/unread-count` mỗi 30–60 giây để cập nhật badge. Module không dùng WebSocket. Các bản ghi legacy giữ nguyên nội dung và cột `type`; các loại nghiệp vụ mới lưu trong `notification_type` và được API trả dưới tên trường `type`.

## 16. Chat hỗ trợ Member – Receptionist

Toàn bộ API dùng session đăng nhập hiện tại. Member chỉ đọc/ghi conversation của chính mình; truy cập conversation khác trả `404 CHAT_CONVERSATION_NOT_FOUND`. Receptionist và CenterManager cần quyền `HANDLE_CHAT`; Coach bị từ chối. Tin nhắn là text thuần, tối đa 2000 ký tự sau trim; FE phải render bằng text để tránh XSS.

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/chat/conversations` | Member lấy conversation `OPEN` hoặc tạo mới; body tùy chọn `{"subject":"Payment question"}`. |
| `GET` | `/api/chat/conversations/me/active` | Conversation đang mở của member; 404 `CHAT_ACTIVE_CONVERSATION_NOT_FOUND` nếu chưa có. |
| `GET` | `/api/chat/me/unread-count` | Tổng số tin chưa đọc trong các conversation của member: `{"unreadCount":2}`. |
| `GET` | `/api/chat/conversations/{id}/messages?afterId=0&limit=30` | Poll theo ID tăng dần; mặc định 30, giới hạn tối đa 50. |
| `POST` | `/api/chat/conversations/{id}/messages` | Gửi `{ "content":"Hello", "clientMessageId":"uuid" }`; ID client tùy chọn để retry an toàn. |
| `POST` | `/api/chat/conversations/{id}/read` | Đánh dấu đã đọc tới message mới nhất cho user hiện tại. |
| `GET` | `/api/chat/conversations?status=OPEN&assigned=true&unreadOnly=false&page=0&size=30` | Hộp thư staff, mới hoạt động gần nhất trước; `assigned=true` lọc hội thoại của người đang xem, `false` lọc hội thoại chưa được nhận; mỗi hàng kèm last message và unread của staff đang xem. |
| `POST` | `/api/chat/conversations/{id}/claim` / `/release` | Nhận hội thoại hoặc tự nhả hội thoại đã nhận. |
| `PATCH` | `/api/chat/conversations/{id}/close` / `/reopen` | Đóng/mở lại hội thoại. Reopen trả 409 nếu member đã có hội thoại khác đang mở. |

Message response gồm `id`, `conversationId`, `senderType`, `senderName`, `isMine`, `content`, `contentType`, `metadata`, `clientMessageId`, `createdAt`. Thời gian trả ISO-8601 cùng offset `+07:00`. Ví dụ: `{ "id":4021,"conversationId":88,"senderType":"STAFF","senderName":"Tran Thi Huong","isMine":false,"content":"Hello","createdAt":"2026-10-24T09:15:00+07:00" }`.

Các mã lỗi chính: `CHAT_CONVERSATION_NOT_FOUND` (404), `CHAT_ACTIVE_CONVERSATION_NOT_FOUND` (404), `CHAT_CONVERSATION_CLOSED` (409), `CHAT_RATE_LIMITED` (429, tối đa 20 tin/phút/người; response có `retryAfterSeconds` và header `Retry-After`), `CHAT_MESSAGE_EMPTY`, `CHAT_MESSAGE_TOO_LONG`, `CHAT_ALREADY_CLAIMED`, `CHAT_NOT_ASSIGNED_TO_ACTOR`, `CHAT_OPEN_CONVERSATION_EXISTS`. FE nên poll message mỗi 3–5 giây khi cửa sổ chat mở, unread count mỗi 30–60 giây, và gửi `clientMessageId` cho mỗi tin.

### Mở rộng chat AI

`ChatService` chỉ xử lý nghiệp vụ; thêm bean `ChatResponder` với `supports(ChatConversation)` và `respond(conversation, N tin gần nhất, memberUserId)`. `ChatResponderDispatcher` chạy nền và gọi responder đầu tiên hỗ trợ conversation khi `handler_type=AI`; lỗi hoặc timeout không rollback hay chặn response của tin member. Reply được lưu `sender_type=AI`, `sender_user_id=NULL`, cùng metadata tùy chọn (quy ước đề nghị: `model`, `latencyMs`, `sources`). Không có responder AI hoặc lời gọi dịch vụ ngoài được bật mặc định. `escalateToStaff(conversationId, reason)` chuyển handler về `STAFF`, lưu SYSTEM message và phát notification tới assigned staff hoặc các receptionist Active trong cùng center.

Chat notification types persisted in `notification_type`: `CHAT_MEMBER_MESSAGE` (member message or escalation to staff) and `CHAT_STAFF_REPLY` (staff/AI reply to member). Notifications are emitted only on that viewer's unread transition from zero to one; storage failures do not roll back the chat message.

## 17. Nhận diện runtime tại health — bổ sung vận hành 27/10

`GET /api/health` vẫnpublic vàtrảstatusUP/messagepong (keep-alive,khôngchứngnhậnDB/SMTPhealthy). Thêm `diagnosticsVersion:"1"`, `buildCommit` từRENDER_GIT_COMMIT(hayunknown),`databaseTargetFingerprint` SHA256 của targetHikari đangdùng (hayunavailable). Chỉexposehash/commit,khôngURL/username/password/exception;không mởkếtnốiDBtronghealthping.HeaderCache-Control:no-store. Mãbăm dùngcùngthuậttoánJDBCpreflight đểso target,khôngthaykiểmdữliệu/migration/schemahoặcaggregatehealth. NếuRenderchưa cófieldmới,instanceLivechưađượckiểmchứnglàbảnsourcechẩnđoánmới.

`GET /actuator/health` là health tổng: trả200 nếu UP,503 khi DOWN; expose tên thành phần và status (ví dụ db/mail/diskSpace), giữ show-details=never nên không có exception/URL/credential. /api/health tiếp tục là liveness/identity và không thay cổng health tổng.

Notification `relatedId` được đọc/trả dưới dạng JSON integer int64 (Java Long), tương thích cột legacy BIGINT và không cắt về32bit; ID nghiệp vụ hiện tại vẫn được tạo từ nguồn có kiểu Integer. Không đổi endpoint/payload field.
