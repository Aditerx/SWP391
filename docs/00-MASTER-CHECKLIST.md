# 00-MASTER-CHECKLIST.md — Kế Hoạch & Tiến Độ Triển Khai SCMS

Tài liệu này là chốt chặn quản lý tiến độ tổng thể và lịch sử bàn giao của dự án Sports Center Management System (SCMS) theo đúng chuẩn `Member.pdf` và `AGENTS.md`.

---

## 📌 PHẦN I: TIẾN ĐỘ THỰC HIỆN (ROADMAP)

### Chốt chặn sau rà soát 21/10/2026
- [ ] Người dùng chốt nguồn nghiệp vụ cho Advanced, hạn 45 ngày, giữ chỗ và phạm vi gói thành viên; xem báo cáo đối chiếu trong HISTORY bên dưới.
- [x] Sửa nhóm có quy tắc rõ: lịch, credit, phân quyền, timeout, toggle và báo cáo; xem checklist triển khai 23/10.
- [x] Cập nhật contract/deployment theo implementation, phân biệt các chính sách chưa chốt; nghiệm thu môi trường thật còn ở checklist tích hợp.

**Lưu ý:** Các dấu hoàn thành của các đợt trước là lịch sử triển khai, không phải xác nhận hệ thống đã đạt toàn bộ yêu cầu. Kết quả test xanh hiện tại chưa chứng minh tuân thủ hai PDF.

### Checklist tích hợp còn lại
- [ ] Chốt quyết định nghiệp vụ đang giữ riêng trong phần triển khai 23/10 bên dưới.
- [ ] Áp dụng migration 06 trên DB test đúng hệ quản trị; kiểm tra mapping Specialization–Subject và phân loại môn cũ.
- [ ] Nghiệm thu trên PostgreSQL/SQL Server, VNPay Sandbox và trình duyệt với CORS/cookie thực tế.
- [ ] Đối chiếu đúng commit/branch FE-01–FE-11 và tích hợp contract mới (Normal atomic, lịch catalog, expiresAt, reconciliationRequired).

---

## 📜 PHẦN II: LỊCH SỬ HOÀN THÀNH (HISTORY)

### [Triển khai 23/10/2026] — Sửa phần có quy tắc rõ, giữ riêng các quyết định nghiệp vụ

Theo yêu cầu người dùng: xử lý phần không cần quyết định, build rồi test và có checklist; không áp dụng migration/reset lên DB đang dùng, không push/deploy. Giữ các thay đổi README/schema/demo có sẵn; migration 05 chỉ thêm guard chống reset vô tình.

#### Checklist đã thực hiện
- [x] BE-01: API `/api/classes/normal-with-sessions` tạo Class + 12 buổi + idempotency trong một transaction, lỗi rollback Class. Cùng requestId/body trả cùng Class. Không thêm Draft; luồng cũ tạo Normal Closed rồi generate/publish. Publish/enroll kiểm đủ 12 Session tương lai thực tế.
- [x] BE-02: owner/phân công/center ở GET Session list/detail. Thêm lịch catalog Normal không có Member/subscription để xem trước mua.
- [x] BE-03/04/05: ngày Session phải nằm trong hạn gói, account Active/không block; Coach Active/cùng center/chuyên môn đúng Subject. Khóa DB center trước subscription/session/invoice/class; đọc projection trước mutex để tránh entity cache cũ sau khi chờ khóa.
- [x] BE-06/08/12: Subject.outdoor lưu DB, backfill 9 môn; guard xóa có liên kết và thay loại môn/phòng/capacity đang có lịch. Sửa GETDATE trong RoomRepository thành ngày từ Clock, tránh SQL Server-only trên PostgreSQL.
- [x] Phần độc lập BE-07/11: validate giá âm, packageType/credit dương; chặn tạo/publish từ Subject Inactive. Giá 0 và package-subject chưa tự chọn.
- [x] BE-14/phần BE-15: giữ đúng quyền Cash, xử lý lặp nhất quán. Hết hạn VNPay trả suất; tiền về muộn ghi Paid + reconciliationRequired, không tự cấp lại quyền và không dùng làm bằng chứng re-activate gói. Chưa đổi holding/Return-IPN.
- [x] BE-17: bỏ timeline demo khi báo cáo rỗng.
- [x] Đóng đường tạo Member tại quầy kèm gói Active chưa Paid: chuyển qua subscription/Invoice Cash Pending; totalSpent lấy Invoice Paid.amount, không tính theo bảng giá gói hiện tại.
- [x] Sửa credit: đổi status không cấp lại credit; chỉ hoàn về subscription gốc còn hạn/Active và không vượt trần, không fallback sang gói khác.
- [x] Normal không báo vắng/hủy enrollment qua member/staff; Pending Invoice có route hủy riêng. HLV 1:1/thay thế kiểm theo Session; staff không ghi nhận xét chuyên môn. Không xóa/dời lịch đã chốt hoặc hủy thẳng quyền đã trả tiền qua update-status.
- [x] Feature toggle khóa 7 ngày mặc định false; giữ nguyên code/test chế tài và ghi vắng. Lifecycle Completed cho Session, Normal Enrollment và Advanced sau khi hết credit lẫn ca đã giữ; không hoàn thành sớm khi còn ca tương lai.
- [x] Promote waitlist Pending có Invoice Cash/hạn giữ chỗ. Chặn capacity thấp hơn số suất; không tự đổi giá Class khi sửa metadata.
- [x] Clock cho invoice/gateway/package/report/class, OTP/login lock, hồ sơ, kế hoạch/đánh giá, audit. Main source không còn LocalDate.now()/LocalDateTime.now() không Clock; bỏ CreationTimestamp ghi đè giờ Invoice do service cấp.
- [x] CORS origin cấu hình, cookie SameSite/Secure cấu hình; script không dò/in mật khẩu, không tự tạo đơn, kiểm rotation thật và exit lỗi khi smoke fail. Đã parse PowerShell, chưa chạy HTTP live.
- [x] Migration 06 có hai bản riêng PostgreSQL/SQL Server, không sửa giá/số dư/hóa đơn cũ. Coach DTO thêm bio/certification/status, EnrollmentResponse thêm amount/expiresAt; cập nhật contract/deployment.

