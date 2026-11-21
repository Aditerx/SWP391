# Ba tin nhắn gửi nhóm Backend (soạn 14/11/2026, chưa gửi)

Người nhận: nhóm Backend. Không chứa dữ liệu cá nhân. Mỗi tin độc lập.

## Tin 1: Cấp quyền cho Admin xem lịch Advanced

Chào nhóm Backend, Frontend đang đồng bộ màn Admin theo hướng Admin là vai trò bao trùm, có thể thao tác thay Manager khi cần. Qua kiểm tra bằng tài khoản Admin trên môi trường live (14/11/2026, chỉ gọi GET):

- Authorities của Admin hiện có 8 quyền: `LOCK_SUBJECTS`, `MANAGE_PERMISSIONS`, `MANAGE_RBAC`, `MANAGE_SPECIALIZATIONS`, `MANAGE_USERS`, `ROLE_ADMIN`, `VIEW_AUDIT_LOG`, `VIEW_REPORTS`. Không có `MANAGE_CLASSES`.
- `GET /api/private-session-proposals` với Admin trả 403 `Access Denied`.

Nhờ nhóm cho biết: Admin có được phép xem danh sách đề xuất lịch private session (và các màn quản lý lớp của Manager) không? Nếu có, nhờ cấp `MANAGE_CLASSES` cho Admin, hoặc cho `ROLE_ADMIN` truy cập endpoint này. Nếu không, nhờ xác nhận để Frontend giữ khung "không có quyền" cho Admin ở màn này. Cảm ơn nhóm.

## Tin 2: Lỗi 409 khi Coach chấp nhận đăng ký Advanced

Chào nhóm Backend, trên bản live, khi Coach bấm Chấp nhận một yêu cầu đăng ký Advanced đang ở trạng thái "Chờ duyệt", Backend trả lỗi:

- Request: `POST /api/advanced-registrations/{id}/decision`
- Body: `{ "accepted": true }`, khớp schema `Decision` trong OpenAPI (`accepted: boolean`, `reason: string`)
- Response: HTTP 409, `"error": "Conflict"`, `"message": "Operation violates a database constraint"`
- Thời điểm: `2026-11-14T08:38:33Z`

Nhờ nhóm xem log lỗi lúc đó để cho biết ràng buộc nào bị vi phạm (khóa duy nhất, khóa ngoại hay không-null) và nguyên nhân (dữ liệu trùng từ lần thử trước, migration chưa khớp, hay thiếu dữ liệu tham chiếu của Coach). Nếu đây là một trường hợp nghiệp vụ hợp lệ (ví dụ yêu cầu đã được xử lý), nhờ trả mã và thông điệp phân biệt được để Frontend hiển thị đúng thay vì lỗi chung. Nhờ nhóm cho biết id đăng ký trong log, và nếu cần Frontend sẽ cung cấp thêm. Cảm ơn nhóm.

## Tin 3: Ghi nhật ký cho các luồng Advanced

Chào nhóm Backend, khi kiểm tra `GET /api/audit-logs?size=100` bằng tài khoản Admin (14/11/2026), nhật ký chỉ có 8 bản ghi, toàn bộ là `LOGIN_GOOGLE` (6) và `REGISTER_MEMBER_GOOGLE` (2). Chưa thấy hành động nào cho các luồng Advanced.

Nhờ nhóm cho biết Backend có ý định ghi nhật ký cho các hành động sau không, và mã `action` cùng `targetEntity` dự kiến là gì:

- Member gửi yêu cầu đăng ký Advanced và Coach chấp nhận hoặc từ chối (`/decision`).
- Tạo đơn mua Advanced (`/order`) và đơn hết hạn sau 24 giờ.
- Coach gửi đề xuất lịch private session, và Member đồng ý hoặc từ chối (`/respond`).
- Hủy buổi private (`/cancel-private`).

Frontend cần danh sách mã này để hiển thị nhãn tiếng Việt/Anh ở màn Nhật ký, chuông thông báo và Tổng quan của Admin thay vì hiện mã thô. Cảm ơn nhóm.

<a id="backend-review"></a>
## Đối chiếu BE và đề xuất chờ Owner duyệt — 14/11/2026

Ba tin phía trên là nội dung FE gửi để đối chiếu, chưa phải kết luận. Phản hồi đề xuất:

**Tin 1 — Đúng về 403, chưa đúng về quyền Admin bao trùm.** Tôi kiểm lại có phiên: Admin vẫn thiếu `MANAGE_CLASSES` và GET proposals trả403; Manager đọc được. Theo D01, Admin không mặc nhiên có mọi authority. Route này còn kiểm center, nên cấp thêm authority chưa đủ để Admin không có center xem toàn hệ thống. Đề xuất giữ phạm vi hiện tại; FE hiển thị thiếu quyền. Nếu Owner muốn Admin giám sát toàn hệ thống, cần duyệt quyền chỉ đọc và phạm vi riêng; không cấp `MANAGE_CLASSES` hoặc bypass toàn role chỉ để mở một màn xem.

**Tin 2 — Lỗi trong ảnh là thật; payload đúng, chưa chứng minh bản mới còn lỗi.** Ảnh đã cho biết đăng ký `18`, Coach `27`, lỗi lúc `08:51:46Z` (15:51:46 giờ VN); tin FE còn ghi một lần lúc `08:38:33Z`. Poll ngay trước/sau ảnh vẫn là BE cũ. Bản cũ đổi `Requested → Accepted` nhưng không tạo/liên kết lớp; DB mới bắt Accepted phải có `class_id`, nên có căn cứ mạnh cho lỗi lệch BE–DB ở cửa chuyển đổi. Chưa có log server xác nhận tên constraint của chính request đó.

