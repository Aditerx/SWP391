# Danh mục toàn bộ API Render và nguồn đối chiếu

Sinh từ snapshot OpenAPI và controller đang checkout. Schema/route khớp không chứng nhận nghiệp vụ.
GET anonymous 401/403 chỉ chứng minh cổng đăng nhập. Không có phiên đăng nhập hoặc mutation production trong đợt audit này.
Phạm vi role/owner/center còn có kiểm tra trong service; biểu thức annotation bên dưới không thay toàn bộ policy.
Schema request/response đầy đủ: [schemas.md](schemas.md); mẫu HTTP thật local: [http-exchanges.json](http-exchanges.json).

| # | Method | Path | Dxx | Local HTTP exchanges | Render anonymous GET |
|---|---|---|---|---|---|
| 1 | GET | `/api/advanced-registrations` | D03 | 0 | 401 |
| 2 | POST | `/api/advanced-registrations` | D03 | 12 | chưa gọi |
| 3 | POST | `/api/advanced-registrations/{id}/decision` | D03 | 6 | chưa gọi |
| 4 | POST | `/api/advanced-registrations/{id}/order` | D03 | 18 | chưa gọi |
| 5 | GET | `/api/attendance` | D15,D17 | 0 | 401 |
| 6 | POST | `/api/attendance/check-in` | D15,D17 | 0 | chưa gọi |
| 7 | POST | `/api/attendance/check-out` | D15,D17 | 0 | chưa gọi |
| 8 | GET | `/api/attendance/checkin-code` | D15,D17 | 0 | 401 |
| 9 | POST | `/api/attendance/correct` | D15,D17 | 0 | chưa gọi |
| 10 | GET | `/api/attendance/me/checkin-code` | D15,D17 | 0 | 401 |
| 11 | POST | `/api/attendance/scan` | D15,D17 | 0 | chưa gọi |
| 12 | GET | `/api/attendances` | D15,D17 | 0 | 401 |
| 13 | POST | `/api/attendances/check-in` | D15,D17 | 0 | chưa gọi |
| 14 | POST | `/api/attendances/check-out` | D15,D17 | 0 | chưa gọi |
| 15 | GET | `/api/attendances/checkin-code` | D15,D17 | 0 | 401 |
| 16 | POST | `/api/attendances/correct` | D15,D17 | 4 | chưa gọi |
| 17 | GET | `/api/attendances/me/checkin-code` | D15,D17 | 3 | 401 |
| 18 | POST | `/api/attendances/scan` | D15,D17 | 5 | chưa gọi |
| 19 | GET | `/api/audit-logs` | baseline | 0 | 401 |
| 20 | POST | `/api/auth/change-password` | baseline | 0 | chưa gọi |
| 21 | POST | `/api/auth/forgot-password` | baseline | 0 | chưa gọi |
| 22 | POST | `/api/auth/login` | baseline | 0 | chưa gọi |
| 23 | POST | `/api/auth/logout` | baseline | 0 | chưa gọi |
| 24 | GET | `/api/auth/me` | baseline | 0 | 401 |
| 25 | POST | `/api/auth/register` | baseline | 0 | chưa gọi |
| 26 | POST | `/api/auth/resend-otp` | baseline | 0 | chưa gọi |
| 27 | POST | `/api/auth/reset-password` | baseline | 0 | chưa gọi |
| 28 | POST | `/api/auth/verify-registration` | baseline | 0 | chưa gọi |
| 29 | GET | `/api/center-incidents` | D06,D08,D12,D14,D15 | 0 | 401 |
| 30 | POST | `/api/center-incidents` | D06,D08,D12,D14,D15 | 4 | chưa gọi |
| 31 | POST | `/api/center-incidents/{id}/compensate` | D06,D08,D12,D14,D15 | 3 | chưa gọi |
| 32 | POST | `/api/center-incidents/{id}/normal-makeup` | D06,D08,D12,D14,D15 | 2 | chưa gọi |
| 33 | POST | `/api/center-incidents/{id}/resolve` | D06,D08,D12,D14,D15 | 5 | chưa gọi |
| 34 | GET | `/api/chat/conversations` | baseline | 0 | 401 |
| 35 | POST | `/api/chat/conversations` | baseline | 0 | chưa gọi |
| 36 | GET | `/api/chat/conversations/me/active` | baseline | 0 | 401 |
| 37 | POST | `/api/chat/conversations/{id}/claim` | baseline | 0 | chưa gọi |
| 38 | PATCH | `/api/chat/conversations/{id}/close` | baseline | 0 | chưa gọi |
| 39 | GET | `/api/chat/conversations/{id}/messages` | baseline | 0 | 401 |
| 40 | POST | `/api/chat/conversations/{id}/messages` | baseline | 0 | chưa gọi |
| 41 | POST | `/api/chat/conversations/{id}/read` | baseline | 0 | chưa gọi |
| 42 | POST | `/api/chat/conversations/{id}/release` | baseline | 0 | chưa gọi |
| 43 | PATCH | `/api/chat/conversations/{id}/reopen` | baseline | 0 | chưa gọi |
| 44 | GET | `/api/chat/me/unread-count` | baseline | 0 | 401 |
| 45 | GET | `/api/classes` | D03,D12,D14,D15 | 0 | 401 |
| 46 | POST | `/api/classes` | D03,D12,D14,D15 | 0 | chưa gọi |
| 47 | POST | `/api/classes/normal-with-sessions` | D03,D12,D14,D15 | 1 | chưa gọi |
| 48 | POST | `/api/classes/{classId}/cancel-enrollment` | D03,D12,D14,D15 | 1 | chưa gọi |
| 49 | POST | `/api/classes/{classId}/enroll` | D03,D12,D14,D15 | 1 | chưa gọi |
| 50 | GET | `/api/classes/{classId}/enrollments` | D03,D12,D14,D15 | 0 | 401 |
| 51 | GET | `/api/classes/{classId}/feedback` | D03,D12,D14,D15 | 0 | 401 |
| 52 | POST | `/api/classes/{classId}/feedback` | D03,D12,D14,D15 | 0 | chưa gọi |
| 53 | GET | `/api/classes/{classId}/feedback/me` | D03,D12,D14,D15 | 0 | 401 |
| 54 | GET | `/api/classes/{id}` | D03,D12,D14,D15 | 0 | 401 |
| 55 | PUT | `/api/classes/{id}` | D03,D12,D14,D15 | 0 | chưa gọi |
| 56 | PATCH | `/api/classes/{id}/coach` | D03,D12,D14,D15 | 0 | chưa gọi |
| 57 | GET | `/api/coaches` | D09 | 0 | 401 |
| 58 | GET | `/api/coaches/me/at-risk-members` | baseline | 0 | 401 |
| 59 | GET | `/api/coaches/me/feedback` | baseline | 0 | 401 |
| 60 | GET | `/api/coaches/{coachId}/available-slots` | baseline | 0 | 401 |
| 61 | GET | `/api/coaches/{id}/admission-capacity` | D03 | 0 | 401 |
| 62 | GET | `/api/coaches/{id}/work-schedule` | D03 | 0 | 401 |
| 63 | PUT | `/api/coaches/{id}/work-schedule` | D03 | 1 | chưa gọi |
| 64 | GET | `/api/enrollments` | D03,D12,D14,D15 | 0 | 401 |
| 65 | POST | `/api/enrollments` | D03,D12,D14,D15 | 0 | chưa gọi |
| 66 | DELETE | `/api/enrollments/{id}` | D03,D12,D14,D15 | 0 | chưa gọi |
| 67 | PATCH | `/api/enrollments/{id}/cancel` | D03,D12,D14,D15 | 0 | chưa gọi |
| 68 | GET | `/api/evaluations` | baseline | 0 | 401 |
| 69 | POST | `/api/evaluations` | baseline | 0 | chưa gọi |
| 70 | DELETE | `/api/evaluations/{id}` | baseline | 0 | chưa gọi |
| 71 | GET | `/api/evaluations/{id}` | baseline | 0 | 401 |
| 72 | PUT | `/api/evaluations/{id}` | baseline | 0 | chưa gọi |
| 73 | GET | `/api/health` | baseline | 0 | 200 |
| 74 | HEAD | `/api/health` | baseline | 0 | chưa gọi |
| 75 | GET | `/api/holidays` | D12,D14 | 0 | 401 |
| 76 | POST | `/api/holidays` | D12,D14 | 4 | chưa gọi |
| 77 | GET | `/api/holidays/active` | D12,D14 | 0 | 401 |
| 78 | POST | `/api/holidays/preview` | D12,D14 | 6 | chưa gọi |
| 79 | PATCH | `/api/holidays/{id}/toggle` | D12,D14 | 3 | chưa gọi |
| 80 | GET | `/api/invoices` | D03,D10,D16 | 0 | 401 |
| 81 | GET | `/api/invoices/{id}` | D03,D10,D16 | 0 | 401 |
| 82 | POST | `/api/invoices/{id}/cancel` | D03,D10,D16 | 0 | chưa gọi |
| 83 | POST | `/api/invoices/{id}/confirm-cash` | D03,D10,D16 | 6 | chưa gọi |
| 84 | POST | `/api/invoices/{id}/pay-online` | D03,D10,D16 | 1 | chưa gọi |
| 85 | GET | `/api/invoices/{id}/receipt` | D03,D10,D16 | 1 | 401 |
| 86 | GET | `/api/member-packages` | D03,D08,D12,D13 | 0 | 401 |
| 87 | POST | `/api/member-packages/subscribe-me` | D03,D08,D12,D13 | 0 | chưa gọi |
| 88 | POST | `/api/member-packages/{id}/extend` | D03,D08,D12,D13 | 0 | chưa gọi |
| 89 | PATCH | `/api/member-packages/{id}/status` | D03,D08,D12,D13 | 0 | chưa gọi |
| 90 | GET | `/api/members` | baseline | 0 | 401 |
| 91 | POST | `/api/members` | baseline | 0 | chưa gọi |
| 92 | GET | `/api/members/at-risk` | baseline | 0 | 401 |
| 93 | GET | `/api/members/me/checkin-code` | baseline | 0 | 401 |
| 94 | GET | `/api/members/me/invoices` | D03,D10,D16 | 0 | 401 |
| 95 | GET | `/api/members/me/invoices/{id}` | D03,D10,D16 | 2 | 401 |
| 96 | POST | `/api/members/me/subscriptions` | D03,D08,D12,D13 | 0 | chưa gọi |
| 97 | POST | `/api/members/register` | baseline | 0 | chưa gọi |
| 98 | GET | `/api/members/{id}` | baseline | 0 | 401 |
| 99 | PUT | `/api/members/{id}` | baseline | 0 | chưa gọi |
| 100 | PATCH | `/api/members/{id}/booking-block/lift` | baseline | 0 | chưa gọi |
| 101 | PATCH | `/api/members/{id}/status` | baseline | 0 | chưa gọi |
| 102 | GET | `/api/members/{memberId}/classes` | baseline | 0 | 401 |
| 103 | GET | `/api/members/{memberId}/enrollments` | baseline | 0 | 401 |
| 104 | GET | `/api/members/{memberId}/packages` | baseline | 0 | 401 |
| 105 | POST | `/api/members/{memberId}/packages` | baseline | 0 | chưa gọi |
| 106 | GET | `/api/members/{memberId}/subscriptions` | D03,D08,D12,D13 | 4 | 401 |
| 107 | POST | `/api/members/{memberId}/subscriptions` | D03,D08,D12,D13 | 0 | chưa gọi |
| 108 | GET | `/api/notifications/me` | baseline | 0 | 401 |
| 109 | PATCH | `/api/notifications/me/read-all` | baseline | 0 | chưa gọi |
| 110 | GET | `/api/notifications/me/unread-count` | baseline | 0 | 401 |
| 111 | PATCH | `/api/notifications/{id}/read` | baseline | 0 | chưa gọi |
| 112 | GET | `/api/packages` | baseline | 0 | 401 |
| 113 | POST | `/api/packages` | baseline | 0 | chưa gọi |
| 114 | PUT | `/api/packages/{id}` | baseline | 0 | chưa gọi |
| 115 | PATCH | `/api/packages/{id}/status` | baseline | 0 | chưa gọi |
| 116 | GET | `/api/payment-resolutions` | D10,D16 | 0 | 401 |
| 117 | POST | `/api/payment-resolutions` | D10,D16 | 4 | chưa gọi |
| 118 | POST | `/api/payment-resolutions/{id}/approve` | D10,D16 | 5 | chưa gọi |
| 119 | POST | `/api/payment-resolutions/{id}/complete` | D10,D16 | 3 | chưa gọi |
| 120 | GET | `/api/payments/vnpay/ipn` | D03,D10,D16 | 4 | chưa gọi |
| 121 | GET | `/api/payments/vnpay/return` | D03,D10,D16 | 1 | chưa gọi |
| 122 | GET | `/api/permissions` | baseline | 0 | 401 |
| 123 | GET | `/api/public/membership-packages` | baseline | 0 | 200 |
| 124 | GET | `/api/public/subjects` | D02 | 0 | 200 |
| 125 | GET | `/api/reports/dashboard` | D13,D18,D19 | 1 | 401 |
| 126 | GET | `/api/reports/members` | D13,D18,D19 | 2 | 401 |
| 127 | GET | `/api/reports/revenue` | D13,D18,D19 | 3 | 401 |
| 128 | GET | `/api/roles` | baseline | 0 | 401 |
| 129 | GET | `/api/roles/{id}` | baseline | 0 | 401 |
| 130 | PUT | `/api/roles/{id}/permissions` | baseline | 0 | chưa gọi |
| 131 | GET | `/api/rooms` | baseline | 0 | 401 |
| 132 | POST | `/api/rooms` | baseline | 0 | chưa gọi |
| 133 | PUT | `/api/rooms/{id}` | baseline | 0 | chưa gọi |
| 134 | PATCH | `/api/rooms/{id}/status` | baseline | 0 | chưa gọi |
| 135 | GET | `/api/sessions` | D03,D12,D14,D15 | 0 | 401 |
| 136 | POST | `/api/sessions` | D03,D12,D14,D15 | 0 | chưa gọi |
| 137 | POST | `/api/sessions/book-private` | D03,D04,D12 | 18 | chưa gọi |
| 138 | GET | `/api/sessions/catalog/{classId}` | D03,D12,D14,D15 | 0 | 401 |
| 139 | POST | `/api/sessions/check-conflict` | D03,D12,D14,D15 | 0 | chưa gọi |
| 140 | GET | `/api/sessions/coaches/{coachId}/available-slots` | D03,D12,D14,D15 | 0 | 401 |
| 141 | POST | `/api/sessions/generate` | D03,D12,D14,D15 | 0 | chưa gọi |
| 142 | POST | `/api/sessions/preview-recurring` | D03,D12,D14,D15 | 0 | chưa gọi |
| 143 | GET | `/api/sessions/time-slots` | D03,D12,D14,D15 | 0 | 401 |
| 144 | DELETE | `/api/sessions/{id}` | D03,D12,D14,D15 | 0 | chưa gọi |
| 145 | GET | `/api/sessions/{id}` | D03,D12,D14,D15 | 0 | 401 |
| 146 | PUT | `/api/sessions/{id}` | D03,D12,D14,D15 | 0 | chưa gọi |
| 147 | PATCH | `/api/sessions/{id}/status` | D03,D12,D14,D15 | 1 | chưa gọi |
| 148 | POST | `/api/sessions/{sessionId}/absence` | D05 | 2 | chưa gọi |
| 149 | GET | `/api/sessions/{sessionId}/absence-notices` | D05 | 0 | 401 |
| 150 | POST | `/api/sessions/{sessionId}/cancel-private` | D05 | 4 | chưa gọi |
| 151 | POST | `/api/sessions/{sessionId}/reschedule` | D06,D07,D08 | 6 | chưa gọi |
| 152 | POST | `/api/sessions/{sessionId}/substitute-coach` | D06 | 0 | chưa gọi |
| 153 | GET | `/api/specializations` | baseline | 0 | 401 |
| 154 | POST | `/api/specializations` | baseline | 0 | chưa gọi |
| 155 | PUT | `/api/specializations/{id}` | baseline | 0 | chưa gọi |
| 156 | PATCH | `/api/specializations/{id}/status` | baseline | 0 | chưa gọi |
| 157 | GET | `/api/staff` | baseline | 0 | 401 |
| 158 | POST | `/api/staff` | baseline | 0 | chưa gọi |
| 159 | PUT | `/api/staff/{id}` | baseline | 0 | chưa gọi |
| 160 | PATCH | `/api/staff/{id}/status` | baseline | 0 | chưa gọi |
| 161 | GET | `/api/subjects` | D02 | 0 | 401 |
| 162 | POST | `/api/subjects` | D02 | 0 | chưa gọi |
| 163 | DELETE | `/api/subjects/{id}` | D02 | 0 | chưa gọi |
| 164 | GET | `/api/subjects/{id}` | D02 | 0 | 401 |
| 165 | PUT | `/api/subjects/{id}` | D02 | 0 | chưa gọi |
| 166 | GET | `/api/subscriptions` | D03,D08,D12,D13 | 0 | 401 |
| 167 | POST | `/api/subscriptions/{id}/extend` | D03,D08,D12,D13 | 0 | chưa gọi |
| 168 | PATCH | `/api/subscriptions/{id}/status` | D03,D08,D12,D13 | 2 | chưa gọi |
| 169 | GET | `/api/training-plans` | baseline | 0 | 401 |
| 170 | POST | `/api/training-plans` | baseline | 0 | chưa gọi |
| 171 | DELETE | `/api/training-plans/{id}` | baseline | 0 | chưa gọi |
| 172 | GET | `/api/training-plans/{id}` | baseline | 0 | 401 |
| 173 | PUT | `/api/training-plans/{id}` | baseline | 0 | chưa gọi |
| 174 | GET | `/api/training-results` | D15,D17 | 0 | 401 |
| 175 | POST | `/api/training-results` | D15,D17 | 0 | chưa gọi |
| 176 | POST | `/api/training-results/bulk` | D15,D17 | 0 | chưa gọi |
| 177 | GET | `/api/users` | baseline | 0 | 401 |
| 178 | POST | `/api/users` | baseline | 0 | chưa gọi |
| 179 | DELETE | `/api/users/me/avatar` | baseline | 0 | chưa gọi |
| 180 | POST | `/api/users/me/avatar` | baseline | 0 | chưa gọi |
| 181 | GET | `/api/users/{id}` | baseline | 0 | 401 |
| 182 | PUT | `/api/users/{id}` | baseline | 0 | chưa gọi |
| 183 | POST | `/api/users/{id}/reissue-password` | baseline | 0 | chưa gọi |
| 184 | PATCH | `/api/users/{id}/role` | baseline | 0 | chưa gọi |
| 185 | PATCH | `/api/users/{id}/status` | baseline | 0 | chưa gọi |

