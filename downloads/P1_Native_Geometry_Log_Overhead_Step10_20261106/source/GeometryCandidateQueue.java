package p1.readonly.geometry.step10;
import java.util.*;

/** Bounded parameter proposals only. Every proposal still needs both native INSIDE gates. */
public final class GeometryCandidateQueue {
    public static final String LOCAL="LOCAL_INSIDE_OFFSET", INTERNAL="INTERNAL_HALTON", EDGE="EDGE_SINGLE_LAYER";
    public static final int LOCAL_BUDGET=16, INTERNAL_BUDGET=8, EDGE_BUDGET=8;
    public static final class Seed {
        public final double[] uv;
        public final String strategy;
        public final int anchorOrdinal;
        public final double offsetFraction;
        Seed(double[] uv,String strategy,int anchorOrdinal,double offsetFraction) {
            this.uv=uv.clone();this.strategy=strategy;this.anchorOrdinal=anchorOrdinal;this.offsetFraction=offsetFraction;
        }
    }
    private GeometryCandidateQueue() { }
    public static List<Seed> build(double[] bounds,List<double[]> knownInside,List<double[]> attempted) {
        if(!GeometrySamplingMath.boundsValid(bounds)||knownInside==null||attempted==null)
            throw new IllegalArgumentException("QUEUE_INPUT_UNAVAILABLE");
        if(knownInside.size()!=3) throw new IllegalArgumentException("THREE_NATIVE_VALID_SEEDS_REQUIRED");
        for(double[] uv:knownInside) if(!finiteInsideRectangle(uv,bounds)) throw new IllegalArgumentException("SEED_OUTSIDE_PARAMETER_RECTANGLE");
        List<Seed> local=new ArrayList<>(),internal=new ArrayList<>(),edge=new ArrayList<>();
        double[] scales={1.0/64,1.0/256,1.0/1024,1.0/16};
        double[][] directions={{1,0},{0,1},{-1,0},{0,-1}};
        // First sixteen cover all four scales and axis directions across the three known native seeds.
        // Remaining layers provide replacements for duplicate/out-of-rectangle proposals, not extra probes.
        for(int layer=0;layer<3;layer++) for(int si=0;si<scales.length;si++) for(int d=0;d<4;d++) {
            int anchor=(layer+si+d)%knownInside.size();double[] uv=knownInside.get(anchor);double s=scales[si];
            local.add(new Seed(new double[]{uv[0]+(bounds[1]-bounds[0])*s*directions[d][0],
                    uv[1]+(bounds[3]-bounds[2])*s*directions[d][1]},LOCAL,anchor+1,s));
        }
        for(int i=1;i<=64;i++) internal.add(new Seed(interpolate(bounds,radicalInverse(i,2),radicalInverse(i,3)),INTERNAL,0,0));
        // One existing layer only, prioritized using Step4's observed useful locations; no new edge layers.
        double e=1.0/32;
        double[][] fractions={{e,1-e},{1-e,e},{e,.5},{.5,e},{e,e},{1-e,1-e},{1-e,.5},{.5,1-e}};
        for(double[] uv:fractions) edge.add(new Seed(interpolate(bounds,uv[0],uv[1]),EDGE,0,e));
        List<Seed> result=new ArrayList<>();List<double[]> seen=new ArrayList<>(attempted);
        int[] positions={0,0,0};
        List<List<Seed>> buckets=Arrays.asList(local,internal,edge);
        for(int cycle=0;cycle<8;cycle++) for(int bucket:new int[]{0,1,0,2}) {
            List<Seed> values=buckets.get(bucket);
            while(positions[bucket]<values.size()) {
                Seed seed=values.get(positions[bucket]++);
                if(!finiteInsideRectangle(seed.uv,bounds)||contains(seen,seed.uv)) continue;
                result.add(seed);seen.add(seed.uv);break;
            }
        }
        return result;
    }
    private static boolean finiteInsideRectangle(double[] uv,double[] b) {
        return uv!=null&&uv.length==2&&Double.isFinite(uv[0])&&Double.isFinite(uv[1])
                &&uv[0]>b[0]&&uv[0]<b[1]&&uv[1]>b[2]&&uv[1]<b[3];
    }
    private static boolean contains(List<double[]> values,double[] uv) {
        for(double[] prior:values) if(GeometrySamplingMath.exactUv(prior,uv)) return true;return false;
    }
    private static double[] interpolate(double[] b,double u,double v) {return new double[]{b[0]+(b[1]-b[0])*u,b[2]+(b[3]-b[2])*v};}
    private static double radicalInverse(int value,int base) {double result=0,factor=1.0/base;while(value>0){result+=(value%base)*factor;value/=base;factor/=base;}return result;}
}
