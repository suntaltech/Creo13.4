# P1 原生几何采样第三步候选

日期：2026-11-05。独立应用p1.readonly.geometry.step3.P1ReadOnlyGeometrySamplerStep3；新命令SISWReadOnlyGeometrySample3，名称Sample Read-Only Geometry Step 3。不替换Step1/Step2或现有业务类，不改Web、转换命令、Ribbon或生产配置。

## 最小改进

1. 每个实际处理的曲面记录原生GetUVExtents返回范围、数值有效性和来源；失败/限制保留UNKNOWN或未产出计数，不把UV矩形当修剪域证明。采样复用该范围，避免重复原生读取。
2. 仅当模型叶名为200002100.prt、面ID为2710/2738/2752/2843且真实类型PLANE时，基础25候选未达3点配额后，追加最多200个有界、去重的边缘偏置/层次候选。其他面仍最多25候选；目标面合计最多225。单位、范围或身份未确认时不伪造补点。
3. 平面样本新增数值非共线、XYZ距离/三角形形状及UV归一化分散指标。这轮新增指标，不反向改写Step2有效点定义，不承诺所有三点已分散；质量不足可以与有效点并存。

原生VerifyUV必须两次INSIDE，返回UV精确匹配、点/法向有限非零、单位已知、同面XYZ不重复等门禁保持不变。边界/域外永不接受。只读，不写/保存/转换模型，不计算厚度。

## 四个面的结果状态

Step2基线四面均0点/25尝试。本轮云端未执行模型采样，因此2710、2738、2752、2843的Step3结果全部为NOT_EXECUTED/UNKNOWN，不预填成功。后续真实运行的TARGET_REFINEMENT_RESULTS逐面报告基准达配额、细化后达配额、部分点、无点或未尝试/不适用等状态，结合最终身份与PARTIAL解释。详见[细化与结果表](docs/refinement-and-target-results.md)。

## 验证与交付

真实otk.jar、Java25编译无错误无警告；两次干净编译8个class散列一致，薄JAR内class与独立文件相同。class格式major61，SDK依赖major69，因此运行仍需Java25。SDK/JDK不随包，pfcasync不使用。

506项纯检查（19项基础、59项原采样数学、428项第三步性质/质量检查）通过，包含候选上限/唯一性/范围、4面策略匹配、数值质量、未知/非有限值处理。数量包括逐候选性质断言，不代表506个真机测试。无SDK Stub、模型Mock、HTTP、Creo执行；字节码46项PTC方法引用均在原采样允许集合内，没有新增模型写方法。

另用Step2的真实122组平面点做纯算术回放：38组直接叉积为0，新的数值质量门槛总计标记48组（含另10组数值近退化），其余74组非共线。该结果来自旧点，不是Step3新采样或物理模型容差判定，未重新发布原始坐标。

包内source、8个class、薄JAR、独立注册模板/消息、manifest、SHA256SUMS、javap与测试/回放证据齐备。patches/source-review.diff为源码对比审阅材料，不是可覆盖旧运行目录的安装脚本。详见[质量指标与兼容性](docs/quality-and-log-compatibility.md)及[受控验证](docs/controlled-verification.md)。

本候选未注册、未部署、未运行Creo或产生新运行requestId。revision仍直接保留SDK值，versionStamp独立，不以其消除旧严格身份PARTIAL。没有等厚/可展或转换能力结论。
