# P1 Preflight 卡点定位第十七步只读候选

2026-11-09，Asia/Shanghai。独立类p1.controlled.step17.P1PreflightDiagnosticStep17，命令SISWPreflightStep17，显示名Read-Only Preflight Diagnostic Step 17。dat name已缩为p1s17（5字符），模板未启用。

本步只定位Step16面门之后的只读预检。默认体/驱动面规则保持不变，不继续放宽same/作用域/ID检查。模型特征创建、显式重生成、元素树提交、保存及转换提交路径均从Step17执行代码移除，静态SDK引用无WCreateFeature/WRegenerate/Save。即使预检通过，第一确认和手动试验厚度输入后也只记录并结束，不出现第二次写提交确认，不执行转换。

每个不同的只读API/同类getter批次都在调用前记录START并FileChannel.force(true)，返回后记录DONE及真实摘要，异常带operation/exceptionClass/message。版本戳、dirty、特征集合获取/数量/条目/ID、失败标志获取/数量/条目和5种状态getter分别有边界；PREFLIGHT内最后两个body/face GetId也有独立边界。审计本身失败则不执行相应调用。

为满足不逐特征高频刷日志，循环改为按getter分批：先取集合成员、再读ID；失败标志先取成员，再分5种getter各扫描。每个批次有独立START/DONE及计数，FEATURE_LIST_ITEM_DONE只写最终汇总，不逐面/特征输出。这样最后START能定位具体API或重复getter批次，不能保证批次内部精确索引；原只读getter顺序因此与Step16交错顺序不同，保留相同类型/唯一ID/上限及失败布尔条件，不冒称完全相同的运行时序或零开销。

日志位于用户目录CreoPreflightDiagnosticsStep17/step17-200002100-once.jsonl。Step17自己的一次点击锁，不读/删/改Step16锁；取消/阻断也消耗，不自动重试或换目录绕过。仅现场主动点击才运行，模块装载不会触发诊断；没有部署、注册或执行Creo。

实际otk.jar/Java25编译无警告，6个源码/15个class/薄JAR，两次干净编译散列一致、JAR字节与class一致。class major61但SDK major69，实际仍需Java25；SDK/JDK不随包，pfcasync不使用。24项审计/单次/文件控制断言、21项默认体、25项驱动面断言通过，共70项，无SDK/模型Mock。审计测试验证先写START再执行、异常记录、START写失败不调用action、一次记录不能重用；这些都不是真机崩溃测试。

实际57项SDK前缀方法引用，无模型创建/再生/保存/删除/导出/宏/配置/射线材料调用。较Step16删除写入与转换后核验方法，新增的字节码接口归属Feature.GetId是旧ModelItem.GetId的继承读取，不是新增模型写。默认体/驱动面规则与Step16源码规范化后逐段一致，其他模型same未全局替换。

任务单报告Step16 requestId=bfdcfb7d-27c5-4e82-9aa5-7fb183c494f2，面/体门通过后出现未响应及dbg_err_crash。此次只收到任务说明，未附该轮原JSONL/截图/完整stderr，因此不宣布具体只读API已经被定位。Step17真正最后START、异常/正常返回和UI输入结果全部NOT_EXECUTED/UNKNOWN。

先读docs/audit-boundaries.md和docs/one-click-capture.md。包内含源码/class/JAR、短名注册/消息模板、manifest、SHA256SUMS、SDK/javap、静态调用表、源补丁及测试。不要为补日志重试转换、清Step16锁或保存/恢复模型。
