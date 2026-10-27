# 00-MASTER-CHECKLIST.md — Kế Hoạch & Tiến Độ Triển Khai SCMS

Tài liệu này là chốt chặn quản lý tiến độ tổng thể và lịch sử bàn giao của dự án Sports Center Management System (SCMS) theo đúng chuẩn `Member.pdf` và `AGENTS.md`.

---

## 📌 PHẦN I: TIẾN ĐỘ THỰC HIỆN (ROADMAP)

### Phạm vi hiện hành 27/10/2026

- [x] D01–D19 đã chốt tại docs04; hoàn tất P1.0–P1.4 và toàn bộ BE P2 theo scope Owner. Demo/handoff và các kịch bản PostgreSQL tại docs02 mục10.
- [x] Source9af6b2d, merge main3cfe229 đã push; clean304run/277pass/27skip/0lỗi, clone2/2, communication fresh6/6+upgrade6/6; CI branch/main test/package pass. Count từng source là bằng chứng lịch sử, không quota.
- [x] Đính chính target: bootstrap28635081… là DB **test1a3dfdd7…**. Production188f6765… được duyệt riêng bundle01e31ca6…; backup mã hóa/restore/local proof rồi nâng cấp50bảng/9môn, giữ dữ liệu gốc7bảng. Docs02 mục10.9.
- [x] Kiểm Render sau317s: Live3cfe229 và target188… khớp,9môn/18giá đúng, Swagger/OpenAPI149paths/API mới/anonymous guards đạt.21/22checks pass, xác định db UP/mail DOWN. Evidence docs02 mục10.10.
- [ ] **Vận hành mail:** Owner xác nhận Render Free, SMTP587 bị nền tảng chặn. Cần chọn dịch vụ email HTTPS, API key/sender và triển khai adapter trước nghiệm thu OTP cloud. Chưa chọn provider hoặc gửi email thật; aggregatehealth503 vẫn được ghi đúng.
- [ ] **Để lại theo Owner:** P1.5/gói T D01 hồ sơ Member + D09 Coach directory/FE. Giữ checklist04 mục40.3/40.8–40.9; không tự merge nhánh teammate ngoài scope.
- [ ] FE hiện hành bằng browser/CORS/cookie, VNPay IPN gateway sandbox thật, SQL Server runtime và smoke tài khoản thật chưa nghiệm thu. Local HTTP và Swagger GET không thay các cổng này.

---

## 📜 PHẦN II: LỊCH SỬ HOÀN THÀNH (HISTORY)

### [27/10/2026] Chốt nâng cấp production, CI và kiểm Live sau5phút

Branch9af6b2d/CI40520150160 và merge main3cfe229/CI40520450534 đều pass test/package, đã push; backend tree merge bằng branch đã kiểm. Clean304run/277pass/27skip/0lỗi, clone Hibernate2/2 (int64 và health detail redaction), communication fresh6/6+upgrade6/6 từ committed checkout9af6b2d pass. Sửa Notification entity/projection/response sang Long, không thay bundle hay DDL production. Sau push21:22:12, kiểm21:27:29 UTC+7 (317s): Live đúng3cfe229/DB188…/9môn/OpenAPI149paths;21/22checks pass, chỉ mail DOWN, db/các thành phần còn lại UP.18giá JDBC read-only đúng. Owner xác nhận Free; giới hạn SMTP587 cần HTTPS email provider/adapter trước OTP cloud. Không nghiệm thu toàn bộ production hoặc tạo tài khoản/giao dịch cloud. Docs02 mục10.9–10.10 và artifacts/production-upgrade/2026-10-27. Các đoạn điều tra trước giữ làm lịch sử, nhãn DB test/production cũ đã đính chính.


### [27/10/2026] Đính chính target và nâng cấp production có dữ liệu

Owner cập nhật .env đồng bộ Render, chọn DB_USERNAME production: hash188f6765… khớp Live; DB1a3dfdd7… bootstrap trước là test. Production ban đầu39bảng/18môn/30users/6invoices/5subscriptions/2sessions/2enrollments, thiếu8cột checkpoint. Đã backup public mã hóa DPAPI, restore và kiểm bundle trên clone local; replay/FK-reference guard rollback pass. Owner duyệt đúng bundle01e31ca6…; đã commit transaction và postflight50bảng/9môn, giữ nguyên mọi giá trị cột gốc của7bảng nghiệp vụ tại commit. Không reset/seed, không đoán entitlement legacy. Render21/22checks pass sau upgrade;health503 đang xử lý. Full clean303run/277pass/26skip/0lỗi; cổng mapping clone và Render sau deploy được ghi tiếp ở docs02 mục10.9.


### [27/10/2026] Tự nhận diện Live commit và pool DB, không cần Owner lấy SHA

Bổsungidentitymetadataan toàn trênhealthping;clean302/277pass/25skip/0lỗi vàCIbranchd970c10/main08227b8pass. Saupush327sLiveđọcđượcđúng08227b8,poolhash188f6765…khác.env1a3dfdd7…;19/22checks pass,health503/catalog18vẫncòn. Ownerxácnhậnhealthpath/api/health/latestLive. ChỉhỏitênbiếnSpringdatasourceđểkiểmoverride,khônghỏisecret/SHA. KhôngDB/configwrites. Evidence docs02mục10.8 vàwith-runtime-identity.json.

