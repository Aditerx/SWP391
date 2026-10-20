# ĐỀ XUẤT TÁI CẤU TRÚC TÀI LIỆU, ĐỐI CHIẾU THIẾT KẾ & MASTER CHECKLIST (SCMS)

> **Mục tiêu:** Tinh gọn hệ thống tài liệu, triệt tiêu tình trạng "nhiễm độc context" (context pollution) do các tài liệu cũ/mâu thuẫn, chuẩn hóa kiến trúc nghiệp vụ giữa `Member.pdf` và Codebase hiện tại, đồng thời thiết lập lộ trình rõ ràng cho các giai đoạn tiếp theo.

---

## PHẦN 1: KẾ HOẠCH DỌN DẸP & SẮP XẾP TÀI LIỆU

### 1.1. Hiện trạng và Vấn đề
Trong thư mục gốc hiện có **19 file `.md`** với tổng dung lượng **hơn 400KB**. Khi AI Agent hoặc lập trình viên đọc các file này mỗi phiên làm việc, xảy ra các rủi ro:
1. **Hao tổn Context Window:** Tốn hàng chục ngàn tokens vô ích cho các file log bàn giao cũ, script demo tạm thời.
2. **Nhiễm độc Context (Mâu thuẫn thông tin):**
   * `CHECKLIST.md` (03/10) ghi cổng thanh toán trực tuyến "Chưa làm", nhưng `CHANGES_2026-10-10.md` đã làm VNPay Sandbox.
   * `FLOW-PROGRESS.md` (03/10) ghi Waitlist & TOTP QR "Chưa làm", nhưng `update_17/10.md` đã hoàn thiện và vượt qua 81 unit tests.
   * `PROJECT_CONTEXT.md` ghi nhận 5 vai trò ban đầu nhưng chưa cập nhật ma trận phân quyền chi tiết RP-04 đến RP-45 từ `Member.pdf`.

### 1.2. Đề xuất phân loại & Gom cụm vào `old-docs/`

Chuyển **14 file tài liệu lịch sử / báo cáo đợt cũ** vào thư mục `old-docs/` (vẫn lưu vết Git, không mất mát):

| Tên file hiện tại | Dung lượng | Lý do đưa vào `old-docs/` |
|---|:---:|---|
| `CHANGES_NOTE_101026.md` | 116 KB | Báo cáo bàn giao chi tiết ngày 10/10, rất nặng, đã được tổng hợp xong |
| `README_BE_CHANGES_2609.md` | 38 KB | Ghi chú kỹ thuật đợt bàn giao tháng 9, đã phản ánh trong code |
| `TECHNICAL_ANALYSIS.md` | 31 KB | Phân tích kỹ thuật đợt 03/10 |
| `REVIEW_CHANGES_2026-10-10.md` | 23 KB | Review code đợt demo 10/10 |
| `DEMO_SCRIPT_BE_0310.md` | 17 KB | Kịch bản demo backend cũ ngày 03/10 |
| `DEMO_FE_BE_2026_10_10.md` | 14 KB | Kịch bản chạy demo Cash ngày 10/10 |
| `CHECKLIST.md` | 14 KB | Checklist ngày 03/10 (đã lỗi thời so với bản 17/10) |
| `API_TEST_REPORT - cu.md` | 11 KB | Báo cáo test API cũ ngày 30/09 |
| `PROGRESS 0310.md` | 7.5 KB | Báo cáo tiến độ ngày 03/10 |
| `PROGRESS_2026_10_10.md` | 6.6 KB | Báo cáo tiến độ ngày 10/10 |
| `CHANGES_2026-10-10.md` | 6.1 KB | Tóm tắt thay đổi ngày 10/10 |
| `FLOW-PROGRESS.md` | 5.8 KB | Đánh giá flow ngày 03/10 |
| `RBAC-CHANGELOG.md` | 4.2 KB | Changelog phân quyền ngày 09/10 |
| `CHECK_TARGET_SWP391_10_10_26.md` | 1.5 KB | File ghi chú câu hỏi kiểm tra ngày 10/10 |

