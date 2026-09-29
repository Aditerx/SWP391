Hệ thống Quản lý Trung tâm Thể thao Sports Center Management System	"Center Manager – Quản lý trung tâm
Coach – Huấn luyện viên
Member – Học viên / Thành viên
Receptionist – Nhân viên lễ tân"	"


Center Manager (Quản lý Trung tâm)
+ Quản lý danh sách thành viên, huấn luyện viên và nhân viên của trung tâm.
+ Quản lý các lớp học, bộ môn, phòng tập và lịch hoạt động.
+ Phân công huấn luyện viên phụ trách từng lớp học.
+ Xem báo cáo số lượng thành viên, tình trạng đăng ký lớp và doanh thu theo thời gian.
+ Quản lý các gói thành viên, học phí và thời hạn sử dụng.
+ Phân quyền truy cập hệ thống cho từng vai trò.
+ Xem lịch sử thao tác quan trọng trên hệ thống.

Coach (Huấn luyện viên)
+ Xem lịch dạy và danh sách học viên trong các lớp phụ trách.
+ Xem thông tin cơ bản và mục tiêu tập luyện của từng học viên.
+ Tạo kế hoạch tập luyện cho cá nhân hoặc cho cả lớp.
+ Ghi nhận kết quả tập luyện của học viên sau mỗi buổi.
+ Đánh giá tiến độ của học viên và ghi nhận nhận xét.
+ Điểm danh học viên trong từng buổi tập.
+ Gửi thông báo hoặc bài tập về nhà cho học viên.
+ Sử dụng AI để gợi ý bài tập phù hợp dựa trên mục tiêu, trình độ và lịch sử tập luyện của học viên.

Member (Học viên / Thành viên)
+ Đăng ký tài khoản và cập nhật thông tin cá nhân.
+ Xem các gói thành viên và đăng ký/gia hạn gói tập.
+ Xem danh sách các lớp học và lịch học.
+ Đăng ký hoặc hủy đăng ký lớp học.
+ Xem lịch tập cá nhân và thông tin huấn luyện viên.
+ Xem lịch sử điểm danh và kết quả tập luyện.
+ Xem kế hoạch tập luyện và nhận xét từ huấn luyện viên.
+ Gửi câu hỏi cho hệ thống AI về lịch tập, bài tập hoặc các dịch vụ của trung tâm.
+ Nhận thông báo về lịch học, thay đổi lịch hoặc thời hạn gói thành viên.

Receptionist (Nhân viên Lễ tân)
+ Tìm kiếm và xem thông tin thành viên.
+ Đăng ký thành viên mới tại quầy.
+ Quản lý đăng ký/gia hạn các gói thành viên.
+ Kiểm tra trạng thái gói tập và thời hạn sử dụng của thành viên.
+ Điểm danh thành viên khi đến trung tâm.
+ Đăng ký lớp học hoặc hỗ trợ hủy lớp cho thành viên.
+ Ghi nhận các khoản thanh toán và in/xuất hóa đơn.
+ Tiếp nhận và ghi nhận các yêu cầu hỗ trợ từ thành viên."
	
Flow 1: User and membership management (required)
Flow 2: Class booking and schedule management (required)
Flow 3: Payment and report managment (required)"	
Flow 4: Training and attendence management (optional)
Flow 5: AI workout recommendation (optional)
Flow 6: AI assistant (optional)"

Dự định:  thêm 1 role cho admin, manager giữ nguyên
Một Center Manager nên tập trung toàn bộ vào:
- vận hành kinh doanh như xếp lịch
- phê duyệt hóa đơn
- phân công huấn luyện viên
- giải quyết khiếu nại và xem báo cáo doanh thu. 
Họ không nên có quyền can thiệp vào cấu trúc hệ thống, tự ý thay đổi bộ phân quyền (Permission) hay xóa vĩnh viễn các dữ liệu lõi. Nếu tài khoản của quản lý trung tâm bị lộ, việc họ nắm luôn quyền Admin sẽ khiến toàn bộ hệ thống rơi vào vòng nguy hiểm.

Suy ra: Admin là người sẽ quản trị hệ thống, còn manager, Receptionist, coach với member là user. Trang admin sẽ là 1 trang riêng chỉ có tài khoản admin login và admin sẽ người tạo tài khoản, phân quyền cho các user khác