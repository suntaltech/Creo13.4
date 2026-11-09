package p1.controlled.step18;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import static p1.controlled.step18.AuditSupport.*;
/** Durable START/return boundaries. Summaries must be Java-only and must not call native getters. */
public final class PreflightAudit {
 @FunctionalInterface public interface Call<T>{T get()throws Exception;}
 public static final class ProbeFailure extends Exception {
  private static final long serialVersionUID=1L;
  ProbeFailure(String operation,Throwable cause){super("PREFLIGHT_READ_EXCEPTION: "+operation+": "+cause.getClass().getName()+": "+cause.getMessage(),cause);}
 }
 private final Step18Control control;private int nextCall;
 public PreflightAudit(Step18Control control){this.control=control;}
 public <T>T read(String start,String done,String operation,Call<T> action,Function<T,Map<String,Object>> summary)throws Exception{
  return read(start,done,operation,Collections.emptyMap(),action,summary);
 }
 public <T>T read(String start,String done,String operation,Map<String,Object> context,Call<T> action,Function<T,Map<String,Object>> summary)throws Exception{
  int call=++nextCall;long begin=System.nanoTime();
  Map<String,Object> startFields=map("phase","START","probeId",call,"operation",operation,"groupedGetterNotation",operation.endsWith("[*]"),"writeModel",false);startFields.putAll(context);control.record(start,startFields);
  try{T value=action.get();long returned=System.nanoTime();Map<String,Object> fields=map("phase","DONE","probeId",call,"operation",operation,"actionAndStartAuditElapsedNs",returned-begin,"returnSummary",summary.apply(value));fields.putAll(context);control.record(done,fields);return value;}
  catch(Exception|LinkageError error){try{Map<String,Object> exceptionFields=map("phase","THREW","probeId",call,"operation",operation,"startEvent",start,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage()));exceptionFields.putAll(context);control.record("PREFLIGHT_PROBE_EXCEPTION",exceptionFields);}catch(IOException|RuntimeException auditError){error.addSuppressed(auditError);}throw new ProbeFailure(operation,error);}
 }
 public void note(String event,Map<String,Object> fields)throws IOException{Map<String,Object> row=map("phase","DONE");row.putAll(fields);control.record(event,row);}
}
