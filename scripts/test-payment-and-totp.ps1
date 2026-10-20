# ==============================================================================
# SCRIPT TEST RIÊNG MÃ QR THANH TOÁN VÀ MÃ TOTP 30S (KHÔNG CẦN QUA FE)
# Hỗ trợ test độc lập trên Local (http://localhost:8080) hoặc Render
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

# Cấu hình UTF-8 cho Console Windows
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Yellow
Write-Host " [SCMS] TEST ĐỘC LẬP: MÃ QR THANH TOÁN VÀ MÃ ĐIỂM DANH TOTP 30 GIÂY" -ForegroundColor Yellow
Write-Host " Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Yellow

$memberSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# ------------------------------------------------------------------------------
# PHẦN 1: TEST MÃ CHECK-IN TOTP 30 GIÂY
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "--- [PHẦN 1] TEST MÃ TOTP CHECK-IN ĐỔI MỖI 30 GIÂY ---" -ForegroundColor Cyan

# 1.1 Đăng nhập Member
Write-Host "1. Đang đăng nhập tài khoản Member ($MemberEmail)..." -NoNewline
$loginSuccess = $false
foreach ($pass in @($MemberPassword, "12345678", "password")) {
    try {
        $loginBody = @{ email = $MemberEmail; password = $pass } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $memberSession -TimeoutSec 10
        Write-Host " [OK] Đăng nhập thành công với mật khẩu '$pass'!" -ForegroundColor Green
        $loginSuccess = $true
        break
    } catch {}
}

if (-not $loginSuccess) {
    Write-Host " [BỎ QUA] Không đăng nhập được với các mật khẩu mẫu." -ForegroundColor Yellow
    Write-Host "   -> Bạn có thể truyền mật khẩu của bạn: -MemberPassword 'mat_khau_that'" -ForegroundColor DarkGray
} else {
    # 1.2 Lấy mã TOTP lần 1
    Write-Host "`n2. Member yêu cầu mã TOTP check-in qua GET /api/attendance/checkin-code..."
    try {
        $totp1 = Invoke-RestMethod -Uri "$BaseUrl/api/attendance/checkin-code" -Method Get -WebSession $memberSession -TimeoutSec 10
        Write-Host "    ==> MÃ TOTP HIỆN TẠI : " -NoNewline
        Write-Host "$($totp1.code)" -ForegroundColor Green -BackgroundColor Black
        Write-Host "    ==> THỜI GIAN HIỆU LỰC: $($totp1.remainingSeconds) giây còn lại" -ForegroundColor Yellow

        Write-Host "`n3. Kiểm tra tính năng làm mới mã sau 30 giây:"
        Write-Host "    Đang chờ 5 giây để quan sát thời gian đếm ngược..."
        Start-Sleep -Seconds 5
        $totp2 = Invoke-RestMethod -Uri "$BaseUrl/api/attendance/checkin-code" -Method Get -WebSession $memberSession -TimeoutSec 10
        Write-Host "    ==> Sau 5 giây, thời gian còn lại: $($totp2.remainingSeconds) giây (Mã vẫn giữ nguyên: $($totp2.code))" -ForegroundColor Cyan
    } catch {
        Write-Host "    [LỖI] Không lấy được mã TOTP: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# ------------------------------------------------------------------------------
# PHẦN 2: TEST MÃ QR THANH TOÁN (VNPAY SANDBOX VÀ VIETQR)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "--- [PHẦN 2] TEST MÃ QR THANH TOÁN (VNPAY SANDBOX VÀ VIETQR) ---" -ForegroundColor Cyan

# 2.1 Test tạo URL cổng VNPay Sandbox
Write-Host "1. Sinh link thanh toán VNPay Sandbox có chứa VNPAY-QR:"
if ($loginSuccess) {
    try {
        $invoices = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/invoices" -Method Get -WebSession $memberSession -TimeoutSec 10
        $targetInv = $invoices | Where-Object { $_.status -match "Pending" } | Select-Object -First 1
        if (-not $targetInv -and $invoices.Count -gt 0) {
            $targetInv = $invoices[0]
        }

        if ($targetInv) {
            $payRes = Invoke-RestMethod -Uri "$BaseUrl/api/invoices/$($targetInv.id)/pay-online" -Method Post -WebSession $memberSession -TimeoutSec 10
            Write-Host "    [OK] Đã sinh link thanh toán cho Hóa đơn #$($targetInv.id) (Số tiền: $($targetInv.amount) VNĐ):" -ForegroundColor Green
            Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "    $($payRes.paymentUrl)" -ForegroundColor Blue
            Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "    HƯỚNG DẪN TEST TRÊN TRÌNH DUYỆT:" -ForegroundColor Yellow
            Write-Host "    - Copy link trên dán vào Chrome/Edge -> Cổng VNPay Sandbox sẽ hiện MÃ QR VNPAY."
            Write-Host "    - Dùng App ngân hàng quét mã hoặc chọn thẻ NCB demo: 9704198526191432198, OTP: 123456"
        } else {
            Write-Host "    Member chưa có hóa đơn nào để test link VNPay. Bạn có thể enroll 1 lớp trước." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "    [LỖI] Không tạo được link VNPay: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "    (Cần đăng nhập Member thành công để lấy hóa đơn và sinh link VNPay)" -ForegroundColor DarkGray
}

# 2.2 Test tạo mã VietQR động trực tiếp bằng thẻ ảnh URL
Write-Host "`n2. Tạo mã VietQR động (Chuyển khoản Ngân hàng tại quầy):"
$sampleAmount = 900000
$sampleInvoice = "INV-2026-000105"
$vietQrUrl = "https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=$sampleAmount&addInfo=$sampleInvoice&accountName=TRUNG%20TAM%20THE%20THAO%20SCMS"
Write-Host "    Đường link ảnh mã QR VietQR (chuẩn Napas 247):" -ForegroundColor Green
Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "    $vietQrUrl" -ForegroundColor Blue
Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "    HƯỚNG DẪN TEST:" -ForegroundColor Yellow
Write-Host "    - Dán đường link trên vào trình duyệt -> Ảnh mã QR ngân hàng MBBank sẽ hiện ra."
Write-Host "    - Dùng điện thoại quét: Số tiền 900.000đ và nội dung 'INV-2026-000105' tự động điền sẵn!"

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Yellow
Write-Host " HOÀN TẤT KỊCH BẢN TEST ĐỘC LẬP!" -ForegroundColor Yellow
Write-Host "======================================================================`n"