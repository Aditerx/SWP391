# Kịch bản thao tác demo SCMS — F1–F5

Dành cho người trình diễn; bản12/11/2026 để Owner duyệt. **11phút trình diễn + 4 phút hỏi đáp**, dự kiến trong khung 17:45–18:15 VN. Thực hiện theo thứ tự **F1 → F2 → F3 → F4 → F5**.

Tên màn sau đăng nhập bên dưới mô tả chức năng; chưa xác minh toàn bộ nhãn/nút trên FE. Chỉ thao tác ghi đã rehearsal đúng case; phần còn lại xem hồ sơ/kết quả chuẩn bị sẵn. Đây là kế hoạch, chưa là biên bản demo thành công. Toàn bộ các phần nói sơ sẽ làm luôn nếu được yêu cầu.

## Chuẩn bị trước khi bắt đầu

- [ ] Mở [VibeSports](https://vibesports.pages.dev/login); mỗi vai dùng một profile/browser riêng, không chỉ đổi tab cùng phiên.
- [ ] Mở sẵn tài khoản, tên lớp/hồ sơ và văn bản cần copy trong [note private](artifacts/runs/demo-15min/2026-11-12/demo-copy-paste.private.md); không trình chiếu file này.
- [ ] Chọn một Member để đăng ký/thanh toán, một hồ sơ Refund riêng, một buổi học/Coach đúng phân công và một hội thoại DEMO.
- [ ] Có Invoice Paid/Enrollment Registered và lịch sử Refund/attendance/chat dự phòng đã đọc lại; chưa có thì dùng sơ đồ và nói rõ chưa chứng minh live.
- [ ] Mở mẫu mail OTP đã nhận. Chuẩn bị [F1-v2](image/F1-v2.png), [F3-v3](image/F3-v3.png) để giải thích khi cần.
- [ ] Chạy thử với đồng hồ; các bước kiểm ID/phiên/điều kiện thuộc [agent/BE](docs/02-DEPLOYMENT-GUIDE.md#demo-tech-check-20261112), không bắt người trình diễn tự tìm tham số kỹ thuật.

## Mở đầu

1. Nói: “SCMS hỗ trợ vận hành trung tâm thể thao với năm vai: Member, Coach, Receptionist, Center Manager và Admin.”
2. Nói: “Phần trình bày dùng dữ liệu DEMO và thanh toán VNPay sandbox.”
3. Chuyển ngay sang màn đăng nhập; không đọc ERD hoặc toàn bộ bảng quyền.

## F1 — Tài khoản, đăng nhập và hồ sơ

1. **Visitor:** tại màn login, chỉ nút “Đăng ký hội viên”. Nếu form đã rehearsal, mở để giới thiệu các thông tin đăng ký; không tạo tài khoản mới chỉ để chờ email.
2. **Visitor/Member:** mở mẫu email OTP và kết quả xác minh đã chuẩn bị. Nói: “OTP hợp lệ kích hoạt tài khoản; sau đó đăng nhập.” Không dùng OTP cũ để thử hoặc nói verify tự đăng nhập.
3. **Google:** chỉ nút/nhánh Google và nói có đăng nhập google.
4. **Receptionist:** mở hồ sơ Member được tạo tại quầy hoặc màn tạo đã chuẩn bị. Nêu cấp **mật khẩu tạm**, buộc đổi lần đăng nhập đầu; không gọi mật khẩu tạm là OTP.
5. **Member:** mở hồ sơ cá nhân và feed Notifications có sẵn. Chỉ một thông báo, nêu đây là thông báo trên website; email OTP/cấp mật khẩu là luồng riêng.
6. **Manager/Admin, lướt nhanh nếu còn giờ:** cho thấy Manager quản lý Member/Staff trong center; Admin có đăng nhập/quản trị riêng theo quyền.

**Đối chiếu:** phân biệt tài khoản Active với phiên đăng nhập; không giới thiệu gói tháng/VIP hoặc Freeze Request từ thiết kế cũ. Chưa có kết quả thật thì dùng sơ đồ để giải thích, không nhận là vừa đăng ký thành công.

## F2 — Lớp học, đăng ký và quản lý lịch

1. **Manager:** mở một lớp Normal đã công bố; chỉ môn, Coach, phòng, lịch 12 buổi và điều kiện nhận học viên. Phân biệt lớp Open để đăng ký với xác nhận khai giảng/Ongoing.
2. **Manager:** lướt danh sách ngày nghỉ/sự cố đã có. Không tạo Holiday diện rộng hoặc sửa nhiều lịch trong lượt demo.
3. **Manager:** mở một hồ sơ dời khai giảng/chuyển lớp đã chuẩn bị; chỉ lịch đề xuất và phản hồi học viên. Nêu cần học viên phản hồi đồng ý, im lặng chưa là đồng ý. Nếu có case thao tác đạt rehearsal thì chỉ thực hiện một bước đã chọn; nếu không, xem lịch sử.
4. **Member:** chỉ phản hồi/thông báo liên quan trên website của đúng tài khoản. Nêu khi phát sinh nghĩa vụ hoàn tiền sẽ xử lý ở F3; chưa chạy Refund tại đây.
5. **Coach, lướt nếu đã mở sẵn:** chỉ lịch được phân công; không đổi Coach hoặc tạo thêm ca để mở rộng demo.
6. **Member:** cuối phần F2, mở lớp khác còn chỗ/còn hạn, chọn đăng ký bằng VNPay. Tạo đơn **ngay lúc này**, kiểm tên lớp và số tiền hệ thống trả về, rồi chuyển thẳng F3.

**Đối chiếu:** đơn VNPay mới còn Pending chưa cấp quyền học; chưa đủ căn cứ thì không nói Member đã ghi danh thành công. Case dời/hủy chuẩn bị riêng có thể khác lớp vừa đăng ký, phải nói rõ.

## F3 — Thanh toán, hoàn tiền và báo cáo

1. **Member:** từ đơn mới cuối F2, mở VNPay **SANDBOX**, đối chiếu số tiền và thanh toán bằng bộ test đã chuẩn bị. Không dùng link Pending cũ.
Ngân hàng: NCB
Số thẻ: 9704198526191432198
Chủ thẻ: NGUYEN VAN A
Ngày phát hành: 07/15
OTP test: 123456
2. **Member:** sau Return, đọc lại hóa đơn và ghi danh; chờ BE xác minh. Chỉ nói thành công khi **Paid hợp lệ, không cần đối soát và Enrollment Registered**, sau tải lại vẫn còn. URL báo thành công chưa đủ.
3. **Receptionist:** chỉ màn/kết quả Cash chuẩn bị sẵn. Nói: “Normal tại quầy thu tiền và tạo Paid/Registered trong cùng giao dịch; Advanced mới có bước xác nhận hóa đơn Cash Pending hợp lệ.” Không tạo thêm đơn Cash cho lượt vừa trả VNPay.
4. **Manager:** mở một hồ sơ **Refund Normal đã chuẩn bị**, xem lịch sử; dùng hồ sơ này giải thích các vai ở bước 5–7, không chạy lại cả chuỗi trong vài chục giây. Chỉ nguồn phát sinh: trung tâm hủy lớp, Member chọn hoàn khi không nhận đề xuất dời/chuyển, hoặc tiền đến sau khi lớp đã hủy.
5. Chỉ phần **Member/Invoice/lớp/số tiền** lấy từ chứng từ gốc và phần **ngân hàng/số tài khoản/tên người nhận** do Member cung cấp cho hồ sơ hoàn. Hai nhóm thông tin này khác nhau; không nói tự trích tài khoản ngân hàng từ hồ sơ Member.
6. Chỉ hai giai đoạn trước chuyển tiền: **Manager xác minh người nhận và duyệt khoản hoàn** → **Receptionist nhận việc**. Nói rõ Receptionist không duyệt hoàn lần hai.
7. Chỉ phần thực hiện/đối soát: **Receptionist thực hiện chuyển khoản ngoài SCMS, ghi mã/số tiền/thời điểm và nộp chứng từ** → **Manager đối chiếu nguồn ngân hàng độc lập** → khớp/thành công mới **Completed**. Chưa rõ thì NeedsReview; duyệt hoặc upload chưa là hoàn tất. Demo dùng chứng cứ mô phỏng, không chuyển tiền thật.
8. **Manager:** nếu đã rehearsal một bước cuối đúng trạng thái thì thực hiện bước đó và tải lại; nếu không, cho xem kết quả/lịch sử có sẵn. Chỉ Invoice vẫn Paid và amount gốc được giữ để đối chiếu.
9. **Manager:** lướt báo cáo thu/hoàn/thu ròng đã chuẩn bị; chỉ khoản Completed mới tính đã hoàn. Cắt phần lướt báo cáo nếu thanh toán mất thời gian.

**Dự phòng:** VNPay quá 20–30giây chưa có kết quả thì dừng chờ. Nói: “Kết quả lần này chưa xác minh xong; Tôi chuyển sang giao dịch DEMO đã kiểm trước.” Chỉ dùng kết quả đã đọc lại, không kết luận thất bại do timeout, không tạo thêm đơn/trả lại khi khoản cũ chưa rõ.

## F4 — Lịch học, attendance và kết quả tập

1. **Receptionist:** mở đúng buổi học và danh sách học viên. Kiểm giờ VN thực tế trước khi ghi: ca6 kết thúc 18:00; ca7 bắt đầu 18:15.
2. **Receptionist:** nếu buổi đang trong cửa giờ và đủ quyền/điều kiện, ghi check-in/attendance theo bước đã rehearsal cho Member A. Mở lại để chỉ kết quả đã lưu; không gọi check-in là hoàn tất kết quả chuyên môn.
3. **Coach:** chuyển sang buổi được phân công; ghi nhận xét/attendance cho Member B hoặc trường dữ liệu khác theo case đã chọn. Tránh hai vai ghi đè cùng trạng thái hoặc xóa nhận xét của nhau chỉ để demo.
4. **Member:** mở lịch, attendance và kết quả của chính mình; tải lại để đối chiếu nhận xét/trạng thái đã lưu.
5. **Coach/Member:** lướt giáo án/tiến bộ có sẵn nếu còn giờ. Không giới thiệu Body Metrics hoặc thao tác sau buổi chưa xác minh trên bản FE/BE đang chạy.

**Ngoài cửa giờ:** thay bước 2–3 bằng xem lịch sử. Nói: “Tôi dùng kết quả đã lưu để minh họa; không ghi attendance mới lúc này.” Ví dụ bắt đầu 17:45 thì F4 khoảng 17:52:30–17:54:30; bắt đầu18:00 thì F4 ở 18:07:30–18:09:30, dùng lịch sử. Không đổi Clock.

## F5 — AI và nhân viên hỗ trợ

1. **Member:** mở hội thoại DEMO; hỏi một câu ngắn về đăng ký/lịch học từ note copy.
2. **Member:** cho xem phản hồi AI; nếu quá10giây thì dùng hội thoại đã có và nói rõ là kết quả trước, không gửi lặp.
3. **Member:** yêu cầu người hỗ trợ theo luồng đã rehearsal. Không diễn giải là chọn tùy ý Receptionist nhận tin.
4. **Receptionist:** mở đúng hội thoại DEMO, nhận việc nếu chưa có người nhận, trả lời ngắn; quay về Member để thấy phản hồi trong **cùng hội thoại**.
5. **Receptionist:** chỉ đóng hội thoại nếu thao tác này đã được rehearsal và còn thời gian; không gửi tin tới staff thật ngoài người đóng vai DEMO.

**Đối chiếu:** phân biệt AI trả lời với Receptionist nhận việc/trả lời; giữ lịch sử hội thoại, không coi ảnh flow là bằng chứng gửi tin thành công.

## Kết

1. Nói: “Ba điểm chính là đúng vai/phạm vi, Paid hợp lệ mới cấp quyền học, và kết quả được lưu để đọc lại.”
2. Dừng thao tác; chuyển sang hỏi đáp. Nếu chậm, bỏ tour Admin/chi tiết giáo án trước, giữ ví dụ thanh toán/readback và tương tác vai.

## Hỏi đáp

1. Mở hồ sơ hoặc sơ đồ có sẵn theo câu hỏi, không bắt đầu luồng ghi dài mới.
2. Với notification: thông báo nghiệp vụ xem trên website; email OTP/cấp mật khẩu là kênh riêng, không hứa email cho mọi thay đổi.
3. Với Refund: Manager **duyệt trước**, Receptionist **thực hiện và nộp chứng cứ**, Manager **đối soát sau**. Chuyển tiền ở ngân hàng ngoài SCMS; Completed là kết quả ghi nhận sau đối soát.
4. Kết thúc khi đủ 15 phút; phần chưa rehearsal nói rõ chưa kiểm.
