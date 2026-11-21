# SCMS — YÊU CẦU BACKEND: VÒNG ĐỜI LỚP NORMAL (BẢN RÚT GỌN)

**Phạm vi:** Lớp Normal học theo nhóm, đăng ký cả khóa **12 buổi**. Chỉ nêu **quy tắc nghiệp vụ bắt buộc**; BE tự kiểm tra logic hiện tại, tái sử dụng phần đúng và quyết định cách triển khai. **Không sửa luồng Advanced**. Phần hoàn tiền tuân theo tài liệu Refund Normal riêng.

## 1. Vòng đời Normal

**Tạo lớp → `Open` (nhận đăng ký + thanh toán) → chốt sĩ số → Manager xác nhận khai giảng → tự chuyển `Ongoing` khi đến buổi đầu → hoàn tất các buổi/điểm danh/học bù → `Closed`.**

- **Tạo lớp:** Phải có Coach, sân/phòng, lịch hợp lệ và tạo đủ 12 buổi. Không mở bán một lớp chưa tạo lịch thành công.
- **Đăng ký:** `Pending` hợp lệ chỉ **giữ chỗ tạm có hạn** để chống vượt sức chứa; không tính vào sĩ số khai giảng. Chỉ học viên có **Invoice `Paid` + Enrollment `Registered` hợp lệ** được tính vào `minCapacity`. `Pending` hết hạn phải giải phóng chỗ; xử lý an toàn trường hợp giao dịch thành công nhưng thông báo đến muộn.
- **Chốt sĩ số — ĐÃ CHỐT:** Hạn đăng ký là **48 giờ trước giờ bắt đầu buổi học đầu tiên**. Hết hạn, BE tính số Member `Paid + Registered`, thông báo lớp đủ/thiếu sĩ số; không nhận ghi danh mới. Nếu dời ngày khai giảng hợp lệ, tính lại hạn chốt từ ngày mới và kiểm tra lại các điều kiện. Không nhận đăng ký sau khi buổi đầu đã bắt đầu, bất kể trạng thái hiển thị.
- **Manager xác nhận — ĐÃ CHỐT:** Chậm nhất **24 giờ trước buổi học đầu tiên**, Manager phải xác nhận mở lớp nếu đạt `minCapacity`, có Coach, phòng và lịch hợp lệ; lưu người và thời gian xác nhận. Quá mốc chưa quyết định: hệ thống cảnh báo, **không tự khai giảng**; Manager phải xử lý trước khi buổi đầu diễn ra. Đủ sĩ số không đồng nghĩa tự khai giảng ngay.
  **Ngoại lệ Owner duyệt10/11:**24h là hạn chuẩn xác nhận **khai giảng**, khác Open nhận đăng ký. Nếu trễ hạn, Manager vẫn được xác nhận trước giờ buổi đầu khi đủ mọi điều kiện, có lý do và lưu người/thời gian quyết định trễ; không mở lại đăng ký sau cutoff48h. Đến buổi đầu vẫn chưa có xác nhận thì không chuyển Ongoing; Manager phải xử lý dời/hủy đúng phạm vi, hệ thống không tự hủy.
- **`Open → Ongoing`:** BE tự chuyển khi tới giờ bắt đầu buổi hợp lệ đầu tiên nếu Manager đã xác nhận và điều kiện còn hợp lệ; nếu không đủ điều kiện thì không chuyển, phải cảnh báo.
- **`Ongoing → Closed`:** Chỉ đóng sau khi học viên được cung cấp đủ **12 buổi hợp lệ** (gồm buổi bù thay thế nếu cần), các buổi/điểm danh đã chốt và không còn nghĩa vụ học bù. Không đóng chỉ vì qua ngày kết thúc.

## 2. QUY TẮC `minCapacity` ĐÃ CHỐT — BẮT BUỘC

**Mặc định khi tạo lớp:**

`minCapacityDefault = ceil(maxCapacity × 2 / 3)` (**làm tròn lên số nguyên**).

- `maxCapacity` là **sức chứa tối đa của lớp**, không được lớn hơn sức chứa của sân/phòng chọn.
- Hệ thống **tự điền `minCapacityDefault`** khi tạo lớp Normal.
- **Center Manager được TĂNG** `minCapacity` khi tạo lớp, trong khoảng **`minCapacityDefault ≤ minCapacity ≤ maxCapacity`**.
- Manager **KHÔNG được giảm** `minCapacity` thấp hơn mức 2/3 mặc định; BE phải xác thực điều này, không chỉ kiểm tra ở FE.
- Chỉ được chỉnh `minCapacity` **trong lúc tạo lớp, trước khi có bất kỳ Member đăng ký/thanh toán**. Một khi lớp đã có đăng ký (kể cả đang `Pending`), **khóa `minCapacity`**, không cho chỉnh qua giao diện hoặc API cập nhật.
- Nếu Manager đổi `maxCapacity` lúc tạo lớp thì tính lại `minCapacityDefault`, và giá trị chọn phải được kiểm tra lại theo giới hạn mới.
- **Lưu ý:** Bảng dưới đây lấy sức chứa sân/phòng làm `maxCapacity` mặc định khi mở lớp. Nếu Manager tạo lớp nhỏ hơn sức chứa phòng, phải tính `minCapacity` theo **sức chứa lớp đã chọn**, không dùng trực tiếp ngưỡng trong bảng.

