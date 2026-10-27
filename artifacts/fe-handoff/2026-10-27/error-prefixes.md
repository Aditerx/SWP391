# Mã lỗi có nguồn và các status đã quan sát

Tách mã literal/prefix trong source và mã đã thấy ở HTTP local. Không phải mọi literal đều đã được kích hoạt; không có status quan sát thì không gán400/409 bằng suy đoán. Các lỗi không có code vẫn cần fallback như contract mục11.
HTTP statuses dưới đây chỉ từ exchange local; danh mục không chứng nhận đã chạy hết error branches trên Render.

| Code | HTTP đã quan sát local | Nguồn |
|---|---|---|
| `ABSENCE_WINDOW_CLOSED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionAbsenceService.java:36` |
| `ADVANCED_BOOKING_TOO_LATE` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:269` |
| `ADVANCED_PACKAGE_EXPIRED` | 400 | `src/main/java/com/sportscenter/session/SessionService.java:255`, `src/main/java/com/sportscenter/session/SessionService.java:414`, `src/main/java/com/sportscenter/session/SessionService.java:799` |
| `ADVANCED_STATUS_WORKFLOW_REQUIRED` | 400 | `src/main/java/com/sportscenter/membership/MemberPackageService.java:122` |
| `AGREEMENT_REQUIRED` | 400 | `src/main/java/com/sportscenter/session/SessionService.java:230`, `src/main/java/com/sportscenter/session/SessionService.java:719`, `src/main/java/com/sportscenter/session/SessionService.java:848` |
| `ALREADY_CANCELLED` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:192`, `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:231` |
| `ALREADY_ENROLLED` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:126` |
| `ATTENDANCE_WINDOW_CLOSED` | 400 | `src/main/java/com/sportscenter/training/SessionAttendancePolicy.java:10` |
| `BOOKING_BLOCKED` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:107`, `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:151`, `src/main/java/com/sportscenter/session/SessionService.java:262` |
| `CANCEL_TOO_LATE` | 400 | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:275`, `src/main/java/com/sportscenter/session/SessionService.java:400` |
| `CASH_NOTE_TOO_LONG` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:186` |
| `CASH_NOT_RECEIVED_CONFIRMATION_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:76` |
| `CENTER_INCIDENT` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:177` |
| `CENTER_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/ScheduleWriteLock.java:25` |
| `CHECKIN_WINDOW_CLOSED` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:252` |
| `CLASS_ALREADY_STARTED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:611` |
| `CLASS_CAPACITY_EXCEEDED` | chưa quan sát | `src/main/java/com/sportscenter/sportclass/SportsClassService.java:76` |
| `CLASS_HAS_SCHEDULE` | chưa quan sát | `src/main/java/com/sportscenter/sportclass/SportsClassService.java:81`, `src/main/java/com/sportscenter/sportclass/SportsClassService.java:102` |
| `CLASS_NOT_OPEN` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:117` |
| `CLASS_NOT_READY` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:120`, `src/main/java/com/sportscenter/session/NormalSchedulePolicy.java:18` |
| `COACH_ACCEPTANCE_REQUIRED` | 400 | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:88` |
| `COACH_CAPACITY_INSUFFICIENT` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:110` |
| `COACH_CENTER_MISMATCH` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:1101` |
| `COACH_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:314`, `src/main/java/com/sportscenter/session/SessionService.java:779` |
| `COACH_INACTIVE` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:136`, `src/main/java/com/sportscenter/session/SessionService.java:1099` |
| `COACH_NOT_ACCEPTING` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:64` |
| `COACH_NOT_WORKING` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:42` |
| `COACH_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:454`, `src/main/java/com/sportscenter/session/SessionService.java:562`, `src/main/java/com/sportscenter/session/SessionService.java:643`, `src/main/java/com/sportscenter/training/TrainingResultService.java:72` |
| `COACH_SCHEDULE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:63` |
| `COACH_SUBJECT_MISMATCH` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:35`, `src/main/java/com/sportscenter/session/SessionService.java:1105` |
| `COACH_SUBSTITUTED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:897` |
| `COACH_UNAVAILABLE` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:31` |
| `CREDIT_ALREADY_RETURNED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:418` |
| `CREDIT_LIMIT_EXCEEDED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:137`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:164`, `src/main/java/com/sportscenter/session/SessionService.java:419` |
| `CREDIT_LOW` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:359` |
| `EMAIL_ALREADY_REGISTERED` | chưa quan sát | `src/main/java/com/sportscenter/auth/RegistrationService.java:69` |
| `ENROLLMENT_CONFIRMED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:309` |
| `ENTITLEMENT_RECONCILIATION_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:321`, `src/main/java/com/sportscenter/membership/CoachCapacityService.java:68`, `src/main/java/com/sportscenter/membership/EntitlementTimePolicy.java:22`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:86`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:132`, `src/main/java/com/sportscenter/session/SessionService.java:913` |
| `EXPIRY_EVIDENCE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:161` |
| `FINALIZATION_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:676` |
| `HOLIDAY_BLOCKED` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:52`, `src/main/java/com/sportscenter/session/SessionService.java:273` |
| `HOLIDAY_CHANGE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:61` |
| `HOLIDAY_DATE_IMMUTABLE` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:70` |
| `HOLIDAY_DATE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:65` |
| `HOLIDAY_DUPLICATE` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:68` |
| `HOLIDAY_HISTORY_IMMUTABLE` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:69`, `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:71` |
| `HOLIDAY_IMPACT_UNRESOLVED` | 409 | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:42` |
| `HOLIDAY_NAME_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:67` |
| `HOLIDAY_PREVIEW_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:40` |
| `HOLIDAY_PREVIEW_STALE` | 409 | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:41` |
| `HOLIDAY_SCOPE_REVIEW_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:78` |
| `HOLIDAY_TAG_INVALID` | chưa quan sát | `src/main/java/com/sportscenter/holiday/HolidayImpactService.java:72` |
| `IDEMPOTENCY_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:45`, `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:85`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:49`, `src/main/java/com/sportscenter/session/SessionService.java:236`, `src/main/java/com/sportscenter/session/SessionService.java:925`, `src/main/java/com/sportscenter/sportclass/NormalClassCreationService.java:32` |
| `IDEMPOTENCY_KEY_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:153` |
| `INCIDENT_ALREADY_RESOLVED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:77` |
| `INCIDENT_OUTSIDE_ENTITLEMENT` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:60`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:107` |
| `INCIDENT_POLICY_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:680` |
| `INCIDENT_RECOVERY_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:105` |
| `INCIDENT_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:155` |
| `INCIDENT_SESSION_MISMATCH` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:156` |
| `INVALID_ATTENDANCE_STATE` | 400 | `src/main/java/com/sportscenter/training/SessionAttendancePolicy.java:15` |
| `INVALID_CHECKIN_CODE` | 400 | `src/main/java/com/sportscenter/training/AttendanceService.java:220`, `src/main/java/com/sportscenter/training/AttendanceService.java:228` |
| `INVALID_DAYS` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:567` |
| `INVALID_EXTENSION` | chưa quan sát | `src/main/java/com/sportscenter/membership/EntitlementTimePolicy.java:24` |
| `INVALID_INCIDENT_TIME` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:53`, `src/main/java/com/sportscenter/session/CenterIncidentService.java:80` |
| `INVALID_MAKEUP_DEADLINE` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:114` |
| `INVALID_PRICE` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:145`, `src/main/java/com/sportscenter/sportclass/SportsClassService.java:134`, `src/main/java/com/sportscenter/sportclass/SportsClassService.java:145`, `src/main/java/com/sportscenter/subject/SubjectService.java:87` |
| `INVALID_REPORT_GROUP` | chưa quan sát | `src/main/java/com/sportscenter/report/ReportService.java:44` |
| `INVALID_REPORT_RANGE` | chưa quan sát | `src/main/java/com/sportscenter/report/ReportService.java:43` |
| `INVALID_RESCHEDULE_CAUSE` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:734` |
| `INVALID_SLOT` | chưa quan sát | `src/main/java/com/sportscenter/membership/CoachCapacityService.java:40` |
| `INVALID_TIME_SLOT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionSlotValidator.java:17` |
| `INVALID_WORK_SCHEDULE` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:134` |
| `LOBBY_CHECKIN_DISABLED` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:85`, `src/main/java/com/sportscenter/training/AttendanceService.java:110` |
| `MAIL_NOT_CONFIGURED` | chưa quan sát | `src/main/java/com/sportscenter/auth/RegistrationService.java:155` |
| `MAKEUP_ALREADY_CREATED` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:48` |
| `MAKEUP_RESOURCE_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:59` |
| `MAKEUP_TIME_INVALID` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:51` |
| `MEMBER_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:60` |
| `MEMBER_NOT_ACTIVE` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:103`, `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:149`, `src/main/java/com/sportscenter/session/SessionService.java:258`, `src/main/java/com/sportscenter/training/AttendanceService.java:187`, `src/main/java/com/sportscenter/training/AttendanceService.java:231` |
| `MEMBER_PROFILE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:150` |
| `NORMAL_CLASS_NON_CANCELLABLE` | 400 | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:255`, `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:265`, `src/main/java/com/sportscenter/session/SessionAbsenceService.java:31` |
| `NORMAL_CLASS_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:565`, `src/main/java/com/sportscenter/sportclass/NormalClassCreationService.java:37` |
| `NORMAL_MAKEUP` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:66` |
| `NORMAL_MAKEUP_EVIDENCE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:43` |
| `NORMAL_SESSION_COUNT` | chưa quan sát | `src/main/java/com/sportscenter/sportclass/SportsClassService.java:128` |
| `NOT_HELD_CONFIRMATION_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:125` |
| `NOT_YOUR_CLASS` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:242`, `src/main/java/com/sportscenter/training/AttendanceService.java:247`, `src/main/java/com/sportscenter/training/TrainingResultService.java:202`, `src/main/java/com/sportscenter/training/TrainingResultService.java:214` |
| `NO_AFFECTED_ENTITLEMENT` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:66` |
| `NO_SHOW_RECORDED` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceFinalizationProcessor.java:115` |
| `OTP_ATTEMPTS_EXCEEDED` | chưa quan sát | `src/main/java/com/sportscenter/auth/PasswordResetService.java:95`, `src/main/java/com/sportscenter/auth/PasswordResetService.java:102`, `src/main/java/com/sportscenter/auth/RegistrationService.java:108`, `src/main/java/com/sportscenter/auth/RegistrationService.java:116`, `src/main/java/com/sportscenter/auth/RegistrationService.java:121` |
| `OTP_EXPIRED` | chưa quan sát | `src/main/java/com/sportscenter/auth/RegistrationService.java:114` |
| `OTP_INVALID` | chưa quan sát | `src/main/java/com/sportscenter/auth/PasswordResetService.java:103`, `src/main/java/com/sportscenter/auth/RegistrationService.java:109`, `src/main/java/com/sportscenter/auth/RegistrationService.java:111`, `src/main/java/com/sportscenter/auth/RegistrationService.java:121` |
| `OTP_RESEND_LIMIT` | chưa quan sát | `src/main/java/com/sportscenter/auth/PasswordResetService.java:75`, `src/main/java/com/sportscenter/auth/RegistrationService.java:163` |
| `OTP_RESEND_TOO_SOON` | chưa quan sát | `src/main/java/com/sportscenter/auth/PasswordResetService.java:72`, `src/main/java/com/sportscenter/auth/RegistrationService.java:160` |
| `OUTDOOR_HEAT_BLOCK` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionSlotValidator.java:14` |
| `OUTDOOR_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:38` |
| `OUT_OF_CREDIT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:250` |
| `PACKAGE_EXPIRED` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:114`, `src/main/java/com/sportscenter/membership/MemberPackageService.java:211`, `src/main/java/com/sportscenter/training/AttendanceService.java:245` |
| `PACKAGE_EXPIRING_SOON` | chưa quan sát | `src/main/java/com/sportscenter/notification/NotificationScheduleJob.java:62` |
| `PACKAGE_EXTENDED` | chưa quan sát | `src/main/java/com/sportscenter/membership/MemberPackageService.java:177` |
| `PAID_ENTITLEMENT_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:175` |
| `PAID_INVOICE_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:44` |
| `PAYMENT_EXPIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:285` |
| `PAYMENT_RESOLUTION` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:84` |
| `PAYMENT_SUCCESS` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:304` |
| `PRIMARY_COACH_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:256` |
| `PRIVATE_BOOKING_CANCELLED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:436` |
| `PRIVATE_BOOKING_CONFIRMED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:354` |
| `REASON_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/common/ActorPolicy.java:30` |
| `REFUND_RECEIPT_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:71` |
| `REFUND_TIME_INVALID` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:73` |
| `REGISTRATION_PENDING` | chưa quan sát | `src/main/java/com/sportscenter/auth/AuthController.java:67` |
| `REGISTRATION_STATE_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:68` |
| `RESCHEDULE_WINDOW_CLOSED` | 400 | `src/main/java/com/sportscenter/session/SessionService.java:727`, `src/main/java/com/sportscenter/session/SessionService.java:731`, `src/main/java/com/sportscenter/session/SessionService.java:733` |
| `RESOLUTION_APPROVAL_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:67` |
| `RESOLUTION_TYPE_INVALID` | chưa quan sát | `src/main/java/com/sportscenter/invoice/PaymentResolutionService.java:43` |
| `ROOM_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:320`, `src/main/java/com/sportscenter/session/SessionService.java:770` |
| `ROOM_HAS_FUTURE_SESSIONS` | chưa quan sát | `src/main/java/com/sportscenter/room/RoomService.java:101` |
| `ROOM_HAS_SCHEDULE` | chưa quan sát | `src/main/java/com/sportscenter/room/RoomService.java:41` |
| `ROOM_UNAVAILABLE` | chưa quan sát | `src/main/java/com/sportscenter/session/NormalMakeupService.java:57` |
| `SCHEDULE_ALREADY_EXISTS` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:566` |
| `SCHEDULE_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:133` |
| `SESSION_ALREADY_CANCELLED` | 400 | `src/main/java/com/sportscenter/session/CenterIncidentService.java:45`, `src/main/java/com/sportscenter/session/SessionService.java:380` |
| `SESSION_HISTORY_IMMUTABLE` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:395`, `src/main/java/com/sportscenter/session/SessionService.java:852`, `src/main/java/com/sportscenter/session/SessionService.java:942` |
| `SESSION_IN_USE` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:699` |
| `SESSION_NOT_HELD` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:141` |
| `SESSION_REMINDER` | chưa quan sát | `src/main/java/com/sportscenter/notification/NotificationScheduleJob.java:46` |
| `SESSION_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:138`, `src/main/java/com/sportscenter/training/AttendanceService.java:204` |
| `SESSION_RESCHEDULED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:832` |
| `SESSION_TIME_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/training/SessionAttendancePolicy.java:8` |
| `SUBJECT_COACH_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/invoice/InvoiceService.java:56` |
| `SUBJECT_DUPLICATE` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:82` |
| `SUBJECT_FACILITY_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionSlotValidator.java:59` |
| `SUBJECT_HAS_SCHEDULE` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:51` |
| `SUBJECT_IDENTITY_IMMUTABLE` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:77` |
| `SUBJECT_INACTIVE` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:99`, `src/main/java/com/sportscenter/sportclass/SportsClassService.java:114` |
| `SUBJECT_IN_USE` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:61` |
| `SUBJECT_NOT_IN_CATALOG` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:75` |
| `SUBJECT_OUTDOOR_MISMATCH` | chưa quan sát | `src/main/java/com/sportscenter/subject/SubjectService.java:80` |
| `SUBJECT_UNAVAILABLE` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:50`, `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:91` |
| `SUBSCRIPTION_LINK_REQUIRED` | chưa quan sát | `src/main/java/com/sportscenter/session/CenterIncidentService.java:56`, `src/main/java/com/sportscenter/session/SessionService.java:411`, `src/main/java/com/sportscenter/session/SessionService.java:717`, `src/main/java/com/sportscenter/session/SessionService.java:794` |
| `SUBSCRIPTION_SUBJECT_MISMATCH` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:304` |
| `SUBSTITUTE_COACH_CONFLICT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:875` |
| `USE_ABSENCE_NOTICE` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:180` |
| `USE_ADVANCED_REGISTRATION` | chưa quan sát | `src/main/java/com/sportscenter/membership/MemberPackageService.java:76` |
| `USE_INCIDENT_ENDPOINT` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:634` |
| `USE_INCIDENT_RECOVERY` | 400 | `src/main/java/com/sportscenter/membership/MemberPackageService.java:121`, `src/main/java/com/sportscenter/membership/MemberPackageService.java:150`, `src/main/java/com/sportscenter/session/SessionService.java:735` |
| `USE_PRIVATE_BOOKING` | chưa quan sát | `src/main/java/com/sportscenter/session/SessionService.java:448` |
| `USE_PRIVATE_SESSION_WORKFLOW` | 400 | `src/main/java/com/sportscenter/session/SessionService.java:675` |
| `USE_SESSION_ATTENDANCE` | chưa quan sát | `src/main/java/com/sportscenter/training/AttendanceService.java:140` |
| `WAITLIST_FULL` | chưa quan sát | `src/main/java/com/sportscenter/enrollment/EnrollmentService.java:141` |
| `WORK_SCHEDULE_HAS_BOOKINGS` | chưa quan sát | `src/main/java/com/sportscenter/membership/AdvancedRegistrationService.java:140` |
