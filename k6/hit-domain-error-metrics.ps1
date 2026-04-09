param(
    [int]$CallsPerStatus = 10,
    [string]$GatewayBaseUrl = "http://localhost:8088",
    [string]$AuthBaseUrl = "http://localhost:8084",
    [string]$UserBaseUrl = "http://localhost:8082",
    [string]$OrderBaseUrl = "http://localhost:8081",
    [int]$SleepMilliseconds = 30
)

$ErrorActionPreference = "Stop"

function Get-HttpStatusCode {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -Method GET -UseBasicParsing -TimeoutSec 5
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }

        # 연결 실패(서비스 미기동 등)
        return -1
    }
}

function Add-Count {
    param(
        [hashtable]$Counter,
        [int]$StatusCode
    )

    $key = [string]$StatusCode
    if (-not $Counter.ContainsKey($key)) {
        $Counter[$key] = 0
    }
    $Counter[$key]++
}

function Invoke-EndpointBurst {
    param(
        [string]$Domain,
        [string]$Label,
        [string]$Uri,
        [int]$Repeat,
        [hashtable]$Counter
    )

    Write-Host "[$Domain] $Label => $Uri"
    for ($i = 1; $i -le $Repeat; $i++) {
        $status = Get-HttpStatusCode -Uri $Uri
        Add-Count -Counter $Counter -StatusCode $status
        if ($SleepMilliseconds -gt 0) {
            Start-Sleep -Milliseconds $SleepMilliseconds
        }
    }
}

$targets = @(
    [PSCustomObject]@{
        Domain = "gateway"
        Url400 = "$GatewayBaseUrl/api/v1/gateway/test/status/400"
        Url500 = "$GatewayBaseUrl/api/v1/gateway/test/status/500"
    },
    [PSCustomObject]@{
        Domain = "auth"
        Url400 = "$AuthBaseUrl/api/v1/auth/test/status/400"
        Url500 = "$AuthBaseUrl/api/v1/auth/test/status/500"
    },
    [PSCustomObject]@{
        Domain = "user"
        Url400 = "$UserBaseUrl/api/v1/users/test/status/400"
        Url500 = "$UserBaseUrl/api/v1/users/test/status/500"
    },
    [PSCustomObject]@{
        Domain = "order"
        Url400 = "$OrderBaseUrl/api/v1/orders/test/status/400"
        Url500 = "$OrderBaseUrl/api/v1/orders/test/status/500"
    }
)

Write-Host "== Domain Error Metrics Burst =="
Write-Host "CallsPerStatus: $CallsPerStatus"
Write-Host "Targets: gateway=$GatewayBaseUrl auth=$AuthBaseUrl user=$UserBaseUrl order=$OrderBaseUrl"
Write-Host ""

$summary = [ordered]@{}

foreach ($target in $targets) {
    $counter = @{}

    Invoke-EndpointBurst `
        -Domain $target.Domain `
        -Label "400 x $CallsPerStatus" `
        -Uri $target.Url400 `
        -Repeat $CallsPerStatus `
        -Counter $counter

    Invoke-EndpointBurst `
        -Domain $target.Domain `
        -Label "500 x $CallsPerStatus" `
        -Uri $target.Url500 `
        -Repeat $CallsPerStatus `
        -Counter $counter

    $summary[$target.Domain] = $counter
    Write-Host ""
}

Write-Host "== Summary =="
[PSCustomObject]@{
    callsPerStatus = $CallsPerStatus
    totalRequests = $CallsPerStatus * 2 * $targets.Count
    statusesByDomain = $summary
} | ConvertTo-Json -Depth 10
