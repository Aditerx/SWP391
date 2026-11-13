# Bàn giao FE — đối chiếu matrix và readiness

Đối tượng: FE nghiệm thu bản BE cập nhật và Owner review. Ngày13/11/2026. Nguồn kết quả duy nhất: [Active](../docs/00-MASTER-CHECKLIST.md#active), QA `session33Readiness20261113`; demo **SKIPPED_BY_OWNER**.

## 1. Kết quả hiện tại

Các luồng Session1–3 có bản bàn giao riêng và [matrix theo rủi ro](../docs/03-TODO.md#session33-risk-matrix-20261113). Bổ sung kiểm quyền approve Refund đủ năm role/authority/center, notification theo owner và timeout phiên container local. BE runtime không đổi trong3.3; không thêm endpoint/schema hoặc thay tài khoản/bundle. Readiness chỉ áp dụng phạm vi BE local có evidence; FE/browser/Render/cloud chưa nghiệm thu.

## 2. Quá trình và quyết định giữ/bỏ

- Giữ kết quả Session1–2 đúng source/case, chạy hồi quy hiện hành và không tích hợp FE/cloud hoặc làm lại demo đã skip.
- Lấp gap tiền/quyền/owner/phiên bằng fixture mới và login/auth-me, response cùng readback sau commit; không dùng anonymous401 làm pass nghiệp vụ.
- Xác minh checker bị dừng vì helper đọc schema root đã xóa; ghi rõ file vắng trong fingerprint mới, giữ lịch sử và deletion, kiểm riêng source/hash thay vì bỏ qua gate.
- Giữ các ô chưa có bằng chứng là NOT_RUN và phân biệt checker route/type/property với nghiệm thu business hoặc browser.

## 3. API và DB liên quan

| Nhóm đối chiếu | Kết luận cần dùng khi tích hợp |
|---|---|
| POST `/api/normal-refunds/{id}/approve` | Chỉ CenterManager Active+MANAGE_INVOICES cùng center, đúng version/receiver/eligibility. Admin dù thêm authority, Receptionist/Coach/Member hoặc Manager thiếu authority đều không được approve. Approved chưa Completed. |
| GET `/api/notifications/me`, `/me/unread-count`; PATCH `/{id}/read`, `/me/read-all` | Mọi role Active đọc/sửa trạng thái đọc của chính mình; foreign ID404 NOTIFICATION_NOT_FOUND. Notification dùng chung ID vẫn có trạng thái đọc riêng từng recipient; read-all không đọc hộ người khác. |
| Login/auth-me và protected API | Phiên mới phải xác minh auth-me. Timeout container local dẫn401 và đăng nhập lại; không xác nhận cookie/proxy/browser của môi trường deploy từ kết quả này. |
| Các luồng Session1–3 còn lại | Tra bản riêng trong [KIEM-TRA](KIEM-TRA.md), contract và OpenAPI; không áp rule của route này sang route khác hoặc suy một GET200 thành ghi thành công. |

DB kiểm là PostgreSQL local mới, dữ liệu synthetic/DEMO, không tài khoản hạ tầng. Notification có direct row và recipient row; Refund giữ chứng từ gốc và audit. H2 fixture bổ sung unique dedupe giống constraint PG hiện có; không sửa DB canonical hoặc dữ liệu cloud. Chi tiết nguồn/môi trường/giới hạn xem Active/QA.

## 4. FE cần làm

1. Xác định build/API origin trước kiểm; login mới và auth-me đúng account, role, authority. Không lấy ID/phiên/cửa giờ trong checkpoint cũ làm precondition hiện tại.
2. Đối chiếu approve Refund theo role+authority và đọc lại status/version; không mở quyền tài chính cho Admin chỉ vì có authority hoặc coi approve là đã chuyển tiền.
3. Khi PATCH notification204, tải lại list/count trong cùng phiên; cache trạng thái đọc theo user và notification, không chỉ notificationId dùng chung.
4. Ghi kết quả FE riêng theo từng bản, kèm readback sau ghi; các ô FE đang trống trong KIEM-TRA không được đánh xanh từ test local hoặc demo skip.

## 5. Việc còn lại

FE/browser/build/API origin, Render/cloud/live smoke/provider và các method/case chưa có evidence riêng tiếp tục NOT_RUN theo matrix. Không có quyền tích hợp/deploy/reset từ bản bàn giao này. Owner tự đóng gói ZIP; thư mục nguồn không chứa credential, cookie, file private hay raw runs. Kết quả checker offline và readiness cuối được chốt ở Active/QA, không sao chép count/SHA vào bản này.
