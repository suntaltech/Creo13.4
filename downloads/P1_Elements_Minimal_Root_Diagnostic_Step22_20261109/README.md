# P1 Elements创建最小定位第二十二步只读候选

2026-11-09，Asia/Shanghai。应用类p1.controlled.step22.P1ElementsMinimalStep22；命令SISWElementsMinimalStep22；短dat name=p1s22。本轮只隔离Elements创建与根元素构造/追加，未部署、注册、点击或执行Creo。

保留Step21预检、两次默认NO确认和明确正有限厚度输入：当前普通零件200002100.prt、mm、初始dirty=false、稳定默认体和原捕获驱动面、版本/dirty、实际恰5特征全ACTIVE及确认间复检。fiveFailureFlagsChecked=false仍明确。输入厚度仅日志记录，不创建厚度元素，不作为根元素值，也不硬编码此前1.2mm。

确认及复检通过后，仅依次执行：
1. CreateModelItemSelection(body)，非空才继续。
2. CreateModelItemSelection(face)，非空才继续。
3. 单独Elements.create一次，返回后立即记录ELEMENTS_CREATE_MINIMAL_DONE，仅摘要是否null，不调用其他SDK getter。
4. 非空时准备根value=null，写ROOT_ELEMENT_VALUE_CREATE_START/DONE，明确ROOT_NULL_VALUE_NO_FACTORY和valueFactoryInvoked=false；不调用任何参数值工厂。
5. 单独Element_Create(PRO_E_FEATURE_TREE,null,0)，写ROOT_ELEMENT_CREATE_START/DONE，非空才继续。
6. 单独elements.append(root)一次，写ROOT_ELEMENT_APPEND_START/DONE，然后立即结束。

不追加其他元素，不调用WSession.CreateElementTree或替代构造方式，不进入创建/再生/保存路径，不恢复Step20/21；整体solid失败flags及WFeature.GetStatusFlag/FeatureStatusFlag布尔读取仍禁用。SDK静态检查确认非根参数工厂和会话建树方法引用为零。

每个START均FileChannel.force(true)成功后才执行对应动作；DONE摘要仅Java数据，不偷偷追加原生getter。根日志携带实际elementId、elementIdName=PRO_E_FEATURE_TREE、elementIndex=0、level=0、argumentKind=NULL_ROOT。根value阶段是明确的Java null记录，不伪装SDK工厂。选择日志用已核验快照ID，不再查询/输出地址。

原外层ELEMENT_TREE_PREPARE_START/DONE保留以便整合，但operation改为PREPARE_MINIMAL_ELEMENTS_AND_ROOT_ONLY，DONE明确elementsAppended=1、sessionElementTreeCreated=false、thicknessAppliedToElement=false。外层DONE不表示创建了会话元素树。成功终态MINIMAL_ROOT_APPENDED_NO_MODEL_WRITE_NO_SAVE仅表示本次最小调用序列返回，不宣称此前卡顿偶发、模型转换成功或生产可用。

Elements.create返回null时，MINIMAL_DONE如实记录returnedNull=true，然后阻断，不构造根。根构造返回null时，ROOT_ELEMENT_CREATE_DONE记录null，随后阻断，不append。异常含operation/原exceptionClass/message和可得根上下文；宿主不返回可能没有THREW/FINAL_RESULT，不补写DONE、不重试。最后未配对START只标识最后可见边界，需结合stderr/jlink和外部时间判断SDK内部原因。

本轮独立一次锁/日志：用户目录CreoElementsMinimalStep22/step22-200002100-once.jsonl。CREATE_NEW，所有结果均不删锁，不访问Step21及更早锁。同步主动命令、静态防重入/本进程一次点击，无自动重试、换目录重置、杀进程、回滚或模型恢复。

实际otk.jar/Java25无警告编译，7源码/16class/薄JAR；class major61，SDK major69，仍需Java25。两次干净编译散列一致，JAR与散装class一致，SDK/JDK不随包，pfcasync排除。54项SDK前缀方法引用，较Step21无新增；模型创建/显式再生/保存、禁用flag、会话建树及非根值工厂引用均为零。109项纯断言通过（审计/单次30、体21、面25、严格PFC门20、诊断确认/元素上下文13），无SDK/模型mock；这不是真机Elements创建或根构造测试。

任务单报告Step21 requestId=2b11e44c-2473-463a-b7d4-736cf97e3b03：体/面选择创建DONE，最后ELEMENTS_CREATE_START无DONE，尚未append或会话建树。本次未附原始JSONL/stderr，以上为用户任务说明，云端未独立复核原生卡点或断言Elements.create必挂。

先读docs/audit-boundaries.md和docs/one-click-capture.md。包内有源码/class/JAR、短名注册/消息模板、manifest/SHA256SUMS、真实SDK声明、静态调用表、源补丁及测试证据；无SDK/JDK、模型、现场原始日志/截图。真实探针结果NOT_EXECUTED/UNKNOWN。后续是否绕开Elements.create或修改根写法，等本轮证据后另行实施，不自动执行下一步。
