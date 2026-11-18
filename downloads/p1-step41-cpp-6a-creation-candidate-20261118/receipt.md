# P1 Step41 C++ 6A 驱动面创建段候选交接回执

2026-11-18 · 仅源码静态核对 · 待 Windows 本机 SDK 编译和 Creo 真机验证

## 候选及源码边界

补丁为 `cpp-6a-r2b-driving-creation.diff`，42,570 字节，SHA-256 `2f5fd2187561b02f1047f85ffffd5fff1d46403277a7e2f2714e8903325aed32`。旁置 `.sha256` 与清单 `manifest.json` 供逐项对账。基线为静态核对过的冻结 R2-B 单位探针快照，四个基线文件的哈希均与既有回执一致。

| 文件 | 变动 |
|---|---|
| `src/entry.cpp` | 在既有三条只读命令之后，注册派生候选的 `p1_smt_drv_conv`。探针基线独立留档，派生候选第 4 个命令使用创建回调。增加七节点树所用的 SDK 头。 |
| `src/smt_driving_command.inl` | 沿用原守卫、选面、输入、重验、单位查询、access、锁和 JSONL。增加树准备、一次创建和一次重生成、默认体及特征回读、状态与双通道反馈。 |
| `src/smt_driving_tree.inl` | 新增七节点树构造及资源所有权管理；用已复制的选面和 `thickness_model`，直接子节点的路径传空，模型和默认体 selection 由 SDK 包装。引用与创建选项使用 SDK 分配，根节点先于引用释放。 |
| `text/chinese_cn/p1_smt_conv.txt`、`text/usascii/p1_smt_conv.txt` | 新增两份 UTF-8 资源。两语言各 30 个键与源码逐项匹配；每条四行，末两行保留输入单要求的 `#` 占位格式。现场解析与中文显示仍待验证。 |

临时副本上的 `git apply --check`、应用及反向还原通过，反向哈希与冻结基线一致。确认三条只读命令的原实现未变，创建链只接受原有守卫通过的数据。树含七个白名单节点；创建、重生成各一个调用点，均在同步转换回调链。静态扫描的旧创建入口、禁止元素和保存调用均为零。取消和成功终态做过格式串静态解析，未运行 SDK。

## 本机离线验收

在包含 `src/` 和 `text/` 的冻结 R2-B **隔离副本根目录**核验清单，再按顺序应用和复算派生文件。

```bash
sha256sum --check --strict /path/to/cpp-6a-r2b-driving-creation.diff.sha256
git apply --whitespace=error --check /path/to/cpp-6a-r2b-driving-creation.diff
git apply --whitespace=error /path/to/cpp-6a-r2b-driving-creation.diff
```

第一条在补丁与旁置校验文件同目录运行。其余两条在基线副本根目录运行；对账 `manifest.json` 里的五份派生源码及资源。

本机复用已归档的 B 类 build 脚本，客户端只调整隔离目录和产物名称。编译输入为副本 `src\entry.cpp`。保留 VS2022 v17.14.41、MSVC 14.44.35207、Windows SDK 10.0.26100.0、Creo 13.4 SDK includes 和 x64 库目录，沿用如下命令口径。

```bat
cl /nologo /c /GS /fp:precise /EHsc /MT /D_WSTDIO_DEFINED /DPRO_MACHINE=36 /DPRO_OS=4 [沿用原 SDK 和 WinSDK /I 参数] /Fo"%BUILD%\entry.obj" "%OUT%\src\entry.cpp"
link /nologo /DLL /MACHINE:X64 /INCREMENTAL:NO /INCLUDE:user_initialize_plus /INCLUDE:user_terminate_plus /OUT:"%BUILD%\p1_step41_cpp6a_driving_creation.dll" /IMPLIB:"%BUILD%\p1_step41_cpp6a_driving_creation.lib" [沿用原 SDK 和 WinSDK /LIBPATH 参数] "%BUILD%\entry.obj" protk_dll_NU.lib ucore.lib udata.lib libcmt.lib kernel32.lib user32.lib wsock32.lib advapi32.lib mpr.lib winspool.lib netapi32.lib psapi.lib gdi32.lib shell32.lib comdlg32.lib ole32.lib ws2_32.lib
```

方括号内是既有脚本参数，执行时沿用原行。保留编译、链接的实测退出码与日志。归档 `dumpbin /exports`、`/dependents`、`/imports` 原文，核对四入口、R10/R2 依赖基线和新增项，并记录实际 DLL、OBJ 哈希。

## 现场日志及判定

真机操作单独授权后由本机助手组织。按同一进程、加载实例、`operation_id` 顺序配对 `SMT_CONV_API_START` 与 `SMT_CONV_API_DONE`。START 的 `pro_error=null` 表示调用尚无返回，DONE 记录真实 SDK 返回码，捕获异常时记录 `exception=true` 和 null。共享日志通道沿用原 I/O 与调试诊断策略。日志缺段时保留为未验证；如果 START 后无 DONE，结合实际会话状态区分阻塞和日志中断。

| 用例 | 预期 |
|---|---|
| 无模型、装配或绘图、已是钣金、选面/输入拒绝或取消 | 走原有分支，创建状态 false/null。对未复现的 SDK 前置过滤分支保留未验证。 |
| 包装、建树或写前消息格式预检失败 | 分别为 `WRAP_SOLID_FAILED`、`TREE_CREATE_FAILED`、`FEEDBACK_NOT_READY`。终态无创建调用。 |
| 创建被拒绝 | `PRO_TK_CANT_MODIFY` → `MODEL_READ_ONLY`，其余失败 → `CREATE_FEATURE_FAILED`；从 `SMT_CONV_ERROR_LIST` 与元素错误事件取证。创建一经调用即保守标记模型可能修改，任何失败分支均不自动重试。 |
| 重生成、回读或反馈失败 | 分别可见 `REGEN_FAILED`、`STATUS_READBACK_FAILED`、`TARGET_BODY_CHANGED`、`STATUS_NOT_SHEETMETAL`、`FEATURE_READBACK_FAILED`、`POST_FEEDBACK_FAILED`。清理失败单独标记并阻断后续写入。 |
| 授权后在普通实体副本用一个有效平面和正板厚 | `SMT_CONV_TREE_READY`、创建及重生成成功 DONE、同一默认体钣金回读与特征 ID/类型/名称齐全，最后 `SUCCESS_UNSAVED`、`save_called=false`。确认三条只读命令仍可响应。 |

需现场确认最小树和默认体引用是否被 SDK 接受、目标体 ID 是否稳定、创建和重生成是否正常返回、错误缓冲及引用生命周期、命令可见性、异单位方向、中文资源显示与编号串替换。原始 R2-B 真机结果只覆盖 mm→mm 和创建前硬停。当前候选尚无编译或正向创建实测。
