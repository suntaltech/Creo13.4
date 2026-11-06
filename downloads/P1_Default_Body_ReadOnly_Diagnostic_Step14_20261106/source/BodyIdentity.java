package p1.readonly.body.step14;
import java.util.*;
import static p1.readonly.body.step14.DiagnosticSupport.*;
/** Independent comparison axes over recorded probes, not a model identity contract or conversion gate. */
public final class BodyIdentity {
 private BodyIdentity(){}
 private static boolean present(Map<String,Object> object){return Boolean.TRUE.equals(object.get("present"));}
 private static boolean nullObject(Map<String,Object> object){return "RETURNED_NULL".equals(object.get("objectState"));}
 @SuppressWarnings("unchecked") private static Map<String,Object> probe(Map<String,Object> object,String key){return (Map<String,Object>)object.getOrDefault(key,map("status","NOT_ATTEMPTED","value",null));}
 private static boolean returned(Map<String,Object> p){return "RETURNED".equals(p.get("status"));}
 private static Map<String,Object> axis(Map<String,Object> left,Map<String,Object> right){
  return returned(left)&&returned(right)?map("status","COMPARED","equal",Objects.equals(left.get("value"),right.get("value"))):map("status","UNKNOWN_NOT_COMPARABLE","equal",null,"leftStatus",left.get("status"),"rightStatus",right.get("status"));
 }
 public static Map<String,Object> compare(Map<String,Object> listed,Map<String,Object> defaulted,boolean javaEqual){
  boolean both=present(listed)&&present(defaulted);Map<String,Object> lp=probe(listed,"pointer"),dp=probe(defaulted,"pointer"),li=probe(listed,"id"),di=probe(defaulted,"id"),sheet=probe(defaulted,"isSheetmetal");
  Map<String,Object> pointers=both?axis(lp,dp):map("status","UNKNOWN_NOT_COMPARABLE","equal",null);
  Map<String,Object> ids=both?axis(li,di):map("status","UNKNOWN_NOT_COMPARABLE","equal",null);
  String sameState="UNKNOWN_OBJECT_READ";Boolean sameResult=null;
  if(nullObject(listed)||nullObject(defaulted)){sameState="WOULD_RETURN_FALSE_NULL_ARGUMENT";sameResult=false;}
  else if(both&&javaEqual){sameState="WOULD_RETURN_TRUE_JAVA_SAME_OBJECT";sameResult=true;}
  else if(both&&returned(lp)&&returned(dp)){sameState="WOULD_RETURN_POINTER_EQUALITY";sameResult=(Boolean)pointers.get("equal");}
  else if(both)sameState="WOULD_THROW_OR_HAVE_UNAVAILABLE_POINTER_CHECK";
  String guard="UNKNOWN_NOT_RECONSTRUCTABLE";
  if(nullObject(defaulted))guard="DEFAULT_BODY_NULL";
  else if(present(defaulted)&&Boolean.FALSE.equals(sameResult))guard="FIRST_ITEM_IDENTITY_CHECK_FALSE";
  else if(present(defaulted)&&Boolean.TRUE.equals(sameResult)&&returned(sheet))guard=Boolean.TRUE.equals(sheet.get("value"))?"NATIVE_ALREADY_SHEETMETAL":"NONE_OF_THREE_RECORDED_CONDITIONS_TRUE";
  else if(present(defaulted)&&sameState.equals("WOULD_THROW_OR_HAVE_UNAVAILABLE_POINTER_CHECK"))guard="POINTER_CHECK_UNAVAILABLE_OR_WOULD_THROW";
  List<String> differences=new ArrayList<>();if(both&&!javaEqual)differences.add("JAVA_REFERENCES_DIFFER");if(Boolean.FALSE.equals(pointers.get("equal")))differences.add("WRAPPER_HANDLES_DIFFER");if(Boolean.FALSE.equals(ids.get("equal")))differences.add("MODEL_SCOPED_IDS_DIFFER");
  return map("bothObjectsPresent",both,"javaReferenceEqual",both?javaEqual:null,"pointerComparison",pointers,"idComparison",ids,"differences",differences,
   "step13SameFunctionReconstruction",map("state",sameState,"value",sameResult),"step13CombinedGuardReconstruction",guard,
   "sheetmetalProbeIndependentOfIdentityComparison",true,"nativeSheetmetalProbeStatus",sheet.get("status"),"nativeSheetmetalValue",sheet.get("value"),
   "reconstructionIsActualPastStep13CallEvidence",false,"pointerEqualityIsCertifiedBodyIdentity",false,"matchingIdAloneProvesOwnership",false,"conversionDecisionMade",false);
 }
}
