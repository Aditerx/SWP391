# Đối chiếu thay đổi 10/10/2026 và khả năng demo SCMS

> **Cập nhật sau khi chốt phạm vi:** nội dung bên dưới là audit trước triển khai. Các lỗi invoice/subscription/hiệu lực gói/điểm danh đã được sửa, FE demo Cash đã nối và kiểm tra. Xem [PROGRESS_2026_10_10.md](PROGRESS_2026_10_10.md) để biết kết quả mới nhất và [DEMO_FE_BE_2026_10_10.md](DEMO_FE_BE_2026_10_10.md) để thao tác trên phiên bản hiện tại. Email OTP tạm hoãn, profile `demo` dùng `123456`; DB không cần migration mới cho đợt này.

Ngày kiểm tra: **10/10/2026**, giờ Việt Nam. Phạm vi: code backend trong `src`, frontend hiện có trong `FE_Project-main`, PostgreSQL cấu hình trong `.env`.

## 1. Kết luận ngắn

**Code đã có phần lớn chức năng mô tả trong `CHANGES_2026-10-10.md`, DB đã có các cấu trúc mới, nhưng chưa đủ điều kiện kết luận toàn bộ luồng demo chạy đúng.** Có lỗi tra cứu hóa đơn trên PostgreSQL, lỗ hổng kích hoạt gói chưa trả tiền và nhiều màn hình frontend còn dùng contract hoặc dữ liệu giả cũ.

Đã đọc `README_BE_CHANGES_2609.md`, `PROJECT_CONTEXT.md`, `CHECKLIST.md`, `CHANGES_2026-10-10.md`. Repo không có `PROGRESS.md`; đã đọc `PROGRESS 0310.md` và đối chiếu thêm `FLOW-PROGRESS.md`, `MIGRATION_FLOW1.md`, `FE-BE.md`, script SQL và demo cũ.

Chỉ thêm test, công cụ đọc schema và báo cáo; **chưa sửa code nghiệp vụ, frontend, dữ liệu hoặc chạy migration**, chưa commit/push. Dependency frontend đã được cài để build. Các thay đổi/xóa frontend có sẵn trong workspace được giữ nguyên.

## 2. CHANGES đã làm gì?

| Nhóm | Diễn giải dễ hiểu | Đối chiếu hiện tại |
|---|---|---|
| Ghi danh | Khóa lớp khi giữ chỗ; tính cả chỗ Pending để tránh vượt sĩ số. Lớp miễn phí vào học ngay; lớp có phí chờ thanh toán. | Có trong `EnrollmentService` và `SportsClassService`; khóa `PESSIMISTIC_WRITE` trong repository. Chưa stress test nhiều kết nối thật. |
| Mua gói | Tạo gói và hóa đơn Pending, lấy giá từ DB. Sau thanh toán mới kích hoạt gói. | Có trong luồng tạo đơn/confirm-cash; vẫn có đường cập nhật trạng thái khác cho phép vượt quy tắc này, xem mục 4. |
| Thu học phí | Cash do lễ tân xác nhận; VNPay có URL/callback, kiểm chữ ký và amount. | Có API/backend; frontend hiện chưa nối đúng. VNPay thực tế tạm gác theo yêu cầu. |
| Hết hạn | Sau 15 phút, invoice Expired; gói Pending bị hủy. Cash lớp nhả chỗ; VNPay lớp giữ chỗ chờ callback đã xác minh. | Có job mỗi 60 giây. Callback thành công muộn được xử lý với `PAID_AFTER_EXPIRY`. |
| Feedback | Member chấm lớp và mentor 1–5, mỗi lớp một lần; cần đã ghi danh và có Present. Manager xem tổng hợp; coach xem phản hồi không có trường định danh member. | Có module riêng `class_feedbacks`, guards, validation, audit và unique constraint; đã bổ sung test. |
| Schema/contract | Thêm bảng feedback, học phí, liên kết hóa đơn và index chống trùng. | Các cấu trúc trọng yếu đã có trên DB đang kết nối; không cần chạy lại migration chỉ dựa vào ghi chú cũ. |

Feedback hiện **gắn với lớp**, gồm điểm lớp và coach được phân công tại lúc gửi. Không có đối tượng khóa học hay endpoint đánh giá mentor độc lập ngoài lớp. `evaluations` là module HLV đánh giá tiến bộ học viên, khác `class_feedbacks`.

