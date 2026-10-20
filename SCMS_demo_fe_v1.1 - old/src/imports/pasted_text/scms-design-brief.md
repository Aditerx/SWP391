Bạn là Senior Product Designer, UX Architect và Design System Specialist. Hãy thiết kế UI/UX high-fidelity cho hệ thống web:

SPORTS CENTER MANAGEMENT SYSTEM – SCMS
HỆ THỐNG QUẢN LÝ TRUNG TÂM THỂ THAO

Thiết kế phải có cảm giác như một sản phẩm SaaS vận hành thực tế, được xây dựng từ nhu cầu nghiệp vụ của người dùng. Không tạo giao diện mang phong cách dashboard template phổ biến hoặc giao diện có dấu hiệu được AI tạo tự động.

I. MỤC TIÊU SẢN PHẨM

SCMS giúp một trung tâm thể thao quản lý:

* Nhân viên và thành viên.
* Gói thành viên và quyền lợi.
* Trạng thái hội viên.
* Lớp học, lịch học, phòng tập và huấn luyện viên.
* Đăng ký và hủy đăng ký lớp.
* Điểm danh học viên.
* Kế hoạch và kết quả tập luyện.
* Thanh toán gói thành viên.
* Báo cáo vận hành và doanh thu.
* Nhật ký các thao tác quan trọng.

MVP chỉ phục vụ một trung tâm thể thao. Không thiết kế quản lý nhiều chi nhánh.

II. NỀN TẢNG VÀ NGÔN NGỮ

1. Nền tảng

Chỉ thiết kế giao diện web dành cho desktop và laptop.

Kích thước frame:

* Desktop chính: 1440 × 1024 px.
* Laptop: 1280 × 800 px.
* Large desktop: 1920 × 1080 px.
* Chiều rộng hỗ trợ tối thiểu: 1024 px.

Không thiết kế:

* Mobile application.
* Mobile responsive layout.
* Tablet application.
* Bottom navigation.
* Native iOS hoặc Android.
* Layout riêng cho điện thoại.

Sử dụng Auto Layout và responsive constraints để giao diện hoạt động tốt trong phạm vi 1024–1920px.

2. Song ngữ

Hệ thống hỗ trợ:

* Tiếng Việt – Vietnamese.
* Tiếng Anh – English.

Yêu cầu:

* Có Language Switcher “VI / EN” trên top bar.
* Mỗi thời điểm chỉ hiển thị một ngôn ngữ.
* Không hiển thị nhãn dạng “Thành viên / Members” trên giao diện thật.
* Ngôn ngữ được giữ nguyên khi chuyển trang hoặc đăng nhập lại.
* Đổi ngôn ngữ không làm mất dữ liệu, bộ lọc hoặc trang hiện tại.
* Component chứa text phải dùng Auto Layout.
* Button, tab, menu, table header và badge không bị cắt khi đổi ngôn ngữ.
* Không cố định chiều rộng nút theo nội dung tiếng Anh.
* Font phải hỗ trợ đầy đủ dấu tiếng Việt.
* Chuẩn bị content variables hoặc localization tokens cho hai ngôn ngữ.
* Tạo phiên bản VI và EN cho các màn hình quan trọng để kiểm tra layout.

Định dạng:

* Tiếng Việt: DD/MM/YYYY và thời gian 24 giờ.
* Tiếng Anh: DD MMM YYYY.
* Tiền tệ: VND.
* Số tiền có dấu phân cách hàng nghìn.

III. VAI TRÒ NGƯỜI DÙNG

1. Center Manager – Quản lý trung tâm

* Quản lý tài khoản nhân viên.
* Quản lý thành viên.
* Quản lý gói thành viên và quyền lợi.
* Quản lý lớp học, lịch học và phòng tập.
* Phân công huấn luyện viên.
* Xem thanh toán và doanh thu.
* Xem báo cáo vận hành.
* Xem Audit Log.
* Truy cập toàn bộ dữ liệu của trung tâm.

2. Coach – Huấn luyện viên

