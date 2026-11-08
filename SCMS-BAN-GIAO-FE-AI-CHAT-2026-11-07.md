# SCMS ban giao FE - AI Chat - 2026-11-07

## Ket qua hien tai
- Provider faq nay chao hoi/cam on/tam biet va tra loi tu KB theo cum tu, token, ho tro dong `Tu khoa:`; hoi chu de co dau/khong dau deu match. Khong khop lan dau danh dau `metadata.fallback=true`, goi y toi da 5 chu de; lan khong khop ke tiep, chu de nhay cam hoac member yeu cau nguoi that thi handoff. SYSTEM hien thi: “Mình đã chuyển cuộc trò chuyện cho nhân viên hỗ trợ. Nhân viên sẽ phản hồi bạn sớm nhất có thể.”; ma ly do luu `metadata.reasonCode`.

- Provider `deepseek` duoc bat bang `SCMS_AI_CHAT_ENABLED=true`, `SCMS_AI_CHAT_PROVIDER=deepseek` va key trong secret `DEEPSEEK_API_KEY`. Model mac dinh `deepseek-flash`, API Chat Completions qua Spring `RestClient`, thinking disabled bang `{"type":"disabled"}`.
- System prompt tieng Viet co dau nam tai `src/main/resources/ai/system-prompt.txt`, doc bang UTF-8 tuong minh; yeu cau luon tra loi tieng Viet co dau. Knowledge base `src/main/resources/ai/knowledge-base.md` duoc doc UTF-8, khong sua noi dung; gioi han do dai mac dinh 12.000 ky tu, co log canh bao neu cat/thieu/rong. Transcript da loc du lieu nhay cam, toi da 20 tin/8.000 ky tu. Quota 30 cau tra loi AI/member/ngay theo `Asia/Ho_Chi_Minh`, rate limit chat 20 tin/phut giu nguyen.
- Token `[HANDOFF]` duoc tach truoc khi hien thi. Backend luu phan tra loi AI, chuyen handler sang STAFF va them tin SYSTEM.
- Knowledge base duoc giu nguyen theo yeu cau; bot khong suy doan va de nghi gap nhan vien neu thieu thong tin. Chua kiem provider that, FE/browser, PostgreSQL hoac Render.

## Qua trinh va quyet dinh giu/bo

1. Doi chieu docs DeepSeek chinh thuc: base URL `https://api.deepseek.com`, `POST /chat/completions`, header `Authorization: Bearer`, roles system/user/assistant, `max_tokens`, `temperature`, `thinking`, `stream`; doc `choices[0].message.content` va `usage.prompt_tokens/completion_tokens`.
2. Giu RestClient khong SDK; thinking `disabled`; chi log model, latency, token counts va loai loi, khong log noi dung hay key.
3. Giu provider mock/faq khong tao HTTP client. DeepSeek request factory/client chi co khi provider la deepseek va key khong rong.
4. Loi 401/402/429, 5xx, timeout/IO va key thieu duoc xu ly khong mat tin member; chi retry toi da mot lan voi 5xx hoac timeout/IO. Sau do fallback tieng Viet va handoff STAFF.
5. Tim thay nguyen nhan chu mock vo dau o gia tri mac dinh trong `application.properties` bi double-encode (`Xin chÃ o`); `.env` khong override. Da sua gia tri properties bang Unicode escapes va xac dinh compiler UTF-8; API JSON serialization giu nguyen dau trong MockMvc. `spring-boot:run` khoi dong va health 200; GET messages live tra 401 vi khong co phien dang nhap, nen khong co du lieu live de doc.

## API va DB lien quan

- FE giu `POST /api/chat/conversations/{id}/messages` voi `content`/`clientMessageId`; poll `GET /api/chat/conversations/{id}/messages?afterId=...`. Tin AI co metadata `model`, `latencyMs`, `inputTokens`, `outputTokens`.
- Handoff backend dung workflow hien huu; FE render sender AI va SYSTEM, sau SYSTEM refresh conversation de doc `handlerType=STAFF`. Endpoint member tu yeu cau nhan vien van la `POST /api/chat/conversations/{id}/escalate`.
- Khong them migration; dung lai `chat_messages.metadata` va conversation handler.

## FE can lam

- Hien thi tin AI/SYSTEM dang text thuong; FE khong xu ly `[HANDOFF]` vi backend da tach token.
- Khong gui key, role, actor hay memberId; tiep tuc xu ly `429 CHAT_RATE_LIMITED` va `Retry-After`.

## Viec con lai

- `mvnw clean test` PASS local working tree: 451 tests, 0 failure/error, 28 skipped (423 pass), tang 5 test so voi baseline 446. Test FAQ/mock/deepseek dung client gia lap, khong goi mang that; skip khong tinh pass.
- Bien moi truong: `DEEPSEEK_API_KEY` (secret); `SCMS_AI_CHAT_DEEPSEEK_BASE_URL` (`https://api.deepseek.com`); `SCMS_AI_CHAT_DEEPSEEK_MODEL` (`deepseek-flash`); `SCMS_AI_CHAT_DEEPSEEK_TIMEOUT_SECONDS` (12); `SCMS_AI_CHAT_DEEPSEEK_MAX_TOKENS` (400); `SCMS_AI_CHAT_TEMPERATURE` (0.2); `SCMS_AI_CHAT_MAX_KNOWLEDGE_BASE_CHARACTERS` (12000); `SCMS_AI_CHAT_MAX_INPUT_CHARACTERS` (8000); `SCMS_AI_CHAT_DAILY_LIMIT_PER_MEMBER` (30); cung `SCMS_AI_CHAT_ENABLED`, `SCMS_AI_CHAT_PROVIDER`, `SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER`.
- Gia tham khao DeepSeek ngay 08/11/2026: Flash peak $0.30/1M input token cache miss va $1.20/1M output token; off-peak $0.15/$0.60. Vi du 1.000 input + 400 output token, 30 luot/ngay x 30 ngay: khoang $0.70 peak hoac $0.35 off-peak/member/thang; token thuc va cache hit co the giam gia. Gia co the thay doi, can kiem tra [bang gia chinh thuc](https://api-docs.deepseek.com/quick_start/pricing/).
- Chua xac nhan FE source hien hanh hoac deploy. Tham khao docs: [Your First API Call](https://api-docs.deepseek.com/); [Chat Completions](https://api-docs.deepseek.com/api/create-chat-completion/); [Error Codes](https://api-docs.deepseek.com/quick_start/error_codes/); [Rate Limit](https://api-docs.deepseek.com/quick_start/rate_limit/).
