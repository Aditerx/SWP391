# 00-MASTER-CHECKLIST.md — Kế Hoạch & Tiến Độ Triển Khai SCMS

Tài liệu này là chốt chặn quản lý tiến độ tổng thể và lịch sử bàn giao của dự án Sports Center Management System (SCMS) theo đúng chuẩn `Member.pdf` và `AGENTS.md`.

---

## 📌 PHẦN I: TIẾN ĐỘ THỰC HIỆN (ROADMAP)

### Giai đoạn 1: Chuẩn Hóa Cơ Sở Dữ Liệu & Migration
- [x] Tạo file migration SQL độc lập `database/migrations/04_migration_member_flow_v2.sql`.
- [x] Thêm cột bảng `subjects`: `normal_price`, `advanced_price` và seed bảng giá chuẩn 9 môn.
- [x] Thêm cột bảng `rooms`: `facility_type` (`Indoor`, `Outdoor`).
- [x] Thêm cột bảng `classes`: `class_type` (`Normal`, `Advanced`), `total_sessions` (default 12), ràng buộc `max_capacity` ($\le 15$ cho Normal, $= 1$ cho Advanced).
- [x] Thêm cột bảng `membership_packages`: `package_type`, `total_credit` (default 12).
- [x] Thêm cột bảng `member_packages`: `remaining_credit`, `validity_days` (default 45).
- [x] Bổ sung bảng `sessions`: `booked_member_id`, `coach_id`, `subscription_id` (hỗ trợ đặt lịch 1:1).
- [x] Tạo bảng `holidays` và nạp danh mục ngày nghỉ lễ/tết cố định và âm lịch Việt Nam.
- [x] Cập nhật các Entity Java tương ứng: `Subject`, `Room`, `SportsClass`, `MembershipPackage`, `MemberPackage`, `Session`, `Holiday`.

---

### Giai đoạn 2: Quy Chuẩn 7 Ca Học & Ràng Buộc Thời Tiết
- [x] Định nghĩa Enum `TimeSlot` chuẩn hóa 7 Ca học (90 phút/ca, nghỉ trưa 11:00–13:00).
- [x] Tạo `SessionSlotValidator` kiểm tra giờ học gửi lên bắt buộc phải khớp đúng với 1 trong 7 ca chuẩn.
- [x] Ràng buộc môn Ngoài trời (Outdoor): Tự động khóa Ca 3 (09:30–11:00) và Ca 4 (13:00–14:30) để chống sốc nhiệt/nắng gắt.
- [x] Tạo API `GET /api/sessions/time-slots` trả về danh sách ca học cho Frontend.
- [x] Bộ Unit Test toàn diện: `SessionSlotValidatorTest` (26 test cases) phủ 100% các kịch bản ca học, nghỉ trưa và ngoài trời.

---

### Giai đoạn 3: Nghiệp Vụ Khóa Normal (Lớp Học Nhóm 12 Buổi Cố Định)
- [x] Tạo Lớp học Normal (`POST /api/classes`):
  - [x] Tự động lấy giá chuẩn `Subject.normal_price` gán vào học phí lớp.
  - [x] Kiểm tra sĩ số `max_capacity <= 15`.
- [x] Thuật toán Sinh Lịch Tĩnh 12 Buổi & Bỏ qua Ngày Lễ (Holiday Skipper):
  - [x] API Preview lịch học (`POST /api/sessions/preview-recurring`): Nhận các thứ trong tuần (VD: T2-4-6), tự động bỏ qua ngày lễ có `is_active = true`, trả về danh sách 12 buổi học và danh sách ngày lễ được bỏ qua.
  - [x] API Xác nhận sinh lịch (`POST /api/sessions/generate`): Lưu 12 buổi học vào DB, tự động cập nhật ngày kết thúc của lớp học theo buổi thứ 12.
