# SCMS — YÊU CẦU NGHIỆP VỤ HOÀN TIỀN THỦ CÔNG CHO LỚP NORMAL

**Dành cho:** Nhóm Backend (BE) — SCMS / VIBE SPORT  
**Ngày:** 09/11/2026  
**Mục đích:** Làm rõ **khi nào cần hoàn tiền, ai khởi tạo, ai phê duyệt, ai chuyển khoản, ai xác minh**, những dữ liệu cần ghi nhận và điều kiện để được xem là hoàn tất.  
**Phạm vi:** Hoàn học phí **Normal** khi **trung tâm không thể tổ chức lớp trước buổi học đầu tiên**, bao gồm trường hợp Member không đồng ý phương án dời/chuyển lớp.  
**Ngoài phạm vi:** Chỉnh sửa toàn bộ vòng đời Normal; cơ chế lớp đủ sĩ số; Advanced; Member tự ý hủy khi trung tâm vẫn cung cấp dịch vụ đúng cam kết; **hoàn một phần sau khi khóa đã bắt đầu** (vì Normal `Ongoing` phải ưu tiên dời buổi/thay Coach/học bù, không hủy theo quy trình thông thường); tự động hoàn qua ngân hàng/VNPay. Nếu không thể tiếp tục cung cấp dịch vụ do sự cố bất khả kháng, phải báo Owner xử lý quyền lợi riêng, không được bỏ qua nghĩa vụ.

> **Lưu ý cho BE:** Đây là tài liệu **yêu cầu nghiệp vụ**, **không phải chỉ dẫn sửa code**. Nhóm BE **tự rà soát logic, mã nguồn, dữ liệu, API, phân quyền và khả năng tái sử dụng chức năng hiện có**; tự đề xuất thiết kế kỹ thuật và triển khai tương thích. Nếu có nghiệp vụ không khả thi hoặc còn mơ hồ, trao đổi với Owner trước khi chốt cách làm. Không được tự thay đổi chính sách hoàn tiền trong tài liệu để phù hợp với code hiện có.

---

## 1. Những nguyên tắc bắt buộc

1. **Chỉ hoàn tiền đã thực nhận.** Hóa đơn chưa thanh toán (`Pending`, `Expired`, `Cancelled`...), nếu xác nhận chưa có tiền vào hệ thống/tài khoản trung tâm, **không phát sinh chuyển tiền hoàn**. Trường hợp trạng thái hóa đơn và dòng tiền không khớp phải được đối soát trước.
2. **Khi trung tâm không cung cấp được lớp Normal trước khai giảng**, Member được hoàn **100% số tiền học phí thực tế đã thanh toán cho khóa chưa được cung cấp**, nếu Member không đồng ý phương án thay thế. Không tự trừ phí quản lý, phí hủy, phí chuyển khoản hoặc buộc đổi sang voucher/credit.
3. **Dời lịch hoặc chuyển lớp phải có sự đồng ý rõ ràng của Member.** Không phản hồi **không đồng nghĩa với đồng ý**; không tự chuyển lớp hoặc đóng nghĩa vụ hoàn tiền khi chưa có phản hồi.
4. **Trung tâm hủy lớp hoàn toàn:** việc hoàn tiền là **nghĩa vụ chủ động của trung tâm**; Member **không cần tự bấm “Yêu cầu hoàn tiền”** mới có quyền nhận tiền.
5. **Dời/chuyển lớp nhưng Member từ chối:** Member có thể chọn **“Yêu cầu hoàn tiền”**; Manager xử lý yêu cầu. Không được cố tình từ chối hoàn chỉ vì Member không muốn lớp thay thế.
6. **Phân tách trách nhiệm:** Center Manager quyết định/phê duyệt khoản hoàn; Receptionist chỉ thực hiện chuyển khoản ngoài SCMS sau phê duyệt và nộp chứng từ; Center Manager đối soát độc lập rồi mới xác nhận hoàn tất.
7. **Phê duyệt ≠ đã chuyển khoản; tải chứng từ ≠ đã xác minh; xác minh xong mới Completed.** Không đánh dấu hoàn tất chỉ dựa vào ảnh chụp màn hình.
8. **Không hoàn trùng, không hoàn vượt số tiền đủ điều kiện;** không làm mất lịch sử hóa đơn, ghi danh và thao tác.
9. **Không chuyển tiền tự động qua SCMS ở giai đoạn này.** Việc hoàn là **chuyển khoản ngân hàng thủ công** do Lễ tân thực hiện; SCMS là nơi quản lý hồ sơ, công việc và bằng chứng.
10. **Bảo vệ Member:** công khai trạng thái xử lý, số tiền và lý do; không tự kết luận Member từ bỏ quyền hoàn tiền vì chưa phản hồi/chưa cung cấp tài khoản nhận.

