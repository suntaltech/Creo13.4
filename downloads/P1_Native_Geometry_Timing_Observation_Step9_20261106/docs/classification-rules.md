# 候选分类规则

输入仅来自同一次运行中已输出的PAIR_ANALYTIC_GAP_CANDIDATE、PAIR_LOCAL_DISTANCE_WITNESS和PAIR_OVERLAP_VALIDATION_SUMMARY。新阶段不读历史XYZ当当前原生点，不重新求距离，不选择众数/中位数当厚度，不改原记录。最多32对、现有每类型16对和域链预算保持。

| 维度 | 规则 | 边界 |
|---|---|---|
| 零距/重合映射 | 原生回读点对应的localPointDistance严格==0 | 不新增近零容差，不证明整个面重合或零壁厚 |
| 非平行平面 | 旧支撑间隙状态NONPARALLEL_NO_CONSTANT_SUPPORT_GAP | 解析候选关系，不把变动点距叫平行壁厚 |
| 非同轴圆柱 | 旧状态NONCOAXIAL_RADIUS_GAP_UNKNOWN，或圆柱轴不平行 | 不将非同轴径向映射距离当半径差/壁厚 |
| 远距离审阅 | 单位严格mm且任一已记录局部距离>10.0 | 只是明示的审阅分组阈值，不过滤点或判定真实壁厚范围 |
| 单向见证 | A→B或B→A恰一侧计数>0 | 两端域内不等于双向覆盖；不推出无另一方向的真实对应 |
| 无见证记录 | 当前保留的见证数==0 | 必须同时查看是否实际探测，不能据此断言无重叠/无相对面 |

一对可同时属于多个标签，tagCounts总和可大于classifiedPairs。不存在以某一“主类别”覆盖其他风险的优先顺序。两向20/单向8/无见证4是本次旧数据回放的方向维度分组，不是全部几何分类的互斥划分。

距离采用已有有限非负值，零值严格比较。负数/非有限/面ID方向错误计入invalidRecords；摘要见证数或方向计数不一致标INCONSISTENT_OR_INVALID_CLASSIFICATION_INPUT，整体不升级完整。法向点积只形成“存在近反向/近同向样本”的提示，使用既有0.95候选余弦值；不会认证实体外向、材料夹层或法向穿越材料。

远距离政策只对mm生效。未知/其他单位为UNKNOWN_UNSUPPORTED_OR_UNKNOWN_UNIT；无距离记录为UNKNOWN_NO_DISTANCE_RECORDS，而不是false/0。不把10 mm当模型公差、薄壁标准或厚度允许值，不执行隐式单位换算；≤10 mm同样没有被认证为壁厚。

probeCoverageState区分有限探测已记录、部分探测、解析未知未探测、未访问/异常中止。无见证标签必须与该字段及sourcePairStatus一起解释，未执行不能填作测量失败。所有分类保留materialOccupancyVerified=false、occlusionVerified=false、trueOppositeFacesConfirmed=false及globalWallThickness=NOT_ASSESSED。

回放prior-step7-classification-replay.csv只使用已提供Step7的32对数值，含重合/非平行/非同轴/远距/单向/无见证。196条见证可能包括反向或重复位置，不认证统计独立性或空间唯一性，更不能把1.2 mm记录的数量换成全局厚度结论。Step8实际分类与原基线保持均待真机核验。
