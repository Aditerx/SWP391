"""Build a Vietnamese frontend handoff from the audited API inventory and real local HTTP evidence."""
from pathlib import Path
import json, re

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts/fe-handoff/2026-10-27'
def load(name):return json.loads((OUT/name).read_text(encoding='utf-8-sig'))
def write(name,text):(OUT/name).write_text(text,encoding='utf-8',newline='\n')
audit=load('audit-summary.json'); exchanges=load('http-exchanges.json'); verification=load('demo/verification.json')

requirements=[
 ('D01','Hồ sơ Member tự phục vụ, Auth làm chủ danh tính','GET/PATCH /api/members/me','BACKLOG: không có handler self-service riêng trong Live OpenAPI','Không dùng /api/members/{id} thay thế; chưa triển khai/test theo scope Owner'),
 ('D02','9môn/18giá; giữ giá Invoice cũ','GET /api/subjects; GET /api/public/subjects','Catalog migration/local proof; HTTP phase1 snapshot; production9môn + JDBC18giá đã kiểm','Public DTO không có giá; GET có giá cần session. IDs local khác production'),
 ('D03','Chọn môn/Primary Coach; nhận trước Invoice; Paid mới cấp12credit','/api/advanced-registrations; /decision; /order; /work-schedule; /admission-capacity','Workflow1–3; AdvancedRegistration/Capacity source; InvoiceServiceTest','Chọn Coach không giữ ca; order.expiresAt là5phút; refresh subscriptions sau Paid; legacy cần đối soát'),
 ('D04','Primary Coach tạo ca có consent; Manager hỗ trợ có lý do','POST /api/sessions/book-private','Workflow4: Member403, thiếu consent400, Coach201, retry không trừ hai lần; AuthorizationHttpTest','FE Member không hiện nút tự tạo; requestKey ổn định khi retry; server quyết định actor/time'),
 ('D05','Hủy>=12h hoàn1; báo vắng<12h riêng; Normal cấm','/cancel-private; /absence; /absence-notices','Workflow4 kiểm đúng12h, retry, notice và balance/attendance DB','Sau hủy tải subscriptions; notice không đồng nghĩa Absent/Excused hoặc hoàn credit'),
 ('D06','Dời/thay Coach/bù do TT có hồ sơ và đồng ý','/reschedule; /substitute-coach; /api/center-incidents','Workflow4/8/9 dời/compensate/makeup; SessionIncidentServiceTest cho thay Coach','Thay Coach có unit/source; demo9case chưa có positive HTTP substitute-coach. Chưa full role matrix trên Render'),
 ('D07','Dời sớm/bất khả kháng; chỉ đổi giờ thành công mới supersede notice','POST /api/sessions/{id}/reschedule','Workflow4: đổi phòng giữ notice; Coach403 emergency; Manager200/retry giữ credit','So sánh cũ-mới; MEMBER_EARLY/MEMBER_EMERGENCY/CENTER_INCIDENT khác điều kiện; không dùng CRUD'),
 ('D08','Bù thời gian thực tế hợp nhất; restore balance gốc qua expiry','/api/center-incidents/{id}/resolve; /compensate; /reschedule','Workflow8: waiting xuyên expiry, thu hồi3/phục hồi3 +1credit ca bị TT hủy; retry không cộng trùng','Không reset12; chờ endedAt thật; gia hạn thêm tối thiểu có reason để đủ ca bù'),
 ('D09','Coach directory hết lỗi lazy, DTO/center/filter/contact đúng','GET /api/coaches','BACKLOG theo Owner; anonymous401 không tái hiện hoặc chứng minh đã hết LazyInitializationException','Chưa merge gói T/FE; authenticated positive/OSIV=false/N+1 vẫn cần khi nhận scope'),
 ('D10','Giữ5phút; Return chỉ đọc/IPN ghi; tiền muộn cần đối soát','/api/invoices; /api/members/me/invoices; /api/payments/vnpay/*; /api/payment-resolutions','Workflow1–3/7: signed callback local, replay/late/receipt/refund; readback hóa đơn muộn','FE không gọi IPN; không tin redirect là Paid; poll ownInvoice hữu hạn rồi Kiểm tra lại; gateway live chưa chạy'),
 ('D11','PostgreSQL local riêng; cloud smoke có kiểm soát','Không phải một endpoint; liên quan tất cả luồng kiểm','Runner committed checkout không.env + DB local; production upgrade đã có phê duyệt riêng','Không suy quyền production seed/mutation từ kiểm OpenAPI hoặc GET; không dùng tài khoản seed cũ'),
 ('D12','45ngày sử dụng skip TT đóng/nghỉ; ca kết thúc không vượt hạn; Outdoor khóa3/4','/subscriptions; /book-private; /available-slots; /holidays','EntitlementTimePolicyTest/SessionSlotValidatorTest; workflow1/5/8; deadline chính xác','Dùng expiresAt timestamp, không tính45ngày phía FE; không cộng nghỉ và incident hai lần'),
 ('D13','Report theo người và từng đăng ký;0credit còn ca vẫn còn quyền','GET /api/reports/members; /dashboard; /revenue','ReportServiceTest; workflow7/9 trước/sau hoàn tất Normal;3chỉ số tiền','Không cộng active+expired+never nếu chưa hiểu alias; Completed khác Expired; legacy tách reconciliation'),
 ('D14','Holiday preview→xử lý ảnh hưởng→confirm có token','/api/holidays/preview; POST /api/holidays; PATCH /{id}/toggle','Workflow5 blocked/stale/union/quá khứ; migrations/history proof','Gửi X-Preview-Token;409 phải preview lại; không tự di chuyển lịch hàng loạt'),
 ('D15','Present/Absent trong[start,end); Manager sửa sau buổi; bù Normal có hồ sơ','/api/training-results; /api/attendances/scan; /correct; /api/center-incidents','Workflow6/9 và attendance/finalizer tests;13physical/12valid Normal','Record mới không Late/Excused; lobby default tắt; absent Member không tự được bù'),
 ('D16','Đảo Cash hẹp Advanced chưa nhận/chưa dùng/không history; chứng từ gốc giữ','/api/payment-resolutions open/approve/complete; /api/invoices/{id}/receipt','Workflow7: có history bị Blocked; case đủ điều kiện Completed; Paid/amount gốc giữ','Approved chưa có tiền hoàn; CashCorrection không phải Refund; không áp dụng Normal'),
 ('D17','QR đúng payload/expiresInSeconds; chỉ bước hiện tại, chống replay','GET /api/attendances/me/checkin-code; POST /scan','Workflow6 và AttendanceServiceScanTest; QR/token được redact khỏi evidence','TTL động1–30s; foreground refetch; không tái dùng payload trong tài liệu; browser scan chưa nghiệm thu'),
 ('D18','Manager/Admin có VIEW_REPORTS; Coach không đọc tổng tài chính','GET /api/reports/dashboard; /members; /revenue','Workflow5 Coach403;7/9 Manager200; ReportServiceTest/AuthorizationHttpTest','Không coi MANAGE_CLASSES tự đủ quyền report; còn kiểm active/center phía service'),
 ('D19','Bằng chứng đúng SHA/môi trường; không gọi local là Live/FE E2E','Toàn bộ danh mục API và9workflow demo','Clean test/PG9workflow+1contract/SHA/trace + inventory controller + GET Render','Chưa có authenticated business matrix đầy đủ trên Render, gateway live hoặc browser FE; các con số không là quota')]