---

## 2. Ai khởi tạo hồ sơ hoàn tiền? — Hai tình huống phải tách rõ

### Tình huống A — Trung tâm đề nghị dời lịch/chuyển lớp, Member không đồng ý

1. Manager quyết định lớp Normal hiện tại chưa thể tổ chức theo kế hoạch và **đưa ra phương án thay thế** (dời lịch hoặc chuyển lớp phù hợp).
2. SCMS thông báo cho Member, nêu rõ phương án, thời gian/địa điểm/Coach nếu thay đổi và **lựa chọn**: **(a) đồng ý** hoặc **(b) yêu cầu hoàn tiền**.
3. **Member chủ động chọn hoàn tiền** nếu không chấp nhận phương án thay thế. Yêu cầu phải liên kết đúng Member, lớp và hóa đơn đã thanh toán.
4. Nếu Member đồng ý phương án mới, không phát sinh hoàn tiền cho cùng quyền học; hệ thống lưu lịch sử đồng ý.
5. Nếu Member yêu cầu hoàn, hồ sơ chuyển sang **chờ Manager kiểm tra/phê duyệt**. Manager kiểm tra tính hợp lệ và số tiền; sau khi duyệt mới giao Lễ tân chuyển khoản.
6. Nếu Member chưa phản hồi, ghi nhận **chờ phản hồi**, tiếp tục thông báo/nhắc lại theo chính sách trung tâm. **Không tự xem là đã đồng ý hoặc từ bỏ quyền lợi.**

**Quan trọng:** Yêu cầu hoàn trong tình huống A là quyền lựa chọn của Member vì **trung tâm thay đổi/không thực hiện được cam kết ban đầu**, không phải trường hợp Member tùy ý hủy một lớp vẫn vận hành bình thường.

### Tình huống B — Trung tâm quyết định hủy lớp hoàn toàn

1. Manager ra quyết định hủy lớp vì trung tâm không thể tổ chức.
2. SCMS **chủ động lập hồ sơ phải hoàn tiền** cho **từng Member có khoản thanh toán đủ điều kiện**, gắn với đúng hóa đơn/lần thu tiền. **Không yêu cầu Member tự mở đơn**.
3. SCMS thông báo cho Member lý do hủy, khoản tiền dự kiến hoàn, và yêu cầu Member **cung cấp hoặc xác nhận thông tin tài khoản nhận tiền**.
4. Manager kiểm tra, xác nhận nghĩa vụ hoàn và phê duyệt số tiền hợp lệ; chuyển công việc cho Lễ tân.
5. Nếu Member chưa cung cấp thông tin nhận tiền, hồ sơ ở trạng thái **chờ bổ sung**, trung tâm tiếp tục có nghĩa vụ xử lý. **Không tự đóng hồ sơ hay coi là hoàn tất.**
6. Nếu Member đã có một hồ sơ hoàn phù hợp từ trước, **không tạo thêm hồ sơ trùng**; tiếp tục xử lý hồ sơ hiện hữu.

**Điểm phân biệt:** Ở tình huống B, **quyết định hủy của trung tâm kích hoạt nghĩa vụ hoàn**; việc Member cung cấp tài khoản chỉ là bước để thực hiện thanh toán, không phải điều kiện phát sinh quyền hoàn tiền.

### Tình huống C — Member tự ý muốn nghỉ khi lớp vẫn tổ chức đúng cam kết

**Không nằm trong phạm vi tài liệu này.** Không tự áp dụng chính sách hoàn 100% ở mục 1. Cần quy định riêng cho hủy chủ động, thời hạn, chi phí hợp lý và điều khoản đã công bố trước thanh toán.

---

## 3. Phân quyền và trách nhiệm của từng bên

