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

## Phạm vi các đợt triển khai

- **Đợt 1 (ĐÃ HOÀN TẤT):** Nền tảng xác thực Session, CRUD Bộ môn, Phòng tập, Gói thành viên, Lớp học ban đầu và Báo cáo tổng quan.
- **Đợt 2 (ĐÃ HOÀN TẤT):** Tách role Admin khỏi Center Manager, Quản lý Users toàn diện, Phân quyền RBAC động, Luồng Lễ tân đăng ký học viên tại quầy và đăng ký / gia hạn gói tập (`/api/members/{memberId}/subscriptions`).
- **Đợt 3 (ĐÃ HOÀN TẤT):** Module Lịch học thực tế (`/api/sessions`), Xếp lịch tự động định kỳ, Thuật toán kiểm tra trùng phòng/HLV theo thời gian thực và Module Đăng ký lớp học (`/api/enrollments`, `/api/classes/{id}/enroll`) với 5 Guards.
- **Đợt 4 (ĐÃ HOÀN TẤT):** Module Hóa đơn, Thanh toán tại quầy (`/api/invoices`), tự động sinh hóa đơn khi mua gói tập và Báo cáo thống kê tài chính nâng cao (`/api/reports/dashboard`, `/api/reports/revenue`, `/api/reports/members`).
- **Đợt 5 (ĐÃ HOÀN TẤT):** Module Kế hoạch tập luyện (`/api/training-plans`), Ghi nhận kết quả buổi học (`/api/training-results`), Điểm danh & Check-in/out (`/api/attendances`) kèm Audit Attendance Correction, và Đánh giá định kỳ (`/api/evaluations`).
- **Đợt 6 (Quy hoạch tương lai):** AI Workout Recommendation & AI Assistant Chatbot (sử dụng LLM service độc lập, không lưu API key tại frontend).

---

# Bàn giao triển khai Đợt 2: Phân quyền RBAC, Quản lý User & Luồng Lễ tân

## 1. Trạng thái Đợt 2
- **Backend (Spring Boot 3 + Java 17 + Spring Security):** Đã hoàn tất và biên dịch thành công (`BUILD SUCCESS`).
- **Frontend (React + Vite + Tailwind CSS):** Đã hoàn tất giao diện, context, api mapper và build thành công (`dist/` build cleanly).
- **Database Scripts:** Đã cập nhật seed script `SQLQuery3_2609.sql` và `schema_postgres.sql` với vai trò `Admin`, danh mục quyền hạn RBAC đầy đủ và tài khoản demo `admin@scms.com`.

## 2. Tài khoản Demo Hệ thống

| Vai trò | Email đăng nhập | Mật khẩu | Chức năng chính |
|---|---|---|---|
| **Admin** 👑 | `admin@scms.com` | `12345678` | Quản lý Users (tạo/sửa/khóa tài khoản/phân vai trò), Ma trận phân quyền RBAC, Xem Audit Log, Báo cáo hệ thống. |
| **Center Manager** 🏢 | `manager@scms.com` | `12345678` | Quản lý nghiệp vụ: Lớp học, Bộ môn, Phòng tập, Gói tập, Báo cáo doanh thu & Vận hành. |
| **Lễ tân (Receptionist)** 🛎️ | `mai.do@fitzone.vn` / `nam.vu@fitzone.vn` | `12345678` | Đăng ký thành viên mới tại quầy, Đăng ký / Gia hạn gói tập (`/api/members/{memberId}/subscriptions`), Ghi nhận thanh toán. |
| **Huấn luyện viên (Coach)** 💪 | `huong.tran@fitzone.vn` | `12345678` | Kế hoạch tập luyện cá nhân, Điểm danh buổi tập, Ghi nhận kết quả đánh giá học viên. |
| **Hội viên (Member)** 🏃 | `oanh.hoang@fitzone.vn` | `12345678` | Xem lịch tập, thông tin gói tập hiện tại, đăng ký lớp học. |

## 3. Các Endpoints Backend mới & Contract API

