import p1.controlled.step21.ScopedFaceIdentity;
import static p1.controlled.step21.ScopedFaceIdentity.Code.*;
public final class ScopedFaceIdentityTest {
 static int checks;
 static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 public static void main(String[]args){
  check(ScopedFaceIdentity.selection(1,true,false,true,true,false)==ACCEPTED);
  check(ScopedFaceIdentity.selection(0,false,false,false,false,false)==SELECTION_EMPTY_OR_MULTIPLE);
  check(ScopedFaceIdentity.selection(2,true,false,true,true,false)==SELECTION_EMPTY_OR_MULTIPLE);
  check(ScopedFaceIdentity.selection(null,false,false,false,false,false)==SELECTION_EMPTY_OR_MULTIPLE);
  check(ScopedFaceIdentity.selection(1,false,false,false,true,false)==SELECTION_NOT_FACE);
  check(ScopedFaceIdentity.selection(1,true,false,false,true,false)==SELECTION_NOT_FACE);
  check(ScopedFaceIdentity.selection(1,true,true,true,true,false)==SELECTION_PATH_NOT_EMPTY);
  check(ScopedFaceIdentity.selection(1,true,false,true,false,false)==SELECTION_OTHER_MODEL);
  check(ScopedFaceIdentity.selection(1,true,false,true,true,true)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.selection(0,false,false,false,false,true)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.face(true,101,false,null,1,false)==ACCEPTED);
  check(ScopedFaceIdentity.face(true,101,true,101,1,false)==ACCEPTED);
  check(ScopedFaceIdentity.face(false,101,false,null,1,false)==FACE_BODY_SCOPE_MISMATCH);
  check(ScopedFaceIdentity.face(false,101,true,101,1,false)==FACE_BODY_SCOPE_MISMATCH);
  check(ScopedFaceIdentity.face(true,101,false,null,0,false)==FACE_ID_NOT_FOUND_IN_BODY);
  check(ScopedFaceIdentity.face(true,101,false,null,2,false)==FACE_ID_NOT_FOUND_IN_BODY);
  check(ScopedFaceIdentity.face(true,101,false,null,null,false)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.face(true,null,false,null,1,false)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.face(true,101,true,null,1,false)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.face(true,102,true,101,1,false)==FACE_ID_CHANGED);
  check(ScopedFaceIdentity.face(true,101,false,null,1,true)==FACE_SCOPE_READ_EXCEPTION);
  check(ScopedFaceIdentity.face(true,-5,false,null,1,false)==ACCEPTED);
  check(ScopedFaceIdentity.face(true,-5,true,-5,1,false)==ACCEPTED);
  check(ScopedFaceIdentity.face(true,0,false,null,1,false)==ACCEPTED);
  check(ScopedFaceIdentity.face(true,101,true,101,0,false)==FACE_ID_NOT_FOUND_IN_BODY);
  System.out.println("SCOPED_FACE_CHECKS_PASSED="+checks);
 }
}
