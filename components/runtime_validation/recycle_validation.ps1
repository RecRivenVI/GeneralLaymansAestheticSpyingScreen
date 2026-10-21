param([Parameter(Mandatory = $true)][string]$ValidationWorld)
$ErrorActionPreference = 'Stop'
$validationRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../validations/integration-remote_projection/instance'))
$resolvedWorld = (Resolve-Path -LiteralPath $ValidationWorld).Path
$expectedPrefix = $validationRoot.TrimEnd('\') + '\'
if (-not $resolvedWorld.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'World is outside this validation' }
if (-not (Split-Path -Leaf $resolvedWorld).StartsWith('GLASS B ', [StringComparison]::Ordinal)) { throw 'Not a generated GLASS B fixture world' }
if ((Split-Path -Leaf (Split-Path -Parent $resolvedWorld)) -ne 'saves') { throw 'Not a fixture save directory' }
Add-Type -AssemblyName Microsoft.VisualBasic
[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory($resolvedWorld, [Microsoft.VisualBasic.FileIO.UIOption]::OnlyErrorDialogs, [Microsoft.VisualBasic.FileIO.RecycleOption]::SendToRecycleBin)
@{ recycledWorld = $resolvedWorld } | ConvertTo-Json -Compress
