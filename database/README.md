# SCMS — Cấu trúc và vận hành database

PostgreSQL/Supabase là môi trường đang dùng. README hướng dẫn chọn script và đọc mô hình dữ liệu trong repo; không chứng nhận DB đang chạy đã đồng bộ với script.

**Lượt cấp mã điểm danh300giây:** [migration26](migrations/26_issued_attendance_codes_postgres.sql) bổ sung bốn cột nullable trong `members`: checkin_code_nonce, checkin_code_issued_at, checkin_code_expires_at, checkin_code_used_at. Guard kiểm schema, thời hạn đúng300giây và thời điểm dùng; replay giữ dữ liệu. Không backfill lượt cấp từ secret/bước mã cũ, không đổi chứng từ/credit/attendance. Bootstrap/bundle có cùng cấu trúc; export loại cả bốn field và secret khỏi SYSTEM-DATA. Metadata và invoiceId/invoiceStatus dùng dữ liệu hiện có, không thêm bảng. Nhánh giữ dữ liệu chạy migration26 sau baseline25 phù hợp; không dùng reset chỉ để thêm cột. Bằng chứng local ở docs00/QA, chưa chạy cloud.

**Bundle sửa deadlock:** bộ sinh lấy khóa reset bằng `pg_try_advisory_xact_lock`, rồi khóa các bảng SCMS bằng `ACCESS EXCLUSIVE NOWAIT` theo thứ tự cố định trước khi đọc dữ liệu. DB bận trả `SCMS_DEMO_DB_BUSY`, reset chồng trả `SCMS_DEMO_RESET_BUSY`; dừng trước khi xóa dữ liệu, không tự kill connection. Vẫn cần dừng BE/job và client DB trước khi Owner chạy nguyên file. Hai ngày cài19/11 và21/11 đã chốt; từng file chỉ chạy đúng ngày anchor VN, giữ riêng bằng chứng từng bộ. [Cách chạy/xử lý lỗi](../docs/02-DEPLOYMENT-GUIDE.md#demo-reset-deadlock), [trạng thái kiểm](../docs/00-MASTER-CHECKLIST.md#active).

**Bộ theo ngày:** [19/11](bundles/2026-11-19/schema_postgres.sql) là SQL dựng lại đã kiểm local, cùng fixture/SYSTEM-DATA/demo-plan và manifest trong thư mục đó. Bản canonical schema_postgres.sql/bootstrap/SYSTEM-DATA tại root khớp bộ19. [21/11](bundles/2026-11-21/schema_postgres.sql) là bản chuẩn bị để review: SQL execution NOT_RUN đến đúng21/11, SYSTEM-DATA mới là thiết kế dự kiến. Phải kiểm SQL local và xuất lại fixture nguyên trạng đúng ngày trước khi giao chạy; không suy PASS từ bản19. Kịch bản chung dẫn hai plan tại [SCRIPT](../docs/reference/SCMS-SCRIPT-DEMO-BE.md#demo-bundles-20261119).

Nguồn sinh giữ bảy ngày/ca làm của Coach; ngày nghỉ cá nhân, Holiday và Outdoor vẫn được kiểm. Riêng SUB01-S01 là ca demo mới Scheduled/chưa attendance, quyền Active11credit, liên kết đúng proposal Accepted để thử hoàn11→12. Không thêm consent cho các buổi lịch sử. Đây là fixture tổng hợp có nhãn DEMO, không khẳng định Member đã thao tác ngoài ứng dụng.

<a id="advanced-class-review"></a>
**Đính chính14/11 — lớp Advanced theo đăng ký:** Owner phát hiện9lớp Advanced mẫu `Open`, chưa có Member/Coach, không đúng bước đăng ký → Coach nhận. Kiểm trước đó chưa kiểm mô hình này. Bản sửa local bỏ các template và cách BE tìm lớp chung theo môn; lớp riêng liên kết qua `advanced_registrations.class_id` unique/FK. Owner đã báo SQL25 PASS và cleanup xóa9template; BE tương ứng đã phát hành. Bằng chứng tại Active/QA.

- **SQL giữ dữ liệu:** [migration25](migrations/25_advanced_registration_classes_postgres.sql) tạo lớp riêng cho đăng ký đã nhận và chuyển `sessions.class_id` theo đúng subscription; giữ PK/buổi/Member/Coach/trạng thái/chứng từ/credit. Dữ liệu thiếu liên kết, tài liệu class-scope không phân được hoặc Pending offer ghi class cũ sẽ dừng, không đoán. Sau đó [SQL dọn](cleanup_unused_advanced_templates_postgres.sql) chỉ xóa template không còn tham chiếu, kể cả FK có CASCADE và audit/thông báo. Trước nâng cấp,2lớp chung đang có buổi không được xóa. **Owner đã báo cả hai file chạy thành công; agent không thực thi SQL cloud.**
- **Dữ liệu sai khác đã sửa trong bundle:** thời điểm tạo/Coach nhận sau ngày thanh toán; Cash Advanced mẫu ngoài5phút; bản Accepted mẫu quá24giờ ngay ngày cài. Đã sửa timeline nguồn sinh; không sửa lại lịch sử live để làm kết quả đẹp. Ca cố ý thu Cash nhầm/tiền đến muộn và quyền0credit có buổi đang chờ vẫn giữ theo kịch bản, không tự xóa.
- **BE hiện hành:** Coach nhận tạo lớp riêng, Requested/Rejected không tạo lớp; retry không nhân. Đặt buổi lấy lớp đúng đăng ký/quyền học, chặn classId khác và tạo/sửa Advanced qua CRUD chung. Danh sách dùng Member/Coach/state từ liên kết; `enrolledCount=1` là chủ lớp đã được Coach nhận, không cấp quyền trước Paid. Member/Coach chỉ thấy lớp riêng thuộc mình; staff cùng center. FE dùng `advancedState`, không gán nhãn mở đăng ký nhóm.
- **Bundle hiện tại:** chỉ sinh8lớp Advanced của8đăng ký Accepted/Invoiced và12Normal; danh mục vẫn9môn. Số8là hệ quả fixture, không phải quota. Không seed consent giả cho buổi cũ; lịch cố định của lớp Advanced có thể null vì buổi được chốt riêng. FK/unique/CHECK/trigger bảo vệ liên kết và không cho đổi chủ lớp đã được nhận.
- **Chốt kiểm bộ sinh:** trước xuất SQL phải khớp hai chiều đăng ký ↔ lớp ↔ quyền học ↔ Member của buổi; Requested/Rejected không có lớp/quyền, Accepted chưa tạo quyền/buổi. SQL postflight và runner đối chiếu lại chủ lớp/môn/Primary Coach/trung tâm sau khi dựng local; sai thì rollback, không thay bundle bàn giao bằng bản chưa đạt. Chi tiết [rule bundle](../docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules).

Trạng thái/bằng chứng chuẩn ở [Active/QA](../docs/00-MASTER-CHECKLIST.md#active). Khi triển khai trên DB khác: dừng writer/job của BE, Owner chạy SQL giữ dữ liệu hoặc chọn bundle dựng lại, rồi phát hành BE tương ứng; không để BE cũ tiếp tục đặt buổi trong cửa chuyển đổi. Đợt phát hành này đã được Owner cho phép; không tự chạy thêm SQL/reset DB hoặc thay lịch sử.

**Bộ demo hiện hành:**47account/20Class (12Normal,8Advanced có đăng ký đã được nhận) và fixture nghiệp vụ tổng hợp/private PDF DEMO. SYSTEM-DATA xuất từ fixture local, không là reset Supabase/deploy. Trạng thái chuẩn tại [Active](../docs/00-MASTER-CHECKLIST.md#active); checkpoint Phase1 cũ ở [lưu trữ](../docs/archive/2026-11/03-TODO.md#normal-phase1-complete-20261110).

## 1. Context cho session debug hoặc thay đổi thiết kế

Đọc [AGENTS.md](../AGENTS.md) → Active của [docs00](../docs/00-MASTER-CHECKLIST.md) → bảng quyết định và đúng Dxx trong [docs04](../docs/04-DEBUG-NOTE.md). Sau đó mở phần schema, source BE và test liên quan.

| Khi cần | Nguồn đọc thêm |
|---|---|
| Field/state/error hoặc thay đổi API | Đúng luồng trong [docs01](../docs/01-API-CONTRACT-FE-BE.md) |
| Test, migration, deploy hoặc recovery | [docs02](../docs/02-DEPLOYMENT-GUIDE.md) |
| Cấu trúc/constraint/trigger và seed | [Schema dựng lại](schema_postgres.sql); [baseline lưu trữ](archive/schema_postgres_old.sql) chỉ để tham khảo khi chọn migration giữ dữ liệu |
| Kết quả kiểm thử đã ghi | Đúng mục trong [QA](../artifacts/qa/latest.json), theo môi trường/source của lần kiểm |

Quyết định, contract, code và DB thực tế có thể lệch nhau; phải xác định nguyên nhân trước khi sửa. Script trong repo không thay cho việc kiểm cấu trúc, migration marker và dữ liệu liên quan trên đúng DB đích. Không mặc định nạp `.env`, backup, log dài hoặc toàn bộ lịch sử.

## 2. Chọn đúng script

**Advanced14/11 — hai cách áp dụng:** [migration24](migrations/24_advanced_session_proposals_postgres.sql) thêm `private_session_proposals` cho DB cần giữ dữ liệu; không sửa hóa đơn/credit/buổi cũ. Hạn xác nhận24giờ dùng `advanced_registrations.accepted_at`, không cần backfill timestamp. Bundle dựng lại nhúng cùng migration sau các dependency; không chép DDL lần hai vào bootstrap. Owner đã chạy migration trên Supabase; kết quả thực thi/runtime ở Active/QA. Không cần reset DB đã nâng cấp chỉ để có bảng này.

<a id="advanced-bundle-proposal"></a>
**Bundle Advanced — Owner đã duyệt chuẩn bị/kiểm local:** compiler liệt kê `private_session_proposals` trong phạm vi xóa/tạo và nhúng migration24. Bảng này có FK tới `sessions`, `member_packages` và các danh mục; bundle cũ bỏ sót nó sẽ bị `DROP ... RESTRICT` chặn. Bundle mới xử lý cả bảng con lẫn các bảng được tham chiếu trong cùng transaction. Đây là công cụ dựng lại demo có đầy đủ thay đổi hiện hành, không phải khôi phục nguyên trạng dữ liệu phát sinh.

1. `scripts/build-demo-schema.py` quản lý một phạm vi reset/ACL/identity có bảng mới; DDL lấy từ migration24 sau các dependency.
2. `database/schema_postgres.sql` sinh qua staging, giữ guard ngày anchor, account/danh mục/fixture đã duyệt; bảng đề xuất khởi đầu rỗng, không bịa Accept cho buổi cũ.
3. `scripts/test-demo-schema-postgres.py` kiểm tạo mới/replay, FK/CHECK/unique/quyền bảng-sequence và reset khi có đề xuất/buổi; giữ object ngoài phạm vi và bộ đếm mã hóa đơn. `DemoRebuildPostgresTest` kiểm BE/schema, Normal/Cash và Advanced/Accept/hủy với đúng owner.

Phạm vi hiện tại chỉ chuẩn bị/kiểm local; chạy Supabase là bước Owner riêng. Bằng chứng bundle và nghiệm thu BE xem [Active/QA](../docs/00-MASTER-CHECKLIST.md#active).

Hai schema có mục đích khác nhau, không phải hai bản sao đồng bộ:

| Script | Mục đích và hành vi chạy lại |
|---|---|
| [schema_postgres_old.sql lưu trữ](archive/schema_postgres_old.sql) | Baseline cũ từ schema root đã chuyển vào database/archive, giữ nội dung để tham khảo; không đồng bộ với bundle demo10/11 và chưa gồm toàn bộ thay đổi16–22. Không dùng file này cho lượt làm sạch hiện tại. DB cần giữ dữ liệu phải chọn migration theo hiện trạng thay vì reset. |
| [database/schema_postgres.sql](schema_postgres.sql) | **File duy nhất cho lượt dựng lại demo.** Xóa/tạo lại56bảng SCMS, xóa thêm4bảng backup cũ. Tạo47account giữ33PDF,9môn/20sân/20Class (12Normal,8Advanced có đăng ký được nhận), chuyên môn/lịch Coach và lịch sử tổng hợp có nhãn DEMO; không seed OTP/QR secret/Pending hết hạn/provider attempts hoặc đồng ý lịch giả. ID tạo lại; PK subtype bằng `users.user_id`, UUID/khóa ghép giữ kiểu. Bộ đếm mã hóa đơn không giảm/tái dùng. |
| [bootstrap/scms_structure_postgres.sql](bootstrap/scms_structure_postgres.sql) | Cấu trúc phục vụ runner kiểm PostgreSQL riêng, không có account/giao dịch demo; không thay schema bàn giao Supabase. |
| [seed_holidays_catalog_2026.sql](seed_holidays_catalog_2026.sql) | Nguồn 11ngày lễ mẫu2026. Compiler bundle lấy danh mục này thành các INSERT tường minh, mặc định tắt. File vẫn dùng riêng để bổ sung ngày còn thiếu vào DB cần giữ, giữ flag/lịch sử cũ; không cần chạy thêm sau bundle mới. Đây là danh mục demo, không xác nhận TT từng đóng cửa. GET `/api/holidays` có cả dòng tắt; `/active` không có các dòng mới này. |

Sau khi Owner duyệt cloud, chạy **toàn bộ đúng một file `database/schema_postgres.sql`** cho lượt làm sạch hiện tại theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#demo-rebuild-20261109). Đã gồm cấu trúc Google, attendance17, audit18, activity19, pricing20 và guard mô hình đã retire; không chạy thêm18/19/20 hoặc chạy lại cleanup SQL16. “Chạy lại được” nghĩa là reset lại thành công, **không giữ dữ liệu demo phát sinh**. Generator [build-demo-schema.py](../scripts/build-demo-schema.py) ghép từ nguồn hiện hành và chỉ trích guard runtime của16, không chạy DML dọn dữ liệu16.

Bundle nhúng [21 lifecycle](migrations/21_normal_lifecycle_postgres.sql) và [22 consent/Refund](migrations/22_normal_changes_refunds_postgres.sql).21 giữ metadata legacy NULL, không bịa khai giảng.22 thêm8bảng normal_proposals/normal_responses/normal_transfers/normal_refunds/normal_refund_receivers/normal_refund_attempts/normal_refund_documents/normal_refund_verifications; tái dùng payment_resolutions cho Refund/report. Invoice.class_id/amount giữ gốc, entitlement_class_id là quyền đích. Snapshot/version/fingerprint giữ consent; receiver/verification giữ lịch sử; unique chặn hai claim active. Upload chưa cấp quyền Completed.

[Compiler fixture](../scripts/build-demo-fixture.py) dùng nguồn build-system-data, bắt buộc anchor VN, tạo2PDF DEMO invariant vào BYTEA riêng tư. Seed tắt đúng hai trigger thời gian classes/sessions trong transaction để nạp lịch sử tổng hợp rồi bật lại; FK/CHECK/unique giữ nguyên. Preflight SQL từ chối sai ngày trước CREATE/DROP; audit ghi anchor_date/business_timezone/fixture_version, migration_key giữ phiên bản mô hình. [Công cụ staging và rule anchor](../docs/02-DEPLOYMENT-GUIDE.md#demo-anchor-rules) sinh bundle/plan và chỉ tùy chọn kiểm DB local mới, không chạy cloud. SYSTEM-DATA xuất từ DB local nguyên trạng cùng anchor/version bằng `build-system-data.py --anchor <ngay-VN> --from-local-pg <DB-fixture-nguyen-trang>`; có `--output` để xuất staging, không xuất password/secret/binary. Kịch bản thao tác ở [SCMS-SCRIPT-DEMO-BE](../docs/reference/SCMS-SCRIPT-DEMO-BE.md); chuẩn bị FE là gợi ý để FE duyệt.

<a id="member-creator-fk"></a>
**Người tạo Member14/11:** `members.registered_by` tham chiếu `users(user_id)` qua `fk_member_registered_by`, phù hợp entity User và luồng Manager/Receptionist tạo hồ sơ. BE lấy người tạo từ phiên; FK không thay kiểm authority/center. Bootstrap và bundle dựng mới dùng quan hệ này. DB đang giữ dữ liệu dùng riêng [migration23](migrations/23_member_registered_by_postgres.sql), không chạy bundle reset: chỉ thay FK cũ trỏ receptionists, giữ toàn bộ dòng/NULL/sequence, kiểm cấu trúc và orphan trước khi đổi; replay trên mô hình đúng không đổi dữ liệu. [Cách áp dụng/kiểm](../docs/02-DEPLOYMENT-GUIDE.md#member-creator-fk-20261114).

<a id="training-result-state"></a>
**TrainingResult và content sau buổi — lưu ý bảo trì11/11:** `training_results.attendance_status` hiện NOT NULL/default Present và entity cũng bắt buộc state; `content` là nhận xét, không phải bằng chứng attendance. R2 chỉ sửa content của result đã tồn tại, giữ status/attendance gốc; không tự tạo result thiếu với Present hoặc nới nullable để vượt ràng buộc. Rule nghiệp vụ xem [D15](../docs/04-DEBUG-NOTE.md#d15), phạm vi triển khai ở [kế hoạch ba session](../docs/archive/2026-11/03-TODO.md#p22-p23-three-sessions-20261111).

Compiler fixture luôn truyền `attendance_status` tường minh khi INSERT, không dùng default DB làm expected nghiệp vụ. Validator của `build-system-data.py` chặn thiếu/null/state ngoài CHECK trước khi sinh SQL; kiểm offline nằm trong `test-demo-anchor.py`. State Late/Excused vẫn được giữ cho lịch sử hợp lệ theo schema/D15, không có nghĩa API mới được tạo state đó. Nếu sau này có yêu cầu tạo result chưa có attendance, phải đối chiếu lại model/DTO/DB và duyệt phạm vi riêng, không sửa SQL đã sinh bằng tay hoặc đổi mỗi default trong bundle. Bản sửa kiểm đầu vào không đổi schema, fixture đã duyệt hoặc yêu cầu reset cloud.

**DB cần giữ dữ liệu:** migration16 là cleanup có guard riêng; migration17–22 là các bước nâng cấp riêng. Chỉ chạy bước còn thiếu sau đối chiếu baseline; hướng dẫn cũ vẫn giữ trong docs02 để phục vụ nhánh nâng cấp, không phải thứ tự phải chạy sau bản dựng lại09/11.

SYSTEM-DATA là sổ tra cứu dữ liệu mẫu để nối mã Nxx/Mxx/RFxx trong SCRIPT với ID, lịch và trạng thái của fixture local. Bảng tra cứu sinh cùng các dòng chi tiết, không chép ID theo số thứ tự; giờ demo là đầu vào tùy chọn của plan, không làm đổi SQL/fixture. Luồng bàn giao hiện hành: hoàn thiện/chốt SCRIPT và SYSTEM-DATA trước, rồi mới review P2.2/P2.3 kết hợp hai file DA_CHOT và Dxx.

**Bundle chuẩn bị14/11:** đã có 11ngày lễ mẫu2026 đang tắt và một ngày đóng cửa DEMO tại anchor+50ngày. Nguồn tên Advanced bỏ `(1:1)`; không đổi số chỗ/giá/quyền. Compiler chặn catalog sai định dạng; postflight kiểm ngày/flag, runner kiểm cả nội dung đã lưu và lịch không rơi vào ngày nghỉ đang bật. Owner đã chạy bundle; readback DB và smoke đọc sau reset đạt trong phạm vi ghi tại Active/QA. Không chạy thêm seed lễ sau bundle mới. Kết quả/hash/ngày cài chuẩn ở [Active/QA](../docs/00-MASTER-CHECKLIST.md#active); gate đọc sau reset theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#session123-release-smoke-20261113).

`members.registered_by` tham chiếu `users(user_id)`, khớp `Member.registeredBy: User` và việc Manager/Receptionist tạo Member. FK cũ trỏ subtype Receptionist làm Manager tạo Member bị từ chối. Bản dựng lại sửa ngay cấu trúc nguồn; DB đang giữ dữ liệu cần một migration riêng có guard, không dùng reset chỉ để sửa FK. Runner kiểm hai loại người tạo sau commit và chặn User không tồn tại; đây là kiểm DB local, không là nghiệm thu HTTP/email/browser.

**Sinh bundle khác với chạy bundle:** generator ghép SQL để tạo file; chạy toàn bộ file SQL đã kiểm sẽ xóa/tạo lại dữ liệu demo trong DB đích. SYSTEM-DATA giữ dữ liệu nghiệp vụ tham chiếu, không cam kết mã hóa đơn/thời điểm tạo/audit giống từng byte ở mỗi lần chạy: invoice_code_seq phải tiếp tục tăng, timestamp runtime do lần thực thi ghi. Sau reset lấy lại ID và phiên từ API; không dùng lại cookie hoặc liên kết thanh toán cũ.

**Dependency Google trước deploy:** BE ghép Google cần cột `users.google_sub` kể cả khi chưa bật đăng nhập Google. Bundle dựng lại hiện hành đã chứa cấu trúc này; DB cũ cần xác minh riêng [13_google_login_postgres.sql](migrations/13_google_login_postgres.sql) theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#google-login). Prefix13 không chứng minh các migration room/capacity/Google thay thế nhau. SQL16 đã PASS trên Supabase không chứng minh Google đã chạy; không chạy lại SQL16 hoặc schema reset để bổ sung cột này.

Ngoại lệ có phê duyệt riêng: `13_owner_class_capacity_20261102_postgres.sql` chỉ dành7lớp/ID center1 Owner vừa xác nhận, không nhúng vào schema gộp hoặc empty-schema release. Bản tạm `0211-1.sql` đã gộp bước này trước13_room_catalog, cùng transaction; `0212-2.sql` là14 và kiểm đủ marker. Không chạy thêm file nguồn khi dùng cặp root. Giữ nguồn Owner-specific này sau khi xóa bản tạm, không áp dụng nhầm cho DB khác chỉ vì trùng số ID.

Mỗi file dùng transaction. Bản dựng lại chỉ drop danh sách bảng SCMS đã nêu, không drop bảng Supabase `auth`/`storage`/`realtime` hoặc bảng ứng dụng ngoài danh sách. Dependency FK/view từ ngoài phạm vi khiến thao tác dừng và rollback thay vì cascade. Sau lỗi, xử lý nguyên nhân trước khi chạy lại; không thêm `CASCADE` để vượt guard.

Owner09/11 yêu cầu rà toàn DB demo và chuẩn bị script dựng lại sạch, thay xử lý riêng buổi164. Lượt audit ban đầu chỉ đọc; backup trong phạm vi schema `public` được mã hóa riêng và kiểm trên clone local, không đưa dump/credential vào Git. Owner đã xác nhận lần reset trước; bundle có guard/metadata anchor mới chưa được thực thi cloud trong lượt chuẩn bị. Tình trạng từng bản/lần chạy giữ tại Active/QA. Quyền đọc hoặc chuẩn bị không cho phép mutation cloud ngoài phạm vi đã giao.

**Attendance09/11, nhánh giữ dữ liệu:** BE mới cần chạy riêng [17_attendance_review_postgres.sql](migrations/17_attendance_review_postgres.sql) sau baseline attendance14 (cột note TEXT đã có). Bản reset09/11 đã nhúng17. Script thêm `sessions.attendance_review_pending` mặc định false và index; không sửa state/note/chứng từ, không đưa buổi đã hoàn tất cũ vào hàng chờ thông báo. Finalizer mới bật marker tại end; review sau cutoff tắt marker cùng transaction. Replay giữ marker đang pending. Không chạy lại schema dựng lại hoặc SQL16 chỉ để thêm cột; vận hành theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#attendance-review-20261109).

Muốn dùng lại demo nhanh, xem [mốc snapshot/restore và prompt](../docs/02-DEPLOYMENT-GUIDE.md#demo-snapshot-recovery). Không phải sinh/reset mỗi ngày để đọc FE; snapshot giữ nguyên dữ liệu/thời điểm, không tự làm mới fixture. Quy trình mới là hướng dẫn chuẩn bị, chưa phải một archive hiện tại đã restore thử hoặc quyền restore Supabase.

## 3. Các bảng dễ nhầm và dữ liệu tham chiếu

| Bảng | Ý nghĩa hiện hành |
|---|---|
| `subjects` | 9 môn chuẩn và 18 mức giá theo D02. Danh mục room02/11 có20tài nguyên; Bóng rổ/Bóng chuyền tách sân. Không thêm subject_id vào rooms. |
| `classes` | Normal do Manager tạo/công bố cùng12buổi. Advanced được tạo riêng khi Coach nhận đăng ký; liên kết unique qua `advanced_registrations.class_id`, có đúng Member/Coach, không là template theo môn. Bundle có12Normal/8Advanced theo dữ liệu đã nhận; không hardcode ID vào FE. |
| `sessions` | Buổi cụ thể liên kết `classes.class_id`. Normal có12buổi hợp lệ; Advanced dùng đúng lớp của registration/subscription, lịch linh hoạt và chỉ tạo buổi/trừ credit sau Member Accept. `total_sessions=12` không tự tạo12buổi. |
| `class_enrollments` | Ghi danh Normal tổng hợp gắn hóa đơn Paid, gồm Registered/Completed/Cancelled theo kịch bản. Pending/waitlist tạo qua API đúng thời điểm demo. |
| `membership_packages` | Danh mục gói chung đã ngừng sử dụng; sau SQL16 bảng trống và cấm ghi lại, giữ cấu trúc cho ORM. Bản dựng lại không seed gói tháng/VIP; 18 giá môn hiện hành nằm ở `subjects`. |
| `member_packages` | Quyền Advanced tổng hợp: môn, Primary Coach, giá mua đóng băng, credit và hạn; có mẫu Active/Completed/Expired/Cancelled trong SYSTEM-DATA. `package_id` có thể NULL vì đăng ký gắn trực tiếp với môn/Coach. Default credit0; Paid hợp lệ mới cấp12credit qua workflow D03. |
| `vnpay_payment_attempts` | Metadata từng link pay: `attempt_id` bigint, FK `invoice_id` RESTRICT, `tmn_code`, `txn_ref`, `transaction_date` (đúng vnp_CreateDate gốc), `amount_minor` (VND×100), `created_at`, `last_query_at`. Unique invoice/merchant/ref/date. BE nội bộ; FE không đọc/ghi bảng. Không lưu secret/fullURL hoặc tự backfill invoice cũ. RLS không có policy cho anon/authenticated. |
| `users` | 47account demo: giữ33danh tính theo `System Account.pdf` và thêm14account theo thiết kế đã duyệt; trạng thái từng dòng xem SYSTEM-DATA. Người dùng thường center1, Admin center NULL. `_scms_accounts` là bảng tạm tự xóa khi commit, không có bảng account thường trực mới. |

**Không khôi phục mô hình gói hội viên chung:** thiết kế hiện hành không bán gói tháng/VIP làm vé vào khóa. Lỗi đã mắc: guard yêu cầu gói Active khiến tài khoản mới không đăng ký Normal được; đã bỏ guard Normal và chặn nhánh giá0 cấp Registered ở BE local. Quyết định tại [D03/D15](../docs/04-DEBUG-NOTE.md), kết quả tại docs00/QA. Tài liệu/seed cũ chỉ là lịch sử, không dùng để tái tạo requirement này.

**Dọn dữ liệu mô hình cũ theo Owner06/11:** file16 tự kiểm/phân loại trong transaction. Chỉ xóa subscription có package_id nhưng cả subject_id/primary_coach_id đều NULL, cùng hóa đơn gói chung cũ và payment attempt/resolution trực tiếp của hóa đơn đó khi không lẫn owner/khóa hiện hành. Gặp link sang session, enrollment, advanced-registration, pause hoặc hình dạng thiếu một phần thông tin thì dừng toàn bộ. Quyền Advanced đã có môn/Coach chỉ được tháo package_id cũ; giá, credit, trạng thái và hóa đơn hiện hành không đổi. Postflight kiểm dữ liệu được bảo vệ; lỗi thì rollback cả phần xóa. Sau khi dọn, trigger chặn tạo lại catalog/subscription/invoice kiểu cũ; không giữ bản sao dòng dữ liệu nghiệp vụ cũ, chỉ lưu số lượng ở marker migration. Bảng danh mục để trống nhằm không phá ORM hiện có, không giữ sản phẩm cũ hoạt động. Không xóa cả member_packages. Script đã kiểm PostgreSQL local; Owner gửi kết quả Supabase PASS, model_retired=true và mọi bộ đếm xóa/tháo liên kết0. Agent không trực tiếp truy vấn cloud; trạng thái BE/A và bằng chứng chuẩn xem docs00/QA.

`classes.total_sessions=12` là cấu hình, không bắt buộc số dòng `sessions` bằng12 sau các nghiệp vụ hủy/bù. Đếm buổi hợp lệ theo D15, không ép mọi truy vấn trả đúng12dòng.

Bản dựng lại hiện hành có lịch sử giao dịch/buổi học/attendance/Refund tổng hợp mang nhãn DEMO theo bộ được duyệt10/11; bộ rỗng09/11 và lịch sử schema29/10 chỉ còn là checkpoint trước. Ngày lịch/fixture được sinh theo anchor VN đã kiểm, không đổi Clock môi trường để hợp thức hóa. Bốn bảng backup cũ trong phạm vi xóa: `attendances_backup_flow4`, `class_enrollments_backup_flow4`, `role_permissions_backup_n06`, `training_results_backup_flow4`.

`attendances.absence_notice_id` liên kết nguồn Member báo vắng; `superseded_at` giữ bản tự động hết hiệu lực do đổi giờ. Unique index chỉ áp dụng bản hiện hành `(session_id,member_id) WHERE superseded_at IS NULL`; không xóa attendance cũ hoặc backfill notice quá khứ. Luồng và quyền theo D05/D15 và docs01.

Nguồn: account theo danh sách Owner đã duyệt; danh mục/quy tắc theo D02/D03/D05/D12/D15 trong [docs04](../docs/04-DEBUG-NOTE.md). Giữ bảng legacy rỗng cần cho ORM và bảng tương lai để không phá BE; giữ bảng pay attempt không đồng nghĩa bật QueryDR. Role/permission dùng baseline hiện hành, chưa mở policy mục5.

SCMS sử dụng BE/JDBC, nên schema thu hồi quyền bảng/sequence SCMS của `PUBLIC`, `anon`, `authenticated`. Owner vẫn sở hữu bảng mới; grant service từ default privileges không bị thu hồi. Grant tùy chỉnh trên bảng cũ không tự được sao chép khi DROP/tạo lại, nên datasource dùng role riêng cần cấp lại đúng quyền. Đây là kiểm catalog privilege, không khẳng định đã khai thác được Data API. Không thay quyền bảng ngoài danh sách. `notifications.related_id` dùng BIGINT đúng entity; unique email không phân biệt hoa thường. Các guard giá dương,12buổi cấu hình và7khung giờ chuẩn theo rule hiện hành, không tạo policy quyền mới.

Mật khẩu demo đã được Owner chốt, lưu salted BCrypt tương thích login BE; chỉ bộ account demo bỏ yêu cầu đổi mật khẩu lần đầu. Luồng tạo/cấp lại mật khẩu qua BE vẫn giữ first-login policy. Kết quả cuối script không xuất mật khẩu/hash. Coach mới chưa chọn `specializationIds` để chuyên môn trống và chưa đủ điều kiện nhận đăng ký/dạy môn đó.

<a id="phase1-20261109"></a>
### Bổ sung Phase1 09/11 — cấu trúc và migration riêng

- [18 audit scope](migrations/18_audit_scope_postgres.sql): `system_logs.audit_category,center_id,scope_source` lưu provenance sự kiện mới; legacy giữ NULL, không suy từ center hiện tại của actor. API chỉ trả loại/phạm vi có chứng cứ theo RP-43; lịch sử chưa rõ vẫn giữ DB.
- [19 room activity](migrations/19_room_activity_postgres.sql): thêm `rooms.activity_type` là loại sân/phòng phục vụ một môn, không thêm subject_id hoặc quan hệ đa môn. Runtime so loại với slug môn chuẩn; tên/mã hiển thị không tham gia quyết định. Map20room lần đầu lấy room_id từ snapshot đã duyệt của migration13, nên đổi tên/mã hiện tại không làm đổi liên kết. Guard rollback khi thiếu nguồn hoặc lịch tương lai sai môn; không sửa lịch cũ. Room chưa rõ loại giữ NULL, chặn xếp lịch mới cho đến khi Admin/Manager cấu hình theo quyền hiện hành.
- [20 publication price](migrations/20_class_publication_price_postgres.sql): `classes.pricing_draft` mặc định false bảo toàn giá lớp legacy; BE chỉ bật cho draft Normal mới, tắt khi công bố. Không đổi Invoice.amount/purchaseAmount, không tạo bằng chứng công bố giả cho lớp cũ. Giá Normal theo Class đã công bố, Advanced order theo Subject như D02/D03/RP-13.

Ba script có guard kiểu cột/replay; đã nhúng vào bản dựng lại09/11, chưa nhúng vào schema root bảo toàn. DB cần giữ dữ liệu chọn migration riêng theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#phase1-migrations-20261109). Không dùng reset chỉ để thêm cột vào dữ liệu cần giữ; không chạy migration nối tiếp bản reset mới.

<a id="room-catalog"></a>
### Danh mục room — Owner02/11

`room_id` vẫn SERIAL/INT và được sessions tham chiếu; `room_code` VARCHAR(40), NOT NULL, duy nhất, chữ hoa và tiền tố đúng center. Tất cả20dòng dưới thuộc center1, khởi tạo Available. ID fixture không là ID production; loại/sức chứa lấy từ room.jpg, không suy từ mã.

| room_code | Tên sân/phòng | Khu vực | Loại | Sức chứa |
|---|---|---|---|---:|
| C01-TEN-01 | Sân Tennis 01 | Khu A – Ngoài trời | Outdoor | 4 |
| C01-TEN-02 | Sân Tennis 02 | Khu A – Ngoài trời | Outdoor | 4 |
| C01-FOOT-01 | Sân Bóng đá 01 | Khu B – Ngoài trời | Outdoor | 15 |
| C01-FOOT-02 | Sân Bóng đá 02 | Khu B – Ngoài trời | Outdoor | 15 |
| C01-ARC-01 | Khu Bắn cung 01 | Khu C – Ngoài trời | Outdoor | 5 |
| C01-ARC-02 | Khu Bắn cung 02 | Khu C – Ngoài trời | Outdoor | 5 |
| C01-BR-01 | Sân Bóng rổ 01 | Nhà thi đấu A | Indoor | 8 |
| C01-BR-02 | Sân Bóng rổ 02 | Nhà thi đấu A | Indoor | 8 |
| C01-BC-01 | Sân Bóng chuyền 01 | Nhà thi đấu A | Indoor | 8 |
| C01-BC-02 | Sân Bóng chuyền 02 | Nhà thi đấu A | Indoor | 8 |
| C01-PICK-01 | Sân Pickleball 01 | Nhà thi đấu B | Indoor | 4 |
| C01-PICK-02 | Sân Pickleball 02 | Nhà thi đấu B | Indoor | 4 |
| C01-BAD-01 | Sân Cầu lông 01 | Nhà thi đấu B | Indoor | 4 |
| C01-BAD-02 | Sân Cầu lông 02 | Nhà thi đấu B | Indoor | 4 |
| C01-TT-01 | Bàn Bóng bàn 01 | Phòng Bóng bàn | Indoor | 2 |
| C01-TT-02 | Bàn Bóng bàn 02 | Phòng Bóng bàn | Indoor | 2 |
| C01-TT-03 | Bàn Bóng bàn 03 | Phòng Bóng bàn | Indoor | 2 |
| C01-TT-04 | Bàn Bóng bàn 04 | Phòng Bóng bàn | Indoor | 2 |
| C01-SWIM-01 | Hồ bơi 01 | Khu hồ bơi | Outdoor | 10 |
| C01-SWIM-02 | Hồ bơi 02 | Khu hồ bơi | Outdoor | 10 |

Migration13 giữ nguyên ID và dữ liệu phòng cũ bằng mã `C01-LEGACY-<id>`; lịch đã diễn ra giữ liên kết cũ. Nhận diện cả các alias cũ lẫn tên trong20phòng đã duyệt; không coi phòng đã có tên Sân Tennis02 là phòng lạ. Lịch Scheduled tương lai giữ đúng số sân nếu tên đã khớp danh mục và đúng môn (Tennis02→C01-TEN-02); các alias còn lại dùng sân01 đúng môn, sân đa năng tách Bóng chuyền→BC01/Bóng rổ→BR01. Sau chuyển hợp lệ mới đóng phòng cũ. Snapshot trước/sau và marker `20261102_room_catalog_v1` nằm trong scms_data_migration_audit; replay giữ chỉnh sửa có chủ đích về sau. Phòng lạ vẫn bị chặn và lỗi có DETAIL liệt kê room_id/tên/trạng thái. Tên/mã xung đột, ca đang diễn ra, lớp vượt sức chứa hoặc sân đích trùng lịch làm toàn transaction rollback. Không tự giảm max_capacity, xóa ghi danh, sửa attendance hoặc đổi chứng từ để vượt guard. Quy trình preflight/deploy ở docs02.

## 4. Script migration và tài liệu lịch sử

`migrations/` giữ các bước nâng cấp riêng cho DB cũ. Không chạy cả thư mục theo tên một cách máy móc: cần đối chiếu trạng thái DB, dependency và nội dung từng file.

| File PostgreSQL | Phạm vi |
|---|---|
| `01_migration_dynamic_permissions.sql` | Quyền động/RBAC |
| `02_migration_class_feedback.sql` | Phản hồi lớp |
| `03_migration_payment.sql` | Thanh toán |
| [15_QueryDR.sql](migrations/15_QueryDR.sql) | Bổ sung bảng pay attempt cho QueryDR; chạy nguyên một file trên DB hiện có, không sửa/xóa invoice hoặc tự backfill metadata |
| `04_migration_member_flow_v2.sql` | Luồng Member cũ; default/seed lịch sử không thay quyết định hiện hành |
| `05_migration_notifications.sql` | Thông báo |
| `06_migration_chat.sql` | Chat, quyền `CHAT_AS_MEMBER`/`HANDLE_CHAT` |
| `06_schedule_safety_postgres.sql` | Guard/an toàn lịch; độc lập với chat cùng prefix06 |
| `07_session_subscription_postgres.sql` | Liên kết Session–subscription |
| `08_advanced_entitlements_postgres.sql` | Quyền Advanced theo môn/Primary Coach, giá mua và hạn |
| `09_approved_catalog_postgres.sql` | Danh mục đã chốt |
| `10_session_incidents_postgres.sql` | Sự cố/bù, thay đổi buổi và workflow liên quan |
| `11_holiday_history_postgres.sql` | Lịch sử đóng cửa/ngày nghỉ |
| `12_payment_resolutions_postgres.sql` | Hồ sơ Refund/CashCorrection |
| `13_room_catalog_postgres.sql` | room_code;20tài nguyên center1; chuyển lịch tương lai có guard, giữ phòng/lịch sử cũ và marker replay |
| `13_owner_class_capacity_20261102_postgres.sql` | Phê duyệt riêng7lớp Normal; kiểm ID/môn/center/old→new/held seats, audit một lần; chạy trước13_room_catalog trong bundle Owner |
| [13_google_login_postgres.sql](migrations/13_google_login_postgres.sql) | Thêm google_sub/index cho DB cũ trước BE ghép Google; độc lập với room/capacity cùng prefix13; không seed/reset |
| `14_absence_attendance_postgres.sql` | Nguồn notice, attendance superseded và unique cho bản hiện hành; không backfill lịch sử |
| [16_retire_legacy_memberships_postgres.sql](migrations/16_retire_legacy_memberships_postgres.sql) | Loại mô hình gói chung theo guard/phân loại; Owner đã xác nhận Supabase PASS, không yêu cầu chạy lại |
| [17_attendance_review_postgres.sql](migrations/17_attendance_review_postgres.sql) | Hàng chờ đối soát attendance sau cutoff, không backfill lịch sử; cần trước BE attendance09/11 |
| [21 Normal lifecycle](migrations/21_normal_lifecycle_postgres.sql) | Metadata minimum/confirmation/postponement/version; giữ legacy NULL, guard/replay |
| [22 Normal changes/Refund](migrations/22_normal_changes_refunds_postgres.sql) | Consent/snapshot/chuyển quyền và workflow Receiver/Claim/private proof/đối soát; không seed hoặc sửa receipt |
| [23 Member creator](migrations/23_member_registered_by_postgres.sql) | Đổi registered_by từ FK Receptionist sang User, giữ người tạo/lịch sử; guard/replay, không seed/reset |

`05_reseed_demo_users_and_coaches.sql` là script reset demo cũ, không thuộc chuỗi nâng cấp an toàn cho DB dùng chung/live. File `*_mssql.sql` dành cho SQL Server, không chạy trên PostgreSQL; runtime SQL Server chưa được nghiệm thu.

`archive/` giữ schema/migration cũ để tra cứu. Các file `schema_user_2310*.sql` là bản SQL riêng, không phải schema gộp hiện hành; chỉ mở khi debug đúng lần chạy/dữ liệu liên quan. Không dùng tên file hoặc thời điểm backup để suy ra cấu trúc DB cloud.
