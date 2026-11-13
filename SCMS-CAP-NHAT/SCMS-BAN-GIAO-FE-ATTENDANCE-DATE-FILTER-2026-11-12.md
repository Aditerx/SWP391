# SCMS — Bàn giao FE: lọc attendance theo ngày buổi học

Dành cho FE và Owner; ngày12/11/2026; Session2.1/R1. Phạm vi GET `/api/attendances` và alias `/api/attendance`. Quyết định ở [D15](../docs/04-DEBUG-NOTE.md#d15), contract ở [R1](../docs/01-API-CONTRACT-FE-BE.md#attendance-date-filter-r1-20261112); kết quả/source/môi trường chuẩn tại [Active/QA](../docs/00-MASTER-CHECKLIST.md#active).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Trước: có filter ngày thì service so bằng checkInTime; attendance Absent hoặc Present có timestamp null bị thiếu dù buổi nằm trong khoảng. Dữ liệu legacy có timestamp khác ngày buổi có thể xuất hiện sai ngày nếu màn hình muốn xem theo ngày buổi.

Sau: record gắn Session dùng sessionDate; chỉ lobby lịch sử không Session mới dùng ngày checkInTime. Hai đầu ngày được tính, bỏ cả hai filter ngày giữ tập record/phạm vi hiện hành. Record thiếu ngày để so không khớp filter ngày. Method, query, response, enum và sort giữ nguyên; không tạo timestamp hoặc sửa attendance để làm kết quả lọc. Nghiệm thu local HTTP/DB/audit và phần chưa kiểm theo Active/QA, không tự là FE/browser PASS.

## 2. Quá trình và quyết định giữ/bỏ

- Owner chốt phương án ngày Session sau khi xem tác động API/FE; giữ lobby fallback và scope/các luồng ghi hiện có.
- Kiểm HTTP trước sửa bằng fixture synthetic riêng, tái hiện các ca chưa đạt expected mới thay vì sửa dữ liệu lịch sử.
- Sửa predicate tại đường service đang chạy, bỏ query repository không có caller còn giữ nghĩa cũ; không thêm module/API/dateBasis hoặc đổi toàn bộ cơ chế search.
- Thêm mô tả ngày vào Swagger của hai alias, đối chiếu schema giữ nguyên shape; kiểm response và trạng thái sau commit rồi clean theo AGENTS.

## 3. API và DB liên quan

GET `/api/attendances` nhận sessionId/memberId/recordedBy/state/startDate/endDate như cũ. Hai field ngày optional dạng YYYY-MM-DD, AND với các filter và quyền hiện có. Gửi startDate=endDate=2026-11-12 sẽ lấy mọi record gắn buổi12/11 trong scope, kể cả checkInTime:null; lobby thì so ngày check-in gốc. Chỉ startDate hoặc chỉ endDate cũng áp dụng cùng nghĩa. Alias `/api/attendance` đồng nhất.

AttendanceResponse đã có sessionDate/checkInTime/note/state; không đổi shape hoặc thêm field bắt buộc. State độc lập timestamp: Present với checkInTime:null vẫn là Present theo record. Superseded vẫn không là bản hiện hành. Member own, Coach được phân công và staff theo center hiện có; không mang ngoại lệ Admin-null của training-results sang đây. Lỗi parse ngày400 và phiên/quyền401/403 giữ hiện hành.

Đọc `sessions.session_date`, `attendances.check_in_time`; không đổi cột/schema/fixture canonical hoặc chạy migration. GET không mutation attendance/note/content/check-in/session/credit/invoice/audit. Trường hợp legacy dùng trong test là dữ liệu synthetic kiểm tương thích, không là bằng chứng luồng ghi hiện tại cho nhập timestamp khác ngày.

## 4. FE cần làm

Giữ cách gọi API và parse response nếu bộ lọc hiện mang nghĩa ngày buổi. Đối chiếu client filter/grouping: dùng sessionDate cho ngày buổi, xử lý checkInTime:null an toàn và không loại record chỉ vì thiếu timestamp. Cách hiển thị ngày/giờ trống do FE quyết định; không suy null thành Absent hoặc tự mặc định Present. List/count tính từ kết quả có thể thay đổi vì các record hợp lệ trước bị lọc mất nay được trả, hoặc timestamp legacy khác ngày buổi được xếp lại đúng ngày buổi. Không giả đã kiểm build FE online; nguồn FE do Owner xác nhận ở [docs02](../docs/02-DEPLOYMENT-GUIDE.md#current-frontend).

Demo chỉ đọc dùng <http://127.0.0.1:18084/swagger-ui/index.html> và DB synthetic local riêng. JSON đăng nhập `artifacts/runs/attendance-dates/2026-11-12-r1/login-manager.private.json` chỉ có email/password; request và expected fixture `artifacts/runs/attendance-dates/2026-11-12-r1/demo-access.private.json` là file local bị Git bỏ qua, không thuộc gói FE công khai. Không gõ chữ loginManager như username. Các bước dùng tên operation và nhãn Swagger đã đối chiếu bundle/schema; chưa quan sát thao tác browser của Owner.

1. Mở JSON đăng nhập, copy toàn bộ. Trong nhóm auth-controller, mở POST `/api/auth/login` → Try it out, thay toàn bộ Request body bằng JSON rồi Execute. Request URL phải dùng `127.0.0.1:18084`. Server response200, userId8/CenterManager/có MANAGE_CLASSES là đúng. Trên cùng Swagger gọi GET `/api/auth/me` → Try it out → Execute,200 cùng actor là đúng trước kiểm nghiệp vụ.
2. GET `/api/attendances`: điền sessionId=1, startDate=2026-11-12, endDate=2026-11-12, các filter khác để trống; Execute.200 với attendanceId `[3,2,1]` là đúng: gồm Present có timestamp, Absent/null và Present/null của cùng buổi. Không dùng checkInTime:null để suy state; xem state của từng row.
3. Giữ các filter trên, thêm state=Absent:200, chỉ attendanceId2/checkInTime:null là đúng. Đổi startDate=endDate=2026-11-11, vẫn sessionId1, bỏ state:200 `[]` là đúng vì buổi này ngày12/11.

Đây là expected để Owner đối chiếu; kết quả API agent đã kiểm không thay kết quả browser. Fixture đọc dùng ngày cụ thể, không phụ thuộc cửa giờ attendance và không cần reset chỉ vì qua ngày.

## 5. Việc còn lại

Owner12/11 đã chọn `skip demo R1`; demo là SKIPPED_BY_OWNER, bằng chứng BE giữ nguyên theo Active/QA. Bước tiếp là2.2 rồi2.3 theo [prompt tiếp Session2](../docs/03-TODO.md#session2-continuation-prompt-20261112); skip R1 không tự duyệt API mới hoặc skip demo các mục sau. FE/browser, build/API origin của FE deployed và smoke Render/Supabase sau reset chưa được nghiệm thu bởi phần BE local này. Không tự reset/seed cloud, SQL16, push/merge/deploy hoặc gọi provider; chưa triển khai content sau buổi2.2/shared plan2.3/Session3.
