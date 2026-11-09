#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidateNotNullOrEmpty()][string]$OutputRoot,
    [Parameter(Mandatory=$true)][ValidateNotNullOrEmpty()][string[]]$ConfigProPaths,
    [string[]]$InstallRoots=@(),
    [string[]]$PluginRoots=@(),
    [string[]]$OtkJarPaths=@(),
    [string[]]$PfcasyncJarPaths=@(),
    [string[]]$RegistryDatPaths=@(),
    [string[]]$LogPaths=@(),
    [ValidateRange(0,65535)][int]$TextCodePage=0,
    [ValidateRange(1024,67108864)][long]$MaxTextBytes=16777216,
    [ValidateRange(100,200000)][int]$MaxScanEntries=20000,
    [ValidateRange(10,20000)][int]$MaxScanDirectories=3000,
    [switch]$DiskOnly
)
Set-StrictMode -Version 2.0
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Collector.Core.ps1')
Initialize-P1Privacy
$issues=New-Object 'System.Collections.Generic.List[object]'
function Add-P1Issue([string]$Stage,[string]$Status,[string]$Path='',[string]$ErrorClass='') {
    [void]$issues.Add(@{stage=$Stage;status=$Status;path=(Protect-P1Text $Path);errorClass=$ErrorClass})
}
function Save-P1Json([string]$Name,$Value) {
    $p=Join-Path $evidenceRoot $Name
    $text=ConvertTo-Json -InputObject $Value -Depth 32
    [IO.File]::WriteAllText($p,$text+"`n",(New-Object Text.UTF8Encoding($false)))
}
function New-P1PathSet { return ,(New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)) }
function Add-P1Path($Set,[string]$Path) {
    if($Path){try{[void]$Set.Add([IO.Path]::GetFullPath($Path))}catch{Add-P1Issue 'PATH' 'INVALID_PATH' $Path $_.Exception.GetType().Name}}
}
function Get-P1ProcessEvidence {
    $records=New-Object 'System.Collections.Generic.List[object]';$discoveredRoots=New-Object 'System.Collections.Generic.List[string]'
    if($DiskOnly){return @{status='DISK_ONLY_PROCESS_INSPECTION_SKIPPED';records=@();discoveredRoots=@();moduleLoadOrder='UNKNOWN'}}
    if([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT){return @{status='NOT_WINDOWS_NO_PROCESS_INSPECTION';records=@();discoveredRoots=@();moduleLoadOrder='UNKNOWN'}}
    try{
        $creo=@(Get-CimInstance -ClassName Win32_Process -Filter "Name='xtop.exe' OR Name='parametric.exe' OR Name='proe.exe'" -ErrorAction Stop)
        $ids=@($creo|ForEach-Object{$_.ProcessId});$processes=New-Object 'System.Collections.Generic.List[object]'
        foreach($p in $creo){[void]$processes.Add($p)}
        if($ids.Count){
            foreach($p in @(Get-CimInstance -ClassName Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction Stop)){
                if($ids -contains $p.ParentProcessId){[void]$processes.Add($p)}
            }
        }
        foreach($p in $processes){
            $jvms=New-Object 'System.Collections.Generic.List[object]';$modules=New-Object 'System.Collections.Generic.List[object]';$image=$null
            if($p.ExecutablePath){$image=Get-P1Identity $p.ExecutablePath
                if($p.Name -match '^(?i)(xtop|parametric|proe)\.exe$'){
                    $v=[Diagnostics.FileVersionInfo]::GetVersionInfo($p.ExecutablePath)
                    $image['fileVersion']=Protect-P1Text $v.FileVersion;$image['productVersion']=Protect-P1Text $v.ProductVersion;$image['company']=Protect-P1Text $v.CompanyName
                    $directory=[IO.Path]::GetDirectoryName($p.ExecutablePath)
                    # Infer Common Files root only if its sibling text/java layout exists.
                    for($i=0;$i -lt 5 -and $directory;$i++){
                        if(Test-Path -LiteralPath (Join-Path $directory 'text') -PathType Container){[void]$discoveredRoots.Add($directory);break}
                        $parent=[IO.Directory]::GetParent($directory);if($null -eq $parent){break};$directory=$parent.FullName
                    }
                }
            }
            $moduleStatus='UNKNOWN'
            try{
                $process=Get-Process -Id $p.ProcessId -ErrorAction Stop;$ordinal=0
                foreach($module in $process.Modules){
                    $ordinal++
                    if($module.ModuleName -match '^(?i)(jvm|otk[^.]*|pfc[^.]*|cip[^.]*|protk[^.]*)\.dll$'){
                        [void]$modules.Add(@{enumerationIndex=$ordinal;orderMeaning='ENUMERATION_ORDER_NOT_LOAD_TIME';file=(Get-P1Identity $module.FileName)})
                        if($module.ModuleName -ieq 'jvm.dll'){[void]$jvms.Add((Get-P1JvmMetadata $module.FileName))}
                    }
                };$moduleStatus='READ_ONLY_OS_MODULE_ENUMERATION_COMPLETE_NOT_JAR_CODESOURCE'
            }catch{$moduleStatus='UNAVAILABLE';Add-P1Issue 'PROCESS_MODULES' 'UNKNOWN_NO_ATTACH_OR_ESCALATION_ATTEMPTED' '' $_.Exception.GetType().Name}
            [void]$records.Add(@{pid=$p.ProcessId;parentPid=$p.ParentProcessId;processName=$p.Name;executable=$image;creationTime=([string]$p.CreationDate);commandLineCopied=$false;classpathArguments=@(Get-P1ClasspathFromCommand $p.CommandLine);moduleStatus=$moduleStatus;selectedModules=$modules.ToArray();jvmEvidence=$jvms.ToArray();licenseOrNativeSupportCertified=$false})
        }
        return @{status=($(if($creo.Count){'CREO_PROCESSES_OBSERVED'}else{'NO_CREO_PROCESS_OBSERVED'}));records=$records.ToArray();discoveredRoots=$discoveredRoots.ToArray();moduleLoadOrder='UNKNOWN_UNLESS_EXISTING_CLASS_LOAD_LOGS_SUPPLIED'}
    }catch{Add-P1Issue 'PROCESS_QUERY' 'UNAVAILABLE' '' $_.Exception.GetType().Name;return @{status='PROCESS_QUERY_UNAVAILABLE';records=@();discoveredRoots=@();moduleLoadOrder='UNKNOWN'}}
}
# Create a new evidence folder only. Never overwrite an existing evidence run or any input.
$outputBase=[IO.Path]::GetFullPath($OutputRoot)
foreach($inputRoot in @($InstallRoots)+@($PluginRoots)){
    if(-not $inputRoot){continue}
    $full=[IO.Path]::GetFullPath($inputRoot).TrimEnd([char[]]'\/')
    if($full -eq [IO.Path]::GetPathRoot($full).TrimEnd([char[]]'\/') -or $full -eq $script:P1Profile){throw 'Refusing drive/profile root scan. Supply a specific installation/plugin directory.'}
    if($outputBase -eq $full -or $outputBase.StartsWith($full+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'OutputRoot must be outside installation/plugin scan roots.'}
}
$stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ');$runName='P1_EnvIdentity_'+$stamp+'_'+[Guid]::NewGuid().ToString('N').Substring(0,8)
[void][IO.Directory]::CreateDirectory($outputBase)
$evidenceRoot=Join-Path $outputBase $runName
if(Test-Path -LiteralPath $evidenceRoot){throw 'Evidence directory already exists.'}
[void](New-Item -ItemType Directory -Path $evidenceRoot -ErrorAction Stop)
[void][IO.Directory]::CreateDirectory((Join-Path $evidenceRoot 'configuration'))
$started=[DateTime]::UtcNow.ToString('o')
$processEvidence=Get-P1ProcessEvidence
$allInstall=New-P1PathSet;$plugins=New-P1PathSet;$otk=New-P1PathSet;$async=New-P1PathSet;$configs=New-P1PathSet;$datFiles=New-P1PathSet
foreach($p in @($InstallRoots)+@($processEvidence.discoveredRoots)){Add-P1Path $allInstall $p}
foreach($p in $PluginRoots){Add-P1Path $plugins $p}
foreach($p in $ConfigProPaths){Add-P1Path $configs $p}
foreach($p in $OtkJarPaths){Add-P1Path $otk $p}
foreach($p in $PfcasyncJarPaths){Add-P1Path $async $p}
foreach($p in $RegistryDatPaths){Add-P1Path $datFiles $p}
$scanEvidence=New-Object 'System.Collections.Generic.List[object]'
$installImages=New-Object 'System.Collections.Generic.List[object]'
foreach($directory in $allInstall){
    $scan=Get-P1Files $directory $evidenceRoot $MaxScanEntries $MaxScanDirectories
    [void]$scanEvidence.Add(@{root=(Protect-P1Text $directory);kind='INSTALLATION';limited=$scan.scanLimited;directoriesVisited=$scan.directoriesVisited;entriesSeen=$scan.entriesSeen;issues=$scan.issues})
    foreach($p in $scan.files){switch -Regex ([IO.Path]::GetFileName($p)){'^(?i)(xtop|parametric|proe)\.exe$'{
        $image=Get-P1Identity $p
        try{$v=[Diagnostics.FileVersionInfo]::GetVersionInfo($p);$image['fileVersion']=Protect-P1Text $v.FileVersion;$image['productVersion']=Protect-P1Text $v.ProductVersion;$image['company']=Protect-P1Text $v.CompanyName}catch{$image['fileVersionStatus']='UNKNOWN'}
        $image['architecture']=Get-P1PeArchitecture $p;$image['discovery']='INSTALLATION_DISK_SCAN_NOT_PROCESS_IDENTITY';[void]$installImages.Add($image)
    };'^(?i)otk\.jar$'{Add-P1Path $otk $p};'^(?i)pfcasync\.jar$'{Add-P1Path $async $p};'^(?i)config\.(pro|sup)$'{Add-P1Path $configs $p}}}
}
$configRecords=New-Object 'System.Collections.Generic.List[object]';$references=New-Object 'System.Collections.Generic.List[object]';$classpaths=New-Object 'System.Collections.Generic.List[object]'
function Read-P1ConfigurationFile([string]$Path,[string]$Kind) {
    if(($Kind -eq 'CONFIG' -and [IO.Path]::GetFileName($Path) -notmatch '^(?i)config\.(pro|sup)$') -or ($Kind -eq 'DAT' -and [IO.Path]::GetExtension($Path) -ine '.dat')){Add-P1Issue 'CONFIGURATION' 'UNSUPPORTED_INPUT_NOT_READ' $Path;return}
    try{
        $snapshot=Read-P1TextSnapshot $Path $MaxTextBytes $TextCodePage
        $parsed=Convert-P1Configuration $snapshot.text
        $leaf=('{0:D4}_{1}.sanitized.txt' -f ($configRecords.Count+1),$Kind.ToLowerInvariant());$relative='configuration/'+$leaf
        [IO.File]::WriteAllText((Join-Path (Join-Path $evidenceRoot 'configuration') $leaf),$parsed.sanitizedText,(New-Object Text.UTF8Encoding($false)))
        [void]$configRecords.Add(@{path=(Protect-P1Text $Path);kind=$Kind;originalSnapshotSha256=$snapshot.sha256;bytesRead=$snapshot.bytes;encoding=$snapshot.encoding;encodingBasis=$snapshot.encodingBasis;stableDuringRead=$snapshot.stableDuringRead;lineCount=$parsed.lineCount;changedLines=$parsed.changedLines;fullLineSequencePreserved=$true;rawUnredactedFileCopied=$false;sanitizedFile=$relative;effectiveStartupConfiguration='UNKNOWN_DISK_SNAPSHOT_ONLY'})
        foreach($reference in $parsed.references){
            $resolved=Resolve-P1Reference $reference.value ([IO.Path]::GetDirectoryName($Path))
            [void]$references.Add(@{config=(Protect-P1Text $Path);lineNumber=$reference.lineNumber;key=$reference.key;configuredReference=(Protect-P1Text $reference.value);resolvedPath=(Protect-P1Text $resolved);resolutionBasis='CONFIG_FILE_DIRECTORY_FOR_RELATIVE_PATH_VERIFY_CREO_STARTUP_WORKING_DIRECTORY';commentedInThisFile=$reference.commentedInThisFile;effectiveAtSessionStartup='UNKNOWN';existsAtCollection=($resolved -and (Test-Path -LiteralPath $resolved -PathType Leaf))})
            if($resolved){Add-P1Path $datFiles $resolved}
        }
        foreach($cp in $parsed.classpaths){
            [void]$classpaths.Add(@{dat=(Protect-P1Text $Path);lineNumber=$cp.lineNumber;value=(Protect-P1Text $cp.value);commentedInThisFile=$cp.commentedInThisFile;provesLoadedClasses=$false})
            # Semicolon is the documented Windows path separator. Never evaluate expressions.
            $classpathText=Split-P1PathValue $cp.value
            if($null -eq $classpathText){Add-P1Issue 'CLASSPATH' 'UNBALANCED_QUOTES_NOT_RESOLVED' $Path;continue}
            foreach($quoteChar in @([char]34,[char]39)){
                if($classpathText.Length -ge 2 -and $classpathText[0] -eq $quoteChar -and $classpathText[$classpathText.Length-1] -eq $quoteChar -and ($classpathText.ToCharArray()|Where-Object{$_ -eq $quoteChar}|Measure-Object).Count -eq 2){$classpathText=$classpathText.Substring(1,$classpathText.Length-2);break}
            }
            foreach($part in $classpathText.Split(';')){
                $resolved=Resolve-P1Reference $part ([IO.Path]::GetDirectoryName($Path))
                if($resolved -and -not $cp.commentedInThisFile){
                    $leaf=[IO.Path]::GetFileName($resolved)
                    if($leaf -ieq 'otk.jar'){Add-P1Path $otk $resolved}
                    elseif($leaf -ieq 'pfcasync.jar'){Add-P1Path $async $resolved}
                    elseif(Test-Path -LiteralPath $resolved -PathType Container){Add-P1Path $plugins $resolved}
                    elseif([IO.Path]::GetExtension($resolved) -ieq '.jar'){Add-P1Path $plugins $resolved}
                }
            }
        }
    }catch{Add-P1Issue 'CONFIGURATION' 'UNREADABLE_OR_SIZE_LIMIT_NO_RAW_COPY' $Path $_.Exception.GetType().Name}
}
foreach($p in @($configs)){Read-P1ConfigurationFile $p 'CONFIG'}
$datRead=New-P1PathSet
# Registry files can reference another dat; bound the closure and keep original files untouched.
while($datRead.Count -lt $datFiles.Count -and $datRead.Count -lt 200){
    foreach($p in @($datFiles)){
        if($datRead.Count -ge 200){break}
        if($datRead.Add($p)){Read-P1ConfigurationFile $p 'DAT'}
    }
}
if($datRead.Count -lt $datFiles.Count){Add-P1Issue 'DAT' 'REFERENCE_LIMIT_REACHED'}
$pluginRecords=New-Object 'System.Collections.Generic.List[object]';$pluginSeen=New-P1PathSet
foreach($root in $plugins){
    $files=@()
    if(Test-Path -LiteralPath $root -PathType Leaf){$files=@($root)}
    elseif(Test-Path -LiteralPath $root -PathType Container){
        $scan=Get-P1Files $root $evidenceRoot $MaxScanEntries $MaxScanDirectories;$files=@($scan.files)
        [void]$scanEvidence.Add(@{root=(Protect-P1Text $root);kind='PLUGIN';limited=$scan.scanLimited;directoriesVisited=$scan.directoriesVisited;entriesSeen=$scan.entriesSeen;issues=$scan.issues})
    }else{Add-P1Issue 'PLUGIN' 'PATH_NOT_FOUND' $root}
    foreach($p in $files){
        if([IO.Path]::GetExtension($p) -notin @('.jar','.class')){continue}
        $leaf=[IO.Path]::GetFileName($p)
        if($leaf -ieq 'otk.jar'){Add-P1Path $otk $p;continue};if($leaf -ieq 'pfcasync.jar'){Add-P1Path $async $p;continue}
        if($pluginSeen.Add($p)){[void]$pluginRecords.Add((Get-P1Identity $p))}
    }
}
$otkRecords=@(foreach($p in $otk){if([IO.Path]::GetFileName($p) -ieq 'otk.jar'){Get-P1Identity $p}else{Add-P1Issue 'SDK' 'EXPECTED_OTK_JAR_NAME' $p}})
$asyncRecords=@(foreach($p in $async){if([IO.Path]::GetFileName($p) -ieq 'pfcasync.jar'){Get-P1Identity $p}else{Add-P1Issue 'SDK' 'EXPECTED_PFCASYNC_JAR_NAME' $p}})
$logRecords=New-Object 'System.Collections.Generic.List[object]'
foreach($p in $LogPaths){
    if([IO.Path]::GetExtension($p) -notin @('.log','.txt','.err','.out','.jsonl')){Add-P1Issue 'LOG' 'NOT_A_SUPPORTED_TEXT_LOG_NO_RAW_COPY' $p;continue}
    try{
        $snapshot=Read-P1TextSnapshot $p $MaxTextBytes $TextCodePage
        [void]$logRecords.Add(@{path=(Protect-P1Text $p);originalSnapshotSha256=$snapshot.sha256;bytesRead=$snapshot.bytes;encoding=$snapshot.encoding;encodingBasis=$snapshot.encodingBasis;stableDuringRead=$snapshot.stableDuringRead;associationToCurrentSession='UNKNOWN_VERIFY_PID_AND_START_TIME';summary=(Get-P1LogSummary $snapshot.text)})
    }catch{Add-P1Issue 'LOG' 'UNREADABLE_OR_SIZE_LIMIT_NO_RAW_COPY' $p $_.Exception.GetType().Name}
}
$processEvidence.discoveredRoots=@($processEvidence.discoveredRoots|ForEach-Object{Protect-P1Text $_})
Save-P1Json 'processes-and-jvm.json' $processEvidence
Save-P1Json 'installation-images.json' @{images=$installImages.ToArray();discoveredBy='BOUNDED_INSTALLATION_SCAN';diskImagesAreNotProofOfRunningVersion=$true}
Save-P1Json 'sdk-identities.json' @{otk=$otkRecords;pfcasync=$asyncRecords;allRecordsAreDiskIdentitiesOnly=$true;actualJarLoading='UNKNOWN_UNLESS_CORRELATED_EXISTING_LOAD_LOGS';bothNamesOnDiskDoNotProveMixedClasspath=$true}
Save-P1Json 'plugin-identities.json' @{files=$pluginRecords.ToArray();binariesCopied=$false;loadedBinaryEqualsDiskFile='UNKNOWN';sourcesOrModelsCopied=$false}
Save-P1Json 'configuration-index.json' @{snapshots=$configRecords.ToArray();datReferences=$references.ToArray();javaAppClasspaths=$classpaths.ToArray();effectivePrecedence='UNKNOWN_NOT_CHANGED_OR_GUESSED';commentedReferencesAreNotActive=$true}
Save-P1Json 'log-summaries.json' @{logs=$logRecords.ToArray();rawLogsOrCrashDumpsCopied=$false;loadOrderCompleteness='UNKNOWN_PARTIAL_EXISTING_LINES_ONLY'}
Save-P1Json 'scan-coverage.json' @{scans=$scanEvidence.ToArray();issues=$issues.ToArray();allConfigurationsOrModulesFound=$false;driveOrProfileRootScanAllowed=$false}
Save-P1Json 'collector-manifest.json' @{schema='P1_STEP25_PASSIVE_ENV_IDENTITY_V1';collectorRevision='STEP25_R1_INLINE_DAT_COMMENTS';collectorCoreSha256=(Get-P1Identity (Join-Path $PSScriptRoot 'Collector.Core.ps1'));startedUtc=$started;finishedUtc=[DateTime]::UtcNow.ToString('o');collectorPowerShellVersion=$PSVersionTable.PSVersion.ToString();collectorPowerShellIsNotCreoJvm=$true;diskOnly=[bool]$DiskOnly;scriptSha256=(Get-P1Identity $PSCommandPath);configurationCodePageRequested=$TextCodePage;maxTextBytes=$MaxTextBytes;maxScanEntries=$MaxScanEntries;maxScanDirectories=$MaxScanDirectories;maxRegistryDatSnapshots=200;privacyPolicy='FULL_CONFIG_LINE_SEQUENCE_REDACTED_LOG_WHITELIST_ONLY';manualPrivacyReviewStillRequiredForUnrecognizedSensitiveValues=$true;modelApisCalled=$false;wfcObjectsConstructed=$false;newBusinessButton=$false;javaOrCreoStarted=$false;attachOrDebuggerUsed=$false;configurationEdited=$false;onceLocksDeletedOrRearmed=$false;autoUpload=$false;licenseAndRegistrationSupportVerdict='UNVERIFIED';rawConfigsCopied=$false}
$hashes=New-Object 'System.Collections.Generic.List[string]'
foreach($f in Get-ChildItem -LiteralPath $evidenceRoot -Recurse -File){$id=Get-P1Identity $f.FullName;if($id.status -ne 'DISK_FILE_HASHED_NOT_PROOF_OF_LOADING'){throw 'Output hash failed'};$relative=$f.FullName.Substring($evidenceRoot.Length+1).Replace('\','/');[void]$hashes.Add($id.sha256+'  '+$relative)}
[IO.File]::WriteAllText((Join-Path $evidenceRoot 'SHA256SUMS'),([string]::Join("`n",$hashes.ToArray())+"`n"),(New-Object Text.UTF8Encoding($false)))
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=Join-Path $outputBase ($runName+'.zip')
[IO.Compression.ZipFile]::CreateFromDirectory($evidenceRoot,$archive,[IO.Compression.CompressionLevel]::Optimal,$true)
$zipIdentity=Get-P1Identity $archive
[IO.File]::WriteAllText(($archive+'.sha256'),($zipIdentity.sha256+'  '+[IO.Path]::GetFileName($archive)+"`n"),(New-Object Text.UTF8Encoding($false)))
[pscustomobject]@{status='LOCAL_EVIDENCE_CREATED_NOT_UPLOADED';archive=$archive;sha256=$zipIdentity.sha256;issues=$issues.Count;runtimeSupportVerdict='UNVERIFIED'}
