# TIẾN ĐỘ DỰ ÁN & THIẾT KẾ API HỆ THỐNG SCMS
## SPORTS CENTER MANAGEMENT SYSTEM

---

## 📊 1. TỔNG QUAN TIẾN ĐỘ THEO CÁC FLOW NGHIỆP VỤ

| Flow Nghiệp Vụ | Phân Loại | Trạng Thái Backend & DB | Trạng Thái Frontend | Ghi Chú |
|---|:---:|:---:|:---:|---|
| **0. Auth & Admin Management** | Bắt buộc | ✅ Hoàn thành (100%) | ✅ Hoàn thành (100%) | Đăng nhập session, RBAC Matrix, Audit Log, Quản lý User |
| **Flow 1: User & Membership Management** | Bắt buộc | ✅ Hoàn thành (100%) | ✅ Hoàn thành (100%) | Quản lý học viên, nhân sự, gói tập, đăng ký & gia hạn |
| **Flow 2: Class Booking & Schedule Management** | Bắt buộc | ✅ Hoàn thành (100%) | ✅ Hoàn thành (100%) | Quản lý bộ môn, phòng, lớp, xếp lịch tự động, 5 Guards |
| **Flow 3: Payment & Report Management** | Bắt buộc | ✅ Hoàn thành (100%) | ✅ Hoàn thành (100%) | Hóa đơn, thu tiền, thanh toán, Dashboard & Báo cáo doanh thu |
| **Flow 4: Training & Attendance Management** | Bắt buộc | ✅ Hoàn thành (100%) | ✅ Hoàn thành (100%) | Điểm danh, check-in, giáo án, ghi nhận kết quả & đánh giá |
| **Flow 5 & 6: AI Workout & AI Assistant** | Mở rộng | ⏳ Kế hoạch đợt tới | ⏳ Chờ tích hợp API | AI gợi ý bài tập cá nhân & Chatbot tư vấn lịch tập/dịch vụ |

---

## 🛠️ 2. CHI TIẾT THIẾT KẾ API THEO TỪNG FLOW

### 🔐 0. Auth & Admin Management
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/auth/login` | `POST` | Public | Đăng nhập hệ thống (Session/Cookie) |
| `/api/auth/logout` | `POST` | Authenticated | Đăng xuất, hủy session |
| `/api/auth/me` | `GET` | Authenticated | Lấy thông tin user hiện tại + quyền |
| `/api/admin/users` | `GET, POST` | `Admin` | Quản trị tài khoản toàn hệ thống |
| `/api/admin/users/{id}/role` | `PATCH` | `Admin` | Gán vai trò cho tài khoản |
| `/api/admin/roles` | `GET, POST, PUT` | `Admin` | Quản lý danh sách vai trò & permissions |
| `/api/admin/audit-logs` | `GET` | `Admin`, `CenterManager` | Tra cứu lịch sử thao tác hệ thống |

---

### 🏋️ Flow 1: User & Membership Management
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/members` | `GET, POST` | `Manager`, `Receptionist` | Danh sách & tạo học viên mới tại quầy |
| `/api/members/{id}` | `GET, PUT` | `Manager`, `Receptionist`, `Member (Self)` | Xem & cập nhật thông tin học viên |
| `/api/members/{id}/status` | `PATCH` | `Manager`, `Receptionist` | Đổi trạng thái (`Active`, `Inactive`, `Locked`) |
| `/api/staff` | `GET, POST, PUT` | `Manager`, `Admin` | Quản lý nhân sự (Coach, Receptionist, Manager) |
| `/api/packages` | `GET, POST, PUT` | `Manager` (Viết), All (Đọc) | Quản lý danh mục gói tập |
| `/api/packages/{id}/status` | `PATCH` | `Manager` | Bật / Tắt gói tập (`Active` / `Inactive`) |
| `/api/members/{id}/subscriptions` | `GET, POST` | `Manager`, `Receptionist`, `Member` | Đăng ký gói tập / gia hạn gói |
| `/api/members/{id}/subscriptions/{subId}/cancel` | `POST` | `Manager`, `Receptionist` | Hủy gói tập của học viên |

---