---

### 1.3. Cấu trúc Bộ Active Docs (Tài liệu sống tinh gọn)

Chỉ duy trì **1 file chốt chặn ở root** và **5 file tài liệu chuyên đề trong thư mục `docs/`**:

```
D:\Projects\SWP391\
├── AGENTS.md                          # Chốt chặn quy tắc bất biến cho AI & Dev (ĐÃ TẠO)
├── docs\
│   ├── 00-MASTER-CHECKLIST.md         # Bảng tiến độ duy nhất đúng, chuẩn hóa theo hiện trạng thực
│   ├── 01-DOMAIN-ARCHITECTURE.md      # Mô hình quan hệ: Môn - Khóa - Lớp - Gói tập - Ca học - Credit
│   ├── 02-FLOW-BOOKING-ATTENDANCE.md  # Luồng Normal (12 buổi tĩnh) vs Advanced 1:1, Hủy/No-show, TOTP QR
│   ├── 03-FLOW-PAYMENT-INVOICE.md     # Thanh toán: Cash quầy, VNPay, Expire 5m, Bảo toàn giá kế toán
│   ├── 04-RBAC-SECURITY-AUDIT.md      # Ma trận quyền RP-04 đến RP-45, Center Scope, Audit Trail
│   └── 05-DEPLOYMENT-ENVIRONMENT.md   # Supabase DB, Render BE, Vite FE, CORS, Cấu hình môi trường
└── old-docs\                          # Lưu trữ 14 file cũ phục vụ tra cứu lịch sử
```

---

## PHẦN 2: ĐỐI CHIẾU HỆ THỐNG THỰC TẾ & 5 MÂU THUẪN THIẾT KẾ CẦN CHỐT

Qua nghiên cứu các hệ thống quản lý phòng tập / trung tâm thể thao thực tế trên thị trường và GitHub (Mindbody, Glofox, OpenGym, Wodify), cũng như đối chiếu trực tiếp giữa `Member.pdf` và mã nguồn hiện tại, dưới đây là **5 mâu thuẫn thiết kế cốt lõi** cần được làm rõ và lựa chọn giải pháp:

---

### 🔍 MÂU THUẪN 1: MÔ HÌNH HÓA KHÓA HỌC 1:1 (ADVANCED) — DÙNG `classes` HAY `member_packages` (CREDIT)?

#### Vấn đề:
* Trong `Member.pdf`:
  * *Trang 1 & 2:* Khóa Advanced **không sinh trước lịch học**; học viên mua gói $\rightarrow$ nạp **12 Credit** $\rightarrow$ tự chọn HLV và tự book từng ca học (90 phút).
  * *Trang 10:* Đề xuất sửa bảng `classes` thêm `class_type ('Normal', 'Advanced')`, sửa ràng buộc `max_capacity = 1` cho Advanced. Đồng thời bảng `membership_packages` thêm `package_type ('Normal', 'Advanced')`, `total_credit = 12`.
* *Mâu thuẫn trong CSDL hiện tại:*
  * Bảng `sessions` hiện tại bắt buộc phải có `class_id` (`NOT NULL`).
  * Nếu học viên A mua gói 1:1 Tennis và tự book 1 buổi học vào 14:45 Thứ Ba với HLV Tuấn, thì buổi học (`Session`) đó thuộc về `Class` nào?

#### So sánh thực tế (Industry Benchmark):
* **Mindbody & Glofox:** Tách biệt hoàn toàn `Class` (Group Fitness - học nhóm lịch cố định) và `Appointment` (Personal Training 1:1 - đặt lịch theo rảnh của HLV). Gói Credit được trừ trực tiếp khi book `Appointment`.
* **Wodify:** Tạo một "Class 1:1" cho từng cặp HLV - Học viên hoặc Class ảo đại diện cho bộ môn.

