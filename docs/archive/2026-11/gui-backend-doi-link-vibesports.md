> **LƯU TRỮ:** nội dung theo thời điểm ghi trong bản này; trạng thái hiện hành xem [Active](../../00-MASTER-CHECKLIST.md#active). Lưu trữ không có nghĩa mọi luồng đã nghiệm thu.

Chào nhóm Backend,

Frontend SCMS chuyển sang link demo mới: `https://vibesports.pages.dev`. Link cũ `https://scms-demo-04nov.pages.dev` vẫn chạy tới hết buổi demo.

Hiện tại preflight CORS từ origin mới tới `/api/auth/me` đang trả 403 (FE kiểm tra ngày 12/11/2026). Link cũ vẫn trả 200 bình thường.

Nhờ nhóm cập nhật trên server `scms-app-p1gy.onrender.com`:

1. **CORS:** thêm `https://vibesports.pages.dev` vào danh sách origin được phép, giữ nguyên origin cũ.
   - Theo code hiện tại (`SecurityConfig`), danh sách này đọc từ biến môi trường `FRONTEND_URL` (hoặc thuộc tính `scms.frontend-origins`), các origin cách nhau bằng dấu phẩy.
   - Chỉ cần sửa biến trên Render thành `https://scms-demo-04nov.pages.dev,https://vibesports.pages.dev` rồi khởi động lại dịch vụ, không phải sửa code.
   - Các cấu hình hiện tại vẫn giữ như cũ:
   - Cho phép credentials (`Access-Control-Allow-Credentials: true`).
   - Liệt kê origin chính xác, không dùng `*`.
   - Cho phép các method GET, POST, PUT, PATCH, DELETE, OPTIONS.
   - Cho phép header `Content-Type` và các header FE đang gửi.
2. **Cookie phiên:** giữ `SameSite=None; Secure`, không gắn `Domain` cố định theo tên miền FE, vì FE và API vẫn khác tên miền.
3. **Google Sign-In:** Client ID không đổi. Nếu server có chỗ kiểm tra origin, hoặc tạo link trỏ về tên miền FE (ví dụ link trong email OTP hay email quên mật khẩu), nhờ thêm hoặc đổi sang link mới.
4. **VNPay:** biến `FRONTEND_PAYMENT_RESULT_URL` (URL trả về sau thanh toán), nếu đang trỏ về link FE cũ, nhờ đổi sang `https://vibesports.pages.dev/payment-result`.
5. Deploy xong, nhờ nhóm báo lại để FE kiểm tra.

Cách tự kiểm tra nhanh bằng preflight CORS. Kết quả đúng là trả 200 hoặc 204, có header `Access-Control-Allow-Origin: https://vibesports.pages.dev` và `Access-Control-Allow-Credentials: true`:

```
curl -i -X OPTIONS https://scms-app-p1gy.onrender.com/api/auth/me \
  -H "Origin: https://vibesports.pages.dev" \
  -H "Access-Control-Request-Method: GET"
```

Cảm ơn nhóm!
