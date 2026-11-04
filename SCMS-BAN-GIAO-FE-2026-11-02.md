# SCMS — Bàn giao FE ngày 02/11/2026

Tài liệu này tổng hợp thay đổi và kết luận sau hai báo cáo 29–31/10; FE dùng trực tiếp cho đợt tích hợp room, giới hạn lớp và báo vắng. Bổ sung QueryDR04/11 tại [mục7](#querydr).

## 1. Trạng thái bàn giao

| Hạng mục | Trạng thái |
|---|---|
| Supabase | Owner xác nhận đã chạy thành công cả `0211-1.sql` và `0212-2.sql`: cập nhật 7 giới hạn lớp, danh mục room và cấu trúc attendance. Hai index attendance được gửi lại đúng định nghĩa yêu cầu. |
| BE mới | Source đã sửa và kiểm local; **chưa xác nhận bản mới đã deploy trên Render**. Migration DB không tự deploy BE. |
| FE/live | Còn kiểm tích hợp có đăng nhập trên đúng bản BE mới; chưa nghiệm thu browser/CORS/cookie/camera QR, OTP/email cloud hoặc VNPay gateway live. |

Base API đang khai báo: `https://scms-app-p1gy.onrender.com`. Đăng nhập dùng cookie **JSESSIONID**; Fetch bật `credentials: 'include'`, Axios bật `withCredentials: true`. Kiểm `GET /api/auth/me` trước nghiệp vụ. Ngày giờ nghiệp vụ theo `Asia/Ho_Chi_Minh`.

## 2. Room: ID số và mã hiển thị riêng

**Lý do:** giữ `room_id` số làm khóa định danh để tương thích với khóa ngoại của lịch học và API hiện có, tránh phải đổi các tham chiếu sang chuỗi. Thêm cột `room_code` (API: `roomCode`) để người dùng nhận biết center/môn/sân qua mã dễ đọc; khi cần đổi mã hiển thị vẫn giữ nguyên ID và liên kết lịch sử của phòng đó.

- `RoomResponse.id` vẫn là số; request đặt/dời lịch tiếp tục gửi `roomId` số lấy từ API. `roomCode` là mã hiển thị/tìm kiếm, ví dụ `C01-TEN-01`; `C01` chỉ center 1. Không suy ID từ mã hoặc thứ tự bảng.
- Không thêm `subjectId` vào room. FE tải lại `GET /api/rooms`, dùng `id`, `name`, `roomCode`, `capacity`, `status`, `facilityType`, `location`, `outdoor` từ response.
- Danh mục mới có **20 tài nguyên**. Phòng cũ được giữ `Closed` để bảo toàn lịch sử; lịch tương lai đủ điều kiện chuyển sang phòng mới, giữ ngày/giờ. GET rooms ẩn phòng `Closed` có mã `LEGACY`; vẫn có thể trả `Maintenance` hoặc phòng đóng khác. Bộ chọn đặt lịch chỉ cho chọn `Available`.

| Mã room (đầy đủ tiền tố C01) | Tên | Số lượng | Sức chứa mỗi room | Loại |
|---|---|---:|---:|---|
| `C01-TEN-01`, `C01-TEN-02` | Sân Tennis 01–02 | 2 | 4 | Outdoor |
| `C01-SWIM-01`, `C01-SWIM-02` | Hồ bơi 01–02 | 2 | 10 | Outdoor |
| `C01-PICK-01`, `C01-PICK-02` | Sân Pickleball 01–02 | 2 | 4 | Indoor |
| `C01-ARC-01`, `C01-ARC-02` | Khu Bắn cung 01–02 | 2 | 5 | Outdoor |
| `C01-BR-01`, `C01-BR-02` | Sân Bóng rổ 01–02 | 2 | 8 | Indoor |
| `C01-BC-01`, `C01-BC-02` | Sân Bóng chuyền 01–02 | 2 | 8 | Indoor |
| `C01-FOOT-01`, `C01-FOOT-02` | Sân Bóng đá 01–02 | 2 | 15 | Outdoor |
| `C01-BAD-01`, `C01-BAD-02` | Sân Cầu lông 01–02 | 2 | 4 | Indoor |
| `C01-TT-01`, `C01-TT-02`, `C01-TT-03`, `C01-TT-04` | Bàn Bóng bàn 01–04 | 4 | 2 | Indoor |

POST/PUT `/api/rooms` nhận thêm `roomCode` tùy chọn, tối đa 40 ký tự, dạng `C[ít nhất 2 chữ số]-[chữ hoa/số]-[chữ hoa/số]`, tiền tố phải đúng center. POST thiếu mã tự sinh `C01-RM-...`; PUT thiếu mã giữ mã cũ. POST trả 201, PUT trả 200; trùng mã 409, sai định dạng 400, sai center 400 với `ROOM_CODE_CENTER_MISMATCH`.

## 3. Đã đổi giới hạn 7 lớp cho vừa room mới

Owner đã duyệt giảm `classes.max_capacity` theo bảng dưới trong cùng transaction chuyển phòng. Đây là **ID lớp trên DB đang bàn giao**, không phải ID room và không phải ID cố định cho mọi môi trường.

| ID lớp | Môn | Giới hạn cũ → mới | Room đích theo môn |
|---:|---|---:|---|
| 11 | Bơi lội | 15 → 10 | `C01-SWIM-01` |
| 12 | Pickleball | 12 → 4 | `C01-PICK-01` |
| 13 | Bắn cung | 10 → 5 | `C01-ARC-01` |
| 14 | Bóng rổ | 15 → 8 | `C01-BR-01` |
| 16 | Bóng chuyền | 15 → 8 | `C01-BC-01` |
| 17 | Cầu lông | 15 → 4 | `C01-BAD-01` |
| 18 | Bóng bàn | 15 → 2 | `C01-TT-01` |

Migration kiểm đúng lớp Normal/center/môn/giới hạn được duyệt và số chỗ `Registered + Pending` không vượt mức mới; lưu audit trước/sau. Không đổi ID lớp, người học, giá, chứng từ hoặc attendance. Lỗi trước đó so sánh **giới hạn tối đa của lớp** với sức chứa room mới, không so số học viên hiện có.

**FE cần:** tải lại `GET /api/classes`, hiển thị/validate theo `maxCapacity` hiện tại; không hardcode mọi Normal đều 15 chỗ. Lớp Normal tối đa 15 và phải vừa room được chọn. Khi hiển thị buổi học, lấy room từ dữ liệu lịch thực tế; không dùng bảng trên để tự ghi đè lịch.

## 4. Báo vắng: thành công phải có attendance Absent

| Thao tác | Contract |
|---|---|
| Member báo vắng | POST `/api/sessions/{id}/absence`, body `{"reason":"Lý do báo vắng"}` → 200 |
| Điều kiện | Member đúng chủ ca Advanced đã đặt; còn **trên 0 và dưới 12 giờ** trước giờ bắt đầu. Normal không dùng luồng này. |
| Response notice | `id`, `sessionId`, `memberId`, `reportedAt`, `reason`, `active`, `supersededAt` |
| Đọc notice | GET `/api/sessions/{id}/absence-notices`; theo phạm vi chủ ca/Coach được giao/Manager |
| Đọc attendance | GET `/api/attendances?sessionId={id}`; thêm `memberId` khi cần và được phép, BE kiểm scope theo phiên |
| Attendance | Response thêm `absenceNoticeId` nullable để truy về notice và lý do |

Sau 200, BE ghi notice và attendance **Absent cùng transaction** cho đúng Member/buổi. Session vẫn `Scheduled`, tài nguyên vẫn được giữ, không hoàn thêm credit; không tạo đánh giá Coach hoặc giờ check-in giả. FE refresh cả notice và attendance; không tự đổi trạng thái session thành Absent.

- Retry hợp lệ trong cửa sổ trả cùng notice, không nhân bản attendance hoặc trừ thêm credit.
- Coach/QR trong buổi có thể cập nhật kết quả thực tế thành `Present`, giữ dấu vết notice. Manager correction sau buổi đồng bộ training result đã có.
- Đổi thời gian: notice hết hiệu lực; attendance tự động cũ được lưu lịch sử bằng `superseded_at`, không xuất trong danh sách attendance hiện hành. Đổi riêng phòng cùng giờ giữ notice/Absent.
- Sai owner/role/center: 403. Ngoài cửa sổ: 400 `ABSENCE_WINDOW_CLOSED`. Normal: 400 `NORMAL_CLASS_NON_CANCELLABLE`. Sửa attendance đã lưu lịch sử: 400 `ATTENDANCE_SUPERSEDED`.

DB đã có `absence_notice_id` và `superseded_at`; unique index chỉ áp dụng attendance hiện hành (`superseded_at IS NULL`). Không backfill hàng loạt notice cũ thành Absent.

## 5. Kết luận các phát hiện gốc cần FE lưu ý

| Phát hiện | Kết luận / FE thực hiện |
|---|---|
| 31/10: báo vắng 200 nhưng thiếu Absent | Lỗi BE đã sửa trong source và kiểm local theo mục 4; cần kiểm lại sau deploy. |
| 29/10 BE-01: Admin dashboard 200/toàn 0 | Phù hợp query hiện tại theo center trong khi Admin có center null. Chưa triển khai tổng hợp toàn hệ thống; không tự gán Admin sang center 1. |
| 29/10 BE-02: `/api/auth/me` thiếu `centerId` | Đúng DTO hiện hành. BE lấy scope từ phiên; FE không yêu cầu thêm field này để thực hiện nghiệp vụ. Giữ thiết kế center 1 đã chốt. |
| 29/10 BE-03: `activeSubscriptions = 11` | Chỉ số là tổng **lượt đăng ký còn quyền** Advanced + Normal, không phải số người hoặc mọi dòng đăng ký. Chưa có đối soát độc lập chứng minh số 11 của snapshot gốc sai. |
| Dashboard khác revenue tháng | Dashboard toàn kỳ và revenue theo khoảng ngày có thể khác. Phân biệt gross Paid, tiền thực nhận, hoàn tiền và net; dùng cùng kỳ trước khi kết luận lỗi. |
| GET `/api/members/me/subscriptions` lỗi | GET này không được định nghĩa. Dùng GET `/api/members/{memberId}/subscriptions`, lấy `memberId` từ `/api/auth/me`; tuyến `/me/subscriptions` là POST mua gói. |
| Normal 12 buổi nhưng có 13 dòng session | Có thể gồm buổi invalidated và buổi bù. Đếm buổi hợp lệ và liên kết bù; danh sách catalog có bộ lọc không bắt buộc trả đủ 12 dòng. |
| F-06/F-07 từng trả 500 | Tổ hợp đã kiểm lại trả 200; chưa chứng nhận toàn bộ role/query hoặc Render hiện tại. Nếu tái hiện, gửi đúng method/path/query, role, response và thời điểm. |
| Số liệu demo/snapshot 29/10 | Là bằng chứng lịch sử theo phạm vi đã xuất, không phải quota hoặc số liệu live phải giữ nguyên. Không seed/reset để ép khớp báo cáo. |

## 6. Việc còn lại để nghiệm thu tích hợp

1. BE xác nhận đã deploy bản có `roomCode`, `absenceNoticeId` và workflow mới trên môi trường FE sử dụng. FE đối chiếu schema thực tế; DB chạy xong chưa đủ xác nhận bước này.
2. FE tải lại rooms/classes, bỏ ID hoặc giới hạn lớp hardcode, cập nhật bộ chọn room và màn hình sĩ số theo mục 2–3.
3. Kiểm có phiên: báo vắng hợp lệ → notice + Absent; retry; sai chủ ca; biên 0/12 giờ; Coach/QR Present; đổi giờ so với đổi riêng phòng; Manager correction và lịch sử.
4. Kiểm browser/CORS/cookie/QR trên FE hiện hành. OTP/email cloud, VNPay live và các tổ hợp CRUD/role chưa kiểm vẫn cần nghiệm thu riêng; hai migration không xác nhận các mục này.


<a id="querydr"></a>
## 7. Bổ sung QueryDR — 04/11

BE thêm **POST `/api/members/me/invoices/{id}/verify-vnpay`**, cookie phiên Member, JSON `{}`. `{id}` là `invoiceId` từ API. Trả200 `{verificationStatus,message,retryAfterSeconds,invoice}`; dành timeout client ít nhất20giây. Status/lỗi ở [contract thanh toán](docs/01-API-CONTRACT-FE-BE.md#payments).

FE tự POST một lần cho đúng hóa đơn VNPay khi quay về từ thanh toán, sau đó GET `/api/members/me/invoices`. Tái sử dụng **Tải lại** để kiểm tra hóa đơn đang theo dõi rồi GET; tuân thủ retryAfterSeconds, không cần nút tự xác nhận đã trả tiền, không tự quét tất cả hóa đơn. Hiển thị theo invoice từ BE; Paid+reconciliationRequired phải báo đối soát. `status=00` trên URL không là chứng từ. GET danh sách vẫn chỉ đọc.

Bảng mới **`vnpay_payment_attempts`** chỉ phục vụ BE, lưu dữ liệu gốc mỗi lần tạo link và cooldown; FE không đọc/ghi trực tiếp. Owner chạy nguyên [15_QueryDR.sql](database/migrations/15_QueryDR.sql) một lần trước deploy. File cộng thêm, không sửa/xóa bảng hoặc chứng từ hiện có. Đơn cũ thiếu attempt trả `ATTEMPT_MISSING`, không tự chữa hoặc mời trả thêm.

**Tình hình:** source/contract đã bổ sung; kết quả local và giới hạn tại [Active docs00](docs/00-MASTER-CHECKLIST.md#active). Chưa deploy Render/chạy SQL cloud hoặc tích hợp FE Pages trong lượt này. Repo chỉ có dist của FE demo hiện tại; đội FE cần ghép lời gọi vào source đang dùng. Toggle và kiểm merchant thật theo [docs02](docs/02-DEPLOYMENT-GUIDE.md#querydr).
