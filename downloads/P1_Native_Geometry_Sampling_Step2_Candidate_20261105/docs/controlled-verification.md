# 后续静态审核与受控真机验证

本文件不是新的GUI/部署授权。本轮未执行模型读取、注册应用或采样，后续须在明确受控范围内操作。

1. 校验ZIP.sha256、内部SHA256SUMS、manifest、源码/8个class/薄JAR及本机真实otk.jar身份。使用可加载major69 SDK的Java25；不要因插件major61改用Java17。SDK/JDK不随包。
2. Step1保持原样，用独立新classes目录及新注册名，模板指向step2类和resources/text。占位符不可直接加载。JAR仅包含同一8个class、无SDK/Main-Class，不能java -jar运行。不要覆盖BatchRename或Step1类、Web、生产配置/服务脚本或Ribbon持久化。
3. 首先确认独立命令注册和消息解析；start只注册，不采样。命令触发方式在本机受控范围内明确，不为触发而写入持久化Ribbon。只用既有测试普通零件，不点任何转换或保存。
4. 首轮保留对带孔/修剪PLANE、CYLINDER周期缝的人工交叉检查。核对候选INSIDE、边界/域外拒绝、输入返回UV一致、点有限、法向非零/单位化；缺少此类边界观察就保持分支未验证，不为凑覆盖强制制造SDK异常或修改模型。
5. 预计当前样本有126 PLANE+96 CYLINDER，但以实际新枚举为准，不硬填222目标或666点。目标每面3点，受25候选、300面/3000尝试/900点及30秒软预算限制，可能出现合理PARTIAL。不能要求在孔/缝不满足时仍凑满。
6. 保存JSONL和实际class/SDK/资源/dat身份及模型前后观察。点记录先是待最终身份检查的证据；本机原生revision可继续null，版本戳独立核对，不把PARTIAL手工改通过。点/法向是零件模型坐标原样，未做实体外法向统一，也未验证厚度。
7. 遇到模型身份/dirty变化、卡住或异常状态，停止后续测试并保留部分文件，不Save/Undo/删除/再生/自动重试。软预算无法安全强制中断原生调用。卸载/配置恢复按既有授权独立留证，不把恢复写进采集器。

新增SDK调用是GetVersionStamp、Surface.GetOrientation/GetUVExtents/VerifyUV/Eval3DData、UVOutline.get、UVParams.create/set/get、SurfXYZData.GetParams/GetPoint/GetNormal、Point3D.get、Vector3D.get、Placement.getValue及方向枚举getValue。UVParams.set只填新建输入参数，未调用模型setter；完整实际PTC符号见evidence/compiled-sdk-calls.json。javap签名证据不是所有边界语义/运行支持的证明。

测试证据为纯函数/数值门禁检查，不含SDK Stub、模型Mock、HTTP或真机样例。下一阶段只有在真实点/法向与上下文核验后才讨论相对面距离/厚度，不在本轮推断等厚或开启转换。
