# SCMS — Cấu trúc và vận hành database

PostgreSQL/Supabase là môi trường đang dùng. README hướng dẫn chọn script và đọc mô hình dữ liệu trong repo; không chứng nhận DB đang chạy đã đồng bộ với script.

## 1. Context cho session debug hoặc thay đổi thiết kế

Đọc [AGENTS.md](../AGENTS.md) → Active của [docs00](../docs/00-MASTER-CHECKLIST.md) → bảng quyết định và đúng Dxx trong [docs04](../docs/04-DEBUG-NOTE.md). Sau đó mở phần schema, source BE và test liên quan.

| Khi cần | Nguồn đọc thêm |
|---|---|
| Field/state/error hoặc thay đổi API | Đúng luồng trong [docs01](../docs/01-API-CONTRACT-FE-BE.md) |
| Test, migration, deploy hoặc recovery | [docs02](../docs/02-DEPLOYMENT-GUIDE.md) |
| Cấu trúc/constraint/trigger và seed | [Schema dựng lại](schema_postgres.sql); [schema root](../schema_postgres.sql) nếu cần bảo toàn dữ liệu |
| Kết quả kiểm thử đã ghi | Đúng mục trong [QA](../artifacts/qa/latest.json), theo môi trường/source của lần kiểm |

Quyết định, contract, code và DB thực tế có thể lệch nhau; phải xác định nguyên nhân trước khi sửa. Script trong repo không thay cho việc kiểm cấu trúc, migration marker và dữ liệu liên quan trên đúng DB đích. Không mặc định nạp `.env`, backup, log dài hoặc toàn bộ lịch sử.

## 2. Chọn đúng script

Hai schema có mục đích khác nhau, không phải hai bản sao đồng bộ:

| Script | Mục đích và hành vi chạy lại |
|---|---|
| [schema_postgres.sql ở root](../schema_postgres.sql) | Nâng cấp DB mới/đang có; bảo toàn ID, chứng từ, attendance và lịch sử nghiệp vụ được bảo vệ. Chuẩn hóa danh mục/account theo marker; bước sửa mật khẩu demo cũ chỉ chạy một lần cho 33 email PDF, replay giữ thay đổi về sau. Không tạo bộ lịch sử demo như bản `database/`. |
| [database/schema_postgres.sql](schema_postgres.sql) | **Dựng lại 47 bảng SCMS và dữ liệu tham chiếu mỗi lần chạy**, kể cả DB đang có. ID số độc lập tạo lại từ 1; PK subtype giữ cùng `users.user_id`, UUID/khóa ghép giữ kiểu khóa. Giữ định nghĩa hai bảng tương lai `ai_consultations`, `support_requests`, nhưng dữ liệu cũ trong phạm vi dựng lại bị thay. |
| [bootstrap/scms_structure_postgres.sql](bootstrap/scms_structure_postgres.sql) | Cấu trúc phục vụ runner kiểm PostgreSQL riêng, không có account/giao dịch demo; không thay schema bàn giao Supabase. |

Copy **toàn bộ đúng một file schema gộp** vào SQL Editor trong một lần thực thi. Không chạy nối tiếp hai biến thể; không cần chạy thêm migration01–15 sau schema gộp. “Chạy lại được” của bản `database/` nghĩa là dựng lại thành công, **không có nghĩa giữ dữ liệu đã phát sinh**. DB đã ở baseline29/10 chỉ cần migration13→14 sau preflight; không dùng bản dựng lại để cập nhật phòng. DB đã cập nhật13/14 chỉ cần [15_QueryDR.sql](migrations/15_QueryDR.sql) để thêm QueryDR; không chạy lại schema gộp.