#### Đối chiếu 20 mục
| Mã | Trạng thái |
|---|---|
| BE-01,02,04,06,08,12,14,17 | Đã triển khai backend; nghiệm thu môi trường thật chưa chạy |
| BE-03,05 | Đã sửa validation/khóa, chưa chọn lại mốc hạn hoặc kiến trúc credit |
| BE-07,11 | Đã có validation độc lập; giá 0, package-subject/phạm vi credit còn chờ |
| BE-09 | Seed workspace hiện có 9 môn; chưa xóa/deactivate hoặc reset dữ liệu live |
| BE-10,13,16,18,19,20 | Giữ riêng quyết định nghiệp vụ, không đánh dấu hoàn thành |
| BE-15 | Sửa timeout/race/tiền về muộn, giữ cấu hình; 5/15 phút và Return/IPN chưa chọn lại |

#### Quyết định đang chờ — giải thích sau run
1. **Advanced:** A trả tiền lấy credit rồi book từng ca (hiện tại) hay B giữ đủ 12 ca trước Paid. B đổi order/schedule/credit, không chỉ sửa FE.
2. **45 ngày:** từ Paid hay từ buổi đầu; tính tới hết endDate hay chính xác 45x24 giờ. Chỉ chặn ca ngoài hạn đã lưu, không đổi mốc trong run này.
3. **Holding/callback:** 5 hay 15 phút; Return được xử lý chữ ký như hiện tại hay chỉ đọc trạng thái IPN. Tiền về muộn hiện được giữ dấu, không chiếm lại suất; cách Manager đối soát/xếp lại/hoàn thủ công còn chờ.
4. **Giá:** bảng Member.pdf hay giá demo/docs; có 0đ không, quyền override giá công bố ở đâu. Không sửa giá kinh doanh hoặc amount hóa đơn cũ. Chưa thiết kế lại luồng miễn phí hiện có.
5. **Gói/credit:** mua khóa trực tiếp hay phải có gói Active; credit theo môn/đăng ký hay dùng chung. Chưa thêm package.subjectId để tránh chọn mô hình; điều kiện gói hiện tại của Normal vẫn giữ.
6. **Sự cố/quyền:** ai duyệt gia hạn, tự động hay duyệt trước; Member có phải đồng ý Coach thay; xử lý credit đã bị expiry xóa. Giữ cơ chế gia hạn cũ, không tự cấp thêm credit; quyền MANAGE_SUBSCRIPTIONS chưa phân lại. Chưa mở quyền Cash cho Manager.
7. **Đặt/hủy/điểm danh:** ngưỡng 12 hay 24 giờ; nút báo không đến dưới 24 giờ; cửa sổ ghi/sửa attendance. Giữ hiện tại, không suy quyền sửa từ cửa sổ QR.
8. **Lễ/nắng:** bắt buộc hay configurable; sửa/xóa Holiday và lịch đã có khi bật ngày nghỉ. Giữ 7 ca, Outdoor cấm ca 3–4, skip active holiday khi generate; không tự dời hàng loạt.
9. **Báo cáo/MVP phụ:** expiredMembers nghĩa hết hạn thật hay không có gói Active; ghi chú/mã biên lai Cash riêng, check-in cửa và CRUD Holiday có thuộc MVP không. Giữ chỉ số/tính năng hiện có đến khi chốt.

#### Kiểm chứng và giới hạn
- Build backend: `.\mvnw.cmd -DskipTests clean package` BUILD SUCCESS, 29,259 giây.
- Test sau build: `.\mvnw.cmd clean test` BUILD SUCCESS, 219 tests, 0 failures, 0 errors, 15 skipped, tức 204 pass; 59,806 giây. Tăng 41 test so với baseline 178, giữ các test chế tài và cập nhật kỳ vọng các hành vi sai đã giải trình.
- FE: `npm run build` thành công (Vite 3,48 giây). Hai script PowerShell parse thành công; `git diff --check` không có lỗi.
- Đã kiểm XML Surefire. Test DB H2 có tranh Coach/phòng, cùng gói đặt hai ca, hủy đồng thời, Cash đồng thời, tranh suất Normal cuối, callback muộn, idempotency và rollback Class khi generate lỗi; HTTP có 401/403 và CORS. H2 là DB in-memory riêng, không thực thi SQL reset hoặc ghi Supabase.
- Chưa chứng nhận migration PostgreSQL/SQL Server hay VNPay live đã pass; 15 test live vẫn opt-in/skip. Phải áp dụng migration 06, kiểm tra outdoor/specialization trước deploy. FE runtime/tích hợp cần đúng branch và môi trường thực.

### Roadmap lịch sử trước đợt sửa 23/10 (giữ nguyên chi tiết)

