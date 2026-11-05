import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import p1.readonly.geometry.step3.GeometrySamplingMath;
import p1.readonly.geometry.step3.GeometryEvidenceSupport;

/** Offline arithmetic on supplied point triples; no SDK/model calls or thickness calculation. */
public final class ReplayStep2Quality {
    public static void main(String[] args) throws Exception {
        int groups=0,nonCollinear=0,collinear=0,unknown=0,directZeroCross=0;
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            if(line.isBlank()) continue;
            String[] fields=line.split(","); if(fields.length!=10) throw new IllegalArgumentException("Expected face ID and 9 coordinates");
            List<double[]> points=new ArrayList<>();
            for(int i=0;i<3;i++) points.add(new double[]{Double.parseDouble(fields[1+i*3]),Double.parseDouble(fields[2+i*3]),Double.parseDouble(fields[3+i*3])});
            double[] a=new double[3],b=new double[3];
            for(int i=0;i<3;i++) { a[i]=points.get(1)[i]-points.get(0)[i];b[i]=points.get(2)[i]-points.get(0)[i]; }
            if(a[1]*b[2]-a[2]*b[1]==0&&a[2]*b[0]-a[0]*b[2]==0&&a[0]*b[1]-a[1]*b[0]==0) directZeroCross++;
            Object flag=GeometrySamplingMath.xyzQuality(points).get("numericallyNonCollinear");
            groups++;if(Boolean.TRUE.equals(flag)) nonCollinear++;else if(Boolean.FALSE.equals(flag)) collinear++;else unknown++;
        }
        System.out.println(GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("type","OFFLINE_REAL_STEP2_POINT_ARITHMETIC_NOT_NATIVE_EVALUATION","planeGroupsWithThreePoints",groups,"numericallyNonCollinear",nonCollinear,"numericallyCollinearOrBelowNumericalThreshold",collinear,"unknown",unknown,"directRawZeroCrossProductGroups",directZeroCross,"additionalNumericalPolicyFlags",collinear-directZeroCross,"threshold",GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD,"wallThicknessAssessed",false)));
    }
}