### A. Quản lý Người dùng (`/api/users`)
- `GET /api/users`: Lấy danh sách người dùng (hỗ trợ query param `?role=Receptionist&search=Mai`).
- `GET /api/users/{id}`: Chi tiết thông tin người dùng theo ID.
- `POST /api/users`: Tạo người dùng mới. Backend tự động đồng bộ sang bảng subtype tương ứng (`coaches`, `receptionists`, `members`).
- `PUT /api/users/{id}`: Cập nhật thông tin và vai trò người dùng.
- `PATCH /api/users/{id}/status`: Đổi trạng thái (`Active`, `Locked`, `Inactive`).
- `PATCH /api/users/{id}/role`: Phân lại vai trò cho người dùng.

### B. Phân quyền RBAC (`/api/roles`, `/api/permissions`)
- `GET /api/roles`: Lấy danh sách các vai trò kèm permissions được gán.
- `GET /api/roles/{id}`: Chi tiết vai trò và permissions.
- `PUT /api/roles/{id}/permissions`: Cập nhật danh sách quyền cho vai trò (payload: `{ "permissionIds": [1, 2, 4, 10] }`), tự động ghi nhận vào Audit Log.
- `GET /api/permissions`: Danh mục toàn bộ quyền hạn trong hệ thống.

### C. Đăng ký & Gia hạn Gói tập Thành viên (`/api/members/{memberId}/subscriptions`)
- Hỗ trợ cả 2 alias endpoint:
  - `/api/members/{memberId}/subscriptions`
  - `/api/members/{memberId}/packages`
  - `/api/subscriptions` & `/api/member-packages`
- **Quy tắc tính toán ngày hiệu lực tự động khi gia hạn (`isRenewal: true`):**
  - Nếu học viên đang có gói cùng loại đang còn hạn (`status = 'Active'` và `endDate >= hôm nay`): ngày bắt đầu gói mới = `activeEndDate + 1 ngày`.
  - Nếu học viên chưa có gói hoặc gói đã hết hạn: ngày bắt đầu gói mới = `hôm nay`.
  - Ngày kết thúc = `startDate + durationDays`.
- Tự động sinh bản ghi hóa đơn (`invoices`) với phương thức thanh toán chỉ định (`Cash`, `BankTransfer`, `CreditCard`, `EWallet`) và trạng thái `Paid`.
- Khi Lễ tân đăng ký học viên mới tại quầy, hệ thống tự động gán `registered_by = currentReceptionistId`.

## 4. Giao diện Frontend mới được tích hợp
1. **Quản trị người dùng (`AdminUserManagement.jsx`):**
   - Lọc người dùng theo vai trò, tìm kiếm tên/email/sđt.
   - Thao tác: Tạo mới người dùng, Cập nhật thông tin, Khóa/Mở khóa tài khoản, Đổi vai trò nhanh.
2. **Quản trị RBAC Matrix (`AdminRBACView.jsx`):**
   - Chọn vai trò để xem ma trận quyền hạn chia theo nhóm chức năng (Người dùng, RBAC, Lớp học, Gói tập, Lễ tân & Điểm danh, Báo cáo & Audit).
   - Tick / Untick quyền hạn và Lưu trực tiếp qua API.
3. **Modal Đăng ký & Gia hạn gói tập (`SubscriptionModal.jsx`):**
   - Tích hợp tại màn hình Lễ tân (`ReceptionMemberSearch.jsx`).
   - Tự động hiển thị thời hạn mới, tính toán ngày hiệu lực, chọn hình thức thanh toán và lưu ghi chú.
4. **Trang đăng nhập (`LoginView.jsx`):**
   - Thêm nút Quick Demo cho cả 5 vai trò (Admin, Center Manager, Lễ tân, Coach, Member).


---

# Bàn giao triển khai Đợt 3: Module Lịch học Thực tế (/api/sessions), Thuật toán Kiểm tra Trùng Phòng/HLV & Đăng ký Lớp học (Enrollment)

