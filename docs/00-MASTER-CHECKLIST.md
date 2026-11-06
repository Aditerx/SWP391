# SCMS — Tiến độ và nghiệm thu

## Active

**Cách mở session:** đọc AGENTS → Active → đúng Dxx trong docs04; khi sửa DB đọc [database/README](../database/README.md) và source/trigger/test liên quan. Đọc docs01 theo API, docs02 theo nhu cầu test/migration. Kết quả bên dưới luôn theo đúng mốc/source/môi trường đã ghi.

**Hướng dẫn thao tác thủ công:** áp dụng mục6 trong AGENTS: kiểm giao diện trước, chia bước cụ thể và nêu “Kết quả trả về … là đúng” sau mỗi nhóm để người dùng đối chiếu; yêu cầu ảnh khi thiếu thông tin, không suy thành công từ kết quả mong đợi. Lượt bổ sung quy tắc chỉ sửa tài liệu.

**Phạm vi đã giao:** P1.0–P1.4, BE P2 và tích hợp D01/D09 theo Owner28/10. P1.5/phần còn lại gói T/FE integration vẫn để lại. Không coi merge OTP/chat/notification hoặc D01/D09 là hoàn tất toàn bộ gói T.

**Bàn giao02/11:** Owner đã chốt20room theo room.jpg, giữ room_id số/thêm room_code và không thêm subject_id. Source room/báo vắng đã kiểm local. Owner xác nhận cả0211-1 và0212-2 hoàn tất trên Supabase, gồm7giới hạn lớp; chưa xác nhận deploy BE mới trên Render. Bản bàn giao cho thay đổi02/11: [SCMS-BAN-GIAO-FE-2026-11-02.md](../SCMS-BAN-GIAO-FE-2026-11-02.md). Giữ thiết kế center1; auth/me không thêm centerId. Hai báo cáo29–31/10 giữ vai trò bằng chứng lịch sử.

**Chẩn đoán CORS/VNPay04/11 (trước QueryDR):** CORS/cookie và redirect Pages `/member/invoices` đã kiểm HTTP. URL/HMAC lỗi cũ khớp key ảnh Render, không có căn cứ sửa encoder/IP/trim. Owner xác nhận gateway thành công nhưng invoice20/22 chưa Paid; chưa có bằng chứng IPN thành công đã commit. Menu merchant chỉ email; Owner không muốn chờ hỗ trợ. Bằng chứng gốc giữ tại `vnpayCorsDiagnosticReview` trong [QA](../artifacts/qa/latest.json).

**VNPay04/11 hiện tại:** Sau cấu hình IPN tại Terminal, Owner gửi response invoice40/INV-2026-000027: VNPay900.000đ, Paid lúc21:53:33.079965, gatewayTransactionRef15725858, reconciliationRequired=false; tạo21:52:33.401005, hết hạn21:57:33.401005. Owner thấy thành công ngay khi quay về FE, không reload. PASS phạm vi ghi nhận tiền sandbox theo response Owner; chưa đọc DB/đăng ký/lịch độc lập hoặc bắt trace callback. Kết quả phù hợp IPN sau cấu hình, không tự chứng minh QueryDR đã hết lỗi. Health mới vẫn200/build `076f04084e4865f5a7fb7a9f7f78fa79335c0500`. Bằng chứng tối thiểu không chứa PII tại `vnpaySandboxPaymentAcceptance` trong [QA](../artifacts/qa/latest.json).

**QueryDR còn mở, quyết định đã chốt:** Probe trên cùng build, log Owner20:33 xác nhận DNS VNPay→103.220.84.173 nhưng TCP443 timeout15giây; đối chứng example.com TCP/HTTPS thành công. Chưa chứng minh bên nào chặn. Owner chốt giữ nguyên code/endpoint, bảng pay attempt, migration15 và các env đã thêm để bảo toàn metadata và kênh xác minh dự phòng; không áp dụng đề xuất tắt/gỡ. Chưa có lượt QueryDR live thành công mới, chưa thêm relay hoặc đổi env. Bàn giao riêng tại [VNPay04/11](../SCMS-BAN-GIAO-FE-VNPAY-2026-11-04.md).

**OTP đăng ký04/11:** PASS ca nhận OTP thật→verify204→users42 Active→login Member theo bằng chứng Owner trên FE Pages/BE Render/Brevo/Gmail. Theo Owner, bàn giao dùng response login từ Network FE làm nguồn danh tính/quyền: userId42/memberId42/role Member/isFirstLogin=false và authorities phù hợp dashboard Member. Chưa kiểm trực tiếp email_otps.used/bảng members hay full role/owner/center. SHA runtime ca này chưa thu; nguồn đối chiếu local `3a2adaf1b6ac11bbe55cdc7e41931a4a5e31530a` không thay SHA deploy. Bàn giao tại [OTP04/11](../SCMS-BAN-GIAO-FE-OTP-2026-11-04.md); bằng chứng tối thiểu tại `otpRegistrationOwnerAcceptance` trong [QA](../artifacts/qa/latest.json).

**Access Denied/RBAC05/11 — mục0:** Owner chốt sửa GET training-results để Member Active chỉ đọc mình; đã thêm guard phiên/role hiện tại/owner và giữ quyền ghi. Rà đủ năm role cho payment-resolutions: role AND MANAGE_INVOICES, staff đúng center; Admin mặc định thiếu authority/center, không có quyền toàn hệ thống. Chỉ sửa BE/tài liệu, theo Owner không sửa FE; yêu cầu FE về request resolution ngoài quyền Member đã chuyển sang [bàn giao RBAC lần1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md), quyền/API chính thức giữ trong docs01. PASS local Java17.0.12/H2/MockMvc, fixture commit và login/auth-me thật cho integration; test đích21/21, `clean test`395run=367pass/28skip/0failure/0error trên bản sao không.env, nền `3a2adaf1b6ac11bbe55cdc7e41931a4a5e31530a` + source/test chưa commit. Fingerprint/XML và phạm vi tại `rbacIssue0` trong [QA](../artifacts/qa/latest.json); sửa assertion mới tìm CENTER_REQUIRED ở field code, không đổi expected nghiệp vụ hoặc code tiền. Chưa deploy/kiểm PostgreSQL riêng, Render hoặc FE; không gán kết quả này cho hai403 Owner trước đó. [Checklist](03-TODO.md) chốt phần BE local mục0; BE-01–BE-07 chưa rà riêng.

**Mục0.1/0.1-C — snapshot trước B/SQL16:** đã bỏ điều kiện gói chung cho Normal và chặn giá lỗi cấp Registered. Bằng chứng từng source/môi trường giữ tại normalMembershipIndependence/normalPositivePriceGuard trong QA, không suy thành Render đã deploy. Kết quả BE hiện hành và Owner chạy SQL ở đoạn B/retire bên dưới; catalog/17 vẫn cần body và trạng thái lớp để kết luận riêng.