## 3. Những kiểm tra đã chạy

| Kiểm tra | Kết quả | Ý nghĩa/giới hạn |
|---|---|---|
| Bộ backend ban đầu: `mvnw.cmd test` | **32/32 pass**, 0 failures/errors | Chủ yếu unit test với repository mock; không chứng minh SQL chạy trên DB thật. |
| `ClassFeedbackAuditTest` mới | **9/9 pass** | Gửi hợp lệ, guards lớp/coach/enrollment/Present, trùng feedback, rating/comment và trường định danh trong response coach. |
| `BusinessGapAuditTest` mới | **3/3 tái hiện thành công** | Xác nhận 3 hành vi thiếu kiểm soát ở mục 4. Đây là test mô tả lỗi hiện có, không có nghĩa các hành vi đó đúng. |
| Bộ backend cuối sau thêm audit | **44 test chạy pass, 11 live test skip**, BUILD SUCCESS | Tổng Surefire: 55, failures/errors 0; live test chỉ bật bằng `-Dscms.live.audit=true`, kết quả lỗi DB vẫn được lưu riêng. |
| `LiveReadOnlyAuditTest` trên Supabase | **8 pass, 2 errors** trong đợt 10 test đầu | Member đọc hóa đơn, các kiểm tra quyền, catalog/enrollment, feedback và callback thiếu chữ ký hoạt động; hai test danh sách hóa đơn nhân viên lỗi SQL. |
| Test bổ sung tìm hóa đơn gói trên DB thật | **1 error** | Query lỗi kiểu tham số trước khi đến assertion kiểm tra đủ hóa đơn gói. |
| Frontend `npm.cmd run build` | **Pass** | Chứng minh biên dịch, không chứng minh API hoặc UI nghiệp vụ chạy đúng. |
| FE logic: G1, G1 guard, G2, FE2 membership/payment | **4 bộ pass** | Phần lớn fixture/mock; không chứng minh tương thích contract BE ngày 10/10. |
| FE logic: G4 | **Fail** | `test/g4_logic_test.mjs:1011` mong mã tự sinh `MB-0010`, mapper trả chuỗi rỗng khi DB/DTO không cung cấp mã. Test và mapper đang khác quy ước. |
| `git diff --check` | Pass | Kiểm tra whitespace đối với diff được Git theo dõi. |

Live test sử dụng Spring/MockMvc và repository thật, session test tạo từ user seed có sẵn; **không thử đăng nhập bằng mật khẩu, không gửi email**. Hikari chỉ đọc, `default_transaction_read_only=on`, worker hết hạn được mock. Không gọi mutation thành công vào DB thật. Vì vậy chưa kiểm chứng E2E tạo tài khoản → trả tiền → điểm danh → feedback có persistence.

Nguồn bằng chứng: `audit/schema-live.txt`, `audit/additional-tests.log`, `audit/invoice-search-regression.log`, `audit/business-gap-tests.log`, `audit/frontend-*-tests.log`; test mới ở `src/test/java/com/sportscenter/audit` và `src/test/java/com/sportscenter/feedback`.

## 4. Lỗi và phần thiếu cần xử lý

### P0 — Chặn hoặc làm sai luồng demo

1. **Tra cứu hóa đơn nhân viên lỗi SQL thật.** `InvoiceRepository.java:53` dùng các điều kiện `:param IS NULL`, `LOWER(:param)` và lọc thời gian. Đã tái hiện `function lower(bytea) does not exist` với filters trống/lọc Pending; truy vấn với filters chuỗi/thời gian được điền vẫn gặp `could not determine data type of parameter $16`. Cần query động/Specification hoặc bind/cast rõ kiểu, kèm integration test PostgreSQL. SQL còn sinh `INNER JOIN classes` từ đường dẫn tên lớp; cần đổi sang `LEFT JOIN` rõ ràng và kiểm tra không làm mất hóa đơn gói. Vấn đề bỏ sót chưa được assertion xác nhận vì query đang lỗi trước đó.

2. **Có thể kích hoạt gói chưa thanh toán qua API đổi trạng thái.** `MemberPackageService.java:100` chỉ chặn `Pending → Active`; `Cancelled/Expired → Active` không kiểm tra invoice Paid. Có thể đi `Pending → Cancelled → Active`. Cần kiểm soát mọi đường chuyển Active, validate status, và đồng bộ hủy subscription với invoice. Đã tái hiện đoạn `Cancelled → Active` không đọc hóa đơn bằng test mock.

