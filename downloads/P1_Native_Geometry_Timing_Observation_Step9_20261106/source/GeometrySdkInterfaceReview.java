package p1.readonly.geometry.step9;
import java.util.*;
/** Static supplied-SDK signature review. No WFC object cast, native invocation or reflection. */
public final class GeometrySdkInterfaceReview {
 public static final String SDK_SHA256="0f95b0cf9c88792c0bd18aff4e7a3ef730fdc3d7924ffe681779a40bfcda2319";
 private GeometrySdkInterfaceReview() { }
 public static List<Map<String,Object>> entries() {
  List<Map<String,Object>> r=new ArrayList<>();
  r.add(entry("RAY_INTERSECTIONS","com.ptc.wfc.wfcModel.WModel","ComputeRayIntersections(double, com.ptc.wfc.wfcModel.Ray)",true,
   "CANDIDATE_QUERY_SIGNATURE_ONLY","Double argument meaning/units, ray direction/normalization, trim handling, hit order/depth, tangent and duplicate/self hits, body/path scope, licensing and read-only semantics unconfirmed"));
  r.add(entry("RAY_ARGUMENT_FACTORY","com.ptc.wfc.wfcModel.wfcModel","Ray_Create(Point3D, Vector3D)",true,
   "ARGUMENT_FACTORY_NOT_EXECUTED","Ray construction alone proves no hit, material entry/exit or occlusion; argument and lifetime semantics unconfirmed"));
  r.add(entry("POINT_PROJECTION","com.ptc.wfc.wfcSolid.WSolid","ProjectPoint(Point3D, double)",true,
   "NOT_A_CONFIRMED_POINT_IN_SOLID_TEST","Double parameter and projection semantics unconfirmed; Surface/UV result is not a material-occupancy boolean"));
  r.add(entry("GEOMETRY_AT_POINT","com.ptc.wfc.wfcPart.WPart","FindGeometry(Point3D)",true,
   "NOT_A_CONFIRMED_POINT_IN_SOLID_TEST","Returns Selections; proximity, topology, tolerance, body scope and inside/outside interpretation unconfirmed"));
  r.add(entry("SHELLS_AND_VOIDS","com.ptc.wfc.wfcSolid.WSolid","FindShellsAndVoids()",true,
   "TOPOLOGY_HINT_NOT_POINT_OCCUPANCY","ShellData alone is not a certified point classifier or proof that the connecting segment remains in material"));
  r.add(entry("CLEARANCE","com.ptc.pfc.pfcInterference.SelectionEvaluator","ComputeClearance()",true,
   "NEAREST_OR_CLEARANCE_CANDIDATE_NOT_WALL_THICKNESS","Supported SelectionPair types, geometry scope, returned interference meaning, licensing, read-only semantics and occupancy proof unconfirmed"));
  r.add(entry("CRITICAL_DISTANCE","com.ptc.pfc.pfcInterference.SelectionEvaluator","ComputeNearestCriticalDistance()",true,
   "DISTANCE_CANDIDATE_NOT_MATERIAL_PROOF","Critical distance/UV/points do not certify a wall, full trimmed overlap or absence of intervening faces"));
  r.add(entry("INTERFERENCE","com.ptc.pfc.pfcInterference.SelectionEvaluator","ComputeInterference(boolean)",true,
   "DO_NOT_INVOKE_WITH_UNCONFIRMED_BOOLEAN_OR_SIDE_EFFECTS","Boolean meaning, returned geometry lifetime/side effects and whether same-body face selections are valid remain unconfirmed"));
  r.add(entry("HIT_RESULT_INTERPRETATION","com.ptc.pfc.pfcSelect.Selection","GetPoint(); GetDepth(); GetSelItem(); GetSelModel(); GetPath()",true,
   "RESULT_ACCESSOR_SIGNATURES_ONLY","Depth must not be assumed a sorted ray distance; coordinates, model path, selected body and hit ownership need verification"));
  r.add(entry("MATERIAL_ASSIGNMENT","com.ptc.wfc.wfcSolid.WSolidBody","GetMaterial()",true,
   "MATERIAL_PROPERTY_NOT_SPATIAL_OCCUPANCY","Assigned Material object does not answer whether any world point or segment lies inside material"));
  r.add(entry("DIRECT_MATERIAL_OCCUPANCY_API","INSPECTED_PUBLIC_SDK_SCOPE","NO_CONFIRMED_DIRECT_POINT_IN_SOLID_SIGNATURE_IN_INSPECTED_OWNERS",false,
   "UNCONFIRMED_NOT_AN_ABSENCE_CLAIM_FOR_ALL_SDKS","Inspected signature scope is documented; no claim that all SDKs lack a point-in-solid operation. Native/official semantic verification is still missing"));
  r.add(entry("DIRECT_OCCLUSION_API","INSPECTED_PUBLIC_SDK_SCOPE","NO_CONFIRMED_DIRECT_SEGMENT_OCCLUSION_SIGNATURE_IN_INSPECTED_OWNERS",false,
   "UNCONFIRMED","Ray/nearest-distance methods alone are not a certified segment-occlusion test; material interval and first-hit semantics must be established"));
  return r;
 }
 private static Map<String,Object> entry(String id,String owner,String signature,boolean found,String role,String unknown) {
  return GeometryEvidenceSupport.map("capability",id,"owner",owner,"inspectedSignature",signature,"signatureFoundInSuppliedSdk",found,
   "verificationLevel","OFFLINE_JAVAP_SIGNATURE_REVIEW_ONLY","suppliedSdkSha256",SDK_SHA256,"interpretation",role,"unconfirmedItems",unknown,
   "runtimeInvokedByThisStep",false,"runtimeObjectCompatibilityTested",false,"runtimeSdkIdentityAuthenticated",false,
   "readOnlySemanticContractCertified",false,"licenseAndAvailabilityConfirmed",false,"materialOccupancyProven",false,"occlusionProven",false);
 }
}
