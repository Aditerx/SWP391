# 02-DEPLOYMENT-GUIDE.md — Hướng Dẫn Triển Khai Hệ Thống Lên Render & Supabase

> **Mục tiêu:** Cung cấp tài liệu chi tiết, từng bước cấu hình môi trường để triển khai Backend Spring Boot lên dịch vụ **Render**, kết nối cơ sở dữ liệu **Supabase (PostgreSQL)** và kịch bản kiểm thử trực tiếp cổng thanh toán **VNPay Sandbox**.

---

## 1. KIẾN TRÚC MÔI TRƯỜNG TRIỂN KHAI

* **Backend Service:** Render Web Service (Java 17, Spring Boot 3.3.5, Dockerfile hoặc Native Java Maven).
* **Database:** Supabase PostgreSQL (Managed Cloud Database).
* **Frontend Service:** Vercel / Netlify / Render Static Site (React + Vite).
* **Múi giờ:** Ép cứng `Asia/Ho_Chi_Minh` (UTC+7) qua Spring bean `Clock`.
* **Cổng thanh toán:** VNPay Sandbox API (VNPAY QR & Thẻ ATM nội địa demo).

---

## 2. DANH SÁCH BIẾN MÔI TRƯỜNG (ENVIRONMENT VARIABLES) TRÊN RENDER

Khi tạo mới một **Web Service** trên Render Dashboard, cấu hình các biến môi trường sau:

| Tên biến (Key) | Giá trị mẫu / Mô tả | Bắt buộc |
|---|---|:---:|
| `PORT` | `8080` (hoặc để Render tự cấp phát) | Có |
| `SPRING_PROFILES_ACTIVE` | `prod` (hoặc `default`) | Có |
| `DB_URL` | `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` | Có |
| `DB_USERNAME` | `postgres` (hoặc user Supabase của bạn) | Có |
| `DB_PASSWORD` | `<mat-khau-database-supabase>` | Có |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | Có |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` | Có |
| `SCMS_PAYMENT_EXPIRE_MINUTES` | `5` (Chuẩn hóa giữ chỗ thanh toán 5 phút) | Có |
| `SCMS_BOOKING_BLOCK_ENABLED` | `false` (Tạm tắt chế tài khóa 7 ngày theo AGENTS.md 2.7) | Có |
| `VNPAY_TMN_CODE` | Mã Merchant VNPay Sandbox (VD: `SCMS0001` hoặc mã từ VNPay cấp) | Có |
| `VNPAY_HASH_SECRET` | Khóa bí mật ký SHA512 VNPay Sandbox | Có |
| `VNPAY_PAY_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | Có |
| `VNPAY_RETURN_URL` | `https://<ten-backend-domain>/api/payments/vnpay/return` | Có |
| `FRONTEND_PAYMENT_RESULT_URL` | `https://<ten-fe-domain>/payment-result` | Có |
| `FRONTEND_URL` | `https://<ten-fe-domain>` (Dùng để cấp phép CORS) | Có |
| `MAIL_USERNAME` | `<gmail-cua-trung-tam>` | Bắt buộc để gửi OTP |
| `MAIL_APP_PASSWORD` | `<gmail-app-password-16-ky-tu>` | Bắt buộc để gửi OTP |
| `REGISTRATION_DEMO_OTP` | Để trống; chỉ đặt mã cố định trong môi trường demo | Tùy chọn |

`application.properties` đọc đúng hai biến Gmail `MAIL_USERNAME` và `MAIL_APP_PASSWORD`; các tên cũ `SPRING_MAIL_USERNAME` và `SPRING_MAIL_PASSWORD` không được đọc và không nên cấu hình. Host mặc định `smtp.gmail.com`, cổng `587`; SMTP có timeout 8 giây. Profile `demo` vẫn hoạt động và cũng chỉ bật OTP cố định khi đặt tường minh `REGISTRATION_DEMO_OTP`. Chế độ demo chỉ dùng cho đăng ký, không dùng cho quên/đặt lại mật khẩu.

Tạo Gmail App Password: bật xác minh 2 bước cho tài khoản Gmail, sau đó tạo App Password 16 ký tự. Không dùng mật khẩu Gmail thông thường. Trên Render, đặt `MAIL_USERNAME` và `MAIL_APP_PASSWORD` trong Environment; không ghi bí mật vào repo. Sau deploy, kiểm tra đăng ký bằng địa chỉ email kiểm thử và xác nhận nhận email OTP. Lỗi gửi mail được ghi log không kèm OTP hay thông tin xác thực; người dùng có thể yêu cầu gửi lại.

---

## 3. CẤU HÌNH BUILD & START TRÊN RENDER

### Phương thức triển khai: Docker (Khuyến nghị chuẩn cho Spring Boot trên Render)
* **Language (Runtime Environment):** Chọn **`Docker`** (Render không hỗ trợ runtime Java độc lập trong menu Language; tất cả Java/Spring Boot đều chạy qua Docker).
* **Dockerfile Path:** `./Dockerfile` (Render tự động nhận diện file `Dockerfile` ở thư mục gốc).
* **Docker Context:** `.` (Mặc định).
* **Build / Start Command:** Để trống (Render tự động vô hiệu hóa 2 ô này khi chọn Docker, vì đã được định nghĩa bên trong `Dockerfile`).
* **Health Check Path:**
  ```
  /api/health
  ```
  *(Endpoint này được cấu hình `permitAll` trong SecurityConfig, trả về HTTP 200 `{"status":"UP","message":"pong"}` giúp Render giám sát container).*

---

## 4. KỊCH BẢN KIỂM THỬ TRỰC TIẾP VNPAY SANDBOX (LIVE TEST SCENARIO)

### 4.1. Thông Tin Thẻ Demo VNPay Sandbox Chuẩn
Khi thực hiện thanh toán trên cổng VNPay Sandbox, nhập thông tin thẻ demo sau:

* **Ngân hàng:** NCB (Ngân hàng Quốc Dân)
* **Số thẻ (Card Number):** `9704198526191432198`
* **Tên chủ thẻ (Cardholder Name):** `NGUYEN VAN A`
* **Ngày phát hành (Issue Date):** `07/15`
* **Mã OTP xác thực:** `123456`

### 4.2. Luồng Kiểm Thử Hóa Đơn Khóa Normal (Lớp Nhóm 12 Buổi)
1. Đăng nhập tài khoản Member trên Frontend (hoặc gọi qua Postman).
2. Gọi `POST /api/classes/{classId}/enroll` với `{"paymentMethod": "VNPay"}`.
3. Nhận về URL thanh toán VNPay (`paymentUrl`). Hạn giữ chỗ là **5 phút**.
4. Mở `paymentUrl` trên trình duyệt: Chọn ngân hàng NCB $\rightarrow$ nhập thông tin thẻ demo trên $\rightarrow$ nhập OTP `123456`.
5. Sau khi thanh toán thành công: VNPay chuyển hướng về `VNPAY_RETURN_URL` $\rightarrow$ Return chỉ kiểm chữ ký và redirect; IPN hợp lệ mới ghi Paid và kích hoạt Registered. FE đọc hóa đơn đã xác thực; không coi redirect success là Paid.
6. Màn hình trả về kết quả `success` tại `FRONTEND_PAYMENT_RESULT_URL`.

### 4.3. Luồng Advanced theo môn/Primary Coach

1. Manager cấu hình work-schedule; Member POST advanced-registrations, Coach nhận rồi Member POST /{id}/order với VNPay (contract mục 6).
2. Mở paymentUrl trên gateway sandbox. Return chỉ đọc, IPN hợp lệ mới ghi Paid/Active/12 credit; chờ Invoice chính thức. Kiểm activatedAt/expiresAt = 45 ngày sử dụng có skip nghỉ TT, không chỉ phần ngày.
3. Coach chính tạo ca >=12h, có consent, chuẩn slot/room/Coach/Member/deadline. Credit còn 11.
4. Member hủy >=12h trả đúng 1 credit; <12h chỉ báo vắng trong 0<notice<12h, không hoàn. Kiểm retry không nhân quyền/credit.
5. Đây là hướng dẫn smoke **cần chạy riêng trên gateway/Render**; kết quả callback ký test local tại mục 10 không chứng nhận gateway live.

