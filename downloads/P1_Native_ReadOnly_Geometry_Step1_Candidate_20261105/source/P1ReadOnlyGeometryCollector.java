package p1.readonly.geometry;

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
import static p1.readonly.geometry.GeometryEvidenceSupport.*;

/** Independent synchronous read-only collector. Model APIs are only called on explicit OnCommand. */
public final class P1ReadOnlyGeometryCollector {
    public static final String COMMAND_ID = "SISWReadOnlyGeometryCollect";
    private static final String MESSAGES = "p1_readonly_geometry.txt";
    private static final int MAX_BODIES = 128, MAX_SURFACES = 10000, MAX_BODY_SURFACE_REFS = 40000;
    private static final long SOFT_BUDGET_NS = 30_000_000_000L;
    private static Session session;
    private static UICommand command;
    private static Listener listener;
    private static volatile boolean active;
    private static final AtomicBoolean collecting = new AtomicBoolean();
    private P1ReadOnlyGeometryCollector() { }

    public static synchronized void start() throws Exception {
        if (active) return;
        if (command != null || listener != null) throw new IllegalStateException("Restart Creo: prior registration may still be attached");
        session = pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
        if (session == null) throw new IllegalStateException("No synchronous session");
        listener = new Listener();
        try {
            command = session.UICreateCommand(COMMAND_ID, listener);
            if (command == null) throw new IllegalStateException("UICreateCommand returned null");
            command.Designate(MESSAGES, "P1_GEO_LABEL", "P1_GEO_HELP", "P1_GEO_DESC");
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
        Face(Surface value, boolean listed) { surface = value; modelListed = listed; }
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
        Run(String id) throws IOException {
            requestId = id;
            String configured = System.getenv("P1_READONLY_GEOMETRY_LOG_DIR");
            Path directory = configured == null || configured.isBlank()
                    ? Paths.get(System.getProperty("user.home"), "CreoReadOnlyGeometry") : Paths.get(configured);
            Files.createDirectories(directory);
            Path output = directory.resolve("geometry-" + requestId + ".jsonl");
            writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            lifecycle("EVIDENCE_FILE=" + output + "; requestId=" + requestId, null);
        }
        void emit(String event, Map<String,Object> fields) throws IOException {
            Map<String,Object> line = map("schema", "P1_NATIVE_READONLY_GEOMETRY_V1", "timestamp", Instant.now().toString(),
                    "requestId", requestId, "commandId", COMMAND_ID, "event", event);
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
            return map("fileName", file, "revision", revision, "isModified", dirty);
        }
        void collect() throws IOException {
            emit("COLLECTION_STARTED", map("modelApiPolicy", "READ_ONLY", "geometrySampling", "NOT_IN_STEP_1",
                    "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE", "uniformThicknessAssessment", "INSUFFICIENT_EVIDENCE",
                    "limits", map("bodies", MAX_BODIES, "uniqueSurfaces", MAX_SURFACES, "bodySurfaceRefs", MAX_BODY_SURFACE_REFS, "softBudgetSeconds", 30)));
            read("Session.UIDisplayMessage(STARTED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_GEO_STARTED", null); return Boolean.TRUE; });
            Map<String,Object> before = null, after = null;
            Model model = null;
            boolean eligible = false, fatal = false, identityStable = false, identityChanged = false;
            try {
                model = read("Session.GetActiveModel.before", session::GetActiveModel);
                if (model == null) { issue("ACTIVE_MODEL_UNAVAILABLE_OR_NULL", null); return; }
                ModelType type = read("Model.GetType", model::GetType);
                if (type != ModelType.MDL_PART || !(model instanceof Solid)) { issue("PART_TYPE_OR_SOLID_INTERFACE_NOT_CONFIRMED", null); return; }
                eligible = true;
                before = identity(model, "before"); emit("MODEL_BEFORE", before);
                Solid solid = (Solid)model; // Public interface guard above; no WSolid cast or internal reflection.
                UnitSystem system = read("Solid.GetPrincipalUnits", solid::GetPrincipalUnits);
                Unit length = system == null ? null : read("UnitSystem.GetUnit(UNIT_LENGTH)", () -> system.GetUnit(UnitType.UNIT_LENGTH));
                String unitName = length == null ? null : clean(read("Unit.GetName", length::GetName));
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
                    Double area = read("Surface[" + id + "].EvalArea", face.surface::EvalArea);
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
                            "sampling", "NOT_REQUESTED_STEP_1", "unsampledReason", "POINT_NORMAL_DISTANCE_COLLECTION_DEFERRED"));
                }
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
                        identityStable = Boolean.TRUE.equals(handleMatch) && stable(before.get("fileName"), after.get("fileName"))
                                && stable(before.get("revision"), after.get("revision")) && stable(before.get("revision"), currentIdentity.get("revision"))
                                && stable(before.get("fileName"), currentIdentity.get("fileName"));
                        identityChanged = Boolean.FALSE.equals(handleMatch)
                                || differs(before.get("fileName"), after.get("fileName")) || differs(before.get("revision"), after.get("revision"))
                                || differs(before.get("isModified"), after.get("isModified"))
                                || differs(before.get("fileName"), currentIdentity.get("fileName"))
                                || differs(before.get("revision"), currentIdentity.get("revision"))
                                || differs(before.get("isModified"), currentIdentity.get("isModified"));
                    }
                }
                read("Session.UIDisplayMessage(FINISHED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_GEO_FINISHED", null); return Boolean.TRUE; });
                boolean requiredReadsComplete = !truncated && identityStable && errors == 0 && bodyEnumerationComplete
                        && modelEnumerationComplete && bodyMembershipComplete && metadataComplete && defaultStateKnown
                        && modelFaceTotal != null && completeModelFaceRecords == modelFaceTotal
                        && bodyTotal != null && bodiesWithState == bodyTotal;
                String outcome = completion(eligible, identityChanged, fatal, requiredReadsComplete);
                emit("COLLECTION_SUMMARY", map("outcome", outcome, "errors", errors, "unknownReadValues", unknownReads,
                        "identityStableWithinObservedFields", identityStable, "identityOrDirtyChangeObserved", identityChanged,
                        "metadataComplete", metadataComplete, "defaultBodyStateKnown", defaultStateKnown,
                        "allBodyEnumerationComplete", bodyEnumerationComplete, "bodyMembershipEnumerationComplete", bodyMembershipComplete,
                        "modelSurfaceEnumerationComplete", modelEnumerationComplete,
                        "bodyStateCoverage", coverage(bodyTotal, bodiesWithState),
                        "modelSurfaceRecordCoverage", coverage(modelFaceTotal, modelFaceRecords),
                        "modelSurfaceRequiredFieldCoverage", coverage(modelFaceTotal, completeModelFaceRecords),
                        "uniqueFacesRetained", faces.size(), "surfaceRecordsEmitted", faceRecordsEmitted, "bodyOnlySurfaceRecords", bodyOnlyFaceRecords,
                        "modelSurfaceRecordsNotProduced", modelFaceTotal == null ? "UNKNOWN" : modelFaceTotal - modelFaceRecords,
                        "modelSurfaceRecordsWithIncompleteFields", modelFaceRecords - completeModelFaceRecords,
                        "bodySurfaceReferencesVisited", bodyRefsVisited,
                        "truncated", truncated, "truncationReason", truncationReason, "elapsedMs", (System.nanoTime() - started) / 1_000_000,
                        "nativeBodyStateDoesNotEstablishThinWallShape", true,
                        "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE", "uniformThicknessAssessment", "INSUFFICIENT_EVIDENCE",
                        "samplingCoverage", "NOT_REQUESTED_STEP_1", "modelWriteAbsenceIndependentlyProven", false));
            }
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
