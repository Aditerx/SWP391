# Hướng Dẫn Quản Lý & Vận Hành Database SCMS

Thư mục này quản lý toàn bộ cấu trúc CSDL PostgreSQL (Supabase) của hệ sinh thái Sports Center Management System (SCMS).

---

## 1. Cấu Trúc Thư Mục

```text
database/
├── schema_postgres.sql                # Schema tổng thể (Full Master Schema) mới nhất
├── README.md                          # Tài liệu hướng dẫn cấu trúc database
├── migrations/                        # Các bản migration tăng dần theo thứ tự thời gian
│   ├── 01_migration_dynamic_permissions.sql
│   ├── 02_migration_class_feedback.sql
│   ├── 03_migration_payment.sql
│   ├── 04_migration_member_flow_v2.sql
│   ├── 05_migration_notifications.sql   # Mở rộng thông báo tương thích ngược
│   ├── 06_migration_chat.sql            # Chat hỗ trợ member và nhân viên
│   ├── 06_schedule_safety_postgres.sql  # Schedule mutex, không bị chat thay thế
│   ├── 06_schedule_safety_mssql.sql     # Biến thể SQL Server, không chạy trên PostgreSQL
│   └── 07_session_subscription_postgres.sql # F-06/F-07: liên kết Session–subscription
└── archive/                           # Lưu trữ lịch sử (Không dùng trong môi trường live)
    ├── legacy_mssql_20260926.sql           # Schema cũ thời kỳ MS SQL Server
    ├── legacy_postgres_backup_20261003.sql # Bản backup Postgres ngày 03/10/2026
    └── flow1_steps/                        # 8 file migration lẻ của Flow 1 đã hoàn tất
```

---

## 2. Hướng Dẫn Chạy Migration Mới Nhất Trên Supabase

Đối chiếu lịch sử DB theo **tên file đầy đủ**, không chỉ số prefix: hai file 06 chat/schedule safety là hai nội dung độc lập; migration 07 vẫn cần cho Session–subscription. DB mới trống cần khởi tạo schema nền và fixture theo kế hoạch riêng; migration nâng cấp không thay schema nền. Hai bản schema_postgres.sql root/database được đồng bộ sau merge. Lượt xử lý conflict chưa chạy các migration mới trên DB.

### File cần chạy trên Supabase SQL Editor:
👉 `database/migrations/06_migration_chat.sql`

Migration 06 tạo các bảng chat và hai quyền `CHAT_AS_MEMBER`, `HANDLE_CHAT`; không xóa, đổi tên hay ghi đè dữ liệu cũ. Migration an toàn khi chạy lại. `HANDLE_CHAT` chỉ được gán mặc định cho Receptionist và CenterManager; Coach không có quyền.

### Các nội dung cập nhật trong file 04:
1. **`subjects`**: Thêm cột `normal_price`, `advanced_price` và seed bảng giá chuẩn 9 môn thể thao theo trang 7 `Member.pdf`.
2. **`rooms`**: Thêm cột `facility_type` (`'Indoor'`, `'Outdoor'`) để áp dụng ràng buộc thời tiết chống nắng nóng.
3. **`classes`**: Thêm `class_type` (`'Normal'`, `'Advanced'`), `total_sessions` (default 12), và cập nhật CHECK constraint `chk_class_capacity` (Normal $\le$ 15, Advanced = 1).
4. **`membership_packages`**: Thêm `package_type` (`'Normal'`, `'Advanced'`), `total_credit` (default 12).
5. **`member_packages`**: Thêm `remaining_credit` (default 12), `validity_days` (default 45).
6. **`sessions`**: Thêm `booked_member_id` và `coach_id` để hỗ trợ ca học 1:1 linh hoạt.
7. **`holidays`**: Tạo bảng lưu danh sách ngày lễ có `tag` (`NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`) và `is_active` để thuật toán sinh lịch tự động nhảy qua ngày lễ.
8. **Seed 9 lớp Template 1:1**: Tự động sinh sẵn 9 lớp học Advanced mẫu cho 9 môn thể thao.

