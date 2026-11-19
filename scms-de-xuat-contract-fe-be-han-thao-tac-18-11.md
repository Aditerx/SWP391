# SCMS: Đề xuất contract FE–BE cho hạn và quyền thao tác (đợt 0)

**Ngày:** 18/11/2026. **Mục đích:** FE tự chốt trước tên field, kiểu thời gian và mã lý do bị chặn dựa trên source BE hiện có, để BE chỉ cần đồng ý hoặc sửa nhỏ thay vì thiết kế lại. Áp dụng cho tài liệu bàn giao "FE đọc để chốt luồng lịch học và điểm danh demo".

**Nguồn đã đối chiếu**
- BE: `Aditerx/SWP391` nhánh `main`, commit `42984f4` (14/11). Schema trên Swagger Render (`/v3/api-docs`) khớp với source này.
- FE: `FE_Project` nhánh `main`, commit `03e83f8` (18/11 22:47).
- Tài liệu bàn giao trỏ tới `docs/05-DEMO-RULE.md`, file này không có trên GitHub. Nghĩa là BE còn thay đổi chưa push. Khi BE push, cần đối chiếu lại các mục dưới.

## 1. Quy ước chung (lấy theo tiền lệ đã có trong BE)

### 1.1. Lỗi
- `BusinessException` → **400**, `BusinessConflictException` → **409**. Nếu message có dạng `MÃ_LỖI: nội dung` thì body là `{code, message}`.
- 403 chỉ có `code` với nhóm `ATTENDANCE_CORRECTION_*`. Lỗi khác trả `{timestamp, status, error, message}`.
- Phần lớn `message` của BE là **tiếng Anh**, vài lỗi là tiếng Việt và không có `code` (hủy buổi `Completed`, hủy buổi không phải 1:1). FE dịch theo `code`; thiếu `code` mới dùng `message`.

### 1.2. Thời gian
- Tiền lệ: `SessionResponse.correctionDeadline` và `SessionResponse.serverTime` là `OffsetDateTime`, trả dạng `2026-11-20T18:15:00+07:00`.
- Field cũ (`expiresAt`, `respondBy`, `confirmationDeadline`, `PrivateSessionProposalResponse.serverTime`...) là `LocalDateTime` không có offset, hiểu là giờ Việt Nam. FE đã xử lý kiểu này trong `paymentAccess.js` (thiếu offset thì gắn `+07:00`).
- **Đề xuất:** mọi field thời gian mới dùng `OffsetDateTime`. Field cũ giữ nguyên định dạng.

### 1.3. Mẫu đặt tên
Theo tiền lệ `correctionAllowed` + `correctionDeadline` + `serverTime`, mỗi thao tác có:
- `<thaoTac>Allowed`: boolean, tính theo người đang đăng nhập tại lần đọc.
- `<thaoTac>Deadline`: hạn cuối, `OffsetDateTime`.
- `<thaoTac>BlockReason`: chuỗi, `null` khi được phép. **Giá trị chính là mã lỗi mà POST tương ứng sẽ trả**, để FE dùng một bảng dịch cho cả hai trường hợp.
- `serverTime`: `OffsetDateTime`, có trên mọi response chứa các field này.

## 2. Field đề xuất theo từng response

### 2.1. `SportsClassResponse` (GET `/api/classes`, GET `/api/classes/{id}`), chỉ cho lớp Normal

| Field | Kiểu | Cách tính |
|---|---|---|
| `firstSessionAt` | OffsetDateTime | `NormalClassPolicy.first()` (buổi hợp lệ đầu tiên đã lưu) |
| `registrationDeadline` | OffsetDateTime | `firstSessionAt − 48h` |
| `registrationAllowed` / `registrationBlockReason` | boolean / string | Kết quả `requireAdmission()`: `NORMAL_REGISTRATION_CLOSED`, `CLASS_NOT_OPEN`, `NORMAL_CHANGE_PENDING`, `CLASS_NOT_READY`, và mã khi lớp đủ chỗ |
| `openingConfirmationDueAt` | OffsetDateTime | `firstSessionAt − 24h` |
| `openingConfirmationDeadline` | OffsetDateTime | `firstSessionAt` |
| `openingReasonRequired` | boolean | `now ≥ firstSessionAt − 24h` |
| `openingConfirmationAllowed` / `openingConfirmationBlockReason` | boolean / string | Chỉ tính cho người có `MANAGE_CLASSES`. Mã: `NORMAL_OPENING_TOO_LATE`, `NORMAL_INSUFFICIENT_PAID`, `CLASS_NOT_READY`, `NORMAL_NOT_AWAITING_OPENING`, `NORMAL_CHANGE_PENDING`, `HOLIDAY_BLOCKED`, `ROOM_UNAVAILABLE`, `COACH_REQUIRED`, `SUBJECT_INACTIVE` |
| `serverTime` | OffsetDateTime | |

