Nguồn cấu trúc: [schema_postgres.sql](../../database/schema_postgres.sql).

Với một ứng dụng chỉ quản lý học viên, lớp, lịch và thu tiền, 55 bảng là khá nhiều. SCMS tăng lên mức này vì có:
- Hai mô hình học: Normal theo lớp và Advanced theo môn/Coach/credit/hạn.
- Đổi lịch, báo vắng, sự cố, bù và phục hồi quyền.
- Thanh toán, đối soát, chuyển lớp, hoàn tiền và bằng chứng xử lý.
- Phân quyền, chat, thông báo, audit và vài cấu trúc cũ/dự phòng.
Riêng thay đổi Normal/Refund gần đây đã thêm 8 bảng. Vì vậy, một chức năng “hoàn tiền” có thể cần nhiều bảng, dù trên giao diện chỉ là một luồng.

55 bảng có thể được chia thành 9 nhóm như sau.

| Nhóm | Bảng | Chức năng cơ bản |
|---|---|---|
| **Tài khoản và phân quyền — 10** | `centers` | Danh mục trung tâm; phạm vi quản lý tài khoản, lớp, phòng. |
| | `users` | Thông tin đăng nhập và thông tin chung của mọi tài khoản. |
| | `roles` | Vai trò: Admin, Manager, Receptionist, Coach, Member. |
| | `permissions` | Danh mục quyền thao tác. |
| | `role_permissions` | Bảng nối vai trò với các quyền được cấp. |
| | `email_otps` | OTP xác minh email/khôi phục tài khoản, hạn và số lần thử. |
| | `members` | Hồ sơ riêng của Member: mục tiêu, sức khỏe, thông tin check-in… |
| | `coaches` | Hồ sơ riêng của Coach: chứng chỉ, giới thiệu… |
| | `center_managers` | Hồ sơ loại Manager; hiện chỉ chứa `user_id`. |
| | `receptionists` | Hồ sơ loại Receptionist; hiện chỉ chứa `user_id`. |
| **Danh mục và khả năng làm việc — 7** | `subjects` | Môn thể thao và giá Normal/Advanced hiện hành. |
| | `specializations` | Chuyên môn của Coach, có thể liên kết một môn. |
| | `coach_specializations` | Nối Coach với nhiều chuyên môn. |
| | `rooms` | Phòng/sân, sức chứa, loại tài nguyên và trạng thái. |
| | `coach_work_schedules` | Cấu hình lịch Coach và việc nhận đăng ký mới. |
| | `coach_work_slots` | Các ca làm việc lặp theo thứ trong tuần. |
| | `coach_days_off` | Những ngày nghỉ ngoại lệ của Coach. |
| **Lớp và học tập — 8** | `classes` | Lớp Normal hoặc template Advanced; môn, Coach, giá, sĩ số, trạng thái. |
| | `sessions` | Buổi học cụ thể: ngày, ca, phòng, Coach; có thông tin booking Advanced. |
| | `class_enrollments` | Ghi danh Member vào lớp Normal và trạng thái ghi danh. |
| | `training_plans` | Giáo án chung cho lớp hoặc giáo án riêng cho Member. |
| | `training_results` | Kết quả/nhận xét của Coach cho Member trong một buổi. |
| | `attendances` | Điểm danh, check-in/out, ghi chú và lịch sử liên quan. |
| | `evaluations` | Coach đánh giá tiến bộ của Member theo ngày. |
| | `class_feedbacks` | Member đánh giá lớp và Coach. |
| **Quyền học Advanced và mô hình gói — 4** | `advanced_registrations` | Yêu cầu đăng ký Advanced, Coach tiếp nhận và kết quả xử lý. |
| | `member_packages` | Quyền Advanced của từng Member: môn, Coach chính, credit, hạn, giá mua. |
| | `subscription_pauses` | Khoảng tạm dừng được dùng khi tính hạn quyền học. |
| | `membership_packages` | Danh mục gói hội viên chung **đã ngừng sử dụng**, còn cấu trúc để tương thích. |
| **Lịch và ngoại lệ — 5** | `holidays` | Ngày nghỉ/đóng cửa của trung tâm. |
| | `session_absence_notices` | Báo vắng của Member, giữ thời điểm báo và lịch buổi lúc báo. |
| | `session_changes` | Lịch sử thay đổi buổi: trước/sau, lý do, người thao tác, chống lặp. |
| | `center_incidents` | Sự cố trung tâm gắn với buổi và quá trình phục hồi hạn/credit. |
| | `incident_affected_members` | Danh sách Member bị ảnh hưởng bởi một sự cố. |
| **Thu tiền và đối soát — 3** | `invoices` | Chứng từ thu tiền; số tiền gốc, trạng thái, lớp/quyền học liên quan. |
| | `vnpay_payment_attempts` | Metadata từng lần tạo thanh toán VNPay để xác minh giao dịch. |
| | `payment_resolutions` | Hồ sơ xử lý tiền như Refund/CashCorrection; duyệt và hoàn tất. |
| **Vòng đời Normal và hoàn tiền — 9** | `normal_class_requests` | Khóa chống tạo lớp Normal lặp khi gửi lại request. |
| | `normal_proposals` | Đề xuất thay đổi lớp/lịch, nội dung và phiên bản cần đồng thuận. |
| | `normal_responses` | Lựa chọn của từng Member/hóa đơn đối với đề xuất. |
| | `normal_transfers` | Dấu vết chuyển ghi danh từ lớp nguồn sang lớp đích. |
| | `normal_refunds` | Thông tin riêng của hồ sơ hoàn tiền Normal. |
| | `normal_refund_receivers` | Các thông tin tài khoản nhận tiền đã được xác nhận/xác minh. |
| | `normal_refund_attempts` | Từng lần thực hiện hoàn tiền, số tiền và trạng thái xử lý. |
| | `normal_refund_documents` | Chứng từ riêng tư đính kèm lần hoàn tiền. |
| | `normal_refund_verifications` | Lịch sử kiểm chứng kết quả chuyển tiền. |
| **Trao đổi và hỗ trợ — 7** | `notifications` | Nội dung/sự kiện thông báo; vẫn có cấu trúc gửi trực tiếp kiểu cũ. |
| | `notification_recipients` | Người nhận và trạng thái đọc riêng của từng người. |
| | `chat_conversations` | Cuộc hội thoại hỗ trợ, người phụ trách, trạng thái. |
| | `chat_messages` | Các tin nhắn trong cuộc hội thoại. |
| | `chat_read_state` | Vị trí đọc cuối của từng người trong hội thoại. |
| | `support_requests` | Cấu trúc yêu cầu hỗ trợ, được giữ cho hướng phát triển sau. |
| | `ai_consultations` | Cấu trúc lưu hỏi–đáp AI riêng, được giữ cho hướng phát triển sau. |
| **Audit và bảo trì — 2** | `system_logs` | Audit thao tác nghiệp vụ: ai làm gì, trên đối tượng nào. |
| | `scms_data_migration_audit` | Marker migration và thông tin đối chiếu dữ liệu trước/sau. |


