# Cấu hình CORS và phiên đăng nhập cho demo SCMS

- Frontend demo: https://scms-demo-04nov.pages.dev
- Backend Render: https://scms-app-p1gy.onrender.com
- Lỗi đã ghi nhận: preflight `OPTIONS /api/auth/login` từ frontend trả `403 Invalid CORS request`.

## 1. Cập nhật cấu hình trên Render

1. Mở **Render → service `scms-app-p1gy` → Environment**. Thêm `https://scms-demo-04nov.pages.dev` vào danh sách `FRONTEND_URL`, giữ các origin đang dùng và phân cách bằng dấu phẩy. Nhập origin HTTPS chính xác, bỏ dấu `/` cuối.
2. Nếu service đặt riêng thuộc tính `scms.frontend-origins`, cập nhật danh sách ở đó vì thuộc tính này được ưu tiên. Trong snapshot BE ngày 29/10, `SecurityConfig` đã bật `allowCredentials=true` và đọc danh sách origin từ cấu hình trên. Đối chiếu lại với bản đang chạy.
3. FE dùng cookie `JSESSIONID` và gửi request với `credentials: "include"`. Frontend Pages và backend Render khác site, nên **cần cấu hình cookie riêng bên cạnh origin CORS**. Trong Environment thêm/sửa đúng hai biến dưới đây, giá trị không kèm dấu nháy:

   ```text
   SESSION_COOKIE_SAME_SITE=none
   SESSION_COOKIE_SECURE=true
   ```

   Source hiện hành đọc đúng hai biến này. `SameSite=Lax` không gửi cookie trong fetch cross-site dù FE đã có `credentials: "include"`; có `Secure` một mình chưa đủ. Vì vậy login có thể trả200 nhưng API kế tiếp lại401. [Giải thích Fetch/SameSite](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch#including_credentials).
4. Chọn **Save, rebuild, and deploy**, chờ service Live. Không chỉ lưu mà chưa deploy; Restart service giữ cấu hình của lần deploy hiện tại. Giữ danh sách origin cụ thể để dùng cùng credentials. [Render Environment](https://render.com/docs/configure-environment-variables), [Render deploys](https://render.com/docs/deploys).

## 2. Kiểm tra sau khi áp dụng

Chạy lệnh sau trong PowerShell. Preflight không cần tài khoản demo.

```powershell
curl.exe -i -X OPTIONS "https://scms-app-p1gy.onrender.com/api/auth/login" -H "Origin: https://scms-demo-04nov.pages.dev" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type"
```

Kết quả mong đợi:

- HTTP `2xx`.
- `Access-Control-Allow-Origin: https://scms-demo-04nov.pages.dev`.
- `Access-Control-Allow-Credentials: true`.
- Preflight cho phép phương thức `POST` và header `content-type`.

Sau đó đăng nhập bằng tài khoản demo từ [URL frontend](https://scms-demo-04nov.pages.dev). Kiểm `GET /api/auth/me` trả `200` và F5 ở màn đã đăng nhập vẫn giữ phiên. Nếu preflight qua nhưng phiên mất sau F5, mở **DevTools → Network** để kiểm thuộc tính `Set-Cookie` của response login và lý do trình duyệt chặn cookie. Khi bàn giao kết quả, chỉ gửi status, header CORS và thuộc tính cookie, bỏ giá trị cookie và thông tin đăng nhập.

Sau khi sửa cấu hình, tải lại FE và đăng nhập mới vì cookie cũ không tự đổi thuộc tính. Trong Network chọn đúng request login loại fetch → Headers → Response Headers; cookie mới phải có `SameSite=None; Secure; HttpOnly`. Chọn request `GET /api/auth/me` phát sinh từ FE và kiểm200; mở endpoint trực tiếp ở tab BE không kiểm chứng cookie cross-site. Nếu vẫn401 dù cookie đã có None/Secure, xem tab Cookies/Issues của request để lấy lý do browser chặn cookie bên thứ ba; không bật cho phép toàn bộ cookie trên mọi website chỉ để thử.

## Tham chiếu

- [SecurityConfig hiện hành](src/main/java/com/sportscenter/config/SecurityConfig.java)
- [Cấu hình session cookie hiện hành](src/main/resources/application.properties)
- [Chẩn đoán VNPay sai chữ ký: cấu hình và cách lấy paymentUrl](docs/02-DEPLOYMENT-GUIDE.md#vnpay-diagnostics)
- [Trạng thái kiểm hiện hành và giới hạn nghiệm thu](docs/00-MASTER-CHECKLIST.md#active)
- [Quy tắc SameSite và Secure](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie#samesitesamesite-value)
