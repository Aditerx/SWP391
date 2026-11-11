# SCMS bàn giao FE — AI Chat — cập nhật 11/11/2026

## Kết quả hiện tại

- Member có thể kết thúc hội thoại của mình bằng `POST /api/chat/conversations/{id}/close`. Endpoint suy ra Member từ session, không nhận body. Khi hội thoại `OPEN`, BE chuyển sang `CLOSED`, ghi `closedAt` theo `Clock`/`Asia/Ho_Chi_Minh`, giữ `handlerType` và nhân viên đã nhận, rồi thêm đúng một tin SYSTEM: “Bạn đã kết thúc cuộc trò chuyện.” Metadata ghi `closedBy=MEMBER`.
- Gọi lại khi `CLOSED` trả trạng thái hiện tại, giữ nguyên `closedAt` và không thêm tin. Hội thoại thiếu hoặc thuộc Member khác cùng trả `404 CHAT_CONVERSATION_NOT_FOUND`. Gửi tin sau đóng vẫn trả `409 CHAT_CONVERSATION_CLOSED`; Member vẫn đọc được lịch sử.
- Tin kết thúc không tạo notification cho Member hoặc Receptionist/Manager, không tăng unreadCount Member. Sau khi đóng, `POST /api/chat/conversations` có thể tạo hội thoại `OPEN` mới theo `SCMS_AI_CHAT_ENABLED` và `SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER` hiện có.
- Reply AI tới sau khi đóng không được lưu; auto-escalate do quota, handoff hoặc lỗi cũng không đổi trạng thái/handler và không thêm SYSTEM message. Không thêm migration; DeepSeek/FAQ, quota và giới hạn tốc độ giữ nguyên.
- Kiểm thử local cuối trên base `88ef854`: `clean test` đạt 548 run, 516 pass, 32 skip, 0 failure/error; tăng 9 run so với baseline (8 pass mới, 1 opt-in PostgreSQL skip). Bộ test đích chat đạt 36/36. SHA-256 fingerprint BE/tests `79A8DC53C7A9B9281210CFD8D5FE6ED616F3EFA735280C7E0C52A09AC832FCD9`; skip không tính là pass.

## Quá trình và quyết định giữ/bỏ

1. Đối chiếu endpoint đóng staff và luồng Member hiện có; thêm route riêng cho `ROLE_MEMBER` + `CHAT_AS_MEMBER`, không đổi endpoint staff.
2. Dùng khóa hội thoại hiện có để kiểm ownership, cập nhật trạng thái và lưu tin SYSTEM cùng transaction; giữ nguyên handler/assignedStaff và làm thao tác đóng idempotent.
3. Bảo vệ worker AI ở cả lúc bắt đầu xử lý, lúc lưu reply và lúc tự chuyển staff; kết quả muộn bị bỏ qua khi trạng thái không còn `OPEN`.
4. Không phát event cho tin kết thúc để tránh thông báo thừa; query unread của Member bỏ riêng tin SYSTEM có `closedBy=MEMBER`, không đánh dấu các tin trước đó là đã đọc.

## API và DB liên quan

- `POST /api/chat/conversations/{id}/close`: không body; yêu cầu Member đăng nhập với `CHAT_AS_MEMBER`; trả cùng `ChatConversationResponse` như các route hội thoại. Không nhận `memberId`, actor hoặc lý do từ client.
- `POST /api/chat/conversations`: sau khi đóng hội thoại cũ, Member gọi lại để tạo/lấy hội thoại `OPEN` mới. Handler của hội thoại mới tiếp tục theo hai cờ cấu hình AI hiện hành.
- `GET /api/chat/conversations/{id}/messages`: Member tiếp tục đọc lịch sử kể cả khi `CLOSED`. `POST /api/chat/conversations/{id}/messages` trả `409 CHAT_CONVERSATION_CLOSED` sau khi đóng.
- Dùng `chat_conversations.status/closed_at/handler_type/assigned_staff_user_id` và `chat_messages.sender_type/content/metadata/created_at` hiện có; không thay schema hoặc migration. Metadata SYSTEM của thao tác này là `{"closedBy":"MEMBER"}`.

## FE cần làm

- Khi Member bấm “Kết thúc”, gọi `POST /api/chat/conversations/{id}/close` không kèm body. Với `200`, cập nhật trạng thái `CLOSED`, hiển thị tin SYSTEM trả về và khóa ô gửi tin; refresh hội thoại/tin nhắn theo response hiện hành.
- Nếu Member chọn bắt đầu cuộc trò chuyện mới, gọi `POST /api/chat/conversations`; dùng `handlerType` và dữ liệu response để mở hội thoại mới. Không tự suy handler theo FE config.
- Xử lý `401` theo phiên đăng nhập; `403` theo role/authority; `404 CHAT_CONVERSATION_NOT_FOUND` như hội thoại không truy cập được; `409 CHAT_CONVERSATION_CLOSED` nếu gửi tin trên hội thoại đã đóng.
- Tiếp tục poll lịch sử theo `afterId` và render sender `SYSTEM`/AI như text thuần. Không gửi memberId, actor, role hoặc metadata đóng từ client.

## Việc còn lại

- PostgreSQL HTTP proof của `CommunicationFeaturesPostgresTest` chưa chạy: cổng loopback `127.0.0.1:55440` không mở trong môi trường này. Trạng thái proof đó là BLOCKED, không tính trong 548 test pass/skip; test HTTP+DB sau commit đã được thêm để chạy khi DB cô lập sẵn sàng.
- FE/browser, PostgreSQL runtime, DeepSeek thật và Render chưa được xác minh trong lượt này; không suy từ test local.