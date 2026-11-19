# SCMS — Bàn giao BE cho FE: lịch học, hạn thao tác và điểm danh

**Đối tượng:** FE, BE và Owner. **Ngày hồ sơ:** 19/11/2026.

BE đã sửa và kiểm local theo phạm vi Owner giao. Đây là bản bàn giao implementation và bộ demo; chưa deploy, chạy SQL Supabase hoặc nghiệm thu FE/provider. Trạng thái, SHA nền, fingerprint working tree và bằng chứng duy nhất tại [Active/QA](docs/00-MASTER-CHECKLIST.md#active), mục `beDemoImplementation20261119`.

## 1. Kết quả hiện tại

BE hỗ trợ ghi điểm danh ban đầu trước start khi bật `SCMS_EARLY_ATTENDANCE_ENABLED`; đơn, bulk và QR dùng cùng điều kiện Normal/Advanced. Buổi có Present/Absent hiện hành chặn hủy hoàn credit. Metadata phản ánh actor, hạn và lý do; đăng ký Advanced có invoiceId/invoiceStatus để đọc lại hóa đơn cũ. Hóa đơn mới tối đa30phút, mã điểm danh đủ300giây từ lúc cấp; chứng từ/hạn cũ được giữ nguyên.

Đã kiểm trên Java17, H2 cách ly và PostgreSQL local riêng, gồm response và dữ liệu sau commit, owner/role/center, biên giờ, retry/rollback/race và migration26. Runner dùng source copy không.env, job/email/provider được mock hoặc dùng server giả lập loopback. Các lượt root trước khi sửa cách ly không được dùng làm bằng chứng nghiệm thu; QA giữ rõ giới hạn này.

Bộ19 đã kiểm SQL local và đường demo tại20:00/23:59:59; SYSTEM-DATA xuất từ fixture nguyên trạng trước HTTP. Bộ21 sinh từ cùng nguồn, kiểm ngoại tuyến cho15–18h, **SQL execution NOT_RUN tới đúng21/11**; SYSTEM-DATA21 là thiết kế dự kiến, chưa có ID DB nghiệm thu. Hai bộ được giữ riêng tại [database/bundles](database/bundles/2026-11-19/manifest.json).

## 2. Quá trình và quyết định giữ/bỏ

- Giữ Paid/Registered, khai giảng, Member Accept, quyền/center/phân công và hạn gốc; công tắc chỉ mở ghi ban đầu trước giờ học, correction và nhận xét sau buổi giữ cửa riêng.
- Giữ Invoice.amount/purchaseAmount/expiresAt cũ, Return chỉ đọc; bỏ đề xuất trả cả order trong đăng ký, dùng invoiceId/invoiceStatus để tìm đúng chứng từ.
- Giữ cutoff Normal T−48h, lý do khai giảng từ T−24h, hạn mua Advanced24h, Accept/hủy12h và45ngày sử dụng; hóa đơn30phút và QR300giây là hai hạn độc lập.
- Bỏ cách đổi bước TOTP thành300; lưu lượt cấp chưa dùng/hết hạn/đã dùng dưới DB lock để refresh không kéo hạn và hai scan chỉ một commit.
- Bỏ lệnh loại Sunday và test né Chủ nhật; thêm riêng ca hoàn SUB01-S01 có proposal Accepted/11credit, không backfill đồng ý cho buổi lịch sử.

## 3. API và DB liên quan

Contract chi tiết tại [docs01](docs/01-API-CONTRACT-FE-BE.md#action-metadata-20261119), schema máy đọc tại [OpenAPI](artifacts/fe-handoff/openapi.json).

| API/response | Thay đổi FE cần dùng |
|---|---|
| GET `/api/classes/{id}` và response ghi lớp | firstSessionAt, registrationDeadline/Allowed/BlockReason, openingConfirmationDueAt/Deadline/Allowed/BlockReason, openingReasonRequired, serverTime. List lớp chưa đánh giá các quyền này: null; FE đọc chi tiết trước thao tác |
| GET `/api/sessions`, `/{id}`, response ghi buổi và session lồng trong proposal | attendanceAllowed/attendanceBlockReason, cancellationAllowed/cancellationDeadline/cancellationBlockReason; giữ correction metadata, owner và creditReturned |
| GET `/api/advanced-registrations` và response request/decision | invoiceId/invoiceStatus nullable, purchaseConfirmationAllowed/BlockReason/serverTime. invoiceStatus lấy từ Invoice.paymentStatus; Member đọc chi tiết qua GET `/api/members/me/invoices/{id}` |
| POST `/api/advanced-registrations/{id}/order` | GET không tạo đơn; retry cùng method giữ ID/method/deadline, đổi method409 IDEMPOTENCY_CONFLICT. Đơn mới phải tạo trước acceptedAt+24h |
| POST `/api/training-results`, `/bulk`; POST `/api/attendances/scan` | Ghi ban đầu sớm khi cờ bật và đủ quyền học; bỏ/null content giữ nhận xét cũ. Bulk lỗi một item rollback toàn bộ. QR không tạo nhận xét giả hoặc trừ credit lần hai |
| POST `/api/sessions/{id}/cancel-private` | Present/Absent hiện hành:409 SESSION_ALREADY_ATTENDED, message “Buổi học đã được điểm danh, không thể hủy hoàn credit.” Hai mã400 bổ sung SESSION_COMPLETED/PRIVATE_SESSION_REQUIRED; hủy hợp lệ hoàn đúng1credit về quyền gốc, expiry không đổi |
| GET `/api/attendances/me/checkin-code` và alias | Giữ payload/expiresInSeconds, thêm expiresAt/serverTime; cấp mới300giây, refresh lượt còn hạn/chưa dùng giữ deadline, dùng rồi/hết hạn không replay |

Allowed:true đi cùng BlockReason:null; false đi cùng mã lý do. Null/chưa có field không là bằng chứng được phép. Metadata là kiểm tại lúc đọc, POST vẫn kiểm lại; dữ liệu đổi có thể trả lý do khác. Normal đầy vẫn có thể Waitlisted; staff đăng ký hộ phải kiểm Member được chọn khi POST. openingReasonRequired:true yêu cầu nhập lý do gửi POST, không tự làm Allowed:false ở GET. Gate manual/QR giữ theo từng method trong contract.

Field thời gian mới dùng datetime có offset+07:00; LocalDateTime cũ giữ định dạng giờ VN. InvoiceResponse thêm serverTime. Không tự cộng grace hoặc biến lỗi nghiệp vụ EXPIRED thành lỗi đăng nhập.

DB giữ dữ liệu cần [migration26](database/migrations/26_issued_attendance_codes_postgres.sql) sau baseline25 phù hợp; thêm bốn cột nullable/constraint trong members, không reset/backfill. Bundle dựng lại đã nhúng cấu trúc và fixture. Cách chọn migration hay dựng lại, phạm vi xóa/tạo và thao tác Owner tại [database/README](database/README.md) và [docs02](docs/02-DEPLOYMENT-GUIDE.md#demo-reset-deadlock).

## 4. FE cần làm

- Giữ các field mới trong mapper, cả false/null; đọc chi tiết lớp trước mở thao tác, dùng deadline/serverTime BE trả và hiển thị BlockReason. Đăng ký hộ gửi Member đã chọn; không coi lớp đầy là cấm Waitlisted.
- Dùng invoiceId/invoiceStatus để mở hóa đơn của Member; bỏ thông báo cứng5phút và grace tự cộng, giữ cùng đơn khi reload/retry. Return không là Paid; Paid cần đối soát chưa là quyền Active.
- Sửa phần QR trong `MemberDashboard.jsx` đang từ chối TTL>30 và `MemberAttendanceView.jsx` đang cắt30; refresh giữ expiresAt của lượt hiện tại. Kiểm lại `MemberCheckinView.jsx`, không viết lại phần đã đúng hoặc log mã/nonce/secret.
- FE-01: `attendanceApi.recordSessionAttendance` và bulk `CoachAttendanceView` bỏ content tự sinh, ghi chú dùng note. FE-02: dùng PATCH result có sẵn trong editor `CoachTrainingProgress` cho nhận xét sau buổi; không POST để upsert attendance giả.
- Giữ caller đề xuất→Member Accept/readback đang có; hủy tải lại đúng buổi/quyền và xử lý409. FE-03 Cash đã đổi caller, chỉ cần hồi quy. FE-04 Reception đi register thay caller createMember của Manager; Chủ nhật kiểm caller/helper0→7 đang có.

Các vị trí FE là phát hiện từ lượt đối chiếu trước, cần xác định lại source/website thực khi tích hợp. BE chưa sửa hoặc chạy test source FE trong đợt này. Cách bố trí màn hình do FE quyết định.

## 5. Việc còn lại

Owner copy nguyên [SQL19](database/bundles/2026-11-19/schema_postgres.sql) để dựng lại đúng19/11 giờ VN, sau khi dừng BE/job và client DB. File đã nhúng fixture/migrations; không chạy thêm fixture/seed. Kết quả `result=PASS` cùng summary chỉ xác nhận cài bộ demo; sau đó phải dùng BE tương ứng, đăng nhập mới và kiểm nghiệp vụ có phiên. Agent không thực thi SQL Supabase.

[SQL21](database/bundles/2026-11-21/schema_postgres.sql) chỉ chuẩn bị; giữ NOT_RUN cho SQL/HTTP đến đúng21/11. Khi đến ngày, kiểm local và xuất SYSTEM-DATA nguyên trạng rồi kiểm cả15:00 và18:00 trước khi giao Owner chạy. Không dùng PASS bộ19 thay cho bộ21, không bỏ guard ngày hoặc đổi Clock runtime.

Sau khi triển khai được giao riêng, Owner cấu hình demo cờ sớm true và hóa đơn30phút theo [docs02](docs/02-DEPLOYMENT-GUIDE.md); lượt này chưa đổi cloud. FE tích hợp rồi kiểm login/auth-me, lưu/đọc lại đúng actor/state/ID sau reset và rehearsal [bốn flow](docs/reference/note-script-demo.md) với [đầu vào theo bộ](docs/reference/SCMS-SCRIPT-DEMO-BE.md#demo-bundles-20261119). Provider VNPay/email thật, Render runtime và FE/browser vẫn NOT_RUN; chỉ đóng từng phạm vi khi có bằng chứng mới.
