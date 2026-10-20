# ==============================================================================
# SCRIPT TEST RIÊNG MÃ QR THANH TOÁN & MÃ TOTP 30S (KHÔNG CẦN QUA FE)
# Hỗ trợ test độc lập trên Local hoặc Render
# Cách chạy:
#   .\scripts\test-payment-and-totp.ps1 -BaseUrl "http://localhost:8080"
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MemberEmail = "oanh.hoang@fitzone.vn",
    [string]$MemberPassword = "password123",
    [string]$CoachEmail = "huong.tran@fitzone.vn",
    [string]$CoachPassword = "password123"
)

$ErrorActionPreference = "Continue"

Write-Host "`n======================================================================" -ForegroundColor Yellow
Write-Host " [SCMS] TEST ĐỘC LẬP: MÃ QR THANH TOÁN - MÃ ĐIỂM DANH TOTP 30 GIÂY" -ForegroundColor Yellow
Write-Host " Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Yellow

$memberSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$coachSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# ------------------------------------------------------------------------------
# PHẦN 1: TEST MÃ CHECK-IN TOTP 30 GIÂY
# ------------------------------------------------------------------------------
Write-Host "`n--- [PHẦN 1] TEST MÃ TOTP CHECK-IN ĐỔI MỖI 30 GIÂY ---" -ForegroundColor Cyan

# 1.1 Đăng nhập Member
Write-Host "1. Đang đăng nhập tài khoản Member ($MemberEmail)..." -NoNewline
try {
    $loginBody = @{ email = $MemberEmail; password = $MemberPassword } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $memberSession
    Write-Host " [OK] Đăng nhập thành công!" -ForegroundColor Green
} catch {
    Write-Host " [LỖI] Đăng nhập thất bại: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "    (Gợi ý: Kiểm tra lại mật khẩu của $MemberEmail hoặc chạy đăng ký tài khoản mới)" -ForegroundColor Yellow
}

# 1.2 Lấy mã TOTP lần 1
Write-Host "`n2. Member yêu cầu mã TOTP check-in qua GET /api/attendance/checkin-code..."
try {
    $totp1 = Invoke-RestMethod -Uri "$BaseUrl/api/attendance/checkin-code" -Method Get -WebSession $memberSession
    Write-Host "    ==> MÃ TOTP HIỆN TẠI : " -NoNewline
    Write-Host "$($totp1.code)" -ForegroundColor Green -BackgroundColor Black
    Write-Host "    ==> THỜI GIAN HIỆU LỰC: $($totp1.remainingSeconds) giây còn lại" -ForegroundColor Yellow

    Write-Host "`n3. Kiểm tra tính năng làm mới mã sau 30 giây:"
    Write-Host "    Đang chờ 5 giây để quan sát thời gian đếm ngược..."
    Start-Sleep -Seconds 5
    $totp2 = Invoke-RestMethod -Uri "$BaseUrl/api/attendance/checkin-code" -Method Get -WebSession $memberSession
    Write-Host "    ==> Sau 5 giây, thời gian còn lại: $($totp2.remainingSeconds) giây (Mã vẫn giữ nguyên: $($totp2.code))" -ForegroundColor Cyan
} catch {
    Write-Host "    [LỖI] Không lấy được mã TOTP: $($_.Exception.Message)" -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# PHẦN 2: TEST MÃ QR THANH TOÁN (VNPAY & VIETQR)
# ------------------------------------------------------------------------------
Write-Host "`n--- [PHẦN 2] TEST MÃ QR THANH TOÁN (VNPAY SANDBOX - VIETQR) ---" -ForegroundColor Cyan

# 2.1 Test tạo URL cổng VNPay Sandbox
Write-Host "1. Sinh link thanh toán VNPay Sandbox có chứa VNPAY-QR:"
try {
    # Lấy danh sách hóa đơn của Member
    $invoices = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/invoices" -Method Get -WebSession $memberSession
    $targetInv = $invoices | Where-Object { $_.status -match "Pending" } | Select-Object -First 1

    if (-not $targetInv -and $invoices.Count -gt 0) {
        $targetInv = $invoices[0]
    }

    if ($targetInv) {
        $payRes = Invoke-RestMethod -Uri "$BaseUrl/api/invoices/$($targetInv.id)/pay-online" -Method Post -WebSession $memberSession
        Write-Host "    [OK] Đã sinh link thanh toán cho Hóa đơn #$($targetInv.id) (Số tiền: $($targetInv.amount) VNĐ):" -ForegroundColor Green
        Write-Host "    ------------------------------------------------------------------"
        Write-Host "    $($payRes.paymentUrl)" -ForegroundColor Blue
        Write-Host "    ------------------------------------------------------------------"
        Write-Host "    HƯỚNG DẪN TEST TRÊN TRÌNH DUYỆT:" -ForegroundColor Yellow
        Write-Host "    1. Copy toàn bộ đường link trên và dán vào Chrome / Edge."
        Write-Host "    2. Giao diện VNPay Sandbox sẽ xuất hiện MÃ QR VNPAY-QR to ở giữa màn hình."
        Write-Host "    3. Bạn có thể dùng App Ngân hàng quét thử (nó sẽ nhận diện đúng số tiền)."
        Write-Host "    4. Để test thanh toán thành công, chọn Thẻ nội địa -> Ngân hàng NCB:"
        Write-Host "       - Số thẻ: 9704198526191432198"
        Write-Host "       - Tên: NGUYEN VAN A"
        Write-Host "       - Ngày: 07/15"
        Write-Host "       - OTP: 123456"
    } else {
        Write-Host "    Chưa có hóa đơn nào của Member để test. Bạn có thể gọi API đăng ký lớp hoặc mua gói trước." -ForegroundColor Yellow
    }
} catch {
    Write-Host "    [LỖI] Không tạo được link VNPay: $($_.Exception.Message)" -ForegroundColor Red
}

# 2.2 Test tạo mã VietQR động trực tiếp bằng thẻ ảnh URL
Write-Host "`n2. Tạo mã VietQR động (Chuyển khoản Ngân hàng tại quầy):"
$sampleAmount = 900000
$sampleInvoice = "INV-2026-000105"
$vietQrUrl = "https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=$sampleAmount&addInfo=$sampleInvoice&accountName=TRUNG%20TAM%20THE%20THAO%20SCMS"
Write-Host "    Đường link ảnh mã QR VietQR (chuẩn Napas 247):" -ForegroundColor Green
Write-Host "    ------------------------------------------------------------------"
Write-Host "    $vietQrUrl" -ForegroundColor Blue
Write-Host "    ------------------------------------------------------------------"
Write-Host "    HƯỚNG DẪN TEST:" -ForegroundColor Yellow
Write-Host "    - Dán đường link trên vào trình duyệt -> Ảnh mã QR ngân hàng MBBank sẽ hiện ra."
Write-Host "    - Dùng điện thoại quét: Số tiền 900.000đ và nội dung 'INV-2026-000105' sẽ tự động điền sẵn!"

Write-Host "`n======================================================================" -ForegroundColor Yellow
Write-Host " HOÀN TẤT KỊCH BẢN TEST ĐỘC LẬP!" -ForegroundColor Yellow
Write-Host "======================================================================`n"

