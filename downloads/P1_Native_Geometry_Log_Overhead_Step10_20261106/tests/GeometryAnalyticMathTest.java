import java.util.*;
import p1.readonly.geometry.step10.*;
/** Analytic formula invariants only; does not simulate native descriptor or VerifyUV behavior. */
public final class GeometryAnalyticMathTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("analytic check "+checks);}
 static List<double[]> ps(double... v){List<double[]> p=new ArrayList<>();for(int i=0;i<v.length;i+=3)p.add(new double[]{v[i],v[i+1],v[i+2]});return p;}
 static void near(double a,double b){check(Math.abs(a-b)<1e-12);}
 public static void main(String[] args){
  double[] o={0,0,0},x={1,0,0},y={0,1,0},z={0,0,1};List<double[]> plane=ps(0,0,0,2,0,0,0,2,0),up=ps(0,0,1,0,0,1,0,0,1);
  check(GeometryAnalyticMath.validate("PLANE",o,x,y,z,null,plane,up).get("parametersConsistentWithSamples").equals(true));
  check(GeometryAnalyticMath.validate("PLANE",new double[]{0,0,.01},x,y,z,null,plane,up).get("parametersConsistentWithSamples").equals(false));
  check(GeometryAnalyticMath.validate("PLANE",o,x,new double[]{1,1,0},z,null,plane,up).get("parametersConsistentWithSamples").equals(false));
  check(GeometryAnalyticMath.validate("PLANE",o,x,y,z,null,plane,ps(1,0,0,1,0,0,1,0,0)).get("parametersConsistentWithSamples").equals(false));
  List<double[]> cyl=ps(2,0,0,0,2,1,-2,0,2),radial=ps(1,0,0,0,1,0,-1,0,0);
  check(GeometryAnalyticMath.validate("CYLINDER",o,x,y,z,2.0,cyl,radial).get("parametersConsistentWithSamples").equals(true));
  for(Double radius:Arrays.asList(2.1,0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))check(GeometryAnalyticMath.validate("CYLINDER",o,x,y,z,radius,cyl,radial).get("parametersConsistentWithSamples").equals(false));
  check(GeometryAnalyticMath.validate("CYLINDER",o,x,z,y,2.0,cyl,radial).get("parametersConsistentWithSamples").equals(false));
  check(GeometryAnalyticMath.validate("PLANE",new double[]{Double.NaN,0,0},x,y,z,null,plane,up).get("parametersConsistentWithSamples").equals(false));
  double[] q=GeometryAnalyticMath.project("PLANE",new double[]{1,2,3},o,z,null);near(q[0],1);near(q[1],2);near(q[2],0);
  q=GeometryAnalyticMath.project("CYLINDER",new double[]{3,4,8},o,z,2.0);near(q[0],1.2);near(q[1],1.6);near(q[2],8);
  check(GeometryAnalyticMath.project("CYLINDER",new double[]{0,0,8},o,z,2.0)==null);
  check(GeometryAnalyticMath.project("CYLINDER",new double[]{1,0,0},o,z,-1.0)==null);
  check(GeometryAnalyticMath.project("PLANE",new double[]{Double.NaN,0,0},o,z,null)==null);
  Map<String,Object> pair=GeometryAnalyticMath.pair("PLANE",o,z,null,new double[]{0,0,4},new double[]{0,0,-1},null);near((Double)pair.get("supportingGapCandidate"),4);
  check(pair.get("finalWallThickness").equals("NOT_ASSESSED"));
  check(GeometryAnalyticMath.pair("PLANE",o,z,null,o,new double[]{1e-7,0,1},null).get("supportingGapCandidate").equals("UNKNOWN"));
  pair=GeometryAnalyticMath.pair("CYLINDER",o,z,2.0,new double[]{0,0,9},new double[]{0,0,-1},3.0);near((Double)pair.get("supportingGapCandidate"),1);check(pair.get("axesNumericallyCoaxialCandidates").equals(true));
  pair=GeometryAnalyticMath.pair("CYLINDER",o,z,2.0,new double[]{.1,0,0},z,3.0);check(pair.get("supportingGapCandidate").equals("UNKNOWN"));check(pair.get("axesNumericallyCoaxialCandidates").equals(false));
  double[] p={400,200,100};check(GeometryAnalyticMath.roundTrip(p,p).get("roundTripMatchesWithinNumericGuard").equals(true));
  check(GeometryAnalyticMath.roundTrip(p,new double[]{400.00001,200,100}).get("roundTripMatchesWithinNumericGuard").equals(false));
  check(GeometryAnalyticMath.roundTrip(p,new double[]{Double.NaN,200,100}).get("roundTripMatchesWithinNumericGuard").equals(false));
  check(GeometryAnalyticMath.roundTrip(p,p).get("guardIsModelTolerance").equals(false));
  // World translations preserve the formula; native trim acceptance remains unknown in every case.
  for(int i=0;i<20;i++){
   double[] origin={i*10,i*-3,5},input={i*10+2,i*-3+4,9};q=GeometryAnalyticMath.project("PLANE",input,origin,z,null);near(q[0],input[0]);near(q[1],input[1]);near(q[2],5);
  }
  check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==256*Math.ulp(1.0));check(GeometryAnalyticMath.MAX_DISTANCE_PAIRS_PER_TYPE==16&&GeometryAnalyticMath.MAX_DOMAIN_EVALUATIONS==768);
  System.out.println("ANALYTIC_MATH_CHECKS_PASSED="+checks);
 }
}
