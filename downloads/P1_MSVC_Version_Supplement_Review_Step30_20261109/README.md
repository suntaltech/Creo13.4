# P1 Step30：MSVC 与 Windows SDK 版本补充核对

日期：2026-11-09。结论：已补充默认版本身份，官方兼容性仍待核实。当前不建议据此另装 VS，也不指定未经证实的替代工具集。

## 三项答复

| 问题 | 当前结论 | 原因 |
| --- | --- | --- |
| MSVC 14.44.35207 是否在 Creo 13.4 支持范围内 | 未核实；不能认定支持或不支持 | 缺适用 Creo 13.4.0.0 的 PTC 编译器支持条目及版本限制/脚注 |
| Windows SDK 10.0.26100.0 是否可用 | 已获用户报告的版本号；官方适用性与实际对象编译可用性均未验证 | 不能从 SDK 版本或 VS 安装成功直接推出 Pro/TOOLKIT 支持；本轮也没有检查其完整文件或执行编译 |
| 是否已有旧工具集可切换，还是必须另装 VS | 两项都不能确定；暂不安装 | 本轮仅报告默认工具集，没有所有并存工具集清单；且官方要求的目标版本尚未知 |

此前未知的默认 MSVC 工具集和 Windows SDK 版本号已由本轮任务单补充，不再要求重复采集同样两个值。缺口现在集中在“官方要求是什么”与“现有安装还包含什么”。未知不能写成不兼容；一次对象编译通过也不能替代 PTC 支持认证。

## 本轮新增信息

以下均为用户提供的本机只读结果摘要，云端未直接访问现场：

- Visual Studio 2022 Community：17.14.41。
- 默认 MSVC 工具集：14.44.35207。
- Windows SDK：10.0.26100.0。
- x64 C++ 工具集组件已安装。

`14.44.35207` 是本轮报告的工具集版本，不是已经读取的 `cl.exe` 完整编译器版本。不能简单改成 `19.44.35207`，也不能将 `14.44.35207` 或 VS 的 `17.14.41` 填入 Step28 的 `P1_DOCUMENTED_MSVC_VERSION`。该参数用于与 CMake 实际检测到的编译器版本比对，仍须在后续取得相应证据。本轮不执行 cl 或 link 获取版本。

默认工具集不等于全部已安装工具集，也不等于未来 CMake 一定选用的工具集。Windows SDK 的版本号不证明它已被选为构建目标，也不代表本机 Windows 操作系统版本。

## 官方依据的状态

本轮没有收到官方支持表正文。Step29 已尝试读取：

https://support.ptc.com/help/creo_toolkit/protoolkit_plus/usascii/creo_toolkit/get_started/Platform_and_Compiler_Support.html

该次请求在代理 CONNECT 阶段返回 403，没有取得 PTC 文档正文。本轮保留这一历史失败记录，没有把它描述为本轮新请求，也没有重复尝试同一受限来源。该 URL 本身不固定 Creo 发布版本，不能仅凭链接将页面归属于 13.4。

需要从本机 `online_help/creo_toolkit/get_started/Platform_and_Compiler_Support.html` 的实际引用取得官方表格，保留标题、修订信息、产品/维护版本、编译器条目及相关脚注。若官方文本只写 VS2022 而未明确补丁范围，不能自行追加“所有 17.x 均支持”或“只允许某个 17.x”的限制；若未规定 Windows SDK 版本，也应记录“该资料未规定”，不能编造明确的支持/禁止条目。

## 切换或安装的判断顺序

1. 先核对官方条目是否确实适用于 Creo 13.4，并提取明确版本或范围。
2. 比对当前 14.44.35207 / 10.0.26100.0。明确匹配时保留现有安装；资料不足时保持待核实。
3. 仅当当前组合有明确不匹配时，再核对同一 VS 安装中的并存工具集及 SDK。若已有官方要求的版本，可规划显式选择该版本；不通过删除默认工具集或修改全局环境实现。
4. 若缺少要求的工具集，先核实能否由现有 VS 安装承载所需组件；只有官方要求确实指向其他 VS 产品版本且现有安装无法满足，才讨论并存安装其他 VS。本轮不执行任何安装。

## 最少补充输入与只读命令示例

优先提供适用本版的官方编译器条目。要回答“有没有旧工具集”，还需列出选定 VS 实例下 `VC/Tools/MSVC` 的直接子目录，以及本机已有 Windows SDK 版本目录；无需重新扫描整个磁盘，也无需再次回传单独的默认值。

下面命令仅作为下一轮本机只读示例，填写已核对的安装根目录后使用。本轮没有在现场运行。它们只列目录或读取文件版本元数据，不启动 cl/link，不调用 vcvars，也不修改默认配置。

```powershell
$vsInstallRoot = '填入已核对的VS安装目录'
$msvcRoot = Join-Path $vsInstallRoot 'VC\Tools\MSVC'
Get-ChildItem -LiteralPath $msvcRoot -Directory -ErrorAction Stop |
    Select-Object -ExpandProperty Name

# 只读磁盘文件版本，不执行 cl.exe；该信息不等同于编译结果。
$compilerFile = Join-Path $msvcRoot '14.44.35207\bin\Hostx64\x64\cl.exe'
(Get-Item -LiteralPath $compilerFile -ErrorAction Stop).VersionInfo |
    Select-Object FileVersion, ProductVersion

$kitsRoot = '填入已核对的WindowsKits安装目录'
Get-ChildItem -LiteralPath (Join-Path $kitsRoot 'Include') -Directory -ErrorAction Stop |
    Select-Object -ExpandProperty Name
Get-ChildItem -LiteralPath (Join-Path $kitsRoot 'Lib') -Directory -ErrorAction Stop |
    Select-Object -ExpandProperty Name
```

只需回传目录版本名及必要版本字段，省略个人完整路径。目录存在不证明文件完整；没有某目录时记录实际缺失或错误，不安装补齐。若机器上有多个 VS 实例，本例只覆盖所选实例，不能用它断言其他实例没有旧工具集。

## 本轮执行与交付

本轮仅完成离线证据比对、报告、结构化日志和打包；没有执行 cl/link，没有生成对象或 DLL，没有启动 Creo、安装 VS 或修改配置。Step28 候选源码和构建模板保持不变；Java/WFC 路线保持停止。

包内 `comparison.json` 保存更新前后身份字段及三项判定；`next-input.json` 列出剩余最小输入；`evidence/events.jsonl` 区分用户报告、历史来源限制、未执行动作和待核实项；`evidence/source-register.json` 保留输入散列；`evidence/verification.json` 仅记录文档/数据一致性检查，不能当成原生编译测试。包内无 SDK、VS 安装器或现场原始日志。
