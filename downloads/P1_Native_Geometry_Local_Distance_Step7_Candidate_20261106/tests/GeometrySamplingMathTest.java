import p1.readonly.geometry.step7.GeometrySamplingMath;
import java.util.*;

/** Synthetic numerical/policy checks; not native trimming/normal verification. */
public final class GeometrySamplingMathTest {
    private static int checks;
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("math check " + checks); }
    private static boolean rejects(Runnable action) { try { action.run(); return false; } catch (IllegalArgumentException expected) { return true; } }
    public static void main(String[] args) {
        List<double[]> grid = GeometrySamplingMath.candidates(-3,7,-5,8);
        check(grid.size()==25);
        Set<String> unique = new HashSet<>();
        for (double[] uv:grid) { check(uv[0]>-3&&uv[0]<7&&uv[1]>-5&&uv[1]<8); unique.add(uv[0]+":"+uv[1]); }
        check(unique.size()==25);
        double[] a=grid.get(0), b=grid.get(1), c=grid.get(2);
        check((b[0]-a[0])*(c[1]-a[1])-(c[0]-a[0])*(b[1]-a[1])!=0);
        check(rejects(()->GeometrySamplingMath.candidates(1,1,0,1)));
        check(rejects(()->GeometrySamplingMath.candidates(2,1,0,1)));
        check(rejects(()->GeometrySamplingMath.candidates(Double.NaN,1,0,1)));
        check(rejects(()->GeometrySamplingMath.candidates(0,Double.POSITIVE_INFINITY,0,1)));
        check(rejects(()->GeometrySamplingMath.candidates(-Double.MAX_VALUE,Double.MAX_VALUE,0,1)));
        check(GeometrySamplingMath.insidePlacement(0,0));
        check(!GeometrySamplingMath.insidePlacement(1,0));
        check(!GeometrySamplingMath.insidePlacement(2,0));
        check(!GeometrySamplingMath.insidePlacement(null,0));
        check(!GeometrySamplingMath.insidePlacement(-1,0));
        check(GeometrySamplingMath.exactUv(new double[]{1,2},new double[]{1,2}));
        check(GeometrySamplingMath.exactUv(new double[]{0,2},new double[]{-0.0,2}));
        check(!GeometrySamplingMath.exactUv(new double[]{1,2},new double[]{Math.nextUp(1),2}));
        check(!GeometrySamplingMath.exactUv(new double[]{1,2},new double[]{Double.NaN,2}));
        check(!GeometrySamplingMath.exactUv(new double[]{1,2},null));
        double[] normal=GeometrySamplingMath.normalUnit(new double[]{0,3,4});
        check(Math.abs(normal[1]-.6)<1e-15&&Math.abs(normal[2]-.8)<1e-15);
        check(Math.abs(Math.hypot(Math.hypot(normal[0],normal[1]),normal[2])-1)<1e-15);
        check(GeometrySamplingMath.normalUnit(new double[]{Double.MIN_VALUE,0,0})[0]==1);
        check(GeometrySamplingMath.normalUnit(new double[]{1e308,0,0})[0]==1);
        check(rejects(()->GeometrySamplingMath.normalUnit(new double[]{0,0,0})));
        check(rejects(()->GeometrySamplingMath.normalUnit(new double[]{Double.NaN,0,0})));
        check(rejects(()->GeometrySamplingMath.normalUnit(new double[]{Double.MAX_VALUE,Double.MAX_VALUE,0})));
        check(!GeometrySamplingMath.finite3(new double[]{1,2,Double.POSITIVE_INFINITY}));
        check(!GeometrySamplingMath.finite3(new double[]{1,2}));
        check(rejects(()->GeometrySamplingMath.list(new double[]{Double.NaN})));
        check(GeometrySamplingMath.TARGET_PER_FACE==3&&GeometrySamplingMath.CANDIDATES_PER_FACE==25);
        check(GeometrySamplingMath.MAX_TARGET_FACES*GeometrySamplingMath.TARGET_PER_FACE==GeometrySamplingMath.MAX_ACCEPTED_SAMPLES);
        check(GeometrySamplingMath.MAX_TOTAL_ATTEMPTS==3000);
        check(GeometrySamplingMath.candidates(1,Math.nextUp(1.0),1,Math.nextUp(1.0)).size()<=4);
        check(GeometrySamplingMath.samePoint(new double[]{0,1,2},new double[]{-0.0,1,2}));
        check(!GeometrySamplingMath.samePoint(new double[]{0,1,2},new double[]{0,Math.nextUp(1.0),2}));
        System.out.println("MATH_POLICY_CHECKS_PASSED="+checks);
    }
}
