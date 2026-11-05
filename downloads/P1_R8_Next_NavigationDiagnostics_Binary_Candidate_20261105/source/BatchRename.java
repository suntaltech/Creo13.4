import com.ptc.pfc.pfcArgument.ArgValue;
import com.ptc.pfc.pfcArgument.pfcArgument;
import com.ptc.pfc.pfcCommand.DefaultUICommandActionListener;
import com.ptc.pfc.pfcCommand.UICommand;
import com.ptc.pfc.pfcFeature.FeatureType;
import com.ptc.pfc.pfcFeature.Feature;
import com.ptc.pfc.pfcFeature.Features;
import com.ptc.pfc.pfcGeometry.Surface;
import com.ptc.pfc.pfcGeometry.SurfaceType;
import com.ptc.pfc.pfcGeometry.Surfaces;
import com.ptc.pfc.pfcModel.Model;
import com.ptc.pfc.pfcModel.ModelType;
import com.ptc.pfc.pfcSelect.Selection;
import com.ptc.pfc.pfcSelect.pfcSelect;
import com.ptc.pfc.pfcSession.CreoCompatibility;
import com.ptc.pfc.pfcSession.Session;
import com.ptc.pfc.pfcSession.pfcSession;
import com.ptc.pfc.pfcSolid.SolidBody;
import com.ptc.pfc.pfcWindow.Window;
import com.ptc.pfc.pfcWindow.Windows;
import com.ptc.wfc.wfcElemIds.wfcElemIds;
import com.ptc.wfc.wfcElementTree.Element;
import com.ptc.wfc.wfcElementTree.ElementTree;
import com.ptc.wfc.wfcElementTree.Elements;
import com.ptc.wfc.wfcFeature.WFeature;
import com.ptc.wfc.wfcFeatureInstructions.FeatCreateOption;
import com.ptc.wfc.wfcFeatureInstructions.FeatCreateOptions;
import com.ptc.wfc.wfcSession.WSession;
import com.ptc.wfc.wfcSolid.WSolid;
import com.ptc.wfc.wfcSolidInstructions.WRegenInstructions;
import com.ptc.wfc.wfcSolidInstructions.wfcSolidInstructions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creo 13 同步 J-Link 入口：注册原生命令、打开内嵌页面，并以独立原生命令执行6A驱动面转换；网页继续只读。
 * 不创建 AsyncConnection，不请求 CREOSON，不在命令回调中等待网络或启动子进程。
 * 必须使用本机 Creo 13 的同步 SDK archive 编译，不混入 pfcasync.jar。
 */
public final class BatchRename {
    private static final String MESSAGES = "p1_6a_ui_commands.txt";
    private static final String RENAME_COMMAND = "SISWBatchRename";
    private static final String RENAME_PAGE = "http://localhost:9056/rename.html";
    private static final String RENAME_ICON = "batchrename.png";
    private static final String SMT_COMMAND = "SISWSheetMetal";
    private static final String SMT_CONVERT_COMMAND = "SISWSheetMetalConvert";
    private static final String CONVERSION_MESSAGES = "p1_6a_r2.txt";
    private static final double MIN_THICKNESS = 0.1;
    private static final double MAX_THICKNESS = 20.0;
    private static final String SMT_PAGE = "http://localhost:9056/sheetmetal.html";
    private static final String SMT_ICON = "sheetmetal.png";
    private static final String ABOUT_COMMAND = "SISWAbout";
    private static final String ABOUT_PAGE = "http://localhost:9056/about.html";
    private static final String ABOUT_ICON = "about.png";
    // WFC AddBrowserMessageListener 在当前 Creo 13.4 同步会话中会长期占用 CIP 原生通道；
    // 先关闭原生浏览器监听，钣金页面通过 CREOSON HTTP 与后续可用的原生路径处理，避免阻塞 Ribbon 命令回调。
    private static final boolean SHEETMETAL_BRIDGE_ENABLED = false;

    private static Session session;
    private static WSession wfcSession;
    private static final java.util.List<UICommand> commands = new java.util.ArrayList<>();
    private static final java.util.List<DefaultUICommandActionListener> listeners = new java.util.ArrayList<>();
    private static boolean active;
    private static boolean opening;
    private static boolean converting;
    private static Thread creosonStarter;
    private static final ThreadLocal<String> pageRequestId = new ThreadLocal<>();

    private BatchRename() { }

    /** 同步注册入口必须是 public static void 且无参数。 */
    public static void start() throws Exception {
        if (active) {
            log("start ignored: already active", null);
            return;
        }
        if (!listeners.isEmpty()) {
            throw new IllegalStateException("Previous listener detach failed; restart Creo before reloading");
        }
        log("start entered; java=" + System.getProperty("java.version"), null);
        try {
            // 显式设置兼容性，避免先用 GetProESession() 进入旧的 C3 兼容模式。
            session = pfcSession.GetCurrentSessionWithCompatibility(CreoCompatibility.C4Compatible);
            if (session == null) {
                throw new IllegalStateException("No synchronous Creo session");
            }
            log("synchronous session acquired", null);
            ensureCreosonRunningAsync();

            registerPageCommand(RENAME_COMMAND, new OpenPageListener(RENAME_COMMAND, RENAME_PAGE),
                    "SISW_BR_LABEL", "SISW_BR_HELP", "SISW_BR_DESC", RENAME_ICON);
            registerPageCommand(SMT_COMMAND, new OpenPageListener(SMT_COMMAND, SMT_PAGE),
                    "SISW_SMT_LABEL", "SISW_SMT_HELP", "SISW_SMT_DESC", SMT_ICON);
            registerPageCommand(ABOUT_COMMAND, new OpenPageListener(ABOUT_COMMAND, ABOUT_PAGE),
                    "SISW_ABOUT_LABEL", "SISW_ABOUT_HELP", "SISW_ABOUT_DESC", ABOUT_ICON);
            log("three native page commands registered", null);

            registerConversionCommand();
            ensureSheetmetalBridgeAsync();

            active = true;
            log("command designated", null);
            log("start returned", null);
            // 功能区位置由首次自定义导出的 .rbn 或用户现有 Ribbon 配置负责。
        } catch (Exception ex) {
            log("start failed", ex);
            detach();
            throw ex; // 让辅助应用程序显示真正的加载错误，而不是伪报启动成功。
        } catch (LinkageError ex) {
            log("JAR/class version or linkage error", ex);
            detach();
            throw ex;
        }
    }

    /** 停止时只解除回调，不退出 Creo，不关闭浏览器，不修改模型。 */
    public static void stop() {
        log("stop entered", null);
        active = false;
        detach();
        log("stop returned", null);
    }

