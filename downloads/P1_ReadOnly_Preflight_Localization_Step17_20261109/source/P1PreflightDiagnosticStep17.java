package p1.controlled.step17;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ptc.cipjava.jxobject_i;
import com.ptc.pfc.pfcSession.*;
import com.ptc.pfc.pfcCommand.*;
import com.ptc.pfc.pfcModel.*;
import com.ptc.pfc.pfcModelItem.*;
import com.ptc.pfc.pfcGeometry.*;
import com.ptc.pfc.pfcSolid.*;
import com.ptc.pfc.pfcSelect.*;
import com.ptc.pfc.pfcFeature.*;
import com.ptc.pfc.pfcUI.*;
import com.ptc.pfc.pfcUnits.*;
import com.ptc.pfc.pfcBase.UnitType;
import com.ptc.wfc.wfcSession.WSession;
import com.ptc.wfc.wfcSolid.WSolid;
import com.ptc.wfc.wfcFeature.*;
import static p1.controlled.step17.AuditSupport.*;

/** Local, unregistered-by-default candidate. Synchronous command, no Save, no retry or model restore. */
public final class P1PreflightDiagnosticStep17 {
 public static final String COMMAND_ID="SISWPreflightStep17";
 private static final String TARGET="200002100.prt",MESSAGES="p1_preflight_step17.txt";
 private static Session session;
 private static UICommand command;
 private static Listener listener;
 private static volatile boolean active;
 private static final AtomicBoolean busy=new AtomicBoolean();
 private static final AtomicBoolean clicked=new AtomicBoolean();
 private P1PreflightDiagnosticStep17(){}
 public static synchronized void start()throws Exception{
  if(active)return;
  if(command!=null||listener!=null)throw new IllegalStateException("Previous registration retained; do not re-register");
  session=pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
  if(session==null)throw new IllegalStateException("No synchronous session");listener=new Listener();
  try{command=session.UICreateCommand(COMMAND_ID,listener);if(command==null)throw new IllegalStateException("No command");
   command.Designate(MESSAGES,"P1_S17_LABEL","P1_S17_HELP","P1_S17_DESC");active=true;
  }catch(Exception|LinkageError error){if(command!=null){try{command.RemoveActionListener(listener);command=null;listener=null;}catch(Exception|LinkageError detach){error.addSuppressed(detach);}}else listener=null;session=null;throw error;}
 }
 public static synchronized void stop()throws Exception{
  active=false;if(busy.get())throw new IllegalStateException("Controlled operation running; do not unload");
  if(command!=null&&listener!=null)command.RemoveActionListener(listener);command=null;listener=null;session=null;
 }
 private static void display(String text,boolean error)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();
  options.SetMessageDialogType(error?MessageDialogType.MESSAGE_ERROR:MessageDialogType.MESSAGE_INFO);
  options.SetDialogLabel("P1 Step17 — 不保存试验状态");MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_OK);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_OK);
  session.UIShowMessageDialog(text,options);
 }
 private static void feedback(String text,boolean error){feedback(text,error,"P1_S17_FAILED");}
 private static void feedback(String text,boolean error,String statusKey){
  System.err.println("[P1_STEP17] "+text);
  try{display(text,error);}catch(Exception|LinkageError dialogError){System.err.println("[P1_STEP17] RESULT_DIALOG_FAILED "+dialogError.getClass().getName());
   try{session.UIDisplayMessage(MESSAGES,statusKey,null);}catch(Exception|LinkageError statusError){System.err.println("[P1_STEP17] BOTH_NATIVE_FEEDBACK_ROUTES_FAILED "+statusError.getClass().getName());}}
 }
 private static boolean confirm(String text)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();options.SetMessageDialogType(MessageDialogType.MESSAGE_WARNING);options.SetDialogLabel("P1 Step17 只读Preflight确认");
  MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_YES);buttons.append(MessageButton.MESSAGE_BUTTON_NO);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_NO);
  return session.UIShowMessageDialog(text,options)==MessageButton.MESSAGE_BUTTON_YES;
 }
 private static final class Listener extends DefaultUICommandActionListener {
  @Override public void OnCommand(){
   if(!active||session==null){feedback("命令未激活，未执行转换。",true);return;}
   if(!busy.compareAndSet(false,true)){feedback("已有受控操作进行中，拒绝重入；未新增转换。",true);return;}
   if(!clicked.compareAndSet(false,true)){busy.set(false);feedback("本进程第十七步点击次数已用尽；未进入转换，不重试。",true);return;}
   String message="本次只读Preflight未完成；一次点击已消耗。";boolean error=true;String statusKey="P1_S17_FAILED";
   try{
    Path directory=Paths.get(System.getProperty("user.home"),"CreoPreflightDiagnosticsStep17");
    try(Step17Control control=new Step17Control(directory)){
     try{message=execute(control);error=false;statusKey="PREFLIGHT_AND_INPUT_RECORDED_NO_CONVERSION_NO_SAVE".equals(control.outcome())?"P1_S17_INPUT_DONE":"P1_S17_CANCELLED";}
     catch(Exception|LinkageError failure){
      String outcome="READ_ONLY_PREFLIGHT_BLOCKED_NO_WRITE";
      String phase=control.state().toString();
      try{control.record("ERROR",map("exceptionClass",failure.getClass().getName(),"message",String.valueOf(failure.getMessage()),"phase",phase,"errorCode",failure instanceof PreflightAudit.ProbeFailure?"PREFLIGHT_READ_EXCEPTION":failure instanceof FaceIdentityFailure faceFailure?faceFailure.code.name():failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.code.name():"OTHER_FAILURE","bodyCheckStage",failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.stage:"NOT_BODY_CHECK","faceCheckStage",failure instanceof FaceIdentityFailure faceFailure?faceFailure.stage:"NOT_FACE_CHECK"));}catch(Exception logError){failure.addSuppressed(logError);}
      failure.printStackTrace(System.err);
      try{control.finish(outcome);}catch(Exception logError){System.err.println("[P1_STEP17] FINAL_AUDIT_WRITE_FAILED "+logError.getClass().getName());}
      message="只读Preflight未完成或被阻断；本命令不调用模型创建/再生/保存。本步一次点击已消耗，保留日志，不重试。\n原因："+failure.getMessage();
     }
    }
   }catch(FileAlreadyExistsException already){message="本机第十七步一次点击锁已存在；本次未进入转换。不要删除记录、换目录或重载绕过单次限制。";}
   catch(Exception|LinkageError outer){error=true;statusKey="P1_S17_FAILED";outer.printStackTrace(System.err);message="只读诊断记录创建或关闭失败；不要重复点击，保留现场和stderr。本命令没有模型写入调用。\n"+outer.getClass().getSimpleName();}
   finally{busy.set(false);}
   feedback(message,error,statusKey);
  }
 }
 private static long ptr(Object value){if(!(value instanceof jxobject_i nativeObject))throw new IllegalStateException("Native identity unavailable");long p=nativeObject.getptr();if(p==0)throw new IllegalStateException("Zero native identity");return p;}
 private static boolean same(Object a,Object b){return a!=null&&b!=null&&(a==b||ptr(a)==ptr(b));}
 private static <T> T adapt(Object source,Class<T> type,String[] names,Step17Control control)throws Exception{
  if(type.isInstance(source))return type.cast(source);
  Exception chain=new Exception("WFC adapter unavailable: "+type.getName());long pointer=ptr(source);
  for(String name:names){try{Class<?> implementation=Class.forName(name);var ctor=implementation.getDeclaredConstructor(long.class);ctor.setAccessible(true);Object wrapper=ctor.newInstance(pointer);
    if(!type.isInstance(wrapper)||ptr(wrapper)!=pointer)throw new IllegalStateException("Adapter identity mismatch");control.record("WFC_INTERNAL_ADAPTER_ACQUIRED",map("publicView",type.getName(),"adapterClass",name,"internalApiRuntimeContractCertified",false));return type.cast(wrapper);
   }catch(ReflectiveOperationException|RuntimeException error){chain.addSuppressed(error);}}
  throw chain;
 }
 private static final class Snapshot {
  Model model;WSolid solid;SolidBody body;Surface face;WSession wsession;PreflightAudit trace;Selection capturedSelection;Integer capturedSelectionCount,stableFaceId;Integer stableBodyId;String stamp;boolean dirty;Set<Integer> featureIds;
 }
 private static Snapshot inspect(Step17Control control)throws Exception{
  Snapshot s=new Snapshot();
  try{s.model=session.GetActiveModel();if(s.model==null||s.model.GetType()!=ModelType.MDL_PART||!TARGET.equalsIgnoreCase(s.model.GetFileName()))throw new BodyIdentityFailure(ScopedBodyIdentity.Code.MODEL_SCOPE_MISMATCH,"INITIAL","只允许当前普通零件200002100.prt，禁止装配/其他模型。",null);}
  catch(BodyIdentityFailure mismatch){throw mismatch;}
  catch(Exception|LinkageError readError){throw new BodyIdentityFailure(ScopedBodyIdentity.Code.STATE_READ_EXCEPTION,"INITIAL","活跃模型/类型/名称读取异常",readError);}
  s.solid=adapt(s.model,WSolid.class,new String[]{"com.ptc.wfc.Implementation.wfcPart$WPart","com.ptc.wfc.Implementation.wfcSolid$WSolid"},control);
  s.wsession=adapt(session,WSession.class,new String[]{"com.ptc.wfc.Implementation.wfcSession$WSession"},control);
  if(!same(s.model,s.solid))throw new BodyIdentityFailure(ScopedBodyIdentity.Code.MODEL_SCOPE_MISMATCH,"INITIAL","WFC model identity mismatch",null);
  s.body=verifyDefaultBody(s,control,false);
  UnitSystem units=s.solid.GetPrincipalUnits();Unit length=units==null?null:units.GetUnit(UnitType.UNIT_LENGTH);
  if(length==null||!"mm".equalsIgnoreCase(length.GetName()))throw new IllegalStateException("原生长度单位未明确为mm，不猜单位。");
  requireFace(s,control,false);
  PreflightAudit trace=new PreflightAudit(control);s.trace=trace;
  s.stamp=trace.read("VERSION_STAMP_READ_START","VERSION_STAMP_READ","Model.GetVersionStamp",()->s.model.GetVersionStamp(),value->map("returnedNull",value==null,"value",value));
  s.dirty=trace.read("MODIFIED_FLAG_READ_START","MODIFIED_FLAG_READ","Model.GetIsModified",()->s.model.GetIsModified(),value->map("value",value));
  if(s.stamp==null||s.stamp.isBlank())throw new IllegalStateException("模型版本上下文不可读，未写模型。");
  s.featureIds=featureIds(s.model,trace);requireNoNativeFailure(s.solid,trace);
  Integer bodyId=trace.read("PREFLIGHT_BODY_ID_READ_START","PREFLIGHT_BODY_ID_READ_DONE","SolidBody.GetId",()->s.body.GetId(),value->map("value",value));
  Integer faceId=trace.read("PREFLIGHT_FACE_ID_READ_START","PREFLIGHT_FACE_ID_READ_DONE","Surface.GetId",()->s.face.GetId(),value->map("value",value));
  control.record("PREFLIGHT",map("phase","DONE","model",TARGET,"nativeIsSheetmetal",false,"bodyId",bodyId,"drivingFaceId",faceId,"drivingFaceRequired",true,"lengthUnit","mm","globalThicknessProven",false,"modelVersionStamp",s.stamp,"dirtyBefore",s.dirty,"featureCountBefore",s.featureIds.size()));return s;
 }
 private static final class BodyIdentityFailure extends IllegalStateException {
  private static final long serialVersionUID=1L;
  final ScopedBodyIdentity.Code code;final String stage;
  BodyIdentityFailure(ScopedBodyIdentity.Code code,String stage,String detail,Throwable cause){super(code.name()+": "+stage+": "+detail,cause);this.code=code;this.stage=stage;}
 }
 private static Map<String,Object> bodyHandle(Object value){
  if(value==null)return map("status","NO_OBJECT","value",null,"diagnosticOnly",true);
  if(!(value instanceof jxobject_i nativeView))return map("status","NO_WRAPPER_HANDLE_INTERFACE","value",null,"diagnosticOnly",true);
  try{return map("status","RETURNED","value","0x"+Long.toUnsignedString(nativeView.getptr(),16),"diagnosticOnly",true);}
  catch(RuntimeException|LinkageError error){return map("status","THREW","value",null,"exceptionClass",error.getClass().getName(),"diagnosticOnly",true);}
 }
 private static SolidBody verifyDefaultBody(Snapshot s,Step17Control control,boolean recheck)throws Exception{
  String stage=recheck?"RECHECK":"INITIAL";boolean scope=false;
  try{scope=active&&session!=null&&s.model!=null&&s.solid!=null&&same(session.GetActiveModel(),s.model)&&same(s.solid,s.model)&&s.model.GetType()==ModelType.MDL_PART&&TARGET.equalsIgnoreCase(s.model.GetFileName());}
  catch(Exception|LinkageError error){control.record("BODY_IDENTITY_ERROR",map("stage",stage,"errorCode","STATE_READ_EXCEPTION","readOperation","MODEL_SCOPE_READ","exceptionClass",error.getClass().getName()));throw new BodyIdentityFailure(ScopedBodyIdentity.Code.STATE_READ_EXCEPTION,stage,"模型作用域读取异常",error);}
  if(!scope){control.record("BODY_IDENTITY_ERROR",map("stage",stage,"errorCode","MODEL_SCOPE_MISMATCH","scopeVerified",false));throw new BodyIdentityFailure(ScopedBodyIdentity.Code.MODEL_SCOPE_MISMATCH,stage,"模型/零件类型/视图作用域不一致",null);}
  ModelItem listed=null;SolidBody current=null;Integer count=null,listedId=null,currentId=null;Boolean sheetmetal=null;boolean readFailed=false,listedTypeOk=true,defaultTypeOk=true;Throwable firstError=null;
  List<Map<String,Object>> readErrors=new ArrayList<>();
  try{ModelItems bodies=s.model.ListItems(ModelItemType.ITEM_BODY);if(bodies!=null){count=bodies.getarraysize();if(count==1)listed=bodies.get(0);}}
  catch(Exception|LinkageError error){readFailed=true;firstError=error;readErrors.add(map("operation","Model.ListItems/first","exceptionClass",error.getClass().getName(),"message",String.valueOf(error.getMessage())));}
  try{current=s.solid.GetDefaultBody();}catch(Exception|LinkageError error){readFailed=true;if(firstError==null)firstError=error;readErrors.add(map("operation","Solid.GetDefaultBody","exceptionClass",error.getClass().getName(),"message",String.valueOf(error.getMessage())));}
  if(listed!=null){try{listedId=listed.GetId();listedTypeOk=listed.GetType()==ModelItemType.ITEM_BODY;}catch(Exception|LinkageError error){readFailed=true;if(firstError==null)firstError=error;readErrors.add(map("operation","EnumeratedBody.GetId/GetType","exceptionClass",error.getClass().getName(),"message",String.valueOf(error.getMessage())));}}
  if(current!=null){try{currentId=current.GetId();defaultTypeOk=current.GetType()==ModelItemType.ITEM_BODY;}catch(Exception|LinkageError error){readFailed=true;if(firstError==null)firstError=error;readErrors.add(map("operation","DefaultBody.GetId/GetType","exceptionClass",error.getClass().getName(),"message",String.valueOf(error.getMessage())));}
   try{sheetmetal=current.IsSheetmetal();}catch(Exception|LinkageError error){readFailed=true;if(firstError==null)firstError=error;readErrors.add(map("operation","DefaultBody.IsSheetmetal","exceptionClass",error.getClass().getName(),"message",String.valueOf(error.getMessage())));}}
  boolean typedScope=scope&&listedTypeOk&&defaultTypeOk;
  ScopedBodyIdentity.Code code=ScopedBodyIdentity.evaluate(typedScope,count,listed!=null,current!=null,listedId,currentId,recheck,s.stableBodyId,sheetmetal,readFailed);
  if(recheck&&s.stableBodyId==null)code=ScopedBodyIdentity.Code.STATE_READ_EXCEPTION;
  Map<String,Object> listedHandle=bodyHandle(listed),currentHandle=bodyHandle(current),previousHandle=bodyHandle(s.body);
  Object handlesEqual="RETURNED".equals(listedHandle.get("status"))&&"RETURNED".equals(currentHandle.get("status"))?Objects.equals(listedHandle.get("value"),currentHandle.get("value")):null;
  control.record("BODY_IDENTITY_CHECK",map("stage",stage,"scopeVerified",typedScope,"model",TARGET,"nativePartTypeConfirmed",true,"bodyCount",count,"enumeratedBodyPresent",listed!=null,"defaultBodyPresent",current!=null,"enumeratedBodyId",listedId,"defaultBodyId",currentId,"baselineBodyId",s.stableBodyId,
   "enumeratedBodyTypeConfirmed",listed==null?null:listedTypeOk,"defaultBodyTypeConfirmed",current==null?null:defaultTypeOk,"nativeDefaultIsSheetmetal",sheetmetal,"readErrors",readErrors,
   "javaReferenceEqual",listed!=null&&current!=null?listed==current:null,"enumeratedHandle",listedHandle,"defaultHandle",currentHandle,"previousDefaultHandle",previousHandle,"enumeratedDefaultHandlesEqual",handlesEqual,
   "idEquality",listedId!=null&&currentId!=null?listedId.equals(currentId):null,"bodyHandleEqualityUsedAsHardGate",false,"bodyIdPositiveOnlyRequired",false,"resultCode",code.name()));
  if(code!=ScopedBodyIdentity.Code.ACCEPTED)throw new BodyIdentityFailure(code,stage,"默认体身份/状态检查未通过，详见BODY_IDENTITY_CHECK",firstError);
  if(!recheck)s.stableBodyId=currentId;
  return current;
 }
 private static final class FaceIdentityFailure extends IllegalStateException {
  private static final long serialVersionUID=1L;
  final ScopedFaceIdentity.Code code;final String stage;
  FaceIdentityFailure(ScopedFaceIdentity.Code code,String stage,String detail,Throwable cause){super(code.name()+": "+stage+": "+detail,cause);this.code=code;this.stage=stage;}
 }
 private static FaceIdentityFailure faceReadFailure(Step17Control control,String stage,String operation,Throwable error)throws Exception{
  control.record("FACE_SCOPE_ERROR",map("stage",stage,"errorCode","FACE_SCOPE_READ_EXCEPTION","operation",operation,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));
  return new FaceIdentityFailure(ScopedFaceIdentity.Code.FACE_SCOPE_READ_EXCEPTION,stage,operation,error);
 }
 private static Surface selectedFace(Snapshot s,Step17Control control,boolean recheck)throws Exception{
  String stage=recheck?"RECHECK":"INITIAL";Selection selection=s.capturedSelection;Integer count=s.capturedSelectionCount;
  try{
   if(!recheck){SelectionBuffer buffer=session.GetCurrentSelectionBuffer();Selections selected=buffer==null?null:buffer.GetContents();count=selected==null?0:selected.getarraysize();selection=count==1?selected.get(0):null;}
   Surface face=null;boolean pathPresent=false,selectedModelMatches=false;
   if(selection!=null){pathPresent=selection.GetPath()!=null;selectedModelMatches=same(selection.GetSelModel(),s.model);ModelItem item=selection.GetSelItem();if(item instanceof Surface surface)face=surface;}
   ScopedFaceIdentity.Code code=ScopedFaceIdentity.selection(count,selection!=null,pathPresent,face!=null,selectedModelMatches,false);
   control.record("FACE_SELECTION_CHECK",map("stage",stage,"selectionSource",recheck?"ORIGINAL_CAPTURED_SELECTION_NOT_NEW_BUFFER":"CURRENT_PRESELECTION_BUFFER","selectionCount",count,"selectionPresent",selection!=null,"pathPresent",pathPresent,"itemIsSurface",face!=null,"selectionModelMatches",selectedModelMatches,"resultCode",code.name()));
   if(code!=ScopedFaceIdentity.Code.ACCEPTED)throw new FaceIdentityFailure(code,stage,"只接受当前零件无装配路径的单个曲面",null);
   if(!recheck){s.capturedSelection=selection;s.capturedSelectionCount=count;}
   return face;
  }catch(FaceIdentityFailure failure){throw failure;}
  catch(Exception|LinkageError error){throw faceReadFailure(control,stage,"SELECTION_READ",error);}
 }
 private static void requireFace(Snapshot s,Step17Control control,boolean recheck)throws Exception{
  String stage=recheck?"RECHECK":"INITIAL";Surface selected=selectedFace(s,control,recheck);Integer selectedId=null,bodyId=null,defaultId=null,enumeratedBodyId=null,bodyCount=null;boolean scope=false;
  try{
   boolean modelScope=active&&s.model!=null&&s.solid!=null&&same(session.GetActiveModel(),s.model)&&same(s.solid,s.model)&&s.model.GetType()==ModelType.MDL_PART&&TARGET.equalsIgnoreCase(s.model.GetFileName());
   if(s.body!=null)bodyId=s.body.GetId();SolidBody current=s.solid.GetDefaultBody();if(current!=null)defaultId=current.GetId();
   ModelItems bodies=s.model.ListItems(ModelItemType.ITEM_BODY);if(bodies!=null){bodyCount=bodies.getarraysize();if(bodyCount==1){ModelItem body=bodies.get(0);if(body!=null)enumeratedBodyId=body.GetId();}}
   scope=modelScope&&s.stableBodyId!=null&&s.stableBodyId.equals(bodyId)&&s.stableBodyId.equals(defaultId)&&bodyCount!=null&&bodyCount==1&&s.stableBodyId.equals(enumeratedBodyId);
   selectedId=selected.GetId();
  }catch(Exception|LinkageError error){throw faceReadFailure(control,stage,"MODEL_AND_CURRENT_BODY_SCOPE_READ",error);}
  if(!scope){control.record("FACE_IDENTITY_CHECK",map("stage",stage,"scopeVerified",false,"model",TARGET,"stableBodyId",s.stableBodyId,"snapshotBodyId",bodyId,"currentDefaultBodyId",defaultId,"enumeratedBodyId",enumeratedBodyId,"bodyCount",bodyCount,"selectedFaceId",selectedId,"resultCode","FACE_BODY_SCOPE_MISMATCH"));throw new FaceIdentityFailure(ScopedFaceIdentity.Code.FACE_BODY_SCOPE_MISMATCH,stage,"面ID只能在当前目标模型和已核验当前体作用域内匹配",null);}
  if(recheck&&s.stableFaceId==null)throw faceReadFailure(control,stage,"BASELINE_FACE_ID",new IllegalStateException("No initial confirmed face ID"));
  if(recheck&&!Objects.equals(selectedId,s.stableFaceId)){control.record("FACE_IDENTITY_CHECK",map("stage",stage,"selectedFaceId",selectedId,"baselineFaceId",s.stableFaceId,"resultCode","FACE_ID_CHANGED"));throw new FaceIdentityFailure(ScopedFaceIdentity.Code.FACE_ID_CHANGED,stage,"原选中面ID在确认期间变化，不替换已确认驱动面",null);}
  SurfaceType type;boolean visible;double area;
  try{type=selected.GetSurfaceType();visible=selected.GetIsVisible();area=selected.EvalArea();}
  catch(Exception|LinkageError error){throw faceReadFailure(control,stage,"SELECTED_FACE_TYPE_VISIBILITY_AREA",error);}
  // Preserve the original geometric gates and their OTHER_FAILURE classification.
  if(type!=SurfaceType.SURFACE_PLANE||!visible)throw new IllegalStateException("驱动面必须是当前实体体的可见平面。");
  if(!Double.isFinite(area)||area<=0.0)throw new IllegalStateException("驱动面面积无效。");
  Surface matched=null;int matches=0,total;
  try{
   Surfaces faces=s.body.ListSurfaces();if(faces==null)throw new IllegalStateException("Body surface list returned null");total=faces.getarraysize();if(total<0||total>10000)throw new IllegalStateException("Body surface list outside existing bound");
   for(int i=0;i<total;i++){Surface candidate=faces.get(i);if(candidate!=null&&candidate.GetId()==selectedId){matches++;if(matched==null)matched=candidate;}}
  }catch(Exception|LinkageError error){throw faceReadFailure(control,stage,"CURRENT_BODY_LISTSURFACES_IDS",error);}
  ScopedFaceIdentity.Code code=ScopedFaceIdentity.face(scope,selectedId,recheck,s.stableFaceId,matches,false);
  Map<String,Object> selectedHandle=bodyHandle(selected),listedHandle=bodyHandle(matched);
  Object handlesEqual="RETURNED".equals(selectedHandle.get("status"))&&"RETURNED".equals(listedHandle.get("status"))?Objects.equals(selectedHandle.get("value"),listedHandle.get("value")):null;
  control.record("FACE_IDENTITY_CHECK",map("stage",stage,"model",TARGET,"scopeVerified",scope,"stableBodyId",s.stableBodyId,"bodyId",bodyId,"currentDefaultBodyId",defaultId,"enumeratedBodyId",enumeratedBodyId,"bodyCount",bodyCount,"selectedFaceId",selectedId,"baselineFaceId",s.stableFaceId,"bodySurfaceCount",total,"matchingIdsInCurrentBody",matches,"selectedFaceType",type.getValue(),"selectedFaceVisible",visible,"selectedFaceArea",area,
   "javaReferenceEqual",matched==null?null:Boolean.valueOf(matched==selected),"selectedFaceHandle",selectedHandle,"listedFaceHandle",listedHandle,"wrapperHandlesEqual",handlesEqual,"faceHandleEqualityUsedAsHardGate",false,"source","CURRENT_VERIFIED_BODY_LISTSURFACES_ONLY","resultCode",code.name()));
  if(code!=ScopedFaceIdentity.Code.ACCEPTED)throw new FaceIdentityFailure(code,stage,"当前体原生曲面列表未找到唯一相同面ID，不跨体按ID匹配",null);
  if(!recheck)s.stableFaceId=selectedId;s.face=selected;
 }
 private static Set<Integer> featureIds(Model model,PreflightAudit trace)throws Exception{
  ModelItems items=trace.read("FEATURE_LIST_START","FEATURE_LIST_COLLECTION_DONE","Model.ListItems(ITEM_FEATURE)",()->model.ListItems(ModelItemType.ITEM_FEATURE),value->map("returnedNull",value==null));
  if(items==null)throw new IllegalStateException("Feature list unavailable");
  int count=trace.read("FEATURE_LIST_COUNT_START","FEATURE_LIST_COUNT_DONE","ModelItems.getarraysize",items::getarraysize,value->map("count",value));
  if(count<0||count>10000)throw new IllegalStateException("Feature list outside existing bound");
  List<Feature> objects=trace.read("FEATURE_LIST_ITEMS_START","FEATURE_LIST_ITEMS_DONE","ModelItems.get[*]",()->{
   List<Feature> result=new ArrayList<>();for(int i=0;i<count;i++){ModelItem item=items.get(i);if(!(item instanceof Feature feature))throw new IllegalStateException("Invalid feature interface at index "+i);result.add(feature);}return result;
  },value->map("processed",value.size(),"groupedRead",true));
  Set<Integer> ids=trace.read("FEATURE_LIST_IDS_START","FEATURE_LIST_DONE","ModelItem.GetId[*]",()->{
   Set<Integer> values=new LinkedHashSet<>();for(int i=0;i<objects.size();i++)if(!values.add(objects.get(i).GetId()))throw new IllegalStateException("Duplicate feature ID at index "+i);return values;
  },value->map("uniqueIds",value.size(),"processed",objects.size(),"groupedRead",true));
  trace.note("FEATURE_LIST_ITEM_DONE",map("samplePolicy","FINAL_COUNTS_ONLY","itemsFetched",objects.size(),"idsRead",ids.size()));return ids;
 }
 private static boolean flagValue(FeatureStatusFlag flag,String method)throws Exception{
  return switch(method){
   case "GetIsFailed"->flag.GetIsFailed();case "GetIsInvalid"->flag.GetIsInvalid();case "GetIsUnregenerated"->flag.GetIsUnregenerated();
   case "GetIsChildOfFailed"->flag.GetIsChildOfFailed();case "GetIsChildOfExternalFailed"->flag.GetIsChildOfExternalFailed();
   default->throw new IllegalArgumentException("Unknown fixed flag method");};
 }
 private static void requireNoNativeFailure(WSolid solid,PreflightAudit trace)throws Exception{
  FeatureStatusFlags flags=trace.read("NATIVE_FAILURE_FLAGS_START","NATIVE_FAILURE_FLAGS_COLLECTION_DONE","WSolid.GetSolidFeatureStatusFlags",solid::GetSolidFeatureStatusFlags,value->map("returnedNull",value==null));
  if(flags==null)throw new IllegalStateException("Feature error flags unknown");
  int count=trace.read("NATIVE_FAILURE_FLAGS_COUNT_START","NATIVE_FAILURE_FLAGS_COUNT_DONE","FeatureStatusFlags.getarraysize",flags::getarraysize,value->map("count",value));
  if(count<0||count>10000)throw new IllegalStateException("Feature status flags outside existing bound");
  List<FeatureStatusFlag> objects=trace.read("NATIVE_FAILURE_FLAGS_ITEMS_START","NATIVE_FAILURE_FLAGS_ITEMS_DONE","FeatureStatusFlags.get[*]",()->{
   List<FeatureStatusFlag> values=new ArrayList<>();for(int i=0;i<count;i++){FeatureStatusFlag flag=flags.get(i);if(flag==null)throw new IllegalStateException("Null feature status flag at index "+i);values.add(flag);}return values;
  },value->map("processed",value.size(),"groupedRead",true));
  for(String method:new String[]{"GetIsFailed","GetIsInvalid","GetIsUnregenerated","GetIsChildOfFailed","GetIsChildOfExternalFailed"}){
   boolean hit=trace.read("NATIVE_FAILURE_FLAG_SCAN_START","NATIVE_FAILURE_FLAG_SCAN_DONE","FeatureStatusFlag."+method+"[*]",()->{
    for(FeatureStatusFlag flag:objects)if(flagValue(flag,method))return true;return false;
   },value->map("flagMethod",method,"anyTrue",value,"batchSize",objects.size(),"groupedRead",true));
   if(hit){trace.note("NATIVE_FAILURE_FLAGS_REJECTED",map("flagMethod",method,"reason","NATIVE_FLAG_TRUE"));throw new IllegalStateException("Native feature failed/invalid/unregenerated flag present: "+method);}
  }
  trace.note("NATIVE_FAILURE_FLAGS_DONE",map("flags",count,"fieldsChecked",5,"anyRejectedFlag",false));
 }
 private static String execute(Step17Control control)throws Exception{
  Snapshot s=inspect(control);
  boolean confirmed=s.trace.read("FIRST_CONFIRMATION_START","FIRST_CONFIRMATION_DONE","Session.UIShowMessageDialog_WITH_EXPLICIT_OPTIONS",()->confirm("只读Preflight已通过。当前200002100.prt，原生默认体非钣金；全局壁厚仍未证明。\n本Step17仅观察确认和厚度输入，不创建特征、不再生、不保存、不提交转换。\n确认当前为可丢弃试验模型，继续输入本次试验厚度并结束诊断吗？"),value->map("accepted",value));
  if(!confirmed){control.finish("CANCELLED_FIRST_CONFIRMATION_READONLY");return "已取消只读确认；本次点击已消耗，没有模型写入调用。";}
  control.acceptRisk();s.trace.read("THICKNESS_PROMPT_START","THICKNESS_PROMPT_DONE","Session.UIDisplayMessage",()->{session.UIDisplayMessage(MESSAGES,"P1_S17_THICKNESS",null);return Boolean.TRUE;},value->map("returned",value));
  String input=s.trace.read("THICKNESS_INPUT_START","THICKNESS_INPUT_DONE","Session.UIReadStringMessage",()->session.UIReadStringMessage(Boolean.FALSE),value->map("returnedNull",value==null,"textLength",value==null?null:value.length()));
  if(input==null||input.isBlank()){control.finish("CANCELLED_THICKNESS_INPUT_READONLY");return "未输入试验厚度，结束只读诊断；本次点击已消耗。";}
  control.setThickness(input);control.finish("PREFLIGHT_AND_INPUT_RECORDED_NO_CONVERSION_NO_SAVE");
  return "预检及输入阶段已完成。试验厚度="+control.thickness()+" mm，仅记录，不构造特征/不再生/不保存。本命令到此结束，不再点击。";
 }
}