- [x] Luồng Đăng ký & Chặn Hủy:
  - [x] Đăng ký thành công $\rightarrow$ Gán Member vào danh sách lớp và 12 buổi học tĩnh với Pessimistic Lock chống Race Condition.
  - [x] Cập nhật `EnrollmentService`: Chặn tuyệt đối nút Hủy đối với lớp Normal (`NORMAL_CLASS_NON_CANCELLABLE`).
  - [x] Module quản lý Ngày Lễ: Tạo `HolidayService` & `HolidayController` (`GET /api/holidays`, `PATCH /api/holidays/{id}/toggle`).
  - [x] Điểm danh vắng $\rightarrow$ Tiêu thụ buổi học (trừ 1 credit / 1 buổi), không học bù.
- [x] Bộ Unit Test toàn diện: `SportsClassServiceTest` (4 tests), `EnrollmentServiceTest` (24 tests), `SessionServiceTest` (11 tests).

---

### Giai đoạn 4: Nghiệp Vụ Khóa Advanced (Học 1:1 Linh Hoạt Bằng 12 Credit)
- [x] Luồng mua gói: Thanh toán thành công $\rightarrow$ Nạp 12 Credit vào `member_packages`, kích hoạt thời hạn 45 ngày đếm ngược từ lúc thanh toán.
- [x] Tra cứu & Đặt lịch linh hoạt (Flexible Booking):
  - [x] API `GET /api/coaches/{coachId}/available-slots`: Lấy ca rảnh của Coach môn đó (loại trừ Ca 3, Ca 4 nếu môn Outdoor, chặn ngày trong quá khứ hoặc ngày lễ).
  - [x] API Member book ca học (`POST /api/sessions/book-private`): Kiểm tra còn credit, còn hạn 45 ngày, đặt trước $\ge 12h$, khóa dòng DB chống trùng ca.
  - [x] Tạo Session 1:1 (`max_capacity = 1`, 90 phút, gán `booked_member_id`, `coach_id`, `subscription_id`).
- [x] Hủy lịch & No-show:
  - [x] Member hủy trước $\ge$ 24h (`POST /api/sessions/{sessionId}/cancel-private`) $\rightarrow$ Hoàn trả 1 Credit, session chuyển `Cancelled`.
  - [x] Member hủy dưới 24h $\rightarrow$ Backend từ chối với lỗi `CANCEL_TOO_LATE: Không thể hủy lịch khi còn dưới 24 tiếng. Buổi học sẽ bị tiêu thụ nếu không tham gia`.
  - [x] Buổi học diễn ra, nếu Member không tới $\rightarrow$ HLV/Cronjob điểm danh "Vắng mặt" (No-show) $\rightarrow$ Tiêu thụ 1 Credit (bảo vệ thù lao HLV), không hoàn, không sinh lịch bù.
- [x] Cronjob 00:00 hàng đêm (`PackageExpirationJob`): Quét các gói 1:1 quá 45 ngày $\rightarrow$ Chuyển `Expired`, hủy credit thừa (`remainingCredit = 0`).
- [x] Bộ Unit Test toàn diện: `PrivateSessionServiceTest` (13 tests), `MemberPackageServiceTest` (2 tests), `InvoiceServiceTest` (9 tests). Toàn bộ 161 tests trên toàn hệ thống pass (146 passed, 15 skipped, 0 fail).

---

### Giai đoạn 5: Background Jobs & Xử Lý Sự Cố
- [x] Xử lý sự cố Trung tâm (Mưa bão, HLV nghỉ):
  - [x] API dời buổi học do sự cố (`POST /api/sessions/{sessionId}/reschedule`): Manager dời sang ngày/ca/phòng mới. Nếu dời vượt quá hạn 45 ngày của gói 1:1 $\rightarrow$ Tự động gia hạn `endDate` và `validityDays`.
  - [x] API chỉ định HLV thay thế (`POST /api/sessions/{sessionId}/substitute-coach`): Phân công HLV khác khi HLV phụ trách nghỉ ốm/đột xuất, kiểm tra trùng lịch.
  - [x] API gia hạn thời hạn gói 1:1 (`POST /api/member-packages/{id}/extend`): Manager chủ động đền bù thêm ngày hiệu lực cho hội viên.
