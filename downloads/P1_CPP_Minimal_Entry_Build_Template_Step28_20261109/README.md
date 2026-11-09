# P1 Step28：同步 Pro/TOOLKIT 最小 C++ 入口候选

日期：2026-11-09。范围：离线源码及构建模板；本轮不生成可部署 DLL。

## 结论与证据边界

先以 `/MT` + `protk_dll_NU.lib` 规划同步 DLL；保留 `/MD` + `protk_dllmd_NU.lib` 作为替代配置。两套不混用。这是按传统 Pro/TOOLKIT 库命名形成的候选，不是对本机 13.4 SDK 的已核定链接结论。当前只有文件元数据，尚无真实头文件正文、Makefiles 正文、适用版本平台支持矩阵或链接结果。不能据此承诺 ABI、许可证或运行可用。

Step27 清单观察到关键头文件及八个库，但扫描在 20,001 项处达到上限；“未发现样例工程”来自本轮用户任务单，不等于磁盘上完全没有样例。平台页面转至外部支持说明，不能从文件名确定受支持的 Visual Studio 版本。

## 七项答复

### 1. 同步 Hello World 链接哪个库

拟定 `protk_dll_NU.lib`，配 x64、Release、`/MT`。它是同步 DLL 的候选主库；是否还需 Windows 系统库、资源或其他 PTC 库，须查本版官方 Makefiles/开发指南并由后续真实链接结果确定。当前不提供一个伪称“完整且已验证”的链接命令。

初始化/终止空入口是 Hello World 的第一阶段；本轮没有消息显示、菜单、日志文件写入或模型调用。后续显示消息需另行增加业务之外的 UI API，并单独验证。

### 2. 四个库的区别

下表为传统命名下的角色解释；本版原文尚待核对，不能只靠大小/哈希证明静态内容或入口 ABI。

| 库名 | 候选用途 | CRT 候选 | 当前选择 |
| --- | --- | --- | --- |
| `protk_dll_NU.lib` | 同步 DLL 接口库族 | `/MT` | 首选规划 |
| `protk_dllmd_NU.lib` | 同步 DLL 的动态 CRT 变体 | `/MD` | 替代规划 |
| `protoolkit_NU.lib` | 非 DLL 的独立程序/spawn 链接库族（本版模式支持待核对） | `/MT` | 不用于本轮同步入口规划 |
| `protkmd_NU.lib` | 上一库族的动态 CRT 变体 | `/MD` | 不用于本轮同步入口规划 |

`pt_asynchronous.lib` / `ptasyncmd.lib` 属异步候选，不加入本轮同步入口。`ucore.lib` / `udata.lib` 只有被本版官方构建说明要求时才加入，不能把八个库全部链接。`_NU` 后缀不能作为无需开发授权、解锁或签名的证据；这些条件独立待核定。

### 3. /MT 还是 /MD

模板默认 `/MT`，用于固定第一套候选组合，并避免先引入动态 CRT 部署前提；这并不证明它是 Creo 13.4 唯一允许的配置。若本版官方工程要求 `/MD`，整套切换为 `protk_dllmd_NU.lib` + `/MD`，不能靠 `/NODEFAULTLIB`、忽略 LNK2038 或强制库覆盖掩盖冲突。不采用 Debug CRT (`/MTd`、`/MDd`)。

候选编译设置：MSVC、Windows x64、Release、`/W3 /EHsc`、对应 `/MT` 或 `/MD`、真实 `includes` 路径。`/c` 表示仅编译对象，未来 DLL 链接才涉及 `/DLL`；本轮没有 DLL 链接目标。具体工具集、Windows SDK、宏及系统依赖仍需适用支持资料核定，不擅定 VS2019/VS2022。

### 4. 入口函数

`src/entry.cpp` 提供待核定的常见入口形状：

```cpp
extern "C" __declspec(dllexport)
int user_initialize(int argc, char *argv[], char *version, char *build,
                    wchar_t errbuf[80]);
extern "C" __declspec(dllexport)
void user_terminate(void);
```

