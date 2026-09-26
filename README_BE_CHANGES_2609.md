# Bàn giao tích hợp Backend–Frontend ngày 26/09

## Trạng thái

**Đợt 1 đã hoàn tất.** Center Manager có thể đăng nhập bằng session thật, đọc dữ liệu
từ backend và thao tác các chức năng quản lý chính mà không dùng optimistic update.

Đã hoàn thành:

- HTTP client dùng `credentials: 'include'`, chuẩn hóa lỗi HTTP/validation/401.
- API adapter và mapper cho subject, room, class, package, dashboard, audit log.
- Đăng nhập, khôi phục phiên bằng `/api/auth/me` và logout bằng session backend.
- Nạp dữ liệu Center Manager đồng thời sau khi xác thực.
- Tạo, sửa, xóa subject trên giao diện **Bộ môn & phòng**.
- Tạo, sửa, đổi trạng thái room trên cùng giao diện.
- Tạo, sửa, đổi trạng thái package trên giao diện **Gói thành viên**.
- Tạo, sửa class và phân công lại coach trên giao diện **Lớp học**.
- Modal tạo lớp lấy subject và room từ backend.
- Chỉ cập nhật React state sau khi API trả thành công; lỗi được giữ trong modal/toast.
- Chế độ seed local hỗ trợ các thao tác trên qua lệnh `npm.cmd run dev:local`.
- Vite proxy `/api` sang `http://localhost:8080`.

## Mapping contract còn giữ ở đợt 1

Backend `classes` hiện là class template, chưa trả session cụ thể. Vì vậy các field
`date`, `startTime`, `endTime`, `room`, `durationMinutes` vẫn là fallback của frontend.
Khi sửa hoặc phân công coach, frontend giữ lại các field fallback này, không ghi chúng
vào API class.

Các mapping đáng chú ý:

| Dữ liệu giao diện | Dữ liệu backend | Quy ước đợt 1 |
|---|---|---|
| `durationMonths` | `durationDays` | 1 tháng = 30 ngày; giữ cả `durationDays` sau khi map |
| `benefits[]` | `benefits` text | Nối/tách bằng dấu `;` hoặc xuống dòng |
| Package tiếng Việt | Chưa có cột riêng | Dùng name/benefits backend làm fallback |
| Class ngày, giờ, phòng | Bảng `sessions` | Chưa tích hợp; giữ fallback hiện có |
| `enrolledCount` | `class_enrollments` | Đợt 1 trả 0 nếu API class chưa có count |
| Audit previous/new value | `system_logs.detail` | Chưa tách đầy đủ, hiển thị detail làm reason |

## 1. Chuẩn bị database

Yêu cầu: Java 17, SQL Server Express, Node.js và npm.

`SQLQuery3_2609.sql` là script fresh bootstrap và có `DROP DATABASE`. Hãy backup dữ
liệu cần giữ và xác nhận đúng SQL Server instance trước khi chạy trong SSMS.

Sau khi chạy script, database phải có tài khoản demo:

```text
manager@scms.com / 12345678
```

## 2. Cấu hình và chạy backend

Cách 1 — tạo file `.env` tại thư mục gốc từ `.env.example`:

```properties
DB_URL=jdbc:sqlserver://PHAT-TAI;instanceName=SQLEXPRESS;databaseName=gym_management_system;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=<mat-khau-local>
```

Không commit `.env`. Backend tự import file này nếu tồn tại.

Cách 2 — đặt biến môi trường trong PowerShell hiện tại:

```powershell
$env:DB_URL = 'jdbc:sqlserver://PHAT-TAI;instanceName=SQLEXPRESS;databaseName=gym_management_system;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = '<mat-khau-local>'
```

Chạy backend tại thư mục gốc:

```powershell
.\mvnw.cmd spring-boot:run
```

Kiểm tra Swagger tại <http://localhost:8080/swagger-ui.html>.

## 3. Chạy frontend với API thật

