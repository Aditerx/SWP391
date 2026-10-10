Bạn là Senior Product Designer và UX Architect. Hãy thiết kế UI/UX hoàn chỉnh cho hệ thống web:

SPORTS CENTER MANAGEMENT SYSTEM – SCMS
HỆ THỐNG QUẢN LÝ TRUNG TÂM THỂ THAO

1. MỤC TIÊU SẢN PHẨM

Thiết kế một hệ thống web giúp một trung tâm thể thao quản lý:

* Nhân viên và thành viên.
* Gói thành viên và đăng ký hội viên.
* Lớp học, lịch học, phòng tập và huấn luyện viên.
* Đăng ký và hủy đăng ký lớp.
* Điểm danh học viên.
* Kế hoạch và kết quả tập luyện.
* Thanh toán gói thành viên.
* Báo cáo vận hành cơ bản.
* Nhật ký các thao tác quan trọng.

Sản phẩm MVP chỉ phục vụ một trung tâm thể thao.

2. NỀN TẢNG THIẾT KẾ

Chỉ thiết kế giao diện web cho desktop và laptop.

Không thiết kế:

* Mobile application.
* Mobile responsive screens.
* Tablet application.
* Native iOS hoặc Android.
* Mobile navigation.
* Bottom navigation.
* Các màn hình riêng dành cho điện thoại.

Kích thước frame đề xuất:

* Desktop tiêu chuẩn: 1440 × 1024 px.
* Laptop: 1280 × 800 px.
* Large desktop: 1920 × 1080 px.
* Chiều rộng hỗ trợ tối thiểu: 1024 px.

Thiết kế cần thích ứng tốt giữa các độ rộng desktop/laptop bằng Auto Layout và responsive constraints, nhưng không cần xây dựng layout dành riêng cho mobile hoặc tablet.

3. YÊU CẦU SONG NGỮ

Hệ thống phải hỗ trợ đầy đủ hai ngôn ngữ:

* Tiếng Việt – Vietnamese.
* Tiếng Anh – English.

Yêu cầu thiết kế:

* Có Language Switcher trên thanh điều hướng phía trên.
* Hiển thị tùy chọn “VI | EN” hoặc menu chọn “Tiếng Việt / English”.
* Ngôn ngữ đã chọn phải được duy trì khi người dùng chuyển trang hoặc đăng nhập lại.
* Không trộn lẫn tiếng Việt và tiếng Anh trong cùng một giao diện, ngoại trừ tên riêng hoặc mã nghiệp vụ.
* Thiết kế phải xử lý được sự khác biệt về độ dài giữa nội dung tiếng Việt và tiếng Anh.
* Button, tab, menu, table header, badge và form label không bị cắt khi đổi ngôn ngữ.
* Không cố định chiều rộng nút dựa trên nội dung của một ngôn ngữ.
* Sử dụng Auto Layout cho các component chứa văn bản.
* Chuẩn bị text styles và content variables để quản lý hai bộ nội dung.
* Các màn hình quan trọng cần có ít nhất một phiên bản tiếng Việt và một phiên bản tiếng Anh để kiểm tra layout.
* Tạo component Language Switcher với đầy đủ trạng thái default, hover, open và selected.

Ví dụ nội dung song ngữ:

| Tiếng Việt        | English             |
| ----------------- | ------------------- |
| Tổng quan         | Dashboard           |
| Thành viên        | Members             |
| Nhân viên         | Staff               |
| Huấn luyện viên   | Coaches             |
| Gói thành viên    | Membership Packages |
| Hội viên          | Memberships         |
| Lớp học           | Classes             |
| Lịch học          | Schedule            |
| Đăng ký lớp       | Class Bookings      |
| Điểm danh         | Attendance          |
| Tiến độ tập luyện | Training Progress   |
| Thanh toán        | Payments            |
| Báo cáo           | Reports             |
| Nhật ký hệ thống  | Audit Logs          |
| Đang hoạt động    | Active              |
| Chờ thanh toán    | Pending Payment     |
| Thời gian gia hạn | Grace Period        |
| Hết hạn           | Expired             |
| Đã hủy            | Cancelled           |
| Có mặt            | Present             |
| Đi muộn           | Late                |
| Vắng mặt          | Absent              |
| Lưu thay đổi      | Save Changes        |
| Hủy               | Cancel              |
| Xác nhận          | Confirm             |
| Tìm kiếm          | Search              |
| Không có dữ liệu  | No Data Available   |

