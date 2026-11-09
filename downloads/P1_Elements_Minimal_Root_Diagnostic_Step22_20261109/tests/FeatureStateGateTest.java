import p1.controlled.step22.FeatureStateGate;
import java.util.*;
public final class FeatureStateGateTest {
 static int checks;static void check(boolean value){checks++;if(!value)throw new AssertionError("check="+checks);}
 public static void main(String[] args){
  check(FeatureStateGate.rejection("FEAT_ACTIVE")==null);
  for(String status:List.of("FEAT_INACTIVE","FEAT_FAMILY_TABLE_SUPPRESSED","FEAT_SIMP_REP_SUPPRESSED","FEAT_PROGRAM_SUPPRESSED","FEAT_SUPPRESSED"))check("FEATURE_NOT_ACTIVE".equals(FeatureStateGate.rejection(status)));
  check("FEAT_UNREGENERATED".equals(FeatureStateGate.rejection("FEAT_UNREGENERATED")));
  for(String status:Arrays.asList(null,"","UNKNOWN_STATUS_99","FEAT_FAILED","feat_active"))check("UNKNOWN_FEATURE_STATUS".equals(FeatureStateGate.rejection(status)));
  check(FeatureStateGate.countRejection(5)==null);
  for(int count:new int[]{-1,0,1,4,6,10000})check("EXPECTED_EXACTLY_FIVE_FEATURES".equals(FeatureStateGate.countRejection(count)));
  check("PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG".equals(FeatureStateGate.SOURCE));
  System.out.println("FEATURE_STATE_GATE_CHECKS_PASSED="+checks);
 }
}
