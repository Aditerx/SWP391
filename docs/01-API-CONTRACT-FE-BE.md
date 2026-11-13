# SCMS — Contract bàn giao Frontend

## 0. Đọc và sử dụng

Gói FE gồm **file này** và [OpenAPI snapshot](../artifacts/fe-handoff/openapi.json). Danh sách đầy đủ method/path/query/body/schema tra bằng Swagger hoặc import snapshot; không có bản inventory/schema Markdown riêng.

Bản tóm tắt đợt tích hợp02/11 để gửi riêng một file: [SCMS-BAN-GIAO-FE-2026-11-02.md](../SCMS-BAN-GIAO-FE-2026-11-02.md), gồm room,7giới hạn lớp và báo vắng. Contract đầy đủ vẫn giữ tại đây; trạng thái triển khai theo docs00.

- Rule Owner: [D01–D19](04-DEBUG-NOTE.md). Trạng thái nghiệm thu và phần còn mở: [docs00](00-MASTER-CHECKLIST.md#active).
- Snapshot/QA là ghi nhận có thời điểm, không cam kết server hiện tại vẫn cùng trạng thái. Khi tích hợp kiểm lại Live identity và tài khoản thực tế.
- D01 hồ sơ Member và D09 Coach directory có trong bản tích hợp; trạng thái local/Render theo docs00. Không dùng API quản trị thay self-service hoặc coi GET401 chứng minh directory hết lỗi lazy.
- JSON dưới đây có nhãn **minh họa** hoặc **trích local đã ghi**. ID/ngày/giá fixture không dùng để replay lên production.

## 1. Kết nối, đăng nhập và lỗi

Base Render: `https://scms-app-p1gy.onrender.com`; local thông thường `http://localhost:8080` (runner dùng port ngẫu nhiên). Auth bằng **JSESSIONID**: Fetch `credentials:'include'`, Axios `withCredentials:true`. Không thay bằng bearer token. Kiểm `GET /api/auth/me` xác nhận phiên trước khi kiểm nghiệp vụ.

| Thao tác | Request / kết quả |
|---|---|
| POST `/api/auth/login` | email,password →200; userId/memberId/role/authorities/isFirstLogin và cookie |
| GET `/api/auth/me` | Danh tính/quyền của phiên, không lấy từ ô nhập FE |
| POST `/api/auth/logout` |204, không parse JSON body |
| POST `/api/auth/change-password` | oldPassword/newPassword theo schema;204; xử lý isFirstLogin theo luồng |
| POST `/api/auth/register` | fullName/email/password bắt buộc; phone/dateOfBirth/gender tùy schema; Pending+OTP |
| POST `/api/auth/verify-registration` | email,otp →204 |
| POST `/api/auth/resend-otp` | email,purpose bắt buộc; purpose=REGISTER cho đăng ký; giới hạn60giây/lần và3OTP/giờ |
| POST `/api/auth/forgot-password` | email; phản hồi chung không tiết lộ account tồn tại |
| POST `/api/auth/reset-password` | email,otp,newPassword →204 |
| POST `/api/auth/google` | credential là Google ID token →200 LoginResponse và cookie JSESSIONID; không gửi access token |

Đăng nhập Google GIS xác minh chữ ký, `aud`, `iss`, hạn và `email_verified` theo `GOOGLE_CLIENT_ID`. Endpoint tìm user theo Google `sub`, sau đó theo email đã xác minh. Email chưa có tài khoản và `GOOGLE_AUTO_REGISTER=true` thì tạo Member Active cùng hồ sơ Member; `isFirstLogin=false`. Nếu cờ tự đăng ký tắt, email chưa có tài khoản trả `404 GOOGLE_ACCOUNT_NOT_FOUND`. Tài khoản Pending không được kích hoạt hoặc liên kết Google; cần hoàn tất OTP đã gửi. Tài khoản staff hiện có giữ nguyên role khi đăng nhập Google. Luồng Google tạo phiên giống login thường; các request tiếp theo vẫn gửi credentials/cookie.

Lỗi của `/api/auth/google` dùng body `{ "code": "...", "message": "..." }`:

| HTTP | code | Ý nghĩa |
|---|---|---|
| 400 | `GOOGLE_CREDENTIAL_MISSING` | Thiếu credential hoặc credential rỗng |
| 401 | `GOOGLE_TOKEN_INVALID` | Token sai chữ ký/audience/issuer/hạn/định dạng, thiếu cấu hình client ID hoặc thiếu `sub`/email |
| 401 | `GOOGLE_EMAIL_NOT_VERIFIED` | Google chưa xác minh email |
| 403 | `ACCOUNT_LOCKED` | Tài khoản bị khóa, vô hiệu hoặc có trạng thái không hỗ trợ đăng nhập |
| 409 | `ACCOUNT_PENDING_VERIFICATION` | Tài khoản đang chờ OTP đăng ký thường |
| 409 | `GOOGLE_ACCOUNT_CONFLICT` | Email đã liên kết với Google `sub` khác |
| 404 | `GOOGLE_ACCOUNT_NOT_FOUND` | Email chưa có tài khoản và tự đăng ký đang tắt |

Ví dụ: `{ "code": "ACCOUNT_PENDING_VERIFICATION", "message": "Tài khoản đang chờ xác minh email." }`.

`LoginResponse` của `/api/auth/login`, `/api/auth/google` và `/api/auth/me` gồm `userId,email,fullName,role,authorities,isFirstLogin,memberId,avatarUrl,isNewAccount`. `isNewAccount=true` chỉ trong đúng response Google đã tạo tài khoản tại lần đăng nhập đó; cờ được tính trong response, không lưu DB. FE tự lưu trạng thái onboarding; client cũ có thể bỏ qua field mới. `isFirstLogin` vẫn false cho tài khoản Google mới. Avatar Cloudinary được tối ưu động bằng f_auto,q_auto; URL Google giữ nguyên, không ghi đè URL trong DB chỉ để tối ưu. Cấu hình/migration trước triển khai theo [docs02](02-DEPLOYMENT-GUIDE.md#google-login).

OTP đăng ký10phút, tối đa5lần sai, dùng một lần. REGISTRATION_PENDING/EMAIL_ALREADY_REGISTERED/OTP_INVALID/OTP_EXPIRED/OTP_ATTEMPTS_EXCEEDED/OTP_RESEND_TOO_SOON/OTP_RESEND_LIMIT cần UI riêng. `OtpMessageResponse.code` là mã trạng thái tùy chọn, có thể null; không chứa mã OTP. HTTP200/thông báo gửi chung không chứng minh thư đã được nhận vì mail gửi bất đồng bộ sau commit. Trạng thái email cloud xem [docs00](00-MASTER-CHECKLIST.md#active). Tài khoản demo lịch sử không phải credential production.

`auth/me` hiện không có `centerId`; thiết kế center1 giữ nguyên. Actor/center dùng để phân quyền được BE đọc từ phiên, không nhận mã C01 hoặc center do FE gửi làm căn cứ cấp quyền.

Cross-site HTTPS cần CORS exact FE origin, credentials và cookie SameSite=None/Secure=true. Source default lax/false; phải kiểm browser thực, không suy từ Swagger/Postman. Không có phiên hợp lệ thì kết quả kiểm role/owner là BLOCKED.

FE xử lý **200/201/204** theo route;204 không có body. Lỗi có thể `{code,message}` hoặc `{timestamp,status,error,message}`/body framework: ưu tiên code→message→HTTP fallback.401 đăng nhập lại;403 thiếu quyền;404 có thể che tài nguyên không thuộc chủ;409 tải lại trạng thái/preview. Không lặp vô hạn hoặc tự đổi trạng thái local để vượt lỗi BE.

**Quyền của phiên đang tồn tại:** trước API có bảo vệ, BE đọc lại account/role/permissions. Account thiếu/không Active/không còn role trả403 `ACCOUNT_NOT_ACTIVE`; không coi `/auth/me`200 cũ là đủ cho các request sau. Role của account đổi trả403 `SESSION_ROLE_CHANGED`, phiên cũ bị hủy: FE đưa về đăng nhập. Cùng role nhưng permission đổi thì gate và authorities trong `/auth/me` dùng quyền hiện hành ngay request tiếp theo. Phiên đã bị đánh dấu thu hồi do reset/cấp lại/đổi password quản trị trả401. Public IPN/Return không phụ thuộc phiên Member. Phạm vi kiểm/deploy xem docs00; đây không phải kết luận nguyên nhân của mọi401/403 lịch sử.

<a id="catalog"></a>
## 2. Hồ sơ và danh mục

| API | Field/giới hạn cần biết |
|---|---|
| GET `/api/public/subjects` | Public; **chỉ** id/slug/name/description, không có giá/outdoor |
| GET `/api/subjects`, `/{id}` | Có phiên; normalPrice/advancedPrice/outdoor/status và metadata. [Bảng giá chuẩn](04-DEBUG-NOTE.md#d02) |
| GET `/api/public/membership-packages?centerId=1` | Tuyến đọc danh mục legacy; sau migration16 danh mục trống, không phải sản phẩm đang bán |
| GET `/api/packages` | Có phiên; danh mục legacy trống sau migration16; giá khóa hiện hành nằm ở Subject/Class |
| GET `/api/coaches?specializationId=...&keyword=...` | Actor Active; specializationId khác subjectId; filter/center/contact theo D09 dưới đây |
| GET `/api/rooms`, `/api/classes` | Có phiên; tra schema/filter; ID từ môi trường đang kết nối |
| GET/PATCH `/api/members/me` | Member Active;200; danh tính từ phiên, chỉ7field hồ sơ theo D01 dưới đây |
| POST/DELETE `/api/users/me/avatar` | Luồng avatar hiện có, tách D01; upload multipart theo schema |

Các group quản trị users/members/staff/roles/permissions/specializations/packages/rooms/classes và training-plan/evaluation/feedback xem OpenAPI. Quyền có thể gồm role+authority+owner/center trong service; snapshot không thay full role matrix. Nhóm baseline chưa có acceptance business đầy đủ chỉ vì route xuất hiện.

**Guard baseline đã bổ sung:** users/members/staff/rooms lọc danh sách và kiểm đối tượng theo center của account hiện hành; role/authority tại controller vẫn cần đạt. Admin có center NULL giữ phạm vi đọc quản trị users/rooms hiện có, không suy ngoại lệ này sang tiền/report. API `/members/{id}` chỉ nhận target role Member; `/staff/{id}` chỉ nhận Coach/Receptionist/CenterManager: PUT/PATCH sai loại hoặc khác center trả403 trước ghi, không dùng các tuyến này để sửa Admin. Tạo Admin phải qua tuyến user administration và guard MANAGE_PERMISSIONS hiện có; POST staff không nhận role Admin/Member. Room mới lấy center từ actor, không nhận center client; Admin center NULL giữ default center1 theo thiết kế hiện tại. RP-04 đã áp dụng ở các mutation users/staff: chỉ Admin được tạo, sửa, đổi status/role hoặc cấp lại mật khẩu tài khoản Admin/CenterManager, kiểm cả role cũ/mới. Không tự gán quyền endpoint cho Admin; quản lý Admin vẫn cần MANAGE_PERMISSIONS như trước. MANAGE_USERS/MANAGE_RBAC trên role khác không vượt trần này. Manager giữ các thao tác hợp quyền trên Coach/Receptionist/Member trong center. Tự đổi password/avatar vẫn qua tuyến self-service. Scope/loại log nghiệp vụ của Manager theo RP-43 còn cần xử lý query/provenance riêng, chưa được nghiệm thu trong nhóm sửa này.

**Room — Owner02/11.** `id`/`roomId` vẫn là số trong path/request lịch; response thêm `roomCode` (ví dụ `C01-TEN-01`) để hiển thị/tìm trong danh sách. Không đổi ID thành mã, không thêm `subjectId`. POST/PUT `/api/rooms` nhận thêm `roomCode` tùy chọn: tối đa40ký tự, chữ hoa, dạng `C[ít nhất2chữ số]-[chữ/số]-[chữ/số]`, tiền tố phải đúng center. POST thiếu mã tự sinh `C01-RM-...`; PUT thiếu mã giữ mã cũ. POST201, PUT200; mã trùng409, sai định dạng400, sai center400 `ROOM_CODE_CENTER_MISMATCH`. Tên/khu vực/loại/sức chứa20tài nguyên theo [danh mục room](../database/README.md#room-catalog); không hardcode ID fixture. GET rooms ẩn phòng `Closed` có mã `LEGACY`, vẫn giữ phòng Maintenance và phòng đóng khác; lịch sử DB không bị xóa.

**D01 — hồ sơ của Member.** GET và PATCH đều trả đúng `name,phone,address,dateOfBirth,gender,goal,healthNote`; không có id/email/role/credit/giá. Lấy định danh và email từ `/api/auth/me`. PATCH giữ field vắng mặt, `null` xóa6field nullable, `{}` không đổi dữ liệu. `name` nếu gửi phải là string khác null/rỗng/trắng. Key ngoài7field được bỏ qua, gồm memberId/role/status/centerId/packageId/credit/amount/email/password/avatarUrl; không dùng chúng chọn người hay cấp quyền.

Giới hạn: name100,phone20,address255,goal255ký tự; gender `Male`/`Female`/`Other` hoặc null; dateOfBirth đúng `YYYY-MM-DD`, không sau ngày hiện tại theo Clock Asia/Ho_Chi_Minh. Phone chỉ giới hạn độ dài, chưa kiểm format. healthNote theo TEXT, không có giới hạn độ dài ở ứng dụng. Empty string của phone/address/goal/healthNote được giữ nguyên, không tự đổi null.

Lỗi400 `{code,message}` gồm NAME_REQUIRED/NAME_TOO_LONG/PHONE_TOO_LONG/ADDRESS_TOO_LONG/GOAL_TOO_LONG/GENDER_TOO_LONG/INVALID_GENDER/INVALID_DATE_OF_BIRTH/FUTURE_DATE_OF_BIRTH và INVALID_PROFILE_BODY cho JSON null. Sai kiểu field có INVALID_PHONE/INVALID_ADDRESS/INVALID_GOAL/INVALID_HEALTHNOTE; body array/scalar/JSON hỏng dùng lỗi framework400, cần fallback ở mục1. Role khác hoặc phiên cũ của account Inactive trả403. `firstLogin=true` vẫn403 PASSWORD_CHANGE_REQUIRED: đổi mật khẩu qua luồng hiện có, đọc lại `/auth/me`, rồi mở hồ sơ; không có ngoại lệ bỏ filter.

**D09 — Coach directory.** Scope lấy từ account trong phiên: tài khoản thường chỉ center của mình; Admin có center null mới đọc toàn hệ thống. `keyword` trim, không phân biệt hoa/thường, tìm tên Coach hoặc specialization; kết hợp specializationId vẫn trả toàn bộ specialization của Coach khớp. Coach không specialization có `specializations:[]`, không kết quả trả `[]`; không cam kết thứ tự list. Email/phone chỉ có giá trị với MANAGE_USERS, nếu thiếu quyền là null. Account Inactive bị403 kể cả còn cookie cũ. Runtime thiếu center trả400; fixture trạng thái sai này kiểm riêng trên H2 vì guard PostgreSQL không cho lưu Member thiếu center.

<a id="phase1-account-resource-20261109"></a>
### Bổ sung Phase1 09/11 — phạm vi hồ sơ, validation và sân

**GET danh mục RBAC — Owner10/11:** GET `/api/roles`, GET `/api/roles/{id}` và GET `/api/permissions` chỉ cho tài khoản **Admin Active**, dựa trên role hiện hành trong DB; không đòi thêm MANAGE_PERMISSIONS cho quyền đọc. Manager/Receptionist/Coach/Member trả403 kể cả có authority này; anonymous401. Phiên Admin cũ của account Inactive/Locked trả403 ACCOUNT_NOT_ACTIVE; role đổi theo guard phiên hiện hành. Response thành công giữ nguyên DTO; Admin tra role không tồn tại giữ404. GET theo ID cùng gate để không đọc permissions qua tuyến chi tiết. Không thay gate PUT `/roles/{id}/permissions` trong nhóm thay đổi này.

Nếu màn không phải Admin đang gọi hai danh mục này để dựng dropdown thì FE cần bỏ phụ thuộc đó; quyền thao tác trên account vẫn theo loại role được phép và BE kiểm độc lập. Không giải quyết403 bằng cấp MANAGE_PERMISSIONS cho Manager. Trạng thái kiểm/deploy xem Active/QA; không coi cập nhật contract là FE đã tích hợp.

Coach/Member dùng `/api/auth/me`, hồ sơ `me`, roster/enrollment được giao và API học tập theo owner. GET `/api/users`, `/api/users/{id}`, `/api/members`, `/api/members/{id}` và `/api/staff` là DTO quản trị chứa thông tin riêng; Coach/Member bị403 kể cả được gán thêm MANAGE_USERS/REGISTER_MEMBER. GET classes/rooms và tra tài nguyên vẫn là lookup có phiên theo scope hiện hành. GET feedback có danh tính `/api/classes/{id}/feedback` cũng từ chối Coach/Member kể cả có authority quản trị; Coach dùng `/api/coaches/me/feedback` ẩn danh theo Coach trong phiên. `/api/members/at-risk` yêu cầu Admin/CenterManager+VIEW_REPORTS; Coach dùng `/api/coaches/me/at-risk-members`, không mở rộng bằng coachId client.

PATCH status của users/members/staff chỉ nhận Active/Inactive/Locked, trim và không phân biệt hoa thường; Suspended là alias Locked. Thiếu/null/rỗng/sai trả400 `INVALID_ACCOUNT_STATUS`, không đổi tài khoản hoặc ghi audit thành công. Tạo mới vẫn mặc định Active khi bỏ status; PUT bỏ status giữ trạng thái cũ, gửi status sai vẫn400. Guard role/target/center hiện có giữ nguyên.

PUT `/api/roles/{id}/permissions` giữ gate MANAGE_PERMISSIONS hiện hành; permissionIds phải là mảng ID dương tồn tại đầy đủ. Null/phần tử null/ID sai trả400; trường hợp service xác định ID sai dùng `INVALID_PERMISSION_IDS`, không thay một phần quyền. ID lặp được gộp, `[]` vẫn là thay bằng tập rỗng theo nghĩa API cũ. Thay toàn bộ tập dưới khóa role, audit actor và tập trước/sau cùng transaction. Không suy từ đó rằng guard quản trị viên cuối cùng hoặc policy đọc full matrix đã được chốt; GET lookup roles/permissions chưa đổi.

POST/PUT/PATCH room cấu hình yêu cầu Admin/CenterManager+MANAGE_CLASSES, Active và scope center hiện hành. Request/response thêm `activityType`: TENNIS, FOOTBALL, ARCHERY, BASKETBALL, VOLLEYBALL, PICKLEBALL, BADMINTON, TABLE_TENNIS hoặc SWIMMING. Đây là loại tài nguyên phục vụ môn, độc lập tên/roomCode và `facilityType` Indoor/Outdoor. Không thêm `subjectId`, không suy môn từ mã/tên. PUT bỏ/null giữ loại cũ; chưa cấu hình trả activityType:null và khi xếp lịch mới trả400 `ROOM_ACTIVITY_UNCONFIGURED`. Giá trị không hỗ trợ400 `INVALID_ROOM_ACTIVITY`; sai môn400 `ROOM_SUBJECT_MISMATCH`. Áp dụng book/create/generate/preview/check-conflict/available-slots có subject+room, reschedule và substitute; admission chỉ tính sân đúng môn và giữ điều kiện Indoor/Outdoor hiện hành. Quy tắc giờ Outdoor D04 không đổi.

Đổi activityType/capacity/facilityType khi còn buổi Scheduled từ hôm nay bị400 `ROOM_HAS_SCHEDULE`; đổi trạng thái unavailable giữ guard lịch cũ. Cấu hình room và ghi lịch cùng khóa center rồi room/record cần thiết, tránh race làm lịch dùng sân sai môn. Đổi tên/mã không đổi loại sân. Nâng cấp dữ liệu20sân dùng snapshot đã duyệt theo [docs02](02-DEPLOYMENT-GUIDE.md#phase1-migrations-20261109), không lấy ID fixture hoặc cấu hình suy từ màn hình.

Mapping dữ liệu để FE đối chiếu môn/tài nguyên, không dùng tên/mã hiển thị thay mapping này; BE luôn kiểm lại khi ghi:

| activityType | Subject.slug |
|---|---|
| TENNIS | quan-vot-tennis |
| FOOTBALL | bong-da |
| ARCHERY | ban-cung |
| BASKETBALL | bong-ro |
| VOLLEYBALL | bong-chuyen |
| PICKLEBALL | pickleball |
| BADMINTON | cau-long |
| TABLE_TENNIS | bong-ban |
| SWIMMING | boi-loi |

<a id="states"></a>
## 3. Field và state cần phân biệt

| Nguồn | Ý nghĩa / cách dùng |
|---|---|
| registration.status | Requested→Accepted→Invoiced hoặc Rejected; Invoiced chưa đồng nghĩa Paid |
| order.status / Invoice.paymentStatus | Trạng thái tiền; Paid có thể còn reconciliationRequired |
| subscription.status | Pending/Active/Cancelled/Expired/Completed, riêng từng đăng ký |
| order.expiresAt | Hạn trả tiền5phút |
| subscription.activatedAt/expiresAt | Thời điểm Paid hợp lệ/hạn45ngày sử dụng đã tính nghỉ TT; không tự cộng45ngày phía FE |
| Invoice.amount / subscription.price | Giá đóng băng khi mua; không tính lại từ catalog |
| remainingCredit=0 | Có thể vẫn còn quyền với ca hợp lệ đã book; tải lại nguồn quyền |
| Session.status | Scheduled/Cancelled/Completed, khác tiền/quyền và notice |
| packageId/subjectId/Coach/timestamps null | Có thể là legacy cần đối soát; không tự điền môn/Coach/12 credit |

Ngày `YYYY-MM-DD`, giờ **string** `HH:mm` hoặc `HH:mm:ss[.fraction]`. Business LocalDateTime không offset đọc theo Asia/Ho_Chi_Minh; chuỗi cóZ/+07:00 giữ nguyên. Timestamp lỗi chung có thể UTC/Z. Countdown chỉ để hiển thị, server quyết định cutoff. Response array khác Page.content; `[]`/null là trạng thái cần xử lý, không tự gán dữ liệu.

<a id="advanced"></a>
## 4. Đăng ký và mua Advanced — D03

**Phân biệt sản phẩm:** Normal là khóa học nhóm mua theo lớp; Advanced là quyền học1:1 theo môn/Primary Coach. Không có sản phẩm gói hội viên chung trước khi mua khóa. `MemberPackageResponse`/subscriptions vẫn là tên kỹ thuật cho quyền Advanced. Các API mua/gia hạn gói chung đã bị đóng tại BE bằng400 LEGACY_MEMBERSHIP_RETIRED; giữ tuyến GET quyền Advanced, không suy danh từ “package” là một sản phẩm hội viên khác.

| Bước / actor | API | Payload và kết quả |
|---|---|---|
| Manager/Admin+MANAGE_CLASSES | PUT `/api/coaches/{id}/work-schedule` | weeklySlots như MONDAY:1,daysOff,acceptingNew; khai lịch làm thực |
| Có phiên | GET cùng path; GET `/api/coaches/{id}/admission-capacity?subjectId=...` | Khả năng nhận dự kiến; không giữ ca/sân hoặc bảo đảm Member rảnh12buổi |
| Member | POST `/api/advanced-registrations` | subjectId,primaryCoachId,requestKey →Requested |
| Primary Coach; Manager hỗ trợ | POST `/{id}/decision` trong group trên | accepted; Manager thêm reason →Accepted/Rejected |
| Member chủ | POST `/{id}/order` | paymentMethod Cash/VNPay →SubscriptionOrderResponse;200 |
| Có phiên đúng phạm vi | GET `/api/advanced-registrations` | Registration+subscription state cho Member chủ/Primary Coach/Manager |
| Member chủ hoặc staff có quyền | GET `/api/members/{memberId}/subscriptions` | MemberPackageResponse; Member không được chọn người khác |

GET `/api/members/me/subscriptions` không được định nghĩa; lỗi400 khi dùng `me` thay ID số không phải lỗi đọc quyền. POST `/members/me/subscriptions` và `/member-packages/subscribe-me` là tuyến mua gói cũ đã đóng: request hợp lệ/đúng quyền trả400 LEGACY_MEMBERSHIP_RETIRED, không tạo đơn hoặc quyền học.

Chưa accepted thì COACH_ACCEPTANCE_REQUIRED. Order kiểm lại năng lực/center lock và snapshot Subject price. requestKey1–80ký tự `[A-Za-z0-9_.:-]`: retry cùng nội dung trả kết quả cũ; khác nội dung409 IDEMPOTENCY_CONFLICT. Không gửi amount/actor/time để thay dữ kiện BE.

Subscribe gói chung bị LEGACY_MEMBERSHIP_RETIRED; mua Advanced dùng advanced-registrations. Sau Paid tải ownInvoice và subscriptions; generic status/extend không được cấp lại quyền hoặc thay incident recovery. Các lỗi cần xử lý: COACH_NOT_ACCEPTING/COACH_SCHEDULE_REQUIRED/COACH_CAPACITY_INSUFFICIENT/COACH_SUBJECT_MISMATCH/MEMBER_NOT_ACTIVE/BOOKING_BLOCKED/INVALID_PRICE; tra response thực để lấy message.

POST `/api/classes/{classId}/enroll` và POST `/api/enrollments` chỉ nhận Normal. Truyền lớp Advanced trả400 USE_ADVANCED_REGISTRATION, không tạo enrollment/hóa đơn; danh sách chờ Advanced cũ không tự được thăng qua cơ chế Normal. Registration Advanced đi Requested→Accepted→Invoiced; Paid là trạng thái hóa đơn, sau xác minh hợp lệ quyền Advanced mới Active.

**Đóng thao tác gói chung:** các POST mua hộ `/api/members/{id}/packages` hoặc `/subscriptions`, POST `/api/packages`, PUT `/api/packages/{id}`, PATCH `/api/packages/{id}/status` trả400 LEGACY_MEMBERSHIP_RETIRED khi request hợp lệ/đủ quyền. Với subscription gói chung cũ, POST `/api/member-packages/{id}/extend` hoặc `/api/subscriptions/{id}/extend` và PATCH các alias `/{id}/status` cũng bị từ chối; subscription Advanced vẫn phải theo workflow thanh toán/sự cố, không được mở quyền qua generic extend/status. Tạo Member tại quầy với packageId khác null bị400 LEGACY_MEMBERSHIP_RETIRED trước khi tạo tài khoản; tạo tài khoản không kèm gói vẫn dùng luồng bình thường. Các guard401/403/validation vẫn áp dụng trước nghiệp vụ.

Sau purge, ID subscription cũ không còn tồn tại có thể trả404 khi extend/status phải tra cứu đối tượng; không tự tạo lại record để vượt lỗi. GET subscriptions hiện hành phục vụ quyền Advanced, không phải lịch sử gói chung đã xóa.

<a id="payments"></a>
## 5. Thanh toán, biên nhận — D10

| API / actor | Hành vi |
|---|---|
| POST `/api/invoices/{id}/confirm-cash` — Receptionist+MANAGE_INVOICES | Cash Pending còn hạn, đủ điều kiện mới Paid/cấp quyền; note optional;200 |
| POST `/api/invoices/{id}/pay-online` — Member chủ | Trả paymentUrl; kiểm lại Pending/deadline/method, không nhận giá |
| POST `/api/members/me/invoices/{id}/verify-vnpay` — Active Member chủ, cùng center | JSON `{}`; BE gọi QueryDR bằng attempt đã lưu;200 `VnPayVerificationResponse` bên dưới |
| GET `/api/members/me/invoices`, `/{id}` | Nguồn tiền chính thức của Member; dùng sau Return/poll hữu hạn |
| GET `/api/invoices/{id}/receipt` | Paid receipt; Member chủ hoặc staff có MANAGE_INVOICES đúng center |
| GET `/api/payments/vnpay/return` | Kiểm chữ ký/redirect; không ghi Paid |
| GET `/api/payments/vnpay/ipn` | Callback gateway có chữ ký, **không gọi từ FE**; có side effect |

Manager chỉ có MANAGE_INVOICES chưa đủ ROLE_RECEPTIONIST để confirm Cash. Paid+reconciliationRequired là đã ghi nhận tiền nhưng cần xử lý, không tự coi quyền Active. FE hiển thị Pending/chờ xác nhận, cập nhật hữu hạn rồi Kiểm tra lại; không lấy redirect làm chứng từ hoặc mời trả thêm khi đang đối soát.

**QueryDR:** `{id}` là `invoiceId` từ API, không phải phần số cuối `invoiceCode`. Gửi cookie phiên, `Content-Type: application/json`, body `{}`; không gửi amount, merchant, chữ ký hoặc kết quả gateway. Response200 có `verificationStatus` (bảng dưới), `message`, `retryAfterSeconds` (số giây tối thiểu chờ trước khi kiểm tra lại;0 khi không đề nghị retry) và `invoice` (đầy đủ `InvoiceResponse` như GET hiện hành).

| verificationStatus | FE xử lý |
|---|---|
| `VERIFIED_PAID`, `ALREADY_PAID` | Đọc `invoice.paymentStatus` và `reconciliationRequired`; nếu cần đối soát thì không thông báo đã cấp quyền |
| `NOT_CONFIRMED` | Chưa có xác nhận thành công; không suy là chưa trả hoặc mời thanh toán lại |
| `QUERY_UNAVAILABLE` | Chưa xác minh được (timeout/lỗi/chữ ký/dữ liệu không hợp lệ); giữ trạng thái hóa đơn |
| `RETRY_LATER` | Chờ `retryAfterSeconds`; BE giới hạn một lượt kiểm tra/30giây/hóa đơn, tối đa3attempt/lượt |
| `ATTEMPT_MISSING` | Đơn cũ thiếu dữ liệu pay gốc; hướng dẫn đối soát, không tự tạo đơn/thanh toán lại |
| `DISABLED`, `VERIFIED_ONLY` | Tính năng chưa bật hoặc đang kiểm chứng chỉ đọc; chưa đồng nghĩa invoice Paid |
| `NOT_APPLICABLE` | Đơn Refunded; theo hồ sơ xử lý hiện hành |

Khi quay lại trang hóa đơn, lấy `invoiceId` từ URL, đọc ownInvoice để kiểm đúng hóa đơn VNPay rồi tự POST xác minh một lần và GET lại danh sách. Có thể tái sử dụng nút **Tải lại** cho POST của hóa đơn đang theo dõi rồi GET; không cần nút “xác nhận đã trả”. Không tự POST tất cả hóa đơn, không poll vô hạn, không dùng `status=00` trên URL để tô Paid. GET `/me/invoices` vẫn chỉ đọc. Nếu IPN đã ghi Paid thì POST trả `ALREADY_PAID` và không query/cấp quyền lại. Xác minh tới/quá hạn vẫn có thể ghi Paid+đối soát nhưng không cấp lại suất/quyền.

Client nên dành60giây riêng cho POST này và chỉ giữ một lời gọi đang chạy trên UI. BE mặc định5giây/attempt, có thể cấu hình tối đa15giây/attempt; mỗi lượt thử tối đa3attempt. Nếu client timeout/mất kết nối, đọc lại hóa đơn và chờ cooldown trước kiểm tra tiếp; không suy là thanh toán thất bại.

Lỗi:401 chưa đăng nhập;403 sai role/không Active/thiếu hồ sơ Member;404 không có hoặc không sở hữu/khác center;400 hóa đơn không phải VNPay hoặc body có field;415 sai Content-Type;409 một mã giao dịch đã gắn hóa đơn khác. Schema mới phải migration trước khi deploy. Contract này là BE local; trạng thái deploy/kiểm live theo docs00.

<a id="booking"></a>
## 6. Coach tạo Advanced — D04/D12

POST `/api/sessions/book-private` →**201 SessionResponse**. Chỉ Primary Coach hoặc Manager/Admin+MANAGE_CLASSES hỗ trợ đúng center; Member/Receptionist không tự tạo.

Luồng mới gửi subscriptionId,coachId,roomId,subjectId,sessionDate,startTime,endTime,agreementConfirmed:true,requestKey; Manager thêm supportReason. classId optional tương thích; incidentId/extraValidityReason chỉ theo recovery hợp lệ. Không gửi actor/memberId/amount thay danh tính.

BE kiểm Paid/Active/credit, môn/Coach/member, lịch làm/tài nguyên,>=12h, Holiday/Outdoor và end<=deadline. Khung ca theo [D12](04-DEBUG-NOTE.md#d12). Retry cùng key/body trả cùng ca, không trừ lần hai. SessionResponse có centerInvalidated/makeupForSessionId/creditReturned/createdBy/agreementConfirmedAt; **không có remainingCredit**, FE đọc lại subscriptions.

Tra ca bằng GET `/api/coaches/{coachId}/available-slots?date=YYYY-MM-DD&subjectId=...&roomId=...`; alias `/api/sessions/coaches/...`. Response7ca có slotNumber/slotName/startTime/endTime/isAvailable/reason. Đó là tra tài nguyên, BE kiểm lại toàn bộ khi ghi.

<a id="cancellation"></a>
## 7. Hủy và báo vắng — D05

| API | Điều kiện / refresh |
|---|---|
| POST `/api/sessions/{id}/cancel-private` | Member chủ, Scheduled,>=12h;200 Cancelled, hoàn1credit gốc; refresh sessions/subscriptions |
| POST `/api/sessions/{id}/absence` body reason |Member chủ,0<notice<12h;200notice và cùng commit tạo attendance Absent; không hoàn credit |
| GET `/api/sessions/{id}/absence-notices` | Chủ/Coach được giao/Manager theo phạm vi; hiển thị notice tách kết quả attendance |

CANCEL_TOO_LATE/SESSION_ALREADY_CANCELLED/ADVANCED_PACKAGE_EXPIRED/OUT_OF_CREDIT cần thông báo phù hợp; retry hủy không hoàn thêm. Normal không hỗ trợ hủy/notice cá nhân (NORMAL_CLASS_NON_CANCELLABLE). FE không bỏ notice trước khi BE xác nhận kết quả dời.

Báo vắng trả `id,sessionId,memberId,reportedAt,reason,active,supersededAt` như trước; refresh notice và attendance sau200. Retry trong cửa sổ trả cùng notice, không nhân attendance hoặc đổi credit. Absent tự động ghi người báo là Member, có `absenceNoticeId` trong attendance; không ghi checkInTime hoặc tạo đánh giá giả của Coach. Coach/QR trong buổi có thể cập nhật Present; Manager correction sau buổi đồng bộ result hiện có. Đổi thời gian làm notice hết hiệu lực và lưu attendance tự động cũ với `superseded_at`; API attendance chỉ trả bản hiện hành, correction bản cũ400 `ATTENDANCE_SUPERSEDED`. Đổi phòng cùng giờ giữ notice/Absent. Sai owner/role/center403; ngoài cửa sổ400 `ABSENCE_WINDOW_CLOSED`. Không backfill hàng loạt notice lịch sử khi migration.

<a id="reschedule"></a>
## 8. Dời/thay Coach — D06/D07

POST `/api/sessions/{id}/reschedule`: newDate/newStartTime/newEndTime/newRoomId,cause,reason,memberAgreed,requestKey; recovery thêm incidentId/extraValidityReason. MEMBER_EARLY yêu cầu ca cũ/mới>=12h; MEMBER_EMERGENCY chỉ Manager, ca cũ còn0<12h/ca mới>=12h; CENTER_INCIDENT cần hồ sơ/quyền Manager. Không gửi extendValidityDays tùy ý để bỏ deadline.

POST `/api/sessions/{id}/substitute-coach`: Manager/Admin+MANAGE_CLASSES; newCoachId,reason,incidentId,memberAgreed,requestKey. Kiểm môn/lịch, không đổi Primary Coach gói. Dời/thay trả200 SessionResponse, lưu audit/consent cũ-mới; đổi phòng/Coach cùng giờ giữ notice, chỉ đổi ngày/giờ thành công mới supersede.

<a id="incidents"></a>
## 9. Sự cố và bù — D06/D08/D15

Group `/api/center-incidents`, Manager/Admin+MANAGE_CLASSES, active/đúng center:

| Method/path | Body / kết quả |
|---|---|
| GET group | Danh sách CenterIncidentResponse |
| POST group | sessionId,startedAt,reason,evidence,requestKey →WaitingForRecovery;200 |
| POST `/{id}/resolve` | endedAt →union pause/tính lại hạn, không cộng trùng |
| POST `/{id}/compensate` | reason,verifiedNotHeld:true →bù buổi thực sự không tổ chức do TT, chống lặp |
| POST `/{id}/normal-makeup` | date,startTime,endTime,roomId,coachId,reason →Session có link makeupForSessionId |

Snapshot originalDeadline/deadlineBefore/deadlineAfter/recoveredCredits giúp hiển thị tác động. Expired trong lúc chờ chưa book; restore balance gốc có chứng cứ, không reset12. Normal giữ buổi gốc invalidated và chỉ1makeup;13 record vật lý có thể chỉ12hợp lệ. Generic update/status/delete không dùng thay workflow này.

<a id="holidays"></a>
## 10. Holiday — D12/D14

GET `/api/holidays`, `/active` cho lookup có phiên. Manager/Admin+MANAGE_CLASSES:

1. POST `/api/holidays/preview`: tạo mới bằng holidayDate/holidayName/tag/active hoặc toggle bằng holidayId/active.
2. Hiển thị previewToken/blocked/affectedSessionIds/reconciliationSubscriptionIds/deadlineChanges. Blocked thì xử lý ca, rồi preview lại.
3. POST `/api/holidays` (201) hoặc PATCH `/{id}/toggle` (200), gửi **X-Preview-Token** lấy từ preview tương ứng.

HOLIDAY_PREVIEW_REQUIRED/HOLIDAY_PREVIEW_STALE/HOLIDAY_IMPACT_UNRESOLVED/HOLIDAY_SCOPE_REVIEW_REQUIRED không là thay đổi đã commit; tải lại dữ liệu/preview. Không tự dời/hủy hàng loạt hoặc tạo ngày nghỉ quá khứ để cộng quyền.

<a id="normal"></a>
## 11. Normal — 12 buổi cố định

Sĩ số tối đa là `maxCapacity` của lớp và phải vừa phòng, đồng thời không quá15; FE không hardcode mọi Normal đều15chỗ. Owner đã duyệt đồng bộ7lớp hiện có xuống sức chứa room mới qua bundle DB trong docs02. Không thay field/API, giá hay số buổi vì điều chỉnh này.

Ưu tiên POST `/api/classes/normal-with-sessions` với requestId,sportsClass,roomId,daysOfWeek,startTime,endTime →201 SportsClassResponse và12buổi cùng transaction. **requestId là UUID chuẩn36ký tự**, ví dụ `94edc3d1-697a-46fc-9475-c0f9828586cb`; không dùng nhãn tự đặt. sportsClass gồm name/subjectId/coachId/maxCapacity/startDate/classType:Normal/totalSessions:12. Retry cùng requestId/body cùng Class; conflict không để nửa lịch.

Tách bước: POST `/api/classes` tạo Normal Closed →POST `/api/sessions/preview-recurring` →POST `/api/sessions/generate` đủ12 →PUT Class Open. Không publish/enroll chỉ dựa ngày client; generate lại có SCHEDULE_ALREADY_EXISTS.

POST `/api/classes/{classId}/enroll` và POST `/api/enrollments` body paymentMethod, staff thêm memberId đúng quyền →201 EnrollmentResponse. Bỏ/null paymentMethod mặc định VNPay. Pending có invoiceId/invoiceCode/paymentStatus/amount/paymentUrl/expiresAt; Paid mới Registered. Lớp đầy có thể Waitlisted; maxCapacity≤15, Registered+Pending còn hạn giữ chỗ. CLASS_CAPACITY_EXCEEDED dùng khi giảm capacity dưới chỗ đang giữ, không mặc định lỗi mọi enrollment đầy.

**Normal Cash tại quầy:** chỉ Receptionist Active có cả REGISTER_MEMBER và MANAGE_INVOICES, cùng center, gửi memberId+paymentMethod:Cash qua hai tuyến trên. Cùng transaction tạo Invoice/Paid/Registered; response có status:Registered và paymentStatus:Paid. Member hoặc Manager chọn Cash bị403; không tạo đơn giữ Cash online. Không thu Cash cho Waitlisted (`NORMAL_CASH_NO_SEAT`). Gửi lại khi đã Registered trả ALREADY_ENROLLED, không nhân hóa đơn; không gọi confirm-cash lần nữa sau checkout nguyên khối. Advanced giữ flow Cash hiện hành.

**Normal độc lập với gói hội viên:** Member Active có thể đăng ký Normal dù subscriptions trả `[]`; không yêu cầu gói chung hoặc Advanced. Khi có chỗ tạo Pending VNPay và hóa đơn học phí của lớp; khi đầy vào Waitlisted. Xét chỗ trống chỉ mời đăng ký, giữ Waitlisted và không tạo hóa đơn/quyền; Member tới lượt gửi lại enroll VNPay hoặc được thu tại quầy. Chưa tới lượt trả WAITLIST_NOT_READY. Các điều kiện tài khoản, owner/center, lớp/lịch, sĩ số và trùng lịch vẫn áp dụng. Chuỗi mời lại tự động khi expire và phối hợp chuyển/hủy/Refund còn theo checkpoint Phase1, không coi lời mời là đã giữ chỗ.

**Giá Normal:** POST `/api/classes/{classId}/enroll` và POST `/api/enrollments` trả400 `INVALID_PRICE` khi học phí lớp thiếu hoặc≤0; không tạo enrollment/hóa đơn, kể cả nhánh Waitlisted. Xét danh sách chờ cũng kiểm học phí: giá không hợp lệ giữ Waitlisted, không cấp Registered. VNPay có chỗ →Pending; Paid hợp lệ mới Registered. Chứng từ Paid/amount và ghi danh lịch sử không bị tự sửa theo giá hiện tại.

**Lifecycle/admission bổ sung10/11:** `sportsClass.minCapacity` tùy chọn khi tạo; mặc định ceil(2×maxCapacity/3), chỉ tăng tới maxCapacity. Draft chưa công bố/chưa từng có đăng ký hoặc hóa đơn có thể chỉnh trong giới hạn; sau đó gửi giá trị khác trả NORMAL_MIN_CAPACITY_LOCKED. SportsClassResponse thêm minCapacity/openingConfirmedAt/openingConfirmedBy/postponementCount/lifecycleVersion/pricingDraft/changePending. Closed+pricingDraft=true là draft, không phải khóa đã hoàn tất.

Đăng ký chỉ khi Open đã công bố, đủ12buổi tương lai, **trước** giờ buổi đầu đã lưu trừ48h; đúng cutoff trả NORMAL_REGISTRATION_CLOSED. TTL VNPay là min(thời hạn cấu hình,cutoff); Pending hết hạn được giải phóng khi job hoặc transaction admission tiếp theo xử lý. Callback hợp lệ đến muộn vẫn Paid+reconciliationRequired, không hồi sinh chỗ. Chưa xử lý khoản đã nhận cần đối soát thì không tạo thêm đơn cùng lớp (`PAYMENT_RECONCILIATION_REQUIRED`). Return không ghi Paid.

POST `/api/classes/{id}/confirm-opening` chỉ CenterManager Active+MANAGE_CLASSES cùng center; body tùy chọn `{ "reason": "..." }`. Trước T−24h có thể bỏ reason; từ đúng T−24h tới trước T phải có reason1–2000ký tự, đủ sĩ số Paid+Registered và Coach/phòng/lịch hợp lệ. Chưa đủ trả NORMAL_INSUFFICIENT_PAID; đã tới T trả NORMAL_OPENING_TOO_LATE. Thành công200 SportsClassResponse lưu người/thời điểm; retry không nhân quyết định. Xác nhận không mở lại đăng ký. Job recheck rồi chuyển Ongoing khi đến T; chưa xác nhận hoặc điều kiện sai thì cảnh báo, không tự khai giảng/hủy. Finalizer đóng Closed sau12buổi hợp lệ đã chốt, giữ cửa correction D15. PUT không tự gán Open/Ongoing/Closed/Cancelled ngoài công bố draft; type bất biến, ngày Class theo lịch đã lưu (`NORMAL_LIFECYCLE_ACTION_REQUIRED`, `CLASS_TYPE_IMMUTABLE`, `NORMAL_SCHEDULE_ACTION_REQUIRED`).

**Chưa có API mới cho dời khai giảng/chuyển/hủy trước khai giảng/Refund Normal đầy đủ.** Không dùng generic PUT, đổi Session hoặc PaymentResolution cũ để thay các hành động này. Sáu bảng dự kiến trong SYSTEM-DATA không là DTO/API. Trạng thái và bước triển khai còn lại tại [checkpoint Phase1](03-TODO.md#normal-phase1-foundation-20261110).

**RP-13 — giá lúc tạo/công bố, Phase1 09/11:** POST `/api/classes`, POST `/api/classes/normal-with-sessions` và PUT Class yêu cầu Admin/CenterManager+MANAGE_CLASSES, Active và center hợp lệ. Normal nhận tuitionFee tùy chọn: bỏ/null lúc tạo lấy Subject.normalPrice; gửi giá công bố dương, biểu diễn được NUMERIC(12,2), ví dụ950000 thì Class và đơn Normal mới dùng950000. Không nhận giá bán từ amount do Member gửi. Tạo Normal riêng luôn Closed; đủ12buổi hợp lệ mới công bố Open. Draft mới chưa từng công bố/chưa có enrollment hoặc invoice có thể chọn giá lúc PUT công bố lần đầu; bỏ giá khi PUT giữ giá đã chọn. Subject đổi giá không tự đổi Class đã tạo, đơn Pending/Paid hay purchaseAmount cũ. Advanced vẫn lấy Subject.advancedPrice khi order.

Lớp legacy không có chứng cứ chưa công bố và lớp đã công bố/có đơn không được tự mở quyền định giá lại. PUT Normal gửi giá khác trong nhóm này trả409 `CLASS_REPRICING_NOT_APPROVED`, giữ giá/lịch/chứng từ và không audit thành công. Metadata cùng giá hoặc bỏ giá tiếp tục theo guard hiện hành. Điều kiện đổi giá sau công bố/đã có Pending/người học chờ Owner review riêng. Gửi lại enroll hiện có giữ400 `ALREADY_ENROLLED`, không tạo đơn mới; khác với retry requestId của tạo lớp nguyên khối.

GET `/api/sessions/catalog/{classId}` là lịch Normal trước mua cho người có phiên, không lộ private Member/subscription. Lịch cá nhân qua `/api/sessions` bị ép owner/Coach/center theo phiên. Normal chỉ bù do TT theo mục9; không hủy/notice cá nhân.

**Coach đọc roster/enrollment:** GET `/api/classes/{classId}/enrollments` và GET `/api/enrollments?classId=...` cùng cho Coach Active đọc đúng lớp có `classes.coach_id` là mình và cùng center; sai lớp/khác center trả403 kể cả có thêm authority quản trị. GET `/api/enrollments` bỏ classId chỉ trả lớp được giao; filter memberId/status chỉ thu hẹp kết quả. Tuyến `/api/members/{id}/enrollments` và alias `/classes` cũng lọc theo phân công khi actor Coach. Lớp/buổi đã kết thúc không tự làm mất quyền đọc roster của Coach vẫn được phân công; quyền ghi attendance vẫn theo[start,end) và correction theo D15. Quyền đọc này không cấp Coach quyền POST/PATCH/DELETE enrollment; không giải quyết403 bằng cấp REGISTER_MEMBER/MANAGE_USERS rộng hơn.

<a id="attendance"></a>
## 12. Attendance và QR — D15/D17

| API / actor | Hành vi BE attendance (trạng thái kiểm/deploy ở docs00) |
|---|---|
| GET `/api/attendances/me/checkin-code` — Member | Chỉ payload/expiresInSeconds; TTL còn lại1–30 giây |
| GET `/api/training-results` — Member Active; Coach hoặc MANAGE_CLASSES/MANAGE_USERS theo quyền hiện có | Filter tùy chọn `sessionId`, `memberId`, `coachId`. Member luôn chỉ đọc mình từ phiên; `memberId` khác →403; không có kết quả →200 `[]`. Coach bị ép `coachId` theo phiên |
| POST `/api/attendances/scan` — staff/Coach có quyền | memberId,code,sessionId; Coach được giao, Paid, trong[start,end), bước hiện tại/chống replay |
| POST `/api/training-results`, `/bulk` — Coach được giao | Manual result/attendance chỉ trong buổi, Present/Absent mới; note tùy chọn, response ghi trả note đã lưu |
| POST `/api/attendances/correct` — Coach được giao+RECORD_RESULT hoặc Manager/Admin+MANAGE_CLASSES | attendanceId **hoặc** sessionId+memberId, newState,reason,note; quyền/hạn và lỗi theo contract09/11 dưới đây |

Alias `/api/attendance` và `/checkin-code`/`/api/members/me/checkin-code` còn để tương thích; chọn1canonical trên FE. QR phải refresh khi hết hạn/foreground, không log payload/secret. Code trong bằng chứng đã redact, không dùng scan. Check-in/out cửa có code nhưng toggle mặc định tắt, không thay attendance ca. Giữ lịch sử Late/Excused; finalizer sau end không tính buổi invalidated và không trừ credit lần hai.

Tên “Check-in/Điểm danh” trong bản thiết kế gốc từng chỉ attendance theo buổi với phạm vi30phút trước/sau. Attendance hiện tách check-in cửa; mốc correction mới Owner chốt09/11 được đặc tả riêng bên dưới. Chọn endpoint theo chức năng và phiên bản đã triển khai, không suy nghĩa từ riêng tên nút. D15 không tự hạn chế thời gian ghi nhận tiến độ/đánh giá chuyên môn ngoài attendance.

**Chọn luồng ghi:** POST `/api/training-results` hoặc `/bulk` chỉ ghi trong buổi, Present/Absent và đúng sessionId; sau end dùng correct theo quyền/hạn09/11 dưới đây. Check-in cửa mặc định tắt (400 LOBBY_CHECKIN_DISABLED), root attendances không có POST (405). Không thử API dự phòng để vượt lỗi hoặc coi GET200 sau POST lỗi là ghi thành công. Gợi ý FE xác nhận lưu sau response ghi thành công/readback; cách thể hiện do FE quyết định.

Ngữ cảnh dữ liệu khi đổi lớp/buổi gồm sessionId, attendance, note và record sửa; roster cùng lớp có thể giống nhau nhưng state/attendanceId/note thuộc từng buổi. Gợi ý FE đồng bộ dữ liệu/form với lựa chọn và tránh áp response cũ hoặc gửi record buổi cũ trong lúc tải; cơ chế UI do FE chọn.

**Ghép record trên màn hình attendance:** GET `/api/attendances?sessionId={id}` là record điểm danh của đúng buổi trong phạm vi actor; chọn record bằng sessionId+memberId và giữ attendanceId riêng khi correction. Không dùng attendanceId như sessionId hoặc lấy trạng thái gần nhất của Member qua buổi/lớp khác. Buổi chưa có record không mặc định Present. GET `/api/sessions/catalog/{classId}` chỉ trả session Scheduled của lớp Normal đã công bố, không là lịch sử đầy đủ để dựng màn hình quản lý attendance; dùng GET `/api/sessions?classId={id}` hoặc `/{sessionId}` có phiên cho thông tin ngày/giờ/status trong phạm vi được phép. OPTIONS200 là preflight; GET tải lại200 sau POST403 không là ghi thành công.

**Tách dữ liệu:** content nằm ở training_results.content; note nằm ở attendances.note và được trả cho đúng owner/scope. reason của correct chỉ lưu audit nội bộ, không echo thành note hoặc trả trong AttendanceResponse. Lý do báo vắng gốc giữ ở notice qua absenceNoticeId; không backfill/parse audit thành note. Khi ghi trong buổi, bỏ content giữ content chuyên môn đã có.403 không ghi mutation/reason; badge FE hoặc Payload không là xác nhận đã lưu. Không mở audit-log toàn cục cho Coach.

<a id="attendance-correction-20261109"></a>
**Contract Owner09/11 — BE đã triển khai, phạm vi kiểm/deploy xem docs00:** [Bàn giao flow và gợi ý FE](../SCMS-BAN-GIAO-FE-ATTENDANCE-2026-11-09.md); bố trí UI/câu chữ do FE quyết định, các giới hạn dưới đây được BE thực thi.

| Method/path | Input/actor và hành vi |
|---|---|
| POST `/api/training-results` hoặc `/bulk` | Giữ ghi trong `[start,end)`; thêm note tùy chọn (bulk trên từng item), không đổi tên attendanceStatus/content hoặc mở quyền chuyên môn |
| POST `/api/attendances/correct` | Chọn đúng một cách định danh: attendanceId **hoặc** sessionId+memberId. newState:Present/Absent, reason bắt buộc, note tùy chọn. Coach Active+RECORD_RESULT, được giao/cùng center, chỉ từ end tới trước00:00 ngày kế tiếp ngày buổi. Quản lý theo quyền correction hiện hành sau end, không áp cutoff Coach.200 trả AttendanceResponse sau commit |
| GET `/api/attendances?sessionId=...` | Giữ scope hiện hành, trả thêm note nullable; Member chỉ đọc bản ghi của mình. Không trả reason/audit qua note |

Correction theo sessionId+memberId tìm/upsert record hiện hành dưới khóa khi finalizer chưa tạo, kiểm Member thuộc đúng buổi và quyền học tại buổi; không suy thiếu bằng chứng lịch sử hoặc chặn chỉ vì finalizer vừa đổi enrollment/entitlement thành Completed. Không cho sửa superseded/buổi hủy/centerInvalidated. Đồng bộ attendance_status của TrainingResult nếu có, không tạo content chuyên môn giả, không đổi credit/lịch/hạn/checkInTime gốc hoặc giả timestamp tới lớp. Cùng transaction lưu state/note và audit actor/time, trước-sau, reason; retry/no-op không tạo record/thông báo trùng.

| Field | Kiểu/ngữ nghĩa |
|---|---|
| note (request) | string nullable/optional; plain text, tối đa1000ký tự sau trim. Bỏ/null giữ nguyên; rỗng sau trim xóa có audit. Ghi mới không có note lưu null |
| note (response) | string nullable, giá trị đã lưu; thêm vào AttendanceResponse và response ghi training-results/bulk. Không lấy từ reason hoặc content. Giữ absenceNoticeId/notice.reason gốc |
| reason (correct) | string bắt buộc, không trắng, tối đa1000ký tự sau trim; kể cả chỉ sửa note bằng newState hiện có. Lưu audit nội bộ, không tự công bố cho Member |
| correctionAllowed | boolean theo actor/resource trên response attendance và session có phiên phục vụ màn điểm danh; chỉ nói quyền dùng correct lúc BE đánh giá, không phải quyền ghi trong buổi hoặc quyền được bảo đảm ở lần POST sau |
| correctionDeadline | datetime có offset+07:00, exclusive00:00 hôm sau của ngày buổi, biểu diễn hạn Coach; không có hạn Coach áp dụng thì null. Member có thể nhận hạn đối soát của buổi để diễn giải kết quả, không được quyền ghi |
| serverTime | datetime có offset+07:00 tại BE để đối chiếu deadline; BE vẫn kiểm Clock ở mutation dù UI đã mở form |

| HTTP/code | Ý nghĩa |
|---|---|
|400 INVALID_ATTENDANCE_STATE | State mới ngoài Present/Absent |
|400 NOTE_TOO_LONG / REASON_REQUIRED / REASON_TOO_LONG | Note/reason không đạt giới hạn; thiếu/trắng reason correction |
|400 INVALID_ATTENDANCE_TARGET | Thiếu/mâu thuẫn hai cách định danh hoặc sessionId+memberId không đầy đủ |
|400 USE_SESSION_ATTENDANCE | Gọi correct trước end, dùng luồng ghi trong buổi khi đủ điều kiện |
|403 ATTENDANCE_CORRECTION_FORBIDDEN | Không đúng quyền/phân công; thông báo rõ không thể sửa/ghi chú buổi không được giao, không tiết lộ dữ liệu ngoài scope |
|403 ATTENDANCE_CORRECTION_DEADLINE_PASSED | Coach đã đạt scope nhưng hết hạn, liên hệ quản lý; không gán mã này cho mọi403 |

Giữ code SESSION_NOT_HELD/ATTENDANCE_SUPERSEDED cho buổi không tổ chức/record superseded và401/403 tài khoản hiện có; xác minh quyền/phạm vi trước thông báo thời gian. Trạng thái Render xem docs00; không suy từ source local thành đã deploy.

Finalizer vẫn hoàn tất buổi/lifecycle tại end và có thể tạo Absent thiếu; kết quả còn có thể điều chỉnh tới cutoff. NO_SHOW_RECORDED/chế tài vắng xử lý sau cutoff bằng state hiện hành, chống lặp; job trễ không kéo dài quyền Coach. Giữ chế tài default tắt. Ví dụ buổi09/11 kết thúc07:30, tự Absent rồi Coach sửa Present09:00: không gửi báo vắng khi job xử lý sau00:00 ngày10/11. Đây không là gia hạn giờ học hoặc credit.

**Gợi ý FE khi chưa nối note:** API đã lưu không chứng minh UI đã hiển thị. Có thể dùng note đã đọc để điền form, gửi note khi lưu và hiển thị giá trị server sau thành công/tải lại; Member đọc note của đúng buổi. note:null là chưa có ghi chú; thiếu field note là cần đối chiếu phiên bản contract/build, không tự kết luận DB chưa lưu. Caption/form/badge và cơ chế tải lại do FE quyết định; đối chiếu OpenAPI bàn giao và build BE đang dùng; không suy BE local thành Render đã deploy.

Ngoại lệ ghi trước buổi duy nhất ở đây là Absent do Member báo vắng hợp lệ theo D05; manual/QR vẫn tuân thủ cửa sổ trong buổi. Attendance response thêm `absenceNoticeId` nullable để truy về notice/reason, kể cả sau khi Coach xác nhận kết quả thực tế.

GET training-results trả danh sách `TrainingResultResponse`: resultId,sessionId,sessionDate,classId,className,memberId,memberName,coachId,coachName,content,attendanceStatus,recordedAt,note. FE Member có thể bỏ `memberId`; mọi filter còn lại chỉ thu hẹp dữ liệu cá nhân. Thiếu phiên →401; tài khoản Member không còn Active/đúng role hoặc cố đọc Member khác →403. Member mặc định không được POST/bulk; không cấp thêm quyền quản trị để xử lý lỗi đọc. Kết quả kiểm và giới hạn môi trường ở docs00; GET không thay dữ liệu.

Member đọc toàn bộ content của kết quả thuộc mình. GET `/api/training-plans` (coachId,classId,memberId tùy chọn) và `/api/training-plans/{id}` đọc kế hoạch; GET `/api/evaluations` (memberId,coachId tùy chọn) và `/api/evaluations/{id}` đọc đánh giá/comment/progressScore. Service kiểm ownership của Member; danh sách không có dữ liệu →200 `[]`, yêu cầu dữ liệu Member khác →403. Không suy các mô tả này là nghiệm thu toàn bộ Coach/role/owner của BE-03/BE-04.

**Ghi giáo án/đánh giá — RP-36:** POST/PUT/DELETE `/api/training-plans` (PUT/DELETE thêm `/{id}`) yêu cầu ROLE_COACH AND MANAGE_TRAINING_PLAN; tương tự `/api/evaluations` yêu cầu ROLE_COACH AND RECORD_RESULT. MANAGE_CLASSES không thay authority chuyên môn; Admin/Manager/Receptionist/Member có thêm authority ghi vẫn403. Service kiểm lại role/authority, actor Active, cùng center và Coach được phân công, kể cả khi không gọi qua controller. Coach chỉ ghi record mang coachId của mình; plan theo classId phải là lớp mình được giao; plan/evaluation theo memberId cần quan hệ khóa Normal Registered+Paid hoặc quyền Advanced đã kích hoạt/Paid gắn Primary Coach đó. Sửa/xóa kiểm record đã lưu, không tin coachId client để chọn phạm vi. Cố chuyển Coach/class/member qua PUT trả400 TRAINING_TARGET_IMMUTABLE; plan PUT có thể bỏ classId/memberId để giữ target cũ. Audit ghi actor thực; quyền Manager correction attendance D15 không mở sang ghi chuyên môn.

**Member đọc giáo án lớp:** GET `/api/training-plans` và `/{id}` giữ đọc plan gán trực tiếp đúng Member, đồng thời đọc plan chung của đúng lớp Normal có enrollment Registered và invoice Paid hợp lệ (không đối soát/không đảo xác nhận Cash), cùng center. Plan lớp không cần memberId; lọc bằng quan hệ Member→enrollment→Class, không nhân bản nội dung cho mỗi người. List chỉ trả phạm vi hợp lệ; ID plan lớp/Member khác hoặc thiếu quyền lớp403; filter memberId khác403. Pending/Waitlisted/Cancelled hoặc invoice đang đối soát không cấp quyền đọc chung. Plan riêng của Member giữ rule lịch sử hiện có, không tự áp rule lớp để xóa quyền đọc riêng. Các ca hủy/đảo quyền/lịch sử ngoài phạm vi này vẫn cần đối chiếu, không cam kết mọi trường hợp.

<a id="normal-changes-20261110"></a>
## Normal dời/chuyển/hủy trước khai giảng

Manager Active+MANAGE_CLASSES, cùng center; dùng version hiện tại, không gán status qua PUT. Thành công200; [bàn giao riêng](../SCMS-BAN-GIAO-FE-NORMAL-LIFECYCLE-2026-11-10.md).

| Method/path | Input và xử lý |
|---|---|
| GET `/api/classes/{id}/normal-proposals` | Manager xem lựa chọn; Member chỉ proposal có Invoice của mình và phản hồi chính mình. schedule là snapshot cụ thể, không tính lại theo lịch mới |
| POST cùng path | kind:Postpone/shiftDays/version/reason hoặc kind:Transfer/targetClassId/version/reason. Postpone do thiếu sĩ số, tối đa1lần; Transfer cùng môn/giá/center. Trả id/version/fingerprint/schedule/choices; preview không giữ chỗ |
| POST `/api/normal-proposals/{id}/respond` — Member | version/fingerprint/choice:Accept hoặc Refund. Accept Transfer khóa/recheck và commit quyền ngay; Refund mở nghĩa vụ và rút quyền cũ. Accept Postpone là consent chờ apply. Retry cùng lựa chọn không nhân quyền; đổi lựa chọn đã ghi409 |
| POST `/api/normal-proposals/{id}/apply` — Manager | version; mọi Member phải phản hồi. Postpone recheck rồi áp lịch/cutoff, đặt lại confirmation; không tự khai giảng. Transfer đã chuyển từng Member; lớp gốc còn chặn nhận mới/chạy cho tới khi Manager xử lý hủy |
| POST `/api/classes/{id}/cancel-before-opening` — Manager | version/reason, chỉ Normal Open trước buổi đầu. Hủy Pending/waitlist/lịch, tạo Refund từng khoản Paid đủ điều kiện; Ongoing bị chặn |

changePending=true chặn admission/confirm/job start. Im lặng không là đồng ý; nhắc Member theo ngày VN có dedupe. Waitlist hết hạn mời/order từ hàng chờ quay cuối hàng, job mời tiếp, không autoCash; TTL mời cùng payment.expire-minutes, chưa là quyền học.

Sau chuyển, Invoice.classId/className/amount giữ lớp mua gốc; entitlementClassId chỉ lớp nhận quyền hiện tại (bằng classId khi chưa chuyển). Enrollment cũ Cancelled có link/lý do, đích Registered; lịch/attendance/plan/result/report nhận quyền đích và giữ lịch sử. Không thanh toán lần nữa. NORMAL_CHANGE_PENDING/NORMAL_PRESTART_REQUIRED/NORMAL_POSTPONEMENT_LIMIT/NORMAL_PROPOSAL_STALE/MEMBER_RESPONSES_PENDING/NORMAL_CHOICE_ALREADY_RECORDED/NORMAL_TRANSFER_INCOMPATIBLE/NORMAL_RECEIPT_NOT_ELIGIBLE/CLASS_FULL/REFUND_ALREADY_EXISTS trả400/409 theo response;409 tải lại version/lịch/chỗ. Lịch đề xuất đã đổi chặn Accept cũ nhưng không tước lựa chọn Refund của Member.

<a id="normal-refunds-20261110"></a>
## Refund Normal trước khai giảng — ngân hàng thủ công

[Bàn giao FE riêng](../SCMS-BAN-GIAO-FE-NORMAL-REFUND-2026-11-10.md). Nguồn hồ sơ CenterCancellation/MemberChoice/LateReceipt sau TT hủy; Pending chưa thu không phát sinh chuyển hoàn. Không áp Member tự hủy lớp đang vận hành hoặc sau Ongoing. Paid/amount gốc giữ nguyên, không có lệnh chuyển ngân hàng tự động. Thành công200.

| Method/path trong `/api/normal-refunds` | Actor/input/kết quả |
|---|---|
| GET collection hoặc `/{id}` | Member chủ; Manager tài chính cùng center; Receptionist tài chính chỉ việc Approved trở đi. View có amount/status/version/receiverId/receiverVerified/dueAt/firstDueAt/overdue/issueNote/attempts; Member nhận accountNumber che phần đầu, contactPhone chỉ staff đúng quyền để FE tùy dùng |
| POST `/{id}/receiver` | Member: bank/accountNumber/accountName/version. Tạo phiên bản mới và hủy duyệt cũ; cấm khi claim chưa đối soát hoặc Completed |
| POST `/{id}/verify-receiver` | Manager+MANAGE_INVOICES: version/source xác minh Member/người nhận; chưa duyệt chuyển |
| POST `/{id}/approve` | Manager: version; receiver đã xác minh, amount từ Invoice. Approved và SLA7ngày TT mở tính ngày mở tiếp theo; giữ firstDueAt cũ |
| POST `/{id}/request-information` | Manager: version/note; yêu cầu sửa thông tin, giữ nghĩa vụ/lịch sử |
| POST `/{id}/claim` | Receptionist+MANAGE_INVOICES: version/requestId UUID. Một attempt với amount/receiver đã duyệt; retry key cũ trả attempt cũ. Không tự hết claim rồi chuyển lại khi bank chưa rõ |
| POST `/{id}/attempts/{attempt}/submit` | Receptionist đã claim: reference/amount/currency:VND/performedAt, đúng amount/from claim tới now; reference duy nhất, metadata đã nộp không ghi đè. Attempt Submitted/hồ sơ AwaitingVerification |
| POST `/{id}/attempts/{attempt}/documents` | Receptionist đã claim: multipart file PNG/JPEG/PDF đúng MIME/signature, tối đa1MiB theo multipart HTTP mặc định hiện hành; append-only, retry cùng bytes giữ ID; chưa Completed |
| GET `/{id}/documents/{document}` | Manager/Receptionist tài chính cùng center; attachment bytes/no-store/private/nosniff, Member/Coach/Admin403. Không public URL/avatar |
| POST `/{id}/attempts/{attempt}/verify` | Manager: version/outcome:Success,Pending,Failed,Reversed/bankSource/note. Success cần proof và reference/amount/receiverId/performedAt khớp nguồn bank độc lập mới Completed. Pending giữ NeedsReview, Failed/Reversed giữ lịch sử và phải duyệt lại trước attempt khác |
| GET `/{id}/history` | Chỉ Manager tài chính cùng center; các receiver/nguồn xác minh và từng lần đối soát cũ |
| POST `/{id}/issues` | Member chủ hoặc staff đúng quyền: version/note; tiếp nhận khiếu nại cả Completed, không tự chuyển lần nữa |

State: AwaitingReceiver→AwaitingApproval→Approved→Processing→AwaitingVerification→Completed; NeedsReview khi bank chưa rõ. Approved/upload không là đã hoàn. Version sai409 REFUND_VERSION_STALE; claim/receiver xung đột409; amount/receiver/proof/time chưa đúng400; role/owner/center403. Tài chính yêu cầu role AND MANAGE_INVOICES; Member chỉ các action nêu trên.

Báo cáo trừ Refund Completed theo performedAt, giữ kỳ thu Invoice riêng; Pending/Approved/NeedsReview/CashCorrection không bị trừ như hoàn. approve/complete cũ bị USE_NORMAL_REFUND_WORKFLOW với hồ sơ mới; D10 tiền đến muộn chưa cấp quyền và D16 Advanced CashCorrection giữ phạm vi. State/data/source đối soát giữ lịch sử, nhắc thiếu thông tin/quá hạn không tự xóa nghĩa vụ. Trạng thái kiểm/deploy ở docs00/QA.

<a id="resolutions"></a>
## 13. Refund/CashCorrection — D10/D16

CashCorrection chỉ xử lý hóa đơn Cash đã bị xác nhận Paid khi **chưa nhận tiền thực cho hóa đơn đó**, trong phạm vi Advanced chưa dùng của D16. Xác nhận Cash dùng invoiceId cụ thể, không dò loại khóa theo giá. Nếu có tiền đã nhận nhưng phân bổ sai hóa đơn, cần xác minh hóa đơn nào thực sự được thu; không tự chuyển tiền hoặc khai moneyNotReceived khi đã thu cho chính hóa đơn đang sửa. Refund áp dụng khoản đã nhận và đủ điều kiện hoàn theo D10.

Đăng ký Normal có hóa đơn riêng không phụ thuộc quyền Advanced chỉ vì tạo sau thời điểm kích hoạt Advanced; Pending hay Paid đều không tự chặn CashCorrection. BE vẫn chặn khi có liên kết `eligibility_subscription_id` tới đúng quyền đang sửa, booking history, hóa đơn khác cùng quyền hoặc lịch sử gia hạn/expiry/sự cố. Kiểm lại khi Complete dưới lock; không đổi hóa đơn/ghi danh Normal độc lập.

Group `/api/payment-resolutions`: GET/POST cho Receptionist/Manager/Admin+MANAGE_INVOICES theo phạm vi; approve/complete chỉ Manager/Admin.

| Role | GET danh sách / POST tạo | POST approve / complete |
|---|---|---|
| Member, Coach | Không, kể cả có MANAGE_INVOICES | Không |
| Receptionist + MANAGE_INVOICES | Có, trong center của actor Active | Không |
| CenterManager + MANAGE_INVOICES | Có, trong center của actor Active | Có, cùng center và đủ điều kiện nghiệp vụ |
| Admin + MANAGE_INVOICES | Controller cho phép; service vẫn giới hạn center, không có ngoại lệ toàn hệ thống | Controller cho phép; service vẫn kiểm center và điều kiện nghiệp vụ |

**Admin mặc định:** schema hiện hành không gán MANAGE_INVOICES và có center NULL, nên mặc định403 ở controller. Nếu được cấp thêm authority nhưng center vẫn NULL: GET chỉ truy vấn center NULL, không phải toàn hệ thống; ghi dừng400 `CENTER_REQUIRED` trước xử lý hồ sơ. Không suy tên role Admin là được vận hành tài chính. Đây là mô tả implementation hiện hành, chưa là quyết định mở quyền mới; không tự gán quyền hoặc center trên DB thật.

Member/Coach bị403 khi gọi nhóm payment-resolutions; không có API đọc hồ sơ resolution cá nhân dành Member trong contract hiện hành. Quyền được xác định bằng role AND MANAGE_INVOICES, cùng kiểm Active/center và điều kiện nghiệp vụ tại BE.

| API | Body / state |
|---|---|
| POST group | invoiceId,type:Refund hoặc CashCorrection,reason →Open;200 |
| POST `/{id}/approve` | Không body; Approved hoặc Blocked; chưa ghi đã hoàn |
| POST `/{id}/complete` Refund | evidence,performedAt,moneyNotReceived:false; thời gian receipt<=performedAt<=now |
| POST `/{id}/complete` CashCorrection | evidence,moneyNotReceived:true; kiểm lại điều kiện hẹp D16 |

PaymentResolutionResponse có id/invoiceId/type/status/amount/reason/evidence/outcomeNote/createdAt/approvedAt/performedAt/completedAt. Refund chỉ hoàn đủ khoản Paid cần đối soát/chưa cấp quyền, thực hiện bên ngoài có chứng cứ. CashCorrection không phải Refund, không Normal hoặc booking history kể cả đã hủy. Giữ Paid/amount gốc, không tạo hoàn giả; retry không tạo case/kết quả lặp.

<a id="reports"></a>
## 14. Báo cáo — D13/D18

GET `/api/reports/dashboard`, `/members`, `/revenue?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&groupBy=month`: CenterManager/Admin+VIEW_REPORTS, active/phạm vi center; Coach403. EndDate bao gồm cả ngày qua[start,nextDay).

| Field | Nghĩa |
|---|---|
| moneyReceived | Tiền thực thu theo paymentDate, gồm khoản nhận cần đối soát, loại Cash đã xác minh chưa nhận |
| moneyRefunded | Refund Completed theo performedAt, không tính chỉ Approved |
| netReceived | moneyReceived−moneyRefunded, có thể âm |
| cashCorrectionsRecorded | Điều chỉnh Cash trong completedAt, không phải tiền hoàn |
| totalRevenue và breakdown cũ | Gross Paid tương thích, không thay các chỉ số thực thu/hoàn/ròng |
| activeMembers | Người còn quyền;0 credit còn ca hợp lệ vẫn có thể active |
| activeSubscriptions | Tổng lượt Advanced còn quyền + ghi danh Normal còn quyền; không phải số người. Advanced phải Paid hợp lệ, Active, đủ metadata, chưa hết hạn và còn credit hoặc ca hợp lệ; Normal Registered/Paid còn ca hợp lệ hoặc đang chờ bù TT |
| expiredMembers | Alias previouslyEntitledMembersWithoutRights; không gom neverEntitledMembers |
| expired/completed/reconciliationRegistrations | Từng lượt đăng ký, không cộng vào số người |

FE tự quyết layout/chỉ số chính. Dữ liệu legacy thiếu provenance tách reconciliation; người có nhiều đăng ký chỉ đếm1ở chỉ số người.

Dashboard không nhận khoảng ngày và lấy toàn kỳ; revenue áp dụng khoảng ngày đã gửi. Query hiện lọc theo center của actor, kể cả Admin: Admin center NULL trả dữ liệu rỗng, không tự tổng hợp toàn hệ thống. Chức năng chọn center/tổng hợp cho Admin chưa thuộc contract này; không gán Admin center1 để chữa số liệu0.

<a id="audit-scope-20261109"></a>
### RP-43 — nhật ký hoạt động, Phase1 09/11

GET `/api/audit-logs` yêu cầu Admin/CenterManager AND VIEW_AUDIT_LOG, tài khoản Active. Admin đọc loại SYSTEM_SECURITY; Manager đọc BUSINESS trong center của mình. Coach/Member/Receptionist có thêm authority vẫn403. Filter chỉ thu hẹp scope: startDate/endDate bao gồm trọn ngày Asia/Ho_Chi_Minh, action/targetEntity khớp chính xác, actorId là người ghi sự kiện. Không có tham số cho client chọn center hay mở loại log khác.

Response200 vẫn là **mảng**, mới nhất trước theo createdAt rồi id giảm dần; `page` từ0 mặc định0, `size`1–100 mặc định50. Header `X-Total-Count`, `X-Page`, `X-Page-Size` sau scope/filter, đã expose qua CORS. Không đọc như Page.content và không mặc định một trang là toàn bộ dữ liệu. Filter/range/page sai400 `INVALID_AUDIT_FILTER` hoặc400 binding của framework.

Giữ id/userId/action/targetEntity/targetId/detail/createdAt; thêm category/centerId/scopeSource chỉ đọc. Sự kiện mới xác định center từ target tin cậy ngay trong transaction, lưu snapshot `TARGET_AT_EVENT`; cấu hình toàn cục dùng `GLOBAL_CONFIGURATION_AT_EVENT`. Không dùng center hiện tại của actor để gán lịch sử. Log legacy hoặc target không đủ nguồn được giữ DB với provenance chưa rõ và **không trả qua API scope này**; đây là giới hạn dữ liệu, không phải đã xóa hoặc đã xác minh phạm vi cũ. Bulk generate mới trỏ target CLASS/id của lớp; xóa session chụp provenance trước xóa. Report/dashboard theo D13/D18 ở trên, không đổi Admin-null thành report toàn hệ thống.

## 15. Notification, chat và nhóm bổ trợ

Notification là feed trong ứng dụng, không phải email/push. Mọi role có phiên đọc dữ liệu của mình: GET `/api/notifications/me` theo page/size/unreadOnly/type →**Page.content**, size tối đa 100; GET `/api/notifications/me/unread-count`; PATCH `/{id}/read` và `/me/read-all` trong group notifications →**204**. NOTIFICATION_NOT_FOUND có thể che bản không thuộc chủ. DTO có id/type/title/message/relatedType/relatedId/isRead/createdAt; relatedId là integer(int64), có thể null, không ép 32 bit. Event nghiệp vụ chỉ phát kết quả sau commit; feed/nhắc lịch không bảo đảm email delivery.

Chat dùng HTTP, FE có thể polling; chọn chu kỳ theo nhu cầu UI. Member cần ROLE_MEMBER+CHAT_AS_MEMBER, staff là Receptionist/CenterManager Active+HANDLE_CHAT; không suy Admin hoặc authority đơn lẻ được đọc mọi hội thoại.

| API | Kết quả / giới hạn |
|---|---|
| POST `/api/chat/conversations` | Body subject optional, get/create một OPEN conversation của Member |
| GET `/api/chat/conversations/me/active`, `/api/chat/me/unread-count` | Hội thoại/count của Member |
| GET `/api/chat/conversations/{id}/messages?afterId=0&limit=30` | **Array** theo cursor, limit 1–50; không phải Page.content |
| POST cùng path messages | content/clientMessageId; trim, không rỗng, tối đa 2.000 ký tự,20 tin/phút/người; cùng sender/conversation/key trả tin cũ |
| POST `/api/chat/conversations/{id}/read` |204; owner/staff hợp lệ |
| GET `/api/chat/conversations` | Staff inbox dạngPage; status/assigned/unreadOnly/page/size, size tối đa 100 |
| POST `/{id}/claim`, `/{id}/release`; PATCH `/{id}/close`, `/{id}/reopen` trong group conversations | Staff; kiểm trạng thái/assignment. Reopen 409 nếu Member đã có OPEN khác |

Owner/staff scope được kiểm lại ở service cho message/read.429 có Retry-After/retryAfterSeconds;400/404/409 có CHAT_*code theo điều kiện, không retry vô hạn. Message gồm id/conversationId/senderType/senderName/isMine/content/contentType/metadata/clientMessageId/createdAt; senderType có MEMBER/STAFF/AI/SYSTEM, không có cam kết AI provider đã bật. Notification/chat datetime có thể offset+07:00.

FE render chat bằng text, không HTML. Có thể poll messages mỗi3–5giây khi mở chat, unread count mỗi30–60giây; không dùng WebSocket. Notification legacy giữ type cũ, loại mới lưu notification_type và trả qua field type. CHAT_MEMBER_MESSAGE/CHAT_STAFF_REPLY chỉ thông báo khi người nhận chuyển từ không có tin chưa đọc sang có tin chưa đọc; lỗi lưu notification không rollback message.

- Hành vi handoff theo `SCMS_AI_CHAT_HANDOFF_MODE` (mặc định `SUGGEST`; `AUTO` giữ cách chuyển tự động cũ). Ở `SUGGEST`, AI chỉ gợi ý và Member tự chọn nút “Gặp nhân viên”; yêu cầu rõ ràng của Member vẫn chuyển ngay. Các thông báo lỗi/quota dạng SYSTEM không tạo notification và không tăng unread của Member.

Các API CRUD/training/evaluation/feedback/audit còn lại tra OpenAPI. Đây không đồng nghĩa mọi role/owner/center/error đã nghiệm thu. Chi tiết thiết kế cũ của OTP/chat/notification truy qua Git tại a4f3cd0 nếu cần, không phải một tài liệu bàn giao độc lập nữa.

## 16. Ví dụ có nguồn — trích local đã ghi, không phải dữ liệu Render

Các ví dụ lấy từ một QA record tại source693f732, Clock test Asia/Ho_Chi_Minh. ID có thể khác production; fixture cố ý đổi giá3000000→3100000 sau đơn đầu để kiểm snapshot, không thay giá production.

**A. Member đã được Coach nhận, POST `/api/advanced-registrations/1/order`, body `{"paymentMethod":"Cash"}` →200:**

```json
{
  "subscriptionId": 1,
  "invoiceId": 1,
  "invoiceCode": "INV-2026-000001",
  "amount": 3000000.0,
  "paymentMethod": "Cash",
  "status": "Pending",
  "expiresAt": "2026-10-27T08:05:00",
  "paymentUrl": null
}
```

Pending chưa Active; paymentUrl null là đúng với Cash, expiresAt là hạn trả tiền5phút.

**B. Sau Receptionist confirm Cash, GET subscriptions →200, trích field:**

```json
{
  "subscriptionId": 1,
  "price": 3000000.0,
  "status": "Active",
  "remainingCredit": 12,
  "activatedAt": "2026-10-27T08:00:00",
  "expiresAt": "2026-12-12T08:00:00"
}
```

Hạn dùng này khác hạn thanh toán; fixture có1ngày TT nghỉ, nên được skip đúng thời gian. Book đọc lại11credit, hủy đúng12h đọc lại12credit; SessionResponse không có balance. Gọi lặp không được cộng/trừ lần hai.

**C. IPN muộn, Member đọc ownInvoice →200, trích field:**

```json
{
  "invoiceId": 3,
  "paymentStatus": "Paid",
  "reconciliationRequired": true
}
```

Workflow kiểm thêm DB: gói Cancelled/0 credit. FE hiển thị đã ghi nhận tiền/cần đối soát, không coi Paid là đã có quyền học.

**D. Report tháng11 của fixture, trích field:**

```json
{
  "totalRevenue": 6200000.0,
  "moneyReceived": 3100000.0,
  "moneyRefunded": 3100000.0,
  "netReceived": 0.0,
  "cashCorrectionsRecorded": 3100000.0
}
```

Gross6200000 khác tiền thực nhận; Approved chưa tính hoàn, CashCorrection không sinh refund giả. Các lượt HTTP/case gốc chỉ mở trong [QA](../artifacts/qa/latest.json) khi cần kiểm một nhận định.

## 17. Bàn giao và nghiệm thu

FE cần xác nhận phiên, lookup ID/role theo môi trường, state/field nullable, refresh nguồn tiền/quyền/lịch sau thao tác, xử lý204/401/403/404/409 và TTL/deadline theo server. Quy trình và lệnh local ở [docs02](02-DEPLOYMENT-GUIDE.md#testing).

Nghiệm thu theo từng điều kiện Dxx: actor+fixture →request →response+state →môi trường/SHA →PASS/FAIL/BLOCKED/NOT_RUN. Ví dụ D05 đúng12h phải hoàn đúng1 và gọi lặp không tăng; D18 dùng phiên Coach hợp lệ để chứng minh403. GET anonymous401 chỉ là security smoke.

Đối chiếu lịch sử nằm trong `authenticatedAcceptance.cases` của [QA duy nhất](../artifacts/qa/latest.json), response tham chiếu `local.exchanges`. Room/báo vắng02/11 dùng `roomAbsenceReview.postgresHttp.cases` và `exchanges` cùng section; không dùng matrix lịch sử để đóng yêu cầu mới. `authenticatedAcceptance.remaining` và Active giữ phần chưa nghiệm thu. [Bảng đối chiếu1:1](00-MASTER-CHECKLIST.md#report-trace) ghi rõ kết luận từng phát hiện gốc và phần capture SQL còn thiếu; test có assertion PASS không đồng nghĩa mọi giá trị đã được trích đầy đủ vào QA.

Bộ contract/schema có thể còn thiếu hoặc sai ở phần chưa chạy. [docs00](00-MASTER-CHECKLIST.md#active) giữ các cổng chưa nghiệm thu; không dùng số route/assertion hoặc bản ghi local để ký full production/FE acceptance.
FE có thể poll `GET /api/notifications/me/unread-count` mỗi 30–60 giây để cập nhật badge. Module không dùng WebSocket. Các bản ghi legacy giữ nguyên nội dung và cột `type`; các loại nghiệp vụ mới lưu trong `notification_type` và được API trả dưới tên trường `type`.

## 16. Chat hỗ trợ Member – Receptionist

Toàn bộ API dùng session đăng nhập hiện tại. Member chỉ đọc/ghi conversation của chính mình; truy cập conversation khác trả `404 CHAT_CONVERSATION_NOT_FOUND`. Receptionist và CenterManager cần quyền `HANDLE_CHAT`; Coach bị từ chối. Tin nhắn là text thuần, tối đa 2000 ký tự sau trim; FE phải render bằng text để tránh XSS.

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/chat/conversations` | Member lấy conversation `OPEN` hoặc tạo mới; body tùy chọn `{"subject":"Payment question"}`. |
| `GET` | `/api/chat/conversations/me/active` | Conversation đang mở của member; 404 `CHAT_ACTIVE_CONVERSATION_NOT_FOUND` nếu chưa có. |
| `GET` | `/api/chat/me/unread-count` | Tổng số tin chưa đọc trong các conversation của member: `{"unreadCount":2}`. |
| `GET` | `/api/chat/conversations/{id}/messages?afterId=0&limit=30` | Poll theo ID tăng dần; mặc định 30, giới hạn tối đa 50. |
| `POST` | `/api/chat/conversations/{id}/messages` | Gửi `{ "content":"Hello", "clientMessageId":"uuid" }`; ID client tùy chọn để retry an toàn. |
| `POST` | `/api/chat/conversations/{id}/escalate` | Member có `CHAT_AS_MEMBER` chuyển cuộc hội thoại của chính mình sang STAFF; không nhận memberId từ body, trả conversation hiện tại. Retry không tạo thêm SYSTEM message nếu đã chuyển. |
| `POST` | `/api/chat/conversations/{id}/close` | Member có `CHAT_AS_MEMBER` tự kết thúc hội thoại của mình; không body, danh tính lấy từ session. Trả `ChatConversationResponse`; đóng lặp idempotent. |
| `POST` | `/api/chat/conversations/{id}/read` | Đánh dấu đã đọc tới message mới nhất cho user hiện tại. |
| `GET` | `/api/chat/conversations?status=OPEN&assigned=true&unreadOnly=false&page=0&size=30` | Hộp thư staff, mới hoạt động gần nhất trước; `assigned=true` lọc hội thoại của người đang xem, `false` lọc hội thoại chưa được nhận; mỗi hàng kèm last message và unread của staff đang xem. |
| `POST` | `/api/chat/conversations/{id}/claim` / `/release` | Nhận hội thoại hoặc tự nhả hội thoại đã nhận. |
| `PATCH` | `/api/chat/conversations/{id}/close` / `/reopen` | Đóng/mở lại hội thoại. Reopen trả 409 nếu member đã có hội thoại khác đang mở. |

Message response gồm `id`, `conversationId`, `senderType`, `senderName`, `isMine`, `content`, `contentType`, `metadata`, `clientMessageId`, `createdAt`, `handoffSuggested` (boolean, mặc định false) và `reasonCode` (string hoặc null). Hai trường mới được BE trích an toàn từ metadata; metadata null/hỏng trả false/null. FE nên làm nổi nút “Gặp nhân viên” khi tin AI/SYSTEM mới nhất có `handoffSuggested=true`; gọi endpoint `/escalate` hiện có khi Member chọn. Thời gian trả ISO-8601 cùng offset `+07:00`.

Các mã lỗi chính: `CHAT_CONVERSATION_NOT_FOUND` (404), `CHAT_ACTIVE_CONVERSATION_NOT_FOUND` (404), `CHAT_CONVERSATION_CLOSED` (409), `CHAT_RATE_LIMITED` (429, tối đa 20 tin/phút/người; response có `retryAfterSeconds` và header `Retry-After`), `CHAT_MESSAGE_EMPTY`, `CHAT_MESSAGE_TOO_LONG`, `CHAT_ALREADY_CLAIMED`, `CHAT_NOT_ASSIGNED_TO_ACTOR`, `CHAT_OPEN_CONVERSATION_EXISTS`. FE nên poll message mỗi 3–5 giây khi cửa sổ chat mở, unread count mỗi 30–60 giây, và gửi `clientMessageId` cho mỗi tin.

Member gọi `POST /api/chat/conversations/{id}/close` khi bấm kết thúc. Conversation không tồn tại hoặc thuộc Member khác cùng trả `404 CHAT_CONVERSATION_NOT_FOUND`. Với `OPEN`, BE khóa hội thoại, đặt `status=CLOSED`, `closedAt` theo `Clock`/`Asia/Ho_Chi_Minh`, giữ nguyên `handlerType` và `assignedStaffUserId`, rồi lưu đúng một tin SYSTEM `Bạn đã kết thúc cuộc trò chuyện.` với metadata `{"closedBy":"MEMBER"}`. Trả cùng `ChatConversationResponse` như các route hội thoại. Nếu đã `CLOSED`, trả `200` trạng thái hiện tại, giữ `closedAt` và không thêm SYSTEM message. Endpoint không nhận body/memberId/actor. Member vẫn đọc được lịch sử; gửi tin vào hội thoại đóng tiếp tục trả `409 CHAT_CONVERSATION_CLOSED`.

Tin SYSTEM do Member tự đóng không phát notification cho Member hoặc staff và không tăng unreadCount của Member. Tin này không đánh dấu các tin trước là đã đọc. Worker AI bỏ qua hội thoại đóng; reply AI đến muộn không được lưu, auto-escalate/quota/handoff không mở lại hội thoại hoặc thêm SYSTEM message. Sau khi đóng, Member có thể gọi `POST /api/chat/conversations` để lấy/tạo hội thoại OPEN mới theo `SCMS_AI_CHAT_ENABLED` và `SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER` hiện hành; quy tắc mỗi Member tối đa một hội thoại OPEN giữ nguyên.

### Mở rộng chat AI

`ChatService` chỉ xử lý nghiệp vụ; `ChatResponder` dùng một interface `AiCompletionClient` và implementation mock hoặc provider đã cấu hình. `ChatResponderDispatcher` chạy nền và gọi responder đầu tiên hỗ trợ conversation khi `handler_type=AI`. `SUGGEST` lưu lời mời phù hợp khi hết quota, responder không khả dụng/rỗng/lỗi hoặc trả token `[HANDOFF]`; token không hiển thị, không đổi handler, và metadata trả `handoffSuggested=true` cùng reasonCode (`SENSITIVE_TOPIC`, `REPEATED_KB_MISS`, `AI_HANDOFF_SUGGESTED`, `AI_DAILY_LIMIT_REACHED`, `AI_EMPTY_REPLY`, `AI_UNAVAILABLE`, `AI_RUNTIME_FAILURE`). Gợi ý SYSTEM không phát notification và không tính unread cho Member; lặp liên tiếp cùng thông báo không thêm SYSTEM message. `AUTO` giữ hành vi handoff tự động trước đây. Hỏi rõ gặp nhân viên được chuyển ngay ở cả hai chế độ. Config `SCMS_AI_CHAT_ENABLED` mặc định false, hội thoại mới mặc định STAFF, handoff mode mặc định `SUGGEST`; giá trị mode không hợp lệ dùng `SUGGEST`. `POST /conversations/{id}/escalate` cho Member chuyển hội thoại của mình sang STAFF; BE suy ra Member từ Authentication, không dùng danh tính từ body. Các reply AI thường vẫn dùng cơ chế unread/notification hiện hành.

**DeepSeek DB tools (opt-in, hiện mặc định tắt):** `SCMS_AI_CHAT_DB_TOOLS_ENABLED=false`; chỉ DeepSeek hỗ trợ nhánh này. `SCMS_AI_CHAT_TOOL_MAX_ROUNDS=3`, `SCMS_AI_CHAT_TOOL_MAX_ROWS=10` (BE chặn ở 25); kết quả mỗi tool tối đa 2.000 ký tự. Sáu tool cố định: `get_membership_packages`, `get_subjects`, `get_open_classes(subjectName?)`, `get_my_subscriptions`, `get_my_upcoming_sessions(fromDate?, toDate?)`, `get_my_recent_invoices`. Ba tool member lấy scope chỉ từ `ChatAiResponder.respond(..., memberUserId)`; không nhận memberId từ model. Lịch nhận ngày `yyyy-MM-dd`, mặc định từ hôm nay; `toDate` inclusive, không trước `fromDate`, toàn khoảng tối đa 31 ngày. JSON lịch gồm `sessions`, `truncated` và `total`; model phải nói rõ nếu truncated. Hóa đơn lấy tối đa 5 mới nhất theo `COALESCE(paymentDate, createdAt)` giảm dần (ngày FE hiển thị paymentDate, fallback createdAt). Tool failure khi cờ bật không dùng FAQ keyword: fallback `AI_UNAVAILABLE` hoặc `AI_RUNTIME_FAILURE` tạo handoff suggestion; cờ tắt giữ FAQ fallback. `SCMS_AI_CHAT_MARKDOWN_ENABLED=false` mặc định buộc câu trả lời dạng gạch đầu dòng, không dùng bảng; bật true cho phép Markdown. Prompt xử lý ngày/tuần theo giờ Asia/Ho_Chi_Minh và yêu cầu gọi tool cho gói/lịch/hóa đơn cá nhân. Không thêm endpoint hay schema DB; giữ DTO whitelist, read-only transaction, data isolation và sáu tool.

Metadata của reply DeepSeek có tool calls thêm `tools` (danh sách tên đã gọi) và `toolRounds` (số vòng tool); khi không gọi tool, các key này không có. Không đổi FAQ/mock/Anthropic. Dữ liệu từ tool được xem là untrusted content; prompt hướng dẫn bỏ qua chỉ thị nằm trong nội dung DB. FE không cần đổi.

Chat notification types persisted in `notification_type`: `CHAT_MEMBER_MESSAGE` (member message or escalation to staff) and `CHAT_STAFF_REPLY` (staff/AI reply to member). Notifications are emitted only on that viewer's unread transition from zero to one; storage failures do not roll back the chat message.

## 17. Kết quả và đánh giá quá trình tập luyện

| Method | Endpoint | Quyền và phạm vi |
|---|---|---|
| `GET` | `/api/training-results?sessionId={id}&memberId={id}&coachId={id}` | MANAGE_CLASSES, MANAGE_USERS, COACH hoặc MEMBER. Member chỉ đọc kết quả của chính mình; BE lấy User từ session Authentication, tự áp `memberId` hiện tại nếu bỏ trống và trả 403 nếu `memberId` khác. Coach giữ scope hiện có; quyền quản lý giữ hành vi hiện có. |
| `GET` | `/api/training-plans?coachId={id}&classId={id}&memberId={id}` và `/api/training-plans/{id}` | Member được đọc kế hoạch của chính mình; service kiểm tra ownership. |
| `GET` | `/api/evaluations?memberId={id}&coachId={id}` và `/api/evaluations/{id}` | Member được đọc đánh giá của chính mình; service kiểm tra ownership. |

Member được xem toàn bộ `content` của training result và `comment` cùng `progressScore` của evaluation do Coach ghi nhận. Không dùng memberId, role hoặc actor từ client để xác định danh tính. Các GET danh sách trả `200` với danh sách rỗng khi không có kết quả phù hợp; truy cập dữ liệu của Member khác trả `403`.
