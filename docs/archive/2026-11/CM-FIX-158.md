> **LƯU TRỮ:** nội dung theo thời điểm ghi trong bản này; trạng thái hiện hành xem [Active](../../00-MASTER-CHECKLIST.md#active). Lưu trữ không có nghĩa mọi luồng đã nghiệm thu.

Tôi đã xác nhận từ controller/service, DTO và OpenAPI hiện tại. **Ba API đã hỗ trợ; không cần sửa BE cho CM-FIX-15B.** Nội dung đầy đủ đã bổ sung tại [contract CM-FIX-15B](../../01-API-CONTRACT-FE-BE.md).

| POST | Nguồn `version` |
|---|---|
| `/api/classes/{id}/normal-proposals` | `SportsClassResponse.lifecycleVersion` từ GET lớp |
| `/api/normal-proposals/{id}/apply` | `NormalChangeProposalView.version` của đúng proposal từ GET danh sách proposal |
| `/api/classes/{id}/cancel-before-opening` | `SportsClassResponse.lifecycleVersion` từ GET lớp mới nhất |

**Điểm quan trọng:** apply tăng `lifecycleVersion` của lớp nhưng **không tăng `proposal.version`**. Vì vậy, không lấy version proposal để hủy lớp sau apply.

Các giá trị phân biệt hoa/thường:

- `kind`: **`Postpone`**, **`Transfer`**. Postpone gửi `shiftDays` nguyên 1–366; Transfer gửi `targetClassId`.
- `proposal.status`: **`Pending`**, **`Applied`**, **`Cancelled`**. FE không gửi `status` để điều khiển các POST này.
- `choices[].choice`: **`Waiting`**, **`Accept`**, **`Refund`**. Member chỉ POST `Accept` hoặc `Refund`; Manager không chọn thay Member.

Cả ba POST yêu cầu **CenterManager Active + `MANAGE_CLASSES`, cùng center**. Admin chỉ có authority này vẫn không thay role Manager. Thao tác mới yêu cầu lớp Normal `Open`, đã công bố và chưa bắt đầu buổi đầu. `reason` bắt buộc, không trắng, tối đa 2.000 ký tự khi **propose/cancel**; apply chỉ cần `version`.

Khi còn Member `Waiting`:

- Apply trả **400**, `code: "MEMBER_RESPONSES_PENDING"` sau khi version/lịch hợp lệ.
- Hết Waiting có thể gồm cả `Accept` và `Refund`; không yêu cầu tất cả Accept.
- Cancel là quyết định trung tâm hủy riêng, được xử lý dù còn Waiting; không biến Waiting thành Accept. FE không tự gọi cancel làm fallback khi apply lỗi.
- Apply không tự khai giảng. Với Transfer, quyền đã chuyển khi từng Member Accept; lớp gốc vẫn `changePending=true` sau apply.

Ví dụ dưới đây dùng **ID/version giả định**, response GET chỉ trích field cần thiết:

```http
GET /api/classes/101
→ {"id":101,"status":"Open","changePending":false,"lifecycleVersion":7}

POST /api/classes/101/normal-proposals
{
  "kind": "Transfer",
  "targetClassId": 202,
  "version": 7,
  "reason": "DEMO: trung tâm đề nghị phương án thay thế"
}
```

Proposal mới có thể là `id=9001, version=8`. Đọc lại trước apply:

```http
GET /api/classes/101/normal-proposals
→ [{
    "id":9001,
    "classId":101,
    "kind":"Transfer",
    "status":"Pending",
    "version":8,
    "choices":[
      {"memberId":11,"choice":"Accept"},
      {"memberId":12,"choice":"Refund"}
    ]
  }]

POST /api/normal-proposals/9001/apply
{"version":8}
```

Sau apply, proposal vẫn version8, còn lớp đã version9:

```http
GET /api/classes/101
→ {"id":101,"status":"Open","changePending":true,"lifecycleVersion":9}

POST /api/classes/101/cancel-before-opening
{
  "version": 9,
  "reason": "DEMO: trung tâm quyết định hủy lớp trước buổi đầu"
}
```

Dạng lỗi chính xác hiện tại:

| Trường hợp | HTTP | Field lỗi |
|---|---:|---|
| Propose/cancel dùng version lớp cũ | **409** | `message: "NORMAL_VERSION_STALE"`; **không có `code`** |
| Apply proposal Pending dùng version cũ hoặc lịch/fingerprint đã lệch | **409** | `code: "NORMAL_PROPOSAL_STALE"` |
| Apply còn Waiting | **400** | `code: "MEMBER_RESPONSES_PENDING"` |

Handler legacy của `NORMAL_VERSION_STALE` còn trả **`body.status=400` dù HTTP thực là409**. FE lấy **HTTP status thực** và đọc `code ?? message`; tải lại lớp/proposal, không tự cộng version rồi retry.

Ngoại lệ retry: proposal đã `Applied` hoặc lớp đã `Cancelled` có thể trả200 trước kiểm version cũ. Xác nhận lần này dựa trên source; chưa có kiểm HTTP/browser mới.