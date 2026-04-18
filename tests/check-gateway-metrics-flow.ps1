param(
    [string]$GatewayBaseUrl = "http://localhost:8088",
    [int]$OrderCallCount = 10,
    [int]$UserCallCount = 10,
    [int]$MaxUserWaitSeconds = 20
)

$ErrorActionPreference = "Stop"

function Get-ErrorBody {
    param($Exception)

    if (-not $Exception.Response) {
        return ""
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        if ($null -eq $stream) {
            return ""
        }

        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        $reader.Close()
        return $body
    } catch {
        return ""
    }
}

function Invoke-ApiJson {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers = @{},
        $Body = $null,
        [switch]$AllowNonSuccess
    )

    $uri = "$GatewayBaseUrl$Path"
    $jsonBody = $null
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 10
    }

    try {
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $Headers -ContentType "application/json" -Body $jsonBody -UseBasicParsing
        $parsed = $null
        if ($resp.Content) {
            try {
                $parsed = $resp.Content | ConvertFrom-Json
            } catch {
                $parsed = $resp.Content
            }
        }

        return [PSCustomObject]@{
            StatusCode = [int]$resp.StatusCode
            Body = $parsed
            Raw = $resp.Content
        }
    } catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }

        $raw = Get-ErrorBody $_.Exception
        $parsed = $null
        if ($raw) {
            try {
                $parsed = $raw | ConvertFrom-Json
            } catch {
                $parsed = $raw
            }
        }

        if (-not $AllowNonSuccess) {
            throw "[FAIL] $Method $Path (status=$statusCode) body=$raw"
        }

        return [PSCustomObject]@{
            StatusCode = $statusCode
            Body = $parsed
            Raw = $raw
        }
    }
}

function Get-JwtPayload {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Token
    )

    $parts = $Token.Split(".")
    if ($parts.Length -lt 2) {
        throw "Invalid JWT format."
    }

    $payload = $parts[1].Replace("-", "+").Replace("_", "/")
    switch ($payload.Length % 4) {
        0 { }
        2 { $payload += "==" }
        3 { $payload += "=" }
        default { throw "Invalid JWT payload." }
    }

    $bytes = [Convert]::FromBase64String($payload)
    $json = [System.Text.Encoding]::UTF8.GetString($bytes)
    return $json | ConvertFrom-Json
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$email = "gw.metrics.$suffix@example.com"
$password = "Passw0rd!"

Write-Host "== Gateway Metrics Flow =="
Write-Host "Gateway: $GatewayBaseUrl"
Write-Host "Email: $email"
Write-Host "OrderCallCount: $OrderCallCount"
Write-Host "UserCallCount: $UserCallCount"

Write-Host "`n[1/4] signup"
$signupResp = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/signup" -Body @{
    email = $email
    password = $password
}
Write-Host "status=$($signupResp.StatusCode)"

Write-Host "`n[2/4] login"
$loginResp = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/login" -Body @{
    email = $email
    password = $password
}
Write-Host "status=$($loginResp.StatusCode)"

$accessToken = $loginResp.Body.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "Login succeeded but accessToken is empty."
}

$claims = Get-JwtPayload -Token $accessToken
$userId = [long]$claims.sub
if ($userId -le 0) {
    throw "Invalid userId in token claims."
}

$authHeaders = @{
    Authorization = "Bearer $accessToken"
}

Write-Host "`n[3/4] wait for user projection"
$userReady = $false
for ($i = 1; $i -le $MaxUserWaitSeconds; $i++) {
    $userResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/users/$userId" -Headers $authHeaders -AllowNonSuccess
    if ($userResp.StatusCode -eq 200) {
        $userReady = $true
        break
    }
    Start-Sleep -Seconds 1
}

if (-not $userReady) {
    throw "User projection was not ready in $MaxUserWaitSeconds seconds."
}
Write-Host "status=200"

Write-Host "`n[4/4] execute burst calls"
$orderStatusCounts = @{}
$userStatusCounts = @{}

for ($i = 1; $i -le $OrderCallCount; $i++) {
    $orderResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/orders?userId=$userId&page=0&limit=20" -Headers $authHeaders -AllowNonSuccess
    $statusKey = [string]$orderResp.StatusCode
    if (-not $orderStatusCounts.ContainsKey($statusKey)) {
        $orderStatusCounts[$statusKey] = 0
    }
    $orderStatusCounts[$statusKey]++
}

for ($i = 1; $i -le $UserCallCount; $i++) {
    $userResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/users/$userId" -Headers $authHeaders -AllowNonSuccess
    $statusKey = [string]$userResp.StatusCode
    if (-not $userStatusCounts.ContainsKey($statusKey)) {
        $userStatusCounts[$statusKey] = 0
    }
    $userStatusCounts[$statusKey]++
}

Write-Host "`n== Summary =="
[PSCustomObject]@{
    gateway = $GatewayBaseUrl
    email = $email
    userId = $userId
    signupStatus = $signupResp.StatusCode
    loginStatus = $loginResp.StatusCode
    orderCallCount = $OrderCallCount
    userCallCount = $UserCallCount
    orderStatusCounts = $orderStatusCounts
    userStatusCounts = $userStatusCounts
} | ConvertTo-Json -Depth 10
