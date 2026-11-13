# SCMS — Bàn giao FE: Refund Normal

Dành cho FE; ngày10/11/2026. Hoàn100% trước khai giảng khi TT hủy/không cung cấp phương án được Member nhận; kiểm local, chưa FE/Render/ngân hàng thật. Bằng chứng chuẩn tại [Active/QA](docs/00-MASTER-CHECKLIST.md#active).

## 1. Kết quả hiện tại

TT hủy chủ động tạo nghĩa vụ cho từng khoản Paid; Member từ chối proposal chọn Refund thì mở hồ sơ. Pending chưa thu không được hoàn. Manager xác minh người nhận/duyệt →Receptionist claim/chuyển ngoài SCMS/nộp chứng từ →Manager đối chiếu nguồn ngân hàng độc lập mới Completed. Invoice Paid/amount giữ nguyên; rút quyền cũ, không vừa chuyển lớp vừa hoàn cùng quyền.

Có receiver theo phiên bản, attempt/claim chống trùng, chứng từ BYTEA riêng tư PNG/JPEG/PDF, lịch sử từng lần xác minh, tiếp nhận khiếu nại và SLA07ngày TT mở. Ngày mở tiếp theo là ngày1; tính T7/CN trừ Holiday. Upload/Approved không là đã hoàn; thông tin thiếu/sự cố không tự hết nghĩa vụ.

## 2. Quá trình và quyết định giữ/bỏ

- Tái dùng payment_resolutions cho khoản Refund/report, giữ D10 tiền đến muộn chưa cấp quyền và D16 CashCorrection Advanced.
- Thêm receiver/attempt/document/verification riêng, khóa center rồi invoice để claim/retry không tạo hai công việc ngân hàng.
- Bỏ đường complete cũ đối với hồ sơ mới; giữ từng chứng từ/nguồn đối soát, không dùng avatar/public URL hoặc ảnh thay xác minh ngân hàng.
- Dữ liệu ngân hàng trong fixture đều DEMO, PDF riêng tư có cảnh báo; không chuyển tiền/provider thật.

## 3. API và DB liên quan

[Contract](docs/01-API-CONTRACT-FE-BE.md#normal-refunds-20261110), [OpenAPI](artifacts/fe-handoff/openapi.json), [bundle/vận hành](docs/02-DEPLOYMENT-GUIDE.md#demo-rebuild-20261109).

Group /api/normal-refunds: GET list/detail; Member POST /{id}/receiver (bank/accountNumber/accountName/version), /{id}/issues (version/note). Manager POST verify-receiver (version/source), approve (version), request-information (version/note); GET history chỉ Manager trả receiver và nguồn đối soát cũ.

Receptionist POST /{id}/claim (version/requestId UUID) →attempt; POST /{id}/attempts/{attempt}/submit (reference/amount/currency:VND/performedAt); upload multipart file tại /documents, tối đa1MiB theo multipart HTTP mặc định hiện hành. GET /{id}/documents/{document} chỉ staff tài chính đúng center, bytes attachment/no-store/private/nosniff. Manager POST /{id}/attempts/{attempt}/verify: version/outcome/bankSource/note; Success kèm exact reference/amount/receiverId/performedAt, có proof, mới Completed. Pending→NeedsReview; Failed/Reversed phải duyệt lại trước attempt mới.

## 4. FE cần làm

Member chỉ đọc hồ sơ của mình, tài khoản nhận che phần đầu; hiển thị trung thực AwaitingReceiver/AwaitingApproval/Approved/Processing/AwaitingVerification/NeedsReview/Completed. contactPhone chỉ staff từ hồ sơ Member, FE quyết định có dùng; không đặt vào chứng từ/log/thông báo chung hoặc dùng thay xác minh.

Receptionist chỉ nhận việc đã duyệt, giữ requestId khi retry claim, không tự chuyển lại khi bank chưa rõ. Upload không tự đổi UI thành Completed. Manager cần nhập nguồn độc lập và dữ liệu đã đối chiếu; sai amount/receiver/reference/time không hoàn tất.409 tải lại version;403 xử lý quyền, không gọi complete cũ làm fallback. Download dùng phiên, không nhúng URL public. Khi Member báo chưa nhận tiền, gửi issues và để Manager tra history.

## 5. Việc còn lại

Owner review local/main và duyệt phát hành. FE nhận contract, kiểm browser theo build/phiên thật sau triển khai; chuyển khoản ngân hàng thật/FE/Render chưa nghiệm thu. Ngoại lệ sau khai giảng và Member tự hủy lớp vẫn vận hành chưa mở policy; không dùng Refund này cho Advanced. SYSTEM-DATA giữ DATA DESIGN dù fixture đã kiểm local.