### 4.4. Kiểm Thử Sự Cố Hết Hạn Giữ Chỗ (Holding Window Timeout)
1. Tạo yêu cầu đặt chỗ/mua gói nhưng không nhập thanh toán, để quá **5 phút**.
2. Cronjob `PaymentExpirationJob` quét tự động:
   * Hóa đơn `Pending` chuyển sang `Expired`.
   * Chỗ đăng ký hoặc gói `Pending` được hủy và giải phóng tài nguyên.

---

## 5. LƯU Ý KẾT NỐI SUPABASE POSTGRESQL

* **Chế độ kết nối:**
  * Khuyến nghị dùng chế độ Session Pooler (port `5432`) hoặc Transaction Pooler (port `6543`).
  * Tham số bắt buộc trong JDBC URL: `?sslmode=require`.
* **Kích thước Connection Pool (HikariCP):**
  * Trên Render gói Free/Starter, thiết lập `maximum-pool-size=5` (đã được cấu hình trong `application.properties`) để tránh cạn kiệt connection pool của Supabase.
* **Migration Database:**
  * Đối chiếu theo đầy đủ tên file và trạng thái DB: `04_migration_member_flow_v2.sql`, `05_migration_notifications.sql`, `06_migration_chat.sql`, `06_schedule_safety_postgres.sql`, `07_session_subscription_postgres.sql`. Hai file prefix 06 là hai nội dung khác nhau; chat không thay schedule safety hoặc migration 07. Đây là hướng dẫn trước triển khai, không xác nhận đã chạy. DB mới đang trống cần kế hoạch khởi tạo schema/fixture riêng, không chỉ chạy migration nâng cấp.

---

## 6. CHECKLIST KIỂM THỬ TRƯỚC VÀ SAU DEPLOY (PRE & POST-DEPLOY CHECKLIST)

### 6.1. Trước khi Deploy (Pre-deploy)
- [x] Chạy lệnh kiểm thử toàn diện trên máy nội bộ:
  ```powershell
  .\mvnw.cmd clean test
  ```
  *Đảm bảo 100% test pass (178 tests run, 163 passed, 15 skipped, 0 fail).*
- [ ] Kiểm tra file migration `04_migration_member_flow_v2.sql` đã được chạy trên Database Supabase.
- [ ] Xác nhận không còn lệnh debug mang thông tin nhạy cảm.

### 6.2. Sau khi Deploy (Post-deploy Smoke Test)
- [ ] Chạy script kiểm thử khói tự động toàn diện:
  ```powershell
  .\scripts\smoke-test-api.ps1 -BaseUrl "https://<ten-app>.onrender.com"
  ```
  *Script sẽ tự động kiểm tra: `/api/health`, 7 ca học chuẩn & khóa ca 3, 4 ngoài trời, danh mục 9 môn và giá chuẩn, danh sách gói, lớp học, phòng tập, ngày lễ, và tạo mã TOTP 30s.*
- [ ] Chạy script kiểm thử riêng mã QR thanh toán và TOTP check-in:
  ```powershell
  .\scripts\test-payment-and-totp.ps1 -BaseUrl "https://<ten-app>.onrender.com"
  ```
- [ ] Kiểm tra log trên Render Console: Đảm bảo Spring Boot khởi động thành công, HikariCP kết nối ổn định, các cronjob (`PackageExpirationJob`, `PaymentExpirationJob`, `AttendanceFinalizationJob`) đăng ký thành công.

### 6.3. Quy Trình Double-Check Giữa Người Dùng & AI
1. Người dùng push code lên GitHub/GitLab liên kết với Render:
   ```powershell
   git status
   git add .
   git commit -m "feat: complete phase 5 backend jobs, incident handling and DTO enrichment"
   git push origin <ten-branch>
   ```
2. Render tự động build và deploy web service.
3. Khi Render báo `Live`, người dùng copy đường link Web Service (VD: `https://scms-backend.onrender.com`) gửi vào đoạn chat.
4. AI sẽ hỗ trợ phân tích log, hướng dẫn chạy smoke test hoặc xác thực toàn bộ các endpoint quan trọng để đảm bảo Backend sẵn sàng 100% cho Frontend kết nối.

## 7. TRIỂN KHAI BẢN SỬA AN TOÀN 23/10/2026

> D10-a đã chốt 5 phút ngày 25/10 tại docs 04 mục 21.3; chưa đổi env/deploy hoặc xác nhận cấu hình live.

1. Không chạy migration 05 như bước nâng cấp: đây là reset demo phá hủy dữ liệu, đã thêm guard chặn chạy vô tình. Chỉ dùng trên DB demo dùng một lần; không chạy schema/reset lên DB thật để lấy tài khoản mẫu.
2. Áp dụng migration 06 trước khi khởi động code mới: `database/migrations/06_schedule_safety_postgres.sql` cho Supabase hoặc `06_schedule_safety_mssql.sql` cho SQL Server; chỉ chọn một bản đúng hệ quản trị. Script thêm outdoor, reconciliation_required, normal_class_requests và Completed cho member_packages; không đổi giá/số dư/hóa đơn cũ. Kiểm tra constraint status tùy chỉnh ngoài schema hiện hành trước khi áp dụng.
3. Preflight: center/bảng nền tồn tại; kiểm tra `SELECT DISTINCT status FROM member_packages`. Trạng thái hợp lệ: Pending/Active/Expired/Cancelled/Completed. Custom Subject chưa phân loại cần Manager đặt outdoor; specialization.subject_id phải được gắn đúng. PostgreSQL chỉ backfill tên built-in khớp rõ ràng; SQL Server cần gắn chuyên môn qua quản lý. Không suy từ tên gói/email.
4. CORS nhận `FRONTEND_URL` hoặc `SCMS_FRONTEND_ORIGINS` (nhiều origin cách dấu phẩy). FE bật credentials. Với HTTPS khác site: `SESSION_COOKIE_SAME_SITE=none`, `SESSION_COOKIE_SECURE=true`; local mặc định lax/false. Phải thử bằng trình duyệt thực sau deploy.
5. Giữ `SCMS_BOOKING_BLOCK_ENABLED=false`. Chuẩn holding D10-a đã chốt 5 phút; kiểm override trên đúng môi trường trước rollout, không tự tính lại expiresAt đơn cũ.
6. Script dùng `SCMS_TEST_MEMBER_EMAIL`/`SCMS_TEST_MEMBER_PASSWORD`, không dò/in mật khẩu. Payment test chỉ tạo đơn nếu truyền `-CreateOrder -PackageId <id>` trên DB test; mặc định không tạo đơn. Rotation TOTP không thay thế kiểm thử quét/replay/Cash/IPN.
7. Các bài DB mới dùng H2 in-memory riêng, không thực thi SQL của PostgreSQL/SQL Server hoặc ghi Supabase. Nghiệm thu migration, DB thật và VNPay Sandbox còn phải chạy riêng.

```powershell
.\mvnw.cmd -DskipTests clean package
.\mvnw.cmd clean test
cd FE_Project-main
npm run build
```


## 8. F-06/F-07 — Session subscription và bằng chứng deploy

### 8.1. Preflight chỉ đọc

Xác nhận DB/branch/schema của đúng service Render từ cấu hình triển khai; không chia sẻ secrets. Lấy SHA từ Deploys và log quanh **23/10/2026 14:26:30 Asia/Ho_Chi_Minh** (07:26:30 UTC). Không dùng HEAD workspace thay SHA deploy.

```sql
BEGIN READ ONLY;
SELECT column_name, data_type, is_nullable FROM information_schema.columns
WHERE table_schema='public' AND table_name='sessions';
SELECT conname, pg_get_constraintdef(oid), convalidated FROM pg_constraint
WHERE conrelid='public.sessions'::regclass AND contype='f';
SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname='public' AND tablename='sessions';
ROLLBACK;
```