    /** 仅将既有服务探测/拉起放到后台；不在后台执行任何Creo API。 */
    private static synchronized void ensureCreosonRunningAsync() {
        if (creosonStarter != null && creosonStarter.isAlive()) return;
        creosonStarter = new Thread(() -> {
            try {
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress("localhost", 9056), 500);
                    log("port 9056 listening; service identity must be checked by frontend", null);
                    return;
                } catch (Exception notListening) { /* 单次探测，不等待端口。 */ }
                String configured = System.getenv("CREO_BR_SERVER_DIR");
                // 保留输入基线的服务默认目录；转换逻辑本身没有安装目录/模型路径依赖。
                if (configured == null || configured.isBlank()) configured = "D:\\creoson\\CreosonServerWithSetup-2.8.2-win64";
                ProcessBuilder process = new ProcessBuilder("cmd.exe", "/c", "creoson_run.bat");
                process.directory(new java.io.File(configured));
                process.inheritIO(); // 保留输入基线的stdin继承行为；Windows启动仍待本机验证。
                process.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                process.redirectError(ProcessBuilder.Redirect.DISCARD);
                process.start();
                log("CREOSON launch requested asynchronously; readiness not confirmed", null);
            } catch (Throwable error) {
                log("EXCEPTION: CREOSON_LAUNCH_FAILED", error);
            }
        }, "creoson-service-start");
        creosonStarter.setDaemon(true);
        creosonStarter.start();
    }

    /** 内部 WFC 会话适配器为包私有；反射复用同一原生指针构造公开 WSession 接口对象。 */
    private static WSession createWfcSession(Session plainSession) throws Exception {
        Class<?> adapterClass = Class.forName("com.ptc.wfc.Implementation.wfcSession$WSession");
        java.lang.reflect.Constructor<?> constructor = adapterClass.getDeclaredConstructor(long.class);
        constructor.setAccessible(true);
        Object adapter = constructor.newInstance(plainSession.getptr());
        return (WSession) adapter;
    }

    /** 本轮永不注册浏览器监听，也不清除供原生命令使用的WFC会话包装。 */
    private static void ensureSheetmetalBridgeAsync() {
        log("sheetmetal browser bridge disabled; 6A uses only SISWSheetMetalConvert native command", null);
    }

    private static UICommand registerPageCommand(String commandId, OpenPageListener pageListener,
                                                  String labelKey, String helpKey, String descriptionKey,
                                                  String iconName) throws Exception {
        UICommand pageCommand = createUiCommand(commandId, MESSAGES, labelKey, helpKey, descriptionKey, pageListener);
        designateUiCommand(commandId, MESSAGES, pageCommand, labelKey, helpKey, descriptionKey);
        try {
            pageCommand.SetIcon(iconName);
            uiRegistrationAudit(commandId, MESSAGES, labelKey, helpKey, descriptionKey, "SetIcon", "RETURNED", null);
            log("icon registered: " + commandId + " -> " + iconName, null);
        } catch (Exception | LinkageError iconError) {
            uiRegistrationAudit(commandId, MESSAGES, labelKey, helpKey, descriptionKey, "SetIcon", "THROWN", iconError);
            log("icon registration failed for " + commandId + "; command remains available with default icon", iconError);
        }
        trackCommand(pageCommand, pageListener);
        return pageCommand;
    }

    /** Only L1 API outcomes are observed here; visibility remains an external observation. */
    private static UICommand createUiCommand(String id, String messageFile, String labelKey,
                                             String helpKey, String descriptionKey,
                                             DefaultUICommandActionListener listener) throws Exception {
        uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "UICreateCommand", "ATTEMPT", null);
        try {
            UICommand command = session.UICreateCommand(id, listener);
            if (command == null) throw new IllegalStateException("UICreateCommand returned null: " + id);
            uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "UICreateCommand", "RETURNED_NON_NULL", null);
            return command;
        } catch (Exception | LinkageError error) {
            uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "UICreateCommand", "THROWN", error);
            throw error;
        }
    }

    private static void designateUiCommand(String id, String messageFile, UICommand command,
                                           String labelKey, String helpKey, String descriptionKey) throws Exception {
        uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "Designate", "ATTEMPT", null);
        try {
            command.Designate(messageFile, labelKey, helpKey, descriptionKey);
            uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "Designate", "RETURNED", null);
        } catch (Exception | LinkageError error) {
            uiRegistrationAudit(id, messageFile, labelKey, helpKey, descriptionKey, "Designate", "THROWN", error);
            throw error;
        }
    }

    private static void uiRegistrationAudit(String id, String messageFile, String labelKey, String helpKey,
                                             String descriptionKey, String stage, String outcome, Throwable error) {
        Map<String,Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("commandId", id);
        event.put("messageFile", messageFile);
        event.put("labelKey", labelKey);
        event.put("helpKey", helpKey);
        event.put("descriptionKey", descriptionKey);
        event.put("stage", stage);
        event.put("outcome", outcome);
        event.put("evidenceLevel", "L1_OBJECT_API");
        event.put("messageLookup", "MESSAGE_LOOKUP_UNVERIFIED");
        event.put("uiVisibility", "UNVERIFIED");
        event.put("objectRegistered", "UNVERIFIED");
        event.put("macroDispatchable", "UNVERIFIED");
        event.put("uiListed", "UNVERIFIED");
        event.put("ribbonVisible", "UNVERIFIED");
        event.put("featureCreated", "UNVERIFIED");
        event.put("runtimeStringseqAcceptance", "RUNTIME_STRINGSEQ_ACCEPTANCE_UNVERIFIED");
        event.put("datStartup", "UNVERIFIED_REGISTRY_NOT_READ");
        event.put("datToolkit", "UNVERIFIED_REGISTRY_NOT_READ");
        event.put("datCreoType", "UNVERIFIED_REGISTRY_NOT_READ");
        event.put("datClasspathShape", "UNVERIFIED_REGISTRY_NOT_READ");
        if (error != null) event.put("exceptionClass", error.getClass().getName());
        log("6A_UI_REGISTRATION " + toJson(event), error);
    }

    private static void detach() {
        active = false;
        for (int i = 0; i < commands.size(); i++) {
            try {
                commands.get(i).RemoveActionListener(listeners.get(i));
            } catch (Throwable ex) {
                log("EXCEPTION: LISTENER_DETACH_FAILED; command index=" + i + "; restart Creo before reloading", ex);
            }
        }
        commands.clear();
        listeners.clear();
        session = null;
        wfcSession = null;
    }

    /** 原生回调只发出本模块浏览器导航，随后立即把控制权交回 Creo。 */
    private static final class OpenPageListener extends DefaultUICommandActionListener {
        private final String commandId;
        private final String pageUrl;

        private OpenPageListener(String commandId, String pageUrl) {
            this.commandId = commandId;
            this.pageUrl = pageUrl;
        }

        @Override
        public void OnCommand() {
            String previousRequest = pageRequestId.get();
            pageRequestId.set(java.util.UUID.randomUUID().toString());
            try {
                if (!active) {
                    log("OnCommand ignored: plugin inactive; target=" + pageUrl, null);
                    return;
                }
                if (opening) {
                    log("OnCommand ignored: previous navigation still running; target=" + pageUrl, null);
                    return;
                }
                if (session == null) {
                    log("OnCommand ignored: J-Link session null; target=" + pageUrl, null);
                    return;
                }
                opening = true;
                String targetUrl = guardedPageUrl(commandId, pageUrl);
                log("OnCommand entered; command=" + commandId + "; target=" + targetUrl, null);
                String preNavigationUrl = null;
                String navigationPhase = "WINDOW_RESOLUTION";
                try {
                    Window window = null;
                    try {
                        window = session.GetCurrentWindow();
                    } catch (Throwable currentWindowError) {
                        log("GetCurrentWindow failed; trying window list", currentWindowError);
                    }
                    if (window == null) {
                        try {
                            Windows windows = session.ListWindows();
                            if (windows != null && windows.getarraysize() > 0) {
                                window = windows.get(0);
                                log("using first listed Creo window after current window was null", null);
                            }
                        } catch (Throwable listWindowsError) {
                            log("ListWindows fallback failed", listWindowsError);
                        }
                    }
                    if (window == null) {
                        log("No available Creo window; cannot open " + pageUrl, null);
                        return;
                    }
                    navigationPhase = "PRE_NAVIGATION_URL_PROBE";
                    pageNavigationAudit(commandId, navigationPhase, "ATTEMPT", targetUrl, null, null);
                    try {
                        preNavigationUrl = window.GetURL();
                        pageNavigationAudit(commandId, navigationPhase, "RETURNED", targetUrl, preNavigationUrl, null);
                    } catch (Exception urlError) {
                        // A failed probe retains the existing null-URL navigation policy.
                        // Keep the real stack and requestId; this is not a SetURL failure.
                        pageNavigationAudit(commandId, navigationPhase, "THROWN", targetUrl, null, urlError);
                    }
                    if (preNavigationUrl == null || !preNavigationUrl.equals(targetUrl)) {
                        navigationPhase = "SET_URL";
                        pageNavigationAudit(commandId, navigationPhase, "ATTEMPT", targetUrl, preNavigationUrl, null);
                        window.SetURL(targetUrl);
                        pageNavigationAudit(commandId, navigationPhase, "RETURNED", targetUrl, preNavigationUrl, null);
                        log("browser navigation issued; target=" + targetUrl, null);
                    } else {
                        pageNavigationAudit(commandId, "SET_URL", "SKIPPED_SAME_URL", targetUrl, preNavigationUrl, null);
                        log("browser tab already open; reusing existing tab", null);
                    }
                    navigationPhase = "SET_BROWSER_SIZE";
                    window.SetBrowserSize(80.0);
                    navigationPhase = "ACTIVATE";
                    window.Activate();
                    navigationPhase = "REFRESH";
                    window.Refresh();
                    pageNavigationAudit(commandId, "CALLBACK_COMPLETION", "RETURNED", targetUrl, preNavigationUrl, null);
                    log("browser activated; preNavigationUrl=" + preNavigationUrl + "; target=" + targetUrl +
                            "; pageLoad=UNVERIFIED; OnCommand returned", null);
                } catch (Exception ex) {
                    pageNavigationAudit(commandId, navigationPhase, "THROWN", targetUrl, preNavigationUrl, ex);
                    log("open embedded browser failed; failedPhase=" + navigationPhase, null);
                } finally {
                    opening = false;
                }
            } finally {
                if (previousRequest == null) pageRequestId.remove();
                else pageRequestId.set(previousRequest);
            }
        }
    }

    /** Diagnostic events only: API returns do not prove browser page load or model safety. */
    private static void pageNavigationAudit(String commandId, String phase, String outcome,
                                             String targetUrl, String preNavigationUrl, Throwable error) {
        Map<String,Object> event = new LinkedHashMap<>();
        event.put("requestId", pageRequestId.get());
        event.put("commandId", commandId);
        event.put("phase", phase);
        event.put("outcome", outcome);
        event.put("targetUrl", targetUrl);
        event.put("preNavigationUrl", preNavigationUrl);
        event.put("pageLoad", "UNVERIFIED");
        if (error != null) event.put("exceptionClass", error.getClass().getName());
        log("PAGE_NAVIGATION " + toJson(event), error);
    }

    private static String detectActiveModelMode() {
        if (session == null) {
            return "no_session";
        }
        Model model;
        try {
            model = session.GetActiveModel();
        } catch (Throwable detectionError) {
            log("active model detection failed", detectionError);
            return "unknown";
        }
        if (model == null) {
            return "no_model";
        }
        ModelType type;
        try {
            type = model.GetType();
        } catch (Throwable typeError) {
            log("active model type detection failed", typeError);
            return "unknown";
        }
        if (type == ModelType.MDL_ASSEMBLY) {
            return "assembly";
        }
        if (type == ModelType.MDL_DRAWING) {
            return "drawing";
        }
        if (type == ModelType.MDL_PART) {
            try {
                WSolid solid = wrapAsWSolid(model);
                SolidBody body = solid.GetDefaultBody();
                if (body == null) return "unknown";
                if (body.IsSheetmetal()) {
                    return "sheetmetal";
                }
            } catch (Throwable sheetmetalCheckIgnored) {
                log("EXCEPTION: MODE_DETECTION_FAILED; returning unknown", sheetmetalCheckIgnored);
                return "unknown";
            }
            return "solid_part";
        }
        return "other";
    }

    private static String guardedPageUrl(String commandId, String baseUrl) {
        if (ABOUT_COMMAND.equals(commandId)) return baseUrl; // S1不需要读取模型或包装WFC对象。
        String mode = detectActiveModelMode();
        if (RENAME_COMMAND.equals(commandId) && !"assembly".equals(mode)) {
            return appendPageQuery(baseUrl, "guard", "need_assembly", "modelMode", mode);
        }
        if (SMT_COMMAND.equals(commandId)) {
            if ("sheetmetal".equals(mode)) {
                return appendPageQuery(baseUrl, "state", "sheetmetal", "modelMode", mode);
            }
            if (!"solid_part".equals(mode)) {
                return appendPageQuery(baseUrl, "guard", "need_part", "modelMode", mode);
            }
            return appendPageQuery(baseUrl, "modelMode", mode);
        }
        return baseUrl;
    }

    private static String appendPageQuery(String baseUrl, String... pairs) {
        try {
            URL source = new URL(baseUrl);
            StringBuilder query = new StringBuilder(source.getQuery() == null ? "" : source.getQuery());
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
            }
            String result = source.getProtocol() + "://" + source.getHost()
                    + (source.getPort() == -1 ? "" : ":" + source.getPort()) + source.getPath();
            if (query.length() > 0) {
                result += "?" + query;
            }
            return result;
        } catch (Exception queryError) {
            log("guard query assembly failed; using base page url", queryError);
            return baseUrl;
        }
    }

    private static final class ConversionFailure extends Exception {
        final String code;
        ConversionFailure(String code, String message) { super(message); this.code = code; }
        ConversionFailure(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
    }

    /** Only installed as a native UICommand listener; never submitted to a background executor. */
    private static final class SmtConvertListener extends DefaultUICommandActionListener {
        private boolean enabled;

        @Override
        public void OnCommand() {
            long started = System.nanoTime();
            Map<String,Object> trace = new LinkedHashMap<>();
            trace.put("requestId", java.util.UUID.randomUUID().toString());
            trace.put("mode", "driving");
            trace.put("model", "UNREAD");
            trace.put("modelType", "UNREAD");
            trace.put("solidAdapterRoute", "NOT_ATTEMPTED");
            trace.put("sessionAdapterRoute", "NOT_ATTEMPTED");
            trace.put("thicknessInputOutcome", "UNCLASSIFIED");
            trace.put("nativeDialogObservation", "NOT_OBSERVABLE_FROM_RETURN_API; external_test_evidence_required");
            trace.put("unitGate", "UNIT_GATE_UNKNOWN_REQUIRED");
            trace.put("modelMayHaveChanged", false);
            trace.put("saved", false);
            trace.put("thread", Thread.currentThread().getName());
            boolean acquired = false;
            nativeAudit(trace, "command", "started");
            try {
                if (session == null) throw new ConversionFailure("NO_SESSION", "插件会话未初始化");
                if (!enabled || !active) throw new ConversionFailure("INACTIVE", "转换命令未启用");
                if (converting || opening) throw new ConversionFailure("BUSY", "已有命令正在处理");
                converting = true;
                acquired = true;
                Map<String,Object> result = convertPreselectedPlane(trace);
                if (result == null) {
                    trace.put("completionClass", "INPUT_CANCELLED_CREATE_NOT_DISPATCHED");
                    showConversionMessage(trace, "P1_6A_CANCELLED", "板厚输入返回null；未提交创建");
                    return;
                }
                showConversionMessage(trace, "P1_6A_SUCCESS", "转换回读完成；未保存；特征=" + result.get("featureId"));
            } catch (Throwable error) {
                String code = error instanceof ConversionFailure ? ((ConversionFailure) error).code : "UNEXPECTED_ERROR";
                trace.put("errorCode", code);
                if (!trace.containsKey("failedPhase")) trace.put("failedPhase", code);
                Throwable cause = error.getCause() == null ? error : error.getCause();
                trace.put("exceptionClass", cause.getClass().getName());
                trace.put("exceptionMessage", cause.getMessage());
                if (!Boolean.TRUE.equals(trace.get("modelMayHaveChanged"))) trace.put("completionClass", "CREATE_NOT_DISPATCHED");
                else if (!Boolean.TRUE.equals(trace.get("featureCreated"))) trace.put("completionClass", "CREATE_DISPATCHED_RESULT_UNVERIFIED");
                else if (!Boolean.TRUE.equals(trace.get("regenerationReturned"))) trace.put("completionClass", "FEATURE_RETURNED_REGEN_UNVERIFIED");
                else if (Boolean.FALSE.equals(trace.get("sheetmetalReadback"))) trace.put("completionClass", "REGEN_RETURNED_NOT_SHEETMETAL");
                else if (!Boolean.TRUE.equals(trace.get("sheetmetal"))) trace.put("completionClass", "REGEN_RETURNED_SHEETMETAL_UNVERIFIED");
                else trace.put("completionClass", "SHEETMETAL_CONFIRMED_FEATURE_INFO_INCOMPLETE");
                conversionLog(trace, "EXCEPTION: " + code, error);
                String feedback = Boolean.TRUE.equals(trace.get("modelMayHaveChanged")) ? "P1_6A_INSPECT" : "P1_6A_FAILED";
                showConversionMessage(trace, feedback, code + "：" + error.getMessage());
            } finally {
                if (acquired) converting = false;
                trace.put("elapsedMs", (System.nanoTime() - started) / 1_000_000L);
                nativeAudit(trace, "command", "finished");
            }
        }
    }

    /** Java identity bookkeeping only; never call SDK equals() for registration tracking. */
    private static void trackCommand(UICommand command, DefaultUICommandActionListener listener) {
        if (command == null || listener == null || commands.size() != listeners.size()) {
            throw new IllegalStateException("Invalid command/listener tracking state");
        }
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) == command || listeners.get(i) == listener) {
                if (commands.get(i) == command && listeners.get(i) == listener) return;
                throw new IllegalStateException("Command/listener already tracked with another pair");
            }
        }
        int index = commands.size();
        commands.add(command);
        try { listeners.add(listener); }
        catch (RuntimeException | Error error) {
            commands.remove(index);
            throw error;
        }
    }

    private static void untrackCommand(UICommand command, DefaultUICommandActionListener listener) {
        if (commands.size() != listeners.size()) throw new IllegalStateException("Unpaired command/listener lists");
        for (int i = 0; i < commands.size(); i++) {
            if (commands.get(i) == command || listeners.get(i) == listener) {
                if (commands.get(i) != command || listeners.get(i) != listener) {
                    throw new IllegalStateException("Cannot remove a mismatched command/listener pair");
                }
                commands.remove(i);
                listeners.remove(i);
                return;
            }
        }
    }

    /** Designate first; only completed registrations enter the normal detach list. */
    private static void registerConversionCommand() {
        SmtConvertListener listener = null;
        UICommand command = null;
        try {
            listener = new SmtConvertListener();
            command = createUiCommand(SMT_CONVERT_COMMAND, MESSAGES, "SISW_SMT_CONVERT_LABEL", "SISW_SMT_CONVERT_HELP", "SISW_SMT_CONVERT_DESC", listener);
            if (command == null) throw new IllegalStateException("UICreateCommand returned null");
            designateUiCommand(SMT_CONVERT_COMMAND, MESSAGES, command, "SISW_SMT_CONVERT_LABEL", "SISW_SMT_CONVERT_HELP", "SISW_SMT_CONVERT_DESC");
            try {
                command.SetIcon(SMT_ICON);
                uiRegistrationAudit(SMT_CONVERT_COMMAND, MESSAGES, "SISW_SMT_CONVERT_LABEL", "SISW_SMT_CONVERT_HELP", "SISW_SMT_CONVERT_DESC", "SetIcon", "RETURNED", null);
            } catch (Throwable iconError) {
                uiRegistrationAudit(SMT_CONVERT_COMMAND, MESSAGES, "SISW_SMT_CONVERT_LABEL", "SISW_SMT_CONVERT_HELP", "SISW_SMT_CONVERT_DESC", "SetIcon", "THROWN", iconError);
                log("6A icon unavailable; native command remains registered", iconError);
            }
            trackCommand(command, listener);
            listener.enabled = true;
            log("native conversion command registered: " + SMT_CONVERT_COMMAND
                    + "; trackedPairs=" + commands.size() + "; browser bridge remains disabled", null);
        } catch (Throwable registrationError) {
            if (listener != null) listener.enabled = false;
            log("EXCEPTION: CONVERT_COMMAND_REGISTER_FAILED; three existing page commands remain available", registrationError);
            if (command != null && listener != null) {
                boolean removed = false;
                try {
                    command.RemoveActionListener(listener);
                    removed = true;
                } catch (Throwable detachError) {
                    log("EXCEPTION: PARTIAL_CONVERT_DETACH_FAILED; disabled listener needs a stop-time retry", detachError);
                }
                try {
                    if (removed) {
                        untrackCommand(command, listener);
                        log("6A partial registration cleaned; no stop-time duplicate detach; trackedPairs=" + commands.size(), null);
                    } else {
                        trackCommand(command, listener);
                        log("6A disabled partial registration retained for stop-time cleanup; trackedPairs=" + commands.size(), null);
                    }
                } catch (Throwable trackingError) {
                    log("EXCEPTION: CONVERT_TRACKING_FAILED; restart Creo before reloading", trackingError);
                }
            }
        }
    }

    /** Diagnostic lookup only: missing pointer metadata must not change conversion validation/result. */
    private static void recordNativeObject(Map<String,Object> trace, String key, Object object) {
        trace.remove(key + "Pointer");
        trace.remove(key + "PointerUnavailable");
        trace.remove(key + "PointerErrorClass");
        trace.remove(key + "Class");
        trace.put(key + "Present", object != null);
        if (object == null) return;
        trace.put(key + "Class", object.getClass().getName());
        try { trace.put(key + "Pointer", "0x" + Long.toUnsignedString(nativePointer(object), 16)); }
        catch (Throwable diagnosticError) {
            trace.put(key + "PointerUnavailable", true);
            trace.put(key + "PointerErrorClass", diagnosticError.getClass().getName());
            Object previous = trace.get("diagnosticPointerFailedKeys");
            trace.put("diagnosticPointerFailedKeys", previous == null ? key : previous + "," + key);
            conversionLog(trace, "EXCEPTION: DIAGNOSTIC_POINTER_READ_FAILED; field=" + key, diagnosticError);
        }
    }

    private static long nativePointer(Object object) throws Throwable {
        if (object == null) throw new IllegalArgumentException("Null native object");
        Object value = object.getClass().getMethod("getptr").invoke(object);
        if (!(value instanceof Number) || ((Number) value).longValue() == 0L) {
            throw new IllegalStateException("Native object pointer unavailable");
        }
        return ((Number) value).longValue();
    }

    /** Type guard first; otherwise use only the two adapter constructors supplied in javap-key. */
    private static WSolid wrapAsWSolid(Model model) throws ConversionFailure {
        return wrapAsWSolid(model, null);
    }

    private static WSolid wrapAsWSolid(Model model, Map<String,Object> trace) throws ConversionFailure {
        if (model instanceof WSolid existing) {
            if (trace != null) {
                trace.put("solidAdapterRoute", "already_WSolid");
                recordNativeObject(trace, "solidView", existing);
            }
            return existing;
        }
        String className = model == null ? "null" : model.getClass().getName();
        String modelType = "unknown";
        try { if (model != null) modelType = String.valueOf(model.GetType()); }
        catch (Throwable typeError) { conversionLog(trace, "EXCEPTION: WRAP_MODEL_TYPE_READ_FAILED; class=" + className, typeError); }
        IllegalStateException chain = new IllegalStateException("WFC adaptation failed; class=" + className + "; type=" + modelType);
        final long pointer;
        try { pointer = nativePointer(model); }
        catch (Throwable pointerError) {
            conversionLog(trace, "EXCEPTION: WRAP_POINTER_FAILED; source=" + className + "; type=" + modelType, pointerError);
            throw new ConversionFailure("WRAP_SOLID_FAILED", "无法取得当前零件的原生对象指针", pointerError);
        }
        for (String adapterName : new String[] {
                "com.ptc.wfc.Implementation.wfcPart$WPart",
                "com.ptc.wfc.Implementation.wfcSolid$WSolid" }) {
            try {
                java.lang.reflect.Constructor<?> constructor = Class.forName(adapterName).getDeclaredConstructor(long.class);
                constructor.setAccessible(true);
                Object wrapper = constructor.newInstance(pointer);
                if (!(wrapper instanceof WSolid solid)) throw new IllegalStateException("Adapter does not implement WSolid");
                if (trace != null) {
                    trace.put("solidAdapterRoute", adapterName);
                    recordNativeObject(trace, "solidView", solid);
                }
                conversionLog(trace, "WFC solid view acquired; source=" + className + "; type=" + modelType + "; adapter=" + adapterName, null);
                return solid;
            } catch (Throwable adapterError) {
                chain.addSuppressed(adapterError);
                conversionLog(trace, "EXCEPTION: WFC_ADAPTER_ATTEMPT_FAILED; source=" + className + "; type=" + modelType + "; adapter=" + adapterName, adapterError);
            }
        }
        throw new ConversionFailure("WRAP_SOLID_FAILED", "无法将当前零件适配为WFC实体，详见日志中的适配器异常链", chain);
    }

    /** Reuse the baseline WSession adapter; never register a browser listener. */
    private static void ensureWfcSession(Map<String,Object> trace) throws ConversionFailure {
        if (wfcSession != null) {
            trace.put("sessionAdapterRoute", "cached");
            recordNativeObject(trace, "wfcSession", wfcSession);
            return;
        }
        if (session == null) throw new ConversionFailure("NO_SESSION", "插件会话未初始化，请重启 Creo");
        try {
            if (session instanceof WSession existing) {
                wfcSession = existing;
                trace.put("sessionAdapterRoute", "already_WSession");
            } else {
                wfcSession = createWfcSession(session);
                trace.put("sessionAdapterRoute", "baseline_reflection_WSession");
            }
            if (wfcSession == null) throw new IllegalStateException("Null WSession wrapper");
            recordNativeObject(trace, "wfcSession", wfcSession);
        } catch (Throwable sessionError) {
            wfcSession = null;
            throw new ConversionFailure("WRAP_SESSION_FAILED", "WFC会话包装失败，未创建特征", sessionError);
        }
    }

    private static boolean sameModel(Model left, Model right) throws Throwable {
        return left != null && right != null && (left == right || nativePointer(left) == nativePointer(right));
    }

    private static void verifyFaceInDefaultBody(WSolid solid, SolidBody body, Surface face, Map<String,Object> trace) throws Throwable {
        trace.put("faceOwnershipRoute", "OWNERSHIP_DIAGNOSTIC_UNAVAILABLE");
        trace.put("faceOwnershipRisk", "UNVERIFIED; surface_ID_match_is_not_native_owner_proof");
        boolean nativeOwnerMatched = false;
        try {
            // Optional INTERNAL implementation method, never a public WSolid compile-time call.
            java.lang.reflect.Method method = solid.getClass().getMethod("GetSurfaceSolidBody", Surface.class);
            method.setAccessible(true);
            Object nativeBody = method.invoke(solid, face);
            recordNativeObject(trace, "nativeFaceOwner", nativeBody);
            if (nativeBody == null) throw new IllegalStateException("Native owner returned null");
            boolean identityEqual = nativeBody == body;
            long ownerPointer = identityEqual ? 0L : nativePointer(nativeBody);
            long bodyPointer = identityEqual ? 0L : nativePointer(body);
            if (!identityEqual && ownerPointer != bodyPointer) {
                trace.put("faceOwnershipRoute", "NATIVE_SOLID_BODY_CALLBACK");
                trace.put("faceOwnershipRisk", "NATIVE_OWNER_MISMATCH");
                throw new ConversionFailure("SELECTED_FACE_OTHER_BODY", "原生owner回读与默认体不一致");
            }
            nativeOwnerMatched = true;
            trace.put("faceOwnershipRoute", "NATIVE_SOLID_BODY_CALLBACK");
            trace.put("faceOwnershipRisk", "INTERNAL_API_RUNTIME_EVIDENCE; not_public_owner_contract");
        } catch (ConversionFailure mismatch) {
            throw mismatch;
        } catch (Throwable optionalOwnerError) {
            trace.put("faceOwnershipDiagnosticError", optionalOwnerError.getClass().getName());
            conversionLog(trace, "OWNERSHIP_DIAGNOSTIC_UNAVAILABLE; falling back to surface ID", optionalOwnerError);
        }
        if (!face.GetIsVisible()) throw new ConversionFailure("SELECTED_FACE_INVALID", "预选曲面当前不可见，请重新选择");
        Surfaces surfaces = body.ListSurfaces();
        if (surfaces != null) {
            for (int i = 0; i < surfaces.getarraysize(); i++) {
                Surface candidate = surfaces.get(i);
                if (candidate != null && candidate.GetId() == face.GetId()) {
                    recordNativeObject(trace, "listedBodySurface", candidate);
                    Object selectedPointer = trace.get("selectedFacePointer");
                    Object listedPointer = trace.get("listedBodySurfacePointer");
                    if (selectedPointer != null && listedPointer != null) {
                        trace.put("selectedAndListedPointerEqual", selectedPointer.equals(listedPointer));
                    }
                    trace.put("faceMembershipProof", "default_body_surface_ID_match");
                    if (!nativeOwnerMatched) trace.put("faceOwnershipRoute", "SURFACE_ID_FALLBACK");
                    // ID/pointer equality alone does not establish native ownership.
                    return;
                }
            }
        }
        throw new ConversionFailure("SELECTED_FACE_OTHER_BODY", "预选面不属于当前默认实体体，6A不处理其他实体体");
    }

    /** Sole feature-writing call chain, reached synchronously only from SmtConvertListener.OnCommand. */
    private static Map<String,Object> convertPreselectedPlane(Map<String,Object> trace) throws Throwable {
        String phase = "READ_MODEL_FAILED";
        Session owner = session;
        WSolid solid = null;
        try {
            if (owner == null) throw new ConversionFailure("NO_SESSION", "插件会话未初始化，请重启 Creo");
            Model model = owner.GetActiveModel();
            if (model == null) throw new ConversionFailure("NO_MODEL", "请先打开普通零件");
            trace.put("modelType", String.valueOf(model.GetType()));
            trace.put("model", model.GetFileName());
            if (model.GetType() != ModelType.MDL_PART) {
                throw new ConversionFailure("NOT_PART", "当前只支持普通零件 .prt；装配、工程图及其他类型不能执行6A");
            }
            String modelName = model.GetFileName();
            if (modelName == null || !modelName.toLowerCase(java.util.Locale.ROOT).endsWith(".prt")) {
                throw new ConversionFailure("NOT_PART", "当前模型文件不是 .prt 普通零件");
            }
            trace.put("model", modelName);
            trace.put("unitCheck", "not_automatic; operator_must_confirm_mm");
            recordNativeObject(trace, "modelObject", model);
            phase = "WRAP_SOLID_FAILED";
            solid = wrapAsWSolid(model, trace);
            if (solid == null) throw new ConversionFailure("WRAP_SOLID_FAILED", "WFC实体视图为空");
            nativeAudit(trace, "solid_adapter", "acquired");
            phase = "READ_BODY_FAILED";
            SolidBody body = solid.GetDefaultBody();
            recordNativeObject(trace, "defaultBody", body);
            if (body == null) throw new ConversionFailure("NO_BODY", "未找到默认实体体");
            if (body.IsSheetmetal()) throw new ConversionFailure("ALREADY_SHEETMETAL", "当前已是钣金，无需重复转换");

            phase = "SELECTED_FACE_INVALID";
            com.ptc.pfc.pfcSelect.SelectionBuffer buffer = owner.GetCurrentSelectionBuffer();
            com.ptc.pfc.pfcSelect.Selections selected = buffer == null ? null : buffer.GetContents();
            int count = selected == null ? 0 : selected.getarraysize();
            if (count == 0) throw new ConversionFailure("NO_SELECTED_FACE", "请先在图形区选择1个平面作为驱动面");
            if (count != 1) throw new ConversionFailure("TOO_MANY_FACES", "请只选择1个驱动平面，不要混选其他对象");
            Selection selection = selected.get(0);
            if (selection == null) throw new ConversionFailure("SELECTED_FACE_INVALID", "预选对象为空，请重新选择");
            if (!(selection.GetSelItem() instanceof Surface face)) {
                throw new ConversionFailure("SELECTED_FACE_INVALID", "请选择曲面，不要选择基准面、边、点或坐标系");
            }
            recordNativeObject(trace, "selectedFace", face);
            SurfaceType selectedType = face.GetSurfaceType();
            trace.put("selectedFaceId", face.GetId());
            trace.put("selectedFaceType", String.valueOf(selectedType));
            if (selectedType != SurfaceType.SURFACE_PLANE) {
                throw new ConversionFailure("SELECTED_NOT_PLANE", "6A暂只支持预选平面，请重新选择平面");
            }
            if (!sameModel(model, selection.GetSelModel())) {
                throw new ConversionFailure("SELECTED_FACE_OTHER_MODEL", "预选面不属于当前激活零件，请重新选择");
            }
            verifyFaceInDefaultBody(solid, body, face, trace);
            double area = face.EvalArea();
            if (!Double.isFinite(area) || area <= 0.0) throw new ConversionFailure("SELECTED_FACE_INVALID", "预选平面面积无效");
            trace.put("selectedFaceArea", area);
            nativeAudit(trace, "selection", "validated");

            phase = "UNIT_GATE_FAILED";
            requireConfirmedMillimetres(owner, trace);
            phase = "THICKNESS_INPUT_FAILED";
            if (!showConversionMessage(trace, "P1_6A_THICKNESS", "请输入0.1–20板厚；不保存模型")) {
                throw new ConversionFailure("UI_PROMPT_FAILED", "状态行提示失败，未创建特征");
            }
            // Signature supplies parameter types only: native bound/prompt semantics need local validation.
            // NATIVE_DIALOG_BLOCKED requires external observation: this call exposes only return/throw.
            Double entered;
            try {
                entered = owner.UIReadRealMessage(MIN_THICKNESS, MAX_THICKNESS);
            } catch (Throwable inputError) {
                trace.put("thicknessInputOutcome", "THROWN_EXCEPTION");
                trace.put("thicknessInputExceptionClass", inputError.getClass().getName());
                trace.put("thicknessInputExceptionMessage", inputError.getMessage());
                throw new ConversionFailure("THICKNESS_INPUT_FAILED", "板厚输入抛出异常，未提交创建", inputError);
            }
            if (entered == null) {
                trace.put("thicknessInputOutcome", "CANCEL_NULL_RETURNED");
                return null;
            }
            if (!Double.isFinite(entered) || entered <= 0.0 || entered < MIN_THICKNESS || entered > MAX_THICKNESS) {
                trace.put("thicknessInputOutcome", "INVALID_RANGE_REJECTED");
                // JSON cannot encode NaN/Infinity as numbers.
                trace.put("rejectedThicknessText", String.valueOf(entered));
                throw new ConversionFailure("INVALID_THICKNESS", "板厚必须为0.1–20的有限正数");
            }
            trace.put("thicknessInputOutcome", "VALID_FINITE_POSITIVE");
            double thickness = entered;
            trace.put("thickness", thickness);
            trace.put("thicknessUnit", "model_length_unit; 6A prerequisite: mm");
            phase = "MODEL_RECHECK_FAILED";
            if (!active || owner != session || !sameModel(model, owner.GetActiveModel())) {
                throw new ConversionFailure("MODEL_CHANGED", "输入期间激活模型或插件状态已变化，未创建特征");
            }
            phase = "READ_BODY_FAILED";
            body = solid.GetDefaultBody();
            recordNativeObject(trace, "defaultBodyAfterInput", body);
            if (body == null) throw new ConversionFailure("NO_BODY", "输入后未找到默认实体体");
            if (body.IsSheetmetal()) throw new ConversionFailure("ALREADY_SHEETMETAL", "当前已是钣金，停止重复转换");
            phase = "SELECTED_FACE_INVALID";
            verifyFaceInDefaultBody(solid, body, face, trace);

            phase = "UNIT_GATE_FAILED";
            if (!"UNIT_GATE_CONFIRMED_MM".equals(trace.get("unitGate"))) {
                throw new ConversionFailure("UNIT_GATE_UNKNOWN_REQUIRED", "单位未人工确认，未构造元素树");
            }
            phase = "TREE_CREATE_FAILED";
            trace.put("materialSidePolicy", "OMITTED_PENDING_RUNTIME_EVIDENCE");
            trace.put("materialSideRisk", "UNVERIFIED");
            Selection bodySelection = pfcSelect.CreateModelItemSelection(body, null);
            Selection faceSelection = pfcSelect.CreateModelItemSelection(face, null);
            if (bodySelection == null || faceSelection == null) throw new ConversionFailure("TREE_CREATE_FAILED", "无法建立真实体/面的选择对象");
            Elements elements = Elements.create();
            if (elements == null) throw new ConversionFailure("TREE_CREATE_FAILED", "元素集合工厂返回空对象");
            addElement(elements, wfcElemIds.PRO_E_FEATURE_TREE, null, 0);
            addElement(elements, wfcElemIds.PRO_E_FEATURE_TYPE,
                    pfcArgument.CreateIntArgValue(FeatureType.FEATTYPE_DATUM_SURFACE.getValue()), 1);
            addElement(elements, wfcElemIds.PRO_E_STD_FEATURE_NAME, pfcArgument.CreateStringArgValue("SMT_CONV_DRIVING"), 1);
            addElement(elements, wfcElemIds.PRO_E_DTM_SRF_TYPE, pfcArgument.CreateIntArgValue(15), 1);
            addElement(elements, wfcElemIds.PRO_E_SMT_CONV_BODY, pfcArgument.CreateSelectionArgValue(bodySelection), 1);
            addElement(elements, wfcElemIds.PRO_E_SMT_DRV_SURF, pfcArgument.CreateSelectionArgValue(faceSelection), 1);
            addElement(elements, wfcElemIds.PRO_E_STD_SMT_THICKNESS, pfcArgument.CreateDoubleArgValue(thickness), 1);
            ensureWfcSession(trace);
            trace.put("drivingNodeCount", elements.getarraysize());
            nativeAudit(trace, "element_tree", "dispatching");
            ElementTree tree = wfcSession.CreateElementTree(elements);
            recordNativeObject(trace, "elementTree", tree);
            if (tree == null) throw new ConversionFailure("TREE_CREATE_FAILED", "元素树创建返回空对象");
            nativeAudit(trace, "element_tree", "created");
            phase = "CREATE_OPTIONS_FAILED";
            FeatCreateOptions options = FeatCreateOptions.create();
            if (options == null) throw new ConversionFailure("TREE_CREATE_FAILED", "创建选项工厂返回空对象");
            options.append(FeatCreateOption.FEAT_CR_NO_OPTS);
            trace.put("createOption", "FEAT_CR_NO_OPTS; semantics_pending_runtime");
            trace.put("createOptionBasis", "retain_baseline; no_evidence_to_change_failure_or_undo_semantics");
            phase = "REGEN_INSTRUCTIONS_FAILED";
            WRegenInstructions regen = wfcSolidInstructions.WRegenInstructions_Create();
            if (regen == null) throw new ConversionFailure("TREE_CREATE_FAILED", "重生成指令工厂返回空对象");
            phase = "CREATE_FEATURE_FAILED";
            trace.put("featureReturnState", "DISPATCH_INTENT");
            nativeAudit(trace, "feature_create", "dispatching");
            WFeature feature;
            trace.put("modelMayHaveChanged", true);
            try { feature = solid.WCreateFeature(tree, options, regen); }
            catch (Throwable createError) {
                trace.put("featureReturnState", "THROWN_EXCEPTION");
                throw createError;
            }
            trace.put("featureReturnState", feature == null ? "null" : "object_returned");
            recordNativeObject(trace, "createdFeature", feature);
            if (feature == null) throw new ConversionFailure("CREATE_FEATURE_FAILED", "创建接口未返回特征，请检查模型状态");
            trace.put("featureCreated", true);
            readFeatureMetadata(feature, trace);
            nativeAudit(trace, "feature_create", "returned");
            phase = "REGEN_FAILED";
            nativeAudit(trace, "regenerate", "dispatching");
            solid.WRegenerate(regen);
            trace.put("regenerationReturned", true);
            nativeAudit(trace, "regenerate", "returned");
            phase = "STATUS_READ_FAILED";
            nativeAudit(trace, "status_read", "dispatching");
            SolidBody convertedBody = solid.GetDefaultBody();
            recordNativeObject(trace, "bodyAfterRegeneration", convertedBody);
            Boolean sheetmetal = convertedBody == null ? null : convertedBody.IsSheetmetal();
            trace.put("sheetmetalReadback", sheetmetal);
            if (!Boolean.TRUE.equals(sheetmetal)) {
                throw new ConversionFailure("STATUS_NOT_SHEETMETAL", "创建/重生成已返回，但未回读到钣金状态，请检查特征树");
            }
            trace.put("sheetmetal", true);
            phase = "FEATURE_RESULT_READ_FAILED";
            if (!readFeatureMetadata(feature, trace)) {
                throw new ConversionFailure("FEATURE_RESULT_READ_FAILED", "钣金状态为真，但特征身份或状态回读不完整");
            }
            trace.put("completionClass", "VERIFIED_CONVERSION_NOT_SAVED");
            nativeAudit(trace, "result_read", "verified");
            return new LinkedHashMap<>(trace);
        } catch (ConversionFailure failure) {
            trace.put("failedPhase", phase);
            if (Boolean.TRUE.equals(trace.get("modelMayHaveChanged"))) readFailedFeatureChildren(solid, trace);
            throw failure;
        } catch (Throwable error) {
            trace.put("failedPhase", phase);
            if (Boolean.TRUE.equals(trace.get("modelMayHaveChanged"))) readFailedFeatureChildren(solid, trace);
            throw new ConversionFailure(phase, "原生步骤未完成，请查看jlink.log中的错误阶段与异常链", error);
        }
    }

    private static void addElement(Elements elements, int id, ArgValue value, int level) throws Exception {
        Element element = com.ptc.wfc.wfcElementTree.wfcElementTree.Element_Create(id, value, level);
        elements.append(element);
    }

    private static void nativeAudit(Map<String,Object> trace, String stage, String outcome) {
        Map<String,Object> event = new LinkedHashMap<>(trace);
        event.put("timestamp", Instant.now().toString());
        event.put("stage", stage);
        event.put("outcome", outcome);
        event.put("saved", false);
        log("6A_AUDIT " + toJson(event), null);
    }

    /** No modal null-options call; zero-substitution resource messages only. */
    private static boolean showConversionMessage(Map<String,Object> trace, String key, String summary) {
        conversionLog(trace, "6A feedback: " + summary, null);
        if (session == null) {
            trace.put("feedbackOutcome", "SESSION_UNAVAILABLE");
            return false;
        }
        try {
            // No substitution arguments: no undocumented stringseq factory is assumed.
            // Resource discovery/encoding and null argument-list acceptance require local validation.
            session.UIDisplayMessage(CONVERSION_MESSAGES, key, null);
            trace.put("feedbackOutcome", "STATUS_LINE_RETURNED");
            trace.put("feedbackKey", key);
            return true;
        } catch (Throwable feedbackError) {
            trace.put("feedbackOutcome", "STATUS_LINE_FAILED");
            trace.put("feedbackExceptionClass", feedbackError.getClass().getName());
            conversionLog(trace, "EXCEPTION: UI_FEEDBACK_FAILED; key=" + key, feedbackError);
            return false;
        }
    }

    /** Every invocation requires a fresh explicit declaration, never cached or automatic. */
    private static void requireConfirmedMillimetres(Session owner, Map<String,Object> trace) throws Throwable {
        trace.put("unitGate", "UNIT_GATE_UNKNOWN_REQUIRED");
        trace.put("unitGateSource", "MANUAL_LITERAL_TOKEN; not_automatic_unit_read");
        if (!showConversionMessage(trace, "P1_6A_UNIT", "人工核对模型长度单位后输入MM；非mm输入NON_MM；其他输入阻断")) {
            throw new ConversionFailure("UI_PROMPT_FAILED", "单位确认提示不可用，未提交创建");
        }
        String answer;
        try {
            // SDK signature: UIReadStringMessage(Boolean). Boolean's exact UI meaning is pending runtime evidence.
            answer = owner.UIReadStringMessage(Boolean.FALSE);
        } catch (Throwable gateError) {
            trace.put("unitGateInputOutcome", "THROWN_EXCEPTION");
            throw new ConversionFailure("UNIT_GATE_UNKNOWN_REQUIRED", "单位确认输入失败，未提交创建", gateError);
        }
        String token = answer == null ? "" : answer.trim().toUpperCase(java.util.Locale.ROOT);
        if ("MM".equals(token)) {
            trace.put("unitGate", "UNIT_GATE_CONFIRMED_MM");
            trace.put("unitGateInputOutcome", "EXPLICIT_MM_DECLARATION");
            nativeAudit(trace, "unit_gate", "manually_confirmed");
            return;
        }
        if ("NON_MM".equals(token)) {
            trace.put("unitGate", "UNIT_GATE_NON_MM_BLOCKED");
            trace.put("unitGateInputOutcome", "EXPLICIT_NON_MM_DECLARATION");
            throw new ConversionFailure("UNIT_GATE_NON_MM_BLOCKED", "人工声明非mm模型，禁止创建");
        }
        trace.put("unitGateInputOutcome", answer == null ? "NULL_RETURNED" : "UNRECOGNISED_TOKEN");
        throw new ConversionFailure("UNIT_GATE_UNKNOWN_REQUIRED", "未取得明确MM确认，禁止创建");
    }

    /** Read each field independently so one unavailable getter does not hide the other evidence. */
    private static boolean readFeatureMetadata(Feature feature, Map<String,Object> trace) {
        boolean complete = true;
        String[] fields = { "featureId", "featureName", "featureFeatType", "featureStatus" };
        String[] methods = { "GetId", "GetName", "GetFeatType", "GetStatus" };
        for (int i = 0; i < fields.length; i++) {
            trace.remove(fields[i]);
            trace.remove(fields[i] + "UnavailableReason");
            try {
                // Public Feature interface, not implementation-only method lookup.
                Object value = Feature.class.getMethod(methods[i]).invoke(feature);
                if (value == null || value.toString().isBlank()) throw new IllegalStateException("Empty metadata");
                if (i == 0 && (!(value instanceof Number) || ((Number) value).longValue() < 0L)) {
                    throw new IllegalStateException("Invalid feature ID");
                }
                trace.put(fields[i], value instanceof Number ? value : value.toString());
            } catch (Throwable metadataError) {
                complete = false;
                trace.put(fields[i] + "UnavailableReason", metadataError.getClass().getName() + ": " + metadataError.getMessage());
                conversionLog(trace, "FEATURE_METADATA_UNAVAILABLE; field=" + fields[i], metadataError);
            }
        }
        trace.put("featureMetadataComplete", complete);
        return complete;
    }

    /** Public API reports CHILDREN of failed features, not an authoritative list of failed parents. */
    private static void readFailedFeatureChildren(WSolid solid, Map<String,Object> trace) {
        trace.put("failedFeatureReadbackRoute", "PUBLIC_ListChildOfFailedFeatures");
        trace.put("failedFeatureReadbackScope", "CHILDREN_OF_FAILED_FEATURES; not_failed_parent_list");
        try {
            if (solid == null) throw new IllegalStateException("Solid unavailable");
            Features children = solid.ListChildOfFailedFeatures();
            if (children == null) {
                trace.put("failedFeatureReadbackOutcome", "NULL_RETURNED; not_evidence_of_no_failures");
                return;
            }
            java.util.List<Map<String,Object>> items = new java.util.ArrayList<>();
            trace.put("failedFeatureChildren", items);
            for (int i = 0; i < children.getarraysize(); i++) {
                Feature child = children.get(i);
                Map<String,Object> item = new LinkedHashMap<>();
                item.put("requestId", trace.get("requestId"));
                item.put("index", i);
                items.add(item);
                recordNativeObject(item, "feature", child);
                if (child != null) readFeatureMetadata(child, item);
                else item.put("unavailableReason", "Null child entry");
            }
            trace.put("failedFeatureReadbackOutcome", "RETURNED; empty_list_does_not_prove_no_failed_features");
        } catch (Throwable diagnosticError) {
            trace.put("failedFeatureReadbackOutcome", "DIAGNOSTIC_FAILED");
            trace.put("failedFeatureReadbackErrorClass", diagnosticError.getClass().getName());
            conversionLog(trace, "FAILED_FEATURE_READBACK_UNAVAILABLE", diagnosticError);
        }
    }

    private static void conversionLog(Map<String,Object> trace, String message, Throwable error) {
        log(message + (trace == null ? "" : "; requestId=" + trace.get("requestId")), error);
    }

    private static String toJson(Object value) {
        StringBuilder out = new StringBuilder();
        appendJson(out, value);
        return out.toString();
    }

    private static void appendJson(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            appendJsonString(out, (String) value);
        } else if (value instanceof Boolean) {
            out.append(value.toString());
        } else if (value instanceof Number) {
            out.append(value.toString());
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) out.append(',');
                first = false;
                appendJsonString(out, String.valueOf(entry.getKey()));
                out.append(':');
                appendJson(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof Iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) out.append(',');
                first = false;
                appendJson(out, item);
            }
            out.append(']');
        } else {
            appendJsonString(out, String.valueOf(value));
        }
    }

    private static void appendJsonString(StringBuilder out, String text) {
        out.append('"');
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    /** 不调用任何 Creo API 的诊断日志；日志失败不影响退出回调。 */
    private static void log(String message, Throwable error) {
        String contextId = message.contains("requestId") ? null : pageRequestId.get();
        String text = Instant.now() + " " + (contextId == null ? "" : "requestId=" + contextId + "; ")
                + message + System.lineSeparator();
        if (error != null) {
            StringWriter buffer = new StringWriter();
            error.printStackTrace(new PrintWriter(buffer));
            text += buffer.toString();
        }
        System.err.print("[BatchRename] " + text);
        try {
            Path folder = Paths.get(System.getProperty("user.home"), "CreoBatchRename");
            Files.createDirectories(folder);
            Files.write(folder.resolve("jlink.log"), text.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // System.err 仍可供 Creo/JVM 日志收集，不递归记录日志错误。
        }
    }
}