## 1. Trạng thái Đợt 3
- **Backend (Spring Boot 3 + Java 17 + Spring Security + JPA):** Hoàn tất toàn bộ entity, repository, service, controller và thuật toán kiểm tra xung đột. Biên dịch và test thành công (`11/11 tests passed - BUILD SUCCESS`).
- **Frontend (React + Vite + Tailwind CSS):** Đã tích hợp API adapter, context, modal xếp lịch `ClassScheduleModal.jsx`, nâng cấp màn hình Lớp học, Lịch dạy Huấn luyện viên, Danh mục lớp học Hội viên và Đăng ký tại quầy Lễ tân. Build thành công (`dist/` build cleanly).

---

## 2. Chi tiết Endpoints Backend & Contract API Đợt 3

### A. Module Lịch học Thực tế (`/api/sessions`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/sessions` | Authenticated | Lấy danh sách buổi học thực tế (hỗ trợ filter: `classId`, `coachId`, `roomId`, `memberId`, `startDate`, `endDate`, `status`). |
| `GET` | `/api/sessions/{id}` | Authenticated | Chi tiết thông tin 1 buổi học. |
| `POST` | `/api/sessions/check-conflict` | Authenticated | **API Thuật toán kiểm tra trùng lịch**: Kiểm tra xung đột phòng hoặc HLV trước khi lưu, trả về chi tiết va chạm. |
| `POST` | `/api/sessions` | `MANAGE_CLASSES` | Tạo 1 buổi học mới (tự động chạy thuật toán kiểm tra trùng phòng và trùng HLV). |
| `POST` | `/api/sessions/generate` | `MANAGE_CLASSES` | **Xếp lịch định kỳ tự động**: Tự động sinh danh sách buổi học theo khoảng ngày (`startDate` - `endDate`) và các thứ trong tuần (`daysOfWeek`), kiểm tra xung đột toàn diện. |
| `PUT` | `/api/sessions/{id}` | `MANAGE_CLASSES` | Cập nhật thông tin buổi học (ngày, giờ, phòng), kiểm tra loại trừ chính buổi đang sửa. |
| `PATCH` | `/api/sessions/{id}/status` | Authenticated | Cập nhật trạng thái buổi học (`Scheduled`, `Completed`, `Cancelled`). |
| `DELETE` | `/api/sessions/{id}` | `MANAGE_CLASSES` | Xóa buổi học. |

### B. Module Đăng ký Lớp học (`/api/enrollments`, `/api/classes/{id}/enroll`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/enrollments` | Authenticated | Danh sách lượt đăng ký lớp (hỗ trợ filter: `classId`, `memberId`, `status`). |
| `GET` | `/api/classes/{classId}/enrollments` | Authenticated | Danh sách học viên đã đăng ký vào một lớp cụ thể. |
| `GET` | `/api/members/{memberId}/classes` | Authenticated | Danh sách lớp học mà học viên đã đăng ký tham gia. |
| `POST` | `/api/classes/{classId}/enroll` | Authenticated | Học viên tự đăng ký hoặc Lễ tân/Quản lý đăng ký thay cho học viên (`memberId`). |
| `POST` | `/api/classes/{classId}/cancel-enrollment` | Authenticated | Hủy đăng ký lớp học cho học viên. |
| `PATCH` | `/api/enrollments/{id}/cancel` | Authenticated | Hủy bản ghi đăng ký theo ID. |

---

## 3. Thuật toán Kiểm tra Trùng Phòng & Huấn luyện viên (Conflict Detection)

Thuật toán tự động áp dụng công thức giao thoa khoảng thời gian mở trên cùng một ngày (`sessionDate`):
$$\text{Overlap} \iff (\text{startTime}_{\text{new}} < \text{endTime}_{\text{existing}}) \land (\text{endTime}_{\text{new}} > \text{startTime}_{\text{existing}})$$