* Xem lịch dạy.
* Xem các lớp được phân công.
* Xem danh sách học viên.
* Điểm danh học viên.
* Chỉnh sửa điểm danh và nhập lý do.
* Xem thành viên được phân công.
* Tạo kế hoạch tập luyện.
* Ghi kết quả và nhận xét.
* Không truy cập nghiệp vụ quản lý thanh toán.

3. Receptionist – Nhân viên lễ tân

* Tìm kiếm và quản lý hồ sơ thành viên.
* Tạo thành viên mới.
* Đăng ký gói thành viên.
* Đăng ký hoặc hủy lớp thay cho thành viên.
* Ghi nhận thanh toán thủ công hoặc mô phỏng.
* Xem trạng thái membership.
* Không được bỏ qua điều kiện kích hoạt membership.
* Không được chỉnh sửa âm thầm giao dịch đã hoàn tất.

4. Member – Thành viên

* Xem hồ sơ cá nhân.
* Xem gói và trạng thái membership.
* Tìm kiếm và đăng ký lớp.
* Hủy đăng ký lớp.
* Xem lịch học sắp tới.
* Xem lịch sử đăng ký và điểm danh.
* Xem kế hoạch và kết quả tập luyện.
* Chỉ truy cập dữ liệu của mình.
* Không chỉnh sửa dữ liệu do Coach tạo.

Tất cả vai trò đều sử dụng giao diện web.

IV. PHẠM VI MVP

1. Authentication and Authorization

Thiết kế:

* Login.
* Logout.
* Forgot Password placeholder.
* Role-based navigation.
* Session expired state.
* 403 Unauthorized.
* 404 Not Found.

Sau khi đăng nhập, người dùng được đưa đến dashboard phù hợp với vai trò.

Thông báo đăng nhập không được tiết lộ email hay mật khẩu nào sai.

2. Staff and Member Management

Thiết kế:

* Staff List.
* Create Staff.
* Edit Staff.
* Suspend/Reactivate Staff.
* Member List.
* Create Member.
* Edit Member.
* Member Detail.

Member Detail gồm:

* Thông tin cá nhân.
* Membership hiện tại.
* Lịch sử membership.
* Coach chính.
* Lớp đã đăng ký.
* Lịch sử điểm danh.
* Tiến độ tập luyện.
* Lịch sử thanh toán.

3. Membership Packages and Memberships

Thiết kế:

* Package List.
* Package Detail.
* Create/Edit Package.
* Membership List.
* Membership Registration.
* Membership Detail.
* Membership Status Timeline.

Thông tin gói:

* Tên gói.
* Giá.
* Thời hạn.
* Mô tả.
* Quyền lợi.
* Giới hạn số lớp.
* Trạng thái.

Trạng thái membership:

* Pending Payment.
* Active.
* Grace Period.
* Expired.
* Cancelled.

Business rules:

* Gói có giá cao hơn phải bao gồm quyền lợi của gói thấp hơn.
* Membership chỉ được kích hoạt sau thanh toán Successful.
* Pending hoặc Failed payment không kích hoạt membership.
* Grace Period kéo dài đúng 72 giờ sau thời điểm hết hạn.
* Hết Grace Period thì quyền sử dụng dịch vụ bị khóa.

4. Class Management

Thiết kế:

* Class List.
* Class Calendar.
* Class Detail.
* Create Class.
* Edit Class.
* Class Session Detail.

Thông tin lớp:

* Tên lớp.
* Bộ môn.
* Coach.
* Phòng tập.
* Ngày và giờ.
* Thời lượng.
* Sức chứa.
* Trạng thái.

Hệ thống phải cảnh báo:

* Trùng lịch Coach.
* Trùng lịch phòng.
* Thiếu dữ liệu bắt buộc.
* Sức chứa không hợp lệ.

Mỗi class session là một buổi học có ngày giờ cụ thể. Lịch lặp lại không phải luồng chính của MVP.

5. Class Booking

Thiết kế:

* Class Catalog.
* Search and Filter.
* Class Detail.
* Booking Confirmation.
* My Bookings.
* Cancel Booking.

Bộ lọc:

* Bộ môn.
* Ngày.
* Khung giờ.
* Coach.
* Còn chỗ.
* Trạng thái.

Không cho phép đăng ký khi:

* Membership đang Pending Payment.
* Membership đã Expired.
* Membership đã Cancelled.
* Lớp đã đủ chỗ.
* Thành viên đã đăng ký lớp.
* Gói không có quyền tham gia lớp.

Mỗi trường hợp phải hiển thị nguyên nhân cụ thể và hướng xử lý tiếp theo.

Không thiết kế waitlist trong MVP.

6. Attendance

Thiết kế:

* Coach Schedule.
* Class Roster.
* Attendance Sheet.
* Attendance Correction Modal.
* Member Attendance History.

Trạng thái:

* Present.
* Late.
* Absent.
* Not Recorded.

Khi sửa điểm danh:

* Hiển thị giá trị cũ.
* Hiển thị giá trị mới.
* Bắt buộc nhập lý do.
* Cảnh báo thay đổi sẽ được ghi vào Audit Log.
* Sau khi lưu, cập nhật dữ liệu ngay lập tức.

Không thiết kế facility check-in trong MVP.

7. Training Progress

Thiết kế:

* Assigned Members.
* Member Training Detail.
* Training Plan.
* Add Training Result.
* Progress Timeline.
* Coach Comments.

Coach có thể:

* Tạo mục tiêu.
* Tạo kế hoạch.
* Ghi nội dung bài tập.
* Ghi kết quả.
* Thêm nhận xét.

Member chỉ được xem.

Mỗi Member chỉ có một Coach chính đang hoạt động tại một thời điểm.

Không thiết kế tư vấn hoặc chẩn đoán y tế.

8. Payments

Thiết kế:

* Payment List.
* Create Payment.
* Payment Detail.
* Transaction History.
* Payment Status Report.

Thông tin thanh toán:

* Thành viên.
* Người thanh toán.
* Membership.
* Số tiền.
* Phương thức.
* Thời điểm thanh toán.
* Trạng thái.
* Ghi chú.

Trạng thái:

* Successful.
* Pending.
* Failed.
* Reversed.

Chỉ Successful payment mới kích hoạt membership.

Không thiết kế:

* Cổng thanh toán thật.
* Member refund request.
* Quy trình phê duyệt hoàn tiền.

9. Manager Dashboard and Reports

KPI:

* Tổng số thành viên.
* Thành viên đang hoạt động.
* Membership chờ thanh toán.
* Membership trong Grace Period.
* Số lớp hôm nay.
* Tổng lượt đăng ký.
* Tỷ lệ điểm danh.
* Doanh thu thành công.
* Số giao dịch Pending.
* Số giao dịch Failed.

Nội dung dashboard:

* Bộ lọc thời gian.
* Revenue trend.
* Membership trend.
* Booking and attendance summary.
* Upcoming classes.
* Memberships requiring attention.
* Payment status breakdown.
* Recent sensitive activities.

Quy tắc doanh thu:

* Chỉ cộng Successful payments.
* Lọc theo thời điểm giao dịch thành công.
* Pending, Failed và Reversed hiển thị riêng.
* Không cộng các trạng thái này vào doanh thu.

10. Audit Log

Thiết kế bảng Audit Log gồm:

* Thời gian.
* Người thực hiện.
* Vai trò.
* Hành động.
* Module.
* Đối tượng bị tác động.
* Giá trị trước.
* Giá trị sau.
* Lý do.

Bộ lọc:

* Người dùng.
* Vai trò.
* Hành động.
* Module.
* Khoảng thời gian.

Các hành động bắt buộc ghi log:

* Thay đổi vai trò.
* Khóa hoặc kích hoạt tài khoản.
* Ghi nhận hoặc điều chỉnh thanh toán.
* Chỉnh sửa điểm danh.

Audit Log chỉ đọc và không có chức năng chỉnh sửa hoặc xóa.

V. INFORMATION ARCHITECTURE

Public:

* Login.
* Forgot Password.
* 403.
* 404.

Manager:

