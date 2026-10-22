# AdvanceFoodnest PowerShell Backend Runner
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " AdvanceFoodnest Spring Boot Backend (Port 8080)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Automatically free port 8080 if an old process is lingering
$occupied = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($occupied) {
    $pids = $occupied | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($p in $pids) {
        if ($p -gt 0) {
            Write-Host "[INFO] Freeing port 8080 from process PID: $p..." -ForegroundColor Yellow
            Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 1
}

# 2. Database Password
if (-not $env:DB_PASSWORD) {
    $env:DB_PASSWORD = "veera8610"
}

# 3. Java 21 Home
if (Test-Path "C:\Program Files\Java\jdk-21.0.11") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
    $env:PATH = "C:\Program Files\Java\jdk-21.0.11\bin;$env:PATH"
}

# 4. Run the production JAR
Set-Location -Path "$PSScriptRoot\backend"
if (Test-Path "target\advance-foodnest-1.0.0.jar") {
    Write-Host "[INFO] Starting advance-foodnest-1.0.0.jar on http://localhost:8080..." -ForegroundColor Green
    java -jar target\advance-foodnest-1.0.0.jar
} else {
    Write-Host "[INFO] Running via Maven..." -ForegroundColor Green
    cmd /c "mvn spring-boot:run"
}