### Quy tắc kiểm tra Trùng Phòng (Room Conflict):
1. **Kiểm tra trạng thái phòng:** Phòng phải ở trạng thái `Available`. Nếu phòng đang `Maintenance` hoặc `Closed`, hệ thống từ chối xếp lịch.
2. **Kiểm tra sức chứa:** Sức chứa tối đa của lớp (`class.maxCapacity`) không được vượt quá sức chứa vật lý của phòng (`room.capacity`).
3. **Kiểm tra va chạm lịch:** Không được có bất kỳ buổi học nào khác (`status <> 'Cancelled'`) trong cùng phòng tại cùng khung giờ giao thoa.

### Quy tắc kiểm tra Trùng Huấn luyện viên (Coach Conflict):
1. Xác định Huấn luyện viên được phân công phụ trách lớp học.
2. Nếu lớp đã có HLV: Kiểm tra xem HLV đó có đang dạy bất kỳ lớp học nào khác trong cùng khung giờ giao thoa vào ngày đó hay không.
3. Nếu phát hiện trùng, hệ thống trả về thông báo lỗi chi tiết chỉ rõ tên lớp và phòng mà HLV đó đang phụ trách.

---

## 4. Quy tắc Nghiệp vụ Đăng ký Lớp học (Enrollment Guards)

Khi một học viên đăng ký vào lớp (trực tuyến hoặc qua quầy Lễ tân), hệ thống tự động kiểm tra 5 điều kiện bắt buộc:
1. **Tài khoản Học viên hợp lệ:** Học viên phải tồn tại và có trạng thái `Active`.
2. **Gói tập còn hiệu lực:** Học viên phải sở hữu ít nhất một gói thành viên (`member_packages`) đang ở trạng thái `Active` và có ngày kết thúc `endDate >= ngày hiện tại`.
3. **Trạng thái Lớp học:** Lớp học phải có trạng thái `Open` hoặc `Ongoing` (không cho phép đăng ký vào lớp `Closed` hoặc `Cancelled`).
4. **Kiểm soát Sức chứa (Capacity limit):** Số lượng học viên đã đăng ký hiện tại (`enrolledCount`) phải nhỏ hơn `maxCapacity` của lớp. Nếu lớp đã đủ học viên, từ chối với thông báo *"Class is full"*.
5. **Chống trùng lặp:** Không cho phép một học viên đăng ký nhiều lần vào cùng một lớp. Nếu trước đó đã từng hủy (`Cancelled`), hệ thống sẽ tái kích hoạt lại trạng thái `Registered`.
6. **Audit Log:** Mọi hành động đăng ký (`ENROLL_CLASS`) và hủy đăng ký (`CANCEL_ENROLLMENT`) đều được ghi nhận tự động vào `system_logs`.

---

## 5. Các Màn hình Frontend đã nâng cấp & Tích hợp

1. **Modal Xếp lịch Lớp học (`ClassScheduleModal.jsx`):**
   - Tích hợp 3 tab: **Danh sách buổi tập**, **Thêm 1 buổi** (có nút *Kiểm tra trùng phòng/HLV* theo thời gian thực), và **Xếp lịch định kỳ tự động** (chọn thứ trong tuần, khoảng ngày).
2. **Màn hình Quản lý Lớp học (`ClassManagement.jsx`):**
   - Thêm nút *Lịch buổi học* trên từng thẻ lớp học.
   - Hiển thị số lượng buổi tập thực tế và tỷ lệ đăng ký trực tiếp (`enrolledCount/capacity`).
3. **Lịch dạy Huấn luyện viên (`CoachScheduleView.jsx`):**
   - Đọc dữ liệu từ `/api/sessions`, hiển thị đầy đủ ngày giờ, phòng tập, tên lớp, số học viên và nút *Hoàn tất & Điểm danh*.
4. **Danh mục Lớp học Hội viên (`MemberClassCatalog.jsx`):**
   - Đăng ký tham gia trực tuyến qua `/api/classes/{id}/enroll`.
   - Hiển thị trạng thái *"Đã đăng ký lớp này"* kèm nút *Hủy đăng ký lớp*, tự động khóa nút nếu lớp đã hết chỗ.
5. **Đăng ký Lớp tại Quầy Lễ tân (`ReceptionClassBooking.jsx`):**
   - Cho phép Lễ tân chọn hội viên, kiểm tra trạng thái gói tập và đăng ký / hủy lớp thay hội viên tức thì.

