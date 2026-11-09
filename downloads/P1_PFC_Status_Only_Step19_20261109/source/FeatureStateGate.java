package p1.controlled.step19;
import java.util.*;
/** Deliberately reduced read-only diagnostic gate; cannot certify full feature health. */
public final class FeatureStateGate {
 private FeatureStateGate(){}
 public static final String SOURCE="PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG";
 private static final Set<String> DIAGNOSTIC=Set.of("FEAT_INACTIVE","FEAT_FAMILY_TABLE_SUPPRESSED","FEAT_SIMP_REP_SUPPRESSED","FEAT_PROGRAM_SUPPRESSED","FEAT_SUPPRESSED");
 public static boolean diagnosticOnly(String status){return status!=null&&DIAGNOSTIC.contains(status);}
 public static String rejection(String status){
  if("FEAT_UNREGENERATED".equals(status))return "FEAT_UNREGENERATED";
  if("FEAT_ACTIVE".equals(status)||diagnosticOnly(status))return null;
  return "UNKNOWN_FEATURE_STATUS";
 }
}
