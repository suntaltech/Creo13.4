import p1.readonly.body.step14.*;
import java.util.*;
import static p1.readonly.body.step14.DiagnosticSupport.*;
public final class BodyIdentityTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static Map<String,Object> object(String state,Object ptr,Object id,Object sheet){return map("objectState",state,"present",state.equals("RETURNED")?Boolean.TRUE:state.equals("RETURNED_NULL")?Boolean.FALSE:null,
  "pointer",map("status",ptr==null?"THREW":ptr.equals("0x0")?"RETURNED_ZERO":"RETURNED","value",ptr),"id",map("status",id==null?"THREW":"RETURNED","value",id),"isSheetmetal",map("status",sheet==null?"THREW":"RETURNED","value",sheet));}
 @SuppressWarnings("unchecked") static Object value(Map<String,Object> r,String axis,String key){return ((Map<String,Object>)r.get(axis)).get(key);}
 static Map<String,Object> compare(Map<String,Object> a,Map<String,Object>b,boolean same){String before=json(a)+json(b);Map<String,Object> r=BodyIdentity.compare(a,b,same);check(before.equals(json(a)+json(b)));check(Boolean.FALSE.equals(r.get("conversionDecisionMade")));check(Boolean.FALSE.equals(r.get("matchingIdAloneProvesOwnership")));return r;}
 public static void main(String[]args){
  Map<String,Object>a=object("RETURNED","0x11",7,false),b=object("RETURNED","0x22",7,false);
  Map<String,Object>r=compare(a,b,false);check(Boolean.FALSE.equals(r.get("javaReferenceEqual")));check(Boolean.FALSE.equals(value(r,"pointerComparison","equal")));check(Boolean.TRUE.equals(value(r,"idComparison","equal")));check(r.get("step13CombinedGuardReconstruction").equals("FIRST_ITEM_IDENTITY_CHECK_FALSE"));
  r=compare(a,object("RETURNED","0x11",7,false),false);check(Boolean.TRUE.equals(value(r,"step13SameFunctionReconstruction","value")));check(r.get("step13CombinedGuardReconstruction").equals("NONE_OF_THREE_RECORDED_CONDITIONS_TRUE"));
  r=compare(object("RETURNED",null,null,false),object("RETURNED",null,null,false),true);check(Boolean.TRUE.equals(value(r,"step13SameFunctionReconstruction","value")));check(value(r,"pointerComparison","equal")==null);
  r=compare(a,object("RETURNED_NULL",null,null,null),false);check(r.get("step13CombinedGuardReconstruction").equals("DEFAULT_BODY_NULL"));check(r.get("javaReferenceEqual")==null);
  r=compare(object("RETURNED_NULL",null,null,null),b,false);check(r.get("step13CombinedGuardReconstruction").equals("FIRST_ITEM_IDENTITY_CHECK_FALSE"));
  r=compare(object("RETURNED","0x0",7,false),b,false);check(r.get("step13CombinedGuardReconstruction").equals("POINTER_CHECK_UNAVAILABLE_OR_WOULD_THROW"));
  r=compare(a,object("RETURNED","0x11",9,false),false);check(Boolean.TRUE.equals(value(r,"pointerComparison","equal")));check(Boolean.FALSE.equals(value(r,"idComparison","equal")));check(r.get("step13CombinedGuardReconstruction").equals("NONE_OF_THREE_RECORDED_CONDITIONS_TRUE"));
  r=compare(a,object("RETURNED","0x11",7,true),false);check(r.get("step13CombinedGuardReconstruction").equals("NATIVE_ALREADY_SHEETMETAL"));
  r=compare(a,object("RETURNED","0x11",7,null),false);check(r.get("nativeSheetmetalValue")==null);check(r.get("step13CombinedGuardReconstruction").equals("UNKNOWN_NOT_RECONSTRUCTABLE"));
  r=compare(a,object("THREW",null,null,null),false);check(r.get("step13CombinedGuardReconstruction").equals("UNKNOWN_NOT_RECONSTRUCTABLE"));check(!r.get("step13CombinedGuardReconstruction").equals("DEFAULT_BODY_NULL"));
  r=compare(a,object("RETURNED","0x22",7,true),false);check(r.get("step13CombinedGuardReconstruction").equals("FIRST_ITEM_IDENTITY_CHECK_FALSE"));check(Boolean.TRUE.equals(r.get("nativeSheetmetalValue")));
  r=compare(object("RETURNED_NULL",null,null,null),object("RETURNED_NULL",null,null,null),true);check(r.get("javaReferenceEqual")==null);check(Boolean.FALSE.equals(r.get("bothObjectsPresent")));
  System.out.println("LOGIC_JSON="+json(r));System.out.println("IDENTITY_CHECKS_PASSED="+checks);
 }
}
