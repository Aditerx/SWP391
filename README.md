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

## Swagger

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Gọi `POST /api/auth/login` với JSON:

```json
{"email":"manager@scms.com","password":"12345678"}
```

Session cookie được giữ trong trình duyệt/Swagger; không dùng Basic Auth nữa.

Nếu login trả `400 email: must be a well-formed email address`, hãy dùng đúng
JSON ở trên (email phải là `manager@scms.com`, không phải username `manager`).
Sau khi login thành công, giữ nguyên tab Swagger để cookie session được gửi cho
các request `GET`/`POST` tiếp theo.

## Demo endpoints

- `GET/POST/PUT/DELETE /api/subjects`
- `GET/POST/PUT/PATCH /api/rooms`
- `GET/POST/PUT/PATCH /api/classes`
- `GET/POST/PUT/PATCH /api/packages`
- `GET /api/reports/dashboard`
- `GET /api/audit-logs`

Hibernate không tạo hoặc drop database (`ddl-auto=none`). Xem kế hoạch tích hợp
FE và các mapping flag trong [README_BE_CHANGES_2609.md](README_BE_CHANGES_2609.md).