| Vai trò | Được làm | Không được làm |
|---|---|---|
| **Member** | Xem thông báo; đồng ý/từ chối phương án thay thế; gửi yêu cầu hoàn ở tình huống A; xác nhận tài khoản nhận; xem trạng thái hoàn và số tiền | Không tự duyệt khoản hoàn; không tự đánh dấu đã nhận tiền/Completed; không xem hồ sơ của người khác |
| **Center Manager** | Đưa ra quyết định hủy/dời/chuyển; xem hồ sơ liên quan trung tâm; kiểm tra số tiền và điều kiện; phê duyệt; yêu cầu bổ sung/sửa thông tin; đối chiếu giao dịch ngân hàng; xác nhận hoàn tất | Không coi hồ sơ Approved là đã hoàn; không xác nhận Completed khi chưa có bằng chứng đối soát đáng tin cậy |
| **Receptionist (Lễ tân)** | Xem công việc **đã được duyệt và đủ thông tin**; chuyển khoản **ngoài SCMS**; nhập dữ liệu giao dịch; tải chứng từ; báo chuyển thất bại/gửi sai hoặc cần sửa | Không tự quyết định người nào đủ điều kiện; không phê duyệt số tiền; không tự xác nhận Completed; không tự thay số tiền/đích nhận đã được duyệt |
| **SCMS** | Lập/nhận hồ sơ đúng tình huống; lưu trạng thái; kiểm soát hoàn trùng; nhắc bổ sung thông tin; thông báo; lưu lịch sử và chứng từ; phản ánh khoản hoàn vào báo cáo | Không tự chuyển khoản; không suy đoán rằng tiền đã đến Member chỉ từ chứng từ ảnh |

**Trường hợp Member thông báo ngoài hệ thống** (tại quầy/điện thoại): Manager hoặc Lễ tân có thể **ghi nhận thông tin tiếp nhận thay Member** nếu có chức năng hỗ trợ, nhưng phải lưu **nguồn tiếp nhận và bằng chứng xác nhận lựa chọn của Member**; việc này **không cấp quyền phê duyệt cho Lễ tân**.

---

## 4. Quy trình hoàn tiền bắt buộc sau khi phát sinh hồ sơ

### Bước 1 — Ghi nhận hồ sơ

Hồ sơ phải xác định: nguyên nhân, nguồn khởi tạo (Member yêu cầu / hệ thống phát sinh do trung tâm hủy), Member, lớp Normal, hóa đơn gốc, số tiền đã thu và số tiền đề nghị hoàn. Hồ sơ **chưa đồng nghĩa được phép chuyển khoản**.

### Bước 2 — Xác nhận người nhận tiền

Member cung cấp hoặc xác nhận tối thiểu **ngân hàng, tên chủ tài khoản, số tài khoản nhận**. SCMS cho Member kiểm tra thông tin trước khi xác nhận; nhân viên chỉ thấy dữ liệu cần thiết theo phân quyền. Nếu thiếu hoặc có dấu hiệu sai, **không chuyển tiền**.

- Nếu tài khoản nhận tiền **khác người đã thanh toán/Member**, hoặc yêu cầu đổi tài khoản bất thường: phải có bước xác minh rõ ràng trước khi duyệt, tránh chuyển tiền nhầm/gian lận.
- Nếu **thông tin tài khoản, số tiền hoặc người nhận thay đổi sau khi duyệt**, yêu cầu **Manager xác nhận lại** trước khi Receptionist chuyển khoản.
- Không yêu cầu Member cung cấp OTP, mật khẩu ngân hàng hoặc thông tin đăng nhập ngân hàng.

**Owner chốt10/11 — dữ liệu liên hệ hỗ trợ:** BE chuẩn bị số điện thoại liên hệ từ hồ sơ Member hiện có trong chi tiết hồ sơ hoàn đúng quyền; FE quyết định có hiển thị/dùng chức năng liên hệ này hay không. Khi khác tên người nhận hoặc có thay đổi bất thường, Manager có thể liên hệ trước duyệt và ghi kết quả xác minh trong lịch sử hồ sơ. Không bắt tích hợp gọi điện/OTP mới, không yêu cầu đưa điện thoại vào chứng từ ngân hàng; cuộc gọi không thay bằng chứng đối soát chuyển tiền. Yêu cầu xác minh người nhận và duyệt lại khi đổi thông tin vẫn được BE bảo đảm, độc lập với bố trí UI.

### Bước 3 — Manager phê duyệt