Quy tắc bản địa hóa:

* Khi dùng tiếng Việt, ngày tháng hiển thị theo định dạng DD/MM/YYYY.
* Khi dùng tiếng Anh, ngày tháng hiển thị theo định dạng phù hợp như DD MMM YYYY.
* Thời gian sử dụng định dạng 24 giờ ở giao diện tiếng Việt.
* Tiền tệ mặc định là VND trong cả hai ngôn ngữ.
* Số tiền cần có dấu phân cách hàng nghìn.
* Không đặt trực tiếp text song ngữ theo dạng “Thành viên / Members” trên giao diện thật.
* Mỗi thời điểm giao diện chỉ hiển thị một ngôn ngữ được chọn.

4. VAI TRÒ NGƯỜI DÙNG

4.1. Center Manager – Quản lý trung tâm

* Quản lý tài khoản nhân viên.
* Quản lý hồ sơ thành viên.
* Quản lý gói thành viên và quyền lợi.
* Quản lý lớp học, lịch học, phòng tập và sức chứa.
* Phân công huấn luyện viên.
* Xem thanh toán và doanh thu.
* Xem báo cáo tổng quan.
* Xem nhật ký thao tác.
* Có quyền truy cập toàn bộ dữ liệu của trung tâm.

4.2. Coach – Huấn luyện viên

* Xem lịch dạy và các lớp được phân công.
* Xem danh sách học viên.
* Điểm danh học viên.
* Chỉnh sửa điểm danh nhưng bắt buộc nhập lý do.
* Xem các thành viên được phân công.
* Tạo kế hoạch tập luyện.
* Ghi nhận kết quả và nhận xét.
* Chỉ truy cập thành viên và lớp được phân công.
* Không truy cập nghiệp vụ quản lý thanh toán.

4.3. Receptionist – Nhân viên lễ tân

* Tìm kiếm và quản lý hồ sơ thành viên.
* Tạo thành viên mới.
* Đăng ký gói thành viên.
* Đăng ký hoặc hủy lớp thay cho thành viên.
* Ghi nhận thanh toán thủ công hoặc mô phỏng.
* Xem trạng thái hội viên.
* Không được bỏ qua điều kiện kích hoạt hội viên hoặc chỉnh sửa âm thầm giao dịch đã hoàn tất.

4.4. Member – Thành viên

* Xem hồ sơ cá nhân.
* Xem gói thành viên và trạng thái sử dụng.
* Tìm kiếm, đăng ký và hủy đăng ký lớp.
* Xem lịch học sắp tới.
* Xem lịch sử đăng ký và điểm danh.
* Xem kế hoạch, kết quả và nhận xét tập luyện.
* Chỉ truy cập dữ liệu của chính mình.
* Không sửa nội dung do Coach ghi nhận.

Tất cả vai trò, bao gồm Member, đều sử dụng giao diện web trong giai đoạn hiện tại.

5. PHẠM VI CHỨC NĂNG MVP

5.1. Authentication and Authorization

* Đăng nhập bằng email hoặc tên đăng nhập và mật khẩu.
* Đăng xuất.
* Hiển thị lỗi đăng nhập chung, không tiết lộ trường nào sai.
* Điều hướng đến dashboard tương ứng với vai trò.
* Trang 403 Không có quyền truy cập.
* Trang 404 Không tìm thấy dữ liệu.
* Kiểm soát quyền truy cập đến menu, màn hình và action theo vai trò.

5.2. Staff and Member Management

* Danh sách nhân viên.
* Tạo và chỉnh sửa nhân viên.
* Tạm khóa hoặc kích hoạt lại nhân viên.
* Danh sách thành viên.
* Tìm kiếm, lọc và sắp xếp thành viên.
* Tạo và cập nhật hồ sơ thành viên.
* Trang chi tiết thành viên gồm:

  * Thông tin cá nhân.
  * Gói thành viên hiện tại.
  * Lịch sử gói.
  * Lớp đã đăng ký.
  * Lịch sử điểm danh.
  * Huấn luyện viên chính.
  * Tiến độ tập luyện.
  * Lịch sử thanh toán.

5.3. Membership Packages and Memberships

* Danh sách gói thành viên.
* Tạo và chỉnh sửa gói.
* Thông tin gói:

  * Tên gói.
  * Mức giá.
  * Thời hạn.
  * Mô tả.
  * Quyền lợi.
  * Giới hạn số lớp.
  * Trạng thái.
