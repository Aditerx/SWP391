# SCMS — Tiến độ và nghiệm thu

## Active

**Mục tiêu hiện tại19/11:** Owner cho phép sửa và kiểm BE local theo [update261118](update261118.md#ke-hoach-be), hoàn thiện bộ demo anchor19/11 trước rồi sinh bản21/11 từ cùng nguồn. Sau khi BE đạt, điền [bàn giao](../scms-ban-giao-2026-11-19.md); Owner copy SQL đã kiểm để chạy Supabase đúng ngày.

**Phạm vi hiện tại:** source/test BE, migration/bootstrap/generator/runner, PostgreSQL local riêng và hồ sơ demo. Owner tiếp tục cho phép commit, tích hợp lịch sử remote, push tai và merge main sau kiểm thử; không chạy SQL Supabase, sửa/test source FE, gọi provider/email thật hoặc deploy. Commit phát hành Git dùng [skip render] vì SQL26 cloud chưa được xác nhận. Bản21 chỉ kiểm SQL đúng21/11, không bỏ guard ngày. Baseline workspace trước sửa lưu tại QA `beDemoImplementation20261119`.

**Đợt bundle16/11:** chỉ sửa bộ sinh/runner và sinh lại SQL/fixture/SYSTEM-DATA theo anchor kiểm16/11; BE/FE source không đổi. Ngày cài chưa chốt; không bỏ guard ngày, không nới12h/24h/45ngày/attendance/trùng lịch/quyền. Giữ bằng chứng14/11 riêng; không coi kiểm local là Supabase hoặc FE PASS.

**Nguồn đối chiếu18/11:** FE main `585fde5ed1fd07e3406d6db41178e1c53cc51a9c` của [FE_Project](https://github.com/hoanganh21022005/FE_Project), lấy lại bằng Git từ remote; không dùng FE_Project-main. BE [Render](https://scms-app-p1gy.onrender.com/api/health) khai báo build `9ef289b8a19c5143dc7ac848d2755cdef6a8537d`, 20 file Java đích local khớp source build. [Website](https://vibesports.pages.dev) trả403 cho HTTP reader; chưa chứng minh SHA website, phiên/DB/cấu hình hoặc lưu FE mới. BE workspace HEAD `fac80f4be081c0f2b36214ad1c8c7fef182d7abd` có diff từ trước; chỉ kết luận khớp cho các file đã kiểm.

**Đối chiếu19/11:** FE main `03e83f8f946f524cc069c39d0064074168d7db86`, tám file Java đích local khớp BE `42984f4` đã được đối chiếu; lần tổ chức này kiểm hash vẫn giữ source đó, không thêm nghiệm thu runtime. Owner đã đồng ý invoiceId/invoiceStatus thay order đầy đủ, giới hạn ghi ban đầu sớm, field thiếu và các mã hủy; [bản phản hồi](../scms-phản-hồi-de-xuat-contract-fe-be-han-thao-tac-18-11.md) giữ nguyên. QA `feSevenPointContractReview20261119` là mốc trước phản hồi; trạng thái tiếp tại `beFirstDocumentationReorganization20261119`.

**Bằng chứng đầu session:** [QA](../artifacts/qa/latest.json) `beFirstDocumentationReorganization20261119` giữ kết quả tổ chức tài liệu, đường dẫn lưu bản chưa commit và kiểm bảo toàn nguồn. Tra `feImplementationPlanning20261118`/`feSevenPointContractReview20261119` khi cần nguồn đối chiếu cũ; không dùng trạng thái chờ Owner của mốc cũ thay chỉ đạo mới hoặc suy đã triển khai.

<a id="con-mo"></a>
### Việc đang làm

| Mã | Cần làm và điều kiện đóng | Trạng thái |
|---|---|---|
| FE-01 | FE bỏ/null content khi chỉ điểm danh; lưu/đọc lại giữ nhận xét | Việc FE sau bàn giao BE: sửa attendanceApi và bulk đang tự sinh content; chỉ nhắc tại [Owner mục5](update261118.md#nhac-fe-khi-ban-giao), chưa nghiệm thu FE |
| FE-02 | FE dùng PATCH result có sẵn cho nhận xét sau buổi, giữ attendance | FE đã có PATCH/editor nhưng form còn POST; FE sửa caller sau BE bàn giao, không giao BE viết source FE |
| FE-03 | Reception Normal Cash Paid/Registered, retry không nhân hóa đơn | Source FE đã chuyển Cash đúng caller; giữ kiểm hồi quy sau bàn giao, không giao thêm field lần nữa; chưa tạo giao dịch mới |
| FE-04 | Reception register, Manager create; đọc lại đúng Member | Caller FE còn đi createMember; FE xử lý sau bàn giao, BE giữ kiểm quyền/center và test API đúng route; email là phạm vi riêng |
| FLOW-01 | Phát triển ba sơ đồ thành bốn flow15–20phút; đúng wording/actor/API/state, nối nhóm feature và dữ liệu để rà schema | Đã soạn [bốn flow](reference/note-script-demo.md): sửa các bước24h/5phút, đề xuất→Accept, QR/attendance; thêm hỗ trợ/thay đổi/Refund, bảng bao phủ và nhánh phụ. QA `fourFlowDemoDesignReview20261114`: chỉ review tĩnh, chờ Owner duyệt/rehearsal/rà schema. VNPay/PAY-01 source mới đã đọc trạng thái Invoice BE; website/provider vẫn chưa kiểm mới; VietQR+SePay vẫn là đề xuất chưa tích hợp. Xem [phân tích](phan-tich-project.md#vnpay-demo-review), [cơ chế](02-DEPLOYMENT-GUIDE.md#vnpay-demo-mechanism), QA `vnpayDemoMechanismReview20261114` |
| DEMO-READY-01 | Giữ riêng hai bộ19/11 và21/11; Owner chạy SQL đúng ngày, kiểm sau reset theo phạm vi | Bộ19 SQL PG52kiểm + HTTP20:00/23:59:59 đạt local; SYSTEM-DATA xuất nguyên trạng trước HTTP, canonical đã thay đồng bộ. Bộ21 đã sinh/review15–18h, SQL/HTTP NOT_RUN tới đúng21/11; SYSTEM-DATA21 là thiết kế dự kiến. [SQL19](../database/bundles/2026-11-19/schema_postgres.sql), [bản21](../database/bundles/2026-11-21/manifest.json), [kịch bản](reference/SCMS-SCRIPT-DEMO-BE.md#demo-bundles-20261119); QA `beDemoImplementation20261119`. Supabase/deploy/FE/provider chưa kiểm |
| GIT-TAI-01 | Tích hợp lịch sử remote, giữ rule19/11, kiểm BE rồi push tai và merge main | Tai đã push thường f73da03 và CI44439751439 đạt clean/package/smoke; local tai đã đồng bộ, giữ cache Owner. Clean local787 tổng/36skip/0failure/error,53test PG được nhận đạt/0skip; giữ lỗi setup ban đầu. Đang merge vào main từ42984f4 bằng checkout tạm; sẽ kiểm CI đúng merge SHA, không tự deploy/SQL cloud, giữ [skip render]. QA `gitTaiIntegration20261119` |
| REVIEW-FE-BE-01 | Đối chiếu3tin FE về quyền Admin, lỗi Coach Accept và audit; Owner duyệt hướng sửa trước khi thực hiện | Đã kiểm source đúng runtime và API đọc có phiên; 403 đúng gate, nhật ký Advanced thuộc Manager, thiếu audit lúc gửi đăng ký; ca18 hiện Rejected. Đề xuất tại [tin nhắn](archive/2026-11/tin-nhan-backend.md#backend-review), QA `backendMessagesReview20261114`; chưa chạy ca ghi/sửa code trong lượt review |

File cần sửa, API và cách kiểm nằm trong bản FE; không nhân đôi hướng dẫn ở đây. Không coi build/mock/GET hoặc HTTP200 đơn lẻ là đã lưu thành công.

### Advanced — việc còn mở

| Mã | Cần làm và điều kiện đóng | Trạng thái / bước tiếp |
|---|---|---|
| ADV-01/FE | Tích hợp lớp riêng/Member/Coach/state, xác nhận mua24giờ và đề xuất → Accept; lưu/đọc lại đúng buổi/credit/quyền | BE lớp riêng đã đạt ca đề xuất/Accept/hủy sớm; FE/browser chưa nghiệm thu. Làm theo [bàn giao](reference/SCMS-BAN-GIAO-FE-ADVANCED-2026-11-14.md) |

Render chạy release `9ef289b8a19c5143dc7ac848d2755cdef6a8537d` từ [PR29](https://github.com/Aditerx/SWP391/pull/29); CI trước/sau merge đạt, Swagger UI/health và API có phiên đã kiểm. Lớp riêng live11khác bundle8vì giữ dữ liệu phát sinh; không áp quota fixture lên cloud. Rà thiết kế flow đã đọc FE main `7f44f0086cfdc687c3a9a3ff10890d1a4f998dc4` và phần không đổi từ `aa5d0b50aa133db243a09fe43026cf0768e42bfe`: có confirmationDeadline và caller đề xuất/Member Accept. Website có asset mới nhưng chưa chứng minh SHA hoặc lưu FE thành công; QA `fourFlowDemoDesignReview20261114`.

### Các luồng còn mở ngoài đợt này

Các mục dưới là giới hạn đã ghi ở các đợt trước, không tự mở quyền thực thi. Khi tiếp tục phải kiểm lại source, môi trường và phần bằng chứng liên quan; không triển khai lại phần đã có.

| Luồng | Phần chưa có nghiệm thu đầy đủ | Nơi tra khi được mở |
|---|---|---|
| Normal Lifecycle/Refund | Tích hợp và lưu/đọc lại trên FE; các cửa cutoff, consent, chứng từ và đối soát đúng vai | [Lifecycle](reference/SCMS-BAN-GIAO-FE-NORMAL-LIFECYCLE-2026-11-10.md), [Refund](reference/SCMS-BAN-GIAO-FE-NORMAL-REFUND-2026-11-10.md) |
| Attendance/Coach/Session1–3 | Correction/note, scope, shared-plan và các ô ngoài phạm vi đã kiểm; chưa có full FE/CRUD/live acceptance | [Attendance](reference/SCMS-BAN-GIAO-FE-ATTENDANCE-2026-11-09.md), [chi tiết Session1–3](reference/SCMS-CAP-NHAT/KIEM-TRA.md) |
| Ngày nghỉ | Repo đã có preview/token; chưa xác nhận thao tác ghi trên bản website tương ứng | [Contract Holiday](01-API-CONTRACT-FE-BE.md#holidays) |
| VNPay/QueryDR | Giữ phần đã xác nhận theo ca; chưa nghiệm thu đủ provider, callback, đối soát và quyền sau commit | [Bản VNPay](reference/SCMS-BAN-GIAO-FE-VNPAY-2026-11-04.md), QA `vnpayRuntimeNetworkEvidence` |
| OTP/Google/avatar | OTP có ca thành công Owner cung cấp; không suy toàn bộ lỗi/biên/onboarding/avatar đã đạt | [Bản OTP](reference/SCMS-BAN-GIAO-FE-OTP-2026-11-04.md), [Google](02-DEPLOYMENT-GUIDE.md#google-login) |
| AI Chat | Provider thật, DB tools/after-commit và FE/live còn thiếu bằng chứng theo từng nhánh | [DB tools](reference/SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-11.md), [lựa chọn nhân viên](reference/SCMS-BAN-GIAO-FE-AI-CHAT-2026-11-12.md), QA `deepseekChatDbTools20261112` |
| Snapshot/recovery | Đã có hướng dẫn; chưa tạo hoặc nghiệm thu mốc khôi phục mới | [Quy trình](02-DEPLOYMENT-GUIDE.md#demo-snapshot-recovery) |

<a id="da-giai-quyet"></a>
### Đã giải quyết trong phạm vi đã kiểm

| Nội dung | Kết quả và giới hạn | Bằng chứng |
|---|---|---|
| DEMO-RULE-02 — BE local | Nhóm2–6 đã sửa/kiểm theo Dxx: đơn/bulk/QR sớm, metadata, hóa đơn liên kết, hạn30phút/QR300giây, hủy đã điểm danh, migration26 và race PG. Clean cuối787 tổng/751thực chạy/36skip/0failure/error; các opt-in PG đã chạy riêng không skip. [Bàn giao thực tế](../scms-ban-giao-2026-11-19.md) đã điền5mục; contract/OpenAPI từ app đã kiểm; chưa deploy/FE/provider. Autoclean dọn phần tạm của phiên, giữ bằng chứng/fixture19 và source của Owner; số đo tại QA | QA `beDemoImplementation20261119`, SHA nền `fac80f4be081c0f2b36214ad1c8c7fef182d7abd` + fingerprint working tree |
| DEMO-SUNDAY-01 — bộ19/local | Nguồn bỏ loại Sunday, giữ ngày nghỉ riêng/Holiday/Outdoor; Normal12buổi Chủ nhật và Advanced đề xuất→Member Accept Chủ nhật đã kiểm, hủy hoàn đúng1credit. Ca hoàn mới SUB01-S01 Scheduled/Active11, proposal Accepted đúng chủ; không backfill consent lịch sử | Cùng QA; bộ21 SQL/HTTP vẫn NOT_RUN, cloud/FE chưa kiểm |
| FE-DESIGN-01 / mục5.2 | Kế hoạch từng đặt nhầm trong bản FE đã chuyển về ghi chú Owner; nội dung chưa commit được lưu một bản archive, root chỉ dẫn đường. Chưa thêm nghiệm thu BE/FE; lần autoclean cũ bị chặn vẫn giữ nguyên | QA `beFirstDocumentationReorganization20261119`, [kế hoạch BE](update261118.md#ke-hoach-be) |
| FE-CONTRACT-REVIEW-01 | Owner đã hiểu/đồng ý bảy điểm và hai lưu ý FE; giữ bản phản hồi, không suy FE đã nhận hoặc triển khai. Không còn chờ Owner quyết định các điểm này | QA `beFirstDocumentationReorganization20261119` |
| BUNDLE-LOCK-01 local | Anchor16/11: reset gate + khóa NOWAIT trước đọc; reader/writer/reset chồng dừng dưới1giây, giữ toàn bộ row/counter; hết tranh chấp reset đạt. PostgreSQL50ca, anchor12test, JPA validate/MockMvc3test không skip đạt; nguồn BE/FE không đổi. Bản14/11 giữ riêng; ngày cài/chạy cloud và full FE chưa nghiệm thu | QA `bundleResetLockFix20261116`; canonical SHA256 `1a4f20d3f9dff90073a58435cd2f9582117eee6b812624de58a6334f22c1b6f5` |
| Đính chính tài liệu17/11 | Sửa wording V2/docs04/Active và bổ sung docs05; giữ QA/bản nguồn, không coi Dxx bị thay thế toàn bộ hoặc prompt cũ là lệnh thực hiện. Chỉ đạt kiểm tài liệu; yêu cầu Owner chốt tiếp/phần còn mở ghi riêng, chưa triển khai | QA `demoRulesDecisionCorrection20261117`; tiếp theo `demoRulesOwnerDecisions20261117` |
| Bundle Advanced và autoclean | PostgreSQL local dựng mới/replay/reset có lịch sử đề xuất, BE validate/schema và luồng Normal/Advanced đạt; clean 762 run/34 skip, không failure/error. Dọn bản tạm/build và nén log giữ hash; chưa reset Supabase | QA `advancedBundleAutoclean20261114` |
| BUNDLE-ADV-02 | Bổ sung rule/chốt kiểm hai chiều đăng ký–lớp–quyền–Member buổi; canonical bằng đúng file đã kiểm. Offline12ca gồm9dữ liệu sai, PostgreSQL45ca đạt; fixture8Advanced/12Normal không đổi. Không chạy cloud/push/deploy; đã dọn container/volume local, giữ bằng chứng | QA `advancedBundleRules20261114` |
| ADV-DATA-01 local | Bỏ template vô chủ, mỗi đăng ký được Coach nhận có lớp riêng; bundle20lớp=12Normal+8Advanced của fixture. Clean763run/34skip/0failure/error; PostgreSQL/schema/upgrade/owner/race đạt. Phát hành được kiểm riêng ở dòng dưới | QA `advancedRegistrationClassCorrection20261114` |
| ADV-DATA-01 phát hành | Owner báo SQL25 PASS, cleanup IDs1–9; merge tai→main/CI trước–sau đạt. Swagger Playwright và API có phiên đạt; đề xuất4 → buổi189 đúng lớp29, credit12→11→12, retry/owner/audit/thông báo đạt. Chưa là FE/provider/toàn biên-race PASS | QA `advancedRegistrationClassRelease20261114` |
| Advanced BE trên Render | Đề xuất1 Pending/12credit → Accept buổi174 Scheduled/11 → hủy sớm Cancelled/12; hạn không đổi. Retry, sai owner/role, audit/thông báo đã đọc lại; không là FE hoặc toàn bộ biên/race/provider PASS | QA `advancedConsentLiveAcceptance20261114` |
| Schema Owner chạy14/11 | Readback READ ONLY xác nhận danh mục lễ, tên Advanced và FK người tạo Member; không cần thêm migration23 cho DB vừa dựng lại. Không chứng minh từng byte SQL đã chạy | QA `fePostResetReview20261114` |
| Smoke đọc sau reset | Có phiên đủ năm vai, đúng scope/catalog. Chưa chứng minh bốn thao tác ghi FE | Cùng mục QA trên |
| Phòng/RBAC trong đợt FE hiện tại | Phòng quá giới hạn bị400, form giữ lại và DB không đổi; phòng và refresh RBAC đã bỏ khỏi nhóm bắt buộc | Cùng mục QA trên |
| Session1–3 | Đã phát hành lại; smoke Render chỉ đạt phạm vi đã ghi. Demo Owner skip không thành browser PASS | QA `session123ReRelease20261113`; FE/live ngoài matrix vẫn chưa nghiệm thu |
| Bản FE hiện tại | Đã rút về bốn chức năng và5mục; sửa tên file theo bản đã kiểm, không tạo bản sao | QA `feHandoffWordingReview20261114`, `documentationLifecycle20261114` |

<a id="tai-lieu-theo-viec"></a>
### Tài liệu theo việc

Đầu session chỉ đọc AGENTS → Active → đúng bản bàn giao/Dxx/phần API cần dùng. Không nạp toàn QA, reference hoặc archive.

| Cần biết | Nguồn |
|---|---|
| Quyết định nghiệp vụ | [D01–D19](04-DEBUG-NOTE.md); yêu cầu chi tiết Normal/Refund ở `reference/` |
| Quy tắc demo đã đồng ý, phần chưa triển khai | [docs05](05-DEMO-RULE.md); không lấy ghi chú V2 thay Dxx hoặc bằng chứng triển khai |
| Kế hoạch BE trước bàn giao | [Owner](update261118.md#ke-hoach-be); sau khi BE đạt điền [khung bàn giao](../scms-ban-giao-2026-11-19.md). [Đọc-chốt cũ](archive/2026-11/FE-đọc-để-chốt.md) chỉ tra lịch sử |
| API/field/state/error | [Contract](01-API-CONTRACT-FE-BE.md), [OpenAPI](../artifacts/fe-handoff/openapi.json) |
| Chạy/test/deploy/recovery | [Vận hành](02-DEPLOYMENT-GUIDE.md) |
| Chọn script DB | [database/README](../database/README.md) |
| Chuẩn bị demo khi được giao | [Kịch bản](reference/SCMS-SCRIPT-DEMO-BE.md), [SYSTEM-DATA](../SYSTEM-DATA.md); bản dữ liệu sinh tự động giữ đường dẫn hiện có |
| Tra một checkpoint cũ | [docs00 lưu trữ](archive/2026-11/00-MASTER-CHECKLIST.md), [docs03 lưu trữ](archive/2026-11/03-TODO.md); lịch sử đã commit tra Git |

<a id="buoc-tiep"></a>
### Bước tiếp

1. Owner copy nguyên [SQL19](../database/bundles/2026-11-19/schema_postgres.sql) đúng19/11 giờ VN sau khi dừng BE/job/client DB, theo [docs02](02-DEPLOYMENT-GUIDE.md#demo-reset-deadlock). Chưa có quyền hoặc kết quả agent chạy Supabase; SQL PASS chỉ chứng nhận cài bộ, chưa thay kiểm nghiệp vụ.
2. Khi được giao triển khai BE tương ứng, áp dependency26 hoặc bundle dựng lại phù hợp, cấu hình demo cờ sớm true/hóa đơn30phút; đăng nhập mới và kiểm `/api/auth/me`, ID/state sau reset. Lượt này chưa commit/push/deploy hoặc đổi cloud.
3. Đúng21/11 mới kiểm SQL21 local, xuất lại SYSTEM-DATA nguyên trạng và kiểm HTTP ở15:00/18:00 trước khi giao Owner chạy. Giữ nguyên guard và bản A; không suy bộ21 PASS từ bộ19.
4. Nhắc Owner chuyển [bàn giao BE](../scms-ban-giao-2026-11-19.md) cho FE tích hợp, kiểm thủ công/quan sát và rehearsal bốn flow. FE-01–04/ADV-01 cùng Render/provider/email/browser chỉ đóng khi có lưu/đọc lại đúng phạm vi; chưa thực hiện trong lượt này.

## HISTORY

- 18–19/11 — Owner chốt phản hồi bảy điểm; mốc tổ chức tài liệu chuyển kế hoạch về update261118, lưu một bản đọc-chốt cũ và chuẩn bị khung bàn giao, chỉ kiểm tài liệu/bảo toàn nguồn; QA `beFirstDocumentationReorganization20261119`, nguồn trước ở `feSevenPointContractReview20261119`/`feImplementationPlanning20261118`. Sau khi Owner mở implementation19/11, BE local và bundle19 đạt, bàn giao đã điền; bundle21 SQL/HTTP chờ đúng ngày, Supabase/deploy/FE/provider NOT_RUN, QA `beDemoImplementation20261119`; kiểm Git sau commit f056f50 tái hiện non-fast-forward và43đường dẫn conflict với origin/tai, chưa tích hợp/push, QA `gitTaiPushConflictReview20261119`.

- 17/11 — Đính chính diễn giải V2/Dxx, giữ QA cũ; Owner tiếp đó chốt điểm danh sớm, chặn hoàn sau điểm danh, ca hoàn sẵn,30phút liên tục và QR300giây từ lúc cấp. Docs05 giải thích mốc/cutoff/phạm vi và FE đề xuất; code/DB/bundle/triển khai giữ nguyên, QA `demoRulesDecisionCorrection20261117`/`demoRulesOwnerDecisions20261117`.

- 16/11 — Bundle khóa reset local đạt với anchor16/11; ngày cài/cloud/FE còn mở, QA `bundleResetLockFix20261116`. Gom V2 thành bàn giao5mục và giữ bản nguồn; cách diễn giải duyệt trong QA `demoRulesSessionHandoff20261116` được đính chính ở mốc17/11, không dùng prompt cũ triển khai nguyên trạng.

- 14/11 — Review ba sơ đồ/bốn flow15–20phút và rule sau lỗi deadlock; chưa rehearsal/ghi FE/đổi rule, QA `fourFlowDemoDesignReview20261114`/`demoReadinessDeadlockRuleReview20261114`. Giải thích VNPay sandbox/link ký, phát hiện PAY-01 suy thành công từ Return; chưa sửa/nghiệm thu mới, QA `vnpayDemoMechanismReview20261114`.

- 14/11 — Owner duyệt bundle Advanced/autoclean; kiểm PostgreSQL local/BE, cập nhật SYSTEM-DATA và rule, siết generator/SQL postflight rồi thay bằng đúng bundle đã kiểm. Không đổi fixture ở lượt siết rule hoặc chạy Supabase; QA `advancedBundleAutoclean20261114`/`advancedBundleRules20261114`.

- 14/11 — Đối chiếu3tin FE bằng source đúng bản Render/API đọc: 403 theo quyền, audit Advanced thuộc Manager; thiếu audit gửi đăng ký và Rejected còn deadline. Lỗi409 trong ảnh nằm ở cửa BE cũ/DB mới, chưa có log constraint; đề xuất chờ Owner, QA `backendMessagesReview20261114`.

- 14/11 — Owner chỉ ra template Advanced sai; sửa BE/bundle local sang lớp riêng, kiểm đạt và chuẩn bị SQL giữ lịch sử, QA `advancedRegistrationClassCorrection20261114`. Sau Owner báo SQL25/cleanup PASS, merge PR29/CI/Render và Swagger Playwright/API đạt ca đề xuất4/Accept buổi189/hủy sớm hoàn credit; QA `advancedRegistrationClassRelease20261114`, chưa suy toàn FE/provider PASS.

- 14/11 — Owner mở nghiệm thu ghi Advanced; API Render đạt đề xuất/Accept/hủy sớm hoàn đúng credit, giữ lịch sử và hạn. FE đã bỏ dự toán theo Owner; bundle mới chỉ đề xuất, chờ duyệt. QA `advancedConsentLiveAcceptance20261114`.

- 14/11 — Rà/triển khai xác nhận mua24giờ và đề xuất/Member Accept; kiểm local trước/sau merge đạt, bàn giao FE. Sau Owner báo SQL24 PASS, phát hành PR28/CI và xác nhận Render/source/DB, API đọc/negative đạt; QA `advancedFlowReview20261114`/`advancedConsentImplementation20261114`/`advancedConsentRelease20261114`.
- 14/11 — Owner duyệt tổ chức tài liệu và cách duy trì sau mỗi đợt; gom việc hiện hành vào docs00, giữ yêu cầu/bàn giao còn hiệu lực ở reference và chuyển bản đã thay thế vào archive. Chỉ kiểm tài liệu/liên kết/công cụ sinh; không thêm nghiệm thu BE/FE. QA `documentationLifecycle20261114`. Owner chạy schema; readback/smoke đọc đạt phạm vi đã ghi, đối chiếu bốn vấn đề FE và rút gọn bản gửi FE. QA `fePostResetReview20261114` và `feHandoffWordingReview20261114`.
