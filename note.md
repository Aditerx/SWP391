
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

### III. VIỆC TIẾP THEO CẦN LÀM
→ PHÂN NÀY CHỈ LÀ THAM KHẢO, CÓ THỂ SÓT VÀI THỨ, VD NHƯ UI HIỆN QR THANH TOÁN
BE CHƯA LÀM: PHẦN THÔNG BÁO, OTP GMAIL ĐĂNG KÝ (NẾU LAN ANH KO LÀM ĐC THÌ A LÀM SAU)

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