### Bảng mặc định cho TOÀN BỘ 20 sân/phòng (theo danh sách cung cấp)

| STT | Tên sân/phòng | Khu vực | Loại | `maxCapacity` mặc định | `minCapacity` mặc định |
|---:|---|---|---|---:|---:|
| 1 | Sân Tennis 01 | Khu A – Ngoài trời | Outdoor | 4 | **3** |
| 2 | Sân Tennis 02 | Khu A – Ngoài trời | Outdoor | 4 | **3** |
| 3 | Hồ bơi 01 | Khu hồ bơi | Outdoor | 10 | **7** |
| 4 | Hồ bơi 02 | Khu hồ bơi | Outdoor | 10 | **7** |
| 5 | Sân Pickleball 01 | Nhà thi đấu B | Indoor | 4 | **3** |
| 6 | Sân Pickleball 02 | Nhà thi đấu B | Indoor | 4 | **3** |
| 7 | Khu Bắn cung 01 | Khu C – Ngoài trời | Outdoor | 5 | **4** |
| 8 | Khu Bắn cung 02 | Khu C – Ngoài trời | Outdoor | 5 | **4** |
| 9 | Sân Bóng rổ 01 | Nhà thi đấu A | Indoor | 8 | **6** |
| 10 | Sân Bóng rổ 02 | Nhà thi đấu A | Indoor | 8 | **6** |
| 11 | Sân Bóng chuyền 01 | Nhà thi đấu A | Indoor | 8 | **6** |
| 12 | Sân Bóng chuyền 02 | Nhà thi đấu A | Indoor | 8 | **6** |
| 13 | Sân Bóng đá 01 | Khu B – Ngoài trời | Outdoor | 15 | **10** |
| 14 | Sân Bóng đá 02 | Khu B – Ngoài trời | Outdoor | 15 | **10** |
| 15 | Sân Cầu lông 01 | Nhà thi đấu B | Indoor | 4 | **3** |
| 16 | Sân Cầu lông 02 | Nhà thi đấu B | Indoor | 4 | **3** |
| 17 | Bàn Bóng bàn 01 | Phòng Bóng bàn | Indoor | 2 | **2** |
| 18 | Bàn Bóng bàn 02 | Phòng Bóng bàn | Indoor | 2 | **2** |
| 19 | Bàn Bóng bàn 03 | Phòng Bóng bàn | Indoor | 2 | **2** |
| 20 | Bàn Bóng bàn 04 | Phòng Bóng bàn | Indoor | 2 | **2** |

**Bảng quy đổi khi kiểm thử:** `2 → 2`; `4 → 3`; `5 → 4`; `8 → 6`; `10 → 7`; `15 → 10`.

**Ví dụ điều chỉnh:** Phòng chứa 10, lớp mở 10: mặc định tối thiểu **7**; Manager được tăng lên **8, 9 hoặc 10**, nhưng không được đặt **6**. Phòng chứa 10, lớp chỉ mở tối đa 8: mặc định tối thiểu **6**; Manager có thể chọn **6–8** trong lúc tạo lớp.

## 3. Thiếu sĩ số, dời lịch, hủy lớp

- **Trước buổi đầu:** Thiếu sĩ số hoặc chưa được Manager xác nhận thì **không tự khai giảng**; cảnh báo để Manager quyết định **dời ngày khai giảng / đề nghị chuyển lớp / hủy trước khai giảng**. Không tự động hủy chỉ vì ít người.
- **Dời ngày khai giảng tối đa 01 lần do thiếu sĩ số trước buổi đầu** (không giới hạn các lần dời buổi học riêng lẻ có sự cố hợp lệ sau khi lớp `Ongoing`); phải thông báo và lấy sự đồng ý của Member đã thanh toán. Khi có ngày mới, tính lại hạn đăng ký 48 giờ, hạn Manager xác nhận 24 giờ và kiểm tra lại sĩ số. Nếu có Member từ chối và yêu cầu hoàn, tính lại sĩ số còn hiệu lực; **không khai giảng nếu dưới `minCapacity`**. Nếu đã dời 01 lần mà vẫn thiếu sĩ số, chỉ được đề nghị chuyển lớp khi Member đồng ý hoặc hủy **trước buổi đầu** và xử lý hoàn tiền theo tài liệu Refund.
- **Chuyển lớp** chỉ được thực hiện khi Member đồng ý rõ ràng; nếu từ chối, Member có thể yêu cầu hoàn theo tài liệu Refund Normal.
  **Owner chốt10/11 — tương thích:** cùng môn, cùng giá học phí đã thanh toán và cùng center; lớp đích Open, còn hạn nhận đăng ký/chỗ, chưa bắt đầu, đủ12buổi hợp lệ, Coach/sân/ngày nghỉ và lịch Member phù hợp. Preview chưa giữ chỗ; khi Member xác nhận phải khóa/kiểm lại, giữ hóa đơn gốc và liên kết chuyển quyền. Nếu không có lựa chọn phù hợp thì lập phương án tổ chức khác còn hợp lệ hoặc hoàn100% đúng phạm vi; không tự chuyển khác điều kiện, coi im lặng là đồng ý, hoặc vừa chuyển vừa hoàn cùng quyền.
