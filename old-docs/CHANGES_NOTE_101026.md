# Ghi chú bàn giao thay đổi ngày 10/10/2026 — BE, FE và demo Cash

Tài liệu này để team FE hiểu thay đổi mà không cần đọc lại hội thoại, tự chọn mục cần áp dụng và giữ quyền quyết định giao diện của FE chính. **Các đề xuất áp dụng trong tài liệu chưa phải quyết định của team FE.**

Phạm vi nghiệp vụ: **bộ môn → lớp → buổi học**, học phí tính cho cả lớp; đăng ký/đăng nhập → mua gói → lễ tân thu Cash → ghi danh lớp → thu học phí → coach điểm danh → member feedback lớp/mentor → manager/coach xem. Email OTP thật và thanh toán online được hoãn; OTP đăng ký `123456` dùng với profile BE `demo`. Chưa thêm đối tượng khóa học độc lập chứa nhiều lớp.

**Điểm cần biết trước khi đọc:** đã có sửa FE và thêm màn hình demo. Không thể xem kết quả demo hoàn tất là bằng chứng FE nguyên bản đã nối xong. Người dùng sau đó xác nhận **không được sửa FE**; các thay đổi FE được ghi lại để team FE đánh giá, chưa tự hoàn tác. Lượt viết tài liệu này chỉ tạo tài liệu, không sửa BE/FE/DB.

## Cách đọc và cơ sở đối chiếu

