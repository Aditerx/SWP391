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

## Kết quả chốt

- Source9af6b2d, merge main3cfe229 đã push. [CI branch/main](push-and-ci.json) test/package pass; backend tree merge bằng branch.
- [Clean test](clean-test-result.json):304run/277pass/27skip/0lỗi. [Clone2/2](schema-validation-result.json): mọi Hibernate mapping hợp lệ, ID thông báo3tỷ không bị cắt32bit, rollback local; health chỉ có component/status, không details. Sửa Java Notification sang Long, giữ bundle đã duyệt nguyên byte.
- [Communication fresh6/6+upgrade6/6](communication-result.json) từ committed checkout9af6b2d: HTTP/PostgreSQL/SMTP loopback, không email/cloud; container riêng đã dừng, giữ data.
- [Render sau merge](render-after-main-merge.json): bắt đầu21:27:29 UTC+7,317s sau push21:22:12. Live đúng3cfe229/target188…/9môn/OpenAPI149paths,21/22checks pass. **db UP/mail DOWN**, các component còn lại UP. [18giá đúng](production-catalog-prices.json), [OpenAPI int64/redaction](render-extra-checks.json).

Owner xác nhận **Render Free**. [Free chặn SMTP25/465/587](https://render.com/docs/free), nên SMTP587 hiện tại không gửi OTP được. Muốn giữ Free cần chọn dịch vụ email HTTPS, API key/sender và triển khai adapter OTP rồi kiểm delivery thật; code hiện vẫn JavaMailSender, chưa có adapter/provider/key được chọn. Đây là phần còn lại cần cấu hình/triển khai, không công bố production đã nghiệm thu toàn bộ. API health ping chỉ liveness, aggregate503/mailDOWN được ghi đúng.

Checker bổ sung ban đầu nhầm public catalog có giá (DTO chỉ có id/slug/name/description) được gắn [INVALIDATED](render-extra-checks-invalidated.json); giá được kiểm lại bằng JDBC read-only. GET đầu timeout đã retry thành công. Các lần fail Docker/mapping/mock mail trước cũng được ghi rõ trong schema proof, không xóa để tạo kết quả xanh.

Demo/hướng dẫn nghiệp vụ vẫn ở [phase2](../../demo/phase-2/2026-10-27/README.md) và docs02 mục10. P1.5/T/D01/D09/FE, gateway live và SQL Server runtime giữ backlog. Báo cáo đóng dùng commit `[skip render]`; Live đã kiểm vẫn3cfe229 dù main có commit tài liệu mới hơn.
