# Bàn giao FE — bốn chức năng cần sửa

**Dành cho:** FE/Codex. **Cập nhật:** 14/11/2026. **Repo:** [FE_Project](https://github.com/hoanganh21022005/FE_Project).

## 1. Kết quả hiện tại

Lần kiểm đã ghi nhận build thành công và các màn xem lớp, ngày nghỉ, bộ môn/phòng, danh sách hoàn tiền của Manager tải được dữ liệu. Chưa xác nhận website chạy đúng bản repo mới nhất. Bốn chức năng ở mục 4 cần sửa và kiểm lại thao tác lưu.

**Đính chính16/11:** bỏ dự toán theo yêu cầu Owner. Bốn vấn đề dưới đây thuộc lượt rà14/11; cần đối chiếu lại repo/website trước khi kết luận chúng vẫn còn trên bản mới.

## 2. Quá trình và quyết định giữ/bỏ

1. Đã đối chiếu mã FE với website và API để xác định bốn vấn đề bên dưới.
2. Giữ phần xem trước/xác nhận ngày nghỉ đã có trong repo; cần kiểm bản được phát hành lên website.
3. Giới hạn việc sửa ở bốn chức năng bên dưới và dùng lại các form hiện có.

## 3. API liên quan

Dùng `GET /api/auth/me` để xác định người đang đăng nhập và quyền của họ. Các trường dữ liệu và mã lỗi xem [hợp đồng API](docs/01-API-CONTRACT-FE-BE.md); bằng chứng kiểm ở [Active/QA](docs/00-MASTER-CHECKLIST.md#active).

## 4. Các thay đổi cần thực hiện

### Điểm danh một hoặc nhiều học viên

Sửa `src/api/attendanceApi.js` và `src/components/views/CoachAttendanceView.jsx`: với `POST /api/training-results` hoặc `POST /api/training-results/bulk`, bỏ trường `content` hoặc gửi null khi chỉ điểm danh. Hiện FE tự điền nhận xét như “Có mặt đầy đủ theo điểm danh lớp”, có thể ghi đè nhận xét đã lưu.

**Kiểm lại:** dùng đúng Coach được phân công và quyền `RECORD_RESULT`, trong khoảng từ lúc bắt đầu đến trước lúc kết thúc buổi. Thử cả đơn và bulk, rồi đọc lại: trạng thái điểm danh đúng và nhận xét đã lưu trước đó vẫn giữ nguyên.

### Sửa hoặc xóa nhận xét sau buổi học

Dùng lại form trong `src/components/views/CoachTrainingProgress.jsx`; thêm hàm gọi API vào `src/api/memberPortalApi.js`. Lấy `resultId` từ kết quả đã tải của đúng học viên và buổi học, rồi gửi:

`PATCH /api/training-results/{resultId}/content` với `{"content": value}`.

Gửi nguyên nội dung người dùng nhập; chuỗi rỗng `""` nghĩa là xóa nhận xét. Nếu chưa có bản ghi kết quả, báo “Chưa có kết quả để sửa”; không gửi POST tạo kết quả mới sau buổi.

Coach phải là người được phân công buổi học, đồng thời là người đã tạo kết quả và có quyền `RECORD_RESULT`. Thời gian được sửa: từ khi buổi học kết thúc đến trước 00:00 hôm sau, giờ Việt Nam. Việc ghi kết quả trong buổi vẫn dùng POST như hiện tại.

**Kiểm lại:** sửa hoặc xóa nhận xét rồi tải lại trang; nội dung được lưu đúng, trạng thái điểm danh không đổi. Sai Coach hoặc quá hạn phải bị từ chối, dữ liệu giữ nguyên.

### Thu tiền mặt khi Reception đăng ký lớp Normal

Sửa nhánh Reception của `bookClass` trong `src/context/SCMSContext.jsx` và `src/api/classEnrollmentApi.js`: gửi thêm `paymentMethod:"Cash"` vào `POST /api/classes/{classId}/enroll` khi thu tiền mặt. Request hiện thiếu trường này, nên rơi vào nhánh mặc định VNPay.

Chỉ báo hoàn tất khi hóa đơn là `Paid` và ghi danh là `Registered`; không gọi xác nhận Cash lần hai. Không thu tiền mặt cho danh sách chờ. Các thao tác thanh toán VNPay giữ phương thức VNPay.

**Kiểm lại:** Reception có quyền đăng ký/thu Cash, đúng trung tâm, chọn lớp còn hạn nhận đăng ký và học viên đủ điều kiện. Tải lại trang vẫn thấy hóa đơn đã thanh toán và học viên đã ghi danh; thử lại không tạo thêm hóa đơn. Nhánh VNPay vẫn gửi VNPay.

### Reception tạo tài khoản hội viên

Sửa `addMember` trong `src/context/SCMSContext.jsx` để chọn hàm trong `src/api/memberApi.js` theo người đăng nhập:

- **Reception:** dùng `registerMemberProfile` → `POST /api/members/register`.
- **Manager:** dùng `createMember` → `POST /api/members`.

Hiện cả hai đều gọi `/api/members`, khiến Reception bị 403 dù có quyền đăng ký hội viên.

**Kiểm lại:** Reception có `REGISTER_MEMBER` tạo được hội viên, tải lại danh sách vẫn thấy người vừa tạo. Manager dùng đúng route quản lý và tạo/đọc lại được trong ca kiểm hợp lệ; thiếu quyền phải bị từ chối.

## 5. Việc còn lại

Chưa xác nhận việc lưu thành công của bốn chức năng trên website. Trước khi kiểm, đối chiếu lại repo và bản website đang chạy, xác nhận phiên bằng `/api/auth/me`, rồi kiểm theo từng dòng **Kiểm lại** ở mục 4. Build, mock hoặc GET thành công chưa đủ để đóng việc.

FE/Codex bàn giao lại: **file đã sửa, kết quả kiểm từng chức năng và thời gian thực tế**.