lines=['# Ma trận đối chiếu D01–D19 → API → bằng chứng → bàn giao FE','',
       'Nguồn rule: docs/04-DEBUG-NOTE.md mục0.2 và quyết định mới nhất30.3–39.3. D01/D09 nằm ngoài scope Owner đã chốt.',
       'Mọi bằng chứng workflow bên dưới là HTTP/PostgreSQL local trừ khi nói rõ production/Render. Không hàng nào tự chứng nhận mọi nhánh quyền/biên/cạnh tranh chỉ vì có một test.','',
       '| Dxx | Quyết định | API liên quan | Bằng chứng / mức hoàn tất | FE cần làm / giới hạn |',
       '|---|---|---|---|---|']
for row in requirements:lines.append('| '+' | '.join(x.replace('|','\\|') for x in row)+' |')
lines+=['','Các alias và request/response cụ thể nằm trong [api-inventory.md](api-inventory.md), [schemas.md](schemas.md), [examples.md](examples.md).',
        'D11 và D19 là yêu cầu môi trường/evidence, không phải hai endpoint. Tên gói T/P1.5 không được tính hoàn thành từ việc OTP/chat/notifications đã merge.','']
write('decision-matrix.md','\n'.join(lines))

examples=[
 ('Member chưa có Coach nhận, order bị chặn',lambda e:e['path'].endswith('/order') and e['actualStatus']==400,'Chưa accept thì chưa có Invoice/quyền mới. FE hiển thị COACH_ACCEPTANCE_REQUIRED, không tự chuyển sang thanh toán.'),
 ('Order Cash Pending, giữ5phút',lambda e:e['path'].endswith('/order') and e['actualStatus']==200 and e['responseBody'].get('paymentMethod')=='Cash','amount là snapshot3000000VND; expiresAt là hạn trả tiền, không phải45ngày sử dụng. paymentUrl null là đúng với Cash.'),
 ('Receptionist xác nhận Cash Paid',lambda e:'/confirm-cash' in e['path'] and e['actualStatus']==200,'Invoice.amount vẫn3000000 dù fixture đã đổi Subject lên3100000. Workflow đồng thời assertDB Active/12credit và deadline2026-12-12T08:00:00 (có1ngày nghỉ TT).'),
 ('Member đọc quyền sau Paid',lambda e:'/subscriptions' in e['path'] and e['method']=='GET' and e['actualStatus']==200 and isinstance(e['responseBody'],list) and e['responseBody'] and e['responseBody'][0].get('remainingCredit')==12,'Response thật gồm price đóng băng, subjectId/primaryCoachId, activatedAt/expiresAt. ID/giờ là fixture local, không hardcode vào FE.'),
 ('Member khác không đọc được Invoice',lambda e:'/api/members/me/invoices/' in e['path'] and e['actualStatus']==404,'Cùng URL invoice nhưng phiên MEMBER_B bị404. Dữ liệu không được chia sẻ chỉ vì FE biết invoiceId.'),
 ('Member tự tạo Advanced bị403',lambda e:e['path']=='/api/sessions/book-private' and e['actor']=='MEMBER_A','Ẩn nút tạo đối với Member không thay kiểm quyền BE; server thực sự từ chối.'),
 ('Coach đặt ca có consent,201',lambda e:e['path']=='/api/sessions/book-private' and e['actualStatus']==201,'SessionResponse không có remainingCredit; workflow assert12→11 và đọc lại subscriptions. Cùng requestKey/body retry trả đúng session cũ, không trừ lần hai.'),
 ('Hủy đúng12h, hoàn1credit',lambda e:e['path'].endswith('/cancel-private') and e['actualStatus']==200,'Tại giờ nghiệp vụ trong record, ca cách đúng12h. Workflow assert11→12; lần hủy lặp bị lỗi và không hoàn thêm.'),
 ('Báo vắng sát giờ tách attendance',lambda e:e['path'].endswith('/absence') and e['actualStatus']==200,'Notice.active=true không đồng nghĩa đã điểm danh. Workflow assert credit vẫn11, bảng attendance chưa có record do notice.'),
 ('Holiday preview cho thấy ảnh hưởng',lambda e:e['path']=='/api/holidays/preview' and e['actualStatus']==200 and e['responseBody'].get('blocked') is True,'FE hiển thị affectedSessionIds/deadlineChanges, yêu cầu xử lý lịch rồi preview lại. X-Preview-Token phải lấy từ preview mới, không tự tạo.'),
 ('QR có TTL hiện tại',lambda e:'checkin-code' in e['path'] and e['actualStatus']==200,'payload đã redact; expiresInSeconds là TTL thực tại thời điểm test. Mã trong tài liệu không dùng để scan. Không chứa TOTP secret hoặc cookie.'),
 ('Scan ngoài giờ ca bị chặn',lambda e:e['path'].endswith('/scan') and e['actualStatus']==400 and isinstance(e['responseBody'],dict) and e['responseBody'].get('code')=='ATTENDANCE_WINDOW_CLOSED','Biên hợp lệ là[start,end); trước start và tại end đều bị chặn. Quyền Coach/phân công và QR/replay còn được kiểm riêng.'),
 ('Đọc lại tiền muộn cần đối soát',lambda e:'/api/members/me/invoices/' in e['path'] and e['actualStatus']==200 and e['responseBody'].get('reconciliationRequired') is True,'Paid có thể đi kèm reconciliationRequired=true và0credit/Cancelled trong DB. FE hiển thị đã ghi nhận tiền/cần xử lý, không coi gói Active.'),
 ('Sự cố WaitingForRecovery',lambda e:e['path']=='/api/center-incidents' and e['actualStatus']==200 and e['responseBody'].get('status')=='WaitingForRecovery','Chưa biết kết thúc thì chưa gia hạn tạm. Manager resolve bằng endedAt thực; phần thời gian đã được Holiday bù không cộng lại.'),
 ('Bù có hồ sơ, chống làm lặp',lambda e:e['path'].endswith('/compensate') and e['actualStatus']==200,'Workflow8 kiểm restore đúng3credit expiry và1credit gốc của ca bị TT hủy; retry không nhân. Workflow9 giữ buổi gốc và thêm1makeup liên kết để đủ12buổi hợp lệ.'),
 ('Refund Approved chưa là Completed',lambda e:e['path'].endswith('/approve') and e['actualStatus']==200 and e['responseBody'].get('type')=='Refund','Approve chưa tăng moneyRefunded. Hoàn ngoàiSCMS thật, có evidence/performedAt rồi complete mới ghi kết quả.'),
 ('Refund Completed có chứng cứ',lambda e:e['path'].endswith('/complete') and e['actualStatus']==200 and e['responseBody'].get('type')=='Refund','amount lấy Invoice gốc; performedAt là kỳ hoàn thực tế, có thể khác kỳ thu. API không tự gọi gateway refund.'),
 ('Normal tạo lớp và lịch nguyên khối',lambda e:e['path']=='/api/classes/normal-with-sessions' and e['actualStatus']==201,'Trong cùng transaction tạo12buổi, rollback khi conflict. RequestId dùng retry cùng nội dung; Member không được hủy Normal vì nghỉ cá nhân.'),
 ('Báo cáo thu/hoàn/ròng khác gross',lambda e:e['path'].startswith('/api/reports/revenue?') and e['actualStatus']==200 and e['responseBody'].get('moneyRefunded',0)>0,'Demo tháng11: gross6200000, thu3100000, hoàn3100000, ròng0, điều chỉnh Cash3100000. Không dùng totalRevenue thay netReceived.'),
 ('Member report sau hoàn tất Normal',lambda e:e['path']=='/api/reports/members' and e['actualStatus']==200 and e['responseBody'].get('completedRegistrations')==1,'Hai Member:1người đã hết quyền và1người chưa từng có quyền. completedRegistrations=1 tách expiredRegistrations; không nhân số người theo số đăng ký.')]