* Dashboard.
* Staff.
* Members.
* Membership Packages.
* Memberships.
* Classes.
* Schedule.
* Payments.
* Reports.
* Audit Logs.
* Profile.

Coach:

* Dashboard.
* Teaching Schedule.
* Classes.
* Attendance.
* Assigned Members.
* Training Plans.
* Training Progress.
* Profile.

Receptionist:

* Dashboard.
* Members.
* Membership Registration.
* Class Booking.
* Payments.
* Payment History.
* Profile.

Member:

* Dashboard.
* My Membership.
* Class Catalog.
* My Bookings.
* Attendance History.
* Training Plan.
* Training Progress.
* Profile.

VI. CÁC USER FLOW CẦN PROTOTYPE

1. Login → Xác thực → Xác định vai trò → Role Dashboard.

2. Receptionist tạo Member → Chọn Package → Tạo Pending Membership → Ghi nhận Successful Payment → Membership chuyển sang Active.

3. Manager tạo Class → Chọn Coach và phòng → Chọn lịch → Kiểm tra xung đột → Publish.

4. Member tìm Class → Xem chi tiết → Hệ thống kiểm tra điều kiện → Xác nhận đăng ký → Hiển thị kết quả.

5. Coach mở Class Session → Xem Roster → Điểm danh → Lưu.

6. Coach sửa Attendance → Xem giá trị cũ → Chọn giá trị mới → Nhập lý do → Xác nhận → Audit Log được tạo.

7. Coach chọn Member → Tạo Training Plan hoặc Result → Lưu → Member xem trên Progress Timeline.

8. Manager chọn khoảng thời gian → Xem Reports → Drill down đến dữ liệu chi tiết.

9. Người dùng chuyển VI/EN → Giữ nguyên trang, bộ lọc và dữ liệu → Nội dung giao diện đổi ngôn ngữ.

VII. NGUYÊN TẮC THIẾT KẾ KHÔNG MANG PHONG CÁCH AI

1. Thiết kế từ nghiệp vụ

* Mỗi màn hình phải phản ánh công việc thực tế của vai trò.
* Không sử dụng cùng một dashboard template cho mọi vai trò.
* Không thêm section chỉ để lấp đầy giao diện.
* Mỗi thành phần phải hỗ trợ một quyết định hoặc hành động cụ thể.

2. Không card hóa mọi nội dung

* Card chỉ dùng cho KPI hoặc nhóm thông tin độc lập.
* Danh sách vận hành phải dùng data table.
* Hồ sơ chi tiết dùng section, tab, summary list hoặc timeline.
* Không bọc card bên trong card.
* Không tạo hàng loạt card giống nhau gồm icon, title, subtitle và button.

3. Không thiết kế dashboard như landing page

Không sử dụng:

* Hero banner lớn.
* Marketing slogan.
* Gradient headline.
* Nội dung quảng cáo.
* Illustration trang trí không phục vụ nghiệp vụ.
* Khoảng trắng lớn nhưng thiếu thông tin vận hành.

Dashboard phải trả lời:

* Hôm nay có việc gì?
* Có vấn đề nào cần xử lý?
* Chỉ số nào thay đổi?
* Hành động tiếp theo là gì?

4. Hạn chế hiệu ứng trang trí

Không sử dụng:

* Gradient tím–xanh.
* Neon glow.
* Glassmorphism.
* Blur nền không cần thiết.
* Shadow lớn trên mọi card.
* Icon nằm trong các ô màu ngẫu nhiên.
* Background pattern mang tính trang trí.
* Rounded rectangle cho mọi thành phần.

Ưu tiên:

* Nền trung tính.
* Border nhẹ.
* Khoảng cách rõ ràng.
* Typography tốt.
* Shadow chỉ dành cho modal, dropdown, popover và overlay.

5. Bo góc có kiểm soát

* Button và input: 6–8px.
* Card và modal: 8–12px.
* Badge: 4–6px hoặc pill khi phù hợp.
* Không sử dụng radius quá lớn cho table, form và container chính.
* Không bo tròn mọi thành phần giống nhau.

6. Mỗi màn hình có một primary action

Ví dụ:

