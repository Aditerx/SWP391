# SCMS — Bàn giao FE: sửa nhận xét result sau buổi — 12/11/2026

Đối tượng: FE tích hợp màn hình kết quả của Coach/Member và Owner nghiệm thu Session2.2/R2. Nguồn field/state chính thức: [contract](../docs/01-API-CONTRACT-FE-BE.md#training-result-after-session-r2-20261112); trạng thái và bằng chứng theo môi trường: [Active/QA](../docs/00-MASTER-CHECKLIST.md#active).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Owner đã duyệt hình dạng PATCH content-only và yêu cầu giải thích tác động flow hiện tại. BE đã đạt local H2/clean và HTTP PostgreSQL, gồm response cùng trạng thái DB/audit sau commit; bằng chứng chuẩn ở Active/QA. FE hiện hành là https://scms-demo-04nov.pages.dev/ nhưng repo/build/API origin chưa xác minh; browser/FE và Render chưa nghiệm thu. Owner đã chọn skip demo2.2, ghi SKIPPED_BY_OWNER; skip không thành PASS.

| Ngữ cảnh FE | Flow trước R2 | Flow sau R2 |
|---|---|---|
| Ghi kết quả trong `[start,end)` | POST đơn/bulk ghi attendance và nhận xét hợp quyền | Tiếp tục POST hiện có; PATCH sau buổi trả400 nếu chưa tới end |
| Coach bổ sung nhận xét sau end, trước cutoff | POST bị chặn; correct chỉ sửa attendance/note | Lấy resultId đã có rồi PATCH content; không gửi lại attendanceStatus/note hoặc gọi correct để sửa nhận xét |
| Sửa attendance/note sau end | POST attendance correct, reason bắt buộc, đúng quyền/hạn | Giữ correct; nếu cùng lúc cần sửa content thì đó là request PATCH riêng, không phải một transaction chung của hai request |
| Result còn thiếu | Không có mutation chỉ tạo nhận xét sau buổi | PATCH404; FE không tự tạo result, Attendance hoặc chuyển sang POST để vượt cửa giờ |
| Đã tới00:00 ngày sau ngày Session | Không có quyền sửa content sau buổi | PATCH403; chỉ đọc nhận xét. Quyền correction attendance của Manager vẫn theo contract riêng |
| Member đọc lại | GET training-results ép own scope | Giữ GET và response hiện có, đọc content đã sửa; không cấp quyền PATCH cho Member |

## 2. Quá trình và quyết định giữ/bỏ

1. Rà D15/RP-36 và POST hiện có xác nhận POST vừa ghi result vừa sync Attendance, nên giữ cửa POST và thêm PATCH riêng để giới hạn đúng một field.
2. Owner duyệt PATCH theo resultId với content bắt buộc/non-null; giữ chuỗi rỗng để xóa và giữ nguyên khoảng trắng như semantics ghi content trước đó.
3. Giữ Coach Active+RECORD_RESULT, cùng center, đúng phân công Session và là tác giả result; không cấp Coach mới sửa result Coach cũ hoặc rewrite tác giả.
4. Tái dùng khóa/transaction và AuditService/system_logs; giữ recordedAt gốc, không thêm bảng, version/snapshot hoặc màn hình audit.
5. Test phân biệt Attendance thật sự thiếu với lịch sử chỉ còn superseded; sửa lookup kiểm riêng để trường hợp superseded bị chặn và retry/rollback không để lọt thay đổi hoặc audit.

## 3. API và DB liên quan

`PATCH /api/training-results/{resultId}/content` nhận đúng mục đích là thay content:

```json
{"content":"Bổ sung hướng dẫn kỹ thuật sau buổi"}
```

`content` bắt buộc/non-null; `""` xóa, chuỗi trắng lưu nguyên. Không dùng semantics bỏ/null để giữ như POST: PATCH thiếu/null là400. Không thêm reason, không lấy actor/role/session/member/coach từ payload để mở scope. Field lạ bị mapper bỏ qua không phải field được hỗ trợ ghi.

Thành công200 trả `TrainingResultResponse` hiện có. `resultId`, `sessionId`, `memberId`, `coachId`, `recordedAt`, `attendanceStatus`, `note` giữ giá trị tương ứng trước sửa; chỉ content đổi. Attendance thiếu thì note trả null và vẫn không tạo Attendance. `recordedAt` tiếp tục là thời điểm ghi gốc, **không phải thời điểm sửa gần nhất**. Audit chứa người sửa/thời điểm/result/session/member/loại thay đổi và giữ nội bộ; không có field lastModified mới cho FE.

| HTTP / điều kiện | FE xử lý |
|---|---|
| 200 | Nhận content đã lưu; GET lại result đúng session/member để đối chiếu |
| 400 validation content | Giữ nội dung đang nhập và yêu cầu gửi chuỗi tường minh |
| 400 RESULT_CONTENT_NOT_OPEN | Đang trước end; dùng flow POST trong buổi nếu đủ điều kiện, không tự retry PATCH |
| 400 SESSION_NOT_HELD / NORMAL_NOT_STARTED / SESSION_TIME_REQUIRED / ATTENDANCE_SUPERSEDED / NOT_YOUR_CLASS | Tải lại ngữ cảnh và hiển thị lý do; không đổi Session/state/eligibility để làm request thành công |
| 401 | Đăng nhập và kiểm auth-me lại trước thao tác |
| 403 | Kiểm Coach/quyền hiện hành, phân công/tác giả/center và hạn; không thử lại bằng role quản trị |
| 404 | Result không tồn tại; tải lại danh sách, không upsert result hoặc Attendance |

Validation/403/404 đi qua handler giữ body timestamp/status/error/message. BusinessException400 có code/message;403 của service R2 không có field code riêng, tiền tố RESULT_CONTENT_FORBIDDEN hoặc RESULT_CONTENT_DEADLINE_PASSED nếu có nằm trong message. Các cổng phiên vẫn có body riêng: ACCOUNT_NOT_ACTIVE, SESSION_ROLE_CHANGED hoặc PASSWORD_CHANGE_REQUIRED có thể nằm trong code và thiếu timestamp. FE dùng HTTP status thực tế, message và code nếu có; không giả định mọi403 cùng envelope hoặc áp mẫu error của attendance correction cho PATCH này.

`training_results.content` là cột được sửa; coach_id/recorded_at/attendance_status và Attendance/note/check-in/credit/lịch/Enrollment/Invoice giữ nguyên. Dùng khóa center→Session→result, lấy now sau khóa; audit `UPDATE_RESULT_CONTENT` cùng transaction, target training_results/resultId, không chép toàn nội dung vào log. No-op kiểm đủ quyền/hạn rồi trả200, không thêm audit. Gửi hai giá trị khác nhau được xử lý tuần tự theo khóa; request ghi sau quyết định giá trị cuối, chưa có ETag/version để cảnh báo tab cũ.

## 4. FE cần làm

1. Giữ flow POST đơn/bulk trong buổi và correction attendance/note hiện có; tách thao tác lưu nhận xét sau buổi để chỉ gửi PATCH với resultId từ GET, không gom state/note/coachId vào body.
2. Gắn draft/loading/kết quả request với resultId và Session đang chọn; đổi buổi hoặc nhận response trễ không được cập nhật nhầm hàng của buổi mới. Kết quả HTTP của hàng nào chỉ áp vào hàng đó; GET sau lưu dùng cùng session/member.
3. Chỉ cho sửa sau end tới trước00:00 ngày kế tiếp ngày Session theo giờ VN; dùng sessionDate/endTime từ Session đang chọn trong flow lịch hiện có. TrainingResultResponse không thêm endTime/contentEditAllowed/serverTime mới; giao diện thời gian chỉ là hỗ trợ, BE vẫn quyết định quyền/hạn. Không tái dùng correctionAllowed của Attendance làm quyền content vì nó không kiểm tác giả result và có ngoại lệ Manager khác nhau.
4. Sau PATCH200 hiển thị content từ response rồi GET lại; không lấy GET200 sau PATCH lỗi làm bằng chứng đã lưu. Giữ draft khi lưu lỗi, xử lý status/message như bảng trên và không dùng recordedAt làm giờ sửa mới.
5. Không có PATCH bulk trong R2; nếu giao diện lưu nhiều hàng thì theo dõi từng request thành công/thất bại, không quảng bá toàn bộ batch là atomic. Bố trí nút, câu chữ và cách giữ draft do FE quyết định.

**Demo ngắn đã chuẩn bị:** [hướng dẫn login→auth-me→PATCH→Member đọc lại](../docs/02-DEPLOYMENT-GUIDE.md#local-r2-content-demo-20261112) trên Swagger <http://127.0.0.1:18085/swagger-ui/index.html>. Coach1/Session1/result1/Member2; fixture synthetic có content ban đầu, attendanceStatus Absent/check-in null và note phải giữ. Dùng JSON riêng bị Git bỏ qua để login, không copy credential sang FE online hoặc tài liệu công khai. Runtime đã kiểm API login/auth-me/GET/schema; buildCommit unknown, nguồn đối chiếu fingerprint với bản đã kiểm, không là SHA deploy hoặc browser PASS.

Cửa sửa thành công từ07:30 ngày12/11 đến trước00:00 ngày13/11 giờ VN; khi chuẩn bị runtime vẫn trước cửa, probe PATCH trả400 RESULT_CONTENT_NOT_OPEN và DB/audit không đổi là đúng cho lớp kiểm đó. Chưa ghi content mới thay Owner hoặc gọi đó là demo success. Clock ứng dụng nguyên trạng; nếu giờ thực không phù hợp thì chờ hoặc Owner chọn skip riêng.

## 5. Việc còn lại

Owner đã chọn skip demo2.2 và cho tiếp2.3/shared plan; giữ bằng chứng BE, không suy thành browser PASS. Các lựa chọn và kết quả2.3 theo Active/QA. Không tự bắt đầu Session3 hoặc phát hành cloud.

Kiểm contract/OpenAPI R2 đối chiếu trực tiếp với app đã kiểm đạt. Script audit contract tổng thể chưa hoàn tất vì helper còn tham chiếu schema_postgres.sql ở root đã bị xóa từ trước; giữ deletion, không khôi phục/đổi fingerprint lịch sử trong R2. Việc duy trì checker này chưa được tính là PASS hoặc là lỗi nghiệp vụ PATCH.
