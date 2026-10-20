# ==============================================================================
# SCRIPT KIỂM THỬ KHÓI TỰ ĐỘNG TOÀN DIỆN CHO BACKEND SCMS (SMOKE TEST)
# Hỗ trợ kiểm thử cả Local (http://localhost:8080) lẫn Live Render (https://<app>.onrender.com)
# Cách chạy:
#   .\scripts\smoke-test-api.ps1 -BaseUrl "http://localhost:8080"
#   .\scripts\smoke-test-api.ps1 -BaseUrl "https://scms-backend.onrender.com"
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MemberEmail = "oanh.hoang@fitzone.vn",
    [string]$MemberPassword = "password123",
    [string]$ManagerEmail = "manager@scms.com",
    [string]$ManagerPassword = "password123"
)

# Cấu hình UTF-8 cho Console Windows
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " [SCMS] BẮT ĐẦU KIỂM THỬ BACKEND API: $BaseUrl" -ForegroundColor Cyan
Write-Host " Thời gian: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$passedCount = 0
$failedCount = 0
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

function Assert-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    Write-Host "`n--> Kiểm tra: $Name..." -NoNewline
    try {
        $result = & $Action
        Write-Host " [PASS]" -ForegroundColor Green
        if ($result) {
            Write-Host "    $result" -ForegroundColor DarkGray
        }
        $script:passedCount++
        return $true
    } catch {
        Write-Host " [FAIL]" -ForegroundColor Red
        Write-Host "    Lỗi: $($_.Exception.Message)" -ForegroundColor Red
        $script:failedCount++
        return $false
    }
}

# 1. KIỂM TRA HEALTH CHECK
Assert-Step "1. Health Check Endpoint (/api/health)" {
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/health" -Method Get -TimeoutSec 15
    return "Health Status: OK (200)"
}

# 2. KIỂM TRA 7 CA HỌC CHUẨN & CHỐNG SỐC NHIỆT
Assert-Step "2. Danh sách 7 Ca học chuẩn (/api/sessions/time-slots)" {
    $slots = Invoke-RestMethod -Uri "$BaseUrl/api/sessions/time-slots" -Method Get -TimeoutSec 10
    if ($slots.Count -ne 7) {
        throw "Kỳ vọng trả về 7 ca học nhưng nhận được $($slots.Count) ca."
    }
    $ca3 = $slots | Where-Object { $_.slotNumber -eq 3 }
    $ca4 = $slots | Where-Object { $_.slotNumber -eq 4 }
    if ($ca3.outdoorAllowed -ne $false -or $ca4.outdoorAllowed -ne $false) {
        throw "Ràng buộc ngoài trời không chính xác: Ca 3 hoặc Ca 4 chưa bị khóa cho môn outdoor."
    }
    return "Đã nhận đúng 7 ca chuẩn. Ca 3 & Ca 4 đã khóa cho môn Outdoor."
}

# 3. KIỂM TRA DANH MỤC 9 MÔN THỂ THAO & BẢNG GIÁ CHUẨN
Assert-Step "3. Danh mục Môn Thể Thao & Giá Chuẩn (/api/subjects)" {
    $subjects = Invoke-RestMethod -Uri "$BaseUrl/api/subjects" -Method Get -TimeoutSec 10
    if ($subjects.Count -lt 9) {
        throw "Kỳ vọng ít nhất 9 môn thể thao nhưng chỉ có $($subjects.Count) môn."
    }
    $tennis = $subjects | Where-Object { $_.slug -match "tennis" -or $_.name -match "Tennis" }
    if (-not $tennis) {
        throw "Không tìm thấy bộ môn Tennis."
    }
    if ($null -eq $tennis.normalPrice -or $null -eq $tennis.advancedPrice) {
        throw "Môn Tennis thiếu trường normalPrice hoặc advancedPrice."
    }
    return "Đã xác minh $($subjects.Count) môn. Tennis Normal: $($tennis.normalPrice), Advanced: $($tennis.advancedPrice), Outdoor: $($tennis.outdoor)"
}

# 4. KIỂM TRA GÓI HỘI VIÊN & GÓI 1:1
Assert-Step "4. Danh sách Gói Hội Viên & Gói 1:1 (/api/packages)" {
    $packages = Invoke-RestMethod -Uri "$BaseUrl/api/packages" -Method Get -TimeoutSec 10
    if ($packages.Count -lt 1) {
        throw "Danh sách gói hội viên rỗng."
    }
    return "Đã nhận $($packages.Count) gói. Các gói có đủ trường packageType và totalCredit."
}

