package p1.readonly.body.step14;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ptc.cipjava.jxobject_i;
import com.ptc.pfc.pfcSession.*;
import com.ptc.pfc.pfcCommand.*;
import com.ptc.pfc.pfcModel.*;
import com.ptc.pfc.pfcModelItem.*;
import com.ptc.pfc.pfcSolid.*;
import com.ptc.pfc.pfcUnits.*;
import com.ptc.pfc.pfcBase.UnitType;
import com.ptc.wfc.wfcSolid.WSolid;
import static p1.readonly.body.step14.DiagnosticSupport.*;

/** Separate repeatable read-only command. Never opens a conversion ledger or invokes feature writes. */
public final class P1DefaultBodyDiagnosticStep14 {
 public static final String COMMAND_ID="SISWDefaultBodyDiagnosticStep14";
 private static final String MESSAGES="p1_default_body_step14.txt",TARGET="200002100.prt";
 private static final int MAX_BODIES=128;
 private static Session session;
 private static UICommand command;
 private static Listener listener;
 private static volatile boolean active;
 private static final AtomicBoolean busy=new AtomicBoolean();
 private P1DefaultBodyDiagnosticStep14(){}
 public static synchronized void start()throws Exception{
  if(active)return;if(command!=null||listener!=null)throw new IllegalStateException("Previous registration may remain attached");
  session=pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);if(session==null)throw new IllegalStateException("No synchronous session");listener=new Listener();
  try{command=session.UICreateCommand(COMMAND_ID,listener);if(command==null)throw new IllegalStateException("No command");command.Designate(MESSAGES,"P1_S14_LABEL","P1_S14_HELP","P1_S14_DESC");active=true;}
  catch(Exception|LinkageError error){if(command!=null){try{command.RemoveActionListener(listener);command=null;listener=null;}catch(Exception|LinkageError detach){error.addSuppressed(detach);}}else listener=null;session=null;throw error;}
 }
 public static synchronized void stop()throws Exception{
  active=false;if(busy.get())throw new IllegalStateException("Diagnostic running; do not unload");if(command!=null&&listener!=null)command.RemoveActionListener(listener);command=null;listener=null;session=null;
 }
 private static void status(String key){try{session.UIDisplayMessage(MESSAGES,key,null);}catch(Exception|LinkageError error){System.err.println("[P1_S14] UI_STATUS_FAILED "+error.getClass().getName());}}
 private static final class Listener extends DefaultUICommandActionListener {
  @Override public void OnCommand(){
   if(!active||session==null)return;if(!busy.compareAndSet(false,true)){status("P1_S14_BUSY");return;}
   try(Run run=new Run()){status("P1_S14_STARTED");run.collect();status(run.resultKey);}
   catch(Exception|LinkageError error){error.printStackTrace(System.err);status("P1_S14_FAILED");}finally{busy.set(false);}
  }
 }
 @FunctionalInterface private interface Read<T>{T get()throws Exception;}
 private static final class Run implements AutoCloseable {
  final String requestId=UUID.randomUUID().toString();final long began=System.nanoTime();final BufferedWriter writer;long sequence;int failures,unavailable;String resultKey="P1_S14_FAILED";
  Run()throws IOException{Path directory=Paths.get(System.getProperty("user.home"),"CreoDefaultBodyDiagnosticsStep14");Files.createDirectories(directory);Path file=directory.resolve("default-body-step14-"+requestId+".jsonl");writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);System.err.println("[P1_S14] EVIDENCE_FILE="+file);}
  void emit(String event,Map<String,Object> fields)throws IOException{Map<String,Object> row=map("schema","P1_STEP14_DEFAULT_BODY_READONLY_V1","step","STEP_14","commandId",COMMAND_ID,"requestId",requestId,"sequence",++sequence,"timestamp",Instant.now().toString(),"elapsedNs",System.nanoTime()-began,"javaThreadId",Thread.currentThread().getId(),"event",event);row.putAll(fields);writer.write(json(row));writer.newLine();writer.flush();}
  Map<String,Object> unavailable(String reason){unavailable++;return map("status",reason,"value",null,"nativeCallAttempted",false);}
  <T> Map<String,Object> probe(String operation,Read<T> read){
   long start=System.nanoTime();try{T value=read.get();return map("status",value==null?"RETURNED_NULL":"RETURNED","value",value,"operation",operation,"elapsedNs",System.nanoTime()-start);}
   catch(Exception|LinkageError error){failures++;return map("status","THREW","value",null,"operation",operation,"elapsedNs",System.nanoTime()-start,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage()),"nativeCallAttempted",true);}
  }
  Map<String,Object> body(ModelItem item,String objectState){
   if(item==null)return map("objectState",objectState,"present",objectState.equals("RETURNED_NULL")?Boolean.FALSE:null,"javaClass",null,"id",unavailable("NO_OBJECT"),"pointer",unavailable("NO_OBJECT"),"isSheetmetal",unavailable("NO_OBJECT"));
   Map<String,Object> id=probe("ModelItem.GetId",item::GetId),pointer;
   if(item instanceof jxobject_i nativeItem){pointer=probe("jxobject_i.getptr_WRAPPER_HANDLE_NOT_CERTIFIED_ID",()->Long.toUnsignedString(nativeItem.getptr(),16));if("RETURNED".equals(pointer.get("status"))){String value=(String)pointer.get("value");pointer.put("value","0x"+value);if(value.equals("0"))pointer.put("status","RETURNED_ZERO");}}
   else pointer=unavailable("JAVA_POINTER_INTERFACE_NOT_IMPLEMENTED");
   Map<String,Object> sheet;
   if(item instanceof SolidBody nativeBody)sheet=probe("SolidBody.IsSheetmetal",nativeBody::IsSheetmetal);
   else{sheet=unavailable("JAVA_SOLIDBODY_INTERFACE_NOT_IMPLEMENTED");sheet.put("diagnosticExceptionClass","java.lang.UnsupportedOperationException");sheet.put("exceptionIsNativeSdkThrow",false);}
   return map("objectState","RETURNED","present",true,"javaClass",item.getClass().getName(),"hasSolidBodyInterface",item instanceof SolidBody,"id",id,"pointer",pointer,"isSheetmetal",sheet);
  }
  WSolid wfcView(Model model)throws IOException{
   if(model instanceof WSolid existing){emit("WFC_VIEW",map("status","ALREADY_PUBLIC_INTERFACE","internalContractCertified",false));return existing;}
   if(!(model instanceof jxobject_i nativeModel)){unavailable++;emit("WFC_VIEW",map("status","JAVA_NATIVE_POINTER_INTERFACE_UNAVAILABLE"));return null;}
   long pointer;try{pointer=nativeModel.getptr();}catch(Exception|LinkageError error){failures++;emit("WFC_VIEW",map("status","POINTER_READ_THROWN","exceptionClass",error.getClass().getName()));return null;}
   if(pointer==0){unavailable++;emit("WFC_VIEW",map("status","ZERO_MODEL_WRAPPER_HANDLE"));return null;}
   for(String className:new String[]{"com.ptc.wfc.Implementation.wfcPart$WPart","com.ptc.wfc.Implementation.wfcSolid$WSolid"}){
    try{var type=Class.forName(className);var ctor=type.getDeclaredConstructor(long.class);ctor.setAccessible(true);Object wrapper=ctor.newInstance(pointer);
     if(!(wrapper instanceof WSolid solid)||!(wrapper instanceof jxobject_i identity)||identity.getptr()!=pointer)throw new IllegalStateException("View type/pointer mismatch");
     emit("WFC_VIEW",map("status","ACQUIRED_INTERNAL_ADAPTER","adapterClass",className,"matchesStep13ViewConstruction",true,"internalContractCertified",false));return solid;
    }catch(ReflectiveOperationException|RuntimeException|LinkageError error){failures++;emit("WFC_VIEW",map("status","ADAPTER_FAILED","adapterClass",className,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}
   }return null;
  }
  Map<String,Object> context(Model model){return map("fileName",probe("Model.GetFileName",model::GetFileName),"typeValue",probe("Model.GetType",()->model.GetType().getValue()),"versionStamp",probe("Model.GetVersionStamp",model::GetVersionStamp),"isModified",probe("Model.GetIsModified",model::GetIsModified));}
  void collect()throws Exception{
   emit("DIAGNOSTIC_STARTED",map("repeatable",true,"modelApiPolicy","READ_ONLY","conversionDecisionMade",false,"conversionLockAccessed",false));
   Model model=null;Map<String,Object> before=null;boolean eligible=false;
   try{
    try{model=session.GetActiveModel();}catch(Exception|LinkageError error){failures++;emit("ACTIVE_MODEL",map("status","THREW","exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));return;}
    if(model==null){unavailable++;emit("ACTIVE_MODEL",map("status","RETURNED_NULL"));return;}
    before=context(model);emit("MODEL_BEFORE",before);
    if(model.GetType()!=ModelType.MDL_PART||!TARGET.equalsIgnoreCase(model.GetFileName())){emit("MODEL_GUARD",map("status","BLOCKED","requiredModel",TARGET));return;}eligible=true;
    ModelItems items=null;String listState="RETURNED_NULL";Integer count=null;
    try{items=model.ListItems(ModelItemType.ITEM_BODY);listState=items==null?"RETURNED_NULL":"RETURNED";if(items!=null)count=items.getarraysize();}
    catch(Exception|LinkageError error){listState="THREW";failures++;emit("BODY_ENUMERATION_ERROR",map("exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}
    emit("BODY_ENUMERATION",map("status",listState,"reportedCount",count,"limit",MAX_BODIES,"countIsUnknownNotZero",count==null));
    ModelItem first=null;String firstState=count!=null&&count==0?"NO_FIRST_ITEM_EMPTY_LIST":"NOT_AVAILABLE";
    if(count!=null&&count>0){try{first=items.get(0);firstState=first==null?"RETURNED_NULL":"RETURNED";}catch(Exception|LinkageError error){failures++;firstState="THREW";emit("BODY_FIRST_ITEM_ERROR",map("exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}}
    Map<String,Object> firstBody=body(first,firstState);
    if(count!=null){for(int i=0;i<Math.min(Math.max(count,0),MAX_BODIES);i++){
      if(i==0){emit("ENUMERATED_BODY",map("index",0,"metadata",firstBody));continue;}
      ModelItem item=null;String state="RETURNED_NULL";try{item=items.get(i);state=item==null?"RETURNED_NULL":"RETURNED";}catch(Exception|LinkageError error){failures++;state="THREW";emit("BODY_ITEM_ERROR",map("index",i,"exceptionClass",error.getClass().getName()));}
      emit("ENUMERATED_BODY",map("index",i,"metadata",body(item,state)));
     }if(count>MAX_BODIES){unavailable++;emit("BODY_ENUMERATION_LIMIT",map("reportedCount",count,"visited",MAX_BODIES,"complete",false));}}
    WSolid wfc=wfcView(model);Solid publicSolid=model instanceof Solid solid?solid:null;
    readDefault("STEP13_WFC_VIEW",wfc,first,firstBody);
    readDefault("PUBLIC_PFC_SOLID_VIEW",publicSolid,first,firstBody);
    Solid unitsOwner=publicSolid!=null?publicSolid:wfc;
    if(unitsOwner==null)emit("UNITS",map("status","NO_SOLID_INTERFACE","lengthUnit",null));
    else{Solid owner=unitsOwner;emit("UNITS",map("view",publicSolid!=null?"PUBLIC_PFC_SOLID_VIEW":"STEP13_WFC_VIEW","lengthUnitName",probe("Solid.GetPrincipalUnits/GetUnit/GetName",()->{UnitSystem system=owner.GetPrincipalUnits();Unit unit=system==null?null:system.GetUnit(UnitType.UNIT_LENGTH);return unit==null?null:unit.GetName();}),"lengthUnitExpression",probe("Solid.GetPrincipalUnits/GetUnit/GetExpression",()->{UnitSystem system=owner.GetPrincipalUnits();Unit unit=system==null?null:system.GetUnit(UnitType.UNIT_LENGTH);return unit==null?null:unit.GetExpression();})));}
   }catch(Exception|LinkageError error){failures++;emit("DIAGNOSTIC_ERROR",map("exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}
   finally{
    if(model!=null){emit("MODEL_AFTER_ORIGINAL",context(model));Map<String,Object> activeName=probe("Session.GetActiveModel/GetFileName.after",()->{Model current=session.GetActiveModel();return current==null?null:current.GetFileName();});emit("ACTIVE_MODEL_AFTER",map("fileName",activeName));}
    resultKey=!eligible?"P1_S14_BLOCKED":failures>0||unavailable>0?"P1_S14_PARTIAL":"P1_S14_ENDED";
    emit("DIAGNOSTIC_SUMMARY",map("outcome",!eligible?"BLOCKED_OR_NO_TARGET_MODEL":failures>0||unavailable>0?"PARTIAL_READ_DIAGNOSTICS":"READ_DIAGNOSTICS_RECORDED_ONLY","exceptions",failures,"unavailableProbes",unavailable,"elapsedMs",(System.nanoTime()-began)/1000000,"conversionDecisionMade",false,"modelWriteAbsenceIndependentlyProven",false,"conversionLockAccessed",false));
   }
  }
  void readDefault(String route,Solid solid,ModelItem first,Map<String,Object> firstBody)throws IOException{
   SolidBody defaultBody=null;String state="NOT_ATTEMPTED_NO_SOLID_VIEW";
   if(solid!=null){try{defaultBody=solid.GetDefaultBody();state=defaultBody==null?"RETURNED_NULL":"RETURNED";}
    catch(Exception|LinkageError error){failures++;state="THREW";emit("DEFAULT_BODY_ERROR",map("route",route,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}}
   Map<String,Object> snapshot=body(defaultBody,state);emit("DEFAULT_BODY",map("route",route,"metadata",snapshot));
   emit("FIRST_BODY_COMPARISON",map("route",route,"firstEnumeratedMetadata",firstBody,"defaultMetadata",snapshot,"comparison",BodyIdentity.compare(firstBody,snapshot,first==defaultBody),"doesNotAlterStep13Guard",true));
  }
  @Override public void close()throws IOException{writer.close();}
 }
}
