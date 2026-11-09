# P1 受控转换恢复第二十步候选

2026-11-09，Asia/Shanghai。应用类p1.controlled.step20.P1ControlledConversionStep20；命令SISWControlledConvertStep20；短dat name=p1s20。本包恢复一次现场受控创建尝试，云端未部署、注册、点击或执行模型API。

## 范围与流程

沿用Step19默认体/原驱动面作用域和身份规则、当前普通零件200002100.prt、mm单位、版本/dirty、逐特征PFC状态审计。按本任务限定：初始dirty必须false，当前恰好5个唯一特征且全部FEAT_ACTIVE。Step19曾接受的inactive/suppressed在本步不满足“全ACTIVE”写前条件，故阻断；UNREGENERATED/null/未知/异常仍阻断。不把旧5个ID、版本3071或厚度1.2硬编码为现场值。

顺序：预检 → 第一次默认NO风险确认 → 用户明确输入正有限测试厚度 → 重新核验 → 第二次默认NO最终确认（显示本次厚度和原驱动面ID）→ 重新核验 → 内存元素树准备 → 最后重新核验 → 持久写意图 → 最多一次WCreateFeature → 返回非null后最多一次显式WRegenerate → 六项有限回读 → 结束。未确认、取消、失效上下文、日志失败、创建返回null或异常均停止，不重试，不自动回滚/恢复模型。

确认期间重检活跃模型、文件名、版本、dirty、稳定默认体身份、mm单位、相同5特征ID集合和全ACTIVE、原始捕获选择及稳定面ID。不改用新选择缓冲区、不全局放宽same或跨体按ID找面。内存元素树由Step16候选方案恢复：DATUM_SURFACE、DTM_SRF_TYPE=15、目标体/驱动面选择、明确输入试验厚度和名称P1_STEP20_DRIVING。其运行时有效性待真机创建验证；没有根据离线签名保证钣金转换成功。

每个关键操作有强制写盘START/RETURNED或DONE。WCREATE_FEATURE_START和WREGENERATE_START均writeModel=true；对应WCREATE_FEATURE_RETURNED只记录returnedNull，WREGENERATE_RETURNED只记录返回，不在这些返回摘要里读取其他原生属性。后续CREATE_RETURNED/REGENERATE_RETURNED兼容汇总事件同样不调用getter。因此若创建/再生未返回，最后START可识别最后可见接口，而不被特征回读掩盖。

继续禁用整体solid失败flags、WFeature.GetStatusFlag及FeatureStatusFlag布尔路径；无回退。NATIVE_FAILURE_FLAGS_DONE.source=PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG，fiveFailureFlagsChecked=false/fullFailureFlagCoverage=false始终明确。本步恢复写入不等于恢复原五类检查。

## 回读与结果边界

创建和显式再生返回后，只调用任务列出的created.GetId、GetName、GetFeatType、GetFeatTypeName、GetStatus、GetElementTree(null,FEAT_EXTRACT_NO_OPTS)，每项独立START/DONE。不调用GetFeatSubType、GetStatusFlag、树元素遍历或转换后其他模型查询；错误路径也不再追加原生失败快照。元素树只记录是否返回，不序列化内容、地址或坐标。

有限回读要求ID不在原集合、状态ACTIVE、类型非null、元素树非null；满足时终态CREATE_REGENERATE_RETURNED_LIMITED_READBACK_NOT_SAVED_UNVERIFIED。只表示这些返回记录成立，不证明创建特征已加入模型列表、默认体IsSheetmetal、驱动墙类型/元素、全局厚度或生产可用。可视树检查保持PENDING_OPERATOR_OBSERVATION；失败/未返回可能留下部分修改。

提交意图之后无论返回或异常，提示模型可能已改变，明确“不要保存、不要重试”。本命令没有Save/Backup/导出/删除/Undo等入口，不拦截用户其他保存入口，不自动杀进程、重启或回滚。WCreateFeature可能内部触发再生，约束仅为一个显式WRegenerate调用，不宣称SDK内部总再生次数为1。若调用永久不返回，应用无法保证展示结果对话框，事先确认和现场清单已说明不要保存。

## 独立一次锁与离线验证

用户目录CreoControlledConversionStep20/step20-200002100-once.jsonl为本轮一次日志兼锁；CREATE_NEW、每条force(true)，成功、取消、异常、阻断都不删除。不访问Step19和以前锁，不因重载/换目录自动重置。同步主动命令、静态防重入及本进程一次点击，持久控制还拒绝重复创建/再生阶段；不自动运行模型操作。

实际otk.jar/Java25无警告编译：7源码、16class、薄JAR，class major61、SDK major69，仍需Java25。重复干净编译散列一致、JAR与散装class一致；pfcasync排除，SDK/JDK不随包。71项SDK前缀方法引用，字节码WCreateFeature和WRegenerate各1处，无保存或禁用flag调用。新增调用属于Step16旧构建方案及3项继承getter接口归属别名（Feature.GetFeatType/GetFeatTypeName、ModelItem.GetName），均在静态记录明确列出。

125项纯断言通过：审计/一次控制30、体21、面25、严格PFC状态门20、受控提交状态29；无SDK类路径/模型mock。验证确认顺序、重复阶段拒绝、持久意图/返回顺序、读写START先落盘、START失败不执行动作、writeModel标记和异常不重试。纯状态测试不构成实际模型创建或SDK稳定性测试。

任务单报告Step19 requestId=2687a81e-59ce-41a3-b152-ace0bc7499cd：体/面门通过、版本3071/dirty=false、5特征ACTIVE、第一次确认接受、输入1.2mm、只读终态完成且未保存。本次未附原始JSONL/stderr，以上注明为用户任务单报告，云端未独立复核，也不由一次GetStatus成功推断普遍稳定。

先读docs/one-click-capture.md和docs/audit-boundaries.md。包内有源码/class/JAR、消息/注册模板、补丁、manifest/SHA256SUMS、真实SDK声明及静态/纯测试证据；无SDK/JDK、模型文件、现场原始日志或截图。真机创建/再生/回读均NOT_EXECUTED/UNKNOWN。