Render hiện đã chạy bản tạo lớp riêng. GET mới cho thấy đăng ký18 đã `Rejected`, không có lớp/quyền học; không dùng lại ID này để thử Accept. Đợt phát hành đã kiểm đề xuất lịch → Member Accept → hủy sớm trên quyền học có sẵn; chưa kiểm Coach nhận đăng ký mới. Đề xuất kiểm một đăng ký `Requested` hợp lệ: Coach nhận → GET lại `Accepted` và một lớp đúng Member/Coach/môn → retry không thêm lớp. Bước này chưa cấp credit hoặc tạo buổi. Nếu còn lỗi, lấy SQLSTATE/tên constraint và trạng thái sau rollback rồi mới sửa; không xóa dữ liệu, nới constraint hoặc reset để vượt lỗi.

**Tin 3 — Chưa thể kết luận Advanced không có nhật ký từ GET của Admin.** Contract RP-43 quy định Admin chỉ đọc `SYSTEM_SECURITY`; Manager đọc `BUSINESS` trong center. Tôi đã đọc được `DECIDE_ADVANCED_REGISTRATION` và `CREATE_ADVANCED_ORDER` bằng Manager. Số8 là quan sát cũ theo scope/trang, không đại diện toàn bộ nhật ký.

Các mã hiện có trong source đang chạy (không đồng nghĩa mọi nhánh đã nghiệm thu live):

| Hành động | `action` → `targetEntity` |
|---|---|
| Member gửi đăng ký | **Chưa ghi audit**; đề xuất `REQUEST_ADVANCED_REGISTRATION` → `ADVANCED_REGISTRATION` |
| Coach nhận/từ chối | `DECIDE_ADVANCED_REGISTRATION` → `ADVANCED_REGISTRATION` |
| Member xác nhận mua, tạo đơn | `CREATE_ADVANCED_ORDER` → `ADVANCED_REGISTRATION` |
| Hóa đơn hết hạn thanh toán | `EXPIRE_INVOICE` → `invoices` (filter phân biệt hoa/thường) |
| Coach gửi đề xuất lịch | `PROPOSE_PRIVATE_SESSION` → `PRIVATE_SESSION_PROPOSAL` |
| Member đồng ý/từ chối lịch | `ACCEPT_PRIVATE_SESSION` / `REJECT_PRIVATE_SESSION` → `PRIVATE_SESSION_PROPOSAL` |
| Buổi được tạo khi Member Accept | `BOOK_PRIVATE_SESSION` → `SESSION` |
| Hủy sớm buổi | `CANCEL_PRIVATE_SESSION` → `SESSION` |

Cần sửa cách nói “đơn hết hạn sau24giờ”:

`Member đăng ký → Coach nhận + tạo lớp riêng → Member xác nhận mua trong24giờ → tạo hóa đơn, hạn thanh toán5phút → Paid mới có quyền học → Coach đề xuất lịch → Member Accept mới có buổi/trừ credit`.

Quá24giờ chưa tạo đơn thì API chặn `REGISTRATION_CONFIRMATION_EXPIRED`; hiện không có job đổi trạng thái/ghi audit riêng cho mốc này. Nhật ký và chuông là hai nguồn khác nhau: chuông dùng `NotificationType` như `PRIVATE_BOOKING_PROPOSED`, `PRIVATE_BOOKING_CONFIRMED`, `PRIVATE_BOOKING_DECLINED`, `PRIVATE_BOOKING_CANCELLED`, `PAYMENT_SUCCESS`, `PAYMENT_EXPIRED`; không lấy audit action làm loại thông báo.

### Phạm vi đề xuất thực hiện sau khi duyệt

1. **Quyền/FE:** giữ RBAC và scope audit hiện tại; FE xử lý403 theo quyền, đặt nhật ký nghiệp vụ ở Manager và ánh xạ các mã đã có. Chưa mở Admin vận hành thay Manager.
2. **Nghiệm thu Accept:** dùng ca Requested hợp lệ trên bản mới, kiểm trạng thái lưu/lớp riêng/retry/quyền; nếu lỗi mới sửa đúng nguyên nhân và kiểm lại trước/sau merge. Ảnh cũ chưa là lý do sửa schema thêm.
3. **Bổ sung hẹp ở BE:** `AdvancedRegistrationService.request` ghi audit sau khi tạo đăng ký; retry cùng key không ghi trùng. `AdvancedRegistrationResponse` không trả `confirmationDeadline` cho Rejected; hiện đăng ký18 bị từ chối vẫn có deadline, dễ làm FE hiển thị nhầm. FE chỉ mở xác nhận mua khi `status=Accepted`. Kiểm audit đúng actor/center, rollback không có log thành công và Rejected không có hạn xác nhận mua.

Chưa sửa source/schema, chạy SQL cloud hoặc gửi tin cho nhóm trong lượt đối chiếu. Bằng chứng và phiên bản kiểm đặt tại [Active](docs/00-MASTER-CHECKLIST.md#active), mục QA `backendMessagesReview20261114`. Căn cứ quyền: [D01](docs/04-DEBUG-NOTE.md#d01), [RP-43](docs/01-API-CONTRACT-FE-BE.md#audit-scope-20261109); luồng mua/lịch: [D03](docs/04-DEBUG-NOTE.md#d03), [D04](docs/04-DEBUG-NOTE.md#d04).
