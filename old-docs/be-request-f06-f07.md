# [SCMS FE → BE] Đề nghị xác minh lỗi 500 của lịch học và dashboard trên Render

Trong lần kiểm tra chỉ đọc lúc **2026-10-23 07:26:30 UTC**, FE ghi nhận:

- `GET /api/sessions` trả **500** với Manager, Receptionist, Member và Coach.
- `GET /api/reports/dashboard` trả **500** với Manager. Trong cùng đợt kiểm tra, `GET /api/reports/members` và `GET /api/reports/revenue` trả **200**.

Khi đối chiếu repo [Aditerx/SWP391](https://github.com/Aditerx/SWP391) tại commit `86d00765a574e1193c6b0042176acf7b26ca14ce`, FE thấy [Session.java](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/src/main/java/com/sportscenter/session/Session.java) ánh xạ `memberPackage` vào cột `sessions.subscription_id`. Tuy nhiên, phần tạo bảng `sessions` trong [schema_postgres.sql](https://github.com/Aditerx/SWP391/blob/86d00765a574e1193c6b0042176acf7b26ca14ce/schema_postgres.sql) và bản trong `database/` không khai báo cột này; FE cũng chưa tìm thấy lệnh bổ sung cột trong các migration PostgreSQL của repo. Ứng dụng cấu hình `spring.jpa.hibernate.ddl-auto=none`.

Đây là **giả thuyết có thể giải thích hai lỗi 500**, vì cả danh sách session và dashboard đều đọc entity `Session`. FE chưa có log hoặc quyền xem cấu trúc DB Render nên chưa thể xác nhận nguyên nhân thực tế. Nhờ đội BE hỗ trợ:

1. Cung cấp **commit SHA thực tế đang chạy trên Render** và kiểm tra stack trace của hai endpoint quanh mốc thời gian trên. Vui lòng che thông tin nhạy cảm khi chia sẻ log.
2. Kiểm tra cấu trúc thực tế của bảng `sessions`, nhất là cột `subscription_id`, khóa ngoại tới `member_packages(subscription_id)` và index liên quan; xác nhận các migration đã áp dụng trên DB live.
3. Nếu xác nhận schema thiếu hoặc lệch, chuẩn bị **migration PostgreSQL mới** cho DB hiện hữu và cập nhật cả hai file schema dùng để dựng DB mới. Trước khi áp dụng, kiểm tra dữ liệu tham chiếu hiện có và định nghĩa constraint/index, kể cả trường hợp chúng đã tồn tại dưới tên khác. Không tự xóa hoặc thay đổi dữ liệu nghiệp vụ để migration chạy qua.
4. Chú ý thứ tự tạo bảng trong schema: `sessions` hiện được tạo trước `member_packages`. Khóa ngoại tới `member_packages` cần được tạo sau khi bảng đích đã tồn tại.
5. Kiểm thử trên PostgreSQL với cả **DB dựng mới** và **DB hiện hữu được nâng cấp**; chạy migration lần thứ hai để kiểm tra khả năng chạy lại. Sau đó khởi động ứng dụng với `ddl-auto=none`, đọc entity `Session` và gọi lại hai endpoint bằng tài khoản có quyền.

Sau khi sửa và deploy, nhờ BE gửi **SHA deploy, nguyên nhân xác nhận từ log, migration đã áp dụng và kết quả kiểm thử**. FE sẽ chạy lại probe chỉ đọc với file bằng chứng mới, kiểm tra HTTP status, dữ liệu trả về và phạm vi truy cập của từng vai trò trước khi cập nhật trạng thái F-06/F-07 và nghiệm thu các luồng lịch học, đặt lớp, dashboard.


---

## Phản hồi BE — đối chiếu ngày 23/10/2026

### Kết luận

| Nội dung FE nêu | Đối chiếu thực tế |
|---|---|
| Session.memberPackage ánh xạ sessions.subscription_id | Đúng ở workspace hiện tại. |
| Hai schema và migration PostgreSQL chưa thêm cột | Đúng trước bản sửa; đã bổ sung migration 07 và đồng bộ hai schema. |
| ddl-auto=none | Đúng; Hibernate không tự bổ sung cột. |
| Session list và dashboard cùng đọc entity Session | Đúng: findFiltered/findByEnrolledMember và ReportService.dashboard -> findBySessionDateBetween. |
| Thiếu cột gây lỗi truy vấn trên Render | Log bổ sung xác nhận lỗi thiếu subscription_id lúc 01:28:28 UTC. Chưa gắn được đoạn log này với hai request lúc 07:26:30 UTC; probe mới hiện trả 200. |
| HTTP 500 lúc 07:26:30 UTC | Là bằng chứng FE cung cấp (14:26:30 UTC+7). Probe mới bắt đầu 19:23:02 UTC+7 trên service được cung cấp không tái hiện 500. |

### 1. SHA deploy và nguyên nhân từ log

- HEAD workspace trước khi sửa: `8e957593b865b1172814c2622578c5f7f1e5656a`. Sửa đổi đợt này chưa commit/push/deploy. Đây không phải SHA Render đã được xác nhận.
- Commit `86d00765a574e1193c6b0042176acf7b26ca14ce` do FE dẫn chưa có object trong Git local; kết luận source áp dụng cho workspace hiện tại.
- Người dùng đã cung cấp URL `https://scms-app-p1gy.onrender.com` và log Render. Lỗi gốc trong log là `org.postgresql.util.PSQLException: ERROR: column s1_0.subscription_id does not exist` lúc `2026-10-23T01:28:28.707135909Z` (08:28:28 UTC+7).
- Đoạn trích là phần cuối stack trace, không có SQL đầy đủ, endpoint hoặc SHA; chưa xác nhận cùng lỗi ở hai request FE lúc 07:26:30 UTC (sau gần 6 giờ). Dòng service live chỉ chứng minh deploy sẵn sàng phục vụ, không chứng minh mọi truy vấn thành công.
- SHA Render vẫn cần lấy từ Deploys/build log; không suy SHA từ phiên bản thư viện hoặc HEAD local.

### 2. Cấu trúc DB và migration đã áp dụng

Đã kết nối JDBC tới PostgreSQL cấu hình trong .env, đặt transaction read-only, đọc catalog và `SELECT subscription_id FROM public.sessions LIMIT 0`, sau đó rollback. Không xuất host, username hoặc password.

- `sessions.subscription_id`: integer, nullable.
- `fk_session_subscription`: FOREIGN KEY (subscription_id) REFERENCES member_packages(subscription_id), đã validated.
- `idx_sessions_subscription`: btree trên public.sessions(subscription_id).
- Không tìm thấy bảng history theo tên migration/flyway/changelog trong schema public. Không thể suy danh sách migration đã chạy hoặc ai đã thêm cột/FK/index từ catalog hiện tại.
- Chưa áp dụng migration 07 hay đổi dữ liệu trên DB này. Catalog hiện tại không chứng minh cấu trúc tại thời điểm probe hoặc DB của service Render.

### 3–4. Bản sửa repo

- Thêm `database/migrations/07_session_subscription_postgres.sql`, sau 04/06, dành cho PostgreSQL schema public.
- Hai file schema PostgreSQL thêm cột nullable; FK/index nằm sau khi member_packages đã được tạo.
- Transaction có lock và timeout 5 giây. Kiểm kiểu/nullability, orphan, định nghĩa FK/index kể cả tên khác; dừng khi FK không tương thích hoặc tên đã dùng bởi định nghĩa khác. Không xóa/sửa dữ liệu nghiệp vụ để vượt guard.
- Giữ NULL cho liên kết lịch sử chưa biết, không tự gán gói hoặc hoàn credit sang gói khác. Giữ nguyên mapping/API và quyết định nghiệp vụ đang chờ trong docs.

### 5. Kiểm thử và thông tin bàn giao FE

- PostgreSQL 17.11 tách biệt: schema gốc dựng mới và bản database/schema_postgres.sql mô phỏng nâng cấp đều pass. Kiểm migration lặp, FK/index tên khác, bảo toàn dữ liệu/liên kết, rollback orphan và index sai định nghĩa.
- 7 test ứng dụng trên mỗi DB, tổng 14 lượt pass, 0 failure/error/skip. Hibernate ddl-auto=none đọc Session thật; MockMvc qua security dùng authorities từ DB: Session list trả 200 cho Manager/Receptionist/Member/Coach, dashboard Manager và reports đối chứng trả 200; request ẩn danh trả 401. Không phải HTTP probe qua Render.
- Hồi quy `.\mvnw.cmd clean test`: BUILD SUCCESS, 223 tests, 204 pass, 19 skipped, 0 failure/error (đã đối chiếu XML Surefire).
- Chi tiết và giới hạn nằm trong HISTORY của `docs/00-MASTER-CHECKLIST.md`; cách chạy lại và checklist deploy nằm tại mục 8 `docs/02-DEPLOYMENT-GUIDE.md`.

**Còn thiếu để đóng toàn bộ F-06/F-07:** SHA deploy thực tế, xác nhận DB Render và migration đã áp dụng, log đầy đủ gắn đúng request FE và nghiệm thu phạm vi dữ liệu/quyền. Đã có log xác nhận lỗi thiếu cột trong quá khứ và probe HTTP mới không tái hiện 500; chi tiết bên dưới.


### Bổ sung sau khi nhận log và URL Render — probe trực tiếp 23/10/2026

Service: https://scms-app-p1gy.onrender.com. Probe bắt đầu **12:23:02 UTC / 19:23:02 Asia/Ho_Chi_Minh**; kiểm phạm vi bắt đầu **12:23:54 UTC / 19:23:54 UTC+7**. Đăng nhập tài khoản demo đã được tài liệu hóa, chỉ GET dữ liệu và logout; không đặt/hủy ca, tạo đơn, chạy migration hoặc deploy. Các thao tác đăng nhập có thể cập nhật trạng thái phiên/bộ đếm đăng nhập theo cơ chế sẵn có.

| Request / vai trò | HTTP | Dữ liệu quan sát |
|---|---:|---|
| GET /api/health | 200 | Object có status, message |
| GET /api/sessions — Manager | 200 | Array 2 buổi, có các field SessionResponse |
| GET /api/sessions — Receptionist | 200 | Array 2 buổi |
| GET /api/sessions — Member mẫu | 200 | Array rỗng |
| GET /api/sessions — Coach mẫu | 200 | Array rỗng |
| GET /api/reports/dashboard — Manager | 200 | Đủ 8 field dashboard, gồm todaySessions, totalRevenue |
| GET /api/reports/members — Manager | 200 | Object báo cáo hội viên |
| GET /api/reports/revenue — Manager | 200 | Object báo cáo doanh thu |
| GET /api/sessions?memberId=-1 — Member | 403 | Từ chối ép bộ lọc khác owner |
| GET /api/sessions?coachId=-1 — Coach | 403 | Từ chối ép bộ lọc khác Coach |
| GET /api/reports/dashboard — Member | 403 | Từ chối |
| GET /api/reports/dashboard — Coach | 200 | Cần đối chiếu quyền live, giải thích bên dưới |
| GET /api/sessions và /api/reports/dashboard — chưa đăng nhập | 401 | Yêu cầu xác thực |

**Điểm quyền cần giữ riêng:** Login live của coach@scms.com trả role Coach và authorities `MANAGE_CLASSES`, `ROLE_COACH`. Annotation dashboard hiện cho MANAGE_CLASSES, nên 200 khớp với quyền đó; seed workspace không cấp MANAGE_CLASSES cho Coach. Không tự thu hồi quyền hoặc đổi annotation khi chưa chốt cấu hình quyền mong muốn. Không coi đây là nguyên nhân lỗi 500 hoặc khẳng định toàn bộ RBAC đã đạt.

**Kết luận cập nhật:** Lỗi truy vấn thiếu cột đã được xác nhận tại thời điểm trong log; các endpoint được yêu cầu hiện trả 200 trên Render. Chưa có bằng chứng ai đã sửa live, chạy SQL nào hay deploy SHA nào; bản migration 07 trong workspace chưa được áp dụng lên DB đang dùng bởi đợt làm việc này. Danh sách Member/Coach rỗng và hai probe bộ lọc không chứng minh đầy đủ mọi trường hợp owner/assignment/center, private booking hay luồng tạo lớp. FE có thể dùng kết quả mới để retest F-06/F-07, nhưng chưa đóng nghiệm thu toàn bộ luồng.


### Kiểm chứng sau khi người dùng chạy migration — 24/10/2026

Người dùng báo đã chạy migration trên Supabase thành công. Kiểm lại bắt đầu **02:12:49 ngày 24/10/2026 Asia/Ho_Chi_Minh** (19:12:49 UTC ngày 23/10); kiểm phạm vi bắt đầu 02:13:14 UTC+7. Không chạy lại migration hoặc thay đổi dữ liệu nghiệp vụ; chỉ JDBC read-only và HTTP login/GET/logout.

**DB cấu hình trong .env:** sessions.subscription_id là integer nullable; fk_session_subscription đúng bảng/cột và validated=true; idx_sessions_subscription là btree; SELECT cột thành công; số tham chiếu mồ côi **0**. Đây là xác minh cấu trúc hiện tại, chưa phải xác minh cấu hình DB đích của Render từ dashboard quản trị.

**Render https://scms-app-p1gy.onrender.com:**

| Kiểm tra sau migration | HTTP | Kết quả |
|---|---:|---|
| Health | 200 | Object hợp lệ |
| Session list — Manager / Receptionist | 200 / 200 | Mỗi tài khoản thấy 2 buổi |
| Session list — Member / Coach mẫu | 200 / 200 | Array rỗng |
| Dashboard — Manager | 200 | Đủ 8 field báo cáo |
| Members / revenue report — Manager | 200 / 200 | Object hợp lệ |
| Member ép memberId khác / Coach ép coachId khác | 403 / 403 | Từ chối |
| Dashboard — Member | 403 | Từ chối |
| Schedule / dashboard — chưa đăng nhập | 401 / 401 | Yêu cầu xác thực |
| Dashboard — Coach | 200 | Vẫn còn quyền live đã nêu ở mục trước |

**Kết luận:** Lỗi 500 của lịch học và dashboard nêu trong F-06/F-07 **không tái hiện ở các request kiểm tra sau migration**; cấu trúc DB .env phù hợp migration 07 và không có tham chiếu mồ côi. Không cần chạy thêm migration để chữa lỗi thiếu cột theo bằng chứng hiện tại. Vì probe ngày 23/10 cũng đã 200 trước lần chạy này, không quy toàn bộ việc phục hồi endpoint cho lần chạy migration vừa rồi.

Phần còn lại để nghiệm thu đầy đủ: SHA deploy, đối chiếu DB đích Render và lịch sử áp dụng, quyền Coach MANAGE_CLASSES, dữ liệu thực có lịch Member/Coach và toàn bộ luồng đặt lớp. Không sửa backend trong lần kiểm chứng này nên không chạy lại bộ unit test; kết quả test local trước đó được giữ nguyên.
