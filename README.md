# Sports Center Management System - Backend MVP

Minimal Spring Boot API for a Center Manager demo.

## Requirements

- Java 17
- Maven
- SQL Server Express with database `gym_management_system`

`TCP/IP` must be enabled for the `SQLEXPRESS` instance, then the SQL Server service must be restarted.

## Run

Windows (không cần cài Maven toàn hệ thống):

```powershell
.\mvnw.cmd spring-boot:run
```

Hoặc nếu Maven đã có trong `PATH`:

```bash
mvn spring-boot:run
```

## Swagger

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Click **Authorize** and use HTTP Basic:

- Username: `manager`
- Password: `Passw0rd!`

## Demo endpoints

- `GET/POST/PUT/DELETE /api/subjects`
- `GET/POST/PUT/PATCH /api/rooms`
- `GET/POST/PUT/PATCH /api/classes`
- `GET/POST/PUT/PATCH /api/packages`
- `GET /api/reports/dashboard`
- `GET /api/audit-logs`

The application uses the existing database and never creates or drops tables (`ddl-auto=none`).
