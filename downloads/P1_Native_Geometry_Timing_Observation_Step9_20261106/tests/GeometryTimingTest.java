import p1.readonly.geometry.step9.*;
import java.nio.file.*;
import java.util.*;
public final class GeometryTimingTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static long n(Map<?,?> m,String k){return ((Number)m.get(k)).longValue();}
 public static void main(String[] args)throws Exception{
  GeometryTimingCounters c=new GeometryTimingCounters();Map<String,Long> before=c.marks();
  c.read("P","Surface[12].GetId",100,"RETURNED",7);
  c.read("P","Surface[98].GetId",200,"RETURNED_NULL",7);
  c.read("P","Surface[99].GetId",-3,"THREW",8);
  check(n(c.marks(),"readActionGroups")==3);check(n(c.marks(),"readActionNs")==300);
  check(n(c.marks(),"readNullGroups")==1);check(n(c.marks(),"readExceptionGroups")==1);
  Map<String,Object> report=c.report();List<?> groups=(List<?>)report.get("actionGroups");check(groups.size()==1);
  check(n((Map<?,?>)groups.get(0),"maxActionNs")==200);check(Boolean.TRUE.equals(report.get("differentReadThreadObserved")));
  check(n(report,"negativeDurationSamples")==1);check(n(c.delta(before),"readActionGroups")==3);
  for(int i=0;i<300;i++)c.read("P","unique"+i,1,"RETURNED",7);
  report=c.report();groups=(List<?>)report.get("actionGroups");check(groups.size()==257);
  long sum=0;for(Object g:groups)sum+=n((Map<?,?>)g,"actionGroups");check(sum==303);check(n(report,"actionGroupsOverflowed")==45);
  before=c.marks();
  for(int i=0;i<20;i++)c.output("EVENT","start","end",2,1,1,1,1,3,4,5,6,20_000_000,i%2==0);
  check(n(c.delta(before),"businessOutputAttempts")==20);check(n(c.marks(),"businessOutputCompleted")==10);check(n(c.marks(),"businessOutputFailed")==10);
  check(n(c.marks(),"writerFlushCalls")==20);check(n(c.marks(),"headerAndSerializationNs")==40);check(n(c.marks(),"observedOutputNs")==400_000_000);
  check(((List<?>)c.report().get("slowOutputSamples")).size()==16);
  Path directory=Files.createTempDirectory("p1-timing-test-");
  try{
   GeometryTimingJournal j=new GeometryTimingJournal("TEST",directory);String file=j.fileName();
   j.record("OPEN",null,GeometryEvidenceSupport.map("escaped","quote\" newline\n 中文"));
   j.record("COMMAND","request",GeometryEvidenceSupport.map("ok",true));
   check(n(j.state(),"journalWriteAttemptsBeforeThisRecord")==2);check(n(j.state(),"journalWriteErrors")==0);j.close();
   List<String> lines=Files.readAllLines(directory.resolve(file));check(lines.size()==2);for(String line:lines)System.out.println("JOURNAL_JSON="+line);
   j.record("AFTER_CLOSE","request",GeometryEvidenceSupport.map());check(Boolean.TRUE.equals(j.state().get("closed")));
   Path blocker=directory.resolve("regular-file");Files.writeString(blocker,"blocked");
   try(GeometryTimingJournal failed=new GeometryTimingJournal("TEST",blocker)){
    failed.record("FALLBACK",null,GeometryEvidenceSupport.map());check(n(failed.state(),"journalWriteErrors")>=1);
    check("STDERR_FALLBACK".equals(failed.state().get("timingSinkState")));
   }
   try(GeometryTimingJournal other=new GeometryTimingJournal("TEST",directory)){check(!other.fileName().equals(file));}
  }finally{try(var paths=Files.walk(directory)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
  System.out.println("TIMING_PURE_CHECKS_PASSED="+checks);
 }
}
