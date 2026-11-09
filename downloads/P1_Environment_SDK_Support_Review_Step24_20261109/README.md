# P1 第二十四步：环境与SDK支持条件离线审查

日期：2026-11-09，Asia/Shanghai。范围：仅核查已有材料和真实SDK静态签名。本轮没有新按钮、Java插件、dat、模型写入、现场运行、重启或恢复转换；Step22/23一次锁保持原样，当前config.pro仍启用Step23 dat仅为任务单报告，未改变。

## 审查结论

继续保留已稳定的Java界面与PFC只读采集；暂停当前内部WFC桥接下的ElementTree工厂及转换尝试。第一优先核对J-Link/OTK运行模式、许可/官方初始化契约与内部WFC视图适配，第二优先核对现场SDK/JVM/本地模块/类加载身份。现有证据不足以认证任何一项为根因，也不足以宣布Java平台本身不可用。

若官方支持的Java OTK运行条件能够合法满足并有同版本正式示例，可在离线证明构造方案后再设计单独受控任务。若支持条件不可满足，或在已认证环境中仍复现相同工厂无返回，建议把转换内核候选转为C/C++ Pro/TOOLKIT评估，保留Java UI/只读层；当前尚未具备可交付的C++转换实现基础。

Step22/23原始日志各212条，最后分别停在Elements.create和Element_Create(PRO_E_FEATURE_TREE,null,0)的START，无DONE/THREW，三轮体/面与5特征ACTIVE检查完成。两个未闭合probe中一个是外层容器，一个才是最内层工厂，不能计为重复重试。只能确认“未观察到返回记录”，没有崩溃栈/外部时间线，不能据此证明永久卡死或锁定JNI内部根因。

## 1. 实际环境：已知与缺口

本轮没有附新的现场环境身份包。下表的未知项不能用云端路径、旧报告或部署模板填充。

| 核查项 | 当前可用材料 | 当前现场结论 |
|---|---|---|
| Creo版本 | 2026-11-05历史操作者报告13.4.0.0 | 未核对当前映像/维护版本 |
| Creo安装路径 | 历史官方文档索引提及D:\Program Files\PTC\Creo 13.4.0.0\Common Files | 只是历史文档路径，不是当前运行映像路径证明 |
| 实际加载otk.jar路径/大小/SHA256 | 仅有上传SDK的云端副本，见下表 | 当前加载来源未知 |
| 插件JAR/class部署路径/散列 | 交付候选和本机上传7源码一致；未附实际class/JAR/加载散列 | 部署目录、已加载二进制未知 |
| JVM版本/供应商 | 云端编译JDK为Oracle 25.0.5.0.3、Linux x86_64 | 不是Creo宿主JVM证据，现场版本/供应商/位数未知 |
| protk.dat与text_dir | 交付模板为startup java，text_dir/classpath均占位符 | 实际dat正文、解析路径未知 |
| config.pro相关配置 | 任务单称Step23 dat仍启用 | 未取得有效配置全文/相关行及覆盖顺序 |
| 启动命令/classpath | 候选编译只用otk.jar，不用pfcasync.jar | 现场JVM classpath和启动命令未知 |
| 模块加载顺序 | 命令开始后的调用日志 | 调用次序不是模块加载顺序，加载身份/先后未知 |

机器可读缺口及每项需采集的只读证据在actual-environment-matrix.json；current-runtime-evidence-template.json全部未采集值为null，不是验证通过的清单。

## 2. 可复核的离线SDK和候选身份

| 离线文件 | 字节 | SHA256 |
|---|---:|---|
| otk.jar | 4362992 | 0f95b0cf9c88792c0bd18aff4e7a3ef730fdc3d7924ffe681779a40bfcda2319 |
| pfcasync.jar | 1207086 | b1765537e2754332fa923e418b81541e3caa2d2a197ebec4ae1decb9ab8bfdb3 |
| Step23候选JAR | 39698 | f6bfe8d32b8560a11a9e4c382d3a9f968734450f649ea68a1fb02ce53f649908 |

