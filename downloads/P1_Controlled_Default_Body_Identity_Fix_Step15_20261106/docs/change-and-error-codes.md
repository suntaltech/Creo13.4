# 修改点和错误码

## 限定默认体策略

verifyDefaultBody由INITIAL及RECHECK共用。先在当前活跃目标零件、原model/solid视图作用域及类型条件成立后，分别读取Model.ListItems(ITEM_BODY)数量/第一项、GetDefaultBody、两者GetId/GetType、默认体IsSheetmetal，保留各自异常。不得将读取异常当null或false。

唯一体与两对象均可用、类型正确且读取成功时，调用纯ScopedBodyIdentity策略比较ID；首次接收绑定stableBodyId，复检还要等于此初始ID。ID仅在该当前模型/体作用域有效，不跨零件/装配或任意ID做匹配。三处recheck（厚度输入后、最终确认后、提交前）全部走此函数，并更新s.body为本次核验返回视图。

Java引用、当前/枚举/上一次默认体句柄及相等结果只作BODY_IDENTITY_CHECK诊断；句柄读取异常也仅记诊断，不转成体身份失败。模型作用域/视图及驱动面原有same保持，不借本证据去全局更改其他对象身份。没有把本轮ID规则升级为完整SDK身份契约认证。

| resultCode / errorCode | 条件 |
|---|---|
| DEFAULT_BODY_NULL | 原生默认体明确返回null，非异常 |
| ENUMERATED_BODY_UNAVAILABLE | 枚举集合/数量未知、非唯一，或唯一第一项为空 |
| MODEL_SCOPE_MISMATCH | 目标模型/零件类型/原生视图/体项目类型作用域不符，或确认期间模型/版本/dirty变化 |
| BODY_ID_MISMATCH | 当前枚举ID与默认ID不等，或复检ID偏离初次stableBodyId |
| ALREADY_SHEETMETAL | 其他体身份门成立后，当前默认体原生状态为true |
| STATE_READ_EXCEPTION | 模型/体元数据或IsSheetmetal读取抛异常、必要字段未知、复检缺初始ID |
| ACCEPTED | 仅这次默认体门接受；不是允许绕过其他门禁或已转换 |

作用域不符先拒绝；已知读取异常不伪装默认体null；缺体/不唯一/不可用、必要值未知、ID不符和原生状态依次拒绝。多项异常时日志readErrors及各原生值保留，不仅依赖单个主错误码。其他未改业务阶段仍可能报OTHER_FAILURE及原阶段消息，不把单位/驱动面/日志I/O异常都伪归为体身份错误。

ERROR增加errorCode/bodyCheckStage，失败UI直接显示含错误码的异常消息；JSON还保留内部异常原因。两次风险对话框、默认NO、明确厚度输入、调用前模型/版本/dirty/特征/单位/驱动面重检、单次提交和转换后只读核验均保留。

## 冻结边界

same和ptr函数、requireFace、特征ID及失败标志检查、7节点WFC驱动墙树（仅Step15命名差异）、创建选项/再生参数、唯一创建/再生、原生钣金/新壁状态及树回读均与Step13规范化比对相同。新代码只增加两个ITEM_BODY类型查询，不加新的写接口。

Step15Control是Step13Control按独立命名规范化后的相同状态机/CREATE_NEW/force(true)机制。它使用Step15自己的单次文件，原Step13文件和代码不变；没有程序化清锁、重置点击或自动转调Step13。仅下载/装载并不提交模型，真实点击尚未发生。
