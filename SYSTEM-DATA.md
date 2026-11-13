# SYSTEM DATA — Bộ demo SCMS đã duyệt

**DATA DESIGN — xuất từ fixture PostgreSQL local nguyên trạng; không là dump Supabase hoặc nghiệm thu cloud.**
**Mục đích của SYSTEM-DATA:** giúp BE/FE tra cứu tài khoản, lớp, lịch và hồ sơ mẫu, nối các mã trong SCRIPT với ID và quan hệ dữ liệu cụ thể. Dùng file này để chuẩn bị đầu vào và đối chiếu kết quả demo; trình tự thao tác nằm ở [SCMS-SCRIPT-DEMO-BE](SCMS-SCRIPT-DEMO-BE.md), còn lệnh dựng lại bundle ở docs02.

Đọc nhanh: [N01–N12](#normal-class-index) → [tài khoản demo](#demo-actor-index) → [proposal/Refund](#demo-financial-index) → [các bảng chi tiết](#table-data). Các gợi ý chuẩn bị/giao diện trong SCRIPT để FE duyệt, quyết định cuối thuộc FE.

Đây là dữ liệu demo tổng hợp có nhãn DEMO, không phải dữ liệu/tiền/chứng từ ngân hàng thật hoặc bằng chứng nghiệp vụ cloud. Bảng bên dưới được xuất từ đúng fixture SQL đã chạy local, gồm các cột thực tế; không còn sáu tên bảng dự kiến đứng song song với schema.

Nguồn: [Normal Lifecycle](SCMS_BE_Normal_Lifecycle_DA_CHOT.md), [Normal Refund](SCMS_BE_Refund_Normal_DA_CHOT.md), [schema/generator](database/README.md), [kết quả và giới hạn](docs/00-MASTER-CHECKLIST.md#active).
Giữ 33 account PDF và thêm 14 account, 9 môn, 20 sân, 9 template Advanced và 12 lớp Normal. N05/N11 được nhận diện theo tên lớp; FE lấy ID thật từ API. Invoice lớp mua/amount giữ nguyên khi chuyển, entitlement_class_id chỉ lớp nhận quyền hiện tại.

Mốc ngày VN của fixture: 2026-11-12. Khi chuẩn bị chạy ở ngày khác, tái sinh cả fixture/schema và kiểm lại lịch trước khi dùng; không đổi Clock Render hoặc kéo dài TTL. Hạn hoàn là hết ngày TT mở thứ bảy tiếp theo; tính cả T7/CN trừ Holiday. Contact phone chỉ cho tài chính đúng quyền, FE quyết định hiển thị.

Mọi lịch sử học/thu/hoàn ở đây là ví dụ tổng hợp được duyệt, không khẳng định đã thực hiện ngoài SCMS. Hai PDF nằm trong normal_refund_documents.content riêng tư, có chữ DEMO; bảng chỉ xuất kích thước/hash. Không xuất password/hash, OTP, QR secret, cookie, token hoặc nội dung binary.


<a id="normal-class-index"></a>
## Tra cứu N01–N12 — lớp và mục đích demo

Nxx là mã lớp Normal trong fixture, không phải classId hoặc thứ tự thao tác. Bảng này được sinh từ chính các dòng local bên dưới; đây là trạng thái **nguyên trạng trước demo**, không phải DB cloud sau smoke. Paid Registered/min/max nghĩa là số người Paid còn quyền / minimum / sức chứa. Các giới hạn này chưa chứng nhận lớp đủ mọi điều kiện khai giảng.

Nguồn: [classes](#table-classes), [enrollments](#table-class_enrollments), [invoices](#table-invoices). Case thao tác dùng mã DEMO-xx trong [SCRIPT](SCMS-SCRIPT-DEMO-BE.md#normal-scenario-map); Dxx vẫn là mã quyết định trong docs04.

| Mã | Tên lớp | classId | State | Paid Registered/min/max | Mục đích/case |
|---|---|---:|---|---|---|
| N01 | Cầu lông Normal N01 | 10 | Open | 1/3/4 | [Thu Cash rồi xác nhận đủ sĩ số](SCMS-SCRIPT-DEMO-BE.md#demo-02) |
| N02 | Tennis Normal N02 | 11 | Open | 3/3/4 | [Cutoff đăng ký và xác nhận trễ](SCMS-SCRIPT-DEMO-BE.md#demo-04) |
| N03 | Bơi lội Normal N03 | 12 | Open | 6/7/10 | [Chặn xác nhận khi thiếu sĩ số](SCMS-SCRIPT-DEMO-BE.md#demo-06) |
| N04 | Bắn cung Normal N04 | 13 | Open | 3/4/5 | [Đã dời một lần; chặn dời lần hai](SCMS-SCRIPT-DEMO-BE.md#demo-04-07) |
| N05 | Pickleball Normal N05 | 14 | Open | 2/3/4 | [Lớp nguồn đang đề nghị chuyển](SCMS-SCRIPT-DEMO-BE.md#demo-08) |
| N06 | Bóng rổ Normal N06 | 15 | Cancelled | 0/6/8 | [TT đã hủy; nghĩa vụ hoàn RF02–RF04](SCMS-SCRIPT-DEMO-BE.md#demo-09) |
| N07 | Bóng đá Normal N07 | 16 | Open | 14/10/15 | [Gần đầy; chuẩn bị chỗ cuối/waitlist, ngoài luồng chính](SCMS-SCRIPT-DEMO-BE.md#normal-scenario-map) |
| N08 | Bóng chuyền Normal N08 | 17 | Ongoing | 6/6/8 | [Ongoing; chặn hủy theo luồng trước khai giảng](SCMS-SCRIPT-DEMO-BE.md#demo-07) |
| N09 | Bóng bàn Normal N09 | 18 | Closed | 0/2/2 | [Closed; xem lịch sử/kết quả, không thay kiểm job đóng lớp](SCMS-SCRIPT-DEMO-BE.md#demo-10) |
| N10 | Cầu lông Normal N10 | 19 | Ongoing | 3/3/4 | [Sự cố TT/buổi bù; xem nguồn và lịch sử](SCMS-SCRIPT-DEMO-BE.md#demo-10) |
| N11 | Pickleball Normal N11 | 20 | Open | 3/3/4 | [Lớp đích N05, ban đầu còn một chỗ](SCMS-SCRIPT-DEMO-BE.md#demo-08) |
| N12 | Cầu lông Normal N12 | 21 | Cancelled | 0/3/4 | [TT đã hủy; xem các state hoàn RF05–RF07](SCMS-SCRIPT-DEMO-BE.md#demo-01) |

### Lịch và tài nguyên đối chiếu — toàn bộ giờ VN

Coach lớp lấy từ classes; Coach/sân buổi đầu lấy từ sessions, không suy rằng mọi buổi đều cùng tài nguyên. Lịch của lớp Cancelled vẫn được giữ để đối chiếu. Xem chi tiết [sessions](#table-sessions), [users](#table-users), [rooms](#table-rooms).

| Mã | Buổi đầu / sessionId | Cutoff đăng ký48h | Từ mốc cần reason24h | Coach lớp | Coach buổi đầu | Sân buổi đầu |
|---|---|---|---|---|---|---|
| N01 | 2026-11-20 09:30 / 1 | 2026-11-18 09:30 | 2026-11-19 09:30 | Võ Minh Đức (userId=26) | Võ Minh Đức (userId=26) | C01-BAD-01 (roomId=3) |
| N02 | 2026-11-13 06:00 / 13 | 2026-11-11 06:00 | 2026-11-12 06:00 | Nguyễn Hoàng Quân (userId=12) | Nguyễn Hoàng Quân (userId=12) | C01-TEN-01 (roomId=15) |
| N03 | 2026-11-14 06:00 / 25 | 2026-11-12 06:00 | 2026-11-13 06:00 | Lê Minh Duy (userId=14) | Lê Minh Duy (userId=14) | C01-SWIM-01 (roomId=13) |
| N04 | 2026-11-20 07:45 / 37 | 2026-11-18 07:45 | 2026-11-19 07:45 | Lê Nguyễn Thiên Phúc (userId=18) | Lê Nguyễn Thiên Phúc (userId=18) | C01-ARC-01 (roomId=1) |
| N05 | 2026-11-19 09:30 / 49 | 2026-11-17 09:30 | 2026-11-18 09:30 | Võ Thanh Long (userId=16) | Võ Thanh Long (userId=16) | C01-PICK-01 (roomId=11) |
| N06 | 2026-11-20 13:00 / 61 | 2026-11-18 13:00 | 2026-11-19 13:00 | Trần Anh Dũng (userId=20) | Trần Anh Dũng (userId=20) | C01-BR-01 (roomId=7) |
| N07 | 2026-11-21 07:45 / 73 | 2026-11-19 07:45 | 2026-11-20 07:45 | Lê Trần Phi (userId=22) | Lê Trần Phi (userId=22) | C01-FOOT-01 (roomId=9) |
| N08 | 2026-11-03 13:00 / 85 | 2026-11-01 13:00 | 2026-11-02 13:00 | Nguyễn Văn Hoàng (userId=24) | Nguyễn Văn Hoàng (userId=24) | C01-BC-01 (roomId=5) |
| N09 | 2026-09-23 14:45 / 97 | 2026-09-21 14:45 | 2026-09-22 14:45 | Lê Minh Khoa (userId=28) | Lê Minh Khoa (userId=28) | C01-TT-01 (roomId=17) |
| N10 | 2026-10-24 14:45 / 109 | 2026-10-22 14:45 | 2026-10-23 14:45 | Võ Minh Đức (userId=26) | Võ Minh Đức (userId=26) | C01-BAD-01 (roomId=3) |
| N11 | 2026-11-20 16:30 / 122 | 2026-11-18 16:30 | 2026-11-19 16:30 | Phạm Thị Lan (userId=17) | Phạm Thị Lan (userId=17) | C01-PICK-02 (roomId=12) |
| N12 | 2026-10-24 16:30 / 134 | 2026-10-22 16:30 | 2026-10-23 16:30 | Võ Minh Đức (userId=26) | Võ Minh Đức (userId=26) | C01-BAD-01 (roomId=3) |

<a id="demo-actor-index"></a>
## Tài khoản dùng trong SCRIPT

Mxx/CM1/R1/ADM là mã đối chiếu fixture, không phải userId hoặc chuỗi gửi thay ID trong body. Email là định danh đăng nhập demo; mật khẩu truyền riêng. R1 ở bảng này là Receptionist, khác mã hạng mục R1/R2 của P2.2. Xem toàn bộ [users](#table-users) và [roles](#table-roles).

| Mã | Họ tên | userId | Role | Email đăng nhập demo |
|---|---|---:|---|---|
| ADM | Admin Account | 1 | Admin | admin@gmail.com |
| M01 | Nguyễn Minh An | 2 | Member | an@gmail.com |
| M02 | Trần Bảo Linh | 3 | Member | linh@gmail.com |
| M03 | Lê Gia Huy | 4 | Member | huy@gmail.com |
| M04 | Phạm Ngọc Mai | 5 | Member | mai@gmail.com |
| PICK-A | Võ Thanh Long | 16 | Coach | coach.pickleball1@gmail.com |
| PICK-B | Phạm Thị Lan | 17 | Coach | coach.pickleball2@gmail.com |
| BAD-A | Võ Minh Đức | 26 | Coach | coach.badminton1@gmail.com |
| R1 | Nguyễn Thị Ngọc Anh | 30 | Receptionist | receptionist1@gmail.com |
| CM1 | Nguyễn Thành Đạt | 32 | CenterManager | manager1@gmail.com |
| M14 | Member Demo14 | 37 | Member | member14@scms.demo.invalid |
| M15 | Member Demo15 | 38 | Member | member15@scms.demo.invalid |

<a id="demo-financial-index"></a>
## Proposal và Refund đối chiếu

EV/RF là mã fixture, không phải ID API. EV-N06/EV-N12 mô tả sự kiện TT hủy trong thiết kế; không suy ra có normal_proposals tương ứng. Chỉ các proposal thực có bên dưới được ánh xạ. Dùng GET hiện hành để lấy version/fingerprint/ID khi thao tác. Nguồn: [normal_proposals](#table-normal_proposals), [normal_refunds](#table-normal_refunds), [payment_resolutions](#table-payment_resolutions), [invoices](#table-invoices).

| Mã sự kiện | proposalId | Lớp nguồn → đích | Kind | State |
|---|---:|---|---|---|
| EV-N05 | 1 | N05 → N11 | Transfer | Pending |
| EV-N04 | 2 | N04 → không có lớp đích | Postpone | Applied |

| Mã hoàn | refundId | Invoice nguồn | Member / memberId | Lớp nguồn | State | Amount VND |
|---|---:|---:|---|---|---|---:|
| RF01 | 2 | 15 | M02 / 3 | N05 | AwaitingReceiver | 1.800.000 |
| RF02 | 3 | 17 | M04 / 5 | N06 | AwaitingReceiver | 1.200.000 |
| RF03 | 4 | 18 | M05 / 6 | N06 | AwaitingApproval | 1.200.000 |
| RF04 | 5 | 19 | M06 / 7 | N06 | Approved | 1.200.000 |
| RF05 | 6 | 48 | M07 / 8 | N12 | AwaitingVerification | 900.000 |
| RF06 | 7 | 49 | M08 / 9 | N12 | Completed | 900.000 |
| RF07 | 8 | 50 | M09 / 10 | N12 | NeedsReview | 900.000 |

Bảng tra cứu không chứa accountNumber hoặc proof; chỉ đọc các trường đó qua API đúng quyền. ID ở đây thuộc fixture local cùng anchor; kiểm lại runtime sau mọi lần dựng lại.

<a id="table-data"></a>
## Dữ liệu thực thi theo bảng


<a id="table-normal_refund_verifications"></a>

### 1. `normal_refund_verifications`

| id | note | outcome | attempt_id | bank_source | verified_at | verified_by |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | DEMO receipt and receiver matched independently | Success | 7 | DEMO independent center bank statement | 2026-11-12T00:00:00 | 33 |

<a id="table-normal_refund_documents"></a>

### 2. `normal_refund_documents`

| id | sha256 | attempt_id | uploaded_at | uploaded_by | content_type | private_content |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 3281b4652d702a2975a95c4c251cfd9e914c99f39d97225042d6484d10ac9716 | 6 | 2026-11-11T08:00:00 | 30 | application/pdf | OMITTED; authenticated download only |
| 2 | 0df58455318204cc1ba1a9e418b276dcfa1fd8908718e2bf54e0685bceda2909 | 7 | 2026-11-11T08:00:00 | 30 | application/pdf | OMITTED; authenticated download only |

<a id="table-normal_refund_attempts"></a>

### 3. `normal_refund_attempts`

| id | amount | status | currency | claim_key | refund_id | claimed_at | claimed_by | bank_source | receiver_id | verified_at | verified_by | outcome_note | performed_at | bank_reference |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 6 | 900000.0 | Submitted | VND | 852316bf-6922-5326-a044-9f8a12e42a13 | 6 | 2026-11-10T08:00:00 | 30 | NULL | 6 | NULL | NULL | NULL | 2026-11-11T08:00:00 | DEMO-TRANSFER-RF05 |
| 7 | 900000.0 | Verified | VND | 0a07aaa4-9db2-58bb-b9bf-06115213545d | 7 | 2026-11-10T08:00:00 | 30 | DEMO independent center bank statement | 7 | 2026-11-12T00:00:00 | 33 | DEMO independently matched success | 2026-11-11T08:00:00 | DEMO-TRANSFER-RF06 |
| 8 | 900000.0 | Claimed | VND | f2f24d0c-a592-569f-8353-bf265375f65f | 8 | 2026-11-10T08:00:00 | 30 | NULL | 8 | NULL | NULL | NULL | 2026-11-11T08:00:00 | DEMO-TRANSFER-RF07 |

<a id="table-normal_refund_receivers"></a>

### 4. `normal_refund_receivers`

| id | bank | refund_id | verified_at | verified_by | account_name | confirmed_at | confirmed_by | account_number | verification_source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4 | DEMO BANK | 4 | NULL | NULL | DEMO M05 | 2026-11-12T00:00:00 | 6 | DEMOACCOUNTM05 | NULL |
| 5 | DEMO BANK | 5 | 2026-11-12T00:00:00 | 32 | DEMO M06 | 2026-11-12T00:00:00 | 7 | DEMOACCOUNTM06 | DEMO Member and receiving account ownership verified |
| 6 | DEMO BANK | 6 | 2026-11-09T00:00:00 | 32 | DEMO M07 | 2026-11-09T00:00:00 | 8 | DEMOACCOUNTM07 | DEMO Member and receiving account ownership verified |
| 7 | DEMO BANK | 7 | 2026-11-09T00:00:00 | 32 | DEMO M08 | 2026-11-09T00:00:00 | 9 | DEMOACCOUNTM08 | DEMO Member and receiving account ownership verified |
| 8 | DEMO BANK | 8 | 2026-11-09T00:00:00 | 32 | DEMO M09 | 2026-11-09T00:00:00 | 10 | DEMOACCOUNTM09 | DEMO Member and receiving account ownership verified |

<a id="table-normal_refunds"></a>

### 5. `normal_refunds`

| id | due_at | origin | version | class_id | issue_note | receiver_id | first_due_at | approved_receiver_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2 | NULL | MemberChoice | 0 | 14 | NULL | NULL | NULL | NULL |
| 3 | NULL | CenterCancellation | 0 | 15 | NULL | NULL | NULL | NULL |
| 4 | NULL | CenterCancellation | 0 | 15 | NULL | 4 | NULL | NULL |
| 5 | 2026-11-19T23:59:59.999 | CenterCancellation | 0 | 15 | NULL | 5 | 2026-11-19T23:59:59.999 | 5 |
| 6 | 2026-11-16T23:59:59.999 | CenterCancellation | 0 | 21 | NULL | 6 | 2026-11-16T23:59:59.999 | 6 |
| 7 | 2026-11-16T23:59:59.999 | CenterCancellation | 0 | 21 | NULL | 7 | 2026-11-16T23:59:59.999 | 7 |
| 8 | 2026-11-16T23:59:59.999 | CenterCancellation | 0 | 21 | DEMO unknown bank outcome; do not retry transfer | 8 | 2026-11-16T23:59:59.999 | 8 |

<a id="table-normal_transfers"></a>

### 6. `normal_transfers`

Rỗng. Chuyển N05→N11 do Member xác nhận qua API trong demo; M01/M03 đang Waiting, preview không giữ chỗ.


<a id="table-normal_responses"></a>

### 7. `normal_responses`

| id | choice | version | member_id | invoice_id | proposal_id | responded_at |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Waiting | 1 | 2 | 14 | 1 | 2026-11-09T00:00:00 |
| 2 | Refund | 1 | 3 | 15 | 1 | 2026-11-09T00:00:00 |
| 3 | Waiting | 1 | 4 | 16 | 1 | 2026-11-09T00:00:00 |
| 4 | Accept | 1 | 2 | 11 | 2 | 2026-11-09T00:00:00 |
| 5 | Accept | 1 | 3 | 12 | 2 | 2026-11-09T00:00:00 |
| 6 | Accept | 1 | 4 | 13 | 2 | 2026-11-09T00:00:00 |

<a id="table-normal_proposals"></a>

### 8. `normal_proposals`

| id | kind | reason | status | version | class_id | created_at | created_by | shift_days | fingerprint | schedule_json | target_class_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Transfer | DEMO center cannot provide original class | Pending | 1 | 14 | 2026-11-09T00:00:00 | 32 | NULL | 44e6225d98f1448b4d648a2a120229b76a09c2b76c6fd63979202b298d29108a | [{"sessionId" : 122, "date" : "2026-11-20", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 123, "date" : "2026-11-23", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 124, "date" : "2026-11-25", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 125, "date" : "2026-11-27", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 126, "date" : "2026-11-30", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 127, "date" : "2026-12-02", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 128, "date" : "2026-12-04", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 129, "date" : "2026-12-07", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 130, "date" : "2026-12-09", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 131, "date" : "2026-12-11", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 132, "date" : "2026-12-14", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}, {"sessionId" : 133, "date" : "2026-12-16", "start" : "16:30", "end" : "18:00", "coachId" : 17, "roomId" : 12}] | 20 |
| 2 | Postpone | DEMO insufficient enrollment; one postponement applied | Applied | 1 | 13 | 2026-11-09T00:00:00 | 32 | 7 | c8790e8ed66b97f9068c4341accfe407785fb7bfd316a915502eeaaf9265beaf | [{"sessionId" : 37, "date" : "2026-11-20", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 38, "date" : "2026-11-23", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 39, "date" : "2026-11-25", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 40, "date" : "2026-11-27", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 41, "date" : "2026-11-30", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 42, "date" : "2026-12-02", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 43, "date" : "2026-12-04", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 44, "date" : "2026-12-07", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 45, "date" : "2026-12-09", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 46, "date" : "2026-12-11", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 47, "date" : "2026-12-14", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}, {"sessionId" : 48, "date" : "2026-12-16", "start" : "07:45", "end" : "09:15", "coachId" : 18, "roomId" : 1}] | NULL |

<a id="table-scms_data_migration_audit"></a>

### 9. `scms_data_migration_audit`

| after_data | before_data | captured_at | migration_key |
| --- | --- | --- | --- |
| {"mode": "approved-synthetic-demo", "rooms": 20, "accounts": 47, "subjects": 9, "anchor_date": "2026-11-12", "normal_classes": 12, "fixture_version": "normal-phase1-v3", "business_timezone": "Asia/Ho_Chi_Minh", "synthetic_fixture": true, "advanced_templates": 9, "invoice_code_floor": 9000000000000} | {"roles": 5, "rooms": 20, "users": 47, "centers": 1, "classes": 21, "coaches": 19, "members": 23, "holidays": 1, "invoices": 58, "sessions": 173, "subjects": 9, "email_otps": 0, "attendances": 86, "evaluations": 2, "permissions": 18, "system_logs": 4, "chat_messages": 2, "notifications": 3, "receptionists": 2, "coach_days_off": 1, "normal_refunds": 7, "training_plans": 4, "center_managers": 2, "chat_read_state": 2, "class_feedbacks": 2, "member_packages": 7, "session_changes": 0, "specializations": 9, "ai_consultations": 0, "center_incidents": 1, "coach_work_slots": 756, "normal_proposals": 2, "normal_responses": 6, "normal_transfers": 0, "role_permissions": 26, "support_requests": 0, "training_results": 79, "class_enrollments": 50, "chat_conversations": 1, "membership_packages": 0, "payment_resolutions": 8, "subscription_pauses": 0, "coach_work_schedules": 18, "coach_specializations": 19, "normal_class_requests": 0, "advanced_registrations": 10, "normal_refund_attempts": 3, "vnpay_payment_attempts": 0, "normal_refund_documents": 2, "normal_refund_receivers": 5, "notification_recipients": 3, "session_absence_notices": 0, "attendances_backup_flow4": 1, "incident_affected_members": 3, "scms_data_migration_audit": 5, "normal_refund_verifications": 1, "role_permissions_backup_n06": 1, "training_results_backup_flow4": 1, "class_enrollments_backup_flow4": 1} | 2026-11-12T18:20:08.066052 | 20261110_demo_rebuild_v3 |
| {"mode":"fresh-demo","model_retired":true} | {"mode":"fresh-demo"} | 2026-11-12T18:20:08.066694 | 20261106_retire_legacy_memberships_v1 |
| {"rooms": [{"status": "Available", "room_id": 1, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-01", "room_name": "Khu Bắn cung 01", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 2, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-02", "room_name": "Khu Bắn cung 02", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 3, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-01", "room_name": "Sân Cầu lông 01", "facility_type": "Indoor"}, {"status": "Available", "room_id": 4, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-02", "room_name": "Sân Cầu lông 02", "facility_type": "Indoor"}, {"status": "Available", "room_id": 5, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-01", "room_name": "Sân Bóng chuyền 01", "facility_type": "Indoor"}, {"status": "Available", "room_id": 6, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-02", "room_name": "Sân Bóng chuyền 02", "facility_type": "Indoor"}, {"status": "Available", "room_id": 7, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-01", "room_name": "Sân Bóng rổ 01", "facility_type": "Indoor"}, {"status": "Available", "room_id": 8, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-02", "room_name": "Sân Bóng rổ 02", "facility_type": "Indoor"}, {"status": "Available", "room_id": 9, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-01", "room_name": "Sân Bóng đá 01", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 10, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-02", "room_name": "Sân Bóng đá 02", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 11, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-01", "room_name": "Sân Pickleball 01", "facility_type": "Indoor"}, {"status": "Available", "room_id": 12, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-02", "room_name": "Sân Pickleball 02", "facility_type": "Indoor"}, {"status": "Available", "room_id": 13, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-01", "room_name": "Hồ bơi 01", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 14, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-02", "room_name": "Hồ bơi 02", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 15, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-01", "room_name": "Sân Tennis 01", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 16, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-02", "room_name": "Sân Tennis 02", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 17, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-01", "room_name": "Bàn Bóng bàn 01", "facility_type": "Indoor"}, {"status": "Available", "room_id": 18, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-02", "room_name": "Bàn Bóng bàn 02", "facility_type": "Indoor"}, {"status": "Available", "room_id": 19, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-03", "room_name": "Bàn Bóng bàn 03", "facility_type": "Indoor"}, {"status": "Available", "room_id": 20, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-04", "room_name": "Bàn Bóng bàn 04", "facility_type": "Indoor"}], "sessions": null} | {"rooms": null, "sessions": null} | 2026-11-12T18:20:02.993059 | 20261102_room_catalog_v1 |
| [{"status": "Available", "room_id": 1, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-01", "room_name": "Khu Bắn cung 01", "activity_type": "ARCHERY", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 2, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-02", "room_name": "Khu Bắn cung 02", "activity_type": "ARCHERY", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 3, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-01", "room_name": "Sân Cầu lông 01", "activity_type": "BADMINTON", "facility_type": "Indoor"}, {"status": "Available", "room_id": 4, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-02", "room_name": "Sân Cầu lông 02", "activity_type": "BADMINTON", "facility_type": "Indoor"}, {"status": "Available", "room_id": 5, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-01", "room_name": "Sân Bóng chuyền 01", "activity_type": "VOLLEYBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 6, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-02", "room_name": "Sân Bóng chuyền 02", "activity_type": "VOLLEYBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 7, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-01", "room_name": "Sân Bóng rổ 01", "activity_type": "BASKETBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 8, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-02", "room_name": "Sân Bóng rổ 02", "activity_type": "BASKETBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 9, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-01", "room_name": "Sân Bóng đá 01", "activity_type": "FOOTBALL", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 10, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-02", "room_name": "Sân Bóng đá 02", "activity_type": "FOOTBALL", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 11, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-01", "room_name": "Sân Pickleball 01", "activity_type": "PICKLEBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 12, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-02", "room_name": "Sân Pickleball 02", "activity_type": "PICKLEBALL", "facility_type": "Indoor"}, {"status": "Available", "room_id": 13, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-01", "room_name": "Hồ bơi 01", "activity_type": "SWIMMING", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 14, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-02", "room_name": "Hồ bơi 02", "activity_type": "SWIMMING", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 15, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-01", "room_name": "Sân Tennis 01", "activity_type": "TENNIS", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 16, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-02", "room_name": "Sân Tennis 02", "activity_type": "TENNIS", "facility_type": "Outdoor"}, {"status": "Available", "room_id": 17, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-01", "room_name": "Bàn Bóng bàn 01", "activity_type": "TABLE_TENNIS", "facility_type": "Indoor"}, {"status": "Available", "room_id": 18, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-02", "room_name": "Bàn Bóng bàn 02", "activity_type": "TABLE_TENNIS", "facility_type": "Indoor"}, {"status": "Available", "room_id": 19, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-03", "room_name": "Bàn Bóng bàn 03", "activity_type": "TABLE_TENNIS", "facility_type": "Indoor"}, {"status": "Available", "room_id": 20, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-04", "room_name": "Bàn Bóng bàn 04", "activity_type": "TABLE_TENNIS", "facility_type": "Indoor"}] | [{"status": "Available", "room_id": 1, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-01", "room_name": "Khu Bắn cung 01", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 2, "capacity": 5, "location": "Khu C – Ngoài trời", "center_id": 1, "room_code": "C01-ARC-02", "room_name": "Khu Bắn cung 02", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 3, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-01", "room_name": "Sân Cầu lông 01", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 4, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-BAD-02", "room_name": "Sân Cầu lông 02", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 5, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-01", "room_name": "Sân Bóng chuyền 01", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 6, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BC-02", "room_name": "Sân Bóng chuyền 02", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 7, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-01", "room_name": "Sân Bóng rổ 01", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 8, "capacity": 8, "location": "Nhà thi đấu A", "center_id": 1, "room_code": "C01-BR-02", "room_name": "Sân Bóng rổ 02", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 9, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-01", "room_name": "Sân Bóng đá 01", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 10, "capacity": 15, "location": "Khu B – Ngoài trời", "center_id": 1, "room_code": "C01-FOOT-02", "room_name": "Sân Bóng đá 02", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 11, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-01", "room_name": "Sân Pickleball 01", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 12, "capacity": 4, "location": "Nhà thi đấu B", "center_id": 1, "room_code": "C01-PICK-02", "room_name": "Sân Pickleball 02", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 13, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-01", "room_name": "Hồ bơi 01", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 14, "capacity": 10, "location": "Khu hồ bơi", "center_id": 1, "room_code": "C01-SWIM-02", "room_name": "Hồ bơi 02", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 15, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-01", "room_name": "Sân Tennis 01", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 16, "capacity": 4, "location": "Khu A – Ngoài trời", "center_id": 1, "room_code": "C01-TEN-02", "room_name": "Sân Tennis 02", "activity_type": null, "facility_type": "Outdoor"}, {"status": "Available", "room_id": 17, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-01", "room_name": "Bàn Bóng bàn 01", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 18, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-02", "room_name": "Bàn Bóng bàn 02", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 19, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-03", "room_name": "Bàn Bóng bàn 03", "activity_type": null, "facility_type": "Indoor"}, {"status": "Available", "room_id": 20, "capacity": 2, "location": "Phòng Bóng bàn", "center_id": 1, "room_code": "C01-TT-04", "room_name": "Bàn Bóng bàn 04", "activity_type": null, "facility_type": "Indoor"}] | 2026-11-12T18:20:03.081253 | 20261109_room_activity_v1 |
| [{"subject_id":1,"subject_name":"Bóng đá","description":"Môn thể thao vua ngoài trời, rèn luyện thể lực và phối hợp","normal_price":1000000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-da","outdoor":true}, <br> {"subject_id":2,"subject_name":"Quần vợt (Tennis)","description":"Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực","normal_price":2500000.00,"advanced_price":8000000.00,"status":"Active","slug":"quan-vot-tennis","outdoor":true}, <br> {"subject_id":3,"subject_name":"Bơi lội","description":"Môn thể thao dưới nước ngoài trời, phát triển toàn diện cơ thể","normal_price":1500000.00,"advanced_price":4500000.00,"status":"Active","slug":"boi-loi","outdoor":true}, <br> {"subject_id":4,"subject_name":"Bắn cung","description":"Môn ngoài trời, rèn luyện sự tập trung cao độ và tính kiên nhẫn","normal_price":2000000.00,"advanced_price":6000000.00,"status":"Active","slug":"ban-cung","outdoor":true}, <br> {"subject_id":5,"subject_name":"Cầu lông","description":"Môn thể thao nhanh nhẹn, linh hoạt trong nhà","normal_price":900000.00,"advanced_price":3000000.00,"status":"Active","slug":"cau-long","outdoor":false}, <br> {"subject_id":6,"subject_name":"Bóng bàn","description":"Phản xạ nhanh, khéo léo với nhịp độ cao trong nhà","normal_price":800000.00,"advanced_price":2500000.00,"status":"Active","slug":"bong-ban","outdoor":false}, <br> {"subject_id":7,"subject_name":"Bóng rổ","description":"Môn thể thao đồng đội tốc độ và sức bật trong nhà","normal_price":1200000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-ro","outdoor":false}, <br> {"subject_id":8,"subject_name":"Bóng chuyền","description":"Phối hợp đồng đội và kỹ thuật phản xạ trên lưới trong nhà","normal_price":1000000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-chuyen","outdoor":false}, <br> {"subject_id":9,"subject_name":"Pickleball","description":"Môn thể thao thời thượng trong nhà, kết hợp tennis, bóng bàn","normal_price":1800000.00,"advanced_price":5000000.00,"status":"Active","slug":"pickleball","outdoor":false}] | [{"subject_id":1,"subject_name":"Bóng đá","description":"Môn thể thao vua ngoài trời, rèn luyện thể lực và phối hợp","normal_price":1000000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-da","outdoor":true}, <br> {"subject_id":2,"subject_name":"Quần vợt (Tennis)","description":"Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực","normal_price":2500000.00,"advanced_price":8000000.00,"status":"Active","slug":"quan-vot-tennis","outdoor":true}, <br> {"subject_id":3,"subject_name":"Bơi lội","description":"Môn thể thao dưới nước ngoài trời, phát triển toàn diện cơ thể","normal_price":1500000.00,"advanced_price":4500000.00,"status":"Active","slug":"boi-loi","outdoor":true}, <br> {"subject_id":4,"subject_name":"Bắn cung","description":"Môn ngoài trời, rèn luyện sự tập trung cao độ và tính kiên nhẫn","normal_price":2000000.00,"advanced_price":6000000.00,"status":"Active","slug":"ban-cung","outdoor":true}, <br> {"subject_id":5,"subject_name":"Cầu lông","description":"Môn thể thao nhanh nhẹn, linh hoạt trong nhà","normal_price":900000.00,"advanced_price":3000000.00,"status":"Active","slug":"cau-long","outdoor":false}, <br> {"subject_id":6,"subject_name":"Bóng bàn","description":"Phản xạ nhanh, khéo léo với nhịp độ cao trong nhà","normal_price":800000.00,"advanced_price":2500000.00,"status":"Active","slug":"bong-ban","outdoor":false}, <br> {"subject_id":7,"subject_name":"Bóng rổ","description":"Môn thể thao đồng đội tốc độ và sức bật trong nhà","normal_price":1200000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-ro","outdoor":false}, <br> {"subject_id":8,"subject_name":"Bóng chuyền","description":"Phối hợp đồng đội và kỹ thuật phản xạ trên lưới trong nhà","normal_price":1000000.00,"advanced_price":3500000.00,"status":"Active","slug":"bong-chuyen","outdoor":false}, <br> {"subject_id":9,"subject_name":"Pickleball","description":"Môn thể thao thời thượng trong nhà, kết hợp tennis, bóng bàn","normal_price":1800000.00,"advanced_price":5000000.00,"status":"Active","slug":"pickleball","outdoor":false}] | 2026-11-12T18:20:02.51243 | 09_approved_catalog |

<a id="table-payment_resolutions"></a>

### 10. `payment_resolutions`

| id | amount | reason | status | evidence | created_at | created_by | invoice_id | approved_at | approved_by | completed_at | completed_by | outcome_note | performed_at | resolution_type |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 3000000.0 | DEMO COUNTER_MISTAKE | Approved | NULL | 2026-11-10T09:00:00 | 32 | 56 | 2026-11-10T09:01:00 | 32 | NULL | NULL | NULL | NULL | CashCorrection |
| 2 | 1800000.0 | DEMO MEMBER_REQUEST | AwaitingReceiver | NULL | 2026-11-09T00:00:00 | 32 | 15 | NULL | NULL | NULL | NULL | NULL | NULL | Refund |
| 3 | 1200000.0 | DEMO CENTER_CANCELLATION | AwaitingReceiver | NULL | 2026-11-09T00:00:00 | 32 | 17 | NULL | NULL | NULL | NULL | NULL | NULL | Refund |
| 4 | 1200000.0 | DEMO CENTER_CANCELLATION | AwaitingApproval | NULL | 2026-11-09T00:00:00 | 32 | 18 | NULL | NULL | NULL | NULL | NULL | NULL | Refund |
| 5 | 1200000.0 | DEMO CENTER_CANCELLATION | Approved | NULL | 2026-11-09T00:00:00 | 32 | 19 | 2026-11-12T00:00:00 | 32 | NULL | NULL | NULL | NULL | Refund |
| 6 | 900000.0 | DEMO CENTER_CANCELLATION | AwaitingVerification | NULL | 2026-10-22T16:30:00 | 32 | 48 | 2026-11-09T00:00:00 | 32 | NULL | NULL | NULL | NULL | Refund |
| 7 | 900000.0 | DEMO CENTER_CANCELLATION | Completed | DEMO private bank evidence retained | 2026-10-22T16:30:00 | 32 | 49 | 2026-11-09T00:00:00 | 32 | 2026-11-12T00:00:00 | 33 | NULL | 2026-11-11T08:00:00 | Refund |
| 8 | 900000.0 | DEMO CENTER_CANCELLATION | NeedsReview | NULL | 2026-10-22T16:30:00 | 32 | 50 | 2026-11-09T00:00:00 | 32 | NULL | NULL | NULL | NULL | Refund |

<a id="table-incident_affected_members"></a>

### 11. `incident_affected_members`

| member_id | incident_id |
| --- | --- |
| 2 | 1 |
| 3 | 1 |
| 4 | 1 |

<a id="table-center_incidents"></a>

### 12. `center_incidents`

| id | reason | status | ended_at | evidence | created_at | created_by | session_id | started_at | request_key | resolved_at | resolved_by | deadline_after | deadline_before | original_deadline | recovered_credits | session_compensated |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | DEMO buổi không tổ chức do trung tâm | Resolved | 2026-11-10T14:45:00 | DEMO xác minh buổi không tổ chức | 2026-11-10T14:45:00 | 32 | 116 | 2026-11-10T14:45:00 | INC-N10 | 2026-11-10T14:45:00 | 32 | NULL | NULL | NULL | 0 | true |

<a id="table-session_changes"></a>

### 13. `session_changes`

Rỗng. Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.


<a id="table-session_absence_notices"></a>

### 14. `session_absence_notices`

Rỗng. Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.


<a id="table-advanced_registrations"></a>

### 15. `advanced_registrations`

| id | reason | status | coach_id | member_id | created_at | subject_id | accepted_at | accepted_by | request_key | subscription_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 10 | NULL | Invoiced | 14 | 9 | 2026-11-07T08:00:00 | 3 | 2026-11-08T08:00:00 | 14 | DEMO-SUB07 | 7 |
| 1 | NULL | Requested | 14 | 37 | 2026-11-07T08:00:00 | 3 | NULL | NULL | DEMO-AR01 | NULL |
| 2 | NULL | Accepted | 16 | 38 | 2026-11-07T08:00:00 | 9 | 2026-11-08T08:00:00 | 16 | DEMO-AR02 | NULL |
| 3 | DEMO Coach từ chối yêu cầu | Rejected | 18 | 39 | 2026-11-07T08:00:00 | 4 | NULL | NULL | DEMO-AR03 | NULL |
| 4 | NULL | Invoiced | 12 | 3 | 2026-11-07T08:00:00 | 2 | 2026-11-08T08:00:00 | 12 | DEMO-SUB01 | 1 |
| 5 | NULL | Invoiced | 12 | 4 | 2026-11-07T08:00:00 | 2 | 2026-11-08T08:00:00 | 12 | DEMO-SUB02 | 2 |
| 6 | NULL | Invoiced | 26 | 5 | 2026-11-07T08:00:00 | 5 | 2026-11-08T08:00:00 | 26 | DEMO-SUB03 | 3 |
| 7 | NULL | Invoiced | 12 | 6 | 2026-11-07T08:00:00 | 2 | 2026-11-08T08:00:00 | 12 | DEMO-SUB04 | 4 |
| 8 | NULL | Invoiced | 12 | 7 | 2026-11-07T08:00:00 | 2 | 2026-11-08T08:00:00 | 12 | DEMO-SUB05 | 5 |
| 9 | NULL | Invoiced | 26 | 8 | 2026-11-07T08:00:00 | 5 | 2026-11-08T08:00:00 | 26 | DEMO-SUB06 | 6 |

<a id="table-coach_days_off"></a>

### 16. `coach_days_off`

| day_off | coach_id |
| --- | --- |
| 2027-01-11 | 17 |

<a id="table-coach_work_slots"></a>

### 17. `coach_work_slots`

| coach_id | slot_key |
| --- | --- |
| 12 | FRIDAY:1 |
| 12 | FRIDAY:2 |
| 12 | FRIDAY:3 |
| 12 | FRIDAY:4 |
| 12 | FRIDAY:5 |
| 12 | FRIDAY:6 |
| 12 | FRIDAY:7 |
| 12 | MONDAY:1 |
| 12 | MONDAY:2 |
| 12 | MONDAY:3 |
| 12 | MONDAY:4 |
| 12 | MONDAY:5 |
| 12 | MONDAY:6 |
| 12 | MONDAY:7 |
| 12 | SATURDAY:1 |
| 12 | SATURDAY:2 |
| 12 | SATURDAY:3 |
| 12 | SATURDAY:4 |
| 12 | SATURDAY:5 |
| 12 | SATURDAY:6 |
| 12 | SATURDAY:7 |
| 12 | THURSDAY:1 |
| 12 | THURSDAY:2 |
| 12 | THURSDAY:3 |
| 12 | THURSDAY:4 |
| 12 | THURSDAY:5 |
| 12 | THURSDAY:6 |
| 12 | THURSDAY:7 |
| 12 | TUESDAY:1 |
| 12 | TUESDAY:2 |
| 12 | TUESDAY:3 |
| 12 | TUESDAY:4 |
| 12 | TUESDAY:5 |
| 12 | TUESDAY:6 |
| 12 | TUESDAY:7 |
| 12 | WEDNESDAY:1 |
| 12 | WEDNESDAY:2 |
| 12 | WEDNESDAY:3 |
| 12 | WEDNESDAY:4 |
| 12 | WEDNESDAY:5 |
| 12 | WEDNESDAY:6 |
| 12 | WEDNESDAY:7 |
| 13 | FRIDAY:1 |
| 13 | FRIDAY:2 |
| 13 | FRIDAY:3 |
| 13 | FRIDAY:4 |
| 13 | FRIDAY:5 |
| 13 | FRIDAY:6 |
| 13 | FRIDAY:7 |
| 13 | MONDAY:1 |
| 13 | MONDAY:2 |
| 13 | MONDAY:3 |
| 13 | MONDAY:4 |
| 13 | MONDAY:5 |
| 13 | MONDAY:6 |
| 13 | MONDAY:7 |
| 13 | SATURDAY:1 |
| 13 | SATURDAY:2 |
| 13 | SATURDAY:3 |
| 13 | SATURDAY:4 |
| 13 | SATURDAY:5 |
| 13 | SATURDAY:6 |
| 13 | SATURDAY:7 |
| 13 | THURSDAY:1 |
| 13 | THURSDAY:2 |
| 13 | THURSDAY:3 |
| 13 | THURSDAY:4 |
| 13 | THURSDAY:5 |
| 13 | THURSDAY:6 |
| 13 | THURSDAY:7 |
| 13 | TUESDAY:1 |
| 13 | TUESDAY:2 |
| 13 | TUESDAY:3 |
| 13 | TUESDAY:4 |
| 13 | TUESDAY:5 |
| 13 | TUESDAY:6 |
| 13 | TUESDAY:7 |
| 13 | WEDNESDAY:1 |
| 13 | WEDNESDAY:2 |
| 13 | WEDNESDAY:3 |
| 13 | WEDNESDAY:4 |
| 13 | WEDNESDAY:5 |
| 13 | WEDNESDAY:6 |
| 13 | WEDNESDAY:7 |
| 14 | FRIDAY:1 |
| 14 | FRIDAY:2 |
| 14 | FRIDAY:3 |
| 14 | FRIDAY:4 |
| 14 | FRIDAY:5 |
| 14 | FRIDAY:6 |
| 14 | FRIDAY:7 |
| 14 | MONDAY:1 |
| 14 | MONDAY:2 |
| 14 | MONDAY:3 |
| 14 | MONDAY:4 |
| 14 | MONDAY:5 |
| 14 | MONDAY:6 |
| 14 | MONDAY:7 |
| 14 | SATURDAY:1 |
| 14 | SATURDAY:2 |
| 14 | SATURDAY:3 |
| 14 | SATURDAY:4 |
| 14 | SATURDAY:5 |
| 14 | SATURDAY:6 |
| 14 | SATURDAY:7 |
| 14 | THURSDAY:1 |
| 14 | THURSDAY:2 |
| 14 | THURSDAY:3 |
| 14 | THURSDAY:4 |
| 14 | THURSDAY:5 |
| 14 | THURSDAY:6 |
| 14 | THURSDAY:7 |
| 14 | TUESDAY:1 |
| 14 | TUESDAY:2 |
| 14 | TUESDAY:3 |
| 14 | TUESDAY:4 |
| 14 | TUESDAY:5 |
| 14 | TUESDAY:6 |
| 14 | TUESDAY:7 |
| 14 | WEDNESDAY:1 |
| 14 | WEDNESDAY:2 |
| 14 | WEDNESDAY:3 |
| 14 | WEDNESDAY:4 |
| 14 | WEDNESDAY:5 |
| 14 | WEDNESDAY:6 |
| 14 | WEDNESDAY:7 |
| 15 | FRIDAY:1 |
| 15 | FRIDAY:2 |
| 15 | FRIDAY:3 |
| 15 | FRIDAY:4 |
| 15 | FRIDAY:5 |
| 15 | FRIDAY:6 |
| 15 | FRIDAY:7 |
| 15 | MONDAY:1 |
| 15 | MONDAY:2 |
| 15 | MONDAY:3 |
| 15 | MONDAY:4 |
| 15 | MONDAY:5 |
| 15 | MONDAY:6 |
| 15 | MONDAY:7 |
| 15 | SATURDAY:1 |
| 15 | SATURDAY:2 |
| 15 | SATURDAY:3 |
| 15 | SATURDAY:4 |
| 15 | SATURDAY:5 |
| 15 | SATURDAY:6 |
| 15 | SATURDAY:7 |
| 15 | THURSDAY:1 |
| 15 | THURSDAY:2 |
| 15 | THURSDAY:3 |
| 15 | THURSDAY:4 |
| 15 | THURSDAY:5 |
| 15 | THURSDAY:6 |
| 15 | THURSDAY:7 |
| 15 | TUESDAY:1 |
| 15 | TUESDAY:2 |
| 15 | TUESDAY:3 |
| 15 | TUESDAY:4 |
| 15 | TUESDAY:5 |
| 15 | TUESDAY:6 |
| 15 | TUESDAY:7 |
| 15 | WEDNESDAY:1 |
| 15 | WEDNESDAY:2 |
| 15 | WEDNESDAY:3 |
| 15 | WEDNESDAY:4 |
| 15 | WEDNESDAY:5 |
| 15 | WEDNESDAY:6 |
| 15 | WEDNESDAY:7 |
| 16 | FRIDAY:1 |
| 16 | FRIDAY:2 |
| 16 | FRIDAY:3 |
| 16 | FRIDAY:4 |
| 16 | FRIDAY:5 |
| 16 | FRIDAY:6 |
| 16 | FRIDAY:7 |
| 16 | MONDAY:1 |
| 16 | MONDAY:2 |
| 16 | MONDAY:3 |
| 16 | MONDAY:4 |
| 16 | MONDAY:5 |
| 16 | MONDAY:6 |
| 16 | MONDAY:7 |
| 16 | SATURDAY:1 |
| 16 | SATURDAY:2 |
| 16 | SATURDAY:3 |
| 16 | SATURDAY:4 |
| 16 | SATURDAY:5 |
| 16 | SATURDAY:6 |
| 16 | SATURDAY:7 |
| 16 | THURSDAY:1 |
| 16 | THURSDAY:2 |
| 16 | THURSDAY:3 |
| 16 | THURSDAY:4 |
| 16 | THURSDAY:5 |
| 16 | THURSDAY:6 |
| 16 | THURSDAY:7 |
| 16 | TUESDAY:1 |
| 16 | TUESDAY:2 |
| 16 | TUESDAY:3 |
| 16 | TUESDAY:4 |
| 16 | TUESDAY:5 |
| 16 | TUESDAY:6 |
| 16 | TUESDAY:7 |
| 16 | WEDNESDAY:1 |
| 16 | WEDNESDAY:2 |
| 16 | WEDNESDAY:3 |
| 16 | WEDNESDAY:4 |
| 16 | WEDNESDAY:5 |
| 16 | WEDNESDAY:6 |
| 16 | WEDNESDAY:7 |
| 17 | FRIDAY:1 |
| 17 | FRIDAY:2 |
| 17 | FRIDAY:3 |
| 17 | FRIDAY:4 |
| 17 | FRIDAY:5 |
| 17 | FRIDAY:6 |
| 17 | FRIDAY:7 |
| 17 | MONDAY:1 |
| 17 | MONDAY:2 |
| 17 | MONDAY:3 |
| 17 | MONDAY:4 |
| 17 | MONDAY:5 |
| 17 | MONDAY:6 |
| 17 | MONDAY:7 |
| 17 | SATURDAY:1 |
| 17 | SATURDAY:2 |
| 17 | SATURDAY:3 |
| 17 | SATURDAY:4 |
| 17 | SATURDAY:5 |
| 17 | SATURDAY:6 |
| 17 | SATURDAY:7 |
| 17 | THURSDAY:1 |
| 17 | THURSDAY:2 |
| 17 | THURSDAY:3 |
| 17 | THURSDAY:4 |
| 17 | THURSDAY:5 |
| 17 | THURSDAY:6 |
| 17 | THURSDAY:7 |
| 17 | TUESDAY:1 |
| 17 | TUESDAY:2 |
| 17 | TUESDAY:3 |
| 17 | TUESDAY:4 |
| 17 | TUESDAY:5 |
| 17 | TUESDAY:6 |
| 17 | TUESDAY:7 |
| 17 | WEDNESDAY:1 |
| 17 | WEDNESDAY:2 |
| 17 | WEDNESDAY:3 |
| 17 | WEDNESDAY:4 |
| 17 | WEDNESDAY:5 |
| 17 | WEDNESDAY:6 |
| 17 | WEDNESDAY:7 |
| 18 | FRIDAY:1 |
| 18 | FRIDAY:2 |
| 18 | FRIDAY:3 |
| 18 | FRIDAY:4 |
| 18 | FRIDAY:5 |
| 18 | FRIDAY:6 |
| 18 | FRIDAY:7 |
| 18 | MONDAY:1 |
| 18 | MONDAY:2 |
| 18 | MONDAY:3 |
| 18 | MONDAY:4 |
| 18 | MONDAY:5 |
| 18 | MONDAY:6 |
| 18 | MONDAY:7 |
| 18 | SATURDAY:1 |
| 18 | SATURDAY:2 |
| 18 | SATURDAY:3 |
| 18 | SATURDAY:4 |
| 18 | SATURDAY:5 |
| 18 | SATURDAY:6 |
| 18 | SATURDAY:7 |
| 18 | THURSDAY:1 |
| 18 | THURSDAY:2 |
| 18 | THURSDAY:3 |
| 18 | THURSDAY:4 |
| 18 | THURSDAY:5 |
| 18 | THURSDAY:6 |
| 18 | THURSDAY:7 |
| 18 | TUESDAY:1 |
| 18 | TUESDAY:2 |
| 18 | TUESDAY:3 |
| 18 | TUESDAY:4 |
| 18 | TUESDAY:5 |
| 18 | TUESDAY:6 |
| 18 | TUESDAY:7 |
| 18 | WEDNESDAY:1 |
| 18 | WEDNESDAY:2 |
| 18 | WEDNESDAY:3 |
| 18 | WEDNESDAY:4 |
| 18 | WEDNESDAY:5 |
| 18 | WEDNESDAY:6 |
| 18 | WEDNESDAY:7 |
| 19 | FRIDAY:1 |
| 19 | FRIDAY:2 |
| 19 | FRIDAY:3 |
| 19 | FRIDAY:4 |
| 19 | FRIDAY:5 |
| 19 | FRIDAY:6 |
| 19 | FRIDAY:7 |
| 19 | MONDAY:1 |
| 19 | MONDAY:2 |
| 19 | MONDAY:3 |
| 19 | MONDAY:4 |
| 19 | MONDAY:5 |
| 19 | MONDAY:6 |
| 19 | MONDAY:7 |
| 19 | SATURDAY:1 |
| 19 | SATURDAY:2 |
| 19 | SATURDAY:3 |
| 19 | SATURDAY:4 |
| 19 | SATURDAY:5 |
| 19 | SATURDAY:6 |
| 19 | SATURDAY:7 |
| 19 | THURSDAY:1 |
| 19 | THURSDAY:2 |
| 19 | THURSDAY:3 |
| 19 | THURSDAY:4 |
| 19 | THURSDAY:5 |
| 19 | THURSDAY:6 |
| 19 | THURSDAY:7 |
| 19 | TUESDAY:1 |
| 19 | TUESDAY:2 |
| 19 | TUESDAY:3 |
| 19 | TUESDAY:4 |
| 19 | TUESDAY:5 |
| 19 | TUESDAY:6 |
| 19 | TUESDAY:7 |
| 19 | WEDNESDAY:1 |
| 19 | WEDNESDAY:2 |
| 19 | WEDNESDAY:3 |
| 19 | WEDNESDAY:4 |
| 19 | WEDNESDAY:5 |
| 19 | WEDNESDAY:6 |
| 19 | WEDNESDAY:7 |
| 20 | FRIDAY:1 |
| 20 | FRIDAY:2 |
| 20 | FRIDAY:3 |
| 20 | FRIDAY:4 |
| 20 | FRIDAY:5 |
| 20 | FRIDAY:6 |
| 20 | FRIDAY:7 |
| 20 | MONDAY:1 |
| 20 | MONDAY:2 |
| 20 | MONDAY:3 |
| 20 | MONDAY:4 |
| 20 | MONDAY:5 |
| 20 | MONDAY:6 |
| 20 | MONDAY:7 |
| 20 | SATURDAY:1 |
| 20 | SATURDAY:2 |
| 20 | SATURDAY:3 |
| 20 | SATURDAY:4 |
| 20 | SATURDAY:5 |
| 20 | SATURDAY:6 |
| 20 | SATURDAY:7 |
| 20 | THURSDAY:1 |
| 20 | THURSDAY:2 |
| 20 | THURSDAY:3 |
| 20 | THURSDAY:4 |
| 20 | THURSDAY:5 |
| 20 | THURSDAY:6 |
| 20 | THURSDAY:7 |
| 20 | TUESDAY:1 |
| 20 | TUESDAY:2 |
| 20 | TUESDAY:3 |
| 20 | TUESDAY:4 |
| 20 | TUESDAY:5 |
| 20 | TUESDAY:6 |
| 20 | TUESDAY:7 |
| 20 | WEDNESDAY:1 |
| 20 | WEDNESDAY:2 |
| 20 | WEDNESDAY:3 |
| 20 | WEDNESDAY:4 |
| 20 | WEDNESDAY:5 |
| 20 | WEDNESDAY:6 |
| 20 | WEDNESDAY:7 |
| 21 | FRIDAY:1 |
| 21 | FRIDAY:2 |
| 21 | FRIDAY:3 |
| 21 | FRIDAY:4 |
| 21 | FRIDAY:5 |
| 21 | FRIDAY:6 |
| 21 | FRIDAY:7 |
| 21 | MONDAY:1 |
| 21 | MONDAY:2 |
| 21 | MONDAY:3 |
| 21 | MONDAY:4 |
| 21 | MONDAY:5 |
| 21 | MONDAY:6 |
| 21 | MONDAY:7 |
| 21 | SATURDAY:1 |
| 21 | SATURDAY:2 |
| 21 | SATURDAY:3 |
| 21 | SATURDAY:4 |
| 21 | SATURDAY:5 |
| 21 | SATURDAY:6 |
| 21 | SATURDAY:7 |
| 21 | THURSDAY:1 |
| 21 | THURSDAY:2 |
| 21 | THURSDAY:3 |
| 21 | THURSDAY:4 |
| 21 | THURSDAY:5 |
| 21 | THURSDAY:6 |
| 21 | THURSDAY:7 |
| 21 | TUESDAY:1 |
| 21 | TUESDAY:2 |
| 21 | TUESDAY:3 |
| 21 | TUESDAY:4 |
| 21 | TUESDAY:5 |
| 21 | TUESDAY:6 |
| 21 | TUESDAY:7 |
| 21 | WEDNESDAY:1 |
| 21 | WEDNESDAY:2 |
| 21 | WEDNESDAY:3 |
| 21 | WEDNESDAY:4 |
| 21 | WEDNESDAY:5 |
| 21 | WEDNESDAY:6 |
| 21 | WEDNESDAY:7 |
| 22 | FRIDAY:1 |
| 22 | FRIDAY:2 |
| 22 | FRIDAY:3 |
| 22 | FRIDAY:4 |
| 22 | FRIDAY:5 |
| 22 | FRIDAY:6 |
| 22 | FRIDAY:7 |
| 22 | MONDAY:1 |
| 22 | MONDAY:2 |
| 22 | MONDAY:3 |
| 22 | MONDAY:4 |
| 22 | MONDAY:5 |
| 22 | MONDAY:6 |
| 22 | MONDAY:7 |
| 22 | SATURDAY:1 |
| 22 | SATURDAY:2 |
| 22 | SATURDAY:3 |
| 22 | SATURDAY:4 |
| 22 | SATURDAY:5 |
| 22 | SATURDAY:6 |
| 22 | SATURDAY:7 |
| 22 | THURSDAY:1 |
| 22 | THURSDAY:2 |
| 22 | THURSDAY:3 |
| 22 | THURSDAY:4 |
| 22 | THURSDAY:5 |
| 22 | THURSDAY:6 |
| 22 | THURSDAY:7 |
| 22 | TUESDAY:1 |
| 22 | TUESDAY:2 |
| 22 | TUESDAY:3 |
| 22 | TUESDAY:4 |
| 22 | TUESDAY:5 |
| 22 | TUESDAY:6 |
| 22 | TUESDAY:7 |
| 22 | WEDNESDAY:1 |
| 22 | WEDNESDAY:2 |
| 22 | WEDNESDAY:3 |
| 22 | WEDNESDAY:4 |
| 22 | WEDNESDAY:5 |
| 22 | WEDNESDAY:6 |
| 22 | WEDNESDAY:7 |
| 23 | FRIDAY:1 |
| 23 | FRIDAY:2 |
| 23 | FRIDAY:3 |
| 23 | FRIDAY:4 |
| 23 | FRIDAY:5 |
| 23 | FRIDAY:6 |
| 23 | FRIDAY:7 |
| 23 | MONDAY:1 |
| 23 | MONDAY:2 |
| 23 | MONDAY:3 |
| 23 | MONDAY:4 |
| 23 | MONDAY:5 |
| 23 | MONDAY:6 |
| 23 | MONDAY:7 |
| 23 | SATURDAY:1 |
| 23 | SATURDAY:2 |
| 23 | SATURDAY:3 |
| 23 | SATURDAY:4 |
| 23 | SATURDAY:5 |
| 23 | SATURDAY:6 |
| 23 | SATURDAY:7 |
| 23 | THURSDAY:1 |
| 23 | THURSDAY:2 |
| 23 | THURSDAY:3 |
| 23 | THURSDAY:4 |
| 23 | THURSDAY:5 |
| 23 | THURSDAY:6 |
| 23 | THURSDAY:7 |
| 23 | TUESDAY:1 |
| 23 | TUESDAY:2 |
| 23 | TUESDAY:3 |
| 23 | TUESDAY:4 |
| 23 | TUESDAY:5 |
| 23 | TUESDAY:6 |
| 23 | TUESDAY:7 |
| 23 | WEDNESDAY:1 |
| 23 | WEDNESDAY:2 |
| 23 | WEDNESDAY:3 |
| 23 | WEDNESDAY:4 |
| 23 | WEDNESDAY:5 |
| 23 | WEDNESDAY:6 |
| 23 | WEDNESDAY:7 |
| 24 | FRIDAY:1 |
| 24 | FRIDAY:2 |
| 24 | FRIDAY:3 |
| 24 | FRIDAY:4 |
| 24 | FRIDAY:5 |
| 24 | FRIDAY:6 |
| 24 | FRIDAY:7 |
| 24 | MONDAY:1 |
| 24 | MONDAY:2 |
| 24 | MONDAY:3 |
| 24 | MONDAY:4 |
| 24 | MONDAY:5 |
| 24 | MONDAY:6 |
| 24 | MONDAY:7 |
| 24 | SATURDAY:1 |
| 24 | SATURDAY:2 |
| 24 | SATURDAY:3 |
| 24 | SATURDAY:4 |
| 24 | SATURDAY:5 |
| 24 | SATURDAY:6 |
| 24 | SATURDAY:7 |
| 24 | THURSDAY:1 |
| 24 | THURSDAY:2 |
| 24 | THURSDAY:3 |
| 24 | THURSDAY:4 |
| 24 | THURSDAY:5 |
| 24 | THURSDAY:6 |
| 24 | THURSDAY:7 |
| 24 | TUESDAY:1 |
| 24 | TUESDAY:2 |
| 24 | TUESDAY:3 |
| 24 | TUESDAY:4 |
| 24 | TUESDAY:5 |
| 24 | TUESDAY:6 |
| 24 | TUESDAY:7 |
| 24 | WEDNESDAY:1 |
| 24 | WEDNESDAY:2 |
| 24 | WEDNESDAY:3 |
| 24 | WEDNESDAY:4 |
| 24 | WEDNESDAY:5 |
| 24 | WEDNESDAY:6 |
| 24 | WEDNESDAY:7 |
| 25 | FRIDAY:1 |
| 25 | FRIDAY:2 |
| 25 | FRIDAY:3 |
| 25 | FRIDAY:4 |
| 25 | FRIDAY:5 |
| 25 | FRIDAY:6 |
| 25 | FRIDAY:7 |
| 25 | MONDAY:1 |
| 25 | MONDAY:2 |
| 25 | MONDAY:3 |
| 25 | MONDAY:4 |
| 25 | MONDAY:5 |
| 25 | MONDAY:6 |
| 25 | MONDAY:7 |
| 25 | SATURDAY:1 |
| 25 | SATURDAY:2 |
| 25 | SATURDAY:3 |
| 25 | SATURDAY:4 |
| 25 | SATURDAY:5 |
| 25 | SATURDAY:6 |
| 25 | SATURDAY:7 |
| 25 | THURSDAY:1 |
| 25 | THURSDAY:2 |
| 25 | THURSDAY:3 |
| 25 | THURSDAY:4 |
| 25 | THURSDAY:5 |
| 25 | THURSDAY:6 |
| 25 | THURSDAY:7 |
| 25 | TUESDAY:1 |
| 25 | TUESDAY:2 |
| 25 | TUESDAY:3 |
| 25 | TUESDAY:4 |
| 25 | TUESDAY:5 |
| 25 | TUESDAY:6 |
| 25 | TUESDAY:7 |
| 25 | WEDNESDAY:1 |
| 25 | WEDNESDAY:2 |
| 25 | WEDNESDAY:3 |
| 25 | WEDNESDAY:4 |
| 25 | WEDNESDAY:5 |
| 25 | WEDNESDAY:6 |
| 25 | WEDNESDAY:7 |
| 26 | FRIDAY:1 |
| 26 | FRIDAY:2 |
| 26 | FRIDAY:3 |
| 26 | FRIDAY:4 |
| 26 | FRIDAY:5 |
| 26 | FRIDAY:6 |
| 26 | FRIDAY:7 |
| 26 | MONDAY:1 |
| 26 | MONDAY:2 |
| 26 | MONDAY:3 |
| 26 | MONDAY:4 |
| 26 | MONDAY:5 |
| 26 | MONDAY:6 |
| 26 | MONDAY:7 |
| 26 | SATURDAY:1 |
| 26 | SATURDAY:2 |
| 26 | SATURDAY:3 |
| 26 | SATURDAY:4 |
| 26 | SATURDAY:5 |
| 26 | SATURDAY:6 |
| 26 | SATURDAY:7 |
| 26 | THURSDAY:1 |
| 26 | THURSDAY:2 |
| 26 | THURSDAY:3 |
| 26 | THURSDAY:4 |
| 26 | THURSDAY:5 |
| 26 | THURSDAY:6 |
| 26 | THURSDAY:7 |
| 26 | TUESDAY:1 |
| 26 | TUESDAY:2 |
| 26 | TUESDAY:3 |
| 26 | TUESDAY:4 |
| 26 | TUESDAY:5 |
| 26 | TUESDAY:6 |
| 26 | TUESDAY:7 |
| 26 | WEDNESDAY:1 |
| 26 | WEDNESDAY:2 |
| 26 | WEDNESDAY:3 |
| 26 | WEDNESDAY:4 |
| 26 | WEDNESDAY:5 |
| 26 | WEDNESDAY:6 |
| 26 | WEDNESDAY:7 |
| 27 | FRIDAY:1 |
| 27 | FRIDAY:2 |
| 27 | FRIDAY:3 |
| 27 | FRIDAY:4 |
| 27 | FRIDAY:5 |
| 27 | FRIDAY:6 |
| 27 | FRIDAY:7 |
| 27 | MONDAY:1 |
| 27 | MONDAY:2 |
| 27 | MONDAY:3 |
| 27 | MONDAY:4 |
| 27 | MONDAY:5 |
| 27 | MONDAY:6 |
| 27 | MONDAY:7 |
| 27 | SATURDAY:1 |
| 27 | SATURDAY:2 |
| 27 | SATURDAY:3 |
| 27 | SATURDAY:4 |
| 27 | SATURDAY:5 |
| 27 | SATURDAY:6 |
| 27 | SATURDAY:7 |
| 27 | THURSDAY:1 |
| 27 | THURSDAY:2 |
| 27 | THURSDAY:3 |
| 27 | THURSDAY:4 |
| 27 | THURSDAY:5 |
| 27 | THURSDAY:6 |
| 27 | THURSDAY:7 |
| 27 | TUESDAY:1 |
| 27 | TUESDAY:2 |
| 27 | TUESDAY:3 |
| 27 | TUESDAY:4 |
| 27 | TUESDAY:5 |
| 27 | TUESDAY:6 |
| 27 | TUESDAY:7 |
| 27 | WEDNESDAY:1 |
| 27 | WEDNESDAY:2 |
| 27 | WEDNESDAY:3 |
| 27 | WEDNESDAY:4 |
| 27 | WEDNESDAY:5 |
| 27 | WEDNESDAY:6 |
| 27 | WEDNESDAY:7 |
| 28 | FRIDAY:1 |
| 28 | FRIDAY:2 |
| 28 | FRIDAY:3 |
| 28 | FRIDAY:4 |
| 28 | FRIDAY:5 |
| 28 | FRIDAY:6 |
| 28 | FRIDAY:7 |
| 28 | MONDAY:1 |
| 28 | MONDAY:2 |
| 28 | MONDAY:3 |
| 28 | MONDAY:4 |
| 28 | MONDAY:5 |
| 28 | MONDAY:6 |
| 28 | MONDAY:7 |
| 28 | SATURDAY:1 |
| 28 | SATURDAY:2 |
| 28 | SATURDAY:3 |
| 28 | SATURDAY:4 |
| 28 | SATURDAY:5 |
| 28 | SATURDAY:6 |
| 28 | SATURDAY:7 |
| 28 | THURSDAY:1 |
| 28 | THURSDAY:2 |
| 28 | THURSDAY:3 |
| 28 | THURSDAY:4 |
| 28 | THURSDAY:5 |
| 28 | THURSDAY:6 |
| 28 | THURSDAY:7 |
| 28 | TUESDAY:1 |
| 28 | TUESDAY:2 |
| 28 | TUESDAY:3 |
| 28 | TUESDAY:4 |
| 28 | TUESDAY:5 |
| 28 | TUESDAY:6 |
| 28 | TUESDAY:7 |
| 28 | WEDNESDAY:1 |
| 28 | WEDNESDAY:2 |
| 28 | WEDNESDAY:3 |
| 28 | WEDNESDAY:4 |
| 28 | WEDNESDAY:5 |
| 28 | WEDNESDAY:6 |
| 28 | WEDNESDAY:7 |
| 29 | FRIDAY:1 |
| 29 | FRIDAY:2 |
| 29 | FRIDAY:3 |
| 29 | FRIDAY:4 |
| 29 | FRIDAY:5 |
| 29 | FRIDAY:6 |
| 29 | FRIDAY:7 |
| 29 | MONDAY:1 |
| 29 | MONDAY:2 |
| 29 | MONDAY:3 |
| 29 | MONDAY:4 |
| 29 | MONDAY:5 |
| 29 | MONDAY:6 |
| 29 | MONDAY:7 |
| 29 | SATURDAY:1 |
| 29 | SATURDAY:2 |
| 29 | SATURDAY:3 |
| 29 | SATURDAY:4 |
| 29 | SATURDAY:5 |
| 29 | SATURDAY:6 |
| 29 | SATURDAY:7 |
| 29 | THURSDAY:1 |
| 29 | THURSDAY:2 |
| 29 | THURSDAY:3 |
| 29 | THURSDAY:4 |
| 29 | THURSDAY:5 |
| 29 | THURSDAY:6 |
| 29 | THURSDAY:7 |
| 29 | TUESDAY:1 |
| 29 | TUESDAY:2 |
| 29 | TUESDAY:3 |
| 29 | TUESDAY:4 |
| 29 | TUESDAY:5 |
| 29 | TUESDAY:6 |
| 29 | TUESDAY:7 |
| 29 | WEDNESDAY:1 |
| 29 | WEDNESDAY:2 |
| 29 | WEDNESDAY:3 |
| 29 | WEDNESDAY:4 |
| 29 | WEDNESDAY:5 |
| 29 | WEDNESDAY:6 |
| 29 | WEDNESDAY:7 |

<a id="table-coach_work_schedules"></a>

### 18. `coach_work_schedules`

| coach_id | accepting_new |
| --- | --- |
| 12 | true |
| 13 | true |
| 14 | true |
| 15 | true |
| 16 | true |
| 17 | true |
| 18 | true |
| 19 | true |
| 20 | true |
| 21 | true |
| 22 | true |
| 23 | true |
| 24 | true |
| 25 | true |
| 26 | true |
| 27 | true |
| 28 | true |
| 29 | true |

<a id="table-subscription_pauses"></a>

### 19. `subscription_pauses`

Rỗng. Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.


<a id="table-normal_class_requests"></a>

### 20. `normal_class_requests`

Rỗng. Fixture SQL tạo lớp trực tiếp có nhãn DEMO; không giả idempotency request/hash của API.


<a id="table-system_logs"></a>

### 21. `system_logs`

| action | detail | log_id | user_id | center_id | target_id | timestamp | scope_source | target_entity | audit_category |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| APPROVE_NORMAL_REFUND | DEMO fixture: synthetic history, not observed provider operations | 2 | 32 | 1 | 21 | 2026-11-12T00:00:00 | TARGET_AT_EVENT | CLASS | BUSINESS |
| CANCEL_NORMAL_PRESTART | DEMO fixture: synthetic history, not observed provider operations | 1 | 32 | 1 | 15 | 2026-11-12T00:00:00 | TARGET_AT_EVENT | CLASS | BUSINESS |
| SUBMIT_REFUND_PROOF | DEMO fixture: synthetic history, not observed provider operations | 3 | 30 | 1 | 21 | 2026-11-12T00:00:00 | TARGET_AT_EVENT | CLASS | BUSINESS |
| VERIFY_NORMAL_REFUND | DEMO fixture: synthetic history, not observed provider operations | 4 | 33 | 1 | 21 | 2026-11-12T00:00:00 | TARGET_AT_EVENT | CLASS | BUSINESS |

<a id="table-ai_consultations"></a>

### 22. `ai_consultations`

Rỗng. Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.


<a id="table-chat_read_state"></a>

### 23. `chat_read_state`

| user_id | updated_at | conversation_id | last_read_message_id |
| --- | --- | --- | --- |
| 30 | 2026-11-12T00:00:00 | 1 | 2 |
| 9 | 2026-11-12T00:00:00 | 1 | 1 |

<a id="table-chat_messages"></a>

### 24. `chat_messages`

| id | content | metadata | created_at | sender_type | content_type | sender_user_id | conversation_id | client_message_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | DEMO tôi muốn kiểm tra khoản hoàn RF06 | NULL | 2026-11-12T00:00:00 | MEMBER | TEXT | 9 | 1 | DEMO-MSG-1 |
| 2 | DEMO trung tâm đã tiếp nhận để kiểm tra lại giao dịch | NULL | 2026-11-12T00:00:00 | STAFF | TEXT | 30 | 1 | DEMO-MSG-2 |

<a id="table-chat_conversations"></a>

### 25. `chat_conversations`

| id | status | subject | closed_at | created_at | updated_at | handler_type | member_user_id | last_message_at | assigned_staff_user_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | OPEN | DEMO refund inquiry | NULL | 2026-11-12T00:00:00 | 2026-11-12T00:00:00 | STAFF | 9 | 2026-11-12T00:00:00 | 30 |

<a id="table-notification_recipients"></a>

### 26. `notification_recipients`

| is_read | read_at | user_id | created_at | notification_id |
| --- | --- | --- | --- | --- |
| false | NULL | 2 | 2026-11-12T00:00:00 | 1 |
| false | NULL | 5 | 2026-11-12T00:00:00 | 2 |
| true | NULL | 9 | 2026-11-12T00:00:00 | 3 |

<a id="table-notifications"></a>

### 27. `notifications`

| type | title | content | is_read | user_id | sender_id | created_at | dedupe_key | related_id | related_type | notification_id | notification_type |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| System | DEMO workflow update | DEMO đã xác minh hoàn900000đ; tham chiếu DEMO-TRANSFER-RF06 | true | 9 | NULL | 2026-11-12T00:00:00 | DEMO:NT03 | 21 | CLASS | 3 | PAYMENT_RECONCILED |
| System | DEMO workflow update | DEMO đề nghị chuyển N05→N11; chờ lựa chọn Member, không tự đồng ý | false | 2 | NULL | 2026-11-12T00:00:00 | DEMO:NT01 | 14 | CLASS | 1 | PAYMENT_RECONCILED |
| System | DEMO workflow update | DEMO trung tâm hủy lớp; đã có nghĩa vụ hoàn, chờ thông tin nhận | false | 5 | NULL | 2026-11-12T00:00:00 | DEMO:NT02 | 15 | CLASS | 2 | PAYMENT_RECONCILED |

<a id="table-support_requests"></a>

### 28. `support_requests`

Rỗng. Không tạo lịch sử giả cho thao tác chưa có trong kịch bản nền.


<a id="table-vnpay_payment_attempts"></a>

### 29. `vnpay_payment_attempts`

Rỗng. Chỉ tạo khi phát hành link VNPay trong workflow local; không seed metadata provider.


<a id="table-invoices"></a>

### 30. `invoices`

| amount | class_id | cash_note | member_id | created_at | expires_at | invoice_id | package_id | invoice_code | payment_date | payment_method | payment_status | receptionist_id | subscription_id | resolution_status | entitlement_class_id | gateway_transaction_ref | reconciliation_required | cash_confirmation_reversed |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1000000.0 | 16 | NULL | 10 | 2026-11-05T08:00:00 | NULL | 22 | NULL | INV-2026-9000000000022 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 11 | 2026-11-05T08:00:00 | NULL | 23 | NULL | INV-2026-9000000000023 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 34 | 2026-11-05T08:00:00 | NULL | 24 | NULL | INV-2026-9000000000024 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 35 | 2026-11-05T08:00:00 | NULL | 25 | NULL | INV-2026-9000000000025 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 36 | 2026-11-05T08:00:00 | NULL | 26 | NULL | INV-2026-9000000000026 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 37 | 2026-11-05T08:00:00 | NULL | 27 | NULL | INV-2026-9000000000027 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 38 | 2026-11-05T08:00:00 | NULL | 28 | NULL | INV-2026-9000000000028 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 39 | 2026-11-05T08:00:00 | NULL | 29 | NULL | INV-2026-9000000000029 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 40 | 2026-11-05T08:00:00 | NULL | 30 | NULL | INV-2026-9000000000030 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 41 | 2026-11-05T08:00:00 | NULL | 31 | NULL | INV-2026-9000000000031 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 42 | 2026-11-05T08:00:00 | NULL | 32 | NULL | INV-2026-9000000000032 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 43 | 2026-11-05T08:00:00 | NULL | 33 | NULL | INV-2026-9000000000033 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 8 | 2026-11-05T08:00:00 | NULL | 20 | NULL | INV-2026-9000000000020 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 16 | NULL | 9 | 2026-11-05T08:00:00 | NULL | 21 | NULL | INV-2026-9000000000021 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 2 | 2026-10-27T08:00:00 | NULL | 34 | NULL | INV-2026-9000000000034 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 3 | 2026-10-27T08:00:00 | NULL | 35 | NULL | INV-2026-9000000000035 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 4 | 2026-10-27T08:00:00 | NULL | 36 | NULL | INV-2026-9000000000036 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 5 | 2026-10-27T08:00:00 | NULL | 37 | NULL | INV-2026-9000000000037 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 6 | 2026-10-27T08:00:00 | NULL | 38 | NULL | INV-2026-9000000000038 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1000000.0 | 17 | NULL | 7 | 2026-10-27T08:00:00 | NULL | 39 | NULL | INV-2026-9000000000039 | 2026-10-27T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1200000.0 | 15 | NULL | 5 | 2026-11-05T08:00:00 | NULL | 17 | NULL | INV-2026-9000000000017 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 1200000.0 | 15 | NULL | 6 | 2026-11-05T08:00:00 | NULL | 18 | NULL | INV-2026-9000000000018 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 1200000.0 | 15 | NULL | 7 | 2026-11-05T08:00:00 | NULL | 19 | NULL | INV-2026-9000000000019 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 10 | 2026-11-05T08:00:00 | NULL | 7 | NULL | INV-2026-9000000000007 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 11 | 2026-11-05T08:00:00 | NULL | 8 | NULL | INV-2026-9000000000008 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 34 | 2026-11-05T08:00:00 | NULL | 9 | NULL | INV-2026-9000000000009 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 35 | 2026-11-05T08:00:00 | NULL | 10 | NULL | INV-2026-9000000000010 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 8 | 2026-11-05T08:00:00 | NULL | 5 | NULL | INV-2026-9000000000005 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1500000.0 | 12 | NULL | 9 | 2026-11-05T08:00:00 | NULL | 6 | NULL | INV-2026-9000000000006 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1800000.0 | 14 | NULL | 2 | 2026-11-05T08:00:00 | NULL | 14 | NULL | INV-2026-9000000000014 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1800000.0 | 14 | NULL | 3 | 2026-11-05T08:00:00 | NULL | 15 | NULL | INV-2026-9000000000015 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 1800000.0 | 14 | NULL | 4 | 2026-11-05T08:00:00 | NULL | 16 | NULL | INV-2026-9000000000016 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1800000.0 | 20 | NULL | 34 | 2026-11-05T08:00:00 | NULL | 45 | NULL | INV-2026-9000000000045 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1800000.0 | 20 | NULL | 35 | 2026-11-05T08:00:00 | NULL | 46 | NULL | INV-2026-9000000000046 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 1800000.0 | 20 | NULL | 36 | 2026-11-05T08:00:00 | NULL | 47 | NULL | INV-2026-9000000000047 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2000000.0 | 13 | NULL | 2 | 2026-11-05T08:00:00 | NULL | 11 | NULL | INV-2026-9000000000011 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2000000.0 | 13 | NULL | 3 | 2026-11-05T08:00:00 | NULL | 12 | NULL | INV-2026-9000000000012 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2000000.0 | 13 | NULL | 4 | 2026-11-05T08:00:00 | NULL | 13 | NULL | INV-2026-9000000000013 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2500000.0 | 11 | NULL | 5 | 2026-11-05T08:00:00 | NULL | 2 | NULL | INV-2026-9000000000002 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2500000.0 | 11 | NULL | 6 | 2026-11-05T08:00:00 | NULL | 3 | NULL | INV-2026-9000000000003 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 2500000.0 | 11 | NULL | 7 | 2026-11-05T08:00:00 | NULL | 4 | NULL | INV-2026-9000000000004 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 3000000.0 | NULL | NULL | 5 | 2026-11-06T07:50:00 | NULL | 53 | NULL | INV-2026-9000000000053 | 2026-11-06T08:00:00 | Cash | Paid | 30 | 3 | NULL | NULL | NULL | false | false |
| 3000000.0 | NULL | NULL | 8 | 2026-11-10T07:50:00 | NULL | 56 | NULL | INV-2026-9000000000056 | 2026-11-10T08:00:00 | Cash | Paid | 30 | 6 | NULL | NULL | NULL | false | false |
| 4500000.0 | NULL | NULL | 9 | 2026-11-09T07:50:00 | NULL | 57 | NULL | INV-2026-9000000000057 | 2026-11-09T08:00:00 | VNPay | Paid | NULL | 7 | NULL | NULL | DEMO-LATE-RECEIPT | true | false |
| 8000000.0 | NULL | NULL | 3 | 2026-11-08T07:50:00 | NULL | 51 | NULL | INV-2026-9000000000051 | 2026-11-08T08:00:00 | Cash | Paid | 30 | 1 | NULL | NULL | NULL | false | false |
| 8000000.0 | NULL | NULL | 4 | 2026-11-07T07:50:00 | NULL | 52 | NULL | INV-2026-9000000000052 | 2026-11-07T08:00:00 | Cash | Paid | 30 | 2 | NULL | NULL | NULL | false | false |
| 8000000.0 | NULL | NULL | 6 | 2026-10-03T07:50:00 | NULL | 54 | NULL | INV-2026-9000000000054 | 2026-10-03T08:00:00 | Cash | Paid | 30 | 4 | NULL | NULL | NULL | false | false |
| 8000000.0 | NULL | NULL | 7 | 2026-09-03T07:50:00 | NULL | 55 | NULL | INV-2026-9000000000055 | 2026-09-03T08:00:00 | Cash | Paid | 30 | 5 | NULL | NULL | NULL | false | false |
| 800000.0 | 18 | NULL | 2 | 2026-09-16T08:00:00 | NULL | 40 | NULL | INV-2026-9000000000040 | 2026-09-16T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 800000.0 | 18 | NULL | 3 | 2026-09-16T08:00:00 | NULL | 41 | NULL | INV-2026-9000000000041 | 2026-09-16T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 900000.0 | 10 | NULL | 2 | 2026-11-05T08:00:00 | NULL | 1 | NULL | INV-2026-9000000000001 | 2026-11-05T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 900000.0 | 19 | NULL | 2 | 2026-10-17T08:00:00 | NULL | 42 | NULL | INV-2026-9000000000042 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 900000.0 | 19 | NULL | 3 | 2026-10-17T08:00:00 | NULL | 43 | NULL | INV-2026-9000000000043 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 900000.0 | 19 | NULL | 4 | 2026-10-17T08:00:00 | NULL | 44 | NULL | INV-2026-9000000000044 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | NULL | NULL | NULL | false | false |
| 900000.0 | 21 | NULL | 10 | 2026-10-17T08:00:00 | NULL | 50 | NULL | INV-2026-9000000000050 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 900000.0 | 21 | NULL | 8 | 2026-10-17T08:00:00 | NULL | 48 | NULL | INV-2026-9000000000048 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | RefundPending | NULL | NULL | false | false |
| 900000.0 | 21 | NULL | 9 | 2026-10-17T08:00:00 | NULL | 49 | NULL | INV-2026-9000000000049 | 2026-10-17T08:01:00 | Cash | Paid | 30 | NULL | Completed | NULL | NULL | false | false |

<a id="table-holidays"></a>

### 31. `holidays`

| tag | is_active | holiday_id | description | holiday_date | holiday_name | actual_closed_until |
| --- | --- | --- | --- | --- | --- | --- |
| CENTER_EVENT | true | 1 | NULL | 2027-01-01 | DEMO trung tâm đóng cửa | NULL |

<a id="table-class_enrollments"></a>

### 32. `class_enrollments`

| status | class_id | member_id | enrolled_at | promoted_at | cancelled_at | cancelled_by | cancel_reason | enrollment_id | eligibility_subscription_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cancelled | 14 | 3 | 2026-11-05T08:00:00 | NULL | 2026-11-09T00:00:00 | 3 | DEMO center refund obligation | 15 | NULL |
| Cancelled | 15 | 5 | 2026-11-05T08:00:00 | NULL | 2026-11-09T00:00:00 | 32 | DEMO center refund obligation | 17 | NULL |
| Cancelled | 15 | 6 | 2026-11-05T08:00:00 | NULL | 2026-11-09T00:00:00 | 32 | DEMO center refund obligation | 18 | NULL |
| Cancelled | 15 | 7 | 2026-11-05T08:00:00 | NULL | 2026-11-09T00:00:00 | 32 | DEMO center refund obligation | 19 | NULL |
| Cancelled | 21 | 10 | 2026-10-17T08:00:00 | NULL | 2026-10-22T16:30:00 | 32 | DEMO center refund obligation | 50 | NULL |
| Cancelled | 21 | 8 | 2026-10-17T08:00:00 | NULL | 2026-10-22T16:30:00 | 32 | DEMO center refund obligation | 48 | NULL |
| Cancelled | 21 | 9 | 2026-10-17T08:00:00 | NULL | 2026-10-22T16:30:00 | 32 | DEMO center refund obligation | 49 | NULL |
| Completed | 18 | 2 | 2026-09-16T08:00:00 | NULL | NULL | NULL | NULL | 40 | NULL |
| Completed | 18 | 3 | 2026-09-16T08:00:00 | NULL | NULL | NULL | NULL | 41 | NULL |
| Registered | 10 | 2 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 1 | NULL |
| Registered | 11 | 5 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 2 | NULL |
| Registered | 11 | 6 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 3 | NULL |
| Registered | 11 | 7 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 4 | NULL |
| Registered | 12 | 10 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 7 | NULL |
| Registered | 12 | 11 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 8 | NULL |
| Registered | 12 | 34 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 9 | NULL |
| Registered | 12 | 35 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 10 | NULL |
| Registered | 12 | 8 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 5 | NULL |
| Registered | 12 | 9 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 6 | NULL |
| Registered | 13 | 2 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 11 | NULL |
| Registered | 13 | 3 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 12 | NULL |
| Registered | 13 | 4 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 13 | NULL |
| Registered | 14 | 2 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 14 | NULL |
| Registered | 14 | 4 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 16 | NULL |
| Registered | 16 | 10 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 22 | NULL |
| Registered | 16 | 11 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 23 | NULL |
| Registered | 16 | 34 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 24 | NULL |
| Registered | 16 | 35 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 25 | NULL |
| Registered | 16 | 36 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 26 | NULL |
| Registered | 16 | 37 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 27 | NULL |
| Registered | 16 | 38 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 28 | NULL |
| Registered | 16 | 39 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 29 | NULL |
| Registered | 16 | 40 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 30 | NULL |
| Registered | 16 | 41 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 31 | NULL |
| Registered | 16 | 42 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 32 | NULL |
| Registered | 16 | 43 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 33 | NULL |
| Registered | 16 | 8 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 20 | NULL |
| Registered | 16 | 9 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 21 | NULL |
| Registered | 17 | 2 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 34 | NULL |
| Registered | 17 | 3 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 35 | NULL |
| Registered | 17 | 4 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 36 | NULL |
| Registered | 17 | 5 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 37 | NULL |
| Registered | 17 | 6 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 38 | NULL |
| Registered | 17 | 7 | 2026-10-27T08:00:00 | NULL | NULL | NULL | NULL | 39 | NULL |
| Registered | 19 | 2 | 2026-10-17T08:00:00 | NULL | NULL | NULL | NULL | 42 | NULL |
| Registered | 19 | 3 | 2026-10-17T08:00:00 | NULL | NULL | NULL | NULL | 43 | NULL |
| Registered | 19 | 4 | 2026-10-17T08:00:00 | NULL | NULL | NULL | NULL | 44 | NULL |
| Registered | 20 | 34 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 45 | NULL |
| Registered | 20 | 35 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 46 | NULL |
| Registered | 20 | 36 | 2026-11-05T08:00:00 | NULL | NULL | NULL | NULL | 47 | NULL |

<a id="table-member_packages"></a>

### 33. `member_packages`

| status | end_date | member_id | expires_at | package_id | start_date | subject_id | activated_at | validity_days | purchase_amount | subscription_id | primary_coach_id | remaining_credit | expiry_processed_at | approved_extra_nanos | expiry_revoked_credit | approved_extra_seconds | deferred_credit_return |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Active | 2026-12-21 | 5 | 2026-12-21T08:00:00 | NULL | 2026-11-06 | 5 | 2026-11-06T08:00:00 | 45 | 3000000.0 | 3 | 26 | 0 | NULL | 0 | 0 | 0 | 0 |
| Active | 2026-12-22 | 4 | 2026-12-22T08:00:00 | NULL | 2026-11-07 | 2 | 2026-11-07T08:00:00 | 45 | 8000000.0 | 2 | 12 | 10 | NULL | 0 | 0 | 0 | 0 |
| Active | 2026-12-23 | 3 | 2026-12-23T08:00:00 | NULL | 2026-11-08 | 2 | 2026-11-08T08:00:00 | 45 | 8000000.0 | 1 | 12 | 12 | NULL | 0 | 0 | 0 | 0 |
| Active | 2026-12-25 | 8 | 2026-12-25T08:00:00 | NULL | 2026-11-10 | 5 | 2026-11-10T08:00:00 | 45 | 3000000.0 | 6 | 26 | 12 | NULL | 0 | 0 | 0 | 0 |
| Cancelled | 2026-12-24 | 9 | NULL | NULL | 2026-11-09 | 3 | NULL | 45 | 4500000.0 | 7 | 14 | 0 | NULL | 0 | 0 | 0 | 0 |
| Completed | 2026-11-17 | 6 | 2026-11-17T08:00:00 | NULL | 2026-10-03 | 2 | 2026-10-03T08:00:00 | 45 | 8000000.0 | 4 | 12 | 0 | NULL | 0 | 0 | 0 | 0 |
| Expired | 2026-10-18 | 7 | 2026-10-18T08:00:00 | NULL | 2026-09-03 | 2 | 2026-09-03T08:00:00 | 45 | 8000000.0 | 5 | 12 | 0 | 2026-10-18T08:00:00 | 0 | 10 | 0 | 0 |

<a id="table-membership_packages"></a>

### 34. `membership_packages`

Rỗng. Mô hình legacy đã retire; không tái tạo catalog gói chung.


<a id="table-class_feedbacks"></a>

### 35. `class_feedbacks`

| comment | class_id | coach_id | member_id | created_at | feedback_id | class_rating | coach_rating |
| --- | --- | --- | --- | --- | --- | --- | --- |
| DEMO phản hồi khóa đã hoàn tất | 18 | 28 | 2 | 2026-11-11T08:00:00 | 1 | 4 | 4 |
| DEMO phản hồi khóa đã hoàn tất | 18 | 28 | 3 | 2026-11-11T08:00:00 | 2 | 4 | 4 |

<a id="table-evaluations"></a>

### 36. `evaluations`

| comment | coach_id | member_id | evaluation_id | progress_score | evaluation_date |
| --- | --- | --- | --- | --- | --- |
| DEMO tiến bộ sau khóa Bóng bàn | 28 | 2 | 1 | 8.0 | 2026-10-28 |
| DEMO tiến bộ sau khóa Bóng bàn | 28 | 3 | 2 | 8.0 | 2026-10-28 |

<a id="table-attendances"></a>

### 37. `attendances`

| note | state | member_id | session_id | recorded_by | attendance_id | check_in_time | superseded_at | check_out_time | absence_notice_id |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DEMO cần ôn lại kỹ thuật | Absent | 4 | 110 | 26 | 68 | NULL | NULL | NULL | NULL |
| DEMO cần ôn lại kỹ thuật | Absent | 4 | 112 | 26 | 74 | NULL | NULL | NULL | NULL |
| DEMO cần ôn lại kỹ thuật | Absent | 4 | 86 | 24 | 23 | NULL | NULL | NULL | NULL |
| DEMO cần ôn lại kỹ thuật | Absent | 4 | 88 | 24 | 35 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 100 | 28 | 45 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 101 | 28 | 47 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 102 | 28 | 49 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 103 | 28 | 51 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 104 | 28 | 53 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 105 | 28 | 55 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 106 | 28 | 57 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 107 | 28 | 59 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 108 | 28 | 61 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 109 | 26 | 63 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 110 | 26 | 66 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 111 | 26 | 69 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 112 | 26 | 72 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 113 | 26 | 75 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 114 | 26 | 78 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 115 | 26 | 81 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 116 | 26 | 84 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 85 | 24 | 15 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 86 | 24 | 21 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 87 | 24 | 27 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 88 | 24 | 33 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 97 | 28 | 39 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 98 | 28 | 41 | NULL | NULL | NULL | NULL |
| NULL | Present | 2 | 99 | 28 | 43 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 100 | 28 | 46 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 101 | 28 | 48 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 102 | 28 | 50 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 103 | 28 | 52 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 104 | 28 | 54 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 105 | 28 | 56 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 106 | 28 | 58 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 107 | 28 | 60 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 108 | 28 | 62 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 109 | 26 | 64 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 110 | 26 | 67 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 111 | 26 | 70 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 112 | 26 | 73 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 113 | 26 | 76 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 114 | 26 | 79 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 115 | 26 | 82 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 116 | 26 | 85 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 85 | 24 | 16 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 86 | 24 | 22 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 87 | 24 | 28 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 88 | 24 | 34 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 97 | 28 | 40 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 98 | 28 | 42 | NULL | NULL | NULL | NULL |
| NULL | Present | 3 | 99 | 28 | 44 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 109 | 26 | 65 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 111 | 26 | 71 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 113 | 26 | 77 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 114 | 26 | 80 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 115 | 26 | 83 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 116 | 26 | 86 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 85 | 24 | 17 | NULL | NULL | NULL | NULL |
| NULL | Present | 4 | 87 | 24 | 29 | NULL | NULL | NULL | NULL |
| NULL | Present | 5 | 85 | 24 | 18 | NULL | NULL | NULL | NULL |
| NULL | Present | 5 | 86 | 24 | 24 | NULL | NULL | NULL | NULL |
| NULL | Present | 5 | 87 | 24 | 30 | NULL | NULL | NULL | NULL |
| NULL | Present | 5 | 88 | 24 | 36 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 160 | 12 | 1 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 161 | 12 | 2 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 162 | 12 | 3 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 163 | 12 | 4 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 164 | 12 | 5 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 165 | 12 | 6 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 166 | 12 | 7 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 167 | 12 | 8 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 168 | 12 | 9 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 169 | 12 | 10 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 170 | 12 | 11 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 171 | 12 | 12 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 85 | 24 | 19 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 86 | 24 | 25 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 87 | 24 | 31 | NULL | NULL | NULL | NULL |
| NULL | Present | 6 | 88 | 24 | 37 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 172 | 12 | 13 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 173 | 12 | 14 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 85 | 24 | 20 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 86 | 24 | 26 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 87 | 24 | 32 | NULL | NULL | NULL | NULL |
| NULL | Present | 7 | 88 | 24 | 38 | NULL | NULL | NULL | NULL |

<a id="table-training_results"></a>

### 38. `training_results`

| content | coach_id | member_id | result_id | session_id | recorded_at | attendance_status |
| --- | --- | --- | --- | --- | --- | --- |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 10 | 169 | 2026-10-29T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 11 | 170 | 2026-10-31T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 12 | 171 | 2026-11-03T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 1 | 160 | 2026-10-08T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 2 | 161 | 2026-10-10T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 3 | 162 | 2026-10-13T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 4 | 163 | 2026-10-15T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 5 | 164 | 2026-10-17T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 6 | 165 | 2026-10-20T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 7 | 166 | 2026-10-22T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 8 | 167 | 2026-10-24T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 6 | 9 | 168 | 2026-10-27T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 7 | 13 | 172 | 2026-09-08T19:45:00 | Present |
| DEMO bài tập cá nhân đã thực hiện | 12 | 7 | 14 | 173 | 2026-09-10T19:45:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 2 | 15 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 2 | 21 | 86 | 2026-11-05T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 2 | 26 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 2 | 32 | 88 | 2026-11-10T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 3 | 16 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 3 | 22 | 86 | 2026-11-05T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 3 | 27 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 3 | 33 | 88 | 2026-11-10T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 4 | 17 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 4 | 28 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 5 | 18 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 5 | 23 | 86 | 2026-11-05T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 5 | 29 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 5 | 34 | 88 | 2026-11-10T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 6 | 19 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 6 | 24 | 86 | 2026-11-05T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 6 | 30 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 6 | 35 | 88 | 2026-11-10T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 7 | 20 | 85 | 2026-11-03T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 7 | 25 | 86 | 2026-11-05T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 7 | 31 | 87 | 2026-11-07T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 24 | 7 | 36 | 88 | 2026-11-10T14:30:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 61 | 109 | 2026-10-24T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 64 | 110 | 2026-10-27T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 66 | 111 | 2026-10-29T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 69 | 112 | 2026-10-31T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 71 | 113 | 2026-11-03T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 74 | 114 | 2026-11-05T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 2 | 77 | 115 | 2026-11-07T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 62 | 109 | 2026-10-24T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 65 | 110 | 2026-10-27T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 67 | 111 | 2026-10-29T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 70 | 112 | 2026-10-31T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 72 | 113 | 2026-11-03T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 75 | 114 | 2026-11-05T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 3 | 78 | 115 | 2026-11-07T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 4 | 63 | 109 | 2026-10-24T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 4 | 68 | 111 | 2026-10-29T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 4 | 73 | 113 | 2026-11-03T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 4 | 76 | 114 | 2026-11-05T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 26 | 4 | 79 | 115 | 2026-11-07T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 37 | 97 | 2026-09-23T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 39 | 98 | 2026-09-25T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 41 | 99 | 2026-09-28T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 43 | 100 | 2026-09-30T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 45 | 101 | 2026-10-02T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 47 | 102 | 2026-10-05T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 49 | 103 | 2026-10-07T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 51 | 104 | 2026-10-09T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 53 | 105 | 2026-10-12T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 55 | 106 | 2026-10-14T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 57 | 107 | 2026-10-16T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 2 | 59 | 108 | 2026-10-19T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 38 | 97 | 2026-09-23T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 40 | 98 | 2026-09-25T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 42 | 99 | 2026-09-28T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 44 | 100 | 2026-09-30T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 46 | 101 | 2026-10-02T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 48 | 102 | 2026-10-05T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 50 | 103 | 2026-10-07T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 52 | 104 | 2026-10-09T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 54 | 105 | 2026-10-12T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 56 | 106 | 2026-10-14T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 58 | 107 | 2026-10-16T16:15:00 | Present |
| DEMO thực hiện bài tập và được nhận xét tại buổi học | 28 | 3 | 60 | 108 | 2026-10-19T16:15:00 | Present |

<a id="table-sessions"></a>

### 39. `sessions`

| status | room_id | class_id | coach_id | end_time | created_by | session_id | start_time | booking_key | booking_hash | session_date | support_reason | credit_returned | subscription_id | booked_member_id | center_invalidated | attendance_finalized | makeup_for_session_id | agreement_confirmed_at | attendance_review_pending |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 134 | 16:30:00 | NULL | NULL | 2026-10-24 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 135 | 16:30:00 | NULL | NULL | 2026-10-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 136 | 16:30:00 | NULL | NULL | 2026-10-29 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 137 | 16:30:00 | NULL | NULL | 2026-10-31 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 138 | 16:30:00 | NULL | NULL | 2026-11-03 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 139 | 16:30:00 | NULL | NULL | 2026-11-05 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 140 | 16:30:00 | NULL | NULL | 2026-11-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 141 | 16:30:00 | NULL | NULL | 2026-11-10 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 142 | 16:30:00 | NULL | NULL | 2026-11-12 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 143 | 16:30:00 | NULL | NULL | 2026-11-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 144 | 16:30:00 | NULL | NULL | 2026-11-17 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 3 | 21 | 26 | 18:00:00 | 32 | 145 | 16:30:00 | NULL | NULL | 2026-11-19 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 61 | 13:00:00 | NULL | NULL | 2026-11-20 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 62 | 13:00:00 | NULL | NULL | 2026-11-23 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 63 | 13:00:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 64 | 13:00:00 | NULL | NULL | 2026-11-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 65 | 13:00:00 | NULL | NULL | 2026-11-30 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 66 | 13:00:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 67 | 13:00:00 | NULL | NULL | 2026-12-04 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 68 | 13:00:00 | NULL | NULL | 2026-12-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 69 | 13:00:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 70 | 13:00:00 | NULL | NULL | 2026-12-11 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 71 | 13:00:00 | NULL | NULL | 2026-12-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Cancelled | 7 | 15 | 20 | 14:30:00 | 32 | 72 | 13:00:00 | NULL | NULL | 2026-12-16 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 160 | 18:15:00 | NULL | NULL | 2026-10-08 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 161 | 18:15:00 | NULL | NULL | 2026-10-10 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 162 | 18:15:00 | NULL | NULL | 2026-10-13 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 163 | 18:15:00 | NULL | NULL | 2026-10-15 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 164 | 18:15:00 | NULL | NULL | 2026-10-17 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 165 | 18:15:00 | NULL | NULL | 2026-10-20 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 166 | 18:15:00 | NULL | NULL | 2026-10-22 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 167 | 18:15:00 | NULL | NULL | 2026-10-24 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 168 | 18:15:00 | NULL | NULL | 2026-10-27 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 169 | 18:15:00 | NULL | NULL | 2026-10-29 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 170 | 18:15:00 | NULL | NULL | 2026-10-31 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 171 | 18:15:00 | NULL | NULL | 2026-11-03 | NULL | false | 4 | 6 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 172 | 18:15:00 | NULL | NULL | 2026-09-08 | NULL | false | 5 | 7 | false | true | NULL | NULL | false |
| Completed | 15 | 1 | 12 | 19:45:00 | 32 | 173 | 18:15:00 | NULL | NULL | 2026-09-10 | NULL | false | 5 | 7 | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 100 | 14:45:00 | NULL | NULL | 2026-09-30 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 101 | 14:45:00 | NULL | NULL | 2026-10-02 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 102 | 14:45:00 | NULL | NULL | 2026-10-05 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 103 | 14:45:00 | NULL | NULL | 2026-10-07 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 104 | 14:45:00 | NULL | NULL | 2026-10-09 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 105 | 14:45:00 | NULL | NULL | 2026-10-12 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 106 | 14:45:00 | NULL | NULL | 2026-10-14 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 107 | 14:45:00 | NULL | NULL | 2026-10-16 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 108 | 14:45:00 | NULL | NULL | 2026-10-19 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 97 | 14:45:00 | NULL | NULL | 2026-09-23 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 98 | 14:45:00 | NULL | NULL | 2026-09-25 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 17 | 18 | 28 | 16:15:00 | 32 | 99 | 14:45:00 | NULL | NULL | 2026-09-28 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 109 | 14:45:00 | NULL | NULL | 2026-10-24 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 110 | 14:45:00 | NULL | NULL | 2026-10-27 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 111 | 14:45:00 | NULL | NULL | 2026-10-29 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 112 | 14:45:00 | NULL | NULL | 2026-10-31 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 113 | 14:45:00 | NULL | NULL | 2026-11-03 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 114 | 14:45:00 | NULL | NULL | 2026-11-05 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 115 | 14:45:00 | NULL | NULL | 2026-11-07 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 3 | 19 | 26 | 16:15:00 | 32 | 116 | 14:45:00 | NULL | NULL | 2026-11-10 | NULL | false | NULL | NULL | true | true | NULL | NULL | false |
| Completed | 5 | 17 | 24 | 14:30:00 | 32 | 85 | 13:00:00 | NULL | NULL | 2026-11-03 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 5 | 17 | 24 | 14:30:00 | 32 | 86 | 13:00:00 | NULL | NULL | 2026-11-05 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 5 | 17 | 24 | 14:30:00 | 32 | 87 | 13:00:00 | NULL | NULL | 2026-11-07 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Completed | 5 | 17 | 24 | 14:30:00 | 32 | 88 | 13:00:00 | NULL | NULL | 2026-11-10 | NULL | false | NULL | NULL | false | true | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 49 | 09:30:00 | NULL | NULL | 2026-11-19 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 50 | 09:30:00 | NULL | NULL | 2026-11-21 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 51 | 09:30:00 | NULL | NULL | 2026-11-24 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 52 | 09:30:00 | NULL | NULL | 2026-11-26 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 53 | 09:30:00 | NULL | NULL | 2026-11-28 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 54 | 09:30:00 | NULL | NULL | 2026-12-01 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 55 | 09:30:00 | NULL | NULL | 2026-12-03 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 56 | 09:30:00 | NULL | NULL | 2026-12-05 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 57 | 09:30:00 | NULL | NULL | 2026-12-08 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 58 | 09:30:00 | NULL | NULL | 2026-12-10 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 59 | 09:30:00 | NULL | NULL | 2026-12-12 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 11 | 14 | 16 | 11:00:00 | 32 | 60 | 09:30:00 | NULL | NULL | 2026-12-15 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 122 | 16:30:00 | NULL | NULL | 2026-11-20 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 123 | 16:30:00 | NULL | NULL | 2026-11-23 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 124 | 16:30:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 125 | 16:30:00 | NULL | NULL | 2026-11-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 126 | 16:30:00 | NULL | NULL | 2026-11-30 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 127 | 16:30:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 128 | 16:30:00 | NULL | NULL | 2026-12-04 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 129 | 16:30:00 | NULL | NULL | 2026-12-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 130 | 16:30:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 131 | 16:30:00 | NULL | NULL | 2026-12-11 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 132 | 16:30:00 | NULL | NULL | 2026-12-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 12 | 20 | 17 | 18:00:00 | 32 | 133 | 16:30:00 | NULL | NULL | 2026-12-16 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 25 | 06:00:00 | NULL | NULL | 2026-11-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 26 | 06:00:00 | NULL | NULL | 2026-11-18 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 27 | 06:00:00 | NULL | NULL | 2026-11-19 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 28 | 06:00:00 | NULL | NULL | 2026-11-21 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 29 | 06:00:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 30 | 06:00:00 | NULL | NULL | 2026-11-26 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 31 | 06:00:00 | NULL | NULL | 2026-11-28 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 32 | 06:00:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 33 | 06:00:00 | NULL | NULL | 2026-12-03 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 34 | 06:00:00 | NULL | NULL | 2026-12-05 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 35 | 06:00:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 13 | 12 | 14 | 07:30:00 | 32 | 36 | 06:00:00 | NULL | NULL | 2026-12-10 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 13 | 06:00:00 | NULL | NULL | 2026-11-13 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 14 | 06:00:00 | NULL | NULL | 2026-11-16 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 15 | 06:00:00 | NULL | NULL | 2026-11-18 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 16 | 06:00:00 | NULL | NULL | 2026-11-20 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 17 | 06:00:00 | NULL | NULL | 2026-11-23 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 18 | 06:00:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 19 | 06:00:00 | NULL | NULL | 2026-11-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 20 | 06:00:00 | NULL | NULL | 2026-11-30 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 21 | 06:00:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 22 | 06:00:00 | NULL | NULL | 2026-12-04 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 23 | 06:00:00 | NULL | NULL | 2026-12-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 11 | 12 | 07:30:00 | 32 | 24 | 06:00:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 15 | 1 | 12 | 19:45:00 | 32 | 146 | 18:15:00 | NULL | NULL | 2026-11-14 | NULL | false | 2 | 4 | false | false | NULL | NULL | false |
| Scheduled | 15 | 1 | 12 | 19:45:00 | 32 | 147 | 18:15:00 | NULL | NULL | 2026-11-19 | NULL | false | 2 | 4 | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 37 | 07:45:00 | NULL | NULL | 2026-11-20 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 38 | 07:45:00 | NULL | NULL | 2026-11-23 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 39 | 07:45:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 40 | 07:45:00 | NULL | NULL | 2026-11-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 41 | 07:45:00 | NULL | NULL | 2026-11-30 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 42 | 07:45:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 43 | 07:45:00 | NULL | NULL | 2026-12-04 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 44 | 07:45:00 | NULL | NULL | 2026-12-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 45 | 07:45:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 46 | 07:45:00 | NULL | NULL | 2026-12-11 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 47 | 07:45:00 | NULL | NULL | 2026-12-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 1 | 13 | 18 | 09:15:00 | 32 | 48 | 07:45:00 | NULL | NULL | 2026-12-16 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 10 | 09:30:00 | NULL | NULL | 2026-12-11 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 11 | 09:30:00 | NULL | NULL | 2026-12-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 12 | 09:30:00 | NULL | NULL | 2026-12-16 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 1 | 09:30:00 | NULL | NULL | 2026-11-20 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 2 | 09:30:00 | NULL | NULL | 2026-11-23 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 3 | 09:30:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 4 | 09:30:00 | NULL | NULL | 2026-11-27 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 5 | 09:30:00 | NULL | NULL | 2026-11-30 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 6 | 09:30:00 | NULL | NULL | 2026-12-02 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 7 | 09:30:00 | NULL | NULL | 2026-12-04 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 8 | 09:30:00 | NULL | NULL | 2026-12-07 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 10 | 26 | 11:00:00 | 32 | 9 | 09:30:00 | NULL | NULL | 2026-12-09 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 19 | 26 | 16:15:00 | 32 | 117 | 14:45:00 | NULL | NULL | 2026-11-12 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 19 | 26 | 16:15:00 | 32 | 118 | 14:45:00 | NULL | NULL | 2026-11-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 19 | 26 | 16:15:00 | 32 | 119 | 14:45:00 | NULL | NULL | 2026-11-17 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 19 | 26 | 16:15:00 | 32 | 120 | 14:45:00 | NULL | NULL | 2026-11-19 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 3 | 19 | 26 | 19:45:00 | 32 | 121 | 18:15:00 | NULL | NULL | 2026-11-25 | NULL | false | NULL | NULL | false | false | 116 | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 148 | 18:15:00 | NULL | NULL | 2026-11-14 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 149 | 18:15:00 | NULL | NULL | 2026-11-17 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 150 | 18:15:00 | NULL | NULL | 2026-11-19 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 151 | 18:15:00 | NULL | NULL | 2026-11-21 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 152 | 18:15:00 | NULL | NULL | 2026-11-24 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 153 | 18:15:00 | NULL | NULL | 2026-11-26 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 154 | 18:15:00 | NULL | NULL | 2026-11-28 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 155 | 18:15:00 | NULL | NULL | 2026-12-01 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 156 | 18:15:00 | NULL | NULL | 2026-12-03 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 157 | 18:15:00 | NULL | NULL | 2026-12-05 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 158 | 18:15:00 | NULL | NULL | 2026-12-08 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 3 | 8 | 26 | 19:45:00 | 32 | 159 | 18:15:00 | NULL | NULL | 2026-12-10 | NULL | false | 3 | 5 | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 89 | 13:00:00 | NULL | NULL | 2026-11-12 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 90 | 13:00:00 | NULL | NULL | 2026-11-14 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 91 | 13:00:00 | NULL | NULL | 2026-11-17 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 92 | 13:00:00 | NULL | NULL | 2026-11-19 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 93 | 13:00:00 | NULL | NULL | 2026-11-21 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 94 | 13:00:00 | NULL | NULL | 2026-11-24 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 95 | 13:00:00 | NULL | NULL | 2026-11-26 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 5 | 17 | 24 | 14:30:00 | 32 | 96 | 13:00:00 | NULL | NULL | 2026-11-28 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 73 | 07:45:00 | NULL | NULL | 2026-11-21 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 74 | 07:45:00 | NULL | NULL | 2026-11-24 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 75 | 07:45:00 | NULL | NULL | 2026-11-26 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 76 | 07:45:00 | NULL | NULL | 2026-11-28 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 77 | 07:45:00 | NULL | NULL | 2026-12-01 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 78 | 07:45:00 | NULL | NULL | 2026-12-03 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 79 | 07:45:00 | NULL | NULL | 2026-12-05 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 80 | 07:45:00 | NULL | NULL | 2026-12-08 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 81 | 07:45:00 | NULL | NULL | 2026-12-10 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 82 | 07:45:00 | NULL | NULL | 2026-12-12 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 83 | 07:45:00 | NULL | NULL | 2026-12-15 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |
| Scheduled | 9 | 16 | 22 | 09:15:00 | 32 | 84 | 07:45:00 | NULL | NULL | 2026-12-17 | NULL | false | NULL | NULL | false | false | NULL | NULL | false |

<a id="table-training_plans"></a>

### 40. `training_plans`

| goal | title | content | plan_id | class_id | coach_id | end_date | member_id | created_at | start_date |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| NULL | DEMO giáo án cá nhân Advanced | Thực hành kỹ thuật riêng cho Member M03 | 1 | NULL | 12 | NULL | 4 | 2026-11-02T08:00:00 | NULL |
| NULL | DEMO giáo án lớp N08 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng | 2 | 17 | 24 | NULL | NULL | 2026-11-02T08:00:00 | NULL |
| NULL | DEMO giáo án lớp N09 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng | 3 | 18 | 28 | NULL | NULL | 2026-11-02T08:00:00 | NULL |
| NULL | DEMO giáo án lớp N10 | Khởi động, kỹ thuật cơ bản, thực hành và thả lỏng | 4 | 19 | 26 | NULL | NULL | 2026-11-02T08:00:00 | NULL |

<a id="table-classes"></a>

### 41. `classes`

| status | class_id | coach_id | end_date | center_id | class_name | class_type | start_date | subject_id | tuition_fee | max_capacity | min_capacity | pricing_draft | change_pending | opening_reason | total_sessions | lifecycle_version | postponement_count | opening_confirmed_at | opening_confirmed_by |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cancelled | 15 | 20 | 2026-12-16 | 1 | Bóng rổ Normal N06 | Normal | 2026-11-20 | 7 | 1200000.0 | 8 | 6 | false | false | NULL | 12 | 1 | 0 | NULL | NULL |
| Cancelled | 21 | 26 | 2026-11-19 | 1 | Cầu lông Normal N12 | Normal | 2026-10-24 | 5 | 900000.0 | 4 | 3 | false | false | NULL | 12 | 1 | 0 | NULL | NULL |
| Closed | 18 | 28 | 2026-10-19 | 1 | Bóng bàn Normal N09 | Normal | 2026-09-23 | 6 | 800000.0 | 2 | 2 | false | false | DEMO confirmed opening | 12 | 1 | 0 | 2026-09-21T14:45:00 | 32 |
| Ongoing | 17 | 24 | 2026-11-28 | 1 | Bóng chuyền Normal N08 | Normal | 2026-11-03 | 8 | 1000000.0 | 8 | 6 | false | false | DEMO confirmed opening | 12 | 1 | 0 | 2026-11-01T13:00:00 | 32 |
| Ongoing | 19 | 26 | 2026-11-25 | 1 | Cầu lông Normal N10 | Normal | 2026-10-24 | 5 | 900000.0 | 4 | 3 | false | false | DEMO confirmed opening | 12 | 1 | 0 | 2026-10-22T14:45:00 | 32 |
| Open | 10 | 26 | 2026-12-16 | 1 | Cầu lông Normal N01 | Normal | 2026-11-20 | 5 | 900000.0 | 4 | 3 | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 11 | 12 | 2026-12-09 | 1 | Tennis Normal N02 | Normal | 2026-11-13 | 2 | 2500000.0 | 4 | 3 | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 12 | 14 | 2026-12-10 | 1 | Bơi lội Normal N03 | Normal | 2026-11-14 | 3 | 1500000.0 | 10 | 7 | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 13 | 18 | 2026-12-16 | 1 | Bắn cung Normal N04 | Normal | 2026-11-20 | 4 | 2000000.0 | 5 | 4 | false | false | NULL | 12 | 1 | 1 | NULL | NULL |
| Open | 14 | 16 | 2026-12-15 | 1 | Pickleball Normal N05 | Normal | 2026-11-19 | 9 | 1800000.0 | 4 | 3 | false | true | NULL | 12 | 1 | 0 | NULL | NULL |
| Open | 16 | 22 | 2026-12-17 | 1 | Bóng đá Normal N07 | Normal | 2026-11-21 | 1 | 1000000.0 | 15 | 10 | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 1 | NULL | NULL | 1 | Tennis - Advanced (1:1) | Advanced | NULL | 2 | 8000000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 20 | 17 | 2026-12-16 | 1 | Pickleball Normal N11 | Normal | 2026-11-20 | 9 | 1800000.0 | 4 | 3 | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 2 | NULL | NULL | 1 | Bơi lội - Advanced (1:1) | Advanced | NULL | 3 | 4500000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 3 | NULL | NULL | 1 | Pickleball - Advanced (1:1) | Advanced | NULL | 9 | 5000000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 4 | NULL | NULL | 1 | Bắn cung - Advanced (1:1) | Advanced | NULL | 4 | 6000000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 5 | NULL | NULL | 1 | Bóng rổ - Advanced (1:1) | Advanced | NULL | 7 | 3500000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 6 | NULL | NULL | 1 | Bóng đá - Advanced (1:1) | Advanced | NULL | 1 | 3500000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 7 | NULL | NULL | 1 | Bóng chuyền - Advanced (1:1) | Advanced | NULL | 8 | 3500000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 8 | NULL | NULL | 1 | Cầu lông - Advanced (1:1) | Advanced | NULL | 5 | 3000000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |
| Open | 9 | NULL | NULL | 1 | Bóng bàn - Advanced (1:1) | Advanced | NULL | 6 | 2500000.0 | 1 | NULL | false | false | NULL | 12 | 0 | 0 | NULL | NULL |

<a id="table-rooms"></a>

### 42. `rooms`

| status | room_id | capacity | location | center_id | room_code | room_name | activity_type | facility_type |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Available | 10 | 15 | Khu B – Ngoài trời | 1 | C01-FOOT-02 | Sân Bóng đá 02 | FOOTBALL | Outdoor |
| Available | 11 | 4 | Nhà thi đấu B | 1 | C01-PICK-01 | Sân Pickleball 01 | PICKLEBALL | Indoor |
| Available | 12 | 4 | Nhà thi đấu B | 1 | C01-PICK-02 | Sân Pickleball 02 | PICKLEBALL | Indoor |
| Available | 13 | 10 | Khu hồ bơi | 1 | C01-SWIM-01 | Hồ bơi 01 | SWIMMING | Outdoor |
| Available | 14 | 10 | Khu hồ bơi | 1 | C01-SWIM-02 | Hồ bơi 02 | SWIMMING | Outdoor |
| Available | 15 | 4 | Khu A – Ngoài trời | 1 | C01-TEN-01 | Sân Tennis 01 | TENNIS | Outdoor |
| Available | 16 | 4 | Khu A – Ngoài trời | 1 | C01-TEN-02 | Sân Tennis 02 | TENNIS | Outdoor |
| Available | 17 | 2 | Phòng Bóng bàn | 1 | C01-TT-01 | Bàn Bóng bàn 01 | TABLE_TENNIS | Indoor |
| Available | 18 | 2 | Phòng Bóng bàn | 1 | C01-TT-02 | Bàn Bóng bàn 02 | TABLE_TENNIS | Indoor |
| Available | 19 | 2 | Phòng Bóng bàn | 1 | C01-TT-03 | Bàn Bóng bàn 03 | TABLE_TENNIS | Indoor |
| Available | 1 | 5 | Khu C – Ngoài trời | 1 | C01-ARC-01 | Khu Bắn cung 01 | ARCHERY | Outdoor |
| Available | 20 | 2 | Phòng Bóng bàn | 1 | C01-TT-04 | Bàn Bóng bàn 04 | TABLE_TENNIS | Indoor |
| Available | 2 | 5 | Khu C – Ngoài trời | 1 | C01-ARC-02 | Khu Bắn cung 02 | ARCHERY | Outdoor |
| Available | 3 | 4 | Nhà thi đấu B | 1 | C01-BAD-01 | Sân Cầu lông 01 | BADMINTON | Indoor |
| Available | 4 | 4 | Nhà thi đấu B | 1 | C01-BAD-02 | Sân Cầu lông 02 | BADMINTON | Indoor |
| Available | 5 | 8 | Nhà thi đấu A | 1 | C01-BC-01 | Sân Bóng chuyền 01 | VOLLEYBALL | Indoor |
| Available | 6 | 8 | Nhà thi đấu A | 1 | C01-BC-02 | Sân Bóng chuyền 02 | VOLLEYBALL | Indoor |
| Available | 7 | 8 | Nhà thi đấu A | 1 | C01-BR-01 | Sân Bóng rổ 01 | BASKETBALL | Indoor |
| Available | 8 | 8 | Nhà thi đấu A | 1 | C01-BR-02 | Sân Bóng rổ 02 | BASKETBALL | Indoor |
| Available | 9 | 15 | Khu B – Ngoài trời | 1 | C01-FOOT-01 | Sân Bóng đá 01 | FOOTBALL | Outdoor |

<a id="table-coach_specializations"></a>

### 43. `coach_specializations`

| coach_id | specialization_id |
| --- | --- |
| 12 | 1 |
| 13 | 1 |
| 14 | 2 |
| 15 | 2 |
| 16 | 3 |
| 17 | 3 |
| 18 | 4 |
| 19 | 4 |
| 20 | 5 |
| 21 | 5 |
| 22 | 6 |
| 23 | 6 |
| 24 | 7 |
| 25 | 7 |
| 26 | 8 |
| 27 | 8 |
| 28 | 9 |
| 29 | 9 |
| 47 | 8 |

<a id="table-specializations"></a>

### 44. `specializations`

| name | status | subject_id | description | specialization_id |
| --- | --- | --- | --- | --- |
| Bắn cung | Active | 4 | NULL | 4 |
| Bơi lội | Active | 3 | NULL | 2 |
| Bóng bàn | Active | 6 | NULL | 9 |
| Bóng chuyền | Active | 8 | NULL | 7 |
| Bóng đá | Active | 1 | NULL | 6 |
| Bóng rổ | Active | 7 | NULL | 5 |
| Cầu lông | Active | 5 | NULL | 8 |
| Pickleball | Active | 9 | NULL | 3 |
| Tennis | Active | 2 | NULL | 1 |

<a id="table-subjects"></a>

### 45. `subjects`

| slug | status | outdoor | subject_id | description | normal_price | subject_name | advanced_price |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ban-cung | Active | true | 4 | Môn ngoài trời, rèn luyện sự tập trung cao độ và tính kiên nhẫn | 2000000.0 | Bắn cung | 6000000.0 |
| boi-loi | Active | true | 3 | Môn thể thao dưới nước ngoài trời, phát triển toàn diện cơ thể | 1500000.0 | Bơi lội | 4500000.0 |
| bong-ban | Active | false | 6 | Phản xạ nhanh, khéo léo với nhịp độ cao trong nhà | 800000.0 | Bóng bàn | 2500000.0 |
| bong-chuyen | Active | false | 8 | Phối hợp đồng đội và kỹ thuật phản xạ trên lưới trong nhà | 1000000.0 | Bóng chuyền | 3500000.0 |
| bong-da | Active | true | 1 | Môn thể thao vua ngoài trời, rèn luyện thể lực và phối hợp | 1000000.0 | Bóng đá | 3500000.0 |
| bong-ro | Active | false | 7 | Môn thể thao đồng đội tốc độ và sức bật trong nhà | 1200000.0 | Bóng rổ | 3500000.0 |
| cau-long | Active | false | 5 | Môn thể thao nhanh nhẹn, linh hoạt trong nhà | 900000.0 | Cầu lông | 3000000.0 |
| pickleball | Active | false | 9 | Môn thể thao thời thượng trong nhà, kết hợp tennis, bóng bàn | 1800000.0 | Pickleball | 5000000.0 |
| quan-vot-tennis | Active | true | 2 | Môn thể thao ngoài trời, rèn luyện phản xạ và thể lực | 2500000.0 | Quần vợt (Tennis) | 8000000.0 |

<a id="table-members"></a>

### 46. `members`

| goal | user_id | join_date | health_note | registered_by | booking_blocked_until |
| --- | --- | --- | --- | --- | --- |
| NULL | 10 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 11 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 2 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 34 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 35 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 36 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 37 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 38 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 39 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 3 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 40 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 41 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 42 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 43 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 44 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 45 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 46 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 4 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 5 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 6 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 7 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 8 | 2026-11-12 | NULL | NULL | NULL |
| NULL | 9 | 2026-11-12 | NULL | NULL | NULL |

<a id="table-coaches"></a>

### 47. `coaches`

| bio | user_id | certification | specialization |
| --- | --- | --- | --- |
| Huấn luyện viên demo môn Bắn cung. | 18 | NULL | Bắn cung |
| Huấn luyện viên demo môn Bắn cung. | 19 | NULL | Bắn cung |
| Huấn luyện viên demo môn Bơi lội. | 14 | NULL | Bơi lội |
| Huấn luyện viên demo môn Bơi lội. | 15 | NULL | Bơi lội |
| Huấn luyện viên demo môn Bóng bàn. | 28 | NULL | Bóng bàn |
| Huấn luyện viên demo môn Bóng bàn. | 29 | NULL | Bóng bàn |
| Huấn luyện viên demo môn Bóng chuyền. | 24 | NULL | Bóng chuyền |
| Huấn luyện viên demo môn Bóng chuyền. | 25 | NULL | Bóng chuyền |
| Huấn luyện viên demo môn Bóng đá. | 22 | NULL | Bóng đá |
| Huấn luyện viên demo môn Bóng đá. | 23 | NULL | Bóng đá |
| Huấn luyện viên demo môn Bóng rổ. | 20 | NULL | Bóng rổ |
| Huấn luyện viên demo môn Bóng rổ. | 21 | NULL | Bóng rổ |
| Huấn luyện viên demo môn Cầu lông. | 26 | NULL | Cầu lông |
| Huấn luyện viên demo môn Cầu lông. | 27 | NULL | Cầu lông |
| Huấn luyện viên demo môn Pickleball. | 16 | NULL | Pickleball |
| Huấn luyện viên demo môn Pickleball. | 17 | NULL | Pickleball |
| Huấn luyện viên demo môn Tennis. | 12 | NULL | Tennis |
| Huấn luyện viên demo môn Tennis. | 13 | NULL | Tennis |
| NULL | 47 | NULL | NULL |

<a id="table-receptionists"></a>

### 48. `receptionists`

| user_id |
| --- |
| 30 |
| 31 |

<a id="table-center_managers"></a>

### 49. `center_managers`

| user_id |
| --- |
| 32 |
| 33 |

<a id="table-email_otps"></a>

### 50. `email_otps`

Rỗng. OTP chỉ tạo tại thời điểm đăng ký/reset demo, không seed secret.


<a id="table-users"></a>

### 51. `users`

| email | phone | gender | status | address | role_id | user_id | center_id | full_name | avatar_url | created_at | locked_until | date_of_birth | is_first_login | avatar_public_id | failed_login_attempts |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| admin@gmail.com | 0905000101 | Male | Active | Văn phòng Trung tâm, Q1, TP.HCM | 5 | 1 | NULL | Admin Account | NULL | 2026-11-12T18:20:02.51243 | NULL | 1985-08-12 | false | NULL | 0 |
| an@gmail.com | 0901000101 | Male | Active | Quận 1, TP.HCM | 3 | 2 | 1 | Nguyễn Minh An | NULL | 2026-11-12T18:20:02.51243 | NULL | 2003-05-12 | false | NULL | 0 |
| bao@gmail.com | 0901000107 | Male | Active | Quận 10, TP.HCM | 3 | 8 | 1 | Bùi Quốc Bảo | NULL | 2026-11-12T18:20:02.51243 | NULL | 2002-12-03 | false | NULL | 0 |
| coach.archery1@gmail.com | 0902000107 | Male | Active | Quận 10, TP.HCM | 2 | 18 | 1 | Lê Nguyễn Thiên Phúc | NULL | 2026-11-12T18:20:02.51243 | NULL | 1991-12-04 | false | NULL | 0 |
| coach.archery2@gmail.com | 0902000108 | Female | Active | Phú Nhuận, TP.HCM | 2 | 19 | 1 | Hồ Thảo My | NULL | 2026-11-12T18:20:02.51243 | NULL | 1996-06-19 | false | NULL | 0 |
| coach.badminton1@gmail.com | 0902000115 | Male | Active | Quận 4, TP.HCM | 2 | 26 | 1 | Võ Minh Đức | NULL | 2026-11-12T18:20:02.51243 | NULL | 1990-09-18 | false | NULL | 0 |
| coach.badminton2@gmail.com | 0902000116 | Female | Active | Tân Phú, TP.HCM | 2 | 27 | 1 | Trần Thị Hương | NULL | 2026-11-12T18:20:02.51243 | NULL | 1996-01-11 | false | NULL | 0 |
| coach.basketball1@gmail.com | 0902000109 | Male | Active | Quận 11, TP.HCM | 2 | 20 | 1 | Trần Anh Dũng | NULL | 2026-11-12T18:20:02.51243 | NULL | 1988-01-30 | false | NULL | 0 |
| coach.basketball2@gmail.com | 0902000110 | Female | Active | Gò Vấp, TP.HCM | 2 | 21 | 1 | Phan Kim Ngân | NULL | 2026-11-12T18:20:02.51243 | NULL | 1992-08-14 | false | NULL | 0 |
| coach.football1@gmail.com | 0902000111 | Male | Active | Quận 12, TP.HCM | 2 | 22 | 1 | Lê Trần Phi | NULL | 2026-11-12T18:20:02.51243 | NULL | 1987-04-25 | false | NULL | 0 |
| coach.football2@gmail.com | 0902000112 | Female | Active | Quận 6, TP.HCM | 2 | 23 | 1 | Trần Ngọc Yến | NULL | 2026-11-12T18:20:02.51243 | NULL | 1993-10-09 | false | NULL | 0 |
| coach.inactive@scms.demo.invalid | NULL | NULL | Inactive | NULL | 2 | 47 | 1 | Demo Inactive Coach | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| coach.pickleball1@gmail.com | 0902000105 | Male | Active | TP. Thủ Đức, TP.HCM | 2 | 16 | 1 | Võ Thanh Long | NULL | 2026-11-12T18:20:02.51243 | NULL | 1994-09-26 | false | NULL | 0 |
| coach.pickleball2@gmail.com | 0902000106 | Female | Active | Tân Bình, TP.HCM | 2 | 17 | 1 | Phạm Thị Lan | NULL | 2026-11-12T18:20:02.51243 | NULL | 1995-02-12 | false | NULL | 0 |
| coach.swimming1@gmail.com | 0902000103 | Male | Active | Quận 7, TP.HCM | 2 | 14 | 1 | Lê Minh Duy | NULL | 2026-11-12T18:20:02.51243 | NULL | 1989-11-08 | false | NULL | 0 |
| coach.swimming2@gmail.com | 0902000104 | Female | Active | Bình Thạnh, TP.HCM | 2 | 15 | 1 | Lê Phương Anh | NULL | 2026-11-12T18:20:02.51243 | NULL | 1993-05-17 | false | NULL | 0 |
| coach.tabletennis1@gmail.com | 0902000117 | Male | Active | Bình Tân, TP.HCM | 2 | 28 | 1 | Lê Minh Khoa | NULL | 2026-11-12T18:20:02.51243 | NULL | 1988-05-23 | false | NULL | 0 |
| coach.tabletennis2@gmail.com | 0902000118 | Female | Active | Quận 3, TP.HCM | 2 | 29 | 1 | Hồ Bích Oanh | NULL | 2026-11-12T18:20:02.51243 | NULL | 1994-12-16 | false | NULL | 0 |
| coach.tennis1@gmail.com | 0902000101 | Male | Active | Quận 1, TP.HCM | 2 | 12 | 1 | Nguyễn Hoàng Quân | NULL | 2026-11-12T18:20:02.51243 | NULL | 1990-03-15 | false | NULL | 0 |
| coach.tennis2@gmail.com | 0902000102 | Female | Active | Quận 3, TP.HCM | 2 | 13 | 1 | Huỳnh Diễm My | NULL | 2026-11-12T18:20:02.51243 | NULL | 1992-07-22 | false | NULL | 0 |
| coach.volleyball1@gmail.com | 0902000113 | Male | Active | Quận 5, TP.HCM | 2 | 24 | 1 | Nguyễn Văn Hoàng | NULL | 2026-11-12T18:20:02.51243 | NULL | 1989-02-21 | false | NULL | 0 |
| coach.volleyball2@gmail.com | 0902000114 | Female | Active | Quận 8, TP.HCM | 2 | 25 | 1 | Phạm Thanh Thư | NULL | 2026-11-12T18:20:02.51243 | NULL | 1995-07-07 | false | NULL | 0 |
| ha@gmail.com | 0901000110 | Female | Active | Gò Vấp, TP.HCM | 3 | 11 | 1 | Phan Thu Hà | NULL | 2026-11-12T18:20:02.51243 | NULL | 2003-03-30 | false | NULL | 0 |
| huy@gmail.com | 0901000103 | Male | Active | Quận 7, TP.HCM | 3 | 4 | 1 | Lê Gia Huy | NULL | 2026-11-12T18:20:02.51243 | NULL | 2004-02-15 | false | NULL | 0 |
| khang@gmail.com | 0901000105 | Male | Active | TP. Thủ Đức, TP.HCM | 3 | 6 | 1 | Võ Đức Khang | NULL | 2026-11-12T18:20:02.51243 | NULL | 2003-07-24 | false | NULL | 0 |
| kiet@gmail.com | 0901000109 | Male | Active | Quận 11, TP.HCM | 3 | 10 | 1 | Đỗ Tuấn Kiệt | NULL | 2026-11-12T18:20:02.51243 | NULL | 2001-09-14 | false | NULL | 0 |
| linh@gmail.com | 0901000102 | Female | Active | Quận 3, TP.HCM | 3 | 3 | 1 | Trần Bảo Linh | NULL | 2026-11-12T18:20:02.51243 | NULL | 2002-08-21 | false | NULL | 0 |
| mai@gmail.com | 0901000104 | Female | Active | Bình Thạnh, TP.HCM | 3 | 5 | 1 | Phạm Ngọc Mai | NULL | 2026-11-12T18:20:02.51243 | NULL | 2001-11-09 | false | NULL | 0 |
| manager1@gmail.com | 0904000101 | Male | Active | Quận 7, TP.HCM | 1 | 32 | 1 | Nguyễn Thành Đạt | NULL | 2026-11-12T18:20:02.51243 | NULL | 1988-04-18 | false | NULL | 0 |
| manager2@gmail.com | 0904000102 | Female | Active | Quận 3, TP.HCM | 1 | 33 | 1 | Trần Ngọc Diệp | NULL | 2026-11-12T18:20:02.51243 | NULL | 1990-09-25 | false | NULL | 0 |
| member11@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 34 | 1 | Member Demo11 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member12@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 35 | 1 | Member Demo12 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member13@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 36 | 1 | Member Demo13 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member14@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 37 | 1 | Member Demo14 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member15@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 38 | 1 | Member Demo15 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member16@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 39 | 1 | Member Demo16 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member17@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 40 | 1 | Member Demo17 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member18@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 41 | 1 | Member Demo18 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member19@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 42 | 1 | Member Demo19 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member20@scms.demo.invalid | NULL | NULL | Active | NULL | 3 | 43 | 1 | Member Demo20 | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member.inactive@scms.demo.invalid | NULL | NULL | Inactive | NULL | 3 | 44 | 1 | Demo Inactive Member | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member.locked@scms.demo.invalid | NULL | NULL | Locked | NULL | 3 | 45 | 1 | Demo Locked Member | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| member.pending@scms.demo.invalid | NULL | NULL | Pending | NULL | 3 | 46 | 1 | Demo Pending Member | NULL | 2026-11-12T18:20:02.51243 | NULL | NULL | false | NULL | 0 |
| nhi@gmail.com | 0901000108 | Female | Active | Phú Nhuận, TP.HCM | 3 | 9 | 1 | Hồ Khánh Nhi | NULL | 2026-11-12T18:20:02.51243 | NULL | 2005-06-27 | false | NULL | 0 |
| receptionist1@gmail.com | 0903000101 | Female | Active | Bình Thạnh, TP.HCM | 4 | 30 | 1 | Nguyễn Thị Ngọc Anh | NULL | 2026-11-12T18:20:02.51243 | NULL | 1998-06-14 | false | NULL | 0 |
| receptionist2@gmail.com | 0903000102 | Male | Active | TP. Thủ Đức, TP.HCM | 4 | 31 | 1 | Trần Minh Khôi | NULL | 2026-11-12T18:20:02.51243 | NULL | 1997-09-22 | false | NULL | 0 |
| vy@gmail.com | 0901000106 | Female | Active | Tân Bình, TP.HCM | 3 | 7 | 1 | Đặng Thảo Vy | NULL | 2026-11-12T18:20:02.51243 | NULL | 2004-04-18 | false | NULL | 0 |

<a id="table-centers"></a>

### 52. `centers`

| center_id | center_name |
| --- | --- |
| 1 | Sports Center 1 |

<a id="table-role_permissions"></a>

### 53. `role_permissions`

| role_id | permission_id |
| --- | --- |
| 1 | 1 |
| 1 | 14 |
| 1 | 16 |
| 1 | 2 |
| 1 | 3 |
| 1 | 4 |
| 1 | 7 |
| 1 | 8 |
| 1 | 9 |
| 2 | 5 |
| 2 | 6 |
| 3 | 15 |
| 4 | 11 |
| 4 | 12 |
| 4 | 14 |
| 4 | 16 |
| 4 | 6 |
| 4 | 7 |
| 4 | 8 |
| 5 | 1 |
| 5 | 10 |
| 5 | 13 |
| 5 | 17 |
| 5 | 18 |
| 5 | 4 |
| 5 | 9 |

<a id="table-permissions"></a>

### 54. `permissions`

| description | permission_id | permission_name |
| --- | --- | --- |
| Chi quan tri vien duoc cau hinh ma tran phan quyen | 13 | MANAGE_PERMISSIONS |
| Dang ky thanh vien moi tai quay | 11 | REGISTER_MEMBER |
| Dang ky va gia han goi tap | 12 | MANAGE_SUBSCRIPTIONS |
| Ghi nhan ket qua buoi tap | 6 | RECORD_RESULT |
| Ghi nhan thanh toan, xuat hoa don | 7 | PROCESS_PAYMENT |
| Khoa hoac mo mon hoc | 17 | LOCK_SUBJECTS |
| Member may access their own support chat | 15 | CHAT_AS_MEMBER |
| Phan quyen va quan tri vai tro he thong | 10 | MANAGE_RBAC |
| Quan ly chuyen mon huan luyen vien | 18 | MANAGE_SPECIALIZATIONS |
| Quan ly goi thanh vien | 3 | MANAGE_PACKAGES |
| Quan ly lop hoc | 2 | MANAGE_CLASSES |
| Quan ly nguoi dung | 1 | MANAGE_USERS |
| Quan ly va xem hoa don cua cac thanh vien | 14 | MANAGE_INVOICES |
| Receptionist and CenterManager may handle support chats | 16 | HANDLE_CHAT |
| Tao/sua ke hoach tap luyen | 5 | MANAGE_TRAINING_PLAN |
| Xem bao cao | 4 | VIEW_REPORTS |
| Xem lich su thao tac he thong | 9 | VIEW_AUDIT_LOG |
| Xu ly yeu cau ho tro | 8 | HANDLE_SUPPORT |

<a id="table-roles"></a>

### 55. `roles`

| role_id | role_name | description |
| --- | --- | --- |
| 1 | CenterManager | Quan ly trung tam - van hanh lop hoc, goi tap, bao cao |
| 2 | Coach | Huan luyen vien |
| 3 | Member | Thanh vien / hoc vien |
| 4 | Receptionist | Nhan vien le tan |
| 5 | Admin | Quan tri vien he thong - quan tri nguoi dung va phan quyen RBAC |

## Kịch bản động và giới hạn

- Normal Pending/Expired/waitlist/chỗ cuối và callback đến muộn tạo tại lúc chạy bằng API; không seed giữ chỗ đã hết hạn. Bộ kiểm HTTP local kiểm commit/race, không gọi gateway thật.
- N05 có phương án chuyển N11: M01/M03 đang chờ phản hồi; M02 đã chọn Refund và mất quyền ở lớp gốc. Accept phải commit transfer cùng linkage; không dùng nhãn Accepted khi quyền chưa chuyển.
- Refund gồm chờ receiver/duyệt/Approved/chờ xác minh/Completed/sự cố chưa rõ; Completed có attempt, private PDF và nguồn đối soát DEMO độc lập. Chứng từ upload không tự Completed. Phiên bản receiver và verification được giữ riêng, lịch sử không bị ghi đè.
- Incident N10 giữ buổi gốc invalidated và buổi bù Scheduled; lớp Ongoing. Attendance cũ là dữ liệu tổng hợp; QR/OTP và cửa sổ correction cần ca đúng thời điểm khi demo.
- Advanced giữ 45 ngày/12 credit; Expired có credit bị thu hồi, Active 0 credit vẫn có buổi đã đặt. CashCorrection không là tiền hoàn.
- R1/R2, FE/browser, cloud/deploy, QueryDR/provider thật và Google/avatar thuộc lượt tiếp theo, chưa nghiệm thu ở đây.

