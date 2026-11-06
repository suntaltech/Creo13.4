import java.util.*;
import p1.readonly.geometry.step10.*;
public final class GeometryCandidateClassificationTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("classification check "+checks);}
 static Map<String,Object> sum(int count,int a,int b){return GeometryEvidenceSupport.map("nativeInsideWitnesses",count,"aToBWitnesses",a,"bToAWitnesses",b,"probeLocationsAttempted",6,"status","DISCRETE_TWO_FACE_NATIVE_INSIDE_WITNESSES");}
 static Map<String,Object> gap(String s){return GeometryEvidenceSupport.map("status",s);}
 static boolean tag(Map<String,Object> c,String t){return ((List<?>)c.get("tags")).contains(t);}
 public static void main(String[]args){
  GeometryCandidateClassification.Stats s=new GeometryCandidateClassification.Stats();s.observe(1,2,1,2,0,-1);s.observe(1,2,2,1,20,1);
  Map<String,Object> c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",gap("NONPARALLEL_NO_CONSTANT_SUPPORT_GAP"),sum(2,1,1),s);
  for(String t:Arrays.asList("ZERO_DISTANCE_COINCIDENT_MAPPED_POINTS_RECORDED","MIXED_ZERO_AND_POSITIVE_LOCAL_DISTANCES","NONPARALLEL_PLANE_CANDIDATE","LOCAL_DISTANCE_EXCEEDS_10_MM_REVIEW_HINT","BIDIRECTIONAL_LOCAL_WITNESSES_NOT_MATERIAL_PROOF"))check(tag(c,t));
  check(c.get("summaryCountConsistency").equals(true));check(c.get("globalWallThickness").equals("NOT_ASSESSED"));check(c.get("materialOccupancyVerified").equals(false));check(c.get("occlusionVerified").equals(false));
  s=new GeometryCandidateClassification.Stats();s.observe(1,2,1,2,1.2,-1);
  c=GeometryCandidateClassification.classify("1_2",1,2,"CYLINDER_CYLINDER","mm",gap("NONCOAXIAL_RADIUS_GAP_UNKNOWN"),sum(1,1,0),s);
  check(tag(c,"NONCOAXIAL_CYLINDER_CANDIDATE"));check(tag(c,"SINGLE_DIRECTION_WITNESSES_ONLY"));check(c.get("anyLocalDistanceAboveReviewThreshold").equals(false));check(c.get("globalWallThickness").equals("NOT_ASSESSED"));
  c=GeometryCandidateClassification.classify("1_2",1,2,"CYLINDER_CYLINDER","inch",null,sum(1,1,0),s);check(c.get("anyLocalDistanceAboveReviewThreshold").equals("UNKNOWN_UNSUPPORTED_OR_UNKNOWN_UNIT"));check(tag(c,"ANALYTIC_PAIR_RELATION_UNKNOWN"));
  c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,null,null);check(c.get("directionState").equals("UNKNOWN_NO_PROBES_OR_SUMMARY"));check(c.get("localDistanceMin").equals("UNKNOWN_NO_RECORDS"));check(tag(c,"NO_PAIR_SUMMARY_UNKNOWN"));
  c=GeometryCandidateClassification.classify("1_2",1,2,"CYLINDER_CYLINDER","mm",gap("COAXIAL_CYLINDER_RADIUS_GAP_CANDIDATE"),sum(0,0,0),null);check(c.get("directionState").equals("NO_WITNESSES_IN_BOUNDED_PROBES"));check(tag(c,"NO_LOCAL_DISTANCE_WITNESSES_RECORDED"));
  Map<String,Object> unprobed=GeometryEvidenceSupport.map("nativeInsideWitnesses",0,"probeLocationsAttempted",0,"status","UNKNOWN_ANALYTIC_PARAMETERS");c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,unprobed,null);check(c.get("summaryCountConsistency").equals(true));check(c.get("directionState").equals("UNKNOWN_NO_PROBES_OR_SUMMARY"));
  s=new GeometryCandidateClassification.Stats();for(double d:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})s.observe(1,2,1,2,d,-1);s.observe(1,2,9,2,1,-1);check(s.invalid==4&&s.count==0);
  c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,sum(0,0,0),s);check(tag(c,"INCONSISTENT_OR_INVALID_CLASSIFICATION_INPUT"));
  s=new GeometryCandidateClassification.Stats();s.observe(1,2,1,2,10,-1);c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,sum(1,1,0),s);check(c.get("anyLocalDistanceAboveReviewThreshold").equals(false));s.observe(1,2,2,1,Math.nextUp(10.0),Double.NaN);c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,sum(2,1,1),s);check(c.get("anyLocalDistanceAboveReviewThreshold").equals(true));check(c.get("unknownNormalWitnesses").equals(1));
  c=GeometryCandidateClassification.classify("1_2",1,2,"PLANE_PLANE","mm",null,sum(3,2,1),s);check(c.get("summaryCountConsistency").equals(false));check(c.get("directionState").equals("UNKNOWN_INVALID_RECORDS"));
  check(GeometrySdkInterfaceReview.entries().size()==12);
  for(Map<String,Object> item:GeometrySdkInterfaceReview.entries()){check(item.get("runtimeInvokedByThisStep").equals(false));check(item.get("readOnlySemanticContractCertified").equals(false));check(item.get("materialOccupancyProven").equals(false));check(item.get("occlusionProven").equals(false));}
  check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==256*Math.ulp(1.0));
  System.out.println("CANDIDATE_CLASSIFICATION_CHECKS_PASSED="+checks);
 }
}
