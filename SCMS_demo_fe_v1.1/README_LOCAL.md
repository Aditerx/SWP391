# SCMS: chạy trong VS Code

Mở thư mục chứa package.json bằng File > Open Folder. Không chép đè repo cũ.

Môi trường kiểm tra: Node 24.19.0, pnpm 11.19.0.
Đã chạy thành công pnpm install --frozen-lockfile và pnpm build:local.
Build còn cảnh báo bundle JavaScript lớn hơn 500 kB; chưa tối ưu chia bundle.

```powershell
node --version
pnpm --version
pnpm install --frozen-lockfile
pnpm dev:local
```

Nếu chưa có pnpm: `npm install -g pnpm@11.19.0`.
Mở http://localhost:5173. Dừng bằng Ctrl+C.

Tài khoản trình diễn:

- Email: `manager@scms.com`
- Mật khẩu: `12345678`

```powershell
pnpm build:local
pnpm preview:local
```

Cấu hình vite.local.config.js phục vụ chạy ngoài Figma; cấu hình Figma gốc vẫn giữ.
Ứng dụng dùng React JSX, Tailwind 4 và Context. `dev:local` dùng dữ liệu mẫu;
luồng `dev` tích hợp session backend cho các màn Center Manager đợt 1.
Mật khẩu form thêm nhân viên chỉ là kiểm tra đầu vào prototype, không tạo thông tin xác thực.

## Thay đổi
- Bắt đầu prototype từ màn hình đăng nhập Quản lý trung tâm.
- Bỏ đăng ký tài khoản công khai và đăng nhập nhanh theo vai trò khỏi luồng demo đầu tiên.
- Hoàn thiện form thêm thành viên: validation, tóm tắt gói, trạng thái đang tạo và cảnh báo khi hủy.
- Sau khi tạo thành viên, tự mở trang chi tiết của hồ sơ vừa tạo ở trạng thái Chờ thanh toán.
- Bổ sung empty state cho danh sách thành viên/nhân viên và xác nhận trước khi khóa tài khoản.
- Không cho tài khoản Quản lý đang đăng nhập tự khóa chính mình.
- Bỏ fallback mở hồ sơ người đầu tiên khi không tìm thấy memberId.
- Bỏ breadcrumb lặp, làm gọn ô thông tin cá nhân, chỉnh nhãn Việt/Anh.
- Mount modal theo trạng thái mở để làm mới form khi mở lại.
- Bỏ blur của modal chi tiết/thêm nhân viên, làm rõ placeholder mật khẩu.
- Xóa chuyên môn khi chuyển sang Lễ tân; chặn đóng form nhân viên trong lúc lưu.
- Chuẩn hóa kiểu chữ: dùng font monospace cho mã kỹ thuật, dùng số tabular cho ngày, giờ, tiền và số điện thoại.
- Chuẩn hóa tiền tệ: tiếng Việt hiển thị `3.200.000 ₫`, tiếng Anh hiển thị `3,200,000 VND`.
- Chuẩn hóa ngày: tiếng Việt `DD/MM/YYYY`, tiếng Anh `DD Mon YYYY`; ngày giờ giao dịch và nhật ký cũng dùng cùng quy tắc.
- Hoàn thiện nhãn VI/EN cho các màn hình Manager chính, gồm Nhân viên, Thành viên, Gói thành viên, Lớp học, Thanh toán, Báo cáo, Nhật ký và Cài đặt.
- Đổi trạng thái `grace_period` thành “Đang ân hạn” để phân biệt với thao tác gia hạn gói.
- Căn lại bảng Thành viên/Nhân viên/Dashboard, tăng cỡ chữ dữ liệu phụ và cố định vùng thao tác.
- Sửa lịch tháng 09/2026 theo đúng thứ trong tuần, hiển thị đủ ngày 1–30 và bản dịch tên lớp.
- Sửa biểu đồ hội viên mới để số liệu theo tháng không bị hiểu nhầm là tổng tích lũy.

## Chưa hoàn thiện
- Toàn bộ dữ liệu và thao tác hiện tại là mock phục vụ demo giao diện, chưa gọi API.
- Một số ngày, mã hồ sơ và trạng thái được mô phỏng để trình bày prototype.
- Chưa kiểm thử trình duyệt, focus trap, Escape và mọi luồng nghiệp vụ.
- Kiểm tra thủ công: đăng nhập; tạo thành viên và mở hồ sơ vừa tạo; tìm kiếm/lọc;
  sửa/hủy/mở lại form; tạo Coach và Receptionist; email trùng; đổi ngôn ngữ; tab Gói đăng ký.
