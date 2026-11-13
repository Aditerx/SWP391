# SCMS-CAP-NHAT — Mục lục và checklist đối chiếu FE

Đối tượng: FE tích hợp và Owner review. Cập nhật13/11/2026: bộ bàn giao Session1–3 đã được bổ sung theo từng luồng; phạm vi BE local và giới hạn nghiệm thu theo [Active](../docs/00-MASTER-CHECKLIST.md#active)/QA. Demo từng mục **SKIPPED_BY_OWNER**; chưa nghiệm thu FE/browser/Render/cloud.

Bộ cập nhật được chia thành các bản bàn giao ngắn theo từng luồng/tính năng. Mỗi bản trình bày vấn đề, phần đã sửa/cập nhật, API/DB liên quan và việc FE cần điều chỉnh hoặc kiểm tra.

`KIEM-TRA.md` là mục lục kiêm checklist tổng: tóm tắt từng nhóm cập nhật và liên kết tới file bàn giao chi tiết để dễ tra cứu, đối chiếu và bắt nhịp. Giữ cách tổ chức theo luồng Owner đã duyệt12/11; kết quả Session3 bổ sung đúng ngày13/11.

Mỗi bản giữ một luồng và năm mục theo AGENTS. Contract chính thức ở [docs01](../docs/01-API-CONTRACT-FE-BE.md); trạng thái/bằng chứng chuẩn ở [Active](../docs/00-MASTER-CHECKLIST.md#active) và [QA](../artifacts/qa/latest.json). File này không chép lại log, SHA/count test hoặc toàn bộ contract. Owner13/11 đã mở Session3 và skip demo; không mở quyền tích hợp FE/cloud, merge/push/deploy/reset/provider hoặc tạo ZIP.

## 1. Bộ bàn giao Session1–3

Các mục có bằng chứng BE local đúng phạm vi theo Active/QA; xem matrix cho phần kiểm mới và reuse. Demo Owner từng mục là **SKIPPED_BY_OWNER**. Ô dưới đây dành cho **đối chiếu FE**, đang để trống vì chưa có bằng chứng FE hoàn tất; không dùng ô trống để hiểu rằng BE chưa sửa hoặc kiểm.

| Mục | Vấn đề và phần đã sửa/cập nhật | API liên quan | FE cần đối chiếu | FE đã kiểm |
|---|---|---|---|---|
| [1.1 — Quyền ghi nhận xét](SCMS-BAN-GIAO-FE-TRAINING-RESULT-CONTENT-2026-11-11.md) | Staff gửi content rỗng có thể ghi đè nhận xét. Nay content non-null chỉ dành cho Coach Active, có RECORD_RESULT, đúng phân công; bỏ/null giữ nhận xét. Bulk lỗi rollback toàn request. | POST `/api/training-results`, POST `/api/training-results/bulk` | Form attendance của staff bỏ content hoặc gửi null; kiểm cả chuỗi rỗng/trắng bị403 và dữ liệu cũ còn nguyên sau lỗi. | [ ] |
| [1.2 — Phạm vi đọc kết quả](SCMS-BAN-GIAO-FE-TRAINING-RESULT-SCOPE-2026-11-11.md) | Đồng nhất scope: staff theo center BE và filter; Admin không center đọc global khi đủ điều kiện; Member/Coach giữ own/author. | GET `/api/training-results` | Đối chiếu danh sách theo phiên/center, không dùng filter client hoặc authority phụ để suy thêm quyền. | [ ] |
| [1.3 — Validation phòng/sân](SCMS-BAN-GIAO-FE-ROOM-VALIDATION-2026-11-11.md) | Request/JPA/contract khớp giới hạn DB: name bắt buộc tối đa50; location optional/nullable tối đa100; vượt giới hạn400 trước mutation. | POST `/api/rooms`, PUT `/api/rooms/{id}` | Đồng bộ form và lỗi field; giữ semantics chuỗi/null, không tự truncate/trim thay BE. | [ ] |
| [2.1/R1 — Lọc ngày attendance](SCMS-BAN-GIAO-FE-ATTENDANCE-DATE-FILTER-2026-11-12.md) | Record gắn buổi lọc theo sessionDate; lobby không Session theo ngày checkInTime; tính cả hai đầu ngày. Absent hoặc Present có timestamp null không còn bị loại sai nếu ngày buổi khớp. | GET `/api/attendances`, alias `/api/attendance` | Rà filter/grouping/count; xử lý checkInTime:null an toàn, không suy state từ timestamp. | [ ] |
| [2.2/R2 — Sửa content sau buổi](SCMS-BAN-GIAO-FE-TRAINING-RESULT-AFTER-SESSION-2026-11-12.md) | Thêm mutation content-only trên result có sẵn, đúng Coach/phân công/tác giả, từ end đến trước00:00 ngày kế tiếp ngày Session giờ VN; audit cùng transaction. | PATCH `/api/training-results/{resultId}/content` | Tách thao tác sau buổi khỏi POST; gửi content tường minh, gắn draft với resultId/Session; PATCH200 rồi GET đọc lại đúng ngữ cảnh. | [ ] |
| [2.3 — Shared plan lịch sử](SCMS-BAN-GIAO-FE-SHARED-PLAN-HISTORY-2026-11-12.md) | Member đọc shared Normal plan khi Registered hoặc Completed với Paid/quyền hiệu lực hợp lệ; list/detail cùng guard. Cancelled ngoài shared read; own history giữ scope. Khai nullable đúng payload có sẵn. | GET `/api/training-plans`, GET `/api/training-plans/{id}` | Rà filter Registered/Ongoing/endDate; nhận shared memberId:null; kiểm list/detail, đổi lớp/cache và scope Member khác. | [ ] |
| [3.1 — RBAC/quản trị cuối](SCMS-BAN-GIAO-FE-RBAC-LAST-ADMIN-2026-11-13.md) | PUT cần Admin Active+MP; giữ một Admin Active có cả MU+MP qua permission role/PUT user/PATCH role/status, khóa chung và chống race. | PUT `/api/roles/{id}/permissions`, các mutation user liên quan | Dùng auth-me hiện hành, xử lý409 LAST_ADMIN_REQUIRED/readback; quyền đổi trên role tác động mọi account dùng role đó. GET RBAC Admin-only giữ nguyên. | [ ] |
| [3.2 — Error/deprecation](SCMS-BAN-GIAO-FE-ERROR-DEPRECATION-2026-11-13.md) |409/body409 nhất quán; room duplicate POST/PUT409 rollback; metadata lỗi/quyền và shared retired đúng runtime, giữ Advanced. | Các operation đích trong catalog; không envelope chung mới | HTTP→code/message fallback, không logout mọi403; bỏ API mua shared, không retire nhầm Advanced; conflict cần tải lại state/version. | [ ] |
| [3.3 — Matrix/readiness](SCMS-BAN-GIAO-FE-MATRIX-READINESS-2026-11-13.md) | Kiểm bổ sung Refund approve theo năm role/authority/center; notification owner/direct/recipient/read/dedupe; timeout container local; checker và scope bằng chứng. | Approve Refund, notifications, login/auth-me; các luồng khác theo bản riêng | Không mở approve cho Admin từ authority; notification read-state theo user; xác định build/origin rồi kiểm FE riêng. | [ ] |

Mục1.1 và2.2 bổ sung cho nhau: POST trong buổi vẫn giữ semantics bỏ/null content; PATCH sau buổi yêu cầu content non-null, dùng `""` để xóa. Không áp semantics của route này cho route kia. Quyền content cũng không lấy từ `correctionAllowed` của Attendance.

## 2. Cách đối chiếu từng luồng

1. Mở bản bàn giao tương ứng, đọc mục4 “FE cần làm”; tra field/state/error tại đúng phần docs01 hoặc [OpenAPI](../artifacts/fe-handoff/openapi.json).
2. Xác định FE build/API origin và BE/DB đích trước khi thử; đăng nhập và GET `/api/auth/me` để kiểm đúng actor/quyền. Nguồn FE hiện hành xem [docs02](../docs/02-DEPLOYMENT-GUIDE.md#current-frontend).
3. Kiểm thao tác hợp lệ và ca bị chặn liên quan; đối chiếu response, rồi GET đọc lại đúng resultId/sessionId/classId. Ca mutation phải có readback dữ liệu/audit theo phạm vi được phép, không chỉ dựa thông báo lưu thành công.
4. Chỉ đánh dấu ô FE khi có kết quả thực tế và ghi nguồn đối chiếu tại Active/QA theo AGENTS. Skip demo tiếp tục là skip; không đổi thành FE PASS.

Các fixture/port/ngày trong từng bản là checkpoint local đã chuẩn bị, không bảo đảm server hoặc cửa giờ còn dùng được khi đọc ZIP. Hướng dẫn môi trường/demo nằm trong [docs02](../docs/02-DEPLOYMENT-GUIDE.md), không đóng gói credential hoặc private fixture.

## 3. Readiness và phần chưa nghiệm thu

- Đã thêm các bản Session3 đúng luồng/ngày và giữ nguyên sáu bản Session1–2; không viết thay đổi mới vào bản ngày cũ.
- PUT room trùng tên500 trước sửa đã được xử lý/kiểm trong3.2. Checker được sửa để ghi nhận root schema bị xóa, giữ deletion và fingerprint lịch sử; kết quả source/hash/route/type/property cuối tại Active/QA.
- Đối chiếu giao nhau: POST content bỏ/null khác PATCH content bắt buộc; shared Completed/Paid khác Cancelled; Admin authority không thay role tài chính; shared retired khác Advanced đang dùng; body status409 khi có field status không đổi coded-error hai field.
- Readiness đủ cho phạm vi BE local có evidence ở [matrix](../docs/03-TODO.md#session33-risk-matrix-20261113). Các method/case chưa có bằng chứng riêng trong inventory vẫn NOT_RUN; checker offline không là full JSON Schema hoặc full business/role matrix.
- FE build/API origin, browser/cookie/proxy, Render/cloud/live smoke/provider tiếp tục **NOT_RUN**; mọi ô FE giữ trống. Không suy BE local hoặc demo skip thành FE PASS.
- Demo1.1→3.3 giữ **SKIPPED_BY_OWNER** theo các quyết định đã ghi; không giữ checkpoint đòi Owner thử lại demo. Bước tiếp thuộc đợt tích hợp/kiểm FE được mở riêng; Owner tự đóng gói ZIP khi cần.

## 4. Phạm vi thư mục và đóng gói

Giữ một thư mục chung `SCMS-CAP-NHAT` ở root repo; giữ từng file theo tính năng, dùng mục lục để phân Session. Các bản Normal Lifecycle/Refund10/11 và bàn giao cũ không thuộc sáu mục Session1–2 này, tiếp tục ở vị trí hiện có.

Thư mục này chỉ chứa ghi chú bàn giao và checklist; contract, OpenAPI, QA, cấu hình và source vẫn có một nguồn chuẩn trong repo. Liên kết sang `../docs`, `../artifacts`, `../database` cần repo tương ứng; khi giải nén, đặt `SCMS-CAP-NHAT` ở root repo để mở các liên kết đó. ZIP riêng vẫn đọc được nội dung và liên kết nội bộ giữa các bản, nhưng không phải bộ source/môi trường độc lập.

Owner tự đóng gói ZIP; agent chỉ duy trì thư mục nguồn, nội dung và liên kết, không tạo hoặc xuất lại ZIP ở cuối session. Không đưa `.env`, credential, cookie, file `*.private.json`, backup hoặc raw runs vào bộ gửi.