### Giai đoạn 1: Chuẩn Hóa Cơ Sở Dữ Liệu & Migration
- [x] Tạo file migration SQL độc lập `database/migrations/04_migration_member_flow_v2.sql`.
- [x] Thêm cột bảng `subjects`: `normal_price`, `advanced_price` và seed bảng giá chuẩn 9 môn.
- [x] Thêm cột bảng `rooms`: `facility_type` (`Indoor`, `Outdoor`).
- [x] Thêm cột bảng `classes`: `class_type` (`Normal`, `Advanced`), `total_sessions` (default 12), ràng buộc `max_capacity` ($\le 15$ cho Normal, $= 1$ cho Advanced).
- [x] Thêm cột bảng `membership_packages`: `package_type`, `total_credit` (default 12).
- [x] Thêm cột bảng `member_packages`: `remaining_credit`, `validity_days` (default 45).
- [x] Bổ sung bảng `sessions`: `booked_member_id`, `coach_id`, `subscription_id` (hỗ trợ đặt lịch 1:1).
- [x] Tạo bảng `holidays` và nạp danh mục ngày nghỉ lễ/tết cố định và âm lịch Việt Nam.
- [x] Reset và chuẩn hóa dữ liệu tài khoản demo (28 users: 5 role đại diện, 18 HLV 9 môn thể thao, 5 hội viên kịch bản nghiệp vụ) với mật khẩu đồng bộ "Scms@2026" (BCrypt) trong `schema_user_2310.sql` và migration 05.
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

---



### [Đợt 23/10/2026] — Đối chiếu PDF tổng hợp BE-01 đến BE-20

- Đã đọc đủ 12 trang `TỔNG HỢP 20 VẤN ĐỀ CẦN XỬ LÝ — SCMS.pdf`, đối chiếu báo cáo 21/10 và source hiện tại. Không sửa backend/frontend/SQL, không chạy migration hoặc thao tác DB live. Giữ nguyên các thay đổi SQL có sẵn trong workspace.
- PDF mới nói FE-01–11 đã làm bằng mock; đây là thông tin từ tài liệu, chưa phải nghiệm thu FE tích hợp. Checkout hiện tại chưa thấy các lời gọi mới như book-private/preview-recurring; cần đối chiếu commit/branch FE được bàn giao, không dùng báo cáo FE cũ để phủ nhận công việc ở branch khác.

| Mã PDF | Kết luận và hướng xử lý |
|---|---|
| BE-01 | Xác nhận lớp có thể Open thiếu lịch. Chọn Draft -> tạo đủ 12 -> publish hoặc API tạo nguyên khối trong transaction; kiểm tra đủ lịch ngay cả ở API enroll; thêm idempotency. Draft không phải lựa chọn bắt buộc duy nhất. |
| BE-02 | Xác nhận thiếu actor scope ở GET Session list/detail từ source, chưa xác nhận sự cố lộ dữ liệu live. Ép owner/assignment/center và tách dữ liệu lịch công khai với thông tin Member. |
| BE-03 | Xác nhận thiếu kiểm tra ngày Session nằm trong hạn gói. Hạn từ Paid phù hợp AGENTS hiện tại; PDF này không tự ghi đè yêu cầu 18/10 vốn tính từ buổi đầu. Cần chốt cách đếm ngày và boundary. |
| BE-04 | Chưa kiểm tra đủ specialization/Active khi book và thay Coach. Thêm liên kết Subject-specialization rõ ràng; áp dụng cả lúc phân công lớp Normal. |
| BE-05 | Khóa subscription chưa bảo vệ Coach/phòng dùng chung. Khóa tài nguyên theo thứ tự cố định, test concurrency DB và retry với idempotency key. Hủy lặp đã có lock Session và kiểm tra Cancelled, không kết luận hiện đang hoàn hai lần. |
| BE-06 | Xác nhận Subject suy Outdoor từ tên/slug. Thêm field persisted, backfill 9 môn và validation DTO; quy tắc Subject OR Room Outdoor đã có trong SessionSlotValidator. |
| BE-07 | Thiếu phân biệt chưa cấu hình và giá 0; Subject mặc định Active. Chốt miễn phí, validation giá/status; chặn tạo/mở bán từ Subject Inactive. |
| BE-08 | Service delete chưa kiểm tra dependency nghiệp vụ. FK và handler hiện có thể trả 409 chung; cần kiểm tra liên kết và thông báo rõ, không cascade-delete. |
| BE-09 | Thông tin seed 13 môn trong PDF là hiện trạng cũ: block seed Subject trong schema hiện tại đã là 9 môn. Chưa xác minh DB live sạch; dùng migration có điều kiện, giữ lịch sử, không giới hạn hệ thống tối đa 9 môn. |
| BE-10 | Vẫn xung đột giá, còn phát sinh seed mới theo bảng docs sai. Chốt giá bán; tách giá hiện tại với snapshot Class/Invoice; không dùng upsert seed ghi đè giá Manager hoặc hóa đơn cũ. |
| BE-11 | Xác nhận thiếu package-subject và validation packageType/credit. Đề xuất credit theo môn/đăng ký, không suy từ tên package; chốt trước khi đổi mô hình. |
| BE-12 | Room đã kiểm Available/capacity khi xếp lịch và chặn Maintenance/Closed khi có lịch tương lai. Bổ sung kiểm facilityType, ảnh hưởng khi đổi capacity/type, integration/concurrency; không ghi toàn bộ validation là chưa có. |
| BE-13 | Đã có dời lịch/gia hạn và audit; thiếu chốt ngoại lệ/quyền. Giới hạn gia hạn do lỗi trung tâm, lưu người/lý do/hạn cũ mới; không tự cho Member gia hạn. |
| BE-14 | confirm-cash đã yêu cầu Receptionist + MANAGE_INVOICES, khóa invoice, kiểm Pending/hạn và lưu người/giờ thu. Không tự mở quyền Manager; kiểm seed và test 401/403/lặp. |
| BE-15 | Cần chốt 5/15 phút; lỗi VNPay giữ suất sau Expired vẫn tồn tại. Giải phóng suất và tách ghi nhận tiền về muộn khỏi kích hoạt đăng ký, không bỏ dấu khoản đã thu. |
| BE-16 | Là quyết định kiến trúc tiên quyết, không nên xếp cuối chỉ vì ghi P2. Chốt mua credit rồi book hay giữ đủ 12 ca trước Paid trước khi thiết kế BE-01/03/05/11/15 và nghiệm thu Advanced. |
| BE-17 | Xác nhận timeline giả 2026-09 vẫn có. Trả [] khi rỗng, test dữ liệu rỗng và theo kỳ, không dùng mock ở API. |
| BE-18 | Xác nhận expiredMembers thực chất là không có gói Active hợp lệ. Đổi tên contract hoặc tách chưa mua/hết hạn/đã hủy; nhãn FE không thay thế sửa định nghĩa BE. |
| BE-19 | Chưa có sửa/xóa Holiday; bật ngày nghỉ không xử lý Session đã tồn tại. Chốt MVP, cảnh báo danh sách lịch bị ảnh hưởng và Manager xử lý có audit/thông báo, không tự dời hàng loạt. |
| BE-20 | Chưa có ghi chú Cash riêng, nhưng đã có invoiceCode/receptionist/paymentDate và audit. Chỉ thêm notes/mã biên lai riêng nếu cần, không tạo lại các trường đã có. |