* Gói có giá cao hơn phải bao gồm quyền lợi của các gói thấp hơn.
* Thể hiện trực quan quan hệ kế thừa quyền lợi giữa các gói.
* Trạng thái hội viên:

  * Pending Payment.
  * Active.
  * Grace Period.
  * Expired.
  * Cancelled.
* Membership chỉ được kích hoạt sau khi thanh toán thành công.
* Grace Period kéo dài đúng 72 giờ sau thời điểm hết hạn hợp đồng.
* Sau Grace Period, quyền sử dụng dịch vụ bị khóa tự động.

5.4. Class Management

* Danh sách lớp dạng bảng.
* Lịch lớp dạng calendar.
* Tạo và chỉnh sửa lớp.
* Các trường:

  * Tên lớp.
  * Bộ môn.
  * Huấn luyện viên.
  * Phòng tập.
  * Ngày và giờ.
  * Thời lượng.
  * Sức chứa.
  * Trạng thái.
* Cảnh báo khi:

  * Trùng lịch huấn luyện viên.
  * Trùng lịch phòng.
  * Thiếu dữ liệu bắt buộc.
  * Sức chứa không hợp lệ.
* Mỗi class session là một buổi học có ngày giờ cụ thể.
* Lịch lặp lại là tùy chọn phụ, không phải luồng chính của MVP.

5.5. Class Booking

* Catalog lớp học.
* Tìm kiếm và lọc theo bộ môn, ngày, giờ, huấn luyện viên và chỗ trống.
* Trang chi tiết lớp.
* Đăng ký lớp.
* Hủy đăng ký.
* Hiển thị sức chứa và số chỗ còn lại.
* Không cho phép đăng ký khi:

  * Membership đang chờ thanh toán.
  * Membership đã hết hạn.
  * Membership đã bị hủy.
  * Lớp đã đủ chỗ.
  * Thành viên đã đăng ký lớp.
  * Gói không có quyền tham gia lớp.
* Hiển thị rõ nguyên nhân bị từ chối và hướng xử lý.
* Không thiết kế waitlist trong MVP.

5.6. Attendance

* Coach xem danh sách học viên theo buổi học.
* Điểm danh theo trạng thái:

  * Present.
  * Late.
  * Absent.
* Hỗ trợ thao tác điểm danh nhanh.
* Khi sửa điểm danh:

  * Hiển thị giá trị cũ.
  * Hiển thị giá trị mới.
  * Bắt buộc nhập lý do.
  * Cảnh báo thao tác sẽ được ghi vào Audit Log.
* Member chỉ xem lịch sử điểm danh của mình.
* Không thiết kế facility check-in trong MVP.

5.7. Training Progress

* Coach xem thành viên được phân công.
* Mỗi thành viên chỉ có một Coach chính đang hoạt động tại một thời điểm.
* Coach có thể:

  * Tạo kế hoạch tập luyện.
  * Ghi mục tiêu.
  * Ghi nội dung bài tập.
  * Ghi kết quả.
  * Thêm nhận xét.
* Member xem kế hoạch và tiến độ.
* Member không được sửa dữ liệu do Coach tạo.
* Trình bày tiến độ bằng timeline, progress card, biểu đồ đơn giản và danh sách kết quả theo ngày.
* Không thiết kế chức năng chẩn đoán hoặc tư vấn y tế.

5.8. Payments

* Receptionist ghi nhận thanh toán thủ công hoặc mô phỏng.
* Không tích hợp cổng thanh toán thật trong MVP.
* Form thanh toán:

  * Người thanh toán.
  * Thành viên.
  * Gói thành viên.
  * Số tiền.
  * Phương thức thanh toán.
  * Thời gian thanh toán.
  * Trạng thái.
  * Ghi chú.
* Trạng thái giao dịch:

  * Successful.
  * Pending.
  * Failed.
  * Reversed.
* Chỉ giao dịch Successful mới kích hoạt membership.
* Hiển thị chi tiết và lịch sử giao dịch.
* Không cho phép thay đổi âm thầm giao dịch đã hoàn tất.
* Không thiết kế yêu cầu hoàn tiền dành cho Member.

5.9. Manager Dashboard and Reports

Hiển thị các KPI:

* Tổng số thành viên.
* Thành viên đang hoạt động.
* Hội viên chờ thanh toán.
* Hội viên đang trong Grace Period.
* Số lớp trong ngày.
* Tổng lượt đăng ký.
* Tỷ lệ điểm danh.
* Doanh thu từ thanh toán thành công.
* Số giao dịch Pending.
* Số giao dịch Failed.

