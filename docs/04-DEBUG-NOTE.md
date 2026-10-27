# 04-DEBUG-NOTE — Sổ quyết định và phân tích SCMS

## 0. ĐỌC ĐẦU TIÊN KHI MỞ SESSION MỚI — checkpoint phía BE

> **Lượt tiếp tục 27/10:** Owner đã yêu cầu thực hiện P1.0–P1.4 và P2, để lại P1.5/gói T/FE; sau hoàn tất phải double-check, tự dựng demo, viết bàn giao/hướng dẫn, push branch và merge main. Source đã commit trên feat/advanced-phases; checkpoint 40.13 ghi đè trạng thái “chỉ plan/merge” lịch sử bên dưới. Owner đã cho phép khởi tạo production theo bundle cụ thể; đã commit46bảng/9môn/0account/giao dịch. Không coi git merge là Render đã deploy.

### 0.1. Vai trò và thứ tự đọc

**Chúng ta chính là phía Backend (Owner + coding agent).** “Handoff BE” trong các mục dưới là ghi chú triển khai nội bộ để chính chúng ta tiếp tục, không phải chuyển việc cho BE khác. Handoff ra ngoài là hợp đồng/bằng chứng để FE tích hợp.

Đang ở giai đoạn **phân tích, chốt nghiệp vụ và chuẩn bị sửa**, chưa triển khai hàng loạt D01–D19. Quyết định đã chốt không hỏi lại chỉ vì AGENTS hoặc implementation cũ chưa cập nhật. Điểm mới mơ hồ/mâu thuẫn thì hỏi theo AGENTS §2.9.

Đọc: AGENTS.md → docs/00 phần active → mục 0 này và mục Dxx đang làm trong 04 → docs/01 mục 14 cùng endpoint liên quan → docs/02 mục 9. Chỉ đọc lại PDF/handoff/report gốc khi cần đối chiếu điểm cụ thể, không đọc lại toàn old-docs. Owner đã yêu cầu docs 04, là ngoại lệ hợp lệ với quy tắc ba docs cũ.

### 0.2. Quyết định có hiệu lực — không hỏi lại

| Nhóm | Tóm tắt; chi tiết trong các mục tương ứng |
|---|---|
| D01 | GET/PATCH /api/members/me, tự xem/sửa name, phone, address, dateOfBirth, gender, goal, healthNote; DTO riêng, danh tính từ Auth |
| D02 | Chỉ 9 môn/18 giá Member.pdf/API 2.1; giữ ID 19–27 theo snapshot, xóa 8 bản trùng và loại Bắn súng ngoài danh mục. Không Inactive/giá 0/seed ghi đè; chưa chạy SQL |
| Fixture | Enrollment 2/Class 4, Package 5 và đúng hai subscription được phép đối soát/tạo lại đúng luồng/giá/môn; không reset DB, không sửa Paid.amount hoặc suy môn từ giá; kiểm lại mapping/FK lúc triển khai |
| D03 | Mua theo môn + Primary Coach, credit/hạn riêng từng đăng ký; Subject là nguồn giá, Invoice snapshot, Paid hợp lệ mới Active/12 credit. Chọn Coach không giữ ca; đồng ý hướng kiểm năng lực/Coach nhận trước Invoice; bỏ khai lịch rảnh Member |
| Thời hạn | Quyết định mới 30.3 thay D03-c về 1080 giờ liên tục: Advanced 45 ngày sử dụng từ Paid, skip thời gian TT nghỉ/đóng, thêm/tắt ngày nghỉ cập nhật hạn gói đang dùng, không bù trùng D08. BE trả deadline chính xác, ca kết thúc không vượt hạn; Invoice vẫn 5 phút. Biên quá khứ/lịch đã đặt đã chốt 30.5 |
| D04 | Coach chính tạo sau trao đổi, Manager hỗ trợ có lý do/audit; xác nhận đã thống nhất, actor/time thật, thông báo sau commit. Member không tự tạo/không vòng duyệt ca; lock/transaction/idempotency |
| D05 | Member hủy Advanced >=12 giờ hoàn một credit gốc; dưới 12 giờ không hoàn thường. Báo vắng khi 0 < thời gian tới ca <12 giờ, lưu riêng, giữ credit/tài nguyên, điểm danh thực tế; Normal không hủy/báo vắng |
| D06 | Đủ dời/thay Coach riêng buổi/hủy-hoàn vì trung tâm. Manager ghi nhận đồng ý đúng phương án/audit/thông báo. Lỗi trung tâm cho ca mới dưới 12 giờ khi ca cũ/mới chưa bắt đầu và các điều kiện khác hợp lệ |
| D07 | Dời sớm: Coach chính/Manager hỗ trợ, ca cũ/mới đều >=12 giờ. Member bất khả kháng sát giờ: chỉ Manager, ca cũ chưa bắt đầu, ca mới vẫn >=12 giờ; giữ credit/hạn. UI tạo–dời dùng chung, điền sẵn/so sánh cũ-mới; BE chuyển lịch nguyên khối |
| Dấu báo vắng | Chỉ bỏ hiệu lực khi tính năng Dời ca thành công và thật sự đổi ngày/giờ; giữ lịch sử phía hệ thống. Đổi sân/Coach cùng giờ, no-op/preview/thất bại không bỏ |
| D08 | Gia hạn toàn gói B, bù gián đoạn thực tế không trùng, Manager duyệt thêm tối thiểu đủ ca bù; phục hồi đúng credit expiry có chứng cứ, job lưu số dư thu hồi. Chưa biết kết thúc thì Chờ khắc phục/duyệt sau, không gia hạn tạm; expired trong lúc chờ chưa đặt tiếp |
| D10 | Holding 5 phút; thiếu method/deadline chặn online, đối soát trước đơn mới. IPN cập nhật, Return chỉ hiển thị; FE refresh hữu hạn rồi Kiểm tra lại. IPN muộn ghi tiền/cần đối soát, không tự kích hoạt. CenterManager duyệt hoàn đủ ngoài SCMS, lưu chứng cứ/kết quả riêng; không cấp lại quyền/refund API trong MVP. Email chuẩn bị mở rộng sau, chưa bật gửi. Chi tiết 21.3–28.5 |
| D11 | Chốt hướng 1: PostgreSQL local riêng để kiểm nghiệp vụ/mutation, sau đó smoke deploy có kiểm soát; giữ .env đang dùng, không tạo staging mặc định. Chi tiết 29.3 |
| D12–D14 | Hạn skip và biên thay ngày nghỉ 30.3/30.5; report cả người/đăng ký, UI ưu tiên người 31.3; Holiday xem ảnh hưởng/xử lý lịch riêng rồi xác nhận 32.3 |
| D15 | Attendance mới Present/Absent trong giờ buổi, không Late/scan sớm; CenterManager sửa sau buổi có audit; sự cố thật báo muộn bù có hồ sơ/chống trùng. Check-in cửa ưu tiên nâng cấp sau. Chi tiết 33.3/33.5/34.3 |
| D16 | Chốt 2 hẹp: CenterManager đảo xác nhận Cash nhầm chưa nhận tiền cho Advanced chưa dùng/booking/phụ thuộc; giữ chứng từ và điều chỉnh riêng, không ngoại lệ Normal. Chi tiết 35.4 |
| D17–D19 | QR payload/expiresInSeconds handoff 36; report quản trị CenterManager/Admin có quyền, Coach dữ liệu được giao 37.3; test/fixture/FE version/bằng chứng theo 38. Chưa code/FE verified |
| Báo cáo tiền | BE trả đủ tiền đã thu/đã hoàn/thu ròng; FE quyết định bố trí/chỉ số chính, BE không ép ưu tiên thu ròng. Chi tiết 39.3 |

Giữ Clock Asia/Ho_Chi_Minh, Invoice.amount bất biến, Paid mới kích hoạt, locking/transaction, toggle chế tài khóa đặt lịch tắt nhưng không xóa code. Bảng trên là quyết định mục tiêu, chưa phải implementation đã chạy.

### 0.3. Điểm tiếp tục, bằng chứng và phần còn mở

**Phân công đề xuất 27/10 tại mục 40:** T ~15% (D01 + D09, file ownership riêng), Phase 1 ~40% (catalog/đăng ký/mua–thanh toán/quyền), Phase 2 ~45% (dùng quyền/lịch/sự cố/attendance/đối soát/report). Có prompt gửi đồng đội 40.7 và chuẩn evidence 40.6. Owner đã yêu cầu review/commit/push baseline lên tai; chưa bắt đầu coding các feature trong phân công hoặc deploy, không thay business rule.

**Làm rõ mới nhất của Owner:** đồng đội làm SMTP cho OTP đăng ký email, chatbox cơ bản và thông báo; gói D01/D09 vừa đề xuất chưa làm. Lượt hiện tại xử lý merge nhánh đồng đội `3309b02` vào tai từ `fc00c64`, không triển khai hai phase. Session sau đọc 40.12 và git status/log trước tiếp tục; không suy OTP/chat/notification đã có nghĩa D01/D09 hoặc P1/P2 hoàn tất.

**Bàn giao mới nhất:** baseline đã push `9097197`, cập nhật chẩn đoán `7c29607`; clean test 223/204 pass/19 skip/0 lỗi, PG local riêng 14 tests pass (docs 02 mục 9.10). Owner chọn DB_PASSWORD hiện tại, đã chuẩn hóa DB_URL local ignored và kết nối Supabase chỉ đọc/rollback thành công; public có 0 bảng, chưa có schema SCMS. Chưa migration/seed/boot Spring/jobs/API/FE trên DB mới; bằng chứng docs 02 mục 9.13. Không hỏi lại nguồn password hoặc yêu cầu gửi secret.

**Cập nhật 27/10:** đã đi qua D09–D19 và chốt phần báo cáo tiền tại 39.3; các ghi chú “còn D10–D19/chờ chọn” ở checkpoint/đoạn phân tích cũ là lịch sử, đối chiếu quyết định cuối trong bảng 0.2. Vẫn đang plan, chưa sửa BE/DB/deploy. D09 còn cần tái hiện/sửa/test theo 20.6; chưa đánh dấu mọi chi tiết triển khai/nghiệm thu hoàn tất. Khi chuẩn bị code cần gom backlog theo phụ thuộc và đồng bộ tài liệu/policy cũ với quyết định mới, không hỏi lại các lựa chọn đã chốt.

- **D09 là điểm tiếp tục:** Render log xác nhận ba lỗi LazyInitializationException Specialization#5/no Session ngày 24/10 UTC 08:51:20, 08:54:15, 08:54:16. Nguyên văn/mức chắc chắn tại 20.5; không yêu cầu gửi lại log/Caused by. Source CoachController đọc specialization LAZY ngoài transaction, OSIV=false; còn gắn URI/frame hoặc tái hiện khi sửa. Hibernate Session khác bảng sessions, không suy proxy ID bị thiếu trong DB.
- BE chúng ta cần service read-only transaction + fetch DTO rõ dữ liệu/center, tránh N+1, giữ contact theo quyền; tái hiện OSIV=false, clean test, deploy/probe đúng SHA khi được yêu cầu triển khai. Không giao lại việc này cho một BE khác hoặc bắt Owner chọn chi tiết fetch/transaction.
- Còn D10 thanh toán/Invoice 3; D11 staging/SHA/DB/quyền mutation; D12 lễ/nắng; D13 báo cáo; D14 Holiday; D15 attendance/check-in/sự cố sau buổi đã chốt; D16 Cash; D17 QR; D18 quyền dashboard; D19 tests. Không coi mọi chi tiết đã chốt từ chữ OK cũ; xem bảng mục 4/câu hỏi mục 7.
- F-06/F-07 là đợt trước: migration 07 trong repo, Owner báo đã chạy Supabase; lần kiểm DB .env đủ cột/FK/index, orphan=0, probe cũ không tái hiện 500 Session. Chưa xác nhận DB .env trùng Render/SHA deploy. Lỗi Coach là lỗi khác.

### 0.4. Trạng thái workspace và quy tắc tiếp nối

Checkpoint ban đầu là tai/8e95759 với nhiều thay đổi/untracked; Owner sau đó đã yêu cầu review/commit/push. Baseline đã được commit/push tới 9097197, cập nhật chẩn đoán tới 7c29607; SHA mới nhất phải lấy từ git log/status, không phải SHA Render. Session mới vẫn kiểm git status/diff, không reset/checkout mất thay đổi hoặc khôi phục tài liệu Owner đã chuyển old-docs. Schema/migration 07/test PostgreSQL thuộc F-06/F-07, docs 04 hiện đã track; chưa triển khai D01/D09/Phase 1/2.

D01–D08 mới sửa docs/đọc source/kiểm kê read-only, chưa áp SQL dọn giá, chưa sửa code nghiệp vụ mới hoặc deploy. Không dùng test xanh cũ làm bằng chứng cho thay đổi chưa code. Sau sửa BE bắt buộc .\mvnw.cmd clean test; kiểm DB/FE/live đúng môi trường được phép. Sandbox không tự tách DB thật. Có thể đọc .env tại workspace khi cần, không cần Owner dán lại secret.

### 0.5. Bản đồ docs và bàn giao FE

- **01:** mục 14 là danh sách thay đổi mục tiêu, 14.1–14.2 quy định trạng thái/gói bàn giao FE; khi code xong phải cập nhật ngay mục endpoint với method/path/DTO/quyền/error/ví dụ thật. Không chỉ thêm ghi chú cuối rồi để FE dùng ví dụ cũ.
- **02:** mục 9 là quy trình triển khai/DB/deploy/test của đợt mới; mục 4.3 được đánh dấu smoke implementation cũ, không dùng nguyên trạng nghiệm thu rule mới. Chưa có migration D01–D08 chỉ vì docs có kế hoạch.
- **04:** quyết định/lý do/source/log và ghi chú triển khai BE nội bộ. Các so sánh cũ là lịch sử; quyết định cuối “đã chốt” có hiệu lực hơn câu “chờ chốt” cũ. Endpoint/DTO/error mới chỉ dự kiến không được gọi là khả dụng.

### 0.6. Context dán vào session mới

```text
Workspace D:\Projects\SWP391. Chúng ta là phía BE của SCMS.
Đọc AGENTS.md, docs/00-MASTER-CHECKLIST.md phần active, docs/04-DEBUG-NOTE.md mục 0 trước; sau đó mục 20/20.5, docs/01-API-CONTRACT-FE-BE.md mục 14 và docs/02-DEPLOYMENT-GUIDE.md mục 9.
Kế thừa D01–D08 đã chốt, không hỏi lại. Hiện đang phân tích/chốt và chuẩn bị sửa, chưa tự triển khai tất cả hoặc chạy mutation DB/deploy.
Tiếp tục D09: log đã xác nhận LazyInitializationException Specialization#5/no Session; chính chúng ta xử lý BE, không chờ BE khác. Đọc source và lập bước tái hiện/sửa/kiểm thử; phân biệt lỗi đã có log với URI/frame còn cần kiểm. Không yêu cầu gửi lại log 20.5.
Kiểm git status/diff, giữ thay đổi workspace/old-docs. Phân tích tuần tự phần còn mở, hỏi khi có mâu thuẫn/nghiệp vụ mới; triển khai theo phạm vi tôi yêu cầu. Sau sửa BE chạy .\mvnw.cmd clean test. Cập nhật docs 01 API thật/handoff FE, docs 02 migration/deploy/bằng chứng, docs 04 quyết định, docs 00 tiến độ. API dự kiến không được coi là đã chạy.
Owner đã làm rõ: đồng đội làm SMTP/OTP đăng ký, chatbox cơ bản và thông báo; gói D01/D09 vừa phân chưa làm. Đọc thêm mục 40.12 về xử lý merge và trạng thái kiểm. Hai phase chỉ tiếp tục trong session sau theo phạm vi Owner yêu cầu, không coi việc merge feature của đồng đội là đã triển khai phase.
```

Ngày mở: 25/10/2026. Trạng thái: **D01 đã chốt phương án; D02 đã chốt 9 môn/18 giá, giữ ID 19–27/xóa bản dư và phạm vi đối soát fixture; D03-a đã chốt mô hình mua, Owner đồng ý hướng D03-b và bỏ nhập lịch rảnh, D03-c đã chốt 1080 giờ từ Paid cùng cơ chế thông tin hạn; D04-a chốt A (Coach chính tạo, Manager hỗ trợ, xác nhận trao đổi). Handoff BE tại 11.16/12.4, FE tại API contract mục 14; D05-a đã chốt hủy Advanced >=12 giờ hoàn một credit, D05-b đã chốt A (báo vắng riêng, điểm danh thực tế), D06-a đã chốt A (Manager ghi nhận đồng ý), D06-b đã chốt A ngoại lệ notice khi dời do trung tâm, D07-a đã chốt A kèm UI/UX chung tạo–dời ca, D07-b đã chốt A Manager duyệt bất khả kháng sát giờ, D07-c đã chốt chỉ bỏ dấu khi Dời ca thật sự đổi ngày/giờ, D08-a đã chốt B gia hạn toàn gói theo thời gian ảnh hưởng, D08-b đã chốt bù gián đoạn thực tế và Manager duyệt thêm đủ ca bù, D08-c đã chốt phục hồi credit thu hồi có căn cứ, D08-d đã chốt Chờ khắc phục/duyệt sau, D09 log đã xác nhận lỗi lazy Specialization, cần kiểm gắn endpoint/tái hiện khi sửa; chưa triển khai**.

## 1. Phạm vi, nguồn và cách làm

- Tạo file này theo yêu cầu trực tiếp của Owner, là ngoại lệ được yêu cầu đối với quy tắc ba docs chính. Chưa sửa AGENTS hoặc biến các đề xuất thành API contract.
- Đã đọc toàn bộ `../system-test-rerun-2026-10-24.md`, `../FE-BE-sandbox-e2e-handoff.txt` và đủ 8 trang `../analysis 2410.pdf`; kiểm tra cả hình trang, không chỉ text extraction.
- Đã đối chiếu quyết định đang chờ trong `00-MASTER-CHECKLIST.md`, các mục liên quan trong `01-API-CONTRACT-FE-BE.md`, AGENTS và code hồ sơ Member. Bằng chứng cũ F-06/F-07 được giữ theo HISTORY; không đọc lại tài liệu đã chuyển vào old-docs trong lượt này.
- Owner báo đã chuyển Checklist 2310, PDF quyền 18/10, PDF 20 vấn đề, user-table-fix và be-request-f06-f07 vào old-docs. Giữ nguyên việc di chuyển; không khôi phục hoặc sửa các tài liệu lưu trữ.
- Các link evidence/script trong báo cáo FE là tham chiếu của workspace FE. Chưa có bằng chứng các file đó tồn tại trong checkout này; không tuyên bố đã kiểm lại evidence gốc.
- Không sửa backend/frontend, không gọi mutation hoặc chạy migration/cleanup DB, không push/deploy trong giai đoạn này. Không chạy lại test nghiệp vụ chỉ để đánh giá tài liệu. Các kết quả FE ngày 24/10 bên dưới là **báo cáo FE**, không phải kiểm thử mới của lượt 25/10.

### Quy tắc phân tích

1. Một vấn đề có một mã Dxx; các nguồn trùng nội dung trỏ về cùng mã, không tạo nhiều quyết định độc lập cho cùng hành vi.
2. Tách **Owner đã xác nhận**, **đề xuất trong PDF**, **code hiện tại**, **bằng chứng live có ngày giờ**, **chưa rõ**. Chữ “OK” không tự giải quyết những lựa chọn vẫn chưa được chọn.
3. Với lịch/credit, xét theo: nguyên nhân, người yêu cầu, người có quyền thực hiện, thời hạn, đồng ý của Member, thay đổi tài nguyên, thay đổi credit, audit và hoàn tác khi lỗi.
4. Với tiền/dữ liệu trùng: kiểm kê trước, chọn bản ghi chuẩn và kế hoạch chuyển liên kết sau; giữ snapshot hóa đơn và lịch sử. Chưa chọn giá thay Owner; chưa xóa bản ghi có liên kết.
5. Với phân quyền: danh tính lấy từ Authentication phía BE; DTO tự phục vụ chỉ có trường được phép sửa. Role/ID trong response đăng nhập không phải bằng chứng client được phép cập nhật các trường đó.
6. Sau mỗi mục: ghi quyết định, câu hỏi còn lại, tiêu chí nghiệm thu. Gặp thiếu dữ kiện/mâu thuẫn/đề xuất cần lựa chọn thì dừng tại mục đó để hỏi; không tự chốt hoặc chuyển sang triển khai.

## 2. Quyết định Owner đã nêu trong lượt 25/10

| Mã quyết định | Nội dung được xác nhận | Phạm vi chưa được tự suy rộng |
|---|---|---|
| O01 | Đã chốt GET + PATCH /api/members/me, Member xem và tự sửa tên, điện thoại, địa chỉ, ngày sinh, giới tính, mục tiêu, ghi chú sức khỏe | BE xác định chủ hồ sơ từ Authentication; DTO riêng; email/mật khẩu dùng luồng riêng; không mở trường quản trị hoặc quyền sửa Member khác. Chưa triển khai |
| O02 | Đã chốt chỉ 9 môn và đủ 18 giá PDF/04; giữ ID 19–27; xóa hẳn 8 bản trùng, không dùng Inactive; không giữ môn ngoài Member.pdf | Phạm vi hiện tại là chốt phương án, chưa chạy SQL. ID 7 Bắn súng thuộc nhóm ngoài danh mục cần loại bỏ; phải kiểm lại liên kết trước xóa, không cascade hoặc đổi lịch sử hóa đơn |
| O03 | I.1–3 và I.5 cần được đánh giá tối ưu, giải thích và đề xuất cải thiện | Chưa coi mọi chi tiết PDF là quy tắc đã thay thế AGENTS |
| O04 | I.4 thực hiện đủ ba phương án: dời lịch, thay Coach, hủy do trung tâm | Cần kết hợp với II; không mặc định mỗi nguyên nhân ở II đều được hưởng cả ba phương án |
| O05 | III.3 đồng ý xem xét nhưng cần đánh giá để chọn hướng chỉ số | Chưa chọn công thức expiredMembers hoặc đổi response |
| O06 | III.4: tự lấy thu ngân từ account đăng nhập, thời điểm, số tiền và lịch sử hóa đơn | Số tiền lấy từ hóa đơn đã chốt, không tin amount client; ghi chú/biên nhận/sửa sai phải bóc tách khi đến mục Cash |
| O07 | III.5: có lịch sử thay đổi Holiday và confirm dời lịch khi phát hiện có lớp | Chưa chốt confirm là duyệt thay đổi Holiday, xác nhận từng lịch mới hay cả hai; không tự dời hàng loạt |
| O08 | III.6: OK với hướng tách check-in cửa và điểm danh; đề xuất trong PDF để check-in cửa sau MVP | Không xóa chức năng hiện có hay gộp hai dữ liệu; chi tiết phạm vi xác nhận khi đến mục D15 |
| O09 | Lưu quyết định/phân tích vào file này, phân tích tuần tự và dừng hỏi khi mơ hồ/mâu thuẫn | Đây là giai đoạn phân tích, chưa phải lệnh sửa toàn bộ code/DB |

## 3. Các quyết định cũ còn mở — chưa bị ghi đè

Nguồn: HISTORY đợt 23/10 trong master checklist. Đây là danh sách cần đối chiếu, không phải xác nhận lại lựa chọn cũ.

| Nhóm | Điểm đang chờ từ trước | Mục xử lý thống nhất |
|---|---|---|
| Advanced | Mua credit rồi đặt từng ca hay giữ đủ 12 ca trước Paid; quyền học theo gói/đăng ký/môn | D03, D04 |
| Hạn gói | Tính từ Paid hay buổi đầu; cuối ngày hay chính xác 45x24 giờ | D03, D08 |
| Thanh toán | Holding 5/15 phút; Return/IPN; đối soát tiền về muộn | D10 |
| Giá | Giá PDF/giá demo, 0đ, quyền override; không đổi amount hóa đơn cũ | D02 |
| Điều kiện mua | Normal có cần gói Active; credit dùng chung hay theo môn | D02, D03 |
| Sự cố | Ai duyệt, consent Coach thay, quyền đã bị expiry xóa, quyền gia hạn | D06–D08 |
| Đặt/hủy/điểm danh | Mốc 12/24 giờ; báo vắng sát giờ; cửa sổ ghi/sửa attendance | D05, D07 |
| Lễ/nắng | Cố định/cấu hình; ảnh hưởng đến lịch đã tạo khi đổi Holiday | D12, D14 |
| Báo cáo/phụ trợ | expiredMembers, Cash note/biên nhận, check-in cửa, CRUD Holiday | D13–D15 |

## 4. Danh mục hợp nhất — chưa phải danh sách đã sửa

| Mã | Vấn đề độc lập | Nguồn gộp | Trạng thái và hướng xử lý tiếp |
|---|---|---|---|
| D01 | Hồ sơ cá nhân Member | Yêu cầu 1 + hình 2 | Owner đã chốt xem + tự sửa bảy trường qua GET/PATCH /me với DTO riêng; chưa triển khai |
| D02 | Danh mục môn, giá 0đ, trùng tên, dữ liệu mâu thuẫn | Yêu cầu 2 + giá đang chờ trong docs | Đã chốt A và giá PDF/04; kiểm kê 18 Subject, 15 Class, 6 package; đã chốt giữ 19–27/xóa bản dư; Enrollment 2/Class 4 đã được phép đối soát/tạo lại; gói 5/hai subscription đã được phép đối soát/tạo lại theo môn; chuyển sang mô hình mua Advanced D03/I.1 |
| D03 | Đăng ký Advanced theo môn, Primary Coach và hạn 45 ngày | I.1 + quyết định Advanced/gói/hạn cũ | Chốt mô hình mua theo môn/Coach, đồng ý hướng xét năng lực trước thu tiền và bỏ khai lịch rảnh; chốt 1080 giờ từ Paid và hiển thị/kiểm hạn. Chưa triển khai; handoff 11.16 |
| D04 | Người tạo lịch Advanced, consent và credit lúc đặt | I.2 + phần chống race/credit trong docs | Chốt A: Coach chính tạo, Manager hỗ trợ có lý do/audit, xác nhận đã trao đổi và thông báo Member; không có vòng Member duyệt ca. Đặc tả/handoff 12.4, chưa triển khai |
| D05 | Member hủy/dời/báo vắng/no-show | I.3 + II.2–3 + quy tắc credit | D05-a đã chốt hủy Advanced >=12 giờ hoàn một credit; chưa triển khai. D05-b đã chốt A báo vắng riêng/điểm danh thực tế ở 14.3; dời tách D07 |
| D06 | Sự cố phía Coach/trung tâm: ba cách xử lý | I.4 + II.1 | Owner đã chốt đủ ba cách; đã rà source, D06-a chốt Manager ghi nhận đồng ý ở 15.4, D06-b chốt ngoại lệ notice tại 15.6; không hỏi lại có làm đủ ba không |
| D07 | Dời lịch thường và dời sát giờ ngoại lệ | II.2–3, dùng chung validation D04/D05 | D06 đã tách lỗi trung tâm; D07-a chốt Coach dời/Manager hỗ trợ, hai mốc >=12 giờ và UI chung ở 16.3–16.4; D07-b chốt A ngoại lệ bất khả kháng ở 17.3, D07-c đã chốt tại 18.4 |
| D08 | Bù quyền học và gia hạn sau sự cố | I.5 + II.1 bước 4 + docs gia hạn | D08-a chốt B gia hạn toàn gói; D08-b chốt bù gián đoạn thực tế tại 19.5, D08-c chốt phục hồi có đối soát tại 19.7, D08-d chốt chờ khắc phục/duyệt sau tại 19.9 |
| D09 | GET /api/coaches lỗi 500 | Báo cáo FE P1 | Log đã xác nhận LazyInitializationException Specialization#5 tại 20.5; source phù hợp, còn kiểm URI/frame/tái hiện khi triển khai, chưa fix |
| D10 | Invoice Pending không payable, VNPay, holding/callback | Báo cáo FE invoice 3 + handoff mục 1–3 | Tách lỗi fixture/điều kiện BE, nút Pay FE và cấu hình Sandbox; chưa sửa Paid/Expired để ép test qua |
| D11 | Đồng bộ source/schema/deploy/DB và môi trường E2E | Báo cáo FE source/deploy + handoff mục 4–5 + F-06/F-07 | Cần SHA/DB đích; sandbox không cô lập dữ liệu; thống nhất môi trường và quyền mutation trước E2E |
| D12 | Khóa ca nắng và ngày nghỉ trong lịch mới | III.1–2 + AGENTS lịch/holiday | Ví dụ giờ PDF sai bộ ca hiện hành; cần chốt nguồn chuẩn, không đưa giờ ví dụ vào code |
| D13 | Chỉ số hội viên hết quyền học / số gói hết hạn | III.3 + expiredMembers cũ | Owner yêu cầu đánh giá chọn hướng; cần định nghĩa quyền học Normal/Advanced trước công thức |
| D14 | Holiday CRUD, lịch sử và lịch đang tồn tại | III.5 + O07 | Đã đồng ý lịch sử/confirm; thiết kế luồng tác động và consent lịch mới còn chờ |
| D15 | Điểm danh buổi học và check-in cửa | III.6 + I.3 + O08 | Tách dữ liệu/quyền; không đồng nhất vào cửa với tham gia buổi |
| D16 | Cash: thu ngân, thời gian, amount, lịch sử, sửa sai | III.4 + O06 + Cash cũ | Đã đồng ý lấy thông tin phía server; đối chiếu trường đã có trước khi thêm mới |
| D17 | QR Dashboard Member | Báo cáo FE P2 | FE báo dùng sai expiresInSeconds và QR giả khi API lỗi; cần đối chiếu đúng branch FE |
| D18 | Dashboard Coach/Admin | Báo cáo FE policy + probe cũ | Coach/Admin 200 chưa tự coi là bug; cần chốt authority mong muốn, không đánh đồng role với quyền được cấp |
| D19 | Bộ test FE lỗi thời/fixture/selector | Báo cáo FE test + handoff helper | Phân biệt lỗi sản phẩm và lỗi test; không thay expected chỉ để xanh; tránh build song song chung dist |

### Nội dung báo cáo FE cần giữ nguyên mức bằng chứng

- Probe ngày 24/10: 51 bước, 48 PASS, 1 FAIL, 2 quyền cần chốt; /api/coaches 500 ở Manager/Member. Đây là số liệu FE báo, chưa chạy lại trong lượt phân tích này.
- Sessions và dashboard Manager 200; alias /api/sessions/coaches/{id}/available-slots 200. Không mở lại F-06/F-07 như lỗi 500 đang tái hiện chỉ vì thiếu bằng chứng SHA.
- Invoice ID nội bộ 3, mã hiển thị INV-2026-000046: FE báo Pending, paymentMethod=null, expiresAt=null; Owner chụp POST pay-online 409. Null deadline không chứng minh hết hạn; chưa xác nhận nguồn gốc invoice hoặc logic của SHA deploy.
- FE báo 14 test Node/logic: 12 PASS, g1/g2 FAIL; FE10 browser dùng loopback giả lập; FE2 PaymentModal 11/13; mock matrix dừng sau lỗi/timeout. Không tính test chưa chạy hoặc mock thành E2E live PASS.
- Handoff báo isDirectSubscriptionLocked luôn false và FE2 yêu cầu khóa mutation Render. Đây là policy chưa chốt, không tự sửa helper/test khi chưa quyết định môi trường được phép ghi.
- Sandbox chỉ mô phỏng tiền; request vẫn có thể ghi subscription, invoice, giữ chỗ và credit vào DB đang kết nối. Chưa xác nhận cấu hình Sandbox trên đúng deployment từ biến môi trường Render.

## 5. Nội dung PDF đã gom theo hành vi

Phần này bảo toàn phương án của tài liệu, chưa phê duyệt các chi tiết mới thay cho quy tắc hiện hành.

### 5.1. I.1–2 → D03–D04

- PDF đề xuất primaryCoachId thuộc từng đăng ký Advanced, độc lập theo môn/Coach/credit/hạn; không nằm trên hồ sơ Member hoặc mẫu gói dùng chung.
- Chọn Coach trước thanh toán; Coach Active, đúng chuyên môn, có khả năng nhận học viên. Chưa bố trí Coach thì không thu tiền. Chọn Coach chưa giữ ca hoặc sân.
- PDF đề xuất Paid là Ngày 1, hết cuối ngày thứ 45 tại Asia/Ho_Chi_Minh, nạp 12 credit. Chưa chuyển thành công thức endDate; quy tắc inclusive này cần đối chiếu docs/code đang dùng today + 45.
- Coach chính được xem thông tin liên hệ trong phạm vi Member đồng ý chia sẻ. Coach tạo lịch sau thỏa thuận, tick xác nhận, lưu người/giờ tạo và gửi thông báo Member.
- Validation: Paid/Active/đúng môn/còn credit, ca trong hạn, đặt trước ít nhất 12 giờ, Coach chính hoặc thay hợp lệ, không trùng Coach/Member/sân, phòng phù hợp/hoạt động, holiday và ca nắng.
- Tạo Session, giữ tài nguyên và trừ 1 credit trong cùng transaction có khóa DB; request lặp không nhân đôi. Điểm danh không trừ credit lần hai.

### 5.2. I.3 + II.2–3 → D05/D07/D15

Bảng trong hình trang 2 không nằm trong text extraction; đã đọc trực quan và ghi lại:

| Tình huống PDF | Phương án PDF |
|---|---|
| Member hủy trước ít nhất 12 giờ | Hủy buổi, giải phóng sân/Coach, hoàn đúng 1 credit vào gói |
| Member dời trước ít nhất 12 giờ | Kiểm ca mới; dời thành công, giữ nguyên số dư credit; ca mới trong hạn gói |
| Member muốn dời khi dưới 12 giờ | Chặn tự dời; chỉ Manager duyệt ngoại lệ |
| Báo vắng dưới 12 giờ | Không hoàn credit; hệ thống ghi nhận đã báo vắng |
| No-show | Coach ghi Absent trong cửa sổ điểm danh; không hoàn credit |

- Phần chữ trang 2 bổ sung: Member bấm báo vắng thì hệ thống ghi Absent và không được sửa lại; Present/Absent thông thường do Coach quyết định. Tách quyền credit khỏi chính sách lương Coach.
- II.2 chưa chọn duy nhất người bấm: ghi Coach chính, hoặc Member trực tiếp, hoặc Manager hỗ trợ. Đây là lựa chọn còn mở, không phải cả ba đã được cấp quyền đồng thời.
- II.3 đề xuất Manager duyệt bất khả kháng của Member; chưa quy định đầy đủ hạn mới/credit/attendance nếu phê duyệt sau buổi học hoặc sau expiry.
- Hình bảng trang 5 bị cắt phía phải ngay trong PDF. Không suy đoán phần mất; phần chữ II.1–3 dùng làm nguồn đối chiếu. Nếu phần bị cắt có ngoại lệ bổ sung thì cần bản đầy đủ.

### 5.3. I.4 + II.1 + I.5 → D06/D08

**Hai trục khác nhau cần kết hợp, không gộp thành một danh sách ba mục:**

- I.4 là **ba kết quả xử lý sự cố phía trung tâm**: dời, thay Coach cho riêng buổi, hoặc hủy/hoàn 1 credit nếu Member không đồng ý hai cách kia. Owner đã yêu cầu có đủ cả ba.
- II là **ba nguyên nhân/điều kiện xin dời**: trung tâm/Coach; Member báo sớm; Member báo sát giờ. Không tự áp dụng quyền hủy/hoàn bất kể cutoff của trung tâm sang lỗi Member.
- Với lỗi trung tâm: Coach báo cáo/trao đổi; Manager xác nhận cách xử lý, Member đồng ý; lưu lý do, actor, lịch cũ/mới. Coach thay không tự thành Primary Coach mới.
- I.5 đề xuất không tự kéo dài toàn gói khi dời thường; Manager duyệt hạn mới đủ học bù, lưu hạn cũ/mới/lý do/buổi gây phát sinh.
- Credit hoàn khi gói hết hạn phải có quyền sử dụng thực tế trong cùng thao tác; không hoàn một số dư không dùng được. Cách bảo toàn credit đã expiry và mô hình quyền buổi bù chưa được chọn.
- Chỗ chưa thống nhất nội bộ PDF: muốn tránh các credit khác được hưởng hạn kéo dài nhưng lại đề xuất Manager kéo hạn gói. Cần đánh giá gia hạn cả gói hay quyền bù riêng, không coi hai cách tương đương.

### 5.4. III.1–6 → D12–D16

- III.1 nghiêng về ca nắng cố định cho MVP; giờ minh họa ca 3 11:00–12:00, ca 4 12:00–13:00 không khớp AGENTS. Hiện chuẩn là ca 3 09:30–11:00, ca 4 13:00–14:30; chưa tự thay chuẩn từ ví dụ.
- III.2 đề xuất Normal bỏ ngày nghỉ để đủ 12 buổi; Advanced không tạo ca, không giữ sân/trừ credit khi ngày nghỉ đang áp dụng. Cụm “ngày nghỉ ... thì không trừ” chưa rõ là không trừ ngày khỏi hạn 45 ngày hay không trừ credit. CENTER_EVENT và toggle hiện hành cũng cần đối chiếu với câu “chỉ ngày nghỉ chính thức”.
- III.3 tách số gói hết hạn khỏi số Member từng có quyền học nhưng hiện không còn quyền học nào. Chưa chọn hướng. Ví dụ đếm không thay định nghĩa quyền học Normal/Advanced hoặc chính sách tài khoản Locked.
- III.4 Cash note tùy chọn; actor/thời điểm/amount/invoice/history bắt buộc; sai xác nhận thu cần quy trình sửa/đối soát, không chỉ sửa note. O06 đã đồng ý tự lấy thông tin phía server.
- III.5 cho sửa/xóa Holiday có kiểm tra ảnh hưởng như tạo; không tự khôi phục lịch đã dời/hủy. O07 thêm lịch sử và confirm; chưa chọn cơ chế giải quyết các lịch bị ảnh hưởng.
- III.6 phân biệt check-in cửa và attendance Session; PDF đề xuất check-in cửa sau MVP. O08 đồng ý hướng, chưa có lệnh xóa chức năng hiện tại.

## 6. Vấn đề đầu tiên đang phân tích: D01 — hồ sơ cá nhân

### 6.1. Bằng chứng code hiện tại

- `src/main/java/com/sportscenter/user/MemberController.java`: GET /api/members/{id} yêu cầu MANAGE_USERS hoặc REGISTER_MEMBER; PUT cùng route yêu cầu MANAGE_USERS. Member chỉ có ROLE_MEMBER không được quyền truy cập qua hai route này.
- Controller chưa có GET/PATCH hồ sơ /api/members/me; có /me/checkin-code nhưng đó là mã điểm danh. UserController có avatar /me, không phải API đọc/sửa toàn bộ hồ sơ.
- `SecurityConfig.userDetailsService` tạo principal kiểu Spring Security User, username là email; principal không có property memberId. `LoginResponse.memberId` là trường JSON riêng, không thêm property vào principal.
- `MemberRequest` là DTO quản trị, có status, centerId, packageId, email, password và các trường hồ sơ. `MemberService.updateMember` có thể thay status, vì vậy chỉ nới annotation rồi dùng lại DTO này là không đủ an toàn.
- Chưa chạy probe mới cho hồ sơ hoặc test quyền trong lượt này. Đây là xác nhận từ source, không phải khẳng định SHA deploy có cùng code.

### 6.2. Đánh giá hình 2 và đề xuất để Owner chọn

- Không chép biểu thức `authentication.principal.memberId` trong hình: nó không khớp principal hiện tại. Tên role MANAGER trong hình cũng không khớp tên CenterManager mà hệ thống đang dùng.
- Chỉ mở quyền GET/PUT /{id} là thay đổi ít dòng nhưng phải thêm kiểm owner ở BE và DTO riêng; không phải bản sửa “5 phút” đã được kiểm chứng.
- **Đề xuất ưu tiên:** GET /api/members/me và PATCH /api/members/me; BE tìm Member bằng Authentication.getName(), không nhận memberId từ body. Giữ endpoint quản trị và quyền của staff tách biệt.
- DTO tự sửa đề xuất: name/fullName (chốt một tên trong contract), phone, address, dateOfBirth, gender, goal, healthNote. PATCH phải phân biệt không gửi với chủ động xóa giá trị cho các trường cho phép xóa.
- Không cho tự sửa role, status, centerId, packageId, credit, hạn gói, totalSpent, actor đăng ký hoặc giá; email/mật khẩu dùng luồng xác thực riêng. Avatar tiếp tục dùng API hiện có.
- GET /me chỉ trả dữ liệu cần cho chủ hồ sơ; kiểm quyền truy cập thông tin sức khỏe/liên hệ của staff/Coach là vấn đề riêng, không mở qua route tự phục vụ.

### 6.3. D01 đã chốt — 25/10/2026

Owner xác nhận: “chốt phương án xem + tự sửa với bộ trường trên”.

- Chốt GET /api/members/me và PATCH /api/members/me.
- Member tự sửa đúng bảy trường: tên, điện thoại, địa chỉ, ngày sinh, giới tính, mục tiêu, ghi chú sức khỏe.
- BE lấy danh tính từ phiên đăng nhập; dùng DTO tự phục vụ riêng, không dùng DTO quản trị có status/centerId/packageId.
- Email/mật khẩu dùng luồng riêng; avatar dùng API hiện có. Không tự sửa role, trạng thái, center, gói, credit, hạn gói hoặc số tiền.
- Đây là chốt phương án phân tích, **chưa triển khai code**. Tên field JSON và quy tắc validation/null được đặc tả kỹ thuật trước khi triển khai, không làm thay đổi phạm vi bảy trường đã chọn.
- Chuyển sang D02; không hỏi lại quyền xem/tự sửa đã được xác nhận.

### 6.4. Tiêu chí kiểm chứng khi triển khai phương án đã chốt

- Member đọc/sửa đúng hồ sơ mình, không sửa được người khác bằng đổi URL/body; anonymous 401.
- Gửi kèm field quản trị không làm thay dữ liệu; phân biệt trường bỏ qua và trường được xóa; validation ngày sinh/phone/độ dài theo contract đã chốt.
- Staff giữ quyền quản trị được cấp; không cấp MANAGE_USERS cho Member để giải quyết lỗi UI.
- Màn FE đọc lại sau lưu; lỗi API không thay bằng hồ sơ mock. Chạy .\mvnw.cmd clean test sau khi sửa backend và test tích hợp owner/DTO thực tế khi đến giai đoạn triển khai.

## 7. Điểm cần hỏi ở các mục tiếp theo — chưa tự chọn

1. D02: A đã chốt (giá riêng cho từng loại Normal/Advanced). Đã chốt giá PDF/04 và kiểm kê trùng/0đ; đã chọn xóa bản dư/giữ 19–27; còn lớp giá cũ và gói chưa gắn môn. Không merge chỉ dựa vào tên, không thay amount hóa đơn cũ.
2. D03: tiêu chí Coach “còn khả năng nhận” khi chưa giữ ca là gì? Chọn Coach có đủ bảo đảm sử dụng gói trước khi hạn từ Paid chạy không? Có thay đổi quyền Member tự book không?
3. D05: có chính thức thay cutoff hủy 24 giờ bằng 12 giờ, và phạm vi báo vắng bất biến là gì? Chưa sửa AGENTS, contract hoặc test theo PDF.
4. D06–D08: consent được ghi trực tiếp bởi Member hay Manager xác nhận trao đổi? Quyền bù riêng hay gia hạn toàn gói? Xử lý ca đã chốt attendance, gói đã expiry và credit đã bị hủy thế nào?
5. D12: giữ bộ 7 ca chuẩn hiện hành hay thay lịch hoạt động? Quy tắc Holiday gồm cả CENTER_EVENT hay chỉ ngày lễ chính thức? Ngày nghỉ có làm tăng hạn sử dụng không?
6. D13: chỉ số chăm sóc hội viên hay chỉ số gói hết hạn; thế nào là quyền học còn hiệu lực khi có cả Normal/Advanced, gói 0 credit nhưng còn lịch đã đặt?
7. D14/D16: confirm Holiday có bao gồm commit lịch mới không? Ai được sửa sai thu Cash, theo cơ chế đối soát nào? Thông tin hệ thống đã có cần dùng lại trước khi thêm trường.
8. D10/D11/D18/D19: policy Sandbox và mutation được Owner cho phép ở môi trường nào; tài khoản/fixture/reset; quyền Coach/Admin; branch FE/SHA BE/deploy và bằng chứng gốc. Không mở mutation hay thay expected test từ giả định.

## 8. Kiểm tra trước bàn giao giai đoạn tiếp nhận

- Đã đọc đủ ba nguồn được yêu cầu và quan sát đủ 8 trang PDF, gồm bảng ảnh trang 2 và bảng bị cắt trang 5.
- Mỗi mục I.1–5, II.1–3, III.1–6 và hai vấn đề bổ sung đều có mã trong danh mục; các báo cáo FE/handoff được gom vào D09–D11, D17–D19.
- Các chữ OK của Owner được lưu đúng mức độ, không tự biến mục còn xin đánh giá thành quyết định cuối.
- Chỉ D01 đã đối chiếu code chi tiết; các mục khác mới được tiếp nhận/loại trùng và ghi câu hỏi, không tuyên bố đã kiểm toàn bộ sản phẩm/DB/live.
- Chỉ tạo/cập nhật tài liệu; không chạy tests hoặc mutation nghiệp vụ. D01 đã chốt; D02 đã chốt A và giá, đã kiểm kê read-only; đã chốt xử lý bản dư tại 10.7; D02-c đã chốt đối soát/tạo lại Enrollment 2; D02-d cũng đã xác nhận fixture gói 5; chuyển câu hỏi đang chờ sang D03/I.1 tại 11.4, chưa xử lý DB.


## 9. D02 — A và bảng giá PDF/04 đã được Owner chốt

### 9.1. Phần đã rõ và phần chưa được chọn

- Owner yêu cầu thống nhất học phí, xác định bản ghi trùng tên có giá 0đ và các mâu thuẫn khác. Đây chưa phải lệnh xóa tất cả bản ghi 0đ.
- Source `Subject.java` hiện có hai trường normalPrice và advancedPrice. `MembershipPackage.java` còn có price/packageType, chưa có liên kết subjectId. Những nguồn giá này phải được đối chiếu, không gộp tất cả bản ghi có cùng tên thành một thực thể.
- Owner đã chọn A: mỗi môn có một giá Normal và một giá Advanced riêng. Owner đã đồng ý đủ hai cột PDF/04; không hỏi lại lựa chọn giá chung hoặc số tiền đã chốt.
- Ban đầu chưa kiểm kê DB. Sau khi Owner chốt giá, kết quả read-only được bổ sung tại mục 10; chưa merge/deactivate hoặc thay giá DB.

### 9.2. Lựa chọn đã được Owner quyết định

| Lựa chọn | Ý nghĩa | Ảnh hưởng |
|---|---|---|
| A — Owner đã chốt | Mỗi môn có một bản ghi danh mục chuẩn; một giá Normal và một giá Advanced được phê duyệt | Vẫn phân biệt hai sản phẩm; loại bỏ giá trùng/mâu thuẫn trong cùng môn và cùng loại, không yêu cầu hai loại cùng mức giá |
| B — không chọn | Mỗi môn dùng đúng một mức giá chung cho cả Normal và Advanced | Là thay đổi chính sách bán hàng và nguồn giá; phải xác nhận rõ trước khi chỉnh mô hình/dữ liệu |

Owner đã chốt A và sau đó đồng ý nguyên bảng PDF/04. Phạm vi override và miễn phí vẫn là lựa chọn riêng, không dùng để tự thay số tiền đã chốt.

### 9.3. Trình tự tiếp theo sau khi có câu trả lời

1. Kiểm kê chỉ đọc theo từng nguồn Subject/Class/MembershipPackage: ID, tên/slug, loại, trạng thái, giá hiện tại và số liên kết; phân biệt trùng thật, tên gần giống, hai loại sản phẩm khác nhau và bản ghi lịch sử.
2. Phân loại giá 0: chưa cấu hình/seed lỗi hay chủ đích miễn phí; đối chiếu bản ghi liên quan trước khi chọn xử lý. Không coi tên giống nhau là đủ để merge.
3. Trình bảng ID cụ thể và các giá mâu thuẫn để chọn danh mục/giá chuẩn; ghi rõ trường hợp thiếu liên kết môn, không suy môn từ tên gói.
4. Sau khi chốt mới thiết kế migration chuyển liên kết hoặc ngừng bán bản ghi dư; không sửa Invoice.amount cũ, không đổi quyền học/số dư/lịch sử vì dọn danh mục. Giá khóa/hóa đơn lịch sử không cần bị ép bằng giá niêm yết hiện tại.

### 9.4. Quyết định A — 25/10/2026

Owner trả lời “A”, đồng thời yêu cầu liệt kê bảng giá để kiểm thiếu sót/mâu thuẫn. Ghi nhận giá chuẩn theo cặp (môn, loại Normal/Advanced), chưa phê duyệt các số tiền trong hai bảng đang khác nhau.

### 9.5. Bảng giá đối chiếu — đơn vị VNĐ cho trọn khóa/gói 12 buổi hoặc 12 credit

Đã đọc trực tiếp và xem hình Member.pdf trang 7–8; đối chiếu với migration 04. Hai cột PDF/04 đã được Owner chốt làm giá niêm yết. Hai cột demo giữ để đối chiếu lịch sử; đã đồng bộ bảng tài liệu API contract, DB chưa đổi.

| Môn | Normal đã chốt (PDF/04) | Advanced đã chốt (PDF/04) | Normal demo (contract/schema/05) | Advanced demo (contract/schema/05) |
|---|---:|---:|---:|---:|
| Quần vợt (Tennis) | 2.500.000 | 8.000.000 | 1.500.000 | 10.000.000 |
| Bơi lội | 1.500.000 | 4.500.000 | 1.200.000 | 9.000.000 |
| Pickleball | 1.800.000 | 5.000.000 | 1.100.000 | 8.500.000 |
| Bắn cung | 2.000.000 | 6.000.000 | 1.400.000 | 9.500.000 |
| Bóng rổ | 1.200.000 | 3.500.000 | 1.000.000 | 8.000.000 |
| Bóng đá | 1.000.000 | 3.500.000 | 900.000 | 8.000.000 |
| Bóng chuyền | 1.000.000 | 3.500.000 | 900.000 | 7.000.000 |
| Cầu lông | 900.000 | 3.000.000 | 900.000 | 7.500.000 |
| Bóng bàn | 800.000 | 2.500.000 | 800.000 | 6.500.000 |

### 9.6. Kết quả kiểm tra thiếu sót và mâu thuẫn

1. **Đủ 9 môn, đủ 18 mức giá** trong cả hai nguồn. Không thiếu môn nào khi đối chiếu hai bảng này. Không có giá 0đ trong hai block seed 9 môn được đối chiếu; chưa suy ra DB không có bản ghi trùng/0đ.
2. **16/18 mức giá khác nhau**: chỉ Normal Cầu lông 900.000 và Normal Bóng bàn 800.000 trùng; toàn bộ 9 giá Advanced khác nhau.
3. Member.pdf trang 7 khớp migration 04; trang 8 cũng dùng Tennis 2.500.000/8.000.000 và Pickleball Normal 1.800.000 làm ví dụ. Bảng đề xuất PDF/04 có căn cứ tài liệu nội bộ, không phải đánh giá giá thị trường.
4. Ở lần đối chiếu trước, API contract mục 2.1 còn dùng bảng demo chưa chốt; sau xác nhận của Owner, bảng niêm yết trong contract đã được cập nhật tài liệu. Hai schema PostgreSQL và script reset demo 05 dùng cùng giá demo. Các script có ON CONFLICT UPDATE giá, nên có thể ghi đè bảng đã chọn nếu dùng sai luồng. Không chạy schema/reset/04 để ép DB đang dùng về bảng giá.
5. Giá còn có nhiều nơi lưu: Subject.normalPrice/advancedPrice, Class.tuitionFee và MembershipPackage.price. InvoiceService tạo hóa đơn gói từ MembershipPackage.price; package chưa có subjectId. Chỉ đổi bảng Subject chưa chứng minh số tiền mua gói Advanced đã đồng bộ. Cần kiểm kê/liên kết khi chuẩn hóa, không suy môn từ tên gói.
6. Giá lớp/hóa đơn lịch sử có thể khác giá niêm yết mới hợp lệ. Không coi mọi khác biệt lịch sử là bản ghi cần sửa; Invoice.amount đã chốt phải được giữ nguyên.
7. Các bảng này không quyết định chính sách miễn phí, giảm giá hoặc quyền override. Bản ghi 0đ cần được phân loại từ DB và nguồn tạo trước khi chọn xử lý, chưa xóa theo giá bằng 0.

### 9.7. Quyết định số tiền — 25/10/2026

Owner đồng ý nguyên hai cột PDF/04 và yêu cầu kiểm kê dữ liệu, lưu ý tránh sinh trùng môn, ghi đè giá và 0đ. Đã chốt đủ 18 mức giá; chỉ được tiếp tục kiểm kê và phân tích, chưa chạy cleanup hoặc tự sửa snapshot lịch sử.

## 10. D02 — kiểm kê DB và phòng ngừa tái phát

### 10.1. Phạm vi và bằng chứng

- Snapshot PostgreSQL trong .env: **2026-10-25 13:34:27.372694 Asia/Ho_Chi_Minh, ngày 25/10/2026**. Transaction **READ ONLY, REPEATABLE READ**, kết thúc rollback. Chưa xác nhận DB này trùng DB Render qua cấu hình quản trị.
- Đọc đủ Subject/Class/MembershipPackage, aggregate liên kết và pg_constraint/pg_indexes. Không xuất tên/liên hệ Member, credential, host hoặc cookie.
- FK trực tiếp tới Subject hiện thấy là classes.subject_id và specializations.subject_id; đếm tiếp coach links/Session/Enrollment/Invoice qua các quan hệ này. Không tuyên bố đã quét mọi tham chiếu dạng text không có FK.
- Mọi số lượng/ID là snapshot; phải kiểm lại dưới lock trước migration tương lai, không hard-code cùng ID cho DB khác.

### 10.2. Subject — 18 bản ghi, 9 bản giá 0đ

Toàn bộ 18 Subject đang Active. ID 1–9 đều có normal_price=advanced_price=0, slug=NULL, outdoor=NULL; **0 Class, 0 Specialization, 0 coach link, 0 Session, 0 Enrollment, 0 Invoice theo lớp** qua các FK đã kiểm.

Nhóm 19–27 có đủ slug/phân loại và giữ liên kết: 15 Class, 9 Specialization, 19 coach link, 2 Session, 2 Enrollment, 1 Invoice theo lớp. Owner đã chọn giữ nhóm này làm bản chuẩn; chưa thay đổi dữ liệu.

| Môn | ID đã chốt giữ | ID 0đ trùng/đồng nghĩa đã chốt xóa | Normal DB → đã chốt | Advanced DB → đã chốt | Lớp/buổi/đăng ký/hóa đơn theo lớp |
|---|---:|---:|---:|---:|---|
| Quần vợt (Tennis) | 19 | 9 | 1.500.000 → 2.500.000 | 10.000.000 → 8.000.000 | 2/0/0/0 |
| Bơi lội | 20 | — | 1.200.000 → 1.500.000 | 9.000.000 → 4.500.000 | 2/0/0/0 |
| Pickleball | 21 | 2 | 1.100.000 → 1.800.000 | 8.500.000 → 5.000.000 | 2/0/1/0 |
| Bắn cung | 22 | 4 | 1.400.000 → 2.000.000 | 9.500.000 → 6.000.000 | 1/0/0/0 |
| Bóng rổ | 23 | 5 | 1.000.000 → 1.200.000 | 8.000.000 → 3.500.000 | 1/0/0/0 |
| Bóng đá | 24 | 8 | 900.000 → 1.000.000 | 8.000.000 → 3.500.000 | 2/0/0/0 |
| Bóng chuyền | 25 | 6 | 900.000 → 1.000.000 | 7.000.000 → 3.500.000 | 1/0/0/0 |
| Cầu lông | 26 | 1 | 900.000 → 900.000 | 7.500.000 → 3.000.000 | 2/2/1/1 |
| Bóng bàn | 27 | 3 | 800.000 → 800.000 | 6.500.000 → 2.500.000 | 2/0/0/0 |

- 7 cặp trùng đúng tên; Quần vợt ID 9 và Quần vợt (Tennis) ID 19 là cặp đồng nghĩa đề xuất, không phải chuỗi giống hệt.
- **ID 7: Bắn súng** là môn riêng ngoài 9 môn đã chốt; không phải Bắn cung. Active, giá 0đ, thiếu slug/outdoor, không có liên kết đã kiểm. Không tự map sang ID 22 hoặc thêm giá cho môn này.
- Nhóm 19–27 vẫn dùng bảng demo: **16/18 giá khác bảng vừa chốt**. Chưa sửa giá trong lượt kiểm kê.

### 10.3. Class — 15 bản, không có giá 0đ

| Class ID | Subject ID | Loại | Giá snapshot lớp | Giá đã chốt | Buổi/đăng ký/hóa đơn |
|---:|---:|---|---:|---:|---|
| 1 | 26 | Normal | 900.000 | 900.000 | 2/1/1 |
| 2 | 19 | Normal | 1.500.000 | 2.500.000 | 0/0/0 |
| 3 | 20 | Normal | 1.200.000 | 1.500.000 | 0/0/0 |
| 4 | 21 | Normal | 1.100.000 | 1.800.000 | 0/1/0 |
| 5 | 24 | Normal | 900.000 | 1.000.000 | 0/0/0 |
| 6 | 27 | Normal | 800.000 | 800.000 | 0/0/0 |
| 7 | 24 | Advanced | 8.000.000 | 3.500.000 | 0/0/0 |
| 8 | 19 | Advanced | 10.000.000 | 8.000.000 | 0/0/0 |
| 9 | 20 | Advanced | 9.000.000 | 4.500.000 | 0/0/0 |
| 10 | 22 | Advanced | 9.500.000 | 6.000.000 | 0/0/0 |
| 11 | 26 | Advanced | 7.500.000 | 3.000.000 | 0/0/0 |
| 12 | 27 | Advanced | 6.500.000 | 2.500.000 | 0/0/0 |
| 13 | 23 | Advanced | 8.000.000 | 3.500.000 | 0/0/0 |
| 14 | 25 | Advanced | 7.000.000 | 3.500.000 | 0/0/0 |
| 15 | 21 | Advanced | 8.500.000 | 5.000.000 | 0/0/0 |

- **13/15 giá snapshot lớp khác niêm yết mới**; Class 1 và 6 khớp. Chênh giá lịch sử không tự động là lỗi cần ghi đè.
- Class 1 có 2 buổi, 1 đăng ký, Invoice 4 Paid 900.000. Class 4 có 1 đăng ký, giá 1.100.000, chưa thấy Invoice trực tiếp theo lớp; chưa suy nguyên nhân từ aggregate. Giữ lịch sử/quyền đã có.
- Lớp Open/Ongoing giá cũ phải được chọn cách xử lý trước khi tiếp tục mở bán: không chỉ đổi Subject rồi mặc định giá lớp đã đồng bộ. Giữ giá cho đăng ký cũ, ngừng nhận mới hoặc tạo phiên bản lớp mới là lựa chọn riêng; chưa reprice hàng loạt.
- Nhiều Class Normal đang mở chưa đủ 12 Session; ghi nhận liên quan D04/D11, chưa chuyển sang sửa lịch trong D02.

### 10.4. MembershipPackage — 6 mẫu gói, không có giá 0đ

| Package ID | Tên | Loại | Giá | Subscription (Active) | Hóa đơn |
|---:|---|---|---:|---|---:|
| 1 | Gói Cơ Bản 1 Tháng | Normal | 500.000 | 0 (0) | 1 |
| 2 | Gói Tiêu Chuẩn 3 Tháng | Normal | 1.350.000 | 1 (1) | 1 |
| 3 | Gói Cao Cấp 6 Tháng | Normal | 2.500.000 | 0 (0) | 0 |
| 4 | Gói VIP 12 Tháng | Normal | 4.800.000 | 0 (0) | 0 |
| 5 | Gói Khóa Học 1:1 (12 Buổi Linh Hoạt) | Advanced | 3.500.000 | 2 (1) | 1 |
| 9 | DEMO CASH 30 ngày | Normal | 300.000 | 2 (0) | 2 |

- Không có cột subject_id trong membership_packages. **Package 5** Advanced chung 3.500.000 có 2 subscription (1 Active), Invoice 1 Paid 3.500.000. Không đủ căn cứ chọn môn hoặc áp cùng giá mới cho quyền học cũ.
- Gói Normal ID 1–4 và DEMO ID 9 khác kỳ hạn/tham chiếu; không coi chúng là môn trùng hoặc tự quy giá thành khóa Normal 12 buổi. Phạm vi gói thành viên còn chờ D03.
- Có 6 Invoice: Paid ID 1/2/4, Pending ID 3, Expired ID 5/6. Giữ nguyên mọi amount nếu chưa chọn quy trình hủy/tạo lại; không điều tra lại invoice 3 ngoài phạm vi D10.

### 10.5. Nguyên nhân/rủi ro và phòng ngừa — chưa triển khai

| Rủi ro | Bằng chứng hiện tại | Đề xuất phòng ngừa |
|---|---|---|
| Trùng legacy/alias | DB chỉ UNIQUE(slug), cho NULL; không unique tên chuẩn hóa; service không kiểm alias | Mã môn ổn định không NULL và unique ở DB để chống tạo đồng thời. Danh sách alias rõ ràng; chuẩn hóa chuỗi chỉ giúp cảnh báo, không tự merge mọi tên gần giống |
| Slug đổi hoặc khác seed | Client có thể gửi slug khác; slugify chưa map đ/Đ sang d | Chuẩn hóa Unicode/case/khoảng trắng/đ; giữ mã ổn định khi đổi tên, trả conflict rõ. Không đổi slug làm thay danh tính môn |
| Môn Active thiếu giá thành 0 | Entity/DB mặc định 0; DTO giá nullable, DecimalMin(0); create mặc định Active | Với 9 môn trả phí đã chốt: chặn mở bán khi thiếu giá hoặc giá không dương; phân biệt chưa cấu hình và miễn phí, không biến null thành miễn phí |
| Client đưa giá lớp thay bảng giá thiếu | SportsClassService fallback request.tuitionFee rồi 0 | BE lấy giá niêm yết hợp lệ; không mở lớp thiếu giá, không fallback số tiền tùy ý client |
| Seed tạo bản mới cạnh dòng NULL slug | Upsert theo slug không nhận diện alias/tên dòng legacy | Import có kiểm kê/mapping trước, dừng khi mơ hồ. Cơ chế này có thể giải thích dữ liệu; chưa có lịch sử chứng minh ai/script nào tạo ID cũ |
| Seed ghi đè giá | 04/hai schema/reset 05 có ON CONFLICT UPDATE giá, bảng giá khác nhau | Tách bootstrap/demo khỏi migration vận hành; insert-only bản thiếu hoặc fail khi giá lệch; đổi giá bằng tác vụ riêng có giá cũ/mới, actor, thời điểm |
| Giá có nhiều nguồn | Class giữ snapshot; Invoice gói lấy MembershipPackage.price; package không gắn Subject | Chốt liên kết theo môn ở D03 và nguồn giá giao dịch mới; giữ snapshot lịch sử, không sửa mỗi Subject rồi tuyên bố đã đồng bộ |
| Inactive vẫn xuất hiện trong dropdown | PublicCatalogController lọc Active, SubjectService.findAll trả mọi dòng | Luồng bán/tạo lớp chỉ dùng danh mục hợp lệ; màn quản trị được xem Inactive có nhãn. Không chỉ sửa status rồi bỏ qua FE đang dùng list tất cả |
| Cập nhật giá đồng thời mất thay đổi | SubjectService.update chưa có version/khóa cho giá | Kiểm phiên bản/giá mong đợi, audit trước/sau; không seed ngầm ghi đè tác vụ của Manager |

**Owner đã chốt phạm vi chỉ gồm 9 môn trong Member.pdf.** Không tự thêm, suy ra hoặc giữ môn ngoài danh mục này. Ý kiến trước đó về việc mở rộng danh mục không còn là phương án đang chọn; muốn mở rộng phải có yêu cầu mới.

### 10.6. Phạm vi đã chốt và giới hạn

- Đã chốt cặp giá/môn và đủ 18 mức tiền PDF/04, cho phép kiểm kê và ghi rủi ro.
- Đã chốt xóa hẳn 8 bản trùng thay cho Inactive, giữ ID 19–27; chỉ giữ môn trong Member.pdf nên loại ID 7 Bắn súng khỏi danh mục. Chưa chạy SQL. Còn xử lý lớp/gói có quyền học hoặc chứng từ cũ, miễn phí/override ngoài 9 giá đã chốt.
- Chưa sửa DB/schema/migration/code để áp giá mới. Không chạy unit tests vì chỉ kiểm kê và cập nhật docs; đã chạy truy vấn DB thật read-only và kiểm count/FK/index. Không chứng nhận toàn bộ hệ thống hoặc cấu hình Render.

### 10.7. D02-b đã chốt — xóa hẳn bản dư, không dùng Inactive

Owner yêu cầu ưu tiên đúng môn và giá trong Member.pdf, không tự thêm hoặc giữ môn khác; đồng ý giữ ID 19–27 làm danh mục chuẩn và **xóa 8 bản trùng**, coi đây là lỗi thiết kế và không muốn giữ Inactive gây nhầm khi demo.

- Giữ nguyên ID 19–27 và liên kết hiện có; áp đúng bảng niêm yết đã chốt cho danh mục khi triển khai.
- Xóa cứng ID 1,2,3,4,5,6,8,9; không thay bằng Inactive và không hỏi lại lựa chọn này.
- ID 7 Bắn súng là môn ngoài Member.pdf: thuộc phạm vi loại bỏ theo yêu cầu chỉ giữ 9 môn, không đổi tên thành Bắn cung hoặc tự cấu hình giá. Với snapshot hiện tại, kế hoạch loại bản dư gồm ID 1–9; chỉ 8 bản trong đó là trùng/đồng nghĩa.
- Chưa chạy SQL trong giai đoạn phân tích. Khi triển khai, dùng migration riêng theo mapping ID + tên/slug/giá kỳ vọng và kiểm FK dưới lock; dừng nếu bản cần xóa đã có liên kết hoặc không khớp snapshot. Không DELETE theo tên mơ hồ, không cascade, không reset DB.
- Siết đường tạo/import/seed để không sinh lại bản dư hoặc giá 0; đồng bộ cả hai schema/seed dùng cho DB mới, không chỉ dọn DB hiện hữu rồi để bootstrap tạo lại.
- Tạo audit trước/sau trong quá trình chuyển đổi, không giữ Subject dư chỉ để ghi dấu; không sửa Invoice.amount cũ. Đây là quyết định xử lý danh mục, chưa phải nghiệm thu migration/code.

### 10.8. D02-c đã chốt — Enrollment 2/Class 4 là fixture được đối soát/tạo lại

Sau khi chốt danh mục, cần phân biệt **giá niêm yết mới** với **quyền học/chứng từ đã tồn tại**. Không tự chuyển câu “dùng giá Member.pdf” thành truy thu hoặc ghi đè amount cũ.

Kiểm tra bổ sung chỉ đọc trong DB .env cho thấy:

- Class ID 4, Pickleball Normal, tuition_fee **1.100.000**, trong khi giá đã chốt là **1.800.000**.
- Enrollment ID **2**, status **Registered**, enrolled_at 2026-10-22 19:30:44.239491.
- Số Invoice cùng Member và cùng Class của đăng ký này: **0** (không xuất danh tính Member).
- Khi kiểm kê chưa có bằng chứng thu học phí lớp; thiếu Invoice không tự cho phép xóa. Sau đó Owner đã xác nhận chính đăng ký này là fixture demo được phép đối soát và tạo lại theo giá mới; xem quyết định bên dưới.
- Class 1 có Invoice Paid giá 900.000, trùng bảng mới; không có lý do sửa chứng từ này. Package 5 có Invoice Paid 3.500.000 và quyền học cũ; để riêng bước gói theo môn.

**Quyết định Owner:** “Đăng ký đó là dữ liệu demo được phép đối soát rồi tạo lại đúng luồng/giá mới”.

- Cho phép xử lý riêng Enrollment ID 2 của Class ID 4 và tạo lại đăng ký Pickleball Normal với giá **1.800.000**.
- Trước thao tác phải kiểm lại mapping Member/Class/status và mọi liên kết phát sinh kể từ snapshot. Ghi nhận trạng thái trước/sau và lý do đối soát fixture; không reset cả DB hoặc suy quyền xóa các đăng ký khác.
- Đăng ký mới phải qua luồng tạo Invoice Pending và xác nhận thanh toán hợp lệ mới Registered. Không chèn Registered hoặc Paid trực tiếp để có dữ liệu demo đẹp.
- Class Normal phải đáp ứng lịch đủ 12 buổi và các validation hiện hành trước khi đăng ký lại. Lịch demo thiếu không được bỏ qua chỉ vì giá mới đã đúng.
- Không mở quyền hủy Normal cho Member/staff để xử lý fixture. Chuyển đổi dữ liệu demo có kế hoạch riêng; cơ chế audit/lưu dấu hay xóa bản fixture cụ thể sẽ được thể hiện trong kế hoạch triển khai, không tự cấp quyền xóa dây chuyền.
- Không thay Invoice.amount hoặc chứng từ Paid đã có của bản ghi khác. Đây là xác nhận phạm vi một fixture, chưa phải xác nhận mọi subscription/gói trong DB đều được reset.
- Chưa chạy SQL, code hoặc test mới trong giai đoạn chốt phương án này. Không hỏi lại nguồn gốc Enrollment 2.

### 10.9. D02-d đã chốt — gói 5 và hai subscription là fixture demo

Theo snapshot kiểm kê ở 10.4:

- MembershipPackage ID **5**, Advanced, giá **3.500.000**, 12 credit/45 ngày, không gắn môn.
- Có **2 subscription, trong đó 1 Active**, và **Invoice ID 1 Paid 3.500.000**.
- Số tiền 3.500.000 trùng giá Advanced đã chốt của nhiều môn; không đủ căn cứ suy đây là Bóng đá, Bóng rổ hay Bóng chuyền. Không đổi tên gói hoặc tự gán môn từ amount.
- Sau kiểm kê, Owner đã xác nhận riêng gói 5 và hai subscription liên quan cũng là fixture demo được phép đối soát/tạo lại theo môn. Không cần giữ nguyên mô hình gói chung để bảo vệ quyền học demo cũ; Invoice Paid vẫn không được tự ghi đè sang giá môn khác.

**Quyết định Owner:** “Gói 5 và hai subscription đó cũng là fixture demo được phép đối soát/tạo lại theo môn”.

- Phạm vi xử lý gồm mẫu gói ID 5 và đúng hai subscription được đối chiếu mapping tại thời điểm triển khai; không tự mở rộng thành reset toàn bộ member_packages hoặc gói Normal khác.
- Được đối soát quyền/credit/hạn cũ và tạo fixture theo môn mới qua luồng Invoice Pending → xác nhận thanh toán hợp lệ → Active/nạp credit. Không sửa trạng thái thành Paid/Active trực tiếp hoặc gán môn từ số tiền 3.500.000.
- Không tự chọn môn cho từng fixture; mapping Member/môn/Coach mới được xác định trong kế hoạch seed E2E, không suy từ giá hoặc tên gói cũ.
- Giữ nguyên amount của Invoice 1 Paid 3.500.000, lưu dấu đối soát và không tính hóa đơn cũ như thanh toán cho đơn mới. Tránh nhân đôi doanh thu/credit khi tạo fixture mới; phải phân biệt dữ liệu đối soát cũ với giao dịch test mới.
- Cho phép tạo lại fixture không tự giải quyết FK Invoice.package_id/subscription_id khi xóa gói cũ: cần kế hoạch lưu vết/quan hệ lịch sử nhất quán, không cascade, không xóa hóa đơn để vượt FK. Quy tắc xóa cứng môn trùng không tự áp cho chứng từ thanh toán.
- Chưa thực thi SQL/code/tests. Hoàn thành xác nhận nguồn gốc D02-c/d; chuyển sang D03/I.1 để chọn mô hình mua theo môn và Primary Coach.

## 11. D03/I.1 — đánh giá đăng ký Advanced theo môn và Coach chính

### 11.1. Bằng chứng source và ranh giới khái niệm

- Subject giữ normalPrice/advancedPrice; đã có chín cặp giá được Owner chốt. Đây là nguồn giá niêm yết mục tiêu, không tạo thêm bảng giá chung Advanced 3.500.000.
- MembershipPackage là mẫu sản phẩm; hiện có price nhưng chưa có subjectId. MemberPackage là từng đăng ký của Member, hiện chưa có subjectId hoặc primaryCoachId. Phải phân biệt mẫu bán và đăng ký đã mua.
- MemberPackageRequest hiện nhận packageId, chưa nhận chọn môn/Primary Coach. InvoiceService.createPackageOrder đang lấy pkg.getPrice().
- SessionService.bookPrivateSession lấy subscription, kiểm credit/hạn, sau đó nhận Class/Subject và Coach; source hiện chưa ràng buộc subject/Primary Coach cố định từ subscription. Không thể chỉ đổi nhãn gói để bảo đảm credit không dùng chéo môn.
- Đây là đối chiếu source workspace, không xác nhận SHA đang deploy hoặc API mới đã tồn tại.

### 11.2. Phần phù hợp trong phương án I.1

1. Gắn **môn và Primary Coach trên từng đăng ký**, không gắn một Coach chung lên hồ sơ Member. Member mua Tennis/Coach A và Bơi/Coach B phải có hai quyền học độc lập.
2. Chọn môn/Coach trước thu tiền; Coach Active, đúng chuyên môn, cùng trung tâm. Dữ liệu giá/role/Member không nhận tùy ý từ client.
3. Không giữ ca/sân chỉ bằng việc chọn Coach; các ca cụ thể chỉ giữ khi đặt thành công, có transaction/lock. Tách chọn người phụ trách khỏi hứa có đủ 12 lịch đã xác nhận.
4. Khi tạo đơn, BE lấy Subject.advancedPrice của môn đã chọn và chốt Invoice.amount; Paid hợp lệ mới Active/nạp credit. Thay giá sau đó không đổi số tiền đơn cũ.

Owner đã chốt mô hình D03-a tại 11.4. Điều kiện nhận học viên và cách tính 45 ngày vẫn là đề xuất riêng, không tự coi toàn bộ I.1 đã được duyệt.

### 11.3. Cải thiện để không tái sinh trùng gói/giá

- Bắt buộc đăng ký Advanced có subjectId và primaryCoachId rõ ràng; mọi ca học phải khớp đăng ký gốc. Không chọn “gói Advanced chung” rồi tùy ý học môn nào cũng được.
- **Một nguồn giá niêm yết:** Subject.advancedPrice. Mẫu gói phục vụ mô tả/quy tắc; không có thêm một giá được Manager/seed sửa độc lập để ghi đè khi mua. Invoice.amount vẫn là snapshot giao dịch, không phải giá tính động.
- UI có thể hiển thị chín lựa chọn Advanced theo chín môn mà không bắt buộc lưu chín bản mẫu gói có chín giá copy. Có thể dùng một cấu hình sản phẩm Advanced + đăng ký gắn môn, hoặc các mẫu theo môn chỉ tham chiếu Subject; lựa chọn schema là chi tiết triển khai cần thiết kế sau, không ép Owner quyết định số bảng/bản ghi.
- Không dùng tên gói để suy môn, không giữ một số dư credit Member dùng chung cho mọi môn. Khi mua lại cùng môn vẫn là đăng ký riêng, không cộng dồn/gia hạn ngầm gói cũ.
- Chọn Coach trước Paid chưa đủ bảo đảm hoàn thành 12 buổi trong hạn: Coach Active/đúng chuyên môn không chứng minh còn năng lực nhận học viên. Cần chốt tiêu chí “nhận được học viên” riêng; xem lịch rảnh chỉ là thông tin thay đổi theo thời gian, không phải giữ chỗ.
- Mốc hạn 45 ngày và cách tính Ngày 1 chưa được lựa chọn lại chỉ từ xác nhận fixture; tách sang phần thời hạn của D03. Quyền Coach/Member bấm tạo lịch thuộc D04/I.2, chưa đổi ở bước chọn sản phẩm.

### 11.4. D03-a đã chốt — mô hình mua Advanced

Owner trả lời “Chốt” đối với 5 điểm:

1. Member chọn môn và Coach chính trước thanh toán.
2. Mỗi đăng ký có môn, Coach chính, credit và hạn riêng; không gắn Coach chung lên hồ sơ Member.
3. 12 credit chỉ dùng đúng môn của đăng ký, với Coach chính hoặc Coach thay hợp lệ.
4. BE lấy Subject.advancedPrice, snapshot Invoice.amount; Paid hợp lệ mới Active/nạp credit.
5. Chọn Coach chưa giữ ca/sân; chỉ giữ tài nguyên khi đặt lịch thành công.

Không hỏi lại mô hình hoặc số tiền. Owner yêu cầu rà tài liệu/ngữ cảnh và tham khảo hệ thống tương tự để đề xuất cho các điểm còn mở.

### 11.5. Tài liệu chính thức đã tham khảo ngày 25/10/2026

| Nguồn | Cơ chế được tài liệu mô tả | Điều áp dụng/thận trọng cho SCMS |
|---|---|---|
| [Calendly — lý do không có giờ trống](https://calendly.com/help/how-to-troubleshoot-unavailable-times-that-should-be-available) | Lịch có thể bị chặn bởi giới hạn ngày/tuần/tháng, thời lượng, khoảng ngày, minimum notice, giờ bận và resource/host availability | Không đồng nhất chưa có Session với Coach có đủ khả năng nhận gói; không suy Calendly có thuật toán bảo đảm 12 buổi của SCMS |
| [Acuity — quản lý tài nguyên dùng chung](https://help.acuityscheduling.com/hc/en-us/articles/16676949567757-Use-resources-to-limit-bookings) | Phòng/thiết bị là giới hạn ngoài lịch nhân viên; nhiều nhân viên cùng rảnh chưa có nghĩa đủ phòng phục vụ | Xét Coach cùng sân/phòng phù hợp và lịch Normal đã giữ, không chỉ lịch Coach |
| [Acuity — tạo/sửa package](https://help.acuityscheduling.com/hc/en-us/articles/16676883484813-Creating-and-editing-packages) | Package có số lượt/loại dịch vụ, hạn mã tính từ mua; mã có thể đặt lịch cho buổi sau hạn tùy scheduling limits | Không sao chép cách đặt buổi sau hạn: SCMS yêu cầu buổi diễn ra trong hạn; không dùng ngày bấm đặt thay ngày học |
| [MyTime — appointments in package](https://help.mytime.com/support/solutions/articles/31000174966-appointments-in-package) | Hạn gói dựa trên múi giờ nơi mua, hiệu lực từ lúc mua đến hết ngày hết hạn | Tham khảo cách hiển thị ngày hết hạn theo múi giờ; không copy chính sách no-show/credit vì khác SCMS |
| [Wix Bookings — tạo appointment](https://support.wix.com/en/article/wix-bookings-creating-an-appointment) | Có lịch làm việc nhân viên, resources và lựa chọn manual approval; chức năng manual approval appointment đó bị giới hạn phương thức thu tiền | Chỉ tham khảo tư duy xét duyệt trước phục vụ. Luồng SCMS đề xuất là duyệt khả năng nhận học viên trước tạo Invoice VNPay, không khẳng định Wix hỗ trợ y hệt |

Những sản phẩm này có quy tắc khác nhau; không coi là một chuẩn ngành bắt buộc. Các phương án dưới đây là đề xuất riêng cho SCMS, không phải tính năng được suy diễn từ nguồn.

### 11.6. D03-b — Coach còn khả năng nhận học viên: thiếu sót hiện tại

- Coach entity hiện chỉ có user, specialization, certification, bio; chưa có lịch làm việc riêng hoặc trạng thái nhận học viên mới.
- SessionService.getCoachAvailableSlots duyệt TimeSlot.values, loại ngày quá khứ/lễ/thiếu notice/nắng nóng/trùng Coach và phòng nếu truyền roomId. Không có lịch làm việc đã công bố của Coach; khi thiếu roomId, giờ rảnh không chứng minh có sân phù hợp.
- Chưa có phép tính tải **credit đã bán nhưng chưa đặt lịch**. Đếm ca chưa có Session hoặc đếm mỗi số Member Active đều có thể đánh giá sai khả năng phục vụ.
- Một Coach có nhiều môn phải chịu tải tổng, không tính riêng từng môn rồi bán trùng năng lực. Một khung giờ có nhiều sân vẫn chỉ là một buổi Coach đó có thể dạy.

### 11.7. So sánh phương án nhận học viên

| Phương án | Lợi ích | Điểm yếu | Đánh giá cho MVP |
|---|---|---|---|
| Chỉ Active + đúng chuyên môn + có ít nhất một ca trống | Ít thao tác | Không chứng minh khả năng học đủ gói, có thể bán quá tải | Không đủ để mở thu tiền |
| Cài cứng tối đa X học viên/Coach | Dễ làm | X chưa có căn cứ; gói 1 credit và 12 credit có tải khác nhau, lịch Normal/sân thay đổi | Không tự đặt một con số tùy ý |
| Giữ đủ 12 ca trước Paid | Khả năng thực hiện rõ hơn | Đổi mô hình đã chốt không giữ ca khi mua, giảm linh hoạt | Không chọn nếu Owner giữ D03-a |
| Lịch làm việc + kiểm tải + Coach xác nhận nhận trước tạo Invoice | Có trách nhiệm vận hành, không cần bộ giải tối ưu lịch đầy đủ | Thêm bước duyệt, phải có lịch làm việc và màn xem tải; không bảo đảm mọi ngày giờ Member muốn đều có chỗ | **Đề xuất cho MVP**, Owner cần chốt bước duyệt mới |

### 11.8. Quy trình nhận học viên đề xuất ban đầu — bước 2 đã được thay thế tại 11.11

1. Manager cấu hình/xác nhận lịch làm việc thực tế theo tuần và ngày nghỉ riêng của Coach, trong bộ 7 ca chuẩn; có quyền tạm ngừng nhận mới. Không mặc định Coach làm cả 7 ca mọi ngày.
2. Member chọn môn, Coach và cung cấp các khoảng thời gian có thể học. Đây là nguyện vọng để kiểm phù hợp, chưa là Session/giữ sân.
3. BE kiểm Active/role/center/chuyên môn, lịch làm việc, khoảng notice, lễ/nắng, ca Normal/Advanced đã đặt và sân phù hợp. Hiển thị khả năng bố trí trong hạn dự kiến, tải credit chưa xếp của các đăng ký đã nhận, kể cả môn khác của Coach.
4. Coach chính xác nhận có thể nhận học viên để hoàn thành 12 buổi trong hạn; Manager có thể xác nhận thay trong center, bắt buộc ghi actor/lý do. Nếu không phù hợp, đề nghị Member chọn Coach/khung thời gian khác; chưa tạo đơn thanh toán.
5. Được xác nhận và kiểm lại hợp lệ thì Member mới tạo Invoice Pending và nhận hạn thanh toán **5 phút theo cấu hình hiện hành**. Trước đó chưa thu tiền, chưa kích hoạt 45 ngày hoặc cấp credit. Thời gian chờ duyệt không ăn vào hạn thanh toán hay hạn gói.
6. Lúc phát hành đơn, dùng lock/transaction/idempotency để ghi nhận cam kết nhận học viên; tính cả đơn Pending chưa hết hạn vào tải, không để nhiều đơn cùng vượt kiểm tra. Khi hết hạn/hủy, giải phóng cam kết đó. Đây là cam kết tải, **không giữ ca/sân hoặc trừ credit**.
7. Paid hợp lệ mới kích hoạt; callback lặp không lặp credit. Kiểm lại trạng thái cần thiết khi kích hoạt, giữ cơ chế đối soát nếu đơn đã hết hạn/không còn payable, không tự chiếm lại tài nguyên hay kéo hạn để che lỗi.

**Cách hiểu tải, tránh trừ kép:** ca đã đặt được trừ khỏi lịch rảnh; phần credit khả dụng chưa đặt của các đăng ký đã nhận là nghĩa vụ còn phải bố trí. Đơn Pending đã giữ cam kết năng lực cần tính số credit sẽ cấp (12), dù remainingCredit hiện còn 0. Không tính thêm 12 cho gói đã dùng gần hết, không đếm ca đã đặt hai lần.

**Giới hạn cần nói rõ:** tổng ca rảnh 45 ngày chưa đủ kết luận có lịch khả thi: còn hạn sớm của gói cũ, khung giờ Member, sân dùng chung giữa Coach và độ phân bố lịch. Bước duyệt có màn tải/cảnh báo và trách nhiệm vận hành, chưa phải một chứng minh tối ưu đủ lịch. Không được hứa đã giữ sẵn 12 ca. Nếu muốn cam kết lịch chắc chắn từ đầu, phải quay lại mô hình giữ/xác nhận ca cụ thể, khác D03-a.

**Cập nhật:** Owner đồng ý hướng đề xuất và sau đó chốt bỏ bước Member nhập thời gian rảnh; xem 11.11. Bước 2 ở danh sách trên được giữ làm dấu đề xuất ban đầu, không còn là yêu cầu MVP.

### 11.9. D03-c — so sánh ban đầu, quyết định cuối tại 11.15

| Lựa chọn | Ý nghĩa | Đánh đổi |
|---|---|---|
| 45 ngày lịch, Paid là Ngày 1 | Hết cuối ngày thứ 45 ở Asia/Ho_Chi_Minh | Dễ hiểu/vận hành, khớp đề xuất analysis 2410; trả tiền cuối ngày vẫn tính ngày đó là Ngày 1 |
| Đủ 45×24 giờ từ Paid | Hết đúng giờ sau 1080 giờ | Công bằng theo thời lượng; phải hiển thị/kiểm timestamp đầy đủ, không chỉ endDate |
| Bắt đầu từ buổi đầu | Chưa chạy hạn khi chưa học | Khác hướng từ Paid trong AGENTS/đề xuất 2410; phải thêm hạn kích hoạt để không treo gói vô thời hạn |

**Đề xuất MVP: 45 ngày lịch, Paid là Ngày 1.** Đây chưa là quyết định đã chốt. Cách hiểu này cần công bố trước thanh toán, đặc biệt khi Member mua muộn trong ngày; không gọi là đủ 1080 giờ.

Quy tắc đề xuất:

- Ngày Paid là ngày xác nhận thanh toán thành công hợp lệ theo Clock Asia/Ho_Chi_Minh; không lấy ngày tạo Invoice/Pending hoặc thời gian client gửi. Callback nhận tiền muộn cần đối soát vẫn không tự kích hoạt.
- startDate = paidDate; endDate = paidDate.plusDays(44). Hết hạn tại **00:00 của endDate + 1**, dùng mốc kết thúc exclusive để tránh sai số 23:59:59.
- Ví dụ Paid ngày **25/10/2026** → Ngày 45 **08/12/2026**, hết hạn **00:00 ngày 09/12/2026**. Nếu Paid 22:00 ngày 25/10 thì thời gian sử dụng thực tế ngắn hơn 45×24 giờ; UI phải thể hiện ngày hết hạn.
- Gói chỉ Active từ lúc Paid; không hợp lệ trước lúc thanh toán chỉ vì startDate là ngày đó. Buổi mới phải đủ notice và **toàn bộ buổi kết thúc trong hạn**; không đặt ngày cuối để học vượt hạn.
- Nếu Pending qua nửa đêm rồi Paid hợp lệ, tính ngày Paid thực tế. Ngày hết hạn hiển thị trước mua là dự kiến; xác nhận/gửi lại hạn sau Paid.
- Cron 00:00 cập nhật trạng thái và xử lý credit còn lại theo rule; API vẫn chặn theo hạn thực tế ngay cả khi cron chậm. Đặt lịch/hủy/hoàn không chỉ tin status Active trong DB.
- Không tự trừ ngày nghỉ hoặc gia hạn do dời lịch; trường hợp trung tâm gây mất quyền học xử lý riêng D08/I.5, không silently đổi quy tắc hết hạn tại đây.
- Mốc Paid là mốc bắt đầu đề xuất, không phải luôn sửa endDate cũ cho mọi gói legacy. Fixture đã được duyệt chuyển đổi riêng; không đổi dữ liệu vận hành khác chỉ vì sửa công thức mới.

**Thiếu nhất quán source:** activateSubscription hiện set startDate=today và endDate=today.plusDays(days); luồng đặt lịch cho phép học đến hết endDate. Với days=45, nếu diễn giải Paid là Ngày 1 thì đang bao gồm 46 ngày lịch. Nếu Owner chọn quy tắc inclusive nêu trên, cần đổi phép tính cho đăng ký mới và cập nhật docs/tests, không chỉ đổi nhãn UI.

**Câu hỏi D03-c:** Chốt 45 ngày lịch/Paid là Ngày 1 như ví dụ trên, hay chọn đủ 45×24 giờ? Chưa thay code/AGENTS/contract theo một phương án chưa duyệt.

### 11.10. Phạm vi tiếp theo và kiểm chứng cần có

- Đã chốt D03-a; Owner đồng ý hướng D03-b và chốt bỏ bước nhập thời gian rảnh, D03-c còn chờ chọn cách tính, cập nhật tại 11.11–11.14. Chưa chuyển sang chốt quyền tạo lịch I.2, cutoff hủy I.3 hoặc gia hạn I.5.
- Khi triển khai phải kiểm thiếu giờ làm, Coach ngừng nhận, xung đột Normal/Advanced/phòng, hai đơn tranh cam kết năng lực cuối, Pending hết hạn, callback lặp/muộn, Paid qua nửa đêm và biên ngày cuối. Không coi một tổng ca rảnh đơn giản là nghiệm thu khả năng phục vụ toàn bộ gói.
- Đã kiểm source, nguồn chính thức và phép tính ngày bằng Python; không sửa backend/DB, chưa chạy test ứng dụng cho luồng mới. Toàn bộ cơ chế xác nhận nhận học viên/giữ cam kết là **đề xuất SCMS**, không phải tính năng đã có hoặc kết luận mọi hệ thống tham khảo làm giống nhau.

### 11.11. Rà bước Member cung cấp thời gian rảnh — đã chốt bỏ khỏi MVP

Owner ban đầu ghi “D02-b” nhưng trích đúng bước 2 của D03-b. Ghi nhận đồng ý hướng xét khả năng nhận học viên trước thu tiền theo nội dung đang thảo luận; không sửa quyết định D02-b xóa môn dư. Sau đó Owner trả lời “1. Ok”, chốt bỏ bước Member khai lịch rảnh lúc mua trong MVP. Bước 2 ở 11.8 là đề xuất cũ đã được thay thế bằng quyết định này.

- Đối chiếu analysis 2410.pdf I.1/I.2: trước mua chọn môn/Coach và kiểm khả năng nhận; sau Paid, Coach và Member trao đổi lịch qua điện thoại/Zalo rồi mới tạo ca. Tài liệu không yêu cầu Member khai lịch rảnh có cấu trúc khi mua.
- Nhập nguyện vọng trước mua không mâu thuẫn trực tiếp với nguyên tắc không giữ ca, nhưng **mở rộng thiết kế**: thêm form, DTO, lưu lịch tuần/ngày ngoại lệ, sửa/xóa nguyện vọng và đối chiếu khả năng tương thích. Nếu chỉ nhập ghi chú tự do thì không thể dùng làm phép kiểm lịch tự động đáng tin cậy.
- **Đã chốt bỏ bước nhập lịch rảnh khỏi MVP**, không thêm cả trường optional ở bước mua khi chưa có nhu cầu rõ. Member chọn môn/Coach; có thể tham khảo lịch làm việc Coach công bố khi tính năng này được triển khai. Coach xem tải và xác nhận khả năng nhận; sau Paid mới thống nhất ca cụ thể như I.2. Không triển khai bộ ghép lịch Member–Coach.
- Bước duyệt chỉ xác nhận năng lực phục vụ dự kiến; không bảo đảm Member sẽ rảnh vào mọi ca còn lại hoặc giữ sẵn 12 buổi. Bỏ dữ liệu thời gian rảnh làm giảm khả năng kiểm tương thích trước bán; phải công bố giới hạn này, không diễn đạt thành “đã bảo đảm hoàn thành 12 buổi”. Nếu cần bảo đảm phù hợp lịch cá nhân trước thu tiền thì phải bổ sung trao đổi trước mua và chốt lại thay đổi luồng, không tự thêm vào đây.
- Các phần khác của hướng D03-b được giữ để thiết kế: lịch làm việc thực, tải các môn và phòng, duyệt trước Invoice, chờ duyệt không chạy 5 phút/45 ngày, kiểm lại bằng transaction khi phát hành đơn. Không tự coi phần công thức tải đã có trong source.

### 11.12. D03-c bổ sung — phương án Ngày 0 từng xem xét, không chọn (xem 11.15)

**Đánh giá:** Có lý như một ưu đãi cuối ngày. Trong hai mốc Owner nêu, đề xuất **từ 19:45 trở đi**, khớp giờ kết thúc Ca 7; 19:00 chưa có chính sách đóng cửa/đóng thu tiền tương ứng. Đây là ngoại lệ mới so với đề xuất I.1 “Paid là Ngày 1” và quy tắc 45 ngày từ thanh toán; không tự cập nhật AGENTS hoặc code trước khi chốt.

**Giới hạn:** Notice hiện tại tối thiểu 12 giờ, Ca 7 bắt đầu 18:15. Sau 06:15 đã không thể tạo mới ca học diễn ra trong ngày, ngay cả khi chưa tới giờ đóng cửa (còn phụ thuộc lịch làm việc, lễ và sân). Vì vậy mốc 19:45 không đại diện cho “mọi trường hợp không còn học được hôm nay”. Không dùng lịch trống thay đổi theo thời gian để tự quyết ngày bắt đầu gói. Không khẳng định 19:45 là chuẩn ngành.

Phương án ngày lịch có ưu đãi nếu được chọn:

- Paid hợp lệ mới Active/nạp 12 credit **ngay lúc xác nhận**, kể cả Ngày 0; không chờ tới nửa đêm để cho phép đặt ca tương lai. Vẫn kiểm notice và hạn cho buổi học. Ngày 0 là quy ước đếm hạn, không phải trạng thái hóa đơn hoặc trì hoãn thanh toán.
- Theo Clock Asia/Ho_Chi_Minh: trước 19:45, Day1 = paidDate; từ đúng 19:45 trở đi, Day1 = paidDate + 1. endDate = Day1 + 44; hết hạn exclusive tại 00:00 ngày kế tiếp endDate.
- Phân biệt paidAt/activatedAt thực tế với ngày bắt đầu đếm 45 ngày. Không sửa paidAt thành ngày hôm sau; không dùng ngày tạo Invoice hoặc thời gian do FE gửi để tính. Mốc xác nhận hợp lệ của BE áp dụng thống nhất Cash/VNPay; callback lặp không tính lại hạn, callback cần đối soát không tự kích hoạt.

| Paid tại Việt Nam | Ngày 1 | Ngày 45 | Hết hạn exclusive |
|---|---|---|---|
| 25/10/2026 19:44 | 25/10/2026 | 08/12/2026 | 09/12/2026 00:00 |
| 25/10/2026 19:45 | 26/10/2026 | 09/12/2026 | 10/12/2026 00:00 |
| 25/10/2026 22:00 | 26/10/2026 | 09/12/2026 | 10/12/2026 00:00 |

**Đánh đổi cần chốt:** Hai giao dịch cách nhau một phút quanh 19:45 có ngày hết hạn lệch một ngày; khoảng sử dụng không đồng nhất. Nếu ưu tiên thời lượng công bằng thì **đủ 45×24 giờ từ Paid** tốt hơn và không cần cutoff, nhưng phải đổi kiểm hạn theo timestamp, hiển thị giờ hết hạn và không chỉ dựa cron 00:00. Nếu ưu tiên vận hành bằng ngày lịch và ưu đãi mua sau đóng cửa, phương án >=19:45 rõ hơn mốc 19:00. Không ghép cả hai công thức.

[Wix — Offering Packages](https://support.wix.com/en/article/wix-bookings-offering-packages) có cấu hình ngày bắt đầu gói riêng (Specific date/Chosen by customer). Nguồn chỉ giúp đối chiếu việc tách ngày bắt đầu; không chứng minh Wix dùng Ngày 0 hoặc cutoff 19:45. Đề xuất cutoff là chính sách riêng của SCMS.

### 11.13. Theo dõi bàn giao FE và điểm dừng

Owner yêu cầu mọi API/feature/nghiệp vụ mới phải được note khi bàn giao. Đã mở mục 14 của API contract dưới dạng **dự kiến, chưa triển khai**: lịch làm việc/nhận mới, xét duyệt trước Invoice, chọn môn/Coach trên từng đăng ký, kiểm tải không giữ ca, mốc Paid/Ngày 1/hết hạn. Mỗi thay đổi cần endpoint/payload/quyền/status/error thực tế và trạng thái triển khai; không để FE lập trình theo tên API giả định.

- Đã chốt bỏ yêu cầu Member nhập lịch rảnh khi mua trong MVP. Owner đã chốt D03-c đủ 45×24 giờ, xem 11.15; các so sánh cutoff ở 11.9/11.12/11.14 là lịch sử phân tích, không phải quy tắc đang chọn. Chưa triển khai code/DB.
- Chỉ sửa tài liệu; không sửa backend/DB hoặc chạy migration. Đã đối chiếu lại I.1/I.2, notice 12 giờ trong SessionService và ca chuẩn; kiểm ví dụ thời hạn bằng phép tính ngày. Chưa có kiểm thử ứng dụng cho các tính năng dự kiến.

### 11.14. D03-c — đánh giá cutoff 18:15 và chi phí thực tế của 45×24 giờ

**18:15 không phải mốc suy ra từ notice 12 giờ.** Ca cuối bắt đầu 18:15 nên để tạo mới ca đó phải thao tác muộn nhất 06:15 cùng ngày. Nếu xét ca đầu 06:00 hôm sau thì mốc đặt muộn nhất là 18:00 hôm trước; thao tác 18:15 chỉ đủ notice từ 06:15 hôm sau, đã lỡ giờ bắt đầu ca đầu. Các mốc này còn giả sử Coach/sân rảnh, môn cho phép và không nghỉ lễ. Có thể chọn 18:15 như ưu đãi thương mại, nhưng không diễn giải thành kết quả tất yếu của quy tắc 12 giờ. Không tiếp tục dịch cutoff từ 19:45 sang 18:15 chỉ để giải quyết từng ngoại lệ.

**Khuyến nghị sau khi kiểm source: chọn đủ 45×24 giờ từ Paid hợp lệ cho đăng ký Advanced mới.** Độ phức tạp triển khai vừa phải do code hiện dùng ngày, nhưng quy tắc vận hành đơn giản hơn hệ thống Ngày 0/cutoff. Đây vẫn là đề xuất, chưa phải Owner đã chốt.

| Phần | Source hiện tại | Tác động dự kiến nếu chọn 45×24 |
|---|---|---|
| Dữ liệu | MemberPackage.startDate/endDate là LocalDate; Invoice.paymentDate đã là LocalDateTime | Dự kiến thêm hai mốc kích hoạt/hết hạn chính xác trên subscription; chốt kiểu timestamp/mapping PG và SQL Server khi thiết kế migration. Không thêm bản ghi mỗi giờ, không thêm bảng lịch cho phép tính hạn |
| Kích hoạt | InvoiceService.activateSubscription và MemberPackageService dùng phép cộng ngày | Chỉ Paid hợp lệ kích hoạt một lần; expiresAt = activatedAt + 1080 giờ. Callback lặp không đặt lại thời gian; giao dịch đối soát không tự kích hoạt |
| Kiểm quyền/đặt/hủy/dời | SessionService kiểm ngày hết hạn, hoàn credit và có gia hạn theo ngày | Một hàm dùng chung kiểm hiệu lực theo Clock; lúc đặt kiểm hiện tại chưa hết hạn, ca bắt đầu không trước kích hoạt và kết thúc không vượt hạn. Không chỉ đổi công thức lúc mua rồi bỏ sót hoàn credit/dời lịch |
| Hết hạn | Repository truy vấn endDate < today; PackageExpirationJob chạy 00:00 | Truy vấn deadline timestamp; có thể giữ cron 00:00 để dọn trạng thái/credit, còn API chặn đúng deadline ngay cả khi job chưa chạy. Không cần timer/job riêng cho từng gói; tần suất dọn sớm hơn chỉ là lựa chọn vận hành sau |
| Quyền liên quan và báo cáo | EnrollmentService, AttendanceService, ReportService còn kiểm ngày | Rà các phép kiểm gói Advanced, không để báo cáo/đọc API coi còn quyền chỉ vì status chưa được cron đổi. Không áp điều kiện “gói còn hạn hiện tại” để cấm hoàn tất điểm danh lịch sử đã học hợp lệ |
| API/FE | MemberPackageResponse, membershipMappers/MyMembershipView/MemberDetailView đang dùng ngày | Bổ sung mốc giờ hết hạn có offset/múi giờ rõ; FE hiển thị giờ Việt Nam và lấy deadline BE trả, không tự +45 ngày hoặc cắt mất phần giờ. Invoice.expiresAt giữ nghĩa 5 phút, không dùng làm hạn học |
| Tương thích | Gói Normal và Advanced dùng chung entity; có dữ liệu legacy | Chỉ đổi rule Advanced trong phạm vi đã chốt; giữ trường ngày nếu cần tương thích nhưng timestamp là nguồn kiểm quyền cho gói mới. Lập kế hoạch backfill/đối soát riêng, không suy giờ thanh toán khi dữ liệu cũ không đủ, không ghi đè invoice Paid |

Chi phí dữ liệu là hằng số trên mỗi đăng ký, không tăng theo 1080 giờ. Chi phí chính là **đồng bộ tất cả nơi kiểm quyền và chuyển đổi contract/dữ liệu cũ**, không phải phép cộng thời gian. Tránh giữ hai công thức ngày/timestamp cùng có quyền quyết định cho một gói; kiểm hạn tập trung và dùng Clock được inject. Tên trường/schema trên đây là thiết kế dự kiến, chưa phải payload/DDL đã bàn giao.

Ví dụ: Paid hợp lệ **25/10/2026 18:30 UTC+7** → hết hạn **09/12/2026 18:30 UTC+7**. Trước deadline còn quyền, từ đúng deadline hết quyền; ca phải bắt đầu trước deadline và kết thúc không vượt deadline. Notice 12 giờ vẫn độc lập: đây là đủ 1080 giờ hiệu lực, không phải cam kết có thể học ngay sau thanh toán hoặc đủ mọi ca mong muốn. Không cộng thêm 12 giờ hoặc Ngày 0 vào công thức này.

Kiểm thử cần khi triển khai: ngay trước/đúng deadline, ca kết thúc đúng/vượt deadline, notice đúng 12 giờ, Paid qua nửa đêm, callback lặp/muộn, hoàn credit gần hết hạn, job chưa chạy, gia hạn và phân biệt Normal/Advanced. Không cần stress test hay bộ giải lịch mới chỉ vì chuyển sang timestamp.

Đã kiểm entity/repository/service/DTO và các điểm FE nêu trên, đối chiếu phép cộng thời gian bằng Python. Chỉ cập nhật docs; chưa sửa code/schema/DB hoặc chạy Maven. Dừng hỏi chọn chính sách D03-c trước thiết kế/triển khai.

### 11.15. D03-c đã chốt — đủ 45×24 giờ và minh bạch hạn khi mua/đặt ca

Owner chốt **45×24 giờ (1080 giờ) từ Paid hợp lệ**, yêu cầu làm rõ thông tin nhắc hết hạn và trách nhiệm BE/FE. Thay thế các đề xuất Ngày 1/ngày lịch hoặc Ngày 0/cutoff ở trên; không cộng thêm 12 giờ vào thời lượng. Chỉ áp dụng phạm vi Advanced đã thảo luận, chưa tự đổi gói Normal hoặc dữ liệu legacy.

- BE xác định mốc kích hoạt từ xác nhận Paid hợp lệ và lưu mốc hết hạn chính thức, callback lặp không tính lại; giữ quy trình đối soát đối với giao dịch không còn payable. Gói có hiệu lực trước mốc hết hạn; từ đúng mốc đó không còn quyền tạo mới ca.
- **BE quyết định quyền, FE trình bày/cảnh báo.** API trả hạn chính thức có giờ và offset/múi giờ rõ. Có thể bổ sung serverTime khi thiết kế DTO để đếm thời gian còn lại nhất quán; tên trường/payload chưa triển khai. BE kiểm lại deadline/credit/notice và toàn bộ ca trong transaction, không tin FE đã disable nút hoặc đồng hồ máy người dùng.
- Trước mua, FE nêu rõ “Hiệu lực 45×24 giờ từ thanh toán thành công; buổi học phải kết thúc trước hoặc đúng hạn; đặt ca trước tối thiểu 12 giờ”. Nếu hiển thị hạn dự kiến trước Paid, phải gắn nhãn dự kiến; sau Paid hiển thị hạn BE xác nhận. Không lấy thời điểm tạo Invoice làm hạn chính thức.
- Trang gói và màn tạo/dời ca hiển thị “Hết hạn lúc HH:mm ngày DD/MM/YYYY (giờ Việt Nam)” và thời gian còn lại theo ngày/giờ nếu cần. Không chỉ ghi ngày hết hạn hoặc làm tròn số ngày theo hướng còn cả ngày.
- Khi chọn ngày cuối, FE đánh dấu ca vượt hạn không thể chọn và nêu lý do cụ thể: “Ca kết thúc lúc …, vượt hạn gói lúc …”. Kết quả lịch trống cho đăng ký cụ thể cần tính cả hạn gói/notice, không chỉ lịch Coach chung; khi gửi tạo/dời, BE vẫn kiểm lại và trả lỗi có lý do rõ để FE tải lại trạng thái.
- Chưa thể viết một câu chung “không thể đặt từ [hạn gói - 12 giờ]”: còn thời lượng 90 phút, 7 ca cố định, môn/Coach/phòng/lễ. Mốc đặt cuối cho **một ca** = giờ bắt đầu ca - 12 giờ; ca đó đồng thời phải kết thúc không vượt hạn gói. Nếu trả ca cuối khả dụng/mốc đặt cuối, BE phải tính theo dữ liệu thực tế và FE thể hiện là thông tin tại thời điểm tra cứu, không là giữ chỗ.

**Ví dụ đã kiểm:** Paid 25/10/2026 18:30 → hết hạn 09/12/2026 18:30 UTC+7. Ngày cuối, Ca 6 **16:30–18:00** nằm trong hạn; Ca 7 **18:15–19:45** vượt hạn nên không được chọn. Riêng để tạo Ca 6 phải thao tác muộn nhất **09/12/2026 04:30**, còn cần Coach/sân/ngày học hợp lệ. Vào 10:00 ngày cuối dù gói chưa hết hạn, các ca còn lại có thể đều không đủ notice; FE cần giải thích “Không còn ca đáp ứng điều kiện đặt trước và hạn gói”, không đổi gói thành Expired sớm. Ca cuối hợp lệ không đồng nghĩa đã có sân/Coach rảnh.

Nhắc trực tiếp trên giao diện và lỗi validation là phạm vi đang làm rõ. Nếu muốn gửi thông báo chủ động trước hạn qua notification/email, cần chốt riêng ngưỡng, kênh và người nhận; chưa tự thêm các mốc 7/3/1 ngày hoặc job gửi mới. Phân biệt với thông báo hạn ngay lúc mua/đặt ca và thông báo tạo lịch đã nêu ở I.2.

Đã cập nhật checklist và API contract mục 14 để bàn giao FE; chưa thêm endpoint/error code giả định, chưa sửa AGENTS/code/schema/DB hoặc chạy Maven. Khi triển khai cần đồng bộ AGENTS và rule timestamp theo quyết định này, kiểm biên đúng deadline/notice và ca cuối hợp lệ.

### 11.16. Handoff BE — đặc tả cơ chế thông tin/hạn Advanced đã được Owner đồng ý

Owner yêu cầu lưu rõ danh sách cập nhật, giải thích thiết kế và hướng dẫn bàn giao BE sau này. **Đây là đặc tả để triển khai, không phải báo cáo code đã hoàn thành.** Các mốc dữ liệu/tên field dưới đây là ngữ nghĩa dự kiến; khi code phải cập nhật JSON/schema thực tế vào API contract.

| Cập nhật cần làm | Mục đích và ràng buộc |
|---|---|
| Lưu thời điểm kích hoạt và hết hạn Advanced | Đủ 1080 giờ từ Paid hợp lệ; không dùng endDate chỉ có ngày làm nguồn kiểm quyền gói mới. Không sửa số tiền/ngày thanh toán hóa đơn để mô phỏng Ngày 0 |
| Một hàm kiểm hiệu lực dùng chung | Dùng Clock được inject; hiệu lực theo khoảng [activatedAt, expiresAt). Không chỉ kiểm status Active; không để cron chạy chậm làm phát sinh quyền quá hạn |
| Kiểm cả giờ kết thúc ca | sessionStart >= activatedAt; sessionEnd <= expiresAt; sessionStart >= now + 12 giờ; giữ ca chuẩn 90 phút và mọi kiểm tra tài nguyên. Không rút ngắn ca cuối để nhét vào hạn |
| Lịch khả dụng theo đăng ký | Kết hợp hạn/credit/môn/Coach của subscription với lịch Coach/phòng/Member/ngày nghỉ. Lịch rảnh chung trước mua không đủ để FE kết luận ca của một gói được đặt |
| Thông tin hạn ở API | Cung cấp timestamp có offset rõ, ví dụ giờ Việt Nam +07:00; dự kiến thời gian máy chủ để FE tính còn lại. Tránh tên/ý nghĩa nhầm với Invoice.expiresAt giữ thanh toán 5 phút |
| Phản hồi ca không hợp lệ | Trả lý do phân biệt gói hết hạn, ca vượt hạn, thiếu notice, hết credit và xung đột tài nguyên; ánh xạ status/error theo quy ước dự án khi triển khai. Chưa tự công bố mã lỗi mới là có sẵn |
| Thông tin mua/đặt/dời trên FE | Trước mua nêu quy tắc và nhãn hạn dự kiến; sau Paid dùng hạn chính thức BE trả; hiển thị giờ Việt Nam, thời gian còn lại và lý do ca không chọn được. Không làm tròn ngày thành quyền học cả ngày |
| Không còn ca phù hợp nhưng chưa hết hạn | Trả lịch/lý do đúng điều kiện, không đổi status Expired sớm, không tự hoàn/gia hạn vì không còn ca đặt được. Hướng xử lý sự cố/gia hạn còn ở D06–D08 |

**Trình tự bàn giao/triển khai BE:**

1. Đọc quyết định 11.4/11.11/11.15 cùng mục này, đối chiếu scope Advanced và D04 còn mở. Không lấy các công thức Ngày 1/Ngày 0 cũ làm yêu cầu mới. Đồng bộ AGENTS §2.4 về định nghĩa 1080 giờ khi triển khai; không sửa quyền người tạo ca trước khi D04 chốt.
2. Thiết kế migration riêng cho timestamp, mapping PostgreSQL/SQL Server và index truy vấn hết hạn; giữ tương thích field ngày nếu FE cũ còn dùng. Kiểm dữ liệu legacy/fixture trước backfill; không tự suy giờ thiếu, không ghi đè Invoice Paid hoặc reset cả DB. Chốt nguồn timestamp duy nhất cho đăng ký mới; trường ngày chỉ là biểu diễn tương thích.
3. Sửa kích hoạt tại InvoiceService và các đường ghi subscription ở MemberPackageService: Paid hợp lệ mới cấp credit/kích hoạt một lần, không gia hạn lại do callback lặp, không kích hoạt khi đang cần đối soát. Quy tắc này là 1080 giờ **từ Paid hợp lệ**, không thêm 12 giờ chờ đặt vào hạn.
4. Dùng chung policy tại SessionService (book/hoàn/dời), Membership service/repository/job và các điểm kiểm quyền trong EnrollmentService/AttendanceService/ReportService. Gia hạn dùng deadline nhất quán theo chính sách D08 sẽ chốt; không dùng kiểm hạn hiện tại để cấm lưu điểm danh lịch sử hợp lệ. Có thể giữ cron 00:00 để dọn trạng thái/credit, API luôn kiểm hạn thời gian thực.
5. Cập nhật MemberPackageResponse, API tra lịch và lỗi; ghi endpoint/method/payload/quyền thực tế vào API contract, đánh dấu thay đổi tương thích/không tương thích. Bàn giao FE ví dụ trước Paid, sau Paid, hết hạn, chưa hết hạn nhưng hết ca hợp lệ, thiếu notice và lỗi tranh chấp sau preview. FE không tự cộng 45 ngày từ lúc bấm mua.
6. Chạy .\mvnw.cmd clean test sau mọi thay đổi backend; bổ sung kiểm thử biên có Clock cố định, tích hợp schema PG/SQL Server phù hợp và E2E hiển thị/chặn ca. Chỉ chuyển checklist implementation sang hoàn thành khi code, migration, contract và kết quả kiểm thực tế khớp.

**Bộ ca nghiệm thu tối thiểu:**

- Paid 25/10/2026 18:30 +07:00 → hết hạn 09/12/2026 18:30 +07:00; callback lặp không dịch hạn/cấp credit thêm. Invoice hết cửa sổ 5 phút hoặc cần đối soát không tự kích hoạt gói.
- Ngay trước deadline còn quyền theo điều kiện khác; đúng deadline chặn quyền mới dù status DB chưa được cron đổi. Ca kết thúc đúng deadline hợp lệ về hạn, vượt dù một giây bị chặn; vẫn phải thuộc ca chuẩn.
- Tạo ca đúng 12 giờ trước bắt đầu được phép về notice; thiếu một giây bị chặn. Với ví dụ hạn trên: Ca 6 ngày cuối nằm trong hạn, Ca 7 vượt hạn; tạo Ca 6 sau 04:30 bị chặn bởi notice.
- FE cũ không được hiểu ngày cuối là cả ngày; FE mới hiển thị offset/giờ đúng, phân biệt hết hạn gói với hết 5 phút thanh toán. Lỗi API không được thay bằng dữ liệu giả hoặc tự coi thành công.
- Hai yêu cầu tranh cùng credit/Coach/phòng không cùng thành công; lỗi transaction không mất credit, không có Session mồ côi. Hoàn/dời lịch không dùng deadline ngày cũ để vượt timestamp mới.

Nhắc trực tiếp trong màn mua/trang gói/màn tạo-dời ca đã được Owner đồng ý. Thông báo chủ động định kỳ trước hạn vẫn cần chốt riêng ngưỡng/kênh, chưa thêm job email/SMS/Zalo. Các phần handoff này chưa phải lệnh triển khai trong giai đoạn phân tích.

## 12. D04/I.2 — người tạo lịch Advanced, xác nhận trao đổi và chống trừ kép

### 12.1. Mâu thuẫn cần Owner chọn trước

| Nguồn | Hành vi |
|---|---|
| AGENTS §2.4 | Member nạp 12 credit, tự book từng ca theo giờ rảnh Coach |
| API contract 6.3 và SessionController.bookPrivate | POST /api/sessions/book-private mở cho ROLE_MEMBER hoặc REGISTER_MEMBER hoặc MANAGE_CLASSES |
| analysis 2410.pdf I.2 | Sau trao đổi điện thoại/Zalo, Coach chính tạo ca và tick đã thống nhất với Member; lưu actor/thời điểm, thông báo Member |

Chốt mua theo môn/Coach tại D03 **chưa tự đổi người có quyền tạo ca**. Khác biệt này ảnh hưởng quyền, nút FE và kiểm người gọi API, không chỉ đổi nhãn màn hình. Chưa sửa AGENTS/controller hoặc cấp thêm MANAGE_CLASSES cho Coach.

### 12.2. Đối chiếu code hiện tại — không coi việc đã có lock là đủ

- PrivateBookingRequest nhận subscriptionId/coachId/roomId/ngày/giờ và classId hoặc subjectId; chưa có xác nhận đã thống nhất hoặc requestId chống lặp.
- bookPrivateSession đã có @Transactional, khóa center rồi findByIdForUpdate trên subscription, kiểm credit và xung đột Coach/phòng/Member, tạo Session và trừ một credit trong transaction. Kiểm chủ gói hiện dành cho người có role Member; chưa có ràng buộc người gọi là Primary Coach của subscription hoặc staff đúng center tại đây.
- Subscription chưa lưu Subject/Primary Coach (D03 sẽ bổ sung); hiện request có thể chọn Coach đúng chuyên môn nhưng chưa ràng buộc Coach chính đã mua. Không thể bảo vệ luồng Coach tạo bằng cách chỉ thêm hasRole('COACH') vào controller.
- Audit BOOK_PRIVATE_SESSION đang ghi userId của Member sở hữu gói thay vì currentUser thực sự thao tác. Nếu staff/Coach làm hộ thì actor bị ghi sai; cần tách actor và Member đích.
- Session và DTO chưa có dấu xác nhận trao đổi. Checkbox chỉ chứng minh người tạo khai đã trao đổi; không đồng nghĩa Member đã bấm duyệt hoặc đã có bằng chứng Member đồng ý độc lập.
- Request lặp chưa có idempotency cho booking. Lock chống tranh chấp không tự trả lại Session của lần thành công trước; không đồng nhất khóa DB với chống gửi lặp.
- SessionService chưa gọi NotificationService ở luồng book. NotificationService.send hiện dùng REQUIRES_NEW; cần thiết kế lưu/gửi thông báo gắn với kết quả commit để tránh thông báo một ca đã rollback. Đây là rủi ro khi tích hợp sau này, chưa có bằng chứng luồng book hiện gửi thông báo sai.
- POST /api/sessions generic tạo ca theo Class nhưng không gắn subscription/bookedMember hoặc trừ credit. Khi triển khai D04 phải rà mọi đường tạo/sửa/generate Advanced, bảo đảm ca học của Member không đi vòng qua luồng kiểm gói/credit. Không kết luận một ca mẫu trống hiện có tự động là đăng ký hợp lệ.

### 12.3. So sánh trước quyết định — Owner đã chọn A tại 12.4

| Phương án | Ưu/nhược điểm |
|---|---|
| A. Coach chính tạo sau thỏa thuận; Manager hỗ trợ có audit; Member xem/nhận thông tin | Gần I.2, trách nhiệm tạo ca rõ, ít trạng thái hơn quy trình Member gửi đề nghị rồi Coach duyệt. Đổi quyền tự book hiện hành; Member phụ thuộc Coach phản hồi. Quyền Manager hỗ trợ là đề xuất, không phải đã được chốt từ II.2 |
| B. Giữ Member tự tạo, Coach cũng được tạo trong phạm vi đăng ký mình phụ trách | Giữ tự phục vụ và giảm phụ thuộc Coach, nhưng phải sửa I.2 để diễn đạt rõ hai cách; không thể đồng thời nói mọi ca đều do Coach tạo sau trao đổi. Cần quy định có bắt buộc trao đổi khi Member tự đặt không |

**Đề xuất A cho hướng MVP đã mô tả trong PDF:** Member chọn sản phẩm/Coach trước Paid, sau Paid trao đổi lịch, Coach chính tạo ca; Manager cùng center hỗ trợ có lý do/audit. Không thêm một vòng Member duyệt từng ca trên hệ thống ở bước này. Coach tạo phải xác nhận đã thống nhất và lưu actor/time; Member nhận thông báo sau thành công. Việc checkbox có đủ làm dấu xác nhận là phần Owner cần chọn cùng mô hình, không tự gọi là consent điện tử của Member.

Chỉ dùng luồng transaction/credit chung sau khi kiểm người gọi và phạm vi; không nhân bản một service Coach tạo bỏ qua validation của luồng Member. Primary Coach khác Coach thay của riêng một buổi: quyền tạo cho người thay chưa tự được mở; xử lý thay Coach ở D06.

**Kết quả D04-a:** Owner trả lời “A”; quyết định và phạm vi handoff tại 12.4. Không hỏi lại quyền tạo hoặc tự thêm vòng Member duyệt từng ca.

Chỉ đọc source và cập nhật docs; chưa thay quyền/code/DB hoặc chạy tests backend. Các kiểm thử phân quyền, idempotency/rollback và thông báo khi commit sẽ được đặc tả sau khi D04-a chốt.

### 12.4. D04-a đã chốt A — phạm vi và handoff BE/FE

Owner trả lời **A** cho phương án và cơ chế xác nhận tại 12.3:

- Coach chính tạo ca của đăng ký mình phụ trách sau khi thống nhất với Member. Manager cùng center được hỗ trợ, có lý do và audit. Member xem/nhận thông báo; không còn tự tạo ca Advanced trong thiết kế mục tiêu.
- Người tạo bắt buộc xác nhận “Đã thống nhất lịch với Member”. BE lưu actor đăng nhập thực tế, Member đích, thời điểm theo Clock và dấu xác nhận. Không nhận actor/time tùy ý từ FE. Đây là người tạo khai đã trao đổi, không phải Member đã ký/duyệt trực tiếp; không thêm vòng Member bấm duyệt từng ca.
- Quyết định này chỉ chốt **tạo ca**; không tự xóa quyền Member hủy/báo vắng, không cấp quyền tự dời hoặc đổi Coach. Các quyền đó được tách D05–D07; Coach thay riêng buổi chưa tự có quyền tạo mọi ca của gói.
- BE kiểm người gọi đúng Primary Coach hoặc Manager được phép cùng center; suy Member/môn từ subscription đã trả tiền, không cấp MANAGE_CLASSES rộng để mở màn Coach. Mọi đường tạo/sửa/generate Advanced phải bảo vệ cùng quy tắc, không chỉ route book-private.
- Giữ đủ điều kiện D03 (1080 giờ, đúng môn, credit, notice 12 giờ, lịch làm thực/tài nguyên/Member/lễ/nắng), khóa/transaction và thứ tự khóa nhất quán. Tạo Session + giữ tài nguyên + trừ một credit + ghi audit cùng commit; điểm danh không trừ thêm lần nữa.
- Bổ sung idempotency: cùng mã yêu cầu và cùng nội dung trả cùng kết quả; mã đó với nội dung khác trả conflict. Không thay kiểm idempotency bằng kiểm trùng lịch, không trừ credit/gửi thông báo lần hai khi retry. Phạm vi mã và tên field/error được đặc tả kỹ thuật khi triển khai.
- Thông báo Member về ca đã tạo thành công, có ngày/giờ/Coach/sân; không thông báo ca rollback. Tận dụng notification hiện có với ràng buộc commit, không tự mở thêm kênh Zalo/email. Kiểm hiện trạng REQUIRES_NEW trước tích hợp để không tạo thông báo ngoài transaction nghiệp vụ đã thất bại.
- Handoff FE: Member bỏ nút tự tạo sau khi BE tương ứng được deploy; Coach/Manager có màn chọn đăng ký hợp lệ, xem ca khả dụng, xác nhận trao đổi; Manager nhập lý do hỗ trợ. Gửi idempotency key và xử lý retry/conflict; UI chỉ báo tạo thành công theo response BE. Endpoint/payload/quyền mới phải cập nhật vào API contract, không coi current contract 6.3 đã đổi bằng tài liệu này.
- Nghiệm thu khi triển khai: Member gọi trực tiếp API tạo bị chặn; Coach khác/khác center bị chặn; Primary Coach đúng/Manager hợp lệ tạo được; thiếu xác nhận hoặc Manager thiếu lý do bị chặn. Audit ghi đúng actor, hai yêu cầu tranh credit/ca không cùng thành công; retry không nhân đôi; rollback không mất credit/không có thông báo ca thất bại. Dùng Clock cố định kiểm notice/hạn và chạy .\mvnw.cmd clean test sau thay đổi backend.

Khi triển khai phải đồng bộ AGENTS §2.4 (câu Member tự book), API contract 6.3/controller/service/DTO và FE theo cùng phiên bản. Hiện chỉ ghi quyết định; chưa đổi AGENTS/code/quyền/DB hoặc chạy test ứng dụng. Không hỏi lại A, chuyển D05-a.

## 13. D05-a — đã chốt cutoff hủy Advanced 12 giờ

### 13.1. Mâu thuẫn và bằng chứng

- AGENTS §2.4, API contract 6.4 và SessionService.cancelPrivateSession hiện dùng **>=24 giờ** để hủy/hoàn một credit; dưới 24 giờ từ chối hủy thông thường.
- analysis 2410.pdf I.3/II.2 đề xuất **>=12 giờ** hủy/hoàn, dưới 12 giờ không hoàn; khác rule đang chạy. Quyết định D04 đổi người tạo không tự đổi cutoff này.
- Book hiện cho phép tạo ca trước đúng 12 giờ. Nếu giữ hủy 24 giờ thì ca được tạo trong khoảng còn 12 đến dưới 24 giờ sẽ **không có cửa sổ hủy miễn mất credit ngay từ lúc tạo**. Đây là hệ quả chính sách, không phải tính toán sai, nhưng phải công bố nếu giữ 24 giờ.
- Source hiện hoàn credit về subscription gốc dưới lock, kiểm trạng thái/hạn theo ngày; khi triển khai D03 phải đổi kiểm hạn sang timestamp. Không hoàn sang gói khác hoặc coi hủy đúng cutoff đồng nghĩa hoàn tiền.

### 13.2. So sánh trước quyết định — Owner chốt 12 giờ tại 13.3

| Lựa chọn | Đánh giá |
|---|---|
| 12 giờ | Đồng bộ minimum notice tạo ca đang dùng và đề xuất I.3; dễ giải thích. Coach/trung tâm có ít thời gian lấp ca trống hơn chính sách 24 giờ |
| Giữ 24 giờ | Bảo vệ thời gian sắp xếp Coach/sân tốt hơn; phải nêu rõ ca tạo khi còn dưới 24 giờ không được hủy hoàn credit thông thường. Không tự nâng notice đặt thành 24 giờ để che hệ quả |

**Đề xuất chọn 12 giờ cho hủy thông thường của Advanced**: còn đúng 12 giờ được hủy và hoàn đúng một credit vào đăng ký gốc; dưới 12 giờ không hủy hoàn thông thường, credit đã trừ khi đặt không hoàn và không trừ thêm lần nữa. Không tự thay đổi Normal. Ngoại lệ lỗi trung tâm/Manager được xử lý D06–D08, không đi vòng bằng cách sửa giờ hoặc trạng thái.

Chỉ xin chốt **cutoff hủy**, chưa cấp quyền dời cho Member hoặc suy ra mọi lần dời đều được hoàn/gia hạn. Quyền/thao tác dời và ngưỡng cho lịch cũ/lịch mới sẽ đối chiếu riêng D07. Báo vắng, Excused/Absent bất biến và sửa điểm danh vẫn tách D05-b/D15: source requestAbsence hiện yêu cầu Enrollment Registered, notice hai giờ và ghi Excused; chưa khớp đầy đủ Advanced book theo subscription hoặc đề xuất Absent tự động. Không sửa/gộp chính sách báo vắng vào lựa chọn cutoff này.

**Kết quả D05-a:** Owner trả lời “12h”; quyết định và handoff tại 13.3. Không hỏi lại ngưỡng hủy hoặc tự suy ra quyền dời/báo vắng đã được chốt.

### 13.3. D05-a đã chốt — hủy Advanced trước ít nhất 12 giờ

- Owner chọn **12 giờ** cho hủy thông thường Advanced. Còn đúng 12 giờ hoặc hơn tính tới giờ bắt đầu ca: hủy, giải phóng ca/Coach/sân và hoàn đúng một credit về subscription gốc. Dưới 12 giờ: không cho hủy hoàn credit thông thường; không trừ thêm credit đã trừ lúc tạo ca.
- Đây là hoàn quyền học, không phải hoàn tiền; không kéo dài hạn 1080 giờ khi hủy. Normal vẫn không hỗ trợ hủy/báo vắng. Ngoại lệ trung tâm và quyền bù sau hạn được xử lý riêng D06–D08; không tự dùng quyền ngoại lệ cho mọi ca.
- Quyết định D04 Coach tạo ca không tước quyền Member hủy ca của mình theo cutoff này. Chưa mở thêm quyền tự dời hoặc đổi Coach; nhân viên hỗ trợ phải có kiểm phạm vi/quyền riêng, không suy cứ có MANAGE_CLASSES là bỏ qua cutoff/center.
- Handoff BE: đổi kiểm 24 giờ trong cancelPrivateSession sang 12 giờ bằng Clock; kiểm Session/owner/center/subscription gốc, status hợp lệ và hạn timestamp theo D03. Trong transaction/lock nhất quán, hủy + hoàn + audit cùng commit; gửi lặp/hai lần đồng thời chỉ hoàn tối đa một credit, không vượt số credit gói hoặc làm mất dấu giao dịch.
- Handoff FE: dùng 12 giờ cho thông tin trước mua, nút hủy, thông báo lỗi và xác nhận tác động; BE vẫn quyết định. Đồng bộ AGENTS §2.4, contract 6.4 và code/test ở cùng đợt triển khai; hiện contract 6.4 còn là mô tả implementation 24 giờ, mục 14 là quyết định mục tiêu.
- Nghiệm thu cần Clock cố định: còn 12 giờ +1 giây / đúng 12 giờ được hủy về thời gian; 12 giờ -1 giây bị chặn. Kiểm retry/race chỉ hoàn một lần, sai chủ/khác center bị chặn, rollback không hoàn một nửa, không đổi Normal hoặc tự gia hạn. Chạy .\mvnw.cmd clean test sau thay đổi backend; giải trình test cũ kỳ vọng 24 giờ cần đổi vì quyết định Owner.

Chỉ cập nhật docs, chưa sửa code/AGENTS/DB hoặc chạy tests ứng dụng. D05-a hoàn tất chốt, chuyển D05-b.

## 14. D05-b — báo vắng có phải điểm danh Absent ngay và bất biến không?

### 14.1. Bằng chứng và vấn đề cần tách

- Phân tích I.3 đề xuất Member báo vắng dưới 12 giờ thì hệ thống ghi Absent ngay, không sửa lại. Owner chốt cutoff hủy 12 giờ **chưa đồng nghĩa chốt cách ghi điểm danh này**.
- AttendanceService.requestAbsence hiện chặn Normal, yêu cầu Enrollment Registered ở Class, notice ít nhất **hai giờ**, ghi Attendance.state=Excused. Chưa dựa trực tiếp vào Session.bookedMember/subscription của Advanced; không thể chỉ đổi thông báo FE mà bảo đảm chủ ca đúng.
- TrainingResultService và AttendanceService.correctAttendance có đường ghi/sửa attendance, không thể tuyên bố Absent bất biến chỉ bằng Member không có nút sửa. Chính sách cửa sổ/chỉnh sai cần đối chiếu riêng D15.
- AttendanceFinalizationProcessor sau giờ kết thúc chỉ tự thêm Absent khi chưa có Attendance. Vì vậy, nếu báo vắng tạo sẵn một Attendance Excused thì job hiện không tự chuyển dòng đó sang Absent. Phải thiết kế rõ bước chốt, không giả định job đang làm điều này.
- Credit đã trừ khi tạo ca; báo vắng sát giờ/no-show không hoàn và không trừ thêm. Việc xác định thù lao/lương không được tự suy ra từ chọn nhãn Excused/Absent trong bước này.

### 14.2. So sánh trước quyết định — Owner chọn A tại 14.3

**Đề xuất A:** lưu “Đã báo vắng” là thông tin riêng của ca, có người báo/thời điểm/lý do, không lập tức chốt attendance Absent bất biến. Với luồng dưới 12 giờ đang thảo luận: cho Member báo trong khoảng **0 < thời gian còn lại <12 giờ**; từ đúng 12 giờ hướng tới hủy đúng hạn nếu không học; từ giờ bắt đầu trở đi xử lý điểm danh, không báo trước hồi tố. Đây là đề xuất thay giới hạn hai giờ hiện tại, chưa triển khai.

- Giữ Session Scheduled, tài nguyên và credit đã giữ; không giải phóng ca như hủy hoàn. Coach thấy dấu báo vắng; nếu Member vẫn đến thì điểm danh Present theo thực tế, nếu không đến thì ghi/chốt Absent theo cửa sổ điểm danh. Không tự khôi phục ca đã Cancelled bằng thao tác điểm danh.
- Lưu dữ kiện báo vắng tách attendance giúp không ghi sai khi Member đổi kế hoạch hoặc bấm nhầm. Không cần thêm một vòng duyệt yêu cầu báo vắng; tên field và cách lưu cụ thể thiết kế khi triển khai.
- Chỉ cho chủ ca báo, kiểm gắn Session.bookedMember/subscription và trạng thái; không cho báo ca của người khác hoặc Normal. Tranh chấp với cancel/reschedule/finalize cần cùng quy tắc khóa/transaction và audit. Chi tiết sửa điểm danh sau chốt, dời ngoại lệ và xử lý lỗi trung tâm vẫn ở D15/D06–D08.

**Phương án B theo I.3:** báo vắng là cam kết bỏ buổi, tự ghi Absent ngay và không sửa lại theo luồng thông thường. Ít bước chốt hơn nhưng có thể sai thực tế nếu Member vẫn đến; cần quy định riêng khả năng sửa sai và thời điểm chốt, không tự cấm mọi correction hiện có khi chưa được duyệt.

**Kết quả D05-b:** Owner trả lời “A”; quyết định và handoff tại 14.3. Không hỏi lại việc tách báo vắng khỏi kết quả điểm danh.

### 14.3. D05-b đã chốt A — báo vắng riêng, điểm danh theo thực tế

- Member chỉ báo ca Advanced của mình khi **0 < thời gian tới giờ bắt đầu <12 giờ**. Từ đúng 12 giờ có luồng hủy đúng hạn; từ giờ bắt đầu trở đi không ghi báo trước hồi tố. Normal không hỗ trợ báo vắng.
- Lưu riêng người báo, thời điểm theo Clock và lý do; giữ Session Scheduled, credit đã trừ và tài nguyên đang giữ. Không hoàn hoặc trừ thêm credit, không tự chốt Absent bất biến. Dấu báo vắng không phải Cancelled/Completed, không tự đổi Primary Coach hoặc hạn gói.
- Coach xem dấu báo vắng và điểm danh theo thực tế: Member vẫn đến thì Present, không đến thì Absent theo quy trình chốt. Nếu chưa có điểm danh sau buổi, job chốt theo điều kiện hiện hành cần được đồng bộ; báo vắng riêng không được khiến job bỏ qua việc ghi kết quả. Chính sách sửa điểm danh sau chốt vẫn xét D15, không mở quyền Member tự sửa attendance.
- Handoff BE: kiểm chủ ca qua bookedMember/subscription, trạng thái ca và cùng center; thay luồng kiểm Enrollment/notice hai giờ/ghi Excused của requestAbsence. Cách lưu field/bảng chốt khi thiết kế migration, không tạo một Attendance giả chỉ để gắn nhãn báo trước. Audit ghi đúng actor/time; đồng bộ khóa với cancel/reschedule/finalize, gửi lặp không sinh dữ liệu trùng hoặc ảnh hưởng credit.
- Handoff FE: nút “Báo vắng” riêng với “Hủy ca”; trước gửi nêu rõ không hoàn credit, lịch vẫn được giữ. Hiển thị “Đã báo vắng” tách Present/Absent, Coach xem được lý do/thời điểm. Không dùng màu/nhãn Absent như kết quả cuối chỉ từ thao tác báo trước; không tự mở thêm kênh thông báo hay màn duyệt yêu cầu.
- Nghiệm thu khi triển khai: đúng 12 giờ không dùng luồng báo sát giờ; dưới 12 giờ và trước bắt đầu được báo; đúng/sau bắt đầu bị chặn. Sai chủ/Normal/ca Cancelled hoặc Completed bị chặn; báo rồi vẫn học được ghi Present; không đến được chốt Absent, không hoàn/trừ kép. Kiểm race với hủy/dời/chốt và các API attendance hiện có; chạy .\mvnw.cmd clean test sau sửa backend.

Chỉ ghi quyết định và handoff, chưa đổi schema/code/quyền/DB hoặc chạy tests ứng dụng. D05-b đã chốt; chuyển D06, không tự quyết định chính sách dời hoặc sửa điểm danh.

## 15. D06 — ba phương án cho sự cố phía trung tâm (Advanced)

### 15.1. Phần đã chốt và cách kết hợp I.4 với II

Owner đã yêu cầu làm **đủ ba phương án**, không hỏi lại lựa chọn đó. Ba phương án là kết quả xử lý cho **nguyên nhân phía trung tâm/Coach**; không đồng nhất với ba nguyên nhân ở II (trung tâm, Member báo sớm, Member báo sát giờ).

| Kết quả khi Member đồng ý | Tác động mục tiêu trong phân tích |
|---|---|
| Dời ca | Giải phóng tài nguyên cũ và giữ ca mới trong cùng transaction, giữ nguyên phần credit đã trừ cho buổi đó; không trừ thêm |
| Coach thay riêng buổi | Giữ ngày/giờ, đổi Coach phù hợp/không trùng lịch; không đổi Primary Coach của subscription, không đổi credit |
| Hủy do trung tâm | Nếu không thống nhất dời/thay, hủy và hoàn một quyền học cho Member, kể cả còn dưới 12 giờ; không dùng cutoff Member để từ chối xử lý lỗi trung tâm |

Phân tích I.4/II.1 đề xuất Coach báo/trao đổi, Manager chốt thao tác ngoại lệ và lưu lý do. Không cho Coach tự phân loại mọi lần hủy/dời thành lỗi trung tâm để bỏ cutoff. Xử lý quyền học khi đã/sắp hết hạn thuộc D08, không coi hoàn một số dư đã hết hạn là hoàn thành nghĩa vụ. Bảng này chưa quyết định lịch mới dưới notice 12 giờ hoặc sửa lịch đã bắt đầu/kết thúc; không suy sự cố được bỏ mọi validation. Không mở hủy Normal từ bảng Advanced này.

### 15.2. Bằng chứng implementation và thiếu sót

- Đã có POST /api/sessions/{sessionId}/reschedule và /substitute-coach, hiện dùng MANAGE_CLASSES; không đồng nghĩa đã khóa nghiệp vụ cho đúng Manager/center hoặc đã có Member đồng ý. Không cấp authority này rộng hơn để vượt phần kiểm còn thiếu.
- SessionRescheduleRequest có lịch mới/phòng/lý do/extendValidityDays; SubstituteCoachRequest có Coach mới/lý do. Chưa có dữ liệu ghi nhận Member đồng ý với phương án cụ thể trong hai DTO.
- substituteCoach đã khóa, kiểm chuyên môn/Active/center của Coach theo validator và xung đột, đổi Session.coach, lưu audit. Chưa có Primary Coach trên subscription để bảo đảm độc lập lâu dài (phụ thuộc D03); chưa có xác nhận đồng ý từ Member ở luồng này.
- rescheduleSession đã kiểm ca/phòng/Coach/Member/lễ và lưu lịch trước/sau; hiện có tự kéo endDate toàn gói khi dời vượt hạn, khác hướng không tự gia hạn toàn gói trong I.5. Ghi rủi ro để xử lý riêng D08, không công nhận đã giải quyết hạn 1080 giờ.
- cancel-private là hủy thông thường, đang chặn dưới 24 giờ ở source; quyết định mới D05-a là 12 giờ. Luồng này chưa thay thế được thao tác hủy do trung tâm bất kể cutoff; không tái dùng bằng cách bỏ điều kiện thời gian cho mọi người gọi.
- requireMutableSession chặn sau giờ kết thúc/finalized, chưa tự diễn đạt thành chặn ngay từ giờ bắt đầu. Các ca đang diễn ra hoặc đã chốt cần được bóc tách khi đến ngoại lệ, không tự sửa lịch sử trong D06-a.

### 15.3. D06-a — so sánh trước quyết định, Owner chọn A tại 15.4

Tài liệu yêu cầu Member đồng ý phương án dời/thay, nhưng chưa chọn **ai ghi nhận sự đồng ý trên hệ thống**. Quyết định D04 checkbox khi tạo ca mới không tự cấp quyền Manager xác nhận thay Member cho mọi xử lý sự cố.

| Lựa chọn | Thiết kế và đánh đổi |
|---|---|
| A. Manager xác nhận đã trao đổi và Member đồng ý | Manager chọn phương án sau trao đổi, ghi lý do và dấu xác nhận; lưu actor/time và lịch/Coach cũ-mới, thông báo kết quả cho Member. Gần quy trình trao đổi bên ngoài đã chọn, ít trạng thái cho MVP. Là lời xác nhận của Manager, không phải Member trực tiếp duyệt |
| B. Member bấm đồng ý phương án trên hệ thống | Manager gửi đề xuất, Member duyệt rồi mới cập nhật ca; bằng chứng trực tiếp rõ hơn. Thêm trạng thái chờ, từ chối/hết hạn đề xuất, tái kiểm tài nguyên và xử lý khi không phản hồi. Không thể coi thời gian im lặng là đồng ý |

**Đề xuất A cho MVP**, nhất quán cách tạo ca sau trao đổi. Dữ liệu xác nhận phải gắn đúng Session và đúng phương án/lịch/Coach đã thống nhất; không tick một lần để áp cho mọi thay đổi sau. Hủy do trung tâm ghi rõ sự cố và kết quả không thống nhất dời/thay, không giả ghi Member đồng ý học với người khác. Giữ quyền quyết định của Member trong vận hành, không ép dời/thay khi Member từ chối.

**Kết quả D06-a:** Owner trả lời “A”; lưu quyết định/handoff tại 15.4. Không thêm vòng Member trực tiếp duyệt hoặc hỏi lại cơ chế xác nhận này.

### 15.4. D06-a đã chốt A — Manager ghi nhận Member đồng ý xử lý sự cố

- Sau trao đổi, Manager cùng center chốt phương án xử lý; Coach báo sự cố/trao đổi, không tự phân loại lỗi để bỏ quy chế. Với dời/thay Coach, Manager bắt buộc xác nhận Member đã đồng ý đúng phương án cụ thể, kèm lý do sự cố, actor và thời điểm BE. Đây là lời xác nhận của Manager, không phải Member đã bấm duyệt trực tiếp.
- Phạm vi xác nhận gắn Session, lịch/Coach/phòng cũ-mới và phương án đã thống nhất. Đổi phương án hoặc đổi ca/Coach khác cần xác nhận lại; không tái dùng một checkbox cho mọi thay đổi sau. Không dùng im lặng hoặc không đọc thông báo làm Member đồng ý.
- Nếu không thống nhất dời/thay và chọn hủy do trung tâm, lưu lý do và kết quả trao đổi tương ứng; không buộc giả xác nhận Member đã đồng ý một ca/Coach mới. Quyền học được bảo toàn theo nhánh hủy/hoàn, cách dùng quyền khi quá hạn chờ D08.
- Handoff BE: kiểm vai trò/quyền nghiệp vụ Manager và center, không chỉ tin MANAGE_CLASSES hoặc actor/consentTime client gửi. Thiết kế dữ liệu xác nhận và DTO cho từng phương án; transaction/lock bảo đảm đổi ca/Coach hoặc hủy-hoàn cùng audit nhất quán, retry không hoàn nhiều credit. Chưa chọn tên endpoint/field/error mới.
- Thông báo Member kết quả sau commit, nêu phương án và ca/Coach cũ-mới hoặc quyền học đã hoàn; không coi gửi thành công là bằng chứng Member đã đồng ý trước đó. Không tự thêm kênh email/SMS/Zalo hoặc trạng thái chờ Member duyệt.
- Handoff FE: màn Manager hiển thị tác động trước/sau, lý do bắt buộc và xác nhận đã trao đổi; nhánh hủy trình bày đúng kết quả trao đổi thay vì checkbox đồng ý lịch mới. Member xem lịch/kết quả cập nhật và nhận thông báo. Chỉ mở tính năng khi BE tương ứng đã triển khai, không suy route hiện có đã đủ validation.
- Nghiệm thu khi triển khai: thiếu xác nhận dời/thay/lý do bị chặn, sai role/center bị chặn; audit đúng người/thời điểm/phương án, không đổi Primary Coach khi thay riêng buổi. Race/conflict phải rollback tài nguyên/credit/audit nghiệp vụ cùng nhau, không thông báo ca thất bại; retry không nhân đôi xử lý. Chạy .\mvnw.cmd clean test khi sửa backend.

Quyết định A không tự cho phép bỏ notice 12 giờ, kéo hạn toàn gói, sửa lịch đã diễn ra hoặc miễn mọi kiểm tra sân/Coach/lễ/nắng. Chỉ docs, chưa đổi code/DB/quyền hoặc chạy tests ứng dụng. Chuyển D06-b riêng về notice khi dời do trung tâm.

### 15.5. D06-b — dời do trung tâm có được chọn ca mới trong vòng 12 giờ?

**Thiếu quy định:** notice tạo ca mới đã chốt >=12 giờ; I.4/II.1 cho xử lý sự cố sát giờ nhưng chưa nói rõ mốc này áp dụng thế nào cho **ca mới khi dời**. Hai khoảng thời gian khác nhau: thời gian còn tới ca cũ (phân loại yêu cầu) và thời gian còn tới ca mới (điều kiện bố trí). Source reschedule hiện không kiểm đủ notice 12 giờ cho ca mới, không thể coi việc API cho qua là Owner đã duyệt ngoại lệ.

| Phương án | Tác động |
|---|---|
| A. Ngoại lệ có kiểm soát cho sự cố trung tâm | Manager có thể dời sang ca mới còn dưới 12 giờ sau khi Member đồng ý; cứu được buổi trong ngày. Phải xác minh lỗi trung tâm, ghi lý do/audit và vẫn kiểm tài nguyên, giờ tương lai, ca chuẩn, hạn học |
| B. Luôn yêu cầu ca mới cách ít nhất 12 giờ | Rule đơn giản và chuẩn bị dễ hơn, nhưng sự cố sát giờ thường phải dời sang ngày khác dù cả hai bên có thể học ca kế tiếp |

**Đề xuất A:** chỉ Manager xử lý lỗi trung tâm được dùng ngoại lệ, với ca cũ chưa bắt đầu và ca mới cũng chưa bắt đầu; không đặt lùi về quá khứ. Member đồng ý theo cơ chế 15.4, Coach/phòng sẵn sàng, đúng môn/ca chuẩn/lễ/nắng và còn quyền học hợp lệ (quyền bù vượt hạn chốt D08). Giữ phần credit đã trừ, không trừ thêm hoặc tự gia hạn từ thao tác này. Việc thay Coach giữ nguyên ca cũng không bị hiểu thành đặt mới phải chờ thêm 12 giờ; trường hợp ca đã bắt đầu/đã kết thúc tách xử lý riêng, chưa mở ở lựa chọn này.

Ví dụ môn trong nhà: lúc 08:30 báo sự cố cho ca 09:30–11:00; Member/Coach thống nhất chuyển sang 13:00–14:30 cùng ngày. Ca mới chỉ cách 4 giờ 30 phút, nên cần ngoại lệ có kiểm soát nếu muốn giữ buổi trong ngày. Đây chỉ là ví dụ điều kiện thời gian, còn phải có sân/Coach và hạn hợp lệ.

Đặt ca mới thông thường vẫn >=12 giờ; dời do Member báo sớm/sát giờ ở D07 chưa được hưởng ngoại lệ lỗi trung tâm. Quy tắc này không thay cutoff hủy Member đã chốt và không mở hủy Normal.

**Kết quả D06-b:** Owner chọn A; quyết định và handoff tại 15.6. Không hỏi lại ngoại lệ notice cho lỗi trung tâm.

### 15.6. D06-b đã chốt A — ngoại lệ notice ca mới do lỗi trung tâm

- Manager cùng center được dời ca do lỗi trung tâm sang ca mới còn dưới 12 giờ, với điều kiện **ca cũ và ca mới đều chưa bắt đầu**, Member đã đồng ý theo 15.4 và ca mới hợp lệ về môn/Coach/phòng/Member/lễ/nắng/ca chuẩn/quyền học. Không đặt lùi quá khứ hoặc sửa buổi đang diễn ra/đã chốt theo ngoại lệ này.
- Giữ credit đã dùng cho ca, không trừ/hoàn thêm hoặc tự kéo hạn 1080 giờ. Nếu cần quyền bù vượt hạn thì phải theo D08 sau khi chốt; không lấy quyết định này làm quyền gia hạn toàn gói.
- Handoff BE: phân biệt nguyên nhân/phạm vi/quyền Manager trên server; không để client chỉ gửi cờ “sự cố” rồi bỏ notice. Kiểm thời gian, trạng thái và tài nguyên lại trong transaction/lock, đổi lịch + audit nhất quán; ghi rõ căn cứ ngoại lệ, thời gian tới ca cũ/mới và xác nhận Member. Các luồng tạo ca mới hoặc dời do Member không tự hưởng ngoại lệ này.
- Handoff FE: chỉ luồng Manager xử lý lỗi trung tâm có lựa chọn ca mới dưới 12 giờ; hiển thị trước/sau và xác nhận trao đổi, lấy lỗi/điều kiện từ BE. Không nới validation toàn bộ màn lịch. Thông báo kết quả sau thành công theo cơ chế 15.4.
- Nghiệm thu khi triển khai: Manager xử lý đúng sự cố/center, ca mới dưới 12 giờ được chấp nhận khi mọi điều kiện đủ; sai quyền/nguyên nhân/center hoặc thiếu xác nhận bị chặn. Đúng giờ bắt đầu ca cũ hoặc ca mới không còn thuộc ngoại lệ; ca trùng lịch, nghỉ lễ, ngoài giờ chuẩn hoặc vượt quyền học bị chặn. Retry/race không đổi credit hoặc tài nguyên hai lần; lỗi rollback giữ nguyên lịch cũ. Chạy .\mvnw.cmd clean test sau sửa backend.

Đặt ca mới thông thường vẫn >=12 giờ; không đổi cutoff hủy/báo vắng hoặc Normal. Chỉ ghi docs, chưa sửa implementation/DB/tests ứng dụng. Chuyển D07-a: Member xin dời sớm.

## 16. D07-a — Member xin dời sớm: ai thao tác và điều kiện ca mới?

### 16.1. Điểm chưa được chốt

- II.2 nêu Coach chính **hoặc** Member tự thao tác **hoặc** Manager hỗ trợ, chưa chọn một mô hình. D04 đã chốt Coach tạo mới nhưng không tự quyết định quyền dời; D05-a mới chốt hủy >=12 giờ. Cần chốt riêng, không coi quyền tạo bao trùm mọi sửa lịch.
- Source reschedule chỉ yêu cầu MANAGE_CLASSES, chưa phân luồng Member báo sớm/lỗi trung tâm và chưa áp notice 12 giờ cho ca mới. Còn tự gia hạn gói khi dời vượt hạn; không phù hợp để dùng nguyên trạng cho dời thường (xem D08).
- Cần phân biệt: còn >=12 giờ tới **ca cũ** mới là yêu cầu dời sớm; **ca mới** cũng cần đủ thời gian chuẩn bị. Ngoại lệ D06-b chỉ cho lỗi trung tâm, không tự áp cho yêu cầu Member.

### 16.2. So sánh trước quyết định — Owner chọn A tại 16.3

1. Member trao đổi bên ngoài với Coach chính để thống nhất ca mới; Coach chính thực hiện, Manager cùng center được hỗ trợ có lý do/audit. Không thêm màn Member tự dời hoặc hàng đợi yêu cầu/duyệt mới trong MVP.
2. Tại thời điểm BE xử lý: còn **>=12 giờ tới ca cũ**, và ca mới bắt đầu **>=12 giờ từ hiện tại**. Nếu chỉ báo bằng Zalo trước cutoff nhưng thao tác sau cutoff thì không tự coi là dời sớm; trường hợp sát giờ xét D07-b, chưa tự nhận thời gian do client khai làm căn cứ bỏ cutoff.
3. Ca mới kết thúc không vượt hạn 1080 giờ của đăng ký; không tự gia hạn. Kiểm đủ Coach/phòng/Member, chuyên môn, ca chuẩn, ngày nghỉ/nắng như luồng tạo. Giữ Coach hiện tại hợp lệ cho buổi; yêu cầu thay Coach không bị gộp ngầm vào thao tác dời giờ.
4. Người thực hiện xác nhận đã thống nhất ca mới; lưu actor/time, lý do và lịch cũ/mới. Chuyển tài nguyên trong cùng transaction, giữ nguyên credit đã trừ; thất bại giữ lịch cũ. Member nhận thông báo sau thành công, không cần vòng duyệt từng ca.

**B — phương án khác:** cho Member tự dời sớm trên giao diện. Có lợi cho tự phục vụ, nhưng cần định nghĩa việc thống nhất với Coach, quyền sửa Session và UX khác luồng Coach thao tác đã chọn; không chỉ mở quyền controller rồi coi đủ quy trình. Không khẳng định B mâu thuẫn tuyệt đối với D04, vì tạo và dời là hai quyền riêng.

**Đề xuất chọn A cùng hai điều kiện 12 giờ nêu trên** để nhất quán MVP trao đổi với Coach, không thêm workflow hoặc ngoại lệ dời sát giờ vào bước này. Chưa chốt ai được dời khi Member báo dưới 12 giờ, ca đang diễn ra/đã chốt, xử lý dấu báo vắng sau dời hoặc quyền bù vượt hạn; ghi riêng cho D07-b/D15/D08 khi đến lượt.

**Kết quả D07-a:** Owner chọn A và yêu cầu tích hợp UI/UX thuận tiện với phần tạo ca. Quyết định/handoff tại 16.3–16.4; không mở Member tự dời hoặc hỏi lại người thao tác.

### 16.3. D07-a đã chốt A — dời sớm do Member

- Member trao đổi với Coach chính; Coach chính thực hiện dời, Manager cùng center hỗ trợ có lý do/audit. Không thêm luồng Member tự dời hay hàng đợi yêu cầu/duyệt trong MVP.
- Khi BE xử lý, ca cũ và ca mới đều bắt đầu sau ít nhất 12 giờ; ca mới kết thúc không vượt hạn 1080 giờ của đăng ký. Thỏa thuận bên ngoài trước cutoff không tự thay thế thời điểm kiểm ở BE; xử lý muộn hơn phải theo chính sách D07-b sau khi chốt.
- Giữ nguyên credit đã trừ và Coach hợp lệ của buổi, không tự gia hạn hoặc đổi Coach ngầm. Kiểm đủ tài nguyên/Member/ca chuẩn/lễ/nắng; chuyển lịch và audit trong transaction/lock. Lỗi giữ nguyên lịch cũ; không tạo ca mới/trừ credit rồi hủy ca cũ như hai request rời.
- Xác nhận đã thống nhất ca mới, ghi actor/time/lý do/lịch cũ-mới; thông báo Member sau commit. BE kiểm đúng Primary Coach hoặc Manager cùng center, không tin người gọi tự gửi actor/quyền/nguyên nhân.
- Nghiệm thu BE khi triển khai: biên đúng 12 giờ của cả ca cũ và ca mới, sai chủ/Coach/center, ca kết thúc vượt hạn, trùng tài nguyên và retry/race; thành công không thay credit/hạn, thất bại không mất lịch cũ. Chạy .\mvnw.cmd clean test sau thay đổi backend.

### 16.4. Handoff UI/UX chung tạo ca và dời ca — theo yêu cầu Owner

Owner yêu cầu thao tác dời thuận tiện và tích hợp với tạo ca trước đó. Thiết kế mục tiêu dùng chung phần chọn ngày/ca/phòng và hiển thị điều kiện, có chế độ **Tạo ca** / **Dời ca** rõ ràng; có thể dùng chung component nhưng không gộp hai nghiệp vụ thành một API tùy ý.

| Điểm thao tác | Yêu cầu bàn giao FE/BE |
|---|---|
| Vào màn | Từ đăng ký chọn “Tạo ca”; từ lịch/chi tiết buổi chọn “Dời ca”. Mở trực tiếp form/ngăn chi tiết dùng chung, không bắt tìm lại Member/gói |
| Dữ liệu điền sẵn | Dời ca nạp Session, Member, môn, Coach, subscription và giờ hết hạn. Môn/đăng ký không đổi bằng sửa field; BE lấy từ Session gốc để kiểm, không tin field khóa ở FE |
| Chọn ca | Cùng lịch 7 ca, phòng phù hợp và lý do ca bị khóa; hiển thị hạn chính xác. Với dời, BE loại chính Session đang sửa khỏi kiểm trùng và vẫn xét mọi lịch khác; không biến slot đang chiếm của chính buổi thành xung đột giả |
| Trước xác nhận | Hiển thị lịch cũ → lịch mới, sân, Coach, hạn và tác động: tạo ca trừ 1 credit; dời giữ nguyên credit/hạn. Chưa đổi hoặc giải phóng lịch cũ khi chỉ preview |
| Xác nhận | Dùng mẫu xác nhận đã thống nhất với Member; Manager hỗ trợ có lý do. Nút ghi đúng “Tạo ca” hoặc “Xác nhận dời ca”, không gọi thao tác dời là tạo lại |
| Quyền/nguyên nhân | Member không có nút tự tạo/dời; Coach chỉ thao tác đúng phạm vi. Luồng Manager xử lý lỗi trung tâm có nhãn riêng và ngoại lệ D06-b, không nới toàn bộ bộ chọn ca dưới 12 giờ cho dời thường |
| Lưu/thất bại | Chỉ cập nhật lịch sau BE thành công; khóa gửi khi đang lưu, chống retry ở BE. Xung đột sau preview: giữ dữ liệu người dùng nhập, báo lý do và tải lại ca khả dụng; lịch cũ còn nguyên nếu dời thất bại |

Handoff BE cần response đủ thông tin lịch hiện tại/đăng ký/hạn và lịch khả dụng theo ngữ cảnh tạo/dời, không buộc FE tự suy hết các rule. Tên endpoint/DTO/version chốt khi triển khai, quyền BE là chốt chặn. Handoff FE ghi rõ phần dùng chung, chế độ tạo/dời và ca E2E credit trước-sau/thất bại/biên cutoff; không giả lập thành công khi API lỗi.

Ngoại lệ Member báo sát giờ đã chốt tại 17.3. Cách xử lý dấu báo vắng của lịch cũ khi dời còn được tách ở mục 18 để không ngầm quyết định bằng thiết kế UI. Chỉ cập nhật docs, chưa code/DB/test ứng dụng.

## 17. D07-b — Member xin dời khi ca cũ còn dưới 12 giờ

### 17.1. Phạm vi còn mở

II.3 đề xuất chặn dời thông thường, chỉ Manager duyệt ngoại lệ bất khả kháng (ví dụ tai nạn/cấp cứu). D07-a đã chốt dời sớm và D06-b cho ngoại lệ **lỗi trung tâm**, chưa tự áp hai quyết định đó cho Member xin dời sát giờ.

### 17.2. Đề xuất trước quyết định — Owner đã chọn A tại 17.3

- Khi **0 < thời gian tới ca cũ <12 giờ**, Member/Coach không được dùng dời thường. Manager cùng center có thể thẩm định lý do bất khả kháng, ghi lý do duyệt và xác nhận ca mới đã thống nhất; đây là lỗi/hoàn cảnh phía Member, không sửa nhãn thành lỗi trung tâm.
- **Ca mới vẫn phải bắt đầu >=12 giờ từ lúc xử lý**, kết thúc trong hạn gói, đủ tài nguyên/Coach/Member/ca chuẩn/lễ/nắng; không kế thừa ngoại lệ ca mới dưới 12 giờ của D06-b. Ca cũ chưa bắt đầu; không hồi tố sau giờ bắt đầu hoặc sửa kết quả đã chốt ở bước này.
- Giữ credit đã trừ cho buổi, không trừ thêm/hoàn credit/tự gia hạn gói. Manager dời qua cùng UI chọn ca ở 16.4 với chế độ ngoại lệ rõ, audit nguyên nhân/actor/time/lịch cũ-mới; Member nhận thông báo sau commit.
- Nếu không duyệt hoặc không có ca mới hợp lệ: lịch cũ chưa thay đổi; Member có thể dùng báo vắng trước giờ bắt đầu theo D05-b. Báo vắng không tự chốt Absent; điểm danh thực tế vẫn được giữ nguyên nguyên tắc.

**B — không có ngoại lệ dời sát giờ do Member:** dưới 12 giờ chỉ còn báo vắng trước giờ bắt đầu; đơn giản hơn nhưng không xử lý mềm trường hợp bất khả kháng như II.3 đề xuất.

**Đề xuất A**, giữ sự khác biệt với lỗi trung tâm và không thêm workflow Member gửi/duyệt trực tuyến. Quyền bù/gia hạn khi hết hạn còn D08; dấu báo vắng lịch cũ sau dời cần quyết định riêng trước bàn giao cuối. Không tự cấp mọi Manager quyền sửa ca đã diễn ra.

**Kết quả D07-b:** Owner chọn A; quyết định/handoff tại 17.3. Không hỏi lại có cho ngoại lệ hay không.

### 17.3. D07-b đã chốt A — Manager duyệt dời sát giờ do bất khả kháng

- Với ca cũ còn **trên 0 và dưới 12 giờ**, Member/Coach không tự dời. Chỉ Manager cùng center thẩm định và duyệt bất khả kháng, ghi lý do và xác nhận ca mới đã thống nhất. Không dùng nhãn sự cố trung tâm để hợp thức hóa yêu cầu của Member.
- Ca mới bắt đầu **>=12 giờ từ lúc xử lý**, kết thúc trong hạn gói và đủ các điều kiện Coach/sân/Member/môn/ca chuẩn/ngày nghỉ/nắng. Không tự gia hạn hoặc áp ngoại lệ ca mới dưới 12 giờ của D06-b; đúng/sau giờ bắt đầu ca cũ không nằm trong lựa chọn này.
- Giữ nguyên credit đã trừ cho buổi, đổi tài nguyên/lịch/audit cùng transaction và lock; lỗi giữ lịch cũ, retry không tạo ca/trừ credit thêm. Không biến thành hủy-hoàn rồi tạo mới qua hai request rời.
- Nếu không duyệt hoặc không có ca hợp lệ, giữ lịch cũ; Member có thể báo vắng trước giờ bắt đầu theo D05-b. Việc Manager chưa duyệt không tự chuyển Session thành Cancelled/Absent, không mặc định có quyền bù sau hạn.
- Handoff BE: tách policy dời sớm, Member bất khả kháng sát giờ và lỗi trung tâm; kiểm quyền/center/nguyên nhân trên server, lưu actor/time/lý do/ca cũ-mới và xác nhận trao đổi. Lý do client không tự cấp quyền vượt cutoff; API/error thực tế bổ sung khi triển khai.
- Handoff FE: dùng chung màn dời 16.4, chỉ Manager có chế độ “Ngoại lệ bất khả kháng của Member”; bắt nhập lý do/xác nhận, giữ điều kiện >=12 giờ của ca mới. Hiển thị đúng tác động không đổi credit/hạn, thông báo kết quả cho Member sau commit.
- Nghiệm thu khi triển khai: sai role/center hoặc thiếu lý do/xác nhận bị chặn; ca cũ còn dưới 12 giờ và chưa bắt đầu được xử lý nếu ca mới hợp lệ. Kiểm biên đúng giờ bắt đầu ca cũ, đúng/thiếu một giây notice ca mới, hạn timestamp, race/rollback/idempotency, không tự gia hạn hoặc giảm credit thêm. Chạy .\mvnw.cmd clean test khi sửa backend.

Chỉ docs, chưa sửa code/quyền/schema/DB hoặc chạy tests ứng dụng. Chuyển D07-c về dấu báo vắng khi đổi lịch, rồi mới xử lý quyền bù/gia hạn D08.

## 18. D07-c — dấu báo vắng của lịch cũ sau khi dời ca

### 18.1. Điểm cần quyết định

D05-b đã chọn báo vắng riêng theo ca, không phải kết quả điểm danh. Khi dời vẫn giữ Session ID và thay ngày/giờ như source hiện tại, nếu chỉ giữ một cờ báo vắng trên Session thì ca mới có thể hiển thị “Đã báo vắng” dù Member vừa đồng ý lịch mới. Nếu xóa cả dữ kiện thì mất lịch sử báo trước; chưa có quyết định nào cho phép một trong hai cách này.

Đây là thiết kế cần bổ sung cho tính năng báo vắng mới, không phải khẳng định source đang có cờ đó hoặc lỗi live này đã được tái hiện. Source reschedule hiện đổi ngày/giờ trên cùng Session và ghi audit lịch cũ-mới; dữ liệu báo vắng riêng chưa triển khai.

### 18.2. Đề xuất A — gắn báo vắng với lịch đã báo, giữ lịch sử

- Khi **dời thành công làm đổi ngày/giờ**, lưu báo vắng cũ trong lịch sử gắn lịch cũ, không còn coi đó là báo vắng đang hiệu lực cho lịch mới. Member đã thống nhất lịch mới; nếu không học được lịch mới thì báo lại theo cửa sổ D05-b.
- Không xóa audit/lý do/người/giờ báo cũ, không sửa thành Present hoặc Absent chỉ vì dời. Ca mới vẫn Scheduled và chưa có kết quả attendance cho buổi chưa diễn ra; không dùng thao tác này để xóa/sửa điểm danh thật của lịch đã chốt.
- Chỉ đổi sân hoặc Coach mà giữ nguyên ngày/giờ thì dấu báo vắng vẫn có hiệu lực cho giờ học đó; không tự coi đổi người dạy là Member đã rút báo vắng.
- Preview hoặc dời thất bại không thay dấu báo vắng. Cập nhật lịch và trạng thái hiệu lực của dấu báo trong cùng transaction; báo vắng gửi đồng thời từ màn lịch cũ cần kiểm phiên bản/ngày-giờ kỳ vọng để không gắn nhầm sang lịch mới. Cách version/lưu lịch sử/field API là chi tiết kỹ thuật sẽ thiết kế khi triển khai.
- Handoff FE: màn dời có dấu báo vắng thì nêu “Báo vắng hiện tại thuộc lịch cũ; lịch mới cần báo lại nếu không thể tham gia”. Sau thành công, lịch mới không mang nhãn báo vắng cũ; chi tiết lịch sử vẫn xem được dữ kiện cũ theo quyền. Không gọi việc bỏ hiệu lực cho lịch mới là hoàn credit hoặc đổi điểm danh.

**Phương án khác:** yêu cầu người thao tác xác nhận giữ hay bỏ dấu báo vắng mỗi lần dời. Linh hoạt khi Member vẫn không học được, nhưng thêm lựa chọn UI và dễ giữ nhầm trên một ca mới vừa được thống nhất; nếu chọn cần định nghĩa rõ thay vì tự copy cờ.

**Kết quả D07-c:** Owner chốt chỉ bỏ dấu khi thực sự đổi ngày/giờ bằng tính năng “Dời ca”; xem 18.4. Các đoạn hỏi/phân tích trên là lịch sử trước quyết định.

### 18.3. Owner yêu cầu giải thích lại thời gian và đơn giản UX — đang làm rõ

Owner yêu cầu dừng để giải thích câu “ca cũ chưa bắt đầu, ca mới >=12 giờ và trong hạn”, đồng thời đề nghị xóa dấu vắng/xem lịch đã dời như buổi mới cho đơn giản. Chưa chuyển D08, chưa tự thay điều kiện thời gian của D07-b đã chọn.

- “Ca cũ chưa bắt đầu”: thời điểm Manager xử lý phải trước giờ bắt đầu ca đang có. Mục đích là dời một lịch tương lai, không dùng reschedule để xóa buổi đã diễn ra/no-show. D07-b còn thêm điều kiện ca cũ dưới 12 giờ vì đây là nhánh ngoại lệ sát giờ.
- “Ca mới >=12 giờ”: tính từ **thời điểm Manager xử lý**, không tính từ giờ ca cũ hoặc thời điểm thanh toán. Ví dụ xử lý 08:30 cho ca cũ 09:30: ca mới 13:00 cùng ngày chưa đủ notice, ca 06:00 hôm sau đủ. Member bất khả kháng chưa được chọn ca mới dưới 12 giờ; ngoại lệ đó đã chốt riêng cho lỗi trung tâm D06-b.
- “Trong hạn”: toàn bộ ca mới phải kết thúc trước hoặc đúng deadline 1080 giờ của gói. Chưa có quyền tự gia hạn từ quyết định dời; nếu không còn ca đáp ứng thì phải xử lý theo chính sách bù/hạn D08 khi được chốt, không sửa ngày để bỏ qua.
- Về UX: đề xuất A tại 18.2 **đã không mang dấu báo vắng cũ sang ngày/giờ mới**. Giữ lịch sử chỉ là lưu vết phía hệ thống, không bắt Member xem lịch sử, xác nhận thêm hoặc nhìn nhãn báo vắng cũ trên lịch mới. Cần sửa cách giải thích để không tạo cảm giác buổi mới vẫn bị đánh vắng.
- Ghi nhận hướng đơn giản Owner muốn: sau dời ngày/giờ thành công, UI thể hiện “Đã dời lịch”, ca mới Scheduled/chưa có kết quả điểm danh và **không có dấu báo vắng đang hiệu lực**. Xem như buổi mới để tham gia trên UI, nhưng vẫn là chuyển một quyền học đã trả: không cấp thêm/trừ thêm credit, không tự reset hạn gói, không xóa lịch sử thao tác. Không cần tạo Session ID mới chỉ để đạt UX này; thiết kế dữ liệu quyết định khi triển khai.
- Không thêm hộp thoại chọn giữ/bỏ dấu trong luồng dời ngày/giờ; thông tin tác động có thể nằm ngay phần xác nhận cũ–mới đã có. Đây là hướng làm rõ UX, không thay điểm danh thật của buổi đã chốt.

**Chỉ còn phạm vi cần xác nhận:** nếu chỉ đổi sân hoặc đổi Coach, giữ nguyên ngày/giờ thì có xem đó là “buổi mới” và bỏ dấu báo vắng không? Đề xuất tiếp tục giữ dấu trong trường hợp này vì lịch Member không đổi; nếu Owner muốn bỏ thì cần có xác nhận Member sẽ tham gia, không xóa ngầm dấu vì đổi người/sân. Chưa tự chốt ngoại lệ này.

### 18.4. D07-c đã chốt — chỉ bỏ dấu khi thực sự “Dời ca” đổi ngày/giờ

Owner xác nhận: **“chỉ bỏ khi thực sự đổi ngày/giờ và tính năng ‘Dời ca’”**.

- Chỉ khi thao tác Dời ca thành công **và** ngày/giờ thực sự thay đổi mới bỏ hiệu lực dấu báo vắng hiện tại. Lịch mới hiển thị Scheduled/“Đã lên lịch”, không mang dấu cũ. Giữ lịch sử phía hệ thống, không hiển thị dấu cũ như trạng thái của lịch mới hoặc thêm bước chọn giữ/bỏ cho người dùng.
- Đổi riêng sân/Coach cùng ngày giờ, lưu lại lịch không đổi, preview, thao tác thất bại hoặc retry không làm đổi lịch thì không bỏ dấu. Không dùng API cập nhật chung để lách điều kiện này; backend kiểm thao tác và giá trị trước/sau, không chỉ tin một cờ “dời ca” từ client.
- Không tạo thêm quyền học, không trừ/hoàn credit hoặc reset hạn gói; không xóa audit/attendance đã chốt. Nếu Member không tham gia lịch mới thì báo lại trong cửa sổ D05-b.
- Handoff BE: đổi lịch, lưu lịch sử và bỏ hiệu lực dấu cũ trong cùng transaction/lock; xử lý race báo vắng từ màn lịch cũ bằng kiểm phiên bản/lịch kỳ vọng. Retry phải trả kết quả nhất quán, không xóa một báo vắng mới được gửi hợp lệ cho lịch mới chỉ vì lặp request dời cũ.
- Handoff FE: tích hợp ngay màn Dời ca dùng chung 16.4; sau thành công cập nhật nhãn theo BE, không tự xóa dấu khi mới chọn ca. Nghiệm thu đổi ngày, đổi ca cùng ngày, đổi sân/Coach cùng giờ, không thay đổi giờ, preview/rollback/retry và báo vắng đồng thời. Chạy .\mvnw.cmd clean test khi sửa backend.

Chỉ cập nhật docs/handoff, chưa sửa code/quyền/DB hoặc chạy tests ứng dụng. Không hỏi lại phạm vi bỏ dấu; chuyển D08.

## 19. D08-a — quyền học bù khi lỗi trung tâm làm vượt hạn gói

### 19.1. Mâu thuẫn cần chọn mô hình

- I.5 muốn không kéo hạn mọi credit khi chỉ một buổi gặp sự cố, nhưng lại đề xuất Manager gia hạn **toàn gói**. Hai cách có tác động khác nhau; không thể chỉ thêm checkbox Manager rồi nói các credit khác vẫn hết hạn cũ.
- Source rescheduleSession đang tự kéo endDate gói nếu lịch mới vượt ngày hạn hoặc có extendValidityDays; MemberPackageService.extendValidity cũng sửa hạn chung, còn expireOutdatedPackages đặt remainingCredit=0. Chưa có quyền bù riêng từng buổi/credit bị ảnh hưởng.
- Các quyết định trước chặn tự gia hạn khi dời thường/Member bất khả kháng. D06 đã yêu cầu lỗi trung tâm có cách bảo toàn quyền học; credit hoàn phải dùng được thật, không chỉ cộng vào gói đã hết hạn. D03 chốt 1080 giờ làm deadline chuẩn, ngoại lệ cần chỉ rõ phạm vi quyền được bù.

### 19.2. So sánh ban đầu — Owner chọn B tại 19.3, phương án A không chọn

| Lựa chọn | Quyền được kéo dài | Đánh đổi |
|---|---|---|
| A. Quyền bù riêng buổi bị ảnh hưởng | Chỉ buổi đã đặt hoặc đúng một quyền học hoàn vì sự cố được dùng đến hạn bù Manager duyệt; các credit khác vẫn hạn gốc | Đúng mục tiêu không mở lại credit không liên quan. Cần thêm dữ liệu quyền bù và kiểm tiêu thụ/hạn, nhiều code hơn sửa một deadline |
| B. Manager duyệt gia hạn toàn gói | Mọi credit còn lại được hưởng hạn mới | Dễ triển khai/giải thích trên một gói; phải chấp nhận mở rộng quyền cả credit không bị sự cố. Credit đã bị cron xóa vẫn cần đối soát phục hồi có căn cứ, không bật Active là xong |

**Đề xuất A** nếu giữ mục tiêu I.5: lỗi trung tâm gây mất một buổi thì bù đúng buổi đó. UI vẫn có thể đơn giản: trong màn Manager xử lý sự cố hiển thị “Hạn gói gốc”, “Hạn học bù của buổi này” và tác động; Member xem một mục “Buổi học bù”, không phải mua gói mới.

Phạm vi A cần bảo đảm nếu được chọn:

- Dời một buổi đã trừ credit sang sau hạn: giữ quyền của buổi đó, không cấp thêm một credit khả dụng chỉ vì cấp hạn bù. Buổi bù vẫn đúng môn/Coach hợp lệ và các validation tài nguyên; Coach thay không tự trở thành Primary Coach.
- Hủy do trung tâm sau/sát hạn: ghi đúng một quyền học được hoàn/bù với hạn riêng, không vừa cộng một credit thường vừa cấp thêm một quyền bù cho cùng lần hủy. Thuật ngữ/representation API sẽ được thiết kế để FE không hiển thị thành hai lượt.
- Liên kết với Session/subscription/sự cố gốc, actor/lý do/hạn cũ-mới; transaction và idempotency tránh bù hai lần. Không phục hồi toàn bộ credit đã hết hạn của gói hoặc dùng invoice Paid cũ để cấp một gói mới.
- Hạn bù cần Manager phê duyệt cụ thể, không vô thời hạn; cách chọn/giới hạn hạn bù sẽ chốt D08-b sau khi chọn mô hình. Chưa tự thêm số ngày mặc định.
- Gói gốc có thể vẫn Expired nhưng quyền bù riêng còn hiệu lực; BE/FE/báo cáo phải phân biệt, không dùng status Active của gói làm điều kiện duy nhất hoặc cộng lại 12 credit. Cách biểu diễn schema/quyền bù là thiết kế kỹ thuật sau quyết định.
- Chỉ cho lỗi trung tâm đã xác nhận trong scope này, không tự áp cho mọi Member xin dời sát giờ. Sự cố được báo/duyệt sau khi buổi đã bắt đầu hoặc job chốt cũng cần quy trình đối soát riêng, chưa tự mở quyền sửa lịch sử.

**Kết quả D08-a:** Owner chọn B và muốn gia hạn theo số ngày/giờ bị ảnh hưởng. Quyết định và phần công thức còn cần chốt tại 19.3–19.4; không triển khai quyền bù riêng theo A hoặc hỏi lại phạm vi gia hạn toàn gói.

### 19.3. D08-a đã chốt B — gia hạn toàn gói theo ảnh hưởng, không nạp lại gói

- Owner chọn **B: gia hạn toàn gói**, các credit còn lại cùng hưởng deadline mới. Quyết định này thay mục tiêu giữ nguyên hạn các credit không liên quan trong phương án A; các phần thiết kế quyền bù riêng tại 19.2 là lịch sử, không phải kế hoạch đang chọn.
- Owner muốn mức gia hạn tương ứng số ngày/giờ bị ảnh hưởng; chưa chốt “ảnh hưởng” là thời gian dịch vụ gián đoạn hay khoảng cách tới lịch học bù. Không tự cộng tròn ngày, không tự đặt 7/14/30 ngày hoặc bắt đầu lại 1080 giờ.
- Không tự cấp lại 12 credit khi kéo hạn. Dời buổi giữ số credit đã trừ; hủy do trung tâm hoàn đúng một credit theo sự cố. Các credit cũ đã bị cron xóa cần đối soát số dư/lịch sử trước khi phục hồi; không coi set Active là hoàn thành, không phục hồi mọi dữ liệu chỉ vì một trường hợp được bù.
- Không áp gia hạn này cho dời thường hoặc bất khả kháng phía Member đã chốt D07. Manager cùng center phải duyệt mức bù và căn cứ, audit hạn cũ/mới, người/thời điểm/sự cố; đồng bộ lưu hạn/credit trong transaction, xử lý lặp không bù lần hai. Không dùng source tự kéo endDate hiện tại làm công thức đã được duyệt.

### 19.4. D08-b — phân tích trước quyết định, đã chốt tại 19.5

**Hai cách tính khác nhau:**

| Căn cứ | Ưu điểm | Rủi ro |
|---|---|---|
| Thời gian trung tâm thực sự không thể phục vụ đăng ký bị ảnh hưởng | Bù đúng khoảng quyền sử dụng bị mất; không phụ thuộc Member tự chọn lịch bù rất xa | Cần Manager xác nhận bắt đầu/kết thúc/phạm vi ảnh hưởng. Gián đoạn ngắn có thể chưa đủ để xếp ca bù trước hạn |
| Độ trễ buổi học bù so với buổi cũ | Dễ thấy trên màn dời và dễ cộng giờ | Cùng một sự cố có thể được bù rất khác vì lịch rảnh cá nhân; nhiều buổi cùng dời có thể làm cộng trùng thời gian nếu cộng từng buổi |

**Đề xuất ưu tiên thời gian gián đoạn thực tế**, theo timestamp, và bảo đảm riêng ca bù đã thống nhất không vượt hạn mới. Đây là đề xuất chính sách cần Owner duyệt, chưa phải implementation:

1. Manager ghi nhận sự cố/phạm vi đăng ký ảnh hưởng, khoảng thời gian không thể cung cấp dịch vụ (từ lúc bắt đầu ảnh hưởng thực tế đến lúc có thể phục vụ lại đúng môn/Coach hoặc phương án thay đã được Member chấp nhận). Không dùng ngày tạo báo cáo hoặc lúc Member đọc thông báo làm mốc. Chưa rõ mốc thì chưa tự tính một con số.
2. Cộng đúng số giờ/phút bị mất, không làm tròn cả ngày. Do hạn chuẩn là thời gian thực 1080 giờ, khoảng gián đoạn liên tục có thể bao gồm đêm; không coi lịch nghỉ thông thường, ngày Member bận hoặc toàn bộ ca trống của Coach là sự cố. Phải xác nhận có ảnh hưởng tới đăng ký, không gia hạn mọi Member chỉ vì một sân hỏng.
3. Trong một đăng ký, các khoảng sự cố trùng nhau chỉ tính phần hợp nhất **một lần**; cùng sự cố ảnh hưởng nhiều buổi không cộng lại theo số buổi. Hai khoảng độc lập không chồng nhau có thể cộng, mỗi khoản có căn cứ/audit riêng. Retry và sửa báo cáo không tự phát sinh khoản bù mới.
4. BE gợi ý deadline sau bù thời lượng; nếu ca bù đã thống nhất vẫn kết thúc sau deadline đó, Manager cần phê duyệt hạn ít nhất đủ kết thúc ca bù. Phần vượt thêm phải hiển thị riêng là quyết định bảo đảm buổi bù, không ghi sai thành thời gian trung tâm đóng cửa. Gia hạn này vẫn áp cho cả số credit còn lại theo lựa chọn B.
5. Ví dụ gói hết 09/12 18:30, gián đoạn được duyệt 48 giờ → hạn gợi ý 11/12 18:30. Sự cố hỏng sân chỉ kéo dài 2 giờ nhưng Member chọn học sau 7 ngày không tự đồng nghĩa được cộng 7 ngày; chỉ cần nâng thêm deadline nếu ca bù thực sự vượt hạn gợi ý và Manager duyệt. Nếu chưa có lịch bù thì không được hứa credit đã hoàn chắc chắn dùng được chỉ từ cộng 2 giờ; cần chốt lịch/hạn thực tế khi xử lý.

**Tối ưu dữ liệu/code:** dùng deadline chung của subscription, không cần bảng quyền bù riêng A. Tuy nhiên vẫn cần lưu căn cứ thời lượng và khoản gia hạn đã áp, hạn gốc/cũ/mới để chống cộng trùng; một ô extendDays không đủ bảo vệ điều này. Chốt cách tính lũy kế/điều chỉnh/không giảm quyền đã cấp khi thiết kế kỹ thuật, không cộng lại toàn bộ lịch sử vào một deadline vốn đã bù. Tận dụng audit và UI sự cố/dời hiện có, không thêm workflow Member duyệt mới.

**UI/UX đề xuất:** tại màn Manager xử lý sự cố, hiển thị khoảng ảnh hưởng, thời lượng bù không trùng, hạn hiện tại → hạn đề xuất, ca bù nếu có và lý do phần gia hạn thêm; Manager duyệt một lần cùng thao tác xử lý. Member thấy hạn gói mới và lý do ngắn, không cần hiểu cách hợp nhất khoảng thời gian. BE quyết định số giờ/hạn, FE không tự gửi số ngày được tin vô điều kiện.

**Giới hạn còn phải xử lý sau công thức:** sự cố chưa biết ngày khôi phục, gói hết hạn trong lúc chờ duyệt, phục hồi đúng credit bị cron xóa và trường hợp báo sau buổi đã diễn ra. Không tạm bỏ kiểm hạn hoặc hồi tố sửa attendance để né các trường hợp này.

**Kết quả D08-b:** Owner đồng ý lấy thời gian gián đoạn thực tế làm mức bù chuẩn, kèm Manager duyệt thêm đủ ca bù khi cần; yêu cầu thiết kế đơn giản. Quyết định/handoff tại 19.5. Không hỏi lại căn cứ tính bù.

### 19.5. D08-b đã chốt — bù gián đoạn thực tế, Manager duyệt thêm khi cần

Owner xác nhận: **“đồng ý lấy thời gian gián đoạn thực tế làm mức bù chuẩn, kèm Manager duyệt thêm để đủ ca bù khi cần. Đơn giản thôi là được”**.

**Quy tắc mục tiêu:** gia hạn deadline chung của gói bằng thời gian gián đoạn đã xác nhận, tính đúng giờ/phút và không trùng phần đã bù. Nếu ca bù thống nhất kết thúc vượt hạn sau bù chuẩn, Manager duyệt tăng thêm tối thiểu đủ kết thúc ca đó, có lý do. Số credit còn lại cùng hưởng deadline mới theo D08-a B; không nạp lại 12 credit hoặc tính lại 1080 giờ từ ngày duyệt.

**Một luồng UI ngắn trong màn xử lý sự cố/dời ca hiện có:**

1. Manager nhập/xác nhận **bắt đầu – kết thúc ảnh hưởng**, lý do và ca học bù nếu có; BE xác minh đăng ký bị ảnh hưởng và phần thời lượng chưa được bù.
2. Hiển thị **thời lượng bù**, **hạn hiện tại → hạn đề xuất**. Chỉ khi ca bù vượt hạn đề xuất mới hiện phần “Gia hạn thêm để đủ ca bù” và yêu cầu lý do. Không buộc người dùng chọn thuật toán hoặc tạo một gói/quyền bù riêng.
3. Manager xác nhận một lần; BE kiểm lại, lưu thay đổi hạn cùng kết quả xử lý sự cố/audit trong transaction thích hợp. Member thấy hạn mới và lý do ngắn. Không thêm hàng đợi Member duyệt hoặc kênh thông báo mới.

**Handoff BE:**

- Lưu đủ liên kết subscription/sự cố, khoảng ảnh hưởng được duyệt, thời lượng đã áp, deadline trước/sau, phần gia hạn thêm nếu có, actor/time/lý do. Đây là dữ liệu kiểm soát phía server, không biến thành nhiều biểu mẫu FE. Tên bảng/field/API thiết kế lúc triển khai, ưu tiên tận dụng luồng/audit hiện có.
- Tính phần gián đoạn không trùng trong từng đăng ký; cùng sự cố/nhiều buổi/request lặp không cộng nhiều lần. Không cộng lại toàn bộ thời lượng lịch sử vào deadline đã được bù. Sửa sự cố đã áp cần đối chiếu phần chênh lệch, không tự giảm quyền đã duyệt hoặc gọi lại như lần bù mới.
- Mức bù gợi ý do BE tính; Manager không có nghĩa là client được tự gửi deadline/giờ bù bất kỳ mà bỏ kiểm quyền/center/căn cứ. Mọi ca mới vẫn kiểm môn/Coach/phòng/Member/holiday/nắng, notice theo đúng nguyên nhân và deadline đã được duyệt trong cùng xử lý.
- Dời giữ credit đã trừ; hủy do trung tâm hoàn đúng một credit. Gia hạn không tự cộng credit, không tự mở lại gói Cancelled/đã đối soát hoặc sửa Invoice.amount. Dữ liệu expiry cần xử lý ở D08-c trước nghiệm thu đầy đủ.
- Source tự kéo endDate khi dời phải được thay bằng policy đã chốt; không để đường reschedule/extend khác bỏ qua mức duyệt và audit. Đồng bộ AGENTS/contract/schema/DTO/jobs khi triển khai, giữ rõ hạn chuẩn 1080 giờ và phần bù có căn cứ.

**Nghiệm thu khi triển khai:** gián đoạn 48 giờ bù đúng 48 giờ; phần trùng và retry không bù thêm; phần không trùng được tính đúng; Manager duyệt thêm chỉ đủ ca bù và có lý do; sai quyền/center hoặc thiếu dữ liệu bị chặn. Kiểm atomic/race/rollback, deadline timestamp/credit không nhân đôi và FE chỉ cập nhật sau BE thành công. Chạy .\mvnw.cmd clean test sau mọi thay đổi backend.

Chỉ cập nhật docs, chưa sửa code/schema/DB hoặc chạy tests ứng dụng. Các việc xác định ca bù sau khi gói đã Expired, credit bị job xóa hoặc sự cố chưa biết giờ kết thúc vẫn là điểm mở, không coi đã được giải quyết chỉ bằng công thức này.

### 19.6. D08-c — phân tích trước quyết định, đã chốt tại 19.7

**Bằng chứng:** expireOutdatedPackages hiện chuyển Expired và đặt remainingCredit=0. Gia hạn sau đó không tự biết trước lúc xóa còn bao nhiêu credit; bật lại Active không đủ và cấp lại 12 sẽ sai. Gia hạn toàn gói theo B cần chốt quyền phục hồi nếu sự cố xảy ra lúc gói còn hạn nhưng được xử lý sau hạn.

**Đề xuất:** Manager vẫn được xử lý sự cố có căn cứ phát sinh trong thời gian gói còn hiệu lực; sau khi hạn mới được duyệt còn hiệu lực, phục hồi **đúng số credit hợp lệ chưa dùng bị expiry thu hồi**, dựa trên snapshot/lịch sử có lưu trước khi job xóa, và đối chiếu mọi giao dịch đã dùng/hoàn/bù để không cộng trùng. Buổi đã đặt giữ phần credit đã trừ; hủy do trung tâm chỉ hoàn một lần. Không suy số dư từ giá hoặc mặc định 12.

Đây không phải quyền mở mọi gói đã hết hạn: không phục hồi credit của gói hết hạn trước khi sự cố ảnh hưởng, gói Cancelled hoặc dữ liệu đã đối soát. Nếu thiếu bằng chứng số dư thì báo cần đối soát, không tự đoán. Gói vẫn không đặt được khi deadline mới chưa hiệu lực; xử lý lịch sử buổi đã diễn ra/chốt attendance là phạm vi riêng.

Hướng kỹ thuật đơn giản: job expiry lưu số dư bị thu hồi cùng mốc/audit trước khi đưa về 0; dùng dữ kiện đó để Manager duyệt phục hồi trong cùng luồng sự cố, không bắt Member mua lại. Đây là đề xuất lưu dữ liệu, chưa có trong source và chưa chạy migration.

**Kết quả D08-c:** Owner trả lời “Đồng ý”; quyết định/handoff tại 19.7. Không hỏi lại quyền phục hồi có căn cứ này hoặc tự áp cho mọi gói cũ.

### 19.7. D08-c đã chốt — phục hồi đúng credit bị expiry thu hồi

- Với sự cố trung tâm có căn cứ phát sinh khi gói còn hiệu lực, Manager cùng center được duyệt hạn bù mới còn hiệu lực và phục hồi đúng số credit hợp lệ chưa dùng bị expiry thu hồi. Không mặc định cấp lại 12, không mở lại gói hết hạn trước sự cố/Cancelled hoặc dữ liệu đã đối soát không đủ điều kiện.
- Số phục hồi phải dựa trên snapshot/lịch sử trước khi job đưa số dư về 0, đối chiếu credit đã dùng/hoàn/phục hồi. Buổi đã đặt giữ phần credit đã trừ; hủy do trung tâm chỉ hoàn đúng một lần. Không hoàn trùng giữa phục hồi số dư và hoàn ca, không thay Invoice.amount hoặc tạo thanh toán giả.
- Handoff BE: job expiry ghi số dư bị thu hồi, thời điểm và đăng ký trong cùng transaction với cập nhật Expired/0; bảo toàn quy tắc credit hết hạn không dùng được. Luồng Manager kiểm quyền/center/căn cứ sự cố/hạn mới, khóa gói và dữ kiện thu hồi để gia hạn + phục hồi + audit nhất quán, idempotency ngăn phục hồi lặp. Tên bảng/field và cách tận dụng audit thiết kế lúc triển khai.
- Thiếu bằng chứng số dư thì hiển thị cần đối soát, không tự đoán. Chưa duyệt hạn mới hoặc deadline mới vẫn đã qua thì không cho đặt chỉ vì có số dư/nhãn Active. Không dùng cơ chế này để sửa attendance thật của buổi đã chốt.
- Handoff FE: tích hợp ngay màn xử lý sự cố/duyệt hạn; hiển thị hạn cũ → mới, số credit sẽ phục hồi và lý do, Manager xác nhận một lần. Member xem hạn/số dư chính thức sau thành công, không mua lại gói hoặc thao tác phục hồi riêng.
- Nghiệm thu khi triển khai: số dư 3 bị expiry thu hồi chỉ phục hồi tối đa phần hợp lệ của 3 sau đối chiếu, không phải 12; lặp request/race với job hoặc hủy ca không nhân đôi. Sai quyền/center, sự cố ngoài thời gian hiệu lực, thiếu chứng cứ hoặc hạn mới chưa hợp lệ bị chặn; rollback không để hạn/số dư/audit lệch. Chạy .\mvnw.cmd clean test sau sửa backend.

Chỉ ghi quyết định/handoff và cập nhật contract/checklist; chưa sửa code/schema/credit hoặc chạy tests ứng dụng. D08-a/b/c đã chốt; trường hợp chưa biết giờ kết thúc còn ở 19.8.

### 19.8. D08-d — phân tích trước quyết định, đã chốt tại 19.9

Công thức đã chốt cần khoảng gián đoạn thực tế có bắt đầu/kết thúc. Nếu chưa biết lúc khôi phục mà nhập ngày dự kiến như sự thật thì mức bù có thể sai; không tự mở hạn vô thời hạn hoặc thêm một bộ đếm gia hạn chạy liên tục.

**Đề xuất:** Manager lưu cùng hồ sơ sự cố với mốc bắt đầu, lý do/phạm vi ảnh hưởng và nhãn “Chờ khắc phục”; khi có mốc khôi phục thực tế thì bổ sung giờ kết thúc, BE gợi ý mức bù và Manager duyệt theo 19.5. Hai thao tác nằm trên cùng màn, không thêm workflow Member duyệt hoặc trường nhập thuật toán. Đây là luồng đề xuất, chưa công bố endpoint/status DB mới.

- Khi chưa chốt thời lượng, không tự đoán giờ kết thúc hoặc thay deadline gói. Gói vẫn dùng theo hạn/quyền hiện có nếu còn điều kiện phục vụ hợp lệ; lịch đang bị ảnh hưởng không tự coi đã được dời/hủy/điểm danh chỉ vì lưu hồ sơ sự cố.
- Nếu gói hết hạn trong lúc chờ: job vẫn ghi số dư thu hồi; sau khi Manager duyệt bù, xử lý phục hồi theo D08-c. Trong khoảng chưa có hạn mới, gói đã hết hạn tạm chưa đặt được; đây là đánh đổi của phương án đơn giản, không được hiển thị sai là đã gia hạn.
- Lúc xử lý cuối vẫn xác nhận phương án với Member, kiểm ca bù/tài nguyên, ghi hạn/số dư trước-sau và thông báo kết quả sau commit. Không cần tạo lại hồ sơ hoặc bù trùng các khoảng đã áp.

**Kết quả D08-d:** Owner trả lời “Đồng ý”; quyết định/handoff tại 19.9. Không tự thêm gia hạn tạm hoặc hỏi lại luồng chờ khắc phục.

### 19.9. D08-d đã chốt — chờ khắc phục rồi duyệt bù thực tế

- Manager lưu sự cố chưa biết giờ kết thúc với mốc bắt đầu, lý do/phạm vi ảnh hưởng và nhãn **“Chờ khắc phục”**. Khi có giờ khôi phục thực tế, bổ sung mốc kết thúc; BE tính thời gian chưa được bù, Manager duyệt theo D08-b ngay trên cùng hồ sơ/màn sự cố.
- Không dùng giờ dự kiến làm giờ thực tế, không tự gia hạn tạm/vô thời hạn hoặc thêm job tự tăng hạn liên tục. Đang chờ không tự đổi lịch/credit/attendance của các buổi liên quan; xử lý dời/thay/hủy vẫn theo phương án đã thống nhất.
- Gói còn hạn vẫn dùng theo quyền/tài nguyên hợp lệ. Nếu expiry trong lúc chờ thì không đặt tiếp bằng gói đã hết hạn; job lưu số dư thu hồi, sau duyệt hạn mới còn hiệu lực thì phục hồi đúng số dư có căn cứ theo D08-c.
- Handoff BE: lưu hồ sơ/khoảng ảnh hưởng và phạm vi, kiểm Manager/center, mốc thời gian hợp lệ; cập nhật kết thúc + gia hạn/phục hồi/hoàn credit tương ứng + audit nhất quán theo transaction nghiệp vụ. Retry hoặc mở lại hồ sơ không tính trùng khoản đã bù; chưa có tên endpoint/field/status DB mới để FE gọi.
- Handoff FE: một màn sự cố với thao tác lưu chờ và hoàn tất khi có giờ thực tế; chỉ phần hoàn tất mới hiển thị mức bù/hạn/số dư trước-sau để duyệt. Member xem đúng tình trạng và hạn hiện có, không hiển thị đã gia hạn khi chưa duyệt; thông báo kết quả sau commit theo cơ chế đã chốt.
- Nghiệm thu khi triển khai: hồ sơ chưa có kết thúc không tự tăng hạn/credit; kết thúc trước bắt đầu hoặc sai quyền/center bị chặn; expiry trong lúc chờ lưu được số dư; hoàn tất tính bù và phục hồi đúng một lần, rollback không thay dữ liệu một phần. Kiểm các ca đang chịu sự cố không bị tự đánh dấu đã dời/hủy/chốt chỉ từ việc lưu hồ sơ; chạy .\mvnw.cmd clean test khi sửa backend.

**Tổng kết phạm vi D08 đã chốt:** gia hạn toàn gói; bù gián đoạn thực tế không trùng; Manager duyệt thêm đủ ca bù khi cần; phục hồi credit expiry có đối soát; chưa biết giờ khôi phục thì lưu chờ và duyệt sau. Chưa triển khai. Sự cố báo sau khi buổi đã diễn ra/attendance đã chốt tiếp tục đối chiếu D15, không mở quyền sửa lịch sử từ các quyết định này.

## 20. D09 — lỗi danh sách Coach: log xác nhận lazy Specialization, còn kiểm gắn endpoint khi triển khai

### 20.1. Bằng chứng và mức chắc chắn

- Báo cáo system-test-rerun-2026-10-24.md: Manager và Member có cookie hợp lệ đều 500 tại 08:52–08:54 UTC ngày 24/10 (15:52–15:54 Việt Nam); /api/staff và /api/sessions cùng lượt có 200. Đây là báo cáo FE cũ, chưa phải probe mới của lượt này.
- Workspace application.properties có spring.jpa.open-in-view=false. CoachController.findAll không có @Transactional; lấy Coach qua coachRepository.findAllWithUser, sau đó gọi coachSpecializationRepository.findByCoachUserId cho từng Coach và đọc item.getSpecialization().getName().
- CoachSpecialization.specialization là ManyToOne LAZY; repository findByCoachUserId chưa khai báo fetch join/entity graph cho specialization. Vì vậy có **nguy cơ LazyInitializationException** khi ánh xạ tên ngoài persistence context; đây là giả thuyết source cần stack trace hoặc tái hiện có OSIV=false, không phải nguyên nhân live đã chứng minh.
- CoachRepository.findAllWithUser đã fetch user và role; không nên khẳng định mọi quan hệ của Coach đều chưa fetch. Chuỗi xử lý còn truy vấn chuyên môn theo từng Coach, tạo mẫu N+1; cải thiện này riêng với việc chứng minh 500.
- DTO CoachSummaryResponse đã có id/name/avatar/specializations/bio/certification/status, email/phone theo MANAGE_USERS. Không dùng sửa lỗi để mở contact cho Member hoặc đổi response tùy ý. Center đang được lọc sau khi lấy danh sách/mapping; cần giữ phạm vi dữ liệu khi thiết kế query thay thế.

### 20.2. Hướng kiểm chứng và sửa sau khi có bằng chứng

1. Lấy stack trace đúng request /api/coaches lỗi, SHA deploy và cấu hình thực tế; không dùng log thiếu sessions.subscription_id ngày 23/10 làm chứng cứ của lỗi Coach ngày 24/10.
2. Nếu log/tái hiện xác nhận lazy access: chuyển read/mapping DTO vào service @Transactional(readOnly=true) và fetch rõ dữ liệu cần dùng. Ưu tiên truy vấn theo center/điều kiện rồi batch-fetch specialization thay vì N+1; không bật OSIV hoặc đổi mọi association thành EAGER để che lỗi.
3. Giữ contact theo authority, center và bộ lọc specializationId/keyword; xử lý Coach chưa có specialization bằng danh sách rỗng hợp lệ, không tự xóa Coach hoặc bịa chuyên môn để hết lỗi.
4. Khi đến triển khai: tái hiện với OSIV=false/dữ liệu Coach có nhiều hoặc không có specialization, test hai quyền có/không MANAGE_USERS, center và bộ lọc; chạy .\mvnw.cmd clean test rồi probe bản deploy đúng SHA và màn FE chọn Coach bằng API thật. Không chỉ xác nhận một HTTP 200 trống.

**Điểm dừng kiểm chứng D09:** cần stack trace Render của /api/coaches (đúng khoảng lỗi hoặc lần tái hiện mới), kèm SHA nếu có. Chưa có nguồn này nên chưa quy lazy loading là root cause, chưa sửa backend/DB/deploy hoặc chạy tests ứng dụng. Không cần Owner chọn chi tiết transaction/fetch; đó là thiết kế kỹ thuật sau xác nhận nguyên nhân trong giai đoạn triển khai.

### 20.3. Tiếp nhận log và cách lấy stack trace

Owner nói cung cấp log hai ngày gần đây nhưng trong tin nhắn đang xử lý chưa có nội dung/file đính kèm đọc được. Kiểm tên file workspace không thấy file Render mới; các log audit hiện có thuộc đợt 10/10, không thay thế log request 24/10. Chưa kết luận log Owner định gửi có/không có lỗi cần tìm, không xác nhận root cause từ việc chưa nhận file.

Hướng dẫn lấy lại: Render service → Logs (runtime), chọn khoảng 24/10/2026 15:50–15:57 giờ Việt Nam (08:50–08:57 UTC), hoặc Live tail và tái hiện bằng tài khoản đăng nhập đang xem danh sách Coach. Tìm /api/coaches, CoachController, LazyInitializationException, could not initialize proxy, Caused by hoặc ERROR; không chỉ tìm giả thuyết lazy mà bỏ qua nguyên nhân khác. Sau khi xác định thời điểm, bỏ bộ lọc chữ/level và lấy toàn bộ đoạn lỗi cùng các dòng stack/chuỗi Caused by lân cận; chỉ lấy dòng ERROR hoặc response 500 không đủ.

[Render Logs chính thức](https://render.com/docs/logging): timestamp trên UI theo múi giờ máy, hover xem UTC; Ctrl+Shift+C sao chép log đang hiển thị, không bảo đảm tự lấy toàn bộ hai ngày. HTTP request logs/bộ lọc method/path/status phụ thuộc gói có hỗ trợ, nên không bắt buộc dùng các filter đó để tìm runtime stack.

Nếu log cũ không còn hoặc không xuất hiện stack, mở Live tail, thực hiện lại đúng GET /api/coaches qua màn FE và ghi thời điểm/status trong Network. Gửi file .log/.txt hoặc dán đầy đủ từ dòng exception đầu đến Caused by cuối và vài dòng trước/sau, kèm SHA deploy nếu có; không cần cung cấp mật khẩu/cookie để đọc stack. Chưa sửa code/cấu hình logging hoặc chạy probe business mới trong bước này.

### 20.4. Đã nhận đoạn log 24/10 08:54:16 UTC — còn thiếu đầu exception

Đã đọc file user gửi tại C:/Users/Admin/.codex/attachments/236a37d7-03c0-4594-bc7b-cbad7be3e4c6/Pasted text.txt. Timestamp 2026-10-24T08:54:16Z (15:54:16 Việt Nam) nằm trong khoảng cần tìm. Nội dung chỉ có phần đuôi stack Spring Security/Servlet/Tomcat, bắt đầu OncePerRequestFilter.doFilter và kết thúc Thread.run; không có dòng exception/message đầu, /api/coaches, CoachController hoặc Caused by.

Chưa đủ gắn đoạn này với đúng request Coach hoặc xác nhận lazy loading; các frame framework chung không chứng minh lỗi phân quyền. Cần kéo lên các dòng ngay trước cùng mốc 08:54:16 UTC, lấy từ ERROR/tên exception/message và các frame com.sportscenter, rồi nối phần đuôi đã có; giữ mọi Caused by nếu xuất hiện. Chưa sửa code/DB hoặc chạy tests ứng dụng.

### 20.5. Log bổ sung đã xác nhận LazyInitializationException — đã lọc trùng

Owner gửi các dòng ERROR và exception đầu. Sau gộp các đoạn lặp, có **ba lần lỗi riêng biệt cùng loại**, không phải sáu lỗi:

| Ngày 24/10/2026 UTC | Giờ Việt Nam | Root error trong log |
|---|---|---|
| 08:51:20 | 15:51:20 | LazyInitializationException, Specialization#5, no Session |
| 08:54:15 | 15:54:15 | Cùng loại và cùng proxy |
| 08:54:16 | 15:54:16 | Cùng loại và cùng proxy; nối được theo thời điểm với phần đuôi user gửi trước |

Thông báo nguyên nhân đã có đầy đủ: `org.hibernate.LazyInitializationException: could not initialize proxy [com.sportscenter.specialization.Specialization#5] - no Session`. Không bắt buộc có dòng Caused by khác: ở đây exception đầu đã chỉ rõ proxy chưa nạp được do không còn Hibernate Session. Không cần tiếp tục yêu cầu Owner tìm Caused by chỉ để xác nhận loại lỗi này.

**Mức kết luận:** log Render đã chứng minh có lỗi lazy loading Specialization#5 ở các thời điểm trên. Đối chiếu source 20.1 và báo cáo FE /api/coaches 500 cùng khoảng thời gian, đường CoachController đọc specialization LAZY là ứng viên rất phù hợp. Đoạn chọn lọc vẫn không chứa URI/request ID hoặc frame com.sportscenter/CoachController, nên liên kết chính xác request/line code cần được khẳng định bằng full stack hoặc test tái hiện khi triển khai; không gọi log này là chứng minh duy nhất mọi 500 đều từ endpoint đó.

`Session` trong thông báo là **Hibernate Session/persistence context**, khác với bảng `sessions` của ca học. Log này không phải bằng chứng thiếu subscription_id và không chứng minh Specialization#5 không tồn tại trong DB; không xóa dữ liệu hoặc tạo migration chỉ từ số ID proxy.

**Handoff BE cho D09:** giữ OSIV=false; đưa query + ánh xạ DTO vào service read-only transaction, fetch specialization cần dùng rõ ràng (ưu tiên batch theo các Coach đúng center để tránh N+1), giữ bộ lọc và contact theo authority. Không bật OSIV toàn cục hoặc đổi mọi association thành EAGER. Không đổi API/field cho FE chỉ để sửa lỗi tải chuyên môn.

**Nghiệm thu sau triển khai:** tái hiện được đường đọc có specialization khi OSIV=false trước sửa; sau sửa danh sách/keyword/specializationId trả đúng, Coach không có chuyên môn trả list rỗng hợp lệ, center/contact không bị mở rộng. Chạy .\mvnw.cmd clean test, kiểm DB/deploy đúng SHA rồi probe Manager/Member và màn chọn Coach thật. Đến lúc đó mới đánh dấu D09 đã sửa; hiện chỉ chốt bằng chứng/hướng sửa, chưa thay backend/DB hoặc chạy tests ứng dụng.

### 20.6. Kế hoạch thực hiện D09 sau khi đọc source — 25/10/2026, chưa sửa BE

**Kiểm workspace:** branch `tai`, HEAD `8e95759`; đã xem status/diff schema và ba docs tracked, đọc docs 04 untracked. Giữ nguyên schema/migration 07/test PostgreSQL, các file xóa/chuyển và toàn bộ old-docs; không khôi phục hoặc stage/commit. Không dùng HEAD này làm SHA Render.

**Đường đọc cần tái hiện:** `CoachController.findAll` → `CoachRepository.findAllWithUser` (đã fetch user/role) → `CoachSpecializationRepository.findByCoachUserId` → `item.getSpecialization().getName()`. Association specialization là LAZY, controller không có transaction, OSIV=false. `getName()` cần nạp proxy, còn chỉ đọc ID có thể không kích hoạt lỗi. Source này giải thích cơ chế phù hợp log 20.5; chưa có test mới để khẳng định frame/URI live. Center hiện được xét sau khi đã đọc tên chuyên môn cả Coach ngoài center.

**Bước 1 — tái hiện trước sửa, trên DB test cô lập:**

1. Tạo test tích hợp dùng H2 in-memory riêng, `spring.config.import=` (không nạp .env), datasource/driver/dialect khai báo rõ, `spring.sql.init.mode=never`, `spring.jpa.open-in-view=false`. Có thể theo cấu trúc `ScheduleDatabaseTest`, nhưng không để transaction của test bao quanh lời gọi controller/request (`Propagation.NOT_SUPPORTED`). Nếu dùng full context/MockMvc, mock các job payment/package/attendance để không chạy nền.
2. Seed trong transaction riêng rồi commit/đóng persistence context trước request. Dữ liệu gồm hai center, Manager/Member, Coach nhiều chuyên môn và Coach không chuyên môn; có tên chuyên môn khác tên Coach để kiểm keyword. Không bắt buộc ID proxy bằng 5: lỗi phụ thuộc vòng đời persistence context, không phụ thuộc số ID.
3. Gọi đúng `GET /api/coaches` qua MockMvc với security thật và danh tính/center tồn tại trong DB test; xác nhận nguyên nhân lồng trong lỗi request là LazyInitializationException tại đường mapping trên. MockMvc có thể ném ServletException thay vì trả response 500 nếu exception không được handler bắt; kiểm nguyên nhân gốc, không ép sai dạng kết quả. Không dùng repository/entity mock để tuyên bố đã tái hiện Hibernate.
4. Lưu kết quả trước sửa. Khi sửa xong, chuyển ca request đó thành regression đòi 200 và nội dung đúng; không giữ expected exception cho endpoint đã sửa. Nếu không tái hiện được, kiểm context còn mở/cache/transaction/fetch thực tế trước kết luận.

**Bước 2 — bản sửa kỹ thuật giới hạn D09:**

- Tạo service đọc danh sách Coach được Spring quản lý, method public `@Transactional(readOnly=true)`; controller gọi qua bean proxy. Lấy center từ CenterContext và xác định contact bằng authority thật; toàn bộ query/filter/mapping kết thúc trước khi trả DTO, không trả entity/Stream còn cần lazy loading.
- Bổ sung query Coach fetch user/role và điều kiện center ngay trong query. Giữ semantics `centerId == null` chỉ cho Admin toàn hệ thống như CenterContext hiện tại; tài khoản thường thiếu center vẫn bị từ chối. Không đổi endpoint/phân quyền khi sửa lỗi.
- Với danh sách Coach đã giới hạn center, lấy assignment + `JOIN FETCH specialization` theo tập Coach ID bằng một query batch; nhóm theo Coach ID, mặc định `specializations=[]`. Danh sách Coach rỗng thì bỏ query `IN` rỗng. Không fetch thêm subject/collection không dùng, không đổi LAZY toàn cục hoặc bật OSIV.
- Giữ nguyên bộ lọc specializationId (ID chuyên môn, không phải subjectId), keyword trim/không phân biệt hoa thường theo tên Coach hoặc tên specialization; không tự thêm lọc Active, chuẩn hóa bỏ dấu, phân trang hay thứ tự mới. Giữ đầy đủ chuyên môn của Coach khớp bộ lọc, không vô tình chỉ trả chuyên môn đang được lọc. Không thay method assignment dùng chung chỉ để sửa đường đọc danh sách.
- Không thêm migration, seed hoặc thao tác sửa Specialization#5 cho D09 từ bằng chứng hiện có. Nếu tái hiện phát hiện lỗi dữ liệu khác thì ghi riêng và đối soát trước.

**Bước 3 — kiểm thử sau sửa:**

| Nhóm | Kỳ vọng cần kiểm bằng dữ liệu thật trong test |
|---|---|
| Vòng đời JPA | Request không có transaction của test/OSIV; 200 với nhiều specialization; JSON serialize được sau service transaction |
| Bộ lọc | Không lọc, keyword null/rỗng/khoảng trắng/khác hoa thường, khớp tên Coach hoặc specialization; specializationId đúng/sai; kết hợp hai điều kiện; không cắt danh sách chuyên môn của Coach khớp |
| Dữ liệu rỗng | Coach không chuyên môn vẫn có `[]`; center không có Coach và lọc không khớp trả `[]` |
| Center | Manager/Member chỉ thấy đúng center, kể cả Coach center khác có keyword/spec trùng; Admin không center giữ phạm vi hiện tại; tài khoản thường thiếu center bị từ chối |
| Contact/quyền | Có MANAGE_USERS nhận email/phone; không có authority này nhận null dù là Manager; anonymous 401 qua SecurityConfig |
| Truy vấn | Đo query khi tăng số Coach, số query không tăng theo từng Coach; tính riêng query xác định actor/center, không nhầm với N+1 assignment |

Chạy `.\mvnw.cmd clean test` sau sửa BE, lưu tổng run/fail/error/skipped từ lần chạy mới; không mở các test live opt-in hoặc dùng .env/DB thật cho fixture. H2 kiểm transaction/query/regression local, không thay nghiệm thu PostgreSQL/SQL Server hoặc Render. Sau khi được yêu cầu triển khai mới kiểm đúng SHA/DB/config và probe Manager/Member/FE, lưu data assertions không chỉ HTTP 200.

**Tài liệu và phần còn mở:** docs 01 mục 4.5 đã sửa mô tả quyền sai thành Authenticated theo SecurityConfig, bổ sung semantics source; không phải thay đổi quyền BE. Kế hoạch bàn giao ở 14.3, rollout/bằng chứng ở docs 02 mục 9.6. D09 đủ hướng kỹ thuật để chuẩn bị sửa, chưa có mâu thuẫn nghiệp vụ mới cần Owner chọn; chưa đánh dấu đã fix. Sau D09 tiếp tục D10 riêng, không tự chốt Invoice 3/return/IPN/tiền về muộn từ kế hoạch kỹ thuật này. Lượt này chỉ đọc source/sửa docs, chưa chạy Maven, tái hiện, probe, mutation DB hoặc deploy.

## 21. D10-a — đối chiếu holding trước khi phân tích tiếp thanh toán

### 21.1. D09 và thứ tự bóc tách D10

Owner yêu cầu tiếp tục plan tuần tự, gặp phát hiện/đề xuất khác hoặc mơ hồ/mâu thuẫn phải dừng hỏi. Đọc lại 20.5–20.6: D09 giữ nguyên kế hoạch tái hiện → service read-only transaction/fetch batch đúng center → regression → clean test → bàn giao theo SHA; chưa phát hiện điểm mới phải đổi phương án trong lượt rà này. Chưa sửa BE/test/live.

Đề xuất thứ tự để phân tích riêng từng vấn đề D10, chưa coi là quyết định nghiệp vụ: (a) thời gian giữ thanh toán và nguồn cấu hình; (b) Invoice 3/điều kiện payable và nguồn gốc fixture; (c) nút Pay/đếm ngược/lỗi trên FE; (d) vai trò Return/IPN và kiểm chữ ký/amount/idempotency; (e) tiền về sau hết giữ chỗ và quy trình đối soát. Môi trường/SHA/quyền mutation thuộc D11, không tự cho phép ghi DB từ việc phân tích D10. Hiện dừng ở (a), chưa chốt (b)–(e) hoặc chuyển D11–D19.

### 21.2. Mâu thuẫn đã thấy tại D10-a

| Nguồn | Nội dung đang ghi |
|---|---|
| AGENTS §2.6 và checkpoint 04 mục 0.2 | Chuẩn giữ thanh toán 5 phút |
| docs 01 mục 12.4 | Giữ cấu hình hiện tại, chưa chọn lại 5/15 phút |
| docs 02 mục 7 bước 5 | Giữ cấu hình hiện tại khi 5/15 phút chưa được chọn lại |
| application.properties / PaymentProperties | Đọc `payment.expire-minutes`, mặc định 5; env ưu tiên `SCMS_PAYMENT_EXPIRE_MINUTES`, sau đó `PAYMENT_EXPIRE_MINUTES` |

AGENTS nêu tên `scms.payment.expire-minutes` nhưng source hiện đọc `payment.expire-minutes`; chưa có căn cứ coi hai key là alias. Đây là sai khác tên cấu hình phải ghi rõ khi triển khai. Chưa đọc giá trị .env hoặc cấu hình Render để suy thời lượng live; mặc định source không chứng minh giá trị deployment.

**Đề xuất A:** xác nhận 5 phút là chuẩn đích; coi câu “chưa chọn lại 5/15” là ghi chú lịch sử cần được đánh dấu thay thế sau xác nhận. Dùng `expiresAt` BE làm deadline chung, không reset thời gian khi mở lại modal/nhấn Pay; việc gọi pay-online hiện chỉ đọc và không kéo deadline. Kiểm deadline tại BE kể cả job giải phóng chạy trễ. Khi tới bước sửa sẽ đồng bộ tên key/documentation theo cấu hình thật và kiểm env đích, chưa tự đổi env hoặc thêm alias lúc này.

**Lựa chọn B:** nếu Owner muốn 15 phút, cần chốt đây là thay đổi policy so với AGENTS/checkpoint và đồng bộ config/contract/test tương ứng. Chưa coi lựa chọn B đã được chấp thuận hoặc live đang dùng 15 phút.

**Câu hỏi đang chờ:** xác nhận giữ chuẩn 5 phút theo A hay muốn đổi sang 15 phút theo B? Dừng tại mâu thuẫn này theo yêu cầu Owner và AGENTS §2.9; chưa kết luận Invoice 3 hết hạn/null deadline, chưa thay amount/status/paymentMethod/expiry để ép thanh toán, chưa triển khai quyết định holding mới. Lượt này chỉ đối chiếu source/docs và cập nhật kế hoạch, không chạy Maven/DB/deploy.

### 21.3. D10-a đã chốt — 5 phút

Owner trả lời “Đồng ý 5 phút”. Chuẩn giữ thanh toán là 5 phút; không hỏi lại 5/15. Các câu chờ chọn ở 21.2 là lịch sử trước quyết định. Kế hoạch dùng expiresAt BE, không kéo deadline khi mở lại/nhấn Pay, giữ kiểm hạn tại BE như phương án A. Quyết định này không cho phép tính lại deadline hóa đơn cũ, sửa env Render hoặc cấp quyền mutation. Source hiện đọc payment.expire-minutes; việc đồng bộ tên key/tài liệu và kiểm override môi trường sẽ thực hiện khi triển khai, chưa đổi code/config.

## 22. D10-b — Invoice 3 Pending nhưng không thanh toán online được

### 22.1. Bằng chứng và hành vi source

- Dữ liệu FE đã lưu ở mục 4: Invoice ID 3 / INV-2026-000046, Pending, paymentMethod=null, expiresAt=null; Owner báo POST pay-online 409. Đây là bằng chứng cũ, chưa đọc DB/probe mới hoặc xác nhận SHA Render trong lượt này.
- InvoiceController chỉ cho ROLE_MEMBER gọi POST /api/invoices/{id}/pay-online. InvoiceService kiểm chủ hóa đơn trước (khác chủ trả 404), sau đó yêu cầu paymentMethod đúng VNPay, status Pending, và chưa expired; vi phạm trả InvoiceConflictException/409 “Invoice is not payable online”.
- expired(invoice) hiện chỉ true khi expiresAt khác null và <= now(clock). Null deadline không bị coi expired. Với snapshot FE, null paymentMethod đủ làm nhánh 409 chạy nếu SHA live cùng logic; chưa kết luận đây là nguyên nhân live duy nhất.
- newInvoice của source hiện tại chỉ nhận Cash/VNPay, chuẩn hóa method, đặt Pending/createdAt và expiresAt=now+config phút. Do đó không coi snapshot thiếu cả method/deadline là đầu ra bình thường của đường tạo mới này. Schema cho hai cột null; chưa chứng minh nguồn gốc seed/import/legacy hay phiên bản khác. Không suy invoiceCode là bằng chứng nguồn tạo.

### 22.2. Phát hiện cần tách và điểm dừng

1. **D10-b1 — hóa đơn thiếu dữ liệu thanh toán:** Pending không đồng nghĩa có thể trả online. Không tự chọn VNPay thay cho method=null hoặc tự đặt hạn now+5 trên Invoice 3.
2. **D10-b2 — null deadline trong guard:** Invoice Pending/VNPay/expiresAt=null vượt guard vì expired trả false; nhưng VnPayGateway.createPaymentUrl gọi expiresAt.atZone sẽ lỗi khi null, chưa trả được URL (đính chính sau khi đọc gateway). Đây là khoảng trống validation so với chuẩn giữ 5 phút; không chỉ đổi null method là xử lý đủ. Guard và chính sách legacy cần thống nhất trước sửa, test riêng null deadline.
3. **D10-c — nút Pay FE:** phải phân biệt trạng thái Pending với điều kiện được trả online, không gọi lỗi 409 là lỗi Sandbox chỉ dựa status. Chưa kiểm source đúng branch FE nên chưa kết luận FE đang implement điều kiện nào.

**Đề xuất A:** hóa đơn Pending thiếu method hoặc deadline được hiển thị “Cần kiểm tra dữ liệu thanh toán”, chặn tạo URL online khi thiếu một trong hai. Giữ chứng từ/amount và lịch sử, không tự backfill hoặc kéo hạn. Đối soát Invoice 3 cùng quyền/liên kết trước khi lập cách hủy đơn cũ và tạo đơn mới đúng luồng khi đủ điều kiện; test VNPay bằng đơn mới đầy đủ dữ liệu trên môi trường được phép. Không tự tạo lại/thu lần hai hay thực hiện mutation từ quyết định phân tích này. Thiết kế thông báo/lỗi cụ thể sẽ ghi API thật khi code, chưa thêm status DB mới.

**Lựa chọn B:** cho phép khôi phục thanh toán ngay trên hóa đơn thiếu dữ liệu sau khi người có quyền xác nhận phương thức và cấp một cửa sổ mới. Cần bổ sung quyền/audit/kiểm giá snapshot, liên kết và tài nguyên còn hợp lệ, chống callback cũ/race; đây là workflow mới, chưa có trong source và không thể coi là sửa mỗi nút Pay.

**Đang chờ Owner chọn A hay B.** Đề xuất A vì không tự hồi sinh giữ chỗ/đổi chứng từ cũ. Dừng tại đây theo yêu cầu phân tích tuần tự; chưa sang Return/IPN/tiền về muộn hoặc chốt UX FE chi tiết. Sau khi chốt, lập regression với Clock cố định: method null/Cash/VNPay, deadline null/trước/đúng/sau now, trạng thái khác Pending, sai owner; đơn hợp lệ tạo URL không đổi expiresAt. Chưa sửa BE/test, chưa Maven/DB/deploy.

### 22.3. D10-b đã chốt A — đối soát hóa đơn thiếu dữ liệu, không mở lại đơn cũ

Owner chọn “A” sau phần giải thích ngắn. Chặn tạo URL online khi Pending thiếu method hoặc deadline; hiển thị cần kiểm tra dữ liệu thanh toán. Giữ hóa đơn/amount/lịch sử, không tự bổ sung VNPay/hạn now+5 hoặc mở lại giữ chỗ. Kiểm quyền/liên kết/tình trạng giao dịch trước khi xử lý hủy đơn cũ và tạo đơn mới đúng luồng nếu đủ điều kiện; không tự thu lần hai, không xóa chứng từ. Không chọn workflow B khôi phục thanh toán trên hóa đơn cũ. Phương án A ở 22.2 là mục tiêu đã chốt, câu chờ chọn cũ là lịch sử.

Khi triển khai, bổ sung guard riêng cho thiếu method/deadline, không đổi expired(null) toàn cục khi chưa đánh giá các caller/Cash. Regression theo 22.2 và clean test bắt buộc sau sửa BE. Đây là chốt kế hoạch; chưa thay code/Invoice/config, chưa có API lỗi mới để FE sử dụng.

## 23. D10-c — nút Pay và diễn giải dữ liệu FE: dừng xác nhận nguồn FE

### 23.1. Phát hiện ở bản FE trong workspace

Đọc `FE_Project-main/src/api/invoiceMappers.js`: normalizePaymentMethod trả `cash` khi method rỗng/null; mapInvoice vẫn giữ rawPaymentMethod=null và expiresAt=null. Do đó dữ liệu thiếu phương thức có thể được biểu diễn thành Cash ở trường đã normalize, khác thông điệp “Cần kiểm tra dữ liệu thanh toán” theo D10-b A. Đây là hành vi mapper local, chưa chứng minh màn Pay dùng trường nào hoặc đang chạy cùng bản này.

`git -C FE_Project-main rev-parse --show-toplevel` trả D:/Projects/SWP391, không cho một checkout FE riêng. Chưa xác nhận FE_Project-main là source FE hiện hành đã tạo báo cáo ngày 24/10 hay bản tham khảo trong repo BE. Không kết luận bug trên deployment FE từ file local, không sửa FE/test để ép khớp.

### 23.2. Hướng xử lý và câu hỏi trước khi tiếp tục

Hướng handoff theo D10-b đã chốt: giữ được trạng thái thiếu dữ liệu, không suy null method là Cash/VNPay; Pay online dựa method thật/đúng owner/Pending/deadline hợp lệ, BE quyết định cuối. Countdown lấy deadline BE; trường thiếu không tự khởi động 5 phút. Chưa chọn field/error code mới hoặc cơ chế đồng bộ đồng hồ riêng.

**Cần Owner xác nhận:** FE_Project-main trong workspace này là bản FE hiện hành để đối chiếu, hay chỉ là bản tham khảo và D10-c nên lập yêu cầu bàn giao cho FE đối chiếu trên branch của họ? Dừng tại nguồn chưa rõ theo yêu cầu Owner; chưa phân tích tiếp hành vi nút Pay/Return/IPN. Không yêu cầu gửi lại log Invoice 3. Lượt này chỉ đọc source/docs; không BE/FE mutation, Maven, DB hoặc deploy.

### 23.3. Owner xác nhận nguồn FE — bản tham khảo rất cũ

Owner xác nhận FE_Project-main là bản tham khảo từ rất lâu, đã out-date nặng. Không dùng source/test/mapper của thư mục này để kết luận hành vi FE hiện tại, không sửa thư mục đó cho đợt BE này. Phát hiện null→cash ở 23.1 chỉ thuộc bản cũ, không coi là bug FE đang chạy. Không hỏi lại nguồn này.

D10-c chuyển thành handoff yêu cầu nhóm FE tự đối chiếu source hiện hành: không suy method null thành Cash/VNPay; Pending không đồng nghĩa payable; hiển thị thiếu dữ liệu theo D10-b A; countdown theo expiresAt BE, không reset khi mở lại/nhấn Pay; khi 409 tải lại hóa đơn và báo đúng trạng thái. BE vẫn kiểm owner/method/status/deadline cuối cùng. FE cung cấp phiên bản và bằng chứng màn/API thật khi tích hợp. Chưa xác nhận FE đã đáp ứng, chưa cần thêm API/field dự kiến chỉ để thay thế việc kiểm đúng source.

## 24. D10-d1 — vai trò Return và IPN: cần chốt trước sửa

### 24.1. Source và tài liệu chính thức

- InvoiceController.vnpayIpn và vnpayReturn đều gọi InvoiceService.processVnpayResult. Vì thế Return qua trình duyệt hiện cũng có thể đổi trạng thái Invoice/kích hoạt quyền học, không chỉ điều hướng FE.
- Service hiện kiểm chữ ký/merchant bằng VnPayGateway, tra invoice, khóa center và invoice, kiểm method/amount, chặn xử lý lại Paid. Có các chốt kiểm này; không mô tả Return là tin mọi query client gửi lên. Tuy nhiên đây vẫn là hai đường cập nhật chung cần quyết định vai trò rõ.
- [FAQ VNPAY chính thức](https://sandbox.vnpayment.vn/apis/docs/faqs/) phân vai Return hiển thị kết quả, IPN cập nhật kết quả tại máy chủ. [Hướng dẫn tích hợp PAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html) mô tả IPN nhận/kiểm/cập nhật và trả RspCode/Message cho VNPAY. Đã tra tài liệu chính thức trong lượt này; chưa kiểm cấu hình IPN của merchant/deployment.

### 24.2. Hai lựa chọn và đề xuất

**A — đề xuất:** chỉ IPN hợp lệ cập nhật Invoice/quyền học. Return kiểm thông tin cần thiết rồi điều hướng màn kết quả, không thay trạng thái/credit. FE đọc hóa đơn qua API xác thực để biết trạng thái BE đã ghi; Return tới trước IPN thì hiển thị “Đang xác nhận thanh toán”, không coi query redirect là xác nhận Paid. Khi IPN chậm/mất thì kiểm lại/đối soát riêng, không tự mở quyền học từ trình duyệt. Chi tiết thời gian retry/poll và đối soát chưa được tự chốt tại đây.

**B:** giữ cả Return và IPN được cập nhật sau kiểm hợp lệ, dùng cùng transaction/locking/idempotency. Có thể cập nhật sớm khi người dùng quay về trước IPN, nhưng tiếp tục giữ hai đường ghi và khác phân vai trong FAQ chính thức; cần chủ động chấp thuận và test đầy đủ các thứ tự, không chọn chỉ vì source đang làm vậy.

**Câu hỏi đang chờ:** chọn A (IPN cập nhật, Return chỉ điều hướng/hiển thị) hay B (giữ cả hai cập nhật có kiểm)? Dừng tại đây theo yêu cầu Owner trước khi phân tích/chốt các nhánh tiếp theo.

Ghi nhận để tách bước sau, chưa giải quyết gộp: controller Return hiện chuyển result.code thành status; code này thuộc phản hồi xử lý, không tự tương đương thanh toán thành công (service có thể trả 00 khi đã nhận kết quả thất bại, hoặc 02 khi đã Paid). Cần làm rõ contract kết quả FE sau quyết định vai trò; không đổi error/field chỉ trong docs như đã triển khai. D10-e tiền về muộn/đối soát còn riêng. Chưa sửa BE/test/config/DB/deploy; clean test sẽ chạy sau khi có sửa BE.

### 24.3. D10-d1 đã chốt A — IPN cập nhật, Return hiển thị

Owner xác nhận “OK. chốt A” sau khi làm rõ tác động FE. Chỉ IPN hợp lệ được cập nhật thanh toán/quyền học trong hai đường callback này; Return không ghi trạng thái Invoice/credit, chỉ kiểm thông tin cần thiết và điều hướng màn kết quả. Không xóa luồng xác nhận Cash riêng. Giữ kiểm chữ ký/merchant/amount, khóa DB/transaction và idempotency cho IPN. Return có chữ ký hợp lệ cũng không thay thế IPN theo mô hình đã chọn.

FE hiện hành cần đọc Invoice từ API BE có xác thực/owner; nếu đang dựa query redirect để báo Paid thì phải chỉnh, nếu đã đọc API thì kiểm thêm trạng thái chờ IPN. Handoff cho nhóm FE; không sửa bản FE_Project-main cũ. B không bị quy là tự động mất an toàn: source đã có các kiểm tra, nhưng Owner chọn phân vai A. Chưa đổi code/config/merchant IPN hoặc deploy; câu chờ A/B ở 24.2 là lịch sử, không hỏi lại.

Kế hoạch test khi sửa: Return hợp lệ/không hợp lệ không làm đổi Invoice/quyền/credit; IPN hợp lệ không cần browser Return vẫn cập nhật; Return trước/sau IPN, callback lặp/đồng thời không nhân quyền. Chạy clean test sau sửa BE và xác minh HTTPS/IPN đúng môi trường trước rollout; không dùng test mock để chứng nhận merchant đã cấu hình.

## 25. D10-d2 — màn kết quả khi IPN chưa tới

### 25.1. API sẵn có và điểm không được suy diễn

BE đã có GET /api/members/me/invoices/{id}, yêu cầu ROLE_MEMBER, kiểm đúng chủ bằng Authentication; khác chủ hoặc không có hóa đơn trả 404. InvoiceResponse có paymentStatus, reconciliationRequired, expiresAt. Có thể dùng route này để đọc kết quả, chưa cần tạo một API công khai đọc hóa đơn bằng ID/mã trên redirect. Phiên hết hạn thì đăng nhập lại; không mở dữ liệu hóa đơn cho người chưa xác thực để tiện màn kết quả.

Source Return hiện gửi status=result.code, không phải trạng thái thanh toán: 00 có thể chỉ là đã xử lý thông báo, 02 có thể là đơn đã Paid. Đây là lỗi nghĩa cần tránh trong contract/FE khi sửa theo A; query URL chỉ giúp điều hướng, không có quyền đổi trạng thái hay xác nhận đã thu tiền. Pending khi Return đến trước IPN không đồng nghĩa giao dịch thất bại. Thời gian FE chờ/refresh hết cũng không tự là Failed hoặc kéo dài holding.

### 25.2. Đề xuất UX chờ và điểm cần chọn

**A — đề xuất:** màn kết quả tự đọc lại hóa đơn trong một khoảng chờ hữu hạn khi BE còn Pending. Trong lúc đó hiển thị “Đang xác nhận thanh toán”, không mời trả lại ngay. Hết khoảng chờ mà chưa có kết quả thì dừng tự đọc, hiển thị chưa xác nhận và cho nút “Kiểm tra lại”/xem hóa đơn; không đổi DB sang Failed, không tự tạo đơn khác. Nhịp/số lần/thời lượng sẽ đặc tả trong handoff triển khai, chưa tự gán con số hoặc coi là một deadline thanh toán mới.

**B:** đọc một lần khi mở trang; nếu chưa xác nhận thì người dùng bấm “Kiểm tra lại” thủ công. Ít request hơn, nhưng có thể phải bấm nhiều lần dù IPN vừa tới.

**Đang chờ Owner chọn A hay B** cho việc tự cập nhật màn chờ. Trạng thái hiển thị phải dựa Invoice BE trong cả hai hướng: Paid và reconciliationRequired=false mới báo thanh toán đã xác nhận; Paid và reconciliationRequired=true báo đã ghi nhận tiền nhưng cần đối soát, không hứa quyền học đã kích hoạt; Pending còn chờ. Failed/Cancelled/Expired phải hiển thị đúng trạng thái BE và không suy chắc chắn “không trừ tiền” chỉ từ trạng thái đơn. Quy trình tiền về muộn/đối soát chi tiết thuộc D10-e, chưa chốt ở bước này.

Sau khi chọn, kiểm ca Return/IPN đảo thứ tự, query status giả, hết phiên đăng nhập, sai owner, IPN chậm và hết khoảng chờ không tự thu lại; API/field/error thật sẽ cập nhật tại docs 01 khi code. Dừng tại lựa chọn UX này; chưa sửa BE/FE/test, không Maven/DB/deploy.

### 25.3. D10-d2 đã chốt A — tự cập nhật hữu hạn, thuận tiện thêm email sau

Owner chọn A và yêu cầu dễ kết hợp thông báo email Member sau này. FE tự đọc lại Invoice trong khoảng chờ hữu hạn khi Pending, sau đó cho Kiểm tra lại/xem hóa đơn; không tự kết luận thất bại/thu lại/kéo deadline. Nhịp và giới hạn sẽ đặc tả kỹ thuật khi triển khai, không hỏi lại lựa chọn tự cập nhật. FE hiện hành do nhóm FE đối chiếu và tích hợp theo handoff.

**Yêu cầu mở rộng email:** kết quả nghiệp vụ lưu tại BE là nguồn chung cho màn hình và thông báo tương lai, không gửi mail từ FE/Return hoặc mỗi lượt polling. Source có auth.EmailService dùng JavaMailSender, NotificationService ghi thông báo trong transaction REQUIRES_NEW; chưa có luồng email thanh toán và không tự coi các service này đã đủ cơ chế sau commit/chống trùng.

Kế hoạch kỹ thuật: tách thông tin kết quả thanh toán ra điểm phát sự kiện/thông báo, chỉ chuyển thông báo ra ngoài sau transaction thanh toán commit; callback lặp không tạo thêm cùng thông báo nghiệp vụ, gửi mail lỗi không rollback Paid/kích hoạt. Khi triển khai email thật cần lưu công việc gửi để retry/chống mất (ví dụ outbox cùng transaction), khóa chống trùng theo Invoice/loại kết quả, đối chiếu email Member từ dữ liệu BE. Không dùng listener in-memory đơn thuần để hứa bảo đảm gửi khi tiến trình dừng. Chưa chốt migration/outbox/provider/template hoặc bật gửi mail thanh toán trong giai đoạn plan này. Thông báo đã xác nhận và cần đối soát phải khác nội dung, không hứa quyền học khi chưa được cấp.

Test mục tiêu sau code: polling/Return không tạo mail; callback lặp không nhân thông báo; rollback thanh toán không gửi mail thành công; lỗi gửi không đổi kết quả đã commit; email tương lai có retry không gửi trùng theo cùng sự kiện. Chưa sửa BE/FE/config/schema hoặc gửi email.

## 26. D10-e1 — IPN tới muộn hay khách thanh toán muộn?

### 26.1. Source hiện tại và ví dụ cần phân biệt

InvoiceService.processVnpayResult sau khi kiểm hợp lệ dùng `!Pending || expired(invoice)` tại giờ xử lý BE để chọn nhánh tiền về muộn; expired so expiresAt với now(clock). Source chưa dùng vnp_PayDate để xác định khách thanh toán trong hạn. Nhánh muộn ghi Paid/reconciliationRequired=true, không tự kích hoạt gói/suất đã giải phóng. Có khóa center/invoice và guard Paid chống xử lý lặp. Đây là hành vi source, chưa probe live hoặc đối soát giao dịch thật.

Ví dụ hóa đơn hết giữ lúc 10:05: khách được gateway xác nhận thanh toán 10:04, IPN tới BE lúc 10:06. Đây là **thanh toán trong hạn nhưng thông báo muộn**, khác khách thực sự thanh toán 10:06. Job có thể đã giải phóng suất lúc IPN tới; kể cả gateway xác nhận trả trong hạn cũng không được tự chiếm chỗ đã cấp cho người khác. Deadline 5 phút vẫn giữ nguyên trong mọi lựa chọn, không kéo thành 6 phút.

### 26.2. Lựa chọn cần chốt trước xử lý đối soát

**A — đề xuất cho MVP, giữ nguyên tắc source:** tại lúc BE xử lý, đã qua deadline hoặc đơn không còn Pending thì ghi nhận tiền/cần đối soát, không tự kích hoạt; kể cả khách trả trong hạn nhưng IPN tới muộn. Dễ giữ nhất quán việc giải phóng chỗ; bất lợi là khách có thể phải chờ hỗ trợ do chậm thông báo chứ không do trả trễ. Lưu thêm thời điểm gateway đã xác minh làm chứng cứ cho người xử lý, không lấy thời gian browser khai.

**B:** phân biệt thời điểm gateway xác nhận trả tiền với thời điểm IPN tới; chỉ cân nhắc tự kích hoạt khi chứng cứ gateway hợp lệ cho thấy trả trong hạn và quyền giữ/tài nguyên vẫn còn hợp lệ dưới lock. Nếu đã giải phóng/cancel/Expired hoặc không đủ điều kiện thì vẫn cần đối soát, không tự mở lại đơn. Cần đặc tả parse/zone/validation timestamp và điều kiện eligibility, không chỉ thay now bằng vnp_PayDate. Không dùng lựa chọn này để sửa deadline hoặc phục hồi suất đã mất.

**Đang chờ Owner chọn A hay B.** Chỉ chốt cách phân loại nhận tiền muộn ở bước này; ai đối soát, hoàn tiền hay cấp lại quyền, cách ghi lịch sử/thông báo sẽ tách D10-e2 sau đó. Chưa triển khai, không gọi gateway/ghi DB/gửi email/deploy. Yêu cầu email tương lai đã ghi ở 25.3 không bị coi là thông báo đã gửi.

### 26.3. Cân nhắc lại theo độ phức tạp và hệ thống hiện tại — chưa chọn thay Owner

Owner yêu cầu đánh giá lại, chưa chọn A/B. Đã đối chiếu InvoiceService, PaymentExpirationJob và InvoiceServiceTest: có sẵn nhánh Paid/reconciliationRequired không kích hoạt và test callback muộn; đây là test source hiện có, chưa chạy mới. A sát cấu trúc hiện tại hơn, vẫn cần bổ sung kiểm biên/race/luồng đối soát chứ không coi là đã đủ nghiệm thu.

Điểm cần làm rõ của B: sau deadline, quyền giữ chỗ đã hết theo policy dù job chưa kịp đổi Pending. Nếu chỉ dựa “vẫn Pending” để coi còn quyền giữ, kết quả sẽ phụ thuộc job chạy nhanh/chậm. Vì vậy B thực chất thêm ngoại lệ nhận thanh toán đúng hạn tại gateway sau khi BE hết quyền giữ, kèm đánh giá lại tài nguyên/nghiệp vụ dưới lock; không phải chỉ parse thêm vnp_PayDate. Không thể mặc định các hàm activateSubscription/activateClassEnrollment hiện tại đã đủ guard cho ngoại lệ này. Normal cần sĩ số/Enrollment/lịch còn phù hợp; Advanced mục tiêu D03 còn năng lực Primary Coach/cam kết đăng ký. B vẫn cần đối soát cho đơn đã giải phóng, nên không loại bỏ workflow D10-e2.

**Khuyến nghị sau cân nhắc vẫn là A cho giai đoạn hiện tại:** một deadline nhất quán giữa thanh toán và giải phóng, ít nhánh tranh chấp hơn khi D01–D08 còn chưa triển khai. Đánh đổi là Member đã trả đúng hạn có thể phải chờ xử lý vì IPN trễ; phải hiển thị “Đã ghi nhận tiền, cần xử lý đăng ký”, giữ chứng cứ giờ gateway/giờ nhận và có trách nhiệm xử lý rõ ở D10-e2. Không gọi đây là khách thanh toán trễ hoặc báo thất bại/mời trả lần hai. Chưa biết tần suất IPN trễ thực tế, không khẳng định trường hợp này hiếm.

B phù hợp để xem xét sau nếu dữ liệu vận hành cho thấy nhiều thanh toán đúng hạn bị đẩy đối soát và hệ thống đã có cơ chế kiểm lại quyền/tài nguyên đầy đủ. Chưa chốt A theo suy luận này; giữ câu hỏi 26.2. Chỉ thêm đánh giá tài liệu, chưa sửa BE/test/config/DB/deploy.

### 26.4. D10-e1 đã chốt A — quá hạn tại lúc xử lý thì đối soát

Owner xác nhận “Chốt A” sau đánh giá ở 26.3. IPN thành công hợp lệ nhưng tại lúc BE xử lý đã tới/quá expiresAt hoặc đơn không còn Pending thì ghi nhận tiền Paid/reconciliationRequired=true, không tự kích hoạt. Đơn đã Paid xử lý idempotent, không phát sinh thu/cấp quyền/thông báo lần hai. Không dùng vnp_PayDate để tự hồi sinh quyền giữ đã hết; có thể lưu giờ gateway đã xác minh và giờ BE nhận/xử lý làm chứng cứ.

Không coi IPN muộn là khách trả trễ; Member thấy “Đã ghi nhận tiền, đăng ký cần được xử lý”, không báo thất bại/mời trả lần nữa. Không kéo deadline 5 phút và không lấy lại suất đã cấp người khác. A được chốt cho plan, chưa chứng nhận nhánh hiện có đủ toàn bộ validation/test/UX hoặc đã deploy. Câu chờ chọn ở 26.2–26.3 là lịch sử.

## 27. D10-e2a — người chịu trách nhiệm xử lý giao dịch cần đối soát

### 27.1. Phần đang thiếu trong source

Rà invoice controller/service: đã có ghi Paid/reconciliationRequired và audit; chưa thấy API nghiệp vụ giải quyết hồ sơ đối soát, phê duyệt cấp lại quyền hoặc hoàn tiền. confirm-cash chỉ dành cho hóa đơn Cash Pending, không dùng để xác nhận lại tiền VNPay đã ghi nhận. API đổi status gói không phải đường giải quyết đối soát; không mở Active trực tiếp hoặc bỏ cờ reconciliationRequired để xem như xử lý xong.

Tách việc cần chốt theo thứ tự: (a) ai chịu trách nhiệm/duyệt; (b) được chọn cấp quyền hợp lệ hay hoàn tiền và Member đồng ý thế nào; (c) cách thực hiện tiền hoàn, audit/trạng thái hoàn tất/chống lặp/thông báo. Chưa chọn (b)/(c) từ lựa chọn (a).

### 27.2. Lựa chọn trách nhiệm — đang chờ

**A — đề xuất:** Manager cùng center chịu trách nhiệm và duyệt phương án cuối. Receptionist có quyền được cấp có thể tiếp nhận thông tin/hướng dẫn Member, nhưng không tự cấp lại quyền hoặc xác nhận hoàn tiền/đóng hồ sơ. BE kiểm role/authority và center; không chỉ ẩn nút FE. Quyền Admin xử lý thay nếu cần sẽ đối chiếu riêng, không tự mở toàn hệ thống từ lựa chọn này.

**B:** Receptionist cùng center được tự duyệt và hoàn tất đối soát trong phạm vi được cấp quyền. Ít phải chờ Manager hơn, nhưng mở thêm quyền quyết định liên quan tiền/quyền học; cần chốt rõ giới hạn nghiệp vụ trước code.

**Câu hỏi:** chọn A (Manager duyệt, Receptionist hỗ trợ) hay B (Receptionist cũng được tự quyết)? Đề xuất A vì khác thao tác thu Cash thông thường và liên quan tiền đã nhận nhưng quyền học chưa cấp. Chưa cho phép hoàn tiền, tự tạo Invoice mới/thu lại hoặc re-activate từ quyết định e1. Dừng tại trách nhiệm này, chưa sang chọn phương án hoàn/cấp quyền. Chỉ source/docs, chưa BE/test/Maven/DB/email/deploy.

### 27.3. D10-e2a đã chốt A — dùng đúng tên role CenterManager

Owner chọn A và hỏi lại cụm “cùng center”. Đính chính cách diễn đạt: role là CenterManager (Spring authority ROLE_CENTERMANAGER), không có role Center được đề xuất. Quyết định là CenterManager duyệt phương án đối soát, Receptionist tiếp nhận/hỗ trợ, không tự quyết cấp quyền/hoàn tiền/đóng hồ sơ. Không hỏi lại A/B.

Cụm center trước đó nói tới phạm vi dữ liệu sẵn có trong source: User.centerId ánh xạ users.center_id và CenterContext.currentCenterId kiểm tài khoản có center assignment (ngoại lệ Admin). Đây là thuộc tính/phạm vi dữ liệu, không phải role khác, không tự chứng minh yêu cầu kinh doanh vận hành nhiều chi nhánh. Giữ kiểm phạm vi hiện có khi chuẩn bị sửa; không thêm mô hình nhiều cơ sở, không bỏ centerId/guard chỉ từ câu hỏi thuật ngữ của Owner. Nếu Owner muốn đổi mô hình phạm vi thì tách thành vấn đề riêng cần đối chiếu, chưa suy thành lệnh thay đổi.

Chưa chuyển sang chốt cách cấp quyền/hoàn tiền ở bước giải thích này. Chỉ sửa docs, không code/test/Maven/DB/deploy.

## 28. D10-e2b — chọn kết quả xử lý tiền đã nhận nhưng chưa cấp quyền

### 28.1. Ranh giới đã chốt và phần thiếu

CenterManager duyệt, Receptionist hỗ trợ (27.3). IPN muộn không tự kích hoạt (26.4), nhưng không có nghĩa cấm CenterManager giải quyết sau kiểm tra; cần chọn rõ được phép cấp quyền có kiểm soát hay chỉ hoàn tiền. Source chưa có workflow xử lý này; Invoice hiện có amount/paymentStatus/reconciliationRequired nhưng chưa có hồ sơ kết quả hoàn/cấp quyền riêng.

Không dùng confirm-cash để thu lại giao dịch VNPay đã nhận, không đổi Invoice.amount hoặc tạo Invoice Paid thứ hai để ghi cùng khoản tiền. Không dùng update-status gói như bằng chứng đã kiểm đủ điều kiện. Trường hợp thiếu method/deadline chưa thu tiền ở D10-b khác giao dịch Paid cần đối soát ở đây; không trộn hai hướng xử lý.

### 28.2. Hai hướng và đề xuất — đang chờ Owner

**A — đề xuất:** CenterManager trao đổi với Member rồi xử lý một trong hai kết quả:

- Nếu Member vẫn muốn học và BE kiểm lại đầy đủ điều kiện cho **đúng đăng ký/sản phẩm đã trả tiền**, cấp quyền trong luồng đối soát riêng có khóa/transaction/audit, gắn khoản Paid gốc, không thu/nạp lần hai. Normal phải còn chỗ, lịch còn phù hợp/đủ điều kiện; Advanced phải còn điều kiện môn/Primary Coach/năng lực theo D03. Không tự chuyển sang môn/lớp/gói khác hoặc lấy suất của người khác. Credit cấp đúng một lần; hạn Advanced giữ nguyên tắc 1080 giờ từ Paid hợp lệ, không reset từ giờ Manager duyệt. Nếu không đủ hạn/không đủ điều kiện thì không ép cấp; việc xác định timestamp Paid chuẩn sẽ đặc tả nhất quán khi triển khai, không cho client tự khai.
- Nếu Member không muốn tiếp tục hoặc không còn điều kiện cấp quyền hợp lệ thì chuyển hướng **hoàn đủ khoản đã nhận của giao dịch này**, không cấp quyền. Cách hoàn qua đâu và khi nào được coi đã hoàn tách ở D10-e2c; chưa tự dùng API refund hoặc ghi hoàn tất.

Manager ghi nhận đã trao đổi, Member đồng ý phương án tương ứng, actor/time/lý do và tác động; không thêm vòng Member duyệt online mặc định. Chọn A là cho phép hướng xử lý có kiểm soát, không mở tự kích hoạt từ IPN. Mọi mở rộng đổi sản phẩm/bù chênh lệch/gia hạn vì chờ đối soát phải tách quyết định mới, không kế thừa ngoại lệ lỗi trung tâm D08 tùy ý.

**B — đơn giản hơn về cấp quyền:** mọi giao dịch Paid cần đối soát vì hết giữ chỗ đều xử lý hoàn đủ khoản nhận; không cấp quyền từ giao dịch này. Nếu Member muốn học thì đăng ký/thanh toán lại sau, không tự tạo thêm đơn thu tiền trong khi khoản cũ còn chưa xử lý. Ít nhánh revalidation/kích hoạt hơn nhưng tăng việc hoàn tiền và Member phải làm lại giao dịch. Vẫn phải có workflow hoàn có audit/chống lặp, không coi đây là không cần code.

**Đang chờ chọn A hay B.** Khuyến nghị A để Member không buộc hoàn/đăng ký lại khi đúng sản phẩm vẫn đủ điều kiện, đồng thời giữ Manager duyệt thay vì tự hồi sinh đơn. A phức tạp hơn B ở nhánh kiểm và cấp quyền; không mô tả là chỉ thêm nút. Test mục tiêu: khoản tiền không tính doanh thu hai lần, không cấp trùng/hoàn trùng hoặc vừa cấp vừa hoàn; sai role/owner/phạm vi bị chặn, tranh chấp chỗ rollback, audit và thông báo sau commit. Chưa triển khai code/API/migration/test/Maven, không DB/email/deploy.

### 28.3. Owner yêu cầu đánh giá độ phức tạp và gộp quyết định liên quan

Owner chưa chọn A/B ở 28.2; yêu cầu giải thích phần kiểm/cấp quyền, ưu tiên đơn giản và bảo đảm project. Từ đây gộp các quyết định thiết kế phụ thuộc nhau thành phương án đầy đủ (có thể 3–4 hướng khi hữu ích), không hỏi quá vụn. Vẫn dừng khi có mơ hồ/mâu thuẫn nghiệp vụ thật; không coi yêu cầu đánh giá là lựa chọn B.

**Chi phí nhánh cấp quyền A:** phải có thao tác đối soát riêng thay cho update-status. Trong transaction/lock: kiểm CenterManager/phạm vi, khóa Invoice và hồ sơ xử lý, chắc chắn tiền đã nhận/còn cần xử lý/chưa hoàn/chưa cấp. Sau đó kiểm lại đăng ký gốc: Normal còn chỗ và lịch đủ điều kiện, không ghi đè Enrollment đã có giao dịch mới; Advanced đúng môn/Primary Coach/năng lực, chưa nhận credit từ khoản khác và còn hạn theo D03. Cấp quyền đúng một lần/liên kết khoản Paid gốc, lưu quyết định/audit, hoàn tất hồ sơ và phát thông báo sau commit. Retry/race không được vừa hoàn vừa cấp. Timestamp Paid chuẩn và việc còn đủ hạn phải nhất quán D03, không tự reset hạn theo giờ duyệt. Source activation hiện chưa chứng nhận đủ các điều kiện mới; D01–D08 còn chưa code nên nhánh này phụ thuộc nhiều phần chưa hoàn thành.

Khóa/transaction có thể tái sử dụng; phần khó là điều kiện đăng ký, liên kết lịch sử và hai hướng loại trừ nhau. Vì thế đánh giá lại khuyến nghị theo ưu tiên Owner: **refund-only với thực hiện ngoài hệ thống có kiểm chứng phù hợp MVP hơn A**. Không khẳng định tốn bao nhiêu ngày khi chưa triển khai/kiểm toàn bộ; không coi test cũ đã bao phủ.

### 28.4. Ba phương án tổng thể thay cho hỏi rời kết quả và kênh hoàn — đang chờ

1. **Chỉ hoàn tiền, thực hiện ngoài hệ thống — đề xuất cho project hiện tại (tương ứng B về kết quả).** CenterManager duyệt; hoàn đủ khoản nhận qua công cụ/kênh hợp lệ ngoài SCMS rồi ghi nhận chứng từ/mã tham chiếu, số tiền, người/thời điểm thực hiện và xác nhận. SCMS giữ khoản Paid/amount gốc, lưu kết quả hoàn riêng, không cấp quyền từ khoản này. Chỉ xác nhận đã hoàn khi có căn cứ; chưa hoàn thì vẫn hiển thị cần xử lý, không đóng bằng một lần click duyệt. Phải có kiểm quyền, audit, khóa/chống xác nhận lặp và báo cáo phân biệt tiền nhận/tiền hoàn; không coi B là không cần code. Không tích hợp API refund trong MVP; không tự đổi sang hoàn Cash hoặc chọn tài khoản nhận tiền từ input không kiểm. Chi tiết biểu mẫu/chứng cứ cụ thể sẽ đặc tả trong phạm vi lựa chọn sau chốt, không giả định đã có API khả dụng.
2. **Chỉ hoàn tiền, tích hợp API hoàn gateway.** Giữ kết quả refund-only nhưng SCMS gửi yêu cầu/tra cứu trạng thái hoàn. Cần tích hợp/kiểm cấu hình gateway, ký request, timeout/retry/đối soát trạng thái chưa rõ và chống hoàn trùng. Tự động hơn phương án 1 nhưng tăng độ phức tạp external integration; chưa có source luồng này. Không thêm tự động cấp quyền.
3. **Cấp quyền có kiểm soát hoặc hoàn ngoài hệ thống — tương ứng A.** Giữ nhánh hoàn như phương án 1, thêm kiểm/cấp quyền tại 28.3 khi Member đồng ý và đủ điều kiện. Linh hoạt nhất về quyền học trong ba hướng, nhưng nhiều phụ thuộc D03/lịch/credit và test nhất. Không tự mở lại đơn từ IPN; CenterManager xử lý riêng.

Mọi hướng giữ D10-e1 đã chốt: IPN muộn ghi tiền/cần đối soát, không tự kích hoạt; giữ D10-e2a CenterManager duyệt, Receptionist hỗ trợ. Email tương lai lấy kết quả đã lưu, phân biệt cần xử lý/đã hoàn/đã cấp nếu hướng đó được chọn; chưa gửi email. Đang chờ Owner chọn 1/2/3; chưa coi hướng 1 được duyệt, chưa sang D11 hoặc triển khai refund. Chỉ cập nhật đánh giá/docs, chưa code/test/Maven/DB/deploy.

### 28.5. D10-e2 chốt hướng 1 — hoàn ngoài SCMS cho MVP, nâng cấp sau

Owner chọn “1” và ưu tiên hoàn tất hệ thống mượt mà trước khi tinh chỉnh/nâng cấp. Chốt kết quả/kênh xử lý theo phương án 1 ở 28.4: CenterManager duyệt hoàn đủ khoản đã nhận, thực hiện ngoài SCMS và lưu bằng chứng/kết quả trong hệ thống; Receptionist hỗ trợ. Giữ hóa đơn Paid và amount gốc, kết quả hoàn riêng gắn giao dịch gốc; chỉ xác nhận đã hoàn khi có chứng cứ, không chỉ dựa quyết định duyệt. Không cấp quyền từ khoản đang đối soát, không thu lần hai hoặc reset hạn. Member muốn học lại đăng ký mới đúng luồng sau xử lý khoản cũ, không tự tạo đơn thu thêm khi đang chờ.

Refund API gateway và cấp lại quyền có kiểm soát là hướng nâng cấp sau, không nằm trong MVP đợt này. Cơ chế lưu hồ sơ hoàn tối thiểu/quyền/audit/chống lặp vẫn thuộc việc cần làm cho phương án 1, không bị hoãn như hai nâng cấp. Không tự thêm workflow nhiều vòng, tích hợp ngân hàng hoặc status DB trước thiết kế implementation. Handoff FE phải phân biệt cần xử lý/đã duyệt hoàn/đã hoàn, không chỉ hiển thị Paid như học được; tên field/status thật sẽ công bố sau code. Các nhánh email tương lai chỉ áp dụng sự kiện nghiệp vụ thuộc phạm vi đã chốt.

D10 đã đủ hướng nghiệp vụ chính cho kế hoạch; chưa code/test/deploy. Các chi tiết kỹ thuật trong phạm vi này triển khai bằng judgment, chỉ hỏi nếu phát hiện mâu thuẫn/quy tắc mới; không chia thêm câu hỏi nhỏ cho từng field. Chuyển D11 về môi trường và bằng chứng tích hợp, chưa tự chạy hoàn tiền hoặc mutation DB.

## 29. D11 — môi trường kiểm thử, dữ liệu và phạm vi được ghi

### 29.1. Bằng chứng hiện có và điểm cần giải quyết

Workspace branch tai/HEAD 8e95759 không chứng minh SHA Render. Migration 07 Owner đã báo chạy Supabase và kiểm kê DB .env trước đây đủ cột/FK/index, nhưng chưa xác nhận DB đó trùng service Render. Đây là bằng chứng lịch sử, chưa kiểm live lại trong lượt này. VNPay Sandbox mô phỏng tiền nhưng đơn/subscription/credit vẫn ghi vào DB ứng dụng. FE_Project-main rất cũ, không dùng build/test đó để chứng nhận FE hiện hành.

D01–D10 sắp sửa có nhiều mutation và schema mới; cần chọn đường kiểm thử trước khi cho E2E tạo/hủy/thu tiền giả lập trên DB đang dùng. Không cần thêm mô hình nhiều chi nhánh. Không coi quyết định chọn môi trường bên dưới là lệnh tạo tài khoản/service, chạy migration hoặc deploy ngay trong giai đoạn plan.

### 29.2. Ba hướng tổng thể — đang chờ Owner

1. **Kiểm thử đầy đủ trên local với DB PostgreSQL riêng, sau đó smoke có kiểm soát trên bản deploy — đề xuất cho quy mô project.** Không dùng .env DB đang chạy làm DB fixture; DB test có dữ liệu riêng. BE/FE hiện hành chạy cùng phiên bản tại môi trường test, kiểm mutation ở đó; sau deploy kiểm CORS/cookie/IPN và smoke bằng tài khoản/fixture được xác định riêng. Ít hạ tầng hơn staging riêng; kết quả local không thay chứng nhận tích hợp Render, mutation cloud vẫn cần phạm vi cụ thể.
2. **Staging riêng: BE deploy riêng + DB riêng + FE hiện hành cấu hình vào staging.** Thuận tiện hai phía chạy E2E chung và kiểm callback HTTPS; thêm công chuẩn bị/cấu hình/duy trì đồng bộ migration/SHA. Không mặc định đã có dịch vụ/DB này hoặc tự phát sinh chi phí.
3. **Dùng bản deploy/DB hiện có với fixture được khoanh rõ.** Ít chuẩn bị hạ tầng, gần cấu hình đang chạy, nhưng dễ trộn dữ liệu demo/giao dịch và ảnh hưởng job/lịch/credit; cần liệt kê bản ghi được tác động, giữ dữ liệu ngoài phạm vi và kế hoạch đối soát, không reset toàn DB. Không đề xuất làm nơi thử chính cho toàn bộ rule mới.

Chung mọi hướng: ghi source/deploy SHA, manifest migration/config không lộ secret, xác nhận DB đích; test BE sửa xong chạy clean test, H2 không thay PG/SQL Server; FE hiện hành có phiên bản/bằng chứng API thật. Không tự bật live tests, tạo đơn Sandbox hay reset schema chỉ từ chữ test. **Cần Owner chọn hướng 1/2/3**; thông tin URL/DB cụ thể sẽ kiểm nguồn sẵn có trước khi hỏi thiếu dữ liệu, chưa yêu cầu gửi secret. Dừng ở chiến lược môi trường, chưa chuyển D12.

### 29.3. D11 đã chốt hướng 1 — local PostgreSQL riêng rồi smoke deploy

Owner chọn “Chốt 1” sau giải thích local và đối chiếu lịch sử. Kiểm nghiệp vụ/mutation bằng BE và PostgreSQL local riêng, fixture riêng, cấu hình test không trỏ DB .env đang dùng. FE hiện hành tích hợp vào BE test khi kiểm browser; không dùng FE_Project-main cũ. Sau đó kiểm bản deploy cho HTTPS/CORS/cookie/IPN và các smoke trong phạm vi fixture xác định. Không mặc định tạo staging riêng hoặc bỏ kiểm Render.

Lịch sử đã kết hợp test H2, PostgreSQL Docker riêng ở F-06/F-07 (master checklist ghi 14 lượt pass) và probe Render/DB .env; không nói trước đây toàn dùng hướng 3. Những số này là bằng chứng lịch sử. Khi thực hiện sửa BE vẫn chạy clean test; PG cô lập kiểm migration/query, callback mô phỏng local không chứng minh merchant IPN thực đã cấu hình đúng. Chọn hướng không phải yêu cầu chạy test/migration/deploy ngay trong giai đoạn plan, không mở toàn bộ quyền mutation cloud. Không hỏi lại hướng 1/2/3; các thông tin đích thiếu sẽ kiểm nguồn trước.

## 30. D12 — ca nắng và ngày nghỉ: gộp quy tắc lịch mới

### 30.1. Đối chiếu và ranh giới với quyết định đã chốt

- AGENTS §2.5 và TimeSlot.java cùng bộ 7 ca, 90 phút; ca 3 là 09:30–11:00, ca 4 là 13:00–14:30, cấm Outdoor. Docs 04 mục 5.4 đã ghi ví dụ PDF dùng 11:00–12:00/12:00–13:00 không khớp; không lấy ví dụ đó làm bộ ca mới.
- AGENTS §2.8 phân NATIONAL_FIXED/LUNAR_FESTIVAL/CENTER_EVENT, chỉ is_active=true ảnh hưởng sinh lịch. HolidayRepository truy vấn active không loại tag CENTER_EVENT; SessionService lịch khả dụng chặn ngày active, preview/generate bỏ ngày active, reschedule có kiểm ngày nghỉ. Đây là source read, chưa chạy lại tests/probe ở lượt này.
- D03-c chốt 1080 giờ từ Paid, mục 11.13 ghi không tự trừ ngày nghỉ/gia hạn. Vì vậy không đưa phương án tự kéo hạn theo ngày lễ vào đây như thể chưa chọn; D08 chỉ xử lý sự cố có căn cứ/Manager duyệt theo phạm vi đã chốt.
- D12 xét tạo/sinh/dời tới lịch mới. Việc thay đổi Holiday làm ảnh hưởng lịch đã tồn tại thuộc D14 (kiểm ảnh hưởng/confirm/audit), không tự hủy/dời lịch đó trong bước này.

### 30.2. Đề xuất bộ quy tắc thống nhất cho MVP — chờ xác nhận

1. Giữ 7 ca hiện hành; Outdoor cấm ca 3/4 đúng giờ trên, Indoor mở các ca chuẩn khi các điều kiện khác hợp lệ. Không thêm thời tiết realtime hoặc giờ ca tùy biến. Phân loại theo dữ liệu Subject/phòng hiện có, không suy tên môn tùy ý.
2. Mọi Holiday active đều chặn lịch mới, gồm cả ba tag; inactive không chặn. Không chỉ lấy ngày lễ quốc gia rồi bỏ ngày trung tâm đã công bố nghỉ. Tag phân nhóm/quản lý, is_active quyết định áp dụng.
3. Normal sinh đủ 12 buổi hợp lệ bằng cách bỏ qua ngày nghỉ active và đi tới ngày học tiếp theo; preview và lưu dùng cùng quy tắc, không coi bỏ ngày là mất một buổi hoặc tự đổi thành 11 buổi.
4. Advanced không tạo/dời tới ngày nghỉ active; request bị từ chối không tạo Session/giữ tài nguyên/trừ credit. Hạn vẫn 1080 giờ, không tự cộng ngày lễ. Lịch đã tồn tại bị ảnh hưởng chuyển D14; sự cố đủ căn cứ theo D08, không cộng bù trùng.

**Câu hỏi chốt gộp:** có đồng ý dùng cả bộ quy tắc này, coi giờ ca lệch ở PDF là ví dụ sai và “không trừ” ở III.2 là không tiêu thụ credit khi không tạo ca, không phải tự dừng đồng hồ 1080 giờ? Nếu muốn chỉ ngày quốc gia chặn lịch (bỏ CENTER_EVENT) hoặc thay giờ ca thì đó là thay đổi so với AGENTS/source, cần nêu rõ phần muốn đổi trước code. Không tạo thêm lựa chọn phức tạp khi không có nhu cầu nghiệp vụ.

Test mục tiêu khi triển khai: Indoor/Outdoor × 7 ca; Holiday cả ba tag active/inactive; preview–generate cùng đủ 12 buổi/rollback khi không sinh đủ; Advanced ngày nghỉ không có mutation kể cả retry; deadline không tự tăng. NormalSchedulePolicy còn cần đối chiếu kiểm lễ/nắng khi đăng ký với D14, không khẳng định mọi đường đã đủ chỉ từ source sinh lịch. Dừng tại mâu thuẫn giờ PDF và nghĩa “không trừ” của D12; chưa code/tests/Maven/DB/deploy.

### 30.3. Owner đổi quyết định hạn Advanced — 45 ngày sử dụng, TT nghỉ/đóng thì skip

Owner chọn hướng 2: “cứ làm đơn giản TT nghỉ/đóng → skip. Thêm/tắt ngày nghỉ sau khi mua có cập nhật hạn gói đang dùng, Không cộng bù hai lần”. Quyết định này thay phần **1080 giờ liên tục không trừ ngày nghỉ** của D03-c và các kế hoạch cũ. Giữ mốc bắt đầu từ Paid hợp lệ, không đổi Invoice holding 5 phút, không nạp/reset credit. BE tính/lưu/trả deadline thực sau skip; FE dùng deadline BE, không tự cộng ngày nghỉ hoặc hardcode Paid+45 ngày.

Ngày trung tâm công bố nghỉ/đóng là căn cứ, không chỉ ngày lễ quốc gia; việc đổi lịch nghỉ phải cập nhật hạn đăng ký đang dùng. Tính phần nghỉ theo hợp nhất thời gian ảnh hưởng; cùng thời gian đã được skip không tiếp tục cộng bù D08. Khi bật/tắt/sửa lặp không được cộng dồn theo số request. Phần sự cố chưa được tính skip vẫn xét theo phạm vi D08 đã chốt, không cộng hai nguồn cho cùng khoảng.

Khi triển khai phải rà lại activation/expiry job/available slots/book/reschedule/extension/DTO/report và thiết kế timestamp/migration tương thích. Lưu được căn cứ nghỉ và bù đã áp để tính lại/chống trùng; không chạy lại cộng số ngày lên endDate mỗi lần sửa Holiday. Những đoạn 1080 giờ liên tục/không tự tăng ở mục cũ của 04/01/02 là phân tích trước quyết định này, không còn là acceptance cho hạn Advanced. Normal vẫn đủ 12 buổi và không mở hủy/báo vắng. Chưa suy Owner đồng ý đổi bộ 7 ca từ việc chọn hạn gói; giữ chuẩn hiện có, điểm giờ ví dụ PDF chưa được dùng làm source mới.

### 30.4. Biên ngày nghỉ quá khứ và lịch đã đặt — đề xuất cần xác nhận cùng nhau

Quyết định 30.3 đã rõ với cập nhật hạn gói đang dùng; chưa rõ việc **tắt/xóa một ngày đã thực sự nghỉ trong quá khứ** có được rút lại phần thời gian đã skip không. Điều này ảnh hưởng quyền đã công bố và các ca đã đặt, không chỉ là phép tính.

**Đề xuất đơn giản:** chỉ tính lại phần lịch nghỉ tương lai bị thay đổi; khoảng TT đã thực sự đóng trong quá khứ giữ lịch sử và phần skip đã hưởng. Tắt một ngày nghỉ tương lai thì tính lại hạn, có thể làm hạn ngắn hơn. Nếu hạn mới làm ca đã đặt vượt hạn, báo danh sách ảnh hưởng và yêu cầu CenterManager xử lý trước khi xác nhận thay đổi, không tự hủy ca/trừ credit hoặc tạo thêm một ngoại lệ hạn ngầm. Dùng cùng kiểm ảnh hưởng ở D14, không xây hai workflow Holiday riêng.

Owner có đồng ý quy tắc gộp này không? Nếu muốn sửa cả ngày nghỉ đã qua thì cần quy trình sửa sai có căn cứ, không dùng toggle thông thường để rút quyền lịch sử. Chưa chốt đề xuất 30.4; không mở lại lựa chọn 45 ngày sử dụng đã chốt. Chỉ docs/plan, chưa code/tests/Maven/DB/deploy.

### 30.5. Đã chốt biên quá khứ và lịch đã đặt

Owner trả lời “Đồng ý” cho toàn bộ đề xuất 30.4. Giữ lịch sử/phần skip của thời gian TT thực sự đã nghỉ trong quá khứ; tính lại hạn theo thay đổi ngày nghỉ tương lai, tắt ngày nghỉ tương lai có thể rút hạn. Nếu hạn mới làm ca đã đặt vượt hạn thì hiển thị ảnh hưởng, CenterManager xử lý trước khi xác nhận thay đổi; không tự hủy ca/trừ credit hoặc thêm ngoại lệ hạn ngầm. Gắn cùng kiểm ảnh hưởng Holiday ở D14, không nhân workflow. Chưa tự mở quyền sửa sai lịch sử ngày nghỉ đã qua.

Kế hoạch kiểm: toggle lặp không cộng/trừ hạn nhiều lần, nghỉ quá khứ không bị rút quyền, nghỉ tương lai tác động đúng gói/ca; thao tác có ca vượt hạn không để lịch/hạn ở trạng thái nửa chừng. D12 phần hạn/skip đã chốt; giữ bộ 7 ca chuẩn hiện có, không đổi theo giờ ví dụ PDF. Chưa code/tests/Maven/DB/deploy.

## 31. D13 — định nghĩa báo cáo hội viên và đăng ký hết hạn

### 31.1. Source hiện tại và vấn đề

GET /api/reports/members trả MemberReportResponse gồm totalMembers, activeMembers, expiredMembers, packageDistribution, monthlyGrowth. ReportService.memberReport lấy Member có bất kỳ MemberPackage Active và endDate chưa qua (null cũng được coi hợp lệ), rồi tính expiredMembers=totalMembers-activeMembers. Chưa xét Normal Enrollment, Advanced credit/ca đã đặt hoặc deadline skip mới. Do đó Member chưa từng mua cũng có thể bị đếm “hết hạn”; Member còn học Normal nhưng không có gói Active cũng có thể bị đếm sai. Không thể sửa chỉ bằng đếm số subscription Expired vì một Member có nhiều đăng ký.

Đây là source hiện tại, chưa probe report live/chạy test mới. Việc scope/authority báo cáo sẽ tiếp tục đối chiếu D18; không cấp thêm quyền hoặc tự coi hiện có scope đúng. Dashboard/revenue hiện cộng Invoice Paid, cần đồng bộ tiền hoàn theo D10 khi thiết kế báo cáo tiền; không gộp việc chọn công thức tài chính vào chỉ số quyền học dưới đây.

### 31.2. Ba hướng báo cáo liên quan — chờ chọn

1. **Chỉ hội viên:** chỉ số phục vụ chăm sóc/gia hạn, đếm mỗi Member một lần; phân biệt chưa từng có quyền học, đang còn quyền, từng có nhưng hiện không còn quyền.
2. **Chỉ đăng ký hết hạn:** đếm từng đăng ký/gói hết hạn, không gọi đó là số Member hết quyền. Dễ tổng hợp nhưng một người có gói hết hạn và gói còn dùng vẫn xuất hiện trong số đăng ký hết hạn; không đủ làm danh sách cần nhắc gia hạn.
3. **Cả hai — đề xuất:** giữ số liệu hội viên theo người, thêm số đăng ký hết hạn theo đăng ký; tên/đơn vị/ý nghĩa khác nhau. Dùng chung logic quyền học, thêm phép tổng hợp thay vì thêm workflow. Không âm thầm đổi field expiredMembers từ đếm người sang đếm gói; DTO/compatibility FE sẽ công bố khi triển khai.

**Định nghĩa đi cùng hướng 1/3:** “còn quyền học” nếu còn ít nhất một đăng ký đã được thanh toán và cấp quyền hợp lệ: Normal còn buổi hợp lệ chưa kết thúc/chưa hoàn tất; Advanced còn hạn mới đã tính skip và còn credit dùng được **hoặc** còn ca đã đặt hợp lệ chưa học xong. 0 credit nhưng còn ca đã đặt không coi là hết quyền. Pending thanh toán/Paid cần đối soát chưa cấp quyền không tạo quyền học; ca đã hủy không tạo quyền. Tài khoản bị khóa là trạng thái riêng, không tự đổi thành hết hạn. Member mới chưa từng có quyền không tính vào “từng học, hiện hết quyền”; một người nhiều đăng ký chỉ đếm một lần ở nhóm người. Không cần gán lý do hết hạn cho toàn bộ người hết quyền: học đủ buổi/hết credit khác hết thời hạn.

Đếm đăng ký hết hạn phải dựa deadline BE tại thời điểm báo cáo, không chờ cron đổi status mới đúng; Completed vì dùng hết credit/học xong không tự gọi Expired. Đăng ký cũ thiếu dữ liệu hạn/quyền phải được đối soát, không coi endDate null là vô hạn tùy ý. Chi tiết query/field sẽ thiết kế kỹ thuật trong hướng đã chọn.

**Cần Owner chọn 1/2/3 cùng cách hiểu quyền học trên.** Khuyến nghị 3 để tránh nhầm số người cần chăm sóc với số đăng ký hết hạn; mức tăng là logic/query/DTO/test, không thêm vòng duyệt. Ví dụ một Member có một gói hết hạn nhưng còn một khóa Normal đang học: đang còn quyền=1, hết quyền=0, đăng ký hết hạn=1. Chưa chốt report doanh thu sau hoàn, D14 hoặc D18 từ lựa chọn này; chưa code/test/Maven/DB/deploy.

### 31.3. D13 chốt hướng 3, UI/UX ưu tiên hướng 1

Owner chọn “3, ưu tiên UI/UX theo 1”. Giữ cả thống kê người và đăng ký, dùng định nghĩa quyền học ở 31.2. Màn báo cáo ưu tiên Chưa từng có quyền học / Đang còn quyền / Đã từng học, hiện hết quyền; mỗi người đếm một lần, tài khoản khóa là trạng thái riêng. Số đăng ký hết hạn là thông tin bổ sung có nhãn/đơn vị riêng, không thay nghĩa expiredMembers thành đếm gói.

UI giải thích ngắn còn/hết quyền, 0 credit còn ca hợp lệ đã đặt vẫn còn quyền. Không thêm chiến dịch email/CRM từ việc chọn UX; DTO/compatibility FE sẽ công bố khi code. Test một người nhiều đăng ký, Normal không có gói Active, Advanced 0 credit/còn ca, Member chưa mua, Paid cần đối soát và deadline skip thay đổi. Chưa code/test/Maven/DB/deploy.

## 32. D14 — thay đổi Holiday: lịch sử, xem ảnh hưởng và xác nhận

### 32.1. Source và quyết định phải giữ

HolidayController/Service hiện có GET danh sách/active, POST tạo và PATCH toggle; ghi yêu cầu MANAGE_CLASSES. Chưa có update/delete, lịch sử thay đổi hoặc preview ảnh hưởng. Toggle không truyền active sẽ đảo giá trị, cần thiết kế retry an toàn khi sửa.

O07 đã đồng ý lịch sử và confirm khi ảnh hưởng lớp, chưa chọn workflow. D12/30.5 giữ nghỉ thực tế quá khứ, cập nhật hạn với ngày nghỉ tương lai, xử lý ca vượt hạn trước xác nhận. D06–D08 giữ luật sự cố/credit/consent, Normal không hủy/hoàn như Advanced. Không tự mở sửa attendance/lịch đã diễn ra.

Các hướng đều cần xem trước tác động lịch/đăng ký/deadline, lưu actor/time/lý do/cũ-mới và kiểm lại dưới lock lúc ghi vì preview có thể cũ. Thông báo sau commit, không xóa dấu lịch sử ngày thực sự nghỉ. Chỉ đổi tên/nhãn thì không tự đổi lịch/hạn; tắt ngày nghỉ không tự đưa buổi đã dời về lịch cũ.

### 32.2. Ba workflow tổng thể — chờ chọn

1. **Báo ảnh hưởng, xử lý lịch riêng rồi xác nhận Holiday — đề xuất MVP.** BE chỉ rõ buổi rơi vào ngày sẽ đóng hoặc ca vượt hạn mới. Nếu còn xung đột thì chặn lưu; CenterManager mở từng buổi xử lý bằng luồng đúng loại khóa/quyền/đồng ý, rồi kiểm lại và xác nhận Holiday. Lúc ghi kiểm lại toàn bộ và cập nhật Holiday/hạn liên quan nhất quán; lịch mới chen vào thì yêu cầu kiểm lại, không cưỡng ép. Không cần tự xếp lại hàng loạt. Phải có đường dời Normal do trung tâm giữ đủ 12 buổi/học viên, không biến thành hủy Normal; khi triển khai kiểm/refactor source phù hợp, chưa giả định reschedule hiện đủ Normal.
2. **Preview phương án dời, chọn và áp dụng chung với Holiday.** BE gợi ý lịch thay thế, Manager xem/chỉnh rồi xác nhận chung Holiday/lịch/hạn. UI thuận tiện hơn nhưng phải xử lý trùng tài nguyên giữa nhiều lịch, consent, dữ liệu thay đổi sau preview và rollback nguyên khối; nhiều phụ thuộc D04–D08 hơn hướng 1.
3. **Tự dời hàng loạt.** Ít thao tác nhưng cần thuật toán chọn lịch/quy tắc đồng ý/không đủ tài nguyên; chưa được cho phép và không đề xuất cho project hiện tại.

Khuyến nghị 1 theo ưu tiên đơn giản/ổn định. Đánh đổi Manager thao tác nhiều buổi, thay đổi Holiday chưa có hiệu lực trước khi xung đột được giải quyết; không hiển thị như đã đóng khi chưa lưu. Sự cố khẩn cấp theo D06/D08, không tự vượt confirm Holiday; nếu cần đóng ngay phải tách quy tắc riêng.

**Cần Owner chọn 1/2/3.** Quyền vai trò ngoài CenterManager theo authority hiện có tiếp tục đối chiếu D18, không nới quyền từ UI. Chưa công bố API/DTO/preview version hoặc status mới. Chưa code/test/Maven/migration/DB/deploy.

### 32.3. D14 đã chốt hướng 1 — xử lý lịch riêng rồi xác nhận

Owner chọn “1”. CenterManager xem ảnh hưởng Holiday lên lịch/hạn, xử lý từng buổi theo đúng loại khóa/quyền/đồng ý, rồi kiểm tra lại và xác nhận thay đổi. Còn xung đột thì không lưu thay đổi có tác động đó. BE kiểm lại dưới lock lúc commit, cập nhật Holiday/hạn nhất quán, không tin preview cũ. Lưu lịch sử cũ-mới/actor/time/lý do, thông báo sau commit. Không tự dời hàng loạt/gợi ý tối ưu lịch trong MVP; không tự phục hồi ca cũ khi tắt ngày nghỉ.

Normal dời do trung tâm giữ đủ 12 buổi/học viên, không thành hủy Normal. Giữ nghỉ thực tế quá khứ và kiểm tác động rút hạn D12; không mở sửa attendance/lịch đã diễn ra. Chưa code/endpoint/migration/test/deploy. Quyền đóng khẩn cấp/lỗi trung tâm không tự mở rộng từ lựa chọn này.

## 33. D15 — check-in cửa, attendance và sửa kết quả sau buổi

### 33.1. Đối chiếu source, nguồn và phần đã chốt

O08/III.6 muốn phân biệt vào cửa với tham gia buổi, PDF đề xuất check-in cửa sau MVP; chưa được hiểu thành xóa code hiện có. AttendanceService.checkIn/checkOut tạo bản ghi session=null/CheckedIn; scan có sessionId kiểm người thuộc buổi rồi ghi Present/Late; cửa sổ scan hiện từ 30 phút trước bắt đầu đến giờ kết thúc, Late khi sau 10 phút. Đây là hai luồng hiện có, không thể dùng vào cửa làm bằng chứng học buổi.

correctAttendance yêu cầu lý do/phạm vi dữ liệu, Coach đúng buổi, đồng bộ TrainingResult và audit; chưa thấy giới hạn sau giờ kết thúc/finalized trong method này. Controller /correct cho RECORD_RESULT/MANAGE_CLASSES/MANAGE_USERS, không riêng CenterManager. Finalization khóa Session, sau giờ kết thúc tạo Absent cho người chưa có attendance, chốt Completed/finalized; Advanced credit đã tiêu thụ lúc book, không trừ lần nữa. D05 đã chốt báo vắng riêng không tự Absent; Normal không hủy/báo vắng. Sửa attendance không được tự đổi tiền/credit từ việc đổi nhãn.

Phải tách “ghi nhầm attendance” với “thực sự không tổ chức được buổi do trung tâm”. Trường hợp sau cần hồ sơ sự cố/bù có căn cứ, không đổi Absent thành Present rồi coi đã học, không dời lùi một Session đã diễn ra. Quyền/lưu chứng cứ của nhánh báo muộn cần chốt trước khi cho hoàn credit/bù sau buổi, không tự suy từ D06–D08.

### 33.2. Ba hướng phạm vi MVP và quyền sửa — chờ chọn

1. **Ưu tiên attendance, cửa để nâng cấp; sau buổi CenterManager sửa có kiểm soát — đề xuất.** UI chính là danh sách buổi/Member, Coach được phân công ghi/sửa trong thời gian buổi đang diễn ra và chưa finalized; QR theo buổi giữ cửa sổ source 30 phút trước–kết thúc, sau 10 phút Late. Sau kết thúc/finalized Coach không tự sửa; CenterManager sửa nhầm có lý do/bằng chứng/audit cũ-mới, đồng bộ TrainingResult, không tự hoàn/trừ credit hoặc mở lại lịch. Chưa có attendance thì finalization ghi Absent theo D05; không dùng session=null như Present. Không ưu tiên phát triển màn vào/ra trong MVP, giữ code/data hiện có. Sự cố báo sau buổi phải vào hồ sơ riêng, chưa tự cấp bù/hoàn từ nút sửa attendance.
2. **Cả attendance và vào/ra trong MVP**, cùng nguyên tắc quyền/sau buổi như hướng 1; thêm màn/định nghĩa visit, chống check-in trùng, phân quyền và test hai dữ liệu tách biệt. Đầy đủ hơn nhưng thêm phạm vi phụ trợ.
3. **Attendance ưu tiên, Coach được sửa cả sau buổi có lý do.** Cửa vẫn để sau; ít phụ thuộc Manager nhưng mở quyền sửa lịch sử rộng hơn, cần kiểm đồng bộ kết quả/Completed/audit/race và giới hạn sửa rõ; không đề xuất khi ưu tiên project đơn giản/kiểm soát.

**Cần Owner chọn 1/2/3** cùng phạm vi/cửa sổ/quyền sửa đã mô tả. Các cửa sổ là đề xuất dựa source, chưa policy khả dụng. Dù chọn hướng nào, cơ chế bù/hoàn cho sự cố thực sự báo sau buổi vẫn cần phân biệt với sửa attendance; sẽ trình bày chung các cách xử lý sự cố này sau lựa chọn phạm vi, không hỏi từng field. Không thay chế tài bookingBlock toggle=false hoặc reset no-show/credit từ thao tác sửa. Chưa code/tests/Maven/DB/deploy.

### 33.3. Chốt phạm vi hướng 1, ưu tiên nâng cấp hướng 2; cửa sổ/Late chưa chốt

Owner chọn 1 nhưng ưu tiên nâng cấp lên 2, yêu cầu cân nhắc Late và 30p–10p. Chốt MVP ưu tiên attendance theo Session; Coach đúng buổi ghi/sửa trong buổi, sau kết thúc/finalized CenterManager sửa sai có lý do/audit/đồng bộ TrainingResult, không tự đổi credit/lịch. Giữ code/data check-in cửa, đưa UI/UX vào-ra tách biệt vào ưu tiên nâng cấp sau. Không coi lựa chọn này đã chấp thuận mốc 30/10 hoặc Late; câu đó ở 33.2 là đề xuất cũ đang rà lại.

### 33.4. Đánh giá và ba cấu hình attendance gộp — đang chờ

Source TrainingResultService xem Present/Late cùng nhánh có tham gia; chưa thấy Late có hậu quả credit riêng trong các đường đã rà. AttendanceRepository còn query Present/Late/CheckedIn trong cùng thống kê, cần tách visit/session khi nâng cấp. Vì vậy chưa có nhu cầu nghiệp vụ chứng minh phải dùng ngưỡng 10 phút; không tạo chế tài đi muộn mới. Mốc 30 phút sớm phục vụ đón/check-in trước buổi nhưng không tự chứng minh đã học; attendance nên gắn buổi thực tế. Không thay entry log bằng attendance.

1. **Đơn giản theo buổi — đề xuất MVP:** điểm danh/scan theo Session khi buổi đang diễn ra; kết quả mới chỉ Present/Absent, không tự phân Late hoặc mở cửa sổ 30 phút sớm. Coach xác nhận tham gia thực tế, cuối buổi finalization chỉ bù Absent cho người chưa có kết quả. Giữ timestamp người/thời điểm ghi để kiểm chứng; timestamp ghi không tự coi là giờ Member đến. Sau buổi sửa bởi CenterManager như 33.3. Không tự tạo Absent trước buổi hoặc chặn tham gia chỉ vì đến trễ.
2. **Cho scan sớm 30 phút, bỏ Late:** giữ tiện tiếp nhận trước buổi, nhưng kết quả scan sớm cần được hiểu/kiểm chứng rõ trước khi coi đã tham gia. Thêm ranh giới giữa có mặt sớm và thực học; không tự ghép mọi lượt vào cửa thành Present. Nếu chọn hướng này cần thể hiện trạng thái tiếp nhận/xác nhận tham gia phù hợp, không bỏ qua rủi ro scan sớm rồi không học.
3. **Giữ 30 phút trước–hết buổi, Late sau 10 phút:** như source hiện tại; thêm nhãn/biên/test cho 10 phút nhưng Late chưa có tác động credit riêng. Chỉ phù hợp nếu trung tâm cần theo dõi đi muộn thật, và phải phân biệt giờ scan/giờ đến/giờ Coach nhập; hiện chưa có xác nhận nhu cầu này.

Khuyến nghị 1 vì dùng giờ Session sẵn có và ít ngưỡng tùy ý, không phải bỏ kiểm attendance. Khi nâng cấp check-in cửa có thể ghi nhận đến sớm riêng; nếu sau này cần thống kê đi muộn thì định nghĩa nguồn giờ đến/ngưỡng khi đó. Dữ liệu Late lịch sử giữ nguyên, đọc/hiển thị tương thích và tính là có tham gia như source, không xóa/chuyển hàng loạt DB. Chỉ khi Owner chọn mới đổi API validation/UI trạng thái mới và các caller.

**Cần Owner chọn cấu hình 1/2/3 ở 33.4**, không hỏi lại phạm vi MVP/nâng cấp đã chốt. Biên kỹ thuật cuối buổi phải nhất quán finalization và khóa DB khi triển khai, không cho scan ghi đè buổi finalized; chưa chốt con số khác hoặc sửa code/test/Maven/DB/deploy.

### 33.5. D15 chốt cấu hình 1 — Present/Absent trong giờ buổi

Owner chọn “1” cho 33.4. Điểm danh/scan theo Session trong giờ buổi đang diễn ra; kết quả attendance mới chỉ Present/Absent, bỏ tự phân Late sau 10 phút và cửa sổ scan sớm 30 phút. Không đánh Absent sớm hoặc phạt credit vì đến trễ; Coach xác nhận tham gia thực tế. Cuối buổi finalization chỉ bổ sung Absent khi chưa có kết quả. Sau buổi/finalized CenterManager sửa nhầm có lý do/audit, đồng bộ TrainingResult; không tự sửa credit/lịch.

Giữ dữ liệu Late cũ và đọc/hiển thị tương thích, không chạy SQL xóa/đổi lịch sử. Giữ code/data vào-ra riêng, ưu tiên nâng cấp UI đó sau MVP; không trộn CheckedIn với Present theo buổi. Test cần nhất quán biên giờ kết thúc/finalized, retry/race và role/assignment, không giữ kỳ vọng Late/30–10 cho dữ liệu mới chỉ để test cũ xanh. Chưa code/test/Maven/DB/deploy.

## 34. D15 tiếp — sự cố trung tâm được xác nhận sau khi buổi đã chốt

### 34.1. Vì sao cần quyết định riêng

Ví dụ TT/Coach thực sự không tổ chức buổi, nhưng sự cố chưa nhập và job đã ghi Absent/Completed. Đây khác Member no-show hoặc Coach nhập nhầm Present/Absent. D06–D08 đã có hướng xử lý sự cố trước buổi/expiry, còn trường hợp báo sau buổi được để lại D15; không tự suy mọi buổi Absent đều được bù. Source SessionService.requireMutableSession chặn dời/xóa buổi đã kết thúc/finalized. Không phá guard đó để dùng reschedule sửa quá khứ; nguồn xử lý hiện nằm trong SessionService, không có class main SessionIncidentService riêng dù có test tên đó.

### 34.2. Hai mức phạm vi cùng hậu quả — chờ chọn

1. **Xử lý có kiểm soát bằng hồ sơ sự cố riêng — đề xuất nếu MVP cần bảo toàn quyền học.** CenterManager xác minh bằng chứng và Member bị ảnh hưởng, lưu mốc/thực tế không tổ chức; không xóa Session/audit hoặc đổi thành Present để giả đã học. Hồ sơ gắn buổi gốc xác định đây không phải vắng do Member, không dùng làm căn cứ chế tài. Advanced hoàn đúng một credit gốc chưa được bù, sau đó book ca tương lai theo luồng đã chốt; hạn/credit expiry theo D08 và skip D12, không bù hai lần. Normal tổ chức buổi bù có liên kết buổi không diễn ra, không thu thêm/cho hủy Normal; bảo đảm tổng nghĩa vụ là 12 buổi thực học hợp lệ, không cộng thành quyền 13 buổi. Khóa/transaction/idempotency ngăn duyệt lặp, Member nhận thông báo sau commit. Chỉ áp dụng buổi thực sự không tổ chức có căn cứ, không tự xử lý buổi dạy dở/ảnh hưởng một phần khi chưa có rule.
2. **MVP chỉ ghi nhận hồ sơ sau buổi, chưa làm bù/hoàn quyền trong SCMS.** Giữ lịch sử và bằng chứng để TT xử lý ngoài hệ thống; đưa nghiệp vụ bù hồi tố vào nâng cấp. Ít code/test hơn nhưng số quyền/credit trong SCMS chưa phản ánh giải quyết ngoài hệ thống; phải hiển thị chưa xử lý, không đánh dấu đã bù hoặc tự sửa DB. Không coi lựa chọn này là cho phép bỏ quyền Member đã trả tiền.

Khuyến nghị 1 nếu đây là tình huống MVP phải giải quyết end-to-end; phức tạp hơn sửa attendance nhưng tận dụng hồ sơ/đối soát credit D08 đã cần làm. Hướng 2 chỉ phù hợp nếu chấp nhận giới hạn sản phẩm đó cho MVP. Chưa chọn thay Owner, chưa thêm quyền phục hồi mọi lịch sử. **Cần chọn 1 hay 2**, rồi tiếp tục D16; không chia thêm câu hỏi từng field. Chưa code/API/migration/tests/Maven/DB/deploy.

### 34.3. D15 chốt xử lý sự cố báo sau buổi theo hướng 1

Owner chọn “1”. CenterManager xác minh buổi thực sự không tổ chức do trung tâm/Coach, hồ sơ gắn buổi gốc và Member bị ảnh hưởng; không xóa lịch/audit, không đổi thành Present để giả đã học. Advanced hoàn đúng một credit gốc chưa được bù, đặt lại tương lai theo luồng chuẩn; Normal có buổi bù liên kết, giữ nghĩa vụ 12 buổi hợp lệ, không cấp thành 13. Hạn/expiry/khôi phục theo D08 và skip D12, không bù trùng, không tính sự cố đã xác minh như no-show của Member.

Giữ transaction/locking/idempotency, trước-sau/audit và thông báo sau commit; không phá guard reschedule quá khứ. Phạm vi này không tự áp cho buổi học dở hoặc Member tự vắng. D15 chính đã chốt tại 33.3/33.5/34.3; chưa code/API/migration/test/deploy.

## 35. D16 — Cash: dữ liệu thu, biên nhận và sửa sai

### 35.1. Source đã có và thiếu

InvoiceController.confirmCash yêu cầu role RECEPTIONIST và MANAGE_INVOICES. InvoiceService lấy actor từ Auth, lock center/invoice, kiểm phạm vi/method Cash/Pending/chưa expired, rồi markPaidAndActivate lưu receptionist/paymentDate và audit. Invoice.amount là snapshot, không nhận amount client ở thao tác xác nhận. InvoiceResponse đã có người/giờ/số tiền/method/code; không thêm lại trường tương đương chỉ vì PDF nêu yêu cầu. O06 đã đồng ý thông tin lấy server, chưa mở CenterManager thành thu ngân.

Route hiện chưa nhận note; chưa thấy API biên nhận hoặc workflow sửa sai thu Cash. Đổi note không đảo được hậu quả đã Active/cấp credit. Giao dịch chưa thu thật nhưng bấm Paid nhầm khác đã thu thật rồi hoàn tiền; không dùng một thao tác “hoàn tiền” cho cả hai, không sửa amount/Paid chứng từ để mất dấu. Tiền ghi nhận nhầm cần hồ sơ riêng phản ánh chưa thực thu, không giả tạo giao dịch hoàn tiền đã trả khách.

### 35.2. Ba gói phạm vi liên quan — chờ chọn

1. **Thu Cash cơ bản + hồ sơ sai sót, chưa tự đảo nghiệp vụ — đề xuất MVP.** Receptionist xác nhận đã nhận đúng tiền của hóa đơn còn hợp lệ, có màn confirm và note tùy chọn. Server lưu actor/time/amount/audit. Biên nhận xem/in dựa mã Invoice hiện có, không cần dịch vụ PDF riêng. Sai sót tạo hồ sơ CenterManager xử lý/ghi kết quả, giữ nguyên chứng từ gốc; không cho Receptionist sửa Paid/amount, tự thu lại hoặc tự đảo Active/credit. Hồ sơ chưa giải quyết hiển thị còn cần xử lý, không chỉ sửa note rồi đóng. Nếu đã thu và cần hoàn thật thì xử lý ngoài SCMS/lưu chứng cứ theo nguyên tắc D10; nếu chưa thực thu thì ghi đúng sự kiện nhầm, không khai hoàn giả. MVP chưa có nút tự động thu hồi quyền/đảo thanh toán; phải công bố giới hạn này, không hứa trạng thái gói tự đúng sau ghi hồ sơ. Thiết kế xử lý kết quả/hậu quả cụ thể khi gặp sai sót không rõ phải dừng đối soát, không tùy ý SQL.
2. **Thêm đảo giao dịch có kiểm soát khi quyền chưa dùng.** Như 1, thêm CenterManager được xử lý hủy hiệu lực khoản ghi nhận sai/thu hồi quyền liên quan trong transaction sau khi kiểm chưa dùng credit/buổi, không có lịch/quyền khác phụ thuộc và không có xử lý trước. Có lịch sử điều chỉnh riêng, giữ amount/hóa đơn gốc, không áp cho trường hợp đã sử dụng. Phức tạp hơn ở liên kết/lịch/credit/race, vẫn cần hồ sơ cho trường hợp không đủ điều kiện.
3. **Đối soát/điều chỉnh tài chính đầy đủ**, gồm đã dùng một phần, bù trừ/chênh lệch và các nhánh hoàn tiền. Nhiều business rule mới, không đề xuất project hiện tại; không suy có sẵn từ code thu Cash.

Khuyến nghị 1 nếu ưu tiên vận hành luồng chuẩn trước/nâng cấp sau; cần nhận rõ giới hạn không tự đảo quyền khi thu sai. Nếu MVP phải sửa được giao dịch thu nhầm end-to-end mà không can thiệp DB thì chọn 2 cho phạm vi chưa dùng; 3 để sau. **Chờ Owner chọn 1/2/3.** Không mở quyền CenterManager thu Cash từ quyền duyệt sai sót; chưa code/API/migration/tests/Maven/DB/deploy.

### 35.3. Đánh giá chi phí hướng 2 theo yêu cầu Owner — 26/10/2026

Owner hỏi độ phức tạp, chưa chọn 2. Rà MemberPackage/ClassEnrollment/SessionRepository và guard thanh toán: có sẵn liên kết Invoice.subscription, Session.memberPackage, lock và audit để tái sử dụng; chưa có hồ sơ điều chỉnh riêng, ClassEnrollment không liên kết trực tiếp một Invoice cụ thể. Không coi sửa amount/status hoặc remainingCredit=12 là chứng minh chưa dùng: đã book rồi hủy/hoàn có thể trở về 12, phải xét lịch sử/phụ thuộc.

Hướng 2 cần tối thiểu hồ sơ điều chỉnh gắn Invoice gốc/lý do/actor/time/kết quả; endpoint CenterManager kiểm quyền, khóa Invoice/đăng ký cùng thứ tự khóa booking; kiểm chưa điều chỉnh/chưa nhận tiền thật hoặc xử lý đúng nhánh tiền thật, không có quyền đã dùng/lịch đang giữ/giao dịch khác phụ thuộc. Khi đủ điều kiện thì vô hiệu hiệu lực thanh toán nhầm/thu hồi đúng quyền trong cùng transaction, giữ chứng từ/amount/audit gốc và phản ánh khoản điều chỉnh trong báo cáo; retry không đảo hai lần. Không tích hợp refund API hoặc tự hủy lịch để làm đủ điều kiện. Không đủ liên kết/căn cứ thì dừng đối soát, không suy từ số dư.

**Phạm vi gọn đề xuất nếu chọn 2:** chỉ xử lý tự động trường hợp xác nhận Cash nhầm khi chưa thực nhận tiền, Advanced gắn chính xác subscription và chưa phát sinh sử dụng/booking/điều chỉnh liên quan. Có tác động/phụ thuộc thì từ chối tự đảo, để hồ sơ Manager xử lý. Tiền đã thực thu rồi muốn hoàn giữ nhánh hoàn có chứng cứ đã đề xuất, không giả là ghi nhầm chưa thu. Đây là đề xuất thu hẹp, chưa quyết định.

**Mâu thuẫn phải làm rõ trước áp cho Normal:** các quyết định cũ cấm hủy Enrollment Normal; hướng 2 không tự tạo ngoại lệ hủy Normal dưới tên sửa sai Cash. Nếu muốn thu hồi cả quyền Normal do xác nhận nhầm, phải chốt ngoại lệ sửa sai kế toán và điều kiện/liên kết riêng trước, không dùng quyền hủy Member hoặc tự xóa Enrollment. Do đó không thể gọi hướng 2 bao trùm hai loại là thay đổi nhỏ.

Đánh giá: phạm vi Advanced hẹp có thể kiểm soát nhưng vẫn là feature nghiệp vụ riêng (model/migration/service/API/FE/report/tests), không chỉ thêm nút. Bao gồm Normal/đã có lịch/đã học/đã thu thật thì độ phức tạp tăng rõ. Chưa ước lượng ngày công hoặc nói test cũ đủ. Chưa chọn thay Owner, không code/tests/Maven/DB/deploy; giữ câu hỏi chọn phạm vi, không triển khai ngoại lệ Normal.

### 35.4. D16 chốt 2 trong phạm vi Advanced hẹp

Owner chọn “2” sau giải thích 35.3. Giữ core thu Cash/note tùy chọn/biên nhận xem-in và thêm CenterManager xử lý xác nhận Cash nhầm **chưa thực nhận tiền**, Advanced liên kết đúng subscription, chưa dùng/booking/điều chỉnh/phụ thuộc. Kiểm lịch sử, không chỉ credit=12. Khóa/transaction/idempotency thu hồi đúng quyền và ghi điều chỉnh, giữ chứng từ/amount gốc, báo cáo phản ánh khoản ghi nhận sai; audit trước-sau/thông báo sau commit.

Không mở ngoại lệ hủy Normal hoặc tự hủy lịch để đủ điều kiện đảo. Có tác động/thiếu căn cứ thì chặn tự đảo và giữ hồ sơ đối soát. Đã thu thật khác bấm nhầm chưa thu; hoàn thật theo nguyên tắc ngoài SCMS/có chứng cứ, không giả refund cho khoản chưa nhận. Receptionist thu Cash theo quyền cũ, không tự duyệt đảo; CenterManager duyệt đảo không đồng nghĩa được thu Cash. Chưa code/API/migration/tests/Maven/DB/deploy.

## 36. D17 — QR Dashboard Member: contract BE và handoff FE

CheckinCodeResponse là `{payload, expiresInSeconds}`. AttendanceService.createCheckinCode lấy Member theo Auth, khóa Member, tạo secret nếu chưa có; TOTP bước 30 giây, expiresInSeconds là số giây còn lại (1–30), không luôn 30. GET /api/members/me/checkin-code yêu cầu ROLE_MEMBER, có alias trong AttendanceController. Không trả secret cho FE. GET này có thể ghi secret lần đầu, không coi mọi probe QR là không mutation.

Báo cáo FE cũ nêu sai TTL/QR giả khi lỗi, chưa kiểm source FE hiện hành; không lấy FE_Project-main cũ làm chứng cứ bug live. Handoff: dùng payload/TTL thật, refresh/dừng mã hết hạn, không QR giả thay lỗi; loading/error/retry rõ, 401 đăng nhập lại, đổi account không giữ mã cũ. D15 chỉ nhận scan attendance trong giờ Session; có QR không tự mở quyền ngoài giờ.

Kiểm TTL biên, đổi account, replay/rate-limit và lỗi API trên môi trường D11 rồi FE API/browser thật; không đổi tên field vì FE cũ đọc sai. D17 là handoff/verification, không business rule mới cần lựa chọn từ source đã đọc; chưa FE verified/probe mới/code hoặc bật check-in cửa MVP.

## 37. D18 — quyền dashboard/báo cáo và UX

### 37.1. Source và bằng chứng

ReportController /api/reports/dashboard cho VIEW_REPORTS hoặc MANAGE_USERS hoặc MANAGE_CLASSES. ReportService trả cả tổng thu Paid/Invoice/gói từ danh sách repository, không phải chỉ số Coach theo buổi được giao. Revenue/members có authorities khác, cần rà mọi endpoint liên quan/scope. Seed có CenterManager/Admin VIEW_REPORTS; bằng chứng lịch sử Coach live có MANAGE_CLASSES nên 200 phù hợp annotation, chưa tự là lỗi auth. Không sửa role DB live chỉ để test khớp.

Role và authority phải phân biệt; Coach thao tác đúng phạm vi D04 không đồng nghĩa cấp MANAGE_CLASSES toàn cục hoặc xem tài chính. Admin đã có trong schema. centerId là phạm vi dữ liệu có sẵn, không thêm mô hình nhiều cơ sở; kiểm scope BE, không chỉ FE ẩn card.

### 37.2. Ba hướng tổng thể — chờ chọn

1. **Dashboard/báo cáo quản trị cho CenterManager/Admin có quyền báo cáo, Coach dùng màn nghiệp vụ của mình — đề xuất.** Không dùng MANAGE_CLASSES đơn lẻ làm quyền xem dashboard tài chính; Coach xem lịch/Member/buổi được phân công qua dữ liệu được phép, không tổng doanh thu/Invoice toàn hệ thống. Rà report query và phạm vi tài khoản hiện có, không mở contact/health từ quyền dashboard. Receptionist giữ thu/Invoice theo quyền cũ, không tự thêm quyền report quản trị.
2. **Giữ policy authority hiện tại:** Coach được MANAGE_CLASSES vẫn xem dashboard quản trị kể cả tổng tài chính; cần Owner chấp thuận phạm vi này. Ít đổi annotation hơn, vẫn cần kiểm query scope.
3. **Một dashboard đổi dữ liệu theo role:** Manager/Admin số quản trị, Coach số cá nhân, Receptionist số quầy. Không trả dữ liệu nhạy cảm rồi trông FE ẩn; nhiều DTO/query/permission matrix hơn, để nâng cấp nếu chưa cần.

**Cần Owner chọn 1/2/3.** Khuyến nghị 1 vì dễ kiểm quyền và tách report khỏi nghiệp vụ; không suy quyền từ tên role chung. Chưa thay authorities/annotation/query/DTO hoặc deploy; D19 cập nhật tests đúng policy sau khi chốt, không đổi expected chỉ để xanh.

### 37.3. D18 đã chốt 1 — quản trị và nghiệp vụ Coach tách phạm vi

Owner chọn “1” ngày 27/10/2026. Dashboard/report quản trị dành cho CenterManager/Admin có quyền báo cáo; MANAGE_CLASSES riêng không đủ để đọc tổng tài chính. Coach dùng dữ liệu lịch/Member/buổi trong phạm vi được giao, không tổng doanh thu/Invoice hệ thống. Receptionist giữ chức năng thu/Invoice theo quyền nghiệp vụ cũ, không tự thêm report quản trị. BE kiểm role/authority và query scope, không chỉ FE ẩn card; không đổi authority DB live để ép test. Chưa code/annotation/DTO/query/test/deploy.

## 38. D19 — kế hoạch kiểm thử và bằng chứng theo các quyết định mới

### 38.1. Phân loại lỗi và giới hạn bằng chứng

Báo cáo FE 24/10 có test logic/PaymentModal/selector/fixture/helper chưa khớp, có lượt dừng/timeout; chỉ là báo cáo lịch sử. FE_Project-main đã xác nhận lỗi thời, không sửa test/build thư mục đó để tuyên bố FE hiện hành pass. Khi tích hợp cần source FE/commit và contract BE tương ứng; test dùng loopback/mock chỉ chứng minh phần nó thực chạy, không thành E2E Render. Tránh build song song chung dist.

Backend hiện có unit/MockMvc/H2 và opt-in PG/live. Ví dụ AuthorizationHttpTest đang cấm Coach book nhưng cho Receptionist REGISTER_MEMBER book; đây là kỳ vọng implementation cũ, cần giải trình cập nhật theo D04 đã chốt, không hạ policy mới để test xanh. Giữ tests/method chế tài bookingBlock dù toggle=false. Các test live Cash/prepare có thể ghi DB, không bật mặc định; D11 đã chọn PG local riêng.

### 38.2. Quy trình kiểm gộp, không cần lựa chọn nghiệp vụ mới

1. Trước sửa ghi baseline từ source hiện tại, kiểm cấu hình/guard test không dùng DB đang chạy. D09 cần tái hiện thật trước sửa, không test Mockito thay cho Hibernate lazy loading.
2. Mỗi nhóm BE sửa có regression tương ứng, chạy `.\mvnw.cmd clean test` bắt buộc; ghi run/pass/failure/error/skip từ lần mới và lý do skip. Khi rule đổi, ghi quyết định làm căn cứ rồi cập nhật expected; không dùng số pass cũ làm acceptance.
3. PG local riêng kiểm schema/migration/bảo toàn dữ liệu và transaction/locking/race. Ma trận chính: Paid trước quyền, 5 phút, Return không ghi/IPN idempotent, refund/điều chỉnh tiền, giá snapshot, 45 ngày skip + D08 không trùng, tạo/dời/hủy/báo vắng, Normal đủ 12, attendance giờ buổi/Present-Absent/sửa sai/bù sự cố, report theo người/quyền role, QR TTL/replay và Owner/assignment/phạm vi.
4. FE hiện hành nhận method/path/DTO/role/error/fixture/SHA thật, kiểm UI tạo–dời dùng chung/cũ-mới/deadline/thanh toán chờ IPN/QR lỗi/report người/preview Holiday. Lỗi do selector/fixture/schema test thì sửa test có bằng chứng, lỗi BE/FE thì sửa sản phẩm đúng phần; không thay API bằng mock khi nghiệm thu tích hợp.
5. Khi được yêu cầu deploy mới kiểm SHA/config/migration/DB đích và smoke HTTPS/CORS/cookie/IPN với fixture được phép; còn dữ liệu/quyền chưa kiểm phải ghi chưa verified. Local xanh không thay nghiệm thu cloud, không reset dữ liệu ngoài phạm vi.

D19 là kế hoạch kiểm thử thực hiện theo phạm vi code được giao, chưa chạy baseline/test mới trong lượt plan này. Chưa triển khai toàn bộ D01–D19, chưa setup PG/migration/SQL/deploy. Còn điểm công thức báo cáo tiền liên quan D10/D16 cần chốt tại 39, không đánh dấu mọi nghiệp vụ đã đủ.

## 39. D13/D16 liên quan — chỉ số tiền sau hoàn và điều chỉnh Cash

### 39.1. Vì sao phải chốt cùng các nghiệp vụ đã chọn

ReportService.dashboard/revenueReport hiện cộng Invoice Paid.amount. D10 giữ Paid/amount gốc khi hoàn ngoài SCMS; D16 giữ chứng từ gốc và hồ sơ điều chỉnh khi Cash xác nhận nhầm. Nếu chỉ cộng Paid như cũ, số tiền hiển thị có thể bao gồm cả khoản đã hoàn và khoản xác nhận nhầm chưa thực nhận. Đã cảnh báo liên quan tại 31.1, nay cần chọn nghĩa UI/contract trước khi code report mới. Không sửa amount hoặc tạo thanh toán giả để làm tổng khớp.

### 39.2. Ba cách trình bày tổng thể — chờ chọn

1. **Tiền đã thu, đã hoàn, thu ròng — đề xuất cho project.** Tiền đã thu là các khoản thực nhận hợp lệ (có khoản Paid cần đối soát đã nhận tiền, loại phần xác nhận nhầm đã được điều chỉnh có căn cứ); tiền đã hoàn chỉ tính hồ sơ xác nhận đã hoàn, không chỉ duyệt; thu ròng=đã thu−đã hoàn. UI hiển thị ba số có nhãn rõ, ưu tiên thu ròng. Khoản cần đối soát vẫn có nhãn riêng, không suy đã cấp quyền hoặc đã hoàn. Đây là chỉ số vận hành tiền, không tự thêm sổ kế toán/phân bổ doanh thu theo buổi.
2. **Chỉ thu ròng làm số chính**, chi tiết tiền thu/hoàn/điều chỉnh nằm trong lịch sử hoặc màn chi tiết. Ít card nhưng vẫn cần cùng tính toán dữ liệu như 1; không thực sự tiết kiệm logic BE.
3. **Giữ tổng Paid như một chỉ số giao dịch gốc**, thêm bảng hoàn/điều chỉnh riêng, không gọi tổng Paid là tiền còn giữ. Ít đổi công thức cũ nhưng người dùng khó đối chiếu và dễ hiểu nhầm khi không xem chi tiết.

Khuyến nghị 1 vì phù hợp hồ sơ hoàn/điều chỉnh đã cần làm, thêm tổng hợp rõ nghĩa thay vì hệ thống kế toán mới. **Cần Owner chọn 1/2/3.** Cách lọc kỳ phải phân biệt ngày nhận/ngày hoàn/ngày điều chỉnh; khi thiết kế sẽ nêu rõ, chưa suy hoàn tháng sau phải sửa số tiền thu gốc tháng trước. Chưa công bố field mới hoặc đổi nghĩa totalRevenue âm thầm. Không thay điều kiện kích hoạt hay quyền báo cáo đã chốt. Chỉ source/docs, chưa code/tests/Maven/DB/deploy.

### 39.3. Chốt trả đủ ba số, FE quyết định trình bày — 27/10/2026

Owner chọn “1. Sẽ trả cả 3, Nhưng hiển thị tiền nào chính, hay cả 3 nên là việc của FE quyết”. BE cung cấp tiền đã thu, tiền đã hoàn và thu ròng theo định nghĩa hướng 1 ở 39.2. Bỏ phần đề xuất ưu tiên thu ròng trên UI: FE tự quyết chỉ số chính, card/thứ tự/bố cục hoặc hiển thị cả ba. BE chịu trách nhiệm công thức, đơn vị, kỳ lọc, dữ liệu và quyền truy cập rõ ràng; không ép layout qua contract.

Chỉ tính hoàn đã có xác nhận thực hiện, không chỉ duyệt. Xác nhận Cash nhầm/chưa nhận tiền đã được điều chỉnh không trở thành thu thật hoặc refund giả; giữ chứng từ/amount gốc và hồ sơ điều chỉnh. Khoản Paid cần đối soát đã nhận tiền phải được phản ánh đúng phần tiền nhận, không suy đã kích hoạt quyền học. Không mở sổ kế toán/phân bổ doanh thu theo buổi từ quyết định ba chỉ số vận hành này.

Khi code phải công bố tên field/DTO chính xác, nghĩa/kỳ lọc và compatibility cho totalRevenue cũ trong docs 01; không gọi đề xuất field là API đã có hoặc đổi nghĩa ngầm. Test tiền thu/hoàn khác kỳ, mới duyệt chưa hoàn, điều chỉnh Cash chưa nhận tiền, callback/retry không nhân tổng và quyền report D18. Ghi kết quả mới sau sửa, không dùng test lịch sử. Chưa code/API/migration/test/Maven/DB/deploy; các câu chờ chọn 39.2 là lịch sử.

## 40. Phân công ba phần và hai phase chính — đề xuất 27/10/2026

> **Cập nhật Owner sau phân công:** gói T bên dưới là đề xuất chưa được thực hiện; công việc đồng đội thực tế là SMTP/OTP đăng ký, chatbox cơ bản và thông báo. Giữ phạm vi D01/D09 và hai phase như backlog, không đánh dấu hoàn tất do merge các feature khác. Trạng thái merge/kiểm/bàn giao mới nhất tại 40.12.

### 40.1. Mục tiêu, tỷ trọng và nguyên tắc thực hiện

Owner yêu cầu giao đồng đội phần nhỏ khoảng 10–20%, ít va chạm/ngoài flow chính; phần còn lại làm hai phase tuần tự, gộp phụ thuộc và kiểm liên tục, demo thật/có bằng chứng. Phân công dưới đây là đề xuất triển khai, không thay business rule hoặc cho phép tự chạy code/DB/deploy trong lượt lập plan. Không gọi 19 mã D là 19 khối bằng nhau.

| Phần | Phạm vi | Tỷ trọng định hướng |
|---|---|---|
| T — đồng đội | D01 hồ sơ Member + D09 Coach directory/read fix và tests | Khoảng 15%, điều chỉnh trong 10–20% sau breakdown kỹ thuật |
| Phase 1 — phía BE chúng ta | Catalog/giá + đăng ký Advanced theo môn/Coach + thời hạn + mua/thanh toán/ghi nhận quyền | Khoảng 40% |
| Phase 2 — phía BE chúng ta | Dùng quyền/booking/dời/hủy/báo vắng + sự cố/attendance/Holiday + đối soát/sửa sai/report/tích hợp | Khoảng 45% |

Tỷ trọng là ước lượng phân bổ tương đối, chưa là ngày công hoặc bảo đảm effort chính xác. T có thể song song công việc Phase 1 không chạm file T; merge T sớm trước demo Phase 1. Phase 2 bắt đầu khi cổng nghiệm thu Phase 1 đạt. Hai phase là thứ tự triển khai, chưa phải hai release production đầy đủ; feature phụ thuộc phase sau chưa được handoff như đã hoàn chỉnh.

### 40.2. Chuẩn bị mốc chung và ranh giới merge

Workspace tai/8e95759 đang dirty, docs 04 untracked, có schema/migration/test và các file chuyển old-docs từ trước. Không gửi mỗi HEAD 8e95759 cho đồng đội như thể đã chứa quyết định mới. Trước coding cần một mốc bàn giao có source + các docs quyết định hiện tại, phân biệt thay đổi tồn tại sẵn với thay đổi từng người; chỉ commit/chia sẻ/push khi có yêu cầu tương ứng, không tự stage toàn bộ dirty tree.

- Mỗi người dùng nhánh/checkout riêng từ mốc chung. Không chỉnh cùng worktree hay chạy build/test dùng chung target/dist đồng thời. Đối chiếu kết quả trên nhánh tích hợp sau merge, không chỉ từng nhánh xanh.
- BE chính giữ quyền ghi các luồng Invoice/MemberPackage/Session/Enrollment/Holiday/Training/Report/SecurityConfig/Clock, entity chung, schema/migrations và docs 00/01/02/04. Số thứ tự migration do một người cấp; không để hai người tạo cùng số hoặc chạy migration trên DB chung.
- T nhận các file cụ thể ở 40.3. Trong lúc song song BE chính chỉ đọc các file T đang sở hữu; nếu cần đổi shared file thì phối hợp chuyển quyền hoặc merge xong trước, không sửa ngầm ở cả hai nhánh.
- T bàn giao JSON contract thật/test/evidence trong nội dung PR hoặc biên bản bàn giao của họ; BE chính đưa phần đã xác minh vào bốn docs. Không yêu cầu T sửa chung docs/schema để tránh conflict; không tạo thêm docs chủ chốt ngoài cấu trúc đã dùng.
- Một commit D01, một commit D09 và các sửa test gắn đúng phần, tránh format/rename toàn repo. Giữ nguyên các thay đổi/old-docs của Owner; merge/cherry-pick đúng commit được duyệt, không mang lại tài liệu đã di chuyển hoặc seed/reset.

### 40.3. Gói T gửi đồng đội — D01 và D09

**Vì sao chọn:** Member đã có goal/healthNote, User đã có name/phone/address/birth/gender; không cần schema mới cho D01. D09 là danh sách đọc và lỗi lazy đã có bằng chứng, không thay nghiệp vụ nhận học viên/chọn Coach/giữ ca. Không giao QR/report/Cash vì vướng D15, quyền D18 và tiền D10/D16; không giao FE cũ.

**D01:** thêm controller/service/DTO riêng cho GET/PATCH /api/members/me, ví dụ MemberSelfProfileController/Service trong package user. Giữ nguyên MemberController/MemberService quản trị và entity User/Member/repository chung nếu không cần sửa; nếu cần shared edit thì báo BE chính trước. Danh tính từ Auth, chỉ đúng bảy trường đã chốt; JSON tên `name`, map vào User.fullName. Không nhận role/status/center/package/credit/amount. PATCH phân biệt bỏ qua với null xóa trường cho phép; field bắt buộc, kiểu/validation/null và response thật phải ghi trong handoff trước tích hợp FE, không tự mở field mới. Email/password/avatar giữ API riêng.

**D09:** sở hữu CoachController chỉ phần GET danh sách, CoachRepository và CoachSpecializationRepository chỉ bổ sung query đọc, thêm CoachDirectoryService và tests chuyên biệt. Method assignment/các đường Coach available-slots/at-risk giữ contract và hành vi cũ. @Transactional(readOnly=true) ở service được Spring gọi qua proxy, lọc center từ context hiện có trước batch fetch specialization và DTO; không OSIV/EAGER toàn cục. Giữ response/contact MANAGE_USERS/keyword/specializationId/Coach không chuyên môn. Nếu cần query repository mới để tránh shared edit thì chỉ phục vụ đọc directory, không đổi entity/seed. BE chính không sửa các file này tới khi merge T hoặc có phối hợp rõ.

**Không thuộc gói T:** migrations/DB live/.env/seed; Subject–Primary Coach model/lịch làm việc/năng lực nhận học viên; booking, credit, payments, expiry, Holiday, attendance, reports; SecurityConfig/role authority rộng; FE_Project-main; deploy/old-docs.

**Tiêu chí bàn giao T:**

1. D01 tests Auth/owner/field bảo vệ, PATCH bỏ qua/null/validation và persistence/rollback. D09 tái hiện OSIV=false bằng Hibernate thật trước sửa (không transaction test che lỗi), regression có dữ liệu sau sửa; batch không tăng query theo mỗi Coach, đúng filter/contact/phạm vi, zero specialization trả [].
2. Chạy `.\mvnw.cmd clean test` mới sau BE changes; ghi command/environment/tổng run/pass/fail/error/skip, không dùng số cũ. Tests chỉ DB cô lập, không opt-in live. Kết quả trước sửa/sau sửa D09 phải ghi đúng loại test, không giả thành Render stack.
3. Handoff gồm base/commit, danh sách file đổi, API JSON thật/success/errors, fixture/test data, bằng chứng local và giới hạn chưa deploy/chưa FE verified; không có token/cookie/secret. Merge xong BE chính chạy lại clean test và kiểm API trên baseline tích hợp.

### 40.4. Phase 1 — nền dữ liệu và luồng mua/thanh toán/ghi nhận quyền

| Bước | Việc gộp theo phụ thuộc | Cổng kiểm/demo |
|---|---|---|
| P1.0 | Chốt mốc/ownership/contract kỹ thuật, baseline test, PG local riêng theo D11; lập manifest entity/migration/API tương thích | Xác nhận DB test riêng không dùng .env live; ghi SHA và baseline; phân loại failure cũ trước khi sửa |
| P1.1 | D02 chuẩn 9 môn/18 giá, chặn tái seed/giá 0/bản dư; đối soát chỉ fixture đã được phép, bảo toàn Invoice snapshot | PG migration nâng cấp/lặp/giữ dữ liệu, không reset 05; API catalog đúng, hóa đơn cũ không đổi |
| P1.2 | D03 đăng ký theo môn/Primary Coach, lịch làm việc/khả năng nhận và Coach/Manager xác nhận trước Invoice; khóa/idempotency cam kết năng lực Pending | Từ chối đúng môn/Coach/năng lực, chưa chấp nhận thì chưa phát hành Invoice/chạy hạn; không hứa giữ ca khi chọn Coach |
| P1.3 | Deadline chính xác từ Paid + 45 ngày sử dụng skip TT đóng, dữ kiện expiry/D08 tránh bù trùng; time policy dùng chung cho phase sau | Clock tests + PG kiểm thời hạn, liên tiếp ngày nghỉ/phần hạn kéo thêm/bằng deadline; dùng fixture lịch nghỉ cố định cho demo P1, quản lý tác động sửa Holiday hoàn chỉnh ở P2 |
| P1.4 | D10/D16 core mua Cash/VNPay, giá snapshot/5 phút, chặn đơn thiếu dữ liệu, Paid mới Active/12 credit, Return đọc/IPN ghi, retry/late cần đối soát | HTTP login thật trên BE local, tạo đơn đúng API; payment callback mô phỏng có chữ ký chỉ ghi nhận là local integration; expiry/replay/race không nhân quyền |
| P1.5 | Merge T, contract/demo tích hợp mua–thanh toán–quyền; receipt/note Cash cơ bản, notification event/data chuẩn bị sau commit | clean test + PG + JSON/DB read assertions, bàn giao FE đúng phiên bản, danh sách feature P2 còn thiếu rõ |

Không đưa workflow hoàn/sửa Cash, booking, thay Holiday có lịch đã tồn tại hoặc report cuối vào demo P1 như đã xong. Time-policy/schema phải đủ dùng chung để P2 không có một nguồn deadline khác; phase sau hoàn thiện guard mọi đường cập nhật/lịch cũ. P1 chưa được coi hệ thống vận hành trọn vẹn hoặc tự deploy chỉ vì test mua xanh.

**Demo P1 bắt buộc:** profile/directory T; lấy đúng catalog, chấp nhận đăng ký theo môn/Coach → tạo Invoice snapshot → Cash hợp lệ hoặc IPN local đã ký → đúng một quyền Active/12 credit/deadline; thay giá catalog không đổi amount cũ; đơn hết 5 phút không giữ quyền, IPN muộn cần đối soát; Return/refresh không cấp quyền; thiếu quyền/sai owner không thành công. Không chèn Paid/Active bằng SQL để dựng demo mua thành công.

### 40.5. Phase 2 — sử dụng quyền, xử lý ngoại lệ và hoàn thiện báo cáo

| Bước | Việc gộp theo phụ thuộc | Cổng kiểm/demo |
|---|---|---|
| P2.1 | D04–D07 tạo/dời chung, Coach chính/Manager hỗ trợ/consent, idempotency/locks; 12h tạo–hủy–dời/báo vắng; ca chuẩn/lễ/nắng/room/Coach/Member/deadline | Slot cuối/race/rollback, credit một lần; no-op/đổi sân–Coach cùng giờ giữ dấu báo vắng, dời thật bỏ hiệu lực và giữ audit |
| P2.2 | D06/D08/D12/D14 cùng một nền sự cố/ngày đóng: thay Coach/dời/hủy vì TT, bù/expiry snapshot/khôi phục có chứng cứ, skip/không trùng; Holiday preview/xử lý riêng/confirm/cũ-mới | Sự cố chưa rõ kết thúc Chờ khắc phục; toggle tương lai/tắt rút hạn và ca vượt hạn; giữ nghỉ quá khứ; preview cũ/race bị chặn, không tự hủy/dời hàng loạt |
| P2.3 | D15/D17 attendance Present/Absent trong buổi, QR TTL/replay/owner, finalization/CenterManager sửa sai; sự cố thật báo muộn bù có hồ sơ | Không Late/scan sớm cho record mới, không double credit; Normal đủ 12 buổi hợp lệ và liên kết buổi bù; giữ lịch sử Late, check-in cửa để nâng cấp |
| P2.4 | D10/D16 xử lý Paid cần đối soát hoàn ngoài SCMS/có chứng cứ; Cash sửa nhầm hẹp Advanced chưa nhận/chưa dùng/booking/phụ thuộc, không Normal; note/receipt/audit/report dữ kiện | Chỉ duyệt chưa coi hoàn; không đảo khi có lịch/sử dụng; không bù/hoàn/đảo lặp hoặc tiền giả, giữ amount/chứng từ gốc |
| P2.5 | D13/D18/39 report theo người + đăng ký, 3 số tiền và role/query scope; D19 full regression/FE handoff/tích hợp | Coach không đọc tổng tài chính, Manager/Admin đúng quyền; credit 0 còn lịch vẫn còn quyền; tiền thu/hoàn/điều chỉnh khác kỳ đúng; BE trả ba số, FE quyết định layout |

P2.2/P2.3 phụ thuộc P2.1, report phụ thuộc kết quả sự cố/hoàn/điều chỉnh thật nên làm cuối. Không giao report tiền cho nhánh độc lập trước khi model hoàn/đảo được thống nhất. Mọi bước sửa BE đều clean test; PG/race checks theo dữ liệu/transaction thay đổi, không đợi cuối phase mới kiểm rồi sửa một loạt test.

### 40.6. Demo, bằng chứng và điều kiện qua phase

- Mỗi bước có kịch bản đầu vào, login thật theo role/fixture, request/response status và body cần thiết, kiểm DB chỉ đọc trước-sau/invariants, kết quả tests và lỗi còn mở. Không log token/cookie/secret hoặc dữ liệu cá nhân không cần cho chứng minh.
- Bằng chứng để ngoài các docs chủ chốt, ví dụ artifacts/demo/phase-1/<run-id>/ và phase-2 tương ứng; docs 00/02 trỏ tới manifest SHA/source/config/migrations/fixtures/commands/results. Path là đề xuất chưa có artifact chạy thật. Đánh dấu LOCAL UNIT/INTEGRATION, LOCAL HTTP, GATEWAY SANDBOX LIVE, DEPLOY/FE BROWSER đúng loại.
- Dùng mock/Clock cố định cho unit/biên lỗi và callback local có chữ ký hợp lệ để test xử lý; ghi đúng là mô phỏng giao thức local. Không gọi nó thanh toán VNPay thật, không dùng WithMockUser làm chứng cứ login thực, không lấy count tests xanh làm đủ bằng chứng browser.
- Demo nghiệp vụ qua HTTP/DB thật trong PG local, fixture tạo có kiểm soát; schema/danh mục/account test có thể seed rõ nguồn, nhưng không SQL chèn Paid/Active/credit để giả vờ API thực hiện. Giữ khởi tạo fixture khác thao tác đang cần chứng minh.
- E2E FE cần source FE hiện hành, commit/build/config nối đúng BE; không sửa hay demo FE_Project-main cũ như bản hiện tại. Chưa có FE mới thì bàn giao BE API và đánh dấu FE pending, không fake màn FE đã tích hợp.
- Chỉ nghiệm thu cổng phase khi tests yêu cầu không failure/error, các skip có lý do, migration/PG checks đạt, kịch bản quan trọng có data assertions và file evidence thật; không đánh dấu D hoàn tất nếu mới docs/code chưa chạy. Các hạn chế chưa xử lý ghi rõ trước chuyển phase.
- Deploy khi được yêu cầu và sau preflight SHA/DB/config/migration/fixture; kiểm CORS/cookie/IPN merchant thực trên đúng bản. Chưa có quyền/setup gateway thì giữ pending cho SANDBOX LIVE/DEPLOY, không dùng local evidence thay. Lỗi mới/nghiệp vụ mâu thuẫn dừng chỉ rõ, không tự mở rule mới.

### 40.7. Prompt ngắn gửi đồng đội

```text
Bạn làm BE SCMS trong nhánh riêng từ mốc bàn giao đã chứa AGENTS.md và docs quyết định hiện tại (không mặc định HEAD 8e95759 chứa docs mới). Chỉ nhận gói T: D01 GET/PATCH /api/members/me với DTO/service/controller tự phục vụ riêng, bảy trường name/phone/address/dateOfBirth/gender/goal/healthNote; và D09 sửa GET /api/coaches lazy specialization bằng service read-only transaction + center-scoped batch query, giữ API/filter/contact.
Đọc docs/04-DEBUG-NOTE.md mục 0, 6.3–6.4, 20.5–20.6 và 40.2–40.3; docs/01 mục 4.5/14 và docs/02 mục 9. Chi tiết quyết định cuối mới hơn phân tích cũ. Đọc đủ yêu cầu áp dụng, không đọc old-docs hoặc FE_Project-main cũ.
Không sửa flow đăng ký/mua/booking/credit/time/Holiday/report/attendance/SecurityConfig/Clock, entity chung, schema/migrations/.env/seed hoặc bốn docs trung tâm. Sở hữu file D01 mới và các file directory ở 40.3; cần shared file khác thì phối hợp trước. Không mutation DB thật/deploy. Không khôi phục file Owner đã chuyển old-docs.
Tái hiện D09 bằng JPA thật OSIV=false trước sửa; test regression Auth/owner/DTO/filter/contact/phạm vi/N+1. Chạy .\mvnw.cmd clean test sau sửa, DB test cô lập. Hai commit chức năng riêng, tránh format repo. Handoff base/commit/file đổi, contract JSON/error thật, fixture/commands/kết quả/evidence trước-sau/giới hạn. Không gọi local là live, không QR/mock/SQL Paid-Active thay luồng thật. BE chính cập nhật docs/merge và chạy kiểm lại nhánh tích hợp.
```

Phần prompt là bản giao việc đề xuất, chưa có nhánh/commit mới hoặc đồng đội được kích hoạt trong lượt này. Không tự tạo agent/push/DB/deploy. Thứ tự/boundary là cải thiện kỹ thuật cho yêu cầu Owner, không chốt thêm business rule.

### 40.8. Checklist làm việc cho đồng đội — đọc theo thứ tự

**Bắt đầu:**

- [ ] Lấy mốc bàn giao mới trên tai sau khi BE chính push; ghi base SHA. Tạo nhánh riêng, ví dụ `feat/member-profile-coach-directory`; không commit/push trực tiếp vào tai hoặc stage thay đổi của người khác.
- [ ] Đọc checkpoint mới trong AGENTS và 04 mục 0 trước. Tên “đã chốt” mới hơn câu “chờ chọn” cũ. Chỉ nhận D01 và D09; không triển khai Phase 1/2 vì thấy docs có kế hoạch.
- [ ] Kiểm git status và danh sách file được sở hữu ở 40.3. Cần file ngoài phạm vi thì phối hợp BE chính trước khi sửa, không tiện tay sửa entity/security/schema.
- [ ] Chuẩn bị DB test H2/PG cô lập, datasource test khai báo rõ; không nạp .env của DB cloud, không bật các test live/prepare/demo opt-in. .env của mỗi người không commit và không gửi password trong bằng chứng.

**D01 — làm riêng một chức năng:**

- [ ] Thêm controller/service/DTO tự phục vụ, không nới quyền endpoint /api/members/{id} quản trị. Literal /me phải được Spring nhận đúng, không bị coi là id.
- [ ] Resolve Member từ Authentication.getName() (email); principal hiện không có property memberId. Không nhận memberId từ body để chọn người được sửa.
- [ ] GET trả hồ sơ đúng chủ; PATCH chỉ bảy trường name/phone/address/dateOfBirth/gender/goal/healthNote. `name` map User.fullName; goal/healthNote thuộc Member. Các thao tác lưu liên quan chung transaction.
- [ ] PATCH bỏ qua trường không gửi; null chỉ xóa trường cho phép xóa. Không tự coi tất cả field đều nullable hoặc lấy DTO quản trị chứa role/status/package. Chốt/ghi kỹ thuật JSON/null/validation theo schema và API hiện có trước handoff; không tự phát minh enum/field hoặc thay nghiệp vụ.
- [ ] Body cố thêm role/status/centerId/credit/package/amount không được làm đổi dữ liệu quản trị; email/password/avatar theo luồng riêng. Response không trả secret/hash/token.
- [ ] Test 401, role không phù hợp, đúng chủ, field bảo vệ, GET sau PATCH, ngày/độ dài/null/absent và rollback User/Member. Không kiểm chỉ status 200 rồi bỏ qua giá trị đã lưu.

**D09 — làm riêng lỗi đọc danh sách:**

- [ ] Lưu tái hiện trước sửa bằng entity/repository thật với OSIV=false; seed trong transaction riêng rồi đóng context. Request/test method không được bao transaction làm proxy vô tình còn sống; dùng Coach có chuyên môn để chạm getName, không chỉ ID.
- [ ] Service public được Spring gọi qua bean proxy, read-only transaction bao query/filter/map DTO. Coach query fetch user/role; lấy đúng phạm vi trước batch assignment+specialization. Không query chuyên môn theo từng Coach hoặc trả entity lazy để serializer nạp sau.
- [ ] Không specialization trả []; không kết quả trả []; filter specializationId/keyword giữ nghĩa hiện tại và giữ đầy đủ chuyên môn của Coach khớp. Contact chỉ khi MANAGE_USERS; role Manager riêng không tự đủ. Không bật OSIV/EAGER, sửa/xóa Specialization#5 hoặc đổi quyền cho Member.
- [ ] Không sửa API available-slots/at-risk hoặc khả năng nhận học viên D03; không đổi method assignment dùng chung. Test anon/Member/authority-contact/phạm vi/Admin theo hiện trạng, filter kết hợp, zero/multiple specialization, serialization ngoài service transaction, query count không tăng theo số Coach.

**Kết thúc từng chức năng:**

- [ ] Chạy `.\mvnw.cmd clean test`, lưu tổng run/pass/fail/error/skip và command/base/source version. Không chỉ `-DskipTests`, không dùng số test cũ, không bật live test để bổ sung count.
- [ ] Đọc lỗi đến root cause; sửa sản phẩm nếu sai behavior, sửa expected chỉ khi quyết định nghiệp vụ/fixture thực sự đổi và giải thích. Không xóa test, mock lazy exception đi hoặc giảm quyền kiểm để xanh.
- [ ] Nếu demo local HTTP, đăng nhập thật bằng account test, lưu request/response đã bỏ credential và xác minh DB sau thao tác. MockMvc/WithMockUser là test tích hợp tương ứng, không ghi là HTTP login thật/Render.
- [ ] Commit D01 và D09 tách biệt, scope diff đúng file đã giao. Handoff có base/head SHA, file đổi, endpoint/method/JSON/error thật, null/validation, cách chạy tests/dữ liệu, trước-sau D09, kết quả/skip và giới hạn chưa deploy/chưa FE verified.
- [ ] BE chính review diff, merge đúng commit, chạy lại clean test và kịch bản trên nhánh tích hợp rồi cập nhật bốn docs. Nhánh đồng đội pass chưa đồng nghĩa merge đã pass.

### 40.9. Khi nào được đánh dấu gói T hoàn thành?

Chỉ đánh dấu hoàn thành sau khi có đủ: code đúng D01/D09, không vượt file/rule; regression meaningful có dữ liệu thật cho JPA; clean test 0 failure/error, skip có lý do; contract thật/hướng chạy lặp được; evidence không lộ secret và phân loại local/live đúng; nhánh tích hợp kiểm lại sau merge. Không bắt số test bằng 138/223 lịch sử. Nếu vướng quyền/DB/source không có thì ghi blocker và phần còn thiếu, không giả có kết quả hoặc tự chuyển sang DB cloud.

### 40.10. Yêu cầu bàn giao và kết nối DB mới — 27/10/2026

Owner đã yêu cầu rà lời dặn/code/nội dung, commit và push branch tai, sau push thử kết nối cấu hình .env mới. Đây là ủy quyền commit/push baseline hiện có và kiểm kết nối chỉ đọc, chưa là lệnh triển khai D01/D09/Phase 1/2, chạy migration hoặc seed DB mới. .env được gitignore, không track/commit password. Ảnh là Supabase direct connection; .env hiện dùng session pooler port 5432 có project ref khớp ảnh, không coi khác hostname là khác project hoặc tự đổi .env.

Kiểm kết nối bằng driver trực tiếp, không boot Spring/jobs: TLS, transaction read-only, SELECT đơn giản/metadata schema rồi rollback; không chạy DB prepare hoặc opt-in test live như phép ping. Kết quả và SHA push thực ghi ở docs 02 khi đã chạy. Test hiện tại là kiểm baseline, không chứng nhận feature mới đã code. Giữ thay đổi/old-docs đã có, không đọc lại tài liệu lịch sử để tái phân tích nghiệp vụ; kiểm move bằng hash. Ghi kết quả kiểm thật trước bàn giao, không giữ chữ “chưa test” như trạng thái hiện tại sau khi đã chạy.

### 40.11. Kết quả bàn giao baseline và lịch sử kiểm kết nối

Đã bổ sung AGENTS checkpoint/checklist 40.8–40.9, review code F-06/F-07 sẵn có và chạy clean test 223/204 pass/19 skip/0 lỗi; PG riêng 14 tests ứng dụng pass cùng các guard migration/data snapshots. Không triển khai D01/D09/Phase 1/2 chỉ từ việc review. Đã push tai `9097197`, remote khớp, .env không track; code DB/archives/docs tách ba commit.

Sau push đã thử đúng .env: pooler trả tenant/user not found (XX000); direct từ ảnh trả UnknownHostException qua JDBC dù Windows DNS có AAAA. Chưa kết nối/SQL/schema thành công, không đổi .env/DB. Đã hỏi Owner cập nhật đúng Session pooler host/username từ panel, không yêu cầu password; không đoán region hoặc tự sửa config. Kết quả/JSON/chứng cứ docs 02 mục 9.11 và artifacts/review-2026-10-27. Lượt ghi kết quả/push tài liệu sau baseline không làm test cũ trở thành test rule mới.

Thử lại sau Owner cập nhật .env: host pooler mới phân giải DNS được; URL đang nhúng user/password kiểu URI, và password đó khác DB_PASSWORD. Đã hỏi chọn nguồn trước chuẩn hóa JDBC, không tự chọn password hoặc đổi file. Checker có preflight báo cấu hình/boolean thay vì để credential thành hostname; docs 02 mục 9.12 và JSON retry/config-check ghi đúng lỗi hiện tại. Chưa SQL/schema thành công; kết quả lỗi pooler cũ giữ làm lịch sử.

**Kết quả cuối:** Owner chọn dùng DB_PASSWORD hiện tại. Đã sửa duy nhất DB_URL local ignored thành JDBC Session pooler không nhúng credential, giữ nguyên DB_USERNAME/DB_PASSWORD. Kiểm JDBC thực thành công: sslmode=require, SELECT 1=1, PostgreSQL 17.11, transaction_read_only=on, rollback/đóng kết nối hoàn tất. Public có 0 bảng, chưa có sessions.subscription_id/schema SCMS; chưa migration/seed hoặc kiểm API/FE trên DB mới. Lỗi và câu “chưa kết nối” ở các đoạn trước là lịch sử, không hỏi lại nguồn password.

Bằng chứng JSON hiện tại và nhãn SSL đúng phạm vi tại docs 02 mục 9.13; pg_stat_ssl là kết nối phía PostgreSQL sau pooler, không đo đường JDBC tới pooler. Các lượt kiểm đều không boot Spring/jobs hay ghi dữ liệu nghiệp vụ. Baseline clean test/PG giữ nguyên kết quả đã chạy; lượt chuẩn hóa chỉ thay .env ignored, checker và docs/evidence, chưa triển khai feature hoặc deploy.

### 40.12. Checkpoint sau xử lý conflict — tiếp tục hai phase ở session sau

**Owner làm rõ:** đồng đội làm SMTP gửi OTP đăng ký email, chatbox cơ bản và thông báo; gói T D01/D09 vừa đề xuất chưa được làm. Không đánh dấu T hoàn thành hoặc dùng merge này làm nghiệm thu hai phase. D01/D09 vẫn là backlog với phạm vi/checklist đã ghi; không hỏi lại D01–D19 đã chốt.

**Merge hiện tại:** tai nền `fc00c64` + nhánh đồng đội `3309b02`. Đã ghép 8 file conflict (API contract, hai service OTP, InvoiceService, MemberPackageService, SessionService, AttendanceFinalizationProcessor, BusinessGapAuditTest). Giữ guards/Clock/locks/lifecycle/toggle của tai và event/hooks/OTP của nhánh đồng đội; sửa import ZoneId, Clock trùng trong ba test, constructor fixture, thứ tự mục API và đồng bộ hai schema ở mức file. Không chọn nguyên ours/theirs để làm mất một bên, không thay expected để xanh.

**Bằng chứng tích hợp:** `.\mvnw.cmd clean test` 269 run/250 pass/19 skip/0 failure-error, BUILD SUCCESS; XML đối chiếu log, artifact tại artifacts/merge-2026-10-27 và docs 02 mục 9.14. Hai lượt compile/testCompile trước fail do ghép import/Clock đã sửa. Đây là local unit/MockMvc/H2 của source hợp nhất, chưa SMTP delivery thật, migration notification/chat trên PostgreSQL, API/FE cloud hay deploy. Giữ .env/old-docs; DB Supabase mới đã kiểm kết nối trước đó nhưng public còn trống, chưa tự init/seed.

**Session sau:** đọc AGENTS → docs 00 active → mục 0/40.12 → kế hoạch 40.3–40.6 và bằng chứng docs 02; kiểm git status/log lấy SHA thực. Trước bắt đầu P1 kiểm baseline hợp nhất, D01/D09 chưa làm và các dependency/migration/fixture; tiếp tục từng nhóm P1 rồi P2, kiểm liên tục đúng cổng. Không sửa FE_Project-main lỗi thời để coi FE hiện hành đã tích hợp; các file FE/dist có sẵn trong nhánh đồng đội được giữ nhưng chưa build/browser verified. Lượt này kết thúc ở xử lý merge/bàn giao, chưa thực hiện hai phase.

### 40.13. Tiếp tục hai phase theo yêu cầu mới của Owner — đang kiểm chứng

- Phạm vi Owner xác nhận qua câu hỏi: làm P1.0–P1.4, toàn bộ BE P2, giữ P1.5/gói T D01/D09/FE ở backlog. Owner bổ sung yêu cầu double-check, thiết kế demo, viết tài liệu bàn giao/hướng dẫn, push branch rồi merge main. Không tự mở lại quyết định D01–D19.
- Source nền `tai`/`0277b2f`; đã fetch thấy `origin/main` ở `86d0076` (merge PR #10 từ tai). Working tree có code của lượt này; các file communication test/proof untracked xuất hiện từ công việc khác phải giữ riêng, không tự stage vào commit phase.
- P1 đã có clean test và ba luồng HTTP PostgreSQL thật local pass; artifacts/demo/phase-1/2026-10-27. P2 đã có chín luồng HTTP H2 thật và clean test pass, đang bổ sung double-check, kiểm lại PG/schema/release bundle và tài liệu. Count tests là chứng cứ từng lượt, không tiêu chí cố định. Bản cuối phải ghi lại đúng source sau commit/merge.
- Build trong worktree `D:/Projects/SWP391-phase-verification` vì từng có Maven khác chạy clean cùng target ở workspace chính. Không copy .env vào worktree kiểm chứng. PostgreSQL local `scms-phases-20261027`, chỉ bind 127.0.0.1:55440; script test-phases-postgres.py không đọc .env.
- Owner xác nhận DB Render là cấu hình ghi “production” trong .env. Preflight JDBC chỉ đọc thấy public có **0 bảng**, rollback hoàn tất; chưa migration/seed production. autoDeploy bật trong render.yaml nên cần trình tự schema → BE khi push/main. Đã chuẩn bị bootstrap cấu trúc/role/catalog không account hoặc giao dịch demo, migrations 06/07/08–12 và bundle kiểm tra hash/empty-schema guard; chưa tự coi đây là quyền ghi production.
- Điểm còn làm: chạy PG demo toàn bộ, kiểm release SQL trên DB local rỗng, hoàn tất review các đường cập nhật cũ/credit/deadline/quyền, đồng bộ docs/AGENTS, tạo evidence/manifest sạch rồi commit/push và merge đúng main. Sandbox gateway live, trình duyệt FE và SQL Server runtime không được gọi là đã nghiệm thu từ H2/PG local.

**Cập nhật thực thi production theo phê duyệt riêng:** Owner trả lời “Cho phép khởi tạo production trước merge” đối với bundle SHA256 `28635081e1724eb89ea82252fc1e468ca53805d347e4334d40ddf17a9c2866dc`. Đã chạy đúng bundle bằng ApplyPhaseRelease (hash/fingerprint/empty-schema guard, Serializable, atomic commit):9môn,0account,0Invoice/subscription/session/enrollment. Postflight JDBC read-only:46bảng,không thiếu8cột checkpoint,rollback hoàn tất. Artifacts production-release.json/production-postflight.json tại demo phase2. Chưa tài khoản thật/transaction/live gateway;không tự tạo Admin. Source feature9d8c07c,fixture regression13a62ee;đang chạy runner từ đúng committed checkout rồi merge main có kiểm lại.

**Cổng source chốt:** runner tạo worktree từ commit13a62ee không có.env, clean test292 run/273pass/19skip/0lỗi và PG HTTP9/9pass,migration replay/snapshot/FK guard pass. 19skip là baseline opt-in live/PG,không tính6communication tests ngoài commit. Đã tạo evidence manifest/test-summary/source-tree/trace và demo.html;hướng tái lập docs02 mục10 và artifact README. Tiếp theo push feature rồi merge main đúng history và kiểm lại bản merge.

**Đóng cổng merge:** feature199bbf9 đã push;merge origin/main86d0076→155968db6398e10689abbf07e1cf72fe0be3e61d trong worktree riêng,không conflict/tree bằng feature. Runner từ đúng mergecommit pass clean292/273pass/19skip/0lỗi +PG HTTP9/9 và migration guards. Evidence bản merge tại artifacts/demo/phase-2/2026-10-27/evidence;docs02 mục10.5. Gói đóng bàn giao sau đó chỉ docs/evidence/script newline LF,không đổi source BE. P1.5/T/D01/D09/FE/gateway live/SQLServer runtime giữ backlog rõ,không còn phaseBE được giao nào chưa kiểm. Production đã bootstrap có phê duyệt;chưa tạo account/giao dịch thật. Main remote phải đối chiếu sau push,không coi merge là Render đã triển khai.

### 40.14. Commit/merge phần communication còn sót và kiểm Render theo Owner

Owner yêu cầu commit/push branch/merge các thay đổi untracked còn lại,sau5phút kiểm Swagger Render. Đã adopt scripts/test-communication-features.py,hai files test communication và artifact INVALIDATED lịch sử;runner nay dùng committed checkout không.env,migrations current vàSMTPloopback localhost. Source3857be5 fresh6/6+upgrade6/6pass,merge2164747 clean298/273pass/25skip/0lỗi;mọi expected/timeout giữ nguyên,không thay src/main/schema. Lượt160013 FAIL được giữ,không gắn pass;diagnostic single-case sau pinlocalhost pass,rồi chạy đủ12case pass. Branch test/communication-proof đã push85ca855,main local đãmerge2164747,chuẩn bị push kết quả/docs rồi đợi>=300s. Kiểm production chỉ GET/anonymous,không seed/login/giao dịch. Chi tiết docs02 mục10.6;không mở lại P1.5/T/FE.

**Kết quả kiểm saupush765339c:**push16:16:30,kiểm16:21:43 UTC+7 (>=313s). Recheck17/19pass:SwaggerHTML/assets/config200,OpenAPI sau retry200/149paths/đủ endpoint vàfields mới. Hai lỗi còn lạihealth503DOWNvàcatalogRender18môn (9legacy nullslug+9canonical),đối chiếu JDBCread-onlyDB.env9môn/0account/giao dịch,fingerprintkhớpbootstrap.Cache-bust vẫn18;chưa có bằng chứng branch/DBruntime từ dashboard.Publicinfo404/khôngdeploySHA,healthcomponentdetailsẩn,không kết luận mail/DB cụ thể.Không đổiRenderconfig/migrateDBkhác khi cómâu thuẫn;đã hỏiOwner branchvàsyncDBenv theoAGENTS2.9,không yêu cầupassword. Chưa nghiệmthu production.Report artifacts/production-check/2026-10-27 vàdocs02 mục10.6. Commit đóngkết quả skiprender chỉdocs/evidence/audit,không thayBE/schema.

### 40.15. Tiếp tục sau Owner xác nhận Render main và DB đã sync

Owner xác nhận main/đãsync;runtime no-cache vẫn18môn/health503.Local.env khôngduplicatekeys/OSoverride.Phát hiệnCI.yml0byte,Actionsrun765339c failure0jobs ngay lập tức;đã sửaCIleastprivilege/pinnedactions/Java17cleanverify trênfix/backend-ci.GitHubrun40485174878/source729d8e4 success,không đổi src/main/schema. Đã hỏiliveSHA/deploy status/healthpath;khả năngRendergiữinstance cũ hoặcautodeploy chờCIchưađượcchứng minh.Chi tiết/evidence docs02 mục10.7.Không được coiconfirmmain/env làđãxác minh runtime hoặc tựmigrationDBkhác.
