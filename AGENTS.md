# AGENTS.md — Nguyên tắc làm việc SCMS

## 0. Bối cảnh project và cách trao đổi

- Đây là **project học tập**; mọi dữ liệu, kế hoạch và thiết kế của SCMS được xem là **demo**. Nhóm phát triển còn ít kinh nghiệm, nên tài liệu, source, schema và dữ liệu thực tế đều cần double-check; không xem bất kỳ nguồn nào là mặc nhiên đúng.
- Chủ động chỉ ra sai sót/mâu thuẫn, phản biện và đề xuất lại bằng ví dụ cụ thể. Hỏi Owner khi cần chọn hướng nghiệp vụ hoặc khi căn cứ đã chốt có mâu thuẫn thực chất; không dùng nhãn “đã chốt” để bỏ qua lỗi, cũng không hỏi lại chỉ vì nguồn lịch sử chưa cập nhật.
- Xưng hô **Tôi–Bạn** trong trao đổi với người dùng.
- Yêu cầu dựng lại/làm sạch DB demo được xử lý theo đúng phạm vi Owner giao; không tự áp quy trình dữ liệu production cho dữ liệu demo. Phân biệt chuẩn bị/kiểm script với thực thi trên Supabase, và bảo vệ credential cùng các thành phần hạ tầng không thuộc phạm vi SCMS.
- Viết câu đủ ý, nêu rõ ai thực hiện, làm gì, khi nào được phép và kết quả; dùng dấu câu và từ nối để người đọc theo được luồng. Tránh chuỗi từ viết tắt hoặc dấu `/` thay cho lời giải thích.
- Giải thích thuật ngữ bằng tiếng Việt ở lần đầu sử dụng. Đặt endpoint, field và state ở phần kỹ thuật liên quan; không dùng chúng thay cho toàn bộ câu mô tả nghiệp vụ.
- Với mỗi mốc thời gian, ghi rõ tính từ sự kiện nào và hết hạn khi nào; phân biệt hạn xác nhận mua, hạn thanh toán, hạn Accept lịch và hạn sử dụng quyền học.
- Khi Owner sửa câu hoặc phản hồi khó hiểu, kiểm lại cả ý nghĩa và căn cứ trước khi đề xuất câu thay thế. Sửa diễn đạt không tự mở phạm vi thay đổi nghiệp vụ; nếu Owner yêu cầu chỉ đề xuất để duyệt thì chờ duyệt trước khi áp dụng.

## 1. Bắt đầu session

1. Đọc file này và **phần Active** của [docs/00-MASTER-CHECKLIST.md](docs/00-MASTER-CHECKLIST.md).
2. Đọc bảng quyết định trong [docs/04-DEBUG-NOTE.md](docs/04-DEBUG-NOTE.md), rồi đúng Dxx thuộc nhiệm vụ.
3. Chỉ mở phần API liên quan trong docs01; docs02 khi cần test/môi trường/deploy.
   Khi debug/sửa DB, đọc [database/README.md](database/README.md) để chọn migration giữ dữ liệu hay bản `database/` dựng lại; mở đúng bảng/trigger và source BE liên quan.
4. Kiểm git status/diff và source thực tế trước khi sửa. Bản bàn giao đang làm lấy từ Active; `docs/reference/` chỉ tra đúng luồng, `docs/archive/` và old-docs không thuộc bộ đọc mặc định. Không đọc hàng loạt artifacts/log/history.

Mỗi nguồn có vai trò riêng: quyết định Owner mô tả điều phải làm; contract mô tả giao tiếp đã công bố; code mô tả implementation; test/log là quan sát trong một môi trường. Không mặc định nguồn nào đúng100%. Khi chúng lệch nhau, xác định nguyên nhân trước; hỏi Owner nếu phát sinh lựa chọn nghiệp vụ mới. Không hỏi lại quyết định đã chốt chỉ vì code hoặc tài liệu lịch sử chưa cập nhật.

## 2. Môi trường

