# SCMS — Tiến độ và nghiệm thu

## Active

**Cách mở session:** đọc AGENTS → Active → đúng Dxx trong docs04; khi sửa DB đọc [database/README](../database/README.md) và source/trigger/test liên quan. Đọc docs01 theo API, docs02 theo nhu cầu test/migration. Kết quả bên dưới luôn theo đúng mốc/source/môi trường đã ghi.

**Phạm vi đã giao:** P1.0–P1.4, BE P2 và tích hợp D01/D09 theo Owner28/10. P1.5/phần còn lại gói T/FE integration vẫn để lại. Không coi merge OTP/chat/notification hoặc D01/D09 là hoàn tất toàn bộ gói T.

**Bàn giao02/11:** Owner đã chốt20room theo room.jpg, giữ room_id số/thêm room_code và không thêm subject_id. Source room/báo vắng đã kiểm local. Owner xác nhận cả0211-1 và0212-2 hoàn tất trên Supabase, gồm7giới hạn lớp; chưa xác nhận deploy BE mới trên Render. Bản duy nhất gửi FE: [SCMS-BAN-GIAO-FE-2026-11-02.md](../SCMS-BAN-GIAO-FE-2026-11-02.md). Giữ thiết kế center1; auth/me không thêm centerId. Hai báo cáo29–31/10 giữ vai trò bằng chứng lịch sử.

**Chẩn đoán CORS/VNPay04/11 (trước QueryDR):** CORS/cookie và redirect Pages `/member/invoices` đã kiểm HTTP. URL/HMAC lỗi cũ khớp key ảnh Render, không có căn cứ sửa encoder/IP/trim. Owner xác nhận gateway thành công nhưng invoice20/22 chưa Paid; chưa có bằng chứng IPN thành công đã commit. Menu merchant chỉ email; Owner không muốn chờ hỗ trợ. Bằng chứng gốc giữ tại `vnpayCorsDiagnosticReview` trong [QA](../artifacts/qa/latest.json).

