# Bàn giao nâng cấp production thực — 27/10/2026

Target SHA256 `188f67654d48aa11c970703f2cdaefc79290f1173a7dedaf93547e74bc6b1048` khớp Render; DB bootstrap1a3dfdd7… trước đây là **test**. Owner duyệt riêng bundle có dữ liệu sẵn, đã thực thi thành công. Không cần chạy lại.

- Bundle: [SQL](scms-existing-production-upgrade.sql), SHA256 `01e31ca6a8606b47d321fd61a6860076c68317d3490f3b529cf42414a80db8d7`.
- [Plan/proof](upgrade-plan.json): backup restore, snapshot mọi cột gốc7bảng, source migration replay, bundle replay refused, referenced duplicate guard rollback.
- [Java executor proof](executor-local-proof.json), [replay rollback](executor-replay-guard.json), [wrong target refused](executor-wrong-target.json).
- [Production commit](production-applied.json), [postflight](production-postflight.json), [data check](production-data-after.json):50bảng/9môn,30users/6invoices/5subscriptions/2sessions/2enrollments; originalRowsPreserved tại commit.
- [Render sau upgrade](render-after-upgrade.json):21/22checks, đúngtarget/Live08227b8/9môn/OpenAPI149paths;health503 chưa đạt.

Không bootstrap/reset/reseed;9môn dư được loại chỉ sau guard exact snapshot/no-FK. Invoice amount/credit/account/dữ liệu cũ giữ nguyên, subscription legacy thiếu căn cứ mới phải đối soát riêng; không cấp lại12credit. Migrations mới và indexes được bổ sung. Hóa đơn/gói demo không được tạo trên production.

Backup public schema (không bao gồm Supabase auth/storage) mã hóa Windows DPAPI current user:
`artifacts/private-backups/production-188f67654d48-20261027_204720.dump.dpapi`. [Metadata](backup-record.json) ghi cipher hash/roundtrip. File backup và diagnostic riêng tư gitignored, không push. Chỉ tài khoản Windows đã mã hóa mới decrypt được; cần backup riêng được Owner kiểm soát nếu chuyển máy.

Khôi phục **chỉ vào DB local mới**, container scms-phases-20261027/PostgreSQL17, bind127.0.0.1:55440:

```powershell
python scripts/prepare-production-upgrade.py --restore-backup artifacts/private-backups/production-188f67654d48-20261027_204720.dump.dpapi
```

Lệnh không truy cập remote. Không rollback bằng DROP/TRUNCATE hay restore đè production. Local proof ban đầu scms_phases_upgrade_20261027_204720; clone executor scms_phases_upgrade_20261027_205112_restore. Clone chứa dữ liệu production: không cho app gửi mail/chạy jobs hoặc mở cổng ra mạng. Test UpgradedSchemaValidationTest chỉ nhận URL loopback đúng namespace, ddl-auto=validate, jobs/mail mock.

```powershell
# Chạy trong checkout kiểm chứng không có .env; sau clean test, không chạy clean đồng thời.
.\mvnw.cmd '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh' '-Dtest=UpgradedSchemaValidationTest' '-Dscms.upgrade.schema-proof=true' '-Dscms.upgrade.local-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_upgrade_20261027_204720' test
```

Clean test:303run/277pass/26skip/0lỗi. Schema validation/CI và Render sau merge sẽ ghi kết quả thực tiếp theo; chưa gọi production đạt khi aggregatehealth503.

Kiểm clone Hibernate đã **PASS2/2**: toàn bộ mapping validate; fixture relatedId=3.000.000.000 giữ nguyên qua entity và response, rollback local; health có components/status và không có details. Sửa Java Notification/entity/projection/response sang Long để khớp cột BIGINT production, không sửa bundle/DDL production. Các ID phát sinh hiện tại vẫn Integer và đọc được trên schema INT cũ; JSON field giữ kiểu number. Lỗi thử trước (Docker dừng, mismatch kiểu, mock mail interface) được ghi trong schema-validation-result.json; không đổi rule/expected nghiệp vụ để làm xanh. Đang chạy lại clean test sau sửa source này.

Chốt source sau sửa Long: **clean test304run/277pass/27skip/0failure-error**, XML đối chiếu log; clone opt-in **2/2pass**. Count303 trước đó là lịch sử trước khi thêm regression int64. Các test communication6case opt-in sẽ chạy fresh+upgrade từ committed checkout; không dùng skip làm bằng chứng đã kiểm. CI/Live sau push được ghi riêng.
