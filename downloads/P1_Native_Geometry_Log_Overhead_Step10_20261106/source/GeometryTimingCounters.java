package p1.readonly.geometry.step10;
import java.util.*;
import java.util.regex.Pattern;

/** Counters over synchronous observed actions; no SDK, thread creation or I/O. */
public final class GeometryTimingCounters {
 public static final int MAX_ACTION_KEYS=256,MAX_SLOW_OUTPUT_SAMPLES=16;
 public static final long SLOW_OUTPUT_SAMPLE_NS=10_000_000L;
 private static final Pattern BRACKETS=Pattern.compile("\\[[^\\]]*\\]");
 private final Map<String,Action> actions=new LinkedHashMap<>();
 private final List<Map<String,Object>> slowOutputs=new ArrayList<>();
 private long reads,readNs,nulls,failures,actionOverflow,negativeDurations;
 private long outputAttempts,outputCompleted,outputFailed,headerJsonNs,writeCalls,newlineCalls,flushCalls,stderrCalls;
 private long writeNs,flushNs,stderrNs,observerNs,totalOutputNs;
 private String firstOutputUtc,lastOutputUtc;
 private long firstReadThread=-1;private boolean differentReadThread;
 private static final class Action {
  long count,total,max,nulls,errors;
 }
 public static String normalize(String operation){return BRACKETS.matcher(operation==null?"UNKNOWN_OPERATION":operation).replaceAll("[]");}
 private long duration(long value){if(value<0){negativeDurations++;return 0;}return value;}
 public void read(String phase,String operation,long elapsedNs,String outcome,long threadId){
  long ns=duration(elapsedNs);reads++;readNs+=ns;if(outcome.equals("RETURNED_NULL"))nulls++;if(outcome.equals("THREW"))failures++;
  if(firstReadThread<0)firstReadThread=threadId;else if(threadId!=firstReadThread)differentReadThread=true;
  String key=(phase==null?"UNSCOPED":phase)+" :: "+normalize(operation);
  if(!actions.containsKey(key)&&actions.size()>=MAX_ACTION_KEYS){key="OVERFLOW_ACTION_GROUPS";actionOverflow++;}
  Action a=actions.computeIfAbsent(key,k->new Action());a.count++;a.total+=ns;a.max=Math.max(a.max,ns);if(outcome.equals("RETURNED_NULL"))a.nulls++;if(outcome.equals("THREW"))a.errors++;
 }
 public void output(String event,String startUtc,String endUtc,long headerNs,long writes,long newlines,long flushes,long stderrLines,long wNs,long fNs,long eNs,long oNs,long totalNs,boolean completed){
  outputAttempts++;if(completed)outputCompleted++;else outputFailed++;if(firstOutputUtc==null)firstOutputUtc=startUtc;lastOutputUtc=endUtc;
  headerJsonNs+=duration(headerNs);writeCalls+=writes;newlineCalls+=newlines;flushCalls+=flushes;stderrCalls+=stderrLines;
  writeNs+=duration(wNs);flushNs+=duration(fNs);stderrNs+=duration(eNs);observerNs+=duration(oNs);long total=duration(totalNs);totalOutputNs+=total;
  if(total>=SLOW_OUTPUT_SAMPLE_NS&&slowOutputs.size()<MAX_SLOW_OUTPUT_SAMPLES)slowOutputs.add(GeometryEvidenceSupport.map("event",event,"startUtc",startUtc,"endUtc",endUtc,
   "headerAndSerializationNs",headerNs,"writeNs",wNs,"flushNs",fNs,"stderrNs",eNs,"classificationObserverNs",oNs,"totalObservedOutputNs",total,"completed",completed));
 }
 public Map<String,Long> marks(){Map<String,Long> r=new LinkedHashMap<>();r.put("readActionGroups",reads);r.put("readActionNs",readNs);r.put("readNullGroups",nulls);r.put("readExceptionGroups",failures);
  r.put("businessOutputAttempts",outputAttempts);r.put("businessOutputCompleted",outputCompleted);r.put("businessOutputFailed",outputFailed);r.put("headerAndSerializationNs",headerJsonNs);
  r.put("writerWriteCalls",writeCalls);r.put("writerNewLineCalls",newlineCalls);r.put("writerFlushCalls",flushCalls);r.put("stderrPrintlnCalls",stderrCalls);r.put("writerWriteNs",writeNs);r.put("writerFlushNs",flushNs);r.put("stderrPrintlnNs",stderrNs);r.put("classificationObserverNs",observerNs);r.put("observedOutputNs",totalOutputNs);return r;}
 public Map<String,Object> delta(Map<String,Long> before){Map<String,Object> r=new LinkedHashMap<>();for(Map.Entry<String,Long> e:marks().entrySet())r.put(e.getKey(),e.getValue()-before.getOrDefault(e.getKey(),0L));return r;}
 public Map<String,Object> report(){List<Map<String,Object>> groups=new ArrayList<>();for(Map.Entry<String,Action> e:actions.entrySet()){Action a=e.getValue();groups.add(GeometryEvidenceSupport.map("phaseAndNormalizedOperation",e.getKey(),"actionGroups",a.count,"actionNs",a.total,"maxActionNs",a.max,"returnedNullGroups",a.nulls,"exceptionGroups",a.errors));}
  return GeometryEvidenceSupport.map("totals",marks(),"actionGroups",groups,"actionKeyLimit",MAX_ACTION_KEYS,"actionGroupsOverflowed",actionOverflow,"negativeDurationSamples",negativeDurations,
   "firstReadThreadId",firstReadThread<0?"UNKNOWN":firstReadThread,"differentReadThreadObserved",differentReadThread,"firstBusinessOutputUtc",firstOutputUtc==null?"UNKNOWN":firstOutputUtc,"lastBusinessOutputUtc",lastOutputUtc==null?"UNKNOWN":lastOutputUtc,
   "slowOutputThresholdNs",SLOW_OUTPUT_SAMPLE_NS,"slowOutputSampleLimit",MAX_SLOW_OUTPUT_SAMPLES,"slowOutputSamples",new ArrayList<>(slowOutputs),
   "callCountingScope","WRAPPED_READ_ACTION_GROUPS_NOT_EXHAUSTIVE_NATIVE_OR_JNI_METHOD_CALL_COUNTS","oneActionCanContainMultipleSdkGetters",true,"actionNsExcludesFailureDiagnosticLogging",true,
   "outputScope","BUSINESS_JSONL_ONLY_LIFECYCLE_TIMING_JOURNAL_REPORTED_SEPARATELY","countersAreObservedNotExclusiveCpuTime",true);
 }
}