#### 3 Phương án để bạn chọn:
* **Phương án A (Tạo Class 1:1 linh hoạt cho từng hợp đồng học viên):**
  Khi Member mua gói 1:1 thành công $\rightarrow$ Hệ thống tự sinh 1 bản ghi `SportsClass` loại `Advanced` (VD: "Tennis 1:1 - HLV Tuấn - Học viên An", `maxCapacity = 1`). Khi học viên book slot nào thì sinh `Session` thuộc Class đó.
  * *Ưu điểm:* Tận dụng 100% cấu trúc `Session` $\leftrightarrow$ `SportsClass` hiện có, không phải sửa khóa ngoại `class_id` trong bảng `sessions`.
  * *Nhược điểm:* Bảng `classes` sẽ sinh ra nhiều bản ghi cá nhân.
* **Phương án B (Dùng 1 Class Advanced chung cho mỗi Môn học):**
  Mỗi bộ môn tạo 1 `SportsClass` mẫu loại `Advanced` (VD: "Quần vợt 1:1"). Mọi buổi học 1:1 của môn đó đều trỏ về `class_id` này. Bảng `sessions` bổ sung cột `coach_id` và `booked_member_id` để biết buổi học cụ thể đó là của ai.
  * *Ưu điểm:* Gọn bảng `classes`, dễ quản lý báo cáo môn học.
  * *Nhược điểm:* Cần thêm cột `booked_member_id` vào `sessions`.
* **Phương án C (Tách riêng bảng `private_sessions` hoặc làm `class_id` nullable):**
  Cho phép `sessions.class_id` nhận `NULL` đối với buổi 1:1, bổ sung `subscription_id` và `member_id`.

👉 **Đề xuất khuyến nghị:** **Phương án B** hoặc **Phương án A** (Phương án A giữ trọn vẹn toàn bộ 81 tests hiện có mà không phải thay đổi cấu trúc bảng `sessions`).

---

### 🔍 MÂU THUẪN 2: XUNG ĐỘT CHÍNH SÁCH HỦY LỊCH (CANCELLATION POLICY)