Dashboard gồm:

* Bộ lọc khoảng thời gian.
* Biểu đồ doanh thu.
* Biểu đồ số lượng thành viên.
* Biểu đồ đăng ký và điểm danh.
* Danh sách lớp sắp diễn ra.
* Danh sách hội viên cần chú ý.
* Bảng trạng thái thanh toán.

Quy tắc báo cáo:

* Chỉ cộng giao dịch Successful vào doanh thu.
* Lọc doanh thu theo thời điểm thanh toán thành công.
* Pending, Failed và Reversed phải được trình bày riêng.

5.10. Audit Log

* Danh sách nhật ký thao tác.
* Các trường:

  * Thời gian.
  * Người thực hiện.
  * Vai trò.
  * Hành động.
  * Đối tượng bị tác động.
  * Giá trị trước.
  * Giá trị sau.
  * Lý do.
* Lọc theo người dùng, hành động, module và thời gian.
* Các hành động bắt buộc ghi log:

  * Thay đổi vai trò.
  * Khóa hoặc kích hoạt tài khoản.
  * Ghi nhận hoặc điều chỉnh thanh toán.
  * Chỉnh sửa điểm danh.
* Audit Log chỉ đọc, không cho phép sửa hoặc xóa.

6. CÁC LUỒNG UX CHÍNH

Thiết kế và tạo prototype cho các luồng:

1. Login → Xác thực → Xác định vai trò → Role Dashboard.

2. Tạo thành viên → Chọn gói → Pending Payment → Thanh toán thành công → Active.

3. Tạo lớp → Chọn Coach và phòng → Kiểm tra xung đột → Xuất bản.

4. Tìm lớp → Xem chi tiết → Kiểm tra điều kiện → Đăng ký.

5. Coach mở buổi học → Xem danh sách → Điểm danh → Lưu.

6. Sửa điểm danh → Nhập lý do → Xác nhận → Tạo Audit Log.

7. Coach cập nhật kế hoạch và kết quả → Member xem tiến độ.

8. Manager chọn khoảng thời gian → Xem báo cáo → Drill down.

9. Người dùng đổi ngôn ngữ VI/EN → Nội dung màn hình được cập nhật nhưng giữ nguyên trang và dữ liệu hiện tại.

10. KIẾN TRÚC MÀN HÌNH

Public:

* Login.
* Forgot Password placeholder.
* 403.
* 404.

Manager:

* Dashboard.
* Staff Management.
* Member Management.
* Membership Packages.
* Memberships.
* Classes.
* Schedule Calendar.
* Payments.
* Reports.
* Audit Logs.
* Personal Profile.

Coach:

* Coach Dashboard.
* Teaching Schedule.
* Class Detail.
* Attendance.
* Assigned Members.
* Member Training Detail.
* Training Plans.
* Results and Comments.
* Personal Profile.

Receptionist:

* Receptionist Dashboard.
* Member Search.
* Member Detail.
* Create Member.
* Membership Registration.
* Class Booking.
* Payment Entry.
* Payment History.
* Personal Profile.

Member:

* Member Dashboard.
* My Membership.
* Class Catalog.
* Class Detail.
* My Bookings.
* Attendance History.
* Training Plan.
* Training Progress.
* Personal Profile.

8. WEB APPLICATION LAYOUT

Sử dụng application shell nhất quán:

Left Sidebar:

* Logo SCMS.
* Navigation theo vai trò.
* Trạng thái active của menu.
* Cho phép thu gọn sidebar.
* Menu không có quyền phải được ẩn, không chỉ disabled.

Top Bar:

* Breadcrumb hoặc page title.
* Global search nếu phù hợp.
* Language Switcher VI/EN.
* Notification placeholder.
* User avatar.
* Profile menu.
* Logout.

Main Content:

* Page header.
* Breadcrumb.
* Page title và mô tả ngắn.
* Primary action.
* Filters.
* Content area.
* Pagination hoặc supporting information.

Không sử dụng mobile hamburger menu làm navigation chính.

9. DESIGN SYSTEM

Phong cách:

* Hiện đại.
* Chuyên nghiệp.
* Năng động.
* Đáng tin cậy.
* Phù hợp lĩnh vực thể thao.
* Ưu tiên khả năng đọc dữ liệu và thao tác nhanh.
* Không sử dụng quá nhiều gradient hoặc hiệu ứng trang trí.

