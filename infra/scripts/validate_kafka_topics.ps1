param(
    [string]$ManifestPath = "$PSScriptRoot\..\kafka\topics.yaml",
    [string]$ContainerName = "kafka",
    [string]$BootstrapServer = "kafka:29092"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Normalize-YamlValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $trimmed = $Value.Trim()
    if ($trimmed.Length -ge 2) {
        if (($trimmed.StartsWith('"') -and $trimmed.EndsWith('"')) -or ($trimmed.StartsWith("'") -and $trimmed.EndsWith("'"))) {
            return $trimmed.Substring(1, $trimmed.Length - 2)
        }
    }
    return $trimmed
}

function Parse-TopicManifest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Manifest file was not found. path=$Path"
    }

    $lines = Get-Content -LiteralPath $Path
    $topics = @()
    $allowUnexpectedTopics = @()
    $section = ""
    $current = $null
    $inConfigs = $false

    foreach ($rawLine in $lines) {
        $line = $rawLine -replace "`t", "    "
        $trimmed = $line.Trim()

        if (-not $trimmed) { continue }
        if ($trimmed.StartsWith("#")) { continue }

        if ($trimmed -eq "topics:") {
            if ($current -ne $null) {
                $topics += [PSCustomObject]$current
                $current = $null
            }
            $section = "topics"
            $inConfigs = $false
            continue
        }

        if ($trimmed -eq "allowUnexpectedTopics:") {
            if ($current -ne $null) {
                $topics += [PSCustomObject]$current
                $current = $null
            }
            $section = "allowUnexpectedTopics"
            $inConfigs = $false
            continue
        }

        if ($section -eq "topics") {
            if ($line -match '^\s*-\s*name\s*:\s*(.+)\s*$') {
                if ($current -ne $null) {
                    $topics += [PSCustomObject]$current
                }

                $current = @{
                    Name = Normalize-YamlValue -Value $Matches[1]
                    Partitions = 1
                    ReplicationFactor = 1
                    Configs = @{}
                }
                $inConfigs = $false
                continue
            }

            if ($current -eq $null) {
                continue
            }

            if ($line -match '^\s*partitions\s*:\s*(\d+)\s*$') {
                $current.Partitions = [int]$Matches[1]
                continue
            }

            if ($line -match '^\s*replicationFactor\s*:\s*(\d+)\s*$') {
                $current.ReplicationFactor = [int]$Matches[1]
                continue
            }

            if ($line -match '^\s*configs\s*:\s*$') {
                $inConfigs = $true
                continue
            }

            if ($inConfigs -and $line -match '^\s*([A-Za-z0-9._-]+)\s*:\s*(.+)\s*$') {
                $key = $Matches[1]
                $value = Normalize-YamlValue -Value $Matches[2]
                $current.Configs[$key] = $value
                continue
            }
        }

        if ($section -eq "allowUnexpectedTopics") {
            if ($line -match '^\s*-\s*(.+)\s*$') {
                $allowUnexpectedTopics += Normalize-YamlValue -Value $Matches[1]
            }
        }
    }

    if ($current -ne $null) {
        $topics += [PSCustomObject]$current
    }

    if ($topics.Count -eq 0) {
        throw "No topics were found in manifest. path=$Path"
    }

    return [PSCustomObject]@{
        Topics = $topics
        AllowUnexpectedTopics = $allowUnexpectedTopics
    }
}

function Assert-CommandExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandName
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Command '$CommandName' was not found."
    }
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$DockerArgs,
        [switch]$AllowFailure
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $rawOutput = $null
    $exitCode = 1
    try {
        $ErrorActionPreference = "Continue"
        $rawOutput = & docker @DockerArgs 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $output = @($rawOutput | ForEach-Object { $_.ToString() })

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        $message = ($output | Out-String).Trim()
        throw "docker $($DockerArgs -join ' ') failed (exit=$exitCode)`n$message"
    }

    return [PSCustomObject]@{
        ExitCode = $exitCode
        Output = $output
    }
}

function Parse-TopicConfigs {
    param(
        [AllowNull()]
        [AllowEmptyString()]
        [string]$ConfigsText = ""
    )

    $map = @{}
    if (-not $ConfigsText) {
        return $map
    }

    $segments = $ConfigsText.Split(",")
    foreach ($segment in $segments) {
        $part = $segment.Trim()
        if (-not $part) { continue }

        $eqIndex = $part.IndexOf("=")
        if ($eqIndex -lt 1) { continue }

        $key = $part.Substring(0, $eqIndex).Trim()
        $value = $part.Substring($eqIndex + 1).Trim()

        if ($key) {
            $map[$key] = $value
        }
    }

    return $map
}

