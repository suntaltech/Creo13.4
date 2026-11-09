# Step18一次现场验收

1. 核对manifest、SHA256SUMS和实际加载的class或JAR；仍需Java25及原SDK/许可。registration/step18.dat.template首行为name p1s18，填写绝对路径，不覆盖Step17/Ribbon或改成长注册名。模板未部署，云端未点击。
2. 当前目标普通零件200002100.prt、mm、唯一默认体；预选该体一个可见平面。保留原体/面门，不以任务单旧ID替代现场采集值。Step17原锁原样保留，不删除、编辑、清理或重试Step17。
3. 仅主动点击SISWPreflightStep18一次；新日志在用户目录CreoPreflightDiagnosticsStep18/step18-200002100-once.jsonl。不要重复点或换目录/重载绕过锁。记录实际标题、命令、加载散列、点击时间和是否未响应。
4. 核对日志没有整体solid flags调用；5个特征是Step17报告值，现场按实际枚举数量核对，不硬编码5。逐特征FEATURE_STATE_DONE有真实ID/status/flagID/全部字段。失败应明确featureId/reason并停止；异常/未返回看最后featureId/index/operation。尚未读取首个ID时只能按索引定位。
5. 全部通过后进入第一次只读确认（默认NO）和手动测试厚度输入；接受后记录输入并结束，没有转换/再生/保存。正常取消消耗一次锁。无需为补齐5条而越过失败门或重试。
6. 回收该轮JSONL原件、相关stderr/jlink、实际加载散列与外部未响应/恢复时间。若停住，保留已有记录，不补DONE、不重复点击；不将对话框等待误记为崩溃。宿主失效按现场原有事故处理保留证据，本包不自动杀进程/重启/Undo/擦除/恢复。

当前验收状态：NOT_EXECUTED/UNKNOWN。现场是否稳定、所有特征是否通过、UI确认/输入均待回传；通过本次只读门也不自动授予恢复转换资格。
