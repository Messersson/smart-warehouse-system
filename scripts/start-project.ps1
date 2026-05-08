param(
    [switch]$Build,
    [switch]$SkipBuild,
    [switch]$FollowLogs,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$buildStateFile = Join-Path $projectRoot '.start-project-state.json'
Set-Location $projectRoot

if ($Build -and $SkipBuild) {
    throw "Parameters -Build and -SkipBuild cannot be used together."
}

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Gray
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[ OK ] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Invoke-ProcessCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    $stdoutFile = [System.IO.Path]::GetTempFileName()
    $stderrFile = [System.IO.Path]::GetTempFileName()

    try {
        $process = Start-Process -FilePath $FilePath `
            -ArgumentList $Arguments `
            -NoNewWindow `
            -Wait `
            -PassThru `
            -RedirectStandardOutput $stdoutFile `
            -RedirectStandardError $stderrFile

        $output = @()
        if (Test-Path $stdoutFile) {
            $output += Get-Content $stdoutFile
        }
        if (Test-Path $stderrFile) {
            $output += Get-Content $stderrFile
        }

        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output   = $output
        }
    } finally {
        Remove-Item $stdoutFile -ErrorAction SilentlyContinue
        Remove-Item $stderrFile -ErrorAction SilentlyContinue
    }
}