Màu đề xuất:

* Primary: navy hoặc blue.
* Secondary: cyan hoặc teal.
* Success: green.
* Warning: orange.
* Error: red.
* Neutral: hệ màu gray.

Typography:

* Sử dụng Inter hoặc sans-serif hiện đại hỗ trợ tốt dấu tiếng Việt.
* Xây dựng Display, H1, H2, H3, Body, Label và Caption.
* Cỡ chữ nội dung chính tối thiểu 14px.
* Kiểm tra typography với cả tiếng Việt và tiếng Anh.

Layout:

* Grid 12 cột.
* Spacing theo hệ 8px.
* Content container linh hoạt theo độ rộng trình duyệt.
* Không kéo giãn bảng và nội dung quá rộng trên màn hình 1920px.
* Các dashboard card cần tự sắp xếp phù hợp ở độ rộng 1024–1920px.

10. COMPONENT LIBRARY

Sử dụng Auto Layout, component properties, variants và variables.

Tạo các component:

* Button.
* Input.
* Textarea.
* Select.
* Date picker.
* Time picker.
* Search bar.
* Checkbox.
* Radio.
* Toggle.
* Tabs.
* Breadcrumb.
* Sidebar.
* Top bar.
* Language Switcher.
* KPI card.
* Data table.
* Pagination.
* Filter bar.
* Calendar.
* Modal.
* Drawer.
* Confirmation dialog.
* Toast.
* Alert.
* Empty state.
* Loading skeleton.
* Avatar.
* Dropdown.
* Tooltip.
* Timeline.
* Progress card.
* Chart container.
* Audit history item.
* Status badge.

Button variants:

* Primary.
* Secondary.
* Outline.
* Ghost.
* Destructive.
* Icon button.

Button states:

* Default.
* Hover.
* Focus.
* Pressed.
* Disabled.
* Loading.

Status badge variants:

Membership:

* Pending Payment.
* Active.
* Grace Period.
* Expired.
* Cancelled.

Payment:

* Successful.
* Pending.
* Failed.
* Reversed.

Class:

* Draft.
* Published.
* Full.
* Completed.
* Cancelled.

Attendance:

* Present.
* Late.
* Absent.
* Not Recorded.

11. FORM VÀ DATA TABLE

Form:

* Label hiển thị phía trên input.
* Trường bắt buộc có ký hiệu rõ ràng.
* Validation đặt ngay dưới trường.
* Form dài được chia thành section.
* Hiển thị cảnh báo khi rời form chưa lưu.
* Action nhạy cảm phải có confirmation dialog.
* Chỉnh sửa điểm danh bắt buộc nhập lý do.

Data table:

* Search.
* Filter.
* Sort.
* Pagination.
* Row actions.
* Column visibility nếu cần.
* Sticky table header với bảng dài.
* Empty state.
* Loading state.
* Error state.
* Không thiết kế card list thay thế dành cho mobile.

12. ACCESSIBILITY

* Tuân thủ WCAG 2.1 AA.
* Contrast đủ cao.
* Focus state rõ ràng.
* Hỗ trợ điều hướng bằng bàn phím.
* Không chỉ sử dụng màu sắc để biểu thị trạng thái.
* Icon cần có label hoặc tooltip.
* Error message phải giải thích nguyên nhân và cách xử lý.
* Dialog phải hỗ trợ focus trap.
* Component phải hoạt động với nội dung của cả hai ngôn ngữ.
* Không để text tiếng Việt hoặc tiếng Anh bị cắt, tràn hoặc chồng lấn.

13. MICROCOPY SONG NGỮ MẪU

Vietnamese:

* “Không thể đăng ký lớp vì gói thành viên của bạn đang chờ thanh toán.”
* “Lớp học đã đủ số lượng thành viên.”
* “Bạn đã đăng ký lớp học này.”
* “Gói thành viên không bao gồm quyền tham gia lớp này.”
* “Thay đổi điểm danh sẽ được lưu trong nhật ký hệ thống.”
* “Thanh toán thành công. Gói thành viên đã được kích hoạt.”
* “Giao dịch đang xử lý nên gói thành viên chưa được kích hoạt.”
* “Bạn không có quyền truy cập chức năng này.”

English:

