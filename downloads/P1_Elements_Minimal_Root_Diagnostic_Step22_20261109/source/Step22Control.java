package p1.controlled.step22;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static p1.controlled.step22.AuditSupport.*;

/** Durable one-click ledger and pure confirmation state; no SDK or model API. Never deletes/rearms. */
public final class Step22Control implements AutoCloseable {
 public enum State { CLAIMED, RISK_ACCEPTED, THICKNESS_SET, FINAL_ACCEPTED, FINISHED }
 public static final String FILE_NAME="step22-200002100-once.jsonl";
 private final FileChannel channel;
 private final String requestId=UUID.randomUUID().toString();
 private final long entered=System.nanoTime();
 private long sequence;
 private State state=State.CLAIMED;
 private Double thickness;
 private String outcome;
 public Step22Control(Path directory)throws IOException {
  Files.createDirectories(directory);
  channel=FileChannel.open(directory.resolve(FILE_NAME),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
  try{record("CLICK_CLAIMED",map("oneClickConsumed",true,"persistentLockNeverAutomaticallyRemoved",true));}
  catch(IOException|RuntimeException error){try{channel.close();}catch(IOException closeError){error.addSuppressed(closeError);}throw error;}
 }
 public String requestId(){return requestId;}
 public State state(){return state;}
 public String outcome(){return outcome;}
 public double thickness(){if(thickness==null)throw new IllegalStateException("No explicit test thickness");return thickness;}
 private void require(State expected){if(state!=expected)throw new IllegalStateException("Wrong confirmation order: "+state);}
 public void acceptRisk()throws IOException{require(State.CLAIMED);record("RISK_CONFIRMATION_ACCEPTED",map("disposableModelExplicitlyAcknowledged",true,"globalThicknessUnproven",true));state=State.RISK_ACCEPTED;}
 public static double parseThickness(String raw){
  if(raw==null||raw.isBlank())throw new IllegalArgumentException("No explicit test thickness");
  double value=new java.math.BigDecimal(raw.strip()).doubleValue();
  if(!Double.isFinite(value)||value<=0)throw new IllegalArgumentException("Thickness must be finite positive");return value;
 }
 public void setThickness(String raw)throws IOException{require(State.RISK_ACCEPTED);double value=parseThickness(raw);record("TEST_THICKNESS_ENTERED",map("testThicknessMm",value,"source","EXPLICIT_OPERATOR_INPUT_NOT_LOCAL_DISTANCE_OR_GLOBAL_PROOF"));thickness=value;state=State.THICKNESS_SET;}
 public void acceptFinal()throws IOException{require(State.THICKNESS_SET);record("FINAL_CONFIRMATION_ACCEPTED",map("testThicknessMm",thickness,"minimalElementsAndRootOnly",true,"noModelWrite",true,"noSave",true,"fiveFailureFlagsChecked",false));state=State.FINAL_ACCEPTED;}
 public void finish(String outcome)throws IOException{if(state==State.FINISHED)throw new IllegalStateException("Already finished");record("FINAL_RESULT",map("outcome",outcome,"writeDispatchIntentRecorded",false,"fiveFailureFlagsChecked",false,"modelCreationOrRegenerationInvoked",false,"automaticRollbackAttempted",false,"saveApiInvokedByThisCommand",false,"otherSaveEntrypointsIntercepted",false));state=State.FINISHED;this.outcome=outcome;}
 public void record(String event,Map<String,Object> fields)throws IOException{
  Map<String,Object> row=map("schema","P1_STEP22_ELEMENTS_MINIMAL_DIAGNOSTIC_V1","step","STEP_22","requestId",requestId,"sequence",++sequence,"timestamp",Instant.now().toString(),"elapsedNs",System.nanoTime()-entered,"javaThreadId",Thread.currentThread().getId(),"event",event,"controlState",state.toString());row.putAll(fields);
  ByteBuffer bytes=ByteBuffer.wrap((json(row)+"\n").getBytes(StandardCharsets.UTF_8));while(bytes.hasRemaining())channel.write(bytes);channel.force(true);
 }
 @Override public void close()throws IOException{try{channel.force(true);}finally{channel.close();}}
}
