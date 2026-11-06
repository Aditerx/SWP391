# SCMS bàn giao FE — AI Chat — 2026-11-07

## Kết quả hiện tại

- Đã dựng khung backend provider-neutral, mock không cần key và Anthropic Messages API có thể cấu hình; feature flag mặc định tắt, hội thoại mới mặc định STAFF.
- Đã thêm Member handoff `POST /api/chat/conversations/{id}/escalate`, polling/API chat hiện hữu giữ nguyên. Chưa có cơ sở dữ liệu local được dựng hoặc FE integration/browser smoke.
- Kết quả test baseline và sau thay đổi được ghi tại [docs00 Active](docs/00-MASTER-CHECKLIST.md#active); đây là local test, không là nghiệm thu PostgreSQL, FE hay deploy.

## Quá trình và quyết định giữ/bỏ

1. Đối chiếu contract chat mục 16, code và hai schema; `chat_messages` đã hỗ trợ `sender_type=AI` và `metadata`, nên không thêm migration.
2. Giữ lựa chọn Anthropic Messages API sau interface mỏng; bỏ SDK nặng vì Spring `RestClient` đủ cho request/timeout.
3. Giữ STAFF làm mặc định khi mở chat cho đến khi Owner chốt; config AI chỉ để bật mock cục bộ và handler AI tường minh.
4. Thêm giới hạn transcript, che tin nghi chứa secret, metadata hiệu năng, không log nội dung; lỗi AI chuyển STAFF an toàn.

## API và DB liên quan

- Luồng Member hiện hành: `POST /api/chat/conversations`, `POST /api/chat/conversations/{id}/messages` với `content`/`clientMessageId`, poll `GET /api/chat/conversations/{id}/messages?afterId=...`.
- Nút gặp nhân viên gọi `POST /api/chat/conversations/{id}/escalate` không cần body; BE lấy Member từ phiên, chỉ cho chủ cuộc hội thoại, trả `CHAT_CONVERSATION_NOT_FOUND` (404) hoặc `CHAT_CONVERSATION_CLOSED` (409) khi phù hợp. Response trả `handlerType=STAFF`; tin SYSTEM/thông báo do BE ghi.
- AI reply xuất hiện như `senderType=AI`, `metadata` có model/latency/token counts nếu có. Render text thuần; tiếp tục xử lý 429 `CHAT_RATE_LIMITED` và header `Retry-After` theo contract.
- Không thay DB. Tái dùng chat tables/notification hiện hữu; không dùng `ai_consultations`.

## FE cần làm

- Chưa xác định source FE hiện hành: `FE_Project-main` đã được đánh dấu out-of-date; không chọn `SCMS_demo_fe_v1.1` thay Owner. Xác nhận repo/thư mục đang deploy trước khi tích hợp.
- Trong Member Support, mở/lấy active conversation, gửi `clientMessageId` duy nhất mỗi tin, poll theo `afterId`, hiển thị riêng MEMBER/AI/STAFF/SYSTEM, gắn nhãn “Trợ lý AI”, thêm nút gặp nhân viên và render text thuần.
- Sau handoff refresh conversation/messages để đổi trạng thái xử lý; không gửi memberId/role/actor trong request.

## Việc còn lại

- Owner/nhóm xác nhận FE source hiện hành, handler mặc định AI hay STAFF, nhà cung cấp/model dùng sau này, và nội dung trung tâm đã duyệt để làm knowledge base tĩnh. Chưa nhét docs nội bộ vào prompt.
- Dựng/đăng nhập PostgreSQL local riêng bằng local credential, chạy `database/schema_postgres.sql` chỉ trên DB mới trống, tạo `.env` local với `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; hiện local PostgreSQL yêu cầu mật khẩu chưa được cung cấp. Không dùng `.env` chưa xác minh hay Supabase.
- Bật local mock bằng `SCMS_AI_CHAT_ENABLED=true`, `SCMS_AI_CHAT_PROVIDER=mock`, `SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER=AI`; không cần key. Anthropic chỉ bật sau khi có `ANTHROPIC_API_KEY` trong local ignored environment và chọn `SCMS_AI_CHAT_MODEL` hợp lệ; không chạy provider test.
- Sau khi có DB credential/source FE: khởi chạy BE, kiểm `/actuator/health`, Swagger, login demo rồi `/api/auth/me`, kiểm luồng polling/handoff thật, chạy lại full Maven suite và đánh giá setup FE local.
