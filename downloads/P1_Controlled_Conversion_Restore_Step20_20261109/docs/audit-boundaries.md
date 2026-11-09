# Step20 日志与结果解释

requestId/probeId配对START和DONE/THREW，sequence、UTC和单调elapsedNs保留。START必须写完并force(true)后才执行动作；失败则不执行。actionAndStartAuditElapsedNs含审计与Java处理，不是纯JNI计时。读探针异常为PREFLIGHT_PROBE_EXCEPTION，写探针异常为NATIVE_OPERATION_EXCEPTION；原exceptionClass/message、operation、probeId保留。宿主崩溃/永久不返回不保证有THREW或最终结果。

Step19的版本、dirty、特征集合/条目/ID、逐特征PFC状态、PREFLIGHT体面ID边界保留。所有写前PFC检查恰好5项且全ACTIVE，不接受inactive/suppressed。FEATURE_STATUS_COVERAGE和NATIVE_FAILURE_FLAGS_DONE明确原5类失败flag未检查。首次特征ID未返回前按index定位，后续状态带featureId/index。

| 关键边界 | 返回/汇总 | 含义 |
|---|---|---|
| FIRST_CONFIRMATION_START | FIRST_CONFIRMATION_DONE | 第一次默认NO风险确认 |
| THICKNESS_PROMPT_START | THICKNESS_PROMPT_DONE | 提示手动输入厚度 |
| THICKNESS_INPUT_START | THICKNESS_INPUT_DONE | 输入返回，摘要仅null/长度 |
| — | TEST_THICKNESS_ENTERED | 显式正有限试验厚度，非全局证明 |
| PRECOMMIT_CONTEXT_START | PRECOMMIT_CONTEXT_DONE | 活跃模型/版本/dirty组合重检 |
| PRECOMMIT_BODY_START | PRECOMMIT_BODY_DONE | 原稳定体门，内部原审计保留 |
| PRECOMMIT_UNITS_START | PRECOMMIT_UNITS_DONE | mm复检 |
| PRECOMMIT_FACE_START | PRECOMMIT_FACE_DONE | 原捕获选择及稳定面门，内部原审计保留 |
| — | PRECOMMIT_RECHECK_DONE | 同5特征ID及全ACTIVE等写前复检通过 |
| FINAL_CONFIRMATION_START | FINAL_CONFIRMATION_DONE | 显示实际输入厚度的第二次默认NO确认 |
| ELEMENT_TREE_PREPARE_START | ELEMENT_TREE_PREPARE_DONE | 内存选择/参数/元素树准备，无模型提交 |
| — | WRITE_DISPATCH_INTENT / CREATE_INTENT | 持久提交意图，不能当作SDK已调用/返回 |
| WCREATE_FEATURE_START | WCREATE_FEATURE_RETURNED | writeModel=true，返回摘要仅returnedNull |
| — | CREATE_RETURNED | 非null创建对象已返回，尚未回读属性 |
| — | REGENERATE_INTENT | 显式再生意图 |
| WREGENERATE_START | WREGENERATE_RETURNED | writeModel=true，返回摘要无原生getter |
| — | REGENERATE_RETURNED | 显式再生已返回 |
| CREATED_ID_START | CREATED_ID_DONE | created.GetId |
| CREATED_NAME_START | CREATED_NAME_DONE | created.GetName |
| CREATED_TYPE_START | CREATED_TYPE_DONE | created.GetFeatType |
| CREATED_TYPE_NAME_START | CREATED_TYPE_NAME_DONE | created.GetFeatTypeName |
| CREATED_STATUS_START | CREATED_STATUS_DONE | created.GetStatus |
| CREATED_TREE_START | CREATED_TREE_DONE | created.GetElementTree，仅记录null，不遍历 |
| — | NATIVE_READBACK | 六项有限摘要，fiveFailureFlagsChecked=false |
| — | FINAL_RESULT | 完成/取消/阻断/提交后未知，明确未保存 |

重复复检是主动确认后的上下文核验，不是创建/再生重试。内存元素树准备与复检组合动作有外层边界，内部卡点可能需结合更细原体/面审计，不能冒称每个内部getter均单独probe。regeneration options对象和设置沿用旧候选，不额外扩大模型API。

创建返回null时仍有WCREATE_FEATURE_RETURNED.returnSummary.returnedNull=true，但没有非null的CREATE_RETURNED，不再执行再生。创建返回记录/审计失败、异常也不再生，不为凑齐验收事件重试。再生返回后的任一回读异常同样停止，无原生失败快照/补救操作。

有限回读终态为CREATE_REGENERATE_RETURNED_LIMITED_READBACK_NOT_SAVED_UNVERIFIED，不能改写成“转换成功/已完整检查/可生产”。NATIVE_READBACK明确nativeSheetmetalStateVerified=false、globalThicknessProven=false、productionReadinessCertified=false。未进行体状态、特征列表成员、元素内容或五类错误flag的转换后核验。

最后未配对START只是最后可见调用边界，可能受审计失败、Java处理、外部终止影响；确认或输入START还可能正常等待人工。不要补DONE。提交后不论返回、异常或宿主停住，均保留现场，不保存、不再次点击；结合stderr/jlink和外部时间记录分析。
