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
│   └── 05_migration_notifications.sql   # Mở rộng thông báo tương thích ngược
└── archive/                           # Lưu trữ lịch sử (Không dùng trong môi trường live)
    ├── legacy_mssql_20260926.sql           # Schema cũ thời kỳ MS SQL Server
    ├── legacy_postgres_backup_20261003.sql # Bản backup Postgres ngày 03/10/2026
    └── flow1_steps/                        # 8 file migration lẻ của Flow 1 đã hoàn tất
```

---

## 2. Hướng Dẫn Chạy Migration Mới Nhất Trên Supabase

Chạy migration theo thứ tự trên DB mới. DB đã áp dụng migration 01–04 chỉ cần chạy migration 05.

### File cần chạy trên Supabase SQL Editor:
👉 `database/migrations/05_migration_notifications.sql`

Migration 05 chỉ thêm cột và index vào `notifications` / `notification_recipients`; không xóa, đổi tên hay ghi đè dữ liệu cũ. Loại thông báo nghiệp vụ được lưu ở `notification_type` để giữ nguyên CHECK constraint và cột `type` legacy. Trạng thái đọc của người nhận nằm ở `notification_recipients`.

### Các nội dung cập nhật trong file 04:
1. **`subjects`**: Thêm cột `normal_price`, `advanced_price` và seed bảng giá chuẩn 9 môn thể thao theo trang 7 `Member.pdf`.
2. **`rooms`**: Thêm cột `facility_type` (`'Indoor'`, `'Outdoor'`) để áp dụng ràng buộc thời tiết chống nắng nóng.
3. **`classes`**: Thêm `class_type` (`'Normal'`, `'Advanced'`), `total_sessions` (default 12), và cập nhật CHECK constraint `chk_class_capacity` (Normal $\le$ 15, Advanced = 1).
4. **`membership_packages`**: Thêm `package_type` (`'Normal'`, `'Advanced'`), `total_credit` (default 12).
5. **`member_packages`**: Thêm `remaining_credit` (default 12), `validity_days` (default 45).
6. **`sessions`**: Thêm `booked_member_id` và `coach_id` để hỗ trợ ca học 1:1 linh hoạt.
7. **`holidays`**: Tạo bảng lưu danh sách ngày lễ có `tag` (`NATIONAL_FIXED`, `LUNAR_FESTIVAL`, `CENTER_EVENT`) và `is_active` để thuật toán sinh lịch tự động nhảy qua ngày lễ.
8. **Seed 9 lớp Template 1:1**: Tự động sinh sẵn 9 lớp học Advanced mẫu cho 9 môn thể thao.

