# SCMS — Tiến độ và nghiệm thu

## Active

**Phạm vi đã giao:** P1.0–P1.4, BE P2 và tích hợp D01/D09 theo Owner28/10. P1.5/phần còn lại gói T/FE integration vẫn để lại. Không coi merge OTP/chat/notification hoặc D01/D09 là hoàn tất toàn bộ gói T.

| Việc còn mở | Trạng thái | Điều kiện tiếp tục |
|---|---|---|
| Kiểm API có phiên, owner/role/center trên Render | BLOCKED; chưa chạy nghiệp vụ | Thiếu tài khoản thử, fixture, phạm vi live và cách đọc state sau commit; kiểm `/auth/me` trước |
| Toàn bộ tổ hợp CRUD baseline × role/owner/center ngoài matrix workflow đích | NOT_RUN đầy đủ | Xác định contract/fixture từng nghiệp vụ; không suy PASS từ route/schema hoặc workflow liên quan |
| OTP/email cloud | BLOCKED ở cấu hình hiện tại | Owner xác nhận Render Free; chọn HTTPS email provider/key/sender và adapter, rồi kiểm delivery |
| D01 hồ sơ Member; D09 Coach directory trên Render | PASS local; chưa publish | Main local đã tích hợp và kiểm PG có phiên; chưa push tại điểm review đã chốt, không gọi đó là nghiệm thu Render |
| FE browser/CORS/cookie/camera QR | NOT_RUN | Nguồn FE hiện hành và môi trường tích hợp được xác định |
| VNPay gateway sandbox live | BLOCKED; chưa gọi gateway | Thiếu Merchant/callback/fixture và quyền kiểm phù hợp; callback ký local không thay gateway |
| SQLServer runtime | NOT_RUN | Môi trường kiểm riêng nếu được chọn |
| Schema demo/Coach theo Owner29/10 | PASS local; chưa chạy Supabase | Owner review/chọn schema root bảo toàn hoặc `database/` dựng lại; xem nghiệm thu bên dưới |

**Mức sẵn sàng FE:** contract/schema đủ để bàn giao phạm vi BE đã kiểm local có điều kiện; chưa ký full authenticated Render hoặc FE acceptance. Matrix và response/state chuẩn ở `authenticatedAcceptance` của [QA duy nhất](../artifacts/qa/latest.json).

**Bước kế tiếp:** review bản tích hợp local trước push main (có thể kích hoạt Render auto-deploy); sau khi remote main chứa D01/D09 mới xóa remote feature branch. Khi có tài khoản/fixture/phạm vi live xác định mới kiểm Render theo matrix, xác minh phiên và state; mở rộng kiểm CRUD baseline theo rủi ro/contract. Không lấy số route làm mục tiêu gọi mọi mutation production. P1.5/phần còn lại gói T/FE integration tiếp tục ngoài phạm vi.

Đọc: [AGENTS](../AGENTS.md) → [quyết định Dxx](04-DEBUG-NOTE.md) → [contract FE](01-API-CONTRACT-FE-BE.md) đúng luồng; [vận hành](02-DEPLOYMENT-GUIDE.md) khi cần.

## Nghiệm thu schema demo/Coach — 29/10/2026

**Source/môi trường:** working tree trên `b59cf51ca1972e17b5337e169ee93d398047ea93`; hash SQL/BE/test/runner trong `demoSchemaCoachReview` của [QA duy nhất](../artifacts/qa/latest.json). Maven chạy bản sao source hiện tại không.env; PostgreSQL17 riêng `127.0.0.1:55440`. Không gọi Supabase/Render/email/gateway.

| Điều kiện nghiệm thu | Kết quả local |
|---|---|
| Account demo dùng mật khẩu Owner đã chốt, không bị ép đổi mật khẩu | PASS cả33account của mỗi schema qua HTTP login →`/auth/me`→API đúng role→logout; kiểm role và trạng thái khóa sau commit |
| Root sửa bộ mật khẩu ngẫu nhiên v1 nhưng không reset mãi | PASS sửa đúng33email, bỏ first-login/khóa cũ; giữ account ngoài danh sách, ID/chứng từ/attendance; replay giữ mật khẩu/khóa/profile/giá/quyền đã sửa sau đó |
| `database/` dựng lại danh sách và liên kết | PASS ID số độc lập từ1, PK subtype giữ user ID;9môn/8phòng; Normal12buổi hợp lệ; Paid/Pending/credit/attendance và chống trùng tài nguyên; FK ngoài phạm vi/phòng trùng làm rollback |
| Coach không được tự gán chuyên môn | PASS tạo và hai đường đổi role; chuyên môn trống khi chưa chọn; chặn nhận đăng ký/tạo lịch dạy và không để lại dữ liệu sau commit; `specializationIds` chỉ cấp đúng môn đã chọn |
| Runner PostgreSQL và regression | PASS bootstrap minimal không có account/demo grants; migration/replay/snapshot/FK guard; workflow HTTP đúng response và SQL state sau commit |