- [x] Điều chỉnh thời gian giữ chỗ thanh toán trong `PaymentProperties` thành 5 phút (chuẩn hóa `SCMS_PAYMENT_EXPIRE_MINUTES = 5`).
- [x] Background Cronjob:
  - [x] `PackageExpirationJob`: Quét 00:00 hàng đêm các gói quá 45 ngày, chuyển `Expired` và vô hiệu hóa credit thừa.
  - [x] `PaymentExpirationJob`: Quét mỗi 60s các hóa đơn quá 5 phút chưa thanh toán, hủy giữ chỗ.
  - [x] `AttendanceFinalizationJob`: Quét mỗi 10 phút chốt điểm danh vắng mặt / no-show cho buổi học đã kết thúc.
- [x] Chuẩn bị kịch bản và tài liệu deploy Render + test live VNPay Sandbox (`docs/02-DEPLOYMENT-GUIDE.md`).
- [x] Bộ Unit Test toàn diện: `SessionIncidentServiceTest` (12 tests), `MemberPackageExtensionTest` (3 tests), `PackageExpirationJobTest` (2 tests). Toàn bộ 178 tests trên toàn hệ thống pass (163 passed, 15 skipped, 0 fail).

---

### Giai đoạn 6: Tích Hợp Giao Diện Frontend (FE)
- [x] Bàn giao & Chuẩn hóa hợp đồng API toàn diện `docs/01-API-CONTRACT-FE-BE.md` cho FE team:
  - [x] Nâng cấp DTOs Backend phục vụ tra cứu danh mục: `SubjectResponse` (thêm `normalPrice`, `advancedPrice`, `outdoor`), `MembershipPackageResponse` (thêm `packageType`, `totalCredit`), `RoomResponse` (thêm `facilityType`, `outdoor`), `SportsClassResponse` (thêm `subjectName`, `coachName`).
  - [x] Biên soạn đầy đủ Bảng giá 9 môn thể thao chuẩn `Member.pdf` & Từ điển mã lỗi chuẩn (Error Codes Dictionary).
  - [x] Kiểm thử hồi quy Backend 100% pass (178 tests run, 163 passed, 15 skipped, 0 fail).
- [ ] Cập nhật `MemberClassCatalog.jsx`: Lọc theo 9 bộ môn thật, chia 2 tab: Khóa Nhóm (Normal) & Khóa 1:1 (Advanced).
- [ ] Màn hình Quản lý Lớp (`ClassManagement.jsx`): Form chọn môn tự đổ giá, chọn T2-4-6, nút Preview 12 buổi học.
- [ ] Màn hình Quản lý Ngày Lễ (Holidays) cho Manager: Xem danh sách theo Tag (`NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`), Toggle bật/tắt ngày nghỉ.
- [ ] Màn hình Member Đặt lịch 1:1: Xem số dư Credit, xem lịch rảnh Coach, chọn ca học và đặt lịch.

---

### Giai đoạn 7: Kiểm Thử Toàn Diện & Bàn Giao
- [x] Bổ sung Unit Test cho: Xử lý sự cố Dời lịch, Thay HLV & Gia hạn gói (`SessionIncidentServiceTest`, `MemberPackageExtensionTest`).
- [x] Bổ sung Unit Test cho: Background Cronjob Expiration (`PackageExpirationJobTest`).
- [x] Bổ sung Unit Test cho: 1:1 Credit Booking & Cancellation (`PrivateSessionServiceTest` - 13 tests).
- [x] Bổ sung Unit Test cho: Holiday Skipper & Auto Price Freeze & Capacity Constraint.
- [x] Bổ sung Unit Test cho: Slot Validator 7 ca chuẩn & Outdoor Heat Policy (26 tests).
- [x] Chạy kiểm tra hồi quy: `.\mvnw.cmd test` đảm bảo 100% tests pass (178 tests run, 163 passed, 15 skipped, 0 fail).

