# 控制流与结果分类

1. 注册start只创建独立同步命令和消息绑定，不转换模型；stop禁止采集中卸载，解绑不清一次锁。生命周期保持原生线程，不启动CREOSON、浏览器/异步桥或其他命令。
2. OnCommand有busy重入保护及永不重置的clicked内存门。然后CREATE_NEW持久审计文件，记录并force(true)，文件已存在就拒绝。此时尚未查询/写模型；错误模型、取消、拒绝、失败也不释放点击资格。创建或关键日志失败不能向下提交。
3. 前置：当前文件名严格200002100.prt、MDL_PART、一个默认实体体、原生IsSheetmetal=false、mm、单个预选可见平面、选择所属模型与原生体列表/对象身份吻合，拒绝装配路径/多体/未知。读取版本戳、dirty、特征ID及失败/无效/未再生/失败子项标记；未知或异常停止。revision未知不冒称已解决；只以可读的当前上下文做确认期间重检。
4. 第一次YES确认可丢弃模型及风险；NO/关闭/拒绝不转换。厚度通过UIReadStringMessage(false)读取，不设置数字默认值；null/空取消，BigDecimal→有限正double，否则停止。数值只是操作者试验参数，不是测量或全局壁厚证明。对话框是否真正显示、取消表现及许可仍待现场，异常按未提交处理。
5. 重检当前模型句柄/文件/版本戳/dirty、默认体/原生钣金状态、单体/mm、特征列表及原驱动面；第二次YES明确最终参数与一次提交。之后同样再检；元素树/创建选项/再生参数准备完成后再检一次。驱动面保留原确认对象，不自动选最大面或替换面。
6. 准备7个既有驱动墙节点；CreateElementTree/参数对象不当作成功转换。FEAT_CR_NO_OPTS；再生参数SetNoResolveMode(true)、SetAllowConfirm(false)，不自动补缺失元素/材料侧值、不要求原生继续交互/重试；实际语义与兼容仍未真机认证。持久WRITE_DISPATCH_INTENT返回之后才唯一一次WCreateFeature；返回null/抛异常同样可能已改模型。返回特征后记录，才唯一一次显式WRegenerate；任何阶段抛错不再往下写。
7. 只读回读：当前仍是同模型、默认体IsSheetmetal、特征在原生列表出现新ID、GetStatus=FEAT_ACTIVE、GetStatusFlag与实体特征状态列表无所检查失败项；GetElementTree(FEAT_EXTRACT_NO_OPTS)读回新特征树。钣金壁元数据条件为FEATTYPE_WALL，或DATUM_SURFACE且一级PRO_E_DTM_SRF_TYPE=15；只验证此次驱动墙模式，不把任意名字叫wall就当成功。不截屏/不伪造视觉树已验证。
8. 所有回读门通过只输出NATIVE_CONVERSION_READBACK_CONFIRMED_NOT_SAVED_VISUAL_TREE_PENDING，UI显示true、新特征ID、状态及需人工看树。实际视觉树/界面报错仍独立记录，不能仅凭“返回对象”或“按钮结束”报成功。若钣金已true而壁/状态读不到，仍FAILED_OR_UNVERIFIED_MODEL_STATE_RETAINED_NO_SAVE，不能自动保存或重试。
9. 前写失败：BLOCKED_BEFORE_WRITE_NO_SAVE；已记录dispatch后失败：FAILED_OR_UNVERIFIED_MODEL_STATE_RETAINED_NO_SAVE。只尝试当前模型默认体/dirty/版本的只读失败快照，不DeleteFeatures/Undo/Erase/模型恢复。若标记后调用前出错，按保守“可能已变”处理。部分记录、I/O关闭/反馈失败不得补成功；stderr保留异常链。
10. 结果优先原生对话框，失败时使用明确FAILED/VERIFIED/CANCELLED消息键的原消息区；若两种原生显示都失败，仅stderr可能可用，无法保证失效的宿主仍显示UI。两个确认本身失败则不会提交；不能用日志替代操作者确认。

本实现只限制自己的入口/代码链，不声称阻止用户或其他插件保存。任务要求试验状态不得保存，必须作为现场规则执行。没有自动备份/回滚的证据，不显示“已恢复原模型”。
