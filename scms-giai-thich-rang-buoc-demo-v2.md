# SCMS — Bàn giao ràng buộc và flow demo bất kỳ giờ

**Đối tượng:** Owner, BE, FE/Codex. Bản bàn giao16/11/2026, **đính chính17/11/2026**: cách gom ghi chú đã diễn giải quá mức trạng thái duyệt và khiến Dxx bị hiểu là đã được thay thế. Thiết kế hiện tại chuyển sang [docs05](docs/05-DEMO-RULE.md), nơi đã ghi phản hồi chốt điểm danh sớm/không hoàn sau điểm danh/30phút liên tục/QR300giây của Owner và phần còn cần chọn; chưa triển khai. Giữ bản này đối chiếu nguồn, không dùng prompt16/11 nguyên trạng.

## 1. Kết quả hiện tại

- Bộ sinh bundle đã sửa khóa reset, đồng bộ SQL/fixture/SYSTEM-DATA và đạt kiểm PostgreSQL cùng BE local. Ngày cài chưa chốt; không chạy bản có anchor cũ vào ngày khác. Trạng thái/bằng chứng chuẩn ở [Active/QA](docs/00-MASTER-CHECKLIST.md#active); cách chạy ở [docs02](docs/02-DEPLOYMENT-GUIDE.md#demo-reset-deadlock).
- Code được rà vẫn có cutoff Normal 48h, điểm danh theo giờ ca/trạng thái lớp, notice đặt Advanced 12h, hóa đơn mặc định 5 phút và QR bước hiện tại tối đa 30 giây. Normal Cash do Member tự chọn vẫn bị BE chặn; Advanced đã có luồng Cash. Các phương án trong bảng dưới **chưa được áp dụng vào bundle, code hoặc contract**; giá trị cấu hình thực trên Render cần kiểm riêng.
- Mục tiêu mới là chạy Manager tạo lớp → Member đăng ký/thanh toán → Manager xác nhận khai giảng → Coach điểm danh, và Advanced đăng ký → mua quyền → đề xuất/Accept → điểm danh, **bất kỳ ngày giờ nào**, kể cả sau18:15. Thiết kế cuối cho mục tiêu này còn mở.

## 2. Quá trình và quyết định giữ/bỏ

1. Giữ Normal12buổi và Advanced theo môn/Primary Coach; Advanced chỉ có lớp riêng sau khi Coach nhận đăng ký.
2. Giữ7ca90phút; Outdoor không có ca3/4, không thêm giờ tự do. Vì vậy sau18:15, ca mới hợp lệ sớm nhất thường là ngày sau.
3. Đã xử lý riêng lỗi khóa reset; việc đó không nới các rule thời gian hoặc thay quyền.
4. Giữ nguyên [bản nguồn có ghi chú duyệt](docs/archive/2026-11/scms-giai-thich-rang-buoc-demo-v2-ghi-chu-owner.md) để đối chiếu. Đính chính17/11: không dùng các ghi chú này để tự coi toàn bộ thiết kế/phạm vi đã hoàn tất hoặc Dxx đã bị thay thế; đối chiếu lại với yêu cầu demo hiện tại trong docs05.
5. Bỏ giới hạn demo “trước18:15” làm phương án cuối; Owner yêu cầu đề xuất lại mục5. “Điểm danh cả ngày của buổi” vẫn chưa giải quyết được việc tạo ca ngày mai rồi demo ngay tối nay.

| Mã / nguồn | Nội dung trong ghi chú V2 | Phần cần đối chiếu khi thiết kế lại |
|---|---|---|
| C1 / H1 | Đóng đăng ký Normal:48h →0h, đăng ký tới trước buổi đầu | Chưa triển khai; chọn ca tương lai có thể không cần nới mốc cho đường demo chính |
| C2 / H5 | Normal điểm danh sau Manager xác nhận khai giảng, không bắt chờ Ongoing | Chưa triển khai; phải đủ Paid/Registered, sĩ số, xác nhận khai giảng và phân công, không chỉ một lần bấm xác nhận |
| C3 / H6 | Điểm danh cả ngày của buổi | Không đủ giải quyết ca ngày mai nhưng demo tối nay; Owner17/11 đã chốt cho điểm danh sớm trước ngày/giờ học theo điều kiện ở docs05, chưa triển khai |
| C4 / H12 | Notice **đặt** Advanced:12h →0h, không âm; ca chưa bắt đầu | Chưa triển khai; chọn ca tương lai có thể giữ12h. Nếu đổi phải đồng bộ đề xuất/respondBy/Accept; không suy cutoff hủy/dời/báo vắng thành0h |
| C5 / H9 | Hóa đơn:5 →30phút | Chưa triển khai; làm rõ giữ suất qua giờ nghỉ/cutoff; FE dùng expiresAt thực |
| H10 | Member chọn Cash hoặc VNPay ở cả Normal/Advanced; Cash ra quầy Receptionist xác nhận | Giữ ghi chú nguồn; Normal tự chọn Cash đang bị chặn, Advanced tận dụng luồng có sẵn. Cần xác định phạm vi thực hiện sau đợt rà soát |
| C11 / H4 | Cho Manager xác nhận tới cuối buổi đầu90phút | Chưa triển khai; cách chọn ca tương lai không cần mở xác nhận sau start để demo chính |
| C12 / H7 | Cho cấu hình bật/tắt tự ghi Absent | Chưa chốt mặc định; docs05 đề xuất giữ job theo giờ thật khi điểm danh ca tương lai, không tự xóa cơ chế |
| C13 / H8 | QR tối đa **300giây**, thay đề xuất120giây | Chưa triển khai; docs05 trình lại phạm vi demo và yêu cầu sinh/scan/TTL/chống replay đồng bộ |
| H3 / nhóm D | Giữ minimum ceil(2×max/3), Paid mới cấp quyền, Present/Absent, cơ chế xử lý vắng | Dùng sĩ số demo nhỏ hợp lệ. Giữ code/tests khóa7ngày, toggle chế tài mặc định tắt |
| Phạm vi | Bản nguồn nêu đổi quy tắc qua cấu hình hệ thống | Owner17/11 ưu tiên tiện trình diễn; docs05 so sánh cách demo trước khi quyết định có cần đổi các quy tắc này. Không lấy dòng cũ loại bỏ phương án demo |

**Ping giữ máy chủ thức** chưa có lựa chọn rõ trong phần duyệt; chưa tự cài dịch vụ/ping hoặc đổi hạ tầng.

## 3. API và DB liên quan

| Luồng | API cần đối chiếu | Dữ liệu/kết quả phải bảo toàn |
|---|---|---|
| Normal | POST /api/classes/normal-with-sessions; POST /api/classes/{id}/enroll; POST /api/classes/{id}/confirm-opening | classes→12sessions; min/max, Coach/sân/lịch; Open/confirmation/Ongoing phải phân biệt. C1 còn ảnh hưởng preview/dời/chuyển/waitlist, không chỉ enroll |
| Cash/VNPay | POST /api/invoices/{id}/confirm-cash; pay-online; verify-vnpay; GET ownInvoice | Member Cash Pending chưa cấp quyền; Receptionist xác nhận hợp lệ mới Paid+Registered/Active. Giữ amount, TTL thực, callback và khoản cần đối soát |
| Advanced | advanced-registrations→decision→order; private-session-proposals→respond | registration→class riêng→subscription/invoice; Pending proposal chưa giữ tài nguyên/credit; Accept mới có Session/trừ1. Giữ hạn xác nhận mua24h và45ngày sử dụng |
| Attendance/result/QR | POST /api/training-results và /bulk; POST /api/attendances/scan; GET /api/attendances/me/checkin-code; PATCH /api/training-results/{id}/content | Đúng sessionId+memberId, quyền/tác giả; không ghi đè content khi chỉ điểm danh. QR không tự tạo result; lưu attendance chưa chốt Session |
| L28 — đơn Advanced hết hạn | POST /api/advanced-registrations/{id}/order và đọc Invoice/quyền hiện có | Trong24h có thể cần lượt mua mới cho đơn thật sự hết hạn/chưa trả; giữ chứng từ cũ, retry không nhân đơn/quyền. Không tự tạo đơn mới khi tiền cũ chưa rõ hoặc cần đối soát |

BE cần đối chiếu đúng các nhóm: NormalClassPolicy/NormalLifecycleService/NormalChangeService, EnrollmentService/InvoiceService/AdvancedRegistrationService, SessionService/PrivateSessionProposalService, SessionAttendancePolicy/AttendanceService/TrainingResultService và job hoàn tất/expiry. Đây là đầu mối để xác định phần thực sự cần sửa sau khi chốt thiết kế, không phải kết luận mọi nhóm đều đang lỗi.

Giữ Auth/role/owner/center, account active/block, FK/unique, sĩ số, trùng Coach/sân/Member, đóng băng giá và idempotency. Không đổi Clock, không ghi Paid từ Return, không đổi dữ liệu lịch sử để giả một lượt demo thành công. Chưa mở Refund sau Ongoing hoặc hủy hoàn Advanced dưới12h chỉ từ ý duyệt C4.

## 4. FE cần làm

Tên file là đầu mối từ các lần rà trước; xác định lại FE main và bản website trước khi giao patch. Các nhãn L01/L25/L28 dưới đây giữ từ bản V2, cần tái hiện trên đúng source/runtime.

| File/caller cần xem | API/yêu cầu mới | Kết quả cần kiểm |
|---|---|---|
| NewClassModal.jsx, ClassManagement.jsx | Tạo Normal, preview12buổi và confirm-opening theo phương án được chọn sau review | Tạo được lớp/lịch hợp lệ ở thời điểm demo; thiếu sĩ số/trùng tài nguyên vẫn chặn |
| SCMSContext.jsx, classEnrollmentApi.js, MemberCourseCatalog.jsx | Normal Member chọn Cash/VNPay; Reception thu đúng method — L25 | Pending Cash chưa có quyền; xác nhận quầy mới Paid/Registered; Cash/VNPay không bị đổi nhầm |
| advancedRegistrationApi.js, MemberCourseCatalog.jsx | Xử lý đơn hết hạn L28 và expiresAt mới | Còn/hết24h đúng; đổi đơn không mất lịch sử hoặc cấp credit trùng |
| memberPortalApi.js, CoachAssignedMembersView.jsx, MemberMyCoachView.jsx | Tận dụng caller đề xuất/Member Accept đã có; cập nhật notice/respondBy và cách hiển thị | Gửi lịch chưa giảm credit; Accept đọc lại đúng Session/credit; không giao viết lại phần đã có |
| attendanceApi.js, CoachAttendanceView.jsx, caller QR hiện có | Áp rule điểm danh/QR sau khi BE chốt; sửa đổi lớp/buổi L01 | Không gửi record buổi cũ; QR300giây khớp BE và chống replay; đơn/bulk/scan cùng điều kiện |
| CoachTrainingProgress.jsx và helper memberPortalApi.js | Phân biệt POST trong phạm vi được duyệt với PATCH content result có sẵn | Không dùng điểm danh để sửa nhận xét; sau lưu/reload đúng nội dung và attendance |

Khác biệt quyền/hạn vẫn do BE kiểm; không giải quyết bằng mở quyền rộng hoặc chỉ bỏ disabled trên nút. Không có dự toán giờ FE trong bản bàn giao này.

## 5. Việc còn lại

1. **Đính chính ưu tiên17/11:** đọc yêu cầu đã chốt và điều kiện đầy đủ tại [docs05](docs/05-DEMO-RULE.md), chỉ chờ Owner chọn phần thực sự còn mở. Normal phải Paid/Registered và Manager xác nhận khai giảng; Advanced phải Paid/Active rồi đúng Member Accept đề xuất tạo buổi, cùng kiểm quyền/phân công/trạng thái. Owner đã đồng ý điểm danh sớm, nhưng source chưa được sửa để hỗ trợ.
2. Làm rõ điểm danh sớm rồi hủy Advanced có hoàn credit không; thời điểm khóa result/correction và job hoàn tất sẽ theo buổi thật hay trạng thái mới; báo cáo/feedback không được vô tình coi một buổi tương lai đã hoàn tất. Chốt mặc định C12 và biên C11. QR300giây phải có thiết kế replay/rate limit đồng bộ.
3. Sau khi có thiết kế cụ thể, cập nhật đúng Dxx/contract và lập patch BE→API/DB→FE kèm điều kiện nghiệm thu. Kiểm cả sau18:15, cuối ngày/qua00:00, trước/đúng/sau cutoff, hóa đơn hết hạn/tiền đến muộn, retry/race và sai quyền. Expected theo quyết định, không theo riêng code.
4. Tái hiện L01/L25/L28 và bốn việc FE còn mở ở [Active](docs/00-MASTER-CHECKLIST.md#con-mo); sửa đúng phần thực sự còn thiếu. Chưa gọi build/mock/GET thành thao tác lưu thành công.
5. Khi chốt ngày cài, sinh/kiểm lại trọn bộ nếu khác anchor đã kiểm; SQL cần chạy thì chuẩn bị cho Owner, không tự chạy Supabase/reset/deploy. Sau SQL PASS phải lấy auth/me/ID mới trước rehearsal. Dữ liệu thử cũ chỉ dọn trong phạm vi đã giao, không xóa lịch sử để làm đẹp demo.

**Phạm vi hiện tại17/11:** chỉ rà soát/đính chính tài liệu và quyết định; giữ nguyên code, DB và triển khai. Giữ lịch sử ghi chú nhưng không dùng nó bỏ qua việc đối chiếu/đánh giá lại; hỏi đúng lựa chọn nghiệp vụ còn mở ở docs05. Sau khi sửa xong mới nhắc Owner kiểm FE thủ công/Playwright và bốn flow.