Chỉ khi cột đã có, kiểm thêm tham chiếu:

```sql
SELECT count(*) AS orphan_count FROM public.sessions s
LEFT JOIN public.member_packages mp ON mp.subscription_id=s.subscription_id
WHERE s.subscription_id IS NOT NULL AND mp.subscription_id IS NULL;
```

### 8.2. Nâng cấp bảo toàn dữ liệu

- PostgreSQL hiện hữu: sau 04/06 chạy riêng `database/migrations/07_session_subscription_postgres.sql`, ví dụ `psql -X -v ON_ERROR_STOP=1 -f database/migrations/07_session_subscription_postgres.sql` bằng kết nối đã kiểm tra. Không dùng reset 05 hay schema seed để nâng cấp.
- Migration 07 chỉ dành cho PostgreSQL schema public, không chạy trên SQL Server. Timeout lock 5 giây sẽ rollback khi bận; chọn cửa sổ thích hợp rồi chạy lại. Dừng và đối soát nếu có orphan, sai kiểu cột, FK không tương thích hoặc tên index/constraint bị chiếm; không sửa dữ liệu để vượt guard.
- FK/index tương đương có tên khác được tái sử dụng; FK NOT VALID tương đương được validate. Không tự backfill subscription lịch sử. Chạy migration lần hai rồi lưu catalog và ghi nhận migration đã áp dụng.
- DB mới: chọn một trong hai schema PostgreSQL đã cập nhật, sau đó 06/07. Schema chứa seed demo/cập nhật dữ liệu nên chỉ chạy trên DB mới tách biệt.

### 8.3. Kiểm thử PostgreSQL tách biệt

Cần Docker, Python, Java/Maven của dự án. Container/DB dưới đây phải chưa tồn tại; script không tự xóa DB để chạy lại. Port chỉ bind loopback, không dùng .env.

```powershell
docker run --name scms-f0607-postgres -e POSTGRES_PASSWORD=scms-local-test -p 127.0.0.1:55439:5432 -d postgres:17
docker exec scms-f0607-postgres pg_isready -U postgres
python scripts/test-session-subscription-postgres.py
.\mvnw.cmd clean test
```

Script kiểm schema gốc dựng mới và bản database/ bỏ phần sửa 07 để mô phỏng DB cũ; kiểm migration lặp, FK/index tên khác, rollback orphan/index sai định nghĩa, giữ dữ liệu/liên kết. Sau đó SessionSchemaPostgresTest chạy trên từng DB: Spring ddl-auto=none, entity thật, MockMvc qua security với authorities từ DB, mock cả ba job để không ghi nền. JVM test dùng Asia/Ho_Chi_Minh như Dockerfile, tránh alias timezone của host.

Đây là kiểm thử ứng dụng local với PostgreSQL thật, chưa phải probe HTTP qua Render hoặc nghiệm thu dữ liệu/phân quyền triển khai. Sau kiểm thử có thể dừng container riêng bằng `docker stop scms-f0607-postgres`; dữ liệu test được giữ để xem lại. Muốn chạy từ đầu, dùng container/volume test mới và giữ đúng tên/port như script yêu cầu.

### 8.4. Bằng chứng FE cần sau deploy

SHA deploy; stack trace/nguyên nhân xác nhận; catalog đúng DB Render và danh sách migration do người triển khai ghi nhận; probe mới cho Session list ở bốn vai trò, dashboard Manager và members/revenue đối chứng. Kiểm status, cấu trúc/dữ liệu, owner/assignment/center. Không ghi F-06/F-07 đã sửa live khi chỉ mới hoàn thành workspace.


### 8.5. Bằng chứng Render bổ sung 23/10/2026

URL người dùng cung cấp: https://scms-app-p1gy.onrender.com. Log lúc 01:28:28 UTC (08:28:28 UTC+7) xác nhận PSQLException: column s1_0.subscription_id does not exist. Đoạn log thiếu phần đầu nên chưa xác định endpoint/SHA, không trùng mốc FE probe 07:26:30 UTC.

Probe HTTP bắt đầu 12:23:02 UTC (19:23:02 UTC+7): Session list cả bốn vai trò, dashboard Manager và reports đối chứng đều 200. Probe phạm vi từ 12:23:54 UTC: Member/Coach đổi bộ lọc sang ID khác bị 403, anonymous schedule/dashboard 401. Coach dashboard 200 vì authorities live có MANAGE_CLASSES; khác seed workspace, chưa tự thay quyền. Bảng bằng chứng đầy đủ nằm trong be-request-f06-f07.md.

Không suy migration 07 đã chạy hoặc SHA nào đã được deploy từ status 200. Đợt làm việc này chỉ sửa repo, kiểm DB .env read-only và probe qua đăng nhập/GET/logout; chưa áp dụng SQL lên DB đang dùng hay deploy service. Lấy SHA và DB/migration history trước khi xác nhận bàn giao cuối.

## 9. Lịch sử kế hoạch và các lần kiểm trước hai phase

> Hạn Advanced có quyết định mới tại docs 04 mục 30.3: 45 ngày sử dụng, skip TT nghỉ/đóng, cập nhật gói đang dùng khi sửa ngày nghỉ, không trùng bù D08. Thay các tiêu chí 1080 giờ liên tục/không tăng hạn ở các đoạn kế hoạch cũ; chưa sửa code/migration/deploy.

### 9.1. Nguồn quyết định và vai trò

Chúng ta là phía BE chịu trách nhiệm thực hiện. docs 04 mục 0 là checkpoint session mới; mục 11–19 là quyết định/ghi chú triển khai nội bộ, mục 20.5 là log D09. docs 01 mục 14 theo dõi hợp đồng/UX phải bàn giao FE, mục 14.2 là gói bằng chứng. Không coi “handoff BE” là chờ một nhóm BE khác sửa, không lấy ví dụ smoke cũ làm policy mới.

D01–D08 đã chốt nhiều nghiệp vụ nhưng chưa code/migration; D09 log xác nhận LazyInitializationException Specialization#5/no Session, chưa fix. Những số test và probe ở các mục trước là lịch sử. Owner đã báo chạy migration 07 Supabase; kiểm DB .env thấy đủ cột/FK/index, chưa xác nhận DB đó là Render. Không chạy lại schema seed/reset 05 vì tưởng feature mới chỉ thiếu dữ liệu.

### 9.2. Preflight trước khi chúng ta sửa/triển khai

1. Kiểm git status/diff, branch và source SHA; giữ thay đổi/untracked và việc chuyển old-docs của Owner. Không dùng HEAD local làm SHA đang chạy trên Render.
2. Xác nhận phạm vi Dxx được yêu cầu thực hiện; không tự triển khai toàn bộ danh sách khi đang phân tích. Quyết định Owner mới hơn có hiệu lực hơn rule cũ; khi code phải đồng bộ AGENTS/contract/tests, không hỏi lại các lựa chọn đã chốt.
3. Kiểm môi trường/DB đích và manifest migration thực tế, phân biệt local/staging/Render; Sandbox VNPay vẫn có thể ghi DB. Dùng .env tại workspace khi cần, không sao chép secret vào biên bản bàn giao.
4. D09: tái hiện query/mapping specialization với OSIV=false, đủ dữ liệu và quyền. Không bật OSIV/EAGER toàn cục hoặc sửa DB chỉ vì proxy Specialization#5; xác nhận endpoint/frame bằng tái hiện hoặc full stack trước tuyên bố root cause theo route.

### 9.3. Migration, dữ liệu và phiên bản tương thích