Manager kiểm tra khoản thu đã xác nhận, lý do hoàn, quyền học đã/ chưa cung cấp, số tiền đủ điều kiện, hồ sơ hoàn trước đó và người nhận đã xác minh. Manager có thể **yêu cầu bổ sung** hoặc từ chối **hồ sơ sai/không hợp lệ** với lý do rõ ràng; **không được từ chối tùy tiện nghĩa vụ hoàn 100% của trung tâm** trong phạm vi mục 1.

Phê duyệt phải lưu người duyệt, thời điểm, số tiền và thông tin người nhận đã được duyệt. **Chỉ hồ sơ Approved, đủ thông tin mới xuất hiện trong hàng đợi chuyển khoản của Receptionist.**

### Bước 4 — Receptionist chuyển khoản ngoài SCMS và nộp chứng từ

Receptionist dùng quy trình ngân hàng của trung tâm để chuyển **đúng số tiền** tới **đúng tài khoản đã được duyệt**, sau đó cập nhật hồ sơ SCMS với:

- Số tiền chuyển và loại tiền (VND).
- Ngân hàng chuyển/nhận (trong phạm vi cần thiết), tên người nhận và thông tin tài khoản nhận đã xác minh (chỉ hiển thị hạn chế).
- **Mã tham chiếu/mã giao dịch ngân hàng**, thời điểm thực hiện, người thao tác.
- **Chứng từ chuyển khoản** (file ảnh hoặc PDF) có thể đọc rõ số tiền, thời điểm và mã giao dịch.
- Ghi chú nếu có sự cố/chuyển thất bại/sai người nhận/đang chờ ngân hàng xử lý.

**Sau bước này chỉ là “đã nộp chứng từ/chờ xác minh”, chưa phải Completed.** Không được tạo một lần chuyển khoản mới chỉ vì lần nhập dữ liệu cũ bị lỗi hoặc trang tải lại; phải kiểm tra kết quả ngân hàng trước.

### Bước 5 — Manager đối soát và xác nhận hoàn tất

Manager đối chiếu **độc lập** với nguồn thông tin ngân hàng đáng tin cậy (ví dụ lịch sử giao dịch ngân hàng của trung tâm/sao kê đã được xác thực), tối thiểu kiểm tra:

- Khoản chuyển thực sự được ngân hàng ghi nhận **thành công**, không phải giao dịch đang chờ hoặc đã bị hoàn ngược.
- Mã giao dịch, số tiền, thời điểm, tài khoản nhận và người nhận **khớp với hồ sơ**.
- Không có khoản hoàn khác đã được ghi nhận cho cùng nghĩa vụ.

**Chỉ sau khi kiểm tra đạt**, Manager mới xác nhận **`Completed`**. Nếu không khớp: yêu cầu Lễ tân bổ sung/điều chỉnh hoặc ghi nhận thất bại/sự cố; **không Completed**.

### Bước 6 — Thông báo và lưu lịch sử

Khi hoàn tất, SCMS cập nhật hồ sơ, thông báo Member đã xử lý hoàn tiền với số tiền/ngày/mã tham chiếu phù hợp; Member có thể liên hệ nếu chưa nhận được. SCMS giữ lịch sử đầy đủ để đối soát và giải quyết khiếu nại.

---

## 5. Trạng thái nghiệp vụ cần thể hiện rõ

Tên trường/enum và số lượng trạng thái cụ thể **để BE lựa chọn**, nhưng hệ thống phải **phân biệt được tối thiểu** các giai đoạn sau:

| Giai đoạn | Ý nghĩa nghiệp vụ | Ai có thể thúc đẩy bước tiếp theo? |
|---|---|---|
| Chờ Member phản hồi | Đã đề xuất dời/chuyển nhưng Member chưa lựa chọn | Member / trung tâm gửi nhắc |
| Chờ thông tin nhận tiền | Đã có nghĩa vụ/yêu cầu hoàn, nhưng chưa có tài khoản nhận hợp lệ | Member bổ sung; nhân viên hỗ trợ |
| Chờ phê duyệt | Hồ sơ đủ điều kiện trình Manager | Manager |
| Đã phê duyệt | Số tiền và đích nhận đã được duyệt, chưa xác nhận đã chuyển | Receptionist |
| Đang xử lý chuyển tiền | Lễ tân xử lý ngoài ngân hàng | Receptionist |
| Chờ xác minh chứng từ | Đã nhập giao dịch và nộp chứng từ; Manager chưa đối soát | Manager |
| Hoàn tất | Đã có đối soát thành công và xác nhận cuối | Trạng thái kết thúc |
| Cần bổ sung / sai thông tin / chuyển thất bại | Có sự cố, cần khắc phục; chưa được xem là hoàn | Người phụ trách theo loại lỗi |
| Không đủ điều kiện / từ chối có lý do | Hồ sơ không thuộc quyền hoàn như đề nghị hoặc dữ liệu không hợp lệ | Manager; có lưu lý do và kênh phản hồi |

