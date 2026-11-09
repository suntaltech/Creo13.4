# P1 元素树构建定位第二十一步只读候选

2026-11-09，Asia/Shanghai。应用类p1.controlled.step21.P1ElementTreeDiagnosticStep21；命令SISWElementTreeStep21；短dat name=p1s21。只定位内存元素树准备，未部署、注册、点击或执行Creo。

## 本轮变更

从Step20移除WCreateFeature、WRegenerate、写提交状态/意图和转换后回读路径，不只是以开关禁用。整体solid失败flags、WFeature.GetStatusFlag及FeatureStatusFlag布尔读取仍不存在；无模型创建/再生/保存、重试或自动恢复转换。

保留原预检及确认上下文：目标当前普通零件200002100.prt、mm、初始dirty=false、稳定默认体/原捕获驱动面、版本/dirty、当前恰5个唯一特征全ACTIVE。继续注明PFC状态仅限覆盖，fiveFailureFlagsChecked=false；不硬编码旧模型版本/特征ID/厚度。第一次默认NO确认后明确输入正有限试验厚度，复检；第二次默认NO确认明确“仅准备元素树”，复检后仅prepareTree一次，返回后立即记录结束。不进入Step20创建，不清理或重试Step20锁。

prepareTree内部将每个实际准备操作分开：目标体选择、目标面选择、Elements.create；每个元素的参数值工厂、Element_Create和Elements.append；最后WSession.CreateElementTree。ELEMENT_TREE_PREPARE_START/DONE外层边界保留，内部每个START均先写盘并FileChannel.force(true)，再执行对应动作。返回摘要仅Java空值/返回布尔，不调用其他原生getter。

除任务要求的SELECTION_BODY_CREATE、SELECTION_FACE_CREATE、ELEMENTS_CREATE、ELEMENT_APPEND和SESSION_CREATE_ELEMENT_TREE前后标记，还增加ELEMENT_VALUE_CREATE和ELEMENT_CREATE，避免把值创建、元素构造、集合追加混为同一边界。元素日志含elementIndex、实际elementId、elementIdName、level、argumentKind；最后未配对START可指出当前接口及具体元素。参数值工厂位于审计lambda内，不会在START之前隐式求值调用SDK。

7元素结构沿用Step20，仅独立命名P1_STEP21_DRIVING：根PRO_E_FEATURE_TREE；FEATURE_TYPE=DATUM_SURFACE；STD_FEATURE_NAME；DTM_SRF_TYPE=15；SMT_CONV_BODY；SMT_DRV_SURF；STD_SMT_THICKNESS=本次显式输入。根value=null，没有值工厂，不伪造该工厂的完成事件。选择记录使用已核验Java快照ID，不在摘要里再读ID或输出原生对象地址。

## 单次与日志结果

日志兼一次锁位于用户目录CreoElementTreeDiagnosticsStep21/step21-200002100-once.jsonl。CREATE_NEW，取消/阻断/异常/成功均消耗，不删除、不访问Step20和更早锁。同步主动命令、静态防重入/本进程一次点击，无线程、重载绕过、自动重试、杀进程、回滚或模型恢复。

全部正常返回时有SESSION_CREATE_ELEMENT_TREE_DONE和ELEMENT_TREE_PREPARE_DONE，最终ELEMENT_TREE_PREPARED_NO_MODEL_WRITE_NO_SAVE；仅证明本次构造返回。CreateElementTree返回null时，其DONE如实写returnedNull=true，然后阻断，不写成功的外层DONE。任一选择/非根参数/元素返回null亦阻断。异常含原exceptionClass/message和具体元素上下文；宿主崩溃或不返回可能没有异常终态，不能补写DONE。

一次成功不证明Step20卡顿是环境偶发，也不自动恢复转换资格；不同运行的命名、宿主状态和审计时序可能影响表现。最后START是最后可见边界，需与stderr/jlink及外部观察交叉判断，不能仅凭日志边界宣布SDK内部根因。强制日志有I/O开销，本步不做性能结论。

## 离线验证与依据

实际otk.jar/Java25无警告编译：7源码、16class、薄JAR，class major61/SDK major69，仍需Java25。重复干净编译散列一致，JAR与散装class字节一致，pfcasync排除，SDK/JDK不随包。60项SDK前缀方法引用，较Step20无新增SDK方法，模型创建/显式再生/保存及禁用flag引用为零。体/面规则、预检、枚举/PFC门与确认间重检源码规范化后保持Step20一致。

109项纯断言通过：审计/一次控制30、体21、面25、严格PFC状态20、元素诊断控制13。无SDK类路径/模型mock；验证START先实际写盘、START失败不执行动作、异常不重试、elementId/index/level在START/DONE/THREW中配对、第二确认顺序及一次锁不可重用。没有执行SDK模型操作、元素树或GUI，真实卡点仍未知。

任务单报告Step20 requestId=a6ad6e22-ab73-431b-840c-3c6209e2a04a：第二确认后PRECOMMIT_RECHECK_DONE完成，最后START为ELEMENT_TREE_PREPARE_START，无对应DONE且未进入创建。本次只有任务说明，未附该轮原始JSONL/stderr；上述为用户报告，未独立复核完整现场。

包内含源码/class/JAR、短名注册/消息模板、manifest/SHA256SUMS、真实SDK声明、静态调用表、源补丁及纯测试证据；无SDK/JDK、模型、现场原始日志/截图。按docs/one-click-capture.md进行一次现场定位后回传日志。不得由此包自动部署或恢复Step20。
