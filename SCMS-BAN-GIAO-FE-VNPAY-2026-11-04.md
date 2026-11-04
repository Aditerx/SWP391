# SCMS — Bàn giao FE VNPay — 04/11/2026

## 1. Kết quả hiện tại

Sau khi lưu IPN URL tại Terminal sandbox, Owner thanh toán thử và nhận hóa đơn **Paid**, có mã giao dịch, `reconciliationRequired=false` ngay khi quay về FE, không cần reload. QueryDR từ Render vẫn timeout kết nối; chưa nghiệm thu QueryDR live. Bằng chứng và phạm vi kiểm tại [Active](docs/00-MASTER-CHECKLIST.md#active).

## 2. Quá trình và quyết định giữ

| Mốc | Việc đã làm và kết quả |
|---|---|
| Trước cấu hình IPN | Gateway báo thành công nhưng SCMS còn Pending rồi Expired; Return không ghi Paid. |
| Bổ sung QueryDR | Thêm POST xác minh, bảng lưu lần tạo link và migration15; Owner đã chạy migration thành công. |
| Kiểm kết nối | Render không kết nối TCP443 tới VNPay trong15giây; QueryDR chưa lấy được phản hồi. Chưa xác định bên nào chặn. |
| Cấu hình IPN | Đặt URL BE, GET, HMACSHA512 tại Terminal; lượt sandbox tiếp theo đã ghi Paid ngay sau Return. |
| Owner chốt giữ nguyên | Giữ code, endpoint, bảng, migration và các env đã thêm; không áp dụng đề xuất tắt QueryDR trước đó. |

**Lý do giữ:** IPN là kênh VNPay chủ động báo kết quả; QueryDR là kênh BE chủ động kiểm tra khi cần. QueryDR còn hữu ích sau khi đường mạng được khôi phục. Bảng `vnpay_payment_attempts` vẫn được BE dùng mỗi lần tạo link thanh toán, kể cả khi tắt QueryDR; xóa bảng riêng sẽ làm lỗi tạo link. Không thêm relay hoặc đổi env trong lần bàn giao này.

## 3. API và bảng DB

**POST `/api/members/me/invoices/{id}/verify-vnpay`** — Active Member sở hữu hóa đơn, cùng center.

- `{id}` là `invoiceId`, không phải phần số cuối `invoiceCode`.
- Body `{}`, `Content-Type: application/json`, cookie phiên (`credentials: 'include'` / `withCredentials: true`). Timeout60giây; chỉ một lời gọi đang chạy.
- HTTP200: `{verificationStatus, message, retryAfterSeconds, invoice}`; `invoice` cùng cấu trúc với GET hóa đơn.
- Bảng `vnpay_payment_attempts`: lưu mã đơn, merchant, số tiền, thời điểm tạo link gốc và lần kiểm tra; chỉ BE đọc/ghi. Giữ [15_QueryDR.sql](database/migrations/15_QueryDR.sql), không chạy lại để sửa đơn cũ.

## 4. FE cần làm

1. Sau Return, GET `/api/members/me/invoices/{id}` và hiển thị ngay trạng thái API; tự POST xác minh một lần rồi GET lại `/api/members/me/invoices`. Nếu IPN đã ghi Paid, POST trả `ALREADY_PAID`, không gọi VNPay lại.
2. Dùng lại nút **Tải lại** cho POST của đúng hóa đơn đang theo dõi rồi GET; chờ `retryAfterSeconds`, không quét toàn bộ hóa đơn hoặc lặp vô hạn.
3. Hiển thị theo `paymentStatus` và `reconciliationRequired`; không lấy `status=00` trên URL làm bằng chứng Paid, không thêm nút tự xác nhận đã trả tiền.
4. Chỉ gọi GET `/api/invoices/{id}/receipt` khi Paid. Pending/Expired dùng GET chi tiết ở bước1; receipt409 không phải kết quả xác minh.

| `verificationStatus` | FE xử lý |
|---|---|
| `VERIFIED_PAID` / `ALREADY_PAID` | Đã ghi nhận tiền; nếu cần đối soát thì không báo đã cấp quyền học. |
| `RETRY_LATER` | Chờ `retryAfterSeconds` trước lần tiếp theo. |
| `NOT_CONFIRMED` / `QUERY_UNAVAILABLE` | Chưa xác minh được; không kết luận chưa trả hoặc mời trả thêm. |
| `ATTEMPT_MISSING` | Thiếu dữ liệu tạo link gốc; chuyển đối soát. |
| `DISABLED` / `VERIFIED_ONLY` | Chưa bật hoặc chỉ kiểm tra; chưa đồng nghĩa Paid. |
| `NOT_APPLICABLE` | Đơn Refunded; theo hồ sơ xử lý hiện hành. |

GET danh sách chỉ đọc; Return không ghi Paid; FE không gọi IPN. BE kiểm chữ ký/merchant/mã đơn/số tiền/trạng thái và chống ghi lặp. Kết quả thành công được xử lý tới/quá hạn5phút hoặc khi quyền không còn thì Paid kèm đối soát, không tự cấp lại quyền học.

## 5. Việc còn lại

- QueryDR: xử lý đường kết nối Render→VNPay trước khi nghiệm thu truy vấn live.
- FE: chưa xác nhận đã ghép POST xác minh; việc thấy Paid ngay sau Return không chứng minh POST này đã được gọi.
- Đơn cũ chưa ghi nhận tiền: đối soát riêng, không thanh toán lại để sửa trạng thái.
- Quyền học: kiểm API đăng ký/lịch trước khi kết luận toàn bộ luồng học đã hoàn tất.

Chi tiết field/error: [contract](docs/01-API-CONTRACT-FE-BE.md#payments). Cấu hình IPN/QueryDR: [vận hành](docs/02-DEPLOYMENT-GUIDE.md#ipn-acceptance). Quyết định: [D10](docs/04-DEBUG-NOTE.md#d10).
