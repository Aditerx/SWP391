# SCMS — Bàn giao FE RBAC lần1 — 06/11/2026

Đối tượng: FE tích hợp cổng Member với API BE. Phạm vi: mục0 và0.1 trong [checklist RBAC](docs/03-TODO.md); SQL16 đã được Owner chạy thành công trên Supabase, chưa nghiệm thu toàn bộ BE-01–BE-07. Bản này ghi yêu cầu tích hợp/câu chữ; contract chính thức ở docs01.

## 1. Kết quả hiện tại

| Vấn đề | BE đã xử lý / phạm vi |
|---|---|
| Member đọc training-results bị403 | Đã cho Member Active đọc kết quả của mình, chặn ID người khác; giữ quyền ghi. Đã kiểm local. |
| Member gọi payment-resolutions bị403 | Giữ403 đúng quyền. Đã rà năm role; API dành nhân viên có quyền và đúng center, Admin không tự có quyền toàn hệ thống. |
| Normal bị PACKAGE_EXPIRED vì chưa có gói | Đã bỏ điều kiện gói chung khi đăng ký/xét danh sách chờ Normal; giữ kiểm tài khoản, owner/center, lịch/sĩ số và thanh toán. Đã kiểm local. |
| Lớp Normal giá0 có thể vào Registered | Đã chặn giá thiếu/≤0 ở đăng ký/promotion; giá hợp lệ phải Pending trước Paid. Test đích và clean đã đạt local; chứng từ Paid trước đó không bị sửa. |
| Advanced đi qua tuyến enroll chung | Đã chặn bằng400 USE_ADVANCED_REGISTRATION; hai tuyến enroll chỉ nhận Normal, không tự thăng waitlist Advanced cũ. |
| Mua/gia hạn gói hội viên chung cũ | BE đã đóng ghi mới/cập nhật gói chung bằng400 LEGACY_MEMBERSHIP_RETIRED. Owner gửi kết quả SQL16 PASS trên Supabase; DB không có dữ liệu cũ cần xóa/tháo liên kết. GET quyền Advanced giữ lại. |
| Normal mua sau Advanced chặn CashCorrection | Đã bỏ suy luận phụ thuộc theo thời gian. Normal có hóa đơn riêng, không liên kết quyền Advanced, dù Pending/Paid đều không chặn sửa xác nhận Cash nhầm. Giữ guard D16/liên kết thật; đã kiểm local H2/PostgreSQL. |

