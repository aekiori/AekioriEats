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

try {
    Assert-CommandExists -CommandName "docker"

    $manifest = Parse-TopicManifest -Path $ManifestPath

    $dockerPs = Invoke-Docker -DockerArgs @("ps", "--format", "{{.Names}}")
    $containerNames = @($dockerPs.Output | ForEach-Object { $_.Trim() })
    if (-not ($containerNames | Where-Object { $_ -eq $ContainerName })) {
        throw "Container '$ContainerName' is not running."
    }

    Write-Host "== Kafka Topic Apply =="
    Write-Host "Manifest : $ManifestPath"
    Write-Host "Container: $ContainerName"
    Write-Host "Bootstrap: $BootstrapServer"
    Write-Host ""

    $failed = 0

    foreach ($topic in $manifest.Topics) {
        $dockerArgs = @(
            "exec",
            $ContainerName,
            "kafka-topics",
            "--bootstrap-server", $BootstrapServer,
            "--create",
            "--if-not-exists",
            "--topic", $topic.Name,
            "--partitions", $topic.Partitions.ToString(),
            "--replication-factor", $topic.ReplicationFactor.ToString()
        )

        foreach ($configKey in ($topic.Configs.Keys | Sort-Object)) {
            $dockerArgs += @("--config", "$configKey=$($topic.Configs[$configKey])")
        }

        $result = Invoke-Docker -DockerArgs $dockerArgs -AllowFailure
        if ($result.ExitCode -ne 0) {
            $failed++
            Write-Host "[FAIL] $($topic.Name)"
            foreach ($line in ($result.Output | Select-Object -First 2)) {
                if ($line.Trim()) {
                    Write-Host "       $line"
                }
            }
            continue
        }

        if ($topic.Configs.Count -gt 0) {
            $configAssignments = @(
                $topic.Configs.Keys |
                Sort-Object |
                ForEach-Object { "$_=$($topic.Configs[$_])" }
            )
            $joinedConfigs = $configAssignments -join ","

            $configResult = Invoke-Docker -DockerArgs @(
                "exec",
                $ContainerName,
                "kafka-configs",
                "--bootstrap-server", $BootstrapServer,
                "--alter",
                "--entity-type", "topics",
                "--entity-name", $topic.Name,
                "--add-config", $joinedConfigs
            ) -AllowFailure

            if ($configResult.ExitCode -ne 0) {
                $failed++
                Write-Host "[FAIL] $($topic.Name) config apply"
                foreach ($line in ($configResult.Output | Select-Object -First 2)) {
                    if ($line.Trim()) {
                        Write-Host "       $line"
                    }
                }
                continue
            }
        }

        Write-Host "[OK] $($topic.Name)"
    }

    Write-Host ""
    if ($failed -gt 0) {
        Write-Host "[FAIL] apply finished with $failed error(s)."
        exit 1
    }

    Write-Host "[OK] all topics are ensured."
} catch {
    Write-Host "[FAIL] $($_.Exception.Message)"
    exit 1
}