### Giai đoạn 8: Thông Báo Trong Ứng Dụng
- [x] Mở rộng tương thích ngược `notifications` và `notification_recipients`; giữ dữ liệu và luồng legacy.
- [x] API danh sách, lọc, đếm chưa đọc, đánh dấu một/tất cả đã đọc; giới hạn theo user đăng nhập.
- [x] Event listener chạy AFTER_COMMIT trong transaction riêng; lỗi lưu thông báo không ảnh hưởng nghiệp vụ chính.
- [x] Tích hợp 13 loại sự kiện, job nhắc lịch 24 giờ và job gói 1:1 sắp hết hạn.
- [x] Bổ sung test service, controller/role, listener và job với Clock cố định.
- [x] Cập nhật hợp đồng API và hướng dẫn migration.
- [x] Chạy hồi quy `.\mvnw.cmd clean test`: 193 tests run, 178 passed, 15 skipped, 0 failed.
- [ ] Chạy migration 05 trên PostgreSQL DB sạch và DB có notifications legacy để xác nhận thực tế.

---

## 📜 PHẦN II: LỊCH SỬ HOÀN THÀNH (HISTORY)

### [Đợt 22/10/2026] — Giai đoạn 8: Thông Báo Trong Ứng Dụng
* **Trạng thái:** Code và kiểm thử Backend hoàn tất; cần xác nhận migration trên PostgreSQL mới và có dữ liệu cũ.
* **Thay đổi:** Mở rộng schema notification tương thích ngược, API cá nhân, 13 loại event, job nhắc lịch và gói sắp hết hạn; FE không thay đổi.
* **Kiểm thử:** 193 tests run, 178 passed, 15 skipped, 0 failed, 0 errors (`.\mvnw.cmd clean test`).
* **DB:** Migration được rà cú pháp/idempotence; chưa chạy trên PostgreSQL vì môi trường hiện tại không có `psql`, Docker hay kết nối DB được cấp.

### [Đợt 20/10/2026] — Chuẩn Hóa DTOs Danh Mục & Hoàn Thiện Hợp Đồng API Frontend (Sẵn Sàng Giai Đoạn 6)
* **Trạng thái:** Hoàn tất 100%, 178 tests run (163 passed, 15 skipped, 0 fail, 0 error).
* **Các hạng mục đã hoàn thiện:**
  1. **Nâng cấp DTOs & Service Backend:**
     - `SubjectResponse` & `SubjectRequest`: Bổ sung `normalPrice`, `advancedPrice`, `slug`, `status`, `outdoor`.
     - `MembershipPackageResponse` & `MembershipPackageRequest`: Bổ sung `packageType`, `totalCredit`.
     - `RoomResponse` & `RoomRequest`: Bổ sung `facilityType` (`Indoor`/`Outdoor`), `outdoor`.
     - `SportsClassResponse`: Bổ sung `subjectName`, `coachName` (tránh N+1 roundtrips cho Frontend).
  2. **Hoàn thiện tài liệu `docs/01-API-CONTRACT-FE-BE.md`:**
     - Mục 1: Quy chuẩn giao tiếp, Base URL, UTC+7, luồng xác thực (Auth flow: login, register, OTP, reset password).
     - Mục 2: Ma trận giá 9 môn thể thao chuẩn hóa theo `Member.pdf` (Trang 7) và bảng so sánh đặc tính kỹ thuật Normal vs Advanced.
     - Mục 3: Nhóm API 7 Ca học chuẩn (90 phút/ca) và quy chế khóa Ca 3 & Ca 4 cho môn ngoài trời chống sốc nhiệt.
     - Mục 4: Nhóm API Danh mục tra cứu (`/api/subjects`, `/api/packages`, `/api/classes`, `/api/rooms`, `/api/coaches`).
     - Mục 5 - 10: Quy chuẩn API Lớp Normal (Holiday Skipper, non-cancellable), Lớp Advanced (12 credit, 45 ngày, 24h cutoff, No-show), Xử lý sự cố (Reschedule, Substitute Coach, Extend Validity), Holidays, Thanh toán (VNPay & Cash, 5 phút giữ chỗ), và Check-in TOTP QR.
     - Mục 11: Bảng từ điển mã lỗi chuẩn (Error Codes Dictionary) hướng dẫn chi tiết cách Frontend hiển thị toast/alert/modal cho người dùng.
  3. **Kiểm thử hồi quy:** Chạy lệnh `.\mvnw.cmd test` đạt 100% pass (178 run, 163 passed, 15 skipped).

