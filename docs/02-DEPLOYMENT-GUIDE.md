# SCMS — Vận hành và kiểm thử

## 1. Nguồn và cấu hình

[docs00](00-MASTER-CHECKLIST.md) giữ trạng thái; [docs04](04-DEBUG-NOTE.md) giữ quyết định; [docs01](01-API-CONTRACT-FE-BE.md) giữ contract FE. File này chỉ giữ cách vận hành, không chép nhật ký từng lần chạy.

Java 17/Spring Boot 3.3.5, Maven wrapper, PostgreSQL/Supabase; Dockerfile ở root là cách build hiện tại. SQLServer có scripts nhưng runtime chưa nghiệm thu. Secret/.env/backup riêng tư ngoài Git, runner nghiệp vụ không nạp .env cloud. Giữ ddl-auto=none và OSIV=false.

<a id="current-frontend"></a>
**FE hiện hành — Owner cập nhật12/11/2026:** <https://vibesports.pages.dev> là site demo mới; <https://scms-demo-04nov.pages.dev> được giữ đến hết buổi demo. Theo ghi chú FE local `gui-backend-doi-link-vibesports.md` (không thuộc bộ phát hành), Owner đã đặt `FRONTEND_URL=https://scms-demo-04nov.pages.dev,https://vibesports.pages.dev` và đổi `FRONTEND_PAYMENT_RESULT_URL` từ `https://scms-demo-04nov.pages.dev/member/invoices` sang `https://vibesports.pages.dev/payment-result`. Source đọc danh sách origin bằng dấu phẩy; cookie cross-site tiếp tục cần SameSite=None/Secure và FE gửi credentials. Không đổi env/deploy thay Owner trong lượt này.

Kiểm public HTTP12/11: OPTIONS GET auth-me và POST login từ cả hai origin trả200, Allow-Origin đúng origin/Allow-Credentials:true; health200/UP. GET auth-me không phiên401 chỉ chứng minh cổng đăng nhập. GET Return không tham số, không theo redirect, trả302 tới site mới `/payment-result?invoiceCode=&status=97`: xác nhận đích redirect cấu hình, **không phải thanh toán thành công**; source Return chỉ đọc/kiểm chữ ký, không ghi Paid. Runtime OpenAPI có Google `isNewAccount` và chat close, không có PATCH content sau buổi của Session2; không suy working tree local đã deploy. `/actuator/info`404 nên SHA deploy chưa xác định; FE build/repo/API origin browser thật vẫn cần đối chiếu.

