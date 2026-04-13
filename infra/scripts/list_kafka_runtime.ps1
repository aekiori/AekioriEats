param(
    [string]$ContainerName = "kafka",
    [string]$BootstrapServer = "kafka:29092",
    [switch]$SkipGroupDescribe
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
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

function Invoke-DockerKafka {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$KafkaArgs,
        [switch]$AllowFailure
    )

    $allArgs = @("exec", $ContainerName) + $KafkaArgs
    return Invoke-Docker -DockerArgs $allArgs -AllowFailure:$AllowFailure
}

function Get-CleanLines {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Lines
    )

    return @(
        $Lines |
        ForEach-Object { $_.Trim() } |
        Where-Object {
            $_ -and
            -not $_.StartsWith("WARN") -and
            -not $_.StartsWith("[")
        }
    )
}

function Parse-ConsumerMembers {
    param(
        [string[]]$DescribeLines = @()
    )

    if ($null -eq $DescribeLines) {
        return @()
    }

    $members = @()
    foreach ($line in $DescribeLines) {
        $trimmed = $line.Trim()
        if (-not $trimmed) { continue }
        if ($trimmed.StartsWith("GROUP")) { continue }
        if ($trimmed.StartsWith("Consumer group")) { continue }
        if ($trimmed.StartsWith("Error:")) { continue }

        $parts = $trimmed -split "\s+"
        if ($parts.Count -lt 9) { continue }

        $consumerId = $parts[$parts.Count - 3]
        $memberHost = $parts[$parts.Count - 2]
        $clientId = $parts[$parts.Count - 1]

        if ($consumerId -eq "-") { continue }

        $members += [PSCustomObject]@{
            ConsumerId = $consumerId
            Host = $memberHost
            ClientId = $clientId
        }
    }

    return @($members | Sort-Object ConsumerId -Unique)
}

try {
    Assert-CommandExists -CommandName "docker"

    $dockerPs = Invoke-Docker -DockerArgs @("ps", "--format", "{{.Names}}")
    $containerNames = @($dockerPs.Output | ForEach-Object { $_.Trim() })
    if ($dockerPs.ExitCode -ne 0) {
        throw "Failed to run 'docker ps'. Is Docker running?"
    }

    if (-not ($containerNames | Where-Object { $_ -eq $ContainerName })) {
        throw "Container '$ContainerName' is not running. Start infra containers first."
    }

    Write-Host "== Kafka Runtime Snapshot =="
    Write-Host "Container: $ContainerName"
    Write-Host "Bootstrap: $BootstrapServer"
    Write-Host ""

    $topicResult = Invoke-DockerKafka -KafkaArgs @("kafka-topics", "--bootstrap-server", $BootstrapServer, "--list")
    $topics = @(Get-CleanLines -Lines $topicResult.Output | Sort-Object -Unique)

    Write-Host "[Topics]"
    if ($topics.Count -eq 0) {
        Write-Host "(none)"
    } else {
        foreach ($topic in $topics) {
            Write-Host "- $topic"
        }
    }
    Write-Host ""

    $groupResult = Invoke-DockerKafka -KafkaArgs @("kafka-consumer-groups", "--bootstrap-server", $BootstrapServer, "--list")
    $groups = @(Get-CleanLines -Lines $groupResult.Output | Sort-Object -Unique)

    Write-Host "[Consumer Groups]"
    if ($groups.Count -eq 0) {
        Write-Host "(none)"
    } else {
        foreach ($group in $groups) {
            Write-Host "- $group"
        }
    }
    Write-Host ""

    if ($SkipGroupDescribe) {
        return
    }

    Write-Host "[Consumers by Group]"
    if ($groups.Count -eq 0) {
        Write-Host "(none)"
        return
    }

    foreach ($group in $groups) {
        Write-Host ""
        Write-Host "[$group]"

        $describeResult = Invoke-DockerKafka -KafkaArgs @("kafka-consumer-groups", "--bootstrap-server", $BootstrapServer, "--describe", "--group", $group) -AllowFailure
        $describeLines = @($describeResult.Output)

        if ($describeResult.ExitCode -ne 0) {
            Write-Host "  ! describe failed (exit=$($describeResult.ExitCode))"
            $firstLines = @($describeLines | Select-Object -First 2)
            foreach ($entry in $firstLines) {
                if ($entry.Trim()) {
                    Write-Host "    $entry"
                }
            }
            continue
        }

        $members = @(Parse-ConsumerMembers -DescribeLines $describeLines)
        if ($members.Count -eq 0) {
            Write-Host "  - active consumers: none"
            continue
        }

        foreach ($member in $members) {
            Write-Host ("  - {0} ({1}, {2})" -f $member.ConsumerId, $member.ClientId, $member.Host)
        }
    }
} catch {
    Write-Host "[FAIL] $($_.Exception.Message)"
    exit 1
}
