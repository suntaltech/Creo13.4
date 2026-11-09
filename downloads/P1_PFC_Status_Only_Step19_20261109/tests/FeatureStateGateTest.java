import p1.controlled.step19.FeatureStateGate;
import java.util.*;
public final class FeatureStateGateTest {
 static int checks;static void check(boolean value){checks++;if(!value)throw new AssertionError("check="+checks);}
 public static void main(String[] args){
  check(FeatureStateGate.rejection("FEAT_ACTIVE")==null);check(!FeatureStateGate.diagnosticOnly("FEAT_ACTIVE"));
  for(String status:List.of("FEAT_INACTIVE","FEAT_FAMILY_TABLE_SUPPRESSED","FEAT_SIMP_REP_SUPPRESSED","FEAT_PROGRAM_SUPPRESSED","FEAT_SUPPRESSED")){check(FeatureStateGate.rejection(status)==null);check(FeatureStateGate.diagnosticOnly(status));}
  check("FEAT_UNREGENERATED".equals(FeatureStateGate.rejection("FEAT_UNREGENERATED")));check(!FeatureStateGate.diagnosticOnly("FEAT_UNREGENERATED"));
  for(String status:Arrays.asList(null,"","UNKNOWN_STATUS_99","FEAT_FAILED","feat_active")){check("UNKNOWN_FEATURE_STATUS".equals(FeatureStateGate.rejection(status)));check(!FeatureStateGate.diagnosticOnly(status));}
  check("PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG".equals(FeatureStateGate.SOURCE));
  System.out.println("FEATURE_STATE_GATE_CHECKS_PASSED="+checks);
 }
}
