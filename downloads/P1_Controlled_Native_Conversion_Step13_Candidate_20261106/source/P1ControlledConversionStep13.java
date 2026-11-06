package p1.controlled.step13;
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
import static p1.controlled.step13.AuditSupport.*;

/** Local, unregistered-by-default candidate. Synchronous command, no Save, no retry or model restore. */
public final class P1ControlledConversionStep13 {
 public static final String COMMAND_ID="SISWControlledConvertStep13";
 private static final String TARGET="200002100.prt",MESSAGES="p1_controlled_conversion_step13.txt";
 private static Session session;
 private static UICommand command;
 private static Listener listener;
 private static volatile boolean active;
 private static final AtomicBoolean busy=new AtomicBoolean();
 private static final AtomicBoolean clicked=new AtomicBoolean();
 private P1ControlledConversionStep13(){}
 public static synchronized void start()throws Exception{
  if(active)return;
  if(command!=null||listener!=null)throw new IllegalStateException("Previous registration retained; do not re-register");
  session=pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
  if(session==null)throw new IllegalStateException("No synchronous session");listener=new Listener();
  try{command=session.UICreateCommand(COMMAND_ID,listener);if(command==null)throw new IllegalStateException("No command");
   command.Designate(MESSAGES,"P1_S13_LABEL","P1_S13_HELP","P1_S13_DESC");active=true;
  }catch(Exception|LinkageError error){if(command!=null){try{command.RemoveActionListener(listener);command=null;listener=null;}catch(Exception|LinkageError detach){error.addSuppressed(detach);}}else listener=null;session=null;throw error;}
 }
 public static synchronized void stop()throws Exception{
  active=false;if(busy.get())throw new IllegalStateException("Controlled operation running; do not unload");
  if(command!=null&&listener!=null)command.RemoveActionListener(listener);command=null;listener=null;session=null;
 }
 private static void display(String text,boolean error)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();
  options.SetMessageDialogType(error?MessageDialogType.MESSAGE_ERROR:MessageDialogType.MESSAGE_INFO);
  options.SetDialogLabel("P1 Step13 — 不保存试验状态");MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_OK);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_OK);
  session.UIShowMessageDialog(text,options);
 }
 private static void feedback(String text,boolean error){feedback(text,error,"P1_S13_FAILED");}
 private static void feedback(String text,boolean error,String statusKey){
  System.err.println("[P1_STEP13] "+text);
  try{display(text,error);}catch(Exception|LinkageError dialogError){System.err.println("[P1_STEP13] RESULT_DIALOG_FAILED "+dialogError.getClass().getName());
   try{session.UIDisplayMessage(MESSAGES,statusKey,null);}catch(Exception|LinkageError statusError){System.err.println("[P1_STEP13] BOTH_NATIVE_FEEDBACK_ROUTES_FAILED "+statusError.getClass().getName());}}
 }
 private static boolean confirm(String text)throws Exception{
  MessageDialogOptions options=pfcUI.MessageDialogOptions_Create();options.SetMessageDialogType(MessageDialogType.MESSAGE_WARNING);options.SetDialogLabel("P1 第十三步二次确认");
  MessageButtons buttons=MessageButtons.create();buttons.append(MessageButton.MESSAGE_BUTTON_YES);buttons.append(MessageButton.MESSAGE_BUTTON_NO);options.SetButtons(buttons);options.SetDefaultButton(MessageButton.MESSAGE_BUTTON_NO);
  return session.UIShowMessageDialog(text,options)==MessageButton.MESSAGE_BUTTON_YES;
 }
 private static final class Listener extends DefaultUICommandActionListener {
  @Override public void OnCommand(){
   if(!active||session==null){feedback("命令未激活，未执行转换。",true);return;}
   if(!busy.compareAndSet(false,true)){feedback("已有受控操作进行中，拒绝重入；未新增转换。",true);return;}
   if(!clicked.compareAndSet(false,true)){busy.set(false);feedback("本进程第十三步点击次数已用尽；未进入转换，不重试。",true);return;}
   String message="本次未提交转换；一次点击已消耗，禁止自动重试。";boolean error=true;String statusKey="P1_S13_FAILED";
   try{
    Path directory=Paths.get(System.getProperty("user.home"),"CreoControlledConversionStep13");
    try(Step13Control control=new Step13Control(directory)){
     try{message=execute(control);error=false;statusKey=control.dispatched()?"P1_S13_VERIFIED":"P1_S13_CANCELLED";}
     catch(Exception|LinkageError failure){
      String outcome=control.dispatched()?"FAILED_OR_UNVERIFIED_MODEL_STATE_RETAINED_NO_SAVE":"BLOCKED_BEFORE_WRITE_NO_SAVE";
      String phase=control.state().toString();
      try{control.record("ERROR",map("exceptionClass",failure.getClass().getName(),"message",String.valueOf(failure.getMessage()),"phase",phase));}catch(Exception logError){failure.addSuppressed(logError);}
      failure.printStackTrace(System.err);
      if(control.dispatched())readonlyFailureSnapshot(control);
      try{control.finish(outcome);}catch(Exception logError){System.err.println("[P1_STEP13] FINAL_AUDIT_WRITE_FAILED "+logError.getClass().getName());}
      message=control.dispatched()?"转换失败或核验不完整。模型可能已部分改变；未自动回滚，失败状态保留。本命令没有保存能力；禁止通过其他入口保存当前试验状态，不重试。\n错误："+failure.getClass().getSimpleName()+" / "+failure.getMessage():"未执行模型转换（前置检查/确认/审计未通过）。一次点击已消耗，不重试。\n原因："+failure.getMessage();
     }
    }
   }catch(FileAlreadyExistsException already){message="本机第十三步一次点击锁已存在；本次未进入转换。不要删除记录、换目录或重载绕过单次限制。";}
   catch(Exception|LinkageError outer){error=true;statusKey="P1_S13_FAILED";outer.printStackTrace(System.err);message="受控记录创建或关闭失败，禁止继续/保存/重试；若已提交，模型可能已改变。请保留现场及stderr。\n"+outer.getClass().getSimpleName();}
   finally{busy.set(false);}
   feedback(message,error,statusKey);
  }
 }
 private static long ptr(Object value){if(!(value instanceof jxobject_i nativeObject))throw new IllegalStateException("Native identity unavailable");long p=nativeObject.getptr();if(p==0)throw new IllegalStateException("Zero native identity");return p;}
 private static boolean same(Object a,Object b){return a!=null&&b!=null&&(a==b||ptr(a)==ptr(b));}
 private static <T> T adapt(Object source,Class<T> type,String[] names,Step13Control control)throws Exception{
  if(type.isInstance(source))return type.cast(source);
  Exception chain=new Exception("WFC adapter unavailable: "+type.getName());long pointer=ptr(source);
  for(String name:names){try{Class<?> implementation=Class.forName(name);var ctor=implementation.getDeclaredConstructor(long.class);ctor.setAccessible(true);Object wrapper=ctor.newInstance(pointer);
    if(!type.isInstance(wrapper)||ptr(wrapper)!=pointer)throw new IllegalStateException("Adapter identity mismatch");control.record("WFC_INTERNAL_ADAPTER_ACQUIRED",map("publicView",type.getName(),"adapterClass",name,"internalApiRuntimeContractCertified",false));return type.cast(wrapper);
   }catch(ReflectiveOperationException|RuntimeException error){chain.addSuppressed(error);}}
  throw chain;
 }
 private static final class Snapshot {
  Model model;WSolid solid;SolidBody body;Surface face;WSession wsession;String stamp;boolean dirty;Set<Integer> featureIds;
 }
 private static Snapshot inspect(Step13Control control)throws Exception{
  Snapshot s=new Snapshot();s.model=session.GetActiveModel();
  if(s.model==null||s.model.GetType()!=ModelType.MDL_PART||!TARGET.equalsIgnoreCase(s.model.GetFileName()))throw new IllegalStateException("只允许当前普通零件200002100.prt，禁止装配/其他模型。");
  s.solid=adapt(s.model,WSolid.class,new String[]{"com.ptc.wfc.Implementation.wfcPart$WPart","com.ptc.wfc.Implementation.wfcSolid$WSolid"},control);
  s.wsession=adapt(session,WSession.class,new String[]{"com.ptc.wfc.Implementation.wfcSession$WSession"},control);
  if(!same(s.model,s.solid))throw new IllegalStateException("WFC model identity mismatch");
  ModelItems bodies=s.model.ListItems(ModelItemType.ITEM_BODY);if(bodies==null||bodies.getarraysize()!=1)throw new IllegalStateException("本次只允许一个原生实体体；多体/未知不执行。");
  s.body=s.solid.GetDefaultBody();if(s.body==null||!same(bodies.get(0),s.body)||s.body.IsSheetmetal())throw new IllegalStateException("默认体未知/不一致/已是钣金，不执行。");
  UnitSystem units=s.solid.GetPrincipalUnits();Unit length=units==null?null:units.GetUnit(UnitType.UNIT_LENGTH);
  if(length==null||!"mm".equalsIgnoreCase(length.GetName()))throw new IllegalStateException("原生长度单位未明确为mm，不猜单位。");
  SelectionBuffer buffer=session.GetCurrentSelectionBuffer();Selections selected=buffer==null?null:buffer.GetContents();
  if(selected==null||selected.getarraysize()!=1)throw new IllegalStateException("需要预选且只预选一个平面作为驱动面；本次未写模型。");
  Selection selection=selected.get(0);if(selection==null||!same(selection.GetSelModel(),s.model)||selection.GetPath()!=null||!(selection.GetSelItem() instanceof Surface face))throw new IllegalStateException("需要当前零件本体上的单个曲面，不接受装配路径/其他模型。");
  s.face=face;requireFace(s);s.stamp=s.model.GetVersionStamp();s.dirty=s.model.GetIsModified();
  if(s.stamp==null||s.stamp.isBlank())throw new IllegalStateException("模型版本上下文不可读，未写模型。");
  s.featureIds=featureIds(s.model);requireNoNativeFailure(s.solid);
  control.record("PREFLIGHT",map("model",TARGET,"nativeIsSheetmetal",false,"bodyId",s.body.GetId(),"drivingFaceId",s.face.GetId(),"drivingFaceRequired",true,"lengthUnit","mm","globalThicknessProven",false,"modelVersionStamp",s.stamp,"dirtyBefore",s.dirty,"featureCountBefore",s.featureIds.size()));return s;
 }
 private static void requireFace(Snapshot s)throws Exception{
  if(s.face.GetSurfaceType()!=SurfaceType.SURFACE_PLANE||!s.face.GetIsVisible())throw new IllegalStateException("驱动面必须是当前实体体的可见平面。");
  double area=s.face.EvalArea();if(!Double.isFinite(area)||area<=0)throw new IllegalStateException("驱动面面积无效。");
  Surfaces faces=s.body.ListSurfaces();if(faces==null||faces.getarraysize()>10000)throw new IllegalStateException("体曲面列表未知/超过界限。");
  for(int i=0;i<faces.getarraysize();i++){Surface candidate=faces.get(i);if(candidate!=null&&candidate.GetId()==s.face.GetId()&&same(candidate,s.face))return;}
  throw new IllegalStateException("驱动面未通过单体原生列表及对象身份匹配。");
 }
 private static Set<Integer> featureIds(Model model)throws Exception{
  ModelItems items=model.ListItems(ModelItemType.ITEM_FEATURE);if(items==null||items.getarraysize()>10000)throw new IllegalStateException("Feature list unavailable/too large");Set<Integer> ids=new LinkedHashSet<>();
  for(int i=0;i<items.getarraysize();i++){ModelItem item=items.get(i);if(!(item instanceof Feature)||!ids.add(item.GetId()))throw new IllegalStateException("Invalid/duplicate feature identity");}return ids;
 }
 private static void requireNoNativeFailure(WSolid solid)throws Exception{
  FeatureStatusFlags flags=solid.GetSolidFeatureStatusFlags();if(flags==null||flags.getarraysize()>10000)throw new IllegalStateException("Feature error flags unknown/too large");
  for(int i=0;i<flags.getarraysize();i++){FeatureStatusFlag flag=flags.get(i);if(flag==null||flag.GetIsFailed()||flag.GetIsInvalid()||flag.GetIsUnregenerated()||flag.GetIsChildOfFailed()||flag.GetIsChildOfExternalFailed())throw new IllegalStateException("Native feature failed/invalid/unregenerated flag present");}
 }
 private static void recheck(Snapshot s)throws Exception{
  if(!active||!same(session.GetActiveModel(),s.model)||!TARGET.equalsIgnoreCase(s.model.GetFileName())||!Objects.equals(s.stamp,s.model.GetVersionStamp())||s.dirty!=s.model.GetIsModified())throw new IllegalStateException("确认期间激活模型/版本/dirty变化，未写模型。");
  SolidBody current=s.solid.GetDefaultBody();if(!same(current,s.body)||current.IsSheetmetal())throw new IllegalStateException("确认期间默认体变化，未执行。");
  ModelItems bodies=s.model.ListItems(ModelItemType.ITEM_BODY);if(bodies==null||bodies.getarraysize()!=1||!same(bodies.get(0),current))throw new IllegalStateException("实体体上下文变化。");
  UnitSystem units=s.solid.GetPrincipalUnits();Unit unit=units==null?null:units.GetUnit(UnitType.UNIT_LENGTH);if(unit==null||!"mm".equalsIgnoreCase(unit.GetName()))throw new IllegalStateException("长度单位变化。");
  if(!s.featureIds.equals(featureIds(s.model)))throw new IllegalStateException("确认期间特征列表变化。");requireFace(s);requireNoNativeFailure(s.solid);
 }
 private static void element(Elements elements,int id,ArgValue value,int level)throws Exception{elements.append(wfcElementTree.Element_Create(id,value,level));}
 private static String execute(Step13Control control)throws Exception{
  Snapshot s=inspect(control);
  if(!confirm("第一次确认：当前零件200002100.prt；原生默认体IsSheetmetal=false。\n全局壁厚尚未证明，局部1.2mm不是全局厚度依据。\n仅对可丢弃试验模型执行一次转换，不保存、不覆盖原文件；失败可能保留部分改变，不自动回滚。\n你确认当前模型可丢弃并同意继续准备本次试验吗？")){control.finish("CANCELLED_FIRST_CONFIRMATION_NO_WRITE");return "已取消第一次确认，未写模型；本次点击已消耗，不重试。";}
  control.acceptRisk();session.UIDisplayMessage(MESSAGES,"P1_S13_THICKNESS",null);String input=session.UIReadStringMessage(Boolean.FALSE);
  if(input==null||input.isBlank()){control.finish("CANCELLED_THICKNESS_INPUT_NO_WRITE");return "未输入试验厚度，未写模型；本次点击已消耗。";}
  control.setThickness(input);recheck(s);
  if(!confirm("第二次最终确认：原生IsSheetmetal=false；全局壁厚未证明。\n可丢弃模型：200002100.prt；预选驱动面ID="+s.face.GetId()+"。\n本次仅使用你明确输入的试验厚度 "+control.thickness()+" mm，不视为真实全局壁厚。\n确认后最多调用一次WCreateFeature与一次WRegenerate；不保存。失败保留状态，禁止保存/重试。\n现在提交这唯一一次原生转换吗？")){control.finish("CANCELLED_FINAL_CONFIRMATION_NO_WRITE");return "已取消最终确认，未写模型；本次点击已消耗。";}
  control.acceptFinal();recheck(s);
  Selection body=pfcSelect.CreateModelItemSelection(s.body,null),face=pfcSelect.CreateModelItemSelection(s.face,null);if(body==null||face==null)throw new IllegalStateException("Native selections unavailable");
  Elements elements=Elements.create();element(elements,wfcElemIds.PRO_E_FEATURE_TREE,null,0);
  element(elements,wfcElemIds.PRO_E_FEATURE_TYPE,pfcArgument.CreateIntArgValue(FeatureType.FEATTYPE_DATUM_SURFACE.getValue()),1);
  element(elements,wfcElemIds.PRO_E_STD_FEATURE_NAME,pfcArgument.CreateStringArgValue("P1_STEP13_DRIVING"),1);
  element(elements,wfcElemIds.PRO_E_DTM_SRF_TYPE,pfcArgument.CreateIntArgValue(15),1);
  element(elements,wfcElemIds.PRO_E_SMT_CONV_BODY,pfcArgument.CreateSelectionArgValue(body),1);
  element(elements,wfcElemIds.PRO_E_SMT_DRV_SURF,pfcArgument.CreateSelectionArgValue(face),1);
  element(elements,wfcElemIds.PRO_E_STD_SMT_THICKNESS,pfcArgument.CreateDoubleArgValue(control.thickness()),1);
  ElementTree tree=s.wsession.CreateElementTree(elements);if(tree==null)throw new IllegalStateException("Element tree unavailable");
  FeatCreateOptions options=FeatCreateOptions.create();options.append(FeatCreateOption.FEAT_CR_NO_OPTS);
  WRegenInstructions regen=wfcSolidInstructions.WRegenInstructions_Create();regen.SetNoResolveMode(true);regen.SetAllowConfirm(false);
  recheck(s);control.markDispatch();
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
 private static void readonlyFailureSnapshot(Step13Control control){
  try{Model current=session.GetActiveModel();if(current==null||!TARGET.equalsIgnoreCase(current.GetFileName())||!(current instanceof Solid solid)){control.record("FAILURE_MODEL_READBACK",map("state","UNAVAILABLE_OR_CHANGED"));return;}
   SolidBody body=solid.GetDefaultBody();control.record("FAILURE_MODEL_READBACK",map("model",TARGET,"nativeDefaultBodyIsSheetmetal",body==null?"UNKNOWN":body.IsSheetmetal(),"dirty",current.GetIsModified(),"versionStamp",known(current.GetVersionStamp()),"noRollbackAttempted",true,"noSave",true));
  }catch(Exception|LinkageError readError){System.err.println("[P1_STEP13] FAILURE_READBACK_UNAVAILABLE "+readError.getClass().getName());}
 }
}
