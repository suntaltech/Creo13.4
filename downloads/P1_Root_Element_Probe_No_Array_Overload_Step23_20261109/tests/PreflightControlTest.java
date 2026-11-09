import p1.controlled.step23.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class PreflightControlTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 interface Throwing{void run()throws Exception;}
 static void rejects(Class<? extends Throwable>type,Throwing action)throws Exception{checks++;try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError(e);}throw new AssertionError("missing failure");}
 public static void main(String[]args)throws Exception{
  Path root=Files.createTempDirectory("p1-preflight-pure-");
  try{
   for(String raw:new String[]{"","0","-1","NaN","Infinity","1e999","1e-999","abc"})rejects(IllegalArgumentException.class,()->Step23Control.parseThickness(raw));
   check(Step23Control.parseThickness("2.5")==2.5);
   Path run=root.resolve("normal");
   try(Step23Control c=new Step23Control(run)){rejects(IllegalStateException.class,()->c.setThickness("2"));PreflightAudit trace=new PreflightAudit(c);
    check(trace.read("TEST_START","TEST_DONE","PURE_VALUE",()->42,value->AuditSupport.map("value",value))==42);
    c.acceptRisk();c.setThickness("2.5");c.finish("PURE_DIAGNOSTIC_COMPLETE_NO_MODEL_CALLS");rejects(IllegalStateException.class,c::acceptRisk);
   }
   rejects(FileAlreadyExistsException.class,()->new Step23Control(run));
   List<String> normal=Files.readAllLines(run.resolve(Step23Control.FILE_NAME));check(normal.size()==6);
   for(String row:normal)System.out.println("TEST_JSON="+row);
   Path failed=root.resolve("failed");AtomicInteger invoked=new AtomicInteger();
   try(Step23Control c=new Step23Control(failed)){PreflightAudit trace=new PreflightAudit(c);rejects(PreflightAudit.ProbeFailure.class,()->trace.read("FAIL_START","FAIL_DONE","PURE_THROW",()->{invoked.incrementAndGet();throw new IllegalStateException("quote\" failure");},value->AuditSupport.map()));check(invoked.get()==1);}
   List<String> error=Files.readAllLines(failed.resolve(Step23Control.FILE_NAME));check(error.size()==3);check(error.get(1).contains("FAIL_START"));check(error.get(2).contains("PREFLIGHT_PROBE_EXCEPTION"));check(error.stream().noneMatch(x->x.contains("FAIL_DONE")));
   Path closed=root.resolve("closed");Step23Control c=new Step23Control(closed);c.close();PreflightAudit trace=new PreflightAudit(c);AtomicInteger afterFailure=new AtomicInteger();
   rejects(java.io.IOException.class,()->trace.read("UNWRITTEN_START","NEVER_DONE","MUST_NOT_RUN",()->afterFailure.incrementAndGet(),value->AuditSupport.map("value",value)));check(afterFailure.get()==0);
   try(Step23Control cancelled=new Step23Control(root.resolve("cancel"))){cancelled.finish("CANCELLED_FIRST_CONFIRMATION_READONLY");check(cancelled.state()==Step23Control.State.FINISHED);}
   check(error.get(2).contains("java.lang.IllegalStateException"));
   Path scoped=root.resolve("scoped");
   try(Step23Control scopedControl=new Step23Control(scoped)){
    PreflightAudit scopedTrace=new PreflightAudit(scopedControl);Map<String,Object> context=AuditSupport.map("featureId",2708,"featureIndex",4,"featureIdKnown",true);
    check(scopedTrace.read("SCOPED_START","SCOPED_DONE","PURE_SCOPED_READ",context,()->{
     String before=Files.readAllLines(scoped.resolve(Step23Control.FILE_NAME)).get(1);
     check(before.contains("SCOPED_START")&&before.contains("\"featureId\":2708"));return 7;
    },value->AuditSupport.map("value",value))==7);
    rejects(PreflightAudit.ProbeFailure.class,()->scopedTrace.read("SCOPED_FAIL_START","SCOPED_FAIL_DONE","PURE_SCOPED_THROW",context,()->{throw new IllegalArgumentException("scoped exception");},value->AuditSupport.map()));
   }
   List<String> scopedRows=Files.readAllLines(scoped.resolve(Step23Control.FILE_NAME));
   check(scopedRows.size()==5);
   check(scopedRows.get(2).contains("\"featureId\":2708")&&scopedRows.get(2).contains("SCOPED_DONE"));
   check(scopedRows.get(4).contains("\"featureId\":2708")&&scopedRows.get(4).contains("java.lang.IllegalArgumentException"));
   for(String row:scopedRows)System.out.println("SCOPED_TEST_JSON="+row);

  }finally{try(var paths=Files.walk(root)){for(Path f:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
  System.out.println("PREFLIGHT_PURE_CHECKS_PASSED="+checks);
 }
}