**Quy tắc:** `Approved` hoặc `AwaitingVerification` không được hiển thị cho Member như “Đã hoàn tiền”. Nếu xảy ra sự cố ở bất cứ bước nào, hồ sơ phải được giữ lại để tiếp tục xử lý, không biến mất khỏi danh sách công việc.

---

## 6. Dữ liệu và chứng từ tối thiểu phải lưu trong SCMS

### 6.1. Hồ sơ hoàn tiền

- Mã hồ sơ hoàn tiền duy nhất; nguồn khởi tạo và lý do.
- Liên kết **Member — lớp Normal — Enrollment (ghi danh) — Invoice (hóa đơn gốc)** và khoản thu liên quan.
- Số tiền học phí đã thực nhận, số tiền đủ điều kiện hoàn, số tiền đã hoàn trước đó (nếu có), số tiền được duyệt, số tiền đã chuyển/xác minh.
- Phương thức thanh toán ban đầu (Cash/VNPay nếu có), **phương thức hoàn giai đoạn này: chuyển khoản ngân hàng thủ công**.
- Thông tin tài khoản nhận tiền **được xác minh**, người duyệt và người xác minh; các mốc thời gian và trạng thái.
- Ghi chú lý do từ chối, cần bổ sung, sự cố chuyển tiền và các lần xử lý lại.

### 6.2. Chứng từ chuyển khoản

- Chấp nhận **ảnh rõ nét hoặc PDF** của giao dịch; có ràng buộc định dạng/dung lượng phù hợp.
- Lưu liên kết chứng từ với **đúng hồ sơ hoàn tiền và đúng lần thao tác chuyển khoản**.
- Lưu người tải lên, thời gian, mã giao dịch được ghi nhận và thông tin truy xuất chứng từ.
- Chỉ người có quyền được xem/tải chứng từ; không để đường dẫn công khai, không đưa dữ liệu ngân hàng nhạy cảm vào thông báo hoặc log thông thường.
- **Không tùy tiện xóa/ghi đè chứng từ đã dùng xác minh**; khi điều chỉnh, giữ lịch sử và lý do thay đổi.
- Hệ thống cần giữ **dấu vết Manager đã đối chiếu với nguồn ngân hàng nào, lúc nào và kết quả ra sao**; chỉ ảnh do Lễ tân tải lên là **chưa đủ**.

### 6.3. Lịch sử thao tác (Audit Log)

Cần truy được: **ai** tạo/ghi nhận yêu cầu, Member đã chọn gì, Manager duyệt bao nhiêu và khi nào, Lễ tân thao tác gì, file nào được nộp, ai đối soát, các thay đổi số tiền/người nhận, lỗi và trạng thái cuối. Người không có quyền không được đọc hồ sơ tài chính/chứng từ của trung tâm khác.

---

## 7. Quy tắc tiền và dữ liệu tránh gây thiệt hại

