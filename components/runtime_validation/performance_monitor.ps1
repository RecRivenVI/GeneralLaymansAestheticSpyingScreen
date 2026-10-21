param([Parameter(Mandatory)][string]$OutputPath, [Parameter(Mandatory)][string]$StopPath)
$ErrorActionPreference = 'Stop'
while (-not (Test-Path -LiteralPath $StopPath)) {
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $processes = @(Get-Process)
    $active = @{}
    foreach ($process in $processes) { $active[$process.Id] = $process }
    $engines = @(Get-CimInstance Win32_PerfFormattedData_GPUPerformanceCounters_GPUEngine | Where-Object UtilizationPercentage -gt 0 | ForEach-Object {
        if ($_.Name -match '^pid_(\d+)_') {
            $processId = [int]$Matches[1]
            if ($active.ContainsKey($processId)) {
                @{ name = $_.Name; pid = $processId; process = $active[$processId].ProcessName; percent = $_.UtilizationPercentage }
            }
        }
    })
    $java = @($processes | Where-Object ProcessName -Match '^java(w)?$' | ForEach-Object {
        @{ pid = $_.Id; name = $_.ProcessName; title = $_.MainWindowTitle; cpuSeconds = $_.CPU; workingSet = $_.WorkingSet64; privateBytes = $_.PrivateMemorySize64 }
    })
    $javaIds = @($java | ForEach-Object { $_.pid })
    $memory = @(Get-CimInstance Win32_PerfFormattedData_GPUPerformanceCounters_GPUProcessMemory | Where-Object {
        $_.Name -match '^pid_(\d+)_' -and [int]$Matches[1] -in $javaIds
    } | Select-Object Name, DedicatedUsage, SharedUsage, TotalCommitted)
    $device = & nvidia-smi --query-gpu=utilization.gpu,memory.used,clocks.gr,power.draw --format=csv,noheader,nounits
    $sample = @{ utcMillis = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds(); processes = $java; engines = $engines; memory = $memory; nvidia = $device; samplerMillis = $timer.ElapsedMilliseconds }
    $sample | ConvertTo-Json -Depth 5 -Compress | Add-Content -LiteralPath $OutputPath -Encoding utf8
    $remaining = [Math]::Max(1, 2000 - $timer.ElapsedMilliseconds)
    Start-Sleep -Milliseconds $remaining
}
