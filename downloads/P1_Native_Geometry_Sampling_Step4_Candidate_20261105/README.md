# P1 原生几何采样第四步候选

2026-11-05；独立类 p1.readonly.geometry.step4.P1ReadOnlyGeometrySamplerStep4，命令 SISWReadOnlyGeometrySample4，显示名称 Sample Read-Only Geometry Step 4。源自Step3，通过有界原生有效点池改善退化平面的最终3点选择。只读采集点/法向；不计算厚度，不保存或转换模型，不改Web、Ribbon持久化、现有业务类及命令。

先按Step3取得原始3点。仅PLANE且初始数值非共线标志不为true时追加最多32次候选探测，最多保留9个同面有效点。所有新点仍须两次原生VerifyUV=INSIDE、返回UV精确一致、单位已知、点/法向有限且法向非零、同面XYZ不重复。固定数值阈值不变。候选池找不到合格且空间面积不退化的3点时保留原3点，明确记录未改善。

真实otk.jar与Java25编译通过、无警告；两次干净编译的10个class散列一致，薄JAR与独立class字节一致。46项实际字节码PTC方法引用全部在原采样允许集合内。原506项纯检查及新增548项池策略检查通过，共1054项断言（含逐候选性质检查，不是1054个真机测试）。无SDK Stub、模型Mock或Creo执行。class major61、依赖SDK major69，运行仍需Java25；SDK/JDK不随包。

48个面的Step4实际改善结果均为NOT_EXECUTED/UNKNOWN。真实Step3旧点纯算术回放126组，仍78组数值非共线、48组退化、替换0组；只有旧3点不能制造新原生点或宣称问题已解决。逐面状态见docs/48-plane-results.csv，后续以新JSONL的PLANE_POOL_SELECTION和PLANE_SAMPLE_QUALITY核对实测改善数。

交付包含4个Java源码、10个class、薄JAR、独立注册模板/消息、manifest、SHA256SUMS、javap、编译/测试/回放证据和审阅diff。没有部署、注册或执行新命令。revision仍直接保留SDK值，versionStamp独立，原严格身份未知时保持PARTIAL。模型未修改的字段观察不是独立的零写入证明；未宣称等厚或可展。

详细策略、日志兼容与后续受控检查见docs目录。模板含路径占位符，不能直接作为生产配置使用。
