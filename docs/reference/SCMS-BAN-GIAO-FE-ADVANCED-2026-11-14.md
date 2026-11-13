# Bàn giao FE — đăng ký và xác nhận lịch Advanced

**Dành cho:** FE/Codex. **Ngày:** 14/11/2026. Dùng form đăng ký, thanh toán và lịch hiện có; chưa sửa source FE trong đợt này.

## 1. Kết quả hiện tại

**Mua khóa:** Member chọn môn/Coach → Coach đồng ý → Member có24giờ bấm **Xác nhận và thanh toán** → tạo hóa đơn có hạn5phút → thanh toán hợp lệ → quyền học Active/12credit, bắt đầu45ngày sử dụng.

**Đặt buổi:** hai người trao đổi ngoài SCMS → Coach gửi đề xuất ngày/ca/sân → Member xem và Accept → BE kiểm lại lịch/sân/credit/hạn → tạo buổi Scheduled và trừ1credit. Đề xuất chờ không giữ sân hoặc credit; từ chối/hết hạn không mất credit.

Ngày học linh hoạt trong7ca90phút, theo lịch Coach và sân. Giữ hủy trước ít nhất12giờ, hạn45ngày sử dụng và quy tắc vắng hiện hành. Member xác nhận mua khóa khác với Accept từng buổi.

