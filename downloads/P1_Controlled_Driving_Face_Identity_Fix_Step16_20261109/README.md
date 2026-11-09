# P1 驱动面身份修正第十六步候选

2026-11-09，Asia/Shanghai。独立类p1.controlled.step16.P1ControlledConversionStep16，命令SISWControlledConvertStep16，显示名Controlled Solid to Sheetmetal Step 16。

本轮只修单个驱动面的选择/作用域/ID检查及错误诊断。保留Step15默认体规则、同目标零件/mm/唯一实体体、可见平面/正面积、手动试验厚度、两次默认NO确认、单次创建/再生、不保存和原生回读。same函数未全局替换；模型视图与Selection.GetSelModel仍走原模型身份检查，不跨模型/装配/体任意用ID放行。

驱动面只从当前预选缓冲捕获1项，拒绝非空装配路径/非Surface/其他模型。通过当前活跃目标200002100.prt、MDL_PART、已核验唯一默认体及初始stableBodyId作用域后，仅在该体原生ListSurfaces列表按面GetId找唯一匹配。Java对象/临时wrapper句柄不同只记日志，不再作为面归属硬失败；不使用全模型面列表或另一个体按ID兜底。

初检保存原Selection与stableFaceId。之后三个recheck均重新核对原Selection的路径、类型、所属模型和面ID、当前体及列表归属，不接受替换面；对话框期间缓冲变化不被当成新的预选输入。原选择数量1在捕获时强制验证并保存，不在每次确认后重新获取一个可能已被UI清空的新缓冲。原模型/版本/dirty、默认体稳定ID、单位和特征状态重检保留。

结构化FACE_SELECTION_CHECK / FACE_IDENTITY_CHECK / FACE_SCOPE_ERROR将选择错误、面作用域与默认体错误分开。支持任务要求的六个面错误码，另给出复检面ID变更/体作用域变化。非平面/不可见/面积无效的几何拒绝条件和原OTHER_FAILURE分类保持，不顺手放宽其他门。

Step16使用用户目录CreoControlledConversionStep16/step16-200002100-once.jsonl，先CREATE_NEW并force记录，本进程clicked不重置。只在现场主动点击、两次YES与全部门禁成立后才可能调用原生转换。取消/前置失败也消耗本步一次；不读/删/改Step15锁，不自动调用旧命令、不重试/清锁或通过重载绕过。未部署、注册或执行Creo，未进行Step16真机转换。

实际otk.jar/Java25编译无错误无警告；5份源码、12个class、薄JAR，重复干净编译散列一致，JAR与独立class字节相同。class major61、SDK major69，运行仍需Java25；SDK/JDK不随包，pfcasync不使用。35项单次控制、21项默认体策略、25项选择/面策略断言通过，共81项，不是81次真机测试，无SDK/模型Mock。

实际90项SDK前缀方法引用，较Step15仅新增SurfaceType.getValue只读枚举取值，不代表90次JNI调用。创建特征/显式重生成仍各1调用点；没有新增模型写、Save、导出、删除/Undo、宏、配置或精度/单位改动及射线/材料调用。默认体纯策略/核验函数、模型same、特征状态检查和创建再生回读块已冻结比对。

任务单报告Step15默认体ACCEPTED后在驱动面身份门阻断，requestId=f39c897e-b07f-455f-829f-b07096bde7cd。本轮只收到该任务说明，没有对应原始Step15 JSONL；把它作为实现需求，不声称独立复核了当时面ID/句柄。Step16实际面门、后续确认/厚度/转换与回读均NOT_EXECUTED/UNKNOWN，不承诺所有门都能通过，不认证全局壁厚或生产可用。

详见docs/face-scope-and-errors.md、docs/controlled-use-and-limits.md，附manifest、SHA256SUMS、静态调用表、javap、source-review.diff和纯逻辑测试日志。若其他门阻断，保留原码/现场，不绕过检查或重试；不因局部1.2mm而自动选择厚度。
