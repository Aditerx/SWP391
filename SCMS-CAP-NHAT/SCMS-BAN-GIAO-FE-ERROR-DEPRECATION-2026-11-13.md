# Bàn giao FE — lỗi API và deprecation

Đối tượng: FE xử lý lỗi và các màn quản trị/đăng ký. Ngày13/11/2026. Kết quả chuẩn tại [Active](../docs/00-MASTER-CHECKLIST.md#active), QA `session32ErrorDeprecation20261113`; demo **SKIPPED_BY_OWNER**, FE/browser/Render chưa nghiệm thu.

## 1. Kết quả hiện tại

Lỗi nghiệp vụ HTTP409 dạng bốn field nay có `status:409,error:"Conflict"`; giữ message và code hiện có. POST/PUT phòng trùng unique trả409 cùng message generic, rollback phòng/lịch/audit; không trả chi tiết SQL. OpenAPI bổ sung các lỗi/quyền đích và chỉ đánh deprecated những mutation shared-membership đã retire. Runtime retired vẫn400 `LEGACY_MEMBERSHIP_RETIRED`; giữ các route Advanced còn dùng.

## 2. Quá trình và quyết định giữ/bỏ

- Đã trình [catalog đầy đủ trước sửa](../docs/03-TODO.md#session32-error-catalog-20261113), tách bug ánh xạ với khác biệt có contract và phần ngoài phạm vi.
- Bỏ việc tạo body400 cho plain conflict409; giữ hai-field code/message và các mã chỉ nằm trong message.
- Bổ sung bắt Hibernate DB constraint khi auto-flush qua audit; giữ unique index/transaction và rollback, không che mọi500 thành409.
- Sửa metadata đúng operation retired và lỗi role/state; giữ gate, DTO, protocol VNPay, filter phiên, chat/notification/correction và nhánh Advanced hiện có.

## 3. API và DB liên quan

| Nhóm API | Kết quả và ý nghĩa |
|---|---|
| POST classes/{id}/normal-proposals, cancel-before-opening và plain conflict nghiệp vụ khác |409 `{timestamp,status:409,error:"Conflict",message}`. `NORMAL_VERSION_STALE` giữ trong message, không tự thêm field code. Reload lớp/phiên bản trước thử lại. |
| POST `/api/rooms`, PUT `/api/rooms/{id}` | Duplicate/DB constraint409 cùng dạng bốn field, message `Operation violates a database constraint`; không lưu một phần room/audit. Các giới hạn name50/location100 và role/center giữ nguyên. |
| RBAC, GET/POST/bulk training-results, Normal proposals và POST normal-refunds/{id}/approve | Annotation thêm các response lỗi hiện có; giữ request/success DTO và gate runtime. Approve chỉ Manager Active+MANAGE_INVOICES cùng center, receiver đã xác minh/đúng version; chưa là hoàn tiền ngân hàng. |
| POST packages; PUT packages/{id}; PATCH packages/{id}/status; POST members/{memberId}/packages hoặc subscriptions; POST members/me/subscriptions hoặc member-packages/subscribe-me | `deprecated:true` trong OpenAPI; input hợp lệ/đủ quyền vẫn400 LEGACY_MEMBERSHIP_RETIRED, không tạo đơn/quyền hoặc sửa dữ liệu. Không chuyển410, không hứa header Deprecation/Sunset. |
| GET catalog/subscriptions; POST advanced-registrations; PATCH subscription status và POST extend qua hai alias | Giữ không deprecated. Advanced generic extend/change không thay workflow thanh toán/sự cố; giữ lỗi USE_INCIDENT_RECOVERY/ADVANCED_STATUS_WORKFLOW_REQUIRED, missing404 và status no-op hiện có. |

Không migration/seed/data rewrite hoặc nới index. Contract chi tiết và các envelope giữ lại ở [docs01](../docs/01-API-CONTRACT-FE-BE.md#error-deprecation-20261113); catalog/source/ca kiểm ở docs03, kết quả runtime ở Active/QA.

## 4. FE cần làm

1. Dùng HTTP status làm chuẩn; ưu tiên field code, rồi message, rồi thông báo theo HTTP nếu body framework/rỗng. Không ép mọi body về một schema có status/code bắt buộc.
2. Với409, giữ draft hợp lý, báo conflict và GET lại resource/version/preview; ca ghi lỗi không được hiển thị đã lưu. Trùng phòng không tự thêm hậu tố tên hoặc bỏ constraint để thử lại.
3. Bỏ nút/API mua shared đã retire và không chuyển sang alias khác khi400; Normal dùng enrollment, Advanced dùng registration hiện hành. Không ẩn cả status/extend/GET Advanced vì nhãn legacy của route dùng chung.
4. Phân biệt403 quyền nghiệp vụ với ACCOUNT_NOT_ACTIVE/SESSION_ROLE_CHANGED/PASSWORD_CHANGE_REQUIRED. Giữ Retry-After của chat; IPN200/Return302 không được coi là Paid.

## 5. Việc còn lại

Đối chiếu FE/build/API origin, các màn báo lỗi và workflow Advanced trong lượt tích hợp được mở riêng. Không suy kiểm BE local thành FE/Render/cloud PASS. Demo3.2 Owner đã skip; không chuẩn bị server/demo hay tái sinh bundle. Chuẩn hóa envelope toàn hệ thống hoặc đổi status/header policy là phạm vi khác, chưa tự triển khai. Matrix/readiness và checklist tổng chốt tại3.3.