**Phản biện thứ tự ưu tiên:** BE-16 cần chốt sớm song song BE-01/02; phần giải phóng suất/tiền về muộn BE-15 là rủi ro vận hành cao. PDF dùng chữ "đã chốt" là thông tin nguồn; không tự thay các quy tắc xung đột giữa PDF 18/10 và AGENTS. Chưa có quyết định mới của người dùng trong phiên này.

**Phát hiện bổ sung ngày 23/10 (từ source, chưa tái hiện DB live):**
1. `database/migrations/05_reseed_demo_users_and_coaches.sql:19` có TRUNCATE nhiều bảng (gồm invoices, sessions, system_logs, users) với RESTART IDENTITY CASCADE. Đây là script reset demo phá hủy dữ liệu, không phải migration bảo toàn; không chạy trên DB đang dùng. Tách khỏi chuỗi migration triển khai và chuyển thành seed cho DB demo riêng hoặc viết migration không phá hủy dữ liệu.
2. `database/schema_postgres.sql:928` và migration 05:192 seed lại bảng giá API contract đang lệch Member.pdf, ON CONFLICT cập nhật giá. Migration 04 đúng bảng giá Member.pdf nhưng chạy 05 có thể ghi đè ngược lại. Không coi việc docs và seed mới trùng nhau là đã đúng yêu cầu; cần chốt một nguồn giá và bỏ việc seed tự ghi đè giá vận hành.
3. `MemberPackageService.updateStatus:104`: chuyển Expired -> Active yêu cầu một hóa đơn Paid đã có, rồi nạp lại totalCredit khi số dư bằng 0. Có thể khôi phục 12 credit đã tiêu thụ mà không có thanh toán mới. Dùng state machine, quyền điều chỉnh có audit và ledger; đổi trạng thái/gia hạn không tự cấp thêm credit.
4. `SessionService.cancelPrivateSession:316`: nếu mất liên kết subscription, fallback chọn gói Advanced Active đầu tiên và không khóa lại gói fallback, có thể hoàn credit sai gói. Nên bắt buộc gói gốc, xử lý dữ liệu legacy có kiểm chứng và kiểm tra hạn/trần quyền hoàn.

**Điều chỉnh phát hiện cũ:** Lỗi tuple users thiếu INSERT trong AUD-17 ngày 21/10 đã được sửa ở source schema hiện tại; không tiếp tục coi là lỗi cú pháp còn nguyên. Chưa chạy SQL thực tế để xác nhận toàn bộ schema/migration hợp lệ. Các lỗi cũ ngoài BE-01–20 vẫn cần theo dõi: toggle khóa 7 ngày, Normal báo vắng, Coach private/thay thế, quyền Receptionist/CenterManager, waitlist thiếu invoice, lifecycle Completed, contract/CORS/Clock và script kiểm thử.

**Kiểm thử:** `.\mvnw.cmd clean test` ngày 23/10/2026: BUILD SUCCESS, 178 tests, 0 failures, 0 errors, 15 skipped (163 pass), tổng thời gian 1 phút 03 giây. Test xanh chưa chứng minh các phát hiện mới hoặc concurrency DB đã được xử lý. Chưa chạy live DB/concurrency hoặc nghiệm thu FE.

### [Đợt 21/10/2026] — Rà soát docs, code, test và xung đột hai PDF

**Phạm vi:** Đọc `AGENTS.md`, cả ba docs chính, `Member.pdf` (13 trang), `SCMS-Requirements-MVP-Role-Permissions-20261018.pdf` (65 trang); đối chiếu các luồng BE, controller/DTO, SQL hiện hành, test và các phần FE liên quan. Không dùng tài liệu `old-docs/`. Chỉ ghi nhận kết quả rà soát, chưa sửa code, test, SQL hoặc quyết định nghiệp vụ. Đây là rà soát có trọng tâm, không phải chứng nhận mọi endpoint hoặc triển khai live đều đúng.

