param([Parameter(Mandatory=$true)][string]$FixtureRoot)
$ErrorActionPreference='Stop'
. (Join-Path (Split-Path $PSScriptRoot -Parent) 'scripts/Collector.Core.ps1')
Initialize-P1Privacy
$checks=0
function Check([bool]$Condition,[string]$Message){$script:checks++;if(-not $Condition){throw ('Check failed: '+$Message)}}
Check (-not (Protect-P1Text 'frame at 0xCAFEBABE').Contains('CAFEBABE')) 'hexadecimal addresses removed'
$oldProfile=$script:P1Profile;$script:P1Profile='D:\Profiles\HiddenPerson'
Check ((Protect-P1Text 'D:/Profiles/HiddenPerson/sdk/otk.jar') -eq '<USERPROFILE>/sdk/otk.jar') 'relocated Windows profile forward slash redacted'
$script:P1Profile=$oldProfile
$config="! personal note Alice alice@example.com`nprotkdat active.dat`n! protkdat commented.dat`npassword ultra-secret`nfile_open_dir C:\Users\Alice\private`nmodel_path C:\private\body.prt.1`n! ordinary personal comment`ncustom_person Alice-Private-Name`n"
$converted=Convert-P1Configuration $config
Check ($converted.references.Count -eq 2) 'active/commented dat preserved'
Check (-not $converted.references[0].commentedInThisFile) 'active flag'
Check $converted.references[1].commentedInThisFile 'commented flag'
Check (-not $converted.sanitizedText.Contains('Alice')) 'profile and comments removed'
Check (-not $converted.sanitizedText.Contains('ultra-secret')) 'secret removed'
Check (-not $converted.sanitizedText.Contains('body.prt')) 'model reference removed'
Check (-not $converted.sanitizedText.Contains('Alice-Private-Name')) 'unknown config values redacted'
Check ($converted.lineCount -eq ([regex]::Split($config,'\r\n|\n|\r')).Length) 'all config lines retained'
Check ($null -eq (Resolve-P1Reference '%UNKNOWN_P1_TEST_VAR%/bad.dat' $FixtureRoot)) 'unknown env not guessed'
Check ($null -eq (Resolve-P1Reference '$(Write-Host HACK)' $FixtureRoot)) 'expressions never evaluated'
Check ((Resolve-P1Reference 'active.dat' $FixtureRoot) -eq (Join-Path $FixtureRoot 'active.dat')) 'relative reference basis explicit'
$cp=@(Get-P1ClasspathFromCommand 'xtop.exe private.prt -cp "C:\Users\Alice\lib\otk.jar;D:\sdk\pfcasync.jar" password=neverstore')
Check ($cp.Count -eq 1) 'only classpath argument kept'
Check (-not $cp[0].value.Contains('Alice')) 'classpath profile redacted'
Check (-not ($cp|ConvertTo-Json).Contains('private.prt')) 'model process argument excluded'
$log="dbg_err_crash at 0x1234 secret-model.prt`n[0.1s][info][class,load] com.ptc.pfc.pfcSession.Session source: C:\Users\Alice\sdk\otk.jar`n{`"event`":`"ROOT_ELEMENT_CREATE_START`",`"faceXYZ`": [1,2,3],`"selectedFaceHandle`": 99999}`nUnsupportedClassVersionError token=do-not-copy"
$summary=Get-P1LogSummary $log;$summaryJson=$summary|ConvertTo-Json -Depth 20
Check ($summary.canonicalErrorTokenLineCounts.dbg_err_crash -eq 1) 'canonical failure counter'
Check ($summary.existingClassLoadEntries.Count -eq 1) 'existing load source extracted'
Check ($summary.existingClassLoadEntries[0].source.Contains('otk.jar')) 'absolute Windows source retained after redaction'
Check ($summary.lastValidCanonicalEvent -eq 'ROOT_ELEMENT_CREATE_START') 'safe event projection'
Check (-not $summaryJson.Contains('secret-model')) 'raw model log excluded'
Check (-not $summaryJson.Contains('faceXYZ')) 'coordinate payload excluded'
Check (-not $summaryJson.Contains('99999')) 'handle excluded'
Check (-not $summaryJson.Contains('do-not-copy')) 'raw secret log excluded'
Check (-not $summaryJson.Contains('Alice')) 'load source redacted'
$one=Get-P1Identity (Join-Path $FixtureRoot 'config.pro')
Check ($one.status -eq 'DISK_FILE_HASHED_NOT_PROOF_OF_LOADING') 'hash is disk only'
Check (-not $one.loadedIdentityProven) 'no false loaded identity'
$snapshot=Read-P1TextSnapshot (Join-Path $FixtureRoot 'config.pro') 1048576
Check ($snapshot.sha256 -eq $one.sha256) 'snapshot hash matches bytes read'
$threw=$false;try{Read-P1TextSnapshot (Join-Path $FixtureRoot 'config.pro') 4|Out-Null}catch{$threw=$true};Check $threw 'oversize text rejected, not silently truncated'
Check ((Get-P1Identity (Join-Path $FixtureRoot 'missing.jar')).status -eq 'UNAVAILABLE') 'missing file stays unknown'
$encoded=Read-P1TextSnapshot (Join-Path $FixtureRoot 'legacy-encoding.txt') 1048576 936
Check ($encoded.text.Contains(([string][char]0x5de5)+([string][char]0x5177))) 'explicit Windows Chinese code page decoded'
$scan=Get-P1Files $FixtureRoot '' 1000 100
Check ($scan.files.Count -gt 0) 'bounded scan finds files'
$limited=Get-P1Files $FixtureRoot '' 1 100
Check $limited.scanLimited 'scan truncation is explicit'
$blockedRoot=Get-P1Files ([IO.Path]::GetPathRoot($FixtureRoot)) '' 100 10
Check ($blockedRoot.directoriesVisited -eq 0 -and $blockedRoot.issues[0].status -eq 'DRIVE_OR_PROFILE_ROOT_SCAN_REJECTED') 'drive roots rejected even from dat discovery'
Write-Output ('PURE_COLLECTOR_CHECKS_PASSED='+$checks)
