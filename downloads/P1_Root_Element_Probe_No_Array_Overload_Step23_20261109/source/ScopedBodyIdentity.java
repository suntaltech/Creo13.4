package p1.controlled.step23;
/** Pure body-specific policy. Handles are diagnostics and are deliberately not policy inputs. */
public final class ScopedBodyIdentity {
 private ScopedBodyIdentity(){}
 public enum Code { ACCEPTED, DEFAULT_BODY_NULL, ENUMERATED_BODY_UNAVAILABLE, MODEL_SCOPE_MISMATCH, BODY_ID_MISMATCH, ALREADY_SHEETMETAL, STATE_READ_EXCEPTION }
 public static Code evaluate(boolean scopeVerified,Integer count,boolean listedPresent,boolean defaultPresent,Integer listedId,Integer defaultId,boolean recheck,Integer baselineId,Boolean nativeSheetmetal,boolean nativeReadFailed){
  if(!scopeVerified)return Code.MODEL_SCOPE_MISMATCH;
  if(nativeReadFailed)return Code.STATE_READ_EXCEPTION;
  if(!defaultPresent)return Code.DEFAULT_BODY_NULL;
  if(count==null||count!=1||!listedPresent)return Code.ENUMERATED_BODY_UNAVAILABLE;
  if(listedId==null||defaultId==null||nativeSheetmetal==null||(recheck&&baselineId==null))return Code.STATE_READ_EXCEPTION;
  if(!listedId.equals(defaultId)||(recheck&&!defaultId.equals(baselineId)))return Code.BODY_ID_MISMATCH;
  if(nativeSheetmetal)return Code.ALREADY_SHEETMETAL;
  return Code.ACCEPTED;
 }
}
