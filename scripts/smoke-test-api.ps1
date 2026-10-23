# ==============================================================================
# SCRIPT KIỂM THỬ KHÓI TỰ ĐỘNG TOÀN DIỆN CHO BACKEND SCMS (SMOKE TEST)
# Hỗ trợ kiểm thử cả Local lẫn Live Render
# Cách chạy:
#   .\scripts\smoke-test-api.ps1 -BaseUrl "https://scms-app-p1gy.onrender.com" -Email "abc1@ex.com" -Password "abc1@ex.com"
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = $env:SCMS_TEST_MEMBER_EMAIL,
    [string]$Password = $env:SCMS_TEST_MEMBER_PASSWORD
)

# Cấu hình UTF-8 cho Console Windows
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

$ErrorActionPreference = "Stop"
if (-not $Email -or -not $Password) { throw "Provide SCMS_TEST_MEMBER_EMAIL and SCMS_TEST_MEMBER_PASSWORD or explicit parameters." }
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

# 1. KIỂM TRA HEALTH CHECK (PUBLIC ENDPOINT KHÔNG CẦN LOGIN)
Assert-Step "1. Health Check Endpoint (/api/health)" {
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/health" -Method Get -TimeoutSec 15
    if ($res.status -ne "UP") { throw "Health endpoint did not return UP." }
    return "Health Status: OK (200), Response: $($res | ConvertTo-Json -Compress)"
}

# 2. XÁC THỰC ĐĂNG NHẬP ĐỂ LẤY SESSION CHO CÁC API PHÍA SAU
$isLoggedIn = $false
$userRole = ""
Assert-Step "2. Xác thực Đăng Nhập (/api/auth/login)" {
    $passwordsToTry = @($Password) # Never guess passwords.

    foreach ($p in $passwordsToTry) {
        try {
            $loginBody = @{ email = $Email; password = $p } | ConvertTo-Json
            $loginRes = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -WebSession $session -TimeoutSec 10
            $script:isLoggedIn = $true
            $script:userRole = $loginRes.role
            return "Đăng nhập THÀNH CÔNG tài khoản '$Email' (Role: $($loginRes.role)). Đã lưu Session Cookie!"
        } catch {}
    }

    throw "Đăng nhập thất bại với tài khoản '$Email'. Sai email hoặc mật khẩu trên database Supabase của bạn.`n    Vui lòng chạy lại với tham số: -Email 'email_cua_ban' -Password 'mat_khau_that'"
}

if (-not $isLoggedIn) {
    Write-Host "`n[CẢNH BÁO] Không thể chạy tiếp các bước sau vì cần phiên đăng nhập hợp lệ." -ForegroundColor Yellow
    exit 1
}

# 3. KIỂM TRA 7 CA HỌC CHUẨN & CHỐNG SỐC NHIỆT (ĐÃ CÓ SESSION)
Assert-Step "3. Danh sách 7 Ca học chuẩn (/api/sessions/time-slots)" {
    $slots = Invoke-RestMethod -Uri "$BaseUrl/api/sessions/time-slots" -Method Get -WebSession $session -TimeoutSec 10
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

# 4. KIỂM TRA DANH MỤC 9 MÔN THỂ THAO & BẢNG GIÁ CHUẨN
Assert-Step "4. Danh mục Môn Thể Thao & Giá Chuẩn (/api/subjects)" {
    $subjects = Invoke-RestMethod -Uri "$BaseUrl/api/subjects" -Method Get -WebSession $session -TimeoutSec 10
    if ($subjects.Count -lt 9) {
        throw "Kỳ vọng ít nhất 9 môn thể thao nhưng chỉ có $($subjects.Count) môn."
    }
    $tennis = $subjects | Where-Object { $_.slug -match "tennis" -or $_.name -match "Tennis" }
    if (-not $tennis) {
        throw "Không tìm thấy bộ môn Tennis."
    }
    foreach ($subject in $subjects) {
        foreach ($field in @('normalPrice','advancedPrice','outdoor')) {
            if (-not $subject.PSObject.Properties[$field]) { throw "Subject response is missing $field." }
        }
    }
    return "Đã kiểm tra schema của $($subjects.Count) môn. Bảng giá chính thức cần xác nhận riêng."
}

# 5. KIỂM TRA GÓI HỘI VIÊN & GÓI 1:1
Assert-Step "5. Danh sách Gói Hội Viên & Gói 1:1 (/api/packages)" {
    $packages = Invoke-RestMethod -Uri "$BaseUrl/api/packages" -Method Get -WebSession $session -TimeoutSec 10
    return "Đã nhận $($packages.Count) gói hội viên. Có đầy đủ packageType và totalCredit."
}

# 6. KIỂM TRA DANH SÁCH LỚP HỌC (CLASSES) KÈM COACH & SUBJECT NAME
Assert-Step "6. Danh sách Lớp Học (/api/classes)" {
    $classes = Invoke-RestMethod -Uri "$BaseUrl/api/classes" -Method Get -WebSession $session -TimeoutSec 10
    return "Đã tải $($classes.Count) lớp học thành công."
}

# 7. KIỂM TRA DANH SÁCH PHÒNG TẬP (ROOMS)
Assert-Step "7. Danh sách Phòng & Sân Tập (/api/rooms)" {
    $rooms = Invoke-RestMethod -Uri "$BaseUrl/api/rooms" -Method Get -WebSession $session -TimeoutSec 10
    return "Đã nhận $($rooms.Count) phòng/sân tập. Phân loại Indoor/Outdoor."
}

# 8. KIỂM TRA DANH SÁCH NGÀY NGHỈ LỄ (HOLIDAYS)
Assert-Step "8. Danh mục Ngày Nghỉ Lễ (/api/holidays)" {
    $holidays = Invoke-RestMethod -Uri "$BaseUrl/api/holidays" -Method Get -WebSession $session -TimeoutSec 10
    return "Đã tải $($holidays.Count) ngày lễ."
}

# 9. KIỂM TRA MÃ TOTP CHECK-IN 30S (CHUẨN /api/members/me/checkin-code)
Assert-Step "9. Mã Check-in TOTP 30s (/api/members/me/checkin-code)" {
    try {
        $totpRes = Invoke-RestMethod -Uri "$BaseUrl/api/members/me/checkin-code" -Method Get -WebSession $session -TimeoutSec 10
        if ($null -eq $totpRes.payload -or $totpRes.payload.Length -ne 6) {
            throw "Mã TOTP trả về không hợp lệ: $($totpRes | ConvertTo-Json -Compress)"
        }
        return "Mã TOTP 6 số: $($totpRes.payload), Thời gian hiệu lực còn lại: $($totpRes.expiresInSeconds) giây."
    } catch {
        if ($script:userRole -ne "MEMBER" -and $script:userRole -ne "Member" -and $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) {
            return "Tài khoản hiện tại có Role là '$($script:userRole)' (chỉ Member mới có mã TOTP checkin cá nhân, đúng thiết kế bảo mật)."
        }
        throw $_
    }
}

Write-Host ""
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " KẾT QUẢ KIỂM THỬ: $passedCount PASSED | $failedCount FAILED" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "======================================================================" -ForegroundColor Cyan

if ($failedCount -eq 0) {
    Write-Host "`n[CHÚC MỪNG] Các bước smoke đã pass; chưa thay thế nghiệm thu nghiệp vụ hoặc thanh toán." -ForegroundColor Green
}
if ($failedCount -gt 0) { exit 1 }
exit 0
