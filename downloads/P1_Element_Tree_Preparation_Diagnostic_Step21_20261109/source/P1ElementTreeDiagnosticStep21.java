package p1.controlled.step21;
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
import com.ptc.pfc.pfcArgument.*;
import com.ptc.wfc.wfcElementTree.*;
import com.ptc.wfc.wfcElemIds.wfcElemIds;
import static p1.controlled.step21.AuditSupport.*;

/** Local, unregistered-by-default candidate. Synchronous command, no Save, no retry or model restore. */
public final class P1ElementTreeDiagnosticStep21 {
 public static final String COMMAND_ID="SISWElementTreeStep21";
 private static final String TARGET="200002100.prt",MESSAGES="p1_element_tree_step21.txt";
 private static Session session;
 private static UICommand command;
 private static Listener listener;
 private static volatile boolean active;
 private static final AtomicBoolean busy=new AtomicBoolean();
 private static final AtomicBoolean clicked=new AtomicBoolean();
 private P1ElementTreeDiagnosticStep21(){}
 public static synchronized void start()throws Exception{
  if(active)return;
  if(command!=null||listener!=null)throw new IllegalStateException("Previous registration retained; do not re-register");
  session=pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
  if(session==null)throw new IllegalStateException("No synchronous session");listener=new Listener();
  try{command=session.UICreateCommand(COMMAND_ID,listener);if(command==null)throw new IllegalStateException("No command");
   command.Designate(MESSAGES,"P1_S21_LABEL","P1_S21_HELP","P1_S21_DESC");active=true;
  }catch(Exception|LinkageError error){if(command!=null){try{command.RemoveActionListener(listener);command=null;listener=null;}catch(Exception|LinkageError detach){error.addSuppressed(detach);}}else listener=null;session=null;throw error;}
 }
 public static synchronized void stop()throws Exception{
  active=false;if(busy.get())throw new IllegalStateException("Controlled operation running; do not unload");
  if(command!=null&&listener!=null)command.RemoveActionListener(listener);command=null;listener=null;session=null;
 }
 private static void display(String text,boolean error)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();
  options.SetMessageDialogType(error?MessageDialogType.MESSAGE_ERROR:MessageDialogType.MESSAGE_INFO);
  options.SetDialogLabel("P1 Step21 — 不保存试验状态");MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_OK);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_OK);
  session.UIShowMessageDialog(text,options);
 }
 private static void feedback(String text,boolean error){feedback(text,error,"P1_S21_FAILED");}
 private static void feedback(String text,boolean error,String statusKey){
  System.err.println("[P1_STEP21] "+text);
  try{display(text,error);}catch(Exception|LinkageError dialogError){System.err.println("[P1_STEP21] RESULT_DIALOG_FAILED "+dialogError.getClass().getName());
   try{session.UIDisplayMessage(MESSAGES,statusKey,null);}catch(Exception|LinkageError statusError){System.err.println("[P1_STEP21] BOTH_NATIVE_FEEDBACK_ROUTES_FAILED "+statusError.getClass().getName());}}
 }
 private static boolean confirm(String text)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();options.SetMessageDialogType(MessageDialogType.MESSAGE_WARNING);options.SetDialogLabel("P1 Step21 只读元素树定位确认");
  MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_YES);buttons.append(MessageButton.MESSAGE_BUTTON_NO);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_NO);
  return session.UIShowMessageDialog(text,options)==MessageButton.MESSAGE_BUTTON_YES;
 }
 private static final class Listener extends DefaultUICommandActionListener {
  @Override public void OnCommand(){
   if(!active||session==null){feedback("命令未激活，未执行转换。",true);return;}
   if(!busy.compareAndSet(false,true)){feedback("已有受控操作进行中，拒绝重入；未新增转换。",true);return;}
   if(!clicked.compareAndSet(false,true)){busy.set(false);feedback("本进程第二十一步点击次数已用尽；未进入转换，不重试。",true);return;}
   String message="本次元素树定位未完成；一次点击已消耗。";boolean error=true;String statusKey="P1_S21_FAILED";
   try{
    Path directory=Paths.get(System.getProperty("user.home"),"CreoElementTreeDiagnosticsStep21");
    try(Step21Control control=new Step21Control(directory)){
     try{message=execute(control);error=false;statusKey="ELEMENT_TREE_PREPARED_NO_MODEL_WRITE_NO_SAVE".equals(control.outcome())?"P1_S21_PREPARED":"P1_S21_CANCELLED";}
     catch(Exception|LinkageError failure){
      String outcome="ELEMENT_TREE_DIAGNOSTIC_BLOCKED_NO_MODEL_WRITE";
      String phase=control.state().toString();
      try{control.record("ERROR",map("exceptionClass",failure.getClass().getName(),"message",String.valueOf(failure.getMessage()),"phase",phase,"errorCode",failure instanceof PreflightAudit.ProbeFailure?"NATIVE_OPERATION_EXCEPTION":failure instanceof FaceIdentityFailure faceFailure?faceFailure.code.name():failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.code.name():"OTHER_FAILURE","bodyCheckStage",failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.stage:"NOT_BODY_CHECK","faceCheckStage",failure instanceof FaceIdentityFailure faceFailure?faceFailure.stage:"NOT_FACE_CHECK"));}catch(Exception logError){failure.addSuppressed(logError);}
      failure.printStackTrace(System.err);
      try{control.finish(outcome);}catch(Exception logError){System.err.println("[P1_STEP21] FINAL_AUDIT_WRITE_FAILED "+logError.getClass().getName());}
      message="元素树定位未完成或被阻断；本命令没有模型创建/再生/保存调用。本步点击已消耗，不重试，保留日志。"+"\n原因："+failure.getMessage();
     }
    }
   }catch(FileAlreadyExistsException already){message="本机第二十一步一次点击锁已存在；本次未进入转换。不要删除记录、换目录或重载绕过单次限制。";}
   catch(Exception|LinkageError outer){error=true;statusKey="P1_S21_FAILED";outer.printStackTrace(System.err);message="记录创建或关闭失败；本命令没有模型创建/再生/保存调用。不要重复点击，保留现场和stderr。\n"+outer.getClass().getSimpleName();}
   finally{busy.set(false);}
   feedback(message,error,statusKey);
  }
 }
 private static long ptr(Object value){if(!(value instanceof jxobject_i nativeObject))throw new IllegalStateException("Native identity unavailable");long p=nativeObject.getptr();if(p==0)throw new IllegalStateException("Zero native identity");return p;}
 private static boolean same(Object a,Object b){return a!=null&&b!=null&&(a==b||ptr(a)==ptr(b));}
 private static <T> T adapt(Object source,Class<T> type,String[] names,Step21Control control)throws Exception{
  if(type.isInstance(source))return type.cast(source);
  Exception chain=new Exception("WFC adapter unavailable: "+type.getName());long pointer=ptr(source);
  for(String name:names){try{Class<?> implementation=Class.forName(name);var ctor=implementation.getDeclaredConstructor(long.class);ctor.setAccessible(true);Object wrapper=ctor.newInstance(pointer);
    if(!type.isInstance(wrapper)||ptr(wrapper)!=pointer)throw new IllegalStateException("Adapter identity mismatch");control.record("WFC_INTERNAL_ADAPTER_ACQUIRED",map("publicView",type.getName(),"adapterClass",name,"internalApiRuntimeContractCertified",false));return type.cast(wrapper);
   }catch(ReflectiveOperationException|RuntimeException error){chain.addSuppressed(error);}}
  throw chain;
 }
 private static final class Snapshot {
  Model model;WSolid solid;SolidBody body;Surface face;WSession wsession;PreflightAudit trace;Selection capturedSelection;Integer capturedSelectionCount,stableFaceId;Integer stableBodyId;String stamp;boolean dirty;LinkedHashMap<Integer,Feature> features;
 }
 private static Snapshot inspect(Step21Control control)throws Exception{
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
  if(s.dirty)throw new IllegalStateException("试验要求dirty=false；不要保存或自动清理模型状态。");
  if(s.stamp==null||s.stamp.isBlank())throw new IllegalStateException("模型版本上下文不可读，未写模型。");
  s.features=featureIds(s.model,trace);requirePfcFeatureStatus(s.features,trace);
  Integer bodyId=trace.read("PREFLIGHT_BODY_ID_READ_START","PREFLIGHT_BODY_ID_READ_DONE","SolidBody.GetId",()->s.body.GetId(),value->map("value",value));
  Integer faceId=trace.read("PREFLIGHT_FACE_ID_READ_START","PREFLIGHT_FACE_ID_READ_DONE","Surface.GetId",()->s.face.GetId(),value->map("value",value));
  control.record("PREFLIGHT",map("phase","DONE","model",TARGET,"nativeIsSheetmetal",false,"bodyId",bodyId,"drivingFaceId",faceId,"drivingFaceRequired",true,"lengthUnit","mm","globalThicknessProven",false,"modelVersionStamp",s.stamp,"dirtyBefore",s.dirty,"featureCountBefore",s.features.size(),"statusGateSource",FeatureStateGate.SOURCE,"fiveFailureFlagsChecked",false,"conversionReadinessCertified",false));return s;
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
 private static SolidBody verifyDefaultBody(Snapshot s,Step21Control control,boolean recheck)throws Exception{
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
 private static FaceIdentityFailure faceReadFailure(Step21Control control,String stage,String operation,Throwable error)throws Exception{
  control.record("FACE_SCOPE_ERROR",map("stage",stage,"errorCode","FACE_SCOPE_READ_EXCEPTION","operation",operation,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));
  return new FaceIdentityFailure(ScopedFaceIdentity.Code.FACE_SCOPE_READ_EXCEPTION,stage,operation,error);
 }
 private static Surface selectedFace(Snapshot s,Step21Control control,boolean recheck)throws Exception{
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
 private static void requireFace(Snapshot s,Step21Control control,boolean recheck)throws Exception{
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
 private static LinkedHashMap<Integer,Feature> featureIds(Model model,PreflightAudit trace)throws Exception{
  ModelItems items=trace.read("FEATURE_LIST_START","FEATURE_LIST_COLLECTION_DONE","Model.ListItems(ITEM_FEATURE)",()->model.ListItems(ModelItemType.ITEM_FEATURE),value->map("returnedNull",value==null));
  if(items==null)throw new IllegalStateException("Feature list unavailable");
  int count=trace.read("FEATURE_LIST_COUNT_START","FEATURE_LIST_COUNT_DONE","ModelItems.getarraysize",items::getarraysize,value->map("count",value));
  if(count<0||count>10000)throw new IllegalStateException("Feature list outside existing bound");
  LinkedHashMap<Integer,Feature> result=new LinkedHashMap<>();
  for(int i=0;i<count;i++){
   final int index=i;Map<String,Object> indexContext=map("featureIndex",index,"featureIdKnown",false);
   Feature feature=trace.read("FEATURE_LIST_ITEM_START","FEATURE_LIST_ITEM_DONE","ModelItems.get",indexContext,()->{
    ModelItem item=items.get(index);if(!(item instanceof Feature found))throw new IllegalStateException("Invalid feature interface at index "+index);return found;
   },value->map("featureInterface",true));
   int id=trace.read("FEATURE_LIST_ID_START","FEATURE_LIST_ID_DONE","Feature.GetId",indexContext,feature::GetId,value->map("featureId",value));
   if(result.putIfAbsent(id,feature)!=null)throw new IllegalStateException("Duplicate feature ID "+id+" at index "+index);
  }
  trace.note("FEATURE_LIST_DONE",map("uniqueIds",result.size(),"processed",count,"featureIds",new ArrayList<>(result.keySet())));return result;
 }
 private static String statusName(FeatureStatus status){
  if(status==null)return null;
  return switch(status.getValue()){
   case FeatureStatus._FEAT_ACTIVE->"FEAT_ACTIVE";case FeatureStatus._FEAT_INACTIVE->"FEAT_INACTIVE";
   case FeatureStatus._FEAT_FAMILY_TABLE_SUPPRESSED->"FEAT_FAMILY_TABLE_SUPPRESSED";
   case FeatureStatus._FEAT_SIMP_REP_SUPPRESSED->"FEAT_SIMP_REP_SUPPRESSED";
   case FeatureStatus._FEAT_PROGRAM_SUPPRESSED->"FEAT_PROGRAM_SUPPRESSED";
   case FeatureStatus._FEAT_SUPPRESSED->"FEAT_SUPPRESSED";case FeatureStatus._FEAT_UNREGENERATED->"FEAT_UNREGENERATED";
   default->"UNKNOWN_STATUS_"+status.getValue();};
 }
 private static void requirePfcFeatureStatus(LinkedHashMap<Integer,Feature> features,PreflightAudit trace)throws Exception{
  trace.note("FEATURE_STATUS_COVERAGE",map("source",FeatureStateGate.SOURCE,"statusGateCoverage","PFC_STATUS_ONLY","wfcStatusFlagEnabled",false,"fiveFailureFlagsChecked",false,"disabledReason","REPORTED_STEP18_STATUS_FLAG_HANG"));
  String countReason=FeatureStateGate.countRejection(features.size());
  if(countReason!=null){trace.note("FEATURE_COUNT_REJECTED",map("count",features.size(),"expected",FeatureStateGate.EXPECTED_FEATURE_COUNT,"reason",countReason));throw new IllegalStateException(countReason);}
  int index=0;int diagnosticOnly=0;
  for(Map.Entry<Integer,Feature> entry:features.entrySet()){
   final int id=entry.getKey();Feature feature=entry.getValue();Map<String,Object> context=map("featureIndex",index++,"featureId",id,"featureIdKnown",true);
   int currentId=trace.read("FEATURE_STATE_ID_START","FEATURE_STATE_ID_DONE","Feature.GetId",context,feature::GetId,value->map("featureId",value));
   if(currentId!=id){trace.note("FEATURE_STATE_REJECTED",map("featureId",id,"currentFeatureId",currentId,"reason","FEATURE_ID_CHANGED"));throw new IllegalStateException("Feature ID changed: "+id+" -> "+currentId);}
   FeatureStatus status=trace.read("FEATURE_STATUS_START","FEATURE_STATUS_DONE","Feature.GetStatus",context,feature::GetStatus,value->map("returnedNull",value==null,"status",statusName(value),"statusCode",value==null?null:value.getValue()));
   String name=statusName(status);String reason=FeatureStateGate.rejection(name);boolean diagnostic=FeatureStateGate.diagnosticOnly(name);
   if(diagnostic)diagnosticOnly++;
   Map<String,Object> summary=map("featureId",id,"featureIndex",index-1,"status",name,"result",reason==null?"ACCEPTED":"REJECTED","reason",reason,"inactiveOrSuppressedDiagnosticOnly",diagnostic,"source",FeatureStateGate.SOURCE,"fiveFailureFlagsChecked",false);
   trace.note("FEATURE_STATE_DONE",summary);
   if(reason!=null){trace.note("FEATURE_STATE_REJECTED",summary);throw new IllegalStateException("Feature state blocked: featureId="+id+", status="+name+", reason="+reason);}
  }
  trace.note("NATIVE_FAILURE_FLAGS_DONE",map("source",FeatureStateGate.SOURCE,"statusGateCoverage","PFC_STATUS_ONLY","featuresChecked",features.size(),"inactiveOrSuppressedDiagnosticCount",diagnosticOnly,"wfcStatusFlagEnabled",false,"fiveFailureFlagsChecked",false,"fullFailureFlagCoverage",false,"pfcStatusesAccepted",true,"requiresExactlyFiveAllActive",true));
 }
 private static void recheck(Snapshot s,Step21Control control)throws Exception{
  PreflightAudit trace=s.trace;
  boolean unchanged=trace.read("PRECOMMIT_CONTEXT_START","PRECOMMIT_CONTEXT_DONE","MODEL_SCOPE_VERSION_DIRTY_RECHECK",()->active&&same(session.GetActiveModel(),s.model)&&TARGET.equalsIgnoreCase(s.model.GetFileName())&&Objects.equals(s.stamp,s.model.GetVersionStamp())&&s.dirty==s.model.GetIsModified(),value->map("unchanged",value));
  if(!unchanged)throw new IllegalStateException("确认期间模型/版本/dirty变化，未提交模型。");
  s.body=trace.read("PRECOMMIT_BODY_START","PRECOMMIT_BODY_DONE","VERIFY_ORIGINAL_DEFAULT_BODY",()->verifyDefaultBody(s,control,true),value->map("accepted",true));
  boolean mm=trace.read("PRECOMMIT_UNITS_START","PRECOMMIT_UNITS_DONE","SOLID_LENGTH_UNIT_RECHECK",()->{UnitSystem units=s.solid.GetPrincipalUnits();Unit unit=units==null?null:units.GetUnit(UnitType.UNIT_LENGTH);return unit!=null&&"mm".equalsIgnoreCase(unit.GetName());},value->map("mm",value));
  if(!mm)throw new IllegalStateException("长度单位变化，不提交模型。");
  LinkedHashMap<Integer,Feature> current=featureIds(s.model,trace);
  if(!s.features.keySet().equals(current.keySet()))throw new IllegalStateException("确认期间特征ID集合变化，不提交模型。");
  requirePfcFeatureStatus(current,trace);
  trace.read("PRECOMMIT_FACE_START","PRECOMMIT_FACE_DONE","VERIFY_ORIGINAL_CAPTURED_FACE",()->{requireFace(s,control,true);return Boolean.TRUE;},value->map("accepted",value));
  trace.note("PRECOMMIT_RECHECK_DONE",map("fiveFeaturesAllActive",true,"originalBodyAndFacePreserved",true,"fiveFailureFlagsChecked",false));
 }
 private static void appendElement(Elements elements,int index,int id,String idName,int level,String argumentKind,PreflightAudit.Call<ArgValue> factory,PreflightAudit trace)throws Exception{
  Map<String,Object> context=map("elementIndex",index,"elementId",id,"elementIdName",idName,"level",level,"argumentKind",argumentKind);
  ArgValue value=factory==null?null:trace.read("ELEMENT_VALUE_CREATE_START","ELEMENT_VALUE_CREATE_DONE","pfcArgument."+argumentKind,context,factory,result->map("returnedNull",result==null));
  if(factory!=null&&value==null)throw new IllegalStateException("Argument value returned null for "+idName);
  Element element=trace.read("ELEMENT_CREATE_START","ELEMENT_CREATE_DONE","wfcElementTree.Element_Create",context,()->wfcElementTree.Element_Create(id,value,level),result->map("returnedNull",result==null));
  if(element==null)throw new IllegalStateException("Element_Create returned null for "+idName);
  trace.read("ELEMENT_APPEND_START","ELEMENT_APPEND_DONE","Elements.append",context,()->{elements.append(element);return Boolean.TRUE;},result->map("returned",result));
 }
 private static ElementTree prepareTree(Snapshot s,Step21Control control)throws Exception{
  PreflightAudit trace=s.trace;
  Selection body=trace.read("SELECTION_BODY_CREATE_START","SELECTION_BODY_CREATE_DONE","pfcSelect.CreateModelItemSelection(body)",map("bodyId",s.stableBodyId),()->pfcSelect.CreateModelItemSelection(s.body,null),value->map("returnedNull",value==null));
  if(body==null)throw new IllegalStateException("Body selection returned null");
  Selection face=trace.read("SELECTION_FACE_CREATE_START","SELECTION_FACE_CREATE_DONE","pfcSelect.CreateModelItemSelection(face)",map("faceId",s.stableFaceId),()->pfcSelect.CreateModelItemSelection(s.face,null),value->map("returnedNull",value==null));
  if(face==null)throw new IllegalStateException("Face selection returned null");
  Elements elements=trace.read("ELEMENTS_CREATE_START","ELEMENTS_CREATE_DONE","Elements.create",Elements::create,value->map("returnedNull",value==null));
  if(elements==null)throw new IllegalStateException("Elements.create returned null");
  appendElement(elements,0,wfcElemIds.PRO_E_FEATURE_TREE,"PRO_E_FEATURE_TREE",0,"NULL_ROOT",null,trace);
  appendElement(elements,1,wfcElemIds.PRO_E_FEATURE_TYPE,"PRO_E_FEATURE_TYPE",1,"CreateIntArgValue",()->pfcArgument.CreateIntArgValue(FeatureType.FEATTYPE_DATUM_SURFACE.getValue()),trace);
  appendElement(elements,2,wfcElemIds.PRO_E_STD_FEATURE_NAME,"PRO_E_STD_FEATURE_NAME",1,"CreateStringArgValue",()->pfcArgument.CreateStringArgValue("P1_STEP21_DRIVING"),trace);
  appendElement(elements,3,wfcElemIds.PRO_E_DTM_SRF_TYPE,"PRO_E_DTM_SRF_TYPE",1,"CreateIntArgValue",()->pfcArgument.CreateIntArgValue(15),trace);
  appendElement(elements,4,wfcElemIds.PRO_E_SMT_CONV_BODY,"PRO_E_SMT_CONV_BODY",1,"CreateSelectionArgValue",()->pfcArgument.CreateSelectionArgValue(body),trace);
  appendElement(elements,5,wfcElemIds.PRO_E_SMT_DRV_SURF,"PRO_E_SMT_DRV_SURF",1,"CreateSelectionArgValue",()->pfcArgument.CreateSelectionArgValue(face),trace);
  appendElement(elements,6,wfcElemIds.PRO_E_STD_SMT_THICKNESS,"PRO_E_STD_SMT_THICKNESS",1,"CreateDoubleArgValue",()->pfcArgument.CreateDoubleArgValue(control.thickness()),trace);
  ElementTree tree=trace.read("SESSION_CREATE_ELEMENT_TREE_START","SESSION_CREATE_ELEMENT_TREE_DONE","WSession.CreateElementTree",()->s.wsession.CreateElementTree(elements),value->map("returnedNull",value==null,"elementsAppended",7));
  if(tree==null)throw new IllegalStateException("Session.CreateElementTree returned null");return tree;
 }
 private static String execute(Step21Control control)throws Exception{
  Snapshot s=inspect(control);PreflightAudit trace=s.trace;
  boolean first=trace.read("FIRST_CONFIRMATION_START","FIRST_CONFIRMATION_DONE","Session.UIShowMessageDialog_FIRST",()->confirm("第一次确认：本Step21只定位内存元素树准备，不创建模型特征、不再生、不保存、不重试。\n当前200002100.prt，原五类失败flag未检查；全局壁厚未证明。\n确认在可丢弃试验模型中继续输入本次测试厚度吗？"),value->map("accepted",value));
  if(!first){control.finish("CANCELLED_FIRST_CONFIRMATION_NO_WRITE");return "已取消第一次确认，未写模型；本步点击已消耗。";}
  control.acceptRisk();trace.read("THICKNESS_PROMPT_START","THICKNESS_PROMPT_DONE","Session.UIDisplayMessage",()->{session.UIDisplayMessage(MESSAGES,"P1_S21_THICKNESS",null);return Boolean.TRUE;},value->map("returned",value));
  String input=trace.read("THICKNESS_INPUT_START","THICKNESS_INPUT_DONE","Session.UIReadStringMessage",()->session.UIReadStringMessage(Boolean.FALSE),value->map("returnedNull",value==null,"textLength",value==null?null:value.length()));
  if(input==null||input.isBlank()){control.finish("CANCELLED_THICKNESS_INPUT_NO_WRITE");return "未输入试验厚度，未写模型；本步点击已消耗。";}
  control.setThickness(input);recheck(s,control);
  boolean last=trace.read("FINAL_CONFIRMATION_START","FINAL_CONFIRMATION_DONE","Session.UIShowMessageDialog_FINAL",()->confirm("第二次确认：仅准备内存元素树以定位卡点。模型200002100.prt，原驱动面ID="+s.stableFaceId+"，明确测试厚度="+control.thickness()+" mm。\n将逐项记录选择、参数值、元素构造/追加及CreateElementTree；不调用模型创建、再生或保存。\n接受后只准备一次并结束，不恢复Step20转换，继续吗？"),value->map("accepted",value));
  if(!last){control.finish("CANCELLED_FINAL_CONFIRMATION_NO_WRITE");return "已取消元素树准备确认，未写模型；本步点击已消耗。";}
  control.acceptFinal();recheck(s,control);
  trace.read("ELEMENT_TREE_PREPARE_START","ELEMENT_TREE_PREPARE_DONE","PREPARE_IN_MEMORY_DRIVING_TREE",()->prepareTree(s,control),value->map("available",true,"elementsAppended",7,"drivingSubtype",15,"testThicknessMm",control.thickness(),"noModelWrite",true));
  control.finish("ELEMENT_TREE_PREPARED_NO_MODEL_WRITE_NO_SAVE");
  return "本次内存元素树准备已返回，逐项日志已记录；本命令到此结束。\n没有模型创建、再生或保存调用，不自动恢复Step20。本次成功不证明此前卡顿是偶发；请回收日志，不再点击。";
 }
}