#### Vấn đề:
* **Code hiện tại (Flow 4 Vòng 2 - [update_17/10.md](file:///d:/Projects/SWP391/update_17/10.md)):**
  Cho phép Member tự hủy bất kỳ lớp học nào nếu thời gian hủy cách giờ bắt đầu $\ge$ 2 tiếng (`CANCEL_TOO_LATE`). Đã có 11 unit test đang bao phủ nghiệp vụ này.
* **Yêu cầu trong `Member.pdf` (FR-015, FR-018, RP-17, RP-19):**
  * **Khóa Normal:** **CẤM HỦY TUYỆT ĐỐI.** Nút Hủy bị vô hiệu hóa. Học viên vắng tự chịu trách nhiệm mất buổi (không hoàn tiền, không hoàn credit, không xếp học bù).
  * **Khóa Advanced (1:1):** Cho phép hủy khi còn **$\ge$ 24 tiếng** $\rightarrow$ hoàn lại 1 Credit. Dưới 24 tiếng $\rightarrow$ khóa nút hủy, mất credit.

#### Đề xuất giải quyết:
Phân nhánh logic trong [`EnrollmentService.java`](file:///d:/Projects/SWP391/src/main/java/com/sportscenter/enrollment/EnrollmentService.java):
1. Khi gọi API hủy: Kiểm tra `class_type`.
2. Nếu `class_type == 'Normal'`: Ném ngoại lệ `BUSINESS_EXCEPTION: NORMAL_CLASS_NON_CANCELLABLE` (Trả mã 400).
3. Nếu `class_type == 'Advanced'`: Kiểm tra mốc 24 tiếng (thay vì 2 tiếng). Nếu $\ge$ 24h thì hoàn 1 credit; nếu < 24h thì chặn hủy.
4. Điều chỉnh lại các unit test cũ cho phù hợp với 2 quy tắc mới này.

---

### 🔍 MÂU THUẪN 3: XỬ LÝ VẮNG MẶT (NO-SHOW) VÀ KHÓA ĐẶT LỊCH (BOOKING BLOCK)

#### Vấn đề:
* **Code hiện tại (Flow 4 Vòng 2):**
  Scheduled job tự động quét các buổi học kết thúc, nếu học viên không có mặt thì đánh dấu `Absent`. Nếu một học viên vắng $\ge$ 3 lần trong tháng hiện tại $\rightarrow$ Khóa quyền đặt lịch 7 ngày (`booking_blocked_until`).
* **Yêu cầu trong `Member.pdf` (FR-020, RP-21):**
  * Khóa Normal: Vắng mặt thì mất buổi học, không xếp học bù.
  * Khóa Advanced: HLV điểm danh "Vắng mặt" (No-show) $\rightarrow$ Trừ 1 Credit vào tài khoản học viên để bảo vệ thu nhập cho HLV. Tuyệt đối không sinh bản ghi học bù (Make-up class).

#### Đề xuất giải quyết:
* Đối với Khóa 1:1: Việc trừ 1 Credit khi No-show là hình phạt tài chính trực tiếp và dứt điểm (tiêu thụ buổi học).
* Đối với Khóa Normal: Vắng mặt đơn giản là mất buổi học đó trong chuỗi 12 buổi.
* **Câu hỏi cần quyết định:** Có tiếp tục giữ cơ chế **"Khóa đặt chỗ 7 ngày nếu vắng 3 lần/tháng"** như một bộ lọc hành vi phụ cho toàn hệ thống không, hay bãi bỏ để chỉ áp dụng trừ credit / mất buổi theo `Member.pdf`?
  *(Khuyến nghị: Vẫn giữ quy tắc khóa 7 ngày đối với lớp Normal để răn đe học viên đăng ký giữ chỗ rồi bỏ trống làm lãng phí sĩ số của trung tâm).*

---

### 🔍 MÂU THUẪN 4: THỜI GIAN GIỮ CHỖ THANH TOÁN (5 PHÚT VS 15 PHÚT)

#### Vấn đề:
* `Member.pdf` (Trang 2, Trang 3 mục 4.1): Quy định thời gian giữ chỗ ở màn hình thanh toán là **5 phút**. Hết 5 phút mà chưa thanh toán thì tự động hủy slot và trả lại hệ thống.
* Code hiện tại ([PaymentProperties.java](file:///d:/Projects/SWP391/src/main/java/com/sportscenter/invoice/PaymentProperties.java)): Đang cấu hình mặc định là `payment.expire-minutes = 15`.

#### Đề xuất giải quyết:
Điều chỉnh biến môi trường / cấu hình trong `application.properties`:
```properties
scms.payment.expire-minutes=5
```
Đồng bộ cả Backend job và đồng hồ đếm ngược trên Frontend về đúng 5 phút (05:00).

---

### 🔍 MÂU THUẪN 5: THUẬT TOÁN SINH 12 BUỔI TĨNH & XỬ LÝ NGÀY NGHỈ LỄ/TẾT

#### Vấn đề:
* `Member.pdf` (Trang 9 mục 3): "Manager chỉ nhập T2-T4-T6, bắt đầu từ ngày X, tổng 12 buổi... Cần chú ý thuật toán bỏ qua các ngày Lễ/Tết".
* Hiện tại trong hệ thống chưa có bảng dữ liệu nào lưu ngày Lễ/Tết của Việt Nam.

#### Đề xuất giải quyết:
* **Phương án 1 (Khuyến nghị - Tạo bảng `holidays`):**
  Tạo bảng `holidays (holiday_id, holiday_date, holiday_name)`. Thuật toán sinh lịch khi nhảy ngày sẽ kiểm tra nếu ngày đó trùng ngày lễ trong bảng `holidays` thì tự động nhảy tiếp sang ngày lặp tiếp theo cho đến khi đủ 12 buổi. Admin/Manager có thể chủ động cấu hình lịch nghỉ lễ của trung tâm.
* **Phương án 2 (Cấu hình danh sách ngày cố định trong code/properties):**
  Cố định danh sách các ngày lễ lớn (1/1, 30/4, 1/5, 2/9) vào file config.

---

## PHẦN 3: BẢNG CHECKLIST MASTER TỔNG HỢP (THEO SÁT TIẾN ĐỘ THẬT)

Bảng này phản ánh đúng hiện trạng sau đợt review 17/10 và các hạng mục mới từ `Member.pdf`:

### 📊 TIẾN ĐỘ TỔNG THỂ

| Hạng mục | Trạng thái kỹ thuật | Đánh giá so với `Member.pdf` |
|---|:---:|---|
| **Xác thực, Phân quyền RBAC & Quản lý User** | ✅ Đã hoàn thành | Khớp 100% |
| **Quản lý Môn học, Phòng tập & Bảng giá** | 🔄 Cần cập nhật | Thiếu giá 2 mức (`normal_price`, `advanced_price`) & `Indoor`/`Outdoor` |
| **Khung giờ Ca học chuẩn & Chặn Nắng nóng** | ⏳ Chưa có | Cần định nghĩa 7 Ca chuẩn & Khóa Ca 3, Ca 4 cho môn ngoài trời |
| **Tạo lớp & Sinh 12 buổi học tĩnh (Normal)** | 🔄 Cần nâng cấp | Hiện sinh theo khoảng ngày tự do, chưa cố định đúng 12 buổi bỏ qua Lễ |
| **Đăng ký & Điểm danh lớp Normal** | ✅ Đã có nền tảng | Cần sửa: Cấm Member tự hủy lớp học Normal |
| **Quản lý Gói Credit & Đặt lịch 1:1 (Advanced)** | ⏳ Chưa có | Chưa có mô hình 12 credit, hạn 45 ngày, học viên tự book ca học |
| **Hủy lịch 1:1 & Phạt No-show trừ Credit** | ⏳ Chưa có | Cần triển khai hủy $\ge$ 24h hoàn credit, No-show trừ 1 credit |
| **Hóa đơn, Thu Cash & VNPay Sandbox** | ✅ Đã hoàn thành | Đã lưu chết giá kế toán, cần chỉnh thời gian giữ chỗ về 5 phút |
| **Xử lý sự cố Trung tâm & Cronjob 45 ngày** | ⏳ Chưa có | Cần bổ sung Cronjob 00:00 quét hết hạn credit |
| **Giao diện Frontend tích hợp** | 🔄 Cần đồng bộ | Cần thay mockup bằng 9 bộ môn thật và bổ sung màn hình đặt lịch 1:1 |

---

## PHẦN 4: CÁC QUYẾT ĐỊNH CẦN BẠN CHỐT

Để tránh tự ý giả định (assume), xin bạn cho ý kiến và lựa chọn đối với các điểm sau:

1. **Về việc dọn dẹp tài liệu:**
   * Bạn có đồng ý cho phép tôi tạo thư mục `old-docs/` và di chuyển 14 file markdown lịch sử vào đó để giải phóng context không?
2. **Về mô hình dữ liệu Khóa học 1:1 (Mâu thuẫn 1):**
   * Bạn chọn **Phương án A** (mỗi học viên mua 1:1 sinh 1 `SportsClass` loại Advanced riêng có `maxCapacity=1`) hay **Phương án B** (1 `SportsClass` Advanced chung cho mỗi môn học, bổ sung `booked_member_id` vào `sessions`)?
3. **Về cơ chế xử lý Ngày Lễ/Tết khi sinh 12 buổi (Mâu thuẫn 5):**
   * Bạn muốn tạo bảng `holidays` trong DB để quản lý động ngày nghỉ lễ, hay cấu hình danh sách ngày nghỉ trong file `application.properties`?
4. **Về thời gian giữ chỗ thanh toán:**
   * Thống nhất chuyển từ 15 phút về **5 phút** theo đúng `Member.pdf`?

