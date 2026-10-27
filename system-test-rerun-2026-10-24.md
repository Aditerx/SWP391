# SCMS — báo cáo kiểm thử lại FE với API Render ngày 24/10/2026

## Kết luận để bàn giao

**Chưa thể nghiệm thu toàn bộ hệ thống.** Lượt này đã chạy 5 role qua API Render bằng đăng nhập và GET chỉ đọc, chạy các test FE và browser loopback. Các luồng ghi dữ liệu thật như tạo lớp, đăng ký, đặt ca 1-1, thanh toán, điểm danh và sửa Holiday **chưa được kiểm thử đầu cuối** vì Render đang là môi trường chung, chưa có staging/DB cô lập cùng fixture/reset được BE xác nhận. Không diễn giải các test mock thành PASS trên API thật.

Tại `2026-10-24T08:52:01Z` (15:52 giờ Việt Nam), probe 51 bước có **48 PASS, 1 FAIL, 2 quyền cần chốt**. `GET /api/coaches` trả **500**; probe bổ sung lúc `2026-10-24T08:54:25Z` xác nhận 500 ở cả Manager và Member. `GET /api/sessions` và Manager Dashboard đã trả 200, nên F-06/F-07 cũ không tái hiện trong lượt này. Đúng alias ca rảnh FE dùng, `GET /api/sessions/coaches/{id}/available-slots`, đã trả 200.

