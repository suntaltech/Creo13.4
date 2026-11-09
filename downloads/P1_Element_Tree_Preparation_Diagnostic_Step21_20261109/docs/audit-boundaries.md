# Step21 prepareTree 边界

外层ELEMENT_TREE_PREPARE_START/DONE包围整个准备阶段。内部每probe有唯一递增probeId和operation，按requestId/probeId匹配START/DONE/THREW；嵌套外层保持自己的probeId，不与内部最后一个探针混淆。每个START写入并force(true)之后才调用action。

| START | DONE | 单个操作/上下文 |
|---|---|---|
| SELECTION_BODY_CREATE_START | SELECTION_BODY_CREATE_DONE | pfcSelect.CreateModelItemSelection(body)，已核验bodyId |
| SELECTION_FACE_CREATE_START | SELECTION_FACE_CREATE_DONE | pfcSelect.CreateModelItemSelection(face)，已核验faceId |
| ELEMENTS_CREATE_START | ELEMENTS_CREATE_DONE | Elements.create |
| ELEMENT_VALUE_CREATE_START | ELEMENT_VALUE_CREATE_DONE | pfcArgument指定工厂，elementId/index/level/argumentKind |
| ELEMENT_CREATE_START | ELEMENT_CREATE_DONE | wfcElementTree.Element_Create(id,value,level)，同元素上下文 |
| ELEMENT_APPEND_START | ELEMENT_APPEND_DONE | Elements.append(element)，同元素上下文 |
| SESSION_CREATE_ELEMENT_TREE_START | SESSION_CREATE_ELEMENT_TREE_DONE | WSession.CreateElementTree(elements) |

ELEMENT_APPEND只表示追加这个动作，不把Element_Create或值工厂放入同一action。数个元素使用相同工厂时由elementIndex/elementId唯一识别。FeatureType.getValue是工厂action内的SDK Java枚举值转换，不是额外模型原生查询。返回摘要不读取原生对象属性；根null值没有工厂调用。7个元素分别执行上述构造/追加操作，非根共6次工厂调用。

| elementIndex | elementIdName | level | argumentKind/本轮来源 |
|---|---|---|---|
| 0 | PRO_E_FEATURE_TREE | 0 | NULL_ROOT，无工厂 |
| 1 | PRO_E_FEATURE_TYPE | 1 | CreateIntArgValue，DATUM_SURFACE枚举值 |
| 2 | PRO_E_STD_FEATURE_NAME | 1 | CreateStringArgValue，P1_STEP21_DRIVING |
| 3 | PRO_E_DTM_SRF_TYPE | 1 | CreateIntArgValue，15 |
| 4 | PRO_E_SMT_CONV_BODY | 1 | CreateSelectionArgValue，原核验体选择 |
| 5 | PRO_E_SMT_DRV_SURF | 1 | CreateSelectionArgValue，原核验面选择 |
| 6 | PRO_E_STD_SMT_THICKNESS | 1 | CreateDoubleArgValue，TEST_THICKNESS_ENTERED中的输入 |

实际数字elementId由真实wfcElemIds常量记录，不用表格伪造SDK值。元素/选择的对象不序列化，不输出XYZ/UV、句柄或元素树内容。elementsAppended=7仅在全部7次append正常返回后记录，不额外调用集合或原生getter验证。

PREFLIGHT、逐特征PFC状态及原BODY/FACE审计保持；FIRST_CONFIRMATION、THICKNESS_PROMPT/INPUT、FINAL_CONFIRMATION、PRECOMMIT复检边界保留，但所有确认仅授权本步内存准备。fiveFailureFlagsChecked=false，不宣称完整错误检查。

Exception/LinkageError记录PREFLIGHT_PROBE_EXCEPTION和具体operation/原exceptionClass/message；elementId/index/level保留。嵌套外层可能再记录包裹异常，定位以最内层原始异常/最后START为准。START写盘失败不执行动作。DONE写盘或摘要失败也可能留下未配对START；actionAndStartAuditElapsedNs含审计与Java处理，不是纯JNI计时。

SESSION_CREATE_ELEMENT_TREE_DONE.returnedNull=true表示调用返回null而非成功，随后阻断；非null才允许ELEMENT_TREE_PREPARE_DONE和成功终态。若宿主永久不返回/崩溃，可能没有THREW/FINAL_RESULT，保留原件，不补DONE。确认/输入START可能仅是人工等待。

最后未配对START可指出最后可见接口及元素，不是内部崩溃根因证明。准备完成不证明此前卡顿是环境偶发；需要回收本轮证据后再决定下一步，不自动恢复转换或重用Step20锁。
