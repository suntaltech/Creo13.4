# 证据勾稽、命名和拒收

本文件只规定未来采集，不生成证据文件、真实日期/操作者名、requestId、截图或日志路径。

| 对象 | 必须关联 | 拒收条件 |
|---|---|---|
| 源码与实际加载来源 | 相对源码名/hash、隔离编译来源、当前会话 | 只有同名源码或编译通过，无加载身份 |
| 受控dat | 实际startup、入口、classpath、text_dir、allow_stop、delay_start | 未解析模板被当作加载证据 |
| 会话 | planId、单变量、模型模式、实际重启 | 跨会话复制结果，或把restartRequired视为已重启 |
| 命令 | 对象解析、搜索、手动Add、回调中的同一commandId | 三旧命令与转换命令混为一个事件 |
| requestId | 实际错误码/阶段/完成分类/调用触及 | 无回调ID、不同请求字段拼接、计划标签补写实际日志 |
| 路线A策略 | 一个policyId、实际结果/异常与本会话引用 | 合并策略、从前一按钮/策略推断后一项，或直接填L3 |
| L3 | MANUAL_UI、准确搜索/类别/模式、身份和外部截图/日志 | Designate/菜单返回、COMMAND_FOUND或宏结果替代手动观察 |
| L4 | 手动Add操作、固定位置、点击/只读反馈、同命令同会话引用 | 只有候选项、只有旧菜单结果或未记录反馈 |
| 路线B | 六项阻塞前提的独立原始证据 | 引用C1/C2/C3满足JAR、解锁、签名、许可或注册条件 |
| 保管核对 | case/session/request/command、源码hash及文件引用 | 只因Schema或SHA形状通过就填运行认证 |

R7 Schema的L3形状最低门槛允许截图或日志引用；本次准备清单建议同时收集截图与日志作交叉印证。满足宽松形状不等于本清单证据齐备，更不等于内容真实。L4须有截图与手动操作记录，并核对反馈与身份；跨引用匹配不能由形状校验代填。

命名组成可用planId或caseId、条件、操作者实际日期、操作者标识、recordKind。离线不填具体文件名；真正文件名和hash由未来真实记录形成。日期与用户名只留在受控外部证据中，不进入本目录的静态自查。

sourceHashMatch、sessionMatch、commandIdMatch和referenceIntegrityCheckedByOperator默认UNVERIFIED。schemaValidatedEvidence只描述形状，runtimeEvidenceAuthenticated=false保持；没有自动证据认证。

若负向门禁未停止、模型可能改变或触及WCreateFeature/WRegenerate/Save，停止后续采集并保留真实状态，不能静默重试、保存或删除来掩盖失败。本目录不执行这些处置动作。