Kết quả/source/môi trường hiện hành tại [Active](docs/00-MASTER-CHECKLIST.md#active) và [QA](artifacts/qa/latest.json). Chưa deploy hoặc nghiệm thu lại FE/Render từ bản local; không coi ảnh lỗi cũ là kết quả của bản sửa.

## 2. Quá trình và quyết định giữ/bỏ

- Owner05/11 duyệt Member đọc kết quả cá nhân; bỏ chặn đọc thiếu quyền nhưng giữ guard chủ sở hữu và quyền ghi.
- Giữ payment-resolutions cho nhân viên; bỏ request gọi nhầm trên FE thay vì mở quyền BE để hết403.
- Owner06/11 duyệt bỏ điều kiện mua gói chung trước Normal; giữ khoản học phí và hóa đơn riêng của lớp, không cấp gói giả.
- Owner06/11 duyệt chặn nhánh giá Normal không hợp lệ; giữ chứng từ/ghi danh lịch sử, không tự sửa dữ liệu thật.
- Owner duyệt B và bỏ mô hình gói chung vì sản phẩm hiện tại chỉ bán khóa Normal/quyền Advanced; sau SQL16 PASS mới sửa A, bỏ chặn theo thời gian nhưng giữ phụ thuộc thật/D16. Ghi chú câu chữ ở bản này để FE quyết định, docs01 giữ contract chính thức.

## 3. API và DB liên quan

| Luồng | API / dữ liệu cần dùng |
|---|---|
| Kết quả cá nhân | GET `/api/training-results`; Member có thể bỏ memberId, BE ép theo phiên; ID người khác403. [Contract](docs/01-API-CONTRACT-FE-BE.md#attendance). |
| Hồ sơ xử lý tiền | Nhóm `/api/payment-resolutions`; kiểm role AND MANAGE_INVOICES. Member/Coach không được dùng. [Ma trận](docs/01-API-CONTRACT-FE-BE.md#resolutions). |
| CashCorrection của A | POST `/api/payment-resolutions/{id}/approve` →Approved/Blocked; POST `/api/payment-resolutions/{id}/complete` với evidence,moneyNotReceived:true →Completed nếu D16 còn hợp lệ. Approve chưa đảo quyền; Complete kiểm lại dưới lock, giữ Paid/amount gốc, hủy quyền Advanced chưa dùng và đánh dấu cashConfirmationReversed. Normal độc lập không đổi; retry không lặp hồ sơ/audit/thông báo. |
| Đăng ký Normal | POST `/api/classes/{classId}/enroll` hoặc POST `/api/enrollments`; paymentMethod=Cash/VNPay, Member lấy danh tính từ phiên. Pending có invoiceId/invoiceCode/amount/expiresAt/paymentUrl theo method; Paid mới Registered. Giá không hợp lệ400 INVALID_PRICE. [Contract](docs/01-API-CONTRACT-FE-BE.md#normal). |
| Gói / khóa | GET `/api/members/{memberId}/subscriptions` đọc gói; enrollment Normal là dữ liệu riêng. users/members là tài khoản/hồ sơ, không tự tạo member_packages sau verify OTP. |
| Danh mục cũ đã đóng ghi | POST `/api/packages`, PUT `/api/packages/{id}`, PATCH `/api/packages/{id}/status` →400 LEGACY_MEMBERSHIP_RETIRED khi request hợp lệ/đúng quyền. GET danh mục sau SQL16 trả mảng rỗng. |
| Mua gói chung cũ đã đóng | POST `/api/members/me/subscriptions`, `/api/member-packages/subscribe-me`, `/api/members/{memberId}/packages` hoặc `/api/members/{memberId}/subscriptions` →400 LEGACY_MEMBERSHIP_RETIRED. Không dùng để mua Advanced. |
| Gia hạn/đổi state cũ | POST `/api/subscriptions/{id}/extend`, PATCH `/api/subscriptions/{id}/status` và alias `member-packages`: gói chung bị từ chối; ID đã xóa có thể404. Advanced vẫn theo incident/workflow, không mở gia hạn tùy ý. |

Sửa quyền đọc, guard Normal và chặn tuyến enroll Advanced không đổi schema. Riêng bỏ mô hình gói chung dùng [SQL16](database/migrations/16_retire_legacy_memberships_postgres.sql), có kiểm tra trước/sau trong một transaction. Cách chạy/đối chiếu theo [docs02](docs/02-DEPLOYMENT-GUIDE.md#retire-legacy-memberships).

**Lý do bỏ:** gói chung tháng/VIP không có vai trò trong thiết kế hiện tại; guard cũ yêu cầu gói Active đã khiến Member vừa verify OTP không mua Normal được. BE bỏ điều kiện này và đóng các API ghi của mô hình cũ để lỗi không quay lại.

| SQL16 xử lý gì | Kết quả Supabase do Owner cung cấp |
|---|---|
| Xóa danh mục `membership_packages` và subscription gói chung cũ được phân loại chắc chắn | Không có bản ghi cần xóa. |
| Xóa hóa đơn cũ cùng payment attempt/resolution trực tiếp của chúng | Không có bản ghi cần xóa; không xóa hóa đơn Normal/Advanced hiện hành. |
| Tháo `package_id` cũ khỏi quyền Advanced/hóa đơn hiện hành khi đủ thông tin môn/Coach/chủ sở hữu | Không có liên kết cần tháo. |
| Cấm ghi lại catalog/subscription/invoice kiểu gói chung cũ bằng trigger | Đã cài theo transaction có kết quả `PASS`, `model_retired=true`. |
| Kiểm bảo toàn quyền học, số tiền, credit, lịch, ghi danh và attendance hiện hành; ghi marker migration | Preflight/postflight đạt. Marker chỉ lưu số lượng; không lưu bản sao dòng nghiệp vụ cũ. |

Tất cả bộ đếm xóa/tháo liên kết Owner gửi đều bằng0: **migration thành công nhưng thực tế không xóa dòng dữ liệu nào**. Giữ bảng catalog trống để ORM hiện có vẫn hoạt động; giữ `member_packages` vì bảng này lưu quyền Advanced. Agent không trực tiếp truy vấn Supabase; kết quả này không xác nhận BE mới đã deploy.

## 4. FE cần làm

1. **training-results:** cho Member tải kết quả cá nhân; có thể bỏ memberId. Không dùng ID nhập từ giao diện để xác định quyền.
2. **payment-resolutions:** không gọi ở đăng nhập, tải nền hoặc màn Member; không hiện chức năng xử lý hồ sơ nội bộ. Bỏ request ngoài quyền để hết thông báo Access Denied phát sinh; chỉ ẩn menu/thông báo mà request vẫn chạy là chưa đủ. Không tắt chung403 hoặc ẩn thông báo nghiệp vụ hợp lệ. Nhân viên chỉ tải khi đúng role và có MANAGE_INVOICES; approve/complete còn yêu cầu Manager/Admin.
3. **Normal:** không khóa đăng ký vì subscriptions rỗng. Gửi đăng ký, xử lý Pending/Waitlisted từ response; chỉ hiển thị đã ghi danh khi BE trả Registered. Khi400 INVALID_PRICE, thông báo lớp chưa có học phí hợp lệ, không tự đặt giá hoặc coi là lớp miễn phí.
4. **Câu “Thông tin sẽ xuất hiện tại đây sau khi Backend ghi nhận”:** ảnh trạng thái trống khiến tài khoản vừa verify OTP có thể hiểu phải chờ nhân viên cấp/duyệt gói. Thực tế chưa mua thì chưa có gói; Normal được mua riêng. Đề xuất mô tả rõ chưa có đăng ký Advanced và dẫn đến luồng mua khóa; chỉ nói chờ thanh toán/Coach nhận khi API có bản ghi/trạng thái tương ứng. **FE quyết định câu chữ, vị trí và cách trình bày.**
5. **Tên “Gói hội viên”:** không dùng để giới thiệu sản phẩm tháng/VIP hoặc điều kiện vào khóa đã bỏ. `MemberPackageResponse`/subscriptions hiện còn là tên kỹ thuật cho quyền Advanced, không phải một sản phẩm hội viên mới. Đề xuất dùng “Đăng ký Advanced/Quyền học Advanced” và giữ “Khóa Normal đã đăng ký” riêng; FE quyết định nhãn giao diện. SQL16 đã thành công theo kết quả Owner; phần đóng API/guard BE còn cần deploy.
6. **Tuyến cũ đã đóng:** không mua Advanced qua `/classes/{id}/enroll` hoặc `/enrollments`; dùng advanced-registrations theo [contract](docs/01-API-CONTRACT-FE-BE.md#advanced). Bỏ thao tác mua/gia hạn/danh mục gói chung; tạo Member tại quầy không gửi packageId. Request hợp lệ/đúng quyền vào lối gói chung cũ trả400 LEGACY_MEMBERSHIP_RETIRED; không xử lý như chờ nhân viên duyệt gói. GET subscriptions để đọc quyền Advanced vẫn dùng được.

## 5. Việc còn lại

- BE local đã kiểm H2/PostgreSQL theo Active/QA; còn deploy và kiểm FE/Render trong phạm vi phù hợp. Chưa nghiệm thu gateway live cho bản sửa này.
- GET catalog/17 trả404 là nhánh riêng với POST enroll lớp19; cần response body và classType/status của lớp17 để kết luận.
- SQL16 đã PASS trên Supabase theo bảng kết quả Owner gửi; không cần chạy lại để cố tạo số lượng xóa. Chưa deploy BE hoặc nghiệm thu lại FE/Render. Không chạy lại schema gộp/seed để dọn dữ liệu.
- A chỉ sửa điều kiện BE, không cần SQL mới. FE nhân viên đọc status Approved/Blocked/Completed của hồ sơ; không coi HTTP200 hoặc Approved là đã hoàn tất điều chỉnh. Member vẫn không gọi nhóm API này.

**Phần có thể bàn giao trước từ audit:** bảng dưới đối chiếu đúng phạm vi0/0.1 và workflow liên quan đã kiểm; chưa đóng trọn BE-01–BE-07.

| Mục audit | Có thể dùng trước / phần còn thiếu |
|---|---|
| BE-01 — Ma trận quyền | Bàn giao quyền `training-results`, bốn thao tác `payment-resolutions` và policy confirm-cash theo docs01. Chưa có ma trận toàn bộ API/năm role. |
| BE-02 — Phiên/401 | Các test local dùng login thật và kiểm auth/me trước nghiệp vụ. Chưa đủ để kết luận cookie/proxy, hết hạn phiên hay lỗi401 cũ trên FE/Render đã hết. |
| BE-03 — Coach | Chưa đóng: cần kiểm Coach được giao/không được giao trên từng nhóm, nhất là cả hai tuyến GET enrollment theo lớp. B không thay policy đọc của Coach. |
| BE-04 — Member/IDOR | Bàn giao riêng GET training-results: bỏ filter vẫn chỉ mình, đổi memberId bị403, thêm filter/authority không mở rộng phạm vi. Kiểm chặn gửi memberId khác khi enroll Normal và đọc chéo invoice/subscription trong workflow là bằng chứng bổ sung; chưa bao phủ mọi API. |
| BE-05 — Xác nhận Cash | Policy hiện hành: chỉ Receptionist+MANAGE_INVOICES, cùng center, hóa đơn Cash Pending còn hạn/đủ điều kiện kích hoạt. Có workflow kiểm Paid/quyền học/số tiền/retry/race và Manager bị403; chưa đủ ma trận riêng năm role. CashCorrection do Manager duyệt/hoàn tất là thao tác khác, không mở quyền confirm-cash cho Manager. |
| BE-06 — Report/audit | Workflow có hồi quy báo cáo tiền liên quan CashCorrection; không thay bằng chứng quyền audit-log/filter/phân trang, cần lượt riêng. |
| BE-07 — Lỗi401/403 | Bàn giao các lỗi cụ thể đã nêu trong bản này; chưa chuẩn hóa toàn bộ operation OpenAPI hoặc nghiệm thu correlation/request ID. |