---

# Bàn giao triển khai Đợt 4: Module Hóa đơn, Thanh toán (/api/invoices) & Báo cáo Thống kê Nâng cao (/api/reports)

## 1. Trạng thái Đợt 4
- **Backend (Spring Boot 3 + Java 17 + JPA + Spring Security):** Hoàn tất toàn bộ entity `Invoice`, DTOs, Repository, Service và Controller cho hóa đơn/thanh toán và báo cáo doanh thu/hội viên theo khoảng thời gian. Đã bổ sung bộ test tự động `InvoiceServiceTest`.
- **Frontend (React + Vite + Tailwind CSS):** Đã nâng cấp `mappers.js`, `managerApi.js`, `SCMSContext.jsx`, `PaymentManagement.jsx`, `ReceptionPaymentsView.jsx`, `PaymentModal.jsx`, `ReportsView.jsx`.

---

## 2. Chi tiết Endpoints Backend & Contract API Đợt 4

### A. Module Hóa đơn & Thanh toán (`/api/invoices`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/invoices` | Authenticated | Danh sách hóa đơn (hỗ trợ filter: `memberId`, `status`, `method`, `startDate`, `endDate`). |
| `GET` | `/api/invoices/{id}` | Authenticated | Lấy chi tiết 1 hóa đơn theo ID. |
| `POST` | `/api/invoices` | `COLLECT_PAYMENTS` / `MANAGE_PAYMENTS` | Tạo mới hóa đơn thanh toán tại quầy hoặc hệ thống. |
| `POST` | `/api/invoices/{id}/pay` | `COLLECT_PAYMENTS` / `MANAGE_PAYMENTS` | Thực hiện thanh toán hóa đơn (`Paid`), cập nhật phương thức thanh toán, mã đối soát và ghi nhận doanh thu. |
| `PATCH` | `/api/invoices/{id}/status` | `MANAGE_PAYMENTS` | Cập nhật trạng thái hóa đơn (`Pending`, `Paid`, `Failed`, `Cancelled`). |

### B. Module Báo cáo Thống kê Nâng cao (`/api/reports`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/reports/dashboard` | `VIEW_REPORTS` | Lấy dữ liệu tổng quan: tổng hội viên, lớp đang mở, HLV, doanh thu lũy kế, số HĐ đã thanh toán, gói đang hoạt động, số buổi học hôm nay. |
| `GET` | `/api/reports/revenue` | `VIEW_REPORTS` | Báo cáo doanh thu theo khoảng thời gian (`startDate`, `endDate`), phân bổ theo gói (`byPackage`), phương thức (`byPaymentMethod`), và biểu đồ chuỗi thời gian (`timeline` theo `groupBy=day` hoặc `month`). |
| `GET` | `/api/reports/members` | `VIEW_REPORTS` | Báo cáo tăng trưởng hội viên theo khoảng thời gian (`startDate`, `endDate`), số lượng gói `active` vs `expired`, chuỗi tăng trưởng hội viên mới (`growth`). |

---

## 3. Quy tắc Nghiệp vụ Hóa đơn & Thanh toán
1. **Tự động sinh Hóa đơn khi Đăng ký/Gia hạn Gói:** Khi Lễ tân hoặc Hội viên đăng ký hoặc gia hạn gói tập qua `MemberPackageService`, hệ thống tự động tạo bản ghi hóa đơn tương ứng với trạng thái `Paid` (nếu đã thanh toán) hoặc `Pending`.
2. **Kích hoạt Gói tập:** Khi hóa đơn được thanh toán thành công qua `/api/invoices/{id}/pay`, nếu hóa đơn có gắn `packageId`, hệ thống tự động kiểm tra và kích hoạt gói thành viên của hội viên đó.
3. **Audit Log:** Mọi hành vi tạo hóa đơn (`CREATE_INVOICE`), thu tiền (`COLLECT_PAYMENT`), và đổi trạng thái (`UPDATE_INVOICE_STATUS`) đều được ghi log với chi tiết số tiền và người thực hiện.