Ghi chú:
- `registrationAllowed` là theo lớp, không theo Member. FE vẫn tự kiểm Member đã ghi danh hay chưa.
- Lớp Advanced trả `null` cho các field này.
- Nếu tính trên danh sách lớp bị chậm, BE có thể chỉ trả ở GET chi tiết. FE sẽ đọc chi tiết trước khi mở nút.

### 2.2. `AdvancedRegistrationResponse`

| Field | Kiểu | Cách tính |
|---|---|---|
| `purchaseConfirmationAllowed` / `purchaseConfirmationBlockReason` | boolean / string | Kết quả kiểm của POST `/{id}/order`: `COACH_ACCEPTANCE_REQUIRED`, `REGISTRATION_CONFIRMATION_EXPIRED`, `SUBJECT_UNAVAILABLE`, `MEMBER_NOT_ACTIVE`, `BOOKING_BLOCKED` |
| `invoiceId`, `invoiceStatus` (nên có) | int / string | Hóa đơn gắn với đăng ký, để FE mở đúng hóa đơn Pending khi quay lại trang thay vì tạo mới |
| `serverTime` | OffsetDateTime | |

Ghi chú:
- `confirmationDeadline` hiện tính là `acceptedAt + 24h` ngay cả khi đăng ký đã `Rejected`. FE sẽ không dùng riêng field này để mở nút.
- `purchaseConfirmationAllowed` là cho phép **tạo hóa đơn mua**, không phải cờ đã thanh toán.

### 2.3. `PrivateSessionProposalResponse`: giữ nguyên
Đã có `status` (tự chuyển `Expired` khi quá hạn), `respondBy`, `serverTime`, `memberId`. FE tự tính: được Accept khi `status = Pending`, `memberId` là người đang đăng nhập và `serverTime ≤ respondBy`. Ở đúng mốc `respondBy` vẫn được Accept, vì BE kiểm bằng `isAfter`.

### 2.4. `SessionResponse`

| Field | Kiểu | Cách tính |
|---|---|---|
| `cancellationAllowed` / `cancellationBlockReason` | boolean / string | Chỉ tính cho Member sở hữu buổi. Mã: `CANCEL_TOO_LATE`, `SESSION_ALREADY_CANCELLED`, **`SESSION_ALREADY_ATTENDED` (mới)**, `SESSION_HISTORY_IMMUTABLE`, `ADVANCED_PACKAGE_EXPIRED`, `SUBSCRIPTION_LINK_REQUIRED`, `CREDIT_ALREADY_RETURNED`, cùng hai mã cần thêm ở mục 3 |
| `cancellationDeadline` | OffsetDateTime | Giờ bắt đầu buổi − 12h |
| `attendanceAllowed` / `attendanceBlockReason` | boolean / string | Tính theo Coach đang đăng nhập và buổi. Mã: `ATTENDANCE_WINDOW_CLOSED`, `NORMAL_NOT_STARTED`, `SESSION_TIME_REQUIRED`, `ATTENDANCE_CORRECTION_FORBIDDEN` (sai Coach hoặc ngoài trung tâm), `SESSION_NOT_HELD` |
| `serverTime` | đã có | |

### 2.5. `CheckinCodeResponse`
Giữ `{payload, expiresInSeconds}`, nên thêm `expiresAt` (OffsetDateTime). Hiện mã đổi theo khung 30 giây (`30 − epoch % 30`). Khi đổi sang 300 giây, `expiresInSeconds` có thể tới 300.

