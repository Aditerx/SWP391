# Sports Center Management System — Backend MVP

Backend Spring Boot tối giản cho demo Center Manager.

## Requirements

- Java 17
- SQL Server Express (`gym_management_system`)
- Maven hoặc Maven Wrapper (`mvnw.cmd`)

## Chuẩn bị môi trường

Chạy `SQLQuery3_2609.sql` trong SSMS nếu muốn tạo lại database demo. **Script
này reset database**, hãy backup trước khi chạy.

PowerShell (thay mật khẩu bằng giá trị local của bạn, không commit):

```powershell
$env:DB_URL = 'jdbc:sqlserver://PHAT-TAI;instanceName=SQLEXPRESS;databaseName=gym_management_system;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = '<mat-khau-local>'
```

Hoặc copy `.env.example` thành `.env` rồi điền giá trị thật. Backend tự đọc
`.env` khi file tồn tại; `.env` và `application-local.properties` đã được ignore
bởi Git.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Swagger & Demo Accounts

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Đăng nhập qua `POST /api/auth/login`. Mật khẩu mặc định toàn hệ thống là **`Scms@2026`**.

```json
{"email":"manager@scms.com","password":"Scms@2026"}
```

Session cookie được giữ trong trình duyệt/Swagger; không dùng Basic Auth.

### Bảng tài khoản demo chính:
| Vai trò | Email đăng nhập | Mật khẩu |
|---|---|---|
| **Center Manager** | `manager@scms.com` | `Scms@2026` |
| **Lễ tân (Receptionist)** | `receptionist@scms.com` | `Scms@2026` |
| **Coach đại diện** | `coach@scms.com` | `Scms@2026` |
| **Member (12 Credit / 45 ngày)** | `member@scms.com` | `Scms@2026` |
| **Admin** | `admin@scms.com` | `Scms@2026` |

*Chi tiết danh sách HLV 9 môn thể thao và các kịch bản hội viên xem tại [docs/01-API-CONTRACT-FE-BE.md](docs/01-API-CONTRACT-FE-BE.md).*

## Demo endpoints

- `GET/POST/PUT/DELETE /api/subjects`
- `GET/POST/PUT/PATCH /api/rooms`
- `GET/POST/PUT/PATCH /api/classes`
- `GET/POST/PUT/PATCH /api/packages`
- `GET /api/reports/dashboard`
- `GET /api/audit-logs`

Hibernate không tạo hoặc drop database (`ddl-auto=none`). Xem kế hoạch tích hợp
FE và các mapping flag trong [README_BE_CHANGES_2609.md](README_BE_CHANGES_2609.md).