**Kiểm thử thực tế ngày 21/10/2026:**
- `.\mvnw.cmd clean test`: BUILD SUCCESS; 178 tests, 0 failures, 0 errors, 15 skipped, tức 163 tests chạy thành công. Thời gian Maven 2 phút 34 giây.
- `npm run build` trong `FE_Project-main/`: thành công, Vite build 8,29 giây. Build không thay thế kiểm thử giao diện/nghiệp vụ.
- Đọc lại XML Surefire để xác minh số lượng. 15 test skip thuộc `DemoDataPreparationTest`, `LiveCashDemoTest`, `LiveReadOnlyAuditTest`, bật theo system property. Không bật các kịch bản live hoặc script tạo đơn; chưa xác minh DB Supabase/Render, callback VNPay thật, trình duyệt hay concurrency DB thực tế.

#### A. Docs có khớp Member.pdf không?

**Kết luận: chưa khớp đầy đủ; có sai giá và contract, đồng thời bản PDF tự có điểm mâu thuẫn.**

Giá VNĐ, mỗi ô theo thứ tự Normal / Advanced:

| Môn | Member.pdf trang 7 và migration 04 | API contract mục 2.1 hiện tại |
|---|---|---|
| Tennis | 2.500.000 / 8.000.000 | 1.500.000 / 10.000.000 |
| Bơi lội | 1.500.000 / 4.500.000 | 1.200.000 / 9.000.000 |
| Pickleball | 1.800.000 / 5.000.000 | 1.100.000 / 8.500.000 |
| Bắn cung | 2.000.000 / 6.000.000 | 1.400.000 / 9.500.000 |
| Bóng rổ | 1.200.000 / 3.500.000 | 1.000.000 / 8.000.000 |
| Bóng đá | 1.000.000 / 3.500.000 | 900.000 / 8.000.000 |
| Bóng chuyền | 1.000.000 / 3.500.000 | 900.000 / 7.000.000 |
| Cầu lông | 900.000 / 3.000.000 | 900.000 / 7.500.000 |
| Bóng bàn | 800.000 / 2.500.000 | 800.000 / 6.500.000 |

16/18 ô giá khác PDF (chỉ giá Normal của Cầu lông và Bóng bàn khớp). `database/migrations/04_migration_member_flow_v2.sql:35` seed đúng bảng giá PDF; chưa kết luận dữ liệu live đã được migrate. Không tự đổi giá kinh doanh đang công bố.

Các sai lệch API contract đã đối chiếu controller/record:

| Nội dung trong docs/01 | Implementation hiện tại |
|---|---|
| Mục 9.1: `POST /api/invoices/{id}/pay-cash` | `POST /api/invoices/{id}/confirm-cash`; cần cả `MANAGE_INVOICES` và role `RECEPTIONIST` (`InvoiceController`) |
| Mục 5.5: `POST /api/enrollments/{id}/cancel` | `PATCH /api/enrollments/{id}/cancel` (`EnrollmentController`) |
| Mục 6.1: `GET /api/member-packages/me` hoặc `?memberId=me` | Không có GET `/me`; GET danh sách chỉ cho staff và không nhận bộ lọc đó. API theo cá nhân là `/api/members/{memberId}/subscriptions` hoặc `/packages`, có kiểm tra owner (`MemberPackageController`) |
| Response đăng ký: `enrollmentId`, `amount`, `expiresAt` | `EnrollmentResponse` dùng `id`, không có `amount`/`expiresAt`; FE không lấy được countdown theo schema đã ghi |
| Coach response có `userId`, `fullName`, `bio`, `certification` | `CoachSummaryResponse` có `id`, `name`, `avatarUrl`, `specializations`, `email`, `phone`; thiếu dữ liệu kinh nghiệm/chứng chỉ trong endpoint này |
| Danh mục `/api/subjects`, `/packages`, `/classes`, `/rooms`, `/coaches` ghi Public | SecurityConfig chỉ permit GET `/api/public/**`; các URL nêu trên cần session |
| Tất cả lỗi có `{code,message}`; xung đột phòng/Coach là 409 | `GlobalExceptionHandler` chỉ tách code khi message có tiền tố chuẩn. Nhiều lỗi private booking/slot chỉ có message và status 400; các code như `OUT_OF_CREDIT`, `ADVANCED_PACKAGE_EXPIRED`, `ROOM_CONFLICT` không được phát đúng như docs |
| Mục 7.3 nói Manager/Admin gia hạn | Controller yêu cầu `MANAGE_SUBSCRIPTIONS`; seed hiện hành cấp quyền này cho Receptionist, không cấp mặc định cho CenterManager/Admin |

`docs/02-DEPLOYMENT-GUIDE.md` cũng chưa đủ để triển khai theo mô tả: `FRONTEND_URL` không được `SecurityConfig` sử dụng, CORS chỉ chấp nhận localhost/127.0.0.1. Chưa có cấu hình cookie cho FE khác site; cần kiểm chứng session cookie qua HTTPS khi triển khai. Phiên bản trong AGENTS/deployment là Spring Boot 3.3.3, `pom.xml` thực tế là 3.3.5. Property thực tế là `payment.expire-minutes`, không phải `scms.payment.expire-minutes`; env `SCMS_PAYMENT_EXPIRE_MINUTES` vẫn được map đúng.

