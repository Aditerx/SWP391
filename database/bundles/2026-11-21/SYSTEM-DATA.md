# SYSTEM DATA — Bản xem trước theo anchor

**Trạng thái: EXPECTED DESIGN; SQL execution NOT_RUN.** Đây là lịch và dữ liệu dự kiến từ nguồn sinh, chưa xuất từ PostgreSQL của anchor này. Kiểm ngoại tuyến không chứng nhận chạy SQL, HTTP hoặc Supabase. Khi đến đúng ngày, kiểm SQL local rồi xuất lại từ fixture nguyên trạng trước HTTP. ID DB được sinh khi seed; `key`, Nxx/Mxx/RFxx là mã đối chiếu, FE phải lấy ID thật từ API. Contract BE hiện hành ở docs01, trạng thái ở docs00/QA.

Nguồn: [Owner decisions](../../../docs/04-DEBUG-NOTE.md), [review và phê duyệt](../../../docs/archive/2026-11/03-TODO.md#system-data-review-20261109), [Normal Lifecycle](../../../docs/reference/SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Refund Normal](../../../docs/reference/SCMS_BE_Refund_Normal_DA_CHOT.md), reference seed đã đối chiếu33account PDF. [Active/QA](../../../docs/00-MASTER-CHECKLIST.md#active) giữ trạng thái kiểm/phát hành; dữ liệu đã đọc trên cloud không được trộn vào bộ mới này.

## Quy ước

- Anchor thiết kế:2026-11-21 00:00 Asia/Ho_Chi_Minh, không phải cấu hình Clock cho môi trường deploy. Fixture thực thi phải chọn lại ngày và kiểm conflict/ràng buộc trước commit.
- Giữ33account PDF, thêm14account riêng. Mật khẩu demo theo quyết định Owner; không đưa hash/secret vào bảng. Chỉ account demo bỏ first-login; không đổi policy tạo account thực qua API.
- 9môn/20room/8lớp Advanced có đăng ký đã nhận/12Normal; N05→N11 Pickleball cùng1.800.000đ/center1, Member đồng ý rõ.
- Ngày làm việc hoàn tiền = ngày TT mở theo Holiday, tính T7/CN nếu không đóng; giữ7ngày làm việc. Lịch nghỉ Coach không dừng SLA của trung tâm.
- Contact phone của Member là dữ liệu liên hệ đúng quyền, FE quyết định dùng/hiển thị; không nằm trong chứng từ ngân hàng và không thay xác minh.
- Mỗi bảng có mục riêng. Bảng rỗng nêu rõ vì sao; các scenario động/Advanced chưa cụ thể hóa nằm ở phần còn lại, không được xem là đã có đủ dòng để thực thi toàn bộ demo.
- Dữ liệu tài chính/chứng từ đều là mô phỏng có nhãn DEMO; không tạo reference giả giống giao dịch ngân hàng thật, không gọi mail/gateway/ngân hàng. Unlisted nullable/runtime columns dùng DEFAULT/NULL theo schema khi triển khai; bảng này không cung cấp secret.

## Các bảng dữ liệu


### 1. `private_session_proposals`

| key | subscription | sports_class | subject | proposed_by | date | start | end | coach | member | room | status | created_at | responded_at | respond_by | session | note |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DEMO-CANCEL-SUB01 | SUB01 | A-AR04 | TEN | TEN-A | 2026-11-26 | 06:00 | 07:30 | TEN-A | M02 | C01-TEN-01 | Accepted | 2026-11-21T00:00 | 2026-11-21T00:00 | 2026-11-25T18:00:00 | SUB01-S01 | DEMO newly approved cancellation fixture; synthetic Member acceptance, not historical consent |

### 2. `normal_refund_verifications`

Chưa có dòng tĩnh trong bản thiết kế này.


### 3. `normal_refund_documents`

Chưa có dòng tĩnh trong bản thiết kế này.


### 4. `normal_refund_attempts`

Chưa có dòng tĩnh trong bản thiết kế này.


### 5. `normal_refund_receivers`

Chưa có dòng tĩnh trong bản thiết kế này.


### 6. `normal_refunds`

Chưa có dòng tĩnh trong bản thiết kế này.


### 7. `normal_transfers`

Chưa có dòng tĩnh trong bản thiết kế này.


### 8. `normal_responses`

Chưa có dòng tĩnh trong bản thiết kế này.


### 9. `normal_proposals`

Chưa có dòng tĩnh trong bản thiết kế này.


### 10. `scms_data_migration_audit`

Marker do migration/reset thực tế tạo, không seed giả migration đã chạy trong bộ thiết kế này.

Chưa có dòng tĩnh trong bản thiết kế này.


### 11. `payment_resolutions`

Tên state/field ở đây diễn đạt giai đoạn nghiệp vụ để thống nhất dữ liệu, chưa là enum/DTO/API đã triển khai. Receiver DEMO không phải tài khoản ngân hàng có thể chuyển tiền. Contact phone tùy FE dùng, không thay xác minh tài khoản.

| key | type | scope | invoice | enrollment | event | member | status | amount | origin | receiver | recipient_version | approved_by | completed_by | contact_phone | sla | approved_at | receiver_confirmed_at | transfer_due_at |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CC01 | CashCorrection | ADVANCED_UNUSED | INV-SUB06 | NULL | NULL | M07 | Approved | 3000000 | COUNTER_MISTAKE | NULL | NULL | CM1 | NULL | NULL | Không phải Refund; chưa nhận tiền; Complete sẽ đảo xác nhận và thu hồi12credit theo D16 | NULL | NULL | NULL |
| RF01 | Refund | NORMAL_PRESTART | INV-N05-M02 | N05-M02 | EV-N05 | M02 | AwaitingReceiver | 1800000 | MEMBER_REQUEST | NULL | 0 | NULL | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | NULL | NULL | NULL |
| RF02 | Refund | NORMAL_PRESTART | INV-N06-M04 | N06-M04 | EV-N06 | M04 | AwaitingReceiver | 1200000 | CENTER_CANCELLATION | NULL | 0 | NULL | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | NULL | NULL | NULL |
| RF03 | Refund | NORMAL_PRESTART | INV-N06-M05 | N06-M05 | EV-N06 | M05 | AwaitingApproval | 1200000 | CENTER_CANCELLATION | DEMO-BANK / M05 / DEMO-ACCOUNT-M05 | 1 | NULL | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | NULL | 2026-11-21T00:00 | NULL |
| RF04 | Refund | NORMAL_PRESTART | INV-N06-M06 | N06-M06 | EV-N06 | M06 | Approved | 1200000 | CENTER_CANCELLATION | DEMO-BANK / M06 / DEMO-ACCOUNT-M06 | 1 | CM1 | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | 2026-11-21T00:00 | 2026-11-21T00:00 | 2026-11-28T23:59:59.999 |
| RF05 | Refund | NORMAL_PRESTART | INV-N12-M07 | N12-M07 | EV-N12 | M07 | AwaitingVerification | 900000 | CENTER_CANCELLATION | DEMO-BANK / M07 / DEMO-ACCOUNT-M07 | 1 | CM1 | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | 2026-11-18T00:00 | 2026-11-18T00:00 | 2026-11-25T23:59:59.999 |
| RF06 | Refund | NORMAL_PRESTART | INV-N12-M08 | N12-M08 | EV-N12 | M08 | Completed | 900000 | CENTER_CANCELLATION | DEMO-BANK / M08 / DEMO-ACCOUNT-M08 | 1 | CM1 | CM2 | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | 2026-11-18T00:00 | 2026-11-18T00:00 | 2026-11-25T23:59:59.999 |
| RF07 | Refund | NORMAL_PRESTART | INV-N12-M09 | N12-M09 | EV-N12 | M09 | TransferIssue | 900000 | CENTER_CANCELLATION | DEMO-BANK / M09 / DEMO-ACCOUNT-M09 | 1 | CM1 | NULL | JOIN users.phone theo member; FE tùy chọn | 7ngày TT mở; chỉ tính khi Approved và đủ receiver | 2026-11-18T00:00 | 2026-11-18T00:00 | 2026-11-25T23:59:59.999 |

### 12. `incident_affected_members`

| incident | member |
| --- | --- |
| INC-N10 | M01 |
| INC-N10 | M02 |
| INC-N10 | M03 |

### 13. `center_incidents`

| key | session | status | reason | evidence | session_compensated | created_by |
| --- | --- | --- | --- | --- | --- | --- |
| INC-N10 | N10-S08 | Resolved | DEMO buổi không tổ chức do trung tâm | DEMO xác minh buổi không tổ chức | true | CM1 |

### 14. `session_changes`

Thao tác dời buổi/thay Coach tạo trong bước demo qua API; không seed history để giả đã đi qua action.

Chưa có dòng tĩnh trong bản thiết kế này.


### 15. `session_absence_notices`

Case Advanced sát giờ cần ca thật tại thời điểm demo; không mở báo vắng cá nhân Normal.

Chưa có dòng tĩnh trong bản thiết kế này.


### 16. `advanced_registrations`

Giữ Requested/Accepted/Rejected/Invoiced riêng với state của Invoice/Subscription; không sửa Advanced theo file Normal.

| key | member | subject | coach | status | subscription | request_key | created_at | accepted_at | reason | sports_class |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AR01 | M14 | SWIM | SWIM-A | Requested | NULL | DEMO-AR01 | 2026-11-19T06:00 | NULL | NULL | NULL |
| AR02 | M15 | PICK | PICK-A | Accepted | NULL | DEMO-AR02 | 2026-11-19T06:00 | 2026-11-21T00:00 | NULL | A-AR02 |
| AR03 | M16 | ARC | ARC-A | Rejected | NULL | DEMO-AR03 | 2026-11-19T06:00 | 2026-11-20T07:00 | DEMO Coach từ chối yêu cầu | NULL |
| AR04 | M02 | TEN | TEN-A | Invoiced | SUB01 | DEMO-SUB01 | 2026-11-17T07:30 | 2026-11-17T07:55 | NULL | A-AR04 |
| AR05 | M03 | TEN | TEN-A | Invoiced | SUB02 | DEMO-SUB02 | 2026-11-16T07:30 | 2026-11-16T07:55 | NULL | A-AR05 |
| AR06 | M04 | BAD | BAD-A | Invoiced | SUB03 | DEMO-SUB03 | 2026-11-15T07:30 | 2026-11-15T07:55 | NULL | A-AR06 |
| AR07 | M05 | TEN | TEN-A | Invoiced | SUB04 | DEMO-SUB04 | 2026-10-12T07:30 | 2026-10-12T07:55 | NULL | A-AR07 |
| AR08 | M06 | TEN | TEN-A | Invoiced | SUB05 | DEMO-SUB05 | 2026-09-12T07:30 | 2026-09-12T07:55 | NULL | A-AR08 |
| AR09 | M07 | BAD | BAD-A | Invoiced | SUB06 | DEMO-SUB06 | 2026-11-19T07:30 | 2026-11-19T07:55 | NULL | A-AR09 |
| AR10 | M08 | SWIM | SWIM-A | Invoiced | SUB07 | DEMO-SUB07 | 2026-11-18T07:30 | 2026-11-18T07:40 | NULL | A-AR10 |

### 17. `coach_days_off`

| coach | date |
| --- | --- |
| PICK-B | 2027-01-20 |

### 18. `coach_work_slots`

Mỗi dòng là một slot thật của quan hệ. T2–T7 là lịch Coach demo, không phải định nghĩa ngày làm việc của TT; Chủ nhật trống để trình diễn cấu hình. Outdoor vẫn cấm ca3/4.

| coach | slot_key |
| --- | --- |
| TEN-A | MONDAY:1 |
| TEN-A | MONDAY:2 |
| TEN-A | MONDAY:3 |
| TEN-A | MONDAY:4 |
| TEN-A | MONDAY:5 |
| TEN-A | MONDAY:6 |
| TEN-A | MONDAY:7 |
| TEN-A | TUESDAY:1 |
| TEN-A | TUESDAY:2 |
| TEN-A | TUESDAY:3 |
| TEN-A | TUESDAY:4 |
| TEN-A | TUESDAY:5 |
| TEN-A | TUESDAY:6 |
| TEN-A | TUESDAY:7 |
| TEN-A | WEDNESDAY:1 |
| TEN-A | WEDNESDAY:2 |
| TEN-A | WEDNESDAY:3 |
| TEN-A | WEDNESDAY:4 |
| TEN-A | WEDNESDAY:5 |
| TEN-A | WEDNESDAY:6 |
| TEN-A | WEDNESDAY:7 |
| TEN-A | THURSDAY:1 |
| TEN-A | THURSDAY:2 |
| TEN-A | THURSDAY:3 |
| TEN-A | THURSDAY:4 |
| TEN-A | THURSDAY:5 |
| TEN-A | THURSDAY:6 |
| TEN-A | THURSDAY:7 |
| TEN-A | FRIDAY:1 |
| TEN-A | FRIDAY:2 |
| TEN-A | FRIDAY:3 |
| TEN-A | FRIDAY:4 |
| TEN-A | FRIDAY:5 |
| TEN-A | FRIDAY:6 |
| TEN-A | FRIDAY:7 |
| TEN-A | SATURDAY:1 |
| TEN-A | SATURDAY:2 |
| TEN-A | SATURDAY:3 |
| TEN-A | SATURDAY:4 |
| TEN-A | SATURDAY:5 |
| TEN-A | SATURDAY:6 |
| TEN-A | SATURDAY:7 |
| TEN-A | SUNDAY:1 |
| TEN-A | SUNDAY:2 |
| TEN-A | SUNDAY:3 |
| TEN-A | SUNDAY:4 |
| TEN-A | SUNDAY:5 |
| TEN-A | SUNDAY:6 |
| TEN-A | SUNDAY:7 |
| TEN-B | MONDAY:1 |
| TEN-B | MONDAY:2 |
| TEN-B | MONDAY:3 |
| TEN-B | MONDAY:4 |
| TEN-B | MONDAY:5 |
| TEN-B | MONDAY:6 |
| TEN-B | MONDAY:7 |
| TEN-B | TUESDAY:1 |
| TEN-B | TUESDAY:2 |
| TEN-B | TUESDAY:3 |
| TEN-B | TUESDAY:4 |
| TEN-B | TUESDAY:5 |
| TEN-B | TUESDAY:6 |
| TEN-B | TUESDAY:7 |
| TEN-B | WEDNESDAY:1 |
| TEN-B | WEDNESDAY:2 |
| TEN-B | WEDNESDAY:3 |
| TEN-B | WEDNESDAY:4 |
| TEN-B | WEDNESDAY:5 |
| TEN-B | WEDNESDAY:6 |
| TEN-B | WEDNESDAY:7 |
| TEN-B | THURSDAY:1 |
| TEN-B | THURSDAY:2 |
| TEN-B | THURSDAY:3 |
| TEN-B | THURSDAY:4 |
| TEN-B | THURSDAY:5 |
| TEN-B | THURSDAY:6 |
| TEN-B | THURSDAY:7 |
| TEN-B | FRIDAY:1 |
| TEN-B | FRIDAY:2 |
| TEN-B | FRIDAY:3 |
| TEN-B | FRIDAY:4 |
| TEN-B | FRIDAY:5 |
| TEN-B | FRIDAY:6 |
| TEN-B | FRIDAY:7 |
| TEN-B | SATURDAY:1 |
| TEN-B | SATURDAY:2 |
| TEN-B | SATURDAY:3 |
| TEN-B | SATURDAY:4 |
| TEN-B | SATURDAY:5 |
| TEN-B | SATURDAY:6 |
| TEN-B | SATURDAY:7 |
| TEN-B | SUNDAY:1 |
| TEN-B | SUNDAY:2 |
| TEN-B | SUNDAY:3 |
| TEN-B | SUNDAY:4 |
| TEN-B | SUNDAY:5 |
| TEN-B | SUNDAY:6 |
| TEN-B | SUNDAY:7 |
| SWIM-A | MONDAY:1 |
| SWIM-A | MONDAY:2 |
| SWIM-A | MONDAY:3 |
| SWIM-A | MONDAY:4 |
| SWIM-A | MONDAY:5 |
| SWIM-A | MONDAY:6 |
| SWIM-A | MONDAY:7 |
| SWIM-A | TUESDAY:1 |
| SWIM-A | TUESDAY:2 |
| SWIM-A | TUESDAY:3 |
| SWIM-A | TUESDAY:4 |
| SWIM-A | TUESDAY:5 |
| SWIM-A | TUESDAY:6 |
| SWIM-A | TUESDAY:7 |
| SWIM-A | WEDNESDAY:1 |
| SWIM-A | WEDNESDAY:2 |
| SWIM-A | WEDNESDAY:3 |
| SWIM-A | WEDNESDAY:4 |
| SWIM-A | WEDNESDAY:5 |
| SWIM-A | WEDNESDAY:6 |
| SWIM-A | WEDNESDAY:7 |
| SWIM-A | THURSDAY:1 |
| SWIM-A | THURSDAY:2 |
| SWIM-A | THURSDAY:3 |
| SWIM-A | THURSDAY:4 |
| SWIM-A | THURSDAY:5 |
| SWIM-A | THURSDAY:6 |
| SWIM-A | THURSDAY:7 |
| SWIM-A | FRIDAY:1 |
| SWIM-A | FRIDAY:2 |
| SWIM-A | FRIDAY:3 |
| SWIM-A | FRIDAY:4 |
| SWIM-A | FRIDAY:5 |
| SWIM-A | FRIDAY:6 |
| SWIM-A | FRIDAY:7 |
| SWIM-A | SATURDAY:1 |
| SWIM-A | SATURDAY:2 |
| SWIM-A | SATURDAY:3 |
| SWIM-A | SATURDAY:4 |
| SWIM-A | SATURDAY:5 |
| SWIM-A | SATURDAY:6 |
| SWIM-A | SATURDAY:7 |
| SWIM-A | SUNDAY:1 |
| SWIM-A | SUNDAY:2 |
| SWIM-A | SUNDAY:3 |
| SWIM-A | SUNDAY:4 |
| SWIM-A | SUNDAY:5 |
| SWIM-A | SUNDAY:6 |
| SWIM-A | SUNDAY:7 |
| SWIM-B | MONDAY:1 |
| SWIM-B | MONDAY:2 |
| SWIM-B | MONDAY:3 |
| SWIM-B | MONDAY:4 |
| SWIM-B | MONDAY:5 |
| SWIM-B | MONDAY:6 |
| SWIM-B | MONDAY:7 |
| SWIM-B | TUESDAY:1 |
| SWIM-B | TUESDAY:2 |
| SWIM-B | TUESDAY:3 |
| SWIM-B | TUESDAY:4 |
| SWIM-B | TUESDAY:5 |
| SWIM-B | TUESDAY:6 |
| SWIM-B | TUESDAY:7 |
| SWIM-B | WEDNESDAY:1 |
| SWIM-B | WEDNESDAY:2 |
| SWIM-B | WEDNESDAY:3 |
| SWIM-B | WEDNESDAY:4 |
| SWIM-B | WEDNESDAY:5 |
| SWIM-B | WEDNESDAY:6 |
| SWIM-B | WEDNESDAY:7 |
| SWIM-B | THURSDAY:1 |
| SWIM-B | THURSDAY:2 |
| SWIM-B | THURSDAY:3 |
| SWIM-B | THURSDAY:4 |
| SWIM-B | THURSDAY:5 |
| SWIM-B | THURSDAY:6 |
| SWIM-B | THURSDAY:7 |
| SWIM-B | FRIDAY:1 |
| SWIM-B | FRIDAY:2 |
| SWIM-B | FRIDAY:3 |
| SWIM-B | FRIDAY:4 |
| SWIM-B | FRIDAY:5 |
| SWIM-B | FRIDAY:6 |
| SWIM-B | FRIDAY:7 |
| SWIM-B | SATURDAY:1 |
| SWIM-B | SATURDAY:2 |
| SWIM-B | SATURDAY:3 |
| SWIM-B | SATURDAY:4 |
| SWIM-B | SATURDAY:5 |
| SWIM-B | SATURDAY:6 |
| SWIM-B | SATURDAY:7 |
| SWIM-B | SUNDAY:1 |
| SWIM-B | SUNDAY:2 |
| SWIM-B | SUNDAY:3 |
| SWIM-B | SUNDAY:4 |
| SWIM-B | SUNDAY:5 |
| SWIM-B | SUNDAY:6 |
| SWIM-B | SUNDAY:7 |
| PICK-A | MONDAY:1 |
| PICK-A | MONDAY:2 |
| PICK-A | MONDAY:3 |
| PICK-A | MONDAY:4 |
| PICK-A | MONDAY:5 |
| PICK-A | MONDAY:6 |
| PICK-A | MONDAY:7 |
| PICK-A | TUESDAY:1 |
| PICK-A | TUESDAY:2 |
| PICK-A | TUESDAY:3 |
| PICK-A | TUESDAY:4 |
| PICK-A | TUESDAY:5 |
| PICK-A | TUESDAY:6 |
| PICK-A | TUESDAY:7 |
| PICK-A | WEDNESDAY:1 |
| PICK-A | WEDNESDAY:2 |
| PICK-A | WEDNESDAY:3 |
| PICK-A | WEDNESDAY:4 |
| PICK-A | WEDNESDAY:5 |
| PICK-A | WEDNESDAY:6 |
| PICK-A | WEDNESDAY:7 |
| PICK-A | THURSDAY:1 |
| PICK-A | THURSDAY:2 |
| PICK-A | THURSDAY:3 |
| PICK-A | THURSDAY:4 |
| PICK-A | THURSDAY:5 |
| PICK-A | THURSDAY:6 |
| PICK-A | THURSDAY:7 |
| PICK-A | FRIDAY:1 |
| PICK-A | FRIDAY:2 |
| PICK-A | FRIDAY:3 |
| PICK-A | FRIDAY:4 |
| PICK-A | FRIDAY:5 |
| PICK-A | FRIDAY:6 |
| PICK-A | FRIDAY:7 |
| PICK-A | SATURDAY:1 |
| PICK-A | SATURDAY:2 |
| PICK-A | SATURDAY:3 |
| PICK-A | SATURDAY:4 |
| PICK-A | SATURDAY:5 |
| PICK-A | SATURDAY:6 |
| PICK-A | SATURDAY:7 |
| PICK-A | SUNDAY:1 |
| PICK-A | SUNDAY:2 |
| PICK-A | SUNDAY:3 |
| PICK-A | SUNDAY:4 |
| PICK-A | SUNDAY:5 |
| PICK-A | SUNDAY:6 |
| PICK-A | SUNDAY:7 |
| PICK-B | MONDAY:1 |
| PICK-B | MONDAY:2 |
| PICK-B | MONDAY:3 |
| PICK-B | MONDAY:4 |
| PICK-B | MONDAY:5 |
| PICK-B | MONDAY:6 |
| PICK-B | MONDAY:7 |
| PICK-B | TUESDAY:1 |
| PICK-B | TUESDAY:2 |
| PICK-B | TUESDAY:3 |
| PICK-B | TUESDAY:4 |
| PICK-B | TUESDAY:5 |
| PICK-B | TUESDAY:6 |
| PICK-B | TUESDAY:7 |
| PICK-B | WEDNESDAY:1 |
| PICK-B | WEDNESDAY:2 |
| PICK-B | WEDNESDAY:3 |
| PICK-B | WEDNESDAY:4 |
| PICK-B | WEDNESDAY:5 |
| PICK-B | WEDNESDAY:6 |
| PICK-B | WEDNESDAY:7 |
| PICK-B | THURSDAY:1 |
| PICK-B | THURSDAY:2 |
| PICK-B | THURSDAY:3 |
| PICK-B | THURSDAY:4 |
| PICK-B | THURSDAY:5 |
| PICK-B | THURSDAY:6 |
| PICK-B | THURSDAY:7 |
| PICK-B | FRIDAY:1 |
| PICK-B | FRIDAY:2 |
| PICK-B | FRIDAY:3 |
| PICK-B | FRIDAY:4 |
| PICK-B | FRIDAY:5 |
| PICK-B | FRIDAY:6 |
| PICK-B | FRIDAY:7 |
| PICK-B | SATURDAY:1 |
| PICK-B | SATURDAY:2 |
| PICK-B | SATURDAY:3 |
| PICK-B | SATURDAY:4 |
| PICK-B | SATURDAY:5 |
| PICK-B | SATURDAY:6 |
| PICK-B | SATURDAY:7 |
| PICK-B | SUNDAY:1 |
| PICK-B | SUNDAY:2 |
| PICK-B | SUNDAY:3 |
| PICK-B | SUNDAY:4 |
| PICK-B | SUNDAY:5 |
| PICK-B | SUNDAY:6 |
| PICK-B | SUNDAY:7 |
| ARC-A | MONDAY:1 |
| ARC-A | MONDAY:2 |
| ARC-A | MONDAY:3 |
| ARC-A | MONDAY:4 |
| ARC-A | MONDAY:5 |
| ARC-A | MONDAY:6 |
| ARC-A | MONDAY:7 |
| ARC-A | TUESDAY:1 |
| ARC-A | TUESDAY:2 |
| ARC-A | TUESDAY:3 |
| ARC-A | TUESDAY:4 |
| ARC-A | TUESDAY:5 |
| ARC-A | TUESDAY:6 |
| ARC-A | TUESDAY:7 |
| ARC-A | WEDNESDAY:1 |
| ARC-A | WEDNESDAY:2 |
| ARC-A | WEDNESDAY:3 |
| ARC-A | WEDNESDAY:4 |
| ARC-A | WEDNESDAY:5 |
| ARC-A | WEDNESDAY:6 |
| ARC-A | WEDNESDAY:7 |
| ARC-A | THURSDAY:1 |
| ARC-A | THURSDAY:2 |
| ARC-A | THURSDAY:3 |
| ARC-A | THURSDAY:4 |
| ARC-A | THURSDAY:5 |
| ARC-A | THURSDAY:6 |
| ARC-A | THURSDAY:7 |
| ARC-A | FRIDAY:1 |
| ARC-A | FRIDAY:2 |
| ARC-A | FRIDAY:3 |
| ARC-A | FRIDAY:4 |
| ARC-A | FRIDAY:5 |
| ARC-A | FRIDAY:6 |
| ARC-A | FRIDAY:7 |
| ARC-A | SATURDAY:1 |
| ARC-A | SATURDAY:2 |
| ARC-A | SATURDAY:3 |
| ARC-A | SATURDAY:4 |
| ARC-A | SATURDAY:5 |
| ARC-A | SATURDAY:6 |
| ARC-A | SATURDAY:7 |
| ARC-A | SUNDAY:1 |
| ARC-A | SUNDAY:2 |
| ARC-A | SUNDAY:3 |
| ARC-A | SUNDAY:4 |
| ARC-A | SUNDAY:5 |
| ARC-A | SUNDAY:6 |
| ARC-A | SUNDAY:7 |
| ARC-B | MONDAY:1 |
| ARC-B | MONDAY:2 |
| ARC-B | MONDAY:3 |
| ARC-B | MONDAY:4 |
| ARC-B | MONDAY:5 |
| ARC-B | MONDAY:6 |
| ARC-B | MONDAY:7 |
| ARC-B | TUESDAY:1 |
| ARC-B | TUESDAY:2 |
| ARC-B | TUESDAY:3 |
| ARC-B | TUESDAY:4 |
| ARC-B | TUESDAY:5 |
| ARC-B | TUESDAY:6 |
| ARC-B | TUESDAY:7 |
| ARC-B | WEDNESDAY:1 |
| ARC-B | WEDNESDAY:2 |
| ARC-B | WEDNESDAY:3 |
| ARC-B | WEDNESDAY:4 |
| ARC-B | WEDNESDAY:5 |
| ARC-B | WEDNESDAY:6 |
| ARC-B | WEDNESDAY:7 |
| ARC-B | THURSDAY:1 |
| ARC-B | THURSDAY:2 |
| ARC-B | THURSDAY:3 |
| ARC-B | THURSDAY:4 |
| ARC-B | THURSDAY:5 |
| ARC-B | THURSDAY:6 |
| ARC-B | THURSDAY:7 |
| ARC-B | FRIDAY:1 |
| ARC-B | FRIDAY:2 |
| ARC-B | FRIDAY:3 |
| ARC-B | FRIDAY:4 |
| ARC-B | FRIDAY:5 |
| ARC-B | FRIDAY:6 |
| ARC-B | FRIDAY:7 |
| ARC-B | SATURDAY:1 |
| ARC-B | SATURDAY:2 |
| ARC-B | SATURDAY:3 |
| ARC-B | SATURDAY:4 |
| ARC-B | SATURDAY:5 |
| ARC-B | SATURDAY:6 |
| ARC-B | SATURDAY:7 |
| ARC-B | SUNDAY:1 |
| ARC-B | SUNDAY:2 |
| ARC-B | SUNDAY:3 |
| ARC-B | SUNDAY:4 |
| ARC-B | SUNDAY:5 |
| ARC-B | SUNDAY:6 |
| ARC-B | SUNDAY:7 |
| BR-A | MONDAY:1 |
| BR-A | MONDAY:2 |
| BR-A | MONDAY:3 |
| BR-A | MONDAY:4 |
| BR-A | MONDAY:5 |
| BR-A | MONDAY:6 |
| BR-A | MONDAY:7 |
| BR-A | TUESDAY:1 |
| BR-A | TUESDAY:2 |
| BR-A | TUESDAY:3 |
| BR-A | TUESDAY:4 |
| BR-A | TUESDAY:5 |
| BR-A | TUESDAY:6 |
| BR-A | TUESDAY:7 |
| BR-A | WEDNESDAY:1 |
| BR-A | WEDNESDAY:2 |
| BR-A | WEDNESDAY:3 |
| BR-A | WEDNESDAY:4 |
| BR-A | WEDNESDAY:5 |
| BR-A | WEDNESDAY:6 |
| BR-A | WEDNESDAY:7 |
| BR-A | THURSDAY:1 |
| BR-A | THURSDAY:2 |
| BR-A | THURSDAY:3 |
| BR-A | THURSDAY:4 |
| BR-A | THURSDAY:5 |
| BR-A | THURSDAY:6 |
| BR-A | THURSDAY:7 |
| BR-A | FRIDAY:1 |
| BR-A | FRIDAY:2 |
| BR-A | FRIDAY:3 |
| BR-A | FRIDAY:4 |
| BR-A | FRIDAY:5 |
| BR-A | FRIDAY:6 |
| BR-A | FRIDAY:7 |
| BR-A | SATURDAY:1 |
| BR-A | SATURDAY:2 |
| BR-A | SATURDAY:3 |
| BR-A | SATURDAY:4 |
| BR-A | SATURDAY:5 |
| BR-A | SATURDAY:6 |
| BR-A | SATURDAY:7 |
| BR-A | SUNDAY:1 |
| BR-A | SUNDAY:2 |
| BR-A | SUNDAY:3 |
| BR-A | SUNDAY:4 |
| BR-A | SUNDAY:5 |
| BR-A | SUNDAY:6 |
| BR-A | SUNDAY:7 |
| BR-B | MONDAY:1 |
| BR-B | MONDAY:2 |
| BR-B | MONDAY:3 |
| BR-B | MONDAY:4 |
| BR-B | MONDAY:5 |
| BR-B | MONDAY:6 |
| BR-B | MONDAY:7 |
| BR-B | TUESDAY:1 |
| BR-B | TUESDAY:2 |
| BR-B | TUESDAY:3 |
| BR-B | TUESDAY:4 |
| BR-B | TUESDAY:5 |
| BR-B | TUESDAY:6 |
| BR-B | TUESDAY:7 |
| BR-B | WEDNESDAY:1 |
| BR-B | WEDNESDAY:2 |
| BR-B | WEDNESDAY:3 |
| BR-B | WEDNESDAY:4 |
| BR-B | WEDNESDAY:5 |
| BR-B | WEDNESDAY:6 |
| BR-B | WEDNESDAY:7 |
| BR-B | THURSDAY:1 |
| BR-B | THURSDAY:2 |
| BR-B | THURSDAY:3 |
| BR-B | THURSDAY:4 |
| BR-B | THURSDAY:5 |
| BR-B | THURSDAY:6 |
| BR-B | THURSDAY:7 |
| BR-B | FRIDAY:1 |
| BR-B | FRIDAY:2 |
| BR-B | FRIDAY:3 |
| BR-B | FRIDAY:4 |
| BR-B | FRIDAY:5 |
| BR-B | FRIDAY:6 |
| BR-B | FRIDAY:7 |
| BR-B | SATURDAY:1 |
| BR-B | SATURDAY:2 |
| BR-B | SATURDAY:3 |
| BR-B | SATURDAY:4 |
| BR-B | SATURDAY:5 |
| BR-B | SATURDAY:6 |
| BR-B | SATURDAY:7 |
| BR-B | SUNDAY:1 |
| BR-B | SUNDAY:2 |
| BR-B | SUNDAY:3 |
| BR-B | SUNDAY:4 |
| BR-B | SUNDAY:5 |
| BR-B | SUNDAY:6 |
| BR-B | SUNDAY:7 |
| FOOT-A | MONDAY:1 |
| FOOT-A | MONDAY:2 |
| FOOT-A | MONDAY:3 |
| FOOT-A | MONDAY:4 |
| FOOT-A | MONDAY:5 |
| FOOT-A | MONDAY:6 |
| FOOT-A | MONDAY:7 |
| FOOT-A | TUESDAY:1 |
| FOOT-A | TUESDAY:2 |
| FOOT-A | TUESDAY:3 |
| FOOT-A | TUESDAY:4 |
| FOOT-A | TUESDAY:5 |
| FOOT-A | TUESDAY:6 |
| FOOT-A | TUESDAY:7 |
| FOOT-A | WEDNESDAY:1 |
| FOOT-A | WEDNESDAY:2 |
| FOOT-A | WEDNESDAY:3 |
| FOOT-A | WEDNESDAY:4 |
| FOOT-A | WEDNESDAY:5 |
| FOOT-A | WEDNESDAY:6 |
| FOOT-A | WEDNESDAY:7 |
| FOOT-A | THURSDAY:1 |
| FOOT-A | THURSDAY:2 |
| FOOT-A | THURSDAY:3 |
| FOOT-A | THURSDAY:4 |
| FOOT-A | THURSDAY:5 |
| FOOT-A | THURSDAY:6 |
| FOOT-A | THURSDAY:7 |
| FOOT-A | FRIDAY:1 |
| FOOT-A | FRIDAY:2 |
| FOOT-A | FRIDAY:3 |
| FOOT-A | FRIDAY:4 |
| FOOT-A | FRIDAY:5 |
| FOOT-A | FRIDAY:6 |
| FOOT-A | FRIDAY:7 |
| FOOT-A | SATURDAY:1 |
| FOOT-A | SATURDAY:2 |
| FOOT-A | SATURDAY:3 |
| FOOT-A | SATURDAY:4 |
| FOOT-A | SATURDAY:5 |
| FOOT-A | SATURDAY:6 |
| FOOT-A | SATURDAY:7 |
| FOOT-A | SUNDAY:1 |
| FOOT-A | SUNDAY:2 |
| FOOT-A | SUNDAY:3 |
| FOOT-A | SUNDAY:4 |
| FOOT-A | SUNDAY:5 |
| FOOT-A | SUNDAY:6 |
| FOOT-A | SUNDAY:7 |
| FOOT-B | MONDAY:1 |
| FOOT-B | MONDAY:2 |
| FOOT-B | MONDAY:3 |
| FOOT-B | MONDAY:4 |
| FOOT-B | MONDAY:5 |
| FOOT-B | MONDAY:6 |
| FOOT-B | MONDAY:7 |
| FOOT-B | TUESDAY:1 |
| FOOT-B | TUESDAY:2 |
| FOOT-B | TUESDAY:3 |
| FOOT-B | TUESDAY:4 |
| FOOT-B | TUESDAY:5 |
| FOOT-B | TUESDAY:6 |
| FOOT-B | TUESDAY:7 |
| FOOT-B | WEDNESDAY:1 |
| FOOT-B | WEDNESDAY:2 |
| FOOT-B | WEDNESDAY:3 |
| FOOT-B | WEDNESDAY:4 |
| FOOT-B | WEDNESDAY:5 |
| FOOT-B | WEDNESDAY:6 |
| FOOT-B | WEDNESDAY:7 |
| FOOT-B | THURSDAY:1 |
| FOOT-B | THURSDAY:2 |
| FOOT-B | THURSDAY:3 |
| FOOT-B | THURSDAY:4 |
| FOOT-B | THURSDAY:5 |
| FOOT-B | THURSDAY:6 |
| FOOT-B | THURSDAY:7 |
| FOOT-B | FRIDAY:1 |
| FOOT-B | FRIDAY:2 |
| FOOT-B | FRIDAY:3 |
| FOOT-B | FRIDAY:4 |
| FOOT-B | FRIDAY:5 |
| FOOT-B | FRIDAY:6 |
| FOOT-B | FRIDAY:7 |
| FOOT-B | SATURDAY:1 |
| FOOT-B | SATURDAY:2 |
| FOOT-B | SATURDAY:3 |
| FOOT-B | SATURDAY:4 |
| FOOT-B | SATURDAY:5 |
| FOOT-B | SATURDAY:6 |
| FOOT-B | SATURDAY:7 |
| FOOT-B | SUNDAY:1 |
| FOOT-B | SUNDAY:2 |
| FOOT-B | SUNDAY:3 |
| FOOT-B | SUNDAY:4 |
| FOOT-B | SUNDAY:5 |
| FOOT-B | SUNDAY:6 |
| FOOT-B | SUNDAY:7 |
| BC-A | MONDAY:1 |
| BC-A | MONDAY:2 |
| BC-A | MONDAY:3 |
| BC-A | MONDAY:4 |
| BC-A | MONDAY:5 |
| BC-A | MONDAY:6 |
| BC-A | MONDAY:7 |
| BC-A | TUESDAY:1 |
| BC-A | TUESDAY:2 |
| BC-A | TUESDAY:3 |
| BC-A | TUESDAY:4 |
| BC-A | TUESDAY:5 |
| BC-A | TUESDAY:6 |
| BC-A | TUESDAY:7 |
| BC-A | WEDNESDAY:1 |
| BC-A | WEDNESDAY:2 |
| BC-A | WEDNESDAY:3 |
| BC-A | WEDNESDAY:4 |
| BC-A | WEDNESDAY:5 |
| BC-A | WEDNESDAY:6 |
| BC-A | WEDNESDAY:7 |
| BC-A | THURSDAY:1 |
| BC-A | THURSDAY:2 |
| BC-A | THURSDAY:3 |
| BC-A | THURSDAY:4 |
| BC-A | THURSDAY:5 |
| BC-A | THURSDAY:6 |
| BC-A | THURSDAY:7 |
| BC-A | FRIDAY:1 |
| BC-A | FRIDAY:2 |
| BC-A | FRIDAY:3 |
| BC-A | FRIDAY:4 |
| BC-A | FRIDAY:5 |
| BC-A | FRIDAY:6 |
| BC-A | FRIDAY:7 |
| BC-A | SATURDAY:1 |
| BC-A | SATURDAY:2 |
| BC-A | SATURDAY:3 |
| BC-A | SATURDAY:4 |
| BC-A | SATURDAY:5 |
| BC-A | SATURDAY:6 |
| BC-A | SATURDAY:7 |
| BC-A | SUNDAY:1 |
| BC-A | SUNDAY:2 |
| BC-A | SUNDAY:3 |
| BC-A | SUNDAY:4 |
| BC-A | SUNDAY:5 |
| BC-A | SUNDAY:6 |
| BC-A | SUNDAY:7 |
| BC-B | MONDAY:1 |
| BC-B | MONDAY:2 |
| BC-B | MONDAY:3 |
| BC-B | MONDAY:4 |
| BC-B | MONDAY:5 |
| BC-B | MONDAY:6 |
| BC-B | MONDAY:7 |
| BC-B | TUESDAY:1 |
| BC-B | TUESDAY:2 |
| BC-B | TUESDAY:3 |
| BC-B | TUESDAY:4 |
| BC-B | TUESDAY:5 |
| BC-B | TUESDAY:6 |
| BC-B | TUESDAY:7 |
| BC-B | WEDNESDAY:1 |
| BC-B | WEDNESDAY:2 |
| BC-B | WEDNESDAY:3 |
| BC-B | WEDNESDAY:4 |
| BC-B | WEDNESDAY:5 |
| BC-B | WEDNESDAY:6 |
| BC-B | WEDNESDAY:7 |
| BC-B | THURSDAY:1 |
| BC-B | THURSDAY:2 |
| BC-B | THURSDAY:3 |
| BC-B | THURSDAY:4 |
| BC-B | THURSDAY:5 |
| BC-B | THURSDAY:6 |
| BC-B | THURSDAY:7 |
| BC-B | FRIDAY:1 |
| BC-B | FRIDAY:2 |
| BC-B | FRIDAY:3 |
| BC-B | FRIDAY:4 |
| BC-B | FRIDAY:5 |
| BC-B | FRIDAY:6 |
| BC-B | FRIDAY:7 |
| BC-B | SATURDAY:1 |
| BC-B | SATURDAY:2 |
| BC-B | SATURDAY:3 |
| BC-B | SATURDAY:4 |
| BC-B | SATURDAY:5 |
| BC-B | SATURDAY:6 |
| BC-B | SATURDAY:7 |
| BC-B | SUNDAY:1 |
| BC-B | SUNDAY:2 |
| BC-B | SUNDAY:3 |
| BC-B | SUNDAY:4 |
| BC-B | SUNDAY:5 |
| BC-B | SUNDAY:6 |
| BC-B | SUNDAY:7 |
| BAD-A | MONDAY:1 |
| BAD-A | MONDAY:2 |
| BAD-A | MONDAY:3 |
| BAD-A | MONDAY:4 |
| BAD-A | MONDAY:5 |
| BAD-A | MONDAY:6 |
| BAD-A | MONDAY:7 |
| BAD-A | TUESDAY:1 |
| BAD-A | TUESDAY:2 |
| BAD-A | TUESDAY:3 |
| BAD-A | TUESDAY:4 |
| BAD-A | TUESDAY:5 |
| BAD-A | TUESDAY:6 |
| BAD-A | TUESDAY:7 |
| BAD-A | WEDNESDAY:1 |
| BAD-A | WEDNESDAY:2 |
| BAD-A | WEDNESDAY:3 |
| BAD-A | WEDNESDAY:4 |
| BAD-A | WEDNESDAY:5 |
| BAD-A | WEDNESDAY:6 |
| BAD-A | WEDNESDAY:7 |
| BAD-A | THURSDAY:1 |
| BAD-A | THURSDAY:2 |
| BAD-A | THURSDAY:3 |
| BAD-A | THURSDAY:4 |
| BAD-A | THURSDAY:5 |
| BAD-A | THURSDAY:6 |
| BAD-A | THURSDAY:7 |
| BAD-A | FRIDAY:1 |
| BAD-A | FRIDAY:2 |
| BAD-A | FRIDAY:3 |
| BAD-A | FRIDAY:4 |
| BAD-A | FRIDAY:5 |
| BAD-A | FRIDAY:6 |
| BAD-A | FRIDAY:7 |
| BAD-A | SATURDAY:1 |
| BAD-A | SATURDAY:2 |
| BAD-A | SATURDAY:3 |
| BAD-A | SATURDAY:4 |
| BAD-A | SATURDAY:5 |
| BAD-A | SATURDAY:6 |
| BAD-A | SATURDAY:7 |
| BAD-A | SUNDAY:1 |
| BAD-A | SUNDAY:2 |
| BAD-A | SUNDAY:3 |
| BAD-A | SUNDAY:4 |
| BAD-A | SUNDAY:5 |
| BAD-A | SUNDAY:6 |
| BAD-A | SUNDAY:7 |
| BAD-B | MONDAY:1 |
| BAD-B | MONDAY:2 |
| BAD-B | MONDAY:3 |
| BAD-B | MONDAY:4 |
| BAD-B | MONDAY:5 |
| BAD-B | MONDAY:6 |
| BAD-B | MONDAY:7 |
| BAD-B | TUESDAY:1 |
| BAD-B | TUESDAY:2 |
| BAD-B | TUESDAY:3 |
| BAD-B | TUESDAY:4 |
| BAD-B | TUESDAY:5 |
| BAD-B | TUESDAY:6 |
| BAD-B | TUESDAY:7 |
| BAD-B | WEDNESDAY:1 |
| BAD-B | WEDNESDAY:2 |
| BAD-B | WEDNESDAY:3 |
| BAD-B | WEDNESDAY:4 |
| BAD-B | WEDNESDAY:5 |
| BAD-B | WEDNESDAY:6 |
| BAD-B | WEDNESDAY:7 |
| BAD-B | THURSDAY:1 |
| BAD-B | THURSDAY:2 |
| BAD-B | THURSDAY:3 |
| BAD-B | THURSDAY:4 |
| BAD-B | THURSDAY:5 |
| BAD-B | THURSDAY:6 |
| BAD-B | THURSDAY:7 |
| BAD-B | FRIDAY:1 |
| BAD-B | FRIDAY:2 |
| BAD-B | FRIDAY:3 |
| BAD-B | FRIDAY:4 |
| BAD-B | FRIDAY:5 |
| BAD-B | FRIDAY:6 |
| BAD-B | FRIDAY:7 |
| BAD-B | SATURDAY:1 |
| BAD-B | SATURDAY:2 |
| BAD-B | SATURDAY:3 |
| BAD-B | SATURDAY:4 |
| BAD-B | SATURDAY:5 |
| BAD-B | SATURDAY:6 |
| BAD-B | SATURDAY:7 |
| BAD-B | SUNDAY:1 |
| BAD-B | SUNDAY:2 |
| BAD-B | SUNDAY:3 |
| BAD-B | SUNDAY:4 |
| BAD-B | SUNDAY:5 |
| BAD-B | SUNDAY:6 |
| BAD-B | SUNDAY:7 |
| TT-A | MONDAY:1 |
| TT-A | MONDAY:2 |
| TT-A | MONDAY:3 |
| TT-A | MONDAY:4 |
| TT-A | MONDAY:5 |
| TT-A | MONDAY:6 |
| TT-A | MONDAY:7 |
| TT-A | TUESDAY:1 |
| TT-A | TUESDAY:2 |
| TT-A | TUESDAY:3 |
| TT-A | TUESDAY:4 |
| TT-A | TUESDAY:5 |
| TT-A | TUESDAY:6 |
| TT-A | TUESDAY:7 |
| TT-A | WEDNESDAY:1 |
| TT-A | WEDNESDAY:2 |
| TT-A | WEDNESDAY:3 |
| TT-A | WEDNESDAY:4 |
| TT-A | WEDNESDAY:5 |
| TT-A | WEDNESDAY:6 |
| TT-A | WEDNESDAY:7 |
| TT-A | THURSDAY:1 |
| TT-A | THURSDAY:2 |
| TT-A | THURSDAY:3 |
| TT-A | THURSDAY:4 |
| TT-A | THURSDAY:5 |
| TT-A | THURSDAY:6 |
| TT-A | THURSDAY:7 |
| TT-A | FRIDAY:1 |
| TT-A | FRIDAY:2 |
| TT-A | FRIDAY:3 |
| TT-A | FRIDAY:4 |
| TT-A | FRIDAY:5 |
| TT-A | FRIDAY:6 |
| TT-A | FRIDAY:7 |
| TT-A | SATURDAY:1 |
| TT-A | SATURDAY:2 |
| TT-A | SATURDAY:3 |
| TT-A | SATURDAY:4 |
| TT-A | SATURDAY:5 |
| TT-A | SATURDAY:6 |
| TT-A | SATURDAY:7 |
| TT-A | SUNDAY:1 |
| TT-A | SUNDAY:2 |
| TT-A | SUNDAY:3 |
| TT-A | SUNDAY:4 |
| TT-A | SUNDAY:5 |
| TT-A | SUNDAY:6 |
| TT-A | SUNDAY:7 |
| TT-B | MONDAY:1 |
| TT-B | MONDAY:2 |
| TT-B | MONDAY:3 |
| TT-B | MONDAY:4 |
| TT-B | MONDAY:5 |
| TT-B | MONDAY:6 |
| TT-B | MONDAY:7 |
| TT-B | TUESDAY:1 |
| TT-B | TUESDAY:2 |
| TT-B | TUESDAY:3 |
| TT-B | TUESDAY:4 |
| TT-B | TUESDAY:5 |
| TT-B | TUESDAY:6 |
| TT-B | TUESDAY:7 |
| TT-B | WEDNESDAY:1 |
| TT-B | WEDNESDAY:2 |
| TT-B | WEDNESDAY:3 |
| TT-B | WEDNESDAY:4 |
| TT-B | WEDNESDAY:5 |
| TT-B | WEDNESDAY:6 |
| TT-B | WEDNESDAY:7 |
| TT-B | THURSDAY:1 |
| TT-B | THURSDAY:2 |
| TT-B | THURSDAY:3 |
| TT-B | THURSDAY:4 |
| TT-B | THURSDAY:5 |
| TT-B | THURSDAY:6 |
| TT-B | THURSDAY:7 |
| TT-B | FRIDAY:1 |
| TT-B | FRIDAY:2 |
| TT-B | FRIDAY:3 |
| TT-B | FRIDAY:4 |
| TT-B | FRIDAY:5 |
| TT-B | FRIDAY:6 |
| TT-B | FRIDAY:7 |
| TT-B | SATURDAY:1 |
| TT-B | SATURDAY:2 |
| TT-B | SATURDAY:3 |
| TT-B | SATURDAY:4 |
| TT-B | SATURDAY:5 |
| TT-B | SATURDAY:6 |
| TT-B | SATURDAY:7 |
| TT-B | SUNDAY:1 |
| TT-B | SUNDAY:2 |
| TT-B | SUNDAY:3 |
| TT-B | SUNDAY:4 |
| TT-B | SUNDAY:5 |
| TT-B | SUNDAY:6 |
| TT-B | SUNDAY:7 |

### 19. `coach_work_schedules`

| coach | accepting_new |
| --- | --- |
| TEN-A | true |
| TEN-B | true |
| SWIM-A | true |
| SWIM-B | true |
| PICK-A | true |
| PICK-B | true |
| ARC-A | true |
| ARC-B | true |
| BR-A | true |
| BR-B | true |
| FOOT-A | true |
| FOOT-B | true |
| BC-A | true |
| BC-B | true |
| BAD-A | true |
| BAD-B | true |
| TT-A | true |
| TT-B | true |

### 20. `subscription_pauses`

Tạo từ incident/ngày nghỉ có nguồn khi chạy fixture Advanced; không thêm pause độc lập để làm đẹp deadline.

Chưa có dòng tĩnh trong bản thiết kế này.


### 21. `normal_class_requests`

Mã REQ-* là key manifest; request_id ở bảng là UUID ổn định cho API. Request hash chỉ sinh từ payload chính xác khi thực hiện tạo lớp, không đoán trong tài liệu.

| key | sports_class | center | request_id |
| --- | --- | --- | --- |
| REQ-N01 | N01 | C01 | 8a3a9d44-c79c-5a11-8a1f-51f983586659 |
| REQ-N02 | N02 | C01 | a55e61c0-db4e-577a-b450-4160c9f08348 |
| REQ-N03 | N03 | C01 | 710b0fe2-2dc7-5b3d-92ef-b69a525d0958 |
| REQ-N04 | N04 | C01 | 71a21e30-d250-523b-a76b-1426656a2b4d |
| REQ-N05 | N05 | C01 | e9910b14-7b92-555a-8b8a-478e5970f8a9 |
| REQ-N06 | N06 | C01 | 32bbf456-13f8-53e7-bb07-cc2379efe37f |
| REQ-N07 | N07 | C01 | f6ac4c7a-b8aa-5219-adc4-bf249f738c47 |
| REQ-N08 | N08 | C01 | 64163eea-d4e5-5b18-af11-8d08b234ab31 |
| REQ-N09 | N09 | C01 | 2ec6a405-3295-5a15-a007-544582ce2255 |
| REQ-N10 | N10 | C01 | c83fbe5c-83e1-5896-8253-ebc002999f16 |
| REQ-N11 | N11 | C01 | 4bbf35e1-17f1-5311-904a-29bd93c52aea |
| REQ-N12 | N12 | C01 | 360069f1-11b5-5359-8783-aea0109eaf1a |

### 22. `system_logs`

| key | action | actor | target | center | category | scope_source | detail |
| --- | --- | --- | --- | --- | --- | --- | --- |
| LOG01 | CANCEL_NORMAL_PRESTART | CM1 | N06 | C01 | BUSINESS | TARGET_AT_EVENT | DEMO; không chứa số tài khoản/file URL/phone |
| LOG02 | APPROVE_NORMAL_REFUND | CM1 | RF04 | C01 | BUSINESS | TARGET_AT_EVENT | DEMO; không chứa số tài khoản/file URL/phone |
| LOG03 | SUBMIT_REFUND_PROOF | R1 | RF05 | C01 | BUSINESS | TARGET_AT_EVENT | DEMO; không chứa số tài khoản/file URL/phone |
| LOG04 | VERIFY_NORMAL_REFUND | CM2 | RF06 | C01 | BUSINESS | TARGET_AT_EVENT | DEMO; không chứa số tài khoản/file URL/phone |
| LOG05 | UPDATE_ROLE_PERMISSIONS | ADM | Coach | NULL | SYSTEM_SECURITY | GLOBAL_CONFIGURATION_AT_EVENT | DEMO Admin gửi lại tập quyền Coach baseline không đổi; log sẽ sinh bởi API |

### 23. `ai_consultations`

Rỗng: AI chưa hoàn tất, không tự tạo lịch sử như thể provider đã chạy.

Chưa có dòng tĩnh trong bản thiết kế này.


### 24. `chat_read_state`

| conversation | user | last_read |
| --- | --- | --- |
| CHAT01 | M08 | MSG01 |
| CHAT01 | R1 | MSG02 |

### 25. `chat_messages`

| key | conversation | sender | content |
| --- | --- | --- | --- |
| MSG01 | CHAT01 | M08 | DEMO tôi muốn kiểm tra khoản hoàn RF06 |
| MSG02 | CHAT01 | R1 | DEMO trung tâm đã tiếp nhận để kiểm tra lại giao dịch |

### 26. `chat_conversations`

| key | member | assigned_staff |
| --- | --- | --- |
| CHAT01 | M08 | R1 |

### 27. `notification_recipients`

| notification | user | is_read |
| --- | --- | --- |
| NT01 | M01 | false |
| NT02 | M04 | false |
| NT03 | M08 | true |

### 28. `notifications`

| key | user | event | related | is_read | content |
| --- | --- | --- | --- | --- | --- |
| NT01 | M01 | TRANSFER_OFFER | EV-N05 | false | DEMO đề nghị chuyển N05→N11; chờ lựa chọn Member, không tự đồng ý |
| NT02 | M04 | REFUND_AWAITING_RECEIVER | RF02 | false | DEMO trung tâm hủy lớp; đã có nghĩa vụ hoàn, chờ thông tin nhận |
| NT03 | M08 | REFUND_COMPLETED | RF06 | true | DEMO đã xác minh hoàn900000đ; tham chiếu DEMO-TRANSFER-RF06 |

### 29. `support_requests`

Rỗng: chưa có workflow ticket được nghiệm thu; dùng chat hiện có cho khiếu nại demo.

Chưa có dòng tĩnh trong bản thiết kế này.


### 30. `vnpay_payment_attempts`

Rỗng ban đầu. Chỉ sinh khi tạo link pay đúng workflow; không backfill metadata/paydate hoặc bật QueryDR để lấp dữ liệu.

Chưa có dòng tĩnh trong bản thiết kế này.


### 31. `invoices`

key ở đây là mã manifest, không phải invoiceCode gửi gateway. InvoiceCode thật do sequence không tái dùng cấp. Tất cả khoản này là dữ liệu mô phỏng được duyệt, không có thu/chuyển tiền thật hoặc callback gateway thật.

| key | member | sports_class | subscription | amount | method | status | created_at | payment_date | receptionist | reconciliation_required | cash_confirmation_reversed | gateway_ref |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| INV-N01-M01 | M01 | N01 | NULL | 900000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N02-M04 | M04 | N02 | NULL | 2500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N02-M05 | M05 | N02 | NULL | 2500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N02-M06 | M06 | N02 | NULL | 2500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M07 | M07 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M08 | M08 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M09 | M09 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M10 | M10 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M11 | M11 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N03-M12 | M12 | N03 | NULL | 1500000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N04-M01 | M01 | N04 | NULL | 2000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N04-M02 | M02 | N04 | NULL | 2000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N04-M03 | M03 | N04 | NULL | 2000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N05-M01 | M01 | N05 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N05-M02 | M02 | N05 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N05-M03 | M03 | N05 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N06-M04 | M04 | N06 | NULL | 1200000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N06-M05 | M05 | N06 | NULL | 1200000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N06-M06 | M06 | N06 | NULL | 1200000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M07 | M07 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M08 | M08 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M09 | M09 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M10 | M10 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M11 | M11 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M12 | M12 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M13 | M13 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M14 | M14 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M15 | M15 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M16 | M16 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M17 | M17 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M18 | M18 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M19 | M19 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N07-M20 | M20 | N07 | NULL | 1000000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N08-M01 | M01 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N08-M02 | M02 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N08-M03 | M03 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N08-M04 | M04 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N08-M05 | M05 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N08-M06 | M06 | N08 | NULL | 1000000 | Cash | Paid | 2026-11-03T08:00 | 2026-11-03T08:01 | R1 | false | false | NULL |
| INV-N09-M01 | M01 | N09 | NULL | 800000 | Cash | Paid | 2026-09-25T08:00 | 2026-09-25T08:01 | R1 | false | false | NULL |
| INV-N09-M02 | M02 | N09 | NULL | 800000 | Cash | Paid | 2026-09-25T08:00 | 2026-09-25T08:01 | R1 | false | false | NULL |
| INV-N10-M01 | M01 | N10 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-N10-M02 | M02 | N10 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-N10-M03 | M03 | N10 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-N11-M11 | M11 | N11 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N11-M12 | M12 | N11 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N11-M13 | M13 | N11 | NULL | 1800000 | Cash | Paid | 2026-11-14T08:00 | 2026-11-14T08:01 | R1 | false | false | NULL |
| INV-N12-M07 | M07 | N12 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-N12-M08 | M08 | N12 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-N12-M09 | M09 | N12 | NULL | 900000 | Cash | Paid | 2026-10-27T08:00 | 2026-10-27T08:01 | R1 | false | false | NULL |
| INV-SUB01 | M02 | NULL | SUB01 | 8000000 | Cash | Paid | 2026-11-17T07:58 | 2026-11-17T08:00 | R1 | false | false | NULL |
| INV-SUB02 | M03 | NULL | SUB02 | 8000000 | Cash | Paid | 2026-11-16T07:58 | 2026-11-16T08:00 | R1 | false | false | NULL |
| INV-SUB03 | M04 | NULL | SUB03 | 3000000 | Cash | Paid | 2026-11-15T07:58 | 2026-11-15T08:00 | R1 | false | false | NULL |
| INV-SUB04 | M05 | NULL | SUB04 | 8000000 | Cash | Paid | 2026-10-12T07:58 | 2026-10-12T08:00 | R1 | false | false | NULL |
| INV-SUB05 | M06 | NULL | SUB05 | 8000000 | Cash | Paid | 2026-09-12T07:58 | 2026-09-12T08:00 | R1 | false | false | NULL |
| INV-SUB06 | M07 | NULL | SUB06 | 3000000 | Cash | Paid | 2026-11-19T07:58 | 2026-11-19T08:00 | R1 | false | false | NULL |
| INV-SUB07 | M08 | NULL | SUB07 | 4500000 | VNPay | Paid | 2026-11-18T07:50 | 2026-11-18T08:00 | NULL | true | false | DEMO-LATE-RECEIPT |

### 32. `holidays`

| key | date | name | tag | is_active |
| --- | --- | --- | --- | --- |
| H01 | 2027-01-10 | DEMO trung tâm đóng cửa | CENTER_EVENT | true |
| HC01 | 2026-01-01 | Tết Dương lịch | NATIONAL_FIXED | false |
| HC02 | 2026-02-16 | Tết Nguyên đán — 29 tháng Chạp | LUNAR_FESTIVAL | false |
| HC03 | 2026-02-17 | Tết Nguyên đán — Mùng 1 | LUNAR_FESTIVAL | false |
| HC04 | 2026-02-18 | Tết Nguyên đán — Mùng 2 | LUNAR_FESTIVAL | false |
| HC05 | 2026-02-19 | Tết Nguyên đán — Mùng 3 | LUNAR_FESTIVAL | false |
| HC06 | 2026-02-20 | Tết Nguyên đán — Mùng 4 | LUNAR_FESTIVAL | false |
| HC07 | 2026-04-26 | Giỗ Tổ Hùng Vương | LUNAR_FESTIVAL | false |
| HC08 | 2026-04-30 | Ngày Giải phóng miền Nam | NATIONAL_FIXED | false |
| HC09 | 2026-05-01 | Ngày Quốc tế Lao động | NATIONAL_FIXED | false |
| HC10 | 2026-09-01 | Quốc khánh — Ngày liền kề mẫu | NATIONAL_FIXED | false |
| HC11 | 2026-09-02 | Quốc khánh | NATIONAL_FIXED | false |

### 33. `class_enrollments`

| key | member | sports_class | invoice | status | paid | enrolled_at |
| --- | --- | --- | --- | --- | --- | --- |
| N01-M01 | M01 | N01 | INV-N01-M01 | Registered | true | 2026-11-14T08:00 |
| N02-M04 | M04 | N02 | INV-N02-M04 | Registered | true | 2026-11-14T08:00 |
| N02-M05 | M05 | N02 | INV-N02-M05 | Registered | true | 2026-11-14T08:00 |
| N02-M06 | M06 | N02 | INV-N02-M06 | Registered | true | 2026-11-14T08:00 |
| N03-M07 | M07 | N03 | INV-N03-M07 | Registered | true | 2026-11-14T08:00 |
| N03-M08 | M08 | N03 | INV-N03-M08 | Registered | true | 2026-11-14T08:00 |
| N03-M09 | M09 | N03 | INV-N03-M09 | Registered | true | 2026-11-14T08:00 |
| N03-M10 | M10 | N03 | INV-N03-M10 | Registered | true | 2026-11-14T08:00 |
| N03-M11 | M11 | N03 | INV-N03-M11 | Registered | true | 2026-11-14T08:00 |
| N03-M12 | M12 | N03 | INV-N03-M12 | Registered | true | 2026-11-14T08:00 |
| N04-M01 | M01 | N04 | INV-N04-M01 | Registered | true | 2026-11-14T08:00 |
| N04-M02 | M02 | N04 | INV-N04-M02 | Registered | true | 2026-11-14T08:00 |
| N04-M03 | M03 | N04 | INV-N04-M03 | Registered | true | 2026-11-14T08:00 |
| N05-M01 | M01 | N05 | INV-N05-M01 | Registered | true | 2026-11-14T08:00 |
| N05-M02 | M02 | N05 | INV-N05-M02 | Registered | true | 2026-11-14T08:00 |
| N05-M03 | M03 | N05 | INV-N05-M03 | Registered | true | 2026-11-14T08:00 |
| N06-M04 | M04 | N06 | INV-N06-M04 | Cancelled | true | 2026-11-14T08:00 |
| N06-M05 | M05 | N06 | INV-N06-M05 | Cancelled | true | 2026-11-14T08:00 |
| N06-M06 | M06 | N06 | INV-N06-M06 | Cancelled | true | 2026-11-14T08:00 |
| N07-M07 | M07 | N07 | INV-N07-M07 | Registered | true | 2026-11-14T08:00 |
| N07-M08 | M08 | N07 | INV-N07-M08 | Registered | true | 2026-11-14T08:00 |
| N07-M09 | M09 | N07 | INV-N07-M09 | Registered | true | 2026-11-14T08:00 |
| N07-M10 | M10 | N07 | INV-N07-M10 | Registered | true | 2026-11-14T08:00 |
| N07-M11 | M11 | N07 | INV-N07-M11 | Registered | true | 2026-11-14T08:00 |
| N07-M12 | M12 | N07 | INV-N07-M12 | Registered | true | 2026-11-14T08:00 |
| N07-M13 | M13 | N07 | INV-N07-M13 | Registered | true | 2026-11-14T08:00 |
| N07-M14 | M14 | N07 | INV-N07-M14 | Registered | true | 2026-11-14T08:00 |
| N07-M15 | M15 | N07 | INV-N07-M15 | Registered | true | 2026-11-14T08:00 |
| N07-M16 | M16 | N07 | INV-N07-M16 | Registered | true | 2026-11-14T08:00 |
| N07-M17 | M17 | N07 | INV-N07-M17 | Registered | true | 2026-11-14T08:00 |
| N07-M18 | M18 | N07 | INV-N07-M18 | Registered | true | 2026-11-14T08:00 |
| N07-M19 | M19 | N07 | INV-N07-M19 | Registered | true | 2026-11-14T08:00 |
| N07-M20 | M20 | N07 | INV-N07-M20 | Registered | true | 2026-11-14T08:00 |
| N08-M01 | M01 | N08 | INV-N08-M01 | Registered | true | 2026-11-03T08:00 |
| N08-M02 | M02 | N08 | INV-N08-M02 | Registered | true | 2026-11-03T08:00 |
| N08-M03 | M03 | N08 | INV-N08-M03 | Registered | true | 2026-11-03T08:00 |
| N08-M04 | M04 | N08 | INV-N08-M04 | Registered | true | 2026-11-03T08:00 |
| N08-M05 | M05 | N08 | INV-N08-M05 | Registered | true | 2026-11-03T08:00 |
| N08-M06 | M06 | N08 | INV-N08-M06 | Registered | true | 2026-11-03T08:00 |
| N09-M01 | M01 | N09 | INV-N09-M01 | Completed | true | 2026-09-25T08:00 |
| N09-M02 | M02 | N09 | INV-N09-M02 | Completed | true | 2026-09-25T08:00 |
| N10-M01 | M01 | N10 | INV-N10-M01 | Registered | true | 2026-10-27T08:00 |
| N10-M02 | M02 | N10 | INV-N10-M02 | Registered | true | 2026-10-27T08:00 |
| N10-M03 | M03 | N10 | INV-N10-M03 | Registered | true | 2026-10-27T08:00 |
| N11-M11 | M11 | N11 | INV-N11-M11 | Registered | true | 2026-11-14T08:00 |
| N11-M12 | M12 | N11 | INV-N11-M12 | Registered | true | 2026-11-14T08:00 |
| N11-M13 | M13 | N11 | INV-N11-M13 | Registered | true | 2026-11-14T08:00 |
| N12-M07 | M07 | N12 | INV-N12-M07 | Cancelled | true | 2026-10-27T08:00 |
| N12-M08 | M08 | N12 | INV-N12-M08 | Cancelled | true | 2026-10-27T08:00 |
| N12-M09 | M09 | N12 | INV-N12-M09 | Cancelled | true | 2026-10-27T08:00 |

### 34. `member_packages`

Advanced theo D03/D10:12credit chỉ sau Paid hợp lệ, còn0credit nhưng có buổi chưa học vẫn Active. SUB07 nhận tiền muộn cần đối soát, không cấp quyền. Các dòng là kế hoạch fixture, cần thực hiện/kiểm qua workflow trước seed.

| key | member | subject | primary_coach | package | status | remaining_credit | purchase_amount | activated_at | expires_at | validity_days | note |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SUB01 | M02 | TEN | TEN-A | NULL | Active | 11 | 8000000 | 2026-11-17T08:00 | 2027-01-01T08:00 | 45 | NULL |
| SUB02 | M03 | TEN | TEN-A | NULL | Active | 10 | 8000000 | 2026-11-16T08:00 | 2026-12-31T08:00 | 45 | NULL |
| SUB03 | M04 | BAD | BAD-A | NULL | Active | 0 | 3000000 | 2026-11-15T08:00 | 2026-12-30T08:00 | 45 | NULL |
| SUB04 | M05 | TEN | TEN-A | NULL | Completed | 0 | 8000000 | 2026-10-12T08:00 | 2026-11-26T08:00 | 45 | NULL |
| SUB05 | M06 | TEN | TEN-A | NULL | Expired | 0 | 8000000 | 2026-09-12T08:00 | 2026-10-27T08:00 | 45 | NULL |
| SUB06 | M07 | BAD | BAD-A | NULL | Active | 12 | 3000000 | 2026-11-19T08:00 | 2027-01-03T08:00 | 45 | DEMO Cash đã ghi nhận nhầm nhưng chưa nhận tiền |
| SUB07 | M08 | SWIM | SWIM-A | NULL | Cancelled | 0 | 4500000 | NULL | NULL | 45 | NULL |

### 35. `membership_packages`

Rỗng và bị chặn ghi theo mô hình retire; không có sản phẩm tháng/VIP.

Chưa có dòng tĩnh trong bản thiết kế này.


### 36. `class_feedbacks`

| key | member | sports_class | rating | comment |
| --- | --- | --- | --- | --- |
| FB-N09-M01 | M01 | N09 | 4 | DEMO phản hồi khóa đã hoàn tất |
| FB-N09-M02 | M02 | N09 | 4 | DEMO phản hồi khóa đã hoàn tất |

### 37. `evaluations`

| key | member | coach | date | score | comment |
| --- | --- | --- | --- | --- | --- |
| EVAL-N09-M01 | M01 | TT-A | 2026-11-06 | 8 | DEMO tiến bộ sau khóa Bóng bàn |
| EVAL-N09-M02 | M02 | TT-A | 2026-11-06 | 8 | DEMO tiến bộ sau khóa Bóng bàn |

### 38. `attendances`

| key | session | member | state | note | check_in_time | absence_notice | superseded_at | recorded_by |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ATT-SUB04-01 | SUB04-S01 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-02 | SUB04-S02 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-03 | SUB04-S03 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-04 | SUB04-S04 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-05 | SUB04-S05 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-06 | SUB04-S06 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-07 | SUB04-S07 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-08 | SUB04-S08 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-09 | SUB04-S09 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-10 | SUB04-S10 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-11 | SUB04-S11 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB04-12 | SUB04-S12 | M05 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB05-01 | SUB05-S01 | M06 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-SUB05-02 | SUB05-S02 | M06 | Present | NULL | NULL | NULL | NULL | TEN-A |
| ATT-N08-S01-M01 | N08-S01 | M01 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S01-M02 | N08-S01 | M02 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S01-M03 | N08-S01 | M03 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S01-M04 | N08-S01 | M04 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S01-M05 | N08-S01 | M05 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S01-M06 | N08-S01 | M06 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M01 | N08-S02 | M01 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M02 | N08-S02 | M02 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M03 | N08-S02 | M03 | Absent | DEMO cần ôn lại kỹ thuật | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M04 | N08-S02 | M04 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M05 | N08-S02 | M05 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S02-M06 | N08-S02 | M06 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M01 | N08-S03 | M01 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M02 | N08-S03 | M02 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M03 | N08-S03 | M03 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M04 | N08-S03 | M04 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M05 | N08-S03 | M05 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S03-M06 | N08-S03 | M06 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M01 | N08-S04 | M01 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M02 | N08-S04 | M02 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M03 | N08-S04 | M03 | Absent | DEMO cần ôn lại kỹ thuật | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M04 | N08-S04 | M04 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M05 | N08-S04 | M05 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S04-M06 | N08-S04 | M06 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M01 | N08-S05 | M01 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M02 | N08-S05 | M02 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M03 | N08-S05 | M03 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M04 | N08-S05 | M04 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M05 | N08-S05 | M05 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N08-S05-M06 | N08-S05 | M06 | Present | NULL | NULL | NULL | NULL | BC-A |
| ATT-N09-S01-M01 | N09-S01 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S01-M02 | N09-S01 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S02-M01 | N09-S02 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S02-M02 | N09-S02 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S03-M01 | N09-S03 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S03-M02 | N09-S03 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S04-M01 | N09-S04 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S04-M02 | N09-S04 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S05-M01 | N09-S05 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S05-M02 | N09-S05 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S06-M01 | N09-S06 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S06-M02 | N09-S06 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S07-M01 | N09-S07 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S07-M02 | N09-S07 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S08-M01 | N09-S08 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S08-M02 | N09-S08 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S09-M01 | N09-S09 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S09-M02 | N09-S09 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S10-M01 | N09-S10 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S10-M02 | N09-S10 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S11-M01 | N09-S11 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S11-M02 | N09-S11 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S12-M01 | N09-S12 | M01 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N09-S12-M02 | N09-S12 | M02 | Present | NULL | NULL | NULL | NULL | TT-A |
| ATT-N10-S01-M01 | N10-S01 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S01-M02 | N10-S01 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S01-M03 | N10-S01 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S02-M01 | N10-S02 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S02-M02 | N10-S02 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S02-M03 | N10-S02 | M03 | Absent | DEMO cần ôn lại kỹ thuật | NULL | NULL | NULL | BAD-A |
| ATT-N10-S03-M01 | N10-S03 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S03-M02 | N10-S03 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S03-M03 | N10-S03 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S04-M01 | N10-S04 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S04-M02 | N10-S04 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S04-M03 | N10-S04 | M03 | Absent | DEMO cần ôn lại kỹ thuật | NULL | NULL | NULL | BAD-A |
| ATT-N10-S05-M01 | N10-S05 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S05-M02 | N10-S05 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S05-M03 | N10-S05 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S06-M01 | N10-S06 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S06-M02 | N10-S06 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S06-M03 | N10-S06 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S07-M01 | N10-S07 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S07-M02 | N10-S07 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S07-M03 | N10-S07 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S08-M01 | N10-S08 | M01 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S08-M02 | N10-S08 | M02 | Present | NULL | NULL | NULL | NULL | BAD-A |
| ATT-N10-S08-M03 | N10-S08 | M03 | Present | NULL | NULL | NULL | NULL | BAD-A |

### 39. `training_results`

| key | session | member | coach | attendance_status | content |
| --- | --- | --- | --- | --- | --- |
| RESULT-SUB04-01 | SUB04-S01 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-02 | SUB04-S02 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-03 | SUB04-S03 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-04 | SUB04-S04 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-05 | SUB04-S05 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-06 | SUB04-S06 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-07 | SUB04-S07 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-08 | SUB04-S08 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-09 | SUB04-S09 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-10 | SUB04-S10 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-11 | SUB04-S11 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB04-12 | SUB04-S12 | M05 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB05-01 | SUB05-S01 | M06 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-SUB05-02 | SUB05-S02 | M06 | TEN-A | Present | DEMO bài tập cá nhân đã thực hiện |
| RESULT-N08-S01-M01 | N08-S01 | M01 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S01-M02 | N08-S01 | M02 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S01-M03 | N08-S01 | M03 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S01-M04 | N08-S01 | M04 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S01-M05 | N08-S01 | M05 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S01-M06 | N08-S01 | M06 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S02-M01 | N08-S02 | M01 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S02-M02 | N08-S02 | M02 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S02-M04 | N08-S02 | M04 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S02-M05 | N08-S02 | M05 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S02-M06 | N08-S02 | M06 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M01 | N08-S03 | M01 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M02 | N08-S03 | M02 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M03 | N08-S03 | M03 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M04 | N08-S03 | M04 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M05 | N08-S03 | M05 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S03-M06 | N08-S03 | M06 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S04-M01 | N08-S04 | M01 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S04-M02 | N08-S04 | M02 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S04-M04 | N08-S04 | M04 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S04-M05 | N08-S04 | M05 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S04-M06 | N08-S04 | M06 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M01 | N08-S05 | M01 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M02 | N08-S05 | M02 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M03 | N08-S05 | M03 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M04 | N08-S05 | M04 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M05 | N08-S05 | M05 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N08-S05-M06 | N08-S05 | M06 | BC-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S01-M01 | N09-S01 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S01-M02 | N09-S01 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S02-M01 | N09-S02 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S02-M02 | N09-S02 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S03-M01 | N09-S03 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S03-M02 | N09-S03 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S04-M01 | N09-S04 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S04-M02 | N09-S04 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S05-M01 | N09-S05 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S05-M02 | N09-S05 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S06-M01 | N09-S06 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S06-M02 | N09-S06 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S07-M01 | N09-S07 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S07-M02 | N09-S07 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S08-M01 | N09-S08 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S08-M02 | N09-S08 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S09-M01 | N09-S09 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S09-M02 | N09-S09 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S10-M01 | N09-S10 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S10-M02 | N09-S10 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S11-M01 | N09-S11 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S11-M02 | N09-S11 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S12-M01 | N09-S12 | M01 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N09-S12-M02 | N09-S12 | M02 | TT-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S01-M01 | N10-S01 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S01-M02 | N10-S01 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S01-M03 | N10-S01 | M03 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S02-M01 | N10-S02 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S02-M02 | N10-S02 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S03-M01 | N10-S03 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S03-M02 | N10-S03 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S03-M03 | N10-S03 | M03 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S04-M01 | N10-S04 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S04-M02 | N10-S04 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S05-M01 | N10-S05 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S05-M02 | N10-S05 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S05-M03 | N10-S05 | M03 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S06-M01 | N10-S06 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S06-M02 | N10-S06 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S06-M03 | N10-S06 | M03 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S07-M01 | N10-S07 | M01 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S07-M02 | N10-S07 | M02 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |
| RESULT-N10-S07-M03 | N10-S07 | M03 | BAD-A | Present | DEMO thực hiện bài tập và được nhận xét tại buổi học |

### 40. `sessions`

Mốc fixture 2026-11-21 00:00 VN. Đổi ngày phải tái sinh toàn bộ bundle và kiểm resource/Member conflict/cửa thời gian; không tự sửa Clock Render hoặc TTL. N10 có13record/12hợp lệ; makeup giữ buổi gốc.

| key | sports_class | date | start | end | coach | room | member | status | finalized | review_pending | center_invalidated | makeup_for | subscription | agreement_confirmed_at | created_by |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| N01-S01 | N01 | 2026-11-30 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S02 | N01 | 2026-12-02 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S03 | N01 | 2026-12-04 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S04 | N01 | 2026-12-07 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S05 | N01 | 2026-12-09 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S06 | N01 | 2026-12-11 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S07 | N01 | 2026-12-14 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S08 | N01 | 2026-12-16 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S09 | N01 | 2026-12-18 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S10 | N01 | 2026-12-21 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S11 | N01 | 2026-12-23 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N01-S12 | N01 | 2026-12-25 | 09:30 | 11:00 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S01 | N02 | 2026-11-23 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S02 | N02 | 2026-11-25 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S03 | N02 | 2026-11-27 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S04 | N02 | 2026-11-30 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S05 | N02 | 2026-12-02 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S06 | N02 | 2026-12-04 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S07 | N02 | 2026-12-07 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S08 | N02 | 2026-12-09 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S09 | N02 | 2026-12-11 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S10 | N02 | 2026-12-14 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S11 | N02 | 2026-12-16 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N02-S12 | N02 | 2026-12-18 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S01 | N03 | 2026-11-25 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S02 | N03 | 2026-11-26 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S03 | N03 | 2026-11-28 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S04 | N03 | 2026-12-02 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S05 | N03 | 2026-12-03 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S06 | N03 | 2026-12-05 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S07 | N03 | 2026-12-09 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S08 | N03 | 2026-12-10 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S09 | N03 | 2026-12-12 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S10 | N03 | 2026-12-16 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S11 | N03 | 2026-12-17 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N03-S12 | N03 | 2026-12-19 | 06:00 | 07:30 | SWIM-A | C01-SWIM-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S01 | N04 | 2026-11-30 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S02 | N04 | 2026-12-02 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S03 | N04 | 2026-12-04 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S04 | N04 | 2026-12-07 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S05 | N04 | 2026-12-09 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S06 | N04 | 2026-12-11 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S07 | N04 | 2026-12-14 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S08 | N04 | 2026-12-16 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S09 | N04 | 2026-12-18 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S10 | N04 | 2026-12-21 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S11 | N04 | 2026-12-23 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N04-S12 | N04 | 2026-12-25 | 07:45 | 09:15 | ARC-A | C01-ARC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S01 | N05 | 2026-11-28 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S02 | N05 | 2026-12-01 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S03 | N05 | 2026-12-03 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S04 | N05 | 2026-12-05 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S05 | N05 | 2026-12-08 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S06 | N05 | 2026-12-10 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S07 | N05 | 2026-12-12 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S08 | N05 | 2026-12-15 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S09 | N05 | 2026-12-17 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S10 | N05 | 2026-12-19 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S11 | N05 | 2026-12-22 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N05-S12 | N05 | 2026-12-24 | 09:30 | 11:00 | PICK-A | C01-PICK-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S01 | N06 | 2026-11-30 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S02 | N06 | 2026-12-02 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S03 | N06 | 2026-12-04 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S04 | N06 | 2026-12-07 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S05 | N06 | 2026-12-09 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S06 | N06 | 2026-12-11 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S07 | N06 | 2026-12-14 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S08 | N06 | 2026-12-16 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S09 | N06 | 2026-12-18 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S10 | N06 | 2026-12-21 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S11 | N06 | 2026-12-23 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N06-S12 | N06 | 2026-12-25 | 13:00 | 14:30 | BR-A | C01-BR-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S01 | N07 | 2026-12-01 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S02 | N07 | 2026-12-03 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S03 | N07 | 2026-12-05 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S04 | N07 | 2026-12-08 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S05 | N07 | 2026-12-10 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S06 | N07 | 2026-12-12 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S07 | N07 | 2026-12-15 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S08 | N07 | 2026-12-17 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S09 | N07 | 2026-12-19 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S10 | N07 | 2026-12-22 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S11 | N07 | 2026-12-24 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N07-S12 | N07 | 2026-12-26 | 07:45 | 09:15 | FOOT-A | C01-FOOT-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S01 | N08 | 2026-11-10 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N08-S02 | N08 | 2026-11-12 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N08-S03 | N08 | 2026-11-14 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N08-S04 | N08 | 2026-11-17 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N08-S05 | N08 | 2026-11-19 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N08-S06 | N08 | 2026-11-21 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S07 | N08 | 2026-11-24 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S08 | N08 | 2026-11-26 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S09 | N08 | 2026-11-28 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S10 | N08 | 2026-12-01 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S11 | N08 | 2026-12-03 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N08-S12 | N08 | 2026-12-05 | 13:00 | 14:30 | BC-A | C01-BC-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N09-S01 | N09 | 2026-10-02 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S02 | N09 | 2026-10-05 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S03 | N09 | 2026-10-07 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S04 | N09 | 2026-10-09 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S05 | N09 | 2026-10-12 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S06 | N09 | 2026-10-14 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S07 | N09 | 2026-10-16 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S08 | N09 | 2026-10-19 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S09 | N09 | 2026-10-21 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S10 | N09 | 2026-10-23 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S11 | N09 | 2026-10-26 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N09-S12 | N09 | 2026-10-28 | 14:45 | 16:15 | TT-A | C01-TT-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S01 | N10 | 2026-11-03 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S02 | N10 | 2026-11-05 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S03 | N10 | 2026-11-07 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S04 | N10 | 2026-11-10 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S05 | N10 | 2026-11-12 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S06 | N10 | 2026-11-14 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S07 | N10 | 2026-11-17 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | false | NULL | NULL | NULL | NULL |
| N10-S08 | N10 | 2026-11-19 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Completed | true | false | true | NULL | NULL | NULL | NULL |
| N10-S09 | N10 | 2026-11-21 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N10-S10 | N10 | 2026-11-24 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N10-S11 | N10 | 2026-11-26 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N10-S12 | N10 | 2026-11-28 | 14:45 | 16:15 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N10-MU01 | N10 | 2026-12-02 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | NULL | Scheduled | false | false | false | N10-S08 | NULL | NULL | NULL |
| N11-S01 | N11 | 2026-11-30 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S02 | N11 | 2026-12-02 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S03 | N11 | 2026-12-04 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S04 | N11 | 2026-12-07 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S05 | N11 | 2026-12-09 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S06 | N11 | 2026-12-11 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S07 | N11 | 2026-12-14 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S08 | N11 | 2026-12-16 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S09 | N11 | 2026-12-18 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S10 | N11 | 2026-12-21 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S11 | N11 | 2026-12-23 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N11-S12 | N11 | 2026-12-25 | 16:30 | 18:00 | PICK-B | C01-PICK-02 | NULL | Scheduled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S01 | N12 | 2026-11-03 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S02 | N12 | 2026-11-05 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S03 | N12 | 2026-11-07 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S04 | N12 | 2026-11-10 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S05 | N12 | 2026-11-12 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S06 | N12 | 2026-11-14 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S07 | N12 | 2026-11-17 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S08 | N12 | 2026-11-19 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S09 | N12 | 2026-11-21 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S10 | N12 | 2026-11-24 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S11 | N12 | 2026-11-26 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| N12-S12 | N12 | 2026-11-28 | 16:30 | 18:00 | BAD-A | C01-BAD-01 | NULL | Cancelled | false | false | false | NULL | NULL | NULL | NULL |
| SUB02-S01 | A-AR05 | 2026-11-26 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M03 | Scheduled | false | false | false | NULL | SUB02 | NULL | NULL |
| SUB02-S02 | A-AR05 | 2026-11-28 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M03 | Scheduled | false | false | false | NULL | SUB02 | NULL | NULL |
| SUB03-S01 | A-AR06 | 2026-11-24 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S02 | A-AR06 | 2026-11-26 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S03 | A-AR06 | 2026-11-28 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S04 | A-AR06 | 2026-12-01 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S05 | A-AR06 | 2026-12-03 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S06 | A-AR06 | 2026-12-05 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S07 | A-AR06 | 2026-12-08 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S08 | A-AR06 | 2026-12-10 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S09 | A-AR06 | 2026-12-12 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S10 | A-AR06 | 2026-12-15 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S11 | A-AR06 | 2026-12-17 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB03-S12 | A-AR06 | 2026-12-19 | 18:15 | 19:45 | BAD-A | C01-BAD-01 | M04 | Scheduled | false | false | false | NULL | SUB03 | NULL | NULL |
| SUB04-S01 | A-AR07 | 2026-10-17 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S02 | A-AR07 | 2026-10-20 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S03 | A-AR07 | 2026-10-22 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S04 | A-AR07 | 2026-10-24 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S05 | A-AR07 | 2026-10-27 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S06 | A-AR07 | 2026-10-29 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S07 | A-AR07 | 2026-10-31 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S08 | A-AR07 | 2026-11-03 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S09 | A-AR07 | 2026-11-05 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S10 | A-AR07 | 2026-11-07 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S11 | A-AR07 | 2026-11-10 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB04-S12 | A-AR07 | 2026-11-12 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M05 | Completed | true | false | false | NULL | SUB04 | NULL | NULL |
| SUB05-S01 | A-AR08 | 2026-09-17 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M06 | Completed | true | false | false | NULL | SUB05 | NULL | NULL |
| SUB05-S02 | A-AR08 | 2026-09-19 | 18:15 | 19:45 | TEN-A | C01-TEN-01 | M06 | Completed | true | false | false | NULL | SUB05 | NULL | NULL |
| SUB01-S01 | A-AR04 | 2026-11-26 | 06:00 | 07:30 | TEN-A | C01-TEN-01 | M02 | Scheduled | false | false | false | NULL | SUB01 | 2026-11-21T00:00 | TEN-A |

### 41. `training_plans`

| key | sports_class | member | coach | title | content |
| --- | --- | --- | --- | --- | --- |
| PLAN-SUB02 | NULL | M03 | TEN-A | DEMO giáo án cá nhân Advanced | Thực hành kỹ thuật riêng cho Member M03 |
| PLAN-N08 | N08 | NULL | BC-A | DEMO giáo án lớp N08 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng |
| PLAN-N09 | N09 | NULL | TT-A | DEMO giáo án lớp N09 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng |
| PLAN-N10 | N10 | NULL | BAD-A | DEMO giáo án lớp N10 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng |

### 42. `classes`

Normal có12buổi và quy trình minimum/confirmation/start-close. Advanced chỉ có lớp sau Coach nhận đăng ký, mỗi đăng ký một lớp riêng; lớp không tự cấp quyền học trước Paid và không tự tạo buổi trước Member Accept.

| key | name | subject | type | coach | room | max_capacity | min_capacity | price | status | sessions | first_session | end_date | lifecycle | postponements | member | registration |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| N01 | Cầu lông Normal N01 | BAD | Normal | BAD-A | C01-BAD-01 | 4 | 3 | 900000 | Open | 12 | 2026-11-30T09:30 | 2026-12-25 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N02 | Tennis Normal N02 | TEN | Normal | TEN-A | C01-TEN-01 | 4 | 3 | 2500000 | Open | 12 | 2026-11-23T06:00 | 2026-12-18 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N03 | Bơi lội Normal N03 | SWIM | Normal | SWIM-A | C01-SWIM-01 | 10 | 7 | 1500000 | Open | 12 | 2026-11-25T06:00 | 2026-12-19 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N04 | Bắn cung Normal N04 | ARC | Normal | ARC-A | C01-ARC-01 | 5 | 4 | 2000000 | Open | 12 | 2026-11-30T07:45 | 2026-12-25 | Chưa xác nhận; không tự khai giảng | 1 | NULL | NULL |
| N05 | Pickleball Normal N05 | PICK | Normal | PICK-A | C01-PICK-01 | 4 | 3 | 1800000 | Open | 12 | 2026-11-28T09:30 | 2026-12-24 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N06 | Bóng rổ Normal N06 | BR | Normal | BR-A | C01-BR-01 | 8 | 6 | 1200000 | Cancelled | 12 | 2026-11-30T13:00 | 2026-12-25 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N07 | Bóng đá Normal N07 | FOOT | Normal | FOOT-A | C01-FOOT-01 | 15 | 10 | 1000000 | Open | 12 | 2026-12-01T07:45 | 2026-12-26 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N08 | Bóng chuyền Normal N08 | BC | Normal | BC-A | C01-BC-01 | 8 | 6 | 1000000 | Ongoing | 12 | 2026-11-10T13:00 | 2026-12-05 | Đã xác nhận | 0 | NULL | NULL |
| N09 | Bóng bàn Normal N09 | TT | Normal | TT-A | C01-TT-01 | 2 | 2 | 800000 | Closed | 12 | 2026-10-02T14:45 | 2026-10-28 | Đã xác nhận | 0 | NULL | NULL |
| N10 | Cầu lông Normal N10 | BAD | Normal | BAD-A | C01-BAD-01 | 4 | 3 | 900000 | Ongoing | 13 | 2026-11-03T14:45 | 2026-12-02 | Đã xác nhận | 0 | NULL | NULL |
| N11 | Pickleball Normal N11 | PICK | Normal | PICK-B | C01-PICK-02 | 4 | 3 | 1800000 | Open | 12 | 2026-11-30T16:30 | 2026-12-25 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| N12 | Cầu lông Normal N12 | BAD | Normal | BAD-A | C01-BAD-01 | 4 | 3 | 900000 | Cancelled | 12 | 2026-11-03T16:30 | 2026-11-28 | Chưa xác nhận; không tự khai giảng | 0 | NULL | NULL |
| A-AR02 | Pickleball - Advanced #AR02 | PICK | Advanced | PICK-A | NULL | 1 | NULL | 5000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M15 | AR02 |
| A-AR04 | Tennis - Advanced #AR04 | TEN | Advanced | TEN-A | NULL | 1 | NULL | 8000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M02 | AR04 |
| A-AR05 | Tennis - Advanced #AR05 | TEN | Advanced | TEN-A | NULL | 1 | NULL | 8000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M03 | AR05 |
| A-AR06 | Cầu lông - Advanced #AR06 | BAD | Advanced | BAD-A | NULL | 1 | NULL | 3000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M04 | AR06 |
| A-AR07 | Tennis - Advanced #AR07 | TEN | Advanced | TEN-A | NULL | 1 | NULL | 8000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M05 | AR07 |
| A-AR08 | Tennis - Advanced #AR08 | TEN | Advanced | TEN-A | NULL | 1 | NULL | 8000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M06 | AR08 |
| A-AR09 | Cầu lông - Advanced #AR09 | BAD | Advanced | BAD-A | NULL | 1 | NULL | 3000000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M07 | AR09 |
| A-AR10 | Bơi lội - Advanced #AR10 | SWIM | Advanced | SWIM-A | NULL | 1 | NULL | 4500000 | Closed | 0 | NULL | NULL | Coach đã nhận đăng ký; quyền học theo thanh toán | NULL | M08 | AR10 |

### 43. `rooms`

| key | name | location | facility_type | capacity | center | status | activity_type |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C01-TEN-01 | Sân Tennis 01 | Khu A – Ngoài trời | Outdoor | 4 | C01 | Available | TENNIS |
| C01-TEN-02 | Sân Tennis 02 | Khu A – Ngoài trời | Outdoor | 4 | C01 | Available | TENNIS |
| C01-FOOT-01 | Sân Bóng đá 01 | Khu B – Ngoài trời | Outdoor | 15 | C01 | Available | FOOTBALL |
| C01-FOOT-02 | Sân Bóng đá 02 | Khu B – Ngoài trời | Outdoor | 15 | C01 | Available | FOOTBALL |
| C01-ARC-01 | Khu Bắn cung 01 | Khu C – Ngoài trời | Outdoor | 5 | C01 | Available | ARCHERY |
| C01-ARC-02 | Khu Bắn cung 02 | Khu C – Ngoài trời | Outdoor | 5 | C01 | Available | ARCHERY |
| C01-BR-01 | Sân Bóng rổ 01 | Nhà thi đấu A | Indoor | 8 | C01 | Available | BASKETBALL |
| C01-BR-02 | Sân Bóng rổ 02 | Nhà thi đấu A | Indoor | 8 | C01 | Available | BASKETBALL |
| C01-BC-01 | Sân Bóng chuyền 01 | Nhà thi đấu A | Indoor | 8 | C01 | Available | VOLLEYBALL |
| C01-BC-02 | Sân Bóng chuyền 02 | Nhà thi đấu A | Indoor | 8 | C01 | Available | VOLLEYBALL |
| C01-PICK-01 | Sân Pickleball 01 | Nhà thi đấu B | Indoor | 4 | C01 | Available | PICKLEBALL |
| C01-PICK-02 | Sân Pickleball 02 | Nhà thi đấu B | Indoor | 4 | C01 | Available | PICKLEBALL |
| C01-BAD-01 | Sân Cầu lông 01 | Nhà thi đấu B | Indoor | 4 | C01 | Available | BADMINTON |
| C01-BAD-02 | Sân Cầu lông 02 | Nhà thi đấu B | Indoor | 4 | C01 | Available | BADMINTON |
| C01-TT-01 | Bàn Bóng bàn 01 | Phòng Bóng bàn | Indoor | 2 | C01 | Available | TABLE_TENNIS |
| C01-TT-02 | Bàn Bóng bàn 02 | Phòng Bóng bàn | Indoor | 2 | C01 | Available | TABLE_TENNIS |
| C01-TT-03 | Bàn Bóng bàn 03 | Phòng Bóng bàn | Indoor | 2 | C01 | Available | TABLE_TENNIS |
| C01-TT-04 | Bàn Bóng bàn 04 | Phòng Bóng bàn | Indoor | 2 | C01 | Available | TABLE_TENNIS |
| C01-SWIM-01 | Hồ bơi 01 | Khu hồ bơi | Outdoor | 10 | C01 | Available | SWIMMING |
| C01-SWIM-02 | Hồ bơi 02 | Khu hồ bơi | Outdoor | 10 | C01 | Available | SWIMMING |

### 44. `coach_specializations`

| coach | specialization |
| --- | --- |
| TEN-A | SP-TEN |
| TEN-B | SP-TEN |
| SWIM-A | SP-SWIM |
| SWIM-B | SP-SWIM |
| PICK-A | SP-PICK |
| PICK-B | SP-PICK |
| ARC-A | SP-ARC |
| ARC-B | SP-ARC |
| BR-A | SP-BR |
| BR-B | SP-BR |
| FOOT-A | SP-FOOT |
| FOOT-B | SP-FOOT |
| BC-A | SP-BC |
| BC-B | SP-BC |
| BAD-A | SP-BAD |
| BAD-B | SP-BAD |
| TT-A | SP-TT |
| TT-B | SP-TT |
| CI | SP-BAD |

### 45. `specializations`

| key | subject | name | status |
| --- | --- | --- | --- |
| SP-TEN | TEN | Tennis | Active |
| SP-SWIM | SWIM | Bơi lội | Active |
| SP-PICK | PICK | Pickleball | Active |
| SP-ARC | ARC | Bắn cung | Active |
| SP-BR | BR | Bóng rổ | Active |
| SP-FOOT | FOOT | Bóng đá | Active |
| SP-BC | BC | Bóng chuyền | Active |
| SP-BAD | BAD | Cầu lông | Active |
| SP-TT | TT | Bóng bàn | Active |

### 46. `subjects`

| key | name | slug | normal_price | advanced_price | outdoor | status |
| --- | --- | --- | --- | --- | --- | --- |
| TEN | Tennis | quan-vot-tennis | 2500000 | 8000000 | true | Active |
| SWIM | Bơi lội | boi-loi | 1500000 | 4500000 | true | Active |
| PICK | Pickleball | pickleball | 1800000 | 5000000 | false | Active |
| ARC | Bắn cung | ban-cung | 2000000 | 6000000 | true | Active |
| BR | Bóng rổ | bong-ro | 1200000 | 3500000 | false | Active |
| FOOT | Bóng đá | bong-da | 1000000 | 3500000 | true | Active |
| BC | Bóng chuyền | bong-chuyen | 1000000 | 3500000 | false | Active |
| BAD | Cầu lông | cau-long | 900000 | 3000000 | false | Active |
| TT | Bóng bàn | bong-ban | 800000 | 2500000 | false | Active |

### 47. `members`

| user | join_date | registered_by | goal | health_note |
| --- | --- | --- | --- | --- |
| M01 | 2026-08-23 | NULL | NULL | NULL |
| M02 | 2026-08-23 | NULL | NULL | NULL |
| M03 | 2026-08-23 | NULL | NULL | NULL |
| M04 | 2026-08-23 | NULL | NULL | NULL |
| M05 | 2026-08-23 | NULL | NULL | NULL |
| M06 | 2026-08-23 | NULL | NULL | NULL |
| M07 | 2026-08-23 | NULL | NULL | NULL |
| M08 | 2026-08-23 | NULL | NULL | NULL |
| M09 | 2026-08-23 | NULL | NULL | NULL |
| M10 | 2026-08-23 | NULL | NULL | NULL |
| M11 | 2026-08-23 | NULL | NULL | NULL |
| M12 | 2026-08-23 | NULL | NULL | NULL |
| M13 | 2026-08-23 | NULL | NULL | NULL |
| M14 | 2026-08-23 | NULL | NULL | NULL |
| M15 | 2026-08-23 | NULL | NULL | NULL |
| M16 | 2026-08-23 | NULL | NULL | NULL |
| M17 | 2026-08-23 | NULL | NULL | NULL |
| M18 | 2026-08-23 | NULL | NULL | NULL |
| M19 | 2026-08-23 | NULL | NULL | NULL |
| M20 | 2026-08-23 | NULL | NULL | NULL |
| MI | 2026-08-23 | NULL | NULL | NULL |
| ML | 2026-08-23 | NULL | NULL | NULL |
| MP | 2026-08-23 | NULL | NULL | NULL |

### 48. `coaches`

| user | specialization | bio |
| --- | --- | --- |
| TEN-A | Tennis | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| TEN-B | Tennis | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| SWIM-A | Bơi lội | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| SWIM-B | Bơi lội | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| PICK-A | Pickleball | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| PICK-B | Pickleball | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| ARC-A | Bắn cung | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| ARC-B | Bắn cung | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BR-A | Bóng rổ | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BR-B | Bóng rổ | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| FOOT-A | Bóng đá | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| FOOT-B | Bóng đá | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BC-A | Bóng chuyền | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BC-B | Bóng chuyền | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BAD-A | Cầu lông | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| BAD-B | Cầu lông | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| TT-A | Bóng bàn | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| TT-B | Bóng bàn | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |
| CI | Cầu lông | Hồ sơ Coach demo; không cấp chuyên môn ngoài môn đã duyệt |

### 49. `receptionists`

| user |
| --- |
| R1 |
| R2 |

### 50. `center_managers`

| user |
| --- |
| CM1 |
| CM2 |

### 51. `email_otps`

Rỗng ban đầu. Demo đăng ký/reset password tạo OTP tại thời điểm chạy; không seed/export OTP hoặc secret.

Chưa có dòng tĩnh trong bản thiết kế này.


### 52. `users`

Giữ nguyên33account PDF. Mã tham chiếu không phải user_id API. Không xuất password/hash/QR secret. Account thêm dùng miền .invalid, không gửi email. Các cột runtime khác giữ DEFAULT/NULL phù hợp; role/center không từ client.

| key | pdf_ordinal | role | name | email | phone | address | gender | date_of_birth | status | center | first_login |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ADM | 1 | Admin | Admin Account | admin@gmail.com | 0905000101 | Văn phòng Trung tâm, Q1, TP.HCM | Male | 1985-08-12 | Active | NULL | false |
| M01 | 2 | Member | Nguyễn Minh An | an@gmail.com | 0901000101 | Quận 1, TP.HCM | Male | 2003-05-12 | Active | C01 | false |
| M02 | 3 | Member | Trần Bảo Linh | linh@gmail.com | 0901000102 | Quận 3, TP.HCM | Female | 2002-08-21 | Active | C01 | false |
| M03 | 4 | Member | Lê Gia Huy | huy@gmail.com | 0901000103 | Quận 7, TP.HCM | Male | 2004-02-15 | Active | C01 | false |
| M04 | 5 | Member | Phạm Ngọc Mai | mai@gmail.com | 0901000104 | Bình Thạnh, TP.HCM | Female | 2001-11-09 | Active | C01 | false |
| M05 | 6 | Member | Võ Đức Khang | khang@gmail.com | 0901000105 | TP. Thủ Đức, TP.HCM | Male | 2003-07-24 | Active | C01 | false |
| M06 | 7 | Member | Đặng Thảo Vy | vy@gmail.com | 0901000106 | Tân Bình, TP.HCM | Female | 2004-04-18 | Active | C01 | false |
| M07 | 8 | Member | Bùi Quốc Bảo | bao@gmail.com | 0901000107 | Quận 10, TP.HCM | Male | 2002-12-03 | Active | C01 | false |
| M08 | 9 | Member | Hồ Khánh Nhi | nhi@gmail.com | 0901000108 | Phú Nhuận, TP.HCM | Female | 2005-06-27 | Active | C01 | false |
| M09 | 10 | Member | Đỗ Tuấn Kiệt | kiet@gmail.com | 0901000109 | Quận 11, TP.HCM | Male | 2001-09-14 | Active | C01 | false |
| M10 | 11 | Member | Phan Thu Hà | ha@gmail.com | 0901000110 | Gò Vấp, TP.HCM | Female | 2003-03-30 | Active | C01 | false |
| TEN-A | 12 | Coach | Nguyễn Hoàng Quân | coach.tennis1@gmail.com | 0902000101 | Quận 1, TP.HCM | Male | 1990-03-15 | Active | C01 | false |
| TEN-B | 13 | Coach | Huỳnh Diễm My | coach.tennis2@gmail.com | 0902000102 | Quận 3, TP.HCM | Female | 1992-07-22 | Active | C01 | false |
| SWIM-A | 14 | Coach | Lê Minh Duy | coach.swimming1@gmail.com | 0902000103 | Quận 7, TP.HCM | Male | 1989-11-08 | Active | C01 | false |
| SWIM-B | 15 | Coach | Lê Phương Anh | coach.swimming2@gmail.com | 0902000104 | Bình Thạnh, TP.HCM | Female | 1993-05-17 | Active | C01 | false |
| PICK-A | 16 | Coach | Võ Thanh Long | coach.pickleball1@gmail.com | 0902000105 | TP. Thủ Đức, TP.HCM | Male | 1994-09-26 | Active | C01 | false |
| PICK-B | 17 | Coach | Phạm Thị Lan | coach.pickleball2@gmail.com | 0902000106 | Tân Bình, TP.HCM | Female | 1995-02-12 | Active | C01 | false |
| ARC-A | 18 | Coach | Lê Nguyễn Thiên Phúc | coach.archery1@gmail.com | 0902000107 | Quận 10, TP.HCM | Male | 1991-12-04 | Active | C01 | false |
| ARC-B | 19 | Coach | Hồ Thảo My | coach.archery2@gmail.com | 0902000108 | Phú Nhuận, TP.HCM | Female | 1996-06-19 | Active | C01 | false |
| BR-A | 20 | Coach | Trần Anh Dũng | coach.basketball1@gmail.com | 0902000109 | Quận 11, TP.HCM | Male | 1988-01-30 | Active | C01 | false |
| BR-B | 21 | Coach | Phan Kim Ngân | coach.basketball2@gmail.com | 0902000110 | Gò Vấp, TP.HCM | Female | 1992-08-14 | Active | C01 | false |
| FOOT-A | 22 | Coach | Lê Trần Phi | coach.football1@gmail.com | 0902000111 | Quận 12, TP.HCM | Male | 1987-04-25 | Active | C01 | false |
| FOOT-B | 23 | Coach | Trần Ngọc Yến | coach.football2@gmail.com | 0902000112 | Quận 6, TP.HCM | Female | 1993-10-09 | Active | C01 | false |
| BC-A | 24 | Coach | Nguyễn Văn Hoàng | coach.volleyball1@gmail.com | 0902000113 | Quận 5, TP.HCM | Male | 1989-02-21 | Active | C01 | false |
| BC-B | 25 | Coach | Phạm Thanh Thư | coach.volleyball2@gmail.com | 0902000114 | Quận 8, TP.HCM | Female | 1995-07-07 | Active | C01 | false |
| BAD-A | 26 | Coach | Võ Minh Đức | coach.badminton1@gmail.com | 0902000115 | Quận 4, TP.HCM | Male | 1990-09-18 | Active | C01 | false |
| BAD-B | 27 | Coach | Trần Thị Hương | coach.badminton2@gmail.com | 0902000116 | Tân Phú, TP.HCM | Female | 1996-01-11 | Active | C01 | false |
| TT-A | 28 | Coach | Lê Minh Khoa | coach.tabletennis1@gmail.com | 0902000117 | Bình Tân, TP.HCM | Male | 1988-05-23 | Active | C01 | false |
| TT-B | 29 | Coach | Hồ Bích Oanh | coach.tabletennis2@gmail.com | 0902000118 | Quận 3, TP.HCM | Female | 1994-12-16 | Active | C01 | false |
| R1 | 30 | Receptionist | Nguyễn Thị Ngọc Anh | receptionist1@gmail.com | 0903000101 | Bình Thạnh, TP.HCM | Female | 1998-06-14 | Active | C01 | false |
| R2 | 31 | Receptionist | Trần Minh Khôi | receptionist2@gmail.com | 0903000102 | TP. Thủ Đức, TP.HCM | Male | 1997-09-22 | Active | C01 | false |
| CM1 | 32 | CenterManager | Nguyễn Thành Đạt | manager1@gmail.com | 0904000101 | Quận 7, TP.HCM | Male | 1988-04-18 | Active | C01 | false |
| CM2 | 33 | CenterManager | Trần Ngọc Diệp | manager2@gmail.com | 0904000102 | Quận 3, TP.HCM | Female | 1990-09-25 | Active | C01 | false |
| M11 | NULL | Member | Member Demo11 | member11@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M12 | NULL | Member | Member Demo12 | member12@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M13 | NULL | Member | Member Demo13 | member13@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M14 | NULL | Member | Member Demo14 | member14@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M15 | NULL | Member | Member Demo15 | member15@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M16 | NULL | Member | Member Demo16 | member16@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M17 | NULL | Member | Member Demo17 | member17@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M18 | NULL | Member | Member Demo18 | member18@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M19 | NULL | Member | Member Demo19 | member19@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| M20 | NULL | Member | Member Demo20 | member20@scms.demo.invalid | NULL | NULL | NULL | NULL | Active | C01 | false |
| MI | NULL | Member | Demo Inactive Member | member.inactive@scms.demo.invalid | NULL | NULL | NULL | NULL | Inactive | C01 | false |
| ML | NULL | Member | Demo Locked Member | member.locked@scms.demo.invalid | NULL | NULL | NULL | NULL | Locked | C01 | false |
| MP | NULL | Member | Demo Pending Member | member.pending@scms.demo.invalid | NULL | NULL | NULL | NULL | Pending | C01 | false |
| CI | NULL | Coach | Demo Inactive Coach | coach.inactive@scms.demo.invalid | NULL | NULL | NULL | NULL | Inactive | C01 | false |

### 53. `centers`

| key | name | weekly_closure |
| --- | --- | --- |
| C01 | Sports Center 1 | Không mặc định nghỉ T7/CN |

### 54. `role_permissions`

Baseline hiện hành; permission legacy có thể tồn tại nhưng không làm API legacy hoạt động trở lại. Không mở thêm Admin tài chính hoặc quyền Coach ngoài assignment.

| role | permission |
| --- | --- |
| CenterManager | MANAGE_USERS |
| CenterManager | MANAGE_CLASSES |
| CenterManager | MANAGE_PACKAGES |
| CenterManager | VIEW_REPORTS |
| CenterManager | PROCESS_PAYMENT |
| CenterManager | HANDLE_SUPPORT |
| CenterManager | VIEW_AUDIT_LOG |
| CenterManager | MANAGE_INVOICES |
| CenterManager | HANDLE_CHAT |
| Coach | MANAGE_TRAINING_PLAN |
| Coach | RECORD_RESULT |
| Receptionist | RECORD_RESULT |
| Receptionist | PROCESS_PAYMENT |
| Receptionist | HANDLE_SUPPORT |
| Receptionist | REGISTER_MEMBER |
| Receptionist | MANAGE_SUBSCRIPTIONS |
| Receptionist | MANAGE_INVOICES |
| Receptionist | HANDLE_CHAT |
| Member | CHAT_AS_MEMBER |
| Admin | MANAGE_USERS |
| Admin | VIEW_REPORTS |
| Admin | VIEW_AUDIT_LOG |
| Admin | MANAGE_RBAC |
| Admin | MANAGE_PERMISSIONS |
| Admin | LOCK_SUBJECTS |
| Admin | MANAGE_SPECIALIZATIONS |

### 55. `permissions`

| key | description |
| --- | --- |
| MANAGE_USERS | Quan ly nguoi dung |
| MANAGE_CLASSES | Quan ly lop hoc |
| MANAGE_PACKAGES | Quan ly goi thanh vien |
| VIEW_REPORTS | Xem bao cao |
| MANAGE_TRAINING_PLAN | Tao/sua ke hoach tap luyen |
| RECORD_RESULT | Ghi nhan ket qua buoi tap |
| PROCESS_PAYMENT | Ghi nhan thanh toan, xuat hoa don |
| HANDLE_SUPPORT | Xu ly yeu cau ho tro |
| VIEW_AUDIT_LOG | Xem lich su thao tac he thong |
| MANAGE_RBAC | Phan quyen va quan tri vai tro he thong |
| REGISTER_MEMBER | Dang ky thanh vien moi tai quay |
| MANAGE_SUBSCRIPTIONS | Dang ky va gia han goi tap |
| MANAGE_PERMISSIONS | Chi quan tri vien duoc cau hinh ma tran phan quyen |
| MANAGE_INVOICES | Quan ly va xem hoa don cua cac thanh vien |
| CHAT_AS_MEMBER | Member may access their own support chat |
| HANDLE_CHAT | Receptionist and CenterManager may handle support chats |
| LOCK_SUBJECTS | Khoa hoac mo mon hoc |
| MANAGE_SPECIALIZATIONS | Quan ly chuyen mon huan luyen vien |

### 56. `roles`

GET roles/permissions/role detail chỉ Admin Active; quyền ghi không tự suy từ quyền đọc.

| key | description |
| --- | --- |
| CenterManager | Quan ly trung tam - van hanh lop hoc, goi tap, bao cao |
| Coach | Huan luyen vien |
| Member | Thanh vien / hoc vien |
| Receptionist | Nhan vien le tan |
| Admin | Quan tri vien he thong - quan tri nguoi dung va phan quyen RBAC |

## Kiểm trước khi dùng bộ theo ngày

Đây là các field thiết kế của bảng thực thi, không là dump hoặc ID DB đã kiểm. SQL execution NOT_RUN đến đúng anchor; kiểm bản SQL đã sinh trên PostgreSQL local mới, rồi xuất SYSTEM-DATA bằng --from-local-pg trước khi thử HTTP. Đối chiếu 12 buổi Normal, chủ đăng ký/lớp/quyền, ca hủy SUB01-S01 có proposal Accepted và11credit, bảy ngày làm việc/ca nghỉ, Holiday/Outdoor, trùng tài nguyên, chứng từ và reset guard.

Invoice mới tối đa30phút và QR300giây được tạo lúc thao tác; không seed mã, nonce hoặc hold hết hạn. Không thay lịch sử để làm ca xanh. Kịch bản tương ứng nằm trong demo-plan.json cùng thư mục và SCRIPT; trạng thái chuẩn ở docs00/QA. Local BE/bộ19 đã đạt không chứng nhận SQL/bộ21, Supabase, FE hoặc provider thật.
