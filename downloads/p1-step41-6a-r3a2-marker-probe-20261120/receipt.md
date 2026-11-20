# P1 Step41 6A R3A 二轮分层契约复核与标记探针

2026-11-20 · Creo 13.4.0.0 x64 · R3 探针基线 `7789c1d1e7b156869a5d20a19f5e47f6246107a9` · 冻结创建基线 `b99692e3f927ff297941e812e36ac5646bd323b7`

首轮三中文静态样本均返回英文，根因仍为 NOT_CLOSED。输入单中的五个资源登记均与 R3 发布源码 ZIP 逐项对账一致。首轮现场 DLL、DAT 和 JSONL 仍以本机登记引用。候选只改 A 的消息回调并新增八份资源。B/C、三条旧只读命令、创建段源代码、原 R3 消息资源与原 usascii 对照哈希均保留。

## M-1 至 M-4 · 契约与证据边界

| 层 | 可依据的契约和本轮结论 |
|---|---|
| M-1 / L1 查找 | 本机 `ProMessage.h:10,106–119` 摘录确认 `ProMessageDisplay(ProFileName file_name, ProCharLine message_name, …)` 同时接受文件与键，列出 `PRO_TK_E_NOT_FOUND`、`PRO_TK_MSG_NOT_FOUND`、`PRO_TK_MSG_NO_TRANS` 等区分。键或译文失败的定义未描述 `text_dir` 起点、语言目录优先顺序、缺同名文件时是否跨文件按键查找。初轮四文件复用键，结果不能确认首轮实际打开的文件。暂保留原 DAT，不根据 `text/<lang>` 的现状直接判布局正确或错误。 |
| M-2 / L2 绑定与时机 | 冻结 R3 的实际调用是 `ProCmdDesignate(id,label,help,description,file)`，传 `p1_r3_readonly.txt`，用于命令标签、帮助及描述。变体用同版已运行的 `ProMessageToBuffer(ProLine,file,key[,value])` 与 `ProMessageDisplay(file,key[,value])` 调用形式。回传 `PRO_TK_E_FOUND` 的 SDK 摘录提到文件已在内存且缺消息，支持记录缓存这一观察项，仍不足以判定独立消息文件先登记是必需条件。本轮保持注册原状；若本机 13.4 官方条目明确要求登记，再单独审核受支持的登记方式和时机。 |
| M-3 / L3 编码 | `ProMessage.h` 摘录确认可变参数会替换格式说明符。R3 `entry.cpp` 的 `ProLine`、`PRO_LINE_SIZE` 及 Windows 宽字符处理提供缓冲观测边界。SDK 摘录未给出消息资源默认代码页、BOM 或 `%0s` 窄参数解码规则。三个首轮文件字节的差异真实存在，英文缓冲无法排除编码造成的加载失败或回退。二轮保留与 R3 相同的动态参数调用方式；三个中文编码仅作对照。 |
| M-4 / L4 显示 | 缓冲和显示是两次独立的 SDK 调用，各有返回值。现有摘录没有保证两次解析或渲染同源。缓冲命中与屏幕文本独立核验，使用同一操作 ID 的返回码、缓冲和对应截图。未调用显示或显示失败的样本保留为未观察，不把上一条状态栏消息归给该样本。 |

PTC 在线帮助请求在本云环境返回 403。本轮未把缺少的官方语言回退规则、注册前置条件、编码约定或显示同源性写成已证结论。

## M-5 · 新文件、键与令牌

同一行的两份语言文件共享该变体的独有标记键，但返回不同的标记令牌。所有键和令牌均为纯 ASCII。

| 独立文件名 | 标记键 | `chinese_cn` 令牌 | `usascii` 令牌 |
|---|---|---|---|
| `p1_r3a2_ctl.txt` | `P1_R3A2_CTL_MARK` | `A2_CTL_ZH` | `A2_CTL_EN` |
| `p1_r3a2_u8.txt` | `P1_R3A2_U8_MARK` | `A2_U8_ZH` | `A2_U8_EN` |
| `p1_r3a2_bom.txt` | `P1_R3A2_BOM_MARK` | `A2_BOM_ZH` | `A2_BOM_EN` |
| `p1_r3a2_gbk.txt` | `P1_R3A2_GBK_MARK` | `A2_GBK_ZH` | `A2_GBK_EN` |