使用 C 链接名并导出，成功返回 `PRO_TK_NO_ERROR`。`errbuf` 是宽字符候选，数组形参退化为指针；长度、参数顺序、调用约定和导出要求仍须用本机 `ProToolkit.h`、`ProToolkitDll.h` 及正式入口说明确认。代码不读写这些指针。若真实声明不符，应修正候选，不制造兼容头、强制转换或绕过编译错误。该原型没有在真实 SDK 上编译验证。

### 5. protk.dat 最小字段

见 `registration/protk.dat.example.txt`：`name`、`startup dll`、`exec_file`、`text_dir`、`end`；另明确列出 `allow_stop true`、`delay_start false` 以说明生命周期意图。是否允许省略 `text_dir` 及这些属性的本版默认值，待官方说明核定。本轮不追求通过删除字段猜测“绝对最少”。

文件名刻意保留 `.example.txt`，其中路径占位符尚无实际 DLL 可对应，不是可加载注册文件；不复制至 Creo 配置、不修改 `config.pro`、不启动或注册应用。`delay_start false` 只解释未来显式加载后的初始化意图，本轮没有加载动作。

### 6. 无样例工程时如何建立

先用下述无编译器审阅模式检查配置和候选库选择（CMake 3.21+，系统已有对应构建生成器）：

```sh
cmake -S . -B build-review -DP1_CRT=MT
cmake --build build-review --target review
```

默认 `LANGUAGES NONE`，不探测 C++ 编译器，不读取 SDK，不链接任何库。`build-plan.json` 是配置计划，不是编译结果。MD 方案使用单独目录 `build-review-md` 和 `-DP1_CRT=MD`。

工程还保留默认关闭的 `P1_ENABLE_OBJECT_CHECK`，供下一阶段真实头文件对象编译使用；本轮不在现场开启它。该路径要求真实 Windows/MSVC、x64、Release、真实 SDK includes，以及根据适用官方资料选择的完整 MSVC 编译器版本 `P1_DOCUMENTED_MSVC_VERSION`（编译器版本不是 VS 产品版本）。它仅创建 OBJECT 目标，无 DLL/EXE 链接、部署或注册步骤。CMake 启用 MSVC 时自身会运行编译器探测，可能生成编译器识别临时产物、对象和探测库，因此也属于未来的编译活动；模板将 try_compile 类型限制为静态库，编译器识别由 CMake 管理。传入版本字符串只作匹配检查，不构成 PTC 支持认证；头文件存在也不证明 SDK 正版或版本匹配。后续应从真实 SDK 出处核定两者，不能伪造头文件完成检查。

即使将来对象编译通过，也只验证该头文件/编译器组合下的语法和声明，不能证明链接依赖、授权、Creo 加载或转换成功。源码与 CMake 均没有业务 API、`DllMain`、自动加载或 Java/WFC 回退路径。

### 7. 是否先检查 Build Tools

需要。先查安装情况和官方支持版本，再考虑缺什么；不要先安装“最新版本”。下一次本机只读核查可用 Visual Studio Installer 随附的 `vswhere.exe`：

```powershell
$vswherePath = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
if (Test-Path -LiteralPath $vswherePath) {
    & $vswherePath -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -format json
}
```

`vswhere` 缺失不证明没有编译器；有输出也不证明 x64 工具集、Windows SDK 及版本均受支持。检查对应安装目录下 VC 工具集版本目录与 Windows SDK 安装元数据，记录安装版本、组件、x64 能力，再与本版 PTC 支持矩阵核对。本轮不运行 `cl`/`link`、不安装组件、不执行 vcvars、不过问许可证服务器、不改系统环境。回传结论时省略个人安装路径和实例身份信息。

## 日志与后续整合

`evidence/verification.json` 记录此次云端实际验证范围；`events.jsonl` 用固定步骤及状态区分观测、候选和未验证项。日志不含本机原始日志、模型数据、私有目录或 SDK 正文。`SHA256SUMS` 覆盖包内其他全部文件。云端 CMake 是验证工具，不随包分发。

下一关：本版官方编译支持与库/入口原文 → 本机 Build Tools 只读确认 → 修订并核定模板 → 另轮真实头文件对象检查 → 另轮最小 DLL 链接及授权核查 → 受控加载/卸载 → 原生只读 API。通过前一关再推进下一关，Java/WFC 停止结论保持有效。
