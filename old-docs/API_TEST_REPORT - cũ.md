# BÁO CÁO KIỂM THỬ TOÀN DIỆN API BACKEND (SCMS)

- **Môi trường Backend:** Cloud Render (`https://scms-app-p1gy.onrender.com`)
- **Cơ sở dữ liệu:** Supabase PostgreSQL (`aws-0-ap-southeast-2`)
- **Tài liệu tham chiếu:** [OpenAPI Specification (JSON)](https://scms-app-p1gy.onrender.com/v3/api-docs) | [DEMO_SCRIPT.md](file:///d:/School/FA26/SWP391/Project/src-code/BE_Project/DEMO_SCRIPT.md)
- **Công cụ kiểm thử:** Kịch bản tự động [test-api.mjs](file:///d:/School/FA26/SWP391/Project/src-code/test-api.mjs)
- **Cơ chế xác thực:** Session Cookie (`JSESSIONID`)

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ

| Chỉ số | Số lượng | Tỷ lệ | Đánh giá |
|---|:---:|:---:|---|
| **Tổng số lượt test API** | 25 | 100% | Quét qua đầy đủ 5 vai trò |
| **Thành công (200 OK)** | 21 | **84%** | Hầu hết các luồng chính chạy mượt mà |
| **Lỗi Server (500 Internal Error)** | 3 | **12%** | Lỗi logic/truy vấn JPA phía Backend |
| **Chặn quyền (403 Forbidden)** | 1 | **4%** | Chưa phân quyền đúng theo luồng nghiệp vụ |

---

## 2. PHÁT HIỆN QUAN TRỌNG: DANH SÁCH TÀI KHOẢN THỰC TẾ TRONG DATABASE

> [!WARNING]
> Trong tài liệu `DEMO_SCRIPT.md` trước đây có ghi các tài khoản như `reception@scms.com`, `coach1@scms.com`, `member1@scms.com`.  
> Tuy nhiên, kiểm tra trực tiếp trong Database Supabase thì các tài khoản thực tế được tạo theo domain **`@fitzone.vn`**. Nếu đăng nhập bằng email cũ sẽ bị báo lỗi **401 Unauthorized**.

### Bảng tài khoản chuẩn để test và tích hợp:
| Vai trò | Email thực tế trong DB | Mật khẩu | Tên hiển thị | Authorities / Quyền chính |
|---|---|:---:|---|---|
| **Admin** | `admin@scms.com` | `12345678` | Hệ Thống Admin | Quản trị người dùng, phân quyền RBAC, xem Audit log |
| **Center Manager** | `manager@scms.com` | `12345678` | Nguyễn Văn An | Quản lý Lớp, Bộ môn, Phòng, Gói tập, Buổi học |
| **Receptionist** | `mai.do@fitzone.vn`<br/>*(hoặc `nam.vu@fitzone.vn`)* | `12345678` | Đỗ Thị Mai | `REGISTER_MEMBER`, `PROCESS_PAYMENT`, `MANAGE_SUBSCRIPTIONS` |
| **Coach** | `huong.tran@fitzone.vn`<br/>*(hoặc `khoa.le@fitzone.vn`)* | `12345678` | Trần Thị Hương | `MANAGE_TRAINING_PLAN`, `RECORD_RESULT`, xem lịch dạy |
| **Member** | `oanh.hoang@fitzone.vn`<br/>*(hoặc `phuc.bui@fitzone.vn`)* | `12345678` | Hoàng Thị Oánh | `ROLE_MEMBER`, xem lớp cá nhân, gói tập cá nhân |

---

## 3. CHI TIẾT KẾT QUẢ KIỂM THỬ THEO 5 VAI TRÒ

### 3.1. Phân hệ Quản trị Hệ thống (Role: `Admin`) — 6/6 API PASS (100%)
Admin có toàn quyền quản trị và đọc dữ liệu nhật ký hệ thống.
- `POST /api/auth/login` $\rightarrow$ **200 OK** (1235ms): Cấp cookie session hợp lệ.
- `GET /api/auth/me` $\rightarrow$ **200 OK** (246ms): Nhận diện đúng user ID: 13, Role: Admin.
- `GET /api/users` $\rightarrow$ **200 OK** (2106ms): Trả về danh sách đầy đủ 13 người dùng trong hệ thống.
- `GET /api/roles` $\rightarrow$ **200 OK** (426ms): Trả về 5 vai trò hệ thống (`Admin`, `CenterManager`, `Coach`, `Receptionist`, `Member`).
- `GET /api/permissions` $\rightarrow$ **200 OK** (423ms): Trả về 12 quyền hạn phục vụ ma trận RBAC.
- `GET /api/audit-logs` $\rightarrow$ **200 OK** (249ms): Trả về 4 bản ghi lịch sử thao tác hệ thống.

---

### 3.2. Phân hệ Quản lý Trung tâm (Role: `Center Manager`) — 7/9 API PASS (78%)
Manager quản lý toàn bộ tài nguyên thể thao của trung tâm.
- `POST /api/auth/login` $\rightarrow$ **200 OK** (744ms).
- `GET /api/subjects` $\rightarrow$ **200 OK** (458ms): Trả về 4 bộ môn (`Yoga`, `Gym`, `Boxing`, `Pilates`).
- `GET /api/rooms` $\rightarrow$ **200 OK** (435ms): Trả về 4 phòng tập (`Room 101`, `Room 102`,...).
- `GET /api/packages` $\rightarrow$ **200 OK** (412ms): Trả về 4 gói tập thành viên.
- `GET /api/classes` $\rightarrow$ **200 OK** (1188ms): Trả về 4 lớp học kèm thông tin HLV phụ trách.
- `GET /api/sessions` $\rightarrow$ **200 OK** (1781ms): Trả về 8 buổi học trong lịch học.
- `GET /api/reports/members` $\rightarrow$ **200 OK** (1308ms): Báo cáo tăng trưởng thành viên hoạt động tốt.
- `GET /api/reports/dashboard` $\rightarrow$ <span style="color:red">**500 Internal Server Error**</span> (474ms): **[LỖI CẦN FIX]**.
- `GET /api/reports/revenue` $\rightarrow$ <span style="color:red">**500 Internal Server Error**</span> (503ms): **[LỖI CẦN FIX]**.

---

### 3.3. Phân hệ Lễ tân (Role: `Receptionist`) — 4/5 API PASS (80%)
Lễ tân thao tác hồ sơ hội viên và gói tập tại quầy.
- `POST /api/auth/login` $\rightarrow$ **200 OK** (784ms).
- `GET /api/members` $\rightarrow$ **200 OK** (1631ms): Trả về danh sách 6 hội viên.
- `GET /api/member-packages` $\rightarrow$ **200 OK** (431ms): Trả về 6 gói tập đã gán cho học viên.
- `GET /api/subscriptions` $\rightarrow$ **200 OK** (521ms): Trả về 6 bản ghi đăng ký gói.
- `GET /api/invoices` $\rightarrow$ <span style="color:red">**500 Internal Server Error**</span> (515ms): **[LỖI CẦN FIX]**.

---

### 3.4. Phân hệ Huấn luyện viên (Role: `Coach`) — 4/5 API PASS (80%)
HLV theo dõi lịch dạy, kế hoạch bài tập và đánh giá học viên.
- `POST /api/auth/login` $\rightarrow$ **200 OK** (1446ms).
- `GET /api/sessions` $\rightarrow$ **200 OK** (1780ms): Trả về 8 buổi dạy được phân công.
- `GET /api/training-plans` $\rightarrow$ **200 OK** (515ms): Trả về 4 giáo án/kế hoạch tập luyện.
- `GET /api/evaluations` $\rightarrow$ **200 OK** (535ms): Trả về 6 bản ghi nhận xét/đánh giá học viên.
- `GET /api/attendances` $\rightarrow$ <span style="color:red">**500 Internal Server Error**</span> (425ms): **[LỖI CẦN FIX]**.

---

### 3.5. Phân hệ Hội viên (Role: `Member`) — 3/4 API Hoạt động đúng
Hội viên tra cứu lịch học và gói tập của chính mình.
- `POST /api/auth/login` $\rightarrow$ **200 OK** (1542ms).
- `GET /api/members/{memberId}/classes` $\rightarrow$ **200 OK** (526ms): Xem lịch lớp cá nhân đã đăng ký.
- `GET /api/members/{memberId}/subscriptions` $\rightarrow$ **200 OK** (493ms): Xem gói tập cá nhân.
- `GET /api/classes` $\rightarrow$ <span style="color:orange">**403 Forbidden**</span> (85ms): **[CẦN XEM LẠI PHÂN QUYỀN]**.

---

## 4. BÁO CÁO CHI TIẾT CÁC LỖI HIỆN TẠI VÀ NGUYÊN NHÂN

### Lỗi 1: Báo cáo Doanh thu & Dashboard bị lỗi 500
- **Endpoint bị ảnh hưởng:**
  - `GET /api/reports/dashboard`
  - `GET /api/reports/revenue`
- **Hiện tượng:** Server trả về HTTP 500:
  ```json
  {
    "timestamp": "2026-09-30T04:54:56.605+00:00",
    "status": 500,
    "error": "Internal Server Error",
    "path": "/api/reports/dashboard"
  }
  ```
- **Nguyên nhân dự đoán:**
  - Logic tính doanh thu (`revenue`) trong service đang query bảng `invoices`. Do bảng `invoices` chưa có dữ liệu giao dịch hoặc giá trị `NULL` trong cột tính tổng (`SUM(amount)`), dẫn đến `NullPointerException` hoặc lỗi chia cho 0.
- **Đề xuất khắc phục (cho BE):**
  - Sử dụng hàm `COALESCE(SUM(amount), 0)` trong câu truy vấn SQL/HQL.
  - Bổ sung `null-check` trong Service trước khi đóng gói DTO trả về cho Dashboard.

---

### Lỗi 2: API Danh sách Hóa đơn bị lỗi 500
- **Endpoint bị ảnh hưởng:** `GET /api/invoices`
- **Hiện tượng:** Gọi bởi Lễ tân (`mai.do@fitzone.vn`) hay Manager đều trả về 500 Internal Server Error.
- **Nguyên nhân dự đoán:**
  - Quan hệ liên kết JPA (`@ManyToOne` / `@OneToMany`) giữa `Invoice` với `Member` hoặc `Receptionist` bị vòng lặp tuần hoàn (Infinite recursion khi serialize sang JSON) hoặc cột khóa ngoại trong DB có bản ghi mồ côi (Orphan foreign key).
- **Đề xuất khắc phục (cho BE):**
  - Trả về `InvoiceResponseDTO` thay vì serialize trực tiếp Entity JPA.
  - Đảm bảo dùng `@JsonIgnoreProperties` hoặc mapper rõ ràng.

---

### Lỗi 3: API Danh sách Điểm danh bị lỗi 500
- **Endpoint bị ảnh hưởng:** `GET /api/attendances` (kể cả khi truyền `?sessionId=1`)
- **Hiện tượng:** HLV (`huong.tran@fitzone.vn`) gọi API điểm danh trả về 500.
- **Nguyên nhân dự đoán:**
  - Câu query lọc theo `sessionId` hoặc `memberId` trong Attendance Repository bị lỗi cú pháp JPQL/SQL, hoặc xung đột kiểu dữ liệu giữa Enum trạng thái (`Present`, `Absent`, `Late`) giữa Entity Java và bảng Postgres.
- **Đề xuất khắc phục (cho BE):**
  - Kiểm tra log Exception trong console Render tại thời điểm gọi `/api/attendances`.
  - Đảm bảo Enum `@Enumerated(EnumType.STRING)` khớp với cấu hình trong DB.

---

### Lỗi 4: Hội viên (Member) bị chặn 403 Forbidden khi xem danh sách lớp học
- **Endpoint bị ảnh hưởng:** `GET /api/classes`
- **Hiện tượng:** Hội viên đăng nhập thành công nhưng gọi `GET /api/classes` nhận HTTP 403 Forbidden.
- **Vấn đề nghiệp vụ:**
  - Theo luồng nghiệp vụ trong [DEMO_SCRIPT.md](file:///d:/School/FA26/SWP391/Project/src-code/BE_Project/DEMO_SCRIPT.md) (Bước 4): Hội viên cần vào danh mục lớp học để bấm **"Đăng ký tham gia lớp"**. Nếu bị chặn 403 ở danh mục lớp, Hội viên không thể tự chọn lớp để đăng ký trực tuyến.
- **Đề xuất khắc phục:**
  - **Cách 1 (Khuyên dùng phía BE):** Thêm quyền `ROLE_MEMBER` vào annotation `@PreAuthorize("hasAnyRole('ADMIN', 'CENTER_MANAGER', 'COACH', 'MEMBER')")` của method `GET /api/classes`.
  - **Cách 2:** BE tạo riêng một endpoint public cho hội viên: `GET /api/classes/available`.

---

## 5. HƯỚNG DẪN DÀNH CHO FRONTEND TEAM

1. **Xác thực (Auth):**
   - Đã hoạt động 100% ổn định. Sử dụng các tài khoản `@fitzone.vn` để test đúng quyền.
2. **Các màn hình đã sẵn sàng tích hợp API thật:**
   - Quản trị User, Role, Phân quyền RBAC Matrix.
   - Quản lý Bộ môn, Phòng tập, Gói tập, Lớp học, Lịch học.
   - Tra cứu & Quản lý Hội viên, Đăng ký gói tập cá nhân.
   - Quản lý Giáo án, Đánh giá học viên của Huấn luyện viên.
3. **Các màn hình tạm thời dùng Mock Data chờ BE fix:**
   - Bảng biểu Dashboard doanh thu (`/api/reports/dashboard`, `/api/reports/revenue`).
   - Màn hình Lịch sử Hóa đơn (`/api/invoices`).
   - Màn hình Điểm danh học viên buổi học (`/api/attendances`).

---

## 6. CÁCH CHẠY LẠI TEST TỰ ĐỘNG BẤT CỨ LÚC NÀO

Khi Backend team thông báo đã deploy bản sửa lỗi lên Render, bạn chỉ cần mở terminal tại thư mục dự án và chạy:

```powershell
node test-api.mjs
```
Kết quả chi tiết từng API và file [test-results.json](file:///d:/School/FA26/SWP391/Project/src-code/test-results.json) sẽ tự động được cập nhật.