## Chi tiết từng thao tác

### 1. GET /api/advanced-registrations

- operationId: `list_2`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `isAuthenticated()`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:24` → `list`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<AdvancedRegistrationResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 2. POST /api/advanced-registrations

- operationId: `request`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:26` → `request`.
- Request body: application/json: AdvancedRegistrationRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AdvancedRegistrationResponse.
- Local HTTP exchange indexes (0-based): 1, 2, 13, 14, 22, 23, 31, 32, 90, 91, 101, 102.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 3. POST /api/advanced-registrations/{id}/decision

- operationId: `decide`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('COACH','CENTERMANAGER','ADMIN')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:28` → `decide`.
- Request body: application/json: Decision; required.
- Responses do OpenAPI khai báo: 200 */*: AdvancedRegistrationResponse.
- Local HTTP exchange indexes (0-based): 4, 16, 25, 34, 93, 104.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 4. POST /api/advanced-registrations/{id}/order

- operationId: `order`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:30` → `order`.
- Request body: application/json: Order; required.
- Responses do OpenAPI khai báo: 200 */*: SubscriptionOrderResponse.
- Local HTTP exchange indexes (0-based): 3, 5, 6, 15, 17, 18, 24, 26, 27, 33, 35, 36, 92, 94, 95, 103, 105, 106.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 5. GET /api/attendance

- operationId: `searchAttendances_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:22` → `searchAttendances`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<AttendanceResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |
| recordedBy | query | False | integer(int32) |  |
| state | query | False | string |  |
| startDate | query | False | string(date) |  |
| endDate | query | False | string(date) |  |

### 6. POST /api/attendance/check-in

- operationId: `checkIn_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:44` → `checkIn`.
- Request body: application/json: CheckInRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 7. POST /api/attendance/check-out

- operationId: `checkOut_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:50` → `checkOut`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | query | True | integer(int32) |  |

### 8. GET /api/attendance/checkin-code

- operationId: `checkinCode_2`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:38` → `checkinCode`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CheckinCodeResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 9. POST /api/attendance/correct

- operationId: `correctAttendance`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:62` → `correctAttendance`.
- Request body: application/json: AttendanceCorrectionRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 10. GET /api/attendance/me/checkin-code

- operationId: `checkinCode_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:38` → `checkinCode`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CheckinCodeResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 11. POST /api/attendance/scan

- operationId: `scan_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:56` → `scan`.
- Request body: application/json: CheckinScanRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 12. GET /api/attendances

- operationId: `searchAttendances`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:22` → `searchAttendances`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<AttendanceResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |
| recordedBy | query | False | integer(int32) |  |
| state | query | False | string |  |
| startDate | query | False | string(date) |  |
| endDate | query | False | string(date) |  |

### 13. POST /api/attendances/check-in

- operationId: `checkIn`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:44` → `checkIn`.
- Request body: application/json: CheckInRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 14. POST /api/attendances/check-out

- operationId: `checkOut`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:50` → `checkOut`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | query | True | integer(int32) |  |

### 15. GET /api/attendances/checkin-code

- operationId: `checkinCode_3`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:38` → `checkinCode`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CheckinCodeResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 16. POST /api/attendances/correct

- operationId: `correctAttendance_1`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:62` → `correctAttendance`.
- Request body: application/json: AttendanceCorrectionRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): 80, 81, 82, 83.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 17. GET /api/attendances/me/checkin-code

- operationId: `checkinCode_4`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:38` → `checkinCode`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CheckinCodeResponse.
- Local HTTP exchange indexes (0-based): 72, 75, 78.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 18. POST /api/attendances/scan

- operationId: `scan`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/AttendanceController.java:56` → `scan`.
- Request body: application/json: CheckinScanRequest; required.
- Responses do OpenAPI khai báo: 200 */*: AttendanceResponse.
- Local HTTP exchange indexes (0-based): 73, 74, 76, 77, 79.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 19. GET /api/audit-logs

