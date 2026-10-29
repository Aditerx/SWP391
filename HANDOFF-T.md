# Gói T — handoff D01/D09

Ngày: 27/10/2026 13:37 ICT  
Base SHA: `0277b2fffa384c91fcb0e39c364e2828326ae5fb`  
Branch: `feat/member-profile-coach-directory`  
Commit SHA D01: `053a5f2896f3e2d2744e21b298b376ecf5303fa2`  
Commit SHA D09: `1dabce57c15af2de8db36865a650287115ebcb1a`

## D01 — `GET/PATCH /api/members/me`

### Contract đã chạy

Chỉ role `MEMBER`; danh tính lấy từ `Authentication.getName()` (email), rồi tra User và Member. URL/body không chọn member. Response chỉ có bảy trường:

```json
{
  "name": "Alice",
  "phone": null,
  "address": null,
  "dateOfBirth": null,
  "gender": null,
  "goal": "A goal",
  "healthNote": "A note"
}
```

PATCH nhận JSON object. Key vắng mặt được giữ nguyên; `null` xóa phone/address/dateOfBirth/gender/goal/healthNote. `name` bắt buộc khác null/rỗng/trắng. `{}` là no-op. Key ngoài danh sách được bỏ qua im lặng và không đổi DB, gồm role/status/centerId/packageId/credit/amount/email/password/avatarUrl/id/memberId.

Ví dụ PATCH thành công và response:

```json
{"name":"Alice Updated","phone":"0901234567","address":"District 1","dateOfBirth":"2000-02-03","gender":"Female","goal":"Run","healthNote":"Knee"}
```

Giới hạn lấy từ `database/schema_postgres.sql` và entity: name 100 (NOT NULL, không blank), phone 20, address 255, gender 10 với đúng giá trị `Male`, `Female`, `Other`; goal 255; healthNote không giới hạn theo cột TEXT; dateOfBirth là ISO `yyyy-MM-dd`, không ở tương lai theo Spring `Clock`. Không tìm thấy phone format validator trong source hiện tại, nên chỉ áp dụng giới hạn cột.

- GET hợp lệ trả JSON trên; PATCH hợp lệ trả cùng shape với giá trị đã lưu. GET sau PATCH xác nhận persistence.
- 400 validation dùng GlobalExceptionHandler: `{"code":"NAME_REQUIRED","message":"name must be a non-blank string"}`, `{"code":"INVALID_GENDER","message":"gender must be Male, Female, Other, or null"}`, `{"code":"FUTURE_DATE_OF_BIRTH","message":"dateOfBirth cannot be in the future"}`; ngày sai định dạng trả code `INVALID_DATE_OF_BIRTH` và message `dateOfBirth must use yyyy-MM-dd`.
- Giới hạn trả code `NAME_TOO_LONG`, `PHONE_TOO_LONG`, `ADDRESS_TOO_LONG`, `GOAL_TOO_LONG` với message lần lượt nêu max 100/20/255/255 ký tự theo cột.
- JSON `null` trả 400 `{"code":"INVALID_PROFILE_BODY","message":"request body must be a JSON object"}`. Array/scalar hoặc JSON sai cú pháp trả 400 rỗng từ Spring `HttpMessageNotReadableException` handler mặc định.
- Anonymous trả 401, body rỗng trong MockMvc do security entry point dùng `sendError(401)`. Role khác trả 403 dạng `{"timestamp":"<runtime Instant>","status":403,"error":"Forbidden","message":"Access Denied"}`.
- Thử `/api/members/{memberBId}` với Member A trả 403 ở route quản trị; `memberId` gửi trong body bị bỏ qua. DB của B không đổi.
- Service dùng một `@Transactional` cho User + Member; test ép lỗi lúc lưu Member xác nhận thay đổi User cũng rollback.
- `FirstLoginPasswordFilter` hiện chặn `/api/members/me` khi `firstLogin=true` (403 `PASSWORD_CHANGE_REQUIRED`) vì endpoint này không thuộc allowlist đổi mật khẩu hiện có. Không sửa filter/config do ngoài phạm vi; cần BE chính xác nhận cách xử lý trước khi FE dùng profile cho tài khoản first-login.
- Câu hỏi BE chính nếu FE cần hiển thị định danh: có cần thêm `id` và/hoặc `email` chỉ đọc vào response không? Hiện response cố ý chỉ có bảy field đã chốt.


### Câu hỏi cho BE chính

- Empty strings `""` for `phone`, `address`, `goal`, and `healthNote` are stored as `""`, not converted to `null`.
- `phone` has a 20-character maximum only; there is no format validation.
- `healthNote` has no application-level length limit (database column is `TEXT`).
- Query `(:centerId IS NULL OR u.centerId = :centerId)` has only been run on H2, not verified on real PostgreSQL.
- Coach and specialization lists have no `ORDER BY`, as in the old code.

