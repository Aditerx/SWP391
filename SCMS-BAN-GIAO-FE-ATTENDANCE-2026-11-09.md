# Bàn giao FE attendance — 09/11/2026

Đối tượng: FE màn điểm danh Coach và lịch sử điểm danh Member; BE policy/API/job attendance. **Thiết kế Owner đã chốt; BE/API/job đã triển khai, kết quả kiểm local ở Active/QA; FE/Render chưa nghiệm thu.** Gợi ý UI dưới đây diễn giải flow/dữ liệu để FE tham khảo; FE quyết định bố trí, câu chữ và cách tương tác. Giới hạn quyền/thời gian được BE thực thi độc lập UI.

## 1. Kết quả hiện tại

- BE đã mở correction đúng Coach/quyền/hạn, nhận/trả note và metadata, tách finalizer tại end với review no-show sau cutoff. Correct giữ timestamp, notice.reason, credit/lịch/hạn và content, không tạo kết quả chuyên môn giả. Không coi các field/code đã có trên Render.
- Owner đã chốt: Coach được giao sửa sau buổi đến hết23:59 ngày buổi; note cho Member đọc của mình, reason/audit riêng; thông báo rõ Coach khác/hết hạn; giữ hoàn tất buổi tại end, xử lý báo vắng/chế tài sau hạn sửa. Chế tài khóa booking vẫn default tắt.
- Quan sát roster200, correct403 và vấn đề đồng bộ buổi trước đây ở [LAN-2 ngày07/11](SCMS-BAN-GIAO-FE-RBAC-LAN-2-2026-11-07.md) là snapshot trước quyết định này. Bản09/11 tiếp nối phần thiết kế attendance, không suy rằng FE đã sửa hoặc BE đã phát hành.
- Trạng thái triển khai/bằng chứng chuẩn: [Active](docs/00-MASTER-CHECKLIST.md#active) và [QA](artifacts/qa/latest.json). Phạm vi kiểm BE local có response/DB sau commit và race; FE/Render/live vẫn chưa nghiệm thu.

## 2. Quá trình và quyết định giữ/bỏ

1. Giữ phân biệt ghi/QR trong buổi với correction sau buổi, bỏ mốc khóa Coach ngay end vì Owner chọn thời gian đối soát đến cuối ngày.
2. Giữ quyền theo phân công/center/authority và reason/audit; Coach khác vẫn bị chặn với thông báo rõ, không mở quyền chỉ để hết403.
3. Giữ note hiển thị riêng với lý do sửa nội bộ và content chuyên môn; Member chỉ đọc note của chính mình, giữ lý do báo vắng gốc.
4. Giữ hoàn tất buổi tại end, tách báo vắng/chế tài tới sau cutoff; migration17 thêm marker hàng chờ mới, không queue buổi finalized cũ.
5. Đã triển khai và kiểm BE local theo Active/QA; giữ minh họa UI là đề xuất để FE quyết định thiết kế.

## 3. API và DB liên quan

Quyết định: [D15](docs/04-DEBUG-NOTE.md#d15). Field/type/validation/error đầy đủ: [contract09/11](docs/01-API-CONTRACT-FE-BE.md#attendance-correction-20261109). Phần này tóm tắt flow, không thay snapshot OpenAPI hiện có.

| Thời điểm theo Clock BE/giờ Việt Nam | Flow đích |
|---|---|
| Trước start | Chưa ghi manual/QR; báo vắng hợp lệ D05 là ngoại lệ riêng |
| `[start,end)` | Coach được giao ghi/đổi qua POST `/api/training-results` hoặc `/bulk`; thêm note tùy chọn từng người |
| Từ end tới trước00:00 hôm sau ngày buổi | Coach Active+RECORD_RESULT, đúng phân công/cùng center dùng POST `/api/attendances/correct`, reason bắt buộc |
| Từ00:00 hôm sau | Coach chỉ đọc; quản lý theo quyền correction hiện hành vẫn sửa được sau end |

POST correct chọn một cách định danh: attendanceId **hoặc** sessionId+memberId. Cách thứ hai xử lý khi finalizer chưa tạo attendance; BE kiểm quan hệ/quyền học đúng buổi và upsert dưới khóa. Chỉ Present/Absent; không tạo kết quả chuyên môn giả, đổi credit/lịch/hạn, giả giờ đến hoặc sửa attendance superseded/buổi hủy/invalidated. Finalizer vừa đổi enrollment/entitlement thành Completed không tự làm mất quyền correction hợp lệ.

Ví dụ request khi đã có attendance, chỉ minh họa dữ liệu:

```json
{
  "attendanceId": 501,
  "newState": "Present",
  "note": "Đã tham gia phần thực hành.",
  "reason": "Đối soát cuối buổi, trước đó ghi nhầm."
}
```

GET `/api/attendances?sessionId={id}` trả note của đúng buổi/Member theo scope; response ghi trả giá trị note đã lưu. Ví dụ phần dữ liệu response của Coach sau khi ghi thành công theo thiết kế:

```json
{
  "attendanceId": 501,
  "sessionId": 42,
  "memberId": 7,
  "state": "Present",
  "note": "Đã tham gia phần thực hành.",
  "correctionAllowed": true,
  "correctionDeadline": "2026-11-10T00:00:00+07:00",
  "serverTime": "2026-11-09T09:00:00+07:00"
}
```

- note: optional/plain text/tối đa1000ký tự sau trim. Bỏ/null giữ nguyên; rỗng sau trim xóa có audit. Response null nghĩa là chưa có ghi chú. Reason correction luôn bắt buộc, tối đa1000ký tự, không tự công bố thành note; chỉ sửa note thì gửi newState hiện có.
- correctionAllowed nói quyền dùng correct của actor tại thời điểm BE đánh giá, không phải quyền ghi trong buổi. correctionDeadline là hạn Coach exclusive00:00, không phải hạn của Manager; Member có thể nhận hạn đối soát nhưng correctionAllowed luôn false. serverTime dùng đối chiếu; BE vẫn kiểm Clock lúc POST.
- 400: state/target/note/reason không hợp lệ, hoặc USE_SESSION_ATTENDANCE khi correct trước end.403 ATTENDANCE_CORRECTION_FORBIDDEN: sai quyền/phân công, không được sửa/note.403 ATTENDANCE_CORRECTION_DEADLINE_PASSED: Coach đúng scope nhưng hết hạn; không ánh xạ mọi403 thành hết giờ. Xác minh quyền trước trả thông tin buổi/thời gian.
- Lưu state/note ở attendances; giữ absenceNoticeId/notice.reason gốc. Đồng bộ training_results.attendance_status nếu đã có result; content là dữ liệu chuyên môn riêng. Actor/time/trước-sau/reason lưu audit system_logs; không cấp audit-log toàn cục cho Coach.

**Hai mốc xử lý:** buổi09/11 diễn ra06:00–07:30 thì07:30 vẫn hoàn tất buổi/lifecycle và có thể tự ghi Absent cho record thiếu. Coach còn đối soát đến hết23:59. Nếu09:00 sửa Absent→Present, sau00:00 ngày10/11 job đọc state hiện hành nên không gửi báo vắng cho người đó. Job trễ không gia hạn quyền Coach; retry không nhân record/thông báo. Chế tài giữ mặc định tắt; nếu sau này bật vẫn xét sau cutoff. Đây không phải kéo dài giờ học hoặc quyền credit.

Quyền mới không cho Coach sửa buổi ngày cũ quá cutoff: ví dụ attendance85/ngày05/11 được đem sửa ngày09/11 vẫn hết hạn Coach; quản lý xử lý theo quyền của mình. Đã chuẩn bị migration17 có guard/replay; chưa mutation cloud.

## 4. FE cần làm

**Các gợi ý tích hợp/hiển thị để FE quyết định thiết kế:**

- Có thể thể hiện trạng thái cùng note ở mỗi dòng học viên theo đúng sessionId+memberId. Khi mở sửa, dùng dữ liệu đã đọc của đúng record; khi đổi buổi, cân nhắc tải/reset form và loại response cũ để không đem note/attendanceId buổi trước sang buổi mới. Cách tải và bố trí do FE chọn.
- Gợi ý hai ô: **Ghi chú cho học viên** kèm lời nhắc “Học viên có thể đọc nội dung này”; **Lý do sửa** kèm giải thích “Lưu lịch sử nội bộ”. Ví dụ Member thấy “Có mặt — Ghi chú: Đã tham gia phần thực hành.”, không nhận reason từ form quản lý.
- Trước hạn có thể hiện “Buổi học đã kết thúc. Kết quả điểm danh còn có thể được điều chỉnh đến hết23:59 hôm nay”; hết hạn có thể chuyển sang chỉ đọc và hiện “Đã hết hạn sửa của Coach. Vui lòng liên hệ quản lý trung tâm”.
- Với Coach khác, gợi ý “Bạn không được phân công phụ trách buổi học này nên không thể sửa điểm danh hoặc ghi chú”; không hiển thị chi tiết record ngoài quyền. Đây là giải thích từ chối, không mở quyền note cho Coach khác.
- Gợi ý xác nhận lưu dựa POST thành công và dữ liệu BE trả/readback; lỗi giữ thông tin người nhập và dữ liệu server để xử lý. GET200 sau POST403 hoặc badge local không chứng minh DB đã lưu; không dùng fallback đổi endpoint để vượt policy.
- **Nếu UI chưa lấy note:** tách ba điểm tích hợp để kiểm: đọc note vào form → gửi note khi lưu → hiển thị note server sau thành công/tải lại. BE lưu được chưa chứng minh UI đã nối đủ. Response note:null có thể hiển thị “Chưa có ghi chú”; response thiếu field note cần đối chiếu contract/build, không tự kết luận DB chưa lưu. Màn Member có thể hiển thị note dưới kết quả buổi tương ứng; cách trình bày do FE quyết định.

## 5. Việc còn lại

- BE local và tiêu chí đã kiểm theo [Active](docs/00-MASTER-CHECKLIST.md#active)/[QA](artifacts/qa/latest.json); OpenAPI sinh từ source đã kiểm. Trước phát hành dùng [migration17/vận hành](docs/02-DEPLOYMENT-GUIDE.md#attendance-review-20261109); schema gộp chưa nhúng17, không dùng bản dựng lại để nâng DB hiện có.
- Khi nghiệm thu FE/live, đối chiếu response + readback đúng buổi/owner, note bỏ/null/xóa, Coach A/B/hết hạn, Manager và thông báo sau cutoff. Không lặp ca local đã đủ bằng chứng khi chưa có diff hoặc nghi vấn mới; không suy test local thành FE đã tích hợp.
- FE có thể dùng bản này để thống nhất cách trình bày và đối chiếu field; bản source/build Pages hiện hành và tiến độ tích hợp chưa được xác nhận. Không sửa FE trong nhóm BE này.
- Nghiệm thu FE/Render/CRUD vẫn theo đợt chung Owner đã hẹn; không yêu cầu lặp lại ca đã đủ bằng chứng khi chưa có thay đổi liên quan. Không reset DB hoặc chạy lại SQL16; hướng dẫn triển khai/migration theo [docs02](docs/02-DEPLOYMENT-GUIDE.md), tiến độ ở [checkpoint](docs/03-TODO.md#attendance-note-correction-proposal-20261109).
