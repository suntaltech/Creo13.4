package p1.readonly.geometry.step10;

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
import com.ptc.pfc.pfcBase.Transform3D;
import static p1.readonly.geometry.step10.GeometryEvidenceSupport.*;

/** Independent synchronous read-only collector. Model APIs are only called on explicit OnCommand. */
public final class P1ReadOnlyGeometrySamplerStep10 {
    private static final long JAVA_INIT_OBSERVED_BEGIN_NS=System.nanoTime();
    private static final String JAVA_INIT_OBSERVED_BEGIN_UTC=Instant.now().toString();
    public static final String COMMAND_ID = "SISWReadOnlyGeometrySample10";
    private static final String MESSAGES = "p1_readonly_sampling_step10.txt";
    private static final int MAX_BODIES = 128, MAX_SURFACES = 10000, MAX_BODY_SURFACE_REFS = 40000;
    private static final long SOFT_BUDGET_NS = 30_000_000_000L;
    private static Session session;
    private static UICommand command;
    private static Listener listener;
    private static volatile boolean active;
    private static final AtomicBoolean collecting = new AtomicBoolean();
    private static GeometryTimingJournal timingJournal;
    static {
        GeometryTimingJournal sink=journal();long end=System.nanoTime();
        sink.record("JAVA_STATIC_INITIALIZATION_OBSERVED",null,map("beginUtc",JAVA_INIT_OBSERVED_BEGIN_UTC,"endUtc",Instant.now().toString(),
                "beginMonotonicNs",JAVA_INIT_OBSERVED_BEGIN_NS,"endMonotonicNs",end,"observedElapsedNs",end-JAVA_INIT_OBSERVED_BEGIN_NS,
                "scope","OBSERVABLE_JAVA_STATIC_INITIALIZATION_NOT_JVM_DEFINITION_VERIFICATION_OR_LINKING_TOTAL","includesTimingJournalSetup",true,
                "firstRecordSerializationAndWriteExcluded",true,"classLoadBeforeJavaEntryKnown",false,"journalState",sink.state()));
    }
    private static synchronized GeometryTimingJournal journal(){if(timingJournal==null)timingJournal=new GeometryTimingJournal(COMMAND_ID);return timingJournal;}
    private static <T> T registrationAction(String operation,Read<T> action) throws Exception {
        long begin=System.nanoTime();String utc=Instant.now().toString(),outcome="THREW";
        try{T value=action.get();outcome=value==null?"RETURNED_NULL":"RETURNED";return value;}
        finally{long end=System.nanoTime();journal().record("REGISTRATION_ACTION_SPAN",null,map("operation",operation,"beginUtc",utc,"endUtc",Instant.now().toString(),
                "beginMonotonicNs",begin,"endMonotonicNs",end,"elapsedNs",end-begin,"outcome",outcome,"includesActionJavaAndSdkWork",true,"countingScope","ONE_WRAPPED_REGISTRATION_ACTION_NOT_INTERNAL_JNI_FANOUT","actionGroups",1));}
    }
    private P1ReadOnlyGeometrySamplerStep10() { }

