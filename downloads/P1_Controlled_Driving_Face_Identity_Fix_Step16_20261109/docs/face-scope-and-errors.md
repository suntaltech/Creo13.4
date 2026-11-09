# 驱动面范围与错误码

## 新范围

首次requireFace捕获CURRENT_PRESELECTION_BUFFER中的唯一Selection，要求Surface且GetPath()==null、GetSelModel与当前目标模型通过原same。不能选边/基准面/其他对象，不能用装配路径或其他模型绕过。没有放开原非空路径规则，即使路径对象表示形式不同也不得隐式改用ID跨装配匹配。

模型/零件/体作用域包括：活跃目标200002100.prt、MDL_PART、原WSolid视图与model身份、当前snapshot体ID=stableBodyId、当前默认体ID=stableBodyId、当前ITEM_BODY数量1且唯一项ID=stableBodyId。默认体的完整类型/状态核验仍先走不变的Step15 verifyDefaultBody，面范围检查再次读取当前体上下文；scope不符立即阻断，不扫描其他体。

选择面的GetId只与上述s.body.ListSurfaces中的ID比较，原列表上限10000保留。唯一相同ID才接受；没有匹配或有歧义都拒绝。选中面本身仍须可见、SurfaceType.SURFACE_PLANE、EvalArea有限正。wrapper句柄/Java对象只作为FACE_IDENTITY_CHECK中的selected/listed诊断，不决定归属；句柄诊断不可用不会取消已满足的模型/体/面ID证据。

复检使用ORIGINAL_CAPTURED_SELECTION_NOT_NEW_BUFFER：原Selection路径/Surface类型/所属模型和faceId要重新读取，faceId必须等于首次stableFaceId，再查当前已核验体的列表。不会用本轮后来新选对象替换已确认驱动面。每次recheck仍先核默认体稳定ID/模型版本/dirty/单位/特征状态，再核驱动面。

## 独立面错误

| code | 含义 |
|---|---|
| SELECTION_EMPTY_OR_MULTIPLE | 缓冲为空、返回无选择或数量不为1 |
| SELECTION_PATH_NOT_EMPTY | 捕获的Selection带非空装配路径 |
| SELECTION_NOT_FACE | 唯一Selection为空或GetSelItem不是Surface |
| SELECTION_OTHER_MODEL | 原Selection所属模型未通过原模型same检查 |
| FACE_ID_NOT_FOUND_IN_BODY | 当前已核验体的ListSurfaces里没有唯一匹配ID（日志保留匹配数量） |
| FACE_SCOPE_READ_EXCEPTION | Selection/面/作用域 getter 抛异常、体曲面列表不可读/超限或缺初始面ID |
| FACE_BODY_SCOPE_MISMATCH | 当前模型/当前体不符合核验作用域 |
| FACE_ID_CHANGED | 原Selection面ID在确认期间改变 |
| ACCEPTED | 仅本次面范围检查接受，不等于原生转换成功 |

读取抛错不填成空集合或false；native read errors有FACE_SCOPE_ERROR及原exceptionClass/message。非平面/不可见、面积非法仍使用旧IllegalStateException/OTHER_FAILURE，不与默认体的BODY_ID_MISMATCH等混用。其他未改业务阶段原错误分类保持。

ERROR增加faceCheckStage并优先记录FaceIdentityFailure.code，默认体BodyIdentityFailure仍独立记录bodyCheckStage。新增日志可按requestId/sequence与既有确认/厚度/单次提交/回读事件关联。所有新字段是实时查询或诊断，不读取历史IsSheetmetal/厚度/面ID替代本次值。

## 修改边界

冻结比对：ptr/same、默认体bodyHandle/verifyDefaultBody、ScopedBodyIdentity、单次状态控制、特征枚举/失败标志、7节点WFC树、WCreateFeature/WRegenerate、之后IsSheetmetal/新壁/原生树与状态回读；除Step16命名/单次文件区别外不变。没有全局替换same，没有用ID更改模型/装配作用域比较。

getptr、匹配ID或任务中旧运行摘要都不是对整个SDK稳定身份契约/全会话零写入的独立认证。本步只在明确作用域内采用用户要求的GetId规则，原生许可/内部视图及实际几何接受性仍待真机。
