import p1.readonly.geometry.step10.*;
import java.util.*;

/** Synthetic point arithmetic and bounded seed policy; no SDK or model mock. */
public final class GeometryPoolSelectionTest {
    private static int checks;
    private static void check(boolean value) { checks++;if(!value) throw new AssertionError("pool check "+checks); }
    private static List<double[]> points(double... v) {
        List<double[]> result=new ArrayList<>();for(int i=0;i<v.length;i+=3) result.add(new double[]{v[i],v[i+1],v[i+2]});return result;
    }
    private static List<double[]> chosen(List<double[]> pool,Map<String,Object> selection) {
        List<double[]> result=new ArrayList<>();for(Object i:(Iterable<?>)selection.get("selectedIndices")) result.add(pool.get((Integer)i));return result;
    }
    public static void main(String[] args) {
        double originalThreshold=256.0*Math.ulp(1.0);
        check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==originalThreshold);
        List<double[]> pool=points(0,0,0,1,0,0,2,0,0,0,2,0,2,2,0,1,3,0);
        Map<String,Object> selected=GeometryPoolSelection.select(pool);
        check(selected.get("initialNumericallyNonCollinear").equals(false));
        check(selected.get("finalNumericallyNonCollinear").equals(true));
        check(selected.get("replacementApplied").equals(true));
        check((Double)selected.get("selectedAreaScore")>=(Double)selected.get("baselineAreaScore"));
        check(GeometrySamplingMath.xyzQuality(chosen(pool,selected)).get("numericallyNonCollinear").equals(true));
        check(new HashSet<>((Collection<?>)selected.get("selectedIndices")).size()==3);
        List<double[]> line=points(0,0,0,1,0,0,2,0,0,3,0,0,4,0,0,5,0,0,6,0,0,7,0,0,8,0,0);
        Map<String,Object> unchanged=GeometryPoolSelection.select(line);
        check(unchanged.get("replacementApplied").equals(false));
        check(unchanged.get("finalNumericallyNonCollinear").equals(false));
        check(unchanged.get("selectedIndices").equals(Arrays.asList(0,1,2)));
        List<double[]> near=points(0,0,0,1,0,0,.5,1e-16,0,0,1,0);
        Map<String,Object> fixed=GeometryPoolSelection.select(near);
        check(fixed.get("initialNumericallyNonCollinear").equals(false)&&fixed.get("finalNumericallyNonCollinear").equals(true));
        check(GeometryPoolSelection.select(points()).get("selectedIndices").equals(Collections.emptyList()));
        check(GeometryPoolSelection.select(points(1,2,3,2,3,4)).get("selectedIndices").equals(Arrays.asList(0,1)));
        List<double[]> invalid=points(0,0,0,1,0,0,2,0,0,Double.NaN,1,0);
        check(GeometryPoolSelection.select(invalid).get("replacementApplied").equals(false));
        boolean rejected=false;try { GeometryPoolSelection.select(points(0,0,0,1,0,0,2,0,0,3,0,0,4,0,0,5,0,0,6,0,0,7,0,0,8,0,0,9,0,0)); }catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        List<double[]> shifted=new ArrayList<>();for(double[] p:pool) shifted.add(new double[]{100+p[0]*7,100+p[1]*7,100+p[2]*7});
        Map<String,Object> stable=GeometryPoolSelection.select(shifted);
        check(stable.get("finalNumericallyNonCollinear").equals(true));
        check((Double)stable.get("selectedAreaScore")>=(Double)stable.get("baselineAreaScore"));
        List<double[]> attempted=GeometrySamplingMath.candidates(0,1,0,1);
        List<double[]> extra=GeometrySamplingMath.qualityCandidates(0,1,0,1,attempted);
        check(extra.size()<=264&&extra.size()>GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE);
        Set<String> seen=new HashSet<>();for(double[] uv:attempted) seen.add(uv[0]+":"+uv[1]);
        for(double[] uv:extra) { check(uv[0]>0&&uv[0]<1&&uv[1]>0&&uv[1]<1);check(seen.add(uv[0]+":"+uv[1])); }
        check(GeometrySamplingMath.MAX_QUALITY_POOL_SIZE==9&&GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE==32);
        check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==originalThreshold);
        System.out.println("POOL_SELECTION_CHECKS_PASSED="+checks);
    }
}
