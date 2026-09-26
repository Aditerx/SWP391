Hãy tiếp tục chỉnh sửa dự án SCMS_demo hiện tại. Bổ sung hai phần còn thiếu:

1. Trang Member Detail khi nhấn “Chi tiết”.
2. Form “Thêm nhân viên”.

Đây là prototype tương tác phục vụ demo đầu tiên: Center Manager Login và User/Membership Management. Không xây dựng lại ứng dụng, không thay đổi các màn hình ngoài phạm vi cần thiết.

A. NGUYÊN TẮC CHUNG

* Giữ nguyên sidebar, header, breadcrumb, font, màu navy, nền sáng, border mảnh và phong cách bảng dữ liệu đang có.
* Tái sử dụng component, dữ liệu mẫu và cơ chế chuyển ngôn ngữ hiện tại.
* Giao diện web desktop/laptop, từ 1024px đến 1920px. Không thiết kế mobile.
* Hỗ trợ tiếng Việt và tiếng Anh cho toàn bộ nội dung mới, gồm label, validation, toast, confirmation và trạng thái.
* Chỉ hiển thị một ngôn ngữ tại một thời điểm.
* Sử dụng React JavaScript và component .jsx phù hợp với source hiện tại.
* Không thêm thư viện hoặc thay đổi kiến trúc nếu không cần thiết.
* Đây là prototype dùng dữ liệu giả và state dùng chung, không kết nối backend, không gửi email hoặc thực hiện thanh toán thật.
* Không biến role phía frontend thành cơ chế bảo mật thật.
* Không thêm gradient, illustration trang trí, biểu đồ không cần thiết hoặc animation gây phân tâm.
* Không tạo button không có hành vi. Với chức năng ngoài phạm vi, ẩn hoặc vô hiệu hóa kèm giải thích.

B. MEMBER DETAIL — CHI TIẾT THÀNH VIÊN

1. Điều hướng

* Nút “Chi tiết” trên danh sách Thành viên mở trang chi tiết đúng người được chọn trong application shell hiện tại.
* Tất cả dòng phải có hành động “Chi tiết”, kể cả người có gói chờ thanh toán, trong thời gian ân hạn hoặc hết hạn.
* Giữ “Thanh toán” là hành động riêng nếu đã tồn tại; không thay thế nút “Chi tiết”.
* Breadcrumb: SCMS Center > Thành viên > Tên thành viên.
* Có nút “Quay lại danh sách”.
* Khi quay lại, giữ bộ lọc, từ khóa tìm kiếm và trang hiện tại.
* Không luôn hiển thị cùng một hồ sơ hard-code cho mọi thành viên.

2. Phần đầu trang

Hiển thị:

* Avatar bằng chữ cái đầu.
* Họ tên.
* Mã thành viên.
* Email và số điện thoại.
* Trạng thái tài khoản với label rõ ràng.
* Nút “Chỉnh sửa hồ sơ”.

Phân biệt hai khái niệm:

* Trạng thái tài khoản: Hoạt động / Tạm khóa.
* Trạng thái gói đăng ký: Chờ thanh toán / Đang hiệu lực / Trong thời gian ân hạn / Hết hạn / Đã hủy.

Một tài khoản có thể Hoạt động nhưng gói đang Chờ thanh toán. Không tự khóa tài khoản chỉ vì gói hết hạn.

3. Nội dung trang

Chỉ tạo hai tab:

* Thông tin cá nhân.
* Gói đăng ký.

Không thêm tab lớp học, tiến độ tập luyện, thanh toán hoặc AI trong lần bổ sung này.

Tab “Thông tin cá nhân”:

* Họ tên.
* Email.
* Số điện thoại.
* Ngày sinh.
* Địa chỉ.
* Ngày tham gia.
* Coach phụ trách, chỉ đọc trong phạm vi này.

Dữ liệu chưa có hiển thị “Chưa cập nhật” hoặc “Chưa phân công”. Không tự bịa thông tin cá nhân để lấp đầy.

“Chỉnh sửa hồ sơ” mở modal:

* Họ tên, email, số điện thoại bắt buộc.
* Ngày sinh và địa chỉ không bắt buộc.
* Không cho sửa mã thành viên, trạng thái Membership hoặc Coach tại form này.
* Validate email, thông tin bắt buộc, ngày sinh không ở tương lai.
* Kiểm tra email trùng trong dữ liệu mẫu, loại trừ chính tài khoản đang chỉnh sửa.
* Lưu thành công cập nhật đồng thời trang chi tiết và danh sách Thành viên.
* Hiển thị toast thành công.
* Hủy không lưu thay đổi; nếu form đã thay đổi, hỏi xác nhận trước khi đóng.

Tab “Gói đăng ký”:

