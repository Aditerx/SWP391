# SCMS — Bàn giao FE ngày 04/11/2026

## VNPay: tự kiểm tra thanh toán bằng QueryDR

**Tình hình:** Owner xác nhận đã chạy migration15 thành công. BE Render đã công bố endpoint mới; CORS từ Pages cho phép POST kèm cookie. POST có phiên đã chạy nhưng kết nối Render→VNPay đang `CONNECT_TIMEOUT`, chưa ghi Paid. Đang kiểm tra đường mạng ngay trên runtime; chưa nghiệm thu đồng bộ thanh toán live. Chưa xác nhận FE hiện hành đã ghép lời gọi dưới đây.

**API:** `POST /api/members/me/invoices/{id}/verify-vnpay`

- `{id}` là `invoiceId` từ API/URL quay về, không phải phần số cuối mã hóa đơn.
- Body `{}`, `Content-Type: application/json`, gửi cookie (`credentials: 'include'` hoặc `withCredentials: true`), timeout riêng cho POST này60giây; giữ một lời gọi đang chạy trên UI.
- Trả200: `{verificationStatus, message, retryAfterSeconds, invoice}`. `invoice` có cùng cấu trúc với GET hóa đơn.

**FE cần làm:**

1. Sau Return, đọc đúng hóa đơn VNPay của Member rồi tự POST xác minh một lần; sau đó GET lại `/api/members/me/invoices`.
2. Dùng lại nút **Tải lại** cho POST của hóa đơn đang theo dõi rồi GET. Chờ `retryAfterSeconds` trước lần tiếp theo; không tự quét tất cả hóa đơn.
3. Hiển thị theo `invoice.paymentStatus` và `reconciliationRequired`. Không dùng `status=00` trên URL để kết luận Paid; không cần nút “xác nhận đã trả tiền”.
4. Chỉ lấy biên lai qua `/api/invoices/{id}/receipt` khi Paid. Với Pending/Expired, đọc chi tiết bằng GET `/api/members/me/invoices/{id}`; receipt409 không phải kết quả QueryDR.

| Kết quả | Cách hiển thị/xử lý |
|---|---|
| `VERIFIED_PAID` / `ALREADY_PAID` | Đã ghi nhận tiền. Nếu `reconciliationRequired=true`, báo cần đối soát, không báo đã cấp quyền học |
| `RETRY_LATER` | Chờ `retryAfterSeconds` rồi cho kiểm tra lại |
| `NOT_CONFIRMED` / `QUERY_UNAVAILABLE` | Chưa xác minh được; không kết luận chưa trả hoặc mời thanh toán thêm |
| `ATTEMPT_MISSING` | Đơn cũ thiếu dữ liệu tạo link gốc; chuyển đối soát |
| `DISABLED` / `VERIFIED_ONLY` | BE chưa bật hoặc đang ở chế độ chỉ kiểm tra; chưa đồng nghĩa Paid |
| `NOT_APPLICABLE` | Đơn Refunded; theo hồ sơ xử lý hiện hành |

**DB:** bảng mới `vnpay_payment_attempts` lưu metadata link gốc và thời điểm kiểm tra, chỉ BE sử dụng. FE không đọc/ghi bảng. Migration không sửa/xóa chứng từ cũ; đơn cũ thiếu metadata không tự được khôi phục.

**Giữ nguyên:** GET danh sách chỉ đọc; Return chỉ kiểm tra/redirect. Paid hợp lệ tới/quá hạn5phút được ghi nhận kèm đối soát, không tự cấp lại quyền học.

Chi tiết lỗi/field: [contract thanh toán](docs/01-API-CONTRACT-FE-BE.md#payments). Cấu hình sandbox: [hướng dẫn QueryDR](docs/02-DEPLOYMENT-GUIDE.md#querydr). Trạng thái/bằng chứng mới nhất: [Active](docs/00-MASTER-CHECKLIST.md#active).
