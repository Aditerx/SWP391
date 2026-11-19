# SCMS — Quy tắc và cách demo

Cập nhật **19/11/2026**. File này giữ quy tắc demo; trạng thái/bằng chứng ở [Active/QA](00-MASTER-CHECKLIST.md#active), quyết định ở [D01–D19](04-DEBUG-NOTE.md). Owner đã mở quyền sửa và kiểm BE local, migration/generator và chuẩn bị hai bộ demo. [Kế hoạch nội bộ BE](update261118.md#ke-hoach-be) giữ thứ tự thực hiện; chỉ điền [bàn giao](../scms-ban-giao-2026-11-19.md) bằng kết quả thực tế sau khi BE đạt phạm vi đợt.

## 1. Điều đã rõ và phần đang đề xuất

Owner19/11 cho phép sửa/test BE và PostgreSQL local riêng, hoàn thiện nguồn sinh và hồ sơ demo; không sửa/test FE, gọi provider/email thật hoặc commit/push/deploy. Agent không thực thi SQL Supabase; Owner copy nguyên SQL đã kiểm để chạy. Hai anchor19/11 và21/11 đã chốt: hoàn chỉnh bản19 trước, bản21 giữ riêng và SQL execution NOT_RUN tới đúng ngày; không bỏ guard hoặc đổi Clock runtime. Thống kê có thể dùng dữ liệu mẫu, nhưng đường thao tác phải thể hiện đúng thanh toán cấp quyền, Accept trừ credit, hủy hoàn credit và hết hạn giữ chỗ.

**Đã chốt trong phản hồi tiếp17/11:** cấu hình demo tại BE; cho điểm danh trước ngày/giờ học theo đúng điều kiện Normal/Advanced bên dưới; buổi đã có điểm danh không hủy hoàn credit; có sẵn một buổi chưa điểm danh để demo hủy hoàn; hóa đơn30phút liên tục qua giờ nghỉ; mã điểm danh dùng đủ300giây từ lúc cấp. Không dùng việc đã chốt yêu cầu để suy code/FE/DB đã hỗ trợ. Các mốc48h/24h/12h hiện hành tiếp tục áp dụng; FE cần thống nhất hiển thị hạn, phạm vi cấu hình và dữ liệu thao tác, không hỏi lại giá trị chỉ vì đang giải thích.

**Owner18/11:** đồng ý BE trả điều kiện/hạn/lý do hủy để FE hiển thị; trung tâm cần hoạt động đủ7ngày trong tuần; thực hiện nhiều vòng schema/API/trình diễn có quan sát/FE thủ công theo mục7 sau khi thiết kế được chốt. Cơ chế credit đã được xác nhận tại mục5; hạn đúng là **45ngày sử dụng**.

**Tài liệu hiện hành:** bản đọc-chốt đã đóng vai trò, [nội dung cũ lưu tại archive](archive/2026-11/FE-đọc-để-chốt.md). [Bản phản hồi bảy điểm](../scms-phản-hồi-de-xuat-contract-fe-be-han-thao-tac-18-11.md) giữ phần trao đổi; kế hoạch BE ở ghi chú Owner, không giao BE sửa source FE. Công tắc, hủy/credit, đường thanh toán và quy trình rehearsal đã chốt; hai ngày chạy bundle19/11 và21/11 đã chốt; trạng thái kiểm riêng ở docs00. Chưa suy FE đã nhận hoặc tích hợp từ việc Owner đồng ý.

“Đính chính trạng thái quyết định” nghĩa là sửa câu đang khiến người đọc hiểu sai mức độ hoàn thành, không phải xóa quyết định hoặc lịch sử:

| Câu cần phân biệt | Cách ghi đúng |
|---|---|
| Bạn đã đồng ý mục tiêu demo mọi giờ | Yêu cầu đã rõ; còn phải chọn cơ chế và xử lý các tình huống liên quan |
| File V2 có ghi chú duyệt C1–C5/C11–C13, Cash/VNPay và QR300giây | Giữ nguyên nguồn ghi chú; đối chiếu phạm vi với trao đổi hiện tại trước khi thay Dxx. Không tự diễn giải thành toàn bộ thiết kế đã hoàn tất |
| Code đang dùng QR tối đa30giây | Quan sát implementation; không chứng minh300giây đã làm hoặc đã kiểm |
| QA ghi PASS kiểm tài liệu/local bundle | Chỉ đạt đúng phạm vi đó; không phải nghiệm thu nghiệp vụ mới hoặc website |

**Đính chính17/11:** cách gom ghi chú V2 ngày16/11 đã khiến Dxx hiện hành bị trình bày như chỉ còn là quy tắc cũ chờ thay. Bỏ cách diễn đạt đó. Dxx tiếp tục là căn cứ hiện hành; mỗi thay đổi cần nêu rõ nội dung/phạm vi được chốt và phần còn thiếu trước khi cập nhật. Giữ QA và bản nguồn cũ để truy lại; prompt16/11 **không dùng làm lệnh triển khai nguyên trạng**. `DEMO-RULE-02` chỉ là mã theo dõi việc, không phải quyết định nghiệp vụ hoặc quyền triển khai.

## 2. Có nên chạy lại schema ngay không?

**Đề xuất: đợi chốt và hoàn tất đợt sửa, tiếp tục dùng DB đang có nếu còn hoạt động.** Bundle sửa khóa không tự nới thời gian hoặc tạo cơ chế demo mọi giờ. Chạy lại nó lúc này sẽ xóa/dựng lại dữ liệu SCMS nhưng chưa giải quyết mục tiêu mới.

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Có cần reset tạm để dùng hệ thống? | Bundle local đã kiểm còn gắn ngày cài16/11, không cài trực tiếp ngày khác; chưa có readback DB mới trong lượt này | Chờ đợt sửa; chỉ chuẩn bị lại bộ cài nếu DB thực sự cần dựng lại | Giữ dữ liệu đang dùng, tránh reset hai lần | Sau sửa: code/schema tương thích; sinh và kiểm cả SQL/fixture/SYSTEM-DATA theo ngày cài thực; Owner chạy rồi kiểm dữ liệu/flow | Nếu DB hiện không dùng được, Bạn báo tình trạng và chọn riêng đợt dựng lại tạm |

Database **không hết dùng chỉ vì sang ngày khác**. Không sửa vài ngày trong SQL hoặc bỏ guard để chạy bản cũ. Khóa `NOWAIT` giúp bundle từ chối sớm khi DB bận, không làm reset hết cần khóa; quy trình chạy nằm ở [docs02](02-DEPLOYMENT-GUIDE.md#demo-reset-deadlock). Chưa có quyền chạy SQL/reset Supabase trong đợt tài liệu này.

## 3. Điểm danh: ai xác nhận điều gì?

Không dùng câu rút gọn “điểm danh sau xác nhận”. Hai luồng cần ghi đầy đủ như sau.

**Normal — luồng ghi ban đầu sớm đã triển khai/kiểm BE local:**

Manager tạo lớp và12buổi hợp lệ, **sĩ số tối đa1/tối thiểu1** → Member đăng ký → thanh toán được BE xác nhận `Paid`, ghi danh `Registered` → lớp đạt sĩ số tối thiểu → **Manager bấm xác nhận khai giảng thành công, BE lưu người và thời điểm xác nhận** → Coach được phân công chọn đúng buổi/Member → Coach lưu điểm danh trước ngày/giờ học → đọc lại kết quả đã lưu.

Sĩ số1 không cần nới công thức hiện có: `ceil(2×1/3)=1`. Nếu đặt tối đa15 nhưng tối thiểu1 thì không hợp lệ theo rule hiện hành. Đây là lớp demo chủ ý chỉ nhận một người, không đổi mọi lớp thành sĩ số1.

Công thức nghĩa là **cần ít nhất2/3 số chỗ của lớp, làm tròn lên, đã Paid/Registered** để khai giảng. Tối đa15 thì minimum thấp nhất là10, không phải1. Tối đa1 thì2/3≈0,67, làm tròn lên1. Một Member hợp lệ lúc đó lấp đủ lớp demo; muốn cho minimum1 với maximum15 là thay chính sách sĩ số, không phải sửa lỗi tính toán.

Đủ điều kiện mới cho điểm danh sớm: lớp đã được Manager xác nhận khai giảng; Member có quyền học hợp lệ của chính lớp; tài khoản và phân công còn hợp lệ; buổi `Scheduled`, chưa hủy/vô hiệu/chốt điểm danh. Vẫn kiểm role/permission/center. **Có Paid mà Manager chưa xác nhận: chặn. Manager đã xác nhận nhưng Member chưa Paid/Registered: cũng chặn.**

**Advanced — luồng ghi ban đầu sớm đã triển khai/kiểm BE local:**

Có sẵn một đăng ký `Requested` đúng Member/môn/Primary Coach, **chưa có lớp riêng/quyền học/hóa đơn** → Coach nhận đăng ký, hệ thống tạo lớp riêng → Member xác nhận mua trong24giờ từ lúc Coach nhận → tạo hóa đơn → thanh toán hợp lệ → quyền học `Active`,12credit và hạn45ngày sử dụng → hai người trao đổi lịch ngoài SCMS → Coach gửi đề xuất ngày/ca/sân → **đúng Member sở hữu bấm Accept, BE kiểm lại và tạo buổi `Scheduled`, credit12→11** → Coach được phân công lưu điểm danh trước ngày/giờ học → đọc lại kết quả. Thao tác Member gửi đăng ký có thể kiểm/rehearsal riêng, không bắt thêm vào đường trình diễn chính.

Coach nhận đăng ký **không đồng nghĩa** Member đã thanh toán hoặc đã chấp nhận lịch. Đề xuất `Pending` chưa tạo buổi/giữ tài nguyên/trừ credit, nên chưa thể điểm danh. Khi điểm danh vẫn kiểm đúng chủ quyền học, buổi, Coach, center, hạn và trạng thái hợp lệ; không trừ credit lần hai.

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Tạo buổi tương lai nhưng muốn điểm danh ngay trong demo | `SessionAttendancePolicy.requireWindow` dùng `[start,end)` khi cờ sớm tắt; khi bật giữ eligibility và cho ghi ban đầu trước start | Owner đã chốt cho điểm danh sớm trong cấu hình demo BE, sau đầy đủ các điều kiện trên | Cần đồng bộ đơn/bulk/QR và xử lý hủy/sửa điểm danh; lưu giờ thao tác thật | Tạo hôm nay cho một ca hợp lệ trong tương lai, lưu/reload đúng buổi; sai Coach/Member, chưa Paid/chưa xác nhận/chưa Accept đều bị chặn | Đã triển khai/kiểm local phạm vi mục4; runtime/FE chưa kiểm |

Đây là **yêu cầu đã chốt, chưa là hành vi hiện tại**. Ghi điểm danh sớm không tự đổi ngày giờ học, không tự đặt buổi `Completed` và không chứng minh buổi đã thực sự diễn ra. Phải nhận biết thao tác demo qua cấu hình/thông tin trả về hoặc audit phù hợp, không chèn chữ demo vào nhận xét chuyên môn.

## 4. Demo trọn luồng vào bất kỳ giờ nào

**`SCMS_EARLY_ATTENDANCE_ENABLED` phía BE** đã triển khai/kiểm local; mặc định false, cloud chưa đổi cấu hình. Sau triển khai BE demo duy trì true; chỉ điều khiển ghi ban đầu trước giờ học cho đơn/bulk/QR của Normal và Advanced. False trở về cửa giờ thường; correction/nhận xét giữ quy tắc riêng. Giữ Clock thật,7ca90phút, lịch làm/ngày nghỉ/Outdoor, quyền học và kiểm trùng tài nguyên. FE không cần nút bật/tắt hoặc gửi cờ bỏ kiểm; chi tiết vận hành nằm ở [ghi chú Owner](update261118.md).

“Phạm vi QR” là **mã điểm danh Member** dùng khi Coach quét/nhập cho buổi học. Mã dùng đủ300giây từ lúc cấp và hóa đơn mới tối đa30phút liên tục **độc lập công tắc điểm danh sớm**. Không đổi OTP, QR/link thanh toán hoặc quyền người quét; bật/tắt không đổi expiresAt hóa đơn cũ. Không còn lựa chọn phạm vi này cần hỏi Owner. [Kế hoạch nội bộ BE](update261118.md#ke-hoach-be) tách việc ghi ban đầu, lưu lượt cấp mã và thời hạn hóa đơn.

### Các mốc48–24–12 giờ hiện tại

| Mốc | Ý nghĩa | Khi demo có phải chờ đủ số giờ đó không? |
|---|---|---|
| Normal48h trước buổi đầu | Ngừng nhận đăng ký và thanh toán ghi danh mới từ mốc này | Không. Chọn buổi đầu còn cách hơn48h là có thể đăng ký/thanh toán ngay; cần thêm khoảng đệm đủ thao tác/trả tiền |
| Normal24h trước buổi đầu | Hạn chuẩn Manager xác nhận khai giảng; sau mốc phải có lý do, vẫn phải trước buổi đầu | Không. Có thể xác nhận ngay khi đủ người Paid, kể cả còn vài ngày mới học |
| Advanced24h sau Coach nhận đăng ký | Member phải xác nhận mua/tạo đơn trước khi cửa24h kết thúc | Không. Member tạo đơn ngay được; đây là hạn tối đa, không phải chờ24h. Đúng24h đã bị chặn |
| Advanced12h trước buổi | Khoảng báo trước tối thiểu khi Coach gửi đề xuất và Member Accept tạo buổi | Coach gửi một lịch cụ thể còn cách ít nhất12h; đúng Member có thể Accept ngay đề xuất đó sau kiểm lại, không tự đặt lịch bỏ qua Coach |
| Advanced12h trước buổi khi hủy | Còn ít nhất12h, gồm đúng12h, mới hủy hoàn credit thường nếu đủ các điều kiện khác | Không. Buổi dựng sẵn còn xa và chưa điểm danh có thể hủy ngay; dưới12h không hoàn thường |

Hai mốc24h phục vụ hai việc khác nhau; mốc12h đặt và12h hủy cũng là hai phép kiểm khác nhau. Hạn hóa đơn30phút bắt đầu khi tạo đơn; hạn45ngày sử dụng bắt đầu sau Paid hợp lệ. Không cộng các mốc này thành thời gian phải chờ để hoàn tất demo.

Các mốc là hạn chót hoặc khoảng báo trước, không thêm một bước đợi: Member có thể mua ngay sau Coach nhận đăng ký, CM có thể xác nhận ngay khi lớp đủ điều kiện. Với từng buổi Advanced, Coach đã thể hiện đồng ý với lịch bằng việc gửi đề xuất bất biến; Member Accept là phần đồng ý còn thiếu, không có bước Coach duyệt lại chính đề xuất ấy. Chọn ca còn>=12h chỉ thỏa điều kiện thời gian, không tự cấp quyền Member tạo buổi.

Lỗi và field FE đang có/chưa có được đối chiếu tại [thỏa thuận Owner](update261118.md#contract-da-thong-nhat). Qua T−24h của Normal không đồng nghĩa khóa xác nhận: trước T vẫn được xác nhận trễ có lý do/đủ điều kiện. Response lớp hiện chưa có đủ metadata các hạn; không suy còn hạn chỉ từ nhãn Open hoặc từ class.startDate. Các field mới trong bản FE là đề xuất, chưa là API đã triển khai.

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Sau18:15 hoặc qua00:00 không còn ca mới trong ngày | Giữ7ca; điểm danh “cả ngày của buổi” vẫn không cho thao tác hôm nay với buổi ngày mai | Đã chốt tạo ca tương lai và cho điểm danh sớm theo mục3 trong cấu hình demo | Thêm nhánh kiểm điểm danh, không phải đổi toàn bộ giờ vận hành | Chạy mới Normal và Advanced lúc10:00,20:30,23:55; tiếp tục sau00:00, không đổi Clock; kết quả ghi đúng hồ sơ | FE đã OK; triển khai UX/metadata theo ghi chú Owner |
| Có cần đồng thời đổi48h/12h/24h/45ngày? | Chọn Normal bắt đầu từ ngày hiện tại+3 trở đi, Advanced từ ngày+2 trở đi có thể giữ mốc hiện hành nếu ngày/ca/tài nguyên hợp lệ | Giữ các mốc đang có; chọn ca tương lai còn trong hạn, không tự áp giá trị0h từ V2 | Ít thay đổi liên quan; vẫn trình diễn được đăng ký/thanh toán/lên lịch/điểm danh ngay | Lớp mới đủ12buổi; Advanced ca kết thúc trong hạn; không cần đợi đến ngày học để trình diễn điểm danh | FE đã OK hướng hiển thị; giữ mốc và kiểm dữ liệu theo5.2 |
| Job, sửa điểm danh/nhận xét và thống kê sau điểm danh sớm | Hiện hoàn tất buổi và correction có mốc theo ngày giờ buổi; thống kê là dữ liệu mẫu theo ưu tiên của Bạn | Giữ job theo giờ thật; chưa tự hoàn tất ca tương lai. Công tắc chỉ ghi ban đầu; correction giữ cửa riêng, nhận xét sau buổi dùng hồ sơ đúng giờ đã chuẩn bị | Không tắt job hàng loạt chỉ để chạy ca tương lai; thống kê có thể gồm dữ liệu demo | Finalizer không ghi đè kết quả đã xác nhận; sửa/retry không nhân record, không đổi credit; ca hủy không bị ghi vắng lại | Giữ job/giờ thật và cửa correction riêng; rehearsal theo kế hoạch đã đồng ý |

**Ví dụ minh họa demo tối18/11, không phải lịch fixture đã được kiểm tài nguyên:**

- Normal20:30: CM tạo lớp tối đa1/tối thiểu1, buổi đầu18:15 ngày21/11 và đủ12buổi hợp lệ. Hạn đăng ký là18:15 ngày19/11, hạn chuẩn xác nhận khai giảng là18:15 ngày20/11. Member đăng ký/trả tiền lúc20:32; CM xác nhận20:34; Coach điểm danh sớm20:35. Mọi bước làm trong tối18/11, không chờ tới21/11.
- Advanced: dùng đăng ký Requested tạo sẵn; Coach nhận20:35 ngày18/11; Member tạo đơn20:36, hạn xác nhận mua là20:35 ngày19/11 và đơn có hạn riêng30phút. Thanh toán20:37; Coach đề xuất18:15 ngày20/11; Member Accept20:38 ngày18/11, còn hơn12h nên tạo buổi/trừ1; Coach điểm danh sớm20:39. Không phải chờ24h hay12h.

Vì điểm danh sớm đã giải quyết bước phải đợi giờ học, các bước khác đều có thể làm ngay với lịch tương lai. Đây là lý do đề xuất **giữ48h/24h/12h**, thay vì đồng thời đổi tất cả thành0h. Ngày/ca thật của bộ demo phải được kiểm lịch làm, môn/phòng/ngày nghỉ và hạn quyền trước; ngày+3/+2 là khoảng cách gợi ý, không phải luôn hợp lệ bất chấp tài nguyên.

### Có tạo thêm hồ sơ ngoài bộ dựng sẵn được không?

**Có. Không phải chỉ các hồ sơ seed mới chạy được.** Normal mới cần buổi đầu còn hơn48h và đủ thời gian thao tác thanh toán; chọn ngày hiện tại+3 là cách có khoảng đệm, không phải rule “phải cách hơn3ngày”. CM vẫn phải tạo đủ12buổi hợp lệ, có Coach/sân/Member không trùng và không vướng ngày nghỉ.

Advanced có thể gửi đăng ký mới, Coach nhận và Member mua ngay; chưa cần chọn ngày học ở bước gửi đăng ký. Sau Paid, Coach có thể gửi thêm đề xuất cho quyền học đó, Member Accept tạo thêm buổi nếu còn credit/đủ12h và buổi kết thúc trong hạn. Dùng Requested có sẵn chỉ tiết kiệm thời gian trình diễn. Không hứa tạo vô hạn: BE còn kiểm khả năng Coach đáp ứng12credit trong hạn, lịch làm, tài nguyên và account; bộ demo cần để sẵn năng lực/ca trống cho tình huống tạo thêm.

Các thao tác khác cũng phụ thuộc thời gian: hủy hoàn cần ca còn>=12h/chưa điểm danh/gói còn hạn; dời ca và sự cố có điều kiện riêng; nhận xét/correction sau buổi cần hồ sơ đã đến cửa giờ tương ứng. Không cần ép tất cả chúng thành ca tương lai: dùng đúng hồ sơ sẵn cho nhánh quá khứ, không đổi Clock hoặc sửa lịch sử để làm đẹp demo.

“Trọn luồng” ở đây gồm tạo/đăng ký → thanh toán → xác nhận/lên lịch → lưu điểm danh và các nhánh hủy/hoàn phù hợp. Các trạng thái cần12buổi hoàn tất, hết hạn45ngày, job sau giờ học hoặc correction sau buổi dùng hồ sơ riêng để trình bày; chưa cam kết làm tất cả từ một hồ sơ mới trong15–20phút.

## 5. Hủy Advanced và hoàn credit khi đã có điểm danh sớm

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Điểm danh sớm rồi Member hủy, còn>=12h thì sao? | `cancelPrivateSession` kiểm12h và trạng thái buổi/quyền/credit; chưa có kiểm riêng attendance cho tình huống điểm danh sớm | **Đã chốt:** buổi đã có điểm danh hiện hành Present/Absent không được hủy hoàn credit thường; không chỉ kiểm mỗi mốc12h | Bổ sung kiểm và khóa/transaction nhất quán với điểm danh; attendance superseded không phải kết quả hiện hành. Sự cố TT giữ workflow riêng | Chưa điểm danh, hủy đủ12h:12→11→12 đúng1lần. Đã điểm danh sớm: chặn hủy, còn11. Race không vừa hoàn vừa giữ điểm danh hợp lệ | FE đã OK metadata/error/UX; sửa và kiểm theoOwner mục kiểm hủy |
| Chỉ demo hủy có hoàn credit | Owner muốn DB có sẵn một buổi để bấm hủy và thấy credit tăng | Một ca Advanced Scheduled tương lai, chưa điểm danh, đúng gói Active còn hạn; có đề xuất Accepted/liên kết Member/consent fixture đúng mô hình, credit trước thao tác11 và đã trừ1 khi đặt | Tách ca hoàn khỏi ca vừa điểm danh trong luồng Advanced chính; không tạo nút cộng credit thủ công | Member bấm “Hủy buổi và hoàn1credit” → Cancelled/creditReturned:true; đọc lại gói11→12, hạn không tăng; bấm lại không hoàn thêm | Đã chốt có ca hoàn sẵn; không cần diễn nhánh không hoàn trên sân khấu nhưng BE vẫn phải test |

Credit tăng sau khi **hoàn tất thao tác hủy**, không phải khi buổi học chuyển Completed. Hoàn về đúng quyền học gốc, còn Active/chưa hết hạn, không vượt credit gốc; không gia hạn hoặc sửa lịch sử Paid/attendance cho đẹp. “Rollback DB làm credit trở lại12” là khôi phục dữ liệu, không phải bằng chứng API hủy đã hoàn đúng.

### Quyền học, môn và các lần trừ/hoàn

“Quyền còn hạn” là gói Advanced của đúng Member/môn/Primary Coach đã được Paid kích hoạt, còn `Active` và chưa tới `expiresAt` BE trả. Còn credit và còn hạn là hai điều khác nhau: còn5credit nhưng đã hết hạn vẫn không được đặt buổi mới hoặc hủy thường để biến thành credit mới. Buổi đặt phải kết thúc không muộn hơn deadline; thu/hủy không tự mở lại45ngày.45ngày sử dụng được tính từ Paid và có xử lý thời gian đóng/nghỉ/pauses theo D12; không tự đổi thành48ngày hoặc tự bỏ Chủ nhật khỏi hạn.

Code hiện cấp **12credit cho mọi quyền Advanced**, giá khác nhau theo môn. Credit gắn từng quyền học, không phải ví chung: mua cầu lông/Coach A không dùng số credit đó học bơi/Coach B. Muốn môn khác đi đăng ký/mua quyền riêng; Coach thay một buổi phải theo workflow hợp lệ, không tự đổi môn hoặc Primary Coach.

Gói Active còn0credit vẫn có thể còn các buổi đã đặt hợp lệ để học/điểm danh;0credit chặn đặt thêm, không tự hủy quyền của buổi đã giữ và không trừ lần hai. Normal dùng quyền từ chính hóa đơn lớp/ghi danh, không yêu cầu credit Advanced.

| Sự kiện | Credit theo thiết kế hiện hành |
|---|---|
| Thanh toán hợp lệ quyền Advanced mới | Cấp12 vào đúng quyền học đó |
| Coach chỉ gửi đề xuất Pending; Member từ chối/hết hạn đề xuất | Không trừ |
| Member Accept tạo thành công một buổi mới | Trừ1; gửi lại cùng quyết định không trừ lần hai |
| Điểm danh Present/Absent hoặc không đến học | Không trừ thêm credit đã dùng để đặt; không tự hoàn |
| Hủy hợp lệ buổi chưa điểm danh | Hoàn1 vào gói gốc đúng1lần cho buổi đó |

Báo cáo “khi trừ, khi không” **chưa được tái hiện ở lượt này**. Cần phân biệt đúng bước trước khi kết luận lỗi; nếu Accept tạo buổi nhưng đọc lại gói không giảm, hoặc cùng buổi giảm hai lần, đó là ca sai cần điều tra. Hết hạn/đối soát/sự cố TT còn có các nhánh điều chỉnh riêng, không gộp thành credit ngẫu nhiên.

**Owner18/11 xác nhận:** mỗi buổi mới được Member Accept trừ1credit; mỗi hủy hợp lệ hoàn1credit, cùng một buổi không hoàn hai lần. Giữ hủy trước ít nhất12giờ, quyền gốc còn hiệu lực và buổi chưa có điểm danh; không kéo dài hạn45ngày sử dụng. Đây là cơ chế dùng để bàn giao và kiểm thử, không còn là lựa chọn cần hỏi lại.

Demo hủy ở đây là **hoàn credit**, không hoàn tiền về Cash/VNPay và không đổi hóa đơn Paid. “BE kiểm cả nhánh bị chặn” nghĩa là test: dưới12h, đã điểm danh, sai Member, hết hạn hoặc gửi lặp phải từ chối/không tăng credit ngoài phép; không bắt Owner diễn các nhánh này trên sân khấu. Kiểm số dư sau commit, không chỉ đọc câu thông báo.

### FE phân biệt được hủy/không được hủy bằng cách nào?

Hiện `SessionResponse` có ngày/giờ buổi, `creditReturned` và `serverTime` khi GET danh sách/chi tiết; **chưa có đủ field quyết định hủy hoàn**. FE tính chênh12h chỉ phản ánh thời gian, chưa đủ biết đã điểm danh, quyền hết hạn hoặc buổi vừa bị người khác sửa. Không giao FE tự đoán toàn bộ rule. Với ca đã điểm danh sớm, đề xuất metadata không cho hủy và POST trả409 `SESSION_ALREADY_ATTENDED`, thông báo “Buổi học đã được điểm danh, không thể hủy hoàn credit.”; credit/buổi/attendance/hạn không đổi. Mã lỗi này chưa có trong source, không được coi đã triển khai.

**Owner18/11 đã đồng ý hướng API, FE đã OK, chưa phải field đã có:** bổ sung response buổi hiện hữu `cancellationAllowed`, `cancellationDeadline`, `cancellationBlockReason`, dùng `serverTime` đã có; đã điểm danh dùng409 `SESSION_ALREADY_ATTENDED`. Không cần thêm endpoint chỉ để lấy điều kiện. BE tính theo actor/buổi/quyền/điểm danh và kiểm lại nguyên khối khi POST hủy. Deadline chỉ nói mốc thời gian, không tự cấp quyền hủy; đồng bộ implementation/contract theo ghi chú Owner, không coi thiết kế là API đã chạy.

| Trường hợp | Gợi ý FE hiển thị | Kết quả khi thao tác |
|---|---|---|
| Còn>=12h, chưa điểm danh, đủ quyền | Nút “Hủy buổi và hoàn1credit”; xác nhận: “Buổi học sẽ bị hủy và hoàn1credit vào gói gốc.” | POST hủy; refresh buổi và subscriptions, chỉ báo “Đã hủy buổi. Credit11→12” sau khi đọc lại đúng dữ liệu |
| Còn dưới12h | “Đã quá hạn hủy hoàn credit: cần hủy trước buổi học ít nhất12giờ.” | Không hiện nút hủy hoàn; báo vắng là thao tác riêng không hoàn credit |
| Đã có điểm danh | “Buổi học đã được điểm danh, không thể hủy hoàn credit.” | Chặn kể cả còn hơn12h; không lấy Present/Absent làm hai mức hoàn khác nhau |
| Đã hủy/hoàn | “Đã hủy · Đã hoàn1credit” | Không có nút hoàn lần nữa; request lặp không tăng credit |
| Trang cũ, trạng thái vừa đổi | Thông báo lý do mới từ BE rồi tải lại | Không báo thành công dựa riêng việc bấm nút hoặc HTTP200 chưa đọc lại |

Ví dụ ca18:15 ngày20/11: mốc hủy là06:15 ngày20/11. Đúng06:15 còn đủ12h; sau mốc đó không hoàn thường. Nếu đã điểm danh từ tối18/11 thì không được hủy hoàn ngay cả trước06:15 ngày20/11. API local đã có metadata và409 SESSION_ALREADY_ATTENDED theo docs01; CANCEL_TOO_LATE, SESSION_ALREADY_CANCELLED và ADVANCED_PACKAGE_EXPIRED tiếp tục áp dụng. Runtime/FE chưa được suy đạt từ source.

## 6. Hóa đơn giữ chỗ qua giờ nghỉ và QR

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Giữ chỗ30phút lố giờ nghỉ, Lễ tân chưa thu được | Mặc định source mới30phút; `InvoiceService.createClassOrder` rút hạn tới cutoff đăng ký nếu sớm hơn. Chưa kiểm cấu hình thực của Render trong lượt này | **Đã chốt30phút liên tục**, không dừng qua giờ nghỉ. Đề xuất giữ trần hạn đăng ký: `expiresAt=min(lúc tạo+30phút,cutoff)` | Cash chưa xác nhận không cấp quyền; hết hạn giải phóng chỗ theo workflow expiry. FE hiển thị hạn thực BE trả | Đơn10:50 hết11:20 nếu không gặp hạn đăng ký sớm hơn; không giữ đến13:30. Paid/expiry tranh chấp phải cho kết quả nhất quán | Không hỏi lại30phút/giờ nghỉ; giải thích trần cutoff bên dưới |
| Chỗ đang giữ là gì? | Normal Pending giữ suất của lớp; lịch sân/Coach đã thuộc lịch lớp. Advanced đề xuất chưa giữ tài nguyên | Ghi rõ “giữ suất Normal”; Advanced chỉ giữ sân/Coach/Member khi Accept thành công | Không nhầm hóa đơn30phút với giữ sân hoặc giữ12buổi1:1 | Đơn hết hạn trả suất; không xóa lịch lớp. Pending đề xuất Advanced không làm sân bận | Không có thay đổi quyền/lịch được suy thêm từ TTL |
| QR300giây để trình bày thuận tiện | Owner đã chốt dùng đủ300giây từ lúc cấp; `AttendanceService` mới lưu lượt cấp300giây và trạng thái đã dùng | Đồng bộ sinh mã/scan/TTL/chống replay; FE đếm theo hạn BE, không chỉ đổi chữ30→300 | Lượt cấp trong members dùng migration26 giữ dữ liệu; không chuyển bước TOTP lịch sử thành lượt cấp giả | Mã cấp20:40 hết20:45; dùng trước hạn và chưa dùng mới hợp lệ, đã dùng thì không quét lại dù còn hạn. Lấy/refresh mã không tự gia hạn mã cũ | 300giây độc lập cờ điểm danh; không đổi OTP/QR thanh toán |

**Cutoff nghĩa là hạn chót nhận đăng ký/thanh toán ghi danh mới của lớp**, không phải giờ nghỉ. Với Normal hiện tại, cutoff = giờ buổi đầu trừ48h. Ví dụ buổi đầu18:15 ngày21/11 thì cutoff18:15 ngày19/11: tạo đơn10:50 ngày18/11 được đủ30phút tới11:20; tạo đơn18:05 ngày19/11 chỉ còn10phút tới18:15. Từ18:15 ngày19/11 không được tạo đơn mới. Nhờ chọn lịch Normal từ ngày demo+7 và Advanced/hủy từ ngày demo+5 trở đi, đường demo chính cách xa cutoff nên thường dùng đủ30phút.

Đề xuất giữ trần này để không còn đơn giữ suất/thu tiền sau lúc lớp ngừng nhận đăng ký; cách diễn đạt đúng là **tối đa30phút, có thể ngắn hơn nếu chạm hạn đăng ký**. Tiền tới muộn đi luồng đối soát, không tự hồi sinh suất đã cấp người khác. Quy tắc rút hạn này đang có với Normal, không suy thêm cutoff48h cho hóa đơn Advanced hoặc suy hạn mua24h là một cutoff hủy.

Normal Member tự chọn Cash và lỗi L01/L25/L28 trong V2 là các đầu mối cần kiểm lại source/runtime; không mặc định tất cả đã tái hiện hoặc cần viết lại. Khả năng FE đã có phải tận dụng.

## 7. Dữ liệu rehearsal và khôi phục

### Đối chiếu fixture cũ với thiết kế mới — 18/11

**Bảng dưới giữ kết quả rà18/11.** Lượt19/11 đã sửa nguồn Sunday, thêm riêng ca SUB01-S01 cùng proposal Accepted và11credit, triển khai BE30phút/QR300giây/điểm danh sớm. Không đọc các câu “chưa” trong bảng lịch sử như trạng thái hiện hành; xem docs00/QA và bundle theo ngày.

Phạm vi là **bản fixture/reference đã commit ở HEAD của lần rà này** so với các nguồn bundle local hiện tại; ref/hash được lưu ở [bằng chứng đối chiếu](../artifacts/runs/fe-decision-review-20261118/fixture-comparison.json), không phải so dữ liệu live. Đọc bản cũ bằng `git show HEAD:database/bootstrap/scms_demo_scenarios.sql` và tương tự cho reference. Đây là so hai bộ dựng lại, không phải migration hứa giữ thao tác cloud phát sinh giữa các vòng reset.

| Nhóm | Bản cũ → hiện tại | Đối chiếu quyết định/nguồn | Kết luận và việc phải làm |
|---|---|---|---|
| Ca Chủ nhật | Cả hai đều xóa mọi ca Coach `SUNDAY:` | [Nguồn sinh](../scripts/build-demo-fixture.py), [D12](04-DEBUG-NOTE.md#d12) | Hạn chế có từ bản cũ, chưa được sửa; nay không đáp ứng yêu cầu trung tâm mở7ngày, phải sửa nguồn sinh/fixture và test đang né Chủ nhật |
| Lớp Advanced mẫu theo môn | Bỏ seed template theo môn ở reference; fixture mới tạo8lớp riêng của đăng ký đã nhận | [Reference](../database/bootstrap/scms_demo_reference.sql), [D03/D04](04-DEBUG-NOTE.md#d03), [giải thích DB](../database/README.md#advanced-class-review) | Đúng hướng mới; Requested/Rejected không có lớp/quyền, đăng ký đã nhận khớp môn/Coach/lớp. Không khôi phục template vô chủ |
| Normal và ghi danh | Giữ đủ12kịch bản lớp có cùng tên; không mất key ghi danh theo Member+kịch bản | [Fixture](../database/bootstrap/scms_demo_scenarios.sql), [D15](04-DEBUG-NOTE.md#d15) | Sĩ số mẫu hợp công thức; lớp CM tạo mới để diễn phải dùng max=min1, không ép mọi lớp lịch sử thành1 |
| Hóa đơn/buổi | Giữ57ID Invoice và173ID Session; amount/Member/method/status Invoice không đổi trong các cột đã so | [Nguồn fixture](../database/bootstrap/scms_demo_scenarios.sql), [D02/D10](04-DEBUG-NOTE.md#d02) | Quan hệ private Session chuyển đúng lớp/quyền/Member; biểu thức mã Invoice chứa anchor/sequence nên không suy chuỗi SQL là mã thực hoặc bảo đảm mọi cột không đổi |
| Attendance/result | Không mất key cũ; bổ sung9cặp attendance/result | [Nguồn fixture](../database/bootstrap/scms_demo_scenarios.sql), [D15](04-DEBUG-NOTE.md#d15) | Result hiện có attendance/state khớp trong phần rà; không phải bằng chứng điểm danh sớm đã chạy hoặc mọi correction đã đạt |
| Danh mục nghỉ | Một ngày DEMO → giữ ngày đó và bổ sung11dòng danh mục | [Nguồn lịch](../scripts/demo_anchor.py), [D12/D14](04-DEBUG-NOTE.md#d12) | Không thấy việc bỏ danh mục này; activation/Holiday và tác động lịch vẫn phải kiểm theo môi trường |
| Quyền/credit | Active hiện còn12,10,0,12; khớp số buổi chưa hủy trong fixture và có validity45ngày | [Thiết kế sinh](../scripts/build-system-data.py), [D03/D05](04-DEBUG-NOTE.md#d03) | Không phải số dư ngẫu nhiên, nhưng **chưa có ca Active11credit để diễn11→12**; phải chuẩn bị ca mới đúng chủ/đề xuất/Accept, không chỉnh số dư riêng lẻ |
| Đề xuất lịch và thay đổi demo | Bảng đề xuất chưa seed dòng; dữ liệu/source chưa áp điểm danh sớm,30phút,QR300 | [D04](04-DEBUG-NOTE.md#d04), [thiết kế Owner](#3-điểm-danh-ai-xác-nhận-điều-gì) | Requested có sẵn là đầu vào đúng cho luồng động; ca hoàn mới cần nguồn đồng ý rõ. Không backfill consent giả cho Session lịch sử, không coi bundle hiện đã sẵn sàng toàn bộ |

Các kiểm văn bản đã xác nhận liên kết đăng ký/lớp/quyền/Member, minimum, validity và số dư Active trong phạm vi cột đã kiểm. **Chưa chạy lại SQL, race, biên thời gian hoặc ghi FE** trong đợt rà này; không kết luận toàn fixture đúng chỉ từ count. Phạm vi bảng reset không mất bảng cũ, thêm bảng đề xuất lịch; khi thực thi vẫn xóa/dựng lại đúng phạm vi SCMS đã mô tả ở docs02, không phải xóa riêng những dòng nêu trong bảng.

### Chủ nhật: BE local đã kiểm, cloud và FE còn chờ

Owner18/11 yêu cầu trung tâm **mở đủ7ngày trong tuần**, không nghỉ cuối tuần mặc định. Source BE hỗ trợ `SUNDAY`/ngày7; FE main được kiểm có Chủ nhật trong form/lưới Coach và helper đổi0 JavaScript thành7 API. Lượt19/11 đã bỏ câu SQL xóa ca Chủ nhật, đồng bộ thiết kế bảy ngày và thay test né Chủ nhật bằng tạo đề xuất/Member Accept/hủy trên Chủ nhật thật. Lịch nghỉ riêng của Coach và Holiday vẫn áp dụng; cloud và FE chưa kiểm. Trạng thái chuẩn ở docs00/QA.

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Chủ nhật không có ca khả dụng trong bộ cũ | Lượt18/11 thấy nguồn xóa Sunday và test né ngày này; lượt19 đã bỏ hạn chế | Nguồn sinh/fixture/test đã đồng bộ7ngày, giữ nghỉ cá nhân/Holiday/Outdoor; kiểm local đã tạo Normal12buổi Chủ nhật và Advanced đề xuất/Accept Chủ nhật | Không buộc mọi Coach làm7ngày ở mọi môi trường; không suy cloud đã đổi lịch | BE local đọc lại đúng lịch/quyền; FE đọc/lưu trên website vẫn chờ tích hợp và kiểm riêng. Coach nghỉ/trùng/Holiday tiếp tục dùng guard hiện hành | Bộ19 có bằng chứng local; bộ21 SQL/HTTP chờ đúng ngày |

### Các vòng kiểm và luyện tập đã thống nhất về quy trình

1. **Thiết kế → rà lại → Owner/FE chốt:** kiểm từng yêu cầu, ví dụ, dữ liệu và phần ngoài scope; giữ cơ chế credit Owner đã xác nhận tại mục5.
2. **Sửa/test BE local:** BE/API/fixture sửa đúng nơi phát sinh lỗi, kiểm riêng schema/SQL. Chỉ sửa DB không làm BE cũ tự hỗ trợ điểm danh sớm/QR300. Hoàn thiện và kiểm thử BE trước khi báo FE tích hợp; triển khai BE tương thích, merge/deploy và SQL cloud chỉ thực hiện theo phạm vi được giao.
3. **Vòng API:** chuẩn bị bộ schema theo ngày cài thực; Owner chạy Supabase. Sau PASS/lấy phiên và ID mới, agent smoke đích nếu cần, kiểm thao tác ghi và readback quyền/credit/lịch. Lưu bằng chứng đúng phiên bản, không dùng count/GET làm pass nghiệp vụ.
4. **Vòng trình diễn có quan sát:** trở lại cùng bộ nền bằng SQL Owner chạy. Trước thao tác, gửi Owner danh sách actor → bước → kết quả mong đợi để đọc. Dùng browser có phiên hiển thị cho Owner nếu công cụ hỗ trợ; nếu không, hướng dẫn theo BE/Swagger Render đã kiểm giao diện. Không hứa đã có phiên Playwright xem chung khi chưa mở/kiểm được.
5. **Vòng Owner kiểm FE:** Owner chạy lại cùng bộ nền rồi luyện trên FE theo hướng dẫn đã đối chiếu; khác dự kiến thì kiểm đúng source/runtime/dữ liệu, sửa đúng tầng. Không mặc định mọi lỗi FE đều cần chỉnh schema.
6. **Đóng bộ và tái dùng:** sau một lượt đạt, chốt cùng tên/môn/role/ca/số lượng và script sinh một nguồn; kiểm lại phần thực sự thay đổi qua các vòng. SQL reset luôn theo phạm vi được giao, dừng writer/job theo docs02; thay ngày phải sinh/kiểm cả bộ, không bỏ guard. Chỉ lặp reset khi chuẩn bị lượt sạch theo kế hoạch, không tự reset cloud.

| Vấn đề | Bằng chứng | Phương án | Ảnh hưởng | Điều kiện nghiệm thu | Phần cần chốt |
|---|---|---|---|---|---|
| Muốn trở lại mốc sạch sau rehearsal | Đã có [quy trình snapshot](02-DEPLOYMENT-GUIDE.md#demo-snapshot-recovery); chưa có mốc mới được kiểm restore. Snapshot giữ nguyên ngày/state cũ | Owner đồng ý một bộ nền, hồ sơ mới mỗi lượt và vài hồ sơ có sẵn. Nếu snapshot bất tiện, sau một lượt rehearsal đạt sẽ chốt bộ dữ liệu/rule rồi sinh theo ngày cài thực khi cần | Giữ cùng tên/môn/role/ca/số lượng xuyên suốt; chỉ tính lại ngày phù hợp từ anchor, không thay kịch bản mỗi lần | Sinh cùng mẫu cho các anchor kiểm; Requested chưa có lớp, ca hoàn chưa điểm danh, credit/quyền/lịch đúng. Diễn lại đúng thao tác trên website | Giữ tên/tài khoản/môn/sân/ca/số lượng đã duyệt; chỉ chốt ngày chạy và kiểm tính hợp lệ, chưa sinh DB trong lượt này |

Ưu tiên đơn giản: **một bộ nền ổn định + ca tương lai mới mỗi lượt + vài hồ sơ sẵn cho nhánh cần quá khứ**. Nếu Bạn chọn sinh lại theo từng ngày rehearsal/demo để quay về đúng bộ nền thì dùng một mẫu đã chốt và anchor ngày cài thực; đây là lựa chọn chuẩn bị dữ liệu, không phải hệ thống bắt buộc reset mỗi ngày. DB còn đủ ca hợp lệ thì vẫn có thể dùng tiếp.

Bộ này phải có ít nhất ba hồ sơ tách biệt: **Normal tạo mới sĩ số1; Advanced Requested để Coach nhận/mua/Accept/điểm danh; Advanced đã Paid/đặt một ca chưa điểm danh để hủy hoàn11→12**. Không dùng cùng đăng ký Requested như thể đã có gói Paid, hoặc lấy buổi vừa điểm danh để diễn hoàn. Ngày của ca hoàn phải còn đủ12h tại giờ demo dự kiến; chọn ngày+2 trở đi giúp có khoảng đệm, vẫn kiểm tài nguyên/hạn.

Hướng dẫn luyện tập cập nhật vào [kịch bản hiện có](reference/note-script-demo.md), gồm role đăng nhập → màn hình/thao tác → dữ liệu cần chọn → kết quả cần thấy/đọc lại → cách chuẩn bị lượt kế. Sau rehearsal đạt mới đóng bộ dữ liệu cố định và sinh từ một nguồn, đồng bộ SQL/fixture/SYSTEM-DATA; không tự đổi account, môn hoặc số lượng theo mỗi lần chạy. Nếu diễn lại trên cùng DB, ca đã hủy không thể hoàn lần nữa; dùng ca mới hoặc trở về bộ nền theo phạm vi reset đã được giao.

Khả năng backup/restore trên Dashboard phụ thuộc gói Supabase; tài liệu chính thức nêu backup hằng ngày cho Pro/Team/Enterprise, Free dùng phương án export. Chưa xác định gói của project nên chưa hướng dẫn nút thao tác. Backup DB cũng không chứa file object của Storage. [Nguồn Supabase](https://supabase.com/docs/guides/platform/backups).

Snapshot không hoàn tác chuyển khoản/email đã gửi; giữ mã hóa đơn không tái dùng và xử lý phiên/link cũ khi restore. Chi tiết kỹ thuật giữ tại docs02, không thêm một quy trình trùng ở đây.

## 8. Phạm vi đã duyệt và thứ tự thực hiện tiếp

**Đã rõ:** cấu hình demo BE; điểm danh sớm theo điều kiện hai flow; không hoàn sau điểm danh; ca hoàn sẵn; metadata hủy từ BE;30phút liên tục;300giây từ lúc cấp; một bộ nền xuyên suốt và trung tâm mở7ngày. Chưa triển khai. Không hỏi lại các giá trị đã chốt.

**Tóm tắt phạm vi FE đã đồng ý; chi tiết sửa/test ở ghi chú Owner mục3–4:**

1. **Thời gian:** đề xuất giữ48h nhận đăng ký/24h xác nhận khai giảng/24h mua Advanced/12h đặt-hủy. Để demo liên tục, tạo Normal tương lai còn hơn48h (gợi ý ngày+3), Advanced chọn ca còn>=12h và trong hạn (gợi ý ngày+2); mở điểm danh sớm theo điều kiện đã chốt. Ví dụ18/11 tạo Normal bắt đầu21/11, Advanced tập20/11, thao tác mua/xác nhận/điểm danh ngay18/11. Vẫn được tạo thêm hồ sơ, không phụ thuộc chỉ bản seed.
2. **Sĩ số:** lớp demo mới max=min1; FE giải thích đây là cách dùng công thức>=2/3 hiện tại, không gửi max15/min1.
3. **Quyền học:** mỗi quyền Advanced có12credit cho đúng môn/Primary Coach và45ngày sử dụng từ Paid; FE hiển thị số dư/hạn BE trả, không gộp credit khác môn. Requested tạo sẵn chưa có lớp/quyền/hóa đơn.
4. **Credit — đã chốt:** trừ1 ở Accept tạo buổi, không trừ khi chỉ gửi đề xuất/điểm danh; hủy hợp lệ trước ít nhất12giờ hoàn1 vào quyền gốc còn hiệu lực. Một buổi chỉ hoàn1lần, đã điểm danh không hủy hoàn; không kéo dài hạn45ngày sử dụng.
5. **Hủy trên FE:** BE trả được hủy/hạn/lý do; FE có nút/xác nhận/thông báo như mục5, reload buổi/gói sau lưu. Đã điểm danh không hủy hoàn. Owner chỉ diễn ca hoàn sẵn11→12; BE kiểm riêng các request phải bị từ chối và xác nhận số dư không đổi.
6. **Thanh toán/QR:** hóa đơn mới tối đa30phút liên tục, giữ trần cutoff Normal; mã điểm danh Normal/Advanced300giây từ lúc cấp và chống dùng lại. Cả hai độc lập công tắc điểm danh sớm; không đổi OTP/QR thanh toán hoặc hạn hóa đơn cũ.
7. **Chủ nhật:** FE đã có biểu diễn ngày; cần kiểm đúng website và đổi0→7 trên caller thực. Sửa nguồn fixture đang xóa Sunday, phân công ca Coach để trung tâm mở7ngày; thêm ca kiểm ghi/đọc lại Chủ nhật.
8. **Luyện tập:** một bộ dữ liệu/cách thao tác; agent gửi trước bước và kết quả, vòng API → vòng có quan sát/Swagger → vòng Owner FE. Sau rehearsal đạt mới đóng dữ liệu/guide; sửa đúng BE/FE/DB, không sinh nhiều bộ khác nhau.

**Đã thống nhất:** hiển thị hạn/quyền thao tác/lý do, metadata hủy và409 `SESSION_ALREADY_ATTENDED`; công tắc chỉ ghi ban đầu sớm, QR300giây/hóa đơn30phút độc lập; Normal Member VNPay, Advanced Reception Cash. Mốc Dxx/cutoff Normal/credit12h giữ nguyên. Đã chọn thêm invoiceId/invoiceStatus để FE đọc hóa đơn hiện có, thay đề xuất order đầy đủ; lượt cấp QR300giây đã triển khai cùng migration26 giữ dữ liệu. Hai anchor19/11 và21/11 đã chốt; bản21 giữ SQL execution NOT_RUN đến đúng ngày; không có mâu thuẫn nghiệp vụ mới cần hỏi ở lượt này.

Giữ job theo giờ thật; giữ cơ chế Absent và code/tests chế tài khóa7ngày, toggle chế tài tiếp tục mặc định tắt. Chưa tự quyết thêm luồng Normal Member tự chọn Cash: hiện có Member VNPay hoặc Reception đăng ký Cash tại quầy; ghi chú H10/V2 và lỗi L01/L25/L28 cần đối chiếu đúng source/website trước khi đưa vào patch. Khi chuẩn bị kịch bản sẽ chọn cách thanh toán đã được hỗ trợ/nghiệm thu, không tự mở rộng chỉ vì demo.

Phạm vi giao việc BE nằm ở [ghi chú Owner](update261118.md#ke-hoach-be): thỏa thuận API → điểm danh/quyền/hủy → metadata/liên kết hóa đơn → Invoice/QR/DB → fixture/kiểm BE tổng hợp → điền bàn giao mới và báo FE. FE tự tích hợp sau khi BE đạt; các phát hiện giao diện chỉ là mục nhắc khi bàn giao. Sau đó nhắc Owner kiểm thủ công hoặc xem lượt có quan sát và rehearsal bốn flow. Kế hoạch không tự mở quyền merge/deploy/reset/cloud SQL.

Chưa merge/push/deploy, chạy SQL, tạo giao dịch/email hoặc thực hiện phương án mới trong đợt này. Kết quả kiểm tài liệu và phần chưa chạy được ghi đúng phạm vi tại Active/QA.
