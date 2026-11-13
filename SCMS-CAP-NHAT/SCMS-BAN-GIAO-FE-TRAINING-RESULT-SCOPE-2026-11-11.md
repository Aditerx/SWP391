# SCMS — Bàn giao FE: phạm vi đọc kết quả buổi tập

Dành cho FE và Owner; ngày11/11/2026; Session1.2. Phạm vi chỉ GET `/api/training-results`; ghi content thuộc [bàn giao1.1](SCMS-BAN-GIAO-FE-TRAINING-RESULT-CONTENT-2026-11-11.md). Trạng thái/bằng chứng chuẩn tại [Active/QA](../docs/00-MASTER-CHECKLIST.md#active), contract tại [scope đọc11/11](../docs/01-API-CONTRACT-FE-BE.md#training-result-read-scope-20261111).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Trước: nhánh staff có MANAGE_CLASSES/MANAGE_USERS không lọc center; fixture local đã tái hiện staff center1/2 nhận cả kết quả center khác, kể cả khi chọn filter ngoài center.

Sau: staff và Admin có center chỉ đọc result thuộc Session.Class cùng center actor; filter sessionId/memberId/coachId kết hợp AND với scope. Admin Active center NULL, có authority đọc vẫn global. Member/Coach giữ own/author kể cả có authority quản trị hoặc lịch sử ở center cũ. Query lấy center actor từ BE; tham số centerId/role/actor client không mở scope. Kết quả kiểm local HTTP có phiên, DB/audit readback ở Active/QA; chưa là nghiệm thu FE/Render.

## 2. Quá trình và quyết định giữ/bỏ

- Đối chiếu D15 và quyết định Owner11/11; giữ ngoại lệ Admin-null đọc global của mẫu scope hiện hành.
- Tái hiện rò scope bằng fixture synthetic hai center; A/B cùng center đối chứng own/author độc lập với quyền quản trị.
- Thêm điều kiện center vào query, kiểm Active/role/authority trong service và ưu tiên nhánh Member/Coach theo role đã lưu.
- Giữ route/shape response/filter/sort và lịch sử own/author; không thêm bảng/API hoặc mở quyền tiền/report/Manager-only.

## 3. API và DB liên quan

GET `/api/training-results` vẫn nhận sessionId/memberId/coachId tùy chọn, trả mảng mới nhất trước. Staff chọn filter không có record trong center mình nhận200 `[]`. Member truyền memberId người khác hoặc Coach truyền coachId tác giả khác nhận403. Staff non-Admin thiếu center403; Admin-null vẫn phải Active và có MANAGE_CLASSES hoặc MANAGE_USERS.

`training_results → sessions → classes.center_id` xác định center của result cho staff; không lấy center hiện tại của Member/Coach để phân loại lịch sử. Scope own theo member_id, author theo training_results.coach_id. GET không đổi content/attendance/note/check-in/session/invoice/enrollment/audit. Không migration; OpenAPI shape không đổi.

## 4. FE cần làm

Giữ các filter hiện có; không gửi centerId/actor/role như cách xin thêm quyền. Xử lý200 `[]` là không có dữ liệu trong phạm vi được đọc. Khi đổi phiên/quyền/center, xác minh `/api/auth/me` rồi tải lại dữ liệu; Member/Coach có authority phụ vẫn hiển thị own/author. Admin-null có authority đọc có thể nhìn cả hai center; Admin có center không được coi là global.

Demo chỉ đọc dùng BE local <http://127.0.0.1:18082/swagger-ui/index.html> và DB synthetic riêng; không dùng Supabase hoặc BE Render. Payload đăng nhập nằm trong file local bị Git bỏ qua: `artifacts/runs/training-scope/2026-11-11-session1-2/demo-access.private.json`, mục `login`; credential không nằm trong bản bàn giao. `login.manager1` là object chứa email/password, không phải tên đăng nhập để gõ nguyên văn. Xem [hướng dẫn đăng nhập từng bước và file JSON sẵn để copy](../docs/02-DEPLOYMENT-GUIDE.md#local-scope-demo-login). Thông tin FE hiện hành được duy trì tại [nguồn và cấu hình](../docs/02-DEPLOYMENT-GUIDE.md#current-frontend); bản này giữ phạm vi demo local của checkpoint11/11.

1. POST `/api/auth/login` với `login.manager1`, rồi GET `/api/auth/me`:200, userId7/CenterManager là đúng. GET `/api/training-results`:200, resultId theo thứ tự `[3,2,1]` là đúng; GET thêm `?sessionId=3`:200 `[]` là đúng vì Session3 thuộc center2.
2. Đăng nhập bằng `login.admin-global`, xác minh auth-me userId11/Admin/có MANAGE_USERS. GET không filter:200, resultId `[5,4,3,2,1]` là đúng; GET `?sessionId=3`: `[5,4]` là đúng. Center NULL là dữ liệu fixture BE, không cần thêm centerId vào request.
3. Đăng nhập bằng `login.admin1`, xác minh userId12/Admin. GET không filter:200, `[3,2,1]` là đúng vì Admin này có center1.
4. Đăng nhập bằng `login.member-a`, xác minh userId1/Member, dù có MANAGE_CLASSES/MANAGE_USERS. GET không filter: `[5,1]` là đúng; GET `?memberId=2`:403 là đúng. Có thể đối chứng `login.coach-a` (userId4/Coach): không filter `[5,2,1]`, `?coachId=5` trả403. Result5 là lịch sử own/author ở center2, không mất chỉ vì account hiện ở center1.

Các bước trên là kết quả mong đợi để Owner đối chiếu. Kiểm HTTP tự động không được đổi thành browser PASS.

## 5. Việc còn lại

Bản này lưu checkpoint BE11/11; quyết định Owner về demo và bước tiếp hiện hành tra [Active/QA](../docs/00-MASTER-CHECKLIST.md#active). Kiểm BE local không tự thành browser PASS. Deploy/Render và smoke Supabase sau reset chưa được nghiệm thu trong luồng này. Không tự reset/seed cloud, SQL16, push/deploy hoặc gọi provider. Session2/3 không được suy đã hoàn tất từ kết quả này.
