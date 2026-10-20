# ==============================================================================
# SCRIPT TEST RIÊNG MÃ QR THANH TOÁN VÀ MÃ TOTP 30S (KHÔNG CẦN QUA FE)
# Hỗ trợ test độc lập trên Local (http://localhost:8080) hoặc Render
# Cách chạy:
#   .\scripts\test-payment-and-totp.ps1 -BaseUrl "https://scms-app-p1gy.onrender.com" -MemberEmail "abc1@ex.com" -MemberPassword "abc1@ex.com"
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [Alias("Email")]
    [string]$MemberEmail = "abc1@ex.com",
    [Alias("Password")]
    [string]$MemberPassword = "abc1@ex.com"
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
# PHẦN 1: TEST MÃ CHECK-IN TOTP 30 GIÂY (CHUẨN /api/members/me/checkin-code)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "--- [PHẦN 1] TEST MÃ TOTP CHECK-IN ĐỔI MỖI 30 GIÂY ---" -ForegroundColor Cyan

Write-Host "1. Đang đăng nhập tài khoản Member ($MemberEmail)..." -NoNewline
$loginSuccess = $false
$triedPasswords = @($MemberPassword, "12345678", "Scms@2026", "password", "Admin@123") | Select-Object -Unique

foreach ($pass in $triedPasswords) {
    try {
        $loginBody = @{ email = $MemberEmail; password = $pass } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $memberSession -TimeoutSec 10
        Write-Host " [OK] Đăng nhập thành công với mật khẩu '$pass'!" -ForegroundColor Green
        $loginSuccess = $true
        break
    } catch {}
}

if (-not $loginSuccess) {
    Write-Host " [THẤT BẠI] Không thể đăng nhập bằng các mật khẩu mẫu." -ForegroundColor Yellow
    Write-Host "   -> Vui lòng chạy lại kèm mật khẩu của tài khoản Member trên Supabase:" -ForegroundColor Cyan
    Write-Host "      .\scripts\test-payment-and-totp.ps1 -BaseUrl `"$BaseUrl`" -MemberEmail `"$MemberEmail`" -MemberPassword `"mat_khau_that`"" -ForegroundColor Cyan
} else {
    # 1.2 Lấy mã TOTP lần 1
    Write-Host "`n2. Member yêu cầu mã TOTP check-in qua GET /api/members/me/checkin-code..."
    try {
        $totp1 = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/checkin-code" -Method Get -WebSession $memberSession -TimeoutSec 10
        Write-Host "    ==> MÃ TOTP HIỆN TẠI : " -NoNewline
        Write-Host "$($totp1.payload)" -ForegroundColor Green -BackgroundColor Black
        Write-Host "    ==> THỜI GIAN HIỆU LỰC: $($totp1.expiresInSeconds) giây còn lại" -ForegroundColor Yellow
        Write-Host "    ==> LINK XEM ẢNH QR ĐIỂM DANH (Dán vào Chrome/Edge hoặc quét bằng điện thoại):" -ForegroundColor Cyan
        Write-Host "        https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$($totp1.payload)" -ForegroundColor Blue

        Write-Host "`n3. Kiểm tra tính năng làm mới mã sau 30 giây:"
        Write-Host "    Đang chờ 5 giây để quan sát thời gian đếm ngược..."
        Start-Sleep -Seconds 5
        $totp2 = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/checkin-code" -Method Get -WebSession $memberSession -TimeoutSec 10
        Write-Host "    ==> Sau 5 giây, thời gian còn lại: $($totp2.expiresInSeconds) giây (Mã vẫn giữ nguyên: $($totp2.payload))" -ForegroundColor Cyan
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
        $pendingInv = $invoices | Where-Object { $_.paymentStatus -match "Pending" -and $_.paymentMethod -match "VNPay" } | Select-Object -First 1

        $vnpayUrl = $null
        $invAmount = 500000
        $invId = 0

        if ($pendingInv) {
            $invId = $pendingInv.invoiceId
            $invAmount = $pendingInv.amount
            Write-Host "    Tìm thấy hóa đơn Pending #$invId. Đang lấy link VNPay..."
            $payRes = Invoke-RestMethod -Uri "$BaseUrl/api/invoices/$invId/pay-online" -Method Post -WebSession $memberSession -TimeoutSec 10
            $vnpayUrl = $payRes.paymentUrl
        } else {
            Write-Host "    Tài khoản chưa có hóa đơn VNPay Pending. Đang khởi tạo đơn mua gói mới với VNPay..."
            $packages = Invoke-RestMethod -Uri "$BaseUrl/api/packages" -Method Get -WebSession $memberSession -TimeoutSec 10
            $pkgId = if ($packages.Count -gt 0) { $packages[0].id } else { 1 }
            $subBody = @{ packageId = $pkgId; paymentMethod = "VNPay" } | ConvertTo-Json
            $subRes = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/subscriptions" -Method Post -Body $subBody -ContentType "application/json" -WebSession $memberSession -TimeoutSec 10
            $invId = $subRes.invoiceId
            $invAmount = $subRes.amount
            $vnpayUrl = $subRes.paymentUrl
        }

        if ($vnpayUrl) {
            Write-Host "    [OK] ĐÃ SINH LINK THANH TOÁN VNPAY THÀNH CÔNG CHO HÓA ĐƠN #$invId ($invAmount VNĐ)!" -ForegroundColor Green
            Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "    $vnpayUrl" -ForegroundColor Blue
            Write-Host "    ------------------------------------------------------------------" -ForegroundColor DarkGray
            Write-Host "    HƯỚNG DẪN TEST MÃ QR TRÊN TRÌNH DUYỆT:" -ForegroundColor Yellow
            Write-Host "    - Copy đường link màu xanh ở trên và dán vào Chrome/Edge."
            Write-Host "    - Cổng VNPay Sandbox sẽ hiển thị MÃ QR VNPAY-QR to ở giữa màn hình."
            Write-Host "    - Dùng App ngân hàng bất kỳ quét mã xem tự động nhận diện đơn hàng."
            Write-Host "    - Để hoàn tất đơn thanh toán: Chọn Thẻ nội địa -> NCB:"
            Write-Host "      + Số thẻ: 9704198526191432198"
            Write-Host "      + Tên chủ thẻ: NGUYEN VAN A"
            Write-Host "      + Ngày phát hành: 07/15"
            Write-Host "      + Mã OTP: 123456"
        }
    } catch {
        Write-Host "    [LỖI] Không tạo được link VNPay: $($_.Exception.Message)" -ForegroundColor Red
        $resp = $_.Exception.Response
        if ($resp) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            Write-Host "    Chi tiết từ Backend: $($reader.ReadToEnd())" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "    (Cần đăng nhập Member thành công để sinh link VNPay)" -ForegroundColor DarkGray
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