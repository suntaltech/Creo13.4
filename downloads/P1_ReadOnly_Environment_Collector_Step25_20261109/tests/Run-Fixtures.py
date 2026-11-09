from pathlib import Path
from zipfile import ZipFile
import os,subprocess,tempfile,json,hashlib,argparse,shutil,platform
parser=argparse.ArgumentParser();parser.add_argument('--pwsh',default=shutil.which('pwsh') or shutil.which('powershell'));args=parser.parse_args();assert args.pwsh,'Supply --pwsh with a trusted PowerShell executable';p=Path(__file__).resolve().parents[1];pwsh=args.pwsh;env=os.environ.copy();env['POWERSHELL_TELEMETRY_OPTOUT']='1'
def run(args):
 x=subprocess.run([pwsh,'-NoLogo','-NoProfile']+args,text=True,capture_output=True,env=env);return x
ps_version=run(['-Command','$PSVersionTable.PSVersion.ToString()']).stdout.strip()
with tempfile.TemporaryDirectory(prefix='step25-io-') as td:
 t=Path(td);inputs=t/'inputs';inputs.mkdir();out=t/'output';out.mkdir();plugin=inputs/'plugin 中文 with spaces';plugin.mkdir();sdk=inputs/'sdk';sdk.mkdir()
 install=inputs/'installation';install.mkdir();(install/'xtop.exe').write_bytes(b'NON_EXECUTABLE_IO_FIXTURE_NOT_RUN')
 (sdk/'otk.jar').write_bytes(b'io fixture, not a loadable SDK');(sdk/'pfcasync.jar').write_bytes(b'io fixture only')
 (plugin/'Main.class').write_bytes(b'io fixture class file, never loaded')
 (plugin/'model.prt').write_bytes(b'MODEL_CONTENT_MUST_NOT_APPEAR')
 (plugin/'plugin.jar').write_bytes(b'plugin bytes are hashed only')
 (inputs/'legacy-encoding.txt').write_bytes('jlink_java_command D:\\工具\\java.exe\n'.encode('gbk'))
 (inputs/'private.log').write_text('dbg_err_crash token=raw-secret private-model.prt at 0x12345678\n[0.1s][info][class,load] com.ptc.pfc.pfcSession.Session source: C:\\Users\\Alice\\sdk\\otk.jar\n{"event":"ROOT_ELEMENT_CREATE_START","faceXYZ":[11,22,33],"selectedFaceHandle":99999}\n')
 (inputs/'active.dat').write_text('name fixture\nstartup java\njava_app_classpath "'+str(plugin)+';'+str(sdk/'otk.jar')+';'+str(sdk/'pfcasync.jar')+'"\ntext_dir C:\\Users\\Alice\\messages\nend\n')
 (inputs/'commented.dat').write_text('name disabled\nstartup java\nend\n')
 (inputs/'config.pro').write_text('! private Alice alice@example.com\nprotkdat "'+str(inputs/'active.dat')+'"\n! protkdat "'+str(inputs/'commented.dat')+'"\npassword raw-config-secret\nfile_open_dir C:\\Users\\Alice\\private\nmodel C:\\private\\secret.prt.1\ncustom_person Alice-Private-Name\n')
 outside=t/'outside';outside.mkdir();(outside/'secret.class').write_bytes(b'OUTSIDE_LINK_MUST_NOT_BE_HASHED');link_created=False
 try:(plugin/'outside-link').symlink_to(outside,target_is_directory=True);link_created=True
 except OSError:pass
 before={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in inputs.rglob('*') if f.is_file()}
 x=run(['-File',str(p/'tests/Test-Collector.ps1'),'-FixtureRoot',str(inputs)]);(p/'evidence/pure-tests.stdout.log').write_text(x.stdout.rstrip()+"\n" if x.stdout else "");(p/'evidence/pure-tests.stderr.log').write_text(x.stderr);assert x.returncode==0,x.stdout+x.stderr
 pure_count=int(x.stdout.split('PURE_COLLECTOR_CHECKS_PASSED=')[-1])
 x=run(['-File',str(p/'scripts/Collect-P1Environment.ps1'),'-OutputRoot',str(out),'-ConfigProPaths',str(inputs/'config.pro'),'-InstallRoots',str(install),'-LogPaths',str(inputs/'private.log'),'-DiskOnly']);(p/'evidence/integration.stdout.log').write_text(x.stdout.rstrip()+"\n" if x.stdout else "");(p/'evidence/integration.stderr.log').write_text(x.stderr);assert x.returncode==0,x.stdout+x.stderr
 assert before=={str(f):hashlib.sha256(f.read_bytes()).hexdigest() for f in inputs.rglob('*') if f.is_file()},'input modified'
 zips=list(out.glob('*.zip'));assert len(zips)==1
 with ZipFile(zips[0]) as z:
  assert z.testzip() is None
  sums=next(n for n in z.namelist() if n.endswith('/SHA256SUMS'));prefix=sums.rsplit('/',1)[0]
  for line in z.read(sums).decode().splitlines():
   digest,leaf=line.split(None,1);assert hashlib.sha256(z.read(prefix+'/'+leaf)).hexdigest()==digest
  assert not any(n.lower().endswith(('.jar','.class','.prt','.asm','.dmp','.log','.jsonl','.exe','.dll')) for n in z.namelist())
  docs={Path(n).name:json.loads(z.read(n)) for n in z.namelist() if n.endswith('.json')}
  entire=b'\n'.join(z.read(n) for n in z.namelist()).decode()
  for forbidden in ['raw-secret','raw-config-secret','Alice','alice@example.com','MODEL_CONTENT_MUST_NOT_APPEAR','private-model.prt','selectedFaceHandle','faceXYZ','Alice-Private-Name']:
   assert forbidden not in entire,forbidden
  config=docs['configuration-index.json'];assert len(config['datReferences'])==2
  assert sorted(x['commentedInThisFile'] for x in config['datReferences'])==[False,True]
  assert len(config['snapshots'])==3
  identities=docs['sdk-identities.json'];assert len(identities['otk'])==1 and len(identities['pfcasync'])==1
  pluginfiles=docs['plugin-identities.json']['files'];assert len(pluginfiles)==2 and all(not x['loadedIdentityProven'] for x in pluginfiles)
  assert len(docs['installation-images.json']['images'])==1 and not docs['installation-images.json']['images'][0]['loadedIdentityProven']
  assert docs['processes-and-jvm.json']['status']=='DISK_ONLY_PROCESS_INSPECTION_SKIPPED'
  assert docs['collector-manifest.json']['modelApisCalled'] is False
 x=run(['-File',str(p/'scripts/Collect-P1Environment.ps1'),'-OutputRoot',str(plugin/'should-not-exist'),'-ConfigProPaths',str(inputs/'config.pro'),'-PluginRoots',str(plugin),'-DiskOnly']);assert x.returncode!=0 and not (plugin/'should-not-exist').exists(),'output overlap guard failed'
 limited_in=t/'limited-input';limited_in.mkdir();limited_out=t/'limited-output'
 dat_paths=[]
 for index in range(202):
  leaf=limited_in/f'registry-{index}.dat';leaf.write_text('name fixture\nstartup java\nend\n');dat_paths.append(leaf)
 (limited_in/'config.pro').write_text(''.join('protkdat "'+str(leaf)+'"\n' for leaf in dat_paths))
 x=run(['-File',str(p/'scripts/Collect-P1Environment.ps1'),'-OutputRoot',str(limited_out),'-ConfigProPaths',str(limited_in/'config.pro'),'-DiskOnly']);assert x.returncode==0,x.stdout+x.stderr
 with ZipFile(next(limited_out.glob('*.zip'))) as z:
  docs2={Path(n).name:json.loads(z.read(n)) for n in z.namelist() if n.endswith('.json')}
  assert len(docs2['configuration-index.json']['snapshots'])==201
  assert any(v['status']=='REFERENCE_LIMIT_REACHED' for v in docs2['scan-coverage.json']['issues'])
 (p/'evidence/verification.json').write_text(json.dumps({'powershellVersion':ps_version,'platform':platform.system()+' '+platform.machine()+'; fixture files only','pureChecks':pure_count,'integrationZipIntegrity':True,'integrationInternalChecksumsVerified':True,'allInputHashesUnchanged':True,'privateFixtureValuesAbsentFromArchive':True,'activeAndCommentedDatCollected':True,'sdkAndPluginHashMetadataNotBinaries':True,'noModelOrRawLogCopied':True,'diskOnlyDoesNotQueryProcesses':True,'windowsCimAndLoadedModulesExecuted':False,'windowsPowerShell51Executed':platform.system()=='Windows' and ps_version.startswith('5.1'),'actualCreoSiteExecuted':False,'outputInsideInputRootRejected':True,'symlinkTraversalSkipped':link_created,'unicodeAndSpacesPathsCovered':True,'datSnapshotLimitEnforcedAt200':True,'installationExecutableHashedWithoutExecutionOrCopy':True},indent=2)+'\n')
 print('fixture end-to-end passed')