1. **Đầy đủ số tiền:** Trung tâm hủy trước khai giảng, chưa cung cấp buổi học nào và Member không nhận phương án thay thế → hoàn **100% khoản học phí Member thực trả cho lớp đó**.
2. **Không hoàn hai lần:** Mỗi khoản đã thu phải được kiểm soát số đã hoàn, đang chờ hoàn và giao dịch đã thực hiện; gửi lại request, tải lại trang hoặc xử lý đồng thời không được tạo hai lệnh chuyển tiền.
3. **Không vượt tiền gốc:** Tổng số tiền hoàn đã xác minh đối với khoản thu không được vượt số tiền đủ điều kiện, trừ khi có quyết định xử lý đặc biệt được phê duyệt ngoài phạm vi này.
4. **Giữ nguyên hóa đơn gốc:** Không xóa hóa đơn `Paid` hay sửa thành chưa thanh toán để “xóa doanh thu”; khoản hoàn phải thể hiện bằng hồ sơ/lịch sử hoàn riêng, có liên kết hóa đơn gốc.
5. **Báo cáo tài chính rõ ràng:** Phân biệt **số đã thu**, **số hoàn đã xác minh**, **doanh thu/số thu thuần**; không trừ khoản mới `Pending/Approved` như đã xuất tiền, không trừ một khoản hai lần. Có thể xem tháng thu và tháng hoàn khác nhau.
6. **Quyền học nhất quán:** Sau khi lớp bị hủy và hồ sơ hoàn xử lý, không để Member vừa được hoàn đầy đủ vừa tiếp tục có quyền sử dụng khóa cũ. Đồng thời không xóa dữ liệu học và lịch sử thanh toán phục vụ đối soát.
7. **Thanh toán đến muộn hoặc sai lệch:** Nếu ngân hàng/VNPay báo đã thu tiền sau khi trạng thái hóa đơn/đăng ký thay đổi, **phải được đối soát và xử lý quyền lợi/tiền đã nhận**; không bỏ qua vì trạng thái ứng dụng đang `Expired` hoặc `Cancelled`.
8. **Chuyển khoản bị hoàn ngược/sai người nhận:** Không được báo đã hoàn cho Member nếu chưa xác minh người nhận hợp lệ; phải ghi nhận sự cố, xử lý trách nhiệm và cập nhật trạng thái, tránh chuyển tiếp lần nữa thiếu kiểm soát.
9. **Chưa cung cấp số tài khoản:** Hồ sơ tiếp tục chờ xử lý và có lịch sử nhắc thông tin; không đặt hạn mặc định khiến khoản phải hoàn tự biến mất.
10. **Thời hạn hoàn — ĐÃ CHỐT:** Sau khi **Manager phê duyệt** và Member đã cung cấp **thông tin tài khoản nhận hợp lệ**, trung tâm đặt mục tiêu **thực hiện chuyển khoản trong tối đa 07 ngày làm việc**. Nếu thiếu thông tin nhận tiền hoặc phát sinh sự cố đã ghi nhận, phải thông báo tiến độ và lý do; không tự đóng nghĩa vụ. **Thời gian ngân hàng ghi có vào tài khoản Member có thể khác** và chỉ được đánh dấu `Completed` sau khi đối soát thành công.

**Định nghĩa ngày làm việc — Owner chốt10/11:** ngày trung tâm mở theo lịch nghỉ đã cấu hình; tính cả thứ Bảy/Chủ nhật nếu trung tâm không đóng. Tái sử dụng ngày nghỉ của trung tâm, không dùng lịch nghỉ cá nhân Coach hoặc mặc định T2–T6/lịch ngân hàng. Giữ mốc07ngày làm việc, không đổi thành07ngày lịch. Thời điểm đủ điều kiện là đã được duyệt và có thông tin nhận hợp lệ; ngày trung tâm mở tiếp theo là ngày1, hết ngày7 theo Asia/Ho_Chi_Minh là hạn thực hiện chuyển khoản. Quá hạn chỉ cảnh báo và theo dõi lý do, không tự hoàn tất/xóa nghĩa vụ; thay đổi hồ sơ không được làm mất lịch sử mốc hạn cũ.

---

## 8. Yêu cầu hiển thị/thông báo theo vai trò

### Với Member

- Khi đề nghị dời/chuyển lớp: thấy **phương án cụ thể**, có thể đồng ý hoặc chọn yêu cầu hoàn tiền.
- Khi trung tâm hủy lớp: được thông báo **tự động có hồ sơ cần hoàn**, không phải tự nộp đơn mới.
- Có thể cung cấp/xác nhận tài khoản nhận và biết khi nào cần bổ sung thông tin.
- Theo dõi **số tiền**, lý do, trạng thái trung thực, thời điểm hoàn dự kiến nếu đã được thông báo và mã tham chiếu sau khi hoàn tất.
- Nếu phản ánh chưa nhận tiền, có kênh liên hệ/tiếp nhận khiếu nại và tiếp tục đối soát, không kết luận chỉ dựa trên ảnh chứng từ.

### Với Center Manager