55 bảng không tương đương 55 chức năng độc lập. Ví dụ role_permissions, coach_specializations, incident_affected_members thể hiện quan hệ nhiều–nhiều; chúng không tạo thêm màn hình nghiệp vụ.

Để trình bày ERD, Tôi đề nghị giản lược theo mức nhìn trước.
1. Bản tổng quan: chọn khoảng 14 bảng chính: centers, users, members, coaches, subjects, rooms, classes, class_enrollments, sessions, member_packages, invoices, attendances, training_results, training_plans. Ghi rõ đây là ERD tổng quan, chỉ hiện các quan hệ chính.
2. Bản chi tiết theo mô-đun: tách tài khoản/phân quyền; lớp và học tập; Advanced và lịch; thanh toán/Normal/Refund; trao đổi và audit.
3. Bản vật lý đầy đủ: vẫn giữ đủ 55 bảng trong phụ lục nếu yêu cầu phản ánh đúng schema đang có.
Ví dụ một phần lõi Normal có thể trình bày rất gọn:

"D:\Projects\SWP391\image-normal-class-erd-example.png"
(ảnh đính kèm)

Sơ đồ này là một phần của ERD, đã lược các bảng và quan hệ khác.

Khi vẽ bản chính thức, có vài điểm dễ sai cần giữ đúng:
- members và coaches dùng khóa hồ sơ bằng chính users.user_id; không phải những tài khoản độc lập.
- Quan hệ Member–Class Normal đi qua class_enrollments.
- invoices không có FK trực tiếp enrollment_id. Không vẽ Invoice–Enrollment thành quan hệ FK có sẵn.
- invoices.class_id giữ lớp gốc của chứng từ; entitlement_class_id thể hiện lớp có quyền hiệu lực sau chuyển lớp.
- training_plans nhắm một lớp hoặc một Member, có ràng buộc loại trừ hai đích.
- Attendance có thể không gắn Session đối với check-in lobby; không ép mọi Attendance thuộc một buổi.
- training_results có unique theo (session_id, member_id); không suy Attendance và Result luôn có quan hệ một–một.
- member_packages là quyền Advanced đang hoạt động; cần giữ trong phần lõi dù membership_packages đã retire.
Bạn có thể giới thiệu thiết kế bằng câu: “SCMS có 55 bảng vật lý, được chia thành các mô-đun nghiệp vụ và hỗ trợ. ERD tổng quan trình bày phần lõi; các quan hệ chi tiết, lịch sử và đối soát được tách sang từng mô-đun.”