function Get-TopicMetadata {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TopicName
    )

    $result = Invoke-Docker -DockerArgs @(
        "exec",
        $ContainerName,
        "kafka-topics",
        "--bootstrap-server", $BootstrapServer,
        "--describe",
        "--topic", $TopicName
    ) -AllowFailure

    if ($result.ExitCode -ne 0) {
        return $null
    }

    $summaryLine = $null
    foreach ($line in $result.Output) {
        if ($line -match "Topic:\s*$([Regex]::Escape($TopicName))\b") {
            $summaryLine = $line
            break
        }
    }

    if ($summaryLine -eq $null) {
        $summaryLine = ($result.Output | Select-Object -First 1)
    }

    if (-not $summaryLine) {
        return $null
    }

    $partitions = $null
    $replicationFactor = $null
    $configsText = ""

    if ($summaryLine -match 'PartitionCount:\s*(\d+)') {
        $partitions = [int]$Matches[1]
    }

    if ($summaryLine -match 'ReplicationFactor:\s*(\d+)') {
        $replicationFactor = [int]$Matches[1]
    }

    if ($summaryLine -match 'Configs:\s*([^\r\n]*)') {
        $configsText = $Matches[1].Trim()
    }

    return [PSCustomObject]@{
        Partitions = $partitions
        ReplicationFactor = $replicationFactor
        Configs = Parse-TopicConfigs -ConfigsText $configsText
    }
}

try {
    Assert-CommandExists -CommandName "docker"

    $manifest = Parse-TopicManifest -Path $ManifestPath
    $desiredTopicNames = @($manifest.Topics | ForEach-Object { $_.Name })
    $allowedUnexpected = @($manifest.AllowUnexpectedTopics)

    $dockerPs = Invoke-Docker -DockerArgs @("ps", "--format", "{{.Names}}")
    $containerNames = @($dockerPs.Output | ForEach-Object { $_.Trim() })
    if (-not ($containerNames | Where-Object { $_ -eq $ContainerName })) {
        throw "Container '$ContainerName' is not running."
    }

    $topicListResult = Invoke-Docker -DockerArgs @(
        "exec",
        $ContainerName,
        "kafka-topics",
        "--bootstrap-server", $BootstrapServer,
        "--list"
    )
    $actualTopicNames = @(
        $topicListResult.Output |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith("WARN") } |
        Sort-Object -Unique
    )

    Write-Host "== Kafka Topic Validate =="
    Write-Host "Manifest : $ManifestPath"
    Write-Host "Container: $ContainerName"
    Write-Host "Bootstrap: $BootstrapServer"
    Write-Host ""

    $missingTopics = @($desiredTopicNames | Where-Object { $_ -notin $actualTopicNames })
    $unexpectedTopics = @(
        $actualTopicNames |
        Where-Object {
            $_ -notin $desiredTopicNames -and
            $_ -notin $allowedUnexpected -and
            -not $_.StartsWith("__")
        }
    )

    $specErrors = 0
    foreach ($topic in $manifest.Topics) {
        $metadata = Get-TopicMetadata -TopicName $topic.Name
        if ($metadata -eq $null) {
            Write-Host "[FAIL] $($topic.Name) - metadata not found"
            $specErrors++
            continue
        }

        $topicFailed = $false
        if ($metadata.Partitions -ne $topic.Partitions) {
            Write-Host "[FAIL] $($topic.Name) - partitions expected=$($topic.Partitions) actual=$($metadata.Partitions)"
            $topicFailed = $true
        }

        if ($metadata.ReplicationFactor -ne $topic.ReplicationFactor) {
            Write-Host "[FAIL] $($topic.Name) - replicationFactor expected=$($topic.ReplicationFactor) actual=$($metadata.ReplicationFactor)"
            $topicFailed = $true
        }

        foreach ($expectedKey in ($topic.Configs.Keys | Sort-Object)) {
            $expectedValue = $topic.Configs[$expectedKey]
            $actualValue = $null
            if ($metadata.Configs.ContainsKey($expectedKey)) {
                $actualValue = $metadata.Configs[$expectedKey]
            }

            if ($actualValue -ne $expectedValue) {
                Write-Host "[FAIL] $($topic.Name) - config '$expectedKey' expected='$expectedValue' actual='$actualValue'"
                $topicFailed = $true
            }
        }

        if (-not $topicFailed) {
            Write-Host "[OK] $($topic.Name)"
            continue
        }

        $specErrors++
    }

    Write-Host ""

    if ($missingTopics.Count -gt 0) {
        Write-Host "[FAIL] Missing topics:"
        foreach ($topicName in $missingTopics) {
            Write-Host "  - $topicName"
        }
    } else {
        Write-Host "[OK] Missing topics: none"
    }

    if ($unexpectedTopics.Count -gt 0) {
        Write-Host "[WARN] Unexpected topics (not in manifest):"
        foreach ($topicName in $unexpectedTopics) {
            Write-Host "  - $topicName"
        }
    } else {
        Write-Host "[OK] Unexpected topics: none"
    }

    if ($missingTopics.Count -gt 0 -or $specErrors -gt 0) {
        Write-Host ""
        Write-Host "[FAIL] topic validation failed."
        exit 1
    }

    Write-Host ""
    Write-Host "[OK] topic validation passed."
} catch {
    Write-Host "[FAIL] $($_.Exception.Message)"
    exit 1
}
