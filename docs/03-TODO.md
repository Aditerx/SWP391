# SCMS — Checklist xác minh Access Denied / RBAC

Mở04/11/2026; cập nhật06/11/2026. Nguồn: [bản audit](../SCMS_ACCESS_DENIED_RBAC_AUDIT.md) và phản ánh Owner. Làm lần lượt **0 → 0.1 → BE-01 → BE-07**; không mặc định nội dung audit là lỗi. Khi còn mơ hồ hoặc cần quyết định nghiệp vụ mới, hỏi Owner trước khi sửa. Kết quả chuẩn tại [Active](00-MASTER-CHECKLIST.md#active)/QA.

## Checklist

- [x] **0 — Hai403 của Member, phần BE local và tài liệu:** đã sửa training-results, rà năm role payment-resolutions và ghi yêu cầu FE. Chưa deploy/nghiệm thu Render/FE.
- [x] **0.1 — Bỏ điều kiện gói chung cho Normal, phần BE local/tài liệu:** đã sửa và kiểm đăng ký/waitlist, thanh toán, owner/center/lịch, retry/race và giữ liên kết cũ. 0.1-B/C đã kiểm local, SQL16 đã được Owner xác nhận PASS; chưa deploy/kiểm Render/FE. Catalog/17 còn mở.
- [x] **0.1-B — Đóng tuyến ghi danh Advanced cũ:** hai tuyến enroll chỉ nhận Normal; không tự thăng waitlist Advanced. Test đích/clean đã đạt; final PG workflow sau migration cũng đạt. Chưa deploy Render.
- [x] **0.1-D — Loại mô hình gói chung:** BE đóng API cũ đã kiểm local; Owner gửi SQL16 PASS trên Supabase, model_retired=true, tất cả bộ đếm xóa/tháo liên kết0. DB không có dữ liệu cũ cần xóa; trigger/marker đã áp dụng theo kết quả này. Agent không tự truy vấn cloud; BE còn cần deploy.
- [x] **0.1-A — CashCorrection, BE local:** sau Owner xác nhận SQL16 PASS đã bỏ suy luận phụ thuộc theo thời gian; Normal Pending/Paid độc lập không chặn. Giữ liên kết thật/D16, kiểm lại khi Complete, giữ hóa đơn/ghi danh Normal và Paid/amount gốc; kiểm H2/PG theo docs00/QA, chưa deploy.
- [ ] **BE-01 — Ma trận quyền:** bàn giao được phần training-results/payment-resolutions và policy confirm-cash từ0/0.1; ma trận toàn bộ API/năm role chưa rà riêng.
- [ ] **BE-02 — Phiên và401:** chưa rà riêng; kiểm auth/me và API đích bằng cùng phiên, đối chiếu cấu hình hiện hành với log cũ.
- [ ] **BE-03 — Phạm vi Coach:** chưa rà riêng; kiểm phân công theo từng nghiệp vụ và cả hai đường enrollment, không mặc định tất cả đều thuộc lớp Normal.
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

- **Rule cũ:** [TECHNICAL_ANALYSIS §4.2](../old-docs/TECHNICAL_ANALYSIS.md) từng yêu cầu có gói Active còn hạn trước khi đăng ký lớp.
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