Mở terminal thứ hai:

```powershell
cd .\SCMS_demo_fe_v1.1
npm.cmd install
npm.cmd run dev
```

Mở <http://localhost:8443> và đăng nhập:

```text
Email: manager@scms.com
Password: 12345678
```

Nếu cần đổi địa chỉ backend:

```powershell
$env:VITE_API_PROXY_TARGET = 'http://localhost:8080'
npm.cmd run dev
```

## 4. Kịch bản test giao diện chi tiết

Nên dùng tiền tố `DEMO-2609-` để dễ nhận biết dữ liệu test.

### 4.1 Đăng nhập và dữ liệu ban đầu

1. Mở frontend, đăng nhập bằng tài khoản manager.
2. DevTools → Network phải thấy `POST /api/auth/login` trả 200 và có cookie session.
3. Refresh trang. Giao diện vẫn đăng nhập và `GET /api/auth/me` trả 200.
4. Kiểm tra các request GET subjects, rooms, classes, packages, dashboard, audit logs
   đều trả 200.

Kết quả mong đợi với seed SQL hiện tại: 4 subjects, 4 rooms, 4 classes, 4 packages.

### 4.2 Subject CRUD

1. Chọn **Bộ môn & phòng** → tab **Subjects**.
2. Chọn **Thêm bộ môn**, nhập `DEMO-2609-Mobility` và mô tả, rồi lưu.
3. Mở nút sửa của item vừa tạo, đổi mô tả và lưu.
4. Xóa item vừa tạo và xác nhận.

Mong đợi: danh sách chỉ đổi sau response thành công; POST/PUT trả 2xx, DELETE trả
204. Nếu subject đang được class tham chiếu, backend có thể từ chối xóa và UI phải
hiển thị lỗi thay vì tự xóa khỏi danh sách.

### 4.3 Room create/update/status

1. Trong **Bộ môn & phòng**, chọn tab **Rooms**.
2. Tạo phòng `DEMO-2609-Studio`, vị trí `Tầng 2`, sức chứa `20`, trạng thái Available.
3. Sửa sức chứa thành `24`.
4. Bấm nút nguồn để đổi Available → Maintenance, rồi bật Available lại.

Mong đợi: POST/PUT/PATCH đều trả 2xx; badge trạng thái và sức chứa cập nhật đúng sau
response.

### 4.4 Package create/update/status

1. Chọn **Gói thành viên** → **Tạo gói mới**.
2. Nhập tên `DEMO-2609-Fitness`, thời hạn `2` tháng, giá `900000`.
3. Nhập mỗi quyền lợi trên một dòng, ví dụ `Gym` và `Yoga`, rồi lưu.
4. Chọn **Sửa**, đổi giá hoặc quyền lợi, rồi lưu.
5. Chọn **Tắt**, kiểm tra badge Inactive; chọn **Bật** để khôi phục.

Mong đợi: request gửi `durationDays: 60`; benefits được serialize thành text và map
trở lại thành danh sách; giá/thời hạn/trạng thái trên card cập nhật sau response.

### 4.5 Class create/edit/assign coach

1. Chọn **Lớp học** → **Tạo lớp học**.
2. Chọn subject và room từ dữ liệu backend, chọn coach, nhập sức chứa và ngày.
3. Lưu và xác nhận POST `/api/classes` thành công.
4. Trên card vừa tạo chọn **Sửa**; đổi tên, subject, sức chứa hoặc ngày rồi lưu.
5. Mở **Phân công coach…**, chọn một coach khác.

Mong đợi: PUT `/api/classes/{id}` và PATCH `/api/classes/{id}/coach` thành công;
tên coach đổi trên card. Giờ/phòng fallback không bị mất sau thao tác phân công.

### 4.6 Audit và logout

1. Chọn **Nhật ký hệ thống** và kiểm tra các thao tác backend đã được ghi.
2. Logout; request `POST /api/auth/logout` trả thành công.
3. Thử mở lại màn quản lý hoặc refresh; ứng dụng phải quay về trang public/login.