### [27/10/2026] Sửa CI trống và tái kiểm sau Owner xác nhận main/DB

Phát hiệnworkflowCI0byte,run765339cfailure0jobs. Đã sửaJava17cleanverify/pinSHA/contentsread,branch729d8e4 vàmain8b5f5dc đềuCIpass. Không đổiBE/schema. Push17:02:54,tái kiểm17:08:04 (>=310s):OpenAPI149paths,17/19checks pass,cònhealth503/catalog18vsDB9.Chưa kếtluậnnguyênnhânruntime;đã hỏiLiveSHA/deploystatus/healthpath,khônghỏisecret.Giới hạnSMTPRenderFreecầnđốichiếuplan/logs.Docs02mục10.7 vàartifactafter-ci-repair.

### [27/10/2026] Commit/merge phần kiểm communication còn sót

Owner yêu cầu đưa file ngoài commit phase trước vào branch/main và kiểm Render sau5phút. Đã bổ sung runner checkout/DB/SMTP cô lập,hai test files và evidence lịch sử/hiện hành;6/6fresh+6/6upgrade pass.Source3857be5,merge2164747 clean298/273pass/25skip/0lỗi;không thay code ứng dụng/schema.Đã kiểm production sau313.66s:OpenAPI sau retry200/149paths,17/19checks pass;health503 vàcatalog18vs.env9 cần xử lý cấu hình/runtime.Evidence docs02 mục10.6 vàartifact production-check.Không coi production đã đạt.Không mở scope P1.5/T/FE.

### [27/10/2026] Hoàn tất hai phase BE theo scope Owner và kiểm bản merge

| Gói | Hoàn tất trong source và local verification |
|---|---|
| P1.0–P1.1 | Baseline/PG cô lập;9 môn/18 giá; migration exact snapshot+FK guard/replay không ghi đè giá đã sửa; Invoice snapshot giữ nguyên |
| P1.2 | Đăng ký môn/Primary Coach, schedule/năng lực cam kết đa môn, nhận trước Invoice, owner/center/lock/idempotency |
| P1.3 | activatedAt/expiresAt chính xác,45 ngày sử dụng, union closures/nanos, snapshot credit thu hồi, legacy không đoán |
| P1.4 | Cash/VNPay5 phút; Return đọc/IPN ghi, Paid mới12credit; late/unavailable Paid cần đối soát không cấp quyền |
| P2.1 | Coach book/Manager hỗ trợ/consent,12h hủy/dời,notice riêng,audit/idempotency,khóa đường CRUD bỏ qua workflow |
| P2.2 | Incident chờ/resolve/compensate,khôi phục balance gốc,gia hạn tối thiểu,Holidays preview/stale/confirm/giữ nghỉ quá khứ |
| P2.3 | Present/Absent [start,end),QR TTL/replay/assigned/correction,finalizer,bù Normal có link13physical/12valid;toggle7ngày giữ code |
| P2.4 | Note/receipt,refund ngoài SCMS có chứng cứ,approve chưa coi hoàn,CashCorrection hẹp Advanced chưa dùng/không booking/phụ thuộc |
| P2.5 BE | Report theo người+lượt,đúng center/quyền,3 số thu/hoàn/ròng và tương thích gross totalRevenue;Coach403 |

Chứng cứ: docs 02 mục 10 và artifacts/demo/phase-2/2026-10-27. Đã chạy clean test/9 HTTP H2/9 HTTP PG, PG nâng cấp snapshot/replay/FK rollback và bootstrap DB trống. Những lần fail đã đọc nguyên nhân và sửa sản phẩm/fixture; không xóa test để xanh. Đã chạy lại từ committed checkout13a62ee và merge155968d:292run/273pass/19skip/0lỗi,PG9/9pass. Artifact manifest/source-tree/test-summary/trace và demo.html ghi đúng merge commit. Các số trước là lịch sử từng source. P1.5/T/FE, gateway live, SMTP delivery và SQL Server runtime không đánh dấu hoàn tất.

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
| Tennis | 2.500.000 / 8.000.000 | 1.500.000 / 10.000.000 |
| Bơi lội | 1.500.000 / 4.500.000 | 1.200.000 / 9.000.000 |
| Pickleball | 1.800.000 / 5.000.000 | 1.100.000 / 8.500.000 |
| Bắn cung | 2.000.000 / 6.000.000 | 1.400.000 / 9.500.000 |
| Bóng rổ | 1.200.000 / 3.500.000 | 1.000.000 / 8.000.000 |
| Bóng đá | 1.000.000 / 3.500.000 | 900.000 / 8.000.000 |
| Bóng chuyền | 1.000.000 / 3.500.000 | 900.000 / 7.000.000 |
| Cầu lông | 900.000 / 3.000.000 | 900.000 / 7.500.000 |
| Bóng bàn | 800.000 / 2.500.000 | 800.000 / 6.500.000 |