**Kết quả BE:** đã phát hành; kiểm API có phiên đọc dữ liệu/quyền và các request bị từ chối đạt trong phạm vi [Active/QA](../00-MASTER-CHECKLIST.md#active). Accept ghi thật trên Render còn chờ phạm vi Owner; FE lưu/đọc lại trên website chưa nghiệm thu.

**Dự toán FE/Codex:** 2–4giờ sửa và test đích; thêm45–90phút kiểm website khi BE/dữ liệu/cửa giờ sẵn. Chưa đo thực tế, không tính thời gian chờ SQL/phát hành hoặc giờ học.

## 2. Quá trình và quyết định giữ/bỏ

1. Giữ chọn môn/Coach, Coach xét nhận rồi Member mới tạo hóa đơn; bổ sung hạn24giờ từ `acceptedAt`.
2. Giữ hạn5phút của hóa đơn tính từ lúc tạo; retry không kéo dài hạn hoặc sinh thêm hóa đơn.
3. Thay đặt buổi trực tiếp bằng đề xuất → Member Accept; bỏ cách Coach tự xác nhận thay Member.
4. Giữ trao đổi ngoài SCMS và7ca hiện có; không thêm chat Member–Coach hoặc lịch giờ tự do.

## 3. API và DB liên quan

| Bước | Request và dữ liệu cần dùng | Kết quả thành công |
|---|---|---|
| Đăng ký | `POST /api/advanced-registrations`, `subjectId,primaryCoachId,requestKey` | `Requested`; lưu `id` đăng ký |
| Coach quyết định | `POST /api/advanced-registrations/{id}/decision`, `{"accepted":true}` | `Accepted`, `acceptedAt`, `confirmationDeadline` |
| Member xác nhận mua | `POST /api/advanced-registrations/{id}/order`, `{"paymentMethod":"VNPay"}` hoặc Cash đúng luồng | Hóa đơn Pending, `invoiceId,expiresAt,paymentUrl`; chưa cấp credit |
| Coach đề xuất lịch | `POST /api/private-session-proposals` | 201, `id` đề xuất, `status:Pending`, `sessionId:null`, `respondBy` |
| Đọc đề xuất | `GET /api/private-session-proposals` hoặc `/{id}` | Member chỉ thấy của mình; Coach thấy buổi mình dạy; staff đúng quyền/trung tâm |
| Member phản hồi | `POST /api/private-session-proposals/{id}/respond`, `{"accepted":true}` hoặc false | 200; Accept có `status:Accepted,sessionId,session`; từ chối là Rejected |

Ví dụ request đề xuất: `{"subscriptionId":42,"subjectId":3,"coachId":7,"roomId":5,"sessionDate":"2026-11-20","startTime":"06:00","endTime":"07:30","agreementConfirmed":true,"requestKey":"adv-42-20261120-1"}`. ID/ngày chỉ minh họa; lấy ID và điều kiện thật từ API. `agreementConfirmed` là Coach xác nhận đã trao đổi; vẫn bắt buộc Member Accept riêng.

`advanced_registrations` → `member_packages` lưu quyền/credit/hạn; `invoices` liên kết quyền học. Bảng mới `private_session_proposals` lưu lịch chờ và phản hồi, liên kết `sessions` sau Accept. Đề xuất hết cửa trước12giờ hiển thị Expired; đúng mốc12giờ vẫn được Accept nếu các điều kiện khác đạt.

**Ví dụ minh họa kết quả thành công:** trước Accept quyền học còn12credit và chưa có buổi. Sau Accept: đề xuất `Accepted`, `sessionId:101`, `session.status:Scheduled`, quyền học còn11credit. Gửi lại cùng phản hồi vẫn cùng buổi101 và11credit; phải GET đọc lại để xác nhận.

## 4. FE cần làm

- **Đăng ký:** sửa `src/api/advancedRegistrationApi.js` và `src/components/views/MemberCourseCatalog.jsx`, dùng `confirmationDeadline` cho nút xác nhận mua. Hết hạn thì báo đăng ký hết hạn; khi bấm nút mới gọi order và mở thanh toán. Không tính45ngày từ lúc Coach nhận.
- **Coach gửi lịch:** sửa `src/api/memberPortalApi.js` và `src/components/views/CoachAssignedMembersView.jsx`, đổi caller đặt buổi sang API đề xuất mới. Giữ form chọn ngày/ca/sân, báo **Đã gửi lịch, chờ học viên xác nhận**; chưa chèn vào lịch Scheduled hoặc tự giảm credit. Coach gọi endpoint cũ nhận400 `USE_PRIVATE_SESSION_PROPOSAL`, không đặt buổi.
- **Member nhận lịch:** trong `src/components/views/MemberMyCoachView.jsx`, thay thao tác Member tự gọi `book-private` bằng thẻ đề xuất đang chờ; thêm helper đọc/phản hồi vào `memberPortalApi.js`. Hiển thị ngày/giờ/Coach/sân và hạn phản hồi, có **Đồng ý**/**Từ chối**. Sau Accept, tải lại đề xuất, lịch và quyền học để lấy trạng thái/credit từ BE.
- **Lỗi:** 409 `REGISTRATION_CONFIRMATION_EXPIRED` yêu cầu đăng ký lại; `PROPOSAL_EXPIRED` là lịch đã quá hạn phản hồi. Trùng sân/Coach, thiếu credit hoặc quá hạn quyền học khi Accept thì giữ thông báo lỗi, tải lại dữ liệu và trao đổi lịch mới; không tự đổi giờ hoặc tự retry ghi.
- **Đề xuất giao diện:** dùng hai nhóm **Chờ xác nhận** và **Đã lên lịch** trên màn lịch hiện có, kèm thông báo nhận lịch. Đây là gợi ý để FE chọn bố trí, không yêu cầu xây một màn quản trị mới.

## 5. Việc còn lại

FE/Codex xác định lại main và bản website trước sửa; xác nhận `/api/auth/me` và đúng actor. Kiểm tạo đơn trước/đúng/sau24giờ, hạn hóa đơn5phút; đề xuất không giảm credit; Accept rồi đọc lại; từ chối/hết hạn; sai Member/trung tâm; trùng tài nguyên khi Accept; retry không nhân buổi/credit. Kiểm tiếp hủy12giờ và điểm danh trên buổi đã xác nhận.

BE mới đã có trên Render; FE chưa được sửa/push hoặc nghiệm thu lưu trong đợt này. Bàn giao lại file đã sửa, kết quả lưu/đọc lại từng bước và thời gian thực tế; ca Accept ghi thật còn chờ Owner mở phạm vi, không suy từ kiểm đọc/negative thành công.
