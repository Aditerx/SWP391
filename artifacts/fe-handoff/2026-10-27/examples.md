# Ví dụ request/response thật và cách FE diễn giải

Nguồn: [http-exchanges.json](http-exchanges.json) từ9workflow HTTP/PostgreSQL local tại commit `693f732ab0e812707625b007f2ee0588f899b318`.
Không phải dữ liệu production. Clock test tiến có kiểm soát từ27/10/2026 tới2027 để kiểm expiry/attendance/refund; URL loopback dùng port ngẫu nhiên. ID môn Cầu lông trong fixture có thể là5, còn production canonical26. Không copy ID/ngày/cookie/mã QR trong tài liệu để gọi Render.
Fixture cố ý đổi giá Cầu lông3000000→3100000 sau đơn đầu để chứng minh price freezing; production không bị sửa giá bởi demo. Cookie/login/OTP/TOTP secret/chữ ký callback không được công bố.

## 1. Member chưa có Coach nhận, order bị chặn

Exchange index **3** (0-based), scenario `acceptanceCashPriceSnapshotAndIdempotency`. Actor `MEMBER_A`, businessNow `2026-10-27T08:00` UTC+7.

`POST /api/advanced-registrations/1/order` → HTTP **400**.

Chưa accept thì chưa có Invoice/quyền mới. FE hiển thị COACH_ACCEPTANCE_REQUIRED, không tự chuyển sang thanh toán.

Request body:
```json
{
  "paymentMethod": "Cash"
}
```

Response body (đủ field ở exchange này):
```json
{
  "code": "COACH_ACCEPTANCE_REQUIRED",
  "message": "Coach must accept before an invoice can be issued"
}
```

## 2. Order Cash Pending, giữ5phút

Exchange index **5** (0-based), scenario `acceptanceCashPriceSnapshotAndIdempotency`. Actor `MEMBER_A`, businessNow `2026-10-27T08:00` UTC+7.

`POST /api/advanced-registrations/1/order` → HTTP **200**.

amount là snapshot3000000VND; expiresAt là hạn trả tiền, không phải45ngày sử dụng. paymentUrl null là đúng với Cash.

Request body:
```json
{
  "paymentMethod": "Cash"
}
```

Response body (đủ field ở exchange này):
```json
{
  "subscriptionId": 1,
  "invoiceId": 1,
  "invoiceCode": "INV-2026-000001",
  "amount": 3000000.0,
  "paymentMethod": "Cash",
  "status": "Pending",
  "expiresAt": "2026-10-27T08:05:00",
  "paymentUrl": null
}
```

## 3. Receptionist xác nhận Cash Paid

Exchange index **9** (0-based), scenario `acceptanceCashPriceSnapshotAndIdempotency`. Actor `RECEPTIONIST`, businessNow `2026-10-27T08:00` UTC+7.

`POST /api/invoices/1/confirm-cash` → HTTP **200**.

Invoice.amount vẫn3000000 dù fixture đã đổi Subject lên3100000. Workflow đồng thời assertDB Active/12credit và deadline2026-12-12T08:00:00 (có1ngày nghỉ TT).

Response body (đủ field ở exchange này):
```json
{
  "invoiceId": 1,
  "memberId": 1,
  "memberName": "phase.member",
  "memberEmail": "phase.member@example.test",
  "memberPhone": null,
  "packageId": null,
  "packageName": null,
  "durationDays": null,
  "receptionistId": 5,
  "receptionistName": "phase.cashier",
  "amount": 3000000.0,
  "paymentMethod": "Cash",
  "paymentStatus": "Paid",
  "paymentDate": "2026-10-27T08:00:00",
  "gatewayTransactionRef": null,
  "invoiceCode": "INV-2026-000001",
  "createdAt": "2026-10-27T08:00:00",
  "expiresAt": "2026-10-27T08:05:00",
  "classId": null,
  "className": null,
  "reconciliationRequired": false,
  "cashNote": null,
  "resolutionStatus": null,
  "cashConfirmationReversed": false
}
```

## 4. Member đọc quyền sau Paid

Exchange index **11** (0-based), scenario `acceptanceCashPriceSnapshotAndIdempotency`. Actor `MEMBER_A`, businessNow `2026-10-27T08:00` UTC+7.

`GET /api/members/1/subscriptions` → HTTP **200**.

Response thật gồm price đóng băng, subjectId/primaryCoachId, activatedAt/expiresAt. ID/giờ là fixture local, không hardcode vào FE.