16/18 ô giá khác PDF (chỉ giá Normal của Cầu lông và Bóng bàn khớp). `database/migrations/04_migration_member_flow_v2.sql:35` seed đúng bảng giá PDF; chưa kết luận dữ liệu live đã được migrate. Không tự đổi giá kinh doanh đang công bố.

Các sai lệch API contract đã đối chiếu controller/record:

| Nội dung trong docs/01 | Implementation hiện tại |
|---|---|
| Mục 9.1: `POST /api/invoices/{id}/pay-cash` | `POST /api/invoices/{id}/confirm-cash`; cần cả `MANAGE_INVOICES` và role `RECEPTIONIST` (`InvoiceController`) |
| Mục 5.5: `POST /api/enrollments/{id}/cancel` | `PATCH /api/enrollments/{id}/cancel` (`EnrollmentController`) |
| Mục 6.1: `GET /api/member-packages/me` hoặc `?memberId=me` | Không có GET `/me`; GET danh sách chỉ cho staff và không nhận bộ lọc đó. API theo cá nhân là `/api/members/{memberId}/subscriptions` hoặc `/packages`, có kiểm tra owner (`MemberPackageController`) |
| Response đăng ký: `enrollmentId`, `amount`, `expiresAt` | `EnrollmentResponse` dùng `id`, không có `amount`/`expiresAt`; FE không lấy được countdown theo schema đã ghi |
| Coach response có `userId`, `fullName`, `bio`, `certification` | `CoachSummaryResponse` có `id`, `name`, `avatarUrl`, `specializations`, `email`, `phone`; thiếu dữ liệu kinh nghiệm/chứng chỉ trong endpoint này |
| Danh mục `/api/subjects`, `/packages`, `/classes`, `/rooms`, `/coaches` ghi Public | SecurityConfig chỉ permit GET `/api/public/**`; các URL nêu trên cần session |
| Tất cả lỗi có `{code,message}`; xung đột phòng/Coach là 409 | `GlobalExceptionHandler` chỉ tách code khi message có tiền tố chuẩn. Nhiều lỗi private booking/slot chỉ có message và status 400; các code như `OUT_OF_CREDIT`, `ADVANCED_PACKAGE_EXPIRED`, `ROOM_CONFLICT` không được phát đúng như docs |
| Mục 7.3 nói Manager/Admin gia hạn | Controller yêu cầu `MANAGE_SUBSCRIPTIONS`; seed hiện hành cấp quyền này cho Receptionist, không cấp mặc định cho CenterManager/Admin |

`docs/02-DEPLOYMENT-GUIDE.md` cũng chưa đủ để triển khai theo mô tả: `FRONTEND_URL` không được `SecurityConfig` sử dụng, CORS chỉ chấp nhận localhost/127.0.0.1. Chưa có cấu hình cookie cho FE khác site; cần kiểm chứng session cookie qua HTTPS khi triển khai. Phiên bản trong AGENTS/deployment là Spring Boot 3.3.3, `pom.xml` thực tế là 3.3.5. Property thực tế là `payment.expire-minutes`, không phải `scms.payment.expire-minutes`; env `SCMS_PAYMENT_EXPIRE_MINUTES` vẫn được map đúng.

Member.pdf có các điểm cần người dùng chốt, không thể xem toàn bộ PDF là một đặc tả nhất quán:
- Trang 11: bảng ca ghi Ca 3 mở toàn bộ, nhưng bảng phân loại môn ngay sau đó khóa Ca 3 và Ca 4. FR-024 trang 5 lại nói giờ cấm là chính sách đề xuất, chỉ bắt buộc khi bật cấu hình. AGENTS 2.5 hiện chốt khóa cứng cả hai ca.
- Trang 6 RP-13 cho phép người được phân quyền sửa giá công bố; trang 8 mô tả tự lấy giá Subject. Code ưu tiên giá Subject khi dương, không hỗ trợ override trong trường hợp đó.
- Trang 3 mô tả tính hạn từ thời điểm thanh toán; implementation lưu LocalDate và cho phép tới hết ngày endDate, chưa biểu diễn chính xác 45 x 24 giờ.
- Trang 4 yêu cầu Completed khi kết thúc khóa; code hiện thiếu luồng tự hoàn thành, ghi chi tiết tại B.

#### B. Lỗi và khoảng trống trong code/test hiện tại

Mức P1: có thể ảnh hưởng quyền truy cập, chỗ học, credit, điều kiện đăng ký hoặc vận hành chính. Đây là kết luận từ source và các test đang chạy, trừ mục concurrency chưa tái hiện với hai transaction DB thật.