### Formatting correction

Byte scan of all 10 files in diff `0277b2f..HEAD`: zero lone CR bytes (CR not followed by LF). The indicated contexts already use LF; no D01 content change was needed.
## D09 — `GET /api/coaches`

Contract giữ `CoachSummaryResponse`, filter `specializationId` và `keyword` (trim, không phân biệt hoa/thường, tìm trong tên Coach hoặc specialization), trả toàn bộ specialization của Coach khớp. Coach không specialization có `specializations: []`; list rỗng trả `[]`. Phạm vi lấy từ `CenterContext`: center cụ thể cho tài khoản thường; null chỉ cho Admin và khi đó query toàn hệ thống. Contact chỉ hiện với authority `MANAGE_USERS`.

### Tái hiện trước và regression sau

- Trước sửa: `.\mvnw.cmd '-Dtest=CoachDirectoryIntegrationTest#reproducesLazySpecializationFailureBeforeFix' test` — 1 test tích hợp H2/MockMvc pass khi xác nhận lỗi. Root: `org.hibernate.LazyInitializationException: could not initialize proxy [com.sportscenter.specialization.Specialization#1] - no Session`, khi controller gọi `getSpecialization().getName()` sau khi repository transaction/persistence context đã đóng (OSIV=false, request không bọc transaction).
- Sau sửa: controller gọi `CoachDirectoryService` qua Spring bean; service `@Transactional(readOnly = true)` lấy center, fetch Coach+User+Role theo center, batch fetch CoachSpecialization+Specialization một lần, lọc và map DTO trước khi transaction đóng. Không bật OSIV/EAGER và không đổi method assignment/available-slots/at-risk.
- Hibernate Statistics: 4 prepared statements với 2 coaches và 4 với 8 coaches. Fixture có 2 center, CenterManager, Member, Admin, hai specialization cho một Coach, Coach không có specialization, Coach center khác; filter kết hợp trả đủ specialization. Empty center scope trả `[]`.
- Contact ẩn với CenterManager không có `MANAGE_USERS`, hiện khi có authority này. Member chỉ thấy coach center mình; Admin không center thấy cả hai center. Anonymous 401; account thường thiếu center bị CenterContext từ chối 400.

## Fixture, lệnh và kết quả

Tất cả là local Windows + Java 17.0.20.1, H2 in-memory độc lập; MockMvc dùng security filter chain thật nhưng principal được cung cấp bởi Spring Security test (`user()`), không phải HTTP login thật. Không nạp `.env`, không kết nối cloud, không chạy migration/seed dự án.

- Baseline trước sửa, `.\mvnw.cmd clean test`: 269 run / 250 pass / 0 failures / 0 errors / 19 skipped.
- Tái hiện D09 trước sửa: 1 / 1 / 0 / 0 / 0 skipped; LazyInitializationException như ghi trên.
- Kiểm tra chức năng: `.\mvnw.cmd '-Dtest=MemberSelfProfileControllerTest,CoachDirectoryIntegrationTest' test`: 11 run / 11 pass / 0 failures / 0 errors / 0 skipped.
- Cuối lượt, `.\mvnw.cmd clean test`: BUILD SUCCESS, 280 run / 261 pass / 0 failures / 0 errors / 19 skipped. So baseline: +11 tests, cùng 19 skip và không thêm lỗi. Skip là các test opt-in/live hiện có, không bật trong lượt này.

## Files và giới hạn

Files code/test thuộc scope:

- D01: `src/main/java/com/sportscenter/user/MemberSelfProfileController.java`, `MemberSelfProfileService.java`, hai DTO mới trong `user/dto/`, `src/test/java/com/sportscenter/user/MemberSelfProfileControllerTest.java`.
- D09: `src/main/java/com/sportscenter/specialization/CoachController.java` (chỉ GET list), `CoachDirectoryService.java` mới, `src/main/java/com/sportscenter/user/CoachRepository.java` (chỉ thêm read query), `src/main/java/com/sportscenter/specialization/CoachSpecializationRepository.java` (chỉ thêm batch read query), `src/test/java/com/sportscenter/specialization/CoachDirectoryIntegrationTest.java`.
- File handoff này ở root, local/untracked, không đưa vào commit.

Chưa deploy, chưa kiểm thử FE, chưa chạy trên PostgreSQL/DB thật; chưa chứng minh HTTP login thật hoặc Render. Không có secret trong handoff. Không sửa file ngoài ownership, docs trung tâm, `.env`, `old-docs/`, frontend hay dist. Không push. BE chính cần merge hai commit và chạy lại `clean test` trên nhánh tích hợp.
