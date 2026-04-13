param(
    [string]$GatewayBaseUrl = "http://localhost:8088",
    [string]$Email = "order-tester@example.com",
    [string]$Password = "Passw0rd!",
    [long]$StoreId = 3,
    [string]$SampleDataPath = "$PSScriptRoot\..\..\Store\sample-data-aekiori-mawang.json",
    [int]$Count = 5
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($Count -lt 1) {
    throw "Count must be greater than 0."
}

function Get-ErrorResponseText {
    param(
        [Parameter(Mandatory = $true)]
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return $ErrorRecord.Exception.Message
    }

    try {
        $stream = $response.GetResponseStream()
        if ($null -eq $stream) {
            return $ErrorRecord.Exception.Message
        }

        $reader = New-Object System.IO.StreamReader($stream)
        $text = $reader.ReadToEnd()
        $reader.Close()
        return $text
    } catch {
        return $ErrorRecord.Exception.Message
    }
}

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST", "PATCH", "PUT", "DELETE")]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [hashtable]$Headers,
        $Body
    )

    $params = @{
        Method      = $Method
        Uri         = $Uri
        ErrorAction = "Stop"
    }

    if ($null -ne $Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 20)
    }

    try {
        $data = Invoke-RestMethod @params
        return [PSCustomObject]@{
            Success    = $true
            StatusCode = 200
            Data       = $data
            ErrorText  = $null
        }
    } catch {
        $statusCode = 0
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }

        return [PSCustomObject]@{
            Success    = $false
            StatusCode = $statusCode
            Data       = $null
            ErrorText  = (Get-ErrorResponseText -ErrorRecord $_)
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
        default { throw "Invalid JWT payload encoding." }
    }

    $bytes = [System.Convert]::FromBase64String($payload)
    $json = [System.Text.Encoding]::UTF8.GetString($bytes)
    return $json | ConvertFrom-Json
}

$base = $GatewayBaseUrl.TrimEnd("/")
$loginBody = @{
    email    = $Email
    password = $Password
}

Write-Host "== Gateway Order Create Test =="
Write-Host "Gateway : $base"
Write-Host "Email   : $Email"
Write-Host "StoreId : $StoreId"
Write-Host "Sample  : $SampleDataPath"
Write-Host ""

$authResult = Invoke-JsonApi -Method "POST" -Uri "$base/api/v1/auth/login" -Body $loginBody
if (-not $authResult.Success) {
    Write-Host "[INFO] login failed (status=$($authResult.StatusCode)). trying signup..."
    $authResult = Invoke-JsonApi -Method "POST" -Uri "$base/api/v1/auth/signup" -Body $loginBody
    if (-not $authResult.Success) {
        Write-Host "[FAIL] signup failed. status=$($authResult.StatusCode)"
        if ($authResult.ErrorText) {
            Write-Host $authResult.ErrorText
        }
        exit 1
    }
}

$accessToken = [string]$authResult.Data.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    Write-Host "[FAIL] accessToken is missing in auth response."
    exit 1
}

$claims = Get-JwtPayload -Token $accessToken
$userId = [long]$claims.sub

Write-Host "[OK] authenticated. userId=$userId"
Write-Host ""

if (-not (Test-Path -LiteralPath $SampleDataPath)) {
    Write-Host "[FAIL] sample file not found: $SampleDataPath"
    exit 1
}

$sample = Get-Content -LiteralPath $SampleDataPath -Raw -Encoding UTF8 | ConvertFrom-Json
$menuMap = @{}
foreach ($group in $sample.store.menuGroups) {
    foreach ($menu in $group.menus) {
        $menuMap[$menu.name] = [PSCustomObject]@{
            Id    = [long]$menu.id
            Name  = [string]$menu.name
            Price = [int]$menu.price
        }
    }
}

function Get-SampleMenu {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not $menuMap.ContainsKey($Name)) {
        throw "Menu was not found in sample JSON: $Name"
    }

    return $menuMap[$Name]
}

function New-OrderItemFromSample {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MenuName,
        [Parameter(Mandatory = $true)]
        [int]$Quantity
    )

    $menu = Get-SampleMenu -Name $MenuName
    return @{
        menuId    = $menu.Id
        menuName  = $menu.Name
        unitPrice = $menu.Price
        quantity  = $Quantity
    }
}

$orderTemplates = @(
    @{
        storeId         = $StoreId
        deliveryAddress = "서울 군포시 산본로 100"
        usedPointAmount = 0
        items           = @(
            (New-OrderItemFromSample -MenuName "마왕치킨" -Quantity 1)
        )
    },
    @{
        storeId         = $StoreId
        deliveryAddress = "서울 군포시 산본로 101"
        usedPointAmount = 1000
        items           = @(
            (New-OrderItemFromSample -MenuName "마왕 순살치킨" -Quantity 1),
            (New-OrderItemFromSample -MenuName "콜라 1.25L" -Quantity 1)
        )
    },
    @{
        storeId         = $StoreId
        deliveryAddress = "서울 군포시 산본로 102"
        usedPointAmount = 0
        items           = @(
            (New-OrderItemFromSample -MenuName "마왕치킨" -Quantity 1),
            (New-OrderItemFromSample -MenuName "치즈볼 (5개)" -Quantity 1)
        )
    },
    @{
        storeId         = $StoreId
        deliveryAddress = "서울 군포시 산본로 103"
        usedPointAmount = 500
        items           = @(
            (New-OrderItemFromSample -MenuName "마왕 순살치킨" -Quantity 1),
            (New-OrderItemFromSample -MenuName "주먹밥" -Quantity 1)
        )
    },
    @{
        storeId         = $StoreId
        deliveryAddress = "서울 군포시 산본로 104"
        usedPointAmount = 0
        items           = @(
            (New-OrderItemFromSample -MenuName "마왕치킨" -Quantity 1),
            (New-OrderItemFromSample -MenuName "닭껍질튀김" -Quantity 1)
        )
    }
)

$limit = [Math]::Min($Count, $orderTemplates.Count)
if ($Count -gt $orderTemplates.Count) {
    Write-Host "[WARN] requested count=$Count, using max=$limit"
}

$success = 0
$failed = 0

for ($i = 0; $i -lt $limit; $i++) {
    $template = $orderTemplates[$i]
    $idempotencyKey = [Guid]::NewGuid().ToString()

    $headers = @{
        Authorization     = "Bearer $accessToken"
        "X-Idempotency-Key" = $idempotencyKey
    }

    $body = @{
        userId          = $userId
        storeId         = [long]$template.storeId
        deliveryAddress = [string]$template.deliveryAddress
        usedPointAmount = [int]$template.usedPointAmount
        items           = $template.items
    }

    $result = Invoke-JsonApi -Method "POST" -Uri "$base/api/v1/orders" -Headers $headers -Body $body
    if ($result.Success) {
        $orderId = $result.Data.orderId
        Write-Host ("[OK]  #{0} orderId={1} idempotencyKey={2}" -f ($i + 1), $orderId, $idempotencyKey)
        $success++
    } else {
        Write-Host ("[FAIL] #{0} status={1} idempotencyKey={2}" -f ($i + 1), $result.StatusCode, $idempotencyKey)
        if ($result.ErrorText) {
            Write-Host ("       {0}" -f $result.ErrorText)
        }
        $failed++
    }
}

Write-Host ""
Write-Host ("Done. success={0}, failed={1}" -f $success, $failed)

if ($failed -gt 0) {
    exit 1
}