Response body (đủ field ở exchange này):
```json
[
  {
    "subscriptionId": 1,
    "memberId": 1,
    "memberName": "phase.member",
    "packageId": null,
    "packageName": "Cầu lông Advanced",
    "price": 3000000.0,
    "startDate": "2026-10-27",
    "endDate": "2026-12-12",
    "status": "Active",
    "packageType": "Advanced",
    "remainingCredit": 12,
    "validityDays": 45,
    "subjectId": 5,
    "primaryCoachId": 2,
    "activatedAt": "2026-10-27T08:00:00",
    "expiresAt": "2026-12-12T08:00:00"
  }
]
```

## 5. Member khác không đọc được Invoice

Exchange index **7** (0-based), scenario `acceptanceCashPriceSnapshotAndIdempotency`. Actor `MEMBER_B`, businessNow `2026-10-27T08:00` UTC+7.

`GET /api/members/me/invoices/1` → HTTP **404**.

Cùng URL invoice nhưng phiên MEMBER_B bị404. Dữ liệu không được chia sẻ chỉ vì FE biết invoiceId.

Response body (đủ field ở exchange này):
```json
{
  "timestamp": "2026-10-27T15:11:20.914363200Z",
  "status": 404,
  "error": "Not Found",
  "message": "Invoice not found: 1"
}
```

## 6. Member tự tạo Advanced bị403

Exchange index **38** (0-based), scenario `primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit`. Actor `MEMBER_A`, businessNow `2026-10-27T08:05` UTC+7.

`POST /api/sessions/book-private` → HTTP **403**.

Ẩn nút tạo đối với Member không thay kiểm quyền BE; server thực sự từ chối.

Request body:
```json
{
  "agreementConfirmed": true,
  "requestKey": "book-boundary",
  "sessionDate": "2026-10-28",
  "startTime": "06:00",
  "endTime": "07:30",
  "subscriptionId": 1,
  "coachId": 2,
  "roomId": 1,
  "subjectId": 5
}
```

Response body (đủ field ở exchange này):
```json
{
  "timestamp": "2026-10-27T15:11:23.677938200Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

## 7. Coach đặt ca có consent,201

Exchange index **40** (0-based), scenario `primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit`. Actor `PRIMARY_COACH`, businessNow `2026-10-27T08:05` UTC+7.

`POST /api/sessions/book-private` → HTTP **201**.

SessionResponse không có remainingCredit; workflow assert12→11 và đọc lại subscriptions. Cùng requestKey/body retry trả đúng session cũ, không trừ lần hai.

Request body:
```json
{
  "agreementConfirmed": true,
  "requestKey": "book-boundary",
  "sessionDate": "2026-10-28",
  "startTime": "06:00",
  "endTime": "07:30",
  "subscriptionId": 1,
  "coachId": 2,
  "roomId": 1,
  "subjectId": 5
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "classId": 1,
  "className": "Phase Advanced template",
  "subjectId": 5,
  "subjectName": "Cầu lông",
  "coachId": 2,
  "coachName": "phase.coach",
  "roomId": 1,
  "roomName": "Phase test room",
  "roomLocation": null,
  "sessionDate": "2026-10-28",
  "startTime": "06:00:00",
  "endTime": "07:30:00",
  "status": "Scheduled",
  "enrolledCount": 1,
  "maxCapacity": 1,
  "bookedMemberId": 1,
  "bookedMemberName": "phase.member",
  "isPrivate": true,
  "centerInvalidated": false,
  "makeupForSessionId": null,
  "creditReturned": false,
  "createdBy": 2,
  "agreementConfirmedAt": "2026-10-27T08:05:00"
}
```

## 8. Hủy đúng12h, hoàn1credit

Exchange index **45** (0-based), scenario `primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit`. Actor `MEMBER_A`, businessNow `2026-10-27T18:00` UTC+7.

`POST /api/sessions/1/cancel-private` → HTTP **200**.

Tại giờ nghiệp vụ trong record, ca cách đúng12h. Workflow assert11→12; lần hủy lặp bị lỗi và không hoàn thêm.

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "classId": 1,
  "className": "Phase Advanced template",
  "subjectId": 5,
  "subjectName": "Cầu lông",
  "coachId": 2,
  "coachName": "phase.coach",
  "roomId": 1,
  "roomName": "Phase test room",
  "roomLocation": null,
  "sessionDate": "2026-10-28",
  "startTime": "06:00:00",
  "endTime": "07:30:00",
  "status": "Cancelled",
  "enrolledCount": 0,
  "maxCapacity": 1,
  "bookedMemberId": 1,
  "bookedMemberName": "phase.member",
  "isPrivate": true,
  "centerInvalidated": false,
  "makeupForSessionId": null,
  "creditReturned": true,
  "createdBy": 2,
  "agreementConfirmedAt": "2026-10-27T08:05:00"
}
```

