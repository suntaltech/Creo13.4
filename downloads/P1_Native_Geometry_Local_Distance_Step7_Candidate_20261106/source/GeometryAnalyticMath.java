package p1.readonly.geometry.step7;
import java.util.*;
/** Parameter hypotheses and local point arithmetic only. No native placements are simulated. */
public final class GeometryAnalyticMath {
 public static final double ANGULAR_NUMERIC_GUARD=4096*Math.ulp(1.0);
 public static final int MAX_DISTANCE_PAIRS_PER_TYPE=16,MAX_DOMAIN_EVALUATIONS=768;
 private GeometryAnalyticMath() { }
 public static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
 public static double[] sub(double[] a,double[] b){return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
 public static double crossLength(double[] a,double[] b){return length(new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]});}
 public static double length(double[] p){return Math.hypot(Math.hypot(p[0],p[1]),p[2]);}
 public static double guard(double[]... values){double scale=1;for(double[] p:values)if(p!=null)for(double v:p){if(!Double.isFinite(v))return Double.NaN;scale=Math.max(scale,Math.abs(v));}return 512*Math.ulp(scale);}
 public static Map<String,Object> validate(String kind,double[] origin,double[] x,double[] y,double[] z,Double radius,List<double[]> points,List<double[]> normals){
  Map<String,Object> r=GeometryEvidenceSupport.map("parametersConsistentWithSamples",false,"status","UNKNOWN_INVALID_ANALYTIC_PARAMETERS",
   "axisConvention","TRANSFORM_Z_AXIS_CANDIDATE_CHECKED_WITH_NATIVE_SAMPLES_NOT_OFFICIAL_SEMANTIC_CERTIFICATION",
   "angularNumericGuard",ANGULAR_NUMERIC_GUARD,"numericGuardMeaning","FLOATING_POINT_REPRESENTATION_POLICY_NOT_MODEL_TOLERANCE");
  if(!Arrays.asList("PLANE","CYLINDER").contains(kind)||!GeometrySamplingMath.finite3(origin)||points==null||normals==null||points.size()!=3||normals.size()!=3)return r;
  double[] nx,ny,nz;
  try{nx=GeometrySamplingMath.normalUnit(x);ny=GeometrySamplingMath.normalUnit(y);nz=GeometrySamplingMath.normalUnit(z);}catch(IllegalArgumentException e){return r;}
  if(Math.max(Math.abs(dot(nx,ny)),Math.max(Math.abs(dot(nx,nz)),Math.abs(dot(ny,nz))))>ANGULAR_NUMERIC_GUARD){r.put("status","UNKNOWN_NONORTHOGONAL_COORDINATE_AXES");return r;}
  if(kind.equals("CYLINDER")&&(radius==null||!Double.isFinite(radius)||radius<=0)){r.put("status","UNKNOWN_INVALID_RADIUS");return r;}
  double worst=0,normalResidual=0,signedMin=1,signedMax=-1,g=guard(origin,new double[]{radius==null?0:radius});
  for(int i=0;i<3;i++){
   if(!GeometrySamplingMath.finite3(points.get(i)))return r;
   double[] normal;try{normal=GeometrySamplingMath.normalUnit(normals.get(i));}catch(IllegalArgumentException e){return r;}
   double[] delta=sub(points.get(i),origin);if(!GeometrySamplingMath.finite3(delta))return r;
   g=Math.max(g,guard(origin,points.get(i)));double along=dot(delta,nz),sd=dot(normal,nz);signedMin=Math.min(signedMin,sd);signedMax=Math.max(signedMax,sd);
   if(kind.equals("PLANE")){worst=Math.max(worst,Math.abs(along));normalResidual=Math.max(normalResidual,crossLength(normal,nz));}
   else{
    double[] radial={delta[0]-along*nz[0],delta[1]-along*nz[1],delta[2]-along*nz[2]};double len=length(radial);
    if(!Double.isFinite(len)||len==0)return r;
    worst=Math.max(worst,Math.abs(len-radius));double radialDot=dot(normal,new double[]{radial[0]/len,radial[1]/len,radial[2]/len});
    normalResidual=Math.max(normalResidual,Math.max(Math.abs(sd),crossLength(normal,new double[]{radial[0]/len,radial[1]/len,radial[2]/len})));
   }
  }
  boolean finite=Double.isFinite(worst)&&Double.isFinite(normalResidual)&&Double.isFinite(g);
  boolean valid=finite&&worst<=g&&normalResidual<=ANGULAR_NUMERIC_GUARD;
  r.put("parametersConsistentWithSamples",valid);r.put("status",valid?"ANALYTIC_AXIS_CANDIDATE_CONSISTENT_WITH_THREE_NATIVE_SAMPLES":"UNKNOWN_ANALYTIC_CANDIDATE_SAMPLE_MISMATCH");
  r.put("sampleResidualMax",finite?worst:"UNKNOWN_NONFINITE");r.put("normalResidualMax",finite?normalResidual:"UNKNOWN_NONFINITE");r.put("lengthNumericGuard",finite?g:"UNKNOWN_NONFINITE");
  r.put("sampleNormalDotZMin",signedMin);r.put("sampleNormalDotZMax",signedMax);return r;
 }
 public static double[] project(String kind,double[] point,double[] origin,double[] axis,Double radius){
  if(!GeometrySamplingMath.finite3(point)||!GeometrySamplingMath.finite3(origin))return null;double[] n;
  try{n=GeometrySamplingMath.normalUnit(axis);}catch(IllegalArgumentException e){return null;}
  double[] d=sub(point,origin);if(!GeometrySamplingMath.finite3(d))return null;double t=dot(d,n);double[] q;
  if(kind.equals("PLANE"))q=new double[]{point[0]-t*n[0],point[1]-t*n[1],point[2]-t*n[2]};
  else if(kind.equals("CYLINDER")&&radius!=null&&Double.isFinite(radius)&&radius>0){double[] radial={d[0]-t*n[0],d[1]-t*n[1],d[2]-t*n[2]};double len=length(radial);if(!Double.isFinite(len)||len==0)return null;q=new double[]{origin[0]+t*n[0]+radius*radial[0]/len,origin[1]+t*n[1]+radius*radial[1]/len,origin[2]+t*n[2]+radius*radial[2]/len};}
  else return null;
  return GeometrySamplingMath.finite3(q)?q:null;
 }
 public static Map<String,Object> pair(String kind,double[] oa,double[] za,Double ra,double[] ob,double[] zb,Double rb){
  Map<String,Object> r=GeometryEvidenceSupport.map("supportingGapCandidate","UNKNOWN","gapMeaning","ANALYTIC_SUPPORT_SURFACE_CANDIDATE_NOT_LOCAL_TRIMMED_DISTANCE_OR_WALL_THICKNESS","finalWallThickness","NOT_ASSESSED");
  if(!GeometrySamplingMath.finite3(oa)||!GeometrySamplingMath.finite3(ob))return r;double[] a,b;
  try{a=GeometrySamplingMath.normalUnit(za);b=GeometrySamplingMath.normalUnit(zb);}catch(IllegalArgumentException e){return r;}
  double d=dot(a,b);r.put("analyticAxisDot",d);double cross=crossLength(a,b);r.put("analyticAxisCrossMagnitude",cross);boolean parallel=cross<=ANGULAR_NUMERIC_GUARD;r.put("axesNumericallyParallel",parallel);
  if(!parallel){r.put("status","NONPARALLEL_NO_CONSTANT_SUPPORT_GAP");return r;}
  double[] delta=sub(ob,oa);if(!GeometrySamplingMath.finite3(delta))return r;double along=dot(delta,a);
  if(kind.equals("PLANE")){double g=Math.abs(along);if(Double.isFinite(g))r.put("supportingGapCandidate",g);r.put("status","PARALLEL_SUPPORT_PLANE_OFFSET_CANDIDATE");}
  else if(kind.equals("CYLINDER")){
   double[] off={delta[0]-along*a[0],delta[1]-along*a[1],delta[2]-along*a[2]};double residual=length(off),g=guard(oa,ob);boolean coaxial=Double.isFinite(residual)&&residual<=g;
   r.put("axisPerpendicularOffsetCandidate",Double.isFinite(residual)?residual:"UNKNOWN");r.put("axisOffsetNumericGuard",g);r.put("axesNumericallyCoaxialCandidates",coaxial);
   if(coaxial&&ra!=null&&rb!=null&&Double.isFinite(ra)&&Double.isFinite(rb)&&ra>0&&rb>0)r.put("supportingGapCandidate",Math.abs(ra-rb));
   r.put("status",coaxial?"COAXIAL_CYLINDER_RADIUS_GAP_CANDIDATE":"NONCOAXIAL_RADIUS_GAP_UNKNOWN");
  }
  return r;
 }
 public static Map<String,Object> roundTrip(double[] requested,double[] actual){double residual=GeometrySamplingMath.finite3(requested)&&GeometrySamplingMath.finite3(actual)?length(sub(actual,requested)):Double.NaN;double g=guard(requested,actual);boolean ok=Double.isFinite(residual)&&Double.isFinite(g)&&residual<=g;return GeometryEvidenceSupport.map("roundTripMatchesWithinNumericGuard",ok,"xyzRoundTripResidual",Double.isFinite(residual)?residual:"UNKNOWN","xyzNumericGuard",Double.isFinite(g)?g:"UNKNOWN","guardIsModelTolerance",false);}
}
