package p1.controlled.step16;
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
import com.ptc.pfc.pfcArgument.*;
import com.ptc.wfc.wfcSession.WSession;
import com.ptc.wfc.wfcSolid.WSolid;
import com.ptc.wfc.wfcFeature.*;
import com.ptc.wfc.wfcFeatureInstructions.*;
import com.ptc.wfc.wfcSolidInstructions.*;
import com.ptc.wfc.wfcElementTree.*;
import com.ptc.wfc.wfcElemIds.wfcElemIds;
import static p1.controlled.step16.AuditSupport.*;

/** Local, unregistered-by-default candidate. Synchronous command, no Save, no retry or model restore. */
public final class P1ControlledConversionStep16 {
 public static final String COMMAND_ID="SISWControlledConvertStep16";
 private static final String TARGET="200002100.prt",MESSAGES="p1_controlled_conversion_step16.txt";
 private static Session session;
 private static UICommand command;
 private static Listener listener;
 private static volatile boolean active;
 private static final AtomicBoolean busy=new AtomicBoolean();
 private static final AtomicBoolean clicked=new AtomicBoolean();
 private P1ControlledConversionStep16(){}
 public static synchronized void start()throws Exception{
  if(active)return;
  if(command!=null||listener!=null)throw new IllegalStateException("Previous registration retained; do not re-register");
  session=pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
  if(session==null)throw new IllegalStateException("No synchronous session");listener=new Listener();
  try{command=session.UICreateCommand(COMMAND_ID,listener);if(command==null)throw new IllegalStateException("No command");
   command.Designate(MESSAGES,"P1_S16_LABEL","P1_S16_HELP","P1_S16_DESC");active=true;
  }catch(Exception|LinkageError error){if(command!=null){try{command.RemoveActionListener(listener);command=null;listener=null;}catch(Exception|LinkageError detach){error.addSuppressed(detach);}}else listener=null;session=null;throw error;}
 }
 public static synchronized void stop()throws Exception{
  active=false;if(busy.get())throw new IllegalStateException("Controlled operation running; do not unload");
  if(command!=null&&listener!=null)command.RemoveActionListener(listener);command=null;listener=null;session=null;
 }
 private static void display(String text,boolean error)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();
  options.SetMessageDialogType(error?MessageDialogType.MESSAGE_ERROR:MessageDialogType.MESSAGE_INFO);
  options.SetDialogLabel("P1 Step16 — 不保存试验状态");MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_OK);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_OK);
  session.UIShowMessageDialog(text,options);
 }
 private static void feedback(String text,boolean error){feedback(text,error,"P1_S16_FAILED");}
 private static void feedback(String text,boolean error,String statusKey){
  System.err.println("[P1_STEP16] "+text);
  try{display(text,error);}catch(Exception|LinkageError dialogError){System.err.println("[P1_STEP16] RESULT_DIALOG_FAILED "+dialogError.getClass().getName());
   try{session.UIDisplayMessage(MESSAGES,statusKey,null);}catch(Exception|LinkageError statusError){System.err.println("[P1_STEP16] BOTH_NATIVE_FEEDBACK_ROUTES_FAILED "+statusError.getClass().getName());}}
 }
 private static boolean confirm(String text)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();options.SetMessageDialogType(MessageDialogType.MESSAGE_WARNING);options.SetDialogLabel("P1 第十六步二次确认");
  MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_YES);buttons.append(MessageButton.MESSAGE_BUTTON_NO);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_NO);
  return session.UIShowMessageDialog(text,options)==MessageButton.MESSAGE_BUTTON_YES;
 }
 private static final class Listener extends DefaultUICommandActionListener {
  @Override public void OnCommand(){
   if(!active||session==null){feedback("命令未激活，未执行转换。",true);return;}
   if(!busy.compareAndSet(false,true)){feedback("已有受控操作进行中，拒绝重入；未新增转换。",true);return;}
   if(!clicked.compareAndSet(false,true)){busy.set(false);feedback("本进程第十六步点击次数已用尽；未进入转换，不重试。",true);return;}
   String message="本次未提交转换；一次点击已消耗，禁止自动重试。";boolean error=true;String statusKey="P1_S16_FAILED";
   try{
    Path directory=Paths.get(System.getProperty("user.home"),"CreoControlledConversionStep16");
    try(Step16Control control=new Step16Control(directory)){
     try{message=execute(control);error=false;statusKey=control.dispatched()?"P1_S16_VERIFIED":"P1_S16_CANCELLED";}
     catch(Exception|LinkageError failure){
      String outcome=control.dispatched()?"FAILED_OR_UNVERIFIED_MODEL_STATE_RETAINED_NO_SAVE":"BLOCKED_BEFORE_WRITE_NO_SAVE";
      String phase=control.state().toString();
      try{control.record("ERROR",map("exceptionClass",failure.getClass().getName(),"message",String.valueOf(failure.getMessage()),"phase",phase,"errorCode",failure instanceof FaceIdentityFailure faceFailure?faceFailure.code.name():failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.code.name():"OTHER_FAILURE","bodyCheckStage",failure instanceof BodyIdentityFailure bodyFailure?bodyFailure.stage:"NOT_BODY_CHECK","faceCheckStage",failure instanceof FaceIdentityFailure faceFailure?faceFailure.stage:"NOT_FACE_CHECK"));}catch(Exception logError){failure.addSuppressed(logError);}
      failure.printStackTrace(System.err);
      if(control.dispatched())readonlyFailureSnapshot(control);
      try{control.finish(outcome);}catch(Exception logError){System.err.println("[P1_STEP16] FINAL_AUDIT_WRITE_FAILED "+logError.getClass().getName());}
      message=control.dispatched()?"转换失败或核验不完整。模型可能已部分改变；未自动回滚，失败状态保留。本命令没有保存能力；禁止通过其他入口保存当前试验状态，不重试。\n错误："+failure.getClass().getSimpleName()+" / "+failure.getMessage():"未执行模型转换（前置检查/确认/审计未通过）。一次点击已消耗，不重试。\n原因："+failure.getMessage();
     }
    }
   }catch(FileAlreadyExistsException already){message="本机第十六步一次点击锁已存在；本次未进入转换。不要删除记录、换目录或重载绕过单次限制。";}
   catch(Exception|LinkageError outer){error=true;statusKey="P1_S16_FAILED";outer.printStackTrace(System.err);message="受控记录创建或关闭失败，禁止继续/保存/重试；若已提交，模型可能已改变。请保留现场及stderr。\n"+outer.getClass().getSimpleName();}
   finally{busy.set(false);}
   feedback(message,error,statusKey);
  }
 }
 private static long ptr(Object value){if(!(value instanceof jxobject_i nativeObject))throw new IllegalStateException("Native identity unavailable");long p=nativeObject.getptr();if(p==0)throw new IllegalStateException("Zero native identity");return p;}
 private static boolean same(Object a,Object b){return a!=null&&b!=null&&(a==b||ptr(a)==ptr(b));}
 private static <T> T adapt(Object source,Class<T> type,String[] names,Step16Control control)throws Exception{
  if(type.isInstance(source))return type.cast(source);
  Exception chain=new Exception("WFC adapter unavailable: "+type.getName());long pointer=ptr(source);
  for(String name:names){try{Class<?> implementation=Class.forName(name);var ctor=implementation.getDeclaredConstructor(long.class);ctor.setAccessible(true);Object wrapper=ctor.newInstance(pointer);
    if(!type.isInstance(wrapper)||ptr(wrapper)!=pointer)throw new IllegalStateException("Adapter identity mismatch");control.record("WFC_INTERNAL_ADAPTER_ACQUIRED",map("publicView",type.getName(),"adapterClass",name,"internalApiRuntimeContractCertified",false));return type.cast(wrapper);
   }catch(ReflectiveOperationException|RuntimeException error){chain.addSuppressed(error);}}
  throw chain;
 }
 private static final class Snapshot {
  Model model;WSolid solid;SolidBody body;Surface face;WSession wsession;Selection capturedSelection;Integer capturedSelectionCount,stableFaceId;Integer stableBodyId;String stamp;boolean dirty;Set<Integer> featureIds;
 }
 private static Snapshot inspect(Step16Control control)throws Exception{
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
  requireFace(s,control,false);s.stamp=s.model.GetVersionStamp();s.dirty=s.model.GetIsModified();
  if(s.stamp==null||s.stamp.isBlank())throw new IllegalStateException("模型版本上下文不可读，未写模型。");
  s.featureIds=featureIds(s.model);requireNoNativeFailure(s.solid);
  control.record("PREFLIGHT",map("model",TARGET,"nativeIsSheetmetal",false,"bodyId",s.body.GetId(),"drivingFaceId",s.face.GetId(),"drivingFaceRequired",true,"lengthUnit","mm","globalThicknessProven",false,"modelVersionStamp",s.stamp,"dirtyBefore",s.dirty,"featureCountBefore",s.featureIds.size()));return s;
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
 private static SolidBody verifyDefaultBody(Snapshot s,Step16Control control,boolean recheck)throws Exception{
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
 private static FaceIdentityFailure faceReadFailure(Step16Control control,String stage,String operation,Throwable error)throws Exception{
  control.record("FACE_SCOPE_ERROR",map("stage",stage,"errorCode","FACE_SCOPE_READ_EXCEPTION","operation",operation,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));
  return new FaceIdentityFailure(ScopedFaceIdentity.Code.FACE_SCOPE_READ_EXCEPTION,stage,operation,error);
 }
 private static Surface selectedFace(Snapshot s,Step16Control control,boolean recheck)throws Exception{
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
 private static void requireFace(Snapshot s,Step16Control control,boolean recheck)throws Exception{
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
 private static Set<Integer> featureIds(Model model)throws Exception{
  ModelItems items=model.ListItems(ModelItemType.ITEM_FEATURE);if(items==null||items.getarraysize()>10000)throw new IllegalStateException("Feature list unavailable/too large");Set<Integer> ids=new LinkedHashSet<>();
  for(int i=0;i<items.getarraysize();i++){ModelItem item=items.get(i);if(!(item instanceof Feature)||!ids.add(item.GetId()))throw new IllegalStateException("Invalid/duplicate feature identity");}return ids;
 }
 private static void requireNoNativeFailure(WSolid solid)throws Exception{
  FeatureStatusFlags flags=solid.GetSolidFeatureStatusFlags();if(flags==null||flags.getarraysize()>10000)throw new IllegalStateException("Feature error flags unknown/too large");
  for(int i=0;i<flags.getarraysize();i++){FeatureStatusFlag flag=flags.get(i);if(flag==null||flag.GetIsFailed()||flag.GetIsInvalid()||flag.GetIsUnregenerated()||flag.GetIsChildOfFailed()||flag.GetIsChildOfExternalFailed())throw new IllegalStateException("Native feature failed/invalid/unregenerated flag present");}
 }
 private static void recheck(Snapshot s,Step16Control control)throws Exception{
  try{if(!active||!same(session.GetActiveModel(),s.model)||!TARGET.equalsIgnoreCase(s.model.GetFileName())||!Objects.equals(s.stamp,s.model.GetVersionStamp())||s.dirty!=s.model.GetIsModified())throw new BodyIdentityFailure(ScopedBodyIdentity.Code.MODEL_SCOPE_MISMATCH,"RECHECK","确认期间激活模型/版本/dirty变化，未写模型。",null);}
  catch(BodyIdentityFailure mismatch){throw mismatch;}
  catch(Exception|LinkageError readError){throw new BodyIdentityFailure(ScopedBodyIdentity.Code.STATE_READ_EXCEPTION,"RECHECK","确认期间模型作用域读取异常",readError);}
  s.body=verifyDefaultBody(s,control,true);
  UnitSystem units=s.solid.GetPrincipalUnits();Unit unit=units==null?null:units.GetUnit(UnitType.UNIT_LENGTH);if(unit==null||!"mm".equalsIgnoreCase(unit.GetName()))throw new IllegalStateException("长度单位变化。");
  if(!s.featureIds.equals(featureIds(s.model)))throw new IllegalStateException("确认期间特征列表变化。");requireFace(s,control,true);requireNoNativeFailure(s.solid);
 }
 private static void element(Elements elements,int id,ArgValue value,int level)throws Exception{elements.append(wfcElementTree.Element_Create(id,value,level));}
 private static String execute(Step16Control control)throws Exception{
  Snapshot s=inspect(control);
  if(!confirm("第一次确认：当前零件200002100.prt；原生默认体IsSheetmetal=false。\n全局壁厚尚未证明，局部1.2mm不是全局厚度依据。\n仅对可丢弃试验模型执行一次转换，不保存、不覆盖原文件；失败可能保留部分改变，不自动回滚。\n你确认当前模型可丢弃并同意继续准备本次试验吗？")){control.finish("CANCELLED_FIRST_CONFIRMATION_NO_WRITE");return "已取消第一次确认，未写模型；本次点击已消耗，不重试。";}
  control.acceptRisk();session.UIDisplayMessage(MESSAGES,"P1_S16_THICKNESS",null);String input=session.UIReadStringMessage(Boolean.FALSE);
  if(input==null||input.isBlank()){control.finish("CANCELLED_THICKNESS_INPUT_NO_WRITE");return "未输入试验厚度，未写模型；本次点击已消耗。";}
  control.setThickness(input);recheck(s,control);
  if(!confirm("第二次最终确认：原生IsSheetmetal=false；全局壁厚未证明。\n可丢弃模型：200002100.prt；预选驱动面ID="+s.face.GetId()+"。\n本次仅使用你明确输入的试验厚度 "+control.thickness()+" mm，不视为真实全局壁厚。\n确认后最多调用一次WCreateFeature与一次WRegenerate；不保存。失败保留状态，禁止保存/重试。\n现在提交这唯一一次原生转换吗？")){control.finish("CANCELLED_FINAL_CONFIRMATION_NO_WRITE");return "已取消最终确认，未写模型；本次点击已消耗。";}
  control.acceptFinal();recheck(s,control);
  Selection body=pfcSelect.CreateModelItemSelection(s.body,null),face=pfcSelect.CreateModelItemSelection(s.face,null);if(body==null||face==null)throw new IllegalStateException("Native selections unavailable");
  Elements elements=Elements.create();element(elements,wfcElemIds.PRO_E_FEATURE_TREE,null,0);
  element(elements,wfcElemIds.PRO_E_FEATURE_TYPE,pfcArgument.CreateIntArgValue(FeatureType.FEATTYPE_DATUM_SURFACE.getValue()),1);
  element(elements,wfcElemIds.PRO_E_STD_FEATURE_NAME,pfcArgument.CreateStringArgValue("P1_STEP16_DRIVING"),1);
  element(elements,wfcElemIds.PRO_E_DTM_SRF_TYPE,pfcArgument.CreateIntArgValue(15),1);
  element(elements,wfcElemIds.PRO_E_SMT_CONV_BODY,pfcArgument.CreateSelectionArgValue(body),1);
  element(elements,wfcElemIds.PRO_E_SMT_DRV_SURF,pfcArgument.CreateSelectionArgValue(face),1);
  element(elements,wfcElemIds.PRO_E_STD_SMT_THICKNESS,pfcArgument.CreateDoubleArgValue(control.thickness()),1);
  ElementTree tree=s.wsession.CreateElementTree(elements);if(tree==null)throw new IllegalStateException("Element tree unavailable");
  FeatCreateOptions options=FeatCreateOptions.create();options.append(FeatCreateOption.FEAT_CR_NO_OPTS);
  WRegenInstructions regen=wfcSolidInstructions.WRegenInstructions_Create();regen.SetNoResolveMode(true);regen.SetAllowConfirm(false);
  recheck(s,control);control.markDispatch();
  WFeature created=s.solid.WCreateFeature(tree,options,regen); // sole feature create site; no loops/retries
  if(created==null)throw new IllegalStateException("WCreateFeature returned null; model state unknown");
  control.record("CREATE_RETURNED",map("createdFeatureId",created.GetId(),"createdFeatureName",created.GetName()));
  s.solid.WRegenerate(regen); // sole explicit regeneration site
  control.record("REGENERATE_RETURNED",map("readbackFollows",true));
  if(!same(session.GetActiveModel(),s.model))throw new IllegalStateException("Active model changed after dispatch");
  SolidBody converted=s.solid.GetDefaultBody();boolean sheetmetal=converted!=null&&converted.IsSheetmetal();Set<Integer> after=featureIds(s.model);int id=created.GetId();
  FeatureType type=created.GetFeatType();FeatureStatus status=created.GetStatus();boolean freshListed=after.contains(id)&&!s.featureIds.contains(id);
  FeatureStatusFlag flag=created.GetStatusFlag();if(flag==null)throw new IllegalStateException("Created feature status flags unavailable");
  boolean failed=flag.GetIsFailed()||flag.GetIsInvalid()||flag.GetIsUnregenerated();
  boolean wall=type==FeatureType.FEATTYPE_WALL;boolean drivingSubtype15=false;
  ElementTree actual=created.GetElementTree(null,FeatureElemTreeExtractOptions.FEAT_EXTRACT_NO_OPTS);if(actual==null)throw new IllegalStateException("Created feature tree unavailable");
  Elements actualElements=actual.ListTreeElements();if(actualElements==null||actualElements.getarraysize()>10000)throw new IllegalStateException("Created tree readback unavailable");
  for(int i=0;i<actualElements.getarraysize();i++){Element e=actualElements.get(i);if(e!=null&&e.GetLevel()==1&&e.GetId()==wfcElemIds.PRO_E_DTM_SRF_TYPE){ArgValue value=e.GetValue();drivingSubtype15=value!=null&&value.GetIntValue()==15;}}
  wall=wall||(type==FeatureType.FEATTYPE_DATUM_SURFACE&&drivingSubtype15);
  control.record("NATIVE_READBACK",map("nativeIsSheetmetalAfter",sheetmetal,"createdFeatureId",id,"createdFeatureName",created.GetName(),"featureType",type==null?"UNKNOWN":type.getValue(),"featureTypeName",created.GetFeatTypeName(),"featureSubtype",created.GetFeatSubType(),"featureStatus",status==null?"UNKNOWN":status.getValue(),"newFeatureListed",freshListed,"nativeFailedOrInvalidOrUnregenerated",failed,"wallTypeOrExtractedDriving15",wall,"extractedDrivingSubtype15",drivingSubtype15,"visualTreeInspection","PENDING_OPERATOR_OBSERVATION","modelIsModifiedAfter",s.model.GetIsModified(),"globalThicknessProof",false));
  requireNoNativeFailure(s.solid);
  if(!sheetmetal||!freshListed||failed||status!=FeatureStatus.FEAT_ACTIVE||!wall)throw new IllegalStateException("Post-conversion sheetmetal/new wall/active status verification incomplete");
  control.finish("NATIVE_CONVERSION_READBACK_CONFIRMED_NOT_SAVED_VISUAL_TREE_PENDING");
  return "原生转换已返回，默认体IsSheetmetal=true；新特征ID="+id+"，钣金墙类型/驱动墙元素回读通过，状态ACTIVE，未发现所查失败标志。\n请只读查看特征树是否显示新钣金壁并记录界面错误。模型未由本命令保存；全局壁厚未证明，不可用于生产发布，不再点击或保存。";
 }
 private static void readonlyFailureSnapshot(Step16Control control){
  try{Model current=session.GetActiveModel();if(current==null||!TARGET.equalsIgnoreCase(current.GetFileName())||!(current instanceof Solid solid)){control.record("FAILURE_MODEL_READBACK",map("state","UNAVAILABLE_OR_CHANGED"));return;}
   SolidBody body=solid.GetDefaultBody();control.record("FAILURE_MODEL_READBACK",map("model",TARGET,"nativeDefaultBodyIsSheetmetal",body==null?"UNKNOWN":body.IsSheetmetal(),"dirty",current.GetIsModified(),"versionStamp",known(current.GetVersionStamp()),"noRollbackAttempted",true,"noSave",true));
  }catch(Exception|LinkageError readError){System.err.println("[P1_STEP16] FAILURE_READBACK_UNAVAILABLE "+readError.getClass().getName());}
 }
}
