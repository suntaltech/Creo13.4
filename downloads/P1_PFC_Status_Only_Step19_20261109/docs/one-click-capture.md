# Step19 一次现场验收

1. 校验manifest/SHA256SUMS和实际加载散列，使用现有实际SDK/许可与Java25。registration/step19.dat.template第一行name p1s19，填写绝对路径；不覆盖Step18注册或Ribbon。本包未自动部署。
2. 同目标当前普通零件200002100.prt、mm、唯一默认体；预选该体一个可见平面。保留默认体/面作用域门，现场实际读取，不硬编码此前体/面/特征ID。保留Step18和更早一次锁，不删除、读取修改、清理或重试旧命令。
3. 只主动点击SISWPreflightStep19一次，日志位于用户目录CreoPreflightDiagnosticsStep19/step19-200002100-once.jsonl。记录点击时间、界面、加载散列和是否未响应；取消、阻断也消耗本步锁，不重复点/换目录/重载绕过。
4. 核对没有WFeature视图/状态flag或整体solid flags调用；现场特征数量按实际枚举核对，任务单报告的5个ID仅用于对照。若状态均符合规则，每个实际特征GetStatus和FEATURE_STATE_DONE应完成；UNREGENERATED/未知/异常/ID变化则停止，日志应能指出featureId/index/operation/reason。
5. 核对NATIVE_FAILURE_FLAGS_DONE.source=PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG、fiveFailureFlagsChecked=false、fullFailureFlagCoverage=false。不能据此写成“完整失败标志检查通过”。若门中途阻断，不补汇总DONE。
6. 全部通过后出现第一次默认NO只读确认，界面说明五类flag未检查。可输入明确正有限试验厚度；随后仅记录并结束，不构造特征、不转换、不再生、不保存。正常对话框等待不要记作卡死。通过也不自动允许恢复Step16转换。
7. 回收新JSONL原件、相关stderr/jlink、加载散列及外部未响应时间。卡住时保留现有记录，不补DONE或再次点击；宿主失效按现场既有事故流程处置，本包不自动杀进程/重启/Undo/擦除/恢复模型。

当前状态：云端离线检查通过，现场全部状态读取、确认/厚度输入及稳定性均NOT_EXECUTED/UNKNOWN。
