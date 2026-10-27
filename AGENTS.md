# AGENTS.md — Quy Chuẩn & Nguyên Tắc Hoạt Động Của Hệ Thống SCMS

Tài liệu này là chốt chặn bắt buộc cho tất cả các AI Agents và Lập trình viên khi tham gia phát triển dự án SCMS (Sports Center Management System).

> **Checkpoint hai phase 27/10/2026:** Owner đã yêu cầu triển khai P1.0–P1.4 và toàn bộ BE P2, giữ P1.5/gói T D01/D09/FE ở backlog; sau double-check/demo/tài liệu phải push branch và merge main. Đã có source, clean test và 9 kịch bản HTTP PostgreSQL local pass; đang chốt kiểm lại exact commit và bàn giao ở docs 02 mục 10/docs 04 mục 40.13. Không lấy test lịch sử làm chỉ tiêu count. Rule đang áp dụng: Primary Coach tạo Advanced; Manager/Admin hỗ trợ có lý do/consent; hủy >=12h; 45 ngày sử dụng hợp nhất nghỉ TT/sự cố; attendance mới Present/Absent trong [start,end); Normal chỉ được bù có hồ sơ khi buổi thực sự không tổ chức vì TT. Đồng đội SMTP/OTP/chat/notification đã merge không đồng nghĩa gói T hoặc FE đã hoàn tất. Production DB đã khởi tạo đúng bundle được Owner duyệt riêng trước merge:46bảng/9môn/0account/giao dịch; evidence docs 02 mục 10. Không suy thêm quyền seed/live tests từ phê duyệt này. Không copy .env hoặc chạy test chuẩn bị dữ liệu vào production.

---

## 1. THÔNG TIN CÔNG NGHỆ & MÔI TRƯỜNG VẬN HÀNH
* **Hệ điều hành:** Windows (Dùng PowerShell, đường dẫn file dùng `/` hoặc `\\`).
* **Backend:** Java 17, Spring Boot 3.3.5, Spring Data JPA, Hibernate, Spring Security 6.
* **Build tool:** Maven (`.\mvnw.cmd`).
* **Database:** PostgreSQL (Supabase) và tương thích MS SQL Server.
* **Frontend:** React + Vite, TailwindCSS (thư mục `FE_Project-main/`).
* **Múi giờ nghiệp vụ:** BẮT BUỘC dùng ZoneId `Asia/Ho_Chi_Minh` (UTC+7) qua Spring bean `Clock`. Tuyệt đối không dùng `LocalDateTime.now()` trôi nổi không múi giờ.

---

## 2. NGUYÊN TẮC BẤT DI BẤT DỊCH (CORE PRINCIPLES)

### 2.1. Bảo toàn Dữ liệu Kế toán & Tiền tệ
* **Lưu chết giá (Price Freezing):** Khi tạo hóa đơn (`Invoice`) từ Gói hội viên hay Lớp học, số tiền `amount` bắt buộc phải được copy từ `Subject` hoặc `MembershipPackage` và lưu cứng vào bảng `invoices`. Tuyệt đối không tính toán động lại số tiền của các hóa đơn cũ khi bảng giá môn học thay đổi.
* **Thanh toán là chốt chặn kích hoạt:** Suất học hoặc Gói hội viên chỉ được chuyển trạng thái `Active`/`Registered` sau khi hóa đơn được xác nhận `Paid` (qua Cash hoặc Callback VNPay có chữ ký hợp lệ). Tuyệt đối không kích hoạt trước khi thanh toán thành công.

### 2.2. Tuyệt đối Không Tin Tưởng Client (Frontend)
* Không nhận `amount`, `role`, hay `member_id` tự do từ frontend gửi lên nếu không khớp với phiên đăng nhập (`Authentication`).
* Backend phải luôn validate:
  1. Tài khoản hội viên có `Active` và không bị khóa đặt lịch (`bookingBlockedUntil`).
  2. Gói hội viên / Tín chỉ còn hạn hiệu lực.
  3. Sĩ số lớp hiện tại (`Registered` + `Pending`) chưa vượt quá `max_capacity`.
  4. Trùng lịch phòng tập và HLV `(A_Start < B_End AND A_End > B_Start)`.