- Dọn D02: chỉ 9 môn/giá đã chốt; ID 19–27 là snapshot một DB, phải kiểm lại tên/slug/giá/FK dưới lock. Xóa bản dư theo mapping đã cho phép, không cascade/reset; đối soát đúng fixture được Owner xác nhận và giữ Invoice Paid.amount. Chặn seed/import/API tái sinh bản dư/giá 0/ghi đè bảng giá.
- D03–D08: migration riêng cho timestamp/liên kết Subject–Primary Coach và dữ liệu xác nhận/báo vắng/sự cố/gia hạn/expiry snapshot cần thiết theo thiết kế lúc code. Chưa có số/tên migration được chốt; không đưa SQL giả vào checklist như đã chạy. Không suy giờ/môn/Coach legacy thiếu dữ liệu; dừng đối soát thay vì backfill tùy ý.
- PostgreSQL và SQL Server cần mapping/script phù hợp, guard tương thích và phép chạy lại; không dùng migration PG trên SQL Server. Tách bootstrap/demo khỏi nâng cấp DB hiện hữu.
- Chốt thứ tự schema → BE → FE theo field tương thích; nếu không tương thích, ghi rõ cửa sổ nâng cấp và phối hợp phiên bản. Không để FE bỏ nút cũ trước BE có policy mới, cũng không để BE mở quyền mới chỉ vì FE đã có màn.

### 9.4. Kiểm thử và nghiệm thu

- Sau mọi sửa BE: `.\mvnw.cmd clean test`; ghi kết quả mới, giải trình test đổi vì rule Owner (ví dụ 24 →12 giờ). Test cần thiết: phân quyền/center, idempotency/race/rollback, Paid/credit/expiry, biên 12 giờ và deadline 1080 giờ; không coi mock/H2 đủ chứng nhận migration PG/SQL Server hoặc live VNPay.
- D09: Manager/Member lấy danh sách và bộ lọc đúng, không có LazyInitializationException, Coach không specialization vẫn trả hợp lệ; contact theo quyền và center không mở rộng. Lưu bằng chứng dữ liệu đúng, không chỉ HTTP 200 trống.
- D01–D08: test theo tiêu chí ở từng mục docs 04, gồm UI tạo–dời chung, cũ-mới, bỏ dấu báo vắng đúng thao tác, hủy hoàn một lần, sự cố/bù không trùng, phục hồi đúng số dư và trạng thái Chờ khắc phục. Rule chưa chốt không sửa expected để test xanh.
- Deploy đúng SHA khi được yêu cầu, lưu migration/config đã áp, kiểm lại `/api/health`, login/CORS/cookie và probe từng feature. Chỉ đóng tích hợp khi FE chạy browser với API thật; lỗi API không dùng mock thay thế.

### 9.5. Bàn giao FE và khôi phục khi triển khai lỗi

Theo mẫu docs 01 mục 14.2: cung cấp SHA nguồn/deploy, URL/môi trường, API/DTO/quyền/error thật, migration/config, fixture đúng môi trường và kết quả local/live/FE cần chạy. Đổi nội dung ở mục endpoint cũ khi implementation đã đổi, giữ trạng thái chưa code/chưa deploy/chưa FE verified riêng biệt. D09 dự kiến sửa nội bộ BE, FE vẫn dùng GET /api/coaches và response hiện có trừ khi có thay đổi được công bố.

Lưu schema/data cần đối soát trước migration; nếu deploy lỗi, ưu tiên bản BE tương thích schema hoặc dừng phần thay đổi theo kế hoạch đã kiểm. Không rollback bằng reset seed, xóa hóa đơn, khôi phục credit toàn gói về 12 hoặc hoàn tác SQL phá dữ liệu. Rollback cụ thể phụ thuộc migration thật và phải được kiểm khi xây bản triển khai; mục này chưa chứng nhận có script rollback chạy được.

### 9.6. D09 — trình tự tái hiện/sửa và sổ bằng chứng đang thiếu

Kế hoạch kỹ thuật đầy đủ: docs 04 mục 20.6; API/FE: docs 01 mục 4.5 và 14.3. D09 dự kiến chỉ sửa Java query/service/controller và test, chưa có căn cứ cần migration/config mới. Giữ OSIV=false. Chuẩn bị test H2 in-memory riêng, không nạp .env, không transaction test bao quanh request; full context phải vô hiệu job ghi nền. Seed chỉ trong DB test đó. Không dùng script F-06/F-07 hoặc schema bootstrap để tái hiện lỗi proxy.

| Bằng chứng | Trạng thái tại lượt chuẩn bị 25/10 |
|---|---|
| Source workspace | `tai` / `8e95759` cùng thay đổi chưa commit; không phải SHA Render |
| Log runtime | Ba lỗi LazyInitializationException Specialization#5 tại docs 04 mục 20.5; chưa có URI/frame ứng dụng trong đoạn đã lưu |
| Tái hiện route trước sửa | Đã lập kịch bản ở 20.6; chưa chạy |
| Bản sửa/test local | Chưa sửa BE, chưa chạy `.\mvnw.cmd clean test` trong lượt này |
| DB/migration | Không truy cập/mutation DB; D09 chưa đề xuất migration |
| Deploy/probe/FE | Chưa deploy, chưa probe mới hoặc FE verified; không dùng kết quả lịch sử để đóng D09 |

Thực hiện sau theo thứ tự: tái hiện route trước sửa → service read-only transaction + query đúng center/batch specialization → regression dữ liệu/quyền/center/query count → clean test → cập nhật API/bằng chứng local. Chỉ khi được yêu cầu triển khai mới deploy/probe đúng SHA và nghiệm thu FE. Với bản sửa Java không đổi schema dự kiến, khôi phục bằng bản BE trước tương thích nếu cần; không sửa DB để rollback lỗi lazy.

### 9.7. D10-a — cấu hình holding cần đối chiếu sau quyết định

Owner đã xác nhận 5 phút tại docs 04 mục 21.3. Source đọc `payment.expire-minutes=${SCMS_PAYMENT_EXPIRE_MINUTES:${PAYMENT_EXPIRE_MINUTES:5}}`; không mặc định key `scms.payment.expire-minutes` trong AGENTS được bind. Chưa kiểm giá trị live hoặc sửa biến môi trường. Bước chuẩn bị triển khai phải ghi riêng giá trị đích 5 và giá trị thực của đúng service/SHA (ẩn secrets), ảnh hưởng đơn Pending đang có deadline, rồi mới lập thay đổi cấu hình/test tương thích; chưa tự tính lại expiresAt hóa đơn cũ.

D10-b đã chốt A tại docs 04 mục 22.3: chặn online khi thiếu method/deadline, đối soát trước xử lý đơn cũ/tạo đơn mới, không mở lại hóa đơn cũ. Chưa cho phép sửa Invoice 3 hay tạo fixture/đơn Sandbox trên DB đang dùng. Khi tới bước xác minh, kiểm nguồn tạo/liên kết và SHA chỉ đọc trước; không tự backfill VNPay/hạn now+5 để vượt 409. Chưa mở test live opt-in hoặc deploy bản thay guard. D10-c: Owner xác nhận FE_Project-main là bản tham khảo rất cũ, chỉ bàn giao yêu cầu để FE kiểm trên source hiện hành (04 mục 23.3), chưa có bằng chứng nghiệm thu FE.

D10-d1 đã chốt A tại 04 mục 24.3; hiện source vẫn cho cả hai cập nhật, chưa triển khai thay đổi. Mục tiêu IPN cập nhật/Return không ghi thanh toán; rollout phải xác minh HTTPS/IPN merchant cấu hình đúng service, khả năng truy cập/retry và test Return đến trước/sau IPN, không Return vẫn nhận thanh toán, callback lặp không cấp quyền hai lần. Chưa thay cấu hình hoặc gửi callback thật. Query redirect status hiện không đồng nghĩa Paid. D10-d2 chốt A tự refresh hữu hạn rồi Kiểm tra lại tại 04 mục 25.3. Kiểm màn chờ với source FE hiện hành, không dùng FE_Project-main cũ.