Ảnh DevTools của Owner lúc `2026-10-24T08:41:22Z` cho thấy `POST /api/invoices/3/pay-online` trả **409**, body `"Invoice is not payable online"`; không có `paymentUrl`. Đọc lại hóa đơn demo ID 3 bằng GET lúc 08:54Z: `paymentStatus=Pending`, `paymentMethod=null`, `expiresAt=null`. Đây là hóa đơn chưa đủ điều kiện VNPay theo [InvoiceService trên BE main](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/src/main/java/com/sportscenter/invoice/InvoiceService.java#L167-L175). `expiresAt=null` không chứng minh đã hết hạn; điều kiện `paymentMethod` đã đủ giải thích 409 nếu bản deploy dùng cùng logic. BE cần xác nhận bản deploy/DB và nguồn gốc hóa đơn này.

## Phạm vi và bằng chứng

- FE: `D:\School\FA26\SWP391\Project\src-code\FE_Project`, branch `PhatTai`, HEAD `9ac28a97c6c995ac083fd4d4e353264b694337c9`, working tree sạch; `origin/main` cục bộ `8842fcfaec9ab6abec0add7ce42f93189453db18`. Không sửa mã ứng dụng.
- BE GitHub `main` khi tra lại: `86d00765a574e1193c6b0042176acf7b26ca14ce`. API live [Swagger](https://scms-app-p1gy.onrender.com/swagger-ui/index.html#/) có 121 path tại thời điểm probe. **Chưa có SHA deploy Render** để chứng minh code đang chạy đúng bằng BE main.
- [Probe 5 role, không lưu body/cookie](./evidence/render-readonly-repeat-20261024-1550.json); [probe bổ sung Coach, alias ca rảnh, hóa đơn](./evidence/render-targeted-readonly-20261024-1605.json). File đặt tên theo lượt chạy; dùng `checkedAt` trong JSON làm thời điểm chuẩn.
- Script: [probe 5 role](./probe-scms-readonly.mjs), [probe bổ sung](./probe-targeted-readonly.mjs). Chỉ `POST /api/auth/login` và GET; không gửi mutation nghiệp vụ. Tài khoản demo đọc trong tiến trình, không ghi vào evidence.
- [Tổng hợp 14 unit/logic test](./evidence/rerun-20261024-1600/summary.json) và log tương ứng trong cùng thư mục. Bằng chứng `409` là hai ảnh DevTools Owner gửi, **không phải POST do lượt kiểm thử này lặp lại**.

## Kết quả API thật theo role

| Vai trò | Đã xác nhận bằng GET | Vấn đề/giới hạn |
|---|---|---|
| Chưa đăng nhập | Health và OpenAPI 200; Sessions và Dashboard 401 | Đúng chặn xác thực cho hai route đã thử. |
| Admin | `/api/users` 200 (28), roles 200 (5), permissions 200 (18), audit logs 200 (4) | Dashboard 200; Owner/BE cần chốt quyền xem Dashboard trung tâm. |
| Manager | Classes 200 (15), members 200 (5), subjects 200 (18), rooms 200 (22), Holidays 200 (18), time slots 200 (7), packages 200 (6), staff 200 (22), sessions 200 (2), invoices và 3 reports 200 | **`/api/coaches` 500**. Chưa thử tạo lớp hoặc sửa dữ liệu. |
| Receptionist | Classes, sessions, invoices 200; Dashboard 403 | Chưa thử thu tiền mặt, tạo hóa đơn, điểm danh. |
| Member | Classes, public subjects/packages, own invoices, own packages 200; global invoices/Dashboard 403. Alias ca rảnh FE dùng 200 | **`/api/coaches` 500**. Sessions list rỗng của tài khoản demo nên chưa xác nhận lọc chủ sở hữu. Chưa đặt ca/đăng ký. |
| Coach | Classes và sessions 200; global invoices 403 | Sessions rỗng nên chưa xác nhận lọc ca được phân công. Dashboard 200 cần chốt policy. Chưa điểm danh/báo vắng. |

## Lỗi và việc cần xử lý

### P1 — BE: `/api/coaches` trả 500 cho Manager và Member

- **Tái hiện:** GET có cookie hợp lệ, cả hai role đều 500; trong cùng lượt Manager `/api/staff` và `/api/sessions` 200. Có thể làm hỏng danh sách HLV ở màn tạo lớp Normal và đặt lịch Advanced.
- **Sở hữu:** BE. Chưa có stack trace; không khẳng định do query, mapper, specialization hay DB. [CoachController trên BE main](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/src/main/java/com/sportscenter/specialization/CoachController.java#L43-L69) đọc coach kèm specialization nên đây là những chỗ cần BE kiểm log, không phải nguyên nhân đã chứng minh.
- **Đề nghị BE:** tra log Render quanh 08:52–08:54 UTC ngày 24/10, gửi stack trace đã ẩn dữ liệu cá nhân, SHA deploy và bản sửa. Sau deploy, trả 200 cùng danh sách HLV có ID/specialization/status cho Manager và Member; FE xác nhận màn chọn HLV bằng browser API thật.

### P1 — BE fixture/data + FE UI: hóa đơn demo `Pending` nhưng không thanh toán VNPay được

- **Tái hiện:** Owner bấm “Thanh toán” cho mã hiển thị `INV-2026-000046`; request dùng **ID nội bộ 3** và nhận 409 `Invoice is not payable online`. GET của chính Member sau đó vẫn thấy ID 3 `Pending`, `paymentMethod=null`, `expiresAt=null`, tạo lúc `2026-10-22T19:30:44` theo LocalDateTime BE. Mã hiển thị và ID nội bộ là hai trường khác nhau.
- **Phân tích:** [BE `payOnline`](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/src/main/java/com/sportscenter/invoice/InvoiceService.java#L167-L175) yêu cầu `VNPay` + `Pending` + chưa hết hạn. `expired()` ở BE chỉ trả true khi `expiresAt` khác null và đã qua thời điểm, nên **không kết luận 409 do hết 5 phút** từ dữ liệu hiện có. `paymentMethod=null` là dấu hiệu trực tiếp cần BE giải thích. Chưa xác minh SHA deploy nên đây là đối chiếu với BE main, không phải trace server của request 409.
- **Sở hữu BE:** xác nhận invoice ID 3 thuộc fixture loại nào, vì sao `paymentMethod`/`expiresAt` null mà status vẫn Pending; sửa seed/migration/trạng thái nếu đây là hóa đơn demo online. Cung cấp hóa đơn VNPay mới trên staging cô lập còn hiệu lực và read-back trước/sau callback Sandbox. Trả lỗi 409 có `code`/message rõ nguyên nhân nếu hợp đồng chốt.
- **Sở hữu FE:** [MemberInvoicesView](../../FE_Project/src/components/views/MemberInvoicesView.jsx) hiện hiện nút Pay chỉ theo `paymentStatus=pending`, không xét `paymentMethod`/`expiresAt`; khi 409 thì chỉ hiện message tiếng Anh từ BE và không tải lại hóa đơn. Cần khóa nút khi dữ liệu không chứng minh payable, hiển thị lý do tiếng Việt, refresh GET sau 409. Đồng bộ chốt an toàn Render với các màn thanh toán khác trước khi mở mutation live.

### P1 — BE source/schema/deploy chưa có bằng chứng đồng bộ

- **Tái kiểm tra GitHub main:** thư mục `database/migrations` dừng tại `06_schedule_safety_*`; chưa có migration 07. Cả [schema gốc](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/schema_postgres.sql#L297-L331) và [schema database](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/database/schema_postgres.sql#L297-L331) chưa có `sessions.subscription_id` trong bảng sessions. GET sessions 200 **không chứng minh** repo, deploy và DB nhất quán.
- **Sở hữu:** BE. Gửi SHA commit chứa migration/schema, SHA deploy Render, DB đích và xác nhận cột/FK/index đang có. Chưa chạy migration từ FE.

### P2 — FE: Dashboard Member dùng QR sai thời hạn và hiển thị QR giả khi API lỗi

- **Bằng chứng mã:** [MemberDashboard](../../FE_Project/src/components/views/MemberDashboard.jsx) đọc `validitySeconds || 60`, trong khi contract BE và [MemberCheckinView](../../FE_Project/src/components/views/MemberCheckinView.jsx) dùng `expiresInSeconds` (30 giây). Khi GET mã lỗi, Dashboard đặt chuỗi `SCMS-...` và vẫn render QR; mã này chưa được BE cấp.
- **Sở hữu:** FE. Dùng đúng `payload`/`expiresInSeconds`, không render QR dùng được khi GET lỗi, cho người dùng thử lại. **Chưa thử quét mã thật**, nên mức ảnh hưởng vận hành cần kiểm tra trên staging.

### Policy — quyền Dashboard Coach/Admin

- Admin và Coach demo nhận 200, Receptionist/Member 403. Chưa kết luận đây là bug: quyền có thể dựa trên authority đã cấp. Owner/BE chốt ma trận quyền và seed role; chạy lại bằng role chuẩn sau khi chốt.

## Kết quả FE tự động và browser

| Nhóm | Kết quả | Diễn giải |
|---|---|---|
| 14 test Node/logic đã chọn | 12 PASS, `g1_guard_test` và `g2_logic_test` FAIL | Hai lỗi này cũng xuất hiện ở lượt cũ. `g1_guard` kỳ vọng `code=UNAUTHORIZED` dù fixture không có code, còn FE `createApiError` giữ `code=null`. `g2_logic` stub URL không khớp URL tuyệt đối của httpClient. Cần sửa **test/contract**, chưa chứng minh sản phẩm lỗi. |
| FE10 full Browser | Unit PASS, FE10-BROWSER-01/02/03 PASS | Chromium/CDP + API loopback giả lập; xác nhận quản lý requestId và tạo 12 sessions ở UI giả lập, **không xác nhận POST tới Render**. |
| FE2 PaymentModal Browser | 11/13 PASS; 2 FAIL | Case `BANK_TRANSFER` kỳ vọng payload `BANK_TRANSFER` nhưng code hiện gửi `VNPAY` cho thanh toán phi tiền mặt; contract hiện mô tả VNPay Sandbox. Case Pending đòi text “chưa có gói” nhưng UI hiện có trạng thái “gói đang chờ thanh toán”, không gắn badge Active. Cần cập nhật kỳ vọng sau khi Owner/BE chốt phương thức. |
| FE2 mock UI matrix | 8 case PASS, MOCK-UI-05 FAIL, sau đó timeout ở luồng tiếp | MOCK-UI-05 tìm invoice trong bảng ở trang tổng quan thay vì tab lịch sử hiện tại; timeout ở bước mở/gửi yêu cầu gói trong mock. Cần rà lại selector, fixture và UI thực tế; chưa coi là lỗi live. Các case sau timeout chưa chạy. |
| `npm run build`, `npm run build:local`, `git diff --check` | PASS khi chạy build **tuần tự** | Lần chạy hai build đồng thời làm `dist/assets` bị `EPERM`; đây là tranh chấp output, không phải lỗi biên dịch. |

Không chạy `g4_logic_test.mjs` vì script có lời gọi PATCH chưa mock, từng có rủi ro chạm Render. Không chạy `live_acceptance.mjs` và các thao tác ghi trong browser thật trên Render. Đây là giới hạn an toàn của bằng chứng, không phải kết quả PASS.

## Thứ tự xử lý và điều kiện nghiệm thu tiếp

1. **BE** sửa 500 `/api/coaches`; giải thích/sửa hóa đơn demo ID 3, cung cấp fixture VNPay payable mới; bàn giao SHA source/deploy và DB/migration. **Owner + BE** chốt quyền Dashboard Coach/Admin.
2. **FE** sửa điều kiện hiển thị/khả dụng nút Pay, refresh trên 409 và QR Dashboard; cập nhật các test cũ có kỳ vọng lỗi thời. Không tự đổi trạng thái hóa đơn trên FE để che lỗi dữ liệu BE.
3. **BE cấp staging/DB cô lập** và tài khoản/fixture có cách reset. Sau đó chạy browser + API thật theo [hướng dẫn test tay](./manual-test-guide.md): tạo lớp Normal 12 buổi và read-back, đăng ký lớp, thanh toán VNPay Sandbox/callback, đặt/hủy ca Advanced và credit, thu tiền quầy, điểm danh/báo vắng, Holiday, role/owner scope, loading/error/retry. Với mỗi mutation phải đọc lại trạng thái và đối soát số lượng/credit, không chỉ nhìn toast.
4. Sau kết quả staging, cập nhật báo cáo này bằng status từng case và evidence đã ẩn credential. Đến lúc đó mới có thể kết luận “toàn bộ luồng và role PASS”.

## Câu hỏi BE cần trả lời

- SHA deploy Render hiện tại là gì, có tương ứng BE main `86d00765` không? Migration 07 và `sessions.subscription_id` đã áp dụng ở DB nào?
- Stack trace thực tế cho `/api/coaches` tại 08:52–08:54 UTC là gì?
- Hóa đơn ID 3 thuộc loại thanh toán nào? Vì sao `Pending` nhưng `paymentMethod` và `expiresAt` null? Có cần chuyển sang Expired/Cancelled hoặc tạo lại fixture VNPay mới không?
- Coach/Admin có được xem Dashboard trung tâm không? Bộ role demo hiện có đúng policy cuối cùng không?
