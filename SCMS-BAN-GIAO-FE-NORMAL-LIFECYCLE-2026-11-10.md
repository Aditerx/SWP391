# SCMS — Bàn giao FE: Normal Lifecycle

Dành cho FE; ngày10/11/2026. Phạm vi BE/schema/fixture local, chưa phát hành hoặc nghiệm thu FE/Render. Kết quả chuẩn và giới hạn tại [Active/QA](docs/00-MASTER-CHECKLIST.md#active).

## 1. Kết quả hiện tại

BE kiểm minimum/cutoff48h/confirmation24h và ngoại lệ trễ có reason; start/close theo12buổi hợp lệ, giữ D15 correction/bù. Cash Normal là thu ngay tại quầy nguyên khối; waitlist chỉ mời, hết hạn thì quay cuối hàng, không autoCash.

Có proposal dời một lần do thiếu sĩ số, lựa chọn Member có version/fingerprint/snapshot lịch, chuyển cùng môn/giá/center và hủy trước khai giảng gắn nghĩa vụ hoàn. Invoice lớp mua/amount giữ gốc, quyền đích có liên kết riêng; API đọc lịch/attendance/plan/result/report theo quyền hiện tại. [Refund có bản bàn giao riêng](SCMS-BAN-GIAO-FE-NORMAL-REFUND-2026-11-10.md).

## 2. Quá trình và quyết định giữ/bỏ

- Giữ transaction tạo12buổi, center lock, giá mua và finalizer/correction; thêm metadata và chặn mọi alias đổi trạng thái tùy ý.
- Bỏ Member Cash online/autoCash waitlist vì chưa phải thu tại quầy; giữ Advanced và CashCorrection D16.
- Giữ Invoice gốc khi chuyển, thêm entitlement_class_id/transfer; bỏ cách dùng Accepted nhưng chưa chuyển quyền.
- Snapshot lịch/receiver/verification được giữ để sửa về sau không đổi nội dung đã đồng ý; dữ liệu demo là tổng hợp, không là giao dịch thật.

## 3. API và DB liên quan

[Contract đầy đủ](docs/01-API-CONTRACT-FE-BE.md#normal-changes-20261110), [OpenAPI](artifacts/fe-handoff/openapi.json), [DB/vận hành](docs/02-DEPLOYMENT-GUIDE.md#demo-rebuild-20261109).

- POST /api/classes/normal-with-sessions: sportsClass.minCapacity tùy chọn, ceil(2×max/3) tới max; sau đăng ký hoặc công bố không sửa tùy ý.
- POST /api/classes/{id}/confirm-opening: Manager+MANAGE_CLASSES, reason khi trễ; không mở lại đăng ký sau cutoff.
- GET/POST /api/classes/{id}/normal-proposals: phương án Postpone/Transfer; response id/version/fingerprint/schedule/choices. Preview không giữ chỗ.
- POST /api/normal-proposals/{id}/respond: Member gửi version/fingerprint/choice:Accept hoặc Refund. Accept Transfer recheck và commit ngay; Accept Postpone chờ apply.
- POST /api/normal-proposals/{id}/apply: Manager gửi version, mọi người phải phản hồi; không tự mở lớp.
- POST /api/classes/{id}/cancel-before-opening: version/reason, Manager; hủy Pending, tạo nghĩa vụ cho khoản Paid. Ongoing bị chặn.
- SportsClassResponse thêm changePending; InvoiceResponse.entitlementClassId là lớp hiện có quyền, classId/amount vẫn mua gốc. Migration21/22 và bundle trong README.

## 4. FE cần làm

Đăng nhập/auth-me trước nhóm có phiên. Dùng UUID requestId khi tạo lớp; tải response mới sau thao tác, không tự tính minimum/cutoff hoặc tự gán Ongoing/Closed. changePending chặn đăng ký/confirm; hiển thị lịch cụ thể và hai lựa chọn, không chọn thay Member.409 tải lại trạng thái, version/fingerprint/chỗ; lịch đã đổi không áp consent cũ, Member vẫn được chọn Refund của phương án TT.

Sau Accept Transfer, tải lại enrollments/lịch/invoice; không tạo đơn mới ở lớp đích. Sau Cash quầy response đã Paid+Registered, không confirm-cash lần nữa. Paid+reconciliationRequired cần đối soát; Return không xác nhận Paid. Bố cục/câu chữ/UI do FE quyết định.

## 5. Việc còn lại

Owner review thay đổi local/main và duyệt phát hành/bundle trước thao tác cloud. FE xác định repo/build hiện hành rồi smoke/auth-me/readback trên build mới; matrix/CRUD/FE/live và policy R1/R2 còn thuộc Phase2. SYSTEM-DATA vẫn DATA DESIGN, không chứng nhận đã reset Supabase.
