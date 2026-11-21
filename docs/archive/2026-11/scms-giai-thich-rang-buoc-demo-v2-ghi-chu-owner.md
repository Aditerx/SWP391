# SCMS: Vì sao chưa demo liền mạch được và cách xử lý (bản dễ hiểu, 16/11/2026)

Bản này viết lại `scms-rang-buoc-khoa-cung-workflow-demo.md` theo cách dễ đọc cho cả nhóm, đã cập nhật các quyết định sau đó (đổi luôn cho hệ thống, giữ 7 ca, Member chọn Cash hoặc VNPay ở cả Normal và Advanced). Bản kỹ thuật gốc vẫn giữ để tra tên tệp và mã lỗi.

## 1. Tóm tắt trong 30 giây
Mục tiêu: trong **một buổi demo, vào bất kỳ ngày giờ nào**, chạy được trọn vẹn Manager tạo lớp → Member đăng ký và trả tiền → Manager khai giảng → Coach điểm danh, và luồng Advanced 1-1.

Hiện chưa làm được vì hệ thống đang có nhiều "cái khóa thời gian và thủ tục" do nhóm tự đặt. Mỗi khóa đều có lý do nghiệp vụ, nhưng cộng lại thì một lớp Normal phải mất ít nhất hơn 2 ngày mới chạy xong. Ý tưởng giải quyết: **biến các mốc thời gian thành tham số cấu hình và nới chúng ra**, giữ nguyên các quy tắc quan trọng (đủ người mới khai giảng, phải thanh toán mới được đăng ký, chỉ có Có mặt hoặc Vắng).

## 2. Các khóa, gom theo ý nghĩa

### Nhóm A: Khóa thời gian (chặn nhiều nhất)
| Khóa | Hiểu đơn giản | Vì sao chặn demo | Đề xuất |
|---|---|---|---|
| Đóng đăng ký 48 giờ (H1) | Lớp Normal ngừng nhận đăng ký 48 giờ trước buổi đầu | Lớp vừa tạo hôm nay đã quá hạn đăng ký | Đổi thành 0 giờ: đăng ký đến lúc buổi đầu bắt đầu (C1) |
| Điểm danh đúng giờ ca (H6) | Chỉ ghi Có mặt hoặc Vắng trong khoảng giờ của buổi | Chưa tới giờ thì không điểm danh được | Cho điểm danh cả ngày của buổi (C3) |
| Mã QR sống 1 đến 30 giây (H8) | Member hiện mã, Coach nhập trong 30 giây | Dễ hết hạn khi đang trình bày | Giữ; tùy chọn nâng lên 120 giây (C13) |
| Đặt buổi 1-1 trước 12 giờ (H12) | Advanced phải đặt buổi trước giờ học 12 giờ | Không thể đặt buổi cho hôm nay | Đổi thành 0 giờ, không cho số âm (C4). Khi đó chỉ đặt được ca chưa bắt đầu |

### Nhóm B: Thủ tục duyệt lớp
| Khóa | Hiểu đơn giản | Đề xuất |
|---|---|---|
| Sĩ số tối thiểu (H3) | Lớp cần đủ người đã trả tiền (tối thiểu = 2/3 sĩ số tối đa, làm tròn lên) mới khai giảng | Giữ. Khi demo tạo lớp sĩ số tối đa nhỏ (1 đến 4) |
| Manager khai giảng trước giờ buổi đầu (H4) | Manager phải bấm "Xác nhận khai giảng", quá giờ thì kẹt | Giữ; tùy chọn cho phép bấm đến hết buổi đầu (C11) |
| Lớp phải ở trạng thái `Ongoing` mới điểm danh (H5) | Trạng thái chỉ đổi sau giờ buổi đầu | Cho phép điểm danh ngay khi Manager đã xác nhận khai giảng, lớp vẫn nhận đăng ký (C2) |

### Nhóm C: Thanh toán
| Khóa | Hiểu đơn giản | Đề xuất |
|---|---|---|
| Hóa đơn giữ 5 phút (H9) | Hết 5 phút hóa đơn tự hủy | Đổi thành 30 phút (C5), chỉ đổi cấu hình |
| Hình thức thanh toán (H10) | Trước đây Normal chỉ cho Member trả VNPay; Cash chỉ Lễ tân đăng ký thay | **Đã chốt:** cả Normal và Advanced đều cho Member chọn VNPay hoặc Cash. Cash thì ra quầy, Lễ tân xác nhận |
| Hạn 24 giờ tạo đơn Advanced (H11) | Coach chấp nhận xong, Member có 24 giờ tạo đơn | Giữ. Thêm xử lý hóa đơn hết hạn: tạo hóa đơn mới nếu còn trong 24 giờ (L28) |

