# P1 Step32：按本机官方样例摘录修订最小入口模板

日期：2026-11-09。本轮已修订源码工程配置及下一轮对象编译示例；本轮不执行原生编译、不生成 DLL、不注册、不启动 Creo。

## 本轮结论

1. **v143 与当前 VS2022 工具集族一致。** 当前报告的工具集 14.44.35207、cl.exe FileVersion 19.44.35229.0 属于 VS2022/v143 这一族，已有的 x64 C++ 组件可作为下一轮对象检查的工具链候选，无需仅因样例要求 v143 就更换 VS。族匹配不等于已经执行成功，也不单独认证 PTC 对每个精确补丁及 Windows SDK 的支持范围。
2. **按用户提供的官方样例摘录，`/MT + protk_dll_NU.lib` 是该样例的同步 DLL 路线。** 同时记录 `ucore.lib`、`udata.lib`、`libcmt.lib` 和列出的系统库；最小对象编译阶段不链接这些库。模板本轮固定 MT，避免混入未由该摘录说明的 MD 路线。
3. **可以据此准备下一轮最小对象试编译。** 已补入样例宏和编译标志，下一步使用真实头文件验证入口声明。当前不再因缺少一个“已由样例给出的工具集族”而要求更换工具链；精确补丁支持仍单列待核实，试编译不能冒充认证。本轮任务并未要求执行，故只交付命令和模板。

来源边界：本轮收到的是用户任务单对 `x86e_win64/obj/protk_install_example.zip` 内 vcxproj/makefile 的摘录，未收到 ZIP 原件或原始项目全文。因此“官方样例路线”是按这份摘录核对，云端没有独立读取项目条件或复算样例散列。此发现补充了此前“未发现样例”的状态；无需继续按旧结论重复找同类文件。

## 对 Step28 的修订

| 配置项 | Step32 配置 | 依据/用途 |
| --- | --- | --- |
| 平台 | Windows x64 / v143 | 样例摘录及已报告的本机工具链身份 |
| CRT | `/MT` / `MultiThreaded` | 样例 `libcmt.lib` 路线，不显式手加 CRT 链接选项 |
| 编译 | `/c /GS /fp:precise` | 对应样例 `-c -GS -fp:precise`；CMake OBJECT 目标提供仅对象语义 |
| 定义 | `_WSTDIO_DEFINED`、`PRO_MACHINE=36`、`PRO_OS=4` | 样例摘录，原样使用 |
| 未来主链接库 | `protk_dll_NU.lib ucore.lib udata.lib` | 只在 link-plan.json 记录，本轮没有链接目标 |
| 未来系统库 | 见 link-plan.json 的 13 项 | 按摘录保留，未试图验证可删减到几项 |
| 编译器身份参数 | `P1_EXPECTED_COMPILER_VERSION` | 替换 Step28 的 `P1_DOCUMENTED_MSVC_VERSION`；仅比较实际检测值，不宣称官方认证 |

`src/entry.cpp` 保持 Step28 的初始化/终止空入口，不新增任何业务 API。摘录未给入口函数原型，故原型仍待真实头文件及正式入口说明核对；发现声明冲突时保留编译错误，不使用假头文件、类型转换或绕过声明。包内不附注册文件，亦无部署动作。

## 下一轮首选：直接生成一个对象文件

下列为 **Windows x64 开发者命令环境中、位于工程根目录时的下一轮示例**；本轮没有执行。先选定已报告的 MSVC/SDK 并具备真实 SDK includes；`P1_CL` 应是对应工具集 `Hostx64/x64/cl.exe` 的绝对路径，`P1_SDK_ROOT` 是本机真实 ProToolkit 根目录：

```bat
"%P1_CL%" /nologo /c /GS /fp:precise /MT /D_WSTDIO_DEFINED /DPRO_MACHINE=36 /DPRO_OS=4 /I"%P1_SDK_ROOT%\includes" /Fo"object-check-step32\entry.obj" "src\entry.cpp"
```

对象输出目录须预先新建，避免覆盖旧证据。命令没有 `/link`、`/LD` 或任何 `.lib` 参数。`/nologo` 只影响输出；使用完整编译器路径防止 PATH 选错版本。

`commands/object-only.example.cmd.txt` 提供完整的下一轮手动示例：填写两个路径占位符，保留它在 commands 目录中的位置，另存为 `.cmd` 后才运行。它在 `setlocal` 范围内调用本机 `vcvarsall.bat`，选择 x64、工具集 14.44.35207、SDK 10.0.26100.0；清空局部 CL/_CL_ 附加标志；使用固定完整 cl 路径；仅新建输出目录并执行 `/c`。不更改系统配置或全局默认工具集。命令文件本轮只做静态审阅，没有 Windows 运行验证。

未来运行产生 `compiler.log`、`exit-code.txt`、`events.jsonl`，成功时还有 `entry.obj`。输出目录已存在则退出，不自动删除或覆盖旧证据。以退出码 0 且对象文件确实存在作为对象检查成功；无论成功与否均不继续链接。保留错误，不自动改 SDK、安装组件或启动 Creo。原始 compiler.log 可能含本机绝对路径，后续整合先提取错误编号、阶段和版本摘要，不直接公开原始日志。

## CMake：默认审阅，未来可选对象检查

默认模式无需 SDK 或 C++ 编译器，仅输出计划：

```sh
cmake -S . -B build-review
cmake --build build-review --target review
```

下一轮若选择 CMake 路径，应使用全新构建目录（不要复用 Step28 缓存），配置示例如下。该路径会运行 CMake 管理的编译器识别/探测，可能产生临时探测对象、库及其他识别产物；本轮未执行它。若要求严格只执行一次 `/c`，使用上面的直接编译路径。

```bat
cmake -S . -B build-object -G "Visual Studio 17 2022" -A x64 -T "v143,version=14.44.35207" -DCMAKE_SYSTEM_VERSION=10.0.26100.0 -DP1_ENABLE_OBJECT_CHECK=ON -DP1_SDK_ROOT="<真实ProToolkit根目录>"
cmake --build build-object --config Release --target p1_entry_object
```

模板检查实际 MSVC、x64、v143、工具集、Windows SDK 和编译器版本；编译器默认预期值来自报告的文件版本 `19.44.35229.0`，不等于已通过 CMake 探测。若实际值不同则停止查证，不强制覆盖参数掩盖差异。真实头文件缺失或宿主非 Windows 时不会开启编译器。CMake/VS 默认还可能加入自身编译标志，生成命令不保证与样例 makefile 逐字一致；最小标志比对以直接 `/c` 命令为准。没有 DLL、EXE、安装或部署工程目标。

## 本轮验证与未验证项

云端仅验证默认审阅配置/构建、生成计划以及防误用路径，不编译 SDK 代码。具体结果见 evidence/verification.json。v143 家族映射另与云端 CMake 3.31.6 的 Windows-MSVC.cmake 映射核对；该映射不是 PTC 支持矩阵。

尚未验证：原始样例条件、精确 PTC 编译器/SDK 支持范围、真实入口声明、Windows 对象编译及 CMake 原生分支、库链接或 Creo 加载。后续若官方资料对精确版本有限制，再据原文调整；不把这些未决项写成当前版本已不兼容。Java/WFC 路线不恢复。