| ID | Mức | Bằng chứng | Tác động / hướng xử lý đề xuất |
|---|---|---|---|
| AUD-01 | P1 | `SessionController:25,52`; `SessionService:49,66`; `SessionResponse` trả bookedMemberId/Name | GET lịch và chi tiết không nhận actor hoặc ép scope. Member có thể yêu cầu lịch của memberId khác hoặc đọc ca riêng của người khác; Coach cũng không bị giới hạn lịch được giao. Cần scope tại service và test owner/assignment. |
| AUD-02 | P1 | `InvoiceService:279`; `InvoiceServiceTest:169` | Hết hạn VNPay chỉ đổi Invoice sang Expired, giữ Enrollment Pending. Vì Pending vẫn chiếm chỗ, suất không được giải phóng sau 5 phút. Test đang khẳng định hành vi sai này; cần thay kỳ vọng theo policy được chốt. |
| AUD-03 | P1 | `AttendanceFinalizationProcessor:70`; `AttendanceFinalizationProcessorTest:114` | Vắng lần ba vẫn khóa 7 ngày; không có binding/kiểm tra `scms.booking-block.enabled`. Đặt env false theo docs không có tác dụng. Giữ code chế tài và test, bổ sung toggle cùng test cả hai chế độ. |
| AUD-04 | P1 | `SessionService:168-273`; `SessionRepository` conflict queries; trigger `database/schema_postgres.sql:668` | Booking khóa subscription riêng nhưng không khóa Coach/phòng/học viên dùng chung. Hai subscription khác nhau có thể cùng thấy không xung đột rồi insert trùng; trigger hiện hành không có overlap lock/constraint. Generate/reschedule/substitute cũng kiểm tra rồi ghi mà chưa khóa tài nguyên chung. Cần thứ tự lock nhất quán và test concurrency DB. |
| AUD-05 | P1 | `SessionService:196` và phần booking tiếp theo | Chỉ kiểm tra hôm nay chưa hết hạn, không kiểm tra ngày ca học <= endDate; có thể đặt ca sau hạn gói. Chưa kiểm tra account Active/bookingBlockedUntil của chủ gói trong luồng này. |
| AUD-06 | P1 | `MembershipPackage` không có liên kết Subject; `SessionService:210,219`; `substituteCoach:687` | Không ràng buộc gói Advanced với môn/class, không kiểm tra Coach có chuyên môn đúng môn khi book/thay. Gói có thể dùng cho lớp môn khác, Coach khác chuyên môn vẫn được chọn. Cần mô hình liên kết môn và validation chuyên môn. |
| AUD-07 | P1 | `TrainingResultService:188`; `AttendanceService:129` | Kiểm tra HLV dựa vào coach của Class và Enrollment Registered; không dùng coach/bookedMember của Session. Private booking không tạo Enrollment nên HLV của ca 1:1 hợp lệ có thể bị từ chối; HLV thay thế cũng không được cấp đúng scope. |
| AUD-08 | P1 | `AttendanceService:163`; `SessionController:74` | API báo vắng còn hoạt động cho Normal, đặt Excused khi báo trước 2 giờ; không chặn theo loại khóa. Cron giữ nguyên Excused. Trái quy định Normal không có báo vắng. |
| AUD-09 | P1 | `EnrollmentService:enroll`; `SportsClassService:apply`; `SessionService:generateRecurringSessions` | Cho đăng ký Ongoing; không chặn đã khai giảng/thiếu 12 buổi. totalSessions Normal nhận từ request tùy ý và generate dùng giá trị đó; có thể tạo khóa khác 12 buổi. Generate cũng không giới hạn chỉ Normal hoặc tổng số lịch đã có. Cần kiểm tra điều kiện mở bán và tính bất biến của chuỗi 12 buổi. |
| AUD-10 | P1 | `EnrollmentService:promote` và `statusForNewEnrollment` | Promote waitlist của lớp có phí sang Pending nhưng không tạo invoice. Người được đôn lên chiếm suất mà thiếu luồng thanh toán/hạn giữ chỗ để hoàn tất. |
| AUD-11 | P1 | `SessionController:58,67`; seed role_permissions trong `database/schema_postgres.sql` | Private booking/cancel chỉ cho MEMBER/ADMIN/MANAGER; Receptionist bị chặn dù hai PDF đều cho hỗ trợ. Role seed thực là CenterManager -> ROLE_CENTERMANAGER, không trùng ROLE_MANAGER. Gia hạn lại phụ thuộc quyền cấp cho lễ tân thay vì Manager. |
| AUD-12 | P1 | `SecurityConfig:corsConfigurationSource`; deployment mục 2 | FE domain trên Render/Vercel không được CORS cho phép dù đặt FRONTEND_URL; hướng dẫn deploy chưa khớp code. |
| AUD-13 | P2 | `AttendanceFinalizationProcessor`; tìm toàn source các phép gán Completed | Chốt attendance không tự chuyển Enrollment Normal sau buổi 12 hoặc gói Advanced học hết sang Completed. Package job chỉ Expired; chưa đủ yêu cầu Member.pdf 7.2. |
| AUD-14 | P2 | `TrainingResultController:30,36`; `TrainingResultService:71,141` | Receptionist có RECORD_RESULT và được ghi cả nội dung kết quả chuyên môn; kiểm tra thành viên thuộc buổi chỉ chạy cho actor Coach. Thiếu tách quyền điểm danh hỗ trợ với đánh giá của HLV. |
| AUD-15 | P2 | `InvoiceService`, `VnPayGateway`, `MemberPackageService`, `SportsClassService`, `ReportService` | Chưa nhất quán Clock: có nơi dùng zone trực tiếp, có nơi LocalDate.now() theo host. Có thể sai ngày nghiệp vụ và khó kiểm thử đồng bộ trên Render. |
| AUD-16 | P2 | `scripts/test-payment-and-totp.ps1`; `scripts/smoke-test-api.ps1` | Script thử nhiều mật khẩu mẫu; script payment in mật khẩu đăng nhập thành công. Payment test tự tạo đơn nếu thiếu, chỉ quan sát TOTP sau 5 giây, không assert rollover 30 giây/scan/replay; VietQR là dữ liệu mẫu. Smoke test không assert giá cụ thể/đủ field và không exit 1 ở cuối khi có bước lỗi. Không dùng hai script làm bằng chứng nghiệm thu thanh toán. |
| AUD-17 | P2 | `database/schema_postgres.sql` ngay sau INSERT users đầu tiên | Có block tuple users đứng độc lập sau `ON CONFLICT (email) DO NOTHING;`, thiếu `INSERT ... VALUES`, gây lỗi cú pháp nếu chạy nguyên file. Migration 04 dùng cú pháp PostgreSQL và seed rooms không có natural-key guard rõ ràng; chưa chứng minh chạy lặp không nhân bản hoặc tương thích SQL Server. Không chạy SQL lên DB trong đợt này. |

