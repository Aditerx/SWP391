# Schema request/response từ OpenAPI Live

Đây là schema công bố, không phải response đã gọi. Required/nullable lấy nguyên snapshot; nullable không khai báo không có nghĩa response thực không thể null. Đối chiếu thêm DTO/trace trong contract chính.

## AbsenceRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| reason | string | True |  |

## AdvancedRegistrationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subjectId | integer(int32) | True |  |
| primaryCoachId | integer(int32) | True |  |
| requestKey | string | True |  |

## AdvancedRegistrationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| subjectId | integer(int32) | False |  |
| primaryCoachId | integer(int32) | False |  |
| status | string | False |  |
| createdAt | string(date-time) | False |  |
| acceptedBy | integer(int32) | False |  |
| acceptedAt | string(date-time) | False |  |
| reason | string | False |  |
| subscriptionId | integer(int32) | False |  |
| memberName | string | False |  |
| subjectName | string | False |  |
| primaryCoachName | string | False |  |
| subscriptionStatus | string | False |  |
| remainingCredit | integer(int32) | False |  |
| activatedAt | string(date-time) | False |  |
| expiresAt | string(date-time) | False |  |

## ApplicationContext

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| autowireCapableBeanFactory | AutowireCapableBeanFactory | False |  |
| applicationName | string | False |  |
| startupDate | integer(int64) | False |  |
| parent | ApplicationContext | False |  |
| id | string | False |  |
| displayName | string | False |  |
| environment | Environment | False |  |
| beanDefinitionCount | integer(int32) | False |  |
| beanDefinitionNames | array<string> | False |  |
| parentBeanFactory | BeanFactory | False |  |
| classLoader | object | False |  |

## AssignCoachRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| coachId | integer(int32) | True |  |

## AtRiskMemberResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| email | string | False |  |

## AttendanceCorrectionRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| attendanceId | integer(int32) | True |  |
| newState | string | True |  |
| reason | string | True |  |

## AttendanceResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| attendanceId | integer(int32) | False |  |
| sessionId | integer(int32) | False |  |
| sessionDate | string(date) | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| memberCode | string | False |  |
| recordedBy | integer(int32) | False |  |
| recordedByName | string | False |  |
| state | string | False |  |
| checkInTime | string(date-time) | False |  |
| checkOutTime | string(date-time) | False |  |

## AutowireCapableBeanFactory

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| (schema) | object | | {"type": "object"} |

## AvatarResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| avatarUrl | string | False |  |

## BeanFactory

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| (schema) | object | | {"type": "object"} |

## BookingBlockLiftRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| reason | string | True |  |

## BulkTrainingResultRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| sessionId | integer(int32) | True |  |
| coachId | integer(int32) | False |  |
| items | array<Item> | False |  |

## Capacity

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| availableSlots | integer(int32) | False |  |
| outstandingCredits | integer(int32) | False |  |
| requestedCredits | integer(int32) | False |  |

## CashNote

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| note | string | False |  |

## CatalogSessionResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| sessionDate | string(date) | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| coachName | string | False |  |
| roomName | string | False |  |

## CenterIncidentOpenRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| sessionId | integer(int32) | True |  |
| startedAt | string(date-time) | True |  |
| reason | string | True |  |
| evidence | string | True |  |
| requestKey | string | True |  |

## CenterIncidentResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| sessionId | integer(int32) | False |  |
| status | string | False |  |
| startedAt | string(date-time) | False |  |
| endedAt | string(date-time) | False |  |
| originalDeadline | string(date-time) | False |  |
| deadlineBefore | string(date-time) | False |  |
| deadlineAfter | string(date-time) | False |  |
| recoveredCredits | integer(int32) | False |  |
| sessionCompensated | boolean | False |  |
| affectedMemberIds | array<integer(int32)> | False | {"uniqueItems": true} |
| reason | string | False |  |
| evidence | string | False |  |

## Change

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| holidayId | integer(int32) | False |  |
| holidayDate | string(date) | False |  |
| holidayName | string | False |  |
| tag | string | False |  |
| active | boolean | False |  |

## ChangePasswordRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| oldPassword | string | True |  |
| newPassword | string | True |  |

## ChatConversationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subject | string | False | {"maxLength": 200, "minLength": 0} |

## ChatConversationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| memberUserId | integer(int32) | False |  |
| memberName | string | False |  |
| status | string | False | {"enum": ["OPEN", "CLOSED"]} |
| assignedStaffUserId | integer(int32) | False |  |
| assignedStaffName | string | False |  |
| handlerType | string | False | {"enum": ["STAFF", "AI"]} |
| subject | string | False |  |
| createdAt | string(date-time) | False |  |
| updatedAt | string(date-time) | False |  |
| lastMessageAt | string(date-time) | False |  |
| closedAt | string(date-time) | False |  |
| lastMessage | ChatMessageResponse | False |  |
| unreadCount | integer(int64) | False |  |

## ChatMessageRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| content | string | False |  |
| clientMessageId | string | False |  |

## ChatMessageResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| conversationId | integer(int32) | False |  |
| senderType | string | False | {"enum": ["MEMBER", "STAFF", "AI", "SYSTEM"]} |
| senderName | string | False |  |
| isMine | boolean | False |  |
| content | string | False |  |
| contentType | string | False |  |
| metadata | string | False |  |
| clientMessageId | string | False |  |
| createdAt | string(date-time) | False |  |

## ChatUnreadCountResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| unreadCount | integer(int64) | False |  |

## CheckInRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | True |  |
| notes | string | False |  |

## CheckinCodeResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| payload | string | False |  |
| expiresInSeconds | integer(int32) | False |  |

## CheckinScanRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | True |  |
| code | string | True |  |
| sessionId | integer(int32) | False |  |

## ClassFeedbackRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | True | {"minimum": 1} |
| coachId | integer(int32) | True | {"minimum": 1} |
| classRating | integer(int32) | True | {"maximum": 5, "minimum": 1} |
| coachRating | integer(int32) | True | {"maximum": 5, "minimum": 1} |
| comment | string | False | {"maxLength": 1000, "minLength": 0} |

## ClassFeedbackResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| feedbackId | integer(int32) | False |  |
| classId | integer(int32) | False |  |
| coachId | integer(int32) | False |  |
| classRating | integer(int32) | False |  |
| coachRating | integer(int32) | False |  |
| comment | string | False |  |
| createdAt | string(date-time) | False |  |

## ClassFeedbackReviewResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| feedbackId | integer(int32) | False |  |
| memberName | string | False |  |
| classRating | integer(int32) | False |  |
| coachRating | integer(int32) | False |  |
| comment | string | False |  |
| createdAt | string(date-time) | False |  |

## ClassFeedbackSummaryResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | False |  |
| totalFeedback | integer(int64) | False |  |
| averageClassRating | number | False |  |
| averageCoachRating | number | False |  |
| feedback | array<ClassFeedbackReviewResponse> | False |  |

## CoachAvailableSlotResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| slotNumber | integer(int32) | False |  |
| slotName | string | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| isAvailable | boolean | False |  |
| reason | string | False |  |

## CoachFeedbackReviewResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | False |  |
| className | string | False |  |
| classRating | integer(int32) | False |  |
| coachRating | integer(int32) | False |  |
| comment | string | False |  |
| createdAt | string(date-time) | False |  |

## CoachFeedbackSummaryResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalFeedback | integer(int64) | False |  |
| averageCoachRating | number | False |  |
| feedback | array<CoachFeedbackReviewResponse> | False |  |

## CoachSummaryResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| avatarUrl | string | False |  |
| specializations | array<string> | False |  |
| email | string | False |  |
| phone | string | False |  |
| bio | string | False |  |
| certification | string | False |  |
| status | string | False |  |

## Compensate

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| reason | string | True |  |
| verifiedNotHeld | boolean | False |  |

## Complete

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| evidence | string | True |  |
| performedAt | string(date-time) | False |  |
| moneyNotReceived | boolean | False |  |

## ConflictCheckRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | False |  |
| coachId | integer(int32) | False |  |
| roomId | integer(int32) | False |  |
| sessionDate | string(date) | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| excludeSessionId | integer(int32) | False |  |

## ConflictCheckResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| hasConflict | boolean | False |  |
| roomConflict | boolean | False |  |
| coachConflict | boolean | False |  |
| message | string | False |  |
| conflictDetails | array<string> | False |  |

## DashboardResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalUsers | integer(int64) | False |  |
| totalMembers | integer(int64) | False |  |
| totalClasses | integer(int64) | False |  |
| totalPackages | integer(int64) | False |  |
| totalRevenue | number | False |  |
| totalInvoices | integer(int64) | False |  |
| activeSubscriptions | integer(int64) | False |  |
| todaySessions | integer(int64) | False |  |
| moneyReceived | number | False |  |
| moneyRefunded | number | False |  |
| netReceived | number | False |  |