- operationId: `findAll_10`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('VIEW_AUDIT_LOG')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/audit/AuditController.java:18` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<SystemLog>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 20. POST /api/auth/change-password

- operationId: `changePassword`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:96` → `changePassword`.
- Request body: application/json: ChangePasswordRequest; required.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 21. POST /api/auth/forgot-password

- operationId: `forgotPassword`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:130` → `forgotPassword`.
- Request body: application/json: ForgotPasswordRequest; required.
- Responses do OpenAPI khai báo: 200 */*: OtpMessageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 22. POST /api/auth/login

- operationId: `login`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:50` → `login`.
- Request body: application/json: LoginRequest; required.
- Responses do OpenAPI khai báo: 200 */*: LoginResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 23. POST /api/auth/logout

- operationId: `logout`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:120` → `logout`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 24. GET /api/auth/me

- operationId: `me`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:88` → `me`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: LoginResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 25. POST /api/auth/register

- operationId: `register`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:136` → `register`.
- Request body: application/json: RegisterRequest; required.
- Responses do OpenAPI khai báo: 200 */*: OtpMessageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 26. POST /api/auth/resend-otp

- operationId: `resendOtp`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:148` → `resendOtp`.
- Request body: application/json: ResendOtpRequest; required.
- Responses do OpenAPI khai báo: 200 */*: OtpMessageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 27. POST /api/auth/reset-password

- operationId: `resetPassword`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:155` → `resetPassword`.
- Request body: application/json: ResetPasswordRequest; required.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 28. POST /api/auth/verify-registration

