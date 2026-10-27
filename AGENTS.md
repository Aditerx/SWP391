# AGENTS.md — Nguyên tắc làm việc SCMS

## 1. Bắt đầu session

1. Đọc file này và **phần Active** của [docs/00-MASTER-CHECKLIST.md](docs/00-MASTER-CHECKLIST.md).
2. Đọc bảng quyết định trong [docs/04-DEBUG-NOTE.md](docs/04-DEBUG-NOTE.md), rồi đúng Dxx thuộc nhiệm vụ.
3. Chỉ mở phần API liên quan trong docs01; docs02 khi cần test/môi trường/deploy.
4. Kiểm git status/diff và source thực tế trước khi sửa. Không đọc hàng loạt artifacts/log/history hoặc old-docs.

Mỗi nguồn có vai trò riêng: quyết định Owner mô tả điều phải làm; contract mô tả giao tiếp đã công bố; code mô tả implementation; test/log là quan sát trong một môi trường. Không mặc định nguồn nào đúng100%. Khi chúng lệch nhau, xác định nguyên nhân trước; hỏi Owner nếu phát sinh lựa chọn nghiệp vụ mới. Không hỏi lại quyết định đã chốt chỉ vì code hoặc tài liệu lịch sử chưa cập nhật.

## 2. Môi trường

- Windows/PowerShell; dùng đường dẫn `/` hoặc `\\`.
- Java 17, Spring Boot 3.3.5, Maven wrapper `.\mvnw.cmd`.
- PostgreSQL/Supabase là môi trường đang dùng; SQLServer có scripts nhưng runtime chưa được nghiệm thu.
- Mọi thời điểm nghiệp vụ dùng Spring bean `Clock`, `Asia/Ho_Chi_Minh`. Không dùng `LocalDateTime.now()` thiếu Clock.
- FE_Project-main là bản tham khảo cũ; nguồn FE hiện hành phải được xác định khi tích hợp.

## 3. Ràng buộc bắt buộc

- Invoice.amount/purchaseAmount đóng băng khi tạo đơn; thay bảng giá không sửa chứng từ cũ.
- Paid hợp lệ mới kích hoạt quyền. Callback kiểm chữ ký/reference/amount/idempotency; Return không ghi Paid.
- Không tin amount/role/actor/memberId từ client; kiểm Auth, owner/center, active/block, quyền học, hạn, credit, sĩ số và trùng tài nguyên.
- Ghi tranh chấp tài nguyên dùng transaction và DB lock, ví dụ findByIdForUpdate; giữ thứ tự khóa nhất quán. Retry không nhân hóa đơn, buổi, credit hoặc thông báo.
- Rule Normal/Advanced,12h,45 ngày sử dụng, attendance và ngoại lệ TT theo Dxx hiện hành trong docs04. Không khôi phục rule lịch sử24h/1080 giờ.
- Giữ code/tests chế tài khóa7ngày; default toggle tắt. Giữ lịch sử attendance và chứng từ; không sửa dữ liệu để làm kết quả đẹp.
- Migration riêng, có guard/tương thích/replay phù hợp. Không DROP TABLE/TRUNCATE/reset dữ liệu thật. Quyền mutation cloud phải đúng phạm vi đã được duyệt; phê duyệt một bundle không mở quyền seed hoặc live tests.
- .env, credential, cookie, OTP/QR secret và backup riêng tư không đưa lên Git hoặc báo cáo. Không dùng tài khoản seed lịch sử để thử production.

## 4. Kiểm thử và kết luận

- Trước sửa, xác định Dxx, điều kiện nghiệm thu và fixture. Expected dựa quyết định đã chốt; code và Swagger cùng nguồn sinh không là kiểm chứng nghiệp vụ độc lập.
- Trong lúc sửa dùng test đích; sau mỗi nhóm thay đổi BE phải chạy `.\mvnw.cmd clean test` và kiểm liên quan. Không chạy hai Maven clean trên cùng target.
- Nếu rule mới làm expected cũ sai, giải trình trước khi cập nhật. Không đổi expected chỉ để xanh.
- Test tiền/quyền/lịch cần kiểm response và trạng thái lưu sau commit; kiểm owner/role/center, biên thời gian, retry/race phù hợp rủi ro.
- 401 anonymous chỉ là cổng đăng nhập; không tính là pass nghiệp vụ. Xác minh `/api/auth/me` trước nhóm kiểm có phiên.
- PASS/FAIL/BLOCKED/NOT_RUN phải kèm phạm vi/môi trường/SHA. Skip không phải pass; count lịch sử không là quota.
- Không gọi local HTTP là gateway live, browser FE hoặc authenticated business trên Render. Chỉ chạy lại kiểm rộng khi có diff, failure hoặc nghi vấn mới.
- Chỉ thay docs/scripts bảo trì thì kiểm syntax, dữ liệu, liên kết và hành vi liên quan; không tự gán kết quả BE mới từ lần test cũ.

## 5. Kỷ luật tài liệu và context

| Nguồn | Trách nhiệm duy nhất |
|---|---|
| docs00 | Trạng thái hiện hành, phần còn mở, nghiệm thu ngắn và HISTORY tóm tắt |
| docs01 | Contract bàn giao FE theo luồng, field/state/error/ví dụ |
| docs02 | Cấu hình, test runner, migration/deploy/recovery |
| docs04 | Quyết định D01–D19 và lý do; không ghi nhật ký deploy ở đây |
| artifacts/fe-handoff/openapi.json | Một snapshot schema máy đọc cho FE |
| artifacts/qa/latest.json | Một bản bằng chứng máy đọc; mở theo câu hỏi, không nạp mặc định |

- Không thêm tài liệu nếu đã có chỗ phù hợp. Không sinh lại bản Markdown của toàn OpenAPI hoặc nhân đôi trace thành nhiều định dạng.
- SHA/count test/deploy chỉ cập nhật ở docs00 và QA, không sao chép vào mọi checkpoint.
- Tìm bằng rg, đọc heading/đoạn/symbol cần thiết. Log dài chỉ trích lỗi liên quan; không in toàn JSON/stack dump/bundle vào context.
- Báo cáo theo điều kiện nghiệm thu, không lấy số request/assertion làm thước đo chính. Một kết quả chuẩn, nơi khác liên kết tới nó.
- Hoàn thành nhóm việc: cập nhật Active, đưa tóm tắt vào HISTORY và ghi bước kế tiếp. Lịch sử đầy đủ truy qua Git khi cần, không tạo nhiều bản before/after trong gói FE.