## 9. Báo vắng sát giờ tách attendance

Exchange index **49** (0-based), scenario `primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit`. Actor `MEMBER_A`, businessNow `2026-10-28T00:00` UTC+7.

`POST /api/sessions/2/absence` → HTTP **200**.

Notice.active=true không đồng nghĩa đã điểm danh. Workflow assert credit vẫn11, bảng attendance chưa có record do notice.

Request body:
```json
{
  "reason": "Unexpected personal issue"
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "sessionId": 2,
  "memberId": 1,
  "reportedAt": "2026-10-28T00:00:00",
  "reason": "Unexpected personal issue",
  "active": true,
  "supersededAt": null
}
```

## 10. Holiday preview cho thấy ảnh hưởng

Exchange index **56** (0-based), scenario `holidayPreviewRejectsUnresolvedAndStaleChangesAndUnionsIncidentTime`. Actor `CENTER_MANAGER`, businessNow `2026-10-28T00:00` UTC+7.

`POST /api/holidays/preview` → HTTP **200**.

FE hiển thị affectedSessionIds/deadlineChanges, yêu cầu xử lý lịch rồi preview lại. X-Preview-Token phải lấy từ preview mới, không tự tạo.

Request body:
```json
{
  "tag": "CENTER_EVENT",
  "holidayName": "Phase planned closure",
  "holidayDate": "2026-10-29",
  "active": true
}
```

Response body (đủ field ở exchange này):
```json
{
  "previewToken": "5a02c1f8683379f4569d23ee2d9c2aaaf1649a9aa9c960775601eb9b2d5b017b",
  "blocked": true,
  "affectedSessionIds": [
    2
  ],
  "reconciliationSubscriptionIds": [],
  "deadlineChanges": [
    {
      "subscriptionId": 2,
      "oldDeadline": "2026-12-12T08:00:00",
      "newDeadline": "2026-12-13T08:00:00"
    },
    {
      "subscriptionId": 1,
      "oldDeadline": "2026-12-12T08:00:00",
      "newDeadline": "2026-12-13T08:00:00"
    }
  ]
}
```

## 11. QR có TTL hiện tại

Exchange index **72** (0-based), scenario `qrAttendanceHonorsExactWindowReplayAndManagerCorrection`. Actor `MEMBER_A`, businessNow `2026-10-31T05:59:59` UTC+7.

`GET /api/attendances/me/checkin-code` → HTTP **200**.

payload đã redact; expiresInSeconds là TTL thực tại thời điểm test. Mã trong tài liệu không dùng để scan. Không chứa TOTP secret hoặc cookie.

Response body (đủ field ở exchange này):
```json
{
  "payload": "[redacted]",
  "expiresInSeconds": 1
}
```

## 12. Scan ngoài giờ ca bị chặn

Exchange index **73** (0-based), scenario `qrAttendanceHonorsExactWindowReplayAndManagerCorrection`. Actor `PRIMARY_COACH`, businessNow `2026-10-31T05:59:59` UTC+7.

`POST /api/attendances/scan` → HTTP **400**.

Biên hợp lệ là[start,end); trước start và tại end đều bị chặn. Quyền Coach/phân công và QR/replay còn được kiểm riêng.

Request body:
```json
{
  "memberId": 1,
  "sessionId": 2,
  "code": "[redacted]"
}
```

Response body (đủ field ở exchange này):
```json
{
  "code": "ATTENDANCE_WINDOW_CLOSED",
  "message": "Attendance is allowed from session start until before session end"
}
```

## 13. Đọc lại tiền muộn cần đối soát

Exchange index **30** (0-based), scenario `deadlineAndLateIpnRequireReconciliationWithoutEntitlement`. Actor `MEMBER_A`, businessNow `2026-10-27T08:05` UTC+7.

`GET /api/members/me/invoices/3` → HTTP **200**.

Paid có thể đi kèm reconciliationRequired=true và0credit/Cancelled trong DB. FE hiển thị đã ghi nhận tiền/cần xử lý, không coi gói Active.

