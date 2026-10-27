# SCMS — Tiến độ và nghiệm thu

## Active

**Phạm vi đã giao:** P1.0–P1.4 và BE P2. P1.5/gói T/D01/D09/FE integration vẫn để lại theo Owner. Không coi merge OTP/chat/notification là hoàn tất gói T.

| Việc còn mở | Trạng thái | Điều kiện tiếp tục |
|---|---|---|
| Kiểm API có phiên, owner/role/center trên Render | NOT_RUN đầy đủ | Tài khoản thử đúng vai trò, fixture và phạm vi thao tác xác định; kiểm `/auth/me` trước |
| OTP/email cloud | BLOCKED ở cấu hình hiện tại | Owner xác nhận Render Free; chọn HTTPS email provider/key/sender và adapter, rồi kiểm delivery |
| D01 hồ sơ Member; D09 Coach directory | BACKLOG theo scope | Chỉ thực hiện khi nhận scope; D09 chưa có positive authenticated proof hết lỗi lazy |
| FE browser/CORS/cookie/camera QR | NOT_RUN | Nguồn FE hiện hành và môi trường tích hợp được xác định |
| VNPay gateway sandbox live | NOT_RUN | Merchant/callback/fixture và quyền kiểm phù hợp; callback ký local không thay gateway |
| SQLServer runtime | NOT_RUN | Môi trường kiểm riêng nếu được chọn |

**Bước kế tiếp đề nghị:** chuẩn bị matrix nghiệm thu có phiên theo rủi ro: tiền/quyền, owner/center, lịch/credit và biên thời gian. Kiểm local PostgreSQL trước theo D11, sau đó smoke Render trong phạm vi được thống nhất. Không lấy 185 route làm mục tiêu phải gọi mọi mutation lên production.

Đọc: [AGENTS](../AGENTS.md) → [quyết định Dxx](04-DEBUG-NOTE.md) → [contract FE](01-API-CONTRACT-FE-BE.md) đúng luồng; [vận hành](02-DEPLOYMENT-GUIDE.md) khi cần.

## Nghiệm thu đã ghi nhận — 27/10/2026

Đây là kết quả lưu từ lần kiểm trước, **không phải lần chạy mới trong đợt dọn tài liệu**. App source: `693f732`; Live đã quan sát: `4d372e5`. SHA hiện tại lấy từ Git và runtime khi bắt đầu công việc mới.

| Phạm vi | Kết quả lưu | Giới hạn |
|---|---|---|
| BE regression | 305run:278pass/27skip/0lỗi | Skip có điều kiện, không phải pass |
| PostgreSQL HTTP | 10/10:9 workflow nghiệp vụ+1OpenAPI regression | Fixture và Clock local, không cloud mutation |
| Contract | 149 exchange/36 operations;114positive/201body checks không lệch cơ bản | Không phải validator đầy đủ nullable/format/required/range hoặc mọi nhánh lỗi |
| Source/OpenAPI | 185 operations/149paths/138schemas; route parser khớp | Có aliases; không chứng minh mọi handler/nghiệp vụ đã chạy |
| GET Render | 3public200,71protected401 | Chỉ public/login gate; authenticated business matrix chưa chạy |
| Health Render | db UP,mail DOWN | `/api/health` chỉ liveness/identity; aggregatehealth503 |
| Production upgrade | Bundle01e31ca6… đã commit,50 bảng/9 môn,giữ dữ liệu gốc7bảng | Quyền thực thi chỉ bundle cụ thể; không suy quyền seed/test giao dịch |

Bằng chứng khi cần: [QA duy nhất](../artifacts/qa/latest.json). Bộ FE chỉ gồm [contract](01-API-CONTRACT-FE-BE.md) và [OpenAPI](../artifacts/fe-handoff/openapi.json). Raw run cuối còn ở `artifacts/runs/phases/2026-10-27-verified` trên máy này, ignored và không mặc định có trên máy khác.

27skip của lần clean gồm19baseline live/data/PG,6communication và2clone opt-in. Communication12case và clone2case đã chạy riêng ở source trước; xem QA theo SHA, không gọi đó là lần chạy mới của source hiện tại.

## HISTORY

| Ngày | Nhóm hoàn tất | Dấu vết |
|---|---|---|
| 27/10/2026 | Chuẩn hóa bàn giao: tách trách nhiệm5docs, FE1contract+1 schema, gom QA; bỏ inventory/schema MD và trace trùng | Không sửa BE/DB/deploy; kết quả kiểm cũ giữ theo SHA |
| 27/10/2026 | Audit và sửa metadata Swagger:24status201/204,6 schema name riêng,LocalTime string; thêm readback/contract regression | App693f732/Live4d372e5; QA |
| 27/10/2026 | Nâng cấp đúng production188f6765… sau backup mã hóa/restore proof và phê duyệt riêng | Bundle production giữ nguyên hash; docs02/QA |
| 27/10/2026 | Hoàn tất hai phase BE; merge communication, sửa CI và runtime identity | Các lần test có phạm vi/SHA riêng trong QA và Git |

Chi tiết phân tích, log, snapshot before/after và checklist cũ nằm trong Git tại `a4f3cd0` (trước chuẩn hóa). Ví dụ `git show a4f3cd0:docs/00-MASTER-CHECKLIST.md`. Chỉ tra khi cần một sự kiện cụ thể; không đọc mặc định hoặc khôi phục nguyên trạng vào docs active.
