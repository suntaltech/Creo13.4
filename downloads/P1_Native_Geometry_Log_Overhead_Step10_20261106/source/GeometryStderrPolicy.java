package p1.readonly.geometry.step10;
import java.util.Locale;
import java.util.Map;
import static p1.readonly.geometry.step10.GeometryEvidenceSupport.map;

/** Synchronous stderr selection only. Does not filter, buffer or write the business JSONL. */
public final class GeometryStderrPolicy {
 public static final String ENVIRONMENT_KEY="P1_READONLY_GEOMETRY_STDERR_MODE";
 public static final String DEFAULT_MODE="REDUCED",FULL_MODE="FULL";
 private final String mode,configurationState;
 private long decisions,mirrored,suppressed;
 public GeometryStderrPolicy(String configured){
  String value=configured==null?"":configured.strip().toUpperCase(Locale.ROOT);
  if(value.isEmpty()){mode=DEFAULT_MODE;configurationState="DEFAULT_REDUCED";}
  else if(value.equals(DEFAULT_MODE)||value.equals(FULL_MODE)){mode=value;configurationState="EXPLICIT_VALID";}
  else{mode=FULL_MODE;configurationState="INVALID_VALUE_FALLBACK_FULL";}
 }
 public static GeometryStderrPolicy fromEnvironment(){
  try{return new GeometryStderrPolicy(System.getenv(ENVIRONMENT_KEY));}
  catch(SecurityException error){return new GeometryStderrPolicy("INVALID_ENVIRONMENT_UNAVAILABLE");}
 }
 public String mode(){return mode;}
 public boolean fallbackFull(){return configurationState.equals("INVALID_VALUE_FALLBACK_FULL");}
 public boolean shouldMirror(String event){
  // Unknown future event types remain visible; suppress only explicitly known high-volume or bulky events.
  if(mode.equals(FULL_MODE)||event==null)return true;
  switch(event){
   case "SURFACE_UV_RANGE":case "SURFACE":case "SURFACE_CANDIDATE_PLAN":case "POINT_NORMAL_SAMPLE":
   case "PLANE_SAMPLE_QUALITY":case "CROSS_RUN_SPREAD_COMPARISON":case "SURFACE_SAMPLING_SUMMARY":
   case "SAMPLE_REJECTED":case "PLANE_QUALITY_POOL_STARTED":case "POINT_NORMAL_POOL_CANDIDATE":
   case "QUALITY_CANDIDATE_QUEUE":case "FROZEN_STEP5_SELECTION_BASELINE":case "PLANE_POOL_SELECTION":
   case "SURFACE_REFINEMENT_STARTED":case "RELATIVE_FACE_PAIR_CANDIDATE":case "SURFACE_ANALYTIC_PARAMETERS":
   case "PAIR_ANALYTIC_GAP_CANDIDATE":case "PAIR_DOMAIN_POINT_EVALUATION":case "PAIR_LOCAL_DISTANCE_WITNESS":
   case "PAIR_OVERLAP_VALIDATION_SUMMARY":case "PAIR_CANDIDATE_CLASSIFICATION":case "SDK_READONLY_INTERFACE_REVIEW_ITEM":
   case "TIMING_OBSERVATION_SNAPSHOT":return false;
   default:return true;
  }
 }
 /** Invoked after successful business flush, before any selected stderr attempt. */
 public boolean decide(String event){boolean yes=shouldMirror(event);decisions++;if(yes)mirrored++;else suppressed++;return yes;}
 public Map<String,Object> report(){return map("schema","P1_NATIVE_GEOMETRY_STDERR_POLICY_V1","effectiveMode",mode,"configurationState",configurationState,
  "environmentKey",ENVIRONMENT_KEY,"selectionDecisionsAfterSuccessfulBusinessFlush",decisions,"mirrorSelectedAttempts",mirrored,"stderrSuppressedRecords",suppressed,
  "unknownEventTypesMirrored",true,"businessJsonlFiltered",false,"businessFlushPolicy","EVERY_RECORD_UNCHANGED_FROM_STEP9","batchingEnabled",false,
  "nativeThreadPolicyChanged",false,"decisionIsNotStderrDeliveryProof",true);}
}