* Member List: Thêm thành viên.
* Class List: Tạo lớp học.
* Payment List: Ghi nhận thanh toán.
* Attendance: Lưu điểm danh.

Không đặt nhiều nút primary có cùng trọng lượng trong một khu vực.

7. Không ép layout đối xứng

* Kích thước khu vực phụ thuộc vào độ quan trọng của nội dung.
* Không bắt buộc ba hoặc bốn card có chiều rộng bằng nhau.
* Chart chính có thể rộng hơn danh sách cảnh báo.
* Bảng dữ liệu có thể chiếm toàn bộ chiều rộng.
* Không kéo dài nội dung để lấp đầy màn hình 1920px.

8. Typography tự nhiên

* Dùng một font family, ưu tiên Inter.
* Không sử dụng quá nhiều font size và font weight.
* Không dùng gradient text.
* Không viết hoa toàn bộ nhãn dài.
* Không dùng headline quá lớn trong màn hình quản trị.
* Tiêu đề phải mô tả nội dung, không dùng khẩu hiệu.

9. Màu sắc có ý nghĩa

* Primary color chỉ dành cho navigation và hành động chính.
* Green dành cho thành công hoặc Active.
* Amber dành cho cảnh báo hoặc Grace Period.
* Red dành cho lỗi, Expired hoặc destructive action.
* Gray dành cho nội dung trung tính.
* Không dùng màu semantic để trang trí.
* Không chỉ dùng màu để phân biệt trạng thái; phải có text hoặc icon.

10. Dữ liệu mẫu thực tế

Không dùng:

* John Doe.
* User 01.
* Class A.
* Lorem ipsum.
* Biểu đồ luôn tăng đều.
* Số liệu quá đẹp hoặc không có ý nghĩa.

Sử dụng dữ liệu nhất quán như:

* Nguyễn Minh Anh.
* Trần Quốc Huy.
* Yoga Foundation.
* Boxing Intermediate.
* Strength Training.
* Studio A.
* Gói Premium 12 tháng.
* 1.500.000 VND.
* Membership đang Grace Period.
* Lớp chỉ còn 2 chỗ.
* Giao dịch Pending hoặc Failed.
* Attendance correction có lý do thực tế.

Ở phiên bản tiếng Anh, giữ nguyên tên riêng nhưng dịch tên trạng thái và nội dung giao diện.

11. Microcopy trực tiếp

Không dùng:

* Something went wrong.
* Invalid input.
* Operation successful.
* Oops!
* Unlock your potential.
* Manage smarter.
* Seamless experience.

Sử dụng thông báo gắn với nghiệp vụ:

* “Không thể đăng ký vì gói thành viên đang chờ thanh toán.”
* “Lớp Boxing Intermediate đã đủ 20 thành viên.”
* “Điểm danh đã được cập nhật và ghi vào nhật ký hệ thống.”
* “Giao dịch đang xử lý nên membership chưa được kích hoạt.”

Mỗi thông báo lỗi phải nói rõ:

* Điều gì xảy ra.
* Nguyên nhân.
* Người dùng có thể làm gì tiếp theo.

12. Thiết kế đầy đủ trạng thái thực tế

Mỗi màn hình quan trọng cần có:

* Default.
* Loading.
* Empty.
* No search results.
* Validation error.
* Server error.
* Unauthorized.
* Disabled action.
* Partial data.
* Long content.
* Large data set.
* Successful action.
* Failed business-rule validation.

13. Confirmation có ngữ cảnh

Không dùng dialog chỉ có nội dung “Are you sure?”.

Ví dụ:

Title:
“Tạm khóa tài khoản Trần Quốc Huy?”

Description:
“Nhân viên sẽ không thể đăng nhập cho đến khi tài khoản được kích hoạt lại.”

Buttons:

* “Hủy”.
* “Tạm khóa tài khoản”.

14. Nhất quán nhưng không máy móc

* Cùng một hành động phải dùng cùng tên.
* Không dùng Save, Submit, Update và Confirm cho cùng một hành động.
* Component dùng chung phải nhất quán.
* Không bắt mọi trang phải có cùng bố cục nếu workflow khác nhau.
* Cho phép layout khác biệt khi có lý do nghiệp vụ rõ ràng.

