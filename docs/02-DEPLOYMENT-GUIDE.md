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
| REGISTRATION_DEMO_OTP | Để trống production; mã cố định chỉ cho demo đăng ký được chọn rõ |
| CLOUDINARY_CLOUD_NAME,CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET | Nếu dùng avatar, secret ngoài Git |

Đây là default/source, không chứng nhận dashboard đã đặt đúng; external Spring configuration có thể override. Cùng host/password chưa chứng minh cùng Supabase project. HikariCP là pool JDBC của ứng dụng, khác Supabase pooler; source default5connection/timeout30000ms. Fingerprint chỉ đối chiếu target hiệu lực, không là chứng nhận toàn API.

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

Output `artifacts/runs/phases/<timestamp>` ignored; raw run cuối trước chuẩn hóa giữ local ở `artifacts/runs/phases/2026-10-27-verified`. Không mặc định có raw run trên máy khác. Communication riêng: `python scripts/test-communication-features.py`, output `artifacts/runs/communication/<run>`; DB/SMTP loopback, không email ra ngoài hoặc .env cloud.

### Kiểm gói QA đã giữ

```powershell
python scripts/audit-fe-contract.py
```

Mặc định offline, đọc một OpenAPI/QA để kiểm route/source và compatibility cơ bản của exchanges đã ghi. Không probe/login Render hoặc sinh thêm nhiều tài liệu. Có --openapi/--qa để chọn bản khác. Nullability/required/format/range/error cần kiểm riêng; không coi kiểm type/property là full JSON Schema.

Báo cáo đủ: `Dxx/case →actor/fixture →expected →response+state →PASS/FAIL/BLOCKED/NOT_RUN →môi trường/SHA →một tham chiếu`. Không lấy số request/assertion làm mức hoàn tất; chỉ mở đúng scenario/index trong QA khi cần.

## 3. Deploy

Kiểm source/SHA/CI/schema tương thích và quyền migration trước push nhánh Render theo dõi. Owner đã xác nhận main; kiểm lại cấu hình khi bắt đầu vận hành mới. Khi cần schema: backup/restore proof →duyệt bundle cụ thể →atomic migration/guard →postflight →deploy code tương thích. Không reset/restore đè dữ liệu để chữa code lỗi.

Theo dõi Live identity, không coi Git merge/dashboard Live là nghiệm thu nghiệp vụ. Nếu yêu cầu kiểm sau5phút, runner từ chối chạy sớm:

```powershell
python scripts/check-render-production.py --output artifacts/runs/render/check.json --source-commit <LIVE_SHA> --require-runtime-identity --expected-db-fingerprint <TARGET_HASH> --pushed-at-utc <ISO_UTC>
```

`/api/health` chỉ liveness/identity, không mở DB connection; `/actuator/health` kiểm db/mail/... và giữ show-details=never. Ghi nhận27/10: Owner dùng `/api/health` trong dashboard, render.yaml khai `/actuator/health`; không giả hai cấu hình giống nhau hoặc đổi path để che dependency hỏng.

CORS/cookie cần browser FE hiện hành kiểm preflight/Set-Cookie/cookie gửi lại. Gateway live cần Merchant/callback/fixture được phép; FE không gọi IPN. Commit chỉ docs/evidence có thể dùng `[skip render]` tránh deploy không cần thiết, theo [Render deploy docs](https://render.com/docs/deploys#skipping-an-auto-deploy).

## 4. Bundle và backup

QA giữ target production188f6765… khác DB test1a3dfdd7…. Production có dữ liệu đã nâng cấp bằng [production-upgrade.sql](../artifacts/releases/2026-10-27/production-upgrade.sql), SHA256 **01e31ca6a8606b47d321fd61a6860076c68317d3490f3b529cf42414a80db8d7**. Đây là bundle đã chạy, replay guard từ chối; không phải việc cần chạy lại. Execution/postflight/backup metadata ở `artifacts/qa/latest.json →releases.production`. Giữ cột gốc7bảng, không backfill legacy từ amount hoặc cấp lại12credit.

Bootstrap test cũ trong Git có hash khác biên bản28635081…; chưa xác định nguyên nhân. Bản tham chiếu lỗi thời bị loại, QA giữ khoảng trống integrity. Không gọi nó là đúng byte đã duyệt hoặc suy target từ filename “production” cũ. Bootstrap mới cần sinh/review/kiểm/hash mới qua scripts/prepare-phases-release.py; quyền cloud phải đúng bundle/target.

Backup public schema mã hóa DPAPI current Windows user ở `artifacts/private-backups/` ignored, giữ nguyên khi dọn. Không gồm Supabase auth/storage ngoài public; cần tài khoản Windows đã mã hóa để decrypt. Khôi phục local mới bằng `python scripts/prepare-production-upgrade.py --restore-backup <backup-path-from-QA>`; script guard private-backups path và container loopback, không truy cập remote ở chế độ restore.

Chế độ --target-fingerprint của script chuẩn bị đọc/backup remote và kiểm clone, không tự chạy remote DDL; output review vào `artifacts/runs/production-upgrade/<timestamp>`. Approval cũ không mở quyền bundle/target khác, seed/account/giao dịch hoặc live tests mới.

## 5. Hạn chế và lưu trữ

Ghi nhận27/10: Render Free,dbUP/mailDOWN. [Render Free](https://render.com/docs/free) chặn SMTP25/465/587; JavaMailSender587 hiện tại chưa gửi OTP cloud. Giữ Free cần HTTPS email provider/sender/domain/API key/adapter và delivery test được phép. Đợt chuẩn hóa không chọn provider/đổi plan. Các cổng SQLServer/browser/gateway/full authenticated business matrix ở docs00.

FE nhận docs01 + `artifacts/fe-handoff/openapi.json`. QA ở `artifacts/qa/latest.json`; bundle ở artifacts/releases; raw mới ở artifacts/runs (ignored); private backup giữ nguyên. Lịch sử đầy đủ trước chuẩn hóa truy qua Git `a4f3cd0`, ví dụ `git show a4f3cd0:artifacts/fe-handoff/2026-10-27/render-get-checks.json`. Không phải mọi raw log đều từng commit; raw run cuối còn local như mục2. Không tạo lại inventory/schema MD/HTML trace để tăng số file bàn giao.
