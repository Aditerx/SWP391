# Sports Center Management System — Backend

Java17, Spring Boot3.3.5, Maven Wrapper; PostgreSQL/Supabase là môi trường hiện hành. SQLServer chưa được nghiệm thu runtime; `FE_Project-main` chỉ là tham khảo cũ.

## Mở session

Đọc [AGENTS](AGENTS.md) → [Active](docs/00-MASTER-CHECKLIST.md#active) → đúng bản bàn giao/Dxx/phần API của nhiệm vụ. Bản gửi FE hiện tại được dẫn ở Active. Không đọc toàn bộ QA, reference hoặc archive khi bắt đầu.

| Cần tra | Nguồn duy nhất |
|---|---|
| Trạng thái, việc còn mở và bước tiếp | [docs00](docs/00-MASTER-CHECKLIST.md) |
| API/field/state/error | [docs01](docs/01-API-CONTRACT-FE-BE.md), [OpenAPI](artifacts/fe-handoff/openapi.json) |
| Chạy/test/deploy/recovery | [docs02](docs/02-DEPLOYMENT-GUIDE.md) |
| Quyết định nghiệp vụ | [D01–D19](docs/04-DEBUG-NOTE.md) |
| Script DB và phạm vi xóa/tạo | [database/README](database/README.md) |
| Yêu cầu/bàn giao chi tiết còn hiệu lực | `docs/reference/`, chỉ mở mục được dẫn |
| Checkpoint/bản đã thay thế | `docs/archive/YYYY-MM/` hoặc Git, không dùng làm trạng thái hiện hành |

## Chạy và kiểm local

Thiết lập Java17 và biến môi trường theo docs02, rồi chạy `.\mvnw.cmd spring-boot:run`. Swagger local: `http://localhost:8080/swagger-ui/index.html`. Auth dùng cookie `JSESSIONID`; kiểm `GET /api/auth/me` trước nhóm kiểm có phiên.

Không commit credential hoặc backup. Runner nghiệp vụ dùng bản sao không.env/fixture local riêng; H2 không thay PostgreSQL/locking, Render hoặc browser. Normal theo lớp, Advanced theo môn/Primary Coach; Paid hợp lệ mới cấp quyền. Rule hiện hành theo Dxx.

Sau mỗi đợt, cập nhật cùng dòng việc trong Active; đóng đúng phạm vi, dẫn bằng chứng và ghi bước tiếp. Không sinh file theo từng cuộc trao đổi. Quy tắc duy trì và bàn giao ở [AGENTS mục5](AGENTS.md#5-kỷ-luật-tài-liệu-và-context).
