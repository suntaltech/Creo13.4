import p1.controlled.step16.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class Step16ControlTest {
 static int checks;
 static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
 interface Throwing{void run()throws Exception;}
 static void rejects(Class<? extends Throwable> type,Throwing test)throws Exception{checks++;try{test.run();}catch(Throwable error){if(type.isInstance(error))return;throw new AssertionError(error);}throw new AssertionError("Expected "+type);}
 public static void main(String[]args)throws Exception{
  Path root=Files.createTempDirectory("p1-control-tests-");
  try{
   rejects(IllegalArgumentException.class,()->Step16Control.parseThickness(null));
   for(String text:new String[]{"","0","-1","NaN","Infinity","1e999","1,5","abc","1e-999"})rejects(IllegalArgumentException.class,()->Step16Control.parseThickness(text));
   check(Step16Control.parseThickness(" 2.5 ")==2.5);
   Path first=root.resolve("first");String id;
   try(Step16Control c=new Step16Control(first)){
    id=c.requestId();check(c.state()==Step16Control.State.CLAIMED);check(!c.dispatched());
    rejects(IllegalStateException.class,c::markDispatch);rejects(IllegalStateException.class,c::acceptFinal);rejects(IllegalStateException.class,()->c.setThickness("2"));
    c.acceptRisk();rejects(IllegalArgumentException.class,()->c.setThickness("0"));check(c.state()==Step16Control.State.RISK_ACCEPTED);
    c.setThickness("2.5");check(c.thickness()==2.5);rejects(IllegalStateException.class,c::markDispatch);
    c.acceptFinal();c.markDispatch();check(c.dispatched());rejects(IllegalStateException.class,c::markDispatch);c.finish("PURE_TEST_NO_MODEL_CALLS");rejects(IllegalStateException.class,c::markDispatch);rejects(IllegalStateException.class,()->c.finish("REPEAT"));
   }
   rejects(FileAlreadyExistsException.class,()->new Step16Control(first));
   List<String> lines=Files.readAllLines(first.resolve(Step16Control.FILE_NAME));check(lines.size()==6);System.out.println("LEDGER_JSON="+lines.get(0));System.out.println("LEDGER_JSON="+lines.get(lines.size()-1));
   try(Step16Control c=new Step16Control(root.resolve("cancel"))){c.finish("CANCELLED_FIRST_CONFIRMATION_NO_WRITE");check(!c.dispatched());rejects(IllegalStateException.class,c::acceptRisk);}
   Path blocked=root.resolve("file-not-dir");Files.writeString(blocked,"x");rejects(java.io.IOException.class,()->new Step16Control(blocked));
   Path failed=root.resolve("closed-channel");Step16Control c=new Step16Control(failed);c.close();rejects(java.io.IOException.class,c::acceptRisk);check(c.state()==Step16Control.State.CLAIMED);check(!c.dispatched());rejects(FileAlreadyExistsException.class,()->new Step16Control(failed));
   Path concurrent=root.resolve("concurrent");AtomicInteger won=new AtomicInteger(),lost=new AtomicInteger(),other=new AtomicInteger();CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
   Runnable action=()->{ready.countDown();try{go.await();try(Step16Control permit=new Step16Control(concurrent)){won.incrementAndGet();}}catch(FileAlreadyExistsException expected){lost.incrementAndGet();}catch(Exception error){other.incrementAndGet();}};
   Thread a=new Thread(action),b=new Thread(action);a.start();b.start();ready.await();go.countDown();a.join();b.join();check(won.get()==1&&lost.get()==1&&other.get()==0);
   check(Files.exists(first.resolve(Step16Control.FILE_NAME)));
  }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
  System.out.println("CONTROL_CHECKS_PASSED="+checks);
 }
}
