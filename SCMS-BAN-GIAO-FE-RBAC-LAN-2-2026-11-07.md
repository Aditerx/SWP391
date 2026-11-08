# SCMS — Bàn giao FE RBAC lần2 — Attendance Coach — 07/11/2026

Đối tượng: FE phụ trách màn hình `/coach/attendance` trên [FE Pages](https://scms-demo-04nov.pages.dev/). Phạm vi: đồng bộ buổi được chọn, dữ liệu điểm danh và thao tác ghi/sửa. Bản này tiếp nối [LAN-1](SCMS-BAN-GIAO-FE-RBAC-LAN-1-2026-11-06.md), không thay nội dung bàn giao Member trước đó. Contract tại [docs01 mục12](docs/01-API-CONTRACT-FE-BE.md#attendance), quyết định tại [D15/D17](docs/04-DEBUG-NOTE.md#d15); nguồn/build và bằng chứng tại [Active](docs/00-MASTER-CHECKLIST.md#active) và `rbacDesignCorrections.ownerFeAttendanceFeedback` trong [QA](artifacts/qa/latest.json).

## 1. Kết quả hiện tại

| Nội dung | Đã quan sát / giới hạn |
|---|---|
| Đọc roster | Owner đăng nhập Coach có RECORD_RESULT, GET `/api/classes/10/enrollments` trả200 và có danh sách học viên. Chưa nghiệm thu toàn bộ role/owner/center hoặc lớp23 của ảnh cũ |
| Lỗi UI đã xác nhận | Đổi buổi09/11 rồi13/11 trên dropdown nhưng vùng điểm danh bên dưới vẫn giữ dữ liệu/ngữ cảnh thao tác cũ. Roster cùng lớp có thể giống nhau; trạng thái điểm danh và record đang sửa phải theo đúng buổi |
| Record dùng để đối chiếu | Payload sửa từng gửi attendanceId85; lịch sử cho biết record này thuộc sessionId10 ngày05/11. Buổi09/11 là sessionId11; buổi13/11 là sessionId109. Các ID này là bằng chứng sự cố, không là fixture để ghi thử |
| Giới hạn kết luận | Payload85 có thể đúng với record cũ còn hiển thị. Chưa kiểm source FE hiện hành để kết luận lỗi nằm ở dropdown, tải dữ liệu, state, response về muộn, form sửa hay hàm gửi. Request đang được chọn trong Network không chứng minh có một POST mới sau mỗi lần đổi buổi |
| Ghi/sửa | Owner quan sát `/correct`403 với Coach, `/check-in`400 LOBBY_CHECKIN_DISABLED và POST root `/attendances`405. Chưa thử điểm danh lần đầu qua đúng luồng và đọc lại trạng thái sau khi ghi thành công |
| Cập nhật Owner / Late | Owner báo lần thử UI mới chỉ còn /correct403 đã biết và FE đang phát triển; payload thử có newState Late. Đây chưa là bằng chứng lưu thành công hoặc chốt thêm trạng thái mới; D15 hiện chỉ nhận Present/Absent, giữ đọc Late lịch sử |

## 2. Quá trình và quyết định giữ/bỏ

1. Giữ kết quả roster200 ở phạm vi đã quan sát; không dùng nó nghiệm thu ghi attendance hoặc đóng toàn BE-03.
2. Sau khi đối chiếu history/catalog/payload, Owner làm rõ rằng vùng dưới không đổi theo dropdown; bỏ kết luận quá sớm rằng riêng hàm gửi đã chọn sai attendanceId.
3. Giữ D15 hiện hành: Present/Absent trong giờ buổi, quản lý sửa sau buổi có reason/audit; Late sau10phút từng nằm trong source/phương án cũ nhưng lịch sử quyết định D15 đã chọn bỏ cho dữ liệu mới, giữ Late lịch sử.
4. Giữ check-in cửa tách attendance buổi và đang tắt; không bật tính năng hoặc mở quyền Coach để làm hết400/403, không đổi lịch sử DB để hợp thức hóa UI.

## 3. API và DB liên quan

| Method / endpoint | Input, dữ liệu và cách dùng |
|---|---|
| GET `/api/auth/me` | Lấy userId/role/authorities hiện hành; không dùng coachId nhập tùy ý để xác định người thao tác |
| GET `/api/sessions?classId={classId}` hoặc `/api/sessions/{sessionId}` | Dữ liệu session trong phạm vi phiên; dùng id, sessionDate, startTime, endTime, status, room để dựng thông tin buổi đang chọn |
| GET `/api/sessions/catalog/{classId}` | Chỉ session Scheduled của lớp Normal Open/Ongoing, không private. Không dùng làm lịch đầy đủ của màn hình quản lý attendance hoặc suy số thứ tự toàn khóa từ danh sách còn lại |
| GET `/api/classes/{classId}/enrollments` | Roster lớp được giao. Danh sách Member của cùng lớp có thể giữ nguyên khi đổi buổi; điều đó không cho phép giữ attendance của buổi cũ |
| GET `/api/attendances?sessionId={sessionId}` | Record attendance của đúng buổi trong phạm vi actor. Ghép theo sessionId + memberId; attendanceId là ID riêng để sửa record. Không có record thì không mặc định Present/Absent |
| POST `/api/training-results` | Ghi attendance thủ công trong buổi: sessionId, memberId, coachId theo phiên và attendanceStatus Present/Absent; content/reason theo schema. Coach phải được phân công và đủ quyền; thành công200 |
| POST `/api/training-results/bulk` | sessionId, coachId và items theo schema; cùng điều kiện nghiệp vụ với ghi đơn. Không dùng bulk để vượt cửa sổ hoặc assignment |
| POST `/api/attendances/scan` | Luồng QR nếu có dùng: memberId, code, sessionId; đúng Coach/phân công, quyền học và giờ buổi. Không thay bằng check-in cửa |
| POST `/api/attendances/correct` | Sửa sau buổi: attendanceId, newState, reason; Manager/Admin có MANAGE_CLASSES, đúng center và điều kiện service. Coach403 là đúng; đây không phải API tạo attendance lần đầu |

Các lỗi cần xử lý theo đúng luồng:

- POST `/api/attendances/check-in` là check-in cửa;400 LOBBY_CHECKIN_DISABLED đã được Owner cung cấp. POST `/api/attendances` không có method ghi nên405. Không thử các tuyến này làm fallback cho điểm danh ca.
- Ngoài giờ buổi, đường ghi attendance đúng cũng có thể trả400 ATTENDANCE_WINDOW_CLOSED; giá trị mới khác Present/Absent trả400 INVALID_ATTENDANCE_STATE. Không đổi endpoint/role hoặc chỉ giấu lỗi để coi là đã ghi.
- OPTIONS200 chỉ là preflight; GET200 sau POST403 là đọc lại dữ liệu, không phải ghi thành công.401 cần xử lý phiên;403 cần giữ thông báo quyền phù hợp.

**Chỗ lưu dữ liệu/ghi chú theo source hiện hành:** ghi thủ công qua training-results lưu training_results (content, attendance_status) và đồng bộ attendances.state; correct cập nhật attendances.state, đồng bộ training_results.attendance_status nếu đã có record, và lưu reason trong system_logs.detail với action ATTENDANCE_CORRECTION/target_entity attendances/target_id attendanceId. correct không gán reason vào attendances.note. Cột note hiện có luồng báo vắng gán lý do, nhưng AttendanceResponse/GET attendances không trả note. POST403 chưa chạy handler nên không lưu thay đổi hoặc reason của request đó. Nếu FE cần hiển thị lý do sửa, cần contract đọc đúng phạm vi riêng, không suy rằng GET attendances đã trả hoặc cấp Coach xem toàn audit-log.

Quan hệ cần giữ: Class → Session → Attendance theo Member; attendanceId không phải sessionId. `recordedBy=null` ở Absent tương thích với finalizer tự chốt, chưa xác nhận nguồn từng dòng cụ thể bằng audit. `checkInTime/checkOutTime=null` tự nó không chứng minh attendance buổi sai; không dùng các field đó thay giờ học trong Session. Không cần migration, reset, chạy lại SQL16 hoặc sửa dữ liệu thật cho bản bàn giao UI này.

## 4. FE cần làm

1. **Đồng bộ ngữ cảnh khi đổi buổi:** cập nhật session được chọn, xóa/đánh dấu hết hiệu lực dữ liệu attendance cũ và form/lý do sửa đang mở, khóa ghi trong lúc tải. Chỉ áp response còn khớp lựa chọn hiện hành; response cũ về muộn không được ghi đè buổi mới. Cách triển khai tùy source FE thực tế, chưa chốt một nguyên nhân kỹ thuật.
2. **Hiển thị đúng dữ liệu:** trạng thái, attendanceId và thao tác phải cùng sessionId + memberId. Buổi mới chưa có record thì hiển thị chưa ghi theo cách FE chọn, không lấy kết quả buổi gần nhất của Member. Ngày/giờ/phòng của buổi lấy từ Session; nếu hiển thị ngày khai giảng lớp thì ghi nhãn phân biệt, không coi đó là giờ buổi đang thao tác.
3. **Phân biệt ghi trong buổi với correction:** Coach ghi Present/Absent qua training-results trong `[start,end)` khi buổi còn hợp lệ. Trước giờ khóa ghi; sau buổi Coach xem, quản lý sửa qua correct nếu đủ điều kiện. Nút bút chì không được mở correction quản lý cho Coach; nếu có thao tác đổi kết quả trong buổi thì vẫn dùng đúng luồng ghi trong buổi.
4. **Trạng thái và lỗi:** không tạo Late/Excused từ nút Trễ/Phép; dữ liệu lịch sử vẫn có thể đọc/hiển thị tương thích. POST thất bại không giữ badge hoặc thông báo như đã lưu. Khi thành công, đọc lại đúng session/member trước khi xác nhận; không để việc tải lại một GET200 che mất POST lỗi.
5. **Giữ quyền tại BE:** không cấp MANAGE_CLASSES/MANAGE_USERS cho Coach để chữa lỗi màn hình. Chỉ gọi API đúng chức năng, không tự thử lần lượt correct/check-in/root attendance hoặc tắt chung thông báo403.
6. **Xác định source đang chạy:** đối chiếu repo/commit/build của FE Pages trước sửa; FE_Project-main trong repo BE là bản tham khảo cũ, chưa được xác nhận là source của bản Pages này.

## 5. Việc còn lại

**Điều phối Owner07/11:** attendance chờ phản hồi FE; nghiệm thu FE/Render thực hiện sau khi xong các mục/luồng khác hoặc kiểm chung. Các checkbox dưới là tiêu chí cho đợt đó, không yêu cầu Owner chạy ngay. Phần rà BE riêng xem [đề xuất Coach](docs/03-TODO.md#coach-review-proposal-20261107).

- [ ] FE xác định source hiện hành và sửa đồng bộ lựa chọn/detail/form/request; bản này mới là bàn giao, chưa sửa source FE.
- [ ] Đổi lần lượt buổi, rồi đổi nhanh để response có thể về khác thứ tự: thông tin buổi, attendance và thao tác luôn khớp lựa chọn cuối; form cũ không còn ghi được vào buổi cũ.
- [ ] Với buổi tương lai/chưa có record: không hiện kết quả buổi cũ, không mặc định Có mặt/Vắng và không cho Coach correction quá khứ từ màn hình đó.
- [ ] Kiểm first-write thủ công trong một buổi được phép: response thành công, tải lại còn đúng kết quả; kiểm lại trước start/tại end và quyền Coach khác bằng fixture được phép. Kiểm QR nếu FE sử dụng. Chưa có các kết quả live này trong bàn giao.
- [ ] Kiểm riêng correction bằng quản lý trên record kiểm thử hợp lệ: reason/audit và state đọc lại; không yêu cầu Coach sửa sau buổi thành công.
- [ ] Ghi kết quả Owner/FE vào Active/QA sau khi hoàn tất. Nếu cần kiểm biên giờ, dùng Clock và PostgreSQL local riêng theo [docs02](docs/02-DEPLOYMENT-GUIDE.md#testing); không đổi lịch sử Supabase để ép test xanh.

Các phần Coach khác được theo dõi riêng ở [checklist Coach](docs/03-TODO.md#coach-open-items-20261107), không gộp vào nghiệm thu attendance hoặc yêu cầu FE sửa trong bản LAN-2 này.