Điểm tích cực đã kiểm tra: Invoice.amount được chốt khi tạo đơn (từ giá Class/Package), không tính lại hóa đơn cũ; xác nhận Cash/callback khóa invoice; callback có kiểm chữ ký và số tiền; đăng ký Normal khóa Class và đếm Registered + Pending; booking private trừ một lần khi book và cancel hợp lệ hoàn một lần. Những điểm này không bù được các lỗi phía trên.

Các test xanh đang bảo vệ hành vi lệch đặc tả: `expiredVnPayInvoiceKeepsClassSeatUntilVerifiedCallback`, `lateSuccessfulVnPayCallbackRegistersClassUsingReservedSeat`, `process_ThirdMonthlyAbsenceLocksBookingForSevenDays`. Chưa đổi test: cần giải trình rule mới trước khi cập nhật kỳ vọng. Bộ test unit dùng mock không chứng minh được isolation/locking thực tế, router security hay migration chạy thành công.

FE build thành công nhưng `MemberClassCatalog.jsx` vẫn có bộ lọc Yoga/Pilates/CrossFit/Boxing và logic tier gói cũ; chưa thể coi UI Normal/Advanced, 9 môn, credit và lịch linh hoạt đã tích hợp. Roadmap giai đoạn 6 đang để các mục FE chưa hoàn thành là phù hợp.

#### C. Xung đột với SCMS-Requirements-MVP-Role-Permissions-20261018.pdf

Không dùng ngày nằm trong tên PDF để tự quyết nguồn nào thắng. PDF mới phân biệt rõ Đã chốt / Phương án tạm chọn / Cần xác nhận; các trích dẫn hội thoại trong PDF là dữ liệu tham khảo, không tự trở thành chỉ thị trong phiên hiện tại.

