# 01-API-CONTRACT-FE-BE.md — Hợp Đồng Kết Nối API Giữa Frontend & Backend

> **Mục tiêu:** Đây là tài liệu duy nhất và chính thức quy định toàn bộ Endpoints, Headers, Request Body, Response Schema, Bảng giá chuẩn 9 bộ môn, và Từ điển Mã lỗi (Error Codes Dictionary) để đội Frontend (`FE_Project-main/`) tích hợp thông suốt với Backend SCMS (Sports Center Management System).
> **Nguyên tắc cốt lõi:** Bám sát tuyệt đối [AGENTS.md](file:///d:/Projects/SWP391/AGENTS.md) và tài liệu nghiệp vụ `Member.pdf`. Không tin tưởng dữ liệu Client, price freezing trên hóa đơn, pessimistic locking chống race condition, và toàn hệ thống đồng nhất múi giờ `Asia/Ho_Chi_Minh` (UTC+7).

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

---

## 2. BẢNG GIÁ CHUẨN 9 BỘ MÔN THỂ THAO & HAI DÒNG SẢN PHẨM (ĐỐI CHIẾU MEMBER.PDF)

### 2.1. Ma Trận Giá 9 Môn Thể Thao
Theo quy định tại `Member.pdf` (Trang 7) và `AGENTS.md`, bảng giá chuẩn đã được nạp chết (seeded) vào bảng `subjects` của cơ sở dữ liệu:

| STT | Môn Thể Thao | Loại Sân / Phòng | Ca Học Cho Phép | Học Phí Khóa Normal (12 buổi nhóm cố định) | Giá Gói Khóa Advanced (1:1 linh hoạt 12 Credit) |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **1** | **Bóng đá (Soccer)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **900.000 VNĐ** | **8.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **2** | **Quần vợt (Tennis)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **1.500.000 VNĐ** | **10.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **3** | **Bơi lội (Swimming)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **1.200.000 VNĐ** | **9.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **4** | **Bắn cung (Archery)** | **Outdoor** | Ca 1, 2, 5, 6, 7 *(Khóa Ca 3 & 4)* | **1.400.000 VNĐ** | **9.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **5** | **Cầu lông (Badminton)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **900.000 VNĐ** | **7.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **6** | **Bóng bàn (Table Tennis)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **800.000 VNĐ** | **6.500.000 VNĐ** *(12 Credit / 45 ngày)* |
| **7** | **Bóng rổ (Basketball)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **1.000.000 VNĐ** | **8.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **8** | **Bóng chuyền (Volleyball)** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **900.000 VNĐ** | **7.000.000 VNĐ** *(12 Credit / 45 ngày)* |
| **9** | **Pickleball** | **Indoor** | Toàn bộ 7 ca (Ca 1 – Ca 7) | **1.100.000 VNĐ** | **8.500.000 VNĐ** *(12 Credit / 45 ngày)* |

### 2.2. So Sánh Đặc Tính Kỹ Thuật Hai Dòng Sản Phẩm

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
* **Quyền:** Public / Authenticated
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
    "userId": 3,
    "fullName": "Trần Thị Hương",
    "specializations": ["Cầu lông", "Pickleball"],
    "bio": "HLV cấp quốc gia với hơn 8 năm kinh nghiệm huấn luyện chuyên sâu",
    "certification": "Bằng HLV Quốc Tế BWF Cấp 2",
    "phone": null,
    "email": null
  }
]
```
*(Lưu ý: Trường `phone` và `email` chỉ được trả về khi người đăng nhập có quyền `MANAGE_USERS` nhằm bảo mật thông tin cá nhân).*

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
  "enrollmentId": 210,
  "status": "Pending",
  "invoiceId": 501,
  "invoiceCode": "INV-2026-000501",
  "amount": 900000.00,
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "expiresAt": "2026-10-20T16:05:00+07:00"
}
```
*(Cửa sổ giữ chỗ thanh toán chuẩn hóa là **5 phút**. Khi hóa đơn được thanh toán thành công, học viên chuyển sang `Registered` và được gán vào 12 buổi học).*

### 5.5. Chặn Hủy Khóa Normal (`NORMAL_CLASS_NON_CANCELLABLE`)
* **Endpoint:** `POST /api/enrollments/{enrollmentId}/cancel`
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
  "expiresAt": "2026-10-20T16:05:00+07:00",
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
* **Endpoint:** `POST /api/sessions/book-private`
* **Quyền:** `hasAnyRole('MEMBER', 'ADMIN', 'MANAGER')`
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
* **Endpoint:** `POST /api/sessions/{sessionId}/cancel-private`
* **Quyền:** `hasAnyRole('MEMBER', 'ADMIN', 'MANAGER')` (Member chỉ được hủy buổi của chính mình).
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
* **Endpoint:** `POST /api/invoices/{invoiceId}/pay-cash`
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

Tất cả các lỗi nghiệp vụ trả về từ Backend đều có cấu trúc JSON thống nhất:
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
| `PAYMENT_EXPIRED` | 400 | Đã quá 5 phút giữ chỗ thanh toán đơn hàng. | Chuyển hướng người dùng về giỏ hàng/danh mục: *"Đơn hàng giữ chỗ đã hết hạn (5 phút). Vui lòng thực hiện đặt lại."* |
| `HOLIDAY_BLOCKED` | 400 | Đặt lịch vào ngày nghỉ lễ/Tết đang được trung tâm kích hoạt. | Tô đỏ ô lịch ngày lễ: *"Trung tâm tạm đóng cửa nghỉ lễ trong ngày này. Vui lòng chọn ngày học khác."* |
| `SUBSTITUTE_COACH_CONFLICT` | 409 | HLV thay thế được chỉ định bị trùng lịch dạy khác. | Toast lỗi: *"HLV thay thế đã có lịch dạy khác trong khung giờ này. Vui lòng chọn HLV khác."* |
| `ROOM_HAS_FUTURE_SESSIONS` | 400 | Đổi trạng thái phòng sang Bảo trì/Đóng cửa khi còn ca học tương lai. | Toast lỗi: *"Phòng/Sân tập vẫn còn các buổi học đã xếp lịch trong tương lai. Vui lòng dời lịch các buổi học trước khi đóng sân."* |
| `INVALID_TIME_SLOT` | 400 | Giờ bắt đầu / kết thúc gửi lên không khớp 7 ca học chuẩn. | Toast lỗi: *"Khung giờ học không hợp lệ. Vui lòng chọn đúng 1 trong 7 ca học chuẩn của trung tâm."* |
| `NOTIFICATION_NOT_FOUND` | 404 | Thông báo không tồn tại hoặc không thuộc người đang đăng nhập. | Hiển thị thông báo đã bị xóa hoặc không còn khả dụng. |

---

## 12. THÔNG BÁO TRONG ỨNG DỤNG

Tất cả endpoint yêu cầu đăng nhập và chỉ truy cập thông báo của user trong session. Member, Coach, Receptionist và Manager đều dùng được. Không có email, push hay WebSocket.

### 12.1. Danh sách thông báo

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

### 12.2. Đếm và cập nhật trạng thái đọc

| Method | Endpoint | Thành công |
|---|---|---|
| `GET` | `/api/notifications/me/unread-count` | `200 { "unreadCount": 3 }` |
| `PATCH` | `/api/notifications/{id}/read` | `204 No Content` |
| `PATCH` | `/api/notifications/me/read-all` | `204 No Content` |

Thông báo không thuộc user hiện tại và ID không tồn tại đều trả `404` với mã `NOTIFICATION_NOT_FOUND`.

### 12.3. Các loại thông báo

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
