import p1.readonly.geometry.step10.*;
import java.util.*;
public final class GeometryStderrPolicyTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static long n(Map<?,?> m,String k){return ((Number)m.get(k)).longValue();}
 public static void main(String[] args){
  GeometryStderrPolicy defaultPolicy=new GeometryStderrPolicy(null);check(defaultPolicy.mode().equals("REDUCED"));check(!defaultPolicy.fallbackFull());
  check(new GeometryStderrPolicy(" ").mode().equals("REDUCED"));check(new GeometryStderrPolicy(" reduced ").mode().equals("REDUCED"));
  check(new GeometryStderrPolicy("full").mode().equals("FULL"));
  GeometryStderrPolicy invalid=new GeometryStderrPolicy("invalid-user-setting");check(invalid.mode().equals("FULL"));check(invalid.fallbackFull());
  check(!invalid.report().toString().contains("invalid-user-setting"));
  for(String event:new String[]{"READ_EXCEPTION","READ_UNAVAILABLE","DATA_ISSUE","COLLECTION_STARTED","COLLECTION_SUMMARY","SAMPLING_SUMMARY","CANDIDATE_CLASSIFICATION_SUMMARY","MODEL_BEFORE","MODEL_AFTER_ORIGINAL","MODEL_AFTER_ACTIVE","STEP2_IDENTITY_CONTEXT","NEW_FUTURE_EVENT"})check(defaultPolicy.shouldMirror(event));
  for(String event:new String[]{"POINT_NORMAL_SAMPLE","SAMPLE_REJECTED","PAIR_LOCAL_DISTANCE_WITNESS","PAIR_DOMAIN_POINT_EVALUATION","TIMING_OBSERVATION_SNAPSHOT"})check(!defaultPolicy.shouldMirror(event));
  check(defaultPolicy.shouldMirror(null));check(n(defaultPolicy.report(),"selectionDecisionsAfterSuccessfulBusinessFlush")==0);
  check(defaultPolicy.decide("READ_EXCEPTION"));check(!defaultPolicy.decide("POINT_NORMAL_SAMPLE"));check(defaultPolicy.decide("COLLECTION_SUMMARY"));
  Map<String,Object> report=defaultPolicy.report();check(n(report,"selectionDecisionsAfterSuccessfulBusinessFlush")==3);check(n(report,"mirrorSelectedAttempts")==2);check(n(report,"stderrSuppressedRecords")==1);
  check(Boolean.FALSE.equals(report.get("businessJsonlFiltered")));check(Boolean.FALSE.equals(report.get("batchingEnabled")));
  GeometryStderrPolicy full=new GeometryStderrPolicy("FULL");for(String event:new String[]{"READ_EXCEPTION","POINT_NORMAL_SAMPLE","SAMPLE_REJECTED","TIMING_OBSERVATION_SNAPSHOT",null})check(full.decide(event));
  check(n(full.report(),"mirrorSelectedAttempts")==5);check(n(full.report(),"stderrSuppressedRecords")==0);
  System.out.println("STDERR_POLICY_CHECKS_PASSED="+checks);
 }
}
