# 镜像策略与批量输出评估

## 本轮实现

每个Run构造时只读取一次环境变量`P1_READONLY_GEOMETRY_STDERR_MODE`，不动态切换、不写配置。未设置/空白为REDUCED；strip后不区分大小写的REDUCED/FULL有效。其他值或读取环境权限失败均回退FULL，生命周期增加警告，报告INVALID_VALUE_FALLBACK_FULL，不记录原始非法值。该变量只控制业务JSONL的stderr镜像，不影响生命周期文件/回退stderr或原生命周期与异常堆栈输出。

FULL保留每条业务JSON的原stderr镜像。REDUCED只排除GeometryStderrPolicy.shouldMirror中显式列出的23类高频/大事件：面/UV、采样计划/点/拒绝/质量、池/队列/空间参考、逐候选/域验证/距离见证/分类/SDK目录ITEM，以及TIMING_OBSERVATION_SNAPSHOT。它们仍全部逐条写JSONL并flush。SAMPLE_REJECTED通常是有界采样拒绝，不是READ_EXCEPTION；逐面或逐对风险详情在文件，stderr摘要不能替代文件审核。

所有未列出的事件继续镜像，包括COLLECTION_STARTED、COLLECTION_SUMMARY、READ_EXCEPTION、READ_UNAVAILABLE、DATA_ISSUE、各整阶段摘要、身份事件和未来未知事件。未知新事件默认可见，不能先假设可丢。生命周期start/stop/phase/close及原诊断不做本轮降采样。REDUCED没有关闭所有stderr。

业务emit原顺序保持：构造完整JSON→write→newline→flush→镜像决策/可选stderr.println→原分类旁路观察。不存在从业务文件删除、抽样、缩减XYZ/UV字段或异步队列；镜像策略不读/修改几何字段。每次成功业务flush后才计一次决策，写入失败之前不伪记“已抑制”。

每行保留Step9全部schema/旧字段，collectorStep=STEP_10、独立命令及文件前缀，增加loggingSchema=P1_NATIVE_GEOMETRY_STDERR_POLICY_V1与stderrMode。原TIMING_COMMAND_CONTEXT和TIMING_OBSERVATION_SNAPSHOT及最后COLLECTION_SUMMARY保持。新生命周期LOG_OUTPUT_POLICY_SELECTED记录配置状态与初始策略计数；快照加stderrPolicyBeforeSnapshotOutput；RUN_OBSERVATION_TOTALS加stderrPolicyThroughClose。后者包含快照本身与最终摘要的决策，前者排除它们，与原时序快照自写排除一致。

正常输出时selectionDecisionsAfterSuccessfulBusinessFlush=mirrorSelectedAttempts+stderrSuppressedRecords，计数与实际执行的stderrPrintlnCalls对应。镜像被选中是尝试，不是stderr独立送达认证；发生RuntimeException时可能选择/尝试已计数而该业务emit未完成，应同时看原timing失败计数。生命周期stderr不在业务stderrPrintlnCalls内。

## 批量与开销选择

| 方案 | Step9已测对象 | 本轮决定/边界 |
|---|---|---|
| 精简业务stderr镜像 | 1103.873 ms，6432次 | 实施REDUCED/FULL同一候选对照；不承诺可节省相同耗时 |
| 批量业务flush/写盘 | flush 184.363 ms；write/newline 6.097 ms | 本轮不实施，保持每条flush及错误出现边界；批量会改变中断尾部可读性和失败位置 |
| 异步日志/后台原生 | 未经本任务验证 | 不实施，不引入线程/队列或原生迁移 |
| 降低采样/门禁/改精度预算 | 不属于日志策略 | 不实施 |

27/6432仅为本次旧日志的选择回放，不是固定配额；不同模型、异常、预算或新增事件会改变数量。回放按原Step9文本和LF估计镜像UTF8字节：FULL 10,770,365、REDUCED 34,823，仅用于输出量，不是Step10真实字节数或机器耗时。新头字段及平台换行可不同。原业务文件完整且有坐标，不在交付包重新分发。

减少同步日志开销可能使既有30秒预算内实际访问数量变化；本轮不更改预算/取样算法，但不能在未执行前保证所有模型的样本数逐位一致。仍应比较同模型的核心字段、停止原因和身份，不能将更多样本自动当作模型变化或将耗时降低当等厚证明。
