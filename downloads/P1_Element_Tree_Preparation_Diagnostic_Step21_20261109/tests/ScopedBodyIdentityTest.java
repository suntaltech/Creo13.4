import p1.controlled.step21.ScopedBodyIdentity;
import static p1.controlled.step21.ScopedBodyIdentity.Code.*;
public final class ScopedBodyIdentityTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static ScopedBodyIdentity.Code result(boolean scope,Integer count,boolean listed,boolean body,Integer id,Integer bodyId,boolean again,Integer baseline,Boolean sheet,boolean failed){return ScopedBodyIdentity.evaluate(scope,count,listed,body,id,bodyId,again,baseline,sheet,failed);}
 public static void main(String[]args){
  check(result(true,1,true,true,-5778,-5778,false,null,false,false)==ACCEPTED);
  check(result(true,1,true,true,-5778,-5778,true,-5778,false,false)==ACCEPTED);
  check(result(false,1,true,true,-5778,-5778,false,null,false,false)==MODEL_SCOPE_MISMATCH);
  check(result(true,1,true,false,-5778,null,false,null,null,false)==DEFAULT_BODY_NULL);
  check(result(true,null,false,true,null,-5778,false,null,false,false)==ENUMERATED_BODY_UNAVAILABLE);
  check(result(true,0,false,true,null,-5778,false,null,false,false)==ENUMERATED_BODY_UNAVAILABLE);
  check(result(true,2,true,true,-5778,-5778,false,null,false,false)==ENUMERATED_BODY_UNAVAILABLE);
  check(result(true,1,false,true,null,-5778,false,null,false,false)==ENUMERATED_BODY_UNAVAILABLE);
  check(result(true,1,true,true,1,2,false,null,false,false)==BODY_ID_MISMATCH);
  check(result(true,1,true,true,2,2,true,1,false,false)==BODY_ID_MISMATCH);
  check(result(true,1,true,true,1,1,true,null,false,false)==STATE_READ_EXCEPTION);
  check(result(true,1,true,true,null,1,false,null,false,false)==STATE_READ_EXCEPTION);
  check(result(true,1,true,true,1,null,false,null,false,false)==STATE_READ_EXCEPTION);
  check(result(true,1,true,true,1,1,false,null,null,false)==STATE_READ_EXCEPTION);
  check(result(true,1,true,true,1,1,false,null,false,true)==STATE_READ_EXCEPTION);
  check(result(true,1,true,false,1,null,false,null,null,true)==STATE_READ_EXCEPTION);
  check(result(true,1,true,true,1,1,false,null,true,false)==ALREADY_SHEETMETAL);
  check(result(true,1,true,true,-1,-1,false,null,false,false)==ACCEPTED);
  check(result(true,1,true,true,-1,-1,true,-1,false,false)==ACCEPTED);
  check(result(false,2,true,true,1,1,false,null,true,false)==MODEL_SCOPE_MISMATCH);
  check(result(true,1,true,true,1,2,false,null,true,false)==BODY_ID_MISMATCH);
  System.out.println("SCOPED_BODY_CHECKS_PASSED="+checks);
 }
}
