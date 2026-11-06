package p1.readonly.geometry.step8;
import java.util.*;

/** Ranking only validated, same-face candidates. No SDK and no change to the numerical quality threshold. */
public final class GeometryPoolSelection {
    private GeometryPoolSelection() { }
    public static Map<String,Object> select(List<double[]> points) {
        int size=points==null?0:points.size();
        List<Integer> selected=new ArrayList<>();for(int i=0;i<Math.min(3,size);i++) selected.add(i);
        Map<String,Object> result=GeometryEvidenceSupport.map("selectedIndices",selected,"status","INSUFFICIENT_POOL",
                "replacementApplied",false,"initialNumericallyNonCollinear","UNKNOWN","finalNumericallyNonCollinear","UNKNOWN",
                "poolScale","UNKNOWN","baselineAreaScore","UNKNOWN","selectedAreaScore","UNKNOWN");
        if(size<3) return result;
        if(size>GeometrySamplingMath.MAX_QUALITY_POOL_SIZE) throw new IllegalArgumentException("POOL_LIMIT_EXCEEDED");
        List<double[]> baseline=Arrays.asList(points.get(0),points.get(1),points.get(2));
        Object initial=GeometrySamplingMath.xyzQuality(baseline).get("numericallyNonCollinear");
        result.put("initialNumericallyNonCollinear",initial);result.put("finalNumericallyNonCollinear",initial);
        double scale=0;
        for(int i=0;i<size;i++) {
            if(!GeometrySamplingMath.finite3(points.get(i))) { result.put("status","BASELINE_RETAINED_NUMERICAL_RANGE_UNKNOWN");return result; }
            for(int j=0;j<i;j++) {
                double[] a=points.get(i),b=points.get(j);
                double distance=Math.hypot(Math.hypot(a[0]-b[0],a[1]-b[1]),a[2]-b[2]);
                if(!Double.isFinite(distance)) { result.put("status","BASELINE_RETAINED_NUMERICAL_RANGE_UNKNOWN");return result; }
                scale=Math.max(scale,distance);
            }
        }
        if(scale==0) { result.put("status","BASELINE_RETAINED_ZERO_SPREAD");return result; }
        double[] baseScore=score(baseline,scale);double[] bestScore=baseScore;
        List<Integer> best=selected;
        boolean found=false;
        for(int i=0;i<size-2;i++) for(int j=i+1;j<size-1;j++) for(int k=j+1;k<size;k++) {
            List<double[]> triple=Arrays.asList(points.get(i),points.get(j),points.get(k));
            if(!Boolean.TRUE.equals(GeometrySamplingMath.xyzQuality(triple).get("numericallyNonCollinear"))) continue;
            double[] candidateScore=score(triple,scale);
            if(candidateScore[0]<baseScore[0]) continue; // Do not trade away spatial area to merely cross the quality flag.
            if(!found||candidateScore[0]>bestScore[0]||(candidateScore[0]==bestScore[0]&&candidateScore[1]>bestScore[1])) {
                found=true;bestScore=candidateScore;best=Arrays.asList(i,j,k);
            }
        }
        if(found) {
            result.put("selectedIndices",best);result.put("replacementApplied",!best.equals(selected));
            result.put("status",best.equals(selected)?"BASELINE_ALREADY_BEST_ELIGIBLE":"NONDEGENERATE_POOL_TRIPLE_SELECTED");
            result.put("finalNumericallyNonCollinear",true);
        } else result.put("status","BASELINE_RETAINED_NO_NONDEGENERATE_NONDEGRADING_TRIPLE");
        result.put("poolScale",scale);result.put("baselineAreaScore",baseScore[0]);result.put("selectedAreaScore",bestScore[0]);
        result.put("baselineMinPairScore",baseScore[1]);result.put("selectedMinPairScore",bestScore[1]);
        result.put("threshold",GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD);
        return result;
    }
    private static double[] score(List<double[]> points,double scale) {
        double[] a=points.get(0),b=points.get(1),c=points.get(2),ab=new double[3],ac=new double[3],bc=new double[3];
        for(int i=0;i<3;i++) { ab[i]=(b[i]-a[i])/scale;ac[i]=(c[i]-a[i])/scale;bc[i]=(c[i]-b[i])/scale; }
        double area=Math.hypot(Math.hypot(ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2]),ab[0]*ac[1]-ab[1]*ac[0]);
        double minimum=Math.min(Math.hypot(Math.hypot(ab[0],ab[1]),ab[2]),Math.min(Math.hypot(Math.hypot(ac[0],ac[1]),ac[2]),Math.hypot(Math.hypot(bc[0],bc[1]),bc[2])));
        return new double[]{area,minimum};
    }
}
