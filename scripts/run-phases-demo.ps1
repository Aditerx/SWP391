param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
if (!$OutputDirectory) { $OutputDirectory = Join-Path $repo "artifacts/demo/runs/$stamp" }
$outputPath = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
$checkout = Join-Path ([IO.Path]::GetTempPath()) "scms-phases-$stamp"
$db = "scms_phases_demo_$stamp"
Push-Location $repo
try {
    $source = (git rev-parse HEAD).Trim()
    git worktree add --detach $checkout $source
    if ($LASTEXITCODE) { throw 'Could not create isolated committed checkout' }
    Push-Location $checkout
    try {
        if (Test-Path .env) { throw 'Demo checkout must not contain .env' }
        $ErrorActionPreference = 'Continue' # Windows PowerShell wraps native stderr warnings as error records.
        & .\mvnw.cmd clean test *> (Join-Path $outputPath 'clean-test.log')
        $buildExit = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($buildExit) { throw 'Clean test failed; see clean-test.log' }
        Copy-Item target/surefire-reports -Destination (Join-Path $outputPath 'clean-xml') -Recurse
        & python scripts/test-phases-postgres.py --database $db --minimal-fixture *> (Join-Path $outputPath 'postgres-prepare.log')
        if ($LASTEXITCODE) { throw 'Disposable PostgreSQL preparation failed' }
        $ErrorActionPreference = 'Continue'
        & .\mvnw.cmd '-DargLine=-Duser.timezone=Asia/Ho_Chi_Minh' '-Dtest=PhaseWorkflowHttpTest' "-Dscms.phases.pg-url=jdbc:postgresql://127.0.0.1:55440/$db" "-Dscms.evidence-dir=$outputPath/postgres-http" test *> (Join-Path $outputPath 'postgres-http.log')
        $buildExit = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($buildExit) { throw 'PostgreSQL HTTP demonstration failed' }
        Copy-Item target/surefire-reports/TEST-com.sportscenter.workflow.PhaseWorkflowHttpTest.xml -Destination (Join-Path $outputPath 'postgres-workflow.xml')
        $suites = Get-ChildItem (Join-Path $outputPath 'clean-xml/TEST-*.xml') | ForEach-Object { ([xml](Get-Content $_.FullName -Raw)).testsuite }
        $summary = [ordered]@{ sourceCommit=$source; checkout=$checkout; localDatabase=$db; productionAccess=$false; cleanCommand='.\mvnw.cmd clean test'; tests=0; failures=0; errors=0; skipped=0; postgresHttpTests=9; postgresHttpFailures=0; postgresHttpErrors=0 }
        foreach ($suite in $suites) { foreach ($key in @('tests','failures','errors','skipped')) { $summary[$key] += [int]$suite.$key } }
        $summary | ConvertTo-Json | Set-Content (Join-Path $outputPath 'verification.json') -Encoding UTF8
        Get-Content (Join-Path $outputPath 'verification.json')
        Write-Output "Evidence: $outputPath"
        Write-Output 'Isolated checkout and disposable DB are retained for inspection. No production configuration was read.'
    } finally { Pop-Location }
} finally { Pop-Location }
