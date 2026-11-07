# Sports Center Management System — Backend

Java17, Spring Boot3.3.5, Maven Wrapper; PostgreSQL/Supabase là môi trường hiện hành. SQLServer có script lịch sử nhưng runtime chưa được nghiệm thu. Source FE cũ trong repo chỉ dùng tham khảo khi chưa xác định bản FE đang tích hợp.

## Bắt đầu

Đọc [AGENTS](AGENTS.md) → [Active](docs/00-MASTER-CHECKLIST.md#active) → đúng quyết định trong [D01–D19](docs/04-DEBUG-NOTE.md). Contract bàn giao FE ở [docs01](docs/01-API-CONTRACT-FE-BE.md) và [một snapshot OpenAPI](artifacts/fe-handoff/openapi.json); cấu hình/test/deploy ở [docs02](docs/02-DEPLOYMENT-GUIDE.md).

Chọn script DB theo [database/README](database/README.md): schema root bảo toàn dữ liệu; bản `database/schema_postgres.sql` dựng lại SCMS. DB đang có dữ liệu chỉ nâng cấp bằng migration đúng dependency, có guard; không chạy reset/seed để sửa lỗi quyền. SQL16 đã được Owner xác nhận trên Supabase, không chạy lại để cố tạo bộ đếm xóa khác0. Migration Google cần xác minh riêng theo docs02 trước deploy.

## Chạy và kiểm local

Thiết lập Java17 và các biến môi trường theo docs02. Không commit `.env`, credential hoặc backup. Ứng dụng có thể đọc `.env` tại thư mục chạy; các runner kiểm nghiệp vụ dùng bản sao không có file này và fixture local riêng.

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger local: `http://localhost:8080/swagger-ui/index.html`. Auth bằng cookie `JSESSIONID`, không Basic Auth/Bearer. Sau login phải kiểm `GET /api/auth/me` trước kiểm role/owner/center. Không dùng credential demo lịch sử để thử production.

Kiểm BE theo [quy trình test](docs/02-DEPLOYMENT-GUIDE.md#testing); hoàn tất một nhóm sửa chạy `clean test` và kiểm liên quan trong môi trường tách biệt. H2 không thay kiểm PostgreSQL/locking, Render hoặc FE browser.

## Nguồn hiện hành và lịch sử

Normal là khóa theo lớp; Advanced là quyền học theo môn/Primary Coach. Không còn bán gói hội viên chung tháng/VIP; Paid hợp lệ mới cấp quyền. Rule12h/45ngày sử dụng/attendance/sự cố theo Dxx, không lấy24h/15phút hoặc API gói cũ từ tài liệu lịch sử.

[Checklist và review thiết kế](docs/03-TODO.md) ghi phần còn mở. Các bản bàn giao có ngày chỉ mô tả đúng luồng/thời điểm của bản đó. `FE-BE.md`, `PROJECT_CONTEXT.md`, `note.md`, `DOCS_RESTRUCTURE_PROPOSAL.md`, `HANDOFF-T.md` đã đánh dấu lịch sử; chúng không thay contract, quyết định hoặc trạng thái hiện hành.

Các PDF yêu cầu gốc được giữ để truy nguồn; khi có rule khác Dxx hiện hành, đọc phần [khác biệt đã được thay thế](docs/03-TODO.md#design-review-20261106), không khôi phục policy cũ từ PDF hoặc checklist lưu trữ.