## 5. Test nhanh bằng Swagger

Trong cùng tab Swagger, gọi lần lượt:

1. `POST /api/auth/login`

```json
{"email":"manager@scms.com","password":"12345678"}
```

2. `GET /api/subjects`, `/api/rooms`, `/api/classes`, `/api/packages`.
3. `POST /api/subjects`

```json
{"name":"DEMO-2609-Pilates","description":"Swagger demo"}
```

4. `POST /api/rooms`

```json
{"name":"DEMO-2609-Studio","location":"Tầng 2","capacity":20,"status":"Available"}
```

5. `POST /api/classes` (thay ID bằng dữ liệu đang có)

```json
{"name":"DEMO-2609-Class","subjectId":1,"coachId":2,"maxCapacity":20,"startDate":"2026-10-01","endDate":"2026-12-01","status":"Open"}
```

6. `PATCH /api/classes/{id}/coach`

```json
{"coachId":3}
```

7. `POST /api/packages`

```json
{"name":"DEMO-2609-Package","durationDays":30,"price":500000,"benefits":"Gym;Yoga","status":"Active"}
```

8. `GET /api/reports/dashboard`, `GET /api/audit-logs`, sau đó
   `POST /api/auth/logout`.

## 6. Chạy frontend bằng seed local

Không cần backend/database:

```powershell
cd .\SCMS_demo_fe_v1.1
npm.cmd run dev:local
```

Đăng nhập bằng form demo. Subject, room, package và class đều thao tác trong memory;
refresh trang sẽ trả dữ liệu về seed ban đầu. Chế độ này chỉ dùng demo UI, không dùng
để xác nhận API hoặc persistence.

Muốn quay lại API thật:

```powershell
npm.cmd run dev
```

`dev` luôn bật tích hợp API tại cổng 8443; `dev:local` luôn dùng seed local tại
cổng 5173. Hai script không phụ thuộc vào biến môi trường còn sót trong terminal.

## 7. Lệnh tự kiểm tra trước khi bàn giao

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
cd .\SCMS_demo_fe_v1.1
npm.cmd run build
```

Kết quả ngày 26/09/2026:

- Maven test: **BUILD SUCCESS**.
- Vite production build: **thành công**.
- Runtime seed local tại `http://localhost:8443`: **HTTP 200**.
- Session backend thật: login role `CenterManager` thành công; GET trả 4 subjects,
  4 rooms, 4 classes, 4 packages; dashboard và audit log hoạt động.
- Smoke test mutation backend: subject create/update/delete (item tạm đã xóa), room
  update và `Available → Maintenance → Available`, package update và
  `Active → Inactive → Active`, class update và assign coach đều thành công.
- Vite cảnh báo bundle JavaScript lớn hơn 500 kB; đây là cảnh báo tối ưu hiệu năng,
  không chặn build hoặc demo đợt 1.

## Các ràng buộc nghiệp vụ bổ sung ngày 26/09

Các quyết định đã thống nhất và đã được bổ sung vào backend/database/frontend:

- Room chuyển sang `Maintenance` hoặc `Closed` sẽ bị từ chối nếu còn session `Scheduled`
  từ ngày hiện tại trở đi. Backend trả danh sách tối đa 5 session bị ảnh hưởng để người quản lý
  phân phòng khác hoặc hủy lịch; hệ thống không tự động hủy lịch và không thay đổi session quá khứ.
- Session mới không được nằm trong quá khứ, không được xếp vào room không ở trạng thái `Available`,
  và `class.max_capacity` không được lớn hơn `room.capacity`. Việc giảm sức chứa room hoặc tăng
  sức chứa class cũng bị chặn nếu làm sai quan hệ này.
