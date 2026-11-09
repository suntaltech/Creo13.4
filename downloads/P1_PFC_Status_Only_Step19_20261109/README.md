# P1 PFC状态门降级第十九步只读候选

2026-11-09，Asia/Shanghai。应用类 p1.controlled.step19.P1PreflightDiagnosticStep19；命令 SISWPreflightStep19；短dat name=p1s19。独立只读诊断候选，未部署、注册或点击。

## 本轮行为

逐特征状态门只使用Feature.GetId与Feature.GetStatus。完全移除WFeature视图适配、GetStatusFlag、FeatureStatusFlag.GetFeatureId、11个布尔flag getter及整体WSolid.GetSolidFeatureStatusFlags路径，无回退。原默认体/驱动面规则、版本戳/dirty、特征枚举及逐特征ID前后审计保留；其他WFC体/面/单位读取沿用，不把此变更解释成整个插件不再使用WFC。

状态规则：FEAT_ACTIVE放行；FEAT_INACTIVE、FEAT_FAMILY_TABLE_SUPPRESSED、FEAT_SIMP_REP_SUPPRESSED、FEAT_PROGRAM_SUPPRESSED、FEAT_SUPPRESSED仅诊断记录，不阻断；FEAT_UNREGENERATED阻断；null/未知枚举、读取异常或枚举ID与当前ID不一致阻断。没有按旧任务单的5个ID硬编码或替代实际读取。FeatureStatus枚举只提供上述状态，不能单独证明不存在failed/invalid/child-of-failed等错误；本轮明确不再检查原5类失败flag。

逐特征GetId/GetStatus的START强制写盘后才执行，DONE包含真实ID/枚举名和数值，异常THREW含operation、原exceptionClass/message、featureId/index。首次枚举ID未返回前只能使用featureIndex定位，返回摘要补ID，后续状态调用均有已知ID。每特征FEATURE_STATE_DONE明确inactiveOrSuppressedDiagnosticOnly和fiveFailureFlagsChecked=false。

状态循环前FEATURE_STATUS_COVERAGE声明降级范围；所有特征通过后NATIVE_FAILURE_FLAGS_DONE明确：
- source=PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG
- statusGateCoverage=PFC_STATUS_ONLY
- wfcStatusFlagEnabled=false
- fiveFailureFlagsChecked=false
- fullFailureFlagCoverage=false

事件名沿用以便整合，但不表示完成旧失败标志检查。PREFLIGHT亦带statusGateSource/fiveFailureFlagsChecked=false/conversionReadinessCertified=false。第一次默认NO确认、消息提示和完成界面均说明覆盖范围。门通过后只进入第一次确认和明确正有限测试厚度输入，记录后结束；没有第二写确认、模型创建/再生/保存或恢复转换资格。

## 单次与验证

用户目录CreoPreflightDiagnosticsStep19/step19-200002100-once.jsonl为本步独立日志兼一次锁。保留Step18及此前锁，不读取/删除/编辑/重置。成功、取消、失败、阻断都消耗一次点击；无后台线程、自动重试、清锁、杀进程或模型恢复。只有现场主动点击才执行。

实际otk.jar/Java25无警告编译：7源码/16class/薄JAR，class major61但SDK major69，仍需Java25。两次干净编译散列一致，JAR与散装class字节一致。50项SDK前缀方法引用，较Step18无新增SDK方法，WFC状态flag/整体solid flags及模型创建/再生/保存引用为零。101项纯逻辑断言（审计/一次控制30、体21、面25、PFC状态门25）通过，无SDK/模型mock。没有执行Creo、插件、模型API或GUI；不认证真机稳定或五类错误不存在。

任务单报告Step18 requestId=7a53455b-0afe-4cbf-81ea-7580c01d913f：枚举ID=[1,3,5,7,69]；featureId=1的GetStatus返回FEAT_ACTIVE，WFeature适配完成，最后START为WFeature.GetStatusFlag且无DONE，发生卡死/严重错误。本次只收到任务单，没有附原始JSONL/stderr，以上为用户报告，未独立复核原生崩溃原因，也不推断所有模型的GetStatus都稳定。

先读docs/one-click-capture.md和docs/audit-boundaries.md。包内包含源码/class/JAR、消息/短名注册模板、manifest/SHA256SUMS、静态SDK调用表、真实SDK声明、测试证据和源补丁，不含SDK/JDK、模型文件、现场日志/截图。本步仍待现场验证全部特征状态及确认/输入能否完成。
