Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:RepositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
$script:RequiredTestModRegistry = [ordered]@{
    customskinloader = [ordered]@{
        Version = '15.0.1-Universal'
        File = 'CustomSkinLoader_Universal-15.0.1.jar'
        Sha512 = '8c65193c46c1435ddea571901f1dd04cc179179f59f1c1a0449349de04722b9c1b6e5efaec166975e05d2fa0b05f6c07c9d5d93a05f1c9a57fece2a0d0fc1ce7'
        Uri = 'https://github.com/xfl03/MCCustomSkinLoader/releases/download/v15.0.1/CustomSkinLoader_Universal-15.0.1.jar'
    }
    modmenu = [ordered]@{
        Version = '20.0.1'
        File = 'modmenu-20.0.1.jar'
        Sha512 = '1aa297ab5e6fac71ad6af750fe6f8cd25281bd00c0340e5fe1c1e9d153d1198df03be1df55727a4a2116db97dcaad541d41754f3fc0063abfe4345b60f193fb4'
        Uri = 'https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/20.0.1/modmenu-20.0.1.jar'
    }
}
$script:KnownForbiddenTestModFiles = @('zume-1.2.2.jar')

function Resolve-RepositoryRelativePath {
    param([Parameter(Mandatory)][string]$RelativePath)

    if ([string]::IsNullOrWhiteSpace($RelativePath) -or [IO.Path]::IsPathRooted($RelativePath)) {
        throw "Runtime path must be non-empty and repository-relative: '$RelativePath'"
    }
    $resolved = [IO.Path]::GetFullPath((Join-Path $script:RepositoryRoot $RelativePath))
    $prefix = $script:RepositoryRoot.TrimEnd('\') + '\'
    if (!$resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Runtime path escapes the repository: '$RelativePath'"
    }
    return $resolved
}

function Get-Sha512 {
    param([Parameter(Mandatory)][string]$Path)

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA512).Hash.ToLowerInvariant()
}

function Read-RepositoryProfile {
    param(
        [Parameter(Mandatory)][string]$Node,
        [Parameter(Mandatory)][ValidateSet('PRIMARY', 'SECONDARY', 'SERVER')][string]$Role,
        [Parameter(Mandatory)][ValidateSet('S1', 'S2', 'M1', 'M2')][string]$Profile
    )

    $scenario = switch ($Profile) {
        'S1' { 'regression-clean-session' }
        'S2' { 'compatibility-client-mods' }
        'M1' { 'integration-multiplayer-session' }
        'M2' { 'integration-dedicated-session' }
    }
    $variant = switch ($Role) {
        'PRIMARY' { 'client' }
        'SECONDARY' { 'client-multiplayer' }
        'SERVER' { 'server' }
    }
    $recordPath = Join-Path $script:RepositoryRoot "validations/$scenario/task/profiles/$Node/$variant.json"
    if (!(Test-Path -LiteralPath $recordPath -PathType Leaf)) {
        throw "Missing repository-local profile: $recordPath"
    }
    $record = Get-Content -LiteralPath $recordPath -Raw | ConvertFrom-Json
    if ($record.schemaVersion -ne 1 -or $record.node -ne $Node -or $record.role -ne $Role -or $record.profile -ne $Profile) {
        throw "Profile identity mismatch: $recordPath"
    }
    $gameDirectory = Resolve-RepositoryRelativePath ([string]$record.gameDirectory)
    $modsDirectory = Resolve-RepositoryRelativePath ([string]$record.modsDirectory)
    if ($modsDirectory -cne (Join-Path $gameDirectory 'mods')) {
        throw "modsDirectory must be gameDirectory/mods: $recordPath"
    }

    $requiredMods = @($record.requiredTestMods)
    $requiredIds = @($requiredMods | ForEach-Object { [string]$_.id })
    if ($requiredIds -contains 'zume') {
        throw "Zume is not permitted in a GLASS profile: $recordPath"
    }
    if ($Role -eq 'SERVER' -or $Profile -eq 'S1') {
        if ($requiredIds.Count -ne 0) {
            throw "S1 and dedicated server profiles must be clean: $recordPath"
        }
    } elseif (@($requiredIds | Sort-Object) -join ',' -cne 'customskinloader,modmenu') {
        throw "Non-clean Fabric clients require exactly CustomSkinLoader and Mod Menu: $recordPath"
    }

    $expectedPlayer = switch ($Role) {
        'PRIMARY' { 'RecRivenVI' }
        'SECONDARY' { 'CNN_MOYAN' }
        'SERVER' { $null }
    }
    if ($record.playerName -cne $expectedPlayer) {
        throw "Unexpected playerName in $recordPath"
    }
    $expectedResolution = if ($Role -eq 'SERVER') { $null } else { '2560x1440' }
    if ($record.resolution -cne $expectedResolution) {
        throw "Unexpected resolution in $recordPath"
    }
    if (@($record.compatibilityFixture).Count -ne 0) {
        throw "GLASS currently has no repository runtime compatibility fixture: $recordPath"
    }

    foreach ($required in $requiredMods) {
        $id = [string]$required.id
        if (!$script:RequiredTestModRegistry.Contains($id)) {
            throw "Unknown required test mod '$id' in $recordPath"
        }
        $pin = $script:RequiredTestModRegistry[$id]
        if ($required.version -cne $pin.Version -or $required.file -cne $pin.File -or $required.sha512 -cne $pin.Sha512) {
            throw "Required test mod pin mismatch for '$id' in $recordPath"
        }
    }

    return [pscustomobject]@{
        RecordPath = $recordPath
        Record = $record
        GameDirectory = $gameDirectory
        ModsDirectory = $modsDirectory
        RequiredMods = $requiredMods
    }
}
