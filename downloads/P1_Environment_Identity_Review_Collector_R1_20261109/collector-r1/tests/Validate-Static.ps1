param([Parameter(Mandatory=$true)][string]$ScriptsRoot)
$ErrorActionPreference='Stop'
$results=@();$forbidden=@('Start-Process','Invoke-Expression','Invoke-WebRequest','Invoke-RestMethod','Stop-Process','Remove-Item','Set-ExecutionPolicy','Set-ItemProperty','Restart-Computer','Start-Job','Enter-PSSession','java','jcmd','jstack','reg.exe')
foreach($file in Get-ChildItem -LiteralPath $ScriptsRoot -Filter '*.ps1'){
 $tokens=$null;$parseErrors=$null;$ast=[Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$parseErrors)
 if($parseErrors.Count){throw ('Parse failure: '+$file.Name)}
 $commands=@($ast.FindAll({param($n)$n -is [Management.Automation.Language.CommandAst]},$true)|ForEach-Object{$_.GetCommandName()}|Where-Object{$_}|Sort-Object -Unique)
 foreach($command in $commands){if($forbidden -contains $command){throw ('Forbidden command: '+$command)}}
 $sdkTypes=@($ast.FindAll({param($n)$n -is [Management.Automation.Language.TypeExpressionAst]},$true)|Where-Object{$_.TypeName.Name -match '^(com\.ptc|pfc|wfc)'})
 if($sdkTypes.Count){throw 'SDK type expression found'}
 $text=[IO.File]::ReadAllText($file.FullName)
 foreach($bad in @('Elements.create','Element_Create','GetStatusFlag','GetSolidFeatureStatusFlags','WCreateFeature','WRegenerate','RunMacro','CreateElementTree')){if($text.Contains($bad)){throw ('Forbidden SDK reference: '+$bad)}}
 $results+=@{file=$file.Name;parsePassed=$true;forbiddenCommandOrSdkTypeAbsent=$true;commands=$commands}
}
$results|ConvertTo-Json -Depth 8