3. **Đăng ký/OTP trên frontend chưa nối BE.** `LoginView.jsx:276,321` dùng timeout và mã cố định `123456`, rồi gọi tạo member; chưa gọi `/api/auth/register`, `/verify-registration`, `/resend-otp`, cũng chưa gửi password đăng ký theo contract thật. BE đã có các endpoint này. Cấu hình đang kiểm tra không có MAIL_USERNAME/MAIL_APP_PASSWORD; `EmailService` bỏ qua gửi nếu không có from-address, trong khi registration vẫn trả thông điệp chung. Cần cấu hình SMTP và kiểm tra nhận OTP bằng hộp thư dùng cho demo.

4. **Frontend mua gói/xem hóa đơn member sai route.** `membershipApi.js:56` POST `/api/members/{id}/packages`, nhưng BE chỉ cho `MANAGE_SUBSCRIPTIONS`; member cần POST `/api/members/me/subscriptions`. `MyMembershipView` đọc `/api/invoices?memberId=...`, nhưng BE yêu cầu `MANAGE_INVOICES`; member cần `/api/members/me/invoices`. Live test xác nhận hai route nhân viên trả **403** cho member. Response đặt đơn cũng cần mapper riêng cho `subscriptionId`, `invoiceId`, `invoiceCode`, `status`, `expiresAt`.

5. **Frontend thu tiền gọi API đã bỏ.** `invoiceApi.js:52,61,70` vẫn POST tạo invoice, POST `/pay`, PATCH `/status`; controller hiện có `/confirm-cash` và `/cancel`. Cần lấy invoice có sẵn do mua gói/ghi danh sinh ra, xác nhận Cash bằng session lễ tân, rồi đọc lại trạng thái.

6. **Ghi danh và điểm danh UI member/coach vẫn chủ yếu trong memory.** `SCMSContext.jsx:1522,1629` chỉ cập nhật React state, không gọi BE; `CoachAttendanceView.jsx:13` lấy ba member đầu thay vì roster theo session. `MemberClassCatalog` còn fallback member đầu; dữ liệu lớp thật được nạp chủ yếu cho manager. Cần load đúng member/lớp/enrollment/session theo role, thao tác API, hiển thị Pending, đọc lại sau F5 và bỏ fallback người khác trong API mode.

7. **Chưa có UI feedback/API adapter cho module mới.** Cần form member chấm lớp/mentor, xem lại feedback của mình, màn tổng hợp manager và phản hồi coach. Điều kiện hiển thị phải bám enrollment và Present từ DB.

### P1 — Đúng nghiệp vụ và dữ liệu kiểm thử

8. **Gói Active nhưng chưa đến ngày hiệu lực vẫn ghi danh được.** `EnrollmentService.java:80` kiểm tra status và endDate, bỏ startDate. Đã tái hiện gói bắt đầu sau 30 ngày vẫn được nhận. Cần `startDate <= hôm nay <= endDate`, thống nhất ngày Việt Nam; hiện enrollment dùng `LocalDate.now()` theo timezone host, payment dùng Asia/Ho_Chi_Minh.

9. **Lọc invoice theo memberId bỏ qua filters khác.** `InvoiceService.java:48` gọi riêng `findByMemberId`, không áp dụng status/method/keyword/date khi memberId được điền. Đã tái hiện bằng test mock.

10. **Điểm danh cần khóa đúng người/lớp và người thực hiện.** `TrainingResultService` nhận coachId/memberId từ body; chưa kiểm tra đầy đủ coach đang đăng nhập là người dạy lớp và member có enrollment Registered/Completed. Luồng ghi kết quả có thể ghi đè attendance cũ mà không reason. Đây là vấn đề từ đọc code, chưa thử mutation trên DB thật. Cần chặn Pending/chưa ghi danh và sửa quá khứ không lý do.

11. **Guard trùng lịch học viên chưa có trong EnrollmentService.** Tài liệu cũ nói đã có; service hiện không đọc sessions để đối chiếu lịch member. Trùng phòng/HLV trong SessionService là một quy tắc khác. Cần xác nhận yêu cầu này và bổ sung nếu demo bao gồm đăng ký hai lớp xung đột.

