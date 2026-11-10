# SCMS — Checklist xác minh Access Denied / RBAC

Mở04/11/2026; cập nhật09/11/2026. Nguồn: [bản audit](../SCMS_ACCESS_DENIED_RBAC_AUDIT.md) và phản ánh Owner. Làm lần lượt **0 → 0.1 → BE-01 → BE-07**; không mặc định nội dung audit là lỗi. Khi còn mơ hồ hoặc cần quyết định nghiệp vụ mới, hỏi Owner trước khi sửa. Kết quả chuẩn tại [Active](00-MASTER-CHECKLIST.md#active)/QA.

## Checklist

<a id="schema-session-handoff-20261110"></a>
### Bàn giao session10/11 — tiếp tục hoàn thiện schema và bộ demo

**Điểm nối:** phần thiết kế đã được Owner duyệt; không hỏi lại3–5. Source/schema/fixture chưa đồng bộ tới thiết kế mới. GET RBAC chỉ Admin đã sửa và kiểm local; Phase1 R3/status/permission/audit/giá/sân và attendance đã có phần kiểm local trước đó, không phải toàn bộ tính năng mới đều đã triển khai. Bằng chứng chuẩn tại [Active](00-MASTER-CHECKLIST.md#active)/QA; các bản bàn giao cũ giữ đúng ngày.

**Checkpoint hiện hành:** đọc [hoàn thiện Phase1/tích hợp main](#normal-phase1-complete-20261110). Snapshot triển khai đầu ở [foundation](#normal-phase1-foundation-20261110). Chuyển lớp/Refund/bộ demo đã được bổ sung ở nhóm sau; các đoạn “chưa triển khai/chưa hoàn thành” bên dưới mô tả lúc bàn giao cũ, không thay Active/QA hiện hành.

| Nguồn cần tiếp tục | Hiện trạng / cách dùng |
|---|---|
| [Normal Lifecycle](../SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Refund Normal](../SCMS_BE_Refund_Normal_DA_CHOT.md), Dxx trong docs04 | Yêu cầu đã chốt, gồm ngoại lệ xác nhận khai giảng muộn, chuyển lớp tương thích, contact tùyFE và7ngày TT mở. Đối chiếu source trước sửa, không tự nới quyền hoặc đổi Advanced |
| [SYSTEM-DATA](../SYSTEM-DATA.md), [build-system-data.py](../scripts/build-system-data.py) | DATA DESIGN:47account giữ33PDF,9môn/20room/21Class; N05→N11 Pickleball. Các dòng/field/key/state là thiết kế, có phần động/asset chưa tạo, không phải dump hoặc script seed hoàn chỉnh. Generator hiện dùng anchor cố định10/11, phải đổi sang anchor được truyền/kiểm khi thực thi fixture |
| [database/schema_postgres.sql](../database/schema_postgres.sql), [build-demo-schema.py](../scripts/build-demo-schema.py) | Bản đã kiểm trước đó chỉ có33account/9template Advanced; chưa có đầy đủ Normal/Refund/fixture47account. Không chạy bản này ngay trên Supabase. Chỉ trích guard runtime từ16, không chạy lại cleanup16 |
| [Schema root](../schema_postgres.sql), [database README](../database/README.md) | Root là biến thể bảo toàn baseline cũ, không phải file đích dựng lại mới. README có đoạn mô tả reset tham chiếu cũ; cập nhật khi chốt bản mới, không lấy tên file/marker làm bằng chứng đã đồng bộ |
| [Migrations18](../database/migrations/18_audit_scope_postgres.sql)/[19](../database/migrations/19_room_activity_postgres.sql)/[20](../database/migrations/20_class_publication_price_postgres.sql) | Owner từng chạy18 thành công,19 rollback vì ca164,20 chưa chạy. Audit/đọc lại không thay việc kiểm baseline khi deploy. Owner đã chuyển sang dựng lại demo, không tiếp tục vá ca164 hoặc yêu cầu consent ca demo cũ |
| docs01/docs02/OpenAPI | Contract hiện công bố khác với DTO/enum mới đang thiết kế. Chỉ cập nhật API đã implement/kiểm; không biến SYSTEM-DATA thành contract. Một canonical OpenAPI, không tạo bản Markdown trùng |

**Điều đã chốt mang sang:** giữ toàn bộ33account PDF và mật khẩu demo đã duyệt, chỉ bổ sung14account riêng;47account/21Class; N11 Pickleball làm đích N05 cùng1.800.000đ/center1, không tăng lớp. Normal12buổi, min=ceil(2*max/3), chỉ được tăng trong giới hạn khi tạo, khóa khi có đăng ký kể cảPending; chốt đăng ký48h, hạn chuẩn xác nhận khai giảng24h, Manager được xác nhận trễ trước T có reason/đủ điều kiện, không mở lại đăng ký. Ongoing không hủy thường; ưu tiên dời/thay/bù thật. Cash Normal thu tại quầy nguyên khối, không Member Cash online hoặc thăng waitlist tự tạo Cash. Chuyển cùng môn/giá/center và đủ lịch/chỗ/consent, recheck dưới khóa, giữ invoice/link chuyển quyền. Refund100% đúng phạm vi trước khai giảng: Manager duyệt→Receptionist chuyển ngoàiSCMS/nộpchứngtừ→Manager đối soát mớiCompleted. Contact từ hồ sơ Member đúng quyền, FE quyết định dùng/hiển thị; không nằm trong chứng từ hoặc thay xác minh.7ngày làm việc theo ngày TT mở, kể cảT7/CN trừHoliday, không dùng lịch nghỉCoach; ngày mở tiếp theo làngày1. Giữ D15 end/cutoff, D16 CashCorrection hẹp, D12 Advanced45ngày sử dụng và các toggle default tắt.

**Các điểm dễ làm sai cần kiểm chủ động:** Class Closed draft khác Closed kết thúc; nhánh makeup hiện kiểm Class Completed sai mô hình. Finalizer/lifecycle tại end vẫn cho correction Coach tới trước00:00, không để Class/EnrollmentCompleted vô tình chặn đọc/sửa hợp lệ. Chuyển quyền giữ invoice lớp gốc thì các query attendance/plan/result/report/eligibility phải nhận liên kết chuyển, không sửa Invoice.class/amount cho tiện. Schema53mục trong SYSTEM-DATA gồm47bảng hiện hành và6tênbảng dự kiến kỹ thuật, chưa chứng minh phải tạo đúng6bảng nếu có thể tái dùng mô hình tốt hơn. Chứng từ phải private; MediaService avatar/publicURL không tự đáp ứng PDF tài chính. Pending/QR/OTP cần tạo đúng thời điểm demo, không kéoTTL/giảcheck-in/đổiClockRender để giữ fixture sống.

**Giữ phạm vi thao tác:** session này chưa commit/push/deploy/reset/seed cloud. Tiếp tục trên working tree hiện có, giữ cả untracked/tài liệu/script/BE/test đã có. Có quyền chuẩn bị/kiểm local bundle demo, không tự suy thành được chạy Supabase/Render, chuyển khoản thật, gửi email/provider hoặc live tests. Backup/dump/credential riêng tư không đưa Git/báo cáo. Không reset DB ngoài DB local mới do runner tạo; không chạy lại SQL16. Nếu cần dữ liệu cloud mới thì chỉ đọc đúng câu hỏi/phạm vi, không nạp.env vào tests. A/B cùngcenter, giữ test/guard center đã có, không mở lượt rà center khác.

**Prompt copy sang session mới** (phần tiếp theo trong tài liệu là checklist thực thi):

```text
Tiếp tục tại D:\Projects\SWP391 theo checkpoint:
docs/03-TODO.md#schema-session-handoff-20261110.

Mục tiêu: làm Phase1 tại docs/03-TODO.md#remaining-two-phases-20261110,
hoàn thiện BE/schema/bộ demo Normal Lifecycle và Normal Refund đã được duyệt.
Không chỉ thêm bảng hoặc sinh lại SYSTEM-DATA rồi báo đã hoàn tất schema.

Trước sửa:
- Đọc AGENTS.md, phần đầu Active của docs/00-MASTER-CHECKLIST.md,
  bảng quyết định và đúng Dxx trong docs/04-DEBUG-NOTE.md, database/README.md.
- Đọc SCMS_BE_Normal_Lifecycle_DA_CHOT.md và SCMS_BE_Refund_Normal_DA_CHOT.md
  đã cập nhật10/11, checkpoint này, SYSTEM-DATA.md và script sinh dữ liệu/schema.
- Kiểm git status/diff và source/test thực tế. Giữ toàn bộ thay đổi hiện có,
  cả untracked. QA chỉ mở đúng nhóm liên quan, không nạp log/history hàng loạt.

Đã chốt, không hỏi lại:
- Giữ33account System Account.pdf, bổ sung riêng thành47account;21Class.
  N11 Pickleball là đích N05, cùng1.800.000đ/center1.
- Normal12buổi; minCapacity=ceil(2*max/3), chỉ tăng trong giới hạn lúc tạo,
  khóa khi có đăng ký kể cả Pending; chốt đăng ký48h.
- Manager xác nhận khai giảng chuẩn24h; được xác nhận trễ trước buổi đầu
  nếu có reason và đủ điều kiện. Không tự khai giảng/hủy hoặc mở lại đăng ký.
- Cash Normal do Receptionist đăng ký và thu ngay; waitlist không tự tạo Cash online.
- Chuyển cùng môn/giá/center, đủ lịch/chỗ/consent; khóa và kiểm lại khi xác nhận,
  giữ Invoice gốc và link chuyển quyền; không phù hợp thì phương án hợp lệ/Refund.
- Refund Normal trước khai giảng: chủ động có nghĩa vụ khi TT hủy;
  Manager duyệt → Receptionist chuyển ngoài SCMS/nộp chứng từ → Manager xác minh
  → Completed. Giữ Paid/amount gốc, không hoàn trùng, không mở hoàn sau khai giảng.
- BE chuẩn bị phone liên hệ từ hồ sơ đúng quyền, FE quyết định dùng/hiển thị;
  không đưa phone vào chứng từ/log chung, không thay bằng chứng ngân hàng.
- 7ngày làm việc là ngày TT mở kể cả T7/CN trừ Holiday;
  ngày mở tiếp theo là ngày1. Không áp T2–T6 hoặc đổi thành7ngày lịch.
- Giữ Advanced/D15/D16 và toggle default tắt. GET roles/permissions/role detail
  chỉ Admin đã kiểm local, không mở lại vì snapshot cũ.

Thực hiện:
1. Đối chiếu nguồn; lập mapping bảng/field/state/action/transaction/job với code,
   ưu tiên tái sử dụng. SYSTEM-DATA là DATA DESIGN, chưa phải seed/API hoàn chỉnh.
2. Làm theo P1.1→P1.6. Hoàn tất từng nhóm có kiểm và checkpoint rõ;
   không nhận toàn bộ schema đã xong khi mới có DDL hoặc data mẫu.
3. Schema dựng lại đích là database/schema_postgres.sql qua generator phù hợp;
   schema root là baseline bảo toàn cũ. Bản reset hiện tại mới có33account/9template,
   không được dùng để reset cloud. Không chạy lại cleanup SQL16.
4. Hoàn thiện fixture động và private DEMO assets, anchor được truyền theo Clock VN,
   FK/tiền/quyền học/lịch/RBAC. Xuất lại SYSTEM-DATA từ cùng nguồn đã kiểm.
5. Test local cô lập, không.env/cloud: test đích rồi clean test cho mỗi nhóm BE;
   PostgreSQL mới riêng cho SQL/lock/race/replay/commit. Không hai Maven clean cùng target.
6. Cập nhật docs đúng vai; chỉ sinh OpenAPI sau implementation đã kiểm.
   FE quyết định UI, không sửa LAN-2 ngày07/11 bằng thay đổi mới.

Không tự seed/reset Supabase, deploy Render, chuyển tiền hoặc gọi provider thật.
Không gộp R1/R2, validation tên/khu vực sân, policy quản trị/đổi giá sau công bố,
AI/QueryDR/Google/avatar vào Phase1 này. Giữ test/guard center đã có,
không mở lượt rà center khác. Nghiệp vụ mới cần nêu ví dụ cụ thể và hỏi trước sửa;
không hỏi lại các mục đã duyệt. Xưng Tôi–Bạn.

Cuối session: cập nhật Active/TODO/QA, báo phần BE/schema/fixture đã kiểm,
phần còn thiếu và bước tiếp. Không gọi DATA DESIGN/local PASS là Supabase,
FE/Render/CRUD/live đã nghiệm thu. Giữ diff hiện có cho session sau.
```

<a id="remaining-two-phases-20261110"></a>
### Toàn bộ việc còn lại — kế hoạch hai phase hiện hành10/11

Kế hoạch này thay **thứ tự điều phối** của hai phase09/11 bên dưới, giữ kết quả/lịch sử và các điểm còn phải duyệt. Chỉ công việc có căn cứ/đã duyệt mới triển khai; một hàng “review” không tự mở quyền sửa policy. Không bắt chạy lại phần đã đạt chỉ để đủ số lượng.

#### Phase1 — Hoàn thiện BE, schema và bộ demo đã duyệt; nghiệm thu local

| Mã / thứ tự | Việc còn làm | Đầu ra và điều kiện hoàn thành |
|---|---|---|
| P1.1 | Đối chiếu thiết kế, source, ORM, schema và data; chốt mapping kỹ thuật/migration/compatibility/lock order | Tái dùng phần đúng, tách state Class/draft/Session/Enrollment/Invoice/Refund.6tên bảng/field trong SYSTEM-DATA là thiết kế, không phải API đã có |
| P1.2 | Normal lifecycle:12buổi/Open, minCapacity,48h/24h và xác nhận trễ, start/close, dời khai giảng1lần, lựa chọn/chuyển lớp, hủy trước khai giảng | Guard mọi alias/job/callback; không hủy Ongoing thường hoặc đóng khi còn bù. Consent/version và khóa/recheck; giữ invoice/link quyền, đọc lịch sử và report đúng sau chuyển |
| P1.3 | Cash quầy nguyên khối, VNPay/Pending/cutoff/waitlist và tiền đến muộn | Không Member Cash online Normal hoặc autoCash khi lên waitlist; giữ TTL cấu hình trong cutoff. Không trùng suất/tiền; khoản nhận muộn cần đối soát, không bỏ tiền/cấp lại quyền. Giữ Advanced/CashCorrection |
| P1.4 | Refund Normal: nghĩa vụ khi TT hủy, Member chọn hoàn, receiver/version/duyệt lại, claim/attempt/private asset/đối soát/SLA | Đúng vai trò, upload khác Completed, contact tùy FE. Bảo vệ quyền đọc, chống trùng/vượt/hoàn ngược; report đúng kỳ, audit/thông báo sau commit. Không tự chuyển tiền hoặc mở policy sau khai giảng |
| P1.5 | Hoàn thiện schema/generator và fixture nền/động theo thiết kế được duyệt | Giữ33PDF/thêm14,21Class/N05→N11,20room/9môn; private DEMO assets, anchor/UUID/FK/credit/closure/lịch hợp lệ. Không giảm invoice_code_seq. Hoàn thiện phần hiện còn deferred trong SYSTEM-DATA |
| P1.6 | Kiểm local unit/HTTP, clean, PostgreSQL fresh/replay/rollback/race/commit/readback; cập nhật contract/OpenAPI/docs02/DBREADME/QA/SYSTEM-DATA | Một bundle rõ scope, không đụng hạ tầng/bảng ngoài; dependency lỗi phải rollback. Nguồn đã kiểm khớp, dữ liệu xuất từ fixture đã kiểm, không còn reset rỗng. Bàn giao riêng từng luồng Normal Lifecycle/Refund, FE tự quyết UI |

**Điều kiện raPhase1:** hoàn thành behavior BE đãchốt cùngschema/fixture tươngứng và kiểm local thực chất; nếu nhóm còn thiếu thì ghi đúngchưađạt, khôngphát hành bản “códata” mà API/job chưa hiểustate. Giữ code/tài liệu chưacommit, không tựchạySupabase/Render. Phần schema mới không cần đợi Google/avatar hoặc R1/R2 chưaduyệt.

<a id="normal-release-handoff-20261110"></a>
#### Điều phối mới sau Supabase PASS — phát hành tai→main, chờ smoke

Owner gửi PASS cho bundle demo đã kiểm và yêu cầu merge tai vào main. Không chạy lại DB reset/SQL16 hoặc mở provider. Source khớp bản nghiệm thu; phần commit/merge phát hành gồm source/schema/fixture/docs đã kiểm, giữ mọi thay đổi có sẵn. Kết quả Git/CI/SHA và bằng chứng Owner nằm ở Active/QA. [Smoke plan theo nhóm và kết quả mong đợi](02-DEPLOYMENT-GUIDE.md#normal-release-smoke-20261110) là bước tiếp sau khi xác nhận Render đúng build. Các đoạn giữ chưa commit/push ở checkpoint bên dưới là snapshot trước yêu cầu phát hành, không thay trạng thái mới này.

Chưa tự thực hiện smoke ghi dữ liệu; trước hết build/DB/login/auth-me/read scope. Cash/chuyển lớp/Refund phải dùng fixture được xác định, không tự tiêu thụ kịch bản FE hoặc làm chứng từ giả như giao dịch thật. FE browser theo đúng source/build hiện hành; R1/R2/policy chờ nhóm review riêng sau smoke.

<a id="normal-phase1-complete-20261110"></a>
#### Checkpoint P1.1–P1.6 đạt local, đã lấy main — chờ Owner review10/11

Nhóm BE/schema/fixture đã hiện thực; kiểm chuẩn và trạng thái cuối ở [Active/QA](00-MASTER-CHECKLIST.md#active). Checkpoint foundation bên dưới là snapshot trước nhóm này, không còn là danh sách thiếu hiện hành. Giữ toàn bộ diff/untracked; Owner đã yêu cầu lấy main sau kiểm local rồi báo conflict để review, chưa push/deploy/reset/cloud.

| Nhóm | Mapping và nghiệm thu cần đối chiếu |
|---|---|
| P1.1 | Tái dùng payment_resolutions/report và mutex center.22 thêm proposal/response/transfer, receiver/attempt/document/verification; Invoice.class_id/amount gốc, entitlement_class_id hiện tại. Chuyển khóa center→Class theo ID tăng→Invoice; tài chính center→Invoice, không provider thật |
| P1.2 | Snapshot lịch/version/fingerprint, dời1lần với mọi consent, chuyển recheck dưới khóa; source Cancelled enrollment/link đích. Im lặng không đồng ý; stale schedule chặn Accept nhưng giữ Refund. Hủy Open trước buổi đầu tạo nghĩa vụ chủ động, Ongoing bị chặn; giữ start/close/D15 |
| P1.3 | Cash quầy nguyên khối, cutoff48h/TTL, callback muộn Paid+đối soát; sau TT hủy có đúng một nghĩa vụ hoàn. Waitlist hết hạn/order hết hạn quay cuối hàng rồi mời tiếp, không tự Cash |
| P1.4 | Member receiver immutable version, Manager xác minh/duyệt, Receptionist UUID claim/nộp metadata/proof, Manager nguồn bank độc lập mới Completed. Pending/Failed/Reversed và khiếu nại giữ lịch sử; cần duyệt lại trước retry. Private proof/protected download/history, contact đúng quyền, SLA/ngày mở và nhắc; report kỳ thu/hoàn riêng |
| P1.5 | Schema55bảng SCMS và4backup cũ trong đúng phạm vi;47account/21Class/20room/9môn,2PDF DEMO private BYTEA, lịch sử tổng hợp rõ nhãn. N05→N11 thật qua API, M01/M03 Waiting, M02 Refund/mất quyền cũ. Advanced credit/hết hạn/phần động theo cùng nguồn đã kiểm |
| P1.6 | Test HTTP login/auth-me/commit/race/readback, clean; PostgreSQL fresh/replay/guard/FK+view rollback/sequence và fixture thật; OpenAPI từ app đã kiểm. SYSTEM-DATA xuất từ DB fixture nguyên trạng riêng, vẫn DATA DESIGN; hai bản bàn giao theo luồng, không sửa LAN-2 ngày07/11 |

**Sai lệch đã sửa khi double-check:** tag CUSTOM không hợp lệ chuyển CENTER_EVENT; không giữ Accepted khi chưa chuyển quyền; thêm credit bị thu hồi cho Advanced Expired; DTO Decision trùng schema Advanced được đặt tên riêng; GET chứng từ ghi audit chuyển sang transaction cho phép audit; stale target không được chặn Member chọn Refund; kiểm lại Paid/đối soát/liên kết quyền ngay lúc nhận chuyển; tách lịch sử verification/snapshot để không ghi đè chứng cứ. Expected fixture33account/rỗng/Member Cash Pending trước đây được cập nhật vì thiết kế47account/có kịch bản và Cash quầy đã duyệt, không nới quyền/đổi rule để test xanh.

**Kết quả lấy main:** đã pull origin/main vào tai bằng fast-forward; không có conflict. Main mới chỉ ghi merge PR21, nội dung tree bằng base trước pull; giữ nguyên toàn bộ code đã kiểm/diff/untracked, không thêm merge commit hoặc push Phase1. Hash trước/sau và bằng chứng source khớp ở Active/QA; có bản sao/stash phục hồi riêng. Không chạy lại BE chỉ vì SHA merge đổi nhưng nội dung không đổi.

| Phần review tích hợp | Kết quả / quyết định |
|---|---|
| Conflict nội dung từ main | Không có; không cần chọn ours/theirs hoặc sửa logic từ main |
| Diff/untracked có sẵn và Phase1 mới | Đã khôi phục nguyên nội dung/line ending bằng hash với snapshot trước pull; các thay đổi còn chưa commit |
| Cache Python sinh lúc kiểm maintenance | Chỉ bỏ cache mới của lượt này/khôi phục cache tracked vốn không nằm trong diff đầu session; không xóa artifact/source của Owner |
| Kiểm sau pull | Tree commit không đổi; đã giữ diff/source đúng byte. Sau đó bổ sung guard Paid/đối soát/liên kết quyền tại commit chuyển và chạy lại HTTP PostgreSQL/clean trên nguồn cuối; kết quả chuẩn ở Active/QA |

**Đề xuất để Owner duyệt tiếp:** review diff Phase1 và hai bản bàn giao trước; duyệt P2.1 với ngày VN chạy demo để tái sinh anchor/kiểm bundle đúng ngày, kế hoạch reset/phát hành BE và smoke FE có auth-me/readback. Chỉ sau smoke mới trình riêng P2.2 (R1/R2/policy) và P2.3–P2.7 (matrix/CRUD/FE/live/AI/QueryDR/Google/avatar theo thứ tự). Chưa duyệt thì chưa reset Supabase, SQL16, deploy Render, push hoặc gọi provider thật.

<a id="normal-phase1-foundation-20261110"></a>
#### Checkpoint triển khai Normal Phase1 — phần lõi, chưa đạt điều kiện ra Phase1

Phạm vi lượt này là phần lõi P1.1/P1.2/P1.3 và kiểm tương ứng P1.6. Kết quả kiểm chuẩn, môi trường và fingerprint source ở [Active](00-MASTER-CHECKLIST.md#active)/QA; không dùng bằng chứng này để nghiệm thu các hàng còn thiếu.

Bàn giao riêng phần đã có API: [Normal Lifecycle10/11](../SCMS-BAN-GIAO-FE-NORMAL-LIFECYCLE-2026-11-10.md). Chưa có bản bàn giao Refund mới vì chưa có implementation/kiểm tương ứng.

| Nhóm | Đã có trong source/schema | Còn phải thực hiện trước khi đánh dấu hoàn thành |
|---|---|---|
| P1.1 | Mapping bên dưới; migration21 additive, metadata legacy giữ NULL, reset generator nhúng21; không chạy DML16 | Mapping/migration/compatibility của proposal, transfer, receiver/attempt/private document và Refund chưa implement |
| P1.2 | minCapacity lúc tạo, khóa sau công bố/đăng ký; Manager confirm-opening chuẩn24h/trễ có reason trước T; job recheck rồi Ongoing; finalizer đóng Closed sau12buổi hợp lệ, giữ correction; chặn PUT trạng thái/type/ngày độc lập và alias sửa session trước khai giảng của mô hình mới | Dời khai giảng1lần với consent/version; proposal/lựa chọn; chuyển lớp giữ invoice/liên kết quyền; hủy trước khai giảng cùng nghĩa vụ hoàn. Các API hành động này **chưa có**, không dùng PUT hoặc incident để giả hoàn tất |
| P1.3 | VNPay mặc định; Cash chỉ Receptionist có REGISTER_MEMBER+MANAGE_INVOICES, tạo/thu/Paid+Registered nguyên khối;48h theo buổi đã lưu; TTL=min(cấu hình,cutoff); admission giải phóng hold đã hết; callback trễ giữ Paid+reconciliation; waitlist chỉ mời, không tạo Cash/quyền | Hoàn thiện mời lại/xếp hàng khi expire, retry/race kết hợp proposal/chuyển/hủy/Refund và chống bỏ sót nghĩa vụ tiền đến sau hủy. Không lấy kiểm suất cuối hiện có thay toàn bộ các race này |
| P1.4 | Đã đối chiếu PaymentResolutionService: Refund hiện có chỉ tiền Paid chưa cấp quyền; CashCorrection giữ phạm vi D16 | Toàn bộ Normal Refund, Member response/receiver/version/xác minh/duyệt lại/claim/attempt/private asset/đối soát/SLA07ngày TT mở/contact tùyFE/report/audit và thông báo sau commit |
| P1.5 | Bản schema tham chiếu cũ đã thêm cột lifecycle; guard FK ngoài phạm vi/sequence vẫn được kiểm | Vẫn33account/9template, **chưa47account/21Class**, chưa private DEMO assets/fixture động/anchor thực thi. SYSTEM-DATA và build-system-data vẫn DATA DESIGN; không dùng schema hiện tại reset cloud |
| P1.6 | Test đích và bộ kiểm local của phần lõi; runner migration21/fresh/replay/rollback; contract/OpenAPI chỉ phần implement | Kiểm hợp nhất toàn bộ Phase1, xuất dữ liệu từ fixture đã kiểm và bàn giao Refund riêng; nghiệm thu FE/Render/live nằm Phase2 |

**Mapping kỹ thuật đã đối chiếu:**

| Đối tượng | Nguồn hiện có / cách tiếp tục |
|---|---|
| Class/draft | `classes.status` chỉ Open/Ongoing/Closed/Cancelled; `pricing_draft` phân biệt Closed chưa công bố với đã kết thúc. `min_capacity`, opening actor/time/reason, postponement_count và lifecycle_version do migration21 thêm; không có Class Completed. Source: SportsClassService/NormalClassCreationService/NormalLifecycleService/NormalClassPolicy |
| Session/attendance | Tái dùng SessionService, incident/makeup và AttendanceFinalizationProcessor. Completed là Session/Enrollment; center_invalidated/makeup_for giữ buổi không tổ chức. Guard mới không cho finalizer/attendance tạo lịch sử học trước khi mô hình mới đã Ongoing. Closed sau buổi cuối không bỏ cửa correction D15; sự cố muộn mở lại đúng nghĩa vụ Ongoing |
| Enrollment/Invoice | Pending giữ chỗ có hạn, Registered cần Paid hợp lệ. Invoice amount/class gốc bất biến. Admission/Cash/IPN/QueryDR dùng center mutex trước; nhánh Normal khóa Class rồi Invoice trước thay quyền. Không tự biến một hóa đơn Paid cũ thành hóa đơn lớp đích |
| Proposal/transfer — còn thiết kế kỹ thuật | Dùng nhóm `normal_class_events`, `normal_member_responses`, `normal_enrollment_transfers` của DATA DESIGN nếu phù hợp; payload phương án cụ thể + version/fingerprint lịch. Preview không giữ chỗ, Member response không tự là quyền đã chuyển. Confirm dưới center mutex rồi khóa hai Class theo ID tăng, Invoice/Enrollment và recheck; update mọi truy vấn Paid/attendance/plan/result/report để theo link chuyển |
| Refund — còn thiết kế kỹ thuật | Có thể mở rộng `payment_resolutions` với scope Normal, nhưng phải chặn endpoint generic bỏ qua workflow mới. Tái dùng unique invoice/type và report tiền riêng; thêm receiver revision/approval snapshot, attempt claim/idempotency, private document và independent verification. Sáu tên bảng trong DATA DESIGN không buộc tạo đúng sáu bảng. MediaService avatar/public URL không đáp ứng chứng từ tài chính |
| Job/thông báo | NormalLifecycleJob cảnh báo cutoff/thiếu xác nhận và không tự hủy; NotificationEvent sau commit có dedupe. Giữ finalizer/review D15, expiry D10 và toggle chế tài tắt. SLA Refund tương lai dùng Holiday global hiện hành, kể cả T7/CN mở; giữ lịch sử hạn cũ khi thay receiver |

**Tiếp tục session sau:** giữ toàn bộ diff/untracked, đọc checkpoint này cùng yêu cầu đã chốt. Hoàn thiện P1.2 còn thiếu gắn với P1.4 trước khi xuất fixture; không dựng7hồ sơ Completed mẫu khi API chưa kiểm được chuỗi actor/chứng từ/đối soát. Sau đó hoàn tất P1.3 liên quan, P1.5 và nghiệm thu P1.6; chỉ lúc đủ mới đánh dấu Phase1 đạt. Không chạy Supabase/Render/SQL16/provider thật hoặc mở Phase2. Hướng dẫn kiểm riêng migration/local tại docs02; schema root bảo toàn chưa được nâng21.

#### Phase2 — Phát hành, nghiệm thu chung và xử lý phần thiết kế/tích hợp còn lại

| Mã / thứ tự | Việc còn làm | Điều kiện / giới hạn |
|---|---|---|
| P2.1 | Owner chạy bundle demo đã kiểm; xác nhận schema/row state; sau đó push tai/merge main/deploy và smoke FE đơn giản | Kiểm baseline/recovery/build trước. Không suy quyền chuẩn bị script thành quyền reset cloud. Không chạy16/18/19/20 nối tiếp máy móc nếu bundle đã nhúng. Giữ thay đổi main mới và kiểm tích hợp; smoke không thay nghiệm thu chung |
| P2.2 | Sau smoke, trình lại mục5cũ: R1/R2, validation tên/khu vực sân và policy còn mở | R1 lọc ngày attendance; R2 content sau buổi/chưa có record/đổi Coach. Room name50/location100, giá sau công bố/đã Pending/người học, lịch sử sau hủy/đảo quyền, guard quản trị cuối cùng/PUT phân quyền. Chỉ sửa sau duyệt cụ thể; không hỏi lại policy đã chốt |
| P2.3 | Hoàn thiện matrix BE-01–BE-07/CRUD/error/deprecation/readiness | Năm role, actor/authority/owner/assignment/state; tái dùng kiểm đã đạt, giữ guard/test center. Rà phần còn thiếu của cookie/timeout/proxy, Coach/Member, Cash/Refund, report/audit,401/403/error contract. Đối chiếu phạm vi nhãn P1.5/gói T cũ, không coi tên gói là nghiệm thu |
| P2.4 | Nghiệm thu chung FE hiện hành/Render/CRUD theo matrix và bộ demo | Xác định repo/branch/build FE thật. Auth/me trước nhóm có phiên; response + SQL sau commit + retry/race cho tiền/quyền/lịch. Normal/Advanced, attendance/note/QR, plan/result/evaluation/feedback/at-risk, roster/catalog, room/holiday/incident/makeup, report/log/chat/notification. Phiên UI kiểm sau flow; live chỉ đúng phạm vi đã cho phép |
| P2.5 | Rà/chốt QueryDR, hoàn thiện AI còn thiếu và kiểm tích hợp liên quan | QueryDR độc lập OTP: đối chiếu quyết định/config được phép xem/hành vi rồi chốt dùng/tắt/để sau; không tự bật hoặc xóa pay attempt. AI còn thiếu: chốt FAQ/provider/quota/fallback/handoff, fake provider trước. OTP chính đã Owner xác nhận xong, chỉ cân nhắc biên sai/hết hạn/dùng lại/rate khi liên quan; không triển khai lại |
| P2.6 — sau bàn giao các phần khác | Google login và avatar | Giữ code ghép từ main; kiểm config/migration/provider/browser/storage đúng quyền. Không chặn bàn giao các phần khác vì Google/avatar; không suy cột Google có trong DB là live PASS |
| P2.7 | Chốt nghiệm thu và tài liệu cuối | Từng tiêu chí PASS/FAIL/BLOCKED/NOT_RUN đúng môi trường/build/actor, có giới hạn và Owner acceptance; cập nhật Active/HISTORY/QA, bàn giao theo luồng, không sao count/SHA hoặc sửa LAN-2 ngày07/11 |

**Tái kiểm có điều kiện:** F05/F06/F12 đãOwnerOK; cácphát hiệnFE khác làlịchsử/cầnkiểmlạitrênbuildmới, không tựcoicònlỗi. Nghiệmthu luồng mở rộng Normal/Refund mới không làm mất xácnhận cũ, nhưng không dùng xácnhận cũ để bỏ kiểm hànhvi mới. Chế tàibooking/check-incửa giữdefaulttắt; SQLServer/ticket/ai_consultations chưađược chọn làmphạmvi triểnkhai, không tự thêmviệc chỉvì cóbảng/code.

<a id="system-data-review-20261109"></a>
### Bộ dữ liệu demo và rà Normal Lifecycle/Refund — review09/11, cập nhật duyệt10/11

**Cập nhật Owner10/11:** đồng ý bộ đề xuất chung, yêu cầu bảo toàn bộ account trong `System Account.pdf`; đã đồng ý chuyển lớp cùng giá/cùng center, đang hỏi thêm để hiểu Cash/waitlist, xác nhận khai giảng24h, xác minh người nhận, lịch7ngày làm việc và log theo trung tâm. Không coi các câu hỏi này là đã chọn hết ngoại lệ hoặc cho phép triển khai ngay. Đã đối chiếu lại PDF với seed: đủ33email/họ tên, thông tin liên hệ/ngày sinh và mật khẩu demo khớp;14account bổ sung không thay danh tính gốc. Các lựa chọn chi tiết tiếp tục ở E/F; chưa đổi BE/SQL hoặc xuất SYSTEM-DATA.

**Chốt tiếp10/11:** Owner duyệt Cash/waitlist, xác nhận khai giảng muộn có kiểm soát ở E3 và hướng log center ở F. Tiếp tục giải thích kiểm lớp tương thích/không có lớp phù hợp, flow người nhận/điện thoại và tính cần thiết của T2–T6; chưa coi lịch7ngày đã đổi sang ngày lịch. Riêng yêu cầu GET roles/permissions chỉ Admin được triển khai trong lượt này, gồm khóa GET role theo ID để tránh đọc permissions qua tuyến chi tiết; kiểm/trạng thái chuẩn tại Active/QA. Không gộp triển khai Normal/refund/R1/R2 vào thay đổi GET này.

**Đã chốt phần3–5 sau cùng10/11:** Owner đồng ý kiểm tương thích/fallback và N11 Pickleball làm đích N05; đồng ý chuẩn bị dữ liệu liên hệ Member để FE tự quyết có dùng; đồng ý ngày làm việc theo ngày TT mở, tính T7/CN nếu không đóng theo Holiday. Các đề xuất T2–T6/đổi7ngày lịch trước đó đã bỏ. Yêu cầu Refund đã đồng bộ quyết định; chuẩn bị [SYSTEM-DATA](../SYSTEM-DATA.md) theo từng bảng ở mức dữ liệu gốc đã duyệt, tách rõ thiết kế với DB đã seed. Chưa triển khai Normal/Refund hoặc chạy reset/cloud.

**Yêu cầu mới nhất:** duyệt lại account/lớp/Coach và bộ kịch bản demo trước; sau duyệt mới sinh script và `SYSTEM-DATA.md` chia từng bảng để thống nhất FE. Owner đồng ý bỏ legacy, lớp/lịch thử, dữ liệu tồn, audit và backup cũ, nhưng muốn thay bằng **dữ liệu demo có kịch bản**, không để toàn bộ nhóm nghiệp vụ rỗng. **Chưa chạy bản reset rỗng trước đó; chưa sửa BE/SQL/contract/OpenAPI theo hai yêu cầu mới.** Phần này là review source tĩnh, không phải nghiệm thu tính năng mới; bằng chứng local trước đây giữ đúng phạm vi trong Active/QA.

Nguồn đối chiếu: **L§n** = mục n của [Normal Lifecycle](../SCMS_BE_Normal_Lifecycle_DA_CHOT.md); **R§n** = mục n của [Refund Normal](../SCMS_BE_Refund_Normal_DA_CHOT.md). Hai file giữ nguyên nội dung Owner cung cấp. Advanced tiếp tục D03/D04/D10/D12/D15/D16, không đổi policy bằng tài liệu Normal. API đang công bố ở [docs01 Normal](01-API-CONTRACT-FE-BE.md#normal) và [resolution](01-API-CONTRACT-FE-BE.md#resolutions); tên API/bảng mới dưới đây chỉ là đề xuất.

#### A. Đối chiếu logic hiện tại — tận dụng gì, thiếu gì

| Phần / căn cứ | Source thực tế | Đề xuất trước triển khai |
|---|---|---|
| Tạo đủ12buổi rồi Open — L§1/4 | `NormalClassCreationService.create` đã tạo lớp + generate12 + publish cùng transaction, khóa center/requestId; `NormalSchedulePolicy` kiểm12buổi tương lai/sân đúng môn | Tái dùng luồng nguyên khối. Giữ khóa tài nguyên, học phí dương, sức chứa và snapshot tiền; bổ sung metadata lifecycle, không viết lại generator |
| Chốt sĩ số48h/xác nhận24h — L§1/2 | `SportsClass`/DTO chưa có minCapacity, xác nhận khai giảng, số lần dời; enrollment chỉ kiểm Open + lịch tương lai. `findAll` đang đếm Registered+Pending thành một số | Bổ sung minCapacity theo ceil(2*max/3), khóa theo yêu cầu; tách paidRegisteredCount/reservedPendingCount/waitlistCount, deadlines từ buổi đầu thực tế. Không dùng occupancy làm sĩ số khai giảng |
| Sức chứa khi cập nhật — L§2 | Tạo lịch có kiểm room/capacity; `SportsClassService.update` kiểm chỗ đang giữ nhưng chưa đối chiếu maxCapacity mới với tất cả room của lịch hiện có | Bổ sung kiểm dưới khóa chung. Min của Advanced để NULL/không áp dụng; min Normal không được tự sửa để hợp thức hóa đổi max sau khi khóa. Đề xuất từ chối cập nhật max nếu làm sai giới hạn min hoặc sức chứa room, không tự giảm quyền/chỗ đã thu |
| Chặn chuyển sai trạng thái — L§4 | `SportsClassService.update/apply/normalizeClassStatus` nhận cả4trạng thái nhưng chưa có bảng chuyển trạng thái. Nhánh draft chỉ chặn Ongoing; lớp đã công bố có thể nhận Cancelled/Closed qua PUT | Cần chặn ở service trước khi mở demo lifecycle. Chuyển trạng thái qua hành động nghiệp vụ; PUT metadata không được bỏ qua guard hoặc tự tạo nghĩa vụ hoàn |
| Bắt đầu/kết thúc — L§1/4 | Chưa tìm thấy job đổi Class Open→Ongoing/Closed. Finalizer hoàn tất Session và enrollment; không đóng Class. `NotificationScheduleJob` nhắc buổi/quyền sắp hết hạn, không quản lý lifecycle Class | Bổ sung processor idempotent cho Class, tái dùng Clock/khóa/sự kiện sau commit. Khởi động lại không nhân xác nhận/thông báo. Không đóng vì endDate đơn thuần |
| Draft/Closed và bù — L§4/6 | Tạo tách bước đang dùng `Closed + pricing_draft=true`. `NormalMakeupService` kiểm Class `Completed`, trong khi enum/check Class không có Completed; không chặn Class Cancelled/Closed chung | Đề xuất vẫn nhận diện draft legacy bằng marker để tương thích; Closed kết thúc khác draft. Sửa nhánh Completed sai mô hình; bù trên Closed chỉ khi có incident xác minh buổi không tổ chức, mở lại nghĩa vụ học có audit. Không cho PUT tùy tiện mở lại |
| Attendance cuối ngày — L§1/4 + D15 | Finalizer tại end, correction Coach đến trước00:00, no-show sau cutoff đã có | Giữ cả hai mốc. Đề xuất “điểm danh đã chốt” là finalized nghiệp vụ tại end, không xóa quyền correction trong khoảng đệm kể cả Class Closed. Nếu Owner muốn chốt tuyệt đối sau00:00 thì cần đối chiếu lại D15 trước sửa |
| Dời khai giảng/chuyển lớp — L§3, R§2 | Có reschedule/substitute từng Session và session_changes; chưa có hồ sơ dời toàn khóa một lần, phản hồi từng Member hoặc chuyển enrollment có truy nguồn tiền | Thêm hồ sơ thay đổi toàn khóa + lựa chọn Member. Không dùng `memberAgreed=true` do staff gửi để giả đồng ý cả lớp. Ngày mới phải đủ12buổi, kiểm conflict và tính lại48h/24h; xác nhận Manager gắn phiên bản lịch |
| Sự cố khi Ongoing — L§3/6 | `CenterIncidentService`, substitute/reschedule và `NormalMakeupService` đã có reason/evidence/khóa, giữ buổi gốc và liên kết makeup | Tái dùng. Không gọi incident của một buổi để thay quy trình hủy cả lớp trước khai giảng; không tự áp hoàn một phần sau khai giảng |
| Cash tại quầy — L§5 | Member hiện được enroll với Cash →Pending; Receptionist gọi confirm-cash ở request khác. `EnrollmentService.promote` còn hardcode tạo Cash khi lên waitlist, chưa kiểm trạng thái lớp/48h trong nhánh này | Đề xuất action Receptionist đăng ký/thu Cash nguyên khối cho Normal, tái dùng createClassOrder/confirmCash và khóa. Member tự phục vụ dùng VNPay; thăng waitlist cũng phải bỏ Cash mặc định, kiểm lại cutoff/lịch/phương thức đã chọn. Không thay Cash Advanced; cần duyệt tác động FE ở E2 |
| VNPay/giữ chỗ — L§1/5, R§7.7 | Default5phút, có env override; expire giải phóng Pending, callback kiểm chữ ký/amount/reference, retry và Paid+reconciliationRequired nếu muộn/mất quyền. Chưa kiểm48h trong pay/activation/waitlist; activation còn chấp nhận Ongoing | Giữ cấu hình TTL, không giả deadline dài để demo. Đề xuất hạn thanh toán min(now+TTL,cutoff48h); cutoff áp cho enroll/promote/order/pay/cash/activation. Tiền tới muộn giữ Paid và đối soát, không hồi sinh chỗ. Không tự bật QueryDR; runtime env chưa được xác nhận ở lượt review này |
| Refund Normal — R§1–5 | `PaymentResolutionService.eligibility` chỉ cho Refund khi reconciliationRequired, tức tiền chưa cấp quyền; invoice Normal Paid/Registered thường bị Blocked. Chỉ staff tạo/list, Manager approve/complete | Không thể seed một Refund Completed rồi tuyên bố flow mới đã xong. Mở nhánh rõ `NORMAL_PRESTART` trong hồ sơ Refund; giữ nhánh reconciliation cũ và CashCorrection D16/Advanced nguyên trạng |
| Phân vai/chứng từ — R§3–6/8 | Resolution chưa có Member self-service/receiver/transfer attempt/private attachment/verification. Complete hiện nhận evidence text; log còn chép evidence. MediaService phục vụ avatar, trả secure_url, chưa là private PDF workflow | Bổ sung hành động Member, hàng đợi Receptionist đã Approved, nộp chứng từ→chờ Manager xác minh. Reuse cấu hình hạ tầng chỉ sau kiểm private storage; không lấy avatar URL công khai làm chứng từ. Log chỉ ID/action, không chứa tài khoản/file URL nhạy cảm |
| Báo cáo/tiền/không lặp — R§7/9 | ReportService đã tách thu/Refund Completed/ròng theo paymentDate/performedAt; resolution unique(invoice,type), mutex center và invoice lock | Tái dùng nền này; thêm approved-recipient version, transfer claim/idempotency, kiểm tổng tiền và verification. Báo cáo kỳ chuyển tiền chỉ tính sau xác minh thành công, không trừ Approved/ảnh upload. Invoice Paid/amount gốc giữ nguyên |

Nguồn source trọng tâm: [SportsClassService](../src/main/java/com/sportscenter/sportclass/SportsClassService.java), [NormalClassCreationService](../src/main/java/com/sportscenter/sportclass/NormalClassCreationService.java), [EnrollmentService](../src/main/java/com/sportscenter/enrollment/EnrollmentService.java), [InvoiceService](../src/main/java/com/sportscenter/invoice/InvoiceService.java), [AttendanceFinalizationProcessor](../src/main/java/com/sportscenter/training/AttendanceFinalizationProcessor.java), [NormalMakeupService](../src/main/java/com/sportscenter/session/NormalMakeupService.java), [CenterIncidentService](../src/main/java/com/sportscenter/session/CenterIncidentService.java), [PaymentResolutionService](../src/main/java/com/sportscenter/invoice/PaymentResolutionService.java), [ReportService](../src/main/java/com/sportscenter/report/ReportService.java). Test workflow hiện có đã mô tả makeup13record/12hợp lệ và refund tiền đến muộn; chưa phải test cho minCapacity/48h/24h hoặc quy trình chứng từ mới.

#### B. Account và Coach — đề xuất để duyệt

Giữ danh tính33account hiện có trong [reference seed](../database/bootstrap/scms_demo_reference.sql), gồm Admin1/Manager2/Receptionist2/Coach18/Member10. Admin center NULL; các tài khoản vận hành center1. Không thêm center mới, không mở lượt rà center khác. Không gán Admin quyền tài chính để demo; ma trận quyền mới chưa duyệt không được seed lách.

| Nhóm | Đề xuất | Mục đích / đối chiếu |
|---|---|---|
| Admin | Giữ `admin@gmail.com`, Active | Quản trị hệ thống; tài chính vẫn bị chặn theo baseline, không suy Admin là toàn quyền — R§3, D18 |
| Manager | Giữ `manager1@gmail.com`, `manager2@gmail.com`, Active | Tạo/xác nhận lớp, duyệt/xác minh hoàn. Demo Manager1 duyệt, Manager2 xác minh để dễ thấy vai trò; không tự đặt rule bắt buộc hai Manager khác nhau — L§1, R§3–5 |
| Receptionist | Giữ `receptionist1@gmail.com`, `receptionist2@gmail.com`, Active | Thu Cash; nhận khoản Approved, claim công việc và nộp chứng từ; thử hai người nhận cùng việc nhưng chỉ một claim — L§5, R§3/7 |
| Member chính | Giữ10Member hiện có + thêm10Member demo =20Member Active | Lớp Bóng đá15chỗ, còn Member để waitlist/race; tách A/B để kiểm owner — L§2, R§9.15/17 |
| Member kiểm trạng thái | Thêm3 riêng: Inactive, Locked, Pending | Không làm hỏng các account trình diễn chính; Pending là OTP chưa verify, không phải quyền học hợp lệ. Không seed OTP/secret thật |
| Coach chính/thay thế | Giữ18Coach Active,2người/môn | A là phân công chính, B cùng môn chưa được giao/bổ sung khi có substitute; cover assignment/RBAC/incident — L§3, D03/D15 |
| Coach kiểm trạng thái | Thêm1Coach Inactive, chuyên môn Cầu lông | Chặn dùng Coach inactive, không cần đổi trạng thái một Coach đang có lịch Paid |
| **Tổng được Owner đồng ý về bộ dữ liệu10/11** | **47account:1Admin +2Manager +2Receptionist +19Coach +23Member** | 43account chính +4account kiểm trạng thái; giữ nguyên33account PDF, bổ sung14account. Chưa sửa SQL |

10Member giữ lại: M01 An (`an@gmail.com`), M02 Linh, M03 Huy, M04 Mai, M05 Khang, M06 Vy, M07 Bảo, M08 Nhi, M09 Kiệt, M10 Hà; email hiện có giữ nguyên. Đề xuất M11–M20: `member11@scms.demo.invalid`…`member20@scms.demo.invalid`, tên hiển thị “Member Demo11”…”Member Demo20”; account kiểm dùng `member.inactive`, `member.locked`, `member.pending`, `coach.inactive` cùng miền `.invalid`. Mã Mxx dùng để review và manifest, FE vẫn tra ID thực từ API.

| Môn / prefix email `coach.<prefix>{1,2}@gmail.com` | Coach A giữ nguyên | Coach B giữ nguyên | Vai trò trong bộ demo |
|---|---|---|---|
| Tennis / tennis | Nguyễn Hoàng Quân | Huỳnh Diễm My | Advanced chính/thay, Normal đủ sĩ số |
| Bơi / swimming | Lê Minh Duy | Lê Phương Anh | Normal thiếu sĩ số |
| Pickleball / pickleball | Võ Thanh Long | Phạm Thị Lan | Phương án dời/chuyển và phản hồi Member |
| Bắn cung / archery | Lê Nguyễn Thiên Phúc | Hồ Thảo My | Đã dời khai giảng1lần |
| Bóng rổ / basketball | Trần Anh Dũng | Phan Kim Ngân | Hủy trước khai giảng, nghĩa vụ hoàn chủ động |
| Bóng đá / football | Lê Trần Phi | Trần Ngọc Yến | Lớp15chỗ, last-seat/waitlist |
| Bóng chuyền / volleyball | Nguyễn Văn Hoàng | Phạm Thanh Thư | Ongoing, dời buổi/thay Coach |
| Cầu lông / badminton | Võ Minh Đức | Trần Thị Hương | Thu Cash, attendance, incident/makeup và Refund |
| Bóng bàn / tabletennis | Lê Minh Khoa | Hồ Bích Oanh | Lớp2chỗ, hoàn tất khóa |

Giữ một chuyên môn hợp lệ/người theo môn, không gán mọi Coach tất cả môn. Lịch rảnh chính T2–T7,7ca/ngày; Chủ nhật làm khoảng trống để demo đóng nhận/đổi lịch rảnh. Outdoor vẫn khóa ca3/4 theo D12 dù weeklySlots có slot đó; lịch lớp Normal và Advanced phải xếp chung để không trùng Coach/phòng/Member. Có thêm một ngày nghỉ Coach B trong tương lai, không để nó đè lịch có sẵn. Đây là phần bộ dữ liệu được Owner đồng ý chung10/11, chưa seed; không đổi danh tính/tài khoản PDF.

#### C. Lớp và lịch — đề xuất21Class, chưa seed

Giữ9template Advanced (1môn/template, capacity1,12credit cấu hình), **thêm12lớp Normal có mục đích cụ thể**. Mỗi Normal có12buổi hợp lệ; N10 có13record do giữ buổi gốc invalidated +1makeup. 20room/9môn/18giá đã duyệt giữ nguyên. Max/min theo **lớp**, không tự tăng phòng. N01–N12 là mã fixture/manifest, không dùng thay classId trong API.

| Mã / môn | Trạng thái, dữ liệu chính đề xuất | max/min | Đối chiếu và mục đích |
|---|---|---:|---|
| N01 Cầu lông | Open, buổi đầu D+7,1Paid; chỗ trống để demo enroll/thu quầy/VNPay | 4/3 | L§1/2/5; tạo Pending mới ngay lúc demo, không seed hold kéo dài |
| N02 Tennis | Open,3Paid, đã chốt48h, chưa xác nhận; buổi đầu còn30–47giờ | 4/3 | L§1; Manager xác nhận trước24h. Chọn khung chuẩn tương lai trong khoảng này, không đổi Clock live |
| N03 Bơi | Open,6Paid, đã chốt48h, còn30–47giờ, thiếu1người | 10/7 | L§1–3; Pending không cứu sĩ số, cảnh báo/dời lần đầu |
| N04 Bắn cung | Open,3Paid, đã dời1lần có phản hồi, ngày mới D+7 | 5/4 | L§3; có thể nhận bổ sung trước cutoff mới; nếu lại thiếu khi chốt thì không dời lần2 |
| N05 Pickleball | Open,3Paid, đề nghị phương án mới;1đồng ý/1yêu cầu hoàn/1chưa phản hồi | 4/3 | L§3, R§2A/9.01–05; chưa phản hồi không là đồng ý; tính lại sĩ số sau lựa chọn |
| N06 Bóng rổ | Cancelled trước khai giảng,3Paid; thêm1Pending chưa thu được cancel | 8/6 | R§2B/9.04/06–08;3nghĩa vụ hoàn chủ động, Pending không có chuyển tiền hoàn |
| N07 Bóng đá | Open,14Paid,1chỗ còn lại; đủ Member khác để race/waitlist | 15/10 | L§2/4; không seed16Registered để giả case vượt sĩ số |
| N08 Bóng chuyền | Ongoing đã được Manager xác nhận,6Paid,4Completed+8Scheduled | 8/6 | L§1/3; cấm hủy thường, thay Coach/dời buổi chưa bắt đầu |
| N09 Bóng bàn | Closed hoàn tất,2Paid,12buổi hợp lệ đã finalized | 2/2 | L§1/4; history/đánh giá/report; không mở lại qua PUT |
| N10 Cầu lông | Ongoing,3Paid;8buổi hợp lệ hoàn tất,1gốc invalidated và4buổi còn lại gồm makeup | 4/3 | L§3/6, D15;13record nhưng12buổi hợp lệ, bù chỉ1lần và giữ lịch sử |
| N11 Pickleball | Open D+7,3Paid/4chỗ, cùng giá1.800.000đ/center1 với N05, Coach B/sân Pickleball02/lịch khác; có lịch sử Expired/Failed/Cancelled chưa thu | 4/3 | Owner duyệt10/11 làm đích N05, một chỗ cho chuyển thành công rồi hết chỗ; giữ case L§1/5 retry/expire/thanh toán muộn, không tăng tổng Class |
| N12 Cầu lông | Cancelled trước khai giảng,3Paid; hoàn ở các bước chứng từ/Completed/sự cố | 4/3 | R§4–9; nối kỳ thu trước/kỳ hoàn sau, không trừ Approved như đã hoàn |

Các ngưỡng mặc định2→2,4→3,5→4,8→6,10→7,15→10 đều có trong bảng. Ví dụ Manager tăng ngưỡng7→8 và lớp8trongphòng10→min6 demo bằng tạo lớp mới. N05 chuyển sang N11 Pickleball đã được Owner duyệt10/11, cùng môn/giá/center, không tạo thêm lớp ngoài bộ21Class; không đổi hóa đơn gốc để ép khớp lớp đích.

**Trục thời gian:** D là ngày dựng fixture theo Clock VN, lưu anchor cụ thể trong manifest. Ongoing/Closed có lịch sử tương đối trước D, Open có lịch tương lai; lịch final phải được giải conflict đầy đủ trước xuất. Tại thời điểm dựng dữ liệu, Paid/consent/khai giảng/incident/attendance phải có thứ tự thời gian hợp lệ, không đặt timestamp phê duyệt sau khi đã bắt đầu. N02/N03/attendance trong ngày là kịch bản có thời hạn; jobs chạy thật nên có thể đổi state sau khi demo. Pending/QR chỉ tạo tại bước trình diễn, không kéo TTL hoặc fake ngày check-in để bộ dữ liệu giữ nguyên mãi.

#### D. Dữ liệu nghiệp vụ và DB đề xuất theo nhóm

| Nhóm / bảng hiện có | Nội dung demo đề xuất | Dữ liệu/logic cần bổ sung, căn cứ |
|---|---|---|
| `classes`, `normal_class_requests`, `sessions` | A01–A09 Advanced templates + N01–N12; requestId nhất quán, lịch không trùng | Thêm min_capacity, confirmation actor/time + schedule version, postponed_count0..1; deadline server tính từ buổi đầu, không tin client. Không thêm enum Completed cho Class — L§1–4 |
| `users`, subtype, `coach_specializations`, schedules/slots/days_off | Bộ B;2Coach/môn,1Inactive riêng | Giữ quyền mặc định, không đổi role để chữa403; quyền Coach theo assignment, không chỉ cùng chuyên môn — L§3,R§3,D15 |
| `invoices`, `class_enrollments`, `advanced_registrations`, `member_packages` | Normal Paid/Registered/Pending/Waitlisted/Expired/Failed/Cancelled; Advanced Requested/Accepted/Rejected, Pending0credit, Active còn12/cònít/0nhưngcònbuổi, Completed, Expired;1case CashCorrection thật sự “chưa nhận tiền” | Chứng từ demo được gắn rõ DEMO trong manifest/mã tham chiếu; không tạo receipt/provider ref giả trông như gateway thật. CashCorrection giữ D16; Paid/amount/purchase_amount không chỉnh theo seed mới — L§1/5,R§1/7,D03/D10/D16 |
| `session_changes`, `center_incidents`, `incident_affected_members`, `subscription_pauses` | Dời buổi/thay Coach/makeup; Advanced có hủy>=12h, báo vắng sát giờ và một pause hợp lệ có nguồn | Không tạo incident để chữa dữ liệu seed sai; không chia sẻ incident Normal sang Advanced. Không mở báo vắng/hủy cá nhân Normal — L§3, D05–D08/D12/D15 |
| `attendances`, `session_absence_notices` | Present/Absent, note có/rỗng; notice gốc và bản superseded; case sau end trước cutoff và quá cutoff | Không seed Late mới. Buổi hôm nay chọn theo thời điểm chạy; kiểm mọi biên bằng Clock local, không sửa Clock Render. Không giả check-in bằng giờ correction — L§1/4, D15 |
| `training_plans`, `training_results`, `evaluations`, `class_feedbacks` | Plan lớp Normal, plan cá nhân Advanced; kết quả/đánh giá của buổi có chuyên môn; một Member có hai đăng ký | Coach A viết đúng assignment; Member A chỉ đọc phần mình, B không thấy. Finalizer Absent không tự sinh nội dung chuyên môn — L§1, D15/RP-36 |
| `normal_class_events` **đề xuất mới** | Đề xuất dời/chuyển, xác nhận khai giảng/hủy, snapshot lịch cũ/mới, reason/actor/version | Thay đổi toàn khóa khác session_changes từng buổi; số lần dời và xác nhận gắn version lịch. Sinh12buổi nguyên khối, không hủy lịch gốc để giấu sự cố — L§1/3/4 |
| `normal_member_responses` **đề xuất mới** | Accept/RefundRequested/WaitingResponse, người phản hồi/kênh/xác nhận phương án cụ thể | Unique(event,member), revision/retry; không phản hồi vẫn chờ. Nhân viên tiếp nhận thay phải lưu source/evidence, không tự giả lựa chọn — L§3, R§2/3 |
| `normal_enrollment_transfers` **đề xuất mới** | Enrollment gốc/đích, khoản Paid gốc, consent và lịch sử bàn giao quyền | Không sửa lớp/amount trên invoice gốc; read/report/eligibility cần đọc link chuyển quyền. Không thu hai lần/cấp hai quyền; chênh giá chờ E — L§3, R§1/2/7 |
| `payment_resolutions` **mở rộng theo scope Normal** | Refund từ N05/N06/N12: thiếu tài khoản nhận, chờ duyệt, Approved, chờ xác minh, Completed, sự cố | Thêm scope/origin/class-event/enrollment; số tiền theo từng bước, recipient snapshot/version, người duyệt/xác minh, hạn7ngày làm việc. State hiện tại không đủ; mở nhánh Normal riêng, không đổi CashCorrection/Refund legacy — R§2–7 |
| `refund_transfer_attempts` **đề xuất mới** | Receptionist1 claim; Receptionist2 không nhận trùng; success/submitted/failed/unknown | Request key, approved version, bank reference, amount/time/actor. Unknown/timeout phải đối soát trước lần chuyển mới; DB không ngăn được chuyển ngoài ngân hàng sai quy trình — R§4/7 |
| `refund_documents` **đề xuất mới** | Ảnh/PDF mẫu có chữ DEMO, gắn đúng attempt; sửa bằng revision có lý do | Private object key, uploader/time/type/size/checksum, không public URL; kiểm role/owner/center. Đề xuất JPEG/PNG/PDF≤5MB/file; chưa chọn provider trước khi kiểm private delivery — R§6.2 |
| `refund_verifications` **đề xuất mới** | Manager so khớp thành công/không khớp/hoàn ngược, nguồn đối soát và thời điểm | Không lấy ảnh Receptionist làm nguồn độc lập. Chỉ verified success mới Completed; thay người nhận/amount sau Approve làm mất hiệu lực bản duyệt cũ — R§4–7 |
| `notifications`, recipients, `system_logs`, chat tables | Nhắc lớp, phản hồi, chờ tài khoản/Approved/chờ xác minh/Completed; chat Member–Receptionist đã đọc/chưa đọc; audit mới có provenance | Bỏ audit cũ nhưng sinh audit demo mới đúng actor/source, chống lặp; thông tin ngân hàng không vào notification/log. Authority phụ không mở dữ liệu người khác — R§6.3/8, D15 |
| `holidays`, `coach_days_off`, `support_requests`, `ai_consultations` | Một ngày nghỉ TT tương lai không đè lịch; một ngày nghỉ Coach; hỗ trợ khiếu nại qua chat hiện có | Không tự triển khai ticket/AI/Google/avatar để lấp DB; bảng chưa dùng để trống có lý do. QueryDR không bật lại chỉ vì có pay-attempt — R§8/9.20, D12/D14 |

**Các mốc Refund phải demo rõ:** chờ phản hồi ở change response, chờ tài khoản, chờ duyệt, Approved, đang chuyển, AwaitingVerification, Completed, NeedInformation/Failed/Unknown. Rejected chỉ dành cho hồ sơ sai có lý do, không xóa nghĩa vụ trung tâm. Hủy N06 tự tạo3case; chưa có tài khoản nhận vẫn còn case. Pending chưa thu không có khoản chuyển hoàn. N12 có một Completed kèm transaction/verification giả lập đánh dấu DEMO và một case chưa xác minh không tính hoàn. 20case R§9 sẽ có matrix “fixture/thao tác/expected”; không cần20bản copy dữ liệu lỗi. Hoàn ngược sau Completed phải ghi sự cố/xác minh mới và điều chỉnh kết quả báo cáo theo dòng tiền thực, không xóa chứng từ cũ hoặc tự chuyển lần2.

**Mapping đủ20case Refund — đề xuất expected, chưa chạy nghiệm thu:**

| Case R§9 | Fixture / bước trình diễn | Expected cần kiểm |
|---|---|---|
| 01 | N05, Member đồng ý lịch mới | Không tạo Refund cho cùng quyền |
| 02 | N05, Member từ chối dời và chọn Refund | Chờ Manager, đúng Invoice/Member |
| 03 | N05, từ chối lớp đích tạo trong bước chuyển | Được yêu cầu hoàn100% đúng phạm vi |
| 04 | N06, action hủy lớp có3Paid | Tự tạo3nghĩa vụ; retry không nhân case |
| 05 | N05, Member chưa trả lời | WaitingResponse, không tự đồng ý hoặc mất quyền |
| 06 | N06, case thiếu tài khoản nhận | Không chuyển/Completed; còn công việc phải xử lý |
| 07 | N06, Pending chưa thu | Hủy giữ chỗ, không chuyển hoàn |
| 08 | N12, hóa đơn Cầu lông900.000đ | Nghĩa vụ đủ900.000đ, không trừ phí |
| 09 | N06, Receptionist thử chuyển case chưa Approved | Bị chặn; không phát sinh attempt hợp lệ |
| 10 | N06, sửa amount/receiver sau Approve | Không dùng bản duyệt cũ, Manager duyệt lại |
| 11 | N12, Receptionist upload mẫu DEMO | AwaitingVerification, chưa Completed |
| 12 | N12, mới có ảnh và chưa kiểm nguồn ngân hàng độc lập | Manager không được Complete thiếu bước xác minh |
| 13 | N12, Manager xác minh khớp giao dịch thành công | Completed; đúng báo cáo/thông báo/audit sau commit |
| 14 | N12, attempt failed/pending/reversed | Không báo hoàn thành; giữ hồ sơ, không tự chuyển tiếp |
| 15 | N06, request trùng/hai Receptionist claim | Một case/claim đang hiệu lực, không hai lần chuyển |
| 16 | N12 Completed, gửi lại thao tác | Không hoàn lần2; lịch sử còn đọc được |
| 17 | Member A/B, Coach, Admin mặc định; guard center hiện có | Chỉ đúng người/role/center đọc chứng từ; giữ test center cũ, không mở rà center mới |
| 18 | N12, thu ở tháng trước, chuyển/xác minh tháng này | Phân biệt kỳ thu/kỳ hoàn, Approved không trừ tiền |
| 19 | N11, callback ký hợp lệ sau expiry/cutoff/hủy | Paid cần đối soát; không mất tiền hoặc cấp lại chỗ |
| 20 | N12 Completed, Member chat chưa nhận tiền | Truy nguồn receiver/attempt/verification, không kết luận chỉ dựa ảnh |

**RBAC và biên thời gian của bộ demo:** Member A đọc/lựa chọn/cung cấp tài khoản của mình, không có approve/upload thay Receptionist/complete; B bị chặn dữ liệu A. Receptionist chỉ nhận case Normal đã Approved/đủ dữ liệu và thao tác attempt/chứng từ, không tự duyệt hoặc complete. Manager quản lý cùng center, có action riêng approve/verify; Coach không đọc tài chính và chỉ ghi chuyên môn đúng phân công. Admin giữ ma trận cũ, không cấp thêm MANAGE_INVOICES để làm demo pass. Các test center đã có được giữ lại. Kiểm request sai, sửa vai trò/ID, retry và race qua API, không seed dữ liệu vi phạm.

Biên bắt buộc trước triển khai fixture cuối: T−48h−1ms/T−48h/T−48h+1ms; T−24h; T buổi đầu; minCapacity thấp hơn mặc định/bằng mặc định/bằng max/vượt max; Pending không tính khai giảng và khóa sửa min dù chưa Paid; cutoff end/23:59:59.999/00:00; callback tới trước/sau chốt với giữ chỗ đã hết; finalize/makeup/Refund retry đồng thời. Đây là danh sách kiểm cho phần sẽ triển khai, không là kết quả PASS của lượt review.

**Cách tạo bộ nền sau duyệt:** schema baseline → fixture demo riêng có anchor/requestKeys/manifest → kiểm SQL/JPA/API sau commit → mới xuất SYSTEM-DATA. Dữ liệu demo được phân biệt với bản ghi gateway/ngân hàng thật; case quyền, tài chính và chống lặp vẫn phải kiểm qua API local. Fixture không gọi mail/VNPay/ngân hàng hoặc đổi toggle chế tài. Case sai thực hiện bằng request bị từ chối, không seed malformed rows để phá constraint. Chưa viết fixture theo state/API chưa triển khai.

**SYSTEM-DATA.md sau duyệt:** một mục cho mỗi bảng hiện hành sau khi chốt migration, gồm mục đích, row demo/ID/FK/trạng thái/thời gian, case liên quan và trường cố ý NULL/rỗng. Có đầy đủ account/lớp/Coach và mapping mã fixture→ID thực để FE đối chiếu, không hardcode ID. Giữ nguyên tắc mã invoice không tái dùng; không xuất password_hash, OTP/QR secret, cookie/token, credential hoặc số tài khoản thật. Chứng từ chỉ là mẫu giả lập. Ngày/ID và nội dung file sinh từ cùng manifest với script để tránh hai bản lệch nhau; chưa tạo file trước duyệt.

#### E. Các lựa chọn — đã được Owner chốt10/11, chưa triển khai Normal/Refund

| Điểm | Ví dụ và đề xuất cụ thể |
|---|---|
| E1 — account/lớp/Coach | Owner đồng ý bộ dữ liệu chung10/11:47account,21Class,18Coach Active+1Inactive, lịch rảnh T2–T7. Bộ33account PDF giữ nguyên danh tính/hồ sơ/email/role và mật khẩu demo đã duyệt; chỉ thêm14account. ID liên kết sẽ thống nhất trong manifest khi reset, FE không dựa ID lịch sử. Các mốc48h/24h/2/3/dời1lần đã chốt, không hỏi lại |
| E2 — Cash Normal tại quầy | **Owner duyệt10/11.** Member tự đăng ký Normal online dùng VNPay; Cash do Receptionist đăng ký + thu tiền nguyên khối. Giữ xử lý Pending cũ và Advanced. Cash tại quầy khi lớp đầy chỉ vào waitlist, chưa thu; khi lên chỗ không tự tạo Cash online nữa, kiểm lại cutoff/sĩ số/lịch và phương thức Member chọn. Chưa triển khai phần này trong lượt chỉ sửa GET RBAC |
| E3 — thiếu xác nhận ởT−24h | **Owner duyệt phân tích/đề xuất10/11.** 24h là xác nhận khai giảng, khác Open nhận đăng ký;48h chốt đăng ký giữ nguyên. Lỡ24h thì cảnh báo, không tự khai giảng hoặc tự hủy; Manager được xác nhận trễ có reason, đủ mọi điều kiện và trước T. Đến T vẫn chưa có xác nhận thì chặn Ongoing, xử lý dời/hủy đúng policy. Lưu actor/time/lý do trễ và phiên bản lịch, không mở lại đăng ký để cứu sĩ số |
| E4 — chuyển lớp/chênh giá | **Owner duyệt10/11:** cùng môn/giá/center, Member đồng ý rõ, lớp đích còn hạn/chỗ, chưa bắt đầu, đủ12buổi hợp lệ và không trùng lịch. Preview rồi recheck dưới khóa khi xác nhận, giữ hóa đơn gốc/link chuyển quyền; không phù hợp thì phương án hợp lệ khác hoặc hoàn đúng phạm vi. N11 Pickleball làm đích N05, tổng21Class giữ nguyên. Không mở chuyển khác giá/center hoặc sau cutoff |
| E5 — xác minh người nhận | **Owner duyệt10/11:** BE chuẩn bị contact phone từ hồ sơ Member đúng quyền; FE quyết định dùng/hiển thị. Chỉ liên hệ khi bất thường và lưu kết quả; không thêm điện thoại ngân hàng/OTP/KYC, không chép phone vào chứng từ/log chung. Contact không thay xác minh người nhận hay đối soát giao dịch. Đổi receiver/amount sauApprove phải duyệt lại |
| E6 — ngày làm việc/SLA | **Owner duyệt10/11:**7ngày TT mở theo lịch nghỉ, tính T7/CN nếu không đóng; không áp T2–T6 hoặc đổi7ngày lịch. ĐủApproved+receiver hợp lệ mới tính; ngày TT mở tiếp theo là ngày1, hết ngày7 giờVN là hạn chuyển. Thiếu thông tin vẫn giữ nghĩa vụ; quá hạn cảnh báo, không đóng case. Không dùng ngày nghỉ Coach làm ngày TT đóng |

**Làm rõ E4 — kiểm tương thích và fallback:** chỉ đề xuất Normal cùng môn/cùng center, học phí đích khớp khoản học phí đã Paid của quyền gốc (không tin giá client, không dùng giá Subject mới để đổi chứng từ). Lớp đích Open, còn hạn nhận đăng ký48h, chưa bắt đầu, đủ12buổi hợp lệ tương lai; Coach Active/đủ chuyên môn, room đúng môn/Available/vừa maxCapacity, không vướng ngày nghỉ và lịch Member. Tính chỗ gồm Registered, Pending chưa hết hạn và chỗ chuyển đang được giữ hợp lệ; không lấy paidCount khai giảng làm occupancy. Chưa yêu cầu mọi lớp đích đã xác nhận khai giảng vì xác nhận có thể nằm sau cutoff nhận đăng ký; trả trạng thái/sĩ số để Member biết, không hứa chắc lớp sẽ mở chỉ từ preview.

Preview không giữ chỗ. Khi Member đồng ý, transaction khóa đúng thứ tự và kiểm lại lịch/sĩ số/giá/quyền gốc/trạng thái hoàn tiền; giữ Invoice gốc, link chuyển quyền và không để vừa học lớp cũ/đích vừa hoàn cùng khoản. Nếu đã hết chỗ hoặc đổi lịch thì báo lựa chọn không còn hợp lệ, không tự thay bằng lớp khác. Nếu không có lớp phù hợp: trả danh sách rỗng và lý do; Manager có thể lập lớp mới hợp lệ hoặc dời khai giảng nếu còn đúng phạm vi một lần/consent. Nếu TT không cung cấp được trước khai giảng, Member được chọn hoàn100%; TT hủy hẳn thì chủ động tạo nghĩa vụ. Không phản hồi vẫn chờ, thiếu tài khoản nhận vẫn còn nghĩa vụ. Khác môn/giá/center và chuyển sau cutoff không tự mở ngoại lệ.

**Làm rõ E5 — flow gốc và bước liên hệ đề xuất:** flow trong R§4 là Member cung cấp/xác nhận tài khoản → Manager duyệt → Receptionist chuyển ngoài SCMS/nộp chứng từ → Manager đối soát độc lập → Completed. File yêu cầu xác minh trường hợp khác tên/đổi bất thường, nhưng không bắt gọi điện và không có flow điện thoại. Bổ sung tối giản đề xuất chỉ chen **trước Approve**: khi thấy khác tên/nghi vấn, Manager dùng `users.phone` đã có để liên hệ làm rõ lựa chọn của Member, ghi kết quả rồi mới duyệt; nếu chưa rõ thì NeedInformation, không mất quyền hoàn. Không thêm số điện thoại ngân hàng, API gọi điện hoặc OTP mới; phone hồ sơ hiện chưa được xác thực bằng OTP, cuộc gọi không tự chứng minh tài khoản ngân hàng thuộc ai. Trường hợp bình thường đi thẳng qua bước duyệt; receiver thay sau Approve phải duyệt lại trước chuyển. Chưa có số liên hệ thì yêu cầu bổ sung/kênh xác nhận phù hợp, không tự từ chối nghĩa vụ.

**Lịch sử thảo luận E6 — định nghĩa ở bảng trên đã thay thế phương án này:** T2–T6 là giả định lịch xử lý hành chính do BE đề xuất để định nghĩa “ngày làm việc”, không phải giới hạn chuyển khoản hay yêu cầu riêng từ file. Với project demo có thể bỏ lịch cuối tuần/ngày nghỉ bằng cách **đổi chính sách thành7ngày lịch liên tục** từ khi đã duyệt và đủ thông tin hợp lệ, ví dụ10/11 10:00→17/11 10:00 giờ VN. Không cần calendar mới/job nghỉ/SLA theo bank; chỉ dueAt, cảnh báo quá hạn và lý do xử lý. Tuy nhiên file R§7.10 đang chốt7ngày làm việc, nên phương án7ngày lịch là thay đổi cần Owner duyệt trước sửa, chưa tự đổi chữ trong file. Giữ Complete phụ thuộc xác minh thực tế, không vào thời hạn đơn thuần.

#### F. Log theo trung tâm — giải thích và đề xuất10/11

| Phần | Hiện trạng đã đọc ở source/contract | Đề xuất |
|---|---|---|
| Lưu center | `SystemLog` có category/centerId/scopeSource; `AuditProvenance` lấy target tin cậy ngay lúc ghi, không dùng center hiện tại của người thao tác để gán lịch sử | Tái sử dụng, mở mapping cho hồ sơ lifecycle/refund mới theo center của lớp/hồ sơ. Audit mới của fixture phải có nguồn, không đưa số tài khoản/chứng từ nhạy cảm vào detail |
| Lọc quyền BE | `AuditQueryService` ép Manager BUSINESS+own-center, Admin SYSTEM_SECURITY; controller role AND VIEW_AUDIT_LOG. Không có center filter cho client | Manager không cần chọn center: BE tự khóa scope. Giữ guard hiện có, không tự mở Admin đọc toàn bộ log BUSINESS |
| FE hiển thị | Contract trả mảng, centerId/category/scopeSource và header phân trang; chưa kiểm FE hiện hành đã đọc field/build đúng | Đề xuất nhãn trung tâm đang xem, thời gian/người/hành động/đối tượng/nội dung ngắn; center từ response/context tin cậy. Bố trí do FE quyết định. Đổi filter không được mở scope |
| Cũ/không rõ nguồn | Migration18 thêm cột, không tự biết center của log cũ; log thiếu provenance không trả qua API scope này | Owner đã đồng ý bỏ audit demo cũ; tạo audit mới đúng nguồn trong fixture. Không backfill theo actor hôm nay để làm log “có center” |
| Trạng thái phát hành | Nhóm scope log Phase1 đã có kiểm local; vẫn trong working tree chưa phát hành. Owner từng báo18 thành công, chưa chứng minh BE/FE mới đang chạy | Kiểm sau deploy bằng Manager đăng nhập/auth-me → GET audit-logs → đối chiếu center trong response và DB snapshot. Chưa gọi việc đọc source là nghiệm thu FE/live |

Nguồn: [AuditQueryService](../src/main/java/com/sportscenter/audit/AuditQueryService.java), [AuditProvenance](../src/main/java/com/sportscenter/audit/AuditProvenance.java), [contract RP-43](01-API-CONTRACT-FE-BE.md#audit-scope-20261109). Đây là tái sử dụng scope đã triển khai; đề nghị mở Admin chọn center để đọc log nghiệp vụ toàn hệ thống, nếu có sau này, là quyết định quyền riêng.

#### G. Kiểm lại DB demo, nguồn thiết kế và ngày làm việc —10/11

**Dữ liệu thực tế:** đã đọc lại Supabase bằng transaction READ ONLY (không seed/reset/sửa lịch). Lớp19 Cầu lông Open,4Paid/Registered,900.000đ,center1, buổi đầu20/11; lớp21 Cầu lông Open cùng giá/center, buổi đầu23/11,12buổi Scheduled tương lai,2Paid/Registered trên max4. Sàng lọc bổ sung không thấy room/Coach/chuyên môn/ngày nghỉ hoặc overlap tài nguyên của lịch21 bị lỗi;2Member nguồn chưa ở21 và không thấy xung đột lịch khác,2Member nguồn đã có ghi danh21 nên không chuyển chồng. Có dữ liệu ứng viên nhưng không thể coi4người đều được chuyển vào2chỗ. Đây là quan sát tại thời điểm query, chưa có consent/transaction chuyển quyền hoặc API mới; cột activity19 cũng chưa có trên DB này, kiểm môn sân dựa snapshot bất biến migration13. Số liệu/mốc/query và giới hạn ở `normalDemoReadiness20261110` trong QA, không là nghiệm thu flow live.

**Phân biệt dữ liệu:** bản `database/schema_postgres.sql` đã kiểm trước đây chỉ seed9template Advanced, không có Normal để demo chuyển lớp sau reset. Bộ21Class đang duyệt cũng mới là kế hoạch, chưa được seed. Trong kế hoạch, N05 Pickleball chưa có lớp đích Pickleball seed sẵn vì trước đó dự kiến tạo đích ở bước demo. Đề xuất điều chỉnh dữ liệu **N11 từ Cầu lông sang Pickleball, giá1.800.000đ** như N05, cùng center1, sân Pickleball02/Coach B/lịch khác và3Paid trên4chỗ; giữ luôn case Pending hết hạn/thanh toán muộn ở N11. Như vậy có sẵn cặp N05→N11, một chỗ để demo chuyển thành công rồi hết chỗ/fallback, không tăng số account/Class. Điều chỉnh fixture đã được Owner duyệt sau cùng10/11 và cập nhật bảng C; script reset chưa được đổi. Ví dụ Refund900.000đ vẫn dùng N12 Cầu lông. Không lấy cặp lớp rehearsal cũ làm bộ dữ liệu chuẩn cuối cùng.

**Nguồn điều kiện tương thích:** L§3 yêu cầu chuyển lớp có Member đồng ý và giữ quyền lợi; R§1/2 yêu cầu phương án thay thế hoặc hoàn khi TT không tổ chức được. Cùng giá/cùng center được Owner duyệt10/11; cùng môn/đủ12buổi/tài nguyên/lịch/sĩ số là đề xuất kỹ thuật dựa guard hiện có của `NormalSchedulePolicy`, `SessionService`, `CoachCapacityService` và enrollment/payment, không lấy từ một đặc tả “lớp tương thích” có sẵn hoặc quy định bên ngoài. File chưa liệt kê đầy đủ bộ lọc nên phải ghi rõ đâu là yêu cầu/đâu là đề xuất. Dời ngày khai giảng giữ Class khác với chuyển19→21; dời giữ lớp phải đối chiếu toàn bộ12buổi mới và consent chứ không tìm lớp đích.

**Điện thoại không phải file đính kèm:** Response resolution hiện chưa có contactPhone hay quy trình gọi điện. Thiết kế đề xuất hiển thị số liên hệ từ `users.phone` trong chi tiết Member/hồ sơ cho Manager có quyền; chỉ case bất thường mới liên hệ trước duyệt, ghi actor/time/kênh/kết quả gắn phiên bản người nhận trong lịch sử hồ sơ. Không yêu cầu đưa số vào ảnh/PDF ngân hàng, `evidence` text hoặc log chung. Khi đối soát sau chuyển, Manager kiểm giao dịch/số tiền/tài khoản/nguồn ngân hàng độc lập; cuộc gọi không thay chứng từ hay chứng minh tiền đã đến. Không thêm API điện thoại/OTP hoặc tự coi phone hiện có đã được xác minh.

**Kết quả rà ngày làm việc:** chưa có định nghĩa lịch hành chính/SLA Refund riêng trong tài liệu quyết định/source/schema. `centers` hiện chỉ có id/name; không có working_weekdays/opening calendar. Source nhận daysOfWeek1..7 và weeklySlots MONDAY..SUNDAY; DB có slot Coach cả7ngày, buổi thực tế T2–T7, không suy Chủ nhật đóng chỉ vì chưa có buổi. Có `holidays`/ngày nghỉ Coach; D12 và `EntitlementTimePolicy` tính thời gian hoạt động sau khi trừ closure, không mặc định loại T7/CN. `Member.pdf`/`analysis 2410.pdf` không tìm thấy quy định lịch tuần chung qua phần text liên quan; ví dụ thứ Hai trong bản analysis là minh họa, không định nghĩa lịch trung tâm.

Vì vậy T2–T6 từng nêu **không phải rule có sẵn**. Sau đối chiếu, đề xuất ưu tiên đơn giản **ngày làm việc Refund = ngày TT mở theo lịch nghỉ đã cấu hình; tính cả T7/CN nếu không đóng**. Tái dùng dữ liệu Holiday, không dùng ngày nghỉ cá nhân Coach để dừng SLA, không cần lịch ngân hàng/bảng tuần riêng. Owner đã duyệt cách hiểu này ở lượt chốt sau cùng10/11; file R đã được đồng bộ, không đổi thành7ngày lịch. Đề xuất7ngày lịch trước đó là phương án thảo luận, chưa được duyệt hoặc triển khai. Lịch Holiday hiện global nên không tự suy đã có calendar riêng cho từng center.

**Trình tự đề xuất sau review:** (1) Owner sửa/duyệt B–E và các diễn giải A/D; (2) session triển khai riêng phần lifecycle/tiền/chứng từ đã thống nhất, có migration/test và giữ Advanced; (3) sinh fixture demo đã kiểm, xuất SYSTEM-DATA, đối chiếu FE; (4) chốt schema/deploy/FE smoke theo phạm vi Owner duyệt. Các thiết kế khác, mục5/R1/R2/validation tên sân để session mới. Không chạy reset còn thiếu bộ demo trước khi chốt dữ liệu này.

**Yêu cầu đã thực hiện:** review06/11 tại [bản rà thiết kế](#design-review-20261106); sau đó Owner duyệt nhóm guard ưu tiên, xử lý tài liệu cũ và thêm kiểm từ ảnh BE-03. Phần local đã hoàn tất tại [checkpoint07/11](#priority-guards-20261107), kết quả chuẩn ở Active/QA. [P1–P7](#pending-policy-20261107) đã đối chiếu lại và rút bảng A/B; không hỏi lại các yêu cầu thiết kế đã rõ; không đóng toàn BE-01–BE-07.

**Cập nhật sau xử lý conflict06/11:** bản ghép main/tai và kiểm hồi quy mới xem [Active](00-MASTER-CHECKLIST.md#active)/PR18; giữ các quyết định0/0.1, không coi merge là nghiệm thu FE/Render hoặc hoàn tất cả mục audit. Mốc Git/source của bàn giao trước chỉ mô tả lúc ghi; khi mở session mới phải kiểm git status/diff hiện tại.

<a id="fe-refresh-plan-20261109"></a>
### FE vừa cập nhật — danh sách cần kiểm lại và đề xuất hai phase — 09/11/2026

**Chỉ đạo mới của Owner:** FE vừa có đợt cập nhật; các phát hiện UI/API đã báo được giữ làm **lịch sử/cần kiểm lại trên build mới**. Chưa có kết quả xác minh mới để kết luận đã sửa hoặc còn lỗi. Khi đọc các checkpoint/bàn giao cũ, trạng thái “FE chưa sửa”, ảnh401/403/400/405/500 và các checkbox là snapshot ở thời điểm ghi. LAN-1/LAN-2 giữ nguyên ngày/nội dung lịch sử; attendance dùng contract09/11 cho lần kiểm tới.

**Mốc lập kế hoạch, trước triển khai Phase1:** lỗi từng quan sát, request bị từ chối đúng contract, yêu cầu tích hợp chưa nghiệm thu và khoảng trống BE từ review source là các loại khác nhau. FE cập nhật không thay bằng chứng kiểm BE đã có; chỉ mở bug hiện tại khi tái hiện trên đúng FE/BE build và có expected độc lập. Lúc lập danh sách chỉ sắp xếp tài liệu; kết quả triển khai sau đó ở [review1–4](#phase1-review-20261109), bằng chứng hiện hành tại Active/QA.

| Mã | Điều đã báo/đưa vào bàn giao | Loại và điều cần kiểm trên FE mới |
|---|---|---|
| F01 | Coach đổi dropdown buổi nhưng detail, attendance/form sửa còn dữ liệu buổi cũ; payload từng dùng attendanceId85 | Quan sát UI cũ. Kiểm đổi buổi/đổi nhanh, response về muộn, sessionId+memberId+attendanceId và form cùng ngữ cảnh; record chưa có không lấy kết quả buổi trước. Chưa kết luận riêng hàm gửi ID sai |
| F02 | Ghi attendance từng gọi correct403, check-in400 LOBBY_CHECKIN_DISABLED hoặc POST root attendances405 | Request cũ theo policy/build cũ. Kiểm chọn đúng training-results trong buổi và correct sau end theo D15 mới; Coach đúng phân công/trong cutoff được sửa, Coach khác/quá hạn vẫn bị chặn. Check-in cửa vẫn tắt, root vẫn không có POST |
| F03 | Payload từng có Late; chưa chứng minh ghi thành công/readback; note trước đây chưa có trong response | State mới chỉ Present/Absent, dữ liệu Late lịch sử vẫn đọc được. Kiểm first-write/bulk/QR nếu dùng, badge sau POST lỗi, note đọc→gửi→lưu→tải lại ở Coach/Member và metadata hạn sửa. Note/cutoff09/11 là tích hợp mới cần nghiệm thu, không mặc định FE đang thiếu |
| F04 | Coach từng gặp403 ở hai GET enrollment lớp23; sau đó roster lớp10 đã trả200 | Giữ cả lỗi cũ và kết quả thành công đúng phạm vi. Kiểm phân công/tài khoản/authority trước khi kết luận403 sai; không tự mở quyền hoặc coi mọi roster đã đạt |
| F05 | Member GET training-results403; Member gọi payment-resolutions403 | **Owner xác nhận OK09/11** cho mục đã báo. Giữ lịch sử và quyền hiện hành; không mở lại chỉ vì thiếu log của snapshot cũ, không suy thành nghiệm thu toàn bộ ma trận Member |
| F06 | Normal từng bị PACKAGE_EXPIRED dù chưa mua gói chung; catalog lớp17 từng404 | **Owner xác nhận OK09/11** cho mục đã báo, gồm nhánh catalog trong nhóm này. Không yêu cầu kiểm lại mặc định; chỉ mở lại khi có diff/failure/nghi vấn liên quan |
| F07 | Nội dung trống “sau khi Backend ghi nhận”, tên “Gói hội viên”, thao tác mua/gia hạn gói chung hoặc enroll Advanced qua tuyến chung | Yêu cầu tích hợp/câu chữ của LAN-1, không phải mọi mục đều có request lỗi đã xác nhận. Kiểm FE mới phản ánh đúng quyền Advanced/khóa Normal, dùng advanced-registrations và không còn đi vào API legacy đã đóng. FE quyết định cách trình bày |
| F08 | Các màn chưa thống nhất quyền confirm-cash của Receptionist/Manager và quyền xử lý hồ sơ tiền | Điểm rà/bàn giao, cần đối chiếu UI mới với D10/D16. Receptionist xác nhận Cash theo quyền hiện hành; duyệt/hoàn tất CashCorrection là thao tác khác, không suy thêm quyền từ nút hiển thị |
| F09 | Log401 cũ trên auth/me và nhiều API; phản hồi Access Denied/phiên | Chưa quy nguyên nhân cho BE hoặc kết luận login hiện tại hỏng. Kiểm cookie/proxy và cách FE xử lý401, ACCOUNT_NOT_ACTIVE, SESSION_ROLE_CHANGED, PASSWORD_CHANGE_REQUIRED;403 nghiệp vụ không mặc định logout |
| F10 | Báo vắng từng200 nhưng thiếu Absent; room/sĩ số thay đổi cần FE tải lại | BE đã sửa/kiểm local, room/capacity migration cũ đã có xác nhận Owner. Kiểm notice+Absent/readback, roster/roomCode/maxCapacity theo response. Không mặc định FE còn hardcode hoặc yêu cầu chạy lại migration cũ |
| F11 | GET me/subscriptions sai tuyến; Admin dashboard0, activeSubscriptions11, revenue khác dashboard,12buổi nhưng13session, F-06/F-07 từng500 | Nhóm API/số liệu lịch sử cần đối chiếu riêng. Có trường hợp đúng contract/cách đếm; F-06/F-07 từng có lượt trả200. Dùng đúng endpoint/kỳ/phạm vi/buổi hợp lệ, không ép số liệu demo hoặc sửa dữ liệu để khớp snapshot |
| F12 | VNPay từng thành công ở gateway nhưng Pending/Expired tại SCMS; tích hợp xác minh/readback | **Owner xác nhận OK09/11** cho luồng VNPay đã báo. QueryDR là nhánh riêng: Owner nhớ đã tắt/không dùng; rà/chốt lại khi đến Phase2 mục3, không coi xác nhận này là QueryDR live PASS và không tự bật lại |

Nguồn chi tiết: [LAN-2](../SCMS-BAN-GIAO-FE-RBAC-LAN-2-2026-11-07.md), [LAN-1](../SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md), [room/báo vắng](../SCMS-BAN-GIAO-FE-2026-11-02.md), [VNPay](../SCMS-BAN-GIAO-FE-VNPAY-2026-11-04.md), [attendance09/11](../SCMS-BAN-GIAO-FE-ATTENDANCE-2026-11-09.md). Không dùng expected “Coach luôn403 sau end” của LAN-2 cho contract mới.

**Phase1 đã được Owner duyệt — thực hiện mục1–4, dừng trước mục5.** Số thứ tự dưới đây đồng bộ với phần trả lời Owner đã duyệt: mục4 gồm giá lớp **và** sân đúng môn; mục5 là R1/R2. Giữ diff hiện có. Ưu tiên kiểm/chốt được bao nhiêu trước bàn giao thì làm bấy nhiêu để FE nhận nhanh; không tự chạy cloud/seed/live ngoài phạm vi.

**Điều phối mới nhất — Owner09/11, audit và dựng lại DB demo:** thay phương án sửa riêng buổi164 bằng rà toàn DB hiện tại và chuẩn bị một [schema dựng lại sạch](../database/schema_postgres.sql). Mọi dữ liệu SCMS là demo; đã bổ sung nguyên tắc double-check/phản biện và xưng Tôi–Bạn trong AGENTS.md. Phạm vi đọc cloud, kiểm local và script chạy lại theo [Active](00-MASTER-CHECKLIST.md#active)/QA; hướng dẫn tại [docs02](02-DEPLOYMENT-GUIDE.md#demo-rebuild-20261109). Không tiếp tục yêu cầu consent cho ca164 hoặc tự tạo incident để vượt19. Sau Owner chạy schema thành công mới push/deploy, kiểm FE đơn giản rồi trình đề xuất mục5, validation độ dài tên/khu vực sân và policy chưa chốt để duyệt; chưa triển khai các mục đó.

**Lịch sử deployment buổi164 — đã được yêu cầu dựng lại thay thế:** query xác nhận Tennis Advanced dùng sân Bắn cung nên guard19 chặn đúng. Không có thao tác đổi sân/consent/incident nào được thực hiện. Pending của user40 là trạng thái OTP hợp lệ, không là dữ liệu rác; audit đã sửa tiêu chí. Log thiếu provenance là legacy cần phân biệt với lỗi dữ liệu, không tự suy toàn DB hỏng từ vài phát hiện. Giữ lịch sử phản ánh FE để kiểm lại trên build mới.

| Thứ tự | Nhóm công việc | Phạm vi/kết quả cần có |
|---|---|---|
| 1 | R3, BE-01/03/04: scope và ma trận quyền | Coach A/B, Member A/B cùng center; roster/session/attendance/result/plan/evaluation/assigned members/feedback/at-risk/Advanced. Tái dùng kiểm đã đạt, bổ sung ô thiếu; tách lookup chung khỏi dữ liệu riêng. Giữ guard/test center đã có, không mở lượt rà center khác |
| 2 | TK08/TK10: validation và RBAC | Status thiếu/sai không tự thành Active; permission ID sai không âm thầm thay tập quyền một phần. Tách lookup role khỏi xem/sửa full matrix. Guard quản trị cuối cùng hoặc quyền đọc chưa đủ căn cứ không được tự thành rule mới |
| 3 | RP-43, BE-06: audit/report | Scope Admin hệ thống/bảo mật, Manager nghiệp vụ/tiền trong center; query/provenance/filter/phân trang, giới hạn log legacy. Không suy center lịch sử từ actor hiện tại hoặc xóa log; report theo D13/D18 |
| 4 | RP-13/P5 và TK05/P4: giá lớp, sân đúng môn | Subject là mặc định, actor có quyền định giá Normal khi tạo/công bố; giữ Invoice/purchaseAmount cũ và Advanced lấy Subject. Tài nguyên đúng môn dùng kiểm nhất quán, theo mô hình đã duyệt; không thêm rooms.subject_id hoặc suy quan hệ từ mã/tên có thể sửa. Điều kiện đổi giá sau công bố/Pending/người học còn thiếu phải trình riêng |
| 5 | **DỪNG — review1–4 và đề xuất R1/R2 cho session mới** | Làm bảng trước/sau hoặc hỏi/trả lời cho mục1–4: thay đổi/giữ lại, API/DB/FE ảnh hưởng, expected và kiểm đã đạt/chưa đạt, điểm còn cần Owner chốt. Sau đó đề xuất riêng R1/R2 để Owner duyệt trước khi tiếp tục ở session mới; không triển khai R1/R2 trong lượt1–4 |
| 6 | BE-01–BE-07/TK11 và readiness còn lại | Contract/error/legacy, ma trận còn thiếu, build/schema/bàn giao và phạm vi P1.5/gói T. Chưa triển khai vượt điểm dừng mục5; không chạy lại SQL16 hoặc tự coi API/DB/cloud đã nghiệm thu |

**Điều kiện ra Phase1:** mỗi nhóm nêu expected độc lập, fixture và phần đạt/chưa đạt; test đích rồi clean test cho nhóm BE, PostgreSQL local khi query/lock/commit cần kiểm; contract/schema/bàn giao khớp source đã kiểm. Các lựa chọn nghiệp vụ còn thiếu được trình bằng ca cụ thể, không hỏi lại Dxx đã chốt. Phê duyệt rà/sửa phần đã rõ không mặc nhiên phê duyệt mọi phương án R1/R2, quyền đọc full matrix, ca lịch sử hoặc guard quản trị cuối cùng.

<a id="phase1-review-20261109"></a>
### Review Phase1 mục1–4 — trước điểm dừng mục5

**Phạm vi:** implementation BE local, contract và migration riêng. Không sửa FE, không sửa LAN-2 ngày07/11, không thực thi SQL cloud/SQL16 hoặc phát hành bản BE Phase1. Trạng thái test/SHA/fingerprint chuẩn duy nhất tại [Active](00-MASTER-CHECKLIST.md#active)/`phase1Implementation20261109` trong QA; chưa suy thành nghiệm thu FE/Render/CRUD/live.

| Mục | Trước | Sau / phần giữ lại | Ảnh hưởng và giới hạn cần review |
|---|---|---|---|
| **1 — R3** | Có owner/assignment/center guard ở các luồng học tập; thêm authority vẫn có thể đi qua API feedback có danh tính, at-risk toàn center và DTO hồ sơ quản trị | Chặn Coach/Member đọc DTO staff kể cả thêm quyền; Coach dùng roster và feedback ẩn danh/at-risk `me`. Kiểm ID/filter/coachId trên session/attendance/result/plan/evaluation và hàng chờ Advanced; denied không mutation/audit. Giữ lookup classes/rooms và guard/test center hiện có | FE gọi đúng endpoint theo luồng; không mở rộng quyền để tránh403. Các trường hợp đổi phân công rồi đọc lịch sử, hủy/đảo quyền chưa có quyết định vẫn giữ giới hạn cũ. Không đóng toàn bộ BE-01–BE-07 hoặc full CRUD từ nhóm này |
| **2 — TK08/TK10** | Status thiếu/sai rơi về Active; ID permission sai có thể làm thay tập quyền một phần | Status explicit thiếu/sai400, default khi tạo vẫn riêng. Validate toàn bộ permission IDs trước thay, khóa role và audit trước/sau cùng transaction; ID lặp gộp, tập rỗng giữ nghĩa API cũ | FE xử lý400, không tự coi tài khoản đã mở khóa hoặc lưu quyền thành công. GET role lookup chưa đổi; quyền đọc full matrix và guard quản trị viên cuối cùng chưa tự bổ sung |
| **3 — RP-43/report** | GET audit chỉ có authority, trả toàn bộ log, thiếu center/loại/provenance/phân trang | Admin SYSTEM_SECURITY, Manager BUSINESS own-center; snapshot target tại sự kiện, query/filter/date/page không mở scope, mảng+header tổng số. Report giữ D13/D18, Admin center NULL không thành report global | Migration18; log legacy/target chưa rõ vẫn giữ DB nhưng chưa trả qua API scope. FE cần đọc mảng và header phân trang. Không suy center lịch sử từ actor hiện tại hoặc gán lại dữ liệu cũ |
| **4 — giá Normal/sân đúng môn** | tuitionFee nhận nhưng override bị bỏ; room chỉ kiểm Indoor/Outdoor/capacity, chưa chứng minh phục vụ đúng môn | Admin/Manager+MANAGE_CLASSES chọn giá tạo/công bố đầu tiên; Subject mặc định, đơn lấy Class đã duyệt, tiền cũ giữ nguyên, Advanced theo Subject. Thêm activityType độc lập tên/mã, dùng chung guard/khóa khi cấu hình và ghi lịch, admission chỉ tính sân đúng môn | Migration19–20. Chưa rõ loại sân thì chặn lịch mới, không sửa lịch sử. Giá khác ở lớp đã công bố/legacy/có đơn trả409 rõ; quyền định giá lại sau công bố/Pending/người học chưa mở. Indoor/Outdoor và giờ D04 giữ rule hiện hành |

**Ma trận bằng chứng theo luồng:** dưới đây là nhóm đã kiểm/reuse trong suite local hiện hành; không phải bảng cấp thêm quyền.

| Nhóm API | Expected bảo vệ / kiểm được | Bằng chứng hoặc phần còn mở |
|---|---|---|
| Roster/enrollment và Member được giao | Coach đúng lớp đọc; B cùng center không lấy roster A bằng classId/memberId hoặc thêm authority; Member owner từ phiên | RbacGuardIntegrationTest và PhaseWorkflowHttpTest giữ các kiểm đã có; không suy mọi alias/CRUD đều đã nghiệm thu FE |
| Session, attendance, result | List/filter chỉ thu hẹp owner/Coach; detail sai phân công403, list không trả record ngoài scope; lịch sử/note/check-in không bị test đọc sửa | RbacGuardIntegrationTest, SessionPrivacyTest, TrainingResultAccessIntegrationTest, AttendanceCorrectionIntegrationTest |
| Plan/evaluation | Coach đúng authority/phân công; Manager/Member có thêm authority không ghi chuyên môn. Member đọc plan lớp của mình, không đọc plan riêng người khác | RbacGuardIntegrationTest; chưa mở quyền cho ca lịch sử sau hủy/đổi Coach |
| Feedback/at-risk/DTO staff | Feedback Coach ẩn danh, filter Coach khác không mở dữ liệu; at-risk theo phiên. DTO quản trị riêng không thành lookup Coach/Member | Test phiên login/auth-me thật với extra authority, read không ghi audit; common classes/rooms vẫn đọc được |
| Advanced queue/decision/book/reschedule | Coach A/B chỉ thấy hàng chờ mình; B không quyết định đơn A, Member không order đơn người khác; retry decision không audit lặp. Book/dời/thay dùng quyền Paid/phân công/lịch và sân đúng môn | RbacGuardIntegrationTest, PhaseWorkflowHttpTest, PrivateSessionServiceTest, SessionIncidentServiceTest, ScheduleDatabaseTest theo môi trường được bật. Skip không là PASS |
| Audit/report | Scope khác nhau theo role/authority, date trọn ngày, filter/page không vượt scope; đổi center actor không chuyển log cũ; log thiếu provenance vẫn giữ DB | RbacGuardIntegrationTest và ReportServiceTest; kết quả PG/commit/migration tại Active/QA. Không thay report scope của Admin |
| Giá/tài nguyên | Giá950000 đã công bố tạo đơn950000 dù Member gửi amount1; Subject đổi không đổi đơn; giá975000 ở draft lần công bố đầu, giá không hợp lệ từ chối. Cấu hình room và tạo lịch tranh chấp không tạo lịch sai môn | PublicationResourceHttpTest với trạng thái sau commit/race; migration guard/replay/snapshot bất biến theo runner riêng. Không có kiểm FE/Render mới |

**Giải trình fixture:** rule sân đúng môn cần fixture khai báo activityType và Subject chuẩn, thay cho room chưa rõ loại/custom slug không phải môn catalog. Các test tiền/enrollment/Coach/QueryDR giả lập giữ expected nghiệp vụ; test totalSpent của staff DTO dùng actor Manager hợp lệ, vẫn giữ assertion chỉ cộng Paid. Test đăng ký Normal lặp giữ400 ALREADY_ENROLLED hiện hành và kiểm không nhân đơn, không đổi endpoint thành trả lại201. Không nới rule hoặc đổi dữ liệu thật để làm test xanh.

**Khoảng trống kỹ thuật phát hiện khi kiểm PG, để mục6/CRUD sau điểm dừng:** DB giới hạn room_name50/location100, nhưng RoomRequest/JPA chưa diễn đạt đầy đủ các giới hạn này; tên quá dài hiện có thể409 do constraint thay vì400 validation rõ. Fixture mới dùng tên hợp lệ; không tăng giới hạn DB hoặc tự sửa luồng CRUD ngoài nhóm được duyệt. Đây là quan sát BE local từ dữ liệu kiểm, không phải lỗi FE mới đã xác nhận. Fixture hóa đơn Cash cần đủ subtype Receptionist và flush trước invoice theo FK PostgreSQL; không đổi model/guard tài chính để bỏ FK.

<a id="phase1-item5-proposal-20261109"></a>
### Mục5 — đề xuất để Owner duyệt cho session mới, chưa triển khai

| Mục | Ví dụ cụ thể / đề nghị | Giới hạn cần Owner duyệt |
|---|---|---|
| **R1 — nên làm tiếp** | Session05/11 có Present và Absent với checkInTime:null. Lọc ngày05/11 hiện có thể bỏ Absent vì so timestamp check-in. Đề nghị startDate/endDate của attendance gắn Session lọc theo sessionDate, bao gồm trọn hai đầu ngày; record lobby lịch sử không Session giữ checkInTime | Chốt nghĩa filter này trước sửa contract/source/test. Chỉ GET, giữ scope và bản ghi cũ; correction muộn không giả giờ check-in. Kiểm Present/Absent/null/date boundaries/filter kết hợp và lobby legacy local; không cần setup buổi trên cloud |
| **R2 — đề nghị chưa mở API mới** | Buổi kết thúc07:30, Coach muốn bổ sung content chuyên môn08:00. Correct D15 đã sửa note/state tới trước nửa đêm, không đồng nghĩa sửa content; evaluations không gắn sessionId nên không tự thay thế mọi nội dung theo buổi | Ưu tiên xác nhận nhu cầu thực tế trên FE mới. Nếu Owner cần, đề xuất riêng content-only cho result có sẵn, đúng Active Coach+RECORD_RESULT/phân công/center, end→cutoff ngày buổi, audit trước/sau và reason nội bộ; không đổi attendance/check-in/credit/lịch. Chưa có result, buổi hủy/không tổ chức/đổi Coach cần xét riêng; không tự tạo nội dung hoặc mở quyền lịch sử |

**Điểm dừng:** Owner review bảng1–4 và chọn/chỉnh hướng R1/R2; chỉ tiếp tục mục5 sau phê duyệt trong session mới. Mục6/readiness và Phase2 chưa được suy là đã hoàn tất. Flow/câu chữ/bố trí UI vẫn do FE quyết định.

**Phase2 được đồng ý với điều chỉnh — nghiệm thu chung FE mới / Render / CRUD.** Chỉ chạy ca phụ thuộc khi đúng source/build/schema và account/fixture được phép.

1. Xác định build mới rồi kiểm các mục lịch sử còn lại; F05/F06/F12 đã được Owner xác nhận OK, chỉ kiểm lại khi có diff/failure/nghi vấn liên quan. Mỗi kết quả có môi trường/build/actor/response/readback phù hợp; không mặc định lỗi cũ còn tồn tại.
2. Gom CRUD/nghiệp vụ năm role: Coach attendance/note/correction/plan/evaluation/feedback/at-risk/Advanced; Member và staff Normal/Advanced/catalog/roster/lịch/phòng/sĩ số, Cash/VNPay/receipt/resolution,12h/45 ngày/credit/báo vắng/hủy/dời/sự cố/bù theo Dxx. Kiểm lưu sau commit và retry; A/B cùng center, giữ guard/test center đã có.
3. **Tích hợp — rà/chốt khi đến mục này:** Google/avatar để **sau khi bàn giao các phần Phase2 khác**. OTP cơ bản register→nhận mã→verify→login đã được Owner xác nhận xong; các biên sai/hết hạn/dùng lại/giới hạn là phạm vi QA cần cân nhắc, không đồng nghĩa tính năng OTP chưa xong và không bắt chạy lại luồng đã đạt. AI chưa hoàn tất: cần rà trạng thái thực tế rồi chốt phần còn lại. QueryDR dùng xác minh thanh toán VNPay, độc lập OTP; quyết định trước ghi giữ code/bảng/migration, Owner nay nhớ đã tắt/không dùng. Khi đến đây đối chiếu quyết định, cấu hình được phép xem và hành vi thực tế để chốt dùng/tắt/để sau; **không tự bật lại, xóa metadata/bảng hoặc sửa env**. Kênh ngoài/provider/email/gateway/live chỉ theo phạm vi được duyệt.
4. Xử lý phiên UI trong lượt chung sau flow; đối chiếu năm role và đóng từng tiêu chí bằng PASS/FAIL/BLOCKED/NOT_RUN.403 đúng quyền được giữ. Chỉ sửa lỗi còn tái hiện theo rule đã chốt; lựa chọn mới trình ví dụ cụ thể. Không gom Google/avatar vào việc chặn bàn giao các phần khác.

**Điểm dừng Owner đã chốt:** thực hiện Phase1 mục1–4 theo thứ tự; trước mục5 gửi bảng so sánh/review và đề xuất mục5 để duyệt cho session mới. Phê duyệt này không mở quyền seed/reset/SQL cloud/live test. Không chỉnh các bản bàn giao ngày cũ thành bằng chứng FE mới.


- [x] **0 — Hai403 của Member, phần BE local và tài liệu:** đã sửa training-results, rà năm role payment-resolutions và ghi yêu cầu FE. Chưa deploy/nghiệm thu Render/FE.
- [x] **0.1 — Bỏ điều kiện gói chung cho Normal, phần BE local/tài liệu:** đã sửa và kiểm đăng ký/waitlist, thanh toán, owner/center/lịch, retry/race và giữ liên kết cũ. 0.1-B/C đã kiểm local, SQL16 đã được Owner xác nhận PASS; chưa deploy/kiểm Render/FE. Catalog/17 còn mở.
- [x] **0.1-B — Đóng tuyến ghi danh Advanced cũ:** hai tuyến enroll chỉ nhận Normal; không tự thăng waitlist Advanced. Test đích/clean đã đạt; final PG workflow sau migration cũng đạt. Chưa deploy Render.
- [x] **0.1-D — Loại mô hình gói chung:** BE đóng API cũ đã kiểm local; Owner gửi SQL16 PASS trên Supabase, model_retired=true, tất cả bộ đếm xóa/tháo liên kết0. DB không có dữ liệu cũ cần xóa; trigger/marker đã áp dụng theo kết quả này. Agent không tự truy vấn cloud; BE còn cần deploy.
- [x] **0.1-A — CashCorrection, BE local:** sau Owner xác nhận SQL16 PASS đã bỏ suy luận phụ thuộc theo thời gian; Normal Pending/Paid độc lập không chặn. Giữ liên kết thật/D16, kiểm lại khi Complete, giữ hóa đơn/ghi danh Normal và Paid/amount gốc; kiểm H2/PG theo docs00/QA, chưa deploy.
- [ ] **BE-01 — Ma trận quyền:** đã bắt đầu đối chiếu contract/controller/service/test, ghi [ma trận quan sát và điểm chờ Owner](#be01-review-20261106); chưa công bố policy toàn API/năm role. Giữ phần đã chốt ở0/0.1; GET roles/permissions/role detail đã được Owner chốt và kiểm local10/11, phần matrix/quyền ghi còn lại vẫn mở.
- [ ] **BE-02 — Phiên và401:** đã kiểm local quyền hiện hành/Inactive/đổi role/registry thu hồi/password và public IPN; chưa đủ nghiệm thu vòng đời cookie/timeout/proxy/Render hoặc quy nguyên nhân401 cũ. Xem checkpoint07/11.
- [ ] **BE-03 — Phạm vi Coach:** đã kiểm hai GET enrollment đúng/sai phân công, bỏ filter/thêm authority/khác center/buổi kết thúc và giữ chặn ghi; guard ghi plan/evaluation có kiểm actor/phân công. Ảnh lớp23 chưa có auth/me/assignment/runtime SHA để kết luận live. Các nhóm Coach còn lại và toàn bộ ma trận vẫn mở.
- [ ] **BE-04 — Phạm vi Member/IDOR:** training-results đã kiểm ID từ phiên, bỏ filter/đổi ID/thêm authority; Normal từ chối gửi memberId khác. Tái sử dụng workflow invoice/subscription làm bằng chứng riêng từng API; chưa bao phủ toàn bộ nhóm.
- [ ] **BE-05 — Xác nhận Cash:** có policy Receptionist+MANAGE_INVOICES và workflow center/state/retry/race/commit; chưa đủ ma trận riêng năm role. Không nhầm với quyền Manager duyệt CashCorrection ở0.1-A.
- [ ] **BE-06 — Report/audit log:** nhóm RP-43 đã triển khai scope/provenance/filter/phân trang và kiểm local theo [review Phase1](#phase1-review-20261109); report giữ D13/D18. Chưa đóng toàn bộ ma trận/legacy hoặc nghiệm thu FE/Render.
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
| GET `/api/roles` | Owner10/11: Admin Active; role khác403 dù có authority quản trị | Đọc danh mục role toàn hệ thống, gồm permissions; không filter center | Anonymous401; trạng thái kiểm/phát hành ở Active/QA, thay gate cũ cho cả năm role |
| GET `/api/roles/{id}` | Cùng gate Admin để tránh lách qua ID | Đọc role toàn hệ thống, gồm permissions | Admin: ID không có404; role khác403 trước đọc dữ liệu |
| GET `/api/permissions` | Owner10/11: Admin Active | Toàn danh mục permission, giữ DTO | Anonymous401; role khác403; không tự đổi gate ghi PUT |
| PUT `/api/roles/{id}/permissions` | `MANAGE_PERMISSIONS`; không AND role Admin | Đổi permission của role dùng chung toàn hệ thống; service lấy actor để audit, không có scope center | Thiếu authority403; role không có404. Chưa có quyết định xác nhận chỉ authority là đủ; chưa đổi quyền |
| GET `/api/users`, `/api/users/{id}` | `MANAGE_USERS` OR `MANAGE_RBAC`; không gate theo tên role | UserService dùng findAll/findById, chưa thấy filter owner/center ở hai đường đọc này | Thiếu authority403; ID không có404. Thiếu đối chiếu đầy đủ policy/fixture center, không coi findAll là quyết định cho phép xuyên center |
| GET `/api/members`, `/api/members/{id}`; GET `/api/staff` | members: `MANAGE_USERS` OR `REGISTER_MEMBER`; staff: `MANAGE_USERS` | Các đường đọc đã xem dùng danh sách/ID toàn cục, chưa thấy scope center; không lấy guard tạo tài khoản làm guard cho đọc | Thiếu authority403; member ID không có404. Chưa nghiệm thu role/owner/center của CRUD; chưa sửa trong lượt này |
| GET `/api/audit-logs` | `VIEW_AUDIT_LOG`; không AND role Admin/Manager | Controller gọi trực tiếp findAllByOrderByCreatedAtDesc; chưa có filter center/owner trên đường gọi này | Thiếu authority403; chưa xác nhận policy đọc theo từng role/center. Chỉ ghi nhận cho matrix, không xử lý gộp BE-06 |

Source đối chiếu: [RoleController](../src/main/java/com/sportscenter/user/RoleController.java), [RoleService](../src/main/java/com/sportscenter/user/RoleService.java), [RoleResponse](../src/main/java/com/sportscenter/user/dto/RoleResponse.java), [UserController](../src/main/java/com/sportscenter/user/UserController.java), [UserService](../src/main/java/com/sportscenter/user/UserService.java), [MemberController](../src/main/java/com/sportscenter/user/MemberController.java), [MemberService](../src/main/java/com/sportscenter/user/MemberService.java), [StaffController](../src/main/java/com/sportscenter/user/StaffController.java), [StaffService](../src/main/java/com/sportscenter/user/StaffService.java), [AuditController](../src/main/java/com/sportscenter/audit/AuditController.java). Mã lỗi trong bảng là nhánh source qua SecurityConfig/[GlobalExceptionHandler](../src/main/java/com/sportscenter/common/exception/GlobalExceptionHandler.java), chưa phải response quan sát ở lượt này; không hứa mọi403 có field `code`.

**Test/chứng cứ:** [RoleServiceTest](../src/test/java/com/sportscenter/user/RoleServiceTest.java) chỉ kiểm actor audit khi đổi permission bằng gọi service trực tiếp với authority rỗng; không chứng minh HTTP cho phép/từ chối role đó. Tìm literal ba nhóm `/api/roles`, `/api/permissions`, `/api/audit-logs` trong `src/test` chưa thấy test HTTP đích; không suy không có mọi hình thức test gián tiếp. [AuthorizationHttpTest](../src/test/java/com/sportscenter/config/AuthorizationHttpTest.java) dùng service mock cho gate một số workflow, không kiểm center/state commit. Bằng chứng integration đã có cho0/0.1 tiếp tục dùng đúng phạm vi tại Active/QA, không chạy lại hoặc gán thành PASS mới.

**Đã được Owner chốt10/11:** ba GET danh mục/chi tiết chỉ Admin Active, không thêm yêu cầu MANAGE_PERMISSIONS cho quyền đọc; không dùng authority phụ để mở Manager/Coach/Member/Receptionist. Triển khai/kiểm theo Active/QA. Quyết định này thay điểm chờ ai được đọc trước đây, không tự chốt quyền ghi hoặc guard quản trị cuối cùng.

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

<a id="rbac-fe-attendance-feedback-20261107"></a>
### Phản hồi FE attendance — 07/11/2026

**Bàn giao:** [RBAC LAN-2 — Attendance Coach](../SCMS-BAN-GIAO-FE-RBAC-LAN-2-2026-11-07.md). Chỉ soạn tài liệu, chưa sửa source FE hoặc xác nhận FE đã tiếp nhận/triển khai.

- Owner đọc roster lớp10 được200; auth/me là Coach có RECORD_RESULT/MANAGE_TRAINING_PLAN, không first-login. GET history và catalog đã có dữ liệu; chưa kiểm âm đầy đủ hoặc đóng BE-03.
- Lỗi được Owner xác nhận là đổi dropdown09/11 rồi13/11 nhưng vùng attendance vẫn giữ dữ liệu/ngữ cảnh cũ. attendance85 thuộc session10/ngày05/11; catalog có session11/ngày09/11 và109/ngày13/11. Rút kết luận quá sớm về riêng hàm gửi chọn sai ID: Payload85 có thể đúng với record cũ còn hiển thị. Network đang mở một request cũ không chứng minh có POST mới sau mỗi lần đổi buổi.
- Giữ D15: Coach ghi trong buổi, quản lý correction sau buổi. POST correct403 phù hợp phiên Coach; check-in400 đã xác nhận LOBBY_CHECKIN_DISABLED; POST root attendance405 không có method ghi. OPTIONS200 và GET200 sau POST lỗi không là ghi thành công. Owner nói chưa thử điểm danh lần đầu: first-write FE/live vẫn NOT_RUN.
- Catalog chỉ Scheduled của Normal đã công bố, không là lịch đủ cả buổi hoàn tất. History có nhiều session/class; attendanceId khác sessionId. Không mặc định Present/Absent từ kết quả buổi khác; null recordedBy tương thích finalizer nhưng chưa kiểm provenance từng dòng. Check-in/out timestamp không thay ngày/giờ Session.
- Nguồn thiết kế đã kiểm: Member.pdf trang4/6 dùng tên Check-in và cửa sổ30phút; analysis24/10 trang8 tách check-in cửa; MVP18/10 trang51/61 còn để quyền sửa/cửa sổ chờ chốt. D15 lịch sử mục33.3/33.5 ghi Owner chọn attendance trong giờ/Present-Absent và Manager sửa sau buổi. Không hỏi lại quyết định này. Tiến độ/đánh giá sau buổi là hành vi cần phân biệt.
- Owner hỏi chỗ lưu điểm danh/ghi chú: đã đối chiếu Attendance/TrainingResult DTO, service và AuditService/SystemLog. Ghi thủ công lưu result và đồng bộ attendance; correct đổi attendance, đồng bộ result nếu có và ghi reason ở system_logs.detail, không set attendances.note. GET attendance không có note; báo vắng mới có nhánh setNote hiện thấy trong source. correct403 không ghi dữ liệu/lý do. Đây là mapping source, chưa query DB runtime. DB hiệu lực do DB_URL của BE Render quyết định, tài liệu môi trường là PostgreSQL/Supabase; chưa xác minh project DB cụ thể ở lượt này.
- Evidence chi tiết, nguồn/môi trường chuẩn ở Active/QA. Bước tiếp theo và tiêu chí nghiệm thu UI ở LAN-2; không yêu cầu Owner gửi lại bằng chứng đã đủ. Không sửa FE/BE/DB, không đổi lịch sử Supabase hoặc chạy test mới trong lượt tài liệu này.

<a id="attendance-note-correction-proposal-20261109"></a>
### Attendance cuối ngày/note — 09/11/2026, BE ĐÃ TRIỂN KHAI, KIỂM LOCAL THEO ACTIVE

**Quyết định hiện hành:** [D15](04-DEBUG-NOTE.md#d15); method/field/type/state/error ở [contract09/11](01-API-CONTRACT-FE-BE.md#attendance-correction-20261109). Owner đã chốt cutoff Coach trước00:00 hôm sau ngày buổi; hai mốc lifecycle/xử lý vắng; note của đúng Member và reason nội bộ. UI là đề xuất, FE quyết định. Không hỏi lại các quyết định này.

- Đã triển khai policy/DTO/API correction cho Coach Active+RECORD_RESULT, đúng phân công/cùng center; reason/target/state/note có mã lỗi riêng. Correct hỗ trợ attendanceId hoặc sessionId+memberId, khóa center→session trước đọc/upsert attendance; giữ check-in gốc, credit/lịch/hạn, notice.reason và content, đồng bộ result nếu có. No-op không nhân audit/record.
- Note có ở GET attendances và response ghi/đọc training-results; bỏ/null giữ, rỗng xóa có audit, trim/giới hạn theo contract. Session GET có metadata quyền/deadline/serverTime. Eligibility khi tạo attendance dựa quyền học đúng buổi/Paid; không chặn chỉ vì enrollment/quyền Advanced vừa Completed hoặc đã hết hạn hiện tại nhưng vẫn bao phủ buổi đó.
- Finalizer giữ lifecycle tại end, tạo Absent thiếu và queue review mới. Review sau cutoff đọc state hiện hành, chống lặp dưới cùng khóa; chế tài vẫn default tắt. Migration17 mặc định marker false, không queue buổi finalized cũ; replay giữ pending. [Vận hành/runner](02-DEPLOYMENT-GUIDE.md#attendance-review-20261109), không reset DB/chạy lại16 hoặc mutation cloud.
- Expected cũ “Coach403 ngay end” và “báo vắng/chế tài tại end” đã thay theo D15 mới; giữ kiểm center/7ngày. Kiểm local gồm login/auth-me, response + DB/audit sau commit, biên end/23:59:59.999/00:00, A/B cùng center, Manager sau hạn, record thiếu, Completed/Expired đúng buổi, manual/QR, note đơn/bulk/rollback/owner, race finalizer/review và thông báo không lặp. Kết quả/môi trường/source chỉ ghi tại [Active](00-MASTER-CHECKLIST.md#active)/[QA](../artifacts/qa/latest.json); lỗi fixture PG thiếu subtype đã sửa, không tính lỗi setup là PASS nghiệp vụ.
- [Bàn giao attendance09/11](../SCMS-BAN-GIAO-FE-ATTENDANCE-2026-11-09.md) nối flow/field cho FE quyết định UI; LAN-2 ngày07/11 giữ nguyên snapshot. Không gộp R1/R2/R3 hoặc RBAC khác, không sửa FE.

**Đối chiếu hệ thống tương đương đã làm trước khi chốt:** [TeamUp scope instructor](https://support.goteamup.com/en/articles/9327607-how-to-create-instructors-add-them-to-classes-appointments-courses) và [nhắc kiểm attendance sau lớp](https://support.goteamup.com/en/articles/9333531-create-a-pre-post-class-notification-for-your-staff) cho thấy có workflow đối soát; không chứng minh hạn sửa chung. [Mindbody](https://support.mindbodyonline.com/s/article/203259423-Classes-Class-Sign-in-screen?language=en_US) có quyền Make Past Reservations (search chính thức có nội dung, mở trực tiếp CSS Error), không đồng nhất correction SCMS. [Momence](https://help.momence.com/en/articles/7869547-momence-term-glossary) mô tả thêm/check-in người ở lớp quá khứ, không nêu deadline từng role. Hạn cuối ngày là quyết định Owner sau giải thích, không suy thành chuẩn bắt buộc từ các hệ thống này.

**Phát hành theo Owner:** đã push tai, mở [PR21](https://github.com/Aditerx/SWP391/pull/21), tích hợp main AI và kiểm lại bản phát hành theo Active/QA. Trạng thái merge/CI theo PR; trước kiểm FE cần migration17 và BE đúng build mới. Agent không chạy SQL cloud hoặc lại16. FE/Render/CRUD/live nghiệm thu đợt chung Owner đã hẹn; chưa xác nhận FE đã nhận/hiển thị note, không suy từ OpenAPI/local test thành live PASS.

<a id="coach-open-items-20261107"></a>
### Coach — phần còn mở sau LAN-2 — 07/11/2026

**Phạm vi rà:** đối chiếu checklist/contract/Dxx, các controller/service Coach liên quan và test đích hiện có; review tĩnh có giới hạn, không phải audit toàn bộ source hoặc một lượt chạy BE mới. LAN-2 chỉ bàn giao attendance; LAN-1 giữ luồng Member. Không lấy kết quả guard local hoặc roster200 làm nghiệm thu mọi thao tác Coach.

**Owner điều phối lại07/11:** attendance chờ phản hồi FE; CRUD FE/Render để sau khi xong các mục/luồng khác hoặc kiểm chung. Lượt rà Coach mới dùng A/B cùng center, không mở thêm kiểm center khác; guard/test center đã có giữ nguyên. Các việc BE dưới đây mới là đề xuất chờ duyệt, không là lệnh sửa được suy từ việc Owner hỏi kế hoạch.

| Nhóm | Trạng thái thực tế | Phần còn thiếu / bước đúng phạm vi |
|---|---|---|
| C01 — Attendance UI và first-write | Lịch sử07/11 có evidence UI không đồng bộ và các request đã giải thích trong LAN-2. Owner09/11 báo FE vừa cập nhật; trạng thái hiện tại cần kiểm lại theo [kế hoạch mới](#fe-refresh-plan-20261109), chưa kết luận còn lỗi | Gom ghi đơn/bulk/readback, QR nếu dùng và correction vào đợt chung. Xác định FE build mới rồi kiểm lại, chỉ sửa phần còn tái hiện. D15 ngày09/11 cho Coach đúng phân công sửa tới cutoff; BE đã triển khai theo Active. Coach khác/quá hạn vẫn bị chặn; không dùng expected Coach luôn403 sau end của bản07/11 |
| C02 — Giáo án/đánh giá RP-36 | Guard Coach + authority + assignment/actor/center và CRUD đích đã sửa/kiểm local theo Active/QA; contract đã có | Owner để CRUD FE/Render vào đợt chung sau; chưa có bàn giao chuyên luồng mới. Lượt BE mới kiểm A/B cùng center, Member ngoài phân công, target không đổi qua PUT; reuse bằng chứng guard đã có, không thêm kiểm center khác. Không ghép triển khai này vào LAN-2 |
| C03 — Tiến độ sau buổi và cửa sổ attendance | Member.pdf §6.2 mô tả ghi kết quả/tiến độ sau buổi; D15 chỉ chốt cửa sổ attendance. TrainingResultService.recordResult/recordBulkResults gọi requireWindow trước xử lý content, trong khi evaluations là API khác | Ví dụ buổi kết thúc07:30, Coach thêm nhận xét08:00 và không đổi attendance: nếu UI dùng training-results thì source hiện chặn cả request. Cần xác định API thực sự của màn Tiến độ và đối chiếu expected trước sửa; chưa kết luận toàn BE sai hoặc tất cả ghi chuyên môn bị khóa. Không mở cửa sổ attendance để chữa việc ghi nội dung |
| C04 — Lọc lịch sử attendance theo ngày | searchAttendances hiện lọc startDate/endDate bằng checkInTime; record Absent có thể null trường này | Với attendance85 ngày05/11/checkInTime null, predicate filter ngày sẽ loại record. Cần đối chiếu ý nghĩa filter trong contract với ngày buổi/lượt check-in, tái hiện test phù hợp rồi mới kết luận/sửa BE. Chưa bàn giao đó là bug BE đã xác nhận và không bắt FE sửa dữ liệu bù |
| C05 — Full scope và lịch sử khi đổi phân công | Roster, training-plan/evaluation writes, phiên và một số owner checks có bằng chứng đích; BE-01/BE-03 chưa đóng toàn bộ | Hoàn thiện method/path/authority/owner/center/error cho lớp/session, attendance/result, plan/evaluation, assigned members, feedback, at-risk và Advanced. Kiểm Coach khác/center khác/extra authority; tách quyền theo record Coach và phân công hiện hành khi đổi Coach/hủy/hết quyền, không tự mở lịch sử |
| C06 — Đặt/dời/nhận Advanced và tài nguyên | D03–D08 có contract và workflow local theo bằng chứng riêng; đây không là sửa mới trong LAN-2 | Chưa nghiệm thu toàn bộ luồng Coach trên FE/Render: GET advanced-registrations và POST /{id}/decision, book-private/reschedule/notice/consent, biên12h/45ngày và retry. Sân đúng môn TK05 còn gap mô hình/guard trong phạm vi room đã duyệt, không thêm subject_id tùy ý |
| C07 — Phiên và API ngoài nhiệm vụ | Guard phiên hiện hành/thu hồi đã kiểm local, docs01 có ACCOUNT_NOT_ACTIVE/SESSION_ROLE_CHANGED/401 | Không phải một lỗi đăng nhập đã xác nhận: auth/me/đọc API Coach hiện dùng được. Phản hồi UI khi hết/thu hồi phiên đưa vào đợt FE chung sau. Nếu Coach gọi payment-resolutions/report quản trị thì403 là đúng quyền hiện hành; chỉ ghi bug FE khi có request thực tế. Không cấp authority để giấu lỗi |
| C08 — API đọc đúng màn Coach | Source có GET /api/coaches/me/feedback và /api/coaches/me/at-risk-members; feedback lớp quản trị và reports có gate khác | Rà màn tương ứng dùng API dành Coach và scope filter đúng; không suy feedback hoặc dữ liệu học viên là báo cáo tài chính. Chưa có evidence live âm đủ để đóng toàn scope |
| C09 — Nguồn bàn giao/thiết kế | LAN-2 soạn cho attendance; docs01 có gate RP-36 và phiên, FE_Project-main vẫn là tham khảo cũ | Chưa có nghiệm thu FE tổng thể hoặc full matrix. Giữ D15 thay30phút/Late, D04/07 thay Member tự book, quyền sửa attendance khác quyền chuyên môn; điểm mơ hồ mới phải đối chiếu thiết kế/Owner trước đổi policy |

**Điều phối hiện hành:** không chờ test attendance để rà BE. Attendance chờ FE và nghiệm thu UI/CRUD/phiên gom sau theo Owner; đề xuất R1–R3 bên dưới mới chờ duyệt. Các case phạm vi Coach đã được chốt thì reuse expected/test, không hỏi lại; chỉ dừng hỏi ví dụ thật sự thiếu quyết định. Không đóng toàn RBAC từ lượt thu hẹp này.

<a id="coach-review-proposal-20261107"></a>
### Đề xuất lượt BE Coach để Owner duyệt — 07/11/2026

**Mốc rà trước triển khai Phase1: R3 đã được Owner duyệt, bắt đầu rà source; R1 cần giải thích thêm, R2 cần đánh giá lại độ cần thiết, chưa duyệt triển khai API mới.** Tại mốc lập đề xuất chưa sửa BE hoặc chạy test mới; trạng thái hiện hành theo [review Phase1](#phase1-review-20261109). Owner báo lần thử UI mới chỉ còn correct403 đã biết; ghi tiến độ FE đang phát triển, chưa suy thành lưu thành công trên BE hoặc chốt tạo Late.

**Căn cứ double-check:** Member.pdf trang4 §6.1–6.2/trang6 RP-08/RP-34/RP-36; analysis24/10 trang8 tách check-in cửa; MVP18/10 trang18 yêu cầu không mặc định Có mặt/không trùng/không trừ hai lần và đúng phân công. Đối chiếu D15/D17/D19 hiện hành, DTO/controller/service và các test liên quan. Giữ quyết định sau thay rule30phút; không dùng code hoặc Swagger cùng nguồn sinh làm expected độc lập. Bằng chứng đọc/source/giới hạn ở QA, không gán review tĩnh thành test PASS.

| Thứ tự / nhóm | Điều đã kiểm ra | Đề xuất công việc và expected cần duyệt | Điều kiện nghiệm thu local |
|---|---|---|---|
| **R1 — C04, lọc ngày lịch sử attendance** | Controller nhận startDate/endDate dạng date nhưng service so với checkInTime. Snapshot không mô tả nghĩa hai filter. Test search hiện chỉ kiểm list/state, chưa bảo vệ trường hợp ngày buổi có Absent/null check-in | Tái hiện bằng fixture của Coach A. Đề xuất attendance gắn Session lọc theo sessionDate; record lobby lịch sử không Session giữ checkInTime. Hai đầu ngày bao gồm trọn ngày; query chỉ đọc, không sửa state/data lịch sử. Nghĩa filter này là expected đề xuất, chưa giả là policy đã chốt | Ngày05/11 có cả Present và Absent/null timestamp đều được trả về khi thuộc Session05/11; ngày khác bị loại; bỏ filter và sessionId/memberId giữ đúng scope; có test biên ngày/null và tương thích lobby lịch sử nếu còn record |
| **R2 — C03, nhận xét kết quả sau buổi** | recordResult và recordBulkResults gọi requireWindow trước xử lý content và đồng bộ attendance. EvaluationRequest có Member/Coach/comment/score/date nhưng không sessionId, nên không tự coi evaluations thay thế mọi nội dung theo buổi | Tái hiện ca kết thúc07:30, Coach phụ trách bổ sung content08:00. Đề xuất tách mutation nội dung theo buổi khỏi mutation attendance: nội dung-only không nhận/đổi attendanceStatus, credit hoặc lịch. Giữ POST attendance trong giờ và correction quản lý sau buổi. Đặc tả method/DTO/error và test trước sửa; chỉ thêm API nếu model/route hiện có không biểu diễn được thao tác này một cách tách biệt | Coach A ghi nội dung đúng record/buổi phụ trách sau end; attendance đã chốt, credit/lịch và nội dung người khác không đổi; audit đúng actor; Coach B và giả coachId/memberId bị chặn. Trường hợp chưa có TrainingResult/attendance, buổi không tổ chức hoặc đổi Coach cần đối chiếu riêng: nếu thiếu quyết định, trình ví dụ rồi dừng, không tự tạo Present/Absent hoặc mở quyền lịch sử |
| **R3 — C05/C06/C08, scope Coach A/B cùng center** | Đã có local guards/SessionPrivacy/TrainingResultAccess/RbacGuard/PhaseWorkflow ở từng phạm vi; feedback hiện lọc Coach theo phiên, at-risk có endpoint me, Advanced list/decision có scope. Chưa có full matrix nghiệm thu Coach | Lập matrix method/path/role-authority/owner/error cho dữ liệu bảo vệ: roster, session, attendance/result, plan/evaluation, assigned members, feedback/at-risk, Advanced list/decision và book/reschedule. Reuse test đã có; bổ sung đúng ô thiếu bằng A/B trong cùng center. Tách danh mục dùng chung/công bố khỏi record riêng trước đặt expected | A làm đúng trên phần được giao; B không đọc/ghi phần riêng của A qua ID/filter/coachId hoặc extra authority, không đổi dữ liệu/audit sai. Case bị chặn phải có phiên hợp lệ và điều kiện nghiệp vụ hợp lệ để không nhầm lỗi thời gian/fixture với lỗi quyền. Luồng đổi phân công/lịch sử có gap policy thì ghi riêng, không mở gộp |

**Cách làm đã điều phối lại:** thực hiện R3 đã duyệt trước; R1 chưa đổi nghĩa filter hoặc setup buổi/cloud, R2 chưa thêm API. Khi nhóm được duyệt thì mỗi nhóm chốt expected/fixture, test đích, sửa phần đã có căn cứ rồi clean test theo AGENTS; PostgreSQL local riêng cho query/transaction cần kiểm. Không chạy hai Maven clean cùng target, không nạp.env cloud, không mutation Render/Supabase. Existing tests/guards center giữ nguyên, không thêm một lượt rà center khác. Sân đúng môn/TK05 và chính sách lịch sử chưa rõ không tự ghép sửa vào gói A/B này.

**Làm rõ R1/R2 theo câu hỏi mới của Owner:** R1 là chọn trường ngày dùng cho query: attendance có sessionDate05/11 nhưng checkInTime null sẽ bị loại nếu lọc bằng checkInTime. Không có ý chạy lại/reset DB; có thể tạo buổi/record test mới, và phải có ca Absent/null timestamp để tái hiện đúng điểm này, không chỉ Present. Chưa chạy setup cloud hoặc đổi ngày lịch thật. R2 ưu tiên rà xem evaluations hiện có đủ nhu cầu nhận xét chung không; thao tác content theo Session là trường hợp khác. Đề xuất chưa mở thêm API trước khi chứng minh nhu cầu; “trình riêng” nghĩa là đưa ví dụ chưa có expected cho Owner quyết định (nhận xét khi chưa có result, hoặc chuyển Coach A→B ai được sửa), không phải tạo một module mới.

**Tra riêng trạng thái Đi muộn/Late — 07/11:** theo yêu cầu Owner nhớ lại thiết kế, đã tìm trực tiếp các thuật ngữ Late/Excused/đi muộn/trễ trong Member.pdf, analysis24/10 và MVP18/10. Member.pdf §6.1/trang4 chỉ ghi Có mặt/Vắng; RP-21/trang6 là hủy trễ/no-show, không là trạng thái đi học muộn. MVP18/10 có từ trễ trong báo không đến/hủy/quyền học, không tìm thấy định nghĩa Late sau10phút trong những chỗ khớp. Lịch sử docs04 §33.1 ghi source cũ từng scan sớm30phút/Late sau10phút; §33.2/33.4 là các phương án chưa chốt; §33.5 ghi Owner chọn phương án1 chỉ Present/Absent trong giờ, giữ Late lịch sử. Lý do đã ghi: timestamp thao tác/scan không tự là giờ đến, chưa có nhu cầu chế tài/credit riêng cho Late; xác nhận có tham gia thực tế. Current SessionAttendancePolicy.state và correct.normalizeState cũng chỉ nhận Present/Absent. Payload thử attendance85/newState Late và POST403 không chứng minh Late đã lưu/hợp lệ: gate quyền Coach từ chối trước validation trạng thái. Chưa có yêu cầu đổi D15 và không hỏi lại lựa chọn đã chốt.

**Phiên đăng nhập — xếp kiểm FE chung sau:** đây là việc FE phản ứng khi BE từ chối phiên, không phải xác định repo FE và hiện chưa có bằng chứng login Coach hỏng. CurrentAccountFilter đã có local evidence. Khi đến đợt tích hợp:401 thì yêu cầu đăng nhập;403 SESSION_ROLE_CHANGED thì phiên cũ đã hủy và cần đăng nhập lại;403 ACCOUNT_NOT_ACTIVE thì hiển thị trạng thái tài khoản;403 nghiệp vụ Access Denied giữ lỗi thao tác, không mặc định logout mọi403; quyền đổi cùng role phải theo authorities mới. PASSWORD_CHANGE_REQUIRED theo luồng first-login riêng. Chỉ dùng account/role kiểm thử được phép, không thay quyền nhóm người dùng thật.

**Nguồn FE — một việc xác định bản code, giao FE xử lý song song:** cần biết repo/branch/commit hoặc nguồn gói build đã tạo ra scms-demo-04nov.pages.dev để sửa LAN-2 đúng bản. Trong repo BE có FE_Project-main nhưng AGENTS xác định là tham khảo cũ; metadata package/version không chứng minh nó là bản đang chạy. Phiên này không lấy được metadata deploy của Pages bằng HTTP tự động; không suy từ tên URL rằng biết ngày/commit build. FE chỉ cần cung cấp thông tin nguồn/build, không cần gửi secret/credential. Việc này không cản R1–R3 và không yêu cầu Owner cấu hình lại login/CORS lúc này.

**Đợt nghiệm thu FE/Render cuối:** sau các mục/luồng BE và phản hồi sửa UI, gom attendance first-write/QR/correction, CRUD plan/evaluation, các màn Coach và xử lý phiên. Các ca đã quan sát thành công được giữ, chỉ kiểm lại khi có diff/liên quan; phần chưa chạy giữ NOT_RUN, không coi hoãn kiểm là PASS. Chưa sửa scope hoặc quyền BE từ đề xuất này khi chưa có duyệt.

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