### 2.3. Chống Tranh Chấp Đồng Thời (Race Condition)
* Mọi thao tác tranh chấp tài nguyên (đăng ký chỗ cuối của lớp, đặt ca học 1:1, xử lý callback thanh toán hóa đơn) BẮT BUỘC phải dùng Database Locking:
  * Sử dụng pessimistic write lock: `findByIdForUpdate` trong Repository.
  * Gom các thao tác cập nhật liên quan trong cùng một Transaction (`@Transactional`).

### 2.4. Phân Định Rõ Hai Dòng Sản Phẩm: Normal vs Advanced (1:1)
* **Khóa Normal (Học nhóm cố định 12 buổi):**
  * Sĩ số tối đa: $\le$ 15 người.
  * Sinh lịch tĩnh 12 buổi vào DB theo ngày bắt đầu và các thứ trong tuần.
  * **CẤM HỦY / BÁO VẮNG:** Nút hủy bị vô hiệu hóa; học viên vắng tự chịu mất buổi, không hoàn tiền, không hoàn credit, không xếp học bù do Member. Ngoại lệ duy nhất: hồ sơ xác minh buổi không tổ chức vì TT, đánh dấu buổi gốc không hợp lệ và liên kết đúng một buổi bù để đủ 12 buổi hợp lệ.
* **Khóa Advanced (Học 1:1 linh hoạt bằng 12 Credit):**
  * Sĩ số: 1 học viên - 1 HLV (`max_capacity = 1`), thời lượng 90 phút/buổi.
  * Member đăng ký môn/Primary Coach, được nhận trước Invoice; Paid mới cấp 12 Credit. Coach chính book, Manager/Admin hỗ trợ có lý do/consent, đặt trước >=12h. Thời hạn 45 ngày sử dụng từ Paid, skip hợp thời gian TT đóng/nghỉ; sessionEnd <= expiresAt chính xác.
  * Hủy lịch $\ge$ 12h: Hoàn lại 1 Credit.
  * Hủy lịch < 12h hoặc Vắng mặt (No-show): HLV điểm danh "Vắng" $\rightarrow$ Tiêu thụ 1 Credit (bảo vệ thù lao HLV), không hoàn, không sinh lịch bù.
  * Cronjob 00:00 hàng đêm: Quét expiresAt đã đến $\rightarrow$ chụp credit bị thu hồi, chuyển Expired. Phục hồi chỉ theo incident/evidence và balance gốc; không cấp lại 12.

### 2.5. Ràng Buộc Khung Giờ Ca Học & Thời Tiết
* Hệ thống vận hành theo **7 Ca học chuẩn (90 phút/ca)**:
  * Ca 1: 06:00 – 07:30
  * Ca 2: 07:45 – 09:15
  * Ca 3: 09:30 – 11:00
  * *Nghỉ trưa: 11:00 – 13:00 (Đóng cửa bảo trì, tránh nắng gắt)*
  * Ca 4: 13:00 – 14:30
  * Ca 5: 14:45 – 16:15
  * Ca 6: 16:30 – 18:00
  * Ca 7: 18:15 – 19:45
* **Môn Ngoài Trời (Outdoor):** Bóng đá, Tennis, Bắn cung, Bơi lội $\rightarrow$ Bắt buộc khóa Ca 3 và Ca 4 (chính sách chống sốc nhiệt/nắng nóng).
* **Môn Trong Nhà (Indoor):** Cầu lông, Bóng bàn, Bóng rổ, Bóng chuyền, Pickleball $\rightarrow$ Mở toàn bộ.

### 2.6. Giữ Chỗ Thanh Toán (Payment Holding Window)
* Thời gian giữ chỗ ở màn hình thanh toán chuẩn hóa là **5 phút** (`scms.payment.expire-minutes = 5`). Hết 5 phút mà chưa thanh toán thành công, hệ thống tự động hủy giữ chỗ và giải phóng slot.

### 2.7. Xử Lý Vắng Mặt & Chế Tài Khóa Đặt Lịch
* **Trừ Credit / Tiêu thụ buổi học:** Áp dụng tiêu thụ buổi học / trừ 1 Credit cho cả 2 loại khóa (Normal mất 1 buổi trong chuỗi 12; Advanced trừ 1 Credit vào tài khoản).
* **Feature Toggle cho Chế tài Khóa 7 ngày:** Tạm thời tắt cơ chế khóa tài khoản 7 ngày khi vắng 3 lần/tháng qua biến cấu hình `scms.booking-block.enabled=false`. **TUYỆT ĐỐI KHÔNG XÓA CODE**, giữ nguyên các method và unit test liên quan để sẵn sàng kích hoạt lại khi cần.