- Thấy rõ hồ sơ **Member tự yêu cầu** và hồ sơ **hệ thống tạo do trung tâm hủy**.
- Thấy đầy đủ thông tin cần kiểm tra để phê duyệt hoặc yêu cầu bổ sung; có cảnh báo khi số tiền/tài khoản thay đổi.
- Có danh sách **đang chờ đối soát**, không thể xác nhận Complete chỉ bằng nút “đã upload”.
- Thấy các hồ sơ quá hạn xử lý hoặc lỗi và lịch sử thao tác.

### Với Receptionist

- Chỉ có danh sách **đã được Manager phê duyệt, đủ thông tin để chuyển khoản**.
- Thấy rõ khoản tiền, người nhận, số tài khoản đã xác nhận (đúng phạm vi cần thiết để thực hiện), mã hồ sơ và hướng dẫn nộp chứng từ.
- Sau khi chuyển khoản, nhập giao dịch và tải chứng từ, hồ sơ **chuyển chờ Manager xác minh**; Lễ tân không được tự kết thúc.
- Có cách báo sự cố, cập nhật chứng từ có kiểm soát mà không xóa lịch sử.

---

## 9. Các tình huống nghiệm thu bắt buộc (BE tự thiết kế test kỹ thuật)

| # | Tình huống | Kết quả nghiệp vụ bắt buộc |
|---|---|---|
| 01 | Trung tâm dời lịch, Member đồng ý | Không tạo lệnh hoàn; quyền học theo thỏa thuận mới |
| 02 | Trung tâm dời lịch, Member không đồng ý | Member gửi yêu cầu hoàn → chờ Manager duyệt |
| 03 | Trung tâm đề xuất chuyển lớp, Member từ chối | Member được chọn hoàn 100% nếu lớp gốc không thể cung cấp như cam kết |
| 04 | Trung tâm hủy toàn bộ lớp có 3 Member đã thanh toán | Tạo/ghi nhận nghĩa vụ hoàn cho từng khoản hợp lệ; không bắt ba người tự gửi đơn |
| 05 | Member không phản hồi khi đề xuất dời lịch | Không tự chấp thuận, không tự tước quyền hoàn; có trạng thái chờ và nhắc |
| 06 | Member chưa cung cấp tài khoản nhận | Không chuyển khoản, không tự Completed; vẫn theo dõi nghĩa vụ hoàn |
| 07 | Invoice Pending chưa thực thu | Không hoàn tiền; xử lý hóa đơn/quyền giữ chỗ theo nghiệp vụ hiện có |
| 08 | Invoice Paid 900.000đ, lớp hủy trước khai giảng | Số tiền phải hoàn 900.000đ khi không có dịch vụ đã cung cấp |
| 09 | Receptionist thử hoàn khi Manager chưa duyệt | Không được phép |
| 10 | Receptionist thử thay người nhận/số tiền đã duyệt | Không tự thay; Manager cần xác nhận lại |
| 11 | Receptionist upload ảnh chuyển khoản | Chỉ chuyển chờ xác minh, chưa Completed |
| 12 | Ảnh chứng từ hợp lệ nhưng Manager chưa đối chiếu ngân hàng | Chưa Completed |
| 13 | Manager xác minh giao dịch ngân hàng thành công, khớp khoản hoàn | Completed, lưu người xác minh, cập nhật báo cáo và thông báo Member |
| 14 | Chuyển khoản thất bại/treo/hoàn ngược | Không Completed; giữ hồ sơ để xử lý sự cố |
| 15 | Gửi yêu cầu hoàn trùng hoặc xử lý đồng thời | Không phát sinh hai khoản hoàn cho cùng nghĩa vụ |
| 16 | Đã hoàn đủ tiền và người dùng thao tác lại | Không hoàn lần hai, vẫn xem được hồ sơ lịch sử |
| 17 | Người không có quyền/khác trung tâm truy cập chứng từ | Bị từ chối; thông tin ngân hàng không lộ |
| 18 | Một hóa đơn Paid tháng 10, hoàn thành tháng 11 | Báo cáo phản ánh đúng kỳ thu và kỳ hoàn, không trừ hai lần |
| 19 | VNPay báo thanh toán thành công muộn so với thay đổi trạng thái | Không bỏ sót tiền thực nhận; đối soát trước khi kết luận khoản hoàn |
| 20 | Member khiếu nại chưa nhận tiền dù trạng thái Completed | Có khả năng truy ngược chứng từ, giao dịch và thông tin đối soát để xử lý |

