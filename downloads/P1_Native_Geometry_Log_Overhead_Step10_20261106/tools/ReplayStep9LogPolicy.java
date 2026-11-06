import p1.readonly.geometry.step10.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
/** File-only policy replay. No SDK, model, plugin initialization, or performance claim. */
public final class ReplayStep9LogPolicy {
 public static void main(String[] args)throws Exception{
  GeometryStderrPolicy policy=new GeometryStderrPolicy(args[0]);long lines=0,mirroredBytes=0;
  try(BufferedReader input=Files.newBufferedReader(Path.of(args[1]),StandardCharsets.UTF_8);
      BufferedReader events=Files.newBufferedReader(Path.of(args[2]),StandardCharsets.UTF_8);
      BufferedWriter file=Files.newBufferedWriter(Path.of(args[3]),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
      BufferedWriter mirror=Files.newBufferedWriter(Path.of(args[4]),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
   String line;while((line=input.readLine())!=null){String event=events.readLine();if(event==null)throw new IOException("missing event");
    // Same order as plugin: retain every JSONL row and flush, then decide mirror.
    file.write(line);file.newLine();file.flush();lines++;
    if(policy.decide(event)){String text="[P1GEO] "+line;mirror.write(text);mirror.newLine();mirroredBytes+=text.getBytes(StandardCharsets.UTF_8).length+1;}
   }
   if(events.readLine()!=null)throw new IOException("extra event");
  }
  System.out.println(GeometryEvidenceSupport.json(GeometryEvidenceSupport.map("sourceLinesRetained",lines,"selectedMirrorUtf8BytesLf",mirroredBytes,"policy",policy.report(),"nativeCallsMade",false,"replayIsNativePerformanceBenchmark",false)));
 }
}