## DeadlineChange

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subscriptionId | integer(int32) | False |  |
| oldDeadline | string(date-time) | False |  |
| newDeadline | string(date-time) | False |  |

## Decision

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| accepted | boolean | False |  |
| reason | string | False |  |

## EnrollmentRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | False |  |
| classId | integer(int32) | False |  |
| paymentMethod | string | False |  |
| reason | string | False |  |

## EnrollmentResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| memberCode | string | False |  |
| memberEmail | string | False |  |
| memberPhone | string | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| subjectName | string | False |  |
| coachName | string | False |  |
| enrolledAt | string(date-time) | False |  |
| status | string | False |  |
| invoiceId | integer(int32) | False |  |
| invoiceCode | string | False |  |
| paymentStatus | string | False |  |
| paymentUrl | string | False |  |
| waitlistPosition | integer(int32) | False |  |
| amount | number | False |  |
| expiresAt | string(date-time) | False |  |

## Environment

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| activeProfiles | array<string> | False |  |
| defaultProfiles | array<string> | False |  |

## EvaluationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | True |  |
| coachId | integer(int32) | True |  |
| evaluationDate | string(date) | False |  |
| comment | string | False |  |
| progressScore | number | False | {"maximum": 10.0, "exclusiveMaximum": false, "minimum": 0.0, "exclusiveMinimum": false} |

## EvaluationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| evaluationId | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| memberCode | string | False |  |
| coachId | integer(int32) | False |  |
| coachName | string | False |  |
| evaluationDate | string(date) | False |  |
| comment | string | False |  |
| progressScore | number | False |  |

## FilterRegistration

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| servletNameMappings | array<string> | False |  |
| urlPatternMappings | array<string> | False |  |
| initParameters | object/map | False | {"additionalProperties": {"type": "string"}} |
| name | string | False |  |
| className | string | False |  |

## ForgotPasswordRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| email | string | True |  |

## HolidayRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| holidayDate | string(date) | True |  |
| holidayName | string | True |  |
| tag | string | False |  |
| active | boolean | False |  |

## HolidayResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| holidayDate | string(date) | False |  |
| holidayName | string | False |  |
| tag | string | False |  |
| active | boolean | False |  |

## HttpStatusCode

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| is4xxClientError | boolean | False |  |
| is5xxServerError | boolean | False |  |
| is1xxInformational | boolean | False |  |
| is2xxSuccessful | boolean | False |  |
| is3xxRedirection | boolean | False |  |
| error | boolean | False |  |

## InvoiceResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| invoiceId | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| memberEmail | string | False |  |
| memberPhone | string | False |  |
| packageId | integer(int32) | False |  |
| packageName | string | False |  |
| durationDays | integer(int32) | False |  |
| receptionistId | integer(int32) | False |  |
| receptionistName | string | False |  |
| amount | number | False |  |
| paymentMethod | string | False |  |
| paymentStatus | string | False |  |
| paymentDate | string(date-time) | False |  |
| gatewayTransactionRef | string | False |  |
| invoiceCode | string | False |  |
| createdAt | string(date-time) | False |  |
| expiresAt | string(date-time) | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| reconciliationRequired | boolean | False |  |
| cashNote | string | False |  |
| resolutionStatus | string | False |  |
| cashConfirmationReversed | boolean | False |  |

## Item

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| memberId | integer(int32) | False |  |
| attendanceStatus | string | False |  |
| content | string | False |  |
| reason | string | False |  |

## JspConfigDescriptor

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| jspPropertyGroups | array<JspPropertyGroupDescriptor> | False |  |
| taglibs | array<TaglibDescriptor> | False |  |

## JspPropertyGroupDescriptor

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| elIgnored | string | False |  |
| pageEncoding | string | False |  |
| errorOnELNotFound | string | False |  |
| scriptingInvalid | string | False |  |
| deferredSyntaxAllowedAsLiteral | string | False |  |
| trimDirectiveWhitespaces | string | False |  |
| errorOnUndeclaredNamespace | string | False |  |
| isXml | string | False |  |
| includePreludes | array<string> | False |  |
| includeCodas | array<string> | False |  |
| defaultContentType | string | False |  |
| urlPatterns | array<string> | False |  |
| buffer | string | False |  |

## LocalTime

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| (schema) | string | | {"pattern": "^([01]\\d\|2[0-3]):[0-5]\\d(?::[0-5]\\d(?:\\.\\d{1,9})?)?$", "type": "string", "description": "Local wall-clock time in Asia/Ho_Chi_Minh, without an offset; HH:mm or HH:mm:ss[.fraction]", "example": "07:45:00"} |

## LoginRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| email | string | True |  |
| password | string | True |  |

## LoginResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| userId | integer(int32) | False |  |
| email | string | False |  |
| fullName | string | False |  |
| role | string | False |  |
| authorities | array<string> | False | {"uniqueItems": true} |
| isFirstLogin | boolean | False |  |
| memberId | integer(int32) | False |  |

## MemberPackageRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| packageId | integer(int32) | True |  |
| startDate | string(date) | False |  |
| durationDays | integer(int32) | False |  |
| isRenewal | boolean | False |  |
| paymentMethod | string | False |  |
| notes | string | False |  |

## MemberPackageResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subscriptionId | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| packageId | integer(int32) | False |  |
| packageName | string | False |  |
| price | number | False |  |
| startDate | string(date) | False |  |
| endDate | string(date) | False |  |
| status | string | False |  |
| packageType | string | False |  |
| remainingCredit | integer(int32) | False |  |
| validityDays | integer(int32) | False |  |
| subjectId | integer(int32) | False |  |
| primaryCoachId | integer(int32) | False |  |
| activatedAt | string(date-time) | False |  |
| expiresAt | string(date-time) | False |  |

## MemberReportResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalMembers | integer(int64) | False |  |
| activeMembers | integer(int64) | False |  |
| expiredMembers | integer(int64) | False |  |
| packageDistribution | array<PackageDistributionItem> | False |  |
| monthlyGrowth | array<MonthlyMemberGrowthItem> | False |  |
| neverEntitledMembers | integer(int64) | False |  |
| previouslyEntitledMembersWithoutRights | integer(int64) | False |  |
| expiredRegistrations | integer(int64) | False |  |
| completedRegistrations | integer(int64) | False |  |
| reconciliationRegistrations | integer(int64) | False |  |
| reconciliationMembers | integer(int64) | False |  |

## MemberRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| email | string | True |  |
| phone | string | False |  |
| password | string | False |  |
| packageId | integer(int32) | False |  |
| goal | string | False |  |
| healthNote | string | False |  |
| address | string | False |  |
| gender | string | False |  |
| dateOfBirth | string(date) | False |  |
| status | string | False |  |
| centerId | integer(int32) | False |  |

## MemberResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| code | string | False |  |
| name | string | False |  |
| email | string | False |  |
| phone | string | False |  |
| joinDate | string(date) | False |  |
| currentPackageId | integer(int32) | False |  |
| currentPackageName | string | False |  |
| membershipStatus | string | False |  |
| primaryCoachId | integer(int32) | False |  |
| primaryCoachName | string | False |  |
| totalSpent | number | False |  |
| goal | string | False |  |
| healthNote | string | False |  |
| status | string | False |  |
| emailSent | boolean | False |  |
| warning | string | False |  |

## MembershipPackageRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| durationDays | integer(int32) | True |  |
| price | number | True | {"minimum": 0.0, "exclusiveMinimum": false} |
| benefits | string | False |  |
| status | string | False |  |
| packageType | string | False | {"pattern": "(?i)Normal\|Advanced"} |
| totalCredit | integer(int32) | False |  |

## MembershipPackageResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| durationDays | integer(int32) | False |  |
| price | number | False |  |
| benefits | string | False |  |
| status | string | False |  |
| packageType | string | False |  |
| totalCredit | integer(int32) | False |  |

## MonthlyMemberGrowthItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| month | string | False |  |
| count | integer(int64) | False |  |

## NormalClassCreationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| requestId | string(uuid) | True |  |
| sportsClass | SportsClassRequest | True |  |
| roomId | integer(int32) | True |  |
| daysOfWeek | array<integer(int32)> | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |

## NormalMakeupRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| date | string(date) | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |
| roomId | integer(int32) | True |  |
| coachId | integer(int32) | True |  |
| reason | string | True |  |

## Notice

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| sessionId | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| reportedAt | string(date-time) | False |  |
| reason | string | False |  |
| active | boolean | False |  |
| supersededAt | string(date-time) | False |  |

## NotificationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| type | string | False |  |
| title | string | False |  |
| message | string | False |  |
| relatedType | string | False |  |
| relatedId | integer(int64) | False |  |
| isRead | boolean | False |  |
| createdAt | string(date-time) | False |  |

## Order

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| paymentMethod | string | True | {"pattern": "Cash\|VNPay"} |

## OtpMessageResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| message | string | False |  |
| code | string | False |  |

## PackageDistributionItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| packageName | string | False |  |
| memberCount | integer(int64) | False |  |

## PackageExtensionRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| extendDays | integer(int32) | True | {"minimum": 1} |
| reason | string | False |  |

## PackageStatusRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| status | string | True |  |

## PageChatConversationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalElements | integer(int64) | False |  |
| totalPages | integer(int32) | False |  |
| last | boolean | False |  |
| first | boolean | False |  |
| numberOfElements | integer(int32) | False |  |
| pageable | PageableObject | False |  |
| size | integer(int32) | False |  |
| content | array<ChatConversationResponse> | False |  |
| number | integer(int32) | False |  |
| sort | array<SortObject> | False |  |
| empty | boolean | False |  |

## PageNotificationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalElements | integer(int64) | False |  |
| totalPages | integer(int32) | False |  |
| last | boolean | False |  |
| first | boolean | False |  |
| numberOfElements | integer(int32) | False |  |
| pageable | PageableObject | False |  |
| size | integer(int32) | False |  |
| content | array<NotificationResponse> | False |  |
| number | integer(int32) | False |  |
| sort | array<SortObject> | False |  |
| empty | boolean | False |  |

## Pageable

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| page | integer(int32) | False | {"minimum": 0} |
| size | integer(int32) | False | {"minimum": 1} |
| sort | array<string> | False |  |

## PageableObject

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| unpaged | boolean | False |  |
| paged | boolean | False |  |
| pageNumber | integer(int32) | False |  |
| pageSize | integer(int32) | False |  |
| offset | integer(int64) | False |  |
| sort | array<SortObject> | False |  |

## PaymentResolutionOpenRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| invoiceId | integer(int32) | True |  |
| type | string | True |  |
| reason | string | True |  |

## PaymentResolutionResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| invoiceId | integer(int32) | False |  |
| type | string | False |  |
| status | string | False |  |
| amount | number | False |  |
| reason | string | False |  |
| evidence | string | False |  |
| outcomeNote | string | False |  |
| createdAt | string(date-time) | False |  |
| approvedAt | string(date-time) | False |  |
| performedAt | string(date-time) | False |  |
| completedAt | string(date-time) | False |  |

## PermissionResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| description | string | False |  |

## Preview

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| previewToken | string | False |  |
| blocked | boolean | False |  |
| affectedSessionIds | array<integer(int32)> | False |  |
| reconciliationSubscriptionIds | array<integer(int32)> | False |  |
| deadlineChanges | array<DeadlineChange> | False |  |

## PrivateBookingRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subscriptionId | integer(int32) | True |  |
| coachId | integer(int32) | True |  |
| roomId | integer(int32) | True |  |
| sessionDate | string(date) | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |
| classId | integer(int32) | False |  |
| subjectId | integer(int32) | False |  |
| agreementConfirmed | boolean | False |  |
| requestKey | string | False |  |
| supportReason | string | False |  |
| incidentId | integer(int32) | False |  |
| extraValidityReason | string | False |  |

## PublicMembershipPackageResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| price | number | False |  |
| durationDays | integer(int32) | False |  |
| benefits | string | False |  |

## PublicSubjectResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| slug | string | False |  |
| name | string | False |  |
| description | string | False |  |

## RecurringPreviewRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | True |  |
| roomId | integer(int32) | True |  |
| startDate | string(date) | True |  |
| daysOfWeek | array<integer(int32)> | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |

## RecurringPreviewResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalSessions | integer(int32) | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| roomId | integer(int32) | False |  |
| roomName | string | False |  |
| hasConflict | boolean | False |  |
| sessions | array<SessionPreviewItem> | False |  |
| skippedHolidays | array<SkippedHolidayItem> | False |  |

## RedirectView

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| applicationContext | ApplicationContext | False |  |
| servletContext | ServletContext | False |  |
| contentType | string | False |  |
| requestContextAttribute | string | False |  |
| staticAttributes | object/map | False | {"additionalProperties": {"type": "object"}} |
| exposePathVariables | boolean | False |  |
| exposeContextBeansAsAttributes | boolean | False | {"writeOnly": true} |
| exposedContextBeanNames | array<string> | False | {"writeOnly": true} |
| beanName | string | False |  |
| url | string | False |  |
| contextRelative | boolean | False | {"writeOnly": true} |
| http10Compatible | boolean | False | {"writeOnly": true} |
| exposeModelAttributes | boolean | False | {"writeOnly": true} |
| encodingScheme | string | False | {"writeOnly": true} |
| statusCode | HttpStatusCode | False |  |
| expandUriTemplateVariables | boolean | False | {"writeOnly": true} |
| propagateQueryParams | boolean | False | {"writeOnly": true} |
| hosts | array<string> | False |  |
| propagateQueryProperties | boolean | False |  |
| redirectView | boolean | False |  |
| attributesCSV | string | False | {"writeOnly": true} |
| attributesMap | object/map | False | {"additionalProperties": {"type": "object"}} |
| attributes | object/map | False | {"additionalProperties": {"type": "string"}, "writeOnly": true} |

## RegisterRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| fullName | string | True |  |
| email | string | True |  |
| password | string | True |  |
| phone | string | False |  |
| dateOfBirth | string(date) | False |  |
| gender | string | False |  |

## ResendOtpRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| email | string | True |  |
| purpose | string | True |  |

## ResetPasswordRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| email | string | True |  |
| otp | string | True |  |
| newPassword | string | True |  |

## Resolve

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| endedAt | string(date-time) | True |  |

## RevenueByMethodItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| method | string | False |  |
| count | integer(int64) | False |  |
| totalRevenue | number | False |  |

## RevenueByPackageItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| packageId | integer(int32) | False |  |
| packageName | string | False |  |
| count | integer(int64) | False |  |
| totalRevenue | number | False |  |

## RevenueReportResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| totalRevenue | number | False |  |
| totalTransactions | integer(int64) | False |  |
| averageTransactionValue | number | False |  |
| revenueByPackage | array<RevenueByPackageItem> | False |  |
| revenueByMethod | array<RevenueByMethodItem> | False |  |
| timeline | array<RevenueTimelineItem> | False |  |
| moneyReceived | number | False |  |
| moneyRefunded | number | False |  |
| netReceived | number | False |  |
| cashCorrectionsRecorded | number | False |  |

## RevenueTimelineItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| period | string | False |  |
| revenue | number | False |  |
| count | integer(int64) | False |  |

## RolePermissionUpdateRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| permissionIds | array<integer(int32)> | True |  |

## RoleResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| description | string | False |  |
| permissions | array<PermissionResponse> | False |  |

## RoomRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| location | string | False |  |
| capacity | integer(int32) | True |  |
| status | string | False |  |
| facilityType | string | False | {"pattern": "Indoor\|Outdoor"} |

## RoomResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| location | string | False |  |
| capacity | integer(int32) | False |  |
| status | string | False |  |
| facilityType | string | False |  |
| outdoor | boolean | False |  |

## RoomStatusRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| status | string | True |  |

## ServletContext

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| effectiveMajorVersion | integer(int32) | False |  |
| effectiveMinorVersion | integer(int32) | False |  |
| servletContextName | string | False |  |
| filterRegistrations | object/map | False | {"additionalProperties": {"$ref": "#/components/schemas/FilterRegistration"}} |
| sessionCookieConfig | SessionCookieConfig | False |  |
| defaultSessionTrackingModes | array<string> | False | {"uniqueItems": true} |
| serverInfo | string | False |  |
| effectiveSessionTrackingModes | array<string> | False | {"uniqueItems": true} |
| jspConfigDescriptor | JspConfigDescriptor | False |  |
| virtualServerName | string | False |  |
| requestCharacterEncoding | string | False |  |
| responseCharacterEncoding | string | False |  |
| sessionTimeout | integer(int32) | False |  |
| servletRegistrations | object/map | False | {"additionalProperties": {"$ref": "#/components/schemas/ServletRegistration"}} |
| sessionTrackingModes | array<string> | False | {"uniqueItems": true, "writeOnly": true} |
| attributeNames | object | False |  |
| initParameterNames | object | False |  |
| contextPath | string | False |  |
| classLoader | object | False |  |
| majorVersion | integer(int32) | False |  |
| minorVersion | integer(int32) | False |  |

## ServletRegistration

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| runAsRole | string | False |  |
| mappings | array<string> | False |  |
| initParameters | object/map | False | {"additionalProperties": {"type": "string"}} |
| name | string | False |  |
| className | string | False |  |

## SessionCookieConfig

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| domain | string | False |  |
| httpOnly | boolean | False |  |
| secure | boolean | False |  |
| maxAge | integer(int32) | False |  |
| path | string | False |  |
| name | string | False |  |
| attributes | object/map | False | {"additionalProperties": {"type": "string"}} |
| comment | string | False | {"deprecated": true} |

## SessionGenerateRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | True |  |
| roomId | integer(int32) | True |  |
| startDate | string(date) | True |  |
| endDate | string(date) | False |  |
| daysOfWeek | array<integer(int32)> | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |

## SessionPreviewItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| sessionNumber | integer(int32) | False |  |
| date | string(date) | False |  |
| dayOfWeek | string | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| hasConflict | boolean | False |  |
| conflictDetails | array<string> | False |  |

## SessionRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| classId | integer(int32) | True |  |
| roomId | integer(int32) | True |  |
| sessionDate | string(date) | True |  |
| startTime | LocalTime | True |  |
| endTime | LocalTime | True |  |
| status | string | False |  |

## SessionRescheduleRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| newDate | string(date) | True |  |
| newStartTime | LocalTime | True |  |
| newEndTime | LocalTime | True |  |
| newRoomId | integer(int32) | False |  |
| reason | string | False |  |
| extendValidityDays | integer(int32) | False |  |
| cause | string | False |  |
| memberAgreed | boolean | False |  |
| requestKey | string | False |  |
| incidentId | integer(int32) | False |  |
| extraValidityReason | string | False |  |

## SessionResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| subjectId | integer(int32) | False |  |
| subjectName | string | False |  |
| coachId | integer(int32) | False |  |
| coachName | string | False |  |
| roomId | integer(int32) | False |  |
| roomName | string | False |  |
| roomLocation | string | False |  |
| sessionDate | string(date) | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| status | string | False |  |
| enrolledCount | integer(int32) | False |  |
| maxCapacity | integer(int32) | False |  |
| bookedMemberId | integer(int32) | False |  |
| bookedMemberName | string | False |  |
| isPrivate | boolean | False |  |
| centerInvalidated | boolean | False |  |
| makeupForSessionId | integer(int32) | False |  |
| creditReturned | boolean | False |  |
| createdBy | integer(int32) | False |  |
| agreementConfirmedAt | string(date-time) | False |  |

## SessionStatusRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| status | string | True |  |

## SkippedHolidayItem

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| date | string(date) | False |  |
| holidayName | string | False |  |
| tag | string | False |  |

## SortObject

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| direction | string | False |  |
| nullHandling | string | False |  |
| ascending | boolean | False |  |
| property | string | False |  |
| ignoreCase | boolean | False |  |

## SpecializationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| subjectId | integer(int32) | False |  |
| description | string | False |  |
| status | string | False |  |

## SpecializationResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| subjectId | integer(int32) | False |  |
| subjectName | string | False |  |
| description | string | False |  |
| status | string | False |  |

## SportsClassRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| subjectId | integer(int32) | True |  |
| coachId | integer(int32) | False |  |
| maxCapacity | integer(int32) | True |  |
| tuitionFee | number | False | {"minimum": 0.0, "exclusiveMinimum": false} |
| startDate | string(date) | False |  |
| endDate | string(date) | False |  |
| status | string | False |  |
| classType | string | False |  |
| totalSessions | integer(int32) | False |  |

## SportsClassResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| subjectId | integer(int32) | False |  |
| subjectName | string | False |  |
| coachId | integer(int32) | False |  |
| coachName | string | False |  |
| maxCapacity | integer(int32) | False |  |
| tuitionFee | number | False |  |
| startDate | string(date) | False |  |
| endDate | string(date) | False |  |
| status | string | False |  |
| enrolledCount | integer(int32) | False |  |
| classType | string | False |  |
| totalSessions | integer(int32) | False |  |

## StaffRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| email | string | True |  |
| phone | string | False |  |
| role | string | True |  |
| specialization | string | False |  |
| password | string | False |  |
| status | string | False |  |
| centerId | integer(int32) | False |  |
| specializationIds | array<integer(int32)> | False | {"uniqueItems": true} |

## StaffResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| code | string | False |  |
| name | string | False |  |
| email | string | False |  |
| phone | string | False |  |
| role | string | False |  |
| specialization | string | False |  |
| specializationVi | string | False |  |
| status | string | False |  |
| activeClassesCount | integer(int32) | False |  |
| emailSent | boolean | False |  |
| warning | string | False |  |

## SubjectRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| name | string | True |  |
| description | string | False |  |
| status | string | False | {"pattern": "(?i)Active\|Inactive"} |
| slug | string | False |  |
| normalPrice | number | False | {"minimum": 0, "exclusiveMinimum": true} |
| advancedPrice | number | False | {"minimum": 0, "exclusiveMinimum": true} |
| outdoor | boolean | False |  |

## SubjectResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| name | string | False |  |
| description | string | False |  |
| normalPrice | number | False |  |
| advancedPrice | number | False |  |
| status | string | False |  |
| slug | string | False |  |
| outdoor | boolean | False |  |

## SubscriptionOrderResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| subscriptionId | integer(int32) | False |  |
| invoiceId | integer(int32) | False |  |
| invoiceCode | string | False |  |
| amount | number | False |  |
| paymentMethod | string | False |  |
| status | string | False |  |
| expiresAt | string(date-time) | False |  |
| paymentUrl | string | False |  |

## SubstituteCoachRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| newCoachId | integer(int32) | True |  |
| reason | string | False |  |
| memberAgreed | boolean | False |  |
| requestKey | string | False |  |
| incidentId | integer(int32) | False |  |

## SystemLog

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| userId | integer(int32) | False |  |
| action | string | False |  |
| targetEntity | string | False |  |
| targetId | integer(int32) | False |  |
| detail | string | False |  |
| createdAt | string(date-time) | False |  |

## TaglibDescriptor

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| taglibURI | string | False |  |
| taglibLocation | string | False |  |

## TimeSlotResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| slotNumber | integer(int32) | False |  |
| name | string | False |  |
| startTime | LocalTime | False |  |
| endTime | LocalTime | False |  |
| outdoorAllowed | boolean | False |  |
| displayName | string | False |  |

## TrainingPlanRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| coachId | integer(int32) | True |  |
| classId | integer(int32) | False |  |
| memberId | integer(int32) | False |  |
| title | string | True |  |
| content | string | False |  |
| goal | string | False |  |
| startDate | string(date) | False |  |
| endDate | string(date) | False |  |

## TrainingPlanResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| planId | integer(int32) | False |  |
| coachId | integer(int32) | False |  |
| coachName | string | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| title | string | False |  |
| content | string | False |  |
| goal | string | False |  |
| startDate | string(date) | False |  |
| endDate | string(date) | False |  |
| createdAt | string(date-time) | False |  |

## TrainingResultRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| sessionId | integer(int32) | True |  |
| memberId | integer(int32) | True |  |
| coachId | integer(int32) | True |  |
| content | string | False |  |
| attendanceStatus | string | False |  |
| reason | string | False |  |

## TrainingResultResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| resultId | integer(int32) | False |  |
| sessionId | integer(int32) | False |  |
| sessionDate | string(date) | False |  |
| classId | integer(int32) | False |  |
| className | string | False |  |
| memberId | integer(int32) | False |  |
| memberName | string | False |  |
| coachId | integer(int32) | False |  |
| coachName | string | False |  |
| content | string | False |  |
| attendanceStatus | string | False |  |
| recordedAt | string(date-time) | False |  |

## UnreadCountResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| unreadCount | integer(int64) | False |  |

## UserRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| fullName | string | True |  |
| email | string | True |  |
| phone | string | False |  |
| address | string | False |  |
| gender | string | False |  |
| dateOfBirth | string(date) | False |  |
| status | string | False |  |
| roleId | integer(int32) | False |  |
| roleName | string | False |  |
| password | string | False |  |
| centerId | integer(int32) | False |  |
| specializationIds | array<integer(int32)> | False | {"uniqueItems": true} |

## UserResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| id | integer(int32) | False |  |
| code | string | False |  |
| fullName | string | False |  |
| email | string | False |  |
| phone | string | False |  |
| address | string | False |  |
| gender | string | False |  |
| dateOfBirth | string(date) | False |  |
| status | string | False |  |
| roleId | integer(int32) | False |  |
| roleName | string | False |  |
| createdAt | string(date-time) | False |  |
| permissions | array<string> | False | {"uniqueItems": true} |
| emailSent | boolean | False |  |
| warning | string | False |  |

## VerifyRegistrationRequest

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| email | string | True |  |
| otp | string | True |  |

## Work

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| weeklySlots | array<string> | True | {"uniqueItems": true} |
| daysOff | array<string(date)> | True | {"uniqueItems": true} |
| acceptingNew | boolean | False |  |

## WorkResponse

| Field | Type | Required in OpenAPI | Constraints / enum |
|---|---|---|---|
| coachId | integer(int32) | False |  |
| acceptingNew | boolean | False |  |
| weeklySlots | array<string> | False | {"uniqueItems": true} |
| daysOff | array<string(date)> | False | {"uniqueItems": true} |