- operationId: `verifyRegistration`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/auth/AuthController.java:142` → `verifyRegistration`.
- Request body: application/json: VerifyRegistrationRequest; required.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 29. GET /api/center-incidents

- operationId: `list_1`; Dxx: D06,D08,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/CenterIncidentController.java:18` → `list`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<CenterIncidentResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 30. POST /api/center-incidents

- operationId: `open_1`; Dxx: D06,D08,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/CenterIncidentController.java:19` → `open`.
- Request body: application/json: CenterIncidentOpenRequest; required.
- Responses do OpenAPI khai báo: 200 */*: CenterIncidentResponse.
- Local HTTP exchange indexes (0-based): 51, 58, 123, 142.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 31. POST /api/center-incidents/{id}/compensate

- operationId: `compensate`; Dxx: D06,D08,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/CenterIncidentController.java:21` → `compensate`.
- Request body: application/json: Compensate; required.
- Responses do OpenAPI khai báo: 200 */*: CenterIncidentResponse.
- Local HTTP exchange indexes (0-based): 127, 128, 144.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 32. POST /api/center-incidents/{id}/normal-makeup

- operationId: `makeup`; Dxx: D06,D08,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/CenterIncidentController.java:22` → `makeup`.
- Request body: application/json: NormalMakeupRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): 145, 146.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 33. POST /api/center-incidents/{id}/resolve

- operationId: `resolve`; Dxx: D06,D08,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/CenterIncidentController.java:20` → `resolve`.
- Request body: application/json: Resolve; required.
- Responses do OpenAPI khai báo: 200 */*: CenterIncidentResponse.
- Local HTTP exchange indexes (0-based): 69, 70, 125, 126, 143.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 34. GET /api/chat/conversations

- operationId: `inbox`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('HANDLE_CHAT')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:35` → `inbox`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: PageChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| status | query | False | string | ['OPEN', 'CLOSED'] |
| assigned | query | False | boolean |  |
| unreadOnly | query | False | boolean | False |
| page | query | False | integer(int32) | 0 |
| size | query | False | integer(int32) | 30 |

### 35. POST /api/chat/conversations

- operationId: `getOrCreate`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:18` → `getOrCreate`.
- Request body: application/json: ChatConversationRequest; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 36. GET /api/chat/conversations/me/active

- operationId: `active`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:23` → `active`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 37. POST /api/chat/conversations/{id}/claim

- operationId: `claim`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('HANDLE_CHAT')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:40` → `claim`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 38. PATCH /api/chat/conversations/{id}/close

- operationId: `close`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('HANDLE_CHAT')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:44` → `close`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 39. GET /api/chat/conversations/{id}/messages

- operationId: `messages`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:27` → `messages`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<ChatMessageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |
| afterId | query | False | integer(int32) | 0 |
| limit | query | False | integer(int32) | 30 |

### 40. POST /api/chat/conversations/{id}/messages

- operationId: `send`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:30` → `send`.
- Request body: application/json: ChatMessageRequest; required.
- Responses do OpenAPI khai báo: 200 */*: ChatMessageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 41. POST /api/chat/conversations/{id}/read

- operationId: `read`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:33` → `read`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 42. POST /api/chat/conversations/{id}/release

- operationId: `release`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('HANDLE_CHAT')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:42` → `release`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 43. PATCH /api/chat/conversations/{id}/reopen

- operationId: `reopen`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('HANDLE_CHAT')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:46` → `reopen`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatConversationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 44. GET /api/chat/me/unread-count

- operationId: `unread`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/chat/ChatController.java:25` → `unread`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ChatUnreadCountResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 45. GET /api/classes

- operationId: `findAll_9`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:27` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<SportsClassResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 46. POST /api/classes

- operationId: `create_6`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:38` → `create`.
- Request body: application/json: SportsClassRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SportsClassResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 47. POST /api/classes/normal-with-sessions

- operationId: `createNormal`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:21` → `createNormal`.
- Request body: application/json: NormalClassCreationRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SportsClassResponse.
- Local HTTP exchange indexes (0-based): 135.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 48. POST /api/classes/{classId}/cancel-enrollment

- operationId: `cancelEnrollment`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:74` → `cancelEnrollment`.
- Request body: application/json: EnrollmentRequest; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: EnrollmentResponse.
- Local HTTP exchange indexes (0-based): 138.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 49. POST /api/classes/{classId}/enroll

- operationId: `enrollInClass`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:44` → `enrollInClass`.
- Request body: application/json: EnrollmentRequest; optional/no body.
- Responses do OpenAPI khai báo: 201 */*: EnrollmentResponse.
- Local HTTP exchange indexes (0-based): 136.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 50. GET /api/classes/{classId}/enrollments

- operationId: `findByClassId`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:30` → `findByClassId`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<EnrollmentResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 51. GET /api/classes/{classId}/feedback

- operationId: `findForClass`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/feedback/ClassFeedbackController.java:37` → `findForClass`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ClassFeedbackSummaryResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 52. POST /api/classes/{classId}/feedback

- operationId: `create_7`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/feedback/ClassFeedbackController.java:17` → `create`.
- Request body: application/json: ClassFeedbackRequest; required.
- Responses do OpenAPI khai báo: 201 */*: ClassFeedbackResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 53. GET /api/classes/{classId}/feedback/me

- operationId: `findMine`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/feedback/ClassFeedbackController.java:28` → `findMine`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: ClassFeedbackResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 54. GET /api/classes/{id}

- operationId: `findById_5`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:32` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: SportsClassResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 55. PUT /api/classes/{id}