---

## 10. Phạm vi giao cho BE và những điểm cần chốt nếu phát sinh

### BE được chủ động quyết định về kỹ thuật

- Tái sử dụng hoặc mở rộng module hoàn tiền/đối soát hiện hữu hay thiết kế thành phần bổ sung.
- Mô hình dữ liệu, trạng thái kỹ thuật, tên API, phân quyền thực tế, nơi lưu chứng từ và cách gửi thông báo.
- Cơ chế bảo đảm dữ liệu nhất quán, chống xử lý trùng, đối soát, cập nhật báo cáo và kiểm thử.
- Thứ tự triển khai và phương án chuyển đổi dữ liệu nếu cần, **miễn không phá chức năng đang hoạt động**.

### Phải hỏi Owner nếu chưa thống nhất

- Khách hàng **tự hủy** lớp đang tổ chức bình thường; phí hủy và chính sách hoàn (ngoài phạm vi).
- Lớp **đã bắt đầu** hoặc đã cung cấp một phần dịch vụ; cách tính hoàn một phần (ngoài phạm vi).
- Kênh xác minh Member/tài khoản ngân hàng và quy trình sự cố. **Mốc thực hiện chuyển khoản tối đa 07 ngày làm việc sau khi duyệt và có đủ tài khoản đã được Owner chốt.**
- Trường hợp đặc biệt không thể hoàn bằng chuyển khoản thông thường.
- Điều chỉnh chính sách hoàn 100% khi lỗi thuộc về trung tâm.

**Không tự mở rộng sang Advanced hoặc thay đổi vòng đời lớp Normal trong nhiệm vụ hoàn tiền này.** Normal **đã `Ongoing` không được hủy qua luồng thông thường**; sự cố phải ưu tiên dời buổi/thay Coach/học bù theo tài liệu vòng đời. Ngoại lệ không thể cung cấp dịch vụ sau khai giảng phải báo Owner để chốt cách bảo vệ quyền lợi, **không tự áp dụng quy tắc hoàn 100% trước khai giảng**. Khi tích hợp, BE kiểm tra tính tương thích và báo rõ phụ thuộc. Không thực hiện chuyển khoản thật hoặc thao tác tài chính trên production khi kiểm thử.

---

## 11. Tiêu chí bàn giao cuối cùng

Một chức năng hoàn tiền Normal được xem là đáp ứng yêu cầu khi:

- [ ] Phân biệt **Member yêu cầu hoàn sau khi từ chối phương án thay thế** và **hệ thống tạo nghĩa vụ hoàn do trung tâm hủy lớp**.
- [ ] Lễ tân **chỉ** nhận khoản đã được Manager duyệt; không có quyền tự duyệt hoặc tự xác nhận hoàn tất.
- [ ] Member xác nhận tài khoản nhận; số tiền/tài khoản thay đổi thì phải được xác minh, duyệt lại.
- [ ] Chuyển khoản thủ công bên ngoài hệ thống; nhập đúng thông tin và tải chứng từ riêng tư.
- [ ] Manager xác minh với dữ liệu ngân hàng thực tế **trước khi** `Completed`.
- [ ] SCMS lưu đầy đủ liên kết hóa đơn gốc, chứng từ, người xử lý, thời gian, trạng thái và Audit Log.
- [ ] Không hoàn trùng, không hoàn vượt, không hiển thị hoàn thành sai thực tế.
- [ ] Member được thông báo và theo dõi trạng thái; lỗi/chưa có thông tin nhận tiền không làm mất nghĩa vụ hoàn.
- [ ] Báo cáo tài chính phản ánh khoản hoàn đã xác minh, không làm mất hóa đơn `Paid` gốc.
- [ ] Các tình huống ở mục 9 được kiểm chứng trong môi trường thử nghiệm an toàn; BE báo kết quả và phần còn tồn tại.

**Thông điệp chốt:** **Member quyết định có nhận phương án dời/chuyển hay không; trung tâm tự chịu trách nhiệm hoàn khi hủy lớp. Manager phê duyệt → Lễ tân chuyển khoản và nộp chứng từ → Manager đối soát → SCMS mới ghi nhận hoàn tất.**
