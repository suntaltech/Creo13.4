package p1.readonly.geometry.step9;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Synchronous best-effort timing journal, independent from the business JSONL. No SDK calls or new threads. */
public final class GeometryTimingJournal implements AutoCloseable {
 private static final AtomicLong NEXT=new AtomicLong();
 private final long openedNs=System.nanoTime();
 private final String traceId="T9-"+Long.toHexString(openedNs)+"-"+NEXT.incrementAndGet();
 private final String commandId;
 private BufferedWriter writer;
 private String fileName="UNKNOWN_FILE_UNAVAILABLE",openError="NONE";
 private boolean closed;
 private long sequence,writeAttempts,writeErrors,writeNs,openNs,closeNs;
 public GeometryTimingJournal(String commandId){this(commandId,null);}
 public GeometryTimingJournal(String commandId,Path overrideDirectory){this.commandId=commandId;long begin=System.nanoTime();try{
  String configured=System.getenv("P1_READONLY_GEOMETRY_LOG_DIR");Path directory=overrideDirectory!=null?overrideDirectory:configured==null||configured.isBlank()?Paths.get(System.getProperty("user.home"),"CreoReadOnlyGeometry"):Paths.get(configured);
  Files.createDirectories(directory);Path file=directory.resolve("geometry-timing-step9-lifecycle-"+traceId+".jsonl");fileName=file.getFileName().toString();
  writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
 }catch(IOException|RuntimeException e){openError=e.getClass().getName();writeErrors++;}finally{openNs=System.nanoTime()-begin;}}
 public String traceId(){return traceId;}public String fileName(){return fileName;}
 public synchronized void record(String event,String requestId,Map<String,Object> fields){
  long begin=System.nanoTime();writeAttempts++;
  try{Map<String,Object> row=GeometryEvidenceSupport.map("schema","P1_NATIVE_GEOMETRY_TIMING_LIFECYCLE_V1","event",event,"collectorStep","STEP_9","commandId",commandId,
    "timingTraceId",traceId,"sequence",++sequence,"timestamp",Instant.now().toString(),"monotonicNs",begin,"journalElapsedNs",begin-openedNs,
    "requestId",requestId==null?"NO_COMMAND_REQUEST_YET":requestId,"javaThreadId",Thread.currentThread().getId(),"javaThreadName",Thread.currentThread().getName(),
    "timingSinkState",writer!=null&&!closed?"FILE":"STDERR_FALLBACK","journalOpenFailure",openError);
   row.putAll(fields);String line=GeometryEvidenceSupport.json(row);
   if(writer!=null&&!closed){writer.write(line);writer.newLine();writer.flush();}else System.err.println("[P1GEO_TIMING] "+line);
  }catch(IOException|RuntimeException e){writeErrors++;try{System.err.println("[P1GEO_TIMING] TIMING_WRITE_FAILED event="+event+" exception="+e.getClass().getName()+" trace="+traceId);}catch(RuntimeException ignored){/* Do not change native/business control flow for a timing failure. */}}
  finally{writeNs+=Math.max(0,System.nanoTime()-begin);}
 }
 public Map<String,Object> state(){return GeometryEvidenceSupport.map("timingTraceId",traceId,"lifecycleFileName",fileName,"journalWriteAttemptsBeforeThisRecord",writeAttempts,"journalWriteErrors",writeErrors,
  "journalOpenNs",openNs,"journalObservedWriteNsBeforeThisRecord",writeNs,"journalCloseNs",closeNs,"closed",closed,"timingSinkState",writer!=null&&!closed?"FILE":"STDERR_FALLBACK","recordOwnWriteExcludedFromState",true);}
 @Override public synchronized void close(){long begin=System.nanoTime();try{if(writer!=null)writer.close();}catch(IOException|RuntimeException e){writeErrors++;}finally{closed=true;closeNs=System.nanoTime()-begin;}}
}
