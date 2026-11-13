# Bàn giao FE — RBAC và quản trị cuối cùng

Đối tượng: FE quản trị tài khoản/phân quyền và Owner. Ngày13/11/2026. Bằng chứng và phạm vi nghiệm thu: [Active](../docs/00-MASTER-CHECKLIST.md#active), QA `session31LastAdministrator20261113`; demo **SKIPPED_BY_OWNER**, FE/browser/Render chưa nghiệm thu.

## 1. Kết quả hiện tại

PUT permissions yêu cầu Admin Active đồng thời có `MANAGE_PERMISSIONS`. Mọi thay permissions/role/status/PUT tài khoản liên quan phải giữ ít nhất một Admin Active có cả `MANAGE_USERS` và `MANAGE_PERMISSIONS`; vi phạm409 `LAST_ADMIN_REQUIRED`, rollback dữ liệu và không thêm audit thay đổi. Hai Admin cùng role vẫn có thể cùng mất quyền nếu sửa role đó, nên không chỉ đếm số account Admin.

## 2. Quá trình và quyết định giữ/bỏ

- Đối chiếu D01 và source xác nhận gate PUT cũ chỉ kiểm authority, còn GET đã Admin-only.
- Giữ GET, thay nguyên tập permissions/validation ID, audit và refresh/thu hồi phiên; thêm kiểm role/authority hiện hành bên trong transaction sau khi lấy khóa.
- Dùng cùng tập role row theo ID tăng dần trước target user cho mọi mutation có thể làm mất quản trị; bỏ cách chỉ khóa role đích để suy chống race.
- Giữ `[]` cho role không phá invariant và giữ audit cho từng PUT hợp lệ, không thêm migration/seed hoặc tài khoản hạ tầng.

## 3. API và DB liên quan

| Method/path | Input/response và FE xử lý lỗi |
|---|---|
| PUT `/api/roles/{id}/permissions` | `{permissionIds:[...]}` thay toàn bộ, ID dương/tồn tại, ID lặp được gộp;200 `RoleResponse`. Sai IDs400, thiếu quyền403, thiếu role404, mất quản trị409 `LAST_ADMIN_REQUIRED`. `[]` không bị cấm chung. |
| PUT `/api/users/{id}` | Giữ request hiện có; đổi role/status không vượt guard. Lỗi409 rollback cả tên và các field cùng request. |
| PATCH `/api/users/{id}/role`, PATCH `/api/users/{id}/status` | Giữ body `roleId`/`status`, gate endpoint/RP-04/center; cùng lỗi409 khi thao tác làm mất Admin đủ quyền cuối cùng. |
| GET `/api/roles`, `/api/roles/{id}`, `/api/permissions` | Giữ Admin-only; không tự yêu cầu thêm MANAGE_PERMISSIONS để đọc. |

DB dùng `users`, `roles`, `permissions`, `role_permissions`, audit hiện có trong `system_logs`; không đổi schema hay lịch sử. `/staff/{id}` và `/members/{id}` không nhận target Admin. Contract chi tiết tại [docs01](../docs/01-API-CONTRACT-FE-BE.md#last-administrator-20261113).

## 4. FE cần làm

1. Chỉ mở thao tác PUT phân quyền khi `/api/auth/me` xác nhận Admin và có `MANAGE_PERMISSIONS`; quyền thực tế vẫn do BE kiểm.
2. Sau200, tải lại role/permissions và auth-me; quyền sửa role áp dụng mọi account dùng role đó, kể cả phiên đang mở của chính người sửa.
3. Với409 `LAST_ADMIN_REQUIRED`, hiển thị lý do và đọc lại trạng thái; cần có Admin Active khác giữ cả hai authority trước khi thử lại thao tác loại bỏ quyền. Không tự nâng quyền hoặc retry vô hạn.
4. Cùng role đổi permissions: dùng authorities mới ở request tiếp. `SESSION_ROLE_CHANGED`403 yêu cầu đăng nhập lại; `ACCOUNT_NOT_ACTIVE`403 hiển thị trạng thái; phiên đã hủy tiếp đó401. Không logout mọi403 nghiệp vụ.

## 5. Việc còn lại

Theo dõi kết quả local/commit/race tại Active/QA, không suy từ đó thành nghiệm thu FE/cloud. Owner đã skip demo3.1; không cần chuẩn bị lại fixture/bundle hoặc mở Swagger để chờ. Tích hợp FE/build/API origin, browser và deploy tách phạm vi sau; Owner tự đóng gói ZIP.
