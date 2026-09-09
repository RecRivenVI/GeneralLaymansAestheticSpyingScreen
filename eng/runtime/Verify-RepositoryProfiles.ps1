param([string]$Node = '26.2-fabric')

. (Join-Path $PSScriptRoot 'RuntimeProfile.Common.ps1')

$expectedProfiles = @(
    @{ Role = 'PRIMARY'; Profile = 'S1' },
    @{ Role = 'PRIMARY'; Profile = 'S2' },
    @{ Role = 'PRIMARY'; Profile = 'M1' },
    @{ Role = 'PRIMARY'; Profile = 'M2' },
    @{ Role = 'SECONDARY'; Profile = 'M1' },
    @{ Role = 'SECONDARY'; Profile = 'M2' },
    @{ Role = 'SERVER'; Profile = 'M2' }
)

$results = foreach ($expected in $expectedProfiles) {
    $resolved = Read-RepositoryProfile -Node $Node -Role $expected.Role -Profile $expected.Profile
    if (!(Test-Path -LiteralPath $resolved.GameDirectory -PathType Container)) {
        throw "Missing game directory: $($resolved.GameDirectory)"
    }
    if (!(Test-Path -LiteralPath $resolved.ModsDirectory -PathType Container)) {
        throw "Missing mods directory: $($resolved.ModsDirectory)"
    }

    $requiredIds = @($resolved.RequiredMods | ForEach-Object { [string]$_.id })
    foreach ($entry in $script:RequiredTestModRegistry.GetEnumerator()) {
        $path = Join-Path $resolved.ModsDirectory $entry.Value.File
        if ($requiredIds -contains $entry.Key) {
            if (!(Test-Path -LiteralPath $path -PathType Leaf)) {
                throw "Missing required test mod: $path"
            }
            if ((Get-Sha512 $path) -cne $entry.Value.Sha512) {
                throw "Required test mod hash mismatch: $path"
            }
        } elseif (Test-Path -LiteralPath $path -PathType Leaf) {
            throw "Clean profile contains forbidden test mod: $path"
        }
    }
    foreach ($fileName in $script:KnownForbiddenTestModFiles) {
        $path = Join-Path $resolved.ModsDirectory $fileName
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            throw "GLASS profile contains forbidden Zume fixture: $path"
        }
    }

    [pscustomobject]@{
        Node = $Node
        Role = $expected.Role
        Profile = $expected.Profile
        GameDirectory = $resolved.Record.gameDirectory
        ModsDirectory = $resolved.Record.modsDirectory
        RequiredTestMods = @($requiredIds)
    }
}

$results