12. **Chuẩn bị dữ liệu demo mới.** DB có 23 lớp, tất cả học phí 0; 8 sessions đều trước ngày 10/10; 7 invoices đều là lịch sử chưa có subscription_id/expires_at/class_id; 1 invoice Pending không có payment_method nên không thể confirm Cash và không được job expires_at xử lý. Cần dữ liệu demo tách biệt cho lớp có phí, lớp miễn phí, buổi hôm nay/tương lai và invoice sinh bằng API mới; giữ dữ liệu lịch sử, không tùy tiện đánh dấu Paid.

13. **Cập nhật tài liệu/test cũ.** PROJECT_CONTEXT/PROGRESS ghi 100% chưa đúng với FE đang có; CHECKLIST/FLOW-PROGRESS ghi một số chức năng chưa làm trong khi BE đã có OTP/first-login/rate limit. README/FE-BE có đoạn nói hết hạn luôn nhả chỗ, mâu thuẫn ngoại lệ VNPay; đường dẫn frontend/cổng cũ cũng cần sửa. G4 cần chốt quy ước mã hội viên trước khi sửa assertion.

## 5. Schema DB nào đang dùng? Đổi để giải quyết gì?

**DB cấu hình cho backend local là PostgreSQL 17.6 trên Supabase, database `postgres`, schema `public`, qua pooler vùng ap-southeast-2.** Không phải SQL Server trong hướng dẫn cũ. `application.properties` dùng PostgreSQLDialect, `ddl-auto=none`; invoice code còn dùng `nextval`, nên backend hiện không thể coi là chạy song song SQL Server mà không điều chỉnh.

Không có Flyway/Liquibase hay bảng lịch sử migration trong repo/schema đã kiểm tra. Vì vậy không xác định được DB được tạo từ file nào hoặc chính xác thời điểm chạy script. **Theo các object thực tế, DB tương ứng cấu trúc Flow 1 + feedback + payment hiện hành**, gần `schema_postgres.sql`; không phải chỉ bản `schema_postgres - backup 0310.sql`. Tên một số constraint/trigger và default có khác snapshot, nên không khẳng định giống từng byte.

| Thay đổi | Script tham chiếu | Mục đích |
|---|---|---|
| failed_login_attempts, locked_until | flow1 step1 | Đếm sai mật khẩu/khóa tạm, chống thử đăng nhập liên tục. |
| centers, center_id users/rooms/classes/packages, guard center | flow1 step2 | Gắn tài khoản/tài nguyên với trung tâm, tránh thiếu chi nhánh. |
| is_first_login | flow1 step3 | Buộc đổi mật khẩu mặc định lần đầu. |
| email_otps, OTP hash/expiry/attempts/purpose | flow1 step5 | Xác thực đăng ký và đặt lại mật khẩu. |
| users.status thêm Pending | flow1 step6 | Chưa cho đăng nhập trước khi xác thực email. |
| subjects status/slug; specializations, coach_specializations; quyền admin | flow1 step7 | Danh mục public/bộ môn và chuyên môn HLV. |
| users.avatar_url/avatar_public_id | flow1 step8 | Lưu ảnh và định danh ảnh để quản lý upload. |
| class_feedbacks, unique(class_id,member_id), checks 1–5, coach index/FKs | migration_class_feedback.sql | Feedback tách khỏi evaluations, chống gửi trùng và tham chiếu sai ID. |
| classes.tuition_fee ≥ 0 | migration_payment.sql | Có học phí riêng theo lớp. |
| Pending trong member_packages/class_enrollments | migration_payment.sql | Mua gói/giữ chỗ trước, kích hoạt sau trả tiền. |
| invoices.class_id/subscription_id | migration_payment.sql | Xác định invoice cho lớp hay gói và đúng subscription cần kích hoạt. |
| invoice_code, sequence, created_at, expires_at | migration_payment.sql | Mã đơn, thời điểm tạo, hết hạn và đối soát callback. |
| Invoice statuses/methods mới, target constraint | migration_payment.sql | Thêm VNPay/Expired/Cancelled; invoice phải có đúng một mục tiêu package hoặc class. |
| Unique invoice_code, transaction reference, Pending(member,package) | migration_payment.sql | Chống mã trùng, callback/giao dịch lặp và hai đơn Pending cùng gói. |

Các object trọng yếu feedback/payment đã hiện diện trên DB; phiên kiểm tra này chỉ SELECT. DB cho phép giữ phương thức lịch sử BankTransfer/CreditCard/EWallet, nhưng **luồng tạo đơn mới chỉ nhận Cash hoặc VNPay**; VietQR/những phương thức khác trong UI/tài liệu cũ không đồng nghĩa đã có luồng thanh toán hiện hành.

