# P1 第二十六步：WFC许可与使用边界审查

2026-11-09，Asia/Shanghai。明确决策：停止当前“PFC兼容会话＋内部构造器桥接＋Java WFC ElementTree”的建树/转换路线，状态BLOCKED_WFC_SUPPORT_CONTRACT。保留已验证且符合自身授权条件的Java UI/PFC只读层。可以进入C/C++ Pro/TOOLKIT前置离线评估，但不立即改写或部署转换内核。

“停止”是本项目当前工程路线的阻断决定，不是已证明现场无许可、违反许可或永远不能使用Java；已有证据不足以给出这些认证结论。当前数据已足以决定不再以现场工厂试错来补支持条件。

## 1. com.ptc.wfc.wfcElementTree属于哪一层

它属于WFC扩展的Creo Object TOOLKIT Java API面，不能凭普通J-Link Free已能运行PFC读接口，就认定这些WFC工厂也在免费/已获授权能力内。当前真实otk.jar明确将Elements及其create、wfcElementTree及其Element_Create声明为public：这些API本身不是仅因包名wfc就成为“内部API”。具体版本、具体方法是否有免费/许可例外及运行条件，仍须官方方法页和许可条款核对；不把包名前缀当成所有方法的统一许可规则。

当前代码另有独立问题：com.ptc.wfc.Implementation.wfcSession$WSession和wfcPart$WPart为非public实现类。源码用getDeclaredConstructor(long.class)、setAccessible(true)、newInstance(pointer)构造其视图；真实Step23记录也明确internalApiRuntimeContractCertified=false。即便构造器在非公开类内标成public，或句柄一致，也不自动成为官方支持的对象取得/初始化方式。该桥接不能继续被当作受支持创建链路。

证据在evidence/sdk-boundary-signatures.txt与observed-bridge-facts.json，来自真实SDK javap和已交付源码静态核对，没有加载/初始化SDK或执行模型调用。pfc命名空间也不意味着其中每个方法必然无额外条件，保留的是已验证的只读能力，不是无限扩张免费API范围。

## 2. 无OTK许可是否会无异常静默挂起，是否必须先解锁

不能由现有材料作出“缺许可必然无异常挂起”的判断。SDK存在XMethodNotLicensed、XToolkitAppNoLicense等异常类型；但工厂签名统一throws jxthrowable，类型存在不能证明这些具体工厂在所有启动/版本条件下会怎样返回。Step22/23只观察到START无DONE/THREW，不能把没有Java异常等同于缺许可的官方症状。

现象也可能涉及未建立的初始化/运行上下文、类加载/本地模块关系、原生缺陷、返回后审计失败或外部终止。静态Elements.create/Element_Create不直接接收反射得到的WSession，因此也不能仅凭内部桥接存在，就断言它已经被证明是静态工厂挂起根因。

许可资格、应用解锁、签名、注册/运行模式是不同条件：开发/测试与分发/运行场景可能有不同要求。是否必须解锁、针对何种工件、是否仍需特定运行许可，要由同版本官方章节和真实合法状态证明，不能笼统说“每次API前先解锁”，更不能把解锁当作取得许可。现场具体许可项、签出结果、应用解锁/签名状态均未提供，本轮不生成工具命令、签名或解锁文件，也不发明feature代码。

## 3. GetCurrentSessionWithCompatibility(C4Compatible)与内部WPart/WSession

前者是公开PFC会话API；C4Compatible是兼容性选择，不是OTK授权、模块初始化或内部对象认证。后接非公开实现类的(long)构造器，没有提供同版本官方支持依据，应判为当前受控创建路线“不接受/未认证”，而非可继续使用的官方适配方式。

正确的会话取得、类型转换/视图暴露和初始化，应以官方受支持注册方式及同版本示例为准；本轮不另猜一个工厂、反射类名或构造器替换它。PFC读取能返回，也不能替代WFC的许可/运行契约证明。

## 4. startup java + jlink_java2是否等于OTK Java官方注册

所采磁盘Step23 dat确为startup java，config.pro有jlink_java2 on。它们说明J-Link配置/注册意图，不是OTK Java许可、签出、解锁或受支持WFC启动流程的证据。

此前用户提供的官方资料摘要提及OTK常见startup otk_java、toolkit object、creo_type parametric等注册字段。但本轮没有取得准确版本的官方注册/兼容章节全文，不能把摘要当作可直接部署的配置模板，也不能笼统断言所有startup java兼容应用都不允许OTK。对本项目可明确的是：当前组合未能证明满足OTK运行契约，不能作为继续WFC工厂调用的依据。本轮遵守禁令，不要求或执行修改startup、jlink_java2或classpath。

## 5. 是否继续CreateElementTreeFromXML

不继续现场尝试。该方法在WSession公开接口中声明，属于同一WFC/OTK能力面，是另一个方法而非PFC免费转换入口。换成XML并不会提供缺失的许可、解锁/签名或初始化/官方会话契约证据；其内部是否共享失败路径也未验证，不能保证能绕开卡点。当前任务的支持条件门未通过，因此XML路线同样阻断，不再生成卡死探针。

## 6. Java与C/C++路线

保留Java UI和已验证PFC只读层，冻结采集器R1。停止当前内部WFC桥接的建树/写模型链路，不做全量重写，也不把磁盘SDK/JDK一致当作创建可用。

允许进入C/C++ Pro/TOOLKIT前置离线评估：先核实同版本/位数SDK头文件与导入库、PTC支持的编译器/ABI、合法C++开发/运行许可及注册条件、官方驱动面转钣金元素树示例和错误处理语义。C++本身不是免许可替代，也不能保证修复原生问题；前提未齐不生成/部署转换DLL。Java UI可作为保留的交互层，与未来受支持的转换内核分工，当前不实施跨语言写入桥。

## 下一个最小离线动作

只完成一张“同版本官方说明＋现场合法许可/工件解锁/签名状态＋实际注册工件”对照表。offline-evidence-matrix.json已列字段和缺项：优先取得安装内Compatibility_with_J_Link、注册文件章节、Unlocking_Running_and_Signing章节与两个WFC工厂方法页；配合法定许可项/签出状态摘要、当前工件散列及相应签名/解锁状态。只读现有材料，不需要现场重启、工厂调用、换JDK、加pfcasync、改startup或新的按钮。

若资料不能证明支持条件，维持BLOCKED并继续C++前置可行性评估；若后续合法证明官方Java OTK路线成立，再另行设计基于官方方式的离线实现和独立受控任务，不能自动恢复Step20或清Step22/23锁。

## 证据等级与访问限制

已独立核对：真实SDK公开/非公开声明、源码反射桥、之前磁盘SDK及Step23 class比对结果。已有官方文档内容来源仍为用户历史摘要，不是原始HTML全文；现场运行许可/解锁/签名状态未知，故障会话实际JVM也未知。Temurin25与SDK字节码代际一致，不再作为未经证实的首要原因，更不在本轮更换。

本轮尝试读取PTC公开兼容文档，官方全文未取得，HTTP/代理CONNECT返回403；未绕过访问策略。evidence/source-register.json列出尝试地址与来源边界。报告没有冒称核对过官方许可全文、没有把静默挂起解释成官方无许可证错误行为。

本包为结论/清单/静态签名证据，不含原始环境日志、SDK/JDK、许可证密钥、模型、现场探针或配置补丁。