* Tên gói.
* Mã Membership.
* Trạng thái gói.
* Giá đăng ký bằng VND.
* Thời hạn gói.
* Ngày bắt đầu hiệu lực và ngày hết hạn hợp đồng nếu đã xác định.
* Thời điểm kết thúc ân hạn khi áp dụng.
* Quyền lợi của gói, gồm quyền lợi kế thừa.
* Lịch sử các lần đăng ký gói, phân biệt rõ từng Membership.

Không sử dụng giá gói mới để âm thầm thay đổi giá của Membership đã đăng ký trước đó.

4. Cách thể hiện trạng thái Membership

PENDING_PAYMENT:

* Nhãn “Chờ thanh toán”.
* Thông báo: “Gói đăng ký chưa có hiệu lực. Quyền sử dụng dịch vụ chỉ được kích hoạt sau khi thanh toán thành công.”
* Nếu ngày hiệu lực chưa được xác định, hiển thị “Chưa kích hoạt”; không bịa ngày hết hạn.
* Không có button “Kích hoạt ngay” hoặc chức năng đổi trực tiếp sang ACTIVE.

ACTIVE:

* Nhãn “Đang hiệu lực”.
* Hiển thị ngày hiệu lực, ngày hết hạn và quyền lợi.

GRACE:

* Nhãn “Trong thời gian ân hạn”.
* Hiển thị cả thời điểm hết hạn hợp đồng và thời điểm kết thúc ân hạn.
* Kết thúc ân hạn bằng hết hạn hợp đồng cộng đúng 72 giờ.
* Không gọi ân hạn là đã gia hạn hợp đồng và không sửa ngày hết hạn gốc.

EXPIRED:

* Nhãn “Hết hạn”.
* Thông báo quyền sử dụng dịch vụ đã kết thúc.

CANCELLED:

* Nhãn “Đã hủy”.
* Chỉ hiển thị trạng thái nếu có dữ liệu mẫu; không bổ sung flow hoàn tiền hoặc hủy hợp đồng.

Không có Membership:

* Empty state: “Thành viên chưa đăng ký gói”.
* Nút “Đăng ký gói”.

5. Đăng ký gói từ Member Detail

Cho phép đăng ký gói khi thành viên chưa có Membership. Với trường hợp đã có gói đang hiệu lực hoặc có yêu cầu chờ thanh toán, không tạo thêm gói trùng; không tự đưa ra chính sách chồng gói hoặc gia hạn mới.

Modal đăng ký gói gồm:

* Tên thành viên, chỉ đọc.
* Dropdown “Chọn gói…”, không chọn sẵn một gói.
* Chỉ chọn được gói đang bán.
* Sau khi chọn, hiển thị giá, thời hạn và quyền lợi.
* Nút “Hủy” và “Tạo đăng ký gói”.
* Thông báo rõ gói mới sẽ có trạng thái Chờ thanh toán.

Khi xác nhận:

* Tạo Membership mẫu với trạng thái PENDING_PAYMENT.
* Không tạo Payment thành công.
* Cập nhật cả trang chi tiết và danh sách Thành viên.
* Giữ ở tab Gói đăng ký và hiển thị kết quả vừa tạo.
* Chặn nhấn submit nhiều lần trong lúc xử lý.

C. FORM “THÊM NHÂN VIÊN”

1. Điểm mở và bố cục

* Nối nút “Thêm nhân viên” đang có trên trang Nhân viên với modal mới.
* Tiêu đề “Thêm nhân viên”.
* Mô tả ngắn: “Tạo tài khoản cho huấn luyện viên hoặc nhân viên lễ tân”.
* Chiều rộng khoảng 640–720px, phù hợp diện tích thực tế.
* Với chiều cao màn hình thấp, nội dung modal cuộn bên trong và footer luôn dễ tiếp cận.
* Không làm wizard nhiều bước.

2. Trường nhập

Thông tin cơ bản:

* Họ và tên, bắt buộc.
* Email đăng nhập, bắt buộc.
* Số điện thoại, bắt buộc.
* Vai trò, bắt buộc, mặc định “Chọn vai trò…”.
* Chỉ có hai lựa chọn: Huấn luyện viên và Lễ tân.

Không cho tạo Center Manager hoặc Member từ form này.

Nếu chọn Huấn luyện viên:

* Hiện trường “Chuyên môn”, bắt buộc.
* Gợi ý ví dụ Yoga, Pilates, Boxing hoặc Fitness.
* Không bắt buộc phân công lớp hay thành viên ở bước tạo tài khoản.

Nếu chọn Lễ tân:

* Ẩn trường chuyên môn.
* Không gửi hoặc giữ dữ liệu chuyên môn của lựa chọn Coach trước đó.

Thông tin tài khoản:

* Mật khẩu tạm thời.
* Xác nhận mật khẩu.
* Show/hide password với accessible label.
* Ghi rõ đây là dữ liệu giả phục vụ prototype, không nhập mật khẩu thật.
* Mật khẩu không hiển thị trong danh sách, trang chi tiết, toast hoặc audit log.
* Không lưu mật khẩu vào localStorage, sessionStorage hoặc console.
* Xóa giá trị mật khẩu khỏi form sau khi hoàn tất hoặc đóng.

Quy tắc mật khẩu prototype tạm dùng tối thiểu 8 ký tự, phải khớp xác nhận. Tách rule để nhóm có thể thay bằng chính sách backend sau này. Không mô tả đây là cơ chế bảo mật production.

Trạng thái khởi tạo:

* Hoạt động, chỉ đọc.
* Không thêm tùy chọn cấp quyền quản trị.

3. Validation và tương tác

* Trim khoảng trắng tên và email.
* Không chấp nhận họ tên chỉ chứa khoảng trắng.
* Email đúng định dạng và không trùng tài khoản trong dữ liệu mẫu.
* Số điện thoại lưu dưới dạng chuỗi, giữ số 0 đầu; cho phép định dạng +84 hoặc số nội địa Việt Nam.
* Lỗi hiển thị ngay dưới field và không chỉ dùng màu đỏ.
* Khi submit lỗi, đưa focus đến field lỗi đầu tiên.
* Không xóa dữ liệu đã nhập khi có lỗi.
* Vai trò chưa chọn hoặc Coach thiếu chuyên môn phải bị chặn.
* Hiển thị “Đang tạo…” và disable submit trong thời gian mô phỏng xử lý.

Lưu thành công:

* Thêm đúng một nhân viên vào state dùng chung.
* Sinh mã duy nhất theo quy ước dữ liệu mẫu hiện tại.
* Cập nhật danh sách và tổng số bản ghi.
* Hiển thị bản ghi mới hoặc chỉ rõ nếu đang bị bộ lọc hiện tại che khuất.
* Toast: “Đã tạo tài khoản nhân viên”.
* Đóng modal.
* Không giả vờ đã gửi email mời.

Hủy:

* Không tạo tài khoản.
* Nếu đã nhập dữ liệu, hỏi xác nhận bỏ thay đổi.
* Trả focus về nút “Thêm nhân viên”.

D. TRẠNG THÁI VÀ KHẢ NĂNG SỬ DỤNG

Bổ sung cho hai phần mới:

* Loading.
* Empty state.
* Không tìm thấy thành viên.
* Lỗi tải dữ liệu kèm “Thử lại”.
* Lỗi validation.
* Thành công sau khi lưu.
* Confirmation bỏ thay đổi.

Modal cần:

* Nhãn trường rõ ràng.
* Điều hướng bằng bàn phím.
* Focus nằm trong modal khi mở và trở về nút kích hoạt khi đóng.
* Escape tuân theo quy tắc xác nhận bỏ thay đổi.
* Có nút đóng được đặt tên cho trình đọc màn hình.

Không hiển thị lỗi đỏ trong form chưa được tương tác.

E. PHẠM VI KHÔNG ĐƯỢC MỞ RỘNG

* Không sửa toàn bộ Dashboard.
* Không làm lại bảng giá hoặc hệ thống màu.
* Không tạo booking, attendance, payment gateway, refund, AI hoặc thông báo email.
* Không sửa đăng nhập nhanh theo vai trò trong nhiệm vụ này.
* Không tự kích hoạt Membership sau khi tạo hồ sơ.
* Không biến mọi thành viên thành ACTIVE.
* Không thay dữ liệu mẫu hiện có bằng bộ dữ liệu hoàn toàn mới.

F. TIÊU CHÍ HOÀN THÀNH

Kiểm tra các kịch bản:

1. Nhấn Chi tiết ở từng dòng mở đúng thành viên.
2. Thành viên chờ thanh toán vẫn xem được hồ sơ.
3. Trạng thái tài khoản và trạng thái gói có label riêng.
4. Sửa hồ sơ cập nhật đúng bản ghi ở cả detail và list.
5. Đăng ký gói cho người chưa có gói tạo PENDING_PAYMENT, không ACTIVE.
6. Chọn Coach hiển thị Chuyên môn; chọn Receptionist ẩn trường này.
7. Email trùng và mật khẩu không khớp không tạo nhân viên.
8. Submit nhiều lần không tạo bản ghi trùng.
9. Hủy không làm thay đổi dữ liệu.
10. Đổi Việt/Anh cập nhật toàn bộ nội dung mới, không làm mất dữ liệu form.
11. Layout sử dụng được tại 1024px, 1366px và 1920px.

Hãy triển khai trực tiếp vào prototype hiện tại. Khi hoàn tất, liệt kê ngắn các component đã thêm/sửa, các tương tác đã nối và những phần vẫn chỉ là mô phỏng frontend.