**Kết quả:** clean331run =303pass/28skip/0failure/0error; test đích40/40. PostgreSQL:11nhóm SQL,2case HTTP demo (33account mỗi schema),20workflow HTTP đều PASS. Skip không là pass; hai case demo PG đã chạy riêng. Lần PG đầu vướng timezone JVM alias và fixture Clock27/10 so với wall-clock DB29/10; sửa setup bằng timezone chuẩn và bỏ riêng hai date guard trong DB minimal local, giữ guard role/phòng/sĩ số/FK và kiểm Clock ở service. Hai SQL bàn giao vẫn giữ date guard gốc; không đổi expected để xanh.

**Bước tiếp:** Owner review và chạy toàn bộ đúng một file SQL trên Supabase, rồi báo kết quả. Bản root giữ dữ liệu; bản `database/` thay dữ liệu SCMS bằng bộ tham chiếu đã duyệt. Nghiệm thu local này không nâng trạng thái cloud/FE/gateway ở Active.

## Nghiệm thu API có phiên — 28/10/2026

**Môi trường:** PostgreSQL17 riêng `127.0.0.1:55440`, DB mới/fixture mới; checkout không.env, Clock `Asia/Ho_Chi_Minh`. Source kiểm `cc7847f7238ce3084ee74be8373236d52ec55756` đã chứa code/test D01/D09 và các sửa review; main local chưa push. Hash test, actor/fixture/expected/API/response và SQL state sau commit nằm trong QA. Không thêm migration hoặc ghi cloud.

| Điều kiện nghiệm thu | Kết quả mới | Bằng chứng/giới hạn |
|---|---|---|
| D01: hồ sơ đúng người/7field, null/validation, race | PASS local | Body không chọn người/role/center; giữ đủ2PATCH field khác nhau dưới lock; User+Member rollback; ngày theo Clock; Inactive403; first-login đổi mật khẩu rồi đọc được |
| D09: directory thật OSIV=false | PASS local | Member/Manager/Admin scope đúng; Admin null center chạy PG; contact theo MANAGE_USERS; query count không tăng theo Coach; Inactive403; invalid Member thiếu center chỉ kiểm H2 vì PG guard cấm fixture |
| D02/D03/D10/D16: tiền và quyền | PASS các case local đã ghi | Pending0credit; Paid hợp lệ12; giữ giá cũ; Return không Paid; IPN sai/chậm/retry/race; Cash race; Refund/CashCorrection giữ chứng từ; admission dùng chung tải Coach đa môn |
| D04/D05/D07: lịch, owner, credit | PASS các case local đã ghi | Consent/Primary Coach; sai role/owner/center; biên12h; notice; idempotency conflict; book/cancel đồng thời và hai slot cùng balance |
| D06/D08/D12/D14: sự cố và hạn | PASS các case local đã ghi | Dời phòng/cấp cứu, thay Coach có consent/retry không đổi Primary Coach; union nghỉ, preview stale/blocked/xuyên center, restore3+1credit, end ca đúng deadline; Outdoor khóa ca3/4 |
| D13/D18: báo cáo | PASS các case local đã ghi | Kỳ thu/hoàn; người khác lượt; Coach có VIEW_REPORTS vẫn403; Admin thiếu quyền/Manager Inactive403; Manager center2 không đọc tiền center1 |
| D15/D17: Normal/attendance/QR | PASS các case local đã ghi | [start,end), TTL/replay; correction;13record/12buổi hợp lệ; hai Member tranh chỗ cuối chỉ1Registered/1Waitlisted |
| D11/D19: môi trường và bằng chứng | PASS local | Migration replay/snapshot/FK guard; `/auth/me` trước nhóm; response+DB read sau commit; không cloud config/seed/live mutation |

**Kết quả kiểm:** clean326run =299pass/27skip/0lỗi; PostgreSQL HTTP20/20 nhóm (19workflow+1contract); matrix52PASS/2BLOCKED/1NOT_RUN, không có FAIL trong lượt cuối. Contract offline kiểm response local với snapshot không lệch type/property cơ bản; chưa là validator đầy đủ format/nullable/range/error. Lỗi fixture join_date và Member thiếu center được ghi riêng, không tính là pass nghiệp vụ. Skip không là pass và nhánh chưa chạy không được nâng theo count.

**Review và merge D01/D09:** giữ nguyên `053a5f2`/`1dabce5` qua merge `dda99ff` trên nhánh tích hợp, sau kiểm đạt fast-forward main local. Đã tái hiện/sửa race PATCH làm mất field và phiên Inactive vẫn đọc directory/contact; bổ sung Active Member cho self-profile. Test gốc coi28/10 là tương lai bị sai theo ngày chạy: cố định Clock fixture, không đổi rule. Giữ7field và first-login policy; không sửa query Admin-null vì PG đã chạy đúng. Sửa matcher contract để ưu tiên `/me` trước `/{id}`, khai báo schema PATCH/unknown-key đúng runtime.