- operationId: `update_5`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:44` → `update`.
- Request body: application/json: SportsClassRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SportsClassResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 56. PATCH /api/classes/{id}/coach

- operationId: `assignCoach`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/sportclass/SportsClassController.java:52` → `assignCoach`.
- Request body: application/json: AssignCoachRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SportsClassResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 57. GET /api/coaches

- operationId: `findAll_8`; Dxx: D09.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/CoachController.java:43` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<CoachSummaryResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| specializationId | query | False | integer(int32) |  |
| keyword | query | False | string |  |

### 58. GET /api/coaches/me/at-risk-members

- operationId: `atRiskMembers_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('COACH')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/CoachController.java:28` → `atRiskMembers`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<AtRiskMemberResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 59. GET /api/coaches/me/feedback

- operationId: `findForCurrentCoach`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('COACH')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/feedback/ClassFeedbackController.java:43` → `findForCurrentCoach`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CoachFeedbackSummaryResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | query | False | integer(int32) |  |

### 60. GET /api/coaches/{coachId}/available-slots

- operationId: `getAvailableSlots`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/CoachController.java:34` → `getAvailableSlots`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<CoachAvailableSlotResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| coachId | path | True | integer(int32) |  |
| date | query | True | string(date) |  |
| subjectId | query | False | integer(int32) |  |
| roomId | query | False | integer(int32) |  |

### 61. GET /api/coaches/{id}/admission-capacity

- operationId: `capacity`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `isAuthenticated()`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:22` → `capacity`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: Capacity.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |
| subjectId | query | True | integer(int32) |  |

### 62. GET /api/coaches/{id}/work-schedule

- operationId: `work_1`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `isAuthenticated()`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:20` → `work`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: WorkResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 63. PUT /api/coaches/{id}/work-schedule

- operationId: `work`; Dxx: D03.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/AdvancedRegistrationController.java:32` → `work`.
- Request body: application/json: Work; required.
- Responses do OpenAPI khai báo: 200 no body schema.
- Local HTTP exchange indexes (0-based): 0.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 64. GET /api/enrollments

- operationId: `findAll_7`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:20` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<EnrollmentResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |
| status | query | False | string |  |

### 65. POST /api/enrollments

- operationId: `createEnrollment`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:59` → `createEnrollment`.
- Request body: application/json: EnrollmentRequest; required.
- Responses do OpenAPI khai báo: 201 */*: EnrollmentResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 66. DELETE /api/enrollments/{id}

- operationId: `deleteEnrollment`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:95` → `deleteEnrollment`.
- Request body: application/json: EnrollmentRequest; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: EnrollmentResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 67. PATCH /api/enrollments/{id}/cancel

- operationId: `cancelById`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:85` → `cancelById`.
- Request body: application/json: EnrollmentRequest; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: EnrollmentResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 68. GET /api/evaluations

- operationId: `searchEvaluations`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('COACH') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/EvaluationController.java:19` → `searchEvaluations`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<EvaluationResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | query | False | integer(int32) |  |
| coachId | query | False | integer(int32) |  |

### 69. POST /api/evaluations

- operationId: `createEvaluation`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/EvaluationController.java:36` → `createEvaluation`.
- Request body: application/json: EvaluationRequest; required.
- Responses do OpenAPI khai báo: 201 */*: EvaluationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 70. DELETE /api/evaluations/{id}

- operationId: `deleteEvaluation`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/EvaluationController.java:52` → `deleteEvaluation`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 71. GET /api/evaluations/{id}

- operationId: `findById_4`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('COACH') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/EvaluationController.java:29` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: EvaluationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 72. PUT /api/evaluations/{id}

- operationId: `updateEvaluation`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/EvaluationController.java:42` → `updateEvaluation`.
- Request body: application/json: EvaluationRequest; required.
- Responses do OpenAPI khai báo: 200 */*: EvaluationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 73. GET /api/health

- operationId: `ping`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/common/HealthCheckController.java:21` → `ping`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: object/map.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 200; 200 public catalog/identity.

### 74. HEAD /api/health

- operationId: `ping_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/common/HealthCheckController.java:21` → `ping`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: object/map.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 75. GET /api/holidays

- operationId: `findAll_6`; Dxx: D12,D14.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/holiday/HolidayController.java:23` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<HolidayResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 76. POST /api/holidays

- operationId: `create_5`; Dxx: D12,D14.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/holiday/HolidayController.java:44` → `create`.
- Request body: application/json: HolidayRequest; required.
- Responses do OpenAPI khai báo: 201 */*: HolidayResponse.
- Local HTTP exchange indexes (0-based): 57, 60, 62, 134.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| X-Preview-Token | header | False | string |  |

### 77. GET /api/holidays/active

- operationId: `findActive`; Dxx: D12,D14.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/holiday/HolidayController.java:28` → `findActive`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<HolidayResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 78. POST /api/holidays/preview

- operationId: `preview`; Dxx: D12,D14.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/holiday/HolidayController.java:19` → `preview`.
- Request body: application/json: Change; required.
- Responses do OpenAPI khai báo: 200 */*: Preview.
- Local HTTP exchange indexes (0-based): 56, 61, 63, 65, 67, 133.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 79. PATCH /api/holidays/{id}/toggle

- operationId: `toggleActive`; Dxx: D12,D14.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/holiday/HolidayController.java:33` → `toggleActive`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: HolidayResponse.
- Local HTTP exchange indexes (0-based): 64, 66, 68.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |
| X-Preview-Token | header | False | string |  |

### 80. GET /api/invoices

- operationId: `findAll_5`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:26` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<InvoiceResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | query | False | integer(int32) |  |
| packageId | query | False | integer(int32) |  |
| receptionistId | query | False | integer(int32) |  |
| status | query | False | string |  |
| method | query | False | string |  |
| keyword | query | False | string |  |
| startDate | query | False | string(date) |  |
| endDate | query | False | string(date) |  |

### 81. GET /api/invoices/{id}

- operationId: `findById_3`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:43` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: InvoiceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 82. POST /api/invoices/{id}/cancel

- operationId: `cancel`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:67` → `cancel`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: InvoiceResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 83. POST /api/invoices/{id}/confirm-cash

- operationId: `confirmCash`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') and hasRole('RECEPTIONIST')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:57` → `confirmCash`.
- Request body: application/json: CashNote; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: InvoiceResponse.
- Local HTTP exchange indexes (0-based): 8, 9, 10, 96, 107, 137.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 84. POST /api/invoices/{id}/pay-online

- operationId: `payOnline`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:71` → `payOnline`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: object/map.
- Local HTTP exchange indexes (0-based): 28.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 85. GET /api/invoices/{id}/receipt

- operationId: `receipt`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:63` → `receipt`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: InvoiceResponse.
- Local HTTP exchange indexes (0-based): 112.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 86. GET /api/member-packages

- operationId: `getAllSubscriptions_1`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:20` → `getAllSubscriptions`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MemberPackageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 87. POST /api/member-packages/subscribe-me

- operationId: `subscribeMe_1`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:40` → `subscribeMe`.
- Request body: application/json: MemberPackageRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SubscriptionOrderResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 88. POST /api/member-packages/{id}/extend

