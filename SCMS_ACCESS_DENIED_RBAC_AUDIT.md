# Báo cáo Access Denied / RBAC dành cho Backend

**Ngày rà soát:** 2026-11-04  
**Phạm vi:** Các API được frontend SCMS sử dụng và hợp đồng OpenAPI tại `D:/Study/FALL2026/SWP391/TONG_HOP/API.txt`; tập trung vào quyền truy cập của Admin, Center Manager, Receptionist, Coach và Member.  
**Mục đích:** Ghi nhận bằng chứng lỗi xác thực đã thấy và các điểm Backend cần xác nhận/kiểm thử để xử lý Access Denied.  
**Giới hạn:** Rà soát tĩnh, không gọi API/Render, không thay đổi dữ liệu hoặc mã Backend. Hành vi runtime sau các thay đổi proxy gần đây chưa được xác minh.

**Theo dõi BE:** [docs/03-TODO.md](docs/03-TODO.md) ghi ngắn vấn đề0, BE đã xử lý và yêu cầu FE; mục0.1 rà điều kiện gói hội viên chặn đăng ký khóa. Tiến độ/kết quả mới xem [Active](docs/00-MASTER-CHECKLIST.md#active); không coi các nhận định dưới đây là bug đã được xác nhận.

**Bàn giao FE đợt này:** [RBAC lần1 — 06/11/2026](SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md), gồm kết quả BE, yêu cầu tích hợp và ghi chú trạng thái trống để FE quyết định. Docs01 giữ contract/thiết kế chính thức.

**Đối chiếu sau mục0/0.1:** mục5 của bản LAN-1 chỉ rõ phần có thể bàn giao trước cho BE-01/BE-04/BE-05 và bằng chứng liên quan các mục khác. Đây là kết quả theo từng API, chưa đóng toàn bộ bảy yêu cầu bên dưới; phạm vi local/Supabase do Owner chạy được tách khỏi nghiệm thu FE/Render.

## 1. Các hạng mục Backend cần xử lý/xác minh

### BE-01 — Công bố ma trận quyền cho toàn bộ API và năm role — Ưu tiên P0

Hợp đồng API hiện không cho biết role/authority nào được phép gọi từng endpoint. Xin BE cung cấp và áp dụng một ma trận tối thiểu gồm: HTTP method, endpoint, role/authority được phép, phạm vi đối tượng được phép truy cập, và mã trạng thái khi bị từ chối.

Các nhóm endpoint cần có trong ma trận:

- **Admin:** `/api/users`, `/api/roles`, `/api/permissions`, `/api/audit-logs`, `/api/specializations`.
- **Center Manager:** `/api/members`, `/api/staff`, `/api/packages`, `/api/subjects`, `/api/rooms`, `/api/classes`, `/api/holidays`, `/api/sessions`, `/api/reports/*`, `/api/audit-logs`, `/api/invoices`.
- **Receptionist:** thành viên, điểm danh, đăng ký lớp, hóa đơn và thông báo; xác định rõ API đọc/ghi nào được cấp.
- **Coach:** lớp được phân công, điểm danh, học viên trong lớp, đánh giá, kế hoạch/kết quả tập luyện, buổi tập và đăng ký nâng cao.
- **Member:** hồ sơ, hóa đơn, đăng ký, gói dịch vụ, buổi tập, điểm danh và thông báo của chính tài khoản đó.

Đây là khoảng trống tài liệu/chứng cứ hiện tại; chưa khẳng định policy trên server đang sai.

### BE-02 — Xác minh vòng đời phiên và phản hồi 401 — Ưu tiên P0

Các lỗi lịch sử trên `/api/auth/me` cùng nhiều API nghiệp vụ có thể cùng phát sinh khi cookie phiên không được gửi, hết hạn, hoặc phiên bị server từ chối. BE cần xác nhận:

1. Login tạo phiên và cookie `JSESSIONID` theo đúng cấu hình môi trường; các API cùng origin chấp nhận cookie đó.
2. `/api/auth/me` trả 401 chỉ khi chưa đăng nhập/phiên hết hạn, và phản hồi có cấu trúc ổn định để client xử lý.
3. Khi một request không có phiên hợp lệ, API nghiệp vụ trả 401 nhất quán; không trả 403 cho trường hợp chưa xác thực.
4. Kiểm thử cùng một phiên trên `/api/auth/me`, `/api/packages`, `/api/members`, `/api/classes` và `/api/notifications/me` để xác định có API nào từ chối sai phiên hay không.

Log cũ không kèm request login, cookie request/response hoặc body nên chưa thể xác định lỗi thuộc cấu hình phiên Backend. Cần tái kiểm tra bằng log mới sau khi frontend hoàn tất điều chỉnh proxy cookie.

### BE-03 — Ràng buộc dữ liệu Coach theo lớp được giao — Ưu tiên P0, rủi ro cần xác minh

Frontend Coach gọi các danh sách attendance, enrollment, evaluation, training plan/result, session, feedback, học viên cần chú ý và đăng ký nâng cao. BE cần bảo đảm mỗi truy vấn chỉ trả dữ liệu thuộc lớp/học viên mà Coach hiện tại được phân công; không dựa vào việc frontend lọc kết quả.

Ngoài ra, frontend có hai cách đọc enrollment theo lớp: `GET /api/classes/{classId}/enrollments` và phương án dự phòng `GET /api/enrollments?classId=...`. BE cần áp dụng cùng một policy và kiểm tra quyền sở hữu lớp cho cả hai đường dẫn.

Chưa có response dữ liệu hoặc thử nghiệm truy cập chéo để chứng minh hiện có lộ dữ liệu.

### BE-04 — Ngăn truy cập chéo dữ liệu Member (IDOR) — Ưu tiên P0, rủi ro cần xác minh

Một số request dùng `memberId` hoặc ID trong đường dẫn để lấy enrollment, subscription, session và attendance. BE cần xác thực chủ thể từ phiên đăng nhập và chỉ cho Member đọc dữ liệu của chính mình. Với role nhân viên, quyền đọc thay mặt Member phải được nêu rõ trong ma trận quyền và giới hạn theo nhiệm vụ.

Không tin `memberId` do client gửi làm bằng chứng quyền truy cập. Kiểm thử âm bằng cách thay ID của một Member khác trên từng API và xác nhận server trả 403 (hoặc 404 theo chính sách chống dò ID) mà không trả dữ liệu.

Đây là yêu cầu kiểm tra ranh giới truy cập; chưa có bằng chứng cho thấy hiện đã xảy ra IDOR.

### BE-05 — Xác định quyền xác nhận thanh toán tiền mặt — Ưu tiên P1

Xác nhận policy cho `POST /api/invoices/{id}/confirm-cash`: role/authority nào được phép, trạng thái hóa đơn nào được chuyển, và cách giới hạn theo cơ sở nếu có. Frontend hiện có các màn hình thể hiện không thống nhất về Manager và Receptionist; BE cần là nơi thực thi policy chuẩn, không dựa vào ẩn/hiện nút phía client.

Kiểm thử cho Receptionist, Center Manager, Coach, Member và Admin; xác nhận role được phép thao tác thành công, role không được phép nhận 403, và thao tác lặp/hoá đơn sai trạng thái không làm thay đổi dữ liệu.

### BE-06 — Quyền đọc báo cáo và audit log — Ưu tiên P1

Xác nhận riêng policy cho `GET /api/audit-logs`, `GET /api/reports/dashboard`, `GET /api/reports/revenue` và `GET /api/reports/members`. Frontend gọi audit log từ khu vực Admin và Manager; hợp đồng hiện chưa nêu role hoặc phạm vi bản ghi được phép xem.

BE cần xác định Manager có được đọc audit log toàn hệ thống hay chỉ trong cơ sở được quản lý, và xác nhận Coach/Receptionist/Member không thể đọc báo cáo hoặc audit log nếu không có quyền. Kiểm tra cả phân trang, bộ lọc và ID bản ghi để tránh mở rộng phạm vi qua query parameter.

### BE-07 — Chuẩn hóa phân biệt 401/403 và hợp đồng lỗi — Ưu tiên P1

Đề nghị cập nhật OpenAPI và phản hồi thực tế để thống nhất:

- `401 Unauthorized`: thiếu, sai hoặc hết hạn xác thực.
- `403 Forbidden`: đã xác thực nhưng role/authority hoặc phạm vi đối tượng không cho phép.
- Body lỗi có mã máy đọc được, thông điệp an toàn và correlation/request ID; không trả stack trace hay thông tin nhạy cảm.
- Khai báo phản hồi 401/403 cho từng operation thay vì chỉ khai báo security cookie chung.

OpenAPI hiện thiếu thông tin 403 và policy theo operation nên chưa đủ để đối chiếu các lỗi Access Denied.

## 2. Ma trận xác minh bắt buộc cho BE

| Role | Kiểm tra truy cập được phép | Kiểm tra truy cập phải bị từ chối |
|---|---|---|
| Admin | API quản trị người dùng, role, permission và audit log theo policy đã công bố. | Thử truy cập dữ liệu ngoài phạm vi/quyền nếu Admin bị giới hạn; xác nhận các thao tác nhạy cảm cần authority cụ thể. |
| Center Manager | API quản lý thành viên, nhân viên, lớp, phòng, gói, hóa đơn và báo cáo trong đúng cơ sở. | Truy cập cơ sở khác; API chỉ dành Admin; thao tác tiền mặt nếu policy không cấp quyền. |
| Receptionist | API phục vụ tiếp nhận, tra cứu thành viên, điểm danh/đăng ký và thu tiền theo policy. | API quản trị hệ thống, phân quyền, audit log/báo cáo nếu không được cấp; hóa đơn ngoài phạm vi. |
| Coach | Dữ liệu lớp, buổi tập và học viên được giao. | Lớp/học viên không được giao; dữ liệu quản lý, phân quyền, hóa đơn và dữ liệu Member khác. |
| Member | Hồ sơ, hóa đơn, đăng ký, buổi tập, điểm danh và thông báo của chính mình. | Thay ID để đọc/sửa dữ liệu Member khác; mọi API quản trị hoặc nhân sự. |

Mỗi ca cần ghi role, endpoint/method, trạng thái đăng nhập, đối tượng/cơ sở được yêu cầu, status/body trả về và kết quả kỳ vọng. Thực hiện cả ca positive lẫn negative trên Backend; không dùng ẩn nút hoặc chặn route frontend làm bằng chứng kiểm soát truy cập.

## 3. Tài liệu và trạng thái kiểm chứng

- Hợp đồng được rà soát: `D:/Study/FALL2026/SWP391/TONG_HOP/API.txt` (OpenAPI 3.0.1). Có khai báo cookie session `JSESSIONID`, nhưng thiếu ma trận role/authority, object scope và phản hồi 403 theo endpoint.
- Log trình duyệt do người dùng cung cấp: 13 dòng 401 trên 10 endpoint như bảng ở phần 2; đây là log cũ, chưa đủ thông tin quy nguyên nhân cho Backend.
- Không gọi API/Render trong lần rà soát này. Không có kết quả runtime mới, response body, server log hoặc kiểm thử trực tiếp từng role.
- Do đó các BE-01 đến BE-07 là yêu cầu BE xác nhận/kiểm thử. Chỉ mở thành bug đã xác nhận khi có request/response hoặc server log tái hiện và policy kỳ vọng được thống nhất.
