# SCMS — Bàn giao FE OTP đăng ký tài khoản — 04/11/2026

## 1. Kết quả hiện tại

Owner cung cấp POST/OPTIONS `/api/auth/resend-otp`200, log Render báo gửi REGISTER thất bại và Brevo0log. Chưa xác định lỗi kết nối, credential, TLS hay server từ chối; chưa nghiệm thu nhận OTP hoặc kích hoạt tài khoản. Source bổ sung log SMTP an toàn để lấy nguyên nhân sau deploy; kết quả/source/môi trường kiểm xem [docs00](docs/00-MASTER-CHECKLIST.md#active), bằng chứng máy đọc tại `otpSmtpDiagnostics` trong [QA](artifacts/qa/latest.json).

## 2. Quá trình và quyết định giữ/bỏ

- Owner chọn Brevo SMTP2525, báo đã xác minh sender và thêm Environment trên Render.
- BE tách MAIL_FROM_EMAIL khỏi SMTP login, giữ fallback cấu hình cũ và gửi OTP bất đồng bộ sau commit.
- Response `code:null` được xác định là mã trạng thái tùy chọn; giữ nguyên contract, không trả OTP về FE.
- Theo log thất bại Owner cung cấp, bổ sung nhóm lỗi/mã SMTP/attemptId; không ghi exception message, nội dung thư hoặc credential.
- Giữ provider/port/STARTTLS và dữ liệu hiện tại; chưa chuyển HTTPS hoặc kết luận Render chặn2525 khi chưa có log phân loại.

## 3. API và DB liên quan

POST `/api/auth/register`: fullName/email/password, các field tùy chọn theo [contract](docs/01-API-CONTRACT-FE-BE.md); tạo Pending và yêu cầu OTP. POST `/api/auth/resend-otp`: `{email,purpose:"REGISTER"}`;200 là phản hồi xử lý, chưa xác nhận delivery. POST `/api/auth/verify-registration`: `{email,otp}`;204 khi xác minh thành công.

Luồng dùng users/members/email_otps; OTP lưu hash, không có migration hoặc thay đổi DB trong lượt chẩn đoán. Chỉ verify hợp lệ mới chuyển Pending→Active.

## 4. FE cần làm

Hiển thị thông báo trung tính khi register/resend200; không hiển thị `code:null` thành lỗi hoặc mã OTP. Chờ ít nhất60giây mới gửi lại, tối đa3OTP/giờ; OTP10phút, tối đa5lần sai và dùng một lần theo contract. Hiển thị riêng các mã REGISTRATION_PENDING/EMAIL_ALREADY_REGISTERED/OTP_INVALID/OTP_EXPIRED/OTP_ATTEMPTS_EXCEEDED/OTP_RESEND_TOO_SOON/OTP_RESEND_LIMIT. Sau verify204 mới tiếp tục luồng đăng nhập.

## 5. Việc còn lại

Owner deploy source mới, chờ Live rồi mở Render Logs và gửi lại đúng1lần; lấy `SMTP_SEND_` có cùng attemptId theo [hướng dẫn docs02](docs/02-DEPLOYMENT-GUIDE.md#chẩn-đoán-otp-smtp), không cần thêm env debug. Agent đối chiếu category/exceptionTypes/smtpCode để đề xuất sửa đúng nguyên nhân. Sau đó kiểm Brevo Transactional→Logs, Inbox/Spam và verify-registration; chỉ ghi PASS delivery khi có thư thật và xác minh trạng thái phù hợp.
