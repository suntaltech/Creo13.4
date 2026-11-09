#requires -Version 5.1
[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][string]$ProToolkitRoot,
 [Parameter(Mandatory=$true)][string]$OutputRoot,
 [ValidateRange(100,200000)][int]$MaxEntries=20000,
 [ValidateRange(10,20000)][int]$MaxDirectories=3000
)
Set-StrictMode -Version 2.0
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($ProToolkitRoot).TrimEnd([char[]]'\/')
$base=[IO.Path]::GetFullPath($OutputRoot)
$drive=[IO.Path]::GetPathRoot([IO.Path]::GetFullPath($ProToolkitRoot)).TrimEnd([char[]]'\/')
if($root -eq $drive -or $root -eq [Environment]::GetFolderPath('UserProfile')){throw 'Supply a specific SDK directory, not a drive/profile root.'}
if($base -eq $root -or $base.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'OutputRoot must be outside the SDK directory.'}
$name='P1_CPP_SDK_Inventory_'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'_'+[Guid]::NewGuid().ToString('N').Substring(0,8)
[void][IO.Directory]::CreateDirectory($base)
$out=Join-Path $base $name
if(Test-Path -LiteralPath $out){throw 'Output run already exists.'}
[void](New-Item -ItemType Directory -Path $out -ErrorAction Stop)
$files=New-Object 'System.Collections.Generic.List[object]';$issues=New-Object 'System.Collections.Generic.List[object]';$q=New-Object 'System.Collections.Generic.Queue[string]';$q.Enqueue($root)
$entries=0;$dirs=0;$limited=$false
while($q.Count -gt 0){
 $dir=$q.Dequeue();if($dirs -ge $MaxDirectories){$limited=$true;break};$dirs++
 try{
  $item=Get-Item -LiteralPath $dir -ErrorAction Stop
  if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){[void]$issues.Add(@{stage='DIRECTORY';status='REPARSE_POINT_SKIPPED'});continue}
  foreach($child in Get-ChildItem -LiteralPath $dir -ErrorAction Stop){
   $entries++;if($entries -gt $MaxEntries){$limited=$true;break}
   if($child.Attributes -band [IO.FileAttributes]::ReparsePoint){continue}
   if($child.PSIsContainer){$q.Enqueue($child.FullName);continue}
   $ext=$child.Extension.ToLowerInvariant()
   $wanted=$ext -in @('.h','.lib','.mak','.mk','.vcxproj','.props','.targets') -or $child.Name -match '(?i)^(makefile.*|.*(?:readme|platform|release).*\.(?:txt|md|html))$'
   if(-not $wanted){continue}
   $relative=$child.FullName.Substring($root.Length+1).Replace([char]92,[char]47)
   foreach($private in @([Environment]::UserName,[Environment]::MachineName)){
    if($private -and $private.Length -ge 3){$relative=[regex]::Replace($relative,'(?<![\w])'+[regex]::Escape($private)+'(?![\w])','<PRIVATE_ID>','IgnoreCase')}
   }
   try{
    $before=Get-Item -LiteralPath $child.FullName -ErrorAction Stop
    $digest=(Get-FileHash -LiteralPath $child.FullName -Algorithm SHA256 -ErrorAction Stop).Hash.ToLowerInvariant()
    $after=Get-Item -LiteralPath $child.FullName -ErrorAction Stop
    [void]$files.Add(@{relativeName=$relative;bytes=$before.Length;sha256=$digest;stableDuringRead=($before.Length -eq $after.Length -and $before.LastWriteTimeUtc -eq $after.LastWriteTimeUtc);kind=($(if($ext -eq '.h'){'HEADER'}elseif($ext -eq '.lib'){'LIBRARY_CANDIDATE_NOT_ARCHITECTURE_CERTIFIED'}else{'BUILD_OR_DOC_METADATA_ONLY'}))})
   }catch{[void]$issues.Add(@{relativeName=$relative;stage='FILE_HASH';status='UNAVAILABLE';errorClass=$_.Exception.GetType().Name})}
  }
 }catch{[void]$issues.Add(@{stage='DIRECTORY';status='UNAVAILABLE';errorClass=$_.Exception.GetType().Name})}
 if($limited){break}
}
$critical=@('ProToolkit.h','ProMdl.h','ProSolid.h','ProSolidBody.h','ProModelitem.h','ProSelection.h','ProSurface.h','ProFeature.h','ProElement.h','ProElemId.h','ProValue.h','ProArray.h','ProSmtDrvSurf.h')
$headers=New-Object 'System.Collections.Generic.List[object]'
foreach($wanted in $critical){$hits=@($files|Where-Object{[IO.Path]::GetFileName($_.relativeName) -ieq $wanted});[void]$headers.Add(@{candidateHeader=$wanted;observed=($hits.Count -gt 0);status=($(if($hits.Count){'OBSERVED_NAME_ONLY'}else{'NOT_OBSERVED_IN_SCANNED_SCOPE_NOT_PROOF_SDK_MISSING'}));exactRequirementMustBeCheckedAgainstVersionDocs=$true})}
$result=@{schema='P1_STEP27_CPP_SDK_DISK_INVENTORY_V1';collectedUtc=[DateTime]::UtcNow.ToString('o');rootLabel='<PROTOOLKIT_ROOT>';absoluteRootNotStored=$true;entriesSeen=$entries;directoriesVisited=$dirs;scanLimited=$limited;maxEntries=$MaxEntries;maxDirectories=$MaxDirectories;files=$files.ToArray();candidateHeaders=$headers.ToArray();issues=$issues.ToArray();rawSdkFilesCopied=$false;compilerVersion='UNKNOWN_NOT_QUERIED';libraryAbiAndOfficialSupport='UNKNOWN_NOT_VERIFIED';license='UNKNOWN_NOT_COLLECTED';creoOrCompilerStarted=$false;dllOrObjectGenerated=$false;siteConfigChanged=$false;modelApiCalled=$false;targetReady=$false}
$utf8=New-Object Text.UTF8Encoding($false)
$jsonPath=Join-Path $out 'sdk-inventory.json'
[IO.File]::WriteAllText($jsonPath,((ConvertTo-Json -InputObject $result -Depth 16)+"`n"),$utf8)
[IO.File]::WriteAllText((Join-Path $out 'SHA256SUMS'),((Get-FileHash -LiteralPath $jsonPath -Algorithm SHA256).Hash.ToLowerInvariant()+"  sdk-inventory.json`n"),$utf8)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=Join-Path $base ($name+'.zip');[IO.Compression.ZipFile]::CreateFromDirectory($out,$zip,[IO.Compression.CompressionLevel]::Optimal,$true)
$sha=(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText(($zip+'.sha256'),($sha+'  '+[IO.Path]::GetFileName($zip)+"`n"),$utf8)
[pscustomobject]@{status='LOCAL_METADATA_INVENTORY_ONLY_NOT_SDK_READY';archive=$zip;sha256=$sha;scanLimited=$limited;issues=$issues.Count}
