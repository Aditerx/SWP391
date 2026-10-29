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

- Quyết định cleanup27/10 giữ canonical IDs19–27, bỏ8bản trùng và Bắn súng ngoài danh mục **khi không có tham chiếu**; gặp liên kết phải đối soát, không cascade/xóa lịch sử.
- Invoice.amount bất biến. Không suy môn từ amount/tên gói hoặc seed lại để ghi đè giá đã thay có chủ đích.
- ID là dữ liệu từng môi trường, không hardcode vào FE. Các quyền xử lý fixture cũ là phê duyệt lịch sử cụ thể, không mở quyền sửa mọi dữ liệu production.
- Owner29/10 chốt hai biến thể SQL: schema root nâng cấp/bảo toàn ID và chứng từ; `database/schema_postgres.sql` được phép dựng lại dữ liệu SCMS tham chiếu cùng ID số độc lập từ1. PK subtype vẫn bằng `users.user_id`; không đổi UUID/khóa ghép sang số. Danh sách8phòng giữ Bóng rổ/Bóng chuyền chung một tài nguyên. Giữ `ai_consultations`/`support_requests` cho tương lai. Quyết định này không là quyền cho agent chạy reset Supabase.

<a id="d03"></a>
## D03 — Mua Advanced

- Chọn môn và Primary Coach trước thu tiền; từng đăng ký có môn/Coach/credit/hạn riêng.12 credit chỉ đúng môn và Coach chính/Coach thay hợp lệ.
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
- Báo vắng riêng khi **0<notice<12h**; giữ Scheduled/tài nguyên/credit, lưu actor/time/reason. Không tự tạo Absent/Excused hoặc hoàn tiền/credit. Đúng/sau start không báo trước hồi tố.
- Coach điểm danh thực tế: vẫn đến thì Present, vắng thì Absent; finalizer không bỏ qua vì có notice.
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
- Return kiểm chữ ký/hiển thị, không ghi Paid. IPN hợp lệ kiểm merchant/reference/amount/chữ ký/idempotency rồi ghi; FE đọc ownInvoice, poll hữu hạn và có Kiểm tra lại.
- IPN thành công nhưng lúc BE xử lý đã tới/quá deadline hoặc quyền giữ không còn: Paid+reconciliationRequired, không hồi sinh suất/quyền. Không suy khách trả trễ từ IPN tới muộn; không dùng vnp_PayDate để bỏ cutoff.
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

- Record mới chỉ Present/Absent, ghi **[start,end)**, không Late/scan sớm. Giữ lịch sử Late/Excused cũ. Điểm danh không trừ lần hai credit đã book.
- Coach được giao ghi trong buổi; CenterManager sửa sau buổi có reason/audit, đồng bộ result và giữ credit. Finalizer sau end chốt thiếu theo policy, bỏ buổi centerInvalidated.
- Normal gồm12buổi hợp lệ, tối đa15Member, Paid mới Registered. Member nghỉ cá nhân mất buổi, không bù.
- Sự cố TT thật có thể báo sau khi đã finalise nhưng phải xác minh buổi **thực sự không tổ chức**, có evidence, không dùng để sửa vắng cá nhân. Giữ buổi gốc không hợp lệ, liên kết đúng1makeup, chống bù lặp;13 record vật lý có thể chỉ12buổi hợp lệ.
- Check-in cửa là nâng cấp sau, default toggle tắt; không dùng check-in/out thay attendance buổi. Giữ code chế tài booking-block nhưng default tắt.

<a id="d16"></a>
## D16 — CashCorrection hẹp

CenterManager được đảo xác nhận Cash nhầm **chưa nhận tiền thực** cho Advanced chưa dùng, đủ12credit, không có booking kể cả đã hủy hoặc phụ thuộc/gia hạn/expiry/sự cố/hóa đơn khác. Không áp dụng Normal. Kiểm lại điều kiện khi Complete dưới lock; giữ chứng từ Paid/amount gốc, điều chỉnh riêng, không xóa hoặc tạo refund giả. Có reason/evidence/audit, chống lặp; không coi Approve là Complete.

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
