package p1.controlled.step17;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import static p1.controlled.step17.AuditSupport.*;
/** Durable START/return boundaries. Summaries must be Java-only and must not call native getters. */
public final class PreflightAudit {
 @FunctionalInterface public interface Call<T>{T get()throws Exception;}
 public static final class ProbeFailure extends Exception {
  private static final long serialVersionUID=1L;
  ProbeFailure(String operation,Throwable cause){super("PREFLIGHT_READ_EXCEPTION: "+operation+": "+cause.getClass().getName()+": "+cause.getMessage(),cause);}
 }
 private final Step17Control control;private int nextCall;
 public PreflightAudit(Step17Control control){this.control=control;}
 public <T>T read(String start,String done,String operation,Call<T> action,Function<T,Map<String,Object>> summary)throws Exception{
  int call=++nextCall;long begin=System.nanoTime();
  control.record(start,map("phase","START","probeId",call,"operation",operation,"groupedGetterNotation",operation.endsWith("[*]"),"writeModel",false));
  try{T value=action.get();long returned=System.nanoTime();Map<String,Object> fields=map("phase","DONE","probeId",call,"operation",operation,"actionAndStartAuditElapsedNs",returned-begin,"returnSummary",summary.apply(value));control.record(done,fields);return value;}
  catch(Exception|LinkageError error){try{control.record("PREFLIGHT_PROBE_EXCEPTION",map("phase","THREW","probeId",call,"operation",operation,"startEvent",start,"exceptionClass",error.getClass().getName(),"exceptionMessage",String.valueOf(error.getMessage())));}catch(IOException|RuntimeException auditError){error.addSuppressed(auditError);}throw new ProbeFailure(operation,error);}
 }
 public void note(String event,Map<String,Object> fields)throws IOException{Map<String,Object> row=map("phase","DONE");row.putAll(fields);control.record(event,row);}
}