`schema_postgres.sql` là bootstrap cho DB mới. `CREATE TABLE IF NOT EXISTS` không tự nâng cấp cột trong DB cũ; không chạy lại toàn bộ bootstrap/seed lên DB đang dùng để thay migration. Nếu dựng DB mới cho demo, kiểm tra toàn bộ flow1 và feedback/payment trên bản sao trước.

## 6. Chạy và kiểm tra

Backend, từ thư mục gốc, Java 17:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui.html`. Khi chạy ứng dụng thật, job hết hạn hoạt động và có thể cập nhật invoice Pending; đây là hành vi sản phẩm, khác chế độ live audit chỉ đọc.

Frontend hiện tại, terminal thứ hai:

```powershell
cd D:\Projects\SWP391\FE_Project-main
npm.cmd ci --no-audit --no-fund
$env:VITE_API_ENABLED = 'true'
$env:VITE_API_PROXY_TARGET = 'http://localhost:8080'
npm.cmd run dev
```

Mở URL Vite in ra, mặc định `http://localhost:5173`. Script dev của frontend mới không tự ép API_ENABLED=true như README cũ. Không dùng dev:local/OTP 123456 để kết luận persistence.

Các kiểm tra có thể chạy lại:

```powershell
# Unit/backend audit offline; không tự bật test DB thật.
.\mvnw.cmd -B -ntp test

# Opt-in đọc DB thật từ .env, mock worker, không ghi DB.
# Hiện dự kiến thất bại ở các test tra cứu hóa đơn vì lỗi đã nêu.
.\mvnw.cmd -B -ntp test '-Dtest=LiveReadOnlyAuditTest' '-Dscms.live.audit=true'

# Đọc schema/constraints/index và thống kê trạng thái, không in credentials.
& 'C:/Program Files/Java/jdk-17/bin/java.exe' -cp 'C:/Users/Admin/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar' audit/SchemaAudit.java
```

Trong `FE_Project-main`: `npm.cmd run build`, `node test/g1_logic_test.mjs`, `node test/g1_guard_test.mjs`, `node test/g2_logic_test.mjs`, `node test/g4_logic_test.mjs`, `node test/fe2_membership_payment_test.mjs`. G4 hiện còn failure như trên. Test đường dẫn/fixture pass chưa thay thế acceptance test API thật.

### Kịch bản Cash sau khi sửa, dùng dữ liệu demo riêng

| Bước | API/thao tác | Kết quả cần kiểm tra |
|---|---|---|
| 1. Member tự đăng ký | POST `/api/auth/register`, nhập OTP thật, POST `/api/auth/verify-registration` | User Pending trước OTP, Active sau OTP, có member subtype, mật khẩu đã hash. OTP sai/hết hạn bị từ chối. |
| 2. Đăng nhập | POST `/api/auth/login`; GET `/api/auth/me`; refresh | Cookie/session thật, đúng Member và đúng user; chưa mua gói thì không đăng ký lớp được. |
| 3. Mua gói | Member POST `/api/members/me/subscriptions` | 201; subscription Pending + invoice Pending, giá từ DB, có invoiceId/code/expiresAt; chưa mở quyền học. |
| 4. Thu tiền gói | Lễ tân POST `/api/invoices/{invoiceId}/confirm-cash` | Invoice Paid + đúng subscription Active trong transaction; member không tự xác nhận được; xác nhận lại trả 409. |
| 5. Chuẩn bị lớp/buổi | Manager tạo class có tuitionFee, assign coach, tạo session; chuyển lớp Ongoing khi phù hợp | Có lịch thật, phòng/coach không trùng, học phí được đọc từ DB. |
| 6. Ghi danh | Member POST `/api/classes/{classId}/enroll` với Cash | Có phí: enrollment Pending và invoice; Pending tính sĩ số. Miễn phí: Registered, không tạo invoice. |
| 7. Đóng học phí | Lễ tân confirm-cash invoice lớp | Paid + enrollment Registered; không kích hoạt nhầm membership. |
| 8. Điểm danh | HLV POST `/api/training-results` đúng session/member | Present lưu cả training_results và attendances; Pending/chưa ghi danh không được nhận. |
| 9. Feedback | Member POST `/api/classes/{classId}/feedback` | Lớp Ongoing/Closed, Registered/Completed, có Present: 201; gửi lần hai: 409. Manager xem trung bình; coach không có trường tên/ID/email member. |
| 10. Đối soát | Logout/login/F5, đọc invoice/enrollment/attendance/feedback, kiểm DB và audit | Trạng thái còn đúng sau tải lại; tổng doanh thu chỉ tăng một lần theo invoice Paid. |