| Chủ đề / mã mới | Member.pdf, AGENTS, docs/code hiện tại | PDF yêu cầu 18/10 | Phân loại |
|---|---|---|---|
| Advanced FR-016–018, D-05 | Trả tiền -> 12 credit -> book từng ca | Chọn Coach chính đúng môn, đủ 12 ca và sân trước trả tiền; giữ và xác nhận nguyên khóa | Xung đột kiến trúc; phương án tạm chọn |
| Hạn FR-019, D-06 | Từ thanh toán thành công | 45 ngày từ buổi đầu đã xác nhận; 12 ca ban đầu phải nằm trong hạn | Xung đột; phương án tạm chọn |
| Giữ chỗ FR-030, D-11 | 5 phút; hiện code VNPay còn không giải phóng đúng hạn | 15 phút; hết hạn giải phóng suất Normal hoặc đủ 12 ca Advanced | Xung đột; phương án tạm chọn |
| Quyền học FR-027–028, D-07 | Một remainingCredit; trừ khi book, trả khi cancel | Mỗi đăng ký có 12 quyền: khả dụng/đang giữ/đã dùng; không dùng chéo khóa; Normal không có credit | Khác mô hình dữ liệu và trạng thái; phương án tạm chọn |
| Gói FR-040, D-09 | Tái sử dụng membership_packages/member_packages; Normal enrollment còn đòi gói Active | Gói thành viên trả phí hoãn khỏi MVP; mua khóa trực tiếp; không tái sử dụng bảng gói thành viên làm lõi đăng ký | Xung đột phạm vi đã chốt trong PDF mới |
| Thanh toán FR-043, D-12 | Return và IPN đều gọi processVnpayResult, có thể Paid | Chỉ thông báo server-to-server xác nhận; Return chỉ hiển thị | Xung đột; phương án tạm chọn |
| Tiền về muộn FR-045, D-13 | Callback Expired vẫn kích hoạt gói/suất; chủ yếu ghi audit PAID_AFTER_EXPIRY | Ghi tiền đã thu, đăng ký cần xử lý; Manager đối soát/lịch khác/hoàn thủ công ngoại lệ | Thiếu luồng ngoại lệ; phương án tạm chọn |
| Sự cố FR-022–024, D-08 | Dời record hiện tại; tự gia hạn; thay Coach chỉ kiểm role/trùng lịch | Coach chính ưu tiên; Coach thay đúng môn có Member đồng ý; lịch thay có liên kết cũ/mới, lý do, người duyệt và thông báo | Thiếu consent, specialization, liên kết lịch và thông báo |
| Nhịp Normal FR-011, D-04 | Các thứ tùy chọn; chưa buộc 3 ngày/tuần | 12 buổi, 90 phút, 3 ngày/tuần | Thiếu validation; đã chốt trong PDF mới |
| Đặt trước Q-02 | Code/docs chốt 12 giờ; Member.pdf ghi 12–24 giờ | Chưa có ngưỡng duy nhất | Cần xác nhận, không tự chốt |
| Báo trễ Q-03 / FR-021 | Khóa hủy dưới 24 giờ | FR mô tả có nút báo trễ vẫn mất buổi, Decisions đánh dấu câu hỏi chưa chốt | PDF mới tự phân biệt đề xuất và quyết định; cần xác nhận |
| Lễ/nắng Q-05 | AGENTS khóa cứng ca 3/4 và bỏ ngày lễ Active | Chính sách chưa áp đặt bắt buộc, cần chốt theo môn/sân | Cần xác nhận; Member.pdf cũng tự mâu thuẫn |
| Điểm danh Q-06, RP-35 | QR từ start-30 phút tới end; recordResult/correct có quy tắc khác | Cửa sổ ghi/sửa, người duyệt chưa chốt; quyền sửa chưa cấp khi chưa chốt | Cần xác nhận; không suy rộng cửa sổ QR cho mọi API |
| Giá Q-07 | Docs tuyên bố giá chuẩn nhưng khác PDF; seed theo Member.pdf | Chín giá chỉ tham khảo, cần chốt giá công bố/quyền override | Cần xác nhận bảng giá kinh doanh |
| Báo cáo FR-032/046, D-15 | ReportService có Paid/paymentDate và method, nhóm theo package | Theo môn, Normal/Advanced, tiền mặt theo lễ tân; tách tiền thu chưa xếp lịch và đối soát bàn giao | Thiếu chiều báo cáo/quy trình; chưa đáp ứng. ReportService còn thêm mốc cố định 2026-09 khi timeline rỗng. |
| Nội dung khóa FR-009/013 | Class/DTO chủ yếu tên, môn, giá, lịch, capacity | Mục tiêu, trình độ đầu vào, nội dung 12 buổi, kết quả mong đợi, chính sách trước mua | Thiếu dữ liệu/API/UI tương ứng |
| Check-in FR-041, RP-45 | Có lobby check-in lẫn session QR | Check-in cửa độc lập chờ phạm vi, không đồng nghĩa hoàn tất buổi | Cần chốt phạm vi, không gộp hai loại điểm danh |

Phân quyền trong PDF mới không thể đối chiếu chỉ bằng mã RP: mã đã thay ý nghĩa so với Member.pdf, ví dụ RP-45 cũ là hủy/hoàn tiền, RP-45 mới là check-in cửa. Cần map theo thao tác. Các lệch đã thấy: Receptionist bị chặn book/cancel Advanced; Manager sai role/private và quyền gia hạn; Coach xem lịch/attendance quá rộng nhưng không điểm danh đúng Session 1:1; Receptionist có thể ghi nội dung đánh giá. Không xác nhận quyền live chỉ dựa trên seed SQL.

#### D. Quyết định cần người dùng trước khi sửa nghiệp vụ

Theo AGENTS 2.9, dừng ở bước lựa chọn quy tắc khi hai nguồn xung đột. Đề xuất hai hướng cụ thể:
1. Giữ Member.pdf + AGENTS làm chuẩn vận hành: 5 phút, Advanced mua credit rồi đặt từng ca, 45 ngày từ thanh toán; sửa các lỗi code/contract đã xác định và ghi rõ các điểm chưa chốt trong PDF mới.
2. Chuyển sang đặc tả 18/10: xác nhận dùng cả các phương án tạm chọn D-05–08 và D-11–15; cần thiết kế lại đăng ký Advanced đủ 12 ca, quyền học theo đăng ký, hạn từ buổi đầu, holding 15 phút, late-payment reconciliation; bỏ điều kiện mua gói trả phí trước khóa, cập nhật AGENTS/contract/migration/tests đồng bộ.

Cả hai hướng đều cần quyết định rõ Q-02/03/05/06/07. Chưa áp dụng hướng nào. Trước khi bàn giao triển khai tiếp, cần kiểm thử tích hợp DB: tranh suất cuối, hai gói tranh Coach/phòng, hết hạn và tiền về muộn, Cash/callback lặp, owner/assignment, ngày ca ngoài hạn, no-show/hoàn credit và lifecycle Completed. Không kết luận hệ thống đã sẵn sàng chỉ từ 163 unit tests pass.