Email Member là yêu cầu mở rộng tương lai: chuẩn bị nguồn kết quả sau commit/chống trùng, không gắn gửi mail vào request polling/Return hoặc buộc SMTP thành công mới ACK thanh toán. Chưa triển khai outbox/migration/provider/template và chưa gửi email. Khi làm cần kiểm rollback/retry/chống mất-trùng riêng, không coi JavaMailSender hoặc REQUIRES_NEW là bảo đảm sau commit.

D10-e1 chốt A tại 04 mục 26.4: tại lúc xử lý đã tới/quá deadline hoặc đơn không còn Pending thì ghi tiền/cần đối soát, không tự kích hoạt; giữ idempotency cho Paid. Test cần có đúng deadline, IPN trong/sau hạn và race với expiry job; chứng cứ gateway trả trong hạn không tự đổi quyết định. D10-e2a chốt A ở 04 mục 27.3: CenterManager duyệt, Receptionist hỗ trợ; giữ guard phạm vi dữ liệu hiện có, không thêm role Center hoặc mô hình nhiều chi nhánh. Chưa có API hoàn tất đối soát/hoàn tiền/cấp lại quyền, chưa thực hiện nghiệp vụ này trên môi trường thật.

D10-e2b đang chờ chọn kết quả cấp quyền hợp lệ/hoàn tiền hoặc chỉ hoàn tiền (04 mục 28.2). Chưa thiết kế migration/hồ sơ hoàn, gọi API refund hoặc mở quyền Manager mới. Khi triển khai phải giữ Invoice.amount, liên kết khoản Paid gốc, kiểm không tính tiền/cấp credit hai lần và không vừa cấp quyền vừa hoàn; đối chiếu hạn Advanced 1080 giờ, không tự reset hạn để tiện fixture. Quyết định chưa chốt không dùng làm lệnh mutation DB.

Đánh giá tiếp theo tại 04 mục 28.3–28.4 gộp kết quả/kênh hoàn thành ba hướng: refund-only ngoài hệ thống có bằng chứng (đề xuất theo ưu tiên MVP), refund-only tích hợp gateway, hoặc thêm nhánh cấp quyền có kiểm soát. Chưa chọn/triển khai. Hoàn ngoài hệ thống vẫn cần hồ sơ thực hiện/xác nhận, audit/idempotency và báo cáo tiền nhận/hoàn; không dùng việc đổi Invoice.amount/status tùy ý thay chứng từ. Chưa có migration/API/ủy quyền thao tác hoàn tiền thật.

Quyết định cuối tại 04 mục 28.5: Owner chọn hướng 1 (hoàn đủ ngoài SCMS, lưu bằng chứng trong SCMS) cho MVP. Refund API/cấp lại quyền để nâng cấp sau; hồ sơ hoàn tối thiểu vẫn cần triển khai. Chưa hoàn tiền thật hoặc xác nhận dữ liệu nào đã hoàn, chưa đổi Invoice.amount. Các đoạn chờ chọn trước là lịch sử phân tích.

### 9.8. D11 — lựa chọn môi trường trước tích hợp

Đang chờ hướng tổng thể tại docs 04 mục 29.2: (1) local PostgreSQL cô lập và FE hiện hành, sau đó smoke deploy có kiểm soát — đề xuất project; (2) staging BE/DB riêng; (3) deploy/DB hiện có với fixture giới hạn. Chưa tạo service/DB, chạy migration, thay .env hay deploy. Mọi hướng đều phải kiểm source/deploy SHA, DB đích, cấu hình và manifest migration; không coi local xanh là HTTPS/IPN cloud đã đúng. Quyền mutation cụ thể trên cloud chưa được tự suy từ quyết định chọn hướng.

**Quyết định cuối D11:** Owner chốt 1 tại 04 mục 29.3; local PostgreSQL/fixture riêng rồi smoke deploy có kiểm soát. Không tạo staging mặc định, không thay .env đang dùng, không chứng nhận IPN thật từ callback mô phỏng. Đoạn chờ chọn trên là lịch sử; chưa thực thi setup/test/migration/deploy trong lượt chốt plan.

D12 đang đối chiếu giờ ca PDF lệch AGENTS/TimeSlot và nghĩa ngày nghỉ “không trừ” (04 mục 30). Chưa đổi enum/time-slot/config/Holiday dữ liệu. Test local sau sửa phải bao gồm tag active/inactive, 12 buổi và Advanced không trừ credit khi bị từ chối; thời hạn 1080 giờ không tự tăng. Không chạy cập nhật Holiday có tác động lịch cũ khi chưa xử lý D14.

**Quyết định mới D12 tại 04 mục 30.3 thay tiêu chí hạn trên:** skip thời gian TT đóng, thêm/tắt ngày nghỉ cập nhật hạn gói đang dùng, chống trùng D08. Cần test tính lại idempotent, nhiều ngày nghỉ liên tiếp/ngày nghỉ nằm trong phần hạn được kéo thêm, hợp nhất khoảng bù và expiry theo deadline mới. Không tự backfill gói cũ thiếu timestamp/căn cứ nghỉ hoặc rút quyền lịch sử; biên ngày đã nghỉ/lịch đã đặt còn chờ 30.4. Chưa có migration, setup/test/SQL/deploy trong lượt này.

Owner đã đồng ý biên D12 tại 04 mục 30.5: giữ skip thực tế quá khứ, cập nhật tương lai, phát hiện ca vượt hạn và xử lý trước khi xác nhận Holiday (D14). Test phải bảo đảm rollback toàn bộ nếu tác động chưa giải quyết, không tự hủy ca hoặc rút quyền quá khứ. D13 mới phân tích chỉ số người/đăng ký tại 04 mục 31; chưa đổi report/DTO/schema hoặc chạy test/probe mới.

D13 đã chốt cả hai, UI ưu tiên người tại 04 mục 31.3; chưa code/probe. D14 đang chờ workflow báo ảnh hưởng/xử lý riêng rồi xác nhận, preview phương án áp dụng chung hoặc tự dời hàng loạt tại 32.2. Đề xuất hướng 1; chưa đổi Holiday/lịch/hạn, migration hoặc quyền. Test sau sửa phải kiểm preview cũ, booking chen vào, retry toggle và atomicity Holiday/deadline; không dùng thay đổi ngày nghỉ thật để thử thuật toán chưa chốt.

D14 Owner chọn 1 tại 04 mục 32.3, thay đoạn chờ chọn trên; chưa triển khai. D15 đang đối chiếu attendance/check-in cửa/quyền sửa sau buổi ở 04 mục 33.2. Test sau thay đổi cần Clock cố định kiểm biên scan 30 phút/10 phút/kết thúc, Coach assigned/CenterManager, race finalization/correction, audit/TrainingResult và không nhân credit; chưa sửa test/DB/live hoặc xóa code check-in cửa.

D15 phạm vi đã chốt tại 04 mục 33.3: attendance trước, UI check-in cửa ưu tiên nâng cấp sau. Owner chưa chốt Late/30–10; ba cấu hình chờ 33.4. Các mốc test trên chỉ phản ánh source/đề xuất trước, không phải acceptance mới; sau quyết định cập nhật test có giải thích business change. Giữ dữ liệu Late lịch sử, không chạy SQL đổi/xóa bản ghi để giản lược UI.

Owner đã chốt cấu hình 1 tại 04 mục 33.5: scan/attendance mới trong giờ buổi, Present/Absent, không Late/30 phút sớm. Test trước/sau giờ, finalized race, correction và tương thích dữ liệu Late cũ; chưa code/chạy test mới. D15 phần sự cố báo sau buổi chờ chọn tại 34.2, chưa cấp bù/hoàn credit/đổi lịch sử hay migration trên DB thật.

D15 sự cố báo muộn đã chốt 1 tại 04 mục 34.3; cần kiểm race/double compensation/Normal đủ 12/Advanced đúng credit gốc, giữ lịch sử và không cộng hạn trùng D08/D12. Chưa mutation/test mới. D16 đang chờ phạm vi Cash/sửa sai ở 35.2; không dùng fixture Paid hoặc thu thật để thử đảo, không mở CenterManager thành thu ngân và chưa sửa amount/chứng từ/Active trực tiếp.