### 2.8. Quản Lý Lịch Nghỉ Lễ/Tết (Holidays)
* Bảng `holidays` được phân nhóm theo bộ Tag/Prefix chuẩn:
  * `NATIONAL_FIXED`: Ngày lễ dương lịch cố định (01/01, 30/04, 01/05, 02/09...).
  * `LUNAR_FESTIVAL`: Lễ Tết âm lịch (Tết Nguyên Đán, Giỗ Tổ 10/03...).
  * `CENTER_EVENT`: Ngày nghỉ riêng do trung tâm chủ động (Bảo trì, Sự kiện thể thao...).
* Quản lý (Manager) có thể chủ động Bật/Tắt (`is_active = true/false`) hoặc thêm bớt ngày nghỉ. Thuật toán sinh 12 buổi học chỉ nhảy qua các ngày có `is_active = true`.

### 2.9. Kỷ Luật Dừng, Hỏi & Kiểm Chứng (Stop, Clarify & Double-Check Rule)
* **Không Tự Ý Giả Định (Zero Assumption):** Khi Người dùng cung cấp thông tin/dữ liệu có dấu hiệu sai lệch, thiếu sót, hoặc phát hiện mâu thuẫn giữa yêu cầu mới và tài liệu chuẩn (`Member.pdf`), AI **BẮT BUỘC PHẢI DỪNG LẠI, CHỈ RÕ MÂU THUẪN VÀ ĐỀ XUẤT PHƯƠNG ÁN ĐỂ NGƯỜI DÙNG QUYẾT ĐỊNH**. Tuyệt đối không nhắm mắt làm theo thông tin sai lệch hay tự ý quyết định thay người dùng.
* **Double-Check Trước Khi Bàn Giao:** Sau mỗi quá trình / giai đoạn, trước khi trả lời Người dùng, AI bắt buộc phải đối chiếu lại toàn bộ checklist yêu cầu của giai đoạn đó, xác nhận code đã chạy kiểm thử thực tế và không để lại lỗi ngầm.

---

## 3. KỶ LUẬT CODE & KIỂM THỬ (TESTING DISCIPLINE)
1. **Quy tắc Kiểm thử:** Sau bất kỳ thay đổi nào ở Backend, PHẢI chạy lệnh:
   ```powershell
   .\mvnw.cmd clean test
   ```
   Đảm bảo 100% tests pass (ghi count từ lần chạy hiện tại; số lịch sử không phải chỉ tiêu). Nếu sửa đổi business rule dẫn đến test cũ bị lệch kỳ vọng, phải giải trình rõ nguyên nhân trước khi cập nhật test.
2. **Kỷ luật Database Migration:**
   * Không tự ý chạy lệnh phá hủy dữ liệu (`DROP TABLE`, `TRUNCATE`).
   * Sử dụng file migration SQL riêng biệt, có `IF NOT EXISTS` và kiểm tra tính tương thích ngược.
3. **Bảo toàn Tài liệu:**
   * Chỉ đọc các file tài liệu trong thư mục hoạt động chính (`docs/` và `AGENTS.md`).
   * Không đọc các file tài liệu lịch sử trong `old-docs/` trừ khi người dùng yêu cầu tra cứu lịch sử, nhằm tiết kiệm context window.
4. **Quản lý Master Checklist & Lịch sử Hoàn thành:**
   * Tiến độ tổng thể được quản lý tập trung tại `docs/00-MASTER-CHECKLIST.md`.
   * Mỗi khi hoàn thành xong một giai đoạn / danh sách công việc lớn (và khi được Người dùng yêu cầu), phải tóm tắt lại (hoặc giữ nguyên hiện trạng chi tiết) và chuyển vào phần `# LỊCH SỬ HOÀN THÀNH (HISTORY)` để giữ checklist active luôn tinh gọn.
5. **Cấu trúc Thư mục Docs Tinh Gọn (Bộ 3 Tinh Gọn):**
   * Duy trì 3 file tài liệu chủ chốt và docs/04-DEBUG-NOTE.md theo yêu cầu Owner trong `docs/`:
     * `docs/00-MASTER-CHECKLIST.md`: Tiến độ và lịch sử bàn giao.
     * `docs/01-API-CONTRACT-FE-BE.md`: Hợp đồng kết nối API toàn hệ thống cho Frontend.
     * `docs/02-DEPLOYMENT-GUIDE.md`: Cấu hình deploy Render, Supabase, CORS & biến môi trường.
