# P1 原生几何壁厚证据第八步真机材料审核

审核日期：2026-11-06（Asia/Shanghai）。输入native-readonly-step8-verify-20261106.zip。本次只解析运行记录、查看截图、独立复算分类并回放无SDK纯Java代码，没有执行Creo或新模型API，没有修改候选、生产配置或生成新class/JAR。

## 结论

第八步的分类与静态接口目录输出有本轮材料支持：32对候选全部输出可重叠标签，计数一致，未发现异常分类输入或观察器溢出；12条SDK风险目录与交付源码一致。第七步已有采样、解析与局部距离证据保持。

这些结果不是材料占据、射线/遮挡运行验证或最终壁厚结论。10 mm只作审阅分类，不是厚度上限、模型公差或INSIDE门禁。全局壁厚仍NOT_ASSESSED，等厚证据不足，总体PARTIAL保持。

| 六类审阅标签 | 对数 |
|---|---:|
| 零距离/重合点映射 | 2 |
| 非平行平面候选 | 7 |
| 非同轴圆柱候选 | 8 |
| 任一局部距离>10 mm | 18 |
| 单向见证 | 8 |
| 本轮有限探测无见证 | 4 |

这些维度可重叠，六项合计47不是候选总数，实际仍32对。不能按某一优先标签掩盖其他几何/覆盖边界。逐对数据见32-candidate-classifications.csv。

JSONL共6430条，requestId=b1fa712a-08fb-491a-ab28-b8cad5d40e35，errors=0、unknownReadValues=4、无截断。采集报告耗时7732毫秒；截图显示Step 8 sampling ended，未显示未响应状态。操作者在CONCLUSION中记载“加载命令时短时未响应，随后恢复”，该情况作为报告保留，根因尚不能由现有证据确定，见response-observation.md。

## 旧阶段保持

剔除新增48条分类/目录事件后，6382条第七步旧事件逐条比对，在排除时间、请求标识、命令/阶段、点求值时间戳和耗时后，所有旧字段一致。新增字段另行核对，没有借“旧字段相同”认证现场二进制身份。

126组平面数值非共线、原16组面积恢复至Step4的结果保持；原32组最小点距下降的边界也保持。666个最终点、2702次原采样尝试、222个解析参数面、196条局部距离记录及544条域验证链保持，28对有局部见证、4对没有。468条域链接受、76条目标域外拒绝不变，未因分类放宽原生INSIDE或数值门槛。

距离仍从0至约389 mm，包含重合映射和远距离。196条是可能含双向/重复位置的记录，不是196对独立面或完整材料覆盖；1.2 mm重复出现没有被选择为全局厚度。

## 分类规则核对

根据本轮原PAIR_LOCAL_DISTANCE_WITNESS、支撑间隙及逐对摘要，独立重算全部32对的见证总数、严格零/正计数、最小最大值、A→B/B→A、法向提示、六类标签和摘要一致性；再用交付纯Java分类器输入本轮相同数值，所有字段与运行分类一致。

方向维度为双向20对、仅A→B 2对、仅B→A 6对、有限探测无见证4对。这个维度可互斥统计32，但不能将它与零距/几何/远距标签混合求和。存在近反向法向样本的有21对，近同向样本有7对；均非实体外向或材料夹层认证。

所有32对probeCoverageState=BOUNDED_PROBES_RECORDED，sourceDomainEvidenceMayBePartial=false，本轮没有未访问或解析未知未探测的分类。4个无见证条目的min/max仍UNKNOWN_NO_RECORDS，未伪填0；far标签也保持UNKNOWN_NO_DISTANCE_RECORDS，而不是把“没测到”当作“不远”。这些4对没有因此被判为无真实重叠。

本例单位全mm，>10.0严格标记，恰等于10不属于该标签；这是已声明审阅政策，不是厚度允许值。其他单位/缺失摘要/部分探测等分支本轮没有实际触发，不将纯逻辑检查等同于这些真机场景均已验证。