文件用沿用的四行格式，即键、值、`#`、`#`。四个变体的静态和动态键也分开；静态值采用 `令牌|正文`，动态值采用 `令牌|%0s`，防止标记命中后不同文字键仍取错来源而被当成编码问题。三个中文目标文件按 UTF‑8 无 BOM、UTF‑8 BOM、CP936 编码，其 ASCII 同名对照只用于二轮定位。此配对布局与首轮不同，二轮查找结果不能反推首轮走过相同路径。

一次触发只显示一个样本，每轮 14 个：1–2 是对照标记、静态；3–6 是 UTF‑8 无 BOM 的标记、静态、ASCII 动态、中文动态；7–10 是 UTF‑8 BOM 同组；11–14 是 CP936 同组。每次触发先重查标记。对照接受任一控制目录，三个中文组只接受各自的中文标记，且静态、动态返回的前缀必须与标记来源一致。中文动态另要求同组静态及 ASCII 动态匹配。

对账字段：

- `requested_file`、`marker_key`、`marker_pro_error`、`marker_buffer_json`、`marker_token_json`、`marker_matched_resource`、`lookup_layer`、`target_source_confirmed` 区分目标命中、同名 usascii、不同变体、未知令牌和 API 失败。匹配路径是按唯一令牌映射的资源身份，应与现场实际文件哈希对账。
- `text_matched_resource`、`text_source_matches_marker`、`text_json`、`expected_units_json`、`wide_units`、`terminated`、`param_roundtrip_ok` 区分文字来源、缓冲解码与动态替换。UTF-16 比较在 API 缓冲中完成，对应文字以 JSON 写入。
- `display_pro_error` 是显示调用状态，`display_observed` 与 `display_ok` 由用户截图回填，探针不自行判断。源不对、未命中或前置失败时，文字/参数用例沿用 `PREREQUISITE_UNVERIFIED`。日志沿用 `PROBE_MSG_BEGIN/RETURN` 和既有 START/DONE 事件及结果码。

## M-6 · 本机采集与验收

| 字段 | 采集 | 判定 |
|---|---|---|
| 候选及会话 | 登记已加载 DLL、DAT 的路径、字节数与 SHA-256，Creo 版本、xtop PID、load_token | 先与本机四门留档一致，再接收现场 trace；不同会话的样本不可直接配对。 |
| 配置与实际路径 | 保存激活 DAT 的 `text_dir` 原值、规范化绝对目录、每份资源的实际相对路径和绝对路径。有官方诊断或受控文件访问记录时，另登记实际查找路径及证据。 | 配置路径和实际命中分别记录，有回执但缺实际观测时，实际查找路径留空。 |
| 语言和代码页 | 保存会话实际 UI 语言与已确认消息语言、相关启动配置和各生效 `config.pro` 的语言/代码页设置及来源。可另外记 Windows 系统区域及代码页。 | 未查明的项留空；系统区域和 UI 语言不能代填 SDK 消息语言。 |
| 八份资源文件 | 用 `Get-Item`、`Get-FileHash -Algorithm SHA256` 报字节数与完整哈希，比对 manifest；登记两目录是否均存在，记录 BOM/编码检查。 | 文件身份要精确匹配。缺文件、旧版本或重复拷贝单独登记，不用其结果判编码。 |
| 事件和截图 | 保存原始 JSONL，按 PID / load token / operation ID 对齐，登记样本序号、SDK 返回、令牌、缓冲文字、截图名和哈希。 | 只有文字前缀与已确认来源一致的样本才用于编码或动态参数判定。截图只归于该次有成功显示调用的样本。 |

本机用 VS2022 17.14.41、MSVC 14.44.35207、Windows SDK 10.0.26100.0 及原 Creo 13.4 SDK 构建，保留 `/MT /EHsc /MACHINE:X64 /INCREMENTAL:NO`、原库和四入口，执行身份、静态、编译链接、dumpbin 及只读核验。用源码 ZIP 就无需再次应用 diff；用 diff 时对 R3 发布源码 ZIP 解压副本运行 `git apply --whitespace=error --check <diff>` 后再应用。压缩包包含清单，旁置 SHA-256 用于校验。

仅源码、资源及证据模板交付。已用 Linux C++14 替身验证 14 样本、命中/回退/错变体分支、参数门控与 B/C 的代表路径。Windows SDK 编译和 Creo 真机行为待本机验证。
