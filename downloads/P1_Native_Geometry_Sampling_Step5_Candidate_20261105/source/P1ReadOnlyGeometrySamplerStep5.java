package p1.readonly.geometry.step5;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ptc.cipjava.jxobject_i;
import com.ptc.pfc.pfcSession.*;
import com.ptc.pfc.pfcCommand.*;
import com.ptc.pfc.pfcModel.*;
import com.ptc.pfc.pfcModelItem.*;
import com.ptc.pfc.pfcSolid.*;
import com.ptc.pfc.pfcGeometry.*;
import com.ptc.pfc.pfcUnits.*;
import com.ptc.pfc.pfcBase.UnitType;
import com.ptc.pfc.pfcBase.UVParams;
import com.ptc.pfc.pfcBase.UVOutline;
import com.ptc.pfc.pfcBase.Placement;
import com.ptc.pfc.pfcBase.Point3D;
import com.ptc.pfc.pfcBase.Vector3D;
import static p1.readonly.geometry.step5.GeometryEvidenceSupport.*;

/** Independent synchronous read-only collector. Model APIs are only called on explicit OnCommand. */
public final class P1ReadOnlyGeometrySamplerStep5 {
    public static final String COMMAND_ID = "SISWReadOnlyGeometrySample5";
    private static final String MESSAGES = "p1_readonly_sampling_step5.txt";
    private static final int MAX_BODIES = 128, MAX_SURFACES = 10000, MAX_BODY_SURFACE_REFS = 40000;
    private static final long SOFT_BUDGET_NS = 30_000_000_000L;
    private static Session session;
    private static UICommand command;
    private static Listener listener;
    private static volatile boolean active;
    private static final AtomicBoolean collecting = new AtomicBoolean();
    private P1ReadOnlyGeometrySamplerStep5() { }

