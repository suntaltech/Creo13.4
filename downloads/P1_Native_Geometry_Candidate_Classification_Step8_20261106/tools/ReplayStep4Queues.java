import java.nio.file.*;
import java.util.*;
import p1.readonly.geometry.step8.*;

/** Plans only parameter candidates from real old seeds; never guesses native placement or XYZ. */
public final class ReplayStep4Queues {
    public static void main(String[] args)throws Exception {
        for(String line:Files.readAllLines(Path.of(args[0]))) {
            String[] f=line.split(",");int pos=1;double[] b=new double[4];for(int i=0;i<4;i++)b[i]=Double.parseDouble(f[pos++]);
            List<double[]> inside=new ArrayList<>(),attempted=new ArrayList<>();
            for(int i=0;i<3;i++)inside.add(new double[]{Double.parseDouble(f[pos++]),Double.parseDouble(f[pos++])});
            while(pos<f.length)attempted.add(new double[]{Double.parseDouble(f[pos++]),Double.parseDouble(f[pos++])});
            List<GeometryCandidateQueue.Seed> q=GeometryCandidateQueue.build(b,inside,attempted);
            List<Map<String,Object>> plan=new ArrayList<>();
            for(GeometryCandidateQueue.Seed s:q)plan.add(GeometryEvidenceSupport.map("uv",GeometrySamplingMath.list(s.uv),"strategy",s.strategy,"anchorOrdinal",s.anchorOrdinal,"offsetFraction",s.offsetFraction));
            System.out.println(GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("surfaceId",Integer.valueOf(f[0]),"plan",plan)));
        }
    }
}