- Windows/PowerShell; dùng đường dẫn `/` hoặc `\\`.
- Java 17, Spring Boot 3.3.5, Maven wrapper `.\mvnw.cmd`.
- PostgreSQL/Supabase là môi trường đang dùng; SQLServer có scripts nhưng runtime chưa được nghiệm thu.
- Mọi thời điểm nghiệp vụ dùng Spring bean `Clock`, `Asia/Ho_Chi_Minh`. Không dùng `LocalDateTime.now()` thiếu Clock.
- FE_Project-main là bản tham khảo cũ; nguồn FE hiện hành phải được xác định khi tích hợp.
- Khi chuẩn bị bộ demo hoặc đổi ngày anchor, đọc [rule anchor/bundle](docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules) và [kịch bản demo](docs/reference/SCMS-SCRIPT-DEMO-BE.md). Sinh/kiểm local không tự mở quyền reset cloud; phần chuẩn bị/màn hình/thao tác FE trong kịch bản là gợi ý để FE duyệt, quyết định cuối thuộc FE.

## 3. Ràng buộc bắt buộc

- Invoice.amount/purchaseAmount đóng băng khi tạo đơn; thay bảng giá không sửa chứng từ cũ.
- Paid hợp lệ mới kích hoạt quyền. Callback kiểm chữ ký/reference/amount/idempotency; Return không ghi Paid.
- Không tin amount/role/actor/memberId từ client; kiểm Auth, owner/center, active/block, quyền học, hạn, credit, sĩ số và trùng tài nguyên.
- Ghi tranh chấp tài nguyên dùng transaction và DB lock, ví dụ findByIdForUpdate; giữ thứ tự khóa nhất quán. Retry không nhân hóa đơn, buổi, credit hoặc thông báo.
- Rule Normal/Advanced, hủy trước ít nhất12h,45 ngày sử dụng, attendance và ngoại lệ TT theo Dxx hiện hành trong docs04. Không khôi phục cutoff hủy24h hoặc hạn1080giờ; hạn24h xác nhận mua ở D03 là bước riêng.
- Bundle Advanced phải theo D03/D04: Requested/Rejected không có lớp; Coach nhận mới có một lớp riêng cho đúng đăng ký/Member/Primary Coach/môn, nối quyền học và buổi đúng chủ. Không seed lớp mẫu theo môn, suy Paid từ việc có lớp hoặc bịa consent cho lịch sử; số lớp lấy từ đăng ký đã nhận, không lấy số lớp live làm quota fixture. Kiểm cả nguồn sinh và dữ liệu sau chạy SQL local trước khi thay bundle bàn giao.
- Giữ code/tests chế tài khóa7ngày; default toggle tắt. Giữ lịch sử attendance và chứng từ; không sửa dữ liệu để làm kết quả đẹp.
- Migration nâng cấp có guard/tương thích/replay phù hợp; script dựng lại demo phải ghi rõ phạm vi xóa/tạo và được kiểm riêng. Không DROP TABLE/TRUNCATE/reset ngoài phạm vi Owner yêu cầu. Quyền mutation cloud phải đúng phạm vi được giao; chuẩn bị bundle không tự mở quyền chạy seed hoặc live tests ngoài bundle.
- .env, credential, cookie, OTP/QR secret và backup riêng tư không đưa lên Git hoặc báo cáo. Không dùng tài khoản seed lịch sử để thử production.

## 4. Kiểm thử và kết luận

