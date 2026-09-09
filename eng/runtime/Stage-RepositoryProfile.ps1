param(
    [string]$Node = '26.2-fabric',
    [Parameter(Mandatory)][ValidateSet('PRIMARY', 'SECONDARY', 'SERVER')][string]$Role,
    [Parameter(Mandatory)][ValidateSet('S1', 'S2', 'M1', 'M2')][string]$Profile
)

. (Join-Path $PSScriptRoot 'RuntimeProfile.Common.ps1')

$resolved = Read-RepositoryProfile -Node $Node -Role $Role -Profile $Profile
New-Item -ItemType Directory -Path $resolved.ModsDirectory -Force | Out-Null
$requiredIds = @($resolved.RequiredMods | ForEach-Object { [string]$_.id })

foreach ($entry in $script:RequiredTestModRegistry.GetEnumerator()) {
    $destination = Join-Path $resolved.ModsDirectory $entry.Value.File
    if ($requiredIds -notcontains $entry.Key) {
        if (Test-Path -LiteralPath $destination -PathType Leaf) {
            Remove-Item -LiteralPath $destination -Force
        }
        continue
    }

    $cacheDirectory = Join-Path $script:RepositoryRoot 'run\cache\required-test-mods'
    New-Item -ItemType Directory -Path $cacheDirectory -Force | Out-Null
    $cacheFile = Join-Path $cacheDirectory $entry.Value.File
    if (!(Test-Path -LiteralPath $cacheFile -PathType Leaf)) {
        $temporaryFile = "$cacheFile.download"
        try {
            Invoke-WebRequest -Uri $entry.Value.Uri -OutFile $temporaryFile
            if ((Get-Sha512 $temporaryFile) -cne $entry.Value.Sha512) {
                throw "Downloaded required test mod hash mismatch: $($entry.Value.File)"
            }
            Move-Item -LiteralPath $temporaryFile -Destination $cacheFile
        } finally {
            if (Test-Path -LiteralPath $temporaryFile -PathType Leaf) {
                Remove-Item -LiteralPath $temporaryFile -Force
            }
        }
    }
    if ((Get-Sha512 $cacheFile) -cne $entry.Value.Sha512) {
        throw "Cached required test mod hash mismatch: $cacheFile"
    }
    if (Test-Path -LiteralPath $destination -PathType Leaf) {
        if ((Get-Sha512 $destination) -cne $entry.Value.Sha512) {
            throw "Refusing to overwrite a different existing mod: $destination"
        }
    } else {
        Copy-Item -LiteralPath $cacheFile -Destination $destination
    }
}

foreach ($fileName in $script:KnownForbiddenTestModFiles) {
    $forbidden = Join-Path $resolved.ModsDirectory $fileName
    if (Test-Path -LiteralPath $forbidden -PathType Leaf) {
        Remove-Item -LiteralPath $forbidden -Force
    }
}

[pscustomobject]@{
    Node = $Node
    Role = $Role
    Profile = $Profile
    GameDirectory = $resolved.GameDirectory
    ModsDirectory = $resolved.ModsDirectory
    RequiredTestMods = @($requiredIds)
}
