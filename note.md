
---

### I. ĐÁNH GIÁ THAY ĐỔI, CẬP NHẬT GIỮA `FE-BE.md` VÀ HỆ THỐNG THỰC TẾ

File [`FE-BE.md`]là **bản ghi chép giai đoạn đầu** (mang tính thảo luận chung trước khi chuẩn hóa). Nếu Frontend code theo các mô tả cũ trong file này sẽ gặp **7 điểm lệch** so với Backend:

| # | Điểm thiết kế | Bản cũ trong `FE-BE.md` | Nghiệp vụ theo (`AGENTS.md` & `01-API-CONTRACT-FE-BE.md`) |
|---|---|---|---|
| **1** | **Thời gian giữ chỗ thanh toán** |Ghi 15 phút (`PAYMENT_EXPIRE_MINUTES: 15`). | **Chuẩn hóa là 5 phút** (`scms.payment.expire-minutes = 5`). |
| **2** | **Chính sách Hủy lớp học** | Cho phép hủy lịch học trước giờ học 2 tiếng. | **Khóa Normal (12 buổi cố định): CẤM HỦY / BÁO VẮNG** (`NORMAL_CLASS_NON_CANCELLABLE`). Vắng tự chịu, không hoàn tiền/credit. |
| **3** | **Hai dòng sản phẩm (Normal vs 1:1)** | Chỉ mô tả chung chung mua gói tập theo tháng/năm. | Phân định rõ 2 luồng: <br>• **Normal:** Nhóm $\le 15$ người, 12 buổi cố định.<br>• **Advanced (1:1):** Nạp 12 Credit, hạn 45 ngày, tự book từng ca theo lịch rảnh HLV (sĩ số = 1). |
| **4** | **Quy chuẩn Khung giờ & Thời tiết** | Giờ học tự do (chỉ cần cách nhau $\ge 30$ phút). | **7 Ca học chuẩn (90 phút/ca)**. Đặc biệt môn Ngoài trời (Outdoor) **bắt buộc khóa Ca 3 (09:30–11:00) và Ca 4 (13:00–14:30)** để chống sốc nhiệt. |
| **5** | **Thuật toán Sinh lịch & Ngày Lễ** | Chỉ nhân lịch theo thứ trong tuần, không quản lý lễ. | Bảng `holidays` (3 tag: `NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`). Thuật toán tự động nhảy qua ngày lễ (Holiday Skipper) để sinh đủ 12 buổi. |
| **6** | **Múi giờ vận hành (Timezone)** | Ghi BE lưu UTC và FE tự parse sang UTC+7. | **Toàn bộ hệ thống ép cứng `Asia/Ho_Chi_Minh` (UTC+7)** qua Spring bean `Clock`. Dữ liệu trao đổi là chuẩn ISO theo giờ Việt Nam. |
| **7** | **Quy tắc Hủy lịch 1:1 (24h Cutoff)** | Cho hủy trước 2 tiếng. | **Hủy $\ge 24h$:** Hoàn 1 Credit.<br>**Hủy $< 24h$ hoặc No-show:** Tiêu thụ 1 Credit (bảo vệ thù lao HLV). |

> [!IMPORTANT]
> **Khuyến nghị:** file [`FE-BE.md`] nên được coi là tài liệu nháp lịch sử. Đội Frontend nền bám sát hợp đồng chính thức duy nhất là **[`docs/01-API-CONTRACT-FE-BE.md`]

---

### II. Chi Tiết Nội Dung Cập Nhật Trong [01-API-CONTRACT-FE-BE.md]

Hợp đồng API đã được chuẩn hóa thành **11 mục rõ ràng**, bao quát toàn bộ hệ thống:

1. **Mục 1 — Quy Chuẩn Giao Tiếp Chung & Luồng Xác Thực (Auth Flow):**
   - Base URL (Local `localhost:8080`, Production Render `https://...`).
   - Múi giờ chuẩn `Asia/Ho_Chi_Minh` (UTC+7), Session Cookie `withCredentials: true`.
   - Chi tiết các API Auth: Đăng nhập (`POST /api/auth/login`), Đăng ký (`POST /api/auth/register`), Xác thực OTP (`POST /api/auth/verify-registration`), Gửi lại OTP, Quên mật khẩu, Đặt lại mật khẩu.
