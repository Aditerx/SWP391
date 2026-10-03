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

## Database environment variables

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the process environment before starting the application. For PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://<host>:<port>/<database>?prepareThreshold=0"
$env:DB_USERNAME = "<database-user>"
$env:DB_PASSWORD = "<database-password>"
.\mvnw.cmd spring-boot:run
```

Spring Boot does not load `.env` files automatically. The ignored `.env` file can be used as a private source for these values, but export them into the shell before running the command.

## RBAC demo accounts

When the `permissions` table is empty, startup seeds the five roles, the default permission matrix, and these sample accounts. Every sample account has the password `Passw0rd!`:

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@sportscenter.com | Passw0rd! |
| CENTER_MANAGER | manager@sportscenter.com | Passw0rd! |
| RECEPTIONIST | receptionist@sportscenter.com | Passw0rd! |
| COACH | coach@sportscenter.com | Passw0rd! |
| MEMBER | member@sportscenter.com | Passw0rd! |

These are demonstration credentials; change or remove them before using a shared or production environment.