lines=['# Ví dụ request/response thật và cách FE diễn giải','',
       'Nguồn: [http-exchanges.json](http-exchanges.json) từ9workflow HTTP/PostgreSQL local tại commit `'+verification['sourceCommit']+'`.',
       'Không phải dữ liệu production. Clock test tiến có kiểm soát từ27/10/2026 tới2027 để kiểm expiry/attendance/refund; URL loopback dùng port ngẫu nhiên. ID môn Cầu lông trong fixture có thể là5, còn production canonical26. Không copy ID/ngày/cookie/mã QR trong tài liệu để gọi Render.',
       'Fixture cố ý đổi giá Cầu lông3000000→3100000 sau đơn đầu để chứng minh price freezing; production không bị sửa giá bởi demo. Cookie/login/OTP/TOTP secret/chữ ký callback không được công bố.','']
missing=[]
for number,(title,predicate,meaning) in enumerate(examples,1):
    hit=next(((i,e) for i,e in enumerate(exchanges) if predicate(e)),None)
    if not hit:missing.append(title);continue
    index,e=hit
    lines += [f'## {number}. {title}','',f"Exchange index **{index}** (0-based), scenario `{e['scenario']}`. Actor `{e['actor']}`, businessNow `{e['businessNow']}` UTC+7.",'',
              f"`{e['method']} {e['path']}` → HTTP **{e['actualStatus']}**.",'',meaning,'']
    if e['requestBody'] is not None:lines+=['Request body:','```json',json.dumps(e['requestBody'],ensure_ascii=False,indent=2),'```','']
    lines+=['Response body (đủ field ở exchange này):','```json',json.dumps(e['responseBody'],ensure_ascii=False,indent=2),'```','']
    if e['previewHeaderPresent']:lines+=['Request còn gửi header X-Preview-Token lấy từ preview tương ứng; giá trị không tự sinh từ FE.','']
