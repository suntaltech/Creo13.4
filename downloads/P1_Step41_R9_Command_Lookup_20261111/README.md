# Step41 R9：按 SDK 动作名查询命令 ID

日期：2026-11-11（北京时间）。本包基于R8真实契约，只增加命令查询观测，不修复UI、不执行命令。

## 1. 改了什么

仅src/entry.cpp增加一个有界日志格式helper、一个查询helper及三个调用点。每个命令ActionAdd成功、Designate调用和原返回日志完成后，调用一次ProCmdCmdIdFind(name, &found)。原Body → ModelInfo → Ping顺序不变；查询插在各自指定返回后，不重新指定或重注册。

新事件COMMAND_LOOKUP_BEGIN/RETURN的revision为UI_R9_COMMAND_LOOKUP，记录实际SDK动作名、日志功能标识、Find原始返回码、同次Designate返回码、ActionAdd ID、Find ID、ID是否为空及比较结果。ID用uintptr_t十六进制JSON字符串表示，不解引用、不释放、不执行。Find失败时lookup_id/空值标志/比较结果均为null，不把初始化的零值冒称SDK返回ID。

ActionAdd失败则原有函数立即返回，没有该命令查询，这是原路径。Designate失败但ActionAdd成功仍作一次查询，同时记录指定错误；不能将找到命令等同于指定成功。每次初始化最多三次查询，不循环、不在access或业务回调中查询。

基线为上轮已核对的R7源码，SHA256 8ecf88e80d366d7053a2c14475dff4393bd6b1fc1770f2f6c6d96fccb6e3faf9；当前R8包不含新源码。完整源码与patches/command-lookup.diff等价，核验基线后在隔离副本git apply --unidiff-zero --check，通过后git apply --unidiff-zero。

## 2. 为什么改与结论边界

R8外部SHA256及内部5项清单通过，真实声明来自ProUICmd.h：ProError ProCmdCmdIdFind(char *cmd_name, uiCmdCmdId *cmd_id)，ID仍为void*。文档支持按动作/选项名找ID，并给出成功/失败返回码；本机样例出现初始化调用，但文档未明确列出允许上下文。本补丁依据该样例设计初始化诊断，不能声称所有初始化场景安全已被文档保证；真实SDK编译和一次受控加载仍需本机验证。

新增观测检验当前动作名能否被SDK查询，以及两个接口取得的ID在本次运行是否一致。文档没有显式保证Find结果与ActionAdd输出相等，故ids_equal是观测，不是断言、错误门或初始化失败条件。

Find不是UI列表、placeability或Designate标签/帮助/描述的回读。ACCESS_AVAILABLE只说明按钮可用，R8资料没有保证它出现在定制列表，也没有说明列表构建期间必调用access，因此本轮不修改或高频记录access。

此前body和可见ModelInfo的描述都-32，不能把ToBuffer截断单独作为隐藏原因。日志facts.command不是SDK输入，不再做“匹配日志名”或重复长动作名试验。既有消息探针六次全部保留，但不再增加消息键。

本机R8审计提到17与7的计数差异：17是原始R7交接包文件清单已核验项数，7是之后派生复核包SHA256SUMS项数，属于不同统计对象；不是已发现哈希失败。本包manifest明确区分当前输入清单、源码基线与校验范围。

## 3. 本机验收与预期信号

先按固定工具链离线重编、链接，核对源码/DLL哈希、导出/依赖及两个plus引用。保留PRO_USE_VAR_ARGS和所有既定构建要求，不重复SDK/编译器版本采集，不强转函数原型或绕过编译错误。

所有R5/R7资源保持原字节，access、业务回调和六次消息探针不变；不加入本包没有的资源改动。沿用已知隔离候选定位与加载方式，记录实际手动/自动加载方法，不为本轮修改config.pro、语言、DAT字段语义或UI设置。

按既有流程由用户手动一次受控加载及同条件列表观察；不部署正式版本，不绑定/点击面计数、不打开或切换模型。收集最新pid/load_token完整初始化及必要列表截图，填acceptance.template.json。

预期每项原Designate返回日志后出现一组查询BEGIN/RETURN，顺序Body、ModelInfo、Ping。之后仍有原六组消息探针，最后INIT_BEFORE_RETURN仍为0。新查询helper不改变任何原返回值。

重点记录：Find是否0、返回ID是否空、与ActionAdd ID是否相同、原Designate是否0，以及独立截图中的实际可见命令。原UI_R4_R1_DIAG身份事件和UI_R6_MESSAGE_PROBE事件保留旧revision；新事件标识和源码/DLL哈希证明R9候选身份。

## 4. 结果如何分支

- 编译/链接失败：回传准确诊断并停止现场操作，不以伪造SDK头文件验证。
- BEGIN有而RETURN缺失：定位最后动作名与事件，保留日志，不自动重试。
- Find失败：原样记录返回码。文档给-1为查找失败；不能从失败自动推断注册对象被删除或UI缓存已失效，不覆盖/重注册。
- Find成功但ID为空：原样记录成功/空ID；不把空指针直接传给其他SDK接口。
- Find成功且与ActionAdd ID不同：记录为待解释身份差异，先比较两个已可见命令是否同样不同；没有文档保证相等，不能据此判SDK错误或执行任一ID。
- 三项均找到且ID一致、Designate均0、UI仍缺body：注册身份观测通过，不能继续说body按名不存在；UI指定/列出关系仍待定位，不再改名、刷新或重跑消息长度试验。
- 突然可见：记录诊断调用对运行过程的潜在影响，不把加查询视为稳定修复，不额外点击扩展试验。

查询位置在各项指定后，仍可能改变运行时序；不声称它绝对无内部副作用。该接口既不输出实际消息路径/语言目录，也不输出列表成员资格或access调用历史。字段UI_membership_read_back固定false。

## 5. 不做什么及验证范围

不改动作名/label/资源、注册顺序/参数、access返回或回调、原日志框架、消息探针、入口/返回值、业务计数；不新增菜单/图标/rbn/刷新/别名或命令执行。不选择/写入/保存/转换模型，不自动启动停止Creo，不改配置、缓存、CRT或链接要求。

云端检查包含输入校验、补丁应用、移除新增helper/三个调用后逐字节恢复R7，以及纯C++日志格式测试：ID相同、不同、成功空ID、失败null和小容量拒绝。checks/lookup_format.cpp只测试格式化函数，无SDK替身或模拟SDK调用；不输出测试用地址到公开结果。没有真实SDK构建、DLL加载或UI验证。本包不包含原始SDK摘录、日志或用户路径。
