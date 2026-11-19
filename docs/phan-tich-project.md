Tôi đã đối chiếu BE hiện tại và repo FE mới nhất ngày **14/11/2026**. Dưới đây là danh sách có thể dùng cho báo cáo; tính năng có trong source chưa đồng nghĩa đã nghiệm thu đầy đủ trên website.

**1. Các công nghệ của project**

**Công nghệ và thư viện trong code**

| Thành phần | Công nghệ | Dùng để làm gì |
|---|---|---|
| Backend | **Java 17, Spring Boot 3.3.5** | Xử lý nghiệp vụ và cung cấp REST API |
| Build BE | **Maven, Maven Wrapper** | Quản lý dependency, build và chạy test |
| API | **Spring Web MVC, JSON** | Giao tiếp giữa FE và BE |
| Bảo mật | **Spring Security, session cookie `JSESSIONID`, RBAC** | Đăng nhập, phân quyền theo vai trò/quyền và phạm vi dữ liệu |
| Truy cập dữ liệu | **Spring Data JPA, Hibernate, JDBC** | Đọc/ghi DB, transaction và khóa dữ liệu |
| Kiểm tra dữ liệu | **Jakarta Bean Validation** | Kiểm tra dữ liệu đầu vào |
| Frontend | **React 19, JavaScript/JSX, Vite 8** | Xây dựng và đóng gói giao diện web |
| Giao diện | **Tailwind CSS 4** | Thiết kế giao diện |
| Điều hướng và trạng thái | **React Router, React Context** | Chuyển trang, quản lý phiên và dữ liệu dùng chung |
| Gọi API | **Fetch API** | FE gửi request và cookie phiên tới BE |
| Thành phần UI | **Lucide React, Motion, Recharts** | Icon, hiệu ứng và biểu đồ |
| QR | **qrcode.react; TOTP/HMAC ở BE** | Hiển thị và xác minh mã điểm danh |
| Tác vụ định kỳ | **Spring Scheduler — `@Scheduled`** | Chạy tác vụ tự động theo khoảng thời gian hoặc biểu thức cron |
| Xử lý bất đồng bộ | **Spring Async, thread pool** | Xử lý email và tác vụ AI |
| Tài liệu API | **Springdoc OpenAPI, Swagger UI** | Mô tả và thử API |
| Theo dõi ứng dụng | **Spring Boot Actuator** | Health check phục vụ kiểm tra và deploy |

