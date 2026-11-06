import java.util.*;
import p1.readonly.geometry.step7.*;
public final class GeometryStep6EvidenceTest {
 static int checks;
 static void check(boolean v){checks++;if(!v)throw new AssertionError("step6 check "+checks);}
 static List<double[]> points(double... x){List<double[]> r=new ArrayList<>();for(int i=0;i<x.length;i+=3)r.add(new double[]{x[i],x[i+1],x[i+2]});return r;}
 public static void main(String[] args){
  List<double[]> pool=points(0,0,0,1,0,0,0,1,0,2,0,0,0,2,0,2,2,0,1,1,0,3,1,0,1,3,0);
  List<Integer> frozen=Arrays.asList(0,1,2);double area=((Number)GeometrySamplingMath.xyzQuality(pool.subList(0,3)).get("xyzTriangleArea")).doubleValue();
  Map<String,Object> s=GeometrySpreadSelection.select(pool,frozen);check(s.get("step5BaselineAreaNotDecreased").equals(true));check(s.get("finalNumericallyNonCollinear").equals(true));
  for(int i=9;i<35;i++){pool.add(new double[]{i*.3,(i%5)*.7,0});s=GeometrySpreadSelection.select(pool,frozen);check(s.get("step5BaselineAreaNotDecreased").equals(true));Map<?,?> q=(Map<?,?>)s.get("finalSpreadQuality");check(((Number)q.get("xyzTriangleArea")).doubleValue()>=area);check(q.get("numericallyNonCollinear").equals(true));}
  check(((Number)s.get("combinationsExamined")).intValue()==6545);boolean bad=false;pool.add(new double[]{9,9,0});try{GeometrySpreadSelection.select(pool,frozen);}catch(IllegalArgumentException e){bad=true;}check(bad);
  List<double[]> line=points(0,0,0,1,0,0,2,0,0);check(GeometrySpreadSelection.select(line,frozen).get("replacementApplied").equals(false));
  List<double[]> invalid=points(0,0,0,1,0,0,0,1,0,Double.NaN,0,0);check(GeometrySpreadSelection.select(invalid,frozen).get("selectedIndices").equals(frozen));
  bad=false;try{GeometrySpreadSelection.select(line,Arrays.asList(0,0,2));}catch(IllegalArgumentException e){bad=true;}check(bad);
  List<double[]> a=points(0,0,0,2,0,0,0,2,0),b=points(0,0,7,2,0,7,0,2,7),up=points(0,0,1,0,0,1,0,0,1),down=points(0,0,-1,0,0,-1,0,0,-1);
  Map<String,Object> pair=GeometryPairEvidence.analyze("PLANE_PLANE",a,up,b,down);check(pair.get("candidate").equals(true));check(pair.get("sampleNormalDotMin").equals(-1.0));check(pair.get("trimmedOverlapProven").equals(false));check(pair.get("thicknessAssessed").equals(false));check(pair.get("trueOppositeFacesConfirmed").equals(false));
  check(GeometryPairEvidence.analyze("PLANE_PLANE",a,up,a,up).get("candidate").equals(true)); // A hint can be coplanar, so it is never confirmed opposite faces.
  check(GeometryPairEvidence.analyze("PLANE_PLANE",a,up,points(9,9,0,10,9,0,9,10,0),down).get("candidate").equals(false));
  check(GeometryPairEvidence.analyze("PLANE_PLANE",a,up,b,points(1,0,0,1,0,0,1,0,0)).get("candidate").equals(false));
  pair=GeometryPairEvidence.analyze("CYLINDER_CYLINDER",a,up,b,down);check(pair.get("cylinderAxisCoaxialityAndRadii").equals("UNKNOWN_NOT_ACQUIRED"));check(pair.get("trueOppositeFacesConfirmed").equals(false));
  check(GeometryPairEvidence.analyze("PLANE_PLANE",a,points(0,0,0,0,0,0,0,0,0),b,down).get("status").equals("UNKNOWN_INVALID_SAMPLE_DATA"));
  check(GeometryPairEvidence.analyze("PLANE_CYLINDER",a,up,b,down).get("candidate").equals(false));
  check(GeometryPairEvidence.analyze("PLANE_PLANE",points(Double.NaN,0,0,1,0,0,0,1,0),up,b,down).get("candidate").equals(false));
  check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==256*Math.ulp(1.0));check(GeometryPairEvidence.MAX_COMPARISONS==16000&&GeometryPairEvidence.MAX_EMITTED_PER_TYPE==64);
  Map<String,Object> comparison=GeometryReferenceBaseline.compare("200002100.prt","3071","mm",2689,GeometrySamplingMath.xyzQuality(a));check(comparison.get("referenceFieldsMatch").equals(true));check(comparison.get("historicalMetricsUsedForSelection").equals(false));check(comparison.get("crossRunIdentityAuthenticated").equals(false));
  check(GeometryReferenceBaseline.compare("200002100.prt","changed","mm",2689,GeometrySamplingMath.xyzQuality(a)).get("referenceFieldsMatch").equals(false));
  System.out.println("STEP6_EVIDENCE_CHECKS_PASSED="+checks);
 }
}