**QueryDR04/11:** Owner đã duyệt và BE đã triển khai POST xác minh riêng, lưu pay attempt, xác thực phản hồi và dùng chung nhánh ghi Paid với IPN. [Migration15](../database/migrations/15_QueryDR.sql) cộng thêm, để Owner copy nguyên file chạy một lần. PASS local theo mục nghiệm thu bên dưới; chưa chạy SQL cloud/deploy Render/tích hợp FE hoặc gọi VNPay thật. Handoff duy nhất đã bổ sung [mục QueryDR](../SCMS-BAN-GIAO-FE-2026-11-02.md#querydr). Có chỉnh sửa email đồng thời ngoài snapshot kiểm QueryDR, được giữ nguyên và ghi riêng trong QA.

| Việc còn mở | Trạng thái | Điều kiện tiếp tục |
|---|---|---|
| Kiểm API có phiên, owner/role/center trên Render | BLOCKED; chưa chạy nghiệp vụ | Thiếu tài khoản thử, fixture, phạm vi live và cách đọc state sau commit; kiểm `/auth/me` trước |
| Toàn bộ tổ hợp CRUD baseline × role/owner/center ngoài matrix workflow đích | NOT_RUN đầy đủ | Xác định contract/fixture từng nghiệp vụ; không suy PASS từ route/schema hoặc workflow liên quan |
| OTP/email cloud | Đã hỗ trợ sender riêng ở source; chưa nghiệm thu Render | Owner04/11 báo đã xác minh sender/thêm env Brevo SMTP2525. Cần deploy BE mới rồi kiểm kết nối/STARTTLS, delivery OTP và xác minh tài khoản; HTTPS API là phương án dự phòng nếu2525 không kết nối được |
| D01 hồ sơ Member; D09 Coach directory trên Render | PASS local; chưa publish | Main local đã tích hợp và kiểm PG có phiên; chưa push tại điểm review đã chốt, không gọi đó là nghiệm thu Render |
| FE browser/CORS/cookie/camera QR | PASS CORS/cookie và redirect Pages HTTP04/11; browser/F5/QR chưa nghiệm thu đầy đủ | Cookie None/Secure/HttpOnly; Return đã trỏ Pages /member/invoices. Cần auth/me200 từ FE/F5, dữ liệu API sau thanh toán và scope QR để chốt phần liên quan |
| VNPay gateway sandbox live | Owner từng xác nhận giao dịch thành công; QueryDR PASS local, chưa kiểm provider thật | Owner chạy15 rồi publish BE; kiểm merchant ở chế độ chỉ đọc trước khi bật settlement; Return không ghi Paid, IPN vẫn được giữ |
| SQLServer runtime | NOT_RUN | Môi trường kiểm riêng nếu được chọn |
| Schema demo/Coach theo Owner29/10 | PASS local; báo cáo có snapshot Supabase29/10 | Snapshot do Owner xuất là bằng chứng lịch sử được báo cáo dẫn, chưa được session này kiểm độc lập hoặc xác minh lại live; không ghi chung là chưa từng chạy Supabase |
| Báo vắng tự ghi Absent | PASS local; Owner xác nhận migration14 hoàn tất; chưa xác nhận deploy/live | Publish BE mới rồi kiểm workflow có phiên; schema sẵn sàng không thay nghiệm thu runtime |
| Danh mục20room/room_code và7giới hạn lớp | Owner xác nhận cả hai bundle Supabase hoàn tất | Nạp lại dữ liệu rooms/classes trên FE sau deploy; Agent chưa truy vấn kiểm độc lập dữ liệu live |

**Mức sẵn sàng FE:** contract/schema đủ để bàn giao phạm vi BE đã kiểm local có điều kiện; chưa ký full authenticated Render hoặc FE acceptance. Matrix lịch sử ở `authenticatedAcceptance`, bằng chứng room/báo vắng02/11 ở `roomAbsenceReview` của [QA duy nhất](../artifacts/qa/latest.json). Đối chiếu từng phát hiện gốc và giới hạn bằng chứng tại [bảng1:1](#report-trace).

**Bản chạy tạm cho Owner:** đã xuất `0211-1.sql` và `0212-2.sql` ở root để chạy đúng hai lần theo docs02. Đã kiểm thứ tự, guard/rollback, chuyển đổi, postflight và replay trên PostgreSQL riêng ở working tree trên `ec2325df07df053257df09698857b27e36489820`; giữ nguyên SQL nguồn13/14, không chạy Supabase hoặc thêm kết quả BE mới. Bằng chứng/checksum tại `rootBundlePackaging` trong QA. Sau khi Owner chạy thành công và kiểm kết quả có thể xóa hai bản root; nguồn migration vẫn giữ.

**Bước kế tiếp:** Owner chạy nguyên migration15 trên DB hiện có, review/publish source BE đã tích hợp (gồm các thay đổi đồng thời cần review riêng), rồi kiểm merchant ở chế độ QueryDR chỉ đọc trước khi bật settlement. FE tự POST hóa đơn đang theo dõi rồi GET theo contract; chưa có source FE hiện hành để ghép trong repo. Tiếp tục kiểm Render có phiên/fixture/state sau commit và các mục còn mở; không suy nghiệm thu live từ local.

Đọc: [AGENTS](../AGENTS.md) → [quyết định Dxx](04-DEBUG-NOTE.md) → [contract FE](01-API-CONTRACT-FE-BE.md) đúng luồng; [vận hành](02-DEPLOYMENT-GUIDE.md) khi cần.

## Nghiệm thu QueryDR — 04/11/2026

**Source/môi trường:** snapshot working tree chưa commit trên `ec2325df07df053257df09698857b27e36489820`; fingerprint đã kiểm và danh sách email thay đổi đồng thời ở `queryDrImplementation` trong [QA](../artifacts/qa/latest.json). Java17; bản sao không.env; H2 và PostgreSQL17 riêng `127.0.0.1:55440`, Clock/JVM Asia/Ho_Chi_Minh; provider HTTP giả lập. Toàn bộ source invoice/test QueryDR hiện tại khớp bản sao đã kiểm; kết quả không bao gồm chỉnh sửa email xuất hiện sau khi chụp source.

| Điều kiện nghiệm thu | Kết quả local |
|---|---|
| Migration15 cộng thêm/replay/guard | PASS9nhóm; hash dữ liệu46bảng cũ giữ nguyên; attempt giữ khi replay; xung đột cấu trúc/guard rollback, FK/unique/check đúng; root/demo và empty release tương thích |
| Request/response và bằng chứng thanh toán | PASS HMAC golden độc lập, đúng pay CreateDate, query thời điểm HCM, chữ ký/merchant/ref/amount/type/status; sai/thiếu chữ ký hoặc timeout không Paid |
| Auth/state/quyền | PASS HTTP login+auth/me; owner/role/center/Active/body; số tiền mua giữ nguyên khi catalog đổi; QueryDR cấp12credit đúng một lần; cutoff và quyền không còn → Paid+đối soát, không cấp lại |
| Retry/race/đọc danh sách | PASS cooldown, metadata nhiều link/kiểm link cũ, QueryDR↔IPN không giữ khóa trong HTTP, không nhân credit/thông báo; GET chỉ đọc; đơn cũ thiếu metadata không tự sửa |
| Contract/rollout | POST trả wrapper+InvoiceResponse; snapshot OpenAPI sinh từ cùng app PG. File15, toggle/chế độ chỉ đọc, thứ tự publish và FE auto POST→GET đã bàn giao |

**Kết quả:** `clean test` trong bản sao không.env:354run =326pass/28skip/0failure/0error; fixture cuối kiểm đích H2 11/11 và PostgreSQL11/11 PASS với ddl-auto=none sau migration15/các prerequisite. Skip không là pass. Lượt PG đầu bị chặn bởi alias JVM Asia/Saigon; chạy lại Asia/Ho_Chi_Minh. Đã sửa setup center_name và dựng đủ migration nền; test fixture cuối được kiểm lại, source BE không đổi. Không nới expected nghiệp vụ.

**Giới hạn:** chưa gọi QueryDR VNPay thật, chưa deploy Render/chạy SQL Supabase hoặc ghép FE browser. Capability merchant/chữ ký live còn NOT_RUN; đơn cũ không có attempt vẫn cần đối soát. Có thay đổi email ngoài snapshot nên không gán PASS toàn bộ working tree hiện hành hoặc nghiệm thu lại email từ count này. Canonical QA giữ cả snapshot đã kiểm và fingerprint hiện tại; kiểm route/schema offline có thể REVIEW_REQUIRED cho phần identity source đồng thời này.

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