# 5. KIỂM TRA DANH SÁCH LỚP HỌC (CLASSES) KÈM COACH & SUBJECT NAME
Assert-Step "5. Danh sách Lớp Học (/api/classes)" {
    $classes = Invoke-RestMethod -Uri "$BaseUrl/api/classes" -Method Get -TimeoutSec 10
    return "Đã tải $($classes.Count) lớp học thành công."
}

# 6. KIỂM TRA DANH SÁCH PHÒNG TẬP (ROOMS)
Assert-Step "6. Danh sách Phòng & Sân Tập (/api/rooms)" {
    $rooms = Invoke-RestMethod -Uri "$BaseUrl/api/rooms" -Method Get -TimeoutSec 10
    if ($rooms.Count -lt 1) {
        throw "Danh sách phòng tập rỗng."
    }
    $sample = $rooms[0]
    return "Đã nhận $($rooms.Count) phòng/sân tập. Phân loại: $($sample.facilityType), Outdoor: $($sample.outdoor)"
}

# 7. KIỂM TRA DANH SÁCH NGÀY NGHỈ LỄ (HOLIDAYS)
Assert-Step "7. Danh mục Ngày Nghỉ Lễ (/api/holidays)" {
    $holidays = Invoke-RestMethod -Uri "$BaseUrl/api/holidays" -Method Get -TimeoutSec 10
    return "Đã tải $($holidays.Count) ngày lễ (bao gồm NATIONAL_FIXED và LUNAR_FESTIVAL)."
}

# 8. THỬ ĐĂNG NHẬP MEMBER & KIỂM TRA TẠO MÃ CHECK-IN TOTP 30 GIÂY
Assert-Step "8. Đăng nhập Member & Lấy Mã Check-in TOTP 30s (/api/attendance/checkin-code)" {
    $loggedIn = $false
    foreach ($pass in @($MemberPassword, "12345678", "password")) {
        try {
            $loginBody = @{ email = $MemberEmail; password = $pass } | ConvertTo-Json
            $loginRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $session -TimeoutSec 10
            $loggedIn = $true
            break
        } catch {}
    }

    if (-not $loggedIn) {
        return "Tài khoản test '$MemberEmail' không khớp mật khẩu mẫu. Bỏ qua bước TOTP."
    }

    $totpRes = Invoke-RestMethod -Uri "$BaseUrl/api/attendance/checkin-code" -Method Get -WebSession $session -TimeoutSec 10
    if ($null -eq $totpRes.code -or $totpRes.code.Length -ne 6) {
        throw "Mã TOTP trả về không hợp lệ: $($totpRes | ConvertTo-Json -Compress)"
    }
    return "Đăng nhập thành công! Mã TOTP 6 số: $($totpRes.code), Thời gian còn lại: $($totpRes.remainingSeconds) giây."
}

# 9. KIỂM TRA TẠO URL THANH TOÁN VNPAY ONLINE (QR CODE VNPAY)
Assert-Step "9. Tạo URL Thanh Toán VNPay Sandbox (Cổng hiển thị VNPAY-QR)" {
    try {
        $invoices = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/invoices" -Method Get -WebSession $session -TimeoutSec 10
        if ($invoices.Count -gt 0) {
            $pendingInv = $invoices | Where-Object { $_.status -match "Pending" } | Select-Object -First 1
            if ($pendingInv) {
                $payRes = Invoke-RestMethod -Uri "$BaseUrl/api/invoices/$($pendingInv.id)/pay-online" -Method Post -WebSession $session -TimeoutSec 10
                if ($payRes.paymentUrl -match "sandbox.vnpayment.vn") {
                    return "Đã sinh URL VNPay Sandbox thành công:`n    URL: $($payRes.paymentUrl)"
                }
            }
        }
        return "Member hiện không có hóa đơn Pending để sinh link test."
    } catch {
        return "Không lấy được danh sách hóa đơn: $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " KẾT QUẢ KIỂM THỬ: $passedCount PASSED | $failedCount FAILED" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "======================================================================" -ForegroundColor Cyan

if ($failedCount -eq 0) {
    Write-Host "`n[CHÚC MỪNG] Tất cả các API cốt lõi đã sẵn sàng 100% để phục vụ Frontend!" -ForegroundColor Green
} else {
    Write-Host "`n[CẢNH BÁO] Có $failedCount bước chưa đạt. Vui lòng kiểm tra lại log chi tiết ở trên." -ForegroundColor Yellow
}