SDK云端副本目录为/workspace/r8-next-binary/sdk-input/creo134-jlink-sdk-jars-20261105。该Linux路径不能替代Windows实际加载路径。Step23候选JAR为lib/p1-root-element-probe-step23.jar；实际部署可能采用散装class，需逐class比对，不能只比较磁盘JAR。

otk.jar的3429个class为major69、116个为major51；pfcasync.jar的1816个为major69，其余为major50/51。两JAR的Manifest均为Created-By: 25 (Oracle Corporation)。这只反映离线工件格式/构建元数据，不能认证PTC官方来源或现场JVM供应商。多数类需要能够加载Java25字节码的JVM；插件自身major61不意味着可用Java17。没有同版本官方支持矩阵，不能据此承诺PTC支持Java25、建议降级JDK或随意使用更高版本。

两JAR有1076个同名class，其中431个字节不同。这是离线冲突风险，不证明现场混装。需核对是否同时加入classpath、先后顺序、实际CodeSource及重复版本；不能用“补加pfcasync”当作修复。编译候选只用otk.jar，未把SDK/JDK随包分发。

交付模板内容完整收录在evidence/offline-sdk-and-artifact-metadata.json：name=p1s23、startup java、java_app_class=p1.controlled.step23.P1RootElementProbeStep23，java_app_classpath/text_dir为显式占位符。源码使用GetCurrentSessionWithCompatibility(C4Compatible)，再以setAccessible/内部实现(long)构造取得WPart/WSession视图；日志明确internalApiRuntimeContractCertified=false。不要把句柄相同视为官方运行契约已满足，也不要未经核查直接把startup改成otk_java。

## 3. 最可能原因排序：工作假设，不是已证实概率

| 顺位 | 假设 | 排序依据与所需只读证据 |
|---:|---|---|
| 1 | J-Link注册/OTK许可及内部WFC桥接支持契约不匹配或未建立 | PFC回调可返回，多个WFC工厂/状态方法无返回记录；模板startup java，源码使用内部构造器。需要当前真实dat/config/启动模式、同版本官方兼容/注册说明、合法开发/运行许可与签名/解锁状态摘要。不能仅凭模板定性。 |
| 2 | SDK、Creo本地模块、JVM或类加载混装/版本不匹配 | 多数SDK类major69且两JAR大量同名不同字节，当前实际加载身份缺失。需要宿主JVM、otk与插件CodeSource/哈希、Creo/JNI模块版本、classpath与加载顺序。未证明实际冲突。 |
| 3 | 调用上下文、初始化顺序或线程契约不满足 | 源码是同步PFC回调，WFC视图未认证。日志均为Java线程ID=3，但不证明Creo原生线程条件满足。需要官方同步/线程规则、现有启动初始化记录和栈，不凭猜测换线程/异步连接。 |
| 4 | 特定版本原生缺陷或宿主异常 | 两种最小工厂都无返回记录。需要准确维护版本、已有stderr/jlink/hs_err或dump受限摘要及官方问题公告；目前没有“已知13.4缺陷/指定修复版本”的依据。 |
| 5 | 模式或根参数前提 | 尚无官方根null/level规则全文。集合工厂无模型参数、根也未提交业务树，使模式/业务元素错误的解释优先级较低，但没有证据彻底排除。不能先切钣金模式试错。 |

详细依据、限制和每项所需证据见hypothesis-ranking.json。日志缺少DONE也可能来自返回后审计失败或外部终止；本轮未接触现场，不能把工作假设升级为崩溃原因。

## 4. WFC支持条件逐项判断

| 条件 | 本轮判断 |
|---|---|
| 必须先进入钣金会话/模式？ | 未证实。根工厂签名不带模型/模式参数，但签名不能排除文档前提；不建议作为下一个现场试错动作。 |
| 必须先初始化某接口？ | 当前wfcElementTree公开工厂声明未标出专门初始化入口，未取得官方流程全文；不能断言无初始化要求，也不能发明初始化调用。 |
| 必须异步连接？ | 没有依据。pfcasync是另一绑定/运行路线，不应为工厂卡点直接加JAR或改连接方式。 |
| 必须特定线程？ | 官方规则待取得。只有Java线程ID=3的一致性，缺少原生线程关联；不以新线程、InvokeLater或线程池试错。 |
| 额外许可/内部API认证？ | 高优先级未核实。SDK存在XLicense/XNotInitialized/XToolkitAppNoLicense/XMethodNotLicensed等类型，但未见相关THREW，不能断言缺许可。内部反射桥并不自动获得官方支持。 |
| 已知版本兼容问题？ | 未提供同版本支持矩阵或缺陷公告；保持未知，不承诺切JDK/补丁可解决。 |