### Nhóm D: Tự động sau buổi học
| Khóa | Hiểu đơn giản | Đề xuất |
|---|---|---|
| Tự ghi Vắng và chặn đặt lịch (H7) | Hết buổi mà chưa điểm danh thì ghi Vắng; vắng 3 lần bị chặn đặt lịch 7 ngày | Giữ. Dùng Member demo riêng, nhớ điểm danh trước khi buổi kết thúc |

## 3. Khóa mới đã thêm sau khi viết bản gốc: 7 ca cố định
Buổi học chỉ xếp được theo 7 ca chuẩn 90 phút (06:00, 07:45, 09:30, 13:00, 14:45, 16:30, 18:15). Môn ngoài trời không có ca 3 và ca 4. Nhóm đã chốt giữ nguyên. Hệ quả cho demo: buổi đầu của lớp mới phải bắt đầu **sau** thời điểm hiện tại. Demo sau 18:15 thì ca sớm nhất là sáng hôm sau, nên điểm danh phải chờ. Chi tiết ở `scms-quyet-dinh-7-ca-hoc-co-dinh.md`.

## 4. Giá trị mới đề xuất (cần nhóm xác nhận)
| Tham số | Cũ | Mới |
|---|---|---|
| Đóng đăng ký trước buổi đầu (C1) | 48 giờ | 0 giờ |
| Khung giờ điểm danh (C3) | Đúng giờ ca | Cả ngày của buổi |
| Điều kiện điểm danh Normal (C2) | Lớp `Ongoing` | Manager đã xác nhận khai giảng là đủ |
| Đặt buổi 1-1 trước giờ học (C4) | 12 giờ | 0 giờ, không âm |
| Hóa đơn giữ (C5) | 5 phút | 30 phút |
| Tùy chọn: Manager khai giảng đến hết buổi đầu (C11) | không | 90 phút |
| Tùy chọn: mã QR sống tối đa (C13) | 30 giây | 120 giây |
| Tùy chọn: tắt tự ghi Vắng khi demo (C12) | bật | bật hoặc tắt |

## 5. Hai kịch bản demo sau khi áp dụng
**Normal (trong một buổi, trước 18:15):** Manager tạo lớp, chọn ca còn lại trong ngày, sĩ số tối đa 1 đến 4 → Member đăng ký (VNPay hoặc Cash qua Lễ tân) → Manager xác nhận khai giảng → Coach điểm danh ngay → hết buổi hệ thống tự ghi Vắng cho người chưa điểm danh → Coach sửa có lý do.

**Advanced:** Member gửi yêu cầu → Coach chấp nhận → Member tạo đơn (VNPay hoặc Cash) → Lễ tân thu → Coach đề xuất buổi (ca chưa bắt đầu) → Member chấp nhận → Coach điểm danh. Nếu demo muộn trong ngày, dùng buổi 1-1 dựng sẵn.

## 6. Cần nhóm quyết định
1. Đồng ý các giá trị ở mục 4 chưa (nhất là 0 giờ, điểm danh cả ngày, 30 phút).
2. Có làm các mục tùy chọn C11, C12, C13 và ping giữ máy chủ thức không.
3. Demo muộn trong ngày (sau 18:15) thì chọn cách nào: lớp dựng sẵn hay chấp nhận chỉ trình bày đến bước khai giảng.

## 7. Việc khác phải xong trước demo (không phải đổi quy tắc)
- Sửa L01 (điểm danh Coach đổi lớp bị hỏng), L25 (Lễ tân đăng ký thay ra hóa đơn VNPay), L28 (hóa đơn Advanced hết hạn bị kẹt, cần backend).
- Dọn dữ liệu thử cũ.

→ 
A.
Mã QR sống 1 đến 30 giây (H8) → 300 giây (5 phút)
Duyệt toàn bộ đề xuất còn lại

B.
Duyệt toàn bộ đề xuất

C.
Duyệt toàn bộ đề xuất

D. OK

4. 
Duyệt toàn bộ đề xuất

5.
Cần đề xuất lại để không cần "trong một buổi, trước 18:15" để demo bất kỳ thời điểm trong ngày