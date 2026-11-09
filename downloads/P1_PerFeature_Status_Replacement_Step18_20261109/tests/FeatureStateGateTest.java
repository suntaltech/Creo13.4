import p1.controlled.step18.FeatureStateGate;
import java.util.*;
public final class FeatureStateGateTest {
 static int checks;static void check(boolean value){checks++;if(!value)throw new AssertionError("check="+checks);}
 static Map<String,Boolean> clean(){Map<String,Boolean> fields=new LinkedHashMap<>();for(String name:FeatureStateGate.FLAG_METHODS)fields.put(name,false);fields.put("GetIsActive",true);return fields;}
 public static void main(String[] args){
  for(String status:List.of("FEAT_ACTIVE","FEAT_INACTIVE","FEAT_FAMILY_TABLE_SUPPRESSED","FEAT_SIMP_REP_SUPPRESSED","FEAT_PROGRAM_SUPPRESSED","FEAT_SUPPRESSED"))check(FeatureStateGate.rejection(9,9,status,clean())==null);
  check("FEAT_UNREGENERATED".equals(FeatureStateGate.rejection(9,9,"FEAT_UNREGENERATED",clean())));
  for(String field:List.of("GetIsFailed","GetIsInvalid","GetIsUnregenerated","GetIsChildOfFailed","GetIsChildOfExternalFailed")){Map<String,Boolean> f=clean();f.put(field,true);check(field.equals(FeatureStateGate.rejection(9,9,"FEAT_ACTIVE",f)));}
  for(String field:FeatureStateGate.FLAG_METHODS){Map<String,Boolean> f=clean();f.remove(field);check(FeatureStateGate.rejection(9,9,"FEAT_ACTIVE",f).startsWith("MISSING_STATUS_FIELD:"));}
  check("FLAG_FEATURE_ID_MISMATCH".equals(FeatureStateGate.rejection(9,10,"FEAT_ACTIVE",clean())));
  check("UNKNOWN_FEATURE_STATUS".equals(FeatureStateGate.rejection(9,9,null,clean())));
  check("UNKNOWN_FEATURE_STATUS".equals(FeatureStateGate.rejection(9,9,"UNKNOWN_STATUS_99",clean())));
  check("MISSING_STATUS_FIELDS".equals(FeatureStateGate.rejection(9,9,"FEAT_ACTIVE",null)));
  Map<String,Boolean> inactive=clean();inactive.put("GetIsInactive",true);check(FeatureStateGate.rejection(9,9,"FEAT_INACTIVE",inactive)==null);
  System.out.println("FEATURE_STATE_GATE_CHECKS_PASSED="+checks);
 }
}
