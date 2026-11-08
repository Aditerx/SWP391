# SCMS — Vận hành và kiểm thử

## 1. Nguồn và cấu hình

[docs00](00-MASTER-CHECKLIST.md) giữ trạng thái; [docs04](04-DEBUG-NOTE.md) giữ quyết định; [docs01](01-API-CONTRACT-FE-BE.md) giữ contract FE. File này chỉ giữ cách vận hành, không chép nhật ký từng lần chạy.

Java 17/Spring Boot 3.3.5, Maven wrapper, PostgreSQL/Supabase; Dockerfile ở root là cách build hiện tại. SQLServer có scripts nhưng runtime chưa nghiệm thu. Secret/.env/backup riêng tư ngoài Git, runner nghiệp vụ không nạp .env cloud. Giữ ddl-auto=none và OSIV=false.

| Nhóm biến | Ý nghĩa / default source |
|---|---|
| PORT | 8080 nếu môi trường không cấp |
| DB_URL,DB_USERNAME,DB_PASSWORD | JDBC PostgreSQL/TLS phù hợp, credential tách riêng; Supabase pooler username chứa project reference |
| SCMS_PAYMENT_EXPIRE_MINUTES | 5; fallback PAYMENT_EXPIRE_MINUTES |
| SCMS_BOOKING_BLOCK_ENABLED,SCMS_ATTENDANCE_LOOKBACK_DAYS | false và3 ở source |
| FRONTEND_URL hoặc scms.frontend-origins | Exact FE origins; source có thêm localhost/127.0.0.1 pattern |
| SESSION_COOKIE_SAME_SITE,SESSION_COOKIE_SECURE | lax/false; cross-site HTTPS cần none/true và browser kiểm thực tế |
| VNPAY_TMN_CODE,VNPAY_HASH_SECRET | Merchant/key trong secret store |
| VNPAY_PAY_URL,VNPAY_RETURN_URL,FRONTEND_PAYMENT_RESULT_URL | Gateway/HTTPS BE return/trang kết quả FE đúng môi trường |
| MAIL_HOST,MAIL_PORT,MAIL_USERNAME,MAIL_APP_PASSWORD | smtp.gmail.com/587 mặc định; credential ngoài Git |
| MAIL_FROM_EMAIL | Địa chỉ người gửi đã xác minh; nếu không đặt dùng spring.mail.username. Tách khỏi SMTP login khi dùng Brevo |
| REGISTRATION_DEMO_OTP | Để trống production; mã cố định chỉ cho demo đăng ký được chọn rõ |
| CLOUDINARY_CLOUD_NAME,CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET | Nếu dùng avatar, secret ngoài Git |
| SCMS_AI_CHAT_ENABLED | false; phải bật rõ ràng mới có responder AI |
| SCMS_AI_CHAT_PROVIDER | mock mặc định để dev FE không cần key; Anthropic chỉ khi chọn rõ |
| SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER | STAFF mặc định; chọn AI riêng cho local mock khi muốn kiểm UI trợ lý |
| ANTHROPIC_API_KEY,SCMS_AI_CHAT_MODEL | Chỉ cần cho provider Anthropic; key lưu secret store, model là model ID đang khả dụng trong Anthropic Console |
| SCMS_AI_CHAT_TIMEOUT_SECONDS,SCMS_AI_CHAT_MAX_TOKENS,SCMS_AI_CHAT_MAX_INPUT_CHARACTERS | timeout12s, đầu ra tối đa400 token, lịch sử tối đa8000 ký tự gửi provider |

Đây là default/source, không chứng nhận dashboard đã đặt đúng; external Spring configuration có thể override. Cùng host/password chưa chứng minh cùng Supabase project. HikariCP là pool JDBC của ứng dụng, khác Supabase pooler; source default5connection/timeout30000ms. Fingerprint chỉ đối chiếu target hiệu lực, không là chứng nhận toàn API.

### Chat AI local (không cần provider key)

`ChatAiResponder` chỉ được đăng ký khi `SCMS_AI_CHAT_ENABLED=true`. Để thử luồng FE với câu trả lời cố định, dùng DB PostgreSQL local đã dựng theo bước Local workflow, rồi trong PowerShell khởi chạy backend với:

```powershell
$env:SCMS_AI_CHAT_ENABLED = 'true'
$env:SCMS_AI_CHAT_PROVIDER = 'mock'
$env:SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER = 'AI'
.\mvnw.cmd spring-boot:run
```

