# Pure file/metadata helpers. No Creo/J-Link/TOOLKIT calls; dot sourcing does not collect anything.
Set-StrictMode -Version 2.0
function Initialize-P1Privacy {
    $script:P1PrivateValues = New-Object 'System.Collections.Generic.List[string]'
    foreach ($v in @($env:USERNAME, $env:COMPUTERNAME, $env:USERDOMAIN, [Environment]::UserName, [Environment]::MachineName)) {
        if ($v -and $v.Length -ge 3 -and -not $script:P1PrivateValues.Contains($v)) { [void]$script:P1PrivateValues.Add($v) }
    }
    $script:P1Profile = [Environment]::GetFolderPath('UserProfile')
}
function Protect-P1Text([AllowNull()][string]$Text) {
    if ($null -eq $Text) { return $null }
    $s = $Text
    if ($script:P1Profile) {
        foreach ($p in @($script:P1Profile, $script:P1Profile.Replace('\','/'))) {
            $s = [regex]::Replace($s, [regex]::Escape($p), '<USERPROFILE>', 'IgnoreCase')
        }
    }
    $s = [regex]::Replace($s, '(?i)(?:[A-Z]:[\\/](?:Users|Documents and Settings)[\\/])[^\\/\s;"'']+', '<USERPROFILE>')
    foreach ($v in $script:P1PrivateValues) {
        $s = [regex]::Replace($s, '(?<![\w])' + [regex]::Escape($v) + '(?![\w])', '<PRIVATE_ID>', 'IgnoreCase')
    }
    $s = [regex]::Replace($s, '\\\\[^\\\s]+\\[^\\\s]+', '<UNC_HOST_SHARE>')
    $s = [regex]::Replace($s, '(?i)[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}', '<EMAIL>')
    $s = [regex]::Replace($s, '(?<=\s)[!#].*$', '<INLINE_COMMENT_REDACTED>')
    $s = [regex]::Replace($s, '(?i)(?:-D)?(?:user[._-]?(?:name|home)|license|licence|server|host|proxy)=[^\s;]+', '<SENSITIVE_ARGUMENT_REDACTED>')
    $s = [regex]::Replace($s, '(?i)\b0x[0-9a-f]{6,}\b', '<ADDRESS_REDACTED>')
    $s = [regex]::Replace($s, '(?i)\bS-1-\d+(?:-\d+){2,}\b', '<SID>')
    $s = [regex]::Replace($s, '(?i)\b(?:password|passwd|token|secret|credential|api[_-]?key|authorization)\s*[:=]\s*\S+', '<SECRET_REDACTED>')
    $s = [regex]::Replace($s, '(?i)(https?://)([^/\s:@]+:[^/\s@]+@)', '$1<AUTH>@')
    $s = [regex]::Replace($s, '([?&])[^\s"'']+', '$1<QUERY_REDACTED>')
    # Model file names are not needed for environment identity.
    if ($s -match '(?i)\.(?:prt|asm|drw|neu|step|stp|iges|igs)(?:\.\d+)?(?:\s|[;"'']|$)') { return '<MODEL_REFERENCE_REMOVED>' }
    return $s
}
function Get-P1ShaBytes([byte[]]$Bytes) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-','').ToLowerInvariant() } finally { $sha.Dispose() }
}
function Get-P1Identity([string]$Path) {
    $display = Protect-P1Text $Path
    try {
        $before = Get-Item -LiteralPath $Path -ErrorAction Stop
        if ($before.PSIsContainer -or ($before.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw [IO.IOException]::new('Not a plain file') }
        $stream = [IO.File]::Open($before.FullName, [IO.FileMode]::Open, [IO.FileAccess]::Read, ([IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete))
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $digest = ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','').ToLowerInvariant() } finally { $stream.Dispose(); $sha.Dispose() }
        $after = Get-Item -LiteralPath $Path -ErrorAction Stop
        return [ordered]@{ path=$display; status='DISK_FILE_HASHED_NOT_PROOF_OF_LOADING'; bytes=$before.Length; sha256=$digest; lastWriteUtc=$before.LastWriteTimeUtc.ToString('o'); stableDuringRead=($before.Length -eq $after.Length -and $before.LastWriteTimeUtc -eq $after.LastWriteTimeUtc); loadedIdentityProven=$false }
    } catch { return [ordered]@{path=$display;status='UNAVAILABLE';errorClass=$_.Exception.GetType().Name;loadedIdentityProven=$false} }
}
function Read-P1TextSnapshot([string]$Path, [long]$MaxBytes, [int]$CodePage=0) {
    $file = Get-Item -LiteralPath $Path -ErrorAction Stop
    if ($file.PSIsContainer -or ($file.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw [IO.IOException]::new('Not a plain file') }
    if ($file.Length -gt $MaxBytes) { throw [IO.InvalidDataException]::new('TEXT_SIZE_LIMIT') }
    $stream = [IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,([IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete))
    try {
        $memory = New-Object IO.MemoryStream
        try {
            $buffer = New-Object byte[] 65536
            while (($n=$stream.Read($buffer,0,$buffer.Length)) -gt 0) {
                if ($memory.Length + $n -gt $MaxBytes) { throw [IO.InvalidDataException]::new('TEXT_SIZE_LIMIT') }
                $memory.Write($buffer,0,$n)
            }
            $bytes=$memory.ToArray()
        } finally { $memory.Dispose() }
    } finally { $stream.Dispose() }
    $offset=0; $encoding=$null; $reason=''
    if ($CodePage -ne 0) { $encoding=[Text.Encoding]::GetEncoding($CodePage);$reason='EXPLICIT_CODE_PAGE' }
    elseif ($bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191) { $encoding=New-Object Text.UTF8Encoding($false,$true);$offset=3;$reason='UTF8_BOM' }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 255 -and $bytes[1] -eq 254) { $encoding=[Text.Encoding]::Unicode;$offset=2;$reason='UTF16_LE_BOM' }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 254 -and $bytes[1] -eq 255) { $encoding=[Text.Encoding]::BigEndianUnicode;$offset=2;$reason='UTF16_BE_BOM' }
    else {
        $encoding=New-Object Text.UTF8Encoding($false,$true)
        try { [void]$encoding.GetString($bytes);$reason='VALID_UTF8_NO_BOM' }
        catch { $encoding=[Text.Encoding]::GetEncoding([Globalization.CultureInfo]::CurrentCulture.TextInfo.ANSICodePage);$reason='CULTURE_ANSI_FALLBACK_VERIFY_OR_SPECIFY_CODE_PAGE' }
    }
    $text=$encoding.GetString($bytes,$offset,$bytes.Length-$offset)
    $after=Get-Item -LiteralPath $Path -ErrorAction Stop
    return @{text=$text;sha256=(Get-P1ShaBytes $bytes);bytes=$bytes.Length;encoding=$encoding.WebName;encodingBasis=$reason;stableDuringRead=($file.Length -eq $after.Length -and $file.LastWriteTimeUtc -eq $after.LastWriteTimeUtc)}
}
function Resolve-P1Reference([string]$Value,[string]$BaseDirectory) {
    $v=$Value.Trim().Trim('"').Trim("'")
    if (-not $v -or $v -match '[<>*?]' -or $v -match '[\r\n]') { return $null }
    $v=[Environment]::ExpandEnvironmentVariables($v)
    if ($v -match '%[^%]+%|\$\(|\$\{|https?://') { return $null }
    try {
        if (-not [IO.Path]::IsPathRooted($v)) { $v=Join-Path $BaseDirectory $v }
        return [IO.Path]::GetFullPath($v)
    } catch { return $null }
}
function Convert-P1Configuration([string]$Text) {
    $lines=[regex]::Split($Text,'\r\n|\n|\r');$safe=New-Object 'System.Collections.Generic.List[string]';$refs=New-Object 'System.Collections.Generic.List[object]';$classpaths=New-Object 'System.Collections.Generic.List[object]';$redactions=0
    for($i=0;$i -lt $lines.Length;$i++) {
        $line=$lines[$i];$work=$line.TrimStart();$commented=$false
        if ($work -match '^[!#]+') { $commented=$true;$work=[regex]::Replace($work,'^[!#]+\s*','') }
        $isDat=$work -match '^(?i)(protkdat|creotkdat|toolkit_registry_file)\s+(.+?)\s*$'
        if ($isDat) {
            $key=$Matches[1];$value=$Matches[2]
            [void]$refs.Add(@{lineNumber=$i+1;key=$key;value=$value;commentedInThisFile=$commented;effectiveAtSessionStartup='UNKNOWN_DISK_REFERENCE_ONLY'})
        }
        if ($work -match '^(?i)java_app_classpath\s+(.+?)\s*$') { [void]$classpaths.Add(@{lineNumber=$i+1;value=$Matches[1];commentedInThisFile=$commented}) }
        if ($commented -and -not $isDat) { [void]$safe.Add('! <COMMENT_REDACTED>');$redactions++;continue }
        if ($work -match '^(\S+)\s+(.+)$') {
            $key=$Matches[1];$value=$Matches[2]
            if ($key -match '(?i)password|passwd|token|secret|credential|license|licence|server|host|proxy|user|login|email|api.key') { $value='<SENSITIVE_VALUE_REDACTED>' }
            elseif ($key -match '^(?i)(name|startup|java_app_class|java_app_classpath|java_app_start|java_app_stop|allow_stop|delay_start|text_dir|exec_file|toolkit|creo_type|protkdat|creotkdat|toolkit_registry_file|jlink[a-z0-9_]*|java[a-z0-9_]*|protk[a-z0-9_]*|creotk[a-z0-9_]*)$') { $value=Protect-P1Text $value }
            else { $value='<NON_ENV_VALUE_REDACTED>' }
            $clean=($(if($commented){'! '}else{''})+$key+' '+$value)
        } else { $clean=Protect-P1Text $line }
        if ($clean -cne $line) { $redactions++ };[void]$safe.Add($clean)
    }
    return @{sanitizedText=[string]::Join("`n",$safe.ToArray());lineCount=$lines.Length;changedLines=$redactions;references=$refs.ToArray();classpaths=$classpaths.ToArray()}
}
function Get-P1LogSummary([string]$Text) {
    $counts=@{};$events=@{};$classes=New-Object 'System.Collections.Generic.List[object]';$lastEvent=$null;$lineNumber=0
    $tokens=@('dbg_err_crash','EXCEPTION_ACCESS_VIOLATION','SIGSEGV','SIGABRT','UnsupportedClassVersionError','UnsatisfiedLinkError','NoClassDefFoundError','ClassNotFoundException','OutOfMemoryError','XLicense','XNotInitialized','XMethodNotLicensed','XToolkitAppNoLicense','XToolkitNoLicense','XToolkitAppInitializationFailed','XToolkitDllInitializeFailed')
    foreach($line in [regex]::Split($Text,'\r\n|\n|\r')) {
        $lineNumber++
        foreach($token in $tokens) { if($line -match [regex]::Escape($token)){if(-not $counts.ContainsKey($token)){$counts[$token]=0};$counts[$token]++} }
        # Only known class-loading syntax; never copy raw log lines, addresses or model payloads.
        if ($line -match '(?<type>(?:com\.ptc\.|p1\.controlled\.)[A-Za-z0-9_.$]+)\s+source:\s*(?<location>\S.*)$') {
            $typeName=$Matches['type'];$location=$Matches['location']
            if($location -notmatch '^(?i)(file:|jar:file:|[A-Z]:[\\/]|/|\\|shared objects file\b|__JVM_DefineClass__)'){$location='<UNRECOGNIZED_SOURCE_REDACTED>'}
            [void]$classes.Add(@{lineNumber=$lineNumber;className=$typeName;source=(Protect-P1Text $location);evidence='EXISTING_CLASS_LOAD_LOG_ENTRY_NOT_A_NEW_TRACE'})
        }
        if ($line.TrimStart().StartsWith('{')) {
            try {
                $row=$line|ConvertFrom-Json -ErrorAction Stop
                $property=$row.PSObject.Properties['event']
                if ($property -and [string]$property.Value -cmatch '^[A-Z][A-Z0-9_]{0,100}$') {
                    $event=[string]$property.Value;if(-not $events.ContainsKey($event)){$events[$event]=0};$events[$event]++;$lastEvent=$event
                }
            } catch { }
        }
    }
    return @{linesInspected=$lineNumber;canonicalErrorTokenLineCounts=$counts;existingClassLoadEntries=$classes.ToArray();eventCounts=$events;lastValidCanonicalEvent=$lastEvent;rawLogCopied=$false;nativeStackOrAddressesCopied=$false;loadOrderStatus=($(if($classes.Count){'PARTIAL_EXISTING_LOG_ORDER_ONLY'}else{'UNKNOWN_NO_RECOGNIZED_EXISTING_CLASS_LOAD_LINES'}));absenceOfErrorTokensDoesNotProveHealthy=$true}
}
function Get-P1Files([string]$Root,[string]$ExcludedDirectory,[int]$MaxFiles=20000,[int]$MaxDirectories=3000) {
    $found=New-Object 'System.Collections.Generic.List[string]';$issues=New-Object 'System.Collections.Generic.List[object]';$queue=New-Object 'System.Collections.Generic.Queue[string]';$queue.Enqueue($Root);$directories=0;$entries=0;$limited=$false
    try {
        $full=[IO.Path]::GetFullPath($Root).TrimEnd([char[]]'\/')
        $drive=[IO.Path]::GetPathRoot([IO.Path]::GetFullPath($Root)).TrimEnd([char[]]'\/')
        if($full -eq $drive -or $full -eq $script:P1Profile) {
            return @{files=@();issues=@(@{path=(Protect-P1Text $Root);status='DRIVE_OR_PROFILE_ROOT_SCAN_REJECTED'});scanLimited=$false;directoriesVisited=0;entriesSeen=0}
        }
    } catch { return @{files=@();issues=@(@{path=(Protect-P1Text $Root);status='INVALID_SCAN_ROOT'});scanLimited=$false;directoriesVisited=0;entriesSeen=0} }
    while($queue.Count -gt 0) {
        $directory=$queue.Dequeue()
        if ($directories -ge $MaxDirectories) { $limited=$true;break };$directories++
        if($ExcludedDirectory -and ($directory -eq $ExcludedDirectory -or $directory.StartsWith($ExcludedDirectory+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase))) { continue }
        try {
            $item=Get-Item -LiteralPath $directory -ErrorAction Stop
            if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){[void]$issues.Add(@{path=(Protect-P1Text $directory);status='REPARSE_POINT_NOT_TRAVERSED'});continue}
            foreach($child in Get-ChildItem -LiteralPath $directory -ErrorAction Stop) {
                $entries++;if($entries -gt $MaxFiles){$limited=$true;break}
                if($child.Attributes -band [IO.FileAttributes]::ReparsePoint){continue}
                if($child.PSIsContainer){$queue.Enqueue($child.FullName)}
                else { [void]$found.Add($child.FullName) }
            }
        } catch { [void]$issues.Add(@{path=(Protect-P1Text $directory);status='DIRECTORY_UNAVAILABLE';errorClass=$_.Exception.GetType().Name}) }
        if($limited){break}
    }
    return @{files=$found.ToArray();issues=$issues.ToArray();scanLimited=$limited;directoriesVisited=$directories;entriesSeen=$entries}
}
function Get-P1PeArchitecture([string]$Path) {
    try {
        $f=[IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::Read,([IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete));$b=New-Object IO.BinaryReader($f)
        try {if($b.ReadUInt16() -ne 0x5a4d){return 'UNKNOWN_NOT_PE'};$f.Position=0x3c;$offset=$b.ReadInt32();if($offset -lt 0 -or $offset -gt $f.Length-6){return 'UNKNOWN_BAD_PE'};$f.Position=$offset;if($b.ReadUInt32() -ne 0x4550){return 'UNKNOWN_BAD_PE'};$machine=$b.ReadUInt16();switch($machine){0x8664{return 'x64'};0x14c{return 'x86'};0xaa64{return 'arm64'};default{return 'UNKNOWN_MACHINE'}}}finally{$b.Dispose();$f.Dispose()}
    } catch { return 'UNKNOWN_UNREADABLE_PE' }
}
function Get-P1JvmMetadata([string]$JvmPath) {
    $identity=Get-P1Identity $JvmPath;$version=[Diagnostics.FileVersionInfo]::GetVersionInfo($JvmPath)
    $record=[ordered]@{loadedJvmDll=$identity;dllCompany=(Protect-P1Text $version.CompanyName);dllProductVersion=(Protect-P1Text $version.ProductVersion);dllFileVersion=(Protect-P1Text $version.FileVersion);architecture=(Get-P1PeArchitecture $JvmPath);releaseFile=$null;releaseFields=@{};javaHomeRuntimeProperty='NOT_READ_NO_ATTACH';versionSource='LOADED_JVM_DLL_FILE_METADATA'}
    $directory=[IO.Path]::GetDirectoryName($JvmPath)
    for($i=0;$i -lt 5 -and $directory;$i++) {
        $release=Join-Path $directory 'release'
        if(Test-Path -LiteralPath $release -PathType Leaf){
            $snapshot=Read-P1TextSnapshot $release 1048576
            $selected=@{}
            foreach($line in [regex]::Split($snapshot.text,'\r\n|\n|\r')){if($line -match '^(JAVA_VERSION|JAVA_RUNTIME_VERSION|IMPLEMENTOR|IMPLEMENTOR_VERSION|OS_ARCH|OS_NAME)="?([^"\r\n]*)"?$'){$selected[$Matches[1]]=Protect-P1Text $Matches[2]}}
            $record.releaseFile=Protect-P1Text $release;$record.releaseFields=$selected;$record.versionSource='LOADED_DLL_PLUS_NEARBY_RELEASE_NOT_LIVE_SYSTEM_PROPERTIES';break
        }
        $parent=[IO.Directory]::GetParent($directory);if($null -eq $parent){break};$directory=$parent.FullName
    }
    return $record
}
function Get-P1ClasspathFromCommand([AllowNull()][string]$CommandLine) {
    $found=New-Object 'System.Collections.Generic.List[object]'
    if($CommandLine){
        foreach($m in [regex]::Matches($CommandLine,'(?i)(?:^|\s)(?:-classpath|-cp|--class-path)(?:=|\s+)(?:"(?<quoted>[^"]*)"|(?<bare>\S+))')){
            $v=$m.Groups['quoted'].Value;if(-not $v){$v=$m.Groups['bare'].Value};[void]$found.Add(@{source='PROCESS_COMMAND_LINE_CLASSPATH_ARGUMENT_ONLY';value=(Protect-P1Text $v);provesLoadedClasses=$false})
        }
    }
    return $found.ToArray()
}