- operationId: `extendValidity`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:59` → `extendValidity`.
- Request body: application/json: PackageExtensionRequest; required.
- Responses do OpenAPI khai báo: 200 */*: MemberPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 89. PATCH /api/member-packages/{id}/status

- operationId: `updateStatus_4`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:52` → `updateStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: MemberPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 90. GET /api/members

- operationId: `getAllMembers`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'REGISTER_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:28` → `getAllMembers`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MemberResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 91. POST /api/members

- operationId: `createMember`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:40` → `createMember`.
- Request body: application/json: MemberRequest; required.
- Responses do OpenAPI khai báo: 201 */*: MemberResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 92. GET /api/members/at-risk

- operationId: `atRiskMembers`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('VIEW_REPORTS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:73` → `atRiskMembers`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<AtRiskMemberResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 93. GET /api/members/me/checkin-code

- operationId: `checkinCode`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:67` → `checkinCode`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: CheckinCodeResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 94. GET /api/members/me/invoices

- operationId: `ownInvoices`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:49` → `ownInvoices`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<InvoiceResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 95. GET /api/members/me/invoices/{id}

- operationId: `ownInvoice`; Dxx: D03,D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:53` → `ownInvoice`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: InvoiceResponse.
- Local HTTP exchange indexes (0-based): 7, 30.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 96. POST /api/members/me/subscriptions

- operationId: `subscribeMe`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:40` → `subscribeMe`.
- Request body: application/json: MemberPackageRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SubscriptionOrderResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 97. POST /api/members/register

- operationId: `registerMember`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('REGISTER_MEMBER')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:47` → `registerMember`.
- Request body: application/json: MemberRequest; required.
- Responses do OpenAPI khai báo: 201 */*: MemberResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 98. GET /api/members/{id}

- operationId: `getMemberById`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'REGISTER_MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:34` → `getMemberById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: MemberResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 99. PUT /api/members/{id}

- operationId: `updateMember`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:54` → `updateMember`.
- Request body: application/json: MemberRequest; required.
- Responses do OpenAPI khai báo: 200 */*: MemberResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 100. PATCH /api/members/{id}/booking-block/lift

- operationId: `liftBookingBlock`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:79` → `liftBookingBlock`.
- Request body: application/json: BookingBlockLiftRequest; required.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 101. PATCH /api/members/{id}/status

- operationId: `updateMemberStatus`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/MemberController.java:60` → `updateMemberStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: MemberResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 102. GET /api/members/{memberId}/classes

- operationId: `findByMemberId_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:36` → `findByMemberId`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<EnrollmentResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 103. GET /api/members/{memberId}/enrollments

- operationId: `findByMemberId`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/enrollment/EnrollmentController.java:36` → `findByMemberId`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<EnrollmentResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 104. GET /api/members/{memberId}/packages

- operationId: `getSubscriptionsByMember`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:26` → `getSubscriptionsByMember`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MemberPackageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 105. POST /api/members/{memberId}/packages

- operationId: `subscribe`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:33` → `subscribe`.
- Request body: application/json: MemberPackageRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SubscriptionOrderResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 106. GET /api/members/{memberId}/subscriptions

- operationId: `getSubscriptionsByMember_1`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:26` → `getSubscriptionsByMember`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MemberPackageResponse>.
- Local HTTP exchange indexes (0-based): 11, 12, 42, 47.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 107. POST /api/members/{memberId}/subscriptions

- operationId: `subscribe_1`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:33` → `subscribe`.
- Request body: application/json: MemberPackageRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SubscriptionOrderResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| memberId | path | True | integer(int32) |  |

### 108. GET /api/notifications/me

- operationId: `listMine`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/notification/NotificationController.java:32` → `listMine`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 401 */*: PageNotificationResponse, 200 */*: PageNotificationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| unreadOnly | query | False | boolean | False |
| type | query | False | string | ['ENROLLMENT_CONFIRMED', 'PRIVATE_BOOKING_CONFIRMED', 'PRIVATE_BOOKING_CANCELLED', 'SESSION_RESCHEDULED', 'COACH_SUBSTITUTED', 'PAYMENT_SUCCESS', 'PAYMENT_EXPIRED', 'PACKAGE_EXTENDED', 'PACKAGE_EXPIRING_SOON', 'PACKAGE_EXPIRED', 'CREDIT_LOW', 'NO_SHOW_RECORDED', 'SESSION_REMINDER', 'CHAT_MEMBER_MESSAGE', 'CHAT_STAFF_REPLY', 'PAYMENT_RECONCILED', 'CENTER_INCIDENT_UPDATED'] |
| pageable | query | True | Pageable |  |

### 109. PATCH /api/notifications/me/read-all

- operationId: `markAllRead`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/notification/NotificationController.java:65` → `markAllRead`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 401 no body schema, 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 110. GET /api/notifications/me/unread-count

- operationId: `unreadCount`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/notification/NotificationController.java:48` → `unreadCount`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: UnreadCountResponse, 401 */*: UnreadCountResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 111. PATCH /api/notifications/{id}/read

- operationId: `markRead`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/notification/NotificationController.java:56` → `markRead`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 404 no body schema, 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 112. GET /api/packages

- operationId: `findAll_4`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MembershipPackageController.java:18` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MembershipPackageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 113. POST /api/packages

- operationId: `create_4`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_PACKAGES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MembershipPackageController.java:24` → `create`.
- Request body: application/json: MembershipPackageRequest; required.
- Responses do OpenAPI khai báo: 201 */*: MembershipPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 114. PUT /api/packages/{id}

- operationId: `update_4`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_PACKAGES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MembershipPackageController.java:31` → `update`.
- Request body: application/json: MembershipPackageRequest; required.
- Responses do OpenAPI khai báo: 200 */*: MembershipPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 115. PATCH /api/packages/{id}/status

- operationId: `updateStatus_3`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_PACKAGES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MembershipPackageController.java:39` → `updateStatus`.
- Request body: application/json: PackageStatusRequest; required.
- Responses do OpenAPI khai báo: 200 */*: MembershipPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 116. GET /api/payment-resolutions

- operationId: `list`; Dxx: D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') and hasAnyRole('RECEPTIONIST','CENTERMANAGER','ADMIN')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/PaymentResolutionController.java:14` → `list`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<PaymentResolutionResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 117. POST /api/payment-resolutions

- operationId: `open`; Dxx: D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') and hasAnyRole('RECEPTIONIST','CENTERMANAGER','ADMIN')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/PaymentResolutionController.java:15` → `open`.
- Request body: application/json: PaymentResolutionOpenRequest; required.
- Responses do OpenAPI khai báo: 200 */*: PaymentResolutionResponse.
- Local HTTP exchange indexes (0-based): 84, 99, 108, 140.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 118. POST /api/payment-resolutions/{id}/approve

- operationId: `approve`; Dxx: D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') and hasAnyRole('CENTERMANAGER','ADMIN')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/PaymentResolutionController.java:16` → `approve`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: PaymentResolutionResponse.
- Local HTTP exchange indexes (0-based): 85, 86, 100, 109, 141.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 119. POST /api/payment-resolutions/{id}/complete

- operationId: `complete`; Dxx: D10,D16.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_INVOICES') and hasAnyRole('CENTERMANAGER','ADMIN')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/PaymentResolutionController.java:17` → `complete`.
- Request body: application/json: Complete; required.
- Responses do OpenAPI khai báo: 200 */*: PaymentResolutionResponse.
- Local HTTP exchange indexes (0-based): 88, 89, 110.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 120. GET /api/payments/vnpay/ipn

