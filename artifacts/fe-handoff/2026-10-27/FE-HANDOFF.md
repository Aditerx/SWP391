# Bàn giao FE SCMS — contract, bằng chứng và giới hạn

Đây là điểm bắt đầu cho FE. Rule gốc: docs04 mục0.2; contract chính: docs01. Phạm vi P1.0–P1.4/BE P2; P1.5/T/D01/D09/FE integration được Owner để lại. Kết quả route/schema/GET không được coi là nghiệm thu mọi nghiệp vụ Render.

**Live đã kiểm:** `4d372e5832bd3e8685008b2e0a2029ff981eb047` lúc 22:19:35 UTC+7, sau push 342.131s. 185/185route khớp, 138schemas;74/74GET anonymous.24status gap đã hết,6schema riêng đúng,LocalTime string.149localexchanges trên36operations;114positive so với SwaggerLive:201bodychecks/0type-property-status mismatch. CIbranch/main pass. DBUP/mailDOWN;0authenticated business operations hoặc mutation tests trênRender. Đây không phải nghiệm thu toàn bộ nghiệp vụ/FE production.

## 1. Các file cần nhận

| File | Dùng làm gì |
|---|---|
| [api-inventory.md](api-inventory.md) / JSON | Tất cả method/path/params/body/status/auth annotation, handler source, Dxx, exchange index và GET probe |
| [schemas.md](schemas.md), [OpenAPI](render-openapi.json) | Kiểu/field/request required/enums công bố; có thể import OpenAPI vào Postman/Insomnia để đọc schema |
| [decision-matrix.md](decision-matrix.md) | D01–D19 → policy → API → bằng chứng → phần chưa nghiệm thu |
| [examples.md](examples.md), [http-exchanges.json](http-exchanges.json) | Request, actor, businessNow, status/body thật cùng cách diễn giải |
| [demo](demo/demo.html), [manifest](demo/verification.json), [test summary](demo/test-summary.txt) | Xem và kiểm lại9workflow local/DB assertions cùng regression contract theo đúng source SHA |
| [audit-summary.json](audit-summary.json), [render-get-checks.json](render-get-checks.json) | Ranh giới kiểm chứng trên Render; không chứa phiên người dùng/PII protected response |

## 2. Kết nối và đăng nhập

Base Render: https://scms-app-p1gy.onrender.com . Base local: http://localhost:8080 hoặc port runner ghi trong trace/log. POST /api/auth/login bằng tài khoản được cấp, giữ JSESSIONID trong cookie jar. Fetch dùng credentials:'include', Axios dùng withCredentials:true; không gắn bearer token thay session. GET /api/auth/me lấy danh tính/role/permission hiện tại; memberId từ phiên/response đăng nhập, không cho người dùng tự chọn Member khác.

Cross-site FE HTTPS cần exact origin CORS, allowCredentials và cookie SameSite=None/Secure=true. Source default lax/false nên chưa suy cấu hình dashboard đã đúng; cần browser kiểm Set-Cookie, cookie gửi lại, preflight và actual response. Swagger/Postman không chứng minh browser cookie/CORS.

## 3. Vai trò và màn hình

| Vai trò | Hành động chính | Điều không được suy |
|---|---|---|
| Member | Tạo registration, đọc của mình, order, ownInvoice/subscriptions/lịch, hủy đúng hạn, notice, QR | Không tự tạo Advanced session; không coi biết invoiceId/memberId là được đọc của người khác |
| Primary Coach | Nhận/từ chối registration được giao, book/dời hợp lệ sau trao đổi, attendance trong giờ buổi | Không đọc tổng tài chính; không thay được quyền Manager chỉ vì có MANAGE_CLASSES |
| Receptionist + MANAGE_INVOICES | Xác nhận Cash, đọc chứng từ/tiếp nhận case theo quyền | Manager có MANAGE_INVOICES vẫn không tự xác nhận Cash nếu thiếu ROLE_RECEPTIONIST |
| CenterManager/Admin + authority | Work-schedule, hỗ trợ có lý do/consent, incident/Holiday, approve/complete/report trong center | Role name chưa đủ; service còn kiểm active/owner/center/permission/trạng thái |

Chi tiết annotation từng route có trong inventory; service guards không thể suy đầy đủ chỉ từ OpenAPI. Generic staff read endpoints chưa có full cross-center HTTP matrix trong audit này. Không ký nghiệm thu toàn bộ phân quyền từ74GET anonymous.

## 4. Chuỗi màn hình Advanced

