package p1.controlled.step18;
import java.util.*;
/** Pure gate over actual read results; no SDK calls or model objects. */
public final class FeatureStateGate {
 private FeatureStateGate(){}
 public static final List<String> FLAG_METHODS=List.of("GetIsInvalid","GetIsInactive","GetIsActive","GetIsFamtabSuppressed","GetIsSimprepSuppressed","GetIsProgramSuppressed","GetIsSuppressed","GetIsUnregenerated","GetIsFailed","GetIsChildOfFailed","GetIsChildOfExternalFailed");
 private static final Set<String> KNOWN=Set.of("FEAT_ACTIVE","FEAT_INACTIVE","FEAT_FAMILY_TABLE_SUPPRESSED","FEAT_SIMP_REP_SUPPRESSED","FEAT_PROGRAM_SUPPRESSED","FEAT_SUPPRESSED","FEAT_UNREGENERATED");
 public static String rejection(int featureId,int flagId,String status,Map<String,Boolean> flags){
  if(featureId!=flagId)return "FLAG_FEATURE_ID_MISMATCH";
  if(status==null||!KNOWN.contains(status))return "UNKNOWN_FEATURE_STATUS";
  if(flags==null)return "MISSING_STATUS_FIELDS";
  for(String field:FLAG_METHODS)if(flags.get(field)==null)return "MISSING_STATUS_FIELD:"+field;
  if("FEAT_UNREGENERATED".equals(status))return "FEAT_UNREGENERATED";
  for(String field:List.of("GetIsFailed","GetIsInvalid","GetIsUnregenerated","GetIsChildOfFailed","GetIsChildOfExternalFailed"))if(flags.get(field))return field;
  return null;
 }
}
