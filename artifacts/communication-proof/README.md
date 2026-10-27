# Kiểm chứng OTP/SMTP, chat và notification

Owner yêu cầu đưa phần còn sót vào branch/main và kiểm Render sau5phút. Nền d95d59b; test source3857be5,merge2164747. Không thay code src/main hoặc schema.

- [Manifest clean test bản merge](merged-verification.json):298run/273pass/25skip/0lỗi;[summary](merged-test-summary.txt). 25skip=19live/PGnền+6communication opt-in.
- [Bộ kiểm thật](20261027-160912-2bfcd8/verification.json):6/6fresh+6/6upgrade,0skip;HTTP login/cookies/PostgreSQL/SMTPloopback. Kiểm migration lặp và snapshot invoice/subscription/session,không cloudDB/không gửi email ra ngoài.
- Lượt075232 invalidated và160013 FAIL được giữ làm lịch sử,không phải bằng chứng pass. Lỗi fixture SMTP timeout được cô lập:pin EHLO localhost để không phụ thuộc tra cứu hostname Windows;giữ timeout10giây,thêm protocol diagnostics,không đổi expected hoặc mock SMTP.

```powershell
python scripts/test-communication-features.py
```

Runner tự tạo detached committed checkout không.env và container PostgreSQL bind127.0.0.1 cổng ngẫu nhiên,apply06/07/08–12,kiểm fresh/upgrade và capture SMTP sockets thật. Container dừng sau chạy,data giữ lại.Log raw ởTEMP,HTTP/DB/SMTPsteps đã bỏ credentials/OTP chỉ ghi trạng thái. Không coi đây là Gmail delivery/FEbrowser/livegateway.

Kiểm Render đọc-only bằng scripts/check-render-production.py,buộc chờ>=300giây theo pushed-at-utc,kết quả riêng artifacts/production-check/2026-10-27. Không đăng nhập/tạo account/giao dịch trên production.