2. **Mục 2 — Ma Trận Giá 9 Môn Thể Thao Chuẩn Hóa (`Member.pdf` Trang 7):**
   - Đối chiếu đầy đủ 9 môn: Bóng đá, Tennis, Bơi lội, Bắn cung (Outdoor: Khóa Ca 3 & 4) và Cầu lông, Bóng bàn, Bóng rổ, Bóng chuyền, Pickleball (Indoor: Mở cả 7 ca).
   - Bảng so sánh rành mạch: Học phí Khóa Normal (12 buổi nhóm) vs Giá Gói Khóa Advanced (12 Credit / 45 ngày).
3. **Mục 3 — Nhóm API 7 Ca Học Chuẩn & Chống Sốc Nhiệt:**
   - `GET /api/sessions/time-slots` (có query `isOutdoor=true` tự loại trừ Ca 3 & 4).
4. **Mục 4 — Nhóm API Danh Mục & Tra Cứu (Catalog & Lookups):**
   - `GET /api/subjects`, `GET /api/packages`, `GET /api/classes`, `GET /api/rooms`, `GET /api/coaches` (kèm danh sách chuyên môn `specializations`), `GET /api/coaches/{coachId}/available-slots`.
5. **Mục 5 — Lớp Học Nhóm Normal (12 Buổi Tĩnh):**
   - `POST /api/classes` (tự đổ giá `normalPrice`, sĩ số $\le 15$).
   - `POST /api/sessions/preview-recurring` (thuật toán Holiday Skipper xem trước 12 buổi học).
   - `POST /api/sessions/generate` (lưu 12 buổi học, cập nhật `endDate`).
   - `POST /api/classes/{id}/enroll` (giữ chỗ 5 phút).
   - Chặn hủy: `POST /api/enrollments/{id}/cancel` trả lỗi `NORMAL_CLASS_NON_CANCELLABLE`.
6. **Mục 6 — Lớp 1:1 Linh Hoạt Bằng 12 Credit (Advanced):**
   - Mua gói `POST /api/member-packages/subscribe-me`, xem credit `GET /api/member-packages/me`.
   - Xem ca rảnh HLV `GET /api/coaches/{id}/available-slots`.
   - Book ca 1:1 `POST /api/sessions/book-private` (đặt trước $\ge 12h$, khóa dòng DB).
   - Hủy ca 1:1 `POST /api/sessions/{id}/cancel-private` (hủy $\ge 24h$ hoàn 1 credit; hủy $< 24h$ trả `CANCEL_TOO_LATE` và mất credit).
   - No-show trừ 1 credit, Cronjob 00:00 quét hết hạn 45 ngày.
7. **Mục 7 — Xử Lý Sự Cố Trung Tâm (Mưa bão, HLV nghỉ, gia hạn gói):**
   - `POST /api/sessions/{id}/reschedule` (dời lịch, tự kéo dài `endDate` và `validityDays` nếu dời quá hạn 45 ngày).
   - `POST /api/sessions/{id}/substitute-coach` (chỉ định HLV thay thế, ghi Audit Log).
   - `POST /api/member-packages/{id}/extend` (chủ động bù ngày sử dụng cho học viên).
8. **Mục 8 — Quản Lý Ngày Nghỉ Lễ (Holidays):**
   - `GET /api/holidays`, `PATCH /api/holidays/{id}/toggle` (Bật/tắt ngày nghỉ theo `NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`).
9. **Mục 9 — Thanh Toán (VNPay Sandbox & Tiền Mặt Tại Quầy):**
   - Thu tiền mặt `POST /api/invoices/{id}/pay-cash`, VNPay URL `POST /api/invoices/{id}/pay-online` (giữ chỗ chuẩn 5 phút).
10. **Mục 10 — Điểm Danh & TOTP QR Check-in:**
    - Lấy mã TOTP 30s `GET /api/attendance/checkin-code`, Quét mã `POST /api/attendance/scan`.
11. **Mục 11 — Bảng Tra Cứu Mã Lỗi Chuẩn Toàn Hệ Thống (Error Codes Dictionary):**
    - Quy định rõ mã lỗi, HTTP status, ý nghĩa nghiệp vụ và hướng dẫn cách Frontend hiển thị toast / modal / alert cho từng trường hợp (`NORMAL_CLASS_NON_CANCELLABLE`, `CANCEL_TOO_LATE`, `ADVANCED_PACKAGE_EXPIRED`, `OUT_OF_CREDIT`, `ADVANCED_BOOKING_TOO_LATE`, `OUTDOOR_HEAT_BLOCK`, `ROOM_CONFLICT`, `COACH_CONFLICT`, `CLASS_CAPACITY_EXCEEDED`, `PAYMENT_EXPIRED`, `HOLIDAY_BLOCKED`, `SUBSTITUTE_COACH_CONFLICT`, `ROOM_HAS_FUTURE_SESSIONS`, `INVALID_TIME_SLOT`).