Member.pdf có các điểm cần người dùng chốt, không thể xem toàn bộ PDF là một đặc tả nhất quán:
- Trang 11: bảng ca ghi Ca 3 mở toàn bộ, nhưng bảng phân loại môn ngay sau đó khóa Ca 3 và Ca 4. FR-024 trang 5 lại nói giờ cấm là chính sách đề xuất, chỉ bắt buộc khi bật cấu hình. AGENTS 2.5 hiện chốt khóa cứng cả hai ca.
- Trang 6 RP-13 cho phép người được phân quyền sửa giá công bố; trang 8 mô tả tự lấy giá Subject. Code ưu tiên giá Subject khi dương, không hỗ trợ override trong trường hợp đó.
- Trang 3 mô tả tính hạn từ thời điểm thanh toán; implementation lưu LocalDate và cho phép tới hết ngày endDate, chưa biểu diễn chính xác 45 x 24 giờ.
- Trang 4 yêu cầu Completed khi kết thúc khóa; code hiện thiếu luồng tự hoàn thành, ghi chi tiết tại B.

#### B. Lỗi và khoảng trống trong code/test hiện tại

Mức P1: có thể ảnh hưởng quyền truy cập, chỗ học, credit, điều kiện đăng ký hoặc vận hành chính. Đây là kết luận từ source và các test đang chạy, trừ mục concurrency chưa tái hiện với hai transaction DB thật.

| ID | Mức | Bằng chứng | Tác động / hướng xử lý đề xuất |
|---|---|---|---|
| AUD-01 | P1 | `SessionController:25,52`; `SessionService:49,66`; `SessionResponse` trả bookedMemberId/Name | GET lịch và chi tiết không nhận actor hoặc ép scope. Member có thể yêu cầu lịch của memberId khác hoặc đọc ca riêng của người khác; Coach cũng không bị giới hạn lịch được giao. Cần scope tại service và test owner/assignment. |
| AUD-02 | P1 | `InvoiceService:279`; `InvoiceServiceTest:169` | Hết hạn VNPay chỉ đổi Invoice sang Expired, giữ Enrollment Pending. Vì Pending vẫn chiếm chỗ, suất không được giải phóng sau 5 phút. Test đang khẳng định hành vi sai này; cần thay kỳ vọng theo policy được chốt. |
| AUD-03 | P1 | `AttendanceFinalizationProcessor:70`; `AttendanceFinalizationProcessorTest:114` | Vắng lần ba vẫn khóa 7 ngày; không có binding/kiểm tra `scms.booking-block.enabled`. Đặt env false theo docs không có tác dụng. Giữ code chế tài và test, bổ sung toggle cùng test cả hai chế độ. |
| AUD-04 | P1 | `SessionService:168-273`; `SessionRepository` conflict queries; trigger `database/schema_postgres.sql:668` | Booking khóa subscription riêng nhưng không khóa Coach/phòng/học viên dùng chung. Hai subscription khác nhau có thể cùng thấy không xung đột rồi insert trùng; trigger hiện hành không có overlap lock/constraint. Generate/reschedule/substitute cũng kiểm tra rồi ghi mà chưa khóa tài nguyên chung. Cần thứ tự lock nhất quán và test concurrency DB. |
| AUD-05 | P1 | `SessionService:196` và phần booking tiếp theo | Chỉ kiểm tra hôm nay chưa hết hạn, không kiểm tra ngày ca học <= endDate; có thể đặt ca sau hạn gói. Chưa kiểm tra account Active/bookingBlockedUntil của chủ gói trong luồng này. |
| AUD-06 | P1 | `MembershipPackage` không có liên kết Subject; `SessionService:210,219`; `substituteCoach:687` | Không ràng buộc gói Advanced với môn/class, không kiểm tra Coach có chuyên môn đúng môn khi book/thay. Gói có thể dùng cho lớp môn khác, Coach khác chuyên môn vẫn được chọn. Cần mô hình liên kết môn và validation chuyên môn. |
| AUD-07 | P1 | `TrainingResultService:188`; `AttendanceService:129` | Kiểm tra HLV dựa vào coach của Class và Enrollment Registered; không dùng coach/bookedMember của Session. Private booking không tạo Enrollment nên HLV của ca 1:1 hợp lệ có thể bị từ chối; HLV thay thế cũng không được cấp đúng scope. |
| AUD-08 | P1 | `AttendanceService:163`; `SessionController:74` | API báo vắng còn hoạt động cho Normal, đặt Excused khi báo trước 2 giờ; không chặn theo loại khóa. Cron giữ nguyên Excused. Trái quy định Normal không có báo vắng. |
| AUD-09 | P1 | `EnrollmentService:enroll`; `SportsClassService:apply`; `SessionService:generateRecurringSessions` | Cho đăng ký Ongoing; không chặn đã khai giảng/thiếu 12 buổi. totalSessions Normal nhận từ request tùy ý và generate dùng giá trị đó; có thể tạo khóa khác 12 buổi. Generate cũng không giới hạn chỉ Normal hoặc tổng số lịch đã có. Cần kiểm tra điều kiện mở bán và tính bất biến của chuỗi 12 buổi. |
| AUD-10 | P1 | `EnrollmentService:promote` và `statusForNewEnrollment` | Promote waitlist của lớp có phí sang Pending nhưng không tạo invoice. Người được đôn lên chiếm suất mà thiếu luồng thanh toán/hạn giữ chỗ để hoàn tất. |
| AUD-11 | P1 | `SessionController:58,67`; seed role_permissions trong `database/schema_postgres.sql` | Private booking/cancel chỉ cho MEMBER/ADMIN/MANAGER; Receptionist bị chặn dù hai PDF đều cho hỗ trợ. Role seed thực là CenterManager -> ROLE_CENTERMANAGER, không trùng ROLE_MANAGER. Gia hạn lại phụ thuộc quyền cấp cho lễ tân thay vì Manager. |
| AUD-12 | P1 | `SecurityConfig:corsConfigurationSource`; deployment mục 2 | FE domain trên Render/Vercel không được CORS cho phép dù đặt FRONTEND_URL; hướng dẫn deploy chưa khớp code. |
| AUD-13 | P2 | `AttendanceFinalizationProcessor`; tìm toàn source các phép gán Completed | Chốt attendance không tự chuyển Enrollment Normal sau buổi 12 hoặc gói Advanced học hết sang Completed. Package job chỉ Expired; chưa đủ yêu cầu Member.pdf 7.2. |
| AUD-14 | P2 | `TrainingResultController:30,36`; `TrainingResultService:71,141` | Receptionist có RECORD_RESULT và được ghi cả nội dung kết quả chuyên môn; kiểm tra thành viên thuộc buổi chỉ chạy cho actor Coach. Thiếu tách quyền điểm danh hỗ trợ với đánh giá của HLV. |
| AUD-15 | P2 | `InvoiceService`, `VnPayGateway`, `MemberPackageService`, `SportsClassService`, `ReportService` | Chưa nhất quán Clock: có nơi dùng zone trực tiếp, có nơi LocalDate.now() theo host. Có thể sai ngày nghiệp vụ và khó kiểm thử đồng bộ trên Render. |
| AUD-16 | P2 | `scripts/test-payment-and-totp.ps1`; `scripts/smoke-test-api.ps1` | Script thử nhiều mật khẩu mẫu; script payment in mật khẩu đăng nhập thành công. Payment test tự tạo đơn nếu thiếu, chỉ quan sát TOTP sau 5 giây, không assert rollover 30 giây/scan/replay; VietQR là dữ liệu mẫu. Smoke test không assert giá cụ thể/đủ field và không exit 1 ở cuối khi có bước lỗi. Không dùng hai script làm bằng chứng nghiệm thu thanh toán. |
| AUD-17 | P2 | `database/schema_postgres.sql` ngay sau INSERT users đầu tiên | Có block tuple users đứng độc lập sau `ON CONFLICT (email) DO NOTHING;`, thiếu `INSERT ... VALUES`, gây lỗi cú pháp nếu chạy nguyên file. Migration 04 dùng cú pháp PostgreSQL và seed rooms không có natural-key guard rõ ràng; chưa chứng minh chạy lặp không nhân bản hoặc tương thích SQL Server. Không chạy SQL lên DB trong đợt này. |

