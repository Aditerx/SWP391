# Cập nhật phân quyền (RBAC)

Tài liệu này ghi lại các thay đổi phân quyền trong lần cập nhật hiện tại.

## Nguồn cấp quyền và thời điểm có hiệu lực

- Các quyền nghiệp vụ như `MANAGE_USERS`, `MANAGE_INVOICES` và `MANAGE_PERMISSIONS` được nạp từ quan hệ `role_permissions` trong cơ sở dữ liệu. Không còn cấp các quyền này theo tên role trong `SecurityConfig`.
- `ROLE_*` vẫn được giữ làm role marker để giới hạn dữ liệu tự truy cập, chẳng hạn Member chỉ xem dữ liệu của mình.
- Spring Security giữ `Authentication` trong session. Vì vậy, thay đổi role-permission trong cơ sở dữ liệu có hiệu lực sau khi người dùng đăng xuất rồi đăng nhập lại để nạp authorities mới.

## Thay đổi chính

| Khu vực | Thay đổi |
| --- | --- |
| Quản lý permission matrix | `PUT /api/roles/{id}/permissions` yêu cầu `MANAGE_PERMISSIONS`; `MANAGE_USERS` không còn đủ quyền thay đổi RBAC. |
| Tài khoản Admin | Tạo, sửa, vô hiệu hóa hoặc đổi role của tài khoản Admin cần `MANAGE_PERMISSIONS`, tránh nâng quyền thông qua quyền quản lý user thông thường. |
| Member và Staff | Các endpoint quản lý Member/nhân viên được bảo vệ bằng `MANAGE_USERS`; một số thao tác đăng ký cho phép `REGISTER_MEMBER` theo chính sách của endpoint. |
| Gói đăng ký | `MemberPackage` là đăng ký/liên kết Member–Package trong bảng `member_packages`, không phải danh mục Package. Đọc và thay đổi đăng ký được bảo vệ bằng `MANAGE_SUBSCRIPTIONS` hoặc quyền đọc phù hợp. Member chỉ đọc đăng ký của chính mình. |
| Hóa đơn | Tạo, thanh toán và cập nhật hóa đơn yêu cầu `MANAGE_INVOICES`. Nhân viên có quyền này được xem hóa đơn theo bộ lọc; Member chỉ xem hóa đơn của mình. Member truyền `memberId` của người khác sẽ bị từ chối (403). |
| Enrollment | Các thao tác tạo/hủy được giới hạn theo `REGISTER_MEMBER`/`MANAGE_CLASSES`; Member chỉ thao tác trên enrollment của chính mình. Truy vấn dữ liệu Member cũng được giới hạn theo người đang đăng nhập. |
| Lịch, điểm danh và dữ liệu huấn luyện | Bổ sung hoặc siết quyền đọc cho session, attendance, training plan, evaluation và training result. Member/Coach được giới hạn dữ liệu theo tài khoản của họ; các thao tác ghi vẫn cần quyền nghiệp vụ tương ứng như `MANAGE_CLASSES`, `RECORD_RESULT` hoặc `MANAGE_TRAINING_PLAN`. |
| Audit RBAC | Thay đổi permission matrix ghi lại user thực hiện vào `system_logs.user_id`; actor được lấy từ tài khoản đang xác thực thay vì `null`. |

## Cập nhật cơ sở dữ liệu

Với cơ sở dữ liệu đã được khởi tạo trước đó, chạy [migration_dynamic_permissions.sql](migration_dynamic_permissions.sql) trước khi dùng các endpoint mới. Migration thêm `MANAGE_PERMISSIONS` và `MANAGE_INVOICES`, rồi gán quyền mặc định theo tên role và permission. `schema_postgres.sql` cũng chứa các thay đổi seed tương ứng cho môi trường khởi tạo mới.

Quyền mặc định liên quan đến cập nhật này:

- `ADMIN`: `MANAGE_PERMISSIONS`.
- `CENTER_MANAGER` và `RECEPTIONIST`: `MANAGE_INVOICES`.
- Các quyền khác tiếp tục lấy từ các dòng được seed trong `role_permissions`; có thể thay đổi qua quản lý RBAC.

## Kiểm tra

Đã bổ sung/cập nhật unit test cho phạm vi hóa đơn của Member, actor trong audit log, giới hạn enrollment của Member và thay đổi chữ ký/phạm vi truy vấn điểm danh.

Chưa xác nhận toàn bộ test suite chạy thành công trong môi trường cập nhật này: lần chạy Maven bị dừng ở bước biên dịch vì môi trường chỉ có JRE, không có JDK (`No compiler is provided`). Cần chạy lại `mvnw test` bằng JDK để xác nhận đầy đủ.