Response body (đủ field ở exchange này):
```json
{
  "invoiceId": 3,
  "memberId": 1,
  "memberName": "phase.member",
  "memberEmail": "phase.member@example.test",
  "memberPhone": null,
  "packageId": null,
  "packageName": null,
  "durationDays": null,
  "receptionistId": null,
  "receptionistName": null,
  "amount": 3100000.0,
  "paymentMethod": "VNPay",
  "paymentStatus": "Paid",
  "paymentDate": "2026-10-27T08:05:00",
  "gatewayTransactionRef": "LOCAL-3",
  "invoiceCode": "INV-2026-000003",
  "createdAt": "2026-10-27T08:00:00",
  "expiresAt": "2026-10-27T08:05:00",
  "classId": null,
  "className": null,
  "reconciliationRequired": true,
  "cashNote": null,
  "resolutionStatus": null,
  "cashConfirmationReversed": false
}
```

## 14. Sự cố WaitingForRecovery

Exchange index **51** (0-based), scenario `primaryCoachBooksAndMemberCancellationAtTwelveHoursReturnsOneCredit`. Actor `CENTER_MANAGER`, businessNow `2026-10-28T00:00` UTC+7.

`POST /api/center-incidents` → HTTP **200**.

Chưa biết kết thúc thì chưa gia hạn tạm. Manager resolve bằng endedAt thực; phần thời gian đã được Holiday bù không cộng lại.

Request body:
```json
{
  "reason": "Room issue",
  "evidence": "Verified maintenance record",
  "startedAt": "2026-10-28T00:00:00",
  "sessionId": 2,
  "requestKey": "room-incident"
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "sessionId": 2,
  "status": "WaitingForRecovery",
  "startedAt": "2026-10-28T00:00:00",
  "endedAt": null,
  "originalDeadline": "2026-12-12T08:00:00",
  "deadlineBefore": null,
  "deadlineAfter": null,
  "recoveredCredits": 0,
  "sessionCompensated": false,
  "affectedMemberIds": [
    1
  ],
  "reason": "Room issue",
  "evidence": "Verified maintenance record"
}
```

## 15. Bù có hồ sơ, chống làm lặp

Exchange index **127** (0-based), scenario `incidentWaitingThroughExpiryRestoresThreeRevokedCreditsAndOneCancelledSessionExactlyOnce`. Actor `CENTER_MANAGER`, businessNow `2026-12-14T08:00` UTC+7.

`POST /api/center-incidents/3/compensate` → HTTP **200**.

Workflow8 kiểm restore đúng3credit expiry và1credit gốc của ca bị TT hủy; retry không nhân. Workflow9 giữ buổi gốc và thêm1makeup liên kết để đủ12buổi hợp lệ.

Request body:
```json
{
  "verifiedNotHeld": true,
  "reason": "Verified session was not held at all"
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 3,
  "sessionId": 11,
  "status": "Resolved",
  "startedAt": "2026-11-08T08:00:00",
  "endedAt": "2026-12-14T08:00:00",
  "originalDeadline": "2026-12-14T08:00:00",
  "deadlineBefore": "2026-12-14T08:00:00",
  "deadlineAfter": "2027-01-19T08:00:00",
  "recoveredCredits": 3,
  "sessionCompensated": true,
  "affectedMemberIds": [
    1
  ],
  "reason": "Verified service interruption",
  "evidence": "LOCAL TEST closure report"
}
```

## 16. Refund Approved chưa là Completed

Exchange index **86** (0-based), scenario `externalRefundAndNarrowCashCorrectionKeepOriginalReceiptsAndMoneyPeriods`. Actor `CENTER_MANAGER`, businessNow `2026-11-01T09:00` UTC+7.

`POST /api/payment-resolutions/1/approve` → HTTP **200**.