Browser CUA tích hợp từng trả apps/browsers rỗng/iab unavailable; HTTP FE403 `error code:1010` không là kết quả của browser Owner hoặc Playwright. Dùng Playwright MCP qua extension để đọc tab hiện có; kết quả/giới hạn hiện hành ở Active/QA. Đọc màn login không thay kiểm cookie phiên, Google/OTP, giao dịch sandbox, AI/mail hoặc nghiệp vụ sau login. Kịch bản chuẩn bị demo15phút tại [SCRIPT](../SCMS-SCRIPT-DEMO-BE.md#demo-fe-15min-20261112); `FE_Project-main` vẫn là tham khảo cũ.

<a id="demo-tech-check-20261112"></a>
### Kiểm dữ liệu thực tế trước rehearsal — dành cho agent/BE

“Runtime” là dữ liệu/phiên/bản ứng dụng đang chạy lúc kiểm, có thể khác fixture trong repo. Phiếu này dành cho agent/BE chọn đúng case trước demo; Owner không phải tự điền ID, version, build SHA vào tờ copy/paste. Note của người trình diễn chỉ giữ tài khoản, tên lớp/hồ sơ dễ tìm trên FE, văn bản mẫu và phương án dự phòng.

| Nhóm cần kiểm | Agent/BE tự đối chiếu |
|---|---|
| Nguồn và phiên | FE build/API origin thực, BE build nếu công bố; auth-me đúng actor/role/center/authority. Không ghi cookie/token/password vào tài liệu công khai. |
| Thanh toán | Lớp còn chỗ/cutoff, Member đủ điều kiện; lấy ID/amount từ BE, tạo Pending lúc demo; có Invoice Paid/Enrollment dự phòng đã đọc lại. |
| Lifecycle/Refund | Lớp/lần dời/cutoff đúng, chọn hồ sơ có state phù hợp; lấy version/receiver/attempt hiện tại qua GET, giữ chứng từ và nguồn đối soát DEMO độc lập. Không bắt Owner tự tăng version. |
| Attendance | Buổi đúng giờ/phân công/quyền học, chọn Member riêng để tránh ghi đè; ca6 kết thúc18:00, ca7 bắt đầu18:15; chuẩn bị lịch sử nếu rơi vào khoảng trống. |
| Chat | Đúng Member và staff DEMO, đúng hội thoại/handler/state; các ID nội bộ do agent/BE lấy, Owner chỉ cần nhận ra cuộc trò chuyện trên UI. |

<a id="playwright-vscode-demo-20261112"></a>
### Browser cho Codex VS Code — Playwright MCP qua extension

Owner đã chọn cấu hình Playwright MCP để **chỉ đọc/chụp site**. Server `playwright` trong cấu hình Codex người dùng dùng Node/npx sẵn có và pin `@playwright/mcp@0.0.85` với `--extension`; output về raw runs bị Git bỏ qua. Các cấu hình MCP khác được bảo toàn. Kiểm package startup/MCP initialize/tools-list là kiểm cài đặt; sau đó phải list tab → snapshot → screenshot để xác nhận đúng surface. Kết quả kết nối mới nhất ở Active/QA, không suy thành nghiệm thu nghiệp vụ FE.

Client chỉ bật `browser_tabs`, `browser_snapshot`, `browser_take_screenshot`, `browser_navigate`, `browser_console_messages`, `browser_network_requests`. Chưa bật tool điền/click/submit/evaluate. Nội dung phiên, cookie/token và request nhạy cảm không đưa vào QA công khai. Owner đã cài extension; không cần cài lại khi tab đang kết nối được.

Owner cài [Playwright Extension của Microsoft](https://chromewebstore.google.com/detail/playwright-extension/mmlmfjhmonkocbjadbfplnigmagldckm), mở tab VibeSports trong Chrome/Edge, rồi chọn tab khi extension hỏi kết nối. Codex VS Code phải nạp được server mới trong phiên; sau khi nạp mới kiểm list tab/snapshot/screenshot, không nhận cài đặt thành PASS browser. Nếu cần prompt tiếp tục, gửi trong chat theo yêu cầu Owner.

Nguồn: [Microsoft — extension dùng tab/phiên đang mở](https://github.com/microsoft/playwright/blob/main/packages/extension/README.md), [OpenAI — MCP dùng chung cấu hình Codex CLI/IDE](https://learn.chatgpt.com/docs/extend/mcp?surface=cli). Chrome DevTools MCP cũng hỗ trợ browser đang chạy qua autoConnect/CDP, nhưng chưa cấu hình trong lượt này. TinyFish có trong kho plugin nhưng chưa được cài/kết nối; không coi nó là Chrome local của Owner. Các thảo luận cộng đồng chỉ là tham khảo, không dùng mẹo sao chép identity/pipe của phiên khác để vượt thiếu kết nối browser.

<a id="local-scope-demo-login"></a>
### Đăng nhập demo scope training-results bằng Swagger local

`login.manager1` trong kịch bản nghĩa là object `manager1` bên trong mục `login` của file payload. Nó không phải username hoặc endpoint. Body gửi POST `/api/auth/login` chỉ gồm hai field `email` và `password` lấy từ object đó. Tài khoản synthetic này nằm trong DB demo local riêng, dùng với BE `http://127.0.0.1:18082`; chưa có căn cứ dùng nó trên trang `/login` của FE online.

1. Mở JSON đăng nhập local `artifacts/runs/training-scope/2026-11-11-session1-2/login-manager1.private.json`. File local này chỉ chứa object có `email` và `password`; copy toàn bộ nội dung. File bị Git bỏ qua, không thuộc gói FE công khai; không chép credential vào tài liệu/log/ảnh bàn giao.
2. Mở <http://127.0.0.1:18082/swagger-ui/index.html>. Trong nhóm `auth-controller`, mở **POST `/api/auth/login`**, chọn **Try it out**. Thay toàn bộ nội dung ô **Request body** bằng JSON vừa copy, giữ media type `application/json`, rồi chọn **Execute**. Không nhập chữ `login.manager1` hoặc bọc thêm key `manager1`/`login` quanh object.
3. Đối chiếu **Request URL** là `http://127.0.0.1:18082/api/auth/login`. Trong **Server response**, Code200 cùng Response body `userId:7`, `role:"CenterManager"`, có authority `MANAGE_CLASSES` là đúng trong phạm vi đăng nhập local. Không lấy phần Responses liệt kê mã mẫu làm kết quả thực thi. Không cần copy cookie/token vào Authorize để thay bước đăng nhập này.
4. Trên cùng Swagger và cùng host `127.0.0.1`, mở **GET `/api/auth/me` → Try it out → Execute**. Code200, userId7/CenterManager/có MANAGE_CLASSES là đúng: phiên vừa đăng nhập được BE nhận ở request tiếp theo. Nếu401, kiểm lại Request URL/host và thực hiện login trên cùng Swagger trước; chưa coi các ca scope phía sau là đã có phiên.
5. Tiếp tục GET `/api/training-results`:200 với resultId `[3,2,1]` là đúng cho Manager center1; thêm `sessionId=3` nhận200 `[]` là đúng cho filter ngoài center. Đây là kết quả mong đợi để Owner đối chiếu, không tự là browser PASS. Đổi vai trong kịch bản bằng cách lấy object tương ứng trong mục `login`, POST lại rồi xác minh auth-me đúng actor mới.

Nhãn thao tác đã đối chiếu bundle Swagger do BE local phục vụ và mã nguồn chính thức của [Try it out](https://github.com/swagger-api/swagger-ui/blob/main/src/core/components/try-it-out-button.jsx), [Execute](https://github.com/swagger-api/swagger-ui/blob/main/src/core/components/execute.jsx). Chưa quan sát màn hình browser của Owner. Kết quả API kiểm lại ngày12/11 ở Active/QA; không suy thành FE online đã dùng bản sửa local hoặc thành nghiệm thu browser. Demo đọc1.2 không phụ thuộc cửa giờ attendance; không reset dữ liệu vì qua ngày.

<a id="local-r2-content-demo-20261112"></a>
### Demo R2: Coach sửa content sau buổi, Member đọc lại — local12/11

Runtime synthetic riêng: <http://127.0.0.1:18085/swagger-ui/index.html>, DB local riêng, Clock ứng dụng thực/giờ VN và scheduler tắt trong harness demo. Không dùng credential này trên FE online. Fixture này không phải bundle Supabase hoặc quan sát một buổi đã tổ chức thật. Readiness API/nguồn runtime theo Active/QA; browser và thao tác Owner vẫn riêng.

Session1 ngày12/11/2026,06:00–07:30; result1 thuộc Member2/Coach1. Cửa PATCH thành công là **từ07:30 ngày12/11 đến trước00:00 ngày13/11, giờ VN**. Trước07:30,400 RESULT_CONTENT_NOT_OPEN là đúng và chỉ xác nhận chưa mở cửa, chưa là demo sửa content thành công. Sau cutoff,403 là đúng; không đổi Clock/ngày/state để làm demo. Nếu chưa tới cửa, Owner có thể chờ hoặc chọn skip demo2.2 rõ ràng.

1. Mở [D:\Projects\SWP391\artifacts\runs\training-content-after\2026-11-12-r2\login-coach.private.json](D:/Projects/SWP391/artifacts/runs/training-content-after/2026-11-12-r2/login-coach.private.json), copy toàn bộ object email/password. Trong Swagger local18085, nhóm `auth-controller` → POST `/api/auth/login` → **Try it out**, thay Request body bằng object đó → **Execute**; không bọc thêm key login/coach hoặc dùng Authorize thay đăng nhập. Request URL phải cùng `127.0.0.1:18085`. Code200/userId1/role Coach/có RECORD_RESULT là đúng cho login local. Tiếp GET `/api/auth/me` → Try it out → Execute,200/userId1/Coach/isFirstLogin:false là đúng: phiên hiện hành được BE nhận; nếu401, kiểm host và login cùng Swagger trước bước sau.
2. Trong nhóm `training-result-controller`, GET `/api/training-results`, nhập `sessionId=1`, để trống filter khác → Execute.200 có resultId1/memberId2 và resultId2/memberId3 là đúng cho own-author Coach; chọn result1, ghi nhận content ban đầu `DEMO R2 - Nhận xét ban đầu`, attendanceStatus Absent, note `DEMO R2 - Giữ ghi chú` và recordedAt hiện có. Đây là fixture synthetic; Absent không có check-in time, không lấy content làm bằng chứng có mặt.
3. **Trong cửa giờ ở trên**, mở PATCH `/api/training-results/{resultId}/content` → Try it out, resultId `1`, body `{"content":"DEMO R2 - Bổ sung hướng dẫn kỹ thuật"}` → Execute.200/content đúng chuỗi vừa gửi, resultId1/coachId1/attendanceStatus Absent/note cũ/recordedAt cũ là đúng: nhận xét đã lưu mà các field gốc giữ nguyên. GET lại với sessionId1 xác nhận result1 có content mới. Nếu400/403/404, giữ response và kiểm giờ/phiên/phân công/ID theo contract; không chạy POST/correct để vượt lỗi. Gửi lại cùng body thành công là no-op theo bằng chứng test; không cần mở quyền audit cho Coach.
4. Mở [D:\Projects\SWP391\artifacts\runs\training-content-after\2026-11-12-r2\login-member.private.json](D:/Projects/SWP391/artifacts/runs/training-content-after/2026-11-12-r2/login-member.private.json), POST login lại với object đó trong cùng Swagger rồi GET auth-me:200/userId2/role Member là đúng cho phiên mới. GET training-results với sessionId1, bỏ memberId:200 chỉ có resultId1 và content đã sửa, attendanceStatus/note/recordedAt giữ nguyên là đúng cho Member own readback. Không trả resultId2. Báo lại status PATCH và content/ID đọc được; không gửi cookie hoặc credential.

Nhãn Try it out/Execute được kiểm từ bundle Swagger UI chính runtime này phục vụ; tên nhóm lấy từ OpenAPI runtime. Chưa thao tác browser thay Owner. [Bàn giao flow trước–sau](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-AFTER-SESSION-2026-11-12.md) giữ tác động tích hợp; không dùng hướng dẫn này để suy FE Pages/Render đã có R2.

<a id="local-shared-plan-history-demo-20261112"></a>
### Demo shared-plan history — local12/11

Dùng Swagger <http://127.0.0.1:18086/swagger-ui/index.html>, fixture synthetic riêng/Clock ứng dụng thực, không có cửa giờ chờ. Member A userId2 có enrollment Completed/Paid hợp lệ tại class1/plan1, đồng thời Cancelled/Paid tại class3/plan3 và private plan4. Member B userId3 chỉ có quyền tại class2/plan2. Đây là demo quyền đọc, không chứng minh khóa thật đã tổ chức hoặc phát sinh tiền thật; Active/QA giữ nguồn/bằng chứng. Không dùng credential này trên FE online.

1. Mở [D:\Projects\SWP391\artifacts\runs\shared-plan-history\2026-11-12-session2-3\login-member-a.private.json](D:/Projects/SWP391/artifacts/runs/shared-plan-history/2026-11-12-session2-3/login-member-a.private.json), copy toàn bộ object email/password. Trong Swagger local18086, nhóm `auth-controller` → POST `/api/auth/login` → **Try it out**, thay Request body bằng object vừa copy → **Execute**. Request URL phải là `http://127.0.0.1:18086/api/auth/login`.200/userId2/role Member là đúng cho login local. GET `/api/auth/me` → Try it out → Execute,200/userId2/Member/isFirstLogin:false là đúng: phiên hiện hành đã được BE nhận. Nếu401, kiểm host/port và login lại trên cùng Swagger trước bước sau; không đưa cookie/password vào báo cáo.
2. Nhóm `training-plan-controller`, GET `/api/training-plans` với `classId=1`, bỏ các filter khác → Execute.200 có duy nhất planId1/classId1/memberId:null là đúng cho shared plan qua quyền Completed/Paid của A. Lấy planId đó gọi GET `/api/training-plans/{id}`, id1:200 và cùng title/content/classId là đúng cho list/detail thống nhất. endDate của fixture đã qua nhưng không tự khóa lịch sử. Nội dung là bản đang lưu, không có bản version tại ngày học mới.
3. Vẫn phiên A, GET list `classId=3`:200 `[]`; GET detail id3:403 là đúng cho Cancelled dù vẫn có Paid. GET detail id4:200/memberId2/classId:null là đúng cho own private history. Các kết quả này không mở flow hủy/hoàn Normal mới và không cấp quyền ghi cho Member.
4. Mở [D:\Projects\SWP391\artifacts\runs\shared-plan-history\2026-11-12-session2-3\login-member-b.private.json](D:/Projects/SWP391/artifacts/runs/shared-plan-history/2026-11-12-session2-3/login-member-b.private.json), POST login lại với object này rồi GET auth-me:200/userId3/Member là đúng cho actor B. GET list `classId=1` trả200 `[]`, detail id1 trả403 là đúng vì B không có quyền lớp của A; GET detail id2 trả200 là đúng cho lớp riêng của B, xác nhận không phải lỗi đăng nhập. Báo status và planId/classId đọc được, không gửi credential/cookie.

Nhãn Try it out/Execute kiểm từ bundle Swagger runtime; tag/route lấy từ OpenAPI cùng source đã kiểm. Agent API readiness không thay browser/Owner demo. [Bàn giao FE riêng](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-SHARED-PLAN-HISTORY-2026-11-12.md) giữ ảnh hưởng client filter/cache/nullable; thao tác4bước này không mutation plan/quyền học/chứng từ.

| Nhóm biến | Ý nghĩa / default source |
|---|---|
| PORT | 8080 nếu môi trường không cấp |
| DB_URL,DB_USERNAME,DB_PASSWORD | JDBC PostgreSQL/TLS phù hợp, credential tách riêng; Supabase pooler username chứa project reference |
| SCMS_PAYMENT_EXPIRE_MINUTES | 5; fallback PAYMENT_EXPIRE_MINUTES |
| SCMS_BOOKING_BLOCK_ENABLED,SCMS_ATTENDANCE_LOOKBACK_DAYS | false và3 ở source |
| FRONTEND_URL hoặc scms.frontend-origins | Exact FE origins; source có thêm localhost/127.0.0.1 pattern |
| SESSION_COOKIE_SAME_SITE,SESSION_COOKIE_SECURE | lax/false; cross-site HTTPS cần none/true và browser kiểm thực tế |
| VNPAY_TMN_CODE,VNPAY_HASH_SECRET | Merchant/key trong secret store |
| VNPAY_PAY_URL,VNPAY_RETURN_URL,FRONTEND_PAYMENT_RESULT_URL | Gateway/HTTPS BE return/trang kết quả FE đúng môi trường |
| MAIL_HOST,MAIL_PORT,MAIL_USERNAME,MAIL_APP_PASSWORD | smtp.gmail.com/587 mặc định; credential ngoài Git |
| MAIL_FROM_EMAIL | Địa chỉ người gửi đã xác minh; nếu không đặt dùng spring.mail.username. Tách khỏi SMTP login khi dùng Brevo |
| REGISTRATION_DEMO_OTP | Để trống production; mã cố định chỉ cho demo đăng ký được chọn rõ |
| CLOUDINARY_CLOUD_NAME,CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET | Nếu dùng avatar, secret ngoài Git |
| SCMS_AI_CHAT_ENABLED | false; phải bật rõ ràng mới có responder AI |
| SCMS_AI_CHAT_PROVIDER | mock mặc định để dev FE không cần key; Anthropic chỉ khi chọn rõ |
| SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER | STAFF mặc định; chọn AI riêng cho local mock khi muốn kiểm UI trợ lý |
| ANTHROPIC_API_KEY,SCMS_AI_CHAT_MODEL | Chỉ cần cho provider Anthropic; key lưu secret store, model là model ID đang khả dụng trong Anthropic Console |
| SCMS_AI_CHAT_TIMEOUT_SECONDS,SCMS_AI_CHAT_MAX_TOKENS,SCMS_AI_CHAT_MAX_INPUT_CHARACTERS | timeout12s, đầu ra tối đa400 token, lịch sử tối đa8000 ký tự gửi provider |

Đây là default/source, không chứng nhận dashboard đã đặt đúng; external Spring configuration có thể override. Cùng host/password chưa chứng minh cùng Supabase project. HikariCP là pool JDBC của ứng dụng, khác Supabase pooler; source default5connection/timeout30000ms. Fingerprint chỉ đối chiếu target hiệu lực, không là chứng nhận toàn API.

### Chat AI local (không cần provider key)

`ChatAiResponder` chỉ được đăng ký khi `SCMS_AI_CHAT_ENABLED=true`. Để thử luồng FE với câu trả lời cố định, dùng DB PostgreSQL local đã dựng theo bước Local workflow, rồi trong PowerShell khởi chạy backend với:

```powershell
$env:SCMS_AI_CHAT_ENABLED = 'true'
$env:SCMS_AI_CHAT_PROVIDER = 'mock'
$env:SCMS_AI_CHAT_NEW_CONVERSATION_HANDLER = 'AI'
.\mvnw.cmd spring-boot:run
```

Khi tắt feature flag, hội thoại mới luôn là STAFF như trước. Nếu bật mock nhưng để handler STAFF thì chat vẫn chuyển đến nhân viên; lựa chọn handler mặc định hiện được giữ STAFF cho đến khi Owner chốt. Mock không gọi mạng. Provider Anthropic dùng `POST /v1/messages`, `x-api-key`, `anthropic-version`, `model`, `max_tokens`, `system` và `messages` theo [Messages API chính thức](https://platform.claude.com/docs/en/api/go/messages/create); chọn model ID có hiệu lực trong Console và chỉ đặt key qua `.env` ignored hoặc secret store. Không cần dependency SDK riêng.

Mỗi request chỉ nhận tối đa20 tin mới nhất, giữ lịch sử trong giới hạn ký tự và loại tin SYSTEM. Tin có dấu hiệu password/OTP/token hoặc số thẻ được bỏ toàn bộ trước khi gửi. Không gửi member ID hoặc dữ liệu hội thoại khác. Timeout tối đa cấu hình12giây, thử lại một lần với timeout/lỗi HTTP5xx; lỗi cuối chuyển hội thoại sang STAFF bằng SYSTEM message. Log chỉ ghi loại exception và conversation ID, không ghi nội dung prompt, response hoặc key. Bộ lọc theo mẫu là lớp giảm thiểu, không thay thế rà soát dữ liệu; chatbot vẫn chưa được phép xử lý dữ liệu tài khoản/thanh toán.

<a id="testing"></a>
## 2. Kiểm thử

Chọn Dxx/điều kiện nghiệm thu trước, chuẩn bị actor/fixture/Clock/trạng thái trước-sau. Test đích khi sửa; hoàn tất nhóm BE chạy `.\mvnw.cmd clean test` và kiểm liên quan. Count lấy từ XML, skip tách riêng; chỉ lặp kiểm rộng khi có diff/lỗi/nghi vấn mới. H2 không thay PostgreSQL/locking thật.

Nhóm có phiên phải login và kiểm `/api/auth/me`, sau đó đúng người/sai người/sai role/khác center.401 do mất phiên là BLOCKED của setup, không phải pass quyền. Tiền/quyền/credit/lịch cần response và state sau commit, gồm biên5phút/12h/[start,end), retry/race. Anonymous smoke báo gộp, không liệt kê71dòng401 vào nghiệm thu nghiệp vụ.

### Local workflow

Session1–3 có runner chung `python scripts/test-session123-postgres.py --suffix <ma_moi> --output-dir <thu_muc_run_moi>`; dùng Java17 và container `scms-phases-20261027` bind127.0.0.1:55440. Runner chỉ tạo DB regression mới từ bootstrap minimal, mỗi suite một DB, sao source không.env, kiểm HTTP/auth-me/response và state/audit/race bằng các test hiện hành. Không dùng bundle demo có guard ngày hoặc sửa Clock runtime. Kết quả từng case nằm trong `result.json`; source fingerprint và giới hạn theo Active/QA. Thiếu suite/skip/failure không được tính PASS.

Smoke phát hành xem [quy trình Session1–3](#session123-release-smoke-20261113).

**Snapshot kiểm foundation10/11:** phần lõi trước nhóm hoàn thiện; checkpoint hiện hành [docs03](03-TODO.md#normal-phase1-complete-20261110), bundle đầy đủ ở mục demo-rebuild bên dưới. Chỉ làm trong bản sao source hiện hành không.env; test đích `NormalLifecycleHttpTest,NormalEnrollmentMembershipIntegrationTest,EnrollmentServiceTest,PublicationResourceHttpTest`, rồi clean test. Không dùng runner lấy committed checkout cũ để chứng minh working tree mới.

Suite `NormalLifecycleHttpTest` mặc định H2; opt-in `-Dscms.normal.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_normal_<fixture_moi>` chỉ nhận DB local đúng regex, ddl-auto=none. Tạo DB mới bằng `python scripts/test-phases-postgres.py --database scms_phases_normal_<fixture_moi> --minimal-fixture`; bao gồm replay21. Actor/Member/Coach/chuyên môn/lịch làm/12buổi và phiên login+auth/me do test tạo rồi kiểm trạng thái qua connection khác sau commit. Clock VN cố định/tiến có kiểm soát; callback ký test key, không gọi VNPay/ngân hàng/SMTP thật. Có thể xuất một OpenAPI từ cùng app bằng `-Dscms.normal.openapi=<duong_dan_json_trong_ban_sao>`.

R2 dùng `TrainingResultContentAfterSessionIntegrationTest` mặc định H2; opt-in `-Dscms.result-content.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` chỉ nhận DB loopback mới/ddl-auto=none. Chuẩn bị minimal fixture bằng runner trên; source copy không.env, test mock job/mail, kiểm login/auth-me, HTTP và snapshot sau commit. Race giữ center row bằng connection riêng, chờ cả hai request có DB lock wait rồi mới thả. Xuất schema bằng `-Dscms.result-content.openapi=<file_json>` từ app đã kiểm; chỉ đưa path/DTO R2 vào canonical, giữ phần khác không đổi. Giữ `TrainingResultContentIntegrationTest`/`TrainingResultServiceTest` làm regression POST/guard và legacy time; hoàn tất nhóm BE chạy clean test theo AGENTS. Demo Clock thực và fixture riêng theo [hướng dẫn R2](#local-r2-content-demo-20261112), không chạy fixture test thay Owner hoặc suy PG local thành browser/cloud PASS.

Kiểm migration21 độc lập: `python scripts/test-normal-lifecycle-migration.py --database scms_phases_normal_migration_<fixture_moi> --output artifacts/runs/normal-phase1/migration-validation.json`. Runner chỉ CREATE DATABASE mới trong container đã ghim; kiểm legacy NULL, giữ receipt/session, replay giữ quyết định, FK/giới hạn dời và rollback DDL khi kiểu/default/nullability sai. Schema tham chiếu kiểm riêng bằng test-demo-schema-postgres; không lấy PASS cấu trúc làm PASS fixture mới. Migration21 đã nhúng generator reset nhưng chưa nằm trong schema root; chỉ chuẩn bị/kiểm local, không tự chạy cloud hoặc SQL16.

```powershell
docker start scms-phases-20261027
.\scripts\run-phases-demo.ps1
```

Cần Docker Desktop/PostgreSQL 17 container trên, bind **127.0.0.1:55440**, credential local-only `scms-local-only`. Runner tạo committed checkout không.env và DB mới; clean test →migration/replay/snapshot/FK guard →HTTP login/workflow/DB assertions. Clock tiến có kiểm soát; callback ký test key không là gateway live. Không chạy hai Maven clean cùng target.

HTTP test dùng `--minimal-fixture`: schema/catalog local và actor mới trong test, không dùng dữ liệu demo hoặc tài khoản seed lịch sử. Mỗi login xác minh `/api/auth/me`; trước mỗi nhóm xác minh lại5actor chính. Các request race dùng2worker sẵn sàng rồi mở chung latch; kiểm cả response và DB qua connection khác sau HTTP commit. Test phụ thuộc fixture trước sẽ skip nếu nhóm trước thất bại; báo BLOCKED, không coi đó là chặn đúng vai trò.

Runner minimal lấy bootstrap cấu trúc, bỏ grants mặc định để test tự định nghĩa role matrix. Chỉ trong DB local mới này, bỏ hai kiểm ngày quá khứ dùng wall-clock của trigger để không xung đột MutableClock; giữ guard role/phòng/sĩ số/FK và mọi kiểm thời gian của service. Không sửa các guard trong hai file SQL bàn giao Supabase. Kiểm account demo riêng bằng `DemoSchemaLoginPostgresTest`, đặt `-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh` và `-Dscms.demo.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_schema_<fixture>`; không dùng URL cloud.

D01/D09: unit/MockMvc gốc chạy H2, còn `PhaseWorkflowHttpTest` kiểm HTTP login thật trên PG. Fixture ngày sinh dùng Clock cố định. Race PATCH giữ row User bằng connection riêng, chờ cả2request ở DB lock rồi thả; hai field khác nhau phải cùng còn sau commit. Directory kiểm Admin center null, scope/contact và query count không tăng theo số Coach. Snapshot account Inactive được đọc trước khi phục hồi fixture. Case Member thiếu center chỉ chạy H2 vì guard PG từ chối lưu trạng thái đó; lỗi setup không tính là pass403/400. Tích hợp này dùng schema hiện có, không thêm migration hay mở quyền cloud.

RBAC theo RP-04/RP-36 và giáo án lớp: `RbacGuardIntegrationTest` mặc định chạy H2; opt-in `-Dscms.rbac.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` chỉ chấp nhận DB local đúng regex này, dùng ddl-auto=none. Chuẩn bị DB mới bằng bootstrap/runner minimal và các prerequisite hiện hành theo phần trên; không dùng DB workflow đã có actor để chạy chồng fixture. Fixture dùng môn chuẩn, subtype Coach/Member/Receptionist, ngày tham gia và Paid invoice có nguồn hợp lệ; không nới FK/NOT NULL để ép test qua. Kiểm target row lock bằng blocker metadata của đúng H2 hoặc PostgreSQL. Chạy trên bản sao không.env; kết quả/source/XML chuẩn ở docs00/QA, không suy PG local thành Supabase hoặc tự chạy migration cloud.

<a id="last-administrator-local-20261113"></a>
Session3.1 dùng `LastAdministratorIntegrationTest`: real HTTP loopback/login/auth-me, fixture Admin DEMO riêng, Clock ứng dụng thực, job/mail mocked và provider tắt. Mặc định H2; PostgreSQL opt-in `-Dscms.last-admin.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` trên DB mới do runner minimal phía trên chuẩn bị, `ddl-auto=none`. Không chạy bundle có anchor12/11 để tạo fixture13/11. Race giữ role rows bằng connection riêng, chờ cả hai HTTP request có DB lock wait rồi thả; kiểm response, số Admin đủ hai quyền và audit sau commit. Có thể lưu evidence không cookie/password qua `-Dscms.last-admin.evidence=<file_json>` và OpenAPI từ đúng app qua `-Dscms.last-admin.openapi=<file_json>`. Chạy bản sao source hiện hành không.env, test đích rồi clean test theo AGENTS; không dùng account hạ tầng hoặc thay authority cloud. Owner đã skip demo, không duy trì server/fixture Swagger cho mục này.

<a id="error-deprecation-local-20261113"></a>
Session3.2: `GlobalExceptionHandlerTest` chỉ kiểm mapping HTTP/body, không tính là authenticated business. Nhóm tích hợp đích là `RoomValidationIntegrationTest,NormalLifecycleHttpTest,NormalEnrollmentMembershipIntegrationTest`; đăng nhập/auth-me và đọc DB/audit sau commit. Room dùng opt-in `-Dscms.room-validation.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>`; Normal dùng `scms.normal.pg-url` theo regex phía trên, mỗi test class một DB minimal mới. Room H2 chỉ thêm unique exact-name cho fixture; PG dùng index partial/case-insensitive thật của migration13, không thay canonical SQL. `-Dscms.room-validation.openapi=<file_json>` xuất schema từ app được kiểm; chỉ đồng bộ các operation trong catalog, giữ DTO/luồng khác. Test version dùng synthetic Clock/fixture sẵn trong Normal regression, không đổi Clock ứng dụng hoặc chạy/vượt guard bundle12/11. Cả nhóm hoàn tất clean test theo AGENTS; skip opt-in/live không là PASS. `PhaseWorkflowHttpTest` kiểm hai alias status/extend Advanced bên cạnh quyền Paid/amount/credit hiện có, không deprecate nguyên route dùng chung hoặc gọi provider thật.

Ngoài exchanges, runner giữ `acceptance-cases.json` (Dxx/fixture/expected/SQL state và index response), `actors.json` và XML trong raw run. Sau khi kiểm kết quả, ghi vào QA chuẩn bằng lệnh dưới; lấy checkout từ `verification.json`. Chỉ chạy với test đã commit hoặc bản test được copy có hash khớp; không gọi kết quả checkout cũ là kết quả working tree mới.

```powershell
python scripts/record-authenticated-acceptance.py --run artifacts/runs/phases/<run> --checkout <checkout> --http-dir postgres-http
```

Script offline kiểm XML, hash test/source và compatibility schema trước khi thay `artifacts/qa/latest.json`; không login/probe live, không đọc.env hoặc tạo thêm bộ tài liệu bàn giao. Runner truyền `scms.test-source-sha256` vào lượt Maven PostgreSQL và XML lưu lại; collector yêu cầu khớp cả bản test hiện tại lẫn checkout, không ghép XML cũ với source test mới. Khi chạy Maven thủ công phải truyền cùng property từ SHA256 file test ngay trước lượt chạy. Case chỉ lưu response một lần trong `local.exchanges`, matrix tham chiếu index; SHA backend và hash bản test chưa commit được ghi riêng. SQL Timestamp trong snapshot có thể serialize UTC/Z, đối chiếu sang Asia/Ho_Chi_Minh với businessNow/expected. Những gate cloud giữ thời điểm/SHA lịch sử và không được nâng thành nghiệm thu mới.

Output `artifacts/runs/phases/<timestamp>` ignored; raw run cuối trước chuẩn hóa giữ local ở `artifacts/runs/phases/2026-10-27-verified`. Không mặc định có raw run trên máy khác. Communication riêng: `python scripts/test-communication-features.py`, output `artifacts/runs/communication/<run>`; DB/SMTP loopback, không email ra ngoài hoặc .env cloud.

### Kiểm gói QA đã giữ

```powershell
python scripts/audit-fe-contract.py
```

Mặc định offline, đọc một OpenAPI/QA để kiểm route/source và compatibility cơ bản của exchanges đã ghi. Không probe/login Render hoặc sinh thêm nhiều tài liệu. Có --openapi/--qa để chọn bản khác. Nullability/required/format/range/error cần kiểm riêng; không coi kiểm type/property là full JSON Schema.

Maintenance13/11: `qa_fingerprint.py` ghi `schema_postgres.sql:null` nếu root legacy đã bị xóa có chủ đích; không thay bằng file khác hoặc sửa hash lịch sử. Các file bắt buộc khác thiếu vẫn lỗi. Canonical DB hiện hành đã nằm trong fingerprint cây `database/`; không khôi phục root để chạy checker. Bằng chứng scoped Session3 mới chỉ được dùng khi clean/PG đạt, source copy khớp và toàn fingerprint/OpenAPI hash hiện hành khớp record. Wire examples cũ tiếp tục gắn source lịch sử; PASS offline chỉ route/status/type/property được kiểm, không nâng thành business mới/FE/cloud. Trạng thái blocker trước/sau, giới hạn và nguồn chuẩn tại Active/QA `session33Readiness20261113`.

Báo cáo đủ: `Dxx/case →actor/fixture →expected →response+state →PASS/FAIL/BLOCKED/NOT_RUN →môi trường/SHA →một tham chiếu`. Không lấy số request/assertion làm mức hoàn tất; chỉ mở đúng scenario/index trong QA khi cần.

## 3. Deploy

<a id="advanced-member-acceptance-20261114"></a>
### Advanced14/11 — SQL trước deploy và kiểm sau phát hành

Agent không tự áp query/migration trên DB đang dùng; Owner thực hiện bước SQL trước phát hành. [Migration24](../database/migrations/24_advanced_session_proposals_postgres.sql) là nâng cấp cộng thêm bảng, không reset/seed và không thay quyền học/chứng từ cũ. Kết quả thực thi và kiểm runtime xem [Active/QA](00-MASTER-CHECKLIST.md#active); kiểm cú pháp ngoại tuyến/H2 không thay nghiệm thu replay PostgreSQL.

Khi Owner mở bước SQL: review đúng file, chạy toàn bộ trên DB SCMS đích; kết quả mong đợi `result=PASS`, `migration_key=20261114_advanced_session_proposals_v1`. Nếu có SCMS_ADVANCED_PROPOSAL_DEPENDENCY/INCOMPATIBLE hoặc timeout thì dừng, giữ lỗi và kiểm hiện trạng, không bỏ guard. Owner gửi kết quả đã che thông tin riêng; agent chưa tự readback khi quyền query vẫn đang dừng. Chỉ tiếp tục deploy/kiểm live khi Owner xác nhận bước này và mở phạm vi tiếp theo.

Sau SQL và phát hành đúng source: GET health/build, auth-me đúng actor rồi kiểm luồng [bàn giao Advanced](reference/SCMS-BAN-GIAO-FE-ADVANCED-2026-11-14.md). Chỉ kiểm ghi trong phạm vi được mở; không tự tạo giao dịch/email hoặc sửa quyền. Merge code và test local không là deploy hoặc FE/browser PASS.

Checker offline giữ wire cũ POST `book-private`→201 sẽ báo REVIEW_REQUIRED vì route đó nay bị đóng. Giữ nguyên bằng chứng cũ; đối chiếu case mới đề xuất→Member Accept và OpenAPI hiện hành, không sửa response lịch sử hoặc gọi toàn checker là PASS.

**Đề xuất bundle chờ duyệt:** thêm bảng mới vào bootstrap và whitelist dựng lại theo thứ tự FK; đưa migration24 vào compiler; thêm fixture đề xuất có nhãn minh họa, cập nhật gate/count và kiểm fresh/replay/rollback rồi sinh bundle cùng anchor. Chưa sửa hoặc sinh lại bundle hiện tại; không cần reset DB đang dùng sau migration24.

<a id="rbac-fe-verification"></a>
### Kiểm phản hồi FE sau bản sửa RBAC

Đối chiếu source/merge/CI tại docs00/QA trước, rồi GET `/api/health` trên BE mà FE thật sự gọi: `buildCommit` phải thuộc bản đã phát hành. HTTP200 health chỉ xác nhận ứng dụng trả lời, không nghiệm thu nghiệp vụ hoặc migration Google. Không chạy lại SQL16/reset DB cho lượt này. Dùng tài khoản và record kiểm thử được phép; không thay role/trạng thái/mật khẩu tài khoản vận hành để thử.

1. **Coach/roster theo ảnh đã gửi:** đăng xuất/đăng nhập lại, kiểm GET `/api/auth/me`200 có role Coach và đúng user. Trong màn hình **Điểm danh**, chọn đúng lớp được giao (lớp23 chỉ dùng nếu đã xác minh phân công). Quan sát Network: GET `/api/classes/{id}/enrollments` trả200 danh sách đúng lớp là đúng; tuyến dự phòng `/api/enrollments?classId={id}` có thể không cần chạy khi tuyến đầu thành công. Lớp hoàn tất vẫn đọc roster, giữ banner khóa sửa attendance.403 ở lớp không được giao là đúng;401 hoặc role sai phải giải quyết phiên trước, không tính là kiểm quyền thành công.
2. **Giáo án/đánh giá:** qua màn hình hiện hành có gọi `training-plans`/`evaluations`, Coach được giao tạo/sửa/xóa record kiểm thử phải nhận201/200/204 và tải lại giữ đúng nội dung/target. Manager cố ghi cùng API phải403 dù có authority quản lý lớp; FE có thể ẩn thao tác theo quyền. Không đổi attendance correction D15 thành bài kiểm ghi giáo án.
3. **Member:** GET `/api/training-plans`200 chỉ gồm plan riêng của mình và plan chung lớp Normal Registered+Paid hợp lệ, đúng center; ID plan lớp/người khác403. Danh sách trống200[] chỉ đúng nếu không có plan đủ điều kiện. GET `payment-resolutions`403 vẫn là đúng quyền Member; FE cần ngừng gọi theo bàn giao RBAC lần1.
4. **Quản trị tài khoản:** bằng record kiểm thử riêng, Admin đủ authority được quản lý Manager; Manager quản lý target Manager/Admin hoặc chuyển role thường thành Manager phải403 và tải lại không có thay đổi. Kiểm đúng các tuyến users/staff/status/role/reissue cần dùng; không gọi cấp lại mật khẩu tài khoản thật để thử.
5. **Phiên đang mở (tùy fixture):** thay permission của role kiểm thử bằng Admin rồi request kế tiếp phải áp quyền mới; đổi role trả403 `SESSION_ROLE_CHANGED` và yêu cầu đăng nhập lại; Inactive trả403 `ACCOUNT_NOT_ACTIVE`; cấp lại/đổi password quản trị thu hồi phiên thành401. Chỉ dùng role/account kiểm thử riêng để không thay quyền của cả nhóm người dùng.

Chỉ hướng dẫn click/menu đã có ảnh hoặc giao diện hiện hành; nếu màn hình giáo án/quản trị khác hoặc chưa có nút tương ứng, lấy ảnh đúng vùng đó trước khi chỉ tiếp, không suy từ FE_Project-main cũ. Gửi kết quả theo role → thao tác → method/path → status/body liên quan và thông báo UI; che email/cookie/token. Không gửi OTP/mật khẩu. Nếu request lỗi, giữ response và xác minh auth/me cùng phiên trước khi đổi quyền. Xong nhóm kiểm FE thì Owner xác nhận kết quả; sau đó mới sắp xếp, giải thích và đề xuất phần tiếp theo, không tự mở policy mới.

<a id="google-login"></a>
### Google login/avatar nhập từ main

Entity User có google_sub; DB ứng dụng đã tồn tại cần [13_google_login_postgres.sql](../database/migrations/13_google_login_postgres.sql) trước deploy bản BE này, kể cả khi chưa bật Google login vì truy vấn User đã đọc cột đó. File chỉ thêm cột/index nếu chưa có, không reset/seed; đây là migration Google riêng, khác các file13 về room. Hai schema gộp đã chứa Google; bootstrap local dùng migration riêng khi chạy runner. Kết quả SQL16 trước đó không chứng minh migration Google đã chạy trên Supabase.

GOOGLE_CLIENT_ID là audience của GIS ID token; GOOGLE_AUTO_REGISTER mặc định true theo source, false thì email chưa đăng ký bị từ chối. Login thường/Google cùng session JSESSIONID; cấu hình CORS/cookie theo mục1. Bản ghép giữ tính năng hiện có từ main; kiểm token giả lập local không thay nghiệm thu Google thật hoặc Render. Trạng thái thực thi DB/deploy xem docs00/QA; agent không tự chạy migration cloud.

<a id="attendance-review-20261109"></a>
### Attendance cuối ngày/note — Owner09/11

BE attendance mới cần [17_attendance_review_postgres.sql](../database/migrations/17_attendance_review_postgres.sql) trước deploy. Baseline cần sessions/attendance14 và attendances.note TEXT; dependency Google13 hiện có vẫn cần kiểm riêng. SQL17 chỉ thêm marker/index, không reset/seed hoặc chạy lại16. Hai schema gộp chưa nhúng17: dùng riêng migration cho DB cũ hoặc sau dựng baseline mới. Không chạy schema dựng lại trên dữ liệu cần giữ.

Migration đặt marker mới false cho dữ liệu hiện có, không queue buổi đã finalized trước nâng cấp; replay giữ pending của finalizer mới. Khi phát hành cần thay bản BE/job cũ bằng cùng bản mới, tránh để finalizer cũ tiếp tục gửi no-show tại end. Finalizer mới vẫn hoàn tất lifecycle tại end; review đọc Absent hiện hành sau00:00 hôm sau, bỏ buổi hủy/invalidated, tắt pending cùng transaction và giữ dedupe notification. Job trễ không đổi cutoff Coach; booking-block vẫn default false.

Kiểm local không.env/cloud:

```powershell
python scripts/test-attendance-review-postgres.py --database scms_phases_attendance_migration_<ma_moi>
python scripts/test-phases-postgres.py --database scms_phases_attendance_<ma_moi> --minimal-fixture
.\mvnw.cmd '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh' '-Dtest=AttendanceCorrectionIntegrationTest' '-Dscms.attendance.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_attendance_<ma_moi>' test
```

Chạy Maven từ bản sao source hiện hành không.env; DB mới loopback55440 đúng runner. Integration test mặc định H2, opt-in PG chỉ nhận URL local đã ghim, ddl-auto=none; fixture login/auth-me thật rồi kiểm response/DB sau commit và race. Có thể xuất schema từ cùng app bằng `-Dscms.attendance.openapi=<duong_dan_raw_openapi.json>`; chỉ cập nhật snapshot sau khi kiểm đạt. Test đích rồi clean test theo mục2. Bằng chứng/SHA/count hiện hành chỉ ghi ở docs00/QA; FE/Render/CRUD nghiệm thu đợt chung, lượt này không mutation cloud.

Session1.1 content: `TrainingResultContentIntegrationTest` dùng server HTTP thật trên random port, chỉ bind127.0.0.1; mặc định H2, opt-in `-Dscms.content.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` dùng PostgreSQL riêng đã chuẩn bị bằng runner minimal ở trên. Chạy test đích `-Dtest=TrainingResultServiceTest,TrainingResultContentIntegrationTest`, rồi `clean test` tuần tự trong source copy không.env. Fixture synthetic/Clock test cố định chỉ phục vụ kiểm biên; Clock ứng dụng không đổi. Test mock job/mail, tắt AI/QueryDR/probe; không gọi provider thật. Demo Owner dùng Clock thực và fixture mới đang trong cửa sổ, hướng dẫn tại [bàn giao content11/11](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-CONTENT-2026-11-11.md); chuẩn bị Swagger/auth-me không là browser PASS. Bằng chứng/môi trường ở Active/QA.

Session1.2 scope đọc: `TrainingResultScopeIntegrationTest` cũng dùng HTTP loopback thật, committed fixture hai center, login/auth-me và snapshot DB/audit; opt-in PG riêng bằng `-Dscms.result-scope.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>`. Đích H2 gồm thêm `TrainingResultServiceTest,TrainingResultAccessIntegrationTest`; PostgreSQL chạy trên DB minimal mới riêng cho suite này. [Bàn giao scope](../SCMS-CAP-NHAT/SCMS-BAN-GIAO-FE-TRAINING-RESULT-SCOPE-2026-11-11.md) dùng DB demo local riêng và không phụ thuộc cửa giờ attendance.

Session1.3 room validation: `RoomValidationIntegrationTest` kiểm POST/PUT/GET, biên name50/location100, null/blank/raw whitespace, schema lengths và DB/audit sau commit. Opt-in `-Dscms.room-validation.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` dùng DB minimal mới riêng, ddl-auto=none; thêm `-Dscms.room-validation.openapi=<duong_dan_staging_json>` để xuất schema từ app đã kiểm, giữ server metadata trung tính như snapshot FE hiện có. Đích liên quan `PublicationResourceHttpTest` giữ room activity/role/center/lock/race. Chạy đích rồi clean test tuần tự sau từng nhóm BE; không.env hoặc provider/cloud. DTO/JPA khớp cột có sẵn nên không cần migration, không chạy schema dựng lại để thêm validation.

Session2.1/R1: `AttendanceDateFilterIntegrationTest` kiểm HTTP loopback thật với login/auth-me, fixture committed có Session Present/Absent/null và lobby legacy, biên nửa đêm, filter AND, owner/assignment/center/superseded và snapshot DB/audit không mutation. Dùng `-Dtest=AttendanceDateFilterIntegrationTest,AttendanceServiceTest` cho đích H2; opt-in PG riêng bằng `-Dscms.attendance-date.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_<fixture_moi>` trên DB minimal mới từ runner, ddl-auto=none. Thêm `-Dscms.attendance-date.openapi=<duong_dan_staging_json>` để xuất schema và đối chiếu các mô tả ngày ở cả hai alias; chỉ cập nhật mô tả param startDate/endDate, giữ shape và metadata ngoài luồng trong snapshot canonical. Fixture dùng ngày synthetic cố định để kiểm đọc, không thay bean Clock hoặc sửa dữ liệu lịch sử thực. Lobby write toggle vẫn tắt; đọc record lịch sử không bật lại luồng check-in cửa. Chạy test đích/PG rồi clean tuần tự trong source copy không.env, không provider/cloud; không cần migration hoặc dựng lại bundle demo.

<a id="demo-rebuild-20261109"></a>
### Bundle demo Normal/Refund — cấu trúc và kiểm local

File đích duy nhất [database/schema_postgres.sql](../database/schema_postgres.sql): tạo lại55bảng SCMS, xóa4backup SCMS cũ; giữ hạ tầng auth/storage/realtime/extensions và bảng ngoài danh sách. FK/view ngoài phạm vi gây RESTRICT rollback. Bundle nhúng baseline/migration17–22, chỉ trích guard runtime16; không chạy cleanup16 hoặc18–22 nối tiếp. Schema root là baseline bảo toàn cũ.

Bộ47account giữ33PDF/thêm14,9môn/20sân/21Class và lịch/quyền/thu/hoàn/attendance/incident/chat/thông báo tổng hợp.2PDF DEMO nằm BYTEA riêng tư, không cần provider/bucket công khai. Seed chỉ tắt hai trigger thời gian classes/sessions để nạp lịch sử tổng hợp rồi bật lại cùng transaction; FK/CHECK/unique vẫn chạy, postflight kiểm dữ liệu. Không coi seed history là nghiệp vụ thật đã quan sát.

Anchor là ngày cài bundle theo VN; compiler bắt buộc nhận `--anchor`. Preflight dùng ngày thực từ clock_timestamp/Asia/Ho_Chi_Minh, chặn khác anchor trước phần tạo/xóa; không đổi Clock Render/TTL để giữ demo. [Rule và công cụ chuẩn bị](#demo-anchor-rules) áp dụng khi đổi ngày. Chuẩn bị/kiểm local không cấp quyền chạy Supabase. Sau Owner duyệt thực thi đúng bundle: ngừng BE/job ghi, kiểm đúng DB/recovery riêng tư rồi chạy nguyên một file; restart BE để hủy phiên/ID cũ, đăng nhập mới/auth-me trước smoke. Không tái dùng pay URL trước reset.

**Kết quả mong đợi sau thực thi được duyệt:** result=PASS, migration_key=20261110_demo_rebuild_v3; summary mode=approved-synthetic-demo/accounts47/subjects9/rooms20/advanced_templates9/normal_classes12/synthetic_fixture:true. Bản generator có rule anchor bổ sung anchor_date/business_timezone/fixture_version; migration_key là phiên bản mô hình, không phải ngày cài. Bằng chứng Supabase trước đó không chứng nhận bundle mới đã chạy; xem Active/QA. Invoice sequence giữ floor cũ và các số fixture vừa dùng, không giảm/tái dùng; rollback có thể có khoảng trống. Thu hồi quyền PUBLIC/anon/authenticated của bảng/sequence SCMS, giữ bảng/grant ngoài phạm vi; role datasource riêng cần đúng quyền sau tạo lại.

Kiểm trong source copy không.env, DB mới riêng do runner tạo; không reset DB tùy ý:

```powershell
python scripts/build-demo-schema.py --anchor 2026-11-10
python scripts/test-demo-schema-postgres.py --database scms_schema_demo_phase1_<ma_moi> --output artifacts/runs/normal-demo/sql-proof.json
python scripts/test-phases-postgres.py --database scms_phases_normal_<ma_moi> --minimal-fixture
.\mvnw.cmd "-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh" "-Dtest=NormalLifecycleHttpTest,DemoRebuildPostgresTest" "-Dscms.normal.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_normal_<ma_moi>" "-Dscms.demo.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_schema_demo_phase1_<ma_moi>" test
.\mvnw.cmd clean test
# Export từ DB fixture nguyên trạng riêng trước test HTTP làm thay đổi kịch bản:
python scripts/build-system-data.py --anchor 2026-11-10 --from-local-pg scms_schema_demo_phase1_<db_nguyen_trang>
```

Ví dụ dùng ngày10/11; thay bằng ngày VN dự kiến và kiểm lại. `--check` cùng `--from-local-pg` kiểm SYSTEM-DATA tái sinh. Compiler dùng reportlab tạo2PDF invariant; không là dependency BE. QR/OTP/Pending/waitlist/provider attempt tạo lúc demo bằng HTTP; không seed secret/hold hết hạn. JVM timezone canonical cần argLine như ví dụ; không load.env hoặc SMTP/gateway thật.

Nhánh giữ dữ liệu chọn baseline theo README,21 rồi22 additive riêng; không reset để vá.22 thêm proposal/response/transfer, entitlement_class_id/change_pending, Refund receiver/attempt/document/verification; guard kiểu/nullability/FK/unique/replay, không backfill consent/ngân hàng hay sửa receipt. Upload HTTP giữ multipart mặc định1MiB; service/DB có trần bảo vệ5MiB, không coi trần lưu trữ là cấu hình nhận HTTP. FE gửi file≤1MiB, tăng trần HTTP cần cấu hình/test riêng; download có phiên/role/center; không dùng MediaService/public URL. PostgreSQL đã kiểm, SQLServer chưa suy tương đương.

Trạng thái chuẩn/điều phối ở Active/QA. audit-supabase-demo.sql vẫn chỉ đọc; không gọi cloud audit/mutation mới chỉ vì bundle được cập nhật.

<a id="demo-anchor-rules"></a>
### Rule đổi ngày và chuẩn bị kịch bản demo

[SCMS-SCRIPT-DEMO-BE](../SCMS-SCRIPT-DEMO-BE.md) là kịch bản Normal/Refund theo actor và fixture. Các đề xuất màn hình/nút/bố cục/cách chuẩn bị FE trong tài liệu đó để FE duyệt; quyết định cuối thuộc FE. Contract và kiểm quyền/state tại BE vẫn áp dụng độc lập giao diện.

1. **Ngày cài và giờ demo riêng nhau.** `--anchor YYYY-MM-DD` là ngày dự kiến chạy SQL theo Asia/Ho_Chi_Minh; bắt buộc, không còn default10/11. Đây chính là anchor, không có field ngày gốc thứ ba. `--demo-at` tùy chọn: ISO timestamp có offset, không trước ngày cài; có thể ở ngày sau. Nó chỉ đánh giá cửa case, không thay captured_at/thời điểm nghiệp vụ thực hoặc đặt Clock. DB đang chạy không tự lỗi stale chỉ vì sang ngày khác; từng case phải recheck state và giờ thực.
2. **Một nguồn anchor xuyên suốt.** Sinh lại design→fixture→schema và plan bằng cùng anchor. Không sửa riêng literal ngày/guard hoặc dịch mọi ngày bằng số ngày chênh lệch: compiler chọn lại các thứ trong tuần, nên giờ đầu/cutoff thay đổi theo từng lớp. Không đổi giá/minimum/quyền/Clock/TTL để tạo kết quả đẹp.
3. **Từ chối sớm.** Bundle chặn sai ngày VN trước CREATE/DROP; kiểm lại trong fixture và rollback cùng transaction. Không thêm cờ bỏ qua guard. Metadata lưu anchor_date, business_timezone, fixture_version; giữ migration_key phiên bản. Không dùng phiên bản có ngày trong tên làm bằng chứng ngày cài.
4. **Plan không phải nghiệm thu.** `demo-plan.json` dùng mã case DEMO-01–DEMO-10, tách khỏi quyết định D01–D19, ghi firstSession/cutoff48h/cửa reason24h. Không nhập giờ demo: plannedDemoAt=null, availability=NOT_EVALUATED; vẫn có đủ mốc lịch. Có giờ thì AVAILABLE chỉ đúng cửa thời gian, mọi executionStatus vẫn NOT_RUN. Ngoài cửa ghi NOT_RUN; thiếu phiên/state/chỗ ghi BLOCKED. Ví dụ anchor11/11 làm N02 khai giảng13/11 06:00, case xác nhận trễ chỉ mở từ12/11 06:00.
5. **Kiểm lại cả dữ liệu.** Các ngày trong tuần, biên tháng/năm/ngày nhuận, trước/đúng cutoff, timezone qua nửa đêm; 12buổi hợp lệ, Coach/phòng/Member không trùng, holiday, min/sĩ số, quyền/credit/hạn, proposal snapshot, thứ tự tài chính và SLA hết ngày TT mở thứ7. FK/CHECK/unique và counter hóa đơn giữ nguyên. OTP/QR/Pending/provider attempt tạo lúc chạy, không seed hoặc kéo hạn.
6. **Bằng chứng tách lớp.** Compile/reproducibility, SQL fresh/replay/rollback/guard và HTTP response+commit là các kết quả riêng. SQL local PASS không là HTTP/Render/browser PASS. Giữ fingerprint nguồn thực kiểm trong QA; không dùng checkout cũ để chứng minh working tree mới.
7. **Xuất từ bộ nguyên trạng.** Export/check SYSTEM-DATA trước HTTP mutation, cùng anchor/version và DB local được giới hạn. Exporter chặn sai anchor hoặc fixture đã thêm Invoice/ghi danh/chuyển quyền/Refund/xác nhận. Bảng tra cứu N01–N12/tài khoản/proposal/Refund được sinh từ cùng các dòng đã xuất; gắn alias bằng tên/email và quan hệ FK, không suy ID bằng số thứ tự. File vẫn DATA DESIGN và không là dump Supabase. DB để HTTP riêng hoặc chỉ dùng sau export.
8. **Phạm vi thực thi riêng.** Công cụ chuẩn bị không đọc.env, không chạy cloud/deploy/provider. Chỉ Owner cho phép thực thi đúng bundle mới mới chạy Supabase trong phạm vi SCMS đã nêu. Sau reset lấy ID/cookie/phiên mới; không tái dùng nhóm smoke đã tiêu thụ. Hạ tầng ngoài SCMS giữ nguyên.

Ví dụ PowerShell dưới đây dùng ngày10/11; nhập đúng ngày dự kiến cài, giờ dự kiến demo và DB mới. Cần Python/reportlab và timezone IANA Asia/Ho_Chi_Minh khả dụng; phần SQL cần Docker/PostgreSQL loopback đã cấu hình. `artifacts/runs/` là staging local được Git bỏ qua, không phải tài liệu bàn giao thứ hai:

```powershell
$demoAnchor = '2026-11-10'
$demoRunId = [guid]::NewGuid().ToString('N').Substring(0,8)
$demoStage = "artifacts/runs/demo-$demoAnchor-$demoRunId"
$demoDb = "scms_schema_demo_phase1_$($demoAnchor.Replace('-',''))_$demoRunId"

python scripts/test-demo-anchor.py
python scripts/prepare-demo-bundle.py --anchor $demoAnchor --output-dir $demoStage --verify-local --database $demoDb
```

Đọc `verification.json` và `demo-plan.json` trong staging. Khi đã biết giờ, có thể thêm `--demo-at '2026-11-10T12:00:00+07:00'` phù hợp ngày cài; bỏ tham số vẫn sinh bundle/lịch, không đoán giờ demo. Lệnh tạo schema/fixture mới, không ghi đè database/schema_postgres.sql hoặc bootstrap canonical. Thư mục đã có kết quả không được ghi đè. SQL verification chỉ chạy Docker loopback127.0.0.1:55440/container đã cấu hình và DB mới `scms_schema_demo_phase1_*`; lỗi DB trùng tên dừng, không tự drop. Nếu anchor chưa phải hôm nay VN, phần SQL là NOT_RUN và exit2, không tạo DB; bundle tương lai vẫn được sinh để review. Không coi exit2 là PASS.

Khi SQL đã PASS, xuất từ fixture nguyên trạng trước test HTTP:

```powershell
python scripts/build-system-data.py --anchor $demoAnchor --from-local-pg $demoDb --output "$demoStage/SYSTEM-DATA.md"
python scripts/build-system-data.py --anchor $demoAnchor --from-local-pg $demoDb --output "$demoStage/SYSTEM-DATA.md" --check
python scripts/test-demo-handoff.py --anchor $demoAnchor --database $demoDb
```

Trong bản sao source hiện hành **không.env**, dùng local HTTP tests ở mục bundle trên: DemoRebuildPostgresTest trỏ `$demoDb`, NormalLifecycleHttpTest trỏ DB minimal riêng theo guard của runner. Lưu response/state sau commit và XML; `verification.json` của công cụ chuẩn bị vẫn ghi HTTP NOT_RUN vì công cụ không chạy HTTP test thay Bạn. Kết quả HTTP thực tế ghi canonical QA. Nếu có thay đổi BE thì chạy clean test theo AGENTS; chỉ sửa docs/script không tự suy nghiệm thu BE từ test cũ.

Sau review/check, BE mới đưa schema/fixture và bản DATA DESIGN đã kiểm vào các file canonical tương ứng, cập nhật Active/QA; không giữ nhiều bản before/after trong gói FE. Không chạy thêm SQL16/migration nối tiếp sau bundle. Việc thực thi cloud và browser FE vẫn là các bước riêng.

<a id="normal-release-smoke-20261110"></a>
### Smoke sau phát hành Normal/Refund — quy trình10/11

<a id="session123-release-smoke-20261113"></a>
**Luồng phát hành Session1–3:** Cloud chỉ login/logout, đọc và negative đã chứng minh không commit trên PG local; Member PUT RBAC403, name51/400 và duplicate room409 phải GET readback quyền/phòng không đổi. Positive mutation, race và đổi thật quyền/status account thực hiện local. Bảng Normal10/11 bên dưới giữ quy trình của phạm vi đã duyệt khi đó; quyền của lượt hiện hành theo Active/QA. Không cloud SQL/SQL16/provider/ZIP hoặc lưu credential/cookie trong report. CORS HTTP chưa là browser PASS.

<a id="session123-release-smoke"></a>
Smoke phát hành: chạy `python scripts/test-release-smoke.py` offline trước; rồi `python scripts/release-smoke.py --expected-commit <runtime_sha> --expected-db-fingerprint <target_hash> --actors-file <file_private_da_duyet> --output <run_json>`. Baseline dùng `--phase baseline`; bản mới dùng `--phase post-deploy --negative-mutations` sau khi các negative request đã chứng minh rollback trên PG local. File actor/cookie không đưa vào Git hoặc log. Kiểm auth-me đúng role/isFirstLogin:false; Active được bảo vệ bởi filter hiện hành, response auth-me không có field status.

Chỉ GET `/api/health`, `/actuator/health`, `/v3/api-docs` retry transport hoặc502/503/504: tối đa3attempt,35giây/attempt, deadline110giây/read, delay2giây. Retry bao gồm đọc body; từng attempt và lỗi đầu được giữ riêng. HTTP/business khác, login/logout và mọi mutation chỉ gửi một lần. Phân biệt `STARTUP_TRANSPORT_EXHAUSTED`, `TRANSPORT_FAILURE`, `API_ASSERTION`, `SETUP_OR_PAYLOAD`; deadline không nâng thành PASS. Smoke cố logout các phiên đã tạo kể cả sau failure. Kiểm SHA và DB fingerprint lại cuối lượt; chỉ là bounded API smoke, không full role/business/JSON Schema/browser/provider acceptance.

Trước merge lưu main/runtime/DB baseline và cây baseline. Với bản phát hành bị revert, chuẩn bị nhánh mới từ main hiện tại rồi áp lại đúng diff, kiểm cây source cuối với candidate đã giữ; merge lại nhánh cũ không chứng minh feature đã trở lại. Sau merge chờ đúng SHA/DB trong deadline triển khai đã ghi của lượt đó, rồi mới smoke. Gate bắt buộc fail hoặc hết deadline: push forward revert có cây đúng baseline của lượt này, kiểm CI/runtime/baseline smoke, giữ bằng chứng lỗi đầu; không force-push/reset DB hoặc tự thử phát hành thêm vòng.

Kết quả thực thi hiện hành, phạm vi PASS/BLOCKED/NOT_RUN và fixture đã tiêu thụ xem [Active/QA](00-MASTER-CHECKLIST.md#active). Quy trình dưới đây không thay bằng chứng hoặc cho phép chạy lại fixture đã dùng như dữ liệu nguyên trạng; mỗi lượt vẫn xác định actor/ID/state/chỗ và quyền Owner trước nhóm ghi.

Owner đã cung cấp PASS cho bundle demo và yêu cầu merge tai→main; bằng chứng/trạng thái Git/CI chuẩn ở Active/QA. Không chạy lại bundle, SQL16, seed hay migration nối tiếp để làm smoke. Không tự gọi VNPay/QueryDR/mail/AI/Google/storage provider hoặc chuyển khoản ngân hàng thật. Mỗi nhóm ghi rõ build/actor/fixture/response và trạng thái sau commit, PASS/FAIL/BLOCKED/NOT_RUN; anonymous401 và health200 chưa là nghiệm thu nghiệp vụ.

| Thứ tự / lớp kiểm | Cách kiểm và kết quả cần có |
|---|---|
| 1. Đúng build và DB — chỉ đọc | Lấy SHA phát hành thực tế từ Git main/CI rồi GET /api/health trên BE FE thật sự gọi: buildCommit phải khớp, status UP; đối chiếu databaseTargetFingerprint với target đã xác nhận. GET /actuator/health phải UP cho DB. Nếu build cũ/DB chưa xác định thì dừng nhóm nghiệp vụ, không đổi credential/env để ép pass |
| 2. Phiên mới và dữ liệu — chỉ đọc | Login lại năm role bằng account demo mới, xác minh GET /api/auth/me cùng cookie đúng user/role/authorities; không dùng session/ID trước reset. Admin/Manager đọc danh mục phù hợp:47account/21Class gồm9template Advanced và12Normal,9môn/20room nếu state chưa bị thao tác. Không coi account Inactive/Locked/Pending đăng nhập thất bại là lỗi role hoặc sửa status để thử |
| 3. Scope và DTO — chỉ đọc | Member chỉ thấy enrollment/lịch/plan/attendance/result và Refund của mình; thử ID Member khác phải bị chặn. Coach đọc roster lớp được giao, lớp không được giao bị chặn. GET roles/permissions chỉ Admin; đọc Refund/proof/history theo role tài chính, Member/Coach không được tải proof. Manager kiểm các state Refund hiện có, receiver che đúng phạm vi/contact không lộ ra thông báo; response giữ field/shape của contract |
| 4. Cash/admission/lifecycle — nhóm ghi demo được xác định | Chọn rõ fixture SMOKE trước khi chạy; thu Cash qua Receptionist có đủ quyền phải Paid+Registered cùng transaction, Invoice.amount theo BE; Member Cash online bị chặn và retry không nhân đơn/suất. Kiểm cutoff/minimum/confirm và chặn hủy Ongoing bằng response+readback. Không đổi Clock, ngày cũ hoặc state DB để vượt cửa thời gian; phần không đúng thời điểm ghi NOT_RUN |
| 5. Chuyển lớp/Refund — nhóm ghi demo được xác định | Nếu dùng N05→N11 phải ghi rõ Member/chỗ sẽ tiêu thụ; preview không giữ chỗ, Accept phải chuyển quyền giữ Invoice.classId/amount, retry không nhân liên kết. Refund dùng workflow Member receiver→Manager verify/approve→Receptionist claim/proof→Manager bank verification; upload chưa Completed. Có thể dừng ở đọc/claim/proof thay vì tạo xác nhận ngân hàng giả trên hồ sơ chưa thống nhất. Retry cùng UUID không tạo attempt khác; mọi evidence mô phỏng phải ghi DEMO; không chuyển tiền thật |
| 6. Kiểm sau ghi và FE browser | Đọc lại Invoice/enrollment/quyền/lịch/Refund, report tiền thu–hoàn–ròng và audit/thông báo đúng scope; đối chiếu DB chỉ trong quyền đọc đã có. Không xóa/sửa lịch sử để làm đẹp kết quả. Trên đúng repo/build FE, kiểm cookie/CORS, chọn lớp/buổi, lưu rồi tải lại; UI mới chưa tích hợp thì ghi NOT_RUN và dùng API có phiên để kiểm BE, không gọi đó là browser PASS |

Smoke ban đầu ưu tiên1–3. Nhóm4–5 cần fixture/thao tác ghi cụ thể được xác định trước; kế hoạch này chưa tự mở quyền sửa dữ liệu cloud. Bộ demo đã seed còn có thể được FE dùng trình diễn, nên không tự tiêu thụ N05→N11/hoàn tất Refund hoặc đổi role/password hàng loạt. Không đếm request làm quota; full race/matrix/CRUD/FE/live thuộc đợt riêng. Sau smoke mới trình R1/R2, validation sân và policy còn mở để Owner duyệt.

<a id="phase1-migrations-20261109"></a>
### Nhánh nâng cấp giữ dữ liệu — migration18–20

### Phase1 09/11 — audit, loại sân và giá công bố

Bản BE Phase1 local cần baseline hiện hành, Google13/attendance14/17 như trước và ba migration riêng theo thứ tự: [18_audit_scope_postgres.sql](../database/migrations/18_audit_scope_postgres.sql), [19_room_activity_postgres.sql](../database/migrations/19_room_activity_postgres.sql), [20_class_publication_price_postgres.sql](../database/migrations/20_class_publication_price_postgres.sql). Không chạy schema dựng lại hoặc SQL16 để thêm cột. Hai schema gộp chưa nhúng18–20; dựng mới xong baseline cũng phải áp migration riêng. **Chưa có quyền thực thi cloud trong lượt này; chưa deploy BE Phase1 lên Render.** Không phát hành BE mới khi schema còn thiếu cột.

18 thêm category/center/provenance nullable vào system_logs; không backfill hoặc xóa lịch sử. 19 thêm rooms.activity_type và map20room bằng **ID trong snapshot bất biến** `20261102_room_catalog_v1`, không suy từ mã/tên hiện tại. Nếu thiếu snapshot/ID không còn đúng center1/có loại mâu thuẫn hoặc lịch Scheduled tương lai dùng sân không rõ/sai môn, script rollback và báo lỗi/ID buổi cần đối chiếu; không tự dời lịch, đổi môn, đóng sân hoặc sửa dữ liệu để vượt guard. Dữ liệu phòng lịch sử ngoài20room giữ nguyên; chưa cấu hình không được xếp lịch mới. 20 thêm pricing_draft mặc định false cho dữ liệu cũ, không đoán Closed là chưa công bố. BE mới tự quản lý marker draft mới; replay giữ marker/loại sân đã được sửa hợp lệ về sau, giữ nguyên giá/chứng từ.

Kiểm tại bản sao source không.env, PostgreSQL loopback55440 đã ghim trong runner; mỗi tên DB phải mới, không reset DB có sẵn:

Khi19 báo `ROOM_ACTIVITY_SCHEDULE_CONFLICT` kèm ID buổi, chạy riêng SELECT trong [check-room-activity-postgres.sql](../scripts/check-room-activity-postgres.sql) trên đúng DB đó. Script mặc định buổi164; chỉ đổi target_session_id khi cần. Query không mutation và dùng được khi activity_type chưa tồn tại sau rollback: nó tính mapping dự kiến từ snapshot bất biến, trả trạng thái cột/marker18–20, môn/room/center của buổi và các room cùng môn kèm sức chứa/trùng giờ để đối chiếu. Kết quả room chỉ là dữ liệu xem xét, chưa xác nhận toàn bộ điều kiện đổi lịch/consent/quyền học. `guard_would_block:false` có thể do trạng thái/thời gian đã đổi; không tự suy lỗi trước đó đã được sửa. Giữ guard và dữ liệu, xác định nguyên nhân trước khi đề xuất xử lý cụ thể; không đổi môn/loại sân, hủy/hoàn tất buổi hoặc bỏ guard để ép migration qua.

```powershell
python scripts/test-phase1-migrations-postgres.py --database scms_phases_p1_migrations_<ma_moi>
python scripts/test-phases-postgres.py --database scms_phases_p1_publication_<ma_moi> --minimal-fixture
.\mvnw.cmd '-Duser.timezone=Asia/Ho_Chi_Minh' '-Dtest=PublicationResourceHttpTest' '-Dscms.publication.pg-url=jdbc:postgresql://127.0.0.1:55440/scms_phases_p1_publication_<ma_moi>' test
```

`RbacGuardIntegrationTest` dùng property riêng `scms.rbac.pg-url`, `AttendanceCorrectionIntegrationTest` dùng `scms.attendance.pg-url`; chuẩn bị **DB mới riêng cho mỗi fixture suite** bằng runner baseline như trên. Không chạy hai Maven clean cùng target. Test đích rồi clean test theo AGENTS; schema xuất từ app đã kiểm, QA/hash/count tại docs00/QA. Các test không nạp.env, gọi provider/gateway hoặc nghiệm thu browser/Render. Bước phát hành/SQL cloud chỉ làm khi được duyệt riêng với source đã kiểm và migration tương ứng; không tự bật QueryDR hoặc chế tài booking.

<a id="retire-legacy-memberships"></a>
### Bỏ dữ liệu gói hội viên chung — migration16

Owner đã duyệt B→dọn mô hình gói chung→A; **dừng chờ Owner chạy Supabase thành công trước khi làm A**. File duy nhất: [16_retire_legacy_memberships_postgres.sql](../database/migrations/16_retire_legacy_memberships_postgres.sql). Cần DB SCMS hiện hành đã có schema Advanced/QueryDR15; không chạy lại schema dựng lại hoặc seed. BE local mới đóng API gói chung và tuyến enroll Advanced cũ; trạng thái publish/runtime xem docs00, SQL không tự deploy BE.

File tự làm preflight→khóa/phân loại→xóa subgraph gói chung cũ chắc chắn→postflight→commit. Nó có thể xóa hóa đơn cũ và payment attempt/resolution trực tiếp thuộc đúng subgraph đã bỏ, **không chỉ xóa danh mục**. Quyền/chứng từ hiện hành được giữ, chỉ tháo FK catalog cũ khi có đủ môn/Coach/owner; current amount/purchaseAmount/credit/state và Normal/session/attendance không được thay. Gặp link chưa rõ/ngoài phạm vi hoặc dữ liệu được bảo vệ đổi ngoài dự kiến thì toàn bộ transaction rollback. Không DROP TABLE/TRUNCATE/CASCADE; không lưu archive dòng nghiệp vụ cũ; marker chỉ có số lượng. Trigger sau migration chặn ghi trở lại mô hình cũ.

1. Mở đúng project Supabase đang chạy SCMS, vào **SQL Editor → New Query**; dán toàn bộ file, không chọn chạy riêng từng đoạn. Bấm **Run** theo [hướng dẫn Supabase](https://supabase.com/docs/guides/database/functions).
2. **Kết quả trả về một dòng `result=PASS`, `migration_key=20261106_retire_legacy_memberships_v1`, `summary.model_retired=true` là đúng**: preflight/postflight đã qua và transaction đã commit. Số lượng xóa có thể0 nếu DB không còn dữ liệu cũ; không so với số fixture local. Replay không xóa thêm; summary vẫn là số lượng của lần áp dụng đầu.
3. Gửi dòng cuối hoặc nội dung lỗi để kiểm tiếp. Nếu có `SCMS_LEGACY_RETIRE_PREFLIGHT/BLOCKED/POSTFLIGHT/REPLAY_CONFLICT`, không bỏ guard hoặc sửa dữ liệu cho qua; gửi lỗi để xác định đúng phần còn vướng. Chỉ tiếp tục A sau kết quả migration thành công; SQL pass không thay FE/Render acceptance. Trạng thái lần chạy Owner đã cung cấp xem docs00/QA.

Kiểm local: `python scripts/test-retire-legacy-memberships.py` chỉ dùng Docker PostgreSQL loopback55440/DB mới, không .env/cloud; có kiểm empty/old+modern/replay/retirement guards/unknown FK/partial Advanced/rollback sau mutation. BE có test đích và clean; workflow có phiên cũng kiểm trên PG sau migration. Khi chạy Maven PG dùng JVM `-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh`, không dùng alias Asia/Saigon cho JDBC của container này. Kết quả/hash/XML tại docs00/QA, không suy local thành Supabase đã chạy.

Chọn script DB theo [database/README](../database/README.md#2-chọn-đúng-script): bản root là baseline bảo toàn cũ, `database/schema_postgres.sql` hiện là bản dựng lại sạch09/11 gồm cấu trúc mới. Khi cần giữ dữ liệu, dùng đúng migration thiếu thay vì reset; không chạy nối tiếp hai schema hoặc toàn thư mục migration. Các bundle02/11 dưới đây là lịch sử nâng cấp, không áp tiếp sau reset mới.

**Bundle room/báo vắng02/11:** baseline29/10 dùng `13_room_catalog_postgres.sql`→`14_absence_attendance_postgres.sql` trước BE mới. Dừng ghi lịch trong cửa sổ migration, backup/restore proof và kiểm DB đích; không dùng bản dựng lại. Preflight đọc rooms, classes.max_capacity, số ghi danh, sessions Scheduled từ hôm nay và môn liên quan. Đối soát lớp vượt sức chứa mới theo danh mục README, ca đang diễn ra, phòng tùy chỉnh, code/tên trùng và sân01 trùng lịch trước khi chạy. Guard lỗi thì transaction rollback; không sửa lớp ngoài7giới hạn được duyệt hoặc tắt trigger để vượt. Bản trước/sau chuyển room và marker lưu trong scms_data_migration_audit; lịch đã diễn ra/chứng từ/attendance không chuyển/xóa. Postflight:20phòng mới Available,8phòng chuẩn cũ Closed nếu nâng cấp từ fixture8phòng; lịch tương lai dùng room Available/đủ sức chứa/không trùng; kiểm marker và unique current attendance. Replay giữ metadata phòng đã chỉnh về sau. BE mới cần đủ hai migration; code cũ chưa hiểu attendance superseded nên không rollback riêng BE sau khi đã phát sinh luồng mới.

**Hai file tạm ở root theo yêu cầu Owner:** chạy toàn bộ `0211-1.sql` (preflight+7giới hạn đã duyệt+migration room13) trong một lần SQL Editor; chỉ khi thành công mới chạy toàn bộ `0212-2.sql` (migration14+postflight) lần thứ hai. File2 kiểm marker của file1 và dừng nếu sai thứ tự. Không cần chạy riêng truy vấn preflight hoặc chạy thêm hai migration nguồn khi dùng cặp này. Nếu guard lỗi, dừng để đối soát; không tự giảm sức chứa/sĩ số hoặc bỏ guard. Sau hai lần thành công và kiểm kết quả, có thể xóa hai bản tạm; migration13/14 trong database/migrations vẫn là nguồn bảo trì. Không dùng cặp file này để dựng lại DB hoặc thay schema gộp ở môi trường chưa có baseline29/10.

Owner đã duyệt điều chỉnh7lớp bên dưới: bản0211-1 hiện gộp `13_owner_class_capacity_20261102_postgres.sql` **trước**13_room_catalog, cùng BEGIN/COMMIT. Bước này chỉ cập nhật max_capacity của7ID đúng môn/center1/Normal, chấp nhận giá trị cũ đã duyệt hoặc đã bằng mức mới; trạng thái khác làm dừng. Registered+Pending được đếm lại dưới lock, không được vượt mức mới. Audit `20261102_owner_normal_class_capacity_v1` giữ đầy đủ hàng lớp và held_seats trước/sau; lỗi chuyển phòng phía sau rollback cả cập nhật lớp/audit. Replay bỏ qua bước đã có marker để không reset thay đổi về sau. File2 mới kiểm cả marker giới hạn lớp và marker phòng. Không nhúng bước theo ID riêng này vào schema gộp/empty release cho mọi DB.

**Sửa lỗi CUSTOM_ROOM02/11:** inventory Owner gửi có9phòng, thêm room9 Sân Tennis02 so với fixture8phòng ban đầu. Bản sửa nhận cả tên trong20phòng đã duyệt và alias cũ; vẫn chặn tên lạ, trả DETAIL room_id/tên/trạng thái. Lịch Tennis02 chuyển đúng C01-TEN-02 để không bị dồn sang Tennis01. Chạy lại toàn bộ bản mới `0211-1.sql`; không chạy file2 nếu file1 lỗi. Với bộ9phòng này, kết quả lần nâng cấp đầu là20phòng mới và9phòng legacy Closed, không buộc tổng bảng chỉ20dòng. ROOM_CATALOG_SCHEDULE_REVIEW vẫn có thể dừng hợp lệ nếu lớp/lịch vượt sức chứa mới; module room không tự giảm max_capacity; bước Owner-specific chỉ đổi đúng7giới hạn đã duyệt và không xóa ghi danh. Lỗi ở trong transaction của cả file nên lần bị chặn không commit cập nhật phòng.

**Chẩn đoán SCHEDULE_REVIEW:** DETAIL của0211-1 mới liệt kê tối đa20buổi bị chặn với session_id/class_id, center_id, target_code, session_start, max_capacity, target_capacity, held_seats (Registered+Pending) và reasons. `CLASS_CAPACITY_EXCEEDS_ROOM` là cấu hình số chỗ tối đa vượt sân, kể cả số người thực tế còn ít; `ENROLLMENTS_EXCEED_ROOM` là số chỗ đã giữ vượt sân; ngoài ra có STARTED_OR_PAST_SCHEDULED, UNKNOWN_SUBJECT_OR_TARGET, WRONG_CENTER và MISSING_CLASS_CAPACITY. Guard không đổi điều kiện hoặc tự xử lý dữ liệu. Đề xuất để Owner chốt sau khi xem DETAIL: nếu chỉ max_capacity vượt nhưng held_seats còn vừa sân, điều chỉnh max_capacity của đúng lớp có audit; nếu số đăng ký vượt sân thì cần phương án lớp/phòng/lịch phù hợp, không tự xóa/hủy ghi danh. Ca đã bắt đầu/đã qua còn Scheduled và liên kết môn/center thiếu/sai phải đối soát theo lịch sử thực tế, không đổi giờ/trạng thái để né guard. Chỉ chạy file2 sau khi file1 thành công.

**Giới hạn7lớp Owner đã duyệt02/11:** các dòng DETAIL được gửi đều CLASS_CAPACITY_EXCEEDS_ROOM, mỗi lớp giữ1chỗ. Đồng bộ max_capacity theo bảng dưới trong transaction trước khi chuyển phòng, có kiểm/audit như trên. Không tăng sức chứa phòng đã chốt; không đổi người học, giá, chứng từ hoặc attendance. Giờ buổi học giữ nguyên; phòng của lịch tương lai chuyển theo migration13. Đây là đúng7lớp được duyệt, không cho phép tự thu nhỏ lớp khác; guard vẫn kiểm mọi lịch trước commit. Agent chỉ chuẩn bị/kiểm local, chưa chạy Supabase.

| class_id | Lớp Normal K01 | max_capacity cũ → đã duyệt |
|---:|---|---:|
|11|Bơi lội|15 → 10|
|12|Pickleball|12 → 4|
|13|Bắn cung|10 → 5|
|14|Bóng rổ|15 → 8|
|16|Bóng chuyền|15 → 8|
|17|Cầu lông|15 → 4|
|18|Bóng bàn|15 → 2|

Kiểm đích bundle có phê duyệt: `python scripts/test-owner-class-capacity-postgres.py --prefix scms_phases_owner_caps_<mã_mới> --output artifacts/runs/<run>/owner-capacity.json`. Runner chỉ dùng PostgreSQL local được ghim, kiểm sai danh tính/giới hạn/held seats, rollback qua lỗi chuyển phòng, đúng7giới hạn, giữ chứng từ/attendance/lịch đã diễn ra, audit và replay. Kết quả chuẩn ở docs00/QA; không tự gán nghiệm thu cloud.

Kiểm local riêng: `python scripts/test-room-absence-postgres.py --prefix scms_phases_room_<mã_mới> --output artifacts/runs/<run>/migration.json`; runner từ chối DB ngoài container/port local được ghim, tạo DB mới và kiểm fresh/rebuild/upgrade/replay/guard/history. Fixture nâng cấp lấy schema trước thay đổi tại commit baseline đã ghim trong runner, không đọc .env. Sau đó kiểm HTTP PostgreSQL bằng PhaseWorkflowHttpTest với fixture mới và SHA256 test như hướng dẫn ở mục2. Kết quả chuẩn nằm ở docs00/QA, không suy từ kiểm H2.

Truy vấn preflight chỉ đọc dưới đây liệt kê lớp/phòng/sĩ số cần so với [sức chứa mới](../database/README.md#room-catalog); kết quả không tự cho phép sửa lớp hoặc chuyển lịch:

```sql
SELECT c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name,
       count(*) AS scheduled_sessions,min(s.session_date+s.start_time) AS next_start,
       (SELECT count(*) FROM class_enrollments e WHERE e.class_id=c.class_id
        AND e.status IN ('Registered','Pending')) AS held_seats
FROM sessions s JOIN classes c USING(class_id) JOIN subjects su USING(subject_id)
JOIN rooms r USING(room_id)
WHERE r.center_id=1 AND s.status='Scheduled' AND NOT s.center_invalidated
  AND s.session_date>=(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
GROUP BY c.class_id,c.class_name,su.slug,c.max_capacity,r.room_id,r.room_name
ORDER BY next_start,c.class_id;
```

Ghi bằng chứng mới bằng `scripts/record-room-absence-review.py --run artifacts/runs/<run> --checkout <bản_source_đã_kiểm>` sau khi lưu clean XML vào `clean-xml`, PG XML vào `postgres-workflow.xml`, HTTP/SQL vào `postgres-http`, migration vào `migration-final.json`. Collector kiểm fingerprint source/schema/fixture và hash test đã chạy trước khi cập nhật QA duy nhất/OpenAPI; không nâng trạng thái cloud hay ghi đè bằng chứng lịch sử.

Kiểm source/SHA/CI/schema tương thích và quyền migration trước push nhánh Render theo dõi. Owner đã xác nhận main; kiểm lại cấu hình khi bắt đầu vận hành mới. Khi cần schema: backup/restore proof →duyệt bundle cụ thể →atomic migration/guard →postflight →deploy code tương thích. Không reset/restore đè dữ liệu để chữa code lỗi.

Theo dõi Live identity, không coi Git merge/dashboard Live là nghiệm thu nghiệp vụ. Nếu yêu cầu kiểm sau5phút, runner từ chối chạy sớm:

```powershell
python scripts/check-render-production.py --output artifacts/runs/render/check.json --source-commit <LIVE_SHA> --require-runtime-identity --expected-db-fingerprint <TARGET_HASH> --pushed-at-utc <ISO_UTC>
```

`/api/health` chỉ liveness/identity, không mở DB connection; `/actuator/health` kiểm db/mail/... và giữ show-details=never. Ghi nhận27/10: Owner dùng `/api/health` trong dashboard, render.yaml khai `/actuator/health`; không giả hai cấu hình giống nhau hoặc đổi path để che dependency hỏng.

CORS/cookie cần browser FE hiện hành kiểm preflight/Set-Cookie/cookie gửi lại. Gateway live cần Merchant/callback/fixture được phép; FE không gọi IPN. Commit chỉ docs/evidence có thể dùng `[skip render]` tránh deploy không cần thiết, theo [Render deploy docs](https://render.com/docs/deploys#skipping-an-auto-deploy).

<a id="vnpay-diagnostics"></a>
### Chẩn đoán VNPay báo sai chữ ký — D10

Không đổi thuật toán ký chỉ dựa vào ảnh lỗi. Đối chiếu source đúng `buildCommit` từ `/api/health`, cấu hình hiệu lực và URL thực tế trước. Không nhập HashSecret vào website kiểm hash, tham số dòng lệnh, chat hoặc báo cáo.

**Nếu không có bộ thông tin VNPay cấp:** ảnh Environment chỉ chứng minh đã nhập giá trị, không chứng minh merchant/key thuộc cùng bộ hoặc còn hiệu lực. Tìm email của người đã đăng ký merchant trong nhóm với từ khóa `VNPAY`, `TmnCode`, `HashSecret`, `thông tin kết nối`, cả Spam. Nếu chưa từng đăng ký hoặc không khôi phục được, Owner tự đăng ký [merchant sandbox](https://sandbox.vnpayment.vn/devreg/) bằng email đang dùng và URL FE; tự đặt mật khẩu/nhập CAPTCHA. Theo [VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), hệ thống gửi thông tin kết nối về email đăng ký. Thay cả mã và key trên Render bằng đúng bộ nhận được; không suy key từ TmnCode hoặc dùng ngẫu nhiên key trong tutorial. Cookie/login lỗi thì xử lý trước theo [hướng dẫn CORS/phiên](../SCMS-HUONG-DAN-CORS-DEMO-CLOUDFLARE.md).

`note.md` có đoạn gọi cặp cấu hình cũ là “Sandbox chung”; đây không phải bằng chứng VNPay cấp cho nhóm. `.env` local cũng không chứng minh cặp nào đang hiệu lực ở gateway. Không thay cấu hình Render bằng cặp tìm được trong ghi chú chỉ vì nó khác cặp đang lỗi; cần nguồn cấp/email hoặc xác nhận từ VNPay. Không chép key từ các file này sang tài liệu/Git.

**Điền form đăng ký hiện hành:** mở [Đăng ký Merchant môi trường test](https://sandbox.vnpayment.vn/devreg/), điền theo bảng dưới rồi tự gửi form:

| Trường | Giá trị cho project |
|---|---|
| Tên website | `SCMS Demo` |
| Địa chỉ URL | `https://scms-demo-04nov.pages.dev` — website FE công khai, không phải endpoint Return/IPN |
| Email đăng ký | Email thật Owner/nhóm kiểm soát và nhận được thư; không dùng email account demo `@scms.com` |
| Mật khẩu | Owner tự đặt; form hiện giới hạn6–15ký tự. Dùng chữ hoa, chữ thường và số; đây là mật khẩu merchant, không phải HashSecret |
| Nhập lại mật khẩu | Nhập lại đúng mật khẩu vừa đặt |
| Nhập mã xác nhận | Nhập CAPTCHA đang hiện trên chính form |

Kiểm Inbox/Spam sau khi đăng ký; nếu báo email đã tồn tại thì dùng luồng khôi phục trên cổng VNPay hoặc liên hệ hỗ trợ, không đoán key. Khi nhận bộ kết nối, lấy `vnp_TmnCode` và `vnp_HashSecret` điền vào hai biến Render cùng tên tương ứng ở bước1 bên dưới. Nếu bước tiếp theo yêu cầu callback: Return=`https://scms-app-p1gy.onrender.com/api/payments/vnpay/return`; IPN=`https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`. `FRONTEND_PAYMENT_RESULT_URL` có thể dùng trang hóa đơn đang tồn tại `https://scms-demo-04nov.pages.dev/member/invoices`; đây chỉ là trang đọc trạng thái, không cập nhật Paid. Deploy xong lấy URL thanh toán mới vì link cũ có chữ ký/key và hạn cũ.

**Nếu nhập OTP xong bị chuyển tới localhost/payment-result:** `payment.frontend-result-url` đang dùng giá trị local (mặc định khi thiếu `FRONTEND_PAYMENT_RESULT_URL`, hoặc bị override). Thêm/sửa biến dưới đây trên đúng service Render, Save/rebuild/deploy rồi chờ Live:

```text
FRONTEND_PAYMENT_RESULT_URL=https://scms-demo-04nov.pages.dev/member/invoices
```

Giữ `VNPAY_RETURN_URL` trỏ endpoint BE như trên; không thay nó bằng trang hóa đơn FE. Trong DevTools, request BE `/api/payments/vnpay/return?...` trả302 thì xem Headers → Response Headers → `Location`; body có thể trống. Request tiếp theo tới localhost bị `ERR_CONNECTION_REFUSED` không có HTTP response nên không thể đọc Response. Backend có thể kiểm riêng cấu hình redirect bằng GET Return không tham số, chặn follow redirect: signature bị từ chối97 và chỉ xem Location, không gọi gateway/IPN hoặc xác nhận giao dịch.

Đối soát **hóa đơn cũ trước khi thanh toán lại**: đăng nhập FE bằng đúng Member, xác minh `/api/auth/me`200, mở `/member/invoices` và đọc `GET /api/members/me/invoices` hoặc `/{invoiceId}`. Xem `paymentStatus`, `reconciliationRequired`, `paymentDate`, `gatewayTransactionRef`. `status=00` do BE gắn vào URL kết quả hiện chỉ xác nhận chữ ký Return và tìm thấy hóa đơn; không chứng nhận Paid hay transaction response thành công. Nếu hóa đơn vẫn Pending/Expired sau lần sandbox đã xác nhận thành công ở ngân hàng, kiểm merchant IPN URL và log server; IPN là server-to-server nên không xuất hiện trong Network FE. Không sửa DB/tự gọi IPN hoặc suy Paid từ redirect; Paid cần đối soát theo D10 khi quyền giữ/hạn không còn.

Nếu đã quay về đúng Pages `/member/invoices` nhưng còn “Chờ thanh toán”, không đổi FRONTEND_PAYMENT_RESULT_URL lần nữa. Bấm **Tải lại**, kiểm response mới nhất của ownInvoice sau khi có phiên: API Paid nhưng UI chưa đổi thì kiểm FE refresh/cache; API Pending/Expired thì kiểm IPN chưa ghi nhận/đã bị từ chối/lỗi. Không lấy nhãn UI hay query `status=00` làm bằng chứng trạng thái DB. Nhánh IPN hiện không có log nhận request/RspCode riêng; log console trống không thay được lịch sử gọi từ VNPay hoặc access log thực tế. Chờ xác minh cấu hình IPN phía VNPay trước khi tạo thêm giao dịch sandbox.

Nếu đọc được `Expired`, `paymentDate=null`, `gatewayTransactionRef=null`: chưa có bằng chứng IPN thành công đã commit. Không kết luận gateway chưa gọi chỉ từ ba field này; IPN có thể chưa tới, bị từ chối, lỗi hoặc giao dịch ngân hàng không thành công. Trong Network của **lần cũ**, chọn BE `return?vnp_...`302 → Payload → Query String Parameters (hoặc Headers → Request URL), lấy riêng `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_TransactionNo`, `vnp_PayDate`; không cần gửi chữ ký/full URL. Kiểm IPN URL đã đăng ký với **VNPay**: `https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`. Chỉ đặt biến Render không đăng ký IPN với gateway. Nếu cổng merchant không có nơi cấu hình thì gửi URL này cho hỗ trợ VNPay và nhờ đối chiếu lịch sử gọi IPN/RspCode của giao dịch cũ theo [hướng dẫn PAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html). Kiểm log đúng khoảng thời gian; không có dòng trong console ứng dụng chưa chứng minh không có request nếu access logging chưa bật. Theo D10, IPN thành công hợp lệ đến sau hạn vẫn phải ghi nhận Paid/cần đối soát và không hồi sinh quyền; không tăng thời gian giữ hoặc sửa Expired thành Paid bằng tay để vượt bước nghiệm thu. Chỉ thử sandbox mới sau khi chốt kết quả giao dịch cũ và cấu hình callback/redirect đúng.

**Kiểm IPN sandbox trước khi thêm relay:** [hướng dẫn của SDK cộng đồng lehuygiang28/vnpay](https://vnpay.js.org/ipn/config-ipn) chỉ chỗ tự đặt IPN tại **Thông tin tài khoản** góc phải → danh sách website → biểu tượng sửa Terminal. Sau đăng nhập có thể mở [TerminalEdit](https://sandbox.vnpayment.vn/merchantv2/Account/TerminalEdit.htm). Owner đã mở được form và báo bấm Hoàn thành với IPN URL `https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn`, GET và HMACSHA512; tiếp tục kiểm lưu/callback theo mục bên dưới. Chọn đúng Terminal/TmnCode đang dùng. Menu **Cài đặt thông báo** Owner đã mở chỉ là email, khác Terminal. Nếu tài khoản khác không có quyền/trường này thì dùng hỗ trợ bên dưới. Không suy IPN từ website/Return URL; không đưa HashSecret lên ảnh/báo cáo. Cấu hình xong vẫn cần xác minh callback thật và state đã commit, không mặc định đã sửa xong.

<a id="ipn-acceptance"></a>
**Sau khi bấm Hoàn thành ở Terminal:** mở lại đúng Terminal để kiểm IPN URL còn giữ nguyên, giao thức GET và HMACSHA512. Không cần deploy BE, chạy SQL hoặc đổi env cho thao tác lưu phía VNPay. Lượt sandbox sau cấu hình đã được Owner gửi response Paid và thấy FE cập nhật ngay; bằng chứng/giới hạn tại [Active](00-MASTER-CHECKLIST.md#active). Checklist dưới dành cho lần nghiệm thu được phép khác, không yêu cầu tạo thêm đơn để lặp ca đã thành công.

1. Dùng tài khoản/fixture sandbox được phép, xác minh `/api/auth/me`200 và mở F12 → Network → Preserve log. Tạo đúng một đơn thử riêng, không thu lại hóa đơn cũ đang chờ đối soát. Ghi invoiceId/code và expiresAt.
2. Thanh toán trên `sandbox.vnpayment.vn` bằng [thẻ thử NCB do VNPay công bố](https://sandbox.vnpayment.vn/apis/vnpay-demo/); không dùng app ngân hàng/thẻ thật. Hoàn tất trong thời gian giữ5phút.
3. Sau Return, đọc GET ownInvoice mới sau commit (hoặc tải lại danh sách), không dùng receipt trước Paid hay `status=00` làm kết quả. Cần paymentStatus=Paid, paymentDate/gatewayTransactionRef có giá trị; nếu xử lý muộn hoặc quyền không còn thì Paid+reconciliationRequired=true và không cấp lại quyền. Nếu còn Pending, chờ15–30giây và GET lại một lần, giữ nguyên đơn khi chẩn đoán.
4. IPN là server→server, không có request IPN trong DevTools FE. Pending/Expired chưa chứng minh IPN không tới; cần kiểm provider delivery/HTTP/RspCode nếu chưa Paid. QueryDR vẫn có thể timeout độc lập; không sửa chữ ký/env hoặc thêm relay chỉ vì status cũ chưa đổi. Cấu hình mới không bảo đảm gateway gửi lại giao dịch cũ. GET hiện trạng các đơn cũ không tạo thanh toán mới.

<a id="vnpay-options"></a>
**Lựa chọn đường kết nối sau probe:** IPN là VNPay→BE, QueryDR là BE→VNPay; lỗi outbound hiện tại không tự chứng minh inbound cũng lỗi. Giữ IPN là kênh thông báo chính và QueryDR là xác minh chủ động theo D10; Return chỉ hiển thị.

| Phương án | Đánh đổi và phạm vi BE |
|---|---|
| Tự cấu hình IPN sandbox | Kiểm trước: dùng endpoint BE hiện có, không cần relay hoặc migration mới. Không chữa đường QueryDR và không bảo đảm VNPay tự gửi lại giao dịch cũ. |
| Khôi phục Render→VNPay trực tiếp | Ít thành phần vận hành; cần khoanh vùng mạng/provider, chưa có thời hạn xử lý. IP riêng chỉ cân nhắc khi có căn cứ allowlist; [Render yêu cầu Pro và phí riêng](https://render.com/docs/dedicated-ips), không bảo đảm mua gói sẽ hết lỗi. |
| Relay local qua Cloudflare Tunnel | Giữ BE/DB hiện tại nhưng thêm transport có xác thực và giới hạn đích. Phải giữ máy/mạng/process chạy; [Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/) đổi hostname mỗi lần, không bảo đảm uptime; phù hợp demo tạm. |
| Relay chạy cloud (Worker/VM) | Không phụ thuộc laptop; thêm dịch vụ, auth/token, giới hạn truy cập và vận hành. [Worker có fetch outbound](https://developers.cloudflare.com/workers/runtime-apis/fetch/) nhưng phải probe Worker→VNPay trước, chưa có bằng chứng tuyến này hoạt động. |
| Chuyển BE sang host/region khác | Giữ QueryDR trực tiếp nhưng phải kiểm tuyến mạng trước, chuyển env/deploy và có thể cập nhật API base/CORS/Return/IPN. Phạm vi lớn hơn relay, chưa cần làm ngay. |

Mọi relay chỉ chuyển tiếp request đã ký tới đích VNPay cố định và trả nguyên phản hồi; HashSecret, xác minh HMAC/merchant/reference/amount/status và nhánh ghi Paid vẫn ở BE. Cần thêm transport/auth có kiểm thử, không chỉ thay URL sang một proxy công khai. Endpoint FE và migration15 không cần thay vì lựa chọn đường mạng. Sau cấu hình Terminal, lượt sandbox đã ghi Paid; Owner chốt giữ nguyên phần đã triển khai, chưa chọn relay. Không thay SDK để chữa timeout: [SDK TypeScript](https://github.com/lehuygiang28/vnpay/blob/43627794aa4735e82d539831b81d53c8891cce19/src/services/query.service.ts) vẫn POST HTTP; [Omnipay PHP](https://github.com/phpviet/omnipay-vnpay/blob/91e2059838d44afa42f0173122fc499a709f7e7b/src/Message/QueryTransactionRequest.php) dùng GET/merchant.html cũ, khác [QueryDR2.1 POST JSON](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html) của SCMS.

<a id="querydr-proposal"></a>
<a id="querydr"></a>
**QueryDR — các bước cho demo sandbox**

**Quyết định hiện hành:** Owner giữ nguyên code/endpoint, bảng `vnpay_payment_attempts`, migration15 và các env đã thêm. Không thực hiện đề xuất tắt QueryDR trước đó; bảng vẫn được dùng khi tạo link thanh toán. Các bước dưới là cấu hình/kiểm tra khi cần, không yêu cầu chạy lại SQL, đổi env hoặc deploy chỉ vì đổi tên tài liệu. QueryDR live vẫn chưa nghiệm thu; xem [D10](04-DEBUG-NOTE.md#d10).

1. DB hiện có: chạy nguyên [15_QueryDR.sql](../database/migrations/15_QueryDR.sql) một lần trước deploy BE. File chỉ thêm bảng pay attempt và các ràng buộc liên quan, không sửa/xóa chứng từ cũ. Nếu đã chạy thành công thì chuyển bước2.
2. Render → Environment: dùng cấu hình dưới đây, chọn **Save, rebuild, and deploy**, chờ **Live**.

   | Biến | Giá trị |
   |---|---|
   | `VNPAY_QUERY_ENABLED` | `true` |
   | `VNPAY_QUERY_SETTLEMENT_ENABLED` | `true` |
   | `VNPAY_QUERY_URL` | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` |
   | `VNPAY_QUERY_IP_ADDR` | Để trống hoặc không thêm; BE tự lấy IP host |

3. Tạo đúng một đơn VNPay mới sau deploy, thanh toán bằng thẻ thử NCB. Khi quay về, FE cần POST `verify-vnpay` cho đúng `invoiceId`, rồi GET lại hóa đơn. Chỉ tải lại GET sẽ không tự chạy QueryDR. Nếu FE chưa tích hợp, dùng Console theo đoạn dưới.
4. Xem `verificationStatus`, `invoice.paymentStatus` và `reconciliationRequired`. `VERIFIED_PAID`/`ALREADY_PAID` mới xác nhận BE đã ghi nhận tiền. Lỗi/chưa xác minh được thì kiểm tra cùng đơn, không thanh toán thêm. Nếu xử lý đã tới/quá hạn5phút, Paid đi kèm đối soát và không cấp lại quyền học.

`SETTLEMENT_ENABLED=false` là **tùy chọn chỉ kiểm tra**, trả `VERIFIED_ONLY` khi VNPay xác nhận nhưng không ghi Paid. Không bắt buộc phải đi qua chế độ này trước khi bật true cho demo sandbox. Dù true, BE vẫn kiểm chữ ký/merchant/reference/amount/type/status và chống lặp. Default hai cờ trong source vẫn false. Giữ nguyên cặp merchant/key đang tạo được link pay; `VNPAY_RETURN_URL` là BE Return, `FRONTEND_PAYMENT_RESULT_URL` là trang hóa đơn Pages.

**Kiểm tra thủ công khi FE chưa tự POST:** mở trang hóa đơn vừa quay về, đăng nhập đúng Member → F12 → Console, chạy một lần:

```javascript
const api = "https://scms-app-p1gy.onrender.com";
const id = new URL(location.href).searchParams.get("invoiceId");
const me = await fetch(api + "/api/auth/me", {credentials: "include"});
if (!me.ok || !/^[1-9]\d*$/.test(id || "")) throw new Error("Cần đăng nhập và mở URL hóa đơn vừa quay về");
const result = await fetch(api + "/api/members/me/invoices/" + id + "/verify-vnpay", {
  method: "POST", credentials: "include", headers: {"Content-Type": "application/json"}, body: "{}"
});
console.log("HTTP", result.status, JSON.stringify(await result.json(), null, 2));
```

Chờ `retryAfterSeconds` trước khi chạy lại. Không lấy `status=00` trên URL làm chứng từ. `ATTEMPT_MISSING` là đơn cũ thiếu metadata gốc; cần đối soát riêng. Nếu `QUERY_UNAVAILABLE`, xem log Render dòng `VNPay QueryDR invoice=... attempt=... reason=...`; chỉ chia sẻ lý do cố định, không gửi key/cookie. Contract/status đầy đủ tại [docs01](01-API-CONTRACT-FE-BE.md#payments), bàn giao VNPay ngày04/11 tại [đây](../SCMS-BAN-GIAO-FE-VNPAY-2026-11-04.md).

**Nếu lỗi TLS/chứng chỉ Java:** có thể thử thêm Environment `JAVA_TOOL_OPTIONS=-Dcom.sun.security.enableAIAcaIssuers=true`, giữ các tùy chọn đã có nếu biến tồn tại, rồi Save/redeploy và kiểm lại **cùng đơn**. Java sẽ tìm chứng chỉ CA trung gian để dựng chuỗi tin cậy, vẫn xác minh TLS/hostname ([Oracle PKIX](https://docs.oracle.com/en/java/javase/17/security/java-pki-programmers-guide.html)). Lựa chọn này đã giúp probe HTTPS Java cục bộ kết nối được VNPay; chưa tự chứng minh nguyên nhân trên Render. Không đổi secret hoặc tạo thêm thanh toán để thử.

<a id="querydr-network"></a>
**Nếu log là CONNECT_TIMEOUT — kiểm tra trên chính Render:**

1. Merge code có `VnPayNetworkProbe` vào đúng nhánh Render deploy rồi đợi bản mới Live. Với repo hiện tại, merge `tai → main`. Chỉ thêm env/redeploy commit cũ sẽ không có `VNPAY_NET`; xác minh bản deploy trước khi tìm log.
2. Thêm Environment `VNPAY_NETWORK_PROBE_ENABLED=true`, lưu và deploy, chờ Live.
3. Mở Logs, tìm `VNPAY_NET`, lấy các dòng tới `finished=true`. Probe tự chạy một lần mỗi lần khởi động khi bật cờ; không cần tạo đơn hoặc POST để kích hoạt. Lượt hiện tại đã có log hoàn tất; Owner chốt giữ nguyên các env đã thêm, không tự tắt cờ hoặc redeploy chỉ để lặp chẩn đoán.

Probe chỉ kiểm DNS, TCP443 tối đa15giây và HTTPS HEAD ở ngưỡng kết nối3/15giây tới VNPay sandbox cùng `example.com` làm đối chứng. TLS/hostname vẫn được kiểm bình thường. Không có merchant, secret, cookie, dữ liệu hóa đơn hoặc body QueryDR; không xác nhận Paid.

- TCP tới VNPay timeout nhưng đối chứng kết nối được: khoanh vùng đường mạng tới VNPay; chưa đủ để khẳng định VNPay chặn IP Render. Khi cần, IP outbound lấy tại Render → service → Connect → Outbound theo [Render](https://render.com/docs/outbound-ip-addresses).
- TCP được nhưng HTTPS lỗi/timeout: kiểm tra TLS/handshake. HTTP405 từ HEAD chứng minh đã kết nối HTTPS, không là lỗi thanh toán vì đây không phải POST QueryDR.
- HTTPS3giây thất bại nhưng15giây thành công: có thể thử `VNPAY_QUERY_CONNECT_TIMEOUT_SECONDS=10`, `VNPAY_QUERY_REQUEST_TIMEOUT_SECONDS=15`, deploy rồi xác minh lại **cùng đơn**. Mặc định vẫn3/5; mỗi giá trị phải từ1 tới15, request không nhỏ hơn connect. Sai cấu hình trả reason `INVALID_QUERY_TIMEOUT` và không gửi request. FE dành60giây riêng cho POST xác minh.

`VNPAY_QUERY_IP_ADDR` chỉ là trường IP bên trong request, không thay đổi địa chỉ IP/đường mạng outbound của Render. Không sửa trường này để chữa TCP timeout.

Bản vá log transport mới phân biệt `CONNECT_TIMEOUT`, `REQUEST_TIMEOUT`, `TLS_FAILURE`, `DNS_FAILURE`, `SERVER_IP_LOOKUP_FAILED`, `CONNECTION_FAILED`, `IO_FAILURE`, `PROVIDER_HTTP_<status>` và cấu hình sai. Phản hồi FE vẫn là `QUERY_UNAVAILABLE`; log không chứa exception message, body hoặc credential. Nếu runtime vẫn chỉ ghi reason chung thì bản vá chưa được publish hoặc lỗi chưa thuộc nhóm đã phân loại. Trạng thái publish theo docs00.

<details>
<summary>Chi tiết kỹ thuật và kiểm thử local — không phải bước cấu hình Render</summary>

**Request tới provider:** HTTP client chỉ POST JSON `vnp_Command=querydr`, không gọi refund. UUID32 RequestId và CreateDate do BE sinh theo Clock/HCM; TransactionDate lấy đúng vnp_CreateDate gốc trong attempt. Chuỗi HMAC-SHA512 nối `|` theo thứ tự `RequestId,Version,Command,TmnCode,TxnRef,TransactionDate,CreateDate,IpAddr,OrderInfo` (JSON có tiền tố `vnp_`), không dùng query string của pay URL. Key chỉ tính HMAC, không gửi trong request.

**Response:** kiểm HMAC-SHA512 theo thứ tự `ResponseId,Command,ResponseCode,Message,TmnCode,TxnRef,Amount,BankCode,PayDate,TransactionNo,TransactionType,TransactionStatus,OrderInfo,PromotionCode,PromotionAmount`; so sánh constant-time, giữ nguyên giá trị, tùy chọn thiếu là rỗng đúng vị trí. ResponseId không là RequestId. Chỉ ghi thành công nếu chữ ký hợp lệ, merchant khớp attempt/cấu hình, TxnRef đúng invoiceCode, Amount đúng số nguyên Invoice.amount×100 và snapshot, loại01, responseCode00/status00, TransactionNo hợp lệ/chưa gắn invoice khác. Lỗi/timeout/thiếu hash/chưa có kết quả không ghi Paid và không chứng minh khách chưa trả.

**Khóa/giới hạn:** mặc định connect3giây/request5giây, có cấu hình giới hạn tại mục kiểm mạng; tối đa3attempt/lượt, luân phiên để không bỏ link cũ; cooldown30giây/hóa đơn bằng DB lock. HTTP chạy ngoài transaction giữ khóa. Khi ghi, kiểm lại owner/role/center/amount/state dưới khóa center→invoice. IPN tới trước thì trả state hiện hành, không cấp thêm credit/thông báo. Biên5phút theo Clock lúc xử lý, không dùng PayDate bỏ cutoff: tới/quá hạn hoặc quyền không còn thì Paid+reconciliationRequired, không hồi sinh quyền. Đơn cũ thiếu metadata trả `ATTEMPT_MISSING`; deploy không tự sửa invoice20/22 đã báo trước đây.

**Kiểm local:** chạy `python scripts/test-querydr-migration.py`, rồi `python scripts/test-querydr-local.py`. Migration runner chỉ dùng PostgreSQL Docker loopback55440 và DB tạm `scms_querydr_*`. BE runner tạo bản sao source không có.env, chạy `./mvnw.cmd clean test` rồi HTTP PostgreSQL `ddl-auto=none` trên DB behavior đã migration. Kết quả ở `artifacts/runs/querydr/validation.json`, tổng hợp vào QA duy nhất. Provider là HTTP giả lập cục bộ, không là VNPay live. Có kiểm chữ ký độc lập, sai merchant/ref/amount, timeout, owner/role/center, retry/IPN race, cutoff,12credit/thông báo không trùng và state sau commit. Các lần kiểm local không thay thế kiểm QueryDR có phiên trên Render; trạng thái hiện hành xem docs00.

</details>

Có thể gửi yêu cầu qua kênh hỗ trợ trong email cấp merchant hoặc `hotrovnpay@vnpay.vn` (địa chỉ được [VNPay công bố](https://sandbox.vnpayment.vn/paymentv2/)); Owner tự gửi, không đính kèm HashSecret/cookie/mật khẩu. Mẫu nội dung:

```text
Nhờ hỗ trợ kiểm tra/thiết lập IPN cho merchant sandbox [TmnCode].
Website: https://scms-demo-04nov.pages.dev
IPN URL (GET): https://scms-app-p1gy.onrender.com/api/payments/vnpay/ipn
Return URL: https://scms-app-p1gy.onrender.com/api/payments/vnpay/return
Giao dịch: [vnp_TransactionNo], mã đơn [vnp_TxnRef], số tiền [VND].
Thời gian thành công: [vnp_PayDate theo GMT+7]. Merchant báo thành công
nhưng hóa đơn SCMS chưa ghi nhận thanh toán. Nhờ xác nhận IPN URL đang
hiệu lực, lịch sử gọi/HTTP status/RspCode, và khả năng gửi lại IPN
cho giao dịch này sau khi khắc phục nếu được hỗ trợ.
```

1. Trong Render → service `scms-app-p1gy` → Environment, đối chiếu `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` với **cùng bộ sandbox** VNPay cấp. Không kèm dấu nháy hoặc khoảng trắng/xuống dòng đầu-cuối. Kiểm thêm Environment Groups và cấu hình Spring override nếu có; `.env` local không chứng minh cấu hình Render. Nếu sửa giá trị, chọn **Save, rebuild, and deploy**, đợi deploy Live rồi lấy URL mới; Restart service giữ cấu hình của lần deploy đang chạy. [Render Environment](https://render.com/docs/configure-environment-variables), [Deploys](https://render.com/docs/deploys).
2. Đối chiếu `VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` và `VNPAY_RETURN_URL=https://scms-app-p1gy.onrender.com/api/payments/vnpay/return`. Return trỏ BE; IPN gateway trỏ `/api/payments/vnpay/ipn`, không gọi tay để thử. `FRONTEND_PAYMENT_RESULT_URL` là trang FE đọc trạng thái hóa đơn, không thay BE Return. FE hiện hành cần có route thật; `/member/invoices` là trang hóa đơn đang phát hành, không tự mặc định FE đã có `/payment-result`.
3. Trong Chrome/Edge: F12 → Network → bật **Preserve log** trước thao tác trên giao dịch demo được phép. Trong request `pay-online` hoặc `order`, mở Response, lấy **giá trị chuỗi** `paymentUrl` nguyên vẹn, không kèm dấu nháy JSON. Chọn request đầu tiên tới `vpcpay.html`, lấy **Headers → General → Request URL**. URL trang `Error.html` không thay được hai URL này. Không cần hoàn tất thanh toán để lấy dữ liệu; ưu tiên request còn trong log, không tạo hàng loạt đơn hay dùng account seed lịch sử. [Network reference](https://developer.chrome.com/docs/devtools/network/reference/).
4. Có thể gửi riêng hai URL cho người kiểm tra, không gửi HAR/cookie/password. Hoặc tự kiểm offline tại root repo:

   ```powershell
   python scripts/check-vnpay-payment-url.py --compare-browser --check-secret
   ```

   Dán URL theo hai lời nhắc; nhập TmnCode và HashSecret **VNPay cấp** khi được hỏi. HashSecret nhập ẩn, không lưu hoặc gửi mạng; không đặt secret vào lệnh. Chỉ gửi JSON kết quả. Muốn kiểm cấu trúc/so sánh mà chưa kiểm key thì bỏ `--check-secret`. Công cụ không đọc `.env`, không gọi gateway và không thay invoice/credit.

| Kết quả offline | Hướng xử lý |
|---|---|
| Chữ ký khớp key trong ảnh/cấu hình Render và hai URL giống nhau, nhưng gateway vẫn sai chữ ký | Chứng minh request nhất quán với key cấu hình, chưa chứng minh key thuộc merchant ở VNPay. Lấy lại bộ thông tin cấp hoặc nhờ VNPay kiểm merchant/key đang hiệu lực; không đổi encoder/IP/trim chỉ để thử. Khi liên hệ gửi mã tra cứu, thời gian, TmnCode và ảnh lỗi; không gửi HashSecret. |
| `signatureMatchesEnteredSecret=true`, merchant đúng, gateway sandbox đúng | Link khớp key vừa nhập; tiếp tục đối chiếu merchant được VNPay kích hoạt và bộ key đúng môi trường, rồi báo VNPay kèm mã tra cứu nếu gateway vẫn từ chối. Không tự gọi đó là PASS gateway. |
| Chữ ký không khớp key VNPay cấp | Đối chiếu key **hiệu lực trên Render** và source deployed; chưa đủ để kết luận encoder sai. |
| `browserComparison.decodedFieldsEqual=false` | Xem `changedParameters`, tìm đoạn FE/redirect thay dữ liệu. Khác cách biểu diễn nhưng decoded/canonical vẫn giống chưa chứng minh lỗi. |
| `enteredSecretHasEdgeWhitespace=true` | Kiểm tra thao tác copy key; sửa cấu hình sai ở nguồn. Không tự trim riêng hàm HMAC và bỏ quên callback. |
| Return URL sai BE hoặc FE result sai route | Sửa đúng cấu hình điều hướng; đây là lỗi riêng, chưa chứng minh nguyên nhân sai chữ ký. |

Giữ HMAC-SHA512 và encoding nhất quán cho URL/chuỗi ký theo [VNPay 2.1](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html); không thay IPv6 hợp lệ thành localhost chỉ vì có dấu `:`. Kiểm local không thay giao dịch sandbox được gateway chấp nhận, IPN và trạng thái sau commit. Bằng chứng hiện hành xem docs00/QA, không ghi key hoặc URL giao dịch thật vào Git.

## 4. Bundle và backup

QA giữ target production188f6765… khác DB test1a3dfdd7…. Production có dữ liệu đã nâng cấp bằng [production-upgrade.sql](../artifacts/releases/2026-10-27/production-upgrade.sql), SHA256 **01e31ca6a8606b47d321fd61a6860076c68317d3490f3b529cf42414a80db8d7**. Đây là bundle đã chạy, replay guard từ chối; không phải việc cần chạy lại. Execution/postflight/backup metadata ở `artifacts/qa/latest.json →releases.production`. Giữ cột gốc7bảng, không backfill legacy từ amount hoặc cấp lại12credit.

Bootstrap test cũ trong Git có hash khác biên bản28635081…; chưa xác định nguyên nhân. Bản tham chiếu lỗi thời bị loại, QA giữ khoảng trống integrity. Không gọi nó là đúng byte đã duyệt hoặc suy target từ filename “production” cũ. Bootstrap mới cần sinh/review/kiểm/hash mới qua scripts/prepare-phases-release.py; quyền cloud phải đúng bundle/target.

Backup public schema mã hóa DPAPI current Windows user ở `artifacts/private-backups/` ignored, giữ nguyên khi dọn. Không gồm Supabase auth/storage ngoài public; cần tài khoản Windows đã mã hóa để decrypt. Khôi phục local mới bằng `python scripts/prepare-production-upgrade.py --restore-backup <backup-path-from-QA>`; script guard private-backups path và container loopback, không truy cập remote ở chế độ restore.

Chế độ --target-fingerprint của script chuẩn bị đọc/backup remote và kiểm clone, không tự chạy remote DDL; output review vào `artifacts/runs/production-upgrade/<timestamp>`. Approval cũ không mở quyền bundle/target khác, seed/account/giao dịch hoặc live tests mới.

## 5. Hạn chế và lưu trữ

### Chẩn đoán OTP SMTP

Deploy source có `SMTP_SEND_START/SMTP_SEND_FAILED/SMTP_SEND_ACCEPTED`, chờ Live; không cần bật JavaMail debug hoặc thêm env để có log. Giữ nguyên provider/port/credential khi lấy bằng chứng ban đầu. Mở Render Logs, chờ ít nhất60giây từ lần gửi trước rồi bấm Gửi lại mã đúng1lần từ FE; tối đa3OTP/giờ theo contract. Đợi khoảng20giây và tìm `SMTP_SEND_`, lấy các dòng có cùng `attemptId` cùng dòng `OTP email delivery failed...` nếu có. Không chép key, OTP hoặc nội dung thư vào chat/báo cáo.

`SMTP_SEND_START` ghi host/port, STARTTLS **được cấu hình** và ba cờ login/password/sender có giá trị; không ghi các giá trị nhạy cảm hoặc khẳng định TLS đã thương lượng. `SMTP_SEND_FAILED` ghi category, mã SMTP (`-1` là không có), tên loại exception và elapsedMs; không ghi raw message/stack trace, recipient hoặc failed-message map. Nhóm lỗi: DNS, TIMEOUT, CONNECTION, TLS, AUTHENTICATION, SMTP_REJECTED, MESSAGE_FORMAT, MISSING_SENDER hoặc UNKNOWN. TIMEOUT/CONNECTION chưa đủ chứng minh Render hay Brevo chặn; AUTHENTICATION cần kiểm SMTP login/key và chính sách tài khoản; SMTP_REJECTED cần đối chiếu smtpCode và trạng thái Brevo.

Khi AUTHENTICATION và Brevo bật chặn SMTP IP: mở **Brevo → Settings → Security → Authorized IPs**, kiểm trạng thái SMTP keys và tab Unauthorized IP addresses. Lấy dải IP tại **Render → Projects → project chứa backend → service backend → Connect → Outbound**, không phải Settings của workspace. Chỉ cho phép IP đã đối chiếu thuộc dải outbound của đúng service; không dùng Remote address của request FE trong Chrome. Quyền IP lưu trên Brevo không cần deploy lại BE. Nếu Render dùng IP khác sau này, kiểm lại dải/entry bị chặn trước khi bổ sung; không tắt chặn hoặc thay key chỉ để thử khi chưa kiểm quyền IP. [Render Outbound IPs](https://render.com/docs/outbound-ip-addresses), [Brevo Authorized IPs](https://help.brevo.com/hc/en-us/articles/5740111683858-Authorize-and-block-IP-addresses-for-API-and-SMTP-security).

`SMTP_SEND_ACCEPTED` chỉ chứng minh JavaMailSender trả về thành công, chưa chứng minh Inbox. Kiểm Brevo **Transactional → Logs** theo đúng thời gian/recipient, rồi Inbox/Spam và verify-registration. `code:null` của API là mã trạng thái tùy chọn, không phải OTP; POST/OPTIONS200 không là email delivery.

### Cấu hình và lưu trữ

Ghi nhận27/10: Render Free,dbUP/mailDOWN. [Render Free](https://render.com/docs/free) chặn SMTP25/465/587. Owner04/11 chọn thử Brevo SMTP2525 và báo đã xác minh sender/thêm env. Cấu hình `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=2525`, `MAIL_USERNAME` là SMTP login Brevo cấp, `MAIL_APP_PASSWORD` là toàn bộ SMTP key (không phải API key), `MAIL_FROM_EMAIL` là sender đã xác minh; giữ STARTTLS đang bật và để trống `REGISTRATION_DEMO_OTP` khi kiểm OTP thật. Không chép credential vào Git. [Brevo hỗ trợ2525](https://help.brevo.com/hc/en-us/articles/10905415650322-Which-SMTP-port-should-I-use-Port-587-465-or-2525); vẫn phải kiểm kết nối/TLS và delivery từ Render, không suy thành công từ ảnh cấu hình. Nếu không kết nối được, phương án HTTPS email API vẫn cần adapter riêng. Các cổng SQLServer/browser/gateway/full authenticated business matrix ở docs00.

FE nhận docs01 + `artifacts/fe-handoff/openapi.json`. QA ở `artifacts/qa/latest.json`; bundle ở artifacts/releases; raw mới ở artifacts/runs (ignored); private backup giữ nguyên. Lịch sử đầy đủ trước chuẩn hóa truy qua Git `a4f3cd0`, ví dụ `git show a4f3cd0:artifacts/fe-handoff/2026-10-27/render-get-checks.json`. Không phải mọi raw log đều từng commit; raw run cuối còn local như mục2. Không tạo lại inventory/schema MD/HTML trace để tăng số file bàn giao.

<a id="advanced-class-model-20261114"></a>
### Lớp Advanced theo đăng ký — bản sửa local14/11

DB đang giữ dữ liệu dùng [migration25](../database/migrations/25_advanced_registration_classes_postgres.sql), rồi [cleanup template không còn tham chiếu](../database/cleanup_unused_advanced_templates_postgres.sql). Phải dừng writer/job của BE trong cửa chuyển đổi; sau SQL mới chạy bản BE có lớp riêng. Nếu gặp SCMS_ADVANCED_CLASS_AMBIGUOUS, INCOMPATIBLE hoặc PENDING_PROPOSAL thì dừng và đối chiếu bản ghi, không bỏ guard/xóa lịch sử. Pending đề xuất ghi class cũ cần Member từ chối và Coach gửi lại qua flow được phép trước nâng cấp; không sửa payload/hash consent bằng SQL. Tiền/credit/PK buổi không bị đổi; chỉ class_id của buổi được nối theo subscription đã xác định chắc chắn.

Bundle dựng lại đã nhúng25, không chạy lại migration riêng sau bundle. Owner đã báo migration25 PASS và cleanup xóa9template, sau đó duyệt merge/deploy; trạng thái runtime hiện hành ở Active/QA. Agent không chạy SQL cloud. Sau phát hành được duyệt: auth-me đúng actor, Requested/Rejected không có lớp; Coach Accepted có classId duy nhất; Member thấy đúng lớp của mình; Paid mới có credit; đề xuất/Accept vào đúng lớp và hủy sớm hoàn đúng1credit.

Kiểm upgrade riêng: `python scripts/test-advanced-class-migration-postgres.py --database scms_schema_adv_class_<ma_moi> --legacy-schema <snapshot_bundle_truoc25> --output <proof_json>`. Chỉ dùng DB local mới trong container được ghim; có cùng tùy chọn container/port loopback của runner bundle. Snapshot cũ chỉ làm fixture nâng cấp, không dùng để reset cloud. Active/QA giữ kết quả và source fingerprint.