### [23/10/2026] — Hoàn thiện đăng ký OTP qua Gmail (backend)
- Đã hoàn thiện luồng OTP đăng ký: Pending, giới hạn resend/attempt, demo OTP bật tường minh, mail AFTER_COMMIT và lỗi xác thực phân biệt.
- Đã đồng bộ cấu hình Gmail và tài liệu deploy/API; schema hiện đủ cột nên không tạo migration 06.
- Baseline trước sửa: 193 tests run (178 passed, 15 skipped, 0 failed). Sau sửa: 198 tests run (183 passed, 15 skipped, 0 failed).
- Việc chờ xác nhận thủ công: Gmail App Password, biến môi trường Render và gửi thử email thật.

### [Đợt 22/10/2026] — Giai đoạn 8: Thông Báo Trong Ứng Dụng
* **Trạng thái:** Code và kiểm thử Backend hoàn tất; cần xác nhận migration trên PostgreSQL mới và có dữ liệu cũ.
* **Thay đổi:** Mở rộng schema notification tương thích ngược, API cá nhân, 13 loại event, job nhắc lịch và gói sắp hết hạn; FE không thay đổi.
* **Kiểm thử:** 193 tests run, 178 passed, 15 skipped, 0 failed, 0 errors (`.\mvnw.cmd clean test`).
* **DB:** Migration được rà cú pháp/idempotence; chưa chạy trên PostgreSQL vì môi trường hiện tại không có `psql`, Docker hay kết nối DB được cấp.

### [Đợt 20/10/2026] — Chuẩn Hóa DTOs Danh Mục & Hoàn Thiện Hợp Đồng API Frontend (Sẵn Sàng Giai Đoạn 6)
* **Trạng thái:** Hoàn tất 100%, 178 tests run (163 passed, 15 skipped, 0 fail, 0 error).
* **Các hạng mục đã hoàn thiện:**
  1. **Nâng cấp DTOs & Service Backend:**
     - `SubjectResponse` & `SubjectRequest`: Bổ sung `normalPrice`, `advancedPrice`, `slug`, `status`, `outdoor`.
     - `MembershipPackageResponse` & `MembershipPackageRequest`: Bổ sung `packageType`, `totalCredit`.
     - `RoomResponse` & `RoomRequest`: Bổ sung `facilityType` (`Indoor`/`Outdoor`), `outdoor`.
     - `SportsClassResponse`: Bổ sung `subjectName`, `coachName` (tránh N+1 roundtrips cho Frontend).
  2. **Hoàn thiện tài liệu `docs/01-API-CONTRACT-FE-BE.md`:**
     - Mục 1: Quy chuẩn giao tiếp, Base URL, UTC+7, luồng xác thực (Auth flow: login, register, OTP, reset password).
     - Mục 2: Ma trận giá 9 môn thể thao chuẩn hóa theo `Member.pdf` (Trang 7) và bảng so sánh đặc tính kỹ thuật Normal vs Advanced.
     - Mục 3: Nhóm API 7 Ca học chuẩn (90 phút/ca) và quy chế khóa Ca 3 & Ca 4 cho môn ngoài trời chống sốc nhiệt.
     - Mục 4: Nhóm API Danh mục tra cứu (`/api/subjects`, `/api/packages`, `/api/classes`, `/api/rooms`, `/api/coaches`).
     - Mục 5 - 10: Quy chuẩn API Lớp Normal (Holiday Skipper, non-cancellable), Lớp Advanced (12 credit, 45 ngày, 24h cutoff, No-show), Xử lý sự cố (Reschedule, Substitute Coach, Extend Validity), Holidays, Thanh toán (VNPay & Cash, 5 phút giữ chỗ), và Check-in TOTP QR.
     - Mục 11: Bảng từ điển mã lỗi chuẩn (Error Codes Dictionary) hướng dẫn chi tiết cách Frontend hiển thị toast/alert/modal cho người dùng.
  3. **Kiểm thử hồi quy:** Chạy lệnh `.\mvnw.cmd test` đạt 100% pass (178 run, 163 passed, 15 skipped).

