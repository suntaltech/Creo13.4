# P1 原生几何壁厚证据第八步候选

日期：2026-11-06（Asia/Shanghai）。独立类p1.readonly.geometry.step8.P1ReadOnlyGeometrySamplerStep8，命令SISWReadOnlyGeometrySample8，显示名称Sample Read-Only Geometry Step 8。本轮只增加已有局部距离的分类与SDK接口静态风险说明，不计算最终壁厚，不保存/转换模型，不修改Web、原业务命令、Ribbon或模型精度。

## 实现

第七步采样、解析、双INSIDE、投影/距离计算与预算保持原代码，仅命名空间变更。新阶段旁路观察自身已输出的间隙/距离/摘要事件，在局部证据阶段后给每个选中候选输出多标签分类；不会因为某个标签修改或丢弃原见证。

零距离/重合点映射、非平行平面、非同轴圆柱、远距离、单向见证、无见证分别记录，可重叠。远距离以明确的>10 mm审阅阈值标记，只是分组提示，不是壁厚上限、模型公差或过滤门禁；非mm/未知单位不自动换算，保留UNKNOWN。原生INSIDE和非共线阈值不变。

真实SDK的公开签名核对发现WModel.ComputeRayIntersections、WSolid.ProjectPoint、WPart.FindGeometry、FindShellsAndVoids及干涉/间隙查询等候选接口。只确认签名存在，材料占据/射线命中排序/遮挡语义、许可、实际模型类型兼容和只读语义仍未认证。本轮不调用、不强转到这些新WFC接口，不构造射线、不调用干涉计算。GetMaterial仅是材料属性，不能当作空间占据判定。

## 验证

真实otk.jar/Java25编译无错误无警告；两次干净编译21个class散列一致，薄JAR与独立class字节一致。实际58项PTC方法引用与Step7完全相同，没有新增原生调用。class major61、SDK依赖major69，运行仍需Java25；SDK/JDK不随包，pfcasync不使用。

原6533项纯断言和新增79项分类/风险目录断言通过，共6612项（含逐候选性质检查，不是6612个独立场景或真机测试）。原8个纯类、采样/域验证/局部距离方法块冻结核对通过。无SDK Stub、模型Mock、Creo执行、注册或部署。

另用Step7实际32对记录做纯分类回放：零距2对、非平行平面7对、非同轴圆柱8对、>10 mm审阅提示18对、单向见证8对、无见证4对；这些标签重叠，不能相加作为32对的互斥分组。双向20对、仅A→B 2对、仅B→A 6对、有限探测无见证4对。所有记录仍NOT_ASSESSED最终厚度，不因1.2 mm或任何距离出现多次而选定全局壁厚。

公开SDK字节码只读名称索引覆盖1612个public-facing类/8076个public声明方法，另核对20个相关公共类型的javap签名。名称索引不是完整语义证明；没有确认直接点在实体材料内/线段遮挡的可用契约，不等于所有SDK都不存在这种能力。

本候选Step8真机结果全部NOT_EXECUTED/UNKNOWN。旧Step7分类回放不是新采样，也不检验新的射线/占据/遮挡操作。交付含11个源码、21个class、薄JAR、独立消息/注册模板、manifest、SHA256SUMS、javap、分类规则、接口风险清单、失败分支及旧记录回放。原始XYZ/UV日志、截图、SDK/JDK不重新分发。

完整重叠区域、材料占据、遮挡、内外拓扑和最终厚度均未证明；原revision/严格身份未知继续PARTIAL。任何新分类都不能升级为等厚、可转或独立零写入认证。