---

# Bàn giao triển khai Đợt 5: Module Kế hoạch Tập luyện, Kết quả, Điểm danh & Đánh giá Huấn luyện viên

## 1. Trạng thái Đợt 5
- **Backend (Spring Boot 3 + Java 17 + JPA + Spring Security):** Hoàn tất toàn bộ 4 sub-module:
  - `TrainingPlan`: Kế hoạch tập luyện được thiết kế bởi Huấn luyện viên.
  - `TrainingResult`: Ghi nhận kết quả buổi tập và điểm danh gắn với từng buổi học (`sessionId`).
  - `Attendance`: Điểm danh buổi học và hỗ trợ Check-in / Check-out tự do tại quầy.
  - `Evaluation`: Đánh giá định kỳ quá trình tập luyện của học viên.
- **Unit Tests:** `TrainingResultServiceTest`, `AttendanceServiceTest` chạy đạt 100%.
- **Frontend:** Nâng cấp `CoachAttendanceView.jsx`, `AttendanceCorrectionModal.jsx`, `CoachTrainingProgress.jsx`, `CoachAssignedMembersView.jsx`, `ReceptionMemberSearch.jsx`.

---

## 2. Chi tiết Endpoints Backend & Contract API Đợt 5

### A. Module Kế hoạch Tập luyện (`/api/training-plans`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/training-plans` | Authenticated | Danh sách kế hoạch (filter: `coachId`, `memberId`, `classId`). |
| `GET` | `/api/training-plans/{id}` | Authenticated | Lấy chi tiết kế hoạch tập luyện. |
| `POST` | `/api/training-plans` | `MANAGE_CLASSES` / `ATTENDANCE_CHECKIN` | Tạo mới kế hoạch tập luyện (mục tiêu, danh sách bài tập, thời gian). |
| `PUT` | `/api/training-plans/{id}` | `MANAGE_CLASSES` / `ATTENDANCE_CHECKIN` | Cập nhật mục tiêu, nội dung hoặc tiến độ kế hoạch. |
| `DELETE` | `/api/training-plans/{id}` | `MANAGE_CLASSES` | Xóa kế hoạch tập luyện. |

### B. Module Kết quả Buổi học (`/api/training-results`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/training-results` | Authenticated | Danh sách kết quả buổi tập (filter: `sessionId`, `memberId`, `coachId`). |
| `POST` | `/api/training-results` | `ATTENDANCE_CHECKIN` | Huấn luyện viên điểm danh buổi học và nhận xét kết quả (`Present`, `Late`, `Absent`). Tự động đồng bộ sang bảng `attendances`. |
| `PUT` | `/api/training-results/{id}` | `ATTENDANCE_CHECKIN` | Cập nhật nhận xét hoặc kết quả buổi tập. |

### C. Module Điểm danh & Check-in / Check-out (`/api/attendances`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/attendances` | Authenticated | Danh sách điểm danh / check-in (filter: `sessionId`, `memberId`, `state`, `date`). |
| `GET` | `/api/attendances/{id}` | Authenticated | Lấy chi tiết bản ghi điểm danh. |
| `POST` | `/api/attendances/check-in` | `ATTENDANCE_CHECKIN` | Check-in tại quầy cho hội viên (`state=CheckedIn`, ghi nhận `checkInTime`). |
| `POST` | `/api/attendances/check-out` | `ATTENDANCE_CHECKIN` | Check-out tại quầy cho hội viên (`state=CheckedOut`, ghi nhận `checkOutTime`). |
| `POST` | `/api/attendances/correct` | `ATTENDANCE_CHECKIN` / `MANAGE_CLASSES` | **Điều chỉnh điểm danh**: Chỉnh sửa trạng thái điểm danh bắt buộc phải có `reason` và tự động ghi Audit Log với hành động `ATTENDANCE_CORRECTION`. |

