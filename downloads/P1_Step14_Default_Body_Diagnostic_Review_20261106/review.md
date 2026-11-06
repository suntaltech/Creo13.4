# P1 第十四步默认体只读诊断审核

2026-11-06（Asia/Shanghai）。输入step14-default-body-diagnostic-evidence-20261106.zip，仅含13行诊断JSONL。本次解析数据、核对既有源码/class/JAR，并用无SDK纯Java比较器精确回放两条比较记录；没有执行SDK/模型API、改代码或重新转换。

## 本轮已明确的事实

当前200002100.prt，原生ITEM_BODY数量1；枚举第一项、WFC默认体、PFC默认体均存在、实现SolidBody，Java类均com.ptc.wfc.Implementation.pfcSolid$SolidBody。三者GetId均返回-5778，各自IsSheetmetal独立调用均返回false；不是空体，也不是本轮原生已钣金。负号来自本次原生返回，不因其为负就改成正数、0或无效判定。

| 对象/视图 | ID | 原生IsSheetmetal | 与枚举项Java同对象 | 与枚举项句柄相等 | ID相等 |
|---|---:|---|---|---|---|
| 枚举第一项 | -5778 | false | — | — | — |
| Step13 WFC默认体 | -5778 | false | false | false | true |
| 公开PFC默认体 | -5778 | false | false | false | true |

三个包装句柄均非零且彼此不同。本报告以HANDLE_ENUM/HANDLE_WFC_DEFAULT/HANDLE_PFC_DEFAULT代号展示，不重新发布具体运行地址；原件散列在manifest，原始值留在输入证据。Java同对象比较仅在两条“第一项↔默认体”记录中提供，不补造WFC↔PFC的Java对象比较。

例外/不可用探针均0；单位名/表达式mm，前后文件名/类型值1/版本戳3071/dirty=false保持，活跃模型结束仍同名。摘要READ_DIAGNOSTICS_RECORDED_ONLY，记录elapsedMs=13（摘要前计时约13.483 ms，不含其自身全部写入、close和后续UI反馈），不是完整命令性能基准或全会话无写入认证。

## 第十三步为何会误阻断

第十三步same采用Java同对象或非零getptr相等，随后默认体组合门为：

```java
s.body==null || !same(bodies.get(0),s.body) || s.body.IsSheetmetal()
```

本次对应WFC路线中，默认体非空，Java同对象false，包装句柄相等false，因此same返回false、第二项!same为true。该组合门会停止在身份比较，后面的IsSheetmetal在旧代码中被短路；第十四步已将该状态读取独立，明确读到false。公开PFC路线得到相同机制。

这是当前候选身份门过严的实现问题：包装对象/getptr句柄不同，不能作为本场景实体体必然不同的依据。本轮在相同当前模型作用域、唯一枚举体、同ID且两种默认体原生状态一致的情况下，复现了句柄门导致误拒绝的机制。不能继续把该错误解释成“默认体空”“已经转换为钣金”或“试验厚度不合适”。

边界仍需保留：Step13原5行日志没有当时的体ID/句柄/原生布尔，本次是较后时间的新只读观测，不能补写成历史已记录子原因。当前证据强烈支持并重现身份检查假阴性，尚未独立认证整个SDK的getptr/ID/对象生命周期或所有模型的体身份契约；同ID本身也不等于完整归属证明。

## 独立核对

两条FIRST_BODY_COMPARISON的元数据与各自DEFAULT_BODY/ENUMERATED_BODY逐字段一致。独立复算Java/句柄/ID三轴及短路顺序，再将本轮实际primitive探针值输入交付的BodyIdentity纯Java类；两条结果完全一致，包括nativeSheetmetalValue=false、step13Same=false、FIRST_ITEM_IDENTITY_CHECK_FALSE。没有用假SDK或模型对象代替本轮读取。

本地Step14源码、6个class/薄JAR与交付manifest匹配；Step13源码/6个class也匹配原包，未修改其判断。它们是本地交付身份，不是现场实际加载散列证明；输入没有Java/SDK/加载class资源独立散列、完整stderr或截图。内部WFC适配在本轮返回成功，但internalContractCertified仍false。

诊断源码/编译调用清单没有模型写、保存、转换锁操作，日志也声明conversionLockAccessed=false/conversionDecisionMade=false。审核方没有访问现场一次锁；不能把这些自报及dirty=false升级成整个Creo会话独立零写入认证。

## 修复方向与控制边界

后续最小代码整改应把体身份与包装句柄拆开：保留当前目标模型/类型、唯一枚举体、默认体存在、模型作用域内稳定ID及原生状态等证据，按SDK身份语义核对归属；包装句柄只用于诊断，不能作为单独硬失败条件。不要不加作用域地用ID代替所有模型/面/体身份检查，也不要把这次比较结果直接当成通过其他单位/驱动面/厚度/确认门禁。

整改不能只改首次默认体门：Step13确认期间的默认体重检也使用same(current,s.body)，应一并审查并测试。模型身份、驱动面归属等其他same用途需要各自证据，不能全局把same替换成ID比较，以免混淆不同类型或作用域。

应为默认体为空、枚举项不可用、作用域/ID不一致、原生已钣金或状态读取异常保留独立错误码和记录，避免再次合并短路信息。新的判断需要离线纯逻辑/实际SDK编译核对后再评审，不能在真机通过删除检查临时放行。

本轮仅审核，无代码修改、不清Step13一次锁、不恢复点击资格、不自动重试转换。原一次点击已消耗；若以后要新的写测试，须由新的受控任务明确范围，不隐含在第十四步只读命令中。原全局壁厚未证明，驱动面、操作者试验厚度、两次确认、不保存和失败状态策略都不能因为本诊断而省略。

当前可以存档：本模型原生非钣金、默认体存在、单体同ID/不同包装句柄、身份门误拒绝机制复现。不能存档为转换成功、可转换生产、全局等厚、材料占据或壁厚已证实。本报告仅派生比较、审核与散列，不重新发布原JSONL、运行地址、XYZ/UV或截图。
