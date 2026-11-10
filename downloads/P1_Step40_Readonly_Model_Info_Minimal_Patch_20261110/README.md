# Step40：p1_readonly_model_info 最小只读补丁

日期：2026-11-10（北京时间）。本轮唯一功能变化：在交接包已成功的 Step39 基线上新增第二个命令 p1_readonly_model_info。保留 p1_readonly_ping、入口签名、成功返回值、日志通道和 plus 链接要求。

## 1. 改了什么文件

| 文件 | 变化 |
| --- | --- |
| src/entry.cpp | 新命令、按实际 SDK 接口读取当前模型/零件名称/默认体特征数组长度、释放数组、记录状态。增加名称 UTF-8/JSON 处理，日志行容量按 ProMdlName 大小计算，防止合法名称导致整条记录丢失。 |
| text/p1_readonly_model_info.txt | 新命令指定所需的三个消息条目，标签为命令名。 |
| patches/readonly-model-info.diff | 一个补丁，针对交接包 baseline-src/entry.cpp，仅修改该源码并增加新消息文件。 |
| text/p1_readonly_ping.txt、build/required-link-options.rsp | 未改变的既有支持文件，随完整可覆盖源码一起提供。 |

基线源码 SHA256：d8b91bbc5e505795100c0b60bce4e659d0ea414f2ff6259aa4caba1dadf220d5，已确认与 Step39 交付源码逐字一致。应用 diff 前先核验；使用隔离工作副本，执行 git apply --unidiff-zero --check 检查，应用时可用 --whitespace=nowarn 保留消息文件四行一条所需的尾部空行。也可直接采用包内完整 src/entry.cpp。

## 2. 为什么改

交接包的成功 JSONL 已包含命令注册、指定和实际 READONLY_PING，相关返回均为成功。当前不再重复验证环境、修改启动契约或重新采集工具链版本。

本轮使用用户补充的 Creo 13.4 真实头文件接口：

- ProMdlCurrentGet：仅 PRO_TK_BAD_CONTEXT 按无活动模型处理；其他错误单独报告。
- ProMdlTypeGet：使用 PRO_MDL_PART 等本版枚举。只有零件才转为 ProSolid。
- ProMdlMdlnameGet：使用 ProMdlName，不使用 deprecated 的 ProMdlNameGet。
- ProSolidDefaultBodyGet：成功表示存在默认体；按所给说明，PRO_TK_GENERAL_ERROR 表示无默认体。
- ProSolidBodyFeaturesGet + ProArraySizeGet：仅取得默认体特征数组及其长度，不访问数组中的特征内容。每个返回的非空数组均调用 ProArrayFree，释放错误单独记录。ProArrayFree 只释放返回给调用方的临时数组，不删除模型中的特征。

没有改用整个零件特征计数，没有 ProSolidFeatVisit，也没有几何查询。feature_count 的口径就是 SDK 返回的默认体特征数组长度，不是零件特征树总数、活动特征数或面数。

模型名按 SDK 原值输出，不自行补扩展名。为支持中文、引号、反斜杠及控制字符，先严格转换 UTF-16 到 UTF-8，再做 JSON 字符串转义；失败时 name 为 null 并保留错误，不输出截断或格式错误的 JSON。

## 3. 预期通过时看到什么

本机助手沿用交接包 step39_working_build.cmd 的已成功编译/链接参数和显式 SDK Include/Lib，只调整新包/新产物位置并加入新消息文件，不重复采集版本。不要复用旧 entry.obj，不覆盖正在加载的 DLL，也不要叠加同名命令凑结果。

保持 /MT、既定宏与系统库、protk_dll_NU.lib ucore.lib udata.lib libcmt.lib，以及：

```text
/INCLUDE:user_initialize_plus
/INCLUDE:user_terminate_plus
```

构建后 dumpbin 至少核对 user_initialize_plus、user_terminate_plus；本源码也继续声明导出 user_initialize、user_terminate，应同时保留。不要手动重命名入口、做导出别名或新增 DllMain。

