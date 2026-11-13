# SCMS — Bàn giao FE: Member đọc shared Normal plan sau hoàn tất — 12/11/2026

Đối tượng: FE tích hợp giáo án lớp/lịch sử học và Owner nghiệm thu Session2.3. Field/state/quyền chuẩn ở [contract](../docs/01-API-CONTRACT-FE-BE.md#shared-plan-history-20261112); kết quả theo nguồn/môi trường ở [Active/QA](../docs/00-MASTER-CHECKLIST.md#active).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Đã đồng nhất predicate GET list/detail cho Member đọc shared plan Normal: enrollment Registered hoặc Completed cùng Paid hợp lệ trên lớp hiệu lực. BE local H2/PostgreSQL/clean cuối đã đạt, gồm response và DB/audit sau commit; bằng chứng chuẩn ở Active/QA. Owner đã chốt Cancelled không có shared read dù từng Paid/có kết quả; own history giữ quyền/scope hiện có. Owner chọn skip riêng demo2.3 và đóng Session2, ghi SKIPPED_BY_OWNER; không thành browser/FE PASS.

| Ngữ cảnh | Trước sửa | Sau sửa |
|---|---|---|
| Registered + Paid hợp lệ | Đọc shared plan của lớp | Giữ đọc, list/detail cùng điều kiện |
| Completed + Paid hợp lệ | List không có plan, detail403 | List có plan và detail200, kể cả endDate của plan đã qua |
| Paid nhưng Refund đã Completed | Detail chặn; list Registered có thể còn lộ shared plan | Cả hai cùng loại đúng invoice có Refund Completed; Invoice vẫn giữ Paid/amount gốc |
| Sau transfer | Invoice.class_id giữ lớp gốc, entitlement_class_id trỏ lớp nhận | Dùng lớp hiệu lực: quyền Registered/Completed tại lớp nhận; không dùng Paid của Invoice gốc để mở lại lớp nguồn |
| Cancelled/Pending/Waitlisted hoặc thiếu quyền Paid | Không thuộc quyền học Registered hợp lệ | Không shared read: list200 không chứa plan, detail403; không thiết kế thêm hủy/hoàn sau Ongoing |
| Own private plan/result/attendance/evaluation | Có scope lịch sử riêng | Giữ scope hiện có, không yêu cầu hồi sinh quyền học để đọc own history |

FE online hiện hành là https://scms-demo-04nov.pages.dev/; repo/build/API origin chưa xác minh. Đây là bàn giao BE local, chưa chứng nhận FE/Render/browser hoặc demo Owner.

## 2. Quá trình và quyết định giữ/bỏ

1. Rà source xác nhận list thiếu guard Refund Completed so với helper Invoice của detail, đồng thời cả hai chỉ xét Registered.
2. Owner chốt Cancelled ngoài shared read sau ví dụ legacy/demo từng Paid và có kết quả; giữ own history và không mở policy hủy/hoàn mới.
3. Thêm Completed ở hai predicate và loại Refund Completed đúng invoice trong list, tái dùng helper Paid/effective class hiện có ở detail.
4. Giữ Invoice/Enrollment/transfer/refund/write RP-36/own-author Coach; bổ sung metadata nullable đúng payload đã có, không thêm field/bảng, nhân bản plan hoặc snapshot/versioning giáo án.
5. Kiểm list/detail cùng scope và DB/audit không đổi sau commit; đối chứng lỗi count fixture evaluation được sửa thành kiểm owner/current record trong lịch sử tích lũy, không đổi expected nghiệp vụ.

## 3. API và DB liên quan

Giữ nguyên method/path/query/response:

- GET `/api/training-plans?coachId=...&classId=...&memberId=...` — filter optional và AND; MemberId phía BE bị ép theo phiên, gửi Member khác403 dù có thêm authority quản trị.
- GET `/api/training-plans/{id}` — plan tồn tại nhưng ngoài quyền403; ID không tồn tại404. Thiếu phiên401; các cổng account/session vẫn theo contract hiện có.

Shared plan có `memberId:null`, `classId` của lớp và `coachId` tác giả. Quyền của Member đến từ enrollment/quyền Paid của lớp, không bắt plan.memberId trực tiếp bằng auth-me.userId. Own private plan vẫn có memberId của chủ; filter classId chỉ giữ plan gắn đúng lớp nên không dùng list lọc lớp để kết luận private history đã mất.

OpenAPI khai nullable cho classId/className (private plan), memberId/memberName (shared plan) và content/goal/startDate/endDate tùy chọn. Đây là sửa metadata khớp payload null đã tồn tại, không thêm field hoặc đổi giá trị trả về. Nếu FE sinh client từ OpenAPI, dùng snapshot mới và xử lý null; không ép các field này thành số/chuỗi/ngày bắt buộc.

Điều kiện shared read đồng thời: Active Member/đúng center theo scope hiện có; lớp Normal; enrollment Registered hoặc Completed; có Invoice cùng Member, Paid, reconciliationRequired:false, cashConfirmationReversed:false, không có PaymentResolution Refund Completed trên invoice đó; `coalesce(entitlement_class_id,class_id)` đúng lớp đang đọc. Không dùng chỉ chữ Paid hoặc Invoice.class_id gốc. Private plan giữ nhánh own riêng; Coach cũ chỉ giữ own-author read trong scope hiện có, không có quyền sửa/đọc mọi roster của Coach khác.

Nội dung trả về là **bản plan đang lưu**, không hứa bản đúng tại thời điểm Member học; startDate/endDate là thông tin giáo án, không tự khóa quyền đọc lịch sử. Không có API tạo snapshot hoặc khôi phục phiên bản mới. Member Completed được thêm quyền đọc, không thêm POST/PUT/DELETE chuyên môn, quyền đặt lịch/credit hoặc quyền tiền.

Source logic đổi ở TrainingPlanRepository.searchMemberPlans và TrainingAccessPolicy.memberCanReadClassPlan; controller mô tả hai GET và TrainingPlanResponse khai nullable cho field vốn tùy chọn. Không đổi schema DB, chứng từ hoặc trạng thái dữ liệu để tạo quyền. GET được kiểm không ghi plan/history/Invoice/Enrollment/audit.

## 4. FE cần làm

1. Rà client filter/dropdown đang chỉ lấy Registered, lớp Ongoing hoặc plan chưa qua endDate: cho phép hiển thị lịch sử Completed mà API trả đúng quyền. Không tự cấp quyền từ Invoice Paid ở client.
2. Phân biệt shared plan `memberId:null/classId!=null` với private plan có memberId; không lọc mất shared plan chỉ vì memberId không bằng userId hoặc tạo bản sao plan cho từng Member.
3. Dùng GET list đúng classId, lấy planId từ response rồi mở detail cùng phiên; gắn loading/cache/response với planId/classId, không hiển thị dữ liệu buổi/lớp cũ khi đổi lựa chọn.
4. Nếu detail403 sau list đã từng có plan, tải lại list/phiên và giải thích quyền hiện hành đã thay đổi; không lấy cache/list cũ làm quyền truy cập. List200 `[]` đúng trong scope bị loại, không phải lỗi đăng nhập.
5. Giữ nút ghi theo RP-36; quyền đọc Completed không làm Member thành Coach hoặc cấp Coach mới quyền sửa plan tác giả cũ. Bố trí giao diện và câu chữ do FE quyết định.

**Demo ngắn:** [đăng nhập A→list/detail→Cancelled/private→đổi B kiểm scope](../docs/02-DEPLOYMENT-GUIDE.md#local-shared-plan-history-demo-20261112) tại <http://127.0.0.1:18086/swagger-ui/index.html>. Member A2/Completed class1/plan1 đọc200 ở cả hai GET; Cancelled class3/plan3 bị loại, private plan4 còn đọc được. Member B3 chỉ có quyền class2, không đọc plan1. Không có cửa giờ chờ. Credential/cookie chỉ ở file private bị Git bỏ qua, không chuyển sang FE online hoặc báo cáo. Runtime dùng Clock thực/scheduler tắt trong harness; buildCommit unknown được đối chiếu source, không là SHA deploy. Owner đã chọn skip riêng demo2.3; giữ API readiness, không nhận Owner/browser PASS.

## 5. Việc còn lại

Owner đã chọn “Skip demo 2.3, đóng Session 2”. Session2 đã đóng trong phạm vi BE local/bàn giao và demo SKIPPED_BY_OWNER; không còn checkpoint demo2.3 chờ. FE/browser/Render chưa nghiệm thu; Session3 chưa bắt đầu và chỉ mở khi Owner yêu cầu riêng.

Checker contract tổng thể vẫn vướng helper tham chiếu schema root đã bị xóa từ trước; không khôi phục deletion hoặc đổi fingerprint lịch sử trong mục này. Kiểm tập trung hai GET/schema/HTTP/commit được ghi riêng, không nhận PASS cho checker tổng thể chưa chạy xong.
