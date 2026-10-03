Dưới đây là danh sách tổng hợp ngắn gọn về **những gì đã làm** và **những điểm còn thiếu / cần bổ sung** cho từng flow:

---

### 🔐 Flow 0 & 1: Xác thực, Phân quyền RBAC & Quản lý Hội viên
* **Đã làm:**
  * Xác thực qua Session Cookie (`JSESSIONID`) và mã hóa mật khẩu `LegacyPasswordEncoder` (hỗ trợ cả SHA256 UTF-16LE và BCrypt).
  * Phân quyền RBAC động với `@PreAuthorize` và bảng `role_permissions`.
  * CRUD tài khoản người dùng, tự động đồng bộ bảng subtype (`Member`, `Coach`, `Staff`).
  * Quản lý gói tập (`packages`), đăng ký & gia hạn gói nối tiếp ngày hết hạn (`subscriptions`).
  * Ghi nhật ký thao tác quan trọng vào `system_logs`.
* **Còn thiếu / Cần bổ sung:**
  * Luồng quên/đặt lại mật khẩu qua OTP/Email (`/api/auth/forgot-password`).
  * Gửi email tự động thông báo tài khoản/mật khẩu mặc định khi tạo mới nhân sự/hội viên.
  * Ép buộc đổi mật khẩu ở lần đăng nhập đầu tiên (`is_first_login`).
  * Kiểm soát phân quyền theo phạm vi trung tâm (`center_id` scope guard).
  * Rate limiting chống Brute-force khi đăng nhập sai nhiều lần.

---

### 📅 Flow 2: Sắp xếp Lịch trình & Quản lý Tài nguyên
* **Đã làm:**
  * CRUD Bộ môn (`subjects`) và Phòng tập (`rooms`) kèm kiểm soát sức chứa (`capacity`).
  * Tạo lớp học (`classes`), phân công HLV và tự động sinh lịch định kỳ (`/api/sessions/generate`).
  * Thuật toán phát hiện xung đột thời gian thực cho Phòng và HLV `(A_Start < B_End AND A_End > B_Start)`.
  * Transaction Rollback toàn bộ nếu có bất kỳ buổi học nào trong chuỗi bị trùng lịch.
* **Còn thiếu / Cần bổ sung:**
  * Tích hợp kiểm tra lịch nghỉ phép của HLV (`CoachLeave`) trước khi xếp lịch.
  * Chuẩn hóa lưu trữ thời gian UTC tại Database và convert sang `UTC+7` khi render trên Frontend.
  * API kiểm tra xung đột trước (`POST /api/sessions/check-conflict`) để FE cảnh báo người dùng sớm.
  * Cronjob tự động chuyển trạng thái buổi học (`Upcoming` ➔ `Ongoing` ➔ `Completed`).
  * Đồng bộ lịch ra file `.ics` (iCalendar/Google Calendar) và bắn thông báo khi có đổi phòng/giờ học.

---

### 💰 Flow 3: Kinh doanh, Thanh toán & Báo cáo Doanh thu
* **Đã làm:**
  * Tự động sinh hóa đơn (`invoices`) khi mua gói tập và tra cứu lịch sử đơn hàng.
  * Thu tiền và ghi nhận thanh toán tại quầy (`Cash`, `VietQR`, `CreditCard`) kèm Audit Log.
  * Báo cáo Dashboard KPI tổng quan, báo cáo doanh thu theo mốc thời gian và tăng trưởng hội viên.
* **Còn thiếu / Cần bổ sung:**
  * Tích hợp cổng thanh toán trực tuyến (VNPay / MoMo) qua link/mã QR.
  * Webhook IPN (`POST /api/webhooks/payment`) có xác thực chữ ký bảo mật & Idempotency.
  * Đồng hồ đếm ngược QR 15 phút trên FE và Cronjob BE tự động hủy (`Canceled`) các đơn quá hạn.
  * Gửi hóa đơn điện tử dạng PDF qua email sau khi thanh toán thành công.

---

### 🏃 Flow 4: Đăng ký học, Điểm danh & Tiến độ Tập luyện
* **Đã làm:**
  * Cơ chế **5 Guards** khi đăng ký lớp (Check Active, Gói tập còn hạn, Sĩ số lớp, Chống trùng lớp, Chống trùng giờ).
  * Điểm danh tại buổi học và Check-in/Check-out tự do tại sảnh.
  * **Attendance Correction Guard:** Bắt buộc nhập lý do (`reason`) khi sửa điểm danh quá khứ và lưu vết Audit Log.
  * Quản lý Giáo án (`training_plans`), ghi nhận kết quả (`training_results`) và Đánh giá định kỳ (`evaluations`).
* **Còn thiếu / Cần bổ sung:**
  * Cơ chế Hàng đợi chờ (**Waitlist**) khi lớp đầy và tự động đôn lên khi có người hủy.
  * Ràng buộc chặn hội viên tự hủy lịch học khi còn dưới 2 tiếng trước giờ bắt đầu.
  * Mã QR Check-in động (**TOTP 30s**) chống chia sẻ tài khoản trái phép.
  * Hệ thống cảnh báo/phạt vắng mặt không phép (**Penalty/No-show**) và cảnh báo học viên có nguy cơ bỏ cuộc (**Churn Risk Alert**).
  * Thực đơn dinh dưỡng (**Diet Plan**) đính kèm giáo án tập luyện.

---

### 🤖 Flow 5 & 6: Trí tuệ Nhân tạo (Gợi ý bài tập & Chatbot AI)
* **Đã làm:**
  * Đã hoàn thiện thiết kế kịch bản System Prompt, Context Injection và cấu trúc lưu vết `AIConsultation`.
* **Còn thiếu / Cần bổ sung:**
  * Tích hợp SDK LLM (OpenAI / Gemini) kết hợp truyền ngữ cảnh thể trạng hội viên.
  * Cơ chế Streaming phản hồi (SSE/WebSocket) hiển thị dạng gõ phím.
  * Bộ nhớ đệm Cache các câu hỏi phổ biến và giới hạn hạn mức câu hỏi (Rate Limiting/Quota).
  * Trả dữ liệu dạng Structured JSON để sinh nhanh giáo án vào hệ thống.

---

### 🛎️ Flow 7: Hỗ trợ Khách hàng (Tickets) & Trung tâm Thông báo
* **Đã làm:**
  * Đã thiết kế cấu trúc dữ liệu (`SupportRequest`, `Notification`, `NotificationRecipient`) và luồng trạng thái.
* **Còn thiếu / Cần bổ sung:**
  * API quản lý trạng thái ticket (`Pending` ➔ `Processing` ➔ `Resolved` ➔ `Closed`) và giám sát SLA 24h.
  * Tích hợp lưu trữ ảnh lỗi đính kèm (Cloudinary / S3).
  * Event-driven Notification ServiceƯ bắn thông báo thời gian thực qua WebSocket/FCM.
  * Giao diện Quả chuông thông báo (Notification Center) trên Header.