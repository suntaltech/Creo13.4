# Step23 根探针与无可用重载的记录

START必须写入并force(true)后才执行动作；失败不调用。requestId/probeId配对START/DONE/THREW，嵌套外层和内部根探针保持各自ID。DONE摘要只做Java空值检查，不读取原生对象属性。

| 事件 | 含义 |
|---|---|
| SELECTION_BODY_CREATE_START/DONE | 保留已核验体选择创建，returnedNull |
| SELECTION_FACE_CREATE_START/DONE | 保留已核验面选择创建，returnedNull |
| ROOT_ELEMENT_CREATE_START/DONE | 唯一一次Element_Create(PRO_E_FEATURE_TREE,null,0)，returnedNull |
| TREE_ALTERNATIVE_UNAVAILABLE | 非空根返回后声明离线SDK无数组/其他集合重载，treeConstructed=false |
| ELEMENT_TREE_PREPARE_START/DONE | 外层兼容事件；operation=PROBE_ROOT_ONLY_NO_AVAILABLE_TREE_OVERLOAD，不表示建树 |
| FINAL_RESULT | ROOT_RETURNED_NO_TREE_OVERLOAD_NO_MODEL_WRITE_NO_SAVE或取消/阻断 |

ROOT上下文包含实际elementId/elementIdName、elementIndex=0、level=0、argumentKind=NULL_ROOT。没有Elements.create/append、值工厂、根追加、会话建树或数组重载调用事件。TREE_ALTERNATIVE_UNAVAILABLE是静态证据说明，不是原生调用DONE；evidenceSource明确OFFLINE_REAL_SDK_SIGNATURES_NOT_RUNTIME_REFLECTION，不能当作现场动态方法扫描。

SDK核对输出表明同名CreateElementTree只接受Elements，且没有公开Elements数组构造器。CreateElementTreeFromXML是另一方法，声明存在但本轮不执行。不要把“无数组/集合重载”解释成“所有建树API都不存在”，也不要伪造替代方法调用结果。

根返回null时ROOT_ELEMENT_CREATE_DONE.returnedNull=true，随后阻断，不记录TREE_ALTERNATIVE_UNAVAILABLE成功链路/外层完成。Exception/LinkageError记录PREFLIGHT_PROBE_EXCEPTION、原异常类/message/operation/根上下文。外层可能包裹异常，原始定位看最内层；宿主崩溃/永不返回可能没有异常终态。最后未配对START仅是最后可见接口，不证明内部根因；审计I/O失败、外部终止也可能造成同样形态，不补DONE。

保留预检/PFC状态、两次确认和厚度输入；五类失败flag未检查，厚度只记录。确认/输入START可能正常等待人工。根返回不代表建树、转换或生产可用，不自动恢复Step20/21/22，也不由此断言旧卡顿偶发。
