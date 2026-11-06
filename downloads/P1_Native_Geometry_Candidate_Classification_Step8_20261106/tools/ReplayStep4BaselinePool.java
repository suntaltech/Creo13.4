import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import p1.readonly.geometry.step8.*;

/** Offline original triples only: cannot invent new valid native points. */
public final class ReplayStep4BaselinePool {
    public static void main(String[] args) throws Exception {
        int groups=0,beforeGood=0,afterGood=0,replaced=0;
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            if(line.isBlank())continue;String[] fields=line.split(",");if(fields.length!=10)throw new IllegalArgumentException("Expected ID and nine coordinates");
            List<double[]> values=new ArrayList<>();for(int i=0;i<3;i++) values.add(new double[]{Double.parseDouble(fields[1+i*3]),Double.parseDouble(fields[2+i*3]),Double.parseDouble(fields[3+i*3])});
            Map<String,Object> selection=GeometryPoolSelection.select(values);groups++;
            if(Boolean.TRUE.equals(selection.get("initialNumericallyNonCollinear")))beforeGood++;
            if(Boolean.TRUE.equals(selection.get("finalNumericallyNonCollinear")))afterGood++;
            if(Boolean.TRUE.equals(selection.get("replacementApplied")))replaced++;
        }
        System.out.println(GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("type","OFFLINE_STEP4_TRIPLES_WITHOUT_NEW_NATIVE_CANDIDATES","planeGroups",groups,
                "beforeNumericallyNonCollinear",beforeGood,"afterNumericallyNonCollinear",afterGood,"replacements",replaced,
                "newNativeCandidateDataAvailable",false,"thresholdUnchanged",true,"runtimeImprovementVerified",false)));
    }
}
