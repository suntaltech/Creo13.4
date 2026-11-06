import java.nio.file.*;
import java.util.*;
import p1.readonly.geometry.step8.*;
/** Reclassifies prior numeric records only; no SDK or new native measurements. */
public final class ReplayStep7CandidateClassification {
 public static void main(String[]args)throws Exception{
  for(String line:Files.readAllLines(Path.of(args[0]))){String[]f=line.split(",");int a=Integer.parseInt(f[1]),b=Integer.parseInt(f[2]);
   Map<String,Object> gap=f[5].equals("NONE")?null:GeometryEvidenceSupport.map("status",f[5]);
   Map<String,Object> sum=f[6].equals("NONE")?null:GeometryEvidenceSupport.map("status",f[6],"probeLocationsAttempted",Integer.valueOf(f[7]),"nativeInsideWitnesses",Integer.valueOf(f[8]),"aToBWitnesses",Integer.valueOf(f[9]),"bToAWitnesses",Integer.valueOf(f[10]));
   GeometryCandidateClassification.Stats s=new GeometryCandidateClassification.Stats();for(int i=11;i<f.length;i+=4)s.observe(a,b,Integer.parseInt(f[i]),Integer.parseInt(f[i+1]),Double.parseDouble(f[i+2]),Double.parseDouble(f[i+3]));
   System.out.println(GeometryEvidenceSupport.json(GeometryCandidateClassification.classify(f[0],a,b,f[3],f[4],gap,sum,s)));
  }
 }
}
