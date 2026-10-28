# Read-only inspection. Writes only the report; does not load a plugin or modify Creo.
[CmdletBinding()]
param(
    [string]$PluginDir = 'D:\creoson\jlink',
    [string]$RegistrationFile = '',
    [string]$LogPath = '',
    [string]$OutFile = ''
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
function U32BE([byte[]]$Bytes, [int]$Offset) {
    return ([uint64]$Bytes[$Offset] * 16777216 + [uint64]$Bytes[$Offset+1] * 65536 + [uint64]$Bytes[$Offset+2] * 256 + [uint64]$Bytes[$Offset+3])
}
function Inspect-File([string]$Path) {
    $record = [ordered]@{path=$Path;exists=(Test-Path -LiteralPath $Path -PathType Leaf)}
    if (-not $record.exists) { return $record }
    $bytes = [IO.File]::ReadAllBytes($Path)
    $record['bytes'] = $bytes.Length
    $record['sha256'] = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    $record['modified_utc'] = (Get-Item -LiteralPath $Path).LastWriteTimeUtc.ToString('o')
    if ($bytes.Length -ge 33 -and [BitConverter]::ToString($bytes,0,8) -eq '89-50-4E-47-0D-0A-1A-0A') {
        $record['format']='PNG'; $record['width']=(U32BE $bytes 16); $record['height']=(U32BE $bytes 20)
        $record['bit_depth']=$bytes[24]; $record['colour_type']=$bytes[25]
    } elseif ($bytes.Length -ge 54 -and $bytes[0] -eq 66 -and $bytes[1] -eq 77) {
        $record['format']='BMP'; $record['width']=[BitConverter]::ToInt32($bytes,18); $record['height']=[BitConverter]::ToInt32($bytes,22)
        $record['bits_per_pixel']=[BitConverter]::ToUInt16($bytes,28)
        $record['compression']=[BitConverter]::ToUInt32($bytes,30)
        $record['palette_entries']=[BitConverter]::ToUInt32($bytes,46)
    } else { $record['format']='not_identified_as_png_or_bmp' }
    return $record
}
try {
    if (-not $RegistrationFile) { $RegistrationFile=Join-Path $PluginDir 'batchrename.dat' }
    if (-not $LogPath) {
        if ($env:CREO_BR_LOG_DIR) { $LogPath=Join-Path $env:CREO_BR_LOG_DIR 'jlink.log' }
        else { $LogPath=Join-Path ([Environment]::GetFolderPath('UserProfile')) 'CreoBatchRename\jlink.log' }
    }
    if (-not $OutFile) { $OutFile=Join-Path ([IO.Path]::GetTempPath()) ('CreoIconDiagnostic_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.json') }
    $notes=New-Object 'Collections.Generic.List[string]'
    $textDir=Join-Path $PluginDir 'text'
    $classesDir=Join-Path $PluginDir 'classes'
    if (Test-Path -LiteralPath $RegistrationFile -PathType Leaf) {
        foreach ($line in Get-Content -LiteralPath $RegistrationFile -Encoding UTF8) {
            if ($line.Trim() -match '^(?i:text_dir)\s+(.+?)\s*$') { $textDir=$Matches[1].Trim().Trim('"') }
            if ($line.Trim() -match '^(?i:java_app_classpath)\s+(.+?)\s*$') { $classesDir=$Matches[1].Trim().Trim('"') }
        }
    } else { $notes.Add('Candidate registration file does not exist; supply the actual file with -RegistrationFile.') }
    $classFile=Join-Path $classesDir 'BatchRename.class'
    $classReport=[ordered]@{path=$classFile;exists=(Test-Path -LiteralPath $classFile -PathType Leaf)}
    if ($classReport.exists) {
        $classBytes=[IO.File]::ReadAllBytes($classFile)
        $classReport['sha256']=(Get-FileHash -LiteralPath $classFile -Algorithm SHA256).Hash.ToLowerInvariant()
        $ascii=[Text.Encoding]::ASCII.GetString($classBytes)
        $classReport['has_r5_marker']=$ascii.Contains('2026-10-28-icon-r5-png-diagnostic')
        $classReport['icon_filename_constants']=@([regex]::Matches($ascii, 'batchrename(?:_r5)?(?:_large|_16)?\.(?:bmp|png)') | ForEach-Object { $_.Value } | Sort-Object -Unique)
        if (-not $classReport.has_r5_marker) { $notes.Add('Installed class lacks r5 marker. Copying PNG alone cannot change a class that still passes a BMP filename to SetIcon.') }
    } else { $notes.Add('No BatchRename.class at the candidate classpath. Multi-entry classpaths require manual inspection.') }
    $assets=@()
    foreach ($name in @('batchrename.bmp','batchrename_16.bmp','batchrename_r5.png','batchrename_r5_large.png')) {
        $assets += Inspect-File (Join-Path (Join-Path $textDir 'resource') $name)
    }
    $tail=@()
    if (Test-Path -LiteralPath $LogPath -PathType Leaf) {
        $tail=@(Get-Content -LiteralPath $LogPath -Encoding UTF8 -Tail 150 | Where-Object { $_ -match '(?i)icon|SetIcon|XToolkit|build=|start entered|start returned|BatchRename\.java|Caused by' })
    } else { $notes.Add('No log at candidate path. Supply -LogPath if a JVM property redirects it.') }
    $notes.Add('Paths from this registration file are candidates, not proof this is the registration Creo actually loaded.')
    $notes.Add('Disk class marker is not proof of loaded class. A fresh start log with build=2026-10-28-icon-r5-png-diagnostic is needed after a complete Creo restart.')
    $report=[ordered]@{
        report_type='read_only_icon_diagnostic';generated_at=(Get-Date).ToString('o')
        registration_candidate=$RegistrationFile;text_dir_candidate=$textDir;classpath_candidate=$classesDir
        installed_class_on_disk=$classReport;assets=$assets;log_path_candidate=$LogPath;relevant_log_tail=$tail
        limitations=$notes.ToArray();creo_runtime='not_inspected';configuration_modified=$false
    }
    [IO.File]::WriteAllText($OutFile,($report | ConvertTo-Json -Depth 10),(New-Object Text.UTF8Encoding($false)))
    Write-Host ('Report: '+$OutFile)
    Write-Host 'No Creo configuration, plugin, Ribbon or image files were modified. Review the report before sharing.'
    exit 0
} catch { Write-Error -ErrorAction Continue $_;exit 1 }