---

### III. Tổng hợp lại toàn bộ API cùng mô tả chức năng
* **Swagger UI Trực Tuyến:** [https://scms-app-p1gy.onrender.com/swagger-ui/index.html#/]
* **Base URL:** `https://scms-app-p1gy.onrender.com` (Production Render) hoặc `http://localhost:8080` (Local Dev)
* **Quy chuẩn trao đổi:** JSON UTF-8, Múi giờ `Asia/Ho_Chi_Minh` (UTC+7), Session Cookie `withCredentials: true`.

#### 1. Nhóm Xác Thực & Hồ Sơ Cá Nhân (Auth & Profile)
* `POST /api/auth/login`: Đăng nhập hệ thống bằng email và mật khẩu (trả về Cookie `JSESSIONID`).
* `GET /api/auth/me`: Lấy thông tin chi tiết tài khoản hiện tại (id, email, fullName, role, permissions).
* `POST /api/auth/logout`: Đăng xuất tài khoản, hủy session trên server.
* `POST /api/auth/change-password`: Đổi mật khẩu tài khoản đang đăng nhập.
* `POST /api/auth/register`: Đăng ký tài khoản hội viên mới (trạng thái ban đầu `Pending`, sinh OTP).
* `POST /api/auth/verify-registration`: Gửi mã OTP 6 số để kích hoạt tài khoản `Pending` sang `Active`.
* `POST /api/auth/resend-otp`: Gửi lại mã OTP qua email (hạn chế tối đa 3 lần/tiếng).
* `POST /api/auth/forgot-password`: Yêu cầu gửi mã OTP đặt lại mật khẩu về email.
* `POST /api/auth/reset-password`: Đặt mật khẩu mới kèm mã OTP xác nhận.
* `POST /api/users/me/avatar`: Upload và cập nhật ảnh đại diện cá nhân (Multipart qua Cloudinary).
* `DELETE /api/users/me/avatar`: Xóa ảnh đại diện cá nhân.

#### 2. Nhóm Quản Lý Người Dùng & Nhân Sự (User & Staff Management)
* `GET /api/users`: Quản lý danh sách người dùng toàn hệ thống (phân trang, tìm kiếm theo email/tên/role).
* `GET /api/users/{id}`: Xem chi tiết một người dùng theo ID.
* `POST /api/users`: Quản lý tạo người dùng nội bộ mới (`CenterManager`, `Receptionist`, `Coach`).
* `PUT /api/users/{id}`: Cập nhật thông tin người dùng.
* `PATCH /api/users/{id}/status`: Khóa hoặc kích hoạt tài khoản người dùng (`Active`/`Inactive`).
* `PATCH /api/users/{id}/role`: Phân lại vai trò cho người dùng.
* `POST /api/users/{id}/reissue-password`: Cấp lại mật khẩu tạm thời ngẫu nhiên cho nhân sự/học viên.
* `GET /api/staff`: Quản lý danh sách nhân viên trung tâm.
* `POST /api/staff`: Tạo hồ sơ nhân viên mới kèm hợp đồng, ngày vào làm.
* `PUT /api/staff/{id}`: Cập nhật hồ sơ nhân viên.
* `PATCH /api/staff/{id}/status`: Chuyển trạng thái công tác của nhân viên.
* `GET /api/roles`: Danh sách tất cả vai trò (Roles) trong hệ thống.
* `GET /api/roles/{id}`: Chi tiết một vai trò kèm danh sách quyền hạn.
* `PUT /api/roles/{id}/permissions`: Cập nhật ma trận phân quyền động cho vai trò.
* `GET /api/permissions`: Danh mục toàn bộ quyền năng tĩnh trong hệ thống.

#### 3. Nhóm Quản Lý Hội Viên (Member Management)
* `GET /api/members`: Danh sách hội viên (hỗ trợ lọc, tìm kiếm theo tên, số điện thoại, email).
* `GET /api/members/{id}`: Xem hồ sơ chi tiết hội viên, ngày tham gia, thông tin sức khỏe.
* `POST /api/members`: Lễ tân tạo hồ sơ hội viên trực tiếp tại quầy.
* `POST /api/members/register`: Đăng ký nhanh hội viên tại quầy.
* `PUT /api/members/{id}`: Cập nhật thông tin hồ sơ hội viên.
* `PATCH /api/members/{id}/status`: Bật/khóa trạng thái hội viên.
* `GET /api/members/at-risk`: Báo cáo danh sách hội viên có nguy cơ bỏ tập (Churn Risk).
* `PATCH /api/members/{id}/booking-block/lift`: Quản lý chủ động mở khóa quyền đặt lịch trước hạn cho hội viên bị phạt do vắng nhiều.

#### 4. Nhóm Danh Mục Môn Thể Thao & Giá Chuẩn (Subjects Catalog)
* `GET /api/subjects`: Danh mục 18 môn thể thao kèm giá chuẩn `normalPrice`, `advancedPrice` và cờ `outdoor`.
* `GET /api/subjects/{id}`: Chi tiết môn thể thao.
* `POST /api/subjects`: Thêm môn thể thao mới vào hệ thống.
* `PUT /api/subjects/{id}`: Cập nhật thông tin môn, giá chuẩn hoặc quy định trong nhà/ngoài trời.
* `DELETE /api/subjects/{id}`: Xóa môn thể thao (chặn xóa nếu đang có lớp học gắn với môn này).
* `GET /api/public/subjects`: Tra cứu danh mục môn học công khai (không yêu cầu đăng nhập).

#### 5. Nhóm Ca Học Chuẩn & Huấn Luyện Viên (Time Slots & Coaches)
* `GET /api/sessions/time-slots`: Danh sách 7 ca học chuẩn 90 phút (truyền `isOutdoor=true` để tự loại bỏ Ca 3 và Ca 4 chống sốc nhiệt).
* `GET /api/coaches`: Danh sách Huấn luyện viên kèm danh mục chuyên môn thể thao (`specializations`).
* `GET /api/coaches/{coachId}/available-slots`: Lấy danh sách các ca rảnh thực tế của HLV trong khoảng ngày để phục vụ đặt lịch 1:1.
* `GET /api/coaches/me/at-risk-members`: HLV xem danh sách học viên do mình phụ trách có nguy cơ bỏ tập.
* `GET /api/specializations`: Danh mục chuyên môn thể thao của Huấn luyện viên.
* `POST /api/specializations`: Tạo chuyên môn mới.
* `PUT /api/specializations/{id}`: Sửa chuyên môn.
* `PATCH /api/specializations/{id}/status`: Đổi trạng thái chuyên môn (`Active`/`Inactive`).

#### 6. Nhóm Lớp Học Nhóm Cố Định (Normal Classes - 12 Buổi Tĩnh)
* `GET /api/classes`: Danh sách các lớp học nhóm (lọc theo môn, HLV, trạng thái mở đăng ký).
* `GET /api/classes/{id}`: Chi tiết lớp học, sĩ số hiện tại/tối đa (<= 15), học phí chuẩn đã lưu chết.
* `POST /api/classes`: Tạo lớp học nhóm mới (tự động áp giá `normalPrice`, giới hạn `maxCapacity <= 15`).
* `PUT /api/classes/{id}`: Cập nhật thông tin lớp học.
* `PATCH /api/classes/{id}/coach`: Phân công hoặc đổi Huấn luyện viên phụ trách lớp.
* `POST /api/sessions/preview-recurring`: Thuật toán Holiday Skipper xem trước 12 buổi học dự kiến và danh sách ngày lễ được tự động bỏ qua trước khi lưu.
* `POST /api/sessions/generate`: Xác nhận sinh và lưu tĩnh 12 buổi học vào CSDL, tự động cập nhật ngày kết thúc (`endDate`).
* `POST /api/classes/{classId}/enroll`: Hội viên đăng ký vào lớp nhóm, tạo hóa đơn giữ chỗ trong 5 phút kèm Database Lock.
* `POST /api/classes/{classId}/cancel-enrollment` (hoặc `DELETE /api/enrollments/{id}`): Chặn hủy lớp Normal (`NORMAL_CLASS_NON_CANCELLABLE`, học viên vắng tự chịu).
* `GET /api/classes/{classId}/enrollments`: Danh sách học viên đã đăng ký vào lớp học.
* `GET /api/members/{memberId}/enrollments`: Lịch sử các lớp học viên đã tham gia.

#### 7. Nhóm Gói Hội Viên & Khóa Học 1:1 Linh Hoạt (Advanced - 12 Credit)
* `GET /api/packages`: Danh sách gói hội viên & gói 1:1 (kèm `packageType`, `totalCredit`).
* `POST /api/packages`: Quản lý tạo gói hội viên mới.
* `PUT /api/packages/{id}`: Sửa thông tin gói hội viên.
* `PATCH /api/packages/{id}/status`: Đổi trạng thái gói hội viên.
* `GET /api/public/membership-packages`: Tra cứu danh sách gói công khai.
* `POST /api/members/me/subscriptions` (hoặc alias `POST /api/member-packages/subscribe-me`): Hội viên mua gói 1:1 (nhận 12 Credit, kích hoạt hạn 45 ngày).
* `GET /api/members/{memberId}/packages` (hoặc alias `/subscriptions`): Xem danh sách gói, số dư Credit còn lại và ngày hết hạn.
* `POST /api/sessions/book-private`: Hội viên đặt lịch ca học 1:1 theo giờ rảnh HLV (sĩ số 1:1, trừ 1 credit, yêu cầu trước >= 12h, khóa dòng chống tranh chấp).
* `POST /api/sessions/{sessionId}/cancel-private`: Hội viên hủy ca học 1:1 (hủy trước >= 24h hoàn 1 credit; hủy dưới 24h từ chối với lỗi `CANCEL_TOO_LATE` và tiêu thụ 1 credit).
* `POST /api/sessions/{sessionId}/absence`: Học viên báo vắng mặt buổi học 1:1 (tiêu thụ 1 credit, bảo vệ thù lao HLV).

#### 8. Nhóm Quản Lý Buổi Học & Xử Lý Sự Cố (Sessions & Incidents)
* `GET /api/sessions`: Tra cứu danh sách buổi học (lọc theo phòng, HLV, lớp, ngày).
* `GET /api/sessions/{id}`: Chi tiết buổi học.
* `POST /api/sessions`: Tạo buổi học đơn lẻ thủ công.
* `PUT /api/sessions/{id}`: Cập nhật thông tin ca học.
* `PATCH /api/sessions/{id}/status`: Cập nhật trạng thái ca học (`Scheduled`, `InProgress`, `Completed`, `Cancelled`).
* `POST /api/sessions/check-conflict`: Kiểm tra trùng lịch phòng và HLV `(A_Start < B_End AND A_End > B_Start)`.
* `POST /api/sessions/{sessionId}/reschedule`: Quản lý dời lịch buổi học do sự cố (mưa bão, cúp điện; tự động kéo dài ngày hết hạn nếu là ca 1:1 dời quá 45 ngày).
* `POST /api/sessions/{sessionId}/substitute-coach`: Quản lý chỉ định HLV dạy thay khi HLV chính ốm đột xuất (kiểm tra trùng lịch và ghi Audit Log).
* `POST /api/member-packages/{id}/extend`: Quản lý chủ động gia hạn bù ngày sử dụng cho gói hội viên.
* `DELETE /api/sessions/{id}`: Xóa buổi học (chỉ cho phép khi buổi học chưa diễn ra).

#### 9. Nhóm Quản Lý Ngày Nghỉ Lễ (Holidays Management)
* `GET /api/holidays`: Danh sách toàn bộ ngày nghỉ lễ theo 3 nhóm chuẩn (`NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`).
* `GET /api/holidays/active`: Danh sách các ngày lễ đang được Bật (`is_active = true`).
* `POST /api/holidays`: Quản lý thêm ngày nghỉ đột xuất hoặc sự kiện riêng của trung tâm.
* `PATCH /api/holidays/{id}/toggle`: Quản lý Bật/Tắt một ngày nghỉ lễ (khi Tắt, thuật toán sinh 12 buổi sẽ xếp lớp bình thường vào ngày này).

#### 10. Nhóm Quản Lý Cơ Sở Vật Chất & Sân Bãi (Rooms & Facilities)
* `GET /api/rooms`: Danh sách sân/phòng tập (kèm loại hình `Indoor`/`Outdoor`, sức chứa, thiết bị).
* `POST /api/rooms`: Thêm phòng/sân tập mới.
* `PUT /api/rooms/{id}`: Sửa thông tin sân tập.
* `PATCH /api/rooms/{id}/status`: Đổi trạng thái sân tập (`Active`, `Maintenance`, `Closed`).

#### 11. Nhóm Điểm Danh & Mã QR Check-in TOTP (Attendance & Check-in)
* `GET /api/members/me/checkin-code` (hoặc alias `GET /api/attendance/checkin-code`): Hội viên lấy mã TOTP 6 số (chuẩn RFC 6238, đếm ngược 30s) để hiển thị mã QR trên điện thoại.
* `POST /api/attendance/scan`: Lễ tân hoặc Huấn luyện viên quét mã QR từ màn hình học viên để ghi nhận điểm danh tức thì.
* `POST /api/attendance/check-in`: Lễ tân/HLV điểm danh thủ công bằng tay.
* `POST /api/attendance/check-out`: Điểm danh ra về cho học viên.
* `POST /api/attendance/correct`: Điều chỉnh lại trạng thái điểm danh khi có sai sót.
* `GET /api/attendance`: Tra cứu lịch sử điểm danh theo buổi, học viên, trạng thái (`Present`, `Absent`, `CheckedIn`).

#### 12. Nhóm Đánh Giá & Kế Hoạch Tập Luyện (Training & Feedback)
* `GET /api/classes/{classId}/feedback`: Danh sách nhận xét, đánh giá của học viên về lớp học.
* `GET /api/classes/{classId}/feedback/me`: Xem đánh giá của chính mình cho lớp học.
* `POST /api/classes/{classId}/feedback`: Học viên gửi đánh giá sao và nhận xét cho lớp/HLV sau khi hoàn thành.
* `GET /api/coaches/me/feedback`: Huấn luyện viên xem tổng hợp đánh giá về bản thân.
* `GET /api/training-plans`: Danh mục giáo trình/kế hoạch bài tập huấn luyện.
* `POST /api/training-plans`: HLV tạo giáo trình huấn luyện mới.
* `PUT /api/training-plans/{id}`: Sửa giáo trình.
* `DELETE /api/training-plans/{id}`: Xóa giáo trình.
* `GET /api/training-results`: Xem kết quả tập luyện thể chất của học viên.
* `POST /api/training-results`: Ghi nhận chỉ số thể chất (chiều cao, cân nặng, sức bền).
* `POST /api/training-results/bulk`: Nhập kết quả tập luyện hàng loạt cho nhiều học viên.
* `GET /api/evaluations`: Danh sách phiếu đánh giá tiến bộ định kỳ của học viên.
* `POST /api/evaluations`: HLV tạo đánh giá tiến bộ cho học viên.
* `PUT /api/evaluations/{id}`: Sửa đánh giá.
* `DELETE /api/evaluations/{id}`: Xóa đánh giá.

#### 13. Nhóm Hóa Đơn & Thanh Toán (Invoices & Payment Gateways)
* `GET /api/invoices`: Quản lý/Thu ngân xem danh sách hóa đơn toàn hệ thống (lọc theo `Pending`, `Paid`, `Cancelled`).
* `GET /api/invoices/{id}`: Chi tiết hóa đơn (lưu chết số tiền, hạn giữ chỗ chuẩn 5 phút).
* `GET /api/members/me/invoices`: Hội viên xem lịch sử hóa đơn cá nhân.
* `GET /api/members/me/invoices/{id}`: Hội viên xem chi tiết một hóa đơn của mình.
* `POST /api/invoices/{id}/confirm-cash` (hoặc alias `pay-cash`): Quầy thu ngân xác nhận đã thu tiền mặt -> Hóa đơn chuyển `Paid`, kích hoạt suất học/gói tập.
* `POST /api/invoices/{id}/pay-online`: Khởi tạo cổng thanh toán online VNPay Sandbox -> Trả về `paymentUrl` hiển thị mã VNPAY-QR to ở giữa màn hình.
* `GET /api/payments/vnpay/return`: Endpoint VNPay chuyển hướng học viên quay về sau khi hoàn tất thanh toán.
* `GET /api/payments/vnpay/ipn`: Webhook Server-to-Server tiếp nhận kết quả thanh toán từ VNPay.
* `POST /api/invoices/{id}/cancel`: Hủy hóa đơn đang ở trạng thái `Pending`.

#### 14. Nhóm Báo Cáo Doanh Thu, Kiểm Toán & Giám Sát (Reports & System)
* `GET /api/reports/dashboard`: Thống kê KPI tổng quan trung tâm (doanh thu ngày/tháng, số hội viên mới, số lớp mở, tỷ lệ lấp đầy).
* `GET /api/reports/revenue`: Báo cáo chi tiết doanh thu theo khoảng thời gian, theo bộ môn hoặc theo gói hội viên.
* `GET /api/reports/members`: Thống kê biến động hội viên, hội viên sắp hết hạn, hội viên vắng nhiều.
* `GET /api/audit-logs`: Nhật ký kiểm toán hệ thống (ghi vết ai tạo lớp, sửa giá, dời lịch, thu tiền, điểm danh).
* `GET /api/health`: Health check container hệ thống (trả về HTTP 200 {"status":"UP","message":"pong"} cho Render giám sát).


---

### IV. VIỆC TIẾP THEO CẦN LÀM - FE
→ PHÂN NÀY CHỈ LÀ THAM KHẢO, CÓ THỂ SÓT VÀI THỨ, VD NHƯ: UI HIỆN QR THANH TOÁN
→ CHƯA LÀM, NẾU LAN ANH KO LÀM ĐC THÌ ANH LÀM SAU: 
THÔNG BÁO & MẤY CÁI LIÊN QUAN
CHATBOX HỖ TRỢ (AI hoặc không, có thể làm tạm dạng FAQ, giả lập auto reply để demo)
SMTP GMAIL ĐĂNG KÝ

Hệ thống đã hoàn thành **100% Giai đoạn 1 đến Giai đoạn 5** (Toàn bộ nội dung/flow trong Member.pdf, CSDL, Cronjobs, Xử lý sự cố, 178 tests pass).

Nhiệm vụ tiếp theo là **Giai đoạn 6: Tích Hợp Giao Diện Frontend (FE)** trong thư mục `FE_Project-main/`:

1. **Bàn giao [`docs/01-API-CONTRACT-FE-BE.md`] cho FE:**
   * Đồng bộ Base URL, Session Cookie (`withCredentials: true`), các Error Code chuẩn (`NORMAL_CLASS_NON_CANCELLABLE`, `CANCEL_TOO_LATE`, v.v.).
2. **Cập nhật màn hình danh mục khóa học ([`MemberClassCatalog.jsx`])
   * Hiển thị danh mục 9 bộ môn thể thao chính thức của trung tâm.
   * Chia thành 2 Tab rõ rệt: **Khóa Nhóm (Normal - 12 buổi)** và **Khóa 1:1 (Advanced - 12 Credit)**.
3. **Cập nhật màn hình Quản lý Lớp ([`ClassManagement.jsx`] & [`NewClassModal.jsx`])
   * Form tạo lớp: Khi Manager chọn môn học $\rightarrow$ Tự động điền giá tiền `normal_price` (không cho nhập tay giá), chọn các thứ trong tuần (T2-4-6 hoặc T3-5-7).
   * Tích hợp nút **Preview 12 buổi học** gọi endpoint `POST /api/sessions/preview-recurring` để hiển thị bảng lịch dự kiến và danh sách ngày lễ được tự động bỏ qua trước khi lưu.
4. **Xây dựng giao diện Quản lý Ngày Lễ (Holidays) cho Manager:**
   * Hiển thị danh sách ngày nghỉ lễ theo 3 nhóm Tag (`NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`).
   * Cung cấp nút Toggle chuyển đổi trạng thái Bật/Tắt ngày nghỉ (`PATCH /api/holidays/{id}/toggle`).
5. **Cập nhật màn hình Member Đặt lịch 1:1 linh hoạt:**
   * Hiển thị số dư Credit còn lại và số ngày hết hạn (đếm ngược từ mốc 45 ngày).
   * Lưới chọn ngày và ca học rảnh của Huấn luyện viên (`GET /api/coaches/{coachId}/available-slots`): Tự động vô hiệu hóa các ca đã trùng, ca trong quá khứ hoặc ca sốc nhiệt (Ca 3, Ca 4 nếu môn ngoài trời).
   * Nút Hủy ca học: Hiển thị cảnh báo nếu hủy trước 24h thì hoàn credit, còn dưới 24h thì không được hủy.
   
---

### V. Thông tin linh tinh

1. Sandbox QR thanh toán: Cơ chế hoạt động & Sinh QR ra sao? Có cần tài khoản không?
Cơ chế hoạt động:
Online (VNPay-QR): Khi tạo đơn mua gói/lớp chọn VNPay, Backend sinh ra 1 đường link bảo mật (đã băm mã HMAC-SHA512). Khi user mở link này, chính cổng VNPay Sandbox sẽ tự render một mã VNPAY-QR to ở giữa màn hình kèm tùy chọn thanh toán bằng thẻ demo (NCB).
Chuyển khoản quầy (VietQR): Sinh trực tiếp qua thẻ ảnh API Napas:
https://img.vietqr.io/image/<BANK>-<STK>-compact2.png?amount=<TIỀN>&addInfo=<MÃ_HÓA_ĐƠN>
Mã QR này tự động điền sẵn số tiền và mã hóa đơn khi khách quét bằng bất kỳ App ngân hàng nào.
Có cần đăng ký/tài khoản gì không?:
Phía người dùng (học viên): KHÔNG. Dùng App ngân hàng bất kỳ để quét mã, hoặc dùng thông tin thẻ test NCB công khai (9704198526191432198, OTP: 123456).
Phía Backend: Dùng mã Sandbox chung của VNPay cấp miễn phí (VNPAY_TMN_CODE = 2QXUI4B4, VNPAY_HASH_SECRET = RAOQUI25088809890), không mất phí. VietQR không cần tài khoản hay API key.

2. Checkin QR sẽ được user nhận ở đâu? Đã sinh QR ok chưa?
User nhận ở đâu:
Học viên đăng nhập vào giao diện web/mobile, vào trang Cá nhân hoặc Điểm danh.
FE gọi API GET /api/members/me/checkin-code và nhận về:
json
{ "payload": "899357", "expiresInSeconds": 30 }
FE truyền chuỗi 6 số payload này vào component tạo mã QR (ví dụ qrcode.react) để hiển thị ảnh QR trên màn hình điện thoại học viên.
Đã sinh QR ok chưa?:
Phía Backend: ĐÃ XONG VÀ TEST OK 100%. Mã TOTP 6 số tự động đếm ngược và đổi mới mỗi 30 giây chống gian lận.
Phía Frontend: Chỉ cần component hiển thị QR từ chuỗi số trên.

3. OTP gmail đăng ký sẽ được user nhận từ đâu?
User nhận từ đâu:
Nhận trực tiếp trong Hộp thư đến (Inbox / Spam) của tài khoản Gmail mà user đã nhập khi đăng ký tài khoản (POST /api/auth/register).
Backend gửi mail qua giao thức SMTP (smtp.gmail.com:587) với tiêu đề "Mã OTP xác thực đăng ký", mã gồm 6 số có hạn trong 10 phút.
Điều kiện: Cần cấu hình 2 biến môi trường trên server: MAIL_USERNAME (Gmail của trung tâm) và MAIL_APP_PASSWORD (Mật khẩu ứng dụng 16 ký tự do Google cấp). Nếu chưa cấu hình mail, hệ thống hỗ trợ biến REGISTRATION_DEMO_OTP (ví dụ 123456) để test trực tiếp mà không cần gửi email thật.

4.
* Tag NATIONAL_FIXED (Lễ Quốc Gia Dương Lịch Cố Định):
01/01: Tết Dương Lịch
30/04: Ngày Giải phóng miền Nam
01/05: Ngày Quốc tế Lao động
02/09 & 03/09: Ngày Quốc khánh Việt Nam
* Tag LUNAR_FESTIVAL (Lễ Hội Truyền Thống / Âm Lịch theo năm):
Giỗ Tổ Hùng Vương (10/03 Âm lịch)
Chuỗi 7 ngày nghỉ Tết Nguyên Đán (Từ 28/29 Tháng Chạp đến Mùng 5 Tết)
* Tag CENTER_EVENT (Lịch Nghỉ Riêng Của Trung Tâm):
Bảo trì sân bãi định kỳ, sự kiện đại hội thể thao, nâng cấp cơ sở vật chất...
* Xanh dương cho NATIONAL_FIXED
Đỏ/Cam cho LUNAR_FESTIVAL
Xám/Tím cho CENTER_EVENT
* Công tắc Toggle On/Off (is_active): Manager chỉ cần click công tắc để Bật/Tắt một ngày nghỉ mà không cần phải xóa dữ liệu.
Nút "Thêm ngày nghỉ đột xuất": Manager chọn ngày, đặt tên và chọn tag CENTER_EVENT.
* Thuật toán sinh 12 buổi học: Khi nhảy ngày sinh lịch, thuật toán chỉ cần lọc:
sql
SELECT holiday_date FROM holidays WHERE is_active = TRUE;
* Nếu ngày học rơi trúng ngày có is_active = TRUE, hệ thống tự động nhảy sang tuần tiếp theo cho đến khi đủ 12 buổi.

---
