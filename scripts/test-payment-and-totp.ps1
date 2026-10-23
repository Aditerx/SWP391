param(
    [string]$BaseUrl = "http://localhost:8080",
    [Alias("Email")][string]$MemberEmail = $env:SCMS_TEST_MEMBER_EMAIL,
    [Alias("Password")][string]$MemberPassword = $env:SCMS_TEST_MEMBER_PASSWORD,
    [switch]$CreateOrder,
    [int]$PackageId
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
if (-not $MemberEmail -or -not $MemberPassword) { throw "Provide SCMS_TEST_MEMBER_EMAIL and SCMS_TEST_MEMBER_PASSWORD or explicit credentials." }
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = @{email=$MemberEmail; password=$MemberPassword} | ConvertTo-Json
Invoke-RestMethod "$BaseUrl/api/auth/login" -Method Post -Body $login -ContentType "application/json" -WebSession $session | Out-Null
Write-Host "PASS: Login. Credentials are not printed."
$first = Invoke-RestMethod "$BaseUrl/api/members/me/checkin-code" -WebSession $session
if ($first.payload -notmatch '^\d{6}$' -or $first.expiresInSeconds -lt 1 -or $first.expiresInSeconds -gt 30) { throw "Invalid TOTP response." }
Start-Sleep -Seconds ([int]$first.expiresInSeconds + 1)
$second = Invoke-RestMethod "$BaseUrl/api/members/me/checkin-code" -WebSession $session
if ($second.payload -notmatch '^\d{6}$' -or $second.expiresInSeconds -lt 1 -or $second.expiresInSeconds -gt 30) { throw "Invalid rotated TOTP response." }
# Adjacent TOTP values can coincide. Observe one more boundary if necessary.
if ($first.payload -eq $second.payload) {
    Start-Sleep -Seconds ([int]$second.expiresInSeconds + 1)
    $third = Invoke-RestMethod "$BaseUrl/api/members/me/checkin-code" -WebSession $session
    if ($second.payload -eq $third.payload) { throw "TOTP did not change across two observed windows." }
}
Write-Host "PASS: TOTP format, expiry and rotation. Scan/replay requires a separate staff integration test."
$invoices = Invoke-RestMethod "$BaseUrl/api/members/me/invoices" -WebSession $session
$nowVietnam = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([datetime]::UtcNow, 'SE Asia Standard Time')
$pending = $invoices | Where-Object { $_.paymentStatus -eq 'Pending' -and $_.paymentMethod -eq 'VNPay' -and [datetime]$_.expiresAt -gt $nowVietnam } | Select-Object -First 1
if ($pending) {
    $payment = Invoke-RestMethod "$BaseUrl/api/invoices/$($pending.invoiceId)/pay-online" -Method Post -WebSession $session
} elseif ($CreateOrder) {
    if ($PackageId -le 0) { throw "-CreateOrder requires an explicit positive -PackageId on a test database." }
    $body = @{packageId=$PackageId; paymentMethod='VNPay'} | ConvertTo-Json
    $payment = Invoke-RestMethod "$BaseUrl/api/members/me/subscriptions" -Method Post -Body $body -ContentType "application/json" -WebSession $session
} else {
    Write-Host "SKIP: No payable VNPay invoice; no order was created. Use -CreateOrder -PackageId on a test database to create one."
    exit 0
}
if (-not $payment.paymentUrl -or $payment.paymentUrl -notmatch '^https://') { throw "Missing HTTPS payment URL." }
Write-Host "PASS: VNPay URL generated. Open manually to test the sandbox and callback."
Write-Host $payment.paymentUrl
Write-Host "This script does not confirm money received, Cash, IPN delivery or QR scanning."
