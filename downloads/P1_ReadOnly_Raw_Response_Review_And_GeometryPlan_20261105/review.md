# 原始只读响应审核与真实数据回放

日期：2026-11-05。依据用户上传readonly-raw-responses-20261105.zip。解析和本地回放仅使用已提供JSON与现有纯评估模块，不发HTTP请求、不加载Creo、不修改插件。

## 结论

4组响应status.error均false；222项曲面ID唯一、面积为正有限数、包围坐标有限且范围有效。字段内容与此前截图及审计摘要相符，补齐了读取值的来源层证据。现有数据能验证曲面数、面积和包围尺寸，不能确认真实平面、相对面法向、壁厚、全局等厚或可展性。

不需要继续更换抽壳/驱动阈值；当前“状态未确认、不推荐转换方式”是这组输入的正确保守输出。下一步应推进只读几何证据采集，而不是再从同样的area/extent字段推导确定分类。

## 真实响应核对

曲面222项全部只有area、min_extent、max_extent、surface_id四个字段；没有真实面类型、法向、邻接、实体归属或厚度样本。最大返回面积对应ID2708、约128863.439816 mm²。实际包围跨度约337.500000000402、75.75000000000009、388.9999999998 mm，与页面三位小数显示一致。fileinfo给出模型200002100.prt、revision1，单位响应为mm。

用历史“任一轴extent跨度≤0.01”规则对这组原始值复算，恰好得到118。这解释了此前候选计数的来源：它是轴向包络启发式，不是SDK面类型查询。旋转、退化面或曲面包络均可能影响此值；不能把118命名为真实平面数，也不能据这个数判断钣金形态。

使用当前已交付sheetmetal-assessment.js原字节对真实响应回放：保留model/revision/units、surfaceCount、最大有效面积记录和spans；nativeSheetmetalState仍UNKNOWN，薄壁/等厚仍INSUFFICIENT_EVIDENCE，estimatedThickness/suggestedMode仍null。结果见evidence/real-response-replay.json。此次不是合成输入测试，但仍是离线回放，不是新的真机读取或几何识别验收。

## 来源与限制

包内JSON为4组响应体，README称通过本机CREOSON直接调用采集。没有原始请求body、采集时间、与上一分析requestId的关联，也只有一份fileinfo而非前后两份。因此本轮可核对数据一致性，不能声称它们就是上轮同一HTTP事务，也不能独立排除采集期间模型变化或认证未发生模型写入。

原fileinfo还含本机dirname路径。本交付不重新分发原JSON或该路径，只发布必要的汇总、散列、离线验证工具和SDK方法签名。当前验证工具只读取用户本地原件，输出时不包含dirname或222项原始曲面记录。

## SDK能力已核实到什么程度

已对用户提供的真实otk.jar做javap静态签名核对：Surface存在GetSurfaceType、Eval3DData(UVParams)、GetUVExtents、GetOrientation等；SurfXYZData存在GetPoint和GetNormal；SolidBody存在IsSheetmetal、ListSurfaces等。具体签名见evidence/real-sdk-read-signatures.javap.txt。

这些是实际依赖中的接口证据，不是运行可用性或采样正确性证明；不臆造GetSurfaceNormal等新方法。接口存在不说明任意UV都有效、法向一定可用、边界重叠已检查或数据足以确认壁厚。后续只读采集规格见geometry-evidence-next-step.md。

## 本轮完成与后续

当前Web及导航二进制继续冻结，不修改core、Java或转换入口，不生成新的识别候选。已完成原始数据结构核对、222项基本有效性检查、旧118计数复现、真实输入模块回放，以及SDK签名定位。

下一轮建议先做“原生状态与真实面类型”的独立只读证据采集，再处理有效采样点/法向/相对面距离；不要一次把采集、识别、转换和保存混入一个改动。本轮没有新增或执行采集命令，未来真机行为仍按相应明确范围进行。

## 本地复核

使用Node运行node verify-raw-responses.cjs <用户原始响应JSON路径>即可离线核对本组响应并回放reference中的原评估模块。无网络或Creo依赖；输入格式为此次4组响应对象，不是通用SDK返回格式。reference模块是已交付代码的原字节，不是应重新部署的新文件。