### 2.6. `InvoiceResponse`
- Đề xuất thêm `serverTime`. FE (`paymentAccess.js`) đã đọc `invoice.serverTime`, nhưng BE chưa trả nên FE đang dùng đồng hồ máy người dùng.
- Hạn hóa đơn hiện đọc từ `payment.expire-minutes`, mặc định **5 phút** (cấu hình qua biến môi trường). Theo quyết định mới là 30 phút. FE luôn đếm theo `expiresAt` nên không phụ thuộc con số này.

## 3. Việc BE cần xác nhận hoặc sửa (để FE làm tiếp)

1. Đồng ý mẫu tên ở mục 1.3 và danh sách field ở mục 2.
2. `...BlockReason` dùng đúng mã lỗi mà POST tương ứng trả.
3. Field thời gian mới dùng `OffsetDateTime` (`+07:00`).
4. Thêm `code` cho hai lỗi đang thiếu ở POST `/api/sessions/{id}/cancel-private`. Tên đề xuất: `SESSION_COMPLETED` (buổi đã hoàn thành) và `PRIVATE_SESSION_REQUIRED` (không phải buổi 1:1).
5. Thêm `invoiceId`/`invoiceStatus` vào `AdvancedRegistrationResponse` (nên có).
6. **Điểm danh sớm:** `SessionAttendancePolicy.requireNormalStarted()` hiện đòi lớp `Ongoing` hoặc `Closed`. Lớp Normal chỉ chuyển `Ongoing` khi tới giờ buổi đầu (job lifecycle), còn xác nhận khai giảng chỉ ghi `openingConfirmedAt`. Vì vậy khi bật điểm danh sớm, cần đổi điều kiện thành "đã có `openingConfirmedAt`". Nếu không, Normal vẫn bị `NORMAL_NOT_STARTED` dù đã bỏ chặn khung giờ.
7. `SESSION_ALREADY_ATTENDED`: 409, body `{code, message}`, đúng như tài liệu bàn giao.

## 4. FE xử lý giai đoạn chuyển tiếp
- Field mới chưa có (`undefined`): FE giữ hành vi hiện tại, tức là mở nút và để BE từ chối khi lưu, rồi hiển thị theo `code`.
- Field có giá trị: FE dùng `...Allowed` để mở hoặc khóa nút, `...BlockReason` để giải thích, `...Deadline` + `serverTime` để hiển thị hạn.
- BE vẫn kiểm lại khi lưu. FE tải lại dữ liệu sau mỗi lần bị từ chối.

## 5. Phát hiện phía FE (đưa vào đợt 1)
1. **Mã điểm danh 300 giây sẽ làm hỏng màn hình hiện tại.** `MemberDashboard.jsx` báo lỗi nếu `expiresInSeconds > 30`, còn `MemberAttendanceView.jsx` cắt TTL về tối đa 30. Phải sửa trước khi BE lên 300 giây. `MemberCheckinView.jsx` đã đọc đúng TTL từ BE.
2. `bookPrivateSession` (POST `/api/sessions/book-private`) trong `memberPortalApi.js` vẫn được export nhưng không còn chỗ gọi. Gỡ đi để không ai dùng nhầm.
3. Chủ nhật: đã có `toBackendDaysOfWeek` (0 → 7) trong `normalClassCreation.js` và `SUNDAY` trong `WeeklySlotGrid.jsx`. Việc còn lại là kiểm các chỗ còn tự dùng `getDay()` (`CoachDashboard.jsx`, `mappers.js`, `MemberClassCatalog.jsx`, `classFormHelpers.js`) và viết test.
4. Bảng dịch mã lỗi đang rải rác (`CoachAttendanceView.jsx`, `CoachTrainingProgress.jsx`, `MemberCourseCatalog.jsx`, `privateSessionProposalErrors.js`). Chưa có bản dịch cho `CANCEL_TOO_LATE`, `ADVANCED_BOOKING_TOO_LATE`, `NORMAL_OPENING_TOO_LATE`, `SESSION_ALREADY_ATTENDED`. Nên gom về một bảng chung trong `apiErrors.js`.
5. `CoachAttendanceView.jsx` đã cho chọn buổi từ hôm nay trở đi. Khi BE mở điểm danh sớm, chỉ cần đọc `attendanceAllowed`/`attendanceBlockReason`, không phải đổi cách chọn buổi.
