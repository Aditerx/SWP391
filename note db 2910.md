| Bảng | Chức năng |
|---|---|
| `roles` | Danh mục vai trò hệ thống. |
| `permissions` | Danh mục quyền thao tác. |
| `role_permissions` | Gán quyền cho vai trò. |
| `centers` | Trung tâm và hàng khóa chống tranh chấp lịch. |
| `users` | Đăng nhập, hồ sơ chung, trạng thái và center. |
| `email_otps` | OTP đăng ký/đặt lại mật khẩu và hạn dùng. |
| `center_managers` | Subtype Manager; hiện chủ yếu phục vụ SQL/fixture. |
| `receptionists` | Hồ sơ subtype Lễ tân. |
| `coaches` | Hồ sơ, mô tả và chứng chỉ Coach. |
| `members` | Hồ sơ Member, sức khỏe, QR và booking block. |
| `subjects` | 9 môn, giá Normal/Advanced và Outdoor. |
| `specializations` | Chuyên môn liên kết với môn học. |
| `coach_specializations` | Gán chuyên môn cho Coach. |
| `rooms` | Tài nguyên sân/phòng để kiểm trùng lịch. |
| `classes` | Chứa cả khóa Normal (`class_type='Normal'`) và template Advanced (`class_type='Advanced'`). Lịch từng buổi nằm trong `sessions`, liên kết bằng `class_id`; xem giải thích bên dưới. |
| `training_plans` | Kế hoạch đào tạo cho lớp hoặc Member. |
| `sessions` | Buổi học, Coach, sân, Member và quyền học. |
| `training_results` | Kết quả chuyên môn từng buổi/Member. |
| `attendances` | Attendance, người ghi và lý do. |
| `evaluations` | Đánh giá tiến bộ Member. |
| `class_feedbacks` | Member đánh giá lớp và Coach. |
| `membership_packages` | Danh mục gói chung/legacy; không seed gói tháng/VIP cũ. |
| `member_packages` | Quyền Advanced, giá mua, Primary Coach, credit và hạn. |
| `class_enrollments` | Ghi danh/waitlist Normal và lịch sử trạng thái. |
| `holidays` | Ngày nghỉ và thời gian đóng cửa thực tế. |
| `invoices` | Chứng từ, holding, thanh toán và đối soát. |
| `support_requests` | **Dự phòng:** phiếu hỗ trợ có người phụ trách. |
| `notifications` | Nội dung thông báo và chống gửi trùng. |
| `notification_recipients` | Người nhận và trạng thái đọc thông báo. |
| `chat_conversations` | Hội thoại hỗ trợ. |
| `chat_messages` | Tin nhắn và chống gửi lặp. |
| `chat_read_state` | Con trỏ tin nhắn đã đọc. |
| `ai_consultations` | **Dự phòng:** lịch sử tư vấn AI độc lập. |
| `system_logs` | Audit người thao tác, hành động và đối tượng. |
| `normal_class_requests` | Chống retry sinh trùng lớp/lịch Normal. |
| `subscription_pauses` | Khoảng gián đoạn dùng để tính hạn Advanced. |
| `coach_work_schedules` | Trạng thái Coach nhận đăng ký mới. |
| `coach_work_slots` | Ca làm việc theo thứ của Coach. |
| `coach_days_off` | Ngày nghỉ riêng của Coach. |
| `advanced_registrations` | Chọn môn/Coach, duyệt nhận và liên kết quyền học. |
| `session_absence_notices` | Báo vắng sát giờ và lịch sử supersede. |
| `session_changes` | Lịch sử dời/thay buổi, consent và snapshot trước/sau. |
| `center_incidents` | Sự cố trung tâm, evidence và kết quả phục hồi. |
| `incident_affected_members` | Member bị ảnh hưởng bởi sự cố. |
| `payment_resolutions` | Hồ sơ Refund/CashCorrection và kết quả xử lý. |
| `scms_data_migration_audit` | Ghi nhận migration một lần, bảo vệ replay. |

## Giải thích rõ bảng `classes` và quan hệ với `sessions`

`classes` lưu thông tin cấp khóa/template; `sessions` lưu từng buổi học cụ thể, gồm ngày, giờ, sân/phòng và Coach. Quan hệ là **`sessions.class_id` → `classes.class_id`**: một khóa/template có thể được nhiều bản ghi buổi học tham chiếu.

Danh sách sau thuộc **bộ seed dựng lại trong [database/schema_postgres.sql](database/schema_postgres.sql)**, không phải cam kết ID/số lượng của DB nâng cấp bằng schema root:

| Dữ liệu trong `classes` | Phân biệt bằng | Số dòng seed | ID seed | Ý nghĩa |
|---|---|---:|---|---|
| Khóa Normal | `class_type='Normal'` | 9 | 10–18 | Mỗi môn có một khóa, tên `<Môn> - Normal - K01`, có lịch 12 buổi hợp lệ trong `sessions`. Member ghi danh qua `class_enrollments`. |
| Template Advanced | `class_type='Advanced'` | 9 | 1–9 | Mỗi môn có một template, tên `<Môn> - Advanced (1:1)`, sĩ số 1. Không tạo sẵn 12 buổi cố định cho từng Member; các ca 1:1 được đặt linh hoạt trong `sessions`, gắn Member/Coach/quyền học cụ thể. |

**Phân biệt “12 buổi” và số bản ghi:**

- `classes.total_sessions=12` là cấu hình số buổi của khóa/template, không phải số dòng thực tế đã có trong `sessions`. Với Advanced, quyền 12 credit và số credit còn lại được quản lý riêng qua `member_packages`.
- Normal có 12 buổi hợp lệ: đếm các dòng `sessions` của lớp có `center_invalidated=false` và `status<>'Cancelled'`.
- Ví dụ seed lớp Tennis Normal `class_id=10`: có **13 dòng** trong `sessions`. Buổi gốc bị hủy do sân bảo trì vẫn giữ lịch sử (`center_invalidated=true`); buổi bù liên kết về buổi gốc qua `makeup_for_session_id`. Vì vậy vẫn chỉ có **12 buổi hợp lệ**.

Để xem cả hai loại trong cùng bảng:

```sql
SELECT class_id, class_name, class_type, total_sessions
FROM public.classes
ORDER BY class_id;
```

Nguồn nghiệp vụ: [D03 — Advanced](docs/04-DEBUG-NOTE.md#d03) và [D15 — Normal/attendance/bù](docs/04-DEBUG-NOTE.md#d15). Các tên, số lượng và ID seed là dữ liệu tham chiếu được dựng cho demo; không phải lịch sử giao dịch thực tế và không dùng ID này để hardcode FE.