将新消息文件放到已经成功解析 p1_readonly_ping.txt 的同一消息位置，沿用已验证的 GUI 命令绑定机制，不改变资源解析方案、DAT 结构或 config.pro。新命令仍需绑定为可点击的 GUI 入口；由本机助手准备和引导，不让用户调代码。

初始化新增日志位于旧 Ping 注册日志之后、INIT_BEFORE_RETURN 之前：

1. MODEL_INFO_REGISTER_BEGIN。
2. MODEL_INFO_ACTION_ADD_RETURN，pro_error=0。
3. MODEL_INFO_DESIGNATE_RETURN，pro_error=0。
4. INIT_BEFORE_RETURN，return_code=0。

点击新命令一次，应对应一条 MODEL_INFO_ENTER 和一条 MODEL_INFO_RETURN。结果仍写入同一个 p1-step35-<PID>.jsonl，tag 仍是 P1_STEP35_DIAG_V1；用 command、UTC、load_token、seq 区分本次事件。访问状态回调不查询模型、不记日志。

| 输入/状态 | MODEL_INFO_RETURN 预期 |
| --- | --- |
| 无活动模型 | result=no_active_model、no_active_model=true；模型名、类型、默认体和特征数为 null，后续查询未执行。 |
| 活动模型是零件，查询均成功 | result=ok、no_active_model=false；model_type=PRO_MDL_PART，model_type_code 为实际枚举值；model_name、default_body_present=true、feature_count 为实际值；数组释放码为 0。 |
| 零件无默认体 | default_body_present=false、feature_count=null；不调用体特征数组接口。 |
| 非零件 | result=non_part_model，只记实际类型；名称、默认体和特征数为 null，不转 ProSolid。 |

只验证必要场景：无模型状态可点一次；若已有活动零件，再点一次核对字段。插件不会为了测试自动打开模型，不要求无目的额外点击，也不要求本轮额外点击旧 Ping。

## 4. 预期失败时如何分支

- 真实 SDK 编译失败：回传首个完整错误和构建结果，不自行换函数名、改核心 C++ 或用 Stub/强转回调掩盖错误。真实 SDK 声明参与类型检查，云端不宣称已完成 SDK 编译。
- 注册或指定返回非 0：停在相应阶段，检查实际错误与新消息资源；DLL 正在运行不代表第二个命令已通过。
- 当前模型查询非 PRO_TK_BAD_CONTEXT 的错误：result=current_model_query_failed，no_active_model 保持 null，不伪装成无模型。
- 名称、默认体、数组取得、长度或释放失败：分别记录对应 *_pro_error；未执行项为 null，未成功的计数为 null。partial_failure 不等于读取全部成功。
- 数组取得成功但没有可用数组：不猜 feature_count=0，保留 partial_failure 和空长度状态；有真实结果后再按 SDK 文档解释。
- 数组释放失败：保留 array_free_pro_error，结果为 partial_failure，不自动重试释放或继续扩大查询。
- 只有 ENTER 或完全没有本次记录：先核对候选 DLL、GUI 绑定和既有日志通道，不由缺日志直接断言 SDK 未调用。不要回头试错已经通过的启动契约。

UI 回调仍返回 0 表示本次命令处理结束；真正数据结果看 MODEL_INFO_RETURN.result 和各 API 状态。不要把命令回调返回、编译通过、加载成功或日志可解析互相替代。模型名是本地业务信息，公开反馈时可脱敏，但保留类型、计数、状态和散列。

## 5. 不做什么

不打开模型、不选面或特征、不查几何参数、不读 feature type、不遍历特征内容、不写特征树、不创建或转换、不修改或保存模型。不改变入口签名、返回值或 plus 链接要求，不新增 DllMain，不改 config.pro，不自动启动 Creo。

云端完成基线核对、补丁应用、静态范围/所有权检查，以及实际独立 JSON 转义函数的宿主 C++ 测试。没有 SDK Stub，没有 Windows/真实 SDK DLL 编译、模型查询或 GUI 测试。按 SOP，由本机助手完成实际构建、导出核对、一次最小现场验证及证据回传。
