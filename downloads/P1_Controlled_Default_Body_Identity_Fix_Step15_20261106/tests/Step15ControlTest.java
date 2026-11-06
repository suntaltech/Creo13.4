import p1.controlled.step15.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class Step15ControlTest {
 static int checks;
 static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
 interface Throwing{void run()throws Exception;}
 static void rejects(Class<? extends Throwable> type,Throwing test)throws Exception{checks++;try{test.run();}catch(Throwable error){if(type.isInstance(error))return;throw new AssertionError(error);}throw new AssertionError("Expected "+type);}
 public static void main(String[]args)throws Exception{
  Path root=Files.createTempDirectory("p1-control-tests-");
  try{
   rejects(IllegalArgumentException.class,()->Step15Control.parseThickness(null));
   for(String text:new String[]{"","0","-1","NaN","Infinity","1e999","1,5","abc","1e-999"})rejects(IllegalArgumentException.class,()->Step15Control.parseThickness(text));
   check(Step15Control.parseThickness(" 2.5 ")==2.5);
   Path first=root.resolve("first");String id;
   try(Step15Control c=new Step15Control(first)){
    id=c.requestId();check(c.state()==Step15Control.State.CLAIMED);check(!c.dispatched());
    rejects(IllegalStateException.class,c::markDispatch);rejects(IllegalStateException.class,c::acceptFinal);rejects(IllegalStateException.class,()->c.setThickness("2"));
    c.acceptRisk();rejects(IllegalArgumentException.class,()->c.setThickness("0"));check(c.state()==Step15Control.State.RISK_ACCEPTED);
    c.setThickness("2.5");check(c.thickness()==2.5);rejects(IllegalStateException.class,c::markDispatch);
    c.acceptFinal();c.markDispatch();check(c.dispatched());rejects(IllegalStateException.class,c::markDispatch);c.finish("PURE_TEST_NO_MODEL_CALLS");rejects(IllegalStateException.class,c::markDispatch);rejects(IllegalStateException.class,()->c.finish("REPEAT"));
   }
   rejects(FileAlreadyExistsException.class,()->new Step15Control(first));
   List<String> lines=Files.readAllLines(first.resolve(Step15Control.FILE_NAME));check(lines.size()==6);System.out.println("LEDGER_JSON="+lines.get(0));System.out.println("LEDGER_JSON="+lines.get(lines.size()-1));
   try(Step15Control c=new Step15Control(root.resolve("cancel"))){c.finish("CANCELLED_FIRST_CONFIRMATION_NO_WRITE");check(!c.dispatched());rejects(IllegalStateException.class,c::acceptRisk);}
   Path blocked=root.resolve("file-not-dir");Files.writeString(blocked,"x");rejects(java.io.IOException.class,()->new Step15Control(blocked));
   Path failed=root.resolve("closed-channel");Step15Control c=new Step15Control(failed);c.close();rejects(java.io.IOException.class,c::acceptRisk);check(c.state()==Step15Control.State.CLAIMED);check(!c.dispatched());rejects(FileAlreadyExistsException.class,()->new Step15Control(failed));
   Path concurrent=root.resolve("concurrent");AtomicInteger won=new AtomicInteger(),lost=new AtomicInteger(),other=new AtomicInteger();CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
   Runnable action=()->{ready.countDown();try{go.await();try(Step15Control permit=new Step15Control(concurrent)){won.incrementAndGet();}}catch(FileAlreadyExistsException expected){lost.incrementAndGet();}catch(Exception error){other.incrementAndGet();}};
   Thread a=new Thread(action),b=new Thread(action);a.start();b.start();ready.await();go.countDown();a.join();b.join();check(won.get()==1&&lost.get()==1&&other.get()==0);
   check(Files.exists(first.resolve(Step15Control.FILE_NAME)));
  }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
  System.out.println("CONTROL_CHECKS_PASSED="+checks);
 }
}
