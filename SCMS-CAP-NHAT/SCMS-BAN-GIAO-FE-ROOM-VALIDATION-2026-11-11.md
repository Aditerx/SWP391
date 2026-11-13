# SCMS — Bàn giao FE: validation tên và vị trí phòng

Dành cho FE và Owner; ngày11/11/2026; Session1.3. Phạm vi POST `/api/rooms`, PUT `/api/rooms/{id}`. Trạng thái và bằng chứng chuẩn tại [Active/QA](../docs/00-MASTER-CHECKLIST.md#active); field/error/semantics ở [contract room11/11](../docs/01-API-CONTRACT-FE-BE.md#room-validation-20261111).

Mở [mục lục/checklist của bộ cập nhật](KIEM-TRA.md) để đối chiếu các mục Session1–3; bản này giữ phạm vi và checkpoint của luồng khi bàn giao.

## 1. Kết quả hiện tại

Trước: RoomRequest chưa giới hạn độ dài, JPA dùng mặc định255; test H2 nhận name51/location101 rồi lưu qua POST/PUT, trong khi PostgreSQL đã giới hạn50/100.

Sau: name bắt buộc, không blank, tối đa50; location optional/nullable, tối đa100. Quá dài trả400 Bean Validation trước DB, không mutation/audit. JPA khớp schema có sẵn; OpenAPI RoomRequest khai báo maxLength và location nullable. Đã kiểm HTTP local có login/auth-me, response và DB/audit sau commit trên H2/PG; phạm vi clean/source/môi trường theo Active/QA, chưa là browser/FE PASS.

## 2. Quá trình và quyết định giữ/bỏ

- Lấy50/100 từ rooms.room_name/location trong [schema project](../database/schema_postgres.sql) và [baseline bảo toàn](../schema_postgres_old.sql); đây là constraint project, không phải tiêu chuẩn ngành hoặc con số mới đặt.
- Bổ sung Size cho DTO và length/nullable cho JPA; giữ cột PostgreSQL, không migration hoặc mở rộng dữ liệu.
- Kiểm biên và cách lưu null/blank/khoảng trắng; giữ nguyên RoomService.apply và quyền/center/lock/guard lịch.
- Sửa fixture PG dùng tên riêng để tuân index tên phòng Available; giữ expected và index. Ghi nhận lỗi ánh xạ PUT trùng tên vào error catalog3.2, ngoài phần sửa validation này.

## 3. API và DB liên quan

- POST `/api/rooms`: input hợp lệ trả201; PUT `/api/rooms/{id}` trả200. Cả hai vẫn cần Admin/CenterManager Active+MANAGE_CLASSES, đúng center theo contract hiện hành.
- Quá50/100 trả400; body giữ cấu trúc lỗi hiện có, message bắt đầu `name:` hoặc `location:`. Không thêm mã lỗi hay thay handler chung ở mục này.
- Name null/bỏ/rỗng/toàn khoảng trắng vẫn400. Name có khoảng trắng ngoài được giữ nguyên nếu không blank và không vượt50; độ dài tính trên chuỗi sẽ lưu, không tự trim/truncate.
- Location bỏ/null lưu null, kể cả PUT; `""` hoặc chuỗi trắng được lưu nguyên. FE muốn giữ location khi PUT cần gửi lại giá trị đang lưu, không áp semantics null-giữ-nguyên của training-results.content sang room.
- GET `/api/rooms` dùng readback theo id trong mảng; PATCH `/{id}/status`, roomCode/activity/facility/capacity và guard lịch giữ nguyên. Không đổi bảng/schema/bundle demo.

## 4. FE cần làm

Đồng bộ giới hạn form name50/location100 theo RoomRequest trong [snapshot FE](../artifacts/fe-handoff/openapi.json). Name vẫn bắt buộc; location có thể bỏ/null. Không cắt chuỗi hoặc tự trim thay BE; hiển thị lỗi400 theo field và giữ form để người dùng sửa. Sau lưu thành công, đối chiếu response rồi tải lại GET rooms khi cần. Role/center và các guard tài nguyên hiện có vẫn phải đạt.

Demo dùng Swagger BE local <http://127.0.0.1:18083/swagger-ui/index.html>, DB synthetic riêng; không dùng Supabase hoặc Render. Payload nằm ở file local bị Git bỏ qua `artifacts/runs/room-validation/2026-11-11-session1-3/demo-access.private.json`; credential không ghi trong bản bàn giao. Fixture Room40/center1, Manager1; phòng không có lịch nên chỉ kiểm validation, không suy đã thử lại mọi conflict lịch trong browser. Công cụ browser hiện không khả dụng, hướng dẫn theo operation/API và kết quả mong đợi.

1. POST `/api/auth/login` với `loginManager`, rồi GET `/api/auth/me`:200, userId1/CenterManager/có MANAGE_CLASSES là đúng. GET `/api/rooms`, tìm id40/tên `DEMO Owner room validation1.3` là đúng.
2. PUT `/api/rooms/40` với `validBoundary`:200, name dài50/location dài100 giữ nguyên là đúng; GET rooms đọc lại id40 để đối chiếu.
3. PUT cùng route với `invalidName51`:400, message bắt đầu `name:` là đúng. Tiếp tục với `invalidLocation101`:400, message bắt đầu `location:` là đúng.
4. GET `/api/rooms`, đọc lại id40: name/location vẫn bằng `validBoundary`, không có `Must not persist` là đúng. Đây là expected cho Owner đối chiếu; agent chưa ghi demo thay Owner.

## 5. Việc còn lại

Bản này lưu checkpoint BE11/11; quyết định Owner về demo và bước tiếp hiện hành tra [Active/QA](../docs/00-MASTER-CHECKLIST.md#active). Kiểm BE local không tự thành browser PASS. Chưa push/merge/deploy, nghiệm thu Render/Supabase sau reset hoặc provider thật trong luồng này; Session2/3 không được suy đã hoàn tất từ kết quả này.

Quan sát ngoài phạm vi: trên PG, trùng tên phòng Available cùng center đụng index có sẵn; POST trả409 nhưng PUT có thể500 khi flush qua audit. Đã ghi [đầu vào error catalog3.2](../docs/03-TODO.md#room-duplicate-error-observation-20261111); chưa sửa handler trong1.3. Ca biên hợp lệ phải dùng tên không trùng, không nới/xóa constraint để ép PASS.