Dependency FE được đối chiếu từ [package.json của repo FE](https://github.com/hoanganh21022005/FE_Project/blob/b392bdbe6592d8a7fce4af46a2a3e850f74386e1/package.json). Dependency BE nằm trong [D:\Projects\SWP391\pom.xml](/D:/Projects/SWP391/pom.xml).

**Dịch vụ bên thứ ba và hạ tầng**

| Dịch vụ | Vai trò trong SCMS | Phạm vi hiện tại |
|---|---|---|
| **Supabase — PostgreSQL** | Lưu dữ liệu nghiệp vụ | DB hiện hành; BE truy cập qua JDBC/JPA |
| **Render** | Hosting Backend | Chạy ứng dụng Spring Boot bằng Docker |
| **Cloudflare Pages** | Hosting Frontend | Website `vibesports.pages.dev` |
| **VNPay Sandbox** | Thanh toán trực tuyến | Tạo link thanh toán, nhận IPN, Return và tích hợp QueryDR |
| **Brevo SMTP** | Gửi email | Email OTP và các email tài khoản; dùng Spring Mail/JavaMail |
| **Google Identity Services** | Đăng nhập bằng Google | FE nhận credential, BE xác minh danh tính Google |
| **Cloudinary** | Lưu trữ ảnh | Upload/xóa avatar; đã có tích hợp trong code |
| **DeepSeek API** | AI Chat | Đã có tích hợp; việc bật provider và công cụ đọc DB phụ thuộc cấu hình |
| **Anthropic API** | Provider AI tùy chọn | Có adapter trong code, chưa có căn cứ xác nhận đang dùng trên live |

Với mục email trong hình, nên ghi **“Brevo SMTP — email/OTP”**. Gmail SMTP còn là giá trị mặc định trong code, còn bằng chứng cấu hình và sử dụng của project là Brevo.

**Công cụ phát triển, kiểm thử và triển khai**

| Nhóm | Công nghệ |
|---|---|
| Quản lý source và CI | **Git, GitHub, GitHub Actions** |
| Đóng gói môi trường | **Docker**, image Java Eclipse Temurin |
| Kiểm thử BE | **JUnit 5, Mockito, Spring Boot Test, Spring Security Test, H2**, kiểm tích hợp PostgreSQL |
| Kiểm thử FE | **Playwright**, các script kiểm API/mock |
| Script hỗ trợ | **Python, PowerShell, Node.js/npm** |

Các tác vụ “cronjob” đã thấy trong BE gồm: hết hạn thanh toán, hết hạn quyền học, cập nhật vòng đời lớp Normal, hoàn tất attendance, nhắc buổi học và nhắc quyền học sắp hết hạn.

**2. Các feature của project**

Project có năm vai trò chính: **Admin, Center Manager, Receptionist, Coach và Member**.

| Nhóm feature | Chức năng |
|---|---|
| **Đăng ký tài khoản** | Member tự đăng ký, nhận OTP email, xác minh và gửi lại OTP; nhân viên đăng ký Member tại quầy |
| **Đăng nhập và mật khẩu** | Đăng nhập email/mật khẩu, Google Sign-In, đăng xuất, quên/reset mật khẩu, đổi mật khẩu và cấp lại mật khẩu tạm |
| **Hồ sơ cá nhân** | Xem/sửa thông tin cá nhân, mục tiêu tập luyện, ghi chú sức khỏe; upload/xóa avatar |
| **Quản lý tài khoản** | Quản lý Member, Coach, Receptionist, Manager và Admin theo quyền; đổi trạng thái và vai trò |
| **Phân quyền** | Quản lý role/permission; giới hạn dữ liệu theo người sở hữu, Coach được phân công và trung tâm |
| **Danh mục** | Quản lý bộ môn, học phí Normal/Advanced, chuyên môn Coach và phòng/sân tập |
| **Danh sách Coach** | Tìm/lọc Coach, xem chuyên môn, lịch làm việc, thời gian trống và khả năng nhận học viên |
| **Khóa Normal** | Tạo lớp và lịch 12 buổi, đăng ký lớp, danh sách chờ, kiểm sĩ số, chốt đăng ký và xác nhận khai giảng |
| **Đăng ký Advanced** | Chọn môn và Primary Coach, Coach xét nhận học viên, tạo đơn mua quyền học, theo dõi credit và hạn sử dụng |
| **Lịch học và lịch dạy** | Xem lịch Member/Coach, tạo buổi học, sinh lịch lặp, xem trước lịch, tìm slot trống và kiểm trùng tài nguyên |
| **Thay đổi buổi học** | Đặt buổi Advanced theo quyền, dời/hủy buổi, thay Coach và báo vắng theo điều kiện nghiệp vụ |
| **Thanh toán VNPay** | Tạo link thanh toán, xử lý IPN/Return, xem kết quả và xác minh giao dịch qua QueryDR theo cấu hình |
| **Thanh toán Cash** | Thu/xác nhận tiền mặt tại quầy; đăng ký Normal và thu Cash theo luồng Reception |
| **Hóa đơn và biên nhận** | Xem hóa đơn cá nhân/quản trị, trạng thái thanh toán, biên nhận, hủy đơn và thanh toán online |
| **Đối soát và sửa Cash** | Mở hồ sơ đối soát, duyệt, ghi nhận kết quả và xử lý trường hợp ghi nhận Cash nhầm trong phạm vi cho phép |
| **Normal Lifecycle** | Đề xuất dời khai giảng/chuyển lớp, lấy phản hồi Member, áp dụng phương án và hủy lớp trước khai giảng |
| **Hoàn tiền Normal** | Thu thập/xác minh người nhận, duyệt hoàn, phân công xử lý, nộp chứng từ, đối soát và theo dõi lịch sử/sự cố |
| **Ngày nghỉ** | Quản lý ngày nghỉ trung tâm, xem trước ảnh hưởng và xác nhận thay đổi bằng preview/token |
| **Sự cố trung tâm và học bù** | Ghi nhận sự cố, xác minh buổi không tổ chức, xử lý bù/gia hạn/khôi phục quyền theo trường hợp |
| **Điểm danh** | Điểm danh thủ công đơn/bulk, quét QR theo buổi, Present/Absent, đọc lịch sử và correction có lý do |
| **Giáo án và kết quả tập** | Coach quản lý giáo án, ghi kết quả/nhận xét, sửa nhận xét sau buổi trong thời hạn; Member xem dữ liệu của mình |
| **Đánh giá tiến độ** | Ghi nhận điểm tiến độ, đánh giá học viên và theo dõi lịch sử tập luyện |
| **Feedback** | Member gửi đánh giá lớp; Coach/quản lý xem phản hồi đúng phạm vi |
| **Học viên có nguy cơ bỏ tập** | Xác định danh sách cần quan tâm dựa trên dữ liệu tham gia học |
| **Thông báo** | Feed thông báo, số chưa đọc, đánh dấu đã đọc, nhắc lịch và nhắc quyền học sắp hết hạn |
| **Chat hỗ trợ** | Member chat với nhân viên; nhân viên nhận/nhả hội thoại, trả lời, đóng/mở lại và theo dõi tin chưa đọc |
| **AI Chat** | Trả lời hỗ trợ, gợi ý chuyển sang nhân viên; có công cụ đọc dữ liệu cá nhân khi bật cấu hình |
| **Dashboard và báo cáo** | Thống kê học viên/đăng ký, doanh thu, tiền thực thu, tiền hoàn, tiền ròng và phân nhóm theo kỳ/phương thức |
| **Nhật ký hoạt động** | Tra cứu người thao tác, hành động, đối tượng và thời điểm theo quyền/phạm vi |

Ba điểm cần ghi đúng khi đưa vào báo cáo:

- **Hoàn tiền hiện là quy trình chuyển khoản ngoài SCMS rồi nộp chứng từ/đối soát**, chưa nên gọi là “VNPay tự động hoàn tiền”.
- AI thật, AI đọc DB, check-in/out tại cửa và chế tài khóa đặt lịch có cấu hình bật/tắt; không gom tất cả thành tính năng đang bật.
- Gói hội viên chung tháng/VIP đã được loại khỏi mô hình đang dùng. Sản phẩm hiện hành là **Normal theo lớp** và **Advanced theo môn/Primary Coach**.

Bốn luồng FE đang bàn giao vẫn cần sửa và kiểm lưu thành công; danh sách trên không thay trạng thái nghiệm thu trong [D:\Projects\SWP391\docs\00-MASTER-CHECKLIST.md](/D:/Projects/SWP391/docs/00-MASTER-CHECKLIST.md).

<a id="vnpay-demo-review"></a>
**3. VNPay cho demo — đối chiếu ưu tiên 14/11/2026**

Theo yêu cầu tìm hiểu thanh toán trước khi chia flow: đã đối chiếu D10, contract, source BE, source main FE và đúng bằng chứng sandbox cũ. Danh sách feature ở mục2 **chưa được nghiệm thu lại toàn bộ trong lượt này**. Bản [bốn flow15–20phút](reference/note-script-demo.md) đã đề xuất wording, vị trí AI, nhánh feature và luồng dữ liệu từ ba sơ đồ Owner gửi; chờ duyệt/rehearsal và rà schema tiếp tại FLOW-01. [Tài liệu chuẩn bị BE](reference/SCMS-SCRIPT-DEMO-BE.md) giữ phần tham chiếu, không tạo bộ demo trùng.

- Nên gọi **“Thanh toán qua cổng VNPay sandbox”**; QR là một phương thức gateway cung cấp, không phải QR nhận tiền cá nhân do SCMS tự sinh. `VnPayGateway.createPaymentLink` gửi số tiền hóa đơn, mã đơn và mô tả ngắn; chưa gửi đầy đủ chi tiết lớp/lịch để hứa app ngân hàng hiện mọi thông tin.
- Đề xuất demo bằng thẻ test NCB trên trang sandbox, có thể thao tác trong trình duyệt điện thoại. Không cần đổi tài khoản nhận thành tài khoản Owner. Chưa có căn cứ cho nhánh app ngân hàng thật xác nhận nhưng không trừ tiền; production/merchant thật cần phạm vi khác. Cơ chế, API→bảng/state và lựa chọn demo nằm tại [docs02](02-DEPLOYMENT-GUIDE.md#vnpay-demo-mechanism).
- Source `VnpayCheckoutHost.verifyInvoice` ở [FE main](https://github.com/hoanganh21022005/FE_Project/blob/main/src/components/common/VnpayCheckoutHost.jsx) đọc ownInvoice khoảng2,5giây/lần, tối đa10phút, dừng khi Paid hoặc trạng thái kết thúc. Đây là polling GET, không phải gọi QueryDR mỗi2,5giây và không kéo dài hạn5phút. Có thể cập nhật trang PC khi cùng hóa đơn được thanh toán trên thiết bị khác, nếu IPN đến BE và phiên PC còn hợp lệ. Chưa rehearsal hai thiết bị; chưa xác định SHA website đang phát hành.

**Bổ sung theo ý Owner: cần thêm lựa chọn Quét QR, không cần giải thích lại cách nhập thẻ sandbox.** Owner giao agent đề xuất hướng áp dụng được; đề xuất hiện hành là **Chuyển khoản QR — VietQR + SePay Webhooks**, ở phần dưới. Bảng này giải thích hai cách, không yêu cầu Owner tự chọn lại công nghệ; chưa coi là đã triển khai:

| Hướng | Tận dụng gì / cần thêm gì | Phạm vi dữ liệu và nghiệm thu |
|---|---|---|
| VNPAY-QR cạnh lựa chọn thẻ | Dùng lại hóa đơn và IPN/QueryDR hiện có; BE nhận lựa chọn được cho phép rồi thêm `vnp_BankCode=VNPAYQR` **trước khi ký**, thẻ nội địa có thể chọn `VNBANK`. Đây là phương thức trên cùng gateway theo [đặc tả VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html) | Có thể giữ `paymentMethod=VNPay`; tách lựa chọn giao diện không tự đòi giá trị DB mới. Nếu cần thống kê riêng QR/thẻ phải thiết kế lưu phương thức thực tế từ phản hồi đã xác minh. Chưa kiểm merchant sandbox hỗ trợ hoàn tất nhánh QR, nên chưa hứa demo QR success không tiền thật |
| Chuyển khoản VietQR vào tài khoản Owner | QR điền sẵn ngân hàng/tài khoản/số tiền/nội dung theo hóa đơn; cần kênh xác nhận giao dịch riêng để web tự đổi trạng thái. Tạo QR và nhận webhook là hai phần khác nhau; xem [tạo QR](https://www.vietqr.io/generate) và [ví dụ webhook ngân hàng của Casso](https://developer.casso.vn/webhook/gia-lap-giao-dich-den) | Luồng mới ngoài VNPay; giao dịch bằng app ngân hàng thật là chuyển tiền thật. Có thể mô phỏng riêng cho demo nhưng phải ghi rõ, không coi là đã trả trên app ngân hàng. `InvoiceService.newInvoice` hiện chỉ nhận Cash/VNPay dù CHECK trong schema có BankTransfer; chưa thể chỉ thêm ô FE và coi đã tích hợp |

**Đề xuất cho SCMS: giữ Cash/VNPay, bổ sung Chuyển khoản QR bằng VietQR + SePay Webhooks.** Lý do: có tài liệu chính thức cho cả QR→chuyển khoản→webhook→web cập nhật và Test Mode phát giao dịch mô phỏng; nhánh hoàn tất QR của merchant VNPay sandbox chưa có bằng chứng. Đây là đề xuất kỹ thuật từ tài liệu provider, chưa là nghiệm thu tích hợp SCMS hoặc quyền thực thi cloud.

“Tách QR khỏi thẻ” trước đó chỉ có nghĩa: thay một nút VNPay dẫn tới trang chọn phương thức bằng hai ô **Thẻ ngân hàng** và **VNPAY-QR**, vẫn cùng cổng VNPay. Nó không tự tạo thêm nguồn nhận tiền hoặc cách mô phỏng QR sandbox.

**Vì sao SePay và mức setup:** VietQR là mã chuyển khoản; SePay là phần nhận thông tin giao dịch và gửi webhook, hai phần bổ sung cho nhau. Đề xuất SePay dựa vào Test Mode đã được nhà cung cấp mô tả, không phải kết luận VNPay không làm QR được. Với SCMS hiện tại, dùng VNPAY-QR ít thay đổi code hơn vì đã có gateway; SePay tăng công việc BE/DB/FE để có một nhánh chuyển khoản QR riêng và chủ động mô phỏng giao dịch. Cấu hình dashboard gồm tài khoản test, webhook và xác thực tương đối gọn, nhưng Test Mode chỉ hiện khi công ty đã được bật và actor có vai trò Admin/Super Admin/Developer; chưa kiểm điều kiện này của nhóm. Tích hợp đúng hóa đơn/quyền và chống trùng là phần phát triển mới, không thể coi chỉ nhập env là xong hoặc đưa thời lượng chưa đo. [Điều kiện Test Mode](https://docs.sepay.vn/test-mode.html), [setup webhook](https://developer.sepay.vn/vi/sepay-webhooks/bat-dau-nhanh).

| Phần | Thiết kế đề xuất / giới hạn |
|---|---|
| Giao diện và luồng thật | Thêm ô **Chuyển khoản QR**; BE trả ngân hàng/tài khoản nhận cấu hình, đúng số tiền Invoice và mã thanh toán gắn duy nhất với Invoice. FE hiện QR; app ngân hàng điền sẵn thông tin; khi chuyển khoản thật, SePay nhận giao dịch và báo webhook để BE xác minh, FE GET đọc lại. [Luồng QR + webhook chính thức](https://developer.sepay.vn/vi/sepay-webhooks/tao-qr-va-form-thanh-toan) |
| Demo không tiền thật | Dùng **SePay Webhooks Test Mode**, tài khoản/webhook riêng, mô phỏng tiền vào đúng số tiền/mã đơn tại SePay để provider gửi webhook về SCMS. Đây là mô phỏng xác nhận giao dịch, không phải app ngân hàng thật thanh toán miễn phí. QR của tài khoản test chưa được kiểm với app ngân hàng thật, không hứa quét tới bước xác nhận được. [Test Mode](https://developer.sepay.vn/vi/sepay-webhooks/test-mode/bat-dau-nhanh), [mô phỏng giao dịch](https://docs.sepay.vn/gia-lap-giao-dich.html) |
| API/service/schema cần thêm | Mở luồng tạo BankTransfer có kiểm quyền/đích và lấy giá từ BE; schema đã cho giá trị này nhưng service hiện chưa nhận. Thêm webhook riêng, xác thực provider, đối chiếu môi trường/tài khoản/chiều tiền vào/mã đơn/số tiền. Thiết kế lưu giao dịch với FK→Invoice và unique theo provider+môi trường+transactionId; chưa chốt tên bảng/DDL. Không dùng ID giao dịch trần lẫn VNPay/SePay vì hai provider có thể trùng ID |
| Rule giữ lại và nghiệm thu | Dùng Clock VN/cutoff5phút theo SCMS, không chép15phút hoặc điều kiện thu dư từ ví dụ provider. Đúng amount và đủ điều kiện mới Paid+cấp quyền; muộn/mất quyền giữ→đối soát. Retry/race không nhân tiền/credit/thông báo. Kiểm response và DB sau commit cho success, sai amount/receiver/ref, giả webhook, gửi trùng, tới/quá hạn; test và live có bằng chứng riêng |

Thứ tự làm khi chuyển sang triển khai: (1) xác minh nhóm có quyền bật SePay Webhooks Test Mode và nhận được webhook có xác thực trên môi trường kiểm được phép; (2) hoàn thiện API/schema và kiểm local cùng PAY-01; (3) tích hợp FE, rehearsal QR/mô phỏng→Paid→quyền học→reload. Bản thật liên kết tài khoản nhận của Owner và chuyển tiền thật là phạm vi riêng. Chưa đăng ký/cấu hình SePay, chưa gửi giao dịch/webhook hay cài thư viện; không tích hợp mới endpoint `/v2/generate` đã bị VietQR.io đánh dấu ngừng hỗ trợ.

**PAY-01 — P1 trước rehearsal: trang kết quả có thể báo thành công khi chưa Paid**

| Nội dung | Kết quả đối chiếu / đề xuất |
|---|---|
| Nguồn/symbol | BE [InvoiceService.inspectVnpayReturn](../src/main/java/com/sportscenter/invoice/InvoiceService.java) trả code00 khi chữ ký hợp lệ và tìm thấy hóa đơn, không kiểm kết quả thanh toán tại nhánh đọc này. [InvoiceController.vnpayReturn](../src/main/java/com/sportscenter/invoice/InvoiceController.java) đưa code đó vào `?status=00`. FE [PaymentResultView](https://github.com/hoanganh21022005/FE_Project/blob/main/src/components/views/PaymentResultView.jsx) dùng `isSuccess = status === '00'`, không đọc invoice trong component |
| Ví dụ cụ thể | Suy từ code: Return hợp lệ cho giao dịch hủy/lỗi vẫn có thể dẫn `status=00` và hiện “Thanh toán thành công”. Mở trực tiếp trang kết quả với query này cũng hiện thông báo xanh; đây không phải quan sát một giao dịch live mới |
| Phân loại/ảnh hưởng | Lệch ý nghĩa field giữa API/FE và thông báo gây hiểu nhầm; chưa có căn cứ kết luận lỗi schema. BE vẫn kiểm IPN/QueryDR riêng để ghi Paid/cấp quyền, nên thông báo xanh không chứng minh bypass thanh toán |
| Hướng xử lý | FE đọc ownInvoice bằng phiên chủ rồi hiển thị `paymentStatus`/`reconciliationRequired`; lúc chưa biết thì “Đang kiểm tra thanh toán”. Giữ Return không ghi Paid; thống nhất tên/ý nghĩa mã điều hướng, không gọi nó là mã kết quả VNPay nếu chỉ là mã kiểm tra Return |
| Cách nghiệm thu | Với đúng owner/phiên: thành công trong hạn → Paid+quyền hợp lệ, tải lại vẫn giữ; Pending/hủy/lỗi và URL tự sửa không báo đã trả; Paid+cần đối soát hiển thị cần xử lý, không báo đã cấp quyền. Sai owner/center không lộ hóa đơn. Callback/verify retry không cấp thêm quyền. Kiểm riêng popup và fallback cùng tab/trình duyệt điện thoại |

Thứ tự đề xuất riêng VNPay: (1) xử lý PAY-01 và xác nhận website đúng bản FE; (2) rehearsal sandbox + đọc lại hóa đơn/quyền trong hạn; (3) kiểm hai thiết bị nếu giữ yêu cầu PC tự cập nhật. Nhánh QR bổ sung theo đề xuất SePay ở trên. QueryDR live còn thiếu bằng chứng, không suy đã hoạt động từ ca IPN/sandbox cũ. Chưa có thay đổi BE/FE/schema, query Supabase, reset hay deploy trong lượt review này. Bằng chứng và phạm vi từng môi trường ở Active/QA.