Đánh giá D16 hướng 2 tại 04 mục 35.3 (26/10) chưa phải quyết định triển khai. Cần hồ sơ điều chỉnh/report và test lặp/race booking, lịch sử dùng rồi hoàn về 12, liên kết thiếu/giao dịch phụ thuộc. Không dùng tiền hoàn thật để giả lập Paid nhầm, không tự hủy Normal vì chưa có ngoại lệ nghiệp vụ này. Khi sửa sẽ chạy clean test và PG cô lập theo D11, chưa chạy migration/SQL/deploy.

D16 đã chốt 2 hẹp tại 04 mục 35.4, thay đoạn chờ trên; chưa triển khai. D17 QR handoff tại 36, chưa FE verified: GET có thể ghi secret lần đầu, kiểm account local trước probe cloud. D18 quyền dashboard/report chờ 37.2; không thay quyền Coach/Admin DB live để làm expected test khớp. Test/probe phải ghi source/deploy SHA, scope và source FE đúng bản, không chỉ status 200.

D18 đã chốt 1 tại 04 mục 37.3: quản trị CenterManager/Admin có quyền report, Coach nghiệp vụ riêng; source chưa thay. D19 kiểm thử theo 04 mục 38: baseline/regression/clean test/PG riêng/FE đúng bản/cloud đúng SHA, không bật live opt-in mặc định hoặc build song song chung dist. Chưa chạy tests/probe mới. Báo cáo tiền chốt ba số tại 39.3, FE quyết định trình bày/chỉ số chính; không dùng tổng Paid cũ làm bằng chứng thu ròng đúng. Test cần nhận/hoàn khác kỳ, hồ sơ chỉ duyệt/chưa hoàn, Cash ghi nhầm và chống nhân tổng; chưa API/DTO/migration hoặc deploy mới.

### 9.9. Hai phase, đồng đội và bằng chứng demo — chưa triển khai

Phân công chi tiết docs 04 mục 40: T (D01/D09) ~15%, Phase 1 nền/mua–thanh toán/quyền ~40%, Phase 2 dùng quyền/lịch/sự cố/attendance/đối soát/report ~45%; ước lượng tương đối, chưa ngày công. Mốc chung phải chứa dirty/untracked docs quyết định đã được bàn giao đúng, không dùng mỗi HEAD 8e95759 thay cho baseline mới; chưa tự commit/push. Mỗi người nhánh/checkout riêng, schema/migration/.env/central docs một người phụ trách, build/test không chung target/dist.

Mỗi lần sửa BE chạy clean test; milestone thêm PG local riêng và kịch bản HTTP có Auth thật, kiểm DB trước-sau. Sau merge T chạy kiểm lại nhánh tích hợp; chỉ bắt đầu P2 sau cổng P1. Baseline lỗi cũ và test expected thay theo rule phải giải trình, skip ghi lý do, không lấy số test cũ làm acceptance mới.

Evidence đề xuất `artifacts/demo/phase-1/<run-id>/` và phase-2: manifest source/commit/config an toàn/migration/fixture/command, HTTP status/body đã bỏ secret, DB read assertions, logs/report test và giới hạn. Đây là path dự kiến chưa có kết quả demo mới. Phân loại local unit/integration, HTTP thật local, gateway Sandbox live, deploy/FE browser; callback ký bằng secret test không chứng minh VNPAY thật đã gọi IPN. Không SQL Paid/Active để giả flow, không WithMockUser để chứng minh login thật, không FE_Project-main cũ để chứng minh FE hiện hành.

Không mặc định P1 là release vận hành đầy đủ: Holiday đổi có tác động/booking/sự cố/hoàn/sửa Cash/report cuối chờ P2. Chỉ deploy khi được yêu cầu, kiểm DB/SHA/config/migration/fixture rồi smoke HTTPS/CORS/cookie/IPN đúng merchant và source FE đúng bản. Thiếu nguồn/quyền thì đánh dấu pending, không đổi nhãn evidence local thành live. Lượt lập phân công chưa chạy Maven/PG/setup/SQL/deploy hoặc tạo artifact demo.

### 9.10. Rà bàn giao và kiểm baseline thực tế 27/10/2026

Owner yêu cầu review hướng dẫn/code, commit/push tai rồi kiểm kết nối .env mới chỉ đọc. AGENTS đã có checkpoint ưu tiên quyết định mới; 04 mục 40.8–40.9 bổ sung checklist đồng đội và điều kiện hoàn thành. Chưa triển khai D01/D09 hoặc các flow Phase 1/2.

- `.\mvnw.cmd clean test`: BUILD SUCCESS, 223 tests, 204 pass, 19 skip, 0 failure/error; đã kiểm 37 XML Surefire trước lượt PG opt-in. Skip gồm các nhóm live/prepare/demo và PG opt-in, không bỏ test lỗi để xanh.
- PostgreSQL Docker 17.11, container mới `scms-f0607-review-20261027`, hai DB local riêng: `scms_f0607_fresh`/`scms_f0607_upgrade`. Mỗi DB 7 tests ứng dụng pass (tổng 14), không fail/error/skip. SQL kiểm dựng mới/nâng cấp, lặp migration, FK/index tên khác, NOT VALID, orphan/index collision/FK cascade rollback và snapshot dữ liệu không đổi.
- Script có thêm `--container scms-f0607-<unique-name>` để chạy trên container test mới mà không thay/xóa container cũ. Port DB test vẫn 127.0.0.1:55439, Java test vẫn guard URL local; không dùng .env cloud. Container review đã dừng, giữ DB để đối chiếu; container cũ không thay đổi.
- Schema gốc và bản database/ đồng nhất. File tài liệu Owner đã chuyển old-docs khớp hash nội dung (khác newline được chuẩn hóa), không phục hồi/đọc lại lịch sử nghiệp vụ. .env gitignored/untracked, scan candidate không phát hiện password .env hiện tại/private key/token thuộc các mẫu đã kiểm.

Bằng chứng: [verification.json](../artifacts/review-2026-10-27/verification.json) và [test-summary.txt](../artifacts/review-2026-10-27/test-summary.txt), có command/hash/giới hạn. Đây là local JPA/MockMvc/PG, không phải FE browser/HTTP Render/VNPay live và chưa chứng nhận tính năng mới. Kiểm kết nối DB Supabase mới sẽ ghi tiếp sau push theo yêu cầu Owner, không boot Spring/job hoặc chạy migration.

### 9.11. Push tai và thử Supabase sau push — 27/10/2026

Baseline đã push tới origin/tai `9097197` và đối chiếu remote SHA khớp local. Bao gồm commit F-06/F-07 `d210f50`, archive/source handoff `c7ad9f6`, quyết định/phân công/checklist/bằng chứng `9097197`. Đây là commit repo, không xác nhận Render đã deploy bản này. .env ignored/không track, không push password. Bản cập nhật kết quả kết nối sẽ là commit tài liệu tiếp theo, lấy SHA cuối từ Git khi bàn giao.