- Class mới có trạng thái `Open` phải có `startDate >= ngày hiện tại`; `endDate >= startDate`.
  Class `Ongoing` được phép có ngày bắt đầu trong quá khứ. Form tạo lớp dùng ngày hiện tại thay cho
  ngày hard-code, đặt `min` cho ngày và giới hạn sức chứa theo room đang chọn.
- Membership package `Inactive` không được dùng để đăng ký hoặc gia hạn mới. Membership và invoice
  lịch sử vẫn được giữ và hiển thị. Room và package tiếp tục dùng status/disable, không có hard delete.
- Thay đổi trạng thái Room/Package qua backend được ghi audit log.
- Cả backend và database đều xác nhận user được phân công cho class thực sự có role `Coach`.

Các guard ở database nằm cuối `SQLQuery3_2609.sql`, sau phần seed, để script vẫn bootstrap được dữ
liệu demo lịch sử. Class API đợt 1 vẫn là class template; room, ngày giờ session và quan hệ class-room
thực được lưu tại `sessions`. Room trên modal hiện chỉ là fallback của frontend cho tới khi Session API
được tích hợp.

## Các việc còn lại

### 1. Chuẩn hóa mapper và trạng thái Room

Schema Room chỉ chấp nhận `Available`, `Maintenance`, `Closed`; frontend hiện lowercase tương ứng
thành `available`, `maintenance`, `closed` để hiển thị và phần này đang đúng. Tuy nhiên backend đang
dùng `ACTIVE` khi request tạo/sửa không gửi status, không thuộc constraint của Room và có thể làm
database từ chối request.

Đề xuất cần thực hiện trọn gói:

- Đổi default Room ở backend thành `Available`.
- Dùng enum/validation tại request hoặc service và chỉ nhận đúng ba giá trị trong schema, không chờ
  database ném lỗi constraint.
- Frontend có thể dùng giá trị lowercase để hiển thị nhưng payload phải gửi lại đúng enum backend.
- Nút nguồn chỉ nên chuyển `Available <-> Maintenance`; `Closed` là trạng thái riêng và không tự động
  bật lại thành `Available` khi chưa có quy tắc nghiệp vụ rõ ràng.

### 2. Bảo toàn trạng thái `Ongoing` của Class

Schema Class chấp nhận `Open`, `Ongoing`, `Closed`, `Cancelled`. Mapper hiện gom cả `Open` và
`Ongoing` thành `published`, dù vẫn lưu trạng thái gốc trong `apiStatus`. Vì vậy lưu mà không đổi status
thường vẫn giữ được `Ongoing`, nhưng nếu người dùng đổi rồi chọn lại option chung `Open / Ongoing`,
frontend sẽ gửi `Open`; người dùng cũng không thể chủ động chuyển riêng giữa hai trạng thái.

Đề xuất cần thực hiện trọn gói:

- Dropdown chỉnh sửa có bốn lựa chọn độc lập, tương ứng đúng bốn enum backend; không dùng option
  chung `Open / Ongoing`.
- Nhãn hiển thị có thể được map riêng, nhưng payload phải bảo toàn từng enum backend và giữ nguyên
  trạng thái gốc khi người dùng không thay đổi.
- Đổi default `ACTIVE` hiện có trong `SportsClassService` thành `Open` và validate enum tại backend.

### 3. Đề xuất bổ sung nhỏ, tạm gác lại

Thiết kế quyền riêng hoặc chế độ `correction/audit` để cho phép sửa dữ liệu lịch sử có kiểm soát.
Nội dung này độc lập với các mapper/constraint ở trên và chưa nằm trong phạm vi triển khai hiện tại.

## Phạm vi các đợt tiếp theo

- Đợt 2: user, member, coach/receptionist và membership lifecycle.
- Đợt 3: session, lịch thật, booking/enrollment và kiểm tra trùng coach/phòng.
- Đợt 4: invoice/payment/report theo khoảng thời gian.
- Đợt 5: training plan, result, attendance, evaluation.
- Đợt 6: AI service riêng; không đặt API key trong frontend.