**Dọn branch:** đã xóa7task branch cũ ở local/remote, xóa local `lan-anh` (remote đã vắng) và `merge/advanced-phases`; giữ LA/tai. Remote `feat/member-profile-coach-directory` còn giữ vì origin/main chưa nhận tích hợp. Danh sách và bằng chứng review ở `integrationReview` trong QA; không xóa worktree/log/backup để làm sạch hình thức.

**Ví dụ local đúng:** giá catalog đổi3.000.000→3.100.000, đơn cũ vẫn3.000.000 và Paid/12credit; IPN đúng mốc5phút ghi Paid cần đối soát nhưng quyền Cancelled/0credit; hai hủy đồng thời trả200/400 và chỉ hoàn1credit. Fixture ngày tương lai dùng Clock test, không là giao dịch Render.

## Bằng chứng giữ lại — 27/10/2026

Đây là bằng chứng cũ, **không chạy lại trong đợt nghiệm thu có phiên này**. App source: `693f732`; Live đã quan sát: `4d372e5`. Không dùng bảng này thay kết quả mới phía trên hoặc suy SHA Live hiện tại.

| Phạm vi | Kết quả lưu | Giới hạn |
|---|---|---|
| BE regression | 305run:278pass/27skip/0lỗi | Skip có điều kiện, không phải pass |
| PostgreSQL HTTP | 10/10:9 workflow nghiệp vụ+1OpenAPI regression | Fixture và Clock local, không cloud mutation |
| Contract | 149 exchange/36 operations;114positive/201body checks không lệch cơ bản | Không phải validator đầy đủ nullable/format/required/range hoặc mọi nhánh lỗi |
| Source/OpenAPI | 185 operations/149paths/138schemas; route parser khớp | Có aliases; không chứng minh mọi handler/nghiệp vụ đã chạy |
| GET Render | 3public200,71protected401 | Chỉ public/login gate; authenticated business matrix chưa chạy |
| Health Render | db UP,mail DOWN | `/api/health` chỉ liveness/identity; aggregatehealth503 |
| Production upgrade | Bundle01e31ca6… đã commit,50 bảng/9 môn,giữ dữ liệu gốc7bảng | Quyền thực thi chỉ bundle cụ thể; không suy quyền seed/test giao dịch |

Bằng chứng khi cần: [QA duy nhất](../artifacts/qa/latest.json). Bộ FE chỉ gồm [contract](01-API-CONTRACT-FE-BE.md) và [OpenAPI](../artifacts/fe-handoff/openapi.json). Raw run cuối còn ở `artifacts/runs/phases/2026-10-27-verified` trên máy này, ignored và không mặc định có trên máy khác.

27skip của lần clean gồm19baseline live/data/PG,6communication và2clone opt-in. Communication12case và clone2case đã chạy riêng ở source trước; xem QA theo SHA, không gọi đó là lần chạy mới của source hiện tại.

## HISTORY

| Ngày | Nhóm hoàn tất | Dấu vết |
|---|---|---|
| 29/10/2026 | Chốt hai biến thể schema, sửa mật khẩu/first-login demo một lần; bỏ mặc định chuyên môn Coach và đổi fixture Yoga; kiểm schema/HTTP/JPA/clean regression, sửa runner minimal | Nghiệm thu schema demo/Coach và QA; chỉ local, chưa ghi Supabase hoặc deploy |
| 28/10/2026 | Review hai commit D01/D09, tái hiện lỗi Clock/race/Inactive, sửa và tích hợp main local; PG login/SQL state/N+1/contract; dọn branch merged và lan-anh | Source và kết quả mới phía trên/QA; chưa push main hoặc nghiệm thu Render; remote feature giữ tới khi publish |
| 28/10/2026 | Lập matrix API có phiên và SQL state sau commit; thêm role/center/biên/race HTTP PostgreSQL, collector QA; làm rõ requestId UUID | Local mới theo mục nghiệm thu; Render/gateway BLOCKED, nhánh HTTP còn thiếu ở Active; không sửa BE hoặc ghi cloud |
| 27/10/2026 | Chuẩn hóa bàn giao: tách trách nhiệm5docs, FE1contract+1 schema, gom QA; bỏ inventory/schema MD và trace trùng | Không sửa BE/DB/deploy; kết quả kiểm cũ giữ theo SHA |
| 27/10/2026 | Audit và sửa metadata Swagger:24status201/204,6 schema name riêng,LocalTime string; thêm readback/contract regression | App693f732/Live4d372e5; QA |
| 27/10/2026 | Nâng cấp đúng production188f6765… sau backup mã hóa/restore proof và phê duyệt riêng | Bundle production giữ nguyên hash; docs02/QA |
| 27/10/2026 | Hoàn tất hai phase BE; merge communication, sửa CI và runtime identity | Các lần test có phạm vi/SHA riêng trong QA và Git |

Chi tiết phân tích, log, snapshot before/after và checklist cũ nằm trong Git tại `a4f3cd0` (trước chuẩn hóa). Ví dụ `git show a4f3cd0:docs/00-MASTER-CHECKLIST.md`. Chỉ tra khi cần một sự kiện cụ thể; không đọc mặc định hoặc khôi phục nguyên trạng vào docs active.
