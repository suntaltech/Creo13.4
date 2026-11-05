# 相对面配对策略及失败分支

本阶段输出粗证据候选，不确认真实相对面。只比较同一已完整枚举、唯一归属的SolidBody中，同真实原生类型PLANE或CYLINDER的两个不同面；每面必须有本轮最终3个有效点/法向及已知单位。跨体、类型不同、归属未知/多归属、点不足等不进入候选比较，缺失合格样本数在摘要保留，不以未发现候选断言无相对面。

输入引用本轮POINT_NORMAL_SAMPLE的面ID/attemptOrdinal，法向保持SDK原方向，不认证实体外法向。输出9组采样法向点积的min/max/maxAbs；|dot|max≥0.95仅为新的宽松候选过滤策略，不是模型容差，也没有改变原非共线阈值。近反向或近同向只描述采样法向关系；同向法向也可能因SDK取向策略出现，不能直接认定材料内外。

选面A第一个采样法向建立公共切平面基，投影两面的3点，求各自二维采样包围矩形及正面积交集候选。只做切向坐标/区间比较，不输出两面法向分离距离或厚度。矩形来自3点，可能跨孔/修剪外部，既不能证明实际材料重叠，也不能用无交集排除整个面重叠；输出projectionDoesNotProveTrimmedMaterialOverlap=true。

圆柱也只记录局部采样法向和同一投影框提示，未读解析轴线、圆心、半径、角域或轴向覆盖。cylinderAxisCoaxialityAndRadii=UNKNOWN_NOT_ACQUIRED，不能把候选理解为同轴圆柱、半径差或壁厚；相同/相反局部法向不足以证明同轴。

失败与限制：

- 无效/不足/非有限样本、零法向：UNKNOWN_INVALID_SAMPLE_DATA，不输出确认。
- 投影数值溢出：UNKNOWN_PROJECTION_NUMERICAL_RANGE。
- 采样投影矩形退化：UNKNOWN_DEGENERATE_SAMPLED_BOX，不补造重叠区域。
- 法向或采样框无粗提示：NO_COARSE_HINT_NOT_PROOF_OF_NO_OPPOSITE_FACE，不判定无真实配对。
- 可用面不足2：INSUFFICIENT_ELIGIBLE_SAMPLE_EVIDENCE。
- 全轮同类型比较最多16000次、共享原30秒软预算；触发限制记录PARTIAL_CANDIDATE_SCAN，整体不升级完整。
- 每类型只发布前64个候选，共≤128；其余数量在摘要candidateRecordsOmittedByOutputCap报告，顺序是首次遇到，不是最优或完整配对排名。摘要同时含全部已比较状态/候选计数与输出数量，不能把输出上限当全模型配对总数。
- 采样/配对抛异常或未进入时，最终relativeFaceEvidenceState标NOT_ENTERED_UNKNOWN/ABORTED_UNKNOWN，不凭缺失摘要补造0对结论。身份变动由最终COLLECTION_SUMMARY整体解释。

旧Step5点纯回放：222面，7875个平面对+4560个圆柱对=12435比较，135/128个粗提示，4181个投影退化UNKNOWN。输入是旧点且未重新验证修剪域，不是Step6新采样结果。trueOppositeFacesConfirmed=false、trimmedOverlapProven=false、thicknessAssessed=false始终保留。不调用新SDK方法或网络，不触发保存/转换。