- Trước sửa, xác định Dxx, điều kiện nghiệm thu và fixture. Expected dựa quyết định đã chốt; code và Swagger cùng nguồn sinh không là kiểm chứng nghiệp vụ độc lập.
- Trong lúc sửa dùng test đích; sau mỗi nhóm thay đổi BE phải chạy `.\mvnw.cmd clean test` và kiểm liên quan. Không chạy hai Maven clean trên cùng target.
- Nếu rule mới làm expected cũ sai, giải trình trước khi cập nhật. Không đổi expected chỉ để xanh.
- Test tiền/quyền/lịch cần kiểm response và trạng thái lưu sau commit; kiểm owner/role/center, biên thời gian, retry/race phù hợp rủi ro.
- 401 anonymous chỉ là cổng đăng nhập; không tính là pass nghiệp vụ. Xác minh `/api/auth/me` trước nhóm kiểm có phiên.
- PASS/FAIL/BLOCKED/NOT_RUN phải kèm phạm vi/môi trường/SHA. Skip không phải pass; count lịch sử không là quota.
- Không gọi local HTTP là gateway live, browser FE hoặc authenticated business trên Render. Chỉ chạy lại kiểm rộng khi có diff, failure hoặc nghi vấn mới.
- Chỉ thay docs/scripts bảo trì thì kiểm syntax, dữ liệu, liên kết và hành vi liên quan; không tự gán kết quả BE mới từ lần test cũ.

## 5. Kỷ luật tài liệu và context

| Nguồn | Trách nhiệm duy nhất |
|---|---|
| docs00 | Một trạng thái hiện hành cho mỗi việc, phần còn mở, đã giải quyết và HISTORY ngắn; đầu mối mở session |
| docs01 | Contract bàn giao FE theo luồng, field/state/error/ví dụ |
| docs02 | Cấu hình, test runner, migration/deploy/recovery |
| docs04 | Quyết định D01–D19 và lý do; không ghi nhật ký deploy ở đây |
| database/README.md | Chọn script DB, giải thích mô hình và phân biệt schema hiện hành với migration lịch sử |
| artifacts/fe-handoff/openapi.json | Một snapshot schema máy đọc cho FE |
| artifacts/qa/latest.json | Một bản bằng chứng máy đọc; mở theo câu hỏi, không nạp mặc định |
| docs/reference/ | Yêu cầu chi tiết và bản bàn giao còn hiệu lực; chỉ mở đúng luồng |
| docs/archive/YYYY-MM/ | Bản đã được thay thế, báo cáo và checkpoint cũ; lưu trữ không đồng nghĩa nghiệm thu |

- Không thêm tài liệu nếu đã có chỗ phù hợp. Không sinh lại bản Markdown của toàn OpenAPI hoặc nhân đôi trace thành nhiều định dạng.
- SHA/count test/deploy chỉ cập nhật ở docs00 và QA, không sao chép vào mọi checkpoint.
- Tìm bằng rg, đọc heading/đoạn/symbol cần thiết. Log dài chỉ trích lỗi liên quan; không in toàn JSON/stack dump/bundle vào context.
- Báo cáo theo điều kiện nghiệm thu, không lấy số request/assertion làm thước đo chính. Một kết quả chuẩn, nơi khác liên kết tới nó.
- Hoàn thành nhóm việc: cập nhật Active, đưa tóm tắt vào HISTORY và ghi bước kế tiếp. Lịch sử đầy đủ truy qua Git khi cần, không tạo nhiều bản before/after trong gói FE.

### Duy trì sau mỗi đợt làm việc

