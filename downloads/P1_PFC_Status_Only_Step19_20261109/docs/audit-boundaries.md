# Step19 审计边界与降级覆盖

START/DONE/THREW按requestId/probeId配对。START写入并FileChannel.force(true)成功后才调用动作；写入失败不执行动作。DONE.returnSummary只做Java摘要，FeatureStatus.getValue是SDK Java枚举值读取，不额外查询模型。actionAndStartAuditElapsedNs包含起始审计I/O和Java处理，不是纯JNI计时。

| START | DONE | operation/摘要 |
|---|---|---|
| VERSION_STAMP_READ_START | VERSION_STAMP_READ | Model.GetVersionStamp |
| MODIFIED_FLAG_READ_START | MODIFIED_FLAG_READ | Model.GetIsModified |
| FEATURE_LIST_START | FEATURE_LIST_COLLECTION_DONE | Model.ListItems(ITEM_FEATURE) |
| FEATURE_LIST_COUNT_START | FEATURE_LIST_COUNT_DONE | ModelItems.getarraysize |
| FEATURE_LIST_ITEM_START | FEATURE_LIST_ITEM_DONE | ModelItems.get(index)，Feature接口验证 |
| FEATURE_LIST_ID_START | FEATURE_LIST_ID_DONE | 首次Feature.GetId，返回摘要给出ID |
| — | FEATURE_LIST_DONE | 实际数量与唯一ID列表 |
| — | FEATURE_STATUS_COVERAGE | 声明PFC_STATUS_ONLY及fiveFailureFlagsChecked=false |
| FEATURE_STATE_ID_START | FEATURE_STATE_ID_DONE | Feature.GetId，与枚举ID再次核对 |
| FEATURE_STATUS_START | FEATURE_STATUS_DONE | Feature.GetStatus，枚举名/数值/null |
| — | FEATURE_STATE_DONE | 该特征状态、判门结果、降级来源和诊断标记 |
| — | FEATURE_STATE_REJECTED | featureId、状态或身份变化及reason |
| — | NATIVE_FAILURE_FLAGS_DONE | 所有PFC状态通过，明确未检查五类flag |
| PREFLIGHT_BODY_ID_READ_START | PREFLIGHT_BODY_ID_READ_DONE | SolidBody.GetId |
| PREFLIGHT_FACE_ID_READ_START | PREFLIGHT_FACE_ID_READ_DONE | Surface.GetId |
| — | PREFLIGHT | 只读门完成，conversionReadinessCertified=false |

不存在FEATURE_WFC_VIEW/FEATURE_STATUS_FLAG/FEATURE_FLAG_ID/FEATURE_FLAG_FIELD调用事件，也没有整体solid flags调用。NATIVE_FAILURE_FLAGS_DONE只是保留汇总事件名，其source精确为PFC_FEATURE_STATUS_ONLY_WFC_STATUS_FLAG_DISABLED_DUE_TO_HANG，不能根据名字判断旧5类失败检查已完成。fiveFailureFlagsChecked和fullFailureFlagCoverage均false；不输出anyRejectedFlag=false之类可能暗示完整flag扫描的结果。

FEAT_ACTIVE接受；五类inactive/suppressed接受并记录inactiveOrSuppressedDiagnosticOnly=true；FEAT_UNREGENERATED拒绝；null及未知枚举拒绝。任一读异常/无法取得当前ID/重复或变化ID仍停止，不用未知状态冒充ACTIVE。列表数量界限0..10000沿用，不增加模型查询或重试。

首次ID读取前只有featureIndex，不能预知未读取的ID；后续GetId/GetStatus的START、DONE和THREW都有顶层featureId/index。异常记录PREFLIGHT_PROBE_EXCEPTION和原exceptionClass/message，外层ERROR/FINAL_RESULT结束。一个特征阻断后不继续读后续特征；不要为了凑齐5个状态绕过门。

PREFLIGHT后仍有FIRST_CONFIRMATION_START/DONE、THICKNESS_PROMPT_START/DONE、THICKNESS_INPUT_START/DONE。首次确认默认NO，界面告知原五类flag未检查；输入只记录，成功终态PREFLIGHT_AND_INPUT_RECORDED_NO_CONVERSION_NO_SAVE。该终态也不是生产模型安全或转换资格证明。

最后未配对START是最后可见边界，审计I/O、宿主终止、Java异常或正常对话框等待可能造成相似形态，需结合现场时间与stderr/jlink。宿主严重错误/不返回不保证有THREW或最终结果，不补写DONE。强制审计增加I/O开销，不以本步时间直接比较旧预检性能。
