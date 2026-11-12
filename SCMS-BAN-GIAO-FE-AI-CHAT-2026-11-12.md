# Bàn giao FE: Member chủ động chọn gặp nhân viên

Ngày: 12/11/2026. Luồng: Chat AI hỗ trợ Member.

## Kết quả hiện tại

BE bổ sung `SCMS_AI_CHAT_HANDOFF_MODE` (mặc định `SUGGEST`, lựa chọn tương thích cũ `AUTO`). Ở `SUGGEST`, gợi ý không tự chuyển hội thoại; Member vẫn có thể tiếp tục hỏi hoặc bấm nút “Gặp nhân viên”. Yêu cầu rõ ràng của Member chuyển ngay. Kết quả kiểm thử và giới hạn môi trường được ghi trong Active của [docs/00-MASTER-CHECKLIST.md](docs/00-MASTER-CHECKLIST.md).

## Quá trình và quyết định giữ/bỏ

- Giữ nguyên endpoint `POST /api/chat/conversations/{id}/escalate`; không thêm endpoint hoặc migration.
- Chuyển các trường hợp quota, responder rỗng/không có/lỗi và token `[HANDOFF]` sang gợi ý khi mode là `SUGGEST`.
- Giữ cách tự chuyển và nội dung thông báo hiện tại khi cấu hình `AUTO`.

## API và DB liên quan

- `POST /api/chat/conversations/{id}/messages` như cũ. Message response thêm `handoffSuggested` (boolean, mặc định `false`) và `reasonCode` (nullable); metadata null hoặc JSON lỗi trả `false`/`null`.
- Gợi ý có thể nằm trên tin AI hoặc SYSTEM. Reason code: `SENSITIVE_TOPIC`, `REPEATED_KB_MISS`, `AI_HANDOFF_SUGGESTED`, `AI_DAILY_LIMIT_REACHED`, `AI_EMPTY_REPLY`, `AI_UNAVAILABLE`, `AI_RUNTIME_FAILURE`.
- SYSTEM gợi ý lỗi/quota không phát notification và không tăng unread của Member; lặp gợi ý SYSTEM cùng nguyên nhân không lưu trùng. Không thay schema DB.
- Khi Member chọn gặp nhân viên, gọi `POST /api/chat/conversations/{id}/escalate`; BE lấy danh tính từ session, chuyển `handlerType=STAFF` và retry không tạo SYSTEM trùng.

## FE cần làm

- Hiển thị hai lựa chọn sau gợi ý: tiếp tục trò chuyện AI hoặc “Gặp nhân viên”.
- Làm nổi nút “Gặp nhân viên” khi tin AI/SYSTEM mới nhất có `handoffSuggested=true`; không suy diễn từ nội dung text hoặc `reasonCode` đơn lẻ.
- Chỉ gọi `/escalate` khi Member chọn chuyển. Sau khi chuyển, cập nhật trạng thái sang STAFF theo response và ngừng kỳ vọng AI trả lời.
- Dùng OpenAPI snapshot tại [artifacts/fe-handoff/openapi.json](artifacts/fe-handoff/openapi.json) và contract mục 16 trong [docs/01-API-CONTRACT-FE-BE.md](docs/01-API-CONTRACT-FE-BE.md).

## Việc còn lại

- FE triển khai và xác nhận UI với lựa chọn của Member.
- Kiểm HTTP + DB sau commit trên PostgreSQL độc lập; chưa xác nhận production/Render hoặc provider thật.
- Cấu hình Render để trống sẽ dùng `SUGGEST`; chỉ đặt `AUTO` khi cần giữ hành vi tự chuyển cũ.
