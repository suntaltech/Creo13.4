package p1.controlled.step21;
/** PFC-only experiment gate, not a full failure-flag check. */
public final class FeatureStateGate {
 private FeatureStateGate(){}
 public static final String SOURCE="PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG";
 public static final int EXPECTED_FEATURE_COUNT=5;
 public static String countRejection(int count){return count==EXPECTED_FEATURE_COUNT?null:"EXPECTED_EXACTLY_FIVE_FEATURES";}
 public static boolean diagnosticOnly(String status){return false;}
 public static String rejection(String status){
  if("FEAT_ACTIVE".equals(status))return null;
  if("FEAT_UNREGENERATED".equals(status))return "FEAT_UNREGENERATED";
  if("FEAT_INACTIVE".equals(status)||"FEAT_FAMILY_TABLE_SUPPRESSED".equals(status)||"FEAT_SIMP_REP_SUPPRESSED".equals(status)||"FEAT_PROGRAM_SUPPRESSED".equals(status)||"FEAT_SUPPRESSED".equals(status))return "FEATURE_NOT_ACTIVE";
  return "UNKNOWN_FEATURE_STATUS";
 }
}
