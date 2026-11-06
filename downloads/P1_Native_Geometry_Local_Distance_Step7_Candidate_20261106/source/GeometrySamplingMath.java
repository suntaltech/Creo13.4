package p1.readonly.geometry.step7;

import java.util.*;

/** Pure numerical policy, no SDK calls. Not a wall-thickness classifier. */
public final class GeometrySamplingMath {
    public static final int TARGET_PER_FACE = 3, CANDIDATES_PER_FACE = 25, MAX_TARGET_FACES = 300,
            MAX_TOTAL_ATTEMPTS = 3000, MAX_ACCEPTED_SAMPLES = 900;
    public static final String REFINEMENT_MODEL_FILE = "200002100.prt";
    public static final Set<Integer> REFINEMENT_SURFACE_IDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(2710,2738,2752,2843)));
    public static final int REFINEMENT_CANDIDATES_PER_FACE = 200;
    public static final double NUMERICAL_TRIANGLE_QUALITY_THRESHOLD = 256.0 * Math.ulp(1.0);
    public static boolean refinementApplies(String modelFile, Integer surfaceId, boolean plane) {
        return plane && REFINEMENT_MODEL_FILE.equalsIgnoreCase(modelFile) && REFINEMENT_SURFACE_IDS.contains(surfaceId);
    }
    public static List<double[]> refinementCandidates(double u0,double u1,double v0,double v1,List<double[]> base) {
        if (!validBounds(u0,u1)||!validBounds(v0,v1)||base==null) throw new IllegalArgumentException("REFINEMENT_RANGE_INVALID");
        List<double[]> fractions = new ArrayList<>();
        for (double edge : new double[]{1.0/1024,1.0/256,1.0/64,1.0/32,1.0/16}) {
            fractions.add(new double[]{edge,edge}); fractions.add(new double[]{1-edge,1-edge});
            fractions.add(new double[]{edge,1-edge}); fractions.add(new double[]{1-edge,edge});
            fractions.add(new double[]{edge,.5}); fractions.add(new double[]{1-edge,.5});
            fractions.add(new double[]{.5,edge}); fractions.add(new double[]{.5,1-edge});
        }
        double[] values={1.0/1024,1.0/256,1.0/64,1.0/32,1.0/16,.125,.25,.5,.75,.875,15.0/16,31.0/32,63.0/64,255.0/256,1023.0/1024};
        for (double u:values) for(double v:values) fractions.add(new double[]{u,v});
        List<double[]> result = new ArrayList<>();
        for(double[] fraction:fractions) {
            double[] value={u0+(u1-u0)*fraction[0],v0+(v1-v0)*fraction[1]};
            if (!Double.isFinite(value[0])||!Double.isFinite(value[1])) throw new IllegalArgumentException("REFINEMENT_NONFINITE");
            boolean duplicate=false;
            for(double[] prior:base) if(exactUv(prior,value)) { duplicate=true;break; }
            if(!duplicate) for(double[] prior:result) if(exactUv(prior,value)) { duplicate=true;break; }
            if(!duplicate) result.add(value);
            if(result.size()==REFINEMENT_CANDIDATES_PER_FACE) break;
        }
        return result;
    }
    public static Object safeValues(double[] values) {
        if(values==null) return "UNKNOWN";
        List<Object> result=new ArrayList<>();
        for(double value:values) result.add(Double.isFinite(value)?value:Double.toString(value));
        return result;
    }
    public static boolean boundsValid(double[] bounds) {
        return bounds!=null&&bounds.length==4&&validBounds(bounds[0],bounds[1])&&validBounds(bounds[2],bounds[3]);
    }
    public static Map<String,Object> xyzQuality(List<double[]> points) {
        Map<String,Object> result=GeometryEvidenceSupport.map("xyzQualityStatus","INSUFFICIENT_ACCEPTED_POINTS",
                "numericallyNonCollinear","UNKNOWN","xyzNormalizedTriangleDoubleArea","UNKNOWN",
                "xyzMinPairDistance","UNKNOWN","xyzMaxPairDistance","UNKNOWN","xyzMinToMaxPairDistanceRatio","UNKNOWN",
                "xyzTriangleArea","UNKNOWN","numericalQualityThreshold",NUMERICAL_TRIANGLE_QUALITY_THRESHOLD,
                "thresholdMeaning","FLOATING_POINT_NUMERICAL_POLICY_NOT_MODEL_TOLERANCE");
        if(points==null||points.size()!=3) return result;
        for(double[] point:points) if(!finite3(point)) { result.put("xyzQualityStatus","UNKNOWN_NONFINITE_POINT"); return result; }
        double[] ab=new double[3],ac=new double[3],bc=new double[3];
        for(int i=0;i<3;i++) { ab[i]=points.get(1)[i]-points.get(0)[i]; ac[i]=points.get(2)[i]-points.get(0)[i]; bc[i]=points.get(2)[i]-points.get(1)[i]; }
        if(!finite3(ab)||!finite3(ac)||!finite3(bc)) { result.put("xyzQualityStatus","UNKNOWN_NUMERICAL_RANGE"); return result; }
        double dab=Math.hypot(Math.hypot(ab[0],ab[1]),ab[2]),dac=Math.hypot(Math.hypot(ac[0],ac[1]),ac[2]),dbc=Math.hypot(Math.hypot(bc[0],bc[1]),bc[2]);
        double maximum=Math.max(dab,Math.max(dac,dbc)),minimum=Math.min(dab,Math.min(dac,dbc));
        if(!Double.isFinite(maximum)) { result.put("xyzQualityStatus","UNKNOWN_NUMERICAL_RANGE"); return result; }
        double quality=0;
        if(maximum>0) {
            for(int i=0;i<3;i++) { ab[i]/=maximum; ac[i]/=maximum; }
            quality=Math.hypot(Math.hypot(ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2]),ab[0]*ac[1]-ab[1]*ac[0]);
        }
        double area=(quality/2)*maximum*maximum;
        result.put("xyzQualityStatus","METRICS_RECORDED_NUMERICAL_ONLY");
        result.put("numericallyNonCollinear",quality>NUMERICAL_TRIANGLE_QUALITY_THRESHOLD);
        result.put("xyzNormalizedTriangleDoubleArea",quality);
        result.put("xyzMinPairDistance",minimum); result.put("xyzMaxPairDistance",maximum);
        result.put("xyzMinToMaxPairDistanceRatio",maximum==0?0:minimum/maximum);
        result.put("xyzTriangleArea",Double.isFinite(area)?area:"UNKNOWN_NUMERICAL_RANGE");
        return result;
    }
    public static Map<String,Object> uvQuality(List<double[]> points,double[] bounds) {
        Map<String,Object> result=GeometryEvidenceSupport.map("uvQualityStatus","UNKNOWN_OR_INSUFFICIENT_POINTS",
                "uvNormalizedBoxAreaFraction","UNKNOWN","uvNormalizedMinPairDistance","UNKNOWN",
                "uvSpreadMeansPhysicalAreaCoverage",false);
        if(points==null||points.size()!=3||!boundsValid(bounds)) return result;
        List<double[]> normalized=new ArrayList<>();
        for(double[] point:points) {
            if(point==null||point.length!=2||!Double.isFinite(point[0])||!Double.isFinite(point[1])) return result;
            double u=(point[0]-bounds[0])/(bounds[1]-bounds[0]),v=(point[1]-bounds[2])/(bounds[3]-bounds[2]);
            if(!Double.isFinite(u)||!Double.isFinite(v)) return result;
            normalized.add(new double[]{u,v});
        }
        double minU=Double.POSITIVE_INFINITY,maxU=Double.NEGATIVE_INFINITY,minV=Double.POSITIVE_INFINITY,maxV=Double.NEGATIVE_INFINITY,minDistance=Double.POSITIVE_INFINITY;
        for(int i=0;i<3;i++) {
            double[] point=normalized.get(i); minU=Math.min(minU,point[0]);maxU=Math.max(maxU,point[0]);minV=Math.min(minV,point[1]);maxV=Math.max(maxV,point[1]);
            for(int j=0;j<i;j++) minDistance=Math.min(minDistance,Math.hypot(point[0]-normalized.get(j)[0],point[1]-normalized.get(j)[1]));
        }
        double area=(maxU-minU)*(maxV-minV);
        if(!Double.isFinite(area)||!Double.isFinite(minDistance)) return result;
        result.put("uvQualityStatus","METRICS_RECORDED_PARAMETER_SPACE_ONLY");
        result.put("uvNormalizedBoxAreaFraction",area); result.put("uvNormalizedMinPairDistance",minDistance);
        return result;
    }
    public static final int MAX_QUALITY_POOL_SIZE=9,MAX_QUALITY_PROBES_PER_FACE=32;
    public static List<double[]> qualityCandidates(double u0,double u1,double v0,double v1,List<double[]> attempted) {
        List<double[]> result=refinementCandidates(u0,u1,v0,v1,attempted);
        // Further deterministic seeds remain data only; every actual probe still requires native INSIDE.
        for(int i=1;i<=64;i++) {
            double u=u0+(u1-u0)*radicalInverse(i,2),v=v0+(v1-v0)*radicalInverse(i,3);
            if(!Double.isFinite(u)||!Double.isFinite(v)) continue;
            double[] value={u,v};boolean duplicate=false;
            for(double[] prior:attempted) if(exactUv(prior,value)) { duplicate=true;break; }
            if(!duplicate) for(double[] prior:result) if(exactUv(prior,value)) { duplicate=true;break; }
            if(!duplicate) result.add(value);
        }
        return result;
    }
    private static double radicalInverse(int value,int base) {
        double factor=1.0/base,result=0;
        while(value>0) { result+=(value%base)*factor;value/=base;factor/=base; }
        return result;
    }
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
