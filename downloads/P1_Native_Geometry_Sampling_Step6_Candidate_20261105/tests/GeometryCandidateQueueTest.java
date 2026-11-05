import java.util.*;
import p1.readonly.geometry.step6.*;

/** Parameter sequence and failure-preservation tests only; no native placement simulation. */
public final class GeometryCandidateQueueTest {
    static int checks;
    static void check(boolean value) {checks++;if(!value)throw new AssertionError("queue check "+checks);}
    static Map<String,Integer> counts(List<GeometryCandidateQueue.Seed> q) {
        Map<String,Integer> result=new HashMap<>();for(GeometryCandidateQueue.Seed s:q)result.merge(s.strategy,1,Integer::sum);return result;
    }
    static void properties(double[] b,List<double[]> inside,List<double[]> attempted) {
        List<GeometryCandidateQueue.Seed> q=GeometryCandidateQueue.build(b,inside,attempted);
        check(q.size()<=32);Map<String,Integer> c=counts(q);
        check(c.getOrDefault(GeometryCandidateQueue.LOCAL,0)<=16);check(c.getOrDefault(GeometryCandidateQueue.INTERNAL,0)<=8);check(c.getOrDefault(GeometryCandidateQueue.EDGE,0)<=8);
        List<double[]> seen=new ArrayList<>(attempted);
        for(GeometryCandidateQueue.Seed s:q) {
            check(Double.isFinite(s.uv[0])&&Double.isFinite(s.uv[1]));
            check(s.uv[0]>b[0]&&s.uv[0]<b[1]&&s.uv[1]>b[2]&&s.uv[1]<b[3]);
            for(double[] previous:seen)check(!GeometrySamplingMath.exactUv(previous,s.uv));
            seen.add(s.uv);
            if(s.strategy.equals(GeometryCandidateQueue.LOCAL)) {
                check(s.anchorOrdinal>=1&&s.anchorOrdinal<=3);
                double[] a=inside.get(s.anchorOrdinal-1);
                check(s.uv[0]==a[0]||s.uv[1]==a[1]);
            }
        }
        List<GeometryCandidateQueue.Seed> again=GeometryCandidateQueue.build(b,inside,attempted);check(q.size()==again.size());
        for(int i=0;i<q.size();i++)check(q.get(i).strategy.equals(again.get(i).strategy)&&GeometrySamplingMath.exactUv(q.get(i).uv,again.get(i).uv));
    }
    public static void main(String[] args) {
        double[] b={0,1,0,1};List<double[]> seeds=Arrays.asList(new double[]{.5,.5},new double[]{.25,.25},new double[]{.75,.75});
        List<double[]> attempted=GeometrySamplingMath.candidates(0,1,0,1);List<GeometryCandidateQueue.Seed> q=GeometryCandidateQueue.build(b,seeds,attempted);
        check(q.size()==32);Map<String,Integer> c=counts(q);check(c.get(GeometryCandidateQueue.LOCAL)==16&&c.get(GeometryCandidateQueue.INTERNAL)==8&&c.get(GeometryCandidateQueue.EDGE)==8);
        String[] cycle={GeometryCandidateQueue.LOCAL,GeometryCandidateQueue.INTERNAL,GeometryCandidateQueue.LOCAL,GeometryCandidateQueue.EDGE};
        for(int i=0;i<32;i++)check(q.get(i).strategy.equals(cycle[i%4]));
        Set<Double> scales=new HashSet<>();for(GeometryCandidateQueue.Seed s:q)if(s.strategy.equals(GeometryCandidateQueue.LOCAL))scales.add(s.offsetFraction);check(scales.size()==4);
        // Known native-valid seed inputs are not modified or treated as proof that nearby offsets are native valid.
        check(seeds.get(0)[0]==.5&&seeds.get(1)[1]==.25);
        properties(b,seeds,attempted);
        properties(new double[]{-100,300,-.02,.03},Arrays.asList(new double[]{10,0},new double[]{20,.001},new double[]{30,.002}),Collections.emptyList());
        properties(b,Arrays.asList(new double[]{Math.nextUp(0.0),.5},new double[]{Math.nextDown(1.0),.5},new double[]{.5,Math.nextUp(0.0)}),attempted);
        List<double[]> already=new ArrayList<>(attempted);for(GeometryCandidateQueue.Seed s:q)already.add(s.uv);properties(b,seeds,already);
        boolean bad=false;try{GeometryCandidateQueue.build(new double[]{0,0,0,1},seeds,attempted);}catch(IllegalArgumentException expected){bad=true;}check(bad);
        bad=false;try{GeometryCandidateQueue.build(b,Collections.emptyList(),attempted);}catch(IllegalArgumentException expected){bad=true;}check(bad);
        bad=false;try{GeometryCandidateQueue.build(b,Arrays.asList(new double[]{Double.NaN,0},new double[]{.5,.5},new double[]{.6,.6}),attempted);}catch(IllegalArgumentException expected){bad=true;}check(bad);
        check(GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD==256*Math.ulp(1.0));
        // The unchanged selector retains original points when no new native coordinates exist.
        List<double[]> line=Arrays.asList(new double[]{0,0,0},new double[]{1,0,0},new double[]{2,0,0});
        check(GeometryPoolSelection.select(line).get("replacementApplied").equals(false));
        System.out.println("MIXED_QUEUE_CHECKS_PASSED="+checks);
    }
}