Approve chưa tăng moneyRefunded. Hoàn ngoàiSCMS thật, có evidence/performedAt rồi complete mới ghi kết quả.

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "invoiceId": 3,
  "type": "Refund",
  "status": "Approved",
  "amount": 3100000.0,
  "reason": "Verified payment after hold expiry",
  "evidence": null,
  "outcomeNote": null,
  "createdAt": "2026-11-01T09:00:00",
  "approvedAt": "2026-11-01T09:00:00",
  "performedAt": null,
  "completedAt": null
}
```

## 17. Refund Completed có chứng cứ

Exchange index **88** (0-based), scenario `externalRefundAndNarrowCashCorrectionKeepOriginalReceiptsAndMoneyPeriods`. Actor `CENTER_MANAGER`, businessNow `2026-11-01T09:00` UTC+7.

`POST /api/payment-resolutions/1/complete` → HTTP **200**.

amount lấy Invoice gốc; performedAt là kỳ hoàn thực tế, có thể khác kỳ thu. API không tự gọi gateway refund.

Request body:
```json
{
  "evidence": "LOCAL TEST external refund receipt R1",
  "performedAt": "2026-11-01T09:00:00",
  "moneyNotReceived": false
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 1,
  "invoiceId": 3,
  "type": "Refund",
  "status": "Completed",
  "amount": 3100000.0,
  "reason": "Verified payment after hold expiry",
  "evidence": "LOCAL TEST external refund receipt R1",
  "outcomeNote": "Full refund performed outside SCMS",
  "createdAt": "2026-11-01T09:00:00",
  "approvedAt": "2026-11-01T09:00:00",
  "performedAt": "2026-11-01T09:00:00",
  "completedAt": "2026-11-01T09:00:00"
}
```

## 18. Normal tạo lớp và lịch nguyên khối

Exchange index **135** (0-based), scenario `normalPaidCourseLateIncidentCreatesOneLinkedMakeupAndCompletesTwelveValidSessions`. Actor `CENTER_MANAGER`, businessNow `2026-12-14T08:00` UTC+7.

`POST /api/classes/normal-with-sessions` → HTTP **201**.

Trong cùng transaction tạo12buổi, rollback khi conflict. RequestId dùng retry cùng nội dung; Member không được hủy Normal vì nghỉ cá nhân.

Request body:
```json
{
  "roomId": 1,
  "startTime": "07:45",
  "endTime": "09:15",
  "requestId": "2af3ec2b-56f8-47ab-a79b-018e469383bd",
  "daysOfWeek": [
    2,
    4
  ],
  "sportsClass": {
    "totalSessions": 12,
    "endDate": "2027-02-01",
    "name": "Phase Normal twelve sessions",
    "maxCapacity": 15,
    "coachId": 2,
    "subjectId": 5,
    "classType": "Normal",
    "startDate": "2026-12-17",
    "status": "Open"
  }
}
```

Response body (đủ field ở exchange này):
```json
{
  "id": 2,
  "name": "Phase Normal twelve sessions",
  "subjectId": 5,
  "subjectName": "Cầu lông",
  "coachId": 2,
  "coachName": "phase.coach",
  "maxCapacity": 15,
  "tuitionFee": 900000.0,
  "startDate": "2026-12-17",
  "endDate": "2027-01-26",
  "status": "Open",
  "enrolledCount": 0,
  "classType": "Normal",
  "totalSessions": 12
}
```

## 19. Báo cáo thu/hoàn/ròng khác gross

Exchange index **113** (0-based), scenario `externalRefundAndNarrowCashCorrectionKeepOriginalReceiptsAndMoneyPeriods`. Actor `CENTER_MANAGER`, businessNow `2026-11-01T09:00` UTC+7.

`GET /api/reports/revenue?startDate=2026-11-01&endDate=2026-11-30` → HTTP **200**.

Demo tháng11: gross6200000, thu3100000, hoàn3100000, ròng0, điều chỉnh Cash3100000. Không dùng totalRevenue thay netReceived.

Response body (đủ field ở exchange này):
```json
{
  "totalRevenue": 6200000.0,
  "totalTransactions": 2,
  "averageTransactionValue": 3100000,
  "revenueByPackage": [
    {
      "packageId": null,
      "packageName": "Cầu lông Advanced",
      "count": 2,
      "totalRevenue": 6200000.0
    }
  ],
  "revenueByMethod": [
    {
      "method": "Cash",
      "count": 2,
      "totalRevenue": 6200000.0
    }
  ],
  "timeline": [
    {
      "period": "2026-11",
      "revenue": 6200000.0,
      "count": 2
    }
  ],
  "moneyReceived": 3100000.0,
  "moneyRefunded": 3100000.0,
  "netReceived": 0.0,
  "cashCorrectionsRecorded": 3100000.0
}
```

## 20. Member report sau hoàn tất Normal

Exchange index **148** (0-based), scenario `normalPaidCourseLateIncidentCreatesOneLinkedMakeupAndCompletesTwelveValidSessions`. Actor `CENTER_MANAGER`, businessNow `2027-01-28T09:15` UTC+7.

`GET /api/reports/members` → HTTP **200**.

Hai Member:1người đã hết quyền và1người chưa từng có quyền. completedRegistrations=1 tách expiredRegistrations; không nhân số người theo số đăng ký.

Response body (đủ field ở exchange này):
```json
{
  "totalMembers": 2,
  "activeMembers": 0,
  "expiredMembers": 1,
  "packageDistribution": [],
  "monthlyGrowth": [
    {
      "month": "2026-10",
      "count": 2
    }
  ],
  "neverEntitledMembers": 1,
  "previouslyEntitledMembersWithoutRights": 1,
  "expiredRegistrations": 3,
  "completedRegistrations": 1,
  "reconciliationRegistrations": 0,
  "reconciliationMembers": 0
}
```