- operationId: `vnpayIpn`; Dxx: D03,D10,D16.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:79` → `vnpayIpn`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: object/map.
- Local HTTP exchange indexes (0-based): 20, 21, 29, 37.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| params | query | True | object/map |  |

### 121. GET /api/payments/vnpay/return

- operationId: `vnpayReturn`; Dxx: D03,D10,D16.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/invoice/InvoiceController.java:88` → `vnpayReturn`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: RedirectView.
- Local HTTP exchange indexes (0-based): 19.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| params | query | True | object/map |  |

### 122. GET /api/permissions

- operationId: `getAllPermissions`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/RoleController.java:35` → `getAllPermissions`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<PermissionResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 123. GET /api/public/membership-packages

- operationId: `packages`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/publicapi/PublicCatalogController.java:29` → `packages`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<PublicMembershipPackageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 200; 200 public catalog/identity.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| centerId | query | False | integer(int32) | 1 |

### 124. GET /api/public/subjects

- operationId: `subjects`; Dxx: D02.
- Authentication: Public theo SecurityConfig.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/publicapi/PublicCatalogController.java:22` → `subjects`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<PublicSubjectResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 200; 200 public catalog/identity.

### 125. GET /api/reports/dashboard

- operationId: `dashboard`; Dxx: D13,D18,D19.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/report/ReportController.java:20` → `dashboard`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: DashboardResponse.
- Local HTTP exchange indexes (0-based): 71.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 126. GET /api/reports/members

- operationId: `members`; Dxx: D13,D18,D19.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/report/ReportController.java:36` → `members`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: MemberReportResponse.
- Local HTTP exchange indexes (0-based): 147, 148.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 127. GET /api/reports/revenue

- operationId: `revenue`; Dxx: D13,D18,D19.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/report/ReportController.java:26` → `revenue`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: RevenueReportResponse.
- Local HTTP exchange indexes (0-based): 87, 113, 114.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| startDate | query | False | string(date) |  |
| endDate | query | False | string(date) |  |
| groupBy | query | False | string | month |

### 128. GET /api/roles

- operationId: `getAllRoles`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/RoleController.java:19` → `getAllRoles`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<RoleResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 129. GET /api/roles/{id}

- operationId: `getRoleById`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/RoleController.java:24` → `getRoleById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: RoleResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 130. PUT /api/roles/{id}/permissions

- operationId: `updateRolePermissions`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_PERMISSIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/RoleController.java:29` → `updateRolePermissions`.
- Request body: application/json: RolePermissionUpdateRequest; required.
- Responses do OpenAPI khai báo: 200 */*: RoleResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 131. GET /api/rooms

- operationId: `findAll_3`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/room/RoomController.java:18` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<RoomResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 132. POST /api/rooms

- operationId: `create_3`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/room/RoomController.java:24` → `create`.
- Request body: application/json: RoomRequest; required.
- Responses do OpenAPI khai báo: 201 */*: RoomResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 133. PUT /api/rooms/{id}

- operationId: `update_3`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/room/RoomController.java:30` → `update`.
- Request body: application/json: RoomRequest; required.
- Responses do OpenAPI khai báo: 200 */*: RoomResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 134. PATCH /api/rooms/{id}/status

- operationId: `updateStatus_2`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/room/RoomController.java:37` → `updateStatus`.
- Request body: application/json: RoomStatusRequest; required.
- Responses do OpenAPI khai báo: 200 */*: RoomResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 135. GET /api/sessions

- operationId: `findAll_2`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:26` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<SessionResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | query | False | integer(int32) |  |
| coachId | query | False | integer(int32) |  |
| roomId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |
| startDate | query | False | string(date) |  |
| endDate | query | False | string(date) |  |
| status | query | False | string |  |

### 136. POST /api/sessions

- operationId: `create_2`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:103` → `create`.
- Request body: application/json: SessionRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 137. POST /api/sessions/book-private

- operationId: `bookPrivate`; Dxx: D03,D04,D12.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:64` → `bookPrivate`.
- Request body: application/json: PrivateBookingRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): 38, 39, 40, 41, 48, 97, 115, 116, 117, 118, 119, 120, 121, 122, 124, 130, 131, 132.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 138. GET /api/sessions/catalog/{classId}

- operationId: `publicTimetable`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:44` → `publicTimetable`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<CatalogSessionResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| classId | path | True | integer(int32) |  |

### 139. POST /api/sessions/check-conflict

- operationId: `checkConflict`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:90` → `checkConflict`.
- Request body: application/json: ConflictCheckRequest; required.
- Responses do OpenAPI khai báo: 200 */*: ConflictCheckResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 140. GET /api/sessions/coaches/{coachId}/available-slots

- operationId: `getCoachAvailableSlots`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:49` → `getCoachAvailableSlots`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<CoachAvailableSlotResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| coachId | path | True | integer(int32) |  |
| date | query | True | string(date) |  |
| subjectId | query | False | integer(int32) |  |
| roomId | query | False | integer(int32) |  |

### 141. POST /api/sessions/generate

- operationId: `generateRecurring`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:110` → `generateRecurring`.
- Request body: application/json: SessionGenerateRequest; required.
- Responses do OpenAPI khai báo: 201 */*: array<SessionResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 142. POST /api/sessions/preview-recurring

- operationId: `previewRecurring`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:96` → `previewRecurring`.
- Request body: application/json: RecurringPreviewRequest; required.
- Responses do OpenAPI khai báo: 200 */*: RecurringPreviewResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 143. GET /api/sessions/time-slots

- operationId: `getTimeSlots`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:38` → `getTimeSlots`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<TimeSlotResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| isOutdoor | query | False | boolean |  |

### 144. DELETE /api/sessions/{id}

- operationId: `delete_1`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:149` → `delete`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 145. GET /api/sessions/{id}

- operationId: `findById_2`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:58` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 146. PUT /api/sessions/{id}

- operationId: `update_2`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:116` → `update`.
- Request body: application/json: SessionRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 147. PATCH /api/sessions/{id}/status

- operationId: `updateStatus_1`; Dxx: D03,D12,D14,D15.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:123` → `updateStatus`.
- Request body: application/json: SessionStatusRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): 43.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 148. POST /api/sessions/{sessionId}/absence

- operationId: `requestAbsence`; Dxx: D05.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:81` → `requestAbsence`.
- Request body: application/json: AbsenceRequest; required.
- Responses do OpenAPI khai báo: 200 */*: Notice.
- Local HTTP exchange indexes (0-based): 49, 139.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | path | True | integer(int32) |  |

### 149. GET /api/sessions/{sessionId}/absence-notices

- operationId: `absenceHistory`; Dxx: D05.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `isAuthenticated()`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:87` → `absenceHistory`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<Notice>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | path | True | integer(int32) |  |

### 150. POST /api/sessions/{sessionId}/cancel-private

- operationId: `cancelPrivate`; Dxx: D05.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:73` → `cancelPrivate`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): 45, 46, 50, 98.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | path | True | integer(int32) |  |

### 151. POST /api/sessions/{sessionId}/reschedule

- operationId: `reschedule`; Dxx: D06,D07,D08.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasRole('COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:130` → `reschedule`.
- Request body: application/json: SessionRescheduleRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): 52, 53, 54, 55, 59, 129.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | path | True | integer(int32) |  |

### 152. POST /api/sessions/{sessionId}/substitute-coach

- operationId: `substituteCoach`; Dxx: D06.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/session/SessionController.java:139` → `substituteCoach`.
- Request body: application/json: SubstituteCoachRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SessionResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | path | True | integer(int32) |  |

### 153. GET /api/specializations

- operationId: `findAll_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/SpecializationController.java:17` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<SpecializationResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| status | query | False | string |  |
| subjectId | query | False | integer(int32) |  |

### 154. POST /api/specializations

- operationId: `create_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SPECIALIZATIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/SpecializationController.java:23` → `create`.
- Request body: application/json: SpecializationRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SpecializationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 155. PUT /api/specializations/{id}

- operationId: `update_1`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SPECIALIZATIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/SpecializationController.java:29` → `update`.
- Request body: application/json: SpecializationRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SpecializationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 156. PATCH /api/specializations/{id}/status

- operationId: `updateStatus`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SPECIALIZATIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/specialization/SpecializationController.java:35` → `updateStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: SpecializationResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 157. GET /api/staff

- operationId: `getAllStaff`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/StaffController.java:20` → `getAllStaff`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<StaffResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 158. POST /api/staff

- operationId: `createStaff`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/StaffController.java:26` → `createStaff`.
- Request body: application/json: StaffRequest; required.
- Responses do OpenAPI khai báo: 201 */*: StaffResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 159. PUT /api/staff/{id}