**Bổ sung06/11 — bỏ gói chung:** schema gộp hiện gồm tới15, chưa nhúng bước retire16. Để loại dữ liệu gói chung và khóa ghi trở lại, chạy **toàn bộ một file** [16_retire_legacy_memberships_postgres.sql](migrations/16_retire_legacy_memberships_postgres.sql) trên DB đã có cấu trúc hiện hành, gồm QueryDR15. Không chạy schema dựng lại để thay migration16. Hướng dẫn và kết quả cần đối chiếu tại [docs02](../docs/02-DEPLOYMENT-GUIDE.md#retire-legacy-memberships).

**Dependency Google trước deploy:** BE ghép Google cần cột `users.google_sub` kể cả khi chưa bật đăng nhập Google. Hai schema gộp đã chứa cấu trúc này; DB cũ cần xác minh riêng [13_google_login_postgres.sql](migrations/13_google_login_postgres.sql) theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md#google-login). Prefix13 không chứng minh các migration room/capacity/Google thay thế nhau. SQL16 đã PASS trên Supabase không chứng minh Google đã chạy; không chạy lại SQL16 hoặc schema reset để bổ sung cột này.

Ngoại lệ có phê duyệt riêng: `13_owner_class_capacity_20261102_postgres.sql` chỉ dành7lớp/ID center1 Owner vừa xác nhận, không nhúng vào schema gộp hoặc empty-schema release. Bản tạm `0211-1.sql` đã gộp bước này trước13_room_catalog, cùng transaction; `0212-2.sql` là14 và kiểm đủ marker. Không chạy thêm file nguồn khi dùng cặp root. Giữ nguồn Owner-specific này sau khi xóa bản tạm, không áp dụng nhầm cho DB khác chỉ vì trùng số ID.

Mỗi file dùng transaction. Bản dựng lại chỉ drop danh sách bảng SCMS đã nêu, không drop bảng Supabase `auth`/`storage`/`realtime` hoặc bảng ứng dụng ngoài danh sách. Dependency FK/view từ ngoài phạm vi khiến thao tác dừng và rollback thay vì cascade. Sau lỗi, xử lý nguyên nhân trước khi chạy lại; không thêm `CASCADE` để vượt guard.

Phê duyệt Owner29/10 cho phép chuẩn bị script dựng lại để Owner tự chạy; không mở quyền tự reset cloud cho session debug tiếp theo. Khi sửa thiết kế trên DB có dữ liệu cần giữ, dùng migration riêng có guard/replay và kiểm tương thích theo [docs02](../docs/02-DEPLOYMENT-GUIDE.md).

## 3. Các bảng dễ nhầm và dữ liệu tham chiếu

| Bảng | Ý nghĩa hiện hành |
|---|---|
| `subjects` | 9 môn chuẩn và 18 mức giá theo D02. Danh mục room02/11 có20tài nguyên; Bóng rổ/Bóng chuyền tách sân. Không thêm subject_id vào rooms. |
| `classes` | Chứa khóa Normal (`class_type='Normal'`) và template Advanced (`class_type='Advanced'`). Bản dựng lại có 9 Normal ID10–18 và 9 template Advanced ID1–9. ID này chỉ thuộc fixture dựng lại, không hardcode vào FE. |
| `sessions` | Mỗi dòng là một buổi cụ thể, liên kết `classes` bằng `class_id`. Normal có 12 buổi hợp lệ; Advanced đặt ca linh hoạt theo quyền học, không sinh sẵn 12 ca cho mỗi Member chỉ vì template có `total_sessions=12`. |
| `class_enrollments` | Ghi danh/waitlist Normal và trạng thái liên quan thanh toán. Bản dựng lại có 9 ghi danh. |
| `membership_packages` | Danh mục gói chung đã ngừng sử dụng; sau SQL16 bảng trống và cấm ghi lại, giữ cấu trúc cho ORM. Bản dựng lại không seed gói tháng/VIP; 18 giá môn hiện hành nằm ở `subjects`. |
| `member_packages` | Quyền Advanced từng đăng ký: môn, Primary Coach, giá mua đóng băng, credit và hạn. Bản dựng lại có 4 dòng; `package_id` có thể NULL vì đăng ký gắn trực tiếp với môn/Coach. Default `remaining_credit=0`; Paid hợp lệ mới cấp 12 credit qua workflow D03. |
| `vnpay_payment_attempts` | Metadata từng link pay: `attempt_id` bigint, FK `invoice_id` RESTRICT, `tmn_code`, `txn_ref`, `transaction_date` (đúng vnp_CreateDate gốc), `amount_minor` (VND×100), `created_at`, `last_query_at`. Unique invoice/merchant/ref/date. BE nội bộ; FE không đọc/ghi bảng. Không lưu secret/fullURL hoặc tự backfill invoice cũ. RLS không có policy cho anon/authenticated. |
| `users` | Danh tính đăng nhập của 33 account theo `System Account.pdf`; Active, người dùng thường center1, Admin center NULL. `_scms_accounts` là bảng tạm tự xóa khi commit, không có bảng account thường trực mới. |

**Không khôi phục mô hình gói hội viên chung:** thiết kế hiện hành không bán gói tháng/VIP làm vé vào khóa. Lỗi đã mắc: guard yêu cầu gói Active khiến tài khoản mới không đăng ký Normal được; đã bỏ guard Normal và chặn nhánh giá0 cấp Registered ở BE local. Quyết định tại [D03/D15](../docs/04-DEBUG-NOTE.md), kết quả tại docs00/QA. Tài liệu/seed cũ chỉ là lịch sử, không dùng để tái tạo requirement này.

**Dọn dữ liệu mô hình cũ theo Owner06/11:** file16 tự kiểm/phân loại trong transaction. Chỉ xóa subscription có package_id nhưng cả subject_id/primary_coach_id đều NULL, cùng hóa đơn gói chung cũ và payment attempt/resolution trực tiếp của hóa đơn đó khi không lẫn owner/khóa hiện hành. Gặp link sang session, enrollment, advanced-registration, pause hoặc hình dạng thiếu một phần thông tin thì dừng toàn bộ. Quyền Advanced đã có môn/Coach chỉ được tháo package_id cũ; giá, credit, trạng thái và hóa đơn hiện hành không đổi. Postflight kiểm dữ liệu được bảo vệ; lỗi thì rollback cả phần xóa. Sau khi dọn, trigger chặn tạo lại catalog/subscription/invoice kiểu cũ; không giữ bản sao dòng dữ liệu nghiệp vụ cũ, chỉ lưu số lượng ở marker migration. Bảng danh mục để trống nhằm không phá ORM hiện có, không giữ sản phẩm cũ hoạt động. Không xóa cả member_packages. Script đã kiểm PostgreSQL local; Owner gửi kết quả Supabase PASS, model_retired=true và mọi bộ đếm xóa/tháo liên kết0. Agent không trực tiếp truy vấn cloud; trạng thái BE/A và bằng chứng chuẩn xem docs00/QA.

`classes.total_sessions=12` là cấu hình, không bắt buộc số dòng `sessions` bằng 12. Fixture Tennis Normal có 13 dòng: giữ buổi gốc bị hủy do trung tâm (`center_invalidated=true`) và thêm buổi bù liên kết `makeup_for_session_id`; còn 12 buổi hợp lệ khi lọc `NOT center_invalidated AND status <> 'Cancelled'`. Nghiệp vụ bù/attendance theo D15.

Bản dựng lại seed 13 invoice và 114 session. Ngày tham chiếu tính từ ngày chạy tại `Asia/Ho_Chi_Minh`; số attendance/kết quả phụ thuộc các buổi đã hoàn thành tại thời điểm dựng. Số quan sát của lần kiểm trước nằm ở docs00/QA, không phải quota cố định cho mọi ngày chạy.

`attendances.absence_notice_id` liên kết nguồn Member báo vắng; `superseded_at` giữ bản tự động hết hiệu lực do đổi giờ. Unique index chỉ áp dụng bản hiện hành `(session_id,member_id) WHERE superseded_at IS NULL`; không xóa attendance cũ hoặc backfill notice quá khứ. Luồng và quyền theo D05/D15 và docs01.

Nguồn: danh tính account theo `System Account.pdf`; danh mục/quy tắc theo D02/D03/D05/D12/D15 trong [docs04](../docs/04-DEBUG-NOTE.md). Chứng từ Cash, lịch sử hủy/hoàn credit, báo vắng, hết hạn, bảo trì/bù và attendance là **fixture tổng hợp để minh họa nghiệp vụ**, không phải giao dịch thật hoặc yêu cầu số lượng từ PDF. Chức năng từng bảng có trong `COMMENT ON TABLE` của hai schema.

Mật khẩu demo đã được Owner chốt, lưu salted BCrypt tương thích login BE; chỉ bộ account demo bỏ yêu cầu đổi mật khẩu lần đầu. Luồng tạo/cấp lại mật khẩu qua BE vẫn giữ first-login policy. Kết quả cuối script không xuất mật khẩu/hash. Coach mới chưa chọn `specializationIds` để chuyên môn trống và chưa đủ điều kiện nhận đăng ký/dạy môn đó.

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

`05_reseed_demo_users_and_coaches.sql` là script reset demo cũ, không thuộc chuỗi nâng cấp an toàn cho DB dùng chung/live. File `*_mssql.sql` dành cho SQL Server, không chạy trên PostgreSQL; runtime SQL Server chưa được nghiệm thu.

`archive/` giữ schema/migration cũ để tra cứu. Các file `schema_user_2310*.sql` là bản SQL riêng, không phải schema gộp hiện hành; chỉ mở khi debug đúng lần chạy/dữ liệu liên quan. Không dùng tên file hoặc thời điểm backup để suy ra cấu trúc DB cloud.