write('examples.md','\n'.join(lines))
save={'missingExamples':missing,'examples':len(examples)-len(missing),'recordedExchanges':len(exchanges),'sourceCommit':verification['sourceCommit']}
write('examples-summary.json',json.dumps(save,indent=2)+'\n')

handbook='''# Bàn giao FE SCMS — contract, bằng chứng và giới hạn

Đây là điểm bắt đầu cho FE. Rule gốc: docs04 mục0.2; contract chính: docs01. Phạm vi P1.0–P1.4/BE P2; P1.5/T/D01/D09/FE integration được Owner để lại. Kết quả route/schema/GET không được coi là nghiệm thu mọi nghiệp vụ Render.

## 1. Các file cần nhận

| File | Dùng làm gì |
|---|---|
| [api-inventory.md](api-inventory.md) / JSON | Tất cả method/path/params/body/status/auth annotation, handler source, Dxx, exchange index và GET probe |
| [schemas.md](schemas.md), [OpenAPI](render-openapi.json) | Kiểu/field/request required/enums công bố; có thể import OpenAPI vào Postman/Insomnia để đọc schema |
| [decision-matrix.md](decision-matrix.md) | D01–D19 → policy → API → bằng chứng → phần chưa nghiệm thu |
| [examples.md](examples.md), [http-exchanges.json](http-exchanges.json) | Request, actor, businessNow, status/body thật cùng cách diễn giải |
| [demo](demo/demo.html), [manifest](demo/verification.json), [test summary](demo/test-summary.txt) | Xem và kiểm lại9workflow local/DB assertions cùng regression contract theo đúng source SHA |
| [audit-summary.json](audit-summary.json), [render-get-checks.json](render-get-checks.json) | Ranh giới kiểm chứng trên Render; không chứa phiên người dùng/PII protected response |

## 2. Kết nối và đăng nhập

Base Render: https://scms-app-p1gy.onrender.com . Base local: http://localhost:8080 hoặc port runner ghi trong trace/log. POST /api/auth/login bằng tài khoản được cấp, giữ JSESSIONID trong cookie jar. Fetch dùng credentials:'include', Axios dùng withCredentials:true; không gắn bearer token thay session. GET /api/auth/me lấy danh tính/role/permission hiện tại; memberId từ phiên/response đăng nhập, không cho người dùng tự chọn Member khác.

Cross-site FE HTTPS cần exact origin CORS, allowCredentials và cookie SameSite=None/Secure=true. Source default lax/false nên chưa suy cấu hình dashboard đã đúng; cần browser kiểm Set-Cookie, cookie gửi lại, preflight và actual response. Swagger/Postman không chứng minh browser cookie/CORS.

## 3. Vai trò và màn hình

| Vai trò | Hành động chính | Điều không được suy |
|---|---|---|
| Member | Tạo registration, đọc của mình, order, ownInvoice/subscriptions/lịch, hủy đúng hạn, notice, QR | Không tự tạo Advanced session; không coi biết invoiceId/memberId là được đọc của người khác |
| Primary Coach | Nhận/từ chối registration được giao, book/dời hợp lệ sau trao đổi, attendance trong giờ buổi | Không đọc tổng tài chính; không thay được quyền Manager chỉ vì có MANAGE_CLASSES |
| Receptionist + MANAGE_INVOICES | Xác nhận Cash, đọc chứng từ/tiếp nhận case theo quyền | Manager có MANAGE_INVOICES vẫn không tự xác nhận Cash nếu thiếu ROLE_RECEPTIONIST |
| CenterManager/Admin + authority | Work-schedule, hỗ trợ có lý do/consent, incident/Holiday, approve/complete/report trong center | Role name chưa đủ; service còn kiểm active/owner/center/permission/trạng thái |

Chi tiết annotation từng route có trong inventory; service guards không thể suy đầy đủ chỉ từ OpenAPI. Generic staff read endpoints chưa có full cross-center HTTP matrix trong audit này. Không ký nghiệm thu toàn bộ phân quyền từ74GET anonymous.

## 4. Chuỗi màn hình Advanced

1. Login→GET subjects(cógiá), Coach directory(D09 còn backlog), work-schedule/admission-capacity. ID lookup theo môi trường; chọn Coach chưa giữ tài nguyên.
2. Member POST registration(subjectId,primaryCoachId,requestKey)→Requested; Coach decision accepted→Accepted. Chưa accept thì order bị chặn.
3. Member POST order(paymentMethod Cash/VNPay)→Invoice/Subscription Pending, amount snapshot, expiresAt thanh toán5phút. Giữ requestKey khi retry cùng thao tác; đổi dữ liệu phải tạo thao tác/key mới.
4. Cash chờ Receptionist confirm; online chỉ điều hướng paymentUrl từ BE. Return hiển thị kết quả/redirect, FE đọc ownInvoice, poll hữu hạn rồi nút Kiểm tra lại. FE không gọi IPN và không tự đặt Paid/Active.
5. Paid bình thường→đọc subscriptions Active/12credit/deadline. Paid+reconciliationRequired→hiển thị cần đối soát, không giả quyền. Legacy null subject/Coach/timestamps→cần hỗ trợ đối soát, không gán12credit/45ngày từ UI.
6. Coach/Manager book có consent/requestKey; response Session201, balance đọc lại subscriptions. Hủy>=12h hoàn1; dưới12h dùng notice riêng. Khi dời, so cũ-mới; không xóa notice ở UI trước khi BE xác nhận đổi thời gian thành công.
7. Sự cố/Holiday có hồ sơ/preview/token/audit. Refresh nguồn gói/lịch sau workflow; không vá deadline/credit phía FE.

## 5. State và dữ liệu dễ nhầm

| Dữ liệu | Nguồn/ý nghĩa |
|---|---|
| registration.status | Requested/Accepted/Rejected/Invoiced; Invoiced không đồng nghĩa Paid |
| order.status / Invoice.paymentStatus | Trạng thái tiền; Paid có thể cần đối soát |
| subscription.status | Quyền Pending/Active/Cancelled/Expired/Completed, riêng từng đăng ký |
| order.expiresAt | Hạn thanh toán5phút |
| subscription.expiresAt | Hạn quyền45ngày sử dụng đã tính skip TT nghỉ/đóng, dùng timestamp chính xác |
| Invoice.amount / subscription.price | Giá snapshot đã mua; không tính lại từ catalog hiện tại |
| Session.remainingCredit | Không có field này; tải lại subscriptions |
| remainingCredit=0 | Không có nghĩa mất mọi quyền nếu còn ca hợp lệ đã book |
| totalRevenue | Gross Paid lịch sử; không thay moneyReceived/moneyRefunded/netReceived |
| Refund Approved | Chưa hoàn tiền; Completed/evidence/performedAt mới ghi hoàn |
| CashCorrection | Xác minh chưa nhận tiền, không phải hoàn tiền; chứng từ Paid gốc được giữ |

Ngày giờ business LocalDateTime không offset được đọc như UTC+7; chuỗi đã cóZ/+07:00 giữ nguyên. Không dùng Date máy người dùng để tự quyết Paid/expiry. Record list có thể[]; notification/chat dùng phân trang, đọc đúng schema content/Page chứ không dùng một parser mảng cho tất cả.

## 6. Normal, attendance và QR

Ưu tiên POST /api/classes/normal-with-sessions tạo nguyên khối12buổi. Nếu tách bước: POST Class→Closed, generate đủ12 rồi PUT Open. Sĩ số Normal≤15, Registered+Pending giữ chỗ; đầy có thể Waitlisted, không mặc định lỗi capacity. Enrollment Pending chưa được học; Paid mới Registered. Normal không hủy/notice vì lý do cá nhân.

Chỉ buổi thật sự không tổ chức do TT có hồ sơ verifiedNotHeld mới được bù; giữ buổi gốc centerInvalidated và link đúng1makeup.13record vật lý có thể chỉ12buổi hợp lệ. Attendance mới Present/Absent trong[start,end), correction sau end bởi Manager có audit. Lobby check-in/out default tắt; không dùng thay attendance buổi.

QR response chỉ payload/expiresInSeconds, TTL còn lại1–30s. Refetch khi foreground/hết hạn; không log payload/secret. Evidence redact payload, nên mã trong tài liệu không thể dùng scan. Coach chỉ scan buổi được giao, replay bị chặn; tại end không còn hợp lệ.

## 7. Xử lý lỗi và HTTP

Phân biệt201(body tạo mới),200(body/void theo route),204(no body: không gọi response.json()). BusinessException có code/message, nhưng legacy/validation/401/403 có thể timestamp/status/error/message hoặc body framework khác. FE ưu tiên code→message→HTTP fallback, không hardcode mọi lỗi đều có code.409 tải lại trạng thái/preview, không lặp vô hạn;401 đăng nhập lại;403 hiển thị thiếu quyền;404 owner-hidden/không tìm thấy dùng thông báo chung.

X-Preview-Token lấy từ preview; stale/blocked không có nghĩa request đã commit. IDEMPOTENCY_CONFLICT giữ lại kết quả cũ và báo người dùng khi key trùng nhưng nội dung khác, không tạo lại nhiều hóa đơn/buổi một cách mù quáng.

## 8. Tái lập và nhận diện bằng chứng

```powershell
docker start scms-phases-20261027
.\\scripts\\run-phases-demo.ps1
```

Runner tạo detached checkout từ HEAD không.env, DB mới loopback127.0.0.1:55440; clean test, migrations/replay/snapshot/FK guard rồi9HTTPworkflow +1regression OpenAPI với login thật và assertDB sau commit. Có Clock test để đi qua biên12h/5phút/expiry, không giả status Paid/Active bằng SQL để thay luồng thanh toán. Một số thay giá/clock/fixture cố ý là setup kiểm biên, được giải thích trong examples. Lệnh không tự kết nối Render, gửi email hoặc dùng tiền thật. Không chạy Maven clean đồng thời trên cùng target. Worktree/DB/log giữ local để kiểm, không reset DB có sẵn.

Demo HTML chỉ xem evidence, không phải FE tích hợp hoặc một phiên server đang mở để Postman replay tự do. Muốn gọi bằng FE thật cần khởi động BE local với fixture/múi giờ/role tương ứng; không copy request ngày/ID giả từ examples sang production.

## 9. Các cổng còn phải nghiệm thu

Authenticated business/role/owner/center/full negative matrix trên Render chưa chạy, vì audit không có phiên tài khoản test và không được suy quyền mutation production từ phê duyệt nâng schema. VNPay callback được ký local bằng test key; chưa gateway sandbox live. FE browser/cookie/CORS và camera QR chưa chạy. SQL Server runtime chưa chạy. D01/D09 vẫn backlog. Render Free chặn SMTP587: db UP/mail DOWN, cần chọn email HTTPS provider/API key/sender và làm adapter để OTP cloud hoạt động.

Tách kết quả schema/route, anonymous gate, workflow local và production business acceptance. Gói này bàn giao đủ danh mục/schema và ví dụ có nguồn; không đánh dấu những cổng còn thiếu thành PASS.
'''
handbook+=f"\nKết quả hiện tại: {audit['operations']}operations / {audit['paths']}paths / {audit['schemas']}schemas, source route match {audit['sourceHandlersMatched']}; GET anonymous {audit['renderAnonymousGetPassed']}/{audit['renderAnonymousGetChecks']}; local exchanges {len(exchanges)} trên {audit['operationsWithLocalExchanges']}operations. Clean {verification['tests']}run/{verification['tests']-verification['skipped']}pass/{verification['skipped']}skip; PG{verification['postgresHttpTests']}/{verification['postgresHttpTests']} (9workflow nghiệp vụ +1regression contract). Live identity/SHA và CI xem audit-summary/docs02 mục10.11.\n"

write('FE-HANDOFF.md',handbook)
print(json.dumps({'requirements':len(requirements),'examples':save['examples'],'missingExamples':missing,'exchanges':len(exchanges)}))
if missing:raise SystemExit(2)
