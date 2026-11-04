# SCMS — Bàn giao kết quả kiểm thử API và database cho BE

Ngày tổng hợp: 31/10/2026. Tài liệu ghi những vấn đề API/DB đã có bằng chứng.

**Gửi FE đợt hiện tại:** dùng riêng [SCMS-BAN-GIAO-FE-2026-11-02.md](SCMS-BAN-GIAO-FE-2026-11-02.md). File31/10 này giữ kết quả lịch sử; không dùng danh sách lỗi/số liệu cũ làm trạng thái triển khai hiện tại.

Cập nhật02/11: BE-ABSENCE-01 đã sửa và kiểm local bằng assertion; Owner xác nhận cả hai bundle migration Supabase hoàn tất, chưa xác nhận deploy/kiểm nghiệp vụ trên Render. Theo dõi tại [Active/nghiệm thu hiện hành](docs/00-MASTER-CHECKLIST.md#active), [đối chiếu1:1 và giới hạn capture](docs/00-MASTER-CHECKLIST.md#report-trace) và [contract báo vắng](docs/01-API-CONTRACT-FE-BE.md#cancellation). Các kết quả bên dưới giữ nguyên mốc/môi trường31/10.

## Kết luận dành cho BE

**Tại thời điểm31/10 còn một điểm lệch nghiệp vụ đã tái hiện: Member báo vắng thành công nhưng chưa tự ghi attendance Absent theo yêu cầu Owner.** Các nhóm nghiệp vụ khác đã có bằng chứng kiểm tra theo phạm vi riêng; không đồng nghĩa mọi trường hợp hoặc môi trường live đều đã nghiệm thu.

Chưa có lỗi cấu trúc hoặc dữ liệu DB mới được chứng minh từ snapshot Supabase đã xuất. Không có căn cứ yêu cầu rebuild/reset DB từ kết quả đợt này.

## BE-ABSENCE-01 — Có notice, thiếu attendance Absent

- Trạng thái tại31/10: **còn mở**. Hiện hành: đã sửa/kiểm local theo docs00; chưa đóng nghiệm thu cloud, còn giới hạn capture đã ghi trong bảng1:1.
- Mức độ: **Medium — chưa đáp ứng nghiệp vụ Owner**.
- Phía cần xử lý: BE workflow báo vắng và contract/test liên quan.

Owner đã chốt: Coach quyết định Present/Absent khi điểm danh thông thường; riêng khi Member bấm **Báo vắng hợp lệ**, hệ thống tự ghi **Absent cho đúng Member tại buổi đó**.

### Bằng chứng tái hiện

1. Endpoint: `POST /api/sessions/{id}/absence`.
2. Môi trường: Spring Boot HTTP local, H2 riêng, Clock Asia/Ho_Chi_Minh. **Chưa xác nhận lại trên Render/Supabase.**
3. Lượt gần nhất: FLOW08-A `run_20261031_095524`, case CA8. Fixture ca Advanced bắt đầu 20/11/2026 07:45; Clock đặt 01:45 cùng ngày, Member chủ báo vắng với reason hợp lệ.
4. API trả 200, notice active=true.
5. SQL sau request cho đúng Member/session: notice_count=1, attendance_count=0. Session vẫn Scheduled.

Lỗi cũng đã tái hiện trong FLOW05-B `run_20261031_081151`. Đây không phải FE gọi sai endpoint. Expected: có attendance Absent, giữ notice/reason để truy vết; không đổi Session.status thành Absent, không tự hủy buổi hoặc hoàn thêm credit.

### Đề nghị BE chỉnh và kiểm lại

1. Ghi attendance Absent trong workflow báo vắng theo yêu cầu Owner, giữ notice/audit và tính nguyên tử; không để notice trả 200 nhưng attendance thiếu.
2. Retry không nhân attendance/notice hoặc thay đổi credit lần hai. Giữ đúng owner và cửa sổ báo vắng đã thống nhất.
3. Coach xử lý attendance, Manager correction theo quy tắc hiện hành; giữ dấu vết nguồn Member báo vắng. Finalizer/scan/correction không tạo trùng hoặc làm mất lịch sử.
4. Cập nhật contract và cung cấp response + SQL sau commit cho: báo vắng hợp lệ, retry, sai owner, ngoài cửa sổ, tương tác finalizer/correction. Chỉ đóng lỗi khi expected Absent đạt; test xác nhận notice-only không phải nghiệm thu yêu cầu Owner.

Hồ sơ chi tiết `SCMS-BE-ABSENCE-FINDING-2026-10-31.md` được bản bàn giao gốc dẫn nhưng chưa có trong workspace. Hai run/case trên là tham chiếu lịch sử; bằng chứng kiểm mới theo docs00/QA, không coi file thiếu là bằng chứng đã được kiểm độc lập.

## Database đã đối chiếu

Nguồn là bộ SQL Owner chạy và xuất bằng DBeaver trên Supabase ngày 29/10/2026, snapshot **19:35:20+07**; không phải snapshot live mới ngày 31/10.

| Nhóm               | Kết quả đã review                                                                                                                                                                                |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Cấu trúc cho audit | 53/53 cột cần kiểm hiện diện; inventory 174 cột/16 bảng. Chưa chứng nhận mọi index/constraint/migration.                                                                                         |
| Dữ liệu bàn giao   | 33 Active users, 9 môn, 8 phòng/sân, 9 Normal + 9 Advanced template; 13 invoice, 4 quyền, 9 enrollment, 114 session, 73 attendance và 73 result.                                                 |
| Normal/makeup      | 9 Normal đều có 12 buổi hợp lệ. Lớp Tennis có 13 dòng vật lý do 1 buổi invalidated và 1 makeup liên kết.                                                                                         |
| Liên kết/lịch sử   | Các query đã xuất không phát hiện orphan/duplicate hoặc mismatch attendance–result trên khóa Member/session; không thấy ca gắn sai chủ quyền, credit âm hoặc sân chồng giờ trong bộ lọc đã kiểm. |
| Hóa đơn demo       | 12 Paid Cash và 1 Expired Cash. Snapshot này không chứng nhận VNPay hoặc Refund/CashCorrection trên Supabase.                                                                                    |

Nguồn schema của lần bàn giao29/10 là bản `schema_postgres.sql` được nhận khi đó, SHA256 lịch sử (không phải hash file đã cập nhật trong workspace hiện nay):
`D6B8C98C20F06BDBA085AD9F3CD4F7F284629C0458E4D77C82147CC718CCCD71`.
Đây là file ROOT additive dùng để đối chiếu, không phải yêu cầu chạy lại vào DB đang có.

Các workflow ghi FLOW01–05-A có bằng chứng trên PostgreSQL test riêng `scms_isolated_test`; các nhóm Clock sau đó chủ yếu chạy H2 riêng. Guard trước/sau H2 chỉ kiểm một số field của sub11/sessions49/50 trên PG tích lũy, không phải checksum toàn DB hoặc xác nhận số liệu live vẫn giữ nguyên bộ demo.

## Giới hạn kết luận

Các lượt local được review không mutation Render/Supabase. Không suy trạng thái deploy Render hoặc gateway VNPay thực tế từ H2 pass; không đưa lỗi live lịch sử vào danh sách đang mở nếu chưa tái hiện lại. Đề nghị tại31/10 là BE xử lý BE-ABSENCE-01 trước; trạng thái xử lý sau đó xem cập nhật đầu tài liệu. Báo cáo gốc không đề nghị migration/rebuild hoặc chỉnh số liệu demo chỉ để khớp báo cáo; migration room/attendance02/11 là phạm vi Owner phê duyệt sau đó.