1. Login→GET subjects(cógiá), Coach directory(D09 còn backlog), work-schedule/admission-capacity. ID lookup theo môi trường; chọn Coach chưa giữ tài nguyên.
2. Member POST registration(subjectId,primaryCoachId,requestKey)→Requested; Coach decision accepted→Accepted. Chưa accept thì order bị chặn.
3. Member POST order(paymentMethod Cash/VNPay)→Invoice/Subscription Pending, amount snapshot, expiresAt thanh toán5phút. Giữ requestKey khi retry cùng thao tác; đổi dữ liệu phải tạo thao tác/key mới.
4. Cash chờ Receptionist confirm; online chỉ điều hướng paymentUrl từ BE. Return hiển thị kết quả/redirect, FE đọc ownInvoice, poll hữu hạn rồi nút Kiểm tra lại. FE không gọi IPN và không tự đặt Paid/Active.
5. Paid bình thường→đọc subscriptions Active/12credit/deadline. Paid+reconciliationRequired→hiển thị cần đối soát, không giả quyền. Legacy null subject/Coach/timestamps→cần hỗ trợ đối soát, không gán12credit/45ngày từ UI.
6. Coach/Manager book có consent/requestKey; response Session201, balance đọc lại subscriptions. Hủy>=12h hoàn1; dưới12h dùng notice riêng. Khi dời, so cũ-mới; không xóa notice ở UI trước khi BE xác nhận đổi thời gian thành công.
7. Sự cố/Holiday có hồ sơ/preview/token/audit. Refresh nguồn gói/lịch sau workflow; không vá deadline/credit phía FE.

## 5. State và dữ liệu dễ nhầm

| Dữ liệu | Nguồn/ý nghĩa |
|---|---|
| registration.status | Requested/Accepted/Rejected/Invoiced; Invoiced không đồng nghĩa Paid |
| order.status / Invoice.paymentStatus | Trạng thái tiền; Paid có thể cần đối soát |
| subscription.status | Quyền Pending/Active/Cancelled/Expired/Completed, riêng từng đăng ký |
| order.expiresAt | Hạn thanh toán5phút |
| subscription.expiresAt | Hạn quyền45ngày sử dụng đã tính skip TT nghỉ/đóng, dùng timestamp chính xác |
| Invoice.amount / subscription.price | Giá snapshot đã mua; không tính lại từ catalog hiện tại |
| Session.remainingCredit | Không có field này; tải lại subscriptions |
| remainingCredit=0 | Không có nghĩa mất mọi quyền nếu còn ca hợp lệ đã book |
| totalRevenue | Gross Paid lịch sử; không thay moneyReceived/moneyRefunded/netReceived |
| Refund Approved | Chưa hoàn tiền; Completed/evidence/performedAt mới ghi hoàn |
| CashCorrection | Xác minh chưa nhận tiền, không phải hoàn tiền; chứng từ Paid gốc được giữ |

Ngày giờ business LocalDateTime không offset được đọc như UTC+7; chuỗi đã cóZ/+07:00 giữ nguyên. Không dùng Date máy người dùng để tự quyết Paid/expiry. Record list có thể[]; notification/chat dùng phân trang, đọc đúng schema content/Page chứ không dùng một parser mảng cho tất cả.

## 6. Normal, attendance và QR

Ưu tiên POST /api/classes/normal-with-sessions tạo nguyên khối12buổi. Nếu tách bước: POST Class→Closed, generate đủ12 rồi PUT Open. Sĩ số Normal≤15, Registered+Pending giữ chỗ; đầy có thể Waitlisted, không mặc định lỗi capacity. Enrollment Pending chưa được học; Paid mới Registered. Normal không hủy/notice vì lý do cá nhân.

Chỉ buổi thật sự không tổ chức do TT có hồ sơ verifiedNotHeld mới được bù; giữ buổi gốc centerInvalidated và link đúng1makeup.13record vật lý có thể chỉ12buổi hợp lệ. Attendance mới Present/Absent trong[start,end), correction sau end bởi Manager có audit. Lobby check-in/out default tắt; không dùng thay attendance buổi.

QR response chỉ payload/expiresInSeconds, TTL còn lại1–30s. Refetch khi foreground/hết hạn; không log payload/secret. Evidence redact payload, nên mã trong tài liệu không thể dùng scan. Coach chỉ scan buổi được giao, replay bị chặn; tại end không còn hợp lệ.

## 7. Xử lý lỗi và HTTP

Phân biệt201(body tạo mới),200(body/void theo route),204(no body: không gọi response.json()). BusinessException có code/message, nhưng legacy/validation/401/403 có thể timestamp/status/error/message hoặc body framework khác. FE ưu tiên code→message→HTTP fallback, không hardcode mọi lỗi đều có code.409 tải lại trạng thái/preview, không lặp vô hạn;401 đăng nhập lại;403 hiển thị thiếu quyền;404 owner-hidden/không tìm thấy dùng thông báo chung.