### 📅 Flow 2: Class Booking & Schedule Management
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/subjects` | `GET, POST, PUT, DELETE` | `Manager` (Viết), All (Đọc) | CRUD bộ môn thể thao |
| `/api/rooms` | `GET, POST, PUT` | `Manager` (Viết), All (Đọc) | Quản lý phòng tập |
| `/api/rooms/{id}/status` | `PATCH` | `Manager` | Đổi trạng thái phòng (`Available`, `Maintenance`, `Closed`) |
| `/api/classes` | `GET, POST, PUT` | `Manager` | Quản lý khung lớp học |
| `/api/classes/{id}/coach` | `PATCH` | `Manager` | Phân công HLV cho lớp |
| `/api/classes/{id}/sessions` | `GET, POST` | `Manager` | Lấy danh sách hoặc sinh lịch học cụ thể |
| `/api/sessions/{id}` | `PUT, DELETE` | `Manager` | Điều chỉnh giờ/phòng của 1 buổi tập riêng lẻ |
| `/api/classes/{id}/enroll` | `POST` | `Member`, `Receptionist`, `Manager` | Đăng ký học viên vào lớp (kiểm tra `maxCapacity`) |
| `/api/classes/{id}/cancel-enroll` | `POST` | `Member`, `Receptionist`, `Manager` | Hủy đăng ký lớp |

---

### 💰 Flow 3: Payment & Report Management
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/invoices` | `GET, POST` | `Manager`, `Receptionist` | Lập hóa đơn khi mua gói tập/lớp |
| `/api/invoices/{id}` | `GET` | `Manager`, `Receptionist`, `Member (Self)` | Xem chi tiết & in hóa đơn |
| `/api/invoices/{id}/pay` | `POST` | `Receptionist`, `Member` | Thanh toán (Tiền mặt, Chuyển khoản, Thẻ) |
| `/api/reports/dashboard` | `GET` | `Manager` | Thống kê tổng quan KPI nhanh |
| `/api/reports/revenue` | `GET` | `Manager` | Báo cáo doanh thu theo mốc thời gian (ngày, tháng, quý) |
| `/api/reports/attendance` | `GET` | `Manager` | Báo cáo tỷ lệ tham gia lớp & check-in |

---

### 📋 Flow 4: Training & Attendance Management
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/attendances/check-in` | `POST` | `Receptionist` | Check-in cổng trung tâm (`session_id = NULL`) |
| `/api/sessions/{id}/attendances` | `GET, POST` | `Coach` | Điểm danh học viên theo buổi học cụ thể |
| `/api/training-plans` | `GET, POST, PUT` | `Coach` | Lập giáo án tập luyện cho lớp hoặc cá nhân |
| `/api/members/{id}/training-plans`| `GET` | `Coach`, `Member` | Xem lộ trình luyện tập |
| `/api/evaluations` | `POST, PUT` | `Coach` | Đánh giá định kỳ tiến độ của học viên |
| `/api/members/{id}/evaluations` | `GET` | `Coach`, `Member` | Xem nhận xét & điểm tiến độ |

---

### 🤖 Flow 5 & 6: AI Workout Recommendation & AI Assistant
| Endpoint | Method | Vai trò cho phép | Mô tả |
|---|---|---|---|
| `/api/ai/workout-recommendations` | `POST` | `Coach`, `Member` | AI phân tích mục tiêu + lịch sử tập để gợi ý bài tập |
| `/api/ai/consultation` | `POST` | `Member`, `Coach` | Chatbot AI giải đáp thắc mắc dịch vụ / lịch tập |
| `/api/ai/consultation/history` | `GET` | `Member`, `Coach` | Xem lịch sử các phiên hỏi đáp AI |

---

## 🛡️ 3. CÁC QUY TẮC BẢO MẬT & NGHIỆP VỤ CỐT LÕI (CORE GUARDS)

1. **Authentication & Session:**
   - Sử dụng Session Cookie (`JSESSIONID`) kèm cơ chế `allowCredentials = true`.
   - Cấu hình CORS tự động khớp mọi cổng local (`http://localhost:*`, `http://127.0.0.1:*`).
2. **5 Guards khi Đăng ký lớp học (Enrollment Guards):**
   - **Guard 1 - Member Active:** Học viên phải ở trạng thái `Active`.
   - **Guard 2 - Valid Package:** Học viên phải có gói tập còn hạn hiệu lực.
   - **Guard 3 - Class Capacity:** Kiểm tra sĩ số lớp hiện tại so với `max_capacity`.
   - **Guard 4 - Duplicate Check:** Ngăn chặn học viên đăng ký trùng lớp đã tham gia.
   - **Guard 5 - Schedule Conflict:** Kiểm tra trùng lịch với các buổi học khác của học viên.
3. **Attendance Correction Guard:**
   - Khi Huấn luyện viên hoặc Quản lý chỉnh sửa trạng thái điểm danh của buổi học đã qua, bắt buộc phải cung cấp `reason` giải trình và hệ thống tự động ghi nhật ký vào `system_logs`.
