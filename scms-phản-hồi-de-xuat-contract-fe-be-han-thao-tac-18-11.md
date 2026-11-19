1. Đồng ý mẫu Allowed, Deadline, BlockReason và serverTime. Allowed phục vụ việc mở hoặc khóa thao tác cho người đang đăng nhập, dựa trên các điều kiện BE xác định được tại lần đọc; BE vẫn kiểm lại khi lưu.

Cần chỉnh ba điểm trong bảng:
- Không mô tả registrationAllowed vừa theo người đăng nhập, vừa chỉ theo trạng thái lớp. Với Member tự đăng ký, phải xét những điều kiện của Member mà BE đã biết, chẳng hạn đã ghi danh rồi. Đăng ký hộ tại quầy còn phải kiểm Member được chọn khi gửi yêu cầu.
- Lớp đầy có thể chuyển sang danh sách chờ theo luồng hiện có; không mặc định đầy lớp là cấm mọi hình thức đăng ký.
- Xác nhận khai giảng cần đúng CenterManager đang Active, có MANAGE_CLASSES và đúng trung tâm; chỉ có permission chưa đủ.

Nói đơn giản: “Lớp đang mở đăng ký” và “người này được thực hiện thao tác đăng ký” là hai thông tin khác nhau. Tài liệu FE hiện đang trộn hai nghĩa đó.

2. Đồng ý dùng cùng mã cho cùng nguyên nhân giữa BlockReason và lỗi POST. BE cần dùng chung quy tắc kiểm và bổ sung mã cho những nhánh còn thiếu; không tái sử dụng mã correction cho ghi ban đầu chỉ vì nội dung gần giống.

BlockReason là kết quả tại lần đọc. Khi POST, BE kiểm lại và có thể trả nguyên nhân khác nếu thời gian hoặc dữ liệu đã thay đổi.

Có hai giới hạn:
- Dữ liệu có thể đổi sau lần đọc. Lúc mở trang còn được hủy, nhưng Coach điểm danh trước khi Member bấm thì POST phải trả SESSION_ALREADY_ATTENDED.
- Một số mã trong bảng FE chưa khớp luồng hiện tại. Chẳng hạn, mã chứa ATTENDANCE_CORRECTION đang nói về sửa điểm danh, không nên tự dùng làm mã của ghi điểm danh ban đầu.

3. Đồng ý: field thời gian mới có +07:00; field cũ giữ định dạng hiện tại.

4. Đồng ý hai mã trên, giữ HTTP 400 hiện có

5. Đồng ý thêm invoiceId và invoiceStatus vào AdvancedRegistrationResponse. Chưa có hóa đơn thì cả hai là null. invoiceStatus lấy từ Invoice.paymentStatus, không lấy từ trạng thái đăng ký hoặc quyền học.

Khi Member đăng nhập để xem đăng ký Advanced của chính mình, FE dùng invoiceId để đọc lại hóa đơn của đăng ký đó qua GET /api/members/me/invoices/{id}. Đã có hóa đơn thì mở hoặc hiển thị hóa đơn đó; không tạo thêm chỉ vì tải lại trang. Số tiền, phương thức, expiresAt và reconciliationRequired đọc từ response hóa đơn hiện có.

6. Đồng ý phát hiện. Khi bật SCMS_EARLY_ATTENDANCE_ENABLED, đường ghi điểm danh ban đầu của Normal phải nhận biết lớp đã được Manager xác nhận khai giảng qua dấu xác nhận đã lưu, dù lớp còn Open.

Phạm vi áp dụng gồm ghi đơn, bulk và QR. Normal vẫn cần Paid/Registered, đủ điều kiện khai giảng, đúng Coach và trung tâm. Advanced vẫn cần quyền Active hợp lệ và buổi Scheduled đã được tạo đúng luồng Accept. Điểm danh không trừ thêm credit; 0 credit không chặn buổi đã đặt hợp lệ.

Không mở ghi sau khi buổi kết thúc hoặc cho buổi đã hủy/vô hiệu/chốt điểm danh. Correction và nhận xét giữ quy tắc riêng. Khi tắt công tắc, giữ cửa giờ thông thường. QR phải xử lý đồng bộ cả kiểm giờ riêng trong scan.

FE đọc attendanceAllowed/attendanceBlockReason; không cần nút bật/tắt công tắc.

7. Đồng ý: buổi có điểm danh hiện hành Present/Absent không được hủy hoàn credit thường, kể cả còn hơn 12 giờ trước buổi học.

GET trả cancellationAllowed:false và cancellationBlockReason:SESSION_ALREADY_ATTENDED. POST hủy trả HTTP 409 với code này và thông báo “Buổi học đã được điểm danh, không thể hủy hoàn credit.”

Yêu cầu bị từ chối không đổi buổi, attendance, credit hoặc hạn quyền học. FE hiển thị lý do, tải lại dữ liệu và không báo hủy thành công. Trường hợp buổi đã Completed dùng mã riêng đã thống nhất ở mục 4.

Hai điểm cần thống nhất về logic FE:
- Thiếu field không đồng nghĩa được phép. Nếu chưa nhận được attendanceAllowed, FE chưa thể kết luận BE hỗ trợ điểm danh sớm. FE cần kiểm tra mapper có giữ field và đọc lại chi tiết buổi. Nếu vẫn thiếu, chức năng điểm danh sớm chưa được xác minh thì chưa mở. Với chức năng cũ, giữ cách xử lý hiện có theo phạm vi đã thống nhất; không dùng quy tắc chung “thiếu field thì mở mọi nút”.
- Sửa giới hạn 30 giây trên giao diện. Sau khi BE triển khai, mã điểm danh mới có thời hạn đủ 300 giây từ lúc cấp. FE cần sửa MemberDashboard.jsx đang từ chối thời hạn trên 30 giây và MemberAttendanceView.jsx đang cắt thời hạn xuống tối đa 30 giây. Các màn hình phải đọc thời hạn BE trả; tải lại mã không tự bắt đầu lại 300 giây. Thay đổi này chỉ áp dụng mã điểm danh, không đổi OTP, QR thanh toán hoặc điều kiện được điểm danh.
