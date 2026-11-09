package p1.controlled.step18;
/** Face-specific policy: scope and original selection precede ID membership; handles are not inputs. */
public final class ScopedFaceIdentity {
 private ScopedFaceIdentity(){}
 public enum Code { ACCEPTED, SELECTION_EMPTY_OR_MULTIPLE, SELECTION_PATH_NOT_EMPTY, SELECTION_NOT_FACE, SELECTION_OTHER_MODEL, FACE_ID_NOT_FOUND_IN_BODY, FACE_SCOPE_READ_EXCEPTION, FACE_BODY_SCOPE_MISMATCH, FACE_ID_CHANGED }
 public static Code selection(Integer count,boolean present,boolean pathPresent,boolean surface,boolean selectedModelMatches,boolean readFailed){
  if(readFailed)return Code.FACE_SCOPE_READ_EXCEPTION;
  if(count==null||count!=1)return Code.SELECTION_EMPTY_OR_MULTIPLE;
  if(!present||!surface)return Code.SELECTION_NOT_FACE;
  if(pathPresent)return Code.SELECTION_PATH_NOT_EMPTY;
  if(!selectedModelMatches)return Code.SELECTION_OTHER_MODEL;
  return Code.ACCEPTED;
 }
 public static Code face(boolean currentModelAndBodyScope,Integer selectedId,boolean recheck,Integer originalId,Integer matchingIdsInCurrentBody,boolean readFailed){
  if(readFailed)return Code.FACE_SCOPE_READ_EXCEPTION;
  if(!currentModelAndBodyScope)return Code.FACE_BODY_SCOPE_MISMATCH;
  if(selectedId==null||(recheck&&originalId==null)||matchingIdsInCurrentBody==null)return Code.FACE_SCOPE_READ_EXCEPTION;
  if(recheck&&!selectedId.equals(originalId))return Code.FACE_ID_CHANGED;
  if(matchingIdsInCurrentBody!=1)return Code.FACE_ID_NOT_FOUND_IN_BODY;
  return Code.ACCEPTED;
 }
}