**Kết nối cấu hình .env chưa thành công:** JDBC 42.7.4 tới pooler `aws-0-ap-southeast-2.pooler.supabase.com:5432` trả SQLState XX000, `FATAL: (ENOTFOUND) tenant/user [REDACTED] not found`. Project ref trong username khớp ảnh, nhưng điều này chưa chứng minh host pooler đúng cho project. [Supabase hướng dẫn kết nối](https://supabase.com/docs/guides/database/connecting-to-postgres) nêu lỗi tenant/user not found khi pooler không ánh xạ được cặp host và username; chưa kết luận password sai và không tự đoán host/region khác.

Đã thử direct hostname trong ảnh với user postgres, dùng password .env ở bộ nhớ và giữ nguyên file. JDBC trả 08001/UnknownHostException, kể cả khi chỉ định preferIPv6; Windows DNS trả được AAAA. Chưa đủ chứng cứ kết luận cụ thể cấu hình IPv6/network nào gây lỗi, chưa chứng minh kết nối trực tiếp hoạt động. Không có SQL nào được thực thi, chưa kiểm schema/TLS/read-only transaction thành công, không boot Spring/job/migration/seed.

Cần Owner lấy đúng **Connect → Session pooler** của project mới, cập nhật DB_URL/DB_USERNAME trong .env rồi thử lại; không gửi password vào chat hoặc Git. Dùng nguyên host/index/region/port của panel, không ghép username project mới với pooler cũ theo suy đoán. Ảnh hiện có là direct connection nên chưa đủ xác nhận pooler hostname. Phạm vi lần thử tiếp vẫn chỉ SELECT/metadata/rollback.

Bằng chứng đã bỏ secret: [configured pooler](../artifacts/review-2026-10-27/supabase-connection.json), [direct](../artifacts/review-2026-10-27/supabase-direct-diagnostic.json), [direct với JVM IPv6 flags](../artifacts/review-2026-10-27/supabase-direct-ipv6-diagnostic.json) và [checker JDBC](../artifacts/review-2026-10-27/CheckDb.java). Checker đọc .env tại runtime, không nhận password ở command line và redacts credentials trong lỗi; không thuộc source ứng dụng/không chạy job. Verification manifest ghi trạng thái fail/pending chính xác, không sửa thành pass để hoàn tất bàn giao.

### 9.12. Lịch sử thử lại sau khi Owner cập nhật .env — xung đột nguồn mật khẩu

Owner báo đã cập nhật Session pooler. Host hiện là `aws-0-ap-northeast-2.pooler.supabase.com:5432`, Windows DNS mặc định/1.1.1.1 và Python đều phân giải được IPv4. JDBC ban đầu báo UnknownHostException; đọc nguyên nhân đã redacted xác định driver đang nhận cả userinfo làm hostname vì DB_URL có dạng URI chứa username/password. Không kết luận host mới lỗi DNS.

Preflight phát hiện username trong URL khớp DB_USERNAME, nhưng password trong URL khác DB_PASSWORD (chỉ so sánh trong bộ nhớ, không ghi giá trị). Đã dừng trước sửa cấu hình/chọn nguồn, hỏi Owner dùng DB_PASSWORD hay password trong DB_URL hoặc tự đồng bộ. Chưa thay .env, chưa kết nối/schema thành công, chưa SQL/migration/job.

Format Spring/JDBC cần dùng với các property riêng theo source và [pgJDBC](https://jdbc.postgresql.org/documentation/use/):

```properties
DB_URL=jdbc:postgresql://<session-pooler-host>:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<password-confirmed-by-owner>
```

Checker đã thêm preflight từ chối URL nhúng userinfo và báo conflict bằng boolean, tránh báo lỗi DNS gây nhầm hoặc in credential vào stack. Kết quả ở [config check](../artifacts/review-2026-10-27/supabase-connection-config-check.json) và [JDBC retry](../artifacts/review-2026-10-27/supabase-connection-retry.json). Các kết quả 9.11 là lịch sử cấu hình trước; không dùng làm kết luận tình trạng host mới.

### 9.13. Kết nối thành công theo nguồn Owner chọn — 27/10/2026

Owner xác nhận **dùng DB_PASSWORD hiện tại**. Đã sửa duy nhất DB_URL trong .env thành `jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require`, bỏ user/password nhúng trong URL; giữ nguyên DB_USERNAME và DB_PASSWORD. .env vẫn ignored/không track, không đưa credential vào Git hoặc command line. Các kết quả lỗi 9.11–9.12 là lịch sử trước chuẩn hóa, không phải blocker hiện tại.

Checker JDBC 42.7.4 đã chạy thực, exit 0: kết nối Session pooler thành công, SELECT 1 trả 1, database postgres/schema public, PostgreSQL 17.11. SHOW transaction_read_only trả on; chỉ đọc metadata và rollback, đóng kết nối hoàn tất trước in success. Không boot Spring/jobs, chạy migration/seed hoặc ghi dữ liệu nghiệp vụ.

JDBC dùng **client_sslmode=require**. Field **postgres_backend_tls=false** lấy từ pg_stat_ssl mô tả kết nối ở PostgreSQL phía sau pooler; không dùng field này để kết luận đường JDBC từ máy tới pooler không mã hóa. sslmode=require yêu cầu mã hóa nhưng không xác minh chứng thư như verify-full; phạm vi các field theo [pgJDBC](https://jdbc.postgresql.org/documentation/use/) và [PostgreSQL pg_stat_ssl](https://www.postgresql.org/docs/17/monitoring-stats.html#MONITORING-PG-STAT-SSL-VIEW).

Metadata thấy **0 bảng BASE TABLE trong schema public**, sessions.subscription_id chưa có. Chưa khởi tạo schema SCMS, kiểm API/FE, VNPay hoặc deploy trên DB mới. Việc chuẩn bị schema/fixture là bước triển khai riêng, không chạy ngầm từ yêu cầu kiểm kết nối.

Bằng chứng hiện tại: [kết quả JDBC chỉ đọc](../artifacts/review-2026-10-27/supabase-connection-normalized.json), [manifest](../artifacts/review-2026-10-27/verification.json) và [checker](../artifacts/review-2026-10-27/CheckDb.java). Chạy lại từ root workspace bằng Java 17 với PostgreSQL JDBC 42.7.4 trong Maven cache, source launcher trỏ tới checker; checker tự đọc .env, không cần truyền password. Kết quả clean test/PG local tại 9.10 giữ nguyên phạm vi baseline; cập nhật này chỉ thay checker/bằng chứng/tài liệu và cấu hình local ignored, chưa sửa source ứng dụng.

### 9.14. Tích hợp nhánh SMTP/OTP, chatbox và thông báo — 27/10/2026

Owner yêu cầu xử lý conflict trước khi tiếp tục hai phase trong session sau. Merge nền tai `fc00c64` với `3309b02` có 8 file conflict: contract API, RegistrationService, PasswordResetService, InvoiceService, MemberPackageService, SessionService, AttendanceFinalizationProcessor và BusinessGapAuditTest. Giữ Clock Asia/Ho_Chi_Minh, ScheduleWriteLock/row locks, kiểm quyền, lifecycle Completed và toggle booking-block; ghép thêm event publisher/hooks, OTP async AFTER_COMMIT của nhánh đồng đội. Không triển khai các policy D01–D19 còn ở backlog từ thao tác merge này.

Kiểm build phát hiện thêm import ZoneId bị mất và Clock trùng ở ba test do auto-merge; đã sửa phần ghép/fixture, không đổi assertions hoặc expected nghiệp vụ. Hai schema PostgreSQL root/database đã đồng bộ phần notification/chat ở mức file, API giữ mục 12–14 cũ và thêm mục 15–16 cho notification/chat. File docs/FEATURES-NOTIFICATION-OTP-CHAT.md của đồng đội được giữ làm mô tả source, không coi ví dụ là evidence live.

**Kiểm thực tế sau sửa:** `.\mvnw.cmd clean test` BUILD SUCCESS, **269 tests / 250 pass / 19 skip / 0 failure-error**. XML Surefire được cộng và đối chiếu summary log; 19 skip thuộc các nhóm opt-in live/demo/prepare và PostgreSQL chuyên biệt. Hai lượt trước thất bại compile (ZoneId) và testCompile (Clock trùng) đã được sửa; không xóa tests để xanh. [Manifest merge](../artifacts/merge-2026-10-27/verification.json) và [test summary](../artifacts/merge-2026-10-27/test-summary.txt) ghi parents, phạm vi, suite/count/hash/giới hạn.

Lượt này không đổi .env, không gửi email/OTP thật, không chạy migration/seed trên Supabase hoặc deploy. Các migration notification/chat chưa được kiểm thực thi PostgreSQL trong lượt merge; 14 tests PG ở 9.10 là baseline trước merge, không dùng thay cho nghiệm thu các migration mới. Còn SMTP delivery, DB migration/permission, API HTTP login thật/FE và cloud cần nghiệm thu riêng. D01/D09 chưa làm; checklist/hai phase và prompt tiếp nối tại docs 04 mục 40.12.

## 10. Bàn giao hai phase và demo tái lập — 27/10/2026

### 10.1. Phạm vi và bằng chứng

Đã triển khai P1.0–P1.4 và BE P2.1–P2.5 theo docs 04 mục 40, để lại P1.5/gói T D01/D09/FE theo xác nhận Owner. Code gồm catalog giá đóng băng, đăng ký/Coach/năng lực/Paid, 45 ngày sử dụng, lịch/hủy/notice/incident/Holiday, attendance/QR/bù Normal, đối soát/biên nhận/Cash sửa hẹp và report. Contract hiện hành docs 01 mục 6–10/14. Không sửa FE cũ để ghi hoàn tất tích hợp.

- Nền: tai `0277b2f`, clean test cô lập 269/250 pass/19 skip. Những count dưới là kết quả từng source, không quota.
- Bản P2 trước chốt: clean test 297 run/272 pass/25 skip/0 fail-error và 9/9 HTTP PostgreSQL pass. 6 skip phát sinh từ communication proof untracked của công việc khác, không đưa vào commit phase. 19 skip nền là các live audit/Cash/data prepare/PostgreSQL opt-in, không bật để ghi production.
- Source chốt `13a62ee`: clean test292 run/273 pass/19 skip/0 failure-error;PG HTTP9/9pass và migrations guards pass. 19skip=13 read-only live audit+1 live Cash+1 prepare demo+4 schema PG opt-in. Kiểm chốt exact commit/merge, summary và trace xem [demo handoff](../artifacts/demo/phase-2/2026-10-27/README.md) và verification.json tại cùng thư mục. Không công bố source sau sửa là đã test nếu chưa có run tương ứng.
- Test cũ được sửa có giải trình: quyền tạo Member/Receptionist→Coach, cutoff 24→12h, date→timestamp skip closure, record mới Late/Excused/scan sớm→Present/Absent trong giờ. Utility TOTP/history và booking-block toggle vẫn giữ tests. Không hạ kiểm concurrency/price/Paid để xanh.

### 10.2. Chạy lại demo

Cần Windows PowerShell, Java 17, Python 3, Docker Desktop đang chạy. Tạo PostgreSQL riêng lần đầu nếu chưa có:

```powershell
docker run --name scms-phases-20261027 -e POSTGRES_PASSWORD=scms-local-only -p 127.0.0.1:55440:5432 -d postgres:17
# Nếu container đã tồn tại và đang dừng:
docker start scms-phases-20261027
.\scripts\run-phases-demo.ps1
```

Runner tạo detached worktree từ HEAD vào TEMP (không có .env), chạy `.\mvnw.cmd clean test`, tạo DB mới `scms_phases_demo_<timestamp>`, chạy migrations hai lần/snapshot/FK rollback guards, rồi 9 HTTP workflow trên PostgreSQL. Không dùng DB_URL/DB_PASSWORD production. Không drop/reset DB có sẵn; nếu tên đụng thì dừng. Giữ worktree/DB và log/XML/HTTP trace để đọc. Output mặc định artifacts/demo/runs/<timestamp>; có thể truyền -OutputDirectory đường dẫn mới. Không chạy hai Maven clean cùng target.

Đây là đăng nhập thật HTTP loopback + transaction DB đã commit; Clock cố định tua 27/10→01/2027 để demo biên hạn mà không chờ 45 ngày. Tài khoản @example.test, tiền Cash/refund và VNPay test key đều fixture local. Không có tiền thật, IPN internet, SMTP delivery hay trình duyệt FE. SQL Server có migration counterpart và dialect sequence nhưng chưa chạy SQL Server runtime.

### 10.3. Manifest migration và rollout

DB nâng cấp đã có SCMS: backup + kiểm schema/orphan/legacy mapping trước; chạy đúng hệ quản trị theo thứ tự 06→07→08→09→10→11→12. 08 bổ sung entitlement/Coach;09 exact catalog guard;10 incident/lịch/audit;11 lịch sử Holiday;12 đối soát/receipt/dependency. Nullable mới giữ dữ liệu cũ, không suy subject/Coach/timestamp từ giá. Migration09 chỉ xóa bản dư đúng snapshot D02 và chưa bị tham chiếu; sai snapshot/FK thì rollback, không xóa cưỡng bức. Không dùng 05_reseed_demo_users_and_coaches.sql để rollout.

DB production Owner xác nhận trong .env: preflight driver trực tiếp/TLS/read-only thấy public **0 bảng**; không boot jobs để ping. Bootstrap mới `database/bootstrap/scms_structure_postgres.sql` chỉ schema/roles/permissions/default center/9 môn, **0 account, 0 Invoice, 0 subscription, 0 session/enrollment**. Bundle reviewable tại [release SQL](../artifacts/demo/phase-2/2026-10-27/release/scms-phases-empty-schema.sql), [plan/hashes](../artifacts/demo/phase-2/2026-10-27/release/release-plan.json). Không thay .env, không commit secret.

Bundle SHA256: `28635081e1724eb89ea82252fc1e468ca53805d347e4334d40ddf17a9c2866dc`. Local DB rỗng scms_phases_release_proof đã commit đúng 9 môn/0 account/0 business rows; chạy lại bị empty-schema guard từ chối P0001 và rollback. ApplyPhaseRelease.java kiểm hash/target fingerprint, Serializable transaction, lock timeout và counts trước commit. Bundle là artifact cố định; nếu tái sinh/đổi bytes phải review hash mới.

Render autoDeploy:true; chưa biết branch cấu hình trong dashboard nên **schema phải sẵn trước merge/push nhánh mà Render theo dõi**. Trình tự: kiểm source/DB đích → schema được Owner cho phép → xác minh counts/columns → push branch/merge main → theo dõi deploy SHA/log → smoke read-only/API theo tài khoản thật. Không coi git merge là đã deploy thành công. Owner đã xác nhận riêng “Cho phép khởi tạo production trước merge”; executor đã commit đúng bundle nói trên. Bằng chứng [production-release.json](../artifacts/demo/phase-2/2026-10-27/production-release.json) và [postflight](../artifacts/demo/phase-2/2026-10-27/production-postflight.json):46bảng,không thiếu cột checkpoint,9môn,0account/business rows. Không có tài khoản/giao dịch demo trên production. Quyền ghi này chỉ áp dụng bundle đã duyệt,không tự mở phạm vi seed/live tests.

Sau bootstrap cần Owner provision tài khoản quản trị thật (email/password riêng, BCrypt, đúng center/role) qua quy trình vận hành được kiểm soát; không dùng tài khoản/mật khẩu demo lịch sử. Tạo HLV/phòng/chuyên môn rồi publish work-schedule trước mở bán Advanced. Nếu đăng ký qua OTP, cần SMTP thật và xác minh email trước khi gán quyền quản trị qua DB bởi Owner. Lượt này không tự tạo tài khoản Owner hoặc phát sinh giao dịch production.

### 10.4. Vận hành và kiểm sau deploy

Giữ ddl-auto=none; cấu hình Clock Asia/Ho_Chi_Minh, holding5 phút, booking-block default false, lobby-checkin default false. VNPay URLs đúng public HTTPS, Return đọc/IPN ghi; cookie/CORS FE domain chính xác. Không chạy opt-in live data/Cash tests trên production. Kiểm log schema mismatch, health/API danh mục trả9 giá đúng, quyền reports Coach403/Manager có quyền, Invoice ownership và luồng mua thật được phép riêng. Nếu code lỗi, quay về code đã tương thích schema bằng deploy trước; không drop cột/bảng hoặc xóa hồ sơ để rollback. Trường hợp legacy thiếu căn cứ cần đối soát, không backfill từ amount.
