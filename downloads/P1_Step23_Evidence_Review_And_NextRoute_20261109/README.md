# P1 Step23 原始证据审核与路线调整

2026-11-09，Asia/Shanghai。本包为证据审核，不是新插件或真机探针。当前WFC ElementTree工厂路线继续阻断，不恢复Step20、不重试Step22/23、不清理一次锁。

## 已由附件核实

| 轮次 | requestId | 最后完整记录（sequence=212，probeId=86） | 上海时间 |
|---|---|---|---|
| Step22 | 13e3cc7e-37cf-4e6b-a09c-072f402f4af4 | ELEMENTS_CREATE_MINIMAL_START / Elements.create | 2026-11-09 13:36:26.668617600 |
| Step23 | 8695d93b-5613-4d2e-a9b8-f03778bbd17f | ROOT_ELEMENT_CREATE_START / wfcElementTree.Element_Create | 2026-11-09 13:55:03.958621500 |

Step23根参数为PRO_E_FEATURE_TREE（实际elementId=386）、value=null、level=0。两轮最后记录均无对应DONE/THREW，没有FINAL_RESULT；两份日志各212行，单requestId，sequence从1连续至212、elapsedNs不递减、末尾有换行、所有行JSON可解析。完整文件哈希在review.json。

各有两个未闭合probe：83为外层准备容器，86为最内层工厂。它们是嵌套未完成，不是两次原生重试或两个独立卡点。体/面选择创建均非空返回，默认体/原驱动面三轮检查通过，5特征[1,3,5,7,69]三轮状态均ACTIVE（15条FEATURE_STATE_DONE），版本3071、初始dirty=false。两次确认已接受，显式输入厚度1.2mm。五类WFC失败flag未检查。

附件7个源码与交付Step23提交eb8b330519fb8bf91cc0d973da09c99632e212f1的源码逐字节一致；Step23没有Elements.create/append或模型创建/再生/保存路径。日志符合绕开Elements.create后转入独立Element_Create的流程。包内未提供现场class/JAR、实际加载散列/注册文件，不能将源码一致升级为运行二进制一致认证。

## 结论边界

证据确认“最后可见边界从集合工厂推进到根元素工厂，仍未观察到返回记录”，足以暂停当前WFC ElementTree路线。它不能单独证明JNI内具体卡死点、进程确已崩溃、永久不会返回或所有模型必挂：调用返回后的审计写入失败、外部终止等也可能留下START。本附件无stderr/jlink、崩溃栈/hs_err、截图和外部未响应时间线，不能定位许可/JVM/二进制/宿主内部根因。

没有到达TREE_ALTERNATIVE_UNAVAILABLE，因为该纯日志在根非空返回后才执行；不能据其缺席推翻已核实的SDK重载结论。WCreateFeature/WRegenerate及XML建树均未由这份证据测试，不应写成它们成功或失败。未发现本命令创建/再生提交记录，匹配源码也无相应调用；不能由此证明整个Creo会话从未通过其他入口保存或修改。

## 下一步路线（离线评估顺序）

1. 固化本次负面基线与Step22/23锁，停止在当前宿主继续试WFC集合/根工厂，不自动恢复创建。
2. 优先核对实际加载的otk.jar、插件JAR/class、Creo版本与JVM、官方支持的启动/注册方式及Object TOOLKIT Java许可/初始化要求。现日志明确记录了内部WFC视图适配且internalApiRuntimeContractCertified=false；这不是证明其导致故障，但需在新实现前验证支持条件。用现有stderr/jlink/崩溃资料关联上述边界，不靠再点同一探针补证据。
3. 核对PFC替代路线。实际SDK确有Solid.CreateFeature(FeatureCreateInstructions)，但指令接口只有GetType()，本JAR公开PFC类常量池中只有该接口和Solid引用它，未找到可用于钣金转换的公开具体指令构造类型。不能伪造该接口的Java实现、用Mock指令或只改方法名宣称可转换。此负面结论只针对当前JAR公开PFC面，不等于断言所有PTC产品都无能力。
4. PFC CreateUDFGroup签名存在，但没有提供可审核、可用于现有实体转钣金的UDF模板/引用映射，不把“能插入UDF”当作“能转换此实体”。XML建树是另一个WFC方法，是否避开同一问题未知，本轮不自动尝试或声称已解决。
5. 若官方支持条件核验仍无法提供稳定的WFC路线，再评估完整Pro/TOOLKIT C/C++受支持的转换元素树接口、匹配版本SDK/许可、官方可编译示例及独立受控测试方案。当前审核未认证其SDK/许可就绪，也不保证会成功。不引入UI宏/自动点击或保存作为未经验证的替代。

本包没有发明新的Step24可执行补丁。下一轮应先完成运行环境/接口可构造性证据，再定义一个具体受支持的最小探针；不要继续生成只有方法名变化而缺少可用指令的转换代码。

## 包内容

review.json为派生审核数据（原件哈希、序列配对、末端边界、源码比对和限制）；sdk-route-assessment.json与sdk-route-signatures.javap.txt为实际SDK静态核对；manifest.json与SHA256SUMS用于整合。未重分发原JSONL、截图、模型、SDK/JDK、运行时句柄地址或坐标。未执行Creo/SDK模型操作、部署、发送外部消息或更改已有锁。
