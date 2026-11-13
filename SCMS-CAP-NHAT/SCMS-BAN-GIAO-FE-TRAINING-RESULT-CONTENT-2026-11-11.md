# SCMS — Bàn giao FE: quyền ghi nhận xét kết quả buổi tập

Dành cho FE và Owner; ngày11/11/2026; Session1.1. Phạm vi chỉ POST training-results đơn/bulk. Kết quả chuẩn, môi trường và giới hạn ở [Active/QA](../docs/00-MASTER-CHECKLIST.md#active); contract ở [docs01](../docs/01-API-CONTRACT-FE-BE.md#training-result-content-20261111).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Trước: staff có quyền attendance gửi `content:""`/chuỗi trắng có thể xóa hoặc ghi đè nhận xét Coach; Coach có MANAGE_CLASSES nhưng thiếu RECORD_RESULT cũng ghi được content.

Sau: non-Coach gửi bất kỳ content non-null đều403; bỏ trường hoặc null giữ nhận xét cũ và vẫn cho attendance/note hợp quyền. Ghi content cần Coach Active+RECORD_RESULT, đúng Coach phụ trách buổi. Bulk lỗi rollback toàn request, gồm item đã xử lý trước. Đã kiểm bằng HTTP loopback có login/auth-me và DB/audit sau commit trên H2/PostgreSQL local; không suy thành nghiệm thu FE/Render.

## 2. Quá trình và quyết định giữ/bỏ

- Đối chiếu D15/RP-36 và expected Owner đã duyệt; tái hiện lỗi qua HTTP trước sửa.
- Bỏ điều kiện bỏ lọt chuỗi blank; dùng cùng guard content cho đơn và từng item bulk, kể cả item có member không tìm thấy.
- Lấy role từ account đã lưu, actor từ phiên, tác giả theo phân công; coachId/actor/role client không cấp quyền.
- Giữ route attendance của staff, transaction/khóa, cửa sổ `[start,end)`, note/check-in/credit/lịch và guard hiện có; chưa làm ghi content sau end, scope GET hoặc room validation.

## 3. API và DB liên quan

- POST `/api/training-results`: content nằm ở request; POST `/api/training-results/bulk`: content nằm trên từng item. Thành công200; content trái quyền403, toàn bộ mutation bị hủy.
- GET `/api/training-results?sessionId=...&memberId=...` để readback bằng actor có quyền đọc; RECORD_RESULT riêng không tự cấp quyền GET cho Receptionist.
- `training_results.content` là nhận xét chuyên môn; `attendances.note` là ghi chú attendance. State Present/Absent và check-in gốc được xử lý theo contract hiện hành, không đồng nhất content với note hoặc giáo án lớp.
- Dùng các bảng hiện có và `system_logs`; không cần migration hoặc đổi OpenAPI schema. POST đơn vẫn yêu cầu coachId để tương thích DTO; BE không tin giá trị đó để chọn tác giả.

## 4. FE cần làm

Staff chỉ thao tác attendance/note phải bỏ content hoặc gửi null; không tự serialize ô nhận xét không sửa thành `""`. Chỉ mở thao tác sửa/xóa nhận xét cho Coach có RECORD_RESULT đúng phân công. Khi403 giữ dữ liệu đang hiển thị, báo lỗi rồi readback khi cần; bulk không đánh dấu riêng item đầu là đã lưu nếu request lỗi. Thành công đối chiếu cả content, attendanceStatus và note trả về.

Demo dùng Swagger của đúng BE local tại <http://127.0.0.1:18081/swagger-ui/index.html>. Nguồn FE hiện hành chưa xác định; không dùng FE_Project-main làm bằng chứng. Đã kiểm HTML Swagger/schema được BE phục vụ; công cụ browser không khả dụng nên chưa xác minh thao tác màn hình và không hướng dẫn vị trí menu/nút.

Fixture synthetic local riêng: Session19, Member3, Coach1, Manager6, cùng center1; ngày11/11, cửa sổ20:30–22:05 giờ VN. BE dùng Clock hệ thống nguyên trạng. Payload đăng nhập và request mẫu nằm trong file local bị Git bỏ qua: `artifacts/runs/training-content/2026-11-11-session1-1/demo-access.private.json`; không đưa credential vào bản bàn giao. Agent mới chỉ kiểm login/auth-me/read rỗng cho fixture demo này, chưa ghi thay Owner.

1. Trong Swagger gọi POST `/api/auth/login` với payload `loginCoach`, rồi GET `/api/auth/me`. Kết quả200, userId1/role Coach/có RECORD_RESULT là đúng. Gọi POST `/api/training-results` với `coachWrite`:200, content `Nhan xet Coach demo 1.1`, attendanceStatus Present là đúng.
2. Đổi phiên bằng POST `/api/auth/login` với `loginManager`, xác minh GET `/api/auth/me` trả200, userId6/role CenterManager/có MANAGE_CLASSES. POST cùng route với `staffAttendance` (không có content):200, state Absent, note `Staff note demo 1.1`, content cũ còn nguyên là đúng.
3. Giữ phiên Manager, POST cùng route với `staffDeniedErase` (`content:""`, state Present, note khác):403 là đúng; request này không được thay state/note/content.
4. GET `/api/training-results?sessionId=19&memberId=3`:200, content vẫn `Nhan xet Coach demo 1.1`, state Absent, note `Staff note demo 1.1` là đúng. Đây là kết quả mong đợi để Owner đối chiếu, chưa là browser PASS.

Nếu thực hiện sau22:05, giữ guard đóng cửa sổ; chuẩn bị **fixture synthetic mới** theo giờ thực trước khi thử lại, không đổi Clock/lịch sử hoặc reset cloud.

## 5. Việc còn lại

Owner đã chọn `skip demo 1.1` và cho tiếp tục1.2/1.3; demo thủ công1.1 ghi **SKIPPED_BY_OWNER**, không là browser PASS. Kiểm BE bắt buộc giữ nguyên theo Active/QA; kết quả1.2/1.3 có bàn giao riêng. Browser/FE, deploy/Render và smoke Supabase sau reset vẫn chưa kiểm ở mục này. Session2/3 ngoài checkpoint này.
