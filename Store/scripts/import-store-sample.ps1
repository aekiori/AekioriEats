param(
    [string]$StoreBaseUrl = "http://localhost:8085",
    [string]$JsonPath = (Join-Path $PSScriptRoot "..\sample-data-aekiori-mawang.json"),
    [long]$OwnerUserId = 0,
    [string]$UserRole = "OWNER",
    [switch]$SkipStoreHours,
    [switch]$SkipStoreHolidays,
    [switch]$SkipMenuReset
)

$ErrorActionPreference = "Stop"

function Read-ErrorBody {
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
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [hashtable]$Headers = @{},
        $Body = $null,
        [switch]$AllowNonSuccess
    )

    $uri = "$StoreBaseUrl$Path"
    $requestBody = $null
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 30
        $requestBody = [System.Text.Encoding]::UTF8.GetBytes($jsonBody)
    }

    try {
        $resp = Invoke-WebRequest `
            -Uri $uri `
            -Method $Method `
            -Headers $Headers `
            -ContentType "application/json; charset=utf-8" `
            -Body $requestBody `
            -UseBasicParsing

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

        $raw = Read-ErrorBody $_.Exception
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

function Resolve-CategoryIds {
    param([array]$RequestedCategories)

    if ($null -eq $RequestedCategories -or $RequestedCategories.Count -eq 0) {
        return @()
    }

    $categoryResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/stores/categories"
    $allCategories = @($categoryResp.Body)

    $byId = @{}
    $byName = @{}
    foreach ($category in $allCategories) {
        $id = [long]$category.id
        $name = ([string]$category.name).Trim()
        $byId["$id"] = $id
        if (-not [string]::IsNullOrWhiteSpace($name)) {
            $byName[$name.ToLowerInvariant()] = $id
        }
    }

    # legacy sample category IDs -> currently seeded category IDs
    $categoryIdAliases = @{
        "11001" = 1   # Chicken
        "11002" = 9   # NightFood
        "11003" = 1   # fallback
    }

    $resolved = New-Object System.Collections.Generic.List[long]
    foreach ($requested in $RequestedCategories) {
        $resolvedId = $null

        if (
            ($requested.PSObject.Properties.Name -contains "id") `
            -and $null -ne $requested.id
        ) {
            $requestedId = [long]$requested.id
            $requestedIdKey = "$requestedId"
            if ($byId.ContainsKey($requestedIdKey)) {
                $resolvedId = [long]$byId[$requestedIdKey]
            } elseif ($categoryIdAliases.ContainsKey($requestedIdKey)) {
                $aliasedId = [long]$categoryIdAliases[$requestedIdKey]
                if ($byId.ContainsKey("$aliasedId")) {
                    $resolvedId = $aliasedId
                }
            }
        }

        if (
            $null -eq $resolvedId `
            -and ($requested.PSObject.Properties.Name -contains "name") `
            -and $null -ne $requested.name
        ) {
            $requestedName = ([string]$requested.name).Trim().ToLowerInvariant()
            if ($byName.ContainsKey($requestedName)) {
                $resolvedId = [long]$byName[$requestedName]
            }
        }

        if ($null -eq $resolvedId) {
            Write-Warning "Category mapping skipped. name='$($requested.name)', id='$($requested.id)'."
            continue
        }

        if (-not $resolved.Contains($resolvedId)) {
            $resolved.Add($resolvedId)
        }
    }

    if ($resolved.Count -eq 0) {
        throw "Category mapping failed. No matching category IDs were resolved from sample json."
    }

    return @($resolved.ToArray())
}

function Normalize-TagNames {
    param([array]$Tags)

    if ($null -eq $Tags -or $Tags.Count -eq 0) {
        return @()
    }

    $names = New-Object System.Collections.Generic.List[string]
    foreach ($tag in $Tags) {
        if ($null -eq $tag) {
            continue
        }

        $name = [string]$tag.name
        if ([string]::IsNullOrWhiteSpace($name)) {
            continue
        }

        $trimmed = $name.Trim()
        if (-not $names.Contains($trimmed)) {
            $names.Add($trimmed)
        }
    }

    return @($names.ToArray())
}

if (-not (Test-Path -LiteralPath $JsonPath)) {
    throw "Sample json not found: $JsonPath"
}

$rawJson = Get-Content -LiteralPath $JsonPath -Encoding UTF8 -Raw
$sample = $rawJson | ConvertFrom-Json
if ($null -eq $sample.store) {
    throw "Invalid sample json format. Missing 'store'."
}

if ($OwnerUserId -le 0) {
    $OwnerUserId = [long]$sample.store.ownerUserId
}
if ($OwnerUserId -le 0) {
    throw "OwnerUserId is required. Provide -OwnerUserId or set store.ownerUserId in json."
}

$headers = @{
    "X-User-Id" = "$OwnerUserId"
    "X-User-Role" = $UserRole
}

$storeName = [string]$sample.store.name
$storeLogoUrl = $sample.store.images.storeLogoUrl
$resolvedCategoryIds = Resolve-CategoryIds -RequestedCategories @($sample.store.categories)

$deliveryPolicyBody = @{
    minOrderAmount = [int]$sample.store.deliveryPolicy.minOrderAmount
    deliveryTip = [int]$sample.store.deliveryPolicy.deliveryTip
}

$upsertBody = @{
    name = $storeName
    categoryIds = $resolvedCategoryIds
    deliveryPolicy = $deliveryPolicyBody
    storeLogoUrl = $storeLogoUrl
}

Write-Host "== Store sample import start =="
Write-Host "Store API: $StoreBaseUrl"
Write-Host "JSON: $JsonPath"
Write-Host "OwnerUserId: $OwnerUserId (Role: $UserRole)"

$ownerStoresResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/owner/stores" -Headers $headers
$ownerStores = @($ownerStoresResp.Body)
$existingStore = $ownerStores | Where-Object { $_.name -eq $storeName } | Select-Object -First 1
if ($null -eq $existingStore -and -not [string]::IsNullOrWhiteSpace($storeLogoUrl)) {
    $existingStore = $ownerStores | Where-Object { $_.storeLogoUrl -eq $storeLogoUrl } | Select-Object -First 1
}

$storeId = $null
if ($null -ne $existingStore) {
    $storeId = [long]$existingStore.storeId
    Write-Host "[Store] found existing store. storeId=$storeId (update)"
    [void](Invoke-ApiJson -Method "PUT" -Path "/api/v1/owner/stores/$storeId" -Headers $headers -Body $upsertBody)
} else {
    Write-Host "[Store] create new store"
    $createdStore = Invoke-ApiJson `
        -Method "POST" `
        -Path "/api/v1/owner/stores" `
        -Headers $headers `
        -Body $upsertBody `
        -AllowNonSuccess

    if ($createdStore.StatusCode -ge 200 -and $createdStore.StatusCode -lt 300) {
        $storeId = [long]$createdStore.Body.storeId
    } elseif (
        $createdStore.StatusCode -eq 409 `
        -and $null -ne $createdStore.Body `
        -and $createdStore.Body.code -eq "STORE_NAME_ALREADY_EXISTS_FOR_OWNER"
    ) {
        $ownerStoresRetryResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/owner/stores" -Headers $headers
        $ownerStoresRetry = @($ownerStoresRetryResp.Body)
        $fallbackStore = $ownerStoresRetry | Select-Object -First 1
        if ($null -eq $fallbackStore) {
            throw "Store already exists but fallback lookup failed."
        }

        $storeId = [long]$fallbackStore.storeId
        Write-Host "[Store] create conflict -> fallback existing storeId=$storeId"
        [void](Invoke-ApiJson -Method "PUT" -Path "/api/v1/owner/stores/$storeId" -Headers $headers -Body $upsertBody)
    } else {
        throw "[FAIL] POST /api/v1/owner/stores (status=$($createdStore.StatusCode)) body=$($createdStore.Raw)"
    }
}

if ($storeId -le 0) {
    throw "Failed to resolve storeId."
}

if ($null -ne $sample.store.status -and -not [string]::IsNullOrWhiteSpace([string]$sample.store.status)) {
    $statusResp = Invoke-ApiJson `
        -Method "PATCH" `
        -Path "/api/v1/owner/stores/$storeId/status" `
        -Headers $headers `
        -Body @{ status = [string]$sample.store.status } `
        -AllowNonSuccess

    if ($statusResp.StatusCode -ge 200 -and $statusResp.StatusCode -lt 300) {
        Write-Host "[Store] status set to $($sample.store.status)"
    } elseif (
        $statusResp.StatusCode -eq 409 `
        -and $null -ne $statusResp.Body `
        -and $statusResp.Body.code -eq "INVALID_STORE_STATUS_TRANSITION"
    ) {
        Write-Host "[Store] status already '$($sample.store.status)' (skip)"
    } else {
        throw "[FAIL] PATCH /api/v1/owner/stores/$storeId/status (status=$($statusResp.StatusCode)) body=$($statusResp.Raw)"
    }
}

if (-not $SkipStoreHours) {
    $weeklyHours = @()
    foreach ($hour in @($sample.store.operatingHours)) {
        $weeklyHours += @{
            dayOfWeek = [int]$hour.dayOfWeek
            openTime = $hour.openTime
            closeTime = $hour.closeTime
        }
    }
    [void](Invoke-ApiJson `
        -Method "PUT" `
        -Path "/api/v1/owner/stores/$storeId/hours" `
        -Headers $headers `
        -Body @{ weeklyHours = $weeklyHours })
}

if (-not $SkipStoreHolidays) {
    $holidays = @()
    foreach ($holiday in @($sample.store.holidays)) {
        $holidays += @{
            date = [string]$holiday.date
            reason = $holiday.reason
        }
    }
    [void](Invoke-ApiJson `
        -Method "PUT" `
        -Path "/api/v1/owner/stores/$storeId/holidays" `
        -Headers $headers `
        -Body @{ holidays = $holidays })
}

if (-not $SkipMenuReset) {
    $detailResp = Invoke-ApiJson -Method "GET" -Path "/api/v1/stores/$storeId"
    $existingMenuGroups = @($detailResp.Body.menuGroups)
    foreach ($menuGroup in $existingMenuGroups) {
        $menuGroupId = [long]$menuGroup.id
        [void](Invoke-ApiJson `
            -Method "DELETE" `
            -Path "/api/v1/owner/stores/$storeId/menu-groups/$menuGroupId" `
            -Headers $headers)
    }
    Write-Host "[Menu] existing menu groups deleted: $($existingMenuGroups.Count)"
}

$createdMenuGroupCount = 0
$createdMenuCount = 0

$groupDisplayOrder = 0
foreach ($menuGroup in @($sample.store.menuGroups)) {
    $createGroupResp = Invoke-ApiJson `
        -Method "POST" `
        -Path "/api/v1/owner/stores/$storeId/menu-groups" `
        -Headers $headers `
        -Body @{
            name = [string]$menuGroup.name
            displayOrder = $groupDisplayOrder
        }

    $createdMenuGroupId = [long]$createGroupResp.Body.id
    $createdMenuGroupCount++

    $menuDisplayOrder = 0
    foreach ($menu in @($menuGroup.menus)) {
        $createMenuResp = Invoke-ApiJson `
            -Method "POST" `
            -Path "/api/v1/owner/stores/$storeId/menus" `
            -Headers $headers `
            -Body @{
                menuGroupId = $createdMenuGroupId
                name = [string]$menu.name
                description = $menu.description
                price = [int]$menu.price
                isAvailable = [bool]$menu.isAvailable
                imageUrl = $menu.imageUrl
                displayOrder = $menuDisplayOrder
            }

        $createdMenuId = [long]$createMenuResp.Body.id
        $createdMenuCount++

        $tagNames = @(Normalize-TagNames -Tags @($menu.tags))
        if ($tagNames.Count -gt 0) {
            [void](Invoke-ApiJson `
                -Method "PUT" `
                -Path "/api/v1/owner/stores/$storeId/menus/$createdMenuId/tags" `
                -Headers $headers `
                -Body @{ tagNames = $tagNames })
        }

        $optionGroups = @()
        foreach ($optionGroup in @($menu.optionGroups)) {
            $options = @()
            foreach ($option in @($optionGroup.options)) {
                $options += @{
                    name = [string]$option.name
                    extraPrice = [int]$option.extraPrice
                    isAvailable = [bool]$option.isAvailable
                }
            }

            $optionGroups += @{
                name = [string]$optionGroup.name
                isRequired = [bool]$optionGroup.isRequired
                isMultiple = [bool]$optionGroup.isMultiple
                minSelectCount = [int]$optionGroup.minSelectCount
                maxSelectCount = [int]$optionGroup.maxSelectCount
                options = $options
            }
        }

        if ($optionGroups.Count -gt 0) {
            [void](Invoke-ApiJson `
                -Method "PUT" `
                -Path "/api/v1/owner/stores/$storeId/menus/$createdMenuId/option-groups" `
                -Headers $headers `
                -Body @{ optionGroups = $optionGroups })
        }

        $menuSoldOut = $false
        if (($menu.PSObject.Properties.Name -contains "isSoldOut") -and $null -ne $menu.isSoldOut) {
            $menuSoldOut = [bool]$menu.isSoldOut
        }

        if ($menuSoldOut) {
            [void](Invoke-ApiJson `
                -Method "PUT" `
                -Path "/api/v1/owner/stores/$storeId/menus/$createdMenuId" `
                -Headers $headers `
                -Body @{
                    menuGroupId = $createdMenuGroupId
                    name = [string]$menu.name
                    description = $menu.description
                    price = [int]$menu.price
                    isAvailable = [bool]$menu.isAvailable
                    isSoldOut = $menuSoldOut
                    imageUrl = $menu.imageUrl
                })
        }

        $menuDisplayOrder++
    }

    $groupDisplayOrder++
}

$summary = [PSCustomObject]@{
    storeId = $storeId
    storeName = $storeName
    ownerUserId = $OwnerUserId
    categoryIds = $resolvedCategoryIds
    menuGroupCount = $createdMenuGroupCount
    menuCount = $createdMenuCount
    sourceJson = $JsonPath
}

Write-Host ""
Write-Host "== Import completed =="
$summary | ConvertTo-Json -Depth 10