X-Preview-Token lấy từ preview; stale/blocked không có nghĩa request đã commit. IDEMPOTENCY_CONFLICT giữ lại kết quả cũ và báo người dùng khi key trùng nhưng nội dung khác, không tạo lại nhiều hóa đơn/buổi một cách mù quáng.

## 8. Tái lập và nhận diện bằng chứng

```powershell
docker start scms-phases-20261027
.\scripts\run-phases-demo.ps1
```

Runner tạo detached checkout từ HEAD không.env, DB mới loopback127.0.0.1:55440; clean test, migrations/replay/snapshot/FK guard rồi9HTTPworkflow +1regression OpenAPI với login thật và assertDB sau commit. Có Clock test để đi qua biên12h/5phút/expiry, không giả status Paid/Active bằng SQL để thay luồng thanh toán. Một số thay giá/clock/fixture cố ý là setup kiểm biên, được giải thích trong examples. Lệnh không tự kết nối Render, gửi email hoặc dùng tiền thật. Không chạy Maven clean đồng thời trên cùng target. Worktree/DB/log giữ local để kiểm, không reset DB có sẵn.

Demo HTML chỉ xem evidence, không phải FE tích hợp hoặc một phiên server đang mở để Postman replay tự do. Muốn gọi bằng FE thật cần khởi động BE local với fixture/múi giờ/role tương ứng; không copy request ngày/ID giả từ examples sang production.

## 9. Các cổng còn phải nghiệm thu

Authenticated business/role/owner/center/full negative matrix trên Render chưa chạy, vì audit không có phiên tài khoản test và không được suy quyền mutation production từ phê duyệt nâng schema. VNPay callback được ký local bằng test key; chưa gateway sandbox live. FE browser/cookie/CORS và camera QR chưa chạy. SQL Server runtime chưa chạy. D01/D09 vẫn backlog. Render Free chặn SMTP587: db UP/mail DOWN, cần chọn email HTTPS provider/API key/sender và làm adapter để OTP cloud hoạt động.

Tách kết quả schema/route, anonymous gate, workflow local và production business acceptance. Gói này bàn giao đủ danh mục/schema và ví dụ có nguồn; không đánh dấu những cổng còn thiếu thành PASS.

Kết quả hiện tại: 185operations / 149paths / 138schemas, source route match 185; GET anonymous 74/74; local exchanges 149 trên 36operations. Clean 305run/278pass/27skip; PG10/10 (9workflow nghiệp vụ +1regression contract). Live identity/SHA và CI xem audit-summary/docs02 mục10.11.

## 10. Đóng kiểm chứng và các lỗi metadata đã sửa

**Live đã kiểm:** `4d372e5832bd3e8685008b2e0a2029ff981eb047` lúc 22:19:35 UTC+7, sau push 342.131s. 185/185route khớp, 138schemas;74/74GET anonymous.24status gap đã hết,6schema riêng đúng,LocalTime string.149localexchanges trên36operations;114positive so với SwaggerLive:201bodychecks/0type-property-status mismatch. CIbranch/main pass. DBUP/mailDOWN;0authenticated business operations hoặc mutation tests trênRender. Đây không phải nghiệm thu toàn bộ nghiệp vụ/FE production.

[handoff-verification.json](handoff-verification.json) tổng hợp nguồn/bằng chứng. [Prefix lỗi](error-prefixes.md) có153literal/prefix từ source cùng status đã quan sát local; không phải153nhánh lỗi đều đã được gọi. [wire-schema-checks.json](wire-schema-checks.json) ghi cả các field nullable thực tế và giới hạn: không phải JSON Schema validator đầy đủ cho nullability/format/bounds/required/error. [openapi-contract-check.json](openapi-contract-check.json) là proof qua HTTP ở local; [release-check.json](release-check.json) ghi CI/Live.

Swagger cũ bị trùng Request/Open/Response và giờ kiểu object có thể khiến client FE sai; snapshot before-fix/metadata defect được giữ trong thư mục này. FE sinh lại model theo snapshot mới, dùng tên request/response riêng và LocalTime string. Field JSON/state/giá/thời hạn không bị đổi bởi đợt metadata này.

Phạm vi185operations là API công bố trong OpenAPI; Swagger assets, /actuator/health và endpoint lỗi framework không là các business operations trong số này. Core report kiểm Swagger/health riêng. Chi tiết runtime/schema không thay toàn bộ service policy hoặc thử mọi baseline endpoint.
