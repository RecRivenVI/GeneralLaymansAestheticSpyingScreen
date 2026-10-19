param([string]$RepositoryRoot = (Join-Path $PSScriptRoot '../..'))

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$reviews = [Collections.Generic.List[string]]::new()

function Get-LayoutChildren {
    param([string]$Path)
    if (!(Test-Path -LiteralPath $Path -PathType Container)) { return }
    $item = Get-Item -LiteralPath $Path -Force
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw "Cannot verify a governed directory through a reparse point: $Path"
    }
    Get-ChildItem -LiteralPath $Path -Force
}

function Test-DocumentName {
    param([string]$Category, [string]$Name)
    if ($Name -cmatch '^adr-') {
        return $Category -ceq 'design' -and $Name -cmatch '^adr-\d+(?:\.\d+)*-[a-z0-9]+(?:-[a-z0-9]+)*\.md$'
    }
    if ($Name -cmatch '^phase-') {
        return $Category -ceq 'project' -and $Name -cmatch '^phase-\d+(?:\.\d+)*-[a-z0-9]+(?:-[a-z0-9]+)*\.md$'
    }
    return $Name -cmatch '^[a-z]+-[a-z0-9]+(?:-[a-z0-9]+)*\.md$'
}

$targets = @('1.20.1-forge', '1.21.1-neoforge', '26.1.2-neoforge', '26.2-fabric', '26.2-neoforge')
$nativeBuildDirectories = @('build', '.gradle', '.kotlin')
$actual = @(Get-LayoutChildren (Join-Path $repositoryRoot 'versions') | Where-Object { $_.PSIsContainer -and $_.Name -cnotin $nativeBuildDirectories } | Select-Object -ExpandProperty Name)
if (@(Compare-Object $targets $actual -CaseSensitive).Count) { throw 'Unexpected target directories, including hidden directories' }
foreach ($target in $targets) {
    $directory = Join-Path $repositoryRoot "versions/$target"
    $null = @(Get-LayoutChildren $directory)
    foreach ($file in @('target.properties', 'build.gradle.kts')) {
        if (!(Test-Path -LiteralPath (Join-Path $directory $file) -PathType Leaf)) { throw "Missing $target/$file" }
    }
    $facts = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $directory 'target.properties') -Raw)
    $expectedStatus = if ($target -eq '26.2-fabric') { 'implemented' } else { 'planned' }
    if ($facts['target.status'] -cne $expectedStatus) { throw "Incorrect target status: $target" }
    if ($expectedStatus -eq 'planned' -and (Test-Path -LiteralPath (Join-Path $directory 'src'))) { throw "Planned target has authored source: $target" }
}
if ((Get-Content -LiteralPath (Join-Path $repositoryRoot 'versions/current') -Raw).Trim() -cne '26.2-fabric') { throw 'Wrong active target' }
foreach ($old in @('src', 'stonecutter.properties.toml', 'build.fabric.gradle.kts', 'build.forge.gradle.kts', 'build.neoforge.gradle.kts', '.github/workflows')) {
    if (Test-Path -LiteralPath (Join-Path $repositoryRoot $old)) { throw "Obsolete authored layout: $old" }
}
foreach ($required in @('versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass', 'versions/26.2-fabric/src/main/resources/assets/glass', 'versions/26.2-fabric/src/main/resources/data/glass', 'AGENTS.md')) {
    if (!(Test-Path -LiteralPath (Join-Path $repositoryRoot $required))) { throw "Missing $required" }
}
$documents = Join-Path $repositoryRoot 'documents'
$categories = @('usage', 'development', 'design', 'project', 'reference', 'release')
if (Test-Path -LiteralPath $documents) {
    foreach ($category in Get-LayoutChildren $documents) {
        if (!$category.PSIsContainer -and $category.Name -cin @('.gitignore', '.gitattributes')) { continue }
        if (!$category.PSIsContainer -or $category.Name -cnotin $categories) { throw "Invalid documents category: $($category.Name)" }
        foreach ($file in Get-LayoutChildren $category.FullName) {
            if (!$file.PSIsContainer -and $file.Name -cin @('.gitignore', '.gitattributes')) { continue }
            if ($file.PSIsContainer -or !(Test-DocumentName $category.Name $file.Name)) {
                throw "Invalid document path (nested and hidden entries are checked): $($category.Name)/$($file.Name)"
            }
        }
    }
}