- Việc mới: thêm hoặc cập nhật một dòng trong Active với mã/luồng, mục tiêu, điều kiện nghiệm thu, trạng thái và bước tiếp; dẫn đúng bản bàn giao/contract. Không tạo file chỉ để ghi một cuộc trao đổi hoặc prompt tiếp tục.
- Việc chưa xong: cập nhật ngay dòng đó với phần đã có bằng chứng, phần thiếu và cách tiếp tục. Đổi session không tạo checkpoint mới và không tự mở thêm quyền thực thi.
- Việc đã xong: chuyển khỏi nhóm đang làm sang “Đã giải quyết”, ghi một câu kết quả đúng phạm vi và một nơi chứa bằng chứng. Kiểm BE local, smoke đọc, FE lưu thành công và provider là các phạm vi riêng; phần còn thiếu vẫn ở mục còn mở.
- Kết thúc một đợt: cập nhật Active và bước tiếp; HISTORY chỉ giữ tối đa10mốc gần đây, mỗi mốc1–2câu. QA chỉ thêm kết quả kiểm/đối chiếu có ý nghĩa, giữ nguyên bằng chứng và trạng thái cũ; không tạo bản ghi cho từng tin nhắn.
- Bản bàn giao mới đặt trong `docs/reference/`, được dẫn từ Active; không thêm Markdown ở root nếu Owner không yêu cầu rõ. Bản đang soạn cập nhật tại chỗ. Sau khi gửi/chốt, giữ đúng phạm vi/ngày; lỗi diễn đạt/liên kết được đính chính có ghi chú, thay đổi chức năng mới dùng bản đúng luồng/ngày và dẫn rõ bản nào được thay thế.
- Chỉ chuyển bản cũ vào `docs/archive/YYYY-MM/` khi đã được thay thế và không còn yêu cầu duy nhất chưa chuyển. Bản còn hiệu lực nhưng không thuộc đợt hiện tại đặt trong `docs/reference/`; việc chưa nghiệm thu vẫn phải hiện ở docs00 dù bằng chứng cũ đã lưu trữ.
- Khi rút gọn, dùng Git cho lịch sử đã commit. Nội dung chưa commit cần giữ thì chuyển một bản vào archive, không tạo nhiều before/after; không xóa bản lưu duy nhất chỉ để giảm số dòng.
- Khi đổi đường dẫn, cập nhật liên kết và công cụ sinh tài liệu liên quan, kiểm file/anchor và giữ các bằng chứng lịch sử theo nguồn gốc. docs00 nên khoảng100–150dòng; khi dài lên, gộp mốc và bỏ lặp trước khi tạo tài liệu mới. Không cắt yêu cầu còn hiệu lực để đạt số dòng.

### Quy tắc ghi chú bàn giao

- Mỗi bản chỉ bàn giao một luồng, ghi rõ đối tượng và ngày; bản FE dùng tên `SCMS-BAN-GIAO-FE-<LUONG>-YYYY-MM-DD.md`. Không gộp luồng không liên quan hoặc ghi thay đổi mới vào bản của ngày cũ.
- Mỗi bản dùng 5 mục: **Kết quả hiện tại → Quá trình và quyết định giữ/bỏ → API và DB liên quan → FE cần làm → Việc còn lại**. Quá trình chỉ 3–5 mốc, mỗi mốc một câu; quyết định phải ghi rõ giữ/bỏ phần nào và lý do.
- Viết hành động cụ thể: endpoint/method, input, state/error, bước FE xử lý. Tách rõ đã quan sát thành công với chưa kiểm; không chép log dài, secret, lịch sử thử sai hoặc toàn bộ contract. Cấu hình/deploy dẫn docs02; bằng chứng dẫn docs00/QA.
- Khi cần chuẩn bị chỗ cho lượt bàn giao sau, đặt hướng dẫn tạm ngay trong file đích và đánh dấu rõ. Agent viết nội dung thực tế phải xóa phần hướng dẫn tạm, giữ lại file bàn giao; không đưa chỉ dẫn riêng của từng lượt vào AGENTS.md.
- Khi tách/đổi tên, chuyển nội dung đúng luồng và cập nhật mọi liên kết đang dùng; không tạo hai bản cùng nội dung. Chỉ ghi kết quả đã kiểm của luồng đang làm, không suy nghiệm thu từ luồng khác.

### Autoclean sau mỗi đợt

