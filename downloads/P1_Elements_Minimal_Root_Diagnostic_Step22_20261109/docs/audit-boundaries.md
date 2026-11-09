# Step22 最小边界与判读

按requestId/probeId配对START/DONE/THREW，sequence、UTC与单调elapsedNs保留。外层probe与内部probe各自唯一，不将外层未完成误当作没有内部细分。所有START先写盘并force(true)，失败不调用动作。返回摘要不额外调用原生getter。

| START | DONE | operation/摘要 |
|---|---|---|
| SELECTION_BODY_CREATE_START | SELECTION_BODY_CREATE_DONE | pfcSelect.CreateModelItemSelection(body)，returnedNull |
| SELECTION_FACE_CREATE_START | SELECTION_FACE_CREATE_DONE | pfcSelect.CreateModelItemSelection(face)，returnedNull |
| ELEMENTS_CREATE_MINIMAL_START | ELEMENTS_CREATE_MINIMAL_DONE | Elements.create，returnedNull |
| ROOT_ELEMENT_VALUE_CREATE_START | ROOT_ELEMENT_VALUE_CREATE_DONE | ROOT_NULL_VALUE_NO_FACTORY，valueIsNull=true/valueFactoryInvoked=false |
| ROOT_ELEMENT_CREATE_START | ROOT_ELEMENT_CREATE_DONE | wfcElementTree.Element_Create(PRO_E_FEATURE_TREE,null,0)，returnedNull |
| ROOT_ELEMENT_APPEND_START | ROOT_ELEMENT_APPEND_DONE | Elements.append(root)，returned=true/elementsAppended=1 |

ROOT三类探针均携带elementIndex=0、真实elementId、elementIdName=PRO_E_FEATURE_TREE、level=0、argumentKind=NULL_ROOT。ROOT_VALUE阶段仅Java null赋值记录，无SDK参数工厂；即使其日志写入出错也停止，不伪造值工厂执行。

原外层ELEMENT_TREE_PREPARE_START/DONE保留，实际operation=PREPARE_MINIMAL_ELEMENTS_AND_ROOT_ONLY。正常根追加后立即返回外层，DONE明确sessionElementTreeCreated=false、thicknessAppliedToElement=false。没有其他根/子元素或会话建树调用；根append返回只证明集合追加返回，不证明元素树语义被Creo接受。

若Elements.create不返回，最后可见内部START应为ELEMENTS_CREATE_MINIMAL_START；若返回null，会有MINIMAL_DONE(returnedNull=true)且阻断，无ROOT_VALUE/CREATE/APPEND。若根构造不返回，最后START为ROOT_ELEMENT_CREATE_START；若返回null，DONE记录null，随后阻断不append。若追加不返回，最后START为ROOT_ELEMENT_APPEND_START。

Exception/LinkageError记录PREFLIGHT_PROBE_EXCEPTION、具体operation和原异常类/message；根上下文保留。外层可能再记录包裹异常，原始定位看最内层事件。宿主崩溃/一直不返回可能没有THREW或FINAL_RESULT。START未配对也可能是审计I/O/宿主终止，不能单凭它宣布内部根因；actionAndStartAuditElapsedNs含审计/Java处理，不是纯JNI计时。

Step21版本/dirty、体/面、逐特征PFC门、两次确认、厚度输入和复检保持；五类失败flag未检查。确认/输入START也可能只是人工等待。成功终态MINIMAL_ROOT_APPENDED_NO_MODEL_WRITE_NO_SAVE不认证旧卡顿偶发，不自动恢复转换或执行替代构造方式；保留原件，不补DONE或为完成日志重试。
