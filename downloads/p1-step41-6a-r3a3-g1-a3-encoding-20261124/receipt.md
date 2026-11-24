# P1 Step41 6A R3A3 G1 A3 中文资源编码整改回执

2026-11-24 · 路径 A · 离线字节验收通过 · A3 候选真机复看待完成。

## 候选与验收

交接包 CRC 和清单校验通过，四份基线资源逐字节匹配 A3 受控部署登记。候选仅在两份 `chinese_cn` 文件的头部各加一次 UTF-8 BOM（`EF BB BF`）。剥掉前三字节后，候选正文与基线逐字节相同。两份 `usascii` 资源仍为纯 ASCII，无 BOM。

| 资源 | 基线 → 候选字节数 | 键数 | 候选编码 |
| --- | ---: | ---: | --- |
| `chinese_cn/p1_smt_conv.txt` | 2,893 → 2,896 | 36 | UTF-8 BOM |
| `chinese_cn/p1_smt_unit_probe.txt` | 1,707 → 1,710 | 22 | UTF-8 BOM |
| `usascii/p1_smt_conv.txt` | 2,952 → 2,952 | 36 | ASCII |
| `usascii/p1_smt_unit_probe.txt` | 1,761 → 1,761 | 22 | ASCII |

四份资源均通过严格 UTF-8 解码。逐键核对键名、键值、注释/空行分块和占位符。转换资源中的 `P1_SMT_CONV_TEXT` 仍有一个 `%0s`，单位探针资源无 `%0s`。带 BOM 的单位探针资源与 R3A3 正对照逐字节相同。归因文档末段提到 CRLF，输入单和实物均为 LF，本次保留 LF。

补丁正向、反向应用及逐字节验证已通过。候选 ZIP 包含两份中文资源、补丁、清单与本回执。应用补丁或提取其中两份中文资源即可用于离线候选审核。C++、DAT、`text_dir` 与 DLL 沿用冻结基线。DLL 复用身份见 `manifest.json`。此前隔离构建记录为 16 项导出、8 个依赖，资源整改未触发重编或重验 dumpbin。

## 受控真机待验

带 BOM 的既有显示证据限于本机 Creo 13.4.0.0 / zh-CN CP936。A3 候选本身仍需受控复看中文按钮标签和状态栏消息，并在可达场景对照 ASCII 编号串与动态 `%0s` 中文参数。现场 `uiLanguage`、`effectiveMessageLanguage`、SDK 内部探测机制、其他编码配置和跨版本、跨代码页行为均未验证。

既有 A3 为 `A3_LOCK_NOT_CONFIRMED / a3_lock`，同时记录 `cleanup_failed=true` 与最终回读未锁。终态对照与输入单的「无 cleanup 失败」须各按实测独立登记。本次编码候选的离线通过无法关闭锁清理门。动态中文参数若因加锁段早停而不可达，也应据实标记未验证。