Khi tắt feature flag, hội thoại mới luôn là STAFF như trước. Nếu bật mock nhưng để handler STAFF thì chat vẫn chuyển đến nhân viên; lựa chọn handler mặc định hiện được giữ STAFF cho đến khi Owner chốt. Mock không gọi mạng. Provider Anthropic dùng `POST /v1/messages`, `x-api-key`, `anthropic-version`, `model`, `max_tokens`, `system` và `messages` theo [Messages API chính thức](https://platform.claude.com/docs/en/api/go/messages/create); chọn model ID có hiệu lực trong Console và chỉ đặt key qua `.env` ignored hoặc secret store. Không cần dependency SDK riêng.

Mỗi request chỉ nhận tối đa20 tin mới nhất, giữ lịch sử trong giới hạn ký tự và loại tin SYSTEM. Tin có dấu hiệu password/OTP/token hoặc số thẻ được bỏ toàn bộ trước khi gửi. Không gửi member ID hoặc dữ liệu hội thoại khác. Timeout tối đa cấu hình12giây, thử lại một lần với timeout/lỗi HTTP5xx; lỗi cuối chuyển hội thoại sang STAFF bằng SYSTEM message. Log chỉ ghi loại exception và conversation ID, không ghi nội dung prompt, response hoặc key. Bộ lọc theo mẫu là lớp giảm thiểu, không thay thế rà soát dữ liệu; chatbot vẫn chưa được phép xử lý dữ liệu tài khoản/thanh toán.

<a id="testing"></a>
## 2. Kiểm thử

Chọn Dxx/điều kiện nghiệm thu trước, chuẩn bị actor/fixture/Clock/trạng thái trước-sau. Test đích khi sửa; hoàn tất nhóm BE chạy `.\mvnw.cmd clean test` và kiểm liên quan. Count lấy từ XML, skip tách riêng; chỉ lặp kiểm rộng khi có diff/lỗi/nghi vấn mới. H2 không thay PostgreSQL/locking thật.

Nhóm có phiên phải login và kiểm `/api/auth/me`, sau đó đúng người/sai người/sai role/khác center.401 do mất phiên là BLOCKED của setup, không phải pass quyền. Tiền/quyền/credit/lịch cần response và state sau commit, gồm biên5phút/12h/[start,end), retry/race. Anonymous smoke báo gộp, không liệt kê71dòng401 vào nghiệm thu nghiệp vụ.

### Local workflow

```powershell
docker start scms-phases-20261027
.\scripts\run-phases-demo.ps1
```

Cần Docker Desktop/PostgreSQL 17 container trên, bind **127.0.0.1:55440**, credential local-only `scms-local-only`. Runner tạo committed checkout không.env và DB mới; clean test →migration/replay/snapshot/FK guard →HTTP login/workflow/DB assertions. Clock tiến có kiểm soát; callback ký test key không là gateway live. Không chạy hai Maven clean cùng target.

HTTP test dùng `--minimal-fixture`: schema/catalog local và actor mới trong test, không dùng dữ liệu demo hoặc tài khoản seed lịch sử. Mỗi login xác minh `/api/auth/me`; trước mỗi nhóm xác minh lại5actor chính. Các request race dùng2worker sẵn sàng rồi mở chung latch; kiểm cả response và DB qua connection khác sau HTTP commit. Test phụ thuộc fixture trước sẽ skip nếu nhóm trước thất bại; báo BLOCKED, không coi đó là chặn đúng vai trò.

Runner minimal lấy bootstrap cấu trúc, bỏ grants mặc định để test tự định nghĩa role matrix. Chỉ trong DB local mới này, bỏ hai kiểm ngày quá khứ dùng wall-clock của trigger để không xung đột MutableClock; giữ guard role/phòng/sĩ số/FK và mọi kiểm thời gian của service. Không sửa các guard trong hai file SQL bàn giao Supabase. Kiểm account demo riêng bằng `DemoSchemaLoginPostgresTest`, đặt `-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh` và `-Dscms.demo.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_schema_<fixture>`; không dùng URL cloud.

D01/D09: unit/MockMvc gốc chạy H2, còn `PhaseWorkflowHttpTest` kiểm HTTP login thật trên PG. Fixture ngày sinh dùng Clock cố định. Race PATCH giữ row User bằng connection riêng, chờ cả2request ở DB lock rồi thả; hai field khác nhau phải cùng còn sau commit. Directory kiểm Admin center null, scope/contact và query count không tăng theo số Coach. Snapshot account Inactive được đọc trước khi phục hồi fixture. Case Member thiếu center chỉ chạy H2 vì guard PG từ chối lưu trạng thái đó; lỗi setup không tính là pass403/400. Tích hợp này dùng schema hiện có, không thêm migration hay mở quyền cloud.

RBAC theo RP-04/RP-36 và giáo án lớp: `RbacGuardIntegrationTest` mặc định chạy H2; opt-in `-Dscms.rbac.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` chỉ chấp nhận DB local đúng regex này, dùng ddl-auto=none. Chuẩn bị DB mới bằng bootstrap/runner minimal và các prerequisite hiện hành theo phần trên; không dùng DB workflow đã có actor để chạy chồng fixture. Fixture dùng môn chuẩn, subtype Coach/Member/Receptionist, ngày tham gia và Paid invoice có nguồn hợp lệ; không nới FK/NOT NULL để ép test qua. Kiểm target row lock bằng blocker metadata của đúng H2 hoặc PostgreSQL. Chạy trên bản sao không.env; kết quả/source/XML chuẩn ở docs00/QA, không suy PG local thành Supabase hoặc tự chạy migration cloud.

Ngoài exchanges, runner giữ `acceptance-cases.json` (Dxx/fixture/expected/SQL state và index response), `actors.json` và XML trong raw run. Sau khi kiểm kết quả, ghi vào QA chuẩn bằng lệnh dưới; lấy checkout từ `verification.json`. Chỉ chạy với test đã commit hoặc bản test được copy có hash khớp; không gọi kết quả checkout cũ là kết quả working tree mới.

```powershell
python scripts/record-authenticated-acceptance.py --run artifacts/runs/phases/<run> --checkout <checkout> --http-dir postgres-http
```

Script offline kiểm XML, hash test/source và compatibility schema trước khi thay `artifacts/qa/latest.json`; không login/probe live, không đọc.env hoặc tạo thêm bộ tài liệu bàn giao. Runner truyền `scms.test-source-sha256` vào lượt Maven PostgreSQL và XML lưu lại; collector yêu cầu khớp cả bản test hiện tại lẫn checkout, không ghép XML cũ với source test mới. Khi chạy Maven thủ công phải truyền cùng property từ SHA256 file test ngay trước lượt chạy. Case chỉ lưu response một lần trong `local.exchanges`, matrix tham chiếu index; SHA backend và hash bản test chưa commit được ghi riêng. SQL Timestamp trong snapshot có thể serialize UTC/Z, đối chiếu sang Asia/Ho_Chi_Minh với businessNow/expected. Những gate cloud giữ thời điểm/SHA lịch sử và không được nâng thành nghiệm thu mới.

Output `artifacts/runs/phases/<timestamp>` ignored; raw run cuối trước chuẩn hóa giữ local ở `artifacts/runs/phases/2026-10-27-verified`. Không mặc định có raw run trên máy khác. Communication riêng: `python scripts/test-communication-features.py`, output `artifacts/runs/communication/<run>`; DB/SMTP loopback, không email ra ngoài hoặc .env cloud.

### Kiểm gói QA đã giữ

```powershell
python scripts/audit-fe-contract.py
```

Mặc định offline, đọc một OpenAPI/QA để kiểm route/source và compatibility cơ bản của exchanges đã ghi. Không probe/login Render hoặc sinh thêm nhiều tài liệu. Có --openapi/--qa để chọn bản khác. Nullability/required/format/range/error cần kiểm riêng; không coi kiểm type/property là full JSON Schema.

Báo cáo đủ: `Dxx/case →actor/fixture →expected →response+state →PASS/FAIL/BLOCKED/NOT_RUN →môi trường/SHA →một tham chiếu`. Không lấy số request/assertion làm mức hoàn tất; chỉ mở đúng scenario/index trong QA khi cần.

## 3. Deploy

<a id="rbac-fe-verification"></a>
### Kiểm phản hồi FE sau bản sửa RBAC

Đối chiếu source/merge/CI tại docs00/QA trước, rồi GET `/api/health` trên BE mà FE thật sự gọi: `buildCommit` phải thuộc bản đã phát hành. HTTP200 health chỉ xác nhận ứng dụng trả lời, không nghiệm thu nghiệp vụ hoặc migration Google. Không chạy lại SQL16/reset DB cho lượt này. Dùng tài khoản và record kiểm thử được phép; không thay role/trạng thái/mật khẩu tài khoản vận hành để thử.

1. **Coach/roster theo ảnh đã gửi:** đăng xuất/đăng nhập lại, kiểm GET `/api/auth/me`200 có role Coach và đúng user. Trong màn hình **Điểm danh**, chọn đúng lớp được giao (lớp23 chỉ dùng nếu đã xác minh phân công). Quan sát Network: GET `/api/classes/{id}/enrollments` trả200 danh sách đúng lớp là đúng; tuyến dự phòng `/api/enrollments?classId={id}` có thể không cần chạy khi tuyến đầu thành công. Lớp hoàn tất vẫn đọc roster, giữ banner khóa sửa attendance.403 ở lớp không được giao là đúng;401 hoặc role sai phải giải quyết phiên trước, không tính là kiểm quyền thành công.
2. **Giáo án/đánh giá:** qua màn hình hiện hành có gọi `training-plans`/`evaluations`, Coach được giao tạo/sửa/xóa record kiểm thử phải nhận201/200/204 và tải lại giữ đúng nội dung/target. Manager cố ghi cùng API phải403 dù có authority quản lý lớp; FE có thể ẩn thao tác theo quyền. Không đổi attendance correction D15 thành bài kiểm ghi giáo án.
3. **Member:** GET `/api/training-plans`200 chỉ gồm plan riêng của mình và plan chung lớp Normal Registered+Paid hợp lệ, đúng center; ID plan lớp/người khác403. Danh sách trống200[] chỉ đúng nếu không có plan đủ điều kiện. GET `payment-resolutions`403 vẫn là đúng quyền Member; FE cần ngừng gọi theo bàn giao RBAC lần1.
4. **Quản trị tài khoản:** bằng record kiểm thử riêng, Admin đủ authority được quản lý Manager; Manager quản lý target Manager/Admin hoặc chuyển role thường thành Manager phải403 và tải lại không có thay đổi. Kiểm đúng các tuyến users/staff/status/role/reissue cần dùng; không gọi cấp lại mật khẩu tài khoản thật để thử.
5. **Phiên đang mở (tùy fixture):** thay permission của role kiểm thử bằng Admin rồi request kế tiếp phải áp quyền mới; đổi role trả403 `SESSION_ROLE_CHANGED` và yêu cầu đăng nhập lại; Inactive trả403 `ACCOUNT_NOT_ACTIVE`; cấp lại/đổi password quản trị thu hồi phiên thành401. Chỉ dùng role/account kiểm thử riêng để không thay quyền của cả nhóm người dùng.

Chỉ hướng dẫn click/menu đã có ảnh hoặc giao diện hiện hành; nếu màn hình giáo án/quản trị khác hoặc chưa có nút tương ứng, lấy ảnh đúng vùng đó trước khi chỉ tiếp, không suy từ FE_Project-main cũ. Gửi kết quả theo role → thao tác → method/path → status/body liên quan và thông báo UI; che email/cookie/token. Không gửi OTP/mật khẩu. Nếu request lỗi, giữ response và xác minh auth/me cùng phiên trước khi đổi quyền. Xong nhóm kiểm FE thì Owner xác nhận kết quả; sau đó mới sắp xếp, giải thích và đề xuất phần tiếp theo, không tự mở policy mới.

<a id="google-login"></a>
### Google login/avatar nhập từ main

Entity User có google_sub; DB ứng dụng đã tồn tại cần [13_google_login_postgres.sql](../database/migrations/13_google_login_postgres.sql) trước deploy bản BE này, kể cả khi chưa bật Google login vì truy vấn User đã đọc cột đó. File chỉ thêm cột/index nếu chưa có, không reset/seed; đây là migration Google riêng, khác các file13 về room. Hai schema gộp đã chứa Google; bootstrap local dùng migration riêng khi chạy runner. Kết quả SQL16 trước đó không chứng minh migration Google đã chạy trên Supabase.

GOOGLE_CLIENT_ID là audience của GIS ID token; GOOGLE_AUTO_REGISTER mặc định true theo source, false thì email chưa đăng ký bị từ chối. Login thường/Google cùng session JSESSIONID; cấu hình CORS/cookie theo mục1. Bản ghép giữ tính năng hiện có từ main; kiểm token giả lập local không thay nghiệm thu Google thật hoặc Render. Trạng thái thực thi DB/deploy xem docs00/QA; agent không tự chạy migration cloud.

<a id="attendance-review-20261109"></a>
### Attendance cuối ngày/note — Owner09/11

BE attendance mới cần [17_attendance_review_postgres.sql](../database/migrations/17_attendance_review_postgres.sql) trước deploy. Baseline cần sessions/attendance14 và attendances.note TEXT; dependency Google13 hiện có vẫn cần kiểm riêng. SQL17 chỉ thêm marker/index, không reset/seed hoặc chạy lại16. Hai schema gộp chưa nhúng17: dùng riêng migration cho DB cũ hoặc sau dựng baseline mới. Không chạy schema dựng lại trên dữ liệu cần giữ.

Migration đặt marker mới false cho dữ liệu hiện có, không queue buổi đã finalized trước nâng cấp; replay giữ pending của finalizer mới. Khi phát hành cần thay bản BE/job cũ bằng cùng bản mới, tránh để finalizer cũ tiếp tục gửi no-show tại end. Finalizer mới vẫn hoàn tất lifecycle tại end; review đọc Absent hiện hành sau00:00 hôm sau, bỏ buổi hủy/invalidated, tắt pending cùng transaction và giữ dedupe notification. Job trễ không đổi cutoff Coach; booking-block vẫn default false.

Kiểm local không.env/cloud:

```powershell
python scripts/test-attendance-review-postgres.py --database scms_phases_attendance_migration_<ma_moi>
python scripts/test-phases-postgres.py --database scms_phases_attendance_<ma_moi> --minimal-fixture
.\mvnw.cmd '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh' '-Dtest=AttendanceCorrectionIntegrationTest' '-Dscms.attendance.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_attendance_<ma_moi>' test
```

Chạy Maven từ bản sao source hiện hành không.env; DB mới loopback55440 đúng runner. Integration test mặc định H2, opt-in PG chỉ nhận URL local đã ghim, ddl-auto=none; fixture login/auth-me thật rồi kiểm response/DB sau commit và race. Có thể xuất schema từ cùng app bằng `-Dscms.attendance.openapi=<duong_dan_raw_openapi.json>`; chỉ cập nhật snapshot sau khi kiểm đạt. Test đích rồi clean test theo mục2. Bằng chứng/SHA/count hiện hành chỉ ghi ở docs00/QA; FE/Render/CRUD nghiệm thu đợt chung, lượt này không mutation cloud.

<a id="retire-legacy-memberships"></a>
### Bỏ dữ liệu gói hội viên chung — migration16

Owner đã duyệt B→dọn mô hình gói chung→A; **dừng chờ Owner chạy Supabase thành công trước khi làm A**. File duy nhất: [16_retire_legacy_memberships_postgres.sql](../database/migrations/16_retire_legacy_memberships_postgres.sql). Cần DB SCMS hiện hành đã có schema Advanced/QueryDR15; không chạy lại schema dựng lại hoặc seed. BE local mới đóng API gói chung và tuyến enroll Advanced cũ; trạng thái publish/runtime xem docs00, SQL không tự deploy BE.

File tự làm preflight→khóa/phân loại→xóa subgraph gói chung cũ chắc chắn→postflight→commit. Nó có thể xóa hóa đơn cũ và payment attempt/resolution trực tiếp thuộc đúng subgraph đã bỏ, **không chỉ xóa danh mục**. Quyền/chứng từ hiện hành được giữ, chỉ tháo FK catalog cũ khi có đủ môn/Coach/owner; current amount/purchaseAmount/credit/state và Normal/session/attendance không được thay. Gặp link chưa rõ/ngoài phạm vi hoặc dữ liệu được bảo vệ đổi ngoài dự kiến thì toàn bộ transaction rollback. Không DROP TABLE/TRUNCATE/CASCADE; không lưu archive dòng nghiệp vụ cũ; marker chỉ có số lượng. Trigger sau migration chặn ghi trở lại mô hình cũ.

1. Mở đúng project Supabase đang chạy SCMS, vào **SQL Editor → New Query**; dán toàn bộ file, không chọn chạy riêng từng đoạn. Bấm **Run** theo [hướng dẫn Supabase](https://supabase.com/docs/guides/database/functions).
2. **Kết quả trả về một dòng `result=PASS`, `migration_key=20261106_retire_legacy_memberships_v1`, `summary.model_retired=true` là đúng**: preflight/postflight đã qua và transaction đã commit. Số lượng xóa có thể0 nếu DB không còn dữ liệu cũ; không so với số fixture local. Replay không xóa thêm; summary vẫn là số lượng của lần áp dụng đầu.
3. Gửi dòng cuối hoặc nội dung lỗi để kiểm tiếp. Nếu có `SCMS_LEGACY_RETIRE_PREFLIGHT/BLOCKED/POSTFLIGHT/REPLAY_CONFLICT`, không bỏ guard hoặc sửa dữ liệu cho qua; gửi lỗi để xác định đúng phần còn vướng. Chỉ tiếp tục A sau kết quả migration thành công; SQL pass không thay FE/Render acceptance. Trạng thái lần chạy Owner đã cung cấp xem docs00/QA.

Kiểm local: `python scripts/test-retire-legacy-memberships.py` chỉ dùng Docker PostgreSQL loopback55440/DB mới, không .env/cloud; có kiểm empty/old+modern/replay/retirement guards/unknown FK/partial Advanced/rollback sau mutation. BE có test đích và clean; workflow có phiên cũng kiểm trên PG sau migration. Khi chạy Maven PG dùng JVM `-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh`, không dùng alias Asia/Saigon cho JDBC của container này. Kết quả/hash/XML tại docs00/QA, không suy local thành Supabase đã chạy.

Chọn script DB theo [database/README](../database/README.md#2-chọn-đúng-script): schema root bảo toàn ID/lịch sử, còn `database/schema_postgres.sql` dựng lại dữ liệu SCMS mỗi lần chạy. Khi chỉ cần sửa thiết kế trên DB cần giữ dữ liệu, chuẩn bị migration riêng thay vì chạy bản dựng lại. Schema gộp đã bao gồm migration01–14; không chạy nối tiếp hai schema hoặc chạy lại toàn thư mục migration sau đó.

**Bundle room/báo vắng02/11:** baseline29/10 dùng `13_room_catalog_postgres.sql`→`14_absence_attendance_postgres.sql` trước BE mới. Dừng ghi lịch trong cửa sổ migration, backup/restore proof và kiểm DB đích; không dùng bản dựng lại. Preflight đọc rooms, classes.max_capacity, số ghi danh, sessions Scheduled từ hôm nay và môn liên quan. Đối soát lớp vượt sức chứa mới theo danh mục README, ca đang diễn ra, phòng tùy chỉnh, code/tên trùng và sân01 trùng lịch trước khi chạy. Guard lỗi thì transaction rollback; không sửa lớp ngoài7giới hạn được duyệt hoặc tắt trigger để vượt. Bản trước/sau chuyển room và marker lưu trong scms_data_migration_audit; lịch đã diễn ra/chứng từ/attendance không chuyển/xóa. Postflight:20phòng mới Available,8phòng chuẩn cũ Closed nếu nâng cấp từ fixture8phòng; lịch tương lai dùng room Available/đủ sức chứa/không trùng; kiểm marker và unique current attendance. Replay giữ metadata phòng đã chỉnh về sau. BE mới cần đủ hai migration; code cũ chưa hiểu attendance superseded nên không rollback riêng BE sau khi đã phát sinh luồng mới.

**Hai file tạm ở root theo yêu cầu Owner:** chạy toàn bộ `0211-1.sql` (preflight+7giới hạn đã duyệt+migration room13) trong một lần SQL Editor; chỉ khi thành công mới chạy toàn bộ `0212-2.sql` (migration14+postflight) lần thứ hai. File2 kiểm marker của file1 và dừng nếu sai thứ tự. Không cần chạy riêng truy vấn preflight hoặc chạy thêm hai migration nguồn khi dùng cặp này. Nếu guard lỗi, dừng để đối soát; không tự giảm sức chứa/sĩ số hoặc bỏ guard. Sau hai lần thành công và kiểm kết quả, có thể xóa hai bản tạm; migration13/14 trong database/migrations vẫn là nguồn bảo trì. Không dùng cặp file này để dựng lại DB hoặc thay schema gộp ở môi trường chưa có baseline29/10.

Owner đã duyệt điều chỉnh7lớp bên dưới: bản0211-1 hiện gộp `13_owner_class_capacity_20261102_postgres.sql` **trước**13_room_catalog, cùng BEGIN/COMMIT. Bước này chỉ cập nhật max_capacity của7ID đúng môn/center1/Normal, chấp nhận giá trị cũ đã duyệt hoặc đã bằng mức mới; trạng thái khác làm dừng. Registered+Pending được đếm lại dưới lock, không được vượt mức mới. Audit `20261102_owner_normal_class_capacity_v1` giữ đầy đủ hàng lớp và held_seats trước/sau; lỗi chuyển phòng phía sau rollback cả cập nhật lớp/audit. Replay bỏ qua bước đã có marker để không reset thay đổi về sau. File2 mới kiểm cả marker giới hạn lớp và marker phòng. Không nhúng bước theo ID riêng này vào schema gộp/empty release cho mọi DB.

**Sửa lỗi CUSTOM_ROOM02/11:** inventory Owner gửi có9phòng, thêm room9 Sân Tennis02 so với fixture8phòng ban đầu. Bản sửa nhận cả tên trong20phòng đã duyệt và alias cũ; vẫn chặn tên lạ, trả DETAIL room_id/tên/trạng thái. Lịch Tennis02 chuyển đúng C01-TEN-02 để không bị dồn sang Tennis01. Chạy lại toàn bộ bản mới `0211-1.sql`; không chạy file2 nếu file1 lỗi. Với bộ9phòng này, kết quả lần nâng cấp đầu là20phòng mới và9phòng legacy Closed, không buộc tổng bảng chỉ20dòng. ROOM_CATALOG_SCHEDULE_REVIEW vẫn có thể dừng hợp lệ nếu lớp/lịch vượt sức chứa mới; module room không tự giảm max_capacity; bước Owner-specific chỉ đổi đúng7giới hạn đã duyệt và không xóa ghi danh. Lỗi ở trong transaction của cả file nên lần bị chặn không commit cập nhật phòng.

**Chẩn đoán SCHEDULE_REVIEW:** DETAIL của0211-1 mới liệt kê tối đa20buổi bị chặn với session_id/class_id, center_id, target_code, session_start, max_capacity, target_capacity, held_seats (Registered+Pending) và reasons. `CLASS_CAPACITY_EXCEEDS_ROOM` là cấu hình số chỗ tối đa vượt sân, kể cả số người thực tế còn ít; `ENROLLMENTS_EXCEED_ROOM` là số chỗ đã giữ vượt sân; ngoài ra có STARTED_OR_PAST_SCHEDULED, UNKNOWN_SUBJECT_OR_TARGET, WRONG_CENTER và MISSING_CLASS_CAPACITY. Guard không đổi điều kiện hoặc tự xử lý dữ liệu. Đề xuất để Owner chốt sau khi xem DETAIL: nếu chỉ max_capacity vượt nhưng held_seats còn vừa sân, điều chỉnh max_capacity của đúng lớp có audit; nếu số đăng ký vượt sân thì cần phương án lớp/phòng/lịch phù hợp, không tự xóa/hủy ghi danh. Ca đã bắt đầu/đã qua còn Scheduled và liên kết môn/center thiếu/sai phải đối soát theo lịch sử thực tế, không đổi giờ/trạng thái để né guard. Chỉ chạy file2 sau khi file1 thành công.

**Giới hạn7lớp Owner đã duyệt02/11:** các dòng DETAIL được gửi đều CLASS_CAPACITY_EXCEEDS_ROOM, mỗi lớp giữ1chỗ. Đồng bộ max_capacity theo bảng dưới trong transaction trước khi chuyển phòng, có kiểm/audit như trên. Không tăng sức chứa phòng đã chốt; không đổi người học, giá, chứng từ hoặc attendance. Giờ buổi học giữ nguyên; phòng của lịch tương lai chuyển theo migration13. Đây là đúng7lớp được duyệt, không cho phép tự thu nhỏ lớp khác; guard vẫn kiểm mọi lịch trước commit. Agent chỉ chuẩn bị/kiểm local, chưa chạy Supabase.

| class_id | Lớp Normal K01 | max_capacity cũ → đã duyệt |
|---:|---|---:|
|11|Bơi lội|15 → 10|
|12|Pickleball|12 → 4|
|13|Bắn cung|10 → 5|
|14|Bóng rổ|15 → 8|
|16|Bóng chuyền|15 → 8|
|17|Cầu lông|15 → 4|
|18|Bóng bàn|15 → 2|

Kiểm đích bundle có phê duyệt: `python scripts/test-owner-class-capacity-postgres.py --prefix scms_phases_owner_caps_<mã_mới> --output artifacts/runs/<run>/owner-capacity.json`. Runner chỉ dùng PostgreSQL local được ghim, kiểm sai danh tính/giới hạn/held seats, rollback qua lỗi chuyển phòng, đúng7giới hạn, giữ chứng từ/attendance/lịch đã diễn ra, audit và replay. Kết quả chuẩn ở docs00/QA; không tự gán nghiệm thu cloud.

Kiểm local riêng: `python scripts/test-room-absence-postgres.py --prefix scms_phases_room_<mã_mới> --output artifacts/runs/<run>/migration.json`; runner từ chối DB ngoài container/port local được ghim, tạo DB mới và kiểm fresh/rebuild/upgrade/replay/guard/history. Fixture nâng cấp lấy schema trước thay đổi tại commit baseline đã ghim trong runner, không đọc .env. Sau đó kiểm HTTP PostgreSQL bằng PhaseWorkflowHttpTest với fixture mới và SHA256 test như hướng dẫn ở mục2. Kết quả chuẩn nằm ở docs00/QA, không suy từ kiểm H2.

Truy vấn preflight chỉ đọc dưới đây liệt kê lớp/phòng/sĩ số cần so với [sức chứa mới](../database/README.md#room-catalog); kết quả không tự cho phép sửa lớp hoặc chuyển lịch:

```sql
SELECT c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name,
       count(*) AS scheduled_sessions,min(s.session_date+s.start_time) AS next_start,
       (SELECT count(*) FROM class_enrollments e WHERE e.class_id=c.class_id
        AND e.status IN ('Registered','Pending')) AS held_seats
FROM sessions s JOIN classes c USING(class_id) JOIN subjects su USING(subject_id)
JOIN rooms r USING(room_id)
WHERE r.center_id=1 AND s.status='Scheduled' AND NOT s.center_invalidated
  AND s.session_date>=(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
GROUP BY c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name
ORDER BY next_start,c.class_id;
```

Ghi bằng chứng mới bằng `scripts/record-room-absence-review.py --run artifacts/runs/<run> --checkout <bản_source_đã_kiểm>` sau khi lưu clean XML vào `clean-xml`, PG XML vào `postgres-workflow.xml`, HTTP/SQL vào `postgres-http`, migration vào `migration-final.json`. Collector kiểm fingerprint source/schema/fixture và hash test đã chạy trước khi cập nhật QA duy nhất/OpenAPI; không nâng trạng thái cloud hay ghi đè bằng chứng lịch sử.

Kiểm source/SHA/CI/schema tương thích và quyền migration trước push nhánh Render theo dõi. Owner đã xác nhận main; kiểm lại cấu hình khi bắt đầu vận hành mới. Khi cần schema: backup/restore proof →duyệt bundle cụ thể →atomic migration/guard →postflight →deploy code tương thích. Không reset/restore đè dữ liệu để chữa code lỗi.

Theo dõi Live identity, không coi Git merge/dashboard Live là nghiệm thu nghiệp vụ. Nếu yêu cầu kiểm sau5phút, runner từ chối chạy sớm:

```powershell
python scripts/check-render-production.py --output artifacts/runs/render/check.json --source-commit <LIVE_SHA> --require-runtime-identity --expected-db-fingerprint <TARGET_HASH> --pushed-at-utc <ISO_UTC>
```

`/api/health` chỉ liveness/identity, không mở DB connection; `/actuator/health` kiểm db/mail/... và giữ show-details=never. Ghi nhận27/10: Owner dùng `/api/health` trong dashboard, render.yaml khai `/actuator/health`; không giả hai cấu hình giống nhau hoặc đổi path để che dependency hỏng.

CORS/cookie cần browser FE hiện hành kiểm preflight/Set-Cookie/cookie gửi lại. Gateway live cần Merchant/callback/fixture được phép; FE không gọi IPN. Commit chỉ docs/evidence có thể dùng `[skip render]` tránh deploy không cần thiết, theo [Render deploy docs](https://render.com/docs/deploys#skipping-an-auto-deploy).

<a id="vnpay-diagnostics"></a>
### Chẩn đoán VNPay báo sai chữ ký — D10

Không đổi thuật toán ký chỉ dựa vào ảnh lỗi. Đối chiếu source đúng `buildCommit` từ `/api/health`, cấu hình hiệu lực và URL thực tế trước. Không nhập HashSecret vào website kiểm hash, tham số dòng lệnh, chat hoặc báo cáo.

**Nếu không có bộ thông tin VNPay cấp:** ảnh Environment chỉ chứng minh đã nhập giá trị, không chứng minh merchant/key thuộc cùng bộ hoặc còn hiệu lực. Tìm email của người đã đăng ký merchant trong nhóm với từ khóa `VNPAY`, `TmnCode`, `HashSecret`, `thông tin kết nối`, cả Spam. Nếu chưa từng đăng ký hoặc không khôi phục được, Owner tự đăng ký [merchant sandbox](https://sandbox.vnpayment.vn/devreg/) bằng email đang dùng và URL FE; tự đặt mật khẩu/nhập CAPTCHA. Theo [VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), hệ thống gửi thông tin kết nối về email đăng ký. Thay cả mã và key trên Render bằng đúng bộ nhận được; không suy key từ TmnCode hoặc dùng ngẫu nhiên key trong tutorial. Cookie/login lỗi thì xử lý trước theo [hướng dẫn CORS/phiên](../SCMS-HUONG-DAN-CORS-DEMO-CLOUDFLARE.md).

`note.md` có đoạn gọi cặp cấu hình cũ là “Sandbox chung”; đây không phải bằng chứng VNPay cấp cho nhóm. `.env` local cũng không chứng minh cặp nào đang hiệu lực ở gateway. Không thay cấu hình Render bằng cặp tìm được trong ghi chú chỉ vì nó khác cặp đang lỗi; cần nguồn cấp/email hoặc xác nhận từ VNPay. Không chép key từ các file này sang tài liệu/Git.

**Điền form đăng ký hiện hành:** mở [Đăng ký Merchant môi trường test](https://sandbox.vnpayment.vn/devreg/), điền theo bảng dưới rồi tự gửi form:

| Trường | Giá trị cho project |
|---|---|
| Tên website | `SCMS Demo` |
| Địa chỉ URL | `https://scms-demo-04nov.pages.dev` — website FE công khai, không phải endpoint Return/IPN |
| Email đăng ký | Email thật Owner/nhóm kiểm soát và nhận được thư; không dùng email account demo `@scms.com` |
| Mật khẩu | Owner tự đặt; form hiện giới hạn6–15ký tự. Dùng chữ hoa, chữ thường và số; đây là mật khẩu merchant, không phải HashSecret |
| Nhập lại mật khẩu | Nhập lại đúng mật khẩu vừa đặt |
| Nhập mã xác nhận | Nhập CAPTCHA đang hiện trên chính form |

Kiểm Inbox/Spam sau khi đăng ký; nếu báo email đã tồn tại thì dùng luồng khôi phục trên cổng VNPay hoặc liên hệ hỗ trợ, không đoán key. Khi nhận bộ kết nối, lấy `vnp_TmnCode` và `vnp_HashSecret` điền vào hai biến Render cùng tên tương ứng ở bước1 bên dưới. Nếu bước tiếp theo yêu cầu callback: Return=`https://scms-app-p1gy.onrender.com/api/payments/vnpay/return`; IPN=`https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`. `FRONTEND_PAYMENT_RESULT_URL` có thể dùng trang hóa đơn đang tồn tại `https://scms-demo-04nov.pages.dev/member/invoices`; đây chỉ là trang đọc trạng thái, không cập nhật Paid. Deploy xong lấy URL thanh toán mới vì link cũ có chữ ký/key và hạn cũ.

**Nếu nhập OTP xong bị chuyển tới localhost/payment-result:** `payment.frontend-result-url` đang dùng giá trị local (mặc định khi thiếu `FRONTEND_PAYMENT_RESULT_URL`, hoặc bị override). Thêm/sửa biến dưới đây trên đúng service Render, Save/rebuild/deploy rồi chờ Live:

```text
FRONTEND_PAYMENT_RESULT_URL=https://scms-demo-04nov.pages.dev/member/invoices
```

Giữ `VNPAY_RETURN_URL` trỏ endpoint BE như trên; không thay nó bằng trang hóa đơn FE. Trong DevTools, request BE `/api/payments/vnpay/return?...` trả302 thì xem Headers → Response Headers → `Location`; body có thể trống. Request tiếp theo tới localhost bị `ERR_CONNECTION_REFUSED` không có HTTP response nên không thể đọc Response. Backend có thể kiểm riêng cấu hình redirect bằng GET Return không tham số, chặn follow redirect: signature bị từ chối97 và chỉ xem Location, không gọi gateway/IPN hoặc xác nhận giao dịch.

Đối soát **hóa đơn cũ trước khi thanh toán lại**: đăng nhập FE bằng đúng Member, xác minh `/api/auth/me`200, mở `/member/invoices` và đọc `GET /api/members/me/invoices` hoặc `/{invoiceId}`. Xem `paymentStatus`, `reconciliationRequired`, `paymentDate`, `gatewayTransactionRef`. `status=00` do BE gắn vào URL kết quả hiện chỉ xác nhận chữ ký Return và tìm thấy hóa đơn; không chứng nhận Paid hay transaction response thành công. Nếu hóa đơn vẫn Pending/Expired sau lần sandbox đã xác nhận thành công ở ngân hàng, kiểm merchant IPN URL và log server; IPN là server-to-server nên không xuất hiện trong Network FE. Không sửa DB/tự gọi IPN hoặc suy Paid từ redirect; Paid cần đối soát theo D10 khi quyền giữ/hạn không còn.

Nếu đã quay về đúng Pages `/member/invoices` nhưng còn “Chờ thanh toán”, không đổi FRONTEND_PAYMENT_RESULT_URL lần nữa. Bấm **Tải lại**, kiểm response mới nhất của ownInvoice sau khi có phiên: API Paid nhưng UI chưa đổi thì kiểm FE refresh/cache; API Pending/Expired thì kiểm IPN chưa ghi nhận/đã bị từ chối/lỗi. Không lấy nhãn UI hay query `status=00` làm bằng chứng trạng thái DB. Nhánh IPN hiện không có log nhận request/RspCode riêng; log console trống không thay được lịch sử gọi từ VNPay hoặc access log thực tế. Chờ xác minh cấu hình IPN phía VNPay trước khi tạo thêm giao dịch sandbox.

Nếu đọc được `Expired`, `paymentDate=null`, `gatewayTransactionRef=null`: chưa có bằng chứng IPN thành công đã commit. Không kết luận gateway chưa gọi chỉ từ ba field này; IPN có thể chưa tới, bị từ chối, lỗi hoặc giao dịch ngân hàng không thành công. Trong Network của **lần cũ**, chọn BE `return?vnp_...`302 → Payload → Query String Parameters (hoặc Headers → Request URL), lấy riêng `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_TransactionNo`, `vnp_PayDate`; không cần gửi chữ ký/full URL. Kiểm IPN URL đã đăng ký với **VNPay**: `https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`. Chỉ đặt biến Render không đăng ký IPN với gateway. Nếu cổng merchant không có nơi cấu hình thì gửi URL này cho hỗ trợ VNPay và nhờ đối chiếu lịch sử gọi IPN/RspCode của giao dịch cũ theo [hướng dẫn PAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html). Kiểm log đúng khoảng thời gian; không có dòng trong console ứng dụng chưa chứng minh không có request nếu access logging chưa bật. Theo D10, IPN thành công hợp lệ đến sau hạn vẫn phải ghi nhận Paid/cần đối soát và không hồi sinh quyền; không tăng thời gian giữ hoặc sửa Expired thành Paid bằng tay để vượt bước nghiệm thu. Chỉ thử sandbox mới sau khi chốt kết quả giao dịch cũ và cấu hình callback/redirect đúng.

**Kiểm IPN sandbox trước khi thêm relay:** [hướng dẫn của SDK cộng đồng lehuygiang28/vnpay](https://vnpay.js.org/ipn/config-ipn) chỉ chỗ tự đặt IPN tại **Thông tin tài khoản** góc phải → danh sách website → biểu tượng sửa Terminal. Sau đăng nhập có thể mở [TerminalEdit](https://sandbox.vnpayment.vn/merchantv2/Account/TerminalEdit.htm). Owner đã mở được form và báo bấm Hoàn thành với IPN URL `https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`, GET và HMACSHA512; tiếp tục kiểm lưu/callback theo mục bên dưới. Chọn đúng Terminal/TmnCode đang dùng. Menu **Cài đặt thông báo** Owner đã mở chỉ là email, khác Terminal. Nếu tài khoản khác không có quyền/trường này thì dùng hỗ trợ bên dưới. Không suy IPN từ website/Return URL; không đưa HashSecret lên ảnh/báo cáo. Cấu hình xong vẫn cần xác minh callback thật và state đã commit, không mặc định đã sửa xong.

<a id="ipn-acceptance"></a>
**Sau khi bấm Hoàn thành ở Terminal:** mở lại đúng Terminal để kiểm IPN URL còn giữ nguyên, giao thức GET và HMACSHA512. Không cần deploy BE, chạy SQL hoặc đổi env cho thao tác lưu phía VNPay. Lượt sandbox sau cấu hình đã được Owner gửi response Paid và thấy FE cập nhật ngay; bằng chứng/giới hạn tại [Active](00-MASTER-CHECKLIST.md#active). Checklist dưới dành cho lần nghiệm thu được phép khác, không yêu cầu tạo thêm đơn để lặp ca đã thành công.

1. Dùng tài khoản/fixture sandbox được phép, xác minh `/api/auth/me`200 và mở F12 → Network → Preserve log. Tạo đúng một đơn thử riêng, không thu lại hóa đơn cũ đang chờ đối soát. Ghi invoiceId/code và expiresAt.
2. Thanh toán trên `sandbox.vnpayment.vn` bằng [thẻ thử NCB do VNPay công bố](https://sandbox.vnpayment.vn/apis/vnpay-demo/); không dùng app ngân hàng/thẻ thật. Hoàn tất trong thời gian giữ5phút.
3. Sau Return, đọc GET ownInvoice mới sau commit (hoặc tải lại danh sách), không dùng receipt trước Paid hay `status=00` làm kết quả. Cần paymentStatus=Paid, paymentDate/gatewayTransactionRef có giá trị; nếu xử lý muộn hoặc quyền không còn thì Paid+reconciliationRequired=true và không cấp lại quyền. Nếu còn Pending, chờ15–30giây và GET lại một lần, giữ nguyên đơn khi chẩn đoán.
4. IPN là server→server, không có request IPN trong DevTools FE. Pending/Expired chưa chứng minh IPN không tới; cần kiểm provider delivery/HTTP/RspCode nếu chưa Paid. QueryDR vẫn có thể timeout độc lập; không sửa chữ ký/env hoặc thêm relay chỉ vì status cũ chưa đổi. Cấu hình mới không bảo đảm gateway gửi lại giao dịch cũ. GET hiện trạng các đơn cũ không tạo thanh toán mới.

<a id="vnpay-options"></a>
**Lựa chọn đường kết nối sau probe:** IPN là VNPay→BE, QueryDR là BE→VNPay; lỗi outbound hiện tại không tự chứng minh inbound cũng lỗi. Giữ IPN là kênh thông báo chính và QueryDR là xác minh chủ động theo D10; Return chỉ hiển thị.

| Phương án | Đánh đổi và phạm vi BE |
|---|---|
| Tự cấu hình IPN sandbox | Kiểm trước: dùng endpoint BE hiện có, không cần relay hoặc migration mới. Không chữa đường QueryDR và không bảo đảm VNPay tự gửi lại giao dịch cũ. |
| Khôi phục Render→VNPay trực tiếp | Ít thành phần vận hành; cần khoanh vùng mạng/provider, chưa có thời hạn xử lý. IP riêng chỉ cân nhắc khi có căn cứ allowlist; [Render yêu cầu Pro và phí riêng](https://render.com/docs/dedicated-ips), không bảo đảm mua gói sẽ hết lỗi. |
| Relay local qua Cloudflare Tunnel | Giữ BE/DB hiện tại nhưng thêm transport có xác thực và giới hạn đích. Phải giữ máy/mạng/process chạy; [Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/) đổi hostname mỗi lần, không bảo đảm uptime; phù hợp demo tạm. |
| Relay chạy cloud (Worker/VM) | Không phụ thuộc laptop; thêm dịch vụ, auth/token, giới hạn truy cập và vận hành. [Worker có fetch outbound](https://developers.cloudflare.com/workers/runtime-apis/fetch/) nhưng phải probe Worker→VNPay trước, chưa có bằng chứng tuyến này hoạt động. |
| Chuyển BE sang host/region khác | Giữ QueryDR trực tiếp nhưng phải kiểm tuyến mạng trước, chuyển env/deploy và có thể cập nhật API base/CORS/Return/IPN. Phạm vi lớn hơn relay, chưa cần làm ngay. |

Mọi relay chỉ chuyển tiếp request đã ký tới đích VNPay cố định và trả nguyên phản hồi; HashSecret, xác minh HMAC/merchant/reference/amount/status và nhánh ghi Paid vẫn ở BE. Cần thêm transport/auth có kiểm thử, không chỉ thay URL sang một proxy công khai. Endpoint FE và migration15 không cần thay vì lựa chọn đường mạng. Sau cấu hình Terminal, lượt sandbox đã ghi Paid; Owner chốt giữ nguyên phần đã triển khai, chưa chọn relay. Không thay SDK để chữa timeout: [SDK TypeScript](https://github.com/lehuygiang28/vnpay/blob/43627794aa4735e82d539831b81d53c8891cce19/src/services/query.service.ts) vẫn POST HTTP; [Omnipay PHP](https://github.com/phpviet/omnipay-vnpay/blob/91e2059838d44afa42f0173122fc499a709f7e7b/src/Message/QueryTransactionRequest.php) dùng GET/merchant.html cũ, khác [QueryDR2.1 POST JSON](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html) của SCMS.

<a id="querydr-proposal"></a>
<a id="querydr"></a>
**QueryDR — các bước cho demo sandbox**

**Quyết định hiện hành:** Owner giữ nguyên code/endpoint, bảng `vnpay_payment_attempts`, migration15 và các env đã thêm. Không thực hiện đề xuất tắt QueryDR trước đó; bảng vẫn được dùng khi tạo link thanh toán. Các bước dưới là cấu hình/kiểm tra khi cần, không yêu cầu chạy lại SQL, đổi env hoặc deploy chỉ vì đổi tên tài liệu. QueryDR live vẫn chưa nghiệm thu; xem [D10](04-DEBUG-NOTE.md#d10).

1. DB hiện có: chạy nguyên [15_QueryDR.sql](../database/migrations/15_QueryDR.sql) một lần trước deploy BE. File chỉ thêm bảng pay attempt và các ràng buộc liên quan, không sửa/xóa chứng từ cũ. Nếu đã chạy thành công thì chuyển bước2.
2. Render → Environment: dùng cấu hình dưới đây, chọn **Save, rebuild, and deploy**, chờ **Live**.

   | Biến | Giá trị |
   |---|---|
   | `VNPAY_QUERY_ENABLED` | `true` |
   | `VNPAY_QUERY_SETTLEMENT_ENABLED` | `true` |
   | `VNPAY_QUERY_URL` | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` |
   | `VNPAY_QUERY_IP_ADDR` | Để trống hoặc không thêm; BE tự lấy IP host |

3. Tạo đúng một đơn VNPay mới sau deploy, thanh toán bằng thẻ thử NCB. Khi quay về, FE cần POST `verify-vnpay` cho đúng `invoiceId`, rồi GET lại hóa đơn. Chỉ tải lại GET sẽ không tự chạy QueryDR. Nếu FE chưa tích hợp, dùng Console theo đoạn dưới.
4. Xem `verificationStatus`, `invoice.paymentStatus` và `reconciliationRequired`. `VERIFIED_PAID`/`ALREADY_PAID` mới xác nhận BE đã ghi nhận tiền. Lỗi/chưa xác minh được thì kiểm tra cùng đơn, không thanh toán thêm. Nếu xử lý đã tới/quá hạn5phút, Paid đi kèm đối soát và không cấp lại quyền học.

`SETTLEMENT_ENABLED=false` là **tùy chọn chỉ kiểm tra**, trả `VERIFIED_ONLY` khi VNPay xác nhận nhưng không ghi Paid. Không bắt buộc phải đi qua chế độ này trước khi bật true cho demo sandbox. Dù true, BE vẫn kiểm chữ ký/merchant/reference/amount/type/status và chống lặp. Default hai cờ trong source vẫn false. Giữ nguyên cặp merchant/key đang tạo được link pay; `VNPAY_RETURN_URL` là BE Return, `FRONTEND_PAYMENT_RESULT_URL` là trang hóa đơn Pages.

**Kiểm tra thủ công khi FE chưa tự POST:** mở trang hóa đơn vừa quay về, đăng nhập đúng Member → F12 → Console, chạy một lần:

```javascript
const api = "https://scms-app-p1gy.onrender.com";
const id = new URL(location.href).searchParams.get("invoiceId");
const me = await fetch(api + "/api/auth/me", {credentials: "include"});
if (!me.ok || !/^[1-9]\d*$/.test(id || "")) throw new Error("Cần đăng nhập và mở URL hóa đơn vừa quay về");
const result = await fetch(api + "/api/members/me/invoices/" + id + "/verify-vnpay", {
  method: "POST", credentials: "include", headers: {"Content-Type": "application/json"}, body: "{}"
});
console.log("HTTP", result.status, JSON.stringify(await result.json(), null, 2));
```

Chờ `retryAfterSeconds` trước khi chạy lại. Không lấy `status=00` trên URL làm chứng từ. `ATTEMPT_MISSING` là đơn cũ thiếu metadata gốc; cần đối soát riêng. Nếu `QUERY_UNAVAILABLE`, xem log Render dòng `VNPay QueryDR invoice=... attempt=... reason=...`; chỉ chia sẻ lý do cố định, không gửi key/cookie. Contract/status đầy đủ tại [docs01](01-API-CONTRACT-FE-BE.md#payments), bàn giao VNPay ngày04/11 tại [đây](../SCMS-BAN-GIAO-FE-VNPAY-2026-11-04.md).

**Nếu lỗi TLS/chứng chỉ Java:** có thể thử thêm Environment `JAVA_TOOL_OPTIONS=-Dcom.sun.security.enableAIAcaIssuers=true`, giữ các tùy chọn đã có nếu biến tồn tại, rồi Save/redeploy và kiểm lại **cùng đơn**. Java sẽ tìm chứng chỉ CA trung gian để dựng chuỗi tin cậy, vẫn xác minh TLS/hostname ([Oracle PKIX](https://docs.oracle.com/en/java/javase/17/security/java-pki-programmers-guide.html)). Lựa chọn này đã giúp probe HTTPS Java cục bộ kết nối được VNPay; chưa tự chứng minh nguyên nhân trên Render. Không đổi secret hoặc tạo thêm thanh toán để thử.

<a id="querydr-network"></a>
**Nếu log là CONNECT_TIMEOUT — kiểm tra trên chính Render:**

1. Merge code có `VnPayNetworkProbe` vào đúng nhánh Render deploy rồi đợi bản mới Live. Với repo hiện tại, merge `tai → main`. Chỉ thêm env/redeploy commit cũ sẽ không có `VNPAY_NET`; xác minh bản deploy trước khi tìm log.
2. Thêm Environment `VNPAY_NETWORK_PROBE_ENABLED=true`, lưu và deploy, chờ Live.
3. Mở Logs, tìm `VNPAY_NET`, lấy các dòng tới `finished=true`. Probe tự chạy một lần mỗi lần khởi động khi bật cờ; không cần tạo đơn hoặc POST để kích hoạt. Lượt hiện tại đã có log hoàn tất; Owner chốt giữ nguyên các env đã thêm, không tự tắt cờ hoặc redeploy chỉ để lặp chẩn đoán.

Probe chỉ kiểm DNS, TCP443 tối đa15giây và HTTPS HEAD ở ngưỡng kết nối3/15giây tới VNPay sandbox cùng `example.com` làm đối chứng. TLS/hostname vẫn được kiểm bình thường. Không có merchant, secret, cookie, dữ liệu hóa đơn hoặc body QueryDR; không xác nhận Paid.

- TCP tới VNPay timeout nhưng đối chứng kết nối được: khoanh vùng đường mạng tới VNPay; chưa đủ để khẳng định VNPay chặn IP Render. Khi cần, IP outbound lấy tại Render → service → Connect → Outbound theo [Render](https://render.com/docs/outbound-ip-addresses).
- TCP được nhưng HTTPS lỗi/timeout: kiểm tra TLS/handshake. HTTP405 từ HEAD chứng minh đã kết nối HTTPS, không là lỗi thanh toán vì đây không phải POST QueryDR.
- HTTPS3giây thất bại nhưng15giây thành công: có thể thử `VNPAY_QUERY_CONNECT_TIMEOUT_SECONDS=10`, `VNPAY_QUERY_REQUEST_TIMEOUT_SECONDS=15`, deploy rồi xác minh lại **cùng đơn**. Mặc định vẫn3/5; mỗi giá trị phải từ1 tới15, request không nhỏ hơn connect. Sai cấu hình trả reason `INVALID_QUERY_TIMEOUT` và không gửi request. FE dành60giây riêng cho POST xác minh.

`VNPAY_QUERY_IP_ADDR` chỉ là trường IP bên trong request, không thay đổi địa chỉ IP/đường mạng outbound của Render. Không sửa trường này để chữa TCP timeout.

Bản vá log transport mới phân biệt `CONNECT_TIMEOUT`, `REQUEST_TIMEOUT`, `TLS_FAILURE`, `DNS_FAILURE`, `SERVER_IP_LOOKUP_FAILED`, `CONNECTION_FAILED`, `IO_FAILURE`, `PROVIDER_HTTP_<status>` và cấu hình sai. Phản hồi FE vẫn là `QUERY_UNAVAILABLE`; log không chứa exception message, body hoặc credential. Nếu runtime vẫn chỉ ghi reason chung thì bản vá chưa được publish hoặc lỗi chưa thuộc nhóm đã phân loại. Trạng thái publish theo docs00.

<details>
<summary>Chi tiết kỹ thuật và kiểm thử local — không phải bước cấu hình Render</summary>

**Request tới provider:** HTTP client chỉ POST JSON `vnp_Command=querydr`, không gọi refund. UUID32 RequestId và CreateDate do BE sinh theo Clock/HCM; TransactionDate lấy đúng vnp_CreateDate gốc trong attempt. Chuỗi HMAC-SHA512 nối `|` theo thứ tự `RequestId,Version,Command,TmnCode,TxnRef,TransactionDate,CreateDate,IpAddr,OrderInfo` (JSON có tiền tố `vnp_`), không dùng query string của pay URL. Key chỉ tính HMAC, không gửi trong request.

**Response:** kiểm HMAC-SHA512 theo thứ tự `ResponseId,Command,ResponseCode,Message,TmnCode,TxnRef,Amount,BankCode,PayDate,TransactionNo,TransactionType,TransactionStatus,OrderInfo,PromotionCode,PromotionAmount`; so sánh constant-time, giữ nguyên giá trị, tùy chọn thiếu là rỗng đúng vị trí. ResponseId không là RequestId. Chỉ ghi thành công nếu chữ ký hợp lệ, merchant khớp attempt/cấu hình, TxnRef đúng invoiceCode, Amount đúng số nguyên Invoice.amount×100 và snapshot, loại01, responseCode00/status00, TransactionNo hợp lệ/chưa gắn invoice khác. Lỗi/timeout/thiếu hash/chưa có kết quả không ghi Paid và không chứng minh khách chưa trả.

**Khóa/giới hạn:** mặc định connect3giây/request5giây, có cấu hình giới hạn tại mục kiểm mạng; tối đa3attempt/lượt, luân phiên để không bỏ link cũ; cooldown30giây/hóa đơn bằng DB lock. HTTP chạy ngoài transaction giữ khóa. Khi ghi, kiểm lại owner/role/center/amount/state dưới khóa center→invoice. IPN tới trước thì trả state hiện hành, không cấp thêm credit/thông báo. Biên5phút theo Clock lúc xử lý, không dùng PayDate bỏ cutoff: tới/quá hạn hoặc quyền không còn thì Paid+reconciliationRequired, không hồi sinh quyền. Đơn cũ thiếu metadata trả `ATTEMPT_MISSING`; deploy không tự sửa invoice20/22 đã báo trước đây.

**Kiểm local:** chạy `python scripts/test-querydr-migration.py`, rồi `python scripts/test-querydr-local.py`. Migration runner chỉ dùng PostgreSQL Docker loopback55440 và DB tạm `scms_querydr_*`. BE runner tạo bản sao source không có.env, chạy `./mvnw.cmd clean test` rồi HTTP PostgreSQL `ddl-auto=none` trên DB behavior đã migration. Kết quả ở `artifacts/runs/querydr/validation.json`, tổng hợp vào QA duy nhất. Provider là HTTP giả lập cục bộ, không là VNPay live. Có kiểm chữ ký độc lập, sai merchant/ref/amount, timeout, owner/role/center, retry/IPN race, cutoff,12credit/thông báo không trùng và state sau commit. Các lần kiểm local không thay thế kiểm QueryDR có phiên trên Render; trạng thái hiện hành xem docs00.

</details>

Có thể gửi yêu cầu qua kênh hỗ trợ trong email cấp merchant hoặc `hotrovnpay@vnpay.vn` (địa chỉ được [VNPay công bố](https://sandbox.vnpayment.vn/paymentv2/)); Owner tự gửi, không đính kèm HashSecret/cookie/mật khẩu. Mẫu nội dung:

```text
Nhờ hỗ trợ kiểm tra/thiết lập IPN cho merchant sandbox [TmnCode].
Website: https://scms-demo-04nov.pages.dev
IPN URL (GET): https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn
Return URL: https://scms-app-p1gy.onrender.com/api/payments/vnpay/return
Giao dịch: [vnp_TransactionNo], mã đơn [vnp_TxnRef], số tiền [VND].
Thời gian thành công: [vnp_PayDate theo GMT+7]. Merchant báo thành công
nhưng hóa đơn SCMS chưa ghi nhận thanh toán. Nhờ xác nhận IPN URL đang
hiệu lực, lịch sử gọi/HTTP status/RspCode, và khả năng gửi lại IPN
cho giao dịch này sau khi khắc phục nếu được hỗ trợ.
```

1. Trong Render → service `scms-app-p1gy` → Environment, đối chiếu `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` với **cùng bộ sandbox** VNPay cấp. Không kèm dấu nháy hoặc khoảng trắng/xuống dòng đầu-cuối. Kiểm thêm Environment Groups và cấu hình Spring override nếu có; `.env` local không chứng minh cấu hình Render. Nếu sửa giá trị, chọn **Save, rebuild, and deploy**, đợi deploy Live rồi lấy URL mới; Restart service giữ cấu hình của lần deploy đang chạy. [Render Environment](https://render.com/docs/configure-environment-variables), [Deploys](https://render.com/docs/deploys).
2. Đối chiếu `VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` và `VNPAY_RETURN_URL=https://scms-app-p1gy.onrender.com/api/payments/vnpay/return`. Return trỏ BE; IPN gateway trỏ `/api/payments/vnpay/ipn`, không gọi tay để thử. `FRONTEND_PAYMENT_RESULT_URL` là trang FE đọc trạng thái hóa đơn, không thay BE Return. FE hiện hành cần có route thật; `/member/invoices` là trang hóa đơn đang phát hành, không tự mặc định FE đã có `/payment-result`.
3. Trong Chrome/Edge: F12 → Network → bật **Preserve log** trước thao tác trên giao dịch demo được phép. Trong request `pay-online` hoặc `order`, mở Response, lấy **giá trị chuỗi** `paymentUrl` nguyên vẹn, không kèm dấu nháy JSON. Chọn request đầu tiên tới `vpcpay.html`, lấy **Headers → General → Request URL**. URL trang `Error.html` không thay được hai URL này. Không cần hoàn tất thanh toán để lấy dữ liệu; ưu tiên request còn trong log, không tạo hàng loạt đơn hay dùng account seed lịch sử. [Network reference](https://developer.chrome.com/docs/devtools/network/reference/).
4. Có thể gửi riêng hai URL cho người kiểm tra, không gửi HAR/cookie/password. Hoặc tự kiểm offline tại root repo:

   ```powershell
   python scripts/check-vnpay-payment-url.py --compare-browser --check-secret
   ```

   Dán URL theo hai lời nhắc; nhập TmnCode và HashSecret **VNPay cấp** khi được hỏi. HashSecret nhập ẩn, không lưu hoặc gửi mạng; không đặt secret vào lệnh. Chỉ gửi JSON kết quả. Muốn kiểm cấu trúc/so sánh mà chưa kiểm key thì bỏ `--check-secret`. Công cụ không đọc `.env`, không gọi gateway và không thay invoice/credit.

| Kết quả offline | Hướng xử lý |
|---|---|
| Chữ ký khớp key trong ảnh/cấu hình Render và hai URL giống nhau, nhưng gateway vẫn sai chữ ký | Chứng minh request nhất quán với key cấu hình, chưa chứng minh key thuộc merchant ở VNPay. Lấy lại bộ thông tin cấp hoặc nhờ VNPay kiểm merchant/key đang hiệu lực; không đổi encoder/IP/trim chỉ để thử. Khi liên hệ gửi mã tra cứu, thời gian, TmnCode và ảnh lỗi; không gửi HashSecret. |
| `signatureMatchesEnteredSecret=true`, merchant đúng, gateway sandbox đúng | Link khớp key vừa nhập; tiếp tục đối chiếu merchant được VNPay kích hoạt và bộ key đúng môi trường, rồi báo VNPay kèm mã tra cứu nếu gateway vẫn từ chối. Không tự gọi đó là PASS gateway. |
| Chữ ký không khớp key VNPay cấp | Đối chiếu key **hiệu lực trên Render** và source deployed; chưa đủ để kết luận encoder sai. |
| `browserComparison.decodedFieldsEqual=false` | Xem `changedParameters`, tìm đoạn FE/redirect thay dữ liệu. Khác cách biểu diễn nhưng decoded/canonical vẫn giống chưa chứng minh lỗi. |
| `enteredSecretHasEdgeWhitespace=true` | Kiểm tra thao tác copy key; sửa cấu hình sai ở nguồn. Không tự trim riêng hàm HMAC và bỏ quên callback. |
| Return URL sai BE hoặc FE result sai route | Sửa đúng cấu hình điều hướng; đây là lỗi riêng, chưa chứng minh nguyên nhân sai chữ ký. |

Giữ HMAC-SHA512 và encoding nhất quán cho URL/chuỗi ký theo [VNPay 2.1](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html); không thay IPv6 hợp lệ thành localhost chỉ vì có dấu `:`. Kiểm local không thay giao dịch sandbox được gateway chấp nhận, IPN và trạng thái sau commit. Bằng chứng hiện hành xem docs00/QA, không ghi key hoặc URL giao dịch thật vào Git.

## 4. Bundle và backup

QA giữ target production188f6765… khác DB test1a3dfdd7…. Production có dữ liệu đã nâng cấp bằng [production-upgrade.sql](../artifacts/releases/2026-10-27/production-upgrade.sql), SHA256 **01e31ca6a8606b47d321fd61a6860076c68317d3490f3b529cf42414a80db8d7**. Đây là bundle đã chạy, replay guard từ chối; không phải việc cần chạy lại. Execution/postflight/backup metadata ở `artifacts/qa/latest.json →releases.production`. Giữ cột gốc7bảng, không backfill legacy từ amount hoặc cấp lại12credit.

Bootstrap test cũ trong Git có hash khác biên bản28635081…; chưa xác định nguyên nhân. Bản tham chiếu lỗi thời bị loại, QA giữ khoảng trống integrity. Không gọi nó là đúng byte đã duyệt hoặc suy target từ filename “production” cũ. Bootstrap mới cần sinh/review/kiểm/hash mới qua scripts/prepare-phases-release.py; quyền cloud phải đúng bundle/target.

Backup public schema mã hóa DPAPI current Windows user ở `artifacts/private-backups/` ignored, giữ nguyên khi dọn. Không gồm Supabase auth/storage ngoài public; cần tài khoản Windows đã mã hóa để decrypt. Khôi phục local mới bằng `python scripts/prepare-production-upgrade.py --restore-backup <backup-path-from-QA>`; script guard private-backups path và container loopback, không truy cập remote ở chế độ restore.

Chế độ --target-fingerprint của script chuẩn bị đọc/backup remote và kiểm clone, không tự chạy remote DDL; output review vào `artifacts/runs/production-upgrade/<timestamp>`. Approval cũ không mở quyền bundle/target khác, seed/account/giao dịch hoặc live tests mới.

## 5. Hạn chế và lưu trữ

### Chẩn đoán OTP SMTP

Deploy source có `SMTP_SEND_START/SMTP_SEND_FAILED/SMTP_SEND_ACCEPTED`, chờ Live; không cần bật JavaMail debug hoặc thêm env để có log. Giữ nguyên provider/port/credential khi lấy bằng chứng ban đầu. Mở Render Logs, chờ ít nhất60giây từ lần gửi trước rồi bấm Gửi lại mã đúng1lần từ FE; tối đa3OTP/giờ theo contract. Đợi khoảng20giây và tìm `SMTP_SEND_`, lấy các dòng có cùng `attemptId` cùng dòng `OTP email delivery failed...` nếu có. Không chép key, OTP hoặc nội dung thư vào chat/báo cáo.

`SMTP_SEND_START` ghi host/port, STARTTLS **được cấu hình** và ba cờ login/password/sender có giá trị; không ghi các giá trị nhạy cảm hoặc khẳng định TLS đã thương lượng. `SMTP_SEND_FAILED` ghi category, mã SMTP (`-1` là không có), tên loại exception và elapsedMs; không ghi raw message/stack trace, recipient hoặc failed-message map. Nhóm lỗi: DNS, TIMEOUT, CONNECTION, TLS, AUTHENTICATION, SMTP_REJECTED, MESSAGE_FORMAT, MISSING_SENDER hoặc UNKNOWN. TIMEOUT/CONNECTION chưa đủ chứng minh Render hay Brevo chặn; AUTHENTICATION cần kiểm SMTP login/key và chính sách tài khoản; SMTP_REJECTED cần đối chiếu smtpCode và trạng thái Brevo.

Khi AUTHENTICATION và Brevo bật chặn SMTP IP: mở **Brevo → Settings → Security → Authorized IPs**, kiểm trạng thái SMTP keys và tab Unauthorized IP addresses. Lấy dải IP tại **Render → Projects → project chứa backend → service backend → Connect → Outbound**, không phải Settings của workspace. Chỉ cho phép IP đã đối chiếu thuộc dải outbound của đúng service; không dùng Remote address của request FE trong Chrome. Quyền IP lưu trên Brevo không cần deploy lại BE. Nếu Render dùng IP khác sau này, kiểm lại dải/entry bị chặn trước khi bổ sung; không tắt chặn hoặc thay key chỉ để thử khi chưa kiểm quyền IP. [Render Outbound IPs](https://render.com/docs/outbound-ip-addresses), [Brevo Authorized IPs](https://help.brevo.com/hc/en-us/articles/5740111683858-Authorize-and-block-IP-addresses-for-API-and-SMTP-security).

`SMTP_SEND_ACCEPTED` chỉ chứng minh JavaMailSender trả về thành công, chưa chứng minh Inbox. Kiểm Brevo **Transactional → Logs** theo đúng thời gian/recipient, rồi Inbox/Spam và verify-registration. `code:null` của API là mã trạng thái tùy chọn, không phải OTP; POST/OPTIONS200 không là email delivery.

### Cấu hình và lưu trữ

Ghi nhận27/10: Render Free,dbUP/mailDOWN. [Render Free](https://render.com/docs/free) chặn SMTP25/465/587. Owner04/11 chọn thử Brevo SMTP2525 và báo đã xác minh sender/thêm env. Cấu hình `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=2525`, `MAIL_USERNAME` là SMTP login Brevo cấp, `MAIL_APP_PASSWORD` là toàn bộ SMTP key (không phải API key), `MAIL_FROM_EMAIL` là sender đã xác minh; giữ STARTTLS đang bật và để trống `REGISTRATION_DEMO_OTP` khi kiểm OTP thật. Không chép credential vào Git. [Brevo hỗ trợ2525](https://help.brevo.com/hc/en-us/articles/10905415650322-Which-SMTP-port-should-I-use-Port-587-465-or-2525); vẫn phải kiểm kết nối/TLS và delivery từ Render, không suy thành công từ ảnh cấu hình. Nếu không kết nối được, phương án HTTPS email API vẫn cần adapter riêng. Các cổng SQLServer/browser/gateway/full authenticated business matrix ở docs00.

FE nhận docs01 + `artifacts/fe-handoff/openapi.json`. QA ở `artifacts/qa/latest.json`; bundle ở artifacts/releases; raw mới ở artifacts/runs (ignored); private backup giữ nguyên. Lịch sử đầy đủ trước chuẩn hóa truy qua Git `a4f3cd0`, ví dụ `git show a4f3cd0:artifacts/fe-handoff/2026-10-27/render-get-checks.json`. Không phải mọi raw log đều từng commit; raw run cuối còn local như mục2. Không tạo lại inventory/schema MD/HTML trace để tăng số file bàn giao.
