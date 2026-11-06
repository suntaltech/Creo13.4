package p1.readonly.geometry.step10;
import java.util.*;

/** Coarse same-body sampled evidence only. No face separation, cylinder radii or thickness calculation. */
public final class GeometryPairEvidence {
    public static final int MAX_COMPARISONS=16000, MAX_EMITTED_PER_TYPE=64;
    public static final double NORMAL_COMPATIBILITY_COSINE=0.95;
    private GeometryPairEvidence() { }
    public static Map<String,Object> analyze(String type,List<double[]> a,List<double[]> an,List<double[]> b,List<double[]> bn) {
        Map<String,Object> result=GeometryEvidenceSupport.map("pairType",type,"status","UNKNOWN_INVALID_SAMPLE_DATA","candidate",false,
                "evidenceLevel","SAMPLED_POINT_NORMAL_AND_PROJECTION_HINT_ONLY","trueOppositeFacesConfirmed",false,
                "trimmedOverlapProven",false,"thicknessAssessed",false,"normalsAreOutwardCertified",false,
                "cylinderAxisCoaxialityAndRadii","UNKNOWN_NOT_ACQUIRED","normalCompatibilityCosine",NORMAL_COMPATIBILITY_COSINE);
        if(!Arrays.asList("PLANE_PLANE","CYLINDER_CYLINDER").contains(type)||!valid(a)||!valid(b)||!valid(an)||!valid(bn))return result;
        List<double[]> na=new ArrayList<>(),nb=new ArrayList<>();
        try{for(double[] n:an)na.add(GeometrySamplingMath.normalUnit(n));for(double[] n:bn)nb.add(GeometrySamplingMath.normalUnit(n));}
        catch(IllegalArgumentException error){return result;}
        double minimum=1,maximum=-1,absolute=0;
        for(double[] x:na)for(double[] y:nb){double dot=Math.max(-1,Math.min(1,x[0]*y[0]+x[1]*y[1]+x[2]*y[2]));minimum=Math.min(minimum,dot);maximum=Math.max(maximum,dot);absolute=Math.max(absolute,Math.abs(dot));}
        result.put("sampleNormalDotMin",minimum);result.put("sampleNormalDotMax",maximum);result.put("sampleNormalAbsoluteDotMax",absolute);
        result.put("normalRelation",minimum<=-NORMAL_COMPATIBILITY_COSINE?"APPROXIMATELY_OPPOSED_SAMPLES_PRESENT":maximum>=NORMAL_COMPATIBILITY_COSINE?"APPROXIMATELY_PARALLEL_SAMPLES_PRESENT":"NO_NORMAL_COMPATIBILITY_HINT");
        result.put("normalPolicyMeaning","COARSE_CANDIDATE_FILTER_NOT_MODEL_TOLERANCE_OR_OUTWARD_ORIENTATION");
        double[] n=na.get(0);int axis=Math.abs(n[0])<=Math.abs(n[1])&&Math.abs(n[0])<=Math.abs(n[2])?0:Math.abs(n[1])<=Math.abs(n[2])?1:2;
        double[] reference=new double[3];reference[axis]=1;
        double[] u=GeometrySamplingMath.normalUnit(cross(n,reference)),v=cross(n,u);
        double[] aa=box(a,a.get(0),u,v),bb=box(b,a.get(0),u,v);
        if(aa==null||bb==null){result.put("status","UNKNOWN_PROJECTION_NUMERICAL_RANGE");return result;}
        List<Double> intersection=Arrays.asList(Math.max(aa[0],bb[0]),Math.min(aa[1],bb[1]),Math.max(aa[2],bb[2]),Math.min(aa[3],bb[3]));
        boolean nondegenerate=aa[1]>aa[0]&&aa[3]>aa[2]&&bb[1]>bb[0]&&bb[3]>bb[2];
        boolean overlap=intersection.get(1)>intersection.get(0)&&intersection.get(3)>intersection.get(2);
        result.put("projectionU",GeometrySamplingMath.list(u));result.put("projectionV",GeometrySamplingMath.list(v));
        result.put("projectionFrame","COMMON_TANGENT_TO_FIRST_SAMPLE_NORMAL_OF_FACE_A_MODEL_COORDINATES");
        result.put("sampleProjectionBoxA",GeometrySamplingMath.safeValues(aa));result.put("sampleProjectionBoxB",GeometrySamplingMath.safeValues(bb));
        result.put("boxOrder",Arrays.asList("uMin","uMax","vMin","vMax"));
        result.put("projectedOverlapRectangleCandidate",nondegenerate&&overlap?intersection:"UNKNOWN_OR_NO_POSITIVE_SAMPLED_BOX_INTERSECTION");
        result.put("sampledProjectionBoxesPositiveOverlap",nondegenerate?overlap:"UNKNOWN_DEGENERATE_SAMPLED_BOX");
        result.put("projectionDoesNotProveTrimmedMaterialOverlap",true);
        result.put("candidate",nondegenerate&&overlap&&absolute>=NORMAL_COMPATIBILITY_COSINE);
        result.put("status",!nondegenerate?"UNKNOWN_DEGENERATE_SAMPLED_BOX":nondegenerate&&overlap&&absolute>=NORMAL_COMPATIBILITY_COSINE?"COARSE_PAIR_CANDIDATE":"NO_COARSE_HINT_NOT_PROOF_OF_NO_OPPOSITE_FACE");
        return result;
    }
    private static boolean valid(List<double[]> p){if(p==null||p.size()!=3)return false;for(double[] x:p)if(!GeometrySamplingMath.finite3(x))return false;return true;}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    private static double[] box(List<double[]> p,double[] origin,double[] u,double[] v){
        double[] b={Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(double[] x:p){double[] d={x[0]-origin[0],x[1]-origin[1],x[2]-origin[2]};if(!GeometrySamplingMath.finite3(d))return null;
            double pu=d[0]*u[0]+d[1]*u[1]+d[2]*u[2],pv=d[0]*v[0]+d[1]*v[1]+d[2]*v[2];if(!Double.isFinite(pu)||!Double.isFinite(pv))return null;
            b[0]=Math.min(b[0],pu);b[1]=Math.max(b[1],pu);b[2]=Math.min(b[2],pv);b[3]=Math.max(b[3],pv);}
        return b;
    }
}