32条分类全部保留outwardNormalCertified=false、independentOrUniqueSpatialLocationsCertified=false、materialOccupancyVerified=false、occlusionVerified=false、trueOppositeFacesConfirmed=false、entireTrimmedOverlapRegionProven=false及globalWallThickness=NOT_ASSESSED。invalidPairInputs=0、observerOverflow=false、summaryCountConsistency=true与逐对记录一致。

## SDK目录：输出正确，不是运行验证

12条SDK_READONLY_INTERFACE_REVIEW_ITEM及摘要用交付的无SDK目录类精确回放，能力名、公共类型、签名、风险说明、来源SDK散列和所有未确认标记完全一致。12是目录条目数，不是12项已验证可只读运行的能力。

射线ComputeRayIntersections、Ray_Create、ProjectPoint、FindGeometry、FindShellsAndVoids、净距/临界距离/干涉与Selection命中解释等，只被标为所供SDK的静态签名线索。GetMaterial明确为属性分配，不是世界点/线段的材料占据。直接材料占据与遮挡契约仍未确认，不等于所有SDK不存在这种能力。

所有目录runtimeInvokedByThisStep/runtimeObjectCompatibilityTested/runtimeSdkIdentityAuthenticated/readOnlySemanticContractCertified/licenseAndAvailabilityConfirmed/materialOccupancyProven/occlusionProven均false；摘要也未声称新射线/材料/遮挡调用发生。交付候选字节码58项PTC方法引用与Step7相同，未添加这些调用。输入没有现场二进制散列，不能仅由日志标志证明整个会话所有插件行为。

目录声明的离线核对范围仍为20个相关公共类型的javap及1612个public-facing类/8076个public声明方法的限定名称索引。索引不是语义穷举证明；来源SDK散列是离线输入，不是当前现场加载JAR的独立认证。double参数含义、单位、排序、射线方向/自命中/切触、修剪体作用域、许可和只读语义等应继续保留未确认，不能因目录已出现于真机日志就自动启用新原生操作。逐项目录见12-interface-review-items.csv。

## 短时未响应的范围

操作者报告应保留，但本包只有结束截图，没有卡顿期间录屏、线程栈、注册/类加载时刻或UI响应计时。采集elapsed=7732 ms，COLLECTION_STARTED到COLLECTION_SUMMARY的墙钟跨度约7706.943 ms；分类开始到摘要约24.062 ms，静态SDK目录开始到摘要约3.988 ms。各区间不是完整CPU剖析，部分准备/加载在区间外，不能相加或直接归因。

本实现仍使用同步命令回调和大量旧阶段采集/逐行日志，UI等待是需要核对的方向，但不能据现有记录认定根因、准确停顿时长或将其归咎新分类。也不能因为最终ended就忽略加载停顿。建议先增加生命周期/阶段观测，再考虑有界的非原生日志优化，不在没有SDK线程契约时将原生模型API移到任意后台线程。本次未修改线程、重试或现场配置。

## 身份与最终边界

revision仍三次null、绝对精度UNKNOWN共4条READ_UNAVAILABLE。版本戳3071/dirty=false/原句柄匹配只说明已观察上下文未变化，不替代缺失revision或证明独立零写入。MULTI_LABEL_REVIEW_ONLY与STATIC_SUPPLIED_SDK_SIGNATURE_REVIEW_ONLY只说明新阶段完成，其余严格身份和材料/区域边界继续存在，最终PARTIAL正确。

原始包没有实际加载class/JAR、注册与资源文件散列。数学/目录回放支持行为一致性，不能认证二进制逐字节相同。没有新增射线、材料占据、遮挡或最终厚度的独立运行证据，未证明完整修剪重叠或可转。

## 后续建议

可冻结Step8作为本模型的分类和静态目录已观察基线。若继续，应先明确最小只读接口语义/对象/许可验证的范围；若优先处理响应问题，应先补充注册与命令/阶段时序。两者都不应通过降低INSIDE、改模型精度、直接取1.2 mm或改原生调用线程来替代证据。建议见next-step-proposal.md。

本次只交付聚合报告、32对分类、12项目录、响应观测及散列；不重新分发完整XYZ/UV/解析参数日志或截图，不修改插件、模型、Web、业务命令或Ribbon。
