package p1.readonly.geometry.step8;
import java.util.*;

/** All inputs are validated same-face native points. No SDK and no synthetic XYZ. */
public final class GeometrySpreadSelection {
    public static final int MAX_SPREAD_POOL_SIZE=35;
    private GeometrySpreadSelection() { }
    public static Map<String,Object> select(List<double[]> points,List<Integer> frozenStep5Indices) {
        if(points==null||points.size()>MAX_SPREAD_POOL_SIZE||points.size()<3||frozenStep5Indices==null||frozenStep5Indices.size()!=3)
            throw new IllegalArgumentException("INVALID_BOUNDED_SPREAD_POOL");
        List<Integer> baseline=new ArrayList<>(frozenStep5Indices);Set<Integer> distinct=new HashSet<>(baseline);
        if(distinct.size()!=3)throw new IllegalArgumentException("BASELINE_INDICES_DUPLICATE");
        for(int i:baseline)if(i<0||i>=points.size())throw new IllegalArgumentException("BASELINE_INDEX_OUT_OF_RANGE");
        List<double[]> base=triple(points,baseline);
        Map<String,Object> before=GeometrySamplingMath.xyzQuality(base);
        List<Integer> best=baseline;
        double scale=0;
        for(int i=0;i<points.size();i++) {
            if(!GeometrySamplingMath.finite3(points.get(i)))return retained(points,baseline,before,"BASELINE_RETAINED_NONFINITE_POOL");
            for(int j=0;j<i;j++) {
                double[] a=points.get(i),b=points.get(j);double d=Math.hypot(Math.hypot(a[0]-b[0],a[1]-b[1]),a[2]-b[2]);
                if(!Double.isFinite(d))return retained(points,baseline,before,"BASELINE_RETAINED_NUMERICAL_RANGE_UNKNOWN");
                scale=Math.max(scale,d);
            }
        }
        if(!Boolean.TRUE.equals(before.get("numericallyNonCollinear"))||scale==0)
            return retained(points,baseline,before,"BASELINE_RETAINED_FROZEN_QUALITY_UNKNOWN_OR_DEGENERATE");
        double[] bestScore=score(base,scale);double baseScore=bestScore[0];
        if(!(before.get("xyzTriangleArea") instanceof Number))return retained(points,baseline,before,"BASELINE_RETAINED_AREA_RANGE_UNKNOWN");
        double minimumArea=((Number)before.get("xyzTriangleArea")).doubleValue();int combinations=0;
        for(int i=0;i<points.size()-2;i++)for(int j=i+1;j<points.size()-1;j++)for(int k=j+1;k<points.size();k++) {
            combinations++;List<Integer> indices=Arrays.asList(i,j,k);List<double[]> values=triple(points,indices);
            Map<String,Object> q=GeometrySamplingMath.xyzQuality(values);
            if(!Boolean.TRUE.equals(q.get("numericallyNonCollinear"))||!(q.get("xyzTriangleArea") instanceof Number)
                    ||((Number)q.get("xyzTriangleArea")).doubleValue()<minimumArea)continue;
            double[] sc=score(values,scale);
            if(sc[0]>bestScore[0]||(sc[0]==bestScore[0]&&sc[1]>bestScore[1])) {best=indices;bestScore=sc;}
        }
        Map<String,Object> result=retained(points,best,before,"BOUNDED_FULL_QUEUE_SPREAD_SELECTION");
        result.put("selectedIndices",best);result.put("replacementApplied",!best.equals(baseline));
        result.put("frozenStep5Indices",baseline);result.put("poolScale",scale);
        result.put("baselineAreaScore",baseScore);result.put("selectedAreaScore",bestScore[0]);
        result.put("baselineMinPairScore",score(base,scale)[1]);result.put("selectedMinPairScore",bestScore[1]);
        result.put("combinationsExamined",combinations);return result;
    }
    private static Map<String,Object> retained(List<double[]> p,List<Integer> indices,Map<String,Object> before,String status) {
        Map<String,Object> after=GeometrySamplingMath.xyzQuality(triple(p,indices));
        Object oldArea=before.get("xyzTriangleArea"),newArea=after.get("xyzTriangleArea");
        boolean comparable=oldArea instanceof Number&&newArea instanceof Number;
        return GeometryEvidenceSupport.map("selectedIndices",indices,"status",status,"replacementApplied",false,
                "initialNumericallyNonCollinear",before.get("numericallyNonCollinear"),"finalNumericallyNonCollinear",after.get("numericallyNonCollinear"),
                "frozenStep5Quality",before,"finalSpreadQuality",after,"threshold",GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD,
                "sameRunFrozenStep5TriangleAreaRatio",comparable&&((Number)oldArea).doubleValue()>0?((Number)newArea).doubleValue()/((Number)oldArea).doubleValue():"UNKNOWN",
                "step5BaselineAreaNotDecreased",comparable?((Number)newArea).doubleValue()>=((Number)oldArea).doubleValue():"UNKNOWN",
                "comparisonScope","SAME_RUN_FROZEN_STEP5_ALGORITHM_NOT_AUTHENTICATED_PRIOR_RUN");
    }
    private static List<double[]> triple(List<double[]> points,List<Integer> indices) {return Arrays.asList(points.get(indices.get(0)),points.get(indices.get(1)),points.get(indices.get(2)));}
    private static double[] score(List<double[]> p,double scale) {
        double[] a=p.get(0),b=p.get(1),c=p.get(2),ab=new double[3],ac=new double[3],bc=new double[3];
        for(int i=0;i<3;i++){ab[i]=(b[i]-a[i])/scale;ac[i]=(c[i]-a[i])/scale;bc[i]=(c[i]-b[i])/scale;}
        double area=Math.hypot(Math.hypot(ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2]),ab[0]*ac[1]-ab[1]*ac[0]);
        double minimum=Math.min(Math.hypot(Math.hypot(ab[0],ab[1]),ab[2]),Math.min(Math.hypot(Math.hypot(ac[0],ac[1]),ac[2]),Math.hypot(Math.hypot(bc[0],bc[1]),bc[2])));
        return new double[]{area,minimum};
    }
}