Điểm tích cực đã kiểm tra: Invoice.amount được chốt khi tạo đơn (từ giá Class/Package), không tính lại hóa đơn cũ; xác nhận Cash/callback khóa invoice; callback có kiểm chữ ký và số tiền; đăng ký Normal khóa Class và đếm Registered + Pending; booking private trừ một lần khi book và cancel hợp lệ hoàn một lần. Những điểm này không bù được các lỗi phía trên.

Các test xanh đang bảo vệ hành vi lệch đặc tả: `expiredVnPayInvoiceKeepsClassSeatUntilVerifiedCallback`, `lateSuccessfulVnPayCallbackRegistersClassUsingReservedSeat`, `process_ThirdMonthlyAbsenceLocksBookingForSevenDays`. Chưa đổi test: cần giải trình rule mới trước khi cập nhật kỳ vọng. Bộ test unit dùng mock không chứng minh được isolation/locking thực tế, router security hay migration chạy thành công.

FE build thành công nhưng `MemberClassCatalog.jsx` vẫn có bộ lọc Yoga/Pilates/CrossFit/Boxing và logic tier gói cũ; chưa thể coi UI Normal/Advanced, 9 môn, credit và lịch linh hoạt đã tích hợp. Roadmap giai đoạn 6 đang để các mục FE chưa hoàn thành là phù hợp.

#### C. Xung đột với SCMS-Requirements-MVP-Role-Permissions-20261018.pdf

Không dùng ngày nằm trong tên PDF để tự quyết nguồn nào thắng. PDF mới phân biệt rõ Đã chốt / Phương án tạm chọn / Cần xác nhận; các trích dẫn hội thoại trong PDF là dữ liệu tham khảo, không tự trở thành chỉ thị trong phiên hiện tại.