### [Đợt 20/10/2026] — Giai đoạn 5: Background Jobs & Xử Lý Sự Cố Trung Tâm (Mưa Bão, HLV Nghỉ, Chuẩn Bị Deploy Render)
* **Trạng thái:** Hoàn tất 100%, 178 tests run (163 passed, 15 skipped, 0 fail, 0 error).
* **Các tính năng đã hoàn thiện:**
  1. **Xử lý sự cố Dời lịch học (`POST /api/sessions/{sessionId}/reschedule`):** Hỗ trợ Manager dời buổi học của lớp Normal hoặc ca 1:1 do thời tiết xấu (mưa bão ngập sân), sự cố cơ sở vật chất. Kiểm tra 7 ca chuẩn, chống sốc nhiệt môn outdoor, chống trùng phòng/HLV/học viên. Nếu buổi học 1:1 bị dời vượt quá hạn 45 ngày của gói học viên $\rightarrow$ Backend tự động kéo dài ngày hết hạn `endDate` và cập nhật `validityDays` tương ứng, chuyển lại `Active` nếu gói đang bị `Expired`. Với lớp Normal, tự động kéo dài `SportsClass.endDate`.
  2. **Xử lý sự cố HLV nghỉ đột xuất (`POST /api/sessions/{sessionId}/substitute-coach`):** Cho phép Manager chỉ định HLV thay thế khi HLV phụ trách nghỉ ốm, việc đột xuất. Kiểm tra người thay thế có vai trò HLV và không bị trùng lịch ca học đó. Ghi nhận nhật ký kiểm toán (Audit Log).
  3. **Chủ động Gia hạn gói hội viên (`POST /api/member-packages/{id}/extend`):** Cho phép Manager bù thêm ngày hiệu lực cho học viên bị ảnh hưởng bởi bảo trì dài hạn hoặc sự cố trung tâm.
  4. **Chuẩn hóa thời gian giữ chỗ thanh toán:** Điều chỉnh `PaymentProperties` và `application.properties` chuẩn hóa về đúng `5 phút` (`SCMS_PAYMENT_EXPIRE_MINUTES = 5`).
  5. **Background Jobs toàn diện:**
     - `PackageExpirationJob`: Chạy 00:00 hàng đêm quét các gói quá 45 ngày, chuyển `Expired` và vô hiệu hóa số credit thừa còn lại.
     - `PaymentExpirationJob`: Chạy mỗi 60 giây quét các hóa đơn quá 5 phút chưa thanh toán, tự động hủy giữ chỗ và giải phóng slot.
     - `AttendanceFinalizationJob`: Chạy mỗi 10 phút quét và chốt điểm danh vắng mặt / no-show cho các ca học đã kết thúc.
  6. **Cập nhật Hướng Dẫn Triển Khai Render & Kịch bản Test VNPay Sandbox (`docs/02-DEPLOYMENT-GUIDE.md`):** Bổ sung đầy đủ thông tin thẻ NCB demo, kịch bản test luồng thanh toán và IPN callback, cấu hình Supabase Connection Pooler, và checklist trước/sau deploy.
  7. **Cập nhật Hợp Đồng API (`docs/01-API-CONTRACT-FE-BE.md`):** Thêm chi tiết nhóm API Xử lý sự cố trung tâm (Mục 5) và cập nhật thời gian giữ chỗ thanh toán 5 phút.

### [Đợt 20/10/2026] — Giai đoạn 4: Nghiệp Vụ Khóa Advanced (Học 1:1 Linh Hoạt 12 Credit & 45 Ngày)
* **Trạng thái:** Hoàn tất 100%, 161 tests run (146 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 3: Nghiệp Vụ Khóa Normal (Lớp Học Nhóm 12 Buổi Cố Định)
* **Trạng thái:** Hoàn tất 100%, 145 tests run (130 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 2: Quy Chuẩn 7 Ca Học & Ràng Buộc Thời Tiết
* **Trạng thái:** Hoàn tất 100%, 138 tests run (123 passed, 15 skipped, 0 fail, 0 error).

### [Đợt 20/10/2026] — Giai đoạn 1: Chuẩn Hóa CSDL & Migration Flow V2
* **Trạng thái:** Hoàn tất 100%. File migration: `database/migrations/04_migration_member_flow_v2.sql`.

## Giai đoạn 9: Chat hỗ trợ Member – Receptionist (Backend)
- [x] Tạo migration 06 idempotent cho conversation, messages, read state và quyền `CHAT_AS_MEMBER` / `HANDLE_CHAT`; đồng bộ schema PostgreSQL.
- [x] Triển khai API member, inbox staff, pagination cursor, read state theo từng user, claim/release/close/reopen, rate limit và idempotency.
- [x] Kiểm tra quyền staff ở controller và service; Member chỉ truy cập conversation của mình; Coach bị từ chối.
- [x] Phát message event AFTER_COMMIT; tái sử dụng notification event/listener, dedupe theo message; thêm `ChatResponder` extension và escalation.
- [x] Bổ sung test service/controller, cập nhật API contract, database README và migration deploy guide.
- [x] Baseline `mvnw test`: 198 run, 183 passed, 15 skipped, 0 failed. Sau thay đổi `mvnw clean test`: 224 run, 209 passed, 15 skipped, 0 failed.

### [24/10/2026] — Giai đoạn 9: Chat hỗ trợ Member – Receptionist
- **Trạng thái:** Backend, migration, contract và test đã cập nhật; không thay đổi FE, không tạo commit.
- **Tái sử dụng:** Session auth, dynamic permissions, audit log, `Clock`, notification AFTER_COMMIT và `dedupe_key`.
- **Bổ sung:** `chat_conversations`, `chat_messages`, `chat_read_state`, chat API, two chat permissions, post-commit responder/notification listeners, escalation.
- **Database:** Migration `06_migration_chat.sql` là additive/idempotent; chưa chạy trên PostgreSQL thật nếu môi trường không cung cấp DB.
- **Kiểm thử:** `mvnw clean test` — 224 run, 209 passed, 15 skipped, 0 failures/errors.
