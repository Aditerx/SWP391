# SCMS — Kịch bản demo FE15phút và tham chiếu BE

Dành cho BE và FE; phần demo15phút cập nhật12/11/2026, phần Phase1 ngày10/11 giữ làm tham chiếu. **Các đề xuất màn hình, nút và phần FE chuẩn bị đều để FE duyệt; quyết định cuối thuộc FE.** Contract, quyền và điều kiện nghiệp vụ do BE kiểm độc lập giao diện.

<a id="demo-fe-15min-20261112"></a>
## Demo FE15phút12/11 — nội dung đã duyệt, chưa rehearsal nghiệp vụ

Owner dự kiến demo hôm nay12/11/2026 khoảng17:45–18:15 VN; tổng thời lượng15phút gồm hỏi đáp, chọn11phút trình diễn +4phút hỏi đáp. Giữ BE/FE đang chạy, chưa tiếp tục Session3 hoặc bàn giao/tích hợp Session1–2. Site mới [VibeSports](https://vibesports.pages.dev); cấu hình/kết nối và giới hạn tool xem [docs02](docs/02-DEPLOYMENT-GUIDE.md#current-frontend), bằng chứng tại Active/QA. Định hướng demo đã duyệt; bản checklist F1–F5 mới theo yêu cầu tách file đang chờ Owner duyệt. Các case, nút sau login và giao dịch vẫn cần rehearsal trước khi dùng live.

Owner đã đồng ý các phân tích/đề xuất/điều chỉnh. Việc chấp thuận định hướng không thay cho đối chiếu UI/fixture và rehearsal; các bước chưa kiểm vẫn giữ nguyên trạng thái đó. Prompt tiếp tục gửi trong chat, tài liệu chỉ giữ kịch bản và trạng thái.

### A. Khả năng hỗ trợ đã xác định

- Playwright MCP đã kết nối tab VibeSports và đọc/chụp `/login`, không gửi form hoặc sửa dữ liệu. Quan sát được “Đăng nhập”, “Đăng ký hội viên”, “Quên mật khẩu?”; snapshot có nhãn “Đăng nhập bằng Google” trong iframe nhưng screenshot chưa hiển thị rõ nút. Chưa kết luận lỗi FE hoặc xác nhận hiển thị/đăng nhập Google đạt; chưa mở form đăng ký hoặc kiểm bước tiếp theo. HTTP403/1010 và CUA unavailable trước đó không còn là bằng chứng thiếu tab hiện tại.
- Tool hiện chỉ quan sát/điều hướng; **chưa đăng nhập hoặc kiểm nghiệp vụ có phiên trên browser FE**. Việc thấy nút Google không chứng minh onboarding thành công; OTP→Active không mặc định tự tạo phiên. Nhãn “Đăng ký hội viên” trên FE là điểm vào tài khoản, không là căn cứ khôi phục gói hội viên chung đã retire.
- Những tên màn sau login trong kịch bản/sơ đồ là chức năng nghiệp vụ, chưa phải nhãn UI đã xác minh. Khi cần thao tác, dùng nút thực đã rehearsal; không đoán từ FE_Project-main. Agent chỉ đọc tab trong lượt này; các thao tác ghi dưới đây dành cho người trình diễn sau khi đủ điều kiện.
- Owner chọn VNPay sandbox có kết quả dự phòng. Chưa chạy giao dịch sandbox, Google thật, gửi OTP, chat AI/staff hoặc mutation DB trong lượt chuẩn bị. Việc chuẩn bị không mở quyền provider/reset/seed/deploy; chọn đúng môi trường/test workflow trước rehearsal ghi.
- Runtime schema đã có `isNewAccount` và chat close; local HEAD chưa có phần Google onboarding mới từ remote main. Không cần merge chỉ để chứng minh feature runtime đã có; chưa biết SHA deploy. Preview conflict docs00/docs01 chưa phải merge hoặc conflict đã giải quyết.

### B. Checklist thao tác F1–F5 — bản riêng để Owner duyệt

Danh sách hành động và lời dẫn đã chuyển sang **[note-script-demo.md](note-script-demo.md)** theo yêu cầu Owner. Đây là bản thao tác duy nhất; không giữ lại bảng/lời dẫn trùng ở đây. Bản mới trình theo F1→F2→F3→F4→F5, giữ11phút trình diễn +4phút hỏi đáp; F2 tạo đơn VNPay ở bước cuối để F3 thanh toán ngay, Refund chỉ trình bày một lần trong F3 bằng hồ sơ chuẩn bị sẵn. Cấu trúc mới chờ Owner duyệt, chưa rehearsal nghiệp vụ.

### C. F1/F3 đã xuất ảnh v2; F2/F4/F5 giữ góp ý

Đã đọc [F1 gốc](image/F1.jpg), [F2](image/F2.jpg), [F3 gốc](image/F3.jpg), [F4](image/F4.jpg), [F5](image/F5.jpg). Hai bản dùng trình bày là **[F1-v2.png](image/F1-v2.png)** và **[F3-v3.png](image/F3-v3.png)**; giữ nguyên JPG gốc và F3-v2 để đối chiếu. Đây là sơ đồ nghiệp vụ đã review asset, chưa chứng nhận các màn sau login đều tồn tại trên FE hiện hành.

| Ảnh | Điểm cần chỉnh/kiểm |
|---|---|
| F1-v2 — tổng thể/Member | Đã đổi tiêu đề **Flow 1: Account, Access and Member Profile Management**; giữ bốn swimlane, palette, icon màn, bo góc, legend và outcome xanh/đỏ. Bỏ gói chung/Freeze Request; Courses & Learning Entitlements / Learning History dùng hộp xám nét đứt dẫn flow học tập. |
| F1-v2 — Google/OTP | Existing Google tới dashboard; tài khoản mới qua Complete Profile. Verify OTP hợp lệ→Account Active→Sign in; không có Auto sign-in. Luồng Google/onboarding/OTP trên FE chưa kiểm trong lượt này. |
| F1-v2 — quầy/Manager/Admin | Tạo Member cấp temporary password và handoff sang Member đổi mật khẩu lần đăng nhập đầu. Manager giới hạn own center, Classes/Course Administration là cross-flow F2; Admin login/dashboard riêng và required permissions, không có quyền toàn bộ mặc định. Change Password trong ảnh là chức năng theo contract, nhãn/màn sau login còn cần kiểm FE. |
| F2 | Tách publish/Open khỏi xác nhận khai giảng/Ongoing; dời/đổi lớp cần đúng điều kiện và consent. Lướt ngày nghỉ/sự cố, chọn một nhánh chính; không vừa tạo Holiday diện rộng vừa sửa lịch hàng loạt trong demo ngắn. |
| F3-v3 — thanh toán/Cash | Giữ VNPay SANDBOX; Return chỉ navigation tới Payment Result & Invoice, sau đó BE xác minh IPN/QueryDR và FE đọc lại Invoice/quyền. Connector đã được kiểm lại để đi qua bước xác minh trước outcome. Chỉ Valid Paid + eligible mới Registered/Active; chưa xác minh/cần đối soát không cấp quyền. Normal quầy thu Cash và Paid/Registered cùng transaction; Advanced tách Confirm Cash của Pending hợp lệ. |
| F3-v3 — Refund/Reports | Có nguồn hồ sơ từ F2/tiền đến muộn sau hủy, phân biệt dữ liệu Invoice với tài khoản nhận do Member cung cấp. Đánh số0→1→2→3→4→5: phát sinh→Member cung cấp người nhận→Manager xác minh/duyệt→Receptionist nhận việc→ghi giao dịch/chứng từ→Manager đối soát. “Bank transfer outside SCMS” nằm trên mũi tên3→4; chỉ bước5 tới Completed/NeedsReview, không có bypass. Giữ Reports, Invoice Paid/amount gốc và ba swimlane. |
| F4 | Check-in desk không tự đồng nghĩa hoàn tất attendance/result; chọn đúng luồng hiện có. Body Metrics và nhãn “create plan” phải kiểm UI/API hiện hành trước đưa vào lời giới thiệu; chưa dùng như feature đã nghiệm thu chỉ vì có hộp trong hình. |
| F5 | Nói “chuyển sang người hỗ trợ/Receptionist nhận hội thoại”, không hứa chọn tùy ý người nhận. Claim/release/escalate/close phải khớp bản đang deploy; giữ lịch sử cùng hội thoại. |

Đã tạo ảnh thực bằng imagegen, mở lại từng PNG để kiểm chữ/ký hiệu, connector, vai/state, tỷ lệ và độ rõ. F3-v3 dùng hộp màu cho SCMS screen, hộp xám đứt cho external/other-flow step; nét liền navigation, nét đứt action/handoff. Đã sửa các connector sai trong nháp v3 trước khi lưu bản bàn giao; không có đường từ người nhận thẳng tới ghi chứng từ hoặc từ Return thẳng tới Paid. Bằng chứng asset/giới hạn ở Active/QA. Sơ đồ là diễn giải quyết định/contract đã chốt; không dùng review ảnh làm expected độc lập hoặc thay nghiệm thu browser/BE.

<a id="f3-refund-clarification-20261112"></a>
**Làm rõ Refund Normal — Owner đã yêu cầu sinh lại, F3-v3 đã xuất và review:** ảnh v2 giữ đúng ba vai nhưng thiếu điểm phát sinh hồ sơ, phần chuyển tiền ngoài SCMS mới là callout nên dễ bị đọc thành nhiều lần kiểm duyệt. Bản [F3-v3](image/F3-v3.png) đã thể hiện các điểm dưới đây; giữ v2 để đối chiếu.

1. Thêm đầu vào chéo từ F2: trung tâm hủy lớp trước khai giảng hoặc Member chọn Refund khi không nhận đề xuất dời/chuyển; thêm chú thích tiền đến muộn sau khi lớp đã hủy. BE tạo nghĩa vụ hoàn từ Invoice Paid thật, gắn đúng Member/lớp/amount; không phải Manager nhập tùy ý một khoản hoàn.
2. Tách dữ liệu có sẵn của hồ sơ (Member/Invoice/lớp/amount) với **tài khoản nhận tiền do Member cung cấp riêng** (ngân hàng/số tài khoản/tên người nhận); không vẽ tự trích tài khoản ngân hàng từ profile.
3. Ghi rõ luồng: **Member cung cấp người nhận → Manager xác minh người nhận và duyệt → Receptionist nhận việc → chuyển khoản ngân hàng ngoài SCMS → Receptionist ghi nhận giao dịch/nộp chứng từ → Manager đối soát → Completed**. Receptionist không duyệt hoàn lần hai; bước Manager cuối xác nhận kết quả thực hiện, khác bước cho phép hoàn ban đầu.
4. Trong ảnh, tách hộp Receptionist thành “Claim Approved Refund” và “Record Transfer & Proof”; đặt “Bank transfer outside SCMS” trên mũi tên hành động giữa hai hộp. Manager cuối là “Reconcile Bank Result”; chỉ verified success + proof tới xanh Completed, chưa rõ tới NeedsReview. Completed không là nút thực hiện chuyển tiền; giữ original Invoice Paid/amount và Reports.

Notification liên quan Refund/dời/hủy là **feed trên website**, không tự gửi mail. Email OTP và email cấp mật khẩu tạm là các luồng EmailService riêng; chưa kiểm delivery mới trong lượt rà này. Căn cứ: D10, docs01 Notification/Refund Normal, NotificationEventListener/NotificationService, EmailService và NormalChangeService/NormalRefundService; đây là review tĩnh source, không là nghiệm thu FE/cloud.

### D. Dữ liệu/phiên cần chuẩn bị và cửa giờ

1. Mỗi vai một browser/profile có cookie độc lập, màu/nhãn vai dễ nhìn. Nhiều tab cùng profile vẫn dùng chung session; không giả chúng là năm người đăng nhập độc lập. Trước rehearsal lấy auth-me, center, authority và ID thật từng vai.
2. Dùng tối đa hai Member chính: một người đăng ký/thanh toán, một người có hồ sơ Refund hoặc attendance đã chuẩn bị; chọn thêm người chỉ khi fixture đòi hỏi. Không ép một lớp vừa đăng ký phải ngay lập tức đi đủ dời/hủy/hoàn/học trong11phút.
3. Đơn Pending tạo tại thời điểm demo do hold5phút. Chuẩn bị lớp còn chỗ/còn cutoff, một Invoice Paid/Enrollment hợp lệ làm dự phòng và kết quả payment-result đã chụp/rehearsal. Không preseed Pending rồi để hết hạn, không ghi Paid bằng Return/querystring hoặc sửa DB.
4. Điểm danh phải có Session trong giờ thực, lớp/quyền học/phân công hợp lệ. Ca6 là16:30–18:00; ca7 là18:15–19:45. Demo17:45 có thể đặt F4 trước18:00 nếu fixture runtime đạt. Nếu bắt đầu sau18:00 và trước18:15 thì dùng lịch sử; không hứa mutation attendance hoặc đổi Clock để vượt cửa giờ.
5. Chọn hồ sơ Refund ở gần bước muốn trình bày và giữ chứng từ DEMO/nguồn đối soát độc lập đã chuẩn bị; dữ liệu tài khoản ngân hàng mô phỏng không phải chuyển tiền thật. Dời khai giảng dùng lớp/cutoff/minimum/lần dời đúng runtime, không lấy mặc định Nxx/ID từ ngày10/11 làm dữ liệu còn dùng được hôm nay.
6. Email OTP mẫu chỉ để minh họa giao diện/nội dung; mã cũ không dùng xác minh. Owner tự mở hộp thư, che nội dung riêng. Google/AI/VNPay sandbox phụ thuộc provider, phải có fallback không chờ vô hạn. Bộ NCB thành công được công bố tại [VNPAY — thẻ test](https://sandbox.vnpayment.vn/apis/vnpay-demo/) và đã đưa vào note private; đọc được nội dung qua kết quả tìm kiếm chính thức, lần mở trực tiếp trang timeout. Chưa thanh toán/rehearsal bộ test trên site trong lượt này.
7. Note để điền và copy: [demo-copy-paste.private.md](artifacts/runs/demo-15min/2026-11-12/demo-copy-paste.private.md), local/Git-ignore; dành cho người trình diễn, chỉ giữ tài khoản, tên lớp/hồ sơ, văn bản mẫu, dữ liệu sandbox và phương án dự phòng. [Kiểm ID/phiên/version/build](docs/02-DEPLOYMENT-GUIDE.md#demo-tech-check-20261112) là việc của agent/BE, không giao Owner một bảng biến kỹ thuật. Không đưa file đã điền secret vào Git, gói bàn giao hoặc màn trình chiếu.

SYSTEM-DATA/bundle hiện tại chỉ là DATA DESIGN đã kiểm ở lần riêng; chưa đối chiếu cloud sau thao tác. Lượt này **chưa sinh lại schema/fixture** vì chưa chứng minh thiếu dữ liệu. Nếu readback/ảnh cho thấy không còn case phù hợp, chuẩn bị staging theo [rule anchor](docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules), đúng ngày cài và giờ demo rồi kiểm local; không tự chạy cloud. Giữ build đang chạy là ưu tiên, không reset chỉ vì qua ngày.

### E. Hỏi đáp nên chuẩn bị

| Câu hỏi | Ý trả lời ngắn cần chứng minh |
|---|---|
| Tại sao không chỉ ẩn nút theo role? | BE kiểm actor/quyền/owner/center/phân công; UI hỗ trợ, request trái quyền vẫn bị chặn. |
| Quay lại từ VNPay có coi là trả tiền chưa? | Return điều hướng; BE xác minh giao dịch và FE GET trạng thái chứng từ/quyền, không tin URL báo thành công. |
| Refund Approved khác Completed thế nào? | Approved là cho phép xử lý; cần chứng từ và đối soát thành công mới Completed; Invoice gốc/amount giữ để đối chiếu. |
| Hai người thao tác trùng thì sao? | Transaction/lock/idempotency theo luồng; không nhân đơn/quyền/attempt do retry. Chỉ nêu phạm vi đã có bằng chứng, không hứa toàn hệ thống. |
| Vì sao năm vai? | Member học/đọc own; Coach chuyên môn/phân công; Receptionist vận hành; Manager quản trị center/tài chính; Admin quản trị tài khoản/quyền theo authority. |

**Bước tiếp:** tab login đã đọc được; chọn fixture/phiên đúng runtime, đối chiếu các màn sau login và rehearsal một lượt trong phạm vi được mở. Chỉ ghi READY khi có case/readback/dự phòng thực và thời lượng đạt. Ảnh v2 và nội dung trình diễn đã hoàn tất phạm vi tài liệu; authenticated browser/business demo vẫn NOT_RUN.

## 0. Hiểu bộ demo trước khi chạy

**SYSTEM-DATA.md là sổ tra cứu dữ liệu demo cho BE/FE:** nối mã tài khoản/lớp/hồ sơ trong SCRIPT với ID, lịch, trạng thái và quan hệ dữ liệu mẫu. Dùng file đó để chuẩn bị đầu vào và đối chiếu kết quả; dùng SCRIPT này để biết ai làm gì, theo thứ tự nào và kết quả cần kiểm. SYSTEM-DATA được sinh từ fixture local nguyên trạng, không phản ánh tự động DB cloud sau khi có người thao tác.

### Bundle là gì, vì sao có và dùng file nào?

Schema bundle là **một file SQL tổng hợp cấu trúc SCMS, các thay đổi schema đã tích hợp, guard và dữ liệu demo tương thích với nhau**. Nó giúp lớp/lịch/quyền/Invoice/Refund cùng thuộc một bộ đã kiểm, tránh tạo dữ liệu bằng các file rời khác phiên bản. Nó không phải bản backup hoặc một schema riêng cho từng lớp Nxx.

| Thành phần | Mục đích và cách dùng |
|---|---|
| [SCMS-SCRIPT-DEMO-BE.md](SCMS-SCRIPT-DEMO-BE.md) | Diễn tiến demo: actor, precondition, thao tác, phản hồi và kiểm sau reload |
| [SYSTEM-DATA.md](SYSTEM-DATA.md) | Tra cứu mã→ID và dữ liệu mẫu; xem [bảng N01–N12](SYSTEM-DATA.md#normal-class-index), [tài khoản](SYSTEM-DATA.md#demo-actor-index), [proposal/Refund](SYSTEM-DATA.md#demo-financial-index) |
| [database/schema_postgres.sql](database/schema_postgres.sql) | Bundle canonical để chạy **nguyên file** trên đúng Supabase demo, sau khi kiểm đúng ngày/bản và quyền Owner; dựng lại dữ liệu SCMS trong phạm vi đã nêu |
| [database/bootstrap/scms_demo_scenarios.sql](database/bootstrap/scms_demo_scenarios.sql) | Fixture do compiler sinh, được ghép vào bundle; không chạy thêm riêng sau schema bundle |
| `demo-plan.json` trong staging | Lịch đầu/cutoff/cửa xác nhận của từng lớp và đánh giá giờ demo nếu có; không tự chạy case hoặc giữ chỗ |
| `verification.json` trong staging; [Active/QA](docs/00-MASTER-CHECKLIST.md#active) | Phân biệt sinh file, kiểm SQL, HTTP, cloud và browser. Kết quả chuẩn nằm ở Active/QA, không suy PASS từ việc có file |

Thư mục staging chỉ chứa bản chuẩn bị; công cụ không tự thay canonical. Chỉ copy chạy `database/schema_postgres.sql` sau khi BE xác nhận file đó đã nhận đúng bundle đã kiểm cho ngày cài. Chạy lại bundle sẽ mất các thao tác demo đã phát sinh; xem kết quả hiện có không đòi hỏi reset.

### Anchor và hai thông tin thời gian

Lịch mẫu cần có ngày gốc để sinh các buổi quá khứ/tương lai, cutoff, hạn quyền và lịch sử thu/hoàn nhất quán. Ngày gốc đó là **anchor — chính là ngày dự kiến cài bundle**, không phải một field ngày thứ ba.

| Thông tin | Vai trò | Có cần nhập khi chuẩn bị? |
|---|---|---|
| Ngày thực thi SQL dự kiến, `--anchor YYYY-MM-DD` | Ngày cài theo Asia/Ho_Chi_Minh; dữ liệu được sinh từ mốc này. Guard so với ngày VN thực khi thực thi SQL | Bắt buộc, không tự lấy ngày cũ hoặc âm thầm đổi ngày |
| Giờ demo dự kiến, `--demo-at` có offset | Chỉ đánh giá case có nằm trong cửa thời gian không; có thể sau ngày cài. Không đặt Clock, giờ nhận tiền, performedAt hoặc hẹn hệ thống chạy | Tùy chọn; chưa biết giờ vẫn sinh bundle và các mốc lịch/cutoff |
| Thời điểm thực thi thực tế | DB/runtime ghi nhận khi SQL/nghiệp vụ thật sự chạy; ví dụ captured_at của audit và thời điểm thao tác | Không nhập thay bằng giờ dự kiến |

Khi không có `--demo-at`, plan ghi plannedDemoAt=null, availability=NOT_EVALUATED; mọi executionStatus vẫn NOT_RUN. Khi có giờ, AVAILABLE chỉ đúng cửa thời gian, chưa kiểm actor/state/version/chỗ. Thay giờ demo không tự yêu cầu cài lại DB; có thể lập lại kế hoạch rồi đọc runtime trước thao tác.

Ví dụ anchor11/11 khiến N02 có buổi đầu13/11 06:00 VN, cửa xác nhận trễ là **[12/11 06:00, 13/11 06:00)**. Chuẩn bị SQL ngày11/11 vẫn hợp lệ, nhưng demo ngay ngày11/11 chưa thể chứng minh case xác nhận trễ. Không dùng giờ dự kiến để giả rằng hệ thống đã tới mốc12/11.

`SCMS_DEMO_STALE_ANCHOR` nghĩa là đang thực thi bundle dành cho ngày cài khác ngày VN thực. Guard chặn trước CREATE/DROP; phải sinh/kiểm lại cả bộ cho đúng ngày, không sửa một literal hoặc bỏ guard. DB đã cài không tự phát lỗi này chỉ vì sang ngày hôm sau, nhưng từng case vẫn chịu giờ/state thực. Metadata anchor_date/business_timezone cho biết ngày cài/timezone; fixture_version và migration_key mô tả phiên bản, không thay ngày cài.

### Quá trình tạo và bàn giao bundle

1. Chọn ngày cài theo VN; có giờ demo thì nhập thêm, chưa có thì bỏ `--demo-at`.
2. Compiler dựng design→fixture→schema từ cùng anchor, ghép cấu trúc/migration/guard; tạo staging và plan, chưa chạm Supabase.
3. Kiểm cú pháp, lịch/quan hệ và tái sinh; kiểm SQL fresh/replay/rollback/counter/ngoài phạm vi trên DB local mới khi đúng ngày cài. Anchor tương lai có thể sinh để review nhưng SQL chưa chạy phải giữ NOT_RUN.
4. Xuất/check SYSTEM-DATA từ fixture local nguyên trạng cùng anchor/version **trước test HTTP ghi dữ liệu**. Bảng tra cứu và bảng chi tiết được sinh từ cùng các dòng, không chép ID theo phỏng đoán.
5. Kiểm HTTP liên quan bằng bản sao source hiện hành không.env, response cùng trạng thái sau commit; ghi rõ case ngoài cửa hoặc chưa kiểm. SQL PASS không thay HTTP/browser PASS.
6. Sau review/check, đưa đúng schema/fixture và DATA DESIGN vào canonical, cập nhật Active/QA. Xác nhận file/ngày/bản sẵn sàng rồi mới thực thi cloud theo phạm vi Owner.
7. Sau lần dựng lại được phép: khởi động lại BE/phiên, đăng nhập mới/auth-me, lấy ID thực rồi kiểm các luồng. Mỗi bước ghi tiêu thụ fixture; trạng thái sau chạy không còn là bộ nguyên trạng trong SYSTEM-DATA.

Chi tiết lệnh, điều kiện local và phạm vi xóa/tạo chỉ giữ tại [rule anchor/bundle](docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules) và [DB README](database/README.md). Các gợi ý chuẩn bị/trình bày cho FE trong quá trình phối hợp này vẫn để FE duyệt.

## 1. Mục tiêu và kết quả hiện tại

Buổi demo cần cho thấy: thu học phí Normal đúng tại quầy; đủ điều kiện mới xác nhận khai giảng; Member tự chọn phương án chuyển lớp; trung tâm xử lý nghĩa vụ hoàn theo đúng vai trò; tải lại vẫn giữ kết quả và retry không nhân giao dịch/quyền.

BE đã có lượt smoke bằng HTTP trực tiếp trên Render và đọc lại Supabase sau commit. Đây không phải thao tác nút trên Swagger hoặc nghiệm thu browser FE. Sau bất kỳ lần reset nào, ID/state/cookie và kết quả smoke trước đó chỉ còn là lịch sử; phải lấy phiên mới và đọc lại runtime trước khi chọn case. Kết quả, build, actor, fixture và giới hạn hiện hành chỉ tra [Active/QA](docs/00-MASTER-CHECKLIST.md#active). SYSTEM-DATA vẫn là DATA DESIGN, không phải dump Supabase.

Hai cách dùng tài liệu:

- **Xem kết quả đang có:** đọc API trên DB hiện hành để xác nhận Invoice, quyền, lịch và Refund thực sự còn ở trạng thái nào; không mặc định trạng thái sau smoke cũ vẫn tồn tại. Bước đã hoàn tất không được coi là vừa chạy lại thành công.
- **Chạy luồng ghi từ đầu:** xác minh fixture hiện tại còn nguyên trạng, đúng ID/state/chỗ và cửa thời gian. Chỉ chuẩn bị/dựng lại khi fixture không còn phù hợp và Owner chọn reset; không bắt reset chỉ vì qua ngày mới. [Quy trình anchor và bundle](docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules) là bước chuẩn bị riêng; tài liệu này không tự mở quyền reset hoặc ghi cloud.

Phạm vi bản đầu là Normal/Lifecycle/Refund. Advanced, attendance ghi trong buổi, provider thanh toán và toàn bộ CRUD không được suy là đã nghiệm thu từ các case dưới đây.

## 2. Các gợi ý chuẩn bị để FE duyệt

FE có thể chọn cách bố trí các thao tác sau, không cần dùng đúng tên màn hình/nút trong tài liệu:

| Công việc đề xuất | Dữ liệu/hành vi cần trao đổi với BE |
|---|---|
| Đăng nhập và đổi vai | Session JSESSIONID; credentials include/withCredentials; xác minh `/api/auth/me` mỗi lần đổi actor |
| Thu Cash tại quầy | Chọn Member/lớp; BE trả Paid+Registered ngay; đọc lại Invoice/ghi danh |
| Xác nhận khai giảng | Hiển thị sĩ số Paid, minimum, ngày buổi đầu và trạng thái; gửi reason khi cần |
| Phương án chuyển lớp | Hiển thị lịch snapshot, giá/lớp gốc–đích và lựa chọn của chính Member; giữ version/fingerprint |
| Hồ sơ Refund | Hiển thị đúng state và thao tác theo role; phân biệt duyệt, upload và Completed |
| Chứng từ riêng tư | Upload file DEMO ≤1MiB đúng PNG/JPEG/PDF; download bằng phiên tài chính, không public URL |
| Đối chiếu sau lưu | Tải lại dữ liệu BE sau ghi và sau reload; báo cáo thu, hoàn, ròng theo kỳ |

Trước buổi chạy, thống nhất URL/build FE, BE FE thật sự gọi, người thao tác từng vai và case sẽ tiêu thụ. Mỗi vai có phiên riêng; nếu dùng chung browser, logout/login và auth-me khi đổi vai. Mở nhiều tab cùng profile không tạo các session độc lập.

Chuẩn bị chứng từ và một bản ghi đối soát **DEMO độc lập với nội dung vừa nhập vào form**: reference, amount, receiverId và performedAt. Đó là dữ liệu mô phỏng đã chuẩn bị để so khớp, không phải xác nhận đã chuyển khoản thật. Không cần VNPay/QueryDR/mail/AI/Google/provider thật cho kịch bản này.

## 3. Tài khoản, lớp và dữ liệu đối chiếu

Nxx là mã lớp Normal; Mxx/CM1/R1/PICK-A là mã tài khoản fixture; RFxx là mã hồ sơ hoàn, EV-Nxx là mã sự kiện mẫu. **DEMO-01–DEMO-10 là mã case thao tác; D01–D19 vẫn là mã quyết định nghiệp vụ trong docs04.** R1 trong bảng tài khoản là Receptionist, khác hạng mục review R1/R2 của P2.2.

Cột ID chỉ là mốc đối chiếu bộ 10/11; FE lấy ID thật từ API sau mỗi lần dựng lại, không hardcode Invoice/proposal/attempt/document ID. Tên/email/ID hiện có trong fixture local tra [bảng tài khoản SYSTEM-DATA](SYSTEM-DATA.md#demo-actor-index); mật khẩu theo kênh riêng Owner bàn giao, không chép vào file này.

Ví dụ N05 trong bộ10/11 là `Pickleball Normal N05`, classId14; khi đã kiểm đúng runtime thì gọi `/api/classes/14/...`, không gửi `/api/classes/N05/...` hoặc mặc định ID bằng5. RF02 tương ứng refundId3 của bộ này, không mặc định bằng2. Những chỗ `{N01}`, `{M14}`, `{proposal}` bên dưới là chỗ điền ID thật vừa đọc, không phải chuỗi gửi nguyên vào URL/body.

| Mã/vai | ID đối chiếu bộ 10/11 | Dùng để làm gì |
|---|---:|---|
| ADM — Admin | 1 | Đọc role catalog; không dùng thay Manager tài chính |
| CM1 — CenterManager | 32 | Xác nhận khai giảng, duyệt receiver/Refund và đối soát |
| R1 — Receptionist | 30 | Thu Cash; claim/nộp metadata và proof Refund đã duyệt |
| BAD-A — Coach | 26 | Xem roster N01 trong phân công |
| PICK-A/PICK-B — Coach | 16/17 | Đối chiếu phân công N05/N11 |
| M14/M15 — Member | 37/38 | Hai người mới vào N01 bằng Cash |
| M01 — Member | 2 | Accept chuyển N05→N11 |
| M02 — Member | 3 | Kiểm owner, chỉ đọc Refund của mình |
| M03 — Member | 4 | Thử Accept sau khi N11 đầy; vẫn Waiting |
| M04 — Member | 5 | Người nhận Refund RF02 của N06 |

<a id="normal-scenario-map"></a>
### N01–N12 có ý nghĩa gì trong buổi demo?

Đây là mục đích của từng lớp trong bộ nguyên trạng. Mapping classId/Coach/sân/lịch/sĩ số được sinh tại [SYSTEM-DATA](SYSTEM-DATA.md#normal-class-index); proposal/Refund tra [bảng hồ sơ](SYSTEM-DATA.md#demo-financial-index). Cùng môn vẫn có nhiều lớp vì chúng đại diện các tình huống khác nhau.

| Mã | Lớp | Mục đích/case |
|---|---|---|
| N01 | Cầu lông | Thu Cash, đủ sĩ số rồi xác nhận — DEMO-02/03 |
| N02 | Tennis | Cutoff đăng ký, xác nhận trễ có reason — DEMO-04/05 |
| N03 | Bơi lội | Thiếu minimum, bị chặn xác nhận — DEMO-06 |
| N04 | Bắn cung | Đã dời một lần, chặn dời lần hai — bổ sung nhóm lifecycle |
| N05 | Pickleball | Lớp nguồn đang đề nghị chuyển — DEMO-08 |
| N06 | Bóng rổ | TT đã hủy, có nghĩa vụ hoàn — DEMO-09 |
| N07 | Bóng đá | Gần đầy; chuẩn bị case chỗ cuối/waitlist, ngoài luồng chính đã kiểm |
| N08 | Bóng chuyền | Ongoing, chặn hủy theo luồng trước khai giảng — DEMO-07 |
| N09 | Bóng bàn | Closed, xem lịch sử/kết quả; không thay kiểm job đóng lớp — DEMO-01/10 |
| N10 | Cầu lông | Sự cố TT và buổi bù, xem nguồn/lịch sử — DEMO-01/10 |
| N11 | Pickleball | Lớp đích của N05, ban đầu còn một chỗ — DEMO-08 |
| N12 | Cầu lông | TT đã hủy, các trạng thái Refund để đối chiếu — DEMO-01 |

| Fixture mới nguyên trạng | Điều kiện dùng cho kịch bản |
|---|---|
| N01 — Cầu lông | Open; Paid Registered1; minimum3/max4; còn trước cutoff48h |
| N02 — Tennis | Open; Paid Registered3/minimum3; cửa xác nhận trễ là `[buổi đầu−24h, buổi đầu)` |
| N03 — Bơi lội | Open; Paid Registered6/minimum7; dùng kiểm thiếu sĩ số |
| N04 — Bắn cung | Đã dời một lần; dùng kiểm chặn dời lần hai |
| N05/N11 — Pickleball | Cùng giá1.800.000đ/center; N05 có proposal, M01/M03 Waiting; N11 Registered3/max4 |
| N08 — Bóng chuyền | Ongoing; dùng kiểm chặn hủy trước khai giảng |
| N06/RF02 | N06 Cancelled; nghĩa vụ của M04 AwaitingReceiver, amount1.200.000đ |

Ngày/giờ thực tế lấy từ `demo-plan.json` của bundle và API hiện hành. `AVAILABLE` trong plan chỉ đánh giá cửa thời gian; chưa kiểm state/chỗ/quyền runtime và không là PASS.

**DB sau smoke hiện không còn nguyên trạng:** hai Cash N01 đã hoàn tất, N01/N02 đã xác nhận; M01 đã chuyển, N11 đủ4/4, M03 vẫn Waiting và N05 còn changePending; RF02 đã Completed DEMO. Xem ID/chứng từ cụ thể ở Active/QA. Không chạy lại DEMO-02/DEMO-05/DEMO-08/DEMO-09 bên dưới rồi ghi PASS mới chỉ vì API trả kết quả đã lưu.

## 4. Kịch bản chính theo thứ tự

Mỗi nhóm: auth-me đúng actor → kiểm ID/state/version/chỗ/giờ → thao tác → đọc lại → reload giao diện nếu FE đã tích hợp. Nếu fixture khác bảng trên, dừng phần phụ thuộc và ghi BLOCKED hoặc chuyển sang xem kết quả có sẵn.

<a id="demo-01"></a>
### DEMO-01 — Đăng nhập và kiểm phạm vi đọc

1. Đăng nhập lần lượt các vai bằng POST `/api/auth/login`; GET `/api/auth/me` cùng cookie phải đúng userId/role/authorities. Kết quả này xác nhận phiên, chưa xác nhận nghiệp vụ khác.
2. Admin/Manager đọc danh mục qua GET `/api/subjects`, `/api/rooms`, `/api/classes` theo quyền. Member xem lịch riêng bằng GET `/api/sessions`, hóa đơn riêng bằng GET `/api/members/me/invoices`; lịch Normal trước mua qua GET `/api/sessions/catalog/{classId}`.
3. Coach đọc GET `/api/classes/{classId}/enrollments` ở lớp mình được giao; lớp khác phải403. GET `/api/roles`, `/api/permissions`, `/api/roles/{id}` chỉ Admin.
4. Member đọc GET `/api/normal-refunds` và chỉ thấy hồ sơ của mình. Manager đọc các state trong center; Receptionist chỉ công việc Approved trở đi. Proof chỉ staff tài chính; history chỉ Manager tài chính.

Kết quả đúng: dữ liệu phù hợp actor; filter/ID của Member khác không mở thêm quyền. 401 chưa login không được tính là đã kiểm đúng quyền owner.

<a id="demo-02"></a>
### DEMO-02 — Receptionist thu Cash cho hai Member vào N01

**Trước ghi:** N01 Open, Registered1/min3/max4; chưa confirm; M14/M15 chưa có đơn/ghi danh N01; còn trước giờ buổi đầu−48h. Hai chỗ sẽ bị tiêu thụ.

1. Manager thử POST `/api/classes/{N01}/confirm-opening`: phải400 `NORMAL_INSUFFICIENT_PAID`.
2. R1 POST `/api/classes/{N01}/enroll`, body `memberId=<M14>`, `paymentMethod:Cash`. BE tự lấy giá900.000đ; kết quả201 có `status:Registered`, `paymentStatus:Paid`, `invoiceId`, `amount`.
3. Gửi lại cùng thao tác:400 `ALREADY_ENROLLED`; đọc lại chỉ một Invoice/ghi danh cho M14. Không gọi confirm-cash thêm sau checkout này.
4. Làm tương tự M15. Member đăng nhập xem Invoice riêng; R1/Manager đọc lại roster. N01 đạt Paid Registered3/min3.

Gợi ý FE: có thể trình bày một lượt thu và một lượt retry để người xem hiểu chống trùng; FE quyết định cách thể hiện. Member/Manager chọn Cash online phải403, không được sinh đơn giữ Cash.

<a id="demo-03"></a>
### DEMO-03 — Manager xác nhận khai giảng N01

**Phụ thuộc:** DEMO-02 hoàn tất; lớp/Coach/phòng/lịch vẫn hợp lệ và chưa tới buổi đầu.

CM1 POST `/api/classes/{N01}/confirm-opening`; nên dùng reason có nhãn DEMO để ghi rõ ngữ cảnh, bắt buộc trong cửa trễ24h. Thành công200 lưu `openingConfirmedAt`, `openingConfirmedBy`; lớp vẫn Open cho tới khi job kiểm đủ điều kiện tại buổi đầu. Retry không tạo quyết định mới. GET Class và reload phải giữ người/thời điểm/version đã lưu; timestamp so theo độ chính xác DB, không yêu cầu chuỗi nanosecond response đầu giống hệt chuỗi đã lưu microsecond.

<a id="demo-04-07"></a>
### DEMO-04–DEMO-07 — Lifecycle và các cửa thời gian

| Case | Hành động | Kết quả đúng |
|---|---|---|
| <a id="demo-04"></a>DEMO-04 — N02 hết đăng ký | R1 enroll N02 sau cutoff48h nhưng trước buổi đầu, dùng M14 chưa đăng ký N02 | 400 `NORMAL_REGISTRATION_CLOSED`; không có Invoice/ghi danh mới |
| <a id="demo-05"></a>DEMO-05 — N02 xác nhận trễ | Trong cửa24h, CM1 confirm thiếu reason rồi gửi reason DEMO hợp lệ | Thiếu reason400 `REASON_REQUIRED`; đủ điều kiện/reason200; lưu actor/time; chưa tự Ongoing |
| <a id="demo-06"></a>DEMO-06 — N03 thiếu sĩ số | CM1 confirm trước buổi đầu, có reason DEMO nếu đang trễ | 400 `NORMAL_INSUFFICIENT_PAID`; giữ Open, chưa confirm |
| <a id="demo-07"></a>DEMO-07 — N08 Ongoing | CM1 POST `/api/classes/{N08}/cancel-before-opening` với version mới/reason DEMO | 400 `NORMAL_PRESTART_REQUIRED`; không hủy lịch/quyền hoặc mở Refund mới |
| Bổ sung N04 | CM1 POST `/api/classes/{N04}/normal-proposals`, kind Postpone/shiftDays/version/reason | Chặn dời lần hai bằng `NORMAL_POSTPONEMENT_LIMIT` nếu các precondition còn đúng |

Case ngoài cửa thực tế ghi NOT_RUN. Không đổi Clock, ngày lịch hoặc DB state để trình diễn. Có thể dùng N09 Closed để **xem lịch sử tổng hợp**, không gọi đó là vừa chạy job đóng lớp. Quan sát auto-start/close cần đúng thời gian và bằng chứng riêng.

<a id="demo-08"></a>
### DEMO-08 — Member chọn chuyển N05→N11

**Trước ghi:** M01 có quyền N05/Paid hợp lệ, proposal Transfer Pending, N11 còn1chỗ và còn hạn nhận; M03 cũng Waiting. Chọn rõ Invoice nguồn của M01 để giữ đối chiếu.

1. M01 GET `/api/classes/{N05}/normal-proposals`: xem schedule snapshot, target, `version`, `fingerprint` và choice của mình. Preview không giữ chỗ; N11 vẫn3/4.
2. M01 POST `/api/normal-proposals/{proposal}/respond`, gửi version/fingerprint vừa đọc và `choice:Accept`.
3. Sau200, đọc lại enrollment/lịch/Invoice: nguồn Cancelled, đích Registered; Invoice `classId`/`className`/`amount` giữ lớp mua N05, `entitlementClassId` là N11. Không tạo đơn mới ở lớp đích.
4. Gửi lại cùng lựa chọn: không nhân transfer/quyền/Invoice. M01 reload thấy lịch theo N11.
5. M03 dùng proposal của mình thử Accept khi N11 đã4/4:409 `CLASS_FULL`, vẫn Waiting. Không chọn Refund thay Member. Manager apply khi còn Waiting bị `MEMBER_RESPONSES_PENDING`.

Sau nhóm này, N11 đã hết chỗ và N05 còn phương án chưa giải quyết. Nếu cần tiếp tục xử lý M03, phải lập bước demo riêng với lựa chọn do người đóng vai Member thực hiện rõ ràng; không tự hủy N05 để dọn kết quả.

<a id="demo-09"></a>
### DEMO-09 — Hoàn học phí N06 theo workflow DEMO

**Trước ghi:** chọn RF02 của M04 bằng GET Refund của chính Member, đối chiếu Invoice N06/amount1.200.000đ, state AwaitingReceiver; chưa có bank attempt. Đây là nghĩa vụ đã tồn tại do TT hủy, không phải Member tự hủy lớp đang vận hành.

| Bước/actor | API trong `/api/normal-refunds/{refundId}` | Kết quả đúng và phần cần kiểm lại |
|---|---|---|
| 1. M04 nhập người nhận | POST `/receiver`: bank/accountNumber/accountName/version | Dùng dữ liệu DEMO; AwaitingApproval, receiverId mới; Member chỉ thấy accountNumber che đầu, contact không lộ |
| 2. CM1 xác minh | POST `/verify-receiver`: version/source DEMO | receiverVerified=true; chưa là duyệt/chuyển tiền |
| 3. CM1 duyệt | POST `/approve`: version hiện tại | Approved; dueAt/firstDueAt là hết ngày TT mở thứ7 từ ngày mở tiếp theo, gồm T7/CN trừ Holiday |
| 4. R1 nhận việc | POST `/claim`: version/requestId UUID chuẩn | Processing và một attempt; giữ UUID khi retry, nhận lại attempt cũ |
| 5. R1 nộp metadata | POST `/attempts/{attempt}/submit`: reference DEMO/amount/VND/performedAt | Submitted/AwaitingVerification; amount đúng khoản duyệt, performedAt từ claim tới thời gian hiện tại |
| 6. R1 upload proof | POST `/attempts/{attempt}/documents`, multipart `file` DEMO | Trả documentId; upload vẫn chưa Completed; cùng bytes không sinh document khác |
| 7. CM1 đối soát | POST `/attempts/{attempt}/verify`: version/outcome/bankSource/note và reference/amount/receiverId/performedAt | Chỉ Success khớp nguồn đối soát DEMO độc lập và có proof mới Completed; không chuyển tiền thật |
| 8. Đọc lại | GET detail/history và `/documents/{document}` theo quyền | Một attempt/kết quả đối soát; Invoice vẫn Paid/amount gốc, enrollment vẫn Cancelled; proof không public |

Sau mỗi bước tải GET mới để lấy version, không tăng version ở FE bằng phép cộng suy đoán. Không POST complete cũ làm fallback. Nếu chọn outcome Pending/Failed/Reversed, kết quả sẽ đi nhánh khác và giữ lịch sử; lập case riêng, không tiếp tục đánh dấu Completed theo bảng Success.

Gợi ý FE: tách rõ “được duyệt”, “đã nộp chứng từ”, “đã đối soát hoàn tất”; FE duyệt cách hiển thị và thao tác tương ứng. Mọi nội dung mô phỏng cần ghi DEMO; không dùng chứng từ hoặc thông báo như bằng chứng tiền thật.

<a id="demo-10"></a>
### DEMO-10 — Reload và đối chiếu sau ghi

Member xem lại Invoice/ghi danh/lịch/Refund đúng chủ; Coach xem roster theo phân công; Manager GET `/api/reports/revenue` với kỳ startDate/endDate/groupBy đã thống nhất. Tiền thu theo ngày nhận Invoice, tiền hoàn chỉ Refund Completed theo performedAt; Approved/upload/NeedsReview không bị tính như đã hoàn. Chuyển lớp không tạo thêm khoản thu.

Đối chiếu audit/thông báo đúng actor/center/người nhận sau commit. Reload browser chỉ PASS khi đã thao tác trên FE thật, thấy lưu thành công rồi tải lại. HTTP API hoặc preflight CORS200 không thay browser PASS.

## 5. Các case chặn cần giữ

- Member/Manager Cash online403; retry enrollment đã Registered không tạo đơn/suất khác.
- Member hỏi dữ liệu người khác403 hoặc404 che tài nguyên; Coach roster sai phân công403; role catalog ngoài Admin403.
- Proposal/version/fingerprint stale409: đọc lại, không tự ép Accept; lớp đầy không chuyển quyền.
- Refund chưa verify receiver không approve; có claim chưa đối soát không đổi receiver/claim lần khác để chuyển lại.
- Metadata/đối soát sai amount/reference/receiver/time không Completed; upload riêng không Completed.
- Member/Coach/Admin không tải proof; Receptionist không đọc history Manager.

Các case này kiểm điều kiện cụ thể sau auth-me; không thay đổi role/quyền, giá, ngày hoặc expected để demo thành công.

## 6. Sinh lại bundle và dùng theo ngày

Ngày anchor là **ngày thực thi SQL cài bộ demo**, không phải cấu hình Clock và không bắt DB reset mỗi ngày. `SCMS_DEMO_STALE_ANCHOR` yêu cầu sinh/kiểm lại bộ dành cho đúng ngày cài; không bỏ guard hoặc sửa một literal ngày trong SQL. Ngày/giờ dự kiến demo có thể sau ngày cài, nhưng mọi case phải kiểm lại precondition runtime.

Dùng [rule và lệnh trong docs02](docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules). Công cụ tạo `schema_postgres.sql`, `scms_demo_scenarios.sql`, `demo-plan.json`, `verification.json` ở staging riêng; không tự thay file canonical hoặc chạy cloud. Kiểm local SQL và HTTP là các lớp bằng chứng riêng.

Nếu plan báo OUTSIDE_WINDOW, hẹn lại đúng giờ hoặc ghi NOT_RUN; không kéo TTL/chỉnh Clock. Nếu state đã bị người khác tiêu thụ, đọc lại và chọn fixture hợp lệ; không dùng ID/cookie/pay URL trước lần reset.

Nếu chưa có giờ demo, có thể bỏ `--demo-at`: vẫn nhận đầy đủ ngày lớp/cutoff, availability=NOT_EVALUATED và executionStatus=NOT_RUN. Có thể bổ sung giờ sau để tính plan; không sửa thời gian thực hoặc dữ liệu tài chính để khớp kế hoạch.

Khi Owner thực thi một bundle đã được xác nhận sẵn sàng: xác minh đúng Supabase demo, ngừng BE/job đang ghi theo quy trình, copy **toàn bộ** canonical `database/schema_postgres.sql` và chạy nguyên file một lần. Không chạy thêm fixture riêng, SQL16 hoặc migration đã ghép. Kết quả mong đợi là result=PASS cùng summary.anchor_date/business_timezone khớp bản đã chọn; đó mới là xác nhận lớp SQL. Sau khi khởi động lại BE, đăng nhập mới/auth-me và smoke theo ID/runtime mới để xác nhận nghiệp vụ; không suy SQL PASS thành browser PASS. Ngày khác ngày cài thì phải chuẩn bị lại, không bỏ guard.

## 7. Ghi kết quả và việc còn lại

Mỗi case ghi: mã case, PASS/FAIL/BLOCKED/NOT_RUN, thời gian VN, URL/build, actor, class/member/Invoice/proposal/refund/attempt/document ID liên quan, precondition, HTTP/state quan sát, kết quả sau reload, dữ liệu đã tiêu thụ và bước tiếp. Không ghi password, cookie, OTP/QR secret hoặc binary vào báo cáo.

Kết quả chuẩn đặt tại Active/QA, không chép log/trace vào tài liệu này. FE duyệt các gợi ý chuẩn bị/trình bày và xác định source/build; sau đó phối hợp kiểm browser trên fixture thực tế. Các cửa auto-start/close, nhánh chưa chọn và provider thật chưa chạy phải giữ đúng trạng thái chưa kiểm.

**Thứ tự công việc sau tài liệu này:** hoàn thiện/chốt và bàn giao SCRIPT-DEMO cùng SYSTEM-DATA trước; phần chuẩn bị hai file đã hoàn tất theo Active/QA. Tiếp tục P2.2/P2.3 từ [bàn giao session mới](docs/03-TODO.md#p22-p23-handoff-20261110), kết hợp [Normal Lifecycle DA_CHOT](SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Refund Normal DA_CHOT](SCMS_BE_Refund_Normal_DA_CHOT.md), Dxx liên quan, contract, code và bằng chứng để duyệt các việc cần làm. Bảng review cần tách phần thiếu triển khai, thiếu kiểm chứng và lựa chọn nghiệp vụ thực sự mới; không hỏi lại rule đã duyệt, không bỏ qua mâu thuẫn thực chất chỉ vì tên file DA_CHOT. Phần chuẩn bị/giao diện FE vẫn là gợi ý để FE quyết định cuối.

Tham chiếu: [contract Normal](docs/01-API-CONTRACT-FE-BE.md#normal), [Lifecycle](docs/01-API-CONTRACT-FE-BE.md#normal-changes-20261110), [Refund](docs/01-API-CONTRACT-FE-BE.md#normal-refunds-20261110), [bàn giao Lifecycle](SCMS-BAN-GIAO-FE-NORMAL-LIFECYCLE-2026-11-10.md), [bàn giao Refund](SCMS-BAN-GIAO-FE-NORMAL-REFUND-2026-11-10.md).
