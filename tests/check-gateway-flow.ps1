param(
    [string]$GatewayBaseUrl = "http://localhost:8088",
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
$email = "gw.flow.$suffix@example.com"
$password = "Passw0rd!"

Write-Host "== Gateway E2E Flow =="
Write-Host "Gateway: $GatewayBaseUrl"
Write-Host "Email: $email"

Write-Host "`n[1/5] signup"
$signupResp = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/signup" -Body @{
    email = $email
    password = $password
}
Write-Host "status=$($signupResp.StatusCode)"

Write-Host "`n[2/5] login"
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

Write-Host "`n[3/5] user get"
$userResp = $null
for ($i = 1; $i -le $MaxUserWaitSeconds; $i++) {
    $userResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/users/$userId" -Headers $authHeaders -AllowNonSuccess
    if ($userResp.StatusCode -eq 200) {
        break
    }

    Start-Sleep -Seconds 1
}

if ($null -eq $userResp -or $userResp.StatusCode -ne 200) {
    throw "User projection was not ready in $MaxUserWaitSeconds seconds."
}
Write-Host "status=$($userResp.StatusCode)"

$idempotencyKey = [guid]::NewGuid().Guid
$orderHeaders = @{
    Authorization = "Bearer $accessToken"
    "X-Idempotency-Key" = $idempotencyKey
}

Write-Host "`n[4/5] order create"
$orderCreateResp = Invoke-ApiJson -Method "POST" -Path "/api/v1/orders" -Headers $orderHeaders -Body @{
    userId = $userId
    storeId = 1001
    deliveryAddress = "Test Address 1"
    usedPointAmount = 0
    items = @(
        @{
            menuId = 9001
            menuName = "Menu A"
            unitPrice = 12000
            quantity = 1
        },
        @{
            menuId = 9002
            menuName = "Menu B"
            unitPrice = 2000
            quantity = 2
        }
    )
}
Write-Host "status=$($orderCreateResp.StatusCode)"

$orderId = [long]$orderCreateResp.Body.orderId
if ($orderId -le 0) {
    throw "Order create succeeded but orderId is invalid."
}

Write-Host "`n[5/5] order get"
$orderGetResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/orders/$orderId" -Headers $authHeaders
Write-Host "status=$($orderGetResp.StatusCode)"

Write-Host "`n== Summary =="
[PSCustomObject]@{
    gateway = $GatewayBaseUrl
    email = $email
    userId = $userId
    orderId = $orderId
    idempotencyKey = $idempotencyKey
    signupStatus = $signupResp.StatusCode
    loginStatus = $loginResp.StatusCode
    userGetStatus = $userResp.StatusCode
    orderCreateStatus = $orderCreateResp.StatusCode
    orderGetStatus = $orderGetResp.StatusCode
} | ConvertTo-Json

