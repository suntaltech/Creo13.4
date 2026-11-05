package p1.readonly.geometry.step2;

import java.util.*;

/** Pure numerical policy, no SDK calls. Not a wall-thickness classifier. */
public final class GeometrySamplingMath {
    public static final int TARGET_PER_FACE = 3, CANDIDATES_PER_FACE = 25, MAX_TARGET_FACES = 300,
            MAX_TOTAL_ATTEMPTS = 3000, MAX_ACCEPTED_SAMPLES = 900;
    private GeometrySamplingMath() { }
    public static boolean validBounds(double lo, double hi) {
        return Double.isFinite(lo) && Double.isFinite(hi) && hi > lo && Double.isFinite(hi - lo);
    }
    public static List<double[]> candidates(double u0, double u1, double v0, double v1) {
        if (!validBounds(u0, u1) || !validBounds(v0, v1)) throw new IllegalArgumentException("UV_EXTENTS_INVALID_OR_DEGENERATE");
        List<double[]> fractions = new ArrayList<>();
        double[][] first = {{.5,.5},{.25,.25},{.75,.25},{.25,.75},{.75,.75}};
        for (double[] pair : first) fractions.add(pair);
        for (double u : new double[]{.125,.25,.5,.75,.875}) for (double v : new double[]{.125,.25,.5,.75,.875}) {
            boolean seen = false;
            for (double[] pair : fractions) if (pair[0] == u && pair[1] == v) { seen = true; break; }
            if (!seen) fractions.add(new double[]{u,v});
        }
        List<double[]> result = new ArrayList<>();
        for (double[] pair : fractions) {
            double u = u0 + (u1-u0)*pair[0], v = v0 + (v1-v0)*pair[1];
            if (!Double.isFinite(u) || !Double.isFinite(v)) throw new IllegalArgumentException("UV_INTERPOLATION_NONFINITE");
            boolean duplicate = false;
            for (double[] prior : result) if (prior[0] == u && prior[1] == v) { duplicate = true; break; }
            if (!duplicate) result.add(new double[]{u,v});
        }
        return result;
    }
    public static boolean finite3(double[] value) {
        return value != null && value.length == 3 && Double.isFinite(value[0]) && Double.isFinite(value[1]) && Double.isFinite(value[2]);
    }
    public static double[] normalUnit(double[] raw) {
        if (!finite3(raw)) throw new IllegalArgumentException("NORMAL_NONFINITE");
        double size = Math.hypot(Math.hypot(raw[0],raw[1]),raw[2]);
        if (!Double.isFinite(size) || size == 0) throw new IllegalArgumentException("NORMAL_ZERO_OR_NONFINITE_LENGTH");
        return new double[]{raw[0]/size,raw[1]/size,raw[2]/size};
    }
    public static boolean samePoint(double[] a, double[] b) {
        return finite3(a) && finite3(b) && a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }
    public static boolean insidePlacement(Integer returnedCode, int nativeInsideCode) {
        return returnedCode != null && returnedCode == nativeInsideCode;
    }
    public static boolean exactUv(double[] expected, double[] actual) {
        return expected != null && actual != null && expected.length == 2 && actual.length == 2
                && Double.isFinite(expected[0]) && Double.isFinite(expected[1])
                && expected[0] == actual[0] && expected[1] == actual[1];
    }
    public static List<Double> list(double[] array) {
        List<Double> value = new ArrayList<>();
        for (double v : array) { if (!Double.isFinite(v)) throw new IllegalArgumentException("NONFINITE_VECTOR"); value.add(v); }
        return value;
    }
}