### D. Module Đánh giá Định kỳ (`/api/evaluations`)
| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/evaluations` | Authenticated | Danh sách đánh giá (filter: `memberId`, `coachId`). |
| `GET` | `/api/evaluations/{id}` | Authenticated | Chi tiết 1 bản ghi đánh giá. |
| `POST` | `/api/evaluations` | `ATTENDANCE_CHECKIN` / `MANAGE_CLASSES` | Huấn luyện viên tạo đánh giá định kỳ cho học viên kèm nhận xét và thang điểm (`progressScore`). |
| `PUT` | `/api/evaluations/{id}` | `ATTENDANCE_CHECKIN` / `MANAGE_CLASSES` | Cập nhật đánh giá định kỳ. |
| `DELETE` | `/api/evaluations/{id}` | `MANAGE_CLASSES` | Xóa đánh giá định kỳ. |

---

## 3. Quy tắc Nghiệp vụ Điểm danh & Audit Correction
1. **Đồng bộ Kết quả Điểm danh:** Khi HLV điểm danh qua `/api/training-results`, hệ thống tự động đồng bộ một bản ghi tương ứng vào bảng `attendances` để phục vụ tra cứu lịch sử tập luyện toàn diện của học viên.
2. **Quy tắc Bắt buộc Lý do Điều chỉnh (`Attendance Correction`):** Khi thay đổi trạng thái điểm danh (từ `Absent` sang `Present` hoặc `Late`), người dùng bắt buộc phải gửi trường `reason`. Nếu thiếu `reason`, API trả về mã lỗi `400 Bad Request`. Hệ thống lập tức ghi một bản ghi Audit Log với `action=ATTENDANCE_CORRECTION` gồm thông tin HLV thực hiện, giá trị cũ, giá trị mới và lý do giải trình.

---

## 4. Kết quả Kiểm thử Toàn diện (Verification Summary)
- **Backend Unit & Integration Tests:**
  - `mvnw.cmd test`: **23/23 tests passed** (100% SUCCESS, 0 failures, 0 errors).
  - Bao gồm: `LegacyPasswordEncoderTest` (2), `EnrollmentServiceTest` (5), `InvoiceServiceTest` (4), `ReportServiceTest` (2), `SessionServiceTest` (5), `AttendanceServiceTest` (3), `TrainingResultServiceTest` (2).
- **Frontend Production Build:**
  - `npm run build`: **Vite build SUCCESS** (0 errors, 0 linting issues, bundle output sẵn sàng tại `dist/`).

# Cập nhật thanh toán Membership + học phí lớp (2026-10-10)

- Thanh toán hội viên hiện tạo `MemberPackage` Pending cùng invoice Pending; tiền chỉ lấy từ giá package trong DB. Cash cần Receptionist có `MANAGE_INVOICES` xác nhận; VNPay chỉ được xác nhận qua callback IPN/return có chữ ký hợp lệ.
- Thêm mã hóa đơn, thời gian hết hạn 15 phút có cấu hình, job quét mỗi phút, idempotency callback và các endpoint tự phục vụ cho member. Hóa đơn và gói được cập nhật trong cùng transaction khi Paid.
- Mở rộng `classes.tuition_fee` và invoices hỗ trợ class_id. Lớp có học phí giữ chỗ bằng enrollment Pending; chỗ Pending được tính vào capacity và được nhả khi hóa đơn hết hạn.
- Chạy `migration_payment.sql` trên PostgreSQL trước khi khởi động ứng dụng; không tự chạy lên DB. Schema mới được mô tả trong `schema_postgres.sql`.
- Kiểm tra ngày 2026-10-10: `mvnw.cmd test` — 26 tests passed.
- Chính sách chỗ lớp VNPay theo yêu cầu: khi invoice hết hạn vẫn giữ enrollment Pending làm chỗ dự trữ cho tới khi callback đã xác minh; callback thành công kích hoạt, callback thất bại nhả chỗ. Chủ động hủy enrollment sẽ hủy invoice liên quan.
- Kết quả kiểm thử mới nhất sau bổ sung thanh toán: `mvnw.cmd test` — 32 tests passed, 0 failures/errors (thay cho số liệu kiểm thử cũ ở các phần bàn giao trước).
