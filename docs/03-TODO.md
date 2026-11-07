# SCMS — Checklist xác minh Access Denied / RBAC

Mở04/11/2026; cập nhật07/11/2026. Nguồn: [bản audit](../SCMS_ACCESS_DENIED_RBAC_AUDIT.md) và phản ánh Owner. Làm lần lượt **0 → 0.1 → BE-01 → BE-07**; không mặc định nội dung audit là lỗi. Khi còn mơ hồ hoặc cần quyết định nghiệp vụ mới, hỏi Owner trước khi sửa. Kết quả chuẩn tại [Active](00-MASTER-CHECKLIST.md#active)/QA.

## Checklist

**Yêu cầu đã thực hiện:** review06/11 tại [bản rà thiết kế](#design-review-20261106); sau đó Owner duyệt nhóm guard ưu tiên, xử lý tài liệu cũ và thêm kiểm từ ảnh BE-03. Phần local đã hoàn tất tại [checkpoint07/11](#priority-guards-20261107), kết quả chuẩn ở Active/QA. [P1–P7](#pending-policy-20261107) đã đối chiếu lại và rút bảng A/B; không hỏi lại các yêu cầu thiết kế đã rõ; không đóng toàn BE-01–BE-07.

**Cập nhật sau xử lý conflict06/11:** bản ghép main/tai và kiểm hồi quy mới xem [Active](00-MASTER-CHECKLIST.md#active)/PR18; giữ các quyết định0/0.1, không coi merge là nghiệm thu FE/Render hoặc hoàn tất cả mục audit. Mốc Git/source của bàn giao trước chỉ mô tả lúc ghi; khi mở session mới phải kiểm git status/diff hiện tại.

- [x] **0 — Hai403 của Member, phần BE local và tài liệu:** đã sửa training-results, rà năm role payment-resolutions và ghi yêu cầu FE. Chưa deploy/nghiệm thu Render/FE.
- [x] **0.1 — Bỏ điều kiện gói chung cho Normal, phần BE local/tài liệu:** đã sửa và kiểm đăng ký/waitlist, thanh toán, owner/center/lịch, retry/race và giữ liên kết cũ. 0.1-B/C đã kiểm local, SQL16 đã được Owner xác nhận PASS; chưa deploy/kiểm Render/FE. Catalog/17 còn mở.
- [x] **0.1-B — Đóng tuyến ghi danh Advanced cũ:** hai tuyến enroll chỉ nhận Normal; không tự thăng waitlist Advanced. Test đích/clean đã đạt; final PG workflow sau migration cũng đạt. Chưa deploy Render.
- [x] **0.1-D — Loại mô hình gói chung:** BE đóng API cũ đã kiểm local; Owner gửi SQL16 PASS trên Supabase, model_retired=true, tất cả bộ đếm xóa/tháo liên kết0. DB không có dữ liệu cũ cần xóa; trigger/marker đã áp dụng theo kết quả này. Agent không tự truy vấn cloud; BE còn cần deploy.
- [x] **0.1-A — CashCorrection, BE local:** sau Owner xác nhận SQL16 PASS đã bỏ suy luận phụ thuộc theo thời gian; Normal Pending/Paid độc lập không chặn. Giữ liên kết thật/D16, kiểm lại khi Complete, giữ hóa đơn/ghi danh Normal và Paid/amount gốc; kiểm H2/PG theo docs00/QA, chưa deploy.
- [ ] **BE-01 — Ma trận quyền:** đã bắt đầu đối chiếu contract/controller/service/test, ghi [ma trận quan sát và điểm chờ Owner](#be01-review-20261106); chưa công bố policy toàn API/năm role. Giữ phần đã chốt ở0/0.1; dừng trước sửa quyền đọc roles/permissions vì chưa có quyết định nghiệp vụ.
- [ ] **BE-02 — Phiên và401:** đã kiểm local quyền hiện hành/Inactive/đổi role/registry thu hồi/password và public IPN; chưa đủ nghiệm thu vòng đời cookie/timeout/proxy/Render hoặc quy nguyên nhân401 cũ. Xem checkpoint07/11.
- [ ] **BE-03 — Phạm vi Coach:** đã kiểm hai GET enrollment đúng/sai phân công, bỏ filter/thêm authority/khác center/buổi kết thúc và giữ chặn ghi; guard ghi plan/evaluation có kiểm actor/phân công. Ảnh lớp23 chưa có auth/me/assignment/runtime SHA để kết luận live. Các nhóm Coach còn lại và toàn bộ ma trận vẫn mở.
- [ ] **BE-04 — Phạm vi Member/IDOR:** training-results đã kiểm ID từ phiên, bỏ filter/đổi ID/thêm authority; Normal từ chối gửi memberId khác. Tái sử dụng workflow invoice/subscription làm bằng chứng riêng từng API; chưa bao phủ toàn bộ nhóm.
- [ ] **BE-05 — Xác nhận Cash:** có policy Receptionist+MANAGE_INVOICES và workflow center/state/retry/race/commit; chưa đủ ma trận riêng năm role. Không nhầm với quyền Manager duyệt CashCorrection ở0.1-A.
- [ ] **BE-06 — Report/audit log:** chưa rà riêng; D18 cho report, policy audit riêng; kiểm role/authority/center và filter/phân trang.
- [ ] **BE-07 — Contract401/403:** chưa rà riêng; đối chiếu filter/handler/OpenAPI; tách yêu cầu có sẵn khỏi đề xuất error code/request ID mới.

## 0 — Hai request403 của Member

| Nội dung | Kết luận / hành động |
|---|---|
| Vấn đề | Member gặp403 ở GET `training-results?memberId=42` và GET `payment-resolutions`; hai API có nguyên nhân khác nhau. |
| BE đã sửa training-results | Cho Member Active đọc kết quả của mình; lấy ID từ phiên, chặn ID người khác, filter chỉ thu hẹp dữ liệu. Giữ quyền ghi hiện có. |
| BE xử lý payment-resolutions | Giữ403 cho Member/Coach. Đã rà role AND MANAGE_INVOICES: Receptionist đọc/tạo, Manager thêm duyệt/hoàn tất; service kiểm Active/center. Admin không mặc nhiên có quyền tài chính/toàn hệ thống. |
| FE cần làm | Áp dụng mục4 của [bản bàn giao RBAC lần1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md); không sửa source FE trong lượt BE này. |
| Phạm vi hoàn tất | BE local và tài liệu đã xong theo Owner05/11. Không sửa source FE; chưa deploy/nghiệm thu Render/FE. |

- Chi tiết quyền/API: [training-results](01-API-CONTRACT-FE-BE.md#attendance), [payment-resolutions](01-API-CONTRACT-FE-BE.md#resolutions); quyết định tại [D15](04-DEBUG-NOTE.md#d15).
- Bằng chứng: [Active](00-MASTER-CHECKLIST.md#active), `rbacIssue0` trong [QA](../artifacts/qa/latest.json). Gate role dùng service mock; integration đọc dùng login/auth-me thật, fixture commit local H2; không thay nghiệm thu hoàn tiền hoặc PostgreSQL/live.
- Còn lại: FE áp dụng yêu cầu; sau deploy kiểm API bằng phiên hợp lệ trên Render. Hai403 gốc chưa có body/auth-me cùng phiên/SHA runtime để xác định chính xác nhánh lỗi live.

## 0.1 — “Gói hội viên của tôi” và điều kiện đăng ký khóa

### Ba khái niệm cần phân biệt

| Việc | Thiết kế hiện tại |
|---|---|
| Tạo tài khoản + verify OTP | Tạo users/members, kích hoạt tài khoản; không tự tạo gói hoặc quyền học miễn phí. |
| Đăng ký Normal | Chọn lớp đủ điều kiện → đơn/hóa đơn Pending → Paid hợp lệ mới Registered. Không cần mua gói hội viên chung trước. |
| Mua Advanced | Chọn môn/Primary Coach → Coach nhận → tạo đơn → Paid hợp lệ mới Active/12credit. Đây là quyền học khóa1:1, khác gói hội viên chung. |

### Vì sao hiện tại có thể bị chặn?

- **Rule cũ:** [TECHNICAL_ANALYSIS §4.2 tại mốc trước merge](https://github.com/Aditerx/SWP391/blob/3a2adaf1b6ac11bbe55cdc7e41931a4a5e31530a/old-docs/TECHNICAL_ANALYSIS.md) từng yêu cầu có gói Active còn hạn trước khi đăng ký lớp. Main đã bỏ file lịch sử này; không khôi phục nó thành yêu cầu hiện hành.
- **Yêu cầu đã thay:** [MVP Role Permissions](../old-docs/SCMS-Requirements-MVP-Role-Permissions-20261018.pdf), trang14/19/52: hoãn gói thành viên vì chưa có quyền lợi riêng; đăng ký khóa không bắt buộc có gói và không dùng gói chung làm lõi đăng ký. D02/D03/D15 trong [docs04](04-DEBUG-NOTE.md) và [contract Normal](01-API-CONTRACT-FE-BE.md#normal) mô tả giá/quyền theo khóa.
- **Nguyên nhân trước sửa:** [EnrollmentService.enroll](../src/main/java/com/sportscenter/enrollment/EnrollmentService.java) đòi member_package Active còn hạn; thiếu thì400 `PACKAGE_EXPIRED`, kể cả người chưa từng mua gói. Guard chạy trước khi tạo hóa đơn Normal và có cả trong `promote`. Bản sửa bỏ điều kiện cho Normal; B đã chặn Advanced đi qua tuyến enroll chung.
- **Phụ thuộc khi sửa:** enroll còn gán `eligibilitySubscription`; [CashCorrection](../src/main/java/com/sportscenter/invoice/PaymentResolutionService.java) kiểm liên kết này và dependency legacy. Không chỉ xóa một dòng guard hoặc xóa dữ liệu để vượt điều kiện.
- **DB không thiếu seed gói:** [database/README](../database/README.md) ghi membership_packages là danh mục legacy, bản dựng lại cố ý không seed gói tháng/VIP. member_packages hiện chủ yếu giữ quyền Advanced; Normal ở class_enrollments. [Schema](../database/schema_postgres.sql) cho eligibility_subscription_id nullable; không thấy trigger bắt mua gói chung trước ghi danh Normal.

### Kết luận và hướng xử lý

**Nguyên nhân trước sửa:** BE còn điều kiện gói Active không phù hợp yêu cầu Normal độc lập. Test cũ `enroll_NoActivePackage_ThrowsException` dùng fixture Advanced; nay đổi tên `enroll_LegacyNonNormalWithoutActivePackage_ThrowsException` để đúng phạm vi, giữ assertion cho nhánh chưa đổi. Normal được kiểm độc lập bằng fixture mới không có gói.

**Bằng chứng Owner06/11:** tài khoản vừa tạo/verify OTP; ảnh FE ở catalog tier=normal, môn Cầu lông, modal lớpK01 giá900.000đ, chọn VNPay. POST `/api/classes/19/enroll` trên Render trả400 `{"code":"PACKAGE_EXPIRED","message":"member package is expired or inactive"}`. Đủ xác nhận triệu chứng chặn đăng ký Normal ngoài mong đợi, khớp guard local; chưa thu SHA runtime hoặc tự tái hiện bằng phiên live. Chưa có đơn/Paid mới trong ca này; không tự tạo gói để vượt lỗi.

| Quan sát kèm theo | Phạm vi kết luận |
|---|---|
| Màn gói hiển thị trạng thái trống; subscriptions/enrollments200 trong ảnh | Ghi chú câu chữ và giải thích cho FE chuyển sang bản bàn giao RBAC lần1. Chưa có body hai response để kiểm dữ liệu. |
| GET `/api/sessions/catalog/17` trả404 | Là lớp17, khác POST enroll lớp19. Source SessionService trả404 khi lớp không tồn tại hoặc không phải Normal đã Open/Ongoing; thiếu buổi đơn thuần trả200 mảng rỗng. Cần body404 và classType/status của lớp17 từ danh sách/chi tiết để xác định đúng nhánh; chưa kết luận sai ID hoặc lỗi BE. |
| GET payment-resolutions403 vẫn có trong ảnh Member | FE vẫn gửi request ngoài quyền ở ca chụp. Yêu cầu mục0 chưa được chứng minh đã áp dụng; không sửa policy BE để tránh403. |

**Owner đã duyệt và BE đã sửa06/11:** Normal đăng ký/xét danh sách chờ không cần gói chung; giữ kiểm tài khoản, owner/center, lớp/lịch/sĩ số/trùng lịch và luồng thanh toán học phí. Không gán eligibility mới cho Normal, giữ liên kết lịch sử; không cấp/seed gói giả, không đổi Advanced/CashCorrection ngoài phạm vi. Kết quả bản bỏ gói chung theo [Active](00-MASTER-CHECKLIST.md#active) và `normalMembershipIndependence` trong [QA](../artifacts/qa/latest.json); chưa deploy. Nhánh giá0 nay được Owner duyệt xử lý riêng tại0.1-C bên dưới.

**Câu chữ/trạng thái trống:** theo Owner, đã chuyển giải thích và yêu cầu để FE quyết định sang mục4 của [bản bàn giao RBAC lần1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md). Docs01 chỉ giữ thiết kế/contract chính thức; không sửa source FE.

### 0.1-C — Chặn giá Normal không hợp lệ — đã xong BE local

Owner duyệt xử lý riêng0.1-C: giá thiếu/≤0 trả400 INVALID_PRICE ở cả hai API đăng ký, không tạo enrollment/hóa đơn; nhánh đầy lớp cũng không tạo Waitlisted cho giá lỗi. Khi xét thăng Waitlisted, giá lỗi giữ nguyên hàng chờ, không đổi Registered; giá hợp lệ tạo Pending trước Paid. Không cập nhật giá hoặc sửa chứng từ/ghi danh lịch sử. Lớp tạo mới đã có guard giá trong SportsClassService; bản sửa này bảo vệ thêm đường đăng ký khi dữ liệu legacy/không hợp lệ lọt vào.

- [x] Test đích và clean test đã đạt trên source cuối; môi trường/hash/XML tại [Active](00-MASTER-CHECKLIST.md#active), `normalPositivePriceGuard` trong [QA](../artifacts/qa/latest.json). Bao gồm giá0/âm/null, hai tuyến đăng ký Cash/VNPay, còn chỗ/đầy, promotion giữ Waitlisted và hóa đơn Paid cũ không đổi. NULL là ca phòng vệ H2 tạm nới NOT NULL trong fixture rồi khôi phục; không sửa schema thật hoặc coi NULL hợp lệ trên PostgreSQL. Giá hợp lệ tạo Pending; retry không nhân đơn. Chưa deploy/kiểm PostgreSQL riêng hoặc Render.

### A/B và phần gói cũ — đã duyệt, theo thứ tự B→SQL→A

**0.1-A — Cash bị đánh dấu Paid dù chưa thu:** lễ tân bấm POST `/api/invoices/{id}/confirm-cash` cho một hóa đơn cụ thể. BE không dò Normal/Advanced theo giá. D16 chỉ cho sửa trong trường hợp Advanced chưa nhận tiền thực, chưa dùng, còn đủ credit và đáp ứng các guard khác; không phải chức năng hoàn tác mọi lần bấm sai đơn.

Ví dụ minh họa, không phải dữ liệu live:

1. Hóa đơn101: Advanced Cầu lông3.000.000đ. Lễ tân lỡ xác nhận Paid trong khi chưa thu khoản này.
2. Hóa đơn102: lớp Normal Cầu lông900.000đ. Member đăng ký và trả đúng khoản900.000đ; đơn102 không sử dụng quyền/tiền của đơn101.
3. Manager xử lý việc xác nhận sai ở đơn101. Trước sửa A, code thấy Member có ghi danh Normal mới, eligibility null và thời điểm sau Advanced thì có thể chặn hồ sơ101 với lý do dependency legacy. Đó là “chặn nhầm”: đơn102 độc lập nhưng bị coi như dùng quyền của đơn101.

**A đã sửa sau SQL16 PASS:** không lấy một ghi danh Normal độc lập làm lý do chặn sửa đơn101. Dùng liên kết đơn/quyền học thật để kiểm phụ thuộc; vẫn giữ D16, chứng từ hiện hành và audit. Test có phiên/commit kiểm Normal Pending/Paid mua sau Advanced, eligibility thật chặn Approve và mới thêm sau Approve chặn Complete, credit thay đổi bị chặn, role/center sai, bắt buộc xác nhận chưa nhận tiền và retry không nhân audit/thông báo. Nếu tiền đã nhận nhưng ghi nhầm hóa đơn, phải xác minh đúng khoản thu trước, không tự khai chưa nhận hoặc chuyển tiền giữa đơn. Không thêm lớp tương thích legacy mới để giữ mô hình đã bỏ.

**0.1-B — Advanced đã có flow riêng, không phải đề xuất mới:** [D03](04-DEBUG-NOTE.md#d03)/[D04](04-DEBUG-NOTE.md#d04), [docs01 §4](01-API-CONTRACT-FE-BE.md#advanced) và [AdvancedRegistrationService](../src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java) đã mô tả/thực thi:

| Bước | API / kết quả |
|---|---|
| Member chọn môn và Primary Coach | POST `/api/advanced-registrations` với subjectId,primaryCoachId,requestKey →Requested |
| Coach được chọn nhận dạy; Manager hỗ trợ đúng quyền | POST `/{id}/decision` →Accepted/Rejected; chưa thu tiền, chưa chạy hạn học |
| Member tạo đơn sau Accepted | POST `/{id}/order` với Cash/VNPay →hóa đơn Pending và đăng ký Advanced Pending |
| BE xác nhận tiền hợp lệ | Cash do Receptionist xác nhận hoặc callback/truy vấn VNPay hợp lệ →Paid; Advanced Active/12credit, hạn theo D12 |
| Thống nhất lịch và tạo buổi học | Primary Coach hoặc Manager hỗ trợ theo D04 tạo từng ca; kiểm môn/Coach/credit/hạn/lịch, không dùng enrollment Normal để thay thế |

**B đã duyệt và đã sửa local:** hai tuyến POST `/api/classes/{classId}/enroll` và POST `/api/enrollments` chỉ nhận Normal. Advanced trả400 USE_ADVANCED_REGISTRATION trước khi tạo enrollment/hóa đơn; waitlist Advanced cũ không tự thăng qua Normal. Đã kiểm cả Cash/VNPay, state sau commit và hồi quy Normal; code chung không còn chọn gói Active hay gán eligibility để đi vòng flow. Unit fixture đăng ký mới chuyển từ Advanced sang Normal có học phí/lịch hợp lệ; các ca hủy lịch sử giữ riêng, không đổi expected chỉ để xanh. A được xử lý sau SQL16, theo mục trên.

**Phần C Owner đang hỏi =0.1-D trong checklist, khác0.1-C giá0 đã xong.** Gói hội viên chung tháng/VIP không còn cần trong thiết kế sản phẩm hiện tại. “Gói Advanced” là cách gọi quyền học của một khóa1:1, không phải gói hội viên chung đổi tên.

| Dữ liệu / thuật ngữ | Vai trò và hướng xử lý |
|---|---|
| membership_packages, mua/gia hạn bằng packageId | Mô hình danh mục gói chung cũ cần loại khỏi thiết kế/caller. Phần dữ liệu thực sự obsolete được xóa theo chỉ đạo Owner, không giữ chỉ vì tương thích. |
| member_packages, subscriptions, MemberPackageResponse | Tên kỹ thuật đang chứa quyền Advanced: môn, Coach, giá mua, credit và hạn. Không xóa cả bảng hoặc mọi record có packageId; có thể có liên kết cần tách/xử lý đúng trước. Đề xuất gọi nghiệp vụ là “đăng ký Advanced/quyền học Advanced”; đổi tên vật lý bảng/API là việc khác, không cần gộp để giải quyết lỗi hiện tại. |
| class_enrollments và hóa đơn class_id | Ghi danh Normal và khoản học phí của chính lớp; không phụ thuộc gói hội viên chung. |

**Bước loại bỏ mô hình cũ — Owner xác nhận Supabase PASS:**

1. BE đã đóng mua/gia hạn/đổi trạng thái gói chung và tạo/sửa danh mục; tạo Member tại quầy kèm packageId cũng bị từ chối trước ghi account. GET quyền Advanced và flow advanced-registrations còn hoạt động; generic extension Advanced vẫn yêu cầu incident recovery.
2. File duy nhất [16_retire_legacy_memberships_postgres.sql](../database/migrations/16_retire_legacy_memberships_postgres.sql) tự kiểm FK/phân loại trong một transaction: chỉ xóa subscription/hóa đơn và payment-case/attempt thuộc gói chung cũ chắc chắn; giữ quyền/chứng từ hiện hành, tháo catalog FK khi đủ bằng chứng. Link sang nghiệp vụ hiện hành/partial Advanced/unknown FK hoặc postflight lệch dữ liệu thì rollback, không xóa lan. Không giữ archive dòng nghiệp vụ cũ; chỉ ghi số lượng migration.
3. Đã test PostgreSQL local: modern-only không đổi, purge đúng subgraph, replay, trigger cấm tạo lại, guard liên kết/FK/partial data và rollback cả phần xóa khi cố ý làm lệch tiền. BE test đích/clean và workflow có phiên trên PG sau migration đã đạt; bằng chứng/hash ở docs00/QA.
4. Owner gửi result=PASS, marker đúng và model_retired=true; toàn bộ số lượng xóa/tháo liên kết0. Đây là DB không có dữ liệu cũ cần dọn, không phải migration bỏ chạy. Đã ghi lý do/phạm vi/kết quả vào [LAN-1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md); điều kiện tiếp tục A đã đạt. Không tự chạy cloud, không suy thành BE đã deploy.

Nguồn thực thi: [EnrollmentService](../src/main/java/com/sportscenter/enrollment/EnrollmentService.java), [PaymentResolutionService](../src/main/java/com/sportscenter/invoice/PaymentResolutionService.java), [MemberPackageController](../src/main/java/com/sportscenter/membership/MemberPackageController.java)/[Service](../src/main/java/com/sportscenter/membership/MemberPackageService.java), [database/README](../database/README.md). Expected mua/gia hạn gói chung thành công được thay bằng từ chối theo quyết định bỏ sản phẩm đã duyệt; kiểm không tạo đơn/tài khoản hoặc khôi phục quyền. Không thay expected tiền/quyền để che failure.

- [x] Đối chiếu rule cũ, tài liệu yêu cầu, contract, source và schema liên quan.
- [x] Owner xác nhận tài khoản vừa tạo/verify OTP.
- [x] Owner cung cấp ảnh Normal và POST enroll lớp19:400 PACKAGE_EXPIRED. Không cần thêm ảnh để kết luận lỗi điều kiện gói ở ca này.
- [x] Owner chốt bỏ guard gói chung cho Normal và giữ lịch sử/dependency; phát hiện thêm phải báo để duyệt.
- [x] Kiểm local H2 với fixture commit, login/auth-me thật: không có gói → Normal Pending; Cash Paid→Registered, giá hóa đơn giữ nguyên khi giá lớp đổi; VNPay chỉ tạo link local, retry không nhân đơn; waitlist→Pending, race chỗ cuối; owner/center/Inactive/block/lịch bị từ chối đúng; gói hết hạn/liên kết lịch sử không bị sửa.
- [x] Test đích và clean test đạt theo từng snapshot trong docs00/QA. B mới đã chuyển fixture đăng ký sang Normal và thay expected cấp quyền cũ bằng từ chối theo quyết định đã duyệt; giữ test hủy lịch sử riêng. PostgreSQL sau SQL16 cũng đã kiểm workflow hiện hành; không suy thành Render/FE hoặc gateway live. Lỗi setup sequence/import/JDBC timezone được sửa theo môi trường, không đổi rule để xanh.
- [ ] Sau deploy: kiểm lại tài khoản mới trên FE/Render, auth/me và API/state tương ứng; FE quyết định câu chữ trạng thái trống. Không sửa source FE trong lượt này.
- [ ] Nhánh404 riêng: cần body response catalog/17 và classType/status của lớp17; không gộp nguyên nhân với lỗi enroll lớp19.

**Đối chiếu audit:** xem bảng tại mục5 của [LAN-1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md). Có thể bàn giao trước các phần BE-01/BE-04/BE-05 đã nêu; không đóng trọn mục hoặc suy PASS từ policy tĩnh. BE-02/BE-03/BE-06 và chuẩn hóa BE-07 cần lượt riêng. Đề xuất lượt tiếp theo rà BE-01 để chốt danh sách API/quyền còn thiếu; chưa tự mở rộng sửa các mục này.

**Bước tiếp đề xuất để Owner duyệt:** rà BE-01 riêng để hoàn thiện ma trận còn thiếu. Catalog/17 cần body và trạng thái lớp17; deploy BE/kiểm FE vẫn còn mở, không suy từ migration hay test local.

<a id="be01-review-20261106"></a>
## BE-01 — Rà ma trận quyền, checkpoint 06/11/2026

**Snapshot review06/11, trước sửa guard:** lúc đó chỉ review tĩnh local, chưa gọi HTTP/DB hoặc sửa BE/FE/SQL; mốc source và giới hạn tại [Active](00-MASTER-CHECKLIST.md#active). Bảng dưới mô tả implementation tại điểm rà, **không phải trạng thái sau sửa07/11 hoặc policy được Owner duyệt**. Đọc [checkpoint07/11](#priority-guards-20261107)/docs01 cho phạm vi mới; toàn bộ BE-01 vẫn chưa hoàn tất.

**Nguồn và điều kiện:** audit BE-01 đề nghị nhóm API theo role nhưng tự ghi chưa xác nhận policy server; docs01 §2/§15 cũng chưa cam kết full role matrix. D01/D09/D10/D15/D16/D18 có quyết định riêng theo luồng, không quyết định thay cho toàn CRUD/RBAC. [SecurityConfig](../src/main/java/com/sportscenter/config/SecurityConfig.java) yêu cầu phiên cho các API dưới đây; [FirstLoginPasswordFilter](../src/main/java/com/sportscenter/config/FirstLoginPasswordFilter.java) có thể trả403 `PASSWORD_CHANGE_REQUIRED` trước controller. Không suy đã kiểm Active chỉ từ việc có phiên.

| Method / endpoint | Role/authority tại controller hiện tại | Phạm vi owner/center trong source đã đọc | Từ chối theo source / phần thiếu |
|---|---|---|---|
| GET `/api/roles` | Không yêu cầu role/authority bổ sung: cả năm role có phiên đều qua gate | RoleService đọc danh mục role toàn hệ thống, trả cả danh sách permission của từng role; không filter center | Anonymous401; chưa có403 riêng theo role. Chờ Owner chốt ai được đọc |
| GET `/api/roles/{id}` | Như GET roles | Đọc role theo ID toàn hệ thống, gồm permissions | Anonymous401; ID không có404; chưa có403 riêng theo role. Chờ cùng quyết định |
| GET `/api/permissions` | Như GET roles | Đọc toàn danh mục permission, gồm id/name/description | Anonymous401; chưa có403 riêng theo role. Chờ cùng quyết định |
| PUT `/api/roles/{id}/permissions` | `MANAGE_PERMISSIONS`; không AND role Admin | Đổi permission của role dùng chung toàn hệ thống; service lấy actor để audit, không có scope center | Thiếu authority403; role không có404. Chưa có quyết định xác nhận chỉ authority là đủ; chưa đổi quyền |
| GET `/api/users`, `/api/users/{id}` | `MANAGE_USERS` OR `MANAGE_RBAC`; không gate theo tên role | UserService dùng findAll/findById, chưa thấy filter owner/center ở hai đường đọc này | Thiếu authority403; ID không có404. Thiếu đối chiếu đầy đủ policy/fixture center, không coi findAll là quyết định cho phép xuyên center |
| GET `/api/members`, `/api/members/{id}`; GET `/api/staff` | members: `MANAGE_USERS` OR `REGISTER_MEMBER`; staff: `MANAGE_USERS` | Các đường đọc đã xem dùng danh sách/ID toàn cục, chưa thấy scope center; không lấy guard tạo tài khoản làm guard cho đọc | Thiếu authority403; member ID không có404. Chưa nghiệm thu role/owner/center của CRUD; chưa sửa trong lượt này |
| GET `/api/audit-logs` | `VIEW_AUDIT_LOG`; không AND role Admin/Manager | Controller gọi trực tiếp findAllByOrderByCreatedAtDesc; chưa có filter center/owner trên đường gọi này | Thiếu authority403; chưa xác nhận policy đọc theo từng role/center. Chỉ ghi nhận cho matrix, không xử lý gộp BE-06 |

Source đối chiếu: [RoleController](../src/main/java/com/sportscenter/user/RoleController.java), [RoleService](../src/main/java/com/sportscenter/user/RoleService.java), [RoleResponse](../src/main/java/com/sportscenter/user/dto/RoleResponse.java), [UserController](../src/main/java/com/sportscenter/user/UserController.java), [UserService](../src/main/java/com/sportscenter/user/UserService.java), [MemberController](../src/main/java/com/sportscenter/user/MemberController.java), [MemberService](../src/main/java/com/sportscenter/user/MemberService.java), [StaffController](../src/main/java/com/sportscenter/user/StaffController.java), [StaffService](../src/main/java/com/sportscenter/user/StaffService.java), [AuditController](../src/main/java/com/sportscenter/audit/AuditController.java). Mã lỗi trong bảng là nhánh source qua SecurityConfig/[GlobalExceptionHandler](../src/main/java/com/sportscenter/common/exception/GlobalExceptionHandler.java), chưa phải response quan sát ở lượt này; không hứa mọi403 có field `code`.

**Test/chứng cứ:** [RoleServiceTest](../src/test/java/com/sportscenter/user/RoleServiceTest.java) chỉ kiểm actor audit khi đổi permission bằng gọi service trực tiếp với authority rỗng; không chứng minh HTTP cho phép/từ chối role đó. Tìm literal ba nhóm `/api/roles`, `/api/permissions`, `/api/audit-logs` trong `src/test` chưa thấy test HTTP đích; không suy không có mọi hình thức test gián tiếp. [AuthorizationHttpTest](../src/test/java/com/sportscenter/config/AuthorizationHttpTest.java) dùng service mock cho gate một số workflow, không kiểm center/state commit. Bằng chứng integration đã có cho0/0.1 tiếp tục dùng đúng phạm vi tại Active/QA, không chạy lại hoặc gán thành PASS mới.

**Điểm dừng cần Owner chốt:** Member đã đăng nhập, không first-login, gọi GET `/api/roles` hiện có thể đi đến response chứa cả quyền của Admin/Manager; GET `/api/permissions` cũng không có gate quản trị. Audit xếp nhóm này dưới Admin nhưng không phải quyết định Owner; docs01 chưa chốt ai được đọc. Cần xác nhận giữ ba GET cho cả năm role hay giới hạn người quản trị (chỉ rõ role/authority, ví dụ Admin AND `MANAGE_PERMISSIONS`). Không dùng quyền ghi PUT để tự suy quyền đọc và không tự thêm/bỏ gate.

**Phần ma trận còn thiếu / bước tiếp sau quyết định:**

- Chốt ba GET trên, sau đó đối chiếu riêng quyền ghi RBAC và các thao tác users/members/staff còn lại; lập fixture cả năm role, authority có/thiếu, account Active/Inactive, own/other owner và cùng/khác center. Role→authority mặc định phải có nguồn xác nhận, không suy tên role hoặc dùng seed lịch sử làm policy.
- Rà tiếp từng method/path của specializations, subjects, rooms, classes, packages/subscriptions (phân biệt API legacy đã đóng), enrollments, advanced-registrations, sessions/center-incidents/holidays, attendances, training-plans/results, evaluations/feedback, invoices/payment-resolutions, reports, notifications/chat và API auth/public/avatar. Các nhóm này **chưa được rà đầy đủ service/test trong checkpoint này**; có route/annotation không đủ để hoàn tất matrix.
- Tận dụng quyết định đã chốt: training-results Member chỉ mình; payment-resolutions không cho Member/Coach; confirm-cash chỉ Receptionist+MANAGE_INVOICES; Normal/Advanced/legacy theo0.1. Không hỏi lại các quyết định này. D18 áp dụng report; không tự suy quyền audit từ quyền report.
- Với từng operation, ghi rõ gate role AND/OR authority, phạm vi query/mutation, lỗi401/403/404/400 và điều kiện Active; test phù hợp kiểm response/state sau commit. Chỉ cập nhật contract docs01 cho policy đã rõ; chưa công bố bảng quan sát này như contract FE.

<a id="design-review-20261106"></a>
## Rà tài liệu thiết kế toàn hệ thống — đề xuất 06/11/2026

**Kết luận:** vấn đề chính là nhiều tài liệu đang cùng mang dáng vẻ “hiện hành”, trong khi baseline CRUD/RBAC còn có đường không áp dụng cùng guard với workflow mới. Những điểm dưới đây là **đề xuất sau review tĩnh**, không là quyết định nghiệp vụ mới, kết quả test BE hoặc nghiệm thu Render. Không sửa BE/FE/SQL, không chạy DB/cloud; giữ các diff có sẵn. Mốc source/môi trường tại [Active](00-MASTER-CHECKLIST.md#active).

**Vai trò của bản rà này:** mô tả source/thiết kế trước nhóm sửa. Tiến độ hiện tại và phần còn thiếu của TK01–TK13 xem [checkpoint07/11](#priority-guards-20261107); không đọc câu “chưa sửa” hoặc phát hiện thiếu guard bên dưới thành trạng thái của source sau sửa.

### Phạm vi và cách đối chiếu

Đã rà bộ thiết kế hiện hành docs01/docs04, hướng dẫn vận hành/mô hình trong docs02/database README, checklist/audit và các bản bàn giao02–06/11; đối chiếu các đoạn thiết kế ở README/PROJECT_CONTEXT/FE-BE/note/DOCS_RESTRUCTURE_PROPOSAL và mô tả bảng trong `note db 2910.md`. Đã đọc yêu cầu trong [Member.pdf](../Member.pdf), [analysis 2410.pdf](../analysis%202410.pdf); kiểm trực quan bảng RP trang6 của Member. Chỉ mở đúng phần yêu cầu/quyết định liên quan của [bản yêu cầu18/10 lưu trữ](../old-docs/SCMS-Requirements-MVP-Role-Permissions-20261018.pdf) và báo cáo20vấn đề cũ để truy nguồn; không nạp hàng loạt changelog/log/backup. `System Account.pdf` là fixture danh tính, không dùng mật khẩu/tài khoản demo để thử live.

| Nhóm thiết kế | Căn cứ hiện hành / đối chiếu |
|---|---|
| Danh tính, hồ sơ, phân quyền | D01/D18, docs01 §1/2; SecurityConfig, AuthController, User/Member/Staff/Role controller-service, ActorPolicy |
| Danh mục, giá, sân, Coach | D02/D09/D12, docs01 §2/3; SubjectService, SportsClassService/Request, RoomService, SessionSlotValidator, CoachCapacityService; schema chỉ đọc phần liên quan |
| Normal/Advanced, lịch, quyền học | D03–D08/D12/D15, docs01 §4/6–11; đối chiếu mô hình DB, enrollment và đường booking/kiểm tài nguyên được nêu ở từng phát hiện |
| Tiền, đối soát, báo cáo | D10/D13/D16/D18, docs01 §5/13/14; phân biệt policy đã chốt với báo cáo/PDF cũ; ReportService và guard actor |
| Attendance/QR, giáo án, đánh giá | D05/D15/D17, docs01 §12; TrainingPlan/Evaluation controller-service-repository và kết quả training đã bàn giao |
| Thông báo/chat, migration, nghiệm thu | docs01 §15, D11/D19, docs02/database README; không suy AI/email/check-in cửa hoặc Google/cloud đã nghiệm thu từ schema/route |

Review thiết kế bao quát các nhóm trên; **không phải audit mọi dòng code/trigger hoặc test toàn bộ API**. Snapshot OpenAPI chỉ mở các operation/schema phục vụ phát hiện bên dưới; không dùng Swagger sinh từ code làm bằng chứng độc lập rằng nghiệp vụ đúng.

### Các phát hiện và đề xuất

**TK01 — P0: guard quản lý Admin có đường vòng qua API Member/Staff.** [UserService](../src/main/java/com/sportscenter/user/UserService.java) có `assertCanManageAdminRole`, nhưng `updateMember/updateMemberStatus` trong [MemberService](../src/main/java/com/sportscenter/user/MemberService.java) và `updateStaff/updateStaffStatus` trong [StaffService](../src/main/java/com/sportscenter/user/StaffService.java) lấy User bằng ID, chưa bắt buộc target đúng subtype/role hoặc áp lại guard Admin. Ví dụ actor có MANAGE_USERS gửi PATCH `/api/members/{adminUserId}/status`: source có đường đổi status của User Admin dù không gọi `/api/users`. Đây là thiếu guard trên đường source đã đọc, chưa tái hiện HTTP. **Đề xuất:** kiểm loại target, quyền quản lý target và center ở service chung cho mọi alias; từ chối trước mutation, audit ghi actor thật. Không xem việc FE chỉ truyền ID Member là bảo vệ.

**TK02 — P0: quyền ghi giáo án/đánh giá chưa ràng buộc người thực hiện và phân công.** POST/PUT/DELETE của [TrainingPlanController](../src/main/java/com/sportscenter/training/TrainingPlanController.java) và [EvaluationController](../src/main/java/com/sportscenter/training/EvaluationController.java) chỉ kiểm authority, không truyền Authentication vào service ghi. [TrainingPlanService](../src/main/java/com/sportscenter/training/TrainingPlanService.java) và [EvaluationService](../src/main/java/com/sportscenter/training/EvaluationService.java) nhận coachId từ request khi tạo, sửa/xóa theo ID và dùng Coach trên record để ghi audit; chưa kiểm actor thực/Active/center/phân công trên các đường này. Ví dụ Coach A có authority gửi coachId B hoặc sửa plan của B; log có thể mang danh B. Điều này lệch nguyên tắc không tin actor client và RP-36 theo phân công. **Đề xuất:** lấy actor từ phiên; kiểm target/assignment trước ghi, audit actor riêng với Coach phụ trách; quyền Manager hỗ trợ cần được mô tả riêng, không suy từ MANAGE_CLASSES.

**TK03 — P0: thu hồi quyền và trạng thái tài khoản chưa có cơ chế nhất quán cho phiên đang tồn tại.** [SecurityConfig](../src/main/java/com/sportscenter/config/SecurityConfig.java) giữ authority snapshot khi login; đổi role/status/permissions tại các service không đồng loạt hủy hoặc làm mới phiên. `/auth/me` trong [AuthController](../src/main/java/com/sportscenter/auth/AuthController.java) trả role từ User hiện tại nhưng authorities từ Authentication cũ. Ví dụ Manager bị đổi thành Member vẫn có thể nhận role Member cùng authority quản trị cũ; các endpoint chỉ có gate authority và không kiểm lại actor sẽ có rủi ro giữ quyền. `expireNow()` ở reset/reissue password cũng cần kiểm cơ chế thực sự từ chối request: cấu hình đã đọc chưa có filter kiểm expired SessionInformation. **Đề xuất:** thiết kế một cơ chế kiểm Active/quyền hiện hành và thu hồi phiên nhất quán; auth/me phải phản ánh quyền có hiệu lực. Chưa kết luận cookie/401 cũ trên Render cùng nguyên nhân này.

**TK04 — P1: phạm vi center/owner giữa CRUD baseline và workflow không đồng nhất.** Các đường đọc users/members/staff đã ghi tại [BE-01](#be01-review-20261106) dùng dữ liệu toàn cục; RoomService cũng dùng findAll/findById và Room mới mặc định center1, trong khi workflow đặt lịch có guard center. AuditController đọc toàn bộ system_logs bằng VIEW_AUDIT_LOG. Ví dụ Manager center1 lấy ID staff/room center2: annotation có authority không tự giới hạn bản ghi. **Đề xuất:** ma trận từng operation phải nêu query/mutation scope, kiểm tại BE; giữ thiết kế triển khai center1 đã chốt, không vì vậy cho phép API bỏ kiểm phạm vi. Quyền Admin đọc toàn hệ thống phải xác định theo từng luồng; không tự mở ngoại lệ tài chính/report hoặc đổi center của Admin.

**TK05 — P1: “sân đúng môn” chưa có mô hình kiểm đủ rõ.** PDF yêu cầu sân đúng bộ môn, danh mục20room phân biệt Tennis/Bơi/Bóng đá; Owner đã chốt không thêm subject_id vào rooms. Nhưng `validateRoomAndCapacity` của [SessionService](../src/main/java/com/sportscenter/session/SessionService.java) mới kiểm center/status/capacity; [SessionSlotValidator](../src/main/java/com/sportscenter/session/SessionSlotValidator.java) kiểm khung giờ/Outdoor; [CoachCapacityService](../src/main/java/com/sportscenter/membership/CoachCapacityService.java) ghép phòng dự kiến chủ yếu bằng Indoor/Outdoor. Ví dụ ca Tennis1:1 chọn sân Bóng đá còn trống có thể qua các kiểm này, hoặc tính năng lực nhận Coach dựa trên phòng không phục vụ môn đó. **Đề xuất cần chốt mô hình:** đặc tả khả năng phục vụ môn của tài nguyên và dùng cùng kiểm khi admission/book/generate/reschedule/substitute. Giữ quyết định không thêm subject_id; không tự lấy tên/mã hiển thị có thể sửa làm quan hệ nghiệp vụ. Cách biểu diễn capability cần Owner duyệt trước migration.

**TK06 — P1: tài liệu cũ vẫn tự nhận mô tả hiện tại, dễ làm khôi phục rule sai.** [FE-BE](../FE-BE.md) có mục “Contract thanh toán hiện đang triển khai” nhưng ghi15phút và callback sau hết hạn vẫn kích hoạt gói; [note](../note.md)/[đề xuất tái cấu trúc cũ](../DOCS_RESTRUCTURE_PROPOSAL.md) vẫn hướng dẫn24h, Member tự book và API bán gói chung. [PROJECT_CONTEXT](../PROJECT_CONTEXT.md) ghi hoàn thành100%, gia hạn gói/check-in tự do, trong khi docs00 còn giới hạn rõ. README mặc định SQLServer/reset demo và có link tới tài liệu đã chuyển. **Đề xuất:** đặt nhãn lịch sử/đã thay thế và liên kết docs00–04 ngay đầu các bản này; README làm lối vào hiện hành, không chép thêm contract hoặc số test. Giữ tài liệu bàn giao có ngày ở đúng thời điểm, không viết kết quả mới vào bản ngày cũ.

**TK07 — P1: nguồn giá và quyền chỉnh giá lớp chưa được diễn đạt thống nhất.** Member.pdf trang6 RP-13 cho sửa giá khi công bố lớp; trang8 vừa nói copy Subject sang Class vừa nói checkout hiển thị giá Subject. [SportsClassService](../src/main/java/com/sportscenter/sportclass/SportsClassService.java) thực tế lấy giá Subject khi tạo, giữ tuitionFee cũ khi update; [SportsClassRequest](../src/main/java/com/sportscenter/sportclass/SportsClassRequest.java)/OpenAPI vẫn nhận tuitionFee nhưng không áp override đó. Ví dụ Class tạo lúc900.000, Subject đổi1.000.000, FE nhập950.000 khi sửa lớp: ba con số đang có thể được người đọc hiểu là giá bán. **Đề xuất:** ghi rõ Normal lấy giá Class tại thời điểm tạo đơn; Advanced lấy Subject khi order; invoice/purchaseAmount đóng băng ngay lúc tạo đơn. RP-13 đã cho người có quyền định giá khi công bố lớp; chỉ phạm vi thay đổi sau công bố/đã có đơn có thể cần làm rõ. Không sửa chứng từ cũ và không lấy amount do Member gửi làm giá bán. Mặc định source hiện tại không hỗ trợ override phải được công bố rõ.

**TK08 — P1: status không hợp lệ có thể bị biến thành Active.** `normalizeUserStatus` của UserService/MemberService/StaffService trả Active cho mọi giá trị ngoài locked/suspended/inactive. Ví dụ PATCH status `Inactve`, chuỗi rỗng hoặc thiếu status có thể kích hoạt lại tài khoản thay vì báo lỗi; các controller status nhận Map và chuyển null xuống service. **Đề xuất:** phân biệt default khi tạo với validation thao tác đổi trạng thái; status thiếu/sai trả400, không mutation. Có danh sách transition rõ; không coi tên gần đúng hoặc null là lệnh mở khóa.

**TK09 — P1: quyền quản lý Manager chưa khớp ranh giới Admin trong yêu cầu gốc.** Member.pdf trang6 RP-04 và yêu cầu18/10 tách Admin quản lý Admin/Manager, Manager vận hành Coach/Receptionist. Nhưng StaffService chấp nhận role Manager/CenterManager khi tạo; UserService chỉ có guard đặc biệt cho Admin, chưa bảo vệ target role Manager tương tự. Với MANAGE_USERS (schema hiện hành có cho CenterManager), có đường tạo/chuyển role Manager. **Đề xuất:** thống nhất ranh giới tạo/sửa/đổi role/ngừng hoạt động của Admin và Manager trên mọi endpoint. Nếu giữ RP-04, Manager chỉ quản lý role cấp vận hành được cho phép; đây không phải lý do tự cấp MANAGE_PERMISSIONS hoặc mở toàn hệ thống. Đính chính sau đối chiếu lại: RP-04 đã có ranh giới này, chưa thấy quyết định mới thay thế; không coi thiếu mục Dxx riêng là lý do hỏi Owner chốt lại. Xem P1 bên dưới.

**TK10 — P2: policy đọc/ghi ma trận RBAC còn khoảng trống; chưa thể kết luận mọi quyền đọc là lỗi bảo mật.** Ba GET roles/permissions cho mọi phiên đọc như checkpoint BE-01; quyền PUT là MANAGE_PERMISSIONS, không AND Admin. [RoleService](../src/main/java/com/sportscenter/user/RoleService.java) dùng findAllById rồi thay toàn bộ tập quyền, chưa kiểm mọi ID yêu cầu tồn tại. Ví dụ gửi một permission ID đúng và một ID sai có thể chỉ giữ ID đúng, vô tình bỏ quyền khác. **Đề xuất:** Owner chốt ai cần đọc; khuyến nghị giới hạn màn hình quản trị bằng role+authority phù hợp, Member dùng authorities của chính mình từ auth/me. Với PUT, validate đủ ID rồi thay tập quyền nguyên khối và audit trước/sau; đề xuất thêm guard tránh bỏ quyền quản trị cuối cùng, nhưng đây là chính sách cần duyệt. Không tự áp dụng các khuyến nghị này hoặc coi việc đọc tên permission tự nó là leo thang quyền.

**TK11 — P1: OpenAPI còn quảng bá thành công cho tuyến legacy đã đóng và chưa mô tả đủ lỗi quyền.** [Snapshot](../artifacts/fe-handoff/openapi.json) của POST `/api/packages` vẫn chỉ khai báo201; roles/permissions chỉ200, các thao tác training đã xem chỉ200/201, không nêu gate/scope/401/403. Trong khi docs01 §4 đã chốt request tạo gói hợp lệ/đúng quyền luôn400 LEGACY_MEMBERSHIP_RETIRED. **Đề xuất:** annotation/schema phải mô tả operation legacy là deprecated/đã ngừng, lỗi retired và gate hiện có; bổ sung response quyền/lỗi theo từng operation rồi sinh lại một snapshot. Không xóa GET quyền Advanced chỉ vì tên kỹ thuật còn “package”; không tự chuẩn hóa toàn bộ lỗi BE-07 trong lượt review này.

**TK12 — P2: bảng hướng dẫn DB chưa liệt kê đủ dependency Google/retire.** docs02 có `13_google_login_postgres.sql`, phân biệt với các file13 room/capacity và nói Entity User cần google_sub trước deploy. database README §4 chưa đưa Google/16 vào bảng migration; §2 nói baseline29/10 cần13→14 rồi15, dễ bị hiểu là đã đủ cho BE ghép Google. **Đề xuất:** cập nhật bảng/dependency theo tên file, dẫn docs02 cho Google và16; tách “schema gộp đã chứa” với “DB cũ đã áp dụng”. Không yêu cầu Owner chạy lại SQL16: Supabase đã PASS và mọi bộ đếm0; migration Google cloud vẫn chưa được xác nhận.

**TK13 — P2: quyền Member xem giáo án theo lớp chưa rõ, dù đã có mô hình giáo án lớp.** TrainingPlan cho phép target đúng một trong classId/memberId. Nhưng [TrainingPlanRepository](../src/main/java/com/sportscenter/training/TrainingPlanRepository.java) lọc memberId theo `tp.member`, và service ép Member về ID phiên; plan chỉ gắn classId sẽ không khớp, `findById` cũng từ chối khi member null. Ví dụ Member đã Paid lớp Normal có giáo án chung nhưng danh sách có thể rỗng. **Đề xuất cần chốt:** Member có được xem giáo án lớp mình còn quyền học và lịch sử lớp đã học hay chỉ giáo án gán riêng? Nếu được, query phải kiểm enrollment/Paid/phạm vi lịch sử, không mở tất cả plan có classId. Chưa coi đây là IDOR hoặc tự mở quyền đọc.

### Những khác biệt đã có quyết định thay thế — không hỏi lại

- PDF/note cũ có24h, điểm danh trước/sau ca30phút, Member tự tạo Advanced, trừ credit lúc Absent hoặc giữ cả12ca trước mua: áp dụng D03–D05/D15/D17 hiện hành; book trừ1credit một lần, manual/QR trong[start,end), báo vắng là ngoại lệ trước buổi đã chốt. analysis2410 trang2 từng nói Absent tự động không được sửa; D05/D15 hiện cho Coach ghi thực tế, không khôi phục rule cũ.
- PDF cũ có cách đếm cuối ngày thứ45 hoặc hạn từ buổi đầu; D12 hiện là45ngày sử dụng từ Paid và trả timestamp chính xác, hợp nhất nghỉ/sự cố. Không sửa thành1080giờ hoặc cho FE tự cộng45ngày. Bảng ca trong Member.pdf trang11 tự mâu thuẫn giữa mô tả ca3/4 và bảng khóa Outdoor; giữ7ca/khóa3–4 theo D12.
- Báo cáo20vấn đề cũ cho thêm môn tùy ý/đổi Outdoor, nhưng D02 đã chốt9môn và phân loại; không tự mở lại danh mục. Giá/số buổi/capacity/20room đã chốt không bị thay bằng fixture hoặc tiêu đề “nhóm10–15người” của bảng giá PDF.
- Giữ Normal độc lập gói chung, giá dương, Paid mới Registered; Advanced qua chọn môn/Coach nhận; không giữ membership tháng/VIP làm điều kiện. SQL16 là phạm vi đã được Owner duyệt, không suy thành quyền reset hoặc xóa chứng từ hiện hành.
- Giữ Receptionist+MANAGE_INVOICES confirm-cash; Member/Coach không gọi payment-resolutions; Admin không tự có quyền tài chính/toàn center. Refund hẹp/D16 không phải hoàn tiền mọi khóa; không sửa DB thủ công sang Cancelled như RP-45 cũ.
- Report phân biệt người/lượt đăng ký, thực thu/hoàn/ròng,0credit còn ca hợp lệ; Holiday global có guard ảnh hưởng theo D14; check-in cửa và booking-block mặc định tắt; AI chưa bật mặc định. Các giới hạn này đã được công bố, không tự gán là bug mới hoặc thêm scope MVP.

### Thứ tự đề xuất và điều kiện kiểm chứng

1. **Ưu tiên tái hiện local TK01–TK04:** fixture riêng có Admin/Manager/Coach A–B, cùng/khác center và phiên trước/sau đổi quyền. Kiểm `/auth/me`, response và state sau commit; request bị chặn phải không đổi record/audit sai. Không dùng account demo lịch sử hoặc live seed để chứng minh.
2. **Chốt thiết kế trước sửa:** Owner xác nhận ranh giới quản lý Manager (TK09), mô hình sân đúng môn (TK05), chỉnh giá Class (TK07), RBAC đọc/ghi (TK10) và giáo án lớp (TK13). TK03 cần chọn cơ chế thu hồi phiên; không thay hạn hoặc quyền đã chốt để làm test xanh.
3. **Dọn tài liệu theo nguồn:** README/nhãn lịch sử (TK06), contract giá/scope đã chốt, annotation/OpenAPI (TK11), dependency DB (TK12). Không chép lại full API inventory hoặc tạo thêm nhiều báo cáo/handoff trùng nội dung. docs04 chỉ thêm quyết định khi Owner chốt; docs01 chỉ công bố policy rõ; docs00/QA mới ghi kết quả test/deploy.

**Trạng thái:** hoàn tất lượt rà và đề xuất ở mức thiết kế/source nêu trên; chưa triển khai các TK, chưa đóng BE-01–BE-07. BE tests/HTTP/DB/cloud NOT_RUN cho lượt này. Kiểm tài liệu gồm diff, UTF-8 và liên kết nội bộ; không lấy kết quả test cũ làm bằng chứng mới. Bước tiếp: Owner chọn phạm vi xử lý và các policy còn thiếu, sau đó làm từng nhóm có test đích/clean test theo AGENTS.

<a id="priority-guards-20261107"></a>
## Thực hiện ưu tiên và kiểm lại — 07/11/2026

**Phạm vi Owner đã cho làm:** TK01–TK04 theo phần guard có policy rõ, xử lý tài liệu cũ (TK06), bổ sung dependency DB (TK12) và dùng ảnh BE-03 để thêm kiểm enrollment. Không sửa FE, không chạy cloud/seed/reset hoặc SQL16 trên Supabase. Phần audit-log/Manager/RBAC/giá/sân/giáo án chưa có policy không được tự mở quyền.

**Ảnh BE-03 Owner bổ sung:** UI Coach tại lớp23/buổi đã hoàn thành báo Access Denied; GET `/api/classes/23/enrollments` và tuyến dự phòng GET `/api/enrollments?classId=23` đều403 qua FE localhost/Vite proxy tới BE Render. Ảnh không có `/auth/me`, SHA runtime hoặc dữ liệu chứng minh `classes.coach_id` là Coach trong phiên, nên chưa xác nhận403 của chính lớp23 là sai quyền. Nếu không được giao lớp thì403 là đúng. Không ghi tên/email hoặc cookie từ ảnh vào báo cáo.

**Snapshot phần sửa đã kiểm local trước đính chính RP-04/RP-36:**

- Member/Staff kiểm đúng loại target và center trước ghi; User mutation khóa target bằng `findByIdForUpdate`, tránh quyết định dựa trên role/center cũ khi có thay đổi đồng thời. Không tạo Admin qua POST staff; guard tạo/quản lý Admin của tuyến users vẫn giữ.
- Training-plan/evaluation lấy actor từ phiên, kiểm center và quan hệ phân công đã lưu; Coach không ghi thay Coach khác, không sửa/xóa record ngoài quyền bằng ID. Audit ghi actor thật. Giữ gate Manager+MANAGE_CLASSES hiện có trong center, chưa cấp quyền mới hoặc quyết định thay Owner về việc giữ/bỏ gate này.
- Mỗi API được bảo vệ đọc lại account/role/permissions; Inactive bị403 trước handler; đổi role hủy phiên cũ với403 SESSION_ROLE_CHANGED; permission thay đổi cùng role có hiệu lực request kế tiếp. Registry session đã thu hồi do đổi/cấp lại/reset password được thực thi thành401. Callback IPN/Return public không phụ thuộc cookie của Member.
- Users/members/staff/rooms lọc/kiểm center; Room mới lấy center actor. Tài khoản Admin center NULL giữ các phạm vi quản trị baseline đã có; không dùng ngoại lệ này mở quyền tiền/report. Audit-log chưa đổi vì thiếu policy/provenance center lịch sử, không coi TK04 toàn bộ đã đóng.
- Hai tuyến enrollment của ảnh dùng cùng guard phân công trong service; Coach được giao đọc được kể cả buổi đã kết thúc, Coach khác/khác center bị403. Bỏ filter chỉ trả lớp được giao, thêm authority quản trị không mở rộng phạm vi; Coach vẫn không được ghi enrollment chỉ nhờ quyền đọc này.

**Tài liệu cũ:** README đã làm lối vào bộ docs00–04 và hướng dẫn chọn DB script đúng vai trò. FE-BE/PROJECT_CONTEXT/note/DOCS_RESTRUCTURE_PROPOSAL/HANDOFF-T có nhãn lịch sử và dẫn nguồn thay thế; nội dung gốc giữ để truy nguồn, không dùng rule24h/15phút/gói chung hoặc count100% cũ làm requirement hiện hành. database README bổ sung migration Google/16 và phân biệt prefix13, schema gộp với DB đã áp dụng. Không sửa nội dung các bản bàn giao ngày cũ để chèn nghiệm thu mới.

**Kiểm và giới hạn:** kiểm cuối đã đạt theo [Active](00-MASTER-CHECKLIST.md#active)/`rbacPriorityGuards` trong QA: test guard có login/auth-me và state commit, đối chứng source trước sửa, clean suite, workflow PostgreSQL riêng và kiểm tài liệu. Các test fixture cũ chỉ gắn quyền vào request đã được bổ sung quyền thật trong DB; expected owner/center/phân công không đổi. Riêng actor Inactive nay bị guard403 trước handler: expected400 cũ được thay có giải thích; vẫn giữ và bổ sung ca staff Active đăng ký cho Member Inactive →400 MEMBER_NOT_ACTIVE. Các ca guard mới dùng H2, race kiểm row lock H2; PostgreSQL là hồi quy workflow hiện có, chưa chạy trọn bộ guard mới trên PG. Chưa nghiệm thu Render/FE hoặc khẳng định ảnh lớp23 đã hết lỗi trên runtime. TK01/guard TK02/TK03/TK06/TK12 xong trong phạm vi này; TK04 chỉ phần users/members/staff/rooms và training/enrollment đã nêu, audit/CRUD khác còn mở.

<a id="pending-policy-20261107"></a>
### Đối chiếu lại P1–P7 — rút bảng lựa chọn A/B trước đó

**Đính chính07/11:** lần đề xuất trước đã có đọc thiết kế nhưng phân loại chưa chặt: lẫn yêu cầu đã có với policy mới, lựa chọn implementation với lựa chọn nghiệp vụ, và quyền code đang cho qua với quyền đã được chấp nhận. Không yêu cầu Owner chốt lại cả bảy mục; bảng A/B cũ đã được rút, không dùng làm căn cứ sửa tiếp. Thiếu một mục trong bản tóm tắt Dxx không đồng nghĩa yêu cầu thiết kế đó chưa từng được xác định. Khi tài liệu gốc có rule đã bị Dxx thay thế thì chỉ bỏ đúng rule đó, không bỏ luôn mọi yêu cầu còn lại của tài liệu.

Căn cứ trực tiếp: [Member.pdf](../Member.pdf) trang6 RP-04/RP-13/RP-36/RP-43, trang8 luồng giá; [yêu cầu18/10](../old-docs/SCMS-Requirements-MVP-Role-Permissions-20261018.pdf) trang11/13/49/51 làm nguồn đối chiếu có thời điểm; [D02/D03/D04/D15/D18](04-DEBUG-NOTE.md) xác định phần đã thay thế/phạm vi hiện hành. Không dùng các rule24h/15phút/giữ12ca cũ trong PDF để phủ định Dxx.

| Mục | Căn cứ/ngữ cảnh | Kết luận sửa lại và phần thực sự còn thiếu |
|---|---|---|
| **P1 — Quản lý Manager (đã sửa trong checkpoint bên dưới)** | RP-04: chỉ Admin quản lý tài khoản Admin/Manager; RP-05: Manager quản lý Coach/Receptionist. Chưa thấy quyết định mới thay ranh giới này | Manager còn tạo/chuyển tài khoản Manager là sai lệch cần sửa theo thiết kế. Không đặt phương án ủy nhiệm Manager ngang hàng với yêu cầu gốc rồi bắt Owner chọn lại; ủy nhiệm là mở rộng mới chỉ bàn nếu Owner thực sự yêu cầu |
| **P2 — Đọc RBAC** | Admin cấu hình/cấp-thu hồi quyền là chức năng quản trị. Manager vận hành nhân sự vẫn cần dữ liệu role để gán Coach/Receptionist; đọc danh mục role không đồng nghĩa sửa permission hoặc cần xem toàn matrix | Đề xuất chặn toàn bộ GET cho người thiếu MANAGE_PERMISSIONS đã dùng quyền ghi để suy quyền đọc. Phải đối chiếu từng operation/field với flow trước: danh mục role được phép gán, xem cấu hình quyền và PUT thay quyền là ba hành vi khác nhau. Chỉ hỏi quyền đọc full matrix nếu flow cần mà nguồn chưa xác định; chưa tự thêm authority/endpoint hoặc cấm lookup cần thiết |
| **P3 — Audit-log** | RP-43 phân biệt Admin xem hệ thống/bảo mật, Manager xem hoạt động nghiệp vụ/dòng tiền; phạm vi vận hành theo center. D18 riêng cho report, không biến report thành audit | Thiếu dữ liệu xác định center/loại log là thiếu implementation/provenance, không phải lý do coi tước quyền đọc log của Manager là lựa chọn mặc định. Giữ mục tiêu đọc đúng nghiệp vụ/phạm vi, xác định query và dữ liệu nào đủ chứng cứ; log legacy không rõ phải ghi giới hạn cụ thể. Không hỏi lại “Manager có được đọc log không” |
| **P4 — Sân đúng môn** | Owner02/11 chốt20room, ID số, mã hiển thị, không thêm subject_id. Lịch phải dùng tài nguyên phục vụ đúng môn. Source hiện chỉ có Indoor/Outdoor/capacity và mã/tên, chưa đủ quan hệ kiểm môn rõ ràng | Rule sân đúng môn đã rõ; cấu trúc bảng capability hay cấu hình BE là lựa chọn kỹ thuật, không phải hai policy nghiệp vụ để Owner chọn. Rút đề xuất mặc định thêm quan hệ mới/hỗ trợ sân đa môn khi chưa chứng minh cần thiết. Phải rà mô hình room đã duyệt và đưa bản sửa nhỏ nhất kèm dữ liệu/guard cụ thể; không tự suy môn từ mã/tên có thể sửa, không lách quyết định không subject_id |
| **P5 — Giá lớp** | RP-13: giá Subject là mặc định, người có quyền được sửa giá khi công bố lớp. D02/AGENTS đóng băng Invoice.amount/purchaseAmount khi tạo đơn; không nói Class bất biến mãi. D03 chốt Advanced order lấy Subject.advancedPrice | Phương án “giữ Class900.000 vĩnh viễn” đã nhầm bất biến chứng từ với quyền định giá sản phẩm. Normal phải dùng giá Class đã được duyệt/công bố; Subject thay đổi không tự sửa lớp/đơn cũ. Ví dụ người có quyền công bố giá950.000 thì BE xác thực quyền/giá rồi tạo đơn950.000, không tin amount tùy ý từ Member. Chỉ điều kiện đổi giá sau công bố/đã có Pending hoặc người học mới có thể cần câu hỏi bổ sung; không hỏi lại quyền định giá lúc công bố |
| **P6 — Giáo án theo lớp** | Thiết kế có Coach lập kế hoạch theo lớp Normal hoặc cá nhân Advanced; Member theo dõi phần học của mình. Plan gắn classId có thể hợp lệ dù memberId NULL | Ép mọi quyền đọc qua tp.member.id sẽ bỏ mất quan hệ Member→enrollment→Class→plan. Phải đối chiếu/sửa ownership theo target của plan; không đưa việc bắt Coach nhân bản giáo án cho từng Member thành phương án tương đương. Chưa khẳng định mọi trường hợp đọc lịch sử sau hủy/đảo quyền đều đã được chốt; nếu cần chỉ hỏi ca biên đó, không mặc định cấp toàn bộ lịch sử hoặc biến chức năng cơ bản thành mở quyền mới |
| **P7 — Ghi giáo án/đánh giá** | RP-36 giao Coach theo phân công. Manager hỗ trợ đặt/dời lịch trong D04 hoặc sửa attendance trong D15 là các thao tác khác; không suy thành quyền ghi chuyên môn chung | MANAGE_CLASSES trong code và một test trả201 chỉ chứng minh implementation. Việc giữ nhánh Manager ghi chuyên môn chưa được chứng minh hợp RP-36; phải rà/sửa role+authority+assignment của đúng hành vi. Không đưa quyền Manager ghi kèm reason như lựa chọn đã có cơ sở ngang với RP-36. Nếu Owner muốn ngoại lệ mới, phải nêu nhu cầu cụ thể và tác động trước |

**Ảnh hưởng đến kết luận tại lúc đính chính (source trước checkpoint triển khai bên dưới):** guard actor/center/subtype/phiên và các quan sát test local vẫn là bằng chứng kỹ thuật đã chạy. Phần sửa tiếp theo ở [checkpoint RP-04/RP-36](#design-corrections-20261107), không đọc mô tả code trước sửa bên dưới thành trạng thái mới. Riêng `existingManagerTrainingPermissionIsCenterScopedAndAuditsManager` là characterization của nhánh code hiện có, **không nghiệm thu quyền Manager ghi chuyên môn**. `TrainingAccessPolicy.write` hiện chỉ ép coachId khi actor là Coach; các role khác có authority ghi vẫn cần kiểm trần role theo nghiệp vụ. Không kết luận TK02/full RBAC đã đúng chỉ từ test xanh. Các đường Manager quản lý Manager cũng chưa được sửa; chuyển từ “chờ Owner chọn policy mới” thành “sai lệch cần xử lý theo thiết kế”, trừ khi có quyết định mới thay thế.

**Bước tiếp sau đính chính:** đã tiếp tục sửa RP-04/RP-36/quan hệ giáo án lớp tại checkpoint bên dưới; tiếp tục đối chiếu từng operation với chức năng/actor/owner/center và nguồn quyết định, sửa các phần đã rõ; chỉ gom câu hỏi thật sự chưa có căn cứ (ví dụ đổi giá sau công bố hoặc quyền đọc lịch sử khi quyền học bị đảo). Lượt đính chính này chỉ sửa tài liệu/diễn giải QA, không sửa BE/FE/DB hoặc chạy/gán kết quả test mới.

**Việc kỹ thuật còn mở, không nhầm với policy mới:** TK08 validation status thiếu/sai; TK10 validate đủ permission ID, bảo vệ quản trị cuối cùng theo policy; TK11 response/deprecation/schema lỗi và full matrix BE-01; các nhóm CRUD/BE-03/BE-04 chưa được bao phủ toàn bộ. Không tự đánh dấu hoàn tất cả TK hoặc BE từ bộ test guard đích.

<a id="design-corrections-20261107"></a>
## Sửa theo đối chiếu thiết kế RP-04/RP-36 và giáo án lớp — 07/11/2026

**Expected độc lập:** RP-04/RP-05 và RP-36 trong thiết kế gốc, đối chiếu D01/D15 và nguyên tắc Paid/owner/center; không lấy code cũ hoặc Swagger sinh cùng code làm expected. Owner đã cho tiếp tục sửa phần có căn cứ rõ; không phục hồi bảng chọn A/B. Nhóm này không triển khai RP-13 định giá lúc công bố, scope audit-log, mapping sân/môn hoặc quyết định mới cho mọi trường hợp đọc lịch sử.

| Điều kiện nghiệm thu | Phần đã sửa / phạm vi |
|---|---|
| RP-04: chỉ Admin quản lý tài khoản Admin/Manager | Guard chung kiểm cả role target hiện tại và role mới. Áp dụng tạo User/Staff, PUT User/Staff, PATCH status/role, cấp lại mật khẩu; không lách bằng roleName/roleId hoặc alias Manager. Manager vẫn vận hành target Coach/Receptionist/Member đúng quyền/center. Admin phải đủ authority endpoint; quản lý Admin vẫn giữ MANAGE_PERMISSIONS. Không thay self-service password/avatar |
| Không giả actor hoặc ghi tranh chấp dựa target cũ | Giữ khóa target từ lượt trước, audit user administration ghi actor thật, không dùng NULL hoặc targetId làm người thực hiện. Kiểm Admin thao tác hợp lệ và request bị từ chối không đổi role/status/name/account/audit |
| RP-36: quyền chuyên môn không đi theo MANAGE_CLASSES | Cả controller và service yêu cầu Coach cùng authority chuyên môn đúng hành vi; không chấp nhận Manager/Admin/Receptionist/Member dù có thêm authority ghi. Giữ kiểm Active/center/phân công và coachId từ record/phiên. Manager correction attendance D15 là luồng khác, không bị gỡ |
| Member đọc plan chung theo quan hệ lớp | Query riêng cho Member: giữ plan gán trực tiếp đúng mình; thêm plan chung lớp Normal có Registered+Paid hợp lệ và đúng center. ID trực tiếp kiểm cùng điều kiện. Không lộ plan lớp khác hoặc plan riêng người khác; filter không mở rộng phạm vi. Pending/Waitlisted/Cancelled hoặc tiền đang đối soát không cấp quyền đọc chung |
| Bằng chứng kiểm | Đối chứng source trước sửa phải tái hiện sai lệch, sau sửa kiểm login/auth-me, response và trạng thái commit; kiểm controller và service trực tiếp, đủ role được cấp thêm authority, guard owner/center/khóa. H2 và PostgreSQL dùng cùng fixture conforming model; kết quả cuối tại Active/QA |

**Giải trình test expected:** case cũ cho Manager tạo/sửa/xóa giáo án được thay bằng từ chối403 theo RP-36 đã nêu ở lượt đính chính. Đây là sửa expected sai căn cứ nghiệp vụ, không phải đổi assertion theo implementation để xanh. Ca ghi hợp lệ của Coach được phân công tiếp tục giữ; bổ sung authority chuyên môn có/thiếu, các role khác cố dùng authority ghi, Admin quản lý Manager hợp lệ và Manager bị chặn ở các alias.

**Kết quả đã kiểm:** nhóm hiện tại đạt kiểm cuối theo [Active](00-MASTER-CHECKLIST.md#active)/`rbacDesignCorrections` trong [QA](../artifacts/qa/latest.json): đối chứng trước sửa, clean suite, guard đầy đủ trên H2 và PostgreSQL, workflow PostgreSQL và snapshot OpenAPI. Source hiện tại khớp các bản sao đã test. Không nghiệm thu FE/Render, ảnh lớp23 hoặc migration Google Supabase từ kết quả local này.

**Fixture và giới hạn:** dùng database local riêng, không.env/cloud hoặc account seed lịch sử. Lượt PG đầu dừng trước nghiệp vụ vì fixture Member thiếu join_date NOT NULL; sửa fixture bằng ngày tham gia hợp lệ, giữ constraint và chạy lại. Không gọi đó là case từ chối quyền thành công. Query plan Member thực thi trên PostgreSQL; không chỉ mock repository. Nguồn/code/schema đang kiểm được tách khỏi các snapshot trước trong QA.

**Phần còn lại theo thứ tự:** RP-13 định giá lúc tạo/công bố (đã rõ yêu cầu; điều kiện đổi sau công bố là câu hỏi riêng), RP-43 query/provenance log theo nghiệp vụ/center, kiểm sân đúng môn theo mô hình đã duyệt, phân biệt lookup role với đọc/sửa matrix, các ca lịch sử/đảo quyền nằm ngoài Registered+Paid và đầy đủ BE-01–BE-07. Không tự coi những phần này đã xong từ nhóm guard hiện tại.

**Bước nghiệm thu Owner yêu cầu07/11:** push/merge nhóm đã kiểm, xác minh đúng build BE rồi [kiểm phản hồi FE theo từng role](02-DEPLOYMENT-GUIDE.md#rbac-fe-verification). Chờ Owner báo thành công/không lỗi trước khi sắp xếp, giải thích và đề xuất phần còn lại. Không mở rộng sửa policy hoặc FE trong lượt phát hành.

<a id="ban-giao-session-rbac-20261106"></a>
## Bàn giao session RBAC — 06/11/2026

**Điểm dừng hiện tại:**0/0.1, nhóm guard ưu tiên và phần sửa theo [RP-04/RP-36/giáo án lớp](#design-corrections-20261107) đều đã kiểm local; checkpoint ghi rõ phần làm/giới hạn, không còn dừng ở review tĩnh. BE-01/full matrix còn mở; P1–P7 đã được phân loại lại, không phải bảy quyết định mới đều chờ Owner; không có mục BE-01–BE-07 nào được đóng toàn bộ. “Bàn giao trước” chỉ nghĩa là dùng phần API/bằng chứng đã kiểm, không phải nghiệm thu cả mục.

| Nội dung cần giữ khi tiếp tục | Trạng thái / nơi đối chiếu |
|---|---|
| Mục0 | Member Active đọc training-results của mình; Member/Coach vẫn không được gọi payment-resolutions. FE cần bỏ request ngoài quyền, không chỉ giấu lỗi403. |
| Mục0.1 | Normal không cần gói chung, giá phải dương, Paid mới Registered; Advanced dùng flow chọn môn/Coach nhận riêng. Đã đóng mua/gia hạn/ghi danh theo mô hình gói chung cũ. |
| SQL16 | Owner đã gửi PASS/model_retired=true; mọi bộ đếm xóa/tháo liên kết0. Không có dòng cũ cần xóa; trigger/marker đã áp dụng theo kết quả Owner. Không yêu cầu chạy lại hoặc reset schema. Agent không truy vấn Supabase độc lập. |
| A sau SQL16 | Đã bỏ suy luận Normal tạo sau Advanced là phụ thuộc; giữ eligibility thật/D16, kiểm lại khi Complete và giữ chứng từ gốc. Không cần SQL mới. |
| Bàn giao FE | [LAN-1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md) đã có lý do giữ/bỏ, phạm vi SQL/thực tế không xóa dòng nào, API và yêu cầu FE; câu “sau khi Backend ghi nhận” để FE quyết định tại đây. Docs01 giữ contract chính thức. |
| Audit có thể tận dụng | BE-01: quyền các API đã rà; BE-04: phạm vi Member trên các API đã kiểm; BE-05: policy/workflow Cash. Chi tiết và phần thiếu ở mục5 LAN-1; cả ba mục vẫn mở. BE-02/03/06/07 chưa có kết luận đầy đủ. |
| Bằng chứng và môi trường | Kết quả/hash/snapshot chuẩn tại [Active](00-MASTER-CHECKLIST.md#active) và các mục rbacIssue0, normalMembershipIndependence, normalPositivePriceGuard, legacyMembershipRetirement, cashCorrectionIndependence trong [QA](../artifacts/qa/latest.json). Chỉ mở mục cần dùng, không nạp cả kho log. |
| Code/Git | Owner báo PR18 đã merge main; checkout local hiện ở tai, mốc cụ thể tại Active. Đầu lượt BE-01 chỉ có hai file docs sửa sẵn, đã giữ nguyên; không còn mặc định BE/test chưa commit từ bàn giao cũ. Kiểm git status/diff trước mỗi lượt, không reset/ghi đè; SHA local không thay SHA runtime. |
| Chưa hoàn tất | Chưa deploy/nghiệm thu lại BE trên Render hoặc FE cho bản sửa này. GET sessions/catalog/17 còn thiếu body và classType/status của lớp17; không đồng nhất với POST enroll lớp19. |

**Lượt kế tiếp:** xem checkpoint RP-04/RP-36/giáo án lớp đã triển khai sau đính chính, tiếp tục các phần còn lại theo thiết kế. Không yêu cầu Owner chọn bảng A/B đã rút; chỉ hỏi tình huống thực sự còn thiếu quyết định. Guard ưu tiên và việc tài liệu đã kiểm xong theo Active/QA, không yêu cầu làm lại để ghi nhận lần nữa. Nếu tiếp tục BE-01, tách lookup role với đọc/sửa full matrix theo flow và nguồn thiết kế, rồi hoàn thiện ma trận còn thiếu; chỉ hỏi tình huống thật sự chưa có căn cứ. Không tự mở quyền hoặc triển khai gộp BE-02–BE-07; quyết định0/0.1 không hỏi lại. Deploy/Google migration/kiểm FE hoặc ảnh lớp23 là phạm vi live riêng, không suy từ local.

### Prompt mở session RBAC sau khi Owner chọn phạm vi

```text
Tiếp tục RBAC tại D:\Projects\SWP391.
Đọc AGENTS.md, Active của docs/00-MASTER-CHECKLIST.md, bảng quyết định docs/04-DEBUG-NOTE.md và mục “Bàn giao session RBAC — 06/11/2026” trong docs/03-TODO.md; sau đó mở đúng Dxx/API/source liên quan.
Đọc review thiết kế TK01–TK13 trong docs03; chỉ triển khai nhóm Owner chọn, không coi đề xuất review là quyết định được duyệt.
Nhóm guard ưu tiên/checkpoint07/11 đã kiểm local, xem Active/rbacPriorityGuards QA. Bảng A/B P1–P7 đã rút sau đối chiếu lại; không hỏi lại yêu cầu thiết kế đã rõ. Không chạy lại SQL16 Supabase hoặc dùng test local để tuyên bố ảnh lớp23 đã hết lỗi trên Render.
Tiếp tục checkpoint BE-01 trong docs03 và SCMS_ACCESS_DENIED_RBAC_AUDIT.md; quyền đọc ba GET roles/permissions đang chờ Owner chốt, không tự đổi gate khi chưa có quyết định. Mục0/0.1 đã xong BE local; BE-01–BE-07 chưa mục nào hoàn tất toàn bộ.
Dùng phần bằng chứng đã có và SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md để xác định phần ma trận quyền còn thiếu; không rà/sửa gộp các mục khác.
Kiểm git status/diff trước làm; giữ thay đổi hiện có. Không sửa FE. SQL16 đã được Owner chạy PASS trên Supabase, không chạy lại hoặc reset DB; BE mới chưa được xác nhận deploy.
Phân biệt quyết định, contract, code và bằng chứng. Chỗ mơ hồ hoặc quyết định nghiệp vụ mới thì giải thích cụ thể, dừng hỏi tôi; không tự mở quyền, không hỏi lại quyết định đã chốt.
Trả lời ngắn, rõ; cập nhật docs/03-TODO.md và tài liệu đúng vai trò theo AGENTS.md.
```