function Get-RelativePath {
    param([string]$Path)

    $uri = [System.Uri]((Resolve-Path $projectRoot).Path.TrimEnd('\') + '\')
    $targetUri = [System.Uri](Resolve-Path $Path).Path
    return [System.Uri]::UnescapeDataString($uri.MakeRelativeUri($targetUri).ToString()).Replace('/', '\')
}

function Get-BuildTrackedFiles {
    $tracked = New-Object System.Collections.Generic.List[string]

    $rootFiles = @(
        (Join-Path $projectRoot 'docker-compose.yml')
    )
    foreach ($file in $rootFiles) {
        if (Test-Path $file) {
            $tracked.Add((Resolve-Path $file).Path)
        }
    }

    $sourceRoots = @(
        (Join-Path $projectRoot 'backend'),
        (Join-Path $projectRoot 'frontend')
    )
    $excludedDirectories = @('target', 'node_modules', 'dist')

    foreach ($root in $sourceRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        Get-ChildItem -Path $root -Recurse -File | Where-Object {
            $relativePath = Get-RelativePath -Path $_.FullName
            -not ($excludedDirectories | Where-Object { $relativePath -match "(^|\\)$_(\\|$)" })
        } | ForEach-Object {
            $tracked.Add($_.FullName)
        }
    }

    return $tracked | Sort-Object -Unique
}

function Get-BuildFingerprint {
    $builder = New-Object System.Text.StringBuilder

    foreach ($path in Get-BuildTrackedFiles) {
        $item = Get-Item $path
        $relativePath = Get-RelativePath -Path $item.FullName
        [void]$builder.AppendLine("$relativePath|$($item.Length)|$($item.LastWriteTimeUtc.Ticks)")
    }

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($builder.ToString())
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString($sha256.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha256.Dispose()
    }
}

function Read-BuildState {
    if (-not (Test-Path $buildStateFile)) {
        return $null
    }

    try {
        return Get-Content $buildStateFile -Raw | ConvertFrom-Json
    } catch {
        Write-Warn "Build state file is unreadable. The launcher will rebuild images."
        return $null
    }
}

function Write-BuildState {
    param([string]$Fingerprint)

    $state = [pscustomobject]@{
        fingerprint = $Fingerprint
        updatedAt   = (Get-Date).ToUniversalTime().ToString('o')
    }
    $state | ConvertTo-Json | Set-Content -Path $buildStateFile
}

function Get-BuildDecision {
    if ($Build) {
        return [pscustomobject]@{
            ShouldBuild = $true
            Fingerprint = Get-BuildFingerprint
            Reason      = 'Forced by -Build.'
        }
    }

    if ($SkipBuild) {
        return [pscustomobject]@{
            ShouldBuild = $false
            Fingerprint = $null
            Reason      = 'Skipped by -SkipBuild.'
        }
    }

    $fingerprint = Get-BuildFingerprint
    $state = Read-BuildState
    if ($null -eq $state -or [string]::IsNullOrWhiteSpace($state.fingerprint)) {
        return [pscustomobject]@{
            ShouldBuild = $true
            Fingerprint = $fingerprint
            Reason      = 'No previous build state was found.'
        }
    }

    if ($state.fingerprint -ne $fingerprint) {
        return [pscustomobject]@{
            ShouldBuild = $true
            Fingerprint = $fingerprint
            Reason      = 'Source files changed since the last successful launcher build.'
        }
    }

    return [pscustomobject]@{
        ShouldBuild = $false
        Fingerprint = $fingerprint
        Reason      = 'No tracked source changes were detected.'
    }
}

function Resolve-ComposeCommand {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker command was not found. Please install Docker Desktop first."
    }

    $composeVersion = Invoke-ProcessCommand -FilePath 'docker' -Arguments @('compose', 'version')
    if ($composeVersion.ExitCode -eq 0) {
        return @('docker', 'compose')
    }

    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        return @('docker-compose')
    }

    throw "Docker Compose command was not found."
}

function Invoke-Compose {
    param(
        [string[]]$Arguments,
        [switch]$Capture,
        [switch]$IgnoreErrors
    )

    if (-not $Capture -and ($Arguments -contains '-f')) {
        if ($script:ComposeCommand.Count -eq 2) {
            & $script:ComposeCommand[0] $script:ComposeCommand[1] @Arguments
        } else {
            & $script:ComposeCommand[0] @Arguments
        }
        return
    }

    $result = $null
    if ($script:ComposeCommand.Count -eq 2) {
        $result = Invoke-ProcessCommand -FilePath $script:ComposeCommand[0] -Arguments (@($script:ComposeCommand[1]) + $Arguments)
    } else {
        $result = Invoke-ProcessCommand -FilePath $script:ComposeCommand[0] -Arguments $Arguments
    }
    $output = $result.Output
    $exitCode = $result.ExitCode

    if ($Capture) {
        if (-not $IgnoreErrors -and $exitCode -ne 0) {
            throw (($output | Out-String).Trim())
        }

        return (($output | Out-String).Trim())
    }

    $output | ForEach-Object {
        Write-Host $_
    }

    if (-not $IgnoreErrors -and $exitCode -ne 0) {
        $message = (($output | Out-String).Trim())
        if ([string]::IsNullOrWhiteSpace($message)) {
            $message = "Docker Compose command failed: $($Arguments -join ' ')"
        }
        throw $message
    }
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs = 1500
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $null = $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Test-HttpEndpoint {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
        return [pscustomobject]@{
            Ready  = $true
            Detail = "HTTP $($response.StatusCode)"
        }
    } catch {
        return [pscustomobject]@{
            Ready  = $false
            Detail = $_.Exception.Message
        }
    }
}

function Get-ContainerState {
    param([string]$ServiceName)

    $containerName = switch ($ServiceName) {
        'mysql' { 'warehouse-mysql' }
        'backend' { 'warehouse-backend' }
        'frontend' { 'warehouse-frontend' }
        default { '' }
    }

    if ([string]::IsNullOrWhiteSpace($containerName)) {
        return [pscustomobject]@{
            Service       = $ServiceName
            ContainerId   = ''
            ContainerName = ''
            Status        = 'not-created'
            Health        = 'unknown'
        }
    }

    $inspectResult = Invoke-ProcessCommand -FilePath 'docker' -Arguments @('inspect', '--format', '{{.State.Id}}|{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.Name}}', $containerName)
    $inspect = (($inspectResult.Output | Out-String).Trim())
    if ($inspectResult.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($inspect)) {
        return [pscustomobject]@{
            Service       = $ServiceName
            ContainerId   = ''
            ContainerName = $containerName
            Status        = 'unknown'
            Health        = 'unknown'
        }
    }

    $parts = $inspect.Trim() -split '\|'
    return [pscustomobject]@{
        Service       = $ServiceName
        ContainerId   = $parts[0]
        ContainerName = ($parts[3] -replace '^/', '')
        Status        = $parts[1]
        Health        = $parts[2]
    }
}

function Get-ServiceStatus {
    param([pscustomobject]$Definition)

    $container = Get-ContainerState -ServiceName $Definition.Service

    if ($Definition.Service -eq 'mysql') {
        $portReady = Test-TcpPort -HostName '127.0.0.1' -Port 3307
        $ready = ($container.Health -eq 'healthy') -or $portReady
        $statusText = if ($container.Status -eq 'unknown' -and $ready) { 'running' } else { $container.Status }
        $healthText = if ($container.Health -eq 'unknown' -and $ready) { 'healthy' } else { $container.Health }
        return [pscustomobject]@{
            Name          = $Definition.Name
            Service       = $Definition.Service
            ContainerName = $container.ContainerName
            Status        = $statusText
            Health        = $healthText
            Ready         = $ready
            Detail        = if ($ready) { 'MySQL is accepting connections on 3307' } else { 'Waiting for MySQL startup' }
        }
    }

    $httpResult = Test-HttpEndpoint -Url $Definition.Url
    $ready = ($container.Health -eq 'healthy') -or $httpResult.Ready
    $statusText = if ($container.Status -eq 'unknown' -and $ready) { 'running' } else { $container.Status }
    $healthText = if ($container.Health -eq 'unknown' -and $ready) { 'healthy' } else { $container.Health }
    return [pscustomobject]@{
        Name          = $Definition.Name
        Service       = $Definition.Service
        ContainerName = $container.ContainerName
        Status        = $statusText
        Health        = $healthText
        Ready         = $ready
        Detail        = if ($ready) { "$($Definition.Url) -> $($httpResult.Detail)" } else { $httpResult.Detail }
    }
}

function Show-Summary {
    param([object[]]$Statuses)

    Write-Section "Startup Summary"
    foreach ($status in $Statuses) {
        $containerName = if ([string]::IsNullOrWhiteSpace($status.ContainerName)) { '-' } else { $status.ContainerName }
        $line = "{0,-10} status={1}, health={2}, container={3}" -f $status.Name, $status.Status, $status.Health, $containerName
        if ($status.Ready) {
            Write-Ok "$line, detail=$($status.Detail)"
        } else {
            Write-Warn "$line, detail=$($status.Detail)"
        }
    }
}

function Show-FailureLogs {
    param([object[]]$Statuses)

    foreach ($status in $Statuses | Where-Object { -not $_.Ready }) {
        Write-Section "Recent logs: $($status.Name)"
        Invoke-Compose -Arguments @('logs', '--tail', '50', $status.Service) -IgnoreErrors
    }
}

$script:ComposeCommand = Resolve-ComposeCommand

Write-Section "Warehouse System Launcher"
Write-Info "Project root: $projectRoot"
Write-Info "Compose command: $($script:ComposeCommand -join ' ')"
Write-Info "Start time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

Write-Section "Preflight Check"
$dockerVersion = Invoke-ProcessCommand -FilePath 'docker' -Arguments @('version')
if ($dockerVersion.ExitCode -ne 0) {
    throw (($dockerVersion.Output | Out-String).Trim())
}
Write-Ok "Docker daemon is available"

Write-Section "Starting Services"
$upArguments = @('up', '-d')
$buildDecision = Get-BuildDecision
$buildFallbackUsed = $false
if ($buildDecision.ShouldBuild) {
    $upArguments += '--build'
    Write-Info "Using docker compose up -d --build"
    Write-Info "Build reason: $($buildDecision.Reason)"
} else {
    Write-Info "Using docker compose up -d"
    Write-Info "Build reason: $($buildDecision.Reason)"
}
try {
    Invoke-Compose -Arguments $upArguments
} catch {
    if ($buildDecision.ShouldBuild -and -not $Build) {
        Write-Warn "Image rebuild failed. The launcher will try existing images once before stopping."
        Write-Warn $_.Exception.Message.Split([Environment]::NewLine)[0]
        $buildFallbackUsed = $true
        Invoke-Compose -Arguments @('up', '-d')
    } else {
        throw
    }
}

$definitions = @(
    [pscustomobject]@{ Name = 'MySQL'; Service = 'mysql'; Url = '' },
    [pscustomobject]@{ Name = 'Backend'; Service = 'backend'; Url = 'http://127.0.0.1:18080/actuator/health' },
    [pscustomobject]@{ Name = 'Frontend'; Service = 'frontend'; Url = 'http://127.0.0.1/' }
)

Write-Section "Waiting For Readiness"
Write-Info "Timeout: $TimeoutSeconds seconds"

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$statuses = @()
do {
    $statuses = $definitions | ForEach-Object { Get-ServiceStatus -Definition $_ }
    if (@($statuses | Where-Object { -not $_.Ready }).Count -eq 0) {
        break
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

Show-Summary -Statuses $statuses

if (@($statuses | Where-Object { -not $_.Ready }).Count -gt 0) {
    Write-Fail "One or more services did not become ready within $TimeoutSeconds seconds."
    Show-FailureLogs -Statuses $statuses
    exit 1
}

if ($buildDecision.ShouldBuild -and -not $buildFallbackUsed -and $null -ne $buildDecision.Fingerprint) {
    Write-BuildState -Fingerprint $buildDecision.Fingerprint
    Write-Info "Updated build state: $buildStateFile"
}

Write-Section "Access Information"
Write-Ok "MySQL    -> 127.0.0.1:3307"
Write-Ok "Backend  -> http://127.0.0.1:18080/actuator/health"
Write-Ok "Frontend -> http://127.0.0.1/"

if ($FollowLogs) {
    Write-Section "Following Logs"
    Invoke-Compose -Arguments @('logs', '-f')
}
