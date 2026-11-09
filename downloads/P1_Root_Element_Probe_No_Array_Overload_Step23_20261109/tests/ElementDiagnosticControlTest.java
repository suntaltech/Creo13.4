import p1.controlled.step23.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Actual file I/O and pure Java actions only; no SDK/model objects or substitutes. */
public final class ElementDiagnosticControlTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check="+checks);}
 interface Throwing{void run()throws Exception;}
 static void rejects(Class<? extends Throwable> type,Throwing action)throws Exception{checks++;try{action.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError(e);}throw new AssertionError("missing failure");}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("step23-pure-");
  try{
   Path run=root.resolve("states");
   try(Step23Control c=new Step23Control(run)){
    rejects(IllegalStateException.class,c::acceptFinal);c.acceptRisk();rejects(IllegalStateException.class,c::acceptFinal);c.setThickness("1.2");c.acceptFinal();check(c.state()==Step23Control.State.FINAL_ACCEPTED);rejects(IllegalStateException.class,c::acceptFinal);c.finish("PURE_CONTROL_ONLY_NO_MODEL");check(c.state()==Step23Control.State.FINISHED);rejects(IllegalStateException.class,c::acceptFinal);
   }
   rejects(FileAlreadyExistsException.class,()->new Step23Control(run));
   for(String row:Files.readAllLines(run.resolve(Step23Control.FILE_NAME)))System.out.println("ELEMENT_CONTROL_TEST_JSON="+row);
   Path traceDir=root.resolve("context");AtomicInteger calls=new AtomicInteger();
   try(Step23Control c=new Step23Control(traceDir)){
    PreflightAudit trace=new PreflightAudit(c);Map<String,Object> context=AuditSupport.map("elementIndex",3,"elementId",101,"level",1,"argumentKind","PURE_VALUE");
    check(trace.read("PURE_ELEMENT_START","PURE_ELEMENT_DONE","PURE_ELEMENT_ACTION",context,()->{
     String before=Files.readAllLines(traceDir.resolve(Step23Control.FILE_NAME)).get(1);
     check(before.contains("PURE_ELEMENT_START")&&before.contains("\"elementId\":101")&&before.contains("\"level\":1"));return 17;
    },value->AuditSupport.map("value",value))==17);
    rejects(PreflightAudit.ProbeFailure.class,()->trace.read("PURE_ELEMENT_FAIL_START","PURE_ELEMENT_FAIL_DONE","PURE_ELEMENT_THROW",context,()->{calls.incrementAndGet();throw new IllegalArgumentException("pure failure");},value->AuditSupport.map()));check(calls.get()==1);
   }
   List<String> rows=Files.readAllLines(traceDir.resolve(Step23Control.FILE_NAME));check(rows.size()==5);check(rows.get(4).contains("\"elementId\":101")&&rows.get(4).contains("java.lang.IllegalArgumentException"));
   for(String row:rows)System.out.println("ELEMENT_TRACE_TEST_JSON="+row);
  }finally{try(var paths=Files.walk(root)){for(Path f:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
  System.out.println("ELEMENT_DIAGNOSTIC_CHECKS_PASSED="+checks);
 }
}
