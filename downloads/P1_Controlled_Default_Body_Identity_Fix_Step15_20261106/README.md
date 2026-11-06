# P1 默认体身份修正第十五步受控候选

2026-11-06，Asia/Shanghai。独立类p1.controlled.step15.P1ControlledConversionStep15，命令SISWControlledConvertStep15，显示名Controlled Solid to Sheetmetal Step 15。只修默认体身份/重检及相关错误诊断，不全局替换same、不改变驱动面/厚度/双确认/创建/再生/回读和不保存策略。

默认体在本次活跃目标200002100.prt、MDL_PART、WFC视图与模型作用域核验、唯一枚举体/默认体均存在且项目类型ITEM_BODY成立后，按模型作用域中的GetId比较。初次接受时保存stableBodyId；确认期间三处recheck通过同一规则，既要求当前枚举ID=当前默认ID，也要求当前ID=初次stableBodyId，允许原生负ID，不把-5778硬编码在产品。包装句柄、Java对象不同只记录，不单独导致默认体失败；其余模型/面same仍保留原逻辑。

包含六种独立体错误码、BODY_IDENTITY_CHECK结构化记录、原始异常原因；体状态实际读取，不沿用Step14 false作为当前值。用户明确输入试验厚度，两次原生YES/NO默认NO，不推断全局壁厚或自动填1.2mm。新规则通过仅表示默认体门通过，不代表其他门禁或转换已成功。

本轮提供新的Step15受控入口，只有现场主动点击且确认/门禁全部通过才可能写模型，未注册/部署/点击/运行Creo。Step15自己的持久记录位于用户目录CreoControlledConversionStep15/step15-200002100-once.jsonl，先CREATE_NEW并force记录；取消/阻断/失败也消耗该次。不自动重试/清锁，不恢复Step13资格，不读取/删除/修改原Step13锁；两个命令彼此独立。装载本候选不自动执行转换，禁止用重载/换目录/删记录绕过Step15单次限制。

实际otk.jar/Java25编译无错误无警告；4份源码、9个class、薄JAR，两次干净编译散列一致，JAR与独立class相同。class major61、SDK major69，运行仍需Java25。SDK/JDK不随包，不用pfcasync，无SDK/模型Mock。35项原单次/确认控制断言及21项体身份策略断言通过，共56项；另用Step14实际原始ID/状态做4个纯策略决策回放（两路线各初检/重检），非新真机采集或转换。

实际SDK引用89项，较Step13只新增ModelItem.GetType与SolidBody.GetType两个只读类型查询。WCreateFeature仍仅一个调用点，显式WRegenerate仍仅一个调用点，无新增模型写；没有Save/模型复制/删除/Undo/导出/精度单位配置修改/射线材料调用。源码same、驱动面校验、特征枚举/错误检查、创建再生回读块按独立命名规范化后冻结核对。

真机Step15默认体门、后续确认/驱动面/厚度/元素树/转换及反馈全部NOT_EXECUTED/UNKNOWN。内部WFC适配和模型/面身份的旧限制仍在，不能承诺这次一定到达确认或原生接受；若其他门禁阻断，记录原码，不顺手放开其他检查或再试。失败可能留部分模型改变，不自动回滚，不保存，仍需现场遵守不要通过其他入口保存可丢弃试验状态。不能据本包宣布材料占据、最终壁厚、可转换生产或全会话零写入。

先读docs/change-and-error-codes.md及docs/controlled-use-and-boundaries.md。交付含source/classes/lib、消息/未启用注册模板、manifest、SHA256SUMS、SDK/javap、静态调用清单、补丁审阅和纯逻辑/历史值回放。原生模型日志/指针地址、截图与SDK不重新分发。