- Chủ động dọn build/cache/bản sao tạm do agent tạo khi đợt việc đã kết thúc; không hỏi lại cho phần đã xác định an toàn. Chỉ giữ workspace chính, bản đang dùng và dữ liệu cần tiếp tục; không tạo clone hoặc cài dependencies trùng khi đã có bản phù hợp.
- Trước xóa, kiểm đường dẫn tuyệt đối nằm đúng thư mục dự kiến, không đi qua symlink/junction; kiểm tiến trình và Git status, gồm file chưa theo dõi/ignored. Không xóa source chưa commit, thay đổi của Owner, credential hoặc bản sao lưu duy nhất. Với worktree sạch, bảo đảm commit còn truy được từ ref rồi dùng `git worktree remove`, không dùng force để vượt thay đổi.
- Có thể xóa dependencies/build sinh lại được của bản không dùng. Giữ báo cáo test, bằng chứng lỗi và file được QA dẫn; log lớn ưu tiên nén giữ nội dung/hash và đường dẫn. Nếu phải chuyển bằng chứng, kiểm archive giải nén/hash và ghi ánh xạ đường dẫn gốc; không sửa kết quả lịch sử thành PASS.
- Script thử một lần và staging đã hết dùng chỉ dọn sau khi kết quả cần giữ đã vào QA/artifacts hoặc Git; script bảo trì còn được dùng phải giữ trong `scripts/`. Không nhân bản toàn source chỉ để giữ một log.
- Container/DB local tạm chỉ dọn khi đúng tên/nhãn của đợt, đã lưu bằng chứng và không còn tiến trình dùng; không prune Docker/cache chung hoặc đụng Supabase để dọn dung lượng. Không xóa cache Maven/npm dùng chung chỉ để giảm số đo.
- Sau dọn, kiểm source/diff/deletion ngoài phạm vi và bằng chứng vẫn nguyên; báo dung lượng thực đo được, phần đã dọn và phần giữ lại. Không tạo tài liệu/checkpoint riêng cho từng lần autoclean; ghi ngắn tại Active/QA của đợt có liên quan.
- Nếu công cụ chặn lệnh dọn, giữ nguyên dữ liệu, ghi hành động/lý do/phần còn giữ và dung lượng thực đã dọn; chưa xóa thì ghi0byte. Không đổi shell hoặc công cụ để lách chặn, không báo autoclean đạt từ việc chỉ nén hoặc lên kế hoạch xóa.

## 6. Hướng dẫn thao tác thủ công

- Trong trao đổi với Owner, khi dẫn file local để mở/review, kèm link click được với nhãn là đường dẫn Windows đầy đủ và đích tuyệt đối tương ứng; không chỉ ghi tên file hoặc đường dẫn tương đối. Không đưa nội dung credential/secret vào trao đổi.
- Tự thực hiện phần có công cụ và quyền truy cập phù hợp; phần người dùng phải làm cần hướng dẫn cụ thể, dựa trên bước họ đã hoàn thành và giao diện hiện tại.
- Trước khi hướng dẫn, kiểm tra tài liệu chính thức hoặc ảnh/giao diện tham khảo phù hợp. Không đoán tên menu, vị trí nút hoặc tính năng chưa xác minh.
- Nếu không tìm được tài liệu phù hợp hoặc giao diện khác tài liệu, yêu cầu ảnh màn hình đúng bước đang vướng; chỉ cần vùng liên quan, che thông tin bí mật. Chỉ yêu cầu ảnh khi thiếu thông tin để hướng dẫn chính xác; chưa hướng dẫn tiếp các bước phụ thuộc vào giao diện chưa rõ.
- Chia thao tác dài thành các nhóm ngắn, đánh số theo thứ tự; ghi đúng tên menu/nút và giá trị cần nhập. Không bắt làm lại bước đã thành công nếu chưa có căn cứ cần kiểm lại.
- Sau mỗi nhóm thao tác, nêu kết quả cụ thể để người dùng đối chiếu: màn hình, thông báo, mã HTTP hoặc field/giá trị trả về. Diễn đạt rõ “Kết quả trả về … là đúng” và ý nghĩa trong phạm vi đó; nếu khác, chỉ ra bước hoặc dữ liệu cần kiểm tra tiếp.
- Phân biệt kết quả mong đợi với kết quả đã quan sát; chỉ xác nhận thành công khi có bằng chứng. Không coi lưu cấu hình, kết nối được hoặc HTTP200 là đã hoàn tất nghiệp vụ nếu chưa kiểm kết quả tương ứng.