- operationId: `updateStaff`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/StaffController.java:33` → `updateStaff`.
- Request body: application/json: StaffRequest; required.
- Responses do OpenAPI khai báo: 200 */*: StaffResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 160. PATCH /api/staff/{id}/status

- operationId: `updateStaffStatus`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/StaffController.java:39` → `updateStaffStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: StaffResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 161. GET /api/subjects

- operationId: `findAll`; Dxx: D02.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/subject/SubjectController.java:18` → `findAll`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<SubjectResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 162. POST /api/subjects

- operationId: `create`; Dxx: D02.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/subject/SubjectController.java:29` → `create`.
- Request body: application/json: SubjectRequest; required.
- Responses do OpenAPI khai báo: 201 */*: SubjectResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 163. DELETE /api/subjects/{id}

- operationId: `delete`; Dxx: D02.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/subject/SubjectController.java:43` → `delete`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 164. GET /api/subjects/{id}

- operationId: `findById_1`; Dxx: D02.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/subject/SubjectController.java:23` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: SubjectResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 165. PUT /api/subjects/{id}

- operationId: `update`; Dxx: D02.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/subject/SubjectController.java:35` → `update`.
- Request body: application/json: SubjectRequest; required.
- Responses do OpenAPI khai báo: 200 */*: SubjectResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 166. GET /api/subscriptions

- operationId: `getAllSubscriptions`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:20` → `getAllSubscriptions`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<MemberPackageResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

### 167. POST /api/subscriptions/{id}/extend

- operationId: `extendValidity_1`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:59` → `extendValidity`.
- Request body: application/json: PackageExtensionRequest; required.
- Responses do OpenAPI khai báo: 200 */*: MemberPackageResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 168. PATCH /api/subscriptions/{id}/status

- operationId: `updateStatus_5`; Dxx: D03,D08,D12,D13.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_SUBSCRIPTIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/membership/MemberPackageController.java:52` → `updateStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: MemberPackageResponse.
- Local HTTP exchange indexes (0-based): 44, 111.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 169. GET /api/training-plans

- operationId: `searchPlans`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingPlanController.java:19` → `searchPlans`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<TrainingPlanResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| coachId | query | False | integer(int32) |  |
| classId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |

### 170. POST /api/training-plans

- operationId: `createPlan`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingPlanController.java:37` → `createPlan`.
- Request body: application/json: TrainingPlanRequest; required.
- Responses do OpenAPI khai báo: 201 */*: TrainingPlanResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 171. DELETE /api/training-plans/{id}

- operationId: `deletePlan`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingPlanController.java:53` → `deletePlan`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 172. GET /api/training-plans/{id}

- operationId: `findById`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingPlanController.java:30` → `findById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: TrainingPlanResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 173. PUT /api/training-plans/{id}

- operationId: `updatePlan`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingPlanController.java:43` → `updatePlan`.
- Request body: application/json: TrainingPlanRequest; required.
- Responses do OpenAPI khai báo: 200 */*: TrainingPlanResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 174. GET /api/training-results

- operationId: `searchResults`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('COACH')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingResultController.java:18` → `searchResults`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<TrainingResultResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| sessionId | query | False | integer(int32) |  |
| memberId | query | False | integer(int32) |  |
| coachId | query | False | integer(int32) |  |

### 175. POST /api/training-results

- operationId: `recordResult`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingResultController.java:29` → `recordResult`.
- Request body: application/json: TrainingResultRequest; required.
- Responses do OpenAPI khai báo: 200 */*: TrainingResultResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 176. POST /api/training-results/bulk

- operationId: `recordBulkResults`; Dxx: D15,D17.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/training/TrainingResultController.java:35` → `recordBulkResults`.
- Request body: application/json: BulkTrainingResultRequest; required.
- Responses do OpenAPI khai báo: 200 */*: array<TrainingResultResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 177. GET /api/users

- operationId: `getAllUsers`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:21` → `getAllUsers`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: array<UserResponse>.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| role | query | False | string |  |
| search | query | False | string |  |

### 178. POST /api/users

- operationId: `createUser`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [201]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:36` → `createUser`.
- Request body: application/json: UserRequest; required.
- Responses do OpenAPI khai báo: 201 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 179. DELETE /api/users/me/avatar

- operationId: `deleteMyAvatar`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [204]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:56` → `deleteMyAvatar`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 204 no body schema.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 180. POST /api/users/me/avatar

- operationId: `uploadMyAvatar`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `không có annotation riêng; vẫn chịu SecurityConfig/service guards`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:49` → `uploadMyAvatar`.
- Request body: multipart/form-data: object; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: AvatarResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

### 181. GET /api/users/{id}

- operationId: `getUserById`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:30` → `getUserById`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: anonymous GET 401; 401/403 only; authorization gate, not business correctness.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 182. PUT /api/users/{id}

- operationId: `updateUser`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:63` → `updateUser`.
- Request body: application/json: UserRequest; required.
- Responses do OpenAPI khai báo: 200 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 183. POST /api/users/{id}/reissue-password

- operationId: `reissuePassword`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAnyAuthority('MANAGE_USERS', 'REGISTER_MEMBER', 'MANAGE_PERMISSIONS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:43` → `reissuePassword`.
- Request body: no body schema; optional/no body.
- Responses do OpenAPI khai báo: 200 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 184. PATCH /api/users/{id}/role

- operationId: `updateUserRole`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:76` → `updateUserRole`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

### 185. PATCH /api/users/{id}/status

- operationId: `updateUserStatus`; Dxx: baseline ngoài quyết định Dxx trực tiếp.
- Authentication: Session JSESSIONID bắt buộc.
- Controller permission: `hasAuthority('MANAGE_USERS')`.
- Success status đọc từ controller: [200]; đối chiếu thêm exchange thực tế (OpenAPI có thể chỉ suy mặc định200).
- Handler: `src/main/java/com/sportscenter/user/UserController.java:69` → `updateUserStatus`.
- Request body: application/json: object/map; required.
- Responses do OpenAPI khai báo: 200 */*: UserResponse.
- Local HTTP exchange indexes (0-based): chưa có trong demo9case.
- Render: chưa gọi thao tác này trong đợt audit; không suy từ Swagger.

| Parameter | In | Required | Type | Default / enum |
|---|---|---|---|---|
| id | path | True | integer(int32) |  |

