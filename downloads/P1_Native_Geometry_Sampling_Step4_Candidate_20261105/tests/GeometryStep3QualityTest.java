import p1.readonly.geometry.step4.GeometrySamplingMath;
import p1.readonly.geometry.step4.GeometryEvidenceSupport;
import java.util.*;

/** Pure synthetic numerical checks, never simulated native domain membership. */
public final class GeometryStep3QualityTest {
    private static int checks;
    private static void check(boolean value) { checks++; if(!value) throw new AssertionError("step3 check "+checks); }
    private static List<double[]> triangle(double... v) {
        return Arrays.asList(new double[]{v[0],v[1],v[2]},new double[]{v[3],v[4],v[5]},new double[]{v[6],v[7],v[8]});
    }
    public static void main(String[] args) {
        List<double[]> base=GeometrySamplingMath.candidates(0,1,0,1);
        List<double[]> fine=GeometrySamplingMath.refinementCandidates(0,1,0,1,base);
        check(base.size()==25&&fine.size()==200);
        Set<String> all=new HashSet<>();
        for(double[] uv:base) all.add(uv[0]+":"+uv[1]);
        for(double[] uv:fine) { check(uv[0]>0&&uv[0]<1&&uv[1]>0&&uv[1]<1);check(all.add(uv[0]+":"+uv[1])); }
        check(all.size()==225);
        check(GeometrySamplingMath.refinementApplies("200002100.PRT",2710,true));
        check(!GeometrySamplingMath.refinementApplies("other.prt",2710,true));
        check(!GeometrySamplingMath.refinementApplies(null,2710,true));
        check(!GeometrySamplingMath.refinementApplies("200002100.prt",2710,false));
        check(!GeometrySamplingMath.refinementApplies("200002100.prt",1,true));
        check(!GeometrySamplingMath.refinementApplies("200002100.prt",null,true));
        for(int id:new int[]{2710,2738,2752,2843}) check(GeometrySamplingMath.refinementApplies("200002100.prt",id,true));
        boolean immutable=false;try { GeometrySamplingMath.REFINEMENT_SURFACE_IDS.add(1); } catch(UnsupportedOperationException expected) { immutable=true; }check(immutable);
        Map<String,Object> q=GeometrySamplingMath.xyzQuality(triangle(0,0,0,2,0,0,0,2,0));
        check(q.get("numericallyNonCollinear").equals(true));
        check(Math.abs((Double)q.get("xyzNormalizedTriangleDoubleArea")-.5)<1e-14);
        check(Math.abs((Double)q.get("xyzTriangleArea")-2)<1e-14);
        Map<String,Object> moved=GeometrySamplingMath.xyzQuality(triangle(100,100,100,114,100,100,100,114,100));
        check(Math.abs((Double)moved.get("xyzNormalizedTriangleDoubleArea")-(Double)q.get("xyzNormalizedTriangleDoubleArea"))<1e-14);
        check(GeometrySamplingMath.xyzQuality(triangle(0,0,0,1,0,0,2,0,0)).get("numericallyNonCollinear").equals(false));
        check(GeometrySamplingMath.xyzQuality(triangle(0,0,0,1,0,0,.5,1e-16,0)).get("numericallyNonCollinear").equals(false));
        check(GeometrySamplingMath.xyzQuality(Arrays.asList(new double[]{1,2,3})).get("numericallyNonCollinear").equals("UNKNOWN"));
        check(GeometrySamplingMath.xyzQuality(triangle(-Double.MAX_VALUE,0,0,Double.MAX_VALUE,0,0,0,1,0)).get("numericallyNonCollinear").equals("UNKNOWN"));
        Map<String,Object> uvq=GeometrySamplingMath.uvQuality(Arrays.asList(new double[]{.25,.25},new double[]{.75,.25},new double[]{.5,.75}),new double[]{0,1,0,1});
        check(Math.abs((Double)uvq.get("uvNormalizedBoxAreaFraction")-.25)<1e-15);
        check(Math.abs((Double)uvq.get("uvNormalizedMinPairDistance")-.5)<1e-15);
        check(uvq.get("uvSpreadMeansPhysicalAreaCoverage").equals(false));
        check(GeometrySamplingMath.uvQuality(base,null).get("uvNormalizedBoxAreaFraction").equals("UNKNOWN"));
        check(GeometrySamplingMath.safeValues(null).equals("UNKNOWN"));
        check(GeometryEvidenceSupport.json(GeometrySamplingMath.safeValues(new double[]{Double.NaN,Double.POSITIVE_INFINITY})).equals("[\"NaN\",\"Infinity\"]"));
        check(GeometrySamplingMath.refinementCandidates(1,Math.nextUp(1.0),1,Math.nextUp(1.0),GeometrySamplingMath.candidates(1,Math.nextUp(1.0),1,Math.nextUp(1.0))).size()==0);
        System.out.println("STEP3_QUALITY_CHECKS_PASSED="+checks);
    }
}
