# SCMS — Bàn giao BE: kết quả kiểm tra API và database ngày 29/10/2026

Phản hồi từng phát hiện và trạng thái sau bàn giao nằm tại [đối chiếu1:1](docs/00-MASTER-CHECKLIST.md#report-trace). Nội dung/số liệu bên dưới giữ đúng mốc29/10; thay đổi20room và báo vắng sau đó không làm các quan sát snapshot này thành lỗi.

## 1. Kết luận

- **Dữ liệu demo mới BE bàn giao đã khớp snapshot Supabase được kiểm tra.** Chưa phát hiện bất thường trong các truy vấn đối soát đã chạy; chưa có căn cứ yêu cầu sửa dữ liệu hoặc rebuild DB hiện tại.
- Các endpoint chỉ đọc đã gọi trả kết quả hoạt động như phần 4. Không còn bằng chứng 500 tại các tổ hợp endpoint/role đã kiểm lại; không suy rộng sang mọi role/query.
- **Chưa có lỗi BE vi phạm contract được xác nhận chắc chắn trong bộ bằng chứng này.** Có một hiện tượng API cần BE kiểm tra scope: Admin gọi dashboard thành công nhưng nhận toàn bộ số liệu bằng 0. Một số trường DTO/chỉ số cần xác nhận cách hiểu như phần 3.
- Kết quả này không phải nghiệm thu toàn bộ nghiệp vụ. Luồng ghi, thanh toán online và callback chưa được xác minh bằng bằng chứng runtime trong gói này; không liệt kê chúng thành lỗi BE.

## 2. Môi trường và căn cứ

| Nội dung           | Thông tin                                                                                                  |
| ------------------ | ---------------------------------------------------------------------------------------------------------- |
| API                | https://scms-app-p1gy.onrender.com                                                                         |
| Database           | Supabase dự án djfyoghrujewipalffdf; Owner xác nhận Render kết nối DB này                                  |
| Snapshot DB        | 29/10/2026 19:35:20 +07:00, database postgres; Owner chạy SQL bằng DBeaver và xuất 23 CSV                  |
| API evidence chính | run-20261029-140233-cae6f2, bắt đầu khoảng 21:02 giờ Việt Nam cùng ngày                                    |
| Schema             | schema_postgres.sql mới nhất do BE/Owner bàn giao tại BE_Project-hand-over, biến thể ROOT additive upgrade |
| Contract           | 01-API-CONTRACT-FE-BE (2).md, phần Reports: CenterManager/Admin+VIEW_REPORTS, active/phạm vi center        |
| Source tham chiếu  | Checkout 87146c8; chưa xác nhận là commit runtime Render                                                   |

DB/API được đọc khác thời điểm. Số liệu phải được hiểu trong thời điểm bằng chứng; không khẳng định runtime hiện tại không thay đổi sau lượt kiểm tra.

## 3. Các vấn đề API đã ghi nhận và đề nghị BE kiểm tra

### BE-01 — Admin dashboard trả HTTP 200 nhưng toàn bộ thống kê bằng 0

**Trạng thái:** đã ghi nhận hiện tượng runtime; cần xác nhận scope trước khi phân loại defect.

**Cách tái hiện trong lượt kiểm thử:** đăng nhập Admin demo được BE bàn giao, sau đó gọi GET `/api/reports/dashboard`. Đối chứng bằng cùng endpoint dưới tài khoản CenterManager.

| Chỉ số              | Admin | CenterManager |
| ------------------- | ----: | ------------: |
| totalUsers          |     0 |            32 |
| totalMembers        |     0 |            10 |
| totalClasses        |     0 |            18 |
| totalRevenue        |     0 |    27.200.000 |
| totalInvoices       |     0 |            12 |
| activeSubscriptions |     0 |            11 |

**Đối chứng DB:** có 33 users, 18 classes, 13 invoices. Admin có center_id NULL; tài khoản Manager thuộc center 1.

**Ảnh hưởng:** API trả thành công nhưng số liệu Admin không thể hiện dữ liệu đang có; cần làm rõ liệu đây là kết quả đúng scope hay nhánh xử lý thiếu.

**Đề nghị BE:**

1. Xác nhận API dành cho Admin phải tổng hợp toàn hệ thống hay yêu cầu chọn center rõ ràng.
2. Nếu Admin được phép xem số liệu: kiểm nhánh lọc center NULL và xử lý scope theo contract; bổ sung test Admin/Manager trên cùng dữ liệu.
3. Nếu thiếu scope là request không hợp lệ: thống nhất cách truyền center và response rõ ràng, tránh trả toàn 0 khiến client hiểu DB không có dữ liệu.

Source tham chiếu lọc theo center là giả thuyết phù hợp hiện tượng; chưa có runtime trace để khẳng định nguyên nhân. Không đề nghị gán Admin center=1 hoặc mở quyền toàn hệ thống để chữa tạm.

**Evidence:** api_verification.log/json trong run-20261029-140233-cae6f2; Q01-totals.csv và Q02-roles.csv.

### BE-02 — auth/me không có trường centerId

**Trạng thái:** quan sát DTO đã xác nhận, chưa phải lỗi vi phạm contract.

GET `/api/auth/me` của cả 5 role trả các key `userId`, `email`, `fullName`, `role`, `authorities`, `isFirstLogin`, `memberId`; **không có key centerId**, không phải centerId:null. DB có center_id=1 cho 32 tài khoản non-Admin.

**Đề nghị BE:** xác nhận nguồn lấy center chính thức nếu client cần center để gọi các API có phạm vi trung tâm. Chỉ bổ sung DTO khi contract yêu cầu; nếu trường này chủ ý không được cung cấp thì ghi rõ. Không coi việc thiếu field tự thân là lỗi mapping.

**Evidence:** các mục auth_me/rawKeys/centerIdStatus trong API results.

### BE-03 — Xác nhận định nghĩa activeSubscriptions, không ghi nhận lỗi số liệu

Manager dashboard trả activeSubscriptions=11, trong khi member_packages có 2 quyền Active và tổng class_enrollments có 9 bản ghi. Hiện chưa đối chiếu trạng thái từng enrollment, vì vậy không khẳng định công thức 2+9 đã được chứng minh.

**Đề nghị BE:** xác nhận chỉ số gồm những loại quyền/ghi danh và trạng thái nào để có thể đối chiếu chính xác. Không yêu cầu sửa giá trị 11 khi chưa chứng minh tính sai.

**Lưu ý doanh thu:** dashboard trả 27.200.000/12 hóa đơn; revenue lọc tháng 10 trả 23.700.000/11 giao dịch. Khác khoảng thời gian nên chưa phải lỗi. CSV tổng hợp hiện chưa đủ tái tính độc lập toàn bộ tiền theo từng hóa đơn.

## 4. Kết quả API đối chứng đã hoạt động

| Role         | Endpoint / hành vi đã kiểm                                                | Kết quả                                                        |
| ------------ | ------------------------------------------------------------------------- | -------------------------------------------------------------- |
| 5 role       | POST /api/auth/login; GET /api/auth/me                                    | 200, đăng nhập/đọc danh tính được                              |
| Admin        | GET /api/users, /api/roles, /api/permissions, /api/audit-logs             | 200; lần lượt 33 users, 5 roles, 18 permissions, 15 audit logs |
| Manager      | GET /api/classes, /api/subjects, /api/rooms, /api/staff, /api/holidays    | 200; lần lượt 18, 9, 8, 22, 18 bản ghi                         |
| Manager      | GET /api/reports/dashboard; /api/reports/revenue với khoảng ngày tháng 10 | 200; số liệu như phần 3                                        |
| Receptionist | GET /api/classes, /api/rooms trong run trước                              | 200; không đại diện toàn bộ API Receptionist                   |
| Coach        | GET /api/sessions                                                         | 200, 15 ca trong lượt đã kiểm                                  |
| Coach        | GET /api/reports/dashboard                                                | 403 đúng ca negative đã kiểm                                   |
| Member       | GET /api/members/me/invoices                                              | 200, 2 hóa đơn                                                 |
| Member       | GET /api/members/2/subscriptions                                          | 200, quyền 2 Active, remainingCredit=11                        |
| Member       | GET /api/sessions/catalog/10                                              | 200, các ID [9,10,11,12,109], có buổi bù 109                   |
| Member       | GET /api/users                                                            | 403 đúng ca negative đã kiểm                                   |
| Public       | GET /api/public/subjects                                                  | 200, 9 môn                                                     |

Các count trên là kiểm tra tại thời điểm gọi, không chứng minh toàn bộ field/ownership/RBAC đã đạt. Catalog trả các ca phù hợp bộ lọc/tương lai nên 5 ca không đồng nghĩa lớp chỉ có 5 buổi trong DB.

**Đính chính GET subscriptions:** GET `/api/members/me/subscriptions` từng trả 400 do kịch bản gọi tuyến không được định nghĩa cho method GET trong contract/source tham chiếu. Đọc quyền dùng GET `/api/members/{memberId}/subscriptions`; tuyến me có POST mua gói. Không bàn giao hiện tượng này thành lỗi BE.

**Lỗi lịch sử F-06/F-07:** trong lượt kiểm lại, Coach sessions và Admin/Manager dashboard đã trả 200. Chưa đủ dữ liệu đóng F-06 cho tất cả role/query cũ; không báo chúng vẫn 500 dựa evidence trước đây.

## 5. Database đã khớp bàn giao mới tới đâu?

| Nội dung BE bàn giao / truy vấn                  | Kết quả snapshot                                                     | Nhận xét                                                                      |
| ------------------------------------------------ | -------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Cấu trúc cần audit                               | 53/53 cột FOUND; inventory 174 cột/16 bảng                           | Đủ các cột truy vấn đã sử dụng; không chứng nhận toàn schema/index/constraint |
| 33 tài khoản Active                              | 33/33; 1 Admin, 2 Manager, 2 Receptionist, 18 Coach, 10 Member       | Khớp danh sách demo/role/center đã đối chiếu                                  |
| 9 môn, 8 sân                                     | Đúng số lượng; môn Active, giá Normal/Advanced và outdoor có dữ liệu | Không phát hiện outdoor NULL trong danh mục đã xuất                           |
| 9 Normal, 9 Advanced                             | 9 Normal Ongoing, 9 Advanced template Open                           | Khớp phân loại bàn giao; Advanced không cần 12 ca cố định                     |
| Normal đủ 12 ca hợp lệ                           | 9/9 lớp đều đủ                                                       | Tennis 13 dòng vật lý do giữ ca gốc invalidated và thêm ca bù                 |
| Buổi bù                                          | 109 thay 1, cùng lớp 10; gốc Cancelled và invalidated                | Không có duplicate makeup reference theo query                                |
| 13 invoices, 4 quyền, 9 ghi danh                 | 13/4/9                                                               | Khớp tổng số bàn giao                                                         |
| 114 sessions, 73 attendance, 73 training results | 114/73/73                                                            | 73 cặp session/member khớp, không orphan/duplicate/mismatch theo query        |
| Quyền Advanced/credit                            | 2 Active, 1 Cancelled, 1 Expired; credit không âm                    | Không thấy buổi gắn sai quyền/chủ quyền; chưa kiểm toán ledger                |
| Lịch sử sự cố/báo vắng                           | 1 incident Resolved, 1 notice active                                 | Xác nhận có bản ghi, không chứng nhận quy trình tạo/xử lý                     |
| Trùng sân                                        | 0 cặp theo Q11                                                       | Không đại diện kiểm concurrency hoặc trùng Coach/Member                       |

**Kết luận DB:** dữ liệu mới đã có và khớp bộ demo được BE mô tả trong phạm vi snapshot/query. Chưa phát hiện lỗi DB cần BE sửa từ lượt đối soát này. Không cần chạy lại schema/rebuild chỉ vì ảnh lỗi cũ hoặc khác biệt giữa tổng dòng vật lý và ca hợp lệ.

## 6. Giới hạn kết luận nghiệp vụ thanh toán

Snapshot có 12 Paid/Cash, 1 Expired/Cash; không có Pending/Online và không có payment_resolutions. Đây là hiện trạng dữ liệu, **không phải lỗi BE**. Chưa có bằng chứng runtime mới đủ kết luận việc tạo đơn online, IPN/Return, hoàn tiền hoặc cấp quyền sau thanh toán đúng/sai. Không yêu cầu BE sửa các luồng chưa được tái hiện và không dùng HTTP 200 của API đọc để chứng nhận chúng.

**Phản hồi mong muốn từ BE:** BE-01 xác nhận scope dashboard và hướng xử lý nếu sai; BE-02 xác nhận nguồn center; BE-03 xác nhận công thức chỉ số. Ngoài các mục trên, bộ bằng chứng hiện tại chưa xác nhận lỗi BE/DB production khác cần sửa. Báo cáo tổng hợp dữ liệu đã lưu ngày 29/10, không phải lượt probe mới tại thời điểm gửi.