    public static synchronized void start() throws Exception {
        long begin=System.nanoTime();String utc=Instant.now().toString(),outcome="THREW";
        journal().record("PLUGIN_START_ENTER",null,map("enteredUtc",utc,"enteredMonotonicNs",begin));
        try {
            if (active){outcome="ALREADY_ACTIVE_NO_REGISTRATION";return;}
            if (command != null || listener != null) throw new IllegalStateException("Restart Creo: prior registration may still be attached");
            session=registrationAction("GetCurrentSessionWithCompatibility",()->pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible));
            if(session==null)throw new IllegalStateException("No synchronous session");
            listener=registrationAction("ListenerJavaConstruction",Listener::new);
            try {
                command=registrationAction("Session.UICreateCommand",()->session.UICreateCommand(COMMAND_ID,listener));
                if(command==null)throw new IllegalStateException("UICreateCommand returned null");
                registrationAction("UICommand.Designate",()->{command.Designate(MESSAGES,"P1_SAMPLE10_LABEL","P1_SAMPLE10_HELP","P1_SAMPLE10_DESC");return Boolean.TRUE;});
                active=true;lifecycle("REGISTERED; command="+COMMAND_ID+"; collection=NOT_EXECUTED",null);outcome="REGISTERED";
            }catch(Exception|LinkageError error){
                lifecycle("REGISTRATION_FAILED",error);
                if(command!=null){try{registrationAction("UICommand.RemoveActionListener.registrationCleanup",()->{command.RemoveActionListener(listener);return Boolean.TRUE;});command=null;listener=null;}
                    catch(Exception|LinkageError detachError){lifecycle("DETACH_FAILED; restart_required",detachError);}}
                else listener=null;
                session=null;throw error;
            }
        }finally{long end=System.nanoTime();journal().record("PLUGIN_START_EXIT",null,map("beginUtc",utc,"endUtc",Instant.now().toString(),"beginMonotonicNs",begin,"endMonotonicNs",end,"elapsedNs",end-begin,"outcome",outcome,"ownExitRecordWriteExcluded",true));}
    }
    public static synchronized void stop() throws Exception {
        long begin=System.nanoTime();boolean completed=false;
        journal().record("PLUGIN_STOP_ENTER",null,map("enteredMonotonicNs",begin));
        try{
            active=false;
            if(collecting.get())throw new IllegalStateException("Collection in progress; do not unload");
            if(command!=null&&listener!=null)registrationAction("UICommand.RemoveActionListener.stop",()->{command.RemoveActionListener(listener);return Boolean.TRUE;});
            command=null;listener=null;session=null;lifecycle("STOP_RETURNED",null);completed=true;
        }finally{GeometryTimingJournal sink=journal();sink.record("PLUGIN_STOP_EXIT",null,map("elapsedNs",System.nanoTime()-begin,"completed",completed,"journalStateBeforeClose",sink.state()));if(completed){sink.close();timingJournal=null;}}
    }
    private static void lifecycle(String message,Throwable error){
        System.err.println("[P1GEO] "+Instant.now()+" "+message);if(error!=null)error.printStackTrace(System.err);
        journal().record("LIFECYCLE_DIAGNOSTIC",null,map("message",message,"exceptionClass",error==null?"NONE":error.getClass().getName(),"stackRemainsInOriginalStderr",error!=null));
    }
    private static final class Listener extends DefaultUICommandActionListener {
        @Override public void OnCommand() {
            long entered=System.nanoTime();String utc=Instant.now().toString();GeometryTimingJournal sink=journal();String requestId=null;String outcome="NOT_ENTERED";
            sink.record("COMMAND_ENTER",null,map("enteredUtc",utc,"enteredMonotonicNs",entered));
            if(!active||session==null||!collecting.compareAndSet(false,true)){
                lifecycle("COLLECTION_NOT_ENTERED; inactive_or_busy",null);sink.record("COMMAND_REJECTED",null,map("elapsedNs",System.nanoTime()-entered,"reason","INACTIVE_OR_BUSY"));return;
            }
            try{
                long uuidBegin=System.nanoTime();requestId=UUID.randomUUID().toString();sink.record("REQUEST_ID_GENERATED",requestId,map("generationNs",System.nanoTime()-uuidBegin));
                try(Run run=new Run(requestId,entered,utc)){run.collect();outcome="COLLECT_AND_LOG_CLOSE_RETURNED";}
            }catch(Exception|LinkageError error){outcome="COLLECTION_OR_LOG_IO_FAILED";lifecycle("COLLECTION_OR_LOG_IO_FAILED; requestId="+requestId,error);}
            finally{collecting.set(false);long end=System.nanoTime();sink.record("COMMAND_EXIT",requestId,map("enteredUtc",utc,"endUtc",Instant.now().toString(),"beginMonotonicNs",entered,"endMonotonicNs",end,"elapsedNs",end-entered,
                    "outcome",outcome,"ownExitRecordWriteAndSubsequentUiRepaintExcluded",true,"noBackgroundNativeThreadIntroduced",true,"journalState",sink.state()));}
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
    private static final class AnalyticData {
        final String kind;double[] origin,x,y,z;Double radius;boolean valid;
        AnalyticData(String kind){this.kind=kind;}
    }
    private static final class DomainPoint {
        final double[] point,normal;final int ordinal;
        DomainPoint(double[] point,double[] normal,int ordinal){this.point=point;this.normal=normal;this.ordinal=ordinal;}
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
        final GeometryTimingCounters timing=new GeometryTimingCounters();
        final GeometryStderrPolicy stderrPolicy=GeometryStderrPolicy.fromEnvironment();
        final GeometryTimingJournal timingSink=journal();
        final long commandEnteredNs;final String commandEnteredUtc;
        String timingPhase="RUN_CONSTRUCTION";boolean timingPhaseActive;
        long timingPhaseBeginNs;String timingPhaseBeginUtc;
        Map<String,Long> timingPhaseMarks;
        final List<Map<String,Object>> timingPhaseReports=new ArrayList<>();
        int errors, unknownReads, bodiesVisited, bodiesWithState, modelFaceRecords, completeModelFaceRecords, bodyRefsVisited, faceRecordsEmitted, bodyOnlyFaceRecords;
        Integer bodyTotal, modelFaceTotal;
        boolean metadataComplete, defaultStateKnown, truncated, bodyMembershipComplete = true, bodyEnumerationComplete, modelEnumerationComplete;
        String truncationReason = "NONE";
        final Map<Integer,Face> faces = new LinkedHashMap<>();
        final Set<Integer> bodyIdsSeen = new HashSet<>();
        Integer defaultBodyId;
        String lengthUnitName;
        String modelFileName,modelVersionStamp;
        final Map<Integer,List<Candidate>> finalFaceSamples=new LinkedHashMap<>();
        boolean pairEvidenceTruncated,pairEvidenceStarted,pairEvidenceFinished;
        boolean analyticPhaseStarted,analyticPhaseFinished,distancePhaseStarted,distancePhaseFinished,distancePhaseTruncated;
        int analyticInvalid,domainEvaluations,domainAccepted,domainRejected;
        boolean classificationFinished,classificationObserverOverflow,interfaceReviewFinished;
        int classificationInvalidPairs;
        final Map<String,Map<String,Object>> classificationGaps=new LinkedHashMap<>(),classificationPairSummaries=new LinkedHashMap<>();
        final Map<String,GeometryCandidateClassification.Stats> classificationStats=new LinkedHashMap<>();
        final Map<Integer,AnalyticData> analyticData=new LinkedHashMap<>();
        final Map<String,List<Map<String,Object>>> distancePairs=new LinkedHashMap<>();
        int modelTypesRead, validModelUvBounds;
        final SamplingCounters sampling = new SamplingCounters();
        final Map<Integer,Map<String,Object>> targetedResults = new LinkedHashMap<>();
        Run(String id,long commandEntered,String enteredUtc) throws IOException {
            requestId=id;commandEnteredNs=commandEntered;commandEnteredUtc=enteredUtc;
            long begin=System.nanoTime();String utc=Instant.now().toString();boolean opened=false;
            timingSink.record("RUN_LOG_IO_BEGIN",id,map("beginUtc",utc,"beginMonotonicNs",begin,"commandEntryMonotonicNs",commandEntered));
            try{
                String configured=System.getenv("P1_READONLY_GEOMETRY_LOG_DIR");Path directory=configured==null||configured.isBlank()?Paths.get(System.getProperty("user.home"),"CreoReadOnlyGeometry"):Paths.get(configured);
                Files.createDirectories(directory);Path output=directory.resolve("geometry-sampling-step10-"+requestId+".jsonl");
                writer=Files.newBufferedWriter(output,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);opened=true;
                timingSink.record("LOG_OUTPUT_POLICY_SELECTED",id,map("policy",stderrPolicy.report(),"scope","BUSINESS_STDERR_MIRROR_ONLY_ALL_JSONL_ROWS_AND_FLUSH_RETAINED"));
                if(stderrPolicy.fallbackFull())lifecycle("LOG_STDERR_MODE_INVALID_OR_UNAVAILABLE; fallback=FULL",null);
                lifecycle("EVIDENCE_FILE="+output+"; requestId="+requestId,null);
            }finally{long end=System.nanoTime();timingSink.record("RUN_LOG_IO_END",id,map("beginUtc",utc,"endUtc",Instant.now().toString(),"beginMonotonicNs",begin,"endMonotonicNs",end,"elapsedNs",end-begin,"opened",opened,
                    "runStartedMonotonicNs",started,"commandEntryToIoEndNs",end-commandEntered,"includesEvidenceDirectoryAndFileSetupAndDiagnosticLogging",true));}
        }
        void emit(String event,Map<String,Object> fields) throws IOException {
            long outputBegin=System.nanoTime(),headerEnd=outputBegin,writeElapsed=0,flushElapsed=0,stderrElapsed=0,observerElapsed=0,writes=0,newlines=0,flushes=0,stderrLines=0;
            String utc=Instant.now().toString();boolean completed=false,headerCompleted=false;
            try{
                Map<String,Object> line=map("schema","P1_NATIVE_READONLY_GEOMETRY_V1","timestamp",utc,"requestId",requestId,"commandId",COMMAND_ID,"event",event,
                    "collectorStep","STEP_10","samplingSchema","P1_NATIVE_GEOMETRY_SAMPLING_V1","samplingQualitySchema","P1_NATIVE_GEOMETRY_SAMPLING_QUALITY_V1",
                    "selectionSchema","P1_NATIVE_PLANE_POINT_POOL_V1","queueSchema","P1_NATIVE_MIXED_UV_QUEUE_V1","spreadSchema","P1_NATIVE_SPREAD_FROZEN_BASELINE_V1","pairSchema","P1_NATIVE_RELATIVE_FACE_CANDIDATES_V1",
                    "distanceSchema","P1_NATIVE_ANALYTIC_LOCAL_DISTANCE_V1","classificationSchema","P1_NATIVE_LOCAL_DISTANCE_CLASSIFICATION_V1","interfaceReviewSchema","P1_NATIVE_READONLY_API_REVIEW_V1",
                    "timingSchema","P1_NATIVE_TIMING_OBSERVATION_V1","timingPhase",timingPhase,
                    "loggingSchema","P1_NATIVE_GEOMETRY_STDERR_POLICY_V1","stderrMode",stderrPolicy.mode());
                line.putAll(fields);String json=json(line);headerEnd=System.nanoTime();headerCompleted=true;long mark=System.nanoTime();
                try{writes++;writer.write(json);newlines++;writer.newLine();}finally{writeElapsed=System.nanoTime()-mark;}
                mark=System.nanoTime();try{flushes++;writer.flush();}finally{flushElapsed=System.nanoTime()-mark;}
                if(stderrPolicy.decide(event)){mark=System.nanoTime();try{stderrLines++;System.err.println("[P1GEO] "+json);}finally{stderrElapsed=System.nanoTime()-mark;}}
                mark=System.nanoTime();try{observeClassificationInput(event,fields);}finally{observerElapsed=System.nanoTime()-mark;}
                completed=true;
            }finally{long end=System.nanoTime();timing.output(event,utc,Instant.now().toString(),(headerCompleted?headerEnd:end)-outputBegin,writes,newlines,flushes,stderrLines,writeElapsed,flushElapsed,stderrElapsed,observerElapsed,end-outputBegin,completed);}
        }
        <T> T read(String operation,Read<T> action) throws IOException {
            T value;
            try{
                long begin=System.nanoTime();String result="THREW";
                try{value=action.get();result=value==null?"RETURNED_NULL":"RETURNED";}
                finally{timing.read(timingPhase,operation,System.nanoTime()-begin,result,Thread.currentThread().getId());}
                if(value==null){unknownReads++;emit("READ_UNAVAILABLE",map("operation",operation,"outcome","RETURNED_NULL"));}return value;
            }catch(Exception|LinkageError error){errors++;StringWriter stack=new StringWriter();error.printStackTrace(new PrintWriter(stack));emit("READ_EXCEPTION",map("operation",operation,"exceptionClass",error.getClass().getName(),"stack",stack.toString()));return null;}
        }
        void beginTimingPhase(String name){
            endTimingPhase("BODY_RETURNED");timingPhase=name;timingPhaseActive=true;timingPhaseBeginNs=System.nanoTime();timingPhaseBeginUtc=Instant.now().toString();timingPhaseMarks=timing.marks();
            timingSink.record("PHASE_BEGIN",requestId,map("phase",name,"beginUtc",timingPhaseBeginUtc,"beginMonotonicNs",timingPhaseBeginNs,"includesThisBeginJournalWrite",true));
        }
        void endTimingPhase(String outcome){
            if(!timingPhaseActive)return;long end=System.nanoTime();Map<String,Object> report=map("phase",timingPhase,"beginUtc",timingPhaseBeginUtc,"endUtc",Instant.now().toString(),
                "beginMonotonicNs",timingPhaseBeginNs,"endMonotonicNs",end,"elapsedNs",end-timingPhaseBeginNs,"outcome",outcome,"businessCounterDelta",timing.delta(timingPhaseMarks),
                "endJournalWriteExcluded",true,"notExclusiveCpuOrPureJniTime",true);
            if(timingPhaseReports.size()<32)timingPhaseReports.add(report);timingPhaseActive=false;timingPhase="BETWEEN_PHASES";timingSink.record("PHASE_END",requestId,report);
        }
        void emitTimingRecord(String event,Map<String,Object> values){try{emit(event,values);}catch(IOException|RuntimeException error){timingSink.record("BUSINESS_TIMING_RECORD_FAILED",requestId,map("event",event,"exceptionClass",error.getClass().getName()));}}
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
            beginTimingPhase("COLLECTION_SETUP_AND_STARTED_MESSAGE");
            emit("COLLECTION_STARTED", map("modelApiPolicy", "READ_ONLY", "geometrySampling", "ADDITIVE_STEP_2_EVENTS",
                    "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE", "uniformThicknessAssessment", "INSUFFICIENT_EVIDENCE",
                    "limits", map("bodies", MAX_BODIES, "uniqueSurfaces", MAX_SURFACES, "bodySurfaceRefs", MAX_BODY_SURFACE_REFS, "softBudgetSeconds", 30)));
            emitTimingRecord("TIMING_COMMAND_CONTEXT",map("commandEnteredUtc",commandEnteredUtc,"commandEnteredMonotonicNs",commandEnteredNs,"runStartedMonotonicNs",started,
                    "timingTraceId",timingSink.traceId(),"lifecycleFileName",timingSink.fileName(),"javaThreadId",Thread.currentThread().getId(),"javaThreadName",Thread.currentThread().getName(),"classLoadBeforeJavaEntryKnown",false));
            read("Session.UIDisplayMessage(STARTED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_SAMPLE10_STARTED", null); return Boolean.TRUE; });
            Map<String,Object> before = null, after = null;
            Model model = null;
            boolean eligible = false, fatal = false, identityStable = false, identityChanged = false, stampContextMatches = false;
            try {
                beginTimingPhase("ACTIVE_MODEL_IDENTITY_UNITS_ACCURACY");
                model = read("Session.GetActiveModel.before", session::GetActiveModel);
                if (model == null) { issue("ACTIVE_MODEL_UNAVAILABLE_OR_NULL", null); return; }
                ModelType type = read("Model.GetType", model::GetType);
                if (type != ModelType.MDL_PART || !(model instanceof Solid)) { issue("PART_TYPE_OR_SOLID_INTERFACE_NOT_CONFIRMED", null); return; }
                eligible = true;
                before = identity(model, "before"); modelFileName = (String)before.get("fileName");modelVersionStamp=(String)before.get("versionStamp"); emit("MODEL_BEFORE", before);
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
                beginTimingPhase("DEFAULT_BODY_STATE");
                SolidBody defaultBody = read("Solid.GetDefaultBody", solid::GetDefaultBody);
                if (defaultBody != null) {
                    defaultBodyId = read("DefaultBody.GetId", defaultBody::GetId);
                    Boolean flag = read("DefaultBody.IsSheetmetal", defaultBody::IsSheetmetal);
                    defaultStateKnown = defaultBodyId != null && flag != null;
                    emit("DEFAULT_BODY", map("bodyId", known(defaultBodyId), "nativeIsSheetmetal", known(flag),
                            "nativeStateSource", "SolidBody.IsSheetmetal", "thinWallShapeAssessment", "INSUFFICIENT_EVIDENCE"));
                } else emit("DEFAULT_BODY", map("bodyId", "UNKNOWN", "nativeIsSheetmetal", "UNKNOWN"));
                beginTimingPhase("MODEL_SURFACE_ENUMERATION");
                enumerateModelFaces(model);
                beginTimingPhase("BODY_ENUMERATION_AND_FALLBACK");
                enumerateBodies(model);
                if (defaultBody != null && (defaultBodyId == null || !bodyIdsSeen.contains(defaultBodyId)) && withinBudget()) {
                    issue("DEFAULT_NOT_CONFIRMED_IN_BODY_ENUM", defaultBodyId);
                    emit("DEFAULT_BODY_FALLBACK_SCOPE", map("reason", "DEFAULT_NOT_CONFIRMED_IN_BODY_ENUM", "allBodiesCoveragePromoted", false));
                    collectBody(defaultBody, false);
                }
                beginTimingPhase("SURFACE_TYPE_AREA_AND_UV_METADATA");
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
                beginTimingPhase("POINT_NORMAL_SAMPLING_AND_SPREAD");
                sampleTargets();
                beginTimingPhase("COARSE_PAIR_ENUMERATION");
                collectPairEvidence();
                beginTimingPhase("ANALYTIC_DESCRIPTOR_READS");
                collectAnalyticParameters();
                beginTimingPhase("DOMAIN_VALIDATION_AND_LOCAL_DISTANCES");
                collectLocalDistances();
                beginTimingPhase("CANDIDATE_CLASSIFICATION");
                collectCandidateClassifications();
                beginTimingPhase("STATIC_SDK_REVIEW_OUTPUT");
                emitSdkInterfaceReview();
                endTimingPhase("BODY_RETURNED");
            } catch (Exception | LinkageError error) {
                endTimingPhase("THREW_OR_ABORTED");
                fatal = true; errors++;
                StringWriter stack = new StringWriter(); error.printStackTrace(new PrintWriter(stack));
                emit("COLLECTION_EXCEPTION", map("exceptionClass", error.getClass().getName(), "stack", stack.toString()));
            } finally {
                endTimingPhase("EARLY_TRY_EXIT");
                beginTimingPhase("FINAL_MODEL_IDENTITY");
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
                beginTimingPhase("FINAL_SUMMARIES_AND_FINISHED_MESSAGE");
                sampling.finishIfMissing(this);
                emit("STEP2_IDENTITY_CONTEXT", map("versionStampContextMatches", stampContextMatches,
                        "versionStampIsRevision", false, "legacyRevisionStrictIdentityStable", identityStable,
                        "nativeRevisionSubstituted", false, "modelWriteAbsenceIndependentlyProven", false));
                read("Session.UIDisplayMessage(FINISHED)", () -> { session.UIDisplayMessage(MESSAGES, "P1_SAMPLE10_FINISHED", null); return Boolean.TRUE; });
                endTimingPhase("BODY_RETURNED");
                emitTimingRecord("TIMING_OBSERVATION_SNAPSHOT",map("timingTraceId",timingSink.traceId(),"lifecycleFileName",timingSink.fileName(),"phaseReports",timingPhaseReports,"counters",timing.report(),"stderrPolicyBeforeSnapshotOutput",stderrPolicy.report(),
                        "snapshotExcludesThisRecordFinalCollectionSummaryAndClose",true,"fullBusinessOutputTotalsFollowInLifecycleJournalAfterClose",true,"journalStateBeforeThisRecord",timingSink.state(),"nativeCallsMovedToBackground",false));
                beginTimingPhase("FINAL_COLLECTION_SUMMARY_OUTPUT");
                boolean requiredReadsComplete = !truncated && identityStable && errors == 0 && bodyEnumerationComplete
                        && modelEnumerationComplete && bodyMembershipComplete && metadataComplete && defaultStateKnown
                        && modelFaceTotal != null && completeModelFaceRecords == modelFaceTotal
                        && bodyTotal != null && bodiesWithState == bodyTotal && sampling.quotaComplete()
                        && modelFaceTotal!=null && validModelUvBounds==modelFaceTotal && sampling.qualityUnimprovedFaces==0 && pairEvidenceFinished && !pairEvidenceTruncated && analyticPhaseFinished && analyticInvalid==0 && distancePhaseFinished && !distancePhaseTruncated && unknownReads==0 && classificationFinished && classificationInvalidPairs==0 && !classificationObserverOverflow && interfaceReviewFinished;
                String outcome = completion(eligible, identityChanged, fatal, requiredReadsComplete);
                emit("COLLECTION_SUMMARY", map("outcome", outcome,"timingTraceId",timingSink.traceId(),"timingObservationScope","OBSERVED_ACTION_GROUPS_AND_SYNCHRONOUS_IO_NOT_FULL_JVM_LOAD_OR_UI_RESPONSIVENESS_PROOF","timingJournalWriteErrorsBeforeSummary",timingSink.state().get("journalWriteErrors"), "errors", errors, "unknownReadValues", unknownReads,
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
                        "relativeFaceEvidenceState",pairEvidenceFinished?"BOUNDED_SCAN_FINISHED_CANDIDATES_ONLY":pairEvidenceStarted?"ABORTED_UNKNOWN":"NOT_ENTERED_UNKNOWN",
                        "analyticParametersFinished",analyticPhaseFinished,"analyticParametersUnknownOrInconsistent",analyticInvalid,
                        "localDistanceEvidenceState",distancePhaseFinished?"BOUNDED_POINT_WITNESSES_ONLY":distancePhaseStarted?"PARTIAL_OR_ABORTED_UNKNOWN":"NOT_ENTERED_UNKNOWN",
                        "classificationState",classificationFinished?"MULTI_LABEL_REVIEW_ONLY":"NOT_COMPLETED_UNKNOWN","classificationInvalidPairs",classificationInvalidPairs,
                        "classificationObserverOverflow",classificationObserverOverflow,"interfaceReviewState",interfaceReviewFinished?"STATIC_SUPPLIED_SDK_SIGNATURE_REVIEW_ONLY":"NOT_COMPLETED_UNKNOWN",
                        "materialOccupancyVerified",false,"occlusionVerified",false,"globalWallThickness","NOT_ASSESSED",
                        "domainEvaluationChains",domainEvaluations,"domainAccepted",domainAccepted,"domainRejected",domainRejected,
                        "localDistanceEvidenceTruncated",distancePhaseTruncated,"finalWallThickness","NOT_ASSESSED","entireTrimmedOverlapRegionProven",false,
                        "relativeFaceEvidenceTruncated",pairEvidenceTruncated,"relativeFaceEvidenceIsConfirmedPairOrThickness",false,
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
                    "maxQualityPoolSize",GeometrySpreadSelection.MAX_SPREAD_POOL_SIZE,"frozenStep5PrefixPoolSize",GeometrySamplingMath.MAX_QUALITY_POOL_SIZE,"maxQualityProbesPerFace",GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE,
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
            List<Integer> frozenStep5Indices=null;
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
                        "maxPoolSize",GeometrySpreadSelection.MAX_SPREAD_POOL_SIZE,"step5FreezePoolSize",GeometrySamplingMath.MAX_QUALITY_POOL_SIZE,"maxAdditionalProbes",GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE));
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
                    if(pool.size()>=GeometrySpreadSelection.MAX_SPREAD_POOL_SIZE) { qualityStopReason="QUALITY_POOL_FULL";break; }
                    if(qualityAttempts>=GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE) { qualityStopReason="QUALITY_PROBE_LIMIT";break; }
                    if(!canProbe()) { qualityStopReason=sampling.stopReason;stopReason=sampling.stopReason;break; }
                    sampling.attempts++;qualityAttempts++;int attempt=sampling.attempts-startAttempts;
                    int[] stats=strategyStats.get(seed.strategy);stats[1]++;
                    attemptedUvs.add(uv);
                    Candidate valid=validateCandidate(id,face,uv,"QUALITY_POOL",attempt,orientationCode,poolPoints,rejectionCounts,seed);
                    if(valid!=null) {
                        stats[2]++;pool.add(valid);poolPoints.add(valid.point);emitPoolCandidate(valid);
                        if(pool.size()==GeometrySamplingMath.MAX_QUALITY_POOL_SIZE) {
                            Map<String,Object> baseline=GeometryPoolSelection.select(poolPoints);
                            frozenStep5Indices=new ArrayList<>();for(Object index:(Iterable<?>)baseline.get("selectedIndices"))frozenStep5Indices.add((Integer)index);
                            List<double[]> frozenPoints=new ArrayList<>(),frozenUvs=new ArrayList<>();List<Integer> frozenAttempts=new ArrayList<>();
                            for(int index:frozenStep5Indices){frozenPoints.add(pool.get(index).point);frozenUvs.add(pool.get(index).requestedUv);frozenAttempts.add(pool.get(index).attempt);}
                            Map<String,Object> frozenQuality=GeometrySamplingMath.xyzQuality(frozenPoints);frozenQuality.putAll(GeometrySamplingMath.uvQuality(frozenUvs,bounds));
                            emit("FROZEN_STEP5_SELECTION_BASELINE",map("surfaceId",id,"qualityAttemptsAtFreeze",qualityAttempts,
                                    "poolSizeAtFreeze",pool.size(),"selectedIndices",frozenStep5Indices,"attemptOrdinals",frozenAttempts,
                                    "quality",frozenQuality,"notAnAuthenticatedPriorRun",true));
                        }
                    } else stats[3]++;
                }
                if(pool.size()>=GeometrySpreadSelection.MAX_SPREAD_POOL_SIZE) qualityStopReason="QUALITY_POOL_FULL";
                else if(qualityAttempts>=GeometrySamplingMath.MAX_QUALITY_PROBES_PER_FACE) qualityStopReason="QUALITY_PROBE_LIMIT";
            }
            Map<String,Object> selection=qualityPoolActivated?(frozenStep5Indices!=null?GeometrySpreadSelection.select(poolPoints,frozenStep5Indices):GeometryPoolSelection.select(poolPoints)):map("status","UNCHANGED_INITIAL_QUOTA",
                    "replacementApplied",false,"initialNumericallyNonCollinear",initialQuality.get("numericallyNonCollinear"));
            List<Integer> indices=new ArrayList<>();
            if(qualityPoolActivated) for(Object index:(Iterable<?>)selection.get("selectedIndices")) indices.add((Integer)index);
            else for(int i=0;i<Math.min(3,pool.size());i++) indices.add(i);
            List<double[]> acceptedPoints=new ArrayList<>(),acceptedUvs=new ArrayList<>();
            List<Candidate> finalCandidates=new ArrayList<>();
            List<Integer> finalOrdinals=new ArrayList<>();boolean usesQualityCandidate=false;
            for(int i=0;i<pool.size();i++) {
                Candidate candidate=pool.get(i);
                int[] strategyCounts="QUALITY_POOL".equals(candidate.record.get("candidatePhase"))?strategyStats.get(candidate.record.get("candidateStrategy")):null;
                if(indices.contains(i)) {
                    if(strategyCounts!=null) strategyCounts[4]++;
                    sampling.accepted++;finalCandidates.add(candidate);acceptedPoints.add(candidate.point);acceptedUvs.add(candidate.requestedUv);finalOrdinals.add(candidate.attempt);
                    Map<String,Object> record=new LinkedHashMap<>(candidate.record);
                    record.put("selectionPolicy",qualityPoolActivated?"FROZEN_STEP5_BASELINE_AND_BOUNDED_FULL_QUEUE":"UNCHANGED_FIRST_QUOTA");
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
                selection.put("frozenStep5SelectionChanged",selection.get("replacementApplied"));
                selection.put("replacementApplied",!indices.equals(Arrays.asList(0,1,2)));
                selection.put("initialNumericallyNonCollinear",initialQuality.get("numericallyNonCollinear"));
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
                emit("CROSS_RUN_SPREAD_COMPARISON",GeometryReferenceBaseline.compare(modelFileName,modelVersionStamp,lengthUnitName,id,finalQuality));
                if(accepted>=3) {
                    sampling.planeGroupsAtQuota++;
                    Object flag=finalQuality.get("numericallyNonCollinear");
                    if(Boolean.TRUE.equals(flag)) sampling.numericalNonCollinearPlaneGroups++;
                    else if(Boolean.FALSE.equals(flag)) sampling.numericalCollinearPlaneGroups++;
                    else sampling.planeQualityUnknownGroups++;
                } else sampling.planeGroupsBelowPointQuota++;
            }
            if(accepted==3&&face.bodyIds.size()==1&&bodyMembershipComplete&&lengthUnitName!=null)finalFaceSamples.put(id,finalCandidates);
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
        void collectPairEvidence() throws IOException {
            pairEvidenceStarted=true;
            int comparisons=0,emitted=0,unavailable=sampling.facesVisited-finalFaceSamples.size();
            Map<String,Integer> candidates=new LinkedHashMap<>(),published=new LinkedHashMap<>(),outcomes=new LinkedHashMap<>();
            List<Integer> ids=new ArrayList<>(finalFaceSamples.keySet());String stop="BOUNDED_COMPARISONS_COMPLETED";
            emit("RELATIVE_FACE_PAIRING_STARTED",map("eligibleFaces",ids.size(),"facesMissingEligibleSamples",unavailable,
                    "scope","SAME_SINGLE_BODY_SAME_NATIVE_TYPE_PLANE_PLANE_OR_CYLINDER_CYLINDER",
                    "maxComparisons",GeometryPairEvidence.MAX_COMPARISONS,"maxPublishedPerType",GeometryPairEvidence.MAX_EMITTED_PER_TYPE,
                    "candidateOnly",true,"noNewNativeApiCalls",true));
            outer:for(int a=0;a<ids.size();a++)for(int b=a+1;b<ids.size();b++) {
                int ia=ids.get(a),ib=ids.get(b);Face fa=faces.get(ia),fb=faces.get(ib);
                if(!fa.bodyIds.equals(fb.bodyIds)||fa.surfaceType.getValue()!=fb.surfaceType.getValue())continue;
                if(!withinBudget()){stop="SOFT_TIME_BUDGET";pairEvidenceTruncated=true;break outer;}
                if(comparisons>=GeometryPairEvidence.MAX_COMPARISONS){stop="PAIR_COMPARISON_LIMIT";pairEvidenceTruncated=true;break outer;}
                String type=fa.surfaceType.getValue()==SurfaceType._SURFACE_PLANE?"PLANE_PLANE":"CYLINDER_CYLINDER";
                comparisons++;
                List<double[]> ap=new ArrayList<>(),an=new ArrayList<>(),bp=new ArrayList<>(),bn=new ArrayList<>();
                List<Integer> aa=new ArrayList<>(),ba=new ArrayList<>();
                for(Candidate c:finalFaceSamples.get(ia)){ap.add(c.point);an.add(vector(c.record.get("normalUnit")));aa.add(c.attempt);}
                for(Candidate c:finalFaceSamples.get(ib)){bp.add(c.point);bn.add(vector(c.record.get("normalUnit")));ba.add(c.attempt);}
                Map<String,Object> evidence=GeometryPairEvidence.analyze(type,ap,an,bp,bn);
                String status=String.valueOf(evidence.get("status"));outcomes.put(status,outcomes.getOrDefault(status,0)+1);
                if(!Boolean.TRUE.equals(evidence.get("candidate")))continue;
                candidates.put(type,candidates.getOrDefault(type,0)+1);
                if(published.getOrDefault(type,0)>=GeometryPairEvidence.MAX_EMITTED_PER_TYPE)continue;
                published.put(type,published.getOrDefault(type,0)+1);emitted++;
                evidence.put("faceA",ia);evidence.put("faceB",ib);evidence.put("bodyId",fa.bodyIds.iterator().next());
                evidence.put("faceAAttemptOrdinals",aa);evidence.put("faceBAttemptOrdinals",ba);
                evidence.put("projectionLengthUnit",lengthUnitName);evidence.put("identityDisposition","PENDING_FINAL_COLLECTION_SUMMARY");
                emit("RELATIVE_FACE_PAIR_CANDIDATE",evidence);
                List<Map<String,Object>> selected=distancePairs.computeIfAbsent(type,key->new ArrayList<>());
                if(selected.size()<GeometryAnalyticMath.MAX_DISTANCE_PAIRS_PER_TYPE)selected.add(new LinkedHashMap<>(evidence));
            }
            if(!withinBudget()){stop="SOFT_TIME_BUDGET";pairEvidenceTruncated=true;}
            int count=0;for(int v:candidates.values())count+=v;
            emit("RELATIVE_FACE_PAIRING_SUMMARY",map("eligibleFaces",ids.size(),"facesMissingEligibleSamples",unavailable,
                    "comparisons",comparisons,"outcomeCounts",outcomes,"candidateCountsByType",candidates,"publishedCountsByType",published,
                    "pairEvidenceOutcome",ids.size()<2?"INSUFFICIENT_ELIGIBLE_SAMPLE_EVIDENCE":pairEvidenceTruncated?"PARTIAL_CANDIDATE_SCAN":"BOUNDED_CANDIDATE_SCAN_ONLY",
                    "published",emitted,"candidateRecordsOmittedByOutputCap",count-emitted,"comparisonTruncated",pairEvidenceTruncated,
                    "stopReason",stop,"ordering","FIRST_SEEN_NOT_BEST_OR_EXHAUSTIVE_OPPOSITE_FACE_RANKING",
                    "unknownCylinderAxesRadiiAndTrimmedOverlap",true,"thicknessAssessed",false,"truePairsConfirmed",0));
            pairEvidenceFinished=true;
        }
        void collectAnalyticParameters() throws IOException {
            int visited=0,valid=0;analyticPhaseStarted=true;
            emit("ANALYTIC_PARAMETERS_STARTED",map("eligibleFaces",finalFaceSamples.size(),"source","Surface.GetSurfaceDescriptor / TransformedSurfaceDescriptor.GetCoordSys / CylinderDescriptor.GetRadius","newNativeReads",true));
            for(Integer id:finalFaceSamples.keySet()) {
                if(!withinBudget()){distancePhaseTruncated=true;break;}
                Face face=faces.get(id);String kind=typeName(face.surfaceType);AnalyticData data=new AnalyticData(kind);
                Map<String,Object> record=map("surfaceId",id,"bodyId",face.bodyIds.iterator().next(),"nativeSurfaceType",kind,
                        "lengthUnit",known(lengthUnitName),"radiusLengthUnit",known(lengthUnitName),"coordinateFrame","ACTIVE_PART_MODEL_AS_RETURNED_BY_NATIVE_DESCRIPTOR",
                        "normalIsOutwardCertified",false,"source","Surface.GetSurfaceDescriptor","failureValuesPreserved",true,
                        "liveSurfaceOrientationCode",finalFaceSamples.get(id).get(0).record.get("surfaceOrientationCode"));
                SurfaceDescriptor descriptor=read("Surface["+id+"].GetSurfaceDescriptor",face.surface::GetSurfaceDescriptor);
                if(descriptor!=null){
                    record.put("descriptorRuntimeClass",((Object)descriptor).getClass().getName());
                    SurfaceType descriptorType=read("Descriptor["+id+"].GetSurfaceType",descriptor::GetSurfaceType);
                    SurfaceOrientation descriptorOrientation=read("Descriptor["+id+"].GetOrientation",descriptor::GetOrientation);
                    record.put("descriptorTypeCode",descriptorType==null?"UNKNOWN":descriptorType.getValue());
                    record.put("descriptorOrientationCode",descriptorOrientation==null?"UNKNOWN":descriptorOrientation.getValue());
                    boolean typed=kind.equals("PLANE")?descriptor instanceof PlaneDescriptor:descriptor instanceof CylinderDescriptor;
                    if(typed&&descriptorType!=null&&descriptorType.getValue()==face.surfaceType.getValue()&&descriptor instanceof TransformedSurfaceDescriptor){
                        TransformedSurfaceDescriptor transformed=(TransformedSurfaceDescriptor)descriptor;
                        Transform3D frame=read("Descriptor["+id+"].GetCoordSys",transformed::GetCoordSys);
                        if(frame!=null){
                            Point3D origin=read("DescriptorFrame["+id+"].GetOrigin",frame::GetOrigin);
                            Vector3D x=read("DescriptorFrame["+id+"].GetXAxis",frame::GetXAxis),y=read("DescriptorFrame["+id+"].GetYAxis",frame::GetYAxis),z=read("DescriptorFrame["+id+"].GetZAxis",frame::GetZAxis);
                            data.origin=origin==null?null:read("DescriptorOrigin["+id+"].get",()->new double[]{origin.get(0),origin.get(1),origin.get(2)});
                            data.x=x==null?null:read("DescriptorXAxis["+id+"].get",()->new double[]{x.get(0),x.get(1),x.get(2)});
                            data.y=y==null?null:read("DescriptorYAxis["+id+"].get",()->new double[]{y.get(0),y.get(1),y.get(2)});
                            data.z=z==null?null:read("DescriptorZAxis["+id+"].get",()->new double[]{z.get(0),z.get(1),z.get(2)});
                        }
                        if(descriptor instanceof CylinderDescriptor){CylinderDescriptor cylinder=(CylinderDescriptor)descriptor;data.radius=read("CylinderDescriptor["+id+"].GetRadius",cylinder::GetRadius);}
                    }else record.put("descriptorFailure","PUBLIC_DESCRIPTOR_INTERFACE_OR_TYPE_MISMATCH");
                }else record.put("descriptorFailure","DESCRIPTOR_UNAVAILABLE");
                List<double[]> points=new ArrayList<>(),normals=new ArrayList<>();for(Candidate c:finalFaceSamples.get(id)){points.add(c.point);normals.add(vector(c.record.get("normalUnit")));}
                Map<String,Object> validation=GeometryAnalyticMath.validate(kind,data.origin,data.x,data.y,data.z,data.radius,points,normals);
                data.valid=lengthUnitName!=null&&Boolean.TRUE.equals(validation.get("parametersConsistentWithSamples"));
                record.put("origin",GeometrySamplingMath.safeValues(data.origin));record.put("xAxisRaw",GeometrySamplingMath.safeValues(data.x));record.put("yAxisRaw",GeometrySamplingMath.safeValues(data.y));record.put("zAxisCandidateRaw",GeometrySamplingMath.safeValues(data.z));record.put("radiusCandidateRaw",data.radius==null?"UNKNOWN":Double.isFinite(data.radius)?data.radius:String.valueOf(data.radius));
                record.put("parameterSampleValidation",validation);record.put("usableForLocalProjection",data.valid);record.put("analyticSemanticConventionIndependentlyCertified",false);
                analyticData.put(id,data);visited++;if(data.valid)valid++;emit("SURFACE_ANALYTIC_PARAMETERS",record);
            }
            analyticInvalid=visited-valid;analyticPhaseFinished=visited==finalFaceSamples.size();
            emit("ANALYTIC_PARAMETERS_SUMMARY",map("eligibleFaces",finalFaceSamples.size(),"visited",visited,"usableParameters",valid,"unknownOrInconsistent",analyticInvalid,"notVisited",finalFaceSamples.size()-visited,"finished",analyticPhaseFinished,"outcome",analyticPhaseFinished&&analyticInvalid==0?"SAMPLE_CONSISTENT_ANALYTIC_CANDIDATES_ONLY":"PARTIAL_OR_UNKNOWN_ANALYTIC_PARAMETERS","axisConventionCertified",false));
        }
        boolean canEvaluateDomain(){
            if(!withinBudget()||domainEvaluations>=GeometryAnalyticMath.MAX_DOMAIN_EVALUATIONS){distancePhaseTruncated=true;return false;}return true;
        }
        DomainPoint evaluateDomain(Integer id,double[] requested,String pairId,String role) throws IOException {
            if(!canEvaluateDomain())return null;int ordinal=++domainEvaluations;
            Map<String,Object> e=map("pairId",pairId,"domainEvaluationOrdinal",ordinal,"surfaceId",id,"role",role,
                    "requestedWorldPoint",GeometrySamplingMath.safeValues(requested),"lengthUnit",known(lengthUnitName),"nativeInsideAccepted",false,
                    "nativeMethod","Surface.EvalParameters -> VerifyUV -> Eval3DData -> exact GetParams -> VerifyUV -> GetPoint/GetNormal",
                    "xyzNumericGuardIsModelTolerance",false,"identityDisposition","PENDING_FINAL_COLLECTION_SUMMARY");
            if(!GeometrySamplingMath.finite3(requested))return domainReject(e,"NONFINITE_WORLD_PROPOSAL");
            Point3D argument=read("Point3D.create/set_ARGUMENT_ONLY[domain:"+ordinal+"]",()->{Point3D v=Point3D.create();if(v!=null)for(int k=0;k<3;k++)v.set(k,requested[k]);return v;});
            if(argument==null)return domainReject(e,"POINT_ARGUMENT_UNAVAILABLE");
            Surface surface=faces.get(id).surface;
            UVParams uv=read("Surface["+id+"].EvalParameters[domain:"+ordinal+"]",()->surface.EvalParameters(argument));
            double[] requestedUv=uv==null?null:read("DomainUV.get["+ordinal+"]",()->new double[]{uv.get(0),uv.get(1)});
            e.put("requestedUv",GeometrySamplingMath.safeValues(requestedUv));
            if(requestedUv==null||!Double.isFinite(requestedUv[0])||!Double.isFinite(requestedUv[1]))return domainReject(e,"NATIVE_PARAMETERS_UNAVAILABLE_OR_NONFINITE");
            Placement first=read("Surface["+id+"].VerifyUV.requested[domain:"+ordinal+"]",()->surface.VerifyUV(uv));
            e.put("requestedVerifyPlacementCode",first==null?"UNKNOWN":first.getValue());
            if(first==null||first.getValue()!=Placement._PLACE_INSIDE)return domainReject(e,"REQUESTED_UV_NOT_NATIVE_INSIDE");
            if(!withinBudget()){distancePhaseTruncated=true;return domainReject(e,"SOFT_BUDGET_BEFORE_DOMAIN_EVALUATION");}
            SurfXYZData value=read("Surface["+id+"].Eval3DData[domain:"+ordinal+"]",()->surface.Eval3DData(uv));
            if(value==null)return domainReject(e,"DOMAIN_EVAL3D_UNAVAILABLE");
            UVParams back=read("DomainSurfXYZ.GetParams["+ordinal+"]",value::GetParams);
            double[] actualUv=back==null?null:read("DomainReturnedUV.get["+ordinal+"]",()->new double[]{back.get(0),back.get(1)});
            e.put("returnedUv",GeometrySamplingMath.safeValues(actualUv));
            if(!GeometrySamplingMath.exactUv(requestedUv,actualUv))return domainReject(e,"RETURNED_UV_NOT_EXACT_NO_CANONICALIZATION");
            Placement second=read("Surface["+id+"].VerifyUV.returned[domain:"+ordinal+"]",()->surface.VerifyUV(back));
            e.put("returnedVerifyPlacementCode",second==null?"UNKNOWN":second.getValue());
            if(second==null||second.getValue()!=Placement._PLACE_INSIDE)return domainReject(e,"RETURNED_UV_NOT_NATIVE_INSIDE");
            Point3D point=read("DomainSurfXYZ.GetPoint["+ordinal+"]",value::GetPoint);Vector3D normal=read("DomainSurfXYZ.GetNormal["+ordinal+"]",value::GetNormal);
            double[] xyz=point==null?null:read("DomainPoint.get["+ordinal+"]",()->new double[]{point.get(0),point.get(1),point.get(2)});
            double[] raw=normal==null?null:read("DomainNormal.get["+ordinal+"]",()->new double[]{normal.get(0),normal.get(1),normal.get(2)});
            e.put("point",GeometrySamplingMath.safeValues(xyz));e.put("normalRaw",GeometrySamplingMath.safeValues(raw));
            if(!GeometrySamplingMath.finite3(xyz))return domainReject(e,"DOMAIN_POINT_UNAVAILABLE_OR_NONFINITE");
            double[] unit;try{unit=GeometrySamplingMath.normalUnit(raw);}catch(IllegalArgumentException error){return domainReject(e,error.getMessage());}
            Map<String,Object> roundTrip=GeometryAnalyticMath.roundTrip(requested,xyz);e.putAll(roundTrip);
            if(!Boolean.TRUE.equals(roundTrip.get("roundTripMatchesWithinNumericGuard")))return domainReject(e,"XYZ_ROUNDTRIP_EXCEEDS_NUMERICAL_GUARD");
            domainAccepted++;e.put("normalUnit",GeometrySamplingMath.list(unit));e.put("nativeInsideAccepted",true);e.put("reason","TWO_NATIVE_INSIDE_EXACT_UV_AND_XYZ_ROUNDTRIP");emit("PAIR_DOMAIN_POINT_EVALUATION",e);
            return new DomainPoint(xyz,unit,ordinal);
        }
        DomainPoint domainReject(Map<String,Object> e,String reason) throws IOException {domainRejected++;e.put("reason",reason);emit("PAIR_DOMAIN_POINT_EVALUATION",e);return null;}
        void collectLocalDistances() throws IOException {
            distancePhaseStarted=true;int planned=0,visited=0,withWitness=0,totalWitnesses=0;
            for(List<Map<String,Object>> values:distancePairs.values())planned+=values.size();
            emit("LOCAL_DISTANCE_ANALYSIS_STARTED",map("plannedPairs",planned,"selection","FIRST_16_PUBLISHED_COARSE_CANDIDATES_PER_TYPE_INTERLEAVED_NOT_THICKNESS_PAIRS",
                    "maxPairsPerType",GeometryAnalyticMath.MAX_DISTANCE_PAIRS_PER_TYPE,"maxDomainPointEvaluationChains",GeometryAnalyticMath.MAX_DOMAIN_EVALUATIONS,
                    "planeProbeLocationsPerPairMax",11,"cylinderProbeLocationsPerPairMax",6,"entireTrimmedOverlapRegionProven",false,"finalWallThickness","NOT_ASSESSED"));
            outer:for(int row=0;row<GeometryAnalyticMath.MAX_DISTANCE_PAIRS_PER_TYPE;row++)for(String type:Arrays.asList("PLANE_PLANE","CYLINDER_CYLINDER")){
                List<Map<String,Object>> values=distancePairs.get(type);if(values==null||row>=values.size())continue;
                if(!canEvaluateDomain())break outer;
                Map<String,Object> coarse=values.get(row);int ia=(Integer)coarse.get("faceA"),ib=(Integer)coarse.get("faceB");String pairId=ia+"_"+ib;visited++;
                AnalyticData aa=analyticData.get(ia),bb=analyticData.get(ib);
                if(aa==null||bb==null||!aa.valid||!bb.valid){emit("PAIR_OVERLAP_VALIDATION_SUMMARY",map("pairId",pairId,"faceA",ia,"faceB",ib,"pairType",type,"lengthUnit",known(lengthUnitName),"supportingGapCandidate","UNKNOWN",
                        "status","UNKNOWN_ANALYTIC_PARAMETERS","probeLocationsAttempted",0,"nativeInsideWitnesses",0,"entireTrimmedOverlapRegionProven",false,"finalWallThickness","NOT_ASSESSED"));continue;}
                Map<String,Object> gap=GeometryAnalyticMath.pair(aa.kind,aa.origin,aa.z,aa.radius,bb.origin,bb.z,bb.radius);
                gap.put("pairId",pairId);gap.put("faceA",ia);gap.put("faceB",ib);gap.put("lengthUnit",lengthUnitName);gap.put("analyticAxesRemainCandidates",true);emit("PAIR_ANALYTIC_GAP_CANDIDATE",gap);
                List<Integer> sourceIds=new ArrayList<>();List<double[]> worldSeeds=new ArrayList<>();List<String> sources=new ArrayList<>();
                for(int id:new int[]{ia,ib})for(Candidate c:finalFaceSamples.get(id)){sourceIds.add(id);worldSeeds.add(c.point);sources.add("REVALIDATED_FINAL_NATIVE_SAMPLE");}
                if(type.equals("PLANE_PLANE")&&coarse.get("projectedOverlapRectangleCandidate") instanceof List<?>){
                    List<?> box=(List<?>)coarse.get("projectedOverlapRectangleCandidate");double[] u=vector(coarse.get("projectionU")),v=vector(coarse.get("projectionV")),origin=finalFaceSamples.get(ia).get(0).point;
                    double u0=((Number)box.get(0)).doubleValue(),u1=((Number)box.get(1)).doubleValue(),v0=((Number)box.get(2)).doubleValue(),v1=((Number)box.get(3)).doubleValue();
                    for(double[] f:new double[][]{{.5,.5},{.25,.25},{.75,.25},{.25,.75},{.75,.75}}){double pu=u0+(u1-u0)*f[0],pv=v0+(v1-v0)*f[1];worldSeeds.add(new double[]{origin[0]+pu*u[0]+pv*v[0],origin[1]+pu*u[1]+pv*v[1],origin[2]+pu*u[2]+pv*v[2]});sourceIds.add(ia);sources.add("COMMON_PROJECTION_RECTANGLE_GRID_PROPOSAL_NOT_NATIVE_VALID_UNTIL_CHECKED");}
                }
                int probes=0,witnesses=0,aToB=0,bToA=0,grid=0,startEvaluations=domainEvaluations;
                for(int probe=0;probe<worldSeeds.size();probe++){
                    if(!canEvaluateDomain())break;probes++;
                    int source=sourceIds.get(probe),target=source==ia?ib:ia;AnalyticData targetData=target==ia?aa:bb;
                    DomainPoint from=evaluateDomain(source,worldSeeds.get(probe),pairId,"SOURCE_REVALIDATION");if(from==null)continue;
                    double[] proposal=GeometryAnalyticMath.project(targetData.kind,from.point,targetData.origin,targetData.z,targetData.radius);
                    if(proposal==null){emit("PAIR_LOCAL_PROBE_REJECTED",map("pairId",pairId,"probeOrdinal",probe+1,"reason","ANALYTIC_TARGET_PROJECTION_UNAVAILABLE","finalWallThickness","NOT_ASSESSED"));continue;}
                    DomainPoint to=evaluateDomain(target,proposal,pairId,"TARGET_PROJECTED_POINT");if(to==null)continue;
                    double distance=GeometryAnalyticMath.length(GeometryAnalyticMath.sub(to.point,from.point));if(!Double.isFinite(distance)){emit("PAIR_LOCAL_PROBE_REJECTED",map("pairId",pairId,"probeOrdinal",probe+1,"reason","NONFINITE_LOCAL_DISTANCE"));continue;}
                    witnesses++;totalWitnesses++;if(source==ia)aToB++;else bToA++;if(sources.get(probe).startsWith("COMMON_"))grid++;
                    emit("PAIR_LOCAL_DISTANCE_WITNESS",map("pairId",pairId,"faceA",ia,"faceB",ib,"sourceFace",source,"targetFace",target,"probeOrdinal",probe+1,"seedSource",sources.get(probe),
                            "sourceDomainEvaluationOrdinal",from.ordinal,"targetDomainEvaluationOrdinal",to.ordinal,"sourcePoint",GeometrySamplingMath.list(from.point),"targetPoint",GeometrySamplingMath.list(to.point),
                            "sourceNormalUnit",GeometrySamplingMath.list(from.normal),"targetNormalUnit",GeometrySamplingMath.list(to.normal),"sampleNormalDot",GeometryAnalyticMath.dot(from.normal,to.normal),
                            "localPointDistance",distance,"lengthUnit",lengthUnitName,"distanceMeaning","DISTANCE_BETWEEN_TWO_NATIVE_INSIDE_MAPPED_POINTS_NOT_GLOBAL_MINIMUM_OR_WALL_THICKNESS",
                            "mapping",aa.kind.equals("PLANE")?"TARGET_SUPPORT_PLANE_NORMAL_PROJECTION":"TARGET_CYLINDER_RADIAL_PROJECTION_CANDIDATE",
                            "bothPointsNativeInside",true,"entireTrimmedOverlapRegionProven",false,"trueOppositeFacesConfirmed",false,"finalWallThickness","NOT_ASSESSED","identityDisposition","PENDING_FINAL_COLLECTION_SUMMARY"));
                }
                if(witnesses>0)withWitness++;
                emit("PAIR_OVERLAP_VALIDATION_SUMMARY",map("pairId",pairId,"faceA",ia,"faceB",ib,"pairType",type,"probeLocationsPlanned",worldSeeds.size(),"probeLocationsAttempted",probes,"domainEvaluationChains",domainEvaluations-startEvaluations,
                        "nativeInsideWitnesses",witnesses,"aToBWitnesses",aToB,"bToAWitnesses",bToA,"projectionGridWitnesses",grid,
                        "status",distancePhaseTruncated?"PARTIAL_DOMAIN_PROBING":witnesses>0?"DISCRETE_TWO_FACE_NATIVE_INSIDE_WITNESSES":"NO_INTERIOR_WITNESS_NOT_PROOF_OF_NO_OVERLAP",
                        "entireTrimmedOverlapRegionProven",false,"cylinderAngularAxialTrimCoverage","UNKNOWN_NOT_RECONSTRUCTED","finalWallThickness","NOT_ASSESSED"));
            }
            distancePhaseFinished=visited==planned&&!distancePhaseTruncated;
            emit("LOCAL_DISTANCE_ANALYSIS_SUMMARY",map("plannedPairs",planned,"visitedPairs",visited,"pairsWithNativeInsideWitnesses",withWitness,"nativeInsideDistanceWitnesses",totalWitnesses,
                    "outcome",planned==0?"NO_SELECTED_CANDIDATES_DISTANCE_UNKNOWN":distancePhaseFinished?"BOUNDED_DISCRETE_DOMAIN_WITNESSES_ONLY":"PARTIAL_OR_ABORTED_LOCAL_EVIDENCE",
                    "domainEvaluationChains",domainEvaluations,"domainAccepted",domainAccepted,"domainRejected",domainRejected,"finished",distancePhaseFinished,"truncated",distancePhaseTruncated,
                    "samplingAttemptsSeparateFromDomainEvaluations",true,"finalWallThickness","NOT_ASSESSED","uniformThicknessAssessment","INSUFFICIENT_EVIDENCE","entireTrimmedOverlapRegionProven",false));
        }
        void observeClassificationInput(String event,Map<String,Object> fields) {
            if(!Arrays.asList("PAIR_ANALYTIC_GAP_CANDIDATE","PAIR_LOCAL_DISTANCE_WITNESS","PAIR_OVERLAP_VALIDATION_SUMMARY").contains(event))return;
            String id=String.valueOf(fields.get("pairId"));
            if(event.equals("PAIR_ANALYTIC_GAP_CANDIDATE")){if(classificationGaps.size()<32||classificationGaps.containsKey(id))classificationGaps.put(id,new LinkedHashMap<>(fields));else classificationObserverOverflow=true;return;}
            if(event.equals("PAIR_OVERLAP_VALIDATION_SUMMARY")){if(classificationPairSummaries.size()<32||classificationPairSummaries.containsKey(id))classificationPairSummaries.put(id,new LinkedHashMap<>(fields));else classificationObserverOverflow=true;return;}
            if(classificationStats.size()>=32&&!classificationStats.containsKey(id)){classificationObserverOverflow=true;return;}
            GeometryCandidateClassification.Stats stats=classificationStats.computeIfAbsent(id,k->new GeometryCandidateClassification.Stats());
            if(!Boolean.TRUE.equals(fields.get("bothPointsNativeInside"))||!(fields.get("localPointDistance") instanceof Number)){stats.invalid++;return;}
            stats.observe(((Number)fields.get("faceA")).intValue(),((Number)fields.get("faceB")).intValue(),((Number)fields.get("sourceFace")).intValue(),((Number)fields.get("targetFace")).intValue(),
                    ((Number)fields.get("localPointDistance")).doubleValue(),fields.get("sampleNormalDot") instanceof Number?((Number)fields.get("sampleNormalDot")).doubleValue():Double.NaN);
        }
        void collectCandidateClassifications() throws IOException {
            Map<String,Integer> tags=new LinkedHashMap<>(),directionCounts=new LinkedHashMap<>();int count=0,planned=0;
            for(List<Map<String,Object>> values:distancePairs.values())planned+=values.size();
            emit("CANDIDATE_CLASSIFICATION_STARTED",map("plannedPairs",planned,"policy","OVERLAPPING_REVIEW_LABELS_ONLY","farReviewThresholdMm",GeometryCandidateClassification.FAR_REVIEW_THRESHOLD_MM,
                    "thresholdIsWallThicknessLimit",false,"newNativeCalls",false,"unknownUnitsNotConverted",true));
            for(int row=0;row<GeometryAnalyticMath.MAX_DISTANCE_PAIRS_PER_TYPE;row++)for(String type:Arrays.asList("PLANE_PLANE","CYLINDER_CYLINDER")){
                List<Map<String,Object>> values=distancePairs.get(type);if(values==null||row>=values.size())continue;
                Map<String,Object> pair=values.get(row);int ia=(Integer)pair.get("faceA"),ib=(Integer)pair.get("faceB");String id=ia+"_"+ib;
                Map<String,Object> result=GeometryCandidateClassification.classify(id,ia,ib,type,lengthUnitName,classificationGaps.get(id),classificationPairSummaries.get(id),classificationStats.get(id));
                if(Boolean.FALSE.equals(result.get("summaryCountConsistency"))||((Number)result.get("invalidRecords")).intValue()>0)classificationInvalidPairs++;
                for(Object tag:(Iterable<?>)result.get("tags")){String key=String.valueOf(tag);tags.put(key,tags.getOrDefault(key,0)+1);}
                String direction=String.valueOf(result.get("directionState"));directionCounts.put(direction,directionCounts.getOrDefault(direction,0)+1);
                result.put("identityDisposition","PENDING_FINAL_COLLECTION_SUMMARY");emit("PAIR_CANDIDATE_CLASSIFICATION",result);count++;
            }
            classificationFinished=count==planned;
            emit("CANDIDATE_CLASSIFICATION_SUMMARY",map("plannedPairs",planned,"classifiedPairs",count,"tagCounts",tags,"tagCountsAreOverlappingNotASumToPairCount",true,
                    "directionCounts",directionCounts,"invalidPairInputs",classificationInvalidPairs,"observerOverflow",classificationObserverOverflow,"finished",classificationFinished,
                    "sourceDomainEvidenceMayBePartial",!distancePhaseFinished,"farReviewThresholdMm",GeometryCandidateClassification.FAR_REVIEW_THRESHOLD_MM,
                    "globalThicknessChosenFromDistanceMode",false,"materialOccupancyVerified",false,"occlusionVerified",false,"finalWallThickness","NOT_ASSESSED","uniformThicknessAssessment","INSUFFICIENT_EVIDENCE"));
        }
        void emitSdkInterfaceReview() throws IOException {
            List<Map<String,Object>> values=GeometrySdkInterfaceReview.entries();
            emit("SDK_READONLY_INTERFACE_REVIEW_STARTED",map("entries",values.size(),"scope","STATIC_INSPECTION_OF_SUPPLIED_REAL_SDK_PUBLIC_SIGNATURES_NOT_RUNTIME_FEASIBILITY_CERTIFICATION",
                    "suppliedSdkSha256",GeometrySdkInterfaceReview.SDK_SHA256,"newNativeOperationsExecuted",false));
            for(Map<String,Object> value:values)emit("SDK_READONLY_INTERFACE_REVIEW_ITEM",value);
            interfaceReviewFinished=true;
            emit("SDK_READONLY_INTERFACE_REVIEW_SUMMARY",map("entries",values.size(),"nativeRayIntersectionsInvoked",false,"nativeMaterialOccupancyInvoked",false,"nativeOcclusionInvoked",false,
                    "readOnlySemanticsCertified",false,"runtimeLicenseAndObjectCompatibilityVerified",false,"globalMaterialThicknessProof","NOT_ASSESSED"));
        }
        double[] vector(Object value) {
            if(!(value instanceof List<?>))return null;List<?> v=(List<?>)value;if(v.size()!=3)return null;
            return new double[]{((Number)v.get(0)).doubleValue(),((Number)v.get(1)).doubleValue(),((Number)v.get(2)).doubleValue()};
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
        @Override public void close() throws IOException {
            endTimingPhase("RUN_CLOSED_OR_ABORTED");long begin=System.nanoTime();String utc=Instant.now().toString();boolean completed=false;
            try{writer.close();completed=true;}
            finally{long end=System.nanoTime();timingSink.record("RUN_LOG_CLOSE",requestId,map("beginUtc",utc,"endUtc",Instant.now().toString(),"elapsedNs",end-begin,"completed",completed));
                timingSink.record("RUN_OBSERVATION_TOTALS",requestId,map("businessCountersThroughClose",timing.report(),"stderrPolicyThroughClose",stderrPolicy.report(),"businessWriterCloseNs",end-begin,"businessWriterCloseCompleted",completed,
                    "phaseReports",timingPhaseReports,"commandEntryToCloseNs",end-commandEnteredNs,"journalStateBeforeThisRecord",timingSink.state(),"journalOwnCurrentAndLaterWritesExcluded",true,"allNativeApisRemainOnCallerThread",true));}
        }
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
