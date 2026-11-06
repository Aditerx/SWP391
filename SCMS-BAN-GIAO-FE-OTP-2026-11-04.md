# SCMS — Bàn giao FE OTP đăng ký tài khoản — 04/11/2026

## 1. Kết quả hiện tại

**PASS ca đăng ký bằng email thật theo xác nhận và ảnh Owner cung cấp:** FE Pages gọi BE Render, OTP được gửi qua Brevo SMTP2525 tới Gmail; Owner nhập OTP và FE hiển thị “Đăng ký hội viên thành công”. Ảnh Brevo/Gmail hiển thị23:08 ngày04/11, với Sent, Delivered và First opening của cùng một email; đây là ba sự kiện, không phải ba thư.

Owner đã bổ sung bằng chứng cho tài khoản kiểm tra42:

| Phần kiểm | Kết quả đã cung cấp |
|---|---|
| Xác minh OTP | POST `/api/auth/verify-registration` trả204 No Content, đúng contract; OPTIONS200 là preflight |
| Trạng thái lưu | Ảnh Supabase bảng users cho thấy user_id42 có status Active |
| Đăng nhập/cổng Member | Ảnh response `/api/auth/login` trong Network FE trả userId42, memberId42, role Member và isFirstLogin=false; FE vào dashboard Member42 |

Ghi nhận PASS ca nhận OTP→xác minh→account Active→đăng nhập Member theo bằng chứng Owner; dùng response `/api/auth/login` làm nguồn danh tính/quyền cho bản bàn giao này. Response trả userId42, memberId42, role Member, authorities và isFirstLogin=false, phù hợp dashboard Member đã hiển thị. Trạng thái used của OTP và các ca lỗi/giới hạn vẫn có phạm vi kiểm riêng. Mốc/source tại [docs00](docs/00-MASTER-CHECKLIST.md#active); bằng chứng tối thiểu tại `otpRegistrationOwnerAcceptance` trong [QA](artifacts/qa/latest.json), không lưu OTP, email, số điện thoại hoặc họ tên từ ảnh.

## 2. Quá trình và quyết định giữ/bỏ

- Chọn Brevo SMTP2525, xác minh sender và tách MAIL_FROM_EMAIL khỏi SMTP login; giữ JavaMailSender, STARTTLS và gửi sau commit.
- Giữ contract `code:null` là mã trạng thái tùy chọn; không trả OTP về FE hoặc coi resend200 là delivery.
- Log SMTP an toàn xác định lỗi AUTHENTICATION; ảnh Brevo cho thấy chặn SMTP IP đang bật, chưa có IP được cho phép.
- Đối chiếu IP bị chặn với dải outbound Render và cho phép IP phù hợp trên Brevo; Owner sau đó cung cấp thư thật, verify204, user42 Active và response login đúng Member42.
- Giữ SMTP2525, log an toàn và rule OTP hiện hành; không triển khai adapter HTTPS hoặc migration DB trong lượt xử lý này.

## 3. API và DB liên quan

| Thao tác | Input / kết quả theo contract |
|---|---|
| POST `/api/auth/register` | fullName/email/password bắt buộc; phone/dateOfBirth/gender tùy chọn;200 tạo Pending và yêu cầu OTP |
| POST `/api/auth/resend-otp` | `{email,purpose:"REGISTER"}`;200 trả thông báo chung, không chứa OTP hoặc xác nhận thư đã tới Inbox |
| POST `/api/auth/verify-registration` | `{email,otp}`;204, không có JSON body, khi xác minh hợp lệ |
| POST `/api/auth/login` | email/password →200 và cookie JSESSIONID; response có userId/memberId/role/authorities/isFirstLogin để FE nhận danh tính và quyền |

Luồng dùng users/members/email_otps; source lưu OTP dạng hash và chỉ verify hợp lệ mới chuyển Pending→Active. Việc đăng ký/xác minh vẫn ghi dữ liệu theo workflow; không có thay đổi schema hoặc migration DB cho bản sửa SMTP/IP. Field/state/error đầy đủ xem [contract](docs/01-API-CONTRACT-FE-BE.md#1-kết-nối-đăng-nhập-và-lỗi).

## 4. FE cần làm

1. Gọi register, chuyển sang nhập OTP với thông báo trung tính; `code:null` không phải lỗi hoặc mã OTP. Nếu REGISTRATION_PENDING, tiếp tục xác minh tài khoản chờ, không tạo thêm account.
2. Gửi lại với purpose REGISTER; chờ ít nhất60giây giữa các yêu cầu và tối đa3OTP/giờ. OTP6chữ số, hạn10phút, tối đa5lần sai, dùng một lần; sau gửi lại dùng mã mới.
3. Hiển thị riêng EMAIL_ALREADY_REGISTERED/OTP_INVALID/OTP_EXPIRED/OTP_ATTEMPTS_EXCEEDED/OTP_RESEND_TOO_SOON/OTP_RESEND_LIMIT theo contract, không bấm gửi lại liên tục khi bị giới hạn.
4. Chỉ hiển thị đăng ký thành công sau verify204; không parse JSON cho response204. Verify không tự tạo phiên: FE gọi `/api/auth/login` trước khi vào cổng hội viên, dùng userId/memberId/role/authorities/isFirstLogin từ response login để cập nhật trạng thái đăng nhập. Các request có phiên cần credentials/include hoặc withCredentials; không lấy role/memberId từ ô nhập hoặc dữ liệu FE tự đặt làm căn cứ phân quyền.

## 5. Việc còn lại

- Ca đăng nhập và dashboard Member đã có bằng chứng cùng response login đúng danh tính; không yêu cầu chạy lại để cập nhật tài liệu.
- Response login đã có memberId42; chưa có ảnh/query riêng email_otps.used hoặc bảng members để ghi nhận kiểm DB trực tiếp.
- Khi nghiệm thu thêm trên Render, dùng fixture/phạm vi được xác định để kiểm OTP sai/hết hạn/dùng lại và giới hạn gửi lại; không nâng các ca chưa chạy thành PASS từ ca thành công này.
- Vận hành giữ quyền IP outbound phù hợp trên Brevo; nếu IP thay đổi, đối chiếu lại trước khi cho phép. Cấu hình và chẩn đoán theo [docs02](docs/02-DEPLOYMENT-GUIDE.md#chẩn-đoán-otp-smtp); không cần triển khai thêm chỉ để nhận lại email cho ca đã đạt.
