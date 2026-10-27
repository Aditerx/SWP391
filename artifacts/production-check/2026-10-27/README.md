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
