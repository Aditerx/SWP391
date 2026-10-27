# Bàn giao và demo hai phase SCMS

Phạm vi: P1.0–P1.4 và BE P2.1–P2.5; P1.5/gói T D01/D09/FE để lại theo Owner. Nền tai 0277b2f. Source BE chốt `13a62ee780be19dce62730b45b119e2f97f31def`: clean test292/273pass/19skip/0lỗi,PG HTTP9/9pass. [Trang demo](evidence/demo.html), [manifest](evidence/verification.json), [test summary](evidence/test-summary.txt), [HTTP trace](evidence/postgres-http.txt). Feature199bbf9 đã push;merge main86d0076→155968d đã kiểm lại clean292/273pass/19skip/0lỗi và PG HTTP9/9. Manifest hiện hành ghi merge155968d;commit đóng docs giữ nguyên BE. Contract: [docs 01](../../../../docs/01-API-CONTRACT-FE-BE.md); vận hành/migration: [docs 02 mục 10](../../../../docs/02-DEPLOYMENT-GUIDE.md#10-bàn-giao-hai-phase-và-demo-tái-lập--27102026).

> Đính chính target theo Owner: các artifact tên production-release/production-postflight và bundle empty-schema trong thư mục này đã chạy trên **DB test1a3dfdd7…**. Production thực188f6765… có dữ liệu sẵn, được nâng cấp riêng có phê duyệt; xem [handoff production](../../../production-upgrade/2026-10-27/README.md). Giữ nguyên artifact/hash lịch sử.

## Chạy demo

Docker Desktop/PostgreSQL17 container scms-phases-20261027 đang chạy, bind 127.0.0.1:55440, local-only password scms-local-only. Java17/Python3/PowerShell sẵn có:

```powershell
.\scripts\run-phases-demo.ps1
```

Runner kiểm đúng **HEAD đã commit** trong detached worktree không có .env, chạy clean test, tạo DB tên mới, nâng cấp/replay/kiểm snapshots/catalog guards, đăng nhập HTTP thật vào server port ngẫu nhiên và kiểm DB sau từng thao tác. Giữ worktree/DB/evidence để người nhận xem. Không cần tài khoản/password production. Xem output verification.json, clean-test.log/XML, postgres-http.log/XML và postgres-http/http-workflow.txt. Không dùng tài khoản demo mục 1.3 contract cho production; fixture @example.test chỉ runner tạo ở DB local. Đọc script/test để đổi dữ liệu demo; không sửa expected trừ khi fixture/rule thay có giải trình.

## Kịch bản thuyết trình 15–20 phút

| Phút | Thao tác/mục đích | Bằng chứng mong đợi |
|---|---|---|
| 0–2 | Giới thiệu9 môn/giá,work-schedule,request→accept→order | Chưa accept:400; Pending0credit/chưa hạn; retry cùng Invoice |
| 2–4 | Cash và VNPay protocol; thử Return trước IPN | Invoice giá gốc dù catalog đổi; Return Pending; IPN Paid12 chỉ1lần; sai chủ404 |
| 4–5 | Tiền tới đúng5phút hoặc môn unavailable | Paid reconciliationRequired,0credit; không bỏ mất receipt |
| 5–8 | Member tự book bị403;Coach book/consent;hủy đúng12h;notice dưới12h | Debit1;retry không nhân;return1credit;notice riêng;room/no-op giữ notice,dời thật supersede |
| 8–10 | Holiday preview/stale/blocked và incident overlap | Không auto dời/hủy;past closure giữ;union không cộng trùng |
| 10–12 | QR trước start/tại start/replay/tại end;Manager sửa sau end | TTL hiện tại;early/end/replay từ chối;Present/Absent;credit không trừ lại |
| 12–14 | Refund khác kỳ và CashCorrection hẹp | Approved chưa hoàn;Completed có evidence;history kể cả canceled chặn sửa Cash;3 số đúng kỳ |
| 14–17 | Sự cố chờ xuyên expiry;balance còn3;compensate và ca bù vượt hạn | Snapshot3→restore3,hoàn1 gốc đúng1lần;gia hạn tới đúng end ca bù,không reset12 |
| 17–20 | Normal12 skip holiday,Member không hủy;late incident,bù một buổi,finalize/report |13 bản vật lý/12 hợp lệ,giữ lịch sử;đủ12 Completed;Coach403reports;quyền Normal độc lập gói Advanced |

Chín method trong PhaseWorkflowHttpTest chạy **theo thứ tự**, chia sẻ một câu chuyện và Clock tăng từ27/10/2026 tới01/2027. Không chạy riêng method ở giữa khi chưa có fixture trước. Unit/DB tests riêng bao phủ locks/last seat/callback race,boundary nanos/12h/QR/report khác kỳ. Trace ghi endpoint/response đã bỏ login/cookie/QR/signature; DB assertions nằm sát thao tác trong test, không chỉ kiểm HTTP200.

## Các thay đổi cần FE tiếp nhận

- Member gửi request Advanced và trả tiền sau Coach nhận;Member không tạo ca. Coach/Manager cần gửi consent/requestKey/cause/incident phù hợp.
- Dùng activatedAt/expiresAt chính xác thay chỉ endDate;Invoice.expiresAt là deadline **thanh toán**, khác hạn gói.
- Tách absence notice khỏi attendance;Normal không mở hủy/báo vắng. Buổi centerInvalidated giữ history,makeupForSessionId nối buổi bù.
- Return chỉ hiển thị đang kiểm tra;đọc Invoice xác thực để biết Paid/reconciliation. Receipt có cashNote/resolution fields,không tự coi giá catalog là số tiền cũ.
- Holiday preview token/các ca ảnh hưởng trước confirm;case hoàn/đảo tiền có trạng thái Approved/Completed/Blocked.
- Hiển thị ba số moneyReceived/moneyRefunded/netReceived;totalRevenue giữ gross cũ. UI do FE quyết định.
- P1.5/T/FE chưa nghiệm thu;không build/browser verify FE cũ trong đợt này.

## Release và giới hạn

[SQL bundle](release/scms-phases-empty-schema.sql),[hash manifest](release/release-plan.json),[local proof](release/local-proof.json),[replay guard](release/local-replay-guard.json). SHA256 28635081e1724eb89ea82252fc1e468ca53805d347e4334d40ddf17a9c2866dc. Bundle chỉ empty schema,1transaction,9môn,0account/business rows. Production trước release read-only0bảng;Owner đã phê duyệt khởi tạo trước merge. [production-release.json](production-release.json) ghi commit đúng bundle,9môn/0account/business rows; [postflight](production-postflight.json)46bảng/đủcột checkpoint. Không chạy seed05/prepare-demo/live Cash trên production.

Demo dùng fake Cash/refund và signed local VNPay protocol,không gateway live/tiền thật. SMTP delivery,FE/CORS/cookie cloud,Render deploy và SQL Server runtime cần nghiệm thu riêng. Migration MSSQL bàn giao nhưng chưa chứng nhận chạy. bootstrap không tạo Admin;Owner cần provision account thật/SMTP/chuyên môn/phòng/work-schedule trước mở bán.

## Double-check và hướng đọc evidence

- So khớp scope từng P1.0–P1.4/P2.1–P2.5 với docs04 mục40;T/D01/D09/FE giữ backlog. Diff không sửa directory/profile hoặc FE. Giữ communication proof untracked của công việc khác ngoài commit này.
- 19 skip: LiveReadOnlyAudit13,LiveCashDemo1,DemoDataPreparation1,SessionSchemaPostgres4. Không bật live tests để tăng count;PostgreSQL mới đã được kiểm qua runner HTTP9 và migration guards.
- Final source đã kiểm Price/Paid/owner/center/locks/retry/12h/nanos/holiday union/expiry balance/QR/correction/Normal12/refund khác kỳ/Cash phụ thuộc/report quyền và các đường CRUD cũ. Invoice list center leak đã sửa;test filter cũ thêm actor fixture,giữ assertion filter.
- Bản check9d8c07c từng fail1 do fixture auth null;13a62ee đã sửa fixture và rerun clean/PG thành công. Lỗi p2-test6 từng dùng source chưa đồng bộ vào worktree;rerun7 đã dùng source đúng. Failed logs giữ local,không được gắn nhãn pass.
- Log raw/XML do runner tạo nằm local,không đóng gói vào Git vì có debug fixture/QR. Evidence công bố chỉ summary/case names/trace đã loại credential/cookie/QR/signature. scripts/build-phases-evidence.py có thể tái tạo manifest/demo từ output runner.

## Mốc Git bàn giao

- Baseline tai0277b2f;feature BE9d8c07c +fixture13a62ee;feature handoff đã push199bbf9.
- Main nền86d0076,merge155968db6398e10689abbf07e1cf72fe0be3e61d,không conflict/toàn tree bằng feature.
- Runner từ đúng mergecommit pass292run/273pass/19skip/0failure-error,PG9/9pass và migration guards. Evidence/source-tree ghi chính commit này.
- Commit đóng gói sau merge chỉ docs/evidence/script LF;main head cuối tra git ls-remote.Giữ nguyên BE đã kiểm.Git push/merge không chứng nhận Render deploy hoặc tạo account Owner.
