# P1 逐特征失败标志替换第十八步只读候选

2026-11-09，Asia/Shanghai。应用类 p1.controlled.step18.P1PreflightDiagnosticStep18；命令 SISWPreflightStep18；短 dat name=p1s18。独立候选，不自动部署、注册、点击或恢复转换。

## 本轮变更

完全移除整体 WSolid.GetSolidFeatureStatusFlags 调用及 FeatureStatusFlags 集合遍历，改用当前模型已枚举的每个 Feature：GetId、GetStatus、WFeature.GetStatusFlag、FeatureStatusFlag.GetFeatureId 与全部11个布尔字段。默认体/驱动面规则、版本戳/dirty、PREFLIGHT后第一次默认NO确认和厚度输入保持Step17逻辑。通过后只记录输入并结束，无第二写确认、创建/再生/保存/重试路径。

枚举返回有序 featureId→Feature 映射，仅在本次同步回调内保留。每个条目get与首次GetId有START/DONE；首次ID未返回前以featureIndex定位，无法预知尚未取得的ID。后续GetId再次核对不变，GetStatus、WFC视图获取、GetStatusFlag、flag所属ID和各字段均独立START/DONE，携带featureId/index；异常THREW也携带这些字段。每个START均FileChannel.force(true)后才执行动作，失败即不调用；无按ID重新获取/换对象重试。每特征完成写FEATURE_STATE_DONE，成功汇总NATIVE_FAILURE_FLAGS_DONE的source=PER_FEATURE_STATUS_AND_FLAG。

保留5类阻断：GetIsFailed、GetIsInvalid、GetIsUnregenerated、GetIsChildOfFailed、GetIsChildOfExternalFailed任何true；Feature.GetStatus=FEAT_UNREGENERATED亦阻断。null/未知枚举、null flag、flag所属ID不匹配、读异常/无法取得WFeature视图、字段缺失均阻断。正常inactive/suppressed并非失败状态，不额外当作错误放宽或加码；原5失败门仍优先。所有可返回字段读完后一次判门，因而可以完整回收状态，若中途不返回/异常则立即停止。

WFeature.GetStatusFlag是真实SDK接口。若枚举对象已实现WFeature直接使用，否则沿用本系列同句柄视图适配方式，通过实际存在的com.ptc.wfc.Implementation.wfcFeature$WFeature(long)构造并核对句柄，不把句柄写入日志。内部实现适配不等于官方运行契约保证；未获视图则阻断，不回退整体solid flags。SDK编译只证明签名/类型成立，现场稳定性待验证。

Step18日志/一次锁：用户目录 CreoPreflightDiagnosticsStep18/step18-200002100-once.jsonl。成功、取消、阻断、失败都消耗一次点击，不访问/删除/重置Step16或Step17锁。只在主动点击后运行，没有后台线程、看门狗、自动杀进程或模型恢复。

## 证据与限制

任务单报告Step17 requestId=b08acfe2-b704-4988-b8e0-979f577af618：体/面门、版本3071、dirty=false及5个特征ID完成，最后START为整体solid flags，无DONE，发生未响应/严重错误。这是本次用户任务单所述结果；没有附原始JSONL/stderr，云端未独立复核原生崩溃根因。该定位是本轮替换依据，不宣称Step18已修复真机崩溃。

实际otk.jar/Java25无警告编译，7源码/16class/薄JAR，class major61但SDK major69，仍需Java25运行；两次干净编译class散列相同，JAR与散装class字节一致。63项SDK前缀方法引用，整体solid flags及模型创建/再生/保存调用引用为零。104项纯断言（审计/一次控制30、体21、面25、状态门28）通过，无SDK/模型mock；包括featureId上下文START/DONE/THREW配对、START失败不执行动作及异常不重试。没有执行插件、Creo、模型API或GUI。

按docs/one-click-capture.md完成现场一次诊断后，回收新JSONL及相关stderr/jlink。新增逐特征日志是有意提高定位粒度：每字段两次强制写入会增加I/O开销，不应以本步时间直接比较此前预检性能。原10000项数量上限保留，特征数多时日志可较大。本步不新增逐面日志。

包含源码、class、JAR、消息及短名注册模板、静态调用表、测试证据、源补丁、manifest及SHA256SUMS；不含SDK/JDK、模型文件、现场原始日志或截图。