* “You cannot book this class because your membership is awaiting payment.”
* “This class has reached its maximum capacity.”
* “You have already booked this class.”
* “Your membership package does not include access to this class.”
* “This attendance change will be recorded in the audit log.”
* “Payment successful. The membership has been activated.”
* “The transaction is still pending, so the membership has not been activated.”
* “You do not have permission to access this feature.”

14. NGOÀI PHẠM VI MVP

Không thiết kế các chức năng sau:

* Mobile hoặc tablet UI.
* Quản lý nhiều chi nhánh.
* Class waitlist.
* Facility check-in.
* Support ticket.
* Notification center hoàn chỉnh.
* Email hoặc push notification.
* Cổng thanh toán thật.
* Hóa đơn điện tử đầy đủ.
* Member refund request.
* AI chatbot.
* AI-generated training advice.
* MFA.
* Health application integration.
* Chẩn đoán hoặc tư vấn y tế.

15. CẤU TRÚC FILE FIGMA

Tổ chức file thành các page:

* 00 – Cover.
* 01 – Foundations.
* 02 – Localization VI-EN.
* 03 – Components.
* 04 – Information Architecture.
* 05 – User Flows.
* 06 – Manager Screens.
* 07 – Coach Screens.
* 08 – Receptionist Screens.
* 09 – Member Web Screens.
* 10 – States and Permissions.
* 11 – Web Responsive Tests.
* 12 – Prototype.
* 13 – UX Notes and Handoff.

Trong page Localization VI-EN, thể hiện:

* Quy tắc chuyển đổi ngôn ngữ.
* Bảng thuật ngữ song ngữ.
* Component Language Switcher.
* Kiểm tra độ dài text.
* Một số màn hình đối chiếu Việt–Anh.
* Quy tắc ngày giờ, số và tiền tệ.

16. CÁC MÀN HÌNH ƯU TIÊN

Thiết kế high-fidelity cho:

* Login.
* Manager Dashboard.
* Staff List.
* Member List.
* Member Detail.
* Package List.
* Package Detail/Edit.
* Membership Registration.
* Class List.
* Class Calendar.
* Create/Edit Class.
* Member Class Catalog.
* Class Detail.
* Booking Confirmation.
* Coach Dashboard.
* Coach Schedule.
* Attendance.
* Attendance Correction Modal.
* Training Progress.
* Receptionist Dashboard.
* Payment Entry.
* Payment Detail.
* Revenue Report.
* Audit Log.
* Personal Profile.
* 403.
* 404.

Tạo phiên bản tiếng Việt và tiếng Anh cho ít nhất các màn hình:

* Login.
* Manager Dashboard.
* Member List.
* Member Detail.
* Class Detail.
* Attendance.
* Payment Entry.
* Audit Log.

17. ĐẦU RA YÊU CẦU

Hãy tạo:

1. Information architecture và sitemap của MVP.
2. User flow cho các nghiệp vụ chính.
3. Design foundations.
4. Localization guideline cho Việt–Anh.
5. Component library có variants và states.
6. High-fidelity web screens cho cả bốn vai trò.
7. Desktop/laptop responsive behavior từ 1024px đến 1920px.
8. Clickable prototype cho các luồng chính.
9. Loading, empty, error, validation và permission states.
10. Handoff-ready design với:

* Auto Layout.
* Variables.
* Component properties.
* Reusable components.
* Design tokens.
* Responsive constraints.
* Localization-ready content.
* Naming convention nhất quán.
* Annotation cho business rules.

18. KẾT QUẢ MONG MUỐN

Thiết kế phải tạo cảm giác đây là một sản phẩm SaaS quản lý trung tâm thể thao có thể triển khai thực tế.

Giao diện cần:

* Nhất quán giữa các vai trò.
* Hỗ trợ đầy đủ tiếng Việt và tiếng Anh.
* Cho phép đổi ngôn ngữ mà không làm thay đổi dữ liệu hoặc vị trí hiện tại.
* Hoạt động tốt trên trình duyệt desktop và laptop.
* Giảm số bước trong các tác vụ thường xuyên.
* Thể hiện rõ quyền hạn của từng vai trò.
* Thể hiện rõ trạng thái nghiệp vụ.
* Giải thích rõ nguyên nhân khi hành động bị từ chối.
* Không tự bổ sung tính năng ngoài phạm vi MVP.

Khi một yêu cầu chưa rõ, hãy lựa chọn giải pháp đơn giản nhất phù hợp với MVP và ghi lại giả định trong trang UX Notes and Handoff.