    public static synchronized void start() throws Exception {
        if (active) return;
        if (command != null || listener != null) throw new IllegalStateException("Restart Creo: prior registration may still be attached");
        session = pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
        if (session == null) throw new IllegalStateException("No synchronous session");
        listener = new Listener();
        try {
            command = session.UICreateCommand(COMMAND_ID, listener);
            if (command == null) throw new IllegalStateException("UICreateCommand returned null");
            command.Designate(MESSAGES, "P1_SAMPLE5_LABEL", "P1_SAMPLE5_HELP", "P1_SAMPLE5_DESC");
            active = true;
            lifecycle("REGISTERED; command=" + COMMAND_ID + "; collection=NOT_EXECUTED", null);
        } catch (Exception | LinkageError error) {
            lifecycle("REGISTRATION_FAILED", error);
            if (command != null) {
                try { command.RemoveActionListener(listener); command = null; listener = null; }
                catch (Exception | LinkageError detachError) { lifecycle("DETACH_FAILED; restart_required", detachError); }
            } else listener = null;
            session = null;
            throw error;
        }
    }
    public static synchronized void stop() throws Exception {
        active = false;
        if (collecting.get()) throw new IllegalStateException("Collection in progress; do not unload");
        if (command != null && listener != null) command.RemoveActionListener(listener);
        command = null; listener = null; session = null;
        lifecycle("STOP_RETURNED", null);
    }
    private static void lifecycle(String message, Throwable error) {
        System.err.println("[P1GEO] " + Instant.now() + " " + message);
        if (error != null) error.printStackTrace(System.err);
    }
    private static final class Listener extends DefaultUICommandActionListener {
        @Override public void OnCommand() {
            if (!active || session == null || !collecting.compareAndSet(false, true)) {
                lifecycle("COLLECTION_NOT_ENTERED; inactive_or_busy", null); return;
            }
            String requestId = UUID.randomUUID().toString();
            try (Run run = new Run(requestId)) { run.collect(); }
            catch (Exception | LinkageError error) { lifecycle("COLLECTION_OR_LOG_IO_FAILED; requestId=" + requestId, error); }
            finally { collecting.set(false); }
        }
    }
    @FunctionalInterface private interface Read<T> { T get() throws Exception; }
    private static final class Face {
        final Surface surface;
        final Set<Integer> bodyIds = new LinkedHashSet<>();
        boolean modelListed;
        SurfaceType surfaceType;
        double[] uvBounds;
        Face(Surface value, boolean listed) { surface = value; modelListed = listed; }
    }
    private static final class Candidate {
        final int attempt;final double[] requestedUv,point;final Map<String,Object> record;
        Candidate(int attempt,double[] requestedUv,double[] point,Map<String,Object> record) {
            this.attempt=attempt;this.requestedUv=requestedUv;this.point=point;this.record=record;
        }
    }
    private static final class SamplingCounters {
        boolean initialized, finished;
        Integer targetTotal;
        int observedTargets, facesVisited, facesWithAny, facesAtQuota, attempts, accepted, rejected;
        int qualityPoolFaces, qualityImprovedFaces, qualityUnimprovedFaces, validButUnselected;
        int planeGroupsAtQuota, numericalNonCollinearPlaneGroups, numericalCollinearPlaneGroups, planeQualityUnknownGroups, planeGroupsBelowPointQuota;
        String stopReason = "NO_GLOBAL_LIMIT";
        boolean quotaComplete() { return initialized && targetTotal != null && facesAtQuota == targetTotal; }
        Map<String,Object> report() {
            return map("scope","MODEL_LISTED_PLANE_OR_CYLINDER","targetDenominator",known(targetTotal),
                    "observedTargets",observedTargets,"facesVisited",facesVisited,"facesWithAnySample",facesWithAny,
                    "facesAtThreeSampleQuota",facesAtQuota,"attempts",attempts,"acceptedSamples",accepted,"rejectedAttempts",rejected,
                    "faceAnySampleCoverage",coverage(targetTotal,facesWithAny),"faceQuotaCoverage",coverage(targetTotal,facesAtQuota),
                    "pointQuotaCoverage",coverage(targetTotal==null?null:targetTotal*GeometrySamplingMath.TARGET_PER_FACE,accepted),
                    "globalStopReason",stopReason,"coverageMeansAreaOrThicknessCoverage",false,
                    "planeGroupsAtQuota",planeGroupsAtQuota,"numericallyNonCollinearPlaneGroups",numericalNonCollinearPlaneGroups,
                    "numericallyCollinearPlaneGroups",numericalCollinearPlaneGroups,"planeQualityUnknownGroups",planeQualityUnknownGroups,
                    "planeGroupsBelowPointQuota",planeGroupsBelowPointQuota,
                    "qualityPoolFaces",qualityPoolFaces,"qualityImprovedFaces",qualityImprovedFaces,"qualityUnimprovedFaces",qualityUnimprovedFaces,
                    "validButUnselectedCandidates",validButUnselected,"nativeGateOrUniquenessRejectedAttempts",rejected-validButUnselected,
                    "qualityObjectiveStatus",qualityPoolFaces==0?"NO_POOL_ACTIVATION_OBSERVED":qualityUnimprovedFaces==0?"ALL_ACTIVATED_GROUPS_NUMERICALLY_IMPROVED":"PARTIAL_IMPROVEMENT");
        }
        void finishIfMissing(Run run) throws IOException {
            if (finished) return;
            finished=true;
            Map<String,Object> report = report();
            report.put("outcome",!initialized?"NOT_ENTERED_OR_ABORTED":targetTotal!=null&&targetTotal==0?"NO_TARGET_SURFACES":quotaComplete()?"POINT_QUOTAS_MET_ONLY":"PARTIAL_SAMPLING");
            report.put("modelIdentityFinalCheck", "SEE_COLLECTION_SUMMARY");
            run.emitTargetedRefinementResults();
            run.emit("SAMPLING_SUMMARY",report);
        }
    }
    private static final class Run implements AutoCloseable {
        final String requestId;
        final BufferedWriter writer;
        final long started = System.nanoTime();
        int errors, unknownReads, bodiesVisited, bodiesWithState, modelFaceRecords, completeModelFaceRecords, bodyRefsVisited, faceRecordsEmitted, bodyOnlyFaceRecords;
        Integer bodyTotal, modelFaceTotal;
        boolean metadataComplete, defaultStateKnown, truncated, bodyMembershipComplete = true, bodyEnumerationComplete, modelEnumerationComplete;
        String truncationReason = "NONE";
        final Map<Integer,Face> faces = new LinkedHashMap<>();
        final Set<Integer> bodyIdsSeen = new HashSet<>();
        Integer defaultBodyId;
        String lengthUnitName;
        String modelFileName;
        int modelTypesRead, validModelUvBounds;
        final SamplingCounters sampling = new SamplingCounters();
        final Map<Integer,Map<String,Object>> targetedResults = new LinkedHashMap<>();
        Run(String id) throws IOException {
            requestId = id;
            String configured = System.getenv("P1_READONLY_GEOMETRY_LOG_DIR");
            Path directory = configured == null || configured.isBlank()
                    ? Paths.get(System.getProperty("user.home"), "CreoReadOnlyGeometry") : Paths.get(configured);
            Files.createDirectories(directory);
            Path output = directory.resolve("geometry-sampling-step5-" + requestId + ".jsonl");
            writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            lifecycle("EVIDENCE_FILE=" + output + "; requestId=" + requestId, null);
        }
        void emit(String event, Map<String,Object> fields) throws IOException {
            Map<String,Object> line = map("schema", "P1_NATIVE_READONLY_GEOMETRY_V1", "timestamp", Instant.now().toString(),
                    "requestId", requestId, "commandId", COMMAND_ID, "event", event,
                    "collectorStep", "STEP_5", "samplingSchema", "P1_NATIVE_GEOMETRY_SAMPLING_V1",
                    "samplingQualitySchema", "P1_NATIVE_GEOMETRY_SAMPLING_QUALITY_V1",
                    "selectionSchema", "P1_NATIVE_PLANE_POINT_POOL_V1", "queueSchema", "P1_NATIVE_MIXED_UV_QUEUE_V1");
            line.putAll(fields);
            String json = json(line);
            writer.write(json); writer.newLine(); writer.flush();
            System.err.println("[P1GEO] " + json);
        }
        <T> T read(String operation, Read<T> action) throws IOException {
            try {
                T value = action.get();
                if (value == null) { unknownReads++; emit("READ_UNAVAILABLE", map("operation", operation, "outcome", "RETURNED_NULL")); }
                return value;
            } catch (Exception | LinkageError error) {
                errors++;
                StringWriter stack = new StringWriter(); error.printStackTrace(new PrintWriter(stack));
                emit("READ_EXCEPTION", map("operation", operation, "exceptionClass", error.getClass().getName(), "stack", stack.toString()));
                return null;
            }
        }
        void issue(String reason, Object context) throws IOException { errors++; emit("DATA_ISSUE", map("reason", reason, "context", context)); }
        boolean withinBudget() {
            if (System.nanoTime() - started > SOFT_BUDGET_NS) { truncated = true; truncationReason = "SOFT_TIME_BUDGET"; return false; }
            return true;
        }
        void limit(String reason) { truncated = true; truncationReason = reason; }
        Map<String,Object> identity(Model model, String stage) throws IOException {
            String file = read(stage + ".GetFileName", model::GetFileName);
            if (file != null) file = file.replace('\\', '/').substring(file.replace('\\', '/').lastIndexOf('/') + 1);
            String revision = read(stage + ".GetRevision", model::GetRevision);
            if (revision != null && revision.isBlank()) revision = null;
            Boolean dirty = read(stage + ".GetIsModified", model::GetIsModified);
            String stamp = clean(read(stage + ".GetVersionStamp", model::GetVersionStamp));
            return map("fileName", file, "revision", revision, "isModified", dirty,
                    "versionStamp", stamp, "versionStampSource", "Model.GetVersionStamp",
                    "revisionSource", "Model.GetRevision");
        }
        void collect() throws IOException {
            emit("COLLECTION_STARTED", map("modelApiPolicy", "READ_ONLY", "geometrySampling", "ADDITIVE_STEP_2_EVENTS",
                    "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE", "uniformThicknessAssessment", "INSUFFICIENT_EVIDENCE",
                    "limits", map("bodies", MAX_BODIES, "uniqueSurfaces", MAX_SURFACES, "bodySurfaceRefs", MAX_BODY_SURFACE_REFS, "softBudgetSeconds", 30)));
            read("Session.UIDisplayMessage(STARTED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_SAMPLE5_STARTED", null); return Boolean.TRUE; });
            Map<String,Object> before = null, after = null;
            Model model = null;
            boolean eligible = false, fatal = false, identityStable = false, identityChanged = false, stampContextMatches = false;
            try {
                model = read("Session.GetActiveModel.before", session::GetActiveModel);
                if (model == null) { issue("ACTIVE_MODEL_UNAVAILABLE_OR_NULL", null); return; }
                ModelType type = read("Model.GetType", model::GetType);
                if (type != ModelType.MDL_PART || !(model instanceof Solid)) { issue("PART_TYPE_OR_SOLID_INTERFACE_NOT_CONFIRMED", null); return; }
                eligible = true;
                before = identity(model, "before"); modelFileName = (String)before.get("fileName"); emit("MODEL_BEFORE", before);
                Solid solid = (Solid)model; // Public interface guard above; no WSolid cast or internal reflection.
                UnitSystem system = read("Solid.GetPrincipalUnits", solid::GetPrincipalUnits);
                Unit length = system == null ? null : read("UnitSystem.GetUnit(UNIT_LENGTH)", () -> system.GetUnit(UnitType.UNIT_LENGTH));
                String unitName = length == null ? null : clean(read("Unit.GetName", length::GetName));
                lengthUnitName = unitName;
                String unitExpression = length == null ? null : clean(read("Unit.GetExpression", length::GetExpression));
                Double rawRelative = read("Solid.GetRelativeAccuracy", solid::GetRelativeAccuracy);
                Double rawAbsolute = read("Solid.GetAbsoluteAccuracy", solid::GetAbsoluteAccuracy);
                Double relative = positive(rawRelative), absolute = positive(rawAbsolute);
                if (rawRelative != null && relative == null) issue("INVALID_RELATIVE_ACCURACY", null);
                if (rawAbsolute != null && absolute == null) issue("INVALID_ABSOLUTE_ACCURACY", null);
                metadataComplete = unitName != null && !unitName.isBlank() && before.get("revision") != null && (relative != null || absolute != null);
                emit("UNITS_AND_ACCURACY", map("lengthUnitName", known(unitName), "lengthUnitExpression", known(unitExpression),
                        "relativeAccuracy", known(relative), "absoluteAccuracyInModelLengthUnits", known(absolute),
                        "effectiveAccuracyMode", "UNVERIFIED_API_VALUES_ONLY", "areaUnits", unitName == null ? "UNKNOWN" : "(" + unitName + ")^2"));
                SolidBody defaultBody = read("Solid.GetDefaultBody", solid::GetDefaultBody);
                if (defaultBody != null) {
                    defaultBodyId = read("DefaultBody.GetId", defaultBody::GetId);
                    Boolean flag = read("DefaultBody.IsSheetmetal", defaultBody::IsSheetmetal);
                    defaultStateKnown = defaultBodyId != null && flag != null;
                    emit("DEFAULT_BODY", map("bodyId", known(defaultBodyId), "nativeIsSheetmetal", known(flag),
                            "nativeStateSource", "SolidBody.IsSheetmetal", "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE"));
                } else emit("DEFAULT_BODY", map("bodyId", "UNKNOWN", "nativeIsSheetmetal", "UNKNOWN"));
                enumerateModelFaces(model);
                enumerateBodies(model);
                if (defaultBody != null && (defaultBodyId == null || !bodyIdsSeen.contains(defaultBodyId)) && withinBudget()) {
                    issue("DEFAULT_NOT_CONFIRMED_IN_BODY_ENUM", defaultBodyId);
                    emit("DEFAULT_BODY_FALLBACK_SCOPE", map("reason", "DEFAULT_NOT_CONFIRMED_IN_BODY_ENUM", "allBodiesCoveragePromoted", false));
                    collectBody(defaultBody, false);
                }
                for (Map.Entry<Integer,Face> entry : faces.entrySet()) {
                    if (!withinBudget()) break;
                    Face face = entry.getValue(); Integer id = entry.getKey();
                    SurfaceType kind = read("Surface[" + id + "].GetSurfaceType", face.surface::GetSurfaceType);
                    face.surfaceType = kind; if (face.modelListed && kind != null) modelTypesRead++;
                    Double area = read("Surface[" + id + "].EvalArea", face.surface::EvalArea);
                    face.uvBounds=readUvBounds(id,face.surface);
                    if(face.modelListed&&GeometrySamplingMath.boundsValid(face.uvBounds)) validModelUvBounds++;
                    emit("SURFACE_UV_RANGE",map("surfaceId",id,"modelListed",face.modelListed,"surfaceTypeName",typeName(kind),
                            "rawUvExtentValues",GeometrySamplingMath.safeValues(face.uvBounds),"valueOrder",Arrays.asList("u0","u1","v0","v1"),
                            "numericUvRangeValid",GeometrySamplingMath.boundsValid(face.uvBounds),"nonFiniteUvValuesEncodedAsStrings",true,
                            "uvRangeSource","Surface.GetUVExtents/UVOutline.get/UVParams.get","rangeIsTrimDomainProof",false));
                    if (positive(area) == null) { area = null; issue("INVALID_OR_UNKNOWN_SURFACE_AREA", id); }
                    String ownerStatus = face.bodyIds.size() == 1 ? "BODY_LIST_MEMBERSHIP" : face.bodyIds.isEmpty() ? "OWNER_UNKNOWN" : "AMBIGUOUS_MEMBERSHIP";
                    if (face.bodyIds.size() > 1) issue("AMBIGUOUS_BODY_MEMBERSHIP", id);
                    boolean complete = kind != null && area != null && face.bodyIds.size() == 1 && bodyMembershipComplete;
                    if (face.modelListed) { modelFaceRecords++; if (complete) completeModelFaceRecords++; }
                    faceRecordsEmitted++; if (!face.modelListed) bodyOnlyFaceRecords++;
                    emit("SURFACE", map("surfaceId", id, "bodyId", face.bodyIds.size() == 1 ? face.bodyIds.iterator().next() : "UNKNOWN",
                            "observedBodyIds", new ArrayList<>(face.bodyIds), "bodyAssociationStatus", ownerStatus,
                            "bodyMembershipEnumerationComplete", bodyMembershipComplete, "modelListed", face.modelListed,
                            "surfaceTypeCode", kind == null ? "UNKNOWN" : kind.getValue(), "surfaceTypeName", typeName(kind),
                            "areaInModelLengthUnitsSquared", known(area), "requiredFieldsComplete", complete,
                            "sampling", "SEE_STEP2_EVENTS", "unsampledReason", "POINT_DATA_NOT_IN_STEP1_SURFACE_RECORD"));
                }
                sampleTargets();
            } catch (Exception | LinkageError error) {
                fatal = true; errors++;
                StringWriter stack = new StringWriter(); error.printStackTrace(new PrintWriter(stack));
                emit("COLLECTION_EXCEPTION", map("exceptionClass", error.getClass().getName(), "stack", stack.toString()));
            } finally {
                if (model != null) {
                    Model current = read("Session.GetActiveModel.after", session::GetActiveModel);
                    after = identity(model, "afterOriginalModel"); emit("MODEL_AFTER_ORIGINAL", after);
                    Boolean handleMatch = current instanceof jxobject_i && model instanceof jxobject_i
                            ? ((jxobject_i)current).getptr() == ((jxobject_i)model).getptr() : null;
                    Map<String,Object> currentIdentity = current == null ? null : identity(current, "afterActiveModel");
                    emit("MODEL_AFTER_ACTIVE", map("identity", currentIdentity, "originalHandleMatch", known(handleMatch)));
                    if (before != null && currentIdentity != null) {
                        stampContextMatches = Boolean.TRUE.equals(handleMatch)
                                && stable(before.get("fileName"), after.get("fileName"))
                                && stable(before.get("fileName"), currentIdentity.get("fileName"))
                                && stable(before.get("versionStamp"), after.get("versionStamp"))
                                && stable(before.get("versionStamp"), currentIdentity.get("versionStamp"))
                                && stable(before.get("isModified"), after.get("isModified"))
                                && stable(before.get("isModified"), currentIdentity.get("isModified"));
                        identityStable = Boolean.TRUE.equals(handleMatch) && stable(before.get("fileName"), after.get("fileName"))
                                && stable(before.get("revision"), after.get("revision")) && stable(before.get("revision"), currentIdentity.get("revision"))
                                && stable(before.get("fileName"), currentIdentity.get("fileName"));
                        identityChanged = Boolean.FALSE.equals(handleMatch)
                                || differs(before.get("fileName"), after.get("fileName")) || differs(before.get("revision"), after.get("revision"))
                                || differs(before.get("isModified"), after.get("isModified"))
                                || differs(before.get("fileName"), currentIdentity.get("fileName"))
                                || differs(before.get("revision"), currentIdentity.get("revision"))
                                || differs(before.get("isModified"), currentIdentity.get("isModified"))
                                || differs(before.get("versionStamp"), after.get("versionStamp"))
                                || differs(before.get("versionStamp"), currentIdentity.get("versionStamp"));
                    }
                }
                sampling.finishIfMissing(this);
                emit("STEP2_IDENTITY_CONTEXT", map("versionStampContextMatches", stampContextMatches,
                        "versionStampIsRevision", false, "legacyRevisionStrictIdentityStable", identityStable,
                        "nativeRevisionSubstituted", false, "modelWriteAbsenceIndependentlyProven", false));
                read("Session.UIDisplayMessage(FINISHED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_SAMPLE5_FINISHED", null); return Boolean.TRUE; });
                boolean requiredReadsComplete = !truncated && identityStable && errors == 0 && bodyEnumerationComplete
                        && modelEnumerationComplete && bodyMembershipComplete && metadataComplete && defaultStateKnown
                        && modelFaceTotal != null && completeModelFaceRecords == modelFaceTotal
                        && bodyTotal != null && bodiesWithState == bodyTotal && sampling.quotaComplete()
                        && modelFaceTotal!=null && validModelUvBounds==modelFaceTotal && sampling.qualityUnimprovedFaces==0;
                String outcome = completion(eligible, identityChanged, fatal, requiredReadsComplete);
                emit("COLLECTION_SUMMARY", map("outcome", outcome, "errors", errors, "unknownReadValues", unknownReads,
                        "identityStableWithinObservedFields", identityStable, "identityOrDirtyChangeObserved", identityChanged,
                        "metadataComplete", metadataComplete, "defaultBodyStateKnown", defaultStateKnown,
                        "allBodyEnumerationComplete", bodyEnumerationComplete, "bodyMembershipEnumerationComplete", bodyMembershipComplete,
                        "modelSurfaceEnumerationComplete", modelEnumerationComplete,
                        "bodyStateCoverage", coverage(bodyTotal, bodiesWithState),
                        "modelSurfaceRecordCoverage", coverage(modelFaceTotal, modelFaceRecords),
                        "modelSurfaceRequiredFieldCoverage", coverage(modelFaceTotal, completeModelFaceRecords),
                        "modelUvRangeCoverage",coverage(modelFaceTotal,validModelUvBounds),
                        "uniqueFacesRetained", faces.size(), "surfaceRecordsEmitted", faceRecordsEmitted, "bodyOnlySurfaceRecords", bodyOnlyFaceRecords,
                        "modelSurfaceRecordsNotProduced", modelFaceTotal == null ? "UNKNOWN" : modelFaceTotal - modelFaceRecords,
                        "modelSurfaceRecordsWithIncompleteFields", modelFaceRecords - completeModelFaceRecords,
                        "bodySurfaceReferencesVisited", bodyRefsVisited,
                        "truncated", truncated, "truncationReason", truncationReason, "elapsedMs", (System.nanoTime() - started) / 1_000_000,
                        "nativeBodyStateDoesNotEstablishThinWallShape", true,
                        "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE", "uniformThicknessAssessment", "INSUFFICIENT_EVIDENCE",
                        "samplingCoverage", "SEE_STEP2_SAMPLING_SUMMARY", "step2SamplingCoverage", sampling.report(),
                        "versionStampContextMatches", stampContextMatches,
                        "samplingIdentityDisposition", identityChanged ? "INVALIDATED_OBSERVED_MODEL_CHANGE"
                                : stampContextMatches ? "OBSERVED_STAMP_FILE_HANDLE_DIRTY_CONTEXT_MATCH_NOT_INDEPENDENT_PROOF"
                                : "IDENTITY_CONTEXT_UNCONFIRMED",
                        "modelWriteAbsenceIndependentlyProven", false));
            }
        }
        double[] readUvBounds(Integer id, Surface surface) throws IOException {
            return read("Surface[" + id + "].GetUVExtents/UVOutline.get/UVParams.get", () -> {
                UVOutline range=surface.GetUVExtents(); if(range==null) return null;
                UVParams lower=range.get(0),upper=range.get(1); if(lower==null||upper==null) return null;
                return new double[]{lower.get(0),upper.get(0),lower.get(1),upper.get(1)};
            });
        }
        void emitTargetedRefinementResults() throws IOException {
            List<Map<String,Object>> values=new ArrayList<>();
            for(Integer id:GeometrySamplingMath.REFINEMENT_SURFACE_IDS) {
                Map<String,Object> recorded=targetedResults.get(id);
                if(recorded!=null) { values.add(recorded);continue; }
                Face face=faces.get(id);
                String status=modelFileName==null?"UNKNOWN_MODEL_IDENTITY"
                        :!GeometrySamplingMath.REFINEMENT_MODEL_FILE.equalsIgnoreCase(modelFileName)?"NOT_APPLICABLE_MODEL_NAME_MISMATCH"
                        :face==null?(modelEnumerationComplete?"NOT_FOUND_IN_RETAINED_ENUMERATION":"UNKNOWN_ENUMERATION_INCOMPLETE")
                        :!face.modelListed?"OUTSIDE_MODEL_LISTED_TARGET_SCOPE"
                        :face.surfaceType==null?"UNKNOWN_SURFACE_TYPE"
                        :face.surfaceType.getValue()!=SurfaceType._SURFACE_PLANE?"NOT_APPLICABLE_NON_PLANE"
                        :"NOT_ATTEMPTED_LIMIT_OR_ABORT";
                values.add(map("surfaceId",id,"status",status,"acceptedSamples","UNKNOWN"));
            }
            emit("TARGET_REFINEMENT_RESULTS",map("caseModel",GeometrySamplingMath.REFINEMENT_MODEL_FILE,
                    "observedModel",known(modelFileName),"results",values,"nativeInsideGateUnchanged",true));
        }
        void sampleTargets() throws IOException {
            List<Map.Entry<Integer,Face>> targets = new ArrayList<>();
            for (Map.Entry<Integer,Face> entry : faces.entrySet()) {
                Face face = entry.getValue();
                if (face.modelListed && face.surfaceType != null && (face.surfaceType.getValue() == SurfaceType._SURFACE_PLANE
                        || face.surfaceType.getValue() == SurfaceType._SURFACE_CYLINDER)) targets.add(entry);
            }
            sampling.initialized = true;
            sampling.observedTargets = targets.size();
            sampling.targetTotal = modelFaceTotal != null && modelEnumerationComplete && modelFaceRecords == modelFaceTotal
                    && modelTypesRead == modelFaceTotal ? targets.size() : null;
            emit("SAMPLING_STARTED", map("scope", "MODEL_LISTED_PLANE_OR_CYLINDER", "targetDenominator", known(sampling.targetTotal),
                    "observedTargets", targets.size(), "samplesPerTarget", GeometrySamplingMath.TARGET_PER_FACE,
                    "maxCandidatesPerFace", GeometrySamplingMath.CANDIDATES_PER_FACE+GeometrySamplingMath.REFINEMENT_CANDIDATES_PER_FACE+GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE,
                    "maxQualityPoolSize",GeometrySamplingMath.MAX_QUALITY_POOL_SIZE,"maxQualityProbesPerFace",GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE,
                    "maxBaseCandidatesPerFace",GeometrySamplingMath.CANDIDATES_PER_FACE,
                    "maxTargetFaces", GeometrySamplingMath.MAX_TARGET_FACES, "maxTotalAttempts", GeometrySamplingMath.MAX_TOTAL_ATTEMPTS,
                    "maxAcceptedSamples", GeometrySamplingMath.MAX_ACCEPTED_SAMPLES,
                    "refinementModelFile",GeometrySamplingMath.REFINEMENT_MODEL_FILE,
                    "refinementSurfaceIds",new ArrayList<>(GeometrySamplingMath.REFINEMENT_SURFACE_IDS),
                    "maxRefinementCandidatesPerFace",GeometrySamplingMath.REFINEMENT_CANDIDATES_PER_FACE,
                    "qualityGateChangesNativeAcceptance",false,
                    "planeNumericalThreshold",GeometrySamplingMath.NUMERICAL_TRIANGLE_QUALITY_THRESHOLD,
                    "finalPointSelection","INITIAL_THREE_PRESERVED_UNLESS_BOUNDED_POOL_FINDS_NONDEGENERATE_NONDEGRADING_SPREAD",
                    "acceptance", "VERIFY_REQUESTED_INSIDE_AND_RETURNED_UV_EXACT_AND_VERIFY_RETURNED_INSIDE_FINITE_POINT_NORMAL_KNOWN_LENGTH_UNIT"));
            for (Map.Entry<Integer,Face> entry : targets) {
                if (!withinBudget()) { sampling.stopReason = "SOFT_TIME_BUDGET"; break; }
                if (sampling.facesVisited >= GeometrySamplingMath.MAX_TARGET_FACES) { sampling.stopReason = "MAX_TARGET_FACES"; limit("SAMPLING_MAX_TARGET_FACES"); break; }
                if (sampling.attempts >= GeometrySamplingMath.MAX_TOTAL_ATTEMPTS) { sampling.stopReason = "MAX_TOTAL_ATTEMPTS"; limit("SAMPLING_MAX_TOTAL_ATTEMPTS"); break; }
                if (sampling.accepted >= GeometrySamplingMath.MAX_ACCEPTED_SAMPLES) { sampling.stopReason = "MAX_ACCEPTED_SAMPLES"; limit("SAMPLING_MAX_ACCEPTED_SAMPLES"); break; }
                sampling.facesVisited++;
                sampleFace(entry.getKey(), entry.getValue());
            }
            if (!withinBudget()) sampling.stopReason = "SOFT_TIME_BUDGET";
            List<Integer> unattempted = new ArrayList<>();
            for (int i = sampling.facesVisited; i < targets.size(); i++) unattempted.add(targets.get(i).getKey());
            if (!unattempted.isEmpty()) emit("SAMPLING_TARGETS_NOT_ATTEMPTED", map("reason", sampling.stopReason,
                    "surfaceIds", unattempted, "count", unattempted.size()));
            sampling.finishIfMissing(this);
        }
        void sampleFace(Integer id, Face face) throws IOException {
            int startAttempts=sampling.attempts,baseAttempts=0,targetedAttempts=0,qualityAttempts=0;
            Map<String,Integer> rejectionCounts=new LinkedHashMap<>();
            Map<String,int[]> strategyStats=new LinkedHashMap<>();
            for(String strategy:Arrays.asList(GeometryCandidateQueue.LOCAL,GeometryCandidateQueue.INTERNAL,GeometryCandidateQueue.EDGE)) strategyStats.put(strategy,new int[6]);
            List<Candidate> pool=new ArrayList<>();
            List<double[]> poolPoints=new ArrayList<>(),attemptedUvs=new ArrayList<>();
            boolean plane=face.surfaceType.getValue()==SurfaceType._SURFACE_PLANE;
            boolean refinementSelected=GeometrySamplingMath.refinementApplies(modelFileName,id,plane);
            boolean qualityPoolActivated=false;
            String stopReason="CANDIDATE_LIMIT_OR_NO_VALID_INSIDE_POINT",qualityStopReason="NOT_ACTIVATED";
            SurfaceOrientation orientation=read("Surface["+id+"].GetOrientation",face.surface::GetOrientation);
            Integer orientationCode=orientation==null?null:orientation.getValue();
            double[] bounds=face.uvBounds;
            List<double[]> candidates=null;int baseCandidateCount=0;
            if(lengthUnitName==null) stopReason="LENGTH_UNIT_UNKNOWN_NO_VALIDATED_SAMPLES";
            else if(bounds==null) stopReason="UV_EXTENTS_UNAVAILABLE";
            else try {
                candidates=GeometrySamplingMath.candidates(bounds[0],bounds[1],bounds[2],bounds[3]);
                baseCandidateCount=candidates.size();
                if(refinementSelected) candidates.addAll(GeometrySamplingMath.refinementCandidates(bounds[0],bounds[1],bounds[2],bounds[3],candidates));
            } catch(IllegalArgumentException error) { stopReason="UV_EXTENTS_INVALID_OR_DEGENERATE";candidates=null; }
            emit("SURFACE_CANDIDATE_PLAN",map("surfaceId",id,"surfaceTypeName",typeName(face.surfaceType),
                    "rawUvExtentValues",GeometrySamplingMath.safeValues(bounds),"valueOrder",Arrays.asList("u0","u1","v0","v1"),
                    "rangeValidForCandidates",candidates!=null,"numericUvRangeValid",GeometrySamplingMath.boundsValid(bounds),
                    "nonFiniteUvValuesEncodedAsStrings",true,"rangeIsTrimDomainProof",false,
                    "modelFileMatchesRefinementCase",GeometrySamplingMath.REFINEMENT_MODEL_FILE.equalsIgnoreCase(modelFileName),
                    "refinementSelected",refinementSelected,"baseCandidateCount",baseCandidateCount,
                    "refinementCandidateCount",candidates==null?0:candidates.size()-baseCandidateCount,
                    "maximumCandidateCountForThisFace",(candidates==null?0:candidates.size())+(plane?GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE:0),
                    "maxQualityProbes",plane?GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE:0,
                    "qualityPoolActivation","INITIAL_THREE_PLANE_POINTS_NOT_NUMERICALLY_NONCOLLINEAR"));
            if(candidates!=null) for(int i=0;i<candidates.size()&&pool.size()<GeometrySamplingMath.TARGET_PER_FACE;i++) {
                if(!canProbe()) { stopReason=sampling.stopReason;break; }
                if(i==baseCandidateCount) emit("SURFACE_REFINEMENT_STARTED",map("surfaceId",id,"reason","BASE_QUOTA_NOT_MET",
                        "acceptedAfterBase",pool.size(),"baseCandidateCount",baseCandidateCount,
                        "refinementCandidatesAvailable",candidates.size()-baseCandidateCount,"nativeInsideGateUnchanged",true));
                String phase=i<baseCandidateCount?"BASE":"TARGETED_REFINEMENT";
                if(i<baseCandidateCount) baseAttempts++;else targetedAttempts++;
                sampling.attempts++;int attempt=sampling.attempts-startAttempts;
                double[] uv=candidates.get(i);attemptedUvs.add(uv);
                Candidate valid=validateCandidate(id,face,uv,phase,attempt,orientationCode,poolPoints,rejectionCounts,null);
                if(valid!=null) { pool.add(valid);poolPoints.add(valid.point); }
            }
            Map<String,Object> initialQuality=plane?GeometrySamplingMath.xyzQuality(poolPoints):map("numericallyNonCollinear","NOT_PLANE");
            if(pool.size()>=GeometrySamplingMath.TARGET_PER_FACE) stopReason="TARGET_QUOTA_MET";
            else if(refinementSelected&&candidates!=null&&baseAttempts+targetedAttempts>=candidates.size()) stopReason="BASE_AND_REFINEMENT_EXHAUSTED_QUOTA_NOT_MET";
            if(plane&&pool.size()==3&&!Boolean.TRUE.equals(initialQuality.get("numericallyNonCollinear"))) {
                qualityPoolActivated=true;sampling.qualityPoolFaces++;
                emit("PLANE_QUALITY_POOL_STARTED",map("surfaceId",id,"initialQuality",initialQuality,
                        "initialAttemptOrdinals",candidateOrdinals(pool),"thresholdUnchanged",true,
                        "maxPoolSize",GeometrySamplingMath.MAX_QUALITY_POOL_SIZE,"maxAdditionalProbes",GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE));
                for(Candidate candidate:pool) emitPoolCandidate(candidate);
                List<double[]> knownInsideUvs=new ArrayList<>();for(Candidate candidate:pool) knownInsideUvs.add(candidate.requestedUv);
                List<GeometryCandidateQueue.Seed> extra=GeometryCandidateQueue.build(bounds,knownInsideUvs,attemptedUvs);
                List<String> plannedOrder=new ArrayList<>();
                for(GeometryCandidateQueue.Seed seed:extra) { strategyStats.get(seed.strategy)[0]++;plannedOrder.add(seed.strategy); }
                emit("QUALITY_CANDIDATE_QUEUE",map("surfaceId",id,"plannedCount",extra.size(),"strategyOrder",plannedOrder,
                        "strategyStats",strategyReport(strategyStats),"orderPolicy","LOCAL_INTERNAL_LOCAL_EDGE_REPEAT_8",
                        "knownInsideSeedAttemptOrdinals",candidateOrdinals(pool),"candidateBoundsAreNotTrimDomainProof",true,
                        "maxQualityProbes",GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE,"runtimeBudgetAndPoolMayStopEarly",true));
                qualityStopReason="QUALITY_CANDIDATE_QUEUE_EXHAUSTED";
                for(GeometryCandidateQueue.Seed seed:extra) {
                    double[] uv=seed.uv;
                    if(pool.size()>=GeometrySamplingMath.MAX_QUALITY_POOL_SIZE) { qualityStopReason="QUALITY_POOL_FULL";break; }
                    if(qualityAttempts>=GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE) { qualityStopReason="QUALITY_PROBE_LIMIT";break; }
                    if(!canProbe()) { qualityStopReason=sampling.stopReason;stopReason=sampling.stopReason;break; }
                    sampling.attempts++;qualityAttempts++;int attempt=sampling.attempts-startAttempts;
                    int[] stats=strategyStats.get(seed.strategy);stats[1]++;
                    attemptedUvs.add(uv);
                    Candidate valid=validateCandidate(id,face,uv,"QUALITY_POOL",attempt,orientationCode,poolPoints,rejectionCounts,seed);
                    if(valid!=null) { stats[2]++;pool.add(valid);poolPoints.add(valid.point);emitPoolCandidate(valid); } else stats[3]++;
                }
                if(pool.size()>=GeometrySamplingMath.MAX_QUALITY_POOL_SIZE) qualityStopReason="QUALITY_POOL_FULL";
                else if(qualityAttempts>=GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE) qualityStopReason="QUALITY_PROBE_LIMIT";
            }
            Map<String,Object> selection=qualityPoolActivated?GeometryPoolSelection.select(poolPoints):map("status","UNCHANGED_INITIAL_QUOTA",
                    "replacementApplied",false,"initialNumericallyNonCollinear",initialQuality.get("numericallyNonCollinear"));
            List<Integer> indices=new ArrayList<>();
            if(qualityPoolActivated) for(Object index:(Iterable<?>)selection.get("selectedIndices")) indices.add((Integer)index);
            else for(int i=0;i<Math.min(3,pool.size());i++) indices.add(i);
            List<double[]> acceptedPoints=new ArrayList<>(),acceptedUvs=new ArrayList<>();
            List<Integer> finalOrdinals=new ArrayList<>();boolean usesQualityCandidate=false;
            for(int i=0;i<pool.size();i++) {
                Candidate candidate=pool.get(i);
                int[] strategyCounts="QUALITY_POOL".equals(candidate.record.get("candidatePhase"))?strategyStats.get(candidate.record.get("candidateStrategy")):null;
                if(indices.contains(i)) {
                    if(strategyCounts!=null) strategyCounts[4]++;
                    sampling.accepted++;acceptedPoints.add(candidate.point);acceptedUvs.add(candidate.requestedUv);finalOrdinals.add(candidate.attempt);
                    Map<String,Object> record=new LinkedHashMap<>(candidate.record);
                    record.put("selectionPolicy",qualityPoolActivated?"FINAL_VALIDATED_PLANE_POOL":"UNCHANGED_FIRST_QUOTA");
                    record.put("poolSelectionDeferredUntilFaceEnd",true);
                    if("QUALITY_POOL".equals(record.get("candidatePhase"))) usesQualityCandidate=true;
                    emit("POINT_NORMAL_SAMPLE",record);
                } else {
                    if(strategyCounts!=null) strategyCounts[5]++;
                    sampling.rejected++;sampling.validButUnselected++;
                    String reason="VALID_NATIVE_CANDIDATE_NOT_SELECTED_BY_QUALITY";
                    rejectionCounts.put(reason,rejectionCounts.getOrDefault(reason,0)+1);
                    emit("SAMPLE_REJECTED",map("surfaceId",id,"attemptOrdinal",candidate.attempt,
                            "requestedUv",GeometrySamplingMath.list(candidate.requestedUv),"reason",reason,"verifyPlacementCode",Placement._PLACE_INSIDE,
                            "candidatePhase",candidate.record.get("candidatePhase"),"candidateStrategy",candidate.record.get("candidateStrategy"),
                            "knownInsideSeedOrdinal",candidate.record.get("knownInsideSeedOrdinal"),"localOffsetRangeFraction",candidate.record.get("localOffsetRangeFraction"),
                            "sampleAccepted",false,"candidateNativeValid",true,"rejectionStage","FINAL_QUOTA_SELECTION_NOT_DOMAIN_VALIDITY"));
                }
            }
            int accepted=acceptedPoints.size();
            Map<String,Object> finalQuality=plane?GeometrySamplingMath.xyzQuality(acceptedPoints):map("numericallyNonCollinear","NOT_PLANE");
            if(qualityPoolActivated) {
                boolean improved=Boolean.TRUE.equals(finalQuality.get("numericallyNonCollinear"))&&!Boolean.TRUE.equals(initialQuality.get("numericallyNonCollinear"));
                if(improved) sampling.qualityImprovedFaces++;else sampling.qualityUnimprovedFaces++;
                selection.put("surfaceId",id);selection.put("poolSize",pool.size());selection.put("qualityProbeAttempts",qualityAttempts);
                selection.put("qualityStopReason",qualityStopReason);selection.put("finalAttemptOrdinals",finalOrdinals);
                selection.put("strategyStats",strategyReport(strategyStats));
                selection.put("initialQuality",initialQuality);selection.put("finalQuality",finalQuality);
                selection.put("numericalQualityImproved",improved);selection.put("nativeInsideGateUnchanged",true);
                emit("PLANE_POOL_SELECTION",selection);
            }
            if(plane) {
                finalQuality.putAll(GeometrySamplingMath.uvQuality(acceptedUvs,bounds));
                finalQuality.put("surfaceId",id);finalQuality.put("pointLengthUnit",known(lengthUnitName));finalQuality.put("acceptedPointCount",accepted);
                finalQuality.put("nativePointAcceptanceRevoked",false);finalQuality.put("qualityIsAreaOrThicknessCoverage",false);
                finalQuality.put("initialNumericallyNonCollinear",initialQuality.get("numericallyNonCollinear"));
                finalQuality.put("qualityPoolActivated",qualityPoolActivated);finalQuality.put("qualityPoolSize",pool.size());finalQuality.put("qualityProbeAttempts",qualityAttempts);
                finalQuality.put("numericalThresholdChanged",false);
                emit("PLANE_SAMPLE_QUALITY",finalQuality);
                if(accepted>=3) {
                    sampling.planeGroupsAtQuota++;
                    Object flag=finalQuality.get("numericallyNonCollinear");
                    if(Boolean.TRUE.equals(flag)) sampling.numericalNonCollinearPlaneGroups++;
                    else if(Boolean.FALSE.equals(flag)) sampling.numericalCollinearPlaneGroups++;
                    else sampling.planeQualityUnknownGroups++;
                } else sampling.planeGroupsBelowPointQuota++;
            }
            if(accepted>0) sampling.facesWithAny++;
            if(accepted>=3) sampling.facesAtQuota++;
            if(refinementSelected) targetedResults.put(id,map("surfaceId",id,"status",accepted>=3
                    ?(targetedAttempts>0?"POINT_QUOTA_MET_AFTER_REFINEMENT":"POINT_QUOTA_MET_IN_BASE")
                    :accepted>0?"PARTIAL_POINTS":"NO_ACCEPTED_POINT","acceptedSamples",accepted,"baseAttempts",baseAttempts,
                    "refinementAttempts",targetedAttempts,"qualityAttempts",qualityAttempts,"finalSelectionUsesQualityPool",usesQualityCandidate,"stopReason",stopReason));
            emit("SURFACE_SAMPLING_SUMMARY",map("surfaceId",id,"targetSamples",3,"attempts",sampling.attempts-startAttempts,
                    "acceptedSamples",accepted,"stopReason",stopReason,"rejectionsByReason",rejectionCounts,"surfaceOrientationCode",known(orientationCode),
                    "refinementSelected",refinementSelected,"baseAttempts",baseAttempts,"refinementAttempts",targetedAttempts,
                    "qualityAttempts",qualityAttempts,"qualityPoolActivated",qualityPoolActivated,"qualityPoolSize",pool.size(),"qualityStopReason",qualityStopReason,
                    "strategyStats",strategyReport(strategyStats),
                    "nativeValidCandidates",pool.size(),"validButUnselectedCandidates",pool.size()-accepted,
                    "uvRangeInterpretation","GET_UV_EXTENTS_CANDIDATE_RANGE_NOT_PROOF_OF_TRIM_DOMAIN","uniformThicknessAssessment","INSUFFICIENT_EVIDENCE"));
            if(!withinBudget()) sampling.stopReason="SOFT_TIME_BUDGET";
        }
        List<Map<String,Object>> strategyReport(Map<String,int[]> stats) {
            List<Map<String,Object>> result=new ArrayList<>();
            for(Map.Entry<String,int[]> entry:stats.entrySet()) {
                int[] v=entry.getValue();result.add(map("strategy",entry.getKey(),"planned",v[0],"attempted",v[1],
                        "nativeValidCandidates",v[2],"nativeGateOrUniquenessRejected",v[3],"finalSelected",v[4],"validButUnselected",v[5]));
            }
            return result;
        }
        boolean canProbe() {
            if(!withinBudget()) { sampling.stopReason="SOFT_TIME_BUDGET";return false; }
            if(sampling.attempts>=GeometrySamplingMath.MAX_TOTAL_ATTEMPTS) { sampling.stopReason="MAX_TOTAL_ATTEMPTS";limit("SAMPLING_MAX_TOTAL_ATTEMPTS");return false; }
            if(sampling.accepted>=GeometrySamplingMath.MAX_ACCEPTED_SAMPLES) { sampling.stopReason="MAX_ACCEPTED_SAMPLES";limit("SAMPLING_MAX_ACCEPTED_SAMPLES");return false; }
            return true;
        }
        List<Integer> candidateOrdinals(List<Candidate> pool) {
            List<Integer> values=new ArrayList<>();for(Candidate value:pool) values.add(value.attempt);return values;
        }
        void emitPoolCandidate(Candidate candidate) throws IOException {
            Map<String,Object> record=new LinkedHashMap<>(candidate.record);record.remove("sampleAccepted");
            record.put("candidateNativeValid",true);record.put("finalSelectionStatus","PENDING_SELECTION");
            emit("POINT_NORMAL_POOL_CANDIDATE",record);
        }
        Candidate validateCandidate(Integer id,Face face,double[] requested,String phase,int attempt,Integer orientationCode,
                                    List<double[]> poolPoints,Map<String,Integer> rejectionCounts,GeometryCandidateQueue.Seed qualitySeed) throws IOException {
                UVParams uv = read("UVParams.create/set_ARGUMENT_ONLY[" + id + ":" + attempt + "]", () -> {
                    UVParams value = UVParams.create();
                    if (value != null) { value.set(0,requested[0]); value.set(1,requested[1]); }
                    return value;
                });
                if (uv == null) { reject(id,attempt,requested,"UV_ARGUMENT_UNAVAILABLE",null,rejectionCounts,phase,qualitySeed); return null; }
                Placement first = read("Surface[" + id + "].VerifyUV.requested[" + attempt + "]", () -> face.surface.VerifyUV(uv));
                if (!GeometrySamplingMath.insidePlacement(first == null ? null : first.getValue(), Placement._PLACE_INSIDE)) {
                    reject(id,attempt,requested,"REQUESTED_UV_NOT_NATIVE_INSIDE",first == null ? null : first.getValue(),rejectionCounts,phase,qualitySeed); return null;
                }
                if (!withinBudget()) { reject(id,attempt,requested,"BUDGET_BEFORE_EVALUATION",first.getValue(),rejectionCounts,phase,qualitySeed); sampling.stopReason = "SOFT_TIME_BUDGET"; return null; }
                SurfXYZData data = read("Surface[" + id + "].Eval3DData[" + attempt + "]", () -> face.surface.Eval3DData(uv));
                if (data == null) { reject(id,attempt,requested,"EVAL3D_DATA_UNAVAILABLE",first.getValue(),rejectionCounts,phase,qualitySeed); return null; }
                UVParams returned = read("SurfXYZData.GetParams[" + id + ":" + attempt + "]", data::GetParams);
                double[] actual = returned == null ? null : read("ReturnedUVParams.get[" + id + ":" + attempt + "]", () -> new double[]{returned.get(0),returned.get(1)});
                if (!GeometrySamplingMath.exactUv(requested,actual)) {
                    reject(id,attempt,requested,"RETURNED_UV_MISSING_NONFINITE_OR_DIFFERS_NO_CANONICALIZATION",null,rejectionCounts,phase,qualitySeed); return null;
                }
                Placement second = read("Surface[" + id + "].VerifyUV.returned[" + attempt + "]", () -> face.surface.VerifyUV(returned));
                if (!GeometrySamplingMath.insidePlacement(second == null ? null : second.getValue(), Placement._PLACE_INSIDE)) {
                    reject(id,attempt,requested,"RETURNED_UV_NOT_NATIVE_INSIDE",second == null ? null : second.getValue(),rejectionCounts,phase,qualitySeed); return null;
                }
                Point3D xyz = read("SurfXYZData.GetPoint[" + id + ":" + attempt + "]", data::GetPoint);
                Vector3D normal = read("SurfXYZData.GetNormal[" + id + ":" + attempt + "]", data::GetNormal);
                double[] point = xyz == null ? null : read("Point3D.get[" + id + ":" + attempt + "]", () -> new double[]{xyz.get(0),xyz.get(1),xyz.get(2)});
                double[] rawNormal = normal == null ? null : read("Vector3D.get[" + id + ":" + attempt + "]", () -> new double[]{normal.get(0),normal.get(1),normal.get(2)});
                if (!GeometrySamplingMath.finite3(point)) { reject(id,attempt,requested,"POINT_MISSING_OR_NONFINITE",null,rejectionCounts,phase,qualitySeed); return null; }
                double[] unitNormal;
                try { unitNormal = GeometrySamplingMath.normalUnit(rawNormal); }
                catch (IllegalArgumentException error) { reject(id,attempt,requested,error.getMessage(),null,rejectionCounts,phase,qualitySeed); return null; }
                boolean duplicatePoint = false;
                for (double[] prior : poolPoints) if (GeometrySamplingMath.samePoint(prior,point)) { duplicatePoint = true; break; }
                if (duplicatePoint) { reject(id,attempt,requested,"DUPLICATE_POINT_IN_FACE",null,rejectionCounts,phase,qualitySeed); return null; }
                Map<String,Object> record = map("surfaceId",id,"bodyId",face.bodyIds.size()==1?face.bodyIds.iterator().next():"UNKNOWN",
                        "observedBodyIds",new ArrayList<>(face.bodyIds),"bodyAttributionComplete",face.bodyIds.size()==1&&bodyMembershipComplete,
                        "surfaceTypeCode",face.surfaceType.getValue(),"surfaceTypeName",typeName(face.surfaceType),"attemptOrdinal",attempt,
                        "candidatePhase",phase,"candidateStrategy",qualitySeed==null?phase:qualitySeed.strategy,
                        "knownInsideSeedOrdinal",qualitySeed==null||qualitySeed.anchorOrdinal==0?"NOT_APPLICABLE":qualitySeed.anchorOrdinal,
                        "localOffsetRangeFraction",qualitySeed==null||qualitySeed.anchorOrdinal==0?"NOT_APPLICABLE":qualitySeed.offsetFraction,
                        "candidateEvaluationTimestamp",Instant.now().toString(),
                        "requestedUv",GeometrySamplingMath.list(requested),"returnedUv",GeometrySamplingMath.list(actual),
                        "uvCoordinates", "NATIVE_SURFACE_PARAMETERS_NOT_LENGTH", "requestedVerifyPlacementCode",first.getValue(),
                        "returnedVerifyPlacementCode",second.getValue(),"validityEvidence", "TWO_NATIVE_VERIFY_UV_PLACE_INSIDE_RESULTS_EXACT_RETURNED_UV",
                        "boundaryAccepted",false,"returnedUvExactMatch",true,"point",GeometrySamplingMath.list(point),
                        "pointLengthUnit",lengthUnitName,"coordinateFrame","ACTIVE_PART_MODEL_COORDINATES_AS_RETURNED_BY_SDK",
                        "normalRaw",GeometrySamplingMath.list(rawNormal),"normalUnit",GeometrySamplingMath.list(unitNormal),
                        "normalBasis","AS_RETURNED_BY_SurfXYZData.GetNormal_NO_OUTWARD_FLIP",
                        "surfaceOrientationCode",known(orientationCode),"sampleAccepted",true,
                        "identityDisposition","PENDING_FINAL_COLLECTION_SUMMARY","wallThicknessAssessed",false);
            return new Candidate(attempt,requested,point,record);
        }

        void reject(Integer id,int attempt,double[] uv,String reason,Integer placement,Map<String,Integer> counts,String phase,GeometryCandidateQueue.Seed qualitySeed) throws IOException {
            sampling.rejected++; counts.put(reason,counts.getOrDefault(reason,0)+1);
            emit("SAMPLE_REJECTED",map("surfaceId",id,"attemptOrdinal",attempt,"requestedUv",GeometrySamplingMath.list(uv),
                    "reason",reason,"verifyPlacementCode",known(placement),"sampleAccepted",false,"candidatePhase",phase,
                    "candidateStrategy",qualitySeed==null?phase:qualitySeed.strategy,
                    "knownInsideSeedOrdinal",qualitySeed==null||qualitySeed.anchorOrdinal==0?"NOT_APPLICABLE":qualitySeed.anchorOrdinal,
                    "localOffsetRangeFraction",qualitySeed==null||qualitySeed.anchorOrdinal==0?"NOT_APPLICABLE":qualitySeed.offsetFraction));
        }
        void enumerateModelFaces(Model model) throws IOException {
            ModelItems items = read("Model.ListItems(ITEM_SURFACE)", () -> model.ListItems(ModelItemType.ITEM_SURFACE));
            if (items == null) return;
            modelFaceTotal = read("ModelSurfaceItems.getarraysize", items::getarraysize);
            if (modelFaceTotal == null || modelFaceTotal < 0) { modelFaceTotal = null; issue("SURFACE_DENOMINATOR_UNKNOWN", null); return; }
            int visited = 0;
            for (int i = 0; i < modelFaceTotal; i++) {
                if (!withinBudget()) break;
                if (faces.size() >= MAX_SURFACES) { limit("MAX_SURFACES"); break; }
                final int index = i;
                ModelItem item = read("ModelSurfaceItems.get[" + i + "]", () -> items.get(index));
                visited++;
                if (!(item instanceof Surface)) { issue("SURFACE_INTERFACE_UNAVAILABLE", i); continue; }
                Integer id = read("ModelSurface.GetId[" + i + "]", item::GetId);
                if (id == null) continue;
                if (faces.containsKey(id)) { issue("DUPLICATE_MODEL_SURFACE_ID", id); continue; }
                faces.put(id, new Face((Surface)item, true));
            }
            modelEnumerationComplete = visited == modelFaceTotal;
        }
        void enumerateBodies(Model model) throws IOException {
            ModelItems items = read("Model.ListItems(ITEM_BODY)", () -> model.ListItems(ModelItemType.ITEM_BODY));
            if (items == null) { bodyMembershipComplete = false; return; }
            bodyTotal = read("BodyItems.getarraysize", items::getarraysize);
            if (bodyTotal == null || bodyTotal < 0) { bodyTotal = null; bodyMembershipComplete = false; issue("BODY_DENOMINATOR_UNKNOWN", null); return; }
            for (int i = 0; i < bodyTotal; i++) {
                if (!withinBudget()) break;
                if (i >= MAX_BODIES) { limit("MAX_BODIES"); break; }
                final int index = i;
                ModelItem item = read("BodyItems.get[" + i + "]", () -> items.get(index));
                bodiesVisited++;
                if (!(item instanceof SolidBody)) { bodyMembershipComplete = false; issue("BODY_INTERFACE_UNAVAILABLE", i); continue; }
                collectBody((SolidBody)item, true);
            }
            bodyEnumerationComplete = bodiesVisited == bodyTotal;
            if (!bodyEnumerationComplete) bodyMembershipComplete = false;
        }
        void collectBody(SolidBody body, boolean enumerated) throws IOException {
            Integer id = read("Body.GetId", body::GetId);
            Boolean flag = read("Body.IsSheetmetal", body::IsSheetmetal);
            if (enumerated && flag != null) bodiesWithState++;
            if (id == null) bodyMembershipComplete = false;
            else if (!bodyIdsSeen.add(id) && enumerated) { bodyMembershipComplete = false; issue("DUPLICATE_BODY_ID", id); }
            Surfaces list = read("Body[" + id + "].ListSurfaces", body::ListSurfaces);
            Integer total = list == null ? null : read("BodySurfaceList.getarraysize", list::getarraysize);
            int visited = 0;
            if (total == null || total < 0) { total = null; bodyMembershipComplete = false; }
            else for (int i = 0; i < total; i++) {
                if (!withinBudget()) break;
                if (bodyRefsVisited >= MAX_BODY_SURFACE_REFS) { bodyMembershipComplete = false; limit("MAX_BODY_SURFACE_REFS"); break; }
                final int index = i;
                Surface surface = read("BodySurfaceList.get[" + i + "]", () -> list.get(index));
                visited++; bodyRefsVisited++;
                if (surface == null) { bodyMembershipComplete = false; continue; }
                Integer surfaceId = read("BodySurface.GetId", surface::GetId);
                if (surfaceId == null) { bodyMembershipComplete = false; continue; }
                Face face = faces.get(surfaceId);
                if (face == null) {
                    if (faces.size() >= MAX_SURFACES) { bodyMembershipComplete = false; limit("MAX_SURFACES"); break; }
                    face = new Face(surface, false); faces.put(surfaceId, face);
                }
                if (id != null) face.bodyIds.add(id);
            }
            if (total == null || visited != total) bodyMembershipComplete = false;
            emit("BODY", map("scope", enumerated ? "MODEL_BODY_ENUM" : "DEFAULT_BODY_FALLBACK", "bodyId", known(id),
                    "isDefaultBody", id == null || defaultBodyId == null ? "UNKNOWN" : id.equals(defaultBodyId),
                    "nativeIsSheetmetal", known(flag), "nativeStateSource", "SolidBody.IsSheetmetal",
                    "surfaceReferenceCoverage", coverage(total, visited), "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE"));
        }
        @Override public void close() throws IOException { writer.close(); }
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value; }
    private static boolean differs(Object a, Object b) { return a != null && b != null && !a.equals(b); }
    private static String typeName(SurfaceType kind) {
        if (kind == null) return "UNKNOWN";
        int value = kind.getValue();
        if (value == SurfaceType._SURFACE_PLANE) return "PLANE";
        if (value == SurfaceType._SURFACE_CYLINDER) return "CYLINDER";
        if (value == SurfaceType._SURFACE_CONE) return "CONE";
        if (value == SurfaceType._SURFACE_TORUS) return "TORUS";
        if (value == SurfaceType._SURFACE_RULED) return "RULED";
        if (value == SurfaceType._SURFACE_REVOLVED) return "REVOLVED";
        if (value == SurfaceType._SURFACE_TABULATED_CYLINDER) return "TABULATED_CYLINDER";
        if (value == SurfaceType._SURFACE_FILLET) return "FILLET";
        if (value == SurfaceType._SURFACE_COONS_PATCH) return "COONS_PATCH";
        if (value == SurfaceType._SURFACE_SPLINE) return "SPLINE";
        if (value == SurfaceType._SURFACE_NURBS) return "NURBS";
        if (value == SurfaceType._SURFACE_CYLINDRICAL_SPLINE) return "CYLINDRICAL_SPLINE";
        if (value == SurfaceType._SURFACE_SPHERICAL_SPLINE) return "SPHERICAL_SPLINE";
        if (value == SurfaceType._SURFACE_FOREIGN) return "FOREIGN";
        if (value == SurfaceType._SURFACE_SPL2DER) return "SPL2DER";
        return "OTHER_SDK_TYPE_CODE_" + value;
    }
}
