# P1 原生几何相对面距离第七步候选

日期：2026-11-06（Asia/Shanghai）。独立类p1.readonly.geometry.step7.P1ReadOnlyGeometrySamplerStep7，命令SISWReadOnlyGeometrySample7，显示名称Sample Read-Only Geometry Step 7。不覆盖Step6，不改Web、原业务命令、Ribbon、模型精度，不保存或转换模型。

## 新增功能

1. 在原Step6采样/粗配对之后，读取真实SDK的Surface.GetSurfaceDescriptor、描述符类型/取向、TransformedSurfaceDescriptor.GetCoordSys的原点与XYZ轴、CylinderDescriptor.GetRadius。原始数值/单位、公共接口不支持、null、异常及样本一致性失败如实记录。
2. Z轴作为平面法向/圆柱轴线候选，先与本轮3个原生点/法向做一致性检查；未取得官方坐标约定的独立认证，不盲目采用矩阵下标或把候选轴认证为实体外向。失败时不计算局部投影结果。
3. 每类型最多取已发布粗候选中的前16个，平面/圆柱交错执行。平面用两面各3个原点及5个公共投影框内部网格提议；圆柱用两面各3个原点。每个源点和映射后的目标点都重新执行原生参数反求、双INSIDE、精确返回UV、有限点/法向与XYZ回读检查。
4. 分开记录解析支撑面候选间隙、两面原生域内点对应的局部距离和最终壁厚。局部距离只在两个点均通过完整门禁后产出；它不是全局最小距离，也未检查中间连线是否穿实壁。最终壁厚始终NOT_ASSESSED，不宣称等厚或可转。

有限原生见证点支持的是离散修剪域内对应验证，不是完整连续重叠区域的精确布尔重建；entireTrimmedOverlapRegionProven=false。没有将263个粗提示改成厚度配对，也不把UV矩形/投影盒当真实材料域。

## 验证状态

真实otk.jar、Java25编译无错误无警告；两次干净编译18个class散列一致，薄JAR与独立class字节一致。实际58项PTC符号在逐项允许集合内，比原46项新增12项：解析getter/原生UV反求及Point3D.create/set参数对象构造。没有模型Set/Save/再生/转换API，Point3D.set仅用于新建输入参数，不写模型几何。

原6439项纯断言与新增94项解析数学断言通过，共6533项（含逐候选性质检查，不是6533个独立场景或真机测试）。SDK签名、字节码、重复编译、数学测试与原Step6冻结路径核对证据在evidence。没有SDK Stub、模型Mock、Creo执行或部署。class major61，SDK依赖major69，因此运行仍需Java25；SDK/JDK不随包。

现有Step6日志没有解析描述符/反求UV结果，不能从旧263个提示推算本轮成功间隙、局部距离或真实域内见证。本包全部Step7实测结果为NOT_EXECUTED/UNKNOWN；旧数据选择计划仅供对照，不是新运行成功列表。

## 预算与交付

原采样≤300面/3000尝试/900最终点、质量追加≤32/面、35点池、原非共线阈值保持。新域验证单独计数，每类型≤16对、最多768条原生点验证链；默认32对最多提出544条链，失败时通常更少。全部阶段共享原30秒软预算，不能硬中断单次SDK调用，超限/未访问/未知保持PARTIAL。

包内9个源码、18个class、薄JAR、独立注册模板/消息、SHA256SUMS、manifest、javap、纯测试/冻结证据、旧数据对照计划和源码审阅diff齐备。详细接口来源、数值策略、真实验证范围、失败分支和日志兼容见docs。原revision严格身份边界保持；所有新增点证据仍等待最终COLLECTION_SUMMARY，不独立证明零写入。