Đọc nhanh để quyết định tích hợp: [contract API](#17-contract-tối-thiểu-cho-team-fe-triển-khai-trên-ui-chính) → [màn hình/route demo](#14-fe--màn-hình-demo-mới-và-route-bị-thay) → [bảng mức cần thiết](#21-phân-loại-để-team-fe-quyết-định) → [checklist quyết định](#26-checklist-để-team-fe-chốt-từng-phần--chưa-áp-dụng-tự-động). Các mã F01–F13 và B01–B09 giúp trao đổi từng mục mà không cần copy toàn bộ tài liệu.

- Phần 1: từng thay đổi, file/hàm/vị trí, code trước–sau, nguyên nhân và ảnh hưởng. Có contract request/response để FE triển khai lại trên UI hiện tại.
- Phần 2: những phần phục vụ demo/kiểm tra, có cần cho FE chính không, phụ thuộc và checklist để team FE chốt từng mục.
- **BE:** các sửa bổ sung được đối chiếu working tree với Git `HEAD`. Các module feedback/payment đã có trong bản code được audit; không gán lại toàn bộ module đó cho lần sửa sau audit.
- **FE:** đối chiếu `FE_Project-main/` với `FE_Project-main.zip`, bỏ khác biệt CRLF/LF. Có **8 file source sửa, 2 file test sửa, 2 file source mới**. Thư mục FE hiện chưa được Git theo dõi trong repo gốc; `git diff` ở repo gốc không đủ để kiểm kê thay đổi FE.
- Số dòng/vị trí là của bản source tại thời điểm lập tài liệu. Dùng tên hàm/chuỗi code trong từng mục để tìm lại nếu source được chỉnh sau này. Link file tương đối tính từ thư mục gốc repo.
- Các file dưới `SCMS_demo_fe_v1.1/` đang bị đánh dấu xóa và thư mục `SCMS_demo_fe_v1.1 - old/` là trạng thái có trước; không tính các mục đó là thay đổi đã thực hiện trong đợt triển khai này.
- Chưa commit/push. Không chạy migration/schema mới trong đợt audit và hoàn thiện demo này. Dữ liệu DEMO và smoke test có được ghi vào DB; xem phần 2.

# PHẦN 1 — Từng thay đổi, code cụ thể, vị trí và giải thích

## 1.1. Chức năng BE đã có trước lần sửa bổ sung

[CHANGES_2026-10-10.md](CHANGES_2026-10-10.md) ghi lại đợt thay đổi BE trước audit. Những chức năng dưới đây là cơ sở mà FE phải tích hợp; không phải toàn bộ đều là code mới viết trong lượt nối demo.

| Nhóm | Source/SQL để tìm | Hành vi hiện có và ý nghĩa với FE |
|---|---|---|
| Ghi danh và học phí | `src/main/java/com/sportscenter/enrollment/`, `sportclass/`, `invoice/` | Lớp có phí tạo invoice và enrollment `Pending`; lớp miễn phí ghi danh `Registered`. Khóa dòng lớp khi giữ chỗ; Pending tính vào sức chứa. |
| Mua gói | `membership/MemberPackageService.java`, `MemberPackageController.java`, `SubscriptionOrderResponse.java` | Mua gói trả **đơn thanh toán**, tạo subscription/invoice Pending; sau Paid mới Active. Endpoint `packages` được giữ dưới dạng alias của `subscriptions` cho staff. |
| Hóa đơn và Cash | `invoice/InvoiceController.java`, `InvoiceService.java`, `InvoiceResponse.java` | Member đọc hóa đơn riêng; receptionist xác nhận Cash; amount lấy DB, người thu lấy session. Hủy/hết hạn cập nhật đối tượng liên kết. |
| Feedback | `feedback/ClassFeedbackController.java`, `ClassFeedbackService.java`, `ClassFeedbackRepository.java`, `ClassFeedback.java`, các DTO `ClassFeedback*` và `CoachFeedback*` | Một member gửi một feedback/lớp, gồm điểm lớp và coach. Chỉ sau Present, enrollment Registered/Completed, lớp Ongoing/Closed. Manager xem tổng hợp; coach xem phản hồi không có danh tính member. |
| Online đã có code BE | `invoice/VnPayGateway.java`, `PaymentProperties.java`, `PaymentExpirationJob.java`, callback trong `InvoiceController.java` | Code hỗ trợ VNPay và hết hạn đã có. **Không có nghiệm thu thanh toán online trong đợt demo Cash này**, không suy ra đã chạy được chỉ vì endpoint tồn tại. |
| DB | `schema_postgres.sql`, `migration_class_feedback.sql`, `migration_payment.sql` | Có bảng feedback, học phí lớp, liên kết invoice với class/subscription và constraint/index chống trùng. Các cấu trúc quan trọng đã hiện diện trên Supabase khi audit. |
| Contract/tài liệu | `FE-BE.md`, `README_BE_CHANGES_2609.md`, `CHANGES_2026-10-10.md` | Ghi lại contract BE trước lần hoàn thiện demo. Câu “Không sửa frontend” trong CHANGES là trạng thái đợt BE đó; FE đã bị sửa ở lượt triển khai sau và được kê đầy đủ dưới đây. |

**Các quy tắc cần giữ dù chọn UI nào:** không tự tạo giá/amount để BE tin; không cho member tự xác nhận Cash; không gán Active/Registered/Paid chỉ do nút FE được bấm; không coi Pending là đã thanh toán; không suy ra member ID từ mã hiển thị; đọc lại BE sau mutation/F5.

Các chi tiết payment đã có trong CHANGES cần phân biệt với phạm vi nghiệm thu Cash:

- Hóa đơn Pending mặc định hết hạn sau 15 phút, job xử lý mỗi phút theo cấu hình. Gói Pending liên kết bị hủy khi hết hạn. Lớp Cash và lớp VNPay có cách xử lý hết hạn khác nhau trong code hiện tại; không dùng một giả định chung cho mọi phương thức.
- Gửi mua lại cùng gói có đơn Pending: service có logic hủy đơn Pending cũ trước khi tạo đơn mới. Không thiết kế FE dựa trên giả định mọi lần mua lại đều bị từ chối do unique index.
- Code VNPay đã có xác minh chữ ký, amount, transaction reference và chống xử lý lặp; chỉ callback đã xác minh mới quyết định Paid. Return URL phía browser không phải bằng chứng thanh toán thành công.
- Với gói, callback hợp lệ đến sau hạn vẫn có đường ghi Paid/kích hoạt và audit `PAID_AFTER_EXPIRY`. Với lớp VNPay, suất Pending được giữ sau hạn cho tới callback hợp lệ: thành công Registered, thất bại hủy/nhả chỗ; không có callback thì có thể tiếp tục giữ chỗ. Đây là hành vi BE đã ghi trong CHANGES, **chưa được nghiệm thu online trong đợt này**.
- Các cấu hình VNPay/thời hạn thanh toán, hai GET callback public và phần loại trừ tương ứng ở FirstLoginPasswordFilter thuộc đợt BE trước audit. Không bổ sung cổng thanh toán khác hoặc UI online trong lượt demo Cash.

## 1.2. FE — đăng ký, OTP và phiên đăng nhập

### F01 — `src/api/authApi.js`: thêm 3 hàm đăng ký

File: [authApi.js](FE_Project-main/src/api/authApi.js), dòng 11–13. `login`, `getCurrentUser`, `logout` có trước và không đổi.

```diff
--- ZIP/src/api/authApi.js
+++ CURRENT/src/api/authApi.js
@@ -9,2 +9,5 @@
 export const getCurrentUser = () => httpClient.get('/api/auth/me', { notifyUnauthorized: false });
 export const logout = () => httpClient.post('/api/auth/logout');
+export const register = (data) => httpClient.post('/api/auth/register', data);
+export const verifyRegistration = (email, otp) => httpClient.post('/api/auth/verify-registration', { email, otp });
+export const resendOtp = (email) => httpClient.post('/api/auth/resend-otp', { email, purpose: 'REGISTER' });
```

**Vì sao:** LoginView cũ chuyển sang OTP bằng timeout, so sánh `123456` tại FE và thêm member theo nhánh giả lập. Ba hàm mới đưa đăng ký/xác thực/gửi lại về BE. Dù OTP hardcode trong demo, BE vẫn xử lý hash, hạn dùng, số lần thử và tài khoản.

**Team FE có thể áp dụng:** ba hàm hoặc viết tương đương trong auth service hiện có; không cần đổi route/dashboard. Giữ payload đúng contract ở mục 1.7.

### F02 — `src/components/views/LoginView.jsx`: đổi handler và nội dung UI

File: [LoginView.jsx](FE_Project-main/src/components/views/LoginView.jsx). Vị trí: `validateRegister` dòng 234; `handleRegister` 277; `handleVerifyOtp` 331; `handleFinishRegisterAndLogin` 369; nội dung OTP khoảng 843; handler gửi lại khoảng 891.

| Phần | Trước | Sau | Ảnh hưởng |
|---|---|---|---|
| `validateRegister()` | Mật khẩu tối thiểu 6 ký tự | Tối thiểu 8; khi API bật phải có chữ và số | Khớp BE, nhưng điều kiện 8 ký tự cũng áp dụng cho mock. Team FE có thể tách validation demo/mock theo quy ước của mình. |
| `handleRegister()` | Timeout rồi hiện OTP | Async gọi `authApi.register`, thành công mới hiện OTP | Gửi password thật, chuẩn hóa email/phone; lỗi được đưa vào trạng thái lỗi form. |
| `handleVerifyOtp()` | So mã tại FE rồi `addMember()` | Nhánh API gọi `verifyRegistration`, hiện màn thành công | Không đi qua staff API tạo member; nhánh mock cũ vẫn còn. |
| `handleFinishRegisterAndLogin()` | Gọi thành công bằng role `'member'` | Gọi context `login(email, password)` rồi `onLoginSuccess(user)` | Xác thực OTP thành công không tự đồng nghĩa đã có phiên đăng nhập; cần login thật. |
| Nội dung OTP | Thông báo đã gửi email | Hiện “Mã OTP demo: 123456” | Thay đổi nội dung hiển thị. Hiện đoạn text này không được giới hạn riêng bằng profile demo từ BE; không nên đưa nguyên xi vào FE chính dùng email thật. |
| Nút gửi lại | Toast giả lập | Nhánh API gọi `resendOtp`, thành công xóa 6 ô | Lỗi quota/thời gian gửi lại do BE trả; không chỉ hiển thị thông báo thành công tại FE. |

Code thay đổi chính xác:

```diff
--- ZIP/src/components/views/LoginView.jsx
+++ CURRENT/src/components/views/LoginView.jsx
@@ -7,4 +7,5 @@
 import { normalizeRole, resolveMockLoginUser, ROLE_ADMIN } from '../../utils/navigation';
 import { PublicFooter } from '../layout/PublicFooter';
+import * as authApi from '../../api/authApi';
 import {
   Activity,
@@ -257,6 +258,6 @@
     if (!regPassword) {
       errors.password = isVi ? 'Vui lòng nhập mật khẩu.' : 'Password is required.';
-    } else if (regPassword.length < 6) {
-      errors.password = isVi ? 'Mật khẩu tối thiểu 6 ký tự.' : 'Password must be at least 6 characters.';
+    } else if (regPassword.length < 8 || (apiEnabled && (!/[a-zA-Z]/.test(regPassword) || !/\d/.test(regPassword)))) {
+      errors.password = isVi ? 'Mật khẩu tối thiểu 8 ký tự, có chữ và số.' : 'Use at least 8 characters, including letters and numbers.';
     }
 
@@ -274,9 +275,18 @@
 
   // Handle Register Submit -> Go to OTP
-  const handleRegister = (e) => {
+  const handleRegister = async (e) => {
     e.preventDefault();
     if (!validateRegister()) return;
 
     setIsRegLoading(true);
+    if (apiEnabled) {
+      try {
+        await authApi.register({ fullName: regName.trim(), email: regEmail.trim().toLowerCase(), phone: regPhone.replace(/\s/g, ''), password: regPassword });
+        setIsOtpMode(true);
+      } catch (error) {
+        setRegErrors({ email: getErrorMessage(error, 'Không thể đăng ký.') });
+      } finally { setIsRegLoading(false); }
+      return;
+    }
     setTimeout(() => {
       setIsRegLoading(false);
@@ -319,5 +329,5 @@
   };
 
-  const handleVerifyOtp = (e) => {
+  const handleVerifyOtp = async (e) => {
     e.preventDefault();
     const code = otpCode.join('');
@@ -328,4 +338,13 @@
 
     setIsOtpVerifying(true);
+    if (apiEnabled) {
+      try {
+        await authApi.verifyRegistration(regEmail.trim().toLowerCase(), code);
+        setIsOtpMode(false);
+        setRegisteredSuccessData({ name: regName.trim(), email: regEmail.trim().toLowerCase(), phone: regPhone, packageName: 'Chưa đăng ký gói' });
+      } catch (error) { setOtpError(getErrorMessage(error, 'Không thể xác thực OTP.')); }
+      finally { setIsOtpVerifying(false); }
+      return;
+    }
     setTimeout(() => {
       setIsOtpVerifying(false);
@@ -348,6 +367,11 @@
   };
 
-  const handleFinishRegisterAndLogin = () => {
+  const handleFinishRegisterAndLogin = async () => {
     if (registeredSuccessData) {
+      if (apiEnabled) {
+        try { onLoginSuccess(await login(registeredSuccessData.email, regPassword)); }
+        catch (error) { setOtpError(getErrorMessage(error, 'Đăng nhập thất bại. Vui lòng quay lại đăng nhập.')); navigate('/login', { state: { loginEmail: registeredSuccessData.email } }); }
+        return;
+      }
       onLoginSuccess('member');
     }
@@ -817,6 +841,6 @@
               <p className="text-xs sm:text-sm text-stone-500 mt-2 leading-relaxed">
                 {isVi
-                  ? <>Mã OTP 6 số đã được gửi tới <strong>{regEmail || 'email của bạn'}</strong>. Vui lòng kiểm tra hộp thư đến hoặc mục thư rác.</>
-                  : <>A 6-digit OTP has been sent to <strong>{regEmail || 'your email'}</strong>. Please check your inbox or spam folder.</>}
+                  ? <>Xác thực tài khoản <strong>{regEmail || 'email của bạn'}</strong>. Mã OTP demo: <strong>123456</strong>.</>
+                  : <>Verify <strong>{regEmail || 'your email'}</strong>. Demo OTP: <strong>123456</strong>.</>}
               </p>
             </div>
@@ -865,6 +889,11 @@
                 type="button"
                 className="text-[#BA0000] font-semibold hover:underline cursor-pointer"
-                onClick={() => {
+                onClick={async () => {
                   setOtpError('');
+                  if (apiEnabled) {
+                    try { await authApi.resendOtp(regEmail.trim().toLowerCase()); setOtpCode(['', '', '', '', '', '']); }
+                    catch (error) { setOtpError(getErrorMessage(error, 'Không thể gửi lại OTP.')); }
+                    return;
+                  }
                   // Mock resend
                   const toast = document.createElement('div');
```

**Quyết định cần FE chốt:** lấy logic gọi API vào form đăng ký hiện có; giữ/chỉnh UX lỗi; chỉ hiện mã OTP khi môi trường demo được xác nhận. Mình không sửa lại các mục đó trong lượt lập tài liệu này.

## 1.3. FE — thanh toán, DTO và dữ liệu thật

### F03 — `src/api/invoiceApi.js`: cập nhật endpoint và chặn thao tác cũ

File: [invoiceApi.js](FE_Project-main/src/api/invoiceApi.js). Các hàm: `getInvoices` dòng 5; `createInvoice` 46; `payInvoice` 50; `updateInvoiceStatus` 58; `invoiceApi.getOwnInvoices` 73.

```diff
--- ZIP/src/api/invoiceApi.js
+++ CURRENT/src/api/invoiceApi.js
@@ -1,9 +1,4 @@
 import { httpClient } from './httpClient.js';
-import {
-  mapInvoice,
-  toInvoiceRequest,
-  toInvoicePaymentRequest,
-  toInvoiceStatusRequest,
-} from './invoiceMappers.js';
+import { mapInvoice } from './invoiceMappers.js';
 import { extractList, isValidNumericId } from './schemaValidators.js';
 
@@ -32,4 +27,5 @@
     query.set('endDate', filters.endDate);
   }
+  if (filters.keyword) query.set('keyword', filters.keyword);
 
   const queryString = query.toString();
@@ -49,7 +45,5 @@
 
 export const createInvoice = async (data) => {
-  const payload = toInvoiceRequest(data);
-  const created = await httpClient.post('/api/invoices', payload);
-  return mapInvoice(created);
+  throw new Error('Hóa đơn được tạo khi đăng ký gói hoặc lớp; dùng API đăng ký tương ứng.');
 };
 
@@ -58,6 +52,5 @@
     throw new Error('ID hóa đơn không hợp lệ.');
   }
-  const payload = toInvoicePaymentRequest(paymentData);
-  const response = await httpClient.post(`/api/invoices/${id}/pay`, payload);
+  const response = await httpClient.post(`/api/invoices/${id}/confirm-cash`);
   return mapInvoice(response);
 };
@@ -67,6 +60,6 @@
     throw new Error('ID hóa đơn không hợp lệ.');
   }
-  const payload = toInvoiceStatusRequest({ status });
-  const response = await httpClient.patch(`/api/invoices/${id}/status`, payload);
+  if (!['cancelled', 'canceled'].includes(String(status).toLowerCase())) throw new Error('Chỉ hủy hóa đơn Pending; xác nhận Cash qua thao tác thu tiền.');
+  const response = await httpClient.post(`/api/invoices/${id}/cancel`);
   return mapInvoice(response);
 };
@@ -78,4 +71,5 @@
   payInvoice,
   updateInvoiceStatus,
+  getOwnInvoices: async () => (await httpClient.get('/api/members/me/invoices')).map(mapInvoice),
 };
 
```

Giải thích từng thay đổi:

1. `getInvoices(filters)` thêm `keyword`. Dùng cho staff; member dùng `getOwnInvoices`, không dùng danh sách `/api/invoices` của staff.
2. `createInvoice(data)` **vẫn còn tên hàm nhưng luôn throw**. Không còn gọi `POST /api/invoices`. Hóa đơn được tạo qua mua gói/ghi danh lớp, không phải FE gửi amount để tạo tùy ý.
3. `payInvoice(id, paymentData)` giữ chữ ký cũ nhưng **bỏ qua paymentData**, gọi `POST .../confirm-cash` không body. Không dùng cho online; receptionist ID và amount không do FE quyết định.
4. `updateInvoiceStatus(id, status)` chỉ nhận `cancelled`/`canceled` không phân biệt hoa thường, gọi POST cancel. Yêu cầu `Paid`/status tùy ý sẽ bị từ chối ngay tại FE.
5. `getOwnInvoices()` đọc hóa đơn của member đang đăng nhập, ánh xạ bằng `mapInvoice`.

**Ảnh hưởng tương thích:** màn hình/modal cũ gọi `createInvoice`, `/pay` hoặc đổi status trực tiếp có thể không chạy theo cách cũ. Team FE phải kiểm tra caller trước khi lấy file này; không chỉ copy adapter rồi mặc định UX hiện tại tương thích. Với FE chính có thể đặt tên rõ hơn `confirmCash`/`cancelInvoice` và sửa caller có kiểm soát.

### F04 — `src/api/invoiceMappers.js`: giữ field invoice thật

File: [invoiceMappers.js](FE_Project-main/src/api/invoiceMappers.js), `mapInvoice()` dòng 30, object trả về khoảng 62.

```diff
--- ZIP/src/api/invoiceMappers.js
+++ CURRENT/src/api/invoiceMappers.js
@@ -63,5 +63,10 @@
     id: invoiceId,
     invoiceId,
-    code: formatInvoiceCode(invoiceId),
+    code: item.invoiceCode || formatInvoiceCode(invoiceId),
+    invoiceCode: item.invoiceCode ?? null,
+    createdAt: item.createdAt ?? null,
+    expiresAt: item.expiresAt ?? null,
+    classId: asNumber(item.classId),
+    className: item.className ?? '',
     memberId,
     memberName: item.memberName || '',
```

**Vì sao:** mã hóa đơn phải dùng `invoiceCode` BE cấp để lễ tân tìm đúng; `expiresAt` phục vụ hạn thu; `classId/className` phân biệt học phí với gói. Hàm cũ `formatInvoiceCode` vẫn làm fallback, chưa bị xóa.

**Team FE chú ý:** mapper cũ chuẩn hóa status về quy ước FE, ví dụ Paid → `successful`; `demoApi` trả DTO BE nguyên bản với `Paid`. Không trộn hai loại dữ liệu rồi so sánh status bằng cùng một literal.

### F05 — `src/api/membershipApi.js`: response đăng ký gói đổi cách xử lý

File: [membershipApi.js](FE_Project-main/src/api/membershipApi.js), `subscribePackage()` dòng 56.

```diff
--- ZIP/src/api/membershipApi.js
+++ CURRENT/src/api/membershipApi.js
@@ -60,5 +60,6 @@
   const payload = toMemberPackageRequest(data);
   const response = await httpClient.post(`/api/members/${memberId}/packages`, payload);
-  return mapMemberSubscription(response);
+  if (!isValidNumericId(response?.subscriptionId) || !isValidNumericId(response?.invoiceId)) throw new Error('Backend chưa trả đầy đủ mã đăng ký gói và hóa đơn.');
+  return response;
 };
 
```

**Trước:** POST `/api/members/{memberId}/packages` rồi `mapMemberSubscription(response)`, coi kết quả là một subscription.

**Sau:** vẫn giữ endpoint alias đó; yêu cầu response có `subscriptionId` và `invoiceId`, rồi trả **order DTO nguyên bản**. `status` ở `SubscriptionOrderResponse` là trạng thái invoice, chưa phải bằng chứng gói Active. Cần đọc GET subscriptions sau thanh toán.

**Rủi ro với caller:** code đang mong `id`, status chuẩn hóa hoặc thông tin package như subscription có thể thiếu field. Kiểm tra các modal/context dùng `subscribePackage`. Member tự mua nên gọi `/api/members/me/subscriptions`; endpoint có `{memberId}` dành cho nhân viên có quyền đăng ký hộ.

### F06 — `src/api/mappers.js`: field học phí lớp

File: [mappers.js](FE_Project-main/src/api/mappers.js), `mapClass()` dòng 78, phần price khoảng 199; `toClassRequest()` dòng 264.

```diff
--- ZIP/src/api/mappers.js
+++ CURRENT/src/api/mappers.js
@@ -197,5 +197,5 @@
   else if (catLower.includes("bóng bàn")) defaultPrice = 600000
 
-  const price = asNumber(item.price) ?? asNumber(merged.price) ?? defaultPrice
+  const price = asNumber(item.tuitionFee) ?? asNumber(item.price) ?? asNumber(merged.price) ?? defaultPrice
 
   return {
@@ -269,5 +269,5 @@
   startDate: item.startDate ?? item.date ?? null,
   endDate: item.endDate ?? item.date ?? null,
-  price: asNumber(item.price),
+  tuitionFee: asNumber(item.price ?? item.tuitionFee) ?? 0,
   status:
     item.apiStatus ?? (item.status === "published" ? "Open" : item.status),
```

**Vì sao:** contract BE dùng `tuitionFee`; DTO lớp có thể không có `price`. Giá bằng 0 là lớp miễn phí hợp lệ, phải giữ 0 thay vì lấy giá mẫu.

**Team FE chú ý:** trong `toClassRequest`, `item.price` đang có thứ tự ưu tiên trước `item.tuitionFee`. Nếu model FE giữ cả hai và chúng khác nhau, cần chọn một nguồn dữ liệu rõ ràng. Đây là điểm cần review khi áp dụng, chưa sửa thêm trong lượt tài liệu.

### F07 — `src/context/SCMSContext.jsx`: không dùng lớp mock khi API lỗi

File: [SCMSContext.jsx](FE_Project-main/src/context/SCMSContext.jsx), `SCMSProvider`; state classes dòng 922, effect tải member/staff 1094, effect tải manager 1107.

```diff
--- ZIP/src/context/SCMSContext.jsx
+++ CURRENT/src/context/SCMSContext.jsx
@@ -920,5 +920,5 @@
   const [staffError, setStaffError] = useState(null)
   const [packages, setPackages] = useState(API_ENABLED ? [] : INITIAL_PACKAGES)
-  const [classes, setClasses] = useState(INITIAL_CLASSES)
+  const [classes, setClasses] = useState(API_ENABLED ? [] : INITIAL_CLASSES)
   const [subjects, setSubjects] = useState(INITIAL_SUBJECTS)
   const [rooms, setRooms] = useState(INITIAL_ROOMS)
@@ -1098,5 +1098,4 @@
       ROLE_RECEPTIONIST,
       ROLE_ADMIN,
-      ROLE_COACH,
     ]
     if (allowedRoles.includes(authUser.role)) {
@@ -1139,10 +1138,5 @@
         setSubjects(loadedSubjects)
         setRooms(loadedRooms)
-        const finalClasses =
-          loadedClasses && loadedClasses.length > 0
-            ? loadedClasses
-            : INITIAL_CLASSES
-        const activeClasses = filterActiveClasses(finalClasses)
-        setClasses(activeClasses.length > 0 ? activeClasses : INITIAL_CLASSES)
+        setClasses(filterActiveClasses(loadedClasses || []))
         setPackages(loadedPackages)
         setDashboard(loadedDashboard)
@@ -1150,6 +1144,5 @@
       } catch (error) {
         if (!active) return
-        const fallback = filterActiveClasses(INITIAL_CLASSES)
-        setClasses(fallback.length > 0 ? fallback : INITIAL_CLASSES)
+        setClasses([])
         const message = getErrorMessage(error)
         setManagerDataError(message)
```

**Vì sao và hành vi:**

- Khi bật API, classes bắt đầu bằng `[]`; chưa có dữ liệu không có nghĩa hiển thị lớp mẫu.
- Bỏ coach khỏi nhóm tự gọi danh sách toàn hệ thống `fetchMembers`/`fetchStaff`, tránh request 403. Coach demo lấy roster theo lớp và lịch theo mình.
- Manager nhận lớp thật (kể cả rỗng); khi lỗi set lớp `[]` và lưu thông báo lỗi. Không fallback sang `INITIAL_CLASSES` khiến người xem nhầm mock là DB thật.

**Giới hạn:** thay đổi chỉ xử lý các đoạn trên, không có nghĩa mọi state/fallback mock trong context đã được loại bỏ. Các state subject/room ban đầu và những nhánh khác cần team FE đánh giá riêng nếu có yêu cầu tách toàn bộ mock.

## 1.4. FE — màn hình demo mới và route bị thay

### F08 — thêm `src/api/demoApi.js`

File mới: [demoApi.js](FE_Project-main/src/api/demoApi.js). Toàn bộ code hiện tại:

```javascript
import { httpClient } from './httpClient.js';

// Current backend contracts. Prices and state transitions are decided by the backend.
const query = (values = {}) => {
  const params = new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v !== null && v !== undefined));
  return params.size ? `?${params}` : '';
};
const post = (path, data) => httpClient.post(path, data, { notifyForbidden: false });
export const demoApi = {
  me: () => httpClient.get('/api/members/me'),
  packages: () => httpClient.get('/api/packages'),
  members: () => httpClient.get('/api/members'),
  classes: () => httpClient.get('/api/classes'),
  staff: () => httpClient.get('/api/staff'),
  saveClass: (data, id) => id ? httpClient.put(`/api/classes/${id}`, data, { notifyForbidden: false }) : post('/api/classes', data),
  subjects: () => httpClient.get('/api/subjects'),
  subscriptions: (memberId) => httpClient.get(`/api/members/${memberId}/subscriptions`),
  subscribe: (packageId, memberId) => post(memberId ? `/api/members/${memberId}/subscriptions` : '/api/members/me/subscriptions', { packageId: Number(packageId), paymentMethod: 'Cash' }),
  invoices: (own, filters) => httpClient.get(own ? '/api/members/me/invoices' : `/api/invoices${query(filters)}`),
  confirmCash: (id) => post(`/api/invoices/${id}/confirm-cash`),
  cancelInvoice: (id) => post(`/api/invoices/${id}/cancel`),
  enrollments: (values) => httpClient.get(`/api/enrollments${query(values)}`),
  enroll: (classId, memberId) => post(`/api/classes/${classId}/enroll`, { ...(memberId ? { memberId: Number(memberId) } : {}), paymentMethod: 'Cash' }),
  cancelEnrollment: (id) => httpClient.patch(`/api/enrollments/${id}/cancel`, {}, { notifyForbidden: false }),
  sessions: (values) => httpClient.get(`/api/sessions${query(values)}`),
  rooms: () => httpClient.get('/api/rooms'),
  createSession: (data) => post('/api/sessions', data),
  roster: (classId) => httpClient.get(`/api/classes/${classId}/enrollments`),
  attendance: (values) => httpClient.get(`/api/attendances${query(values)}`),
  record: (data) => post('/api/training-results', data),
  results: (values) => httpClient.get(`/api/training-results${query(values)}`),
  feedbackMine: (classId) => httpClient.get(`/api/classes/${classId}/feedback/me`),
  feedback: (classId, data) => post(`/api/classes/${classId}/feedback`, { ...data, classId }),
  classFeedback: (classId) => httpClient.get(`/api/classes/${classId}/feedback`),
  coachFeedback: (classId) => httpClient.get(`/api/coaches/me/feedback${query({ classId })}`),
};
```

**Cách hoạt động:** dùng `httpClient` cũ; query bỏ tham số rỗng; các POST mặc định `notifyForbidden: false` để component tự hiện lỗi. `subscribe` và `enroll` chỉ gửi `Cash` trong demo. Không gửi amount; memberId chỉ thêm khi thao tác hộ.

**Ảnh hưởng kiến trúc:** đây là adapter bổ sung song song với auth/invoice/membership/manager API hiện có. Không bắt buộc giữ tên `demoApi` hoặc giữ hai bộ adapter trong FE chính. Có thể chuyển từng hàm sang service theo domain của FE rồi xóa file sau khi sửa import/caller.

### F09 — thêm `src/components/views/LiveDemoViews.jsx`

File mới: [LiveDemoViews.jsx](FE_Project-main/src/components/views/LiveDemoViews.jsx). File này có UI, form, bảng, modal, state/loading/error; **không phải chỉ nối API dưới giao diện cũ**.

| Vị trí / component | Hàm/handler, dữ liệu và hành vi | Cần lấy nguyên component cho FE chính? |
|---|---|---|
| Dòng 16 `useAction()` | `run(task)` dùng ref khóa thao tác lặp, busy/message/error, lấy thông báo từ `getErrorMessage`; mở khóa trong finally | Không; có thể dùng cơ chế mutation/loading hiện có. Cần hành vi chống bấm lặp. |
| Dòng 31 `Notice()` | Hiện loading, lỗi bằng `role=alert`, thành công bằng `role=status` | Không; dùng toast/alert hiện có. |
| Dòng 35 `InvoiceTable()` | Hiện mã, member/email, gói hoặc lớp, amount, trạng thái/hạn thu; chỉ Pending hiện nút; confirm chỉ Cash và cashier | Không; quy tắc hiện nút và field là tham khảo để tích hợp bảng cũ. |
| Dòng 45 `LiveMembershipView()` | `refresh()` lấy member `/me`, sau đó song song packages/subscriptions/own invoices; `buy()` tạo order, báo mã invoice, đọc lại; handler hủy gọi cancel rồi đọc lại | Không; đưa logic vào MyMembershipView hiện tại nếu FE chọn. |
| Dòng 76 `LiveCashPaymentsView({readOnly})` | `refresh()` lọc invoices theo keyword/status; effect lấy member/packages; form tạo đơn hộ; modal confirm Cash; handler cancel; readOnly cho manager | Không; đưa vào màn hình thu tiền hiện có. Hộp xác nhận là UX thêm cho demo. |
| Dòng 109 `MemberFeedback()` | Effect GET feedback/me; 404 coi là chưa gửi, các lỗi khác hiện lỗi; form gửi classId/coachId, hai điểm 1–5, comment ≤1000; lưu response | Không; có thể viết form riêng. Cần xử lý chưa có feedback và chống gửi trùng. |
| Dòng 126 `LiveClassCatalog({receptionist})` | `refresh()` lấy classes/subjects/enrollments; staff chọn member; handler enroll/cancel đọc lại; hiển thị tuitionFee, Pending/Registered; gắn form feedback với lớp hợp lệ | Không; giữ catalog/booking hiện tại và nối cùng contract. |
| Dòng 158 `RosterMember()` | Form Present/Absent/Late, content, reason; gửi sessionId/memberId/coachId; `afterSave()` tải lại roster/results | Không; áp dụng payload và lý do sửa vào attendance UI hiện có. |
| Dòng 169 `LiveAttendanceView({member})` | `refresh()` lấy lịch/member attendance hoặc lịch coach + feedback; `refreshRoster()` lấy enrollments/results; chỉ Registered/Completed hiện form; member chỉ xem | Không; file đã gom nhiều nghiệp vụ vào một màn hình demo. |
| Dòng 212 `LiveManagerFeedback()` | Effect lấy classes, effect theo classId lấy summary; hiện số đánh giá, hai trung bình, nhận xét | Không; API summary có thể gắn vào màn manager hiện tại. |
| Dòng 222 `LiveManagerSessions()` | `refresh()` lấy class/room/session; handler tạo buổi status Scheduled, số ID kiểu Number; phòng Available; ngày mặc định theo Việt Nam | Không; dùng form lịch hiện tại. |
| Dòng 248 `LiveManagerClasses()` | `empty()` tạo form; `refresh()` lấy class/subject/staff; `field()` bind input; create/update với tuitionFee/capacity kiểu Number, coach nullable; reset form/đọc lại | Không; dùng form lớp hiện tại, bổ sung field contract nếu cần. |

Code tiêu biểu dưới đây là **trích nguyên source**, để thấy cách mua gói và tải lại dữ liệu. Những hàm còn lại tìm theo bảng trên, không cần áp dụng nguyên file:

Tr?ch `FE_Project-main/src/components/views/LiveDemoViews.jsx`, d?ng 45?66:

```jsx
export function LiveMembershipView() {
  const [data, setData] = useState({ packages: [], subscriptions: [], invoices: [], member: null });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const action = useAction();
  const refresh = useCallback(async () => {
    setLoading(true); setLoadError('');
    try {
      const member = await api.me();
      const memberId = member.id ?? member.memberId;
      if (!Number.isInteger(memberId) || memberId < 1) throw new Error('Backend chưa trả ID hội viên hợp lệ.');
      const [packages, subscriptions, invoices] = await Promise.all([api.packages(), api.subscriptions(memberId), api.invoices(true)]);
      setData({ member, packages, subscriptions, invoices });
    } catch (e) { setLoadError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  const buy = (pkg) => action.run(async (notify) => {
    const order = await api.subscribe(pkg.id);
    notify(`Yêu cầu ${order.invoiceCode} đã tạo: ${money(order.amount)}. Mang mã đến lễ tân đóng tiền mặt. Gói sẽ được kích hoạt sau khi thu tiền.`);
    await refresh();
  });
```

Ví dụ ghi điểm danh/nhận xét và lý do sửa:

Tr?ch `FE_Project-main/src/components/views/LiveDemoViews.jsx`, d?ng 158?167:

```jsx
function RosterMember({ enrollment, existing, session, afterSave }) {
  const [state, setState] = useState(existing?.attendanceStatus || 'Present');
  const [content, setContent] = useState(existing?.content || '');
  const [reason, setReason] = useState('');
  const action = useAction();
  return <form className="border-b py-3 space-y-2" onSubmit={e => { e.preventDefault(); action.run(async notify => { await api.record({ sessionId: session.id, memberId: enrollment.memberId, coachId: session.coachId, attendanceStatus: state, content, reason }); notify('Đã lưu điểm danh và nhận xét.'); await afterSave(); }); }}>
    <p className="font-semibold">{enrollment.memberName} · {enrollment.memberEmail} · {status(enrollment.status)}</p><Notice error={action.error} message={action.message} />
    <div className="flex flex-wrap gap-2"><select className={input} aria-label={`Điểm danh ${enrollment.memberName}`} value={state} onChange={e => setState(e.target.value)}>{['Present', 'Absent', 'Late'].map(s => <option key={s} value={s}>{status(s)}</option>)}</select><input className={input} aria-label="Nhận xét buổi học" placeholder="Nhận xét buổi học" value={content} onChange={e => setContent(e.target.value)} /><input className={input} aria-label="Lý do sửa điểm danh" placeholder="Lý do sửa điểm danh quá khứ" value={reason} onChange={e => setReason(e.target.value)} /><button className={button} disabled={action.busy}>Lưu điểm danh</button></div>
  </form>;
}
```

**Giới hạn của UI demo:** cần bấm Tải lại/F5 sau thao tác từ role khác; không có realtime/polling tự đồng bộ. Bộ component chưa được coi là thiết kế FE chính đã được team FE duyệt. Tên, bố cục và việc gom các màn hình là quyết định triển khai demo cần được review.

### F10 — `src/App.jsx`: đổi 15 route ở chế độ API

File: [App.jsx](FE_Project-main/src/App.jsx), lazy import dòng 21–28; routes khoảng 348–375 trong `SCMSApp()`.

Code lazy import mới:

Tr?ch `FE_Project-main/src/App.jsx`, d?ng 21?28:

```jsx
const live = () => import('./components/views/LiveDemoViews');
const LiveMembershipView = React.lazy(() => live().then(m => ({ default: m.LiveMembershipView })));
const LiveCashPaymentsView = React.lazy(() => live().then(m => ({ default: m.LiveCashPaymentsView })));
const LiveClassCatalog = React.lazy(() => live().then(m => ({ default: m.LiveClassCatalog })));
const LiveAttendanceView = React.lazy(() => live().then(m => ({ default: m.LiveAttendanceView })));
const LiveManagerFeedback = React.lazy(() => live().then(m => ({ default: m.LiveManagerFeedback })));
const LiveManagerSessions = React.lazy(() => live().then(m => ({ default: m.LiveManagerSessions })));
const LiveManagerClasses = React.lazy(() => live().then(m => ({ default: m.LiveManagerClasses })));
```

Code mẫu của một route đã bị đổi:

Tr?ch `FE_Project-main/src/App.jsx`, d?ng 372?372:

```jsx
                <Route path="/member/my-membership" element={<RequireRole allowedRoles={[ROLE_MEMBER]} currentRole={role} attemptedPath={location.pathname} onForbidden={() => navigate(`/${role}/dashboard`)}>{apiEnabled ? <LiveMembershipView /> : <MyMembershipView />}</RequireRole>} />
```

Danh sách đầy đủ:

| Route | Component cũ | Khi API bật hiện component mới |
|---|---|---|
| `/manager/classes` | `ClassManagement` | `LiveManagerClasses` + `LiveManagerSessions` + `LiveManagerFeedback` |
| `/manager/payments` | `PaymentManagement` | `LiveCashPaymentsView readOnly` |
| `/coach/dashboard` | `CoachDashboard` | `LiveAttendanceView` |
| `/coach/schedule` | `CoachScheduleView` | `LiveAttendanceView` |
| `/coach/attendance` | `CoachAttendanceView` | `LiveAttendanceView` |
| `/coach/assigned-members` | `CoachAssignedMembersView` | `LiveAttendanceView` |
| `/coach/progress` | `CoachTrainingProgress` | `LiveAttendanceView` |
| `/receptionist/dashboard` | `ReceptionistDashboard` | `LiveCashPaymentsView` |
| `/receptionist/booking` | `ReceptionClassBooking` | `LiveClassCatalog receptionist` |
| `/receptionist/payments` | `ReceptionPaymentsView` | `LiveCashPaymentsView` |
| `/member/dashboard` | `MemberDashboard` | `LiveMembershipView` |
| `/member/my-membership` | `MyMembershipView` | `LiveMembershipView` |
| `/member/catalog` | `MemberClassCatalog` | `LiveClassCatalog` |
| `/member/attendance` | `CoachAttendanceView` | `LiveAttendanceView member` |
| `/member/progress` | `CoachTrainingProgress` | `LiveAttendanceView member` |

`apiEnabled=false` vẫn chọn component cũ. Tuy nhiên **bật API không chỉ thay nguồn dữ liệu: nó thay cả nội dung màn hình**, gồm dashboard/progress/assigned members. Vì vậy việc giữ nguyên sidebar/header/style không có nghĩa FE không bị đổi.

**Khuyến nghị cho FE chính:** team FE tự quyết định giữ route/component cũ và chuyển logic API cần thiết vào đó. Không cần áp dụng route replacement để BE chạy. Nếu bỏ LiveDemoViews, phải bỏ lazy imports và thay lại toàn bộ caller tương ứng; chỉ xóa file sẽ làm app lỗi import. Chưa thực hiện thao tác bỏ/hoàn tác này.

### F11–F12 — hai file test FE đã sửa

| File/vị trí | Thay đổi code/fixture | Vì sao và cách FE đánh giá |
|---|---|---|
| [test/fe2_membership_payment_test.mjs](FE_Project-main/test/fe2_membership_payment_test.mjs), khoảng dòng 326, 347–388, 433, 975 | Response mua gói: Active → Pending và thêm invoiceId/code/amount/expiresAt. Test pay chuyển `/pay` → `/confirm-cash`, body undefined. Bỏ hai ca tạo invoice độc lập cũ; thay bằng assert reject createInvoice và tự chuyển Paid. Thêm mock POST cancel lỗi 409. Fixture cuối có subscriptionId/invoiceId. | Theo contract mới; là test adapter/logic với mock HTTP, không tự chứng minh UI hay DB thật. Có thể lấy các assertion theo contract rồi viết lại theo kiến trúc FE chính. |
| [test/g4_logic_test.mjs](FE_Project-main/test/g4_logic_test.mjs), dòng 1011/1014 | Hai expected code `MB-0010`/`MB-0011` → `''` khi DTO member không cấp code. | Mapper member đã trả rỗng trước đợt này; sửa test, không sửa mapper để tạo mã mới. Không suy ra mọi DTO đều có cùng memberCode: EnrollmentResponse hiện có memberCode riêng. |

Code G4 thay đổi chính xác:

```diff
--- ZIP/test/g4_logic_test.mjs
+++ CURRENT/test/g4_logic_test.mjs
@@ -1009,8 +1009,8 @@
   assert.equal(mappedMembers[0].id, 10);
   assert.equal(mappedMembers[0].name, 'Le Van Direct');
-  assert.equal(mappedMembers[0].code, 'MB-0010');
+  assert.equal(mappedMembers[0].code, '', 'Do not fabricate a member code absent from backend data');
   assert.equal(mappedMembers[1].id, 11);
   assert.equal(mappedMembers[1].name, 'Pham Thi Direct');
-  assert.equal(mappedMembers[1].code, 'MB-0011');
+  assert.equal(mappedMembers[1].code, '', 'Do not fabricate a member code absent from backend data');
   mappedMembers.forEach(m => {
     assert.ok(typeof m.id === 'number' && m.id > 0, `Member id must be positive number, got ${m.id}`);
```

Ví dụ assertion thanh toán mới, trích source:

Tr?ch `FE_Project-main/test/fe2_membership_payment_test.mjs`, d?ng 361?385:

```javascript
assert.equal(subResult.invoiceCode, 'SCMS-DEMO-42');
console.log('PASS: subscribePackage sends correct payload to /api/members/101/packages and maps response');

// 6.2 Pay invoice via Receptionist cash confirmation
const payResult = await invoiceApi.payInvoice(42, {
  paymentMethod: 'CASH',
  receptionistId: 5,
  notes: 'Thu tiền mặt tại quầy',
});

assert.equal(recordedPostPath, '/api/invoices/42/confirm-cash');
assert.equal(recordedPostBody, undefined, 'Amount and receptionist identity are decided by backend');
assert.equal(payResult.invoiceId, 42);
assert.equal(payResult.paymentStatus, 'successful');
console.log('PASS: payInvoice calls /api/invoices/42/confirm-cash and transitions to successful');

// 6.3 Removed standalone invoice creation cannot be used to fabricate invoices.
await assert.rejects(() => invoiceApi.createInvoice({ memberId: 101, amount: 1 }), /đăng ký/);
await assert.rejects(() => invoiceApi.updateInvoiceStatus(42, 'Paid'), /Chỉ hủy/);
console.log('PASS: Removed invoice create/status routes cannot fabricate a paid invoice');

// 6.5 In-progress transaction cache retain and discard
const { inProgressTransactionCache } = await import('../src/api/paymentSession.js');
inProgressTransactionCache.set('101', {
  subscriptionId: 99,
```

### F13 — phần kết nối FE đã có sẵn, không sửa trong đợt này

- [httpClient.js](FE_Project-main/src/api/httpClient.js): `httpRequest()` dùng `fetch`, `credentials: 'include'`, JSON, xử lý 204/401/403; `VITE_API_BASE_URL` đã có. Không thêm cơ chế bearer token mới.
- [vite.config.js](FE_Project-main/vite.config.js): proxy `/api` sang `VITE_API_PROXY_TARGET` (mặc định localhost:8080), các xử lý origin/referer/cookie đã có. Không tính chúng là code mới viết để demo.
- `src/api/apiErrors.js`, `schemaValidators.js`, `memberMappers.js`, `paymentSession.js`, các file API khác ngoài danh sách F01/F03–F06/F08 không đổi so với ZIP.
- `package.json`, lockfile, `.env.example`, `.env.mock`, CSS/assets/header/sidebar không đổi so với ZIP. `node_modules`/`dist` là dependencies/build output, không phải thay đổi thiết kế FE.
- Lệnh đặt `VITE_API_ENABLED=true`, `VITE_API_PROXY_TARGET=http://localhost:8080` chỉ là cấu hình terminal chạy demo. Không sửa file env FE để ép bật API vĩnh viễn.

Luồng kết nối đang dùng: **component → adapter API → httpClient có cookie → Vite proxy `/api` → BE 8080 → Supabase**. `demoApi` trả DTO nguyên bản; adapter cũ có mapper. FE chính cần chọn quy ước nhất quán.

## 1.5. BE — những sửa bổ sung sau audit

Trong phần này, đường dẫn source BE bắt đầu tại `src/main/java/com/sportscenter/`. Diff là thay đổi thực tế so với Git HEAD, không bao gồm toàn bộ module payment/feedback đã tồn tại.

### B01 — thông tin member thật trong auth response và API `/members/me`

Files: [auth/AuthController.java](src/main/java/com/sportscenter/auth/AuthController.java), [auth/LoginResponse.java](src/main/java/com/sportscenter/auth/LoginResponse.java), [user/MemberController.java](src/main/java/com/sportscenter/user/MemberController.java).

Vị trí: `AuthController.toResponse()` khoảng 150; `LoginResponse` record dòng 5; `MemberController.getCurrentMember()` khoảng 22.

```diff
--- HEAD/src/main/java/com/sportscenter/auth/AuthController.java
+++ CURRENT/src/main/java/com/sportscenter/auth/AuthController.java
@@ -38,4 +38,5 @@
     private final SecurityContextRepository securityContextRepository;
     private final UserRepository userRepository;
+    private final com.sportscenter.user.MemberRepository memberRepository;
     private final LoginAttemptService loginAttemptService;
     private final UserService userService;
@@ -157,5 +158,6 @@
                         .map(authority -> authority.getAuthority())
                         .toList()),
-                user.isFirstLogin());
+                user.isFirstLogin(),
+                memberRepository.existsById(user.getId()) ? user.getId() : null);
     }
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/auth/LoginResponse.java
+++ CURRENT/src/main/java/com/sportscenter/auth/LoginResponse.java
@@ -9,4 +9,5 @@
         String role,
         Set<String> authorities,
-        boolean isFirstLogin) {
+        boolean isFirstLogin,
+        Integer memberId) {
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/user/MemberController.java
+++ CURRENT/src/main/java/com/sportscenter/user/MemberController.java
@@ -17,4 +17,12 @@
 public class MemberController {
     private final MemberService memberService;
+    private final UserRepository userRepository;
+
+    @GetMapping("/me")
+    @PreAuthorize("hasRole('MEMBER')")
+    public MemberResponse getCurrentMember(org.springframework.security.core.Authentication authentication) {
+        User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
+        return memberService.findById(user.getId());
+    }
 
     @GetMapping
```

**Giải thích:** login và `/api/auth/me` bổ sung `memberId` khi thực sự có row member ứng với user, các tài khoản khác có thể null. API mới `GET /api/members/me` chỉ cho MEMBER, lấy user từ email trong phiên để trả profile. FE không cần quyền quản lý member để tìm ID của chính mình.

**Áp dụng với FE chính:** có thể dùng memberId trong auth DTO hoặc `/members/me`; giữ null cho staff, không lấy số từ chuỗi `MB-...`. Đây là API dùng được ngoài demo, không cần LiveDemoViews.

### B02 — OTP hardcode chỉ khi có cấu hình demo

File: [auth/RegistrationService.java](src/main/java/com/sportscenter/auth/RegistrationService.java), field demoOtp khoảng 38, `issueOtp()` dòng 126.

```diff
--- HEAD/src/main/java/com/sportscenter/auth/RegistrationService.java
+++ CURRENT/src/main/java/com/sportscenter/auth/RegistrationService.java
@@ -36,4 +36,6 @@
     private final AuditService auditService;
     private final PasswordResetService passwordResetService;
+    @org.springframework.beans.factory.annotation.Value("${registration.demo-otp:}")
+    private String demoOtp = "";
 
     @Transactional
@@ -129,5 +131,6 @@
             otpRepository.save(previous);
         });
-        String rawOtp = String.format("%06d", RANDOM.nextInt(1_000_000));
+        String rawOtp = demoOtp != null && !demoOtp.isBlank()
+                ? demoOtp : String.format("%06d", RANDOM.nextInt(1_000_000));
         EmailOtp otp = new EmailOtp();
         otp.setEmail(email);
@@ -139,5 +142,5 @@
         otp.setUsed(false);
         otpRepository.save(otp);
-        emailService.sendOtpEmail(email, rawOtp, PURPOSE);
+        if (demoOtp == null || demoOtp.isBlank()) emailService.sendOtpEmail(email, rawOtp, PURPOSE);
     }
 
```

**Giải thích:** khi `registration.demo-otp` khác rỗng, dùng mã cố định và bỏ gửi email; mặc định không cấu hình vẫn dùng SecureRandom và EmailService. Đường xác thực, hash OTP, hạn dùng, số lần thử/quota/used giữ nguyên. Đây là thay đổi BE giúp tạm hoãn SMTP, không có nghĩa so sánh OTP tại FE.

**Đối với FE:** vẫn POST OTP đến BE. Profile demo và thông báo OTP ở UI là hai việc khác nhau; FE không tự biết BE đang chạy demo chỉ vì `VITE_API_ENABLED=true`.

### B03 — query tìm hóa đơn PostgreSQL và khóa xử lý đồng thời

File: [invoice/InvoiceRepository.java](src/main/java/com/sportscenter/invoice/InvoiceRepository.java), interface dòng 15, lock queries 21–29, các finder subscription 35–36, `searchInvoices()` 53.

```diff
--- HEAD/src/main/java/com/sportscenter/invoice/InvoiceRepository.java
+++ CURRENT/src/main/java/com/sportscenter/invoice/InvoiceRepository.java
@@ -13,5 +13,5 @@
 
 @Repository
-public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
+public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Invoice> {
 
     @Query(value = "SELECT nextval('invoice_code_seq')", nativeQuery = true)
@@ -19,11 +19,11 @@
 
     @Lock(LockModeType.PESSIMISTIC_WRITE)
-    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.member LEFT JOIN FETCH i.membershipPackage " +
-           "LEFT JOIN FETCH i.sportsClass LEFT JOIN FETCH i.subscription LEFT JOIN FETCH i.receptionist WHERE i.invoiceId = :id")
+    // Lock the root row in the SELECT itself. Fetch joins cause Hibernate follow-on locking,
+    // which can leave a concurrent caller holding a stale Pending snapshot after waiting.
+    @Query("SELECT i FROM Invoice i WHERE i.invoiceId = :id")
     java.util.Optional<Invoice> findByIdForUpdate(@Param("id") Integer id);
 
     @Lock(LockModeType.PESSIMISTIC_WRITE)
-    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.member LEFT JOIN FETCH i.membershipPackage " +
-           "LEFT JOIN FETCH i.sportsClass LEFT JOIN FETCH i.subscription WHERE i.invoiceCode = :code")
+    @Query("SELECT i FROM Invoice i WHERE i.invoiceCode = :code")
     java.util.Optional<Invoice> findByInvoiceCodeForUpdate(@Param("code") String code);
 
@@ -33,4 +33,6 @@
 
     boolean existsByMemberIdAndMembershipPackageIdAndPaymentStatus(Integer memberId, Integer packageId, String status);
+    boolean existsBySubscriptionSubscriptionIdAndPaymentStatus(Integer subscriptionId, String status);
+    List<Invoice> findBySubscriptionSubscriptionId(Integer subscriptionId);
 
     @Query("SELECT i FROM Invoice i " +
@@ -49,21 +51,5 @@
     List<Invoice> findByMemberId(@Param("memberId") Integer memberId);
 
-    @Query("SELECT i FROM Invoice i " +
-           "LEFT JOIN FETCH i.member m " +
-           "LEFT JOIN FETCH i.membershipPackage p " +
-           "LEFT JOIN FETCH i.receptionist r " +
-           "WHERE (:memberId IS NULL OR i.member.id = :memberId) " +
-           "AND (:packageId IS NULL OR (i.membershipPackage IS NOT NULL AND i.membershipPackage.id = :packageId)) " +
-           "AND (:receptionistId IS NULL OR (i.receptionist IS NOT NULL AND i.receptionist.id = :receptionistId)) " +
-           "AND (:status IS NULL OR LOWER(i.paymentStatus) = LOWER(:status)) " +
-           "AND (:method IS NULL OR LOWER(i.paymentMethod) = LOWER(:method)) " +
-           "AND (:keyword IS NULL OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
-           "OR LOWER(i.member.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
-           "OR (i.membershipPackage IS NOT NULL AND LOWER(i.membershipPackage.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
-           "OR (i.sportsClass IS NOT NULL AND LOWER(i.sportsClass.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
-           "AND (:startDate IS NULL OR i.paymentDate >= :startDate) " +
-           "AND (:endDate IS NULL OR i.paymentDate <= :endDate) " +
-           "ORDER BY i.invoiceId DESC")
-    List<Invoice> searchInvoices(
+    default List<Invoice> searchInvoices(
             @Param("memberId") Integer memberId,
             @Param("packageId") Integer packageId,
@@ -74,5 +60,30 @@
             @Param("startDate") LocalDateTime startDate,
             @Param("endDate") LocalDateTime endDate
-    );
+    ) {
+        return findAll((root, query, cb) -> {
+            root.fetch("member", jakarta.persistence.criteria.JoinType.LEFT);
+            root.fetch("membershipPackage", jakarta.persistence.criteria.JoinType.LEFT);
+            root.fetch("sportsClass", jakarta.persistence.criteria.JoinType.LEFT);
+            root.fetch("receptionist", jakarta.persistence.criteria.JoinType.LEFT);
+            var member = root.join("member", jakarta.persistence.criteria.JoinType.LEFT);
+            var pkg = root.join("membershipPackage", jakarta.persistence.criteria.JoinType.LEFT);
+            var cls = root.join("sportsClass", jakarta.persistence.criteria.JoinType.LEFT);
+            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
+            if (memberId != null) conditions.add(cb.equal(member.get("id"), memberId));
+            if (packageId != null) conditions.add(cb.equal(pkg.get("id"), packageId));
+            if (receptionistId != null) conditions.add(cb.equal(root.get("receptionist").get("id"), receptionistId));
+            if (status != null) conditions.add(cb.equal(cb.lower(root.get("paymentStatus")), status.toLowerCase(java.util.Locale.ROOT)));
+            if (method != null) conditions.add(cb.equal(cb.lower(root.get("paymentMethod")), method.toLowerCase(java.util.Locale.ROOT)));
+            if (keyword != null) {
+                String pattern = "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%";
+                conditions.add(cb.or(cb.like(cb.lower(root.get("invoiceCode")), pattern),
+                        cb.like(cb.lower(member.get("fullName")), pattern),
+                        cb.like(cb.lower(pkg.get("name")), pattern), cb.like(cb.lower(cls.get("name")), pattern)));
+            }
+            if (startDate != null) conditions.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), startDate));
+            if (endDate != null) conditions.add(cb.lessThanOrEqualTo(root.get("paymentDate"), endDate));
+            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
+        }, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "invoiceId"));
+    }
 
     @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i " +
```

**Các vấn đề đã sửa:**

1. Query cũ chứa điều kiện optional `:param IS NULL`/LOWER trên tham số null, gây lỗi suy kiểu trên PostgreSQL khi không nhập filter. Specification chỉ thêm predicate cho tham số có giá trị.
2. Dùng LEFT joins cho package/class/member, tránh làm mất invoice gói khi tìm kiếm vì invoice gói không có class, và ngược lại. Có từ khóa mã invoice/tên member/tên gói/tên lớp; sort ID giảm dần. Bộ lọc ngày vẫn theo **paymentDate**, không phải createdAt.
3. Thêm finder theo subscription để kiểm tra Paid và hủy các invoice Pending liên kết.
4. Lock query bỏ fetch joins, chỉ `SELECT i FROM Invoice i WHERE ...` với `PESSIMISTIC_WRITE`. Fetch joins trước đó khiến Hibernate khóa sau khi đã đọc snapshot; hai request Cash có thể cùng thấy Pending và cùng trả 200. Sau sửa, kiểm tra thật cho **một 200, một 409**.

**FE cần hiểu:** confirm lặp có thể trả 409; hiển thị lỗi và GET lại trạng thái, không tự ghi Paid từ request thất bại. Đây là sửa tính đúng đắn BE, không phụ thuộc UI demo.

### B04 — invoice filter và đồng bộ subscription

File: [invoice/InvoiceService.java](src/main/java/com/sportscenter/invoice/InvoiceService.java), `findAll()` dòng 44, `requirePaidSubscription()` 291, `cancelPendingSubscriptionPayment()` 298.

```diff
--- HEAD/src/main/java/com/sportscenter/invoice/InvoiceService.java
+++ CURRENT/src/main/java/com/sportscenter/invoice/InvoiceService.java
@@ -46,7 +46,6 @@
             Authentication authentication) {
         Integer scopedMemberId = resolveMemberScope(authentication, memberId);
-        List<Invoice> list = scopedMemberId != null
-                ? invoiceRepository.findByMemberId(scopedMemberId)
-                : invoiceRepository.searchInvoices(null, packageId, receptionistId, blankToNull(status), blankToNull(method), blankToNull(keyword), startDate, endDate);
+        List<Invoice> list = invoiceRepository.searchInvoices(scopedMemberId, packageId, receptionistId,
+                blankToNull(status), blankToNull(method), blankToNull(keyword), startDate, endDate);
         return list.stream().map(InvoiceResponse::from).toList();
     }
@@ -289,4 +288,21 @@
         return invoiceRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
     }
+    @Transactional(readOnly = true)
+    public void requirePaidSubscription(Integer subscriptionId) {
+        if (!invoiceRepository.existsBySubscriptionSubscriptionIdAndPaymentStatus(subscriptionId, "Paid")) {
+            throw new BusinessException("A subscription can only be activated after its invoice is Paid");
+        }
+    }
+
+    @Transactional
+    public void cancelPendingSubscriptionPayment(Integer subscriptionId) {
+        for (Invoice candidate : invoiceRepository.findBySubscriptionSubscriptionId(subscriptionId)) {
+            Invoice invoice = lockInvoice(candidate.getInvoiceId());
+            if ("Pending".equals(invoice.getPaymentStatus())) {
+                invoice.setPaymentStatus("Cancelled");
+                cancelLinked(invoice);
+            }
+        }
+    }
     private boolean expired(Invoice invoice) { return invoice.getExpiresAt() != null && !invoice.getExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE)); }
     private User currentUser(Authentication authentication) {
```

**Giải thích:** trước đây có memberId là đi thẳng `findByMemberId`, bỏ qua các filter khác. Nay luôn gọi search cùng scope member và đủ filters. Hai hàm mới buộc có invoice Paid trước khi kích hoạt và khóa/hủy invoice Pending khi subscription bị hủy/hết hạn.

File liên quan: [membership/MemberPackageService.java](src/main/java/com/sportscenter/membership/MemberPackageService.java), `updateStatus()` dòng 100, `normalizeStatus()` 119.

```diff
--- HEAD/src/main/java/com/sportscenter/membership/MemberPackageService.java
+++ CURRENT/src/main/java/com/sportscenter/membership/MemberPackageService.java
@@ -104,7 +104,9 @@
         String normalizedStatus = normalizeStatus(status);
         String previousStatus = mp.getStatus();
-        if ("Pending".equalsIgnoreCase(previousStatus) && "Active".equals(normalizedStatus)) {
-            throw new BusinessException("A Pending subscription can only be activated after its invoice is Paid");
+        if (!"Active".equalsIgnoreCase(previousStatus) && "Active".equals(normalizedStatus)) {
+            invoiceService.requirePaidSubscription(subscriptionId);
         }
+        if ("Cancelled".equals(normalizedStatus) || "Expired".equals(normalizedStatus))
+            invoiceService.cancelPendingSubscriptionPayment(subscriptionId);
         mp.setStatus(normalizedStatus);
         MemberPackage saved = memberPackageRepository.save(mp);
@@ -118,5 +120,6 @@
         if ("Expired".equalsIgnoreCase(status)) return "Expired";
         if ("Cancelled".equalsIgnoreCase(status)) return "Cancelled";
-        return "Active";
+        if ("Active".equalsIgnoreCase(status)) return "Active";
+        throw new BusinessException("Subscription status must be Active, Expired or Cancelled");
     }
 }
```

**Giải thích:** không chỉ Pending → Active mà mọi trạng thái khác → Active đều phải có Paid; chặn đường vòng Cancelled → Active khi chưa trả tiền. Cancelled/Expired đồng bộ invoice Pending. Giá trị status không hợp lệ nay báo lỗi, không mặc định Active.

**FE cần áp dụng:** bỏ thao tác tự kích hoạt gói chưa Paid; xử lý lỗi BE trả khi đổi status. Không cần sửa UI để BE thực thi guard.

### B05 — hiệu lực gói khi ghi danh

File: [enrollment/EnrollmentService.java](src/main/java/com/sportscenter/enrollment/EnrollmentService.java), `enroll()` overload đầy đủ dòng 69, kiểm tra package khoảng 80.

```diff
--- HEAD/src/main/java/com/sportscenter/enrollment/EnrollmentService.java
+++ CURRENT/src/main/java/com/sportscenter/enrollment/EnrollmentService.java
@@ -80,6 +80,7 @@
         List<MemberPackage> activePackages = memberPackageRepository.findByMemberId(memberId).stream()
                 .filter(p -> "Active".equalsIgnoreCase(p.getStatus())
-                        && p.getEndDate() != null
-                        && !p.getEndDate().isBefore(LocalDate.now()))
+                        && p.getStartDate() != null && p.getEndDate() != null
+                        && !p.getStartDate().isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))
+                        && !p.getEndDate().isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
                 .toList();
 
```

**Giải thích:** Active chưa đủ; start/end phải có và ngày hiện tại Việt Nam nằm trong khoảng đó. Gói đã Paid nhưng chưa đến startDate không được ghi danh hôm nay. Trước chỉ kiểm tra endDate.

**FE:** hiển thị ngày hiệu lực và lỗi BE; không suy ra mua/gia hạn thành công là dùng ngay.

### B06 — lịch học với filter ngày null trên PostgreSQL

File: [session/SessionRepository.java](src/main/java/com/sportscenter/session/SessionRepository.java), `findFiltered()` khoảng 25, `findByEnrolledMember()` khoảng 80.

```diff
--- HEAD/src/main/java/com/sportscenter/session/SessionRepository.java
+++ CURRENT/src/main/java/com/sportscenter/session/SessionRepository.java
@@ -19,6 +19,6 @@
            "  AND (:coachId IS NULL OR c.coach.id = :coachId) " +
            "  AND (:roomId IS NULL OR r.id = :roomId) " +
-           "  AND (:startDate IS NULL OR s.sessionDate >= :startDate) " +
-           "  AND (:endDate IS NULL OR s.sessionDate <= :endDate) " +
+           "  AND (CAST(:startDate AS date) IS NULL OR s.sessionDate >= :startDate) " +
+           "  AND (CAST(:endDate AS date) IS NULL OR s.sessionDate <= :endDate) " +
            "  AND (:status IS NULL OR s.status = :status) " +
            "ORDER BY s.sessionDate ASC, s.startTime ASC")
@@ -74,6 +74,6 @@
            "JOIN FETCH s.room r " +
            "WHERE c.id IN (SELECT e.sportsClass.id FROM ClassEnrollment e WHERE e.member.id = :memberId AND e.status = 'Registered') " +
-           "  AND (:startDate IS NULL OR s.sessionDate >= :startDate) " +
-           "  AND (:endDate IS NULL OR s.sessionDate <= :endDate) " +
+           "  AND (CAST(:startDate AS date) IS NULL OR s.sessionDate >= :startDate) " +
+           "  AND (CAST(:endDate AS date) IS NULL OR s.sessionDate <= :endDate) " +
            "  AND (:status IS NULL OR s.status = :status) " +
            "ORDER BY s.sessionDate ASC, s.startTime ASC")
```

**Giải thích:** CAST start/endDate về date trong nhánh IS NULL để PostgreSQL xác định kiểu tham số khi không truyền ngày. Sửa cả lịch staff và lịch member, không đổi payload FE. Query lịch enrolled member hiện lấy enrollment `Registered`; không ghi rằng Completed đã được hỗ trợ trong query này.

### B07 — điểm danh/training result: enrollment, coach, lý do sửa và bulk

Files: [training/TrainingResultController.java](src/main/java/com/sportscenter/training/TrainingResultController.java), [TrainingResultRequest.java](src/main/java/com/sportscenter/training/TrainingResultRequest.java), [TrainingResultService.java](src/main/java/com/sportscenter/training/TrainingResultService.java).

Vị trí: controller POST đơn khoảng 31, bulk 37; request record dòng 6; service `recordResult(request, authentication)` 56, `recordBulkResults(..., authentication)` 122, `normalizeAttendanceStatus()` 168.

```diff
--- HEAD/src/main/java/com/sportscenter/training/TrainingResultController.java
+++ CURRENT/src/main/java/com/sportscenter/training/TrainingResultController.java
@@ -29,12 +29,12 @@
     @PostMapping
     @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
-    public ResponseEntity<TrainingResultResponse> recordResult(@Valid @RequestBody TrainingResultRequest request) {
-        return ResponseEntity.ok(trainingResultService.recordResult(request));
+    public ResponseEntity<TrainingResultResponse> recordResult(@Valid @RequestBody TrainingResultRequest request, Authentication authentication) {
+        return ResponseEntity.ok(trainingResultService.recordResult(request, authentication));
     }
 
     @PostMapping("/bulk")
     @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
-    public ResponseEntity<List<TrainingResultResponse>> recordBulkResults(@Valid @RequestBody BulkTrainingResultRequest request) {
-        return ResponseEntity.ok(trainingResultService.recordBulkResults(request));
+    public ResponseEntity<List<TrainingResultResponse>> recordBulkResults(@Valid @RequestBody BulkTrainingResultRequest request, Authentication authentication) {
+        return ResponseEntity.ok(trainingResultService.recordBulkResults(request, authentication));
     }
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/training/TrainingResultRequest.java
+++ CURRENT/src/main/java/com/sportscenter/training/TrainingResultRequest.java
@@ -15,5 +15,10 @@
         String content,
 
-        String attendanceStatus
+        String attendanceStatus,
+        String reason
 ) {
+    public TrainingResultRequest(Integer sessionId, Integer memberId, Integer coachId,
+                                 String content, String attendanceStatus) {
+        this(sessionId, memberId, coachId, content, attendanceStatus, null);
+    }
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/training/TrainingResultService.java
+++ CURRENT/src/main/java/com/sportscenter/training/TrainingResultService.java
@@ -6,4 +6,5 @@
 import com.sportscenter.session.Session;
 import com.sportscenter.session.SessionRepository;
+import com.sportscenter.enrollment.ClassEnrollmentRepository;
 import com.sportscenter.user.User;
 import com.sportscenter.user.UserRepository;
@@ -15,4 +16,6 @@
 
 import java.time.LocalDateTime;
+import java.time.LocalDate;
+import java.time.ZoneId;
 import java.util.ArrayList;
 import java.util.List;
@@ -26,4 +29,5 @@
     private final UserRepository userRepository;
     private final AuditService auditService;
+    private final ClassEnrollmentRepository enrollmentRepository;
 
     @Transactional(readOnly = true)
@@ -46,6 +50,28 @@
     @Transactional
     public TrainingResultResponse recordResult(TrainingResultRequest request) {
+        return recordResult(request, null);
+    }
+
+    @Transactional
+    public TrainingResultResponse recordResult(TrainingResultRequest request, Authentication authentication) {
         Session session = sessionRepository.findById(request.sessionId())
                 .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));
+
+        if (session.getSportsClass() == null || session.getSportsClass().getCoach() == null
+                || !session.getSportsClass().getCoach().getId().equals(request.coachId())) {
+            throw new BusinessException("Coach must be assigned to this session's class");
+        }
+        if (authentication != null && authentication.getAuthorities().stream()
+                .anyMatch(a -> "ROLE_COACH".equals(a.getAuthority()))) {
+            User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
+                    .orElseThrow(() -> new AccessDeniedException("Coach account not found"));
+            if (!actor.getId().equals(request.coachId())) {
+                throw new AccessDeniedException("Coaches may only record attendance for their own classes");
+            }
+        }
+        if (!enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatusIn(
+                request.memberId(), session.getSportsClass().getId(), List.of("Registered", "Completed"))) {
+            throw new BusinessException("Attendance requires a Registered or Completed enrollment in this class");
+        }
 
         User member = userRepository.findById(request.memberId())
@@ -59,4 +85,13 @@
         TrainingResult result = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
                 .orElseGet(TrainingResult::new);
+
+        Attendance previousAttendance = attendanceRepository.findBySessionIdAndMemberId(session.getId(), member.getId()).orElse(null);
+        String previousStatus = previousAttendance != null ? previousAttendance.getState() : result.getAttendanceStatus();
+        boolean correction = previousStatus != null && !previousStatus.equals(status);
+        if (correction && session.getSessionDate() != null
+                && session.getSessionDate().isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")))
+                && (request.reason() == null || request.reason().isBlank())) {
+            throw new BusinessException("A reason is required to correct attendance for a past session");
+        }
 
         result.setSession(session);
@@ -72,7 +107,7 @@
         syncAttendanceRecord(session, member, coach, status);
 
-        auditService.log(coach.getId(), "RECORD_RESULT", "training_results", saved.getResultId(),
+        auditService.log(coach.getId(), correction ? "ATTENDANCE_CORRECTION" : "RECORD_RESULT", "training_results", saved.getResultId(),
                 "Recorded attendance (" + status + ") and result for member " + member.getFullName() +
-                        " in session #" + session.getId());
+                        " in session #" + session.getId() + (correction ? "; previous=" + previousStatus + "; reason=" + request.reason() : ""));
 
         return TrainingResultResponse.from(saved);
@@ -81,4 +116,9 @@
     @Transactional
     public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request) {
+        return recordBulkResults(request, null);
+    }
+
+    @Transactional
+    public List<TrainingResultResponse> recordBulkResults(BulkTrainingResultRequest request, Authentication authentication) {
         Session session = sessionRepository.findById(request.sessionId())
                 .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.sessionId()));
@@ -89,6 +129,5 @@
         }
 
-        User coach = coachId != null ? userRepository.findById(coachId).orElse(null) : null;
-        if (coach == null) {
+        if (coachId == null) {
             throw new BusinessException("Coach is required for recording results");
         }
@@ -98,31 +137,10 @@
         if (request.items() != null) {
             for (BulkTrainingResultRequest.Item item : request.items()) {
-                if (item.memberId() == null) continue;
-
-                User member = userRepository.findById(item.memberId()).orElse(null);
-                if (member == null) continue;
-
-                String status = normalizeAttendanceStatus(item.attendanceStatus());
-
-                TrainingResult tr = trainingResultRepository.findBySessionIdAndMemberId(session.getId(), member.getId())
-                        .orElseGet(TrainingResult::new);
-
-                tr.setSession(session);
-                tr.setMember(member);
-                tr.setCoach(coach);
-                if (item.content() != null) {
-                    tr.setContent(item.content());
-                }
-                tr.setAttendanceStatus(status);
-                tr.setRecordedAt(LocalDateTime.now());
-
-                TrainingResult saved = trainingResultRepository.save(tr);
-                syncAttendanceRecord(session, member, coach, status);
-
-                responses.add(TrainingResultResponse.from(saved));
+                responses.add(recordResult(new TrainingResultRequest(request.sessionId(), item.memberId(), coachId,
+                        item.content(), item.attendanceStatus(), item.reason()), authentication));
             }
         }
 
-        auditService.log(coach.getId(), "RECORD_RESULT", "sessions", session.getId(),
+        auditService.log(coachId, "RECORD_RESULT", "sessions", session.getId(),
                 "Bulk attendance recorded for " + responses.size() + " members in session #" + session.getId());
 
@@ -151,5 +169,6 @@
         if ("Absent".equalsIgnoreCase(status) || "absent".equalsIgnoreCase(status)) return "Absent";
         if ("Late".equalsIgnoreCase(status) || "late".equalsIgnoreCase(status)) return "Late";
-        return "Present";
+        if (status == null || "Present".equalsIgnoreCase(status)) return "Present";
+        throw new BusinessException("Attendance status must be Present, Absent or Late");
     }
 }
```

**Giải thích theo từng guard:**

- Controller chuyển Authentication vào service; giữ overload cũ cho code/test nội bộ. HTTP mutation đi qua overload có Authentication.
- Request đơn thêm `reason`; giữ constructor 5 tham số Java cho tương thích caller nội bộ. `BulkTrainingResultRequest.Item` đã có reason trước đợt sửa này.
- coachId request phải đúng coach được phân công lớp của session; nếu người gọi là coach thì phải đúng ID của người đang đăng nhập.
- Member phải có enrollment Registered/Completed trong đúng lớp; Pending chưa đủ điều kiện điểm danh.
- Chỉ chấp nhận Present/Absent/Late; null giữ mặc định Present theo hành vi cũ. Chuỗi lạ không còn tự biến thành Present.
- Khi đổi trạng thái của buổi quá khứ phải có reason; kiểm tra cả Attendance cũ dù chưa có TrainingResult.
- Save result vẫn đồng bộ Attendance; audit ghi `ATTENDANCE_CORRECTION`, trạng thái cũ và reason khi là correction.
- Bulk gọi lại cùng hàm validate/save cho từng item; không âm thầm bỏ member thiếu/không tồn tại như trước. Có lỗi sẽ thất bại transaction, FE không nên báo “đã lưu tất cả” khi server trả lỗi.

**FE:** gửi các ID thật, status đúng và reason khi sửa; chỉ dựng roster từ enrollment hợp lệ. Không tự cấp coachId khác để vượt quyền. Lý do sửa không phải field chỉ để trang trí UI demo.

### B08 — API sửa Attendance riêng cũng kiểm tra người gọi

Files: [training/AttendanceController.java](src/main/java/com/sportscenter/training/AttendanceController.java), [AttendanceService.java](src/main/java/com/sportscenter/training/AttendanceService.java), `correctAttendance()` controller khoảng 52; service overload 108.

```diff
--- HEAD/src/main/java/com/sportscenter/training/AttendanceController.java
+++ CURRENT/src/main/java/com/sportscenter/training/AttendanceController.java
@@ -50,6 +50,6 @@
     @PostMapping("/correct")
     @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES', 'MANAGE_USERS')")
-    public ResponseEntity<AttendanceResponse> correctAttendance(@Valid @RequestBody AttendanceCorrectionRequest request) {
-        return ResponseEntity.ok(attendanceService.correctAttendance(request));
+    public ResponseEntity<AttendanceResponse> correctAttendance(@Valid @RequestBody AttendanceCorrectionRequest request, Authentication authentication) {
+        return ResponseEntity.ok(attendanceService.correctAttendance(request, authentication));
     }
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/training/AttendanceService.java
+++ CURRENT/src/main/java/com/sportscenter/training/AttendanceService.java
@@ -102,6 +102,24 @@
     @Transactional
     public AttendanceResponse correctAttendance(AttendanceCorrectionRequest request) {
+        return correctAttendance(request, null);
+    }
+
+    @Transactional
+    public AttendanceResponse correctAttendance(AttendanceCorrectionRequest request, Authentication authentication) {
         Attendance att = attendanceRepository.findById(request.attendanceId())
                 .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + request.attendanceId()));
+
+        if (request.reason() == null || request.reason().isBlank()) {
+            throw new BusinessException("A reason is required for attendance correction");
+        }
+        if (authentication != null && authentication.getAuthorities().stream()
+                .anyMatch(a -> "ROLE_COACH".equals(a.getAuthority()))) {
+            User actor = userRepository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
+            if (att.getSession() == null || att.getSession().getSportsClass() == null
+                    || att.getSession().getSportsClass().getCoach() == null
+                    || !actor.getId().equals(att.getSession().getSportsClass().getCoach().getId())) {
+                throw new AccessDeniedException("Coaches may only correct attendance for their own classes");
+            }
+        }
 
         String oldState = att.getState();
```

**Giải thích:** POST `/api/attendances/correct` truyền Authentication; reason phải khác rỗng; coach chỉ được sửa attendance lớp của mình. API correction riêng yêu cầu reason cho mọi correction; quy tắc reason ở training-results chỉ bắt buộc khi đổi trạng thái buổi quá khứ. Hai đường API không nên được mô tả là giống hệt nhau.

### B09 — giảm query lặp khi tải lớp/lịch/staff

| File/hàm | Thay đổi | Ý nghĩa |
|---|---|---|
| [enrollment/ClassEnrollmentRepository.java](src/main/java/com/sportscenter/enrollment/ClassEnrollmentRepository.java), `countGroupedByClass()` khoảng 52 | Một GROUP BY trả classId/count theo các status truyền vào | Tránh query count riêng cho mỗi lớp. |
| [sportclass/SportsClassService.java](src/main/java/com/sportscenter/sportclass/SportsClassService.java), `findAll()` dòng 28 | Dùng grouped counts Registered + Pending | Giữ quy tắc số chỗ đã giữ; không đổi thành chỉ đếm người đã trả tiền. |
| [session/SessionService.java](src/main/java/com/sportscenter/session/SessionService.java), `findAll()` khoảng 28 | Grouped counts Registered | Giữ cách tính sĩ số lịch trước đó; khác catalog có Pending là có chủ đích trong code hiện tại. |
| [user/UserRepository.java](src/main/java/com/sportscenter/user/UserRepository.java), `findAll()` khoảng 15 | `@EntityGraph(attributePaths = "role")` | Tải role cùng user, giảm query riêng từng user. |
| [user/StaffService.java](src/main/java/com/sportscenter/user/StaffService.java), `findAllStaff()` dòng 38 | Lấy `allClasses` một lần rồi tính count cho coach | Trước đó `findAll()` lớp bị gọi lại cho từng coach. |

Code thay đổi chính xác:

```diff
--- HEAD/src/main/java/com/sportscenter/enrollment/ClassEnrollmentRepository.java
+++ CURRENT/src/main/java/com/sportscenter/enrollment/ClassEnrollmentRepository.java
@@ -49,4 +49,7 @@
     long countBySportsClassIdAndStatusIn(Integer classId, List<String> statuses);
 
+    @Query("SELECT e.sportsClass.id, COUNT(e) FROM ClassEnrollment e WHERE e.status IN :statuses GROUP BY e.sportsClass.id")
+    List<Object[]> countGroupedByClass(@Param("statuses") List<String> statuses);
+
     boolean existsByMemberIdAndSportsClassIdAndStatus(Integer memberId, Integer classId, String status);
 }
```
```diff
--- HEAD/src/main/java/com/sportscenter/sportclass/SportsClassService.java
+++ CURRENT/src/main/java/com/sportscenter/sportclass/SportsClassService.java
@@ -27,6 +27,8 @@
     @Transactional(readOnly = true)
     public List<SportsClassResponse> findAll() {
+        var counts = classEnrollmentRepository.countGroupedByClass(List.of("Registered", "Pending")).stream()
+                .collect(java.util.stream.Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).intValue()));
         return repository.findAll().stream().map(c -> {
-            int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatusIn(c.getId(), List.of("Registered", "Pending"));
+            int count = counts.getOrDefault(c.getId(), 0);
             return SportsClassResponse.from(c, count);
         }).toList();
```
```diff
--- HEAD/src/main/java/com/sportscenter/session/SessionService.java
+++ CURRENT/src/main/java/com/sportscenter/session/SessionService.java
@@ -40,6 +40,8 @@
             list = sessionRepository.findFiltered(classId, coachId, roomId, startDate, endDate, status);
         }
+        var counts = classEnrollmentRepository.countGroupedByClass(List.of("Registered")).stream()
+                .collect(java.util.stream.Collectors.toMap(r -> (Integer) r[0], r -> ((Number) r[1]).intValue()));
         return list.stream().map(s -> {
-            int count = (int) classEnrollmentRepository.countBySportsClassIdAndStatus(s.getSportsClass().getId(), "Registered");
+            int count = counts.getOrDefault(s.getSportsClass().getId(), 0);
             return SessionResponse.from(s, count);
         }).toList();
```
```diff
--- HEAD/src/main/java/com/sportscenter/user/UserRepository.java
+++ CURRENT/src/main/java/com/sportscenter/user/UserRepository.java
@@ -11,4 +11,7 @@
 
 public interface UserRepository extends JpaRepository<User, Integer> {
+    @Override
+    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "role")
+    java.util.List<User> findAll();
     @Query("select distinct u from User u " +
             "left join fetch u.role r " +
```
```diff
--- HEAD/src/main/java/com/sportscenter/user/StaffService.java
+++ CURRENT/src/main/java/com/sportscenter/user/StaffService.java
@@ -38,4 +38,5 @@
     public List<StaffResponse> findAllStaff() {
         List<User> users = userRepository.findAll();
+        var allClasses = sportsClassRepository.findAll();
         Map<Integer, Coach> coachMap = coachRepository.findAllWithUser().stream()
                 .collect(Collectors.toMap(Coach::getUserId, c -> c));
@@ -49,5 +50,5 @@
                     Coach coach = coachMap.get(u.getId());
                     int activeClassesCount = "Coach".equalsIgnoreCase(u.getRole().getName())
-                            ? (int) sportsClassRepository.findAll().stream()
+                            ? (int) allClasses.stream()
                                     .filter(c -> c.getCoach() != null && u.getId().equals(c.getCoach().getId()))
                                     .count()
```

Các sửa này nằm ở BE; FE không cần component/route mới để được hưởng lợi. Chưa tuyên bố một mức thời gian đáp ứng/SLA cố định từ thay đổi này.

## 1.6. DB: cấu trúc đang dùng và dữ liệu đã ghi

**Không thêm/chạy DDL trong đợt hoàn thiện demo.** Supabase kiểm tra có cấu trúc Flow1 đến step8 và feedback/payment. Không có bảng history Flyway/Liquibase để xác định chắc DB được dựng từ file nào/thời điểm nào; không gọi một snapshot là “version chính xác” chỉ vì tên file.

| Cấu trúc đã có | Dùng để khắc phục/đáp ứng |
|---|---|
| `class_feedbacks`, unique `(class_id, member_id)`, FK lớp/member/coach, điểm và comment | Lưu feedback lớp/mentor thật, ngăn gửi trùng và giữ quan hệ đúng. |
| `classes.tuition_fee >= 0` | Giá học phí lớp do BE/DB quyết định; 0 là miễn phí. |
| `member_packages.status` có Pending | Không kích hoạt gói ngay lúc tạo yêu cầu mua. |
| `class_enrollments.status` có Pending | Giữ chỗ có phí trước khi thu tiền. |
| `invoices.class_id`, `subscription_id`, target constraint package/class | Một hóa đơn biết đang thu cho lớp hay gói và biết subscription liên quan. |
| `invoice_code`, sequence, created/expires, transaction ref và status | Mã tra cứu, hạn thu và đối chiếu thanh toán. |
| Unique Pending member/package, unique gateway transaction ref | Ngăn nhiều đơn Pending cùng mục tiêu và callback trùng giao dịch. |

Nguồn SQL để review: [migration_class_feedback.sql](migration_class_feedback.sql), [migration_payment.sql](migration_payment.sql), [schema_postgres.sql](schema_postgres.sql). Audit cấu trúc: [audit/schema-live.txt](audit/schema-live.txt); giải thích khác biệt snapshot trong [REVIEW_CHANGES_2026-10-10.md](REVIEW_CHANGES_2026-10-10.md).

**Dữ liệu đã ghi:** gói DEMO CASH, staff riêng, lớp có phí/miễn phí, session theo ngày Việt Nam; runner UI/HTTP ghi thêm member, invoice, enrollment, attendance, feedback và lớp last-seat riêng. Đây là ghi dữ liệu thử bằng luồng thật, không phải chỉ thay mock. Test LiveCashDemoTest dùng rollback; runner Node không rollback. Không sửa mật khẩu tài khoản cũ, không xóa dữ liệu thử trong lượt tài liệu này.

## 1.7. Contract tối thiểu cho team FE triển khai trên UI chính

Các JSON dưới đây là **ví dụ hình dạng payload/response**, ID/mã/ngày thay bằng dữ liệu BE trả; không phải seed bắt buộc hoặc bản ghi DB để copy. List hiện dùng array trực tiếp, summary feedback dùng object có mảng `feedback`.

### C01 — đăng ký → xác thực → login

```http
POST /api/auth/register
Content-Type: application/json

{"fullName":"Demo Member","email":"member@example.test","phone":"0900000000","password":"DemoCash2026"}

POST /api/auth/verify-registration

{"email":"member@example.test","otp":"123456"}

POST /api/auth/resend-otp

{"email":"member@example.test","purpose":"REGISTER"}

POST /api/auth/login

{"email":"member@example.test","password":"DemoCash2026"}
```

Trong test hiện tại register trả 200, verify trả 204 không JSON, login trả 200 và cookie. FE dùng `credentials: 'include'`; reload lấy `/api/auth/me`. LoginResponse có **userId**/email/fullName/role/authorities/isFirstLogin/memberId; model context FE có thể ánh xạ userId thành id, không nhầm tên field của JSON BE. Không suy ra đã login chỉ từ verify 204. Password BE cần ≥8 ký tự, có chữ và số. OTP 123456 chỉ khớp BE có property demo tương ứng.

Member profile: `GET /api/members/me`; demo đọc `id ?? memberId`, kiểm tra ID số nguyên dương. Không gọi GET danh sách member toàn hệ thống từ role member để lấy profile.

### C02 — mua gói: order khác subscription

```http
POST /api/members/me/subscriptions

{"packageId":12,"paymentMethod":"Cash"}
```

Lễ tân tạo hộ dùng `POST /api/members/{memberId}/subscriptions`. Alias `/api/members/{memberId}/packages` có cùng handler BE; quyền vẫn là staff, không vì giữ alias mà member được gọi.

Response 201 dạng `SubscriptionOrderResponse`:

```json
{"subscriptionId":99,"invoiceId":42,"invoiceCode":"INV-2026-000042","amount":300000,"paymentMethod":"Cash","status":"Pending","expiresAt":"2026-10-10T15:00:00","paymentUrl":null}
```

`status` ở đây lấy từ **invoice**. Lưu mã order/hiển thị chờ thu tiền; sau confirm đọc GET `/api/members/{memberId}/subscriptions` (member chỉ đọc đúng mình) và own invoices để hiển thị Active/Paid. Không map response này như subscription đầy đủ ngay sau POST.

### C03 — thu Cash và hủy

```http
GET /api/members/me/invoices
GET /api/members/me/invoices/42
GET /api/invoices?status=Pending&method=Cash&keyword=INV-2026
POST /api/invoices/42/confirm-cash
POST /api/invoices/42/cancel
```

Confirm không cần amount/receptionistId/body. Chỉ receptionist có `MANAGE_INVOICES` được confirm. Manager trong demo chỉ đọc. Cancel dành cho chủ invoice hoặc staff có quyền và invoice còn Pending. InvoiceResponse có `invoiceId`, `invoiceCode`, `amount`, `paymentStatus`, `paymentMethod`, `member*`, `package*`, `classId/className`, `createdAt/expiresAt`, `paymentDate`, `receptionist*`, `gatewayTransactionRef`. Package hoặc class có thể null tùy mục tiêu; không yêu cầu cả hai cùng có.

FE đọc field invoice `paymentStatus`; không dùng `status` của enrollment/subscription thay thế. Cash Paid/Cancelled/Expired không confirm lại; conflict 409 cần đọc lại trạng thái. Invoice không của member qua own-by-ID trả 404 trong smoke test.

### C04 — lớp, ghi danh và giữ chỗ

```http
GET /api/subjects
GET /api/classes
GET /api/enrollments
POST /api/classes/29/enroll

{"paymentMethod":"Cash"}
```

Lễ tân thêm `memberId` số vào body; member để BE lấy từ phiên. Class DTO dùng `tuitionFee`, `maxCapacity`, `enrolledCount`, `subjectId`, `coachId`, ngày và status.

Enrollment POST 201: có `id`, `classId`, `memberId`, `status` cùng thông tin liên quan; lớp có phí bổ sung `invoiceId`, `invoiceCode`, `paymentStatus`, `paymentUrl` và ở Pending. Lớp miễn phí Registered ngay. **EnrollmentResponse không có field amount**; lấy học phí từ class hoặc amount từ invoice, không đòi response enroll phải trả amount. Các GET enrollment dùng mapper `from(enrollment)` có thể không có invoice fields, nên đừng dùng list enrollment làm nguồn duy nhất cho hóa đơn.

Đọc `GET /api/enrollments?memberId=...` cho staff theo member; scope của member do BE kiểm soát. Hủy dùng `PATCH /api/enrollments/{id}/cancel` với `{}`. Mua gói chưa Paid hoặc gói chưa đến ngày bắt đầu không đủ điều kiện enroll. Pending tính vào số chỗ giữ; tranh chỗ cuối có thể một request 201, request còn lại 400.

### C05 — lịch, roster và điểm danh

```http
GET /api/sessions?coachId=7
GET /api/classes/29/enrollments
GET /api/training-results?sessionId=15
POST /api/training-results

{"sessionId":15,"memberId":101,"coachId":7,"attendanceStatus":"Present","content":"Hoàn thành buổi học","reason":""}
```

CoachId phải coach của lớp và của người gọi nếu role coach; roster form chỉ Registered/Completed. Khi đổi trạng thái buổi quá khứ, gửi reason khác rỗng. POST training result trả 200; BE đồng bộ Attendance. Member xem `GET /api/attendances`; DTO attendance dùng `state`, training result dùng `attendanceStatus`, không nhầm hai field.

Bulk: POST `/api/training-results/bulk` với `sessionId`, `coachId` và `items`, mỗi item có `memberId`, `attendanceStatus`, `content`, `reason`; mỗi item qua cùng guard. Correction riêng: POST `/api/attendances/correct` với `{"attendanceId":10,"newState":"Present","reason":"Sửa theo danh sách đã đối chiếu"}`, bắt buộc reason và quyền đúng lớp. DTO correction này dùng `newState`, không dùng `attendanceStatus`.

### C06 — feedback lớp/mentor

```http
GET /api/classes/29/feedback/me
POST /api/classes/29/feedback

{"classId":29,"coachId":7,"classRating":5,"coachRating":4,"comment":"Buổi học dễ theo dõi"}
```

POST trả 201 `ClassFeedbackResponse`: feedbackId/classId/coachId/hai điểm/comment/createdAt. GET feedback/me chưa có trả 404, có thể hiển thị form; lỗi 403/500 không được xử lý như “chưa gửi”. POST trùng trả 409. BE kiểm tra lớp Ongoing/Closed, Registered/Completed, đã Present; không chỉ dựa vào việc FE hiện nút.

Manager: GET `/api/classes/{id}/feedback` trả classId/totalFeedback/averageClassRating/averageCoachRating/feedback. Coach: GET `/api/coaches/me/feedback?classId=...` (classId tùy chọn) trả totalFeedback/averageCoachRating/feedback; không có field định danh member trong review. Không dùng module `evaluations` để thay feedback lớp/mentor này.

### C07 — tạo/sửa lớp và tạo buổi

```http
POST /api/classes

{"name":"Lớp demo","subjectId":1,"coachId":7,"maxCapacity":4,"tuitionFee":150000,"startDate":"2026-10-10","endDate":"2026-10-17","status":"Ongoing"}

POST /api/sessions

{"classId":29,"roomId":2,"sessionDate":"2026-10-10","startTime":"08:00","endTime":"08:30","status":"Scheduled"}
```

Sửa lớp dùng PUT `/api/classes/{id}`. Học phí là cả lớp, không phải giá mỗi buổi. Giá invoice đã tạo là giá lúc tạo đơn; đổi tuitionFee sau đó không nên được hiểu là tự đổi amount invoice cũ. Phòng/coach/sức chứa/trùng giờ do BE kiểm tra; FE nên hiển thị lỗi cụ thể.

### C08 — status, lỗi và đọc lại

| Đối tượng | Field BE | Status dùng trong demo | FE nên làm |
|---|---|---|---|
| Order mua gói | `status` | Pending/Paid… của invoice | Hiện yêu cầu thu tiền, không coi là subscription Active. |
| Subscription GET | `status` | Pending/Active/Expired/Cancelled | Hiện hiệu lực và ngày; Active không đủ nếu chưa đến ngày. |
| Invoice | `paymentStatus` | Pending/Paid/Cancelled/Expired… | Confirm chỉ Cash Pending; đọc lại sau mutation. |
| Enrollment | `status` | Pending/Registered/Cancelled/Completed | Pending chưa được điểm danh. |
| Class | `status` | Open/Ongoing/Closed/Cancelled | Cần Ongoing/Closed để gửi feedback. |
| Attendance | `state` | Present/Absent/Late | Phân biệt attendance check-in chung và buổi học. |
| Training result | `attendanceStatus` | Present/Absent/Late | Kèm content/reason và ID session. |

401: phiên hết hạn/chưa login. 403: thiếu quyền hoặc điều kiện tham gia/feedback. 404: đối tượng không tìm thấy/không thuộc scope hoặc chưa có feedback tùy endpoint. 400: request/điều kiện nghiệp vụ không hợp lệ. 409: conflict như invoice đã xử lý/feedback trùng. Đây là hướng dẫn theo các endpoint đã kiểm, không phải mọi lỗi nghiệp vụ đều có một mã cố định duy nhất. Dùng message BE và không chỉ hiện “thành công” trước khi biết HTTP result.

## 1.8. Test BE được thêm/chỉnh và bằng chứng kiểm tra

| File | Hàm/ca kiểm tra và thay đổi | Loại |
|---|---|---|
| [src/test/java/com/sportscenter/invoice/InvoiceServiceTest.java](src/test/java/com/sportscenter/invoice/InvoiceServiceTest.java), khoảng dòng 64 | Đổi verify từ findByMemberId sang searchInvoices có member scope, phù hợp B04 | Sửa test có trước |
| [src/test/java/com/sportscenter/training/TrainingResultServiceTest.java](src/test/java/com/sportscenter/training/TrainingResultServiceTest.java), setup khoảng 60, test mới 123–160 | Thêm mock enrollment/class/coach; 4 test từ chối Pending/không enrolled, coach sai lớp, giả coachId, sửa quá khứ thiếu reason dù chỉ có Attendance | Sửa test có trước |
| [BusinessGapAuditTest.java](src/test/java/com/sportscenter/audit/BusinessGapAuditTest.java) | `cancelledSubscriptionRequiresPaidInvoiceBeforeReactivation`, `memberIdAndOtherInvoiceFiltersAreAppliedTogether`, `futureActivePackageCannotEnrollToday` | File mới, 3 unit regression |
| [ClassFeedbackAuditTest.java](src/test/java/com/sportscenter/feedback/ClassFeedbackAuditTest.java) | 9 ca: hợp lệ/audit, path/class sai, coach sai, lớp Open, thiếu enrollment/Present, trùng, rating/comment, ẩn danh coach | File mới, unit, từ giai đoạn audit |
| [LiveReadOnlyAuditTest.java](src/test/java/com/sportscenter/audit/LiveReadOnlyAuditTest.java) | 13 ca DB/controller: invoice filters/đủ invoice gói/quyền, feedback, catalog/enrollment, ngày lịch, grouped count; callback unsigned không ghi | File mới, opt-in `scms.live.audit=true`, DB chỉ đọc, mock worker hết hạn |
| [LiveCashDemoTest.java](src/test/java/com/sportscenter/audit/LiveCashDemoTest.java) | `registrationCashEnrollmentAttendanceFeedbackAndReadback`: register/OTP/login, mua gói/thu Cash, lớp có phí/miễn phí, điểm danh, feedback, quyền/lỗi/F5 tương đương đọc lại | File mới, opt-in `scms.live.cash=true`, MockMvc/DB thật, transaction rollback |
| [DemoDataPreparationTest.java](src/test/java/com/sportscenter/audit/DemoDataPreparationTest.java) | `account`, `prepareIsolatedDemoAccountsClassesAndSessions` tạo dữ liệu DEMO để thao tác bằng trình duyệt | File mới, opt-in `scms.prepare.demo=true`, **Commit dữ liệu**, không phải test rollback |

**Kết quả đã chạy trong đợt triển khai, không chạy lại khi viết tài liệu:**

- Backend cuối: **63 test, 62 pass, 1 skip, 0 failure/error**, log [audit/final-cash-regression-tests.log](audit/final-cash-regression-tests.log). Skip là preparation opt-in đã chạy riêng trước đó. Kết quả 62 all-pass ở log cũ là mốc trước khi thêm regression cuối, không mâu thuẫn với 63/1-skip.
- FE 5 bộ: G1 logic, G1 guard, G2, G4, FE2 pass; build pass. Test logic/mock và build không thay thế acceptance UI thật.
- UI thật: **8/8 pass**, [audit/browser-cash-result.json](audit/browser-cash-result.json): đăng ký OTP, mua gói/F5, lớp có phí Pending, Cash/free enrollment, Present, feedback/F5, manager đọc, coach ẩn danh.
- HTTP/concurrency thật: **4/4 pass**, [audit/cash-concurrency-result.json](audit/cash-concurrency-result.json): confirm đồng thời 200/409, chỉ một Pending ở chỗ cuối lớp có phí, Cash → Registered, cancel đồng bộ/đọc invoice khác 404.
- **Phạm vi bằng chứng:** UI/browser đã kiểm là FE có LiveDemoViews và route replacement. Không chứng minh FE ZIP nguyên bản, FE deploy khác hoặc online/email thật đã chạy được.

# PHẦN 2 — File/code hỗ trợ demo, chức năng và mức cần thiết cho FE chính

## 2.1. Phân loại để team FE quyết định

“Cần hành vi/contract” nghĩa là FE cần triển khai tương đương nếu chọn demo chức năng đó, **không có nghĩa bắt buộc copy file hoặc thiết kế UI này**. BE fixes về tính đúng đắn tách biệt với màn hình demo.

| File/code | Chức năng | Cần cho FE chính? | Nếu không áp dụng thì làm gì? |
|---|---|---|---|
| F01 authApi 3 hàm | Đăng ký/verify/resend thật | Cần hành vi nếu có đăng ký; tên file/hàm tùy FE | Viết service tương đương, form gọi endpoint đúng. |
| F02 handler LoginView | Chuyển mock registration thành API | Cần logic tương đương; không bắt buộc lấy UI/text mới | Giữ form hiện tại, thay handler; quyết định UX OTP demo riêng. |
| F03–F05 invoice/membership adapters | Contract mua gói/thu Cash/hủy và đọc invoice | Cần tương thích contract nếu demo thanh toán | Review caller; tránh lấy adapter làm caller cũ bị đổi return type bất ngờ. |
| F06 tuitionFee mapper | Đọc/gửi học phí đúng BE | Cần field đúng nếu quản lý/đăng ký lớp có phí | Ánh xạ trong model/service chính, giữ miễn phí 0. |
| F07 context | Không hiện lớp mock khi API rỗng/lỗi, scope role | Cần hành vi đúng dữ liệu/quyền; không bắt buộc cách set state này | Áp dụng cơ chế loading/error/scope phù hợp state store của FE. |
| F08 demoApi.js — mới | Adapter tập hợp endpoint demo | **Không bắt buộc giữ file** | Chuyển hàm cần thiết sang domain services. Không xóa trước khi sửa imports/callers. |
| F09 LiveDemoViews.jsx — mới | Màn hình thao tác Cash/enroll/attendance/feedback | **Không bắt buộc; UI demo tham khảo** | Giữ UI chính và tích hợp logic tương đương. |
| F10 route replacement trong App.jsx | Dẫn route sang UI demo khi API bật | **Không cần cho BE; không bắt buộc cho FE chính** | Team FE tự giữ route/component cũ; bỏ lazy imports/callers nếu bỏ file demo. |
| F11–F12 test FE | Kiểm tra logic theo contract | Không tham gia runtime; nên có regression tương đương | Viết assertion theo architecture và DTO của FE chính. |
| B01 memberId + `/members/me` | ID/profile thật theo phiên | API dùng được cho FE chính | Chọn một nguồn profile/ID thật; không suy ID từ code. |
| B03–B09 BE fixes | Query PostgreSQL, giao dịch/quyền/hiệu lực gói và tốc độ | Không phải file demo FE; nên giữ tính đúng đắn ở BE | FE xử lý guard/lỗi; không cần dùng màn demo để hưởng fixes. |
| B02 + application-demo.properties | OTP cố định, SMTP health off, pool phù hợp demo | **Chỉ môi trường demo**, không phải code FE | Khi dùng email thật bỏ property OTP cố định, cấu hình mail và đổi text FE có kiểm soát. |
| DemoDataPreparationTest | Tạo fixture staff/gói/lớp/lịch | Không cần ship trong FE chính | Dùng dữ liệu riêng phù hợp môi trường; không hardcode ID fixture vào FE. |
| LiveCash/LiveReadOnly/BusinessGap/ClassFeedback tests | Bằng chứng BE regression/DB | Không nằm trong FE bundle | Team FE dùng như tiêu chí integration. |
| Node browser/concurrency runner | Acceptance UI/HTTP thật | Không phải source FE chính | Giữ trong test tooling; runner UI cần cập nhật nếu UI đổi. |
| SchemaAudit/ConnectionAudit | Đọc schema và connection/locks khi debug | Không cần cho FE runtime | Chỉ dùng công cụ đối chiếu; không copy credentials vào FE. |
| Markdown, JSON/log, ảnh audit | Bàn giao, bằng chứng, hướng dẫn | Không phải chức năng FE | Giữ tài liệu/test evidence nếu cần, không import vào UI. |

## 2.2. Profile BE demo: code mới và ý nghĩa từng property

File mới [src/main/resources/application-demo.properties](src/main/resources/application-demo.properties):

```properties
# Explicit demo profile: registration OTP is fixed; OTP expiry/use/attempt guards remain active.
registration.demo-otp=123456
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.max-lifetime=240000
spring.datasource.hikari.keepalive-time=60000
spring.datasource.hikari.validation-timeout=15000
management.health.mail.enabled=false
```

| Property | Mục đích | Cần cho FE chính? |
|---|---|---|
| `registration.demo-otp=123456` | Bật OTP cố định và bỏ gửi mail trong RegistrationService | Không; FE vẫn verify qua API. Không bật mặc định khi dùng email thật. |
| pool max 3/min idle 1 | Giảm số connection đồng thời để chạy demo trên pool Supabase hiện tại | Không; cấu hình vận hành BE, không phải quy tắc nghiệp vụ. |
| max lifetime 240000/keepalive 60000/validation timeout 15000 | Điều chỉnh tuổi/giữ/kiểm tra kết nối sau vấn đề kết nối khi test/demo | Không; cần đánh giá theo môi trường deploy, không sao chép như giá trị tối ưu mọi hệ thống. |
| `management.health.mail.enabled=false` | Không để kiểm tra SMTP ảnh hưởng health khi email đang hoãn | Không; không tắt các guard OTP/auth và không thay mail production toàn cục. |

Chạy BE demo từ root:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Chạy FE với API/proxy hiện có:

```powershell
cd FE_Project-main
$env:VITE_API_ENABLED = 'true'
$env:VITE_API_PROXY_TARGET = 'http://localhost:8080'
npm.cmd run dev
```

Không cần hai FE port để mở hai role. Dùng cùng FE ở cửa sổ thường và ẩn danh hoặc hai browser/profile. Cookie không được tách chỉ bởi đổi port; các tab cùng profile dùng chung phiên. Sau thao tác role kia, màn hiện tại cần Tải lại/F5.

## 2.3. Dữ liệu demo: file tạo, mức tác động và tính phụ thuộc

[DemoDataPreparationTest.java](src/test/java/com/sportscenter/audit/DemoDataPreparationTest.java): dòng 27 opt-in; `account()` khoảng 44; `prepareIsolatedDemoAccountsClassesAndSessions()` dòng 62.

Code bật chuẩn bị dữ liệu và commit:

```java
@EnabledIfSystemProperty(named="scms.prepare.demo", matches="true")
// ...
@Test @Transactional @Commit
void prepareIsolatedDemoAccountsClassesAndSessions()
```

**Nó tạo gì:**

- Manager `demo.cash.manager.1010@example.test`, receptionist `demo.cash.reception.1010@example.test`, coach `demo.cash.coach.1010@example.test`; password lúc tạo mới `DemoCash2026`, hash qua PasswordEncoder; role/subtype đúng để login và dùng API.
- Nếu email đã có, kiểm tra role phù hợp và **không reset password**. Không nên suy password trên chắc chắn đúng nếu tài khoản đã được đổi password sau khi tạo.
- Gói `DEMO CASH 30 ngày`, 300.000 đ, 30 ngày, Active nếu chưa có theo tên.
- Lớp `DEMO CASH Có phí YYYY-MM-DD` học phí 150.000 đ, lớp miễn phí 0; sức chứa 4, coach demo, Ongoing, thời gian từ hôm nay đến +7 ngày.
- Chọn subject Active và room Available có capacity ≥4. Tạo buổi hôm nay nếu chưa có, tìm slot 30 phút trong 08:00–22:00 không trùng phòng/coach. Không tìm được thì báo lỗi cần chọn lại dữ liệu, không tự thay quy tắc nghiệp vụ.

Chạy từ root **chỉ khi chủ động muốn tạo/commit fixture**:

```powershell
.\mvnw.cmd -B -ntp test '-Dtest=DemoDataPreparationTest' '-Dscms.prepare.demo=true'
```

**Không cần cho FE chính:** không import class test, không hardcode ID package/class/session; tìm dữ liệu từ API. Fixture được tạo lại theo ngày demo khác, không coi một session cũ là buổi hôm nay. Nếu không có role/room/subject phù hợp, cần xử lý dữ liệu môi trường chứ không sửa FE để giả lập đã đủ.

## 2.4. Test và runner demo: phân biệt rollback với ghi thật

| File/công cụ | Hàm/code chính, chức năng | Ghi dữ liệu không? | Điều kiện chạy và mức cần thiết |
|---|---|---|---|
| `src/test/java/com/sportscenter/audit/LiveCashDemoTest.java` | `registrationCashEnrollmentAttendanceFeedbackAndReadback`, helper auth/JSON; MockMvc qua BE + DB thật | Tạo dữ liệu trong transaction **rollback**, worker hết hạn được mock | Opt-in `scms.live.cash=true`, profile demo trong test; không cần chạy server FE. Không nằm trong FE chính. |
| `src/test/java/com/sportscenter/audit/LiveReadOnlyAuditTest.java` | 13 test như mục 1.8; connection read-only/default read-only, mock worker | Chỉ đọc/ca bị từ chối không ghi | Opt-in `scms.live.audit=true`; Supabase truy cập được. Không phải test UI. |
| `src/test/java/com/sportscenter/audit/BusinessGapAuditTest.java` và `src/test/java/com/sportscenter/feedback/ClassFeedbackAuditTest.java` | Unit guards và contract; mock repositories | Không ghi DB thật | Maven unit regression; không cần copy vào FE. |
| [audit/browser-cash-demo.mjs](audit/browser-cash-demo.mjs) | `send`, `evaluate`, `wait`, `navigate`, helper nhập/click/login; Chrome CDP headless, thao tác UI thật qua các role, ảnh và JSON; `--resume` dùng member/class trước | **Có, ghi thật và giữ lại**, không rollback | BE demo 8080, FE API 127.0.0.1:5173, Node có fetch/WebSocket, Chrome ở đường dẫn Windows hardcode, port debug 9333, fixture DEMO. Runner phụ thuộc text/DOM/màn demo; cần sửa test runner khi UI chính đổi. |
| [audit/cash-concurrency.mjs](audit/cash-concurrency.mjs) | `call`, `login`, `check`; HTTP thật với cookie, Promise.all race; tạo lớp last-seat 100.000 đ, maxCapacity 1 | **Có**, thêm order/member/class/enrollment/thu tiền giả lập | BE demo, staff/fixture, **browser-cash-result.json passed=true** và member từ lần UI trước; không phải test chạy độc lập tùy ý. Không cần cho runtime FE. |
| [audit/SchemaAudit.java](audit/SchemaAudit.java) | `main`: đọc DB config từ `.env`, JDBC SELECT schema/constraint/index/sequence/thống kê, timeout | Chỉ đọc | Cần JDBC PostgreSQL/classpath; không in credentials. Là audit DB, không phải migration. |
| [audit/ConnectionAudit.java](audit/ConnectionAudit.java) | `main`: SELECT pg_stat_activity, pid/state/wait/blocking pids, timeout | Chỉ đọc | Debug pool/locks; không dừng session hoặc sửa DB. Không cần cho FE. |
| [audit/inspect-demo-page.mjs](audit/inspect-demo-page.mjs) | Kết nối Chrome debug 9333, Runtime.evaluate đọc path/alert/title/status mặc định | Mặc định chỉ đọc DOM; cho phép truyền expression | Công cụ debug tạm, cần Chrome CDP còn chạy. Không cần FE chính; expression tùy ý có thể có tác động, không coi mọi cách gọi đều read-only. |
| [audit/recover-browser-context.mjs](audit/recover-browser-context.mjs) | Login demo lễ tân, tìm member `DEMO Browser Cash`, đọc invoice, cập nhật memberEmail/classId trong kết quả runner | Tạo phiên login; đọc nghiệp vụ, **sửa JSON local**, không tạo giao dịch demo | Phục hồi lần chạy dở trong workspace này; không dùng như công cụ chuẩn bị dữ liệu tổng quát. |

Lệnh kiểm tra có sẵn, **không chạy lại chỉ để đọc tài liệu**:

```powershell
# Tại root: unit/regression mặc định
.\mvnw.cmd -B -ntp test
# Opt-in DB chỉ đọc
.\mvnw.cmd -B -ntp test '-Dtest=LiveReadOnlyAuditTest' '-Dscms.live.audit=true'
# Opt-in luồng Cash rollback
.\mvnw.cmd -B -ntp test '-Dtest=LiveCashDemoTest' '-Dscms.live.cash=true'
# UI thật ghi dữ liệu; cần server demo và fixture
node audit/browser-cash-demo.mjs
# Sau UI runner; HTTP concurrency ghi dữ liệu
node audit/cash-concurrency.mjs
```

FE tests/build từ `FE_Project-main/`:

```powershell
npm.cmd run build
node test/g1_logic_test.mjs
node test/g1_guard_test.mjs
node test/g2_logic_test.mjs
node test/g4_logic_test.mjs
node test/fe2_membership_payment_test.mjs
```

Không cần đưa các runner vào bundle FE. Team FE có thể giữ test nghiệp vụ và viết lại thao tác browser theo UI mình chọn. Trong đợt này chưa nghiệm thu online/email thật, chưa chạy acceptance lại trên UI ZIP gốc, không tuyên bố toàn bộ chức năng ngoài Cash đã được kiểm toàn diện.

## 2.5. Tài liệu và bằng chứng được thêm/cập nhật

| File/nhóm | Nội dung | Cần cho FE chính? |
|---|---|---|
| [PROGRESS_2026_10_10.md](PROGRESS_2026_10_10.md) — thêm | Checklist, nhật ký, kết quả/giới hạn, trạng thái bàn giao | Để theo dõi; “hoàn tất” là phạm vi Cash trên FE đã chỉnh sửa, không phê duyệt thiết kế FE chính. |
| [DEMO_FE_BE_2026_10_10.md](DEMO_FE_BE_2026_10_10.md) — thêm | Lệnh chạy, tài khoản, dữ liệu, từng thao tác/expected result; bổ sung demo member/receptionist song song | Hướng dẫn chạy bản demo hiện tại; tên nút/route/component cần cập nhật nếu FE chọn UI khác. |
| [REVIEW_CHANGES_2026-10-10.md](REVIEW_CHANGES_2026-10-10.md) — thêm ở giai đoạn audit, cập nhật block đầu sau triển khai | Báo cáo đối chiếu code/DB ban đầu, lỗi/phạm vi đề xuất; block đầu trỏ tiến độ mới | Đọc thời điểm từng kết luận; đoạn “chưa sửa FE/chỉ SELECT” là audit trước triển khai, không mô tả trạng thái sau demo. |
| `CHANGES_NOTE_101026.md` — thêm trong lượt này | Bàn giao thay đổi, code/vị trí, demo vs FE chính, quyết định áp dụng | Tài liệu để team FE review, không tự phê duyệt bất kỳ mục nào. |
| `audit/browser-cash-result.json`, `audit/cash-concurrency-result.json` | Kết quả assertions; browser result có memberEmail/classId cho resume/concurrency | Bằng chứng/fixture runner, không phải cấu hình app FE. |
| `audit/demo-cash-member.png`, `audit/demo-cash-coach.png`, `audit/demo-cash-receptionist.png` | Ảnh UI đã kiểm | Minh họa, không phải asset thiết kế mới phải import vào FE. |
| `audit/schema-live.txt` | Snapshot schema đọc được lúc audit | Giúp đối chiếu; không phải migration hoặc version history. |
| `audit/backend-threads.txt`, `audit/backend-current-threads.txt` | Thread dump khi điều tra độ trễ/pool | Debug evidence; không cần FE/app runtime. |
| `audit/*.log` | Logs build/test/dev: backend tests, invoice query regression, live Cash/read, FE suites/build, classpath/dev | Các mốc có thể cũ hơn kết quả cuối; dùng final-cash-regression + result JSON để đánh giá bản cuối. Không import vào FE. |

`CHECK_TARGET_SWP391_10_10_26.md/.pdf`, `FE_Project-main.zip`, các tài liệu dự án có trước được dùng làm đầu vào đối chiếu. Không sửa/gán việc tạo các đầu vào đó cho lượt viết note này. Các script patch một lần đã được xóa sau triển khai không phải phụ thuộc để chạy app/demo.

## 2.6. Checklist để team FE chốt từng phần — chưa áp dụng tự động

Các ô dưới đây là **quyết định của team FE**, không phải tiến độ đã hoàn tất. Team FE có thể đánh dấu áp dụng, thay cách triển khai hoặc bỏ theo nhu cầu.

- [ ] Chốt dùng UI/route FE chính; đánh giá từng route trong F10, nhất là dashboard/progress bị gom vào màn demo.
- [ ] Chốt vị trí các service: giữ demoApi tạm hay chuyển endpoint sang auth/membership/invoice/class/session/feedback services.
- [ ] Chốt model DTO: dùng raw BE hay mapper FE; giữ thống nhất status và các field id/invoiceId/subscriptionId.
- [ ] Lấy logic đăng ký/verify/resend/login thật; tách thông báo OTP demo khỏi UI dùng email thật.
- [ ] Review validation password và cách hiển thị lỗi register/OTP/login trong form chính.
- [ ] Review mọi caller của subscribePackage/createInvoice/payInvoice/updateInvoiceStatus trước khi áp dụng adapter.
- [ ] Member dùng API own/profile của mình; receptionist gọi API staff với quyền; không tự gửi người thu/amount.
- [ ] Hiển thị Pending/Active/Paid/Registered đúng đối tượng; tải lại subscriptions/invoices/enrollments sau mutation.
- [ ] Dùng tuitionFee cả lớp, giữ 0; chốt nguồn duy nhất nếu model có cả price và tuitionFee.
- [ ] Attendance dùng ID thật, roster hợp lệ, reason correction; handling bulk không báo thành công khi transaction lỗi.
- [ ] Feedback theo classId/coachId, 404 chưa có khác 403 lỗi quyền/điều kiện, 409 trùng; coach không hiện danh tính member.
- [ ] Quyết định refresh thủ công/polling khác theo UX chính; demo hiện chỉ refresh thủ công.
- [ ] Nếu bỏ LiveDemoViews/demoApi: xử lý toàn bộ import/caller trước khi xóa; không dùng git restore repo gốc để mong phục hồi thư mục FE untracked.
- [ ] Chạy integration/browser lại trên **FE chính đã chọn**, rồi mới kết luận FE chính đạt toàn bộ demo. Không dùng kết quả UI demo cũ thay nghiệm thu này.

**Phụ thuộc khi chọn tối thiểu:** BE fixes không đòi route replacement. FE chính có thể giữ màn hình cũ, triển khai service/payload theo mục 1.7, xử lý DTO và state thật theo F01–F07. F08/F09/F10 là cách tổ chức demo có thể bỏ hoặc viết lại. Việc bỏ/khôi phục chưa được thực hiện; note chỉ cung cấp bằng chứng để team FE quyết định.
