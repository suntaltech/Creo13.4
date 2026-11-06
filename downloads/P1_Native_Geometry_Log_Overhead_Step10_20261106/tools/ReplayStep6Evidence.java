import java.nio.file.*;
import java.util.*;
import p1.readonly.geometry.step10.*;
/** Prior records only: no new native validity or Step6 runtime is inferred. */
public final class ReplayStep6Evidence {
 public static void main(String[] args)throws Exception {
  if(args[0].equals("spread")){
   for(String line:Files.readAllLines(Path.of(args[1]))){String[] f=line.split(",");List<Integer> baseline=Arrays.asList(Integer.valueOf(f[1]),Integer.valueOf(f[2]),Integer.valueOf(f[3]));List<double[]> p=new ArrayList<>();for(int i=4;i<f.length;i+=3)p.add(new double[]{Double.parseDouble(f[i]),Double.parseDouble(f[i+1]),Double.parseDouble(f[i+2])});Map<String,Object> s=GeometrySpreadSelection.select(p,baseline);s.put("surfaceId",Integer.valueOf(f[0]));System.out.println(GeometryEvidenceSupport.json(s));}
  }else{
   List<String[]> rows=new ArrayList<>();for(String line:Files.readAllLines(Path.of(args[1])))rows.add(line.split(","));
   Map<String,Integer> candidates=new LinkedHashMap<>(),outcomes=new LinkedHashMap<>();int comparisons=0;
   for(int i=0;i<rows.size();i++)for(int j=i+1;j<rows.size();j++){
    String[] a=rows.get(i),b=rows.get(j);if(!a[1].equals(b[1])||!a[2].equals(b[2]))continue;comparisons++;
    Map<String,Object> evidence=GeometryPairEvidence.analyze(a[1]+"_"+b[1],values(a,3),values(a,12),values(b,3),values(b,12));
    String status=(String)evidence.get("status");outcomes.merge(status,1,Integer::sum);if(Boolean.TRUE.equals(evidence.get("candidate")))candidates.merge((String)evidence.get("pairType"),1,Integer::sum);
   }
   System.out.println(GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("type","PURE_STEP5_POINT_PAIR_HINT_REPLAY_NOT_STEP6_RUNTIME","faces",rows.size(),"comparisons",comparisons,"candidateCountsByType",candidates,"outcomeCounts",outcomes,"newNativeCalls",0,"thicknessAssessed",false)));
  }
 }
 static List<double[]> values(String[] f,int start){List<double[]> p=new ArrayList<>();for(int i=start;i<start+9;i+=3)p.add(new double[]{Double.parseDouble(f[i]),Double.parseDouble(f[i+1]),Double.parseDouble(f[i+2])});return p;}
}