**B/retire gói chung06/11 — OWNER XÁC NHẬN SUPABASE PASS:** B local đóng enroll/waitlist Advanced chung, mua/gia hạn/catalog/state gói cũ và tạo Member kèm packageId. BE đích/clean, SQL16 guard/purge/replay/rollback và PG workflow sau16 đạt theo `legacyMembershipRetirement` trong [QA](../artifacts/qa/latest.json); giữ nguyên bằng chứng từng snapshot tại đó. Owner gửi một dòng PASS, marker `20261106_retire_legacy_memberships_v1`, model_retired=true, mọi bộ đếm xóa/tháo liên kết0: DB không có dòng cũ cần dọn, trigger/marker đã áp dụng theo kết quả này. Agent không trực tiếp truy vấn cloud. File [SQL16](../database/migrations/16_retire_legacy_memberships_postgres.sql), SHA256 `b63c43328b1900aac8dba86f63dbbd90e4c5ff38bbb2eb7eca44b804564aaa69`; [cách chạy](02-DEPLOYMENT-GUIDE.md#retire-legacy-memberships). [LAN-1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md) liệt kê lý do bỏ/phạm vi xử lý và thực tế không xóa dòng nào. Điều kiện chờ trước A đã đạt; chưa deploy BE/FE hoặc gateway live. BE-01–BE-07 chỉ tận dụng phần bằng chứng phù hợp, catalog/17 còn mở.

**0.1-A06/11 — BE LOCAL HOÀN TẤT:** đã bỏ suy luận ghi danh Normal tạo sau Advanced là phụ thuộc; giữ eligibility thật và các guard D16/lock/retry. Kiểm HTTP có phiên và DB sau commit: Normal Pending/Paid độc lập không bị đổi, Paid/amount gốc giữ nguyên, dependency/credit mới phát sinh chặn Complete, role/center sai bị403, audit/thông báo không nhân. Test đích35/35; clean412run=384pass/28skip/0failure/0error; PG sau SQL16 đạt6/6 ca A và22/22 workflow. Java17.0.12/H2/PG17, bản sao không.env, nền `3a2adaf1b6ac11bbe55cdc7e41931a4a5e31530a` + source/test chưa commit. Source main giống nhau ở các lượt; fixture test sau clean chuyển sang môn cau-long chuẩn để qua constraint PG, đã kiểm lại đích H2/PG, không đổi expected hoặc nới DB guard. Hash/XML/phân biệt snapshot ở `cashCorrectionIndependence` trong [QA](../artifacts/qa/latest.json), raw artifacts/runs/cash-correction/2026-11-06. [LAN-1 mục5](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md) đối chiếu cả7audit: bàn giao trước phần BE-01/BE-04/BE-05, chưa đóng trọn mục. Không có SQL mới cho A, chưa deploy BE/kiểm FE/Render hoặc gateway live. Đề xuất tiếp theo để Owner duyệt: rà BE-01 riêng; catalog/17 còn cần body/type/status.

**Ghép main vào tai06/11 — KIỂM LOCAL ĐẠT:** hợp nhất tai `9a4ffb7` với main `b033ab7`, giữ nguyên guard Member Active/role hiện tại của tai tại hai conflict training-results; fixture test từ main nay gán đúng Active/Member, không đổi expected. Giữ Google login/avatar và thay đổi khác của main; loại khối lịch sử cũ bị chèn lại, gom contract Google/chat/training đúng mục, cập nhật một snapshot OpenAPI. Đích54/54; clean432run=404pass/28skip/0failure/0error; PG workflow sau Google13/QueryDR15/retire16 đạt22/22. Java17.0.12, checkout riêng không.env, PostgreSQL17 local loopback; hash/XML/giới hạn tại `rbacMainMergeIntegration` trong [QA](../artifacts/qa/latest.json), raw artifacts/runs/merge-main-rbac/2026-11-06. Chưa nghiệm thu Google thật/FE/Render hoặc thực thi migration Google trên Supabase. Entity User cần google_sub trước deploy; [hướng dẫn](02-DEPLOYMENT-GUIDE.md#google-login). Kiểm này không đóng BE-01–BE-07; trạng thái push/merge thực tế truy [PR18](https://github.com/Aditerx/SWP391/pull/18), không suy deploy từ merge.

**Chuẩn bị Chat AI local07/11:** Trên nhánh `feature/ai-chatbot-env`, baseline trước sửa397 tests/369 pass/28 skip/0 lỗi; sau sửa402 tests/374 pass/28 skip/0 lỗi, `clean test` BUILD SUCCESS. Source tree chưa commit, dựa trên HEAD `b033ab7719bb124ac4fce9c1e0f6712e05c4e5c9`. PostgreSQL17.11 local service đang chạy nhưng chưa tạo DB; local connection yêu cầu mật khẩu chưa được cung cấp. FE hiện hành chưa xác định; không lấy `FE_Project-main` làm nguồn. Chưa chạy backend/FE local hoặc Anthropic thật. Bàn giao một luồng tại [AI Chat07/11](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-07.md).

| Việc còn mở | Trạng thái | Điều kiện tiếp tục |
---

## 📜 PHẦN II: LỊCH SỬ HOÀN THÀNH (HISTORY)

### [07/11/2026] Chuẩn bị khung môi trường Chat AI local

- Tạo nhánh `feature/ai-chatbot-env` từ HEAD `b033ab7719bb124ac4fce9c1e0f6712e05c4e5c9`. Thêm `AiCompletionClient`, fixed mock, Anthropic Messages adapter dùng Spring `RestClient`, config timeout/model/token/system prompt, giới hạn và bỏ tin có dấu hiệu nhạy cảm; lỗi/không có responder chuyển STAFF. Không gọi provider thật.
- Giữ feature flag mặc định false và handler mới STAFF cho tới khi Owner chốt; thêm Member handoff `POST /api/chat/conversations/{id}/escalate`. Tái dùng metadata/chat schema, không migration/không dùng `ai_consultations`; cập nhật API/config/handoff ngày07/11.
- Baseline `clean test`: 397 tests,369 pass,28 skip,0 fail/error. Sau thay đổi `clean test`: 402 tests,374 pass,28 skip,0 fail/error; BUILD SUCCESS. Test unit/API không là nghiệm thu PostgreSQL, browser hoặc provider.
- Theo D11/D19: PostgreSQL17.11 local service và port5432 hoạt động, nhưng local role cần password; không kết nối DB, không đọc `.env`, không tạo DB/schema. Chưa xác định source FE hiện hành; backend health, auth `/api/auth/me`, polling, FE browser và Anthropic đều NOT_RUN. Chi tiết/việc tiếp tục tại [handoff AI Chat](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-07.md).

### [04/11/2026] Sửa quyền Member đọc kết quả và đánh giá tập luyện

- Mở `GET /api/training-results` cho MEMBER; service bắt buộc scope theo User lấy từ Authentication, tự áp memberId khi thiếu và từ chối ID khác. Member được xem toàn bộ `content` và `comment` theo Owner xác nhận.
- Thêm service test cho scope/IDOR và HTTP authorization test cho Member, anonymous, Coach, Manager. `.\mvnw.cmd clean test`: 289 tests, 270 pass, 19 skipped, 0 failures/errors; BUILD SUCCESS.
- Cập nhật API contract mục 17. Chưa xác minh source FE hiện hành, bản deploy/SHA Render hoặc trang “Chỉ số cơ thể”; không có xác nhận API riêng cho body metrics trong checkout.

### [27/10/2026] Xử lý conflict nhánh SMTP/OTP, chatbox và thông báo

- Owner làm rõ phạm vi đồng đội là SMTP/OTP đăng ký, chatbox cơ bản và thông báo; D01/D09 vừa phân chưa làm. Giữ backlog/hai phase, tiếp tục session sau theo docs 04 mục 40.12.
- Ghép 8 file conflict của merge `fc00c64` + `3309b02`; giữ Clock, locking, kiểm quyền, lifecycle Completed và toggle chế tài. Sửa import ZoneId và Clock trùng do auto-merge, đồng bộ hai schema PostgreSQL ở mức file và giữ API contract cũ + phần OTP/notification/chat mới.
- Chạy `.\mvnw.cmd clean test`: 269 run, 250 pass, 19 skip, 0 failure/error, BUILD SUCCESS. Không đổi expected để làm xanh. Kết quả nhánh đồng đội 193/198/224 phía dưới là lịch sử, không phải kết quả tích hợp này.
- Chưa SMTP delivery thật, migration 05 notification/06 chat trên PostgreSQL, FE browser, VNPay live hoặc deploy. Không sửa .env/old-docs hoặc chạy mutation DB cloud; bằng chứng tại artifacts/merge-2026-10-27 và docs 02 mục 9.14.

### [27/10/2026] Kết nối Supabase mới thành công sau chuẩn hóa JDBC

- Owner chọn dùng DB_PASSWORD hiện tại. Chỉ chuẩn hóa DB_URL thành JDBC tới Session pooler, bỏ credential nhúng; giữ nguyên DB_USERNAME/DB_PASSWORD, .env ignored/không track.
- Driver JDBC 42.7.4 kết nối thành công, sslmode=require, SELECT 1=1, PostgreSQL 17.11, transaction_read_only=on và rollback hoàn tất. Public có 0 bảng; đây là kiểm kết nối/metadata, chưa phải schema SCMS hoặc nghiệm thu API/FE.
- Không boot Spring/jobs, migration/seed hoặc ghi dữ liệu nghiệp vụ. Giữ các lỗi thử trước làm lịch sử, kết quả hiện tại tại docs 02 mục 9.13 và artifacts/review-2026-10-27/supabase-connection-normalized.json. Baseline tests vẫn đúng phạm vi cũ, không thành bằng chứng feature mới.

### [27/10/2026] Đã push baseline, kiểm Supabase sau push chưa thành công

- Origin/tai đã nhận `9097197` (F-06/F-07 d210f50, archive/source c7ad9f6, docs/checklist/evidence 9097197), remote SHA khớp local. .env không track; các file Owner chuyển archive giữ nguyên nội dung. Không triển khai core mới/Render.
- Đã thử JDBC không Spring/jobs trên .env mới: pooler XX000 tenant/user not found; direct theo ảnh 08001/UnknownHostException, Windows DNS có AAAA. Không SQL hoặc thay .env/DB; chưa chứng nhận TLS/schema/flow thành công. Đã hỏi Owner đúng Session pooler để cập nhật host/username, không yêu cầu password.
- Ghi thất bại/pending và checker/JSON đã bỏ secret trong artifacts/review-2026-10-27 và docs 02 mục 9.11. Cập nhật kết quả này bằng commit tài liệu tiếp theo; kết quả local tests vẫn giữ đúng phạm vi, không fake pass DB mới.

### [27/10/2026] Rà checklist đồng đội, code hiện có và chạy kiểm thật trước push

- Owner yêu cầu review/commit/push tai và thử DB mới sau push. Bổ sung AGENTS checkpoint rule mới và 04 mục 40.8–40.10: checklist làm việc từng bước, điều tránh, acceptance và phạm vi kết nối chỉ đọc. Chưa triển khai D01/D09/Phase 1/2.
- Maven clean test đạt 223 tests/204 pass/19 skip/0 failure-error; PG 17.11 container mới hai DB tách biệt đạt 14 tests ứng dụng và các guard migration/snapshot. Giữ/dừng container test review, không thay DB/container cũ hoặc .env cloud. Script thêm lựa chọn container test tên riêng.
- Artifact review tại `artifacts/review-2026-10-27/`, docs 02 mục 9.10; .env ignored, move old-docs giữ nội dung và schema copies đồng nhất. Commit/push/kết nối thực sẽ ghi kết quả sau khi hoàn thành, không dùng kiểm baseline làm bằng chứng rule mới.

### [27/10/2026] Đề xuất chia ba phần, hai phase chính và chuẩn demo

- Owner yêu cầu gói đồng đội 10–20% ít conflict, hai phase còn lại tuần tự/kiểm liên tục/demo thật. Đối chiếu source User/Member đã đủ trường D01 và D09 chỉ đọc: đề xuất T gồm D01/D09 ~15%, Phase 1 mua/quyền ~40%, Phase 2 sử dụng/ngoại lệ/report ~45%; chưa là estimate ngày công hoặc coding authorization.
- Ghi ownership/mốc dirty tree/nhánh riêng/commit nhỏ, central docs và migration một chủ, tiêu chí bàn giao T và prompt copy tại 04 mục 40.2–40.3/40.7. P1/P2 nhóm theo phụ thuộc, không dồn tests đến cuối hoặc để report hoàn tiền làm trước model nguồn.
- Chuẩn evidence 40.6/02 mục 9.9 phân biệt local callback/test/HTTP với Sandbox live/deploy/FE thật; không SQL Paid/Active/mock/frontend cũ thay demo. Chưa tạo artifact demo, Maven/PG/setup/DB/email/deploy; chỉ docs/đọc source, giữ thay đổi workspace/old-docs.

### [27/10/2026] Chốt ba chỉ số tiền và quyền quyết định UI của FE

- Owner chọn BE trả đủ tiền đã thu/đã hoàn/thu ròng, FE quyết định chỉ số chính/bố cục/hiển thị cả ba; bỏ yêu cầu ưu tiên thu ròng. Quyết định 04 mục 39.3, đồng bộ docs 01/02.
- Vòng phân tích đã đi qua D09–D19, cập nhật checkpoint 04 mục 0 và làm gọn active checklist thành công việc chuẩn bị triển khai/nghiệm thu. Không đánh dấu feature đã code/test hoặc mọi chi tiết nghiệp vụ đã hết khả năng phát hiện mâu thuẫn.
- Chỉ docs, giữ thay đổi workspace/old-docs; chưa BE/test/Maven/migration/mutation DB/email/deploy.

### [27/10/2026] D18 chốt 1; D19 kế hoạch kiểm và rà liên kết báo cáo tiền

- Owner chọn quyền dashboard/report hướng 1, ghi 04 mục 37.3: CenterManager/Admin có quyền report, Coach dữ liệu nghiệp vụ được giao; không MANAGE_CLASSES đơn lẻ đọc tổng tài chính. Chưa sửa annotation/quyền DB.
- D19 gộp kế hoạch test/fixture/source FE/SHA tại 38 theo D11 và AGENTS, không phát sinh lựa chọn mới; chưa chạy baseline/Maven. Test cũ khác policy phải giải trình cập nhật, không đổi expected để xanh.
- Rà phụ thuộc D10/D16 với ReportService tổng Paid, cần chọn chỉ số tiền thực nhận/hoàn/ròng tại 39.2 trước sửa report. Đồng bộ docs 01/02; chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [26/10/2026] D16 chốt 2 hẹp; D17 handoff; mở quyền report D18

- Owner chọn 2 phạm vi đã giải thích: Cash xác nhận nhầm/chưa nhận tiền, Advanced chưa dùng/booking/phụ thuộc, CenterManager duyệt; không Normal. Quyết định 04 mục 35.4, chưa API/migration.
- D17 đối chiếu payload/expiresInSeconds 1–30, GET có thể ghi secret lần đầu; handoff FE đúng bản, không QR giả/TTL hằng và scan trong giờ buổi. Chưa FE/probe verified.
- Rà ReportController/Service quyền rộng/tổng tài chính, Coach 200 phù hợp authority cũ. Chờ policy/UX ở 37.2. Đồng bộ docs 01/02; chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [26/10/2026] D16 đánh giá hướng 2, chưa chốt

- Owner hỏi chi phí, chưa chọn. Đối chiếu liên kết subscription/session/enrollment và guard thanh toán tại 04 mục 35.3: cần hồ sơ điều chỉnh/transaction/audit/report/tests, không chỉ nút hay kiểm credit=12.
- Đề xuất thu hẹp vào Cash xác nhận nhầm/chưa thực nhận, Advanced chưa sử dụng/phụ thuộc; Normal đụng quy tắc không hủy nên chưa tự mở ngoại lệ. Đồng bộ docs 01/02, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D15 chốt bù sự cố báo muộn; mở D16 Cash

- Owner chọn hướng 1 tại 34.3: xác minh/hồ sơ riêng, Advanced hoàn đúng một credit, Normal bù đủ 12, giữ lịch sử và chống bù trùng; không sửa thành Present hoặc dời quá khứ.
- Rà confirmCash/InvoiceResponse đã có actor/time/amount/audit/lock, chưa note/receipt/correction. Gộp ba mức phạm vi thu/biên nhận/sửa sai tại 04 mục 35.2; đề xuất core+hồ sơ chưa tự đảo, nêu rõ giới hạn và phân biệt chưa thu thật với hoàn thật. Chờ chọn, chưa mở quyền hoặc đảo giao dịch.
- Đồng bộ docs 01/02; chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D15 chốt Present/Absent, mở sự cố báo sau buổi

- Owner chọn cấu hình 1 ở 33.4, ghi quyết định 33.5: giờ buổi/Present-Absent, bỏ Late và scan sớm 30 phút cho attendance mới, giữ dữ liệu cũ; CenterManager sửa sai sau buổi, check-in cửa ưu tiên nâng cấp sau.
- Đối chiếu SessionService guard lịch đã diễn ra và D08. Tách sự cố thật không tổ chức khỏi sửa nhầm attendance/no-show; đề xuất hồ sơ bù có kiểm soát hoặc chỉ ghi nhận MVP ở 04 mục 34.2, chờ chọn. Chưa bù/hoàn/đổi lịch sử.
- Đồng bộ docs 01/02; chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D15 chốt phạm vi MVP, rà lại Late và 30–10 phút

- Owner chọn attendance MVP/quyền sửa hướng 1, ưu tiên nâng cấp check-in cửa hướng 2. Late/mốc 30/10 chưa chốt; quyết định 04 mục 33.3, giữ code/data vào-ra cũ.
- Rà TrainingResultService xem Late như Present, chưa thấy tác động credit riêng; đề xuất cấu hình đơn giản theo giờ buổi/Present-Absent, so với scan sớm hoặc giữ Late tại 33.4. Chờ chọn, không xóa Late lịch sử/thêm chế tài đi muộn.
- Đồng bộ docs 01/02; chỉ source/docs, chưa code/tests/Maven/DB/deploy.

### [25/10/2026] D14 chốt hướng 1; mở D15 attendance/check-in

- Owner chọn báo ảnh hưởng/xử lý lịch riêng rồi kiểm lại và xác nhận Holiday tại 04 mục 32.3. Giữ D12/lịch sử/Normal đủ 12, chưa API/code/migration.
- Rà AttendanceService/Controller/Finalization: check-in cửa session=null khác scan theo buổi; scan 30 phút trước–kết thúc/Late sau 10 phút, correction chưa giới hạn finalized. Gộp ba hướng phạm vi và quyền sửa tại 33.2; đề xuất attendance ưu tiên/cửa để sau/CenterManager sửa sau buổi. Sự cố thực báo muộn khác sửa nhầm kết quả, chưa cho hoàn/bù từ correction.
- Đồng bộ docs 01/02, chỉ source/docs; chưa code/tests/Maven/DB/deploy.

### [25/10/2026] D13 chốt cả hai/UI theo người; mở D14 workflow Holiday

- Owner chọn 3, UI/UX ưu tiên 1: chỉ số theo người làm chính, đăng ký hết hạn bổ sung; giữ định nghĩa quyền học 31.2, quyết định tại 31.3. Chưa thêm field/API/report code.
- Rà HolidayController/Service chỉ có create/toggle/read, thiếu lịch sử/preview/update/delete. Gộp ba hướng xử lý lịch ảnh hưởng tại 04 mục 32.2, đề xuất báo tác động/xử lý riêng rồi xác nhận, chờ Owner. Giữ D12 và không mở hủy Normal/attendance lịch sử.
- Đồng bộ docs 01/02, chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [25/10/2026] Chốt biên D12; phân tích chỉ số D13

- Owner đồng ý bộ 30.4, ghi quyết định 30.5: giữ skip thực tế quá khứ, tính lại tương lai và CenterManager xử lý ca vượt hạn trước xác nhận thay đổi. Liên kết D14, không tự hủy lịch hoặc rút quyền lịch sử.
- Rà ReportService/MemberReportResponse: expiredMembers hiện là phần bù người có subscription Active, không xét đủ Normal/credit/ca đã đặt và tính cả Member chưa mua. Gộp đề xuất chỉ người/chỉ đăng ký/cả hai cùng định nghĩa quyền học ở 04 mục 31.2; chờ chọn, chưa sửa report.
- Đồng bộ docs 01/02, chỉ source/docs; chưa code/test/Maven/DB/deploy.

### [25/10/2026] D12 đổi hạn Advanced theo ngày TT hoạt động

- Owner chọn 45 ngày sử dụng: TT nghỉ/đóng thì skip, thêm/tắt ngày nghỉ cập nhật hạn gói đang dùng, không cộng bù hai lần. Quyết định mới 04 mục 30.3 thay phần 1080 giờ liên tục D03-c; giữ Paid làm mốc bắt đầu và holding Invoice 5 phút. Đã cập nhật checkpoint/cảnh báo kế hoạch docs 01/02.
- Tách biên tác động có liên quan thành một đề xuất 30.4: giữ thời gian thực sự đã nghỉ quá khứ, tính lại phần tương lai, kiểm lịch đã đặt trước khi rút hạn. Chờ xác nhận biên này, không hỏi lại hướng skip đã chốt.
- Chỉ docs/plan, chưa BE/test/Maven/DB/deploy; chưa migration/backfill hoặc sửa Holiday thật.

### [25/10/2026] D11 chốt hướng 1; mở D12 ca nắng/ngày nghỉ

- Owner chốt local PostgreSQL riêng rồi smoke deploy có kiểm soát tại 04 mục 29.3; FE dùng source hiện hành, không thay .env hoặc mặc định staging riêng. Lịch sử đã có local H2/PG và Render, không coi trước đây toàn dùng hướng 3.
- Đối chiếu AGENTS/TimeSlot/HolidayRepository/SessionService, gộp đề xuất D12 tại 30.2: giữ ca, cả ba tag active, Normal đủ 12, Advanced không tạo/trừ credit ngày nghỉ; hạn 1080 giờ giữ quyết định D03. Dừng hỏi giờ PDF/nghĩa “không trừ”, không mở lại hạn gói hoặc tự xử lý lịch cũ thuộc D14.
- Đồng bộ docs 01/02; chỉ source/docs, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D10 chốt hoàn ngoài SCMS cho MVP; mở D11

- Owner chọn hướng 1 tại 04 mục 28.5: CenterManager duyệt hoàn đủ ngoài hệ thống, SCMS lưu bằng chứng/kết quả, không cấp lại quyền; refund API và cấp quyền lại để nâng cấp sau. Hồ sơ hoàn tối thiểu vẫn trong phạm vi MVP, chưa code/API/migration.
- Gộp D11 thành chiến lược local DB riêng + smoke deploy, staging BE/DB riêng hoặc deploy hiện có với fixture giới hạn; đề xuất hướng 1 theo quy mô project, chờ chọn ở 04 mục 29.2. Không tự coi chọn môi trường là cho phép mutation/deploy trong giai đoạn plan.
- Cập nhật checkpoint D10 và docs 01/02; chỉ docs/read-only, chưa code/test/Maven/DB/email/deploy. Giữ thay đổi workspace/old-docs.

### [25/10/2026] Cân nhắc lại đối soát theo ưu tiên đơn giản, gộp lựa chọn phụ thuộc

- Owner chưa chọn A/B; yêu cầu làm rõ chi phí cấp quyền và tránh chia câu hỏi quá nhỏ. Ghi 04 mục 28.3–28.4: cấp quyền cần kiểm gốc/Normal/Advanced/hạn/credit, transaction/idempotency và loại trừ hoàn; nhiều phụ thuộc D03 chưa triển khai.
- Điều chỉnh khuyến nghị theo ưu tiên project sang refund-only ngoài hệ thống có bằng chứng; so với refund API hoặc thêm cấp quyền/hoàn. Chờ chọn 1/2/3, không tự chốt thay Owner. Đồng bộ docs 01/02, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D10-e2b tách lựa chọn kết quả đối soát

- Rà source/đối chiếu e1/e2a, D03 hạn gói và D10-b. Tách khoản Paid cần xử lý khỏi hóa đơn thiếu dữ liệu chưa thu; không thu lại, sửa amount hoặc reset hạn từ giờ duyệt.
- Đề xuất CenterManager thỏa thuận Member rồi cấp đúng quyền nếu đủ điều kiện, nếu không thì hoàn đủ; so với phương án chỉ hoàn đủ rồi đăng ký lại. Chờ chọn ở 04 mục 28.2; cách hoàn tiền để bước e2c riêng. Đồng bộ kế hoạch docs 01/02, chưa code/test/Maven/DB/email/deploy.

### [25/10/2026] D10-e2a chốt CenterManager duyệt, làm rõ thuật ngữ center

- Owner chọn A; dùng đúng tên role CenterManager, Receptionist hỗ trợ. Giải thích center là trường/phạm vi dữ liệu User.centerId và CenterContext hiện có, không phải role mới hoặc chốt mô hình nhiều chi nhánh. Chi tiết 04 mục 27.3, đồng bộ docs 01/02.
- Chưa chốt cách cấp quyền/hoàn tiền; chỉ docs, chưa code/test/Maven/DB/deploy.

### [25/10/2026] D10-e1 chốt A; mở trách nhiệm đối soát D10-e2a

- Owner chốt A sau cân nhắc code/nghiệp vụ: quá hạn tại lúc BE xử lý thì ghi tiền/cần đối soát, không tự kích hoạt, không quy lỗi khách trả trễ. Quyết định tại 04 mục 26.4; đồng bộ contract/rollout, chưa sửa BE.
- Rà source chưa có workflow hoàn tất đối soát; tách người duyệt khỏi phương án hoàn tiền/cấp quyền và kỹ thuật thực hiện. Đề xuất Manager cùng center duyệt, Receptionist hỗ trợ; dừng hỏi tại 04 mục 27.2.
- Chỉ đọc source/sửa docs 00/01/02/04; chưa BE/test/Maven/mutation DB/email/deploy. Giữ các thay đổi workspace/old-docs.

### [25/10/2026] D10-d2 chốt A và yêu cầu mở rộng email; mở D10-e1

- Owner chọn tự refresh hữu hạn rồi Kiểm tra lại, yêu cầu dễ kết hợp email Member sau này. Ghi 04 mục 25.3: nguồn kết quả BE, phát thông báo sau commit, chống trùng, email lỗi không đảo Paid; chưa bật gửi mail/outbox hoặc sửa code.
- Rà source có EmailService xác thực và NotificationService REQUIRES_NEW, chưa có pipeline thanh toán sau commit. Tách yêu cầu email khỏi polling/Return và ghi giới hạn đảm bảo gửi/retry khi triển khai.
- D10-e1 phân biệt trả tiền trong hạn nhưng IPN muộn với trả thực sự muộn; source xét giờ xử lý BE. Đề xuất A bảo thủ/đối soát hoặc B xét timestamp gateway và tài nguyên còn hợp lệ, dừng hỏi ở 04 mục 26.2. Chỉ source/docs, chưa tests/Maven/DB/mail/deploy.

### [25/10/2026] D10-d1 chốt A; phân tích màn chờ kết quả

- Owner chốt IPN cập nhật, Return hiển thị/điều hướng; giữ kiểm chữ ký/merchant/amount/locking/idempotency, không đổi luồng Cash. Quyết định và test mục tiêu tại 04 mục 24.3, handoff ở docs 01 mục 14.6.
- D10-d2 đối chiếu API own Invoice hiện có và mã redirect không đồng nghĩa Paid. Đề xuất tự refresh hữu hạn rồi cho kiểm tra lại, hoặc chỉ thủ công; dừng hỏi ở 04 mục 25.2. Không tự coi hết thời gian chờ là thất bại hoặc Paid cần đối soát là quyền học đã kích hoạt.
- Chỉ đọc source/cập nhật docs; chưa sửa code/config/test, Maven, mutation DB hoặc deploy. Giữ thay đổi workspace và old-docs.

### [25/10/2026] Xác định FE tham khảo cũ; phân tích vai trò Return/IPN

- Owner xác nhận FE_Project-main out-date nặng; không lấy mapper/test đó làm bằng chứng FE hiện hành. D10-c chuyển thành yêu cầu handoff ở 04 mục 23.3/API contract 14.5.
- Đính chính D10-b: null deadline vượt guard nhưng gateway đọc atZone sẽ lỗi, chưa lấy được URL; phương án A đã chốt giữ nguyên. D10-d1 phát hiện cả Return/IPN gọi chung service ghi DB, đối chiếu FAQ/hướng dẫn PAY chính thức; đề xuất IPN cập nhật/Return hiển thị, dừng hỏi ở 04 mục 24.2. Contract redirect status tách bước sau.
- Chỉ đọc BE/tra tài liệu/cập nhật docs 00/01/02/04; chưa sửa code/test/config, Maven, DB hoặc deploy.

### [25/10/2026] D10-b chốt A; D10-c kiểm nguồn FE

- Owner chọn A: chặn online khi thiếu method/deadline, giữ chứng từ/amount và đối soát trước xử lý đơn cũ/tạo đơn mới; không mở lại hóa đơn thiếu dữ liệu. Ghi 04 mục 22.3, đồng bộ trạng thái ở docs 01/02.
- Mapper FE local mặc định method null thành cash (vẫn giữ rawPaymentMethod); chưa xác nhận source FE hiện hành/màn Pay dùng trường nào. Dừng hỏi nguồn tại 04 mục 23.2; chưa kết luận lỗi live hoặc sửa FE. Chưa BE/test/Maven/DB/deploy.

### [25/10/2026] D10-a chốt 5 phút; phân tích D10-b Invoice 3

- Owner đồng ý holding 5 phút; đồng bộ ghi chú cũ 5/15 ở docs 01/02, quyết định tại docs 04 mục 21.3. Chưa sửa key/config/env hoặc deadline cũ.
- Đối chiếu pay-online: Pending chưa đủ; method null trong snapshot Invoice 3 phù hợp nhánh 409 nếu live cùng source. Null deadline không bị guard hiện tại coi expired; không tự sửa method/hạn để ép test. newInvoice hiện tạo đủ method/deadline, chưa xác nhận nguồn gốc bản ghi cũ.
- Đề xuất D10-b A chặn hóa đơn thiếu dữ liệu/đối soát trước đơn mới, B workflow khôi phục có kiểm soát; dừng hỏi ở 04 mục 22.2. Chỉ source/docs, chưa BE/tests/Maven/DB/deploy.

### [25/10/2026] Rà D09 và mở phân tích D10-a

- Đọc lại D09, giữ phương án đã ghi; chưa có thay đổi BE/test. Bóc tách thứ tự D10 tại docs 04 mục 21.1.
- Phát hiện chuẩn 5 phút ở AGENTS/checkpoint khác ghi chú 5/15 chưa chốt ở docs 01/02; source dùng payment.expire-minutes với env override, khác tên key trong AGENTS. Đề xuất giữ 5 phút nhưng dừng hỏi trước khi tiếp tục theo yêu cầu Owner; chưa đổi policy/env/Invoice.
- Cập nhật tham chiếu trạng thái chờ ở docs 01/02/04; chỉ phân tích/docs, không Maven/mutation DB/deploy. Chưa đánh dấu D09/D10 đã sửa.

### [25/10/2026] D09 hoàn tất bước đọc source và chuẩn bị sửa

- Kiểm status/diff trên tai/8e95759, giữ toàn bộ thay đổi/untracked và old-docs. Xác định đường controller → assignment repository → specialization.getName ngoài transaction; query user/role đã fetch, chuyên môn đang N+1 và được đọc trước lọc center. Log 20.5 đủ xác nhận loại lỗi; không yêu cầu gửi lại.
- Ghi kế hoạch tái hiện OSIV=false/DB test cô lập/không transaction bao request, service read-only transaction + lọc center/batch fetch và ma trận regression tại docs 04 mục 20.6. Chưa sửa BE hoặc chạy tái hiện/Maven.
- docs 01 mục 4.5 sửa mô tả quyền theo source (Authenticated), bổ sung semantics hiện tại; 14.3 ghi handoff FE dự kiến. docs 02 mục 9.6 ghi trình tự và bằng chứng còn thiếu. D09 chưa fix/deploy/FE verified; không mutation DB hoặc thay quyết định D01–D08.

### [25/10/2026] Chuẩn bị session mới, xác lập lại vai trò BE và rà docs 01/02/04

- Chúng ta là BE thực hiện code/test/deploy theo phạm vi Owner yêu cầu; “handoff BE” cũ là ghi chú nội bộ, không chờ BE khác. Thêm checkpoint/context copy ở đầu docs 04 mục 0: quyết định D01–D08, D09/log, phần còn mở, workspace/giới hạn và thứ tự đọc.
- docs 01 mục 14 bổ sung D01/D02/D09, mục 14.1–14.2 quy định trạng thái và gói bàn giao FE. Sửa cảnh báo D04 cũ còn ghi chờ chọn; đánh dấu các ví dụ 24 giờ/Member tự book/công thức ngày/sự cố chưa phản ánh policy mới. Không phát hành API dự kiến như đã chạy.
- docs 02 mục 9 bổ sung preflight/migration/phiên bản/kiểm thử/deploy/handoff và kế hoạch khôi phục; đánh dấu smoke 4.3 là implementation cũ, sửa GET gói cá nhân không tồn tại thành route đúng theo source. Nghiệp vụ mới chưa có migration/code chỉ vì docs đã ghi.
- Checkpoint branch tai/HEAD 8e95759 là local, không xác nhận SHA Render. Giữ thay đổi/untracked/old-docs; D09 chưa code, test cũ không dùng làm bằng chứng mới. Đợt này chỉ docs/read-only, kiểm diff whitespace; không Maven/mutation/deploy.

### [25/10/2026] D09 nhận được root error lazy Specialization, đã lọc log trùng

- Owner gửi ERROR/exception đầu; lọc thành ba lần 24/10 lúc 08:51:20, 08:54:15, 08:54:16 UTC, cùng LazyInitializationException Specialization#5/no Session. Không cần thêm Caused by để nhận diện loại lỗi.
- Log xác nhận lazy loading trên Render; source CoachController phù hợp nhưng đoạn chọn lọc thiếu URI/frame ứng dụng để gắn chính xác endpoint/line. Ghi bằng chứng và handoff BE ở 04 mục 20.5, giữ kiểm endpoint bằng full stack/tái hiện lúc triển khai.
- Không nhầm Hibernate Session với bảng ca học hoặc suy Specialization#5 bị thiếu. Chưa sửa backend/DB, chưa chạy tests/probe mới; D09 chưa được đánh dấu đã fix.

### [25/10/2026] D08-d chốt chờ khắc phục; D09 rà lỗi danh sách Coach

- Owner đồng ý lưu chờ khi sự cố chưa rõ giờ kết thúc; có giờ khôi phục thực tế mới tính/duyệt bù, không gia hạn tạm/vô hạn. Expiry trong lúc chờ phục hồi theo D08-c sau duyệt hạn hợp lệ. Handoff/nghiệm thu 04 mục 19.9/API contract mục 14; D08-a/b/c/d đã chốt, chưa triển khai.
- Rà source D09 thấy OSIV=false, controller ánh xạ specialization LAZY ngoài transaction và query theo từng Coach; giả thuyết lazy loading/N+1 chưa là root cause live. Ghi chứng cứ và kế hoạch kiểm ở 04 mục 20; cần stack trace /api/coaches đúng ngày/request và SHA deploy, không dùng log Session cũ.
- Chỉ docs/đọc source, chưa sửa backend/schema/DB hoặc chạy tests/probe live mới. Sự cố báo sau buổi đã diễn ra/attendance chốt vẫn để D15, không tự mở sửa lịch sử.

### [25/10/2026] D08-c chốt phục hồi credit có đối soát

- Owner đồng ý Manager phục hồi đúng credit bị expiry thu hồi khi sự cố phát sinh lúc gói còn hạn và hạn bù mới hợp lệ. Job lưu số dư thu hồi trước khi về 0, đối soát sử dụng/hoàn/phục hồi, không nạp lại 12 hoặc mở mọi gói cũ. Handoff BE/FE/nghiệm thu 04 mục 19.7 và API contract mục 14.
- D08-d đề xuất MVP lưu sự cố “Chờ khắc phục”, khi biết giờ khôi phục thực tế mới tính/duyệt bù; nếu expiry trong lúc chờ thì phục hồi sau duyệt theo D08-c, không đặt tiếp khi chưa có hạn hợp lệ. Dừng hỏi trước chọn gia hạn tạm hoặc luồng khác.
- Chỉ docs, chưa sửa backend/schema/DB hoặc chạy tests ứng dụng.

### [25/10/2026] D08-b chốt công thức bù và UX đơn giản

- Owner đồng ý bù theo gián đoạn thực tế, không trùng, Manager duyệt thêm để đủ ca bù nếu cần; yêu cầu đơn giản. Ghi đặc tả/handoff BE–FE/nghiệm thu tại 04 mục 19.5, API contract mục 14.
- UI một luồng trong màn sự cố: khoảng ảnh hưởng, thời lượng/hạn đề xuất, duyệt; BE kiểm/chống cộng trùng và audit phía sau. Không thêm quyền bù riêng/workflow Member duyệt hoặc nạp lại 12 credit/1080 giờ.
- D08-c còn thiếu chính sách phục hồi số dư khi job đã đưa credit về 0: đề xuất chỉ phục hồi đúng số dư có căn cứ của sự cố phát sinh lúc gói còn hạn, deadline bù đã được duyệt còn hiệu lực; dừng hỏi trước mở quyền. Chỉ docs, chưa code/DB/tests ứng dụng.

### [25/10/2026] D08-a chọn B gia hạn toàn gói; đề xuất cách tính bù theo ảnh hưởng

- Owner chọn B, các credit còn lại cùng hưởng hạn mới theo số ngày/giờ bị ảnh hưởng; không chọn quyền bù riêng A, không cấp lại 1080 giờ/12 credit. Lưu 04 mục 19.3/API contract mục 14.
- D08-b phân biệt gián đoạn thực tế với độ trễ ca bù. Đề xuất tính thời lượng gián đoạn không trùng, cộng đúng giờ/phút, Manager duyệt thêm nếu cần đủ ca bù thực tế; dừng hỏi trước chốt công thức. Ghi rủi ro sự cố chưa có giờ kết thúc, expiry khi chờ duyệt và credit đã bị cron xóa.
- Chỉ docs/đối chiếu source đã đọc, không code/DB/tests ứng dụng; không tự áp mức bù hoặc gia hạn.

### [25/10/2026] D07-c chốt bỏ dấu đúng thao tác Dời ca; chuyển D08-a

- Owner chốt chỉ bỏ dấu báo vắng khi “Dời ca” thành công và thực sự đổi ngày/giờ; đổi sân/Coach cùng giờ/no-op/preview/thất bại giữ dấu, lưu lịch sử phía hệ thống. Handoff và biên race/retry ở 04 mục 18.4/API contract mục 14.
- D08-a đối chiếu I.5 với source: mục tiêu không kéo hạn credit khác nhưng đề xuất gia hạn toàn gói; source đang tự kéo endDate chung, job expiry xóa số dư. Đề xuất quyền bù riêng đúng buổi bị ảnh hưởng, nhiều code hơn nhưng không mở lại quyền khác; dừng hỏi A/B trước thiết kế hạn bù.
- Chỉ cập nhật docs/đọc source, chưa thay code/DB/quyền hoặc chạy tests ứng dụng.

### [25/10/2026] Dừng làm rõ D07-b và UX dấu báo vắng D07-c

- Owner yêu cầu giải thích lại hai mốc thời gian: ca cũ chưa bắt đầu; ca mới cách lúc Manager xử lý >=12 giờ và kết thúc trong hạn. Giữ phân biệt với ngoại lệ lỗi trung tâm, chưa tự đổi D07-b.
- Ghi nhận mong muốn UX bỏ dấu báo vắng trên lịch mới, thể hiện như buổi mới; làm rõ đề xuất trước đã bỏ dấu khi đổi ngày/giờ, chỉ lưu audit phía hệ thống. Không thêm credit/đổi hạn hoặc bắt Member thao tác lịch sử; không thêm hộp thoại giữ/bỏ dấu khi dời ngày/giờ.
- Còn hỏi phạm vi chỉ đổi sân/Coach cùng giờ có bỏ dấu không; chưa chuyển D08. Chi tiết 04 mục 18.3/API contract mục 14. Chỉ docs, chưa code/DB/tests ứng dụng.

### [25/10/2026] D07-b chốt A; tách dấu báo vắng sau dời D07-c

- Owner chọn A: chỉ Manager duyệt Member bất khả kháng khi ca cũ còn dưới 12 giờ/chưa bắt đầu, ca mới vẫn >=12 giờ và trong hạn; lý do/xác nhận/audit, giữ credit/hạn. Handoff BE/FE và kiểm thử ở 04 mục 17.3/API contract mục 14.
- D07-c cần chốt dấu báo vắng khi Session giữ ID nhưng đổi lịch: đề xuất đổi ngày/giờ thì lưu dấu cũ trong lịch sử, lịch mới không mang dấu; chỉ đổi sân/Coach cùng giờ thì giữ. Dừng hỏi trước áp dụng; source chưa triển khai dữ liệu báo vắng riêng nên không khẳng định lỗi live.
- Chỉ docs/đọc source, không code/DB hoặc tests ứng dụng. D08 quyền bù/gia hạn tiếp tục để riêng.

### [25/10/2026] D07-a chốt A và UI/UX chung tạo–dời ca

- Owner chọn A: Coach chính dời sớm/Manager hỗ trợ có audit, cả ca cũ và ca mới >=12 giờ từ hiện tại, trong hạn/không đổi credit; yêu cầu UI/UX tích hợp thuận tiện với tạo ca. Handoff ở 04 mục 16.3–16.4/API contract mục 14.
- Ghi rõ mở dời từ buổi đang xem, điền sẵn thông tin, dùng chung bộ chọn ca/phòng và hiển thị cũ-mới/tác động; không giải phóng lịch khi preview hoặc tạo-hủy bằng hai request. Quyền và ngoại lệ do BE kiểm theo ngữ cảnh.
- Dừng hỏi D07-b: đề xuất Manager duyệt bất khả kháng khi Member báo sát giờ, ca cũ chưa bắt đầu, ca mới vẫn >=12 giờ và trong hạn; không tự áp ngoại lệ lỗi trung tâm. Dấu báo vắng sau dời và quyền bù quá hạn còn tách riêng.
- Chỉ docs, chưa sửa code/DB hoặc chạy tests ứng dụng.

### [25/10/2026] D06-b chốt A; chuyển dời sớm do Member D07-a

- Owner chọn A: Manager được dời do lỗi trung tâm sang ca mới dưới 12 giờ, ca cũ/mới chưa bắt đầu, Member đồng ý và đủ tài nguyên/quyền; giữ credit, không tự gia hạn hoặc áp sang đặt mới. Handoff và nghiệm thu ở 04 mục 15.6/API contract mục 14.
- D07-a: II.2 chưa chọn người thao tác dời sớm. Đề xuất Coach chính dời/Manager hỗ trợ sau thỏa thuận, cả ca cũ và ca mới cách hiện tại >=12 giờ, trong hạn gói và không đổi credit; dừng hỏi A/B trước triển khai. Chưa chốt Member tự dời hoặc ngoại lệ sát giờ.
- Chỉ docs/đối chiếu source đã đọc; không sửa code/quyền/DB hoặc chạy tests ứng dụng.

### [25/10/2026] D06-a chốt A; tách notice dời do trung tâm D06-b

- Owner chọn A: Manager ghi nhận đã trao đổi/Member đồng ý đúng phương án xử lý sự cố, lý do/actor/time và tác động trước/sau; audit, thông báo sau commit, không thêm vòng Member trực tiếp duyệt. Handoff ở 04 mục 15.4 và API contract mục 14.
- D06-b phát hiện chưa rõ notice đối với ca mới khi dời do trung tâm; source hiện không kiểm 12 giờ ở reschedule. Đề xuất Manager được ngoại lệ ca mới dưới 12 giờ nếu ca cũ/mới chưa bắt đầu, Member đồng ý và tài nguyên/quyền hợp lệ; dừng hỏi trước chốt.
- Không tự gia hạn gói/bỏ kiểm tài nguyên hoặc mở ngoại lệ sang yêu cầu Member. Chỉ docs/đọc source, chưa code/DB/tests ứng dụng.

### [25/10/2026] D05-b chốt A; rà ba cách xử lý sự cố trung tâm D06

- Owner chọn A: báo vắng Advanced riêng khi còn dưới 12 giờ và trước giờ bắt đầu, lưu người/giờ/lý do, giữ tài nguyên và credit; kết quả điểm danh theo thực tế, không tự Absent bất biến. Lưu handoff BE/FE/kiểm thử ở 04 mục 14.3, API contract mục 14.
- D06 giữ quyết định có đủ dời/thay Coach/hủy-hoàn, không hỏi lại. Rà DTO/source thấy thiếu ghi nhận Member đồng ý; reschedule đang tự gia hạn toàn gói cần xử lý D08. Không coi route hiện có đã đủ nghiệp vụ mới.
- Dừng hỏi D06-a: Manager xác nhận đã trao đổi và Member đồng ý (đề xuất MVP), hay Member trực tiếp bấm duyệt. Chưa thêm trạng thái chờ/endpoint hoặc mở ngoại lệ dưới notice. Chỉ docs/đọc source, không code/DB/tests ứng dụng.

### [25/10/2026] D05-a chốt 12 giờ; chuyển riêng báo vắng D05-b

- Owner chọn 12h: Advanced còn >=12 giờ được hủy/hoàn đúng một credit gói gốc, dưới ngưỡng không hủy hoàn thông thường; không trừ kép/gia hạn hoặc đổi Normal. Đã ghi 04 mục 13.3 và API contract mục 14, thêm cảnh báo implementation 6.4 vẫn 24 giờ.
- Đối chiếu báo vắng: code yêu cầu Enrollment/notice hai giờ và ghi Excused; finalization không tự đổi Attendance đã tồn tại thành Absent. PDF đề xuất tự Absent bất biến. Đề xuất D05-b A lưu báo vắng riêng dưới 12 giờ trước giờ bắt đầu, chốt attendance theo thực tế; dừng hỏi A/B trước sửa.
- Chỉ docs/đọc source; chưa sửa AGENTS/backend/DB hoặc chạy tests ứng dụng.

### [25/10/2026] D04-a chốt A; chuyển cutoff hủy D05-a

- Owner chọn A: Coach chính tạo sau thỏa thuận, Manager hỗ trợ cùng center có lý do/audit; xác nhận đã thống nhất, lưu actor/time và thông báo Member, không thêm vòng Member duyệt từng ca. Handoff và ca nghiệm thu tại 04 mục 12.4/API contract mục 14; chưa triển khai, chưa tự đổi quyền hủy/dời.
- D05-a đối chiếu AGENTS/API/source: hủy hiện >=24 giờ, PDF đề xuất >=12 giờ; đặt hiện >=12 giờ nên giữ 24 giờ sẽ có ca mới tạo đã không được hủy hoàn thông thường. Đề xuất 12 giờ cho Advanced, dừng hỏi trước khi đổi policy.
- Báo vắng/dời/ngoại lệ để riêng; không coi chốt A là chốt ngưỡng hủy, tự ghi Absent hoặc cấp quyền Member dời. Chỉ cập nhật docs/đọc source, không code/DB/tests ứng dụng/deploy.

### [25/10/2026] Handoff BE hạn gói và bắt đầu D04 quyền tạo ca

- Owner đồng ý cơ chế thông tin hạn đã nêu, yêu cầu lưu hướng dẫn BE. Bổ sung 04 mục 11.16: cập nhật dữ liệu/policy/lịch khả dụng/API/lỗi, trình tự triển khai tương thích, biên deadline/notice, kiểm thực tế và nghĩa vụ handoff FE; chưa triển khai.
- D04 đối chiếu source: AGENTS/API cho Member tự book, PDF I.2 đề xuất Coach tạo sau trao đổi. Chưa có dấu xác nhận/requestId booking; audit đang ghi chủ gói thay actor, book chưa gửi notification. Ghi tại 04 mục 12, không tự thay quyền hoặc cấp MANAGE_CLASSES cho Coach.
- Dừng hỏi D04-a: A Coach chính tạo/Manager hỗ trợ có audit, checkbox xác nhận trao đổi và thông báo Member; hay B giữ Member tự book và mở thêm Coach. Chưa đi cutoff hủy 12/24 giờ.
- Chỉ cập nhật docs/đọc source; không sửa code/DB, không chạy tests hoặc deploy.

### [25/10/2026] D03-c chốt 45×24 giờ và thông tin hạn khi mua/đặt ca

- Owner chốt đủ 1080 giờ từ Paid hợp lệ; các đề xuất Ngày 0/cutoff/ngày lịch không được chọn. Lưu quyết định tại 04 mục 11.15, cập nhật API contract mục 14.
- Phân định BE tính/lưu/trả hạn và kiểm ca kết thúc trong hạn cùng notice; FE công bố trước mua, hiển thị giờ hết hạn và cảnh báo ca không hợp lệ. Không lấy deadline -12 giờ làm mốc đặt cuối chung, không coi gói Expired sớm chỉ vì không còn ca phù hợp.
- Ví dụ hạn 09/12 18:30: Ca 6 kết thúc 18:00 nằm trong hạn, Ca 7 kết thúc 19:45 vượt hạn; tạo Ca 6 cần trước/đúng 04:30 cùng ngày. Còn phụ thuộc Coach/phòng/lễ; chưa tự thêm job gửi nhắc hạn theo ngưỡng/kênh.
- Chỉ cập nhật docs và kiểm phép tính, chưa sửa backend/DB hoặc chạy tests ứng dụng.

### [25/10/2026] Chốt bỏ nhập lịch rảnh; rà chi phí hạn 1080 giờ

- Owner chốt bỏ bước Member khai thời gian rảnh lúc mua; đã cập nhật quyết định 04 mục 11.11 và API contract mục 14.
- Đối chiếu ca/notice: 06:15 là mốc cuối để tạo mới ca 18:15 cùng ngày, 18:00 là mốc cuối để tạo ca 06:00 hôm sau. Cutoff Ngày 0 lúc 18:15 không suy ra từ notice 12 giờ.
- Kiểm source LocalDate trên MemberPackage/repository, kích hoạt, book/hoàn/dời/gia hạn, job, quyền/báo cáo và DTO/FE: 45×24 giờ không cần dữ liệu mỗi giờ hoặc job mỗi gói; chi phí chính là chuyển kiểm hạn sang timestamp nhất quán. Ghi đánh giá và đề xuất chọn 1080 giờ tại 04 mục 11.14; chưa tự chốt.
- Chỉ sửa docs; kiểm ví dụ thời gian, không chạy backend tests/migration hay đổi DB.

### [25/10/2026] Rà phạm vi nhập lịch Member và đề xuất Ngày 0

- Owner đồng ý hướng xét khả năng nhận học viên; trích “D02-b” nhưng nội dung là D03-b bước 2. Giữ nguyên quyết định xóa môn dư. Đối chiếu I.1/I.2: nhập lịch rảnh trước mua là phạm vi mới, đề xuất bỏ khỏi MVP và giữ trao đổi ca sau Paid; chờ chốt phần tinh giản.
- Đánh giá Ngày 0: 19:45 khớp kết thúc ca cuối hơn 19:00; notice 12 giờ khiến không thể lấy giờ đóng cửa làm tiêu chí duy nhất cho ngày không học được. Ghi rõ công thức, ví dụ, chênh một ngày quanh cutoff và lựa chọn đủ 45×24 giờ tại 04 mục 11.12; chưa chọn thay Owner.
- Mở API contract mục 14 để theo dõi các API/feature/logic cần bàn giao FE, đánh dấu dự kiến/chưa triển khai. Chỉ sửa docs, không backend/DB; chưa chạy test ứng dụng cho luồng mới.


### [25/10/2026] D03-a đã chốt; nghiên cứu nhận học viên và hạn 45 ngày

- Owner chốt 5 điểm mua Advanced theo môn/Primary Coach, credit riêng, Subject là nguồn giá, Invoice snapshot/Paid mới kích hoạt và không giữ ca khi chọn Coach. Đã lưu tại 04-DEBUG-NOTE.md 11.4.
- Rà source phát hiện getCoachAvailableSlots chưa có lịch làm việc riêng/tải credit chưa đặt; không đủ để kết luận Coach có thể nhận trọn gói. Đề xuất D03-b: giờ làm thực + kiểm tải và Coach/Manager xác nhận trước Invoice, có cam kết năng lực Pending/Active nhưng không giữ ca. Đây là luồng mới, chưa được duyệt.
- Tham khảo tài liệu chính thức Calendly/Acuity/MyTime/Wix; lưu link, cơ chế và giới hạn đối chiếu ở mục 11.5. Không sao chép rule package booking sau expiry hoặc no-show của sản phẩm khác vào SCMS.
- Đề xuất D03-c: Paid là Ngày 1, 45 ngày lịch tại Asia/Ho_Chi_Minh, endDate=paidDate+44. Ví dụ 25/10 → hết ngày 08/12; đã kiểm phép tính. So sánh rõ với 45×24 giờ và từ buổi đầu. Source +45/inclusive cần sửa nếu chọn Ngày 1, chưa tự cập nhật code.
- Chờ Owner chọn riêng D03-b/c trước khi đi I.2/I.3/I.5; chỉ cập nhật docs, không sửa DB/backend, không chạy test ứng dụng hay deploy.


### [25/10/2026] D02-d xác nhận fixture gói Advanced, bắt đầu D03/I.1

- Owner xác nhận gói 5 và hai subscription là fixture được phép đối soát/tạo lại theo môn. Ghi phạm vi tại 04-DEBUG-NOTE.md mục 10.9; không tự reset gói khác hoặc ghi đè Invoice 1 Paid 3.500.000.
- Đối chiếu source: subscription chưa gắn Subject/Primary Coach, Invoice package lấy giá mẫu gói; cần một nguồn giá niêm yết và ràng buộc môn trên quyền học, không chỉ đổi tên demo.
- D03/I.1 đã có đánh giá và cải thiện tại mục 11: đề xuất chọn môn/Coach trước thanh toán, 12 credit theo đúng đăng ký, không giữ ca trước khi đặt, lấy Subject.advancedPrice và snapshot Invoice. Điều kiện Coach còn khả năng nhận và mốc 45 ngày để chốt riêng tiếp theo.
- Dừng hỏi D03-a về mô hình sản phẩm; chưa sửa backend/DB, chạy migration/test hay deploy.


### [25/10/2026] D02-c xác nhận fixture đăng ký Pickleball

- Owner xác nhận Enrollment 2/Class 4 là dữ liệu demo được phép đối soát và tạo lại theo luồng/giá mới 1.800.000. Không suy quyền reset bản ghi khác.
- Ghi quyết định tại 04-DEBUG-NOTE.md mục 10.8: kiểm lại mapping/liên kết, audit, lịch Normal đủ 12 buổi, Invoice Pending rồi thanh toán hợp lệ mới Registered; không mở quyền hủy Normal hoặc chèn Paid để dựng demo.
- Chưa thực thi SQL/code/test. Chuyển sang D02-d hỏi gói Advanced 5 và hai subscription có cùng phạm vi fixture hay phải giữ quyền học khi chuyển đổi; giữ nguyên Invoice Paid 3.500.000 trong lúc phân tích.


### [25/10/2026] D02-b chốt xóa bản dư, chỉ giữ danh mục Member.pdf

- Owner chọn chỉ 9 môn/18 giá Member.pdf, giữ ID 19–27 và xóa hẳn 8 bản trùng ID 1,2,3,4,5,6,8,9; không dùng Inactive. ID 7 Bắn súng ngoài danh mục thuộc nhóm cần loại bỏ, không map sang Bắn cung. Chưa chạy SQL.
- Khi triển khai phải kiểm lại mapping và FK dưới lock, audit trước/sau; không cascade/reset hay sửa Invoice.amount. Cần chặn seed/API tái tạo bản dư và ghi đè giá. Cập nhật quyết định trong 04-DEBUG-NOTE.md mục 10.7 và phạm vi API contract.
- Kiểm bổ sung read-only: Class 4 giá 1.100.000 có Enrollment 2 Registered và 0 Invoice cùng Member/Class, trong khi niêm yết mới 1.800.000. Dừng D02-c hỏi đây là fixture demo có thể đối soát/tạo lại hay quyền học phải giữ; không tự hủy, tăng giá hoặc tạo Paid.
- Chỉ cập nhật docs/đọc DB; chưa sửa backend/migration, chưa chạy test hay deploy. Những lựa chọn Inactive trong HISTORY cũ là phương án từng đề xuất, đã bị quyết định mới thay thế.


### [25/10/2026] D02 chốt giá niêm yết, hoàn tất kiểm kê chỉ đọc

- Owner đồng ý đủ hai cột PDF/04. Đồng bộ bảng niêm yết API contract mục 2.1 và quyết định trong 04-DEBUG-NOTE.md; chưa đổi DB/code.
- Snapshot DB .env 13:34:27 UTC+7, READ ONLY/REPEATABLE READ: 18 Subject; ID 1–9 giá 0/null slug/outdoor không có liên kết FK đã kiểm. Có 8 bản trùng/đồng nghĩa và ID 7 Bắn súng là môn riêng ngoài 9 môn đã chốt. Nhóm 19–27 giữ liên kết, lệch 16/18 giá so với đã chốt.
- 15 Class không có giá 0, 13 snapshot giá khác niêm yết mới; không tự ghi đè. 6 package không có giá 0; package 5 Advanced chung 3.500.000, thiếu subjectId, đã có 2 subscription và 1 Invoice Paid. Giữ riêng quyết định xử lý, không suy môn từ tên.
- Đã kiểm constraints/index/FK và đường tạo/cập nhật/seed; ghi rủi ro DEFAULT 0/DTO nullable, slug nullable/alias, upsert ghi đè, fallback client và giá gói tách rời. Bảng ID và phòng ngừa tại 04-DEBUG-NOTE.md mục 10.
- Dừng hỏi D02-b: đề xuất giữ 19–27, Inactive tám bản trùng 1,2,3,4,5,6,8,9. Chưa xử lý ID 7, cleanup/migration hoặc deploy. Chưa xác minh DB .env trùng DB Render.


### [25/10/2026] D02 chốt giá riêng Normal/Advanced, trình bảng tiền để duyệt

- Owner chọn A: mỗi môn có một giá Normal và một giá Advanced riêng. Chưa chọn số tiền, chưa thay DB/code.
- Đọc trực tiếp Member.pdf trang 7–8, đối chiếu migration 04 và bảng API contract/hai schema/reset 05. Cả hai bảng đủ 9 môn; 16/18 mức giá lệch nhau, chỉ Normal Cầu lông/Bóng bàn trùng.
- Bảng so sánh đầy đủ và ảnh hưởng nguồn giá nằm tại 04-DEBUG-NOTE.md mục 9.5–9.7. Đề xuất PDF/04 làm giá niêm yết ban đầu; dừng chờ Owner chốt số tiền trước khi kiểm kê/chuẩn hóa bản ghi trùng/0đ. Không sửa snapshot hóa đơn cũ hoặc chạy lại seed.


### [25/10/2026] Chốt phương án D01, chuyển sang làm rõ D02

- Owner chốt GET/PATCH /api/members/me để xem và tự sửa bảy trường hồ sơ; Authentication xác định chủ hồ sơ, DTO riêng, email/mật khẩu dùng luồng riêng, không mở các trường quản trị.
- Đã cập nhật quyết định vào 04-DEBUG-NOTE.md. Đây là hoàn tất lựa chọn D01, chưa triển khai code hoặc kiểm thử tính năng mới.
- D02: source đang có giá Normal/Advanced tách riêng; cần xác nhận “một mức học phí” theo từng loại hay một giá chung. Đề xuất giữ một giá chuẩn cho mỗi môn và mỗi loại; chưa chọn giá hoặc sửa dữ liệu.
- Chỉ cập nhật tài liệu; dừng hỏi D02 theo yêu cầu phân tích tuần tự.


### [25/10/2026] Tiếp nhận báo cáo FE và phân tích 2410 — chưa chốt nghiệp vụ mới

- Hoàn thành bước đọc system-test-rerun-2026-10-24.md, FE-BE-sandbox-e2e-handoff.txt và đủ 8 trang analysis 2410.pdf (gồm bảng ảnh trang 2, bảng trang 5 bị cắt bên phải).
- Tạo docs/04-DEBUG-NOTE.md theo yêu cầu Owner, ngoại lệ có chỉ định đối với bộ ba docs; bảo toàn quyết định đang chờ, gom nguồn trùng thành 19 nhóm, ánh xạ I/II/III và hai yêu cầu bổ sung.
- Chỉ D01 đã đối chiếu code chi tiết: thiếu self-profile route; principal không có memberId; DTO quản trị có status/centerId/packageId nên chưa mở thẳng quyền update cho Member. Đề xuất /me với DTO riêng, dừng hỏi phạm vi tự sửa trước khi phân tích sâu mục kế tiếp.
- Đã ghi mâu thuẫn cutoff 12/24 giờ, giờ ca minh họa, đơn vị giá, quyền tạo lịch và cách gia hạn/bù credit; chưa tự thay quy tắc hiện hành. Nội dung FE ngày 24/10 được giữ là báo cáo có nguồn, chưa probe lại ngày 25/10.
- Chỉ thay đổi tài liệu; không sửa BE/FE/DB, không chạy mutation/test hay push/deploy. Các file Owner chuyển vào old-docs được giữ nguyên; không đọc lại archive trong đợt tiếp nhận.


### [F-06/F-07 — 24/10/2026] Kiểm chứng sau migration do người dùng thực hiện

- [x] Người dùng báo chạy migration Supabase thành công. JDBC transaction read-only xác nhận trên DB .env: subscription_id integer nullable, FK đúng/validated, btree index, SELECT cột thành công, **0 orphan**. Không thực thi thêm migration/ghi dữ liệu nghiệp vụ.
- [x] Probe Render bắt đầu **02:12:49 UTC+7 ngày 24/10**, scope từ 02:13:14: health 200; Session list bốn vai trò 200 (Manager/Receptionist 2 buổi, Member/Coach mẫu 0); dashboard Manager, reports members/revenue 200. Ép bộ lọc người khác 403; Member dashboard 403; anonymous schedule/dashboard 401.
- [x] Đối chiếu kết quả với F-06/F-07: lỗi 500 không tái hiện trong các request đã thử; cấu trúc DB phù hợp migration 07. Ghi bảng bằng chứng mới vào be-request-f06-f07.md. Không thay backend, không chạy lại unit tests vì đây là kiểm chứng DB/HTTP và cập nhật docs.
- Quyền Coach dashboard vẫn 200 như ngày 23/10; giữ riêng để đối chiếu MANAGE_CLASSES. Chưa có SHA deploy hoặc xác minh DB đích qua cấu hình Render; danh sách mẫu rỗng chưa nghiệm thu đầy đủ ownership/assignment/center và luồng đặt lớp.
- Probe trước migration ngày 23/10 đã 200, nên không khẳng định lần chạy vừa rồi là tác động duy nhất đã khắc phục live. Hiện không có bằng chứng cần migration bổ sung để chữa thiếu subscription_id.


### [F-06/F-07 — 23/10/2026] Đồng bộ schema Session và phản hồi FE

#### Đã hoàn thành trong workspace

- [x] Đọc yêu cầu be-request-f06-f07.md, AGENTS và đủ ba docs chính; đối chiếu Session/Repository/ReportService, cấu hình và SQL. Không thay quy tắc nghiệp vụ đang chờ quyết định.
- [x] Xác nhận repo thiếu sessions.subscription_id trong cả hai schema và migration PostgreSQL dù entity sử dụng; ddl-auto=none. Dashboard và Session list đều đọc Session.
- [x] Kiểm DB trong .env bằng JDBC transaction read-only: cột integer nullable, FK fk_session_subscription đã validated và btree idx_sessions_subscription đều tồn tại. Không sửa DB này; chưa xác nhận đây là DB Render. Không có bảng history migration/flyway/changelog theo tên trong public, không suy danh sách migration đã chạy.
- [x] Thêm migration 07 PostgreSQL/public với transaction, lock timeout, nullable column, kiểm orphan và định nghĩa FK/index kể cả tên khác. Dừng nếu không tương thích, không backfill/xóa/sửa dữ liệu nghiệp vụ. Đồng bộ nguyên block vào hai schema sau bảng member_packages; hai file schema giống nhau.
- [x] Thêm script PostgreSQL Docker tách biệt và SessionSchemaPostgresTest opt-in. Thử schema gốc dựng mới và schema database/ mô phỏng DB trước bản sửa; migration lặp, tên FK/index tùy chỉnh, NOT VALID được validate, orphan/index sai/FK cascade bị từ chối và rollback. Đối chiếu snapshot dữ liệu và liên kết hợp lệ được giữ nguyên.
- [x] Phản hồi đủ năm nhóm yêu cầu và phần thông tin còn thiếu trong be-request-f06-f07.md. Cập nhật API contract mục 13 và deployment mục 8; giữ đúng ba docs chủ chốt.

#### Kết quả kiểm chứng

- PostgreSQL Docker **17.11**, hai DB local riêng. Mỗi DB chạy **7 test ứng dụng, 0 failure/error/skip**, tổng **14 lượt pass**. Spring ddl-auto=none đọc Session thật; MockMvc qua security dùng quyền lấy từ DB: Session list bốn vai trò 200, dashboard Manager và reports members/revenue 200, request ẩn danh 401. Mock cả ba job, không có ghi nền. Chưa phải HTTP probe qua Render hay nghiệm thu toàn bộ phạm vi dữ liệu.
- `.\mvnw.cmd clean test`: **BUILD SUCCESS**, **223 tests, 204 pass, 19 skipped, 0 failure/error**, Maven 1 phút 20 giây. Đã đối chiếu 37 XML Surefire. 19 skip gồm 15 mục opt-in cũ và 4 mục opt-in của class PostgreSQL (khi bật, parameterized test bốn vai trò tạo tổng 7 lượt chạy). Không sửa kỳ vọng test nghiệp vụ cũ.
- Schema gốc/bản database đồng nhất và phần FK đặt sau member_packages; kiểm diff whitespace trước bàn giao.

#### Giới hạn bàn giao / chờ nghiệm thu

- HEAD trước sửa: `8e957593b865b1172814c2622578c5f7f1e5656a`, không phải SHA deploy. Commit FE dẫn chưa có object local. Chưa commit/push/deploy hoặc áp dụng migration lên DB đang dùng.
- Log Render do người dùng cung cấp xác nhận PSQLException thiếu s1_0.subscription_id lúc 23/10/2026 08:28:28 Asia/Ho_Chi_Minh (01:28:28 UTC), sớm hơn probe FE 14:26:30 gần 6 giờ. Đoạn trích chưa có endpoint/SQL đầy đủ/SHA, không tự gắn với cả hai request FE.
- Probe thật tại https://scms-app-p1gy.onrender.com bắt đầu 19:23:02 UTC+7: health 200; Session list bốn vai trò 200 (Manager/Receptionist 2 buổi, Member/Coach mẫu 0); dashboard Manager và reports members/revenue 200. Kiểm thêm từ 19:23:54: Member/Coach ép bộ lọc ID khác bị 403, Member dashboard 403, anonymous schedule/dashboard 401. Login/logout bình thường; không ghi nghiệp vụ, migration hay deploy.
- Coach dashboard trả 200: login live có MANAGE_CLASSES và ROLE_COACH; hợp annotation hiện tại nhưng khác seed workspace. Giữ nguyên, chờ đối chiếu quyền mong muốn; không chứng nhận toàn bộ RBAC.
- Đã có bằng chứng lỗi quá khứ và endpoint hiện không lỗi 500. Chưa có SHA deploy, lịch sử migration/DB Render hoặc log đúng request cũ; chưa nghiệm thu toàn bộ dữ liệu/phạm vi/luồng đặt lớp. Báo cáo chi tiết và bảng probe nằm trong be-request-f06-f07.md.

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

### Giai đoạn 8: Thông Báo Trong Ứng Dụng
- [x] Mở rộng tương thích ngược `notifications` và `notification_recipients`; giữ dữ liệu và luồng legacy.
- [x] API danh sách, lọc, đếm chưa đọc, đánh dấu một/tất cả đã đọc; giới hạn theo user đăng nhập.
- [x] Event listener chạy AFTER_COMMIT trong transaction riêng; lỗi lưu thông báo không ảnh hưởng nghiệp vụ chính.
- [x] Tích hợp 13 loại sự kiện, job nhắc lịch 24 giờ và job gói 1:1 sắp hết hạn.
- [x] Bổ sung test service, controller/role, listener và job với Clock cố định.
- [x] Cập nhật hợp đồng API và hướng dẫn migration.
- [x] Chạy hồi quy `.\mvnw.cmd clean test`: 193 tests run, 178 passed, 15 skipped, 0 failed.
- [ ] Chạy migration 05 trên PostgreSQL DB sạch và DB có notifications legacy để xác nhận thực tế.

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
| Kiểm API có phiên, owner/role/center trên Render | BLOCKED; chưa chạy nghiệp vụ | Thiếu tài khoản thử, fixture, phạm vi live và cách đọc state sau commit; kiểm `/auth/me` trước |
| Toàn bộ tổ hợp CRUD baseline × role/owner/center ngoài matrix workflow đích | NOT_RUN đầy đủ | Xác định contract/fixture từng nghiệp vụ; không suy PASS từ route/schema hoặc workflow liên quan |
| OTP/email đăng ký cloud | PASS nhận thư, verify204, user42 Active và login đúng Member42 theo Owner | Dùng response login trên FE: userId/memberId42, role Member, authorities và isFirstLogin=false, phù hợp dashboard Member. Các ca OTP lỗi/biên và full quyền chưa nghiệm thu |
| D01 hồ sơ Member; D09 Coach directory trên Render | PASS local; chưa publish | Main local đã tích hợp và kiểm PG có phiên; chưa push tại điểm review đã chốt, không gọi đó là nghiệm thu Render |
| FE browser/CORS/cookie/camera QR | PASS CORS/cookie và redirect Pages HTTP04/11; browser/F5/QR chưa nghiệm thu đầy đủ | Cookie None/Secure/HttpOnly; Return đã trỏ Pages /member/invoices. Cần auth/me200 từ FE/F5, dữ liệu API sau thanh toán và scope QR để chốt phần liên quan |
| VNPay ghi nhận tiền sandbox | PASS ca invoice40 theo Owner/API: Paid, gateway ref có giá trị, không đối soát; FE hiện ngay sau Return | Chưa nghiệm thu trace IPN/race/retry live hoặc quyền đăng ký/lịch; giao dịch cũ đối soát riêng, không thu lại |
| VNPay QueryDR dự phòng | Owner chốt giữ nguyên code/endpoint, bảng, migration và env; kết nối live chưa khôi phục | Không tắt/gỡ hoặc đổi env theo đề xuất cũ; xử lý tuyến mạng riêng nếu tiếp tục nghiệm thu QueryDR |
| SQLServer runtime | NOT_RUN | Môi trường kiểm riêng nếu được chọn |
| Schema demo/Coach theo Owner29/10 | PASS local; báo cáo có snapshot Supabase29/10 | Snapshot do Owner xuất là bằng chứng lịch sử được báo cáo dẫn, chưa được session này kiểm độc lập hoặc xác minh lại live; không ghi chung là chưa từng chạy Supabase |
| Báo vắng tự ghi Absent | PASS local; Owner xác nhận migration14 hoàn tất; chưa xác nhận deploy/live | Publish BE mới rồi kiểm workflow có phiên; schema sẵn sàng không thay nghiệm thu runtime |
| Danh mục20room/room_code và7giới hạn lớp | Owner xác nhận cả hai bundle Supabase hoàn tất | Nạp lại dữ liệu rooms/classes trên FE sau deploy; Agent chưa truy vấn kiểm độc lập dữ liệu live |

**Mức sẵn sàng FE:** contract/schema đủ để bàn giao phạm vi BE đã kiểm local có điều kiện; chưa ký full authenticated Render hoặc FE acceptance. Matrix lịch sử ở `authenticatedAcceptance`, bằng chứng room/báo vắng02/11 ở `roomAbsenceReview` của [QA duy nhất](../artifacts/qa/latest.json). Đối chiếu từng phát hiện gốc và giới hạn bằng chứng tại [bảng1:1](#report-trace).

**Bản chạy tạm cho Owner:** đã xuất `0211-1.sql` và `0212-2.sql` ở root để chạy đúng hai lần theo docs02. Đã kiểm thứ tự, guard/rollback, chuyển đổi, postflight và replay trên PostgreSQL riêng ở working tree trên `ec2325df07df053257df09698857b27e36489820`; giữ nguyên SQL nguồn13/14, không chạy Supabase hoặc thêm kết quả BE mới. Bằng chứng/checksum tại `rootBundlePackaging` trong QA. Sau khi Owner chạy thành công và kiểm kết quả có thể xóa hai bản root; nguồn migration vẫn giữ.

**Bước kế tiếp:** OTP: bàn giao theo bản OTP04/11, dùng response login để FE nhận danh tính/quyền; giữ SMTP2525 và quyền IP đã xử lý. Login/portal Member đã có bằng chứng Owner, không yêu cầu làm lại; chỉ kiểm OTP lỗi/biên khi có fixture/phạm vi tương ứng. Không cần deploy hoặc gửi thư lặp chỉ để ghi nhận lại ca đã đạt. FE dùng bản VNPay riêng để ghép POST/GET theo contract; không tạo thêm thanh toán chỉ để lặp ca đã Paid. Nếu chốt toàn bộ quyền học, đọc đăng ký/lịch của ca đã Paid; đơn cũ chưa ghi tiền vẫn đối soát riêng. QueryDR giữ nguyên theo D10, chưa thêm relay hoặc đổi env.

Đọc: [AGENTS](../AGENTS.md) → [quyết định Dxx](04-DEBUG-NOTE.md) → [contract FE](01-API-CONTRACT-FE-BE.md) đúng luồng; [vận hành](02-DEPLOYMENT-GUIDE.md) khi cần.

## Nghiệm thu QueryDR — 04/11/2026

**Bổ sung kiểm mạng runtime:** commit local `ea15e9712d3ea07e2134888e427a321b12ce9c0f` đã kiểm trong bản sao không.env: clean368run=340pass/28skip/0failure/0error, Java17/H2/local HTTP. Client10case và network probe6case, gồm default-off, bật bean, chạy một lần, không chặn khởi động, lỗi DNS/TCP vẫn kiểm đối chứng, TLS3→15 và log không lộ exception message; toàn bộ signature/settlement regression giữ. Timeout mới mặc định3/5, chặn1..15 và request≥connect; chỉ client/config/probe thay đổi, không DB/migration. Bằng chứng ở `vnpayConnectTimeoutFollowup` trong QA. Chưa push/deploy hoặc chứng minh đường mạng Render bằng probe.

**Cập nhật runtime chẩn đoán:** Render hiện báo `4e6a88ee536a064d3ba3d8929b2f0684e8ce9fea`; đã đối chiếu source client có CONNECT_TIMEOUT/TLS_FAILURE/DNS_FAILURE và khớp local. POST37 sau cập nhật vẫn QUERY_UNAVAILABLE; đang chờ log reason mới, chưa xác minh được tiền. Các ghi chú "chưa publish" ở các mốc kiểm trước là lịch sử.

**Bổ sung chẩn đoán transport:** commit local `2840629` trên base `1edb26dc877c25b46351ce8d95e0bae8d37a2562` tách mã lỗi timeout/TLS/DNS/I/O/HTTP status, không xuất exception message/URL/body/key; chữ ký, settlement và timeout giữ nguyên. Chỉ commit client/test, giữ ngoài phần tài liệu/OTP. Test đích client8/8 và `clean test` mới trong bản sao không.env:360run=332pass/28skip/0failure/0error, Java17/H2/local HTTP; source khớp bản copy đã kiểm trước commit. Fingerprint và XML summary ở `vnpayTransportFollowup.diagnosticValidation` trong QA. Patch chưa push/deploy; probe Java cục bộ chỉ chứng minh AIA giúp kết nối TLS tại máy kiểm tra, chưa chốt lỗi Render. Owner đã gửi lại QUERY_UNAVAILABLE sau bước thử JVM; cần log mới sau publish patch.

**Source/môi trường:** snapshot working tree chưa commit trên `ec2325df07df053257df09698857b27e36489820`; fingerprint đã kiểm và danh sách email thay đổi đồng thời ở `queryDrImplementation` trong [QA](../artifacts/qa/latest.json). Java17; bản sao không.env; H2 và PostgreSQL17 riêng `127.0.0.1:55440`, Clock/JVM Asia/Ho_Chi_Minh; provider HTTP giả lập. Toàn bộ source invoice/test QueryDR hiện tại khớp bản sao đã kiểm; kết quả không bao gồm chỉnh sửa email xuất hiện sau khi chụp source.

| Điều kiện nghiệm thu | Kết quả local |
|---|---|
| Migration15 cộng thêm/replay/guard | PASS9nhóm; hash dữ liệu46bảng cũ giữ nguyên; attempt giữ khi replay; xung đột cấu trúc/guard rollback, FK/unique/check đúng; root/demo và empty release tương thích |
| Request/response và bằng chứng thanh toán | PASS HMAC golden độc lập, đúng pay CreateDate, query thời điểm HCM, chữ ký/merchant/ref/amount/type/status; sai/thiếu chữ ký hoặc timeout không Paid |
| Auth/state/quyền | PASS HTTP login+auth/me; owner/role/center/Active/body; số tiền mua giữ nguyên khi catalog đổi; QueryDR cấp12credit đúng một lần; cutoff và quyền không còn → Paid+đối soát, không cấp lại |
| Retry/race/đọc danh sách | PASS cooldown, metadata nhiều link/kiểm link cũ, QueryDR↔IPN không giữ khóa trong HTTP, không nhân credit/thông báo; GET chỉ đọc; đơn cũ thiếu metadata không tự sửa |
| Contract/rollout | POST trả wrapper+InvoiceResponse; snapshot OpenAPI sinh từ cùng app PG. File15, toggle/chế độ chỉ đọc, thứ tự publish và FE auto POST→GET đã bàn giao |

**Kết quả:** `clean test` trong bản sao không.env:354run =326pass/28skip/0failure/0error; fixture cuối kiểm đích H2 11/11 và PostgreSQL11/11 PASS với ddl-auto=none sau migration15/các prerequisite. Skip không là pass. Lượt PG đầu bị chặn bởi alias JVM Asia/Saigon; chạy lại Asia/Ho_Chi_Minh. Đã sửa setup center_name và dựng đủ migration nền; test fixture cuối được kiểm lại, source BE không đổi. Không nới expected nghiệp vụ.

**Giới hạn của lượt kiểm local ban đầu:** lúc kiểm chưa gọi QueryDR VNPay thật, deploy Render/chạy SQL Supabase hoặc ghép FE browser. Cập nhật deploy/migration hiện hành ở Active. Capability merchant/chữ ký live còn NOT_RUN; đơn cũ không có attempt vẫn cần đối soát. Có thay đổi email ngoài snapshot nên không gán PASS toàn bộ working tree hiện hành hoặc nghiệm thu lại email từ count này. Canonical QA giữ cả snapshot đã kiểm và fingerprint hiện tại; kiểm route/schema offline có thể REVIEW_REQUIRED cho phần identity source đồng thời này.

## Nghiệm thu room/báo vắng — 02/11/2026

**Source/môi trường:** working tree chưa commit trên `ec2325df07df053257df09698857b27e36489820`; fingerprint source/schema/test/runner, hash SQL và case response+SQL sau commit ở `roomAbsenceReview` trong [QA duy nhất](../artifacts/qa/latest.json). Java17/H2 clean test trên bản sao không.env; HTTP PostgreSQL17 riêng `127.0.0.1:55440`, DB/actor/fixture mới, Clock Asia/Ho_Chi_Minh. Không gọi cloud/email/gateway.

| Điều kiện nghiệm thu | Kết quả local |
|---|---|
| room_id số, room_code riêng; không subject_id | PASS HTTP POST/PUT/GET và schema: mã lưu đúng, trùng409, format/center400, Member403; thiếu mã POST tự sinh/PUT giữ mã; ẩn Closed LEGACY |
|20tài nguyên theo room.jpg; Bóng rổ/Bóng chuyền tách sân | PASS root fresh/replay, schema dựng lại/replay và empty release bundle; metadata/sức chứa dùng trong fixture, Normal không vượt phòng |
| Nâng cấp bảo toàn và guard | PASS giữ chứng từ/quyền/ghi danh/attendance/result và lịch đã diễn ra; chuyển lịch tương lai đã phù hợp, giữ8phòng cũ Closed; replay giữ sửa đổi; over-capacity/custom room/overlap dừng và rollback |
| Báo vắng tạo notice+Absent cùng commit | PASS đúng Member trong0<notice<12h; sai owner/role/center, Normal và biên0/12h bị chặn; retry/race chỉ1notice/attendance; inject lỗi PG không commit notice/attendance; credit giữ nguyên |
| Lịch sử và kết quả thực tế | PASS đổi phòng giữ notice; đổi giờ archive attendance tự động, không xóa; bản cũ không xuất trong kết quả hiện hành/không correction; Coach/QR cập nhật Present; finalizer/retry và Manager correction đúng, đồng bộ result hiện có; khác center403 trước kiểm trạng thái bản cũ |
| Bàn giao contract | PASS snapshot OpenAPI từ cùng HTTP app, kiểm route/status/type/property offline; docs01 giải thích mã phòng, notice/attendance và nghĩa report; README giữ một bảng20room, docs02 có preflight/migration/postflight |

**Kết quả:** clean338run =310pass/28skip/0failure/0error; HTTP PG22/22 nhóm PASS;9nhóm migration PASS; empty bundle PASS20room/0account. Skip không là pass. Lượt clean đầu lỗi fixture H2 chèn room thiếu code: thêm mã cho fixture, giữ nguyên expected nghiệp vụ. Expected notice-only cũ được thay theo Owner31/10 đã xác nhận02/11, không đổi để ép xanh. Bổ sung kiểm thứ tự scope khi correction bản archived trước lượt clean/PG cuối. Mỗi case có phiên đã kiểm auth/me và state sau commit; callback local không là gateway live.

**Giới hạn của lượt kiểm local này:** tại thời điểm kiểm chưa áp dụng migration/deploy hoặc kiểm lại Supabase/Render; kết quả Owner chạy migration về sau ghi ở mục owner-class-capacity; snapshot29/10 không được nâng thành kết quả mới. PASS local ở trên dựa assertion đã chạy, không có nghĩa mọi giá trị SQL đã được trích đầy đủ vào QA; thiếu capture cụ thể ghi tại [bảng1:1](#report-trace). Nghiệm thu ban đầu dùng fixture đã đối soát max_capacity; module danh mục chung không tự giảm giới hạn lớp. Bước Owner-specific mới được duyệt sau đó xem mục owner-class-capacity bên dưới. Chạy guard trên DB đích vẫn có thể dừng nếu dữ liệu thay đổi/ngoài phạm vi duyệt. Hồ sơ báo vắng chi tiết gốc chưa có trong workspace; báo cáo31/10 đã sửa liên kết tới nơi theo dõi hiện hành. Phần còn lại và thứ tự triển khai ở Active/docs02.

## Sửa tương thích inventory room — 02/11/2026

**Nguyên nhân đã xác định:** Owner gửi inventory Supabase9phòng, gồm room9 Sân Tennis02. Guard cũ chỉ liệt kê tên/alias của8phòng fixture nên chặn nhầm tên đã thuộc20phòng Owner duyệt. Đây là thiếu sót nhận diện/fixture của script, không yêu cầu Owner xóa phòng9 hoặc đổi dữ liệu để vượt guard.

**Đã sửa lỗi Tennis02:** migration13, hai schema gộp và0211-1 nhận cả tên20phòng đã duyệt, giữ chặn phòng lạ kèm DETAIL room_id/tên/trạng thái. Lịch tương lai của Tennis02 chuyển đúng C01-TEN-02, không dồn vào TEN01. Phòng/ID cũ và lịch sử vẫn giữ. Module room13 không tự đổi giới hạn lớp; bước7lớp được Owner duyệt riêng sau đó nằm trong0211-1, còn0212-2 đã bổ sung kiểm marker bước này. DDL migration14 không đổi.

**Kiểm:** working tree trên `ec2325df07df053257df09698857b27e36489820`, PostgreSQL17 riêng `127.0.0.1:55440`, fixture đủ9phòng Owner và hai sân Tennis có lịch song song với Coach khác nhau.10nhóm SQL và5nhóm hai file root PASS, gồm guard/rollback/replay, bảo toàn chứng từ/attendance và map đúng TEN02. Fingerprint/checksum và bằng chứng tại `roomCatalogCompatibilityFix`/`rootBundlePackaging` trong QA. Java/endpoint/test BE không thay đổi; không chạy Maven mới hoặc gán lại count BE từ lượt cũ cho SQL mới.

**Hướng dẫn tại thời điểm sửa:** Owner copy lại toàn bộ0211-1 mới vào SQL Editor, chỉ chạy0212-2 sau khi file1 thành công. Lần bị CUSTOM_ROOM trong transaction của file không commit thay đổi. Với9phòng cũ, lần chuyển thành công giữ9legacy Closed và20room mới; nếu tiếp tục gặp SCHEDULE_REVIEW thì xử lý lớp/lịch theo preflight, không bỏ guard. Agent không truy cập/ghi Supabase. Owner đã xác nhận cả hai bundle hoàn tất sau bước duyệt giới hạn lớp; xem mục owner-class-capacity.

**Bổ sung chẩn đoán SCHEDULE_REVIEW02/11:**0211-1/nguồn13/hai schema gộp nay trả DETAIL gồm class_id/session_id, max_capacity/target_capacity/held_seats, thời điểm/center/phòng đích và reasons. Điều kiện chặn không đổi, không tự sửa dữ liệu.13nhóm PostgreSQL local PASS trên source SQL hiện tại cùng baseline/fingerprint Java giữ nguyên; đã kiểm phân biệt cấu hình vượt sân dù ít học viên, max_capacity NULL, rollback và hai file root. Không chạy Maven mới hoặc Supabase; xem scheduleReviewDiagnosticSupport trong QA. Kết quả DETAIL Owner đã gửi và đề xuất xử lý hiện hành ngay bên dưới.

**DETAIL Owner và quyết định tiếp theo:** các dòng được gửi đều CLASS_CAPACITY_EXCEEDS_ROOM ở7lớp11/12/13/14/16/17/18, mỗi lớp giữ1chỗ. Owner đã chốt bảng cũ→mới trong docs02; xem bundle triển khai ngay bên dưới. DETAIL tối đa20buổi nên chưa suy đủ mọi lớp ngoài phần đã gửi; guard tổng vẫn kiểm trước commit. Agent chưa xác minh trực tiếp hoặc cập nhật Supabase.

<a id="owner-class-capacity"></a>
## Bàn giao7giới hạn lớp đã duyệt — 02/11/2026

**Source/phạm vi:** working tree trên `ec2325df07df053257df09698857b27e36489820`; bước theo ID đã duyệt ở `13_owner_class_capacity_20261102_postgres.sql`, gộp vào0211-1 trước room13 trong cùng transaction. Không nhúng bước này vào schema gộp/empty release cho DB khác. Chỉ7lớp đúng center1/Normal/môn/giới hạn cũ hoặc đã bằng mức mới được chấp nhận; Registered+Pending phải vừa mức mới. Sai/stale hoặc quá chỗ thì toàn file dừng.

**Kiểm local:** PostgreSQL17 riêng `127.0.0.1:55440`, baseline9phòng và7giới hạn cũ Owner đã gửi.13nhóm kiểm Owner-capacity và13nhóm regression SQL/root PASS. Đã kiểm sai môn/center/loại lớp/giới hạn stale, quá held seats, rollback cả7giới hạn/audit khi bước room sau đó lỗi, đúng7mức mới, không đổi lớp ngoài scope hoặc field khác, giữ tiền/quyền/ghi danh/attendance/lịch đã diễn ra. Audit trước/sau đủ7lớp và held seats; replay không ghi đè thay đổi về sau, không nhân audit; trường hợp một lớp đã bằng mức mới vẫn được xử lý. Bằng chứng/hash/fingerprint tại `ownerClassCapacityApproval` và rootBundlePackaging trong QA. Không đổi Java/endpoint hoặc chạy Maven mới; không nâng kết quả BE/cloud từ kiểm SQL.

**Kết quả Owner:** đã xác nhận0211-1 và0212-2 đều hoàn tất trên Supabase. File2 kiểm đủ marker Owner-capacity và room. Bước cập nhật lớp chỉ đổi max_capacity, không sửa người học/giá/chứng từ/attendance; lịch tương lai đổi room theo13, giữ nguyên ngày/giờ. Agent không ghi hoặc truy vấn độc lập Supabase. Guard vẫn chặn dữ liệu ngoài bảng hoặc vượt sĩ số thực; không hỏi lại7mức đã chốt.

**Bằng chứng được gửi:** `idx_attendance_absence_notice` trên absence_notice_id với điều kiện IS NOT NULL; `ux_attendance_session_member` unique trên(session_id,member_id) với điều kiện session_id IS NOT NULL AND superseded_at IS NULL. Hai định nghĩa khớp migration14. Ghi nhận migration thành công theo xác nhận Owner, không nâng thành PASS authenticated Render/FE hoặc chứng nhận đã đối soát độc lập toàn DB. Nguồn chi tiết tại `ownerReportedSupabaseMigrationCompletion` trong QA; tài liệu FE gồm đủ20mã room và bảng7lớp cũ→mới.

**Kiểm tài liệu sau xác nhận:** đối chiếu20mã/sức chứa/loại phòng với migration13 và7giới hạn đã duyệt, kiểm liên kết tới bản FE độc lập, UTF-8 và git diff --check đạt. Offline contract audit PASS trên bằng chứng/source đã giữ; lượt này không chạy lại BE hoặc probe cloud.

<a id="report-trace"></a>
## Đối chiếu1:1 với báo cáo gốc — review tài liệu02/11

Nguồn: [báo cáo29/10](../SCMS-BAO-CAO-KIEM-THU-FE-API-DB-2026-10-29.md), [bàn giao31/10](../SCMS-BE-API-DB-TEST-HANDOFF-2026-10-31.md) và thay đổi room Owner đã duyệt trong session. Bảng dưới nối từng phát hiện/nhóm quan sát với kết luận và nơi bàn giao; không xác nhận lại các run/CSV/live gốc. SHA working tree và môi trường local giữ theo nghiệm thu phía trên; lần review này chỉ kiểm tài liệu/QA/source, không chạy BE mới.

| Nguồn / phát hiện gốc | Kết luận, trạng thái hiện hành | Bàn giao / giới hạn |
|---|---|---|
|29 §2: center1, môi trường và source Render chưa xác nhận | Giữ thiết kế center1 đã chốt; không phân loại thành lỗi | docs01 mục1/14; không đồng nhất SHA tham chiếu trong báo cáo với runtime Render |
|29 BE-01: Admin dashboard200/toàn0 | Hiện tượng cũ phù hợp query theo center; đã giải thích, không tự mở scope toàn hệ thống | docs01 mục14; chưa có kiểm mới riêng Admin-null+VIEW_REPORTS/dashboard trên cùng snapshot gốc |
|29 BE-02: auth/me không có centerId | Đúng DTO, không phải lỗi mapping; giữ nguyên | docs01 mục1; BE lấy center từ phiên, không cần thêm field theo thiết kế đã chốt |
|29 BE-03: activeSubscriptions11 | Chưa chứng minh số11 sai; code đếm lượt Advanced+Normal còn quyền, không phải người | docs01 mục14; chưa tái tính độc lập từng quyền/ghi danh của snapshot gốc, không coi2+9 tự chứng minh11 |
|29 BE-03: doanh thu dashboard khác revenue tháng10 | Khác kỳ chưa phải lỗi | docs01 mục14; chưa đối soát lại từng hóa đơn gốc, không sửa số liệu để khớp |
|29 §4: API đọc200 và các ca403 đúng role | Giữ kết quả lịch sử đúng endpoint/role/query đã kiểm | Không nâng toàn bộ CRUD/RBAC/field thành PASS; phạm vi chưa kiểm ở Active |
|29 §4: GET members/me/subscriptions400 | Tuyến GET không được định nghĩa; không phải lỗi BE cần sửa | docs01 mục4: dùng GET members/{memberId}/subscriptions; tuyến me là POST mua gói |
|29 §4: F-06/F-07 từng500 | Không gọi lỗi vẫn còn tại tổ hợp đã kiểm lại200; chưa đóng toàn bộ role/query cũ | Giữ giới hạn trong báo cáo gốc; không suy current Render từ local |
|29 §4: catalog trả5ca | Bộ lọc/tương lai không phản ánh tổng số buổi trong DB | docs01 mục11; không yêu cầu bổ sung buổi chỉ để catalog trả12 |
|29 §5 /31 DB:53cột và inventory16bảng | Đủ phần truy vấn được báo cáo kiểm, không chứng nhận toàn schema/index/constraint | Giữ snapshot lịch sử; migration13–14 mới có nghiệm thu riêng phía trên |
|29 §5 /31 DB:33account,9môn,8phòng | Các số được báo cáo xác nhận tại snapshot29/10, không phải lỗi |8phòng được Owner thay bằng20 sau đó; nâng cấp giữ phòng legacy, không buộc tổng vật lý của DB nâng cấp bằng20 |
|29 §5 /31 DB:9Normal+9template;12buổi hợp lệ;13dòng Tennis/makeup109 | Đúng mô hình, không xóa buổi invalidated để làm đẹp count | README database mục3, D15 và docs01 mục9/11; ID/số dòng gốc không là quota cho DB khác |
|29 §5 /31 DB: invoice/quyền/ghi danh và114session | Số liệu snapshot, chưa chứng minh bất thường | Không seed/rebuild để giữ nguyên số cũ; nghiệm thu migration kiểm bảo toàn dữ liệu được bảo vệ |
|29 §5 /31 DB:73attendance/73result; không orphan/duplicate/mismatch | Giới hạn đúng các query gốc, chưa kiểm lại CSV độc lập | Không suy mọi attendance luôn cần result: Absent do Member báo vắng không tạo đánh giá giả của Coach |
|29 §5 /31 DB: credit không âm, đúng chủ quyền | Không có lỗi được chứng minh trong query gốc; chưa là kiểm toán ledger | Workflow local mới không nâng thành chứng nhận ledger/live gốc |
|29 §5 /31 DB: có incident/notice và0cặp trùng sân | Xác nhận tồn tại/không thấy lỗi trong bộ lọc, không chứng minh toàn workflow/race/Coach/Member | Phần local mới có case riêng; không gộp thành kết quả snapshot gốc |
|29 §6 /31 thanh toán: Cash, thiếu Pending/Online/resolution | Không phải lỗi dữ liệu; chưa chứng nhận gateway/refund live | D10/D16, docs01 và Active; callback ký local không thay VNPay sandbox |
|31 BE-ABSENCE-01 đề nghị1–2: ghi Absent nguyên tử, retry/owner/window | Đã sửa và PASS local bằng assertion HTTP/SQL trong test | D05/D15, docs01 mục7; QA ABSENCE-UNDER-12H/LIFECYCLE/ATOMIC-ROLLBACK; giới hạn trích xuất SQL bên dưới |
|31 BE-ABSENCE-01 đề nghị3–4: Coach/finalizer/correction, lịch sử và contract | Đã sửa/đồng bộ và PASS local; chưa triển khai cloud | QA QR-WINDOW-REPLAY-CORRECTION, CENTER-ROOM-NOTICE, EMERGENCY-MOVE-NOTICE; không coi thiếu hồ sơ gốc là đã kiểm độc lập run CA8 |
|Owner02/11: room theo ảnh, mã C01; ID số, không subject_id | Thay đổi thiết kế mới, không là lỗi của snapshot8phòng trước đó | Một danh mục20dòng ở README; contract/docs02/migration13–14; ảnh mã16dòng chỉ tham khảo quy ước mã |

**Điểm còn thiếu trong gói bằng chứng:** case ABSENCE-UNDER-12H lưu SQL quyền/credit thay vì ảnh chụp attendance ngay sau notice; ATOMIC-ROLLBACK lưu trạng thái session thay vì hai count notice/attendance. Các count/link/state này có assertion trong test PG đã PASS, nhưng không hiện đầy đủ trong stateAfterCommit của QA. Case QR lưu cả bản Absent archived và Present hiện hành nhưng query trích không có superseded_at, dễ bị đọc nhầm là trùng. Vì vậy đã đối chiếu đủ các mục ở mức tài liệu/source/assertion, **chưa có bộ response+SQL snapshot1:1 hoàn chỉnh cho mọi chi tiết**. Cần bổ sung capture từ lượt kiểm đích mới nếu muốn bàn giao đủ ảnh chụp; không sửa dữ liệu hay viết lại quan sát cũ để lấp chỗ thiếu.

**Việc tiếp:** hoàn thiện capture nêu trên khi kiểm đích tiếp theo; Admin-null/số11 và CSV gốc chỉ kết luận đúng phạm vi đã có. Cloud migration/deploy, FE browser/email/gateway và CRUD ngoài matrix vẫn theo Active. Không chạy lại Maven chỉ vì lần chỉnh tài liệu này; kết quả BE giữ đúng lượt nghiệm thu đã ghi.

## Nghiệm thu schema demo/Coach — 29/10/2026

**Source/môi trường:** working tree trên `b59cf51ca1972e17b5337e169ee93d398047ea93`; hash SQL/BE/test/runner trong `demoSchemaCoachReview` của [QA duy nhất](../artifacts/qa/latest.json). Maven chạy bản sao source hiện tại không.env; PostgreSQL17 riêng `127.0.0.1:55440`. Không gọi Supabase/Render/email/gateway.

| Điều kiện nghiệm thu | Kết quả local |
|---|---|
| Account demo dùng mật khẩu Owner đã chốt, không bị ép đổi mật khẩu | PASS cả33account của mỗi schema qua HTTP login →`/auth/me`→API đúng role→logout; kiểm role và trạng thái khóa sau commit |
| Root sửa bộ mật khẩu ngẫu nhiên v1 nhưng không reset mãi | PASS sửa đúng33email, bỏ first-login/khóa cũ; giữ account ngoài danh sách, ID/chứng từ/attendance; replay giữ mật khẩu/khóa/profile/giá/quyền đã sửa sau đó |
| `database/` dựng lại danh sách và liên kết | PASS ID số độc lập từ1, PK subtype giữ user ID;9môn/8phòng; Normal12buổi hợp lệ; Paid/Pending/credit/attendance và chống trùng tài nguyên; FK ngoài phạm vi/phòng trùng làm rollback |
| Coach không được tự gán chuyên môn | PASS tạo và hai đường đổi role; chuyên môn trống khi chưa chọn; chặn nhận đăng ký/tạo lịch dạy và không để lại dữ liệu sau commit; `specializationIds` chỉ cấp đúng môn đã chọn |
| Runner PostgreSQL và regression | PASS bootstrap minimal không có account/demo grants; migration/replay/snapshot/FK guard; workflow HTTP đúng response và SQL state sau commit |

**Kết quả:** clean331run =303pass/28skip/0failure/0error; test đích40/40. PostgreSQL:11nhóm SQL,2case HTTP demo (33account mỗi schema),20workflow HTTP đều PASS. Skip không là pass; hai case demo PG đã chạy riêng. Lần PG đầu vướng timezone JVM alias và fixture Clock27/10 so với wall-clock DB29/10; sửa setup bằng timezone chuẩn và bỏ riêng hai date guard trong DB minimal local, giữ guard role/phòng/sĩ số/FK và kiểm Clock ở service. Hai SQL bàn giao vẫn giữ date guard gốc; không đổi expected để xanh.

**Bước tiếp:** Owner review và chạy toàn bộ đúng một file SQL trên Supabase, rồi báo kết quả. Bản root giữ dữ liệu; bản `database/` thay dữ liệu SCMS bằng bộ tham chiếu đã duyệt. Nghiệm thu local này không nâng trạng thái cloud/FE/gateway ở Active.

## Nghiệm thu API có phiên — 28/10/2026

**Môi trường:** PostgreSQL17 riêng `127.0.0.1:55440`, DB mới/fixture mới; checkout không.env, Clock `Asia/Ho_Chi_Minh`. Source kiểm `cc7847f7238ce3084ee74be8373236d52ec55756` đã chứa code/test D01/D09 và các sửa review; main local chưa push. Hash test, actor/fixture/expected/API/response và SQL state sau commit nằm trong QA. Không thêm migration hoặc ghi cloud.

| Điều kiện nghiệm thu | Kết quả mới | Bằng chứng/giới hạn |
|---|---|---|
| D01: hồ sơ đúng người/7field, null/validation, race | PASS local | Body không chọn người/role/center; giữ đủ2PATCH field khác nhau dưới lock; User+Member rollback; ngày theo Clock; Inactive403; first-login đổi mật khẩu rồi đọc được |
| D09: directory thật OSIV=false | PASS local | Member/Manager/Admin scope đúng; Admin null center chạy PG; contact theo MANAGE_USERS; query count không tăng theo Coach; Inactive403; invalid Member thiếu center chỉ kiểm H2 vì PG guard cấm fixture |
| D02/D03/D10/D16: tiền và quyền | PASS các case local đã ghi | Pending0credit; Paid hợp lệ12; giữ giá cũ; Return không Paid; IPN sai/chậm/retry/race; Cash race; Refund/CashCorrection giữ chứng từ; admission dùng chung tải Coach đa môn |
| D04/D05/D07: lịch, owner, credit | PASS các case local đã ghi | Consent/Primary Coach; sai role/owner/center; biên12h; notice; idempotency conflict; book/cancel đồng thời và hai slot cùng balance |
| D06/D08/D12/D14: sự cố và hạn | PASS các case local đã ghi | Dời phòng/cấp cứu, thay Coach có consent/retry không đổi Primary Coach; union nghỉ, preview stale/blocked/xuyên center, restore3+1credit, end ca đúng deadline; Outdoor khóa ca3/4 |
| D13/D18: báo cáo | PASS các case local đã ghi | Kỳ thu/hoàn; người khác lượt; Coach có VIEW_REPORTS vẫn403; Admin thiếu quyền/Manager Inactive403; Manager center2 không đọc tiền center1 |
| D15/D17: Normal/attendance/QR | PASS các case local đã ghi | [start,end), TTL/replay; correction;13record/12buổi hợp lệ; hai Member tranh chỗ cuối chỉ1Registered/1Waitlisted |
| D11/D19: môi trường và bằng chứng | PASS local | Migration replay/snapshot/FK guard; `/auth/me` trước nhóm; response+DB read sau commit; không cloud config/seed/live mutation |

**Kết quả kiểm:** clean326run =299pass/27skip/0lỗi; PostgreSQL HTTP20/20 nhóm (19workflow+1contract); matrix52PASS/2BLOCKED/1NOT_RUN, không có FAIL trong lượt cuối. Contract offline kiểm response local với snapshot không lệch type/property cơ bản; chưa là validator đầy đủ format/nullable/range/error. Lỗi fixture join_date và Member thiếu center được ghi riêng, không tính là pass nghiệp vụ. Skip không là pass và nhánh chưa chạy không được nâng theo count.

**Review và merge D01/D09:** giữ nguyên `053a5f2`/`1dabce5` qua merge `dda99ff` trên nhánh tích hợp, sau kiểm đạt fast-forward main local. Đã tái hiện/sửa race PATCH làm mất field và phiên Inactive vẫn đọc directory/contact; bổ sung Active Member cho self-profile. Test gốc coi28/10 là tương lai bị sai theo ngày chạy: cố định Clock fixture, không đổi rule. Giữ7field và first-login policy; không sửa query Admin-null vì PG đã chạy đúng. Sửa matcher contract để ưu tiên `/me` trước `/{id}`, khai báo schema PATCH/unknown-key đúng runtime.

**Dọn branch:** đã xóa7task branch cũ ở local/remote, xóa local `lan-anh` (remote đã vắng) và `merge/advanced-phases`; giữ LA/tai. Remote `feat/member-profile-coach-directory` còn giữ vì origin/main chưa nhận tích hợp. Danh sách và bằng chứng review ở `integrationReview` trong QA; không xóa worktree/log/backup để làm sạch hình thức.

**Ví dụ local đúng:** giá catalog đổi3.000.000→3.100.000, đơn cũ vẫn3.000.000 và Paid/12credit; IPN đúng mốc5phút ghi Paid cần đối soát nhưng quyền Cancelled/0credit; hai hủy đồng thời trả200/400 và chỉ hoàn1credit. Fixture ngày tương lai dùng Clock test, không là giao dịch Render.

## Bằng chứng giữ lại — 27/10/2026

Đây là bằng chứng cũ, **không chạy lại trong đợt nghiệm thu có phiên này**. App source: `693f732`; Live đã quan sát: `4d372e5`. Không dùng bảng này thay kết quả mới phía trên hoặc suy SHA Live hiện tại.

| Phạm vi | Kết quả lưu | Giới hạn |
|---|---|---|
| BE regression | 305run:278pass/27skip/0lỗi | Skip có điều kiện, không phải pass |
| PostgreSQL HTTP | 10/10:9 workflow nghiệp vụ+1OpenAPI regression | Fixture và Clock local, không cloud mutation |
| Contract | 149 exchange/36 operations;114positive/201body checks không lệch cơ bản | Không phải validator đầy đủ nullable/format/required/range hoặc mọi nhánh lỗi |
| Source/OpenAPI | 185 operations/149paths/138schemas; route parser khớp | Có aliases; không chứng minh mọi handler/nghiệp vụ đã chạy |
| GET Render | 3public200,71protected401 | Chỉ public/login gate; authenticated business matrix chưa chạy |
| Health Render | db UP,mail DOWN | `/api/health` chỉ liveness/identity; aggregatehealth503 |
| Production upgrade | Bundle01e31ca6… đã commit,50 bảng/9 môn,giữ dữ liệu gốc7bảng | Quyền thực thi chỉ bundle cụ thể; không suy quyền seed/test giao dịch |

Bằng chứng khi cần: [QA duy nhất](../artifacts/qa/latest.json). Bộ FE chỉ gồm [contract](01-API-CONTRACT-FE-BE.md) và [OpenAPI](../artifacts/fe-handoff/openapi.json). Raw run cuối còn ở `artifacts/runs/phases/2026-10-27-verified` trên máy này, ignored và không mặc định có trên máy khác.

27skip của lần clean gồm19baseline live/data/PG,6communication và2clone opt-in. Communication12case và clone2case đã chạy riêng ở source trước; xem QA theo SHA, không gọi đó là lần chạy mới của source hiện tại.

## HISTORY

| Ngày | Nhóm hoàn tất | Dấu vết |
|---|---|---|
| 06/11/2026 | Owner gửi SQL16 Supabase PASS/tất cả bộ đếm0; sau đó hoàn tất A local và đánh giá bàn giao audit từng phần | Lý do bỏ/phạm vi và không xóa dòng thực tế ghi LAN-1; main bỏ suy luận dependency theo thời gian, guard D16 giữ. Đích35/clean412/PG A6+workflow22 theo Active/cashCorrectionIndependence QA. BE-01/04/05 chỉ bàn giao phần có chứng cứ, chưa deploy/FE/Render hoặc đóng toàn audit |
| 06/11/2026 | Xử lý conflict main/tai và phần kèm theo Owner duyệt | Giữ guard D15 của tai, sửa fixture test main, không nới quyền; bảo toàn Google/avatar, dọn docs cũ/lặp và snapshot OpenAPI. Kiểm đích/clean/PG đạt theo Active/rbacMainMergeIntegration QA; chưa ký Google live/FE/Render hoặc migration Google cloud |
| 06/11/2026 | Hoàn tất B local, đóng ghi gói chung cũ và bàn giao migration16; dừng chờ Owner Supabase trước A | BE test đích/clean và PG workflow sau16 đạt; SQL guard/purge/replay/rollback đạt theo Active/legacyMembershipRetirement QA. Chưa chạy cloud hoặc deploy; một file SQL và hướng dẫn ở docs02, A chưa sửa |
| 06/11/2026 | Làm rõ ví dụ CashCorrection, dẫn flow Advanced có sẵn và ghi chỉ đạo loại bỏ dữ liệu gói chung cũ | Docs03/D03/docs01/database README/bàn giao thống nhất thuật ngữ và kế hoạch phân loại FK trước xóa; chỉ tài liệu, chưa sửa A/B hoặc truy vấn/xóa DB đích, không gán kết quả test mới |
| 06/11/2026 | Hoàn tất0.1-C và chuyển ghi chú FE sang bàn giao RBAC lần1 theo Owner | Chặn giá Normal lỗi ở đăng ký/promotion, giữ Paid/chứng từ cũ; test đích/clean đạt theo Active/normalPositivePriceGuard QA. A/B/D chỉ giải thích cụ thể để duyệt; không sửa FE/DB hoặc deploy |
| 06/11/2026 | Sửa mục0.1 theo Owner: Normal không cần gói chung, giữ lịch sử và luồng thu tiền | Local test đích/clean đạt theo Active/normalMembershipIndependence QA; không migration/FE/deploy. Ghi câu chữ FE và phát hiện0.1-A–D chờ duyệt; ưu tiên đề xuất chặn nhánh giá0 |
| 06/11/2026 | Nhận bằng chứng Owner cho mục0.1: Normal lớp19 bị400 PACKAGE_EXPIRED; catalog lớp17 trả404 | Docs03 tách lỗi guard BE khỏi404 chưa đủ dữ kiện; không lưu PII ảnh, chưa sửa code hoặc kiểm runtime mới; chờ chốt hướng sửa |
| 06/11/2026 | Gọn ghi chú mục0 và rà nguyên nhân gói hội viên chặn đăng ký khóa ở mục0.1 | Docs03 ghi rule cũ/hiện hành/guard BE và yêu cầu làm rõ ca FE; chỉ tài liệu, không thay code/test/DB hoặc nâng kết quả local trước thành nghiệm thu live |
| 05/11/2026 | Hoàn tất mục0 BE local theo Owner: Member đọc kết quả cá nhân, rà năm role resolution, ghi yêu cầu FE | Test đích và clean đạt theo Active/rbacIssue0 trong QA; khóa owner tại BE, giữ policy tiền, không sửa FE/DB hoặc deploy. FE/Render chưa nghiệm thu; BE-01 chờ lượt riêng |
| 04/11/2026 | Lập checklist Access Denied/RBAC và rà tĩnh mục0 theo hai403 Member Owner cung cấp | Đối chiếu yêu cầu cũ, D10/D15/D16, contract, schema và source; ghi phát hiện/hướng sửa chờ chốt ở docs03. Chỉ tài liệu, chưa sửa quyền hoặc kiểm runtime; tiếp tục mục0 trước BE-01 |
| 04/11/2026 | Owner chốt bản bàn giao OTP dùng response login làm nguồn danh tính/quyền | Giữ bằng chứng userId42/memberId42/role Member/isFirstLogin=false và authorities từ response login trên FE; đồng bộ OTP/Active/QA, chỉ sửa tài liệu |
| 04/11/2026 | Owner bổ sung verify-registration POST204/OPTIONS200, users42 Active và đăng nhập vào dashboard Member42 | PASS phạm vi nhận OTP/xác minh/Active/login theo bằng chứng Owner; GET đang mở là notifications/me200, chưa có OTP-used/members riêng. Cập nhật bản OTP, Active và otpRegistrationOwnerAcceptance; chỉ docs/QA, không thêm lượt BE/cloud hoặc ghi PII |
| 04/11/2026 | Bổ sung quy tắc hướng dẫn thao tác thủ công vào AGENTS theo Owner | Từng bước dựa trên giao diện đã kiểm; nêu kết quả đúng để đối chiếu và bước kiểm khi khác. Chỉ sửa tài liệu, không thay code/runtime/DB |
| 04/11/2026 | Owner xác nhận OTP vào Gmail và đăng ký thành công trên FE; hoàn tất bàn giao OTP theo5mục | PASS ca thành công theo ảnh Brevo/Gmail23:08 và FE,3event của1email. otpRegistrationOwnerAcceptance trong QA; SHA runtime chưa thu, source review3a2adaf1b6ac11bbe55cdc7e41931a4a5e31530a. Không lưu OTP/PII; chưa kiểm SQL hoặc ca lỗi/biên; lượt này chỉ docs/QA, không chạy lại BE |
| 04/11/2026 | Theo log Owner22:27, bổ sung chẩn đoán SMTP an toàn và hoàn tất ghi chú bàn giao OTP; giữ provider/port/API/DB | PASS14/14 test đích; clean377run=349pass/28skip/0failure/0error, Java17/H2/local HTTP trên bản sao working tree không.env nền cea7fbe856e9aed139cffbacd3d7da714ab0c1aa. Hash source/test/config và XML/raw run ở otpSmtpDiagnostics trong QA. Chưa push/deploy hoặc gửi mail cloud; Owner deploy rồi lấy SMTP_SEND_ để xác định nguyên nhân |
| 04/11/2026 | Theo Owner, tạo file OTP04/11 chứa hướng dẫn tạm và chuyển AGENTS thành quy tắc bàn giao chung | Agent sau thay hướng dẫn tạm bằng nội dung thực tế rồi xóa mục hướng dẫn, giữ file; chưa nghiệm thu OTP mới, chỉ sửa docs/evidence |
| 04/11/2026 | Owner chốt giữ toàn bộ QueryDR; đổi tên bàn giao thành VNPay riêng, thêm quy tắc bàn giao OTP đăng ký trong AGENTS | Giữ code/endpoint, bảng, migration15 và env vì metadata/kênh dự phòng; không áp dụng đề xuất tắt/gỡ. Chưa có OTP trong bản cũ để chuyển; chỉ sửa docs/evidence |
| 04/11/2026 | Sau lưu IPN Terminal, Owner gửi invoice40 Paid/ref15725858/reconciliationRequired=false; FE hiện thành công ngay khi Return | PASS ghi nhận tiền ca sandbox theo response Owner; không nâng QueryDR/trace callback/quyền học thành PASS. Cập nhật bàn giao04/11, chưa thay BE/DB hoặc thêm relay |
| 04/11/2026 | Owner mở được Terminal, ảnh hiển thị IPN BE/GET/HMACSHA512 và báo đã bấm Hoàn thành | Chờ kiểm lưu cấu hình/callback thật và ownInvoice sau commit; health200 chỉ chứng minh app đang UP. Không thay BE/DB hoặc triển khai relay |
| 04/11/2026 | Owner yêu cầu so sánh phương án trước khi chọn relay; tìm được hướng tự cấu hình IPN sandbox tại Terminal từ SDK cộng đồng | Public TerminalEdit chuyển tới login đã kiểm; quyền tài khoản/callback thật chưa kiểm. So sánh ở docs02; chưa thêm relay hoặc thay BE/DB |
| 04/11/2026 | Owner cung cấp probe runtime20:33: DNS VNPay OK, TCP443 timeout15s; đối chứng TCP/HTTPS thành công | `vnpayRuntimeNetworkEvidence` trong QA; local HEAD405/0,39s chỉ chứng minh đường local. Chưa Paid; relay mới là đề xuất, chờ chọn phương án |
| 04/11/2026 | Xác minh nguyên nhân không có VNPAY_NET: commit probe chưa push/merge; đã push nhánh tai | Remote tai `cea7fbe`; main/Render vẫn `4e6a88e`, class probe không tồn tại ở runtime. Chờ Owner merge; chưa deploy/probe/gateway test mới |
| 04/11/2026 | Owner cung cấp CONNECT_TIMEOUT; thêm probe DNS/TCP/TLS runtime một lần, mặc định tắt và cấu hình timeout có giới hạn | Commit `ea15e9712d3ea07e2134888e427a321b12ce9c0f`; clean368run=340pass/28skip PASS local; ký/ghi Paid/schema giữ nguyên. Probe Render NOT_RUN, chờ publish/cờ bật |
| 04/11/2026 | Phân loại lỗi QueryDR an toàn sau khi Owner cung cấp POST37 QUERY_UNAVAILABLE; kiểm probe TLS Java/AIA | Client8/8 và clean360run=332pass/28skip PASS local; không thay chữ ký/quyền/timeout, chưa publish patch. Kết quả sau JVM option trên Render còn chờ |
| 04/11/2026 | Rút gọn hướng dẫn sandbox, giữ settlement=true theo bước Owner đang làm; chuyển bàn giao VNPay sang04/11 | Owner xác nhận SQL15; health/OpenAPI/CORS PASS trên Render; POST có phiên đơn37 còn chờ. Không sửa BE hoặc ghi cloud |
| 04/11/2026 | Owner duyệt QueryDR: thêm POST verify, bảng pay attempt/migration15 và FE auto POST→GET; Return vẫn không ghi Paid | PASS local theo mục QueryDR/QA; chưa cloud/deploy/live; giữ riêng thay đổi email đồng thời |
| 04/11/2026 | Theo Owner đã xác minh sender/thêm env Brevo SMTP2525: tách MAIL_FROM_EMAIL khỏi SMTP login, giữ fallback và luồng OTP sau commit | PASS local5/5 test đích; clean357run=329pass/28skip/0failure/0error (XML), Java17/H2/local HTTP trên bản sao working tree không.env, Clock timezone Asia/Ho_Chi_Minh, base ec2325df07df053257df09698857b27e36489820. Source mail/config/test khớp SHA256 bản kiểm; skip không là pass. Chưa push/deploy hoặc kiểm mail thật từ Render |
| 04/11/2026 | Kiểm CORS/source deploy/HMAC và merchant; Owner cung cấp hai mã gateway00/merchant thành công trước hạn nhưng SCMS Expired. Xác minh redirect localhost, hướng dẫn kiểm IPN và chuẩn bị yêu cầu hỗ trợ | vnpayCorsDiagnosticReview trong QA; PASS HTTP/offline đúng phạm vi, thành công gateway theo Owner; redirect FE FAIL và IPN/Paid chưa nghiệm thu. Agent không sửa BE/deploy/DB, gọi gateway hoặc gửi thư hộ |
| 02/11/2026 | Owner xác nhận cả hai bundle hoàn tất trên Supabase, gửi đúng hai index attendance; tổng hợp một file FE có20mã room,7giới hạn lớp và workflow báo vắng | ownerReportedSupabaseMigrationCompletion trong QA; chỉ cập nhật tài liệu/bằng chứng Owner, chưa xác nhận deploy hoặc runtime Render/FE |
| 02/11/2026 | Owner duyệt7giới hạn lớp; bổ sung bước có guard/audit trong transaction0211-1, kiểm đủ marker ở0212-2; giữ module room chung không tự resize | ownerClassCapacityApproval/rootBundlePackaging trong QA; kiểm SQL local trước khi Owner chạy Supabase |
| 02/11/2026 | Bổ sung DETAIL cho SCHEDULE_REVIEW: chỉ rõ lớp/buổi, sức chứa tối đa, số chỗ đã giữ, sân đích và reasons; giữ nguyên guard | SQL local/schema/root13nhóm PASS; QA scheduleReviewDiagnosticSupport; chưa có DETAIL cloud hoặc thay đổi lớp được Owner chốt |
| 02/11/2026 | Sửa CUSTOM_ROOM với inventory Owner9phòng: nhận tên20phòng duyệt, map TEN02 đúng sân, thêm DETAIL phòng lạ và đồng bộ0211-1/schema/nguồn13 | roomCatalogCompatibilityFix/rootBundlePackaging trong QA; SQL local PASS, không đổi BE hoặc ghi cloud; chờ Owner chạy lại |
| 02/11/2026 | Đóng gói hai file SQL tạm ở root theo Owner: preflight+13, kiểm thứ tự+14+postflight | rootBundlePackaging trong QA; kiểm PostgreSQL local/rollback/replay PASS; chưa chạy Supabase, không đổi BE hoặc bỏ guard |
| 02/11/2026 | Review bàn giao1:1 với hai báo cáo gốc; sửa nhầm trạng thái lịch sử/hiện tại, bổ sung trace và ghi thiếu capture SQL trong QA | Bảng report-trace; chỉ docs/ghi chú QA, không có lượt BE/cloud mới |
| 02/11/2026 | Hoàn tất room_code/20room, migration13–14, báo vắng+Absent/archived history; làm rõ report/auth/contract và bàn giao | Nghiệm thu02/11 và roomAbsenceReview trong QA; clean/PG/guard/replay/schema PASS local; chưa cloud migration/deploy |
| 02/11/2026 | Review tĩnh hai báo cáo29–31/10 với quyết định/contract/source/schema; xác định notice-only lệch Owner được báo cáo dẫn, giải thích chỉ số/scope và đề xuất20room theo ảnh | Ghi nhận ở Active; chưa sửa BE/SQL, chưa chạy test hoặc xác minh cloud; bảng room chờ Owner chốt |
| 02/11/2026 | Chuẩn hóa context session và README database: phân biệt bảo toàn/dựng lại, giải thích classes/quyền học, bổ sung migration08–12, bỏ mô tả default credit lịch sử dễ nhầm | Chỉ sửa tài liệu; kiểm liên kết/nội dung với SQL trong repo, không chạy BE hoặc ghi cloud |
| 29/10/2026 | Chốt hai biến thể schema, sửa mật khẩu/first-login demo một lần; bỏ mặc định chuyên môn Coach và đổi fixture Yoga; kiểm schema/HTTP/JPA/clean regression, sửa runner minimal | Nghiệm thu schema demo/Coach và QA; chỉ local, chưa ghi Supabase hoặc deploy |
| 28/10/2026 | Review hai commit D01/D09, tái hiện lỗi Clock/race/Inactive, sửa và tích hợp main local; PG login/SQL state/N+1/contract; dọn branch merged và lan-anh | Source và kết quả mới phía trên/QA; chưa push main hoặc nghiệm thu Render; remote feature giữ tới khi publish |
| 28/10/2026 | Lập matrix API có phiên và SQL state sau commit; thêm role/center/biên/race HTTP PostgreSQL, collector QA; làm rõ requestId UUID | Local mới theo mục nghiệm thu; Render/gateway BLOCKED, nhánh HTTP còn thiếu ở Active; không sửa BE hoặc ghi cloud |
| 27/10/2026 | Chuẩn hóa bàn giao: tách trách nhiệm5docs, FE1contract+1 schema, gom QA; bỏ inventory/schema MD và trace trùng | Không sửa BE/DB/deploy; kết quả kiểm cũ giữ theo SHA |
| 27/10/2026 | Audit và sửa metadata Swagger:24status201/204,6 schema name riêng,LocalTime string; thêm readback/contract regression | App693f732/Live4d372e5; QA |
| 27/10/2026 | Nâng cấp đúng production188f6765… sau backup mã hóa/restore proof và phê duyệt riêng | Bundle production giữ nguyên hash; docs02/QA |
| 27/10/2026 | Hoàn tất hai phase BE; merge communication, sửa CI và runtime identity | Các lần test có phạm vi/SHA riêng trong QA và Git |

Chi tiết phân tích, log, snapshot before/after và checklist cũ nằm trong Git tại `a4f3cd0` (trước chuẩn hóa). Ví dụ `git show a4f3cd0:docs/00-MASTER-CHECKLIST.md`. Chỉ tra khi cần một sự kiện cụ thể; không đọc mặc định hoặc khôi phục nguyên trạng vào docs active.
