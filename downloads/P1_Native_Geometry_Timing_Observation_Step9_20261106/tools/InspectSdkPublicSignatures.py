"""Read class-file method tables only. Does not load SDK classes or invoke JNI."""
from zipfile import ZipFile
from pathlib import Path
import sys,json,hashlib,re
sdk=Path(sys.argv[1]);out=Path(sys.argv[2]);hits=[];classes=methods=0
pattern=re.compile(r'^(ComputeRayIntersections|Ray_Create|ProjectPoint|FindGeometry|FindShellsAndVoids|ComputeClearance|ComputeNearestCriticalDistance|ComputeInterference|CreateSelectionEvaluator|.*Classif.*Point.*|.*Point.*Classif.*|.*Point.*Contain.*|.*Contain.*Point.*|.*Point.*Inside.*|.*Inside.*Point.*|.*PointInSolid.*|.*Occlu.*|.*Obstruct.*)$',re.I)
with ZipFile(sdk) as z:
 for n in z.namelist():
  if not n.endswith('.class') or not n.startswith(('com/ptc/pfc/','com/ptc/wfc/')) or '/Implementation/' in n:continue
  d=z.read(n);p=10;i=1;count=int.from_bytes(d[8:10],'big');strings={}
  while i<count:
   tag=d[p];p+=1
   if tag==1:
    length=int.from_bytes(d[p:p+2],'big');p+=2;strings[i]=d[p:p+length].decode('utf-8',errors='replace');p+=length
   elif tag in [3,4,9,10,11,12,17,18]:p+=4
   elif tag in [5,6]:p+=8;i+=1
   elif tag in [7,8,16,19,20]:p+=2
   elif tag==15:p+=3
   else:raise ValueError(tag)
   i+=1
  flags=int.from_bytes(d[p:p+2],'big');p+=6
  if not flags&1:continue
  classes+=1
  interfaces=int.from_bytes(d[p:p+2],'big');p+=2+interfaces*2
  def skip_attributes(p):
   k=int.from_bytes(d[p:p+2],'big');p+=2
   for _ in range(k):length=int.from_bytes(d[p+2:p+6],'big');p+=6+length
   return p
  fields=int.from_bytes(d[p:p+2],'big');p+=2
  for _ in range(fields):p=skip_attributes(p+6)
  mcount=int.from_bytes(d[p:p+2],'big');p+=2
  for _ in range(mcount):
   access=int.from_bytes(d[p:p+2],'big');name=strings[int.from_bytes(d[p+2:p+4],'big')];desc=strings[int.from_bytes(d[p+4:p+6],'big')];p=skip_attributes(p+6)
   if access&1:
    methods+=1
    if pattern.match(name):hits.append({'owner':n[:-6].replace('/','.'),'method':name,'descriptor':desc})
summary={'type':'READ_ONLY_CLASSFILE_PUBLIC_DECLARED_METHOD_NAME_INDEX_NOT_SEMANTIC_OR_RUNTIME_PROOF','sdkSha256':hashlib.sha256(sdk.read_bytes()).hexdigest(),'scopePrefixes':['com.ptc.pfc','com.ptc.wfc'],'implementationPackagesExcluded':True,'publicFacingClassesInspected':classes,'publicDeclaredMethodsIndexed':methods,'candidateNameMatches':hits,'searchPattern':pattern.pattern,'noClaimOfSemanticExhaustiveness':True,'runtimeClassesLoaded':False,'nativeCallsExecuted':False}
out.write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps({'classes':classes,'publicMethods':methods,'nameMatches':len(hits)}))