VIII. APPLICATION SHELL

Left Sidebar:

* Logo SCMS.
* Navigation theo vai trò.
* Active navigation state.
* Collapsible sidebar.
* Menu không có quyền phải được ẩn.
* Không chỉ hiển thị menu disabled.

Top Bar:

* Breadcrumb hoặc page title.
* Global search khi phù hợp.
* Language Switcher.
* Notification placeholder.
* User avatar.
* Profile menu.
* Logout.

Main Content:

* Page title.
* Supporting description khi cần.
* Primary action.
* Filters.
* Main content.
* Pagination hoặc supporting information.

Không sử dụng hamburger menu làm navigation chính trên desktop.

IX. DESIGN SYSTEM

Visual direction:

* Professional.
* Operational.
* Calm.
* Trustworthy.
* Sport-oriented nhưng không phô trương.
* Data-first.
* Không mang phong cách marketing landing page.

Color:

* Một primary color đậm như navy hoặc deep blue.
* Một secondary accent tiết chế như teal.
* Neutral gray làm nền chính.
* Semantic colors cho success, warning và error.
* Không dùng gradient.
* Không dùng quá nhiều màu accent trên cùng màn hình.

Typography:

* Inter hoặc sans-serif hỗ trợ tiếng Việt.
* Display chỉ dùng cho Cover hoặc Login.
* H1 cho page title.
* H2 cho section.
* Body cho nội dung.
* Label cho form và table.
* Caption cho metadata.

Layout:

* 12-column grid.
* 8px spacing system.
* Sidebar khoảng 240–256px khi mở.
* Main content có max-width phù hợp ở màn hình lớn.
* Bảng dữ liệu được phép dùng toàn bộ chiều rộng.
* Không thu nhỏ font để nhét quá nhiều nội dung.

X. COMPONENT LIBRARY

Tạo component với Auto Layout, variables và variants:

* Button.
* Icon Button.
* Text Input.
* Password Input.
* Textarea.
* Select.
* Search.
* Date Picker.
* Time Picker.
* Checkbox.
* Radio.
* Toggle.
* Tabs.
* Breadcrumb.
* Sidebar.
* Top Bar.
* Language Switcher.
* KPI Card.
* Data Table.
* Pagination.
* Filter Bar.
* Calendar.
* Modal.
* Drawer.
* Confirmation Dialog.
* Toast.
* Inline Alert.
* Error Summary.
* Empty State.
* Loading Skeleton.
* Avatar.
* Dropdown.
* Tooltip.
* Timeline.
* Progress Summary.
* Chart Container.
* Audit History.
* Status Badge.

Component states:

* Default.
* Hover.
* Focus.
* Pressed.
* Selected.
* Disabled.
* Loading.
* Error.
* Read-only.

XI. FORM VÀ TABLE

Form:

* Label luôn hiển thị phía trên input.
* Không dùng placeholder thay thế label.
* Trường bắt buộc có ký hiệu rõ ràng.
* Hint text chỉ xuất hiện khi thực sự hữu ích.
* Validation hiển thị gần trường bị lỗi.
* Không xóa dữ liệu đã nhập khi validation thất bại.
* Form dài chia thành section hợp lý.
* Hiển thị cảnh báo khi rời form chưa lưu.

Data Table:

* Search.
* Filter.
* Sort.
* Pagination.
* Row actions.
* Column visibility khi cần.
* Sticky table header với bảng dài.
* Empty state.
* Loading state.
* Error state.
* Không chuyển thành card list vì không thiết kế mobile.

XII. ACCESSIBILITY

* Tuân thủ WCAG 2.1 AA.
* Contrast tối thiểu 4.5:1 cho body text.
* Focus state nhìn thấy rõ.
* Hỗ trợ keyboard navigation.
* Dialog có focus trap.
* Không chỉ dùng màu để truyền đạt trạng thái.
* Icon-only button phải có tooltip và accessible label.
* Error message phải liên kết với input tương ứng.
* Nội dung VI và EN không bị cắt hoặc chồng lấn.
* Charts phải có legend và giá trị thay thế có thể đọc được.

