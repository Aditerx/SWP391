MỤC TIÊU PHỤ:

Giúp tôi kiểm tra và đánh giá: theo PROJECT_CONTEXT.md, các mục tiêu sau có đúng không, tôi hiện đang thiếu/chưa thiết kế hay có gì chưa đúng và đề xuất giải pháp:

1. Khóa học = lớp học (class) trong phạm vi demo. subject là môn; class là khóa/lớp Member đăng ký; session là từng buổi để điểm danh. Dùng classId cho ghi danh và feedback. Cách này khớp mô hình BE hiện có, tránh thêm một thực thể khóa học ngay trước demo.

2. Thanh toán đi từ hóa đơn và do BE xác nhận. Tiền mặt: Receptionist xác nhận thanh toán trên hóa đơn test. Online: dùng cổng sandbox mà BE đã tích hợp, BE nhận và xác minh kết quả rồi cập nhật hóa đơn. FE chỉ hiển thị Paid sau khi đọc lại trạng thái từ BE. Nếu BE chưa có cổng sandbox/callback, cần báo ngay cho nhóm: không thể nghiệm thu thanh toán online thật bằng một màn QR hoặc nút “thành công” tự tạo ở FE.

3. Một lần feedback cho lớp và mentor. Member đã tham gia lớp gửi classId, coachId, điểm lớp, điểm mentor và nhận xét; BE lưu và cung cấp GET để xem lại sau F5. API evaluations hiện có chưa chứa classId hoặc điểm lớp, nên BE cần xác nhận mở rộng contract hoặc cấp endpoint feedback riêng. FE sẽ theo contract được chốt, không tự suy ra feedback khóa học từ đánh giá HLV.