此前用户索引摘要提及OTK注册、J-Link兼容、解锁/签名/许可条件，但未附原始官方HTML。本轮只能据其定位需要的文档，不能冒称核对过全文。support-conditions.json保留证据等级。

## 5. PFC与其他候选路线

- Solid.CreateFeature(FeatureCreateInstructions)签名存在，但该指令接口只有GetType()。当前JAR公开PFC类中，常量池引用该接口的只有自身和Solid；未找到可直接公开构造的实体转钣金指令类/工厂。不能伪造FeatureCreateInstructions实现或用Mock对象代替原生对象。
- Session.UIGetCommand(String)是命令查找；UICommand声明只有SetIcon/Designate，没有强类型钣金转换执行入口。BaseSession.RunMacro虽然存在，但用户明确排除UI宏/自动点击，因此不作为替代路线。此结论只针对当前审查过的SDK公开面，不断言所有PTC接口都不存在转换能力。
- CreateElementTreeFromXML(String)属于WSession的公开WFC方法，是不同方法而非数组重载。接口存在不证明当前许可/初始化支持，也不证明绕开同一原生问题；没有同版本格式/官方样例或运行契约，不建议作为下一轮现场探针。
- CreateUDFGroup签名存在，但未提供可审核、适用于现有实体转换的UDF模板、基准/面引用映射、单位/厚度语义及许可条件。不能把插入UDF等同于实体转钣金。
- WSession.CreateElementTree仍只有Elements参数，Elements无公开数组构造器。不存在Element[]重载这一事实保持；不反射其(long)构造器、不伪造句柄或业务指令。

## 6. 是否继续Java，以及下一步最小只读探针

Java的UI/已验证PFC只读链路可保留；当前WFC写入方向暂停。下一步首先不是SDK工厂探针，而是PASSIVE_ENV_IDENTITY_V1身份资料：既有安装/文件/配置/启动日志/当前进程属性的只读收集与离线比对，零模型API、不新建按钮、不重载、不触发Elements/Element/XML/状态flag。

若这些资料仍缺少已加载类身份，未来独立任务才考虑在已有稳定回调中仅记录JVM系统属性及已加载类的CodeSource/classloader、线程名称/ID；不初始化新SDK类、不构造WFC视图、不调用任何新模型或工厂API。本轮只给设计，不生成/部署/执行该采集代码。详细字段及不应推断的结论见next-readonly-evidence.md。

本轮不需要重启Creo、不改仍启用的Step23 dat，也不再点击Step23。现有文件和既有启动信息可优先采集；如后续必须改变注册模式或重启，先核实官方流程并另立受控任务，不在本报告自动执行。

## 7. 若转C/C++ Pro/TOOLKIT的前置条件

先提供与准确Creo维护版本和目标位数匹配的完整C/C++ SDK头文件/导入库、PTC支持的Windows编译器/ABI、合法开发/运行许可及官方注册/解锁/签名要求、同版本驱动面转钣金元素树官方说明与可编译示例。当前工作区未检出ProToolkit.h/ProFeature.h或目标导入库，历史输入主要是Java JAR/javap；未认证C++开发环境就绪。

需先确定受支持的模型/体/面引用方式、主线程/回调与错误返回契约、创建/再生调用条件、失败后不保存/不重试的状态边界，以及用于比对的最小可丢弃模型。离线编译和静态审核后，才另定独立一次受控验证；不能把换语言当作自动修复，也不能先生成会反复卡死的新探针。

本轮没有生成新的Java/C++现场代码或配置，未宣称真实转换可用。
