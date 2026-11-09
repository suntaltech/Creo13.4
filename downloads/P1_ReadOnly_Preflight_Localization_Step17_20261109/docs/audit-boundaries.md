# 审计边界与判读

## 有返回的标记

每个probe有递增probeId、operation、phase=START/DONE/THREW、requestId/sequence/UTC/单调elapsedNs。START写完并force(true)后才调用action；DONE的returnSummary只做Java数据摘要，不偷偷调用其他原生getter。actionAndStartAuditElapsedNs包含起始审计与批次Java处理，不是纯JNI或CPU计时。START/记录写入本身可能增加耗时，正常返回后写DONE失败也可能留下未配对START。

| 前置START事件 | 返回/汇总事件 | operation |
|---|---|---|
| VERSION_STAMP_READ_START | VERSION_STAMP_READ | Model.GetVersionStamp，记录实际value/null |
| MODIFIED_FLAG_READ_START | MODIFIED_FLAG_READ | Model.GetIsModified，记录实际boolean |
| FEATURE_LIST_START | FEATURE_LIST_COLLECTION_DONE | Model.ListItems(ITEM_FEATURE) |
| FEATURE_LIST_COUNT_START | FEATURE_LIST_COUNT_DONE | ModelItems.getarraysize |
| FEATURE_LIST_ITEMS_START | FEATURE_LIST_ITEMS_DONE | ModelItems.get[*]，含Feature接口检查 |
| FEATURE_LIST_IDS_START | FEATURE_LIST_DONE | ModelItem.GetId[*]，检查唯一ID |
| — | FEATURE_LIST_ITEM_DONE | 仅最终itemsFetched/idsRead计数 |
| NATIVE_FAILURE_FLAGS_START | NATIVE_FAILURE_FLAGS_COLLECTION_DONE | WSolid.GetSolidFeatureStatusFlags |
| NATIVE_FAILURE_FLAGS_COUNT_START | NATIVE_FAILURE_FLAGS_COUNT_DONE | FeatureStatusFlags.getarraysize |
| NATIVE_FAILURE_FLAGS_ITEMS_START | NATIVE_FAILURE_FLAGS_ITEMS_DONE | FeatureStatusFlags.get[*] |
| NATIVE_FAILURE_FLAG_SCAN_START | NATIVE_FAILURE_FLAG_SCAN_DONE | 5种FeatureStatusFlag getter，每种独立probe |
| — | NATIVE_FAILURE_FLAGS_DONE | flags数量、已查字段数量及无拒绝标记 |
| PREFLIGHT_BODY_ID_READ_START | PREFLIGHT_BODY_ID_READ_DONE | SolidBody.GetId |
| PREFLIGHT_FACE_ID_READ_START | PREFLIGHT_FACE_ID_READ_DONE | Surface.GetId |
| — | PREFLIGHT | 以上读/检查全部通过，只表示预检记录 |

5种状态getter为GetIsFailed、GetIsInvalid、GetIsUnregenerated、GetIsChildOfFailed、GetIsChildOfExternalFailed，与Step16相同。任一实际true仍拒绝，不进入确认；原生集合null、错误接口、重复ID、超原10000项界限等仍停止。加入负数量拒绝，不把异常计数当空集合。

## 循环与原顺序的区别

特征原本get(i)与GetId交错，现将成员获取和ID读取拆开；失败标志原本逐项短路5字段，现逐字段扫描所有项（遇该字段true即止）。这是明确的只读诊断重排，未新增状态getter或放宽拒绝布尔条件；没有模型写入路径。保留批次Java类型/唯一ID校验，异常可能来自Java校验而非SDK，其实际exceptionClass/message会记录。重排可影响对象缓存/时序，因此不能据本包运行时间复原旧回调耗时。

不逐项写大量审计，具体API可定位，批次内实际索引若无异常message则未知。若最后START operation=FeatureStatusFlag.GetIsInvalid[*]，只能说明此批次未观察到DONE，不能精确指定第几项或JNI内部位置。FEATURE_LIST_ITEM_DONE是最终计数，不是伪造逐项完成。无面枚举高频日志变更。

## 异常/未返回/人工等待

action或返回摘要/审计抛Exception/LinkageError时记录PREFLIGHT_PROBE_EXCEPTION，包含原异常类/message、probeId/operation/startEvent；外层再记录ERROR和只读阻断FINAL_RESULT，不自动重试。START写入失败会阻断action，不能把“没START”当成原生API已调用。native进程/VM崩溃或一直不返回可能没有异常终态，保持原件，不补DONE。

按probeId找最后一个START且没有相同DONE/THREW的调用。它是最后可见边界，不是自动根因证明：审计I/O、Java校验、调度、宿主崩溃或外部终止也可能留下同样形态。完整stderr/dbg_err_crash与外部未响应时间要一起核对。

PREFLIGHT之后还有FIRST_CONFIRMATION_START/DONE、THICKNESS_PROMPT_START/DONE、THICKNESS_INPUT_START/DONE；对话框以明确options调用，首次确认默认NO。停在第一确认START也可能只是等待用户，不自动判为崩溃。输入只记录null/长度，成功解析的明确试验厚度通过TEST_THICKNESS_ENTERED记录，随后FINAL_RESULT=PREFLIGHT_AND_INPUT_RECORDED_NO_CONVERSION_NO_SAVE；没有markDispatch、最终写确认或转换。