- **Trung tâm hủy trước buổi đầu:** Chủ động phát sinh hồ sơ hoàn cho các khoản đã thu hợp lệ; `Pending` chưa thu thì hủy và giải phóng chỗ.
- **Khi lớp đã `Ongoing`: CẤM hủy lớp theo luồng hủy thông thường** (giao diện và API). Nếu trung tâm gặp sự cố: **ưu tiên thay Coach cho buổi chưa bắt đầu**, hoặc **dời buổi chưa bắt đầu**; nếu buổi đã bắt đầu/không diễn ra như cam kết, **ghi nhận sự cố và tạo buổi học bù**, không sửa/xóa lịch sử buổi cũ. Thông báo Member, kiểm tra xung đột Coach/phòng/lịch và bảo đảm đủ 12 buổi thực học hợp lệ.
- **Ngoại lệ bất khả kháng:** Nếu về thực tế không thể tiếp tục tổ chức các buổi còn lại, BE phải báo Owner/Manager để có phương án bảo vệ quyền lợi Member riêng; **không tự đóng lớp, không tự hủy/xóa nghĩa vụ, không giữ tiền mà không cung cấp giải pháp**. Chính sách giải quyết ngoại lệ sau khai giảng chưa thuộc phạm vi Refund Normal hiện tại.

## 4. Các nguyên tắc kỹ thuật/nghiệp vụ phải bảo đảm

- Không chuyển trạng thái ngược/sai (`Ongoing → Open`, `Open → Closed`, `Ongoing → Cancelled` trong luồng thông thường; không mở lại `Cancelled/Closed` tùy tiện). **BE kiểm soát ở tầng xử lý nghiệp vụ/API**, không chỉ giao diện.
- Chống vượt sức chứa khi nhiều người thanh toán cùng lúc; đối soát tiền thực thu nếu callback VNPay chậm/trùng hoặc giao dịch quá hạn. Không mất tiền và không cấp hai suất cho một chỗ.
- Tạo 12 buổi, chốt điểm danh, học bù và chuyển trạng thái phải an toàn khi job chạy lặp hoặc hệ thống khởi động lại.
- Không xóa hóa đơn, Enrollment, Attendance, quyền học khi dời/hủy; giữ lịch sử thao tác và thông báo. **Trạng thái lớp** phải tách khỏi trạng thái Invoice/Enrollment.

## 5. Các quyết định thanh toán đã chốt

- **Cash:** Ưu tiên Member đến quầy, Lễ tân đăng ký và **thu tiền ngay**; tránh Member chọn Cash online rồi giữ chỗ chờ đến trung tâm. BE **kiểm tra luồng Cash hiện có và tính khả thi trước khi sửa**, không làm mất kiểm soát sức chứa/hóa đơn.
- **VNPay Pending:** Giữ thời hạn thanh toán đang cấu hình **nếu BE xác minh hoạt động đúng**; không bắt đổi thành 5/15 phút cố định. Hạn giữ chỗ/thanh toán phải được đối soát với hạn chốt đăng ký; tiền đã thu nhưng callback muộn phải được xử lý, không bỏ sót.

## 6. BE cần thực hiện

1. **Kiểm tra hiện trạng trước, chưa sửa:** xác định phần đúng, thiếu hoặc lỗi; rà soát Cash/VNPay, `Pending`, phân quyền, vòng đời lớp, xử lý sự cố, thay Coach, dời buổi và học bù. Đặc biệt kiểm tra chặn `Ongoing → Cancelled` và việc tạo buổi bù khi lớp đang `Ongoing`/`Closed` (đối chiếu các trạng thái hiện có trong `NormalMakeupService`).
2. **Đã chốt, không hỏi lại:** hạn đăng ký **48 giờ**, hạn chuẩn Manager xác nhận **24 giờ trước buổi đầu** cùng ngoại lệ xác nhận trễ có kiểm soát được duyệt10/11 ở mục1, dời khai giảng thiếu sĩ số tối đa **01 lần**, công thức 2/3 và quyền chỉ được tăng `minCapacity`, bảng 20 sân/phòng; không hủy lớp `Ongoing` theo quy trình thông thường.
3. **Chỉ đề xuất/xác minh phần kỹ thuật và ngoại lệ thực sự thiếu:** hạn VNPay hiện tại, chuyển đổi Cash tại quầy, các sự cố không thể tiếp tục tổ chức. Không tự đặt quy tắc hoàn tiền sau khai giảng.
4. **Sau khi thống nhất mới sửa:** giữ phần đang đúng, chỉ triển khai phần thiếu; kiểm thử an toàn, không thu tiền/ghi dữ liệu thật trên production. Báo cáo rõ API và FE cần phối hợp.

**Phạm vi không bao gồm:** thay đổi Advanced hoặc triển khai lại quy trình Refund đã có tài liệu riêng.