# Only the explicitly governed validation boundary is checked here. The contents
# of task/result/instance keep their native tool, evidence and Minecraft layouts.
foreach ($validation in Get-LayoutChildren (Join-Path $repositoryRoot 'validations')) {
    if (!$validation.PSIsContainer -and $validation.Name -cin @('.gitignore', '.gitattributes')) { continue }
    if (!$validation.PSIsContainer -or $validation.Name -cnotmatch '^[a-z]+-[a-z0-9]+(?:-[a-z0-9]+)*$') {
        throw "Invalid validation project: $($validation.Name)"
    }
    foreach ($entry in Get-LayoutChildren $validation.FullName) {
        if ($entry.PSIsContainer) {
            if ($entry.Name -cnotin @('task', 'instance', 'result')) { throw "Invalid validation directory: $($validation.Name)/$($entry.Name)" }
        } elseif ($entry.Name -cnotin @('validation.md', 'validation-guide.md', '.gitignore', '.gitattributes')) {
            throw "Invalid validation root file: $($validation.Name)/$($entry.Name)"
        }
    }
    if (!(Test-Path -LiteralPath (Join-Path $validation.FullName 'task') -PathType Container) -or
        !(Test-Path -LiteralPath (Join-Path $validation.FullName 'validation.md') -PathType Leaf)) {
        throw "Validation needs its actual task inputs and validation.md: $($validation.Name)"
    }
}

foreach ($target in Get-LayoutChildren (Join-Path $repositoryRoot 'instances')) {
    if (!$target.PSIsContainer -and $target.Name -cin @('.gitignore', '.gitattributes')) { continue }
    if (!$target.PSIsContainer -or $target.Name -cnotin $targets) { throw "Unknown daily instance target: $($target.Name)" }
    foreach ($variant in Get-LayoutChildren $target.FullName) {
        if (!$variant.PSIsContainer -or $variant.Name -cnotin @('client', 'client-multiplayer', 'server')) {
            throw "Invalid daily instance variant: $($target.Name)/$($variant.Name)"
        }
    }
}

# Known standard Markdown files remain at the root. Unrecognized uppercase names
# are reviewed instead of pretending the examples form a complete whitelist.
foreach ($file in Get-LayoutChildren $repositoryRoot | Where-Object { !$_.PSIsContainer -and $_.Extension -ieq '.md' }) {
    if ($file.Name -cmatch '^(AGENTS|CLAUDE|README|LICENSE|COPYING|CONTRIBUTING|SECURITY|CODE_OF_CONDUCT|SUPPORT|CHANGELOG|AUTHORS|NOTICE)(\.[A-Za-z0-9-]+)*\.md$') { continue }
    if ($file.Name -cmatch '^\.?[a-z]') { throw "Ordinary project documentation belongs in documents/<category>: $($file.Name)" }
    $reviews.Add("Confirm whether root file '$($file.Name)' is an ecosystem standard document.")
}

# These are unresolved retention decisions, not permanent compliance exemptions.
# Do not recurse into, modify or silently accept historical instances/evidence.
foreach ($legacyRoot in @('run', 'runs', 'tests', 'results', '.local', 'docs')) {
    if (Test-Path -LiteralPath (Join-Path $repositoryRoot $legacyRoot)) {
        $reviews.Add("Existing '$legacyRoot' requires ownership/retention review; it is not certified as a conforming active layout.")
    }
}
if (Test-Path -LiteralPath (Join-Path $repositoryRoot 'build/migration-20260909')) {
    $reviews.Add('Migration snapshots under build/migration-20260909 need a retention decision before clean; they are not all reproducible cache.')
}

foreach ($review in $reviews) { Write-Warning "REVIEW_REQUIRED: $review" }
if ($reviews.Count) {
    "CHECKED: enforced layout rules; ERRORS=0; REVIEW_REQUIRED=$($reviews.Count). This is not a full-layout compliance claim."
} else {
    'PASS: enforced target, documentation, validation and daily-instance boundary rules; runtime behavior is not assessed.'
}
