# Step18 边界与状态门

每probe的START、DONE、THREW按requestId/probeId配对，operation精确标识接口，sequence与UTC/elapsedNs保留。DONE.returnSummary仅Java摘要，枚举getValue为SDK Java枚举读取，不追加模型原生查询。actionAndStartAuditElapsedNs含强制审计/Java批次，不是纯JNI计时。

| START | DONE | 内容 |
|---|---|---|
| VERSION_STAMP_READ_START | VERSION_STAMP_READ | Model.GetVersionStamp |
| MODIFIED_FLAG_READ_START | MODIFIED_FLAG_READ | Model.GetIsModified |
| FEATURE_LIST_START | FEATURE_LIST_COLLECTION_DONE | Model.ListItems(ITEM_FEATURE) |
| FEATURE_LIST_COUNT_START | FEATURE_LIST_COUNT_DONE | ModelItems.getarraysize |
| FEATURE_LIST_ITEM_START | FEATURE_LIST_ITEM_DONE | ModelItems.get(index)，Feature接口验证 |
| FEATURE_LIST_ID_START | FEATURE_LIST_ID_DONE | 首次Feature.GetId，返回摘要给出ID |
| — | FEATURE_LIST_DONE | 数量、所有唯一特征ID |
| FEATURE_STATE_ID_START | FEATURE_STATE_ID_DONE | 当前Feature.GetId，与枚举ID核对 |
| FEATURE_STATUS_START | FEATURE_STATUS_DONE | Feature.GetStatus，枚举名/数值/null |
| FEATURE_WFC_VIEW_START | FEATURE_WFC_VIEW_DONE | WFeature视图适配，无原生状态调用 |
| FEATURE_STATUS_FLAG_START | FEATURE_STATUS_FLAG_DONE | WFeature.GetStatusFlag，null判断 |
| FEATURE_FLAG_ID_START | FEATURE_FLAG_ID_DONE | FeatureStatusFlag.GetFeatureId |
| FEATURE_FLAG_FIELD_START | FEATURE_FLAG_FIELD_DONE | operation指明以下某个字段getter |
| — | FEATURE_STATE_DONE | 本特征status、flagID、11字段、判门结果及reason |
| — | FEATURE_STATE_REJECTED | 可读失败原因、featureId/状态（视失败点可得信息） |
| — | NATIVE_FAILURE_FLAGS_DONE | 全部特征通过，source=PER_FEATURE_STATUS_AND_FLAG |
| PREFLIGHT_BODY_ID_READ_START | PREFLIGHT_BODY_ID_READ_DONE | SolidBody.GetId |
| PREFLIGHT_FACE_ID_READ_START | PREFLIGHT_FACE_ID_READ_DONE | Surface.GetId |
| — | PREFLIGHT | 上述只读门通过 |

11字段固定顺序：GetIsInvalid、GetIsInactive、GetIsActive、GetIsFamtabSuppressed、GetIsSimprepSuppressed、GetIsProgramSuppressed、GetIsSuppressed、GetIsUnregenerated、GetIsFailed、GetIsChildOfFailed、GetIsChildOfExternalFailed。除首次枚举ID外，每次状态/字段START/DONE/THREW都有顶层featureId、featureIndex、featureIdKnown=true；首次ID前不可能知道未读取ID，记录索引，返回摘要补ID。重复ID拒绝，后续身份变化拒绝，不跨模型按ID匹配。

任一原5错误布尔为true、枚举FEAT_UNREGENERATED、未知/缺失状态、缺失字段或flagID不一致均阻断；完整字段正常读取后统一判门。FeatureStatus枚举只有active/inactive/各suppressed/unregenerated，不能用枚举单独推断failed，所以保留逐特征flag的5类拒绝条件。

PREFLIGHT之后FIRST_CONFIRMATION_START/DONE、THICKNESS_PROMPT_START/DONE、THICKNESS_INPUT_START/DONE沿用。第一次默认NO确认后可输入明确正有限测试厚度，结束为PREFLIGHT_AND_INPUT_RECORDED_NO_CONVERSION_NO_SAVE。没有写模型或二次确认。

原生/Java异常记录PREFLIGHT_PROBE_EXCEPTION（phase=THREW、operation、原exceptionClass/message及上下文），外层ERROR/FINAL_RESULT停止，不重试。最后未配对START是最后可见调用边界；审计I/O失败、宿主终止也可能留下同样形态，不能单凭一个START宣布SDK内部根因。确认/输入START也可能是正常等待用户，需外部观察。崩溃或永久不返回无法保证THREW或最终结果，不能补写DONE。