XIII. CẤU TRÚC FILE FIGMA

Tổ chức file thành:

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
* 11 – Desktop Responsive Tests.
* 12 – Prototype.
* 13 – UX Notes and Handoff.

Page Localization VI-EN gồm:

* Language Switcher.
* Thuật ngữ song ngữ.
* Content variables.
* Quy tắc ngày giờ và tiền tệ.
* Text expansion test.
* Các màn hình đối chiếu VI và EN.

XIV. CÁC MÀN HÌNH ƯU TIÊN

Thiết kế high-fidelity cho:

* Login.
* Manager Dashboard.
* Staff List.
* Member List.
* Member Detail.
* Package List.
* Package Detail.
* Membership Registration.
* Membership Detail.
* Class List.
* Class Calendar.
* Create/Edit Class.
* Class Catalog.
* Class Detail.
* Booking Confirmation.
* Coach Dashboard.
* Coach Schedule.
* Attendance Sheet.
* Attendance Correction Modal.
* Training Plan.
* Training Progress.
* Receptionist Dashboard.
* Payment Entry.
* Payment Detail.
* Revenue Report.
* Audit Log.
* Profile.
* 403.
* 404.

Tạo phiên bản VI và EN cho ít nhất:

* Login.
* Manager Dashboard.
* Member List.
* Member Detail.
* Class Detail.
* Attendance Sheet.
* Payment Entry.
* Audit Log.

XV. NGOÀI PHẠM VI

Không thiết kế:

* Mobile UI.
* Tablet UI.
* Multi-branch management.
* Waitlist.
* Facility check-in.
* Support tickets.
* Notification center hoàn chỉnh.
* Email hoặc push notification.
* Real payment gateway.
* Member refund workflow.
* AI chatbot.
* AI training adviser.
* Medical diagnosis.
* MFA.
* Health application integrations.

XVI. ĐẦU RA YÊU CẦU

Tạo:

1. Information architecture và sitemap.
2. User flows cho các nghiệp vụ chính.
3. Design foundations.
4. Localization guideline VI-EN.
5. Component library.
6. High-fidelity web screens cho bốn vai trò.
7. Layout behavior từ 1024px đến 1920px.
8. Clickable prototype.
9. Loading, empty, error, permission và business-rule states.
10. Handoff-ready design với:

* Auto Layout.
* Variables.
* Component properties.
* Reusable components.
* Design tokens.
* Responsive constraints.
* Localization-ready content.
* Consistent naming.
* Business-rule annotations.

XVII. TIÊU CHÍ ĐÁNH GIÁ CUỐI CÙNG

Trước khi hoàn tất, kiểm tra:

* Nếu bỏ logo, người xem có nhận ra đây là hệ thống quản lý trung tâm thể thao không?
* Dashboard của các vai trò có thực sự khác nhau theo công việc không?
* Có section hoặc card nào không hỗ trợ quyết định hay hành động cụ thể không?
* Có quá nhiều card, shadow, radius hoặc icon trang trí không?
* Có nhiều primary button cạnh tranh nhau không?
* Dữ liệu mẫu có thực tế và nhất quán không?
* Các business rule có được thể hiện trong UI không?
* Các trạng thái lỗi và từ chối có giải thích rõ nguyên nhân không?
* Bản tiếng Việt và tiếng Anh có giữ nguyên cấu trúc không?
* Giao diện có hoạt động tốt ở 1024px, 1280px, 1440px và 1920px không?
* Người dùng bàn phím có thể nhận biết focus không?
* Có nội dung nào giống khẩu hiệu hoặc văn bản chung chung do AI tạo không?

Quy tắc quyết định cuối cùng:

Mỗi thành phần trong giao diện phải trả lời được câu hỏi:

“Thành phần này giúp người dùng SCMS hiểu thông tin, đưa ra quyết định hoặc hoàn thành công việc gì?”

Nếu không có câu trả lời rõ ràng, hãy loại bỏ thành phần đó.
