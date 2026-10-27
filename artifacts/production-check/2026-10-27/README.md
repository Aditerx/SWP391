# Kiểm production Render sau push — 27/10/2026

Đã push main/test/communication-proof `765339c781ef36bb48bfe5ea63b4f38e5121d6d6` lúc16:16:30 UTC+7. Lượt kiểm đầu bắt đầu16:21:43,đã chờ313.66giây,đúng yêu cầu>=5phút.

- [Lượt đầu](after-push.json):SwaggerHTML/assets/config200,ping200;aggregatehealth503DOWN;OpenAPI timeout/502;catalog18môn thay vì9.
- [Lượt kiểm lại](recheck.json):**17/19pass,2fail**;OpenAPI200,149paths,đủ endpoint/schema mới.AdvancedRegistration/CenterIncident/PaymentResolution/Holidaypreview đều có.Anon reports/invoices/incidents401 đúng.
- [Tóm tắt OpenAPI retry](openapi-retry.json) lưuhashresponse/schemas/paths,không giữ toàn schema công khai.
- [DB được Owner chỉ định](configured-db-comparison.json):JDBCread-only/TLS,fingerprint khớp bundle trước,9môn,0user/invoice/subscription/session,rollback hoàn tất.

**Chưa nghiệm thu production:**aggregate /actuator/health vẫn503DOWN,catalog Render trả18môn (9bản legacy nullslug +9canonical) trong khi DB.env trả9canonical.IDs cũng khác;GET thêm cache-busting vẫn18,một cacheHTTPđơn giản không giải thích chênh lệch này.Liveness/readiness riêng200;healthcomponentdetail bị ẩn nên chưa kết luậnmail/DB/thành phần nào làmDOWN. Publicactuatorinfo404,không có deploySHA để chứng nhận exactcommit quaAPI.

Đây là mismatch runtime/DB đích đã quan sát,không tự kết luận service dùng branch/credential cụ thể nào. Đã hỏi Owner xác nhậnbranchvà bộDBenv trong dashboard,không yêu cầu password. Không sửa Renderconfig,không migrate/seed/delete DB khác để làm xanh. Các thao tác production chỉGETvàJDBCread-only,không login/account/mail/payment.

Browser không kết nối trong phiên và webreader không mở đượcURL;không ghi là đã browser-render/screenshot verified. HTTPđã kiểm đúng Swagger/assets/config/OpenAPI thay vì chỉ trangHTML.

Kết quả được commit với `[skip render]` vì chỉ đóng report/audit;không thay code ứng dụng hoặc schema đã kiểm.

## Sau xác nhận main/DB của Owner — sửa lỗi CI và kiểm lại

Owner xác nhận branchmain/đãsyncDB.Tái kiểm no-cache vẫn18/health503;local.env khôngduplicateDBkeys hoặcOSoverride.Phát hiệnCI.yml0byte,GitHubrun40479537371 failure0jobs;[evidence](ci-invalid-workflow.json). Đã sửaworkflowJava17cleanverify/leastprivilege/pinnedactions,branchfix/backend-ci729d8e4 [CIpass](ci-repair-success.json),merge main8b5f5dc [mainCIpass](main-ci-success.json). Không thay src/main/schema.

Push main17:02:54 UTC+7,lượt [sau sửa CI](after-ci-repair.json) bắt đầu17:08:04 (310.68s saupush):**17/19pass**,OpenAPI149paths vàfields mới có;health503DOWN,catalog18 vẫncòn.CIinvalid là lỗiđãsửa,không đượccoi là đãgiải quyết runtime/DBmismatch.

Đã hỏiOwnerLiveSHA/latestdeploy status/HealthCheckPath;chưa có thông tin này.Phiên không cóRenderconnector/APIcredential hoặcbrowserkết nối đểtựđọc dashboard.Không hỏi lạiDBpassword/không thayenv/schema.Chưa nghiệmthu production.

Repo khai báoRenderFree/Gmail587;[RenderFree docs](https://render.com/docs/free) xác nhậnSMTP25/465/587 bịchặn trênFree.Nếu service thực tế dùngFree,SMTPcần paid instance hoặc kênh HTTPS phù hợp;chưa thay provider haykhẳngđịnh đây làhealthcomponent đanghỏng khi logschưacó.Render có thểgiữinstance cũ khi deploymới khôngquahealthcheck theo [Renderhealthdocs](https://render.com/docs/health-checks);đây vẫn làgiảthuyết chờLiveSHA/logs,không thay đổihealthcheckđểcheDOWN.