Payload ví dụ (thay ID bằng dữ liệu thật, không mặc định ID luôn là 1):

```json
{"fullName":"DEMO Member","email":"<hop-thu-demo>","password":"DemoCash2026","phone":"0900000000"}
```

```json
{"email":"<hop-thu-demo>","otp":"<OTP-nhan-tu-email>"}
```

```json
{"packageId":2,"paymentMethod":"Cash"}
```

```json
{"paymentMethod":"Cash"}
```

```json
{"sessionId":12,"memberId":8,"coachId":4,"attendanceStatus":"Present","content":"Demo attendance"}
```

```json
{"classId":5,"coachId":4,"classRating":5,"coachRating":4,"comment":"Buoi hoc de theo doi, mentor huong dan ro."}
```

Các ca âm bắt buộc: gói Pending/expired/future; lớp đầy có chỗ Pending; gửi trùng; hủy Pending hủy invoice/nhả chỗ; Cash hết hạn/hủy/Paid không xác nhận lại; sửa amount từ FE không đổi giá DB; member truy cập invoice người khác; feedback trước Present, sai coach, rating 0/6/comment 1001 ký tự; điểm danh sai lớp/member; sửa điểm danh quá khứ không reason; F5 không mất dữ liệu. Chạy thêm hai request giữ chỗ cuối đồng thời và hai confirm Cash đồng thời trên DB test để chứng minh transaction/lock thật.

VNPay: giữ việc kiểm tra sandbox, signed callback, duplicate transaction reference, amount mismatch, thành công muộn và chỗ treo sau expiry trong đợt online sau; hiện chưa thực hiện giao dịch gateway hay thanh toán thật.

## 7. Phạm vi đề xuất để bạn chốt

**Đợt demo trước: sửa P0 + các P1 liên quan thanh toán/điểm danh/hiệu lực gói, nối FE thật, chuẩn bị dữ liệu và chạy acceptance Cash đầy đủ.** Hoàn thành theo thứ tự:

1. Sửa query invoice, kiểm soát kích hoạt/hủy subscription, guard hiệu lực gói và guard điểm danh; giữ integration test để bắt các lỗi đã tái hiện.
2. Nối registration/OTP, mua gói, hóa đơn member và confirm Cash; xử lý order DTO mới và readback Pending/Paid.
3. Nối catalog/ghi danh/học phí, session/roster/điểm danh, feedback member/manager/coach; kiểm tra persistence sau F5.
4. Chuẩn bị dữ liệu demo có phí/miễn phí và ngày hợp lệ, cấu hình mail demo, chạy E2E/ca âm/concurrency trên DB test; cập nhật hướng dẫn và sửa contract/test cũ.

Phạm vi đề xuất hiểu **“khóa học” là danh mục/bộ môn → lớp → buổi học**, học phí và feedback theo lớp như code đang có. Nếu bạn muốn khóa học độc lập gồm nhiều lớp, đăng ký/trả phí/feedback ở cấp khóa học, cần chốt mô hình đó trước vì hiện chưa có course entity hay enrollment tương ứng.

Demo **Cash trước** theo yêu cầu; online tạm gác. Việc thu tay BankTransfer/CreditCard/EWallet cần chốt thêm quy tắc xác nhận/đối soát nếu bạn muốn đưa vào demo; DB chứa enum lịch sử chưa đủ để chứng minh hỗ trợ. Chưa cần waitlist, TOTP QR, AI, drag/drop hay ticketing để đạt luồng demo này.

**Tiêu chí xong:** member mới đăng ký và đăng nhập bằng session thật; mua gói Pending → lễ tân thu Cash → Active; lớp có phí Pending → thu Cash → Registered; coach điểm danh Present; member gửi feedback lớp/mentor; manager/coach đọc đúng; F5/logout/login và DB xác nhận không mất dữ liệu, không thu tiền hai lần.

Chờ bạn chốt phạm vi sửa và cách hiểu “khóa học”; chưa triển khai các thay đổi sản phẩm này trong phiên đối chiếu.
