# SCMS — Quyết định nghiệp vụ hiện hành

## 0. Cách dùng

File này giữ **yêu cầu Owner đã chốt**, không thay trạng thái implementation hoặc nghiệm thu. Trạng thái ở [docs00](00-MASTER-CHECKLIST.md); API/field thật ở [docs01](01-API-CONTRACT-FE-BE.md). Khi rule/code/test lệch nhau, xác minh đúng nguồn, không mặc định sửa expected hoặc mở lại quyết định cũ.

Bản rút gọn từ lịch sử chốt25–27/10/2026. D12 về45ngày sử dụng thay quyết định1080giờ liên tục trước đó. Phân tích cũ, phương án không chọn và log gốc truy qua Git: `git show a4f3cd0:docs/04-DEBUG-NOTE.md`. Không cần đọc lịch sử khi mở session bình thường.

| Mã | Quyết định | API/luồng FE |
|---|---|---|
| [D01](#d01) | Hồ sơ Member tự phục vụ | [Hồ sơ và danh mục](01-API-CONTRACT-FE-BE.md#catalog) |
| [D02](#d02) | 9 môn/18giá, bảo toàn chứng từ | [Danh mục](01-API-CONTRACT-FE-BE.md#catalog) |
| [D03](#d03) | Đăng ký môn/Primary Coach, Paid mới cấp quyền | [Advanced](01-API-CONTRACT-FE-BE.md#advanced) |
| [D04](#d04) | Coach tạo ca, Manager hỗ trợ có consent | [Lịch Advanced](01-API-CONTRACT-FE-BE.md#booking) |
| [D05](#d05) | Hủy>=12h, notice sát giờ riêng | [Hủy/báo vắng](01-API-CONTRACT-FE-BE.md#cancellation) |
| [D06](#d06) | Sự cố TT: dời/thay Coach/hủy-bù có hồ sơ | [Sự cố](01-API-CONTRACT-FE-BE.md#incidents) |
| [D07](#d07) | Dời sớm/bất khả kháng và dấu notice | [Dời ca](01-API-CONTRACT-FE-BE.md#reschedule) |
| [D08](#d08) | Bù thực tế và restore credit gốc | [Sự cố](01-API-CONTRACT-FE-BE.md#incidents) |
| [D09](#d09) | Coach directory, sửa lazy đúng phạm vi | [Danh mục](01-API-CONTRACT-FE-BE.md#catalog) |
| [D10](#d10) | Holding/IPN/đối soát/hoàn ngoài SCMS | [Thanh toán](01-API-CONTRACT-FE-BE.md#payments) |
| [D11](#d11) | Local PG riêng rồi smoke deploy | [Kiểm thử](02-DEPLOYMENT-GUIDE.md#testing) |
| [D12](#d12) | 45 ngày sử dụng/ngày nghỉ/ca nắng | [Hạn và ngày giờ](01-API-CONTRACT-FE-BE.md#states) |
| [D13](#d13) | Report người/đăng ký và tiền | [Báo cáo](01-API-CONTRACT-FE-BE.md#reports) |
| [D14](#d14) | Holiday preview/xử lý ảnh hưởng/confirm | [Holiday](01-API-CONTRACT-FE-BE.md#holidays) |
| [D15](#d15) | Attendance trong buổi, bù Normal có hồ sơ | [Attendance](01-API-CONTRACT-FE-BE.md#attendance) |
| [D16](#d16) | Sửa Cash nhầm phạm vi hẹp | [Đối soát](01-API-CONTRACT-FE-BE.md#resolutions) |
| [D17](#d17) | QR payload/TTL/replay | [QR](01-API-CONTRACT-FE-BE.md#attendance) |
| [D18](#d18) | Quyền report quản trị | [Báo cáo](01-API-CONTRACT-FE-BE.md#reports) |
| [D19](#d19) | Evidence theo điều kiện nghiệm thu | [Kiểm thử](02-DEPLOYMENT-GUIDE.md#testing) |

<a id="d01"></a>
## D01 — Hồ sơ Member

GET/PATCH `/api/members/me`, DTO tự phục vụ riêng; danh tính từ Auth. Tự sửa đúng7trường: name,phone,address,dateOfBirth,gender,goal,healthNote. Email/password/avatar theo luồng riêng; không mở role/status/center/gói/credit/giá. Validation/null phải đặc tả khi triển khai, không tự mở thêm field. Trạng thái triển khai/nghiệm thu xem docs00.

Owner29/10 chốt riêng bộ33account demo theo `System Account.pdf`: mật khẩu demo đã duyệt, Active, `is_first_login=false`, không còn khóa đăng nhập cũ. Luồng tạo/cấp lại mật khẩu của BE vẫn giữ first-login hiện hành; không bỏ filter toàn hệ thống.

Ranh giới quản trị theo RP-04/RP-05 của thiết kế gốc, đã đối chiếu lại07/11: chỉ Admin quản lý tài khoản Admin/CenterManager; CenterManager vận hành Coach/Receptionist/Member trong center. Áp dụng cho cả target hiện tại và role được gán, không lách qua alias users/staff, PUT, đổi trạng thái hoặc cấp lại mật khẩu. Quyền endpoint vẫn phải đạt; Admin không mặc nhiên có mọi authority. Đây không thay luồng tự đổi mật khẩu/avatar của chính tài khoản và không mở phạm vi tài chính/report cho Admin.

Owner10/11 chốt GET `/api/roles` và GET `/api/permissions` chỉ dành Admin; các role khác bị chặn kể cả được gán thêm authority quản trị. GET `/api/roles/{id}` trả cùng dữ liệu permissions nên phải cùng gate để không lách qua ID. Đây là quyền đọc danh mục/quyền, không tự thay gate PUT phân quyền hoặc chốt guard quản trị cuối cùng; trạng thái triển khai ở docs00.

Owner11/11 đồng ý hướng P2.2/P2.3: PUT `/api/roles/{id}/permissions` cần Admin Active AND MANAGE_PERMISSIONS; giữ ít nhất một Admin Active có MANAGE_USERS và MANAGE_PERMISSIONS để còn khả năng quản trị. Invariant áp dụng các mutation permission/role/status/PUT user liên quan và phải chống thao tác đồng thời; permission trên role tác động mọi account dùng role đó. Không cấm tập rỗng cho mọi role và không tự đổi seed/quyền cloud. GET RBAC Admin-only đã chốt giữ nguyên. Đây là quyết định để triển khai ở session3, không phải trạng thái code hiện hành; kế hoạch/test theo docs03/Active.

<a id="d02"></a>
## D02 — Danh mục và giá

Chỉ9môn, giá VND cho trọn12buổi Normal hoặc12credit Advanced:

| Môn | Normal | Advanced | Loại |
|---|---:|---:|---|
| Tennis | 2.500.000 | 8.000.000 | Outdoor |
| Bơi lội | 1.500.000 | 4.500.000 | Outdoor |
| Pickleball | 1.800.000 | 5.000.000 | Indoor |
| Bắn cung | 2.000.000 | 6.000.000 | Outdoor |
| Bóng rổ | 1.200.000 | 3.500.000 | Indoor |
| Bóng đá | 1.000.000 | 3.500.000 | Outdoor |
| Bóng chuyền | 1.000.000 | 3.500.000 | Indoor |
| Cầu lông | 900.000 | 3.000.000 | Indoor |
| Bóng bàn | 800.000 | 2.500.000 | Indoor |

- Quyết định cleanup27/10 trên fixture khi đó giữ canonical IDs19–27, bỏ8bản trùng và Bắn súng ngoài danh mục **khi không có tham chiếu**; gặp liên kết phải đối soát, không cascade/xóa lịch sử. Dải ID19–27 không là yêu cầu ID chung cho mọi DB hoặc bản dựng lại29/10.
- Invoice.amount bất biến. Không suy môn từ amount/tên gói hoặc seed lại để ghi đè giá đã thay có chủ đích.
- RP-13 cho người có quyền định giá Normal khi công bố lớp, với giá Subject là mặc định. Bất biến Invoice/purchaseAmount không có nghĩa khóa giá Class vĩnh viễn; giá Class đã công bố không tự đổi theo Subject. Advanced order theo D03. Không suy từ quyền định giá lúc công bố thành quyền định giá lại mọi lớp đã mở/có đơn; phần sau cần xét đúng tình huống và giữ chứng từ cũ.
- ID là dữ liệu từng môi trường, không hardcode vào FE. Các quyền xử lý fixture cũ là phê duyệt lịch sử cụ thể, không mở quyền sửa mọi dữ liệu production.
- Owner29/10 chốt hai biến thể SQL: schema root nâng cấp/bảo toàn ID và chứng từ; `database/schema_postgres.sql` được phép dựng lại dữ liệu SCMS tham chiếu cùng ID số độc lập từ1. PK subtype vẫn bằng `users.user_id`; không đổi UUID/khóa ghép sang số. Danh sách8phòng giữ Bóng rổ/Bóng chuyền chung một tài nguyên. Giữ `ai_consultations`/`support_requests` cho tương lai. Quyết định này không là quyền cho agent chạy reset Supabase.
- Owner02/11 thay danh mục8phòng bằng20tài nguyên theo `room.jpg`: tách Bóng rổ/Bóng chuyền, giữ `room_id` số và thêm `room_code` C01; không thêm `subject_id`. Migration giữ phòng/lịch sử cũ, đối soát lịch và sức chứa trước chuyển; danh mục duy nhất xem database/README.
- Owner09/11 xác nhận toàn bộ dữ liệu SCMS là demo của project học tập; yêu cầu rà DB Supabase hiện tại và chuẩn bị lại `database/schema_postgres.sql` để làm sạch. Yêu cầu này thay phương án hòa giải riêng ca164: bộ dựng lại bỏ lịch sử demo cũ, giữ danh mục/account đã duyệt và mô hình hiện hành; không tự thay các rule quyền/tiền/lịch trong Dxx. Các nguồn thiết kế, code và dữ liệu phải được double-check, nêu mâu thuẫn và hỏi khi phát sinh lựa chọn nghiệp vụ mới. Chuẩn bị/kiểm local không đồng nghĩa đã thực thi reset cloud.
- Owner09/11 bổ sung: đồng ý bỏ dữ liệu legacy/thử/tồn/audit/backup, nhưng bộ dựng lại cần dữ liệu demo có kịch bản cho giao dịch/quyền học, lịch/attendance, chuyên môn và vận hành. Account/lớp/Coach phải được Owner duyệt lại; sau duyệt mới xuất `SYSTEM-DATA.md` theo từng bảng và thống nhất với FE. [Normal Lifecycle](../SCMS_BE_Normal_Lifecycle_DA_CHOT.md) và [Refund Normal](../SCMS_BE_Refund_Normal_DA_CHOT.md) là yêu cầu để rà trước triển khai, ưu tiên tái sử dụng phần đúng; chưa tự sửa code hoặc tự chốt ngoại lệ chưa rõ. Đề xuất và điều còn cần duyệt ở docs03; không coi bộ rỗng đã kiểm là bộ demo cuối cùng.
- Owner10/11 đồng ý bộ đề xuất dữ liệu chung, yêu cầu giữ nguyên bộ account trong `System Account.pdf`; account demo mới chỉ bổ sung, không thay danh tính gốc. Đồng ý ràng buộc chuyển lớp cùng giá/cùng center. Các câu hỏi về Cash/waitlist, ngoại lệ xác nhận sau mốc24h, xác minh người nhận, cách đếm7ngày làm việc và log center đang được giải thích trong docs03; không tự coi là đã duyệt mọi phương án kỹ thuật/ngoại lệ hoặc mở quyền mới.
- Owner10/11 chốt tiếp phần3–5: duyệt kiểm lớp tương thích/fallback và điều chỉnh N11 sang Pickleball làm đích N05, không tăng tổng lớp; BE chuẩn bị dữ liệu liên hệ Member, FE quyết định có dùng/hiển thị; ngày làm việc Refund là ngày TT mở theo lịch nghỉ, kể cả T7/CN nếu không đóng. Giữ07ngày làm việc và tài khoản PDF; không áp giả định T2–T6 hoặc tự đổi thành07ngày lịch. Quyết định dữ liệu khác với việc seed/triển khai, trạng thái theo docs00.
- Owner02/11 đã duyệt riêng bảng max_capacity cũ→mới của7lớp Normal center1 trong DB đang đối soát (ID11/12/13/14/16/17/18; bảng tại docs02). Cho phép điều chỉnh đúng7giới hạn trong cùng transaction chuyển phòng, kiểm lại danh tính lớp và Registered+Pending không vượt mức mới, lưu audit trước/sau; không đổi đăng ký, giá/chứng từ/attendance hoặc tự chia/hủy lớp. Đây là phạm vi theo ID đã được Owner xác nhận, không là quy tắc áp dụng các ID này cho mọi DB. Replay không ghi đè thay đổi có chủ đích về sau.

Owner11/11 đồng ý đồng bộ validation room name50/location100 với VARCHAR(50)/VARCHAR(100) của schema project đang có. Name vẫn bắt buộc, location optional/nullable; quá dài báo400 trước DB, không tự truncate/đổi trim hoặc mở rộng cột. Đây là đồng bộ constraint kỹ thuật hiện có, không là giới hạn lấy từ tiêu chuẩn ngành; implementation/kiểm theo Active.

<a id="d03"></a>
## D03 — Mua Advanced

- **Thuật ngữ/phạm vi06/11:** hệ thống hiện tại bán khóa Normal và quyền học Advanced theo môn/Primary Coach, không dùng gói hội viên chung tháng/VIP làm sản phẩm hoặc điều kiện đăng ký. `member_packages` hiện lưu đăng ký/quyền học Advanced, không phải lý do giữ mô hình hội viên chung. Owner yêu cầu loại bỏ dữ liệu thực sự thuộc mô hình cũ sau kiểm kê/phân loại, không giữ để tương thích dự phòng. Phạm vi bản ghi/DB và phụ thuộc cần xác định trước migration; không suy tên bảng hoặc package_id khác NULL là dữ liệu bỏ được. Không tự xóa quyền học/chứng từ của mô hình hiện tại.
- Chọn môn và Primary Coach trước thu tiền; từng đăng ký có môn/Coach/credit/hạn riêng.12 credit chỉ đúng môn và Coach chính/Coach thay hợp lệ.
- Owner06/11 duyệt B: tuyến enroll lớp/enrollments chỉ nhận Normal; Advanced bị USE_ADVANCED_REGISTRATION và phải dùng flow chọn môn/Coach nhận. Không tự cấp ghi danh từ waitlist Advanced legacy.
- Owner06/11 duyệt bỏ mô hình chung: đóng bán/gia hạn/đổi trạng thái gói chung và tạo danh mục mới; tạo Member không mua kèm gói bằng packageId. Migration16 chỉ xóa subgraph gói chung cũ xác định chắc chắn; bảo toàn quyền/chứng từ Normal/Advanced hiện hành, gặp liên kết hoặc hình dạng chưa rõ thì rollback. A đã duyệt nhưng phải chờ Owner chạy migration Supabase thành công mới triển khai.
- Kiểm lịch làm việc, tải cam kết đa môn, phòng và năng lực dự kiến; Coach nhận trước Invoice. Kiểm lại dưới transaction/lock khi order.
- Không yêu cầu Member khai lịch rảnh trong MVP, kể cả optional. Không có bộ ghép lịch Member–Coach; năng lực dự kiến không bảo đảm Member rảnh mọi ca hoặc giữ sẵn12ca.
- Chờ duyệt chưa chạy5phút/45 ngày. Chọn Coach không giữ tài nguyên. Invoice lấy Subject.advancedPrice và đóng băng; Paid hợp lệ mới Active/12 credit.
- Hạn theo D12. Legacy thiếu môn/Coach/thời điểm phải đối soát, không đoán hoặc cấp lại12credit.

<a id="d04"></a>
## D04 — Người tạo Advanced

Primary Coach tạo sau trao đổi; Manager cùng center hỗ trợ có lý do/audit. Actor/time từ BE và xác nhận đã thống nhất đúng lịch; đây là lời xác nhận của người tạo, không thêm vòng Member bấm duyệt. Coach thay một buổi không tự được tạo mọi ca của gói.

Kiểm Paid/Active, member active/block, đúng môn/Coach, credit, notice>=12h, lịch làm, phòng/Coach/Member không trùng, Holiday/Outdoor và sessionEnd<=deadline. Tạo ca/giữ tài nguyên/trừ1credit/audit cùng commit, thông báo sau commit. Retry cùng key/body cùng kết quả; khác body conflict; không tạo/thay trạng thái Advanced qua CRUD chung để bỏ workflow.

<a id="d05"></a>
## D05 — Hủy và báo vắng

- Member hủy ca Advanced của mình khi còn **>=12h**, gồm đúng12h: giải phóng tài nguyên, hoàn đúng1credit gói gốc. Không gia hạn; retry/race không hoàn thêm. Dưới12h không hủy hoàn thường, không trừ thêm credit đã book.
- Báo vắng riêng khi **0<notice<12h**; giữ Scheduled/tài nguyên/credit, lưu actor/time/reason. Theo Owner31/10 và xác nhận02/11, **notice và attendance Absent của đúng Member phải cùng commit**, không hoàn tiền/credit. Đây là ngoại lệ ghi trước buổi của D15, thay rule notice-only cũ; test cũ expected0attendance cần thay theo quyết định này. Đúng/sau start không báo trước hồi tố.
- Coach điểm danh thực tế: vẫn đến thì Present, vắng thì Absent; finalizer không bỏ qua vì có notice.
- Đổi thời gian ca làm notice hết hiệu lực: lưu nguyên attendance tự động cũ với superseded_at và tạo/ghi attendance hiện hành riêng; chỉ đổi phòng không làm notice hết hiệu lực. Không ghi đè kết quả Coach đã xác nhận.
- Normal không hủy/báo vắng vì lý do Member. Ngoại lệ TT theo D06/D08/D15.

<a id="d06"></a>
## D06 — Lỗi trung tâm

Có đủ dời, thay Coach riêng buổi hoặc hủy/bù vì TT. Manager cùng center xử lý có lý do/hồ sơ; dời/thay phải ghi Member đã đồng ý **đúng phương án cũ-mới**. Đổi phương án cần xác nhận lại; không dùng im lặng/thông báo làm consent. Nhánh hủy lưu đúng kết quả trao đổi, không giả đồng ý một ca mới.

Ca cũ và mới chưa bắt đầu; vì TT có thể chọn ca mới dưới12h nếu các điều kiện khác hợp lệ. Coach thay đúng môn, Active, lịch làm và không trùng; không đổi Primary Coach của gói. Audit/lock/idempotency, thông báo sau commit. Khả năng dùng quyền sau hạn theo D08, không tự mở ngoại lệ cho ca cá nhân.

<a id="d07"></a>
## D07 — Dời ca

- MEMBER_EARLY: Primary Coach/Manager hỗ trợ; cả ca cũ/mới còn>=12h, đã trao đổi, giữ credit/hạn.
- MEMBER_EMERGENCY: chỉ Manager; ca cũ còn0<12h, ca mới>=12h; có lý do bất khả kháng/đồng ý/audit. Đây là dời, không hoàn credit hay gia hạn tự do.
- CENTER_INCIDENT theo D06, có hồ sơ và quyền Manager.
- UI tạo/dời dùng chung, điền sẵn và so sánh cũ-mới. BE đổi lịch nguyên khối sau kiểm tài nguyên/deadline/consent.
- Chỉ dời thành công **thật sự đổi ngày/giờ** mới supersede notice, giữ lịch sử. Đổi sân/Coach cùng giờ, no-op/preview/fail không bỏ notice.

<a id="d08"></a>
## D08 — Bù và phục hồi

Gia hạn **toàn gói**, bù gián đoạn thực tế có căn cứ và hợp nhất khoảng trùng Holiday/incident. Chưa biết endedAt thì WaitingForRecovery, không gia hạn tạm; đã Expired trong lúc chờ chưa được book tiếp.

Job expiry lưu số credit thu hồi. Khi phục hồi chỉ restore balance expiry/deferred thực tế và credit gốc của ca bị TT hủy, chống lặp; không reset12. Nếu phần bù thực tế chưa đủ tới ca bù hợp lệ, Manager duyệt thêm **tối thiểu đủ cuối ca**, có lý do và kiểm giới hạn. Tiếp tục kiểm môn/Coach/tài nguyên/hạn chính xác. Sự cố báo sau buổi phải theo D15.

<a id="d09"></a>
## D09 — Coach directory

Log lịch sử đã xác nhận LazyInitializationException specialization ngoài Hibernate Session; không suy đã hết lỗi từ GET anonymous401. Nghiệm thu bằng JPA thật OSIV=false; service read-only transaction/DTO/batch query theo center, tránhN+1; giữ filter/contact theo quyền. Không mở rộng thành đổi capacity/admission workflow. Trạng thái triển khai/nghiệm thu xem docs00.

Coach mới hoặc mới chuyển role không tự gán chuyên môn. Gán môn qua `specializationIds`; khi chưa gán, directory trả danh sách rỗng và kiểm qualification vẫn chặn nhận đăng ký/tạo lịch dạy môn đó.

<a id="d10"></a>
## D10 — Thanh toán và đối soát

- Holding **5 phút**. Pending thiếu method/deadline hoặc dữ kiện legacy phải đối soát, không mở lại đơn hoặc thu thêm khi khoản cũ chưa xử lý.
- Return kiểm chữ ký/hiển thị, không ghi Paid. IPN hợp lệ kiểm merchant/reference/amount/chữ ký/idempotency rồi ghi. Owner04/11 duyệt thêm QueryDR: BE tự truy vấn VNPay bằng dữ liệu pay gốc đã lưu, xác minh chữ ký phản hồi và merchant/reference/amount/loại01/response00/status00 trước khi dùng chung nhánh ghi Paid. Không tin trạng thái/amount do FE gửi; lỗi hoặc thiếu chứng cứ không xác nhận chưa trả tiền.
- FE tự POST xác minh hóa đơn VNPay khi quay về, rồi GET ownInvoice; tái sử dụng Tải lại để kiểm tra có giới hạn, không bắt Member tự xác nhận đã trả. GET danh sách chỉ đọc. Đơn cũ thiếu metadata pay không được đoán/backfill thời điểm.
- Owner04/11 chốt giữ nguyên toàn bộ QueryDR đã triển khai: code/endpoint, bảng pay attempt, migration15 và các env đã thêm; không áp dụng đề xuất tắt/gỡ sau khi IPN hoạt động. Lý do: giữ kênh xác minh chủ động dự phòng và metadata gốc; code tạo link hiện vẫn dùng bảng attempt. Giữ không có nghĩa QueryDR live đã nghiệm thu; chưa tự thêm relay hoặc đổi env.
- IPN/QueryDR thành công nhưng lúc BE xử lý đã tới/quá deadline hoặc quyền giữ không còn: Paid+reconciliationRequired, không hồi sinh suất/quyền. Không suy khách trả trễ từ callback/truy vấn tới muộn; không dùng vnp_PayDate để bỏ cutoff.
- CenterManager duyệt hoàn đủ khoản đã nhận nhưng chưa cấp quyền, thực hiện **ngoài SCMS**, lưu evidence/kết quả riêng; Receptionist hỗ trợ. Approved chưa là đã hoàn; chỉ Completed có chứng cứ mới tính hoàn.
- Giữ Invoice.Paid/amount gốc, không refund API gateway hoặc cấp lại quyền từ tiền đang đối soát trong MVP. Muốn học lại đi đăng ký mới sau xử lý khoản cũ. Email nghiệp vụ là hướng mở rộng, không tự thêm kênh.

<a id="d11"></a>
## D11 — Môi trường kiểm

PostgreSQL local riêng/fixture riêng để kiểm mutation, không đọc.env cloud từ runner. Sau đó smoke deploy có kiểm soát và tích hợp FE hiện hành. Không mặc định tạo staging; không coi local thay CORS/cookie/IPN/Render. Quyền ghi cloud là phạm vi cụ thể được duyệt, không suy từ lựa chọn môi trường.

<a id="d12"></a>
## D12 — Hạn sử dụng, ngày nghỉ và ca chuẩn

- Advanced **45 ngày sử dụng từ Paid**, skip hợp thời gian TT đóng/nghỉ; đây là quyết định thay1080giờ liên tục. BE trả timestamp chính xác, end ca không vượt deadline; Invoice vẫn5phút.
- Thêm/tắt Holiday tính lại hạn quyền đang dùng, không cộng trùng incident. Phần nghỉ TT đã thực sự xảy ra được giữ; tắt nghỉ tương lai không xóa phần quá khứ.
- Không tạo nghỉ quá khứ để tự bù; dùng incident có chứng cứ. Thay đổi làm lịch đã đặt không hợp lệ phải preview/xử lý trước, không tự hủy/dời hoặc tự kéo hạn.
- 7ca 90phút:06:00–07:30;07:45–09:15;09:30–11:00;13:00–14:30;14:45–16:15;16:30–18:00;18:15–19:45. Nghỉ11:00–13:00.
- Outdoor(Tennis/Bơi/Bắn cung/Bóng đá) khóa ca3/4; Indoor mở7ca. Không suy indoor/outdoor từ tên khi metadata thiếu.

<a id="d13"></a>
## D13 — Report người, đăng ký và tiền

Đếm **người** và **lượt đăng ký** riêng. Người có nhiều đăng ký chỉ tính1lần ở chỉ số người;0 credit nhưng còn ca hợp lệ vẫn có quyền. Phân biệt chưa từng có quyền, từng có nay hết, Expired/Completed và legacy cần đối soát. UI ưu tiên theo người; số lượt dùng cho phân tích, không cộng chúng thành người.

BE trả tiền đã thu/đã hoàn/thu ròng theo kỳ thực; Approved chưa là hoàn. Cash được xác minh chưa nhận không là thu thật/refund giả; Paid cần đối soát nhưng đã nhận tiền vẫn là tiền thu. Giữ gross totalRevenue tương thích và nói rõ nghĩa. FE quyết định bố cục/chỉ số chính, không ép ưu tiên net. Quyền theo D18.

<a id="d14"></a>
## D14 — Holiday

Manager xem preview ảnh hưởng, xử lý từng lịch riêng qua workflow phù hợp rồi confirm bằng token còn hiệu lực. Dữ liệu đổi phải preview lại; không tự dời/hủy hàng loạt. Rút hạn tương lai không được làm ca đã đặt vượt hạn chưa xử lý. Giữ audit/ngày nghỉ thực tế quá khứ theo D12. Holidays hiện global; phạm vi center khác phải được kiểm/xử lý rõ, không tự ghi xuyên center.

<a id="d15"></a>
## D15 — Attendance và bù Normal

- Owner10/11 duyệt thiết kế [Normal Lifecycle](../SCMS_BE_Normal_Lifecycle_DA_CHOT.md) và [Refund Normal](../SCMS_BE_Refund_Normal_DA_CHOT.md): min=ceil(2×max/3), Manager chỉ tăng lúc tạo và khóa sau đăng ký/công bố; dời khai giảng thiếu sĩ số tối đa1lần với consent. Chuyển cùng môn/giá/center, recheck lịch/chỗ và giữ Invoice gốc/liên kết quyền; im lặng không đồng ý. TT hủy trước buổi đầu chủ động tạo nghĩa vụ hoàn100% khoản thực nhận; Manager duyệt→Receptionist chuyển ngoài SCMS/nộp chứng từ riêng tư→Manager đối soát nguồn ngân hàng độc lập mới Completed. Receiver đổi phải xác minh/duyệt lại, không hoàn trùng/vượt, không mở policy sau Ongoing hoặc Member tự hủy lớp vẫn vận hành. SLA07ngày TT mở từ ngày mở tiếp theo, kể cảT7/CN trừHoliday; thiếu thông tin/sự cố không xóa nghĩa vụ. Contact hồ sơ Member chỉ đúng quyền, FE quyết định hiển thị; không thay xác minh ngân hàng. Trạng thái implementation/nghiệm thu theo docs00.

- Owner10/11 duyệt hướng Cash Normal tại quầy và xử lý waitlist không tự tạo Cash online; Advanced giữ nguyên. Về lifecycle trong file Normal mới:48h chốt đăng ký,24h là hạn chuẩn xác nhận khai giảng; quá24h cảnh báo, Manager được xác nhận trễ trước buổi đầu nếu đủ điều kiện và có reason/actor/time. Đến buổi đầu chưa xác nhận thì không tự Ongoing hoặc tự hủy. Đã duyệt hướng log center hiện hành; phần tương thích lớp/contact tùyFE/ngàyTTmở đã được chốt sau cùng10/11 trong docs03 và fileRefund. Không mở ngoại lệ khác hoặc đổi7ngày làm việc thành ngày lịch.
- Owner06/11 chốt đăng ký và xét danh sách chờ Normal không yêu cầu có gói hội viên chung/Advanced Active trước đó. Quyền học theo hóa đơn của chính lớp; giữ kiểm tài khoản/owner/center/lịch/sĩ số và Paid mới Registered. Không cấp gói giả hoặc dùng credit Advanced làm điều kiện Normal; không xóa liên kết/lịch sử eligibility cũ.
- Owner06/11 duyệt0.1-C: học phí Normal phải dương; thiếu/≤0 thì chặn đăng ký và không thăng Waitlisted thành Registered. Học phí hợp lệ vẫn tạo Pending trước Paid. Không tự sửa giá/chứng từ/ghi danh lịch sử để hợp thức hóa dữ liệu legacy.
- Owner05/11 chốt Member Active được đọc lịch sử kết quả tập của chính mình qua GET `/api/training-results`. BE xác định Member từ phiên, ép phạm vi kể cả khi bỏ filter hoặc có thêm authority quản trị; truyền Member khác bị403. Quyền đọc này không cấp quyền ghi/sửa kết quả của Coach.
- State mới chỉ Present/Absent; ghi thủ công/QR trong **[start,end)**, không Late/scan sớm; ngoại lệ Member báo vắng tự ghi Absent trước buổi theo D05. Giữ lịch sử Late/Excused và attendance superseded; superseded không tính là kết quả hiện hành. Điểm danh/correction không trừ lần hai credit đã book.
- **Owner09/11 chốt thay mốc khóa Coach cuối buổi:** Coach được giao, Active, có RECORD_RESULT được correction sau end tới **trước00:00 ngày kế tiếp của ngày buổi học**, Clock/Asia/Ho_Chi_Minh; diễn đạt hết23:59 cùng ngày, không phải24giờ sau end. Sau cutoff chỉ quản lý theo quyền correction hiện hành; Manager không bị áp hạn Coach. Coach khác không được sửa/note, có thông báo rõ sai phân công; không mở dữ liệu ngoài scope. Correction cần reason/audit, đồng bộ result nếu có, giữ credit/lịch/hạn và timestamp check-in gốc. Khi finalizer chưa tạo attendance, cho định danh sessionId+memberId thay attendanceId và tạo record hiện hành sau kiểm eligibility đúng buổi, không tạo kết quả chuyên môn giả.
- **Owner09/11 chốt hai mốc:** giữ hoàn tất Session/khóa/quyền học lúc end theo điều kiện hiện có; attendance thiếu có thể tự ghi Absent nhưng Coach còn được đối soát trong khoảng đệm. NO_SHOW_RECORDED và chế tài vắng chỉ xử lý sau cutoff trên state hiện hành; job trễ không kéo dài quyền Coach, retry không nhân thông báo. Không ghi đè kết quả đã xác nhận; buổi hủy/centerInvalidated và attendance superseded không được correction theo đường này. Chế tài khóa7ngày vẫn default tắt. Trạng thái triển khai/kiểm/deploy ở docs00, không suy từ quyết định này.
- **Owner09/11 chốt note:** ghi chú của attendance được Member đọc trên bản ghi của chính mình; không mở note người khác hoặc reason/audit. Note optional/plain text/tối đa1000ký tự sau trim; bỏ/null giữ nguyên, rỗng sau trim xóa có audit; reason correction bắt buộc, tối đa1000ký tự, không tự chép thành note. Giữ absenceNoticeId/notice.reason gốc, không backfill note bằng parse log; sửa note sau buổi cũng theo quyền/hạn và reason correction. Thiết kế hiển thị FE chỉ là gợi ý flow/dữ liệu, FE quyết định bố trí/câu chữ; BE kiểm quyền và dữ liệu độc lập UI. Chi tiết contract09/11 xem docs01; trạng thái runtime/deploy theo docs00.
- RP-36: tạo/sửa/xóa giáo án và đánh giá chuyên môn dành Coach theo phân công; role quản trị hoặc authority quản lý lớp không tự cấp quyền ghi chuyên môn. Không lấy quyền hỗ trợ đặt lịch D04 hoặc correction attendance ở trên để suy rộng. Giáo án Normal theo lớp có quan hệ với Member qua enrollment/quyền học, không bắt mỗi plan phải gắn memberId trực tiếp hoặc nhân bản cho từng người. Trường hợp Normal Registered/Paid đọc đúng plan lớp được tách khỏi các ca hủy/đảo quyền/lịch sử cần đối chiếu riêng.
- Normal gồm12buổi hợp lệ, tối đa15Member, Paid mới Registered. Member nghỉ cá nhân mất buổi, không bù.
- Sự cố TT thật có thể báo sau khi đã finalise nhưng phải xác minh buổi **thực sự không tổ chức**, có evidence, không dùng để sửa vắng cá nhân. Giữ buổi gốc không hợp lệ, liên kết đúng1makeup, chống bù lặp;13 record vật lý có thể chỉ12buổi hợp lệ.
- Check-in cửa là nâng cấp sau, default toggle tắt; không dùng check-in/out thay attendance buổi. Giữ code chế tài booking-block nhưng default tắt.

**Owner11/11 — hướng đã duyệt cho ba session P2.2/P2.3, chưa mặc định đã triển khai:**

- GET training-results: staff thường theo center actor phía BE, kết hợp sessionId/memberId/coachId bằng AND; Member/Coach giữ own/author. Admin Active có center NULL và đạt authority đọc được đọc toàn hệ thống theo mẫu AccountScope/TrainingAccessPolicy hiện có; Admin có center theo center được gán. Không suy ngoại lệ này sang tài chính/report, log BUSINESS hoặc các thao tác chỉ CenterManager; D18/RP-43 và quyền từng route giữ nguyên.
- Content sau buổi: Owner đồng ý Coach được giao, có RECORD_RESULT theo RP-36 sửa content của TrainingResult **đã tồn tại** từ end tới trước00:00 ngày kế tiếp ngày Session, giờ VN; chỉ sửa content, giữ attendance/check-in/note/credit/lịch/quyền. Có audit bằng cơ chế hiện có để truy actor/thời điểm/record, không đòi bảng hoặc màn hình mới. Không tự tạo result/attendance thiếu, không dùng default Present như bằng chứng đã học; method/DTO và test sẽ trình riêng trước sửa ở session2. Quyền correction attendance/note và manual/QR [start,end) ở trên không đổi.
- Owner12/11 duyệt hình dạng R2 content-only theo resultId: content bắt buộc/non-null, chuỗi rỗng xóa và giữ nguyên khoảng trắng; giữ tác giả/recordedAt gốc. Coach phải vừa đúng phân công buổi vừa là tác giả result, không cấp Coach mới sửa record Coach cũ. Audit cùng transaction, no-op không log thừa; thiếu result không tạo mới. Handoff phải giải thích ảnh hưởng flow POST/correction/PATCH và cách FE đọc lại để bắt nhịp; demo2.2/2.3 vẫn chờ Owner thử hoặc skip riêng.
- Shared plan: Owner đồng ý xem xét hướng Member chỉ đọc giáo án lớp khi Completed và có bằng chứng quyền Paid hợp lệ; muốn giữ đơn giản, phân quyền đọc là đủ, không làm snapshot/versioning giáo án trong phạm vi này và chỉ bàn nâng cấp nếu Owner hỏi. Own-author read của Coach cũ giữ phạm vi hiện có, không mở mọi roster/attendance hoặc quyền sửa record của Coach khác. Phạm vi Cancelled được đối chiếu bằng ví dụ và đề nghị giữ ngoài mở rộng mới; chưa tự mở quyền chỉ vì từng Paid. Kế hoạch/tác động cần trình trước sửa, không suy quyết định này cho phép hủy/hoàn Normal sau khai giảng. Snapshot consent Normal/receiver Refund đã có vẫn giữ.
- Owner12/11 chốt sau ví dụ Cancelled legacy/demo: chỉ Registered hoặc Completed với Paid/effective entitlement hợp lệ được shared Normal plan; Cancelled không được shared read dù từng Paid/có kết quả. Giữ own private plan/result/attendance/evaluation history theo scope hiện có, list/detail cùng quyền và không thêm policy hủy/hoàn sau Ongoing. Không thêm bảng, nhân bản plan hoặc snapshot/versioning giáo án.
- R1: Owner muốn được trình lựa chọn nghĩa filter ngày khi tới mục, chưa chốt nghĩa mới; không sửa query/check-in data trước đó. Ba session làm từng mục có kiểm agent và demo Owner FE/Swagger hoặc skip tường minh, không gộp; trạng thái/bằng chứng ở docs00/QA, điều phối ở docs03.

**Owner12/11 — đã chọn R1 sau giải thích tác động API/FE:** startDate/endDate của GET attendance dùng ngày Session đối với record gắn buổi; lobby lịch sử không có Session dùng ngày checkInTime. Hai đầu ngày đều tính, giờ nghiệp vụ Asia/Ho_Chi_Minh. Bỏ filter ngày giữ tập/phạm vi hiện hành; không giả timestamp cho Absent/null hoặc sửa dữ liệu lịch sử. Giữ endpoint/query/response hiện có, quyền và các luồng ghi; FE cần đối chiếu client filter/grouping và cách xử lý checkInTime:null, chưa suy FE deployed đã tương thích. Triển khai/kiểm theo Active/QA, không tự mở quyền reset/deploy hoặc chuyển2.2.

<a id="d16"></a>
## D16 — CashCorrection hẹp

CenterManager được đảo xác nhận Cash nhầm **chưa nhận tiền thực** cho Advanced chưa dùng, đủ12credit, không có booking kể cả đã hủy hoặc phụ thuộc/gia hạn/expiry/sự cố/hóa đơn khác. Không áp dụng Normal. Kiểm lại điều kiện khi Complete dưới lock; giữ chứng từ Paid/amount gốc, điều chỉnh riêng, không xóa hoặc tạo refund giả. Có reason/evidence/audit, chống lặp; không coi Approve là Complete.

Owner06/11 duyệt A: một đăng ký Normal có hóa đơn riêng, không liên kết quyền Advanced đang sửa, không phải phụ thuộc chỉ vì được tạo sau ngày kích hoạt Advanced. Bỏ suy luận legacy theo thời gian sau bước loại mô hình cũ; vẫn chặn liên kết eligibility thật và hóa đơn khác thuộc cùng quyền, giữ các guard D16 trên.

<a id="d17"></a>
## D17 — QR

Response đúng payload/expiresInSeconds; TTL còn lại1–30 giây, không cố định30. FE refresh khi hết hạn/foreground, không log token/secret. Scan cần session/Member/Coach được giao/quyền Paid, chỉ bước QR hiện tại trong[start,end), chống replay/rate. Không dùng QR test/hết hạn để chứng nhận browser tích hợp.

<a id="d18"></a>
## D18 — Quyền report

Dashboard/report quản trị dành CenterManager/Admin **có VIEW_REPORTS**, active và đúng phạm vi center. Coach chỉ nghiệp vụ được giao, không đọc tổng tài chính dù có authority phụ. Kiểm BE role/authority/query scope; không chỉ ẩn UI hoặc sửa authority DB live để ép test.

<a id="d19"></a>
## D19 — Nghiệm thu

Mỗi case ghi Dxx, precondition/fixture/actor, hành động, kết quả response+state, môi trường/SHA và PASS/FAIL/BLOCKED/NOT_RUN. Ưu tiên tiền/quyền/owner-center/lịch/biên/retry/race; không coi route count,200 hoặc401 là nghiệp vụ đã đạt. Validate phiên trước nhóm có đăng nhập; test lỗi setup là BLOCKED, không tính sai vai trò đã bị chặn.

Expected phải đối chiếu quyết định Owner và dữ liệu cụ thể; Swagger/code/test có thể cùng sai. Local callback/SMTP loopback không thay gateway/email delivery thật. Kiểm BE theo AGENTS; full role/owner/center/FEbrowser trên Render chỉ khi có tài khoản/fixture/phạm vi được xác định. Raw QA chỉ mở khi cần, không bàn giao cả kho log cho FE.