### [Đợt 20/10/2026] — Giai đoạn 5: Background Jobs & Xử Lý Sự Cố Trung Tâm (Mưa Bão, HLV Nghỉ, Chuẩn Bị Deploy Render)
* **Trạng thái:** Hoàn tất 100%, 178 tests run (163 passed, 15 skipped, 0 fail, 0 error).
* **Các tính năng đã hoàn thiện:**
  1. **Xử lý sự cố Dời lịch học (`POST /api/sessions/{sessionId}/reschedule`):** Hỗ trợ Manager dời buổi học của lớp Normal hoặc ca 1:1 do thời tiết xấu (mưa bão ngập sân), sự cố cơ sở vật chất. Kiểm tra 7 ca chuẩn, chống sốc nhiệt môn outdoor, chống trùng phòng/HLV/học viên. Nếu buổi học 1:1 bị dời vượt quá hạn 45 ngày của gói học viên $\rightarrow$ Backend tự động kéo dài ngày hết hạn `endDate` và cập nhật `validityDays` tương ứng, chuyển lại `Active` nếu gói đang bị `Expired`. Với lớp Normal, tự động kéo dài `SportsClass.endDate`.
  2. **Xử lý sự cố HLV nghỉ đột xuất (`POST /api/sessions/{sessionId}/substitute-coach`):** Cho phép Manager chỉ định HLV thay thế khi HLV phụ trách nghỉ ốm, việc đột xuất. Kiểm tra người thay thế có vai trò HLV và không bị trùng lịch ca học đó. Ghi nhận nhật ký kiểm toán (Audit Log).
  3. **Chủ động Gia hạn gói hội viên (`POST /api/member-packages/{id}/extend`):** Cho phép Manager bù thêm ngày hiệu lực cho học viên bị ảnh hưởng bởi bảo trì dài hạn hoặc sự cố trung tâm.
  4. **Chuẩn hóa thời gian giữ chỗ thanh toán:** Điều chỉnh `PaymentProperties` và `application.properties` chuẩn hóa về đúng `5 phút` (`SCMS_PAYMENT_EXPIRE_MINUTES = 5`).
  5. **Background Jobs toàn diện:**
     - `PackageExpirationJob`: Chạy 00:00 hàng đêm quét các gói quá 45 ngày, chuyển `Expired` và vô hiệu hóa số credit thừa còn lại.
     - `PaymentExpirationJob`: Chạy mỗi 60 giây quét các hóa đơn quá 5 phút chưa thanh toán, tự động hủy giữ chỗ và giải phóng slot.
     - `AttendanceFinalizationJob`: Chạy mỗi 10 phút quét và chốt điểm danh vắng mặt / no-show cho các ca học đã kết thúc.
  6. **Cập nhật Hướng Dẫn Triển Khai Render & Kịch bản Test VNPay Sandbox (`docs/02-DEPLOYMENT-GUIDE.md`):** Bổ sung đầy đủ thông tin thẻ NCB demo, kịch bản test luồng thanh toán và IPN callback, cấu hình Supabase Connection Pooler, và checklist trước/sau deploy.
  7. **Cập nhật Hợp Đồng API (`docs/01-API-CONTRACT-FE-BE.md`):** Thêm chi tiết nhóm API Xử lý sự cố trung tâm (Mục 5) và cập nhật thời gian giữ chỗ thanh toán 5 phút.

### [Đợt 20/10/2026] — Giai đoạn 4: Nghiệp Vụ Khóa Advanced (Học 1:1 Linh Hoạt 12 Credit & 45 Ngày)
* **Trạng thái:** Hoàn tất 100%, 161 tests run (146 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 3: Nghiệp Vụ Khóa Normal (Lớp Học Nhóm 12 Buổi Cố Định)
* **Trạng thái:** Hoàn tất 100%, 145 tests run (130 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 2: Quy Chuẩn 7 Ca Học & Ràng Buộc Thời Tiết
* **Trạng thái:** Hoàn tất 100%, 138 tests run (123 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 1: Chuẩn Hóa CSDL & Migration Flow V2
* **Trạng thái:** Hoàn tất 100%. File migration: `database/migrations/04_migration_member_flow_v2.sql`.
