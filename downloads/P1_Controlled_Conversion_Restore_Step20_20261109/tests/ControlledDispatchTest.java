import p1.controlled.step20.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Exercises Java state/audit only. No SDK classpath, models, native calls or model substitutes. */
public final class ControlledDispatchTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check="+checks);}
 interface Throwing{void run()throws Exception;}
 static void rejects(Class<? extends Throwable> type,Throwing action)throws Exception{checks++;try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError(e);}throw new AssertionError("missing failure");}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("step20-control-pure-");
  try{
   Path run=root.resolve("states");
   try(Step20Control c=new Step20Control(run)){
    check(!c.dispatched());rejects(IllegalStateException.class,c::acceptFinal);rejects(IllegalStateException.class,c::markDispatch);rejects(IllegalStateException.class,c::beginCreate);rejects(IllegalStateException.class,c::beginRegenerate);
    c.acceptRisk();rejects(IllegalStateException.class,c::acceptFinal);c.setThickness("1.2");check(c.thickness()==1.2);rejects(IllegalStateException.class,c::markDispatch);
    c.acceptFinal();rejects(IllegalStateException.class,c::acceptFinal);c.markDispatch();check(c.dispatched());rejects(IllegalStateException.class,c::markDispatch);
    rejects(IllegalStateException.class,c::beginRegenerate);c.beginCreate();rejects(IllegalStateException.class,c::beginCreate);rejects(IllegalStateException.class,c::beginRegenerate);
    c.createReturned();rejects(IllegalStateException.class,c::createReturned);c.beginRegenerate();rejects(IllegalStateException.class,c::beginRegenerate);c.regenerateReturned();rejects(IllegalStateException.class,c::regenerateReturned);c.finish("PURE_STATE_MACHINE_ONLY");rejects(IllegalStateException.class,c::beginCreate);rejects(IllegalStateException.class,c::beginRegenerate);
   }
   rejects(FileAlreadyExistsException.class,()->new Step20Control(run));
   List<String> rows=Files.readAllLines(run.resolve(Step20Control.FILE_NAME));check(rows.get(rows.size()-1).contains("\"writeDispatchIntentRecorded\":true"));
   for(String row:rows)System.out.println("DISPATCH_TEST_JSON="+row);
   Path traceDir=root.resolve("audit");AtomicInteger calls=new AtomicInteger();
   try(Step20Control c=new Step20Control(traceDir)){
    PreflightAudit trace=new PreflightAudit(c);
    check(trace.write("PURE_WRITE_START","PURE_WRITE_RETURNED","PURE_JAVA_ACTION",()->{
     List<String> before=Files.readAllLines(traceDir.resolve(Step20Control.FILE_NAME));check(before.get(1).contains("\"writeModel\":true")&&before.get(1).contains("PURE_WRITE_START"));return 3;
    },value->AuditSupport.map("value",value))==3);
    rejects(PreflightAudit.ProbeFailure.class,()->trace.write("PURE_THROW_START","PURE_NEVER_RETURNED","PURE_JAVA_THROW",()->{calls.incrementAndGet();throw new IllegalStateException("pure exception");},value->AuditSupport.map()));check(calls.get()==1);
   }
   List<String> audit=Files.readAllLines(traceDir.resolve(Step20Control.FILE_NAME));check(audit.size()==5);check(audit.get(4).contains("NATIVE_OPERATION_EXCEPTION")&&audit.get(4).contains("\"writeModel\":true"));
   for(String row:audit)System.out.println("WRITE_TRACE_TEST_JSON="+row);
   Step20Control closed=new Step20Control(root.resolve("closed"));closed.close();PreflightAudit trace=new PreflightAudit(closed);AtomicInteger blocked=new AtomicInteger();
   rejects(java.io.IOException.class,()->trace.write("UNWRITTEN","NEVER","PURE_NEVER",()->blocked.incrementAndGet(),value->AuditSupport.map()));check(blocked.get()==0);
  }finally{try(var paths=Files.walk(root)){for(Path f:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
  System.out.println("CONTROLLED_DISPATCH_CHECKS_PASSED="+checks);
 }
}