| Chủ đề / mã mới | Member.pdf, AGENTS, docs/code hiện tại | PDF yêu cầu 18/10 | Phân loại |
|---|---|---|---|
| Advanced FR-016–018, D-05 | Trả tiền -> 12 credit -> book từng ca | Chọn Coach chính đúng môn, đủ 12 ca và sân trước trả tiền; giữ và xác nhận nguyên khóa | Xung đột kiến trúc; phương án tạm chọn |
| Hạn FR-019, D-06 | Từ thanh toán thành công | 45 ngày từ buổi đầu đã xác nhận; 12 ca ban đầu phải nằm trong hạn | Xung đột; phương án tạm chọn |
| Giữ chỗ FR-030, D-11 | 5 phút; hiện code VNPay còn không giải phóng đúng hạn | 15 phút; hết hạn giải phóng suất Normal hoặc đủ 12 ca Advanced | Xung đột; phương án tạm chọn |
| Quyền học FR-027–028, D-07 | Một remainingCredit; trừ khi book, trả khi cancel | Mỗi đăng ký có 12 quyền: khả dụng/đang giữ/đã dùng; không dùng chéo khóa; Normal không có credit | Khác mô hình dữ liệu và trạng thái; phương án tạm chọn |
| Gói FR-040, D-09 | Tái sử dụng membership_packages/member_packages; Normal enrollment còn đòi gói Active | Gói thành viên trả phí hoãn khỏi MVP; mua khóa trực tiếp; không tái sử dụng bảng gói thành viên làm lõi đăng ký | Xung đột phạm vi đã chốt trong PDF mới |
| Thanh toán FR-043, D-12 | Return và IPN đều gọi processVnpayResult, có thể Paid | Chỉ thông báo server-to-server xác nhận; Return chỉ hiển thị | Xung đột; phương án tạm chọn |
| Tiền về muộn FR-045, D-13 | Callback Expired vẫn kích hoạt gói/suất; chủ yếu ghi audit PAID_AFTER_EXPIRY | Ghi tiền đã thu, đăng ký cần xử lý; Manager đối soát/lịch khác/hoàn thủ công ngoại lệ | Thiếu luồng ngoại lệ; phương án tạm chọn |
| Sự cố FR-022–024, D-08 | Dời record hiện tại; tự gia hạn; thay Coach chỉ kiểm role/trùng lịch | Coach chính ưu tiên; Coach thay đúng môn có Member đồng ý; lịch thay có liên kết cũ/mới, lý do, người duyệt và thông báo | Thiếu consent, specialization, liên kết lịch và thông báo |
| Nhịp Normal FR-011, D-04 | Các thứ tùy chọn; chưa buộc 3 ngày/tuần | 12 buổi, 90 phút, 3 ngày/tuần | Thiếu validation; đã chốt trong PDF mới |
| Đặt trước Q-02 | Code/docs chốt 12 giờ; Member.pdf ghi 12–24 giờ | Chưa có ngưỡng duy nhất | Cần xác nhận, không tự chốt |
| Báo trễ Q-03 / FR-021 | Khóa hủy dưới 24 giờ | FR mô tả có nút báo trễ vẫn mất buổi, Decisions đánh dấu câu hỏi chưa chốt | PDF mới tự phân biệt đề xuất và quyết định; cần xác nhận |
| Lễ/nắng Q-05 | AGENTS khóa cứng ca 3/4 và bỏ ngày lễ Active | Chính sách chưa áp đặt bắt buộc, cần chốt theo môn/sân | Cần xác nhận; Member.pdf cũng tự mâu thuẫn |
| Điểm danh Q-06, RP-35 | QR từ start-30 phút tới end; recordResult/correct có quy tắc khác | Cửa sổ ghi/sửa, người duyệt chưa chốt; quyền sửa chưa cấp khi chưa chốt | Cần xác nhận; không suy rộng cửa sổ QR cho mọi API |
| Giá Q-07 | Docs tuyên bố giá chuẩn nhưng khác PDF; seed theo Member.pdf | Chín giá chỉ tham khảo, cần chốt giá công bố/quyền override | Cần xác nhận bảng giá kinh doanh |
| Báo cáo FR-032/046, D-15 | ReportService có Paid/paymentDate và method, nhóm theo package | Theo môn, Normal/Advanced, tiền mặt theo lễ tân; tách tiền thu chưa xếp lịch và đối soát bàn giao | Thiếu chiều báo cáo/quy trình; chưa đáp ứng. ReportService còn thêm mốc cố định 2026-09 khi timeline rỗng. |
| Nội dung khóa FR-009/013 | Class/DTO chủ yếu tên, môn, giá, lịch, capacity | Mục tiêu, trình độ đầu vào, nội dung 12 buổi, kết quả mong đợi, chính sách trước mua | Thiếu dữ liệu/API/UI tương ứng |
| Check-in FR-041, RP-45 | Có lobby check-in lẫn session QR | Check-in cửa độc lập chờ phạm vi, không đồng nghĩa hoàn tất buổi | Cần chốt phạm vi, không gộp hai loại điểm danh |

Phân quyền trong PDF mới không thể đối chiếu chỉ bằng mã RP: mã đã thay ý nghĩa so với Member.pdf, ví dụ RP-45 cũ là hủy/hoàn tiền, RP-45 mới là check-in cửa. Cần map theo thao tác. Các lệch đã thấy: Receptionist bị chặn book/cancel Advanced; Manager sai role/private và quyền gia hạn; Coach xem lịch/attendance quá rộng nhưng không điểm danh đúng Session 1:1; Receptionist có thể ghi nội dung đánh giá. Không xác nhận quyền live chỉ dựa trên seed SQL.

#### D. Quyết định cần người dùng trước khi sửa nghiệp vụ

Theo AGENTS 2.9, dừng ở bước lựa chọn quy tắc khi hai nguồn xung đột. Đề xuất hai hướng cụ thể:
1. Giữ Member.pdf + AGENTS làm chuẩn vận hành: 5 phút, Advanced mua credit rồi đặt từng ca, 45 ngày từ thanh toán; sửa các lỗi code/contract đã xác định và ghi rõ các điểm chưa chốt trong PDF mới.
2. Chuyển sang đặc tả 18/10: xác nhận dùng cả các phương án tạm chọn D-05–08 và D-11–15; cần thiết kế lại đăng ký Advanced đủ 12 ca, quyền học theo đăng ký, hạn từ buổi đầu, holding 15 phút, late-payment reconciliation; bỏ điều kiện mua gói trả phí trước khóa, cập nhật AGENTS/contract/migration/tests đồng bộ.

Cả hai hướng đều cần quyết định rõ Q-02/03/05/06/07. Chưa áp dụng hướng nào. Trước khi bàn giao triển khai tiếp, cần kiểm thử tích hợp DB: tranh suất cuối, hai gói tranh Coach/phòng, hết hạn và tiền về muộn, Cash/callback lặp, owner/assignment, ngày ca ngoài hạn, no-show/hoàn credit và lifecycle Completed. Không kết luận hệ thống đã sẵn sàng chỉ từ 163 unit tests pass.

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
