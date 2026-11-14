# SCMS — Tiến độ và nghiệm thu

## Active

**ADV-DATA-01 — phát hành lớp Advanced riêng,14/11:** Owner báo migration25 PASS, cleanup đã xóa9template IDs1–9; cho phép merge tai→main và kiểm Swagger bằng Playwright sau deploy. Đang kiểm candidate riêng trước merge; chưa kết luận Render hoặc FE. Chi tiết flow/API ở [bàn giao Advanced](reference/SCMS-BAN-GIAO-FE-ADVANCED-2026-11-14.md), DB ở [README](../database/README.md#advanced-class-review), bằng chứng ở QA `advancedRegistrationClassRelease20261114`. Không chạy thêm SQL/reset cloud hoặc sửa/push FE.


**Advanced14/11 — PASS RENDER API ĐỀ XUẤT/ACCEPT/HỦY SỚM:** Owner mở ghi demo; ca quyền học6/Member8/Coach26: đề xuất1 Pending/12credit → Accept buổi174 Scheduled/11 → hủy sớm Cancelled/12, hạn không đổi. GET sau commit, retry, sai owner/role, audit/thông báo đạt trên runtime `0d7ee460a71eee49deb022d67fcd90c45a2cd5ae`, cùng DB. QA `advancedConsentLiveAcceptance20261114`; bằng chứng local và đọc/negative trước đó giữ nguyên. FE/browser, biên thời gian/race live, đơn mới/provider chưa nghiệm thu. **Còn mở:** FE tích hợp theo [bàn giao](reference/SCMS-BAN-GIAO-FE-ADVANCED-2026-11-14.md); Owner duyệt [BUNDLE-01](../database/README.md#advanced-bundle-proposal) trước chuẩn bị/kiểm local, không tự reset Supabase. Bundle/source BE chưa đổi; đã bỏ dự toán FE theo Owner.

**Phát hành lại Session1–3 — ĐÃ PHÁT HÀNH, PASS SCOPED RENDER:** PR https://github.com/Aditerx/SWP391/pull/27 đã merge sau hai CI candidate PASS; CI main PASS. Render lên đúng `48a94559daea388b43a2f2c24cec03e3c37cd8a9`, cùng DB fingerprint baseline; smoke có phiên tám account DEMO đủ năm role PASS, gồm đọc scope và negative RBAC/room đã chứng minh rollback local. Không positive mutation/race/thay quyền cloud; **không rollback trong lượt mới này**. GET OpenAPI đầu lại timeout35giây; attempt thứ hai thành công trong retry hữu hạn, giữ lỗi đầu trong QA. Không retry login/mutation; chưa xác định nguyên nhân cold-start/hạ tầng. Main/runtime/source và bằng chứng chuẩn tại QA `session123ReRelease20261113`; không nâng thành full API/FE/provider PASS.

Local Java17/H2 clean751run=717pass/34skip/0failure/error và package PASS; PostgreSQL17.11 loopback234case/0skip/0failure/error trên11DB regression mới. Matrix method/path×actor×precondition×response×commit/environment nằm tại QA; có dữ liệu/audit sau commit, biên, retry/race và timeout container thật.12test smoke transport PASS, retry chỉ GET readiness/schema với3attempt/35s request/110s deadline, giữ lỗi đầu; login/mutation một lần. Không sửa BE hoặc expected nghiệp vụ; baseline trước phát hành đã login/auth-me/logout lại và còn một Admin Active đủ MU+MP.

Source worktree được kiểm theo hash byte riêng; khác EOL với bản cũ đã đối chiếu không có khác biệt nội dung, không normalize hàng loạt. OpenAPI chỉ khôi phục servers metadata canonical đã kiểm; contract/path/components không đổi, checker PASS OFFLINE đúng giới hạn. Giữ original workspace/bundle/holiday/note/ảnh/deletion và bằng chứng timeout/rollback của lượt đầu. Không cloud SQL/SQL16/provider/FE/ZIP. Demo **SKIPPED_BY_OWNER**; FE/browser/provider và cases ngoài matrix **NOT_RUN**. **Bước tiếp:** FE review/tích hợp trong phạm vi riêng khi được mở; không cần phát hành lại chỉ vì commit evidence.

**Lượt phát hành ĐẦU TIÊN Session1–3 ngày13/11 — ĐÃ HOÀN NGUYÊN, BASELINE LIVE ĐÃ KIỂM CÓ PHIÊN:** PR [#26](https://github.com/Aditerx/SWP391/pull/26) đã merge sau predeploy local/PG và hai CI candidate PASS; CI main cũng PASS. Render từng lên đúng `a7b7d2ba0ceea38ba4e720e83a7a2fe14927cb03`, cùng DB. Smoke bị TimeoutError35giây ở GET `/v3/api-docs` sau health/DB UP, trước login hoặc mutation; **authenticated business trên bản mới NOT_RUN**, không kết luận API nghiệp vụ lỗi. Đã đẩy forward revert ngay theo Owner, commit `f0bb3805b2dda8965f1f40322df33554b406887b` có cây code đúng baseline `1dcf5cbf342d3d5b678c47048783dcfb10290eaf`, không force-push/reset DB. Rollback CI PASS; Render Live đúng SHA hoàn nguyên/cùng DB và smoke có phiên tám account DEMO đủ năm role PASS sau schema sẵn sàng. Bản mới không được giữ trên main/Render; source tích hợp và bản sửa smoke vẫn ở nhánh release/local.

Lần đọc OpenAPI chẩn đoán tiếp theo của đợt mới trả200/0,67giây; lần đọc schema đầu của chính baseline hoàn nguyên cũng timeout, rồi lượt kiểm lại đạt. Đây là căn cứ nghi delay khởi tạo/schema cold-start, **chưa chứng minh lỗi nghiệp vụ hoặc nguyên nhân hạ tầng cụ thể**. Smoke đã được chuẩn bị bounded retry cho GET readiness/schema; không retry login/mutation, không sửa expected API để xanh. Tại checkpoint lượt đầu chưa triển khai lại sau rollback; lượt mới được ghi ở đầu Active. QA `session123Release20261113` giữ nguyên cả smoke FAIL/timeout đầu, kết quả follow-up và baseline PASS; không biến retry thành bằng chứng bản mới đã được nghiệm thu. Owner review source/script trước lượt phát hành tiếp theo; FE/browser/provider và full API matrix vẫn NOT_RUN.

**Checkpoint predeploy của lượt phát hành ĐẦU TIÊN Session1–3 — kết quả lịch sử:** yêu cầu mới cho phép tích hợp main, kiểm trước deploy rồi merge/phát hành và smoke; nếu bản mới thất bại phải hoàn nguyên code về baseline. Lượt này thay giới hạn chưa merge/deploy ở các checkpoint Session cũ bên dưới, không mở quyền reset/migration/seed Supabase, SQL16, provider thật hoặc đổi bundle/kịch bản/ảnh. Snapshot ban đầu và bản gộp cô lập được giữ riêng, không mất diff/deletion của Owner. Trạng thái build/SHA/kết quả/rollback chuẩn ở QA `session123Release20261113`; không coi kết quả source cũ là nghiệm thu bản gộp.

Main mới tích hợp là `1dcf5cbf342d3d5b678c47048783dcfb10290eaf`, cũng là SHA Live baseline đã xác minh trước phát hành; DB fingerprint giữ tại QA. Conflict thực tế ở docs00/QA đã giữ cả hai nguồn/bằng chứng, không có semantic QA overwrite; docs01/OpenAPI/handler tự gộp, đã rà giữ GoogleAuthException và409/constraint mới. OpenAPI sinh lại từ app tích hợp. Java17.0.12/H2/PostgreSQL17.11 loopback/source không.env: clean verify751run=717pass/34skip/0failure/error và package đạt; PostgreSQL204cases0skip đạt qua các nhóm nguồn hiện hành, có HTTP/auth-me/state/audit/race theo từng test. Baseline Render tám account DEMO đủ năm role đọc/auth-me/logout đạt; giữ một Admin Active đủ hai quyền, chưa đổi quyền hay mutation nghiệp vụ cloud.

Lỗi setup trước predeploy: JPA slice DB tools thiếu ObjectMapper, fixture chat thiếu migration google_sub; đã sửa harness/provision DB local mới. Ca close retry từng so nano giây response đầu với micro giây PostgreSQL; đã giải thích và kiểm response đầu trong độ chính xác DB, retry đúng timestamp đã commit/DB không đổi. Không đổi expected idempotency hay BE runtime để che lỗi. Hai opt-in harness đã được compile/kiểm PG sau sửa; CI sẽ clean verify toàn bộ bản commit cuối trước merge. Bước tiếp push nhánh release cô lập/CI, merge main khi đạt, theo dõi đúng Live SHA và smoke; nếu fail hoàn nguyên code về baseline, không reset DB hoặc force-push. FE/browser/provider thật vẫn NOT_RUN.

**Session3 mở lại13/11 theo Owner — HOÀN TẤT3.1→3.2→3.3 TRONG PHẠM VI BE LOCAL/BÀN GIAO:** yêu cầu mới thay ưu tiên tạm dừng12/11; đã làm lần lượt từng mục, demo từng mục `SKIPPED_BY_OWNER`. Giữ kết quả/bàn giao Session1–2 và mọi diff/deletion/bundle/holiday hiện có. Không tích hợp FE/cloud, deploy/merge/push/reset/provider/SQL16/ZIP. Các ghi chú tạm dừng và fixture/cửa giờ12/11 bên dưới là checkpoint lịch sử, không là precondition hiện tại; không sửa Clock/bundle guard/kịch bản/ảnh demo.

**Session3.3 matrix/readiness — PASS LOCAL CHECKED SCOPE13/11:** đã lấp gap approve Refund năm role/authority/center, notification own/direct/recipient/read-all/dedupe/retry và timeout container local thật; kiểm lại RBAC và workflow tiền/quyền hiện hành trên PG. [Matrix theo rủi ro](03-TODO.md#session33-risk-matrix-20261113), [bản readiness](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-MATRIX-READINESS-2026-11-13.md), [KIEM-TRA đã hoàn thiện](../SCMS-CAP-NHAT/KIEM-TRA.md). Không sửa BE runtime/schema trong3.3; helper checker là maintenance và tests/fixtures bổ sung, không thay business expected.

QA `session33Readiness20261113`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; Windows/Java17.0.12/H2/PostgreSQL17.11 loopback với4DB synthetic mới. Đích9/9; PG66/66 (RBAC35, notification8, Refund approval1, workflow22), **clean cuối713run=682pass/31skip/0failure/0error**. Source main/test/pom bản clean và PG khớp working tree; main không đổi từ3.2. Login/auth-me, response và DB/audit sau commit; negative Refund/foreign notification không mutation, approved chưa Completed, read-state không ảnh hưởng recipient khác. Timeout local không đổi business Clock, không là cookie/proxy/browser deploy PASS. Hai lỗi fixture đầu là H2 thiếu unique dedupe giống PG và SQL readback sai khóa, đã sửa fixture/kiểm lại không đổi expected. S1 source cũ có thay đổi ở S2 nên không gọi PG lịch sử là lượt mới; current clean có hồi quy S1–2, reuse R2/shared đúng business/test hash và giới hạn source trong QA.

**Checker tổng thể: PASS OFFLINE, blocker cũ đã xử lý.** Tái hiện FileNotFoundError vì helper trỏ schema root bị xóa; fingerprint mới ghi explicit absence, giữ deletion và lịch sử hash, file bắt buộc thiếu vẫn lỗi. Kiểm tạm cho thấy phục hồi file legacy ở thư mục temp làm fingerprint đổi; không tạo lại file root workspace. Sau path fix, REVIEW_REQUIRED do source/hash cũ; sau đối chiếu bằng chứng mới, source/hash hiện hành khớp,210route đúng/no extra/no wire issues.52positive exchanges/82type-property checks vẫn là ví dụ lịch sử; không phải kiểm business mới/full JSON Schema hoặc full role matrix. Các ô inventory không có evidence riêng giữ NOT_RUN.

**Trạng thái cuối:** PASS các cases local đã nêu; không có FAIL/BLOCKED còn mở trong phạm vi này. FE/build/API origin/browser/Render/cloud/live smoke/provider và các cases ngoài bằng chứng **NOT_RUN**; không biến31skip suite hoặc demo thành PASS. Demo3.1/3.2/3.3 đều **SKIPPED_BY_OWNER** theo yêu cầu13/11. Kịch bản/ảnh/bundle/holiday và các deletion có sẵn giữ nguyên; Owner tự ZIP. **Bước tiếp:** Owner review bộ nguồn bàn giao, rồi mở riêng đợt tích hợp/kiểm FE hoặc live khi cần; agent chưa tích hợp, merge/push/deploy/reset hay gọi provider.

**Session3.2 error/deprecation — PASS LOCAL13/11:** đã trình [catalog19dạng trước sửa](03-TODO.md#session32-error-catalog-20261113), sửa plain conflict HTTP409/body409 và Hibernate DB constraint flush qua audit về409 generic. Giữ các envelope/code/gate khác, không chuẩn hóa toàn hệ thống. Đồng bộ25operation OpenAPI; deprecated đúng mutation shared retired400, không retire các route status/extend Advanced. [Contract](01-API-CONTRACT-FE-BE.md#error-deprecation-20261113), [bàn giao riêng](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-ERROR-DEPRECATION-2026-11-13.md).

QA `session32ErrorDeprecation20261113`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; Windows/Java17.0.12/H2/PostgreSQL17.11 loopback, source copy không.env. Đối chứng13ca có4FAIL+1ERROR đúng lỗi/mismatch đã nêu; sau sửa55/55đích và35/35PG. Clean cuối704run=673pass/31skip/0failure/0error, source/test clean khớp working tree; source main PG cuối khớp. Clean đầu vướng một expected schema cũ yêu cầu201 POST packages, đã giải thích rồi sửa sang deprecated400 theo contract retired; focused metadata PASS trước clean cuối, không đổi expected nghiệp vụ. Probe compiler constructor sai đã sửa fixture, lượt bị gián đoạn chưa có kết quả không tính PASS. Response+DB/audit rollback/retry duplicate room, version stale/consent/tiền/Refund được kiểm; Advanced no-op/status/extend hai alias và shared denied đã kiểm H2. Không migration/seed/reset/provider/cloud. **Demo3.2 SKIPPED_BY_OWNER**; FE/browser/Render NOT_RUN. Bước tiếp3.3 lấp gap theo rủi ro, xác minh checker/schema root deletion và hoàn thiện readiness/bộ bàn giao.

**Session3.1 RBAC/quản trị cuối — PASS LOCAL13/11:** PUT permissions Admin Active+MANAGE_PERMISSIONS, giữ ít nhất một Admin Active đồng thời MU+MP trên mọi PUT user/PATCH role/status và permission role dùng chung. Khóa cùng tập role theo ID trước user, recheck authority sau chờ; aliases staff/member chặn Admin. Giữ GET Admin-only, ID nguyên khối/`[]` hợp lệ/audit/refresh-thu hồi phiên.409 `LAST_ADMIN_REQUIRED` rollback dữ liệu/audit. [Contract](01-API-CONTRACT-FE-BE.md#last-administrator-20261113), [bàn giao riêng](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-RBAC-LAST-ADMIN-2026-11-13.md).

QA `session31LastAdministrator20261113`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; Windows/Java17.0.12/H2/PostgreSQL17.11 loopback DB synthetic `scms_phases_s31_20261113a`, real Clock, không.env/provider/cloud. Clean691run=660pass/31skip/0failure/0error; PG20/20, login/auth-me/response và DB/audit sau commit. Bốn race có hai request chờ khóa thật: status/status, role/role, permission/permission200+409; permission/status200+403 khi actor mất quyền trong lúc chờ; đều còn một Admin đủ quyền, audit tăng một. Lượt đích đầu có một lỗi input fixture alias thiếu field bắt buộc, đã sửa fixture giữ expected403; fixture Coach regression bổ sung Admin đủ quyền, không đổi expected chuyên môn. Metadata PUT description được kiểm trên source/app PG cuối; clean trước thay riêng metadata này, gate/business code khớp. **Demo3.1 SKIPPED_BY_OWNER**; không dựng Swagger/bundle mới. Bước tiếp3.2 trình catalog trước sửa; FE/browser/Render NOT_RUN.

**Bổ sung danh mục lễ riêng12/11 — PASS SQL LOCAL, CHƯA CHẠY SUPABASE:** Owner yêu cầu query sinh nhanh danh sách, không tác động phần khác. Đã tạo [seed_holidays_catalog_2026.sql](../database/seed_holidays_catalog_2026.sql): chèn ngày2026 còn thiếu vào `holidays` với `is_active=false`, không UPDATE/DELETE, không đổi dòng mẫu01/01/2027 hoặc ngày/flag/history đã có. Đây là danh mục mẫu, không là phục hồi chính xác danh sách Owner từng tạo; clone trước reset09/11 không có dòng holidays. Bundle hiện hành vẫn chỉ seed một ngày DEMO theo anchor; reset thay dữ liệu tự tạo không nằm trong seed. Chưa quan sát response Holiday trên FE/Render hoặc đọc DB cloud trong lượt này, nên không kết luận mọi trường hợp FE trống đều do reset.

PostgreSQL17.11 loopback/HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree: SQL fresh/replay/conflict và guard custom trigger đạt;11dòng mới đều tắt/actual_closed_until=NULL,54bảng còn lại và các sequence không thuộc holidays giữ nguyên fingerprint. Replay không thêm dòng/đẩy sequence; ngày trùng giữ cả tên/tag/status/description/lịch sử. Bằng chứng `holidayCatalogOnly20261112` trong [QA](../artifacts/qa/latest.json). Không sửa BE/canonical bundle/SYSTEM-DATA/kịch bản demo hoặc chạy lại Maven cho SQL bổ sung; không mượn count BE trước làm nghiệm thu mới. **Bước tiếp:** Owner chạy nguyên file bổ sung nếu cần danh mục; GET `/api/holidays` trả cả dòng tắt, `/active` chỉ trả ngày đang áp dụng. Muốn áp dụng đóng cửa phải đi workflow D12/D14; query này không bật nghỉ hoặc tự đổi lịch/hạn/quyền.

**Bundle dựng lại12/11 — SẴN SÀNG ĐỂ OWNER CHẠY, PASS LOCAL:** theo yêu cầu sinh lại schema cho Supabase, đã đưa bộ cùng anchor **2026-11-12 / Asia/Ho_Chi_Minh** vào [schema canonical](../database/schema_postgres.sql), [fixture](../database/bootstrap/scms_demo_scenarios.sql) và [SYSTEM-DATA](../SYSTEM-DATA.md). Giữ mô hình/giá/quyền/47account/21Class;20nguồn cấu trúc/migration nhúng không đổi, chỉ fixture/ngày và metadata anchor được tái sinh. Hai file `note-script-demo.md`/`SCMS-SCRIPT-DEMO-BE.md` giữ nguyên từng byte; note private chỉ điền thông tin đăng nhập8account đủ5role trong mục tài khoản, không ghi credential vào tài liệu/QA/Git công khai.

Windows/Java17.0.12/PostgreSQL17.11 loopback, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree. Offline anchor9/9, SQL fresh/replay/rollback/guard/phạm vi22/22, SYSTEM-DATA export/check khớp và handoff6/6; API Spring MockMvc+PG sau commit Demo3/3 và Normal lifecycle18/18 đạt. Ca booking ban đầu trả COACH_NOT_WORKING đúng vì test `today+3` rơi Chủ nhật; chỉ sửa fixture test bỏ qua Chủ nhật sang thứ Hai, giữ expected201 và rule Coach T2–T7, kiểm lại bằng DB mới. Clean cuối 671run=640pass/31skip/0failure/0error; skip không là pass. Bundle SHA256 `3cc9f84a6150553070133c2b8d59f1ea4feecd358bbfed8c990872495e519521`; bằng chứng chuẩn `demoBundleRegeneration20261112` trong [QA](../artifacts/qa/latest.json). Lượt đầu PostgreSQL đang khởi động chưa tạo DB; chạy lại khi ready đã đạt. Không sửa BE main/FE, không merge/push/deploy hoặc thực thi Supabase/provider; browser/Render vẫn NOT_RUN.

**Bước tiếp:** Owner chạy nguyên một file canonical trên đúng Supabase demo **trong ngày12/11 giờ VN**; script dựng lại55bảng SCMS/xóa4backup trong phạm vi đã nêu, không giữ thao tác demo phát sinh. Kết quả mong đợi result=PASS, summary.anchor_date=2026-11-12, business_timezone=Asia/Ho_Chi_Minh, fixture_version=normal-phase1-v3; migration_key vẫn là phiên bản20261110_demo_rebuild_v3. Không chạy thêm fixture/SQL16/migration sau bundle. Sau reset restart BE, đăng nhập mới/auth-me và đọc lại case trước demo; ngày cài khác phải sinh/kiểm lại, giữ guard.

**F3-v3 — ĐÃ SINH LẠI THEO OWNER, PASS REVIEW ASSET LOCAL:** [ảnh dùng trình bày](../image/F3-v3.png) có nguồn hồ sơ0, Member cung cấp người nhận1, Manager xác minh/duyệt2, Receptionist nhận việc3 và ghi giao dịch/chứng từ4, Manager đối soát5 rồi Completed/NeedsReview. “Bank transfer outside SCMS” nằm trên mũi tên3→4; giữ VNPay sandbox/xác minh Paid, hai nhánh Cash, Reports và Invoice Paid/amount gốc. Đã sửa connector sai của nháp, mở lại PNG đã lưu và kiểm thứ tự0→1→2→3→4→5, chữ/legend/role/state; giữ F3.jpg/F3-v2/F1-v2, cập nhật link dùng v3 trong SCRIPT/checklist/note private. QA `demoF3V3AndTemporaryPasswordReview20261112`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; asset review không là browser/BE PASS.

**Cấp/cấp lại mật khẩu tạm — CÓ IMPLEMENTATION, CHƯA XÁC NHẬN TRỌN LUỒNG TRÊN FE HIỆN HÀNH:** MemberService/UserService sinh mật khẩu ngẫu nhiên, lưu hash, bật first-login và gọi welcome email; đổi thành công tắt first-login, cấp lại thu hồi phiên cũ. Response báo kết quả gửi mail, tạo/cấp mật khẩu có thể đã lưu dù email gửi thất bại. Bằng chứng lịch sử local PostgreSQL `PROFILE-FIRST-LOGIN` PASS cho403 trước đổi→change-password204→profile200; fixture đã đăng nhập và tự bật cờ first-login, không phải ca tạo Member→nhận mật khẩu qua hộp thư. Email OTP Owner từng nhận không chứng minh email mật khẩu tạm. Lượt này chỉ rà source/bằng chứng, chưa tạo/cấp lại account, gửi mail hoặc kiểm FE đăng nhập/đổi lần đầu; SMTP acceptance cũng chưa chứng minh inbox delivery. Không chạy Maven/DB/provider mới. Bước tiếp để nghiệm thu riêng: tạo/cấp lại đúng account DEMO/mail được quản lý→nhận thư→đăng nhập bằng mật khẩu tạm→bị buộc đổi→đổi xong đọc lại hồ sơ trong phiên, khi phạm vi kiểm đó được mở.

**Checklist trình diễn F1–F5 — ĐÃ SOẠN LẠI, CHỜ OWNER DUYỆT:** thay phần Owner vừa tách trong [note-script-demo.md](../note-script-demo.md) bằng danh sách hành động đánh số theo F1→F2→F3→F4→F5, giữ11phút+4phút hỏi đáp. SCRIPT mụcB dẫn bản này, bỏ bảng/lời dẫn trùng; note private đã đồng bộ nhịp. F2 tạo đơn ở bước cuối, F3 thanh toán ngay và chỉ trình Refund một lần bằng hồ sơ riêng. Đã đối chiếu contract/source: notification nghiệp vụ là in-app, email OTP/mật khẩu tạm tách riêng; Refund Normal phát sinh từ nghiệp vụ đủ điều kiện, tài khoản nhận do Member cung cấp, Manager duyệt trước/đối soát sau, Receptionist nhận việc/thực hiện/ghi chứng cứ. [Phần Refund đã sinh lại trên F3-v3](../SCMS-SCRIPT-DEMO-BE.md#f3-refund-clarification-20261112) thêm nguồn phát sinh và tách nhận việc/chuyển ngân hàng ngoài SCMS/ghi chứng từ; bản mới đã review theo checkpoint trên. QA `demoPresenterChecklistAndRefundClarification20261112`; chỉ docs/review tĩnh local, browser/business/delivery/rehearsal mới NOT_RUN. Giữ ảnh v2 để đối chiếu và mọi diff/deletion; bước tiếp Owner duyệt checklist, rồi chọn case và rehearsal trong phạm vi được mở.

**Ảnh F1/F3 cho demo12/11 — PASS REVIEW ASSET LOCAL:** đã sinh bằng imagegen và mở lại [F1-v2](../image/F1-v2.png), [F3-v2](../image/F3-v2.png); giữ ảnh JPG gốc. F1 có OTP→Active→đăng nhập, Google existing/new onboarding, mật khẩu tạm/đổi lần đầu, liên kết quyền học và scope Manager/Admin. F3 có VNPay sandbox/Return chỉ điều hướng, xác minh Paid/đủ điều kiện mới cấp quyền, Cash Normal nguyên khối tách Advanced và Refund ba vai/chứng từ/đối soát; đã bỏ mũi tên thừa cho phép hiểu nhầm bỏ qua Receptionist. Giữ Reports/Invoice Paid/amount gốc. Review chữ, legend, connector và tỷ lệ trên PNG, không là nghiệm thu browser/BE. QA `demoFlowAssetsAndPresenter20261112`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; không chạy Maven/DB vì chỉ ảnh/tài liệu.

**Browser cho demo — ĐÃ KẾT NỐI, CHỈ ĐỌC MÀN LOGIN:** Playwright MCP list tab/snapshot/screenshot thành công tại `https://vibesports.pages.dev/login`. Đã quan sát “Đăng nhập”, “Đăng ký hội viên”, “Quên mật khẩu?”; snapshot đọc được nhãn Google trong iframe nhưng ảnh chụp không hiển thị rõ nút, chưa kết luận lỗi FE hoặc hiển thị/đăng nhập Google đạt. Chưa mở form đăng ký, đăng nhập hoặc gửi dữ liệu. Nút “Đăng ký hội viên” là nhãn đăng ký tài khoản, không chứng minh có gói hội viên chung. Chưa kiểm onboarding/OTP/cookie phiên, dashboard từng vai, thanh toán/quyền học, Refund/attendance/chat hoặc SHA/build/API origin FE. [Cách kết nối](02-DEPLOYMENT-GUIDE.md#playwright-vscode-demo-20261112); screenshot/snapshot và giới hạn ở QA `demoFlowAssetsAndPresenter20261112`. Checkpoint cài MCP trước đó vẫn giữ riêng, không còn coi thiếu kết nối tab là blocker hiện tại.

Kịch bản đã bổ sung lời dẫn/ngưỡng chuyển dự phòng theo khung **11phút trình diễn +4phút hỏi đáp** và note private ngắn có dữ liệu sandbox theo tài liệu VNPAY. Fixture runtime/kết quả dự phòng vẫn CHƯA CHỐT, rehearsal business NOT_RUN; không ghi READY. Bước tiếp: chọn đúng case/phiên bằng readback hợp quyền và rehearsal UI trong phạm vi được mở, giữ build đang chạy; Session3 và bàn giao/tích hợp Session1–2 tiếp tục tạm dừng.

Owner đã đồng ý toàn bộ phân tích/đề xuất/điều chỉnh demo, sẽ phản hồi thêm nếu cần. Note copy/paste đã rút về dữ liệu cho người trình diễn; ID/phiên/version/build do agent/BE kiểm tại docs02. Theo Owner, prompt tiếp tục gửi trong chat; đã bỏ nguyên prompt khỏi docs03, giữ trạng thái/link/anchor. Không đổi BE/FE đang chạy, schema/fixture/ảnh gốc; Session3 và bàn giao/tích hợp Session1–2 vẫn chưa tiếp tục.

**Ưu tiên Owner12/11 — GIỮ BẢN ĐANG CHẠY, CHUẨN BỊ DEMO15PHÚT:** chưa tiếp tục Session3 hoặc bàn giao/tích hợp Session1–2. Owner duyệt mô tả bộ cập nhật trong KIEM-TRA; giữ các diff/kiểm local trước đó, không suy đã deploy. Buổi demo hôm nay12/11 khoảng17:45–18:15 VN, có15phút gồm hỏi đáp; chọn VNPay sandbox và kết quả đã chuẩn bị làm dự phòng. [Đề xuất/kịch bản khung](../SCMS-SCRIPT-DEMO-BE.md#demo-fe-15min-20261112), [điểm vào session demo mới](03-TODO.md#demo15min-continuation-prompt-20261112). Chưa sinh/chạy lại bundle hoặc mutation cloud/provider.

**Checkpoint kết nối HTTP trước Playwright — PASS PUBLIC HTTP, BROWSER KHI ĐÓ BLOCKED:** site Owner xác nhận `https://vibesports.pages.dev`, giữ origin cũ; CORS GET/POST preflight của cả hai origin200 đúng Allow-Origin/credentials, health200/UP. Return rỗng302 tới `/payment-result?invoiceCode=&status=97` xác nhận env đích, không là giao dịch Paid; anonymous auth-me401 không là business pass. Công cụ UI tích hợp khi đó không có browser; HTTP FE403/1010 không là kết quả của tab Playwright mới đã đọc ở trên. Authenticated business vẫn chưa kiểm. Runtime schema có Google onboarding/chat close nhưng chưa có R2 PATCH; build SHA/API origin browser chưa xác minh. Bằng chứng lịch sử `vibesportsDemoReadiness20261112` trong QA; docs02 giữ cấu hình/giới hạn.

HEAD local `88ef854ad80c828ff0980542884a612cff29843e`, remote main sau fetch `a4cb2dba289611b64fc1628058f0c9fd0cbce279`; remote có Google onboarding/isNewAccount và chat close mới. Chỉ fetch/rà, chưa merge/resolve/push/deploy. Preview conflict với working tree hiện có ở docs00/docs01; không có conflict đã giải quyết. Giữ nguồn hiện hành và bàn xử lý merge riêng nếu cần sau khi chốt phạm vi demo. Lượt này chỉ docs/copy-paste private/đọc public HTTP; không chạy Maven/PG hoặc gán nghiệm thu BE mới.

**Session2 — ĐÃ ĐÓNG12/11 THEO OWNER, BE LOCAL ĐÃ KIỂM/BÀN GIAO RIÊNG:** Owner chọn “Skip demo 2.3, đóng Session 2”. Cả2.1/2.2/2.3 đã có bằng chứng BE local và demo SKIPPED_BY_OWNER riêng; skip không thành browser/FE PASS. Không còn lựa chọn nghiệp vụ hoặc checkpoint demo chờ trong Session2. Bằng chứng từng mục ở QA; tổng kết `session2Closure20261112`.

**Bộ bàn giao Session1–2:** sáu bản theo luồng đã chuyển vào [SCMS-CAP-NHAT](../SCMS-CAP-NHAT/KIEM-TRA.md), có mục lục/checklist FE ngắn; liên kết trong docs/QA đã đổi sang vị trí mới, không giữ bản sao ở root. Session3 khi được Owner mở sẽ thêm bản đúng luồng/ngày vào cùng thư mục và hoàn thiện `KIEM-TRA.md` ở3.3. Owner tự đóng gói ZIP; agent chỉ duy trì tài liệu/liên kết và đã bỏ ZIP tự tạo. Không có nghiệm thu BE/FE mới; bằng chứng tổ chức bộ và điều chỉnh đóng gói ở `handoffBundleOrganization20261112`/`handoffBundleOwnerPackaging20261112` trong QA.

| Mục | Kết quả trong phạm vi BE local | Demo Owner |
|---|---|---|
| 2.1/R1 | PASS, lọc ngày Session/lobby, đã bàn giao riêng | SKIPPED_BY_OWNER |
| 2.2/R2 | PASS, PATCH content-only/guard/lock/audit, đã bàn giao riêng | SKIPPED_BY_OWNER |
| 2.3 | PASS, shared plan Registered/Completed/Paid hiệu lực và Cancelled ngoài scope, đã bàn giao riêng | SKIPPED_BY_OWNER |

**Giới hạn còn nguyên:** checker contract tổng thể BLOCKED vì helper tham chiếu schema root đã xóa; kiểm riêng contract/OpenAPI của hai luồng đạt. Browser/FE/Render và repo/build/API origin FE chưa nghiệm thu. Lượt đóng chỉ cập nhật docs/QA và đối chiếu hash nguồn đã kiểm, không thêm nghiệm thu BE hoặc chạy lại Maven/PG/HTTP. **Bước tiếp khi Owner mở Session3:** theo [kế hoạch ba session](03-TODO.md#p22-p23-three-sessions-20261111), lần lượt3.1 PUT RBAC/quản trị cuối cùng →3.2 trình catalog lỗi đầy đủ trước sửa →3.3 matrix/readiness/bàn giao. **Session3 chưa bắt đầu.** Không cloud/reset/seed/SQL16/push/merge/deploy/provider.

**Session2.3 shared plan — PASS BE LOCAL12/11, DEMO SKIPPED_BY_OWNER:** Owner đã skip demo2.2 và chốt Cancelled ngoài shared read dù từng Paid/có kết quả; giữ own history/scope hiện có. Đã đồng nhất list/detail Registered hoặc Completed + Paid hợp lệ/effective class, loại reconciliation/reversed/Refund Completed đúng invoice; giữ Invoice gốc/amount/Enrollment/transfer/refund/write RP-36. Không thêm field wire/bảng/nhân bản plan/snapshot/versioning. [Contract](01-API-CONTRACT-FE-BE.md#shared-plan-history-20261112), [bàn giao flow FE](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-SHARED-PLAN-HISTORY-2026-11-12.md).

QA `session23SharedPlanHistory20261112`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ mọi diff/deletion; Windows/Java17.0.12/source copy không.env/H2/PostgreSQL17.11 loopback. Trước sửa28ca:6FAIL/0error/skip (5ca hành vi Completed/refund parity và1metadata mô tả); fixture evaluation tích lũy đã sửa riêng trước đối chứng chuẩn, không đổi expected nghiệp vụ. Cuối cùng H2/related63/63 trong clean (shared28/RBAC35), PG28/28, clean671run=640pass/31skip/0failure/0error; skip không là pass. Login/auth-me thật, response list/detail và DB/audit sau commit không đổi; own/other Member/center/authority, Pending/Waitlisted/Cancelled, Paid invalid/reconciliation/reversed/Refund Completed, transfer source/target/Invoice gốc và own private/result/attendance/evaluation/Coach cũ đã kiểm.

Kiểm sâu wire/schema phát hiện metadata cũ thiếu nullable cho classId/className/memberId/memberName/content/goal/startDate/endDate. Đã khai đúng8field tùy chọn hiện có, kiểm actual null payload và sinh schema từ app source cuối; không đổi field/type/format hoặc payload. Sau diff metadata đã kiểm lại PG/clean cuối ở trên; không cộng các lượt lịch sử làm quota. OpenAPI giữ route/query/response/R2, chỉ sửa mô tả2GET và nullable. Checker contract tổng thể vẫn BLOCKED theo quan sát R2 do helper tham chiếu schema root đã xóa; không rerun khi đường dẫn/tool không đổi, không khôi phục deletion hoặc sửa fingerprint lịch sử; kiểm riêng schema/contract/shared flow đạt.

Demo local18086/DB synthetic riêng, Clock thực/scheduler tắt trong harness: Member A2/Completed class1/plan1 đọc list/detail200; Cancelled class3/plan3 bị loại và private plan4 còn đọc; Member B3 không có quyền class1 nhưng đọc own class2. Agent login/auth-me/GET/Swagger/schema và DB/audit không đổi đã kiểm; buildCommit unknown đối chiếu fingerprint nguồn, không là SHA deploy/browser PASS. [Hướng dẫn login/đọc lại/đổi Member](02-DEPLOYMENT-GUIDE.md#local-shared-plan-history-demo-20261112), credential chỉ ở file private bị Git bỏ qua. Owner demo2.3 SKIPPED_BY_OWNER theo lựa chọn riêng; browser/FE/Render NOT_RUN. **Session2 đã đóng theo Owner; bước tiếp Session3 chỉ khi được mở riêng, chưa tự làm.** Không cloud/reset/SQL16/push/merge/deploy/provider.

**Session2.2/R2 — PASS BE LOCAL12/11, DEMO SKIPPED_BY_OWNER:** Owner duyệt [proposal](03-TODO.md#session22-r2-mutation-proposal-20261112) và yêu cầu phân tích ảnh hưởng flow FE. Đã thêm PATCH `/api/training-results/{resultId}/content`, DTO chỉ content bắt buộc/non-null,200 response hiện có. Chỉ sửa content result có sẵn; giữ tác giả/recordedAt/attendance/note/credit/lịch, không tạo result/Attendance. Coach Active+RECORD_RESULT cùng center/đúng phân công và tác giả; `[end, 00:00 ngày kế tiếp ngày Session)` giờ VN. Giữ guard quyền tại buổi/Completed; phân biệt thiếu Attendance với superseded, audit cùng transaction/no-op không log. POST/correction/handler giữ nguyên phạm vi. [Contract](01-API-CONTRACT-FE-BE.md#training-result-after-session-r2-20261112), [bàn giao flow trước–sau](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-AFTER-SESSION-2026-11-12.md).

QA `session22R2ContentAfterSession20261112`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ mọi diff/deletion; Windows/Java17.0.12/source copy không.env/H2/PostgreSQL17.11 loopback. Trước sửa38ca:37FAIL expected route mới/0error/skip; sau sửa H2 đích68/68 trong clean cuối (R2 gồm38, POST regression18, service12), PG HTTP38/38; clean643run=612pass/31skip/0failure/0error. Login/auth-me thật, response và snapshot DB/audit sau commit; biên end/cutoff/VN Clock, actor/authority/phân công/tác giả/center, ID giả, no-op/retry và hai nhóm chờ khóa center có kiểm metadata đều đạt. Đã sửa gap superseded lookup bị ẩn trong ca trước; lỗi setup spy transaction/tên cột audit/cách quote timestamp H2 đã sửa, không đổi expected. Skip không là pass.

OpenAPI từ app PG đã kiểm chỉ thêm path PATCH và DTO mới; TrainingResultResponse/phần khác giữ nguyên. Kiểm riêng contract/schema R2 đạt. `audit-fe-contract.py` tổng thể BLOCKED bởi helper còn tham chiếu schema_postgres.sql ở root đã bị xóa từ trước; không khôi phục deletion/đổi fingerprint lịch sử hoặc suy check đó PASS. R1 core/filter giữ nguyên; AttendanceRepository chỉ thêm lookup superseded dùng R2, các method cũ giữ nguyên. Reuse R1 PG/demo-skip, clean bắt buộc vẫn kiểm regression hiện có, không chạy lại đợt R1 PG riêng.

Demo local18085/DB synthetic riêng: Coach1/Member2/Session1/result1, Clock ứng dụng thực và scheduler tắt trong harness. Login/auth-me/GET/Swagger/schema đã kiểm, trước07:30 PATCH400 RESULT_CONTENT_NOT_OPEN và DB/audit không đổi; chưa ghi content mới thay Owner. Cửa success từ07:30 ngày12/11 tới trước00:00 ngày13/11 giờ VN. [Hướng dẫn login/PATCH/readback](02-DEPLOYMENT-GUIDE.md#local-r2-content-demo-20261112); credential chỉ ở file private bị Git bỏ qua. BuildCommit runtime unknown, nguồn đối chiếu fingerprint với bản đã kiểm; browser/FE/Render NOT_RUN. Owner đã chọn skip demo2.2, ghi SKIPPED_BY_OWNER, không thành browser PASS. **Session2 đã đóng theo lựa chọn skip riêng của Owner cho từng mục; Session3 chưa bắt đầu.** không cloud/reset/SQL16/push/merge/deploy/provider.

**Session2.1/R1 — PASS BE LOCAL12/11, DEMO SKIPPED_BY_OWNER:** đã lọc attendance gắn Session theo sessionDate; lobby lịch sử không Session theo ngày checkInTime, tính cả hai đầu ngày. Bỏ filter giữ tập/phạm vi cũ; date AND với các filter, giữ own/assignment/center/superseded. Predicate service là đường chạy hiện hành; query repository không có caller theo nghĩa cũ đã bỏ. Method/query/response không đổi; OpenAPI chỉ đổi mô tả hai tham số ngày ở hai alias. Không đổi timestamp/state/note/content/credit/lịch, schema hoặc Clock. [Bàn giao R1](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-ATTENDANCE-DATE-FILTER-2026-11-12.md), [contract](01-API-CONTRACT-FE-BE.md#attendance-date-filter-r1-20261112).

QA `attendanceDateFilterR120261112`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ mọi diff/deletion có sẵn; Windows/Java17.0.12/source copy không.env/H2/PostgreSQL17.11 loopback. Trước sửa:15ca,10ca chưa đạt expected R1 mới,0error/skip; sau sửa H2 đích30/30 (R1 gồm16ca và service14ca), PG HTTP16/16, clean602run=571pass/31skip/0failure/0error. Login/auth-me thật; response và snapshot DB/audit sau commit; ngày buổi/null, timestamp legacy khác ngày, biên nửa đêm PG microsecond, from/to đơn lẻ, AND, alias, actor/owner/assignment/center và guard cũ đã kiểm. Không tính31skip là pass; precursor trước sửa chưa có ca metadata OpenAPI bổ sung sau.

Demo R1 local18084 dùng DB synthetic riêng, actor Manager8/Session1 ngày12/11; đã kiểm login/auth-me/Swagger/schema và GET ra `[3,2,1]`, state Absent chỉ row2/null timestamp, ngày11/11 cho Session1 trả `[]`. BuildCommit runtime unknown, nguồn đối chiếu fingerprint source đã kiểm, không gán SHA deploy hoặc browser PASS. File login riêng bị Git bỏ qua, không chép credential vào báo cáo. Owner12/11 yêu cầu `skip demo R1`: demo R1 là SKIPPED_BY_OWNER; FE/browser và build/API origin chưa nghiệm thu. Demo Session1 vẫn SKIPPED_BY_OWNER. **Bước tiếp: dùng [prompt tiếp Session2](03-TODO.md#session2-continuation-prompt-20261112) để làm riêng2.2 rồi2.3; chưa bắt đầu implementation hai mục này.** Lượt chuẩn bị prompt chỉ sửa docs/QA, đối chiếu hash nguồn R1 không đổi; không chạy lại Maven/PG hoặc suy thêm browser PASS. Không reset/cloud/SQL16/push/merge/deploy/provider hoặc làm Session3.

**Hướng dẫn đăng nhập demo và nguồn FE12/11 — ĐÃ BỔ SUNG, CHƯA KIỂM BROWSER:** `login.manager1` là object email/password trong payload local, không phải username hoặc endpoint. [Hướng dẫn từng bước](02-DEPLOYMENT-GUIDE.md#local-scope-demo-login) có JSON riêng để copy, POST login → auth-me → GET scope trên BE local18082. Kiểm lại HTTP12/11: login200, userId7/CenterManager; auth-me200/có MANAGE_CLASSES; GET trả resultId `[3,2,1]`. Bằng chứng `demoLoginGuidanceAndFrontend20261112` trong QA, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree; nguồn BE và fingerprint DB của runtime đối chiếu checkpoint1.2, buildCommit vẫn unknown. Không suy thành browser PASS.

Owner xác nhận FE hiện hành tại [Pages](https://scms-demo-04nov.pages.dev/); cấu hình hiện hành ở [docs02](02-DEPLOYMENT-GUIDE.md#current-frontend). Chưa xác minh repo/build/API origin của FE; browser không có surface, đọc URL bằng công cụ nhận403 nên không kết luận browser của Owner truy cập lỗi. Chỉ cập nhật docs, sửa lỗi mã hóa/câu trạng thái bị cũ của lượt trước và tạo payload đăng nhập local bị Git bỏ qua; không đổi source BE/schema/Clock hoặc nghiệm thu11/11. Maven/test PG mới/browser/FE authenticated/Render smoke NOT_RUN; không reset/seed cloud/push/deploy/provider. Demo1.1 SKIPPED_BY_OWNER, demo Owner1.2/1.3 vẫn NOT_RUN.

**Session1.3 room validation — PASS BE LOCAL11/11; Session1.2/1.3 ĐÃ BÀN GIAO RIÊNG:** DTO name bắt buộc/max50, location optional/nullable/max100; JPA khớp VARCHAR(50) NOT NULL/VARCHAR(100) có sẵn. POST/PUT quá dài400 trước DB, giữ raw whitespace/null, không truncate/trim hoặc đổi RoomService/handler/schema. Snapshot FE chỉ cập nhật RoomRequest từ schema app đã kiểm; mô tả notifications lệch encoding khi sinh lại được giữ nguyên ở canonical. [Bàn giao room](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-ROOM-VALIDATION-2026-11-11.md), [bàn giao scope1.2](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-SCOPE-2026-11-11.md).

QA `p22P23Session13RoomValidation20261111`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree/source copy không.env, Windows/Java17.0.12/H2/PostgreSQL17.11 loopback. Trước sửa13ca:6FAIL invariant,0error/skip; H2 nhận51/101 và JPA255. Sau sửa/correct fixture H2 room13/13, PG13/13; liên quan publication4/4 và final clean586run=555pass/31skip/0failure/0error. Response + DB/audit sau commit, exact50/100, name bắt buộc, location null/omitted/empty/whitespace, role/center/readback/schema đã kiểm. Skip không là pass. Lượt PG đầu13ca có2fail do fixture trùng tên Available; đổi tên riêng đúng index và chạy DB mới, không sửa expected hoặc nới constraint.

**Phát hiện ngoài phạm vi còn mở:** PG POST trùng tên phòng trả409, PUT có thể500 khi flush qua audit; đã ghi [đầu vào error catalog3.2](03-TODO.md#room-duplicate-error-observation-20261111). Chưa sửa handler trong1.3 hoặc coi trùng tên là input hợp lệ cho ca success. Owner đã skip demo1.1 và cho hoàn tất cả1.2/1.3; demo Owner1.2/1.3, browser/FE NOT_RUN. Swagger local18082/18083 có DB synthetic riêng, login/auth-me/GET/schema đã kiểm; fixture room chưa ghi thay Owner, Clock ứng dụng nguyên trạng. BuildCommit runtime unknown, nguồn đối chiếu bằng fingerprint, không gán SHA deploy. **Dừng sau Session1; Session2/3 và cloud/reset/seed/SQL16/push/deploy/provider ngoài checkpoint này.**

**Session1.2 GET training-results — PASS BE LOCAL11/11:** staff/Admin có center bị giới hạn bởi Session.Class.center_id từ actor BE, AND với sessionId/memberId/coachId. Admin-null Active+có authority đọc giữ global; Member/Coach ưu tiên own/author kể cả extra authority và lịch sử center cũ. Không mở quyền tiền/report/Manager-only, không đổi schema/Clock/shape response. [Bàn giao scope đọc](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-SCOPE-2026-11-11.md).

QA `p22P23Session12Scope20261111`, HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ1.1 và diff có sẵn; Windows/Java17.0.12/source copy không.env. Trước sửa13ca:8FAIL scope invariant,0error/skip; sau sửa đích H2 30/30, HTTP PostgreSQL17.11 loopback13/13; clean573run=542pass/31skip/0failure/0error. Login/auth-me thật, synthetic A/B cùng center và hai center, readback + snapshot DB/audit không mutation; unit fail-closed cho staff non-Admin thiếu center vì PG không cho lưu fixture invalid đó. Skip không là pass.

Owner đã chọn SKIPPED_BY_OWNER cho demo1.1 và trả lời cho hoàn tất cả1.2/1.3 trong cùng lượt, kiểm/bàn giao riêng. Demo Owner1.2/browser/FE vẫn NOT_RUN; BE local18082 đã kiểm API/readiness trên DB synthetic riêng, buildCommit unknown được đối chiếu source fingerprint; không là SHA deploy hoặc browser PASS. Render/Supabase sau reset/cloud/provider/push/deploy NOT_RUN. **Checkpoint1.2 đã cho tiếp1.3; kết quả1.3 ở trên, không gộp Session2/3.**

**Session1.1 P2.2/P2.3 — PASS BE LOCAL11/11, DEMO SKIPPED_BY_OWNER:** đã chặn non-Coach ghi/xóa `training_results.content` qua POST đơn/bulk, kể cả chuỗi rỗng/trắng; bỏ/null content vẫn giữ nhận xét và attendance/note hợp quyền. Ghi content cần Coach Active+RECORD_RESULT và đúng phân công buổi; role/actor từ BE, coachId client không cấp quyền. Bulk lỗi rollback cả item trước và audit. Không đổi `[start,end)`, schema/Clock hoặc triển khai content sau end/GET scope/room.

Bằng chứng `p22P23Session11Content20261111` trong [QA](../artifacts/qa/latest.json), HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree service/tests/docs; Windows/Java17.0.12, source copy không.env. Trước sửa: precursor17ca,7FAIL invariant (HTTP200 trái expected403),0error/skip; sau sửa test đích H2:25/25, HTTP PostgreSQL17.11 loopback:18/18, clean558run=527pass/31skip/0failure/0error. HTTP thật có login/auth-me, fixture synthetic A/B cùng center, DB/audit sau commit; kiểm null/blank/nonblank, phân công/authority/active/center/eligibility, biên start/end và bulk rollback. Skip không là pass; lỗi compile/setup đầu lượt và readback Receptionist thiếu quyền GET đã sửa fixture/import, không tính nghiệm thu hoặc đổi expected nghiệp vụ.

[Bàn giao FE/Owner content11/11](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-CONTENT-2026-11-11.md) có đối chiếu trước→sau và demo4bước. Đã chuẩn bị BE local127.0.0.1:18081, DB demo synthetic riêng, Clock thực, health/Swagger/schema/login/auth-me/read rỗng đạt; buildCommit runtime `unknown`, nguồn được đối chiếu bằng fingerprint với source đã kiểm, không gán SHA deploy. Chưa ghi demo thay Owner. Công cụ browser không khả dụng: browser/FE NOT_RUN; demo Owner1.1 SKIPPED_BY_OWNER theo lựa chọn đã nhận; Render/Supabase sau reset/live/provider/push/deploy NOT_RUN. Giữ mọi diff/untracked/deletion có sẵn; không reset/seed cloud hoặc chạy SQL16. **Owner đã cho tiếp tục1.2/1.3; kết quả riêng ở trên.**

**Phòng ngừa lặp giả định sai về TrainingResult11/11 — PASS OFFLINE, KHÔNG ĐỔI SCHEMA:** theo câu hỏi Owner, đã ghi [rule bảo trì DB/compiler](../database/README.md#training-result-state), giữ NOT NULL/default Present và R2 chỉ cập nhật result có sẵn. Compiler vốn INSERT attendance_status tường minh; validator bổ sung chặn thiếu/null/state ngoài CHECK trước khi sinh SQL. Không nới nullable, không bỏ default hoặc cho content tự suy Present; giữ Late/Excused cho lịch sử schema cho phép.

Bằng chứng `trainingResultFixtureStateGuard20261111` trong [QA](../artifacts/qa/latest.json), HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree scripts/docs; kiểm offline9/9,0failure/error, gồm ca fixture thiếu/null/rỗng/CheckedIn bị chặn. Sinh staging đúng anchor10/11 để đối chứng bản đang có: schema/fixture trùng byte canonical, bundle SHA `a6664c07970bac49e84de4d6c9f9c6a5bab99980d89d876d6d706f40c395f0f7`; không chạy SQL hoặc đổi anchor của bundle bàn giao. BE/test ứng dụng/DB/contract/OpenAPI/SYSTEM-DATA không đổi, Maven/HTTP/PG/browser/live NOT_RUN. Owner đã đồng ý các điểm điều chỉnh kế hoạch; chưa triển khai BE1.1, các lựa chọn hẹn từng bước vẫn giữ. Không reset/seed/SQL16/push/deploy/provider.

**Ba session P2.2/P2.3 theo Owner11/11 — ĐÃ LẬP KẾ HOẠCH/RÀ TÀI LIỆU, CHƯA BẮT ĐẦU SỬA:** [điểm vào hiện hành](03-TODO.md#p22-p23-three-sessions-20261111) chia session1 content→center→room, session2 R1→R2→shared plan, session3 RBAC→toàn bộ cấu trúc lỗi/error-deprecation→matrix/readiness/bàn giao. Mỗi mục sửa/kiểm riêng rồi trình demo đơn giản FE đúng build hoặc Swagger; chờ Owner thử/skip tường minh trước chuyển mục, skip demo không thành business/browser PASS. Không làm lại Normal Phase1 đã phát hành hoặc gộp tích hợp P2.4–P2.6. Trước mỗi bước tiếp đọc lại diff/tài liệu/source vừa đổi.

Đã đối chiếu lại nguồn trong prompt ban đầu và CM-FIX-158; phát hiện/đơn giản hóa nằm cùng điểm vào: Admin-null global read học tập đã có mẫu contract/helper, rút đề xuất403 trước; result.content-only thiếu record chưa tương thích NOT NULL/default Present; shared-plan mở Completed/Paid bằng query/helper list/detail, không bảng mới/snapshot; Cancelled đề nghị giữ own history và chưa mở shared read. Quyết định Owner về room, R2 cutoff/audit, PUT/quản trị cuối và hướng Admin/read/history đã ghi đúng phạm vi trong docs04; chưa quảng bá contract/API chưa implement. Toàn bộ error catalog kèm nguồn/lý do/tác động phải trình trước sửa ở3.2; CM-FIX-15B HTTP409/body400 không bị coi là đã sửa.

Giải thích room50/100 là constraint của schema project/baseline, audit R2 tái dùng AuditService/system_logs để biết ai sửa/lúc nào/record nào, và smoke sau reset là kiểm nhanh mới trên DB đã reset chứ không reset thêm. Không reset vì sang ngày; SYSTEM-DATA giữ DATA DESIGN, ID/state/cookie smoke cũ vẫn lịch sử. Agent test local độc lập không cần đợi FE/browser; demo thực tế phải lấy fixture/build/phiên hiện hành, không đổi Clock/lịch sử.

Bằng chứng `p22P23ThreeSessionPlan20261111` trong [QA](../artifacts/qa/latest.json), HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ nguyên các diff có sẵn; BE/test/pom không đổi. Lượt này chỉ review source và sửa docs00/03/04/QA; Maven/HTTP/DB/cloud/browser/provider NOT_RUN, không có nghiệm thu BE mới. Chưa triển khai1.1, không reset/seed/SQL16/push/deploy/tiền thật. Bước tiếp Owner xem bảng ba session và các điểm cần chốt đúng lúc; mở session1 thì chỉ bắt đầu content, sau kiểm mới demo và chuyển mục tiếp theo theo nhịp Owner yêu cầu.

**Giải thích đề xuất P2.2/P2.3 ngày11/11 — CHƯA DUYỆT BƯỚC SỬA:** Owner yêu cầu mô tả cụ thể trước triển khai; đã ghi [đề xuất từng mục](03-TODO.md#p22-p23-proposal-clarification-20261111). Làm rõ content của result, giới hạn room50/location100, query staff theo center buổi/lớp, error/deprecation đích và bốn lựa chọn R1/R2/shared-history/PUT-quản trị cuối cùng. Đề nghị ưu tiên content→center→room→API vì scope dữ liệu cần xử lý trước validation input. Đồng ý hướng center/API chưa được diễn giải thành quyền sửa ngay; GET Admin-only và Normal/Refund giữ quyết định hiện hành.

Đính chính R2: entity/DB TrainingResult.attendanceStatus NOT NULL/default Present, nên ghi chú “content-only status nullable theo model hiện có” ở review trước chưa đúng. Đề xuất tối thiểu chỉ sửa result có sẵn; tạo result thiếu phải rà riêng, không sinh Present/attendance giả. Shared plan chưa có snapshot version; HTTP409/body.status400 đã được CM-FIX-15B ghi nhận cần tách phạm vi handler khi duyệt nhóm API. QA `p22P23ProposalClarification20261111` là review source/đề xuất, không test/HTTP/DB/browser mới; giữ mọi diff sẵn có, không đổi BE/test/schema/contract hoặc quyết định Dxx. Bước tiếp Owner duyệt từng nhóm sau giải thích; không chạy cloud/live/provider/reset/deploy/push.

**CM-FIX-15B — xác nhận contract Normal Lifecycle11/11, KHÔNG SỬA BE:** [nguồn version/enum/quyền/Waiting/lỗi và ví dụ GET→POST](01-API-CONTRACT-FE-BE.md#normal-cm-fix-15b-20261111) đã đối chiếu NormalChangeController/Service, SportsClassResponse, error handler và OpenAPI. Propose/cancel dùng lifecycleVersion lớp; apply dùng proposal.version. Apply tăng version lớp nhưng không tăng proposal.version, nên FE phải GET lại lớp trước cancel. Enum đúng hoa/thường; chỉ Manager Active+MANAGE_CLASSES cùng center, còn Waiting chặn apply400. Version lớp cũ409 hiện nằm trong message NORMAL_VERSION_STALE, không có code; body có status400 legacy dù HTTP409, lấy HTTP status thực. Proposal stale409 có code NORMAL_PROPOSAL_STALE. Giữ ngoại lệ retry Applied/Cancelled như contract.

Bằng chứng `normalCmFix15bContract20261111` trong [QA](../artifacts/qa/latest.json) là xác nhận source/contract, không là HTTP/browser mới hoặc nghiệm thu FE đã bỏ khóa ghi. Không đổi BE/test/schema/fixture/OpenAPI, không reset/deploy/provider. Bước tiếp: FE tích hợp đúng nguồn version, tải lại class/proposal sau mutation và xử lý409; xác minh build/phiên/fixture hiện hành trước ca runtime mới. G1–G4/P2.2/P2.3 còn theo phạm vi review đã ghi, không gộp sửa vào xác nhận này.

**Review P2.2/P2.3 ngày10/11 — ĐÃ REVIEW, CHƯA SỬA BE/API/POLICY:** [bảng phát hiện/nhóm sửa G1–G4](03-TODO.md#p22-p23-review-20261110) và [matrix từng method/path](03-TODO.md#p23-method-matrix-20261110) tách năm role/authority với owner/assignment/center/state/input/error/readiness. Giữ phần đã giải quyết để reuse: GET RBAC Admin-only, RP-04/RP-36 và private/Registered-Paid scope, phiên hiện hành, permission IDs/lock/audit, attendance correction, giá công bố/sân, Normal/Refund/report/provenance. Không đóng toàn bộ BE-01–BE-07 từ inventory hoặc smoke cũ.

**Phát hiện cần xử lý:** P0 non-Coach có quyền attendance có thể xóa content chuyên môn bằng chuỗi rỗng/trắng ở POST training-results đơn/bulk; P1 thiếu validation room_name50/location100, thiếu center scope nhánh staff GET training-results (tĩnh), TK11 annotation/error/deprecation còn thiếu. Đề nghị G1 sửa kỹ thuật trước sau Owner duyệt phạm vi; R1 nghĩa ngày Session/lobby, nhu cầu R2 content sau buổi/missing record/actor, shared-plan history và PUT/quản trị cuối cùng cần quyết định riêng. Giá sau công bố giữ409 hiện hành, không mở lại yêu cầu giá lúc tạo/công bố hoặc Normal/Refund đã chốt.

Bằng chứng mới `p22P23Review20261110` trong [QA](../artifacts/qa/latest.json), HEAD `88ef854ad80c828ff0980542884a612cff29843e` + working tree tài liệu/script đang có; `git diff 9fc3acaa3ba8f35f872f62a95c55ad4a39cfd6d1 -- src/main src/test pom.xml` rỗng. Windows/Java17.0.12, bản sao không.env, service mock và Bean Validation:9/9 probe tái hiện quan sát, không skip. Hai probe xác nhận non-Coach clear content thành công là **FAIL invariant ở service**, không phải PASS nghiệp vụ. DTO51/101 vẫn được chấp nhận là gap validation; HTTP/PG400 và DB commit chưa kiểm. Probe còn xác nhận lọc ngày bỏ Absent-null, R2 sau end bị chặn trước lookup, lobby/biên ngày/scope A/B và guard Cancelled/invalidated. Lỗi fixture Mockito đầu lượt không tính pass; sửa fixture, giữ expected theo D15 và không đổi source/Clock ứng dụng. Maven clean rộng không chạy vì không đổi BE và không có lý do chạy lại phần đã đủ bằng chứng.

Đã cập nhật docs03, Active/HISTORY/QA; không thay BE/test ứng dụng/schema/fixture/contract/OpenAPI, không reset/seed/cloud/live/provider/deploy/push. **Sau reset gần nhất vẫn chưa có smoke độc lập**; ID/state/cookie cũ chỉ lịch sử. SYSTEM-DATA giữ DATA DESIGN, FE quyết định cuối gợi ý chuẩn bị/flow/giao diện. Bước tiếp: Owner duyệt G1 và các lựa chọn thật sự còn thiếu; rồi test đích/response+state commit theo nhóm, giữ test center hiện có. P2.4/browser và các tích hợp sau tách riêng.

**Bàn giao P2.2/P2.3 cho session mới10/11 — ĐÃ CHUẨN BỊ REVIEW, CHƯA TRIỂN KHAI MỚI:** [điểm vào hiện hành](03-TODO.md#p22-p23-handoff-20261110) liệt kê phần đã có implementation/bằng chứng để reuse, phần còn thiếu kiểm chứng và lựa chọn cần duyệt. Đối chiếu cả hai DA_CHOT, Dxx, contract, source và đúng key QA; R1/R2/room validation/giá sau công bố/lịch sử đổi quyền/PUT RBAC-quản trị cuối cùng được tách từng ca, không hỏi lại GET Admin-only hoặc rule Normal/Refund đã chốt. P2.3 có phạm vi matrix BE-01–BE-07, năm role/owner/assignment/state/error/CRUD và điều kiện nghiệm thu; không cần chờ FE/browser để review BE, chưa mở quyền sửa policy/API mới.

Đã cập nhật hai handoff Normal/Refund10/11 từ mốc “chưa phát hành/Render” sang đúng checkpoint có phát hành/smoke API trước reset; sửa câu contract GET roles/permissions còn ghi chưa đổi, giữ PUT MANAGE_PERMISSIONS hiện hành. SCRIPT không còn mặc định DB sau smoke cũ vẫn tồn tại sau reset. Giữ FE quyết định cuối các gợi ý chuẩn bị/giao diện và SYSTEM-DATA là DATA DESIGN. Bằng chứng review tĩnh/tài liệu tại `p22P23SessionHandoff20261110` trong [QA](../artifacts/qa/latest.json); không sửa BE/schema/fixture, không có Maven/HTTP/cloud/browser mới. Bước tiếp: dùng prompt bàn giao để mở review P2.2/P2.3; trình nhóm sửa và câu hỏi nghiệp vụ thật sự còn thiếu trước triển khai.

**Owner xác nhận chạy lại bundle thành công10/11:** Owner báo vừa chạy lại `database/schema_postgres.sql` thành công trên Supabase demo sau lượt dò cuối. Đây là xác nhận trực tiếp của Owner; chưa gửi response/summary hoặc đối chiếu cloud độc lập trong lượt ghi nhận này, nên không tự gán lại số lượng/metadata/build hay nghiệm thu API/browser. Bằng chứng `demoBundleOwnerRerun20261110` trong [QA](../artifacts/qa/latest.json). Fixture/ID/cookie và kết quả smoke trước reset chỉ còn là lịch sử, không mô tả trạng thái hiện tại; cần restart BE, đăng nhập mới/auth-me và kiểm readback trước demo. Không cần reset chỉ vì qua ngày; chỉ sinh/kiểm bundle theo ngày mới khi muốn dựng lại DB. `migration_key` giữ phiên bản mô hình, ngày cài nằm ở `summary.anchor_date`.

**Dò cuối bundle10/11 với hai DA_CHOT — PASS LOCAL trong phạm vi schema/dữ liệu mẫu:** đã chạy nguyên canonical SQL trên DB PostgreSQL local mới; fresh/replay/rollback/guard/sequence22kiểm đạt, offline anchor8/8 và handoff6/6 đạt. Đối chiếu bảng20sân, min ceil(2/3),12buổi/quyền Paid/cutoff/xác nhận/đóng lớp, Coach–môn–lịch–sân/ngày nghỉ, N05→N11 cùng môn/giá/center và một chỗ, nghĩa vụ/receiver/duyệt/chứng từ/đối soát/SLA Refund; không phát hiện sai lệch trong các nhóm dữ liệu đã kiểm. Bằng chứng chuẩn `demoBundleFinalReview20261110` trong [QA](../artifacts/qa/latest.json); bundle vẫn SHA `a6664c07970bac49e84de4d6c9f9c6a5bab99980d89d876d6d706f40c395f0f7`, anchor2026-11-10 Asia/Ho_Chi_Minh.

Đã sửa README còn mô tả bộ rỗng/33account và liên kết schema root cũ; baseline lưu trữ đã đổi tên vẫn giữ nguyên nội dung. Chạy file SQL là **thực thi bundle đã sinh** để dựng lại DB demo, mất dữ liệu smoke đã tiêu thụ; không phải thao tác sinh bundle. Mã hóa đơn tăng tiếp và timestamp tạo/audit phụ thuộc lần chạy, nên SYSTEM-DATA là tham chiếu nghiệp vụ, không hứa trùng byte sau reset. Lượt dò cuối không thay SQL/fixture/BE/SYSTEM-DATA, không thực thi Supabase/deploy/provider hay kiểm browser mới. Owner đã báo chạy lại thành công ở checkpoint mới hơn; khi kiểm runtime tiếp lấy phiên mới/auth-me và xác minh build/DB/readback. Chỉ khi chọn dựng lại vào ngày khác mới sinh/kiểm cả bundle, không sửa riêng guard. P2.2/P2.3 tiếp tục từ bàn giao đầu Active.

**Hoàn thiện hai file bàn giao10/11 — SẴN SÀNG ĐỂ OWNER GỬI:** [SCRIPT-DEMO](../SCMS-SCRIPT-DEMO-BE.md) có phần đọc trước về sự cần thiết/thành phần/quá trình tạo và cách dùng bundle, phân biệt vai trò SYSTEM-DATA; giải thích anchor chính là ngày cài, giờ demo là tùy chọn và thời điểm thực tế do DB/runtime ghi. Case đổi sang DEMO-01–DEMO-10 để không nhầm Dxx quyết định. Gợi ý chuẩn bị/màn hình/nút/bố cục FE vẫn để FE duyệt, quyết định cuối thuộc FE.

[SYSTEM-DATA](../SYSTEM-DATA.md) mô tả mục đích tra cứu/chuẩn bị đầu vào/đối chiếu kết quả; có index N01–N12, Coach/sân/lịch/cutoff, tài khoản và proposal/Refund được sinh từ cùng các dòng local. Mapping dùng tên/email/FK, không suy ID theo thứ tự; phân biệt bộ nguyên trạng với cloud sau smoke. Toàn bộ55bảng chi tiết giữ nguyên dữ liệu so với đầu lượt, canonical bundle/fixture SQL không đổi byte. File vẫn DATA DESIGN, không phải dump Supabase.

Bằng chứng `demoHandoffFinalization20261110` trong [QA](../artifacts/qa/latest.json): offline8/8 kiểm ngày/cutoff/giờ tùy chọn/đổi mã case, SQL/fixture cùng byte khi có hoặc không có giờ; local SELECT/export6/6 kiểm alias với ID bị đổi/thứ tự dòng đảo, FK Invoice/Refund, thiếu/trùng dữ liệu bị chặn và tái sinh đúng byte. Cú pháp/liên kết và route cũ trong SCRIPT đạt; không sửa BE, không chạy Maven/SQL reset/cloud/provider mới. Docker local tạm dừng lúc setup đã khởi động lại, không tính lần setup lỗi là pass; lượt kiểm hoàn tất ghi riêng trong QA.

**Bước tiếp theo Owner chốt:** bàn giao SCRIPT và SYSTEM-DATA trước; sau đó mới P2.2/P2.3, kết hợp hai file Normal Lifecycle/Refund DA_CHOT, Dxx, contract/source/bằng chứng để duyệt việc cần làm. Tách thiếu triển khai/thiếu kiểm chứng/lựa chọn mới; không mở lại rule đã chốt hoặc bỏ qua mâu thuẫn thực chất. FE/build/browser còn phối hợp riêng. Git ghi nhận `schema_postgres.sql` ở root bị xóa trong lượt này ngoài phạm vi sửa; giữ nguyên, không tự khôi phục hoặc gộp xử lý vào nhóm bàn giao.

**Kịch bản demo/rule anchor10/11 — ĐÃ CHUẨN BỊ VÀ KIỂM LOCAL:** Owner đồng ý [SCMS-SCRIPT-DEMO-BE](../SCMS-SCRIPT-DEMO-BE.md) theo actor/fixture/thứ tự Cash→lifecycle→chuyển→Refund→reload/report. Toàn bộ đề xuất chuẩn bị/màn hình/nút/bố cục/thứ tự trình bày phía FE để FE duyệt; quyết định cuối thuộc FE. [Rule và lệnh chuẩn bị](02-DEPLOYMENT-GUIDE.md#demo-anchor-rules) là nguồn vận hành duy nhất; kịch bản dẫn contract/handoff, không chép toàn API hoặc log.

Generator bắt buộc anchor ngày cài VN, công cụ staging nhận giờ demo có offset và sinh availability/cutoff riêng; AVAILABLE không là PASS. Guard dùng thời gian VN thực, chặn trước CREATE/DROP; audit thêm anchor_date/business_timezone/fixture_version, giữ migration_key phiên bản. Canonical schema/fixture đã tái sinh cho10/11, hash bundle `a6664c07970bac49e84de4d6c9f9c6a5bab99980d89d876d6d706f40c395f0f7`; tại checkpoint chuẩn bị chưa thực thi các byte mới trên Supabase, sau đó Owner đã báo chạy lại như đầu Active. Chưa có đối chiếu độc lập executed bytes/summary sau lần chạy lại. SYSTEM-DATA vẫn DATA DESIGN, xuất từ DB local nguyên trạng cùng anchor/version và tái sinh đúng byte; exporter chặn sai anchor hoặc dữ liệu đã bị ghi tiêu thụ.

Bằng chứng `demoScriptAnchorPreparation20261110` trong [QA](../artifacts/qa/latest.json), base `88ef854ad80c828ff0980542884a612cff29843e` + working tree giữ nhóm smoke trước. Offline6/6 kiểm ISO/offset/ngày trong tuần/biên tháng–năm–nhuận/cutoff/staging tái sinh; PostgreSQL local22kiểm fresh/replay/rollback/guard ngày cũ–tương lai/ngoài phạm vi/sequence đạt. Java17 local API/PostgreSQL21/21,0skip: NormalLifecycle dùng MockMvc/commit DB, DemoRebuild có HTTP loopback/phiên thật; kiểm Normal/chuyển/Refund trên bản sao không.env. BE/tests/SQL trùng byte nguồn cuối, không sửa BE hoặc test Java. Không chạy clean rộng cho nhóm bảo trì; không tái dùng smoke Render thành kiểm mới.

**Checkpoint staging tương lai:** bundle11/11 đã sinh ở staging, SQL/HTTP NOT_RUN do chưa tới ngày cài; lệnh verify-local trả exit2 và không tạo DB. N02 của anchor11/11 bắt đầu13/11 06:00 VN, cửa xác nhận trễ từ12/11 06:00; không sửa Clock/expected để ép chạy. FE duyệt các gợi ý và xác định build; browser vẫn chưa kiểm mới. Chỉ sau quyền Owner cho đúng bundle/ngày cài mới thực thi cloud riêng. Owner đã báo reset canonical10/11 ở đầu Active; trạng thái sau smoke cũ bên dưới chỉ là lịch sử, không mô tả DB hiện tại.

**Smoke sau phát hành10/11, TRƯỚC LẦN RESET GẦN NHẤT — PASS API/DEMO trong phạm vi đã chọn; BROWSER BLOCKED:** BE `https://scms-app-p1gy.onrender.com` trả buildCommit đúng deployTargetCommit `f4d6866daadb0c54f74bb2e90c6648e2de1d11b2`, health/DB UP; fingerprint khớp Supabase đã xác nhận. SQL chỉ đọc xác minh marker `20261110_demo_rebuild_v3` và47account/21Class/9môn/20room. HEAD đầu session `88ef854ad80c828ff0980542884a612cff29843e` chỉ khác mốc deploy ở Active/QA; Git đầu session sạch. Bằng chứng chuẩn `normalPhase1Smoke20261110` trong [QA](../artifacts/qa/latest.json); không suy từ đây rằng build/DB/fixture sau reset mới đã được kiểm lại.

Đã login mới và auth-me cùng phiên cho Admin1/Manager32/Receptionist30/Coach16/Member cùng các Member fixture; kiểm owner, roster đúng phân công, role catalog Admin-only và Refund/proof/history đúng quyền. Cash quầy, minimum/cutoff/xác nhận trễ có reason/chặn hủy Ongoing, chuyển lớp giữ Invoice gốc, workflow Refund DEMO, retry và response+readback sau commit đạt trong phạm vi QA. Báo cáo thu–hoàn–ròng khớp SQL theo kỳ; audit đúng actor/center và thông báo sau commit đúng người nhận. Ba lệch ban đầu của runner được giữ trong QA cùng căn cứ kiểm lại:404 che Invoice khác chủ, độ chính xác timestamp lưu DB và SLA hết ngày TT mở thứ7 theo quyết định E6; không đổi rule/source/DB để làm xanh.

**Lịch sử fixture DEMO đã tiêu thụ TRƯỚC RESET, không dùng các ID/state này làm precondition hiện tại:** Member37/38 vào N01(class10), Invoice58/59 mỗi900.000đ và enrollment51/52; Manager32 xác nhận N01 và xác nhận trễ N02(class11). Member2 Accept proposal1 N05(14)→N11(20), transfer1/enrollment53; Invoice14 vẫn class14/1.800.000đ, entitlementClassId20. N11 đủ4/4, Member4 còn Waiting và N05 còn changePending; apply bị chặn đúng. Refund3/Invoice17/Member5 đã Completed **mô phỏng DEMO**, attempt9/document3; không chuyển tiền thật, Invoice vẫn Paid và enrollment Cancelled. Upload riêng trước đó vẫn AwaitingVerification; retry không nhân Invoice/quyền/attempt/chứng từ.

**Còn thiếu/bước tiếp:** FE Owner xác nhận `https://scms-demo-04nov.pages.dev/`, chưa biết commit; QA lưu hash HTML/JS và xác minh asset gọi đúng BE với credentials include. CORS HTTP đúng origin và cookie Secure/HttpOnly/SameSite=None đạt; **không thay browser PASS**. Công cụ browser có inventory rỗng, mở iab báo unavailable: browser login/lưu/tải lại và UI Normal mới BLOCKED/NOT_RUN. Cần browser khả dụng và nguồn/build FE để kiểm tiếp trên fixture sau smoke. Tự khai giảng/đóng lớp và đúng biên48h/24h NOT_RUN ngoài cửa thời gian; N02 có buổi đầu11/11/2026 06:00 giờVN để quan sát tiếp. Cross-center/race/CRUD/matrix và các nhánh chưa chọn vẫn NOT_RUN; R1/R2/policy review riêng. Không sửa BE nên không chạy Maven mới; không reset/seed/SQL16/deploy/provider thật. SYSTEM-DATA giữ DATA DESIGN, không là dump Supabase.

**Phát hành Normal Phase1 đã merge/push10/11:** tai→main fast-forward, không có conflict; đã đẩy đồng thời hai nhánh lên commit phát hành `f4d6866daadb0c54f74bb2e90c6648e2de1d11b2`. [CI main](https://github.com/Aditerx/SWP391/actions/runs/42755542205) và [CI tai](https://github.com/Aditerx/SWP391/actions/runs/42755542031) đều SUCCESS. Commit BE nguồn `9fc3acaa3ba8f35f872f62a95c55ad4a39cfd6d1` giữ toàn bộ nhóm đã kiểm; fingerprint sau checkout vẫn khớp, không bỏ diff/untracked của Owner. Bản ghi evidence tiếp theo chỉ sửa Active/QA và có skip CI/Render; mốc BE dự kiến triển khai là deployTargetCommit trong QA, không nhầm với HEAD chỉ cập nhật tài liệu. **Runtime build/DB và smoke API DEMO đã kiểm ở đầu Active; browser vẫn BLOCKED.**

**DeepSeek AI Chat DB tools — PASS local; real provider/PostgreSQL/live NOT_RUN:** Giữ 6 tool JSON read-only, `SCMS_AI_CHAT_DB_TOOLS_ENABLED=false`, member scope chỉ từ `respond(..., memberUserId)`, 3 vòng/10 hàng mặc định, DTO whitelist và giới hạn 2.000 ký tự/tool. `get_my_upcoming_sessions` nhận `fromDate`/`toDate` yyyy-MM-dd; mặc định fromDate là hôm nay, khoảng inclusive tối đa 31 ngày; nếu bỏ toDate thì lấy fromDate+30 ngày, trả `truncated` và `total`. Prompt có ngày/giờ/thứ Asia/Ho_Chi_Minh, quy tắc tuần/ngày tự nhiên, gọi tool cho dữ liệu của chính Member, không mời handoff sau câu trả lời đủ dữ liệu; mặc định danh sách gạch đầu dòng, không bảng Markdown (`SCMS_AI_CHAT_MARKDOWN_ENABLED=false`). Hóa đơn sắp theo `COALESCE(paymentDate, createdAt)` giảm dần, tối đa 5. Khi DB tools bật và provider/runtime lỗi, fallback ngắn kèm `AI_UNAVAILABLE`/`AI_RUNTIME_FAILURE`, không chạy FAQ keyword; khi cờ tắt giữ FAQ fallback. `clean test` PASS Java17, 572 run=540 pass/32 skip/0 failure/0 error; test đích tool 7/7 PASS. QA: `deepseekChatDbTools20261112`. Provider thật, PostgreSQL fixtures, FE/browser và Render chưa chạy; bước tiếp là smoke provider và kiểm query trên DB fixture cô lập.

**Member chọn gặp nhân viên trong AI Chat — PASS local; FE/PostgreSQL/live NOT_RUN:** Trên branch `feature/chat-member-choose-handoff`, base mới nhất `origin/main` `a4cb2db` (đã gồm merge close Member), thêm `SCMS_AI_CHAT_HANDOFF_MODE` mặc định `SUGGEST`, giữ `AUTO`; explicit request vẫn chuyển. Gợi ý token/FAQ, quota, responder rỗng/không có/lỗi được lưu với metadata `handoffSuggested`/`reasonCode`; SYSTEM fallback không phát notification hoặc unread Member và chống lặp cùng nguyên nhân. Response API trích metadata an toàn; cập nhật docs01 mục16, OpenAPI và [bàn giao FE AI Chat](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-12.md). Không migration, endpoint, quyền, quota/rate limit hoặc gọi mạng provider. `clean test` local Java17: 560run=528pass/32skip/0failure/0error; các test chat liên quan55/55 và `DeepSeekChatClientTest`7/7 PASS trong lượt clean. SHA-256 manifest BE/tests `74E85D2B08CC010F008127A5101567E18761F94A20F99729A6858CCF4CAF530D` (14 changed files). So với base mới nhất ước tính 551 test (548 ghi nhận ở checkpoint close trước merge Google Auth +3 test từ merge đó), scope này thêm9 test; base chưa được chạy độc lập trong lượt này. Diff check/OpenAPI JSON PASS. PostgreSQL after-commit, FE/browser, provider thật và Render NOT_RUN; bước tiếp: FE hiển thị lựa chọn và gọi `/escalate` chỉ khi Member chọn, sau đó chạy PG proof cô lập.

**Member tự kết thúc AI Chat — unit/controller PASS local; HTTP+DB after-commit proof BLOCKED:** Chat không có Dxx riêng; áp dụng tiêu chuẩn evidence D19. Trên branch `feature/chat-member-close`, base `origin/main` `88ef854`, thêm `POST /api/chat/conversations/{id}/close` cho Member; kiểm ownership dưới lock, đóng theo Clock, giữ handler/assignedStaff, SYSTEM message idempotent, không notification/unread cho Member; member gửi sau đóng vẫn409, đọc lịch sử và mở conversation mới theo cờ hiện hành. Reply AI muộn không được lưu; worker bỏ qua CLOSED và escalation không sửa hội thoại đóng. Không migration, không sửa luồng close staff/DeepSeek/FAQ/quota/rate limit. Test đích chat36/36; full `clean test` cuối548run=516pass/32skip/0failure/0error, so baseline cùng base +9run (+8pass,+1 opt-in skip). Fingerprint SHA-256 BE/tests `79A8DC53C7A9B9281210CFD8D5FE6ED616F3EFA735280C7E0C52A09AC832FCD9`. HTTP+DB proof đã thêm trong `CommunicationFeaturesPostgresTest` nhưng BLOCKED vì `127.0.0.1:55440` không mở; không tính skip là pass. FE/browser, PostgreSQL proof, provider thật và Render chưa nghiệm thu. Bước tiếp: FE gọi close khi Member bấm “Kết thúc”, rồi POST `/api/chat/conversations` khi họ chọn bắt đầu chat mới; chạy PG proof trên DB loopback cô lập khi môi trường sẵn sàng.

**Phát hành Normal Phase1 đã merge/push10/11:** tai→main fast-forward, không có conflict; đã đẩy đồng thời hai nhánh lên commit phát hành `f4d6866daadb0c54f74bb2e90c6648e2de1d11b2`. [CI main](https://github.com/Aditerx/SWP391/actions/runs/42755542205) và [CI tai](https://github.com/Aditerx/SWP391/actions/runs/42755542031) đều SUCCESS. Commit BE nguồn `9fc3acaa3ba8f35f872f62a95c55ad4a39cfd6d1` giữ toàn bộ nhóm đã kiểm; fingerprint sau checkout vẫn khớp, không bỏ diff/untracked của Owner. Bản ghi evidence tiếp theo chỉ sửa Active/QA và có skip CI/Render; mốc BE dự kiến triển khai là deployTargetCommit trong QA, không nhầm với HEAD chỉ cập nhật tài liệu. **Chưa xác nhận Render Live/buildCommit hoặc chạy smoke cloud.**


**Owner10/11 — SUPABASE DEMO PASS, YÊU CẦU MERGE tai→main:** Owner gửi result=PASS/migration_key=20261110_demo_rebuild_v3, đúng bộ47account/21Class/9môn/20room và mode approved-synthetic-demo. Đây là bằng chứng Owner chạy SQL, không phải agent tự mutation/readback cloud hoặc nghiệm thu BE/FE. Đã ghi `normalDemoOwnerAcceptance20261110` trong QA; không chạy lại reset/seed/SQL16 hoặc migration nối tiếp. SYSTEM-DATA vẫn DATA DESIGN, chưa chuyển thành dump Supabase.

**Phát hành Phase1:** source/test/schema khớp fingerprint nghiệm thu local ở dưới, không phát sinh BE diff mới; đóng gói Java17/JAR có module mới đạt, không gọi app/provider. Đã commit nhóm Phase1 tại `9fc3acaa3ba8f35f872f62a95c55ad4a39cfd6d1` và hợp nhất tai→main bằng fast-forward, không có conflict. Khôi phục đúng byte/EOL sau checkout và fingerprint vẫn khớp bản đã kiểm; các worktree khác giữ nguyên. Commit metadata phát hành chỉ cập nhật Active/QA; lấy SHA main/CI thực tế làm mốc build Render. Trạng thái Git/CI theo `normalPhase1Release20261110` trong QA; Kết quả runtime/smoke hiện hành ở đầu Active/QA; browser chưa nghiệm thu. [Quy trình smoke cụ thể](02-DEPLOYMENT-GUIDE.md#normal-release-smoke-20261110): kiểm build/DB/phiên trước, đọc scope theo vai trò, sau đó mới nhóm giao dịch demo theo fixture được xác định; browser FE tách khỏi API.


**Phase1 10/11 — P1.1–P1.6 ĐẠT LOCAL; KẾT QUẢ PHÁT HÀNH MỚI Ở ĐẦU ACTIVE:** đã hoàn thiện Normal minimum/cutoff/Cash/waitlist/lifecycle cùng dời1lần có consent, proposal snapshot/version/fingerprint, chuyển quyền giữ Invoice gốc và hủy trước khai giảng gắn nghĩa vụ hoàn. Refund có receiver version/xác minh/duyệt lại, UUID claim/attempt/private proof/từng lần đối soát/Failed–Pending–Reversed/tiếp nhận khiếu nại/SLA ngày TT mở/contact/report/audit/thông báo sau commit. Giữ D12/D15/D16, role/owner/center và toggle mặc định tắt. [Checkpoint hiện hành](03-TODO.md#normal-phase1-complete-20261110), [bàn giao Lifecycle](../SCMS-BAN-GIAO-FE-NORMAL-LIFECYCLE-2026-11-10.md), [bàn giao Refund](../SCMS-BAN-GIAO-FE-NORMAL-REFUND-2026-11-10.md).

Bằng chứng `normalPhase1Implementation20261110` trong [QA](../artifacts/qa/latest.json), base `d3394d61151644d88e637fd4d0ad31e2337ee36f` + working tree chưa commit; source BE/tests khớp hai bản sao không.env. Java17/H2 clean539run=508PASS/31SKIP/0failure/0error; PostgreSQL17.11 loopback HTTP21/21, có auth-me/commit/readback, claim race, chuyển N05→N11 từ fixture thật, biên và kỳ thu/hoàn. Bundle fresh/replay/FK+view rollback/sequence/credit/lịch/thứ tự tài chính đạt20kiểm; guard22/replay không sửa receipt/private asset. OpenAPI từ app PostgreSQL đã kiểm,209route khớp source; wire lịch sử chỉ kiểm cấu trúc. Lỗi DTO schema trùng Advanced và transaction read-only ghi audit đã sửa, giữ expected nghiệp vụ.

Schema đích qua generator nay có55bảng SCMS/47account/21Class/9môn/20room và2PDF DEMO riêng tư; mọi lịch sử tổng hợp ghi rõ DEMO. Bộ đếm hóa đơn không giảm, bảng/hạ tầng ngoài phạm vi giữ RESTRICT. [SYSTEM-DATA](../SYSTEM-DATA.md) vẫn **DATA DESIGN**, xuất từ DB fixture local nguyên trạng; OTP/QR/Pending/provider attempt tạo lúc demo, không kéo TTL hoặc seed secret. Rà thêm sửa Holiday tag, Accepted chưa chuyển quyền, credit Expired và thứ tự timestamp hồ sơ hoàn; không coi seed là tiền/học thật đã quan sát.

**Đã tích hợp main theo Owner:** `git pull --no-rebase origin main` trên tai fast-forward từ `165bb45` tới `d3394d6` (merge PR21); hai commit có tree giống hệt, **không có conflict nội dung**. Đã áp lại diff/untracked, kiểm hash từng file và khôi phục đúng byte/line ending; sau đó bổ sung guard kiểm lại Paid/đối soát/liên kết quyền ngay lúc chuyển và chạy lại clean/HTTP PostgreSQL trên nguồn cuối; các kết quả ở trên là lượt cuối này. Giữ bản sao/stash phục hồi riêng, chưa commit hoặc push Phase1. [Bảng review và đề xuất tiếp](03-TODO.md#normal-phase1-complete-20261110).

**Cập nhật điều phối:** Owner đã chạy bundle demo PASS và yêu cầu merge tai→main theo đầu Active. Runtime Render và smoke API DEMO đã kiểm theo đầu Active; tiếp theo browser FE và các cửa thời gian chưa tới. R1/R2/policy và matrix/CRUD/live còn trình riêng. Agent không tự reset/seed lại, deploy thủ công, chạy cleanup16 hoặc gọi provider thật.

Các snapshot dưới đây giữ lịch sử trước nhóm hoàn thiện, không là danh sách thiếu hiện hành.

**Owner10/11 — GET RBAC ĐÃ KIỂM LOCAL; NORMAL/DEMO ĐÃ CHỐT THIẾT KẾ, CHƯA TRIỂN KHAI:** đã sửa GET `/api/roles`, `/api/permissions` và `/api/roles/{id}` chỉ Admin Active, chặn tuyến chi tiết để tránh đọc permissions qua ID. Không thêm authority đọc hoặc đổi gate PUT. Bằng chứng `roleCatalogReadAccess20261110` trong [QA](../artifacts/qa/latest.json), base `165bb45e8e15f909c66efb5f9e8b15f29282cd0a` + working tree chưa commit, fingerprint khớp bản sao không.env. Test đích H2/phiên thật35/35; clean520run=490PASS/30SKIP/0failure/0error. Admin vẫn đọc khi thiếu MANAGE_PERMISSIONS; bốn role khác có thêm authority vẫn403; anonymous401 và phiên Admin Inactive/Locked403 được kiểm riêng. OpenAPI sinh từ HTTP app đã kiểm khớp nội dung snapshot nên giữ file canonical; contract ghi gate mới, offline190route đạt. Chưa push/deploy hoặc nghiệm thu FE/Render/CRUD/live.

**Dữ liệu/Normal10/11 — PHẦN THIẾT KẾ ĐÃ ĐƯỢC DUYỆT:** Owner chốt tiếp3–5: N11 Pickleball làm đích N05, contact Member chuẩn bị ở BE nhưng FE quyết định dùng/hiển thị,07ngày làm việc tính ngày TT mở theo Holiday kể cả T7/CN. Không còn chọn T2–T6/đổi7ngày lịch. Đã đồng bộ hai file yêu cầu và docs03/04, giữ33account PDF. Đã tạo [SYSTEM-DATA.md](../SYSTEM-DATA.md) theo47bảng hiện hành +6bảng dự kiến,47account/21Class, nhãn DATA DESIGN chưa seed; dữ liệu tham chiếu/dòng Normal/Advanced/Refund minh họa và bảng rỗng/động đều nêu phạm vi. Generator tái sinh đúng byte, kiểm khóa tham chiếu/role/UUID,12buổi hợp lệ, min/sĩ số, thời gian và overlap Coach/room/Member của bộ thiết kế; bằng chứng `approvedDemoDataDesign20261110` trong QA. Đây không là xuấtSQL-ready/fullrow từ DB đã seed, không khẳng định BE mới đã triển khai; private asset và case động còn thực hiện khi tạo fixture. Source BE/SQL vẫn khớp lượt GET RBAC đã kiểm, không chạy Maven mới cho docs/generator. Chưa triển khai Normal/refund/seed/reset/cloud/FE; bước tiếp là hiện thực nhóm đã chốt và fixture rồi xuất dữ liệu thực thi từ cùng nguồn. Các thiết kế khác tiếp tục session mới.

**Owner09/11 — REVIEW BỘ DEMO + NORMAL LIFECYCLE/REFUND, CHƯA TRIỂN KHAI:** Owner muốn duyệt account/lớp/Coach và dữ liệu demo cho giao dịch/quyền học, lịch/attendance, chuyên môn, vận hành trước khi xuất `SYSTEM-DATA.md`. Đã đọc hai file yêu cầu mới, đối chiếu source/contract/schema/test hiện có; [bảng đề xuất và điểm cần chốt](03-TODO.md#system-data-review-20261109) là nguồn review hiện hành. Bộ đề xuất47account/21Class và hồ sơ lifecycle/refund mới chưa được duyệt hoặc seed. Đã thấy thiếu minCapacity/48h/24h, guard chuyển trạng thái Class, nhánh bù dùng nhầm Completed và quy trình chứng từ/Member Refund chưa có; phần đúng được đề xuất tái sử dụng. Chỉ review tĩnh/tài liệu, chưa sửa BE/SQL/contract/OpenAPI hoặc chạy test mới. **Tạm giữ bước chạy reset rỗng/push/deploy ở snapshot dưới; không chạy bản reset trước khi thống nhất bộ demo mới.** Sau duyệt mới triển khai phần thiếu, kiểm fixture và xuất SYSTEM-DATA từng bảng; các thiết kế khác để session mới. FE/Render/CRUD/live chưa nghiệm thu.

**DB demo09/11 — PASS LOCAL, CHƯA RESET SUPABASE:** Owner yêu cầu rà DB hiện tại và dựng lại sạch; thay phương án sửa riêng ca164/chờ consent. Đã audit Supabase trực tiếp chỉ đọc:51bảng public/2146dòng,4backup cũ,128log legacy thiếu provenance,1ca164 sai môn/sân; user40 Pending hợp lệ cho OTP, tiêu chí audit đã sửa. Thiếu pricing_draft do20 chưa chạy là NOT_RUN. Catalog có anon SELECT trên46bảng chưa bật RLS, chưa kiểm khai thác Data API. Đã mã hóa backup public ngoài Git và restore clone local; không mutation cloud.

Bằng chứng chuẩn `demoDbAuditRebuild20261109` trong [QA](../artifacts/qa/latest.json), base `165bb45e8e15f909c66efb5f9e8b15f29282cd0a` + working tree chưa commit; BE/pom không đổi so Phase1, fingerprint/schema/XML ở QA. [File duy nhất dựng lại](../database/schema_postgres.sql) đã kiểm trên clone DB thực tế, fresh/replay/rollback FK+view ngoài phạm vi, giữ bảng/quyền ngoài SCMS, thu hồi browser grants và không tái dùng mã thanh toán. Bộ sạch có33account/9môn/20sân/9template Advanced, không dựng Paid/credit/buổi học/kết quả. Audit sau reset38/38 không phát hiện dòng vi phạm trong các query; không suy thành mọi nghiệp vụ đã nghiệm thu. JPA validate và PostgreSQL HTTP2/2: đăng nhập33account, Normal12buổi→Cash, Advanced→Cash→đặt ca, retry/credit/readback và sai sân bị chặn. Clean test518run=488PASS/30SKIP/0failure/0error;2test PG mới nằm trong skip của clean nhưng đã PASS riêng. Sửa `notifications.related_id` thành BIGINT đúng entity; giữ Pending OTP, guard quyền/consent/lịch hiện hành. Generator tái sinh đúng byte, contract offline190route đạt; OpenAPI không đổi vì BE không đổi. AGENTS đã ghi bối cảnh học tập/demo, double-check và xưng Tôi–Bạn.

**Bước tiếp của snapshot reset rỗng — đã được yêu cầu review bộ demo thay thế:** hướng dẫn [docs02](02-DEPLOYMENT-GUIDE.md#demo-rebuild-20261109) chưa thực hiện cloud. Theo điều phối mới ở đầu Active, duyệt dữ liệu/lifecycle/refund trước rồi mới chốt script chạy, phát hành và FE đơn giản. Không chạy thêm16/18/19/20 máy móc; giữ toàn bộ diff/tài liệu hiện có và snapshot LAN-2 ngày07/11.

**Phase1 09/11 — PASS LOCAL MỤC1–4, DỪNG TRƯỚC MỤC5:** đã triển khai các phần có căn cứ của R3/private scope, validation status/permission, audit RP-43, giá công bố Normal và sân đúng môn. [Bảng review trước/sau và ma trận theo luồng](03-TODO.md#phase1-review-20261109); [đề xuất R1/R2 để duyệt cho session mới](03-TODO.md#phase1-item5-proposal-20261109). R1/R2 chưa sửa source/API; quyền đọc GET danh mục đã được Owner chốt10/11 theo đầu Active; guard quản trị cuối cùng, giá sau công bố và ca lịch sử chưa rõ không được tự mở quyền.

Bằng chứng chuẩn: `phase1Implementation20261109` trong [QA](../artifacts/qa/latest.json), base Git `165bb45e8e15f909c66efb5f9e8b15f29282cd0a` + working tree chưa commit; fingerprint BE `c8ebeb6a85e4f6bc1aeb09997150fe655376d56b3eae6e40dd5a4e74f06137e7`. Windows/Java17.0.12, Clock Asia/Ho_Chi_Minh, bản sao không.env. Test đích rồi clean test:516run=488PASS/28SKIP/0failure/0error; skip không là pass. Sau clean chỉ chỉnh fixture PG (flush subtype trước hóa đơn Cash, tên sân synthetic trong giới hạn50), không đổi BE/schema; kiểm lại fixture cuối H2 đạt4/4. PostgreSQL17.11 loopback: guard/audit33, giá/sân4, attendance22 — tổng59/59, có commit/race/readback; các suite dùng DB mới riêng. Migration18–20 guard/replay/bảo toàn lịch sử đạt, OpenAPI sinh từ app đã kiểm và kiểm schema/header/enum; hash/XML/fingerprint đầy đủ tại QA. QueryDR trong regression chỉ dùng provider giả local, không xác nhận live.

**Chưa phát hành/nghiệm thu:** giữ diff hiện có, chưa push/merge bản Phase1, agent chưa mutation Supabase/chạy SQL16 hoặc deploy Render; Owner đã chạy18 và19 theo trạng thái đầu Active; bản dựng lại09/11 đã PASS local, schema root bảo toàn vẫn ở baseline cũ. FE/Render/CRUD/live vẫn đợt chung. F05/F06/F12 được Owner xác nhận OK trong phạm vi đã báo, các phản ánh FE khác giữ lịch sử/cần kiểm trên build mới. Log audit legacy chưa rõ provenance giữ DB nhưng chưa trả qua API scope; gap độ dài room_name50/location100 ghi ở mục6/CRUD, không coi là lỗi FE mới. Google/avatar để sau bàn giao các phần Phase2 khác; OTP chính giữ trạng thái đã xong, AI còn mở, QueryDR rà/chốt tại Phase2 mục3 và không đổi env. **Bước tiếp hiện tại: hoàn tất schema demo theo điều phối đầu Active; sau SQL/deploy/FE đơn giản mới trình lại mục5 và policy để Owner duyệt cho session mới.**

**Owner09/11 — FE VỪA CẬP NHẬT, GIỮ PHÁT HIỆN CŨ ĐỂ KIỂM LẠI:** Owner yêu cầu giữ các phản ánh UI/API đã báo ở trạng thái lịch sử/cần kiểm lại; có khả năng đợt FE mới đã xử lý. Agent chưa kiểm build mới nên chưa xác nhận đã sửa hoặc còn lỗi. Các nhận định “FE chưa sửa”/ảnh lỗi/checkbox trong snapshot bên dưới và LAN-1/LAN-2 không là kết luận cho FE hiện tại; giữ các bản bàn giao ngày cũ. [Danh sách F01–F12 và đề xuất hai phase](03-TODO.md#fe-refresh-plan-20261109) tách quan sát lỗi cũ, request đúng contract, tích hợp chưa nghiệm thu và khoảng trống BE còn thấy ở source. Kế hoạch đã được Owner duyệt với điểm dừng trước mục5; implementation/kiểm hiện hành ở đầu Active. Giữ khác biệt giữa xác nhận FE của Owner và các kết quả BE local; không biến log/capture cũ thành bug hiện tại hoặc nghiệm thu live mới.
**SRS — bản 1.1 cập nhật 11/11/2026:** [DOCX](../output/SRS.docx), [PDF](../output/SRS.pdf), [Analysis Report](../output/Analysis_Report.md). Đã đối chiếu use case/actor và ERD theo hai Draw.io mới; thay bảng UC cũ bằng 30 capability/actor/description từ diagram và cập nhật Related UC ở mục 4; ghi rõ diagram không có mã UC, các actor không đồng nhất, cùng xung đột thuật ngữ membership package với D03. `FE_Project-main` là FE tham khảo cũ; FE tích hợp hiện hành, live DB/provider và triển khai không xác minh. PDF xuất bằng ReportLab vì Windows runtime thiếu LibreOffice; nội dung đã trích xuất và trang PDF đã kiểm tra trực quan. Không chạy test, gọi dịch vụ ngoài hoặc mutation cloud.

**Phát hành attendance09/11 — BẢN TÍCH HỢP ĐÃ KIỂM:** theo Owner, đã push `tai`, mở [PR21](https://github.com/Aditerx/SWP391/pull/21) và hợp nhất main mới, giữ nhóm AI chatbot. Source kiểm `a921bff6a920288f8657721b993383c25a78aa11`, tích hợp main `0bac4ffe7d755471b98400933f6b2600de180926`; bản sao không.env khớp byte source/test/schema. Windows/Java17.0.12/H2: clean verify502run=474PASS/28SKIP/0failure/0error, đóng gói JAR đạt; PostgreSQL17.11 loopback attendance22/22. CI push/PR của source kiểm đã SUCCESS. CI đầu phát hiện test SMTP phụ thuộc logger WARN từ context khác; tái hiện đúng lỗi, fixture tự đặt/khôi phục INFO, giữ expected và code gửi mail; kiểm đích12/12 dưới root WARN. OpenAPI sinh lại từ app tích hợp, bổ sung route escalate đã có trên main, giữ operation attendance. Bằng chứng/hash/XML tại `attendanceRelease20261109` trong [QA](../artifacts/qa/latest.json); source/CI/merge phát hành theo PR21, không suy push/merge thành Render Live. Trước kiểm FE, Supabase cần [migration17](02-DEPLOYMENT-GUIDE.md#attendance-review-20261109) và BE đúng build mới; agent không mutation cloud/chạy lại16. FE/Render/CRUD/live vẫn chưa nghiệm thu. Bước tiếp: Owner kiểm FE theo bản mới và đợt nghiệm thu chung; bố trí UI do FE quyết định.

**AI chat DeepSeek / 08-11-2026:** Them provider `deepseek`, FAQ deterministic theo KB, system prompt + knowledge base UTF-8 co dau, fallback/handoff, metadata va quota AI 30/ngay theo Asia/Ho_Chi_Minh; khong migration. Sua thong nhat binding DeepSeek sang khoa phang `scms.ai.chat.deepseek-*`, trim key va fallback khi key rong/chi whitespace. `mvnw clean test` PASS tren local working tree: 454 tests, 0 failure/error, 28 skip (426 pass); skip khong tinh pass. Test Spring xac nhan binding/header qua HTTP server gia lap va ra soat truong cho moi khoa `scms.ai.chat.*`. Chua kiem provider that, PostgreSQL, FE hoac Render; chi tiet tai [ban giao AI chat](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-11.md).

**Snapshot attendance09/11 trước tích hợp main — BE PASS LOCAL, FE/RENDER CHƯA NGHIỆM THU:** đã triển khai D15/contract correction Coach Active+RECORD_RESULT, đúng phân công/cùng center, từ end đến trước00:00 hôm sau giờ VN; Manager giữ quyền sau hạn. Correct nhận attendanceId hoặc sessionId+memberId, note Member đọc đúng chủ, reason/audit riêng và metadata/codes rõ; giữ check-in, notice.reason, content, credit/lịch/hạn. Finalizer giữ lifecycle tại end; review no-show/chế tài sau cutoff trên state hiện hành, chống lặp, chế tài mặc định tắt. [Migration17/vận hành](02-DEPLOYMENT-GUIDE.md#attendance-review-20261109) có guard/replay, không queue buổi finalized cũ. Nền `42c3ea7194891b69b272250309d0b8151ec07c14` + source/test chưa commit; bản sao không.env khớp workspace, Windows/Java17.0.12/H2 và PostgreSQL17.11 loopback. PG attendance22/22; clean480run=452PASS/28SKIP/0failure/0error, skip không là pass. Đã kiểm response/DB/audit sau commit, biên thời gian, Coach A/B, Manager, eligibility Completed/Expired đúng buổi, record thiếu, note đơn/bulk/rollback/owner, race và thông báo không lặp; giữ guard/test center đã có. Lỗi fixture subtype/sĩ số đã sửa, không nới constraint. OpenAPI sinh từ app local cuối, chỉ6schema attendance đổi; path/operation giữ nguyên. Hash/XML/phạm vi chuẩn tại `attendanceImplementation20261109` trong [QA](../artifacts/qa/latest.json). [Checkpoint](03-TODO.md#attendance-note-correction-proposal-20261109), [bàn giao attendance09/11](../SCMS-BAN-GIAO-FE-ATTENDANCE-2026-11-09.md). Giữ diff đầu lượt và LAN-2 ngày07/11; không sửa FE, gộp R1/R2/R3 hoặc mutation cloud/chạy lại16. Bước tiếp: review/phát hành BE+migration17 đúng phạm vi; FE/Render/CRUD/live vẫn chờ đợt chung Owner đã hẹn.

**Owner hỏi lại Late07/11 — ĐÃ TRA NGUỒN, RULE CHƯA ĐỔI:** Member.pdf trang4 chỉ Có mặt/Vắng; “trễ” ở nguồn gốc còn dùng cho báo/hủy trễ. Lịch sử docs04 tại Git a4f3cd0 §33.1–33.4 ghi source/phương án Late sau10phút; §33.5 ghi Owner chọn Present/Absent, giữ dữ liệu Late cũ. Source hiện cùng rule. Owner báo lần thử UI chỉ còn correct403; chưa có ghi BE200/readback chứng minh Late. [Làm rõ và điều phối R1/R2/R3](03-TODO.md#coach-review-proposal-20261107): R3 đã duyệt/bắt đầu đọc source A/B cùng center; R1 cần giải thích, R2 cần cân nhắc, chưa API mới/DB setup hoặc test mới. Ghi cập nhật LAN-2 theo tiến độ FE, không hỏi lại D15 đã chốt.

**Owner điều phối Coach07/11 — PHẠM VI ĐÃ CHỐT, BẢN SỬA CHỜ DUYỆT:** attendance chờ phản hồi FE; CRUD/FE/Render kiểm sau hoặc gom chung. Lượt rà mới chỉ A/B cùng center, không thêm rà center khác; guard/test center hiện có giữ nguyên. Đã double-check PDF/decision/source/DTO/test và soạn [R1 lọc ngày → R2 nội dung sau buổi → R3 scope Coach](03-TODO.md#coach-review-proposal-20261107). Nghĩa lọc ngày và bản sửa tách nội dung mới là expected đề xuất, chưa triển khai hoặc tự coi được phê duyệt. Phiên là phản hồi khi mất/thu hồi login, xếp kiểm UI cuối; nguồn FE là repo/revision/build Pages, giao FE xác định song song, không chặn BE local. Chỉ docs/QA; không chạy test hoặc sửa BE/FE/DB mới.

**LAN-2/Coach07/11 — ĐÃ SOẠN BÀN GIAO, FE CHƯA SỬA:** [RBAC LAN-2 — Attendance Coach](../SCMS-BAN-GIAO-FE-RBAC-LAN-2-2026-11-07.md) ghi đúng triệu chứng dropdown/detail/form không đồng bộ, API/role/thời gian và tiêu chí nghiệm thu; không chốt sai hàm gửi ID hoặc ghi thay đổi vào LAN-1. [C01–C09 còn mở](03-TODO.md#coach-open-items-20261107) tách UI, kiểm FE/live, bàn giao chuyên môn, full matrix và các điểm BE/contract cần rà: tiến độ sau buổi bị ghép với cửa sổ attendance, filter ngày attendance dùng checkInTime có thể loại Absent null. Đây là review tĩnh có giới hạn; không sửa FE/BE/SQL, không test/mutation mới. Source main/test/schema vẫn khớp snapshot đã kiểm tại QA; các count/SHA cũ giữ đúng phạm vi. Bước tiếp: FE xác định source Pages, nhận LAN-2 và nghiệm thu first-write/readback; chưa xác nhận FE tiếp nhận hoặc hoàn tất các nhóm Coach khác.

**Phản hồi attendance07/11 — XÁC NHẬN UI KHÔNG ĐỒNG BỘ KHI ĐỔI BUỔI, CHƯA CHỐT LỖI HÀM GỬI ID:** Owner đính chính: đổi dropdown09/11 rồi13/11 nhưng vùng điểm danh bên dưới giữ dữ liệu/thao tác cũ. Payload85 có thể đi theo record cũ còn hiển thị, chưa đủ căn cứ quy riêng hàm gửi API chọn nhầm ID. Rút kết luận trước “đã xác nhận hàm ghép sai attendance”; giữ sự kiện85 thuộc session10/ngày05/11 và các lựa chọn buổi mới. Lỗi quan sát là dropdown, dữ liệu/detail và ngữ cảnh thao tác chưa đồng bộ; danh sách Member có thể giống nhau vì cùng lớp, nhưng attendance phải theo đúng buổi. Source FE hiện hành chưa được kiểm, không tự sửa FE/BE/DB. POST403 vẫn phù hợp quyền Coach; first-write/live còn NOT_RUN. [Checkpoint](03-TODO.md#rbac-fe-attendance-feedback-20261107).

**Đối chiếu gốc attendance07/11 — ĐÃ LÀM RÕ NGUỒN:** sau câu hỏi Owner, đã đọc trang liên quan và kiểm ảnh PDF Member/analysis24-10/MVP18-10, rồi truy lịch sử quyết định D15 tại docs04 mục33.3/33.5 (Git `a4f3cd0`). Bản gốc gọi attendance là check-in và có30phút trước/sau; D15 ghi Owner chọn trong giờ/Present-Absent và Manager sửa sau buổi. Endpoint REST lấy từ contract/source, không từ PDF; tên check-in tự nó không chứng minh FE sai. Thu hẹp kết luận trước thành ba request lệch luồng hiện hành, chưa loại trừ lỗi BE ở đường ghi đúng. [Căn cứ và giới hạn](03-TODO.md#rbac-fe-attendance-feedback-20261107). Chỉ docs/QA; giữ toàn bộ diff, không sửa FE/BE/DB/test hoặc hỏi lại D15.

**Phản hồi FE07/11 — ROSTER HTTP200, GHI ATTENDANCE CÒN MỞ:** Owner dùng FE Pages báo GET enrollments lớp10 trả200, UI Coach có roster; Owner bổ sung auth/me Coach có RECORD_RESULT, không first-login; state ghi chưa được kiểm lại. POST correct403/check-in400/root attendances405 được tách theo [checkpoint attendance](03-TODO.md#rbac-fe-attendance-feedback-20261107): quyền correction khác quyền Coach ghi buổi, check-in cửa khác attendance ca, root không có POST. Owner bổ sung body400 LOBBY_CHECKIN_DISABLED nên đã xác định check-in cửa bị chặn đúng D15; phiên Coach bị403 correct là đúng role, còn POST root405 là FE dùng tuyến không có method ghi. Agent đọc health cùng BE thấy200/build `63fb46c1e150527abf577e2f2a0c520a563bad5c`; đây là health public riêng, không là authenticated acceptance/migration Google. PR19 đã merge, CI branch/PR/main đã đạt theo Git; source checkout `42c3ea7` có cùng tree với merge `63fb46c` trước lượt docs này. Chỉ ghi nhận evidence/contract, không sửa FE/BE/SQL hoặc chạy test mới. Chờ dữ liệu request/response và Owner kiểm từng nhóm trước khi đề xuất tiếp.

**Phát hành RBAC07/11 theo yêu cầu Owner:** code đã push lên tai tại `cbfa72cd313a5c9630eb1712f2477966b09bbe62`; [PR19](https://github.com/Aditerx/SWP391/pull/19) là nguồn trạng thái CI/merge vào main. Không suy push/merge thành Render đã Live; health quan sát trước phát hành còn build `cc5e18f153321e85ab8aa8802a7f283753a920e0`. [Kiểm phản hồi FE](02-DEPLOYMENT-GUIDE.md#rbac-fe-verification) chỉ bắt đầu khi đúng build mới; tiếp tục chờ Owner báo thành công/không lỗi rồi mới sắp xếp, giải thích và đề xuất phần tiếp theo. Không sửa FE hoặc chạy lại SQL16.

**Sửa theo thiết kế RP-04/RP-36 và giáo án lớp07/11 — PASS LOCAL, CHƯA ĐÓNG TOÀN AUDIT:** Admin mới được quản lý target Admin/Manager, kiểm cả role hiện tại và role mới qua users/staff/status/role/cấp lại mật khẩu; audit ghi actor thật. Ghi giáo án/đánh giá yêu cầu Coach + authority chuyên môn + phân công, chặn cả role khác được thêm authority. Member đọc giáo án chung Normal qua Registered+Paid hợp lệ và đúng center; không mở dữ liệu lớp/người khác. [Checkpoint hiện hành](03-TODO.md#design-corrections-20261107). Source nền `91e49a198ca26badc5b994e73db3f2961a5cceab` + main/test chưa commit khớp các bản sao không.env, Java17.0.12/H2/PostgreSQL17.11 loopback. Đối chứng trước sửa tái hiện3lỗi assertion; clean457run=429pass/28skip/0failure/0error; guard25/25 trên cả H2 và PG, workflow PG22/22. Lượt PG đầu lỗi fixture thiếu join_date trước nghiệp vụ; đã sửa fixture giữ constraint và kiểm lại, không tính đó là PASS phân quyền. Hash/XML/giới hạn tại `rbacDesignCorrections` trong [QA](../artifacts/qa/latest.json); OpenAPI khớp snapshot, không sinh bản trùng. Không sửa FE/SQL, không deploy/cloud hoặc chạy lại SQL16 Supabase; ảnh lớp23 chưa kiểm lại live và Google migration Supabase chưa xác nhận. Tiếp theo: RP-13 giá lúc công bố, RP-43 scope/provenance log, sân/môn và lookup/matrix; ca lịch sử/đảo quyền và đầy đủ BE-01–BE-07 còn mở.

**Đính chính P1–P7 đã áp dụng:** đã rút bảng A/B theo [đối chiếu nguồn thiết kế](03-TODO.md#pending-policy-20261107); không hỏi lại yêu cầu đã có, không coi characterization Manager ghi thành công là nghiệm thu RP-36. Nhóm trên sửa các sai lệch đã rõ; các kết quả bên dưới là snapshot theo thời điểm, không thay trạng thái hiện hành.

**Snapshot guard trước sửa RP-04/RP-36 — PHẦN ĐÃ DUYỆT LOCAL07/11:** theo Owner, đã sửa TK01/TK02 guard, TK03 và phần baseline rõ policy của TK04; cập nhật contract, README/nhãn tài liệu cũ TK06 và dependency Google/16 TK12. Hai GET enrollment cho Coach được phân công dùng cùng kiểm service; Coach khác/khác center vẫn403, không mở quyền ghi. Thêm khóa target trước kiểm center/subtype và kiểm race sau commit; phiên đọc lại quyền, hủy khi role đổi và thực thi thu hồi password. Source nền `91e49a198ca26badc5b994e73db3f2961a5cceab` + main/test chưa commit, Java17.0.12/H2/MockMvc/local HTTP; source chính/test khớp hai bản sao không.env. Đối chứng source trước sửa tái hiện5lỗi đúng expected; test guard mới18/18, clean450run=422pass/28skip/0failure/0error. PostgreSQL17.11 loopback riêng sau Google13/QueryDR15/retire16 đạt workflow22/22; đây là hồi quy workflow cũ, không thay test18guard mới trên PG. Hash/XML/fixture và giới hạn chuẩn tại `rbacPriorityGuards` trong [QA](../artifacts/qa/latest.json), raw artifacts/runs/rbac-guards/2026-11-06. OpenAPI sinh từ cùng app PG khớp snapshot hiện có, không sinh bản trùng. [Chi tiết và lựa chọn P1–P7](03-TODO.md#priority-guards-20261107). Các fixture chỉ gắn authority request trước đây nay có quyền thật trong DB; actor Inactive403 được tách khỏi staff đủ quyền đăng ký cho Member Inactive400; không đổi expected owner/center/phân công. Audit scope/provenance, các policy thiếu và TK08/TK11 còn mở; chưa đóng trọn TK04 hoặc BE-01–BE-07. Không sửa FE/SQL hoặc truy cập cloud/chạy lại SQL16 Supabase. Ảnh lớp23 chỉ chứng minh hai403 ở FE proxy tới Render; chưa có auth/me/assignment/SHA runtime và chưa kiểm lại live. Bước tiếp đã đính chính: dùng [đối chiếu lại P1–P7](03-TODO.md#pending-policy-20261107), không chọn bảng A/B cũ; triển khai/deploy theo phạm vi riêng, migration Google Supabase vẫn chưa xác nhận.

**Snapshot rà thiết kế toàn hệ thống06/11 — ĐÃ GHI ĐỀ XUẤT:** theo yêu cầu mới của Owner, mở rộng từ checkpoint BE-01 sang [đối chiếu thiết kế và13phát hiện TK01–TK13](03-TODO.md#design-review-20261106). Ưu tiên: guard target Admin qua tuyến Member/Staff, actor/phân công khi ghi giáo án-đánh giá, thu hồi quyền phiên và scope center baseline; tách rõ tài liệu cũ đã bị thay với policy còn cần chốt. Review tĩnh local trên HEAD `91e49a198ca26badc5b994e73db3f2961a5cceab`, giữ toàn bộ diff đầu lượt. Chỉ cập nhật docs00/docs03; không sửa BE/FE/SQL, BE test/HTTP/DB/cloud NOT_RUN, không chạy lại SQL16 hoặc xác nhận Google cloud. Chưa đóng BE-01–BE-07. Bước tiếp: Owner chọn nhóm sửa và chốt các khoảng trống chính sách; giữ các quyết định0/0.1/Dxx đã có.

**Chuyển session RBAC06/11:** [điểm dừng và prompt session mới](03-TODO.md#ban-giao-session-rbac-20261106). Mục0/0.1 xong BE local; BE-01–BE-07 đều còn mở, phần bàn giao trước không thay hoàn tất cả mục. Lượt kế tiếp đề xuất rà riêng BE-01; lần ghi bàn giao này chỉ sửa docs, không thêm kết quả BE/DB hoặc deploy.

**Snapshot BE-01 review06/11 — khoảng trống lúc review:** đã bắt đầu [ma trận quan sát contract/source/test](03-TODO.md#be01-review-20261106), dừng trước đổi gate vì ba GET roles/permissions hiện cho cả năm role có phiên đọc, nhưng chưa có policy Owner xác nhận. Ghi thêm khoảng trống scope các đường đọc users/members/staff/audit; chưa nghiệm thu runtime hoặc đóng BE-01. Phạm vi review tĩnh local nhánh tai, HEAD `91e49a198ca26badc5b994e73db3f2961a5cceab`; đầu lượt chỉ docs00/docs03 có diff đã giữ. Owner báo PR18 merge main `cc5e18f`, chưa đồng nhất checkout này với main/deploy. Chỉ sửa docs; BE test/HTTP/DB/cloud NOT_RUN trong lượt này, không chạy SQL16 hoặc xác nhận migration Google Supabase. Hướng tiếp tục đã đính chính tại checkpoint hiện hành: tách lookup role khỏi đọc/sửa full matrix; chỉ hỏi tình huống thực sự thiếu căn cứ, không hỏi lại0/0.1.

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

**Chuẩn bị Chat AI local07/11:** Trên nhánh `feature/ai-chatbot-env`, baseline trước sửa397 tests/369 pass/28 skip/0 lỗi; sau sửa402 tests/374 pass/28 skip/0 lỗi, `clean test` BUILD SUCCESS. Source tree chưa commit, dựa trên HEAD `b033ab7719bb124ac4fce9c1e0f6712e05c4e5c9`. PostgreSQL17.11 local service đang chạy nhưng chưa tạo DB; local connection yêu cầu mật khẩu chưa được cung cấp. FE hiện hành chưa xác định; không lấy `FE_Project-main` làm nguồn. Chưa chạy backend/FE local hoặc Anthropic thật. Bàn giao một luồng tại [AI Chat07/11](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-11.md).

| Việc còn mở | Trạng thái | Điều kiện tiếp tục |
---

## 📜 PHẦN II: LỊCH SỬ HOÀN THÀNH (HISTORY)

### [12/11/2026] Sinh F3-v3 và rà trạng thái cấp mật khẩu tạm

Owner yêu cầu hiện thực đề xuất Refund; đã tạo/mở lại F3-v3 bằng imagegen, sửa connector nháp và đồng bộ link, giữ bản gốc/v2. Rà source tạo/cấp lại/first-login/email với đúng ca QA lịch sử; tách BE implementation/kiểm first-login local khỏi gửi mật khẩu qua hộp thư và FE hiện hành chưa nghiệm thu. Bằng chứng/giới hạn ở Active/QA; không sửa BE/schema, Maven/DB/cloud/provider hoặc tiếp tục Session3.

### [12/11/2026] Tách checklist F1–F5 và làm rõ notification/Refund

Thay note Owner vừa tách bằng checklist hành động F1→F5 để duyệt; SCRIPT dẫn bản thao tác duy nhất và note private đồng bộ thời gian. Review tĩnh phân biệt notification in-app với email xác thực/cấp mật khẩu; làm rõ nghĩa vụ hoàn từ chứng từ, Member cung cấp người nhận, Manager duyệt/đối soát và Receptionist thực hiện/ghi chứng cứ. Đề xuất bổ sung đầu vào và bước chuyển ngân hàng trên F3; chưa sửa ảnh hoặc nghiệp vụ. Phạm vi/bằng chứng/việc còn lại ở Active/QA, không có Maven/DB/cloud/provider hoặc nghiệm thu FE mới.

### [12/11/2026] Sinh và review F1/F3, kết nối tab demo chỉ đọc

Tạo hai PNG v2 theo đề xuất Owner duyệt, giữ ảnh gốc/swimlane/phong cách và sửa connector Refund sau review. Playwright đã đọc/chụp màn login VibeSports, chưa submit hoặc kiểm nghiệp vụ có phiên. Hoàn thiện lời dẫn11phút+4phút hỏi đáp và note copy/paste private; fixture/rehearsal còn mở theo Active/QA. Chỉ ảnh/tài liệu, giữ mọi diff/deletion; không Maven/DB/cloud/provider/ZIP hoặc tiếp tục Session3/tích hợp Session1–2.

### [12/11/2026] Kết nối browser cho VS Code và đơn giản hóa note demo

Đối chiếu tài liệu OpenAI/Microsoft/Chrome và thảo luận cộng đồng, xác định giải pháp gắn tab có sẵn thay vì chỉ đọc HTTP. Owner chọn Playwright MCP; đã thêm cấu hình người dùng, pin package/giới hạn tool quan sát và kiểm protocol discovery, chưa chọn tab hoặc thao tác FE. Rút note thành dữ liệu người trình diễn, đưa kiểm kỹ thuật về docs02; bỏ prompt trong docs03 theo yêu cầu gửi chat. Không merge/deploy/reset/provider hoặc có nghiệm thu BE/business mới.

### [12/11/2026] Site VibeSports và chuẩn bị session demo15phút

Owner duyệt mô tả bàn giao theo luồng, cập nhật site/env và tạm ưu tiên bản BE/FE đang chạy để demo hôm nay. Đã kiểm CORS/health/redirect công khai, phân biệt browser blocked và authenticated business chưa kiểm; đọc F1–F5, đề xuất khung11phút+4phút hỏi đáp, sửa wording sơ đồ và chuẩn bị note private để copy. Fetch/rà Google/chat main mới, chỉ preview conflict docs00/docs01, chưa merge/deploy/reset/provider. Session3 và bàn giao/tích hợp Session1–2 chưa tiếp tục; bước tiếp là rehearsal trên UI/fixture runtime trong session demo riêng.

### [12/11/2026] Owner tự đóng gói bộ bàn giao

Owner yêu cầu không tạo ZIP; đã bỏ yêu cầu agent xuất ZIP khỏi checklist/kế hoạch3.3 và xóa đúng archive agent tạo trước đó. Giữ sáu bản theo luồng và bảng liên kết trong KIEM-TRA; mô tả cách tổ chức được trình lại để Owner duyệt, chưa nhận là đã duyệt. Chỉ kiểm tài liệu/JSON/liên kết, không có nghiệm thu BE/FE mới hoặc bắt đầu Session3.

### [12/11/2026] Gom bộ bàn giao Session1–2 và chuẩn bị checklist cuối Session3

Chuyển sáu bản hiện hành vào SCMS-CAP-NHAT, giữ từng luồng/năm mục và thêm KIEM-TRA làm mục lục/đối chiếu FE; cập nhật liên kết docs/QA, giữ một bản nguồn cho mỗi luồng. Session3 vẫn NOT_STARTED; chỉ ghi đầu ra phải bổ sung ở3.3, chưa tạo bàn giao implementation giả. Kiểm liên kết/cấu trúc/ZIP và hash nguồn không đổi theo QA; không chạy lại Maven/HTTP/PG hoặc thao tác cloud/provider.

### [12/11/2026] Đóng Session2 sau Owner skip demo2.3

Owner chọn skip demo2.3 và đóng Session2; cả2.1/2.2/2.3 giữ kết quả BE/bàn giao riêng và SKIPPED_BY_OWNER, không thành browser PASS. Cập nhật Active/docs03/handoff/QA; đối chiếu hash source/schema/fixture/OpenAPI không đổi, không chạy lại test hoặc HTTP/DB. Checker tổng thể còn BLOCKED theo phạm vi đã ghi, FE/Render chưa nghiệm thu; Session3 chưa bắt đầu, không cloud/reset/deploy/provider.

### [12/11/2026] Shared plan history2.3 — BE local đã kiểm, chờ demo/skip

Owner chốt Cancelled ngoài shared read, giữ own history; đã đồng nhất Registered/Completed/Paid hiệu lực ở list/detail và loại Refund Completed. Kiểm H2/PG/clean cuối/response/DB/audit đạt, sửa nullable metadata khớp null payload hiện có, giữ write/Invoice/Enrollment/transfer/refund và không snapshot/versioning. Bàn giao riêng có flow FE và Swagger hai Member; chờ demo/skip2.3 trước đóng Session2, không tự làm Session3.

### [12/11/2026] Skip demo R2 và bắt đầu shared plan2.3

Owner chọn skip demo2.2, giữ bằng chứng BE và ghi SKIPPED_BY_OWNER riêng. Đã rà/trình tác động list/detail và ví dụ Cancelled; Owner chốt Cancelled ngoài shared read, own history giữ nguyên scope. Triển khai và kiểm riêng2.3, không mở hủy/hoàn sau Ongoing hoặc Session3.

### [12/11/2026] Triển khai R2 — chỉ sửa content result có sẵn sau end

Owner duyệt PATCH/resultId/content và yêu cầu bàn giao tác động flow; đã giữ POST/correction, thêm guard/khóa/audit riêng và kiểm H2/PG/clean theo Active/QA. Sửa gap lookup superseded bị ẩn, giữ no-op/rollback và không tạo Attendance; contract/OpenAPI cùng handoff riêng có bảng trước–sau/cách FE lưu từng hàng/Member đọc lại. Runtime Swagger Clock thực đã chuẩn bị với cửa07:30–trước00:00; chờ Owner demo/skip2.2, chưa chuyển2.3 hoặc Session3. Checker contract tổng thể còn vướng helper tham chiếu schema root đã xóa; không đổi deletion hoặc nhận PASS cho check đó.

### [12/11/2026] Rà R2 và trình mutation content tối thiểu

Đối chiếu POST/result/attendance, D15/RP-36, schema và cơ chế khóa/audit; ghi proposal PATCH content-only trong docs03 gồm DTO/error/guard tác giả và phân công, cutoff, no-op/transaction, fixture/nghiệm thu. Chỉ docs/QA, chưa sửa API hoặc chạy test BE mới; giữ R1 đã kiểm/skip và mọi diff/deletion. Chờ duyệt hình dạng mutation theo prompt trước implementation2.2, chưa chuyển2.3/Session3.

### [12/11/2026] Skip demo R1 và chuẩn bị prompt tiếp Session2

Owner chọn `skip demo R1`; giữ nguyên bằng chứng BE, ghi SKIPPED_BY_OWNER cho demo và bỏ điểm chờ sau2.1. Prompt tại docs03 hướng dẫn2.2 rồi2.3, kiểm/bàn giao từng mục, giữ checkpoint duyệt mutation R2, quyết định Cancelled và demo các mục mới. Chỉ sửa docs/QA; source R1 đối chiếu hash không đổi, chưa triển khai2.2/2.3, không chạy lại BE/cloud.

### [12/11/2026] Triển khai R1 — lọc theo ngày buổi, giữ lobby legacy

Owner chốt phương án1 sau khi xem tác động FE; đã sửa predicate service và bỏ query không dùng, giữ shape API/các luồng ghi. Kiểm HTTP trước/sau, H2/PG và clean đạt theo Active/QA; cập nhật mô tả Swagger/contract và bàn giao riêng. Demo local/readiness có JSON đăng nhập riêng, browser/Owner R1 NOT_RUN; dừng chờ thử/skip trước2.2, không cloud/provider/deploy.

### [12/11/2026] Skip demo1.2/1.3 và bắt đầu giải thích R1

Owner skip hai demo, bằng chứng BE giữ nguyên; thêm quy tắc link local đầy đủ để dễ review. Đối chiếu đường gọi và field response attendance, ghi tác động ngày Session/check-in vào docs03 trước chọn nghĩa filter. Chưa sửa BE hoặc chạy test mới; chờ Owner chốt R1, không mở rộng sang2.2/2.3/cloud.

### [12/11/2026] Làm rõ đăng nhập local và ghi nhận URL FE hiện hành

Bổ sung hướng dẫn Swagger và payload riêng để copy cho login.manager1; kiểm lại login/auth-me/GET scope local, không đổi trạng thái browser/FE hoặc số test BE của11/11. Ghi URL FE do Owner xác nhận tại docs02, chưa xác minh build/API origin; sửa lỗi mã hóa và câu trạng thái cũ trong tài liệu vừa tạo. Source BE/schema/Clock và dữ liệu cloud không đổi.

### [11/11/2026] Session1.3 — đồng bộ room name50/location100

Đã bổ sung DTO/JPA/OpenAPI/contract theo schema project; HTTP H2/PG sau commit và clean đạt theo Active/QA. Fixture PG trùng tên đã đổi tên riêng, giữ index/expected; lỗi PUT duplicate500 ghi cho error catalog3.2. Bàn giao room riêng, demo Owner/browser NOT_RUN; không cloud/provider/deploy hoặc bắt đầu Session2/3.

### [11/11/2026] Session1.2 — giới hạn staff, giữ Admin-null và own/author

Đã thêm center predicate AND filter cho GET training-results, kiểm actor Active/role/quyền và giữ own/author history. HTTP H2/PG + clean đạt theo Active/QA; bàn giao scope riêng và chuẩn bị demo đọc local. Owner skip demo1.1 và cho hoàn tất1.2/1.3 trong cùng lượt; demo1.2 chưa thực hiện, không ghi browser PASS.

### [11/11/2026] Session1.1 — bảo vệ content, giữ attendance của staff

Sửa guard chung cho POST training-results đơn/bulk theo D15/RP-36; từ chối content non-null của non-Coach và Coach thiếu RECORD_RESULT, giữ null/omitted và các guard attendance. Kiểm HTTP có phiên/DB/audit commit trên H2/PG và clean đạt theo Active/QA; chuẩn bị Swagger/fixture demo local với Clock thực. Bàn giao riêng content11/11; Owner đã skip demo1.1 và cho tiếp1.2/1.3, browser chưa nghiệm thu. Không đổi schema hoặc tác động cloud/provider/deploy.

### [11/11/2026] Ghi rule TrainingResult và chặn fixture thiếu status

- Bổ sung README phân biệt content/attendance, NOT NULL/default Present và phạm vi R2 chỉ sửa result có sẵn; ghi nguồn compiler luôn truyền state tường minh và quy tắc thay model phải được rà riêng.
- Validator design chặn thiếu/null/state ngoài CHECK trước sinh SQL; kiểm offline và staging đối chứng đạt theo Active/QA. Canonical schema/fixture cùng byte, không chạy DB hoặc thay rule/schema để hỗ trợ result thiếu.
- Chỉ maintenance scripts/docs; giữ mọi diff Owner, BE chưa bắt đầu. Bước tiếp theo kế hoạch ba session là1.1 content guard, không gộp các mục còn lại.

### [11/11/2026] Chia ba session P2.2/P2.3 và rà trước triển khai

- Ghi kế hoạch từng mục/kiểm agent/demo FE–Swagger–Owner skip/bàn giao đối chiếu; phân biệt ba session mới với Normal Phase1 đã phát hành và khung hai phase cũ. Chưa bắt đầu sửa BE.
- Xác nhận Admin-null global read theo contract/helper học tập; thu phạm vi shared plan về đọc Completed/Paid trên API sẵn có, không snapshot/bảng mới, Cancelled không tự mở. Giải thích nguồn room50/100, audit tái dùng và smoke không yêu cầu reset thêm.
- Đối chiếu hai DA_CHOT/Dxx/contract/handoff/CM-FIX/SCRIPT/SYSTEM-DATA/DB/source, ghi điểm mâu thuẫn/thừa và tác động tối thiểu tại docs03; các quyết định đã đồng ý ghi docs04. QA chỉ ghi review/tài liệu, không tạo test/live PASS; giữ mọi thay đổi Owner.

### [11/11/2026] Giải thích đề xuất P2.2/P2.3 trước duyệt

- Làm rõ bốn nhóm sửa kỹ thuật, các lựa chọn nghiệp vụ và phần thiếu kiểm chứng; đề nghị ưu tiên content→center→room→API, không coi đồng ý hướng là lệnh triển khai trước mô tả.
- Sửa ghi chú R2 không đúng với NOT NULL/default Present của model hiện tại; đề xuất content-only tối thiểu chỉ cập nhật result có sẵn. Phân biệt quyền đọc shared plan với snapshot lịch sử và tách lỗi HTTP409/body400 đã ghi nhận khỏi sửa annotation.
- Chỉ review source/tài liệu, không chạy test/HTTP/DB hoặc sửa BE/API/policy; bằng chứng mới tại Active/QA. Bước tiếp duyệt từng nhóm, giữ P2.4/browser/live và các tích hợp sau riêng.

### [11/11/2026] Xác nhận contract ba API Normal Lifecycle cho CM-FIX-15B

- Bổ sung docs01 nguồn version từng POST, enum chính xác, quyền Manager, Waiting, lỗi409/400 và ví dụ GET→POST. Làm rõ proposal.version khác lifecycleVersion sau apply và handler legacy không luôn trả field code.
- Chỉ đối chiếu source/DTO/OpenAPI/error handler; không sửa BE/policy, không có HTTP/browser/cloud mới. Bằng chứng `normalCmFix15bContract20261111` trong QA; FE tích hợp và kiểm trên đúng build/phiên/fixture, không suy đây là nghiệm thu UI.

### [10/11/2026] Review P2.2/P2.3 và khoanh nhóm sửa

- Đối chiếu hai DA_CHOT, Dxx, contract/handoff/SCRIPT/SYSTEM-DATA cùng controller/service/query/DTO/schema; liệt kê phần E1–E8 tái dùng, F01–F11 và nhóm sửa G1–G4 tại docs03. Matrix tách method/path/năm role/authority, profile owner/assignment/center/state, response/readback/error/deprecation/readiness; chưa nghiệm thu toàn bộ BE-01–BE-07.
- Kiểm local service/DTO trên bản sao không.env xác nhận filter ngày/R2, room length và non-Coach clear content; guard buổi hủy/invalidated đã có. Kết quả/môi trường/source/giới hạn và lỗi fixture đầu lượt ghi ở Active/QA, không lấy probe hoặc review tĩnh làm HTTP/PG/browser PASS.
- Giữ mọi thay đổi có sẵn, không sửa BE/API/policy, schema/Clock/fixture, không cloud/live/provider/reset/deploy/push. Bước tiếp Owner duyệt G1 và lựa chọn R1/R2/shared-history/PUT-quản trị cuối cùng; browser/tích hợp sau vẫn nhóm riêng, smoke trước reset giữ lịch sử.

### [10/11/2026] Rà bàn giao và chuẩn bị session P2.2/P2.3

- Bổ sung điểm vào docs03 với phần đã giải quyết/bằng chứng reuse, sáu nhóm P2.2 còn mở và matrix BE-01–BE-07 cho P2.3; yêu cầu tách thiếu triển khai, thiếu kiểm chứng và lựa chọn nghiệp vụ thật sự cần duyệt, giữ FE quyết định giao diện.
- Sửa hai handoff10/11 còn ghi chưa phát hành/Render, câu contract GET RBAC chưa đổi và giả định SCRIPT dùng DB sau smoke cũ. Làm rõ kết quả trước reset khác trạng thái sau xác nhận Owner chạy lại; không yêu cầu phát hành/reset lại hoặc chờ browser để mở review BE.
- Chỉ review tĩnh/tài liệu, không sửa BE/schema, không chạy test BE/HTTP/cloud/provider mới. Bằng chứng `p22P23SessionHandoff20261110` trong QA; bước tiếp mở session review từ docs03, trình nhóm sửa trước triển khai policy/API mới.

### [10/11/2026] Owner xác nhận thực thi lại bundle sau dò cuối

- Owner báo chạy lại schema demo thành công; ghi nhận riêng trong QA, không suy từ lời xác nhận thành đã đọc summary/DB hoặc smoke mới. Bundle/source không đổi trong lượt này.
- Các ID/phiên và fixture đã tiêu thụ trước reset là lịch sử; bước tiếp lấy phiên mới/auth-me/readback. Làm rõ migration_key là phiên bản, anchor_date là ngày cài và không bắt buộc reset theo từng ngày demo.

### [10/11/2026] Dò cuối bundle với Normal Lifecycle và Refund DA_CHOT

- Kiểm canonical SQL trên DB local mới, đối chiếu dữ liệu/quan hệ/state với cả hai DA_CHOT và D02/D10/D12/D13/D15; giữ nguyên bundle/fixture và DATA DESIGN, không phát hiện lỗi trong phạm vi kiểm. Bằng chứng nguồn/môi trường/kết quả và giới hạn tại `demoBundleFinalReview20261110` trong QA.
- Sửa mô tả README từ checkpoint bộ rỗng sang bộ demo hiện hành; liên kết baseline root đã đổi tên, làm rõ sinh SQL khác thực thi reset và mã hóa đơn/timestamp runtime không cố định. Giữ mọi thay đổi có trước.
- Chưa chạy Supabase/HTTP/BE/browser/provider mới; Owner thực thi đúng bundle/ngày nếu chọn reset, rồi lấy phiên mới và smoke response/readback. Không suy local PASS thành nghiệm thu toàn bộ runtime hoặc FE; P2.2/P2.3 sau bàn giao như đã chốt.

### [10/11/2026] Hoàn thiện SCRIPT-DEMO và SYSTEM-DATA để bàn giao

- Theo toàn bộ đề xuất Owner duyệt, bổ sung giải thích bundle/quá trình tạo/cách dùng, mục đích SYSTEM-DATA ở cả hai file; ngày cài bắt buộc, giờ demo tùy chọn và không thay thời gian thực. Dùng DEMO-xx cho case, giữ Dxx cho quyết định.
- Sinh index N01–N12/tài nguyên/lịch/tài khoản/proposal/Refund từ cùng fixture local; giữ nguyên toàn bộ bảng chi tiết và byte bundle/fixture. Kiểm tái sinh/alias/FK/ID đổi/thứ tự đảo, syntax và links; bằng chứng chuẩn `demoHandoffFinalization20261110` trong QA.
- Không triển khai P2.2/P2.3, BE/cloud/SQL reset/provider/browser mới. FE quyết định cuối gợi ý chuẩn bị/giao diện; bước tiếp là Owner bàn giao hai file rồi review hai DA_CHOT/Dxx/contract/source/test để duyệt việc tiếp.

### [10/11/2026] Chuẩn bị kịch bản demo và công cụ anchor

- Theo Owner, tạo kịch bản Normal/Lifecycle/Refund theo actor/precondition/thao tác/response/readback và fixture đã tiêu thụ; phần FE chỉ gợi ý để FE duyệt, quyết định cuối thuộc FE. Rule vận hành ở docs02, AGENTS/README/TODO dẫn cùng nguồn.
- Bắt buộc anchor rõ ràng, guard ngày VN trước DDL, metadata ngày cài riêng phiên bản, staging không ghi đè canonical; plan nêu cửa thời gian và NOT_RUN, không tự chạy cloud. Bằng chứng chuẩn `demoScriptAnchorPreparation20261110` trong QA; mốc và phạm vi ở đầu Active.
- Kiểm offline/SQL local/HTTP local và exporter nguyên trạng; guard từ chối ngày cũ/tương lai, exporter chặn sai anchor/fixture đã ghi, giữ sequence và thành phần ngoài SCMS. Source BE không đổi, không reset/deploy/provider hoặc kiểm browser mới.
- Bước tiếp: FE duyệt gợi ý/build; chọn ngày cài và chuẩn bị đúng bundle, quyền thực thi cloud xét riêng. SYSTEM-DATA tiếp tục DATA DESIGN, không là dump Supabase; giữ trạng thái smoke cũ và các case ngoài cửa NOT_RUN.

### [10/11/2026] Smoke Phase1 trên Render/Supabase demo

- Xác minh đúng deployTargetCommit/DB và migration demo; đăng nhập mới từng role, auth-me cùng cookie, kiểm owner/Coach assignment/Admin-only catalog và phạm vi Refund/proof/history. Bằng chứng chuẩn `normalPhase1Smoke20261110` trong [QA](../artifacts/qa/latest.json); phạm vi và mốc build ở đầu Active.
- Đã tiêu thụ fixture Cash N01, xác nhận N01/N02, chuyển Member2 N05→N11 và Refund3 mô phỏng DEMO; response cùng readback/Invoice gốc/retry/report/audit/thông báo đạt. Danh sách ID/state sau kiểm chỉ giữ ở Active/QA; không reset hoặc sửa dữ liệu để làm đẹp.
- Giữ ba lệch oracle ban đầu cùng căn cứ kiểm lại404/độ chính xác timestamp/SLA hết ngày7; không có lỗi BE được xác nhận trong nhóm đã chạy. Không sửa source BE, không chạy Maven mới, không deploy/provider/chuyển tiền thật.
- CORS HTTP/cookie đạt; browser BLOCKED do công cụ không có trình duyệt, commit FE chưa xác định. Bước tiếp: browser theo đúng URL/build và fixture sau smoke; tự start/close/biên thời gian cùng matrix ngoài phạm vi còn NOT_RUN. SYSTEM-DATA vẫn DATA DESIGN.

### [12/11/2026] DeepSeek AI Chat đọc dữ liệu SCMS bằng tool cố định

- Thêm 6 hàm chỉ đọc cho gói đang bán, bộ môn active, lớp Normal đang Open, subscriptions riêng, sessions sắp tới và tối đa5 invoice gần đây. Member ID chỉ lấy từ context backend; DTO chỉ có field phục vụ câu trả lời, truy vấn phân trang, output giới hạn2.000 ký tự và transaction read-only.
- DeepSeek chạy riêng vòng function calling/role tool, giữ thinking disabled, non-stream và retry; cộng token usage, giới hạn số vòng/deadline và map lỗi về FAQ. Không đổi nhánh FAQ/mock/Anthropic, handoff hoặc quota. Cấu hình opt-in mặc định false, thêm giới hạn rows/rounds và system instruction chống prompt injection từ DB.
- Cập nhật docs01 mục16 và env mẫu/Render. Unit test xác nhận hai Member không trộn data, không có memberId trong schema/args, tool loop, giới hạn vòng, fallback và cờ opt-in.
- `clean test` local Java17: 566 tests,534 pass,32 skipped,0 failure/error; test chat/tool24/24 PASS. SHA-256 manifest 19 source/test files `4334bc5d018e640e269007fd6606d19c5c5fefcd7f8ee43a1af4ec6b4885e4e5`; chi tiết trong [QA](../artifacts/qa/latest.json).
- Chưa chạy truy vấn trên PostgreSQL fixture, chưa gọi DeepSeek thật hoặc kiểm FE/browser/Render. Bước tiếp: kiểm cả sáu truy vấn trên PostgreSQL cô lập với Member A+B, sau đó mới thử provider thật.

### [07/11/2026] Chuẩn bị khung môi trường Chat AI local

- Tạo nhánh `feature/ai-chatbot-env` từ HEAD `b033ab7719bb124ac4fce9c1e0f6712e05c4e5c9`. Thêm `AiCompletionClient`, fixed mock, Anthropic Messages adapter dùng Spring `RestClient`, config timeout/model/token/system prompt, giới hạn và bỏ tin có dấu hiệu nhạy cảm; lỗi/không có responder chuyển STAFF. Không gọi provider thật.
- Giữ feature flag mặc định false và handler mới STAFF cho tới khi Owner chốt; thêm Member handoff `POST /api/chat/conversations/{id}/escalate`. Tái dùng metadata/chat schema, không migration/không dùng `ai_consultations`; cập nhật API/config/handoff ngày07/11.
- Baseline `clean test`: 397 tests,369 pass,28 skip,0 fail/error. Sau thay đổi `clean test`: 402 tests,374 pass,28 skip,0 fail/error; BUILD SUCCESS. Test unit/API không là nghiệm thu PostgreSQL, browser hoặc provider.
- Theo D11/D19: PostgreSQL17.11 local service và port5432 hoạt động, nhưng local role cần password; không kết nối DB, không đọc `.env`, không tạo DB/schema. Chưa xác định source FE hiện hành; backend health, auth `/api/auth/me`, polling, FE browser và Anthropic đều NOT_RUN. Chi tiết/việc tiếp tục tại [handoff AI Chat](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-11.md).

### [08/11/2026] Sửa binding key DeepSeek

- Chuẩn hóa năm thuộc tính DeepSeek trong `application.properties` sang tên phẳng khớp `ChatAiProperties`; cập nhật condition của client, HTTP config, missing-key client và test sang `deepseek-api-key`.
- Trim key trước khi dùng; key rỗng hoặc chỉ whitespace không tạo DeepSeek HTTP client. Test Spring nạp cấu hình phẳng, xác nhận `ChatAiProperties`, request header chính xác qua server giả, và thiếu key chọn missing-key client. Thêm test đối chiếu mỗi khóa `scms.ai.chat.*` với field bindable; phát hiện và bổ sung `newConversationHandler`.
- `clean test` PASS local working tree: 454 tests,426 pass,28 skip,0 failure/error. Không gọi dịch vụ DeepSeek thật; chưa kiểm key/provider thật, PostgreSQL, FE hoặc Render. Chi tiết tại [handoff AI Chat](../SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-11.md).

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

- 13/11/2026 — Lượt phát hành lại Session1–3: CI/Render/scoped smoke đạt, không rollback. Bằng chứng, source và giới hạn tại Active/QA; giữ toàn bộ failure lượt đầu, demo skip, không cloud SQL/provider/FE/ZIP.

- 13/11/2026 — Mở lại một lượt phát hành theo Owner: áp lại source đã revert trên worktree mới, kiểm smoke transport và Session1–3 PG đầy đủ theo Active/QA; giữ baseline/forward revert của lượt này. Chờ CI/merge/Render; không cloud SQL/provider/demo/ZIP.

- 13/11/2026 — Mở đợt phát hành theo Owner, tích hợp main mới với Session1–3 trên worktree cô lập, giải conflict docs00/QA giữ hai phía và sinh lại OpenAPI từ app gộp. Predeploy local/PG/package/baseline có phiên đạt theo Active/QA; chưa coi CI/deploy mới là PASS. Giữ toàn bộ bundle/demo/holiday/deletion, chuẩn bị smoke và rollback code, không cloud SQL/provider.

- 13/11/2026 — Hoàn tất3.3 local/readiness và KIEM-TRA: thêm kiểm Refund/notification/timeout, PG current RBAC/workflow, sửa helper fingerprint cho deletion đã có và chốt checker offline đúng giới hạn. Bằng chứng/count/source/NOT_RUN tại Active/QA; demo skip toàn bộ, Owner tự ZIP. Tiếp review rồi phạm vi FE/live riêng, không suy full application/browser/cloud PASS.

- 13/11/2026 — Hoàn tất3.2 sau catalog: sửa409/body400 và unique room PUT500, metadata error/deprecation đích; giữ Advanced và các envelope khác. Kiểm/giới hạn/bàn giao tại Active/QA; demo skip theo Owner. Tiếp3.3 matrix/checker/readiness, không cloud/integrate/deploy/ZIP.

- 13/11/2026 — Owner mở Session3, skip demo. Hoàn tất3.1 RBAC/quản trị cuối local và bàn giao riêng; recheck quyền sau khóa, guard mọi đường role/status/PUT user, giữ aliases/GET/audit/phiên. Kiểm/giới hạn tại Active/QA; tiếp3.2 catalog lỗi trước sửa, không tích hợp/deploy/cloud hoặc đổi bundle/kịch bản demo.

- 12/11/2026 — Theo phản ánh mất danh sách lễ và yêu cầu query riêng, chuẩn bị danh mục lễ mẫu2026 tắt áp dụng, chỉ chèn holidays còn thiếu. Kiểm replay/conflict/phạm vi local theo Active/QA; không reset lại bundle hoặc thực thi Supabase. Bước tiếp Owner chạy file bổ sung và kiểm danh sách gồm dòng tắt.

- 12/11/2026 — Sinh lại bundle cho Owner tự chạy Supabase; đồng bộ DATA DESIGN, giữ nguyên hai kịch bản và chỉ điền đăng nhập private. SQL/anchor/handoff/API local/clean theo checkpoint đầu Active; sửa fixture test booking rơi Chủ nhật, không đổi expected hoặc rule. Agent chưa thực thi cloud; bước tiếp Owner chạy đúng bundle/ngày rồi lấy phiên và readback mới.

### [12/11/2026] DeepSeek tool follow-up theo kết quả kiểm thử

- Mở rộng truy vấn lịch theo khoảng ngày, đánh dấu kết quả bị cắt và tổng số; yêu cầu model gọi tool cho dữ liệu cá nhân, sắp xếp hóa đơn theo ngày FE hiển thị và chuẩn hóa handoff/fallback.
- Thêm cờ Markdown mặc định tắt cùng test mô tả tool, prompt thời gian hiện tại, date validation, truncated, hóa đơn ngày/mã lệch thứ tự và fallback.
- `clean test` Java17: 572 run, 540 pass, 32 skipped, 0 failure/error; provider/PostgreSQL/FE/live NOT_RUN. QA chuẩn: `deepseekChatDbTools20261112`.

- 12/11/2026 — Member có quyền chọn tiếp tục AI hoặc `/escalate`; thêm mode `SUGGEST`/`AUTO`, metadata handoff an toàn, fallback quota/lỗi không notify/unread hoặc lưu lặp. `clean test` 560run=528pass/32skip/0failure/0error trên base `a4cb2db` + working tree; fingerprint BE/tests tại Active; thêm9 test so với base (551 case tính theo checkpoint close + merge Google Auth), không chạy lại baseline riêng. OpenAPI JSON và diff check PASS. Không migration; FE/PG/provider thật/Render NOT_RUN. Bàn giao: `SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-12.md`.

- 11/11/2026 — Thêm Member tự kết thúc AI Chat, idempotency/ownership/response, bảo vệ AI async, giữ unread/notification đúng; cập nhật docs01 mục16 và bàn giao AI chat. Theo D19: test đích36/36, clean548run=516pass/32skip/0failure/0error trên base88ef854 + fingerprint BE/tests ở Active; PostgreSQL after-commit proof BLOCKED vì loopback55440 không mở. Không migration, FE/live/provider chưa nghiệm thu.

- 10/11/2026 — Owner cung cấp Supabase demo bundle PASS và yêu cầu merge tai→main. Ghi nhận bằng chứng Owner, kiểm fingerprint/đóng gói source đã nghiệm thu và chuẩn bị phát hành; kế hoạch smoke có build/DB/auth-me/role/readback, chưa tự chạy smoke ghi dữ liệu hoặc provider thật. Kết quả Git/CI hiện hành ở Active/QA.


- 10/11/2026 — Hoàn thiện P1.1–P1.6 local: consent/dời/chuyển/hủy và Refund với ngân hàng thủ công/private proof/đối soát; bundle47account/21Class và export DATA DESIGN từ fixture PostgreSQL đã kiểm. Guard/migration/replay/rollback/clean/HTTP và giới hạn theo Active/QA; giữ diff, chưa cloud/provider. Owner yêu cầu tích hợp main rồi review conflict trước Phase2.


- 10/11/2026 — Triển khai/kiểm local phần lõi Normal lifecycle/admission và migration21, giữ CashCorrection/Advanced/correction D15; đồng bộ contract/OpenAPI/bàn giao riêng Normal. Fixture cũ được chỉnh về đúng cutoff, Cash quầy và Class Ongoing trước finalizer; expected quyền độc lập/tiền/lịch vẫn được kiểm. Chưa đủ Phase1, chưa có Refund/chuyển lớp/bộ demo mới hoặc phát hành cloud; checkpoint ở Active.

- 10/11/2026 — Bàn giao session tiếp tục schema/demo trong docs03, có prompt và kế hoạch hai phase thay thứ tự điều phối cũ. Tách phần đã kiểm, thiết kế đã duyệt và việc chưa triển khai; giữ quyền chuẩn bị/kiểm local khác cloud mutation. Chỉ cập nhật tài liệu, không có nghiệm thu BE/cloud mới.
- 10/11/2026 — Owner chốt phần3–5 và liên hệ tùyFE/ngàyTTmở. Đồng bộ yêu cầu, điều chỉnh N11 làm đích N05 và tạo SYSTEM-DATA ở mức thiết kế từng bảng cùng generator/check dữ liệu. Không seed/reset/mutation cloud hoặc nhận nghiệm thu BE/FE mới; phần hiện thực/fixture vẫn còn theo Active/QA.

- 10/11/2026 — Theo câu hỏi Owner, đọc lại Supabase chỉ đọc để sàng lọc lớp chuyển19→21 và rà định nghĩa ngày làm việc qua schema/source/tài liệu; làm rõ contact phone khác chứng từ ngân hàng. Đề xuất tái dùng fixture N11 cho đích Pickleball và calendar Holiday hiện có, chưa sửa BE/policy/seed hoặc chạy nghiệp vụ cloud. Bằng chứng đọc tại QA, không nâng nghiệm thu live.

- 10/11/2026 — Owner chốt GET roles/permissions chỉ Admin; khóa cả role detail, giữ PUT. Kiểm phiên thật/authority phụ/status và clean đạt theo Active/QA, OpenAPI nội dung không đổi. Đồng thời ghi nhận Cash/waitlist, ngoại lệ xác nhận khai giảng và log đã duyệt; tiếp tục giải thích tương thích lớp/phone/hạn hoàn, chưa triển khai Normal hoặc reset/deploy.

- 10/11/2026 — Owner đồng ý bộ demo chung, yêu cầu bảo toàn account PDF và làm rõ5điểm cùng log center. Đã đối chiếu PDF/seed, giải thích theo source và R/L, ghi phần đã duyệt/còn hỏi trong docs03; chỉ tài liệu, không triển khai hoặc nâng nghiệm thu cũ thành kết quả mới.
- 09/11/2026 — Owner yêu cầu duyệt lại account/lớp/Coach, chuẩn bị dữ liệu demo đầy đủ thay bộ nghiệp vụ rỗng và đọc hai yêu cầu Normal trước sửa. Đã review source/contract/schema và lập bảng tái sử dụng, khoảng trống, bộ kịch bản và lựa chọn cần duyệt trong docs03; chưa triển khai hoặc xuất SYSTEM-DATA/chạy cloud. Không gán kết quả local cũ cho yêu cầu mới.
- 09/11/2026 — Owner chuyển từ hòa giải ca164 sang audit toàn DB demo và schema sạch. Đọc Supabase/backup mã hóa/clone local; bỏ lịch sử seed giả, nhúng cấu trúc hiện hành và kiểm reset/replay/rollback/JPA/workflow/clean. Bằng chứng tại Active/QA; chưa reset Supabase, push/deploy hoặc nghiệm thu FE.

- 09/11/2026 — Phase1 mục1–4: bổ sung R3 owner/role scope, validation status/permission nguyên khối, audit provenance/phân trang, giá công bố Normal và loại sân đúng môn; kiểm local/PG, giữ lịch sử/tiền cũ, cập nhật contract/OpenAPI/QA. Dừng trước R1/R2 để Owner review; chưa push/deploy/mutation cloud hoặc nghiệm thu FE. Bằng chứng hiện hành ở Active/QA, không sửa LAN-2 ngày07/11.


- 09/11: Owner báo FE có đợt cập nhật; chuyển các phản ánh cũ về lịch sử/cần kiểm lại, không kết luận còn lỗi hoặc đã sửa. Gom F01–F12 và đề xuất Phase1 hoàn thiện BE/Phase2 nghiệm thu chung để duyệt; chỉ docs, chưa có test/runtime hoặc triển khai mới.

- 09/11: chuẩn bị phát hành attendance theo Owner qua PR21, hợp nhất main AI, xử lý fixture log SMTP phụ thuộc context; kiểm/package/source/giới hạn theo Active/attendanceRelease20261109 QA. Cloud SQL, FE/Render/CRUD/live chưa nghiệm thu.
| 08/11/2026 | Bo sung DeepSeek provider va prompt tieng Viet co dau cho AI chat, quota member/day, knowledge base va an toan handoff | Clean test local 446 tests = 418 pass + 28 skip, 0 fail/error tren working tree branch feature/ai-chatbot-env; chi tiet trong ban giao AI chat. Chua live/provider/Render/FE. |
| 08/11/2026 | Sua FAQ chao hoi/khop KB/fallback hai lan va noi dung SYSTEM tieng Viet | Clean test local 451 tests = 423 pass + 28 skip, 0 fail/error tren working tree feature/ai-chatbot-env; tang 5 test so voi baseline 446. Khong migration/doi endpoint; chi tiet trong ban giao AI chat va docs01 muc15. |

- 09/11: chốt và triển khai BE attendance cuối ngày/note; tách lifecycle tại end với no-show sau cutoff, migration17 bảo toàn lịch sử; cập nhật contract/OpenAPI/bàn giao. PASS local theo Active/attendanceImplementation20261109 QA; UI do FE quyết định, FE/Render/CRUD/live chưa nghiệm thu; không mutation cloud hoặc chạy lại16.

| Ngày | Nhóm hoàn tất | Dấu vết |
|---|---|---|
| 07/11/2026 | Ghi phạm vi Owner và double-check đề xuất BE Coach R1–R3 | Hoãn FE/CRUD và phiên UI sang đợt chung; A/B cùng center. Đề xuất chờ duyệt, source/tests chưa đổi hoặc chạy mới; không mở quyền/DB mutation |
| 07/11/2026 | Soạn LAN-2 attendance Coach và checklist Coach còn mở | Bàn giao UI/state/contract/acceptance; C01–C09 phân biệt source cần rà với phần chưa kiểm/bàn giao. Chỉ docs/QA, không đổi FE/BE/DB hoặc chạy/gán test mới |
| 07/11/2026 | Làm rõ lỗi UI attendance không đổi detail theo dropdown | Owner đính chính vùng dưới giữ buổi cũ; rút kết luận quá sớm rằng hàm gửi chọn sai attendanceId. UI/state phải đồng bộ, root cause source chưa kiểm; Coach403 và first-write chưa nghiệm thu giữ nguyên |
| 07/11/2026 | Ghi phản hồi roster lớp10 và phân loại ba lỗi ghi attendance sau merge PR19 | Owner đọc200, auth/me Coach/RECORD_RESULT, check-in400 LOBBY_CHECKIN_DISABLED; health public build63fb46c. Chỉ docs/QA, chưa kiểm state ghi/full Coach hoặc sửa FE |
| 07/11/2026 | Push nhóm RBAC đã kiểm và mở PR19 theo yêu cầu Owner | Source/CI/merge truy PR19; không tái gán test hoặc nghiệm thu FE/cloud. Hướng dẫn kiểm theo role ở docs02; Owner xác nhận trước khi đề xuất phần tiếp theo |
| 07/11/2026 | Sửa sai lệch RP-04/RP-36 và quyền Member đọc giáo án lớp theo nguồn thiết kế | Clean457=429pass/28skip, guard25 H2 +25 PG, workflow22 PG; đối chứng3lỗi trước sửa, nguồn/XML/fixture tại rbacDesignCorrections QA. Không FE/SQL/cloud/deploy; chỉ phạm vi checkpoint, chưa đóng toàn BE audit |
| 07/11/2026 | Đối chiếu lại và rút bảng lựa chọn P1–P7 sau phản hồi Owner | Tách yêu cầu thiết kế đã rõ, gap implementation và lựa chọn kỹ thuật; không xem test Manager201 là nghiệm thu RP-36. Chỉ docs/QA, giữ nguyên bằng chứng chạy và source; chưa sửa các sai lệch nghiệp vụ còn lại |
| 07/11/2026 | Hoàn tất nhóm guard đã duyệt, kiểm hai tuyến roster Coach và xử lý tài liệu cũ | Đối chứng5lỗi source trước sửa; guard18/clean450/PGworkflow22 theo Active/rbacPriorityGuards QA. Giữ quyết định Dxx và diff đầu lượt, thêm khóa target/kiểm thu hồi phiên/audit actor thật; không FE/SQL/cloud. P1–P7 và phạm vi audit còn chờ, không đóng trọn BE audit |
| 06/11/2026 | Rà thiết kế toàn hệ thống theo yêu cầu Owner, ghi13đề xuất TK trong docs03 | Đối chiếu D01–D19/contract/mô hình DB, PDF gốc và source đích; phân biệt thiếu guard, mâu thuẫn tài liệu cũ và quyết định còn thiếu. Chỉ review/docs, chưa sửa nghiệp vụ/test/deploy hoặc đóng audit |
| 06/11/2026 | Checkpoint review tĩnh BE-01 theo contract/controller/service/test | Matrix quan sát và phần thiếu ở docs03; chờ Owner chốt ba GET roles/permissions. Chỉ docs, chưa sửa quyền/BE/FE/SQL hoặc đóng BE-01; không thêm kết quả test/deploy |
| 06/11/2026 | Ghi bàn giao session RBAC và prompt tiếp tục trong docs03 | Phân biệt phần đã kiểm với toàn mục audit; điểm dừng sau0/0.1, đề xuất BE-01 riêng. Chỉ tài liệu, không thêm kết quả test/SQL/deploy |
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
