# P1 Step33：最小对象通过后的链接与生命周期验证路线

日期：2026-11-09。结论：可以准备下一轮最小 DLL 链接验证；沿用已编译通过的空入口，不增加业务逻辑。受控加载/卸载安排在链接及二进制检查通过之后。本轮只交付判断和下一轮命令示例，没有执行链接或现场操作。

## 四项判断

| 问题 | 结论 |
| --- | --- |
| 是否进入最小 DLL 链接 | 可以。按本轮本机结果报告，Step32 包哈希匹配、仅对象编译退出 0 且 entry.obj 已生成，已具备进入下一轮链接验证的基础。不需要重新重复对象编译来证明同一结论。 |
| 是否先补初始化/终止逻辑 | 暂不需要。返回成功的 user_initialize 和空 user_terminate 足以作为第一轮链接/入口验证的最小候选；无需加入模型、菜单、转换或资源管理逻辑。 |
| 是否只链接三个 PTC 库 | 不是完整链接清单。保留样例三个 PTC 库、MT CRT 规则和系统库；对象文件本身也是必需输入。 |
| 是否规划一次受控加载/卸载 | 可以规划。在链接及静态二进制检查通过后，用空模型会话做一次人工加载→停止/卸载观察，不做模型操作。本轮未执行或自动安排现场加载。 |

输入证据是用户任务单中的结果摘要，云端未收到原始 compiler.log、entry.obj 或其散列。故本轮记录为 `OBJECT_COMPILE_PASS_USER_REPORTED`，不冒充云端独立重现。原型可编译只说明该源在所用头文件下编译通过；不能独自证明入口已被 Creo 正确调用、终止回调已执行或精确补丁已获官方支持认证。

## 初始化/终止代码

沿用 Step32 的 `src/entry.cpp`，保持 `extern "C"` 与两个导出入口。不要添加自定义 DllMain，不指定自定义 `/ENTRY` 或 `/NOENTRY`，也不为了通过链接增添业务 API。CRT 默认 DLL 启动机制与 TOOLKIT 的 user_initialize/user_terminate 是不同层次。

若原始官方样例说明有必须的非业务初始化项，按该说明另作最小修订；本轮提供的摘录没有给出这类要求，不能凭猜测加入。现有空入口没有分配资源，因此暂不需要补清理逻辑。

## 最小链接输入

以已验证的 entry.obj 为输入，保持 x64/MT：

- PTC：`protk_dll_NU.lib ucore.lib udata.lib`。
- CRT：样例记录 `libcmt.lib`，配 `/MT` 对象；编译器在对象中记录的其他默认运行库依赖由正常库搜索解决。
- 系统库：`kernel32.lib user32.lib wsock32.lib advapi32.lib mpr.lib winspool.lib netapi32.lib psapi.lib gdi32.lib shell32.lib comdlg32.lib ole32.lib ws2_32.lib`。

不混入 MD 或异步库，不用 `/NODEFAULTLIB`、`/FORCE` 或忽略机器类型/运行库冲突来掩盖问题。系统库清单沿用 Step32 官方样例摘录，本轮不猜测哪些可删。空入口没有业务 API 引用，链接器可能不提取某些 PTC 库成员；即使成功，也不能证明后续 API 所需的全部链接符号和依赖都已验证。

## 下一轮链接命令示例

前提：使用本机同一 x64 开发环境，选中已报告的 MSVC 14.44.35207 和 SDK 10.0.26100.0；P1_LINK 为该工具集 Hostx64/x64/link.exe 的完整路径，P1_OBJ 为本次已验证对象的完整路径，P1_SDK_ROOT 为真实 ProToolkit 根。下面是工程根目录下的示例，输出目录须事先新建：

```bat
"%P1_LINK%" /nologo /DLL /MACHINE:X64 /INCREMENTAL:NO /OUT:"dll-link-step33\p1_entry_minimal.dll" /IMPLIB:"dll-link-step33\p1_entry_minimal.lib" /LIBPATH:"%P1_SDK_ROOT%\x86e_win64\obj" "%P1_OBJ%" protk_dll_NU.lib ucore.lib udata.lib libcmt.lib kernel32.lib user32.lib wsock32.lib advapi32.lib mpr.lib winspool.lib netapi32.lib psapi.lib gdi32.lib shell32.lib comdlg32.lib ole32.lib ws2_32.lib
```

`commands/link-only.example.cmd.txt` 提供下一轮手动示例。填写三个路径占位符，保持文件在 commands 目录中，另存为 .cmd 后才运行。它在局部环境内调用 vcvarsall，使用完整 link/dumpbin 路径，新建输出目录，链接后捕获 headers/exports/dependents；无 cl 调用、注册或 Creo 启动。已有输出目录则退出，不删除旧证据。该命令文件未在 Windows 执行，本轮只做静态审阅。

退出码 0 仅表示示例的链接、输出文件存在性检查及三个 dumpbin 命令完成，不表示导出/依赖已经人工审核通过，更不表示可自动加载。非零时保留日志并停止：2 为输入/工具缺失，3 为开发环境初始化失败，4 为目录操作失败，5 为已有输出目录，6 为链接返回成功但 DLL 缺失，7 为 dumpbin 命令失败；其他链接器非零值原样保留，以 events.jsonl 中的事件和阶段区分。

## 加载前的静态检查

下一轮保留输入对象与输出 DLL 的 SHA256、链接退出码和工具链版本记录。检查 dumpbin 结果：PE 为 x64 DLL，导出表确有 C 链接名 `user_initialize`、`user_terminate`，依赖符合所选 SDK/CRT 的实际结果且现场可解析。发现缺失、位数错误或不明导出时停止，不自动改导出别名掩盖问题。`/dependents` 仅列出直接导入依赖，不能独自证明所有传递依赖都能解析；dumpbin 成功也不是验证通过。

## 一次受控生命周期观察

链接和静态检查通过后，再按本版 TOOLKIT 的实际注册/运行机制核对所需注册字段与开发/运行条件。使用空模型会话，不自动关闭现有模型、不保存、不创建特征、不执行转换。允许的工程目标是一次人工显式加载，观察应用状态，然后停止/卸载并记录状态，不自动重试或替换正在加载的 DLL。

不改 config.pro，不配置自动启动；如本版支持通过辅助应用界面临时显式注册，用经核对的临时注册文件，保持本轮无实际注册动作。卸载受该注册方式和运行状态约束；停止失败则保留结果，不强制终止进程或删除加载中的文件。若 Creo 或官方流程提示授权/解锁条件，记录原提示并按原文核对，不猜测命令。

现有空入口无诊断标记，因此“应用状态显示已加载/已停止”与“直接观察两个回调进入”必须分别记录，不能把无回调日志写成生命周期回调已证实。若后续必须证明回调进入，可另作仅输出诊断标记的小改动，再重新编译/链接；不在本轮加入。

## 证据与本轮边界

未来命令示例仅在本地生成 linker.log、headers.log、exports.log、dependents.log、exit-code.txt 和 events.jsonl，以及成功链接后的 DLL/导入库等产物。原始工具输出可能含本机路径或地址，后续整合提取版本、散列、错误编号、导出名和状态摘要，不直接公开原始输出。

本包只有决策、命令文本和结构化核查日志，无 SDK、对象、DLL 或注册文件。Step32 源码及模板未被修改，本轮不编译、不链接、不注册、不启动 Creo，不保存模型、不创建特征、不写转换逻辑、不改 config.pro。Java/WFC 路线不恢复。
