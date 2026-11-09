# 受控使用与回收

本包未部署/注册/运行Creo。先校验manifest/SHA256SUMS、实际Java25/SDK及独立classes或薄JAR；填未启用注册模板的本机路径，不覆盖旧注册/Ribbon，不混用Step15或其他旧class。只使用SISWControlledConvertStep16新入口。

当前模型仍须可丢弃200002100.prt、普通零件、mm、唯一默认实体体非钣金；在图形区只预选一个该体的可见平面，不选装配路径/其他模型/边/基准面。新增面规则不消除其他门禁；默认体检查若再次阻断，保留实际错误码而非抄旧ACCEPTED。

只主动点击Step16一次。它有自己的持久once文件，取消/阻断/失败亦消耗，不读取/删除Step15锁，不自动重试、不通过换目录或重载绕过。模块装载本身不转换。两次原生YES/NO都默认NO；手动输入本次试验厚度，不用1.2mm局部距离自动猜全局值。没有提前默认同意、自动执行或后台模型API。

回收Step16一次JSONL、stderr、加载身份及现场结果：BODY_IDENTITY_CHECK、FACE_SELECTION_CHECK和FACE_IDENTITY_CHECK各阶段、selectionCount/pathPresent/itemIsSurface/selectionModelMatches、body/default/baseline ID、selected/stableFaceId、匹配数量、句柄差异/resultCode，是否到两次确认/厚度输入及真实取消/确认结果。确认期间若原Selection被改变，原ID重检会拒绝，不改用新缓冲面。

全部门通过后仍最多一次创建/一次显式再生，不Save/覆盖原模型、不自动Undo/删除/恢复。创建/再生失败或回读不完整明确保留可能改变的失败状态、不得保存或重试。候选未全局拦截用户/其他插件保存入口，现场仍需遵守可丢弃试验模型不保存约束。成功也只做原生状态/新特征回读及人工看树，不认证全局壁厚、材料占据、等厚或生产可发布。

本轮任务文字给出了Step15结果摘要，没有其原始日志；新25项面策略测试使用纯参数，不是几何/SDK Mock或真机运行。现有56项默认体/控制测试继续通过，新增25项覆盖空/多选、路径、非Surface、其他模型、跨体作用域、无/多重ID匹配、缺值/读错及确认后面变化。实际Step16面门和其余UI/原生转换还未执行；若其他门阻断按原码停止，不顺手放宽。
