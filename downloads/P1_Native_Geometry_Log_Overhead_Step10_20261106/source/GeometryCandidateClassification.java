package p1.readonly.geometry.step10;
import java.util.*;

/** Multiple review labels over already-recorded distances; never decides material occupancy or wall thickness. */
public final class GeometryCandidateClassification {
 public static final double FAR_REVIEW_THRESHOLD_MM=10.0;
 private GeometryCandidateClassification() { }
 public static final class Stats {
  public int count,zero,positive,invalid,aToB,bToA,opposed,sameDirection,unknownNormal;
  public double minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
  public void observe(int faceA,int faceB,int source,int target,double distance,double normalDot) {
   if(!Double.isFinite(distance)||distance<0||!((source==faceA&&target==faceB)||(source==faceB&&target==faceA))){invalid++;return;}
   count++;if(distance==0)zero++;else positive++;minimum=Math.min(minimum,distance);maximum=Math.max(maximum,distance);
   if(source==faceA)aToB++;else bToA++;
   if(!Double.isFinite(normalDot)||Math.abs(normalDot)>1+GeometryAnalyticMath.ANGULAR_NUMERIC_GUARD)unknownNormal++;
   else{if(normalDot<=-GeometryPairEvidence.NORMAL_COMPATIBILITY_COSINE)opposed++;if(normalDot>=GeometryPairEvidence.NORMAL_COMPATIBILITY_COSINE)sameDirection++;}
  }
 }
 public static Map<String,Object> classify(String pairId,int faceA,int faceB,String type,String unit,Map<String,Object> gap,Map<String,Object> summary,Stats s) {
  if(s==null)s=new Stats();List<String> tags=new ArrayList<>();
  String gapStatus=gap==null?"UNKNOWN_NO_ANALYTIC_GAP_RECORD":String.valueOf(gap.get("status"));
  String status=summary==null?"NOT_VISITED_OR_ABORTED_UNKNOWN":String.valueOf(summary.get("status"));
  boolean summaryKnown=summary!=null&&summary.get("nativeInsideWitnesses") instanceof Number;
  boolean directionsKnown=summary!=null&&summary.get("aToBWitnesses") instanceof Number&&summary.get("bToAWitnesses") instanceof Number;
  boolean countsMatch=summaryKnown&&((Number)summary.get("nativeInsideWitnesses")).intValue()==s.count
    &&(!directionsKnown||(((Number)summary.get("aToBWitnesses")).intValue()==s.aToB&&((Number)summary.get("bToAWitnesses")).intValue()==s.bToA));
  boolean invalid=s.invalid>0||(summaryKnown&&!countsMatch);
  String coverage=summary==null?"NOT_VISITED_OR_ABORTED_UNKNOWN":status.startsWith("PARTIAL")?"PARTIALLY_PROBED":summary.get("probeLocationsAttempted") instanceof Number&&((Number)summary.get("probeLocationsAttempted")).intValue()==0?"NOT_PROBED_ANALYTIC_UNKNOWN":"BOUNDED_PROBES_RECORDED";
  if(summary==null)tags.add("NO_PAIR_SUMMARY_UNKNOWN");
  if(coverage.equals("NOT_PROBED_ANALYTIC_UNKNOWN"))tags.add("NO_PROBES_ANALYTIC_PARAMETERS_UNKNOWN");
  if(coverage.equals("PARTIALLY_PROBED"))tags.add("PARTIAL_PROBING_UNKNOWN_COVERAGE");
  if(invalid)tags.add("INCONSISTENT_OR_INVALID_CLASSIFICATION_INPUT");
  if(s.zero>0)tags.add("ZERO_DISTANCE_COINCIDENT_MAPPED_POINTS_RECORDED");
  if(s.zero>0&&s.positive>0)tags.add("MIXED_ZERO_AND_POSITIVE_LOCAL_DISTANCES");
  if(s.count==0)tags.add("NO_LOCAL_DISTANCE_WITNESSES_RECORDED");
  if(type.equals("PLANE_PLANE")&&gapStatus.equals("NONPARALLEL_NO_CONSTANT_SUPPORT_GAP"))tags.add("NONPARALLEL_PLANE_CANDIDATE");
  if(type.equals("CYLINDER_CYLINDER")&&(gapStatus.equals("NONCOAXIAL_RADIUS_GAP_UNKNOWN")||gapStatus.equals("NONPARALLEL_NO_CONSTANT_SUPPORT_GAP")))tags.add("NONCOAXIAL_CYLINDER_CANDIDATE");
  if(type.equals("CYLINDER_CYLINDER")&&gapStatus.equals("NONPARALLEL_NO_CONSTANT_SUPPORT_GAP"))tags.add("CYLINDER_AXES_NOT_NUMERICALLY_PARALLEL");
  if(gap==null||gapStatus.startsWith("UNKNOWN"))tags.add("ANALYTIC_PAIR_RELATION_UNKNOWN");
  String direction=invalid?"UNKNOWN_INVALID_RECORDS":s.aToB>0&&s.bToA>0?"BIDIRECTIONAL_WITNESSES":s.aToB>0?"A_TO_B_ONLY":s.bToA>0?"B_TO_A_ONLY":(summary==null||coverage.equals("NOT_PROBED_ANALYTIC_UNKNOWN"))?"UNKNOWN_NO_PROBES_OR_SUMMARY":"NO_WITNESSES_IN_BOUNDED_PROBES";
  if(direction.equals("A_TO_B_ONLY")||direction.equals("B_TO_A_ONLY"))tags.add("SINGLE_DIRECTION_WITNESSES_ONLY");
  if(direction.equals("BIDIRECTIONAL_WITNESSES"))tags.add("BIDIRECTIONAL_LOCAL_WITNESSES_NOT_MATERIAL_PROOF");
  Object far;
  if(!"mm".equals(unit)){far="UNKNOWN_UNSUPPORTED_OR_UNKNOWN_UNIT";tags.add("FAR_REVIEW_POLICY_UNIT_UNKNOWN");}
  else if(s.count==0)far="UNKNOWN_NO_DISTANCE_RECORDS";
  else {far=s.maximum>FAR_REVIEW_THRESHOLD_MM;if(Boolean.TRUE.equals(far))tags.add("LOCAL_DISTANCE_EXCEEDS_10_MM_REVIEW_HINT");}
  if(s.unknownNormal>0)tags.add("NORMAL_RELATION_UNKNOWN_FOR_SOME_RECORDS");
  if(s.opposed>0)tags.add("APPROXIMATELY_OPPOSED_NORMAL_SAMPLES_PRESENT");
  if(s.sameDirection>0)tags.add("APPROXIMATELY_SAME_DIRECTION_NORMAL_SAMPLES_PRESENT");
  tags.add("MATERIAL_OCCUPANCY_OCCLUSION_AND_FINAL_THICKNESS_UNCONFIRMED");
  return GeometryEvidenceSupport.map("pairId",pairId,"faceA",faceA,"faceB",faceB,"pairType",type,"lengthUnit",unit==null?"UNKNOWN":unit,
   "classificationScope","MULTI_LABEL_REVIEW_OF_RECORDED_LOCAL_POINTS_NOT_A_WALL_THICKNESS_DECISION","tags",tags,"tagsMutuallyExclusive",false,
   "probeCoverageState",coverage,"sourcePairStatus",status,"analyticGapStatus",gapStatus,
   "witnessRecords",s.count,"zeroDistanceRecords",s.zero,"positiveDistanceRecords",s.positive,"invalidRecords",s.invalid,
   "localDistanceMin",s.count>0?s.minimum:"UNKNOWN_NO_RECORDS","localDistanceMax",s.count>0?s.maximum:"UNKNOWN_NO_RECORDS",
   "aToBWitnesses",s.aToB,"bToAWitnesses",s.bToA,"directionState",direction,"summaryCountConsistency",summaryKnown?countsMatch:"UNKNOWN_NO_SUMMARY",
   "directionCountComparisonState",directionsKnown?"COUNTS_PROVIDED":"UNKNOWN_NOT_IN_PAIR_SUMMARY",
   "opposedNormalWitnesses",s.opposed,"sameDirectionNormalWitnesses",s.sameDirection,"unknownNormalWitnesses",s.unknownNormal,
   "outwardNormalCertified",false,"farDistanceReviewThresholdMm",FAR_REVIEW_THRESHOLD_MM,"anyLocalDistanceAboveReviewThreshold",far,
   "farPolicyMeaning","EXPLICIT_10_MM_TRIAGE_ONLY_NOT_MODEL_TOLERANCE_OR_THICKNESS_LIMIT_NO_UNIT_CONVERSION",
   "independentOrUniqueSpatialLocationsCertified",false,"materialOccupancyVerified",false,"occlusionVerified",false,
   "trueOppositeFacesConfirmed",false,"entireTrimmedOverlapRegionProven",false,"globalWallThickness","NOT_ASSESSED","uniformThicknessAssessment","INSUFFICIENT_EVIDENCE");
 }
}
